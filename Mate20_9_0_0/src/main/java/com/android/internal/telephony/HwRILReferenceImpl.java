package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.radio.V1_0.CdmaSmsMessage;
import android.hardware.radio.V1_0.CdmaSmsSubaddress;
import android.hardware.radio.V1_0.RadioResponseInfo;
import android.hardware.radio.V1_0.SetupDataCallResult;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.data.DataCallResponse;
import android.text.TextUtils;
import com.android.ims.HwImsManagerInner;
import com.android.internal.telephony.AbstractRIL.HwRILReference;
import com.android.internal.telephony.HwCallManagerReference.HWBuffer;
import com.android.internal.telephony.fullnetwork.HwFullNetworkChipCommon;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.uicc.IccUtils;
import huawei.cust.HwCustUtils;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vendor.huawei.hardware.radio.V2_0.IRadio;
import vendor.huawei.hardware.radio.deprecated.V1_0.IOemHook;

public class HwRILReferenceImpl extends Handler implements HwRILReference {
    private static final String ACTION_HW_EXIST_NETWORK_INFO_ROAMING_PLUS = "android.intent.action.HW_EXIST_NETWORK_INFO";
    private static final String ACTION_IMS_SWITCH_STATE_CHANGE = "com.huawei.ACTION_IMS_SWITCH_STATE_CHANGE";
    private static final int BYTE_SIZE = 1;
    private static final String CARRIER_CONFIG_CHANGE_STATE = "carrierConfigChangeState";
    private static final int CARRIER_CONFIG_STATE_LOAD = 1;
    private static final int DEFAULT_SUBID = -1;
    private static final int DEFAULT_SUB_ID_RESET = -1;
    private static final int EVENT_GET_IMS_SWITCH_RESULT = 1;
    private static final int EVENT_SET_IMS_SWITCH_RESULT = 2;
    private static final int EVENT_UNSOL_SIM_NVCFG_FINISHED = 3;
    private static final boolean FEATURE_HW_VOLTE_ON = SystemProperties.getBoolean("ro.config.hw_volte_on", false);
    private static final boolean FEATURE_HW_VOWIFI_ON = SystemProperties.getBoolean("ro.vendor.config.hw_vowifi", false);
    private static final boolean FEATURE_SHOW_VOLTE_SWITCH = SystemProperties.getBoolean("ro.config.hw_volte_show_switch", true);
    private static final boolean FEATURE_VOLTE_DYN = SystemProperties.getBoolean("ro.config.hw_volte_dyn", false);
    private static final int HW_ANTENNA_STATE_TYPE = 2;
    private static final int HW_BAND_CLASS_TYPE = 1;
    private static final int HW_MAX_TX_POWER_TYPE = 4;
    private static final String HW_VOLTE_USER_SWITCH = "hw_volte_user_switch";
    private static final String[] HW_VOLTE_USER_SWITCH_DUALIMS = new String[]{"hw_volte_user_switch_0", "hw_volte_user_switch_1"};
    private static final int HW_VOLTE_USER_SWITCH_OFF = 0;
    private static final int HW_VOLTE_USER_SWITCH_ON = 1;
    private static final String IMS_SERVICE_STATE_CHANGED_ACTION = "huawei.intent.action.IMS_SERVICE_STATE_CHANGED";
    private static final String IMS_STATE = "state";
    private static final String IMS_STATE_CHANGE_SUBID = "subId";
    private static final String IMS_STATE_REGISTERED = "REGISTERED";
    private static final int INT_SIZE = 4;
    private static final int NVCFG_RESULT_FINISHED = 1;
    private static final boolean RILJ_LOGD = true;
    private static final boolean RILJ_LOGV = true;
    private static final String RILJ_LOG_TAG = "RILJ-HwRILReferenceImpl";
    private static final int RIL_MAX_COMMAND_BYTES = 8192;
    private static String mMcc = null;
    private Context existNetworkContext;
    protected Registrant mAntStateRegistrant;
    private final BroadcastReceiver mCarrierConfigListener = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent != null && HwRILReferenceImpl.this.mHwRilReferenceInstanceId != null) {
                String action = intent.getAction();
                HwRILReferenceImpl hwRILReferenceImpl = HwRILReferenceImpl.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("receive event: action=");
                stringBuilder.append(action);
                stringBuilder.append(", mHwRilReferenceInstanceId=");
                stringBuilder.append(HwRILReferenceImpl.this.mHwRilReferenceInstanceId);
                hwRILReferenceImpl.riljLog(stringBuilder.toString());
                int subId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                if (HwImsManagerInner.isDualImsAvailable() || HwRILReferenceImpl.this.mHwRilReferenceInstanceId.intValue() == subId) {
                    boolean z;
                    int curSubForCarrier;
                    HwRILReferenceImpl hwRILReferenceImpl2;
                    if ("android.telephony.action.CARRIER_CONFIG_CHANGED".equals(action)) {
                        z = false;
                        curSubForCarrier = intent.getIntExtra("subscription", 0);
                        if ((HwImsManagerInner.isDualImsAvailable() || subId == curSubForCarrier) && curSubForCarrier == HwRILReferenceImpl.this.mHwRilReferenceInstanceId.intValue()) {
                            hwRILReferenceImpl2 = HwRILReferenceImpl.this;
                            if (intent.getIntExtra(HwRILReferenceImpl.CARRIER_CONFIG_CHANGE_STATE, 1) == 1) {
                                z = true;
                            }
                            hwRILReferenceImpl2.mIsCarrierConfigLoaded = z;
                            HwRILReferenceImpl.this.riljLog("handle event: CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.");
                            if (HwRILReferenceImpl.this.mRil == null || !HwRILReferenceImpl.this.mIsCarrierConfigLoaded) {
                                HwRILReferenceImpl.this.riljLog("mRil is null or carrier config is cleared.");
                            } else {
                                HwRILReferenceImpl.this.handleUnsolicitedRadioStateChanged(HwRILReferenceImpl.this.mRil.getRadioState().isOn(), HwRILReferenceImpl.this.mContext);
                            }
                        } else {
                            HwRILReferenceImpl.this.riljLog("getDefault4GSlotId do not match subId from intent.");
                            return;
                        }
                    } else if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                        if (!(HwImsManagerInner.isDualImsAvailable() || HwRILReferenceImpl.this.mRil == null)) {
                            HwRILReferenceImpl.this.handleUnsolicitedRadioStateChanged(HwRILReferenceImpl.this.mRil.getRadioState().isOn(), HwRILReferenceImpl.this.mContext);
                        }
                    } else if (HwRILReferenceImpl.IMS_SERVICE_STATE_CHANGED_ACTION.equals(action)) {
                        curSubForCarrier = intent.getIntExtra("subId", -1);
                        HwRILReferenceImpl hwRILReferenceImpl3 = HwRILReferenceImpl.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("IMS_SERVICE_STATE_CHANGED_ACTION curSubId is : ");
                        stringBuilder2.append(curSubForCarrier);
                        hwRILReferenceImpl3.riljLog(stringBuilder2.toString());
                        if (curSubForCarrier == HwRILReferenceImpl.this.mHwRilReferenceInstanceId.intValue()) {
                            HwRILReferenceImpl.this.mIsImsRegistered = HwRILReferenceImpl.IMS_STATE_REGISTERED.equals(intent.getStringExtra(HwRILReferenceImpl.IMS_STATE));
                            z = HwRILReferenceImpl.FEATURE_HW_VOLTE_ON;
                            if (HwRILReferenceImpl.FEATURE_VOLTE_DYN && HwRILReferenceImpl.this.mIsCarrierConfigLoaded) {
                                z = HwImsManagerInner.isVolteEnabledByPlatform(context, HwRILReferenceImpl.this.mHwRilReferenceInstanceId.intValue());
                            } else if (HwRILReferenceImpl.FEATURE_VOLTE_DYN && !HwRILReferenceImpl.this.mIsCarrierConfigLoaded) {
                                return;
                            }
                            hwRILReferenceImpl2 = HwRILReferenceImpl.this;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("mIsImsRegistered is : ");
                            stringBuilder3.append(HwRILReferenceImpl.this.mIsImsRegistered);
                            stringBuilder3.append(" and isSupportVolte is : ");
                            stringBuilder3.append(z);
                            hwRILReferenceImpl2.riljLog(stringBuilder3.toString());
                            if ((!HwRILReferenceImpl.FEATURE_VOLTE_DYN || HwRILReferenceImpl.this.mIsCarrierConfigLoaded) && HwRILReferenceImpl.this.mIsImsRegistered && !z) {
                                HwRILReferenceImpl.this.handleImsSwitch(HwRILReferenceImpl.this.mIsImsRegistered);
                            }
                        }
                    } else if ("com.huawei.ACTION_NETWORK_FACTORY_RESET".equals(action)) {
                        HwRILReferenceImpl.this.riljLog("receive action of reset ims");
                        if (intent.getIntExtra("subId", -1) == HwRILReferenceImpl.this.mHwRilReferenceInstanceId.intValue()) {
                            HwRILReferenceImpl.this.riljLog("reset ims state");
                            HwRILReferenceImpl.this.handleUnsolicitedRadioStateChanged(HwRILReferenceImpl.this.mRil.getRadioState().isOn(), HwRILReferenceImpl.this.mContext);
                        }
                    }
                    return;
                }
                HwRILReferenceImpl hwRILReferenceImpl4 = HwRILReferenceImpl.this;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("getDefault4GSlotId do not match mHwRilReferenceInstanceId=");
                stringBuilder4.append(HwRILReferenceImpl.this.mHwRilReferenceInstanceId);
                hwRILReferenceImpl4.riljLog(stringBuilder4.toString());
            }
        }
    };
    private Context mContext;
    protected Registrant mCurBandClassRegistrant;
    private HwCommonRadioIndication mHwCommonRadioIndication;
    private volatile IRadio mHwCommonRadioProxy = null;
    private HwCommonRadioResponse mHwCommonRadioResponse;
    private HwCustRILReference mHwCustRILReference;
    private Integer mHwRilReferenceInstanceId;
    protected RegistrantList mIccUimLockRegistrants = new RegistrantList();
    private boolean mIsCarrierConfigLoaded = false;
    private boolean mIsImsRegistered = false;
    protected Registrant mMaxTxPowerRegistrant;
    private HwOemHookIndication mOemHookIndication;
    private volatile IOemHook mOemHookProxy = null;
    private HwOemHookResponse mOemHookResponse;
    private WorkSource mRILDefaultWorkSource;
    private RIL mRil;
    private String mcc_operator = null;
    private boolean shouldReportRoamingPlusInfo = true;

    private interface RILCommand {
        void excute(IRadio iRadio, int i) throws RemoteException, RuntimeException;
    }

    public HwRILReferenceImpl(RIL ril) {
        this.mRil = ril;
        if (!(this.mRil == null || this.mRil.getContext() == null || this.mRil.getContext().getApplicationInfo() == null)) {
            this.mRILDefaultWorkSource = new WorkSource(this.mRil.getContext().getApplicationInfo().uid, this.mRil.getContext().getPackageName());
        }
        this.mHwCustRILReference = (HwCustRILReference) HwCustUtils.createObj(HwCustRILReference.class, new Object[0]);
        this.mOemHookResponse = new HwOemHookResponse(this.mRil);
        this.mOemHookIndication = new HwOemHookIndication(this.mRil);
        this.mHwCommonRadioResponse = new HwCommonRadioResponse(this.mRil);
        this.mHwCommonRadioIndication = new HwCommonRadioIndication(this.mRil);
        if (FEATURE_VOLTE_DYN && !HwModemCapability.isCapabilitySupport(9)) {
            IntentFilter filter = new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED");
            filter.addAction(IMS_SERVICE_STATE_CHANGED_ACTION);
            filter.addAction("com.huawei.ACTION_NETWORK_FACTORY_RESET");
            filter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
            if (!(this.mRil == null || this.mRil.getContext() == null)) {
                riljLog("register receiver CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED.");
                this.mRil.getContext().registerReceiver(this.mCarrierConfigListener, filter);
            }
            this.mRil.registerForUnsolNvCfgFinished(this, 3, null);
        }
    }

    public void handleMessage(Message msg) {
        if (msg != null) {
            AsyncResult ar;
            switch (msg.what) {
                case 1:
                    Rlog.d(RILJ_LOG_TAG, "Event EVENT_GET_IMS_SWITCH_RESULT Received");
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception == null) {
                        boolean z = false;
                        if (1 == ar.result[0]) {
                            z = true;
                        }
                        handleImsSwitch(z);
                        break;
                    }
                    Rlog.d(RILJ_LOG_TAG, "Get IMS switch failed!");
                    break;
                case 2:
                    ar = msg.obj;
                    String str = RILJ_LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Event EVENT_SET_IMS_SWITCH_RESULT Received, AsyncResult.userObj = ");
                    stringBuilder.append(ar.userObj);
                    Rlog.d(str, stringBuilder.toString());
                    if (ar.exception != null && (ar.userObj instanceof Boolean) && ((Boolean) ar.userObj).booleanValue()) {
                        saveSwitchStatusToDB(getImsSwitch() ^ 1);
                        break;
                    }
                case 3:
                    handleUnsolSimNvCfgFinished(msg);
                    break;
                default:
                    String str2 = RILJ_LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid Message id:[");
                    stringBuilder2.append(msg.what);
                    stringBuilder2.append("]");
                    Rlog.d(str2, stringBuilder2.toString());
                    break;
            }
        }
    }

    private void saveSwitchStatusToDB(boolean on) {
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ims switch in DB: ");
        stringBuilder.append(on);
        Rlog.d(str, stringBuilder.toString());
        if (this.mContext != null) {
            try {
                System.putInt(this.mContext.getContentResolver(), HwImsManagerInner.isDualImsAvailable() ? HW_VOLTE_USER_SWITCH_DUALIMS[this.mHwRilReferenceInstanceId.intValue()] : HW_VOLTE_USER_SWITCH, on ? 1 : 0);
            } catch (NullPointerException e) {
                Rlog.e(RILJ_LOG_TAG, "saveSwitchStatusToDB NullPointerException");
            } catch (Exception e2) {
                Rlog.e(RILJ_LOG_TAG, "saveSwitchStatusToDB Exception");
            }
        }
    }

    private void handleUnsolSimNvCfgFinished(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar == null || ar.exception != null) {
            Rlog.e(RILJ_LOG_TAG, "handleUnsolSimNvCfgFinished: ar exception");
            return;
        }
        Object obj = ar.result;
        if (obj == null || !(obj instanceof Integer)) {
            Rlog.e(RILJ_LOG_TAG, "handleUnsolSimNvCfgFinished: obj is null or not number");
            return;
        }
        int result = ((Integer) obj).intValue();
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleUnsolSimNvCfgFinished: result=");
        stringBuilder.append(result);
        Rlog.d(str, stringBuilder.toString());
        boolean needSetVolteSwitchToModem = true;
        boolean singleIms = HwImsManagerInner.isDualImsAvailable() ^ true;
        if (1 != result) {
            needSetVolteSwitchToModem = false;
        }
        if (singleIms) {
            int subId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
            String str2 = RILJ_LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleUnsolSimNvCfgFinished: subId=");
            stringBuilder2.append(subId);
            stringBuilder2.append(", currentId=");
            stringBuilder2.append(this.mHwRilReferenceInstanceId);
            Rlog.d(str2, stringBuilder2.toString());
            if (subId != this.mHwRilReferenceInstanceId.intValue()) {
                needSetVolteSwitchToModem = false;
            }
        }
        if (needSetVolteSwitchToModem) {
            handleUnsolicitedRadioStateChanged(this.mRil.getRadioState().isOn(), this.mContext);
        }
    }

    private String requestToStringEx(int request) {
        return HwTelephonyBaseManagerImpl.getDefault().requestToStringEx(request);
    }

    public Object processSolicitedEx(int rilRequest, Parcel p) {
        if (rilRequest == 518) {
            return responseVoid(p);
        }
        if (rilRequest == 524) {
            return responseVoid(p);
        }
        if (rilRequest == 532) {
            return responseInts(p);
        }
        if (rilRequest == 2017) {
            return responseVoid(p);
        }
        if (rilRequest == 2121) {
            return responseVoid(p);
        }
        Rlog.d(RILJ_LOG_TAG, "The Message is not processed in HwRILReferenceImpl");
        return null;
    }

    private void riljLog(String msg) {
        String stringBuilder;
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(msg);
        if (this.mHwRilReferenceInstanceId != null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" [SUB");
            stringBuilder3.append(this.mHwRilReferenceInstanceId);
            stringBuilder3.append("]");
            stringBuilder = stringBuilder3.toString();
        } else {
            stringBuilder = "";
        }
        stringBuilder2.append(stringBuilder);
        Rlog.d(str, stringBuilder2.toString());
    }

    private void loge(String msg) {
        String stringBuilder;
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(msg);
        if (this.mHwRilReferenceInstanceId != null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" [SUB");
            stringBuilder3.append(this.mHwRilReferenceInstanceId);
            stringBuilder3.append("]");
            stringBuilder = stringBuilder3.toString();
        } else {
            stringBuilder = "";
        }
        stringBuilder2.append(stringBuilder);
        Rlog.e(str, stringBuilder2.toString());
    }

    private Object responseInts(Parcel p) {
        int numInts = p.readInt();
        int[] response = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            response[i] = p.readInt();
        }
        return response;
    }

    private Object responseVoid(Parcel p) {
        return null;
    }

    public void handleImsSwitch(boolean modemImsStatus) {
        if (this.mContext == null) {
            riljLog("handleImsSwitch, mContext is null.nothing to do");
            return;
        }
        int subId = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
        if (this.mHwRilReferenceInstanceId == null || !(HwImsManagerInner.isDualImsAvailable() || this.mHwRilReferenceInstanceId.intValue() == subId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getDefault4GSlotId do not match mHwRilReferenceInstanceId=");
            stringBuilder.append(this.mHwRilReferenceInstanceId);
            riljLog(stringBuilder.toString());
            return;
        }
        boolean apImsStatus = getImsSwitch();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("handleImsSwitch and apImsStatus is : ");
        stringBuilder2.append(apImsStatus);
        stringBuilder2.append(" and modemImsStatus is : ");
        stringBuilder2.append(modemImsStatus);
        riljLog(stringBuilder2.toString());
        if (apImsStatus != modemImsStatus) {
            setImsSwitch(apImsStatus, false);
        }
        sendBroadCastToIms(apImsStatus);
    }

    public void setImsSwitch(boolean on) {
        setImsSwitch(on, true);
    }

    private void setImsSwitch(final boolean on, boolean isSaveDB) {
        String str;
        StringBuilder stringBuilder;
        if (this.mHwRilReferenceInstanceId == null || !(HwImsManagerInner.isDualImsAvailable() || this.mHwRilReferenceInstanceId.intValue() == HwTelephonyManagerInner.getDefault().getDefault4GSlotId())) {
            riljLog("current slot not support volte");
            return;
        }
        if (this.mContext != null && isSaveDB) {
            try {
                System.putInt(this.mContext.getContentResolver(), HwImsManagerInner.isDualImsAvailable() ? HW_VOLTE_USER_SWITCH_DUALIMS[this.mHwRilReferenceInstanceId.intValue()] : HW_VOLTE_USER_SWITCH, on ? 1 : 0);
            } catch (NullPointerException e) {
                str = RILJ_LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("e = ");
                stringBuilder.append(e);
                Rlog.e(str, stringBuilder.toString());
            } catch (Exception ex) {
                str = RILJ_LOG_TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ex = ");
                stringBuilder.append(ex);
                Rlog.e(str, stringBuilder.toString());
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setImsSwitch -> imsstatte : ");
        stringBuilder2.append(on);
        riljLog(stringBuilder2.toString());
        invokeIRadio(2114, obtainMessage(2, Boolean.valueOf(isSaveDB)), new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setImsSwitch(serial, on);
            }
        });
    }

    public boolean getImsSwitch() {
        return HwImsManagerInner.isEnhanced4gLteModeSettingEnabledByUser(this.mContext, this.mHwRilReferenceInstanceId.intValue());
    }

    public void handleUnsolicitedRadioStateChanged(boolean on, Context context) {
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleUnsolicitedRadioStateChanged: state on =  ");
        stringBuilder.append(on);
        Rlog.d(str, stringBuilder.toString());
        this.mContext = context;
        if (!HwModemCapability.isCapabilitySupport(9)) {
            str = RILJ_LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("hand radio state change and volte on is ");
            stringBuilder.append(FEATURE_HW_VOLTE_ON);
            Rlog.d(str, stringBuilder.toString());
            if ((!on || !FEATURE_HW_VOLTE_ON) && !FEATURE_HW_VOWIFI_ON) {
                Rlog.d(RILJ_LOG_TAG, "not to do, radio state is off");
            } else if (!FEATURE_VOLTE_DYN || this.mIsCarrierConfigLoaded) {
                boolean isSupportVolte = HwImsManagerInner.isVolteEnabledByPlatform(context, this.mHwRilReferenceInstanceId.intValue());
                if (!FEATURE_VOLTE_DYN || isSupportVolte || (this.mIsImsRegistered && !isSupportVolte)) {
                    getModemImsSwitch(obtainMessage(1));
                }
            } else {
                Rlog.d(RILJ_LOG_TAG, "CarrierConfig is not loaded completely");
            }
        }
    }

    public void getModemImsSwitch(Message result) {
        riljLog("getModemImsSwitch");
        invokeIRadio(2115, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getImsSwitch(serial);
            }
        });
    }

    private void sendBroadCastToIms(boolean imsSwitchOn) {
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendBroadCastToIms, imsSwitchOn is: ");
        stringBuilder.append(imsSwitchOn);
        Rlog.d(str, stringBuilder.toString());
        Intent intent = new Intent();
        intent.setAction(ACTION_IMS_SWITCH_STATE_CHANGE);
        if (this.mContext != null) {
            this.mContext.sendBroadcast(intent);
        }
    }

    public void iccGetATR(Message result) {
        invokeIRadio(2032, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getSimATR(serial);
            }
        });
    }

    public void getPOLCapabilty(Message response) {
        invokeIRadio(2064, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getPolCapability(serial);
            }
        });
    }

    public void getCurrentPOLList(Message response) {
        invokeIRadio(2065, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getPolList(serial);
            }
        });
    }

    public void setPOLEntry(final int index, final String numeric, final int nAct, Message response) {
        invokeIRadio(2066, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                if (numeric == null || numeric.length() == 0) {
                    radio.setPolEntry(serial, Integer.toString(index), "", Integer.toString(nAct));
                } else {
                    radio.setPolEntry(serial, Integer.toString(index), numeric, Integer.toString(nAct));
                }
            }
        });
    }

    public void writeContent(CdmaSmsMessage msg, String pdu) {
        StringBuilder stringBuilder;
        try {
            int i;
            byte[] pduBytes = pdu.getBytes("ISO-8859-1");
            int i2 = 0;
            for (byte content : pduBytes) {
                String str = RILJ_LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("writeSmsToRuim pdu is");
                stringBuilder2.append(content);
                Rlog.e(str, stringBuilder2.toString());
            }
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pduBytes));
            msg.teleserviceId = dis.readInt();
            boolean z = true;
            msg.isServicePresent = ((byte) dis.read()) == (byte) 1;
            msg.serviceCategory = dis.readInt();
            msg.address.digitMode = dis.readInt();
            msg.address.numberMode = dis.readInt();
            msg.address.numberType = dis.readInt();
            msg.address.numberPlan = dis.readInt();
            int addrNbrOfDigits = (byte) dis.read();
            for (i = 0; i < addrNbrOfDigits; i++) {
                msg.address.digits.add(Byte.valueOf((byte) dis.read()));
            }
            msg.subAddress.subaddressType = dis.readInt();
            CdmaSmsSubaddress cdmaSmsSubaddress = msg.subAddress;
            if (((byte) dis.read()) != (byte) 1) {
                z = false;
            }
            cdmaSmsSubaddress.odd = z;
            int subaddrNbrOfDigits = (byte) dis.read();
            for (i = 0; i < subaddrNbrOfDigits; i++) {
                msg.subAddress.digits.add(Byte.valueOf((byte) dis.read()));
            }
            i = dis.readInt();
            while (i2 < i) {
                msg.bearerData.add(Byte.valueOf((byte) dis.read()));
                i2++;
            }
        } catch (UnsupportedEncodingException ex) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("writeSmsToRuim: UnsupportedEncodingException: ");
            stringBuilder.append(ex);
            riljLog(stringBuilder.toString());
        } catch (IOException ex2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("writeSmsToRuim: conversion from input stream to object failed: ");
            stringBuilder.append(ex2);
            riljLog(stringBuilder.toString());
        }
    }

    public void setShouldReportRoamingPlusInfo(boolean on) {
        if (on) {
            riljLog("shouldReportRoamingPlusInfo will be set true");
            this.shouldReportRoamingPlusInfo = true;
        }
    }

    public void handleRequestGetImsiMessage(RILRequest rr, Object ret, Context context) {
        if (rr.mRequest == 11) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("<      RIL_REQUEST_GET_IMSI xxxxx ");
            riljLog(stringBuilder.toString());
            if (ret != null && !((String) ret).equals("")) {
                String temp_mcc = ((String) ret).substring(0, 3);
                if (mMcc == null && temp_mcc != null && this.mcc_operator != null && !this.mcc_operator.equals(temp_mcc) && temp_mcc.equals(HwFullNetworkChipCommon.PREFIX_LOCAL_MCC) && this.shouldReportRoamingPlusInfo) {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_HW_EXIST_NETWORK_INFO_ROAMING_PLUS);
                    intent.putExtra("current_mcc", this.mcc_operator);
                    context.sendBroadcast(intent);
                    String str = RILJ_LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("sendBroadcast:ACTION_HW_EXIST_NETWORK_INFO_ROAMING_PLUS with extra: mcc=");
                    stringBuilder2.append(this.mcc_operator);
                    stringBuilder2.append("when handleRequestGetImsiMessage");
                    Rlog.d(str, stringBuilder2.toString());
                    this.shouldReportRoamingPlusInfo = false;
                }
                mMcc = temp_mcc;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" mMcc = ");
                stringBuilder3.append(mMcc);
                riljLog(stringBuilder3.toString());
            }
        }
    }

    private void unsljLog(int response) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[UNSL]< ");
        stringBuilder.append(unsolResponseToString(response));
        riljLog(stringBuilder.toString());
    }

    private String unsolResponseToString(int request) {
        if (request == 3001) {
            return "UNSOL_HW_RESIDENT_NETWORK_CHANGED";
        }
        if (request == 3003) {
            return "UNSOL_HW_CS_CHANNEL_INFO_IND";
        }
        if (request == 3005) {
            return "UNSOL_HW_ECCNUM";
        }
        if (request == 3031) {
            return "UNSOL_HW_XPASS_RESELECT_INFO";
        }
        if (request == 3034) {
            return "UNSOL_HW_EXIST_NETWORK_INFO";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<unknown response>:");
        stringBuilder.append(request);
        return stringBuilder.toString();
    }

    private void invokeIRadio(int requestId, Message result, RILCommand cmd) {
        IRadio radioProxy = getHuaweiCommonRadioProxy(null);
        if (radioProxy != null) {
            RILRequest rr = RILRequest.obtain(requestId, result, this.mRILDefaultWorkSource);
            this.mRil.addRequestEx(rr);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("> ");
            stringBuilder.append(RIL.requestToString(requestId));
            riljLog(stringBuilder.toString());
            try {
                cmd.excute(radioProxy, rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                this.mRil.handleRadioProxyExceptionForRREx(RIL.requestToString(requestId), e, rr);
            }
        }
    }

    public IRadio getHuaweiCommonRadioProxy(Message result) {
        if (!this.mRil.mIsMobileNetworkSupported) {
            riljLog("getRadioProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                result.sendToTarget();
            }
            return null;
        } else if (this.mHwCommonRadioProxy != null) {
            return this.mHwCommonRadioProxy;
        } else {
            try {
                this.mHwCommonRadioProxy = IRadio.getService(RIL.HIDL_SERVICE_NAME[this.mRil.mPhoneId == null ? 0 : this.mRil.mPhoneId.intValue()]);
            } catch (RemoteException | RuntimeException e) {
                StringBuilder stringBuilder;
                try {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getRadioProxy: huaweiradioProxy got 1_0 exception = ");
                    stringBuilder.append(e);
                    loge(stringBuilder.toString());
                } catch (RemoteException | RuntimeException e2) {
                    this.mHwCommonRadioProxy = null;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RadioProxy getService/setResponseFunctions: ");
                    stringBuilder.append(e2);
                    loge(stringBuilder.toString());
                }
            }
            if (this.mHwCommonRadioProxy != null) {
                this.mHwCommonRadioProxy.setResponseFunctionsHuawei(this.mHwCommonRadioResponse, this.mHwCommonRadioIndication);
            } else {
                loge("getRadioProxy: huawei radioProxy == null");
            }
            if (this.mHwCommonRadioProxy == null && result != null) {
                AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                result.sendToTarget();
            }
            return this.mHwCommonRadioProxy;
        }
    }

    public void clearHuaweiCommonRadioProxy() {
        riljLog("clearHuaweiCommonRadioProxy");
        this.mHwCommonRadioProxy = null;
    }

    public IOemHook getHwOemHookProxy(Message result) {
        if (!this.mRil.mIsMobileNetworkSupported) {
            riljLog("getHwOemHookProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                result.sendToTarget();
            }
            return null;
        } else if (this.mOemHookProxy != null) {
            return this.mOemHookProxy;
        } else {
            try {
                this.mOemHookProxy = IOemHook.getService(RIL.HIDL_SERVICE_NAME[this.mRil.mPhoneId == null ? 0 : this.mRil.mPhoneId.intValue()]);
                if (this.mOemHookProxy != null) {
                    this.mOemHookProxy.setResponseFunctions(this.mOemHookResponse, this.mOemHookIndication);
                } else {
                    loge("getHwOemHookProxy: mOemHookProxy == null");
                }
            } catch (RemoteException | RuntimeException e) {
                this.mOemHookProxy = null;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OemHookProxy getService/setResponseFunctions: ");
                stringBuilder.append(e);
                loge(stringBuilder.toString());
            }
            if (this.mOemHookProxy == null && result != null) {
                AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                result.sendToTarget();
            }
            return this.mOemHookProxy;
        }
    }

    public void clearHwOemHookProxy() {
        riljLog("clearOemHookProxy");
        this.mOemHookProxy = null;
    }

    public void setPowerGrade(final int powerGrade, Message response) {
        invokeIRadio(518, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setPowerGrade(serial, Integer.toString(powerGrade));
            }
        });
    }

    public void setWifiTxPowerGrade(final int powerGrade, Message response) {
        invokeIRadio(535, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setWifiPowerGrade(serial, Integer.toString(powerGrade));
            }
        });
    }

    public void supplyDepersonalization(final String netpin, final int type, Message result) {
        invokeIRadio(8, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                if (radio != null) {
                    radio.supplyDepersonalization(serial, HwRILReferenceImpl.this.convertNullToEmptyString(netpin), type);
                } else {
                    HwRILReferenceImpl.this.riljLog("not support below radio 2.0");
                }
            }
        });
    }

    public void registerForUimLockcard(Handler h, int what, Object obj) {
        this.mIccUimLockRegistrants.add(new Registrant(h, what, obj));
    }

    public void unregisterForUimLockcard(Handler h) {
        this.mIccUimLockRegistrants.remove(h);
    }

    public void notifyIccUimLockRegistrants() {
        if (this.mIccUimLockRegistrants != null) {
            this.mIccUimLockRegistrants.notifyRegistrants();
        }
    }

    public void sendSMSSetLong(final int flag, Message result) {
        invokeIRadio(2015, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setLongMessage(serial, flag);
            }
        });
    }

    public void dataConnectionDetach(final int mode, Message response) {
        invokeIRadio(2011, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.dataConnectionDeatch(serial, mode);
            }
        });
    }

    public void dataConnectionAttach(final int mode, Message response) {
        invokeIRadio(2012, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.dataConnectionAttach(serial, mode);
            }
        });
    }

    public void restartRild(Message result) {
        invokeIRadio(HwFullNetworkConstants.EVENT_SET_PRIMARY_STACK_LTE_SWITCH_DONE, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.restartRILD(serial);
            }
        });
    }

    public void sendResponseToTarget(Message response, int responseCode) {
        if (response != null) {
            AsyncResult.forMessage(response, null, CommandException.fromRilErrno(responseCode));
            response.sendToTarget();
        }
    }

    public void requestSetEmergencyNumbers(final String ecclist_withcard, final String ecclist_nocard) {
        riljLog("setEmergencyNumbers()");
        invokeIRadio(HwFullNetworkConstants.EVENT_RADIO_ON_PROCESS_SIM_STATE, null, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setEccNum(serial, ecclist_withcard, ecclist_nocard);
            }
        });
    }

    public void queryEmergencyNumbers() {
        riljLog("queryEmergencyNumbers()");
        invokeIRadio(522, null, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getEccNum(serial);
            }
        });
    }

    public void getCdmaGsmImsi(Message result) {
        invokeIRadio(529, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getCdmaGsmImsi(serial);
            }
        });
    }

    public void getCdmaModeSide(Message result) {
        invokeIRadio(2127, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getCdmaModeSide(serial);
            }
        });
    }

    public void setCdmaModeSide(final int modemID, Message result) {
        invokeIRadio(2118, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setCdmaModeSide(serial, modemID);
            }
        });
    }

    public void setVpMask(final int vpMask, Message result) {
        invokeIRadio(2099, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setVoicePreferStatus(serial, vpMask);
            }
        });
    }

    public void resetAllConnections() {
        invokeIRadio(2017, null, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.resetAllConnections(serial);
            }
        });
    }

    public Map<String, String> correctApnAuth(String username, int authType, String password) {
        if (this.mHwCustRILReference != null && this.mHwCustRILReference.isCustCorrectApnAuthOn()) {
            return this.mHwCustRILReference.custCorrectApnAuth(username, authType, password);
        }
        Map<String, String> map = new HashMap();
        if (authType == 1) {
            if (TextUtils.isEmpty(username)) {
                authType = 0;
                password = "";
                riljLog("authType is pap but username is null, clear all");
            }
        } else if (authType == 2) {
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                authType = 0;
                username = "";
                password = "";
                riljLog("authType is chap but username or password is null, clear all");
            }
        } else if (authType == 3) {
            if (TextUtils.isEmpty(username)) {
                authType = 0;
                password = "";
                riljLog("authType is pap_chap but username is null, clear all");
            } else if (TextUtils.isEmpty(password)) {
                authType = 1;
                riljLog("authType is pap_chap but password is null, tune authType to pap");
            }
        }
        map.put("userName", username);
        map.put("authType", String.valueOf(authType));
        map.put("password", password);
        return map;
    }

    public void setHwRILReferenceInstanceId(int instanceId) {
        this.mHwRilReferenceInstanceId = Integer.valueOf(instanceId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set HwRILReference InstanceId: ");
        stringBuilder.append(instanceId);
        riljLog(stringBuilder.toString());
    }

    public void notifyCModemStatus(final int state, Message result) {
        invokeIRadio(2121, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.notifyCModemStatus(serial, state);
            }
        });
    }

    public boolean unregisterSarRegistrant(int type, Message result) {
        Registrant removedRegistrant = null;
        riljLog("unregisterSarRegistrant start");
        if (type != 4) {
            switch (type) {
                case 1:
                    removedRegistrant = this.mCurBandClassRegistrant;
                    break;
                case 2:
                    removedRegistrant = this.mAntStateRegistrant;
                    break;
            }
        }
        removedRegistrant = this.mMaxTxPowerRegistrant;
        if (removedRegistrant == null || result == null || removedRegistrant.getHandler() != result.getTarget()) {
            return false;
        }
        removedRegistrant.clear();
        return true;
    }

    public boolean registerSarRegistrant(int type, Message result) {
        boolean isSuccess = false;
        boolean z = false;
        if (result == null) {
            riljLog("registerSarRegistrant the param result is null");
            return false;
        }
        if (type != 4) {
            switch (type) {
                case 1:
                    this.mCurBandClassRegistrant = new Registrant(result.getTarget(), result.what, result.obj);
                    isSuccess = true;
                    break;
                case 2:
                    this.mAntStateRegistrant = new Registrant(result.getTarget(), result.what, result.obj);
                    isSuccess = true;
                    break;
            }
        }
        this.mMaxTxPowerRegistrant = new Registrant(result.getTarget(), result.what, result.obj);
        isSuccess = true;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerSarRegistrant type = ");
        stringBuilder.append(type);
        stringBuilder.append(",isSuccess = ");
        if (!isSuccess) {
            z = true;
        }
        stringBuilder.append(z);
        riljLog(stringBuilder.toString());
        return isSuccess;
    }

    public void notifyAntOrMaxTxPowerInfo(byte[] data) {
        ByteBuffer payload = ByteBuffer.wrap(data);
        payload.order(ByteOrder.nativeOrder());
        int type_id = payload.get();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("type_id in notifyAntOrMaxTxPowerInfo is ");
        stringBuilder.append(type_id);
        riljLog(stringBuilder.toString());
        int response_size = payload.getShort();
        if (response_size < 0 || response_size > 8192) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Response Size is Invalid ");
            stringBuilder2.append(response_size);
            riljLog(stringBuilder2.toString());
            return;
        }
        int result = payload.getInt();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("notifyAntOrMaxTxPowerInfo result=");
        stringBuilder3.append(result);
        riljLog(stringBuilder3.toString());
        ByteBuffer resultData = ByteBuffer.allocate(4);
        resultData.order(ByteOrder.nativeOrder());
        resultData.putInt(result);
        notifyResultByType(type_id, resultData);
    }

    public void notifyBandClassInfo(byte[] data) {
        ByteBuffer payload = ByteBuffer.wrap(data);
        payload.order(ByteOrder.nativeOrder());
        int activeBand = payload.getInt();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyBandClassInfo activeBand=");
        stringBuilder.append(activeBand);
        riljLog(stringBuilder.toString());
        ByteBuffer resultData = ByteBuffer.allocate(4);
        resultData.order(ByteOrder.nativeOrder());
        resultData.putInt(activeBand);
        if (this.mCurBandClassRegistrant != null) {
            this.mCurBandClassRegistrant.notifyResult(resultData.array());
        }
    }

    private void notifyResultByType(int type, ByteBuffer resultData) {
        boolean isSuccess = false;
        riljLog("notifyResultByType start");
        if (type != 2) {
            if (type == 4 && this.mMaxTxPowerRegistrant != null) {
                this.mMaxTxPowerRegistrant.notifyResult(resultData.array());
                isSuccess = true;
            }
        } else if (this.mAntStateRegistrant != null) {
            this.mAntStateRegistrant.notifyResult(resultData.array());
            isSuccess = true;
        }
        if (!isSuccess) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyResultByType type = ");
            stringBuilder.append(type);
            stringBuilder.append(" notifyResult failed");
            riljLog(stringBuilder.toString());
        }
    }

    public void sendRacChangeBroadcast(byte[] data) {
        if (data != null) {
            ByteBuffer payload = ByteBuffer.wrap(data);
            payload.order(ByteOrder.nativeOrder());
            int rat = payload.get();
            int rac = payload.get();
            String str = RILJ_LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rat: ");
            stringBuilder.append(rat);
            stringBuilder.append(" rac: ");
            stringBuilder.append(rac);
            Rlog.d(str, stringBuilder.toString());
            Intent intent = new Intent("com.huawei.android.intent.action.RAC_CHANGED");
            intent.putExtra("rat", rat);
            intent.putExtra("rac", rac);
            if (this.mContext != null) {
                this.mContext.sendBroadcast(intent);
            }
        }
    }

    public void sendHWBufferSolicited(Message result, int event, byte[] reqData) {
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendHWBufferSolicited, event:");
        stringBuilder.append(event);
        stringBuilder.append(", reqdata:");
        stringBuilder.append(IccUtils.bytesToHexString(reqData));
        Rlog.v(str, stringBuilder.toString());
        int length = reqData == null ? 0 : reqData.length;
        RIL ril = this.mRil;
        int dataSize = 5 + length;
        ByteBuffer buf = ByteBuffer.wrap(new byte[(("QOEMHOOK".length() + 8) + dataSize)]);
        try {
            buf.order(ByteOrder.nativeOrder());
            RIL ril2 = this.mRil;
            buf.put("QOEMHOOK".getBytes("UTF-8"));
            ril2 = this.mRil;
            buf.putInt(598043);
            buf.putInt(dataSize);
            buf.putInt(event);
            buf.put((byte) length);
            if (length > 0) {
                ril2 = this.mRil;
                if (HWBuffer.BUFFER_SIZE >= length) {
                    buf.put(reqData);
                }
            }
            this.mRil.invokeOemRilRequestRaw(buf.array(), result);
        } catch (UnsupportedEncodingException e) {
            Rlog.d(RILJ_LOG_TAG, "sendHWBufferSolicited failed, UnsupportedEncodingException");
        }
    }

    public void processHWBufferUnsolicited(byte[] respData) {
        if (respData == null || 5 > respData.length) {
            Rlog.d(RILJ_LOG_TAG, "response data is null or unavailable, it from Qcril !!!");
        } else {
            this.mRil.mHWBufferRegistrants.notifyRegistrants(new AsyncResult(null, respData, null));
        }
    }

    public void notifyDeviceState(final String device, final String state, final String extra, Message result) {
        invokeIRadio(537, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.impactAntDevstate(serial, device, state, extra);
            }
        });
    }

    public void iccOpenLogicalChannel(final String AID, final byte p2, Message response) {
        invokeIRadio(536, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                String str = AID;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("");
                stringBuilder.append(p2);
                radio.openChannelWithP2(serial, str, stringBuilder.toString());
            }
        });
    }

    /* JADX WARNING: Missing block: B:14:0x0059, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void existNetworkInfo(String state) {
        unsljLog(3034);
        if (state == null || state.length() < 3) {
            Rlog.d(RILJ_LOG_TAG, "plmn para error! break");
            return;
        }
        this.mcc_operator = state.substring(0, 3);
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("recieved RIL_UNSOL_HW_EXIST_NETWORK_INFO with mcc_operator =");
        stringBuilder.append(this.mcc_operator);
        stringBuilder.append("and mMcc =");
        stringBuilder.append(mMcc);
        Rlog.d(str, stringBuilder.toString());
        if ((this.mcc_operator == null || !this.mcc_operator.equals(mMcc)) && ((mMcc == null || mMcc.equals(HwFullNetworkChipCommon.PREFIX_LOCAL_MCC)) && mMcc != null && this.shouldReportRoamingPlusInfo)) {
            Intent intent = new Intent();
            intent.setAction(ACTION_HW_EXIST_NETWORK_INFO_ROAMING_PLUS);
            intent.putExtra("current_mcc", this.mcc_operator);
            this.existNetworkContext.sendBroadcast(intent);
            String str2 = RILJ_LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendBroadcast:ACTION_HW_EXIST_NETWORK_INFO_ROAMING_PLUS with extra: mcc_operator=");
            stringBuilder2.append(this.mcc_operator);
            Rlog.d(str2, stringBuilder2.toString());
            this.shouldReportRoamingPlusInfo = false;
        }
    }

    private String convertNullToEmptyString(String string) {
        return string != null ? string : "";
    }

    public void sendSimMatchedOperatorInfo(String opKey, String opName, int state, String reserveField, Message response) {
        riljLog("sendSimMatchedOperatorInfo");
        final String str = opKey;
        final String str2 = opName;
        final int i = state;
        final String str3 = reserveField;
        invokeIRadio(2177, response, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                if (radio != null) {
                    radio.sendSimMatchedOperatorInfo(serial, str, str2, i, str3);
                    return;
                }
                HwRILReferenceImpl.this.riljLog("sendSimMatchedOperatorInfo: not support by radio 2.0");
            }
        });
    }

    public void getSignalStrength(Message result) {
        invokeIRadio(331, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getHwSignalStrength(serial);
            }
        });
    }

    public void responseDataCallList(RadioResponseInfo responseInfo, ArrayList<SetupDataCallResult> dataCallResultList) {
        RILRequest rr = this.mRil.processResponse(responseInfo);
        ArrayList<DataCallResponse> dcResponseList = new ArrayList();
        int resultSize = dataCallResultList.size();
        for (int i = 0; i < resultSize; i++) {
            dcResponseList.add(convertDataCallResult((SetupDataCallResult) dataCallResultList.get(i)));
        }
        if (rr != null) {
            if (responseInfo.error == 0) {
                sendMessageResponse(rr.mResult, dcResponseList);
            }
            this.mRil.processResponseDone(rr, responseInfo, dcResponseList);
        }
    }

    private DataCallResponse convertDataCallResult(SetupDataCallResult dcResult) {
        StringBuilder stringBuilder;
        SetupDataCallResult setupDataCallResult = dcResult;
        if (setupDataCallResult == null) {
            return null;
        }
        int length;
        String address;
        String[] addresses = null;
        if (!TextUtils.isEmpty(setupDataCallResult.addresses)) {
            addresses = setupDataCallResult.addresses.split("\\s+");
        }
        String[] addresses2 = addresses;
        List<LinkAddress> laList = new ArrayList();
        int i = 0;
        if (addresses2 != null) {
            for (String address2 : addresses2) {
                address = address2.trim();
                if (!address.isEmpty()) {
                    try {
                        LinkAddress la;
                        if (address.split("/").length == 2) {
                            la = new LinkAddress(address);
                        } else {
                            InetAddress ia = NetworkUtils.numericToInetAddress(address);
                            la = new LinkAddress(ia, ia instanceof Inet4Address ? 32 : 64);
                        }
                        laList.add(la);
                    } catch (IllegalArgumentException e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IllegalArgumentException: Unknown address: ");
                        stringBuilder.append(address);
                        riljLog(stringBuilder.toString());
                    }
                }
            }
        }
        addresses = null;
        if (!TextUtils.isEmpty(setupDataCallResult.dnses)) {
            addresses = setupDataCallResult.dnses.split("\\s+");
        }
        String[] dnses = addresses;
        List<InetAddress> dnsList = new ArrayList();
        if (dnses != null) {
            for (String address22 : dnses) {
                address = address22.trim();
                try {
                    dnsList.add(NetworkUtils.numericToInetAddress(address));
                } catch (IllegalArgumentException e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("IllegalArgumentException: Unknown dns: ");
                    stringBuilder.append(address);
                    riljLog(stringBuilder.toString());
                }
            }
        }
        addresses = null;
        if (!TextUtils.isEmpty(setupDataCallResult.gateways)) {
            addresses = setupDataCallResult.gateways.split("\\s+");
        }
        String[] gateways = addresses;
        List<InetAddress> gatewayList = new ArrayList();
        if (gateways != null) {
            length = gateways.length;
            while (i < length) {
                String gateway = gateways[i].trim();
                try {
                    gatewayList.add(NetworkUtils.numericToInetAddress(gateway));
                } catch (IllegalArgumentException e3) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("IllegalArgumentException: Unknown gateway: ");
                    stringBuilder2.append(gateway);
                    riljLog(stringBuilder2.toString());
                }
                i++;
            }
        }
        return new DataCallResponse(setupDataCallResult.status, setupDataCallResult.suggestedRetryTime, setupDataCallResult.cid, setupDataCallResult.active, setupDataCallResult.type, setupDataCallResult.ifname, laList, dnsList, gatewayList, new ArrayList(Arrays.asList(setupDataCallResult.pcscf.trim().split("\\s+"))), setupDataCallResult.mtu);
    }

    static void sendMessageResponse(Message msg, Object ret) {
        if (msg != null) {
            AsyncResult.forMessage(msg, ret, null);
            msg.sendToTarget();
        }
    }

    public void getSimMatchedFileFromRilCache(final int fileId, Message result) {
        invokeIRadio(2179, result, new RILCommand() {
            public void excute(IRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getSimMatchedFileFromRilCache(serial, fileId);
            }
        });
    }
}
