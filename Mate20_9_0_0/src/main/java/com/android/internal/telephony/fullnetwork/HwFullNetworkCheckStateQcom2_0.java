package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.SubCarrierType;

public class HwFullNetworkCheckStateQcom2_0 extends HwFullNetworkCheckStateBase {
    private static final String LOG_TAG = "HwFullNetworkCheckStateQcom2_0";
    public HwFullNetworkChipQcom mChipQcom;

    public HwFullNetworkCheckStateQcom2_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        this.mChipQcom = null;
        this.mChipQcom = HwFullNetworkChipQcom.getInstance();
        logd("HwFullNetworkCheckStateQcom2_0 constructor");
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.obj == null) {
            loge("msg or msg.obj is null, return!");
            return;
        }
        AsyncResult ar = msg.obj;
        if (msg.what != HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_DONE) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown msg:");
            stringBuilder.append(msg.what);
            logd(stringBuilder.toString());
        } else {
            int subId = msg.arg1;
            HwFullNetworkChipQcom hwFullNetworkChipQcom = this.mChipQcom;
            hwFullNetworkChipQcom.mNumOfGetPrefNwModeSuccess++;
            if (ar.exception == null) {
                int modemNetworkMode = ((int[]) ar.result)[0];
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("subId = ");
                stringBuilder2.append(subId);
                stringBuilder2.append(" modemNetworkMode = ");
                stringBuilder2.append(modemNetworkMode);
                logd(stringBuilder2.toString());
                this.mChipQcom.mModemPreferMode[subId] = modemNetworkMode;
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Failed to get preferred network mode for slot");
                stringBuilder3.append(subId);
                logd(stringBuilder3.toString());
                this.mChipQcom.mModemPreferMode[subId] = -1;
            }
            if (HwFullNetworkConfig.IS_QCOM_DUAL_LTE_STACK) {
                checkNetworkTypeForSubscription(subId);
            } else if (this.mChipQcom.mNumOfGetPrefNwModeSuccess == HwFullNetworkConstants.SIM_NUM) {
                handleGetPreferredNetworkForMapping();
                this.mChipQcom.mNumOfGetPrefNwModeSuccess = 0;
            }
        }
    }

    public boolean checkIfAllCardsReady(Message msg) {
        logd("checkIfAllCardsReady!");
        if ("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(msg.obj)) {
            return processSubInfoRecordUpdated(msg.arg1);
        }
        if ("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE".equals(msg.obj)) {
            return processSubStateChanged();
        }
        if ("android.intent.action.SIM_STATE_CHANGED".equals(msg.obj)) {
            return processSimStateChanged();
        }
        return false;
    }

    public boolean processSimStateChanged() {
        logd("processSimStateChanged!");
        boolean isSetDefault4GNeed = false;
        int new4GSlotId = this.defaultMainSlot;
        if (1 == this.mChipQcom.is4GSlotReviewNeeded) {
            logd("processSimStateChanged: auto mode!");
            isSetDefault4GNeed = judgeDefaultMainSlot();
            new4GSlotId = this.defaultMainSlot;
        } else if (2 == this.mChipQcom.is4GSlotReviewNeeded) {
            logd("processSimStateChanged: fix mode!");
            this.mChipQcom.judgeNwMode(this.mChipQcom.mUserPref4GSlot);
            isSetDefault4GNeed = this.mChipQcom.isSetDefault4GSlotNeeded(this.mChipQcom.mUserPref4GSlot);
            new4GSlotId = this.mChipQcom.mUserPref4GSlot;
        }
        if (isSetDefault4GNeed) {
            this.mChipQcom.judgeNwMode(new4GSlotId);
            if (true == this.mChipCommon.isSet4GSlotInProgress) {
                logd("There is event in progress");
                return false;
            }
            this.mChipCommon.default4GSlot = new4GSlotId;
            this.mCheckStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT, new4GSlotId, 0).sendToTarget();
            return false;
        }
        logd("processSimStateChanged: there is no need to set the 4G slot");
        if (2 == this.mChipQcom.is4GSlotReviewNeeded) {
            checkNetworkType();
        }
        return false;
    }

    private boolean processSubInfoRecordUpdated(int detectedType) {
        logd("processSubInfoRecordUpdated!");
        if (4 == detectedType) {
            int userPref4GSlot = this.mChipCommon.getUserSwitchDualCardSlots();
            boolean set4GDefaltSlot = (userPref4GSlot < HwFullNetworkConstants.SIM_NUM && !this.mChipCommon.isSimInsertedArray[userPref4GSlot]) || userPref4GSlot >= HwFullNetworkConstants.SIM_NUM;
            boolean need4GCheckWhenBoot = HwFullNetworkConfig.IS_CT_4GSWITCH_DISABLE && this.mChipCommon.subCarrierTypeArray[0].isCTCard() != this.mChipCommon.subCarrierTypeArray[1].isCTCard();
            if (set4GDefaltSlot) {
                judgeDefaultMainSlot();
                userPref4GSlot = this.defaultMainSlot;
            } else if (need4GCheckWhenBoot && judgeDefaultMainSlot()) {
                userPref4GSlot = this.defaultMainSlot;
            }
            if (this.mChipCommon.judgeSubCarrierTypeByMccMnc(userPref4GSlot)) {
                this.mChipQcom.judgeNwMode(userPref4GSlot);
                if (this.mChipQcom.isSetDefault4GSlotNeeded(userPref4GSlot)) {
                    logd("setDefault4GSlot when networkmode change!");
                    return true;
                }
                for (int sub = 0; sub < HwFullNetworkConstants.SIM_NUM; sub++) {
                    if (this.mCis[sub] != null) {
                        this.mCis[sub].getPreferredNetworkType(obtainMessage(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_DONE, sub, 0));
                    }
                }
                return false;
            }
            this.mChipQcom.is4GSlotReviewNeeded = 2;
            this.mChipQcom.mUserPref4GSlot = userPref4GSlot;
            this.mChipCommon.subCarrierTypeArray[userPref4GSlot] = SubCarrierType.OTHER;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("userPref4GSlot=");
            stringBuilder.append(userPref4GSlot);
            stringBuilder.append(" SubCarrierType=");
            stringBuilder.append(this.mChipCommon.subCarrierTypeArray[userPref4GSlot]);
            stringBuilder.append(" Need to check in Sim State Change!");
            logd(stringBuilder.toString());
            return false;
        } else if (judgeDefaultMainSlot()) {
            this.mChipQcom.judgeNwMode(this.defaultMainSlot);
            if (true == this.mChipCommon.isSet4GSlotInProgress) {
                logd("There is event in progress");
                return false;
            }
            logd("Need set main slot!");
            return true;
        } else {
            logd("there is no need to set the 4G slot");
            return false;
        }
    }

    private boolean processSubStateChanged() {
        logd("processSubStateChanged!");
        if (judgeDefaultMainSlot()) {
            PhoneFactory.getSubInfoRecordUpdater().resetInsertSimState();
            this.mChipQcom.judgeNwMode(this.defaultMainSlot);
            return true;
        }
        logd("there is no need to set the 4G slot");
        return false;
    }

    public boolean judgeDefaultMainSlot() {
        if (judgeDefaultMainSlotForMDM()) {
            return true;
        }
        this.defaultMainSlot = this.mOperatorBase.getDefaultMainSlot(false);
        return this.mOperatorBase.isMainSlotFound();
    }

    public int getDefaultMainSlot() {
        return this.defaultMainSlot;
    }

    protected void checkNetworkType() {
        for (int sub = 0; sub < HwFullNetworkConstants.SIM_NUM; sub++) {
            if (this.mCis[sub] != null) {
                this.mCis[sub].getPreferredNetworkType(obtainMessage(HwFullNetworkConstants.EVENT_GET_PREF_NETWORK_DONE, sub, 0));
            }
        }
    }

    private void checkNetworkTypeForSubscription(int subId) {
        int prefNwMode = this.mChipQcom.getNetworkTypeFromDB(subId);
        if (prefNwMode != this.mChipQcom.mModemPreferMode[subId]) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("preferred network mode for sub ");
            stringBuilder.append(subId);
            stringBuilder.append(" is ");
            stringBuilder.append(prefNwMode);
            loge(stringBuilder.toString());
            HwTelephonyManagerInner hwTelephonyManager = HwTelephonyManagerInner.getDefault();
            if (hwTelephonyManager != null) {
                hwTelephonyManager.setLteServiceAbility(subId, hwTelephonyManager.getLteServiceAbility(subId));
            }
        }
    }

    private void handleGetPreferredNetworkForMapping() {
        int curr4GSlot = this.mChipCommon.getUserSwitchDualCardSlots();
        int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
        if (dataSub != curr4GSlot) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleGetPreferredNetworkForMapping dataSub = ");
            stringBuilder.append(dataSub);
            stringBuilder.append(" ;curr4GSlot = ");
            stringBuilder.append(curr4GSlot);
            logd(stringBuilder.toString());
            HwTelephonyManagerInner.getDefault().setDefaultDataSlotId(curr4GSlot);
        }
        boolean diff = false;
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            int prefNwMode = this.mChipQcom.getNetworkTypeFromDB(i);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("subid = ");
            stringBuilder2.append(i);
            stringBuilder2.append(" prefNwMode = ");
            stringBuilder2.append(prefNwMode);
            logd(stringBuilder2.toString());
            if (this.mChipQcom.mModemPreferMode[i] != prefNwMode) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("modemprefermode is not same with prefer mode in slot = ");
                stringBuilder2.append(i);
                logd(stringBuilder2.toString());
                diff = true;
                break;
            }
        }
        if (diff) {
            this.mChipQcom.mNeedSetLteServiceAbility = true;
            this.mChipQcom.setLteServiceAbility();
            return;
        }
        logd("handleGetPreferredNetworkForMapping PreferMode same");
    }

    protected boolean judgeSetDefault4GSlotForCMCC(int cmccSlotId) {
        if (cmccSlotId == this.mChipCommon.getUserSwitchDualCardSlots()) {
            return false;
        }
        this.defaultMainSlot = cmccSlotId;
        return true;
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
