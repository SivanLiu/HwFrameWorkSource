package com.android.internal.telephony;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.emcom.EmcomManager;
import android.net.ConnectivityManager;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.CellLocation;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.HwVSimManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyManager.MultiSimVariants;
import android.telephony.UiccAuthResponse;
import android.text.TextUtils;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.HwPhoneManager.PhoneServiceInterface;
import com.android.internal.telephony.IHwTelephony.Stub;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.dataconnection.HwVSimNetworkFactory;
import com.android.internal.telephony.dataconnection.InCallDataStateMachine;
import com.android.internal.telephony.dataconnection.TelephonyNetworkFactory;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import com.android.internal.telephony.gsm.HwGsmServiceStateManager;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimUtils;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import huawei.cust.HwCustUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;

public class HwPhoneService extends Stub implements PhoneServiceInterface {
    private static final String CALLBACK_AFBS_INFO = "AntiFakeBaseStationInfo";
    private static final String CALLBACK_CF_INFO = "CallForwardInfos";
    private static final String CALLBACK_EXCEPTION = "EXCEPTION";
    private static final String CALLBACK_RESULT = "RESULT";
    private static final int CMD_ENCRYPT_CALL_INFO = 500;
    private static final int CMD_HANDLE_DEMO = 1;
    private static final int CMD_IMS_GET_DOMAIN = 103;
    private static final int CMD_INVOKE_OEM_RIL_REQUEST_RAW = 116;
    private static final int CMD_SET_DEEP_NO_DISTURB = 203;
    private static final int CMD_UICC_AUTH = 101;
    private static final String DAY_MODE = "day_mode";
    private static final String DAY_MODE_TIME = "day_mode_time";
    private static final String DEVICEID_PREF = "deviceid";
    private static final int EVENT_ANTIFAKE_BASESTATION_CHANGED = 122;
    private static final int EVENT_BASIC_COMM_PARA_UPGRADE_DONE = 112;
    private static final int EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE = 113;
    private static final int EVENT_CHANGE_ICC_PIN_COMPLETE = 505;
    private static final int EVENT_COMMON_IMSA_MAPCON_INFO = 53;
    private static final int EVENT_ENABLE_ICC_PIN_COMPLETE = 504;
    private static final int EVENT_ENCRYPT_CALL_INFO_DONE = 501;
    private static final int EVENT_GET_CALLFORWARDING_DONE = 119;
    private static final int EVENT_GET_CARD_TRAY_INFO_DONE = 601;
    private static final int EVENT_GET_CELL_BAND_DONE = 6;
    private static final int EVENT_GET_LAA_STATE_DONE = 115;
    private static final int EVENT_GET_NUMRECBASESTATION_DONE = 121;
    private static final int EVENT_GET_PREF_NETWORKS = 3;
    private static final int EVENT_GET_PREF_NETWORK_TYPE_DONE = 9;
    private static final int EVENT_ICC_GET_ATR_DONE = 507;
    private static final int EVENT_ICC_STATUS_CHANGED = 54;
    private static final int EVENT_IMS_GET_DOMAIN_DONE = 104;
    private static final int EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE = 117;
    private static final int EVENT_NOTIFY_CMODEM_STATUS = 110;
    private static final int EVENT_NOTIFY_DEVICE_STATE = 111;
    private static final int EVENT_QUERY_CARD_TYPE_DONE = 506;
    private static final int EVENT_QUERY_ENCRYPT_FEATURE = 502;
    private static final int EVENT_QUERY_ENCRYPT_FEATURE_DONE = 503;
    private static final int EVENT_RADIO_AVAILABLE = 51;
    private static final int EVENT_RADIO_NOT_AVAILABLE = 52;
    private static final int EVENT_REG_ANT_STATE_IND = 11;
    private static final int EVENT_REG_BAND_CLASS_IND = 10;
    private static final int EVENT_REG_MAX_TX_POWER_IND = 12;
    private static final int EVENT_RETRY_SET_PREF_NETWORK_TYPE = 14;
    private static final int EVENT_SEND_LAA_CMD_DONE = 114;
    private static final int EVENT_SET_4G_SLOT_DONE = 200;
    private static final int EVENT_SET_CALLFORWARDING_DONE = 118;
    private static final int EVENT_SET_DEEP_NO_DISTURB_DONE = 204;
    private static final int EVENT_SET_LINENUM_DONE = 202;
    private static final int EVENT_SET_LTE_SWITCH_DONE = 5;
    private static final int EVENT_SET_PREF_NETWORKS = 4;
    private static final int EVENT_SET_PREF_NETWORK_TYPE_DONE = 13;
    private static final int EVENT_SET_SUBSCRIPTION_DONE = 201;
    private static final int EVENT_UICC_AUTH_DONE = 102;
    private static final int EVENT_USB_TETHER_STATE = 120;
    private static final int HW_PHONE_EXTEND_EVENT_BASE = 600;
    private static final int HW_SWITCH_SLOT_DONE = 1;
    private static final String HW_SWITCH_SLOT_STEP = "HW_SWITCH_SLOT_STEP";
    private static final String IMEI_PREF = "imei";
    private static final String INCOMING_SMS_LIMIT = "incoming_limit";
    private static final int INVALID = -1;
    private static final int INVALID_NETWORK_MODE = -1;
    private static final int INVALID_STEP = -99;
    private static final boolean IS_4G_SWITCH_SUPPORTED = SystemProperties.getBoolean("persist.sys.dualcards", false);
    private static final boolean IS_DUAL_IMS_SUPPORTED = HwModemCapability.isCapabilitySupport(21);
    private static final boolean IS_FULL_NETWORK_SUPPORTED = SystemProperties.getBoolean(HwFullNetworkConfig.PROPERTY_FULL_NETWORK_SUPPORT, false);
    private static final boolean IS_GSM_NONSUPPORT = SystemProperties.getBoolean("ro.config.gsm_nonsupport", false);
    private static final String IS_OUTGOING = "isOutgoing";
    private static final String KEY1 = "key1";
    private static final String LOG_TAG = "HwPhoneService";
    private static final int LTE_SERVICE_OFF = 0;
    private static final int LTE_SERVICE_ON = 1;
    private static final int MAX_QUERY_COUNT = 10;
    private static final int MESSAGE_RETRY_PENDING_DELAY = 3000;
    private static final String MONTH_MODE = "month_mode";
    private static final String MONTH_MODE_TIME = "month_mode_time";
    private static final int MSG_ENCRYPT_CALL_BASE = 500;
    private static final int NOTIFY_CMODEM_STATUS_FAIL = -1;
    private static final int NOTIFY_CMODEM_STATUS_SUCCESS = 1;
    private static final String OUTGOING_SMS_LIMIT = "outgoing_limit";
    private static final int PARATYPE_BASIC_COMM = 1;
    private static final int PARATYPE_CELLULAR_CLOUD = 2;
    private static final int PARA_PATHTYPE_COTA = 1;
    private static final String PATTERN_SIP = "^sip:(\\+)?[0-9]+@[^@]+";
    private static final String PATTERN_TEL = "^tel:(\\+)?[0-9]+";
    private static final String POLICY_REMOVE_ALL = "remove_all_policy";
    private static final String POLICY_REMOVE_SINGLE = "remove_single_policy";
    private static final int REDUCE_TYPE_CELL = 1;
    private static final int REDUCE_TYPE_WIFI = 2;
    private static final int REGISTER_TYPE = 1;
    private static final int REGISTER_TYPE_ANTENNA = 2;
    private static final int REGISTER_TYPE_BAND = 1;
    private static final int REGISTER_TYPE_MAX_TX_POWER = 4;
    private static final String REMOVE_TYPE = "removeType";
    private static final int RETRY_MAX_TIME = 20;
    private static final int SERVICE_2G_OFF = 0;
    private static final boolean SHOW_DIALOG_FOR_NO_SIM = SystemProperties.getBoolean("ro.config.no_sim", false);
    private static final int SIM_NUM = TelephonyManager.getDefault().getPhoneCount();
    private static final int STATE_IN_AIR_PLANE_MODE = 1;
    public static final int SUB1 = 0;
    public static final int SUB2 = 1;
    public static final int SUB_NONE = -1;
    private static final int SUCCESS = 0;
    private static final String TIME_MODE = "time_mode";
    private static final int TYPEMASK_PARATYPE_BASIC_COMM = 0;
    private static final int TYPEMASK_PARATYPE_CELLULAR_CLOUD = 1;
    private static final int UNREGISTER_TYPE = 2;
    private static final String USED_OF_DAY = "used_number_day";
    private static final String USED_OF_MONTH = "used_number_month";
    private static final String USED_OF_WEEK = "used_number_week";
    private static final String USER_DATACALL_SUBSCRIPTION = "user_datacall_sub";
    private static final String WEEK_MODE = "week_mode";
    private static final String WEEK_MODE_TIME = "week_mode_time";
    private static int queryCount = 0;
    private static HwPhoneService sInstance = null;
    private static final boolean sIsPlatformSupportVSim = SystemProperties.getBoolean("ro.radio.vsim_support", false);
    private final int ENCRYPT_CALL_FEATURE_CLOSE = 0;
    private final String ENCRYPT_CALL_FEATURE_KEY = "encrypt_version";
    private final int ENCRYPT_CALL_FEATURE_OPEN = 1;
    private final int ENCRYPT_CALL_FEATURE_SUPPORT = 1;
    private final int ENCRYPT_CALL_NV_OFFSET = 4;
    private Message getLaaStateCompleteMsg = null;
    private IPhoneCallback mAntiFakeBaseStationCB = null;
    private AppOpsManager mAppOps;
    private byte[] mCardTrayInfo = null;
    private final Object mCardTrayLock = new Object();
    private Context mContext;
    private int mEncryptCallStatus = 0;
    private HwCustPhoneService mHwCustPhoneService = null;
    private IPhoneCallback mImsaToMapconInfoCB = null;
    InCallDataStateMachine mInCallDataStateMachine;
    protected final Object mLock = new Object();
    private BroadcastReceiver mMDMSmsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "com.huawei.devicepolicy.action.POLICY_CHANGED".equals(intent.getAction())) {
                String removeType = intent.getStringExtra(HwPhoneService.REMOVE_TYPE);
                HwPhoneService hwPhoneService = HwPhoneService.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("removeType: ");
                stringBuilder.append(removeType);
                hwPhoneService.log(stringBuilder.toString());
                boolean isOutgoing = intent.getBooleanExtra(HwPhoneService.IS_OUTGOING, false);
                if (HwPhoneService.POLICY_REMOVE_SINGLE.equals(removeType)) {
                    String timeMode = intent.getStringExtra(HwPhoneService.TIME_MODE);
                    HwPhoneService hwPhoneService2 = HwPhoneService.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mMDMSmsReceiver onReceive : ");
                    stringBuilder2.append(timeMode);
                    hwPhoneService2.log(stringBuilder2.toString());
                    HwPhoneService.this.clearSinglePolicyData(context, timeMode, isOutgoing);
                } else if (HwPhoneService.POLICY_REMOVE_ALL.equals(removeType)) {
                    HwPhoneService.this.clearAllPolicyData(context);
                }
            }
        }
    };
    private MainHandler mMainHandler;
    private HandlerThread mMessageThread = new HandlerThread("HuaweiPhoneTempService");
    private HwPhone mPhone;
    private PhoneServiceReceiver mPhoneServiceReceiver;
    private PhoneStateHandler mPhoneStateHandler;
    private HwPhone[] mPhones = null;
    private IPhoneCallback mRadioAvailableIndCB = null;
    private IPhoneCallback mRadioNotAvailableIndCB = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null && "android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                if (HwPhoneService.this.mPhone == null) {
                    HwPhoneService.loge("received ACTION_SIM_STATE_CHANGED, but mPhone is null!");
                    return;
                }
                int slotId = intent.getIntExtra("slot", -1000);
                String simState = intent.getStringExtra("ss");
                if (!HwPhoneService.IS_FULL_NETWORK_SUPPORTED && "READY".equals(simState)) {
                    HwPhoneService.this.log("mReceiver receive ACTION_SIM_STATE_CHANGED READY,check pref network type");
                    HwPhoneService.this.mPhone.getPreferredNetworkType(HwPhoneService.this.mMainHandler.obtainMessage(9));
                }
                if (HwPhoneService.IS_FULL_NETWORK_SUPPORTED && "IMSI".equals(simState)) {
                    HwPhoneService.this.setSingleCardPrefNetwork(slotId);
                }
            }
            if (HwPhoneService.SHOW_DIALOG_FOR_NO_SIM && intent != null && "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
                if (telephonyManager != null && 1 == telephonyManager.getSimState()) {
                    Builder dialogBuilder = new Builder(context, 3);
                    dialogBuilder.setTitle(33685979).setMessage(33685980).setCancelable(false).setPositiveButton(33685981, null);
                    AlertDialog alertDialog = dialogBuilder.create();
                    alertDialog.getWindow().setType(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_MODE_DONE);
                    alertDialog.show();
                }
            }
        }
    };
    private final ArrayList<Record> mRecords = new ArrayList();
    private Object[] mRegAntStateCallbackArray = new Object[]{this.mRegAntStateCallbackLists0, this.mRegAntStateCallbackLists1};
    private final ArrayList<Record> mRegAntStateCallbackLists0 = new ArrayList();
    private final ArrayList<Record> mRegAntStateCallbackLists1 = new ArrayList();
    private Object[] mRegBandClassCallbackArray = new Object[]{this.mRegBandClassCallbackLists0, this.mRegBandClassCallbackLists1};
    private final ArrayList<Record> mRegBandClassCallbackLists0 = new ArrayList();
    private final ArrayList<Record> mRegBandClassCallbackLists1 = new ArrayList();
    private Object[] mRegMaxTxPowerCallbackArray = new Object[]{this.mRegMaxTxPowerCallbackList0, this.mRegMaxTxPowerCallbackList1};
    private final ArrayList<Record> mRegMaxTxPowerCallbackList0 = new ArrayList();
    private final ArrayList<Record> mRegMaxTxPowerCallbackList1 = new ArrayList();
    private final ArrayList<IBinder> mRemoveRecordsList = new ArrayList();
    private String[] mServiceCellBand = new String[2];
    protected final Object mSetOPinLock = new Object();
    private BroadcastReceiver mSetRadioCapDoneReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if ("com.huawei.action.ACTION_HW_SWITCH_SLOT_DONE".equals(intent.getAction())) {
                    HwPhoneService.this.handleSwitchSlotDone(intent);
                }
            }
        }
    };
    boolean mSupportEncryptCall = SystemProperties.getBoolean("persist.sys.cdma_encryption", false);
    Phone phone;
    private int retryCount = 0;
    private Message sendLaaCmdCompleteMsg = null;
    private boolean setResultForChangePin = false;
    private boolean setResultForPinLock = false;

    private static final class EncryptCallPara {
        byte[] buf = null;
        int event;
        HwPhone phone = null;

        public EncryptCallPara(HwPhone phone, int event, byte[] buf) {
            this.phone = phone;
            this.event = event;
            this.buf = buf;
        }
    }

    private final class MainHandler extends Handler {
        MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String str;
            StringBuilder stringBuilder;
            int i = msg.what;
            if (i != 1) {
                switch (i) {
                    case 3:
                        handleGetPreferredNetworkTypeResponse(msg);
                        return;
                    case 4:
                        handleSetPreferredNetworkTypeResponse(msg);
                        return;
                    case 5:
                        HwPhoneService.this.log("4G-Switch EVENT_SET_LTE_SWITCH_DONE");
                        HwPhoneService.this.handleSetLteSwitchDone(msg);
                        return;
                    case 6:
                        HwPhoneService.this.handleQueryCellBandDone(msg);
                        return;
                    default:
                        switch (i) {
                            case 9:
                                HwPhoneService.this.log("EVENT_GET_PREF_NETWORK_TYPE_DONE");
                                HwPhoneService.this.handleGetPrefNetworkTypeDone(msg);
                                return;
                            case 10:
                                HwPhoneService.this.log("EVENT_REG_BAND_CLASS_IND");
                                HwPhoneService.this.handleSarInfoUploaded(1, msg);
                                return;
                            case 11:
                                HwPhoneService.this.log("EVENT_REG_ANT_STATE_IND");
                                HwPhoneService.this.handleSarInfoUploaded(2, msg);
                                return;
                            case 12:
                                HwPhoneService.this.log("EVENT_REG_MAX_TX_POWER_IND");
                                HwPhoneService.this.handleSarInfoUploaded(4, msg);
                                return;
                            default:
                                switch (i) {
                                    case 51:
                                        HwPhoneService.this.handleRadioAvailableInd(msg);
                                        return;
                                    case 52:
                                        HwPhoneService.this.handleRadioNotAvailableInd(msg);
                                        return;
                                    case 53:
                                        HwPhoneService.this.handleCommonImsaToMapconInfoInd(msg);
                                        return;
                                    default:
                                        int i2 = 0;
                                        AsyncResult ar;
                                        StringBuilder stringBuilder2;
                                        StringBuilder stringBuilder3;
                                        switch (i) {
                                            case HwPhoneService.CMD_UICC_AUTH /*101*/:
                                                HwPhoneService.this.handleCmdUiccAuth(msg);
                                                return;
                                            case HwPhoneService.EVENT_UICC_AUTH_DONE /*102*/:
                                                ar = (AsyncResult) msg.obj;
                                                MainThreadRequest request = ar.userObj;
                                                HwPhoneService.loge("EVENT_UICC_AUTH_DONE");
                                                request.result = new UiccAuthResponse();
                                                if (ar.exception == null && ar.result != null) {
                                                    request.result = ar.result;
                                                } else if (ar.result == null) {
                                                    HwPhoneService.loge("UiccAuthReq: Empty response");
                                                } else if (ar.exception instanceof CommandException) {
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("UiccAuthReq: CommandException: ");
                                                    stringBuilder2.append(ar.exception);
                                                    HwPhoneService.loge(stringBuilder2.toString());
                                                } else {
                                                    HwPhoneService.loge("UiccAuthReq: Unknown exception");
                                                }
                                                synchronized (request) {
                                                    request.notifyAll();
                                                }
                                                return;
                                            case HwPhoneService.CMD_IMS_GET_DOMAIN /*103*/:
                                                HwPhoneService.this.handleCmdImsGetDomain(msg);
                                                return;
                                            case HwPhoneService.EVENT_IMS_GET_DOMAIN_DONE /*104*/:
                                                ar = (AsyncResult) msg.obj;
                                                MainThreadRequest request2 = ar.userObj;
                                                if (ar.exception == null && ar.result != null) {
                                                    request2.result = ar.result;
                                                } else if (ar.result == null) {
                                                    request2.result = new int[]{2};
                                                    HwPhoneService.loge("getImsDomain: Empty response,return 2");
                                                } else if (ar.exception instanceof CommandException) {
                                                    request2.result = new int[]{2};
                                                    stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("getImsDomain: CommandException:return 2 ");
                                                    stringBuilder3.append(ar.exception);
                                                    HwPhoneService.loge(stringBuilder3.toString());
                                                } else {
                                                    request2.result = new int[]{2};
                                                    HwPhoneService.loge("getImsDomain: Unknown exception,return 2");
                                                }
                                                synchronized (request2) {
                                                    request2.notifyAll();
                                                }
                                                return;
                                            default:
                                                String str2;
                                                switch (i) {
                                                    case HwPhoneService.EVENT_NOTIFY_CMODEM_STATUS /*110*/:
                                                        Rlog.d(HwPhoneService.LOG_TAG, "EVENT_NOTIFY_CMODEM_STATUS");
                                                        ar = (AsyncResult) msg.obj;
                                                        if (ar != null) {
                                                            IPhoneCallback callback = ar.userObj;
                                                            if (callback != null) {
                                                                int result = 1;
                                                                if (ar.exception != null) {
                                                                    result = -1;
                                                                }
                                                                try {
                                                                    Rlog.d(HwPhoneService.LOG_TAG, "EVENT_NOTIFY_CMODEM_STATUS onCallback1");
                                                                    callback.onCallback1(result);
                                                                    return;
                                                                } catch (RemoteException ex) {
                                                                    str = HwPhoneService.LOG_TAG;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("EVENT_NOTIFY_CMODEM_STATUS onCallback1 RemoteException:");
                                                                    stringBuilder.append(ex);
                                                                    Rlog.e(str, stringBuilder.toString());
                                                                    return;
                                                                }
                                                            }
                                                            return;
                                                        }
                                                        return;
                                                    case HwPhoneService.EVENT_NOTIFY_DEVICE_STATE /*111*/:
                                                        ar = (AsyncResult) msg.obj;
                                                        if (ar == null) {
                                                            HwPhoneService.loge("EVENT_NOTIFY_DEVICE_STATE, ar is null.");
                                                            return;
                                                        } else if (ar.exception == null) {
                                                            Rlog.d(HwPhoneService.LOG_TAG, "EVENT_NOTIFY_DEVICE_STATE success.");
                                                            return;
                                                        } else if (ar.exception instanceof CommandException) {
                                                            stringBuilder3 = new StringBuilder();
                                                            stringBuilder3.append("EVENT_NOTIFY_DEVICE_STATE, ");
                                                            stringBuilder3.append(ar.exception);
                                                            HwPhoneService.loge(stringBuilder3.toString());
                                                            return;
                                                        } else {
                                                            HwPhoneService.loge("EVENT_NOTIFY_DEVICE_STATE, unknown exception.");
                                                            return;
                                                        }
                                                    case HwPhoneService.EVENT_BASIC_COMM_PARA_UPGRADE_DONE /*112*/:
                                                        Rlog.d(HwPhoneService.LOG_TAG, "EVENT_BASIC_COMM_PARA_UPGRADE_DONE");
                                                        ar = (AsyncResult) msg.obj;
                                                        if (ar.exception != null) {
                                                            str2 = HwPhoneService.LOG_TAG;
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("Error in BasicImsNVPara Upgrade:");
                                                            stringBuilder2.append(ar.exception);
                                                            Rlog.e(str2, stringBuilder2.toString());
                                                            return;
                                                        }
                                                        int[] resultUpgrade = ar.result;
                                                        if (resultUpgrade.length != 0) {
                                                            str = HwPhoneService.LOG_TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("EVENT_BASIC_COMM_PARA_UPGRADE_DONE: result=");
                                                            stringBuilder.append(resultUpgrade[0]);
                                                            Rlog.d(str, stringBuilder.toString());
                                                        } else {
                                                            Rlog.e(HwPhoneService.LOG_TAG, "EVENT_BASIC_COMM_PARA_UPGRADE_DONE: resultUpgrade.length = 0");
                                                            resultUpgrade[0] = -1;
                                                        }
                                                        EmcomManager.getInstance().responseForParaUpgrade(1, 1, resultUpgrade[0]);
                                                        Rlog.d(HwPhoneService.LOG_TAG, "responseForParaUpgrade()");
                                                        return;
                                                    case HwPhoneService.EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE /*113*/:
                                                        Rlog.d(HwPhoneService.LOG_TAG, "EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE");
                                                        ar = (AsyncResult) msg.obj;
                                                        if (ar.exception != null) {
                                                            str2 = HwPhoneService.LOG_TAG;
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("Error in Cellular Cloud Para Upgrade:");
                                                            stringBuilder2.append(ar.exception);
                                                            Rlog.e(str2, stringBuilder2.toString());
                                                            return;
                                                        }
                                                        int[] phoneResult = ar.result;
                                                        if (phoneResult.length != 0) {
                                                            str = HwPhoneService.LOG_TAG;
                                                            StringBuilder stringBuilder4 = new StringBuilder();
                                                            stringBuilder4.append("EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE: phoneResult=");
                                                            stringBuilder4.append(phoneResult[0]);
                                                            Rlog.d(str, stringBuilder4.toString());
                                                        } else {
                                                            Rlog.e(HwPhoneService.LOG_TAG, "EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE: phoneResult.length = 0");
                                                            phoneResult[0] = -1;
                                                        }
                                                        EmcomManager.getInstance().responseForParaUpgrade(2, 1, phoneResult[0]);
                                                        Rlog.d(HwPhoneService.LOG_TAG, "responseForParaUpgrade()");
                                                        return;
                                                    case HwPhoneService.EVENT_SEND_LAA_CMD_DONE /*114*/:
                                                        HwPhoneService.this.log("EVENT_SEND_LAA_CMD_DONE");
                                                        HwPhoneService.this.handleSendLaaCmdDone(msg);
                                                        return;
                                                    case HwPhoneService.EVENT_GET_LAA_STATE_DONE /*115*/:
                                                        HwPhoneService.this.log("EVENT_GET_LAA_STATE_DONE");
                                                        HwPhoneService.this.handleGetLaaStateDone(msg);
                                                        return;
                                                    case HwPhoneService.CMD_INVOKE_OEM_RIL_REQUEST_RAW /*116*/:
                                                        HwPhoneService.this.handleCmdOemRilRequestRaw(msg);
                                                        return;
                                                    case HwPhoneService.EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE /*117*/:
                                                        HwPhoneService.this.handleCmdOemRilRequestRawDone(msg);
                                                        return;
                                                    case HwPhoneService.EVENT_SET_CALLFORWARDING_DONE /*118*/:
                                                        HwPhoneService.this.log("EVENT_SET_CALLFORWARDING_DONE");
                                                        HwPhoneService.this.handleSetFunctionDone(msg);
                                                        return;
                                                    case HwPhoneService.EVENT_GET_CALLFORWARDING_DONE /*119*/:
                                                        HwPhoneService.this.log("EVENT_GET_CALLFORWARDING_DONE");
                                                        HwPhoneService.this.handleGetCallforwardDone(msg);
                                                        return;
                                                    case 120:
                                                        ar = msg.obj;
                                                        if (ar == null || ar.exception == null) {
                                                            Rlog.d(HwPhoneService.LOG_TAG, "EVENT_USB_TETHER_STATE is success.");
                                                            return;
                                                        } else {
                                                            HwPhoneService.loge("EVENT_USB_TETHER_STATE is failed.");
                                                            return;
                                                        }
                                                    default:
                                                        switch (i) {
                                                            case 200:
                                                                HwPhoneService.this.log("EVENT_SET_4G_SLOT_DONE");
                                                                HwPhoneService.this.handleSetFunctionDone(msg);
                                                                return;
                                                            case 201:
                                                                HwPhoneService.this.log("EVENT_SET_SUBSCRIPTION_DONE");
                                                                HwPhoneService.this.handleSetFunctionDone(msg);
                                                                return;
                                                            default:
                                                                switch (i) {
                                                                    case HwFullNetworkConstants.MESSAGE_PENDING_DELAY /*500*/:
                                                                        Rlog.d(HwPhoneService.LOG_TAG, "requestForECInfo receive event");
                                                                        MainThreadRequest request3 = msg.obj;
                                                                        EncryptCallPara ECpara = request3.argument;
                                                                        Message onCompleted = obtainMessage(HwPhoneService.EVENT_ENCRYPT_CALL_INFO_DONE, request3);
                                                                        HwPhone sPhone = ECpara.phone;
                                                                        if (sPhone != null) {
                                                                            sPhone.requestForECInfo(onCompleted, ECpara.event, ECpara.buf);
                                                                            return;
                                                                        }
                                                                        return;
                                                                    case HwPhoneService.EVENT_ENCRYPT_CALL_INFO_DONE /*501*/:
                                                                        Rlog.d(HwPhoneService.LOG_TAG, "requestForECInfo receive event done");
                                                                        ar = msg.obj;
                                                                        MainThreadRequest request4 = ar.userObj;
                                                                        if (ar.exception == null) {
                                                                            if (ar.result == null || ((byte[]) ar.result).length <= 0) {
                                                                                request4.result = new byte[]{(byte) 1};
                                                                                Rlog.d(HwPhoneService.LOG_TAG, "requestForECInfo success,return 1");
                                                                            } else {
                                                                                request4.result = ar.result;
                                                                                Rlog.d(HwPhoneService.LOG_TAG, "requestForECInfo success,return ar.result");
                                                                            }
                                                                        } else if (ar.exception instanceof CommandException) {
                                                                            request4.result = new byte[]{(byte) -1};
                                                                            stringBuilder3 = new StringBuilder();
                                                                            stringBuilder3.append("requestForECInfo: CommandException:return -1 ");
                                                                            stringBuilder3.append(ar.exception);
                                                                            HwPhoneService.loge(stringBuilder3.toString());
                                                                        } else {
                                                                            request4.result = new byte[]{(byte) -2};
                                                                            HwPhoneService.loge("requestForECInfo: Unknown exception,return -2");
                                                                        }
                                                                        synchronized (request4) {
                                                                            request4.notifyAll();
                                                                        }
                                                                        return;
                                                                    case HwPhoneService.EVENT_QUERY_ENCRYPT_FEATURE /*502*/:
                                                                        Rlog.d(HwPhoneService.LOG_TAG, "radio available, query encrypt call feature");
                                                                        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                                                                            HwPhone[] access$1200 = HwPhoneService.this.mPhones;
                                                                            int length = access$1200.length;
                                                                            while (i2 < length) {
                                                                                HwPhoneService.this.handleEventQueryEncryptCall(access$1200[i2]);
                                                                                i2++;
                                                                            }
                                                                            return;
                                                                        }
                                                                        HwPhoneService.this.handleEventQueryEncryptCall(HwPhoneService.this.mPhone);
                                                                        return;
                                                                    case HwPhoneService.EVENT_QUERY_ENCRYPT_FEATURE_DONE /*503*/:
                                                                        HwPhoneService.this.handleQueryEncryptFeatureDone(msg);
                                                                        return;
                                                                    case HwPhoneService.EVENT_ENABLE_ICC_PIN_COMPLETE /*504*/:
                                                                    case HwPhoneService.EVENT_CHANGE_ICC_PIN_COMPLETE /*505*/:
                                                                        handlePinResult(msg);
                                                                        synchronized (HwPhoneService.this.mSetOPinLock) {
                                                                            HwPhoneService.this.mSetOPinLock.notifyAll();
                                                                        }
                                                                        return;
                                                                    default:
                                                                        handleMessageEx(msg);
                                                                        return;
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
            }
        }

        private void handleMessageEx(Message msg) {
            AsyncResult ar = null;
            Integer index = null;
            if (msg.what == 54 || msg.what == HwPhoneService.EVENT_QUERY_CARD_TYPE_DONE || msg.what == HwPhoneService.EVENT_ICC_GET_ATR_DONE) {
                index = HwPhoneService.this.getCiIndex(msg);
                if (index.intValue() < 0 || index.intValue() >= HwPhoneService.this.mPhones.length) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid index : ");
                    stringBuilder.append(index);
                    stringBuilder.append(" received with event ");
                    stringBuilder.append(msg.what);
                    HwPhoneService.loge(stringBuilder.toString());
                    return;
                } else if (msg.obj != null && (msg.obj instanceof AsyncResult)) {
                    ar = msg.obj;
                }
            }
            String str;
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case 54:
                    str = HwPhoneService.LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Received EVENT_ICC_STATUS_CHANGED on index ");
                    stringBuilder2.append(index);
                    Rlog.d(str, stringBuilder2.toString());
                    HwPhoneService.this.onIccStatusChanged(index);
                    break;
                case HwPhoneService.EVENT_GET_NUMRECBASESTATION_DONE /*121*/:
                    HwPhoneService.this.log("EVENT_GET_NUMRECBASESTATION_DONE");
                    HwPhoneService.this.handleGetNumRecBaseStattionDone(msg);
                    break;
                case HwPhoneService.EVENT_ANTIFAKE_BASESTATION_CHANGED /*122*/:
                    HwPhoneService.this.log("EVENT_ANTIFAKE_BASESTATION_CHANGED");
                    HwPhoneService.this.handleAntiFakeBaseStation(msg);
                    break;
                case 202:
                    HwPhoneService.this.log("EVENT_SET_LINENUM_DONE");
                    HwPhoneService.this.handleSetFunctionDone(msg);
                    break;
                case 203:
                    HwPhoneService.this.log("CMD_SET_DEEP_NO_DISTURB");
                    HwPhoneService.this.handleCmdSetDeepNoDisturb(msg);
                    break;
                case 204:
                    HwPhoneService.this.log("EVENT_SET_DEEP_NO_DISTURB_DONE");
                    HwPhoneService.this.handleSetDeepNoDisturbDone(msg);
                    break;
                case HwPhoneService.EVENT_QUERY_CARD_TYPE_DONE /*506*/:
                    str = HwPhoneService.LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Received EVENT_QUERY_CARD_TYPE_DONE on index ");
                    stringBuilder2.append(index);
                    Rlog.d(str, stringBuilder2.toString());
                    if (ar != null && ar.exception == null) {
                        if (!(ar == null || ar.result == null || !(ar.result instanceof int[]))) {
                            HwPhoneService.this.saveCardTypeProperties(((int[]) ar.result)[0], index.intValue());
                            break;
                        }
                    }
                    Rlog.d(HwPhoneService.LOG_TAG, "Received EVENT_QUERY_CARD_TYPE_DONE got exception");
                    break;
                    break;
                case HwPhoneService.EVENT_ICC_GET_ATR_DONE /*507*/:
                    str = HwPhoneService.LOG_TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Received EVENT_ICC_GET_ATR_DONE on index ");
                    stringBuilder2.append(index);
                    Rlog.d(str, stringBuilder2.toString());
                    if (ar != null && ar.exception == null) {
                        HwPhoneService.this.handleIccATR((String) ar.result, index);
                        break;
                    }
                case HwPhoneService.EVENT_GET_CARD_TRAY_INFO_DONE /*601*/:
                    HwPhoneService.this.log("EVENT_GET_CARD_TRAY_INFO_DONE");
                    HwPhoneService.this.handleGetCardTrayInfoDone(msg);
                    break;
                default:
                    HwPhoneService hwPhoneService = HwPhoneService.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("MainHandler unhandled message: ");
                    stringBuilder2.append(msg.what);
                    hwPhoneService.log(stringBuilder2.toString());
                    break;
            }
        }

        private void handlePinResult(Message msg) {
            if (msg.obj.exception != null) {
                Rlog.d(HwPhoneService.LOG_TAG, "set fail.");
                if (msg.what == HwPhoneService.EVENT_ENABLE_ICC_PIN_COMPLETE) {
                    HwPhoneService.this.setResultForPinLock = false;
                    return;
                } else if (msg.what == HwPhoneService.EVENT_CHANGE_ICC_PIN_COMPLETE) {
                    HwPhoneService.this.setResultForChangePin = false;
                    return;
                } else {
                    return;
                }
            }
            Rlog.d(HwPhoneService.LOG_TAG, "set success.");
            if (msg.what == HwPhoneService.EVENT_ENABLE_ICC_PIN_COMPLETE) {
                HwPhoneService.this.setResultForPinLock = true;
            } else if (msg.what == HwPhoneService.EVENT_CHANGE_ICC_PIN_COMPLETE) {
                HwPhoneService.this.setResultForChangePin = true;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            Rlog.d(HwPhoneService.LOG_TAG, "[enter]handleGetPreferredNetworkTypeResponse");
            AsyncResult ar = msg.obj;
            if (ar.exception == null) {
                int type = ((int[]) ar.result)[0];
                String str = HwPhoneService.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getPreferredNetworkType is ");
                stringBuilder.append(type);
                Rlog.d(str, stringBuilder.toString());
                return;
            }
            String str2 = HwPhoneService.LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getPreferredNetworkType exception=");
            stringBuilder2.append(ar.exception);
            Rlog.d(str2, stringBuilder2.toString());
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            Rlog.d(HwPhoneService.LOG_TAG, "[enter]handleSetPreferredNetworkTypeResponse");
            AsyncResult ar = msg.obj;
            if (ar == null) {
                Rlog.d(HwPhoneService.LOG_TAG, "setPreferredNetworkType ar == null");
            } else if (ar.exception != null) {
                String str = HwPhoneService.LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setPreferredNetworkType exception=");
                stringBuilder.append(ar.exception);
                Rlog.d(str, stringBuilder.toString());
                HwPhoneService.this.mPhone.getPreferredNetworkType(obtainMessage(3));
            } else {
                int setPrefMode = ((Integer) ar.userObj).intValue();
                if (HwPhoneService.this.getCurrentNetworkTypeFromDB() != setPrefMode) {
                    HwPhoneService.this.saveNetworkTypeToDB(setPrefMode);
                }
            }
        }
    }

    private static final class MainThreadRequest {
        public Object argument;
        public Object result;
        public Integer subId;

        public MainThreadRequest(Object argument) {
            this.argument = argument;
        }

        public MainThreadRequest(Object argument, Integer subId) {
            this.argument = argument;
            this.subId = subId;
        }
    }

    private class PhoneServiceReceiver extends BroadcastReceiver {
        public PhoneServiceReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.RADIO_TECHNOLOGY");
            HwPhoneService.this.mContext.registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            Rlog.d(HwPhoneService.LOG_TAG, "radio tech changed, query encrypt call feature");
            if (intent != null && "android.intent.action.RADIO_TECHNOLOGY".equals(intent.getAction())) {
                int i = 0;
                HwPhoneService.queryCount = 0;
                if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                    HwPhone[] access$1200 = HwPhoneService.this.mPhones;
                    int length = access$1200.length;
                    while (i < length) {
                        HwPhoneService.this.handleEventQueryEncryptCall(access$1200[i]);
                        i++;
                    }
                    return;
                }
                HwPhoneService.this.handleEventQueryEncryptCall(HwPhoneService.this.mPhone);
            }
        }
    }

    private final class PhoneStateHandler extends Handler {
        PhoneStateHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 4) {
                switch (i) {
                    case 1:
                        handleRadioAvailable(msg);
                        return;
                    case 2:
                        handleRadioNotAvailable(msg);
                        return;
                    default:
                        return;
                }
            }
            handleCommonImsaToMapconInfo(msg);
        }

        private void handleRadioAvailable(Message msg) {
            AsyncResult ar = msg.obj;
            int phoneId = HwPhoneService.this.getPhoneId(msg).intValue();
            if (ar.exception == null) {
                HwPhoneService.this.notifyPhoneEventWithCallback(phoneId, 1, 0, null);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("radio available exception: ");
            stringBuilder.append(ar.exception);
            HwPhoneService.loge(stringBuilder.toString());
        }

        private void handleRadioNotAvailable(Message msg) {
            AsyncResult ar = msg.obj;
            int phoneId = HwPhoneService.this.getPhoneId(msg).intValue();
            if (ar.exception == null) {
                HwPhoneService.this.notifyPhoneEventWithCallback(phoneId, 2, 0, null);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("radio not available exception: ");
            stringBuilder.append(ar.exception);
            HwPhoneService.loge(stringBuilder.toString());
        }

        private void handleCommonImsaToMapconInfo(Message msg) {
            AsyncResult ar = msg.obj;
            int phoneId = HwPhoneService.this.getPhoneId(msg).intValue();
            if (ar.exception == null) {
                Bundle bundle = new Bundle();
                bundle.putByteArray("imsa2mapcon_msg", ar.result);
                HwPhoneService.this.notifyPhoneEventWithCallback(phoneId, 4, 0, bundle);
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("imsa to mapcon info exception: ");
            stringBuilder.append(ar.exception);
            HwPhoneService.loge(stringBuilder.toString());
        }
    }

    private static class Record {
        IBinder binder;
        IPhoneCallback callback;
        int events;
        int phoneId;

        private Record() {
            this.phoneId = -1;
        }

        /* synthetic */ Record(AnonymousClass1 x0) {
            this();
        }

        boolean matchPhoneStateListenerEvent(int events) {
            return (this.callback == null || (this.events & events) == 0) ? false : true;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("binder=");
            stringBuilder.append(this.binder);
            stringBuilder.append(" callback=");
            stringBuilder.append(this.callback);
            stringBuilder.append(" phoneId=");
            stringBuilder.append(this.phoneId);
            stringBuilder.append(" events=");
            stringBuilder.append(Integer.toHexString(this.events));
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        boolean matchPhoneEvent(int events) {
            return (this.callback == null || (this.events & events) == 0) ? false : true;
        }
    }

    private static final class UiccAuthPara {
        byte[] auth;
        int auth_type;
        byte[] rand;

        public UiccAuthPara(int auth_type, byte[] rand, byte[] auth) {
            this.auth_type = auth_type;
            this.rand = rand;
            this.auth = auth;
        }
    }

    public HwPhoneService() {
        this.mMessageThread.start();
        this.mMainHandler = new MainHandler(this.mMessageThread.getLooper());
        this.mPhoneStateHandler = new PhoneStateHandler(this.mMessageThread.getLooper());
        this.mHwCustPhoneService = (HwCustPhoneService) HwCustUtils.createObj(HwCustPhoneService.class, new Object[]{this, this.mMessageThread.getLooper()});
    }

    public void setPhone(Phone phone, Context context) {
        this.mPhone = new HwPhone(phone);
        this.mContext = context;
        if (this.mHwCustPhoneService != null && this.mHwCustPhoneService.isDisable2GServiceCapabilityEnabled()) {
            this.mHwCustPhoneService.setPhone(this.mPhone, this.mContext);
        }
        initService();
    }

    public void setPhone(Phone[] phones, Context context) {
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        this.mPhones = new HwPhone[numPhones];
        for (int i = 0; i < numPhones; i++) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Creating HwPhone sub = ");
            stringBuilder.append(i);
            Rlog.d(str, stringBuilder.toString());
            this.mPhones[i] = new HwPhone(phones[i]);
        }
        this.mPhone = this.mPhones[0];
        this.mContext = context;
        if (this.mHwCustPhoneService != null && this.mHwCustPhoneService.isDisable2GServiceCapabilityEnabled()) {
            this.mHwCustPhoneService.setPhone(this.mPhone, this.mContext);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setPhone mPhones = ");
        stringBuilder2.append(this.mPhones);
        log(stringBuilder2.toString());
        initService();
        this.mInCallDataStateMachine = new InCallDataStateMachine(context, phones);
        this.mInCallDataStateMachine.start();
    }

    private static void saveInstance(HwPhoneService service) {
        sInstance = service;
    }

    public static HwPhoneService getInstance() {
        return sInstance;
    }

    private void initService() {
        Rlog.d(LOG_TAG, "initService()");
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService("appops");
        ServiceManager.addService("phone_huawei", this);
        saveInstance(this);
        initPrefNetworkTypeChecker();
        registerForRadioOnInner();
        if (this.mPhoneServiceReceiver == null) {
            this.mPhoneServiceReceiver = new PhoneServiceReceiver();
        }
        registerMDMSmsReceiver();
        registerSetRadioCapDoneReceiver();
        if (HuaweiTelephonyConfigs.isHisiPlatform()) {
            registerForIccStatusChanged();
        }
    }

    private void registerForIccStatusChanged() {
        Rlog.d(LOG_TAG, "registerForIccStatusChanged");
        if (this.mPhones == null) {
            Rlog.d(LOG_TAG, "register failed, mphones is null");
            return;
        }
        for (int i = 0; i < SIM_NUM; i++) {
            Integer index = Integer.valueOf(i);
            this.mPhones[i].getPhone().mCi.registerForIccStatusChanged(this.mMainHandler, 54, index);
            this.mPhones[i].getPhone().mCi.registerForAvailable(this.mMainHandler, 54, index);
        }
    }

    private void onIccStatusChanged(Integer index) {
        this.mPhones[index.intValue()].getPhone().mCi.queryCardType(this.mMainHandler.obtainMessage(EVENT_QUERY_CARD_TYPE_DONE, index));
        this.mPhones[index.intValue()].getPhone().mCi.iccGetATR(this.mMainHandler.obtainMessage(EVENT_ICC_GET_ATR_DONE, index));
    }

    private void saveCardTypeProperties(int cardTypeResult, int index) {
        int cardType = -1;
        int uiccOrIcc = (cardTypeResult & 240) >> 4;
        int appType = cardTypeResult & 15;
        switch (appType) {
            case 1:
                if (uiccOrIcc != 2) {
                    if (uiccOrIcc == 1) {
                        cardType = 10;
                        break;
                    }
                }
                cardType = 20;
                break;
                break;
            case 2:
                cardType = 30;
                break;
            case 3:
                if (uiccOrIcc != 2) {
                    if (uiccOrIcc == 1) {
                        cardType = 41;
                        break;
                    }
                }
                cardType = 43;
                break;
                break;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("uiccOrIcc :  ");
        stringBuilder.append(uiccOrIcc);
        stringBuilder.append(", appType : ");
        stringBuilder.append(appType);
        stringBuilder.append(", cardType : ");
        stringBuilder.append(cardType);
        Rlog.d(str, stringBuilder.toString());
        if (index == 0) {
            SystemProperties.set(HwFullNetworkConstants.CARD_TYPE_SIM1, String.valueOf(cardType));
        } else {
            SystemProperties.set(HwFullNetworkConstants.CARD_TYPE_SIM2, String.valueOf(cardType));
        }
    }

    private void handleIccATR(String strATR, Integer index) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleIccATR, ATR: [");
        stringBuilder.append(strATR);
        stringBuilder.append("], index:[");
        stringBuilder.append(index);
        stringBuilder.append("]");
        Rlog.d(str, stringBuilder.toString());
        if (strATR == null || strATR.isEmpty()) {
            strATR = "null";
        }
        if (strATR.length() > 66) {
            Rlog.d(LOG_TAG, "strATR.length() greater than PROP_VALUE_MAX");
            strATR = strATR.substring(0, 66);
        }
        if (index.intValue() == 0) {
            SystemProperties.set("gsm.sim.hw_atr", strATR);
        } else {
            SystemProperties.set("gsm.sim.hw_atr1", strATR);
        }
    }

    private Integer getCiIndex(Message msg) {
        Integer index = new Integer(0);
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

    private void log(String msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhoneIntfMgr] ");
        stringBuilder.append(msg);
        Rlog.d(str, stringBuilder.toString());
    }

    private void logForOemHook(String msg) {
        Rlog.d("HwPhoneService_OEMHOOK", msg);
    }

    private void logForSar(String msg) {
        Rlog.d("HwPhoneService_SAR", msg);
    }

    public String getDemoString() {
        enforceReadPermission();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(this.mPhone);
        stringBuilder.append(this.mContext);
        return stringBuilder.toString();
    }

    public String getMeidForSubscriber(int slot) {
        if (!canReadPhoneState(slot, "getMeid") || slot < 0 || slot >= this.mPhones.length) {
            return null;
        }
        if (-1 != SystemProperties.getInt("persist.radio.stack_id_0", -1)) {
            slot = SystemProperties.getInt("persist.radio.stack_id_0", -1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("QC after switch slot = ");
            stringBuilder.append(slot);
            log(stringBuilder.toString());
        }
        return this.mPhones[slot].getMeid();
    }

    public String getPesnForSubscriber(int slot) {
        enforceReadPermission();
        if (slot < 0 || slot >= this.mPhones.length) {
            return null;
        }
        if (-1 != SystemProperties.getInt("persist.radio.stack_id_0", -1)) {
            slot = SystemProperties.getInt("persist.radio.stack_id_0", -1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("QC after switch slot = ");
            stringBuilder.append(slot);
            log(stringBuilder.toString());
        }
        return this.mPhones[slot].getPesn();
    }

    public int getSubState(int subId) {
        enforceReadPermission();
        if (SubscriptionController.getInstance() != null) {
            return SubscriptionController.getInstance().getSubState(subId);
        }
        return 0;
    }

    public void setUserPrefDataSlotId(int slotId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        if (HwSubscriptionManager.getInstance() != null) {
            HwSubscriptionManager.getInstance().setUserPrefDataSlotId(slotId);
        } else {
            Rlog.e(LOG_TAG, "HwSubscriptionManager is null!!");
        }
    }

    public int getDataStateForSubscriber(int subId) {
        enforceReadPermission();
        return PhoneConstantConversions.convertDataState(this.mPhones[SubscriptionController.getInstance().getPhoneId(subId)].getDataConnectionState());
    }

    public String getNVESN() {
        enforceReadPermission();
        return this.mPhone.getNVESN();
    }

    public void closeRrc() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        int dataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
        if (dataSubId >= 0 && dataSubId < this.mPhones.length) {
            this.mPhones[dataSubId].closeRrc();
        }
    }

    public boolean isCTCdmaCardInGsmMode() {
        enforceReadPermission();
        int i = 0;
        if (!HuaweiTelephonyConfigs.isChinaTelecom()) {
            return false;
        }
        boolean noCdmaPhone = true;
        while (i < this.mPhones.length) {
            if (this.mPhones[i].isCDMAPhone()) {
                noCdmaPhone = false;
                break;
            }
            i++;
        }
        return noCdmaPhone;
    }

    public boolean isLTESupported() {
        boolean result;
        int networkMode = RILConstants.PREFERRED_NETWORK_MODE;
        if (!(networkMode == 16 || networkMode == 18 || networkMode == 21 || networkMode == 52)) {
            switch (networkMode) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    break;
                default:
                    switch (networkMode) {
                        case 13:
                        case 14:
                            break;
                        default:
                            result = true;
                            break;
                    }
            }
        }
        result = false;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isLTESupported ");
        stringBuilder.append(result);
        Rlog.i(str, stringBuilder.toString());
        return result;
    }

    public void setDefaultMobileEnable(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        int i = 0;
        if (!(Global.getInt(this.mContext.getContentResolver(), "mobile_data_always_on", 0) != 0) || enabled) {
            int numPhones = TelephonyManager.getDefault().getPhoneCount();
            while (i < numPhones) {
                this.mPhones[i].setDefaultMobileEnable(enabled);
                TelephonyNetworkFactory telephonyNetworkFactory = PhoneFactory.getTelephonyNetworkFactory(i);
                if (telephonyNetworkFactory != null) {
                    telephonyNetworkFactory.reconnectDefaultRequestRejectByWifi();
                }
                i++;
            }
            Phone vsimPhone = HwVSimPhoneFactory.getVSimPhone();
            if (vsimPhone != null) {
                HwPhone hwVsimPhone = new HwPhone(vsimPhone);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setDefaultMobileEnable to ");
                stringBuilder.append(enabled);
                stringBuilder.append(" for vsimPhone");
                Rlog.d(str, stringBuilder.toString());
                hwVsimPhone.setDefaultMobileEnable(enabled);
            }
            HwVSimNetworkFactory vsimNetworkFactory = HwVSimPhoneFactory.getsVSimNetworkFactory();
            if (vsimNetworkFactory != null) {
                vsimNetworkFactory.reconnectDefaultRequestRejectByWifi();
            }
            return;
        }
        Rlog.d(LOG_TAG, "setDefaultMobileEnable: isDataAlwaysOn && !enabled, return.");
    }

    public void setDataEnabledWithoutPromp(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        int phoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (phoneId >= 0 && phoneId < this.mPhones.length) {
            this.mPhones[phoneId].setDataEnabledWithoutPromp(enabled);
        }
    }

    public void setDataRoamingEnabledWithoutPromp(boolean enabled) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        int phoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
        if (phoneId >= 0 && phoneId < this.mPhones.length) {
            this.mPhones[phoneId].setDataRoamingEnabledWithoutPromp(enabled);
        }
    }

    private Object sendRequest(int command, Object argument) {
        return sendRequest(command, argument, null);
    }

    private Object sendRequest(int command, Object argument, Integer subId) {
        if (Looper.myLooper() != this.mMainHandler.getLooper()) {
            MainThreadRequest request = new MainThreadRequest(argument, subId);
            this.mMainHandler.obtainMessage(command, request).sendToTarget();
            synchronized (request) {
                while (request.result == null) {
                    try {
                        request.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return request.result;
        }
        throw new RuntimeException("This method will deadlock if called from the main thread.");
    }

    public void setPreferredNetworkType(int nwMode) {
        enforceModifyPermissionOrCarrierPrivilege();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[enter]setPreferredNetworkType ");
        stringBuilder.append(nwMode);
        Rlog.d(str, stringBuilder.toString());
        if (TelephonyManager.getDefault().isMultiSimEnabled() && HwFullNetworkManager.getInstance().getBalongSimSlot() == 1) {
            this.mPhone = this.mPhones[1];
        } else {
            this.mPhone = this.mPhones[0];
        }
        if (this.mPhone == null) {
            Rlog.e(LOG_TAG, "4G-Switch mPhone is null. return!");
        } else {
            this.mPhone.setPreferredNetworkType(nwMode, this.mMainHandler.obtainMessage(4, Integer.valueOf(nwMode)));
        }
    }

    public void setLteServiceAbility(int ability) {
        enforceModifyPermissionOrCarrierPrivilege();
        setLteServiceAbilityForSubId(IS_4G_SWITCH_SUPPORTED ? getDefault4GSlotId() : 0, ability);
    }

    public void setLteServiceAbilityForSubId(int subId, int ability) {
        enforceModifyPermissionOrCarrierPrivilege();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("=4G-Switch= setLteServiceAbility: ability=");
        stringBuilder.append(ability);
        stringBuilder.append(", subId=");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        if (!isValidSlotId(subId)) {
            return;
        }
        if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK || HwFullNetworkConfig.IS_QCRIL_CROSS_MAPPING) {
            HwFullNetworkManager.getInstance().setLteServiceAbilityForQCOM(subId, ability, HwNetworkTypeUtils.lteOnMappingMode);
            return;
        }
        HwPhone phone;
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            phone = this.mPhones[subId];
            if (subId == getDefault4GSlotId()) {
                this.mPhone = phone;
            }
        } else {
            phone = this.mPhone;
        }
        if (phone == null) {
            loge("4G-Switch phone is null. return!");
            return;
        }
        int networkType;
        if (HwNetworkTypeUtils.IS_MODEM_FULL_PREFMODE_SUPPORTED) {
            networkType = calculateNetworkType(ability);
        } else if (phone.isCDMAPhone()) {
            networkType = ability == 1 ? 8 : 4;
        } else if (IS_GSM_NONSUPPORT) {
            networkType = ability == 1 ? 12 : 2;
        } else {
            networkType = ability == 1 ? 9 : 3;
        }
        boolean isQcomSRAL = HuaweiTelephonyConfigs.isQcomPlatform() && !TelephonyManager.getDefault().isMultiSimEnabled() && IS_FULL_NETWORK_SUPPORTED;
        if (isQcomSRAL) {
            if (phone.isCDMAPhone()) {
                networkType = ability == 1 ? 10 : 7;
            } else {
                networkType = ability == 1 ? 20 : 18;
            }
        }
        if (this.mHwCustPhoneService != null && this.mHwCustPhoneService.isDisable2GServiceCapabilityEnabled() && this.mHwCustPhoneService.get2GServiceAbility() == 0) {
            networkType = this.mHwCustPhoneService.getNetworkTypeBaseOnDisabled2G(networkType);
        }
        if (HwTelephonyFactory.getHwNetworkManager().isNetworkModeAsynchronized(phone.getPhone())) {
            HwTelephonyFactory.getHwNetworkManager().handle4GSwitcherForNoMdn(phone.getPhone(), networkType);
            sendLteServiceSwitchResult(subId, true);
            return;
        }
        phone.setPreferredNetworkType(networkType, this.mMainHandler.obtainMessage(5, networkType, subId));
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("=4G-Switch= setPreferredNetworkType-> ");
        stringBuilder2.append(networkType);
        log(stringBuilder2.toString());
    }

    private int calculateNetworkType(int ability) {
        int mappingNetworkType;
        int curPrefMode = getCurrentNetworkTypeFromDB();
        if (ability == 1) {
            mappingNetworkType = HwNetworkTypeUtils.getOnModeFromMapping(curPrefMode);
        } else {
            mappingNetworkType = HwNetworkTypeUtils.getOffModeFromMapping(curPrefMode);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("=4G-Switch= curPrefMode = ");
        stringBuilder.append(curPrefMode);
        stringBuilder.append(" ,mappingNetworkType = ");
        stringBuilder.append(mappingNetworkType);
        log(stringBuilder.toString());
        if (-1 != mappingNetworkType) {
            return mappingNetworkType;
        }
        return ability == 1 ? HwNetworkTypeUtils.lteOnMappingMode : HwNetworkTypeUtils.lteOffMappingMode;
    }

    private int getCurrentNetworkTypeFromDB() {
        return getCurrentNetworkTypeFromDB(IS_4G_SWITCH_SUPPORTED ? getDefault4GSlotId() : 0);
    }

    private int getCurrentNetworkTypeFromDB(int subId) {
        StringBuilder stringBuilder;
        int curPrefMode = -1;
        try {
            int curPrefMode2;
            if (HwNetworkTypeUtils.IS_MODEM_FULL_PREFMODE_SUPPORTED && TelephonyManager.getDefault().isMultiSimEnabled()) {
                curPrefMode2 = TelephonyManager.getIntAtIndex(this.mContext.getContentResolver(), "preferred_network_mode", subId);
            } else if (IS_DUAL_IMS_SUPPORTED && TelephonyManager.getDefault().isMultiSimEnabled()) {
                ContentResolver contentResolver = this.mContext.getContentResolver();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("preferred_network_mode");
                stringBuilder2.append(subId);
                curPrefMode = Global.getInt(contentResolver, stringBuilder2.toString(), -1);
                int curModeOfDefault4G = Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode", -1);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("=4G-Switch= curPrefMode:");
                stringBuilder2.append(curPrefMode);
                stringBuilder2.append(" curModeOfDefault4G:");
                stringBuilder2.append(curModeOfDefault4G);
                log(stringBuilder2.toString());
                StringBuilder stringBuilder3;
                if (-1 == curModeOfDefault4G && subId == getDefault4GSlotId()) {
                    curPrefMode = RILConstants.PREFERRED_NETWORK_MODE;
                    saveNetworkTypeToDB(curPrefMode);
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("=4G-Switch= curModeOfDefault4G is invalid, set sub");
                    stringBuilder3.append(subId);
                    stringBuilder3.append(" is ");
                    stringBuilder3.append(curPrefMode);
                    log(stringBuilder3.toString());
                } else if (curModeOfDefault4G != curPrefMode && subId == getDefault4GSlotId()) {
                    saveNetworkTypeToDB(subId, curModeOfDefault4G);
                    curPrefMode = curModeOfDefault4G;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("=4G-Switch= curModeOfDefault4G of sub");
                    stringBuilder3.append(subId);
                    stringBuilder3.append(" is ");
                    stringBuilder3.append(curModeOfDefault4G);
                    loge(stringBuilder3.toString());
                } else if (-1 == curPrefMode) {
                    curPrefMode = isDualImsSwitchOpened() ? RILConstants.PREFERRED_NETWORK_MODE : 3;
                    saveNetworkTypeToDB(subId, curPrefMode);
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("=4G-Switch= curPrefMode of sub");
                    stringBuilder3.append(subId);
                    stringBuilder3.append(" is ");
                    stringBuilder3.append(curPrefMode);
                    loge(stringBuilder3.toString());
                }
                curPrefMode2 = curPrefMode;
            } else {
                curPrefMode2 = Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode", -1);
            }
            return curPrefMode2;
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("=4G-Switch=  PREFERRED_NETWORK_MODE RuntimeException = ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return curPrefMode;
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("=4G-Switch=  PREFERRED_NETWORK_MODE Exception = ");
            stringBuilder.append(e2);
            loge(stringBuilder.toString());
            return curPrefMode;
        }
    }

    private void saveNetworkTypeToDB(int setPrefMode) {
        saveNetworkTypeToDB(IS_4G_SWITCH_SUPPORTED ? getDefault4GSlotId() : 0, setPrefMode);
    }

    private void saveNetworkTypeToDB(int subId, int setPrefMode) {
        if (HwNetworkTypeUtils.IS_MODEM_FULL_PREFMODE_SUPPORTED && TelephonyManager.getDefault().isMultiSimEnabled()) {
            TelephonyManager.putIntAtIndex(this.mContext.getContentResolver(), "preferred_network_mode", subId, setPrefMode);
        } else if (IS_DUAL_IMS_SUPPORTED && TelephonyManager.getDefault().isMultiSimEnabled()) {
            if (subId == getDefault4GSlotId()) {
                Global.putInt(this.mContext.getContentResolver(), "preferred_network_mode", setPrefMode);
            }
            ContentResolver contentResolver = this.mContext.getContentResolver();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("preferred_network_mode");
            stringBuilder.append(subId);
            Global.putInt(contentResolver, stringBuilder.toString(), setPrefMode);
        } else {
            Global.putInt(this.mContext.getContentResolver(), "preferred_network_mode", setPrefMode);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("=4G-Switch= save network mode ");
        stringBuilder2.append(setPrefMode);
        stringBuilder2.append(" to database success!");
        log(stringBuilder2.toString());
    }

    private boolean isDualImsSwitchOpened() {
        return 1 == SystemProperties.getInt("persist.radio.dualltecap", 0);
    }

    private void handleSetLteSwitchDone(Message msg) {
        log("=4G-Switch= in handleSetLteSwitchDone");
        AsyncResult ar = msg.obj;
        if (ar == null) {
            loge("=4G-Switch= ar is null!");
            return;
        }
        int setPrefMode = msg.arg1;
        int subId = msg.arg2;
        if (ar.exception != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("=4G-Switch= ");
            stringBuilder.append(ar.exception);
            loge(stringBuilder.toString());
            sendLteServiceSwitchResult(subId, false);
            return;
        }
        int curPrefMode = getCurrentNetworkTypeFromDB(subId);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("=4G-Switch= subId:");
        stringBuilder2.append(subId);
        stringBuilder2.append(" curPrefMode in db:");
        stringBuilder2.append(curPrefMode);
        stringBuilder2.append(" setPrefMode:");
        stringBuilder2.append(setPrefMode);
        log(stringBuilder2.toString());
        if (curPrefMode != setPrefMode) {
            saveNetworkTypeToDB(subId, setPrefMode);
        }
        sendLteServiceSwitchResult(subId, true);
    }

    private void sendLteServiceSwitchResult(int subId, boolean result) {
        if (this.mContext == null) {
            loge("=4G-Switch= mContext is null. return!");
            return;
        }
        Intent intent = new Intent("com.huawei.telephony.PREF_4G_SWITCH_DONE");
        intent.putExtra("subscription", subId);
        intent.putExtra("setting_result", result);
        this.mContext.sendOrderedBroadcast(intent, "android.permission.READ_PHONE_STATE");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("=4G-Switch= result is ");
        stringBuilder.append(result);
        stringBuilder.append(". broadcast PREFERRED_4G_SWITCH_DONE");
        log(stringBuilder.toString());
    }

    public int getLteServiceAbility() {
        enforceReadPermission();
        return getLteServiceAbilityForSubId(IS_4G_SWITCH_SUPPORTED ? getDefault4GSlotId() : 0);
    }

    public int getLteServiceAbilityForSubId(int subId) {
        enforceReadPermission();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLteServiceAbility, subId = ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        if (!isValidSlotId(subId)) {
            return 0;
        }
        int curPrefMode = getCurrentNetworkTypeFromDB(subId);
        int ability = HwNetworkTypeUtils.isLteServiceOn(curPrefMode);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getLteServiceAbility, curPrefMode = ");
        stringBuilder2.append(curPrefMode);
        stringBuilder2.append(", ability =");
        stringBuilder2.append(ability);
        log(stringBuilder2.toString());
        return ability;
    }

    public int get2GServiceAbility() {
        if (this.mHwCustPhoneService == null || !this.mHwCustPhoneService.isDisable2GServiceCapabilityEnabled()) {
            return 0;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
        return this.mHwCustPhoneService.get2GServiceAbility();
    }

    public void set2GServiceAbility(int ability) {
        if (this.mHwCustPhoneService != null && this.mHwCustPhoneService.isDisable2GServiceCapabilityEnabled()) {
            enforceModifyPermissionOrCarrierPrivilege();
            this.mHwCustPhoneService.set2GServiceAbility(ability);
        }
    }

    private void enforceModifyPermissionOrCarrierPrivilege() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            log("No modify permission, check carrier privilege next.");
            if (hasCarrierPrivileges() != 1) {
                loge("No Carrier Privilege.");
                throw new SecurityException("No modify permission or carrier privilege.");
            }
        }
    }

    private int hasCarrierPrivileges() {
        if (this.mPhone == null) {
            log("hasCarrierPrivileges: mPhone is null");
            return -1;
        }
        UiccCard card = UiccController.getInstance().getUiccCard(this.mPhone.getPhone().getPhoneId());
        if (card != null) {
            return card.getCarrierPrivilegeStatusForCurrentTransaction(this.mContext.getPackageManager());
        }
        loge("hasCarrierPrivileges: No UICC");
        return -1;
    }

    private static void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public boolean isSubDeactivedByPowerOff(long sub) {
        enforceReadPermission();
        Rlog.d(LOG_TAG, "isSubDeactivedByPowerOff: in HuaweiPhoneService");
        SubscriptionController sc = SubscriptionController.getInstance();
        if (TelephonyManager.getDefault().getSimState(sc.getSlotIndex((int) sub)) == 5 && sc.getSubState((int) sub) == 0) {
            return true;
        }
        return false;
    }

    public boolean isNeedToRadioPowerOn(long sub) {
        enforceReadPermission();
        Rlog.d(LOG_TAG, "isNeedToRadioPowerOn: in HuaweiPhoneService");
        if (HwModemCapability.isCapabilitySupport(9) || !(MultiSimVariants.DSDS == TelephonyManager.getDefault().getMultiSimConfiguration() || SystemProperties.getBoolean("ro.hwpp.set_uicc_by_radiopower", false))) {
            Rlog.d(LOG_TAG, "isNeedToRadioPowerOn: hisi dsds not in");
            int phoneId = SubscriptionController.getInstance().getPhoneId((int) sub);
            if (PhoneFactory.getPhone(phoneId) != null && PhoneFactory.getPhone(phoneId).getServiceState().getState() == 3) {
                return true;
            }
        }
        if (!isSubDeactivedByPowerOff((long) ((int) sub))) {
            return true;
        }
        SubscriptionController.getInstance().activateSubId((int) sub);
        return false;
    }

    private void enforceReadPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
    }

    public void updateCrurrentPhone(int lteSlot) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCrurrentPhone with lteSlot = ");
        stringBuilder.append(lteSlot);
        Rlog.d(str, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        if (TelephonyManager.getDefault().getPhoneCount() > lteSlot) {
            this.mPhone = this.mPhones[lteSlot];
        } else {
            Rlog.e(LOG_TAG, "Invalid slot ID");
        }
    }

    public void setDefaultDataSlotId(int slotId) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDefaultDataSlotId: slotId = ");
        stringBuilder.append(slotId);
        Rlog.d(str, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        if (slotId < 0 || slotId >= SIM_NUM) {
            Rlog.d(LOG_TAG, "setDefaultDataSlotId: invalid slotId!!!");
            return;
        }
        Global.putInt(this.mContext.getContentResolver(), USER_DATACALL_SUBSCRIPTION, slotId);
        if (SubscriptionController.getInstance() != null) {
            SubscriptionController.getInstance().setDefaultDataSubId(SubscriptionController.getInstance().getSubId(slotId)[0]);
        } else {
            Rlog.d(LOG_TAG, "SubscriptionController.getInstance()! null");
        }
        Rlog.d(LOG_TAG, "setDefaultDataSlotId done");
    }

    public int getDefault4GSlotId() {
        try {
            return System.getInt(this.mContext.getContentResolver(), "switch_dual_card_slots");
        } catch (SettingNotFoundException e) {
            Rlog.d(LOG_TAG, "Settings Exception Reading Dual Sim Switch Dual Card Slots Values");
            return 0;
        }
    }

    public void setDefault4GSlotId(int slotId, Message response) {
        int uid = Binder.getCallingUid();
        StringBuilder stringBuilder;
        if (!HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE || uid == 1000 || uid == 1001 || uid == 0) {
            enforceModifyPermissionOrCarrierPrivilege();
            stringBuilder = new StringBuilder();
            stringBuilder.append("in setDefault4GSlotId for slotId: ");
            stringBuilder.append(slotId);
            log(stringBuilder.toString());
            Message msg = this.mMainHandler.obtainMessage(200);
            msg.obj = response;
            HwFullNetworkManager.getInstance().setMainSlot(slotId, msg);
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("setDefault4GSlotId: Disallowed call for uid ");
        stringBuilder.append(uid);
        loge(stringBuilder.toString());
    }

    public boolean isSetDefault4GSlotIdEnabled() {
        enforceReadPermission();
        log("in isSetDefault4GSlotIdEnabled.");
        if (HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimCauseCardReload() || HwVSimUtils.isSubActivationUpdate() || !HwVSimUtils.isAllowALSwitch()) {
            log("vsim is working, so isSetDefault4GSlotIdEnabled return false");
            return false;
        } else if (!TelephonyManager.getDefault().isMultiSimEnabled()) {
            return false;
        } else {
            SubscriptionController sc = SubscriptionController.getInstance();
            int[] sub1 = sc.getSubId(0);
            int[] sub2 = sc.getSubId(1);
            if (TelephonyManager.getDefault().getSimState(0) == 5 && sc.getSubState(sub1[0]) == 0 && TelephonyManager.getDefault().getSimState(1) == 5 && sc.getSubState(sub2[0]) == 0) {
                return false;
            }
            if (!HwFullNetworkConfig.IS_HISI_DSDX || ((TelephonyManager.getDefault().getSimState(0) != 5 || sc.getSubState(sub1[0]) != 0) && (TelephonyManager.getDefault().getSimState(1) != 5 || sc.getSubState(sub2[0]) != 0))) {
                return HwFullNetworkManager.getInstance().isSwitchDualCardSlotsEnabled();
            }
            log("isSetDefault4GSlotIdEnabled return false when has sim INACTIVE when IS_HISI_DSDS_AUTO_SWITCH_4G_SLOT");
            return false;
        }
    }

    public void waitingSetDefault4GSlotDone(boolean waiting) {
        enforceReadPermission();
        if (!HwModemCapability.isCapabilitySupport(9)) {
            HwFullNetworkManager.getInstance().setWaitingSwitchBalongSlot(waiting);
        }
    }

    public int getPreferredDataSubscription() {
        enforceReadPermission();
        int subId = SubscriptionController.getInstance().getPreferredDataSubscription();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPreferredDataSubscription return subId = ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        return subId;
    }

    public int getOnDemandDataSubId() {
        enforceReadPermission();
        int subId = SubscriptionController.getInstance().getOnDemandDataSubId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getOnDemandDataSubId return subId = ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        return subId;
    }

    public String getCdmaGsmImsi() {
        Rlog.d(LOG_TAG, "getCdmaGsmImsi: in HWPhoneService");
        enforceReadPermission();
        for (HwPhone hp : this.mPhones) {
            if (hp.getHwPhoneType() == 2) {
                return hp.getCdmaGsmImsi();
            }
        }
        return null;
    }

    public String getCdmaGsmImsiForSubId(int subId) {
        enforceReadPermission();
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCdmaGsmImsi: in HWPhoneService subId:");
        stringBuilder.append(subId);
        Rlog.d(str, stringBuilder.toString());
        if (isValidSlotId(subId)) {
            Phone phone = PhoneFactory.getPhone(subId);
            if (phone == null) {
                return null;
            }
            if (phone.getCdmaGsmImsi() != null || !isCtSimCard(subId)) {
                return phone.getCdmaGsmImsi();
            }
            Rlog.d(LOG_TAG, "getCdmaGsmImsi is null");
            IccRecords iccRecords = UiccController.getInstance().getIccRecords(subId, 2);
            if (iccRecords != null) {
                return iccRecords.getCdmaGsmImsi();
            }
            return null;
        }
        Rlog.d(LOG_TAG, "subId is not avaible!");
        return null;
    }

    public int getUiccCardType(int slotId) {
        Rlog.d(LOG_TAG, "getUiccCardType: in HwPhoneService");
        enforceReadPermission();
        if (slotId < 0 || slotId >= this.mPhones.length) {
            return this.mPhone.getUiccCardType();
        }
        return this.mPhones[slotId].getUiccCardType();
    }

    public boolean isCardUimLocked(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCardUimLocked for slotId ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        enforceReadPermission();
        UiccCard card = UiccController.getInstance().getUiccCard(slotId);
        if (card != null) {
            return card.isCardUimLocked();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("isCardUimLocked: No UICC for slotId");
        stringBuilder2.append(slotId);
        loge(stringBuilder2.toString());
        return false;
    }

    public int getSpecCardType(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getSpecCardType for slotId ");
        stringBuilder.append(slotId);
        log(stringBuilder.toString());
        enforceReadPermission();
        return HwFullNetworkManager.getInstance().getSpecCardType(slotId);
    }

    public boolean isRadioOn(int slot) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isRadioOn for slotId ");
        stringBuilder.append(slot);
        log(stringBuilder.toString());
        enforceReadPermission();
        if (slot < 0 || slot >= this.mPhones.length) {
            return false;
        }
        return this.mPhones[slot].getPhone().isRadioOn();
    }

    public Bundle getCellLocation(int slotId) {
        Rlog.d(LOG_TAG, "getCellLocation: in HwPhoneService");
        int uid = Binder.getCallingUid();
        if (uid == 1000 || uid == 1001) {
            enforceCellLocationPermission("getCellLocation");
            Bundle data = new Bundle();
            CellLocation cellLoc;
            if (slotId >= 0 && slotId < this.mPhones.length) {
                cellLoc = this.mPhones[slotId].getCellLocation();
                if (cellLoc != null) {
                    cellLoc.fillInNotifierBundle(data);
                }
            } else if (sIsPlatformSupportVSim && slotId == 2) {
                cellLoc = getVSimPhone().getCellLocation();
                if (cellLoc != null) {
                    cellLoc.fillInNotifierBundle(data);
                }
            } else {
                cellLoc = this.mPhone.getCellLocation();
                if (cellLoc != null) {
                    cellLoc.fillInNotifierBundle(data);
                }
            }
            return data;
        }
        Rlog.e(LOG_TAG, "getCellLocation not allowed for third-party apps");
        return null;
    }

    private void enforceCellLocationPermission(String message) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION", message);
    }

    public String getCdmaMlplVersion() {
        Rlog.d(LOG_TAG, "getCdmaMlplVersion: in HwPhoneService");
        enforceReadPermission();
        for (HwPhone hp : this.mPhones) {
            if (hp.getHwPhoneType() == 2) {
                return hp.getCdmaMlplVersion();
            }
        }
        return null;
    }

    public String getCdmaMsplVersion() {
        Rlog.d(LOG_TAG, "getCdmaMsplVersion: in HwPhoneService");
        enforceReadPermission();
        for (HwPhone hp : this.mPhones) {
            if (hp.getHwPhoneType() == 2) {
                return hp.getCdmaMsplVersion();
            }
        }
        return null;
    }

    private boolean isSystemApp(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            return false;
        }
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if (appInfo == null || (appInfo.flags & 1) == 0) {
                return false;
            }
            return true;
        } catch (NameNotFoundException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(" not found.");
            Rlog.e(str, stringBuilder.toString());
        }
    }

    private boolean canReadPhoneState(int subId, String message) {
        String callingPackage = null;
        PackageManager pm = this.mContext.getPackageManager();
        if (pm != null) {
            String[] callingPackageName = pm.getPackagesForUid(Binder.getCallingUid());
            if (callingPackageName != null) {
                callingPackage = callingPackageName[0];
            }
        }
        try {
            return TelephonyPermissions.checkCallingOrSelfReadPhoneState(this.mContext, subId, callingPackage, message);
        } catch (SecurityException phoneStateException) {
            if (isSystemApp(callingPackage)) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(callingPackage);
                stringBuilder.append(" allowed.");
                Rlog.d(str, stringBuilder.toString());
                return true;
            }
            throw phoneStateException;
        }
    }

    public String getUniqueDeviceId(int scope) {
        IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
        if (manager != null) {
            manager.sendBehavior(BehaviorId.PHONEINTERFACE_GETDEVICEID);
        }
        isReadPhoneNumberBlocked();
        if (canReadPhoneState(0, "getDeviceId")) {
            String sharedPref = DEVICEID_PREF;
            if (1 == scope) {
                sharedPref = IMEI_PREF;
            }
            String deviceId = getDeviceIdFromSP(sharedPref);
            if (deviceId != null) {
                return deviceId;
            }
            String newDeviceId = readDeviceIdFromLL(scope);
            if (!(newDeviceId == null || newDeviceId.matches("^0*$"))) {
                deviceId = newDeviceId;
                saveDeviceIdToSP(newDeviceId, sharedPref);
            }
            if (TextUtils.isEmpty(deviceId) && isWifiOnly(this.mContext)) {
                Rlog.d(LOG_TAG, "Current is wifi-only version, return SN number as DeviceId");
                deviceId = Build.SERIAL;
            }
            return deviceId;
        }
        Rlog.e(LOG_TAG, "getUniqueDeviceId can't read phone state.");
        return null;
    }

    private boolean isReadPhoneNumberBlocked() {
        try {
            return ProxyController.getInstance().getPhoneSubInfoController().isReadPhoneNumberBlocked();
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isReadPhoneNumberBlocked, exception=");
            stringBuilder.append(e);
            Rlog.d(str, stringBuilder.toString());
            return false;
        }
    }

    private void saveDeviceIdToSP(String deviceId, String sharedPref) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        try {
            deviceId = HwAESCryptoUtil.encrypt(HwFullNetworkConstants.MASTER_PASSWORD, deviceId);
        } catch (Exception e) {
            Rlog.d(LOG_TAG, "HwAESCryptoUtil encrypt excepiton");
        }
        editor.putString(sharedPref, deviceId);
        editor.commit();
    }

    private String getDeviceIdFromSP(String sharedPref) {
        String deviceId = PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(sharedPref, null);
        try {
            return HwAESCryptoUtil.decrypt(HwFullNetworkConstants.MASTER_PASSWORD, deviceId);
        } catch (Exception e) {
            Rlog.d(LOG_TAG, "HwAESCryptoUtil decrypt excepiton");
            return deviceId;
        }
    }

    private String readDeviceIdFromLL(int scope) {
        int phoneId = 0;
        if (HwModemCapability.isCapabilitySupport(15) && TelephonyManager.getDefault().isMultiSimEnabled() && SystemProperties.getBoolean("persist.sys.dualcards", false)) {
            if (getWaitingSwitchBalongSlot()) {
                Rlog.d(LOG_TAG, "readDeviceIdFromLL getWaitingSwitchBalongSlot");
                return null;
            }
            phoneId = HwTelephonyManagerInner.getDefault().isImeiBindSlotSupported() ? 0 : HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("readDeviceIdFromLL: phoneId=");
        stringBuilder.append(phoneId);
        Rlog.d(str, stringBuilder.toString());
        if (this.mPhones == null || this.mPhones[phoneId] == null) {
            return null;
        }
        if (1 == scope) {
            return this.mPhones[phoneId].getImei();
        }
        if (HuaweiTelephonyConfigs.isChinaTelecom()) {
            return this.mPhones[phoneId].getMeid();
        }
        str = this.mPhones[phoneId].getImei();
        if (str != null) {
            return str;
        }
        return this.mPhones[phoneId].getMeid();
    }

    private Phone getVSimPhone() {
        return HwVSimPhoneFactory.getVSimPhone();
    }

    public boolean getWaitingSwitchBalongSlot() {
        log("getWaitingSwitchBalongSlot start");
        return HwFullNetworkManager.getInstance().getWaitingSwitchBalongSlot();
    }

    public boolean setISMCOEX(String setISMCoex) {
        enforceModifyPermissionOrCarrierPrivilege();
        int phoneId = 0;
        if (TelephonyManager.getDefault().isMultiSimEnabled() && SystemProperties.getBoolean("persist.sys.dualcards", false)) {
            phoneId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        }
        if (this.mPhones == null || this.mPhones[phoneId] == null) {
            loge("mPhones is invalid!");
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setISMCoex =");
        stringBuilder.append(setISMCoex);
        log(stringBuilder.toString());
        return this.mPhones[phoneId].setISMCOEX(setISMCoex);
    }

    public boolean isDomesticCard(int slotId) {
        log("isDomesticCard start");
        enforceReadPermission();
        return true;
    }

    public boolean setWifiTxPower(int power) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setWifiTxPower: start=");
        stringBuilder.append(power);
        Rlog.d(str, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setWifiTxPower: end=");
        stringBuilder.append(power);
        Rlog.d(str, stringBuilder.toString());
        return true;
    }

    public boolean setCellTxPower(int power) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCellTxPower: start=");
        stringBuilder.append(power);
        Rlog.d(str, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        getCommandsInterface().setPowerGrade(power, null);
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setCellTxPower: end=");
        stringBuilder.append(power);
        Rlog.d(str, stringBuilder.toString());
        return true;
    }

    private CommandsInterface getCommandsInterface() {
        return PhoneFactory.getDefaultPhone().mCi;
    }

    public String[] queryServiceCellBand() {
        Rlog.d(LOG_TAG, "queryServiceCellBand");
        enforceReadPermission();
        Phone phone = PhoneFactory.getPhone(SubscriptionController.getInstance().getDefaultDataSubId());
        boolean isWait = true;
        if (ServiceState.isCdma(phone.getServiceState().getRilDataRadioTechnology())) {
            this.mServiceCellBand = new String[2];
            this.mServiceCellBand[0] = "CDMA";
            this.mServiceCellBand[1] = "0";
        } else {
            synchronized (this.mLock) {
                phone.mCi.queryServiceCellBand(this.mMainHandler.obtainMessage(6));
                while (isWait) {
                    try {
                        this.mLock.wait();
                        isWait = false;
                    } catch (InterruptedException e) {
                        log("interrupted while trying to update by index");
                    }
                }
            }
        }
        if (this.mServiceCellBand == null) {
            return new String[0];
        }
        return (String[]) this.mServiceCellBand.clone();
    }

    public void handleQueryCellBandDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            this.mServiceCellBand = null;
        } else {
            this.mServiceCellBand = (String[]) ar.result;
        }
        synchronized (this.mLock) {
            this.mLock.notifyAll();
        }
    }

    private void handleQueryEncryptFeatureDone(Message msg) {
        Rlog.d(LOG_TAG, "query encrypt call feature received");
        AsyncResult ar = msg.obj;
        HwPhone phone = ar.userObj;
        if (ar.exception != null || ar.result == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("query encrypt call feature failed ");
            stringBuilder.append(ar.exception);
            loge(stringBuilder.toString());
            if (msg.arg1 < 10) {
                this.mMainHandler.sendEmptyMessageDelayed(EVENT_QUERY_ENCRYPT_FEATURE, 1000);
            } else {
                queryCount = 0;
            }
        } else {
            byte[] res = ar.result;
            if (res.length > 0) {
                boolean z = true;
                if ((res[0] & 15) != 1) {
                    z = false;
                }
                this.mSupportEncryptCall = z;
                this.mEncryptCallStatus = res[0] >>> 4;
                if (this.mSupportEncryptCall) {
                    SystemProperties.set("persist.sys.cdma_encryption", Boolean.toString(this.mSupportEncryptCall));
                    checkEcSwitchStatusInNV(phone, this.mEncryptCallStatus);
                }
            }
        }
    }

    public boolean registerForRadioAvailable(IPhoneCallback callback) {
        log("registerForRadioAvailable");
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            this.mPhone = this.mPhones[1];
        } else {
            this.mPhone = this.mPhones[0];
        }
        if (this.mPhone == null) {
            loge("phone is null!");
            return false;
        }
        this.mRadioAvailableIndCB = callback;
        this.mPhone.getPhone().mCi.registerForAvailable(this.mMainHandler, 51, null);
        return true;
    }

    private void handleRadioAvailableInd(Message msg) {
        AsyncResult ar = msg.obj;
        if (this.mRadioAvailableIndCB == null) {
            loge("handleRadioAvailableInd mRadioAvailableIndCB is null");
            return;
        }
        if (ar.exception == null) {
            try {
                this.mRadioAvailableIndCB.onCallback1(0);
            } catch (RemoteException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleRadioAvailableInd RemoteException: ex = ");
                stringBuilder.append(ex);
                loge(stringBuilder.toString());
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("radio available ind exception: ");
            stringBuilder2.append(ar.exception);
            loge(stringBuilder2.toString());
        }
    }

    public boolean unregisterForRadioAvailable(IPhoneCallback callback) {
        log("unregisterForRadioAvailable");
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            this.mPhone = this.mPhones[1];
        } else {
            this.mPhone = this.mPhones[0];
        }
        if (this.mPhone == null) {
            loge("phone is null!");
            return false;
        }
        this.mRadioAvailableIndCB = null;
        this.mPhone.getPhone().mCi.unregisterForAvailable(this.mMainHandler);
        return true;
    }

    public boolean registerForRadioNotAvailable(IPhoneCallback callback) {
        log("registerForRadioNotAvailable");
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            this.mPhone = this.mPhones[1];
        } else {
            this.mPhone = this.mPhones[0];
        }
        if (this.mPhone == null) {
            loge("phone is null!");
            return false;
        }
        this.mRadioNotAvailableIndCB = callback;
        this.mPhone.getPhone().mCi.registerForNotAvailable(this.mMainHandler, 52, null);
        return true;
    }

    private void handleRadioNotAvailableInd(Message msg) {
        AsyncResult ar = msg.obj;
        if (this.mRadioNotAvailableIndCB == null) {
            loge("handleRadioNotAvailableInd mRadioNotAvailableIndCB is null");
            return;
        }
        if (ar.exception == null) {
            try {
                this.mRadioNotAvailableIndCB.onCallback1(0);
            } catch (RemoteException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleRadioNotAvailableInd RemoteException: ex = ");
                stringBuilder.append(ex);
                loge(stringBuilder.toString());
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("radio not available ind exception: ");
            stringBuilder2.append(ar.exception);
            loge(stringBuilder2.toString());
        }
    }

    public boolean unregisterForRadioNotAvailable(IPhoneCallback callback) {
        log("unregisterForRadioNotAvailable");
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            this.mPhone = this.mPhones[1];
        } else {
            this.mPhone = this.mPhones[0];
        }
        if (this.mPhone == null) {
            loge("phone is null!");
            return false;
        }
        this.mRadioNotAvailableIndCB = null;
        this.mPhone.getPhone().mCi.unregisterForNotAvailable(this.mMainHandler);
        return true;
    }

    public boolean registerCommonImsaToMapconInfo(IPhoneCallback callback) {
        log("registerCommonImsaToMapconInfo");
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            this.mPhone = this.mPhones[1];
        } else {
            this.mPhone = this.mPhones[0];
        }
        if (this.mPhone == null) {
            loge("phone is null!");
            return false;
        }
        this.mImsaToMapconInfoCB = callback;
        this.mPhone.getPhone().mCi.registerCommonImsaToMapconInfo(this.mMainHandler, 53, null);
        return true;
    }

    private void handleCommonImsaToMapconInfoInd(Message msg) {
        AsyncResult ar = msg.obj;
        log("handleCommonImsaToMapconInfoInd");
        if (this.mImsaToMapconInfoCB == null) {
            loge("handleCommonImsaToMapconInfoInd mImsaToMapconInfoCB is null");
            return;
        }
        if (ar.exception == null) {
            byte[] data = ar.result;
            Bundle bundle = new Bundle();
            bundle.putByteArray("imsa2mapcon_msg", data);
            try {
                this.mImsaToMapconInfoCB.onCallback3(0, 0, bundle);
            } catch (RemoteException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleCommonImsaToMapconInfoInd RemoteException: ex = ");
                stringBuilder.append(ex);
                loge(stringBuilder.toString());
            }
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("imsa to mapcon info exception: ");
            stringBuilder2.append(ar.exception);
            loge(stringBuilder2.toString());
        }
    }

    public boolean unregisterCommonImsaToMapconInfo(IPhoneCallback callback) {
        log("unregisterCommonImsaToMapconInfo");
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            this.mPhone = this.mPhones[1];
        } else {
            this.mPhone = this.mPhones[0];
        }
        if (this.mPhone == null) {
            loge("phone is null!");
            return false;
        }
        this.mImsaToMapconInfoCB = null;
        this.mPhone.getPhone().mCi.unregisterCommonImsaToMapconInfo(this.mMainHandler);
        return true;
    }

    private void handleRemoveListLocked() {
        int size = this.mRemoveRecordsList.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                removeRecord((IBinder) this.mRemoveRecordsList.get(i));
            }
            this.mRemoveRecordsList.clear();
        }
    }

    private void removeRecord(IBinder binder) {
        synchronized (this.mRecords) {
            int recordCount = this.mRecords.size();
            for (int i = 0; i < recordCount; i++) {
                if (((Record) this.mRecords.get(i)).binder == binder) {
                    this.mRecords.remove(i);
                    return;
                }
            }
        }
    }

    private void notifyPhoneEventWithCallback(int phoneId, int event, int arg, Bundle bundle) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyPhoneEventWithCallback phoneId = ");
        stringBuilder.append(phoneId);
        stringBuilder.append(" event = ");
        stringBuilder.append(event);
        log(stringBuilder.toString());
        synchronized (this.mRecords) {
            Iterator it = this.mRecords.iterator();
            while (it.hasNext()) {
                Record r = (Record) it.next();
                if (r.matchPhoneEvent(event) && r.phoneId == phoneId) {
                    try {
                        r.callback.onCallback3(event, arg, bundle);
                    } catch (RemoteException e) {
                        this.mRemoveRecordsList.add(r.binder);
                    }
                }
            }
            handleRemoveListLocked();
        }
    }

    public boolean registerForPhoneEvent(int phoneId, IPhoneCallback callback, int events) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerForPhoneEvent, phoneId = ");
        stringBuilder.append(phoneId);
        stringBuilder.append(", events = ");
        stringBuilder.append(Integer.toHexString(events));
        log(stringBuilder.toString());
        int i = 0;
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
            return false;
        } else if (this.mPhones[phoneId] == null || callback == null) {
            loge("phone or callback is null!");
            return false;
        } else {
            synchronized (this.mRecords) {
                Record r;
                IBinder b = callback.asBinder();
                int N = this.mRecords.size();
                while (i < N) {
                    r = (Record) this.mRecords.get(i);
                    if (b == r.binder) {
                        break;
                    }
                    i++;
                }
                r = new Record();
                r.binder = b;
                this.mRecords.add(r);
                log("registerForPhoneEvent: add new record");
                r.callback = callback;
                r.phoneId = phoneId;
                r.events = events;
            }
            if ((events & 1) != 0) {
                this.mPhones[phoneId].getPhone().mCi.registerForAvailable(this.mPhoneStateHandler, 1, Integer.valueOf(phoneId));
            }
            if ((events & 2) != 0) {
                this.mPhones[phoneId].getPhone().mCi.registerForNotAvailable(this.mPhoneStateHandler, 2, Integer.valueOf(phoneId));
            }
            if ((events & 4) != 0) {
                this.mPhones[phoneId].getPhone().mCi.registerCommonImsaToMapconInfo(this.mPhoneStateHandler, 4, Integer.valueOf(phoneId));
            }
            return true;
        }
    }

    public void unregisterForPhoneEvent(IPhoneCallback callback) {
        if (callback != null) {
            removeRecord(callback.asBinder());
        }
    }

    public boolean isRadioAvailable() {
        int phoneId;
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            phoneId = 1;
        } else {
            phoneId = 0;
        }
        return isRadioAvailableByPhoneId(phoneId);
    }

    public boolean isRadioAvailableByPhoneId(int phoneId) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
            return false;
        } else if (this.mPhones[phoneId] != null) {
            return this.mPhones[phoneId].isRadioAvailable();
        } else {
            loge("phone is null!");
            return false;
        }
    }

    public void setImsSwitch(boolean value) {
        int phoneId;
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            phoneId = 1;
        } else {
            phoneId = 0;
        }
        setImsSwitchByPhoneId(phoneId, value);
    }

    public void setImsSwitchByPhoneId(int phoneId, boolean value) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
        } else if (this.mPhones[phoneId] == null) {
            loge("phone is null!");
        } else {
            this.mPhones[phoneId].setImsSwitch(value);
        }
    }

    public boolean getImsSwitch() {
        int phoneId;
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            phoneId = 1;
        } else {
            phoneId = 0;
        }
        return getImsSwitchByPhoneId(phoneId);
    }

    public boolean getImsSwitchByPhoneId(int phoneId) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
            return false;
        } else if (this.mPhones[phoneId] == null) {
            loge("phone is null!");
            return false;
        } else {
            boolean result = this.mPhones[phoneId].getImsSwitch();
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsSwitch = ");
            stringBuilder.append(result);
            Rlog.d(str, stringBuilder.toString());
            return result;
        }
    }

    public void setImsDomainConfig(int domainType) {
        int phoneId;
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            phoneId = 1;
        } else {
            phoneId = 0;
        }
        setImsDomainConfigByPhoneId(phoneId, domainType);
    }

    public void setImsDomainConfigByPhoneId(int phoneId, int domainType) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
        } else if (this.mPhones[phoneId] == null) {
            loge("phone is null!");
        } else {
            this.mPhones[phoneId].setImsDomainConfig(domainType);
        }
    }

    public boolean handleMapconImsaReq(byte[] Msg) {
        int phoneId;
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            phoneId = 1;
        } else {
            phoneId = 0;
        }
        return handleMapconImsaReqByPhoneId(phoneId, Msg);
    }

    public boolean handleMapconImsaReqByPhoneId(int phoneId, byte[] Msg) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
            return false;
        } else if (this.mPhones[phoneId] == null) {
            loge("phone is null!");
            return false;
        } else {
            this.mPhones[phoneId].handleMapconImsaReq(Msg);
            return true;
        }
    }

    public int getUiccAppType() {
        int phoneId;
        if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
            phoneId = 1;
        } else {
            phoneId = 0;
        }
        return getUiccAppTypeByPhoneId(phoneId);
    }

    public int getUiccAppTypeByPhoneId(int phoneId) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
            return 0;
        } else if (this.mPhones[phoneId] != null) {
            return this.mPhones[phoneId].getUiccAppType();
        } else {
            loge("phone is null!");
            return 0;
        }
    }

    public int getImsDomain() {
        if (this.mPhone != null) {
            return getImsDomainByPhoneId(this.mPhone.getPhone().getPhoneId());
        }
        loge("phone is null!");
        return -1;
    }

    public int getImsDomainByPhoneId(int phoneId) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
            return -1;
        } else if (this.mPhones[phoneId] == null) {
            loge("phone is null!");
            return -1;
        } else if (ImsManager.isWfcEnabledByPlatform(this.mContext)) {
            return ((int[]) sendRequest(CMD_IMS_GET_DOMAIN, null, Integer.valueOf(phoneId)))[0];
        } else {
            log("vowifi not support!");
            return -1;
        }
    }

    public UiccAuthResponse handleUiccAuth(int auth_type, byte[] rand, byte[] auth) {
        if (this.mPhone != null) {
            return handleUiccAuthByPhoneId(this.mPhone.getPhone().getPhoneId(), auth_type, rand, auth);
        }
        loge("phone is null!");
        return null;
    }

    public UiccAuthResponse handleUiccAuthByPhoneId(int phoneId, int auth_type, byte[] rand, byte[] auth) {
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            loge("phoneId is invalid!");
            return null;
        } else if (this.mPhones[phoneId] != null) {
            return (UiccAuthResponse) sendRequest(CMD_UICC_AUTH, new UiccAuthPara(auth_type, rand, auth), Integer.valueOf(phoneId));
        } else {
            loge("phone is null!");
            return null;
        }
    }

    private Integer getPhoneId(Message msg) {
        Integer id = Integer.valueOf(-1);
        if (msg == null) {
            return id;
        }
        if (msg.obj != null && (msg.obj instanceof Integer)) {
            return msg.obj;
        }
        if (msg.obj == null || !(msg.obj instanceof AsyncResult)) {
            return id;
        }
        AsyncResult ar = msg.obj;
        if (ar.userObj == null || !(ar.userObj instanceof Integer)) {
            return id;
        }
        return ar.userObj;
    }

    private void initPrefNetworkTypeChecker() {
        TelephonyManager mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (mTelephonyManager != null && !mTelephonyManager.isMultiSimEnabled() && HwModemCapability.isCapabilitySupport(9)) {
            log("initPrefNetworkTypeChecker");
            if (SHOW_DIALOG_FOR_NO_SIM) {
                this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
            }
            this.mContext.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        }
    }

    private void handleCmdImsGetDomain(Message msg) {
        MainThreadRequest request = msg.obj;
        Message onCompleted = this.mMainHandler.obtainMessage(EVENT_IMS_GET_DOMAIN_DONE, request);
        if (request.subId != null) {
            this.mPhones[request.subId.intValue()].getImsDomain(onCompleted);
            return;
        }
        this.mPhone.getImsDomain(onCompleted);
    }

    private void handleCmdUiccAuth(Message msg) {
        MainThreadRequest request = msg.obj;
        UiccAuthPara para = request.argument;
        Message onCompleted = this.mMainHandler.obtainMessage(EVENT_UICC_AUTH_DONE, request);
        if (request.subId != null) {
            this.mPhones[request.subId.intValue()].handleUiccAuth(para.auth_type, para.rand, para.auth, onCompleted);
            return;
        }
        this.mPhone.handleUiccAuth(para.auth_type, para.rand, para.auth, onCompleted);
    }

    private void handleGetPrefNetworkTypeDone(Message msg) {
        Rlog.d(LOG_TAG, "handleGetPrefNetworkTypeDone");
        AsyncResult ar = msg.obj;
        if (ar.exception == null) {
            int prefNetworkType = ((int[]) ar.result)[0];
            int currentprefNetworkTypeInDB = getCurrentNetworkTypeFromDB();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("prefNetworkType:");
            stringBuilder.append(prefNetworkType);
            stringBuilder.append(" currentprefNetworkTypeInDB:");
            stringBuilder.append(currentprefNetworkTypeInDB);
            log(stringBuilder.toString());
            if (prefNetworkType == currentprefNetworkTypeInDB) {
                return;
            }
            if (currentprefNetworkTypeInDB == -1 || this.mPhone == null || this.mPhone.getPhone() == null) {
                log("INVALID_NETWORK_MODE in DB,set 4G-Switch on");
                setLteServiceAbility(1);
                return;
            }
            this.mPhone.setPreferredNetworkType(currentprefNetworkTypeInDB, this.mMainHandler.obtainMessage(5, currentprefNetworkTypeInDB, this.mPhone.getPhone().getSubId()));
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setPreferredNetworkType -> currentprefNetworkTypeInDB:");
            stringBuilder2.append(currentprefNetworkTypeInDB);
            log(stringBuilder2.toString());
            return;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("getPreferredNetworkType exception=");
        stringBuilder3.append(ar.exception);
        log(stringBuilder3.toString());
    }

    private void setSingleCardPrefNetwork(int slotId) {
        if (isValidSlotId(slotId)) {
            int prefNetwork;
            int ability = getLteServiceAbility();
            if (isCDMASimCard(slotId)) {
                prefNetwork = 1 == ability ? 10 : 7;
            } else {
                prefNetwork = 1 == ability ? 20 : 18;
            }
            this.mPhone.setPreferredNetworkType(prefNetwork, this.mMainHandler.obtainMessage(13, slotId, prefNetwork));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setSingleCardPrefNetwork, LTE ability = ");
            stringBuilder.append(ability);
            stringBuilder.append(", pref network = ");
            stringBuilder.append(prefNetwork);
            log(stringBuilder.toString());
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("invalid slotId ");
        stringBuilder2.append(slotId);
        loge(stringBuilder2.toString());
    }

    private boolean isCDMASimCard(int slotId) {
        HwTelephonyManagerInner hwTelephonyManager = HwTelephonyManagerInner.getDefault();
        return hwTelephonyManager != null && hwTelephonyManager.isCDMASimCard(slotId);
    }

    private void handleSetPrefNetworkTypeDone(Message msg) {
        AsyncResult ar = null;
        if (msg.obj != null && (msg.obj instanceof AsyncResult)) {
            ar = msg.obj;
        }
        int slot = msg.arg1;
        int setPrefMode = msg.arg2;
        if (ar != null && ar.exception == null) {
            if (getCurrentNetworkTypeFromDB() != setPrefMode) {
                saveNetworkTypeToDB(setPrefMode);
            }
            this.retryCount = 0;
            log("handleSetPrefNetworkTypeDone, success.");
        } else if (this.retryCount < 20) {
            this.retryCount++;
            this.mMainHandler.sendMessageDelayed(this.mMainHandler.obtainMessage(14, slot, setPrefMode), 3000);
        } else {
            this.retryCount = 0;
            loge("handleSetPrefNetworkTypeDone faild.");
        }
    }

    private static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService("connectivity");
        return (cm == null || cm.isNetworkSupported(0)) ? false : true;
    }

    public boolean registerForWirelessState(int type, int slotId, IPhoneCallback callback) {
        enforceReadPermission();
        boolean isSuccess = handleWirelessStateRequest(true, type, slotId, callback);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerForWirelessState type=");
        stringBuilder.append(type);
        stringBuilder.append(",isSuccess=");
        stringBuilder.append(isSuccess);
        logForSar(stringBuilder.toString());
        return isSuccess;
    }

    public boolean unregisterForWirelessState(int type, int slotId, IPhoneCallback callback) {
        enforceReadPermission();
        boolean isSuccess = handleWirelessStateRequest(true, type, slotId, callback);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterForWirelessState type=");
        stringBuilder.append(type);
        stringBuilder.append(",isSuccess=");
        stringBuilder.append(isSuccess);
        logForSar(stringBuilder.toString());
        return isSuccess;
    }

    public boolean setMaxTxPower(int type, int power) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMaxTxPower: start=");
        stringBuilder.append(power);
        Rlog.d(str, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        if (type == 2) {
            getCommandsInterface().setWifiTxPowerGrade(power, null);
        } else if (type == 1) {
            if (HwModemCapability.isCapabilitySupport(9)) {
                getCommandsInterface().setPowerGrade(power, null);
            } else {
                if (getCommandsInterface(0) != null) {
                    getCommandsInterface(0).setPowerGrade(power, null);
                }
                if (getCommandsInterface(1) != null) {
                    getCommandsInterface(1).setPowerGrade(power, null);
                }
            }
        }
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setMaxTxPower: end=");
        stringBuilder2.append(power);
        Rlog.d(str2, stringBuilder2.toString());
        return false;
    }

    private boolean handleWirelessStateRequest(int opertype, int type, int slotId, IPhoneCallback callback) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("In handleWirelessStateRequest service type=");
        stringBuilder.append(type);
        stringBuilder.append(",slotId=");
        stringBuilder.append(slotId);
        logForSar(stringBuilder.toString());
        boolean isSuccess = false;
        CommandsInterface ci = getCommandsInterface(slotId);
        if (callback == null) {
            logForSar("handleWirelessStateRequest callback is null.");
            return false;
        } else if (ci == null) {
            logForSar("handleWirelessStateRequest ci is null.");
            return false;
        } else {
            switch (opertype) {
                case 1:
                    isSuccess = registerUnitSarControl(type, slotId, ci, callback);
                    break;
                case 2:
                    isSuccess = unregisterUnitSarControl(type, slotId, ci, callback);
                    break;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleWirelessStateRequest type=");
            stringBuilder2.append(type);
            stringBuilder2.append(",isSuccess=");
            stringBuilder2.append(isSuccess);
            logForSar(stringBuilder2.toString());
            return isSuccess;
        }
    }

    private boolean isValidSlotId(int slotId) {
        return slotId >= 0 && slotId < SIM_NUM;
    }

    private void handleSarInfoUploaded(int type, Message msg) {
        log("in handleSarInfoUploaded.");
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            loge("handleSarInfoUploaded error, ar exception.");
            return;
        }
        int slotId = ((Integer) ar.userObj).intValue();
        CommandsInterface ci = getCommandsInterface(slotId);
        if (ci == null) {
            loge("handleSarInfoUploaded ci is null.");
            return;
        }
        if (!handleSarDataFromModem(type, ar)) {
            log("handleSarInfoUploaded hasClient is false,so to close the switch of upload Sar Info in modem");
            closeSarInfoUploadSwitch(type, slotId, ci);
            unregisterSarRegistrant(type, slotId, ci);
        }
    }

    private boolean handleSarDataFromModem(int type, AsyncResult ar) {
        log("handleSarDataFromModem start");
        boolean hasClient = false;
        int slotId = ((Integer) ar.userObj).intValue();
        Bundle bundle = getBundleData(ar);
        Object[] callbackArray = getCallbackArray(type);
        if (callbackArray == null) {
            loge("handleSarDataFromModem callbackArray is null.");
            return false;
        }
        synchronized (callbackArray[slotId]) {
            ArrayList<Record> callbackList = callbackArray[slotId];
            for (int i = callbackList.size() - 1; i >= 0; i--) {
                Record r = (Record) callbackList.get(i);
                try {
                    r.callback.onCallback3(type, slotId, bundle);
                    hasClient = true;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleSarDataFromModem oncallback r=");
                    stringBuilder.append(r);
                    log(stringBuilder.toString());
                } catch (RemoteException e) {
                    log("handleSarDataFromModem callback false,ex is RemoteException");
                    callbackList.remove(r);
                } catch (Exception e2) {
                    log("handleSarDataFromModem callback false,ex is Exception");
                    callbackList.remove(r);
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleSarDataFromModem record size=");
            stringBuilder2.append(callbackList.size());
            log(stringBuilder2.toString());
        }
        return hasClient;
    }

    private boolean closeSarInfoUploadSwitch(int type, int slotId, CommandsInterface ci) {
        boolean isSuccess = false;
        if (type != 4) {
            switch (type) {
                case 1:
                    ci.closeSwitchOfUploadBandClass(this.mMainHandler.obtainMessage(10, Integer.valueOf(slotId)));
                    isSuccess = true;
                    break;
                case 2:
                    ci.closeSwitchOfUploadAntOrMaxTxPower(2);
                    isSuccess = true;
                    break;
            }
        }
        ci.closeSwitchOfUploadAntOrMaxTxPower(4);
        isSuccess = true;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("closeSarInfoUploadSwitch mPhones[");
        stringBuilder.append(slotId);
        stringBuilder.append("]: type = ");
        stringBuilder.append(type);
        stringBuilder.append(",isSuccess = ");
        stringBuilder.append(isSuccess);
        log(stringBuilder.toString());
        return isSuccess;
    }

    private Bundle getBundleData(AsyncResult ar) {
        log("getBundleData start");
        if (ar == null) {
            Rlog.d(LOG_TAG, "getBundleData: ar is null");
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap((byte[]) ar.result, 0, 4);
        buf.order(ByteOrder.nativeOrder());
        int result = buf.getInt();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY1, result);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getBundleData result = ");
        stringBuilder.append(result);
        log(stringBuilder.toString());
        return bundle;
    }

    private CommandsInterface getCommandsInterface(int slotId) {
        if (isValidSlotId(slotId)) {
            return PhoneFactory.getPhones()[slotId].mCi;
        }
        log("getCommandsInterface the slotId is invalid");
        return null;
    }

    private Object[] getCallbackArray(int type) {
        Object[] callbackArray = null;
        if (type != 4) {
            switch (type) {
                case 1:
                    callbackArray = this.mRegBandClassCallbackArray;
                    break;
                case 2:
                    callbackArray = this.mRegAntStateCallbackArray;
                    break;
            }
        }
        callbackArray = this.mRegMaxTxPowerCallbackArray;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCallbackArray type=");
        stringBuilder.append(type);
        logForSar(stringBuilder.toString());
        return callbackArray;
    }

    private boolean unregisterUnitSarControl(int type, int slotId, CommandsInterface ci, IPhoneCallback callback) {
        boolean hasFind = false;
        Object[] callbackArray = getCallbackArray(type);
        if (callbackArray == null) {
            logForSar("unregisterUnitSarControl callbackArray is null.");
            return false;
        }
        boolean isSuccess;
        synchronized (callbackArray[slotId]) {
            ArrayList<Record> callbackList = callbackArray[slotId];
            IBinder b = callback.asBinder();
            int recordCount = callbackList.size();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("callbackArray[");
            stringBuilder.append(slotId);
            stringBuilder.append("] lenght is ");
            stringBuilder.append(recordCount);
            logForSar(stringBuilder.toString());
            for (int i = recordCount - 1; i >= 0; i--) {
                if (((Record) callbackList.get(i)).binder == b) {
                    Record r = (Record) callbackList.get(i);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("unregisterUnitSarControl remove: ");
                    stringBuilder2.append(r);
                    logForSar(stringBuilder2.toString());
                    callbackList.remove(i);
                    hasFind = true;
                    break;
                }
            }
            if (hasFind) {
                recordCount = callbackList.size();
                stringBuilder = new StringBuilder();
                stringBuilder.append("unregisterUnitSarControl record size = ");
                stringBuilder.append(recordCount);
                logForSar(stringBuilder.toString());
                if (recordCount == 0 && closeSarInfoUploadSwitch(type, slotId, ci)) {
                    isSuccess = unregisterSarRegistrant(type, slotId, ci);
                } else {
                    isSuccess = true;
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unregisterUnitSarControl not find the callback,type=");
                stringBuilder.append(type);
                logForSar(stringBuilder.toString());
                isSuccess = true;
            }
        }
        return isSuccess;
    }

    private boolean registerUnitSarControl(int type, int slotId, CommandsInterface ci, IPhoneCallback callback) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerUnitSarControl start slotId=");
        stringBuilder.append(slotId);
        stringBuilder.append(",type=");
        stringBuilder.append(type);
        logForSar(stringBuilder.toString());
        boolean hasFind = false;
        Object[] callbackArray = getCallbackArray(type);
        int i = 0;
        if (callbackArray == null) {
            logForSar("registerUnitSarControl callbackArray is null.");
            return false;
        } else if (registerSarRegistrant(type, slotId, ci)) {
            synchronized (callbackArray[slotId]) {
                ArrayList<Record> callbackList = callbackArray[slotId];
                IBinder b = callback.asBinder();
                int N = callbackList.size();
                while (i < N) {
                    if (b == ((Record) callbackList.get(i)).binder) {
                        hasFind = true;
                        break;
                    }
                    i++;
                }
                if (!hasFind) {
                    Record r = new Record();
                    r.binder = b;
                    r.callback = callback;
                    callbackList.add(r);
                    logForSar("registerUnitSarControl: add new record");
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("registerUnitSarControl record size=");
                stringBuilder2.append(callbackList.size());
                logForSar(stringBuilder2.toString());
            }
            return openSarInfoUploadSwitch(type, slotId, ci);
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("registerUnitSarControl mPhones[");
            stringBuilder3.append(slotId);
            stringBuilder3.append("] register return false");
            logForSar(stringBuilder3.toString());
            return false;
        }
    }

    private boolean openSarInfoUploadSwitch(int type, int slotId, CommandsInterface ci) {
        boolean isSuccess = false;
        if (type != 4) {
            switch (type) {
                case 1:
                    ci.openSwitchOfUploadBandClass(this.mMainHandler.obtainMessage(10, Integer.valueOf(slotId)));
                    isSuccess = true;
                    break;
                case 2:
                    isSuccess = ci.openSwitchOfUploadAntOrMaxTxPower(2);
                    break;
            }
        }
        isSuccess = ci.openSwitchOfUploadAntOrMaxTxPower(4);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("openSarInfoUploadSwitch mPhones[");
        stringBuilder.append(slotId);
        stringBuilder.append("]: type = ");
        stringBuilder.append(type);
        stringBuilder.append(",isSuccess = ");
        stringBuilder.append(isSuccess);
        logForSar(stringBuilder.toString());
        return isSuccess;
    }

    private boolean registerSarRegistrant(int type, int slotId, CommandsInterface ci) {
        boolean isSuccess = false;
        Message message = null;
        if (type != 4) {
            switch (type) {
                case 1:
                    message = this.mMainHandler.obtainMessage(10, Integer.valueOf(slotId));
                    break;
                case 2:
                    message = this.mMainHandler.obtainMessage(11, Integer.valueOf(slotId));
                    break;
            }
        }
        message = this.mMainHandler.obtainMessage(12, Integer.valueOf(slotId));
        if (message != null && ci.registerSarRegistrant(type, message)) {
            isSuccess = true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerSarRegistrant mPhones[");
        stringBuilder.append(slotId);
        stringBuilder.append("]: type = ");
        stringBuilder.append(type);
        stringBuilder.append(",isSuccess = ");
        stringBuilder.append(!isSuccess);
        loge(stringBuilder.toString());
        return isSuccess;
    }

    private boolean unregisterSarRegistrant(int type, int slotId, CommandsInterface ci) {
        boolean isSuccess = false;
        Message message = null;
        if (type != 4) {
            switch (type) {
                case 1:
                    message = this.mMainHandler.obtainMessage(10, Integer.valueOf(slotId));
                    break;
                case 2:
                    message = this.mMainHandler.obtainMessage(11, Integer.valueOf(slotId));
                    break;
            }
        }
        message = this.mMainHandler.obtainMessage(12, Integer.valueOf(slotId));
        if (message != null && ci.unregisterSarRegistrant(type, message)) {
            isSuccess = true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterSarRegistrant mPhones[");
        stringBuilder.append(slotId);
        stringBuilder.append("]: type = ");
        stringBuilder.append(type);
        stringBuilder.append(",isSuccess = ");
        stringBuilder.append(!isSuccess);
        logForSar(stringBuilder.toString());
        return isSuccess;
    }

    public boolean cmdForECInfo(int event, int action, byte[] buf) {
        boolean z = false;
        try {
            enforceReadPermission();
            boolean res = false;
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                boolean res2 = false;
                for (HwPhone phone : this.mPhones) {
                    if (phone != null && phone.isCDMAPhone()) {
                        boolean z2;
                        if (!phone.cmdForECInfo(event, action, buf)) {
                            if (!requestForECInfo(phone, event, action, buf)) {
                                z2 = false;
                                res2 = z2;
                            }
                        }
                        z2 = true;
                        res2 = z2;
                    }
                }
                res = res2;
            } else if (this.mPhone != null && this.mPhone.isCDMAPhone()) {
                if (this.mPhone.cmdForECInfo(event, action, buf) || requestForECInfo(this.mPhone, event, action, buf)) {
                    z = true;
                }
                res = z;
            }
            return res;
        } catch (Exception ex) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cmdForECInfo fail:");
            stringBuilder.append(ex);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }

    private void registerForRadioOnInner() {
        Rlog.d(LOG_TAG, "registerForRadioOnInner");
        if (this.mPhone == null) {
            Rlog.e(LOG_TAG, "registerForRadioOnInner failed, phone is null!");
        } else {
            this.mPhone.getPhone().mCi.registerForOn(this.mMainHandler, EVENT_QUERY_ENCRYPT_FEATURE, null);
        }
    }

    private void handleEventQueryEncryptCall(HwPhone phone) {
        if (phone != null && phone.isCDMAPhone()) {
            byte[] req = new byte[]{(byte) null};
            if (HuaweiTelephonyConfigs.isQcomPlatform()) {
                this.mSupportEncryptCall = phone.cmdForECInfo(7, 0, req);
                if (this.mSupportEncryptCall) {
                    SystemProperties.set("persist.sys.cdma_encryption", Boolean.toString(this.mSupportEncryptCall));
                }
                CommandsInterface ci = getCommandsInterface(phone.getPhone().getPhoneId());
                if (ci != null) {
                    this.mEncryptCallStatus = ((HwQualcommRIL) ci).getEcCdmaCallVersion();
                    checkEcSwitchStatusInNV(phone, this.mEncryptCallStatus);
                    return;
                }
                loge("qcomRil is null");
                return;
            }
            Message msg = this.mMainHandler.obtainMessage(EVENT_QUERY_ENCRYPT_FEATURE_DONE, phone);
            msg.arg1 = queryCount;
            phone.requestForECInfo(msg, 7, req);
            queryCount++;
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("query EncryptCall Count : ");
            stringBuilder.append(queryCount);
            Rlog.d(str, stringBuilder.toString());
        }
    }

    private boolean requestForECInfo(HwPhone phone, int event, int action, byte[] buf) {
        EncryptCallPara para;
        if (event != 3) {
            switch (event) {
                case 5:
                case 6:
                case 7:
                    break;
                default:
                    para = new EncryptCallPara(phone, event, buf);
                    break;
            }
        }
        para = new EncryptCallPara(phone, event, null);
        byte[] res = (byte[]) sendRequest(HwFullNetworkConstants.MESSAGE_PENDING_DELAY, para);
        int len = res.length;
        boolean z = true;
        if (len == 1) {
            if (res[0] <= (byte) 0 || ((byte) (res[0] & 15)) != (byte) 1) {
                z = false;
            }
            return z;
        } else if (buf == null || 1 >= len || len > buf.length) {
            return false;
        } else {
            System.arraycopy(res, 0, buf, 0, len);
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requestForECInfo res length:");
            stringBuilder.append(len);
            Rlog.d(str, stringBuilder.toString());
            return true;
        }
    }

    public boolean isCtSimCard(int slotId) {
        enforceReadPermission();
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        if (slotId < 0 || slotId >= numPhones) {
            return false;
        }
        return this.mPhones[slotId].isCtSimCard();
    }

    public void notifyCModemStatus(int status, IPhoneCallback callback) {
        if (Binder.getCallingUid() == 1000) {
            Message msg = this.mMainHandler.obtainMessage(EVENT_NOTIFY_CMODEM_STATUS);
            msg.obj = callback;
            try {
                getCommandsInterface().notifyCModemStatus(status, msg);
            } catch (RuntimeException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notifyCModemStatus got e = ");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
                e.printStackTrace();
                if (callback != null) {
                    try {
                        callback.onCallback1(-1);
                    } catch (RemoteException ex) {
                        String str = LOG_TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("notifyCModemStatus onCallback1 RemoteException:");
                        stringBuilder2.append(ex);
                        Rlog.e(str, stringBuilder2.toString());
                    }
                }
            }
        }
    }

    public boolean notifyDeviceState(String device, String state, String extra) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "notifyDeviceState");
        if (TextUtils.isEmpty(device) || TextUtils.isEmpty(state)) {
            return false;
        }
        try {
            getCommandsInterface().notifyDeviceState(device, state, extra, this.mMainHandler.obtainMessage(EVENT_NOTIFY_DEVICE_STATE));
            return true;
        } catch (RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyDeviceState got e = ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return false;
        }
    }

    private void checkEcSwitchStatusInNV(HwPhone phone, int statusInNV) {
        if (phone != null && phone.isCDMAPhone()) {
            int statusInDB = Secure.getInt(this.mContext.getContentResolver(), "encrypt_version", 0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkEcSwitchStatus, encryptCall statusInNV=");
            stringBuilder.append(statusInNV);
            stringBuilder.append(" statusInDB=");
            stringBuilder.append(statusInDB);
            log(stringBuilder.toString());
            if (statusInNV != statusInDB) {
                int action = statusInDB;
                byte[] buf = new byte[]{(byte) statusInDB};
                if (!HuaweiTelephonyConfigs.isQcomPlatform()) {
                    phone.requestForECInfo(this.mMainHandler.obtainMessage(EVENT_QUERY_ENCRYPT_FEATURE_DONE, phone), 8, buf);
                } else if (phone.cmdForECInfo(8, action, buf)) {
                    log("qcom reset NV success.");
                } else {
                    loge("qcom reset NV fail!");
                }
            }
        }
    }

    public void notifyCellularCommParaReady(int paratype, int pathtype, Message response) {
        if (Binder.getCallingUid() != 1000) {
            Rlog.e(LOG_TAG, "getCallingUid() != Process.SYSTEM_UID, return");
            return;
        }
        Rlog.d(LOG_TAG, "notifyCellularCommParaReady");
        if (1 == paratype) {
            if (TelephonyManager.getDefault().isMultiSimEnabled() && getDefault4GSlotId() == 1) {
                this.mPhone = this.mPhones[1];
            } else {
                this.mPhone = this.mPhones[0];
            }
            if (this.mPhone == null) {
                Rlog.e(LOG_TAG, "phone is null!");
                return;
            }
            this.mPhone.getPhone().notifyCellularCommParaReady(paratype, pathtype, this.mMainHandler.obtainMessage(EVENT_BASIC_COMM_PARA_UPGRADE_DONE));
        }
        if (2 == paratype) {
            if (this.mPhones[0] == null) {
                Rlog.e(LOG_TAG, "phone is null!");
                return;
            }
            this.mPhones[0].getPhone().notifyCellularCommParaReady(paratype, pathtype, this.mMainHandler.obtainMessage(EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE));
            if (HwVSimManager.getDefault().isVSimEnabled()) {
                Rlog.d(LOG_TAG, "isVSimEnabled is true");
                if (this.mPhones[2] == null) {
                    Rlog.e(LOG_TAG, "phone[2] is null!");
                    return;
                }
                this.mPhones[2].getPhone().notifyCellularCommParaReady(paratype, pathtype, this.mMainHandler.obtainMessage(EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE));
            } else if (!TelephonyManager.getDefault().isMultiSimEnabled()) {
                Rlog.d(LOG_TAG, "MultiSim is disable, mPhones[0] already processed above");
            } else if (this.mPhones[1] == null) {
                Rlog.e(LOG_TAG, "phone[1] is null!");
            } else {
                this.mPhones[1].getPhone().notifyCellularCommParaReady(paratype, pathtype, this.mMainHandler.obtainMessage(EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE));
            }
        }
    }

    public boolean setPinLockEnabled(boolean enablePinLock, String password, int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "setPinLockEnabled");
        if (!isValidStatus(enablePinLock, subId)) {
            return false;
        }
        this.setResultForPinLock = false;
        synchronized (this.mSetOPinLock) {
            this.phone.getIccCard().setIccLockEnabled(enablePinLock, password, this.mMainHandler.obtainMessage(EVENT_ENABLE_ICC_PIN_COMPLETE));
            boolean isWait = true;
            while (isWait) {
                try {
                    this.mSetOPinLock.wait();
                    isWait = false;
                } catch (InterruptedException e) {
                    log("interrupted while trying to update by index");
                }
            }
        }
        return this.setResultForPinLock;
    }

    public boolean changeSimPinCode(String oldPinCode, String newPinCode, int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", "changeSimPinCode");
        if (!isValidStatus(subId)) {
            return false;
        }
        this.setResultForChangePin = false;
        synchronized (this.mSetOPinLock) {
            this.phone.getIccCard().changeIccLockPassword(oldPinCode, newPinCode, this.mMainHandler.obtainMessage(EVENT_CHANGE_ICC_PIN_COMPLETE));
            boolean isWait = true;
            while (isWait) {
                try {
                    this.mSetOPinLock.wait();
                    isWait = false;
                } catch (InterruptedException e) {
                    log("interrupted while trying to update by index");
                }
            }
        }
        return this.setResultForChangePin;
    }

    private boolean isValidStatus(int subId) {
        return isValidStatus(false, subId);
    }

    private boolean isValidStatus(boolean enablePinLock, int subId) {
        if (!isValidSlotId(subId)) {
            return false;
        }
        this.phone = PhoneFactory.getPhone(subId);
        if (this.phone == null) {
            return false;
        }
        int airplaneMode = Global.getInt(this.phone.getContext().getContentResolver(), "airplane_mode_on", 0);
        StringBuilder stringBuilder;
        if (1 == airplaneMode) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("airplaneMode : ");
            stringBuilder.append(airplaneMode);
            log(stringBuilder.toString());
            return false;
        }
        State mExternalState = this.phone.getIccCard().getState();
        if (mExternalState == State.PIN_REQUIRED || mExternalState == State.PUK_REQUIRED) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Need to unlock pin first! mExternalState : ");
            stringBuilder.append(mExternalState);
            log(stringBuilder.toString());
            return false;
        } else if (SubscriptionController.getInstance() == null) {
            return false;
        } else {
            if (!(SubscriptionController.getInstance().getSubState(subId) == 1)) {
                return false;
            }
            boolean pinState = this.phone.getIccCard().getIccLockEnabled();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("pinState = ");
            stringBuilder2.append(pinState);
            log(stringBuilder2.toString());
            if (pinState && enablePinLock) {
                log("already in PIN_REQUIRED");
                return false;
            } else if (pinState || enablePinLock) {
                return true;
            } else {
                log("not in PIN_REQUIRED");
                return false;
            }
        }
    }

    private void registerMDMSmsReceiver() {
        IntentFilter filter = new IntentFilter("com.huawei.devicepolicy.action.POLICY_CHANGED");
        if (this.mContext != null) {
            this.mContext.registerReceiver(this.mMDMSmsReceiver, filter);
        }
    }

    private void clearSinglePolicyData(Context context, String timeMode, boolean isOutgoing) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("clearSinglePolicyData: ");
        stringBuilder.append(timeMode);
        log(stringBuilder.toString());
        if (HwTelephonyFactory.getHwInnerSmsManager().isLimitNumOfSmsEnabled(isOutgoing)) {
            if (!TextUtils.isEmpty(timeMode)) {
                String policyName = isOutgoing ? OUTGOING_SMS_LIMIT : INCOMING_SMS_LIMIT;
                Object obj = -1;
                int hashCode = timeMode.hashCode();
                if (hashCode != -2105529586) {
                    if (hashCode != -1628741630) {
                        if (hashCode == 1931104358 && timeMode.equals(DAY_MODE)) {
                            obj = null;
                        }
                    } else if (timeMode.equals(MONTH_MODE)) {
                        obj = 2;
                    }
                } else if (timeMode.equals(WEEK_MODE)) {
                    obj = 1;
                }
                switch (obj) {
                    case null:
                        removeSharedDayModeData(context, policyName, USED_OF_DAY, DAY_MODE_TIME);
                        break;
                    case 1:
                        removeSharedDayModeData(context, policyName, USED_OF_WEEK, WEEK_MODE_TIME);
                        break;
                    case 2:
                        removeSharedDayModeData(context, policyName, USED_OF_MONTH, MONTH_MODE_TIME);
                        break;
                }
            }
            return;
        }
        clearAllPolicyData(context, isOutgoing);
    }

    private void removeSharedDayModeData(Context context, String policyName, String keyUsedNum, String keyTime) {
        if (!TextUtils.isEmpty(policyName) && context != null) {
            Editor editor = context.getSharedPreferences(policyName, null).edit();
            editor.remove(keyUsedNum);
            editor.remove(keyTime);
            editor.commit();
        }
    }

    private void clearAllPolicyData(Context context) {
        clearAllPolicyData(context, true);
        clearAllPolicyData(context, false);
    }

    private void clearAllPolicyData(Context context, boolean isOutgoing) {
        if (!HwTelephonyFactory.getHwInnerSmsManager().isLimitNumOfSmsEnabled(isOutgoing) && context != null) {
            Editor editor = context.getSharedPreferences(isOutgoing ? OUTGOING_SMS_LIMIT : INCOMING_SMS_LIMIT, null).edit();
            editor.clear();
            editor.commit();
        }
    }

    private void registerSetRadioCapDoneReceiver() {
        if (IS_DUAL_IMS_SUPPORTED && !HuaweiTelephonyConfigs.isQcomPlatform()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.huawei.action.ACTION_HW_SWITCH_SLOT_DONE");
            if (this.mContext != null) {
                this.mContext.registerReceiver(this.mSetRadioCapDoneReceiver, filter);
            }
        }
    }

    private void handleSwitchSlotDone(Intent intent) {
        int switchSlotStep = intent.getIntExtra("HW_SWITCH_SLOT_STEP", -99);
        if (!HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && 1 == switchSlotStep) {
            if (isDualImsSwitchOpened()) {
                log("handleSwitchSlotDone. dual ims switch open, return.");
            } else if (1 == intent.getIntExtra(HwFullNetworkConstants.IF_NEED_SET_RADIO_CAP, 0)) {
                log("handleSwitchSlotDone. not happen real sim slot change, return.");
            } else {
                int networkTypeForSub1 = Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode0", -1);
                int networkTypeForSub2 = Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode1", -1);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleSwitchSlotDone: sub1 is ");
                stringBuilder.append(networkTypeForSub1);
                stringBuilder.append(", sub2 is ");
                stringBuilder.append(networkTypeForSub2);
                log(stringBuilder.toString());
                if (-1 != networkTypeForSub1 && -1 != networkTypeForSub2) {
                    if (getDefault4GSlotId() == 0) {
                        if (HwNetworkTypeUtils.isLteServiceOn(networkTypeForSub1)) {
                            networkTypeForSub1 = 3;
                            log("handleSwitchSlotDone: sub2 is slave sim card, shouldn't have LTE ability.");
                        }
                    } else if (HwNetworkTypeUtils.isLteServiceOn(networkTypeForSub2)) {
                        networkTypeForSub2 = 3;
                        log("handleSwitchSlotDone: sub1 is slave sim card, shouldn't have LTE ability.");
                    }
                    saveNetworkTypeToDB(0, networkTypeForSub2);
                    saveNetworkTypeToDB(1, networkTypeForSub1);
                }
            }
        }
    }

    public boolean sendPseudocellCellInfo(int type, int lac, int cid, int radioTech, String plmn, int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        CommandsInterface ci = getCommandsInterface(subId);
        if (ci == null) {
            loge("sendPseudocellCellInfo  ci is null.");
            return false;
        }
        ci.sendPseudocellCellInfo(type, lac, cid, radioTech, plmn, null);
        return true;
    }

    private Phone getPhone(int subId) {
        SubscriptionController sc = SubscriptionController.getInstance();
        if (sc != null) {
            return PhoneFactory.getPhone(sc.getPhoneId(subId));
        }
        return null;
    }

    public void setImsRegistrationStateForSubId(int subId, boolean registered) {
        enforceModifyPermissionOrCarrierPrivilege();
        Phone phone = getPhone(subId);
        if (phone != null) {
            phone.setImsRegistrationState(registered);
        }
    }

    public boolean isImsRegisteredForSubId(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.isImsRegistered();
        }
        return false;
    }

    public boolean isWifiCallingAvailableForSubId(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.isWifiCallingEnabled();
        }
        return false;
    }

    public boolean isVolteAvailableForSubId(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.isVolteEnabled();
        }
        return false;
    }

    public boolean isVideoTelephonyAvailableForSubId(int subId) {
        Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.isVideoEnabled();
        }
        return false;
    }

    public boolean isDeactivatingSlaveData() {
        if (this.mInCallDataStateMachine == null) {
            return false;
        }
        return this.mInCallDataStateMachine.isDeactivatingSlaveData();
    }

    public boolean isSlaveActive() {
        if (this.mInCallDataStateMachine == null) {
            return false;
        }
        return this.mInCallDataStateMachine.isSlaveActive();
    }

    public boolean isSwitchingToSlave() {
        if (this.mInCallDataStateMachine == null) {
            return false;
        }
        return this.mInCallDataStateMachine.isSwitchingToSlave();
    }

    public void registerImsCallStates(boolean enable, int phoneId) {
        if (this.mInCallDataStateMachine != null) {
            this.mInCallDataStateMachine.registerImsCallStates(enable, phoneId);
        }
    }

    public boolean sendLaaCmd(int cmd, String reserved, Message response) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        CommandsInterface ci = getCommandsInterface(getDefault4GSlotId());
        this.sendLaaCmdCompleteMsg = response;
        if (ci == null) {
            loge("sendLaaCmd  ci is null.");
            sendResponseToTarget(response, -1);
            return false;
        }
        if (reserved == null) {
            reserved = "";
        }
        ci.sendLaaCmd(cmd, reserved, this.mMainHandler.obtainMessage(EVENT_SEND_LAA_CMD_DONE));
        return true;
    }

    private void handleSendLaaCmdDone(Message msg) {
        AsyncResult ar = msg.obj;
        int result = -1;
        if (ar != null && ar.exception == null) {
            result = 0;
        }
        sendResponseToTarget(this.sendLaaCmdCompleteMsg, result);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleSendLaaCmdDone:sendLaaCmd result is ");
        stringBuilder.append(result);
        log(stringBuilder.toString());
    }

    public boolean getLaaDetailedState(String reserved, Message response) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        CommandsInterface ci = getCommandsInterface(getDefault4GSlotId());
        this.getLaaStateCompleteMsg = response;
        if (ci == null) {
            loge("getLaaDetailedState  ci is null.");
            sendResponseToTarget(response, -1);
            return false;
        }
        if (reserved == null) {
            reserved = "";
        }
        ci.getLaaDetailedState(reserved, this.mMainHandler.obtainMessage(EVENT_GET_LAA_STATE_DONE));
        return true;
    }

    private void handleGetLaaStateDone(Message msg) {
        AsyncResult ar = msg.obj;
        int result = -1;
        if (ar != null && ar.exception == null && (ar.result instanceof int[])) {
            result = ((int[]) ar.result)[0];
        }
        sendResponseToTarget(this.getLaaStateCompleteMsg, result);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleGetLaaStateDone getLaaDetailedState result is ");
        stringBuilder.append(result);
        log(stringBuilder.toString());
    }

    private void handleGetCallforwardDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar != null) {
            Message cbMsg = ar.userObj;
            if (!(cbMsg == null || cbMsg.replyTo == null)) {
                Bundle data = new Bundle();
                int i = 0;
                if (ar.exception != null) {
                    data.putBoolean(CALLBACK_RESULT, false);
                    data.putString(CALLBACK_EXCEPTION, ar.exception.toString());
                } else {
                    data.putBoolean(CALLBACK_RESULT, true);
                }
                if (ar.result != null) {
                    CallForwardInfo[] cfiArray = ar.result;
                    if (cfiArray != null && cfiArray.length > 0) {
                        int length = cfiArray.length;
                        ArrayList<CallForwardInfo> cfiList = new ArrayList();
                        while (i < length) {
                            cfiList.add(cfiArray[i]);
                            i++;
                        }
                        data.putParcelableArrayList(CALLBACK_CF_INFO, cfiList);
                    }
                }
                cbMsg.setData(data);
                try {
                    cbMsg.replyTo.send(cbMsg);
                } catch (RemoteException ex) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_GET_CALLFORWARDING_DONE RemoteException:");
                    stringBuilder.append(ex);
                    Rlog.e(str, stringBuilder.toString());
                }
            }
        }
    }

    private void handleGetNumRecBaseStattionDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar == null) {
            Rlog.e(LOG_TAG, "handleGetNumRecBaseStattionDone ar null");
            return;
        }
        Message cbMsg = ar.userObj;
        if (cbMsg == null || cbMsg.replyTo == null) {
            log("handleGetNumRecBaseStattionDone  cbMsg is null or cbMsg is null");
        } else {
            Bundle data = new Bundle();
            data.putBoolean(CALLBACK_RESULT, true);
            StringBuilder stringBuilder;
            if (ar.exception == null) {
                log("handleGetNumRecBaseStattionDone succ result is 0");
                data.putInt(CALLBACK_AFBS_INFO, 0);
            } else if (ar.exception instanceof CommandException) {
                data.putInt(CALLBACK_AFBS_INFO, ((CommandException) ar.exception).getCommandError().ordinal());
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleGetNumRecBaseStattionDone succ ex result is ");
                stringBuilder.append(((CommandException) ar.exception).getCommandError().ordinal());
                log(stringBuilder.toString());
            } else {
                data.putInt(CALLBACK_AFBS_INFO, -1);
                stringBuilder = new StringBuilder();
                stringBuilder.append("handleGetNumRecBaseStattionDone succ ex result is null ");
                stringBuilder.append(ar.exception);
                log(stringBuilder.toString());
            }
            cbMsg.setData(data);
            try {
                cbMsg.replyTo.send(cbMsg);
            } catch (RemoteException ex) {
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("EVENT_GET_NUMRECBASESTATION_DONE RemoteException:");
                stringBuilder2.append(ex);
                Rlog.e(str, stringBuilder2.toString());
            }
        }
    }

    private void handleSetFunctionDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar != null) {
            Message cbMsg = ar.userObj;
            if (!(cbMsg == null || cbMsg.replyTo == null)) {
                Bundle data = new Bundle();
                if (ar.exception != null) {
                    data.putBoolean(CALLBACK_RESULT, false);
                    data.putString(CALLBACK_EXCEPTION, ar.exception.toString());
                } else {
                    data.putBoolean(CALLBACK_RESULT, true);
                }
                cbMsg.setData(data);
                try {
                    cbMsg.replyTo.send(cbMsg);
                } catch (RemoteException ex) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_SET_FUNCTION_DONE RemoteException:");
                    stringBuilder.append(ex);
                    Rlog.e(str, stringBuilder.toString());
                }
            }
        }
    }

    public boolean isCspPlmnEnabled(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCspPlmnEnabled for subId ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        Phone phone = getPhone(subId);
        if (phone != null) {
            return phone.isCspPlmnEnabled();
        }
        return false;
    }

    public void setCallForwardingOption(int subId, int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCallForwardingOption for subId ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        Phone phone = getPhone(subId);
        Message msg;
        if (phone != null) {
            msg = this.mMainHandler.obtainMessage(EVENT_SET_CALLFORWARDING_DONE);
            msg.obj = response;
            phone.setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, timerSeconds, msg);
        } else if (response != null) {
            try {
                if (response.replyTo != null) {
                    msg = Message.obtain(response);
                    Bundle data = new Bundle();
                    data.putBoolean(CALLBACK_RESULT, false);
                    msg.setData(data);
                    response.replyTo.send(msg);
                }
            } catch (RemoteException ex) {
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setCallForwardingOption RemoteException:");
                stringBuilder2.append(ex);
                Rlog.e(str, stringBuilder2.toString());
            }
        }
    }

    public void getCallForwardingOption(int subId, int commandInterfaceCFReason, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCallForwardingOption for subId ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        Phone phone = getPhone(subId);
        Message msg;
        if (phone != null) {
            msg = this.mMainHandler.obtainMessage(EVENT_GET_CALLFORWARDING_DONE);
            msg.obj = response;
            phone.getCallForwardingOption(commandInterfaceCFReason, msg);
        } else if (response != null) {
            try {
                if (response.replyTo != null) {
                    msg = Message.obtain(response);
                    Bundle data = new Bundle();
                    data.putBoolean(CALLBACK_RESULT, false);
                    msg.setData(data);
                    response.replyTo.send(msg);
                }
            } catch (RemoteException ex) {
                String str = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getCallForwardingOption RemoteException:");
                stringBuilder2.append(ex);
                Rlog.e(str, stringBuilder2.toString());
            }
        }
    }

    public boolean setSubscription(int subId, boolean activate, Message response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSubscription for subId: ");
        stringBuilder.append(subId);
        stringBuilder.append(", activate: ");
        stringBuilder.append(activate);
        log(stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        Message msg = this.mMainHandler.obtainMessage(201);
        msg.obj = response;
        if (HwSubscriptionManager.getInstance() != null) {
            return HwSubscriptionManager.getInstance().setSubscription(subId, activate, msg);
        }
        return false;
    }

    public String getImsImpu(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getImsImpu for subId ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        if (HwCustUtil.isVZW) {
            PackageManager pm = this.mContext.getPackageManager();
            String callingPackage = null;
            if (pm != null) {
                callingPackage = pm.getPackagesForUid(Binder.getCallingUid())[0];
            }
            if (!checkReadPhoneNumber(callingPackage, "getImsImpu")) {
                return null;
            }
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        String impu = null;
        Phone phone = getPhone(subId);
        if (phone != null) {
            ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
            if (imsPhone != null) {
                impu = imsPhone.getImsImpu();
            }
        }
        return impu;
    }

    public String getLine1NumberFromImpu(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLine1NumberFromImpu for subId ");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        String impu = getImsImpu(subId);
        if (TextUtils.isEmpty(impu)) {
            return null;
        }
        impu = impu.trim();
        if (impu.matches(PATTERN_TEL)) {
            return impu.substring(impu.indexOf(":") + 1);
        }
        if (!impu.matches(PATTERN_SIP)) {
            return null;
        }
        String number = impu.substring(impu.indexOf(":") + 1, impu.indexOf("@"));
        String str = (TextUtils.isEmpty(number) || getPhone(subId) == null || number.equals(getPhone(subId).getSubscriberId())) ? null : number;
        return str;
    }

    private boolean checkReadPhoneNumber(String callingPackage, String message) {
        if (this.mAppOps.noteOp(15, Binder.getCallingUid(), callingPackage) == 0) {
            return true;
        }
        try {
            return checkReadPhoneState(callingPackage, message);
        } catch (SecurityException e) {
            boolean z = false;
            AppOpsManager appOpsManager;
            int opCode;
            try {
                this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SMS", message);
                appOpsManager = this.mAppOps;
                opCode = AppOpsManager.permissionToOpCode("android.permission.READ_SMS");
                if (opCode == -1) {
                    return true;
                }
                if (this.mAppOps.noteOp(opCode, Binder.getCallingUid(), callingPackage) == 0) {
                    z = true;
                }
                return z;
            } catch (SecurityException e2) {
                try {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_NUMBERS", message);
                    appOpsManager = this.mAppOps;
                    opCode = AppOpsManager.permissionToOpCode("android.permission.READ_PHONE_NUMBERS");
                    if (opCode == -1) {
                        return true;
                    }
                    if (this.mAppOps.noteOp(opCode, Binder.getCallingUid(), callingPackage) == 0) {
                        z = true;
                    }
                    return z;
                } catch (SecurityException e3) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(message);
                    stringBuilder.append(": Neither user ");
                    stringBuilder.append(Binder.getCallingUid());
                    stringBuilder.append(" nor current process has ");
                    stringBuilder.append("android.permission.READ_PHONE_STATE");
                    stringBuilder.append(", ");
                    stringBuilder.append("android.permission.READ_SMS");
                    stringBuilder.append(", or ");
                    stringBuilder.append("android.permission.READ_PHONE_STATE");
                    stringBuilder.append(".");
                    throw new SecurityException(stringBuilder.toString());
                }
            }
        }
    }

    private boolean checkReadPhoneState(String callingPackage, String message) {
        boolean z = true;
        try {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PRIVILEGED_PHONE_STATE", message);
            return true;
        } catch (SecurityException e) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", message);
            if (this.mAppOps.noteOp(51, Binder.getCallingUid(), callingPackage) != 0) {
                z = false;
            }
            return z;
        }
    }

    private void sendResponseToTarget(Message response, int result) {
        if (response != null && response.getTarget() != null) {
            response.arg1 = result;
            response.sendToTarget();
        }
    }

    public void registerForCallAltSrv(int subId, IPhoneCallback callback) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
        Phone phone = PhoneFactory.getPhone(subId);
        if (phone == null || callback == null) {
            log("registerForCallAltSrv:phone or callback is null,return");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerForCallAltSrv for subId=");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        phone.registerForCallAltSrv(phone, EVENT_CELLULAR_CLOUD_PARA_UPGRADE_DONE, callback);
    }

    public void unregisterForCallAltSrv(int subId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_PHONE_STATE", null);
        Phone phone = PhoneFactory.getPhone(subId);
        if (phone == null) {
            log("unregisterForCallAltSrv:phone or callback is null,return");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregisterForCallAltSrv for subId=");
        stringBuilder.append(subId);
        log(stringBuilder.toString());
        phone.unregisterForCallAltSrv(phone);
    }

    public int invokeOemRilRequestRaw(int phoneId, byte[] oemReq, byte[] oemResp) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        if (phoneId < 0 || phoneId >= this.mPhones.length) {
            logForOemHook("phoneId is invalid!");
            return -1;
        } else if (this.mPhones[phoneId] == null) {
            logForOemHook("phone is null!");
            return -1;
        } else if (oemReq == null || oemResp == null) {
            logForOemHook("oemReq or oemResp is null!");
            return -1;
        } else {
            int returnValue = 0;
            try {
                AsyncResult result = (AsyncResult) sendRequest(CMD_INVOKE_OEM_RIL_REQUEST_RAW, oemReq, Integer.valueOf(phoneId));
                if (result.exception != null) {
                    returnValue = result.exception.getCommandError().ordinal();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("invokeOemRilRequestRaw fail exception + returnValue:");
                    stringBuilder.append(returnValue);
                    logForOemHook(stringBuilder.toString());
                    if (returnValue > 0) {
                        returnValue *= -1;
                    }
                } else if (result.result != null) {
                    byte[] responseData = result.result;
                    if (responseData.length > oemResp.length) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Buffer to copy response too small: Response length is ");
                        stringBuilder2.append(responseData.length);
                        stringBuilder2.append("bytes. Buffer Size is ");
                        stringBuilder2.append(oemResp.length);
                        stringBuilder2.append("bytes.");
                        loge(stringBuilder2.toString());
                    }
                    System.arraycopy(responseData, 0, oemResp, 0, responseData.length);
                    returnValue = responseData.length;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("invokeOemRilRequestRaw success, returnValue");
                    stringBuilder3.append(returnValue);
                    logForOemHook(stringBuilder3.toString());
                } else {
                    logForOemHook("invokeOemRilRequestRaw fail result.result is null");
                }
            } catch (RuntimeException e) {
                logForOemHook("invokeOemRilRequestRaw: Runtime Exception");
                returnValue = Error.GENERIC_FAILURE.ordinal();
                if (returnValue > 0) {
                    returnValue *= -1;
                }
            }
            return returnValue;
        }
    }

    private void handleCmdOemRilRequestRaw(Message msg) {
        MainThreadRequest request = msg.obj;
        Message onCompleted = this.mMainHandler.obtainMessage(EVENT_INVOKE_OEM_RIL_REQUEST_RAW_DONE, request);
        if (request.subId != null) {
            int phoneId = request.subId.intValue();
            Phone phone = this.mPhones[phoneId].getPhone();
            if (phone != null) {
                phone.invokeOemRilRequestRaw((byte[]) request.argument, onCompleted);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invokeOemRilRequestRaw success by phoneId=");
                stringBuilder.append(phoneId);
                logForOemHook(stringBuilder.toString());
                return;
            }
        }
        Phone phone2 = this.mPhone.getPhone();
        if (phone2 != null) {
            phone2.invokeOemRilRequestRaw((byte[]) request.argument, onCompleted);
            logForOemHook("invokeOemRilRequestRaw with default phone");
        }
    }

    private void handleCmdOemRilRequestRawDone(Message msg) {
        AsyncResult ar = msg.obj;
        MainThreadRequest request = ar.userObj;
        request.result = ar;
        synchronized (request) {
            request.notifyAll();
        }
    }

    public boolean isSecondaryCardGsmOnly() {
        enforceReadPermission();
        TelephonyManager mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        boolean z = false;
        if (mTelephonyManager != null && !mTelephonyManager.isMultiSimEnabled()) {
            return false;
        }
        if (HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && HwFullNetworkManager.getInstance().isCMCCHybird()) {
            z = true;
        }
        return z;
    }

    public boolean bindSimToProfile(int slotId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.BIND_DEVICE_ADMIN", null);
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        if (slotId >= 0 && slotId <= numPhones) {
            return Global.putInt(this.mContext.getContentResolver(), "afw_work_slotid", slotId);
        }
        log("bind sim fail");
        return false;
    }

    public boolean setLine1Number(int subId, String alphaTag, String number, Message onComplete) {
        enforceModifyPermissionOrCarrierPrivilege();
        Phone phone = PhoneFactory.getPhone(subId);
        if (phone == null) {
            return false;
        }
        Message msg = this.mMainHandler.obtainMessage(202);
        msg.obj = onComplete;
        return phone.setLine1Number(alphaTag, number, msg);
    }

    public boolean setDeepNoDisturbState(int slotId, int state) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        return ((Boolean) sendRequest(203, Integer.valueOf(state), Integer.valueOf(slotId))).booleanValue();
    }

    private void handleCmdSetDeepNoDisturb(Message msg) {
        MainThreadRequest request = msg.obj;
        if (request != null) {
            if (request.subId == null || request.argument == null) {
                log("handleCmdSetDeepNoDisturb request null");
                request.result = Boolean.valueOf(false);
                synchronized (request) {
                    request.notifyAll();
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleCmdSetDeepNoDisturb subId=");
            stringBuilder.append(request.subId);
            stringBuilder.append(" arg=");
            stringBuilder.append(request.argument);
            log(stringBuilder.toString());
            CommandsInterface ci = getCommandsInterface(request.subId.intValue());
            if (ci == null) {
                log("handleCmdSetDeepNoDisturb ci null");
                request.result = Boolean.valueOf(false);
                synchronized (request) {
                    request.notifyAll();
                }
                return;
            }
            ci.setDeepNoDisturbState(((Integer) request.argument).intValue(), this.mMainHandler.obtainMessage(204, request));
        }
    }

    private void handleSetDeepNoDisturbDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar != null) {
            MainThreadRequest request = ar.userObj;
            if (request != null) {
                if (ar.exception == null) {
                    request.result = Boolean.valueOf(true);
                } else {
                    request.result = Boolean.valueOf(false);
                }
                log("handleSetDeepNoDisturbDone notifyAll");
                synchronized (request) {
                    request.notifyAll();
                }
            }
        }
    }

    public void informModemTetherStatusToChangeGRO(int enable, String faceName) {
        try {
            getCommandsInterface().informModemTetherStatusToChangeGRO(enable, faceName, this.mMainHandler.obtainMessage(120));
        } catch (RuntimeException e) {
            loge("informModemTetherStatusToChangeGRO got RuntimeException");
        }
    }

    public boolean sendSimMatchedOperatorInfo(int slotId, String opKey, String opName, int state, String reserveField) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        if (isValidSlotId(slotId)) {
            CommandsInterface ci = getCommandsInterface(slotId);
            if (ci == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sendSimMatchedOperatorInfo: ci is null, slotId = ");
                stringBuilder.append(slotId);
                loge(stringBuilder.toString());
                return false;
            }
            ci.sendSimMatchedOperatorInfo(opKey, opName, state, reserveField, null);
            return true;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("sendSimMatchedOperatorInfo: slotId = ");
        stringBuilder2.append(slotId);
        stringBuilder2.append(" is invalid.");
        loge(stringBuilder2.toString());
        return false;
    }

    public boolean is4RMimoEnabled(int subId) {
        Phone phone = PhoneFactory.getPhone(subId);
        if (!(phone == null || phone.getServiceStateTracker() == null)) {
            HwGsmServiceStateManager hwGsmSSM = HwServiceStateManager.getHwGsmServiceStateManager(phone.getServiceStateTracker(), (GsmCdmaPhone) phone);
            if (hwGsmSSM != null) {
                return hwGsmSSM.is4RMimoEnabled();
            }
        }
        return false;
    }

    public boolean getAntiFakeBaseStation(Message response) {
        log("getAntiFakeBaseStation ");
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        if (getCommandsInterface() != null) {
            Message msg = this.mMainHandler.obtainMessage(EVENT_GET_NUMRECBASESTATION_DONE);
            msg.obj = response;
            return getCommandsInterface().getAntiFakeBaseStation(msg);
        }
        loge("getAntiFakeBaseStation phone null");
        return false;
    }

    public boolean registerForAntiFakeBaseStation(IPhoneCallback callback) {
        log("registerForAntiFakeBaseStation");
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < numPhones; i++) {
            if (this.mPhones[i] != null) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("registerForAntiFakeBaseStation HwPhone sub = ");
                stringBuilder.append(i);
                Rlog.d(str, stringBuilder.toString());
                this.mPhones[i].getPhone().mCi.registerForAntiFakeBaseStation(this.mMainHandler, EVENT_ANTIFAKE_BASESTATION_CHANGED, null);
            }
        }
        this.mAntiFakeBaseStationCB = callback;
        return true;
    }

    private void handleAntiFakeBaseStation(Message msg) {
        AsyncResult ar = msg.obj;
        if (this.mAntiFakeBaseStationCB == null) {
            loge("handleAntiFakeBaseStation mAntiFakeBaseStationCB is null");
            return;
        }
        if (ar == null || ar.exception != null || ar.result == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleAntiFakeBaseStation exception: ar ");
            stringBuilder.append(ar);
            loge(stringBuilder.toString());
        } else {
            int parm = ((Integer) ar.result).intValue();
            Intent intent = new Intent("com.huawei.action.ACTION_HW_ANTIFAKE_BASESTATION");
            intent.setPackage("com.huawei.systemmanager");
            intent.putExtra(CALLBACK_AFBS_INFO, parm);
            this.mContext.sendBroadcast(intent);
            try {
                this.mAntiFakeBaseStationCB.onCallback1(parm);
            } catch (RemoteException ex) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleAntiFakeBaseStation RemoteException: ex = ");
                stringBuilder2.append(ex);
                loge(stringBuilder2.toString());
            }
            log("handleAntiFakeBaseStation send");
        }
    }

    public boolean unregisterForAntiFakeBaseStation() {
        log("unregisterForAntiFakeBaseStation");
        this.mContext.enforceCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE", null);
        int numPhones = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < numPhones; i++) {
            if (this.mPhones[i] != null) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unregisterForAntiFakeBaseStation HwPhone sub = ");
                stringBuilder.append(i);
                Rlog.d(str, stringBuilder.toString());
                this.mPhones[i].getPhone().mCi.unregisterForAntiFakeBaseStation(this.mMainHandler);
            }
        }
        this.mAntiFakeBaseStationCB = null;
        return true;
    }

    private void handleGetCardTrayInfoDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar == null || ar.result == null || ar.exception != null) {
            this.mCardTrayInfo = null;
            loge("handleGetCardTrayInfoDone, exception occurs");
        } else if (ar.result instanceof byte[]) {
            this.mCardTrayInfo = (byte[]) ar.result;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleGetCardTrayInfoDone, mCardTrayInfo:");
            stringBuilder.append(IccUtils.bytesToHexString(this.mCardTrayInfo));
            log(stringBuilder.toString());
        }
        synchronized (this.mCardTrayLock) {
            this.mCardTrayLock.notifyAll();
        }
    }

    public byte[] getCardTrayInfo() {
        Rlog.d(LOG_TAG, "getCardTrayInfo");
        enforceReadPermission();
        synchronized (this.mCardTrayLock) {
            getCommandsInterface().getCardTrayInfo(this.mMainHandler.obtainMessage(EVENT_GET_CARD_TRAY_INFO_DONE));
            boolean isWait = true;
            while (isWait) {
                try {
                    this.mCardTrayLock.wait();
                    isWait = false;
                } catch (InterruptedException e) {
                    Rlog.e(LOG_TAG, "Interrupted Exception in getCardTrayInfo");
                }
            }
        }
        if (this.mCardTrayInfo == null) {
            return new byte[0];
        }
        return (byte[]) this.mCardTrayInfo.clone();
    }
}
