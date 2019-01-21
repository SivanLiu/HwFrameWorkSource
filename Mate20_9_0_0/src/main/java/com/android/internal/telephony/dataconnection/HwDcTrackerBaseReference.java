package com.android.internal.telephony.dataconnection;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.common.HwFrameworkFactory;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
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
import android.telephony.HwTelephonyManagerInner;
import android.telephony.HwVSimManager;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import com.android.internal.telephony.DctConstants.State;
import com.android.internal.telephony.GlobalParamsAdaptor;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwModemCapability;
import com.android.internal.telephony.HwServiceStateManager;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.dataconnection.AbstractDcTrackerBase.DcTrackerBaseReference;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimUtils;
import huawei.cust.HwCfgFilePolicy;
import huawei.cust.HwCustUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class HwDcTrackerBaseReference implements DcTrackerBaseReference {
    private static final String ACTION_BT_CONNECTION_CHANGED = "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    private static final String ALLOW_MMS_PROPERTY_INT = "allow_mms_property_int";
    protected static final String CAUSE_NO_RETRY_AFTER_DISCONNECT = SystemProperties.get("ro.hwpp.disc_noretry_cause", "");
    private static final String CHINA_OPERATOR_MCC = "460";
    private static final String CLEARCODE_2HOUR_DELAY_OVER = "clearcode2HourDelayOver";
    private static final String CT_CDMA_OPERATOR = "46003";
    private static final String CUST_PREFERRED_APN = SystemProperties.get("ro.hwpp.preferred_apn", "").trim();
    public static final int DATA_ROAMING_EXCEPTION = -1;
    public static final int DATA_ROAMING_INTERNATIONAL = 2;
    public static final int DATA_ROAMING_NATIONAL = 1;
    public static final int DATA_ROAMING_OFF = 0;
    public static final String DATA_ROAMING_SIM2 = "data_roaming_sim2";
    private static final boolean DBG = true;
    private static final int DELAY_2_HOUR = 7200000;
    private static final boolean DISABLE_GW_PS_ATTACH = SystemProperties.getBoolean("ro.odm.disable_m1_gw_ps_attach", false);
    private static final String DS_USE_DURATION_KEY = "DSUseDuration";
    private static final int DS_USE_STATISTICS_REPORT_INTERVAL = 3600000;
    protected static final String ENABLE_ALLOW_MMS = "enable_always_allow_mms";
    private static final int EVENT_FDN_RECORDS_LOADED = 2;
    private static final int EVENT_FDN_SWITCH_CHANGED = 1;
    private static final int EVENT_LIMIT_PDP_ACT_IND = 4;
    private static final int EVENT_VOICE_CALL_ENDED = 3;
    protected static final Uri FDN_URL = Uri.parse("content://icc/fdn/subId/");
    private static final String INTENT_LIMIT_PDP_ACT_IND = "com.android.internal.telephony.limitpdpactind";
    private static final String INTENT_SET_PREF_NETWORK_TYPE = "com.android.internal.telephony.set-pref-networktype";
    private static final String INTENT_SET_PREF_NETWORK_TYPE_EXTRA_TYPE = "network_type";
    private static final int INVALID_VALUE = -1;
    private static final boolean IS_ATT;
    private static final boolean IS_DUAL_4G_SUPPORTED = HwModemCapability.isCapabilitySupport(21);
    private static final String IS_LIMIT_PDP_ACT = "islimitpdpact";
    private static final int MCC_LENGTH = 3;
    protected static final boolean MMSIgnoreDSSwitchNotRoaming;
    protected static final boolean MMSIgnoreDSSwitchOnRoaming = (((MMS_PROP >> 1) & 1) == 1);
    protected static final boolean MMS_ON_ROAMING = ((MMS_PROP & 1) == 1);
    protected static final int MMS_PROP = SystemProperties.getInt("ro.config.hw_always_allow_mms", 4);
    private static final String NETD_PROCESS_UID = "0";
    private static final int NETWORK_MODE_GSM_UMTS = 3;
    private static final int NETWORK_MODE_LTE_GSM_WCDMA = 9;
    private static final int NETWORK_MODE_UMTS_ONLY = 2;
    private static final int PID_STATS_FILE_IFACE_INDEX = 1;
    private static final int PID_STATS_FILE_PROCESS_NAME_INDEX = 2;
    private static final int PID_STATS_FILE_UDP_RX_INDEX = 14;
    private static final int PID_STATS_FILE_UDP_TX_INDEX = 20;
    private static final int PID_STATS_FILE_UID_INDEX = 3;
    private static final int PS_CLEARCODE_APN_DELAY_DEFAULT_MILLIS_4G = 10000;
    private static final int PS_CLEARCODE_APN_DELAY_DEFAULT_MILLIS_NOT_4G = 45000;
    private static final long PS_CLEARCODE_APN_DELAY_MILLIS_2G_3G = (SystemProperties.getLong("ro.config.clearcode_2g3g_timer", 45) * 1000);
    private static final long PS_CLEARCODE_APN_DELAY_MILLIS_4G = (SystemProperties.getLong("ro.config.clearcode_4g_timer", 10) * 1000);
    private static final long PS_CLEARCODE_LIMIT_PDP_ACT_DELAY = (SystemProperties.getLong("ro.config.clearcode_limit_timer", 1) * 1000);
    private static final String PS_CLEARCODE_PLMN = SystemProperties.get("ro.config.clearcode_plmn", "");
    private static final boolean RESET_PROFILE = SystemProperties.getBoolean("ro.hwpp_reset_profile", false);
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    public static final int SUB2 = 1;
    private static final String TAG = "HwDcTrackerBaseReference";
    protected static final boolean USER_FORCE_DATA_SETUP = SystemProperties.getBoolean("ro.hwpp.allow_data_onlycs", false);
    private static final String XCAP_DATA_ROAMING_ENABLE = "carrier_xcap_data_roaming_switch";
    protected static final boolean isMultiSimEnabled = HwFrameworkFactory.getHwInnerTelephonyManager().isMultiSimEnabled();
    private static boolean mIsScreenOn = false;
    private static int newRac = -1;
    private static int oldRac = -1;
    private static final String pidStatsPath = "/proc/net/xt_qtaguid/stats_pid";
    protected boolean ALLOW_MMS = false;
    private boolean SETAPN_UNTIL_CARDLOADED = SystemProperties.getBoolean("ro.config.delay_setapn", false);
    private boolean SUPPORT_MPDN = SystemProperties.getBoolean("persist.telephony.mpdn", true);
    private ContentObserver allowMmsObserver = null;
    private boolean broadcastPrePostPay = true;
    GsmCellLocation cellLoc = new GsmCellLocation();
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage msg=");
            stringBuilder.append(msg.what);
            hwDcTrackerBaseReference.log(stringBuilder.toString());
            switch (msg.what) {
                case 3:
                    HwDcTrackerBaseReference.this.onVoiceCallEndedHw();
                    return;
                case 4:
                    AsyncResult ar = msg.obj;
                    if (ar.exception != null) {
                        HwDcTrackerBaseReference hwDcTrackerBaseReference2 = HwDcTrackerBaseReference.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("PSCLEARCODE EVENT_LIMIT_PDP_ACT_IND exception ");
                        stringBuilder2.append(ar.exception);
                        hwDcTrackerBaseReference2.log(stringBuilder2.toString());
                        return;
                    }
                    HwDcTrackerBaseReference.this.onLimitPDPActInd(ar);
                    return;
                default:
                    return;
            }
        }
    };
    private boolean isRecievedPingReply = false;
    private boolean isSupportPidStats = false;
    private PendingIntent mAlarmIntent;
    private AlarmManager mAlarmManager;
    private PendingIntent mClearCodeLimitAlarmIntent = null;
    public DcFailCause mCurFailCause;
    private int mDSUseDuration = 0;
    private DcTracker mDcTrackerBase;
    private int mDelayTime = 3000;
    private boolean mDoRecoveryAddDnsProp = SystemProperties.getBoolean("ro.config.dorecovery_add_dns", true);
    private FdnAsyncQueryHandler mFdnAsyncQuery;
    private FdnChangeObserver mFdnChangeObserver;
    ServiceStateTracker mGsmServiceStateTracker = null;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("android.intent.action.SIM_STATE_CHANGED")) {
                int subid = 0;
                if (intent.getExtra("subscription") != null) {
                    subid = intent.getIntExtra("subscription", -1);
                }
                if (subid != HwDcTrackerBaseReference.this.mSubscription.intValue()) {
                    Rlog.d(HwDcTrackerBaseReference.TAG, "receive INTENT_VALUE_ICC_ABSENT or INTENT_VALUE_ICC_CARD_IO_ERROR , but the subid is different from mSubscription");
                    return;
                }
                String curSimState = intent.getStringExtra("ss");
                if (TextUtils.equals(curSimState, HwDcTrackerBaseReference.this.mSimState)) {
                    Rlog.d(HwDcTrackerBaseReference.TAG, "the curSimState is same as mSimState, so return");
                    return;
                }
                if (("ABSENT".equals(curSimState) || "CARD_IO_ERROR".equals(curSimState)) && !"ABSENT".equals(HwDcTrackerBaseReference.this.mSimState) && !"CARD_IO_ERROR".equals(HwDcTrackerBaseReference.this.mSimState) && HwDcTrackerBaseReference.RESET_PROFILE) {
                    Rlog.d(HwDcTrackerBaseReference.TAG, "receive INTENT_VALUE_ICC_ABSENT or INTENT_VALUE_ICC_CARD_IO_ERROR , resetprofile");
                    HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.mCi.resetProfile(null);
                }
                HwDcTrackerBaseReference.this.mSimState = curSimState;
            } else if (intent.getAction() != null && intent.getAction().equals(HwDcTrackerBaseReference.INTENT_SET_PREF_NETWORK_TYPE)) {
                HwDcTrackerBaseReference.this.onActionIntentSetNetworkType(intent);
            } else if (intent.getAction() != null && HwDcTrackerBaseReference.INTENT_LIMIT_PDP_ACT_IND.equals(intent.getAction())) {
                HwDcTrackerBaseReference.this.onActionIntentLimitPDPActInd(intent);
            } else if (intent.getAction() != null && "android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getContext());
                Editor editor = sp.edit();
                editor.putInt(HwDcTrackerBaseReference.DS_USE_DURATION_KEY, HwDcTrackerBaseReference.this.mDSUseDuration);
                editor.commit();
                HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Put mDSUseDuration into SharedPreferences, put: ");
                stringBuilder.append(HwDcTrackerBaseReference.this.mDSUseDuration);
                stringBuilder.append(", get: ");
                stringBuilder.append(sp.getInt(HwDcTrackerBaseReference.DS_USE_DURATION_KEY, 0));
                hwDcTrackerBaseReference.log(stringBuilder.toString());
            } else if (intent.getAction() != null && "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                int lastDSUseDuration = PreferenceManager.getDefaultSharedPreferences(HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getContext()).getInt(HwDcTrackerBaseReference.DS_USE_DURATION_KEY, 0);
                HwDcTrackerBaseReference.access$512(HwDcTrackerBaseReference.this, lastDSUseDuration);
                HwDcTrackerBaseReference hwDcTrackerBaseReference2 = HwDcTrackerBaseReference.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Read last mDSUseDuration back from SharedPreferences, lastDSUseDuration: ");
                stringBuilder2.append(lastDSUseDuration);
                stringBuilder2.append(", mDSUseDuration: ");
                stringBuilder2.append(HwDcTrackerBaseReference.this.mDSUseDuration);
                hwDcTrackerBaseReference2.log(stringBuilder2.toString());
            } else if (intent.getAction() != null && "android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                HwDcTrackerBaseReference.mIsScreenOn = true;
            } else if (intent.getAction() != null && "android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                HwDcTrackerBaseReference.mIsScreenOn = false;
            } else if (HwDcTrackerBaseReference.ACTION_BT_CONNECTION_CHANGED.equals(intent.getAction())) {
                if (intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1) == 0) {
                    HwDcTrackerBaseReference.this.mIsBtConnected = false;
                } else if (intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1) == 2) {
                    HwDcTrackerBaseReference.this.mIsBtConnected = true;
                }
                HwDcTrackerBaseReference hwDcTrackerBaseReference3 = HwDcTrackerBaseReference.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Received bt_connect_state = ");
                stringBuilder3.append(HwDcTrackerBaseReference.this.mIsBtConnected);
                hwDcTrackerBaseReference3.log(stringBuilder3.toString());
            }
        }
    };
    protected boolean mIsBtConnected = false;
    private boolean mIsClearCodeEnabled = SystemProperties.getBoolean("ro.config.hw_clearcode_pdp", false);
    private boolean mIsLimitPDPAct = false;
    private INetworkManagementService mNetworkManager = null;
    private long mNextReportDSUseDurationStamp = (SystemClock.elapsedRealtime() + 3600000);
    private int mNwOldMode = Phone.PREFERRED_NT_MODE;
    private AlertDialog mPSClearCodeDialog = null;
    private String mSimState = null;
    private Integer mSubscription;
    private int mTryIndex = 0;
    protected UiccController mUiccController = UiccController.getInstance();
    private int netdPid = -1;
    private int nwMode = Phone.PREFERRED_NT_MODE;
    private ContentObserver nwModeChangeObserver = null;
    private int oldRadioTech = 0;
    private Condition pingCondition = this.pingThreadlLock.newCondition();
    private ReentrantLock pingThreadlLock = new ReentrantLock();
    PhoneStateListener pslForCellLocation = new PhoneStateListener() {
        /* JADX WARNING: Removed duplicated region for block: B:49:0x01e6 A:{Catch:{ Exception -> 0x0200 }} */
        /* JADX WARNING: Removed duplicated region for block: B:48:0x01e5 A:{Catch:{ Exception -> 0x0200 }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onCellLocationChanged(CellLocation location) {
            if (HwDcTrackerBaseReference.this.mDcTrackerBase.mApnContexts != null) {
                try {
                    HwDcTrackerBaseReference.this.log("CLEARCODE onCellLocationChanged");
                    if (location instanceof GsmCellLocation) {
                        GsmCellLocation newCellLoc = (GsmCellLocation) location;
                        HwDcTrackerBaseReference.this.mGsmServiceStateTracker = HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getServiceStateTracker();
                        HwDcTrackerBaseReference.newRac = HwServiceStateManager.getHwGsmServiceStateManager(HwDcTrackerBaseReference.this.mGsmServiceStateTracker, (GsmCdmaPhone) HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone).getRac();
                        int radioTech = HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getServiceState().getRilDataRadioTechnology();
                        HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("CLEARCODE newCellLoc = ");
                        stringBuilder.append(newCellLoc);
                        stringBuilder.append(", oldCellLoc = ");
                        stringBuilder.append(HwDcTrackerBaseReference.this.cellLoc);
                        stringBuilder.append(" oldRac = ");
                        stringBuilder.append(HwDcTrackerBaseReference.oldRac);
                        stringBuilder.append(" newRac = ");
                        stringBuilder.append(HwDcTrackerBaseReference.newRac);
                        stringBuilder.append(" radioTech = ");
                        stringBuilder.append(radioTech);
                        stringBuilder.append(" oldRadioTech = ");
                        stringBuilder.append(HwDcTrackerBaseReference.this.oldRadioTech);
                        hwDcTrackerBaseReference.log(stringBuilder.toString());
                        boolean z = true;
                        boolean isClearRetryAlarm = (HuaweiTelephonyConfigs.isQcomPlatform() || HwDcTrackerBaseReference.this.oldRadioTech == radioTech) ? false : true;
                        if (isClearRetryAlarm) {
                            HwDcTrackerBaseReference.this.oldRadioTech = radioTech;
                            HwDcTrackerBaseReference hwDcTrackerBaseReference2 = HwDcTrackerBaseReference.this;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("clearcode oldRadioTech = ");
                            stringBuilder2.append(HwDcTrackerBaseReference.this.oldRadioTech);
                            hwDcTrackerBaseReference2.log(stringBuilder2.toString());
                            HwDcTrackerBaseReference.oldRac = -1;
                            HwDcTrackerBaseReference.this.resetTryTimes();
                        }
                        if (-1 == HwDcTrackerBaseReference.newRac) {
                            HwDcTrackerBaseReference.this.log("CLEARCODE not really changed");
                            return;
                        } else if (HwDcTrackerBaseReference.oldRac == HwDcTrackerBaseReference.newRac || radioTech != 3) {
                            HwDcTrackerBaseReference.this.log("CLEARCODE RAC not really changed");
                            return;
                        } else if (-1 == HwDcTrackerBaseReference.oldRac) {
                            HwDcTrackerBaseReference.oldRac = HwDcTrackerBaseReference.newRac;
                            HwDcTrackerBaseReference.this.log("CLEARCODE oldRac = -1 return");
                            return;
                        } else {
                            HwDcTrackerBaseReference.oldRac = HwDcTrackerBaseReference.newRac;
                            HwDcTrackerBaseReference.this.cellLoc = newCellLoc;
                            DcTracker dcTracker = HwDcTrackerBaseReference.this.mDcTrackerBase;
                            ApnContext defaultApn = (ApnContext) HwDcTrackerBaseReference.this.mDcTrackerBase.mApnContexts.get("default");
                            if (!(!HwDcTrackerBaseReference.this.mDcTrackerBase.isUserDataEnabled() || defaultApn == null || defaultApn.getState() == State.CONNECTED)) {
                                boolean isDisconnected;
                                int curPrefMode = Global.getInt(HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "preferred_network_mode", 0);
                                HwDcTrackerBaseReference hwDcTrackerBaseReference3 = HwDcTrackerBaseReference.this;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("CLEARCODE onCellLocationChanged radioTech = ");
                                stringBuilder3.append(radioTech);
                                stringBuilder3.append(" curPrefMode");
                                stringBuilder3.append(curPrefMode);
                                hwDcTrackerBaseReference3.log(stringBuilder3.toString());
                                if (!(curPrefMode == 9 || curPrefMode == 2)) {
                                    HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.setPreferredNetworkType(9, null);
                                    Global.putInt(HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "preferred_network_mode", 9);
                                    HwServiceStateManager.getHwGsmServiceStateManager(HwDcTrackerBaseReference.this.mGsmServiceStateTracker, (GsmCdmaPhone) HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone).setRac(-1);
                                    HwDcTrackerBaseReference.this.log("CLEARCODE onCellLocationChanged try switch 3G to 4G and set newrac to -1");
                                }
                                if (defaultApn.getState() != State.IDLE) {
                                    if (defaultApn.getState() != State.FAILED) {
                                        isDisconnected = false;
                                        HwDcTrackerBaseReference.this.log("CLEARCODE onCellLocationChanged try setup data again");
                                        if (!isDisconnected) {
                                            z = false;
                                        }
                                        DcTrackerUtils.cleanUpConnection(dcTracker, z, defaultApn);
                                        HwDcTrackerBaseReference.this.setupDataOnConnectableApns("cellLocationChanged", null);
                                        HwDcTrackerBaseReference.this.resetTryTimes();
                                    }
                                }
                                isDisconnected = true;
                                HwDcTrackerBaseReference.this.log("CLEARCODE onCellLocationChanged try setup data again");
                                if (!isDisconnected) {
                                }
                                DcTrackerUtils.cleanUpConnection(dcTracker, z, defaultApn);
                                HwDcTrackerBaseReference.this.setupDataOnConnectableApns("cellLocationChanged", null);
                                HwDcTrackerBaseReference.this.resetTryTimes();
                            }
                            return;
                        }
                    }
                    HwDcTrackerBaseReference.this.log("CLEARCODE location not instanceof GsmCellLocation");
                } catch (Exception e) {
                    Rlog.e(HwDcTrackerBaseReference.TAG, "Exception in CellStateHandler.handleMessage:", e);
                }
            }
        }
    };
    private boolean removePreferredApn = true;

    /* renamed from: com.android.internal.telephony.dataconnection.HwDcTrackerBaseReference$6 */
    static /* synthetic */ class AnonymousClass6 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType = new int[AppType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_USIM.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_SIM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_RUIM.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[AppType.APPTYPE_CSIM.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    class AllowMmmsContentObserver extends ContentObserver {
        public AllowMmmsContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            boolean z = false;
            int allowMms = System.getInt(HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getContext().getContentResolver(), HwDcTrackerBaseReference.ENABLE_ALLOW_MMS, 0);
            HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
            if (allowMms == 1) {
                z = true;
            }
            hwDcTrackerBaseReference.ALLOW_MMS = z;
        }
    }

    private class FdnAsyncQueryHandler extends AsyncQueryHandler {
        public FdnAsyncQueryHandler(ContentResolver cr) {
            super(cr);
        }

        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            long subId = (long) HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getSubId();
            boolean isFdnActivated1 = SystemProperties.getBoolean("gsm.hw.fdn.activated1", false);
            boolean isFdnActivated2 = SystemProperties.getBoolean("gsm.hw.fdn.activated2", false);
            HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fddn onQueryComplete subId:");
            stringBuilder.append(subId);
            stringBuilder.append(" ,isFdnActivated1:");
            stringBuilder.append(isFdnActivated1);
            stringBuilder.append(" ,isFdnActivated2:");
            stringBuilder.append(isFdnActivated2);
            hwDcTrackerBaseReference.log(stringBuilder.toString());
            if ((subId == 0 && isFdnActivated1) || (subId == 1 && isFdnActivated2)) {
                HwDcTrackerBaseReference.this.retryDataConnectionByFdn();
            }
        }
    }

    private class FdnChangeObserver extends ContentObserver {
        public FdnChangeObserver() {
            super(HwDcTrackerBaseReference.this.mDcTrackerBase);
        }

        public void onChange(boolean selfChange) {
            HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fddn FdnChangeObserver onChange, selfChange:");
            stringBuilder.append(selfChange);
            hwDcTrackerBaseReference.log(stringBuilder.toString());
            HwDcTrackerBaseReference.this.asyncQueryContact();
        }
    }

    class NwModeContentObserver extends ContentObserver {
        public NwModeContentObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean Change) {
            if (HwDcTrackerBaseReference.isMultiSimEnabled) {
                if (TelephonyManager.getTelephonyProperty(HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getPhoneId(), "gsm.data.gsm_only_not_allow_ps", "false").equals("false")) {
                    return;
                }
            } else if (!SystemProperties.getBoolean("gsm.data.gsm_only_not_allow_ps", false)) {
                return;
            }
            HwDcTrackerBaseReference.this.nwMode = Global.getInt(HwDcTrackerBaseReference.this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "preferred_network_mode", Phone.PREFERRED_NT_MODE);
            HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NwModeChangeObserver onChange nwMode = ");
            stringBuilder.append(HwDcTrackerBaseReference.this.nwMode);
            hwDcTrackerBaseReference.log(stringBuilder.toString());
            if (HwDcTrackerBaseReference.this.mDcTrackerBase instanceof DcTracker) {
                DcTracker dcTracker = HwDcTrackerBaseReference.this.mDcTrackerBase;
                if (1 == HwDcTrackerBaseReference.this.nwMode) {
                    DcTrackerUtils.cleanUpAllConnections(dcTracker, true, "nwTypeChanged");
                } else if (1 == HwDcTrackerBaseReference.this.mNwOldMode) {
                    DcTrackerUtils.onTrySetupData(dcTracker, "nwTypeChanged");
                }
            }
            HwDcTrackerBaseReference.this.mNwOldMode = HwDcTrackerBaseReference.this.nwMode;
        }
    }

    static /* synthetic */ int access$512(HwDcTrackerBaseReference x0, int x1) {
        int i = x0.mDSUseDuration + x1;
        x0.mDSUseDuration = i;
        return i;
    }

    static {
        boolean z = false;
        boolean z2 = "07".equals(SystemProperties.get("ro.config.hw_opta")) && "840".equals(SystemProperties.get("ro.config.hw_optb"));
        IS_ATT = z2;
        if (((MMS_PROP >> 2) & 1) == 1) {
            z = true;
        }
        MMSIgnoreDSSwitchNotRoaming = z;
    }

    public HwDcTrackerBaseReference(DcTracker dcTrackerBase) {
        this.mDcTrackerBase = dcTrackerBase;
    }

    public void init() {
        boolean z = false;
        if (System.getInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), ENABLE_ALLOW_MMS, 0) == 1) {
            z = true;
        }
        this.ALLOW_MMS = z;
        Uri allowMmsUri = System.CONTENT_URI;
        this.allowMmsObserver = new AllowMmmsContentObserver(this.mDcTrackerBase);
        this.mDcTrackerBase.mPhone.getContext().getContentResolver().registerContentObserver(allowMmsUri, true, this.allowMmsObserver);
        this.nwModeChangeObserver = new NwModeContentObserver(this.mDcTrackerBase);
        this.mDcTrackerBase.mPhone.getContext().getContentResolver().registerContentObserver(Global.getUriFor("preferred_network_mode"), true, this.nwModeChangeObserver);
        Phone phone = this.mDcTrackerBase.mPhone;
        this.nwMode = Global.getInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "preferred_network_mode", Phone.PREFERRED_NT_MODE);
        this.mNwOldMode = this.nwMode;
        if (this.mDcTrackerBase.mPhone.getCallTracker() != null) {
            this.mDcTrackerBase.mPhone.getCallTracker().registerForVoiceCallEnded(this.handler, 3, null);
        }
        this.mSubscription = Integer.valueOf(this.mDcTrackerBase.mPhone.getSubId());
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mAlarmManager = (AlarmManager) this.mDcTrackerBase.mPhone.getContext().getSystemService("alarm");
        filter.addAction(INTENT_SET_PREF_NETWORK_TYPE);
        if (!TextUtils.isEmpty(PS_CLEARCODE_PLMN)) {
            this.mDcTrackerBase.mPhone.mCi.registerForLimitPDPAct(this.handler, 4, null);
            filter.addAction(INTENT_LIMIT_PDP_ACT_IND);
        }
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction(ACTION_BT_CONNECTION_CHANGED);
        this.mDcTrackerBase.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mDcTrackerBase.mPhone);
        isSupportPidStatistics();
    }

    public void dispose() {
        if (this.allowMmsObserver != null) {
            this.mDcTrackerBase.mPhone.getContext().getContentResolver().unregisterContentObserver(this.allowMmsObserver);
        }
        if (this.nwModeChangeObserver != null) {
            this.mDcTrackerBase.mPhone.getContext().getContentResolver().unregisterContentObserver(this.nwModeChangeObserver);
        }
        if (this.mDcTrackerBase.mPhone.getCallTracker() != null) {
            this.mDcTrackerBase.mPhone.getCallTracker().unregisterForVoiceCallEnded(this.handler);
        }
        if (!TextUtils.isEmpty(PS_CLEARCODE_PLMN)) {
            this.mDcTrackerBase.mPhone.mCi.unregisterForLimitPDPAct(this.handler);
        }
        if (this.mIntentReceiver != null) {
            this.mDcTrackerBase.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        }
    }

    private void onResetApn() {
        ApnContext apnContext = (ApnContext) this.mDcTrackerBase.mApnContexts.get("default");
        if (apnContext != null) {
            apnContext.setEnabled(true);
            apnContext.setDependencyMet(true);
        }
    }

    public void beforeHandleMessage(Message msg) {
        if (HwFullNetworkConstants.EVENT_DEFAULT_STATE_BASE == msg.what) {
            onResetApn();
        }
    }

    public boolean isDataAllowedByApnContext(ApnContext apnContext) {
        if (isBipApnType(apnContext.getApnType())) {
            return true;
        }
        if (isGsmOnlyPsNotAllowed()) {
            log("in GsmMode not allowed PS!");
            return false;
        } else if (!isLimitPDPAct()) {
            return true;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PSCLEARCODE Limit PDP Act apnContext: ");
            stringBuilder.append(apnContext);
            log(stringBuilder.toString());
            return false;
        }
    }

    public boolean isLimitPDPAct() {
        return this.mIsLimitPDPAct && isPSClearCodeRplmnMatched();
    }

    public boolean isPSClearCodeRplmnMatched() {
        if (!HuaweiTelephonyConfigs.isHisiPlatform() || this.mDcTrackerBase == null || this.mDcTrackerBase.mPhone == null || this.mDcTrackerBase.mPhone.getServiceState() == null) {
            return false;
        }
        String operator = this.mDcTrackerBase.mPhone.getServiceState().getOperatorNumeric();
        if (TextUtils.isEmpty(PS_CLEARCODE_PLMN) || TextUtils.isEmpty(operator)) {
            return false;
        }
        return PS_CLEARCODE_PLMN.contains(operator);
    }

    private boolean isGsmOnlyPsNotAllowed() {
        boolean z = false;
        if (isMultiSimEnabled) {
            int subId = this.mDcTrackerBase.mPhone.getPhoneId();
            int networkMode = this.nwMode;
            if (IS_DUAL_4G_SUPPORTED && SIM_NUM > 1) {
                ContentResolver contentResolver = this.mDcTrackerBase.mPhone.getContext().getContentResolver();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("preferred_network_mode");
                stringBuilder.append(subId);
                String stringBuilder2 = stringBuilder.toString();
                Phone phone = this.mDcTrackerBase.mPhone;
                networkMode = Global.getInt(contentResolver, stringBuilder2, Phone.PREFERRED_NT_MODE);
            }
            if (TelephonyManager.getTelephonyProperty(subId, "gsm.data.gsm_only_not_allow_ps", "false").equals("true") && 1 == networkMode) {
                z = true;
            }
            return z;
        }
        if (SystemProperties.getBoolean("gsm.data.gsm_only_not_allow_ps", false) && 1 == this.nwMode) {
            z = true;
        }
        return z;
    }

    public boolean isDataAllowedForRoaming(boolean isMms) {
        int allowMmsPropertyByPlmn = getallowMmsPropertyByPlmn();
        boolean z = false;
        if (-1 != allowMmsPropertyByPlmn) {
            boolean mmsOnRoaming = (allowMmsPropertyByPlmn & 1) == 1;
            if (!this.mDcTrackerBase.mPhone.getServiceState().getRoaming() || this.mDcTrackerBase.getDataRoamingEnabled() || ((this.ALLOW_MMS || mmsOnRoaming) && isMms)) {
                z = true;
            }
            return z;
        }
        if (!this.mDcTrackerBase.mPhone.getServiceState().getRoaming() || this.mDcTrackerBase.getDataRoamingEnabled() || ((this.ALLOW_MMS || MMS_ON_ROAMING) && isMms)) {
            z = true;
        }
        return z;
    }

    public void onAllApnFirstActiveFailed() {
        if (isMultiSimEnabled) {
            ApnReminder.getInstance(this.mDcTrackerBase.mPhone.getContext(), this.mDcTrackerBase.mPhone.getPhoneId()).allApnActiveFailed();
            return;
        }
        ApnReminder.getInstance(this.mDcTrackerBase.mPhone.getContext()).allApnActiveFailed();
    }

    public void onAllApnPermActiveFailed() {
        if (true == this.broadcastPrePostPay && GlobalParamsAdaptor.getPrePostPayPreCondition()) {
            log("tryToActionPrePostPay.");
            GlobalParamsAdaptor.tryToActionPrePostPay();
            this.broadcastPrePostPay = false;
        }
        ApnReminder.getInstance(this.mDcTrackerBase.mPhone.getContext()).getCust().handleAllApnPermActiveFailed(this.mDcTrackerBase.mPhone.getContext());
    }

    public boolean isBipApnType(String type) {
        if (HuaweiTelephonyConfigs.isModemBipEnable() || (!type.equals("bip0") && !type.equals("bip1") && !type.equals("bip2") && !type.equals("bip3") && !type.equals("bip4") && !type.equals("bip5") && !type.equals("bip6"))) {
            return false;
        }
        return true;
    }

    public ApnSetting fetchBipApn(ApnSetting preferredApn, ArrayList<ApnSetting> allApnSettings) {
        if (!HuaweiTelephonyConfigs.isModemBipEnable()) {
            ApnSetting mDataProfile = ApnSetting.fromString(SystemProperties.get("gsm.bip.apn"));
            if ("default".equals(SystemProperties.get("gsm.bip.apn"))) {
                if (preferredApn != null) {
                    log("find prefer apn, use this");
                    return preferredApn;
                }
                if (allApnSettings != null) {
                    int list_size = allApnSettings.size();
                    for (int i = 0; i < list_size; i++) {
                        ApnSetting apn = (ApnSetting) allApnSettings.get(i);
                        if (apn.canHandleType("default")) {
                            log("find the first default apn");
                            return apn;
                        }
                    }
                }
                log("find non apn for default bip");
                return null;
            } else if (mDataProfile != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fetchBipApn: global BIP mDataProfile=");
                stringBuilder.append(mDataProfile);
                log(stringBuilder.toString());
                return mDataProfile;
            }
        }
        return null;
    }

    private void log(String string) {
        Rlog.d(TAG, string);
    }

    public void setFirstTimeEnableData() {
        log("=PREPOSTPAY=, Data Setup Successful.");
        if (this.broadcastPrePostPay) {
            this.broadcastPrePostPay = false;
        }
    }

    public boolean needRemovedPreferredApn() {
        if (true != this.removePreferredApn || !GlobalParamsAdaptor.getPrePostPayPreCondition()) {
            return false;
        }
        log("Remove preferred apn.");
        this.removePreferredApn = false;
        return true;
    }

    public String getDataRoamingSettingItem(String originItem) {
        if (isMultiSimEnabled && this.mDcTrackerBase.mPhone.getPhoneId() == 1) {
            return DATA_ROAMING_SIM2;
        }
        return originItem;
    }

    public void disableGoogleDunApn(Context c, String apnData, ApnSetting dunSetting) {
        if (SystemProperties.getBoolean("ro.config.enable.gdun", false)) {
            dunSetting = ApnSetting.fromString("this is false");
        }
    }

    public boolean getAnyDataEnabledByApnContext(ApnContext apnContext, boolean enable) {
        int allowMmsPropertyByPlmn = getallowMmsPropertyByPlmn();
        boolean z = false;
        boolean ignoreDSSwitchOnRoaming;
        if (this.mDcTrackerBase.mPhone.getServiceState().getRoaming()) {
            if (getXcapDataRoamingEnable() && "xcap".equals(apnContext.getApnType())) {
                return true;
            }
            if (-1 != allowMmsPropertyByPlmn) {
                ignoreDSSwitchOnRoaming = ((allowMmsPropertyByPlmn >> 1) & 1) == 1;
                if (((this.ALLOW_MMS || ignoreDSSwitchOnRoaming) && "mms".equals(apnContext.getApnType())) || enable) {
                    z = true;
                }
                return z;
            }
            if (((this.ALLOW_MMS || MMSIgnoreDSSwitchOnRoaming) && "mms".equals(apnContext.getApnType())) || enable) {
                z = true;
            }
            return z;
        } else if (-1 != allowMmsPropertyByPlmn) {
            ignoreDSSwitchOnRoaming = ((allowMmsPropertyByPlmn >> 2) & 1) == 1;
            if (((this.ALLOW_MMS || ignoreDSSwitchOnRoaming) && "mms".equals(apnContext.getApnType())) || enable) {
                z = true;
            }
            return z;
        } else {
            if (((this.ALLOW_MMS || MMSIgnoreDSSwitchNotRoaming) && "mms".equals(apnContext.getApnType())) || enable) {
                z = true;
            }
            return z;
        }
    }

    public boolean shouldDisableMultiPdps(boolean onlySingleDcAllowed) {
        if (!(this.SUPPORT_MPDN || SystemProperties.getBoolean("gsm.multipdp.plmn.matched", false))) {
            onlySingleDcAllowed = true;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SUPPORT_MPDN: ");
            stringBuilder.append(this.SUPPORT_MPDN);
            log(stringBuilder.toString());
        }
        if (isMultiSimEnabled) {
            int subId = this.mDcTrackerBase.mPhone.getPhoneId();
            if (subId == 0) {
                SystemProperties.set("gsm.check_is_single_pdp_sub1", Boolean.toString(onlySingleDcAllowed));
            } else if (subId == 1) {
                SystemProperties.set("gsm.check_is_single_pdp_sub2", Boolean.toString(onlySingleDcAllowed));
            }
        } else {
            SystemProperties.set("gsm.check_is_single_pdp", Boolean.toString(onlySingleDcAllowed));
        }
        return onlySingleDcAllowed;
    }

    public void setMPDN(boolean bMPDN) {
        if (bMPDN == this.SUPPORT_MPDN) {
            log("MPDN is same,Don't need change");
            return;
        }
        if (bMPDN) {
            int radioTech = this.mDcTrackerBase.mPhone.getServiceState().getRilDataRadioTechnology();
            if (ServiceState.isCdma(radioTech) && radioTech != 13) {
                log("technology is not EHRPD and ServiceState is CDMA,Can't set MPDN");
                return;
            }
        }
        this.SUPPORT_MPDN = bMPDN;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SUPPORT_MPDN change to ");
        stringBuilder.append(bMPDN);
        log(stringBuilder.toString());
    }

    public void setMPDNByNetWork(String plmnNetWork) {
        if (this.mDcTrackerBase.mPhone == null) {
            log("mPhone is null");
            return;
        }
        String plmnsConfig = System.getString(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "mpdn_plmn_matched_by_network");
        if (TextUtils.isEmpty(plmnsConfig)) {
            log("plmnConfig is Empty");
            return;
        }
        boolean bMPDN = false;
        for (String plmn : plmnsConfig.split(",")) {
            if (!TextUtils.isEmpty(plmn) && plmn.equals(plmnNetWork)) {
                bMPDN = true;
                break;
            }
        }
        setMPDN(bMPDN);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMpdnByNewNetwork done, bMPDN is ");
        stringBuilder.append(bMPDN);
        log(stringBuilder.toString());
    }

    public String getCTOperatorNumeric(String operator) {
        String result = operator;
        if (!HuaweiTelephonyConfigs.isChinaTelecom() || this.mDcTrackerBase.mPhone.getPhoneId() != 0) {
            return result;
        }
        result = CT_CDMA_OPERATOR;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCTOperatorNumeric: use china telecom operator=");
        stringBuilder.append(result);
        log(stringBuilder.toString());
        return result;
    }

    public ApnSetting makeHwApnSetting(Cursor cursor, String[] types) {
        return new HwApnSetting(cursor, types);
    }

    public boolean noNeedDoRecovery(ConcurrentHashMap mApnContexts) {
        return SystemProperties.getBoolean("persist.radio.hw.nodorecovery", false) || (SystemProperties.getBoolean("hw.ds.np.nopollstat", true) && !isActiveDefaultApnPreset(mApnContexts));
    }

    public boolean isActiveDefaultApnPreset(ConcurrentHashMap<String, ApnContext> mApnContexts) {
        ApnContext apnContext = (ApnContext) mApnContexts.get("default");
        if (apnContext != null && State.CONNECTED == apnContext.getState()) {
            ApnSetting apnSetting = apnContext.getApnSetting();
            if (apnSetting != null && (apnSetting instanceof HwApnSetting)) {
                HwApnSetting hwapnSetting = (HwApnSetting) apnSetting;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("current default apn is ");
                stringBuilder.append(hwapnSetting.isPreset() ? "preset" : "non-preset");
                log(stringBuilder.toString());
                return hwapnSetting.isPreset();
            }
        }
        return true;
    }

    public boolean isApnPreset(ApnSetting apnSetting) {
        if (apnSetting == null || !(apnSetting instanceof HwApnSetting)) {
            return true;
        }
        return ((HwApnSetting) apnSetting).isPreset();
    }

    public void getNetdPid() {
        int ret = -1;
        if (this.mDoRecoveryAddDnsProp) {
            try {
                this.mNetworkManager = Stub.asInterface(ServiceManager.getService("network_management"));
                if (this.mNetworkManager != null) {
                    ret = this.mNetworkManager.getNetdPid();
                } else {
                    log("getNetdPid mNetdService is null");
                }
            } catch (RemoteException e) {
                log("getNetdPid mNetdService RemoteException");
            }
            this.netdPid = ret;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getNetdPid:");
        stringBuilder.append(ret);
        stringBuilder.append(",prop:");
        stringBuilder.append(this.mDoRecoveryAddDnsProp);
        log(stringBuilder.toString());
    }

    public void isSupportPidStatistics() {
        if (!this.mDoRecoveryAddDnsProp) {
            return;
        }
        if (new File(pidStatsPath).exists()) {
            this.isSupportPidStats = true;
        } else {
            this.isSupportPidStats = false;
        }
    }

    private long parseLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long[] getDnsPacketTxRxSum() {
        int i = 2;
        long[] ret = new long[]{0, 0};
        BufferedReader bReader = null;
        FileInputStream fis = null;
        if (!this.isSupportPidStats || this.netdPid == -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSupportPidStats=");
            stringBuilder.append(this.isSupportPidStats);
            stringBuilder.append(",netdPid=");
            stringBuilder.append(this.netdPid);
            log(stringBuilder.toString());
        } else {
            try {
                fis = new FileInputStream(pidStatsPath);
                bReader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
                long udpTx = 0;
                long udpRx = 0;
                String netdPidKey = new StringBuilder();
                netdPidKey.append(":");
                netdPidKey.append(String.valueOf(this.netdPid));
                netdPidKey.append("_");
                netdPidKey = netdPidKey.toString();
                String[] allMobiles = TrafficStats.getMobileIfacesEx();
                while (true) {
                    String readLine = bReader.readLine();
                    String line = readLine;
                    int i2 = 1;
                    if (readLine == null) {
                        break;
                    }
                    String[] tokens = line.split(" ");
                    if (tokens.length > 20 && tokens[3].equals(NETD_PROCESS_UID) && (tokens[i].equals("netd") || tokens[i].contains(netdPidKey))) {
                        int length = allMobiles.length;
                        long udpRx2 = udpRx;
                        long udpTx2 = udpTx;
                        int udpTx3 = 0;
                        while (udpTx3 < length) {
                            if (tokens[i2].equals(allMobiles[udpTx3])) {
                                udpTx2 += parseLong(tokens[20]);
                                udpRx2 += parseLong(tokens[14]);
                            }
                            udpTx3++;
                            i2 = 1;
                        }
                        udpTx = udpTx2;
                        udpRx = udpRx2;
                    }
                    i = 2;
                }
                ret[0] = ret[0] + udpTx;
                ret[1] = ret[1] + udpRx;
                try {
                    bReader.close();
                    fis.close();
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                log("pidStatsPath not found");
                if (bReader != null) {
                    bReader.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (Throwable th) {
                FileInputStream fis2 = fis;
                BufferedReader fis3 = bReader;
                Throwable bReader2 = th;
                if (fis3 != null) {
                    try {
                        fis3.close();
                    } catch (IOException e3) {
                    }
                }
                if (fis2 != null) {
                    fis2.close();
                }
            }
        }
        return ret;
    }

    public HwCustDcTracker getCust(DcTracker dcTracker) {
        return (HwCustDcTracker) HwCustUtils.createObj(HwCustDcTracker.class, new Object[]{dcTracker});
    }

    public void setupDataOnConnectableApns(String reason, String excludedApnType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setupDataOnConnectableApns: ");
        stringBuilder.append(reason);
        stringBuilder.append(", excludedApnType = ");
        stringBuilder.append(excludedApnType);
        log(stringBuilder.toString());
        Iterator it = this.mDcTrackerBase.mPrioritySortedApnContexts.iterator();
        while (it.hasNext()) {
            ApnContext apnContext = (ApnContext) it.next();
            if (TextUtils.isEmpty(excludedApnType) || !excludedApnType.equals(apnContext.getApnType())) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setupDataOnConnectableApns: apnContext ");
                stringBuilder2.append(apnContext);
                log(stringBuilder2.toString());
                if (apnContext.getState() == State.FAILED) {
                    apnContext.setState(State.IDLE);
                }
                if (apnContext.isConnectable()) {
                    log("setupDataOnConnectableApns: isConnectable() call trySetupData");
                    apnContext.setReason(reason);
                    this.mDcTrackerBase.onTrySetupData(apnContext);
                }
            }
        }
    }

    public boolean needRetryAfterDisconnected(DcFailCause cause) {
        String failCauseStr = "";
        if (DcFailCause.ERROR_UNSPECIFIED != cause) {
            return true;
        }
        failCauseStr = SystemProperties.get("ril.ps_ce_reason", "");
        if (TextUtils.isEmpty(failCauseStr)) {
            return true;
        }
        for (String noRetryCause : CAUSE_NO_RETRY_AFTER_DISCONNECT.split(",")) {
            if (failCauseStr.equals(noRetryCause)) {
                return false;
            }
        }
        return true;
    }

    public void setRetryAfterDisconnectedReason(DataConnection dc, ArrayList<ApnContext> apnsToCleanup) {
        for (ApnContext apnContext : dc.mApnContexts.keySet()) {
            apnContext.setReason("noRetryAfterDisconnect");
        }
        apnsToCleanup.addAll(dc.mApnContexts.keySet());
    }

    public boolean isChinaTelecom(int slotId) {
        return HwTelephonyManagerInner.getDefault().isChinaTelecom(slotId);
    }

    public boolean isFullNetworkSupported() {
        return HwTelephonyManagerInner.getDefault().isFullNetworkSupported();
    }

    public boolean isCTSimCard(int slotId) {
        return HwTelephonyManagerInner.getDefault().isCTSimCard(slotId);
    }

    public int getDefault4GSlotId() {
        return HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
    }

    public boolean isCTDualModeCard(int sub) {
        int SubType = HwTelephonyManagerInner.getDefault().getCardType(sub);
        if (41 != SubType && 43 != SubType) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sub = ");
        stringBuilder.append(sub);
        stringBuilder.append(", SubType = ");
        stringBuilder.append(SubType);
        stringBuilder.append(" is CT dual modem card");
        log(stringBuilder.toString());
        return true;
    }

    private boolean isInChina() {
        String mcc = null;
        String operatorNumeric = ((TelephonyManager) this.mDcTrackerBase.mPhone.getContext().getSystemService("phone")).getNetworkOperatorForPhone(this.mDcTrackerBase.mPhone.getSubId());
        if (operatorNumeric != null && operatorNumeric.length() > 3) {
            mcc = operatorNumeric.substring(0, 3);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isInChina current mcc = ");
            stringBuilder.append(mcc);
            debugLog(stringBuilder.toString());
        }
        if ("460".equals(mcc)) {
            return true;
        }
        return false;
    }

    public boolean isPingOk() {
        boolean ret = false;
        if (HwVSimUtils.isVSimOn()) {
            debugLog("isPineOk always ok for vsim on");
            return true;
        } else if (noNeedDoRecovery(this.mDcTrackerBase.mApnContexts) || this.mDcTrackerBase.mPhone.getServiceState().getDataRegState() != 0) {
            debugLog("isPineOk always false if not default apn or dataRegState not in service");
            return false;
        } else {
            try {
                String pingBeforeDorecovery = SystemProperties.get("ro.sys.ping_bf_dorecovery", "false");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isPingOk pingBeforeDorecovery = ");
                stringBuilder.append(pingBeforeDorecovery);
                debugLog(stringBuilder.toString());
                Thread pingThread = new Thread(new Runnable() {
                    public void run() {
                        String result = "";
                        String serverName = "connectivitycheck.platform.hicloud.com";
                        if (!HwDcTrackerBaseReference.this.isInChina()) {
                            serverName = "www.google.com";
                        }
                        HwDcTrackerBaseReference hwDcTrackerBaseReference = HwDcTrackerBaseReference.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("ping thread enter, server name = ");
                        stringBuilder.append(serverName);
                        hwDcTrackerBaseReference.debugLog(stringBuilder.toString());
                        try {
                            String readLine;
                            HwDcTrackerBaseReference.this.pingThreadlLock.lock();
                            HwDcTrackerBaseReference.this.isRecievedPingReply = false;
                            HwDcTrackerBaseReference.this.pingThreadlLock.unlock();
                            HwDcTrackerBaseReference.this.debugLog("pingThread begin to ping");
                            Runtime runtime = Runtime.getRuntime();
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("/system/bin/ping -c 1 -W 1 ");
                            stringBuilder2.append(serverName);
                            Process process = runtime.exec(stringBuilder2.toString());
                            int status = process.waitFor();
                            HwDcTrackerBaseReference hwDcTrackerBaseReference2 = HwDcTrackerBaseReference.this;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("pingThread, process.waitFor, status = ");
                            stringBuilder3.append(status);
                            hwDcTrackerBaseReference2.debugLog(stringBuilder3.toString());
                            BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            StringBuffer stringBuffer = new StringBuffer();
                            String line = "";
                            while (true) {
                                readLine = buf.readLine();
                                line = readLine;
                                if (readLine == null) {
                                    break;
                                }
                                stringBuffer.append(line);
                                stringBuffer.append("\r\n");
                            }
                            readLine = stringBuffer.toString();
                            buf.close();
                            HwDcTrackerBaseReference hwDcTrackerBaseReference3 = HwDcTrackerBaseReference.this;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("ping result:");
                            stringBuilder4.append(readLine);
                            hwDcTrackerBaseReference3.debugLog(stringBuilder4.toString());
                            HwDcTrackerBaseReference.this.debugLog("pingThread pingThreadlLock.lock");
                            HwDcTrackerBaseReference.this.pingThreadlLock.lock();
                            if (status != 0 || readLine.indexOf("1 packets transmitted, 1 received") < 0) {
                                HwDcTrackerBaseReference.this.isRecievedPingReply = false;
                            } else {
                                HwDcTrackerBaseReference.this.isRecievedPingReply = true;
                            }
                            HwDcTrackerBaseReference.this.pingCondition.signal();
                            HwDcTrackerBaseReference hwDcTrackerBaseReference4 = HwDcTrackerBaseReference.this;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("pingThread pingThreadlLock.unlock, ping thread return ");
                            stringBuilder5.append(HwDcTrackerBaseReference.this.isRecievedPingReply);
                            hwDcTrackerBaseReference4.debugLog(stringBuilder5.toString());
                            HwDcTrackerBaseReference.this.pingThreadlLock.unlock();
                        } catch (Exception e) {
                            Rlog.e(HwDcTrackerBaseReference.TAG, "ping thread Exception: ", e);
                        }
                    }
                }, "ping thread");
                debugLog("isPingOk pingThreadlLock.lock");
                this.pingThreadlLock.lock();
                pingThread.start();
                this.pingCondition.await(1100, TimeUnit.MILLISECONDS);
                ret = this.isRecievedPingReply;
                this.pingThreadlLock.unlock();
                debugLog("isPingOk pingThreadlLock.unlock");
            } catch (Exception e) {
                Rlog.e(TAG, "isPingOk Exception: ", e);
            }
            return ret;
        }
    }

    public boolean isClearCodeEnabled() {
        return this.mIsClearCodeEnabled;
    }

    public void startListenCellLocationChange() {
        ((TelephonyManager) this.mDcTrackerBase.mPhone.getContext().getSystemService("phone")).listen(this.pslForCellLocation, 16);
    }

    public void stopListenCellLocationChange() {
        ((TelephonyManager) this.mDcTrackerBase.mPhone.getContext().getSystemService("phone")).listen(this.pslForCellLocation, 0);
    }

    public void operateClearCodeProcess(ApnContext apnContext, DcFailCause cause, int delay) {
        this.mDelayTime = delay;
        if (cause.isPermanentFailure(this.mDcTrackerBase.mPhone.getContext(), this.mDcTrackerBase.mPhone.getSubId())) {
            log("CLEARCODE isPermanentFailure,perhaps APN is wrong");
            boolean isClearcodeDcFailCause = cause == DcFailCause.SERVICE_OPTION_NOT_SUBSCRIBED || cause == DcFailCause.USER_AUTHENTICATION;
            if ("default".equals(apnContext.getApnType()) && isClearcodeDcFailCause) {
                this.mTryIndex++;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CLEARCODE mTryIndex increase,current mTryIndex = ");
                stringBuilder.append(this.mTryIndex);
                log(stringBuilder.toString());
                if (this.mTryIndex >= 3) {
                    if (isLteRadioTech()) {
                        this.mDcTrackerBase.mPhone.setPreferredNetworkType(3, null);
                        Global.putInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "preferred_network_mode", 3);
                        this.mGsmServiceStateTracker = this.mDcTrackerBase.mPhone.getServiceStateTracker();
                        HwServiceStateManager.getHwGsmServiceStateManager(this.mGsmServiceStateTracker, (GsmCdmaPhone) this.mDcTrackerBase.mPhone).setRac(-1);
                        log("CLEARCODE mTryIndex >= 3 and is LTE,switch 4G to 3G and set newrac to -1");
                    } else {
                        log("CLEARCODE mTryIndex >= 3 and is 3G,show clearcode dialog");
                        if (this.mPSClearCodeDialog == null) {
                            this.mPSClearCodeDialog = createPSClearCodeDiag(cause);
                            if (this.mPSClearCodeDialog != null) {
                                this.mPSClearCodeDialog.show();
                            }
                        }
                        set2HourDelay();
                    }
                    this.mTryIndex = 0;
                    apnContext.markApnPermanentFailed(apnContext.getApnSetting());
                }
            } else {
                this.mTryIndex = 0;
                apnContext.markApnPermanentFailed(apnContext.getApnSetting());
            }
            return;
        }
        this.mTryIndex = 0;
        log("CLEARCODE not isPermanentFailure ");
    }

    public void resetTryTimes() {
        if (isClearCodeEnabled()) {
            this.mTryIndex = 0;
            if (this.mAlarmManager != null && this.mAlarmIntent != null) {
                this.mAlarmManager.cancel(this.mAlarmIntent);
                log("CLEARCODE cancel Alarm resetTryTimes");
            }
        }
    }

    private boolean isLteRadioTech() {
        if (this.mDcTrackerBase.mPhone.getServiceState().getRilDataRadioTechnology() == 14) {
            return true;
        }
        return false;
    }

    public void setCurFailCause(AsyncResult ar) {
        if (!isClearCodeEnabled()) {
            return;
        }
        if (ar.result instanceof DcFailCause) {
            this.mCurFailCause = (DcFailCause) ar.result;
        } else {
            this.mCurFailCause = null;
        }
    }

    private AlertDialog createPSClearCodeDiag(DcFailCause cause) {
        Builder buider = new Builder(this.mDcTrackerBase.mPhone.getContext(), this.mDcTrackerBase.mPhone.getContext().getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null));
        if (cause == DcFailCause.USER_AUTHENTICATION) {
            buider.setMessage(33685827);
            log("CLEARCODE clear_code_29");
        } else if (cause != DcFailCause.SERVICE_OPTION_NOT_SUBSCRIBED) {
            return null;
        } else {
            buider.setMessage(33685828);
            log("CLEARCODE clear_code_33");
        }
        buider.setIcon(17301543);
        buider.setCancelable(false);
        buider.setPositiveButton("Aceptar", new OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                HwDcTrackerBaseReference.this.mPSClearCodeDialog = null;
            }
        });
        AlertDialog dialog = buider.create();
        dialog.getWindow().setType(HwFullNetworkConstants.EVENT_RESET_OOS_FLAG);
        return dialog;
    }

    private void set2HourDelay() {
        int delayTime = SystemProperties.getInt("gsm.radio.debug.cause_delay", DELAY_2_HOUR);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CLEARCODE dataRadioTech is 3G and mTryIndex >= 3,so set2HourDelay delayTime =");
        stringBuilder.append(delayTime);
        log(stringBuilder.toString());
        Intent intent = new Intent(INTENT_SET_PREF_NETWORK_TYPE);
        intent.putExtra(INTENT_SET_PREF_NETWORK_TYPE_EXTRA_TYPE, 9);
        this.mAlarmIntent = PendingIntent.getBroadcast(this.mDcTrackerBase.mPhone.getContext(), 0, intent, 134217728);
        if (this.mAlarmManager != null) {
            this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + ((long) delayTime), this.mAlarmIntent);
        }
    }

    public int getDelayTime() {
        if (this.mCurFailCause == DcFailCause.SERVICE_OPTION_NOT_SUBSCRIBED || this.mCurFailCause == DcFailCause.USER_AUTHENTICATION) {
            if (isLteRadioTech()) {
                this.mDelayTime = PS_CLEARCODE_APN_DELAY_DEFAULT_MILLIS_4G;
            } else {
                this.mDelayTime = PS_CLEARCODE_APN_DELAY_DEFAULT_MILLIS_NOT_4G;
            }
        }
        return this.mDelayTime;
    }

    protected void onActionIntentSetNetworkType(Intent intent) {
        int networkType = intent.getIntExtra(INTENT_SET_PREF_NETWORK_TYPE_EXTRA_TYPE, 9);
        int curPrefMode = Global.getInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "preferred_network_mode", networkType);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CLEARCODE switch network type : ");
        stringBuilder.append(networkType);
        stringBuilder.append(" curPrefMode = ");
        stringBuilder.append(curPrefMode);
        log(stringBuilder.toString());
        if (!(networkType == curPrefMode || curPrefMode == 2)) {
            this.mDcTrackerBase.mPhone.setPreferredNetworkType(networkType, null);
            log("CLEARCODE switch network type to 4G and set newRac to -1");
            Global.putInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "preferred_network_mode", networkType);
            this.mGsmServiceStateTracker = this.mDcTrackerBase.mPhone.getServiceStateTracker();
            HwServiceStateManager.getHwGsmServiceStateManager(this.mGsmServiceStateTracker, (GsmCdmaPhone) this.mDcTrackerBase.mPhone).setRac(-1);
        }
        ApnContext defaultApn = (ApnContext) this.mDcTrackerBase.mApnContexts.get("default");
        boolean z = true;
        boolean isDisconnected = defaultApn.getState() == State.IDLE || defaultApn.getState() == State.FAILED;
        log("CLEARCODE 2 hours of delay is over,try setup data");
        DcTracker dcTracker = this.mDcTrackerBase;
        if (isDisconnected) {
            z = false;
        }
        DcTrackerUtils.cleanUpConnection(dcTracker, z, defaultApn);
        setupDataOnConnectableApns(CLEARCODE_2HOUR_DELAY_OVER, null);
    }

    public void unregisterForImsiReady(IccRecords r) {
        r.unregisterForImsiReady(this.mDcTrackerBase);
    }

    public void registerForImsiReady(IccRecords r) {
        r.registerForImsiReady(this.mDcTrackerBase, 270338, null);
    }

    public void unregisterForRecordsLoaded(IccRecords r) {
        r.unregisterForRecordsLoaded(this.mDcTrackerBase);
    }

    public void registerForRecordsLoaded(IccRecords r) {
        r.registerForRecordsLoaded(this.mDcTrackerBase, 270338, null);
    }

    public void registerForGetAdDone(UiccCardApplication newUiccApplication) {
        newUiccApplication.registerForGetAdDone(this.mDcTrackerBase, 270338, null);
    }

    public void unregisterForGetAdDone(UiccCardApplication newUiccApplication) {
        newUiccApplication.unregisterForGetAdDone(this.mDcTrackerBase);
    }

    public void registerForImsi(UiccCardApplication newUiccApplication, IccRecords newIccRecords) {
        if (!TextUtils.isEmpty(PS_CLEARCODE_PLMN) || this.SETAPN_UNTIL_CARDLOADED) {
            newIccRecords.registerForRecordsLoaded(this.mDcTrackerBase, 270338, null);
            return;
        }
        switch (AnonymousClass6.$SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppType[newUiccApplication.getType().ordinal()]) {
            case 1:
            case 2:
                log("New USIM records found");
                newUiccApplication.registerForGetAdDone(this.mDcTrackerBase, 271144, null);
                break;
            case 3:
            case 4:
                log("New CSIM records found");
                newIccRecords.registerForImsiReady(this.mDcTrackerBase, 271144, null);
                break;
            default:
                log("New other records found");
                break;
        }
        newIccRecords.registerForRecordsLoaded(this.mDcTrackerBase, 270338, null);
    }

    public boolean checkMvnoParams() {
        boolean result = false;
        String operator = this.mDcTrackerBase.getCTOperator(this.mDcTrackerBase.getOperatorNumeric());
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(this.mDcTrackerBase.mPhone.getSubId()))) {
                operator = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(this.mDcTrackerBase.mPhone.getSubId()));
            }
        } else if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
            operator = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
        }
        if (operator != null) {
            String selection = new StringBuilder();
            selection.append("numeric = '");
            selection.append(operator);
            selection.append("'");
            selection = selection.toString();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkMvnoParams: selection=");
            stringBuilder.append(selection);
            log(stringBuilder.toString());
            Cursor cursor = this.mDcTrackerBase.mPhone.getContext().getContentResolver().query(Carriers.CONTENT_URI, null, selection, null, "_id");
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    result = checkMvno(cursor);
                }
                cursor.close();
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("checkMvnoParams: X result = ");
        stringBuilder2.append(result);
        log(stringBuilder2.toString());
        return result;
    }

    private boolean checkMvno(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                String mvnoType = cursor.getString(cursor.getColumnIndexOrThrow("mvno_type"));
                String mvnoMatchData = cursor.getString(cursor.getColumnIndexOrThrow("mvno_match_data"));
                if (!TextUtils.isEmpty(mvnoType) && !TextUtils.isEmpty(mvnoMatchData)) {
                    log("checkMvno: X has mvno paras");
                    return true;
                }
            } while (cursor.moveToNext());
        }
        return false;
    }

    public void registerForFdnRecordsLoaded(IccRecords r) {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            r.registerForFdnRecordsLoaded(this.mDcTrackerBase, 2, null);
        }
    }

    public void unregisterForFdnRecordsLoaded(IccRecords r) {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            r.unregisterForFdnRecordsLoaded(this.mDcTrackerBase);
        }
    }

    public void registerForFdn() {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            log("registerForFdn");
            this.mUiccController.registerForFdnStatusChange(this.mDcTrackerBase, 1, null);
            this.mFdnChangeObserver = new FdnChangeObserver();
            ContentResolver cr = this.mDcTrackerBase.mPhone.getContext().getContentResolver();
            cr.registerContentObserver(FDN_URL, true, this.mFdnChangeObserver);
            this.mFdnAsyncQuery = new FdnAsyncQueryHandler(cr);
        }
    }

    public void unregisterForFdn() {
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            log("unregisterForFdn");
            this.mUiccController.unregisterForFdnStatusChange(this.mDcTrackerBase);
            if (this.mFdnChangeObserver != null) {
                this.mDcTrackerBase.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mFdnChangeObserver);
            }
        }
    }

    public boolean isPsAllowedByFdn() {
        long curSubId = (long) this.mDcTrackerBase.mPhone.getSubId();
        String isFdnActivated1 = SystemProperties.get("gsm.hw.fdn.activated1", "false");
        String isFdnActivated2 = SystemProperties.get("gsm.hw.fdn.activated2", "false");
        String isPSAllowedByFdn1 = SystemProperties.get("gsm.hw.fdn.ps.flag.exists1", "false");
        String isPSAllowedByFdn2 = SystemProperties.get("gsm.hw.fdn.ps.flag.exists2", "false");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fddn isPSAllowedByFdn ,isFdnActivated1:");
        stringBuilder.append(isFdnActivated1);
        stringBuilder.append(" ,isFdnActivated2:");
        stringBuilder.append(isFdnActivated2);
        stringBuilder.append(" ,isPSAllowedByFdn1:");
        stringBuilder.append(isPSAllowedByFdn1);
        stringBuilder.append(" ,isPSAllowedByFdn2:");
        stringBuilder.append(isPSAllowedByFdn2);
        log(stringBuilder.toString());
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            if (curSubId == 0 && "true".equals(isFdnActivated1) && "false".equals(isPSAllowedByFdn1)) {
                return false;
            }
            if (curSubId == 1 && "true".equals(isFdnActivated2) && "false".equals(isPSAllowedByFdn2)) {
                return false;
            }
        }
        return true;
    }

    public void handleCustMessage(Message msg) {
        switch (msg.what) {
            case 1:
            case 2:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fddn msg.what = ");
                stringBuilder.append(msg.what);
                log(stringBuilder.toString());
                retryDataConnectionByFdn();
                return;
            default:
                return;
        }
    }

    public void retryDataConnectionByFdn() {
        if (this.mDcTrackerBase.mPhone.getSubId() != SubscriptionController.getInstance().getCurrentDds()) {
            log("fddn retryDataConnectionByFdn, not dds sub, do nothing.");
            return;
        }
        if (isPsAllowedByFdn()) {
            log("fddn retryDataConnectionByFdn, FDN status change and PS is enable, try setup data.");
            setupDataOnConnectableApns("psRestrictDisabled", null);
        } else {
            log("fddn retryDataConnectionByFdn, PS restricted by FDN, cleaup all connections.");
            this.mDcTrackerBase.cleanUpAllConnections(true, "psRestrictEnabled");
        }
    }

    private void asyncQueryContact() {
        long subId = (long) this.mDcTrackerBase.mPhone.getSubId();
        if (HuaweiTelephonyConfigs.isPsRestrictedByFdn()) {
            this.mFdnAsyncQuery.startQuery(0, null, ContentUris.withAppendedId(FDN_URL, subId), new String[]{"number"}, null, null, null);
        }
    }

    public boolean isActiveDataSubscription() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isActiveDataSubscription getSubId= ");
        stringBuilder.append(this.mDcTrackerBase.mPhone.getSubId());
        stringBuilder.append("mCurrentDds");
        stringBuilder.append(SubscriptionController.getInstance().getCurrentDds());
        log(stringBuilder.toString());
        return this.mDcTrackerBase.mPhone.getSubId() == SubscriptionController.getInstance().getCurrentDds();
    }

    public int get4gSlot() {
        int slot = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        if (slot == 0) {
        }
        return slot;
    }

    public int get2gSlot() {
        return HwTelephonyManagerInner.getDefault().getDefault4GSlotId() == 0 ? 1 : 0;
    }

    public void addIfacePhoneHashMap(DcAsyncChannel dcac, HashMap<String, Integer> mIfacePhoneHashMap) {
        LinkProperties tempLinkProperties = dcac.getLinkPropertiesSync();
        if (tempLinkProperties != null) {
            String iface = tempLinkProperties.getInterfaceName();
            if (iface != null) {
                mIfacePhoneHashMap.put(iface, Integer.valueOf(this.mDcTrackerBase.mPhone.getPhoneId()));
            }
        }
    }

    public int getVSimSubId() {
        return HwVSimManager.getDefault().getVSimSubId();
    }

    public void sendRoamingDataStatusChangBroadcast() {
        this.mDcTrackerBase.mPhone.getContext().sendBroadcast(new Intent("com.android.huawei.INTERNATIONAL_ROAMING_DATA_STATUS_CHANGED"));
    }

    public void sendDSMipErrorBroadcast() {
        if (SystemProperties.getBoolean("ro.config.hw_mip_error_dialog", false)) {
            this.mDcTrackerBase.mPhone.getContext().sendBroadcast(new Intent("com.android.huawei.DATA_CONNECTION_MOBILE_IP_ERROR"));
        }
    }

    public boolean enableTcpUdpSumForDataStall() {
        return SystemProperties.getBoolean("ro.hwpp_enable_tcp_udp_sum", false);
    }

    public String networkTypeToApnType(int networkType) {
        if (networkType == 0) {
            return "default";
        }
        if (networkType == 48) {
            return "internaldefault";
        }
        switch (networkType) {
            case 2:
                return "mms";
            case 3:
                return "supl";
            case 4:
                return "dun";
            case 5:
                return "hipri";
            default:
                switch (networkType) {
                    case 10:
                        return "fota";
                    case 11:
                        return "ims";
                    case 12:
                        return "cbs";
                    default:
                        switch (networkType) {
                            case 14:
                                return "ia";
                            case 15:
                                return "emergency";
                            default:
                                switch (networkType) {
                                    case 38:
                                        return "bip0";
                                    case 39:
                                        return "bip1";
                                    case 40:
                                        return "bip2";
                                    case 41:
                                        return "bip3";
                                    case 42:
                                        return "bip4";
                                    case 43:
                                        return "bip5";
                                    case 44:
                                        return "bip6";
                                    case HwVSimConstants.EVENT_CARD_POWER_ON_DONE /*45*/:
                                        return "xcap";
                                    default:
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Error mapping networkType ");
                                        stringBuilder.append(networkType);
                                        stringBuilder.append(" to apnType");
                                        log(stringBuilder.toString());
                                        return "";
                                }
                        }
                }
        }
    }

    public boolean isApnTypeDisabled(String apnType) {
        if (TextUtils.isEmpty(apnType)) {
            return false;
        }
        for (String type : "ro.hwpp.disabled_apn_type".split(",")) {
            if (apnType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNeedDataRoamingExpend() {
        if (this.mDcTrackerBase.mPhone == null || this.mDcTrackerBase.mPhone.mIccRecords == null || this.mDcTrackerBase.mPhone.mIccRecords.get() == null) {
            log("mPhone or mIccRecords is null");
            return false;
        }
        boolean dataRoamState = false;
        boolean hasHwCfgConfig = false;
        try {
            Boolean dataRoam = (Boolean) HwCfgFilePolicy.getValue("hw_data_roam_option", SubscriptionManager.getSlotIndex(this.mDcTrackerBase.mPhone.getSubId()), Boolean.class);
            if (dataRoam != null) {
                dataRoamState = dataRoam.booleanValue();
                hasHwCfgConfig = true;
            }
            if (dataRoamState) {
                return true;
            }
            if (hasHwCfgConfig && !dataRoamState) {
                return false;
            }
            String plmnsConfig = System.getString(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "hw_data_roam_option");
            if (TextUtils.isEmpty(plmnsConfig)) {
                log("plmnConfig is Empty");
                return false;
            } else if ("ALL".equals(plmnsConfig)) {
                return true;
            } else {
                String mccmnc = ((IccRecords) this.mDcTrackerBase.mPhone.mIccRecords.get()).getOperatorNumeric();
                for (String plmn : plmnsConfig.split(",")) {
                    if (!TextUtils.isEmpty(plmn) && plmn.equals(mccmnc)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception e) {
            Rlog.e(TAG, "read data_roam_option error : ", e);
        }
    }

    public boolean setDataRoamingScope(int scope) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dram setDataRoamingScope scope ");
        stringBuilder.append(scope);
        log(stringBuilder.toString());
        if (scope < 0 || scope > 2) {
            return false;
        }
        if (getDataRoamingScope() != scope) {
            Global.putInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), getDataRoamingSettingItem("data_roaming"), scope);
            if (this.mDcTrackerBase.mPhone.getServiceState() != null && this.mDcTrackerBase.mPhone.getServiceState().getRoaming()) {
                log("dram setDataRoamingScope send EVENT_ROAMING_ON");
                this.mDcTrackerBase.sendMessage(this.mDcTrackerBase.obtainMessage(270347));
            }
        }
        return true;
    }

    public int getDataRoamingScope() {
        try {
            return Global.getInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), getDataRoamingSettingItem("data_roaming"));
        } catch (SettingNotFoundException e) {
            return -1;
        }
    }

    public boolean getDataRoamingEnabledWithNational() {
        boolean result = true;
        int dataRoamingScope = getDataRoamingScope();
        if (dataRoamingScope == 0 || (1 == dataRoamingScope && true == isInternationalRoaming())) {
            result = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dram getDataRoamingEnabledWithNational result ");
        stringBuilder.append(result);
        stringBuilder.append(" dataRoamingScope ");
        stringBuilder.append(dataRoamingScope);
        log(stringBuilder.toString());
        return result;
    }

    public boolean isInternationalRoaming() {
        if (this.mDcTrackerBase.mPhone == null || this.mDcTrackerBase.mPhone.mIccRecords == null || this.mDcTrackerBase.mPhone.mIccRecords.get() == null) {
            log("mPhone or mIccRecords is null");
            return false;
        } else if (this.mDcTrackerBase.mPhone.getServiceState() == null) {
            log("dram isInternationalRoaming ServiceState is not start up");
            return false;
        } else if (this.mDcTrackerBase.mPhone.getServiceState().getRoaming()) {
            String simNumeric = ((IccRecords) this.mDcTrackerBase.mPhone.mIccRecords.get()).getOperatorNumeric();
            String operatorNumeric = this.mDcTrackerBase.mPhone.getServiceState().getOperatorNumeric();
            if (TextUtils.isEmpty(simNumeric) || TextUtils.isEmpty(operatorNumeric)) {
                log("dram isInternationalRoaming SIMNumeric or OperatorNumeric is not got!");
                return false;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dram isInternationalRoaming simNumeric ");
            stringBuilder.append(simNumeric);
            stringBuilder.append(" operatorNumeric ");
            stringBuilder.append(operatorNumeric);
            log(stringBuilder.toString());
            if (simNumeric.length() <= 3 || operatorNumeric.length() <= 3 || simNumeric.substring(0, 3).equals(operatorNumeric.substring(0, 3))) {
                return false;
            }
            return true;
        } else {
            log("dram isInternationalRoaming Current service state is not roaming, bail ");
            return false;
        }
    }

    private void onLimitPDPActInd(AsyncResult ar) {
        if (ar != null && ar.exception == null && ar.result != null) {
            int[] responseArray = ar.result;
            if (responseArray != null && responseArray.length >= 2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PSCLEARCODE onLimitPDPActInd result flag: ");
                stringBuilder.append(responseArray[0]);
                stringBuilder.append(" , cause: ");
                stringBuilder.append(responseArray[1]);
                log(stringBuilder.toString());
                this.mIsLimitPDPAct = responseArray[0] == 1;
                DcFailCause cause = DcFailCause.fromInt(responseArray[1]);
                if (this.mIsLimitPDPAct && !isLteRadioTech()) {
                    showPSClearCodeDialog(cause);
                }
                if (!(this.mAlarmManager == null || this.mClearCodeLimitAlarmIntent == null)) {
                    this.mAlarmManager.cancel(this.mClearCodeLimitAlarmIntent);
                    this.mClearCodeLimitAlarmIntent = null;
                }
                Intent intent = new Intent(INTENT_LIMIT_PDP_ACT_IND);
                intent.putExtra(IS_LIMIT_PDP_ACT, this.mIsLimitPDPAct);
                intent.addFlags(268435456);
                this.mClearCodeLimitAlarmIntent = PendingIntent.getBroadcast(this.mDcTrackerBase.mPhone.getContext(), 0, intent, 134217728);
                if (this.mAlarmManager != null) {
                    this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + PS_CLEARCODE_LIMIT_PDP_ACT_DELAY, this.mClearCodeLimitAlarmIntent);
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PSCLEARCODE startAlarmForLimitPDPActInd: delay=");
                stringBuilder2.append(PS_CLEARCODE_LIMIT_PDP_ACT_DELAY);
                stringBuilder2.append(" flag=");
                stringBuilder2.append(this.mIsLimitPDPAct);
                log(stringBuilder2.toString());
            }
        }
    }

    private void showPSClearCodeDialog(DcFailCause cause) {
        if (this.mPSClearCodeDialog == null) {
            this.mPSClearCodeDialog = createPSClearCodeDiag(cause);
            if (this.mPSClearCodeDialog != null) {
                this.mPSClearCodeDialog.show();
            }
        }
    }

    private void onActionIntentLimitPDPActInd(Intent intent) {
        if (intent != null) {
            boolean isLimitPDPAct = intent.getBooleanExtra(IS_LIMIT_PDP_ACT, false);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PSCLEARCODE onActionIntentLimitPDPActInd: flag = ");
            stringBuilder.append(isLimitPDPAct);
            log(stringBuilder.toString());
            if (!isLimitPDPAct) {
                this.mDcTrackerBase.updateApnContextState();
                setupDataOnConnectableApns("limitPDPActDisabled", null);
            }
        }
    }

    public long updatePSClearCodeApnContext(AsyncResult ar, ApnContext apnContext, long delay) {
        long delayTime = delay;
        if (ar == null || apnContext == null || apnContext.getApnSetting() == null) {
            return delayTime;
        }
        DcFailCause dcFailCause = ar.result;
        if (DcFailCause.PDP_ACTIVE_LIMIT == dcFailCause) {
            log("PSCLEARCODE retry APN. new delay = -1");
            return -1;
        }
        if (DcFailCause.SERVICE_OPTION_NOT_SUBSCRIBED == dcFailCause || DcFailCause.USER_AUTHENTICATION == dcFailCause) {
            if (isLteRadioTech()) {
                delayTime = PS_CLEARCODE_APN_DELAY_MILLIS_4G;
            } else {
                delayTime = PS_CLEARCODE_APN_DELAY_MILLIS_2G_3G;
            }
            apnContext.getApnSetting().permanentFailed = false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PSCLEARCODE retry APN. new delay = ");
        stringBuilder.append(delayTime);
        log(stringBuilder.toString());
        return delayTime;
    }

    private void onVoiceCallEndedHw() {
        log("onVoiceCallEndedHw");
        if (!HwModemCapability.isCapabilitySupport(0)) {
            int currentSub = this.mDcTrackerBase.mPhone.getPhoneId();
            SubscriptionController subscriptionController = SubscriptionController.getInstance();
            int defaultDataSubId = subscriptionController.getDefaultDataSubId();
            if (subscriptionController.getSubState(defaultDataSubId) == 0 && currentSub != defaultDataSubId) {
                StringBuilder stringBuilder;
                if (HwVSimUtils.isVSimInProcess() || HwVSimUtils.isVSimCauseCardReload()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("vsim is in process or cardreload, not set dds to ");
                    stringBuilder.append(currentSub);
                    debugLog(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("defaultDataSub ");
                    stringBuilder.append(defaultDataSubId);
                    stringBuilder.append(" is inactive, set dataSubId to ");
                    stringBuilder.append(currentSub);
                    debugLog(stringBuilder.toString());
                    subscriptionController.setDefaultDataSubId(currentSub);
                }
            }
            if (this.mDcTrackerBase.mPhone.getServiceStateTracker() != null) {
                this.mDcTrackerBase.mPhone.notifyServiceStateChangedP(this.mDcTrackerBase.mPhone.getServiceStateTracker().mSS);
            }
        }
    }

    public boolean isDataConnectivityDisabled(int slotId, String tag) {
        return HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(slotId, tag);
    }

    private void debugLog(String logStr) {
        log(logStr);
    }

    public ApnSetting getCustPreferredApn(ArrayList<ApnSetting> apnSettings) {
        if (CUST_PREFERRED_APN == null || "".equals(CUST_PREFERRED_APN)) {
            return null;
        }
        if (apnSettings == null || apnSettings.isEmpty()) {
            log("getCustPreferredApn mAllApnSettings == null");
            return null;
        }
        int list_size = apnSettings.size();
        for (int i = 0; i < list_size; i++) {
            ApnSetting p = (ApnSetting) apnSettings.get(i);
            if (CUST_PREFERRED_APN.equals(p.apn)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getCustPreferredApn: X found apnSetting");
                stringBuilder.append(p);
                log(stringBuilder.toString());
                return p;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getCustPreferredApn: not found apn: ");
        stringBuilder2.append(CUST_PREFERRED_APN);
        log(stringBuilder2.toString());
        return null;
    }

    public boolean isRoamingPushDisabled() {
        return HwTelephonyManagerInner.getDefault().isRoamingPushDisabled();
    }

    public boolean processAttDataRoamingOff() {
        boolean z = false;
        if (!IS_ATT) {
            return false;
        }
        if (Global.getInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "ATT_DOMESTIC_DATA", 0) != 0) {
            z = true;
        }
        boolean domesticDataEnabled = z;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processAttRoamingOff domesticDataEnabled = ");
        stringBuilder.append(domesticDataEnabled);
        log(stringBuilder.toString());
        this.mDcTrackerBase.setUserDataEnabled(domesticDataEnabled);
        if (domesticDataEnabled) {
            this.mDcTrackerBase.notifyOffApnsOfAvailability("roamingOff");
            setupDataOnConnectableApns("roamingOff", null);
        } else {
            this.mDcTrackerBase.cleanUpAllConnections(true, "roamingOff");
            this.mDcTrackerBase.notifyOffApnsOfAvailability("roamingOff");
        }
        return true;
    }

    public boolean processAttDataRoamingOn() {
        if (!IS_ATT) {
            return false;
        }
        if (!this.mDcTrackerBase.mPhone.getServiceState().getDataRoaming()) {
            return true;
        }
        boolean dataRoamingEnabled = this.mDcTrackerBase.getDataRoamingEnabled();
        this.mDcTrackerBase.setUserDataEnabled(dataRoamingEnabled);
        if (dataRoamingEnabled) {
            log("onRoamingOn: setup data on in internal roaming");
            setupDataOnConnectableApns("roamingOn", null);
            this.mDcTrackerBase.notifyDataConnection("roamingOn");
        } else {
            log("onRoamingOn: Tear down data connection on internal roaming.");
            this.mDcTrackerBase.cleanUpAllConnections(true, "roamingOn");
            this.mDcTrackerBase.notifyOffApnsOfAvailability("roamingOn");
        }
        return true;
    }

    public boolean getXcapDataRoamingEnable() {
        CarrierConfigManager configLoader = (CarrierConfigManager) this.mDcTrackerBase.mPhone.getContext().getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configLoader != null) {
            b = configLoader.getConfigForSubId(this.mDcTrackerBase.mPhone.getSubId());
        }
        boolean xcapDataRoamingEnable = false;
        if (b != null) {
            xcapDataRoamingEnable = b.getBoolean(XCAP_DATA_ROAMING_ENABLE);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getXcapDataRoamingEnable:xcapDataRoamingEnable ");
        stringBuilder.append(xcapDataRoamingEnable);
        log(stringBuilder.toString());
        try {
            Boolean getXcapEnable = (Boolean) HwCfgFilePolicy.getValue(XCAP_DATA_ROAMING_ENABLE, SubscriptionManager.getSlotIndex(this.mDcTrackerBase.mPhone.getSubId()), Boolean.class);
            if (getXcapEnable != null) {
                return getXcapEnable.booleanValue();
            }
            return xcapDataRoamingEnable;
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception: read carrier_xcap_data_roaming_switch error ");
            stringBuilder2.append(e.toString());
            log(stringBuilder2.toString());
            return xcapDataRoamingEnable;
        }
    }

    public void updateDSUseDuration() {
        if (mIsScreenOn) {
            this.mDSUseDuration++;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateDSUseDuration: Update mDSUseDuration: ");
            stringBuilder.append(this.mDSUseDuration);
            log(stringBuilder.toString());
            long curTime = SystemClock.elapsedRealtime();
            if (curTime > this.mNextReportDSUseDurationStamp) {
                HwTelephonyFactory.getHwDataServiceChrManager().sendIntentDSUseStatistics(this.mDcTrackerBase.mPhone, this.mDSUseDuration);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateDSUseDuration: report mDSUseDuration: ");
                stringBuilder2.append(this.mDSUseDuration);
                log(stringBuilder2.toString());
                this.mDSUseDuration = 0;
                this.mNextReportDSUseDurationStamp = 3600000 + curTime;
            }
        }
    }

    private int getallowMmsPropertyByPlmn() {
        int allowMmsPropertyInt = -1;
        try {
            int allowMmsPropertyInt2;
            StringBuilder stringBuilder;
            int subId = this.mDcTrackerBase.mPhone.getSubId();
            Integer hwAlwaysAllowMms = (Integer) HwCfgFilePolicy.getValue(ALLOW_MMS_PROPERTY_INT, SubscriptionManager.getSlotIndex(subId), Integer.class);
            if (hwAlwaysAllowMms != null) {
                allowMmsPropertyInt2 = hwAlwaysAllowMms.intValue();
            } else {
                CarrierConfigManager configLoader = (CarrierConfigManager) this.mDcTrackerBase.mPhone.getContext().getSystemService("carrier_config");
                if (configLoader == null) {
                    return allowMmsPropertyInt;
                }
                PersistableBundle bundle = configLoader.getConfigForSubId(subId);
                if (bundle == null) {
                    return allowMmsPropertyInt;
                }
                allowMmsPropertyInt2 = bundle.getInt(ALLOW_MMS_PROPERTY_INT, -1);
            }
            if (allowMmsPropertyInt2 >= 0) {
                if (allowMmsPropertyInt2 > 7) {
                }
                allowMmsPropertyInt = allowMmsPropertyInt2;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getallowMmsPropertyByPlmn:allowMmsPropertyInt ");
                stringBuilder.append(allowMmsPropertyInt);
                log(stringBuilder.toString());
                return allowMmsPropertyInt;
            }
            allowMmsPropertyInt2 = -1;
            allowMmsPropertyInt = allowMmsPropertyInt2;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getallowMmsPropertyByPlmn:allowMmsPropertyInt ");
            stringBuilder.append(allowMmsPropertyInt);
            log(stringBuilder.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception: read allow_mms_property_int error ");
            stringBuilder2.append(e.toString());
            log(stringBuilder2.toString());
        }
        return allowMmsPropertyInt;
    }

    public boolean getAttachedStatus(boolean attached) {
        if (PhoneFactory.IS_QCOM_DUAL_LTE_STACK || DISABLE_GW_PS_ATTACH) {
            int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
            if (dataSub != this.mDcTrackerBase.mPhone.getSubId() && SubscriptionManager.isUsableSubIdValue(dataSub)) {
                return true;
            }
        }
        return attached;
    }

    public boolean isBtConnected() {
        return this.mIsBtConnected;
    }

    public boolean isWifiConnected() {
        ConnectivityManager cm = ConnectivityManager.from(this.mDcTrackerBase.mPhone.getContext());
        if (cm != null) {
            NetworkInfo mWifiNetworkInfo = cm.getNetworkInfo(1);
            if (mWifiNetworkInfo != null && mWifiNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                log("isWifiConnected return true");
                return true;
            }
        }
        return false;
    }

    public boolean isDataNeededWithWifiAndBt() {
        boolean isDataAlwaysOn = Global.getInt(this.mDcTrackerBase.mPhone.getContext().getContentResolver(), "mobile_data_always_on", 0) != 0;
        if (isDataAlwaysOn) {
            log("isDataNeededWithWifiAndBt:isDataAlwaysOn = true");
        }
        if (isDataAlwaysOn) {
            return true;
        }
        if (isBtConnected() || isWifiConnected()) {
            return false;
        }
        return true;
    }
}
