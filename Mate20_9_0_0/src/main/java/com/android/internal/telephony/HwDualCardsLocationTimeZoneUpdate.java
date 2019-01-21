package com.android.internal.telephony;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.TimeServiceHelper.Listener;
import com.android.internal.telephony.latlongtotimezone.TimezoneMapper;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimUtils;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import java.util.List;
import java.util.TimeZone;
import libcore.util.TimeZoneFinder;

public class HwDualCardsLocationTimeZoneUpdate extends Handler {
    private static final int AUTO_TIME_ZONE_OFF = 0;
    private static final int EVENT_LOCATION_CHANGED = 100;
    private static final int EVENT_RPLMNS_STATE_CHANGED = 1;
    private static final int INVAILD_SUBID = -1;
    private static final String LOG_TAG = "HwDualCardsLocTZUpdate";
    private static final int MCC_LEN = 3;
    private static final int NUMERIC_MIN_LEN = 5;
    private static final int PHONE_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final String PROPERTY_GLOBAL_OPERATOR_NUMERIC = "ril.operator.numeric";
    private static final long REQUEST_CURRENT_LOCATION = 1800000;
    private static final int SINGLE_CARD_PHONE = 1;
    private static final int SINGLE_TIME_ZONE_COUNTRY = 1;
    private static boolean isAllowUpdateOnce = false;
    private static HwDualCardsLocationTimeZoneUpdate mInstance = null;
    private boolean hasRegTzLocUpdater;
    private boolean hasUpdateTzByLoc;
    private ConnectivityManager mCM;
    private Context mContext;
    private ContentResolver mCr = null;
    private Location mCurrentLocation = null;
    private HwLocationUpdateManager mHwLocationUpdateManager;
    private long mLastGetLocTime;
    private NetworkStateUpdateCallback mNetworkStateUpdateCallback;
    private NitzStateMachine mNitzState;
    private BroadcastReceiver mReceiver;
    private BroadcastReceiver mScreenOnReceiver;
    private TimeServiceHelper mTimeServiceHelper;
    private String numericSub1 = "";
    private String numericSub2 = "";

    private class NetworkStateUpdateCallback extends NetworkCallback {
        private NetworkStateUpdateCallback() {
        }

        /* synthetic */ NetworkStateUpdateCallback(HwDualCardsLocationTimeZoneUpdate x0, AnonymousClass1 x1) {
            this();
        }

        public void onAvailable(Network network) {
            if (HwDualCardsLocationTimeZoneUpdate.this.mCurrentLocation == null) {
                HwDualCardsLocationTimeZoneUpdate.this.log("Get location when ps attach is have not got location before.");
                HwDualCardsLocationTimeZoneUpdate.this.mHwLocationUpdateManager.requestLocationUpdate(true);
            }
        }
    }

    private HwDualCardsLocationTimeZoneUpdate(Context context) {
        ServiceStateTracker serviceStateTracker = null;
        int index = 0;
        this.hasUpdateTzByLoc = false;
        this.hasRegTzLocUpdater = false;
        this.mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if ("android.intent.action.SERVICE_STATE".equals(intent.getAction())) {
                        int slotId = intent.getIntExtra("subscription", -1);
                        HwDualCardsLocationTimeZoneUpdate hwDualCardsLocationTimeZoneUpdate = HwDualCardsLocationTimeZoneUpdate.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ACTION_SERVICE_STATE_CHANGED.");
                        stringBuilder.append(slotId);
                        hwDualCardsLocationTimeZoneUpdate.log(stringBuilder.toString());
                        Phone phone = PhoneFactory.getPhone(slotId);
                        ServiceState serviceState = phone != null ? phone.getServiceState() : null;
                        if (serviceState != null) {
                            String numeric = serviceState.getOperatorNumeric();
                            HwDualCardsLocationTimeZoneUpdate hwDualCardsLocationTimeZoneUpdate2 = HwDualCardsLocationTimeZoneUpdate.this;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("numeric:");
                            stringBuilder2.append(numeric);
                            hwDualCardsLocationTimeZoneUpdate2.log(stringBuilder2.toString());
                            HwDualCardsLocationTimeZoneUpdate.this.updatePlmn(numeric, slotId);
                        }
                        if (HwDualCardsLocationTimeZoneUpdate.this.isDualCardsIsoNotEquals()) {
                            HwDualCardsLocationTimeZoneUpdate.this.registerTimeZoneLocationUpdater();
                        } else {
                            HwDualCardsLocationTimeZoneUpdate.this.unregisterTimeZoneLocationUpdater();
                        }
                    }
                }
            }
        };
        this.mScreenOnReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                        long lastUpdateTimeZoneSpace = SystemClock.elapsedRealtime() - HwDualCardsLocationTimeZoneUpdate.this.mLastGetLocTime;
                        if (HwDualCardsLocationTimeZoneUpdate.this.mCurrentLocation == null || lastUpdateTimeZoneSpace > HwDualCardsLocationTimeZoneUpdate.REQUEST_CURRENT_LOCATION) {
                            if (HwDualCardsLocationTimeZoneUpdate.this.mCurrentLocation == null) {
                                HwDualCardsLocationTimeZoneUpdate.this.log("request location: mCurrentLocation is null");
                            } else {
                                HwDualCardsLocationTimeZoneUpdate.this.log("request location: last get location space is over 0.5H");
                            }
                            HwDualCardsLocationTimeZoneUpdate.this.mHwLocationUpdateManager.requestLocationUpdate(false);
                        } else {
                            HwDualCardsLocationTimeZoneUpdate.this.log("There is no need to request location.");
                        }
                    }
                }
            }
        };
        this.mContext = context;
        if (PHONE_NUM == 1) {
            log("this is a single card cell phone.");
            return;
        }
        this.mHwLocationUpdateManager = new HwLocationUpdateManager(this.mContext, this);
        while (index < PHONE_NUM) {
            if (PhoneFactory.getPhone(index) != null) {
                PhoneFactory.getPhone(index).mCi.registerForRplmnsStateChanged(this, 1, Integer.valueOf(index));
            }
            index++;
        }
        Phone phone = PhoneFactory.getDefaultPhone();
        if (phone != null) {
            serviceStateTracker = phone.getServiceStateTracker();
        }
        if (serviceStateTracker != null) {
            this.mNitzState = serviceStateTracker.getNitzState();
        }
        sendMessage(obtainMessage(1));
        if (this.mContext != null) {
            this.mCr = this.mContext.getContentResolver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SERVICE_STATE");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
            this.mCM = (ConnectivityManager) this.mContext.getSystemService("connectivity");
            this.mTimeServiceHelper = new TimeServiceHelper(this.mContext);
            this.mTimeServiceHelper.setListener(new Listener() {
                public void onTimeDetectionChange(boolean enabled) {
                }

                public void onTimeZoneDetectionChange(boolean enabled) {
                    if (enabled) {
                        HwDualCardsLocationTimeZoneUpdate.this.log("auto update time zone key change to true.");
                        HwDualCardsLocationTimeZoneUpdate.isAllowUpdateOnce = true;
                        if (HwDualCardsLocationTimeZoneUpdate.this.mNitzState != null) {
                            HwDualCardsLocationTimeZoneUpdate.this.mNitzState.handleAutoTimeZoneEnabled();
                        }
                    }
                }
            });
        } else {
            log("mContext is null");
        }
        log("HwDualCardsLocationTimeZoneUpdate init ");
    }

    public static void init(Context context) {
        if (mInstance == null) {
            mInstance = new HwDualCardsLocationTimeZoneUpdate(context);
        }
    }

    public static HwDualCardsLocationTimeZoneUpdate getDefault() {
        if (mInstance == null) {
            Rlog.e(LOG_TAG, "mInstance null");
        }
        return mInstance;
    }

    public boolean isNeedLocationTimeZoneUpdate(Phone phone, String zoneId) {
        if (PHONE_NUM == 1) {
            log("this is a single card cell phone.");
            return false;
        } else if (!this.hasUpdateTzByLoc) {
            return isForbiddenUpdateTimeZoneByRegState(phone, zoneId);
        } else {
            log("update time zone by location.");
            return true;
        }
    }

    private boolean isForbiddenUpdateTimeZoneByRegState(Phone phone, String zoneId) {
        Phone vSimPhone = VSimUtilsInner.getVSimPhone();
        if (HwVSimUtils.isVSimEnabled() && isRegInService(vSimPhone) && !getRoamingState(vSimPhone)) {
            String vSimZoneId = getTimeZoneId(getMcc(vSimPhone));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("vsim zoneId=");
            stringBuilder.append(vSimZoneId);
            log(stringBuilder.toString());
            if (TextUtils.isEmpty(vSimZoneId)) {
                return false;
            }
            return 1 ^ vSimZoneId.equals(zoneId);
        } else if (!getRoamingState(phone)) {
            return false;
        } else {
            int otherSubId = getOtherSubId(phone);
            if (isCardPresent(otherSubId)) {
                Phone otherPhone = PhoneFactory.getPhone(otherSubId);
                if (isRegInService(otherPhone)) {
                    if (getRoamingState(otherPhone)) {
                        String currentZoneId = TimeZone.getDefault().getID();
                        if (isVaildZoneId(zoneId) && zoneId.equals(currentZoneId)) {
                            log("dual cards present ,allow update time zone.");
                            return false;
                        }
                    }
                    if (!isAllowUpdateOnce) {
                        return true;
                    }
                    isAllowUpdateOnce = false;
                    if (this.hasUpdateTzByLoc) {
                        log("dual cards registration roaming, requst location updata time zone.");
                        this.mHwLocationUpdateManager.requestLocationUpdate(false);
                    }
                    return false;
                }
                log("the card is first registration, update time zone.");
                return false;
            }
            log("only one card present,allow update time zone.");
            return false;
        }
    }

    private boolean isDualCardsIsoNotEquals() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("numericSub1:");
        stringBuilder.append(this.numericSub1);
        stringBuilder.append("  numericSub2: ");
        stringBuilder.append(this.numericSub2);
        log(stringBuilder.toString());
        if (isInvalidOperatorNumeric(this.numericSub1) || isInvalidOperatorNumeric(this.numericSub2)) {
            return false;
        }
        return this.numericSub1.substring(0, 3).equals(this.numericSub2.substring(0, 3)) ^ 1;
    }

    public void handleMessage(Message msg) {
        int i = msg.what;
        if (i == 1) {
            i = getCiIndex(msg).intValue();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EVENT_RPLMNS_STATE_CHANGED");
            stringBuilder.append(i);
            log(stringBuilder.toString());
            Phone phone = PhoneFactory.getPhone(i);
            ServiceState serviceState = phone != null ? phone.getServiceState() : null;
            if (serviceState == null) {
                return;
            }
            if (serviceState.getVoiceRegState() != 0 || serviceState.getDataRegState() != 0) {
                updatePlmn(SystemProperties.get(PROPERTY_GLOBAL_OPERATOR_NUMERIC, ""), i);
            }
        } else if (i != 100) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unknow msg:");
            stringBuilder2.append(msg.what);
            log(stringBuilder2.toString());
        } else {
            log("EVENT_LOCATION_CHANGED");
            updateTimeZoneByLocation((Location) msg.obj);
        }
    }

    private Integer getCiIndex(Message msg) {
        Integer index = Integer.valueOf(null);
        if (msg == null) {
            return index;
        }
        if (msg.obj != null && (msg.obj instanceof Integer)) {
            return msg.obj;
        }
        if (msg.obj == null || !(msg.obj instanceof AsyncResult)) {
            return index;
        }
        AsyncResult ar = msg.obj;
        if (ar.userObj == null || !(ar.userObj instanceof Integer)) {
            return index;
        }
        return ar.userObj;
    }

    private void updateTimeZoneByLocation(Location location) {
        this.mCurrentLocation = location;
        if (this.mCurrentLocation != null) {
            this.mLastGetLocTime = SystemClock.elapsedRealtime();
            if (getAutoTimeZone()) {
                String newZoneId = TimezoneMapper.latLngToTimezoneString(this.mCurrentLocation.getLatitude(), this.mCurrentLocation.getLongitude());
                String currentZoneId = TimeZone.getDefault().getID();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateTimeZoneByLocation, current time zone: ");
                stringBuilder.append(currentZoneId);
                stringBuilder.append("  new zone: ");
                stringBuilder.append(newZoneId);
                log(stringBuilder.toString());
                if (isVaildZoneId(newZoneId) && !newZoneId.equals(currentZoneId)) {
                    setTimeZone(newZoneId);
                    this.hasUpdateTzByLoc = true;
                    return;
                }
                return;
            }
            loge("Auto time zone disabled!");
            return;
        }
        loge("current loction is null!");
    }

    private void setTimeZone(String zoneId) {
        if (this.mContext != null) {
            AlarmManager alarm = (AlarmManager) this.mContext.getSystemService("alarm");
            if (alarm != null) {
                alarm.setTimeZone(zoneId);
                HwReportManagerImpl.getDefault().reportSetTimeZoneByLocation(zoneId);
            }
        }
    }

    private boolean isVaildZoneId(String zoneId) {
        if (TextUtils.isEmpty(zoneId) || "unknown".equals(zoneId) || "unusedtimezone".equals(zoneId)) {
            return false;
        }
        return true;
    }

    private void registerTimeZoneLocationUpdater() {
        if (!this.hasRegTzLocUpdater) {
            log("registerTimeZoneLocationUpdater!");
            this.mHwLocationUpdateManager.registerPassiveLocationUpdate();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            this.mContext.registerReceiver(this.mScreenOnReceiver, intentFilter);
            this.hasRegTzLocUpdater = true;
            this.mNetworkStateUpdateCallback = new NetworkStateUpdateCallback(this, null);
            this.mCM.registerDefaultNetworkCallback(this.mNetworkStateUpdateCallback);
            Phone phone = PhoneFactory.getDefaultPhone();
            PowerManager powerManager = null;
            if (!(phone == null || phone.getContext() == null)) {
                powerManager = (PowerManager) phone.getContext().getSystemService("power");
            }
            if (powerManager != null && powerManager.isScreenOn() && this.mCurrentLocation == null) {
                log("screen is on, request location.");
                this.mHwLocationUpdateManager.requestLocationUpdate(false);
            }
        }
    }

    private void unregisterTimeZoneLocationUpdater() {
        if (this.hasRegTzLocUpdater) {
            log("unregisterTimeZoneLocationUpdater!");
            this.mCurrentLocation = null;
            this.mHwLocationUpdateManager.unregisterPassiveLocationUpdate();
            this.mContext.unregisterReceiver(this.mScreenOnReceiver);
            this.mCM.unregisterNetworkCallback(this.mNetworkStateUpdateCallback);
            this.hasRegTzLocUpdater = false;
        }
        this.hasUpdateTzByLoc = false;
    }

    private void updatePlmn(String plmn, int subId) {
        if (subId == 0) {
            this.numericSub1 = plmn;
        } else if (subId == 1) {
            this.numericSub2 = plmn;
        }
    }

    private boolean isInvalidOperatorNumeric(String operatorNumeric) {
        return TextUtils.isEmpty(operatorNumeric) || operatorNumeric.length() < 5;
    }

    private boolean getAutoTimeZone() {
        return Global.getInt(this.mCr, "auto_time_zone", 0) != 0;
    }

    private int getOtherSubId(Phone phone) {
        int ownSubId = phone.getSubId();
        if (ownSubId == 0) {
            return 1;
        }
        if (ownSubId == 1) {
            return 0;
        }
        return -1;
    }

    private String getMcc(Phone phone) {
        ServiceState serviceState = phone != null ? phone.getServiceState() : null;
        if (serviceState == null) {
            log("serviceState is null.");
            return "";
        }
        String numeric = serviceState.getOperatorNumeric();
        if (!isInvalidOperatorNumeric(numeric)) {
            return numeric.substring(0, 3);
        }
        log("numeric is not valid.");
        return "";
    }

    private String getTimeZoneId(String mcc) {
        if (TextUtils.isEmpty(mcc)) {
            return "";
        }
        String zoneId = this.mNitzState != null ? this.mNitzState.getTimeZoneFromMcc(mcc) : null;
        if (zoneId != null) {
            return zoneId;
        }
        List<android.icu.util.TimeZone> timeZones = TimeZoneFinder.getInstance().lookupTimeZonesByCountry(MccTable.countryCodeForMcc(Integer.parseInt(mcc)));
        if (timeZones == null || timeZones.size() != 1) {
            return "";
        }
        return ((android.icu.util.TimeZone) timeZones.get(0)).getID();
    }

    private boolean getRoamingState(Phone phone) {
        ServiceState serviceState = phone != null ? phone.getServiceState() : null;
        if (serviceState != null) {
            return serviceState.getRoaming();
        }
        log("serviceState is null retrun true.");
        return false;
    }

    private boolean isRegInService(Phone phone) {
        ServiceState serviceState = phone != null ? phone.getServiceState() : null;
        boolean z = false;
        if (serviceState == null) {
            return false;
        }
        if (serviceState.getVoiceRegState() == 0 || serviceState.getDataRegState() == 0) {
            z = true;
        }
        return z;
    }

    public boolean isCardPresent(int slotId) {
        UiccController uiccController = UiccController.getInstance();
        boolean z = false;
        if (uiccController.getUiccCard(slotId) == null) {
            return false;
        }
        if (uiccController.getUiccCard(slotId).getCardState() != CardState.CARDSTATE_ABSENT) {
            z = true;
        }
        return z;
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    protected void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }
}
