package com.android.internal.telephony;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.icu.util.TimeZone;
import android.os.AsyncResult;
import android.os.BaseBundle;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.provider.Telephony.ServiceStateTable;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.DataSpecificRegistrationStates;
import android.telephony.NetworkRegistrationState;
import android.telephony.PhysicalChannelConfig;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.VoiceSpecificRegistrationStates;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.view.Display;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.dataconnection.TransportManager;
import com.android.internal.telephony.gsm.HwCustGsmServiceStateTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.telephony.util.TimeStampedValue;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import libcore.util.TimeZoneFinder;

public class ServiceStateTracker extends Handler {
    private static final String ACTION_COMMFORCE = "huawei.intent.action.COMMFORCE";
    private static final String ACTION_RADIO_OFF = "android.intent.action.ACTION_RADIO_OFF";
    private static final String ACTION_TIMEZONE_SELECTION = "com.huawei.intent.action.ACTION_TIMEZONE_SELECTION";
    private static final boolean CLEAR_NITZ_WHEN_REG = SystemProperties.getBoolean("ro.config.clear_nitz_when_reg", true);
    public static final int CS_DISABLED = 1004;
    public static final int CS_EMERGENCY_ENABLED = 1006;
    public static final int CS_ENABLED = 1003;
    public static final int CS_NORMAL_ENABLED = 1005;
    public static final int CS_NOTIFICATION = 999;
    public static final int CS_REJECT_CAUSE_ENABLED = 2001;
    public static final int CS_REJECT_CAUSE_NOTIFICATION = 111;
    static final boolean DBG = true;
    public static final int DEFAULT_GPRS_CHECK_PERIOD_MILLIS = 60000;
    public static final String DEFAULT_MNC = "00";
    private static final int DELAY_TIME_TO_NOTIF_NITZ = 60000;
    private static final boolean ENABLE_DEMO = SystemProperties.getBoolean("ro.config.enable_demo", false);
    protected static final int EVENT_ALL_DATA_DISCONNECTED = 49;
    protected static final int EVENT_CDMA_PRL_VERSION_CHANGED = 40;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 39;
    protected static final int EVENT_CHANGE_IMS_STATE = 45;
    protected static final int EVENT_CHECK_REPORT_GPRS = 22;
    protected static final int EVENT_ERI_FILE_LOADED = 36;
    protected static final int EVENT_GET_AD_DONE = 1002;
    protected static final int EVENT_GET_CELL_INFO_LIST = 43;
    protected static final int EVENT_GET_LOC_DONE = 15;
    protected static final int EVENT_GET_PREFERRED_NETWORK_TYPE = 19;
    protected static final int EVENT_GET_SIGNAL_STRENGTH = 3;
    public static final int EVENT_ICC_CHANGED = 42;
    protected static final int EVENT_IMS_CAPABILITY_CHANGED = 48;
    protected static final int EVENT_IMS_SERVICE_STATE_CHANGED = 53;
    protected static final int EVENT_IMS_STATE_CHANGED = 46;
    protected static final int EVENT_IMS_STATE_DONE = 47;
    protected static final int EVENT_LOCATION_UPDATES_ENABLED = 18;
    protected static final int EVENT_NETWORK_STATE_CHANGED = 2;
    private static final int EVENT_NITZ_CAPABILITY_NOTIFICATION = 100;
    protected static final int EVENT_NITZ_TIME = 11;
    protected static final int EVENT_NV_READY = 35;
    protected static final int EVENT_OTA_PROVISION_STATUS_CHANGE = 37;
    protected static final int EVENT_PHONE_TYPE_SWITCHED = 50;
    protected static final int EVENT_PHYSICAL_CHANNEL_CONFIG = 55;
    protected static final int EVENT_POLL_SIGNAL_STRENGTH = 10;
    protected static final int EVENT_POLL_STATE_CDMA_SUBSCRIPTION = 34;
    protected static final int EVENT_POLL_STATE_GPRS = 5;
    protected static final int EVENT_POLL_STATE_NETWORK_SELECTION_MODE = 14;
    protected static final int EVENT_POLL_STATE_OPERATOR = 6;
    protected static final int EVENT_POLL_STATE_REGISTRATION = 4;
    protected static final int EVENT_RADIO_ON = 41;
    protected static final int EVENT_RADIO_POWER_FROM_CARRIER = 51;
    protected static final int EVENT_RADIO_POWER_OFF = 56;
    protected static final int EVENT_RADIO_POWER_OFF_DONE = 54;
    protected static final int EVENT_RADIO_STATE_CHANGED = 1;
    protected static final int EVENT_RESET_PREFERRED_NETWORK_TYPE = 21;
    protected static final int EVENT_RESTRICTED_STATE_CHANGED = 23;
    protected static final int EVENT_RUIM_READY = 26;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 27;
    protected static final int EVENT_SET_PREFERRED_NETWORK_TYPE = 20;
    protected static final int EVENT_SET_RADIO_POWER_OFF = 38;
    protected static final int EVENT_SIGNAL_STRENGTH_UPDATE = 12;
    protected static final int EVENT_SIM_NOT_INSERTED = 52;
    protected static final int EVENT_SIM_READY = 17;
    protected static final int EVENT_SIM_RECORDS_LOADED = 16;
    protected static final int EVENT_UNSOL_CELL_INFO_LIST = 44;
    private static final String EXTRA_SHOW_EMERGENCYONLY = "showEmergencyOnly";
    private static final String EXTRA_SHOW_WIFI = "showWifi";
    private static final String EXTRA_WIFI = "wifi";
    private static final boolean FEATURE_DELAY_UPDATE_SIGANL_STENGTH = SystemProperties.getBoolean("ro.config.delay_send_signal", true);
    private static final boolean FEATURE_RECOVER_AUTO_NETWORK_MODE = SystemProperties.getBoolean("ro.hwpp.recover_auto_mode", false);
    protected static final boolean HW_FAST_SET_RADIO_OFF = SystemProperties.getBoolean("ro.config.hw_fast_set_radio_off", false);
    private static final int HW_OPTA = SystemProperties.getInt("ro.config.hw_opta", -1);
    private static final int HW_OPTB = SystemProperties.getInt("ro.config.hw_optb", -1);
    private static final boolean IGNORE_GOOGLE_NON_ROAMING = true;
    private static final int INVALID_LTE_EARFCN = -1;
    public static final String INVALID_MCC = "000";
    public static final boolean ISDEMO;
    private static final boolean IS_HISI_PLATFORM = (HwModemCapability.isCapabilitySupport(9) ^ 1);
    private static final long LAST_CELL_INFO_LIST_MAX_AGE_MS = 2000;
    private static final boolean MDOEM_WORK_MODE_IS_SRLTE = SystemProperties.getBoolean("ro.config.hw_srlte", false);
    private static final int MS_PER_HOUR = 3600000;
    private static final int NITZ_UPDATE_SPACING_TIME = 1800000;
    protected static final int NOT_REGISTERED_ON_CDMA_SYSTEM = -1;
    private static final String PERMISSION_COMM_FORCE = "android.permission.COMM_FORCE";
    private static boolean PLUS_TRANFER_IN_MDOEM = HwModemCapability.isCapabilitySupport(2);
    private static final int POLL_PERIOD_MILLIS = 20000;
    protected static final String PROP_FORCE_ROAMING = "telephony.test.forceRoaming";
    protected static final int PS_CS = 1;
    public static final int PS_DISABLED = 1002;
    public static final int PS_ENABLED = 1001;
    public static final int PS_NOTIFICATION = 888;
    protected static final int PS_ONLY = 0;
    protected static final String REGISTRATION_DENIED_AUTH = "Authentication Failure";
    protected static final String REGISTRATION_DENIED_GEN = "General";
    protected static final boolean RESET_PROFILE = SystemProperties.getBoolean("ro.hwpp_reset_profile", false);
    protected static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    public static final String UNACTIVATED_MIN2_VALUE = "000000";
    public static final String UNACTIVATED_MIN_VALUE = "1111110111";
    protected static final boolean VDBG = false;
    private static String data = null;
    protected static final boolean display_blank_ons = "true".equals(SystemProperties.get("ro.config.hw_no_display_ons", "false"));
    private String LOG_TAG = "ServiceStateTracker";
    private boolean hasUpdateCellLocByPS = false;
    protected boolean isCurrent3GPsCsAllowed = true;
    private boolean mAlarmSwitch = false;
    private final LocalLog mAttachLog = new LocalLog(10);
    protected RegistrantList mAttachedRegistrants = new RegistrantList();
    private CarrierServiceStateTracker mCSST;
    private RegistrantList mCdmaForSubscriptionInfoReadyRegistrants = new RegistrantList();
    private CdmaSubscriptionSourceManager mCdmaSSM;
    public CellLocation mCellLoc;
    protected CommandsInterface mCi;
    private final ContentResolver mCr;
    private String mCurDataSpn = null;
    private String mCurPlmn = null;
    private String mCurRegplmn = null;
    private boolean mCurShowPlmn = false;
    private boolean mCurShowSpn = false;
    private boolean mCurShowWifi = false;
    private String mCurSpn = null;
    private String mCurWifi = null;
    private String mCurrentCarrier = null;
    private int mCurrentOtaspMode = 0;
    private RegistrantList mDataRegStateOrRatChangedRegistrants = new RegistrantList();
    private boolean mDataRoaming = false;
    private RegistrantList mDataRoamingOffRegistrants = new RegistrantList();
    private RegistrantList mDataRoamingOnRegistrants = new RegistrantList();
    private Display mDefaultDisplay;
    private int mDefaultDisplayState = 0;
    private int mDefaultRoamingIndicator;
    protected boolean mDesiredPowerState;
    protected RegistrantList mDetachedRegistrants = new RegistrantList();
    private boolean mDeviceShuttingDown = false;
    private final DisplayListener mDisplayListener = new SstDisplayListener();
    private boolean mDoRecoveryMarker = false;
    protected boolean mDontPollSignalStrength = false;
    private ArrayList<Pair<Integer, Integer>> mEarfcnPairListForRsrpBoost = null;
    private boolean mEmergencyOnly = false;
    private boolean mGsmRoaming = false;
    private final HandlerThread mHandlerThread;
    private HbpcdUtils mHbpcdUtils = null;
    private int[] mHomeNetworkId = null;
    private int[] mHomeSystemId = null;
    private HwCustGsmServiceStateTracker mHwCustGsmServiceStateTracker;
    protected IccRecords mIccRecords = null;
    private boolean mImsRegistered = false;
    private boolean mImsRegistrationOnOff = false;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            ServiceStateTracker serviceStateTracker;
            if (intent.getAction().equals("android.intent.action.AIRPLANE_MODE")) {
                serviceStateTracker = ServiceStateTracker.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("intent: ");
                stringBuilder.append(intent.getAction());
                serviceStateTracker.loge(stringBuilder.toString());
                ServiceStateTracker.this.mCurPlmn = null;
                ServiceStateTracker.this.updateSpnDisplay();
                if (ServiceStateTracker.this.mHwCustGsmServiceStateTracker != null && intent.getBooleanExtra("state", false)) {
                    ServiceStateTracker.this.mHwCustGsmServiceStateTracker.clearPcoValue(ServiceStateTracker.this.mPhone);
                }
            } else if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                ServiceStateTracker.this.onCarrierConfigChanged();
            } else if (ServiceStateTracker.this.mPhone.isPhoneTypeGsm()) {
                if (intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                    ServiceStateTracker.this.updateSpnDisplay();
                } else if (intent.getAction().equals(ServiceStateTracker.ACTION_RADIO_OFF)) {
                    ServiceStateTracker.this.mAlarmSwitch = false;
                    ServiceStateTracker.this.powerOffRadioSafely(ServiceStateTracker.this.mPhone.mDcTracker);
                } else if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction()) || "android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction()) || ServiceStateTracker.ACTION_COMMFORCE.equals(intent.getAction()) || "android.intent.action.HEADSET_PLUG".equals(intent.getAction()) || "android.intent.action.PHONE_STATE".equals(intent.getAction())) {
                    ServiceStateTracker.this.mPhone.updateReduceSARState();
                }
            } else {
                serviceStateTracker = ServiceStateTracker.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Ignoring intent ");
                stringBuilder2.append(intent);
                stringBuilder2.append(" received on CDMA phone");
                serviceStateTracker.loge(stringBuilder2.toString());
            }
        }
    };
    private boolean mIsEriTextLoaded = false;
    private boolean mIsInPrl;
    private boolean mIsMinInfoReady = false;
    private boolean mIsSimReady = false;
    private boolean mIsSubscriptionFromRuim = false;
    private boolean mKeepNwSelManual = SystemProperties.getBoolean("ro.config.hw_keep_sel_manual", false);
    private List<CellInfo> mLastCellInfoList = null;
    private long mLastCellInfoListTime;
    private List<PhysicalChannelConfig> mLastPhysicalChannelConfigList = null;
    long mLastReceivedNITZReferenceTime;
    private SignalStrength mLastSignalStrength = null;
    private final LocaleTracker mLocaleTracker;
    private int mLteRsrpBoost = 0;
    private final Object mLteRsrpBoostLock = new Object();
    private int mMaxDataCalls = 1;
    private String mMdn;
    private String mMin;
    protected String mMlplVersion;
    protected String mMsplVersion;
    private RegistrantList mNetworkAttachedRegistrants = new RegistrantList();
    private RegistrantList mNetworkDetachedRegistrants = new RegistrantList();
    private CellLocation mNewCellLoc;
    private int mNewMaxDataCalls = 1;
    private int mNewReasonDataDenied = -1;
    private int mNewRejectCode;
    protected ServiceState mNewSS;
    private final NitzStateMachine mNitzState;
    private Notification mNotification;
    private final SstSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SstSubscriptionsChangedListener(this, null);
    private boolean mPendingRadioPowerOffAfterDataOff = false;
    private int mPendingRadioPowerOffAfterDataOffTag = 0;
    private final GsmCdmaPhone mPhone;
    private final LocalLog mPhoneTypeLog = new LocalLog(10);
    @VisibleForTesting
    public int[] mPollingContext;
    private boolean mPowerOffDelayNeed = true;
    private boolean mPreVowifiState = false;
    private int mPreferredNetworkType;
    private int mPrevSubId = -1;
    private String mPrlVersion;
    private RegistrantList mPsRestrictDisabledRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictEnabledRegistrants = new RegistrantList();
    private boolean mRadioDisabledByCarrier = false;
    private boolean mRadioOffByDoRecovery = false;
    private PendingIntent mRadioOffIntent = null;
    private final LocalLog mRadioPowerLog = new LocalLog(20);
    private final LocalLog mRatLog = new LocalLog(20);
    private final RatRatcheter mRatRatcheter;
    private int mReasonDataDenied = -1;
    private boolean mRecoverAutoSelectMode = false;
    private final SparseArray<NetworkRegistrationManager> mRegStateManagers = new SparseArray();
    private String mRegistrationDeniedReason;
    private int mRegistrationState = -1;
    private int mRejectCode;
    private boolean mReportedGprsNoReg;
    public RestrictedState mRestrictedState;
    private int mRoamingIndicator;
    private final LocalLog mRoamingLog = new LocalLog(10);
    private boolean mRplmnIsNull = false;
    public ServiceState mSS;
    protected SignalStrength mSignalStrength;
    private boolean mSimCardsLoaded = false;
    private boolean mSpnUpdatePending = false;
    private boolean mStartedGprsRegCheck;
    @VisibleForTesting
    public int mSubId = -1;
    private SubscriptionController mSubscriptionController;
    private SubscriptionManager mSubscriptionManager;
    private final TransportManager mTransportManager;
    protected UiccCardApplication mUiccApplcation = null;
    private UiccController mUiccController = null;
    protected boolean mVoiceCapable;
    private RegistrantList mVoiceRoamingOffRegistrants = new RegistrantList();
    private RegistrantList mVoiceRoamingOnRegistrants = new RegistrantList();
    private WakeLock mWakeLock = null;
    private boolean mWantContinuousLocationUpdates;
    private boolean mWantSingleLocationUpdate;

    private class CellInfoResult {
        List<CellInfo> list;
        Object lockObj;

        private CellInfoResult() {
            this.lockObj = new Object();
        }

        /* synthetic */ CellInfoResult(ServiceStateTracker x0, AnonymousClass1 x1) {
            this();
        }
    }

    public class SstDisplayListener implements DisplayListener {
        public void onDisplayAdded(int displayId) {
        }

        public void onDisplayRemoved(int displayId) {
        }

        public void onDisplayChanged(int displayId) {
            if (displayId == 0) {
                int oldState = ServiceStateTracker.this.mDefaultDisplayState;
                ServiceStateTracker.this.mDefaultDisplayState = ServiceStateTracker.this.mDefaultDisplay.getState();
                if (ServiceStateTracker.this.mDefaultDisplayState != oldState && ServiceStateTracker.this.mDefaultDisplayState == 2 && !ServiceStateTracker.this.mDontPollSignalStrength) {
                    ServiceStateTracker.this.sendMessage(ServiceStateTracker.this.obtainMessage(10));
                }
            }
        }
    }

    private class SstSubscriptionsChangedListener extends OnSubscriptionsChangedListener {
        public final AtomicInteger mPreviousSubId;

        private SstSubscriptionsChangedListener() {
            this.mPreviousSubId = new AtomicInteger(-1);
        }

        /* synthetic */ SstSubscriptionsChangedListener(ServiceStateTracker x0, AnonymousClass1 x1) {
            this();
        }

        public void onSubscriptionsChanged() {
            ServiceStateTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged");
            int subId = ServiceStateTracker.this.mPhone.getSubId();
            ServiceStateTracker.this.mPrevSubId = this.mPreviousSubId.get();
            if (this.mPreviousSubId.getAndSet(subId) != subId) {
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    Context context = ServiceStateTracker.this.mPhone.getContext();
                    ServiceStateTracker.this.mPhone.notifyPhoneStateChanged();
                    ServiceStateTracker.this.mPhone.notifyCallForwardingIndicator();
                    ServiceStateTracker.this.mPhone.sendSubscriptionSettings(context.getResources().getBoolean(17957110) ^ 1);
                    ServiceStateTracker.this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(ServiceStateTracker.this.mSS.getRilDataRadioTechnology()));
                    if (ServiceStateTracker.this.mSpnUpdatePending) {
                        ServiceStateTracker.this.mSubscriptionController.setPlmnSpn(ServiceStateTracker.this.mPhone.getPhoneId(), ServiceStateTracker.this.mCurShowPlmn, ServiceStateTracker.this.mCurPlmn, ServiceStateTracker.this.mCurShowSpn, ServiceStateTracker.this.mCurSpn);
                        ServiceStateTracker.this.mSpnUpdatePending = false;
                    }
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                    String oldNetworkSelection = sp.getString(Phone.NETWORK_SELECTION_KEY, "");
                    String oldNetworkSelectionName = sp.getString(Phone.NETWORK_SELECTION_NAME_KEY, "");
                    String oldNetworkSelectionShort = sp.getString(Phone.NETWORK_SELECTION_SHORT_KEY, "");
                    if (!(TextUtils.isEmpty(oldNetworkSelection) && TextUtils.isEmpty(oldNetworkSelectionName) && TextUtils.isEmpty(oldNetworkSelectionShort))) {
                        Editor editor = sp.edit();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(Phone.NETWORK_SELECTION_KEY);
                        stringBuilder.append(subId);
                        editor.putString(stringBuilder.toString(), oldNetworkSelection);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(Phone.NETWORK_SELECTION_NAME_KEY);
                        stringBuilder.append(subId);
                        editor.putString(stringBuilder.toString(), oldNetworkSelectionName);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(Phone.NETWORK_SELECTION_SHORT_KEY);
                        stringBuilder.append(subId);
                        editor.putString(stringBuilder.toString(), oldNetworkSelectionShort);
                        editor.remove(Phone.NETWORK_SELECTION_KEY);
                        editor.remove(Phone.NETWORK_SELECTION_NAME_KEY);
                        editor.remove(Phone.NETWORK_SELECTION_SHORT_KEY);
                        editor.commit();
                    }
                    ServiceStateTracker.this.updateSpnDisplay();
                }
                if (ServiceStateTracker.this.mSubscriptionController.getSlotIndex(subId) == -1) {
                    ServiceStateTracker.this.sendMessage(ServiceStateTracker.this.obtainMessage(52));
                }
            }
        }
    }

    static {
        boolean z = (HW_OPTA == 735 && HW_OPTB == 156) || ENABLE_DEMO;
        ISDEMO = z;
    }

    public ServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci) {
        int transportType;
        this.mNitzState = TelephonyComponentFactory.getInstance().makeNitzStateMachine(phone);
        this.mPhone = phone;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.LOG_TAG);
        stringBuilder.append("[SUB");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        this.LOG_TAG = stringBuilder.toString();
        this.mCi = ci;
        this.mRatRatcheter = new RatRatcheter(this.mPhone);
        this.mVoiceCapable = this.mPhone.getContext().getResources().getBoolean(17957068);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 42, null);
        this.mCi.setOnSignalStrengthUpdate(this, 12, null);
        this.mCi.registerForCellInfoList(this, 44, null);
        this.mCi.registerForPhysicalChannelConfiguration(this, 55, null);
        this.mSubscriptionController = SubscriptionController.getInstance();
        this.mSubscriptionManager = SubscriptionManager.from(phone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mRestrictedState = new RestrictedState();
        this.mTransportManager = new TransportManager();
        for (Integer transportType2 : this.mTransportManager.getAvailableTransports()) {
            transportType = transportType2.intValue();
            this.mRegStateManagers.append(transportType, new NetworkRegistrationManager(transportType, phone));
            ((NetworkRegistrationManager) this.mRegStateManagers.get(transportType)).registerForNetworkRegistrationStateChanged(this, 2, null);
        }
        this.mHandlerThread = new HandlerThread(LocaleTracker.class.getSimpleName());
        this.mHandlerThread.start();
        this.mLocaleTracker = TelephonyComponentFactory.getInstance().makeLocaleTracker(this.mPhone, this.mHandlerThread.getLooper());
        this.mCi.registerForImsNetworkStateChanged(this, 46, null);
        this.mCi.registerForRadioStateChanged(this, 1, null);
        this.mCi.setOnNITZTime(this, 11, null);
        this.mCr = phone.getContext().getContentResolver();
        int airplaneMode = Global.getInt(this.mCr, "airplane_mode_on", 0);
        transportType = Global.getInt(this.mCr, "enable_cellular_on_boot", 1);
        boolean z = transportType > 0 && airplaneMode <= 0;
        this.mDesiredPowerState = z;
        LocalLog localLog = this.mRadioPowerLog;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("init : airplane mode = ");
        stringBuilder2.append(airplaneMode);
        stringBuilder2.append(" enableCellularOnBoot = ");
        stringBuilder2.append(transportType);
        localLog.log(stringBuilder2.toString());
        setSignalStrengthDefaultValues();
        this.mPhone.getCarrierActionAgent().registerForCarrierAction(1, this, 51, null, false);
        data = System.getString(this.mCr, "enable_get_location");
        this.mCi.getSignalStrength(obtainMessage(3));
        Context context = this.mPhone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        context.registerReceiver(this.mIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(ACTION_RADIO_OFF);
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        context.registerReceiver(this.mIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(ACTION_COMMFORCE);
        context.registerReceiver(this.mIntentReceiver, filter, PERMISSION_COMM_FORCE, null);
        this.mHwCustGsmServiceStateTracker = (HwCustGsmServiceStateTracker) HwCustUtils.createObj(HwCustGsmServiceStateTracker.class, new Object[]{this.mPhone});
        if (this.mHwCustGsmServiceStateTracker != null && (this.mHwCustGsmServiceStateTracker.isDataOffForbidLTE() || this.mHwCustGsmServiceStateTracker.isDataOffbyRoamAndData())) {
            this.mHwCustGsmServiceStateTracker.initOnce(this.mPhone, this.mCi);
        }
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(this.mIntentReceiver, filter2);
        this.mPhone.notifyOtaspChanged(0);
        this.mCi.setOnRestrictedStateChanged(this, 23, null);
        updatePhoneType();
        this.mCi.registerForOffOrNotAvailable(this, 56, null);
        this.mCSST = new CarrierServiceStateTracker(phone, this);
        registerForNetworkAttached(this.mCSST, 101, null);
        registerForNetworkDetached(this.mCSST, 102, null);
        registerForDataConnectionAttached(this.mCSST, 103, null);
        registerForDataConnectionDetached(this.mCSST, AbstractPhoneBase.EVENT_ECC_NUM, null);
        initDisplay();
    }

    private void initDisplay() {
        DisplayManager dm = (DisplayManager) this.mPhone.getContext().getSystemService("display");
        dm.registerDisplayListener(this.mDisplayListener, null);
        this.mDefaultDisplay = dm.getDisplay(0);
    }

    @VisibleForTesting
    public void updatePhoneType() {
        if (this.mSS != null && this.mSS.getVoiceRoaming()) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (this.mSS != null && this.mSS.getDataRoaming()) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        }
        if (this.mSS != null && this.mSS.getVoiceRegState() == 0) {
            this.mNetworkDetachedRegistrants.notifyRegistrants();
        }
        if (this.mSS != null && this.mSS.getDataRegState() == 0) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        this.mSS = new ServiceState();
        this.mNewSS = new ServiceState();
        this.mLastCellInfoListTime = 0;
        this.mLastCellInfoList = null;
        this.mSignalStrength = new SignalStrength();
        this.mStartedGprsRegCheck = false;
        this.mReportedGprsNoReg = false;
        this.mMdn = null;
        this.mMin = null;
        this.mPrlVersion = null;
        this.mIsMinInfoReady = false;
        this.mNitzState.handleNetworkUnavailable();
        cancelPollState();
        if (this.mPhone.isPhoneTypeGsm()) {
            if (this.mCdmaSSM != null) {
                this.mCdmaSSM.dispose(this);
            }
            this.mCi.unregisterForCdmaPrlChanged(this);
            this.mPhone.unregisterForEriFileLoaded(this);
            this.mCi.unregisterForCdmaOtaProvision(this);
            this.mPhone.unregisterForSimRecordsLoaded(this);
            this.mCellLoc = new GsmCellLocation();
            this.mNewCellLoc = new GsmCellLocation();
        } else {
            boolean z = true;
            if (true == HwModemCapability.isCapabilitySupport(9)) {
                this.mCurPlmn = null;
            }
            this.mPhone.registerForSimRecordsLoaded(this, 16, null);
            this.mCellLoc = new CdmaCellLocation();
            this.mNewCellLoc = new CdmaCellLocation();
            this.mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(this.mPhone.getContext(), this.mCi, this, 39, null);
            if (this.mCdmaSSM.getCdmaSubscriptionSource() != 0) {
                z = false;
            }
            this.mIsSubscriptionFromRuim = z;
            this.mCi.registerForCdmaPrlChanged(this, 40, null);
            this.mPhone.registerForEriFileLoaded(this, 36, null);
            this.mCi.registerForCdmaOtaProvision(this, 37, null);
            this.mHbpcdUtils = new HbpcdUtils(this.mPhone.getContext());
            updateOtaspState();
        }
        onUpdateIccAvailability();
        this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(0));
        this.mCi.getSignalStrength(obtainMessage(3));
        sendMessage(obtainMessage(50));
        logPhoneTypeChange();
        notifyDataRegStateRilRadioTechnologyChanged();
    }

    @VisibleForTesting
    public void requestShutdown() {
        if (!this.mDeviceShuttingDown) {
            this.mDeviceShuttingDown = true;
            this.mDesiredPowerState = false;
            setPowerStateToDesired();
        }
    }

    public void dispose() {
        this.mCi.unSetOnSignalStrengthUpdate(this);
        this.mUiccController.unregisterForIccChanged(this);
        this.mCi.unregisterForCellInfoList(this);
        this.mCi.unregisterForPhysicalChannelConfiguration(this);
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mHandlerThread.quit();
        this.mCi.unregisterForImsNetworkStateChanged(this);
        this.mPhone.getCarrierActionAgent().unregisterForCarrierAction(this, 1);
        if (this.mUiccApplcation != null) {
            this.mUiccApplcation.unregisterForGetAdDone(this);
        }
        unregisterForRuimEvents();
        HwTelephonyFactory.getHwNetworkManager().dispose(this);
        if (this.mHwCustGsmServiceStateTracker != null && (this.mHwCustGsmServiceStateTracker.isDataOffForbidLTE() || this.mHwCustGsmServiceStateTracker.isDataOffbyRoamAndData())) {
            this.mHwCustGsmServiceStateTracker.dispose(this.mPhone);
        }
        if (this.mCSST != null) {
            this.mCSST.dispose();
            this.mCSST = null;
        }
    }

    public boolean getDesiredPowerState() {
        return this.mDesiredPowerState;
    }

    public boolean getPowerStateFromCarrier() {
        return this.mRadioDisabledByCarrier ^ 1;
    }

    public long getLastCellInfoListTime() {
        return this.mLastCellInfoListTime;
    }

    public void setDesiredPowerState(boolean dps) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDesiredPowerState, dps = ");
        stringBuilder.append(dps);
        log(stringBuilder.toString());
        this.mDesiredPowerState = dps;
    }

    protected boolean notifySignalStrength() {
        boolean notified = false;
        if (this.mSignalStrength.equals(this.mLastSignalStrength)) {
            return false;
        }
        try {
            this.mPhone.notifySignalStrength();
            notified = true;
            this.mLastSignalStrength = this.mSignalStrength;
            return true;
        } catch (NullPointerException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateSignalStrength() Phone already destroyed: ");
            stringBuilder.append(ex);
            stringBuilder.append("SignalStrength not notified");
            loge(stringBuilder.toString());
            return notified;
        }
    }

    protected void notifyDataRegStateRilRadioTechnologyChanged() {
        int rat = this.mSS.getRilDataRadioTechnology();
        int drs = this.mSS.getDataRegState();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyDataRegStateRilRadioTechnologyChanged: drs=");
        stringBuilder.append(drs);
        stringBuilder.append(" rat=");
        stringBuilder.append(rat);
        log(stringBuilder.toString());
        this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(rat));
        this.mDataRegStateOrRatChangedRegistrants.notifyResult(new Pair(Integer.valueOf(drs), Integer.valueOf(rat)));
    }

    protected void useDataRegStateForDataOnlyDevices() {
        if (!this.mVoiceCapable) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("useDataRegStateForDataOnlyDevice: VoiceRegState=");
            stringBuilder.append(this.mNewSS.getVoiceRegState());
            stringBuilder.append(" DataRegState=");
            stringBuilder.append(this.mNewSS.getDataRegState());
            log(stringBuilder.toString());
            this.mNewSS.setVoiceRegState(this.mNewSS.getDataRegState());
        }
    }

    protected void updatePhoneObject() {
        if (this.mPhone.getContext().getResources().getBoolean(17957050)) {
            boolean isRegistered = this.mSS.getVoiceRegState() == 0 || this.mSS.getVoiceRegState() == 2;
            if (isRegistered) {
                this.mPhone.updatePhoneObject(this.mSS.getRilVoiceRadioTechnology());
            } else {
                log("updatePhoneObject: Ignore update");
            }
        }
    }

    public void registerForVoiceRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceRoamingOnRegistrants.add(r);
        if (this.mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOn(Handler h) {
        this.mVoiceRoamingOnRegistrants.remove(h);
    }

    public void registerForVoiceRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceRoamingOffRegistrants.add(r);
        if (!this.mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOff(Handler h) {
        this.mVoiceRoamingOffRegistrants.remove(h);
    }

    public void registerForDataRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataRoamingOnRegistrants.add(r);
        if (this.mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOn(Handler h) {
        this.mDataRoamingOnRegistrants.remove(h);
    }

    public void registerForDataRoamingOff(Handler h, int what, Object obj, boolean notifyNow) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataRoamingOffRegistrants.add(r);
        if (notifyNow && !this.mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOff(Handler h) {
        this.mDataRoamingOffRegistrants.remove(h);
    }

    public void reRegisterNetwork(Message onComplete) {
        if (HwModemCapability.isCapabilitySupport(7) && this.mPhone.isPhoneTypeGsm()) {
            log("modem support rettach, rettach");
            int i = 0;
            this.mCi.dataConnectionDetach(14 == this.mSS.getRilDataRadioTechnology() ? 1 : 0, null);
            CommandsInterface commandsInterface = this.mCi;
            if (14 == this.mSS.getRilDataRadioTechnology()) {
                i = 1;
            }
            commandsInterface.dataConnectionAttach(i, null);
            return;
        }
        log("modem not support rettach, reRegisterNetwork");
        this.mCi.getPreferredNetworkType(obtainMessage(19, onComplete));
    }

    public void setRadioPower(boolean power) {
        this.mDesiredPowerState = power;
        setPowerStateToDesired();
    }

    protected void setPowerStateToDesired(boolean power, Message msg) {
        if (Global.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", 0) <= 0) {
            this.mDesiredPowerState = power;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mDesiredPowerState = ");
        stringBuilder.append(this.mDesiredPowerState);
        log(stringBuilder.toString());
        getCaller();
        this.mCi.setRadioPower(this.mDesiredPowerState, msg);
    }

    public void setRadioPower(boolean power, Message msg) {
        setPowerStateToDesired(power, msg);
    }

    public void setRadioPowerFromCarrier(boolean enable) {
        this.mRadioDisabledByCarrier = enable ^ 1;
        setPowerStateToDesired();
    }

    public void enableSingleLocationUpdate() {
        boolean containPackage = isContainPackage(data, getAppName(Binder.getCallingPid()));
        if ((!HwTelephonyFactory.getHwNetworkManager().isCustScreenOff(this.mPhone) || containPackage) && !this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mWantSingleLocationUpdate = true;
            this.mCi.setLocationUpdates(true, obtainMessage(18));
        }
    }

    public void enableLocationUpdates() {
        if (!HwTelephonyFactory.getHwNetworkManager().isCustScreenOff(this.mPhone) && !this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mWantContinuousLocationUpdates = true;
            this.mCi.setLocationUpdates(true, obtainMessage(18));
        }
    }

    protected void disableSingleLocationUpdate() {
        this.mWantSingleLocationUpdate = false;
        if (!this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mCi.setLocationUpdates(false, null);
        }
    }

    public void disableLocationUpdates() {
        this.mWantContinuousLocationUpdates = false;
        if (!this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mCi.setLocationUpdates(false, null);
        }
    }

    private void processCellLocationInfo(CellLocation cellLocation, CellIdentity cellIdentity) {
        int baseStationId;
        int baseStationLatitude;
        int baseStationLongitude;
        int systemId;
        if (!this.mPhone.isPhoneTypeGsm()) {
            baseStationId = -1;
            baseStationLatitude = KeepaliveStatus.INVALID_HANDLE;
            baseStationLongitude = KeepaliveStatus.INVALID_HANDLE;
            systemId = 0;
            int networkId = 0;
            if (cellIdentity != null && cellIdentity.getType() == 2) {
                baseStationId = ((CellIdentityCdma) cellIdentity).getBasestationId();
                baseStationLatitude = ((CellIdentityCdma) cellIdentity).getLatitude();
                baseStationLongitude = ((CellIdentityCdma) cellIdentity).getLongitude();
                systemId = ((CellIdentityCdma) cellIdentity).getSystemId();
                networkId = ((CellIdentityCdma) cellIdentity).getNetworkId();
            }
            int systemId2 = systemId;
            int networkId2 = networkId;
            if (baseStationLatitude == 0 && baseStationLongitude == 0) {
                baseStationLatitude = KeepaliveStatus.INVALID_HANDLE;
                baseStationLongitude = KeepaliveStatus.INVALID_HANDLE;
            }
            ((CdmaCellLocation) cellLocation).setCellLocationData(baseStationId, baseStationLatitude, baseStationLongitude, systemId2, networkId2);
        } else if (!this.mPhone.isCTSimCard(this.mPhone.getPhoneId()) || !this.hasUpdateCellLocByPS) {
            baseStationId = -1;
            baseStationLatitude = -1;
            baseStationLongitude = -1;
            if (cellIdentity != null) {
                systemId = cellIdentity.getType();
                if (systemId != 1) {
                    switch (systemId) {
                        case 3:
                            baseStationLatitude = ((CellIdentityLte) cellIdentity).getCi();
                            baseStationLongitude = ((CellIdentityLte) cellIdentity).getTac();
                            break;
                        case 4:
                            baseStationLatitude = ((CellIdentityWcdma) cellIdentity).getCid();
                            baseStationLongitude = ((CellIdentityWcdma) cellIdentity).getLac();
                            baseStationId = ((CellIdentityWcdma) cellIdentity).getPsc();
                            break;
                        case 5:
                            baseStationLatitude = ((CellIdentityTdscdma) cellIdentity).getCid();
                            baseStationLongitude = ((CellIdentityTdscdma) cellIdentity).getLac();
                            break;
                    }
                }
                baseStationLatitude = ((CellIdentityGsm) cellIdentity).getCid();
                baseStationLongitude = ((CellIdentityGsm) cellIdentity).getLac();
            }
            if (HwTelephonyFactory.getHwNetworkManager().isUpdateLacAndCid(this, this.mPhone, baseStationLatitude) || this.mHwCustGsmServiceStateTracker.isUpdateLacAndCidCust(this)) {
                ((GsmCellLocation) cellLocation).setLacAndCid(baseStationLongitude, baseStationLatitude);
                ((GsmCellLocation) cellLocation).setPsc(baseStationId);
            }
        }
    }

    private int getLteEarfcn(CellIdentity cellIdentity) {
        if (cellIdentity == null || cellIdentity.getType() != 3) {
            return -1;
        }
        return ((CellIdentityLte) cellIdentity).getEarfcn();
    }

    private boolean getRecoverAutoModeFutureState() {
        int subId = this.mPhone.getSubId();
        Boolean valueFromCard = (Boolean) HwCfgFilePolicy.getValue("recover_auto_mode", subId, Boolean.class);
        boolean valueFromProp = FEATURE_RECOVER_AUTO_NETWORK_MODE;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getRecoverAutoModeFutureState, subId:");
        stringBuilder.append(subId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        log(stringBuilder.toString());
        return valueFromCard != null ? valueFromCard.booleanValue() : valueFromProp;
    }

    public void handleMessage(Message msg) {
        if (this.mHwCustGsmServiceStateTracker == null || !this.mHwCustGsmServiceStateTracker.handleMessage(msg)) {
            int i = msg.what;
            if (i != 100) {
                boolean z = false;
                if (i != 1002) {
                    switch (i) {
                        case 1:
                            if (!this.mDesiredPowerState) {
                                break;
                            }
                            setDoRecoveryMarker(false);
                            handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                            queueNextSignalStrengthPoll();
                            setPowerStateToDesired();
                            modemTriggeredPollState();
                            break;
                        case 2:
                            modemTriggeredPollState();
                            break;
                        case 3:
                            if (this.mCi.getRadioState().isOn()) {
                                onSignalStrengthResult((AsyncResult) msg.obj);
                                queueNextSignalStrengthPoll();
                                break;
                            }
                            return;
                        case 4:
                        case 5:
                        case 6:
                            handlePollStateResult(msg.what, msg.obj);
                            break;
                        default:
                            boolean isNeedUpdate;
                            StringBuilder stringBuilder;
                            AsyncResult ar;
                            switch (i) {
                                case 10:
                                    this.mCi.getSignalStrength(obtainMessage(3));
                                    break;
                                case 11:
                                    isNeedUpdate = this.mNitzState.getNitzSpaceTime() > 1800000;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("isNeedUpdate: ");
                                    stringBuilder.append(isNeedUpdate);
                                    stringBuilder.append("  nitzSpaceTime : ");
                                    stringBuilder.append(this.mNitzState.getNitzSpaceTime());
                                    stringBuilder.append("  elapsedRealtime  :");
                                    stringBuilder.append(SystemClock.elapsedRealtime());
                                    log(stringBuilder.toString());
                                    if (isNeedUpdate || HwTelephonyFactory.getHwNetworkManager().needGsmUpdateNITZTime(this, this.mPhone)) {
                                        AsyncResult ar2 = msg.obj;
                                        String nitzString = ((Object[]) ar2.result)[0];
                                        long nitzReceiveTime = ((Long) ((Object[]) ar2.result)[1]).longValue();
                                        this.mLastReceivedNITZReferenceTime = nitzReceiveTime;
                                        setTimeFromNITZString(nitzString, nitzReceiveTime);
                                        break;
                                    }
                                    return;
                                case 12:
                                    ar = (AsyncResult) msg.obj;
                                    this.mDontPollSignalStrength = true;
                                    onSignalStrengthResult(ar);
                                    break;
                                default:
                                    StringBuilder stringBuilder2;
                                    switch (i) {
                                        case 14:
                                            log("EVENT_POLL_STATE_NETWORK_SELECTION_MODE");
                                            ar = (AsyncResult) msg.obj;
                                            if (!this.mPhone.isPhoneTypeGsm()) {
                                                if (ar.exception == null && ar.result != null) {
                                                    if (ar.result[0] == 1) {
                                                        this.mPhone.setNetworkSelectionModeAutomatic(null);
                                                        break;
                                                    }
                                                }
                                                log("Unable to getNetworkSelectionMode");
                                                break;
                                            }
                                            handlePollStateResult(msg.what, ar);
                                            break;
                                            break;
                                        case 15:
                                            ar = msg.obj;
                                            if (ar.exception == null) {
                                                processCellLocationInfo(this.mCellLoc, ((NetworkRegistrationState) ar.result).getCellIdentity());
                                                this.mPhone.notifyLocationChanged();
                                            }
                                            disableSingleLocationUpdate();
                                            break;
                                        case 16:
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("EVENT_SIM_RECORDS_LOADED: what=");
                                            stringBuilder2.append(msg.what);
                                            log(stringBuilder2.toString());
                                            this.mSimCardsLoaded = true;
                                            updatePhoneObject();
                                            updateOtaspState();
                                            if (this.mPhone.isPhoneTypeGsm()) {
                                                updateSpnDisplay();
                                                if (this.mHwCustGsmServiceStateTracker != null) {
                                                    this.mHwCustGsmServiceStateTracker.updateRomingVoicemailNumber(this.mSS);
                                                    break;
                                                }
                                            }
                                            break;
                                        case 17:
                                            this.mOnSubscriptionsChangedListener.mPreviousSubId.set(-1);
                                            this.mPrevSubId = -1;
                                            this.mIsSimReady = true;
                                            this.mSimCardsLoaded = false;
                                            log("skip setPreferredNetworkType when EVENT_SIM_READY");
                                            isNeedUpdate = this.mPhone.getContext().getResources().getBoolean(17957110);
                                            if (getRecoverAutoModeFutureState()) {
                                                log("Feature recover network mode automatic is on..");
                                                this.mRecoverAutoSelectMode = true;
                                            } else if (!isNeedUpdate) {
                                                if (!HwModemCapability.isCapabilitySupport(4) || this.mKeepNwSelManual) {
                                                    this.mPhone.restoreSavedNetworkSelection(null);
                                                } else {
                                                    log("Modem can select network auto with manual mode");
                                                }
                                            }
                                            pollState();
                                            queueNextSignalStrengthPoll();
                                            break;
                                        case 18:
                                            if (((AsyncResult) msg.obj).exception == null) {
                                                ((NetworkRegistrationManager) this.mRegStateManagers.get(1)).getNetworkRegistrationState(1, obtainMessage(15, null));
                                                break;
                                            }
                                            break;
                                        case 19:
                                            ar = (AsyncResult) msg.obj;
                                            if (ar.exception == null) {
                                                this.mPreferredNetworkType = ((int[]) ar.result)[0];
                                            } else {
                                                this.mPreferredNetworkType = 7;
                                            }
                                            this.mCi.setPreferredNetworkType(7, obtainMessage(20, ar.userObj));
                                            break;
                                        case 20:
                                            this.mCi.setPreferredNetworkType(this.mPreferredNetworkType, obtainMessage(21, ((AsyncResult) msg.obj).userObj));
                                            break;
                                        case 21:
                                            ar = (AsyncResult) msg.obj;
                                            if (ar.userObj != null) {
                                                AsyncResult.forMessage((Message) ar.userObj).exception = ar.exception;
                                                ((Message) ar.userObj).sendToTarget();
                                                break;
                                            }
                                            break;
                                        case 22:
                                            if (!(!this.mPhone.isPhoneTypeGsm() || this.mSS == null || isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState()))) {
                                                this.mPhone.getCellLocation();
                                                this.mReportedGprsNoReg = true;
                                            }
                                            this.mStartedGprsRegCheck = false;
                                            break;
                                        case 23:
                                            if (this.mPhone.isPhoneTypeGsm()) {
                                                log("EVENT_RESTRICTED_STATE_CHANGED");
                                                onRestrictedStateChanged(msg.obj);
                                                break;
                                            }
                                            break;
                                        default:
                                            String prlVersion;
                                            switch (i) {
                                                case 26:
                                                    if (this.mPhone.getLteOnCdmaMode() == 1) {
                                                        log("Receive EVENT_RUIM_READY");
                                                        pollState();
                                                    } else {
                                                        log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                                                        getSubscriptionInfoAndStartPollingThreads();
                                                    }
                                                    this.mCi.getNetworkSelectionMode(obtainMessage(14));
                                                    break;
                                                case 27:
                                                    if (!this.mPhone.isPhoneTypeGsm()) {
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("EVENT_RUIM_RECORDS_LOADED: what=");
                                                        stringBuilder2.append(msg.what);
                                                        log(stringBuilder2.toString());
                                                        updatePhoneObject();
                                                        if (!this.mPhone.isPhoneTypeCdma()) {
                                                            RuimRecords ruim = this.mIccRecords;
                                                            if (ruim != null) {
                                                                if (ruim.isProvisioned()) {
                                                                    this.mMdn = ruim.getMdn();
                                                                    this.mMin = ruim.getMin();
                                                                    parseSidNid(ruim.getSid(), ruim.getNid());
                                                                    prlVersion = ruim.getPrlVersion();
                                                                    if (prlVersion != null) {
                                                                        prlVersion = prlVersion.trim();
                                                                        if (!("".equals(prlVersion) || "65535".equals(prlVersion))) {
                                                                            this.mPrlVersion = prlVersion;
                                                                            SystemProperties.set("persist.radio.hwprlversion", this.mPrlVersion);
                                                                        }
                                                                    }
                                                                    this.mIsMinInfoReady = true;
                                                                }
                                                                updateOtaspState();
                                                                notifyCdmaSubscriptionInfoReady();
                                                            }
                                                            pollState();
                                                            break;
                                                        }
                                                        updateSpnDisplay();
                                                        break;
                                                    }
                                                    break;
                                                default:
                                                    StringBuilder stringBuilder3;
                                                    switch (i) {
                                                        case 34:
                                                            if (!this.mPhone.isPhoneTypeGsm()) {
                                                                ar = (AsyncResult) msg.obj;
                                                                if (ar.exception == null) {
                                                                    String[] cdmaSubscription = ar.result;
                                                                    if (cdmaSubscription != null && cdmaSubscription.length >= 5) {
                                                                        if (cdmaSubscription[0] != null) {
                                                                            this.mMdn = cdmaSubscription[0];
                                                                        }
                                                                        parseSidNid(cdmaSubscription[1], cdmaSubscription[2]);
                                                                        if (cdmaSubscription[3] != null) {
                                                                            this.mMin = cdmaSubscription[3];
                                                                        }
                                                                        if (cdmaSubscription[4] != null) {
                                                                            this.mPrlVersion = cdmaSubscription[4];
                                                                            SystemProperties.set("persist.radio.hwprlversion", this.mPrlVersion);
                                                                        }
                                                                        if (this.mMdn != null) {
                                                                            log("GET_CDMA_SUBSCRIPTION: MDN = ****");
                                                                        }
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("ril.csim.mlpl_mspl_ver");
                                                                        stringBuilder.append(this.mPhone.getPhoneId());
                                                                        prlVersion = SystemProperties.get(stringBuilder.toString());
                                                                        if (prlVersion != null) {
                                                                            String[] arrayMlplMspl = prlVersion.split(",");
                                                                            if (arrayMlplMspl.length >= 2) {
                                                                                this.mMlplVersion = arrayMlplMspl[0];
                                                                                this.mMsplVersion = arrayMlplMspl[1];
                                                                            }
                                                                        }
                                                                        stringBuilder3 = new StringBuilder();
                                                                        stringBuilder3.append("GET_CDMA_SUBSCRIPTION: mMlplVersion=");
                                                                        stringBuilder3.append(this.mMlplVersion);
                                                                        stringBuilder3.append(" mMsplVersion=");
                                                                        stringBuilder3.append(this.mMsplVersion);
                                                                        log(stringBuilder3.toString());
                                                                        this.mIsMinInfoReady = true;
                                                                        updateOtaspState();
                                                                        notifyCdmaSubscriptionInfoReady();
                                                                        if (!this.mIsSubscriptionFromRuim && this.mIccRecords != null) {
                                                                            log("GET_CDMA_SUBSCRIPTION set imsi in mIccRecords");
                                                                            this.mIccRecords.setImsi(getImsi());
                                                                            break;
                                                                        }
                                                                        log("GET_CDMA_SUBSCRIPTION either mIccRecords is null or NV type device - not setting Imsi in mIccRecords");
                                                                        break;
                                                                    }
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("GET_CDMA_SUBSCRIPTION: error parsing cdmaSubscription params num=");
                                                                    stringBuilder.append(cdmaSubscription.length);
                                                                    log(stringBuilder.toString());
                                                                    break;
                                                                }
                                                            }
                                                            break;
                                                        case 35:
                                                            updatePhoneObject();
                                                            this.mCi.getNetworkSelectionMode(obtainMessage(14));
                                                            getSubscriptionInfoAndStartPollingThreads();
                                                            break;
                                                        case 36:
                                                            log("ERI file has been loaded, repolling.");
                                                            pollState();
                                                            break;
                                                        case 37:
                                                            ar = (AsyncResult) msg.obj;
                                                            if (ar.exception == null) {
                                                                int otaStatus = ((int[]) ar.result)[0];
                                                                if (otaStatus == 8 || otaStatus == 10) {
                                                                    log("EVENT_OTA_PROVISION_STATUS_CHANGE: Complete, Reload MDN");
                                                                    this.mCi.getCDMASubscription(obtainMessage(34));
                                                                    break;
                                                                }
                                                            }
                                                            break;
                                                        case 38:
                                                            synchronized (this) {
                                                                if (this.mPendingRadioPowerOffAfterDataOff && msg.arg1 == this.mPendingRadioPowerOffAfterDataOffTag) {
                                                                    log("EVENT_SET_RADIO_OFF, turn radio off now.");
                                                                    hangupAndPowerOff();
                                                                    this.mPendingRadioPowerOffAfterDataOffTag++;
                                                                    this.mPendingRadioPowerOffAfterDataOff = false;
                                                                } else {
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("EVENT_SET_RADIO_OFF is stale arg1=");
                                                                    stringBuilder2.append(msg.arg1);
                                                                    stringBuilder2.append("!= tag=");
                                                                    stringBuilder2.append(this.mPendingRadioPowerOffAfterDataOffTag);
                                                                    log(stringBuilder2.toString());
                                                                }
                                                                releaseWakeLock();
                                                            }
                                                            break;
                                                        case 39:
                                                            handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                                                            break;
                                                        case 40:
                                                            ar = (AsyncResult) msg.obj;
                                                            if (ar.exception == null) {
                                                                this.mPrlVersion = Integer.toString(ar.result[0]);
                                                                SystemProperties.set("persist.radio.hwprlversion", this.mPrlVersion);
                                                                break;
                                                            }
                                                            break;
                                                        default:
                                                            switch (i) {
                                                                case 42:
                                                                    onUpdateIccAvailability();
                                                                    if (!(this.mUiccApplcation == null || this.mUiccApplcation.getState() == AppState.APPSTATE_READY)) {
                                                                        this.mIsSimReady = false;
                                                                        updateSpnDisplay();
                                                                        break;
                                                                    }
                                                                case 43:
                                                                    ar = (AsyncResult) msg.obj;
                                                                    if (ar.userObj instanceof AsyncResult) {
                                                                        log("EVENT_GET_CELL_INFO_LIST userObj is AsyncResult!");
                                                                        ar = ar.userObj;
                                                                    }
                                                                    if (!(ar.userObj instanceof CellInfoResult)) {
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("EVENT_GET_CELL_INFO_LIST userObj:");
                                                                        stringBuilder.append(ar.userObj);
                                                                        log(stringBuilder.toString());
                                                                        break;
                                                                    }
                                                                    CellInfoResult result = ar.userObj;
                                                                    synchronized (result.lockObj) {
                                                                        if (ar.exception != null) {
                                                                            stringBuilder3 = new StringBuilder();
                                                                            stringBuilder3.append("EVENT_GET_CELL_INFO_LIST: error ret null, e=");
                                                                            stringBuilder3.append(ar.exception);
                                                                            log(stringBuilder3.toString());
                                                                            result.list = null;
                                                                        } else {
                                                                            result.list = (List) ar.result;
                                                                        }
                                                                        this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                                                                        this.mLastCellInfoList = result.list;
                                                                        result.lockObj.notify();
                                                                    }
                                                                    break;
                                                                case 44:
                                                                    ar = (AsyncResult) msg.obj;
                                                                    if (ar.exception == null) {
                                                                        List<CellInfo> list = ar.result;
                                                                        this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                                                                        this.mLastCellInfoList = list;
                                                                        this.mPhone.notifyCellInfo(list);
                                                                        break;
                                                                    }
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("EVENT_UNSOL_CELL_INFO_LIST: error ignoring, e=");
                                                                    stringBuilder.append(ar.exception);
                                                                    log(stringBuilder.toString());
                                                                    break;
                                                                case 45:
                                                                    log("EVENT_CHANGE_IMS_STATE:");
                                                                    setPowerStateToDesired();
                                                                    break;
                                                                case 46:
                                                                    this.mCi.getImsRegistrationState(obtainMessage(47));
                                                                    break;
                                                                case 47:
                                                                    ar = msg.obj;
                                                                    if (ar.exception == null) {
                                                                        if (ar.result[0] == 1) {
                                                                            z = true;
                                                                        }
                                                                        this.mImsRegistered = z;
                                                                    }
                                                                    if (this.mPhone.isCTSimCard(this.mPhone.getPhoneId())) {
                                                                        pollState();
                                                                        break;
                                                                    }
                                                                    break;
                                                                case 48:
                                                                    log("EVENT_IMS_CAPABILITY_CHANGED");
                                                                    updateSpnDisplay();
                                                                    if (!(this.mPhone == null || this.mPhone.getImsPhone() == null)) {
                                                                        isNeedUpdate = this.mPreVowifiState;
                                                                        this.mPreVowifiState = this.mPhone.getImsPhone().isWifiCallingEnabled();
                                                                        if (isNeedUpdate && !this.mPhone.getImsPhone().isWifiCallingEnabled() && this.mPollingContext[0] == 0) {
                                                                            log("mPollingContext == 0");
                                                                            pollState();
                                                                            break;
                                                                        }
                                                                    }
                                                                case 49:
                                                                    ProxyController.getInstance().unregisterForAllDataDisconnected(SubscriptionManager.getDefaultDataSubscriptionId(), this);
                                                                    synchronized (this) {
                                                                        if (this.mPendingRadioPowerOffAfterDataOff) {
                                                                            log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                                                                            hangupAndPowerOff();
                                                                            this.mPendingRadioPowerOffAfterDataOff = false;
                                                                        } else {
                                                                            log("EVENT_ALL_DATA_DISCONNECTED is stale");
                                                                        }
                                                                    }
                                                                    break;
                                                                case 50:
                                                                    break;
                                                                case 51:
                                                                    ar = (AsyncResult) msg.obj;
                                                                    if (ar.exception == null) {
                                                                        boolean enable = ((Boolean) ar.result).booleanValue();
                                                                        stringBuilder3 = new StringBuilder();
                                                                        stringBuilder3.append("EVENT_RADIO_POWER_FROM_CARRIER: ");
                                                                        stringBuilder3.append(enable);
                                                                        log(stringBuilder3.toString());
                                                                        setRadioPowerFromCarrier(enable);
                                                                        break;
                                                                    }
                                                                    break;
                                                                case 52:
                                                                    log("EVENT_SIM_NOT_INSERTED");
                                                                    cancelAllNotifications();
                                                                    this.mMdn = null;
                                                                    this.mMin = null;
                                                                    this.mIsMinInfoReady = false;
                                                                    break;
                                                                case 53:
                                                                    log("EVENT_IMS_SERVICE_STATE_CHANGED");
                                                                    if (this.mSS.getState() != 0) {
                                                                        this.mPhone.notifyServiceStateChanged(this.mPhone.getServiceState());
                                                                        break;
                                                                    }
                                                                    break;
                                                                case 54:
                                                                    log("EVENT_RADIO_POWER_OFF_DONE");
                                                                    if (this.mDeviceShuttingDown && this.mCi.getRadioState().isAvailable()) {
                                                                        this.mCi.requestShutdown(null);
                                                                        break;
                                                                    }
                                                                case 55:
                                                                    ar = msg.obj;
                                                                    if (ar.exception == null) {
                                                                        List<PhysicalChannelConfig> list2 = ar.result;
                                                                        this.mPhone.notifyPhysicalChannelConfiguration(list2);
                                                                        this.mLastPhysicalChannelConfigList = list2;
                                                                        if (RatRatcheter.updateBandwidths(getBandwidthsFromConfigs(list2), this.mSS)) {
                                                                            this.mPhone.notifyServiceStateChanged(this.mSS);
                                                                            break;
                                                                        }
                                                                    }
                                                                    break;
                                                                case 56:
                                                                    log("EVENT_RADIO_POWER_OFF");
                                                                    this.mNewSS.setStateOff();
                                                                    pollStateDone();
                                                                    break;
                                                                default:
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("Unhandled message with number: ");
                                                                    stringBuilder2.append(msg.what);
                                                                    log(stringBuilder2.toString());
                                                                    break;
                                                            }
                                                            break;
                                                    }
                                            }
                                    }
                            }
                            if (!this.mDesiredPowerState && HwTelephonyFactory.getHwUiccManager().uiccHwdsdsNeedSetActiveMode() && !this.mDoRecoveryMarker) {
                                log("CdmaSerive/Gsmserive tracker need wait SetActiveMode ");
                                break;
                            }
                            setDoRecoveryMarker(false);
                            if (!this.mPhone.isPhoneTypeGsm() && this.mCi.getRadioState() == RadioState.RADIO_ON) {
                                handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                                queueNextSignalStrengthPoll();
                            }
                            setPowerStateToDesired();
                            modemTriggeredPollState();
                            break;
                            break;
                    }
                } else if (this.mPollingContext[0] == 0) {
                    log("EVENT_GET_AD_DONE pollState ");
                    pollState();
                } else {
                    log("EVENT_GET_AD_DONE pollState working ,no need do again");
                }
            } else {
                log("[settimezone]EVENT_NITZ_CAPABILITY_NOTIFICATION");
                sendTimeZoneSelectionNotification();
            }
        }
    }

    private int[] getBandwidthsFromConfigs(List<PhysicalChannelConfig> list) {
        return list.stream().map(-$$Lambda$ServiceStateTracker$WWHOcG5P4-jgjzPPgLwm-wN15OM.INSTANCE).mapToInt(-$$Lambda$ServiceStateTracker$UV1wDVoVlbcxpr8zevj_aMFtUGw.INSTANCE).toArray();
    }

    public String getRplmn() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return HwTelephonyFactory.getHwNetworkManager().getGsmRplmn(this, this.mPhone);
        }
        return "";
    }

    public void setCurrent3GPsCsAllowed(boolean allowed) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCurrent3GPsCsAllowed:");
        stringBuilder.append(allowed);
        log(stringBuilder.toString());
        this.isCurrent3GPsCsAllowed = allowed;
    }

    protected boolean isSidsAllZeros() {
        if (this.mHomeSystemId != null) {
            for (int i : this.mHomeSystemId) {
                if (i != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isHomeSid(int sid) {
        if (this.mHomeSystemId != null) {
            for (int i : this.mHomeSystemId) {
                if (sid == i) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getMdnNumber() {
        return this.mMdn;
    }

    public String getCdmaMin() {
        return this.mMin;
    }

    public String getPrlVersion() {
        int subId = this.mPhone.getSubId();
        int simCardState = TelephonyManager.getDefault().getSimState(subId);
        if (5 == simCardState) {
            this.mPrlVersion = this.mCi.getHwPrlVersion();
        } else {
            this.mPrlVersion = ProxyController.MODEM_0;
        }
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPrlVersion: prlVersion=");
        stringBuilder.append(this.mPrlVersion);
        stringBuilder.append(", subid=");
        stringBuilder.append(subId);
        stringBuilder.append(", simState=");
        stringBuilder.append(simCardState);
        Rlog.d(str, stringBuilder.toString());
        return this.mPrlVersion;
    }

    public String getMlplVersion() {
        String realMlplVersion = null;
        int subId = this.mPhone.getSubId();
        if (true == HwModemCapability.isCapabilitySupport(9) && 5 == TelephonyManager.getDefault().getSimState(subId)) {
            realMlplVersion = this.mCi.getHwCDMAMlplVersion();
        }
        if (realMlplVersion == null) {
            realMlplVersion = this.mMlplVersion;
        }
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getMlplVersion: mlplVersion=");
        stringBuilder.append(realMlplVersion);
        Rlog.d(str, stringBuilder.toString());
        return realMlplVersion;
    }

    public String getMsplVersion() {
        String realMsplVersion = null;
        int subId = this.mPhone.getSubId();
        if (true == HwModemCapability.isCapabilitySupport(9) && 5 == TelephonyManager.getDefault().getSimState(subId)) {
            realMsplVersion = this.mCi.getHwCDMAMsplVersion();
        }
        if (realMsplVersion == null) {
            realMsplVersion = this.mMsplVersion;
        }
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getMsplVersion: msplVersion=");
        stringBuilder.append(realMsplVersion);
        Rlog.d(str, stringBuilder.toString());
        return realMsplVersion;
    }

    public String getImsi() {
        String operatorNumeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (TextUtils.isEmpty(operatorNumeric) || getCdmaMin() == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(operatorNumeric);
        stringBuilder.append(getCdmaMin());
        return stringBuilder.toString();
    }

    public boolean isMinInfoReady() {
        return this.mIsMinInfoReady;
    }

    public int getOtasp() {
        int provisioningState = 3;
        if (this.mPhone.isPhoneTypeGsm()) {
            log("getOtasp: otasp not needed for GSM");
            return 3;
        } else if (this.mIsSubscriptionFromRuim && this.mMin == null) {
            return 3;
        } else {
            if (this.mMin == null || this.mMin.length() < 6) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getOtasp: bad mMin='");
                stringBuilder.append(this.mMin);
                stringBuilder.append("'");
                log(stringBuilder.toString());
                provisioningState = 1;
            } else if (this.mMin.equals(UNACTIVATED_MIN_VALUE) || this.mMin.substring(0, 6).equals(UNACTIVATED_MIN2_VALUE) || SystemProperties.getBoolean("test_cdma_setup", false)) {
                provisioningState = 2;
            }
            int provisioningState2 = provisioningState;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getOtasp: state=");
            stringBuilder2.append(provisioningState2);
            log(stringBuilder2.toString());
            return provisioningState2;
        }
    }

    protected void parseSidNid(String sidStr, String nidStr) {
        String[] sid;
        int i = 0;
        if (sidStr != null) {
            sid = sidStr.split(",");
            this.mHomeSystemId = new int[sid.length];
            for (int i2 = 0; i2 < sid.length; i2++) {
                try {
                    this.mHomeSystemId[i2] = Integer.parseInt(sid[i2]);
                } catch (NumberFormatException ex) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("error parsing system id: ");
                    stringBuilder.append(ex);
                    loge(stringBuilder.toString());
                }
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("CDMA_SUBSCRIPTION: SID=");
        stringBuilder2.append(sidStr);
        log(stringBuilder2.toString());
        if (nidStr != null) {
            sid = nidStr.split(",");
            this.mHomeNetworkId = new int[sid.length];
            while (i < sid.length) {
                try {
                    this.mHomeNetworkId[i] = Integer.parseInt(sid[i]);
                } catch (NumberFormatException ex2) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("CDMA_SUBSCRIPTION: error parsing network id: ");
                    stringBuilder3.append(ex2);
                    loge(stringBuilder3.toString());
                }
                i++;
            }
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("CDMA_SUBSCRIPTION: NID=");
        stringBuilder4.append(nidStr);
        log(stringBuilder4.toString());
    }

    protected void updateOtaspState() {
        int otaspMode = getOtasp();
        int oldOtaspMode = this.mCurrentOtaspMode;
        this.mCurrentOtaspMode = otaspMode;
        if (oldOtaspMode != this.mCurrentOtaspMode) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateOtaspState: call notifyOtaspChanged old otaspMode=");
            stringBuilder.append(oldOtaspMode);
            stringBuilder.append(" new otaspMode=");
            stringBuilder.append(this.mCurrentOtaspMode);
            log(stringBuilder.toString());
            this.mPhone.notifyOtaspChanged(this.mCurrentOtaspMode);
        }
    }

    protected Phone getPhone() {
        return this.mPhone;
    }

    protected void handlePollStateResult(int what, AsyncResult ar) {
        if (this.mHwCustGsmServiceStateTracker != null) {
            this.mHwCustGsmServiceStateTracker.custHandlePollStateResult(what, ar, this.mPollingContext);
        }
        if (ar.userObj == this.mPollingContext) {
            StringBuilder stringBuilder;
            if (ar.exception != null) {
                Error err = null;
                if (ar.exception instanceof IllegalStateException) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("handlePollStateResult exception ");
                    stringBuilder.append(ar.exception);
                    log(stringBuilder.toString());
                }
                if (ar.exception instanceof CommandException) {
                    err = ((CommandException) ar.exception).getCommandError();
                }
                if (err == Error.RADIO_NOT_AVAILABLE) {
                    cancelPollState();
                    return;
                } else if (err != Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RIL implementation has returned an error where it must succeed");
                    stringBuilder.append(ar.exception);
                    loge(stringBuilder.toString());
                }
            } else {
                try {
                    handlePollStateResultMessage(what, ar);
                } catch (RuntimeException ex) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception while polling service state. Probably malformed RIL response.");
                    stringBuilder.append(ex);
                    loge(stringBuilder.toString());
                }
            }
            int[] iArr = this.mPollingContext;
            boolean isVoiceInService = false;
            iArr[0] = iArr[0] - 1;
            if (this.mPollingContext[0] == 0) {
                if (this.mPhone.isPhoneTypeGsm()) {
                    updateRoamingState();
                    this.hasUpdateCellLocByPS = false;
                } else {
                    StringBuilder stringBuilder2;
                    boolean namMatch = false;
                    if (!isSidsAllZeros() && isHomeSid(this.mNewSS.getCdmaSystemId())) {
                        namMatch = true;
                    }
                    if (this.mIsSubscriptionFromRuim) {
                        boolean isRoamingBetweenOperators = isRoamingBetweenOperators(this.mNewSS.getVoiceRoaming(), this.mNewSS);
                        if (isRoamingBetweenOperators != this.mNewSS.getVoiceRoaming()) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("isRoamingBetweenOperators=");
                            stringBuilder2.append(isRoamingBetweenOperators);
                            stringBuilder2.append(". Override CDMA voice roaming to ");
                            stringBuilder2.append(isRoamingBetweenOperators);
                            log(stringBuilder2.toString());
                            this.mNewSS.setVoiceRoaming(isRoamingBetweenOperators);
                        }
                    }
                    if (ServiceState.isCdma(this.mNewSS.getRilDataRadioTechnology())) {
                        if (this.mNewSS.getVoiceRegState() == 0) {
                            isVoiceInService = true;
                        }
                        boolean isVoiceRoaming;
                        StringBuilder stringBuilder3;
                        if (isVoiceInService) {
                            isVoiceRoaming = this.mNewSS.getVoiceRoaming();
                            if (this.mNewSS.getDataRoaming() != isVoiceRoaming) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Data roaming != Voice roaming. Override data roaming to ");
                                stringBuilder3.append(isVoiceRoaming);
                                log(stringBuilder3.toString());
                                this.mNewSS.setDataRoaming(isVoiceRoaming);
                            }
                        } else if (-1 != this.mRoamingIndicator) {
                            isVoiceRoaming = isRoamIndForHomeSystem(Integer.toString(this.mRoamingIndicator));
                            if (this.mNewSS.getDataRoaming() == isVoiceRoaming) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("isRoamIndForHomeSystem=");
                                stringBuilder3.append(isVoiceRoaming);
                                stringBuilder3.append(", override data roaming to ");
                                stringBuilder3.append(isVoiceRoaming ^ 1);
                                log(stringBuilder3.toString());
                                this.mNewSS.setDataRoaming(isVoiceRoaming ^ 1);
                            }
                        }
                    }
                    this.mNewSS.setCdmaDefaultRoamingIndicator(this.mDefaultRoamingIndicator);
                    this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                    isVoiceInService = true;
                    if (TextUtils.isEmpty(this.mPrlVersion)) {
                        isVoiceInService = false;
                    }
                    if (!isVoiceInService || this.mNewSS.getRilVoiceRadioTechnology() == 0) {
                        log("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                        this.mNewSS.setCdmaRoamingIndicator(1);
                    } else if (!isSidsAllZeros()) {
                        if (!namMatch && !this.mIsInPrl) {
                            this.mNewSS.setCdmaRoamingIndicator(this.mDefaultRoamingIndicator);
                        } else if (!namMatch || this.mIsInPrl) {
                            if (!namMatch && this.mIsInPrl) {
                                this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                            } else if (this.mRoamingIndicator <= 2) {
                                this.mNewSS.setCdmaRoamingIndicator(1);
                            } else {
                                this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                            }
                        } else if (ServiceState.isLte(this.mNewSS.getRilVoiceRadioTechnology())) {
                            log("Turn off roaming indicator as voice is LTE");
                            this.mNewSS.setCdmaRoamingIndicator(1);
                        } else {
                            this.mNewSS.setCdmaRoamingIndicator(2);
                        }
                    }
                    int roamingIndicator = this.mNewSS.getCdmaRoamingIndicator();
                    this.mNewSS.setCdmaEriIconIndex(this.mPhone.mEriManager.getCdmaEriIconIndex(roamingIndicator, this.mDefaultRoamingIndicator));
                    this.mNewSS.setCdmaEriIconMode(this.mPhone.mEriManager.getCdmaEriIconMode(roamingIndicator, this.mDefaultRoamingIndicator));
                    HwTelephonyFactory.getHwNetworkManager().updateCTRoaming(this, this.mPhone, this.mNewSS, this.mNewSS.getRoaming());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Set CDMA Roaming Indicator to: ");
                    stringBuilder2.append(this.mNewSS.getCdmaRoamingIndicator());
                    stringBuilder2.append(". voiceRoaming = ");
                    stringBuilder2.append(this.mNewSS.getVoiceRoaming());
                    stringBuilder2.append(". dataRoaming = ");
                    stringBuilder2.append(this.mNewSS.getDataRoaming());
                    stringBuilder2.append(", isPrlLoaded = ");
                    stringBuilder2.append(isVoiceInService);
                    stringBuilder2.append(". namMatch = ");
                    stringBuilder2.append(namMatch);
                    stringBuilder2.append(" , mIsInPrl = ");
                    stringBuilder2.append(this.mIsInPrl);
                    stringBuilder2.append(", mRoamingIndicator = ");
                    stringBuilder2.append(this.mRoamingIndicator);
                    stringBuilder2.append(", mDefaultRoamingIndicator= ");
                    stringBuilder2.append(this.mDefaultRoamingIndicator);
                    log(stringBuilder2.toString());
                }
                this.mNewSS.setEmergencyOnly(this.mEmergencyOnly);
                pollStateDone();
                if (this.mHwCustGsmServiceStateTracker != null) {
                    this.mHwCustGsmServiceStateTracker.clearLteEmmCause(getPhone().getPhoneId(), this.mSS);
                }
            }
        }
    }

    private boolean isRoamingBetweenOperators(boolean cdmaRoaming, ServiceState s) {
        return cdmaRoaming && !isSameOperatorNameFromSimAndSS(s);
    }

    void handlePollStateResultMessage(int what, AsyncResult ar) {
        int i = what;
        AsyncResult asyncResult = ar;
        List list = null;
        if (i != 14) {
            int registrationState;
            int cssIndicator;
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            switch (i) {
                case 4:
                    StringBuilder stringBuilder3;
                    NetworkRegistrationState networkRegState = asyncResult.result;
                    VoiceSpecificRegistrationStates voiceSpecificStates = networkRegState.getVoiceSpecificStates();
                    registrationState = networkRegState.getRegState();
                    cssIndicator = voiceSpecificStates.cssSupported;
                    int newVoiceRat = HwTelephonyFactory.getHwNetworkManager().getCARilRadioType(this, this.mPhone, ServiceState.networkTypeToRilRadioTechnology(networkRegState.getAccessNetworkTechnology()));
                    this.mNewSS.setVoiceRegState(regCodeToServiceState(registrationState));
                    this.mNewSS.setCssIndicator(cssIndicator);
                    this.mNewSS.setRilVoiceRadioTechnology(newVoiceRat);
                    this.mNewSS.addNetworkRegistrationState(networkRegState);
                    setPhyCellInfoFromCellIdentity(this.mNewSS, networkRegState.getCellIdentity());
                    int reasonForDenial = networkRegState.getReasonForDenial();
                    this.mEmergencyOnly = networkRegState.isEmergencyEnabled();
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mGsmRoaming = regCodeIsRoaming(registrationState);
                        this.mNewRejectCode = reasonForDenial;
                        HwTelephonyFactory.getHwNetworkManager().sendGsmRoamingIntentIfDenied(this, this.mPhone, registrationState, reasonForDenial);
                        this.mPhone.getContext().getResources().getBoolean(17957068);
                    } else {
                        int roamingIndicator = voiceSpecificStates.roamingIndicator;
                        int systemIsInPrl = voiceSpecificStates.systemIsInPrl;
                        int defaultRoamingIndicator = voiceSpecificStates.defaultRoamingIndicator;
                        this.mRegistrationState = registrationState;
                        boolean cdmaRoaming = regCodeIsRoaming(registrationState) && !isRoamIndForHomeSystem(Integer.toString(roamingIndicator));
                        this.mNewSS.setVoiceRoaming(cdmaRoaming);
                        this.mRoamingIndicator = roamingIndicator;
                        this.mIsInPrl = systemIsInPrl != 0;
                        this.mDefaultRoamingIndicator = defaultRoamingIndicator;
                        int systemId = 0;
                        int networkId = 0;
                        CellIdentity cellIdentity = networkRegState.getCellIdentity();
                        if (cellIdentity != null && cellIdentity.getType() == 2) {
                            systemId = ((CellIdentityCdma) cellIdentity).getSystemId();
                            networkId = ((CellIdentityCdma) cellIdentity).getNetworkId();
                        }
                        i = networkId;
                        this.mNewSS.setCdmaSystemAndNetworkId(systemId, i);
                        if (reasonForDenial == 0) {
                            this.mRegistrationDeniedReason = REGISTRATION_DENIED_GEN;
                        } else if (reasonForDenial == 1) {
                            this.mRegistrationDeniedReason = REGISTRATION_DENIED_AUTH;
                        } else {
                            this.mRegistrationDeniedReason = "";
                        }
                        if (this.mRegistrationState == 3) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Registration denied, ");
                            stringBuilder3.append(this.mRegistrationDeniedReason);
                            log(stringBuilder3.toString());
                        }
                    }
                    processCellLocationInfo(this.mNewCellLoc, networkRegState.getCellIdentity());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("handlPollVoiceRegResultMessage: regState=");
                    stringBuilder3.append(registrationState);
                    stringBuilder3.append(" radioTechnology=");
                    stringBuilder3.append(newVoiceRat);
                    log(stringBuilder3.toString());
                    return;
                case 5:
                    NetworkRegistrationState networkRegState2 = asyncResult.result;
                    DataSpecificRegistrationStates dataSpecificStates = networkRegState2.getDataSpecificStates();
                    int registrationState2 = networkRegState2.getRegState();
                    registrationState = regCodeToServiceState(registrationState2);
                    cssIndicator = HwTelephonyFactory.getHwNetworkManager().updateHSPAStatus(this, this.mPhone, HwTelephonyFactory.getHwNetworkManager().updateCAStatus(this, this.mPhone, ServiceState.networkTypeToRilRadioTechnology(networkRegState2.getAccessNetworkTechnology())));
                    this.mNewSS.setDataRegState(registrationState);
                    this.mNewSS.setRilDataRadioTechnology(cssIndicator);
                    this.mNewSS.addNetworkRegistrationState(networkRegState2);
                    if (registrationState == 1) {
                        this.mLastPhysicalChannelConfigList = null;
                    }
                    setPhyCellInfoFromCellIdentity(this.mNewSS, networkRegState2.getCellIdentity());
                    boolean isDataRoaming;
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mNewReasonDataDenied = networkRegState2.getReasonForDenial();
                        this.mNewMaxDataCalls = dataSpecificStates.maxDataCalls;
                        this.mDataRoaming = regCodeIsRoaming(registrationState2);
                        this.mNewSS.setDataRoamingFromRegistration(this.mDataRoaming);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("handlPollStateResultMessage: GsmSST dataServiceState=");
                        stringBuilder.append(registrationState);
                        stringBuilder.append(" regState=");
                        stringBuilder.append(registrationState2);
                        stringBuilder.append(" dataRadioTechnology=");
                        stringBuilder.append(cssIndicator);
                        log(stringBuilder.toString());
                    } else if (this.mPhone.isPhoneTypeCdma()) {
                        isDataRoaming = regCodeIsRoaming(registrationState2);
                        this.mNewSS.setDataRoaming(isDataRoaming);
                        this.mNewSS.setDataRoamingFromRegistration(isDataRoaming);
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("handlPollStateResultMessage: cdma dataServiceState=");
                        stringBuilder2.append(registrationState);
                        stringBuilder2.append(" regState=");
                        stringBuilder2.append(registrationState2);
                        stringBuilder2.append(" dataRadioTechnology=");
                        stringBuilder2.append(cssIndicator);
                        log(stringBuilder2.toString());
                    } else {
                        int oldDataRAT = this.mSS.getRilDataRadioTechnology();
                        if ((oldDataRAT == 0 && cssIndicator != 0) || ((ServiceState.isCdma(oldDataRAT) && ServiceState.isLte(cssIndicator)) || (ServiceState.isLte(oldDataRAT) && ServiceState.isCdma(cssIndicator)))) {
                            this.mCi.getSignalStrength(obtainMessage(3));
                        }
                        isDataRoaming = regCodeIsRoaming(registrationState2);
                        this.mNewSS.setDataRoaming(isDataRoaming);
                        this.mNewSS.setDataRoamingFromRegistration(isDataRoaming);
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("handlPollStateResultMessage: CdmaLteSST dataServiceState=");
                        stringBuilder4.append(registrationState);
                        stringBuilder4.append(" registrationState=");
                        stringBuilder4.append(registrationState2);
                        stringBuilder4.append(" dataRadioTechnology=");
                        stringBuilder4.append(cssIndicator);
                        log(stringBuilder4.toString());
                    }
                    updateServiceStateLteEarfcnBoost(this.mNewSS, getLteEarfcn(networkRegState2.getCellIdentity()));
                    if (registrationState == 0 && this.mPhone.isCTSimCard(this.mPhone.getPhoneId())) {
                        processCtVolteCellLocationInfo(this.mNewCellLoc, networkRegState2.getCellIdentity());
                        return;
                    }
                    return;
                case 6:
                    String[] opNames;
                    List brandOverride;
                    if (this.mPhone.isPhoneTypeGsm()) {
                        opNames = asyncResult.result;
                        if (opNames != null && opNames.length >= 3) {
                            if (this.mUiccController.getUiccCard(getPhoneId()) != null) {
                                list = this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride();
                            }
                            brandOverride = list;
                            if (brandOverride != null) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("EVENT_POLL_STATE_OPERATOR: use brandOverride=");
                                stringBuilder2.append(brandOverride);
                                log(stringBuilder2.toString());
                                this.mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                            } else {
                                this.mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                            }
                            upatePlmn(brandOverride, opNames[0], opNames[1], opNames[2]);
                            return;
                        }
                        return;
                    }
                    opNames = (String[]) asyncResult.result;
                    if (opNames == null || opNames.length < 3) {
                        log("EVENT_POLL_STATE_OPERATOR_CDMA: error parsing opNames");
                        return;
                    }
                    if (opNames[2] == null || opNames[2].length() < 5 || "00000".equals(opNames[2])) {
                        opNames[0] = null;
                        opNames[1] = null;
                        opNames[2] = null;
                    }
                    if (this.mIsSubscriptionFromRuim) {
                        if (this.mUiccController.getUiccCard(getPhoneId()) != null) {
                            list = this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride();
                        }
                        brandOverride = list;
                        if (brandOverride != null) {
                            this.mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                            return;
                        } else {
                            this.mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                            return;
                        }
                    }
                    this.mNewSS.setOperatorName(opNames[0], opNames[1], opNames[2]);
                    return;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("handlePollStateResultMessage: Unexpected RIL response received: ");
                    stringBuilder.append(i);
                    loge(stringBuilder.toString());
                    return;
            }
        }
        int[] ints = asyncResult.result;
        this.mNewSS.setIsManualSelection(ints[0] == 1);
        if (ints[0] == 1 && (this.mPhone.shouldForceAutoNetworkSelect() || this.mRecoverAutoSelectMode)) {
            this.mPhone.setNetworkSelectionModeAutomatic(null);
            log(" Forcing Automatic Network Selection, manual selection is not allowed");
            this.mRecoverAutoSelectMode = false;
        } else if (this.mRecoverAutoSelectMode) {
            this.mRecoverAutoSelectMode = false;
        }
    }

    private static boolean isValidLteBandwidthKhz(int bandwidth) {
        if (bandwidth == 1400 || bandwidth == 3000 || bandwidth == AbstractPhoneBase.SET_TO_AOTO_TIME || bandwidth == 10000 || bandwidth == 15000 || bandwidth == POLL_PERIOD_MILLIS) {
            return true;
        }
        return false;
    }

    private void setPhyCellInfoFromCellIdentity(ServiceState ss, CellIdentity cellIdentity) {
        if (cellIdentity == null) {
            log("Could not set ServiceState channel number. CellIdentity null");
            return;
        }
        ss.setChannelNumber(cellIdentity.getChannelNumber());
        if (cellIdentity instanceof CellIdentityLte) {
            int bandwidth;
            StringBuilder stringBuilder;
            CellIdentityLte cl = (CellIdentityLte) cellIdentity;
            int[] bandwidths = null;
            if (!ArrayUtils.isEmpty(this.mLastPhysicalChannelConfigList)) {
                bandwidths = getBandwidthsFromConfigs(this.mLastPhysicalChannelConfigList);
                for (int bw : bandwidths) {
                    if (!isValidLteBandwidthKhz(bw)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid LTE Bandwidth in RegistrationState, ");
                        stringBuilder.append(bw);
                        loge(stringBuilder.toString());
                        bandwidths = null;
                        break;
                    }
                }
            }
            if (bandwidths == null || bandwidths.length == 1) {
                bandwidth = cl.getBandwidth();
                if (isValidLteBandwidthKhz(bandwidth)) {
                    bandwidths = new int[]{bandwidth};
                } else if (bandwidth != KeepaliveStatus.INVALID_HANDLE) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid LTE Bandwidth in RegistrationState, ");
                    stringBuilder.append(bandwidth);
                    loge(stringBuilder.toString());
                }
            }
            if (bandwidths != null) {
                ss.setCellBandwidths(bandwidths);
            }
        }
    }

    private boolean isRoamIndForHomeSystem(String roamInd) {
        String[] homeRoamIndicators = Resources.getSystem().getStringArray(17235993);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isRoamIndForHomeSystem: homeRoamIndicators=");
        stringBuilder.append(Arrays.toString(homeRoamIndicators));
        log(stringBuilder.toString());
        if (homeRoamIndicators != null) {
            for (String homeRoamInd : homeRoamIndicators) {
                if (homeRoamInd.equals(roamInd)) {
                    return true;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isRoamIndForHomeSystem: No match found against list for roamInd=");
            stringBuilder2.append(roamInd);
            log(stringBuilder2.toString());
            return false;
        }
        log("isRoamIndForHomeSystem: No list found");
        return false;
    }

    protected void updateRoamingState() {
        boolean z = false;
        if (this.mPhone.isPhoneTypeGsm()) {
            if (this.mGsmRoaming || this.mDataRoaming) {
                z = true;
            }
            boolean roaming = z;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateRoamingState: original roaming = ");
            stringBuilder.append(roaming);
            stringBuilder.append(" mGsmRoaming:");
            stringBuilder.append(this.mGsmRoaming);
            stringBuilder.append(" mDataRoaming:");
            stringBuilder.append(this.mDataRoaming);
            log(stringBuilder.toString());
            if (checkForRoamingForIndianOperators(this.mNewSS)) {
                log("indian operator,skip");
            } else if (this.mGsmRoaming && !isOperatorConsideredRoaming(this.mNewSS) && (isSameNamedOperators(this.mNewSS) || isOperatorConsideredNonRoaming(this.mNewSS))) {
                roaming = false;
                log("updateRoamingState: set roaming = false");
            }
            CarrierConfigManager configLoader = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(this.mPhone.getSubId());
                    StringBuilder stringBuilder2;
                    if (alwaysOnHomeNetwork(b)) {
                        log("updateRoamingState: carrier config override always on home network");
                        roaming = false;
                    } else if (isNonRoamingInGsmNetwork(b, this.mNewSS.getOperatorNumeric())) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateRoamingState: carrier config override set non roaming:");
                        stringBuilder2.append(this.mNewSS.getOperatorNumeric());
                        log(stringBuilder2.toString());
                        roaming = false;
                    } else if (isRoamingInGsmNetwork(b, this.mNewSS.getOperatorNumeric())) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateRoamingState: carrier config override set roaming:");
                        stringBuilder2.append(this.mNewSS.getOperatorNumeric());
                        log(stringBuilder2.toString());
                        roaming = true;
                    }
                } catch (Exception e) {
                    loge("updateRoamingState: unable to access carrier config service");
                }
            } else {
                log("updateRoamingState: no carrier config service available");
            }
            roaming = HwTelephonyFactory.getHwNetworkManager().getGsmRoamingState(this, this.mPhone, roaming);
            this.mNewSS.setVoiceRoaming(roaming);
            this.mNewSS.setDataRoaming(roaming);
            return;
        }
        CarrierConfigManager configLoader2 = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (configLoader2 != null) {
            try {
                PersistableBundle b2 = configLoader2.getConfigForSubId(this.mPhone.getSubId());
                String systemId = Integer.toString(this.mNewSS.getCdmaSystemId());
                if (alwaysOnHomeNetwork(b2)) {
                    log("updateRoamingState: carrier config override always on home network");
                    setRoamingOff();
                } else {
                    StringBuilder stringBuilder3;
                    if (!isNonRoamingInGsmNetwork(b2, this.mNewSS.getOperatorNumeric())) {
                        if (!isNonRoamingInCdmaNetwork(b2, systemId)) {
                            if (isRoamingInGsmNetwork(b2, this.mNewSS.getOperatorNumeric()) || isRoamingInCdmaNetwork(b2, systemId)) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("updateRoamingState: carrier config override set roaming:");
                                stringBuilder3.append(this.mNewSS.getOperatorNumeric());
                                stringBuilder3.append(", ");
                                stringBuilder3.append(systemId);
                                log(stringBuilder3.toString());
                                setRoamingOn();
                            }
                        }
                    }
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("updateRoamingState: carrier config override set non-roaming:");
                    stringBuilder3.append(this.mNewSS.getOperatorNumeric());
                    stringBuilder3.append(", ");
                    stringBuilder3.append(systemId);
                    log(stringBuilder3.toString());
                    setRoamingOff();
                }
            } catch (Exception e2) {
                loge("updateRoamingState: unable to access carrier config service");
            }
        } else {
            log("updateRoamingState: no carrier config service available");
        }
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
    }

    private void setRoamingOn() {
        this.mNewSS.setVoiceRoaming(true);
        this.mNewSS.setDataRoaming(true);
        this.mNewSS.setCdmaEriIconIndex(0);
        this.mNewSS.setCdmaEriIconMode(0);
    }

    private void setRoamingOff() {
        this.mNewSS.setVoiceRoaming(false);
        this.mNewSS.setDataRoaming(false);
        this.mNewSS.setCdmaEriIconIndex(1);
    }

    /* JADX WARNING: Removed duplicated region for block: B:52:0x0167  */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x0162  */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x0214  */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x0206  */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x027b  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0237  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x0342  */
    /* JADX WARNING: Removed duplicated region for block: B:116:0x0349  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateSpnDisplay() {
        int dataIdx;
        updateOperatorNameFromEri();
        String wfcVoiceSpnFormat = null;
        String wfcDataSpnFormat = null;
        int combinedRegState = HwTelephonyFactory.getHwNetworkManager().getGsmCombinedRegState(this, this.mPhone, this.mSS);
        if (this.mPhone.getImsPhone() != null && this.mPhone.getImsPhone().isWifiCallingEnabled() && combinedRegState == 0) {
            String[] wfcSpnFormats = this.mPhone.getContext().getResources().getStringArray(17236092);
            int voiceIdx = 0;
            dataIdx = 0;
            CarrierConfigManager showEmergencyOnly = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (showEmergencyOnly != null) {
                try {
                    PersistableBundle b = showEmergencyOnly.getConfigForSubId(this.mPhone.getSubId());
                    if (b != null) {
                        voiceIdx = b.getInt("wfc_spn_format_idx_int");
                        dataIdx = b.getInt("wfc_data_spn_format_idx_int");
                    }
                } catch (Exception e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateSpnDisplay: carrier config error: ");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                }
            }
            wfcVoiceSpnFormat = wfcSpnFormats[voiceIdx];
            wfcDataSpnFormat = wfcSpnFormats[dataIdx];
        }
        String wfcVoiceSpnFormat2 = wfcVoiceSpnFormat;
        String wfcDataSpnFormat2 = wfcDataSpnFormat;
        String regplmn;
        StringBuilder stringBuilder2;
        int subId;
        boolean showPlmn;
        String wfcVoiceSpnFormat3;
        int showWifi;
        int combinedRegState2;
        String plmn;
        String str;
        int i;
        String str2;
        if (this.mPhone.isPhoneTypeGsm()) {
            boolean showPlmn2;
            boolean forceDisplayNoService;
            boolean showPlmn3;
            String plmn2;
            boolean showEmergencyOnly2;
            String dataSpn;
            boolean showEmergencyOnly3;
            String regplmn2;
            OnsDisplayParams onsDispalyParams;
            boolean showSpn;
            int rule;
            String plmn3;
            String spn;
            boolean showWifi2;
            int[] subIds;
            boolean show_blank_ons;
            String plmn4;
            String spn2;
            String wifi;
            boolean showWifi3;
            String spn3;
            boolean showWifi4;
            StringBuilder stringBuilder3;
            Object[] objArr;
            Intent intent;
            IccRecords iccRecords = this.mIccRecords;
            int rule2 = iccRecords != null ? iccRecords.getDisplayRule(this.mSS) : 0;
            regplmn = this.mSS.getOperatorNumeric();
            boolean showEmergencyOnly4 = false;
            if (combinedRegState == 1 || combinedRegState == 2) {
                showPlmn2 = true;
                forceDisplayNoService = this.mPhone.getContext().getResources().getBoolean(17956934) && !this.mIsSimReady;
                if (!this.mEmergencyOnly || forceDisplayNoService) {
                    wfcVoiceSpnFormat = Resources.getSystem().getText(17040350).toString();
                } else {
                    wfcVoiceSpnFormat = Resources.getSystem().getText(17039987).toString();
                    showEmergencyOnly4 = true;
                }
                if (this.mHwCustGsmServiceStateTracker != null) {
                    wfcVoiceSpnFormat = this.mHwCustGsmServiceStateTracker.setEmergencyToNoService(this.mSS, wfcVoiceSpnFormat, this.mEmergencyOnly);
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateSpnDisplay: radio is on but out of service, set plmn='");
                stringBuilder2.append(wfcVoiceSpnFormat);
                stringBuilder2.append("'");
                log(stringBuilder2.toString());
            } else if (combinedRegState == 0) {
                boolean showSpn2;
                getOperator();
                wfcVoiceSpnFormat = HwTelephonyFactory.getHwNetworkManager().getGsmPlmn(this, this.mPhone);
                showPlmn3 = !TextUtils.isEmpty(wfcVoiceSpnFormat) && (rule2 & 2) == 2;
                plmn2 = wfcVoiceSpnFormat;
                showPlmn2 = showPlmn3;
                showEmergencyOnly2 = showEmergencyOnly4;
                wfcVoiceSpnFormat = iccRecords == null ? iccRecords.getServiceProviderName() : "";
                wfcDataSpnFormat = wfcVoiceSpnFormat;
                showEmergencyOnly4 = (combinedRegState == 0 || TextUtils.isEmpty(wfcVoiceSpnFormat) || (rule2 & 1) != 1) ? false : true;
                if (TextUtils.isEmpty(wfcVoiceSpnFormat) && !TextUtils.isEmpty(wfcVoiceSpnFormat2) && !TextUtils.isEmpty(wfcDataSpnFormat2)) {
                    wfcDataSpnFormat = String.format(wfcDataSpnFormat2, new Object[]{wfcVoiceSpnFormat.trim()});
                    showEmergencyOnly4 = true;
                    showPlmn2 = false;
                } else if (TextUtils.isEmpty(plmn2) && !TextUtils.isEmpty(wfcVoiceSpnFormat2)) {
                    plmn2.trim();
                } else if (this.mSS.getVoiceRegState() == 3 || (showPlmn && TextUtils.equals(wfcVoiceSpnFormat, plmn2))) {
                    wfcVoiceSpnFormat = null;
                    showEmergencyOnly4 = false;
                }
                dataSpn = wfcDataSpnFormat;
                showEmergencyOnly3 = showEmergencyOnly2;
                regplmn2 = regplmn;
                onsDispalyParams = HwTelephonyFactory.getHwNetworkManager().getGsmOnsDisplayParams(this, this.mPhone, showEmergencyOnly4, showPlmn2, rule2, plmn2, wfcVoiceSpnFormat);
                showSpn = onsDispalyParams.mShowSpn;
                showPlmn3 = onsDispalyParams.mShowPlmn;
                rule = onsDispalyParams.mRule;
                plmn3 = onsDispalyParams.mPlmn;
                spn = onsDispalyParams.mSpn;
                showWifi2 = onsDispalyParams.mShowWifi;
                regplmn = onsDispalyParams.mWifi;
                dataIdx = -1;
                subIds = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
                if (subIds != null && subIds.length > 0) {
                    dataIdx = subIds[0];
                }
                subId = dataIdx;
                forceDisplayNoService = false;
                if (this.mHwCustGsmServiceStateTracker == null) {
                    showSpn2 = showSpn;
                    if (this.mHwCustGsmServiceStateTracker.isStopUpdateName(this.mSimCardsLoaded)) {
                        forceDisplayNoService = true;
                    }
                } else {
                    showSpn2 = showSpn;
                }
                show_blank_ons = forceDisplayNoService;
                if ((!display_blank_ons || show_blank_ons) && combinedRegState == 0) {
                    log("In service , display blank ons for tracfone");
                    showPlmn = true;
                    forceDisplayNoService = false;
                    spn = " ";
                    plmn3 = " ";
                } else {
                    showPlmn = showPlmn3;
                    forceDisplayNoService = showSpn2;
                }
                if (this.mSubId != subId) {
                    plmn4 = plmn3;
                    spn2 = spn;
                    wfcDataSpnFormat2 = forceDisplayNoService;
                    wifi = regplmn;
                    wfcVoiceSpnFormat3 = wfcVoiceSpnFormat2;
                    wfcVoiceSpnFormat2 = showPlmn;
                    showWifi3 = showWifi2;
                    showWifi = 0;
                    if (!isOperatorChanged(showPlmn, forceDisplayNoService, spn, dataSpn, plmn4, showWifi2, wifi, regplmn2) && (this.mHwCustGsmServiceStateTracker == null || !this.mHwCustGsmServiceStateTracker.isInServiceState(combinedRegState))) {
                        combinedRegState2 = combinedRegState;
                        int i2 = rule;
                        combinedRegState = dataSpn;
                        boolean z = showEmergencyOnly3;
                        plmn = plmn4;
                        spn3 = spn2;
                        plmn2 = wifi;
                        showWifi4 = showWifi3;
                        this.mSubId = subId;
                        this.mCurShowSpn = wfcDataSpnFormat2;
                        this.mCurShowPlmn = wfcVoiceSpnFormat2;
                        this.mCurSpn = spn3;
                        this.mCurDataSpn = combinedRegState;
                        this.mCurPlmn = plmn;
                        this.mCurShowWifi = showWifi4;
                        this.mCurWifi = plmn2;
                        this.mCurRegplmn = regplmn2;
                        str = wfcVoiceSpnFormat3;
                        i = combinedRegState2;
                        return;
                    }
                }
                wfcVoiceSpnFormat3 = wfcVoiceSpnFormat2;
                plmn4 = plmn3;
                spn2 = spn;
                int[] iArr = subIds;
                wifi = regplmn;
                OnsDisplayParams onsDisplayParams = onsDispalyParams;
                wfcVoiceSpnFormat2 = showPlmn;
                str2 = wfcDataSpnFormat2;
                showWifi3 = showWifi2;
                showWifi = 0;
                wfcDataSpnFormat2 = forceDisplayNoService;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("updateSpnDisplay: changed sending intent rule=");
                stringBuilder3.append(rule);
                stringBuilder3.append(" showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'");
                wfcVoiceSpnFormat = stringBuilder3.toString();
                objArr = new Object[6];
                plmn = plmn4;
                objArr[1] = plmn;
                objArr[2] = Boolean.valueOf(wfcDataSpnFormat2);
                spn3 = spn2;
                objArr[3] = spn3;
                regplmn = dataSpn;
                objArr[4] = regplmn;
                objArr[5] = Integer.valueOf(subId);
                log(String.format(wfcVoiceSpnFormat, objArr));
                updateOperatorProp();
                intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
                intent.putExtra("showSpn", wfcDataSpnFormat2);
                intent.putExtra("spn", spn3);
                intent.putExtra("spnData", regplmn);
                intent.putExtra("showPlmn", wfcVoiceSpnFormat2);
                intent.putExtra("plmn", plmn);
                forceDisplayNoService = showWifi3;
                intent.putExtra(EXTRA_SHOW_WIFI, forceDisplayNoService);
                spn = wifi;
                intent.putExtra(EXTRA_WIFI, spn);
                showPlmn2 = showEmergencyOnly3;
                intent.putExtra(EXTRA_SHOW_EMERGENCYONLY, showPlmn2);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
                this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                if (this.mSS != null && this.mSS.getState() == 0) {
                    if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), wfcVoiceSpnFormat2, plmn, wfcDataSpnFormat2, spn3)) {
                        this.mSpnUpdatePending = true;
                    }
                }
                if (this.mHwCustGsmServiceStateTracker != null) {
                    this.mHwCustGsmServiceStateTracker.setExtPlmnSent(showWifi);
                }
                plmn2 = spn;
                showWifi4 = forceDisplayNoService;
                combinedRegState2 = combinedRegState;
                combinedRegState = regplmn;
                HwTelephonyFactory.getHwNetworkManager().sendGsmDualSimUpdateSpnIntent(this, this.mPhone, wfcDataSpnFormat2, spn3, wfcVoiceSpnFormat2, plmn);
                this.mSubId = subId;
                this.mCurShowSpn = wfcDataSpnFormat2;
                this.mCurShowPlmn = wfcVoiceSpnFormat2;
                this.mCurSpn = spn3;
                this.mCurDataSpn = combinedRegState;
                this.mCurPlmn = plmn;
                this.mCurShowWifi = showWifi4;
                this.mCurWifi = plmn2;
                this.mCurRegplmn = regplmn2;
                str = wfcVoiceSpnFormat3;
                i = combinedRegState2;
                return;
            } else {
                showPlmn2 = true;
                wfcVoiceSpnFormat = Resources.getSystem().getText(17040350).toString();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateSpnDisplay: radio is off w/ showPlmn=");
                stringBuilder2.append(true);
                stringBuilder2.append(" plmn=");
                stringBuilder2.append(wfcVoiceSpnFormat);
                log(stringBuilder2.toString());
            }
            plmn2 = wfcVoiceSpnFormat;
            showEmergencyOnly2 = showEmergencyOnly4;
            if (iccRecords == null) {
            }
            wfcDataSpnFormat = wfcVoiceSpnFormat;
            if (combinedRegState == 0) {
            }
            if (TextUtils.isEmpty(wfcVoiceSpnFormat)) {
            }
            if (TextUtils.isEmpty(plmn2)) {
            }
            wfcVoiceSpnFormat = null;
            showEmergencyOnly4 = false;
            dataSpn = wfcDataSpnFormat;
            showEmergencyOnly3 = showEmergencyOnly2;
            regplmn2 = regplmn;
            onsDispalyParams = HwTelephonyFactory.getHwNetworkManager().getGsmOnsDisplayParams(this, this.mPhone, showEmergencyOnly4, showPlmn2, rule2, plmn2, wfcVoiceSpnFormat);
            showSpn = onsDispalyParams.mShowSpn;
            showPlmn3 = onsDispalyParams.mShowPlmn;
            rule = onsDispalyParams.mRule;
            plmn3 = onsDispalyParams.mPlmn;
            spn = onsDispalyParams.mSpn;
            showWifi2 = onsDispalyParams.mShowWifi;
            regplmn = onsDispalyParams.mWifi;
            dataIdx = -1;
            subIds = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
            dataIdx = subIds[0];
            subId = dataIdx;
            forceDisplayNoService = false;
            if (this.mHwCustGsmServiceStateTracker == null) {
            }
            show_blank_ons = forceDisplayNoService;
            if (display_blank_ons) {
            }
            log("In service , display blank ons for tracfone");
            showPlmn = true;
            forceDisplayNoService = false;
            spn = " ";
            plmn3 = " ";
            if (this.mSubId != subId) {
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("updateSpnDisplay: changed sending intent rule=");
            stringBuilder3.append(rule);
            stringBuilder3.append(" showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'");
            wfcVoiceSpnFormat = stringBuilder3.toString();
            objArr = new Object[6];
            plmn = plmn4;
            objArr[1] = plmn;
            objArr[2] = Boolean.valueOf(wfcDataSpnFormat2);
            spn3 = spn2;
            objArr[3] = spn3;
            regplmn = dataSpn;
            objArr[4] = regplmn;
            objArr[5] = Integer.valueOf(subId);
            log(String.format(wfcVoiceSpnFormat, objArr));
            updateOperatorProp();
            intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            intent.putExtra("showSpn", wfcDataSpnFormat2);
            intent.putExtra("spn", spn3);
            intent.putExtra("spnData", regplmn);
            intent.putExtra("showPlmn", wfcVoiceSpnFormat2);
            intent.putExtra("plmn", plmn);
            forceDisplayNoService = showWifi3;
            intent.putExtra(EXTRA_SHOW_WIFI, forceDisplayNoService);
            spn = wifi;
            intent.putExtra(EXTRA_WIFI, spn);
            showPlmn2 = showEmergencyOnly3;
            intent.putExtra(EXTRA_SHOW_EMERGENCYONLY, showPlmn2);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), wfcVoiceSpnFormat2, plmn, wfcDataSpnFormat2, spn3)) {
            }
            if (this.mHwCustGsmServiceStateTracker != null) {
            }
            plmn2 = spn;
            showWifi4 = forceDisplayNoService;
            combinedRegState2 = combinedRegState;
            combinedRegState = regplmn;
            HwTelephonyFactory.getHwNetworkManager().sendGsmDualSimUpdateSpnIntent(this, this.mPhone, wfcDataSpnFormat2, spn3, wfcVoiceSpnFormat2, plmn);
            this.mSubId = subId;
            this.mCurShowSpn = wfcDataSpnFormat2;
            this.mCurShowPlmn = wfcVoiceSpnFormat2;
            this.mCurSpn = spn3;
            this.mCurDataSpn = combinedRegState;
            this.mCurPlmn = plmn;
            this.mCurShowWifi = showWifi4;
            this.mCurWifi = plmn2;
            this.mCurRegplmn = regplmn2;
            str = wfcVoiceSpnFormat3;
            i = combinedRegState2;
            return;
        }
        String plmn5;
        wfcVoiceSpnFormat3 = wfcVoiceSpnFormat2;
        showWifi = 0;
        combinedRegState2 = combinedRegState;
        str2 = wfcDataSpnFormat2;
        OnsDisplayParams onsDispalyParams2 = HwTelephonyFactory.getHwNetworkManager().getCdmaOnsDisplayParams(this, this.mPhone);
        wfcVoiceSpnFormat = onsDispalyParams2.mPlmn;
        showPlmn = onsDispalyParams2.mShowPlmn;
        boolean showWifi5 = onsDispalyParams2.mShowWifi;
        wfcDataSpnFormat2 = onsDispalyParams2.mWifi;
        int subId2 = -1;
        int[] subIds2 = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
        if (subIds2 != null && subIds2.length > 0) {
            subId2 = subIds2[showWifi];
        }
        subId = subId2;
        if (TextUtils.isEmpty(wfcVoiceSpnFormat)) {
            plmn = wfcVoiceSpnFormat3;
        } else {
            plmn = wfcVoiceSpnFormat3;
            TextUtils.isEmpty(plmn);
        }
        if (display_blank_ons && (wfcVoiceSpnFormat != null || this.mSS.getState() == 0)) {
            log("In service , display blank ons for tracfone");
            wfcVoiceSpnFormat = " ";
        }
        int combinedRegState3 = combinedRegState2;
        if (combinedRegState3 == 1) {
            wfcVoiceSpnFormat = Resources.getSystem().getText(17040350).toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateSpnDisplay: radio is on but out of svc, set plmn='");
            stringBuilder2.append(wfcVoiceSpnFormat);
            stringBuilder2.append("'");
            log(stringBuilder2.toString());
        }
        wfcDataSpnFormat = wfcVoiceSpnFormat;
        if (this.mSubId == subId && TextUtils.equals(wfcDataSpnFormat, this.mCurPlmn) && this.mCurShowWifi == showWifi5 && TextUtils.equals(wfcDataSpnFormat2, this.mCurWifi)) {
            plmn5 = wfcDataSpnFormat;
            i = combinedRegState3;
            str = plmn;
        } else {
            log(String.format("updateSpnDisplay: changed sending intent showPlmn='%b' plmn='%s' subId='%d'", new Object[]{Boolean.valueOf(showPlmn), wfcDataSpnFormat, Integer.valueOf(subId)}));
            Intent intent2 = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            intent2.putExtra("showSpn", showWifi);
            intent2.putExtra("spn", "");
            intent2.putExtra("showPlmn", showPlmn);
            intent2.putExtra("plmn", wfcDataSpnFormat);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, this.mPhone.getPhoneId());
            intent2.putExtra(EXTRA_SHOW_WIFI, showWifi5);
            intent2.putExtra(EXTRA_WIFI, wfcDataSpnFormat2);
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent2, UserHandle.ALL);
            if (this.mSS == null || this.mSS.getState() != 0) {
                str = plmn;
            } else {
                if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), showPlmn, wfcDataSpnFormat, false, "")) {
                    this.mSpnUpdatePending = true;
                }
            }
            regplmn = wfcDataSpnFormat;
            plmn5 = regplmn;
            HwTelephonyFactory.getHwNetworkManager().sendCdmaDualSimUpdateSpnIntent(this, this.mPhone, false, "", showPlmn, regplmn);
        }
        updateOperatorProp();
        this.mSubId = subId;
        this.mCurShowSpn = showWifi;
        this.mCurShowPlmn = showPlmn;
        this.mCurSpn = "";
        this.mCurPlmn = plmn5;
        this.mCurShowWifi = showWifi5;
        this.mCurWifi = wfcDataSpnFormat2;
    }

    protected void setPowerStateToDesired() {
        getCaller();
        String tmpLog = new StringBuilder();
        tmpLog.append("mDeviceShuttingDown=");
        tmpLog.append(this.mDeviceShuttingDown);
        tmpLog.append(", mDesiredPowerState=");
        tmpLog.append(this.mDesiredPowerState);
        tmpLog.append(", getRadioState=");
        tmpLog.append(this.mCi.getRadioState());
        tmpLog.append(", mPowerOffDelayNeed=");
        tmpLog.append(this.mPowerOffDelayNeed);
        tmpLog.append(", mAlarmSwitch=");
        tmpLog.append(this.mAlarmSwitch);
        tmpLog.append(", mRadioDisabledByCarrier=");
        tmpLog.append(this.mRadioDisabledByCarrier);
        tmpLog = tmpLog.toString();
        log(tmpLog);
        this.mRadioPowerLog.log(tmpLog);
        if (ISDEMO) {
            this.mCi.setRadioPower(false, null);
        }
        if (this.mPhone.isPhoneTypeGsm() && this.mAlarmSwitch) {
            log("mAlarmSwitch == true");
            ((AlarmManager) this.mPhone.getContext().getSystemService("alarm")).cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = false;
        }
        if (this.mDesiredPowerState && !this.mRadioDisabledByCarrier && this.mCi.getRadioState() == RadioState.RADIO_OFF) {
            if (this.mHwCustGsmServiceStateTracker != null) {
                this.mHwCustGsmServiceStateTracker.setRadioPower(this.mCi, true);
            }
            this.mCi.setRadioPower(true, null);
        } else if ((!this.mDesiredPowerState || this.mRadioDisabledByCarrier) && this.mCi.getRadioState().isOn()) {
            if (!this.mPhone.isPhoneTypeGsm() || !this.mPowerOffDelayNeed) {
                powerOffRadioSafely(this.mPhone.mDcTracker);
            } else if (!this.mImsRegistrationOnOff || this.mAlarmSwitch) {
                powerOffRadioSafely(this.mPhone.mDcTracker);
            } else {
                log("mImsRegistrationOnOff == true");
                Context context = this.mPhone.getContext();
                AlarmManager am = (AlarmManager) context.getSystemService("alarm");
                this.mRadioOffIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_RADIO_OFF), 0);
                this.mAlarmSwitch = true;
                log("Alarm setting");
                am.set(2, SystemClock.elapsedRealtime() + 3000, this.mRadioOffIntent);
            }
            if (this.mHwCustGsmServiceStateTracker != null) {
                this.mHwCustGsmServiceStateTracker.setRadioPower(this.mCi, false);
            }
        } else if (this.mDeviceShuttingDown && this.mCi.getRadioState().isAvailable()) {
            this.mCi.requestShutdown(null);
        }
    }

    protected void onUpdateIccAvailability() {
        if (this.mUiccController != null) {
            UiccCardApplication newUiccApplication = getUiccCardApplication();
            if (this.mUiccApplcation != newUiccApplication) {
                if (this.mUiccApplcation != null) {
                    log("Removing stale icc objects.");
                    this.mUiccApplcation.unregisterForReady(this);
                    this.mUiccApplcation.unregisterForGetAdDone(this);
                    if (this.mIccRecords != null) {
                        this.mIccRecords.unregisterForRecordsLoaded(this);
                        HwTelephonyFactory.getHwNetworkManager().unregisterForSimRecordsEvents(this, this.mPhone, this.mIccRecords);
                    }
                    this.mIccRecords = null;
                    this.mUiccApplcation = null;
                }
                if (newUiccApplication != null) {
                    log("New card found");
                    this.mUiccApplcation = newUiccApplication;
                    this.mIccRecords = this.mUiccApplcation.getIccRecords();
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mUiccApplcation.registerForReady(this, 17, null);
                        this.mUiccApplcation.registerForGetAdDone(this, 1002, null);
                        if (this.mIccRecords != null) {
                            this.mIccRecords.registerForRecordsLoaded(this, 16, null);
                            HwTelephonyFactory.getHwNetworkManager().registerForSimRecordsEvents(this, this.mPhone, this.mIccRecords);
                        }
                    } else if (this.mIsSubscriptionFromRuim) {
                        registerForRuimEvents();
                    }
                }
            }
        }
    }

    private void logRoamingChange() {
        this.mRoamingLog.log(this.mSS.toString());
    }

    private void logAttachChange() {
        this.mAttachLog.log(this.mSS.toString());
    }

    private void logPhoneTypeChange() {
        this.mPhoneTypeLog.log(Integer.toString(this.mPhone.getPhoneType()));
    }

    private void logRatChange() {
        this.mRatLog.log(this.mSS.toString());
    }

    protected void log(String s) {
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("] ");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    protected void loge(String s) {
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("] ");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    public int getCurrentDataConnectionState() {
        return this.mSS.getDataRegState();
    }

    public boolean isConcurrentVoiceAndDataAllowed() {
        boolean z = false;
        if (this.mSS.getCssIndicator() == 1) {
            return this.mSS.getRilDataRadioTechnology() == 14 && !MDOEM_WORK_MODE_IS_SRLTE;
        } else {
            if (!this.mPhone.isPhoneTypeGsm()) {
                return false;
            }
            if (SystemProperties.get("ro.hwpp.wcdma_voice_preference", "false").equals("true") && !this.isCurrent3GPsCsAllowed) {
                log("current not allow voice and data simultaneously by vp");
                return false;
            } else if (this.mSS.getRilDataRadioTechnology() >= 3 && this.mSS.getRilDataRadioTechnology() != 16) {
                return true;
            } else {
                if (this.mSS.getCssIndicator() == 1) {
                    z = true;
                }
                return z;
            }
        }
    }

    public void onImsServiceStateChanged() {
        sendMessage(obtainMessage(53));
    }

    public void setImsRegistrationState(boolean registered) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ImsRegistrationState - registered : ");
        stringBuilder.append(registered);
        log(stringBuilder.toString());
        if (this.mImsRegistrationOnOff && !registered && this.mAlarmSwitch) {
            this.mImsRegistrationOnOff = registered;
            ((AlarmManager) this.mPhone.getContext().getSystemService("alarm")).cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = false;
            sendMessage(obtainMessage(45));
            return;
        }
        this.mImsRegistrationOnOff = registered;
    }

    public void onImsCapabilityChanged() {
        sendMessage(obtainMessage(48));
    }

    public boolean isRadioOn() {
        return this.mCi.getRadioState() == RadioState.RADIO_ON;
    }

    public void pollState() {
        pollState(false);
    }

    private void modemTriggeredPollState() {
        pollState(true);
    }

    public void pollState(boolean modemTriggered) {
        this.mPollingContext = new int[1];
        this.mPollingContext[0] = 0;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pollState: modemTriggered=");
        stringBuilder.append(modemTriggered);
        stringBuilder.append(", radioState is ");
        stringBuilder.append(this.mCi.getRadioState());
        log(stringBuilder.toString());
        switch (this.mCi.getRadioState()) {
            case RADIO_UNAVAILABLE:
                this.mNewSS.setStateOutOfService();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mNitzState.handleNetworkUnavailable();
                pollStateDone();
                return;
            case RADIO_OFF:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mNitzState.handleNetworkUnavailable();
                if (this.mDeviceShuttingDown || !(modemTriggered || 18 == this.mSS.getRilDataRadioTechnology())) {
                    pollStateDone();
                    return;
                }
        }
        int[] iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        this.mCi.getOperator(obtainMessage(6, this.mPollingContext));
        iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        ((NetworkRegistrationManager) this.mRegStateManagers.get(1)).getNetworkRegistrationState(2, obtainMessage(5, this.mPollingContext));
        iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        ((NetworkRegistrationManager) this.mRegStateManagers.get(1)).getNetworkRegistrationState(1, obtainMessage(4, this.mPollingContext));
        if (this.mPhone.isPhoneTypeGsm()) {
            iArr = this.mPollingContext;
            iArr[0] = iArr[0] + 1;
            this.mCi.getNetworkSelectionMode(obtainMessage(14, this.mPollingContext));
            HwTelephonyFactory.getHwNetworkManager().getLocationInfo(this, this.mPhone);
        }
        if (this.mHwCustGsmServiceStateTracker != null) {
            this.mHwCustGsmServiceStateTracker.getLteFreqWithWlanCoex(this.mCi, this);
        }
    }

    private void updateOperatorProp() {
        if (this.mPhone != null && this.mSS != null) {
            this.mPhone.setSystemProperty("gsm.operator.alpha", this.mSS.getOperatorAlphaLong());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:276:0x07a7  */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x07bf  */
    /* JADX WARNING: Removed duplicated region for block: B:293:0x07f3  */
    /* JADX WARNING: Removed duplicated region for block: B:295:0x07fa  */
    /* JADX WARNING: Removed duplicated region for block: B:304:0x082e  */
    /* JADX WARNING: Removed duplicated region for block: B:306:0x0835  */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x0842  */
    /* JADX WARNING: Missing block: B:133:0x0288, code skipped:
            if (r0.mNewSS.getRilDataRadioTechnology() == true) goto L_0x028d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void pollStateDone() {
        boolean hasLacChanged;
        boolean hasLostMultiApnSupport;
        boolean hasDataRoamingOn;
        boolean hasVoiceRoamingOff;
        CellLocation cellLocation;
        boolean hasVoiceRoamingOn;
        String mccmnc;
        String mcc;
        boolean hasDataDetached;
        boolean hasDataRegStateChanged;
        boolean hasRilDataRadioTechnologyChanged;
        if (!this.mPhone.isPhoneTypeGsm()) {
            updateRoamingState();
        }
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        if (Build.IS_DEBUGGABLE && this.mPhone.mTelephonyTester != null) {
            this.mPhone.mTelephonyTester.overrideServiceState(this.mNewSS);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Poll ServiceState done:  oldSS=[");
        stringBuilder.append(this.mSS);
        stringBuilder.append("] newSS=[");
        stringBuilder.append(this.mNewSS);
        stringBuilder.append("] oldMaxDataCalls=");
        stringBuilder.append(this.mMaxDataCalls);
        stringBuilder.append(" mNewMaxDataCalls=");
        stringBuilder.append(this.mNewMaxDataCalls);
        stringBuilder.append(" oldReasonDataDenied=");
        stringBuilder.append(this.mReasonDataDenied);
        stringBuilder.append(" mNewReasonDataDenied=");
        stringBuilder.append(this.mNewReasonDataDenied);
        log(stringBuilder.toString());
        boolean hasRegistered = this.mSS.getVoiceRegState() != 0 && this.mNewSS.getVoiceRegState() == 0;
        boolean hasDeregistered = this.mSS.getVoiceRegState() == 0 && this.mNewSS.getVoiceRegState() != 0;
        boolean hasDataAttached = this.mSS.getDataRegState() != 0 && this.mNewSS.getDataRegState() == 0;
        boolean hasDataDetached2 = this.mSS.getDataRegState() == 0 && this.mNewSS.getDataRegState() != 0;
        boolean hasDataRegStateChanged2 = this.mSS.getDataRegState() != this.mNewSS.getDataRegState();
        boolean hasVoiceRegStateChanged = this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState();
        boolean hasOperatorNumericChanged = false;
        if (this.mNewSS.getOperatorNumeric() != null) {
            hasOperatorNumericChanged = this.mNewSS.getOperatorNumeric().equals(this.mSS.getOperatorNumeric()) ^ 1;
        }
        boolean hasLocationChanged = this.mNewCellLoc.equals(this.mCellLoc) ^ true;
        if (this.mHwCustGsmServiceStateTracker != null) {
            this.mHwCustGsmServiceStateTracker.updateLTEBandWidth(this.mNewSS);
        }
        boolean updateCaByCell = true;
        if (this.mHwCustGsmServiceStateTracker != null) {
            updateCaByCell = this.mHwCustGsmServiceStateTracker.isUpdateCAByCell(this.mNewSS);
        }
        boolean isCtVolte = this.mPhone.isCTSimCard(this.mPhone.getPhoneId()) && this.mImsRegistered;
        boolean isDataInService = this.mNewSS.getDataRegState() == 0;
        if (isDataInService && updateCaByCell && !isCtVolte) {
            this.mRatRatcheter.ratchet(this.mSS, this.mNewSS, hasLocationChanged);
        }
        boolean hasRilVoiceRadioTechnologyChanged = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology();
        boolean hasRilDataRadioTechnologyChanged2 = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology();
        boolean hasChanged = this.mNewSS.equals(this.mSS) ^ true;
        boolean hasVoiceRoamingOn2 = !this.mSS.getVoiceRoaming() && this.mNewSS.getVoiceRoaming();
        updateCaByCell = this.mSS.getVoiceRoaming() && !this.mNewSS.getVoiceRoaming();
        isCtVolte = !this.mSS.getDataRoaming() && this.mNewSS.getDataRoaming();
        isDataInService = this.mSS.getDataRoaming() && !this.mNewSS.getDataRoaming();
        boolean hasVoiceRegStateChanged2 = hasVoiceRegStateChanged;
        boolean hasOperatorNumericChanged2 = hasOperatorNumericChanged;
        boolean hasRejectCauseChanged = this.mRejectCode != this.mNewRejectCode;
        boolean hasCssIndicatorChanged = this.mSS.getCssIndicator() != this.mNewSS.getCssIndicator();
        hasOperatorNumericChanged = false;
        boolean needNotifyData = this.mSS.getCssIndicator() != this.mNewSS.getCssIndicator();
        if (this.mPhone.isPhoneTypeGsm()) {
            hasLacChanged = false;
            hasOperatorNumericChanged = ((GsmCellLocation) this.mNewCellLoc).isNotLacEquals((GsmCellLocation) this.mCellLoc);
        } else {
            hasLacChanged = false;
        }
        boolean has4gHandoff = false;
        boolean hasLacChanged2 = hasOperatorNumericChanged;
        if (this.mPhone.isPhoneTypeCdmaLte()) {
            hasVoiceRegStateChanged = this.mNewSS.getDataRegState() == 0 && ((ServiceState.isLte(this.mSS.getRilDataRadioTechnology()) && this.mNewSS.getRilDataRadioTechnology() == 13) || (this.mSS.getRilDataRadioTechnology() == 13 && ServiceState.isLte(this.mNewSS.getRilDataRadioTechnology())));
            boolean has4gHandoff2 = !ServiceState.isLte(this.mNewSS.getRilDataRadioTechnology()) ? hasVoiceRegStateChanged : hasVoiceRegStateChanged;
            if (!(ServiceState.isLte(this.mSS.getRilDataRadioTechnology()) || this.mSS.getRilDataRadioTechnology() == 13)) {
                hasVoiceRegStateChanged = true;
                hasLacChanged = hasVoiceRegStateChanged;
                hasVoiceRegStateChanged = this.mNewSS.getRilDataRadioTechnology() < 4 && this.mNewSS.getRilDataRadioTechnology() <= 8;
                hasOperatorNumericChanged = hasLacChanged;
                hasLostMultiApnSupport = hasVoiceRegStateChanged;
                hasVoiceRegStateChanged = has4gHandoff2;
            }
            hasVoiceRegStateChanged = false;
            hasLacChanged = hasVoiceRegStateChanged;
            if (this.mNewSS.getRilDataRadioTechnology() < 4) {
            }
            hasOperatorNumericChanged = hasLacChanged;
            hasLostMultiApnSupport = hasVoiceRegStateChanged;
            hasVoiceRegStateChanged = has4gHandoff2;
        } else {
            hasOperatorNumericChanged = false;
            hasLostMultiApnSupport = false;
            hasVoiceRegStateChanged = has4gHandoff;
        }
        boolean hasLostMultiApnSupport2 = hasLostMultiApnSupport;
        StringBuilder stringBuilder2 = new StringBuilder();
        boolean hasMultiApnSupport = hasOperatorNumericChanged;
        stringBuilder2.append("pollStateDone: hasRegistered=");
        stringBuilder2.append(hasRegistered);
        stringBuilder2.append(" hasDeregistered=");
        stringBuilder2.append(hasDeregistered);
        stringBuilder2.append(" hasDataAttached=");
        stringBuilder2.append(hasDataAttached);
        stringBuilder2.append(" hasDataDetached=");
        stringBuilder2.append(hasDataDetached2);
        stringBuilder2.append(" hasDataRegStateChanged=");
        stringBuilder2.append(hasDataRegStateChanged2);
        stringBuilder2.append(" hasRilVoiceRadioTechnologyChanged= ");
        stringBuilder2.append(hasRilVoiceRadioTechnologyChanged);
        stringBuilder2.append(" hasRilDataRadioTechnologyChanged=");
        stringBuilder2.append(hasRilDataRadioTechnologyChanged2);
        stringBuilder2.append(" hasChanged=");
        stringBuilder2.append(hasChanged);
        stringBuilder2.append(" hasVoiceRoamingOn=");
        stringBuilder2.append(hasVoiceRoamingOn2);
        stringBuilder2.append(" hasVoiceRoamingOff=");
        stringBuilder2.append(updateCaByCell);
        stringBuilder2.append(" hasDataRoamingOn=");
        stringBuilder2.append(isCtVolte);
        stringBuilder2.append(" hasDataRoamingOff=");
        stringBuilder2.append(isDataInService);
        stringBuilder2.append(" hasLocationChanged=");
        stringBuilder2.append(hasLocationChanged);
        stringBuilder2.append(" has4gHandoff = ");
        stringBuilder2.append(hasVoiceRegStateChanged);
        stringBuilder2.append(" hasMultiApnSupport=");
        hasOperatorNumericChanged = hasMultiApnSupport;
        stringBuilder2.append(hasOperatorNumericChanged);
        stringBuilder2.append(" hasLostMultiApnSupport=");
        hasOperatorNumericChanged = hasLostMultiApnSupport2;
        stringBuilder2.append(hasOperatorNumericChanged);
        boolean hasLostMultiApnSupport3 = hasOperatorNumericChanged;
        stringBuilder2.append(" hasCssIndicatorChanged=");
        hasOperatorNumericChanged = hasCssIndicatorChanged;
        stringBuilder2.append(hasOperatorNumericChanged);
        boolean hasLocationChanged2 = hasLocationChanged;
        stringBuilder2.append(" hasOperatorNumericChanged");
        hasLocationChanged = hasOperatorNumericChanged2;
        stringBuilder2.append(hasLocationChanged);
        boolean hasDataRoamingOff = isDataInService;
        boolean z = hasLostMultiApnSupport3;
        log(stringBuilder2.toString());
        if (hasVoiceRegStateChanged2 || hasDataRegStateChanged2) {
            int i;
            if (this.mPhone.isPhoneTypeGsm()) {
                i = EventLogTags.GSM_SERVICE_STATE_CHANGE;
            } else {
                i = EventLogTags.CDMA_SERVICE_STATE_CHANGE;
            }
            hasDataRoamingOn = isCtVolte;
            r12 = new Object[true];
            hasVoiceRoamingOff = updateCaByCell;
            r12[0] = Integer.valueOf(this.mSS.getVoiceRegState());
            r12[1] = Integer.valueOf(this.mSS.getDataRegState());
            r12[2] = Integer.valueOf(this.mNewSS.getVoiceRegState());
            r12[3] = Integer.valueOf(this.mNewSS.getDataRegState());
            EventLog.writeEvent(i, r12);
        } else {
            hasVoiceRoamingOff = updateCaByCell;
            hasDataRoamingOn = isCtVolte;
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            if (hasRilVoiceRadioTechnologyChanged) {
                cellLocation = this.mNewCellLoc;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("RAT switched ");
                stringBuilder3.append(ServiceState.rilRadioTechnologyToString(this.mSS.getRilVoiceRadioTechnology()));
                stringBuilder3.append(" -> ");
                stringBuilder3.append(ServiceState.rilRadioTechnologyToString(this.mNewSS.getRilVoiceRadioTechnology()));
                stringBuilder3.append(" at cell ");
                stringBuilder3.append(-1);
                log(stringBuilder3.toString());
            }
            if (hasOperatorNumericChanged) {
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_CSS_INDICATOR_CHANGED);
            }
            if (hasChanged && hasRegistered) {
                this.mPhone.mDcTracker.setMPDNByNetWork(this.mNewSS.getOperatorNumeric());
            }
            this.mReasonDataDenied = this.mNewReasonDataDenied;
            this.mMaxDataCalls = this.mNewMaxDataCalls;
            this.mRejectCode = this.mNewRejectCode;
        }
        if (hasRegistered || hasDataAttached) {
            log("service state hasRegistered , poll signal strength at once");
            sendMessage(obtainMessage(10));
        }
        ServiceState oldMergedSS = this.mPhone.getServiceState();
        if (this.mPhone.isPhoneTypeGsm()) {
            hasVoiceRoamingOn = hasVoiceRoamingOn2;
            if (HwTelephonyFactory.getHwNetworkManager().proccessGsmDelayUpdateRegisterStateDone(this, this.mPhone, this.mSS, this.mNewSS)) {
                return;
            }
        }
        hasVoiceRoamingOn = hasVoiceRoamingOn2;
        if (this.mPhone.isPhoneTypeCdmaLte() && HwTelephonyFactory.getHwNetworkManager().proccessCdmaLteDelayUpdateRegisterStateDone(this, this.mPhone, this.mSS, this.mNewSS)) {
            return;
        }
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        this.mNewSS.setStateOutOfService();
        cellLocation = this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = cellLocation;
        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (hasRilDataRadioTechnologyChanged2) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
            StatsLog.write(76, ServiceState.rilRadioTechnologyToNetworkType(this.mSS.getRilDataRadioTechnology()), this.mPhone.getPhoneId());
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }
        } else {
            CellLocation cellLocation2 = cellLocation;
        }
        if (hasRegistered || hasLocationChanged) {
            this.mPhone.getContext().sendBroadcast(new Intent("com.android.net.wifi.countryCode"));
            this.mNetworkAttachedRegistrants.notifyRegistrants();
            if (CLEAR_NITZ_WHEN_REG) {
                if (SystemClock.elapsedRealtime() - this.mLastReceivedNITZReferenceTime > 5000) {
                    this.mNitzState.handleNetworkAvailable();
                }
            }
        } else {
            boolean z2 = hasLocationChanged;
        }
        if (hasDeregistered) {
            if (this.mPhone.isPhoneTypeCdma() && SystemProperties.getBoolean("ro.config_hw_doubletime", false)) {
                mccmnc = this.mSS.getOperatorNumeric();
                mcc = "";
                if (mccmnc != null) {
                    System.putString(this.mCr, "last_registed_mcc", mccmnc.substring(0, 3));
                }
            }
            this.mNetworkDetachedRegistrants.notifyRegistrants();
            this.mNitzState.handleNetworkUnavailable();
        }
        if (hasRejectCauseChanged) {
            setNotification(2001);
        }
        boolean hasRegistered2;
        boolean hasRilVoiceRadioTechnologyChanged2;
        boolean hasDeregistered2;
        if (hasChanged) {
            updateSpnDisplay();
            tm.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlpha());
            if (!this.mPhone.isPhoneTypeCdma()) {
                updateOperatorProp();
            }
            if (this.mPhone.isPhoneTypeGsm()) {
                judgeToLaunchCsgPeriodicSearchTimer();
            }
            mccmnc = tm.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String prevCountryIsoCode = tm.getNetworkCountryIso(this.mPhone.getPhoneId());
            mcc = this.mSS.getOperatorNumeric();
            if (!this.mPhone.isPhoneTypeGsm() && isInvalidOperatorNumeric(mcc)) {
                mcc = fixUnknownMcc(mcc, this.mSS.getCdmaSystemId());
            }
            tm.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), mcc);
            if (isInvalidOperatorNumeric(mcc)) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("operatorNumeric ");
                stringBuilder4.append(mcc);
                stringBuilder4.append(" is invalid");
                log(stringBuilder4.toString());
                this.mLocaleTracker.updateOperatorNumericAsync("");
                this.mNitzState.handleNetworkUnavailable();
                hasRegistered2 = hasRegistered;
                hasRilVoiceRadioTechnologyChanged2 = hasRilVoiceRadioTechnologyChanged;
                hasDeregistered2 = hasDeregistered;
                hasDataDetached = hasDataDetached2;
                hasDataRegStateChanged = hasDataRegStateChanged2;
                hasRilDataRadioTechnologyChanged = hasRilDataRadioTechnologyChanged2;
            } else {
                if (this.mSS.getRilDataRadioTechnology() != 18) {
                    if (!this.mPhone.isPhoneTypeGsm()) {
                        setOperatorIdd(mcc);
                    }
                    this.mLocaleTracker.updateOperatorNumericSync(mcc);
                    String countryIsoCode = this.mLocaleTracker.getCurrentCountry();
                    hasVoiceRoamingOn2 = iccCardExists();
                    hasDataRegStateChanged = hasDataRegStateChanged2;
                    hasDataRegStateChanged2 = networkCountryIsoChanged(countryIsoCode, prevCountryIsoCode);
                    hasOperatorNumericChanged2 = hasVoiceRoamingOn2 && hasDataRegStateChanged2;
                    boolean countryChanged = hasOperatorNumericChanged2;
                    hasRilVoiceRadioTechnologyChanged2 = hasRilVoiceRadioTechnologyChanged;
                    hasDeregistered2 = hasDeregistered;
                    long ctm = System.currentTimeMillis();
                    hasRilDataRadioTechnologyChanged = hasRilDataRadioTechnologyChanged2;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    hasRegistered2 = hasRegistered;
                    stringBuilder5.append("Before handleNetworkCountryCodeKnown: countryChanged=");
                    hasRegistered = countryChanged;
                    stringBuilder5.append(hasRegistered);
                    hasDataDetached = hasDataDetached2;
                    stringBuilder5.append(" iccCardExist=");
                    stringBuilder5.append(hasVoiceRoamingOn2);
                    stringBuilder5.append(" countryIsoChanged=");
                    stringBuilder5.append(hasDataRegStateChanged2);
                    stringBuilder5.append(" operatorNumeric=");
                    stringBuilder5.append(mcc);
                    stringBuilder5.append(" prevOperatorNumeric=");
                    stringBuilder5.append(mccmnc);
                    stringBuilder5.append(" countryIsoCode=");
                    stringBuilder5.append(countryIsoCode);
                    stringBuilder5.append(" prevCountryIsoCode=");
                    stringBuilder5.append(prevCountryIsoCode);
                    stringBuilder5.append(" ltod=");
                    stringBuilder5.append(TimeUtils.logTimeOfDay(ctm));
                    log(stringBuilder5.toString());
                    this.mNitzState.handleNetworkCountryCodeSet(hasRegistered);
                } else {
                    hasRegistered2 = hasRegistered;
                    hasRilVoiceRadioTechnologyChanged2 = hasRilVoiceRadioTechnologyChanged;
                    hasDeregistered2 = hasDeregistered;
                    hasDataDetached = hasDataDetached2;
                    hasDataRegStateChanged = hasDataRegStateChanged2;
                    hasRilDataRadioTechnologyChanged = hasRilDataRadioTechnologyChanged2;
                }
            }
            int phoneId = this.mPhone.getPhoneId();
            hasChanged = this.mPhone.isPhoneTypeGsm() ? this.mSS.getVoiceRoaming() : this.mSS.getVoiceRoaming() || this.mSS.getDataRoaming();
            tm.setNetworkRoamingForPhone(phoneId, hasChanged);
            if (this.mHwCustGsmServiceStateTracker != null) {
                this.mHwCustGsmServiceStateTracker.setLTEUsageForRomaing(this.mSS.getVoiceRoaming());
            }
            setRoamingType(this.mSS);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Broadcasting ServiceState : ");
            stringBuilder.append(this.mSS);
            log(stringBuilder.toString());
            if (!oldMergedSS.equals(this.mPhone.getServiceState())) {
                this.mPhone.notifyServiceStateChanged(this.mPhone.getServiceState());
            }
            this.mPhone.getContext().getContentResolver().insert(ServiceStateTable.getUriForSubscriptionId(this.mPhone.getSubId()), ServiceStateTable.getContentValuesForServiceState(this.mSS));
            TelephonyMetrics.getInstance().writeServiceStateChanged(this.mPhone.getPhoneId(), this.mSS);
        } else {
            hasRegistered2 = hasRegistered;
            boolean z3 = hasChanged;
            hasRilVoiceRadioTechnologyChanged2 = hasRilVoiceRadioTechnologyChanged;
            hasDeregistered2 = hasDeregistered;
            hasDataDetached = hasDataDetached2;
            hasDataRegStateChanged = hasDataRegStateChanged2;
            hasRilDataRadioTechnologyChanged = hasRilDataRadioTechnologyChanged2;
        }
        if (hasDataAttached || hasVoiceRegStateChanged || hasDataDetached || hasRegistered || hasDeregistered) {
            logAttachChange();
        }
        if (hasDataAttached || hasVoiceRegStateChanged) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasDataDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasRilDataRadioTechnologyChanged || hasRilVoiceRadioTechnologyChanged) {
            logRatChange();
        }
        if (hasDataRegStateChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_IWLAN_AVAILABLE);
            } else {
                Message roamingOn;
                hasRegistered = true;
                if (hasRegistered) {
                    this.mPhone.notifyDataConnection(null);
                }
                if (hasVoiceRoamingOn || hasVoiceRoamingOff || hasDataRoamingOn || hasDataRoamingOff) {
                    logRoamingChange();
                }
                if (hasVoiceRoamingOn) {
                    if (this.mPhone.isPhoneTypeGsm() && SystemProperties.getBoolean("ro.config_hw_doubletime", false) && HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId() == getPhoneId()) {
                        log("[settimezone]roaming on, waiting for a few minutes to see if the NITZ is supported by the current network.");
                        roamingOn = obtainMessage();
                        roamingOn.what = 100;
                        sendMessageDelayed(roamingOn, 60000);
                    }
                    this.mVoiceRoamingOnRegistrants.notifyRegistrants();
                }
                if (hasVoiceRoamingOff) {
                    this.mVoiceRoamingOffRegistrants.notifyRegistrants();
                }
                if (hasDataRoamingOn) {
                    if (this.mPhone.isPhoneTypeGsm() && SystemProperties.getBoolean("ro.config_hw_doubletime", false) && HwFrameworkFactory.getHwInnerTelephonyManager().getDefault4GSlotId() == getPhoneId()) {
                        log("[settimezone]roaming on, waiting for a few minutes to see if the NITZ is supported by the current network.");
                        roamingOn = obtainMessage();
                        roamingOn.what = 100;
                        sendMessageDelayed(roamingOn, 60000);
                    }
                    this.mDataRoamingOnRegistrants.notifyRegistrants();
                }
                if (hasDataRoamingOff) {
                    this.mDataRoamingOffRegistrants.notifyRegistrants();
                }
                if (hasLocationChanged2) {
                    this.mPhone.notifyLocationChanged();
                }
                if (this.mPhone.isPhoneTypeGsm()) {
                    if (hasLacChanged2) {
                        Rlog.i(this.LOG_TAG, "LAC changed, update operator name display");
                        updateSpnDisplay();
                    }
                    if (isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
                        this.mReportedGprsNoReg = false;
                    } else if (!(this.mStartedGprsRegCheck || this.mReportedGprsNoReg)) {
                        this.mStartedGprsRegCheck = true;
                        sendMessageDelayed(obtainMessage(22), (long) Global.getInt(this.mPhone.getContext().getContentResolver(), "gprs_register_check_period_ms", 60000));
                    }
                }
            }
        }
        hasRegistered = needNotifyData;
        if (hasRegistered) {
        }
        logRoamingChange();
        if (hasVoiceRoamingOn) {
        }
        if (hasVoiceRoamingOff) {
        }
        if (hasDataRoamingOn) {
        }
        if (hasDataRoamingOff) {
        }
        if (hasLocationChanged2) {
        }
        if (this.mPhone.isPhoneTypeGsm()) {
        }
    }

    private void sendTimeZoneSelectionNotification() {
        String currentMcc = null;
        String operator = this.mSS.getOperatorNumeric();
        if (operator != null && operator.length() >= 3) {
            currentMcc = operator.substring(0, 3);
        }
        if (!this.mNitzState.getNitzTimeZoneDetectionSuccessful()) {
            List<TimeZone> timeZones = null;
            int tzListSize = 0;
            String iso = getSystemProperty("gsm.operator.iso-country", "");
            String lastMcc = System.getString(this.mCr, "last_registed_mcc");
            boolean isTheSameNWAsLast = (lastMcc == null || currentMcc == null || !lastMcc.equals(currentMcc)) ? false : true;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[settimezone] the network ");
            stringBuilder.append(operator);
            stringBuilder.append(" don't support nitz! current network isTheSameNWAsLast");
            stringBuilder.append(isTheSameNWAsLast);
            log(stringBuilder.toString());
            if (!"".equals(iso)) {
                timeZones = TimeZoneFinder.getInstance().lookupTimeZonesByCountry(iso);
                tzListSize = timeZones == null ? 0 : timeZones.size();
            }
            if (1 != tzListSize || isTheSameNWAsLast) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[settimezone] there are ");
                stringBuilder2.append(tzListSize);
                stringBuilder2.append(" timezones in ");
                stringBuilder2.append(iso);
                log(stringBuilder2.toString());
                Intent intent = new Intent(ACTION_TIMEZONE_SELECTION);
                intent.putExtra("operator", operator);
                intent.putExtra("iso", iso);
                this.mPhone.getContext().sendStickyBroadcast(intent);
            } else {
                TimeZone tz = (TimeZone) timeZones.get(0);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("[settimezone] time zone:");
                stringBuilder3.append(tz.getID());
                log(stringBuilder3.toString());
                TimeServiceHelper.setDeviceTimeZoneStatic(this.mPhone.getContext(), tz.getID());
            }
        }
        if (currentMcc != null) {
            System.putString(this.mCr, "last_registed_mcc", currentMcc);
        }
    }

    private void updateOperatorNameFromEri() {
        if (this.mPhone.isPhoneTypeCdma()) {
            if (this.mCi.getRadioState().isOn() && !this.mIsSubscriptionFromRuim) {
                String eriText;
                if (this.mSS.getVoiceRegState() == 0) {
                    eriText = this.mPhone.getCdmaEriText();
                } else {
                    eriText = this.mPhone.getContext().getText(17041033).toString();
                }
                this.mSS.setOperatorAlphaLong(eriText);
            }
        } else if (this.mPhone.isPhoneTypeCdmaLte()) {
            boolean hasBrandOverride = (this.mUiccController.getUiccCard(getPhoneId()) == null || this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() == null) ? false : true;
            if (!hasBrandOverride && this.mCi.getRadioState().isOn() && this.mPhone.isEriFileLoaded() && ((!ServiceState.isLte(this.mSS.getRilVoiceRadioTechnology()) || this.mPhone.getContext().getResources().getBoolean(17956867)) && !this.mIsSubscriptionFromRuim)) {
                String eriText2 = this.mSS.getOperatorAlpha();
                if (this.mSS.getVoiceRegState() == 0) {
                    eriText2 = this.mPhone.getCdmaEriText();
                } else if (this.mSS.getVoiceRegState() == 3) {
                    eriText2 = this.mIccRecords != null ? this.mIccRecords.getServiceProviderName() : null;
                    if (TextUtils.isEmpty(eriText2)) {
                        eriText2 = SystemProperties.get("ro.cdma.home.operator.alpha");
                    }
                } else if (this.mSS.getDataRegState() != 0) {
                    eriText2 = this.mPhone.getContext().getText(17041033).toString();
                }
                this.mSS.setOperatorAlphaLong(eriText2);
            }
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == AppState.APPSTATE_READY && this.mIccRecords != null) {
                if ((this.mSS.getVoiceRegState() == 0 || this.mSS.getDataRegState() == 0) && !ServiceState.isLte(this.mSS.getRilVoiceRadioTechnology())) {
                    boolean showSpn = ((RuimRecords) this.mIccRecords).getCsimSpnDisplayCondition();
                    int iconIndex = this.mSS.getCdmaEriIconIndex();
                    if (showSpn && iconIndex == 1 && isInHomeSidNid(this.mSS.getCdmaSystemId(), this.mSS.getCdmaNetworkId()) && this.mIccRecords != null && !TextUtils.isEmpty(this.mIccRecords.getServiceProviderName())) {
                        this.mSS.setOperatorAlphaLong(this.mIccRecords.getServiceProviderName());
                    }
                }
            }
        }
    }

    private boolean isInHomeSidNid(int sid, int nid) {
        if (isSidsAllZeros() || this.mHomeSystemId.length != this.mHomeNetworkId.length || sid == 0) {
            return true;
        }
        int i = 0;
        while (i < this.mHomeSystemId.length) {
            if (this.mHomeSystemId[i] == sid && (this.mHomeNetworkId[i] == 0 || this.mHomeNetworkId[i] == 65535 || nid == 0 || nid == 65535 || this.mHomeNetworkId[i] == nid)) {
                return true;
            }
            i++;
        }
        return false;
    }

    protected void setOperatorIdd(String operatorNumeric) {
        if (PLUS_TRANFER_IN_MDOEM) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setOperatorIdd() return. because of PLUS_TRANFER_IN_MDOEM=");
            stringBuilder.append(PLUS_TRANFER_IN_MDOEM);
            log(stringBuilder.toString());
            return;
        }
        String idd = this.mHbpcdUtils.getIddByMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
        if (idd == null || idd.isEmpty()) {
            this.mPhone.setGlobalSystemProperty("gsm.operator.idpstring", "+");
        } else {
            this.mPhone.setGlobalSystemProperty("gsm.operator.idpstring", idd);
        }
    }

    private boolean isInvalidOperatorNumeric(String operatorNumeric) {
        return operatorNumeric == null || operatorNumeric.length() < 5 || operatorNumeric.startsWith(INVALID_MCC);
    }

    private String fixUnknownMcc(String operatorNumeric, int sid) {
        if (sid <= 0) {
            return operatorNumeric;
        }
        java.util.TimeZone tzone;
        boolean isNitzTimeZone;
        int mcc = 0;
        if (this.mNitzState.getSavedTimeZoneId() != null) {
            tzone = java.util.TimeZone.getTimeZone(this.mNitzState.getSavedTimeZoneId());
            isNitzTimeZone = true;
        } else {
            java.util.TimeZone tzone2;
            NitzData lastNitzData = this.mNitzState.getCachedNitzData();
            if (lastNitzData == null) {
                tzone2 = null;
            } else {
                tzone2 = TimeZoneLookupHelper.guessZoneByNitzStatic(lastNitzData);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fixUnknownMcc(): guessNitzTimeZone returned ");
                stringBuilder.append(tzone2 == null ? tzone2 : tzone2.getID());
                log(stringBuilder.toString());
            }
            tzone = tzone2;
            isNitzTimeZone = false;
        }
        int utcOffsetHours = 0;
        if (tzone != null) {
            utcOffsetHours = tzone.getRawOffset() / MS_PER_HOUR;
        }
        NitzData nitzData = this.mNitzState.getCachedNitzData();
        boolean isDst = nitzData != null && nitzData.isDst();
        HbpcdUtils hbpcdUtils = this.mHbpcdUtils;
        if (isDst) {
            mcc = 1;
        }
        mcc = hbpcdUtils.getMcc(sid, utcOffsetHours, mcc, isNitzTimeZone);
        if (mcc > 0) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Integer.toString(mcc));
            stringBuilder2.append(DEFAULT_MNC);
            operatorNumeric = stringBuilder2.toString();
        }
        return operatorNumeric;
    }

    private boolean isGprsConsistent(int dataRegState, int voiceRegState) {
        return voiceRegState != 0 || dataRegState == 0;
    }

    private int regCodeToServiceState(int code) {
        if (code == 1 || code == 5) {
            return 0;
        }
        return 1;
    }

    private boolean regCodeIsRoaming(int code) {
        return 5 == code;
    }

    private boolean isSameOperatorNameFromSimAndSS(ServiceState s) {
        String spn = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNameForPhone(getPhoneId());
        String onsl = s.getOperatorAlphaLong();
        String onss = s.getOperatorAlphaShort();
        boolean equalsOnsl = !TextUtils.isEmpty(spn) && spn.equalsIgnoreCase(onsl);
        boolean equalsOnss = !TextUtils.isEmpty(spn) && spn.equalsIgnoreCase(onss);
        if (equalsOnsl || equalsOnss) {
            return true;
        }
        return false;
    }

    private boolean isSameNamedOperators(ServiceState s) {
        return currentMccEqualsSimMcc(s) && isSameOperatorNameFromSimAndSS(s);
    }

    private boolean currentMccEqualsSimMcc(ServiceState s) {
        try {
            return ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(getPhoneId()).substring(0, 3).equals(s.getOperatorNumeric().substring(0, 3));
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isOperatorConsideredNonRoaming(ServiceState s) {
        return false;
    }

    private boolean isOperatorConsideredRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        String[] numericArray = null;
        if (configManager != null) {
            PersistableBundle config = configManager.getConfigForSubId(this.mPhone.getSubId());
            if (config != null) {
                numericArray = config.getStringArray("roaming_operator_string_array");
            }
        }
        if (ArrayUtils.isEmpty(numericArray) || operatorNumeric == null) {
            return false;
        }
        for (String numeric : numericArray) {
            if (!TextUtils.isEmpty(numeric) && operatorNumeric.startsWith(numeric)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkForRoamingForIndianOperators(ServiceState s) {
        String simNumeric = SystemProperties.get("gsm.sim.operator.numeric", "");
        String operatorNumeric = s.getOperatorNumeric();
        try {
            String simMCC = simNumeric.substring(0, 3);
            String operatorMCC = operatorNumeric.substring(0, 3);
            if ((simMCC.equals("404") || simMCC.equals("405")) && (operatorMCC.equals("404") || operatorMCC.equals("405"))) {
                return true;
            }
        } catch (RuntimeException e) {
        }
        return false;
    }

    private void onRestrictedStateChanged(AsyncResult ar) {
        RestrictedState newRs = new RestrictedState();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRestrictedStateChanged: E rs ");
        stringBuilder.append(this.mRestrictedState);
        log(stringBuilder.toString());
        if (ar.exception == null && ar.result != null) {
            int state = ((Integer) ar.result).intValue();
            boolean z = false;
            boolean z2 = ((state & 1) == 0 && (state & 4) == 0) ? false : true;
            newRs.setCsEmergencyRestricted(z2);
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == AppState.APPSTATE_READY) {
                z2 = ((state & 2) == 0 && (state & 4) == 0) ? false : true;
                newRs.setCsNormalRestricted(z2);
                if ((state & 16) != 0) {
                    z = true;
                }
                newRs.setPsRestricted(z);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onRestrictedStateChanged: new rs ");
            stringBuilder2.append(newRs);
            log(stringBuilder2.toString());
            if (!this.mRestrictedState.isPsRestricted() && newRs.isPsRestricted()) {
                this.mPsRestrictEnabledRegistrants.notifyRegistrants();
                setNotification(1001);
            } else if (this.mRestrictedState.isPsRestricted() && !newRs.isPsRestricted()) {
                this.mPsRestrictDisabledRegistrants.notifyRegistrants();
                setNotification(1002);
            }
            if (this.mRestrictedState.isCsRestricted()) {
                if (!newRs.isAnyCsRestricted()) {
                    setNotification(1004);
                } else if (!newRs.isCsNormalRestricted()) {
                    setNotification(1006);
                } else if (!newRs.isCsEmergencyRestricted()) {
                    setNotification(1005);
                }
            } else if (!this.mRestrictedState.isCsEmergencyRestricted() || this.mRestrictedState.isCsNormalRestricted()) {
                if (this.mRestrictedState.isCsEmergencyRestricted() || !this.mRestrictedState.isCsNormalRestricted()) {
                    if (newRs.isCsRestricted()) {
                        setNotification(1003);
                    } else if (newRs.isCsEmergencyRestricted()) {
                        setNotification(1006);
                    } else if (newRs.isCsNormalRestricted()) {
                        setNotification(1005);
                    }
                } else if (!newRs.isAnyCsRestricted()) {
                    setNotification(1004);
                } else if (newRs.isCsRestricted()) {
                    setNotification(1003);
                } else if (newRs.isCsEmergencyRestricted()) {
                    setNotification(1006);
                }
            } else if (!newRs.isAnyCsRestricted()) {
                setNotification(1004);
            } else if (newRs.isCsRestricted()) {
                setNotification(1003);
            } else if (newRs.isCsNormalRestricted()) {
                setNotification(1005);
            }
            this.mRestrictedState = newRs;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("onRestrictedStateChanged: X rs ");
        stringBuilder.append(this.mRestrictedState);
        log(stringBuilder.toString());
    }

    public CellLocation getCellLocation(WorkSource workSource) {
        if (((GsmCellLocation) this.mCellLoc).getLac() >= 0 && ((GsmCellLocation) this.mCellLoc).getCid() >= 0) {
            return this.mCellLoc;
        }
        List<CellInfo> result = getAllCellInfo(workSource);
        if (result == null) {
            return this.mCellLoc;
        }
        GsmCellLocation cellLocOther = new GsmCellLocation();
        for (CellInfo ci : result) {
            if (ci instanceof CellInfoGsm) {
                CellIdentityGsm cellIdentityGsm = ((CellInfoGsm) ci).getCellIdentity();
                cellLocOther.setLacAndCid(cellIdentityGsm.getLac(), cellIdentityGsm.getCid());
                cellLocOther.setPsc(cellIdentityGsm.getPsc());
                return cellLocOther;
            } else if (ci instanceof CellInfoWcdma) {
                CellIdentityWcdma cellIdentityWcdma = ((CellInfoWcdma) ci).getCellIdentity();
                cellLocOther.setLacAndCid(cellIdentityWcdma.getLac(), cellIdentityWcdma.getCid());
                cellLocOther.setPsc(cellIdentityWcdma.getPsc());
                return cellLocOther;
            } else if ((ci instanceof CellInfoLte) && (cellLocOther.getLac() < 0 || cellLocOther.getCid() < 0)) {
                CellIdentityLte cellIdentityLte = ((CellInfoLte) ci).getCellIdentity();
                if (!(cellIdentityLte.getTac() == KeepaliveStatus.INVALID_HANDLE || cellIdentityLte.getCi() == KeepaliveStatus.INVALID_HANDLE)) {
                    cellLocOther.setLacAndCid(cellIdentityLte.getTac(), cellIdentityLte.getCi());
                    cellLocOther.setPsc(0);
                }
            }
        }
        return cellLocOther;
    }

    private void setTimeFromNITZString(String nitzString, long nitzReceiveTime) {
        long start = SystemClock.elapsedRealtime();
        String str = this.LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NITZ: ");
        stringBuilder.append(nitzString);
        stringBuilder.append(",");
        stringBuilder.append(nitzReceiveTime);
        stringBuilder.append(" start=");
        stringBuilder.append(start);
        stringBuilder.append(" delay=");
        stringBuilder.append(start - nitzReceiveTime);
        Rlog.d(str, stringBuilder.toString());
        NitzData newNitzData = NitzData.parse(nitzString);
        if (newNitzData != null) {
            try {
                this.mNitzState.handleNitzReceived(new TimeStampedValue(newNitzData, nitzReceiveTime));
            } finally {
                long end = SystemClock.elapsedRealtime();
                String str2 = this.LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("NITZ: end=");
                stringBuilder2.append(end);
                stringBuilder2.append(" dur=");
                stringBuilder2.append(end - start);
                Rlog.d(str2, stringBuilder2.toString());
            }
        }
    }

    private void cancelAllNotifications() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cancelAllNotifications: mPrevSubId=");
        stringBuilder.append(this.mPrevSubId);
        log(stringBuilder.toString());
        NotificationManager notificationManager = (NotificationManager) this.mPhone.getContext().getSystemService("notification");
        if (SubscriptionManager.isValidSubscriptionId(this.mPrevSubId)) {
            notificationManager.cancel(Integer.toString(this.mPrevSubId), PS_NOTIFICATION);
            notificationManager.cancel(Integer.toString(this.mPrevSubId), CS_NOTIFICATION);
            notificationManager.cancel(Integer.toString(this.mPrevSubId), 111);
        }
    }

    @VisibleForTesting
    public void setNotification(int notifyType) {
        int i = notifyType;
        StringBuilder stringBuilder;
        if (this.mHwCustGsmServiceStateTracker != null && this.mHwCustGsmServiceStateTracker.isCsPopShow(i)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("cs notification no need to send");
            stringBuilder.append(i);
            log(stringBuilder.toString());
        } else if (SystemProperties.getBoolean("ro.hwpp.cell_access_report", false)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("setNotification: create notification ");
            stringBuilder.append(i);
            log(stringBuilder.toString());
            if (!SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("cannot setNotification on invalid subid mSubId=");
                stringBuilder.append(this.mSubId);
                loge(stringBuilder.toString());
            } else if (!SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("cannot setNotification on invalid subid mSubId=");
                stringBuilder.append(this.mSubId);
                loge(stringBuilder.toString());
            } else if (this.mPhone.getContext().getResources().getBoolean(17957067)) {
                StringBuilder stringBuilder2;
                Context context = this.mPhone.getContext();
                CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService("carrier_config");
                if (configManager != null) {
                    PersistableBundle bundle = configManager.getConfig();
                    if (bundle != null && bundle.getBoolean("disable_voice_barring_notification_bool", false) && (i == 1003 || i == 1005 || i == 1006)) {
                        log("Voice/emergency call barred notification disabled");
                        return;
                    }
                }
                CharSequence details = "";
                CharSequence title = "";
                int notificationId = CS_NOTIFICATION;
                int icon = 17301642;
                boolean multipleSubscriptions = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getPhoneCount() > 1;
                int simNumber = this.mSubscriptionController.getSlotIndex(this.mSubId) + 1;
                if (i != 2001) {
                    String string;
                    switch (i) {
                        case 1001:
                            int simNumber2 = simNumber;
                            if (((long) SubscriptionManager.getDefaultDataSubscriptionId()) == ((long) this.mPhone.getSubId())) {
                                notificationId = PS_NOTIFICATION;
                                title = context.getText(17039466);
                                if (multipleSubscriptions) {
                                    string = context.getString(17039477, new Object[]{Integer.valueOf(simNumber2)});
                                } else {
                                    string = context.getText(17039476);
                                }
                                details = string;
                                break;
                            }
                            return;
                        case 1002:
                            notificationId = PS_NOTIFICATION;
                            break;
                        case 1003:
                            title = context.getText(17039463);
                            if (multipleSubscriptions) {
                                string = context.getString(17039477, new Object[]{Integer.valueOf(simNumber)});
                            } else {
                                string = context.getText(17039476);
                            }
                            details = string;
                            break;
                        case 1005:
                            title = context.getText(17039472);
                            if (multipleSubscriptions) {
                                string = context.getString(17039477, new Object[]{Integer.valueOf(simNumber)});
                            } else {
                                string = context.getText(17039476);
                            }
                            details = string;
                            break;
                        case 1006:
                            title = context.getText(17039469);
                            if (multipleSubscriptions) {
                                string = context.getString(17039477, new Object[]{Integer.valueOf(simNumber)});
                            } else {
                                string = context.getText(17039476);
                            }
                            details = string;
                            break;
                    }
                }
                notificationId = 111;
                int resId = selectResourceForRejectCode(this.mRejectCode, multipleSubscriptions);
                if (resId == 0) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("setNotification: mRejectCode=");
                    stringBuilder2.append(this.mRejectCode);
                    stringBuilder2.append(" is not handled.");
                    loge(stringBuilder2.toString());
                    return;
                }
                icon = 17303476;
                title = context.getString(resId, new Object[]{Integer.valueOf(this.mSubId)});
                details = null;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setNotification, create notification, notifyType: ");
                stringBuilder2.append(i);
                stringBuilder2.append(", title: ");
                stringBuilder2.append(title);
                stringBuilder2.append(", details: ");
                stringBuilder2.append(details);
                stringBuilder2.append(", subId: ");
                stringBuilder2.append(this.mSubId);
                log(stringBuilder2.toString());
                this.mNotification = new Builder(context).setWhen(System.currentTimeMillis()).setAutoCancel(true).setSmallIcon(icon).setTicker(title).setColor(context.getResources().getColor(17170784)).setContentTitle(title).setStyle(new BigTextStyle().bigText(details)).setContentText(details).setChannel(NotificationChannelController.CHANNEL_ID_ALERT).build();
                NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
                if (i == 1002 || i == 1004) {
                    notificationManager.cancel(Integer.toString(this.mSubId), notificationId);
                } else {
                    boolean show = false;
                    if (this.mSS.isEmergencyOnly() && i == 1006) {
                        show = true;
                    } else if (i == 2001) {
                        show = true;
                    } else if (this.mSS.getState() == 0) {
                        show = true;
                    }
                    if (show) {
                        notificationManager.notify(Integer.toString(this.mSubId), notificationId, this.mNotification);
                    }
                }
            } else {
                log("Ignore all the notifications");
            }
        }
    }

    private int selectResourceForRejectCode(int rejCode, boolean multipleSubscriptions) {
        int i;
        if (rejCode != 6) {
            switch (rejCode) {
                case 1:
                    if (multipleSubscriptions) {
                        i = 17040523;
                    } else {
                        i = 17040522;
                    }
                    return i;
                case 2:
                    if (multipleSubscriptions) {
                        i = 17040529;
                    } else {
                        i = 17040528;
                    }
                    return i;
                case 3:
                    if (multipleSubscriptions) {
                        i = 17040527;
                    } else {
                        i = 17040526;
                    }
                    return i;
                default:
                    return 0;
            }
        }
        if (multipleSubscriptions) {
            i = 17040525;
        } else {
            i = 17040524;
        }
        return i;
    }

    private UiccCardApplication getUiccCardApplication() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
        }
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 2);
    }

    private void queueNextSignalStrengthPoll() {
        if (!this.mDontPollSignalStrength && this.mDefaultDisplayState == 2) {
            Message msg = obtainMessage();
            msg.what = 10;
            sendMessageDelayed(msg, 20000);
        }
    }

    private void notifyCdmaSubscriptionInfoReady() {
        if (this.mCdmaForSubscriptionInfoReadyRegistrants != null) {
            log("CDMA_SUBSCRIPTION: call notifyRegistrants()");
            this.mCdmaForSubscriptionInfoReadyRegistrants.notifyRegistrants();
        }
    }

    public void registerForDataConnectionAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mAttachedRegistrants.add(r);
        if (getCurrentDataConnectionState() == 0) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataConnectionAttached(Handler h) {
        this.mAttachedRegistrants.remove(h);
    }

    public void registerForDataConnectionDetached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDetachedRegistrants.add(r);
        if (getCurrentDataConnectionState() != 0) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataConnectionDetached(Handler h) {
        this.mDetachedRegistrants.remove(h);
    }

    public void registerForDataRegStateOrRatChanged(Handler h, int what, Object obj) {
        this.mDataRegStateOrRatChangedRegistrants.add(new Registrant(h, what, obj));
        notifyDataRegStateRilRadioTechnologyChanged();
    }

    public void unregisterForDataRegStateOrRatChanged(Handler h) {
        this.mDataRegStateOrRatChangedRegistrants.remove(h);
    }

    public void registerForNetworkAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNetworkAttachedRegistrants.add(r);
        if (this.mSS.getVoiceRegState() == 0) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForNetworkAttached(Handler h) {
        this.mNetworkAttachedRegistrants.remove(h);
    }

    public void registerForNetworkDetached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNetworkDetachedRegistrants.add(r);
        if (this.mSS.getVoiceRegState() != 0) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForNetworkDetached(Handler h) {
        this.mNetworkDetachedRegistrants.remove(h);
    }

    public void registerForPsRestrictedEnabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPsRestrictEnabledRegistrants.add(r);
        if (this.mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedEnabled(Handler h) {
        this.mPsRestrictEnabledRegistrants.remove(h);
    }

    public void registerForPsRestrictedDisabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPsRestrictDisabledRegistrants.add(r);
        if (this.mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedDisabled(Handler h) {
        this.mPsRestrictDisabledRegistrants.remove(h);
    }

    public void powerOffRadioSafely(DcTracker dcTracker) {
        synchronized (this) {
            if (RESET_PROFILE && this.mPhone.getContext() != null && Global.getInt(this.mPhone.getContext().getContentResolver(), "airplane_mode_on", -1) == 0) {
                Rlog.d(this.LOG_TAG, "powerOffRadioSafely, it is not airplaneMode, resetProfile.");
                this.mCi.resetProfile(null);
            }
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                int dds = SubscriptionManager.getDefaultDataSubscriptionId();
                boolean isDisconnected = false;
                if (HW_FAST_SET_RADIO_OFF || IS_HISI_PLATFORM) {
                    isDisconnected = dcTracker.isDisconnectedOrConnecting();
                }
                if (!isDisconnected) {
                    if (dcTracker.isDisconnected()) {
                        if (dds != this.mPhone.getSubId() && (dds == this.mPhone.getSubId() || !ProxyController.getInstance().isDataDisconnected(dds))) {
                            if (!SubscriptionManager.isValidSubscriptionId(dds)) {
                            }
                        }
                    }
                    if (this.mPhone.isInCall()) {
                        this.mPhone.mCT.mRingingCall.hangupIfAlive();
                        this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
                        this.mPhone.mCT.mForegroundCall.hangupIfAlive();
                    }
                    ImsPhone imsPhone = null;
                    if (this.mPhone.getImsPhone() != null) {
                        imsPhone = (ImsPhone) this.mPhone.getImsPhone();
                    }
                    if (imsPhone != null && imsPhone.isInCall()) {
                        imsPhone.getForegroundCall().hangupIfAlive();
                        imsPhone.getBackgroundCall().hangupIfAlive();
                        imsPhone.getRingingCall().hangupIfAlive();
                    }
                    dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                    if (!(dds == this.mPhone.getSubId() || ProxyController.getInstance().isDataDisconnected(dds))) {
                        log("Data is active on DDS.  Wait for all data disconnect");
                        ProxyController.getInstance().registerForAllDataDisconnected(dds, this, 49, null);
                        this.mPendingRadioPowerOffAfterDataOff = true;
                    }
                    Message msg = Message.obtain(this);
                    msg.what = 38;
                    int i = this.mPendingRadioPowerOffAfterDataOffTag + 1;
                    this.mPendingRadioPowerOffAfterDataOffTag = i;
                    msg.arg1 = i;
                    if (sendMessageDelayed(msg, 30000)) {
                        log("Wait upto 30s for data to disconnect, then turn off radio.");
                        acquireWakeLock();
                        this.mPendingRadioPowerOffAfterDataOff = true;
                    } else {
                        log("Cannot send delayed Msg, turn off radio right away.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    }
                }
                dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                log("Data disconnected, turn off radio right away.");
                hangupAndPowerOff();
            }
        }
    }

    public boolean processPendingRadioPowerOffAfterDataOff() {
        synchronized (this) {
            if (this.mPendingRadioPowerOffAfterDataOff) {
                HwTelephonyFactory.getHwNetworkManager().delaySendDetachAfterDataOff(this.mPhone);
                this.mPendingRadioPowerOffAfterDataOffTag++;
                hangupAndPowerOff();
                this.mPendingRadioPowerOffAfterDataOff = false;
                return true;
            }
            return false;
        }
    }

    private boolean containsEarfcnInEarfcnRange(ArrayList<Pair<Integer, Integer>> earfcnPairList, int earfcn) {
        if (earfcnPairList != null) {
            Iterator it = earfcnPairList.iterator();
            while (it.hasNext()) {
                Pair<Integer, Integer> earfcnPair = (Pair) it.next();
                if (earfcn >= ((Integer) earfcnPair.first).intValue() && earfcn <= ((Integer) earfcnPair.second).intValue()) {
                    return true;
                }
            }
        }
        return false;
    }

    ArrayList<Pair<Integer, Integer>> convertEarfcnStringArrayToPairList(String[] earfcnsList) {
        ArrayList<Pair<Integer, Integer>> earfcnPairList = new ArrayList();
        if (earfcnsList != null) {
            int i = 0;
            while (i < earfcnsList.length) {
                try {
                    String[] earfcns = earfcnsList[i].split("-");
                    if (earfcns.length != 2) {
                        return null;
                    }
                    int earfcnStart = Integer.parseInt(earfcns[0]);
                    int earfcnEnd = Integer.parseInt(earfcns[1]);
                    if (earfcnStart > earfcnEnd) {
                        return null;
                    }
                    earfcnPairList.add(new Pair(Integer.valueOf(earfcnStart), Integer.valueOf(earfcnEnd)));
                    i++;
                } catch (PatternSyntaxException e) {
                    return null;
                } catch (NumberFormatException e2) {
                    return null;
                }
            }
        }
        return earfcnPairList;
    }

    private void onCarrierConfigChanged() {
        PersistableBundle config = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
        if (config != null) {
            updateLteEarfcnLists(config);
            updateReportingCriteria(config);
        }
    }

    private void updateLteEarfcnLists(PersistableBundle config) {
        synchronized (this.mLteRsrpBoostLock) {
            this.mLteRsrpBoost = config.getInt("lte_earfcns_rsrp_boost_int", 0);
            this.mEarfcnPairListForRsrpBoost = convertEarfcnStringArrayToPairList(config.getStringArray("boosted_lte_earfcns_string_array"));
        }
    }

    private void updateReportingCriteria(PersistableBundle config) {
        this.mPhone.setSignalStrengthReportingCriteria(config.getIntArray("lte_rsrp_thresholds_int_array"), 3);
        this.mPhone.setSignalStrengthReportingCriteria(config.getIntArray("wcdma_rscp_thresholds_int_array"), 2);
    }

    private void updateServiceStateLteEarfcnBoost(ServiceState serviceState, int lteEarfcn) {
        synchronized (this.mLteRsrpBoostLock) {
            if (lteEarfcn != -1) {
                try {
                    if (containsEarfcnInEarfcnRange(this.mEarfcnPairListForRsrpBoost, lteEarfcn)) {
                        serviceState.setLteEarfcnRsrpBoost(this.mLteRsrpBoost);
                    }
                } finally {
                }
            }
            serviceState.setLteEarfcnRsrpBoost(0);
        }
    }

    private boolean getDelaySendSignalFutureState() {
        int subId = this.mPhone.getSubId();
        Boolean valueFromCard = (Boolean) HwCfgFilePolicy.getValue("delay_send_signal", subId, Boolean.class);
        boolean valueFromProp = FEATURE_DELAY_UPDATE_SIGANL_STENGTH;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDelaySendSignalFutureState, subId:");
        stringBuilder.append(subId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        log(stringBuilder.toString());
        return valueFromCard != null ? valueFromCard.booleanValue() : valueFromProp;
    }

    protected boolean onSignalStrengthResult(AsyncResult ar) {
        boolean isGsm = false;
        int dataRat = this.mSS.getRilDataRadioTechnology();
        int voiceRat = this.mSS.getRilVoiceRadioTechnology();
        if (this.mPhone.isPhoneTypeGsm() || ((dataRat != 18 && ServiceState.isGsm(dataRat)) || (voiceRat != 18 && ServiceState.isGsm(voiceRat)))) {
            isGsm = true;
        }
        if (getDelaySendSignalFutureState()) {
            return onSignalStrengthResultHW(ar, isGsm);
        }
        if (ar.exception != null || ar.result == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSignalStrengthResult() Exception from RIL : ");
            stringBuilder.append(ar.exception);
            log(stringBuilder.toString());
            this.mSignalStrength = new SignalStrength(isGsm);
        } else {
            this.mSignalStrength = (SignalStrength) ar.result;
            this.mSignalStrength.validateInput();
            if (dataRat == 0 && voiceRat == 0) {
                this.mSignalStrength.fixType();
            } else {
                this.mSignalStrength.setGsm(isGsm);
            }
            this.mSignalStrength.setLteRsrpBoost(this.mSS.getLteEarfcnRsrpBoost());
            PersistableBundle config = getCarrierConfig();
            this.mSignalStrength.setUseOnlyRsrpForLteLevel(config.getBoolean("use_only_rsrp_for_lte_signal_bar_bool"));
            this.mSignalStrength.setLteRsrpThresholds(config.getIntArray("lte_rsrp_thresholds_int_array"));
            this.mSignalStrength.setWcdmaDefaultSignalMeasurement(config.getString("wcdma_default_signal_strength_measurement_string"));
            this.mSignalStrength.setWcdmaRscpThresholds(config.getIntArray("wcdma_rscp_thresholds_int_array"));
        }
        HwTelephonyFactory.getHwNetworkManager().updateHwnff(this, this.mSignalStrength);
        return notifySignalStrength();
    }

    protected boolean onSignalStrengthResultHW(AsyncResult ar, boolean isGsm) {
        SignalStrength newSignalStrength;
        SignalStrength oldSignalStrength = this.mSignalStrength;
        if (ar.exception == null && ar.result != null && (ar.result instanceof SignalStrength)) {
            newSignalStrength = ar.result;
            newSignalStrength.validateInput();
            newSignalStrength.setGsm(isGsm);
        } else {
            StringBuilder stringBuilder;
            if (ar.exception != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onSignalStrengthResult() Exception from RIL : ");
                stringBuilder.append(ar.exception);
                log(stringBuilder.toString());
            } else if (ar.result != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("result : ");
                stringBuilder.append(ar.result);
                log(stringBuilder.toString());
            } else {
                log("ar.result is null!");
            }
            newSignalStrength = new SignalStrength(isGsm);
        }
        HwTelephonyFactory.getHwNetworkManager().updateHwnff(this, newSignalStrength);
        if (this.mPhone.isPhoneTypeGsm()) {
            return HwTelephonyFactory.getHwNetworkManager().notifyGsmSignalStrength(this, this.mPhone, oldSignalStrength, newSignalStrength);
        }
        newSignalStrength.setCdma(true);
        return HwTelephonyFactory.getHwNetworkManager().notifyCdmaSignalStrength(this, this.mPhone, oldSignalStrength, newSignalStrength);
    }

    protected void hangupAndPowerOff() {
        if (!this.mPhone.isPhoneTypeGsm() || this.mPhone.isInCall()) {
            this.mPhone.mCT.mRingingCall.hangupIfAlive();
            this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
            this.mPhone.mCT.mForegroundCall.hangupIfAlive();
        }
        this.mCi.setRadioPower(false, obtainMessage(54));
    }

    protected void cancelPollState() {
        this.mPollingContext = new int[1];
    }

    private boolean networkCountryIsoChanged(String newCountryIsoCode, String prevCountryIsoCode) {
        if (TextUtils.isEmpty(newCountryIsoCode)) {
            log("countryIsoChanged: no new country ISO code");
            return false;
        } else if (!TextUtils.isEmpty(prevCountryIsoCode)) {
            return newCountryIsoCode.equals(prevCountryIsoCode) ^ 1;
        } else {
            log("countryIsoChanged: no previous country ISO code");
            return true;
        }
    }

    private boolean iccCardExists() {
        if (this.mUiccApplcation == null) {
            return false;
        }
        return this.mUiccApplcation.getState() != AppState.APPSTATE_UNKNOWN;
    }

    public String getSystemProperty(String property, String defValue) {
        return TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), property, defValue);
    }

    public List<CellInfo> getAllCellInfo(WorkSource workSource) {
        CellInfoResult result = new CellInfoResult(this, null);
        if (this.mCi.getRilVersion() < 8) {
            log("SST.getAllCellInfo(): not implemented");
            result.list = null;
        } else if (!isCallerOnDifferentThread()) {
            log("SST.getAllCellInfo(): return last, same thread can't block");
            result.list = this.mLastCellInfoList;
        } else if (HwTelephonyFactory.getHwNetworkManager().isCellRequestStrategyPassed(this, workSource, this.mPhone)) {
            Message msg = obtainMessage(43, result);
            synchronized (result.lockObj) {
                result.list = null;
                if (workSource != null) {
                    HwTelephonyFactory.getHwNetworkManager().countPackageUseCellInfo(this, this.mPhone, workSource.getName(null));
                }
                this.mCi.getCellInfoList(msg, workSource);
                try {
                    result.lockObj.wait(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            log("SST.getAllCellInfo(): return last, back to back calls");
            result.list = this.mLastCellInfoList;
        }
        synchronized (result.lockObj) {
            if (result.list != null) {
                List list = result.list;
                return list;
            }
            log("SST.getAllCellInfo(): X size=0 list=null");
            return null;
        }
    }

    public SignalStrength getSignalStrength() {
        return this.mSignalStrength;
    }

    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCdmaForSubscriptionInfoReadyRegistrants.add(r);
        if (isMinInfoReady()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
        this.mCdmaForSubscriptionInfoReadyRegistrants.remove(h);
    }

    private void saveCdmaSubscriptionSource(int source) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Storing cdma subscription source: ");
        stringBuilder.append(source);
        log(stringBuilder.toString());
        Global.putInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", source);
        stringBuilder = new StringBuilder();
        stringBuilder.append("Read from settings: ");
        stringBuilder.append(Global.getInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", -1));
        log(stringBuilder.toString());
    }

    private void getSubscriptionInfoAndStartPollingThreads() {
        this.mCi.getCDMASubscription(obtainMessage(34));
        pollState();
    }

    private void handleCdmaSubscriptionSource(int newSubscriptionSource) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Subscription Source : ");
        stringBuilder.append(newSubscriptionSource);
        log(stringBuilder.toString());
        this.mIsSubscriptionFromRuim = newSubscriptionSource == 0;
        stringBuilder = new StringBuilder();
        stringBuilder.append("isFromRuim: ");
        stringBuilder.append(this.mIsSubscriptionFromRuim);
        log(stringBuilder.toString());
        saveCdmaSubscriptionSource(newSubscriptionSource);
        if (this.mIsSubscriptionFromRuim) {
            registerForRuimEvents();
            return;
        }
        unregisterForRuimEvents();
        sendMessage(obtainMessage(35));
    }

    private void dumpEarfcnPairList(PrintWriter pw) {
        pw.print(" mEarfcnPairListForRsrpBoost={");
        if (this.mEarfcnPairListForRsrpBoost != null) {
            int i = this.mEarfcnPairListForRsrpBoost.size();
            Iterator it = this.mEarfcnPairListForRsrpBoost.iterator();
            while (it.hasNext()) {
                Pair<Integer, Integer> earfcnPair = (Pair) it.next();
                pw.print("(");
                pw.print(earfcnPair.first);
                pw.print(",");
                pw.print(earfcnPair.second);
                pw.print(")");
                i--;
                if (i != 0) {
                    pw.print(",");
                }
            }
        }
        pw.println("}");
    }

    private void dumpCellInfoList(PrintWriter pw) {
        pw.print(" mLastCellInfoList={");
        if (this.mLastCellInfoList != null) {
            boolean first = true;
            for (CellInfo info : this.mLastCellInfoList) {
                if (!first) {
                    pw.print(",");
                }
                first = false;
                pw.print(info.toString());
            }
        }
        pw.println("}");
    }

    private void registerForRuimEvents() {
        log("registerForRuimEvents");
        if (this.mUiccApplcation != null) {
            this.mUiccApplcation.registerForReady(this, 26, null);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.registerForRecordsLoaded(this, 27, null);
        }
    }

    private void unregisterForRuimEvents() {
        log("unregisterForRuimEvents");
        if (this.mUiccApplcation != null) {
            this.mUiccApplcation.unregisterForReady(this);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForRecordsLoaded(this);
        }
    }

    public void setSignalStrength(SignalStrength signalStrength) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSignalStrength : ");
        stringBuilder.append(signalStrength);
        log(stringBuilder.toString());
        this.mSignalStrength = signalStrength;
    }

    public void setDoRecoveryTriggerState(boolean state) {
        this.mRadioOffByDoRecovery = state;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDoRecoveryTriggerState, ");
        stringBuilder.append(this.mRadioOffByDoRecovery);
        log(stringBuilder.toString());
    }

    public boolean getDoRecoveryTriggerState() {
        return this.mRadioOffByDoRecovery;
    }

    public void setDoRecoveryMarker(boolean state) {
        this.mDoRecoveryMarker = state;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDoRecoveryMarker, ");
        stringBuilder.append(this.mDoRecoveryMarker);
        log(stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ServiceStateTracker:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mSubId=");
        stringBuilder.append(this.mSubId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mSS=");
        stringBuilder.append(this.mSS);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNewSS=");
        stringBuilder.append(this.mNewSS);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mVoiceCapable=");
        stringBuilder.append(this.mVoiceCapable);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mRestrictedState=");
        stringBuilder.append(this.mRestrictedState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPollingContext=");
        stringBuilder.append(this.mPollingContext);
        stringBuilder.append(" - ");
        stringBuilder.append(this.mPollingContext != null ? Integer.valueOf(this.mPollingContext[0]) : "");
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mDesiredPowerState=");
        stringBuilder.append(this.mDesiredPowerState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mDontPollSignalStrength=");
        stringBuilder.append(this.mDontPollSignalStrength);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mSignalStrength=");
        stringBuilder.append(this.mSignalStrength);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mLastSignalStrength=");
        stringBuilder.append(this.mLastSignalStrength);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mRestrictedState=");
        stringBuilder.append(this.mRestrictedState);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPendingRadioPowerOffAfterDataOff=");
        stringBuilder.append(this.mPendingRadioPowerOffAfterDataOff);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPendingRadioPowerOffAfterDataOffTag=");
        stringBuilder.append(this.mPendingRadioPowerOffAfterDataOffTag);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCellLoc=");
        stringBuilder.append(Rlog.pii(false, this.mCellLoc));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNewCellLoc=");
        stringBuilder.append(Rlog.pii(false, this.mNewCellLoc));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mLastCellInfoListTime=");
        stringBuilder.append(this.mLastCellInfoListTime);
        pw.println(stringBuilder.toString());
        dumpCellInfoList(pw);
        pw.flush();
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPreferredNetworkType=");
        stringBuilder.append(this.mPreferredNetworkType);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mMaxDataCalls=");
        stringBuilder.append(this.mMaxDataCalls);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNewMaxDataCalls=");
        stringBuilder.append(this.mNewMaxDataCalls);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mReasonDataDenied=");
        stringBuilder.append(this.mReasonDataDenied);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNewReasonDataDenied=");
        stringBuilder.append(this.mNewReasonDataDenied);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mGsmRoaming=");
        stringBuilder.append(this.mGsmRoaming);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mDataRoaming=");
        stringBuilder.append(this.mDataRoaming);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mEmergencyOnly=");
        stringBuilder.append(this.mEmergencyOnly);
        pw.println(stringBuilder.toString());
        pw.flush();
        this.mNitzState.dumpState(pw);
        pw.flush();
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mStartedGprsRegCheck=");
        stringBuilder.append(this.mStartedGprsRegCheck);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mReportedGprsNoReg=");
        stringBuilder.append(this.mReportedGprsNoReg);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mNotification=");
        stringBuilder.append(this.mNotification);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurSpn=");
        stringBuilder.append(this.mCurSpn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurDataSpn=");
        stringBuilder.append(this.mCurDataSpn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurShowSpn=");
        stringBuilder.append(this.mCurShowSpn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurPlmn=");
        stringBuilder.append(this.mCurPlmn);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurShowPlmn=");
        stringBuilder.append(this.mCurShowPlmn);
        pw.println(stringBuilder.toString());
        pw.flush();
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurrentOtaspMode=");
        stringBuilder.append(this.mCurrentOtaspMode);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mRoamingIndicator=");
        stringBuilder.append(this.mRoamingIndicator);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIsInPrl=");
        stringBuilder.append(this.mIsInPrl);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mDefaultRoamingIndicator=");
        stringBuilder.append(this.mDefaultRoamingIndicator);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mRegistrationState=");
        stringBuilder.append(this.mRegistrationState);
        pw.println(stringBuilder.toString());
        pw.println(" mMdn=xxxx");
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mHomeSystemId=");
        stringBuilder.append(this.mHomeSystemId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mHomeNetworkId=");
        stringBuilder.append(this.mHomeNetworkId);
        pw.println(stringBuilder.toString());
        pw.println(" mMin=xxxx");
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPrlVersion=");
        stringBuilder.append(this.mPrlVersion);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIsMinInfoReady=");
        stringBuilder.append(this.mIsMinInfoReady);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIsEriTextLoaded=");
        stringBuilder.append(this.mIsEriTextLoaded);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mIsSubscriptionFromRuim=");
        stringBuilder.append(this.mIsSubscriptionFromRuim);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCdmaSSM=");
        stringBuilder.append(this.mCdmaSSM);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mRegistrationDeniedReason=");
        stringBuilder.append(this.mRegistrationDeniedReason);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCurrentCarrier=");
        stringBuilder.append(this.mCurrentCarrier);
        pw.println(stringBuilder.toString());
        pw.flush();
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mImsRegistered=");
        stringBuilder.append(this.mImsRegistered);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mImsRegistrationOnOff=");
        stringBuilder.append(this.mImsRegistrationOnOff);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mAlarmSwitch=");
        stringBuilder.append(this.mAlarmSwitch);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mRadioDisabledByCarrier");
        stringBuilder.append(this.mRadioDisabledByCarrier);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mPowerOffDelayNeed=");
        stringBuilder.append(this.mPowerOffDelayNeed);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mDeviceShuttingDown=");
        stringBuilder.append(this.mDeviceShuttingDown);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mSpnUpdatePending=");
        stringBuilder.append(this.mSpnUpdatePending);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mLteRsrpBoost=");
        stringBuilder.append(this.mLteRsrpBoost);
        pw.println(stringBuilder.toString());
        dumpEarfcnPairList(pw);
        this.mLocaleTracker.dump(fd, pw, args);
        pw.println(" Roaming Log:");
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.increaseIndent();
        this.mRoamingLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.println(" Attach Log:");
        ipw.increaseIndent();
        this.mAttachLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.println(" Phone Change Log:");
        ipw.increaseIndent();
        this.mPhoneTypeLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.println(" Rat Change Log:");
        ipw.increaseIndent();
        this.mRatLog.dump(fd, ipw, args);
        ipw.decreaseIndent();
        ipw.println(" Radio power Log:");
        ipw.increaseIndent();
        this.mRadioPowerLog.dump(fd, ipw, args);
        this.mNitzState.dumpLogs(fd, ipw, args);
    }

    public boolean isImsRegistered() {
        return this.mImsRegistered;
    }

    protected void checkCorrectThread() {
        if (Thread.currentThread() != getLooper().getThread()) {
            throw new RuntimeException("ServiceStateTracker must be used from within one thread");
        }
    }

    protected boolean isCallerOnDifferentThread() {
        return Thread.currentThread() != getLooper().getThread();
    }

    protected void updateCarrierMccMncConfiguration(String newOp, String oldOp, Context context) {
        if ((newOp == null && !TextUtils.isEmpty(oldOp)) || (newOp != null && !newOp.equals(oldOp))) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update mccmnc=");
            stringBuilder.append(newOp);
            stringBuilder.append(" fromServiceState=true");
            log(stringBuilder.toString());
            MccTable.updateMccMncConfiguration(context, newOp, true);
        }
    }

    protected boolean inSameCountry(String operatorNumeric) {
        if (TextUtils.isEmpty(operatorNumeric) || operatorNumeric.length() < 5) {
            return false;
        }
        String homeNumeric = getHomeOperatorNumeric();
        if (TextUtils.isEmpty(homeNumeric) || homeNumeric.length() < 5) {
            return false;
        }
        String networkMCC = operatorNumeric.substring(0, 3);
        String homeMCC = homeNumeric.substring(0, 3);
        String networkCountry = "";
        String homeCountry = "";
        try {
            networkCountry = MccTable.countryCodeForMcc(Integer.parseInt(networkMCC));
            homeCountry = MccTable.countryCodeForMcc(Integer.parseInt(homeMCC));
        } catch (NumberFormatException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("inSameCountry: get networkCountry or homeCountry error: ");
            stringBuilder.append(ex);
            log(stringBuilder.toString());
        }
        if (networkCountry.isEmpty() || homeCountry.isEmpty()) {
            return false;
        }
        boolean inSameCountry = homeCountry.equals(networkCountry);
        if (inSameCountry) {
            return inSameCountry;
        }
        if ("us".equals(homeCountry) && "vi".equals(networkCountry)) {
            inSameCountry = true;
        } else if ("vi".equals(homeCountry) && "us".equals(networkCountry)) {
            inSameCountry = true;
        }
        return inSameCountry;
    }

    protected void setRoamingType(ServiceState currentServiceState) {
        int curRoamingIndicator;
        boolean isVoiceInService = currentServiceState.getVoiceRegState() == 0;
        if (isVoiceInService) {
            if (!currentServiceState.getVoiceRoaming()) {
                currentServiceState.setVoiceRoamingType(0);
            } else if (!this.mPhone.isPhoneTypeGsm()) {
                int[] intRoamingIndicators = this.mPhone.getContext().getResources().getIntArray(17235994);
                if (intRoamingIndicators != null && intRoamingIndicators.length > 0) {
                    currentServiceState.setVoiceRoamingType(2);
                    curRoamingIndicator = currentServiceState.getCdmaRoamingIndicator();
                    for (int i : intRoamingIndicators) {
                        if (curRoamingIndicator == i) {
                            currentServiceState.setVoiceRoamingType(3);
                            break;
                        }
                    }
                } else if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                    currentServiceState.setVoiceRoamingType(2);
                } else {
                    currentServiceState.setVoiceRoamingType(3);
                }
            } else if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                currentServiceState.setVoiceRoamingType(2);
            } else {
                currentServiceState.setVoiceRoamingType(3);
            }
        }
        boolean isDataInService = currentServiceState.getDataRegState() == 0;
        curRoamingIndicator = currentServiceState.getRilDataRadioTechnology();
        if (!isDataInService) {
            return;
        }
        if (!currentServiceState.getDataRoaming()) {
            currentServiceState.setDataRoamingType(0);
        } else if (this.mPhone.isPhoneTypeGsm()) {
            if (!ServiceState.isGsm(curRoamingIndicator)) {
                currentServiceState.setDataRoamingType(1);
            } else if (isVoiceInService) {
                currentServiceState.setDataRoamingType(currentServiceState.getVoiceRoamingType());
            } else {
                currentServiceState.setDataRoamingType(1);
            }
        } else if (ServiceState.isCdma(curRoamingIndicator)) {
            if (isVoiceInService) {
                currentServiceState.setDataRoamingType(currentServiceState.getVoiceRoamingType());
            } else {
                currentServiceState.setDataRoamingType(1);
            }
        } else if (inSameCountry(currentServiceState.getDataOperatorNumeric())) {
            currentServiceState.setDataRoamingType(2);
        } else {
            currentServiceState.setDataRoamingType(3);
        }
    }

    private void setSignalStrengthDefaultValues() {
        this.mSignalStrength = new SignalStrength(true);
    }

    protected String getHomeOperatorNumeric() {
        String numeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (this.mPhone.isPhoneTypeGsm() || !TextUtils.isEmpty(numeric)) {
            return numeric;
        }
        return SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "");
    }

    protected int getPhoneId() {
        return this.mPhone.getPhoneId();
    }

    protected void resetServiceStateInIwlanMode() {
        if (this.mCi.getRadioState() == RadioState.RADIO_OFF) {
            boolean resetIwlanRatVal = false;
            log("set service state as POWER_OFF");
            if (18 == this.mNewSS.getRilDataRadioTechnology()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pollStateDone: mNewSS = ");
                stringBuilder.append(this.mNewSS);
                log(stringBuilder.toString());
                log("pollStateDone: reset iwlan RAT value");
                resetIwlanRatVal = true;
            }
            String operator = this.mNewSS.getOperatorAlphaLong();
            this.mNewSS.setStateOff();
            if (resetIwlanRatVal) {
                this.mNewSS.setRilDataRadioTechnology(18);
                this.mNewSS.setDataRegState(0);
                this.mNewSS.setOperatorAlphaLong(operator);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pollStateDone: mNewSS = ");
                stringBuilder2.append(this.mNewSS);
                log(stringBuilder2.toString());
            }
        }
    }

    protected final boolean alwaysOnHomeNetwork(BaseBundle b) {
        return b.getBoolean("force_home_network_bool");
    }

    private boolean isInNetwork(BaseBundle b, String network, String key) {
        String[] networks = b.getStringArray(key);
        if (networks == null || !Arrays.asList(networks).contains(network)) {
            return false;
        }
        return true;
    }

    protected final boolean isRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "gsm_roaming_networks_string_array");
    }

    protected final boolean isNonRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "gsm_nonroaming_networks_string_array");
    }

    protected final boolean isRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "cdma_roaming_networks_string_array");
    }

    protected final boolean isNonRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "cdma_nonroaming_networks_string_array");
    }

    public boolean isDeviceShuttingDown() {
        return this.mDeviceShuttingDown;
    }

    protected void getCaller() {
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("    at ");
            stringBuilder.append(ste.getClassName());
            stringBuilder.append(".");
            stringBuilder.append(ste.getMethodName());
            stringBuilder.append("(");
            stringBuilder.append(ste.getFileName());
            stringBuilder.append(":");
            stringBuilder.append(ste.getLineNumber());
            stringBuilder.append(")");
            log(stringBuilder.toString());
        }
    }

    protected void acquireWakeLock() {
        if (this.mWakeLock == null) {
            this.mWakeLock = ((PowerManager) this.mPhone.getContext().getSystemService("power")).newWakeLock(1, "SERVICESTATE_WAIT_DISCONNECT_WAKELOCK");
        }
        this.mWakeLock.setReferenceCounted(false);
        log("Servicestate wait disconnect, acquire wakelock");
        this.mWakeLock.acquire();
    }

    protected void releaseWakeLock() {
        if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
            log("release wakelock");
        }
    }

    protected void judgeToLaunchCsgPeriodicSearchTimer() {
        if (this.mHwCustGsmServiceStateTracker != null) {
            this.mHwCustGsmServiceStateTracker.judgeToLaunchCsgPeriodicSearchTimer();
            log("mHwCustGsmServiceStateTracker is not null");
        }
    }

    public boolean isContainPackage(String data, String packageName) {
        String[] enablePackage = null;
        if (!TextUtils.isEmpty(data)) {
            enablePackage = data.split(";");
        }
        if (enablePackage == null || enablePackage.length == 0) {
            return false;
        }
        int i = 0;
        while (i < enablePackage.length) {
            if (!TextUtils.isEmpty(packageName) && packageName.equals(enablePackage[i])) {
                return true;
            }
            i++;
        }
        return false;
    }

    public HwCustGsmServiceStateTracker returnObject() {
        return this.mHwCustGsmServiceStateTracker;
    }

    private boolean isOperatorChanged(boolean showPlmn, boolean showSpn, String spn, String dataSpn, String plmn, boolean showWifi, String wifi, String regplmn) {
        boolean isRealChange = (true == HwModemCapability.isCapabilitySupport(9) && TextUtils.isEmpty(this.mSS.getOperatorNumeric()) && HwTelephonyFactory.getHwNetworkManager().getGsmCombinedRegState(this, this.mPhone, this.mSS) == 0) ? false : true;
        if ((showPlmn != this.mCurShowPlmn || showSpn != this.mCurShowSpn || !TextUtils.equals(spn, this.mCurSpn) || !TextUtils.equals(dataSpn, this.mCurDataSpn) || !TextUtils.equals(plmn, this.mCurPlmn) || showWifi != this.mCurShowWifi || !TextUtils.equals(wifi, this.mCurWifi) || !TextUtils.equals(regplmn, this.mCurRegplmn)) && isRealChange) {
            return true;
        }
        return false;
    }

    private void getOperator() {
        if (true == HwModemCapability.isCapabilitySupport(9) && HwTelephonyFactory.getHwNetworkManager().getGsmCombinedRegState(this, this.mPhone, this.mSS) == 0 && TextUtils.isEmpty(this.mSS.getOperatorNumeric()) && !this.mRplmnIsNull) {
            this.mCi.getOperator(obtainMessage(6, this.mPollingContext));
            this.mRplmnIsNull = true;
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0028, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void upatePlmn(String brandOverride, String opNames0, String opNames1, String rplmn) {
        if (this.mRplmnIsNull && !TextUtils.isEmpty(rplmn) && HwModemCapability.isCapabilitySupport(9)) {
            if (brandOverride != null) {
                this.mSS.setOperatorName(brandOverride, brandOverride, rplmn);
            } else {
                this.mSS.setOperatorName(opNames0, opNames1, rplmn);
            }
            this.mRplmnIsNull = false;
            updateSpnDisplay();
        }
    }

    protected int getCombinedRegState() {
        int regState = this.mSS.getVoiceRegState();
        int dataRegState = this.mSS.getDataRegState();
        if ((regState != 1 && regState != 3) || dataRegState != 0) {
            return regState;
        }
        log("getCombinedRegState: return STATE_IN_SERVICE as Data is in service");
        return dataRegState;
    }

    public String getAppName(int pid) {
        String processName = "";
        List<RunningAppProcessInfo> l = ((ActivityManager) this.mPhone.getContext().getSystemService("activity")).getRunningAppProcesses();
        if (l == null) {
            return processName;
        }
        for (RunningAppProcessInfo info : l) {
            try {
                if (info.pid == pid) {
                    processName = info.processName;
                    break;
                }
            } catch (RuntimeException e) {
                log("RuntimeException");
            } catch (Exception e2) {
                log("Get The appName is wrong");
            }
        }
        return processName;
    }

    private PersistableBundle getCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (configManager != null) {
            PersistableBundle config = configManager.getConfigForSubId(this.mPhone.getSubId());
            if (config != null) {
                return config;
            }
        }
        return CarrierConfigManager.getDefaultConfig();
    }

    public LocaleTracker getLocaleTracker() {
        return this.mLocaleTracker;
    }

    protected NitzStateMachine getNitzState() {
        return this.mNitzState;
    }

    protected SparseArray<NetworkRegistrationManager> getRegStateManagers() {
        return this.mRegStateManagers;
    }

    public CellLocation getCellLocationInfo() {
        return this.mCellLoc;
    }

    private void processCtVolteCellLocationInfo(CellLocation cellLoc, CellIdentity cellIdentity) {
        if (cellIdentity != null && cellIdentity.getType() == 3) {
            int cid = ((CellIdentityLte) cellIdentity).getCi();
            int lac = ((CellIdentityLte) cellIdentity).getTac();
            if (this.mPhone.isPhoneTypeGsm()) {
                ((GsmCellLocation) cellLoc).setLacAndCid(lac, cid);
                this.hasUpdateCellLocByPS = true;
                return;
            }
            ((CdmaCellLocation) cellLoc).setLacAndCid(lac, cid);
        }
    }
}
