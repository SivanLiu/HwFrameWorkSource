package com.android.server.location;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.IGnssStatusListener.Stub;
import android.location.ILocationManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import java.util.ArrayList;
import java.util.Iterator;

public class HwGpsActionReporter implements IHwGpsActionReporter {
    private static final int ACTION_LOC_REMOVE = 0;
    private static final int ACTION_LOC_REQUEST = 1;
    private static final String GPS_INJECT_LOCATION_PERMISSION = "com.huawei.android.permission.INJECT_LOCATION";
    private static final int GPS_POSITION_MODE_MS_BASED = 1;
    private static final int GPS_POSITION_MODE_STANDALONE = 0;
    private static final int GPS_START = 1;
    private static final int GPS_STOP = 0;
    private static final int GPS_SWITCH_OFF = 0;
    private static final int GPS_SWITCH_ON = 1;
    protected static final boolean HWFLOW;
    protected static final boolean HWLOGW_E = true;
    private static final int LOCATION_ACTION_REMOVE = 1;
    private static final int LOCATION_ACTION_REQUEST = 0;
    private static final int LOCATION_CHANGE_THRESHOLD_VALUE = 5000;
    private static final long LOCATION_INTERVAL = 0;
    private static final float LOCATION_MIN_DISTANCE = 0.0f;
    private static final int LOCATION_REPORT_GPSSETTING = 65002;
    private static final int LOCATION_REPORT_GPSSWITCH = 65001;
    private static final int LOCATION_REPORT_GPS_ACTION = 65004;
    private static final int LOCATION_REPORT_GPS_DISABLE = 0;
    private static final int LOCATION_REPORT_GPS_ENABLE = 1;
    private static final int LOCATION_REPORT_GPS_START = 1;
    private static final int LOCATION_REPORT_GPS_STOP = 0;
    private static final int LOCATION_REPORT_LOCATION_ACTION = 65003;
    private static final int LOCATION_REPORT_LOCATION_CHANGE = 65005;
    private static final int NO_FIX_TIMEOUT = 60000;
    private static final String TAG = "HwGpsActionReporter";
    private static final int VALID_DISTANCE_RANGE = SystemProperties.getInt("ro.config.invalid_distance", 10000);
    private static final int VALID_TIME_RANGE = SystemProperties.getInt("ro.config.invalid_time", AwareAppAssociate.ASSOC_DECAY_MIN_TIME);
    private static final Object mLock = new Object();
    private static volatile HwGpsActionReporter mSingleInstance = null;
    private int mCellID = -1;
    private ConnectivityManager mConnectivityManager = null;
    private Context mContext = null;
    private int mCurrentUserId = 0;
    private ArrayList<Location> mErrorNetworkLocations = new ArrayList();
    private Location mFirstLocation = null;
    private Location mGpsLastLocation = null;
    private int mGpsStatus = 0;
    private GpsStatusListenerTransport mGpsStatusListener = null;
    private ILocationManager mILocationManager;
    private Location mInjectLocation = null;
    private final BroadcastReceiver mInjectedLocationReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(HwGpsActionReporter.TAG, "intent is null");
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                Log.e(HwGpsActionReporter.TAG, "action is null");
                return;
            }
            if (action.equals("action_inject_location")) {
                if (HwGpsActionReporter.HWFLOW) {
                    Log.i(HwGpsActionReporter.TAG, "receive inject location broadcast");
                }
                HwGpsActionReporter.this.processLocInjectInfo((Location) intent.getParcelableExtra("key_location"));
            } else if (action.equals("android.intent.action.USER_SWITCHED")) {
                if (HwGpsActionReporter.HWFLOW) {
                    Log.i(HwGpsActionReporter.TAG, "receive switch user broadcast");
                }
                HwGpsActionReporter.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
            }
        }
    };
    private int mLastGpsSwitchStatus;
    private Location mLastLocation = null;
    private LocationManager mLocationManager;
    private String mMccMnc = "";
    private boolean mNetworkAvailable = false;
    private int mNetworkLocCorrCnt = 0;
    private int mNetworkLocErrCnt = 0;
    private int mNetworkType = 0;
    private PassiveLocationListener mPassiveLocationListener = null;
    private String mProvider = null;
    private HwReportTool mReportTool = null;
    private String mTableID = null;
    private TelephonyManager mTelephonyManager = null;
    private ArrayList<Location> mUncheckdNetworkLocations = new ArrayList();

    private class GpsStatusListenerTransport extends Stub {
        private double mDeltaDis;
        private int mSvCount;
        private int mTimeToFirstFix;

        private GpsStatusListenerTransport() {
            this.mTimeToFirstFix = 0;
            this.mSvCount = 0;
            this.mDeltaDis = -1.0d;
        }

        /* synthetic */ GpsStatusListenerTransport(HwGpsActionReporter x0, AnonymousClass1 x1) {
            this();
        }

        public void onGnssStarted() {
            int positionMode;
            if (Global.getInt(HwGpsActionReporter.this.mContext.getContentResolver(), "assisted_gps_enabled", 1) != 0) {
                positionMode = 1;
            } else {
                positionMode = 0;
            }
            HwGpsActionReporter.this.reportGpsStarted(positionMode);
            HwGpsActionReporter.this.setGpsStatus(1);
        }

        public void onGnssStopped() {
            if (HwGpsActionReporter.this.mInjectLocation == null) {
                this.mDeltaDis = -1.0d;
                if (HwGpsActionReporter.HWFLOW) {
                    Log.i(HwGpsActionReporter.TAG, "mInjectLocation is null.");
                }
            } else if (HwGpsActionReporter.this.mFirstLocation != null) {
                this.mDeltaDis = (double) HwGpsActionReporter.this.mInjectLocation.distanceTo(HwGpsActionReporter.this.mFirstLocation);
            } else {
                this.mDeltaDis = -1.0d;
                if (HwGpsActionReporter.HWFLOW) {
                    Log.i(HwGpsActionReporter.TAG, "mFirstLocation is null.");
                }
            }
            HwGpsActionReporter.this.reportGpsStopped(this.mTimeToFirstFix, this.mSvCount, this.mDeltaDis);
            this.mTimeToFirstFix = 0;
            this.mSvCount = 0;
            HwGpsActionReporter.this.clearStateInfo();
            HwGpsActionReporter.this.setGpsStatus(0);
        }

        public void onFirstFix(int ttff) {
            this.mTimeToFirstFix = ttff;
        }

        public void onSvStatusChanged(int svCount, int[] prnWithFlags, float[] cn0s, float[] elevations, float[] azimuths, float[] carrierFreqs) {
            this.mSvCount = svCount;
        }

        public void onNmeaReceived(long timestamp, String nmea) {
        }
    }

    private class PassiveLocationListener implements LocationListener {
        private PassiveLocationListener() {
        }

        /* synthetic */ PassiveLocationListener(HwGpsActionReporter x0, AnonymousClass1 x1) {
            this();
        }

        public void onLocationChanged(Location location) {
            if (location == null) {
                Log.e(HwGpsActionReporter.TAG, "location is null");
                return;
            }
            String str;
            StringBuilder stringBuilder;
            boolean isGpsProvider = "gps".equals(location.getProvider());
            boolean isNetworkProvider = "network".equals(location.getProvider());
            boolean isFusedProvider = "fused".equals(location.getProvider());
            if (HwGpsActionReporter.HWFLOW) {
                str = HwGpsActionReporter.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("provider:");
                stringBuilder.append(location.getProvider());
                stringBuilder.append(",acc is ");
                stringBuilder.append(location.getAccuracy());
                Log.i(str, stringBuilder.toString());
            }
            if (isGpsProvider) {
                if (HwGpsActionReporter.this.mFirstLocation == null) {
                    HwGpsActionReporter.this.mFirstLocation = location;
                }
                HwGpsActionReporter.this.mGpsLastLocation = location;
                if (HwGpsActionReporter.HWFLOW) {
                    Log.i(HwGpsActionReporter.TAG, "refresh mGpsLastLocation");
                }
            }
            if (isNetworkProvider) {
                HwGpsActionReporter.this.mUncheckdNetworkLocations.add(location);
                if (HwGpsActionReporter.HWFLOW) {
                    str = HwGpsActionReporter.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("add:");
                    stringBuilder.append(Integer.toHexString(System.identityHashCode(location)));
                    Log.i(str, stringBuilder.toString());
                }
            }
            if (isGpsProvider || isNetworkProvider) {
                HwGpsActionReporter.this.removeExpiredNetworkLocations();
                HwGpsActionReporter.this.checkErrorNetworkLocations();
                HwGpsActionReporter.this.reportErrorNetworkLocations();
            }
            if (isGpsProvider || isNetworkProvider || isFusedProvider) {
                HwGpsActionReporter.this.reportLocationChange(location);
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public static HwGpsActionReporter getInstance(Context context, ILocationManager iLocationManager) {
        if (mSingleInstance == null) {
            synchronized (mLock) {
                if (mSingleInstance == null) {
                    mSingleInstance = new HwGpsActionReporter(context, iLocationManager);
                }
            }
        }
        return mSingleInstance;
    }

    private HwGpsActionReporter(Context context, ILocationManager iLocationManager) {
        this.mContext = context;
        this.mILocationManager = iLocationManager;
        this.mReportTool = HwReportTool.getInstance(context);
        this.mGpsStatusListener = new GpsStatusListenerTransport(this, null);
        this.mPassiveLocationListener = new PassiveLocationListener(this, null);
        this.mLastGpsSwitchStatus = isGpsProviderSettingOn(this.mCurrentUserId);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("action_inject_location");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mInjectedLocationReceiver, intentFilter, GPS_INJECT_LOCATION_PERMISSION, null);
        try {
            if (!this.mILocationManager.registerGnssStatusCallback(this.mGpsStatusListener, this.mContext.getPackageName())) {
                Log.e(TAG, "addGpsStatusListener failed");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "addGpsStatusListener catch RemoteException", e);
        }
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        this.mLocationManager.requestLocationUpdates("passive", 0, 0.0f, this.mPassiveLocationListener);
        this.mContext.getContentResolver().registerContentObserver(Secure.getUriFor("location_providers_allowed"), true, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                String providersSetting = HwGpsActionReporter.this.getProvidersSettingAllowed(HwGpsActionReporter.this.mCurrentUserId);
                int gpsSwitchStatus = HwGpsActionReporter.this.isGpsProviderSettingOn(HwGpsActionReporter.this.mCurrentUserId);
                if (gpsSwitchStatus != HwGpsActionReporter.this.mLastGpsSwitchStatus) {
                    HwGpsActionReporter.this.reportGpsSwitch(gpsSwitchStatus);
                    HwGpsActionReporter.this.mLastGpsSwitchStatus = gpsSwitchStatus;
                }
                HwGpsActionReporter.this.reportProvidersSetting(providersSetting);
            }
        }, -1);
    }

    public boolean uploadLocationAction(int actionType, String actionMsg) {
        StringBuilder strBuilder = new StringBuilder("");
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("uploadLocationAction:actionType:");
            stringBuilder.append(actionType);
            stringBuilder.append("actionMsg:");
            stringBuilder.append(actionMsg);
            Log.i(str, stringBuilder.toString());
        }
        if (actionMsg == null) {
            return false;
        }
        boolean reportRst;
        switch (actionType) {
            case 0:
                strBuilder.append("{RT:0");
                strBuilder.append(actionMsg);
                strBuilder.append("}");
                reportRst = report(LOCATION_REPORT_LOCATION_ACTION, strBuilder.toString());
                break;
            case 1:
                strBuilder.append("{RT:1");
                strBuilder.append(actionMsg);
                strBuilder.append("}");
                reportRst = report(LOCATION_REPORT_LOCATION_ACTION, strBuilder.toString());
                break;
            default:
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unexpected action type:");
                stringBuilder2.append(actionType);
                Log.e(str2, stringBuilder2.toString());
                return false;
        }
        return reportRst;
    }

    private void reportGpsSwitch(int gpsSwitchStatus) {
        boolean checkResult = isGpsProviderSettingOn(this.mCurrentUserId) == gpsSwitchStatus;
        StringBuilder strBuilder = new StringBuilder("");
        strBuilder.append("{ACT:");
        strBuilder.append(gpsSwitchStatus);
        strBuilder.append(",RT:");
        strBuilder.append(checkResult);
        strBuilder.append("}");
        String reportString = strBuilder.toString();
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reportGpsSwitch: ");
            stringBuilder.append(reportString);
            Log.i(str, stringBuilder.toString());
        }
        report(LOCATION_REPORT_GPSSWITCH, reportString);
    }

    private void reportProvidersSetting(String itemValue) {
        StringBuilder strBuilder = new StringBuilder("");
        strBuilder.append("{Mod:[");
        strBuilder.append(itemValue);
        strBuilder.append("]}");
        String reportString = strBuilder.toString();
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reportProvidersSetting: ");
            stringBuilder.append(reportString);
            Log.i(str, stringBuilder.toString());
        }
        report(LOCATION_REPORT_GPSSETTING, reportString);
    }

    private void reportGpsStarted(int positionMode) {
        StringBuilder strBuilder = new StringBuilder("");
        strBuilder.append("{GA:");
        strBuilder.append(1);
        strBuilder.append(",PAR1:");
        strBuilder.append(positionMode);
        strBuilder.append(",PAR2:");
        strBuilder.append(60000);
        strBuilder.append("}");
        String reportString = strBuilder.toString();
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reportGpsStarted: ");
            stringBuilder.append(reportString);
            Log.i(str, stringBuilder.toString());
        }
        report(LOCATION_REPORT_GPS_ACTION, reportString);
    }

    private void reportGpsStopped(int ttff, int svCount, double deltaDistance) {
        StringBuilder strBuilder = new StringBuilder("");
        strBuilder.append("{GA:");
        strBuilder.append(0);
        strBuilder.append(",PAR1:");
        strBuilder.append(ttff);
        strBuilder.append(",PAR2:");
        strBuilder.append(svCount);
        strBuilder.append(",PAR3:");
        strBuilder.append(this.mProvider);
        strBuilder.append(",PAR4:");
        strBuilder.append(this.mTableID);
        strBuilder.append(",PAR5:");
        strBuilder.append(deltaDistance);
        strBuilder.append("}");
        String reportString = strBuilder.toString();
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reportGpsStopped: ");
            stringBuilder.append(reportString);
            Log.i(str, stringBuilder.toString());
        }
        report(LOCATION_REPORT_GPS_ACTION, reportString);
    }

    private void reportLocationChange(Location location) {
        if (location != null && isLocationChange(location)) {
            refreshNetworkStatus();
            refreshCellStatus();
            StringBuilder strBuilder = new StringBuilder("");
            strBuilder.append("{CID:");
            strBuilder.append(this.mCellID);
            strBuilder.append(",MCCMNC:");
            strBuilder.append(this.mMccMnc);
            strBuilder.append(",LAT:");
            strBuilder.append(location.getLatitude());
            strBuilder.append(",LON:");
            strBuilder.append(location.getLongitude());
            strBuilder.append(",NA:");
            strBuilder.append(this.mNetworkAvailable);
            strBuilder.append(",NT:");
            strBuilder.append(this.mNetworkType);
            strBuilder.append("}");
            String reportString = strBuilder.toString();
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("reportLocationChange:");
                stringBuilder.append(location);
                Log.i(str, stringBuilder.toString());
            }
            report(LOCATION_REPORT_LOCATION_CHANGE, reportString);
        }
    }

    private void reportErrorNetworkLocations() {
        if (!this.mErrorNetworkLocations.isEmpty()) {
            refreshNetworkStatus();
            refreshCellStatus();
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mErrorNetworkLocations's size is: ");
                stringBuilder.append(this.mErrorNetworkLocations.size());
                Log.i(str, stringBuilder.toString());
            }
            Iterator<Location> unReportedLocations = this.mErrorNetworkLocations.iterator();
            while (unReportedLocations.hasNext()) {
                Location location = (Location) unReportedLocations.next();
                this.mNetworkLocErrCnt++;
                float corrRate = ((float) this.mNetworkLocCorrCnt) / ((float) (this.mNetworkLocErrCnt + this.mNetworkLocCorrCnt));
                StringBuilder strBuilder = new StringBuilder("");
                strBuilder.append("{CID:");
                strBuilder.append(this.mCellID);
                strBuilder.append(",MCCMNC:");
                strBuilder.append(this.mMccMnc);
                strBuilder.append(",LAT:");
                strBuilder.append(location.getLatitude());
                strBuilder.append(",LON:");
                strBuilder.append(location.getLongitude());
                strBuilder.append(",NA:");
                strBuilder.append(this.mNetworkAvailable);
                strBuilder.append(",NT:");
                strBuilder.append(this.mNetworkType);
                strBuilder.append(",RATE:");
                strBuilder.append(corrRate);
                strBuilder.append("}");
                String reportString = strBuilder.toString();
                if (HWFLOW) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("corrRate:");
                    stringBuilder2.append(corrRate);
                    Log.i(str2, stringBuilder2.toString());
                }
                report(LOCATION_REPORT_LOCATION_CHANGE, reportString);
                unReportedLocations.remove();
            }
        }
    }

    private void checkErrorNetworkLocations() {
        if (!this.mUncheckdNetworkLocations.isEmpty() && isReferenceLocationAvailable()) {
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkErrorNetworkLocations mUncheckdNetworkLocations's size is: ");
                stringBuilder.append(this.mUncheckdNetworkLocations.size());
                Log.i(str, stringBuilder.toString());
            }
            Iterator<Location> uncheckedLocations = this.mUncheckdNetworkLocations.iterator();
            while (uncheckedLocations.hasNext()) {
                Location networkLocation = (Location) uncheckedLocations.next();
                if (networkLocation == null) {
                    Log.e(TAG, "removeExpiredNetworkLocations networkLocation is null");
                } else if (this.mGpsLastLocation.getAccuracy() - networkLocation.getAccuracy() <= 0.0f) {
                    float distance = this.mGpsLastLocation.distanceTo(networkLocation);
                    if (distance > ((float) VALID_DISTANCE_RANGE)) {
                        this.mErrorNetworkLocations.add(networkLocation);
                    } else {
                        this.mNetworkLocCorrCnt++;
                    }
                    uncheckedLocations.remove();
                    if (HWFLOW) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("remove: ");
                        stringBuilder2.append(Integer.toHexString(System.identityHashCode(networkLocation)));
                        stringBuilder2.append(", reason: checked: ");
                        stringBuilder2.append(distance);
                        stringBuilder2.append(" meters");
                        Log.i(str2, stringBuilder2.toString());
                    }
                } else if (HWFLOW) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("input gpsLocation is less accuracy,");
                    stringBuilder3.append(Integer.toHexString(System.identityHashCode(networkLocation)));
                    Log.i(str3, stringBuilder3.toString());
                }
            }
        }
    }

    private void removeExpiredNetworkLocations() {
        if (!this.mUncheckdNetworkLocations.isEmpty()) {
            long referenceTime = System.currentTimeMillis();
            if (HWFLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeExpiredNetworkLocations mUncheckdNetworkLocations's size is: ");
                stringBuilder.append(this.mUncheckdNetworkLocations.size());
                Log.i(str, stringBuilder.toString());
            }
            Iterator<Location> uncheckedLocations = this.mUncheckdNetworkLocations.iterator();
            while (uncheckedLocations.hasNext()) {
                Location location = (Location) uncheckedLocations.next();
                if (location == null) {
                    Log.e(TAG, "removeExpiredNetworkLocations network location is null");
                } else {
                    long diffTime = referenceTime - location.getTime();
                    if (diffTime > ((long) VALID_TIME_RANGE)) {
                        if (HWFLOW) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("remove: ");
                            stringBuilder2.append(Integer.toHexString(System.identityHashCode(location)));
                            stringBuilder2.append(", reason: expired: ");
                            stringBuilder2.append(diffTime);
                            stringBuilder2.append("ms");
                            Log.i(str2, stringBuilder2.toString());
                        }
                        uncheckedLocations.remove();
                    }
                }
            }
        }
    }

    private boolean isReferenceLocationAvailable() {
        if (this.mGpsLastLocation == null) {
            if (HWFLOW) {
                Log.i(TAG, "mGpsLastLocation is null");
            }
            return false;
        }
        long diffTime = System.currentTimeMillis() - this.mGpsLastLocation.getTime();
        if (diffTime <= ((long) VALID_TIME_RANGE)) {
            return true;
        }
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("gpslocation is expired:");
            stringBuilder.append(diffTime);
            stringBuilder.append("ms");
            Log.i(str, stringBuilder.toString());
        }
        return false;
    }

    private boolean isLocationChange(Location location) {
        boolean isChange = false;
        if (location == null) {
            Log.e(TAG, "location is null");
            return false;
        } else if (this.mLastLocation == null) {
            this.mLastLocation = location;
            return true;
        } else {
            float dis1 = location.distanceTo(this.mLastLocation);
            float dis2 = location.getAccuracy();
            if (5000.0f <= (dis1 > dis2 ? dis1 : dis2)) {
                isChange = true;
            }
            if (isChange) {
                this.mLastLocation = location;
            }
            return isChange;
        }
    }

    @SuppressLint({"NewApi"})
    private void refreshCellStatus() {
        String str;
        StringBuilder stringBuilder;
        if (this.mTelephonyManager == null) {
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        }
        if (this.mTelephonyManager != null) {
            this.mMccMnc = this.mTelephonyManager.getSimOperator();
            int type = this.mTelephonyManager.getCurrentPhoneType();
            CellLocation location = this.mTelephonyManager.getCellLocation();
            switch (type) {
                case 1:
                    if (location instanceof GsmCellLocation) {
                        try {
                            this.mCellID = ((GsmCellLocation) location).getCid();
                            return;
                        } catch (Exception e) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("GsmCellLocation Type Cast Exception :");
                            stringBuilder.append(e.getMessage());
                            Log.e(str, stringBuilder.toString());
                            return;
                        }
                    }
                    return;
                case 2:
                    if (location instanceof CdmaCellLocation) {
                        try {
                            this.mCellID = ((CdmaCellLocation) location).getBaseStationId();
                            return;
                        } catch (Exception e2) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("CdmaCellLocation Type Cast Exception :");
                            stringBuilder.append(e2.getMessage());
                            Log.e(str, stringBuilder.toString());
                            return;
                        }
                    }
                    return;
                default:
                    this.mCellID = -1;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("unexpected phone type:");
                    stringBuilder2.append(type);
                    Log.e(str2, stringBuilder2.toString());
                    return;
            }
        }
        Log.e(TAG, "mTelephonyManager is null");
    }

    private void refreshNetworkStatus() {
        if (this.mConnectivityManager == null) {
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        NetworkInfo networkInfo = null;
        if (this.mConnectivityManager != null) {
            networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        }
        if (networkInfo != null) {
            this.mNetworkAvailable = networkInfo.isAvailable();
            this.mNetworkType = networkInfo.getSubtype();
        }
    }

    private void processLocInjectInfo(Location location) {
        if (location == null) {
            if (HWFLOW) {
                Log.i(TAG, "processLocInjectInfo: location is null");
            }
        } else if (this.mGpsStatus == 0) {
            if (HWFLOW) {
                Log.i(TAG, "processLocInjectInfo: gps does not start");
            }
        } else if (this.mFirstLocation != null) {
            if (HWFLOW) {
                Log.i(TAG, "processLocInjectInfo: gps has already fixed");
            }
        } else {
            this.mInjectLocation = location;
            Bundle b = location.getExtras();
            if (b != null) {
                this.mProvider = b.getString(HwLocalLocationProvider.KEY_LOC_SOURCE);
                this.mTableID = b.getString(HwLocalLocationProvider.KEY_LOC_TABLEID);
            }
        }
    }

    private boolean report(int actionID, String actionMsg) {
        if (this.mReportTool != null) {
            return this.mReportTool.report(actionID, actionMsg);
        }
        return false;
    }

    void setGpsStatus(int gpsStatus) {
        if (1 == gpsStatus || gpsStatus == 0) {
            this.mGpsStatus = gpsStatus;
        } else if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unexpceted gpsStatus:");
            stringBuilder.append(gpsStatus);
            Log.i(str, stringBuilder.toString());
        }
    }

    void clearStateInfo() {
        this.mInjectLocation = null;
        this.mFirstLocation = null;
        this.mTableID = null;
        this.mProvider = null;
    }

    void switchUser(int userId) {
        if (this.mCurrentUserId != userId) {
            this.mCurrentUserId = userId;
            this.mLastGpsSwitchStatus = isGpsProviderSettingOn(userId);
        }
    }

    boolean isGpsProviderSettingOn(int userId) {
        try {
            return Secure.isLocationProviderEnabledForUser(this.mContext.getContentResolver(), "gps", userId);
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("illegal userId for isLocationProviderEnabledForUser:");
            stringBuilder.append(userId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    boolean isNetworkProviderSettingOn(int userId) {
        try {
            return Secure.isLocationProviderEnabledForUser(this.mContext.getContentResolver(), "network", userId);
        } catch (IllegalArgumentException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("illegal userId for isLocationProviderEnabledForUser:");
            stringBuilder.append(userId);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    String getProvidersSettingAllowed(int userId) {
        String allowedProviders = Secure.getStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", userId);
        if (allowedProviders != null) {
            return allowedProviders.replace("0,", "").replace("0", "");
        }
        return "";
    }
}
