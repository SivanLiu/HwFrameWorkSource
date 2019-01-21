package com.android.internal.telephony.dataconnection;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkFactory;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.provider.Telephony.Carriers;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PcoData;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.data.DataProfile;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.AbstractPhoneInternalInterface;
import com.android.internal.telephony.DctConstants.Activity;
import com.android.internal.telephony.DctConstants.State;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HbpcdLookup;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwDataConnectionManager;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.ITelephony.Stub;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.dataconnection.DataConnectionReasons.DataDisallowedReasonType;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.VSimUtilsInner;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import com.google.android.mms.pdu.CharacterSets;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DcTracker extends AbstractDcTrackerBase {
    protected static final int ACTIVE_PDP_FAIL_TO_RESTART_RILD_COUNT = 3;
    protected static final long ACTIVE_PDP_FAIL_TO_RESTART_RILD_MILLIS = 600000;
    static final String APN_ID = "apn_id";
    public static final String APN_TYPE_VOWIFIMMS = "vowifi_mms";
    private static final int CAUSE_BY_DATA = 0;
    private static final int CAUSE_BY_ROAM = 1;
    private static final int CDMA_NOT_ROAMING = 0;
    private static final int CDMA_ROAMING = 1;
    private static final String CT_LTE_APN_PREFIX = SystemProperties.get("ro.config.ct_lte_apn", "ctnet");
    private static final String CT_NOT_ROAMING_APN_PREFIX = SystemProperties.get("ro.config.ct_not_roaming_apn", "ctnet");
    private static final String CT_ROAMING_APN_PREFIX = SystemProperties.get("ro.config.ct_roaming_apn", "ctnet");
    public static final boolean CT_SUPL_FEATURE_ENABLE = SystemProperties.getBoolean("ro.hwpp.ct_supl_feature_enable", false);
    public static final boolean CUST_RETRY_CONFIG = SystemProperties.getBoolean("ro.config.cust_retry_config", false);
    private static final int DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 60000;
    private static int DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT = POLL_NETSTAT_SCREEN_OFF_MILLIS;
    private static int DATA_STALL_ALARM_PUNISH_DELAY_IN_MS_DEFAULT = 1800000;
    private static final String DATA_STALL_ALARM_TAG_EXTRA = "data.stall.alram.tag";
    private static final boolean DATA_STALL_NOT_SUSPECTED = false;
    private static final boolean DATA_STALL_SUSPECTED = true;
    private static final boolean DBG = true;
    private static final String DEBUG_PROV_APN_ALARM = "persist.debug.prov_apn_alarm";
    private static final boolean ENABLE_WIFI_LTE_CE = SystemProperties.getBoolean("ro.config.enable_wl_coexist", false);
    private static final boolean ESM_FLAG_ADAPTION_ENABLED = SystemProperties.getBoolean("ro.config.attach_apn_enabled", false);
    private static final int ESM_FLAG_INVALID = -1;
    private static final String GC_ICCID = "8985231";
    private static final String GC_MCCMNC = "45431";
    private static final String GC_SPN = "CTExcel";
    private static final int GSM_ROAMING_CARD1 = 2;
    private static final int GSM_ROAMING_CARD2 = 3;
    private static final int HW_SWITCH_SLOT_DONE = 1;
    private static final String HW_SWITCH_SLOT_STEP = "HW_SWITCH_SLOT_STEP";
    private static final String INTENT_DATA_STALL_ALARM = "com.android.internal.telephony.data-stall";
    protected static final String INTENT_PDP_RESET_ALARM = "com.android.internal.telephony.pdp-reset";
    private static final String INTENT_PROVISIONING_APN_ALARM = "com.android.internal.telephony.provisioning_apn_alarm";
    private static final String INTENT_RECONNECT_ALARM = "com.android.internal.telephony.data-reconnect";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_REASON = "reconnect_alarm_extra_reason";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_TYPE = "reconnect_alarm_extra_type";
    private static final int INVALID_STEP = -99;
    public static final boolean IS_DELAY_ATTACH_ENABLED = SystemProperties.getBoolean("ro.config.delay_attach_enabled", false);
    private static String LOG_TAG = "DCT";
    private static final int LTE_NOT_ROAMING = 4;
    static final Uri MSIM_TELEPHONY_CARRIERS_URI = Uri.parse("content://telephony/carriers/subId");
    private static final int NUMBER_SENT_PACKETS_OF_HANG = 10;
    protected static final int NVCFG_RESULT_FINISHED = 1;
    protected static final int PDP_RESET_ALARM_DELAY_IN_MS = 300000;
    protected static final String PDP_RESET_ALARM_TAG_EXTRA = "pdp.reset.alram.tag";
    private static final boolean PERMANENT_ERROR_HEAL_ENABLED = SystemProperties.getBoolean("ro.config.permanent_error_heal", false);
    private static final int POLL_NETSTAT_MILLIS = 1000;
    private static final int POLL_NETSTAT_SCREEN_OFF_MILLIS = 600000;
    private static final int POLL_PDP_MILLIS = 5000;
    static final Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID = Uri.parse("content://telephony/carriers/preferapn_no_update/subId/");
    private static final String PREFERRED_APN_ID = "preferredApnIdEx";
    private static final int PREF_APN_ID_LEN = 5;
    private static final int PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT = 900000;
    private static final String PROVISIONING_APN_ALARM_TAG_EXTRA = "provisioning.apn.alarm.tag";
    private static final int PROVISIONING_SPINNER_TIMEOUT_MILLIS = 120000;
    private static final String PUPPET_MASTER_RADIO_STRESS_TEST = "gsm.defaultpdpcontext.active";
    private static final boolean RADIO_TESTS = false;
    private static final int RECONNECT_ALARM_DELAY_TIME_FOR_CS_ATTACHED = 5000;
    private static final int RECONNECT_ALARM_DELAY_TIME_SHORT = 50;
    private static int RESTART_RADIO_PUNISH_TIME_IN_MS = 43200000;
    private static final int SUB_1 = 1;
    protected static final boolean USER_FORCE_DATA_SETUP = SystemProperties.getBoolean("ro.hwpp.allow_data_onlycs", false);
    private static final boolean VDBG = true;
    private static final boolean VDBG_STALL = false;
    public static final int VP_END = 0;
    public static final int VP_START = 1;
    private static final String WAP_APN = "3gwap";
    protected static final HashMap<String, Integer> mIfacePhoneHashMap = new HashMap();
    protected static final boolean mWcdmaVpEnabled = SystemProperties.get("ro.hwpp.wcdma_voice_preference", "false").equals("true");
    private static int sEnableFailFastRefCounter = 0;
    public AtomicBoolean isCleanupRequired;
    protected boolean isMultiSimEnabled;
    private long lastRadioResetTimestamp;
    private Activity mActivity;
    private final AlarmManager mAlarmManager;
    private ArrayList<ApnSetting> mAllApnSettings;
    private RegistrantList mAllDataDisconnectedRegistrants;
    public final ConcurrentHashMap<String, ApnContext> mApnContexts;
    private final SparseArray<ApnContext> mApnContextsById;
    private ApnChangeObserver mApnObserver;
    private HashMap<String, Integer> mApnToDataConnectionId;
    private AtomicBoolean mAttached;
    private AtomicBoolean mAutoAttachOnCreation;
    private boolean mAutoAttachOnCreationConfig;
    private boolean mCanSetPreferApn;
    private boolean mCdmaPsRecoveryEnabled;
    private final ConnectivityManager mCm;
    private int mCurrentState;
    private HashMap<Integer, DcAsyncChannel> mDataConnectionAcHashMap;
    private final Handler mDataConnectionTracker;
    private HashMap<Integer, DataConnection> mDataConnections;
    private final DataEnabledSettings mDataEnabledSettings;
    private final LocalLog mDataRoamingLeakageLog;
    private final DataServiceManager mDataServiceManager;
    private PendingIntent mDataStallAlarmIntent;
    private int mDataStallAlarmTag;
    private volatile boolean mDataStallDetectionEnabled;
    private TxRxSum mDataStallTxRxSum;
    private DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    private DcController mDcc;
    private String mDefaultApnId;
    private ArrayList<Message> mDisconnectAllCompleteMsgList;
    private int mDisconnectPendingCount;
    private ApnSetting mEmergencyApn;
    private boolean mEmergencyApnLoaded;
    private volatile boolean mFailFast;
    protected long mFirstPdpActFailTimestamp;
    protected HwCustDcTracker mHwCustDcTracker;
    private final AtomicReference<IccRecords> mIccRecords;
    private boolean mInVoiceCall;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mIsDisposed;
    private boolean mIsProvisioning;
    private boolean mIsPsRestricted;
    private boolean mIsScreenOn;
    private boolean mMvnoMatched;
    private boolean mNetStatPollEnabled;
    private int mNetStatPollPeriod;
    private int mNoRecvPollCount;
    private final OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;
    protected int mPdpActFailCount;
    protected PendingIntent mPdpResetAlarmIntent;
    protected int mPdpResetAlarmTag;
    protected final Phone mPhone;
    PhoneStateListener mPhoneStateListener;
    private final Runnable mPollNetStat;
    private ApnSetting mPreferredApn;
    protected final ArrayList<ApnContext> mPrioritySortedApnContexts;
    private final String mProvisionActionName;
    private BroadcastReceiver mProvisionBroadcastReceiver;
    private PendingIntent mProvisioningApnAlarmIntent;
    private int mProvisioningApnAlarmTag;
    private ProgressDialog mProvisioningSpinner;
    private String mProvisioningUrl;
    private PendingIntent mReconnectIntent;
    private AsyncChannel mReplyAc;
    private String mRequestedApnType;
    private boolean mReregisterOnReconnectFailure;
    private ContentResolver mResolver;
    protected boolean mRestartRildEnabled;
    private long mRxPkts;
    private long mSentSinceLastRecv;
    private final SettingsObserver mSettingsObserver;
    private State mState;
    private SubscriptionManager mSubscriptionManager;
    private final int mTransportType;
    private long mTxPkts;
    protected UiccCardApplication mUiccApplcation;
    private final UiccController mUiccController;
    private AtomicInteger mUniqueIdGenerator;
    public int mVpStatus;
    private int oldCallState;
    private int preDataRadioTech;
    private int preSetupBasedRadioTech;

    /* renamed from: com.android.internal.telephony.dataconnection.DcTracker$7 */
    static /* synthetic */ class AnonymousClass7 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$DctConstants$State = new int[State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[State.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[State.DISCONNECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[State.RETRYING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[State.CONNECTING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[State.IDLE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[State.SCANNING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[State.FAILED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver() {
            super(DcTracker.this.mDataConnectionTracker);
        }

        public void onChange(boolean selfChange) {
            DcTracker.this.sendMessage(DcTracker.this.obtainMessage(270355));
        }
    }

    private class ProvisionNotificationBroadcastReceiver extends BroadcastReceiver {
        private final String mNetworkOperator;
        private final String mProvisionUrl;

        public ProvisionNotificationBroadcastReceiver(String provisionUrl, String networkOperator) {
            this.mNetworkOperator = networkOperator;
            this.mProvisionUrl = provisionUrl;
        }

        private void setEnableFailFastMobileData(int enabled) {
            DcTracker.this.sendMessage(DcTracker.this.obtainMessage(270372, enabled, 0));
        }

        private void enableMobileProvisioning() {
            Message msg = DcTracker.this.obtainMessage(270373);
            msg.setData(Bundle.forPair("provisioningUrl", this.mProvisionUrl));
            DcTracker.this.sendMessage(msg);
        }

        public void onReceive(Context context, Intent intent) {
            DcTracker.this.log("onReceive : ProvisionNotificationBroadcastReceiver");
            DcTracker.this.mProvisioningSpinner = new ProgressDialog(context);
            DcTracker.this.mProvisioningSpinner.setTitle(this.mNetworkOperator);
            DcTracker.this.mProvisioningSpinner.setMessage(context.getText(17040418));
            DcTracker.this.mProvisioningSpinner.setIndeterminate(true);
            DcTracker.this.mProvisioningSpinner.setCancelable(true);
            DcTracker.this.mProvisioningSpinner.getWindow().setType(2009);
            DcTracker.this.mProvisioningSpinner.show();
            DcTracker.this.sendMessageDelayed(DcTracker.this.obtainMessage(270378, DcTracker.this.mProvisioningSpinner), 120000);
            DcTracker.this.setRadio(true);
            setEnableFailFastMobileData(1);
            enableMobileProvisioning();
        }
    }

    private static class RecoveryAction {
        public static final int CLEANUP = 1;
        public static final int GET_DATA_CALL_LIST = 0;
        public static final int RADIO_RESTART = 3;
        public static final int REREGISTER = 2;

        private RecoveryAction() {
        }

        private static boolean isAggressiveRecovery(int value) {
            return value == 1 || value == 2 || value == 3;
        }
    }

    private enum RetryFailures {
        ALWAYS,
        ONLY_ON_CHANGE
    }

    public static class TxRxSum {
        public long rxPkts;
        public long txPkts;

        public TxRxSum() {
            reset();
        }

        public TxRxSum(long txPkts, long rxPkts) {
            this.txPkts = txPkts;
            this.rxPkts = rxPkts;
        }

        public TxRxSum(TxRxSum sum) {
            this.txPkts = sum.txPkts;
            this.rxPkts = sum.rxPkts;
        }

        public void reset() {
            this.txPkts = -1;
            this.rxPkts = -1;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{txSum=");
            stringBuilder.append(this.txPkts);
            stringBuilder.append(" rxSum=");
            stringBuilder.append(this.rxPkts);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public void updateTxRxSum() {
            this.txPkts = TrafficStats.getMobileTcpTxPackets();
            this.rxPkts = TrafficStats.getMobileTcpRxPackets();
        }

        public void updateThisModemMobileTxRxSum(HashMap<String, Integer> ifacePhoneHashMap, int phoneId) {
            this.txPkts = HwTelephonyFactory.getHwDataConnectionManager().getThisModemMobileTxPackets(ifacePhoneHashMap, phoneId);
            this.rxPkts = HwTelephonyFactory.getHwDataConnectionManager().getThisModemMobileRxPackets(ifacePhoneHashMap, phoneId);
        }
    }

    private void registerSettingsObserver() {
        Uri contentUri;
        this.mSettingsObserver.unobserve();
        if (TelephonyManager.getDefault().getSimCount() == 1) {
            contentUri = Global.getUriFor("data_roaming");
        } else {
            contentUri = Global.getUriFor(getDataRoamingSettingItem("data_roaming"));
        }
        this.mSettingsObserver.observe(contentUri, 270384);
        this.mSettingsObserver.observe(Global.getUriFor("device_provisioned"), 270379);
        this.mSettingsObserver.observe(Global.getUriFor("device_provisioning_mobile_data"), 270379);
    }

    private void onActionIntentReconnectAlarm(Intent intent) {
        Message msg = obtainMessage(270383);
        msg.setData(intent.getExtras());
        sendMessage(msg);
    }

    private void onDataReconnect(Bundle bundle) {
        String reason = bundle.getString(INTENT_RECONNECT_ALARM_EXTRA_REASON);
        String apnType = bundle.getString(INTENT_RECONNECT_ALARM_EXTRA_TYPE);
        int phoneSubId = this.mPhone.getSubId();
        int currSubId = bundle.getInt("subscription", -1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onDataReconnect: currSubId = ");
        stringBuilder.append(currSubId);
        stringBuilder.append(" phoneSubId=");
        stringBuilder.append(phoneSubId);
        log(stringBuilder.toString());
        if (SubscriptionManager.isValidSubscriptionId(currSubId) && currSubId == phoneSubId) {
            ApnContext apnContext = (ApnContext) this.mApnContexts.get(apnType);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onDataReconnect: mState=");
            stringBuilder2.append(this.mState);
            stringBuilder2.append(" reason=");
            stringBuilder2.append(reason);
            stringBuilder2.append(" apnType=");
            stringBuilder2.append(apnType);
            stringBuilder2.append(" apnContext=");
            stringBuilder2.append(apnContext);
            stringBuilder2.append(" mDataConnectionAsyncChannels=");
            stringBuilder2.append(this.mDataConnectionAcHashMap);
            log(stringBuilder2.toString());
            if (apnContext != null && apnContext.isEnabled()) {
                apnContext.setReason(reason);
                State apnContextState = apnContext.getState();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onDataReconnect: apnContext state=");
                stringBuilder3.append(apnContextState);
                log(stringBuilder3.toString());
                if (apnContextState == State.FAILED || apnContextState == State.IDLE) {
                    log("onDataReconnect: state is FAILED|IDLE, disassociate");
                    DcAsyncChannel dcac = apnContext.getDcAc();
                    if (dcac != null) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("onDataReconnect: tearDown apnContext=");
                        stringBuilder4.append(apnContext);
                        log(stringBuilder4.toString());
                        dcac.tearDown(apnContext, "", null);
                    }
                    apnContext.setDataConnectionAc(null);
                    apnContext.setState(State.IDLE);
                } else {
                    log("onDataReconnect: keep associated");
                }
                sendMessage(obtainMessage(270339, apnContext));
                apnContext.setReconnectIntent(null);
            }
            return;
        }
        log("receive ReconnectAlarm but subId incorrect, ignore");
    }

    private void onActionIntentDataStallAlarm(Intent intent) {
        Message msg = obtainMessage(270353, intent.getAction());
        msg.arg1 = intent.getIntExtra(DATA_STALL_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    public DcTracker(Phone phone, int transportType) {
        StringBuilder stringBuilder;
        this.isCleanupRequired = new AtomicBoolean(false);
        this.oldCallState = 0;
        this.mRequestedApnType = "default";
        this.lastRadioResetTimestamp = 0;
        this.mPrioritySortedApnContexts = new ArrayList();
        this.mAllApnSettings = new ArrayList();
        this.mPreferredApn = null;
        this.mIsPsRestricted = false;
        this.mEmergencyApn = null;
        this.mIsDisposed = false;
        this.mIsProvisioning = false;
        this.mProvisioningUrl = null;
        this.mProvisioningApnAlarmIntent = null;
        this.mProvisioningApnAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mReplyAc = new AsyncChannel();
        this.mDataRoamingLeakageLog = new LocalLog(50);
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                DcTracker dcTracker;
                StringBuilder stringBuilder;
                String operator;
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    DcTracker.this.log("screen on");
                    DcTracker.this.mIsScreenOn = true;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                    DcTracker.this.log("screen off");
                    DcTracker.this.mIsScreenOn = false;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                } else if (action.startsWith(DcTracker.INTENT_RECONNECT_ALARM)) {
                    dcTracker = DcTracker.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Reconnect alarm. Previous state was ");
                    stringBuilder.append(DcTracker.this.mState);
                    dcTracker.log(stringBuilder.toString());
                    DcTracker.this.onActionIntentReconnectAlarm(intent);
                } else if (action.equals(DcTracker.INTENT_DATA_STALL_ALARM)) {
                    DcTracker.this.log("Data stall alarm");
                    DcTracker.this.onActionIntentDataStallAlarm(intent);
                } else if (action.equals(DcTracker.INTENT_PROVISIONING_APN_ALARM)) {
                    DcTracker.this.log("Provisioning apn alarm");
                    DcTracker.this.onActionIntentProvisioningApnAlarm(intent);
                } else if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    if (DcTracker.this.mIccRecords.get() != null && ((IccRecords) DcTracker.this.mIccRecords.get()).getRecordsLoaded() && !((IccRecords) DcTracker.this.mIccRecords.get()).isHwCustDataRoamingOpenArea()) {
                        DcTracker.this.setDefaultDataRoamingEnabled();
                    }
                } else if (action.equals(DcTracker.INTENT_PDP_RESET_ALARM)) {
                    DcTracker.this.log("Pdp reset alarm");
                    DcTracker.this.onActionIntentPdpResetAlarm(intent);
                } else if ("com.huawei.action.ACTION_HW_SWITCH_SLOT_DONE".equals(action)) {
                    DcTracker.this.log("Received SWITCH_SLOT_DONE");
                    operator = DcTracker.this.getOperatorNumeric();
                    int switchSlotStep = intent.getIntExtra(DcTracker.HW_SWITCH_SLOT_STEP, -99);
                    if (!TextUtils.isEmpty(operator) && 1 == switchSlotStep) {
                        DcTracker.this.onRecordsLoadedOrSubIdChanged();
                    }
                } else if (action.equals(AbstractPhoneInternalInterface.OTA_OPEN_CARD_ACTION)) {
                    DcTracker.this.log("onUserSelectOpenService ");
                    DcTracker.this.onUserSelectOpenService();
                } else if ("com.huawei.devicepolicy.action.POLICY_CHANGED".equals(intent.getAction())) {
                    DcTracker.this.log("com.huawei.devicepolicy.action.POLICY_CHANGED");
                    operator = intent.getStringExtra("action_tag");
                    if (!TextUtils.isEmpty(operator) && operator.equals("action_disable_data_4G") && DcTracker.this.mPhone != null && DcTracker.this.mPhone.getSubId() == 1) {
                        if (intent.getBooleanExtra("dataState", false)) {
                            DcTracker.this.cleanUpAllConnections(PhoneInternalInterface.REASON_DATA_DISABLED);
                        } else {
                            DcTracker.this.onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
                        }
                    }
                } else {
                    dcTracker = DcTracker.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onReceive: Unknown action=");
                    stringBuilder.append(action);
                    dcTracker.log(stringBuilder.toString());
                }
            }
        };
        this.mPollNetStat = new Runnable() {
            public void run() {
                DcTracker.this.updateDataActivity();
                if (DcTracker.this.mIsScreenOn) {
                    DcTracker.this.mNetStatPollPeriod = Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_poll_interval_ms", 1000);
                } else {
                    DcTracker.this.mNetStatPollPeriod = Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_long_poll_interval_ms", DcTracker.POLL_NETSTAT_SCREEN_OFF_MILLIS);
                }
                if (DcTracker.this.mNetStatPollEnabled) {
                    DcTracker.this.mDataConnectionTracker.postDelayed(this, (long) DcTracker.this.mNetStatPollPeriod);
                }
            }
        };
        this.mOnSubscriptionsChangedListener = new OnSubscriptionsChangedListener() {
            public void onSubscriptionsChanged() {
                DcTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged");
                if (SubscriptionManager.isValidSubscriptionId(DcTracker.this.mPhone.getSubId())) {
                    DcTracker.this.registerSettingsObserver();
                }
            }
        };
        this.mDisconnectAllCompleteMsgList = new ArrayList();
        this.mAllDataDisconnectedRegistrants = new RegistrantList();
        this.mIccRecords = new AtomicReference();
        this.mActivity = Activity.NONE;
        this.mState = State.IDLE;
        this.mNetStatPollEnabled = false;
        this.mDataStallTxRxSum = new TxRxSum(0, 0);
        this.mDataStallAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mDataStallAlarmIntent = null;
        this.mNoRecvPollCount = 0;
        this.mDataStallDetectionEnabled = true;
        this.mFailFast = false;
        this.mInVoiceCall = false;
        this.mReconnectIntent = null;
        this.mAutoAttachOnCreationConfig = true;
        this.mAutoAttachOnCreation = new AtomicBoolean(false);
        this.mIsScreenOn = true;
        this.mMvnoMatched = false;
        this.mUniqueIdGenerator = new AtomicInteger(0);
        this.mDataConnections = new HashMap();
        this.mDataConnectionAcHashMap = new HashMap();
        this.mApnToDataConnectionId = new HashMap();
        this.mApnContexts = new ConcurrentHashMap();
        this.mApnContextsById = new SparseArray();
        this.mDisconnectPendingCount = 0;
        this.mReregisterOnReconnectFailure = false;
        this.mCanSetPreferApn = false;
        this.mAttached = new AtomicBoolean(false);
        this.mCdmaPsRecoveryEnabled = false;
        this.mDefaultApnId = "0,0,0,0,0";
        this.mCurrentState = -1;
        this.preDataRadioTech = -1;
        this.preSetupBasedRadioTech = -1;
        this.mVpStatus = 0;
        this.isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
        this.mPdpActFailCount = 0;
        this.mFirstPdpActFailTimestamp = 0;
        this.mRestartRildEnabled = true;
        this.mPdpResetAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mPdpResetAlarmIntent = null;
        this.mUiccApplcation = null;
        this.mPhoneStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if (DcTracker.this.oldCallState == 2 && state == 0 && DcTracker.this.mPhone.getSubId() == SubscriptionController.getInstance().getDefaultDataSubId()) {
                    DcTracker.this.onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
                    if (DcTracker.this.mPrioritySortedApnContexts != null) {
                        int list_size = DcTracker.this.mPrioritySortedApnContexts.size();
                        for (int i = 0; i < list_size; i++) {
                            ApnContext apnContext = (ApnContext) DcTracker.this.mPrioritySortedApnContexts.get(i);
                            if (apnContext.getApnType().equals("default")) {
                                DcTracker.this.log("resetRetryCount");
                                apnContext.resetRetryCount();
                            }
                        }
                    }
                }
                DcTracker.this.oldCallState = state;
            }
        };
        this.mEmergencyApnLoaded = false;
        this.mPhone = phone;
        if (this.mHwCustDcTracker == null) {
            this.mHwCustDcTracker = (HwCustDcTracker) HwCustUtils.createObj(HwCustDcTracker.class, new Object[]{this});
        }
        if (phone.getPhoneType() == 1) {
            LOG_TAG = "GsmDCT";
        } else if (phone.getPhoneType() == 2) {
            LOG_TAG = "CdmaDCT";
        } else {
            LOG_TAG = "DCT";
            stringBuilder = new StringBuilder();
            stringBuilder.append("unexpected phone type [");
            stringBuilder.append(phone.getPhoneType());
            stringBuilder.append("]");
            loge(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(LOG_TAG);
        stringBuilder.append(".constructor");
        log(stringBuilder.toString());
        this.mTransportType = transportType;
        this.mDataServiceManager = new DataServiceManager(phone, transportType);
        if (phone.getPhoneType() == 2 && SystemProperties.getBoolean("hw.dct.psrecovery", false)) {
            this.mCdmaPsRecoveryEnabled = true;
        } else {
            this.mCdmaPsRecoveryEnabled = false;
        }
        this.mResolver = this.mPhone.getContext().getContentResolver();
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 270369, null);
        if (VSimUtilsInner.isVSimSub(this.mPhone.getSubId())) {
            VSimUtilsInner.registerForIccChanged(this, 270369, null);
        }
        this.mAlarmManager = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        this.mCm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.huawei.devicepolicy.action.POLICY_CHANGED");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction(INTENT_DATA_STALL_ALARM);
        filter.addAction(INTENT_PROVISIONING_APN_ALARM);
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        filter.addAction(INTENT_PDP_RESET_ALARM);
        filter.addAction("com.huawei.action.ACTION_HW_SWITCH_SLOT_DONE");
        if (SystemProperties.getBoolean("ro.config.hw_enable_ota_bip_lgu", false)) {
            filter.addAction(AbstractPhoneInternalInterface.OTA_OPEN_CARD_ACTION);
        }
        this.mDataEnabledSettings = new DataEnabledSettings(phone);
        this.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mPhone);
        this.mAutoAttachOnCreation.set(PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext()).getBoolean(Phone.DATA_DISABLED_ON_BOOT_KEY, false));
        registerPhoneStateListener(this.mPhone.getContext());
        this.mSubscriptionManager = SubscriptionManager.from(this.mPhone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        HandlerThread dcHandlerThread = new HandlerThread("DcHandlerThread");
        dcHandlerThread.start();
        Handler dcHandler = new Handler(dcHandlerThread.getLooper());
        this.mDcc = DcController.makeDcc(this.mPhone, this, this.mDataServiceManager, dcHandler);
        this.mDcTesterFailBringUpAll = new DcTesterFailBringUpAll(this.mPhone, dcHandler);
        this.mDataConnectionTracker = this;
        registerForAllEvents();
        update();
        this.mApnObserver = new ApnChangeObserver();
        phone.getContext().getContentResolver().registerContentObserver(Carriers.CONTENT_URI, true, this.mApnObserver);
        initApnContexts();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            filter = new IntentFilter();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("com.android.internal.telephony.data-reconnect.");
            stringBuilder2.append(apnContext.getApnType());
            filter.addAction(stringBuilder2.toString());
            this.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mPhone);
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("com.android.internal.telephony.PROVISION");
        stringBuilder3.append(phone.getPhoneId());
        this.mProvisionActionName = stringBuilder3.toString();
        this.mSettingsObserver = new SettingsObserver(this.mPhone.getContext(), this);
        registerSettingsObserver();
        super.init();
        if (isClearCodeEnabled()) {
            startListenCellLocationChange();
        }
        registerForFdn();
        sendMessage(obtainMessage(271137));
    }

    @VisibleForTesting
    public DcTracker() {
        this.isCleanupRequired = new AtomicBoolean(false);
        this.oldCallState = 0;
        this.mRequestedApnType = "default";
        this.lastRadioResetTimestamp = 0;
        this.mPrioritySortedApnContexts = new ArrayList();
        this.mAllApnSettings = new ArrayList();
        this.mPreferredApn = null;
        this.mIsPsRestricted = false;
        this.mEmergencyApn = null;
        this.mIsDisposed = false;
        this.mIsProvisioning = false;
        this.mProvisioningUrl = null;
        this.mProvisioningApnAlarmIntent = null;
        this.mProvisioningApnAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mReplyAc = new AsyncChannel();
        this.mDataRoamingLeakageLog = new LocalLog(50);
        this.mIntentReceiver = /* anonymous class already generated */;
        this.mPollNetStat = /* anonymous class already generated */;
        this.mOnSubscriptionsChangedListener = /* anonymous class already generated */;
        this.mDisconnectAllCompleteMsgList = new ArrayList();
        this.mAllDataDisconnectedRegistrants = new RegistrantList();
        this.mIccRecords = new AtomicReference();
        this.mActivity = Activity.NONE;
        this.mState = State.IDLE;
        this.mNetStatPollEnabled = false;
        this.mDataStallTxRxSum = new TxRxSum(0, 0);
        this.mDataStallAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mDataStallAlarmIntent = null;
        this.mNoRecvPollCount = 0;
        this.mDataStallDetectionEnabled = true;
        this.mFailFast = false;
        this.mInVoiceCall = false;
        this.mReconnectIntent = null;
        this.mAutoAttachOnCreationConfig = true;
        this.mAutoAttachOnCreation = new AtomicBoolean(false);
        this.mIsScreenOn = true;
        this.mMvnoMatched = false;
        this.mUniqueIdGenerator = new AtomicInteger(0);
        this.mDataConnections = new HashMap();
        this.mDataConnectionAcHashMap = new HashMap();
        this.mApnToDataConnectionId = new HashMap();
        this.mApnContexts = new ConcurrentHashMap();
        this.mApnContextsById = new SparseArray();
        this.mDisconnectPendingCount = 0;
        this.mReregisterOnReconnectFailure = false;
        this.mCanSetPreferApn = false;
        this.mAttached = new AtomicBoolean(false);
        this.mCdmaPsRecoveryEnabled = false;
        this.mDefaultApnId = "0,0,0,0,0";
        this.mCurrentState = -1;
        this.preDataRadioTech = -1;
        this.preSetupBasedRadioTech = -1;
        this.mVpStatus = 0;
        this.isMultiSimEnabled = TelephonyManager.getDefault().isMultiSimEnabled();
        this.mPdpActFailCount = 0;
        this.mFirstPdpActFailTimestamp = 0;
        this.mRestartRildEnabled = true;
        this.mPdpResetAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mPdpResetAlarmIntent = null;
        this.mUiccApplcation = null;
        this.mPhoneStateListener = /* anonymous class already generated */;
        this.mEmergencyApnLoaded = false;
        this.mAlarmManager = null;
        this.mCm = null;
        this.mPhone = null;
        this.mUiccController = null;
        this.mDataConnectionTracker = null;
        this.mProvisionActionName = null;
        this.mSettingsObserver = new SettingsObserver(null, this);
        this.mDataEnabledSettings = null;
        this.mTransportType = 0;
        this.mDataServiceManager = null;
    }

    public void registerServiceStateTrackerEvents() {
        this.mPhone.getServiceStateTracker().registerForDataConnectionAttached(this, 270352, null);
        this.mPhone.getServiceStateTracker().registerForDataConnectionDetached(this, 270345, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOn(this, 270347, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOff(this, 270348, null, true);
        this.mPhone.getServiceStateTracker().registerForPsRestrictedEnabled(this, 270358, null);
        this.mPhone.getServiceStateTracker().registerForPsRestrictedDisabled(this, 270359, null);
        log("registerForDataRegStateOrRatChanged");
        this.mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(this, 270377, null);
        if (mWcdmaVpEnabled) {
            this.mPhone.mCi.registerForReportVpStatus(this, 271140, null);
        }
    }

    public void unregisterServiceStateTrackerEvents() {
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionAttached(this);
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionDetached(this);
        this.mPhone.getServiceStateTracker().unregisterForDataRoamingOn(this);
        this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
        this.mPhone.getServiceStateTracker().unregisterForPsRestrictedEnabled(this);
        this.mPhone.getServiceStateTracker().unregisterForPsRestrictedDisabled(this);
        log("unregisterForDataRegStateOrRatChanged");
        this.mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(this);
    }

    private void registerForAllEvents() {
        this.mPhone.mCi.registerForUnsolNvCfgFinished(this, 271145, null);
        if (this.mTransportType == 1) {
            this.mPhone.mCi.registerForAvailable(this, 270337, null);
            this.mPhone.mCi.registerForOffOrNotAvailable(this, 270342, null);
            this.mPhone.mCi.registerForPcoData(this, 270381, null);
        }
        if (this.mPhone.getCallTracker() != null) {
            this.mPhone.getCallTracker().registerForVoiceCallEnded(this, 270344, null);
            this.mPhone.getCallTracker().registerForVoiceCallStarted(this, 270343, null);
        }
        registerServiceStateTrackerEvents();
        this.mPhone.mCi.registerForPcoData(this, 270381, null);
        this.mPhone.getCarrierActionAgent().registerForCarrierAction(0, this, 270382, null, false);
        this.mDataServiceManager.registerForServiceBindingChanged(this, 270385, null);
    }

    public void dispose() {
        log("DCT.dispose");
        if (this.mProvisionBroadcastReceiver != null) {
            this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
            this.mProvisionBroadcastReceiver = null;
        }
        if (this.mProvisioningSpinner != null) {
            this.mProvisioningSpinner.dismiss();
            this.mProvisioningSpinner = null;
        }
        cleanUpAllConnections(true, null);
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            dcac.disconnect();
        }
        this.mDataConnectionAcHashMap.clear();
        this.mIsDisposed = true;
        this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        if (VSimUtilsInner.isVSimSub(this.mPhone.getSubId())) {
            VSimUtilsInner.unregisterForIccChanged(this);
        }
        this.mUiccController.unregisterForIccChanged(this);
        this.mSettingsObserver.unobserve();
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mDcc.dispose();
        this.mDcTesterFailBringUpAll.dispose();
        this.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mApnObserver);
        this.mApnContexts.clear();
        this.mApnContextsById.clear();
        this.mPrioritySortedApnContexts.clear();
        unregisterForAllEvents();
        if (isClearCodeEnabled()) {
            stopListenCellLocationChange();
        }
        unregisterForFdn();
        destroyDataConnections();
        disposeCustDct();
        super.dispose();
    }

    private void unregisterForAllEvents() {
        if (this.mTransportType == 1) {
            this.mPhone.mCi.unregisterForAvailable(this);
            this.mPhone.mCi.unregisterForOffOrNotAvailable(this);
            this.mPhone.mCi.unregisterForPcoData(this);
        }
        if (this.mUiccApplcation != null) {
            unregisterForGetAdDone(this.mUiccApplcation);
            this.mUiccApplcation = null;
        }
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (r != null) {
            unregisterForRecordsLoaded(r);
            unregisterForImsiReady(r);
            unregisterForFdnRecordsLoaded(r);
            this.mIccRecords.set(null);
        }
        if (this.mPhone.getCallTracker() != null) {
            this.mPhone.getCallTracker().unregisterForVoiceCallEnded(this);
            this.mPhone.getCallTracker().unregisterForVoiceCallStarted(this);
        }
        unregisterServiceStateTrackerEvents();
        this.mPhone.mCi.unregisterForPcoData(this);
        this.mPhone.getCarrierActionAgent().unregisterForCarrierAction(this, 0);
        if (mWcdmaVpEnabled) {
            this.mPhone.mCi.unregisterForReportVpStatus(this);
        }
        this.mDataServiceManager.unregisterForServiceBindingChanged(this);
        this.mPhone.mCi.unregisterForUnsolNvCfgFinished(this);
    }

    protected void registerPhoneStateListener(Context context) {
        ((TelephonyManager) context.getSystemService("phone")).listen(this.mPhoneStateListener, 32);
    }

    private String getAppName(int pid) {
        String processName = "";
        List<RunningAppProcessInfo> l = ((ActivityManager) this.mPhone.getContext().getSystemService("activity")).getRunningAppProcesses();
        if (l == null) {
            return processName;
        }
        for (RunningAppProcessInfo info : l) {
            try {
                if (info.pid == pid) {
                    processName = info.processName;
                }
            } catch (RuntimeException e) {
                log("RuntimeException");
            } catch (Exception e2) {
                log("Get The appName is wrong");
            }
        }
        return processName;
    }

    public void setUserDataEnabled(boolean enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DcTrackerBase setDataEnabled=");
        stringBuilder.append(enable);
        log(stringBuilder.toString());
        int pid = Binder.getCallingPid();
        String appName = getAppName(pid);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Get the caller pid and appName. pid is ");
        stringBuilder2.append(pid);
        stringBuilder2.append(", appName is ");
        stringBuilder2.append(appName);
        log(stringBuilder2.toString());
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stackArray = new Exception().getStackTrace();
        for (StackTraceElement element : stackArray) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(element.toString());
            stringBuilder3.append("\n");
            sb.append(stringBuilder3.toString());
        }
        log(sb.toString());
        Message msg = obtainMessage(270366);
        msg.arg1 = enable;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("setDataEnabled: sendMessage: enable=");
        stringBuilder4.append(enable);
        log(stringBuilder4.toString());
        sendMessage(msg);
    }

    private void onSetUserDataEnabled(boolean enabled) {
        if (this.mDataEnabledSettings.isUserDataEnabled() != enabled) {
            this.mDataEnabledSettings.setUserDataEnabled(enabled);
            if (enabled) {
                this.mHwCustDcTracker.setDataOrRoamOn(0);
            }
            if (!getDataRoamingEnabled() && this.mPhone.getServiceState().getDataRoaming()) {
                if (enabled) {
                    notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_ON);
                } else {
                    notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_DATA_DISABLED);
                }
            }
            if (enabled) {
                ApnContext apnContext = (ApnContext) this.mApnContexts.get("default");
                if (!(apnContext == null || apnContext.isEnabled() || !isDataNeededWithWifiAndBt())) {
                    log("onSetUserDataEnabled default apn is disabled and isDataNeededWithWifiAndBt is true, so we need try to restore apncontext");
                    apnContext.setEnabled(true);
                    apnContext.setDependencyMet(true);
                }
            }
            this.mPhone.notifyUserMobileDataStateChanged(enabled);
            if (enabled) {
                reevaluateDataConnections();
                onTrySetupData(AbstractPhoneInternalInterface.REASON_USER_DATA_ENABLED);
                return;
            }
            onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
            clearRestartRildParam();
        }
    }

    private void reevaluateDataConnections() {
        if (this.mDataEnabledSettings.isDataEnabled()) {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (apnContext.isConnectedOrConnecting()) {
                    DcAsyncChannel dcac = apnContext.getDcAc();
                    if (dcac != null) {
                        NetworkCapabilities netCaps = dcac.getNetworkCapabilitiesSync();
                        StringBuilder stringBuilder;
                        if (netCaps != null && !netCaps.hasCapability(13) && !netCaps.hasCapability(11)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Tearing down restricted metered net:");
                            stringBuilder.append(apnContext);
                            log(stringBuilder.toString());
                            apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
                            cleanUpConnection(true, apnContext);
                        } else if (apnContext.getApnSetting().isMetered(this.mPhone) && netCaps != null && netCaps.hasCapability(11)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Tearing down unmetered net:");
                            stringBuilder.append(apnContext);
                            log(stringBuilder.toString());
                            apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
                            cleanUpConnection(true, apnContext);
                        }
                    }
                }
            }
        }
    }

    private void onDeviceProvisionedChange() {
        if (isDataEnabled()) {
            reevaluateDataConnections();
            onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
            return;
        }
        onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
    }

    public long getSubId() {
        return (long) this.mPhone.getSubId();
    }

    public Activity getActivity() {
        return this.mActivity;
    }

    private void setActivity(Activity activity) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setActivity = ");
        stringBuilder.append(activity);
        log(stringBuilder.toString());
        this.mActivity = activity;
        this.mPhone.notifyDataActivity();
    }

    public boolean isDisconnectedOrConnecting() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getState() == State.CONNECTED || apnContext.getState() == State.DISCONNECTING) {
                return false;
            }
        }
        return true;
    }

    public void requestNetwork(NetworkRequest networkRequest, LocalLog log) {
        ApnContext apnContext = (ApnContext) this.mApnContextsById.get(ApnContext.apnIdForNetworkRequest(networkRequest));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DcTracker.requestNetwork for ");
        stringBuilder.append(networkRequest);
        stringBuilder.append(" found ");
        stringBuilder.append(apnContext);
        log.log(stringBuilder.toString());
        if (apnContext != null) {
            apnContext.requestNetwork(networkRequest, log);
        }
    }

    public void releaseNetwork(NetworkRequest networkRequest, LocalLog log) {
        ApnContext apnContext = (ApnContext) this.mApnContextsById.get(ApnContext.apnIdForNetworkRequest(networkRequest));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DcTracker.releaseNetwork for ");
        stringBuilder.append(networkRequest);
        stringBuilder.append(" found ");
        stringBuilder.append(apnContext);
        log.log(stringBuilder.toString());
        if (apnContext != null) {
            apnContext.releaseNetwork(networkRequest, log);
        }
    }

    public void clearDefaultLink() {
        ApnContext apnContext = (ApnContext) this.mApnContextsById.get(0);
        if (apnContext != null) {
            DcAsyncChannel dcac = apnContext.getDcAc();
            if (dcac != null) {
                dcac.clearLink(null, null, null);
            }
        }
    }

    public void resumeDefaultLink() {
        ApnContext apnContext = (ApnContext) this.mApnContextsById.get(0);
        if (apnContext != null) {
            DcAsyncChannel dcac = apnContext.getDcAc();
            if (dcac != null) {
                dcac.resumeLink(null, null, null);
            }
        }
    }

    public boolean isApnSupported(String name) {
        if (name == null) {
            loge("isApnSupported: name=null");
            return false;
        } else if (((ApnContext) this.mApnContexts.get(name)) != null) {
            return true;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Request for unsupported mobile name: ");
            stringBuilder.append(name);
            loge(stringBuilder.toString());
            return false;
        }
    }

    public int getApnPriority(String name) {
        ApnContext apnContext = (ApnContext) this.mApnContexts.get(name);
        if (apnContext == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Request for unsupported mobile name: ");
            stringBuilder.append(name);
            loge(stringBuilder.toString());
        }
        return apnContext.priority;
    }

    private void setRadio(boolean on) {
        try {
            Stub.asInterface(ServiceManager.checkService("phone")).setRadio(on);
        } catch (Exception e) {
        }
    }

    protected void finalize() {
        if (this.mPhone != null) {
            log("finalize");
        }
    }

    private ApnContext addApnContext(String type, NetworkConfig networkConfig) {
        ApnContext apnContext = new ApnContext(this.mPhone, type, LOG_TAG, networkConfig, this);
        this.mApnContexts.put(type, apnContext);
        this.mApnContextsById.put(ApnContext.apnIdForApnName(type), apnContext);
        this.mPrioritySortedApnContexts.add(0, apnContext);
        return apnContext;
    }

    private void initApnContexts() {
        log("initApnContexts: E");
        for (String networkConfigString : this.mPhone.getContext().getResources().getStringArray(17236063)) {
            NetworkConfig networkConfig = new NetworkConfig(networkConfigString);
            if (!VSimUtilsInner.isVSimFiltrateApn(this.mPhone.getSubId(), networkConfig.type)) {
                String apnType = networkTypeToApnType(networkConfig.type);
                StringBuilder stringBuilder;
                if (isApnTypeDisabled(apnType)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("apn type ");
                    stringBuilder.append(apnType);
                    stringBuilder.append(" disabled!");
                    log(stringBuilder.toString());
                } else {
                    ApnContext apnContext;
                    int i = networkConfig.type;
                    if (i == 0) {
                        apnContext = addApnContext("default", networkConfig);
                    } else if (i != 48) {
                        switch (i) {
                            case 2:
                                apnContext = addApnContext("mms", networkConfig);
                                break;
                            case 3:
                                apnContext = addApnContext("supl", networkConfig);
                                break;
                            case 4:
                                apnContext = addApnContext("dun", networkConfig);
                                break;
                            case 5:
                                apnContext = addApnContext("hipri", networkConfig);
                                break;
                            default:
                                switch (i) {
                                    case 10:
                                        apnContext = addApnContext("fota", networkConfig);
                                        break;
                                    case 11:
                                        apnContext = addApnContext("ims", networkConfig);
                                        break;
                                    case 12:
                                        apnContext = addApnContext("cbs", networkConfig);
                                        break;
                                    default:
                                        switch (i) {
                                            case 14:
                                                apnContext = addApnContext("ia", networkConfig);
                                                break;
                                            case 15:
                                                apnContext = addApnContext("emergency", networkConfig);
                                                break;
                                            default:
                                                switch (i) {
                                                    case 38:
                                                        apnContext = addApnContext("bip0", networkConfig);
                                                        break;
                                                    case 39:
                                                        apnContext = addApnContext("bip1", networkConfig);
                                                        break;
                                                    case 40:
                                                        apnContext = addApnContext("bip2", networkConfig);
                                                        break;
                                                    case 41:
                                                        apnContext = addApnContext("bip3", networkConfig);
                                                        break;
                                                    case 42:
                                                        apnContext = addApnContext("bip4", networkConfig);
                                                        break;
                                                    case 43:
                                                        apnContext = addApnContext("bip5", networkConfig);
                                                        break;
                                                    case 44:
                                                        apnContext = addApnContext("bip6", networkConfig);
                                                        break;
                                                    case 45:
                                                        apnContext = addApnContext("xcap", networkConfig);
                                                        break;
                                                    default:
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("initApnContexts: skipping unknown type=");
                                                        stringBuilder.append(networkConfig.type);
                                                        log(stringBuilder.toString());
                                                        continue;
                                                        continue;
                                                        continue;
                                                        continue;
                                                }
                                        }
                                }
                        }
                    } else {
                        apnContext = addApnContext("internaldefault", networkConfig);
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("initApnContexts: apnContext=");
                    stringBuilder.append(apnContext);
                    log(stringBuilder.toString());
                }
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("initApnContexts: X mApnContexts=");
        stringBuilder2.append(this.mApnContexts);
        log(stringBuilder2.toString());
        Collections.sort(this.mPrioritySortedApnContexts, new Comparator<ApnContext>() {
            public int compare(ApnContext c1, ApnContext c2) {
                return c2.priority - c1.priority;
            }
        });
    }

    public LinkProperties getLinkProperties(String apnType) {
        ApnContext apnContext = (ApnContext) this.mApnContexts.get(apnType);
        if (apnContext != null) {
            DcAsyncChannel dcac = apnContext.getDcAc();
            if (dcac != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("return link properites for ");
                stringBuilder.append(apnType);
                log(stringBuilder.toString());
                return dcac.getLinkPropertiesSync();
            }
        }
        log("return new LinkProperties");
        return new LinkProperties();
    }

    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        ApnContext apnContext = (ApnContext) this.mApnContexts.get(apnType);
        if (apnContext != null) {
            DcAsyncChannel dataConnectionAc = apnContext.getDcAc();
            if (dataConnectionAc != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("get active pdp is not null, return NetworkCapabilities for ");
                stringBuilder.append(apnType);
                log(stringBuilder.toString());
                return dataConnectionAc.getNetworkCapabilitiesSync();
            }
        }
        log("return new NetworkCapabilities");
        return new NetworkCapabilities();
    }

    public String[] getActiveApnTypes() {
        log("get all active apn types");
        ArrayList<String> result = new ArrayList();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady()) {
                result.add(apnContext.getApnType());
            }
        }
        return (String[]) result.toArray(new String[0]);
    }

    public String getActiveApnString(String apnType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get active apn string for type:");
        stringBuilder.append(apnType);
        log(stringBuilder.toString());
        ApnContext apnContext = (ApnContext) this.mApnContexts.get(apnType);
        if (apnContext != null) {
            ApnSetting apnSetting = apnContext.getApnSetting();
            if (apnSetting != null) {
                return apnSetting.apn;
            }
        }
        return null;
    }

    public State getState(String apnType) {
        for (DataConnection dc : this.mDataConnections.values()) {
            ApnSetting apnSetting = dc.getApnSetting();
            if (apnSetting != null && apnSetting.canHandleType(apnType)) {
                if (dc.isActive()) {
                    return State.CONNECTED;
                }
                if (dc.isActivating()) {
                    return State.CONNECTING;
                }
                if (dc.isInactive()) {
                    return State.IDLE;
                }
                if (dc.isDisconnecting()) {
                    return State.DISCONNECTING;
                }
            }
        }
        return State.IDLE;
    }

    private boolean isProvisioningApn(String apnType) {
        ApnContext apnContext = (ApnContext) this.mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.isProvisioningApn();
        }
        return false;
    }

    public State getOverallState() {
        boolean isConnecting = false;
        boolean isFailed = true;
        boolean isAnyEnabled = false;
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.isEnabled()) {
                isAnyEnabled = true;
                switch (AnonymousClass7.$SwitchMap$com$android$internal$telephony$DctConstants$State[apnContext.getState().ordinal()]) {
                    case 1:
                    case 2:
                        log("overall state is CONNECTED");
                        return State.CONNECTED;
                    case 3:
                    case 4:
                        isConnecting = true;
                        isFailed = false;
                        break;
                    case 5:
                    case 6:
                        isFailed = false;
                        break;
                    default:
                        isAnyEnabled = true;
                        break;
                }
            }
        }
        if (!isAnyEnabled) {
            log("overall state is IDLE");
            return State.IDLE;
        } else if (isConnecting) {
            log("overall state is CONNECTING");
            return State.CONNECTING;
        } else if (isFailed) {
            log("overall state is FAILED");
            return State.FAILED;
        } else {
            log("overall state is IDLE");
            return State.IDLE;
        }
    }

    @VisibleForTesting
    public boolean isDataEnabled() {
        return this.mDataEnabledSettings.isDataEnabled();
    }

    private void onDataConnectionDetached() {
        log("onDataConnectionDetached: stop polling and notify detached");
        stopNetStatPoll();
        stopDataStallAlarm();
        notifyDataConnection(PhoneInternalInterface.REASON_DATA_DETACHED);
        this.mAttached.set(false);
        this.mPhone.getServiceStateTracker().setDoRecoveryTriggerState(false);
        this.mPhone.getServiceStateTracker().setDoRecoveryMarker(true);
        if (this.mCdmaPsRecoveryEnabled && getOverallState() == State.CONNECTED) {
            startPdpResetAlarm(PDP_RESET_ALARM_DELAY_IN_MS);
        }
    }

    private void onDataConnectionAttached() {
        log("onDataConnectionAttached");
        this.mAttached.set(true);
        if (this.mCdmaPsRecoveryEnabled) {
            stopPdpResetAlarm();
        }
        clearRestartRildParam();
        this.mPhone.getServiceStateTracker().setDoRecoveryTriggerState(false);
        if (getOverallState() == State.CONNECTED) {
            log("onDataConnectionAttached: start polling notify attached");
            startNetStatPoll();
            startDataStallAlarm(false);
            notifyDataConnection(PhoneInternalInterface.REASON_DATA_ATTACHED);
        } else {
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_DATA_ATTACHED);
        }
        if (this.mAutoAttachOnCreationConfig) {
            this.mAutoAttachOnCreation.set(true);
        }
        if (!(!isCTSimCard(this.mPhone.getPhoneId()) || this.preSetupBasedRadioTech == 0 || this.preSetupBasedRadioTech == this.mPhone.getServiceState().getRilDataRadioTechnology())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDataConnectionAttached need to clear ApnContext, preSetupBasedRadioTech: ");
            stringBuilder.append(this.preSetupBasedRadioTech);
            log(stringBuilder.toString());
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (apnContext.getState() == State.SCANNING) {
                    apnContext.setState(State.IDLE);
                    cancelReconnectAlarm(apnContext);
                }
            }
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_DATA_ATTACHED);
    }

    public boolean isDataAllowed(DataConnectionReasons dataConnectionReasons) {
        return isDataAllowed(null, dataConnectionReasons);
    }

    boolean isDataAllowed(ApnContext apnContext, DataConnectionReasons dataConnectionReasons) {
        boolean isMeteredApnType;
        ApnContext apnContext2 = apnContext;
        DataConnectionReasons dataConnectionReasons2 = dataConnectionReasons;
        DataConnectionReasons reasons = new DataConnectionReasons();
        boolean internalDataEnabled = this.mDataEnabledSettings.isInternalDataEnabled();
        boolean attachedState = getAttachedStatus(this.mAttached.get());
        boolean desiredPowerState = this.mPhone.getServiceStateTracker().getDesiredPowerState();
        boolean radioStateFromCarrier = this.mPhone.getServiceStateTracker().getPowerStateFromCarrier();
        int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
        if (radioTech == 18) {
            desiredPowerState = true;
            radioStateFromCarrier = true;
        }
        boolean recordsLoaded = this.mIccRecords.get() != null && (((IccRecords) this.mIccRecords.get()).getRecordsLoaded() || ((IccRecords) this.mIccRecords.get()).getImsiReady());
        if (!(this.mIccRecords.get() == null || recordsLoaded)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isDataAllowed getImsiReady=");
            stringBuilder.append(((IccRecords) this.mIccRecords.get()).getImsiReady());
            log(stringBuilder.toString());
        }
        boolean isDataAllowedVoWiFi = HuaweiTelephonyConfigs.isQcomPlatform() && radioTech == 18;
        int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean defaultDataSelected = SubscriptionManager.isValidSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (VSimUtilsInner.isVSimEnabled()) {
            defaultDataSelected = true;
        }
        boolean isMeteredApnType2 = apnContext2 == null || ApnSetting.isMeteredApnType(apnContext.getApnType(), this.mPhone);
        PhoneConstants.State phoneState = PhoneConstants.State.IDLE;
        if (this.mPhone.getCallTracker() != null) {
            phoneState = this.mPhone.getCallTracker().getState();
        }
        if (apnContext2 != null) {
            isMeteredApnType = isMeteredApnType2;
            if (apnContext.getApnType().equals("emergency") && apnContext.isConnectable()) {
                if (dataConnectionReasons2 != null) {
                    dataConnectionReasons2.add(DataAllowedReasonType.EMERGENCY_APN);
                }
                return true;
            }
        }
        isMeteredApnType = isMeteredApnType2;
        if (!(apnContext2 == null || apnContext.isConnectable())) {
            reasons.add(DataDisallowedReasonType.APN_NOT_CONNECTABLE);
        }
        if (apnContext2 != null && ((apnContext.getApnType().equals("default") || apnContext.getApnType().equals("ia")) && radioTech == 18)) {
            reasons.add(DataDisallowedReasonType.ON_IWLAN);
            apnContext2.setPdpFailCause(DcFailCause.NOT_ALLOWED_RADIO_TECHNOLOGY_IWLAN);
        }
        if (isEmergency()) {
            reasons.add(DataDisallowedReasonType.IN_ECBM);
        }
        if (!(attachedState || ((this.mAutoAttachOnCreation.get() && this.mPhone.getSubId() == dataSub) || isNeedForceSetup(apnContext)))) {
            reasons.add(DataDisallowedReasonType.NOT_ATTACHED);
        }
        if (!(recordsLoaded || isNeedForceSetup(apnContext) || isDataAllowedVoWiFi)) {
            reasons.add(DataDisallowedReasonType.RECORD_NOT_LOADED);
        }
        if (!(phoneState == PhoneConstants.State.IDLE || this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) {
            reasons.add(DataDisallowedReasonType.INVALID_PHONE_STATE);
            reasons.add(DataDisallowedReasonType.CONCURRENT_VOICE_DATA_NOT_ALLOWED);
        }
        if (!internalDataEnabled) {
            reasons.add(DataDisallowedReasonType.INTERNAL_DATA_DISABLED);
        }
        if (!defaultDataSelected) {
            reasons.add(DataDisallowedReasonType.DEFAULT_DATA_UNSELECTED);
        }
        if (!(isDataAllowedForRoaming(isMmsApn(apnContext)) || ((isXcapApn(apnContext) && getXcapDataRoamingEnable()) || !this.mPhone.getServiceState().getDataRoaming() || getDataRoamingEnabled()))) {
            reasons.add(DataDisallowedReasonType.ROAMING_DISABLED);
        }
        if (this.mIsPsRestricted) {
            reasons.add(DataDisallowedReasonType.PS_RESTRICTED);
        }
        if (!desiredPowerState) {
            reasons.add(DataDisallowedReasonType.UNDESIRED_POWER_STATE);
        }
        if (!radioStateFromCarrier) {
            reasons.add(DataDisallowedReasonType.RADIO_DISABLED_BY_CARRIER);
        }
        if (!this.mDataEnabledSettings.isDataEnabled()) {
            reasons.add(DataDisallowedReasonType.DATA_DISABLED);
        }
        if (!isPsAllowedByFdn()) {
            reasons.add(DataDisallowedReasonType.PS_RESTRICTED_BY_FDN);
        }
        if (dataSub == 1 && isDataConnectivityDisabled(1, "disable-data")) {
            reasons.add(DataDisallowedReasonType.INTERNAL_DATA_DISABLED);
            cleanUpAllConnections(PhoneInternalInterface.REASON_DATA_DISABLED);
        }
        if (this.mPhone.getSubId() == 1 && isDataDisableBySim2()) {
            log("isDataAllowed sim2 data disable by cust");
            reasons.add(DataDisallowedReasonType.INTERNAL_DATA_DISABLED);
        }
        if (HwTelephonyFactory.getHwDataConnectionManager().isSwitchingToSlave() && get4gSlot() == this.mPhone.getSubId()) {
            reasons.add(DataDisallowedReasonType.INTERNAL_DATA_DISABLED);
        }
        if (reasons.containsHardDisallowedReasons()) {
            if (dataConnectionReasons2 != null) {
                dataConnectionReasons2.copyFrom(reasons);
            }
            return false;
        }
        boolean z;
        if (!(isMeteredApnType || reasons.allowed())) {
            reasons.add(DataAllowedReasonType.UNMETERED_APN);
        }
        if (apnContext2 != null) {
            if (!apnContext2.hasNoRestrictedRequests(true) && reasons.contains(DataDisallowedReasonType.DATA_DISABLED)) {
                reasons.add(DataAllowedReasonType.RESTRICTED_REQUEST);
            }
        }
        if (apnContext2 == null || reasons.allowed()) {
            z = false;
        } else {
            z = false;
            if (getAnyDataEnabledByApnContext(apnContext2, false)) {
                reasons.add(DataAllowedReasonType.NORMAL);
            }
        }
        if ((this.mHwCustDcTracker != null && apnContext2 != null && reasons.allowed() && this.mHwCustDcTracker.isRoamDisallowedByCustomization(apnContext2)) || (isMmsApn(apnContext) && this.mPhone.getServiceState().getDataRoaming() && isRoamingPushDisabled())) {
            reasons.add(DataDisallowedReasonType.ROAMING_DISABLED);
        }
        if (reasons.allowed()) {
            reasons.add(DataAllowedReasonType.NORMAL);
        }
        if (dataConnectionReasons2 != null) {
            dataConnectionReasons2.copyFrom(reasons);
        }
        isMeteredApnType2 = true;
        if (apnContext2 != null) {
            isMeteredApnType2 = isDataAllowedByApnContext(apnContext);
        }
        if (reasons.allowed() && dataAllowedByApnContext) {
            z = true;
        }
        return z;
    }

    private void setupDataOnConnectableApns(String reason) {
        setupDataOnConnectableApns(reason, RetryFailures.ALWAYS);
    }

    private void setupDataOnConnectableApns(String reason, RetryFailures retryFailures) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setupDataOnConnectableApns: ");
        stringBuilder.append(reason);
        log(stringBuilder.toString());
        stringBuilder = new StringBuilder(120);
        Iterator it = this.mPrioritySortedApnContexts.iterator();
        while (it.hasNext()) {
            ApnContext apnContext = (ApnContext) it.next();
            stringBuilder.append(apnContext.getApnType());
            stringBuilder.append(":[state=");
            stringBuilder.append(apnContext.getState());
            stringBuilder.append(",enabled=");
            stringBuilder.append(apnContext.isEnabled());
            stringBuilder.append("] ");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setupDataOnConnectableApns: ");
        stringBuilder2.append(reason);
        stringBuilder2.append(" ");
        stringBuilder2.append(stringBuilder);
        log(stringBuilder2.toString());
        if (!getmIsPseudoImsi() || reason.equals(AbstractPhoneInternalInterface.REASON_SET_PS_ONLY_OK)) {
            Iterator it2 = this.mPrioritySortedApnContexts.iterator();
            while (it2.hasNext()) {
                ApnContext apnContext2 = (ApnContext) it2.next();
                if (apnContext2.getState() == State.FAILED || apnContext2.getState() == State.SCANNING) {
                    if (retryFailures == RetryFailures.ALWAYS) {
                        apnContext2.releaseDataConnection(reason);
                    } else if (!apnContext2.isConcurrentVoiceAndDataAllowed() && this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                        apnContext2.releaseDataConnection(reason);
                    }
                }
                if (isDefaultDataSubscription() && !apnContext2.isEnabled() && PhoneInternalInterface.REASON_SIM_LOADED.equals(reason) && "default".equals(apnContext2.getApnType()) && isDataNeededWithWifiAndBt()) {
                    log("setupDataOnConnectableApns: for IMSI done, call setEnabled");
                    apnContext2.setEnabled(true);
                }
                if (apnContext2.isConnectable()) {
                    log("isConnectable() call trySetupData");
                    if (!getmIsPseudoImsi() || apnContext2.getApnType().equals("bip0")) {
                        this.preSetupBasedRadioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("setupDataOnConnectableApns: current radio technology: ");
                        stringBuilder3.append(this.preSetupBasedRadioTech);
                        log(stringBuilder3.toString());
                        apnContext2.setReason(reason);
                        trySetupData(apnContext2);
                        if (getmIsPseudoImsi()) {
                            log("setupDataOnConnectableApns: pseudo imsi single connection only");
                            break;
                        }
                        HwTelephonyFactory.getHwDataServiceChrManager().setCheckApnContextState(true);
                    }
                } else {
                    HwTelephonyFactory.getHwDataServiceChrManager().sendIntentApnContextDisabledWhenWifiDisconnected(this.mPhone, isWifiConnected(), this.mDataEnabledSettings.isDataEnabled(), apnContext2);
                }
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("getmIsPseudoImsi(): ");
        stringBuilder.append(getmIsPseudoImsi());
        stringBuilder.append("  reason: ");
        stringBuilder.append(reason);
        log(stringBuilder.toString());
    }

    boolean isEmergency() {
        boolean result = this.mPhone.isInEcm() || this.mPhone.isInEmergencyCall();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isEmergency: result=");
        stringBuilder.append(result);
        log(stringBuilder.toString());
        return result;
    }

    private boolean trySetupData(ApnContext apnContext) {
        HwDataConnectionManager sHwDataConnectionManager = HwTelephonyFactory.getHwDataConnectionManager();
        if (sHwDataConnectionManager != null && sHwDataConnectionManager.getNamSwitcherForSoftbank() && sHwDataConnectionManager.isSoftBankCard(this.mPhone) && !sHwDataConnectionManager.isValidMsisdn(this.mPhone)) {
            log("trySetupData sbnam not allow activate data if MSISDN of softbank card is empty  !");
            return false;
        } else if (isDataConnectivityDisabled(this.mPhone.getSubId(), "disable-data")) {
            return false;
        } else {
            int voiceState = this.mPhone.getServiceState().getVoiceRegState();
            int dataState = this.mPhone.getServiceState().getDataRegState();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dataState = ");
            stringBuilder.append(dataState);
            stringBuilder.append("voiceState = ");
            stringBuilder.append(voiceState);
            stringBuilder.append("OperatorNumeric = ");
            stringBuilder.append(getOperatorNumeric());
            log(stringBuilder.toString());
            if ("default".equals(apnContext.getApnType()) && (dataState == 0 || voiceState == 0)) {
                this.mPreferredApn = getApnForCT();
                stringBuilder = new StringBuilder();
                stringBuilder.append("get prefered dp for CT ");
                stringBuilder.append(this.mPreferredApn);
                log(stringBuilder.toString());
                if (this.mPreferredApn == null) {
                    this.mPreferredApn = getPreferredApn();
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("get prefered DP ");
                stringBuilder.append(this.mPreferredApn);
                log(stringBuilder.toString());
            }
            if (VSimUtilsInner.isVSimEnabled() && !VSimUtilsInner.isVSimSub(this.mPhone.getPhoneId()) && !"mms".equals(apnContext.getApnType())) {
                log("trySetupData not allowed vsim is on for non vsim Dds except mms is enabled");
                return false;
            } else if (VSimUtilsInner.isVSimSub(this.mPhone.getPhoneId()) && VSimUtilsInner.isMmsOnM2()) {
                log("trySetupData not allowed for vsim sub while mms is on m2");
                return false;
            } else if (VSimUtilsInner.isVSimOn() && VSimUtilsInner.isSubOnM2(this.mPhone.getPhoneId()) && "mms".equals(apnContext.getApnType()) && VSimUtilsInner.isM2CSOnly()) {
                log("trySetupData not allowed for sub on m2 while ps not ready");
                VSimUtilsInner.checkMmsStart(this.mPhone.getPhoneId());
                return false;
            } else {
                boolean isQcomDualLteImsApn = PhoneFactory.IS_QCOM_DUAL_LTE_STACK && PhoneFactory.IS_DUAL_VOLTE_SUPPORTED && "ims".equals(apnContext.getApnType());
                if (!isDefaultDataSubscription() && !"mms".equals(apnContext.getApnType()) && !"xcap".equals(apnContext.getApnType()) && !isQcomDualLteImsApn && !NetworkFactory.isDualCellDataEnable()) {
                    log("trySetupData not allowed on non defaultDds except mms or xcap or qcomDualLte ims is enabled");
                    return false;
                } else if (this.mPhone.getSimulatedRadioControl() != null) {
                    apnContext.setState(State.CONNECTED);
                    this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
                    log("trySetupData: X We're on the simulator; assuming connected retValue=true");
                    return true;
                } else {
                    DataConnectionReasons dataConnectionReasons = new DataConnectionReasons();
                    boolean isDataAllowed = isDataAllowed(apnContext, dataConnectionReasons);
                    String logStr = new StringBuilder();
                    logStr.append("trySetupData for APN type ");
                    logStr.append(apnContext.getApnType());
                    logStr.append(", reason: ");
                    logStr.append(apnContext.getReason());
                    logStr.append(". ");
                    logStr.append(dataConnectionReasons.toString());
                    logStr = logStr.toString();
                    log(logStr);
                    apnContext.requestLog(logStr);
                    if (getmIsPseudoImsi() || isDataAllowed) {
                        if (apnContext.getState() == State.FAILED) {
                            String str = "trySetupData: make a FAILED ApnContext IDLE so its reusable";
                            log(str);
                            apnContext.requestLog(str);
                            apnContext.setState(State.IDLE);
                        }
                        int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
                        apnContext.setConcurrentVoiceAndDataAllowed(this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
                        if (apnContext.getState() == State.IDLE) {
                            ArrayList<ApnSetting> waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
                            if (waitingApns.isEmpty()) {
                                notifyNoData(DcFailCause.MISSING_UNKNOWN_APN, apnContext);
                                notifyOffApnsOfAvailability(apnContext.getReason());
                                String str2 = "trySetupData: X No APN found retValue=false";
                                log(str2);
                                apnContext.requestLog(str2);
                                return false;
                            }
                            apnContext.setWaitingApns(waitingApns);
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("trySetupData: Create from mAllApnSettings : ");
                            stringBuilder2.append(apnListToString(this.mAllApnSettings));
                            log(stringBuilder2.toString());
                        }
                        boolean unmeteredUseOnly = dataConnectionReasons.contains(DataAllowedReasonType.UNMETERED_APN);
                        if ("default".equals(SystemProperties.get("gsm.bip.apn")) && isBipApnType(apnContext.getApnType())) {
                            unmeteredUseOnly = false;
                        }
                        boolean retValue = setupData(apnContext, radioTech, unmeteredUseOnly);
                        notifyOffApnsOfAvailability(apnContext.getReason());
                        sendOTAAttachTimeoutMsg(apnContext, retValue);
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("trySetupData: X retValue=");
                        stringBuilder3.append(retValue);
                        log(stringBuilder3.toString());
                        return retValue;
                    }
                    if (!apnContext.getApnType().equals("default") && apnContext.isConnectable()) {
                        this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
                    }
                    notifyOffApnsOfAvailability(apnContext.getReason());
                    HwTelephonyFactory.getHwDataServiceChrManager().sendIntentApnContextDisabledWhenWifiDisconnected(this.mPhone, isWifiConnected(), this.mDataEnabledSettings.isDataEnabled(), apnContext);
                    StringBuilder str3 = new StringBuilder();
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("trySetupData failed. apnContext = [type=");
                    stringBuilder4.append(apnContext.getApnType());
                    stringBuilder4.append(", mState=");
                    stringBuilder4.append(apnContext.getState());
                    stringBuilder4.append(", apnEnabled=");
                    stringBuilder4.append(apnContext.isEnabled());
                    stringBuilder4.append(", mDependencyMet=");
                    stringBuilder4.append(apnContext.getDependencyMet());
                    stringBuilder4.append("] ");
                    str3.append(stringBuilder4.toString());
                    if (!this.mDataEnabledSettings.isDataEnabled()) {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("isDataEnabled() = false. ");
                        stringBuilder4.append(this.mDataEnabledSettings);
                        str3.append(stringBuilder4.toString());
                    }
                    if (apnContext.getState() == State.SCANNING) {
                        apnContext.setState(State.FAILED);
                        str3.append(" Stop retrying.");
                    }
                    log(str3.toString());
                    apnContext.requestLog(str3.toString());
                    return false;
                }
            }
        }
    }

    protected void notifyOffApnsOfAvailability(String reason) {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            StringBuilder stringBuilder;
            if (this.mAttached.get() && apnContext.isReady()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("notifyOffApnsOfAvailability skipped apn due to attached && isReady ");
                stringBuilder.append(apnContext.toString());
                log(stringBuilder.toString());
            } else if (apnContext.getApnType() == null || !apnContext.getApnType().startsWith("bip")) {
                String str;
                stringBuilder = new StringBuilder();
                stringBuilder.append("notifyOffApnOfAvailability type:");
                stringBuilder.append(apnContext.getApnType());
                log(stringBuilder.toString());
                Phone phone = this.mPhone;
                if (reason != null) {
                    str = reason;
                } else {
                    str = apnContext.getReason();
                }
                phone.notifyDataConnection(str, apnContext.getApnType(), DataState.DISCONNECTED);
            }
        }
    }

    protected boolean cleanUpAllConnections(boolean tearDown, String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cleanUpAllConnections: tearDown=");
        stringBuilder.append(tearDown);
        stringBuilder.append(" reason=");
        stringBuilder.append(reason);
        log(stringBuilder.toString());
        boolean didDisconnect = false;
        boolean disableMeteredOnly = false;
        if (!TextUtils.isEmpty(reason)) {
            boolean z = reason.equals(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED) || reason.equals(PhoneInternalInterface.REASON_ROAMING_ON) || reason.equals(PhoneInternalInterface.REASON_CARRIER_ACTION_DISABLE_METERED_APN) || reason.equals(PhoneInternalInterface.REASON_PDP_RESET);
            disableMeteredOnly = z;
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            switch (AnonymousClass7.$SwitchMap$com$android$internal$telephony$DctConstants$State[apnContext.getState().ordinal()]) {
                case 5:
                case 6:
                case 7:
                    break;
                default:
                    didDisconnect = true;
                    if (!disableMeteredOnly) {
                        apnContext.setReason(reason);
                        cleanUpConnection(tearDown, apnContext);
                        break;
                    }
                    ApnSetting apnSetting = apnContext.getApnSetting();
                    if (!(apnSetting == null || !apnSetting.isMetered(this.mPhone) || apnContext.getApnType().equals("xcap"))) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("clean up metered ApnContext Type: ");
                        stringBuilder2.append(apnContext.getApnType());
                        log(stringBuilder2.toString());
                        apnContext.setReason(reason);
                        cleanUpConnection(tearDown, apnContext);
                        break;
                    }
            }
        }
        stopNetStatPoll();
        stopDataStallAlarm();
        this.mRequestedApnType = "default";
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("cleanUpConnection: mDisconnectPendingCount = ");
        stringBuilder3.append(this.mDisconnectPendingCount);
        log(stringBuilder3.toString());
        if (tearDown && this.mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
        return didDisconnect;
    }

    private void onCleanUpAllConnections(String cause) {
        cleanUpAllConnections(true, cause);
    }

    void sendCleanUpConnection(boolean tearDown, ApnContext apnContext) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendCleanUpConnection: tearDown=");
        stringBuilder.append(tearDown);
        stringBuilder.append(" apnContext=");
        stringBuilder.append(apnContext);
        log(stringBuilder.toString());
        Message msg = obtainMessage(270360);
        msg.arg1 = tearDown;
        msg.arg2 = 0;
        msg.obj = apnContext;
        sendMessage(msg);
    }

    protected void cleanUpConnection(boolean tearDown, ApnContext apnContext) {
        if (apnContext == null) {
            log("cleanUpConnection: apn context is null");
            return;
        }
        DcAsyncChannel dcac = apnContext.getDcAc();
        String str = new StringBuilder();
        str.append("cleanUpConnection: tearDown=");
        str.append(tearDown);
        str.append(" reason=");
        str.append(apnContext.getReason());
        str = str.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(" apnContext=");
        stringBuilder.append(apnContext);
        log(stringBuilder.toString());
        apnContext.requestLog(str);
        if (tearDown) {
            if (apnContext.isDisconnected()) {
                apnContext.setState(State.IDLE);
                if (!apnContext.isReady()) {
                    if (dcac != null) {
                        str = "cleanUpConnection: teardown, disconnected, !ready";
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(str);
                        stringBuilder2.append(" apnContext=");
                        stringBuilder2.append(apnContext);
                        log(stringBuilder2.toString());
                        apnContext.requestLog(str);
                        dcac.tearDown(apnContext, "", null);
                    }
                    apnContext.setDataConnectionAc(null);
                }
            } else if (dcac == null) {
                apnContext.setState(State.IDLE);
                apnContext.requestLog("cleanUpConnection: connected, bug no DCAC");
                this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            } else if (apnContext.getState() != State.DISCONNECTING) {
                boolean disconnectAll = false;
                if ("dun".equals(apnContext.getApnType()) && teardownForDun()) {
                    log("cleanUpConnection: disconnectAll DUN connection");
                    disconnectAll = true;
                }
                int generation = apnContext.getConnectionGeneration();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("cleanUpConnection: tearing down");
                stringBuilder3.append(disconnectAll ? " all" : "");
                stringBuilder3.append(" using gen#");
                stringBuilder3.append(generation);
                str = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(str);
                stringBuilder3.append("apnContext=");
                stringBuilder3.append(apnContext);
                log(stringBuilder3.toString());
                apnContext.requestLog(str);
                Message msg = obtainMessage(270351, new Pair(apnContext, Integer.valueOf(generation)));
                if (disconnectAll) {
                    apnContext.getDcAc().tearDownAll(apnContext.getReason(), msg);
                } else {
                    apnContext.getDcAc().tearDown(apnContext, apnContext.getReason(), msg);
                }
                apnContext.setState(State.DISCONNECTING);
                this.mDisconnectPendingCount++;
            }
        } else if (PhoneInternalInterface.REASON_RADIO_TURNED_OFF.equals(apnContext.getReason()) && apnContext.getState() == State.CONNECTING) {
            log("ignore the set IDLE message, because the current state is connecting!");
        } else {
            if (dcac != null) {
                dcac.reqReset();
            }
            apnContext.setState(State.IDLE);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            apnContext.setDataConnectionAc(null);
        }
        setupDataForSinglePdnArbitration(apnContext.getReason());
        if (dcac != null) {
            cancelReconnectAlarm(apnContext);
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("cleanUpConnection: X tearDown=");
        stringBuilder.append(tearDown);
        stringBuilder.append(" reason=");
        stringBuilder.append(apnContext.getReason());
        str = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(" apnContext=");
        stringBuilder.append(apnContext);
        stringBuilder.append(" dcac=");
        stringBuilder.append(apnContext.getDcAc());
        log(stringBuilder.toString());
        apnContext.requestLog(str);
    }

    @VisibleForTesting
    public ArrayList<ApnSetting> fetchDunApns() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            log("fetchDunApns: net.tethering.noprovisioning=true ret: empty list");
            return new ArrayList(0);
        }
        int bearer = this.mPhone.getServiceState().getRilDataRadioTechnology();
        IccRecords r = (IccRecords) this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        ArrayList<ApnSetting> dunCandidates = new ArrayList();
        ArrayList<ApnSetting> retDunSettings = new ArrayList();
        ApnSetting preferredApn = getPreferredApn();
        if (this.mHwCustDcTracker == null || !this.mHwCustDcTracker.isDocomoApn(preferredApn)) {
            StringBuilder stringBuilder;
            String apnData = Global.getString(this.mResolver, "tether_dun_apn");
            if (!TextUtils.isEmpty(apnData)) {
                dunCandidates.addAll(ApnSetting.arrayFromString(apnData));
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("fetchDunApns: dunCandidates from Setting: ");
                stringBuilder2.append(dunCandidates);
                log(stringBuilder2.toString());
            }
            if (preferredApn != null) {
                for (String type : preferredApn.types) {
                    if (type.contains("dun")) {
                        dunCandidates.add(preferredApn);
                        log("fetchDunApn: add preferredApn");
                        break;
                    }
                }
            }
            if (dunCandidates.isEmpty() && !ArrayUtils.isEmpty(this.mAllApnSettings)) {
                Iterator it = this.mAllApnSettings.iterator();
                while (it.hasNext()) {
                    ApnSetting apn = (ApnSetting) it.next();
                    for (String type2 : apn.types) {
                        if (type2.contains("dun")) {
                            dunCandidates.add(apn);
                        }
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("fetchDunApns: dunCandidates from database: ");
                stringBuilder.append(dunCandidates);
                log(stringBuilder.toString());
            }
            Iterator it2 = dunCandidates.iterator();
            while (it2.hasNext()) {
                ApnSetting dunSetting = (ApnSetting) it2.next();
                if (ServiceState.bitmaskHasTech(dunSetting.networkTypeBitmask, ServiceState.rilRadioTechnologyToNetworkType(bearer))) {
                    if (dunSetting.numeric.equals(operator)) {
                        if (this.mHwCustDcTracker == null || !this.mHwCustDcTracker.addSpecifiedApnSwitch()) {
                            if (!dunSetting.hasMvnoParams()) {
                                if (!this.mMvnoMatched) {
                                    retDunSettings.add(dunSetting);
                                    break;
                                }
                            } else if (r != null && ApnSetting.mvnoMatches(r, dunSetting.mvnoType, dunSetting.mvnoMatchData)) {
                                retDunSettings.add(dunSetting);
                                break;
                            }
                        } else if (this.mHwCustDcTracker.addSpecifiedApnToWaitingApns(this, preferredApn, dunSetting)) {
                            retDunSettings.add(dunSetting);
                            break;
                        }
                    }
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("fetchDunApns: dunSettings=");
            stringBuilder.append(retDunSettings);
            log(stringBuilder.toString());
            return retDunSettings;
        }
        retDunSettings.add(this.mHwCustDcTracker.getDocomoApn(preferredApn));
        return retDunSettings;
    }

    private int getPreferredApnSetId() {
        ContentResolver contentResolver = this.mPhone.getContext().getContentResolver();
        Uri uri = Carriers.CONTENT_URI;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("preferapnset/subId/");
        stringBuilder.append(this.mPhone.getSubId());
        Cursor c = contentResolver.query(Uri.withAppendedPath(uri, stringBuilder.toString()), new String[]{"apn_set_id"}, null, null, null);
        if (c == null) {
            loge("getPreferredApnSetId: cursor is null");
            return 0;
        }
        int setId;
        if (c.getCount() < 1) {
            loge("getPreferredApnSetId: no APNs found");
            setId = 0;
        } else {
            c.moveToFirst();
            setId = c.getInt(0);
        }
        if (!c.isClosed()) {
            c.close();
        }
        return setId;
    }

    public boolean hasMatchedTetherApnSetting() {
        ArrayList<ApnSetting> matches = fetchDunApns();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hasMatchedTetherApnSetting: APNs=");
        stringBuilder.append(matches);
        log(stringBuilder.toString());
        return matches.size() > 0;
    }

    protected void setupDataForSinglePdnArbitration(String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setupDataForSinglePdn: reason = ");
        stringBuilder.append(reason);
        stringBuilder.append(" isDisconnected = ");
        stringBuilder.append(isDisconnected());
        log(stringBuilder.toString());
        if (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) && isDisconnected() && !PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION.equals(reason)) {
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION);
        }
    }

    private boolean teardownForDun() {
        boolean z = true;
        if (ServiceState.isCdma(this.mPhone.getServiceState().getRilDataRadioTechnology())) {
            return true;
        }
        if (fetchDunApns().size() <= 0) {
            z = false;
        }
        return z;
    }

    private void cancelReconnectAlarm(ApnContext apnContext) {
        if (apnContext != null) {
            PendingIntent intent = apnContext.getReconnectIntent();
            if (intent != null) {
                ((AlarmManager) this.mPhone.getContext().getSystemService("alarm")).cancel(intent);
                apnContext.setReconnectIntent(null);
            }
        }
    }

    private String[] parseTypes(String types) {
        if (types != null && !types.equals("")) {
            return types.split(",");
        }
        return new String[]{CharacterSets.MIMENAME_ANY_CHARSET};
    }

    boolean isPermanentFailure(DcFailCause dcFailCause) {
        return dcFailCause.isPermanentFailure(this.mPhone.getContext(), this.mPhone.getSubId()) && !(this.mAttached.get() && dcFailCause == DcFailCause.SIGNAL_LOST);
    }

    private ApnSetting makeApnSetting(Cursor cursor) {
        Cursor cursor2 = cursor;
        String[] types = parseTypes(cursor2.getString(cursor2.getColumnIndexOrThrow("type")));
        ApnSetting apn = new ApnSetting(cursor2.getInt(cursor2.getColumnIndexOrThrow(HbpcdLookup.ID)), cursor2.getString(cursor2.getColumnIndexOrThrow("numeric")), cursor2.getString(cursor2.getColumnIndexOrThrow("name")), cursor2.getString(cursor2.getColumnIndexOrThrow("apn")), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("proxy"))), cursor2.getString(cursor2.getColumnIndexOrThrow("port")), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("mmsc"))), NetworkUtils.trimV4AddrZeros(cursor2.getString(cursor2.getColumnIndexOrThrow("mmsproxy"))), cursor2.getString(cursor2.getColumnIndexOrThrow("mmsport")), cursor2.getString(cursor2.getColumnIndexOrThrow("user")), cursor2.getString(cursor2.getColumnIndexOrThrow("password")), cursor2.getInt(cursor2.getColumnIndexOrThrow("authtype")), types, cursor2.getString(cursor2.getColumnIndexOrThrow("protocol")), cursor2.getString(cursor2.getColumnIndexOrThrow("roaming_protocol")), cursor2.getInt(cursor2.getColumnIndexOrThrow("carrier_enabled")) == 1, cursor2.getInt(cursor2.getColumnIndexOrThrow("network_type_bitmask")), cursor2.getInt(cursor2.getColumnIndexOrThrow("profile_id")), cursor2.getInt(cursor2.getColumnIndexOrThrow("modem_cognitive")) == 1, cursor2.getInt(cursor2.getColumnIndexOrThrow("max_conns")), cursor2.getInt(cursor2.getColumnIndexOrThrow("wait_time")), cursor2.getInt(cursor2.getColumnIndexOrThrow("max_conns_time")), cursor2.getInt(cursor2.getColumnIndexOrThrow("mtu")), cursor2.getString(cursor2.getColumnIndexOrThrow("mvno_type")), cursor2.getString(cursor2.getColumnIndexOrThrow("mvno_match_data")), cursor2.getInt(cursor2.getColumnIndexOrThrow("apn_set_id")));
        ApnSetting hwApn = makeHwApnSetting(cursor2, types);
        if (hwApn != null) {
            return hwApn;
        }
        return apn;
    }

    private ArrayList<ApnSetting> createApnList(Cursor cursor) {
        ArrayList<ApnSetting> result;
        ArrayList<ApnSetting> mnoApns = new ArrayList();
        ArrayList<ApnSetting> mvnoApns = new ArrayList();
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (cursor.moveToFirst()) {
            do {
                ApnSetting apn = makeApnSetting(cursor);
                if (apn != null) {
                    if (!apn.hasMvnoParams()) {
                        mnoApns.add(apn);
                    } else if (r != null && ApnSetting.mvnoMatches(r, apn.mvnoType, apn.mvnoMatchData)) {
                        mvnoApns.add(apn);
                    }
                }
            } while (cursor.moveToNext());
        }
        if (mvnoApns.isEmpty()) {
            result = mnoApns;
            this.mMvnoMatched = false;
        } else {
            result = mvnoApns;
            this.mMvnoMatched = true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createApnList: X result=");
        stringBuilder.append(result);
        log(stringBuilder.toString());
        return result;
    }

    private boolean dataConnectionNotInUse(DcAsyncChannel dcac) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dataConnectionNotInUse: check if dcac is inuse dcac=");
        stringBuilder.append(dcac);
        log(stringBuilder.toString());
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getDcAc() == dcac) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("dataConnectionNotInUse: in use by apnContext=");
                stringBuilder.append(apnContext);
                log(stringBuilder.toString());
                return false;
            }
        }
        log("dataConnectionNotInUse: tearDownAll");
        dcac.tearDownAll("No connection", null);
        log("dataConnectionNotInUse: not in use return true");
        return true;
    }

    private DcAsyncChannel findFreeDataConnection() {
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            if (dcac.isInactiveSync() && dataConnectionNotInUse(dcac)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("findFreeDataConnection: found free DataConnection= dcac=");
                stringBuilder.append(dcac);
                log(stringBuilder.toString());
                return dcac;
            }
        }
        log("findFreeDataConnection: NO free DataConnection");
        return null;
    }

    protected boolean isLTENetwork() {
        int dataRadioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dataRadioTech = ");
        stringBuilder.append(dataRadioTech);
        log(stringBuilder.toString());
        if (dataRadioTech == 13 || dataRadioTech == 14) {
            return true;
        }
        return false;
    }

    private ApnSetting getApnForCT() {
        if (!isCTSimCard(this.mPhone.getPhoneId())) {
            log("getApnForCT not isCTSimCard");
            return null;
        } else if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty()) {
            log("getApnForCT mAllApnSettings == null");
            return null;
        } else if (get2gSlot() == this.mPhone.getSubId() && !isCTDualModeCard(get2gSlot())) {
            log("getApnForCT otherslot == mPhone.getSubId() && !isCTDualModeCard(otherslot)");
            return null;
        } else if (this.mPhone.getServiceState().getOperatorNumeric() == null) {
            log("getApnForCT mPhone.getServiceState().getOperatorNumeric() == null");
            return null;
        } else if (getPreferredApn() != null && !isApnPreset(getPreferredApn())) {
            return null;
        } else {
            ApnSetting apnSetting = null;
            this.mCurrentState = getCurState();
            int matchApnId = matchApnId(this.mCurrentState);
            if (-1 == matchApnId) {
                switch (this.mCurrentState) {
                    case 0:
                        if (!isCTCardForFullNet()) {
                            apnSetting = setApnForCT(CT_NOT_ROAMING_APN_PREFIX);
                            break;
                        }
                        log("getApnForCT: select ctnet for fullNet product");
                        apnSetting = setApnForCT(CT_ROAMING_APN_PREFIX);
                        break;
                    case 1:
                    case 2:
                    case 3:
                        apnSetting = setApnForCT(CT_ROAMING_APN_PREFIX);
                        break;
                    case 4:
                        apnSetting = setApnForCT(CT_LTE_APN_PREFIX);
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Error in CurrentState");
                        stringBuilder.append(this.mCurrentState);
                        log(stringBuilder.toString());
                        break;
                }
            }
            setPreferredApn(matchApnId);
            return apnSetting;
        }
    }

    private int matchApnId(int sign) {
        String is4gSlot;
        String preferredApnIdSlot;
        ContentResolver cr = this.mPhone.getContext().getContentResolver();
        int matchId = -1;
        if (this.isMultiSimEnabled) {
            is4gSlot = get4gSlot() == this.mPhone.getSubId() ? "4gSlot" : "2gSlot";
            preferredApnIdSlot = new StringBuilder();
            preferredApnIdSlot.append(PREFERRED_APN_ID);
            preferredApnIdSlot.append(is4gSlot);
            preferredApnIdSlot = preferredApnIdSlot.toString();
        } else {
            preferredApnIdSlot = PREFERRED_APN_ID;
        }
        is4gSlot = preferredApnIdSlot;
        StringBuilder stringBuilder;
        try {
            preferredApnIdSlot = System.getString(cr, is4gSlot);
            stringBuilder = new StringBuilder();
            stringBuilder.append("MatchApnId:LastApnId: ");
            stringBuilder.append(preferredApnIdSlot);
            stringBuilder.append(", CurrentState: ");
            stringBuilder.append(this.mCurrentState);
            stringBuilder.append(", preferredApnIdSlot: ");
            stringBuilder.append(is4gSlot);
            log(stringBuilder.toString());
            if (preferredApnIdSlot != null) {
                String[] ApId = preferredApnIdSlot.split(",");
                if (5 != ApId.length || ApId[this.mCurrentState] == null) {
                    System.putString(cr, is4gSlot, this.mDefaultApnId);
                } else if (!ProxyController.MODEM_0.equals(ApId[this.mCurrentState])) {
                    matchId = Integer.parseInt(ApId[this.mCurrentState]);
                }
            } else {
                System.putString(cr, is4gSlot, this.mDefaultApnId);
            }
        } catch (Exception ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("MatchApnId got exception =");
            stringBuilder.append(ex);
            log(stringBuilder.toString());
            System.putString(cr, is4gSlot, this.mDefaultApnId);
        }
        return matchId;
    }

    private int getCurState() {
        int currentStatus = -1;
        if (isLTENetwork()) {
            currentStatus = 4;
        } else if (this.mPhone.getPhoneType() == 2) {
            currentStatus = TelephonyManager.getDefault().isNetworkRoaming(get4gSlot()) ? 1 : 0;
        } else if (this.mPhone.getPhoneType() == 1) {
            if (get4gSlot() == this.mPhone.getSubId() && TelephonyManager.getDefault().isNetworkRoaming(get4gSlot())) {
                currentStatus = 2;
            } else if (get2gSlot() == this.mPhone.getSubId() && TelephonyManager.getDefault().isNetworkRoaming(get2gSlot())) {
                currentStatus = 3;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCurState:CurrentStatus =");
        stringBuilder.append(currentStatus);
        log(stringBuilder.toString());
        return currentStatus;
    }

    /* JADX WARNING: Missing block: B:35:0x0096, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ApnSetting setApnForCT(String apn) {
        if (apn == null || "".equals(apn)) {
            return null;
        }
        ContentResolver resolver = this.mPhone.getContext().getContentResolver();
        if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty() || resolver == null) {
            return null;
        }
        ContentValues values = new ContentValues();
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, Long.toString((long) this.mPhone.getSubId()));
        Iterator it = this.mAllApnSettings.iterator();
        while (it.hasNext()) {
            ApnSetting dp = (ApnSetting) it.next();
            if (apn.equals(dp.apn) && dp.canHandleType(this.mRequestedApnType)) {
                if (!isLTENetwork() || dp.bearer == 13 || dp.bearer == 14) {
                    if (!isLTENetwork()) {
                        if (dp.bearer == 13) {
                            continue;
                        } else if (dp.bearer == 14) {
                        }
                    }
                    resolver.delete(uri, null, null);
                    values.put(APN_ID, Integer.valueOf(dp.id));
                    resolver.insert(uri, values);
                    return dp;
                }
            }
        }
        return null;
    }

    private boolean setupData(ApnContext apnContext, int radioTech, boolean unmeteredUseOnly) {
        ApnContext apnContext2 = apnContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setupData: apnContext=");
        stringBuilder.append(apnContext2);
        log(stringBuilder.toString());
        apnContext2.requestLog("setupData");
        DcAsyncChannel dcac = null;
        ApnSetting apnSetting = apnContext.getNextApnSetting();
        if (apnSetting == null) {
            log("setupData: return for no apn found!");
            return false;
        }
        int i;
        int profileId = apnSetting.profileId;
        if (profileId == 0) {
            profileId = getApnProfileID(apnContext.getApnType());
        }
        int profileId2 = profileId;
        if (!(apnContext.getApnType().equals("dun") && teardownForDun())) {
            dcac = checkForCompatibleConnectedApnContext(apnContext);
        }
        if (dcac == null) {
            i = radioTech;
            if (isOnlySingleDcAllowed(i)) {
                if (isHigherPriorityApnContextActive(apnContext)) {
                    log("setupData: Higher priority ApnContext active.  Ignoring call");
                    return false;
                } else if (apnContext.getApnType().equals("ims") || !cleanUpAllConnections(true, PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION)) {
                    log("setupData: Single pdp. Continue setting up data call.");
                } else {
                    log("setupData: Some calls are disconnecting first. Wait and retry");
                    return false;
                }
            }
            dcac = findFreeDataConnection();
            if (dcac == null) {
                dcac = createDataConnection();
            }
            if (dcac == null) {
                log("setupData: No free DataConnection and couldn't create one, WEIRD");
                return false;
            }
        }
        i = radioTech;
        DcAsyncChannel dcac2 = dcac;
        int generation = apnContext.incAndGetConnectionGeneration();
        stringBuilder = new StringBuilder();
        stringBuilder.append("setupData: dcac=");
        stringBuilder.append(dcac2);
        stringBuilder.append(" apnSetting=");
        stringBuilder.append(apnSetting);
        stringBuilder.append(" gen#=");
        stringBuilder.append(generation);
        log(stringBuilder.toString());
        apnContext2.setDataConnectionAc(dcac2);
        apnContext2.setApnSetting(apnSetting);
        apnContext2.setState(State.CONNECTING);
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        Message msg = obtainMessage();
        msg.what = 270336;
        msg.obj = new Pair(apnContext2, Integer.valueOf(generation));
        HwTelephonyFactory.getHwDataServiceChrManager().setBringUp(true);
        dcac2.bringUp(apnContext2, profileId2, i, unmeteredUseOnly, msg, generation);
        log("setupData: initing!");
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:90:0x0231  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x020c  */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x0282  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x0261  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setInitialAttachApn() {
        ApnSetting iaApnSetting = null;
        ApnSetting defaultApnSetting = null;
        ApnSetting firstApnSetting = null;
        if (get4gSlot() != this.mPhone.getSubId() && !HwModemCapability.isCapabilitySupport(21)) {
            log("setInitialAttachApn: not 4g slot , skip");
            if (IS_DELAY_ATTACH_ENABLED) {
                log("setInitialAttachApn: sbnam APN handling done, activate cs&ps");
                this.mPhone.mCi.dataConnectionAttach(1, null);
            }
        } else if (VSimUtilsInner.isVSimOn() || VSimUtilsInner.isVSimInProcess()) {
            log("setInitialAttachApn: vsim is on or in process, skip");
        } else if (SystemProperties.getBoolean("persist.radio.iot_attach_apn", false)) {
            log("setInitialAttachApn: iot attach apn enabled, skip");
        } else {
            int esmFlagFromCard;
            StringBuilder stringBuilder;
            String operator;
            int esmFlag = 0;
            boolean esmFlagAdaptionEnabled = getEsmFlagAdaptionEnabled();
            if (esmFlagAdaptionEnabled) {
                esmFlagFromCard = getEsmFlagFromCard();
                if (esmFlagFromCard != -1) {
                    esmFlag = esmFlagFromCard;
                } else {
                    String plmnsConfig = System.getString(this.mPhone.getContext().getContentResolver(), "plmn_esm_flag");
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("setInitialAttachApn: plmnsConfig = ");
                    stringBuilder.append(plmnsConfig);
                    log(stringBuilder.toString());
                    IccRecords r = (IccRecords) this.mIccRecords.get();
                    operator = r != null ? r.getOperatorNumeric() : "null";
                    if (plmnsConfig != null) {
                        for (String plmn : plmnsConfig.split(",")) {
                            if (plmn != null && plmn.equals(operator)) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("setInitialAttachApn: send initial attach apn for operator ");
                                stringBuilder2.append(operator);
                                log(stringBuilder2.toString());
                                esmFlag = 1;
                                break;
                            }
                        }
                    }
                }
            }
            if (isCTSimCard(this.mPhone.getPhoneId())) {
                log("setInitialAttachApn: send initial attach apn for CT");
                esmFlag = 1;
            }
            if (esmFlag == 0 && this.mPreferredApn != null && !isApnPreset(this.mPreferredApn) && this.mPreferredApn.canHandleType("ia")) {
                log("setInitialAttachApn: send initial attach apn for IA");
                esmFlag = 1;
            }
            if (esmFlag != 0) {
                ApnSetting initialAttachApnSetting;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("setInitialApn: E mPreferredApn=");
                stringBuilder3.append(this.mPreferredApn);
                log(stringBuilder3.toString());
                if (this.mPreferredApn != null && this.mPreferredApn.canHandleType("ia")) {
                    iaApnSetting = this.mPreferredApn;
                } else if (!(this.mAllApnSettings == null || this.mAllApnSettings.isEmpty())) {
                    firstApnSetting = (ApnSetting) this.mAllApnSettings.get(0);
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("setInitialApn: firstApnSetting=");
                    stringBuilder3.append(firstApnSetting);
                    log(stringBuilder3.toString());
                    Iterator it = this.mAllApnSettings.iterator();
                    while (it.hasNext()) {
                        ApnSetting apn = (ApnSetting) it.next();
                        if (apn.canHandleType("ia")) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("setInitialApn: iaApnSetting=");
                            stringBuilder3.append(apn);
                            log(stringBuilder3.toString());
                            iaApnSetting = apn;
                            break;
                        } else if (defaultApnSetting == null && apn.canHandleType("default")) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("setInitialApn: defaultApnSetting=");
                            stringBuilder.append(apn);
                            log(stringBuilder.toString());
                            if (!isCTSimCard(this.mPhone.getPhoneId())) {
                                defaultApnSetting = apn;
                            } else if (isSupportLTE(apn)) {
                                defaultApnSetting = apn;
                            }
                        }
                    }
                    initialAttachApnSetting = null;
                    if (this.mPreferredApn == null) {
                        log("setInitialAttachApn: using mPreferredApn");
                        initialAttachApnSetting = isCTSimCard(this.mPhone.getPhoneId()) ? isSupportLTE(this.mPreferredApn) ? this.mPreferredApn : defaultApnSetting != null ? defaultApnSetting : iaApnSetting : this.mPreferredApn;
                    } else if (defaultApnSetting != null) {
                        log("setInitialAttachApn: using defaultApnSetting");
                        initialAttachApnSetting = defaultApnSetting;
                    } else if (iaApnSetting != null) {
                        log("setInitialAttachApn: using iaApnSetting");
                        initialAttachApnSetting = iaApnSetting;
                    } else if (firstApnSetting != null) {
                        log("setInitialAttachApn: using firstApnSetting");
                        if (!isCTSimCard(this.mPhone.getPhoneId())) {
                            initialAttachApnSetting = firstApnSetting;
                        } else if (isSupportLTE(firstApnSetting)) {
                            initialAttachApnSetting = firstApnSetting;
                        }
                    }
                    if (initialAttachApnSetting != null) {
                        log("setInitialAttachApn: X There in no available apn");
                        if (IS_DELAY_ATTACH_ENABLED) {
                            log("setInitialAttachApn: sbnam APN handling done, activate cs&ps");
                            this.mPhone.mCi.dataConnectionAttach(1, null);
                        }
                    } else {
                        ApnSetting apnSetting;
                        ApnSetting apnSetting2;
                        int i;
                        boolean z;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("setInitialAttachApn: X selected Apn=");
                        stringBuilder4.append(initialAttachApnSetting);
                        log(stringBuilder4.toString());
                        HwDataConnectionManager sHwDataConnectionManager = HwTelephonyFactory.getHwDataConnectionManager();
                        if (sHwDataConnectionManager != null && sHwDataConnectionManager.getNamSwitcherForSoftbank()) {
                            HashMap<String, String> userInfo = sHwDataConnectionManager.encryptApnInfoForSoftBank(this.mPhone, initialAttachApnSetting);
                            if (userInfo != null) {
                                operator = "username";
                                String pswKey = "password";
                                String username = (String) userInfo.get(operator);
                                String password = (String) userInfo.get(pswKey);
                                DataServiceManager dataServiceManager = this.mDataServiceManager;
                                esmFlagFromCard = initialAttachApnSetting.profileId;
                                String str = initialAttachApnSetting.apn;
                                ApnSetting apnSetting3 = iaApnSetting;
                                String str2 = initialAttachApnSetting.protocol;
                                apnSetting = defaultApnSetting;
                                int i2 = initialAttachApnSetting.authType;
                                apnSetting2 = firstApnSetting;
                                int i3 = initialAttachApnSetting.bearerBitmask == null ? 0 : ServiceState.bearerBitmapHasCdma(initialAttachApnSetting.bearerBitmask) ? 2 : 1;
                                i = esmFlag;
                                z = esmFlagAdaptionEnabled;
                                dataServiceManager.setInitialAttachApn(new DataProfile(esmFlagFromCard, str, str2, i2, username, password, i3, initialAttachApnSetting.maxConnsTime, initialAttachApnSetting.maxConns, initialAttachApnSetting.waitTime, initialAttachApnSetting.carrierEnabled, initialAttachApnSetting.typesBitmap, initialAttachApnSetting.roamingProtocol, initialAttachApnSetting.bearerBitmask, initialAttachApnSetting.mtu, initialAttachApnSetting.mvnoType, initialAttachApnSetting.mvnoMatchData, initialAttachApnSetting.modemCognitive), this.mPhone.getServiceState().getDataRoaming(), null);
                                log("onConnect: mApnSetting.user-mApnSetting.password handle finish");
                                return;
                            }
                        }
                        apnSetting = defaultApnSetting;
                        apnSetting2 = firstApnSetting;
                        i = esmFlag;
                        z = esmFlagAdaptionEnabled;
                        HwDataConnectionManager hwDataConnectionManager = sHwDataConnectionManager;
                        this.mDataServiceManager.setInitialAttachApn(createDataProfile(initialAttachApnSetting), this.mPhone.getServiceState().getDataRoamingFromRegistration(), null);
                    }
                }
                initialAttachApnSetting = null;
                if (this.mPreferredApn == null) {
                }
                if (initialAttachApnSetting != null) {
                }
            } else if (esmFlagAdaptionEnabled) {
                log("setInitialAttachApn: send empty initial attach apn to clear esmflag");
                this.mDataServiceManager.setInitialAttachApn(new DataProfile(0, "", SystemProperties.get("ro.config.attach_ip_type", "IP"), 0, "", "", 0, 0, 0, 0, false, 0, "", 0, 0, "", "", false), this.mPhone.getServiceState().getDataRoaming(), null);
            } else {
                log("setInitialAttachApn: no need to send initial attach apn");
                if (IS_DELAY_ATTACH_ENABLED) {
                    log("setInitialAttachApn: sbnam APN handling done, activate cs&ps");
                    this.mPhone.mCi.dataConnectionAttach(1, null);
                }
            }
        }
    }

    private void onApnChanged() {
        State overallState = getOverallState();
        boolean z = true;
        boolean isDisconnected = overallState == State.IDLE || overallState == State.FAILED;
        if (this.mPhone instanceof GsmCdmaPhone) {
            ((GsmCdmaPhone) this.mPhone).updateCurrentCarrierInProvider();
        }
        log("onApnChanged: createAllApnList and cleanUpAllConnections");
        createAllApnList();
        setInitialAttachApn();
        ApnSetting mCurPreApn = getPreferredApn();
        if (this.mPhone.getSubId() == SubscriptionController.getInstance().getDefaultDataSubId() && mCurPreApn == null) {
            if (isDisconnected) {
                z = false;
            }
            cleanUpAllConnections(z, PhoneInternalInterface.REASON_APN_CHANGED);
        } else {
            if (isDisconnected) {
                z = false;
            }
            cleanUpConnectionsOnUpdatedApns(z, PhoneInternalInterface.REASON_APN_CHANGED);
        }
        if (this.mPhone.getSubId() == SubscriptionManager.getDefaultDataSubscriptionId()) {
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_APN_CHANGED);
        }
    }

    private void updateApnId() {
        String is4gSlot;
        String preferredApnIdSlot;
        ContentResolver cr = this.mPhone.getContext().getContentResolver();
        if (this.isMultiSimEnabled) {
            is4gSlot = get4gSlot() == this.mPhone.getSubId() ? "4gSlot" : "2gSlot";
            preferredApnIdSlot = new StringBuilder();
            preferredApnIdSlot.append(PREFERRED_APN_ID);
            preferredApnIdSlot.append(is4gSlot);
            preferredApnIdSlot = preferredApnIdSlot.toString();
        } else {
            preferredApnIdSlot = PREFERRED_APN_ID;
        }
        is4gSlot = preferredApnIdSlot;
        StringBuilder stringBuilder;
        try {
            preferredApnIdSlot = System.getString(cr, is4gSlot);
            this.mCurrentState = getCurState();
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateApnId:LastApnId: ");
            stringBuilder.append(preferredApnIdSlot);
            stringBuilder.append(", CurrentState: ");
            stringBuilder.append(this.mCurrentState);
            stringBuilder.append(", preferredApnIdSlot: ");
            stringBuilder.append(is4gSlot);
            log(stringBuilder.toString());
            if (preferredApnIdSlot != null) {
                String[] ApId = preferredApnIdSlot.split(",");
                ApnSetting CurPreApn = getPreferredApn();
                StringBuffer temApnId = new StringBuffer();
                if (5 != ApId.length || ApId[this.mCurrentState] == null) {
                    System.putString(cr, is4gSlot, this.mDefaultApnId);
                } else {
                    if (CurPreApn == null) {
                        log("updateApnId:CurPreApn: CurPreApn == null");
                        ApId[this.mCurrentState] = ProxyController.MODEM_0;
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateApnId:CurPreApn: ");
                        stringBuilder2.append(CurPreApn);
                        stringBuilder2.append(", CurPreApnId: ");
                        stringBuilder2.append(Integer.toString(CurPreApn.id));
                        log(stringBuilder2.toString());
                        ApId[this.mCurrentState] = Integer.toString(CurPreApn.id);
                    }
                    for (int i = 0; i < ApId.length; i++) {
                        temApnId.append(ApId[i]);
                        if (i != ApId.length - 1) {
                            temApnId.append(",");
                        }
                    }
                    System.putString(cr, is4gSlot, temApnId.toString());
                }
                return;
            }
            System.putString(cr, is4gSlot, this.mDefaultApnId);
        } catch (Exception ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateApnId got exception =");
            stringBuilder.append(ex);
            log(stringBuilder.toString());
            System.putString(cr, is4gSlot, this.mDefaultApnId);
        }
    }

    private DcAsyncChannel findDataConnectionAcByCid(int cid) {
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            if (dcac.getCidSync() == cid) {
                return dcac;
            }
        }
        return null;
    }

    private boolean isHigherPriorityApnContextActive(ApnContext apnContext) {
        if (apnContext.getApnType().equals("ims")) {
            return false;
        }
        Iterator it = this.mPrioritySortedApnContexts.iterator();
        while (it.hasNext()) {
            ApnContext otherContext = (ApnContext) it.next();
            if (!otherContext.getApnType().equals("ims")) {
                if (apnContext.getApnType().equalsIgnoreCase(otherContext.getApnType())) {
                    return false;
                }
                if (otherContext.isEnabled() && otherContext.getState() != State.FAILED) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOnlySingleDcAllowed(int rilRadioTech) {
        int[] singleDcRats = null;
        CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (configManager != null) {
            PersistableBundle bundle = configManager.getConfig();
            if (bundle != null) {
                singleDcRats = bundle.getIntArray("only_single_dc_allowed_int_array");
            }
        }
        boolean onlySingleDcAllowed = false;
        int i = 0;
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("persist.telephony.test.singleDc", false)) {
            onlySingleDcAllowed = true;
        }
        if (singleDcRats != null) {
            while (true) {
                int i2 = i;
                if (i2 >= singleDcRats.length || onlySingleDcAllowed) {
                    break;
                }
                if (rilRadioTech == singleDcRats[i2]) {
                    onlySingleDcAllowed = true;
                }
                i = i2 + 1;
            }
        }
        onlySingleDcAllowed = shouldDisableMultiPdps(onlySingleDcAllowed);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isOnlySingleDcAllowed(");
        stringBuilder.append(rilRadioTech);
        stringBuilder.append("): ");
        stringBuilder.append(onlySingleDcAllowed);
        log(stringBuilder.toString());
        return onlySingleDcAllowed;
    }

    void sendRestartRadio() {
        log("sendRestartRadio:");
        sendMessage(obtainMessage(270362));
    }

    private void restartRadio() {
        log("restartRadio: ************TURN OFF RADIO**************");
        cleanUpAllConnections(true, PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
        this.mPhone.getServiceStateTracker().powerOffRadioSafely(this);
        SystemProperties.set("net.ppp.reset-by-timeout", String.valueOf(Integer.parseInt(SystemProperties.get("net.ppp.reset-by-timeout", ProxyController.MODEM_0)) + 1));
    }

    private boolean retryAfterDisconnected(ApnContext apnContext) {
        if (isDataConnectivityDisabled(this.mPhone.getSubId(), "disable-data")) {
            return false;
        }
        boolean retry = true;
        String reason = apnContext.getReason();
        if (PhoneInternalInterface.REASON_RADIO_TURNED_OFF.equals(reason) || (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) && isHigherPriorityApnContextActive(apnContext))) {
            retry = false;
        }
        if (AbstractPhoneInternalInterface.REASON_NO_RETRY_AFTER_DISCONNECT.equals(reason)) {
            retry = false;
        }
        return retry;
    }

    private void startAlarmForReconnect(long delay, ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("com.android.internal.telephony.data-reconnect.");
        stringBuilder.append(apnType);
        Intent intent = new Intent(stringBuilder.toString());
        intent.addFlags(268435456);
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON, apnContext.getReason());
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE, apnType);
        intent.addFlags(268435456);
        intent.putExtra("subscription", this.mPhone.getSubId());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("startAlarmForReconnect: delay=");
        stringBuilder2.append(delay);
        stringBuilder2.append(" action=");
        stringBuilder2.append(intent.getAction());
        stringBuilder2.append(" apn=");
        stringBuilder2.append(apnContext);
        log(stringBuilder2.toString());
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        apnContext.setReconnectIntent(alarmIntent);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + delay, alarmIntent);
    }

    private void notifyNoData(DcFailCause lastFailCauseCode, ApnContext apnContext) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyNoData: type=");
        stringBuilder.append(apnContext.getApnType());
        log(stringBuilder.toString());
        if (isPermanentFailure(lastFailCauseCode) && !apnContext.getApnType().equals("default")) {
            SystemProperties.set("ril.ps_ce_reason", String.valueOf(lastFailCauseCode.getErrorCode()));
            this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
        }
    }

    public boolean getAutoAttachOnCreation() {
        return this.mAutoAttachOnCreation.get();
    }

    private void onRecordsLoadedOrSubIdChanged() {
        log("onRecordsLoadedOrSubIdChanged: createAllApnList");
        if (HwTelephonyFactory.getHwDataConnectionManager().getNamSwitcherForSoftbank()) {
            HwTelephonyFactory.getHwNetworkManager().setPreferredNetworkTypeForLoaded(this.mPhone, Global.getInt(this.mPhone.getContext().getContentResolver(), "preferred_network_mode", 9));
        }
        this.mAutoAttachOnCreationConfig = this.mPhone.getContext().getResources().getBoolean(17956894);
        updateApnContextState();
        createAllApnList();
        if (isBlockSetInitialAttachApn()) {
            log("onRecordsLoadedOrSubIdChanged: block setInitialAttachApn");
        } else if (!getmIsPseudoImsi()) {
            setInitialAttachApn();
        }
        if (getmIsPseudoImsi()) {
            checkPLMN(this.mHwCustDcTracker.getPlmn());
            log("onRecordsLoaded: createAllApnList --return due to IsPseudoImsi");
            return;
        }
        if (this.mPhone.mCi.getRadioState().isOn()) {
            log("onRecordsLoadedOrSubIdChanged: notifying data availability");
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_SIM_LOADED);
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_SIM_LOADED);
    }

    private void onSetCarrierDataEnabled(AsyncResult ar) {
        StringBuilder stringBuilder;
        if (ar.exception != null) {
            String str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CarrierDataEnable exception: ");
            stringBuilder.append(ar.exception);
            Rlog.e(str, stringBuilder.toString());
            return;
        }
        boolean enabled = ((Boolean) ar.result).booleanValue();
        if (enabled != this.mDataEnabledSettings.isCarrierDataEnabled()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("carrier Action: set metered apns enabled: ");
            stringBuilder.append(enabled);
            log(stringBuilder.toString());
            this.mDataEnabledSettings.setCarrierDataEnabled(enabled);
            if (enabled) {
                this.mPhone.notifyOtaspChanged(this.mPhone.getServiceStateTracker().getOtasp());
                reevaluateDataConnections();
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_DATA_ENABLED);
            } else {
                this.mPhone.notifyOtaspChanged(5);
                cleanUpAllConnections(true, PhoneInternalInterface.REASON_CARRIER_ACTION_DISABLE_METERED_APN);
            }
        }
    }

    private void onSimNotReady() {
        log("onSimNotReady");
        cleanUpAllConnections(true, PhoneInternalInterface.REASON_SIM_NOT_READY);
        this.mAllApnSettings = new ArrayList();
        this.mAutoAttachOnCreationConfig = false;
        this.mAutoAttachOnCreation.set(false);
    }

    private void onSetDependencyMet(String apnType, boolean met) {
        if (!"hipri".equals(apnType)) {
            ApnContext apnContext = (ApnContext) this.mApnContexts.get(apnType);
            if (apnContext == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onSetDependencyMet: ApnContext not found in onSetDependencyMet(");
                stringBuilder.append(apnType);
                stringBuilder.append(", ");
                stringBuilder.append(met);
                stringBuilder.append(")");
                loge(stringBuilder.toString());
                return;
            }
            applyNewState(apnContext, apnContext.isEnabled(), met);
            if ("default".equals(apnType)) {
                apnContext = (ApnContext) this.mApnContexts.get("hipri");
                if (apnContext != null) {
                    applyNewState(apnContext, apnContext.isEnabled(), met);
                }
            }
        }
    }

    public void setPolicyDataEnabled(boolean enabled) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPolicyDataEnabled: ");
        stringBuilder.append(enabled);
        log(stringBuilder.toString());
        Message msg = obtainMessage(270368);
        msg.arg1 = enabled;
        sendMessage(msg);
    }

    private void onSetPolicyDataEnabled(boolean enabled) {
        boolean prevEnabled = isDataEnabled();
        if (this.mDataEnabledSettings.isPolicyDataEnabled() != enabled) {
            this.mDataEnabledSettings.setPolicyDataEnabled(enabled);
            if (prevEnabled == isDataEnabled()) {
                return;
            }
            if (prevEnabled) {
                onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
                return;
            }
            reevaluateDataConnections();
            onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
        }
    }

    private void applyNewState(ApnContext apnContext, boolean enabled, boolean met) {
        boolean cleanup = false;
        boolean trySetup = false;
        String str = new StringBuilder();
        str.append("applyNewState(");
        str.append(apnContext.getApnType());
        str.append(", ");
        str.append(enabled);
        str.append("(");
        str.append(apnContext.isEnabled());
        str.append("), ");
        str.append(met);
        str.append("(");
        str.append(apnContext.getDependencyMet());
        str.append("))");
        str = str.toString();
        log(str);
        apnContext.requestLog(str);
        if (apnContext.isReady()) {
            cleanup = true;
            if (enabled && met) {
                State state = apnContext.getState();
                switch (AnonymousClass7.$SwitchMap$com$android$internal$telephony$DctConstants$State[state.ordinal()]) {
                    case 1:
                    case 2:
                    case 4:
                        log("applyNewState: 'ready' so return");
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("applyNewState state=");
                        stringBuilder.append(state);
                        stringBuilder.append(", so return");
                        apnContext.requestLog(stringBuilder.toString());
                        return;
                    case 3:
                    case 5:
                    case 6:
                    case 7:
                        if (!getCustRetryConfig() || !"mms".equals(apnContext.getApnType())) {
                            trySetup = true;
                            apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
                            break;
                        }
                        log("applyNewState: the mms is retrying,return.");
                        return;
                        break;
                }
            } else if (met) {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DISABLED);
            } else {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DEPENDENCY_UNMET);
            }
        } else if (enabled && met) {
            if (apnContext.isEnabled()) {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DEPENDENCY_MET);
            } else {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
            }
            if (apnContext.getState() == State.FAILED) {
                apnContext.setState(State.IDLE);
            }
            trySetup = true;
        }
        apnContext.setEnabled(enabled);
        apnContext.setDependencyMet(met);
        if (cleanup) {
            cleanUpConnection(true, apnContext);
            if ("default".equals(apnContext.getApnType())) {
                log("applyNewState disable default apncontext, need to reset all param");
                clearRestartRildParam();
            }
        }
        if (trySetup) {
            apnContext.resetErrorCodeRetries();
            trySetupData(apnContext);
        }
    }

    private DcAsyncChannel checkForCompatibleConnectedApnContext(ApnContext apnContext) {
        StringBuilder stringBuilder;
        String apnType = apnContext.getApnType();
        ArrayList<ApnSetting> dunSettings = null;
        ApnSetting bipSetting = null;
        if ("dun".equals(apnType)) {
            dunSettings = sortApnListByPreferred(fetchDunApns());
        }
        if (isBipApnType(apnType)) {
            bipSetting = fetchBipApn(this.mPreferredApn, this.mAllApnSettings);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("checkForCompatibleConnectedApnContext: apnContext=");
        stringBuilder2.append(apnContext);
        log(stringBuilder2.toString());
        DcAsyncChannel potentialDcac = null;
        ApnContext potentialApnCtx = null;
        for (ApnContext curApnCtx : this.mApnContexts.values()) {
            DcAsyncChannel curDcac = curApnCtx.getDcAc();
            if (curDcac != null) {
                ApnSetting apnSetting = curApnCtx.getApnSetting();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("apnSetting: ");
                stringBuilder3.append(apnSetting);
                log(stringBuilder3.toString());
                if (dunSettings == null || dunSettings.size() <= 0) {
                    if (bipSetting == null) {
                        if (apnSetting != null && ((apnContext.getWaitingApns() == null && apnSetting.canHandleType(apnType)) || ((apnContext.getWaitingApns() != null && apnContext.getWaitingApns().contains(apnSetting)) || (this.mHwCustDcTracker != null && this.mHwCustDcTracker.isDocomoTetheringApn(apnSetting, apnType))))) {
                            switch (AnonymousClass7.$SwitchMap$com$android$internal$telephony$DctConstants$State[curApnCtx.getState().ordinal()]) {
                                case 1:
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("checkForCompatibleConnectedApnContext: found canHandle conn=");
                                    stringBuilder.append(curDcac);
                                    stringBuilder.append(" curApnCtx=");
                                    stringBuilder.append(curApnCtx);
                                    log(stringBuilder.toString());
                                    return curDcac;
                                case 2:
                                    if (potentialDcac != null) {
                                        break;
                                    }
                                    potentialDcac = curDcac;
                                    potentialApnCtx = curApnCtx;
                                    break;
                                case 3:
                                case 4:
                                    potentialDcac = curDcac;
                                    potentialApnCtx = curApnCtx;
                                    break;
                                default:
                                    break;
                            }
                        }
                    } else if (bipSetting.equals(apnSetting)) {
                        int i = AnonymousClass7.$SwitchMap$com$android$internal$telephony$DctConstants$State[curApnCtx.getState().ordinal()];
                        if (i != 1) {
                            switch (i) {
                                case 3:
                                case 4:
                                    potentialDcac = curDcac;
                                    potentialApnCtx = curApnCtx;
                                    break;
                                default:
                                    break;
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("checkForCompatibleConnectedApnContext: found bip conn=");
                        stringBuilder.append(curDcac);
                        stringBuilder.append(" curApnCtx=");
                        stringBuilder.append(curApnCtx);
                        log(stringBuilder.toString());
                        return curDcac;
                    } else {
                        continue;
                    }
                } else {
                    Iterator it = dunSettings.iterator();
                    while (it.hasNext()) {
                        if (((ApnSetting) it.next()).equals(apnSetting)) {
                            switch (AnonymousClass7.$SwitchMap$com$android$internal$telephony$DctConstants$State[curApnCtx.getState().ordinal()]) {
                                case 1:
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("checkForCompatibleConnectedApnContext: found dun conn=");
                                    stringBuilder.append(curDcac);
                                    stringBuilder.append(" curApnCtx=");
                                    stringBuilder.append(curApnCtx);
                                    log(stringBuilder.toString());
                                    return curDcac;
                                case 2:
                                    if (potentialDcac != null) {
                                        break;
                                    }
                                    potentialDcac = curDcac;
                                    potentialApnCtx = curApnCtx;
                                    break;
                                case 3:
                                case 4:
                                    potentialDcac = curDcac;
                                    potentialApnCtx = curApnCtx;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    continue;
                }
            } else {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("checkForCompatibleConnectedApnContext: not conn curApnCtx=");
                stringBuilder4.append(curApnCtx);
                log(stringBuilder4.toString());
            }
        }
        if (potentialDcac != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkForCompatibleConnectedApnContext: found potential conn=");
            stringBuilder.append(potentialDcac);
            stringBuilder.append(" curApnCtx=");
            stringBuilder.append(potentialApnCtx);
            log(stringBuilder.toString());
            return potentialDcac;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkForCompatibleConnectedApnContext: NO conn apnContext=");
        stringBuilder.append(apnContext);
        log(stringBuilder.toString());
        return null;
    }

    public void setEnabled(int id, boolean enable) {
        Message msg = obtainMessage(270349);
        msg.arg1 = id;
        msg.arg2 = enable;
        sendMessage(msg);
    }

    public boolean isMpLinkEnable(String ApnType) {
        boolean MplinkEnable = true;
        boolean mlinkCond = System.getInt(this.mPhone.getContext().getContentResolver(), "mplink_db_condition_value", 0) == 1;
        boolean isDefaultApn = "default".equals(ApnType);
        if (!(isDefaultApn && mlinkCond && System.getInt(this.mPhone.getContext().getContentResolver(), "smart_network_switching", 0) == 1)) {
            MplinkEnable = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dct isMpLinkEnable mlinkCond:");
        stringBuilder.append(mlinkCond);
        stringBuilder.append(",isDefaultApn:");
        stringBuilder.append(isDefaultApn);
        stringBuilder.append(",return:");
        stringBuilder.append(MplinkEnable);
        log(stringBuilder.toString());
        return MplinkEnable;
    }

    private void onEnableApn(int apnId, int enabled) {
        ApnContext apnContext = (ApnContext) this.mApnContextsById.get(apnId);
        StringBuilder stringBuilder;
        if (apnContext == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("onEnableApn(");
            stringBuilder.append(apnId);
            stringBuilder.append(", ");
            stringBuilder.append(enabled);
            stringBuilder.append("): NO ApnContext");
            loge(stringBuilder.toString());
            return;
        }
        clearAndResumeNetInfiForWifiLteCoexist(apnId, enabled, apnContext);
        if (isMpLinkEnable(apnContext.getApnType()) && enabled == 0) {
            log("onEnableApn: mplink enable, so return");
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("onEnableApn: apnContext=");
        stringBuilder.append(apnContext);
        stringBuilder.append(" call applyNewState");
        log(stringBuilder.toString());
        boolean z = true;
        if (enabled != 1) {
            z = false;
        }
        applyNewState(apnContext, z, apnContext.getDependencyMet());
        if (enabled == 0 && isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) && !isHigherPriorityApnContextActive(apnContext)) {
            log("onEnableApn: isOnlySingleDcAllowed true & higher priority APN disabled");
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION);
        }
    }

    protected boolean onTrySetupData(String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onTrySetupData: reason=");
        stringBuilder.append(reason);
        log(stringBuilder.toString());
        setupDataOnConnectableApns(reason);
        return true;
    }

    protected boolean onTrySetupData(ApnContext apnContext) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onTrySetupData: apnContext=");
        stringBuilder.append(apnContext);
        log(stringBuilder.toString());
        return trySetupData(apnContext);
    }

    public boolean isUserDataEnabled() {
        if (this.mDataEnabledSettings.isAnySimDetected() || !this.mDataEnabledSettings.isProvisioning()) {
            return this.mDataEnabledSettings.isUserDataEnabled();
        }
        return this.mDataEnabledSettings.isProvisioningDataEnabled();
    }

    public void setDataRoamingEnabledByUser(boolean enabled) {
        int phoneSubId = this.mPhone.getSubId();
        if (getDataRoamingEnabled() != enabled) {
            boolean roaming = enabled;
            if (TelephonyManager.getDefault().getSimCount() == 1) {
                Global.putInt(this.mResolver, "data_roaming", roaming);
                setDataRoamingFromUserAction(true);
            } else {
                Global.putInt(this.mResolver, getDataRoamingSettingItem("data_roaming"), roaming);
            }
            this.mSubscriptionManager.setDataRoaming(roaming, phoneSubId);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDataRoamingEnabledByUser: set phoneSubId=");
            stringBuilder.append(phoneSubId);
            stringBuilder.append(" isRoaming=");
            stringBuilder.append(enabled);
            log(stringBuilder.toString());
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setDataRoamingEnabledByUser: unchanged phoneSubId=");
        stringBuilder2.append(phoneSubId);
        stringBuilder2.append(" isRoaming=");
        stringBuilder2.append(enabled);
        log(stringBuilder2.toString());
    }

    public boolean getDataRoamingEnabled() {
        boolean isDataRoamingEnabled = "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false"));
        int phoneSubId = this.mPhone.getSubId();
        boolean z = true;
        if (VSimUtilsInner.isVSimSub(this.mPhone.getSubId())) {
            return true;
        }
        try {
            if (isNeedDataRoamingExpend()) {
                isDataRoamingEnabled = getDataRoamingEnabledWithNational();
            } else if (TelephonyManager.getDefault().getSimCount() == 1) {
                if (Global.getInt(this.mResolver, "data_roaming", isDataRoamingEnabled) == 0) {
                    z = false;
                }
                isDataRoamingEnabled = z;
            } else {
                if (Global.getInt(this.mResolver, getDataRoamingSettingItem("data_roaming")) == 0) {
                    z = false;
                }
                isDataRoamingEnabled = z;
            }
        } catch (SettingNotFoundException snfe) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDataRoamingEnabled: SettingNofFoundException snfe=");
            stringBuilder.append(snfe);
            log(stringBuilder.toString());
            isDataRoamingEnabled = getDefaultDataRoamingEnabled();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getDataRoamingEnabled: phoneSubId=");
        stringBuilder2.append(phoneSubId);
        stringBuilder2.append(" isDataRoamingEnabled=");
        stringBuilder2.append(isDataRoamingEnabled);
        log(stringBuilder2.toString());
        return isDataRoamingEnabled;
    }

    private boolean getDefaultDataRoamingEnabled() {
        return "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false")) | ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId()).getBoolean("carrier_default_data_roaming_enabled_bool");
    }

    private void setDefaultDataRoamingEnabled() {
        String setting = "data_roaming";
        boolean useCarrierSpecificDefault = false;
        if (TelephonyManager.getDefault().getSimCount() != 1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(setting);
            stringBuilder.append(this.mPhone.getSubId());
            setting = stringBuilder.toString();
            try {
                Global.getInt(this.mResolver, setting);
            } catch (SettingNotFoundException e) {
                useCarrierSpecificDefault = true;
            }
        } else if (!isDataRoamingFromUserAction()) {
            useCarrierSpecificDefault = true;
        }
        if (useCarrierSpecificDefault) {
            boolean defaultVal = getDefaultDataRoamingEnabled();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setDefaultDataRoamingEnabled: ");
            stringBuilder2.append(setting);
            stringBuilder2.append("default value: ");
            stringBuilder2.append(defaultVal);
            log(stringBuilder2.toString());
            Global.putInt(this.mResolver, setting, defaultVal);
            this.mSubscriptionManager.setDataRoaming(defaultVal, this.mPhone.getSubId());
        }
    }

    private boolean isDataRoamingFromUserAction() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        if (!sp.contains(Phone.DATA_ROAMING_IS_USER_SETTING_KEY) && Global.getInt(this.mResolver, "device_provisioned", 0) == 0) {
            sp.edit().putBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, false).commit();
        }
        return sp.getBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, true);
    }

    private void setDataRoamingFromUserAction(boolean isUserAction) {
        PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext()).edit().putBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, isUserAction).commit();
    }

    private void onDataRoamingOff() {
        log("onDataRoamingOff");
        if (processAttDataRoamingOff()) {
            log("process ATT DataRoaming off");
            return;
        }
        if (getDataRoamingEnabled()) {
            notifyDataConnection(PhoneInternalInterface.REASON_ROAMING_OFF);
        } else {
            if (!TextUtils.isEmpty(getOperatorNumeric())) {
                setInitialAttachApn();
            }
            setDataProfilesAsNeeded();
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_OFF);
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_ROAMING_OFF);
        }
    }

    private void onDataRoamingOnOrSettingsChanged(int messageType) {
        log("onDataRoamingOnOrSettingsChanged");
        boolean settingChanged = messageType == 270384;
        if (settingChanged && getDataRoamingEnabled()) {
            this.mHwCustDcTracker.setDataOrRoamOn(1);
        }
        if (processAttDataRoamingOn()) {
            log("process ATT DataRoaming off");
        } else if (this.mPhone.getServiceState().getDataRoaming()) {
            checkDataRoamingStatus(settingChanged);
            if (getDataRoamingEnabled()) {
                log("onDataRoamingOnOrSettingsChanged: setup data on roaming");
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_ROAMING_ON);
                notifyDataConnection(PhoneInternalInterface.REASON_ROAMING_ON);
            } else {
                log("onDataRoamingOnOrSettingsChanged: Tear down data connection on roaming.");
                cleanUpAllConnections(true, PhoneInternalInterface.REASON_ROAMING_ON);
                notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_ON);
            }
        } else {
            log("device is not roaming. ignored the request.");
        }
    }

    private void checkDataRoamingStatus(boolean settingChanged) {
        if (!settingChanged && !getDataRoamingEnabled() && this.mPhone.getServiceState().getDataRoaming()) {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (apnContext.getState() == State.CONNECTED) {
                    LocalLog localLog = this.mDataRoamingLeakageLog;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PossibleRoamingLeakage  connection params: ");
                    stringBuilder.append(apnContext.getDcAc() != null ? apnContext.getDcAc().mLastConnectionParams : "");
                    localLog.log(stringBuilder.toString());
                }
            }
        }
    }

    private void onRadioAvailable() {
        log("onRadioAvailable");
        if (this.mPhone.getSimulatedRadioControl() != null) {
            notifyDataConnection(null);
            log("onRadioAvailable: We're on the simulator; assuming data is connected");
        }
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (r != null && (r.getImsiReady() || r.getRecordsLoaded())) {
            notifyOffApnsOfAvailability(null);
        }
        if (getOverallState() != State.IDLE) {
            cleanUpConnection(true, null);
        }
    }

    private void onRadioOffOrNotAvailable() {
        this.mReregisterOnReconnectFailure = false;
        this.mAutoAttachOnCreation.set(false);
        this.mIsPsRestricted = false;
        this.mPhone.getServiceStateTracker().mRestrictedState.setPsRestricted(false);
        if (this.mPhone.getSimulatedRadioControl() != null) {
            log("We're on the simulator; assuming radio off is meaningless");
        } else {
            log("onRadioOffOrNotAvailable: is off and clean up all connections");
            cleanUpAllConnections(false, PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
        }
        notifyOffApnsOfAvailability(null);
    }

    private void completeConnection(ApnContext apnContext) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("completeConnection: successful, notify the world apnContext=");
        stringBuilder.append(apnContext);
        log(stringBuilder.toString());
        if (this.mIsProvisioning && !TextUtils.isEmpty(this.mProvisioningUrl)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("completeConnection: MOBILE_PROVISIONING_ACTION url=");
            stringBuilder.append(this.mProvisioningUrl);
            log(stringBuilder.toString());
            Intent newIntent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER");
            newIntent.setData(Uri.parse(this.mProvisioningUrl));
            newIntent.setFlags(272629760);
            try {
                this.mPhone.getContext().startActivity(newIntent);
            } catch (ActivityNotFoundException e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("completeConnection: startActivityAsUser failed");
                stringBuilder2.append(e);
                loge(stringBuilder2.toString());
            }
        }
        this.mIsProvisioning = false;
        this.mProvisioningUrl = null;
        if (this.mProvisioningSpinner != null) {
            sendMessage(obtainMessage(270378, this.mProvisioningSpinner));
        }
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        startNetStatPoll();
        startDataStallAlarm(false);
    }

    private boolean needSetCTProxy(ApnSetting apn) {
        boolean needSet = false;
        if (!isCTSimCard(this.mPhone.getPhoneId())) {
            return false;
        }
        String networkOperatorNumeric = this.mPhone.getServiceState().getOperatorNumeric();
        if (!(apn == null || apn.apn == null || !apn.apn.contains(CT_NOT_ROAMING_APN_PREFIX) || networkOperatorNumeric == null || !"46012".equals(networkOperatorNumeric))) {
            needSet = true;
        }
        return needSet;
    }

    /* JADX WARNING: Missing block: B:16:0x003e, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean needRestartRadioOnError(ApnContext apnContext, DcFailCause cause) {
        TelephonyManager tm = TelephonyManager.getDefault();
        if (apnContext == null || tm == null || !"default".equals(apnContext.getApnType()) || tm.getCallState() != 0 || get4gSlot() != this.mPhone.getSubId() || !PERMANENT_ERROR_HEAL_ENABLED || !apnContext.restartOnError(cause.getErrorCode())) {
            return false;
        }
        log("needRestartRadioOnError return true");
        return true;
    }

    private void onDataSetupComplete(AsyncResult ar) {
        StringBuilder stringBuilder;
        DcFailCause cause = DcFailCause.UNKNOWN;
        boolean handleError = false;
        ApnContext apnContext = getValidApnContext(ar, "onDataSetupComplete");
        if (apnContext != null) {
            boolean isDefault = "default".equals(apnContext.getApnType());
            StringBuilder stringBuilder2;
            StringBuilder stringBuilder3;
            if (ar.exception == null) {
                DcAsyncChannel dcac = apnContext.getDcAc();
                if (dcac == null) {
                    log("onDataSetupComplete: no connection to DC, handle as error");
                    cause = DcFailCause.CONNECTION_TO_DATACONNECTIONAC_BROKEN;
                    handleError = true;
                } else {
                    String port;
                    StringBuilder stringBuilder4;
                    addIfacePhoneHashMap(dcac, mIfacePhoneHashMap);
                    if (isClearCodeEnabled() && isDefault) {
                        resetTryTimes();
                    }
                    ApnSetting apn = apnContext.getApnSetting();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onDataSetupComplete: success apn=");
                    stringBuilder2.append(apn == null ? "unknown" : apn.apn);
                    log(stringBuilder2.toString());
                    if (isDefault) {
                        SystemProperties.set("gsm.default.apn", apn == null ? "" : apn.apn);
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("gsm.default.apn: ");
                        stringBuilder2.append(SystemProperties.get("gsm.default.apn"));
                        log(stringBuilder2.toString());
                    }
                    if (needSetCTProxy(apn)) {
                        try {
                            dcac.setLinkPropertiesHttpProxySync(new ProxyInfo("10.0.0.200", Integer.parseInt("80"), "127.0.0.1"));
                        } catch (NumberFormatException e) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onDataSetupComplete: NumberFormatException making ProxyProperties (");
                            stringBuilder.append(apn.apn);
                            stringBuilder.append("): ");
                            stringBuilder.append(e);
                            loge(stringBuilder.toString());
                        }
                    } else if (!(apn == null || apn.proxy == null || apn.proxy.length() == 0)) {
                        try {
                            port = apn.port;
                            if (TextUtils.isEmpty(port)) {
                                port = "8080";
                            }
                            dcac.setLinkPropertiesHttpProxySync(new ProxyInfo(apn.proxy, Integer.parseInt(port), "127.0.0.1"));
                        } catch (NumberFormatException e2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onDataSetupComplete: NumberFormatException making ProxyProperties (");
                            stringBuilder.append(apn.port);
                            stringBuilder.append("): ");
                            stringBuilder.append(e2);
                            loge(stringBuilder.toString());
                        }
                    }
                    if (TextUtils.equals(apnContext.getApnType(), "default")) {
                        try {
                            SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "true");
                        } catch (RuntimeException e3) {
                            log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to true");
                        }
                        if (apn != null) {
                            port = getOpKeyByActivedApn(apn.numeric, apn.apn, apn.user);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("onDataSetupComplete: activedApnVnkey = ");
                            stringBuilder.append(port);
                            log(stringBuilder.toString());
                            setApnOpkeyToSettingsDB(port);
                        }
                        if (this.mCanSetPreferApn && this.mPreferredApn == null) {
                            log("onDataSetupComplete: PREFERRED APN is null");
                            this.mPreferredApn = apn;
                            if (this.mPreferredApn != null) {
                                setPreferredApn(this.mPreferredApn.id);
                            }
                        }
                    } else {
                        try {
                            SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                        } catch (RuntimeException e4) {
                            log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
                        }
                    }
                    apnContext.setPdpFailCause(DcFailCause.NONE);
                    apnContext.setState(State.CONNECTED);
                    checkDataRoamingStatus(false);
                    boolean isProvApn = apnContext.isProvisioningApn();
                    ConnectivityManager cm = ConnectivityManager.from(this.mPhone.getContext());
                    if (this.mProvisionBroadcastReceiver != null) {
                        this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
                        this.mProvisionBroadcastReceiver = null;
                    }
                    if (!isProvApn || this.mIsProvisioning) {
                        cm.setProvisioningNotificationVisible(false, 0, this.mProvisionActionName);
                        completeConnection(apnContext);
                    } else {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("onDataSetupComplete: successful, BUT send connected to prov apn as mIsProvisioning:");
                        stringBuilder4.append(this.mIsProvisioning);
                        stringBuilder4.append(" == false && (isProvisioningApn:");
                        stringBuilder4.append(isProvApn);
                        stringBuilder4.append(" == true");
                        log(stringBuilder4.toString());
                        this.mProvisionBroadcastReceiver = new ProvisionNotificationBroadcastReceiver(cm.getMobileProvisioningUrl(), TelephonyManager.getDefault().getNetworkOperatorName());
                        this.mPhone.getContext().registerReceiver(this.mProvisionBroadcastReceiver, new IntentFilter(this.mProvisionActionName));
                        cm.setProvisioningNotificationVisible(true, 0, this.mProvisionActionName);
                        setRadio(false);
                    }
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("onDataSetupComplete: SETUP complete type=");
                    stringBuilder4.append(apnContext.getApnType());
                    stringBuilder4.append(", reason:");
                    stringBuilder4.append(apnContext.getReason());
                    log(stringBuilder4.toString());
                    if (Build.IS_DEBUGGABLE) {
                        int pcoVal = SystemProperties.getInt("persist.radio.test.pco", -1);
                        if (pcoVal != -1) {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("PCO testing: read pco value from persist.radio.test.pco ");
                            stringBuilder3.append(pcoVal);
                            log(stringBuilder3.toString());
                            byte[] value = new byte[]{(byte) pcoVal};
                            Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_PCO_VALUE");
                            intent.putExtra("apnType", "default");
                            intent.putExtra("apnProto", "IPV4V6");
                            intent.putExtra("pcoId", 65280);
                            intent.putExtra("pcoValue", value);
                            this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
                        }
                    }
                    clearRestartRildParam();
                    openServiceStart(this.mUiccController);
                    setFirstTimeEnableData();
                    log("CHR inform CHR the APN info when data setup succ");
                    LinkProperties chrLinkProperties = getLinkProperties(apnContext.getApnType());
                    if (chrLinkProperties != null) {
                        HwTelephonyFactory.getHwDataServiceChrManager().sendIntentWhenDataConnected(this.mPhone, apn, chrLinkProperties);
                    }
                    getNetdPid();
                }
            } else {
                cause = ar.result;
                ApnSetting apn2 = apnContext.getApnSetting();
                String str = "onDataSetupComplete: error apn=%s cause=%s";
                Object[] objArr = new Object[2];
                objArr[0] = apn2 == null ? "unknown" : apn2.apn;
                objArr[1] = cause;
                log(String.format(str, objArr));
                sendDSMipErrorBroadcast();
                if (cause.isEventLoggable()) {
                    int cid = getCellLocationId();
                    EventLog.writeEvent(EventLogTags.PDP_SETUP_FAIL, new Object[]{Integer.valueOf(cause.ordinal()), Integer.valueOf(cid), Integer.valueOf(TelephonyManager.getDefault().getNetworkType())});
                }
                apn2 = apnContext.getApnSetting();
                this.mPhone.notifyPreciseDataConnectionFailed(apnContext.getReason(), apnContext.getApnType(), apn2 != null ? apn2.apn : "unknown", cause.toString());
                Intent intent2 = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_REQUEST_NETWORK_FAILED");
                intent2.putExtra("errorCode", cause.getErrorCode());
                intent2.putExtra("apnType", apnContext.getApnType());
                this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent2);
                long now = SystemClock.elapsedRealtime();
                if ((cause.isRestartRadioFail(this.mPhone.getContext(), this.mPhone.getSubId()) || needRestartRadioOnError(apnContext, cause)) && (now - this.lastRadioResetTimestamp > ((long) RESTART_RADIO_PUNISH_TIME_IN_MS) || 0 == this.lastRadioResetTimestamp)) {
                    this.lastRadioResetTimestamp = SystemClock.elapsedRealtime();
                    log("Modem restarted.");
                    sendRestartRadio();
                }
                if (isClearCodeEnabled()) {
                    long delay = apnContext.getDelayForNextApn(this.mFailFast);
                    String str2 = LOG_TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("clearcode onDataSetupComplete delay=");
                    stringBuilder3.append(delay);
                    Rlog.d(str2, stringBuilder3.toString());
                    operateClearCodeProcess(apnContext, cause, (int) delay);
                } else if (isPermanentFailure(cause)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("cause = ");
                    stringBuilder2.append(cause);
                    stringBuilder2.append(", mark apn as permanent failed. apn = ");
                    stringBuilder2.append(apn2);
                    log(stringBuilder2.toString());
                    apnContext.markApnPermanentFailed(apn2);
                }
                handleError = true;
            }
            if (handleError) {
                onDataSetupCompleteError(ar);
            }
            if (!this.mDataEnabledSettings.isInternalDataEnabled()) {
                cleanUpAllConnections(PhoneInternalInterface.REASON_DATA_DISABLED);
            }
        }
    }

    private ApnContext getValidApnContext(AsyncResult ar, String logString) {
        if (ar != null && (ar.userObj instanceof Pair)) {
            Pair<ApnContext, Integer> pair = ar.userObj;
            ApnContext apnContext = pair.first;
            if (apnContext != null) {
                int generation = apnContext.getConnectionGeneration();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getValidApnContext (");
                stringBuilder.append(logString);
                stringBuilder.append(") on ");
                stringBuilder.append(apnContext);
                stringBuilder.append(" got ");
                stringBuilder.append(generation);
                stringBuilder.append(" vs ");
                stringBuilder.append(pair.second);
                log(stringBuilder.toString());
                if (generation == ((Integer) pair.second).intValue()) {
                    return apnContext;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("ignoring obsolete ");
                stringBuilder.append(logString);
                log(stringBuilder.toString());
                return null;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(logString);
        stringBuilder2.append(": No apnContext");
        throw new RuntimeException(stringBuilder2.toString());
    }

    private void onDataSetupCompleteError(AsyncResult ar) {
        ApnContext apnContext = getValidApnContext(ar, "onDataSetupCompleteError");
        if (apnContext != null) {
            StringBuilder stringBuilder;
            if (apnContext.isLastApnSetting()) {
                onAllApnFirstActiveFailed();
                if (SystemProperties.getBoolean("ro.config.hw_enable_ota_bip_lgu", false) && "bip0".equals(apnContext.getApnType())) {
                    Intent intent = new Intent();
                    intent.setAction(AbstractPhoneInternalInterface.OTA_OPEN_SERVICE_ACTION);
                    intent.putExtra(AbstractPhoneInternalInterface.OTA_TAG, 1);
                    this.mPhone.getContext().sendBroadcast(intent);
                    if (hasMessages(270386)) {
                        removeMessages(270386);
                    }
                    log("sendbroadcast OTA_OPEN_SERVICE_ACTION");
                    return;
                }
            }
            if (HwTelephonyFactory.getHwPhoneManager().isSupportOrangeApn(this.mPhone)) {
                HwTelephonyFactory.getHwPhoneManager().addSpecialAPN(this.mPhone);
                Rlog.d(LOG_TAG, "onDataSetupCompleteError.addSpecialAPN()");
            }
            long delay = apnContext.getDelayForNextApn(this.mFailFast);
            if (isPSClearCodeRplmnMatched()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("PSCLEARCODE retry APN. old delay = ");
                stringBuilder.append(delay);
                log(stringBuilder.toString());
                delay = updatePSClearCodeApnContext(ar, apnContext, delay);
            }
            if (delay >= 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onDataSetupCompleteError: Try next APN. delay = ");
                stringBuilder.append(delay);
                log(stringBuilder.toString());
                if (isClearCodeEnabled()) {
                    setCurFailCause(ar);
                }
                apnContext.setState(State.SCANNING);
                if (isClearCodeEnabled()) {
                    delay = (long) getDelayTime();
                }
                startAlarmForReconnect(delay, apnContext);
                if (!(apnContext.getReason() == null || apnContext.getApnSetting() == null || apnContext.getApnType() == null)) {
                    HwTelephonyFactory.getHwDataServiceChrManager().sendIntentDataConnectionSetupResult(this.mPhone.getSubId(), DataState.DISCONNECTED.toString(), apnContext.getReason(), apnContext.getApnSetting().apn, apnContext.getApnType(), getLinkProperties(apnContext.getApnType()));
                }
            } else {
                onAllApnPermActiveFailed();
                apnContext.setState(State.FAILED);
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_APN_FAILED, apnContext.getApnType());
                apnContext.setDataConnectionAc(null);
                log("onDataSetupCompleteError: Stop retrying APNs.");
            }
        }
    }

    private void onDataConnectionRedirected(String redirectUrl) {
        if (!TextUtils.isEmpty(redirectUrl)) {
            Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED");
            intent.putExtra("redirectionUrl", redirectUrl);
            this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Notify carrier signal receivers with redirectUrl: ");
            stringBuilder.append(redirectUrl);
            log(stringBuilder.toString());
        }
    }

    private void onDisconnectDone(AsyncResult ar) {
        ApnContext apnContext = getValidApnContext(ar, "onDisconnectDone");
        if (apnContext != null) {
            StringBuilder stringBuilder;
            if (apnContext.getState() == State.CONNECTING) {
                DcAsyncChannel dcac = apnContext.getDcAc();
                if (!(dcac == null || dcac.isInactiveSync() || !dcac.checkApnContextSync(apnContext))) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onDisconnectDone: apnContext is activating, ignore ");
                    stringBuilder.append(apnContext);
                    loge(stringBuilder.toString());
                    return;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onDisconnectDone: EVENT_DISCONNECT_DONE apnContext=");
            stringBuilder2.append(apnContext);
            log(stringBuilder2.toString());
            apnContext.setState(State.IDLE);
            if ("default".equals(apnContext.getApnType())) {
                SystemProperties.set("gsm.default.apn", "");
                stringBuilder = new StringBuilder();
                stringBuilder.append("gsm.default.apn: ");
                stringBuilder.append(SystemProperties.get("gsm.default.apn"));
                log(stringBuilder.toString());
            }
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            if (this.mCdmaPsRecoveryEnabled && getOverallState() != State.CONNECTED) {
                stopPdpResetAlarm();
            }
            if (isDisconnected()) {
                if (this.mPhone.getServiceStateTracker().processPendingRadioPowerOffAfterDataOff()) {
                    log("onDisconnectDone: radio will be turned off, no retries");
                    apnContext.setApnSetting(null);
                    apnContext.setDataConnectionAc(null);
                    if (this.mDisconnectPendingCount > 0) {
                        this.mDisconnectPendingCount--;
                    }
                    if (this.mDisconnectPendingCount == 0) {
                        notifyDataDisconnectComplete();
                        notifyAllDataDisconnected();
                    }
                    return;
                }
                log("data is disconnected and check if need to setPreferredNetworkType");
                ServiceStateTracker sst = this.mPhone.getServiceStateTracker();
                if (sst != null) {
                    HwTelephonyFactory.getHwNetworkManager().checkAndSetNetworkType(sst, this.mPhone);
                }
                if (!(sst == null || sst.returnObject() == null || (!sst.returnObject().isDataOffForbidLTE() && !sst.returnObject().isDataOffbyRoamAndData()))) {
                    log("DCT onDisconnectDone");
                    sst.returnObject().processEnforceLTENetworkTypePending();
                }
            }
            if (this.mAttached.get() && apnContext.isReady() && retryAfterDisconnected(apnContext)) {
                try {
                    SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                } catch (RuntimeException e) {
                    log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
                }
                log("onDisconnectDone: attached, ready and retry after disconnect");
                long delay = apnContext.getRetryAfterDisconnectDelay();
                if (SystemProperties.getLong("persist.radio.telecom_apn_delay", 0) <= 0) {
                    StringBuilder stringBuilder3;
                    if (this.mIsScreenOn && (DcFailCause.LOST_CONNECTION.toString().equals(apnContext.getReason()) || PhoneInternalInterface.REASON_PDP_RESET.equals(apnContext.getReason()))) {
                        delay = 50;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(apnContext.getReason());
                        stringBuilder3.append(" reduce the delay time to ");
                        stringBuilder3.append(50);
                        log(stringBuilder3.toString());
                    }
                    if (isCTSimCard(this.mPhone.getPhoneId()) && PhoneInternalInterface.REASON_NW_TYPE_CHANGED.equals(apnContext.getReason())) {
                        delay = 50;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("NW_TYPE_CHANGED & CTSIM reduce delay to ");
                        stringBuilder3.append(50);
                        log(stringBuilder3.toString());
                    }
                }
                if (delay > 0) {
                    startAlarmForReconnect(delay, apnContext);
                }
            } else {
                boolean restartRadioAfterProvisioning = this.mPhone.getContext().getResources().getBoolean(17957008);
                if (apnContext.isProvisioningApn() && restartRadioAfterProvisioning) {
                    log("onDisconnectDone: restartRadio after provisioning");
                    restartRadio();
                }
                apnContext.setApnSetting(null);
                apnContext.setDataConnectionAc(null);
                if (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology())) {
                    log("onDisconnectDone: isOnlySigneDcAllowed true so setup single apn");
                    if (AbstractPhoneInternalInterface.REASON_NO_RETRY_AFTER_DISCONNECT.equals(apnContext.getReason())) {
                        setupDataOnConnectableApns(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION, apnContext.getApnType());
                    } else {
                        setupDataOnConnectableApns(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION);
                    }
                } else if (this.mCdmaPsRecoveryEnabled && this.mPhone.getServiceState().getVoiceRegState() == 0 && retryAfterDisconnected(apnContext)) {
                    log("onDisconnectDone: cdma cs attached, retry after disconnect");
                    startAlarmForReconnect(5000, apnContext);
                } else {
                    log("onDisconnectDone: not retrying");
                }
            }
            if (this.mDisconnectPendingCount > 0) {
                this.mDisconnectPendingCount--;
            }
            if (this.mDisconnectPendingCount == 0) {
                apnContext.setConcurrentVoiceAndDataAllowed(this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
                notifyDataDisconnectComplete();
                notifyAllDataDisconnected();
            }
        }
    }

    private void onDisconnectDcRetrying(AsyncResult ar) {
        ApnContext apnContext = getValidApnContext(ar, "onDisconnectDcRetrying");
        if (apnContext != null) {
            apnContext.setState(State.RETRYING);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDisconnectDcRetrying: apnContext=");
            stringBuilder.append(apnContext);
            log(stringBuilder.toString());
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        }
    }

    private void onVoiceCallStarted() {
        log("onVoiceCallStarted");
        this.mInVoiceCall = true;
        if (isConnected() && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            log("onVoiceCallStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection(PhoneInternalInterface.REASON_VOICE_CALL_STARTED);
        }
    }

    private void onVoiceCallEnded() {
        log("onVoiceCallEnded");
        this.mInVoiceCall = false;
        if (isConnected()) {
            if (this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                resetPollStats();
            } else {
                startNetStatPoll();
                startDataStallAlarm(false);
                notifyDataConnection(PhoneInternalInterface.REASON_VOICE_CALL_ENDED);
            }
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_VOICE_CALL_ENDED);
    }

    protected void onVpStatusChanged(AsyncResult ar) {
        log("onVpStatusChanged");
        if (ar.exception != null) {
            log("Exception occurred, failed to report the rssi and ecio.");
            return;
        }
        this.mVpStatus = ((Integer) ar.result).intValue();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onVpStatusChanged, mVpStatus:");
        stringBuilder.append(this.mVpStatus);
        log(stringBuilder.toString());
        if (1 == this.mVpStatus) {
            onVPStarted();
        } else {
            onVPEnded();
        }
    }

    public void onVPStarted() {
        log("onVPStarted");
        this.mPhone.getServiceStateTracker().setCurrent3GPsCsAllowed(false);
        if (isConnected() && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed() && this.mInVoiceCall) {
            log("onVPStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection(PhoneInternalInterface.REASON_VP_STARTED);
        }
    }

    public void onVPEnded() {
        log("onVPEnded");
        if (!this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            this.mPhone.getServiceStateTracker().setCurrent3GPsCsAllowed(true);
            if (isConnected() && this.mInVoiceCall) {
                startNetStatPoll();
                startDataStallAlarm(false);
                synchronized (this.mDataEnabledSettings) {
                    if (this.mDataEnabledSettings.isInternalDataEnabled() && this.mDataEnabledSettings.isUserDataEnabled()) {
                        if (this.mDataEnabledSettings.isPolicyDataEnabled()) {
                            notifyDataConnection(PhoneInternalInterface.REASON_VP_ENDED);
                        }
                    }
                    onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_DISABLED);
                }
            }
        }
    }

    private void onCleanUpConnection(boolean tearDown, int apnId, String reason) {
        log("onCleanUpConnection");
        ApnContext apnContext = (ApnContext) this.mApnContextsById.get(apnId);
        if (apnContext != null) {
            apnContext.setReason(reason);
            cleanUpConnection(tearDown, apnContext);
        }
    }

    private boolean isConnected() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getState() == State.CONNECTED) {
                return true;
            }
        }
        return false;
    }

    public boolean isDisconnected() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!apnContext.isDisconnected()) {
                return false;
            }
        }
        return true;
    }

    protected void notifyDataConnection(String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyDataConnection: reason=");
        stringBuilder.append(reason);
        log(stringBuilder.toString());
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady()) {
                String str;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("notifyDataConnection: type:");
                stringBuilder2.append(apnContext.getApnType());
                log(stringBuilder2.toString());
                Phone phone = this.mPhone;
                if (reason != null) {
                    str = reason;
                } else {
                    str = apnContext.getReason();
                }
                phone.notifyDataConnection(str, apnContext.getApnType());
            }
        }
        notifyOffApnsOfAvailability(reason);
    }

    private void setDataProfilesAsNeeded() {
        log("setDataProfilesAsNeeded");
        if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            ArrayList<DataProfile> dps = new ArrayList();
            Iterator it = this.mAllApnSettings.iterator();
            while (it.hasNext()) {
                ApnSetting apn = (ApnSetting) it.next();
                if (apn.modemCognitive) {
                    DataProfile dp = createDataProfile(apn);
                    if (!dps.contains(dp)) {
                        dps.add(dp);
                    }
                }
            }
            if (dps.size() > 0) {
                this.mDataServiceManager.setDataProfile(dps, this.mPhone.getServiceState().getDataRoamingFromRegistration(), null);
            }
        }
    }

    public String getOperatorNumeric() {
        IccRecords r = (IccRecords) this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        if (operator == null) {
            operator = "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getOperatorNumberic - returning from card: ");
        stringBuilder.append(operator);
        log(stringBuilder.toString());
        return operator;
    }

    public String getCTOperator(String operator) {
        if (!isCTSimCard(this.mPhone.getPhoneId())) {
            return operator;
        }
        operator = SystemProperties.get("gsm.national_roaming.apn", "46003");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Select china telecom hplmn: ");
        stringBuilder.append(operator);
        log(stringBuilder.toString());
        return operator;
    }

    private void createAllApnList() {
        StringBuilder stringBuilder;
        this.mMvnoMatched = false;
        this.mAllApnSettings = new ArrayList();
        String operator = getCTOperator(getOperatorNumeric());
        IccRecords record = (IccRecords) this.mIccRecords.get();
        String preSpn = record != null ? record.getServiceProviderName() : "";
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("gsm.sim.preiccid_");
        stringBuilder2.append(this.mPhone.getPhoneId());
        String preIccid = SystemProperties.get(stringBuilder2.toString(), "");
        if ("46003".equals(operator) && (GC_ICCID.equals(preIccid) || GC_SPN.equals(preSpn))) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Hongkong GC card and iccid is: ");
            stringBuilder.append(preIccid);
            stringBuilder.append(",spn is: ");
            stringBuilder.append(preSpn);
            log(stringBuilder.toString());
            operator = GC_MCCMNC;
        }
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(this.mPhone.getSubId()))) {
                operator = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(this.mPhone.getSubId()));
            }
        } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            operator = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
        }
        if (operator != null) {
            Cursor cursor;
            String selection = new StringBuilder();
            selection.append("numeric = '");
            selection.append(operator);
            selection.append("'");
            selection = selection.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("createAllApnList: selection=");
            stringBuilder3.append(selection);
            log(stringBuilder3.toString());
            String subId = Long.toString((long) this.mPhone.getSubId());
            if (this.isMultiSimEnabled) {
                cursor = this.mPhone.getContext().getContentResolver().query(Uri.withAppendedPath(MSIM_TELEPHONY_CARRIERS_URI, subId), null, selection, null, HbpcdLookup.ID);
            } else {
                cursor = this.mPhone.getContext().getContentResolver().query(Carriers.CONTENT_URI, null, selection, null, HbpcdLookup.ID);
            }
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    this.mAllApnSettings = createApnList(cursor);
                }
                cursor.close();
            }
            IccRecords r = (IccRecords) this.mIccRecords.get();
            if (this.mAllApnSettings.isEmpty() && !VSimUtilsInner.isVSimSub(this.mPhone.getPhoneId()) && get4gSlot() == this.mPhone.getSubId() && r != null && true == r.getRecordsLoaded() && operator.length() != 0) {
                HwTelephonyFactory.getHwDataServiceChrManager().sendIntentApnListEmpty(this.mPhone.getSubId());
            }
        }
        if (VSimUtilsInner.isVSimSub(this.mPhone.getPhoneId()) && this.mAllApnSettings.isEmpty()) {
            log("createAllApnList: vsim enabled and apn not in database");
            this.mAllApnSettings = VSimUtilsInner.createVSimApnList();
        }
        addEmergencyApnSetting();
        if (this.mAllApnSettings.isEmpty() || VSimUtilsInner.isVSimSub(this.mPhone.getPhoneId())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("createAllApnList: No APN found for carrier: ");
            stringBuilder.append(operator);
            log(stringBuilder.toString());
            this.mPreferredApn = null;
        } else {
            this.mPreferredApn = getPreferredApn();
            if (this.mPreferredApn == null) {
                ApnSetting apn = getCustPreferredApn(this.mAllApnSettings);
                if (apn != null) {
                    setPreferredApn(apn.id);
                    this.mPreferredApn = getPreferredApn();
                }
            }
            if (!(this.mPreferredApn == null || this.mPreferredApn.numeric.equals(operator))) {
                this.mPreferredApn = null;
                setPreferredApn(-1);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("createAllApnList: mPreferredApn=");
            stringBuilder.append(this.mPreferredApn);
            log(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("createAllApnList: X mAllApnSettings=");
        stringBuilder.append(this.mAllApnSettings);
        log(stringBuilder.toString());
        setDataProfilesAsNeeded();
    }

    private void dedupeApnSettings() {
        ArrayList<ApnSetting> resultApns = new ArrayList();
        for (int i = 0; i < this.mAllApnSettings.size() - 1; i++) {
            ApnSetting first = (ApnSetting) this.mAllApnSettings.get(i);
            int j = i + 1;
            while (j < this.mAllApnSettings.size()) {
                ApnSetting second = (ApnSetting) this.mAllApnSettings.get(j);
                if (first.similar(second)) {
                    ApnSetting newApn = mergeApns(first, second);
                    this.mAllApnSettings.set(i, newApn);
                    first = newApn;
                    this.mAllApnSettings.remove(j);
                } else {
                    j++;
                }
            }
        }
    }

    public ArrayList<ApnSetting> getAllApnList() {
        return this.mAllApnSettings;
    }

    private ApnSetting mergeApns(ApnSetting dest, ApnSetting src) {
        String str;
        ApnSetting apnSetting = dest;
        ApnSetting apnSetting2 = src;
        int id = apnSetting.id;
        ArrayList<String> resultTypes = new ArrayList();
        resultTypes.addAll(Arrays.asList(apnSetting.types));
        int id2 = id;
        for (String srcType : apnSetting2.types) {
            if (!resultTypes.contains(srcType)) {
                resultTypes.add(srcType);
            }
            if (srcType.equals("default")) {
                id2 = apnSetting2.id;
            }
        }
        String mmsc = TextUtils.isEmpty(apnSetting.mmsc) ? apnSetting2.mmsc : apnSetting.mmsc;
        String mmsProxy = TextUtils.isEmpty(apnSetting.mmsProxy) ? apnSetting2.mmsProxy : apnSetting.mmsProxy;
        String mmsPort = TextUtils.isEmpty(apnSetting.mmsPort) ? apnSetting2.mmsPort : apnSetting.mmsPort;
        String proxy = TextUtils.isEmpty(apnSetting.proxy) ? apnSetting2.proxy : apnSetting.proxy;
        String port = TextUtils.isEmpty(apnSetting.port) ? apnSetting2.port : apnSetting.port;
        String protocol = apnSetting2.protocol.equals("IPV4V6") ? apnSetting2.protocol : apnSetting.protocol;
        if (apnSetting2.roamingProtocol.equals("IPV4V6")) {
            str = apnSetting2.roamingProtocol;
        } else {
            str = apnSetting.roamingProtocol;
        }
        String roamingProtocol = str;
        id = (apnSetting.networkTypeBitmask == 0 || apnSetting2.networkTypeBitmask == 0) ? 0 : apnSetting.networkTypeBitmask | apnSetting2.networkTypeBitmask;
        if (id == 0) {
            int bearerBitmask = (apnSetting.bearerBitmask == 0 || apnSetting2.bearerBitmask == 0) ? 0 : apnSetting.bearerBitmask | apnSetting2.bearerBitmask;
            id = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(bearerBitmask);
        }
        String str2 = apnSetting.numeric;
        String str3 = apnSetting.carrier;
        String str4 = apnSetting.apn;
        String str5 = apnSetting.user;
        String str6 = apnSetting.password;
        int i = apnSetting.authType;
        String[] strArr = (String[]) resultTypes.toArray(new String[0]);
        boolean z = apnSetting.carrierEnabled;
        int i2 = apnSetting.profileId;
        boolean z2 = apnSetting.modemCognitive != null || apnSetting2.modemCognitive;
        return new ApnSetting(id2, str2, str3, str4, proxy, port, mmsc, mmsProxy, mmsPort, str5, str6, i, strArr, protocol, roamingProtocol, z, id, i2, z2, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId);
    }

    private DcAsyncChannel createDataConnection() {
        StringBuilder stringBuilder;
        log("createDataConnection E");
        int id = this.mUniqueIdGenerator.getAndIncrement();
        DataConnection conn = DataConnection.makeDataConnection(this.mPhone, id, this, this.mDataServiceManager, this.mDcTesterFailBringUpAll, this.mDcc);
        this.mDataConnections.put(Integer.valueOf(id), conn);
        DcAsyncChannel dcac = new DcAsyncChannel(conn, LOG_TAG);
        int status = dcac.fullyConnectSync(this.mPhone.getContext(), this, conn.getHandler());
        if (status == 0) {
            this.mDataConnectionAcHashMap.put(Integer.valueOf(dcac.getDataConnectionIdSync()), dcac);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("createDataConnection: Could not connect to dcac=");
            stringBuilder.append(dcac);
            stringBuilder.append(" status=");
            stringBuilder.append(status);
            loge(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("createDataConnection() X id=");
        stringBuilder.append(id);
        stringBuilder.append(" dc=");
        stringBuilder.append(conn);
        log(stringBuilder.toString());
        return dcac;
    }

    private void destroyDataConnections() {
        if (this.mDataConnections != null) {
            log("destroyDataConnections: clear mDataConnectionList");
            this.mDataConnections.clear();
            return;
        }
        log("destroyDataConnections: mDataConnecitonList is empty, ignore");
    }

    private ArrayList<ApnSetting> buildWaitingApns(String requestedApnType, int radioTech) {
        ArrayList<ApnSetting> dunApns;
        StringBuilder stringBuilder;
        boolean usePreferred;
        String str = requestedApnType;
        int i = radioTech;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("buildWaitingApns: E requestedApnType=");
        stringBuilder2.append(str);
        log(stringBuilder2.toString());
        ArrayList<ApnSetting> apnList = new ArrayList();
        if (str.equals("dun")) {
            dunApns = fetchDunApns();
            if (dunApns.size() > 0) {
                Iterator it = dunApns.iterator();
                while (it.hasNext()) {
                    apnList.add((ApnSetting) it.next());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("buildWaitingApns: X added APN_TYPE_DUN apnList=");
                    stringBuilder.append(apnList);
                    log(stringBuilder.toString());
                }
                return sortApnListByPreferred(apnList);
            }
        }
        if (isBipApnType(requestedApnType)) {
            ApnSetting bip = fetchBipApn(this.mPreferredApn, this.mAllApnSettings);
            if (bip != null) {
                apnList.add(bip);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("buildWaitingApns: X added APN_TYPE_BIP apnList=");
                stringBuilder3.append(apnList);
                log(stringBuilder3.toString());
                return apnList;
            }
        }
        if (CT_SUPL_FEATURE_ENABLE && "supl".equals(str) && isCTSimCard(this.mPhone.getSubId())) {
            dunApns = buildWaitingApnsForCTSupl(requestedApnType, radioTech);
            if (!dunApns.isEmpty()) {
                return dunApns;
            }
        }
        String operator = getCTOperator(getOperatorNumeric());
        boolean usePreferred2 = true;
        try {
            usePreferred = this.mPhone.getContext().getResources().getBoolean(17956935) ^ true;
        } catch (NotFoundException e) {
            log("buildWaitingApns: usePreferred NotFoundException set to true");
            usePreferred = true;
        }
        if (usePreferred) {
            this.mPreferredApn = getPreferredApn();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("buildWaitingApns: usePreferred=");
        stringBuilder.append(usePreferred);
        stringBuilder.append(" canSetPreferApn=");
        stringBuilder.append(this.mCanSetPreferApn);
        stringBuilder.append(" mPreferredApn=");
        stringBuilder.append(this.mPreferredApn);
        stringBuilder.append(" operator=");
        stringBuilder.append(operator);
        stringBuilder.append(" radioTech=");
        stringBuilder.append(i);
        log(stringBuilder.toString());
        usePreferred2 = true;
        if (this.mHwCustDcTracker != null) {
            usePreferred2 = this.mHwCustDcTracker.isCanHandleType(this.mPreferredApn, str);
        }
        boolean isNeedFilterVowifiMmsForPrefApn = isNeedFilterVowifiMms(this.mPreferredApn, str);
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("buildWaitingApns: isNeedFilterVowifiMmsForPrefApn = ");
        stringBuilder4.append(isNeedFilterVowifiMmsForPrefApn);
        log(stringBuilder4.toString());
        int i2 = 13;
        if (usePreferred && this.mCanSetPreferApn && this.mPreferredApn != null && this.mPreferredApn.canHandleType(str) && isApnCanHandleType && !isNeedFilterVowifiMmsForPrefApn) {
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("buildWaitingApns: Preferred APN:");
            stringBuilder5.append(operator);
            stringBuilder5.append(":");
            stringBuilder5.append(this.mPreferredApn.numeric);
            stringBuilder5.append(":");
            stringBuilder5.append(this.mPreferredApn);
            log(stringBuilder5.toString());
            if (this.mPreferredApn.numeric == null || !this.mPreferredApn.numeric.equals(operator)) {
                log("buildWaitingApns: no preferred APN");
                setPreferredApn(-1);
                this.mPreferredApn = null;
            } else if (isCTSimCard(this.mPhone.getPhoneId()) && isLTENetwork() && isApnPreset(this.mPreferredApn)) {
                if (this.mPreferredApn.bearer == 13 || this.mPreferredApn.bearer == 14) {
                    apnList.add(this.mPreferredApn);
                    return apnList;
                }
            } else if (!ServiceState.bitmaskHasTech(this.mPreferredApn.bearerBitmask, i)) {
                log("buildWaitingApns: no preferred APN");
                setPreferredApn(-1);
                this.mPreferredApn = null;
            } else if (this.mHwCustDcTracker == null || this.mHwCustDcTracker.apnRoamingAdjust(this, this.mPreferredApn, this.mPhone)) {
                apnList.add(this.mPreferredApn);
                apnList = sortApnListByPreferred(apnList);
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("buildWaitingApns: X added preferred apnList=");
                stringBuilder6.append(apnList);
                log(stringBuilder6.toString());
                return apnList;
            }
        }
        String operatorCT = this.mPhone.getServiceState().getOperatorNumeric();
        if (operatorCT == null || "".equals(operatorCT) || !("46003".equals(operatorCT) || "46011".equals(operatorCT) || "46012".equals(operatorCT))) {
        }
        if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty()) {
            loge("mAllApnSettings is null!");
        } else {
            StringBuilder stringBuilder7 = new StringBuilder();
            stringBuilder7.append("buildWaitingApns: mAllApnSettings=");
            stringBuilder7.append(this.mAllApnSettings);
            log(stringBuilder7.toString());
            Iterator it2 = this.mAllApnSettings.iterator();
            while (it2.hasNext()) {
                Object obj;
                ApnSetting apn = (ApnSetting) it2.next();
                usePreferred2 = true;
                if (this.mHwCustDcTracker != null) {
                    usePreferred2 = this.mHwCustDcTracker.isCanHandleType(apn, str);
                }
                boolean isNeedFilterVowifiMms = isNeedFilterVowifiMms(apn, str);
                StringBuilder stringBuilder8 = new StringBuilder();
                stringBuilder8.append("buildWaitingApns: isNeedFilterVowifiMms = ");
                stringBuilder8.append(isNeedFilterVowifiMms);
                log(stringBuilder8.toString());
                if (!apn.canHandleType(str) || !isApnCanHandleType || isNeedFilterVowifiMms) {
                    obj = 14;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("buildWaitingApns: couldn't handle requested ApnType=");
                    stringBuilder4.append(str);
                    log(stringBuilder4.toString());
                } else if (WAP_APN.equals(apn.apn) && isApnPreset(apn) && "default".equals(str)) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("buildWaitingApns: unicom skip add 3gwap for default, ");
                    stringBuilder4.append(apn.toString());
                    log(stringBuilder4.toString());
                    obj = 14;
                } else if (isCTSimCard(this.mPhone.getPhoneId()) && isLTENetwork()) {
                    if (apn.bearer != i2) {
                        obj = 14;
                        if (apn.bearer != 14) {
                        }
                    } else {
                        obj = 14;
                    }
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("buildWaitingApns: adding apn=");
                    stringBuilder4.append(apn.toString());
                    log(stringBuilder4.toString());
                    apnList.add(apn);
                } else {
                    obj = 14;
                    if (!ServiceState.bitmaskHasTech(apn.bearerBitmask, i)) {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("buildWaitingApns: bearerBitmask:");
                        stringBuilder4.append(apn.bearerBitmask);
                        stringBuilder4.append(" or networkTypeBitmask:");
                        stringBuilder4.append(apn.networkTypeBitmask);
                        stringBuilder4.append("do not include radioTech:");
                        stringBuilder4.append(i);
                        log(stringBuilder4.toString());
                    } else if (this.mHwCustDcTracker == null || this.mHwCustDcTracker.apnRoamingAdjust(this, apn, this.mPhone)) {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("buildWaitingApns: adding apn=");
                        stringBuilder4.append(apn);
                        log(stringBuilder4.toString());
                        apnList.add(apn);
                    }
                }
                Object obj2 = obj;
                i2 = 13;
            }
        }
        apnList = sortApnListByPreferred(apnList);
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("buildWaitingApns: ");
        stringBuilder4.append(apnList.size());
        stringBuilder4.append(" APNs in the list: ");
        stringBuilder4.append(apnList);
        log(stringBuilder4.toString());
        return apnList;
    }

    @VisibleForTesting
    public ArrayList<ApnSetting> sortApnListByPreferred(ArrayList<ApnSetting> list) {
        if (list == null || list.size() <= 1) {
            return list;
        }
        final int preferredApnSetId = getPreferredApnSetId();
        if (preferredApnSetId != 0) {
            list.sort(new Comparator<ApnSetting>() {
                public int compare(ApnSetting apn1, ApnSetting apn2) {
                    if (apn1.apnSetId == preferredApnSetId) {
                        return -1;
                    }
                    if (apn2.apnSetId == preferredApnSetId) {
                        return 1;
                    }
                    return 0;
                }
            });
        }
        return list;
    }

    private String apnListToString(ArrayList<ApnSetting> apns) {
        StringBuilder result = new StringBuilder();
        if (apns == null) {
            return null;
        }
        int size = apns.size();
        for (int i = 0; i < size; i++) {
            result.append('[');
            result.append(((ApnSetting) apns.get(i)).toString());
            result.append(']');
        }
        return result.toString();
    }

    private void setPreferredApn(int pos) {
        if (this.mCanSetPreferApn) {
            Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, Long.toString((long) this.mPhone.getSubId()));
            log("setPreferredApn: delete");
            ContentResolver resolver = this.mPhone.getContext().getContentResolver();
            resolver.delete(uri, null, null);
            if (pos >= 0) {
                log("setPreferredApn: insert");
                ContentValues values = new ContentValues();
                values.put(APN_ID, Integer.valueOf(pos));
                resolver.insert(uri, values);
            }
            return;
        }
        log("setPreferredApn: X !canSEtPreferApn");
    }

    private ApnSetting getPreferredApn() {
        if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPreferredApn: mAllApnSettings is ");
            stringBuilder.append(this.mAllApnSettings == null ? "null" : "empty");
            log(stringBuilder.toString());
            return null;
        } else if (needRemovedPreferredApn()) {
            return null;
        } else {
            Cursor cursor = this.mPhone.getContext().getContentResolver().query(Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, Long.toString((long) this.mPhone.getSubId())), new String[]{HbpcdLookup.ID, "name", "apn"}, null, null, "name ASC");
            int i = 0;
            if (cursor != null) {
                this.mCanSetPreferApn = true;
            } else {
                this.mCanSetPreferApn = false;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getPreferredApn: mRequestedApnType=");
            stringBuilder2.append(this.mRequestedApnType);
            stringBuilder2.append(" cursor=");
            stringBuilder2.append(cursor);
            stringBuilder2.append(" cursor.count=");
            if (cursor != null) {
                i = cursor.getCount();
            }
            stringBuilder2.append(i);
            log(stringBuilder2.toString());
            if (this.mCanSetPreferApn && cursor.getCount() > 0) {
                cursor.moveToFirst();
                i = cursor.getInt(cursor.getColumnIndexOrThrow(HbpcdLookup.ID));
                Iterator it = this.mAllApnSettings.iterator();
                while (it.hasNext()) {
                    ApnSetting p = (ApnSetting) it.next();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("getPreferredApn: apnSetting=");
                    stringBuilder3.append(p);
                    log(stringBuilder3.toString());
                    if (p.id == i && p.canHandleType(this.mRequestedApnType)) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("getPreferredApn: X found apnSetting");
                        stringBuilder4.append(p);
                        log(stringBuilder4.toString());
                        cursor.close();
                        return p;
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            log("getPreferredApn: X not found");
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:31:0x00b4, code skipped:
            if (r0.isSoftBankCard(r7.mPhone) != false) goto L_0x05e2;
     */
    /* JADX WARNING: Missing block: B:39:0x00ee, code skipped:
            r7.mPhone.mCi.resetAllConnections();
     */
    /* JADX WARNING: Missing block: B:199:0x052c, code skipped:
            onDataRoamingOnOrSettingsChanged(r8.what);
     */
    /* JADX WARNING: Missing block: B:213:0x0577, code skipped:
            com.android.internal.telephony.HwTelephonyFactory.getHwDataServiceChrManager().setReceivedSimloadedMsg(r7.mPhone, true, r7.mApnContexts, r7.mDataEnabledSettings.isUserDataEnabled());
            r0 = r7.mPhone.getSubId();
     */
    /* JADX WARNING: Missing block: B:214:0x0592, code skipped:
            if (android.telephony.SubscriptionManager.isValidSubscriptionId(r0) == false) goto L_0x0598;
     */
    /* JADX WARNING: Missing block: B:215:0x0594, code skipped:
            onRecordsLoadedOrSubIdChanged();
     */
    /* JADX WARNING: Missing block: B:216:0x0598, code skipped:
            r1 = new java.lang.StringBuilder();
            r1.append("Ignoring EVENT_RECORDS_LOADED as subId is not valid: ");
            r1.append(r0);
            log(r1.toString());
     */
    /* JADX WARNING: Missing block: B:217:0x05ad, code skipped:
            onRadioAvailable();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMessage msg=");
        stringBuilder.append(msg);
        log(stringBuilder.toString());
        beforeHandleMessage(msg);
        int i = msg.what;
        if (i != 69636) {
            boolean z = true;
            switch (i) {
                case 270336:
                    onDataSetupComplete((AsyncResult) msg.obj);
                    break;
                case 270337:
                    break;
                case 270338:
                    break;
                case 270339:
                    if (!(msg.obj instanceof ApnContext)) {
                        if (!(msg.obj instanceof String)) {
                            loge("EVENT_TRY_SETUP request w/o apnContext or String");
                            break;
                        } else {
                            onTrySetupData((String) msg.obj);
                            break;
                        }
                    }
                    onTrySetupData((ApnContext) msg.obj);
                    break;
                default:
                    switch (i) {
                        case 270342:
                            onRadioOffOrNotAvailable();
                            break;
                        case 270343:
                            onVoiceCallStarted();
                            break;
                        case 270344:
                            onVoiceCallEnded();
                            if (mWcdmaVpEnabled) {
                                this.mPhone.getServiceStateTracker().setCurrent3GPsCsAllowed(true);
                                break;
                            }
                            break;
                        case 270345:
                            onDataConnectionDetached();
                            break;
                        default:
                            switch (i) {
                                case 270347:
                                    break;
                                case 270348:
                                    onDataRoamingOff();
                                    break;
                                case 270349:
                                    onEnableApn(msg.arg1, msg.arg2);
                                    break;
                                default:
                                    switch (i) {
                                        case 270351:
                                            log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DONE ");
                                            onDisconnectDone((AsyncResult) msg.obj);
                                            break;
                                        case 270352:
                                            onDataConnectionAttached();
                                            break;
                                        case 270353:
                                            onDataStallAlarm(msg.arg1);
                                            break;
                                        case 270354:
                                            doRecovery();
                                            break;
                                        case 270355:
                                            if (isCTSimCard(this.mPhone.getPhoneId())) {
                                                updateApnId();
                                            }
                                            onApnChanged();
                                            break;
                                        default:
                                            ApnContext apnContext;
                                            boolean tearDown;
                                            StringBuilder stringBuilder2;
                                            switch (i) {
                                                case 270358:
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("EVENT_PS_RESTRICT_ENABLED ");
                                                    stringBuilder.append(this.mIsPsRestricted);
                                                    log(stringBuilder.toString());
                                                    stopNetStatPoll();
                                                    stopDataStallAlarm();
                                                    this.mIsPsRestricted = true;
                                                    break;
                                                case 270359:
                                                    stringBuilder = new StringBuilder();
                                                    stringBuilder.append("EVENT_PS_RESTRICT_DISABLED ");
                                                    stringBuilder.append(this.mIsPsRestricted);
                                                    log(stringBuilder.toString());
                                                    this.mIsPsRestricted = false;
                                                    if (isConnected()) {
                                                        startNetStatPoll();
                                                        startDataStallAlarm(false);
                                                        break;
                                                    }
                                                    if (this.mState == State.FAILED) {
                                                        cleanUpAllConnections(false, PhoneInternalInterface.REASON_PS_RESTRICT_ENABLED);
                                                        this.mReregisterOnReconnectFailure = false;
                                                    }
                                                    apnContext = (ApnContext) this.mApnContextsById.get(0);
                                                    if (apnContext != null) {
                                                        apnContext.setReason(PhoneInternalInterface.REASON_PS_RESTRICT_ENABLED);
                                                        trySetupData(apnContext);
                                                        break;
                                                    }
                                                    loge("**** Default ApnContext not found ****");
                                                    if (Build.IS_DEBUGGABLE) {
                                                        throw new RuntimeException("Default ApnContext not found");
                                                    }
                                                    break;
                                                case 270360:
                                                    if (msg.arg1 == 0) {
                                                        z = false;
                                                    }
                                                    tearDown = z;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("EVENT_CLEAN_UP_CONNECTION tearDown=");
                                                    stringBuilder2.append(tearDown);
                                                    log(stringBuilder2.toString());
                                                    if (!(msg.obj instanceof ApnContext)) {
                                                        onCleanUpConnection(tearDown, msg.arg2, (String) msg.obj);
                                                        break;
                                                    } else {
                                                        cleanUpConnection(tearDown, (ApnContext) msg.obj);
                                                        break;
                                                    }
                                                default:
                                                    switch (i) {
                                                        case 270362:
                                                            restartRadio();
                                                            break;
                                                        case 270363:
                                                            if (msg.arg1 != 1) {
                                                                z = false;
                                                            }
                                                            onSetInternalDataEnabled(z, (Message) msg.obj);
                                                            break;
                                                        default:
                                                            String s;
                                                            StringBuilder stringBuilder3;
                                                            switch (i) {
                                                                case 270365:
                                                                    if (!(msg.obj == null || (msg.obj instanceof String))) {
                                                                        msg.obj = null;
                                                                    }
                                                                    onCleanUpAllConnections((String) msg.obj);
                                                                    break;
                                                                case 270366:
                                                                    if (msg.arg1 != 1) {
                                                                        z = false;
                                                                    }
                                                                    tearDown = z;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("CMD_SET_USER_DATA_ENABLE enabled=");
                                                                    stringBuilder2.append(tearDown);
                                                                    log(stringBuilder2.toString());
                                                                    onSetUserDataEnabled(tearDown);
                                                                    break;
                                                                case 270367:
                                                                    if (msg.arg1 != 1) {
                                                                        z = false;
                                                                    }
                                                                    tearDown = z;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("CMD_SET_DEPENDENCY_MET met=");
                                                                    stringBuilder2.append(tearDown);
                                                                    log(stringBuilder2.toString());
                                                                    Bundle bundle = msg.getData();
                                                                    if (bundle != null) {
                                                                        String apnType = (String) bundle.get("apnType");
                                                                        if (apnType != null) {
                                                                            onSetDependencyMet(apnType, tearDown);
                                                                            break;
                                                                        }
                                                                    }
                                                                    break;
                                                                case 270368:
                                                                    if (msg.arg1 != 1) {
                                                                        z = false;
                                                                    }
                                                                    onSetPolicyDataEnabled(z);
                                                                    break;
                                                                case 270369:
                                                                    onUpdateIcc();
                                                                    break;
                                                                case 270370:
                                                                    log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DC_RETRYING");
                                                                    onDisconnectDcRetrying((AsyncResult) msg.obj);
                                                                    break;
                                                                case 270371:
                                                                    onDataSetupCompleteError((AsyncResult) msg.obj);
                                                                    break;
                                                                case 270372:
                                                                    sEnableFailFastRefCounter += msg.arg1 == 1 ? 1 : -1;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA:  sEnableFailFastRefCounter=");
                                                                    stringBuilder.append(sEnableFailFastRefCounter);
                                                                    log(stringBuilder.toString());
                                                                    if (sEnableFailFastRefCounter < 0) {
                                                                        s = new StringBuilder();
                                                                        s.append("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: sEnableFailFastRefCounter:");
                                                                        s.append(sEnableFailFastRefCounter);
                                                                        s.append(" < 0");
                                                                        loge(s.toString());
                                                                        sEnableFailFastRefCounter = 0;
                                                                    }
                                                                    tearDown = sEnableFailFastRefCounter > 0;
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: enabled=");
                                                                    stringBuilder3.append(tearDown);
                                                                    stringBuilder3.append(" sEnableFailFastRefCounter=");
                                                                    stringBuilder3.append(sEnableFailFastRefCounter);
                                                                    log(stringBuilder3.toString());
                                                                    if (this.mFailFast != tearDown) {
                                                                        this.mFailFast = tearDown;
                                                                        if (tearDown) {
                                                                            z = false;
                                                                        }
                                                                        this.mDataStallDetectionEnabled = z;
                                                                        if (!this.mDataStallDetectionEnabled || getOverallState() != State.CONNECTED || (this.mInVoiceCall && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) {
                                                                            log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: stop data stall");
                                                                            stopDataStallAlarm();
                                                                            break;
                                                                        }
                                                                        log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: start data stall");
                                                                        stopDataStallAlarm();
                                                                        startDataStallAlarm(false);
                                                                        break;
                                                                    }
                                                                    break;
                                                                case 270373:
                                                                    Bundle bundle2 = msg.getData();
                                                                    if (bundle2 != null) {
                                                                        try {
                                                                            this.mProvisioningUrl = (String) bundle2.get("provisioningUrl");
                                                                        } catch (ClassCastException e) {
                                                                            StringBuilder stringBuilder4 = new StringBuilder();
                                                                            stringBuilder4.append("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url not a string");
                                                                            stringBuilder4.append(e);
                                                                            loge(stringBuilder4.toString());
                                                                            this.mProvisioningUrl = null;
                                                                        }
                                                                    }
                                                                    if (!TextUtils.isEmpty(this.mProvisioningUrl)) {
                                                                        StringBuilder stringBuilder5 = new StringBuilder();
                                                                        stringBuilder5.append("CMD_ENABLE_MOBILE_PROVISIONING: provisioningUrl=");
                                                                        stringBuilder5.append(this.mProvisioningUrl);
                                                                        loge(stringBuilder5.toString());
                                                                        this.mIsProvisioning = true;
                                                                        startProvisioningApnAlarm();
                                                                        break;
                                                                    }
                                                                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url is empty, ignoring");
                                                                    this.mIsProvisioning = false;
                                                                    this.mProvisioningUrl = null;
                                                                    break;
                                                                case 270374:
                                                                    int i2;
                                                                    log("CMD_IS_PROVISIONING_APN");
                                                                    s = null;
                                                                    try {
                                                                        boolean isProvApn;
                                                                        Bundle bundle3 = msg.getData();
                                                                        if (bundle3 != null) {
                                                                            s = (String) bundle3.get("apnType");
                                                                        }
                                                                        if (TextUtils.isEmpty(s)) {
                                                                            loge("CMD_IS_PROVISIONING_APN: apnType is empty");
                                                                            isProvApn = false;
                                                                        } else {
                                                                            isProvApn = isProvisioningApn(s);
                                                                        }
                                                                        tearDown = isProvApn;
                                                                    } catch (ClassCastException e2) {
                                                                        loge("CMD_IS_PROVISIONING_APN: NO provisioning url ignoring");
                                                                        tearDown = false;
                                                                    }
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("CMD_IS_PROVISIONING_APN: ret=");
                                                                    stringBuilder3.append(tearDown);
                                                                    log(stringBuilder3.toString());
                                                                    AsyncChannel asyncChannel = this.mReplyAc;
                                                                    if (!tearDown) {
                                                                        i2 = 0;
                                                                    }
                                                                    asyncChannel.replyToMessage(msg, 270374, i2);
                                                                    break;
                                                                case 270375:
                                                                    log("EVENT_PROVISIONING_APN_ALARM");
                                                                    apnContext = (ApnContext) this.mApnContextsById.get(0);
                                                                    if (apnContext.isProvisioningApn() && apnContext.isConnectedOrConnecting()) {
                                                                        if (this.mProvisioningApnAlarmTag != msg.arg1) {
                                                                            stringBuilder2 = new StringBuilder();
                                                                            stringBuilder2.append("EVENT_PROVISIONING_APN_ALARM: ignore stale tag, mProvisioningApnAlarmTag:");
                                                                            stringBuilder2.append(this.mProvisioningApnAlarmTag);
                                                                            stringBuilder2.append(" != arg1:");
                                                                            stringBuilder2.append(msg.arg1);
                                                                            log(stringBuilder2.toString());
                                                                            break;
                                                                        }
                                                                        log("EVENT_PROVISIONING_APN_ALARM: Disconnecting");
                                                                        this.mIsProvisioning = false;
                                                                        this.mProvisioningUrl = null;
                                                                        stopProvisioningApnAlarm();
                                                                        sendCleanUpConnection(true, apnContext);
                                                                        break;
                                                                    }
                                                                    log("EVENT_PROVISIONING_APN_ALARM: Not connected ignore");
                                                                    break;
                                                                    break;
                                                                case 270376:
                                                                    if (msg.arg1 != 1) {
                                                                        if (msg.arg1 == 0) {
                                                                            handleStopNetStatPoll((Activity) msg.obj);
                                                                            break;
                                                                        }
                                                                    }
                                                                    handleStartNetStatPoll((Activity) msg.obj);
                                                                    break;
                                                                    break;
                                                                case 270377:
                                                                    if (this.mPhone.getServiceState().getRilDataRadioTechnology() != 0) {
                                                                        onRatChange();
                                                                        if (!onUpdateIcc()) {
                                                                            if (!isCTSimCard(this.mPhone.getPhoneId())) {
                                                                                for (ApnContext apnContext2 : this.mApnContexts.values()) {
                                                                                    boolean isSetupDataNeeded = (isPermanentFailure(apnContext2.getPdpFailCause()) || DcFailCause.NOT_ALLOWED_RADIO_TECHNOLOGY_IWLAN == apnContext2.getPdpFailCause()) && (apnContext2.getState() == State.FAILED || apnContext2.getState() == State.IDLE);
                                                                                    if (isSetupDataNeeded) {
                                                                                        stringBuilder2 = new StringBuilder();
                                                                                        stringBuilder2.append("tryRestartDataConnections, which reason is ");
                                                                                        stringBuilder2.append(apnContext2.getPdpFailCause());
                                                                                        log(stringBuilder2.toString());
                                                                                        apnContext2.setPdpFailCause(DcFailCause.NONE);
                                                                                        setupDataOnConnectableApns(PhoneInternalInterface.REASON_NW_TYPE_CHANGED);
                                                                                        break;
                                                                                    }
                                                                                }
                                                                                break;
                                                                            }
                                                                        }
                                                                        log("onUpdateIcc: tryRestartDataConnections nwTypeChanged");
                                                                        setupDataOnConnectableApns(PhoneInternalInterface.REASON_NW_TYPE_CHANGED, RetryFailures.ONLY_ON_CHANGE);
                                                                        break;
                                                                    }
                                                                    break;
                                                                case 270378:
                                                                    if (this.mProvisioningSpinner == msg.obj) {
                                                                        this.mProvisioningSpinner.dismiss();
                                                                        this.mProvisioningSpinner = null;
                                                                        break;
                                                                    }
                                                                    break;
                                                                case 270379:
                                                                    onDeviceProvisionedChange();
                                                                    break;
                                                                case 270380:
                                                                    s = msg.obj;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("dataConnectionTracker.handleMessage: EVENT_REDIRECTION_DETECTED=");
                                                                    stringBuilder2.append(s);
                                                                    log(stringBuilder2.toString());
                                                                    onDataConnectionRedirected(s);
                                                                    break;
                                                                case 270381:
                                                                    handlePcoData((AsyncResult) msg.obj);
                                                                    break;
                                                                case 270382:
                                                                    onSetCarrierDataEnabled((AsyncResult) msg.obj);
                                                                    break;
                                                                case 270383:
                                                                    onDataReconnect(msg.getData());
                                                                    break;
                                                                case 270384:
                                                                    break;
                                                                case 270385:
                                                                    onDataServiceBindingChanged(((Boolean) ((AsyncResult) msg.obj).result).booleanValue());
                                                                    break;
                                                                case 270386:
                                                                    onOtaAttachFailed((ApnContext) msg.obj);
                                                                    break;
                                                                default:
                                                                    switch (i) {
                                                                        case 271137:
                                                                            break;
                                                                        case 271138:
                                                                            onDataSetupCompleteFailed();
                                                                            break;
                                                                        case 271139:
                                                                            onPdpResetAlarm(msg.arg1);
                                                                            break;
                                                                        case 271140:
                                                                            if (mWcdmaVpEnabled) {
                                                                                log("EVENT_VP_STATUS_CHANGED");
                                                                                onVpStatusChanged(msg.obj);
                                                                                break;
                                                                            }
                                                                            break;
                                                                        default:
                                                                            switch (i) {
                                                                                case 271144:
                                                                                    if (!checkMvnoParams()) {
                                                                                        HwDataConnectionManager sHwDataConnectionManager = HwTelephonyFactory.getHwDataConnectionManager();
                                                                                        if (sHwDataConnectionManager != null) {
                                                                                            if (sHwDataConnectionManager.getNamSwitcherForSoftbank()) {
                                                                                                break;
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    break;
                                                                                case 271145:
                                                                                    AsyncResult arNvcfg = msg.obj;
                                                                                    if (arNvcfg != null && arNvcfg.exception == null) {
                                                                                        int nvcfgResult = ((Integer) arNvcfg.result).intValue();
                                                                                        String operator = getOperatorNumeric();
                                                                                        StringBuilder stringBuilder6 = new StringBuilder();
                                                                                        stringBuilder6.append("EVENT_UNSOL_SIM_NVCFG_FINISHED: operator=");
                                                                                        stringBuilder6.append(operator);
                                                                                        stringBuilder6.append(", nvcfgResult=");
                                                                                        stringBuilder6.append(nvcfgResult);
                                                                                        log(stringBuilder6.toString());
                                                                                        if (!TextUtils.isEmpty(operator) && 1 == nvcfgResult) {
                                                                                            setInitialAttachApn();
                                                                                            break;
                                                                                        }
                                                                                    }
                                                                                    loge("EVENT_UNSOL_SIM_NVCFG_FINISHED: ar exception.");
                                                                                    break;
                                                                                    break;
                                                                                default:
                                                                                    stringBuilder2 = new StringBuilder();
                                                                                    stringBuilder2.append("Unhandled event=");
                                                                                    stringBuilder2.append(msg);
                                                                                    Rlog.e("DcTracker", stringBuilder2.toString());
                                                                                    break;
                                                                            }
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("DISCONNECTED_CONNECTED: msg=");
        stringBuilder.append(msg);
        log(stringBuilder.toString());
        DcAsyncChannel dcac = msg.obj;
        this.mDataConnectionAcHashMap.remove(Integer.valueOf(dcac.getDataConnectionIdSync()));
        dcac.disconnected();
        handleCustMessage(msg);
    }

    private void onRatChange() {
        if (isCTSimCard(this.mPhone.getPhoneId())) {
            int dataRadioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
            boolean RatChange = ServiceState.isHrpd1X(dataRadioTech) != ServiceState.isHrpd1X(this.preDataRadioTech);
            boolean SetupRatChange = ServiceState.isHrpd1X(dataRadioTech) != ServiceState.isHrpd1X(this.preSetupBasedRadioTech);
            State overallState = getOverallState();
            boolean isConnected = overallState == State.CONNECTED || overallState == State.CONNECTING;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRatChange: preDataRadioTech is: ");
            stringBuilder.append(this.preDataRadioTech);
            stringBuilder.append("; dataRadioTech is: ");
            stringBuilder.append(dataRadioTech);
            log(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("onRatChange: preSetupBasedRadioTech is: ");
            stringBuilder.append(this.preSetupBasedRadioTech);
            stringBuilder.append("; overallState is: ");
            stringBuilder.append(overallState);
            log(stringBuilder.toString());
            if (dataRadioTech != 0) {
                if (this.preDataRadioTech != -1 && RatChange) {
                    if (this.preSetupBasedRadioTech == 0 || SetupRatChange) {
                        if (isConnected) {
                            cleanUpAllConnections(true, PhoneInternalInterface.REASON_NW_TYPE_CHANGED);
                        } else {
                            cleanUpAllConnections(false, PhoneInternalInterface.REASON_NW_TYPE_CHANGED);
                            updateApnContextState();
                        }
                        setupDataOnConnectableApns(PhoneInternalInterface.REASON_NW_TYPE_CHANGED);
                    } else {
                        log("setup data call has been trigger by other flow, have no need to execute again.");
                    }
                }
                this.preDataRadioTech = dataRadioTech;
            }
        }
    }

    public void updateApnContextState() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getState() == State.SCANNING) {
                apnContext.setState(State.IDLE);
                apnContext.setDataConnectionAc(null);
                cancelReconnectAlarm(apnContext);
            }
        }
    }

    private int getApnProfileID(String apnType) {
        if (TextUtils.equals(apnType, "ims") || TextUtils.equals(apnType, "xcap")) {
            return 2;
        }
        if (TextUtils.equals(apnType, "fota")) {
            return 3;
        }
        if (TextUtils.equals(apnType, "cbs")) {
            return 4;
        }
        if (!TextUtils.equals(apnType, "ia") && TextUtils.equals(apnType, "dun")) {
            return 1;
        }
        return 0;
    }

    private int getCellLocationId() {
        CellLocation loc = this.mPhone.getCellLocation();
        if (loc == null) {
            return -1;
        }
        if (loc instanceof GsmCellLocation) {
            return ((GsmCellLocation) loc).getCid();
        }
        if (loc instanceof CdmaCellLocation) {
            return ((CdmaCellLocation) loc).getBaseStationId();
        }
        return -1;
    }

    private IccRecords getUiccRecords(int appFamily) {
        if (VSimUtilsInner.isVSimSub(this.mPhone.getPhoneId())) {
            return VSimUtilsInner.fetchVSimIccRecords(appFamily);
        }
        return this.mUiccController.getIccRecords(this.mPhone.getPhoneId(), appFamily);
    }

    private UiccCardApplication getUiccCardApplication(int appFamily) {
        if (this.mPhone == null) {
            return null;
        }
        if (VSimUtilsInner.isVSimSub(this.mPhone.getPhoneId())) {
            return VSimUtilsInner.getVSimUiccCardApplication(appFamily);
        }
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), appFamily);
    }

    private boolean onUpdateIcc() {
        boolean result = false;
        if (this.mUiccController == null) {
            loge("onUpdateIcc: mUiccController is null. Error!");
            return false;
        }
        int appFamily = 1;
        if (VSimUtilsInner.isVSimPhone(this.mPhone)) {
            appFamily = 1;
        } else if (this.mPhone.getPhoneType() == 1) {
            appFamily = 1;
        } else if (this.mPhone.getPhoneType() == 2) {
            appFamily = 2;
        } else {
            log("Wrong phone type");
        }
        IccRecords newIccRecords = getUiccRecords(appFamily);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onUpdateIcc: newIccRecords ");
        stringBuilder.append(newIccRecords != null ? newIccRecords.getClass().getName() : null);
        log(stringBuilder.toString());
        UiccCardApplication newUiccApplication = getUiccCardApplication(appFamily);
        IccRecords r = (IccRecords) this.mIccRecords.get();
        if (!(this.mUiccApplcation == newUiccApplication && r == newIccRecords)) {
            if (this.mUiccApplcation != null) {
                log("Removing stale icc objects.");
                unregisterForGetAdDone(this.mUiccApplcation);
                if (r != null) {
                    unregisterForImsiReady(r);
                    unregisterForRecordsLoaded(r);
                    this.mIccRecords.set(null);
                }
                this.mUiccApplcation = null;
            }
            if (newUiccApplication == null || newIccRecords == null) {
                onSimNotReady();
            } else if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
                log("New records found");
                this.mUiccApplcation = newUiccApplication;
                this.mIccRecords.set(newIccRecords);
                registerForImsi(newUiccApplication, newIccRecords);
                HwTelephonyFactory.getHwDataServiceChrManager().setRecordsLoadedRegistered(true, this.mPhone.getSubId());
                registerForFdnRecordsLoaded(newIccRecords);
                newIccRecords.registerForRecordsLoaded(this, 270338, null);
            }
            result = true;
        }
        return result;
    }

    public void update() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("update sub = ");
        stringBuilder.append(this.mPhone.getSubId());
        log(stringBuilder.toString());
        log("update(): Active DDS, register for all events now!");
        onUpdateIcc();
        this.mAutoAttachOnCreation.set(false);
        ((GsmCdmaPhone) this.mPhone).updateCurrentCarrierInProvider();
        HwTelephonyFactory.getHwDataServiceChrManager().setCheckApnContextState(false);
    }

    public void updateForVSim() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("vsim update sub = ");
        stringBuilder.append(this.mPhone.getSubId());
        log(stringBuilder.toString());
        unregisterForAllEvents();
        log("update(): Active DDS, register for all events now!");
        registerForAllEvents();
        onUpdateIcc();
        this.mDataEnabledSettings.setUserDataEnabled(true);
    }

    public void cleanUpAllConnections(String cause) {
        cleanUpAllConnections(cause, null);
    }

    public void updateRecords() {
        onUpdateIcc();
    }

    public void cleanUpAllConnections(String cause, Message disconnectAllCompleteMsg) {
        log("cleanUpAllConnections");
        if (disconnectAllCompleteMsg != null) {
            this.mDisconnectAllCompleteMsgList.add(disconnectAllCompleteMsg);
        }
        Message msg = obtainMessage(270365);
        msg.obj = cause;
        sendMessage(msg);
    }

    private void notifyDataDisconnectComplete() {
        log("notifyDataDisconnectComplete");
        Iterator it = this.mDisconnectAllCompleteMsgList.iterator();
        while (it.hasNext()) {
            ((Message) it.next()).sendToTarget();
        }
        this.mDisconnectAllCompleteMsgList.clear();
    }

    private void notifyAllDataDisconnected() {
        sEnableFailFastRefCounter = 0;
        this.mFailFast = false;
        this.mAllDataDisconnectedRegistrants.notifyRegistrants();
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        this.mAllDataDisconnectedRegistrants.addUnique(h, what, obj);
        if (isDisconnected()) {
            log("notify All Data Disconnected");
            notifyAllDataDisconnected();
        }
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        this.mAllDataDisconnectedRegistrants.remove(h);
    }

    public void registerForDataEnabledChanged(Handler h, int what, Object obj) {
        this.mDataEnabledSettings.registerForDataEnabledChanged(h, what, obj);
    }

    public void unregisterForDataEnabledChanged(Handler h) {
        this.mDataEnabledSettings.unregisterForDataEnabledChanged(h);
    }

    private void onSetInternalDataEnabled(boolean enabled, Message onCompleteMsg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetInternalDataEnabled: enabled=");
        stringBuilder.append(enabled);
        log(stringBuilder.toString());
        boolean sendOnComplete = true;
        this.mDataEnabledSettings.setInternalDataEnabled(enabled);
        if (enabled) {
            log("onSetInternalDataEnabled: changed to enabled, try to setup data call");
            onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
        } else {
            sendOnComplete = false;
            log("onSetInternalDataEnabled: changed to disabled, cleanUpAllConnections");
            cleanUpAllConnections(PhoneInternalInterface.REASON_DATA_DISABLED, onCompleteMsg);
        }
        if (sendOnComplete && onCompleteMsg != null) {
            onCompleteMsg.sendToTarget();
        }
    }

    public boolean setInternalDataEnabledFlag(boolean enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setInternalDataEnabledFlag(");
        stringBuilder.append(enable);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stackArray = new Exception().getStackTrace();
        for (StackTraceElement element : stackArray) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(element.toString());
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        log(sb.toString());
        this.mDataEnabledSettings.setInternalDataEnabled(enable);
        return true;
    }

    public boolean setInternalDataEnabled(boolean enable) {
        return setInternalDataEnabled(enable, null);
    }

    public boolean setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setInternalDataEnabled(");
        stringBuilder.append(enable);
        stringBuilder.append(")");
        log(stringBuilder.toString());
        StringBuffer sb = new StringBuffer();
        StackTraceElement[] stackArray = new Exception().getStackTrace();
        for (StackTraceElement element : stackArray) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(element.toString());
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        log(sb.toString());
        Message msg = obtainMessage(270363, onCompleteMsg);
        msg.arg1 = enable;
        sendMessage(msg);
        return true;
    }

    public void setDataAllowed(boolean enable, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDataAllowed: enable=");
        stringBuilder.append(enable);
        log(stringBuilder.toString());
        this.isCleanupRequired.set(enable ^ 1);
        this.mPhone.mCi.setDataAllowed(enable, response);
        this.mDataEnabledSettings.setInternalDataEnabled(enable);
    }

    protected boolean isDefaultDataSubscription() {
        long subId = (long) this.mPhone.getSubId();
        boolean z = true;
        if (VSimUtilsInner.isVSimSub(this.mPhone.getSubId())) {
            return true;
        }
        long defaultDds = (long) SubscriptionController.getInstance().getDefaultDataSubId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isDefaultDataSubscription subId: ");
        stringBuilder.append(subId);
        stringBuilder.append("defaultDds: ");
        stringBuilder.append(defaultDds);
        log(stringBuilder.toString());
        if (subId != defaultDds) {
            z = false;
        }
        return z;
    }

    private void log(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    private void loge(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        pw.println("DcTracker:");
        pw.println(" RADIO_TESTS=false");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDataEnabledSettings=");
        stringBuilder2.append(this.mDataEnabledSettings);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" isDataAllowed=");
        stringBuilder2.append(isDataAllowed(null));
        pw.println(stringBuilder2.toString());
        pw.flush();
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mRequestedApnType=");
        stringBuilder2.append(this.mRequestedApnType);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mPhone=");
        stringBuilder2.append(this.mPhone.getPhoneName());
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mActivity=");
        stringBuilder2.append(this.mActivity);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mState=");
        stringBuilder2.append(this.mState);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mTxPkts=");
        stringBuilder2.append(this.mTxPkts);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mRxPkts=");
        stringBuilder2.append(this.mRxPkts);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mNetStatPollPeriod=");
        stringBuilder2.append(this.mNetStatPollPeriod);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mNetStatPollEnabled=");
        stringBuilder2.append(this.mNetStatPollEnabled);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDataStallTxRxSum=");
        stringBuilder2.append(this.mDataStallTxRxSum);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDataStallAlarmTag=");
        stringBuilder2.append(this.mDataStallAlarmTag);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mDataStallDetectionEnabled=");
        stringBuilder2.append(this.mDataStallDetectionEnabled);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mSentSinceLastRecv=");
        stringBuilder2.append(this.mSentSinceLastRecv);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mNoRecvPollCount=");
        stringBuilder2.append(this.mNoRecvPollCount);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mResolver=");
        stringBuilder2.append(this.mResolver);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mReconnectIntent=");
        stringBuilder2.append(this.mReconnectIntent);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mAutoAttachOnCreation=");
        stringBuilder2.append(this.mAutoAttachOnCreation.get());
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mIsScreenOn=");
        stringBuilder2.append(this.mIsScreenOn);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" mUniqueIdGenerator=");
        stringBuilder2.append(this.mUniqueIdGenerator);
        pw.println(stringBuilder2.toString());
        pw.println(" mDataRoamingLeakageLog= ");
        this.mDataRoamingLeakageLog.dump(fd, pw, args);
        pw.flush();
        pw.println(" ***************************************");
        DcController dcc = this.mDcc;
        if (dcc != null) {
            dcc.dump(fd, pw, args);
        } else {
            pw.println(" mDcc=null");
        }
        pw.println(" ***************************************");
        if (this.mDataConnections != null) {
            Set<Entry<Integer, DataConnection>> mDcSet = this.mDataConnections.entrySet();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" mDataConnections: count=");
            stringBuilder3.append(mDcSet.size());
            pw.println(stringBuilder3.toString());
            for (Entry<Integer, DataConnection> entry : mDcSet) {
                pw.printf(" *** mDataConnection[%d] \n", new Object[]{entry.getKey()});
                ((DataConnection) entry.getValue()).dump(fd, pw, args);
            }
        } else {
            pw.println("mDataConnections=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        HashMap<String, Integer> apnToDcId = this.mApnToDataConnectionId;
        if (apnToDcId != null) {
            Set<Entry<String, Integer>> apnToDcIdSet = apnToDcId.entrySet();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(" mApnToDataConnectonId size=");
            stringBuilder4.append(apnToDcIdSet.size());
            pw.println(stringBuilder4.toString());
            for (Entry<String, Integer> entry2 : apnToDcIdSet) {
                pw.printf(" mApnToDataConnectonId[%s]=%d\n", new Object[]{entry2.getKey(), entry2.getValue()});
            }
        } else {
            pw.println("mApnToDataConnectionId=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        ConcurrentHashMap<String, ApnContext> apnCtxs = this.mApnContexts;
        if (apnCtxs != null) {
            Set<Entry<String, ApnContext>> apnCtxsSet = apnCtxs.entrySet();
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mApnContexts size=");
            stringBuilder.append(apnCtxsSet.size());
            pw.println(stringBuilder.toString());
            for (Entry<String, ApnContext> entry3 : apnCtxsSet) {
                ((ApnContext) entry3.getValue()).dump(fd, pw, args);
            }
            pw.println(" ***************************************");
        } else {
            pw.println(" mApnContexts=null");
        }
        pw.flush();
        ArrayList<ApnSetting> apnSettings = this.mAllApnSettings;
        if (apnSettings != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mAllApnSettings size=");
            stringBuilder.append(apnSettings.size());
            pw.println(stringBuilder.toString());
            for (int i = 0; i < apnSettings.size(); i++) {
                pw.printf(" mAllApnSettings[%d]: %s\n", new Object[]{Integer.valueOf(i), apnSettings.get(i)});
            }
            pw.flush();
        } else {
            pw.println(" mAllApnSettings=null");
        }
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mPreferredApn=");
        stringBuilder5.append(this.mPreferredApn);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mIsPsRestricted=");
        stringBuilder5.append(this.mIsPsRestricted);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mIsDisposed=");
        stringBuilder5.append(this.mIsDisposed);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mIntentReceiver=");
        stringBuilder5.append(this.mIntentReceiver);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mReregisterOnReconnectFailure=");
        stringBuilder5.append(this.mReregisterOnReconnectFailure);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" canSetPreferApn=");
        stringBuilder5.append(this.mCanSetPreferApn);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mApnObserver=");
        stringBuilder5.append(this.mApnObserver);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" getOverallState=");
        stringBuilder5.append(getOverallState());
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mDataConnectionAsyncChannels=%s\n");
        stringBuilder5.append(this.mDataConnectionAcHashMap);
        pw.println(stringBuilder5.toString());
        stringBuilder5 = new StringBuilder();
        stringBuilder5.append(" mAttached=");
        stringBuilder5.append(this.mAttached.get());
        pw.println(stringBuilder5.toString());
        this.mDataEnabledSettings.dump(fd, pw, args);
        pw.flush();
    }

    public String[] getPcscfAddress(String apnType) {
        log("getPcscfAddress()");
        if (apnType == null) {
            log("apnType is null, return null");
            return null;
        }
        ApnContext apnContext;
        if (TextUtils.equals(apnType, "emergency")) {
            apnContext = (ApnContext) this.mApnContextsById.get(9);
        } else if (TextUtils.equals(apnType, "ims")) {
            apnContext = (ApnContext) this.mApnContextsById.get(5);
        } else {
            log("apnType is invalid, return null");
            return null;
        }
        if (apnContext == null) {
            log("apnContext is null, return null");
            return null;
        }
        DcAsyncChannel dcac = apnContext.getDcAc();
        if (dcac == null) {
            return null;
        }
        String[] result = dcac.getPcscfAddr();
        if (result != null) {
            for (String[] result2 = null; result2 < result.length; result2++) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pcscf[");
                stringBuilder.append(result2);
                stringBuilder.append("]: ");
                stringBuilder.append(result[result2]);
                log(stringBuilder.toString());
            }
        }
        return result;
    }

    private void initEmergencyApnSetting() {
        Cursor cursor = this.mPhone.getContext().getContentResolver().query(Uri.withAppendedPath(Carriers.CONTENT_URI, "filtered"), null, "type=\"emergency\"", null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                this.mEmergencyApn = makeApnSetting(cursor);
            }
            cursor.close();
        }
    }

    private void addEmergencyApnSetting() {
        if (!this.mEmergencyApnLoaded) {
            initEmergencyApnSetting();
            this.mEmergencyApnLoaded = true;
        }
        if (this.mEmergencyApn == null) {
            return;
        }
        if (this.mAllApnSettings == null) {
            this.mAllApnSettings = new ArrayList();
            return;
        }
        boolean hasEmergencyApn = false;
        Iterator it = this.mAllApnSettings.iterator();
        while (it.hasNext()) {
            if (ArrayUtils.contains(((ApnSetting) it.next()).types, "emergency")) {
                hasEmergencyApn = true;
                break;
            }
        }
        if (hasEmergencyApn) {
            log("addEmergencyApnSetting - E-APN setting is already present");
        } else {
            this.mAllApnSettings.add(this.mEmergencyApn);
        }
    }

    protected void onDataSetupCompleteFailed() {
        ApnContext apnContext = (ApnContext) this.mApnContexts.get("default");
        long currentTimeMillis = System.currentTimeMillis();
        TelephonyManager tm = TelephonyManager.getDefault();
        if (this.mPhone.getServiceState().getVoiceRegState() != 0 || this.mAttached.get()) {
            log("onDataSetupCompleteFailed, cs out of service || ps in service!");
            return;
        }
        log("onDataSetupCompleteFailed, cs in service & ps out of service!");
        if (apnContext.isReady() && this.mDataEnabledSettings.isInternalDataEnabled() && this.mDataEnabledSettings.isUserDataEnabled()) {
            this.mPdpActFailCount++;
            if (1 == this.mPdpActFailCount) {
                this.mFirstPdpActFailTimestamp = currentTimeMillis;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDataSetupCompleteFailed, mFirstPdpActFailTimestamp ");
            stringBuilder.append(this.mFirstPdpActFailTimestamp);
            stringBuilder.append(", currentTimeMillis ");
            stringBuilder.append(currentTimeMillis);
            stringBuilder.append(", mAttached ");
            stringBuilder.append(this.mAttached);
            stringBuilder.append(", mRestartRildEnabled ");
            stringBuilder.append(this.mRestartRildEnabled);
            stringBuilder.append(", mPdpActFailCount ");
            stringBuilder.append(this.mPdpActFailCount);
            log(stringBuilder.toString());
            if (3 <= this.mPdpActFailCount && currentTimeMillis - this.mFirstPdpActFailTimestamp >= ACTIVE_PDP_FAIL_TO_RESTART_RILD_MILLIS && tm.getCallState(0) == 0 && tm.getCallState(1) == 0 && this.mRestartRildEnabled) {
                this.mPhone.mCi.restartRild(null);
                this.mRestartRildEnabled = false;
            }
        }
    }

    protected void clearRestartRildParam() {
        log("clearRestartRildParam");
        this.mFirstPdpActFailTimestamp = 0;
        this.mPdpActFailCount = 0;
        this.mRestartRildEnabled = true;
    }

    private boolean containsAllApns(ArrayList<ApnSetting> oldApnList, ArrayList<ApnSetting> newApnList) {
        Iterator it = newApnList.iterator();
        while (it.hasNext()) {
            ApnSetting newApnSetting = (ApnSetting) it.next();
            boolean canHandle = false;
            Iterator it2 = oldApnList.iterator();
            while (it2.hasNext()) {
                if (((ApnSetting) it2.next()).equals(newApnSetting, this.mPhone.getServiceState().getDataRoamingFromRegistration())) {
                    canHandle = true;
                    break;
                }
            }
            if (!canHandle) {
                return false;
            }
        }
        return true;
    }

    private void cleanUpConnectionsOnUpdatedApns(boolean tearDown, String reason) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cleanUpConnectionsOnUpdatedApns: tearDown=");
        stringBuilder.append(tearDown);
        log(stringBuilder.toString());
        if (this.mAllApnSettings != null && this.mAllApnSettings.isEmpty()) {
            cleanUpAllConnections(tearDown, PhoneInternalInterface.REASON_APN_CHANGED);
        } else if (this.mPhone.getServiceState().getRilDataRadioTechnology() != 0) {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                ArrayList<ApnSetting> currentWaitingApns = apnContext.getWaitingApns();
                ArrayList<ApnSetting> waitingApns = buildWaitingApns(apnContext.getApnType(), this.mPhone.getServiceState().getRilDataRadioTechnology());
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("new waitingApns:");
                stringBuilder2.append(waitingApns);
                log(stringBuilder2.toString());
                if (!(currentWaitingApns == null || (waitingApns.size() == currentWaitingApns.size() && containsAllApns(currentWaitingApns, waitingApns)))) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("new waiting apn is different for ");
                    stringBuilder2.append(apnContext);
                    log(stringBuilder2.toString());
                    apnContext.setWaitingApns(waitingApns);
                    if (!apnContext.isDisconnected()) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("cleanUpConnectionsOnUpdatedApns for ");
                        stringBuilder2.append(apnContext);
                        log(stringBuilder2.toString());
                        apnContext.setReason(reason);
                        cleanUpConnection(true, apnContext);
                    }
                }
            }
        } else {
            return;
        }
        if (!isConnected()) {
            stopNetStatPoll();
            stopDataStallAlarm();
        }
        this.mRequestedApnType = "default";
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDisconnectPendingCount = ");
        stringBuilder.append(this.mDisconnectPendingCount);
        log(stringBuilder.toString());
        if (tearDown && this.mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
    }

    private void resetPollStats() {
        this.mTxPkts = -1;
        this.mRxPkts = -1;
        this.mNetStatPollPeriod = 1000;
    }

    private void startNetStatPoll() {
        if (getOverallState() == State.CONNECTED && !this.mNetStatPollEnabled) {
            log("startNetStatPoll");
            resetPollStats();
            this.mNetStatPollEnabled = true;
            this.mPollNetStat.run();
        }
        if (this.mPhone != null) {
            this.mPhone.notifyDataActivity();
        }
    }

    private void stopNetStatPoll() {
        this.mNetStatPollEnabled = false;
        removeCallbacks(this.mPollNetStat);
        log("stopNetStatPoll");
        if (this.mPhone != null) {
            this.mPhone.notifyDataActivity();
        }
    }

    public void sendStartNetStatPoll(Activity activity) {
        Message msg = obtainMessage(270376);
        msg.arg1 = 1;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStartNetStatPoll(Activity activity) {
        startNetStatPoll();
        startDataStallAlarm(false);
        setActivity(activity);
    }

    public void sendStopNetStatPoll(Activity activity) {
        Message msg = obtainMessage(270376);
        msg.arg1 = 0;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStopNetStatPoll(Activity activity) {
        stopNetStatPoll();
        stopDataStallAlarm();
        setActivity(activity);
    }

    private void updateDataActivity() {
        TxRxSum preTxRxSum = new TxRxSum(this.mTxPkts, this.mRxPkts);
        TxRxSum curTxRxSum = new TxRxSum();
        curTxRxSum.updateThisModemMobileTxRxSum(mIfacePhoneHashMap, this.mPhone.getPhoneId());
        this.mTxPkts = curTxRxSum.txPkts;
        this.mRxPkts = curTxRxSum.rxPkts;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateDataActivity: curTxRxSum=");
        stringBuilder.append(curTxRxSum);
        stringBuilder.append(" preTxRxSum=");
        stringBuilder.append(preTxRxSum);
        log(stringBuilder.toString());
        if (!this.mNetStatPollEnabled) {
            return;
        }
        if (preTxRxSum.txPkts > 0 || preTxRxSum.rxPkts > 0) {
            Activity newActivity;
            long sent = this.mTxPkts - preTxRxSum.txPkts;
            long received = this.mRxPkts - preTxRxSum.rxPkts;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateDataActivity: sent=");
            stringBuilder2.append(sent);
            stringBuilder2.append(" received=");
            stringBuilder2.append(received);
            log(stringBuilder2.toString());
            if (sent > 0 && received > 0) {
                newActivity = Activity.DATAINANDOUT;
                updateDSUseDuration();
            } else if (sent > 0 && received == 0) {
                newActivity = Activity.DATAOUT;
                updateDSUseDuration();
            } else if (sent != 0 || received <= 0) {
                newActivity = this.mActivity == Activity.DORMANT ? this.mActivity : Activity.NONE;
            } else {
                newActivity = Activity.DATAIN;
                updateDSUseDuration();
            }
            if (this.mActivity != newActivity && this.mIsScreenOn) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("updateDataActivity: newActivity=");
                stringBuilder3.append(newActivity);
                log(stringBuilder3.toString());
                this.mActivity = newActivity;
                this.mPhone.notifyDataActivity();
            }
        }
    }

    private void handlePcoData(AsyncResult ar) {
        if (ar.exception != null) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PCO_DATA exception: ");
            stringBuilder.append(ar.exception);
            Rlog.e(str, stringBuilder.toString());
            return;
        }
        Iterator it;
        DataConnection dc;
        PcoData pcoData = ar.result;
        if (this.mHwCustDcTracker != null) {
            this.mHwCustDcTracker.savePcoData(pcoData);
        }
        ArrayList<DataConnection> dcList = new ArrayList();
        DataConnection temp = this.mDcc.getActiveDcByCid(pcoData.cid);
        if (temp != null) {
            dcList.add(temp);
        }
        if (dcList.size() == 0) {
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("PCO_DATA for unknown cid: ");
            stringBuilder2.append(pcoData.cid);
            stringBuilder2.append(", inferring");
            Rlog.e(str2, stringBuilder2.toString());
            for (DataConnection dc2 : this.mDataConnections.values()) {
                int cid = dc2.getCid();
                if (cid == pcoData.cid) {
                    str2 = LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("  found ");
                    stringBuilder3.append(dc2);
                    Rlog.d(str2, stringBuilder3.toString());
                    dcList.clear();
                    dcList.add(dc2);
                    break;
                } else if (cid == -1) {
                    for (ApnContext apnContext : dc2.mApnContexts.keySet()) {
                        if (apnContext.getState() == State.CONNECTING) {
                            String str3 = LOG_TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("  found potential ");
                            stringBuilder4.append(dc2);
                            Rlog.d(str3, stringBuilder4.toString());
                            dcList.add(dc2);
                            break;
                        }
                    }
                }
            }
        }
        if (dcList.size() == 0) {
            Rlog.e(LOG_TAG, "PCO_DATA - couldn't infer cid");
            return;
        }
        it = dcList.iterator();
        while (it.hasNext()) {
            dc2 = (DataConnection) it.next();
            if (dc2.mApnContexts.size() == 0) {
                break;
            }
            for (ApnContext apnContext2 : dc2.mApnContexts.keySet()) {
                String apnType = apnContext2.getApnType();
                Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_PCO_VALUE");
                intent.putExtra("apnType", apnType);
                intent.putExtra("apnProto", pcoData.bearerProto);
                intent.putExtra("pcoId", pcoData.pcoId);
                intent.putExtra("pcoValue", pcoData.contents);
                this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
            }
        }
    }

    private int getRecoveryAction() {
        return System.getInt(this.mResolver, "radio.data.stall.recovery.action", 0);
    }

    protected void putRecoveryAction(int action) {
        System.putInt(this.mResolver, "radio.data.stall.recovery.action", action);
    }

    private void broadcastDataStallDetected(int recoveryAction) {
        Intent intent = new Intent("android.intent.action.DATA_STALL_DETECTED");
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        intent.putExtra("recoveryAction", recoveryAction);
        this.mPhone.getContext().sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
    }

    private void doRecovery() {
        if (getOverallState() == State.CONNECTED) {
            int recoveryAction = getRecoveryAction();
            TelephonyMetrics.getInstance().writeDataStallEvent(this.mPhone.getPhoneId(), recoveryAction);
            HwTelephonyFactory.getHwDataServiceChrManager().sendIntentWhenDorecovery(this.mPhone, recoveryAction);
            broadcastDataStallDetected(recoveryAction);
            switch (recoveryAction) {
                case 0:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_GET_DATA_CALL_LIST, this.mSentSinceLastRecv);
                    log("doRecovery() get data call list");
                    if (this.mDcc != null) {
                        this.mDcc.getDataCallList();
                    }
                    long now = SystemClock.elapsedRealtime();
                    if (!noNeedDoRecovery(this.mApnContexts) && this.mPhone.getServiceState().getDataRegState() == 0 && (this.lastRadioResetTimestamp == 0 || now - this.lastRadioResetTimestamp >= ((long) DATA_STALL_ALARM_PUNISH_DELAY_IN_MS_DEFAULT))) {
                        log("Since this apn is preseted apn, so we need to do recovery.");
                        putRecoveryAction(1);
                        break;
                    }
                    putRecoveryAction(0);
                    log("This apn is not preseted apn or we set nodorecovery to fobid do recovery, so we needn't to do recovery.");
                    break;
                    break;
                case 1:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_CLEANUP, this.mSentSinceLastRecv);
                    log("doRecovery() cleanup all connections");
                    cleanUpAllConnections(PhoneInternalInterface.REASON_PDP_RESET);
                    putRecoveryAction(2);
                    break;
                case 2:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_REREGISTER, this.mSentSinceLastRecv);
                    log("doRecovery() re-register");
                    this.mPhone.getServiceStateTracker().reRegisterNetwork(null);
                    putRecoveryAction(3);
                    break;
                case 3:
                    this.lastRadioResetTimestamp = SystemClock.elapsedRealtime();
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART, this.mSentSinceLastRecv);
                    log("restarting radio");
                    this.mPhone.getServiceStateTracker().setDoRecoveryTriggerState(true);
                    restartRadio();
                    putRecoveryAction(0);
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("doRecovery: Invalid recoveryAction=");
                    stringBuilder.append(recoveryAction);
                    throw new RuntimeException(stringBuilder.toString());
            }
            this.mSentSinceLastRecv = 0;
        }
    }

    private void updateDataStallInfo() {
        TxRxSum preTxRxSum = new TxRxSum(this.mDataStallTxRxSum);
        if (enableTcpUdpSumForDataStall()) {
            this.mDataStallTxRxSum.updateThisModemMobileTxRxSum(mIfacePhoneHashMap, this.mPhone.getPhoneId());
        } else {
            this.mDataStallTxRxSum.updateTxRxSum();
            long[] dnsTxRx = getDnsPacketTxRxSum();
            TxRxSum txRxSum = this.mDataStallTxRxSum;
            txRxSum.txPkts += dnsTxRx[0];
            txRxSum = this.mDataStallTxRxSum;
            txRxSum.rxPkts += dnsTxRx[1];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateDataStallInfo: getDnsPacketTxRxSum dnsTx=");
            stringBuilder.append(dnsTxRx[0]);
            stringBuilder.append(" dnsRx=");
            stringBuilder.append(dnsTxRx[1]);
            log(stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("updateDataStallInfo: mDataStallTxRxSum=");
        stringBuilder2.append(this.mDataStallTxRxSum);
        stringBuilder2.append(" preTxRxSum=");
        stringBuilder2.append(preTxRxSum);
        log(stringBuilder2.toString());
        long sent = this.mDataStallTxRxSum.txPkts - preTxRxSum.txPkts;
        long received = this.mDataStallTxRxSum.rxPkts - preTxRxSum.rxPkts;
        if (sent > 0 && received > 0) {
            this.mSentSinceLastRecv = 0;
            putRecoveryAction(0);
        } else if (sent > 0 && received == 0) {
            if (isPhoneStateIdle()) {
                this.mSentSinceLastRecv += sent;
            } else {
                this.mSentSinceLastRecv = 0;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("updateDataStallInfo: OUT sent=");
            stringBuilder2.append(sent);
            stringBuilder2.append(" mSentSinceLastRecv=");
            stringBuilder2.append(this.mSentSinceLastRecv);
            log(stringBuilder2.toString());
        } else if (sent == 0 && received > 0) {
            this.mSentSinceLastRecv = 0;
            putRecoveryAction(0);
        }
    }

    private boolean isPhoneStateIdle() {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        int i = 0;
        while (i < phoneCount) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone == null || phone.getState() == PhoneConstants.State.IDLE) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isPhoneStateIdle: Voice call active on sub: ");
                stringBuilder.append(i);
                log(stringBuilder.toString());
                return false;
            }
        }
        ImsPhone imsPhone = (ImsPhone) this.mPhone.getImsPhone();
        if (imsPhone == null || !imsPhone.mHwImsPhone.isBusy()) {
            return true;
        }
        log("isPhoneStateIdle: ImsPhone isBusy true");
        return false;
    }

    private void onDataStallAlarm(int tag) {
        if (this.mDataStallAlarmTag != tag) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDataStallAlarm: ignore, tag=");
            stringBuilder.append(tag);
            stringBuilder.append(" expecting ");
            stringBuilder.append(this.mDataStallAlarmTag);
            log(stringBuilder.toString());
            return;
        }
        updateDataStallInfo();
        boolean suspectedStall = false;
        if (this.mSentSinceLastRecv >= ((long) Global.getInt(this.mResolver, "pdp_watchdog_trigger_packet_count", 10))) {
            if (isPingOk()) {
                this.mSentSinceLastRecv = 0;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onDataStallAlarm: tag=");
                stringBuilder2.append(tag);
                stringBuilder2.append(" do recovery action=");
                stringBuilder2.append(getRecoveryAction());
                log(stringBuilder2.toString());
                suspectedStall = true;
                sendMessage(obtainMessage(270354));
            }
        }
        startDataStallAlarm(suspectedStall);
    }

    private void startDataStallAlarm(boolean suspectedStall) {
        int nextAction = getRecoveryAction();
        if (this.mDataStallDetectionEnabled && getOverallState() == State.CONNECTED) {
            int delayInMs;
            if (this.mIsScreenOn || suspectedStall || RecoveryAction.isAggressiveRecovery(nextAction)) {
                delayInMs = Global.getInt(this.mResolver, "data_stall_alarm_aggressive_delay_in_ms", 60000);
            } else {
                if (SystemProperties.getBoolean("ro.config.power", false)) {
                    DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 6000000;
                }
                delayInMs = Global.getInt(this.mResolver, "data_stall_alarm_non_aggressive_delay_in_ms", DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
            }
            this.mDataStallAlarmTag++;
            Intent intent = new Intent(INTENT_DATA_STALL_ALARM);
            intent.addFlags(268435456);
            intent.putExtra(DATA_STALL_ALARM_TAG_EXTRA, this.mDataStallAlarmTag);
            this.mDataStallAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
            this.mAlarmManager.setExact(3, SystemClock.elapsedRealtime() + ((long) delayInMs), this.mDataStallAlarmIntent);
        }
    }

    private void stopDataStallAlarm() {
        this.mDataStallAlarmTag++;
        if (this.mDataStallAlarmIntent != null) {
            this.mAlarmManager.cancel(this.mDataStallAlarmIntent);
            this.mDataStallAlarmIntent = null;
        }
    }

    private void restartDataStallAlarm() {
        if (!isConnected()) {
            return;
        }
        if (RecoveryAction.isAggressiveRecovery(getRecoveryAction())) {
            log("restartDataStallAlarm: action is pending. not resetting the alarm.");
            return;
        }
        stopDataStallAlarm();
        startDataStallAlarm(false);
    }

    boolean isSupportLTE(ApnSetting apnSettings) {
        if (((apnSettings.bearer == 13 || apnSettings.bearer == 14) && isApnPreset(apnSettings)) || !isApnPreset(apnSettings)) {
            return true;
        }
        return false;
    }

    protected boolean isCTCardForFullNet() {
        if (isFullNetworkSupported()) {
            return isCTSimCard(this.mPhone.getPhoneId());
        }
        return false;
    }

    private void onActionIntentProvisioningApnAlarm(Intent intent) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onActionIntentProvisioningApnAlarm: action=");
        stringBuilder.append(intent.getAction());
        log(stringBuilder.toString());
        Message msg = obtainMessage(270375, intent.getAction());
        msg.arg1 = intent.getIntExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    private void startProvisioningApnAlarm() {
        int delayInMs = Global.getInt(this.mResolver, "provisioning_apn_alarm_delay_in_ms", PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT);
        if (Build.IS_DEBUGGABLE) {
            try {
                delayInMs = Integer.parseInt(System.getProperty(DEBUG_PROV_APN_ALARM, Integer.toString(delayInMs)));
            } catch (NumberFormatException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startProvisioningApnAlarm: e=");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
            }
        }
        this.mProvisioningApnAlarmTag++;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("startProvisioningApnAlarm: tag=");
        stringBuilder2.append(this.mProvisioningApnAlarmTag);
        stringBuilder2.append(" delay=");
        stringBuilder2.append(delayInMs / 1000);
        stringBuilder2.append("s");
        log(stringBuilder2.toString());
        Intent intent = new Intent(INTENT_PROVISIONING_APN_ALARM);
        intent.addFlags(268435456);
        intent.putExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, this.mProvisioningApnAlarmTag);
        this.mProvisioningApnAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + ((long) delayInMs), this.mProvisioningApnAlarmIntent);
    }

    private void stopProvisioningApnAlarm() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopProvisioningApnAlarm: current tag=");
        stringBuilder.append(this.mProvisioningApnAlarmTag);
        stringBuilder.append(" mProvsioningApnAlarmIntent=");
        stringBuilder.append(this.mProvisioningApnAlarmIntent);
        log(stringBuilder.toString());
        this.mProvisioningApnAlarmTag++;
        if (this.mProvisioningApnAlarmIntent != null) {
            this.mAlarmManager.cancel(this.mProvisioningApnAlarmIntent);
            this.mProvisioningApnAlarmIntent = null;
        }
    }

    private ArrayList<ApnSetting> buildWaitingApnsForCTSupl(String requestedApnType, int radioTech) {
        ArrayList<ApnSetting> apnList = new ArrayList();
        if (!(this.mAllApnSettings == null || this.mAllApnSettings.isEmpty())) {
            Iterator it = this.mAllApnSettings.iterator();
            while (it.hasNext()) {
                ApnSetting apn = (ApnSetting) it.next();
                if (apn.canHandleType(requestedApnType) && ((!isLTENetwork() && ServiceState.bitmaskHasTech(apn.bearerBitmask, radioTech)) || (isLTENetwork() && (apn.bearer == 13 || apn.bearer == 14)))) {
                    if (!(TelephonyManager.getDefault().isNetworkRoaming(this.mPhone.getSubId()) && "ctnet".equals(apn.apn)) && (TelephonyManager.getDefault().isNetworkRoaming(this.mPhone.getSubId()) || !"ctwap".equals(apn.apn))) {
                        log("buildWaitingApns: ct supl featrue endabled, APN not match");
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("buildWaitingApns: adding apn=");
                        stringBuilder.append(apn);
                        log(stringBuilder.toString());
                        apnList.add(apn);
                    }
                }
            }
        }
        return apnList;
    }

    protected void onActionIntentPdpResetAlarm(Intent intent) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onActionIntentPdpResetAlarm: action=");
        stringBuilder.append(intent.getAction());
        log(stringBuilder.toString());
        Message msg = obtainMessage(271139, intent.getAction());
        msg.arg1 = intent.getIntExtra(PDP_RESET_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    protected void onPdpResetAlarm(int tag) {
        if (this.mPdpResetAlarmTag != tag) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPdpRestAlarm: ignore, tag=");
            stringBuilder.append(tag);
            stringBuilder.append(" expecting ");
            stringBuilder.append(this.mPdpResetAlarmTag);
            log(stringBuilder.toString());
            return;
        }
        cleanUpAllConnections(PhoneInternalInterface.REASON_PDP_RESET);
    }

    protected void startPdpResetAlarm(int delay) {
        this.mPdpResetAlarmTag++;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startPdpResetAlarm: tag=");
        stringBuilder.append(this.mPdpResetAlarmTag);
        stringBuilder.append(" delay=");
        stringBuilder.append(delay / 1000);
        stringBuilder.append("s");
        log(stringBuilder.toString());
        Intent intent = new Intent(INTENT_PDP_RESET_ALARM);
        intent.putExtra(PDP_RESET_ALARM_TAG_EXTRA, this.mPdpResetAlarmTag);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("startPdpResetAlarm: delay=");
        stringBuilder2.append(delay);
        stringBuilder2.append(" action=");
        stringBuilder2.append(intent.getAction());
        log(stringBuilder2.toString());
        this.mPdpResetAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + ((long) delay), this.mPdpResetAlarmIntent);
    }

    protected void stopPdpResetAlarm() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopPdpResetAlarm: current tag=");
        stringBuilder.append(this.mPdpResetAlarmTag);
        stringBuilder.append(" mPdpResetAlarmIntent=");
        stringBuilder.append(this.mPdpResetAlarmIntent);
        log(stringBuilder.toString());
        this.mPdpResetAlarmTag++;
        if (this.mPdpResetAlarmIntent != null) {
            this.mAlarmManager.cancel(this.mPdpResetAlarmIntent);
            this.mPdpResetAlarmIntent = null;
        }
    }

    void sendDataSetupCompleteFailed() {
        log("sendDataSetupCompleteFailed:");
        sendMessage(obtainMessage(271138));
    }

    private boolean isBlockSetInitialAttachApn() {
        String plmnsConfig = System.getString(this.mPhone.getContext().getContentResolver(), "apn_reminder_plmn");
        IccRecords r = (IccRecords) this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        if (TextUtils.isEmpty(plmnsConfig) || TextUtils.isEmpty(operator)) {
            return false;
        }
        return plmnsConfig.contains(operator);
    }

    private void onUserSelectOpenService() {
        log("onUserSelectOpenService set apn = bip0");
        if (this.mAllApnSettings != null && this.mAllApnSettings.isEmpty()) {
            createAllApnList();
        }
        if (this.mPhone.mCi.getRadioState().isOn()) {
            log("onRecordsLoaded: notifying data availability");
            notifyOffApnsOfAvailability(AbstractPhoneInternalInterface.REASON_SIM_LOADED_PSEUDOIMSI);
        }
        setupDataOnConnectableApns(AbstractPhoneInternalInterface.REASON_SET_PS_ONLY_OK);
    }

    public void checkPLMN(String plmn) {
        if (this.mHwCustDcTracker != null) {
            this.mHwCustDcTracker.checkPLMN(plmn);
        }
    }

    private void onOtaAttachFailed(ApnContext apnContext) {
        if (this.mHwCustDcTracker != null) {
            this.mHwCustDcTracker.onOtaAttachFailed(apnContext);
        }
    }

    private boolean getmIsPseudoImsi() {
        if (this.mHwCustDcTracker != null) {
            return this.mHwCustDcTracker.getmIsPseudoImsi();
        }
        return false;
    }

    private void sendOTAAttachTimeoutMsg(ApnContext apnContext, boolean retValue) {
        if (this.mHwCustDcTracker != null) {
            this.mHwCustDcTracker.sendOTAAttachTimeoutMsg(apnContext, retValue);
        }
    }

    private void openServiceStart(UiccController uiccController) {
        if (this.mHwCustDcTracker != null) {
            this.mHwCustDcTracker.openServiceStart(uiccController);
        }
    }

    private void clearAndResumeNetInfiForWifiLteCoexist(int apnId, int enabled, ApnContext apnContext) {
        if (ENABLE_WIFI_LTE_CE || isMpLinkEnable(apnContext.getApnType())) {
            String apnType = apnContext.getApnType();
            if (apnType.equals("default") || "hipri".equals(apnType)) {
                log("enableApnType but already actived");
                if (enabled != 1) {
                    ConnectivityManager cm = ConnectivityManager.from(this.mPhone.getContext());
                    if (!apnContext.isDisconnected() && isWifiConnected()) {
                        log("clearAndResumeNetInfiForWifiLteCoexist:disableApnType due to WIFI Connected");
                        stopNetStatPoll();
                        stopDataStallAlarm();
                        if (!isMpLinkEnable(apnContext.getApnType())) {
                            apnContext.setEnabled(false);
                        }
                        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
                    }
                } else if (apnContext.getState() == State.CONNECTED) {
                    log("enableApnType: return APN_ALREADY_ACTIVE");
                    apnContext.setEnabled(true);
                    startNetStatPoll();
                    restartDataStallAlarm();
                    this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
                }
            }
        }
    }

    boolean isNeedFilterVowifiMms(ApnSetting apn, String requestedApnType) {
        boolean isMmsRequested = "mms".equals(requestedApnType);
        boolean hasVowifiMmsType = apn != null && ArrayUtils.contains(apn.types, APN_TYPE_VOWIFIMMS);
        if (isMmsRequested && hasVowifiMmsType && HuaweiTelephonyConfigs.isHisiPlatform()) {
            return true;
        }
        return false;
    }

    private boolean isMmsApn(ApnContext apnContext) {
        return apnContext != null && "mms".equals(apnContext.getApnType());
    }

    private boolean isXcapApn(ApnContext apnContext) {
        return apnContext != null && "xcap".equals(apnContext.getApnType());
    }

    private boolean isNeedForceSetup(ApnContext apnContext) {
        return apnContext != null && PhoneInternalInterface.REASON_DATA_ENABLED.equals(apnContext.getReason()) && this.mPhone.getServiceState().getVoiceRegState() == 0 && USER_FORCE_DATA_SETUP;
    }

    private static DataProfile createDataProfile(ApnSetting apn) {
        return createDataProfile(apn, apn.profileId);
    }

    @VisibleForTesting
    public static DataProfile createDataProfile(ApnSetting apn, int profileId) {
        int i;
        ApnSetting apnSetting = apn;
        int bearerBitmap = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(apnSetting.networkTypeBitmask);
        if (bearerBitmap == 0) {
            i = 0;
        } else if (ServiceState.bearerBitmapHasCdma(bearerBitmap)) {
            i = 2;
        } else {
            i = 1;
        }
        int profileType = i;
        String str = apnSetting.apn;
        String str2 = apnSetting.protocol;
        int i2 = apnSetting.authType;
        String str3 = apnSetting.user;
        String str4 = apnSetting.password;
        int i3 = apnSetting.maxConnsTime;
        int i4 = apnSetting.maxConns;
        int i5 = apnSetting.waitTime;
        boolean z = apnSetting.carrierEnabled;
        int i6 = apnSetting.typesBitmap;
        String str5 = apnSetting.roamingProtocol;
        int profileType2 = profileType;
        String str6 = str5;
        return new DataProfile(profileId, str, str2, i2, str3, str4, profileType2, i3, i4, i5, z, i6, str6, bearerBitmap, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.modemCognitive);
    }

    private void onDataServiceBindingChanged(boolean bound) {
        if (bound) {
            this.mDcc.start();
        } else {
            this.mDcc.dispose();
        }
    }

    private boolean isDataDisableBySim2() {
        if (this.mHwCustDcTracker != null) {
            return this.mHwCustDcTracker.isDataDisableBySim2();
        }
        log("isDataDisableBySim2: Maybe Exception occurs, mHwCustDcTracker is null");
        return false;
    }

    private boolean getCustRetryConfig() {
        int subId = this.mPhone.getSubId();
        Boolean valueFromCard = (Boolean) HwCfgFilePolicy.getValue("cust_retry_config", subId, Boolean.class);
        boolean valueFromProp = CUST_RETRY_CONFIG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCustRetryConfig, subId:");
        stringBuilder.append(subId);
        stringBuilder.append(", card:");
        stringBuilder.append(valueFromCard);
        stringBuilder.append(", prop:");
        stringBuilder.append(valueFromProp);
        log(stringBuilder.toString());
        return valueFromCard != null ? valueFromCard.booleanValue() : valueFromProp;
    }

    private boolean getEsmFlagAdaptionEnabled() {
        int subId = this.mPhone.getSubId();
        Boolean esmFlagAdaptionEnabled = (Boolean) HwCfgFilePolicy.getValue("attach_apn_enabled", subId, Boolean.class);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getEsmFlagAdaptionEnabled, subId:");
        stringBuilder.append(subId);
        stringBuilder.append(", card:");
        stringBuilder.append(esmFlagAdaptionEnabled);
        log(stringBuilder.toString());
        return esmFlagAdaptionEnabled != null ? esmFlagAdaptionEnabled.booleanValue() : ESM_FLAG_ADAPTION_ENABLED;
    }

    private int getEsmFlagFromCard() {
        int subId = this.mPhone.getSubId();
        Integer esmFlagFromCard = (Integer) HwCfgFilePolicy.getValue("plmn_esm_flag", subId, Integer.class);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getEsmFlagFromCard, subId:");
        stringBuilder.append(subId);
        stringBuilder.append(", card:");
        stringBuilder.append(esmFlagFromCard);
        log(stringBuilder.toString());
        return esmFlagFromCard != null ? esmFlagFromCard.intValue() : -1;
    }

    public String getOpKeyByActivedApn(String activedNumeric, String activedApn, String activedUser) {
        if (this.mHwCustDcTracker != null) {
            return this.mHwCustDcTracker.getOpKeyByActivedApn(activedNumeric, activedApn, activedUser);
        }
        log("getOpKeyByActivedApn: Maybe Exception occurs, mHwCustDcTracker is null");
        return null;
    }

    public void setApnOpkeyToSettingsDB(String activedApnOpkey) {
        if (this.mHwCustDcTracker == null) {
            log("setApnOpkeyToSettingsDB: Maybe Exception occurs, mHwCustDcTracker is null");
        } else {
            this.mHwCustDcTracker.setApnOpkeyToSettingsDB(activedApnOpkey);
        }
    }

    public void disposeCustDct() {
        if (this.mHwCustDcTracker == null) {
            log("dispose: Maybe Exception occurs, mHwCustDcTracker is null");
        } else {
            this.mHwCustDcTracker.dispose();
        }
    }
}
