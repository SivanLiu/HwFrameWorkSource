package com.android.internal.telephony;

import android.app.AlarmManager;
import android.app.Notification.Action;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
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
import android.provider.Settings.Secure;
import android.telephony.CellLocation;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.latlongtotimezone.TimezoneMapper;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.util.TimeZone;

public class HwLocationBasedTimeZoneUpdater extends StateMachine {
    private static final String ACTION_ENABLE_LOCATION = "huawei.intent.action.ENABLE_LOCATION";
    private static final double DEFAULT_LOCATION_CHECK_DISTANCE = 100000.0d;
    private static final long DEFAULT_NITZ_OR_LOCATION_CHECK_PERIOD_MS = 3600000;
    private static final int DELAY_STOP_LOCATION_AFTER_SCREEN_OFF = 5000;
    public static final int EVENT_LOCATION_CHANGED = 100;
    private static final int EVENT_NET_ISO_CHANGED = 3;
    public static final int EVENT_NITZ_TIMEZONE_UPDATE = 1;
    private static final int EVENT_REQUEST_LOCATION_UPDATE = 5;
    private static final int EVENT_RPLMNS_STATE_CHANGED = 4;
    private static final int EVENT_SHOW_NOTIFICATION_WHEN_NO_NITZ = 7;
    private static final int EVENT_STOP_LOCATION_UPDATE = 6;
    private static final int INVALID_LAC = -1;
    private static final String LOCATION_SETTINGS_ACTIVITY = "android.settings.LOCATION_SOURCE_SETTINGS";
    private static final String LOG_TAG = "HwLocationBasedTzUpdater";
    private static final int MCC_LEN = 3;
    private static final String MULTI_TIMEZONE_COUNTRY = "au,br,ca,cd,cl,cy,ec,es,fm,gl,id,ki,kz,mn,mx,nz,pf,pg,pt,ru,um,us";
    private static final long NANOS_PER_MILLI = 1000000;
    private static final int NOTIFICATION_ID_LOCATION_ACCESS = 1;
    private static final String NOTIFICATION_TAG = "HwLocationBasedTzUpdater";
    private static final int PHONE_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final String PROPERTY_GLOBAL_OPERATOR_NUMERIC = "ril.operator.numeric";
    private static final long RESET_NOTIFY_AFTER_LOCATION_TZ_MS = 3600000;
    private static final long SEND_NOTIFY_WHEN_NO_NITZ_MS = 600000;
    private static final String TIME_UPDATE_NOTIFY_CHANNEL = "time_update_notify_channel";
    private static HwLocationBasedTimeZoneUpdater sInstance = null;
    private ContentObserver mAirplaneModeObserver = null;
    private ContentObserver mAutoTimeZoneObserver = null;
    private ConnectivityManager mCM;
    private final CheckingState mCheckingState = new CheckingState(this, null);
    private Context mContext;
    private ContentResolver mCr;
    private Location mCurrentLocation = null;
    private HwLocationUpdateManager mHwLocationUpdateManager = null;
    private final IdleState mIdleState = new IdleState(this, null);
    private boolean mIsRoaming = false;
    private int mLastLocationMode = 0;
    private int mLastNitzLac = -1;
    private Location mLastTzUpdateLocation = null;
    private String mLastValidNetIso = "";
    private final LocationTzUpdatedState mLocationTzUpdatedState = new LocationTzUpdatedState(this, null);
    private BroadcastReceiver mMultiTimeZoneStateReceiver = null;
    private Handler mMyHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4:
                    AsyncResult ar = msg.obj;
                    Integer slotId = ar.userObj;
                    int i = 0;
                    while (i < HwLocationBasedTimeZoneUpdater.PHONE_NUM) {
                        Phone phone = PhoneFactory.getPhone(i);
                        ServiceState ss = phone != null ? phone.getServiceState() : null;
                        if (ss == null || (!(ss.getVoiceRegState() == 0 || ss.getDataRegState() == 0) || TextUtils.isEmpty(HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso))) {
                            i++;
                        } else {
                            HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("[SLOT");
                            stringBuilder.append(i);
                            stringBuilder.append("] is in service, ignore EVENT_RPLMNS_STATE_CHANGED");
                            hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                            return;
                        }
                    }
                    HwLocationBasedTimeZoneUpdater.this.oldRplmn = HwLocationBasedTimeZoneUpdater.this.rplmn;
                    if (ar.result == null || !(ar.result instanceof String)) {
                        HwLocationBasedTimeZoneUpdater.this.rplmn = SystemProperties.get(HwLocationBasedTimeZoneUpdater.PROPERTY_GLOBAL_OPERATOR_NUMERIC, "");
                    } else {
                        HwLocationBasedTimeZoneUpdater.this.rplmn = (String) ar.result;
                    }
                    HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater2 = HwLocationBasedTimeZoneUpdater.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[SLOT");
                    stringBuilder2.append(slotId);
                    stringBuilder2.append("]EVENT_RPLMNS_STATE_CHANGED, rplmn");
                    stringBuilder2.append(HwLocationBasedTimeZoneUpdater.this.rplmn);
                    hwLocationBasedTimeZoneUpdater2.log(stringBuilder2.toString());
                    String mcc = "";
                    String oldMcc = "";
                    if (HwLocationBasedTimeZoneUpdater.this.rplmn != null && HwLocationBasedTimeZoneUpdater.this.rplmn.length() > 3) {
                        mcc = HwLocationBasedTimeZoneUpdater.this.rplmn.substring(0, 3);
                    }
                    if (HwLocationBasedTimeZoneUpdater.this.oldRplmn != null && HwLocationBasedTimeZoneUpdater.this.oldRplmn.length() > 3) {
                        oldMcc = HwLocationBasedTimeZoneUpdater.this.oldRplmn.substring(0, 3);
                    }
                    if (!("".equals(mcc) || mcc.equals(oldMcc))) {
                        HwLocationBasedTimeZoneUpdater.this.log("rplmn mcc changed.");
                        String netIso = "";
                        try {
                            netIso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
                        } catch (NumberFormatException e) {
                            HwLocationBasedTimeZoneUpdater.this.loge("countryCodeForMcc NumberFormatException");
                        }
                        if (!TextUtils.isEmpty(netIso)) {
                            HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso = netIso;
                            HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater3 = HwLocationBasedTimeZoneUpdater.this;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("[SLOT");
                            stringBuilder3.append(slotId);
                            stringBuilder3.append("]EVENT_RPLMNS_STATE_CHANGED, mLastValidNetIso:");
                            stringBuilder3.append(HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso);
                            hwLocationBasedTimeZoneUpdater3.log(stringBuilder3.toString());
                            HwLocationBasedTimeZoneUpdater.this.getHandler().sendMessage(obtainMessage(3, HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso));
                            if (HwLocationBasedTimeZoneUpdater.this.isMultiTimeZoneCountry(HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso)) {
                                HwReportManagerImpl.getDefault().reportMultiTZRegistered();
                                break;
                            }
                        }
                    }
                    break;
                case 5:
                    boolean useNetOnly = ((Boolean) msg.obj).booleanValue();
                    HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater4 = HwLocationBasedTimeZoneUpdater.this;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("EVENT_REQUEST_LOCATION_UPDATE, useNetOnly: ");
                    stringBuilder4.append(useNetOnly);
                    hwLocationBasedTimeZoneUpdater4.log(stringBuilder4.toString());
                    HwLocationBasedTimeZoneUpdater.this.mHwLocationUpdateManager.requestLocationUpdate(useNetOnly);
                    if (useNetOnly) {
                        HwLocationBasedTimeZoneUpdater.this.mTimeZoneUpdateTime = SystemClock.elapsedRealtime();
                        break;
                    }
                    break;
                case 6:
                    HwLocationBasedTimeZoneUpdater.this.log("EVENT_STOP_LOCATION_UPDATE.");
                    HwLocationBasedTimeZoneUpdater.this.mHwLocationUpdateManager.stopLocationUpdate();
                    break;
                case 7:
                    HwLocationBasedTimeZoneUpdater.this.log("EVENT_SHOW_NOTIFICATION_WHEN_NO_NITZ, check whether to show notification.");
                    if (!HwLocationBasedTimeZoneUpdater.this.mHwLocationUpdateManager.isLocationEnabled()) {
                        HwLocationBasedTimeZoneUpdater.this.showLocationAccessNotification();
                    }
                    HwReportManagerImpl.getDefault().reportMultiTZNoNitz();
                    break;
            }
        }
    };
    private NetworkConnectionStateCallback mNetworkConnectionStateCallback = null;
    private long mNitzTimeZoneUpdateTime = 0;
    private final NitzTzUpdatedState mNitzTzUpdatedState = new NitzTzUpdatedState(this, null);
    private final NotUpdateState mNotUpdateState = new NotUpdateState(this, null);
    private NotificationManager mNotificationManager;
    private BroadcastReceiver mServiceStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if ("android.intent.action.SERVICE_STATE".equals(intent.getAction())) {
                    String currentNetIso = null;
                    int mainSlot = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                    Phone phone = PhoneFactory.getPhone(mainSlot);
                    ServiceState serviceState = null;
                    ServiceState ss = phone != null ? phone.getServiceState() : null;
                    if (ss != null && (ss.getVoiceRegState() == 0 || ss.getDataRegState() == 0)) {
                        currentNetIso = TelephonyManager.getDefault().getNetworkCountryIso(mainSlot);
                    }
                    if (TextUtils.isEmpty(currentNetIso)) {
                        int i = 1;
                        if (HwLocationBasedTimeZoneUpdater.PHONE_NUM > 1) {
                            if (mainSlot != 0) {
                                i = 0;
                            }
                            int otherSlot = i;
                            phone = PhoneFactory.getPhone(otherSlot);
                            if (phone != null) {
                                serviceState = phone.getServiceState();
                            }
                            ss = serviceState;
                            if (ss != null && (ss.getVoiceRegState() == 0 || ss.getDataRegState() == 0)) {
                                currentNetIso = TelephonyManager.getDefault().getNetworkCountryIso(otherSlot);
                            }
                        }
                    }
                    HwLocationBasedTimeZoneUpdater.this.setNetworkRoamingState();
                    boolean netIsoChanged = false;
                    if (!(TextUtils.isEmpty(currentNetIso) || currentNetIso.equals(HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso))) {
                        netIsoChanged = true;
                        HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ACTION_SERVICE_STATE_CHANGED, network country iso changed from ");
                        stringBuilder.append(HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso);
                        stringBuilder.append(" to ");
                        stringBuilder.append(currentNetIso);
                        hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                    }
                    if (!TextUtils.isEmpty(currentNetIso)) {
                        HwLocationBasedTimeZoneUpdater.this.mLastValidNetIso = currentNetIso;
                    }
                    if (netIsoChanged) {
                        HwLocationBasedTimeZoneUpdater.this.getHandler().sendMessage(HwLocationBasedTimeZoneUpdater.this.obtainMessage(3, currentNetIso));
                        if (!TextUtils.isEmpty(currentNetIso) && HwLocationBasedTimeZoneUpdater.this.isMultiTimeZoneCountry(currentNetIso)) {
                            HwReportManagerImpl.getDefault().reportMultiTZRegistered();
                        }
                    }
                }
            }
        }
    };
    private long mTimeZoneUpdateTime = 0;
    private String oldRplmn = "";
    private String rplmn = "";

    private class AirplaneModeObserver extends ContentObserver {
        public AirplaneModeObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            if (Global.getInt(HwLocationBasedTimeZoneUpdater.this.mCr, "airplane_mode_on", 0) > 0) {
                HwLocationBasedTimeZoneUpdater.this.log("airplane mode on, reset mCurrentLocation.");
                HwLocationBasedTimeZoneUpdater.this.mCurrentLocation = null;
            }
        }
    }

    private class AutoTimeZoneObserver extends ContentObserver {
        public AutoTimeZoneObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            if (Global.getInt(HwLocationBasedTimeZoneUpdater.this.mCr, "auto_time_zone", 0) == 0) {
                HwLocationBasedTimeZoneUpdater.this.log("Auto time zone disabled, send EVENT_STOP_LOCATION_UPDATE immediately.");
                HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessage(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(6));
            } else if (HwLocationBasedTimeZoneUpdater.this.mHwLocationUpdateManager.isLocationEnabled()) {
                HwLocationBasedTimeZoneUpdater.this.log("Auto time zone enabled, send EVENT_REQUEST_LOCATION_UPDATE immediately.");
                HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessage(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(5, Boolean.valueOf(false)));
                HwLocationBasedTimeZoneUpdater.this.mCurrentLocation = null;
            }
        }
    }

    private class CheckingState extends State {
        private CheckingState() {
        }

        /* synthetic */ CheckingState(HwLocationBasedTimeZoneUpdater x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            HwLocationBasedTimeZoneUpdater.this.log("entering CheckingState");
            HwLocationBasedTimeZoneUpdater.this.registerEventsForMultiTimeZoneCountry();
        }

        public void exit() {
            HwLocationBasedTimeZoneUpdater.this.log("leaving CheckingState");
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater;
            if (i == 1) {
                HwLocationBasedTimeZoneUpdater.this.log("CheckingState.processMessage EVENT_NITZ_TIMEZONE_UPDATE");
                HwLocationBasedTimeZoneUpdater.this.mTimeZoneUpdateTime = HwLocationBasedTimeZoneUpdater.this.mNitzTimeZoneUpdateTime = SystemClock.elapsedRealtime();
                HwLocationBasedTimeZoneUpdater.this.mLastNitzLac = ((Integer) msg.obj).intValue();
                hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_NITZ_TIMEZONE_UPDATE, mNitzTimeZoneUpdateTime: ");
                stringBuilder.append(HwLocationBasedTimeZoneUpdater.this.mNitzTimeZoneUpdateTime);
                stringBuilder.append("ms");
                hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                if (HwLocationBasedTimeZoneUpdater.this.mMyHandler.hasMessages(7)) {
                    HwLocationBasedTimeZoneUpdater.this.log("CheckingState, remove EVENT_SHOW_NOTIFICATION_WHEN_NO_NITZ.");
                    HwLocationBasedTimeZoneUpdater.this.mMyHandler.removeMessages(7);
                }
                HwLocationBasedTimeZoneUpdater.this.transitionTo(HwLocationBasedTimeZoneUpdater.this.mNitzTzUpdatedState);
                return true;
            } else if (i == 3) {
                HwLocationBasedTimeZoneUpdater.this.log("CheckingState.processMessage EVENT_NET_ISO_CHANGED");
                if (HwLocationBasedTimeZoneUpdater.this.isMultiTimeZoneCountry((String) msg.obj)) {
                    HwLocationBasedTimeZoneUpdater.this.registerEventsForMultiTimeZoneCountry();
                } else {
                    HwLocationBasedTimeZoneUpdater.this.transitionTo(HwLocationBasedTimeZoneUpdater.this.mNotUpdateState);
                }
                return true;
            } else if (i != 100) {
                hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("CheckingState.processMessage default:");
                stringBuilder2.append(msg.what);
                hwLocationBasedTimeZoneUpdater.log(stringBuilder2.toString());
                return false;
            } else {
                HwLocationBasedTimeZoneUpdater.this.log("CheckingState.processMessage EVENT_LOCATION_CHANGED");
                if (HwLocationBasedTimeZoneUpdater.this.shouldUpateTimeZoneInCheckingState((Location) msg.obj)) {
                    HwLocationBasedTimeZoneUpdater.this.transitionTo(HwLocationBasedTimeZoneUpdater.this.mLocationTzUpdatedState);
                }
                return true;
            }
        }
    }

    private class IdleState extends State {
        private IdleState() {
        }

        /* synthetic */ IdleState(HwLocationBasedTimeZoneUpdater x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            HwLocationBasedTimeZoneUpdater.this.log("entering IdleState");
        }

        public void exit() {
            HwLocationBasedTimeZoneUpdater.this.log("leaving IdleState");
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater;
            StringBuilder stringBuilder;
            if (i == 1) {
                HwLocationBasedTimeZoneUpdater.this.log("IdleState.processMessage EVENT_NITZ_TIMEZONE_UPDATE");
                HwLocationBasedTimeZoneUpdater.this.mTimeZoneUpdateTime = HwLocationBasedTimeZoneUpdater.this.mNitzTimeZoneUpdateTime = SystemClock.elapsedRealtime();
                HwLocationBasedTimeZoneUpdater.this.mLastNitzLac = ((Integer) msg.obj).intValue();
                hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_NITZ_TIMEZONE_UPDATE, mNitzTimeZoneUpdateTime: ");
                stringBuilder.append(HwLocationBasedTimeZoneUpdater.this.mNitzTimeZoneUpdateTime);
                stringBuilder.append("ms");
                hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                return true;
            } else if (i != 3) {
                hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IdleState.processMessage default:");
                stringBuilder.append(msg.what);
                hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                return true;
            } else {
                HwLocationBasedTimeZoneUpdater.this.log("IdleState.processMessage EVENT_NET_ISO_CHANGED");
                if (HwLocationBasedTimeZoneUpdater.this.isMultiTimeZoneCountry((String) msg.obj)) {
                    HwLocationBasedTimeZoneUpdater.this.transitionTo(HwLocationBasedTimeZoneUpdater.this.mCheckingState);
                } else {
                    HwLocationBasedTimeZoneUpdater.this.transitionTo(HwLocationBasedTimeZoneUpdater.this.mNotUpdateState);
                }
                return true;
            }
        }
    }

    private class LocationTzUpdatedState extends State {
        private LocationTzUpdatedState() {
        }

        /* synthetic */ LocationTzUpdatedState(HwLocationBasedTimeZoneUpdater x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            HwLocationBasedTimeZoneUpdater.this.log("entering LocationTzUpdatedState");
            if (!HwLocationBasedTimeZoneUpdater.this.getAutoTimeZone()) {
                HwLocationBasedTimeZoneUpdater.this.loge("Auto time zone disabled!");
            } else if (HwLocationBasedTimeZoneUpdater.this.mCurrentLocation != null) {
                String newZoneId = TimezoneMapper.latLngToTimezoneString(HwLocationBasedTimeZoneUpdater.this.mCurrentLocation.getLatitude(), HwLocationBasedTimeZoneUpdater.this.mCurrentLocation.getLongitude());
                HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LocationTzUpdatedState, time zone after conversion: ");
                stringBuilder.append(newZoneId);
                hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                boolean isValidZoneId = HwLocationBasedTimeZoneUpdater.this.isValidTimeZone(newZoneId);
                String currentZoneId = TimeZone.getDefault().getID();
                HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater2 = HwLocationBasedTimeZoneUpdater.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("LocationTzUpdatedState, current time zone: ");
                stringBuilder2.append(currentZoneId);
                hwLocationBasedTimeZoneUpdater2.log(stringBuilder2.toString());
                if (isValidZoneId) {
                    if (!newZoneId.equals(currentZoneId)) {
                        HwLocationBasedTimeZoneUpdater.this.setTimeZone(newZoneId);
                        if (HwLocationBasedTimeZoneUpdater.this.mMyHandler.hasMessages(7)) {
                            HwLocationBasedTimeZoneUpdater.this.log("LocationTzUpdatedState, reset EVENT_SHOW_NOTIFICATION_WHEN_NO_NITZ.");
                            HwLocationBasedTimeZoneUpdater.this.mMyHandler.removeMessages(7);
                            HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessageDelayed(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(7), 3600000);
                        }
                    }
                    HwLocationBasedTimeZoneUpdater.this.mTimeZoneUpdateTime = SystemClock.elapsedRealtime();
                    HwLocationBasedTimeZoneUpdater.this.mLastTzUpdateLocation = HwLocationBasedTimeZoneUpdater.this.mCurrentLocation;
                    hwLocationBasedTimeZoneUpdater2 = HwLocationBasedTimeZoneUpdater.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("LocationTzUpdatedState, mTimeZoneUpdateTime: ");
                    stringBuilder2.append(HwLocationBasedTimeZoneUpdater.this.mTimeZoneUpdateTime);
                    stringBuilder2.append("ms");
                    hwLocationBasedTimeZoneUpdater2.log(stringBuilder2.toString());
                }
            }
        }

        public void exit() {
            HwLocationBasedTimeZoneUpdater.this.log("leaving LocationTzUpdatedState");
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LocationTzUpdatedState.processMessage default:");
            stringBuilder.append(msg.what);
            hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
            return false;
        }
    }

    private class MultiTimeZoneBroadcastReceiver extends BroadcastReceiver {
        private MultiTimeZoneBroadcastReceiver() {
        }

        /* synthetic */ MultiTimeZoneBroadcastReceiver(HwLocationBasedTimeZoneUpdater x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    if (HwLocationBasedTimeZoneUpdater.this.mMyHandler.hasMessages(6)) {
                        HwLocationBasedTimeZoneUpdater.this.log("ACTION_SCREEN_ON, remove EVENT_STOP_LOCATION_UPDATE.");
                        HwLocationBasedTimeZoneUpdater.this.mMyHandler.removeMessages(6);
                    }
                    long tzUpdateDiff = SystemClock.elapsedRealtime() - HwLocationBasedTimeZoneUpdater.this.mTimeZoneUpdateTime;
                    HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ACTION_SCREEN_ON, tzUpdateDiff: ");
                    stringBuilder.append(tzUpdateDiff);
                    stringBuilder.append("ms, mTimeZoneUpdateTime: ");
                    stringBuilder.append(HwLocationBasedTimeZoneUpdater.this.mTimeZoneUpdateTime);
                    stringBuilder.append("ms");
                    hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                    if (HwLocationBasedTimeZoneUpdater.this.getAutoTimeZone() && HwLocationBasedTimeZoneUpdater.this.mHwLocationUpdateManager.isNetworkLocationAvailable() && (HwLocationBasedTimeZoneUpdater.this.mCurrentLocation == null || tzUpdateDiff > HwLocationBasedTimeZoneUpdater.this.getCheckPeriodByRoamingState())) {
                        HwLocationBasedTimeZoneUpdater.this.log("ACTION_SCREEN_ON, send EVENT_REQUEST_LOCATION_UPDATE.");
                        HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessage(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(5, Boolean.valueOf(true)));
                    }
                } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    if (HwLocationBasedTimeZoneUpdater.this.mMyHandler.hasMessages(5)) {
                        HwLocationBasedTimeZoneUpdater.this.log("ACTION_SCREEN_OFF, remove EVENT_REQUEST_LOCATION_UPDATE.");
                        HwLocationBasedTimeZoneUpdater.this.mMyHandler.removeMessages(5);
                    }
                    HwLocationBasedTimeZoneUpdater.this.log("ACTION_SCREEN_OFF, delay send EVENT_STOP_LOCATION_UPDATE.");
                    HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessageDelayed(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(6), 5000);
                } else if ("android.location.MODE_CHANGED".equals(action)) {
                    int currentLocationMode = Secure.getInt(HwLocationBasedTimeZoneUpdater.this.mCr, "location_mode", 0);
                    if (Secure.getInt(HwLocationBasedTimeZoneUpdater.this.mCr, "location_mode", 0) == 0) {
                        HwLocationBasedTimeZoneUpdater.this.log("Location service disabled, send EVENT_STOP_LOCATION_UPDATE immediately.");
                        HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessage(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(6));
                    } else if (HwLocationBasedTimeZoneUpdater.this.getAutoTimeZone() && HwLocationBasedTimeZoneUpdater.this.mLastLocationMode == 0) {
                        HwLocationBasedTimeZoneUpdater.this.log("Location service enabled, send EVENT_REQUEST_LOCATION_UPDATE immediately.");
                        HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessage(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(5, Boolean.valueOf(false)));
                        HwLocationBasedTimeZoneUpdater.this.dismissLocationAccessNotification();
                        HwLocationBasedTimeZoneUpdater.this.mCurrentLocation = null;
                    }
                    HwLocationBasedTimeZoneUpdater.this.mLastLocationMode = currentLocationMode;
                } else if (HwLocationBasedTimeZoneUpdater.ACTION_ENABLE_LOCATION.equals(action)) {
                    HwLocationBasedTimeZoneUpdater.this.log("receives ACTION_ENABLE_LOCATION, dismiss notification and enable location access.");
                    HwLocationBasedTimeZoneUpdater.this.dismissLocationAccessNotification();
                    HwLocationBasedTimeZoneUpdater.this.mHwLocationUpdateManager.setLocationEnabled(true);
                }
            }
        }
    }

    private class NetworkConnectionStateCallback extends NetworkCallback {
        private NetworkConnectionStateCallback() {
        }

        /* synthetic */ NetworkConnectionStateCallback(HwLocationBasedTimeZoneUpdater x0, AnonymousClass1 x1) {
            this();
        }

        public void onAvailable(Network network) {
            HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Default network available: ");
            stringBuilder.append(network);
            hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
            if (HwLocationBasedTimeZoneUpdater.this.mCurrentLocation == null && HwLocationBasedTimeZoneUpdater.this.mHwLocationUpdateManager.isNetworkLocationAvailable()) {
                HwLocationBasedTimeZoneUpdater.this.log("request network location when mobile/wifi network available");
                HwLocationBasedTimeZoneUpdater.this.mMyHandler.sendMessage(HwLocationBasedTimeZoneUpdater.this.mMyHandler.obtainMessage(5, Boolean.valueOf(true)));
            }
        }
    }

    private class NitzTzUpdatedState extends State {
        private NitzTzUpdatedState() {
        }

        /* synthetic */ NitzTzUpdatedState(HwLocationBasedTimeZoneUpdater x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            HwLocationBasedTimeZoneUpdater.this.log("entering NitzTzUpdatedState");
        }

        public void exit() {
            HwLocationBasedTimeZoneUpdater.this.log("leaving NitzTzUpdatedState");
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 100) {
                HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NitzTzUpdatedState.processMessage default:");
                stringBuilder.append(msg.what);
                hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                return false;
            }
            HwLocationBasedTimeZoneUpdater.this.log("NitzTzUpdatedState.processMessage EVENT_LOCATION_CHANGED");
            if (HwLocationBasedTimeZoneUpdater.this.shouldUpateTimeZoneInNitzState((Location) msg.obj)) {
                HwLocationBasedTimeZoneUpdater.this.transitionTo(HwLocationBasedTimeZoneUpdater.this.mLocationTzUpdatedState);
            }
            return true;
        }
    }

    private class NotUpdateState extends State {
        private NotUpdateState() {
        }

        /* synthetic */ NotUpdateState(HwLocationBasedTimeZoneUpdater x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            HwLocationBasedTimeZoneUpdater.this.log("entering NotUpdateState");
            HwLocationBasedTimeZoneUpdater.this.unregisterEventsForMultiTimeZoneCountry();
        }

        public void exit() {
            HwLocationBasedTimeZoneUpdater.this.log("leaving NotUpdateState");
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                HwLocationBasedTimeZoneUpdater.this.log("NotUpdateState.processMessage EVENT_NITZ_TIMEZONE_UPDATE, ignore.");
                return true;
            } else if (i != 3) {
                HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater = HwLocationBasedTimeZoneUpdater.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NotUpdateState.processMessage default:");
                stringBuilder.append(msg.what);
                hwLocationBasedTimeZoneUpdater.log(stringBuilder.toString());
                return false;
            } else {
                HwLocationBasedTimeZoneUpdater.this.log("NotUpdateState.processMessage EVENT_NET_ISO_CHANGED");
                if (HwLocationBasedTimeZoneUpdater.this.isMultiTimeZoneCountry((String) msg.obj)) {
                    HwLocationBasedTimeZoneUpdater.this.transitionTo(HwLocationBasedTimeZoneUpdater.this.mCheckingState);
                } else {
                    HwLocationBasedTimeZoneUpdater.this.unregisterEventsForMultiTimeZoneCountry();
                }
                return true;
            }
        }
    }

    private HwLocationBasedTimeZoneUpdater(Context context) {
        super("HwLocationBasedTzUpdater");
        this.mContext = context;
        if (this.mContext != null) {
            this.mCr = this.mContext.getContentResolver();
            this.mCM = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        addState(this.mIdleState);
        addState(this.mNotUpdateState, this.mIdleState);
        addState(this.mCheckingState, this.mIdleState);
        addState(this.mNitzTzUpdatedState, this.mCheckingState);
        addState(this.mLocationTzUpdatedState, this.mCheckingState);
        setInitialState(this.mIdleState);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SERVICE_STATE");
        if (this.mContext != null) {
            this.mContext.registerReceiver(this.mServiceStateReceiver, filter);
        }
        registerRplmnChange();
        this.mHwLocationUpdateManager = new HwLocationUpdateManager(context, getHandler());
    }

    public static HwLocationBasedTimeZoneUpdater init(Context context) {
        HwLocationBasedTimeZoneUpdater hwLocationBasedTimeZoneUpdater;
        synchronized (HwLocationBasedTimeZoneUpdater.class) {
            if (sInstance == null) {
                sInstance = new HwLocationBasedTimeZoneUpdater(context);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("init() called multiple times! sInstance = ");
                stringBuilder.append(sInstance);
                Log.wtf("HwLocationBasedTzUpdater", stringBuilder.toString());
            }
            hwLocationBasedTimeZoneUpdater = sInstance;
        }
        return hwLocationBasedTimeZoneUpdater;
    }

    public static HwLocationBasedTimeZoneUpdater getInstance() {
        if (sInstance == null) {
            Log.wtf("HwLocationBasedTzUpdater", "getInstance null");
        }
        return sInstance;
    }

    public void dispose() {
        quit();
    }

    private boolean isValidTimeZone(String zoneId) {
        return (TextUtils.isEmpty(zoneId) || "unknown".equals(zoneId) || "unusedtimezone".equals(zoneId)) ? false : true;
    }

    private void setNetworkRoamingState() {
        int i = 0;
        while (i < PHONE_NUM) {
            if (TelephonyManager.getDefault().isNetworkRoaming(i)) {
                this.mIsRoaming = true;
                break;
            }
            i++;
        }
        if (PHONE_NUM == i) {
            this.mIsRoaming = false;
        }
    }

    private long getCheckPeriodByRoamingState() {
        return this.mIsRoaming ? 1800000 : 3600000;
    }

    private void registerRplmnChange() {
        for (int i = 0; i < PHONE_NUM; i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (!(phone == null || phone.mCi == null)) {
                phone.mCi.registerForRplmnsStateChanged(this.mMyHandler, 4, Integer.valueOf(i));
            }
        }
    }

    private boolean isMultiTimeZoneCountry(String iso) {
        boolean ret = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isMultiTimeZoneCountry iso: ");
        stringBuilder.append(iso);
        log(stringBuilder.toString());
        int i = 0;
        if (TextUtils.isEmpty(iso)) {
            return false;
        }
        String[] multiTimeZoneIsoArray = MULTI_TIMEZONE_COUNTRY.split(",");
        int length = multiTimeZoneIsoArray.length;
        while (i < length) {
            if (iso.equalsIgnoreCase(multiTimeZoneIsoArray[i])) {
                ret = true;
                break;
            }
            i++;
        }
        return ret;
    }

    private void registerEventsForMultiTimeZoneCountry() {
        log("registerEventsForMultiTimeZoneCountry");
        if (this.mAutoTimeZoneObserver == null && this.mCr != null) {
            this.mAutoTimeZoneObserver = new AutoTimeZoneObserver(getHandler());
            this.mCr.registerContentObserver(Global.getUriFor("auto_time_zone"), true, this.mAutoTimeZoneObserver);
        }
        if (this.mAirplaneModeObserver == null && this.mCr != null) {
            this.mAirplaneModeObserver = new AirplaneModeObserver(getHandler());
            this.mCr.registerContentObserver(Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        }
        if (this.mMultiTimeZoneStateReceiver == null && this.mContext != null) {
            this.mMultiTimeZoneStateReceiver = new MultiTimeZoneBroadcastReceiver(this, null);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.location.MODE_CHANGED");
            intentFilter.addAction(ACTION_ENABLE_LOCATION);
            this.mContext.registerReceiver(this.mMultiTimeZoneStateReceiver, intentFilter);
        }
        if (this.mNetworkConnectionStateCallback == null) {
            this.mNetworkConnectionStateCallback = new NetworkConnectionStateCallback(this, null);
            this.mCM.registerDefaultNetworkCallback(this.mNetworkConnectionStateCallback, getHandler());
        }
        this.mHwLocationUpdateManager.registerPassiveLocationUpdate();
        if (this.mMyHandler.hasMessages(7)) {
            log("remove EVENT_SHOW_NOTIFICATION_WHEN_NO_NITZ");
            this.mMyHandler.removeMessages(7);
        }
        this.mMyHandler.sendMessageDelayed(this.mMyHandler.obtainMessage(7), SEND_NOTIFY_WHEN_NO_NITZ_MS);
        log("delay send EVENT_SHOW_NOTIFICATION_WHEN_NO_NITZ");
        this.mCurrentLocation = null;
        if (isScreenOn()) {
            log("request network and gps location once first enter multi tz country");
            this.mMyHandler.sendMessage(this.mMyHandler.obtainMessage(5, Boolean.valueOf(false)));
        }
    }

    private void unregisterEventsForMultiTimeZoneCountry() {
        log("unregisterEventsForMultiTimeZoneCountry");
        if (!(this.mAutoTimeZoneObserver == null || this.mCr == null)) {
            this.mCr.unregisterContentObserver(this.mAutoTimeZoneObserver);
            this.mAutoTimeZoneObserver = null;
        }
        if (!(this.mAirplaneModeObserver == null || this.mCr == null)) {
            this.mCr.unregisterContentObserver(this.mAirplaneModeObserver);
            this.mAirplaneModeObserver = null;
        }
        if (!(this.mMultiTimeZoneStateReceiver == null || this.mContext == null)) {
            this.mContext.unregisterReceiver(this.mMultiTimeZoneStateReceiver);
            this.mMultiTimeZoneStateReceiver = null;
        }
        if (this.mNetworkConnectionStateCallback != null) {
            this.mCM.unregisterNetworkCallback(this.mNetworkConnectionStateCallback);
            this.mNetworkConnectionStateCallback = null;
        }
        this.mHwLocationUpdateManager.unregisterPassiveLocationUpdate();
        if (this.mMyHandler.hasMessages(5)) {
            log("remove EVENT_REQUEST_LOCATION_UPDATE");
            this.mMyHandler.removeMessages(5);
        }
        if (this.mMyHandler.hasMessages(7)) {
            log("remove EVENT_SHOW_NOTIFICATION_WHEN_NO_NITZ");
            this.mMyHandler.removeMessages(7);
        }
        this.mMyHandler.sendMessage(this.mMyHandler.obtainMessage(6));
    }

    private boolean shouldUpateTimeZoneInCheckingState(Location location) {
        boolean ret = false;
        if (this.mCurrentLocation == null || this.mLastTzUpdateLocation == null) {
            log("shouldUpateTimeZoneInCheckingState, mCurrentLocation or mLastTzUpdateLocation is null.");
            ret = true;
        } else if (location != null) {
            double distance = HwLocationUpdateManager.getDistance(location.getLatitude(), location.getLongitude(), this.mLastTzUpdateLocation.getLatitude(), this.mLastTzUpdateLocation.getLongitude());
            long locTimeDiff = location.getElapsedRealtimeNanos() - this.mLastTzUpdateLocation.getElapsedRealtimeNanos();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("shouldUpateTimeZoneInCheckingState, distance: ");
            stringBuilder.append(distance);
            stringBuilder.append("m, locTimeDiff: ");
            stringBuilder.append(locTimeDiff);
            stringBuilder.append("ns");
            log(stringBuilder.toString());
            if (distance > DEFAULT_LOCATION_CHECK_DISTANCE || locTimeDiff > getCheckPeriodByRoamingState() * NANOS_PER_MILLI) {
                ret = true;
            }
        }
        this.mCurrentLocation = location;
        return ret;
    }

    private boolean shouldUpateTimeZoneInNitzState(Location location) {
        boolean ret = false;
        int newLac = -1;
        long nitzTimeDiff = SystemClock.elapsedRealtime() - this.mNitzTimeZoneUpdateTime;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("shouldUpateTimeZoneInNitzState, nitzTimeDiff: ");
        stringBuilder.append(nitzTimeDiff);
        stringBuilder.append("ms, mNitzTimeZoneUpdateTime: ");
        stringBuilder.append(this.mNitzTimeZoneUpdateTime);
        stringBuilder.append("ms");
        log(stringBuilder.toString());
        if (this.mIsRoaming && this.mCurrentLocation == null) {
            log("shouldUpateTimeZoneInNitzState, first get location in roaming.");
            this.mCurrentLocation = location;
            return true;
        }
        for (int i = 0; i < PHONE_NUM; i++) {
            CellLocation cellLoc = HwTelephonyManagerInner.getDefault().getCellLocation(i);
            if (cellLoc != null && (cellLoc instanceof GsmCellLocation) && -1 != ((GsmCellLocation) cellLoc).getLac()) {
                newLac = ((GsmCellLocation) cellLoc).getLac();
                break;
            }
        }
        if (nitzTimeDiff > getCheckPeriodByRoamingState() && (newLac != this.mLastNitzLac || (!hasIccCard() && this.mLastNitzLac == -1 && newLac == -1))) {
            log("shouldUpateTimeZoneInNitzState, lac changed or card absent.");
            ret = true;
        }
        this.mCurrentLocation = location;
        return ret;
    }

    private boolean hasIccCard() {
        for (int i = 0; i < PHONE_NUM; i++) {
            if (TelephonyManager.getDefault().hasIccCard(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean isScreenOn() {
        if (this.mContext != null) {
            PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
            if (pm != null) {
                return pm.isScreenOn();
            }
        }
        return false;
    }

    private boolean getAutoTimeZone() {
        return Global.getInt(this.mCr, "auto_time_zone", 0) != 0;
    }

    private void setTimeZone(String zoneId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setTimeZone, zoneId: ");
        stringBuilder.append(zoneId);
        log(stringBuilder.toString());
        if (this.mContext != null) {
            AlarmManager alarm = (AlarmManager) this.mContext.getSystemService("alarm");
            if (alarm != null) {
                alarm.setTimeZone(zoneId);
                HwReportManagerImpl.getDefault().reportSetTimeZoneByLocation(zoneId);
            }
        }
    }

    private void showLocationAccessNotification() {
        log("showLocationAccessNotification");
        if (this.mContext != null) {
            Intent resultIntent = new Intent(LOCATION_SETTINGS_ACTIVITY);
            resultIntent.setFlags(335544320);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this.mContext, 0, resultIntent, 134217728);
            this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
            this.mNotificationManager.createNotificationChannel(new NotificationChannel(TIME_UPDATE_NOTIFY_CHANNEL, this.mContext.getString(33686045), 3));
            this.mNotificationManager.notify("HwLocationBasedTzUpdater", 1, new Builder(this.mContext).setSmallIcon(33751759).setAppName(this.mContext.getString(33686045)).setWhen(System.currentTimeMillis()).setShowWhen(true).setAutoCancel(true).setDefaults(-1).setContentTitle(this.mContext.getString(33686046)).setContentText(this.mContext.getString(33686047)).setContentIntent(resultPendingIntent).setStyle(new BigTextStyle()).setChannelId(TIME_UPDATE_NOTIFY_CHANNEL).addAction(getEnableLocationAction()).build());
        }
    }

    private Action getEnableLocationAction() {
        if (this.mContext == null) {
            return null;
        }
        Intent intent = new Intent(ACTION_ENABLE_LOCATION);
        intent.setPackage(this.mContext.getPackageName());
        return new Action.Builder(null, this.mContext.getString(33686048), PendingIntent.getBroadcast(this.mContext, 0, intent, 134217728)).build();
    }

    private void dismissLocationAccessNotification() {
        if (this.mNotificationManager != null) {
            log("dismissLocationAccessNotification");
            this.mNotificationManager.cancel("HwLocationBasedTzUpdater", 1);
            this.mNotificationManager = null;
        }
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected void loge(String s, Throwable e) {
        Rlog.e(getName(), s, e);
    }
}
