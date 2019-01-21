package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneStateListener;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConfig;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimUtils;
import java.util.List;

public class HwSubscriptionManager extends Handler {
    private static final boolean DBG = true;
    public static final int DEFAULT_SLOT_ID = 0;
    public static final int DEFAULT_SUB_ID = 0;
    private static final boolean ERROR = true;
    private static final int EVENT_FAST_SWITCH_SIM_SLOT_RESULT = 12;
    private static final int EVENT_SET_SUBSCRIPTION_TIMEOUT = 11;
    private static final int EVENT_SET_UICC_SUBSCRIPTION_DONE = 10;
    private static final String LOG_TAG = "HwSubMgr";
    private static final int SUB_0 = 0;
    private static final int SUB_1 = 1;
    public static final int SUB_INIT_STATE = 255;
    private static final int SUB_NUMS = TelephonyManager.getDefault().getPhoneCount();
    private static final int TIME_SET_SUBSCRIPTION_TIMEOUT = 90000;
    private static final String USER_DATACALL_SUBSCRIPTION = "user_datacall_sub";
    private static boolean hasRetry = false;
    private static HwSubscriptionManager mHwSubscriptionManager;
    private static UiccController mUiccController;
    private static SubscriptionControllerUtils subscriptionControllerUtils = new SubscriptionControllerUtils();
    private boolean mCardChange;
    private CommandsInterface[] mCi;
    private Message mCompleteMsg;
    private Context mContext;
    private Handler mHandler;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener(Integer.valueOf(1)) {
        public void onCallStateChanged(int state, String incomingNumber) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCallStateChanged   state : ");
            stringBuilder.append(state);
            HwSubscriptionManager.logd(stringBuilder.toString());
            if (state == 0 && HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-sub")) {
                HwSubscriptionManager.this.setSubscription(1, false, null);
            }
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int result;
            boolean targetSate = true;
            int slotId;
            if ("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT".equals(intent.getAction())) {
                slotId = intent.getIntExtra("subscription", -1);
                HwSubscriptionManager.this.mSetSubscriptionInProgress = false;
                HwSubscriptionManager.this.mHandler.removeMessages(11);
                result = intent.getIntExtra("operationResult", 0);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received ACTION_SUBSCRIPTION_SET_UICC_RESULT on slotId: ");
                stringBuilder.append(slotId);
                stringBuilder.append(", result = ");
                stringBuilder.append(result);
                HwSubscriptionManager.logd(stringBuilder.toString());
                if (result == 1) {
                    HwSubscriptionManager.this.sendCompleteMsg(new RuntimeException("setSubScription fail!!!"));
                    if (!HwSubscriptionManager.hasRetry && slotId == 1 && HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-sub")) {
                        boolean targetState = ((Integer) intent.getExtra("newSubState", Integer.valueOf(-1))).intValue() != 0;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("retry deactive sub2, targetState: ");
                        stringBuilder2.append(targetState);
                        HwSubscriptionManager.logd(stringBuilder2.toString());
                        HwSubscriptionManager.this.setSubscription(1, targetState, null);
                        HwSubscriptionManager.hasRetry = true;
                    }
                } else {
                    HwSubscriptionManager.this.sendCompleteMsg(null);
                }
                if (slotId == 1 && HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-sub")) {
                    Intent intentForMDM = new Intent("android.intent.ACTION_MDM_DISABLE_SUB_RESULT");
                    intent.putExtra("disableSubResult", result);
                    context.sendBroadcast(intentForMDM);
                }
            } else if ("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE".equals(intent.getAction())) {
                slotId = intent.getIntExtra("subscription", -1);
                result = intent.getIntExtra("intContent", 0);
                String column = intent.getStringExtra("columnName");
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Received ACTION_SUBINFO_CONTENT_CHANGE on slotId: ");
                stringBuilder3.append(slotId);
                stringBuilder3.append(" for ");
                stringBuilder3.append(column);
                stringBuilder3.append(", intValue: ");
                stringBuilder3.append(result);
                HwSubscriptionManager.logd(stringBuilder3.toString());
                if ("sub_state".equals(column) && -1 != slotId) {
                    if (result == 1) {
                        HwSubscriptionManager.this.notifySlotSubscriptionActivated(slotId);
                    } else if (result == 0) {
                        HwSubscriptionManager.this.notifySlotSubscriptionDeactivated(slotId);
                    }
                }
            }
            if ("com.huawei.devicepolicy.action.POLICY_CHANGED".equals(intent.getAction())) {
                HwSubscriptionManager.logd("com.huawei.devicepolicy.action.POLICY_CHANGED");
                String action_tag = intent.getStringExtra("action_tag");
                if (!TextUtils.isEmpty(action_tag) && action_tag.equals("action_disable_sub")) {
                    int subId = intent.getIntExtra(HwVSimConstants.EXTRA_NETWORK_SCAN_SUBID, -1);
                    if (subId == 0) {
                        boolean z = true;
                    } else {
                        result = 0;
                    }
                    if (intent.getBooleanExtra("subState", false)) {
                        targetSate = false;
                    }
                    HwSubscriptionManager.this.setSubscription(subId, targetSate, null);
                    HwSubscriptionManager.hasRetry = false;
                }
            }
        }
    };
    private Message mSavedCompleteMsg;
    private boolean mSetSubscriptionInProgress = false;
    private RegistrantList[] mSubActivatedRegistrantsOnSlot;
    private RegistrantList[] mSubDeactivatedRegistrantsOnSlot;
    private SubscriptionController mSubscriptionController;
    private SubscriptionHelper mSubscriptionHelper;
    private TelephonyManager mTelephonyManager;

    public HwSubscriptionManager(Context context, CommandsInterface[] ci) {
        int i = 0;
        this.mContext = context;
        this.mCi = ci;
        this.mHandler = this;
        this.mSubscriptionController = SubscriptionController.getInstance();
        mUiccController = UiccController.getInstance();
        IntentFilter filter = new IntentFilter("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT");
        filter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        filter.addAction("com.huawei.devicepolicy.action.POLICY_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, filter);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        } else {
            logd("mTelephonyManager is null!");
        }
        this.mSubDeactivatedRegistrantsOnSlot = new RegistrantList[SUB_NUMS];
        this.mSubActivatedRegistrantsOnSlot = new RegistrantList[SUB_NUMS];
        while (i < SUB_NUMS) {
            this.mSubActivatedRegistrantsOnSlot[i] = new RegistrantList();
            this.mSubDeactivatedRegistrantsOnSlot[i] = new RegistrantList();
            i++;
        }
        logd("Constructor - Complete");
    }

    public static HwSubscriptionManager init(Context c, CommandsInterface[] ci) {
        HwSubscriptionManager hwSubscriptionManager;
        synchronized (HwSubscriptionManager.class) {
            if (mHwSubscriptionManager == null) {
                mHwSubscriptionManager = new HwSubscriptionManager(c, ci);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("init() called multiple times!  mHwSubscriptionManager = ");
                stringBuilder.append(mHwSubscriptionManager);
                logw(stringBuilder.toString());
            }
            hwSubscriptionManager = mHwSubscriptionManager;
        }
        return hwSubscriptionManager;
    }

    public static HwSubscriptionManager getInstance() {
        if (mHwSubscriptionManager == null) {
            logw("getInstance null");
        }
        return mHwSubscriptionManager;
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 11:
                logd("EVENT_SET_SUBSCRIPTION_TIMEOUT");
                this.mSetSubscriptionInProgress = false;
                sendCompleteMsg(new RuntimeException("setSubScription timeout!!!"));
                return;
            case 12:
                logd("EVENT_FAST_SWITCH_SIM_SLOT_RESULT");
                logd("send mSavedCompleteMsg to target");
                if (this.mSavedCompleteMsg != null) {
                    AsyncResult ar = msg.obj;
                    AsyncResult.forMessage(this.mSavedCompleteMsg).exception = ar.exception;
                    this.mSavedCompleteMsg.sendToTarget();
                    this.mSavedCompleteMsg = null;
                    return;
                }
                return;
            default:
                return;
        }
    }

    public boolean setSubscription(int slotId, boolean activate, Message onCompleteMsg) {
        int i = slotId;
        boolean z = activate;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setSubscription: slotId = ");
        stringBuilder.append(i);
        stringBuilder.append(", activate = ");
        stringBuilder.append(z);
        logd(stringBuilder.toString());
        boolean isNotVSim = false;
        if (!setSubscriptionCheck(slotId)) {
            return false;
        }
        HwFullNetworkManager.getInstance().resetUiccSubscriptionResultFlag(i);
        int subId = getSubIdFromSlotId(slotId);
        this.mCompleteMsg = onCompleteMsg;
        int otherSlotId = i == 0 ? 1 : 0;
        Message response = null;
        StringBuilder stringBuilder2;
        boolean isNeedSetDefault4GSlot;
        if (z) {
            if (this.mSubscriptionController.getSubState(subId) == 1) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setSubscription: slotId = ");
                stringBuilder2.append(subId);
                stringBuilder2.append(" is already ACTIVED.");
                logd(stringBuilder2.toString());
                sendCompleteMsg(null);
            } else {
                boolean isTLHybirdActiveCMCC = HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && HwFullNetworkManager.getInstance().isCMCCCardBySlotId(i) && HwFullNetworkManager.getInstance().isCMCCHybird();
                boolean isCTHybirdActive = HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE && HwFullNetworkManager.getInstance().isCTCardBySlotId(i) && HwFullNetworkManager.getInstance().isCTHybird();
                isNeedSetDefault4GSlot = HwFullNetworkConfig.IS_HISI_DSDX && HwTelephonyManagerInner.getDefault().getDefault4GSlotId() == otherSlotId && ((this.mSubscriptionController.getSubState(otherSlotId) == 0 || isTLHybirdActiveCMCC || isCTHybirdActive) && SystemProperties.getBoolean("persist.sys.dualcards", false));
                if (isNeedSetDefault4GSlot) {
                    if (!(HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimCauseCardReload())) {
                        isNotVSim = true;
                    }
                    if (isNotVSim) {
                        if (HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT) {
                            response = obtainMessage(12);
                            this.mSavedCompleteMsg = this.mCompleteMsg;
                            this.mCompleteMsg = null;
                            this.mSubscriptionController.activateSubId(subId);
                        }
                        this.mSubscriptionController.setSubState(getSubIdFromSlotId(slotId), 1);
                        HwFullNetworkManager.getInstance().setMainSlot(i, response);
                        return true;
                    }
                }
                this.mSubscriptionController.activateSubId(subId);
                if (subId != 1 && HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(otherSlotId, "disable-data")) {
                    HwFullNetworkManager.getInstance().setMainSlot(i, null);
                }
                this.mHandler.sendEmptyMessageDelayed(11, 90000);
            }
        } else if (this.mSubscriptionController.getSubState(subId) == 0) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setSubscription: slotId = ");
            stringBuilder2.append(subId);
            stringBuilder2.append(" is already INACTIVED.");
            logd(stringBuilder2.toString());
            sendCompleteMsg(null);
        } else {
            isNeedSetDefault4GSlot = HwFullNetworkConfig.IS_HISI_DSDX && HwTelephonyManagerInner.getDefault().getDefault4GSlotId() == i && this.mSubscriptionController.getSubState(otherSlotId) == 1 && SystemProperties.getBoolean("persist.sys.dualcards", false);
            if (isNeedSetDefault4GSlot) {
                boolean isNotVSim2 = (HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimCauseCardReload()) ? false : true;
                if (isNotVSim2) {
                    if (HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT) {
                        response = obtainMessage(12);
                        this.mSavedCompleteMsg = this.mCompleteMsg;
                        this.mCompleteMsg = null;
                        this.mSubscriptionController.deactivateSubId(subId);
                    }
                    this.mSubscriptionController.setSubState(getSubIdFromSlotId(slotId), 0);
                    HwFullNetworkManager.getInstance().setMainSlot(otherSlotId, response);
                    return true;
                }
            }
            this.mSubscriptionController.deactivateSubId(subId);
            this.mHandler.sendEmptyMessageDelayed(11, 90000);
        }
        return true;
    }

    private static boolean isValidSlotId(int slotId) {
        return slotId >= 0 && slotId < SUB_NUMS;
    }

    private boolean setSubscriptionCheck(int slotId) {
        if (isValidSlotId(slotId)) {
            if (this.mSubscriptionController == null) {
                this.mSubscriptionController = SubscriptionController.getInstance();
                if (this.mSubscriptionController == null) {
                    loge("setSubscriptionCheck: mSubscriptionController is null... return false");
                    return false;
                }
            }
            if (this.mSubscriptionHelper == null) {
                this.mSubscriptionHelper = SubscriptionHelper.getInstance();
                if (this.mSubscriptionHelper == null) {
                    loge("setSubscriptionCheck: mSubscriptionHelper is null... return false");
                    return false;
                }
            }
            if (mUiccController == null) {
                mUiccController = UiccController.getInstance();
                if (mUiccController == null) {
                    loge("setSubscriptionCheck: mUiccController is null... return false");
                    return false;
                }
            }
            if (this.mSetSubscriptionInProgress) {
                logd("setSubscriptionCheck: operation is in processing!! return false");
                return false;
            } else if (HwFullNetworkManager.getInstance().get4GSlotInProgress()) {
                logd("setSubscriptionCheck: setDefault4GSlot is in processing!! return false");
                return false;
            } else {
                UiccCard uiccCard = mUiccController.getUiccCard(slotId);
                CardState cardState = CardState.CARDSTATE_ABSENT;
                if (uiccCard != null) {
                    cardState = uiccCard.getCardState();
                }
                if (cardState != CardState.CARDSTATE_PRESENT) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setSubscriptionCheck: Card is not present in slot ");
                    stringBuilder.append(slotId);
                    stringBuilder.append(", return false");
                    logd(stringBuilder.toString());
                    return false;
                } else if (TelephonyManager.getDefault().getCallState(getSubIdFromSlotId(slotId)) != 0) {
                    logw("setSubscriptionCheck: Call State is not IDLE, can't set subscription!");
                    return false;
                } else {
                    int otherSlotId = slotId == 0 ? 1 : 0;
                    if (HwFullNetworkConfig.IS_HISI_DSDX && slotId == HwTelephonyManagerInner.getDefault().getDefault4GSlotId() && TelephonyManager.getDefault().getCallState(getSubIdFromSlotId(otherSlotId)) != 0) {
                        logw("setSubscriptionCheck: Call State is not IDLE, can't set default sub subscription!");
                        return false;
                    } else if (!HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE || !HwFullNetworkManager.getInstance().isCMCCCardBySlotId(slotId) || this.mSubscriptionController.getSubState(slotId) != 0 || HwFullNetworkManager.getInstance().isCMCCCardBySlotId(otherSlotId) || TelephonyManager.getDefault().getCallState(getSubIdFromSlotId(otherSlotId)) == 0) {
                        return true;
                    } else {
                        logw("setSubscriptionCheck: other card is not idle, TL version can not active CMCC card!");
                        return false;
                    }
                }
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("setSubscriptionCheck: slotId is not correct : ");
        stringBuilder2.append(slotId);
        loge(stringBuilder2.toString());
        return false;
    }

    protected void updateUserPreferences(boolean setDds) {
        boolean z = setDds;
        List<SubscriptionInfo> subInfoList = this.mSubscriptionController.getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
        int mActCount = 0;
        SubscriptionInfo mNextActivatedSub = null;
        int defaultSubId = this.mSubscriptionController.getDefaultSubId();
        int defaultDataSubId = this.mSubscriptionController.getDefaultDataSubId();
        int defaultVoiceSubId = this.mSubscriptionController.getDefaultVoiceSubId();
        int defaultSmsSubId = this.mSubscriptionController.getDefaultSmsSubId();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateUserPreferences: defaultSubId = ");
        stringBuilder.append(defaultSubId);
        stringBuilder.append(",defaultDataSubId = ");
        stringBuilder.append(defaultDataSubId);
        stringBuilder.append(",defaultVoiceSubId = ");
        stringBuilder.append(defaultVoiceSubId);
        stringBuilder.append(",defaultSmsSubId = ");
        stringBuilder.append(defaultSmsSubId);
        stringBuilder.append(",setDDs = ");
        stringBuilder.append(z);
        logd(stringBuilder.toString());
        if (subInfoList == null) {
            logd("updateUserPreferences: subscription are not avaiable!!! Exit !");
            return;
        }
        for (SubscriptionInfo subInfo : subInfoList) {
            if (this.mSubscriptionController.getSubState(subInfo.getSubscriptionId()) == 1) {
                mActCount++;
                if (mNextActivatedSub == null) {
                    mNextActivatedSub = subInfo;
                }
            }
        }
        if (mActCount < 2) {
            this.mSubscriptionController.setSMSPromptEnabled(false);
            this.mSubscriptionController.setVoicePromptEnabled(false);
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("updateUserPreferences: mActCount = ");
        stringBuilder.append(mActCount);
        logd(stringBuilder.toString());
        if (mNextActivatedSub != null) {
            if (mActCount == 1) {
                if (this.mSubscriptionController.getSubState(defaultSubId) == 0 || TelephonyManager.getDefault().getSimState(defaultSubId) == 1) {
                    subscriptionControllerUtils.setDefaultFallbackSubId(this.mSubscriptionController, mNextActivatedSub.getSubscriptionId());
                }
                int dataSubState = this.mSubscriptionController.getSubState(defaultDataSubId);
                int dataSimState = TelephonyManager.getDefault().getSimState(defaultDataSubId);
                if (!(HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK || HwFullNetworkConfig.IS_QCRIL_CROSS_MAPPING || (!z && dataSubState != 0 && dataSimState != 1))) {
                    if (dataSubState == 0 || dataSimState == 1) {
                        if (HwVSimUtils.isVSimEnabled()) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("updateUserPreferences: vsim is enabled, block set dds, dataSubState = ");
                            stringBuilder2.append(dataSubState);
                            logd(stringBuilder2.toString());
                        } else {
                            defaultDataSubId = mNextActivatedSub.getSubscriptionId();
                        }
                    }
                    this.mSubscriptionController.setDefaultDataSubId(defaultDataSubId);
                }
                if ((this.mSubscriptionController.getSubState(defaultVoiceSubId) == 0 || TelephonyManager.getDefault().getSimState(defaultVoiceSubId) == 1) && !this.mSubscriptionController.isVoicePromptEnabled()) {
                    this.mSubscriptionController.setDefaultVoiceSubId(mNextActivatedSub.getSubscriptionId());
                }
                if ((this.mSubscriptionController.getSubState(defaultSmsSubId) == 0 || TelephonyManager.getDefault().getSimState(defaultSmsSubId) == 1) && !this.mSubscriptionController.isSMSPromptEnabled()) {
                    this.mSubscriptionController.setDefaultSmsSubId(mNextActivatedSub.getSubscriptionId());
                }
            } else if (mActCount == subInfoList.size()) {
                StringBuilder stringBuilder3;
                int UserPrefDefaultSubId = getSubIdFromSlotId(Global.getInt(this.mContext.getContentResolver(), HwFullNetworkConstants.USER_DEFAULT_SUBSCRIPTION, 0));
                if (!(UserPrefDefaultSubId == defaultSubId && UserPrefDefaultSubId == defaultVoiceSubId && UserPrefDefaultSubId == defaultSmsSubId)) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("updateUserPreferences: set UserPrefDefaultSubId from ");
                    stringBuilder4.append(defaultSubId);
                    stringBuilder4.append("to ");
                    stringBuilder4.append(UserPrefDefaultSubId);
                    logd(stringBuilder4.toString());
                    subscriptionControllerUtils.setDefaultFallbackSubId(this.mSubscriptionController, UserPrefDefaultSubId);
                    this.mSubscriptionController.setDefaultSmsSubId(UserPrefDefaultSubId);
                    this.mSubscriptionController.setDefaultVoiceSubId(UserPrefDefaultSubId);
                }
                int UserPrefDataSubId = getSubIdFromSlotId(Global.getInt(this.mContext.getContentResolver(), USER_DATACALL_SUBSCRIPTION, 0));
                if (!(UserPrefDataSubId == defaultDataSubId || HwFullNetworkConfig.IS_HISI_DSDX || HwFullNetworkConfig.IS_QCRIL_CROSS_MAPPING || HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK || HwFullNetworkManager.getInstance().isSettingDefaultData())) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("updateUserPreferences: set UserPrefDataSubId from ");
                    stringBuilder3.append(defaultDataSubId);
                    stringBuilder3.append("to ");
                    stringBuilder3.append(UserPrefDataSubId);
                    logd(stringBuilder3.toString());
                    this.mSubscriptionController.setDefaultDataSubId(UserPrefDataSubId);
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isSet4GSlotManuallyTriggered = ");
                stringBuilder3.append(HwFullNetworkManager.getInstance().isSet4GSlotManuallyTriggered());
                logd(stringBuilder3.toString());
                if (HwFullNetworkConfig.IS_HISI_DSDX && !HwFullNetworkManager.getInstance().isSet4GSlotManuallyTriggered()) {
                    int default4Gslot = HwTelephonyManagerInner.getDefault().getDefault4GSlotId();
                    if (!(default4Gslot == defaultDataSubId || HwFullNetworkManager.getInstance().isSettingDefaultData())) {
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("updateUserPreferences: set defaultDataSubId to default4Gslot from ");
                        stringBuilder5.append(defaultDataSubId);
                        stringBuilder5.append(" to ");
                        stringBuilder5.append(default4Gslot);
                        logd(stringBuilder5.toString());
                        this.mSubscriptionController.setDefaultDataSubId(default4Gslot);
                    }
                }
                if (HwTelephonyManagerInner.getDefault().isDataConnectivityDisabled(1, "disable-data")) {
                    HwFullNetworkManager.getInstance().setDefault4GSlotForMDM();
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateUserPreferences: after current DataSub = ");
            stringBuilder.append(this.mSubscriptionController.getDefaultDataSubId());
            stringBuilder.append(" VoiceSub = ");
            stringBuilder.append(this.mSubscriptionController.getDefaultVoiceSubId());
            stringBuilder.append(" SmsSub = ");
            stringBuilder.append(this.mSubscriptionController.getDefaultSmsSubId());
            logd(stringBuilder.toString());
        }
    }

    public int getUserPrefDataSubId() {
        int[] userPrefDataSubId = this.mSubscriptionController.getSubId(Global.getInt(this.mContext.getContentResolver(), USER_DATACALL_SUBSCRIPTION, 0));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getUserPrefDataSubId: userPrefDataSubId = ");
        stringBuilder.append(userPrefDataSubId[0]);
        logd(stringBuilder.toString());
        return userPrefDataSubId[0];
    }

    public void updateDataSlot() {
        List<SubscriptionInfo> subInfoList = this.mSubscriptionController.getActiveSubscriptionInfoList(this.mContext.getOpPackageName());
        int mActCount = 0;
        if (subInfoList != null) {
            for (SubscriptionInfo subInfo : subInfoList) {
                if (this.mSubscriptionController.getSubState(subInfo.getSubscriptionId()) == 1) {
                    mActCount++;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateDataSlot: mActCount =");
            stringBuilder.append(mActCount);
            logd(stringBuilder.toString());
            if (mActCount > 1) {
                int defaultSubId = this.mSubscriptionController.getDefaultDataSubId();
                int UserPrefDataSlotId = getUserPrefDataSubId();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateDataSlot: set UserPrefDefaultSubId from ");
                stringBuilder2.append(defaultSubId);
                stringBuilder2.append("to ");
                stringBuilder2.append(UserPrefDataSlotId);
                logd(stringBuilder2.toString());
                this.mSubscriptionController.setDefaultDataSubId(UserPrefDataSlotId);
            }
        }
    }

    private void sendCompleteMsg(Exception e) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendCompleteMsg to target, Exception = ");
        stringBuilder.append(e);
        logd(stringBuilder.toString());
        if (this.mCompleteMsg != null) {
            AsyncResult.forMessage(this.mCompleteMsg).exception = e;
            this.mCompleteMsg.sendToTarget();
            this.mCompleteMsg = null;
        }
    }

    public void setUserPrefDefaultSlotId(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setUserPrefDefaultSubId: slotId = ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        if (slotId < 0 || slotId >= SUB_NUMS) {
            loge("setUserPrefDefaultSubId: invalid slotId!!!");
            return;
        }
        Global.putInt(this.mContext.getContentResolver(), HwFullNetworkConstants.USER_DEFAULT_SUBSCRIPTION, slotId);
        int subId = getSubIdFromSlotId(slotId);
        subscriptionControllerUtils.setDefaultFallbackSubId(this.mSubscriptionController, subId);
        this.mSubscriptionController.setDefaultSmsSubId(subId);
        this.mSubscriptionController.setDefaultVoiceSubId(subId);
    }

    public void setUserPrefDataSlotId(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setUserPrefDataSubId: slotId = ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        if (slotId < 0 || slotId >= SUB_NUMS) {
            loge("setUserPrefDefaultSubId: invalid slotId!!!");
            return;
        }
        Global.putInt(this.mContext.getContentResolver(), USER_DATACALL_SUBSCRIPTION, slotId);
        this.mSubscriptionController.setDefaultDataSubId(getSubIdFromSlotId(slotId));
    }

    protected int getDefaultDataSlotId() {
        int slotId = this.mSubscriptionController.getSlotIndex(this.mSubscriptionController.getDefaultDataSubId());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getDefaultDataSlotId: slotId = ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        return slotId;
    }

    protected int getSubIdFromSlotId(int slotId) {
        return this.mSubscriptionController.getSubId(slotId)[0];
    }

    public void registerForSubscriptionActivatedOnSlot(int slotId, Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mSubActivatedRegistrantsOnSlot[slotId]) {
            this.mSubActivatedRegistrantsOnSlot[slotId].add(r);
        }
    }

    public void unregisterForSubscriptionActivatedOnSlot(int slotId, Handler h) {
        synchronized (this.mSubActivatedRegistrantsOnSlot[slotId]) {
            this.mSubActivatedRegistrantsOnSlot[slotId].remove(h);
        }
    }

    public void registerForSubscriptionDeactivatedOnSlot(int slotId, Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        synchronized (this.mSubDeactivatedRegistrantsOnSlot[slotId]) {
            this.mSubDeactivatedRegistrantsOnSlot[slotId].add(r);
        }
    }

    public void unregisterForSubscriptionDeactivatedOnSlot(int slotId, Handler h) {
        synchronized (this.mSubDeactivatedRegistrantsOnSlot[slotId]) {
            this.mSubDeactivatedRegistrantsOnSlot[slotId].remove(h);
        }
    }

    private void notifySlotSubscriptionActivated(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifySlotSubscriptionActivated: slotId = ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        this.mSubActivatedRegistrantsOnSlot[slotId].notifyRegistrants();
    }

    private void notifySlotSubscriptionDeactivated(int slotId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifySlotSubscriptionDeactivated: slotId = ");
        stringBuilder.append(slotId);
        logd(stringBuilder.toString());
        this.mSubDeactivatedRegistrantsOnSlot[slotId].notifyRegistrants();
    }

    private static void logd(String message) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwSubscriptionManager]");
        stringBuilder.append(message);
        Rlog.d(str, stringBuilder.toString());
    }

    private static void logw(String message) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwSubscriptionManager]");
        stringBuilder.append(message);
        Rlog.w(str, stringBuilder.toString());
    }

    private static void loge(String message) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[HwSubscriptionManager]");
        stringBuilder.append(message);
        Rlog.e(str, stringBuilder.toString());
    }
}
