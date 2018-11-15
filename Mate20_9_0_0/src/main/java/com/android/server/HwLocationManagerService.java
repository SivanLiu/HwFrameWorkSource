package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hdm.HwDeviceManager;
import android.location.GeoFenceParams;
import android.location.Geofence;
import android.location.Location;
import android.location.LocationRequest;
import android.net.ConnectivityManager;
import android.net.HwNetworkPolicyManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocationManagerService.Receiver;
import com.android.server.LocationManagerService.UpdateRecord;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.location.GnssLocationProvider;
import com.android.server.location.GpsFreezeProc;
import com.android.server.location.HwCryptoUtility.AESLocalDbCrypto;
import com.android.server.location.HwGeoFencerBase;
import com.android.server.location.HwGeoFencerProxy;
import com.android.server.location.HwGpsLogServices;
import com.android.server.location.HwGpsPowerTracker;
import com.android.server.location.HwLocalLocationManager;
import com.android.server.location.HwLocalLocationProvider;
import com.android.server.location.LocationProviderProxy;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONException;
import org.json.JSONObject;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwLocationManagerService extends LocationManagerService {
    private static final String ACCURACY = "accuracy";
    private static final String AIDL_MESSAGE_SERVICE_CLASS = ".HwLBSService";
    private static final String AIDL_MESSAGE_SERVICE_PACKAGE = "com.huawei.lbs";
    static final int CODE_ADD_LOCATION_MODE = 1004;
    static final int CODE_GET_POWR_TYPE = 1001;
    static final int CODE_GNSS_DETECT = 1007;
    static final int CODE_LOG_EVENT = 1002;
    static final int CODE_REMOVE_LOCATION_MODE = 1005;
    public static final boolean D = Log.isLoggable(TAG, 3);
    protected static final int DEFAULT_MODE = 0;
    private static final String DESCRIPTOR = "android.location.ILocationManager";
    private static final String GNSS_LOCATION_FIX_STATUS = "GNSS_LOCATION_FIX_STATUS";
    private static final int INVALID_MODE_CODE = -1;
    private static final String LATITUDE = "latitude";
    private static final String LOCATION_MODE_BATTERY_SAVING = Integer.toString(2);
    private static final String LOCATION_MODE_HIGH_ACCURACY = Integer.toString(3);
    private static final String LOCATION_MODE_OFF = Integer.toString(0);
    private static final String LOCATION_MODE_SENSORS_ONLY = Integer.toString(1);
    private static final String LONGITUDE = "longitude";
    private static final String MASTER_PASSWORD = HwLocalLocationManager.MASTER_PASSWORD;
    protected static final int MODE_BATCHING = 2;
    protected static final int MODE_FREEZE = 5;
    protected static final int MODE_GPS = 1;
    protected static final int MODE_NETWORK = 3;
    protected static final int MODE_PASSIVE = 4;
    private static final int MSG_DELAY_START_LBS_SERVICE = 26;
    private static final int MSG_LOCATION_FIX_TIMEOUT = 25;
    private static final int MSG_LOCATION_FIX_TIMEOUT_DELAY = 10000;
    private static final int MSG_LOCTION_HUAWEI_BEGIN = 20;
    private static final int MSG_SAVE_LOCATION_IN_DATABASE = 27;
    private static final int MSG_SAVE_LOCATION_TIMEOUT_DELAY = 600000;
    private static final int MSG_SAVE_LOCATION_VALID_PERIOD = 3600000;
    private static final int MSG_SCREEN_OFF = 23;
    private static final int MSG_SCREEN_ON = 24;
    private static final int MSG_WIFI_ALWAYS_SCAN_REOPEN = 22;
    private static final int MSG_WIFI_ALWAYS_SCAN_RESET = 21;
    private static final int NETWORK_LOCATION_MIN_INTERVAL_BY_5G = 40000;
    private static final int OPERATION_SUCCESS = 0;
    private static final int OTHER_EXCEPTION = -2;
    private static final String SAVED_LOCATION = "save_location_database";
    private static final int SERVICE_RESTART_COUNT = 3;
    private static final long SERVICE_RESTART_TIME_INTERVAL = 60000;
    private static final long SERVICE_RUN_TIME_INTERVAL = 600000;
    static final int SETTINGS_LOCATION_MODE = 1006;
    private static final String SOURCE = "source";
    private static final String TAG = "HwLocationManagerService";
    private static final String TIMESTAMP = "timestamp";
    private static final int WIFI_ALWAYS_SCAN_REOPEN_DELAY = 30000;
    private static final int WIFI_ALWAYS_SCAN_SCEENONOFF_DELAY = 5000;
    private static final String WIFI_SCAN_ALWAYS_AVAILABLE_RESET_FLAG = "wifi_scan_always_available_reset_flag";
    private static final String WIFI_SCAN_ALWAYS_AVAILABLE_USER_OPR = "wifi_scan_always_available_user_selection";
    private static Map<Integer, Integer> mSettingsByUserId = new HashMap();
    private static ArrayList<String> mSupervisoryControlWhiteList = new ArrayList(Arrays.asList(new String[]{"com.sankuai.meituan"}));
    private long LOCK_HELD_BAD_TIME = 60000;
    private boolean WIFISCAN_CONTROL_ON = SystemProperties.getBoolean("ro.config.hw_wifiscan_control_on", false);
    private boolean isAlwaysScanReset = false;
    private boolean isAlwaysScanScreenOff = false;
    private IBinder mBinderLBSService;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = HwLocationManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("receive broadcast intent, action: ");
            stringBuilder.append(action);
            Log.d(str, stringBuilder.toString());
            if ("android.location.GPS_ENABLED_CHANGE".equals(action)) {
                if (intent.hasExtra("enabled")) {
                    boolean navigating = intent.getBooleanExtra("enabled", false);
                    String str2 = HwLocationManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("EXTRA_GPS_ENABLED navigating=");
                    stringBuilder2.append(navigating);
                    Log.d(str2, stringBuilder2.toString());
                    HwLocationManagerService.this.refreshSystemUIStatus(navigating);
                    if (!navigating) {
                        HwLocationManagerService.this.sendHwLocationMessage(25, 0);
                    }
                }
            } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                HwLocationManagerService.this.handleWifiStateChanged(intent.getIntExtra("wifi_state", 4));
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                HwLocationManagerService.this.sendHwLocationMessage(24, HwLocationManagerService.WIFI_ALWAYS_SCAN_SCEENONOFF_DELAY);
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                HwLocationManagerService.this.sendHwLocationMessage(23, HwLocationManagerService.WIFI_ALWAYS_SCAN_SCEENONOFF_DELAY);
            }
        }
    };
    private HwGeoFencerBase mGeoFencer;
    private final Context mHwContext;
    private HwGpsPowerTracker mHwGpsPowerTracker;
    private boolean mIs5GHzBandSupported = false;
    private boolean mLBSServiceStart = false;
    private LBSServiceDeathHandler mLBSServicedeathHandler;
    private Location mLastLocation = null;
    private Handler mLbsServiceHandler;
    private HwLocalLocationProvider mLocalLocationProvider;
    private LocationManagerServiceUtil mLocationManagerServiceUtil;
    private BroadcastReceiver mPackegeClearReceiver = new BroadcastReceiver() {
        private String getPackageName(Intent intent) {
            Uri uri = intent.getData();
            return uri != null ? uri.getSchemeSpecificPart() : null;
        }

        private boolean explicitlyStopped(String pkg) {
            PackageManager pm = HwLocationManagerService.this.mContext.getPackageManager();
            if (pm == null) {
                return false;
            }
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                if (ai == null || (ai.flags & HighBitsCompModeID.MODE_EYE_PROTECT) == 0) {
                    return false;
                }
                return true;
            } catch (NameNotFoundException e) {
                String str = HwLocationManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("package info not found:");
                stringBuilder.append(pkg);
                Log.w(str, stringBuilder.toString());
                return false;
            }
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.intent.action.PACKAGE_DATA_CLEARED".equals(action) || "android.intent.action.PACKAGE_RESTARTED".equals(action)) {
                    String pkg = getPackageName(intent);
                    if (explicitlyStopped(pkg)) {
                        HwLocationManagerService.this.removeStoppedRecords(pkg);
                    }
                }
            }
        }
    };
    HashMap<String, ArrayList<UpdateRecord>> mPreservedRecordsByPkg = new HashMap();
    private long mServiceRestartCount;
    private long mStartServiceTime;
    HashMap<String, String> mSupervisoryPkgList = new HashMap();

    private class LBSServiceDeathHandler implements ServiceConnection, DeathRecipient {
        private LBSServiceDeathHandler() {
        }

        /* synthetic */ LBSServiceDeathHandler(HwLocationManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d(HwLocationManagerService.TAG, "bindLbsService Connect lbs service successful");
            HwLocationManagerService.this.mBinderLBSService = service;
            HwLocationManagerService.this.mLBSServiceStart = true;
            HwLocationManagerService.this.notifyServiceDied();
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.d(HwLocationManagerService.TAG, "bindLbsService disconnect lbs service");
            HwLocationManagerService.this.mBinderLBSService = null;
            HwLocationManagerService.this.mLBSServiceStart = false;
        }

        public void binderDied() {
            Log.d(HwLocationManagerService.TAG, "bindLbsService lbs service has died!");
            if (HwLocationManagerService.this.mBinderLBSService != null) {
                HwLocationManagerService.this.mBinderLBSService.unlinkToDeath(HwLocationManagerService.this.mLBSServicedeathHandler, 0);
                HwLocationManagerService.this.mBinderLBSService = null;
            }
            if (System.currentTimeMillis() - HwLocationManagerService.this.mStartServiceTime > 600000) {
                HwLocationManagerService.this.mServiceRestartCount = 0;
            }
            HwLocationManagerService.this.sendLbsServiceRestartMessage();
        }
    }

    static {
        mSupervisoryControlWhiteList.add("com.sankuai.meituan.dispatch.homebrew");
        mSupervisoryControlWhiteList.add("com.huawei.hidisk");
        mSupervisoryControlWhiteList.add("com.nianticlabs.pokemongo");
    }

    public HwLocationManagerService(Context context) {
        super(context);
        this.mLocationManagerServiceUtil = LocationManagerServiceUtil.getDefault(this, context);
        this.mHwContext = context;
        this.mGeoFencerEnabled = false;
        this.mIs5GHzBandSupported = isDualBandSupported();
        registerPkgClearReceiver();
    }

    private void refreshSystemUIStatus(boolean isRefreshMonitor) {
        synchronized (this.mLocationManagerServiceUtil.getmLock()) {
            for (Receiver receiver : this.mLocationManagerServiceUtil.getReceivers().values()) {
                receiver.updateMonitoring(isRefreshMonitor);
            }
        }
    }

    private void checkWifiScanAlwaysResetFlag() {
        if (Global.getInt(this.mHwContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE_RESET_FLAG, 0) == 1) {
            Log.d(TAG, " the phone is boot before the wlan alwas scan reset to open.");
            sendHwLocationMessage(22, 0);
        }
    }

    private void sendHwLocationMessage(int what, int delay) {
        Handler locationHandler = this.mLocationManagerServiceUtil.getLocationHandler();
        locationHandler.removeMessages(what);
        Message m = Message.obtain(locationHandler, what);
        if (delay > 0) {
            locationHandler.sendMessageDelayed(m, (long) delay);
        } else {
            locationHandler.sendMessage(m);
        }
    }

    private void handleWifiStateChanged(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wifistate :");
        stringBuilder.append(state);
        Log.d(str, stringBuilder.toString());
        if (state == 1) {
            int always_wifi_scan = Global.getInt(this.mHwContext.getContentResolver(), "wifi_scan_always_enabled", 0);
            int userAction = Global.getInt(this.mHwContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE_USER_OPR, 0);
            if ((Secure.getInt(this.mHwContext.getContentResolver(), "device_provisioned", 0) != 0) && this.isAlwaysScanReset && always_wifi_scan == 1 && userAction != 1) {
                sendHwLocationMessage(21, 0);
            }
            this.isAlwaysScanReset = false;
        } else if (state == 3) {
            this.isAlwaysScanReset = true;
        }
    }

    /* JADX WARNING: Missing block: B:23:0x010d, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean hwLocationHandleMessage(Message msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwLocationHandleMessage :");
        stringBuilder.append(msg.what);
        stringBuilder.append(", sceenoff:");
        stringBuilder.append(this.isAlwaysScanScreenOff);
        Log.d(str, stringBuilder.toString());
        int i = msg.what;
        if (i != 7) {
            switch (i) {
                case 21:
                    Global.putInt(this.mHwContext.getContentResolver(), "wifi_scan_always_enabled", 0);
                    Global.putInt(this.mHwContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE_RESET_FLAG, 1);
                    sendHwLocationMessage(22, 30000);
                    break;
                case 22:
                    Global.putInt(this.mHwContext.getContentResolver(), "wifi_scan_always_enabled", 1);
                    Global.putInt(this.mHwContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE_RESET_FLAG, 0);
                    break;
                case 23:
                    i = Global.getInt(this.mHwContext.getContentResolver(), "wifi_scan_always_enabled", 0);
                    int userAction = Global.getInt(this.mHwContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE_USER_OPR, 0);
                    if ((Secure.getInt(this.mHwContext.getContentResolver(), "device_provisioned", 0) != 0) && i == 1 && userAction != 1) {
                        this.isAlwaysScanScreenOff = true;
                        Global.putInt(this.mHwContext.getContentResolver(), "wifi_scan_always_enabled", 0);
                        Global.putInt(this.mHwContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE_RESET_FLAG, 1);
                        break;
                    }
                case 24:
                    if (this.isAlwaysScanScreenOff) {
                        this.isAlwaysScanScreenOff = false;
                        Global.putInt(this.mHwContext.getContentResolver(), "wifi_scan_always_enabled", 1);
                        Global.putInt(this.mHwContext.getContentResolver(), WIFI_SCAN_ALWAYS_AVAILABLE_RESET_FLAG, 0);
                        break;
                    }
                    break;
                case 25:
                    if (Global.getInt(this.mHwContext.getContentResolver(), GNSS_LOCATION_FIX_STATUS, 0) == 1) {
                        Log.d(TAG, "set gnss_location_fix_status to 0");
                        Global.putInt(this.mHwContext.getContentResolver(), GNSS_LOCATION_FIX_STATUS, 0);
                    }
                    readySaveLocationToDataBase();
                    break;
                case 26:
                    Log.d(TAG, "bindLbsService start service message has come.");
                    startLBSService();
                    bindLBSService();
                    break;
                case 27:
                    saveLocationToDataBase();
                    break;
            }
        }
        hwLocationCheck();
        return false;
    }

    protected void enableLocalLocationProviders(GnssLocationProvider gnssProvider) {
        if (gnssProvider == null || !gnssProvider.isLocalDBEnabled()) {
            Log.e(TAG, "localDB is disabled");
            return;
        }
        Log.e(TAG, "init and enable localLocationProvider ");
        this.mLocalLocationProvider = HwLocalLocationProvider.getInstance(this.mLocationManagerServiceUtil.getContext(), this);
        this.mLocalLocationProvider.enable();
        this.mLocationManagerServiceUtil.addProviderLocked(this.mLocalLocationProvider);
        this.mLocationManagerServiceUtil.getRealProviders().put(HwLocalLocationManager.LOCAL_PROVIDER, this.mLocalLocationProvider);
        this.mLocationManagerServiceUtil.getEnabledProviders().add(this.mLocalLocationProvider.getName());
    }

    protected void updateLocalLocationDB(Location location, String provider) {
        if (location != null) {
            if (!provider.equals("passive")) {
                try {
                    String encryptedLong = MASTER_PASSWORD;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(location.getLongitude());
                    stringBuilder.append("");
                    encryptedLong = AESLocalDbCrypto.encrypt(encryptedLong, stringBuilder.toString());
                    String encryptedLat = MASTER_PASSWORD;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(location.getLatitude());
                    stringBuilder2.append("");
                    encryptedLat = AESLocalDbCrypto.encrypt(encryptedLat, stringBuilder2.toString());
                    String str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("result loc: ");
                    stringBuilder3.append(encryptedLong);
                    stringBuilder3.append(", ");
                    stringBuilder3.append(encryptedLat);
                    Log.d(str, stringBuilder3.toString());
                } catch (Exception e) {
                    Log.e(TAG, "print loc Exception");
                }
            }
            if (this.mLocalLocationProvider == null || this.mLocalLocationProvider.isValidLocation(location)) {
                if (!(provider.equals("passive") || provider.equals(HwLocalLocationManager.LOCAL_PROVIDER) || this.mLocalLocationProvider == null || !this.mLocalLocationProvider.isEnabled())) {
                    this.mLocalLocationProvider.updataLocationDB(location);
                }
                updateLastLocationDataBase(location, provider);
                return;
            }
            Log.d(TAG, "incoming location is invdlid,and not report app");
        }
    }

    public void initHwLocationPowerTracker(Context context) {
        this.mHwGpsPowerTracker = new HwGpsPowerTracker(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.location.GPS_ENABLED_CHANGE");
        if (this.WIFISCAN_CONTROL_ON) {
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            intentFilter.addCategory("android.net.wifi.WIFI_STATE_CHANGED@hwBrExpand@WifiStatus=WIFIENABLED|WifiStatus=WIFIDISABLED");
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
        }
        this.mHwContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, intentFilter, null, this.mLocationManagerServiceUtil.getLocationHandler());
        this.mLBSServicedeathHandler = new LBSServiceDeathHandler(this, null);
        this.mLbsServiceHandler = this.mLocationManagerServiceUtil.getLocationHandler();
        checkWifiScanAlwaysResetFlag();
    }

    public void hwLocationPowerTrackerRecordRequest(String pkgName, int quality, boolean isIntent) {
        this.mHwGpsPowerTracker.recordRequest(pkgName, quality, isIntent);
    }

    public void hwLocationPowerTrackerRemoveRequest(String pkgName) {
        this.mHwGpsPowerTracker.removeRequest(pkgName);
    }

    public void hwLocationPowerTrackerDump(PrintWriter pw) {
        this.mHwGpsPowerTracker.dump(pw);
    }

    public void hwQuickGpsSwitch() {
        if ("hi110x".equalsIgnoreCase(SystemProperties.get("ro.connectivity.chiptype", ""))) {
            this.mLocationManagerServiceUtil.getContext().getContentResolver().registerContentObserver(Global.getUriFor("quick_gps_switch"), true, new ContentObserver(this.mLocationManagerServiceUtil.getLocationHandler()) {
                public void onChange(boolean selfChange) {
                    int quickGpsSettings = Global.getInt(HwLocationManagerService.this.mLocationManagerServiceUtil.getContext().getContentResolver(), "quick_gps_switch", 1);
                    SystemProperties.set("persist.sys.pgps.config", Integer.toString(quickGpsSettings));
                    String str = HwLocationManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Settings.Global.QUICK_GPS_SWITCH  set ");
                    stringBuilder.append(quickGpsSettings);
                    Log.d(str, stringBuilder.toString());
                }
            }, -1);
        }
    }

    public void hwSendLocationChangedAction(Context context, String packageName) {
        Intent intent = new Intent("android.location.LOCATION_REQUEST_CHANGE_ACTION");
        intent.putExtra("package", packageName);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void checkGeoFencerEnabled(PackageManager packageManager) {
        this.mGeoFencerPackageName = Resources.getSystem().getString(33685939);
        if (this.mGeoFencerPackageName == null || packageManager.resolveService(new Intent(this.mGeoFencerPackageName), 0) == null) {
            this.mGeoFencer = null;
            this.mGeoFencerEnabled = false;
        } else {
            this.mGeoFencer = HwGeoFencerProxy.getGeoFencerProxy(this.mLocationManagerServiceUtil.getContext(), this.mGeoFencerPackageName);
            this.mGeoFencerEnabled = true;
        }
        this.mComboNlpPackageName = Resources.getSystem().getString(33685940);
        if (this.mComboNlpPackageName != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mComboNlpPackageName);
            stringBuilder.append(".nlp:ready");
            this.mComboNlpReadyMarker = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mComboNlpPackageName);
            stringBuilder.append(".nlp:screen");
            this.mComboNlpScreenMarker = stringBuilder.toString();
        }
    }

    public boolean addQcmGeoFencer(Geofence geofence, LocationRequest sanitizedRequest, int uid, PendingIntent intent, String packageName) {
        if (this.mGeoFencer == null || !this.mGeoFencerEnabled) {
            return false;
        }
        long j;
        if (sanitizedRequest.getExpireAt() == Long.MAX_VALUE) {
            j = -1;
        } else {
            j = sanitizedRequest.getExpireAt() - SystemClock.elapsedRealtime();
        }
        this.mGeoFencer.add(new GeoFenceParams(uid, geofence.getLatitude(), geofence.getLongitude(), geofence.getRadius(), j, intent, packageName));
        return true;
    }

    public boolean removeQcmGeoFencer(PendingIntent intent) {
        if (this.mGeoFencer == null || !this.mGeoFencerEnabled) {
            return false;
        }
        this.mGeoFencer.remove(intent);
        return true;
    }

    private void setGnssLocationFixStatus(Location location, String provider) {
        int location_fix_status = Global.getInt(this.mHwContext.getContentResolver(), GNSS_LOCATION_FIX_STATUS, 0);
        Bundle extras = location.getExtras();
        if ((extras == null || !extras.getBoolean("QUICKGPS")) && "gps".equals(provider)) {
            if (location_fix_status == 0) {
                Log.d(TAG, "set gnss_location_fix_status to 1");
                Global.putInt(this.mHwContext.getContentResolver(), GNSS_LOCATION_FIX_STATUS, 1);
            }
            sendHwLocationMessage(25, 10000);
        }
    }

    protected Location screenLocationLocked(Location location, String provider) {
        setGnssLocationFixStatus(location, provider);
        if (this.mLocationManagerServiceUtil.isMockProvider("network")) {
            return location;
        }
        LocationProviderProxy providerProxy = (LocationProviderProxy) this.mLocationManagerServiceUtil.getProvidersByName().get("network");
        if (this.mComboNlpPackageName == null || providerProxy == null || !provider.equals("network") || this.mLocationManagerServiceUtil.isMockProvider("network")) {
            return location;
        }
        String connectedNlpPackage = providerProxy.getConnectedPackageName();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("connectedNlpPackage ");
        stringBuilder.append(connectedNlpPackage);
        Log.d(str, stringBuilder.toString());
        if (connectedNlpPackage == null) {
            return location;
        }
        String[] pNames = connectedNlpPackage.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
        if (pNames.length == 2) {
            if (!(pNames[0].equals(this.mComboNlpPackageName) || pNames[1].equals(this.mComboNlpPackageName))) {
                return location;
            }
        } else if (!connectedNlpPackage.equals(this.mComboNlpPackageName)) {
            return location;
        }
        Bundle extras = location.getExtras();
        boolean isBeingScreened = false;
        if (extras == null) {
            extras = new Bundle();
        }
        if (extras.containsKey(this.mComboNlpReadyMarker)) {
            if (D) {
                Log.d(TAG, "This location is marked as ready for broadcast");
            }
            extras.remove(this.mComboNlpReadyMarker);
        } else {
            ArrayList<UpdateRecord> records = (ArrayList) this.mLocationManagerServiceUtil.getRecordsByProvider().get("passive");
            if (records != null) {
                Iterator it = records.iterator();
                while (it.hasNext()) {
                    UpdateRecord r = (UpdateRecord) it.next();
                    if (r.mReceiver.mIdentity.mPackageName.equals(this.mComboNlpPackageName)) {
                        if (!isBeingScreened) {
                            isBeingScreened = true;
                            extras.putBoolean(this.mComboNlpScreenMarker, true);
                        }
                        if (!r.mReceiver.callLocationChangedLocked(location)) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("RemoteException calling onLocationChanged on ");
                            stringBuilder2.append(r.mReceiver);
                            Slog.w(str2, stringBuilder2.toString());
                        } else if (D) {
                            Log.d(TAG, "Sending location for screening");
                        }
                    }
                }
            }
            if (isBeingScreened) {
                return null;
            }
            if (D) {
                Log.d(TAG, "Not screening locations");
            }
        }
        return location;
    }

    protected void setGeoFencerEnabled(boolean enabled) {
        if (this.mGeoFencer != null) {
            this.mGeoFencerEnabled = enabled;
        }
    }

    protected void dumpGeoFencer(PrintWriter pw) {
        if (this.mGeoFencer != null && this.mGeoFencerEnabled) {
            this.mGeoFencer.dump(pw, "");
        }
    }

    public boolean proxyGps(String pkg, int uid, boolean proxy) {
        if (proxy) {
            GpsFreezeProc.getInstance().addFreezeProcess(pkg, uid);
        } else {
            GpsFreezeProc.getInstance().removeFreezeProcess(pkg, uid);
        }
        return true;
    }

    protected boolean isFreeze(String pkg) {
        return GpsFreezeProc.getInstance().isFreeze(pkg);
    }

    protected void dumpGpsFreezeProxy(PrintWriter pw) {
        GpsFreezeProc.getInstance().dump(pw);
    }

    public void refreshPackageWhitelist(int type, List<String> pkgList) {
        GpsFreezeProc.getInstance().refreshPackageWhitelist(type, pkgList);
    }

    protected ArraySet<String> getPackageWhiteList(int type) {
        return GpsFreezeProc.getInstance().getPackageWhiteList(type);
    }

    private int addSupervisoryControlProc(String packagename, int uid, int expectedmode) {
        Object obj = packagename;
        int i = uid;
        String mExpectedMode = getMode(expectedmode);
        if (mExpectedMode == null) {
            return -1;
        }
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        if (i <= 1000) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("addSupervisoryControlProc, uid less than 1000, invalid! uid is ");
            stringBuilder.append(i);
            Slog.i(str, stringBuilder.toString());
            return -2;
        } else if (GpsFreezeProc.getInstance().isInPackageWhiteListByType(5, obj)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("addSupervisoryControlProc, packagename:");
            stringBuilder.append(obj);
            stringBuilder.append(" is in PackageWhiteList break");
            Slog.i(str, stringBuilder.toString());
            return -2;
        } else if (mSupervisoryControlWhiteList.contains(obj)) {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("addSupervisoryControlProc, packagename:");
            stringBuilder2.append(obj);
            stringBuilder2.append(" is in local whiteList, return");
            Slog.i(str, stringBuilder2.toString());
            return 0;
        } else {
            synchronized (this.mLocationManagerServiceUtil.getmLock()) {
                this.mSupervisoryPkgList.put(obj, mExpectedMode);
            }
            if (isNetworkAvailable(i) || !mExpectedMode.equals("network")) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("addSupervisoryControlProc, mExpectedMode is ");
                stringBuilder2.append(mExpectedMode);
                stringBuilder2.append(",packagename=");
                stringBuilder2.append(obj);
                stringBuilder2.append(", mSupervisoryPkgList is ");
                stringBuilder2.append(this.mSupervisoryPkgList.toString());
                Slog.d(str, stringBuilder2.toString());
                synchronized (this.mLocationManagerServiceUtil.getmLock()) {
                    HashMap<UpdateRecord, String> mSwitchRecords = new HashMap();
                    HashMap<Object, Receiver> mReceivers = this.mLocationManagerServiceUtil.getReceivers();
                    if (mReceivers != null) {
                        for (Receiver receiver : mReceivers.values()) {
                            String str2;
                            String currentMode = "";
                            if (receiver.mIdentity.mPackageName.equals(obj)) {
                                for (UpdateRecord record : receiver.mUpdateRecords.values()) {
                                    currentMode = record.mProvider;
                                    String mode = getSwitchMode(i, record.mRealRequest.getProvider(), currentMode, mExpectedMode);
                                    if (!mode.equals(currentMode)) {
                                        mSwitchRecords.put(record, mode);
                                        String str3 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("addSupervisoryControlProc, originalMode: ");
                                        stringBuilder.append(record.mRealRequest.getProvider());
                                        stringBuilder.append(", currentMode: ");
                                        stringBuilder.append(currentMode);
                                        stringBuilder.append(", mExpectedMode: ");
                                        stringBuilder.append(mExpectedMode);
                                        stringBuilder.append(", receiver: ");
                                        stringBuilder.append(Integer.toHexString(System.identityHashCode(receiver)));
                                        Slog.d(str3, stringBuilder.toString());
                                    }
                                    str2 = packagename;
                                }
                            }
                            str2 = packagename;
                        }
                    }
                    ArrayList<String> updateProviders = updateLocationMode(mSwitchRecords);
                    if (!(updateProviders == null || updateProviders.isEmpty())) {
                        Handler mHandler = this.mLocationManagerServiceUtil.getLocationHandler();
                        mHandler.sendMessage(Message.obtain(mHandler, 6, updateProviders));
                    }
                }
                return 0;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("addSupervisoryControlProc, packagename=");
            stringBuilder.append(obj);
            stringBuilder.append("expectedmode is network, but Network is not Available");
            Slog.i(str, stringBuilder.toString());
            return -2;
        }
    }

    private void removeSupervisoryControlProc(String packagename, int uid) {
        String str;
        StringBuilder stringBuilder;
        if (uid <= 1000 && uid != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeSupervisoryControlProc, uid less than 1000, invalid! uid is ");
            stringBuilder.append(uid);
            Slog.i(str, stringBuilder.toString());
        } else if (mSupervisoryControlWhiteList.contains(packagename)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeSupervisoryControlProc packagename:");
            stringBuilder.append(packagename);
            stringBuilder.append(" is in local whiteList, return");
            Slog.i(str, stringBuilder.toString());
        } else {
            HashMap<UpdateRecord, String> mSwitchRecords = new HashMap();
            synchronized (this.mLocationManagerServiceUtil.getmLock()) {
                String str2;
                StringBuilder stringBuilder2;
                if (packagename == null && uid == 0) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("remove All SupervisoryControlProc ");
                    stringBuilder2.append(this.mSupervisoryPkgList.toString());
                    Slog.d(str2, stringBuilder2.toString());
                    for (Entry<String, String> entry : this.mSupervisoryPkgList.entrySet()) {
                        mSwitchRecords.putAll(getRemoveSwitchRecords((String) entry.getKey()));
                    }
                    this.mSupervisoryPkgList.clear();
                } else if (packagename != null) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("remove SupervisoryControlProc packagename=");
                    stringBuilder2.append(packagename);
                    Slog.d(str2, stringBuilder2.toString());
                    mSwitchRecords.putAll(getRemoveSwitchRecords(packagename));
                    this.mSupervisoryPkgList.remove(packagename);
                }
                ArrayList<String> updateProviders = updateLocationMode(mSwitchRecords);
                if (updateProviders == null) {
                    updateProviders = new ArrayList();
                }
                ArrayList<UpdateRecord> preservedRecords = (ArrayList) this.mPreservedRecordsByPkg.get(packagename);
                if (preservedRecords != null) {
                    Iterator<UpdateRecord> it = preservedRecords.iterator();
                    while (it.hasNext()) {
                        UpdateRecord record = (UpdateRecord) it.next();
                        if (!record.mReceiver.mUpdateRecords.containsValue(record)) {
                            String provider = record.mRealRequest.getProvider();
                            record.mReceiver.mUpdateRecords.put(provider, record);
                            String str3 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("removeSupervisoryControlProc add origin record ");
                            stringBuilder3.append(provider);
                            Log.i(str3, stringBuilder3.toString());
                            if (!updateProviders.contains(provider)) {
                                updateProviders.add(provider);
                            }
                        }
                    }
                }
                this.mPreservedRecordsByPkg.remove(packagename);
                if (!(updateProviders == null || updateProviders.isEmpty())) {
                    Handler mHandler = this.mLocationManagerServiceUtil.getLocationHandler();
                    mHandler.sendMessage(Message.obtain(mHandler, 6, updateProviders));
                }
            }
        }
    }

    private HashMap<UpdateRecord, String> getRemoveSwitchRecords(String packagename) {
        HashMap<Object, Receiver> mReceivers = this.mLocationManagerServiceUtil.getReceivers();
        HashMap<UpdateRecord, String> mSwitchRecords = new HashMap();
        if (mReceivers != null) {
            for (Receiver receiver : mReceivers.values()) {
                if (receiver.mIdentity.mPackageName.equals(packagename)) {
                    for (UpdateRecord record : receiver.mUpdateRecords.values()) {
                        if (!record.mProvider.equals(record.mRealRequest.getProvider())) {
                            mSwitchRecords.put(record, record.mRealRequest.getProvider());
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("getRemoveSwitchRecords, packagename:");
                            stringBuilder.append(packagename);
                            stringBuilder.append(" originalMode: ");
                            stringBuilder.append(record.mRealRequest.getProvider());
                            stringBuilder.append(", currentMode: ");
                            stringBuilder.append(record.mProvider);
                            stringBuilder.append(", receiver: ");
                            stringBuilder.append(Integer.toHexString(System.identityHashCode(receiver)));
                            Slog.d(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }
        ArrayList<UpdateRecord> preservedRecords = (ArrayList) this.mPreservedRecordsByPkg.get(packagename);
        if (preservedRecords != null) {
            Iterator<UpdateRecord> it = preservedRecords.iterator();
            while (it.hasNext()) {
                UpdateRecord record2 = (UpdateRecord) it.next();
                if (!record2.mProvider.equals(record2.mRealRequest.getProvider())) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getRemoveSwitchRecords record ");
                    stringBuilder2.append(record2);
                    Log.i(str2, stringBuilder2.toString());
                    mSwitchRecords.put(record2, record2.mRealRequest.getProvider());
                    it.remove();
                }
            }
        }
        return mSwitchRecords;
    }

    private ArrayList<String> updateLocationMode(HashMap<UpdateRecord, String> mSwitchRecords) {
        if (mSwitchRecords.size() == 0) {
            Slog.d(TAG, " package has no request send yet");
            return null;
        }
        HashMap<String, ArrayList<UpdateRecord>> recordsByProvider = this.mLocationManagerServiceUtil.getRecordsByProvider();
        ArrayList<String> updateproviders = new ArrayList();
        for (Entry<UpdateRecord, String> entry : mSwitchRecords.entrySet()) {
            String target = (String) entry.getValue();
            String current = ((UpdateRecord) entry.getKey()).mProvider;
            ((UpdateRecord) entry.getKey()).mProvider = target;
            ((ArrayList) recordsByProvider.get(current)).remove(entry.getKey());
            if (((ArrayList) recordsByProvider.get(target)) == null) {
                recordsByProvider.put(target, new ArrayList());
            }
            if (!((ArrayList) recordsByProvider.get(target)).contains(entry.getKey())) {
                ((ArrayList) recordsByProvider.get(target)).add((UpdateRecord) entry.getKey());
            }
            ((UpdateRecord) entry.getKey()).mReceiver.mUpdateRecords.remove(current, entry.getKey());
            Receiver receiver = ((UpdateRecord) entry.getKey()).mReceiver;
            UpdateRecord oldRecord = (UpdateRecord) ((UpdateRecord) entry.getKey()).mReceiver.mUpdateRecords.put(target, (UpdateRecord) entry.getKey());
            if (oldRecord != null) {
                ArrayList<UpdateRecord> oldRecords = (ArrayList) this.mPreservedRecordsByPkg.get(oldRecord.mReceiver.mIdentity.mPackageName);
                if (oldRecords == null) {
                    oldRecords = new ArrayList();
                    this.mPreservedRecordsByPkg.put(oldRecord.mReceiver.mIdentity.mPackageName, oldRecords);
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateLocationMode oldRecord ");
                stringBuilder.append(oldRecord);
                Log.i(str, stringBuilder.toString());
                if (!oldRecords.contains(oldRecord)) {
                    oldRecords.add(oldRecord);
                }
            }
            if (!updateproviders.contains(current)) {
                updateproviders.add(current);
            }
            if (!updateproviders.contains(target)) {
                updateproviders.add(target);
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateLocationMode receive : ");
            stringBuilder2.append(((UpdateRecord) entry.getKey()).mReceiver.toString());
            stringBuilder2.append(" ");
            stringBuilder2.append(current);
            stringBuilder2.append(" -> ");
            stringBuilder2.append(target);
            Slog.d(str2, stringBuilder2.toString());
        }
        return updateproviders;
    }

    /* JADX WARNING: Missing block: B:25:0x0047, code:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String getSwitchMode(int uid, String originalMode, String currentMode, String mExpectedMode) {
        String targetmode = null;
        if (originalMode == null || mExpectedMode == null || currentMode == null || mExpectedMode.equals(currentMode) || "passive".equals(originalMode)) {
            return currentMode;
        }
        if ("gps".equals(mExpectedMode)) {
            targetmode = originalMode;
        }
        if ("network".equals(mExpectedMode) && "gps".equals(currentMode)) {
            targetmode = "network";
        }
        if ("passive".equals(mExpectedMode)) {
            targetmode = "passive";
        }
        if (targetmode == null || !isLocationModeAvailable(targetmode, uid)) {
            return currentMode;
        }
        return targetmode;
    }

    private String getMode(int mode) {
        switch (mode) {
            case 0:
            case 2:
            case 5:
                break;
            case 1:
                return "gps";
            case 3:
                return "network";
            case 4:
                return "passive";
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("add unknow LocationMode, error! expectedmode is ");
                stringBuilder.append(mode);
                Slog.e(str, stringBuilder.toString());
                break;
        }
        return null;
    }

    private boolean isLocationModeAvailable(String locationMode, int uid) {
        if (locationMode == null) {
            return false;
        }
        if ("network".equals(locationMode)) {
            if (this.mLocationManagerServiceUtil.isAllowedByCurrentUserSettingsLocked("network")) {
                return true;
            }
            Slog.i(TAG, "network setting is unavailable.");
            return false;
        } else if (!"gps".equals(locationMode)) {
            return false;
        } else {
            if (this.mLocationManagerServiceUtil.isAllowedByCurrentUserSettingsLocked("gps")) {
                return true;
            }
            Slog.i(TAG, "gps setting is unavailable.");
            return false;
        }
    }

    private void hwRemoveSwitchUpdates(Receiver receiver) {
        synchronized (this.mLocationManagerServiceUtil.getmLock()) {
            ArrayList<UpdateRecord> preservedRecords = (ArrayList) this.mPreservedRecordsByPkg.get(receiver.mIdentity.mPackageName);
            if (preservedRecords != null) {
                Iterator<UpdateRecord> it = preservedRecords.iterator();
                while (it.hasNext()) {
                    UpdateRecord record = (UpdateRecord) it.next();
                    if (record.mReceiver.equals(receiver)) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("hwRemoveUpdatesLocked ");
                        stringBuilder.append(record);
                        Log.i(str, stringBuilder.toString());
                        record.disposeLocked(false);
                        it.remove();
                    }
                }
            }
        }
    }

    private boolean isNetworkAvailable(int uid) {
        int i = uid;
        long identity;
        try {
            if (this.mHwContext == null) {
                Slog.e(TAG, "mHwContext is null error!");
                return false;
            }
            ConnectivityManager connectivity = (ConnectivityManager) this.mHwContext.getSystemService("connectivity");
            if (connectivity == null) {
                Slog.e(TAG, "connectivityManager is null error!");
                return false;
            }
            NetworkInfo infoWifi = connectivity.getNetworkInfo(1);
            NetworkInfo infoMoblie = connectivity.getNetworkInfo(0);
            if (infoWifi == null || infoMoblie == null) {
                Slog.e(TAG, "infoWifi or infoMoblie is null error!");
                return false;
            }
            boolean isWifiConn = infoWifi.isConnected();
            boolean isMobileConn = infoMoblie.isConnected();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("uid is ");
            stringBuilder.append(i);
            stringBuilder.append(" , isWifiConn is ");
            stringBuilder.append(isWifiConn);
            stringBuilder.append(" , isMobileConn is ");
            stringBuilder.append(isMobileConn);
            Slog.d(str, stringBuilder.toString());
            HwNetworkPolicyManager manager = HwNetworkPolicyManager.from(this.mHwContext);
            if (manager == null) {
                Slog.e(TAG, "HwNetworkPolicyManager is null error!");
                return false;
            }
            identity = Binder.clearCallingIdentity();
            int policy = 0;
            policy = manager.getHwUidPolicy(i);
            Binder.restoreCallingIdentity(identity);
            boolean wifiAccess = (policy & 2) == 0;
            boolean mobileAccess = (policy & 1) == 0;
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("policy is ");
            stringBuilder2.append(policy);
            stringBuilder2.append(" , wifiAccess is ");
            stringBuilder2.append(wifiAccess);
            stringBuilder2.append(", mobileAccess is ");
            stringBuilder2.append(mobileAccess);
            Slog.d(str2, stringBuilder2.toString());
            boolean wifiAvaiable = wifiAccess && isWifiConn;
            boolean mobileAvailable = mobileAccess && isMobileConn;
            str2 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("wifiAvaiable is ");
            stringBuilder3.append(wifiAvaiable);
            stringBuilder3.append(" , mobileAvailable is ");
            stringBuilder3.append(mobileAvailable);
            Slog.d(str2, stringBuilder3.toString());
            if (wifiAvaiable || mobileAvailable) {
                return true;
            }
            return false;
        } catch (RuntimeException e) {
            String str3 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("RuntimeException error!");
            stringBuilder4.append(e.toString());
            Slog.e(str3, stringBuilder4.toString());
            return false;
        } catch (Exception e2) {
            Slog.e(TAG, "Exception error!");
            return false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            Throwable th2 = th;
        }
    }

    private boolean enforceAccessPermission(int pid, int uid) {
        if (this.mHwContext.checkPermission("android.permission.ACCESS_FINE_LOCATION", pid, uid) == 0 || this.mHwContext.checkPermission("android.permission.ACCESS_COARSE_LOCATION", pid, uid) == 0) {
            return true;
        }
        return false;
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        int _result;
        int _result2;
        String packagename;
        switch (code) {
            case 1001:
                data.enforceInterface(DESCRIPTOR);
                _result = getPowerTypeByPackageName(data.readString());
                reply.writeNoException();
                reply.writeInt(_result);
                return true;
            case 1002:
                data.enforceInterface(DESCRIPTOR);
                _result2 = logEvent(data.readInt(), data.readInt(), data.readString());
                reply.writeNoException();
                reply.writeInt(_result2);
                return true;
            case 1004:
                data.enforceInterface(DESCRIPTOR);
                packagename = data.readString();
                _result = data.readInt();
                int expectedmode = data.readInt();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("on transact ADD_LOCATION_MODE  uid is ");
                stringBuilder.append(_result);
                stringBuilder.append(" , expectedmode is ");
                stringBuilder.append(expectedmode);
                stringBuilder.append(" , packagename is ");
                stringBuilder.append(packagename);
                Slog.d(str, stringBuilder.toString());
                _result2 = addSupervisoryControlProc(packagename, _result, expectedmode);
                reply.writeNoException();
                reply.writeInt(_result2);
                return true;
            case 1005:
                data.enforceInterface(DESCRIPTOR);
                packagename = data.readString();
                _result = data.readInt();
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("on transact REMOVE_LOCATION_MODE uid is ");
                stringBuilder2.append(_result);
                stringBuilder2.append(" , packagename is ");
                stringBuilder2.append(packagename);
                Slog.d(str2, stringBuilder2.toString());
                removeSupervisoryControlProc(packagename, _result);
                reply.writeNoException();
                return true;
            case 1006:
                data.enforceInterface(DESCRIPTOR);
                _result2 = checkLocationSettings(data.readInt(), data.readString(), data.readString());
                reply.writeNoException();
                reply.writeInt(_result2);
                return true;
            case 1007:
                data.enforceInterface(DESCRIPTOR);
                Slog.d(TAG, "on transact CODE_GNSS_DETECT");
                ArrayList<String> _result3 = gnssDetect(data.readString());
                reply.writeNoException();
                reply.writeStringList(_result3);
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    private int checkLocationSettings(int userId, String name, String value) {
        int resultValue = -1;
        int mUserId = userId;
        if (!isChineseVersion()) {
            return -1;
        }
        String str;
        if ("location_mode".equals(name)) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LOCATION_MODE, name: ");
            stringBuilder.append(name);
            stringBuilder.append(", value: ");
            stringBuilder.append(value);
            Log.d(str, stringBuilder.toString());
            if (LOCATION_MODE_SENSORS_ONLY.equals(value) || LOCATION_MODE_BATTERY_SAVING.equals(value)) {
                resultValue = 3;
                mSettingsByUserId.put(Integer.valueOf(mUserId), Integer.valueOf(2));
            } else if (LOCATION_MODE_OFF.equals(value) || LOCATION_MODE_HIGH_ACCURACY.equals(value)) {
                mSettingsByUserId.put(Integer.valueOf(mUserId), Integer.valueOf(2));
            }
        } else if ("location_providers_allowed".equals(name)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("LOCATION_PROVIDERS_ALLOWED, name: ");
            stringBuilder2.append(name);
            stringBuilder2.append(", value: ");
            stringBuilder2.append(value);
            Log.d(str2, stringBuilder2.toString());
            if (mSettingsByUserId.containsKey(Integer.valueOf(mUserId))) {
                int cnt = ((Integer) mSettingsByUserId.get(Integer.valueOf(mUserId))).intValue();
                if (cnt > 0) {
                    cnt--;
                    if (cnt == 0) {
                        str = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("clear mSettingsByUserId, cnt: ");
                        stringBuilder3.append(cnt);
                        Log.d(str, stringBuilder3.toString());
                        mSettingsByUserId.clear();
                    } else {
                        mSettingsByUserId.put(Integer.valueOf(mUserId), Integer.valueOf(cnt));
                    }
                }
            } else if ("+gps".equals(value) || "+network".equals(value)) {
                resultValue = 3;
                mSettingsByUserId.put(Integer.valueOf(mUserId), Integer.valueOf(2));
            } else if ("-gps".equals(value) || "-network".equals(value)) {
                resultValue = 0;
                mSettingsByUserId.put(Integer.valueOf(mUserId), Integer.valueOf(2));
            }
        }
        return resultValue;
    }

    private boolean isChineseVersion() {
        return "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    }

    public static int qualityToType(int quality) {
        if (quality != 100) {
            if (!(quality == 102 || quality == 104)) {
                if (quality != 203) {
                    switch (quality) {
                        case 200:
                            return 0;
                        case 201:
                            break;
                        default:
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("quality( ");
                            stringBuilder.append(quality);
                            stringBuilder.append(" ) is error !");
                            Slog.d(str, stringBuilder.toString());
                            return -1;
                    }
                }
            }
            return 1;
        }
        return 2;
    }

    public int getPowerTypeByPackageName(String packageName) {
        if (!enforceAccessPermission(Binder.getCallingPid(), Binder.getCallingUid())) {
            return -1;
        }
        int power_type_ret = -1;
        if (TextUtils.isEmpty(packageName)) {
            return -1;
        }
        synchronized (this.mLocationManagerServiceUtil.getmLock()) {
            for (Receiver receiver : this.mLocationManagerServiceUtil.getReceivers().values()) {
                if (packageName.equals(receiver.mIdentity.mPackageName)) {
                    HashMap<String, UpdateRecord> updateRecords = receiver.mUpdateRecords;
                    if (updateRecords != null) {
                        for (UpdateRecord updateRecord : updateRecords.values()) {
                            int power_type = qualityToType(updateRecord.mRequest.getQuality());
                            if (power_type > power_type_ret) {
                                power_type_ret = power_type;
                                if (power_type_ret == 2) {
                                    return power_type_ret;
                                }
                            }
                        }
                        continue;
                    } else {
                        continue;
                    }
                }
            }
            return power_type_ret;
        }
    }

    public int logEvent(int type, int event, String parameter) {
        if (enforceAccessPermission(Binder.getCallingPid(), Binder.getCallingUid()) && !TextUtils.isEmpty(parameter)) {
            return HwGpsLogServices.getInstance(this.mHwContext).logEvent(type, event, parameter);
        }
        return -1;
    }

    protected boolean isGPSDisabled() {
        String allowedProviders = Secure.getStringForUser(this.mContext.getContentResolver(), "location_providers_allowed", ActivityManager.getCurrentUser());
        if (!HwDeviceManager.disallowOp(13)) {
            return false;
        }
        if (allowedProviders.contains("gps")) {
            Slog.i(TAG, "gps provider cannot be enabled, set it to false .");
            Secure.setLocationProviderEnabledForUser(this.mContext.getContentResolver(), "gps", false, ActivityManager.getCurrentUser());
        }
        return true;
    }

    protected String getLocationProvider(int uid, LocationRequest request, String packageName, String provider) {
        String str;
        StringBuilder stringBuilder;
        String targetprovider = provider;
        if ("gps".equals(provider) && GpsFreezeProc.getInstance().isInPackageWhiteListByType(5, packageName)) {
            targetprovider = "network";
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageName:");
            stringBuilder.append(packageName);
            stringBuilder.append(" is change gps provider to ");
            stringBuilder.append(targetprovider);
            Log.d(str, stringBuilder.toString());
        } else if (this.mSupervisoryPkgList.containsKey(packageName)) {
            if (isNetworkAvailable(uid) || !((String) this.mSupervisoryPkgList.get(packageName)).equals("network")) {
                targetprovider = getSwitchMode(uid, provider, provider, (String) this.mSupervisoryPkgList.get(packageName));
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("network is not available, packageName:");
                stringBuilder.append(packageName);
                stringBuilder.append(" need not change provider to network");
                Log.d(str, stringBuilder.toString());
                targetprovider = provider;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageName:");
            stringBuilder.append(packageName);
            stringBuilder.append(" is change ");
            stringBuilder.append(provider);
            stringBuilder.append(" provider to ");
            stringBuilder.append(targetprovider);
            Log.d(str, stringBuilder.toString());
        }
        if ("network".equals(provider) && this.mIs5GHzBandSupported && request.getInterval() < 40000) {
            Log.d(TAG, "5G network min interval need to 40s");
            request.setInterval(40000);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("lbsService mLBSServiceStart:");
        stringBuilder.append(this.mLBSServiceStart);
        Log.d(str, stringBuilder.toString());
        if ("gps".equals(provider) && !this.mLBSServiceStart) {
            Log.d(TAG, " LocationManager. bindLbsService start.");
            startLBSService();
            bindLBSService();
            if (this.mLbsServiceHandler.hasMessages(26)) {
                Log.d(TAG, " mLbsServiceHandler has delay message. bindLbsService start.");
                this.mLbsServiceHandler.removeMessages(26);
                this.mServiceRestartCount = 0;
            }
        }
        return targetprovider;
    }

    private void startLBSService() {
        this.mStartServiceTime = System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mContext.getPackageName());
        stringBuilder.append(" start lbs service. bindLbsService start time:");
        stringBuilder.append(this.mStartServiceTime);
        Log.d(str, stringBuilder.toString());
        try {
            Intent bindIntent = new Intent();
            bindIntent.setClassName(AIDL_MESSAGE_SERVICE_PACKAGE, "com.huawei.lbs.HwLBSService");
            bindIntent.addFlags(268435456);
            this.mContext.startService(bindIntent);
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startLBSService Exception: ");
            stringBuilder2.append(e.getMessage());
            Log.e(str2, stringBuilder2.toString());
        }
    }

    private void bindLBSService() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mContext.getPackageName());
        stringBuilder.append(" bind lbs service. bindLbsService");
        Log.d(str, stringBuilder.toString());
        try {
            Intent bindIntent = new Intent();
            bindIntent.setClassName(AIDL_MESSAGE_SERVICE_PACKAGE, "com.huawei.lbs.HwLBSService");
            this.mContext.bindService(bindIntent, this.mLBSServicedeathHandler, 1);
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("bindLBSService Exception: ");
            stringBuilder2.append(e.getMessage());
            Log.e(str2, stringBuilder2.toString());
        }
    }

    private void notifyServiceDied() {
        try {
            if (this.mBinderLBSService != null) {
                this.mBinderLBSService.linkToDeath(this.mLBSServicedeathHandler, 0);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "IBinder register linkToDeath function fail.");
        }
    }

    private void sendLbsServiceRestartMessage() {
        long delayTime = 0;
        if (!this.mLbsServiceHandler.hasMessages(26)) {
            Message mTimeOut = Message.obtain(this.mLbsServiceHandler, 26);
            delayTime = (long) (60000.0d * Math.pow(2.0d, (double) this.mServiceRestartCount));
            if (delayTime > 0) {
                this.mLbsServiceHandler.sendMessageDelayed(mTimeOut, delayTime);
            } else {
                this.mLbsServiceHandler.sendMessage(mTimeOut);
            }
        }
        if (this.mServiceRestartCount < 3) {
            this.mServiceRestartCount++;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bindLbsService sendLbsServiceRestartMessage mServiceRestartCount: ");
        stringBuilder.append(this.mServiceRestartCount);
        stringBuilder.append(",delayTime:");
        stringBuilder.append(delayTime);
        Log.d(str, stringBuilder.toString());
    }

    protected void updateLastLocationDataBase(Location location, String provider) {
        if (this.mLocalLocationProvider == null || this.mLocalLocationProvider.isValidLocation(location)) {
            try {
                if (this.mLastLocation == null) {
                    this.mLastLocation = readLastLocationDataBase();
                    if (this.mLastLocation == null) {
                        this.mLastLocation = new Location(location);
                        saveLocationToDataBase();
                        return;
                    }
                }
                if (location.getProvider().equals("network") && this.mLastLocation.getProvider().equals("gps") && location.getTime() - this.mLastLocation.getTime() > WifiProCommonUtils.RECHECK_DELAYED_MS) {
                    Log.e(TAG, "New GPS Provider result not be refreshed by Network Provider result");
                    return;
                }
                this.mLastLocation.setLongitude(location.getLongitude());
                this.mLastLocation.setLatitude(location.getLatitude());
                this.mLastLocation.setTime(location.getTime());
                this.mLastLocation.setProvider(location.getProvider());
                this.mLastLocation.setAccuracy(location.getAccuracy());
                return;
            } catch (Exception e) {
                Log.e(TAG, "updateLastLocationDataBase failed", e);
                return;
            }
        }
        Log.e(TAG, "incoming location is invdlid,and not report app");
    }

    protected void readySaveLocationToDataBase() {
        if (this.mLastLocation == null) {
            Log.e(TAG, "mLastLocation is NULL, nothing to saved");
            return;
        }
        long nowTime = System.currentTimeMillis();
        if (0 == this.mLastLocation.getTime() || nowTime - this.mLastLocation.getTime() > WifiProCommonUtils.RECHECK_DELAYED_MS) {
            sendHwLocationMessage(27, 0);
        } else {
            sendHwLocationMessage(27, 600000);
        }
    }

    protected void saveLocationToDataBase() {
        JSONObject jsonObj = new JSONObject();
        String encryptedLat;
        StringBuilder stringBuilder;
        try {
            String encryptedLong = MASTER_PASSWORD;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mLastLocation.getLongitude());
            stringBuilder2.append("");
            encryptedLong = AESLocalDbCrypto.encrypt(encryptedLong, stringBuilder2.toString());
            encryptedLat = MASTER_PASSWORD;
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mLastLocation.getLatitude());
            stringBuilder.append("");
            encryptedLat = AESLocalDbCrypto.encrypt(encryptedLat, stringBuilder.toString());
            jsonObj.put(LONGITUDE, encryptedLong);
            jsonObj.put(LATITUDE, encryptedLat);
            jsonObj.put(ACCURACY, (double) this.mLastLocation.getAccuracy());
            jsonObj.put(TIMESTAMP, this.mLastLocation.getTime());
            jsonObj.put(SOURCE, this.mLastLocation.getProvider());
        } catch (JSONException e) {
            Log.e(TAG, "updateLastLocationDataBase json error!");
            return;
        } catch (Exception e2) {
            encryptedLat = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("saveLocationToDataBase Exception :");
            stringBuilder.append(e2.getMessage());
            Log.e(encryptedLat, stringBuilder.toString());
        }
        Global.putString(this.mHwContext.getContentResolver(), SAVED_LOCATION, jsonObj.toString());
    }

    protected Location readLastLocationDataBase() {
        try {
            String locationStr = Global.getString(this.mHwContext.getContentResolver(), SAVED_LOCATION);
            if (locationStr == null) {
                Log.e(TAG, "readLastLocationDataBase locationStr null");
                return null;
            }
            JSONObject jsonObj = new JSONObject(locationStr);
            String encryptedLong = jsonObj.getString(LONGITUDE);
            String decryptedLat = AESLocalDbCrypto.decrypt(MASTER_PASSWORD, jsonObj.getString(LATITUDE));
            double longitudeSaved = Double.parseDouble(AESLocalDbCrypto.decrypt(MASTER_PASSWORD, encryptedLong));
            double latitudeSaved = Double.parseDouble(decryptedLat);
            String providerSeaved = jsonObj.getString(SOURCE);
            long timestampSaved = jsonObj.getLong(TIMESTAMP);
            float accuricy = (float) jsonObj.getLong(TIMESTAMP);
            float f;
            if (0.0d == longitudeSaved) {
                f = accuricy;
            } else if (0.0d == latitudeSaved) {
                String str = locationStr;
                f = accuricy;
            } else {
                Location location = new Location("gps");
                location.setLongitude(longitudeSaved);
                location.setLatitude(latitudeSaved);
                location.setTime(timestampSaved);
                location.setProvider(providerSeaved);
                location.setAccuracy(accuricy);
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("readLastLocationDataBase");
                stringBuilder.append(location);
                Log.i(str2, stringBuilder.toString());
                return location;
            }
            Log.e(TAG, "No record in Database");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "readLastLocationDataBase failed", e);
            return null;
        }
    }

    protected boolean isExceptionAppForUserSettingsLocked(String packageName) {
        if (packageName == null) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isExceptionAppForUserSettingsLocked packageName:");
        stringBuilder.append(packageName);
        Log.e(str, stringBuilder.toString());
        if ("com.hisi.mapcon".equals(packageName)) {
            return true;
        }
        return false;
    }

    private boolean isDualBandSupported() {
        return this.mContext.getResources().getBoolean(17957073);
    }

    protected void hwSendBehavior(BehaviorId bid) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(Binder.getCallingUid(), Binder.getCallingPid(), bid);
        }
    }

    public void hwRequestLocationUpdatesLocked(LocationRequest request, Receiver receiver, int pid, int uid, String packageName) {
        HwGnssDetectManager.getInstance(this.mHwContext).hwRequestLocationUpdatesLocked(request, receiver, pid, uid, packageName);
        Handler locationHandler = this.mLocationManagerServiceUtil.getLocationHandler();
        if (!locationHandler.hasMessages(7)) {
            locationHandler.sendEmptyMessageDelayed(7, HwArbitrationDEFS.DelayTimeMillisB);
        }
    }

    public ArrayList<String> gnssDetect(String packageName) {
        ArrayList<String> gnssDetect;
        synchronized (this.mLocationManagerServiceUtil.getmLock()) {
            gnssDetect = HwGnssDetectManager.getInstance(this.mHwContext).gnssDetect(packageName);
        }
        return gnssDetect;
    }

    public void hwRemoveUpdatesLocked(Receiver receiver) {
        hwRemoveSwitchUpdates(receiver);
    }

    private void registerPkgClearReceiver() {
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        packageFilter.addAction("android.intent.action.PACKAGE_RESTARTED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this.mPackegeClearReceiver, UserHandle.OWNER, packageFilter, null, null);
    }

    private void hwLocationCheck() {
        removeNotRunAndNotExistRecords();
        hwLockCheck();
    }

    /* JADX WARNING: Missing block: B:38:0x011e, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void removeNotRunAndNotExistRecords() {
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        synchronized (this.mLocationManagerServiceUtil.getmLock()) {
            HashMap<String, ArrayList<UpdateRecord>> recordsByProvider = this.mLocationManagerServiceUtil.getRecordsByProvider();
            if (recordsByProvider == null || recordsByProvider.isEmpty()) {
            } else if (appProcessList == null) {
                Log.w(TAG, "no Process find");
            } else {
                HashMap<Object, Receiver> receivers = (HashMap) this.mLocationManagerServiceUtil.getReceivers().clone();
                for (Entry<String, ArrayList<UpdateRecord>> entry : recordsByProvider.entrySet()) {
                    ArrayList<UpdateRecord> records = (ArrayList) entry.getValue();
                    if (records != null) {
                        Iterator it = ((ArrayList) records.clone()).iterator();
                        while (it.hasNext()) {
                            UpdateRecord record = (UpdateRecord) it.next();
                            if (receivers.containsValue(record.mReceiver)) {
                                boolean isfound = false;
                                for (RunningAppProcessInfo appProcess : appProcessList) {
                                    if (appProcess.pid == record.mReceiver.mIdentity.mPid && appProcess.uid == record.mReceiver.mIdentity.mUid) {
                                        isfound = true;
                                        break;
                                    }
                                }
                                if (!isfound) {
                                    this.mLocationManagerServiceUtil.removeUpdatesLocked(record.mReceiver);
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("process may be died, but request not remove!  pid = ");
                                    stringBuilder.append(record.mReceiver.mIdentity.mPid);
                                    stringBuilder.append(" uid = ");
                                    stringBuilder.append(record.mReceiver.mIdentity.mUid);
                                    stringBuilder.append(" receiver = ");
                                    stringBuilder.append(record.mReceiver.toString());
                                    Log.w(str, stringBuilder.toString());
                                }
                            } else {
                                record.disposeLocked(false);
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("receiver not exists, but updateRecord not remove!  pid = ");
                                stringBuilder2.append(record.mReceiver.mIdentity.mPid);
                                stringBuilder2.append(" uid = ");
                                stringBuilder2.append(record.mReceiver.mIdentity.mUid);
                                stringBuilder2.append(" UpdateRecord = ");
                                stringBuilder2.append(record);
                                Log.w(str2, stringBuilder2.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    private void hwLockCheck() {
        ArrayList<UpdateRecord> longLockRecords = checkLongLock();
        clearLongLock(longLockRecords);
        callClientHandle(longLockRecords);
        finalCheck();
    }

    private ArrayList<UpdateRecord> checkLongLock() {
        ArrayList<UpdateRecord> longLockRecords = new ArrayList();
        synchronized (this.mLocationManagerServiceUtil.getmLock()) {
            HashMap<String, ArrayList<UpdateRecord>> recordsByProvider = this.mLocationManagerServiceUtil.getRecordsByProvider();
            long currentTime = SystemClock.elapsedRealtime();
            for (Entry<String, ArrayList<UpdateRecord>> entry : recordsByProvider.entrySet()) {
                Iterator it = ((ArrayList) entry.getValue()).iterator();
                while (it.hasNext()) {
                    long currentTime2;
                    UpdateRecord record = (UpdateRecord) it.next();
                    long acquireLockTime = record.mReceiver.mAcquireLockTime;
                    if (acquireLockTime > record.mReceiver.mReleaseLockTime) {
                        currentTime2 = currentTime;
                        if (currentTime - acquireLockTime >= this.LOCK_HELD_BAD_TIME) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("hold lock for too long time ");
                            stringBuilder.append(record);
                            stringBuilder.append(" receiver ");
                            stringBuilder.append(Integer.toHexString(System.identityHashCode(record.mReceiver)));
                            Log.i(str, stringBuilder.toString());
                            longLockRecords.add(record);
                        }
                    } else {
                        currentTime2 = currentTime;
                    }
                    currentTime = currentTime2;
                }
            }
        }
        return longLockRecords;
    }

    private void clearLongLock(ArrayList<UpdateRecord> longLockRecords) {
        if (longLockRecords != null && !longLockRecords.isEmpty()) {
            int listSize = longLockRecords.size();
            for (int i = 0; i < listSize; i++) {
                UpdateRecord record = (UpdateRecord) longLockRecords.get(i);
                synchronized (record.mReceiver) {
                    record.mReceiver.clearPendingBroadcastsLocked();
                }
            }
        }
    }

    private void callClientHandle(ArrayList<UpdateRecord> badRecords) {
        if (badRecords != null && !badRecords.isEmpty()) {
            synchronized (this.mLocationManagerServiceUtil.getmLock()) {
                Iterator it = badRecords.iterator();
                while (it.hasNext()) {
                    UpdateRecord record = (UpdateRecord) it.next();
                    if (record.mReceiver.mListener != null) {
                        record.mReceiver.callLocationChangedLocked(new Location("DEAD"));
                    }
                }
            }
        }
    }

    private void finalCheck() {
        boolean needCheckAgain;
        int totalPendingBroadcasts = 0;
        synchronized (this.mLocationManagerServiceUtil.getmLock()) {
            HashMap<String, ArrayList<UpdateRecord>> recordsByProvider = this.mLocationManagerServiceUtil.getRecordsByProvider();
            for (Entry<String, ArrayList<UpdateRecord>> entry : recordsByProvider.entrySet()) {
                Iterator it = ((ArrayList) entry.getValue()).iterator();
                while (it.hasNext()) {
                    totalPendingBroadcasts += ((UpdateRecord) it.next()).mReceiver.mPendingBroadcasts;
                }
            }
            ArrayList<UpdateRecord> gpsRecords = (ArrayList) recordsByProvider.get("gps");
            boolean z = (totalPendingBroadcasts <= 0 || gpsRecords == null || gpsRecords.isEmpty()) ? false : true;
            needCheckAgain = z;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwCheckLock finalCheck ");
        stringBuilder.append(totalPendingBroadcasts);
        stringBuilder.append(" needCheckAgain ");
        stringBuilder.append(needCheckAgain);
        Log.i(str, stringBuilder.toString());
        if (needCheckAgain) {
            Handler locationHandler = this.mLocationManagerServiceUtil.getLocationHandler();
            if (!locationHandler.hasMessages(7)) {
                locationHandler.sendEmptyMessageDelayed(7, HwArbitrationDEFS.DelayTimeMillisB);
            }
        }
    }

    private void removeStoppedRecords(String pkgName) {
        if (pkgName != null) {
            boolean isPkgHasRecord = false;
            synchronized (this.mLocationManagerServiceUtil.getmLock()) {
                for (Entry<String, ArrayList<UpdateRecord>> entry : this.mLocationManagerServiceUtil.getRecordsByProvider().entrySet()) {
                    ArrayList<UpdateRecord> records = (ArrayList) entry.getValue();
                    if (records != null) {
                        Iterator it = ((ArrayList) records.clone()).iterator();
                        while (it.hasNext()) {
                            if (pkgName.equals(((UpdateRecord) it.next()).mReceiver.mIdentity.mPackageName)) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("package stopped, remove updateRecords and receivers: ");
                                stringBuilder.append(pkgName);
                                Log.i(str, stringBuilder.toString());
                                isPkgHasRecord = true;
                                break;
                            }
                        }
                        if (isPkgHasRecord) {
                            break;
                        }
                    }
                }
            }
            if (isPkgHasRecord) {
                removeNotRunAndNotExistRecords();
            }
        }
    }
}
