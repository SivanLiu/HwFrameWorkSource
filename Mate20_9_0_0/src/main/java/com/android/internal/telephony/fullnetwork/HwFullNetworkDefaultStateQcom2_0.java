package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwNetworkTypeUtils;
import com.android.internal.telephony.PhoneFactory;

public class HwFullNetworkDefaultStateQcom2_0 extends HwFullNetworkDefaultStateBase {
    private static final String LOG_TAG = "HwFullNetworkDefaultStateQcom2_0";
    private static final int OOS_DELAY_TIME = 20000;
    private HwFullNetworkChipQcom mChipQcom = HwFullNetworkChipQcom.getInstance();

    public HwFullNetworkDefaultStateQcom2_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        logd("HwFullNetworkDefaultStateQcom2_0 constructor");
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.huawei.intent.action.ACTION_SUBSCRIPTION_SET_UICC_RESULT");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.obj == null) {
            loge("msg or msg.obj is null, return!");
            return;
        }
        AsyncResult ar = null;
        if (msg.obj instanceof AsyncResult) {
            ar = msg.obj;
        }
        switch (msg.what) {
            case HwFullNetworkConstants.EVENT_SET_LTE_SERVICE_ABILITY /*2004*/:
                setNetworkType(ar, msg.arg1);
                break;
            case HwFullNetworkConstants.EVENT_SET_PRIMARY_STACK_LTE_SWITCH_DONE /*2005*/:
                logd("Received EVENT_SET_PRIMARY_STACK_LTE_SWITCH_DONE");
                handleSetPrimaryStackLteSwitchDone(ar, msg.arg1);
                break;
            case HwFullNetworkConstants.EVENT_SET_SECONDARY_STACK_LTE_SWITCH_DONE /*2006*/:
                logd("Received EVENT_SET_SECONDARY_STACK_LTE_SWITCH_DONE");
                handleSetSecondaryStackLteSwitchDone(ar, msg.arg1);
                break;
            case HwFullNetworkConstants.EVENT_SET_PRIMARY_STACK_ROLL_BACK_DONE /*2007*/:
                logd("Received EVENT_SET_PRIMARY_STACK_ROLL_BACK_DONE");
                handleRollbackDone(ar);
                break;
            case HwFullNetworkConstants.EVENT_RESET_OOS_FLAG /*2008*/:
                int index = this.mChipCommon.getCiIndex(msg).intValue();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received EVENT_RESET_OOS_FLAG on index ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                PhoneFactory.getPhone(index).setOOSFlagOnSelectNetworkManually(false);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    protected void onRadioUnavailable(Integer index) {
        this.mChipCommon.mIccIds[index.intValue()] = null;
    }

    protected void onRadioAvailable(Integer index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRadioAvailable, index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        if (HwFullNetworkConstants.SIM_NUM == 2) {
            syncNetworkTypeFromDB(index.intValue());
        }
        super.onRadioAvailable(index);
    }

    protected void processSubSetUiccResult(Intent intent) {
        int slotId = intent.getIntExtra("subscription", -1);
        int subState = intent.getIntExtra("newSubState", -1);
        int result = intent.getIntExtra("operationResult", -1);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("received ACTION_SUBSCRIPTION_SET_UICC_RESULT,slotId:");
        stringBuilder.append(slotId);
        stringBuilder.append(" subState:");
        stringBuilder.append(subState);
        stringBuilder.append(" result:");
        stringBuilder.append(result);
        logd(stringBuilder.toString());
        if (slotId >= 0 && HwFullNetworkConstants.SIM_NUM > slotId) {
            if (1 == result) {
                if (1 == subState) {
                    this.mChipQcom.mSetUiccSubscriptionResult[slotId] = subState;
                } else {
                    this.mChipQcom.mSetUiccSubscriptionResult[slotId] = -1;
                }
            } else if (result == 0) {
                this.mChipQcom.mSetUiccSubscriptionResult[slotId] = -1;
            }
        }
    }

    private void syncNetworkTypeFromDB(int subId) {
        if (this.mChipCommon.isValidIndex(subId)) {
            int pefMode = this.mChipQcom.getNetworkTypeFromDB(subId);
            boolean firstStart = Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0 && Secure.getInt(this.mContext.getContentResolver(), "user_setup_complete", 1) == 0;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("syncNetworkTypeFromDB, sub = ");
            stringBuilder.append(subId);
            stringBuilder.append(", pefMode = ");
            stringBuilder.append(pefMode);
            stringBuilder.append(", firstStart =");
            stringBuilder.append(firstStart);
            logd(stringBuilder.toString());
            if (pefMode == -1 || firstStart) {
                int defaultPrefMode = SystemProperties.getInt("ro.telephony.default_network", -1);
                if (this.mChipCommon.isDualImsSwitchOpened()) {
                    setLteServiceAbilityForQCOM(subId, 1, defaultPrefMode);
                } else if (subId == this.mChipCommon.getUserSwitchDualCardSlots()) {
                    setLteServiceAbilityForQCOM(subId, 1, defaultPrefMode);
                } else {
                    setLteServiceAbilityForQCOM(subId, 0, defaultPrefMode);
                }
            }
        }
    }

    protected void processPreBootCompleted() {
    }

    protected void setMainSlot(int slotId, Message responseMsg) {
        if (!SystemProperties.getBoolean("persist.sys.dualcards", false)) {
            loge("setMainSlot: main slot switch disabled, return failure");
            this.mChipCommon.sendResponseToTarget(responseMsg, 2);
        } else if (this.mChipCommon.isValidIndex(slotId)) {
            this.mChipQcom.is4GSlotReviewNeeded = 2;
            this.mChipQcom.mUserPref4GSlot = slotId;
            this.mChipCommon.prefer4GSlot = slotId;
            StringBuilder stringBuilder;
            if (slotId == this.mChipCommon.getUserSwitchDualCardSlots()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setDefault4GSlot: the default 4G slot is already ");
                stringBuilder.append(slotId);
                loge(stringBuilder.toString());
                this.mChipCommon.sendResponseToTarget(responseMsg, 0);
            } else if (this.mChipCommon.isSet4GSlotInProgress) {
                loge("setDefault4GSlot: The setting is in progress, return failure");
                this.mChipCommon.sendResponseToTarget(responseMsg, 2);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("setDefault4GSlot: target slot id is: ");
                stringBuilder.append(slotId);
                logd(stringBuilder.toString());
                this.mChipCommon.mSet4GSlotCompleteMsg = responseMsg;
                this.mChipQcom.refreshCardState();
                this.mChipCommon.judgeSubCarrierType();
                this.mChipQcom.judgeNwMode(slotId);
                this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT, slotId, 0, responseMsg).sendToTarget();
            }
        } else {
            loge("setDefault4GSlot: invalid slotid, return failure");
            this.mChipCommon.sendResponseToTarget(responseMsg, 2);
        }
    }

    protected void setLteServiceAbilityForQCOM(int subId, int ability, int lteOnMappingMode) {
        getStackPhoneId();
        if (this.mChipCommon.isDualImsSwitchOpened() || subId != this.mChipCommon.getUserSwitchDualCardSlots()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLteServiceAbilityForQCOM, dual Ims, sub =");
            stringBuilder.append(subId);
            stringBuilder.append(", ability =");
            stringBuilder.append(ability);
            logd(stringBuilder.toString());
            if (this.mChipQcom.mPrimaryStackPhoneId == subId) {
                this.mChipQcom.mPrimaryStackNetworkType = getPrimaryStackNetworkType(ability, lteOnMappingMode);
            } else {
                this.mChipQcom.mSecondaryStackNetworkType = getSecondaryStackNetworkType(ability, lteOnMappingMode);
            }
            this.mCis[subId].getPreferredNetworkType(obtainMessage(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_MODE_DONE, subId, 0));
            return;
        }
        logd("in setLteServiceAbilityForQCOM, single Ims.");
        recordPrimaryAndSecondaryStackNetworkType(ability, lteOnMappingMode);
        this.mCis[this.mChipQcom.mPrimaryStackPhoneId].getPreferredNetworkType(obtainMessage(HwFullNetworkConstants.EVENT_SET_LTE_SERVICE_ABILITY, this.mChipQcom.mPrimaryStackPhoneId, 0));
    }

    private void getStackPhoneId() {
        int i = 0;
        this.mChipQcom.mPrimaryStackPhoneId = SystemProperties.getInt("persist.radio.msim.stackid_0", 0);
        HwFullNetworkChipQcom hwFullNetworkChipQcom = this.mChipQcom;
        if (this.mChipQcom.mPrimaryStackPhoneId == 0) {
            i = 1;
        }
        hwFullNetworkChipQcom.mSecondaryStackPhoneId = i;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getStackPhoneId mPrimaryStackPhoneId:");
        stringBuilder.append(this.mChipQcom.mPrimaryStackPhoneId);
        stringBuilder.append(", mSecondaryStackPhoneId:");
        stringBuilder.append(this.mChipQcom.mSecondaryStackPhoneId);
        logd(stringBuilder.toString());
    }

    private int getPrimaryStackNetworkType(int ability, int lteOnMappingMode) {
        int primaryLteOnNetworkType = lteOnMappingMode;
        int primaryLteOffNetworkType = HwNetworkTypeUtils.getOffModeFromMapping(lteOnMappingMode);
        if (-1 == lteOnMappingMode) {
            primaryLteOnNetworkType = 22;
            primaryLteOffNetworkType = 21;
        }
        return ability == 1 ? primaryLteOnNetworkType : primaryLteOffNetworkType;
    }

    private int getSecondaryStackNetworkType(int ability, int lteOnMappingMode) {
        int secondaryNetworkType;
        if (ability == 1) {
            secondaryNetworkType = HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK ? lteOnMappingMode : 20;
            if (lteOnMappingMode == 9 || lteOnMappingMode == 10) {
                return 9;
            }
            return secondaryNetworkType;
        }
        int offModeFromMapping;
        if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK) {
            offModeFromMapping = HwNetworkTypeUtils.getOffModeFromMapping(lteOnMappingMode);
        } else {
            offModeFromMapping = 18;
        }
        secondaryNetworkType = offModeFromMapping;
        if (lteOnMappingMode == 9 || lteOnMappingMode == 10) {
            return 3;
        }
        return secondaryNetworkType;
    }

    private void recordPrimaryAndSecondaryStackNetworkType(int ability, int lteOnMappingMode) {
        this.mChipQcom.mPrimaryStackNetworkType = getPrimaryStackNetworkType(ability, lteOnMappingMode);
        this.mChipQcom.mSecondaryStackNetworkType = getSecondaryStackNetworkType(ability, lteOnMappingMode);
        int otherSub = 0;
        if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK && !this.mChipCommon.isDualImsSwitchOpened()) {
            if (this.mChipQcom.mPrimaryStackPhoneId == this.mChipCommon.getUserSwitchDualCardSlots()) {
                this.mChipQcom.mSecondaryStackNetworkType = getSecondaryStackNetworkType(0, lteOnMappingMode);
            } else {
                this.mChipQcom.mPrimaryStackNetworkType = getPrimaryStackNetworkType(0, lteOnMappingMode);
            }
        }
        boolean isCmccHybird = HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && this.mChipCommon.isCmccHybirdBySubCarrierType();
        boolean isRoaming = TelephonyManager.getDefault().isNetworkRoaming(this.mChipCommon.getCMCCCardSlotId());
        if (isCmccHybird && !isRoaming) {
            recordNetworkTypeForCmccHybird(ability);
            if (this.mChipCommon.default4GSlot == 0) {
                otherSub = 1;
            }
            PhoneFactory.getPhone(otherSub).setOOSFlagOnSelectNetworkManually(true);
            if (hasMessages(HwFullNetworkConstants.EVENT_RESET_OOS_FLAG)) {
                removeMessages(HwFullNetworkConstants.EVENT_RESET_OOS_FLAG);
            }
            Message msg = obtainMessage(HwFullNetworkConstants.EVENT_RESET_OOS_FLAG, Integer.valueOf(otherSub));
            AsyncResult.forMessage(msg, null, null);
            sendMessageDelayed(msg, 20000);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("recordPrimaryAndSecondaryStackNetworkType mPrimaryStackNetworkType:");
        stringBuilder.append(this.mChipQcom.mPrimaryStackNetworkType);
        stringBuilder.append(",mSecondaryStackNetworkType:");
        stringBuilder.append(this.mChipQcom.mSecondaryStackNetworkType);
        logd(stringBuilder.toString());
    }

    private void recordNetworkTypeForCmccHybird(int ability) {
        int otherNetworkType = ability == 1 ? 22 : 21;
        if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK) {
            otherNetworkType = 21;
        }
        this.mChipCommon.default4GSlot = this.mChipCommon.getUserSwitchDualCardSlots();
        if (this.mChipQcom.mPrimaryStackPhoneId == this.mChipCommon.default4GSlot) {
            this.mChipQcom.mSecondaryStackNetworkType = otherNetworkType;
        } else {
            this.mChipQcom.mPrimaryStackNetworkType = otherNetworkType;
        }
    }

    private void setNetworkType(AsyncResult ar, int subId) {
        int modemNetworkMode = -1;
        if (ar != null && ar.exception == null) {
            modemNetworkMode = ((int[]) ar.result)[0];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subId = ");
        stringBuilder.append(subId);
        stringBuilder.append(" modemNetworkMode = ");
        stringBuilder.append(modemNetworkMode);
        logd(stringBuilder.toString());
        Message response;
        StringBuilder stringBuilder2;
        if (this.mChipQcom.mPrimaryStackPhoneId == subId) {
            response = obtainMessage(HwFullNetworkConstants.EVENT_SET_PRIMARY_STACK_LTE_SWITCH_DONE, this.mChipQcom.mPrimaryStackNetworkType, 0, Integer.valueOf(this.mChipQcom.mPrimaryStackPhoneId));
            if (modemNetworkMode == -1 || modemNetworkMode != this.mChipQcom.mPrimaryStackNetworkType) {
                this.mCis[this.mChipQcom.mPrimaryStackPhoneId].setPreferredNetworkType(this.mChipQcom.mPrimaryStackNetworkType, response);
                return;
            }
            AsyncResult.forMessage(response);
            response.sendToTarget();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("The sub");
            stringBuilder2.append(subId);
            stringBuilder2.append(" pref network mode is same with modem's, don't set again");
            logd(stringBuilder2.toString());
        } else if (this.mChipQcom.mSecondaryStackPhoneId == subId) {
            response = obtainMessage(HwFullNetworkConstants.EVENT_SET_SECONDARY_STACK_LTE_SWITCH_DONE, this.mChipQcom.mSecondaryStackNetworkType, 0, Integer.valueOf(this.mChipQcom.mSecondaryStackPhoneId));
            if (modemNetworkMode == -1 || modemNetworkMode != this.mChipQcom.mSecondaryStackNetworkType) {
                this.mCis[this.mChipQcom.mSecondaryStackPhoneId].setPreferredNetworkType(this.mChipQcom.mSecondaryStackNetworkType, response);
                return;
            }
            AsyncResult.forMessage(response);
            response.sendToTarget();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("The sub");
            stringBuilder2.append(subId);
            stringBuilder2.append(" pref network mode is same with modem's, don't set again");
            logd(stringBuilder2.toString());
        }
    }

    private void handleSetPrimaryStackLteSwitchDone(AsyncResult ar, int subId) {
        logd("in handleSetPrimaryStackLteSwitchDone");
        if (ar == null || ar.exception != null) {
            loge("set prefer network mode failed!");
            if (HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED) {
                sendLteServiceSwitchResult(this.mChipQcom.mPrimaryStackPhoneId, false);
            } else {
                sendLteServiceSwitchResult(this.mChipCommon.getUserSwitchDualCardSlots(), false);
            }
            return;
        }
        this.mChipQcom.mSetPrimaryStackPrefMode = subId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPrimaryStackPrefMode = ");
        stringBuilder.append(this.mChipQcom.mSetPrimaryStackPrefMode);
        logd(stringBuilder.toString());
        if (!HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED || (HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED && !this.mChipCommon.isDualImsSwitchOpened())) {
            this.mCis[this.mChipQcom.mSecondaryStackPhoneId].getPreferredNetworkType(obtainMessage(HwFullNetworkConstants.EVENT_SET_LTE_SERVICE_ABILITY, this.mChipQcom.mSecondaryStackPhoneId, 0));
        } else {
            saveNetworkTypeToDB(this.mChipQcom.mPrimaryStackPhoneId);
            logd("set prefer network mode success!");
            sendLteServiceSwitchResult(this.mChipQcom.mPrimaryStackPhoneId, true);
        }
    }

    private void sendLteServiceSwitchResult(int subId, boolean result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LTE service Switch result is ");
        stringBuilder.append(result);
        stringBuilder.append(". broadcast PREFERRED_4G_SWITCH_DONE");
        logd(stringBuilder.toString());
        if (this.mContext == null) {
            loge("Context is null, return!");
            return;
        }
        Intent intent = new Intent("com.huawei.telephony.PREF_4G_SWITCH_DONE");
        intent.putExtra("subscription", subId);
        intent.putExtra("setting_result", result);
        this.mContext.sendOrderedBroadcast(intent, "android.permission.READ_PHONE_STATE");
    }

    private void handleSetSecondaryStackLteSwitchDone(AsyncResult ar, int subId) {
        if (ar == null || ar.exception != null) {
            loge("set prefer network mode failed!");
            if (HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED) {
                sendLteServiceSwitchResult(subId, false);
            } else {
                sendLteServiceSwitchResult(this.mChipCommon.getUserSwitchDualCardSlots(), false);
                rollbackPrimaryStackPrefNetworkType();
            }
            return;
        }
        this.mChipQcom.mSetSecondaryStackPrefMode = subId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set prefer network mode success! setSecondaryStackPrefMode = ");
        stringBuilder.append(this.mChipQcom.mSetSecondaryStackPrefMode);
        logd(stringBuilder.toString());
        if (!HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED || (HwFullNetworkConfig.IS_DUAL_4G_SUPPORTED && !this.mChipCommon.isDualImsSwitchOpened())) {
            saveNetworkTypeToDB();
            sendLteServiceSwitchResult(this.mChipCommon.getUserSwitchDualCardSlots(), true);
        } else {
            saveNetworkTypeToDB(subId);
            sendLteServiceSwitchResult(subId, true);
        }
    }

    private void saveNetworkTypeToDB(int subId) {
        int curPrefMode = this.mChipQcom.getNetworkTypeFromDB(subId);
        StringBuilder stringBuilder;
        if (subId == this.mChipQcom.mPrimaryStackPhoneId) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("curPrefMode = ");
            stringBuilder.append(curPrefMode);
            stringBuilder.append(", mSetPrimaryStackPrefMode =");
            stringBuilder.append(this.mChipQcom.mSetPrimaryStackPrefMode);
            logd(stringBuilder.toString());
            if (curPrefMode != this.mChipQcom.mSetPrimaryStackPrefMode && this.mChipQcom.mSetPrimaryStackPrefMode != -1) {
                this.mChipQcom.setNetworkTypeToDB(subId, this.mChipQcom.mSetPrimaryStackPrefMode);
                return;
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("curPrefMode = ");
        stringBuilder.append(curPrefMode);
        stringBuilder.append(", mSetSecondaryStackPrefMode =");
        stringBuilder.append(this.mChipQcom.mSetSecondaryStackPrefMode);
        logd(stringBuilder.toString());
        if (curPrefMode != this.mChipQcom.mSetSecondaryStackPrefMode && this.mChipQcom.mSetSecondaryStackPrefMode != -1) {
            this.mChipQcom.setNetworkTypeToDB(subId, this.mChipQcom.mSetSecondaryStackPrefMode);
        }
    }

    private void saveNetworkTypeToDB() {
        saveNetworkTypeToDB(this.mChipQcom.mPrimaryStackPhoneId);
        saveNetworkTypeToDB(this.mChipQcom.mSecondaryStackPhoneId);
    }

    private void rollbackPrimaryStackPrefNetworkType() {
        int curPrefMode = this.mChipQcom.getNetworkTypeFromDB(this.mChipQcom.mPrimaryStackPhoneId);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("rollbackPrimaryStackPrefNetworkType, curPrefMode = ");
        stringBuilder.append(curPrefMode);
        stringBuilder.append(", mSetPrimaryStackPrefMode =");
        stringBuilder.append(this.mChipQcom.mSetPrimaryStackPrefMode);
        logd(stringBuilder.toString());
        if (curPrefMode != this.mChipQcom.mSetPrimaryStackPrefMode) {
            this.mCis[this.mChipQcom.mPrimaryStackPhoneId].setPreferredNetworkType(curPrefMode, obtainMessage(HwFullNetworkConstants.EVENT_SET_PRIMARY_STACK_ROLL_BACK_DONE, curPrefMode, 0, Integer.valueOf(this.mChipQcom.mPrimaryStackPhoneId)));
        }
    }

    private void handleRollbackDone(AsyncResult ar) {
        logd("in rollbackDone");
        if (ar == null || ar.exception != null) {
            loge("set prefer network mode failed!");
        }
    }

    protected void onServiceStateChangedForCMCC(Intent intent) {
        int cmccSlotId = this.mChipCommon.getCMCCCardSlotId();
        if (HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && cmccSlotId != -1) {
            int slotId = intent.getIntExtra("subscription", -1);
            ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
            if (slotId == cmccSlotId && serviceState.getState() == 0) {
                boolean newRoamingState = TelephonyManager.getDefault().isNetworkRoaming(cmccSlotId);
                boolean oldRoamingState = this.mChipCommon.getLastRoamingStateFromSP();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mPhoneStateListener cmcccSlotId = ");
                stringBuilder.append(cmccSlotId);
                stringBuilder.append(" oldRoamingState=");
                stringBuilder.append(oldRoamingState);
                stringBuilder.append(" newRoamingState=");
                stringBuilder.append(newRoamingState);
                logd(stringBuilder.toString());
                if (oldRoamingState != newRoamingState) {
                    this.mChipCommon.saveLastRoamingStateToSP(newRoamingState);
                    if (this.mChipCommon.needForceSetDefaultSlot(newRoamingState, cmccSlotId)) {
                        forceSetDefault4GSlotForCMCC(cmccSlotId);
                        return;
                    } else {
                        this.mChipQcom.mNeedSetLteServiceAbility = true;
                        this.mChipQcom.setLteServiceAbility();
                    }
                }
            }
            if (slotId == cmccSlotId) {
                if (this.mChipCommon.mCmccSubIdOldState != 0 && serviceState.getState() == 0) {
                    logd("OUT_OF_SERVICE -> IN_SERVICE, setPrefNW");
                    this.mChipQcom.mNeedSetLteServiceAbility = true;
                    this.mChipQcom.setLteServiceAbility();
                }
                this.mChipCommon.mCmccSubIdOldState = serviceState.getState();
            }
        }
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
