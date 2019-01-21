package com.android.internal.telephony.fullnetwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandsInterface.RadioState;
import com.android.internal.telephony.fullnetwork.HwFullNetworkConstants.SubCarrierType;

public class HwFullNetworkInitStateQcom2_0 extends HwFullNetworkInitStateBase {
    private static final String LOG_TAG = "HwFullNetworkInitStateQcom2_0";
    protected HwFullNetworkChipQcom mChipQcom;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwFullNetworkInitStateQcom2_0.this.loge("intent is null, return");
                return;
            }
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(action)) {
                HwFullNetworkInitStateQcom2_0.this.processSubInfoRecordUpdated(intent);
            } else if ("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE".equals(action)) {
                HwFullNetworkInitStateQcom2_0.this.processSubStateChanged(intent);
            } else if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                HwFullNetworkInitStateQcom2_0.this.processSimStateChanged(intent);
            }
        }
    };

    public HwFullNetworkInitStateQcom2_0(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        logd("HwFullNetworkInitStateQcom2_0 constructor");
        this.mChipQcom = HwFullNetworkChipQcom.getInstance();
        initParams();
        IntentFilter filter = new IntentFilter("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        filter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    private void initParams() {
        for (int i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            mChipCommon.subCarrierTypeArray[i] = SubCarrierType.OTHER;
            this.mChipQcom.mSetUiccSubscriptionResult[i] = -1;
        }
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.obj == null) {
            loge("msg or msg.obj is null, return!");
            return;
        }
        Integer index = mChipCommon.getCiIndex(msg);
        StringBuilder stringBuilder;
        if (mChipCommon.isValidIndex(index.intValue())) {
            if (msg.what != HwFullNetworkConstants.EVENT_RADIO_ON_PROCESS_SIM_STATE) {
                super.handleMessage(msg);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("EVENT_RADIO_ON_PROCESS_SIM_STATE  on index ");
                stringBuilder.append(index);
                logd(stringBuilder.toString());
                this.mCis[index.intValue()].unregisterForOn(this);
                processSimStateChanged("IMSI", index.intValue());
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid index : ");
        stringBuilder.append(index);
        stringBuilder.append(" received with event ");
        stringBuilder.append(msg.what);
        loge(stringBuilder.toString());
    }

    public void processSubInfoRecordUpdated(Intent intent) {
        int status = intent.getIntExtra("simDetectStatus", -1);
        if (status != -1) {
            boolean z = true;
            if (4 != status) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processSubInfoRecordUpdated, cards in the slots are changed with status: ");
                stringBuilder.append(status);
                logd(stringBuilder.toString());
                this.mChipQcom.mNeedSetLteServiceAbility = true;
                this.mChipQcom.refreshCardState();
                mChipCommon.judgeSubCarrierType();
                this.mChipQcom.is4GSlotReviewNeeded = 1;
                this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, status, 0, "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED").sendToTarget();
            } else {
                if (!(HwFullNetworkConfig.IS_FULL_NETWORK_SUPPORTED || HwFullNetworkConfig.IS_CMCC_CU_DSDX_ENABLE || HwFullNetworkConfig.IS_CMCC_4G_DSDX_ENABLE)) {
                    z = false;
                }
                if (z) {
                    if (mChipCommon.isSet4GSlotInProgress) {
                        logd("processSubInfoRecordUpdated: setting lte slot is in progress, ignore this event");
                        return;
                    }
                    logd("processSubInfoRecordUpdated EXTRA_VALUE_NOCHANGE check!");
                    this.mChipQcom.refreshCardState();
                    mChipCommon.judgeSubCarrierType();
                    this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, status, 0, "android.intent.action.ACTION_SUBINFO_RECORD_UPDATED").sendToTarget();
                }
            }
        }
    }

    public void processSubStateChanged(Intent intent) {
        int slotId = intent.getIntExtra("subscription", -1000);
        int subState = intent.getIntExtra("intContent", 0);
        if ("sub_state".equals(intent.getStringExtra("columnName")) && mChipCommon.isValidIndex(slotId)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processSubStateChanged: slot Id = ");
            stringBuilder.append(slotId);
            stringBuilder.append(", subState = ");
            stringBuilder.append(subState);
            logd(stringBuilder.toString());
            if (mChipCommon.isSet4GSlotInProgress) {
                logd("processSubStateChanged: set lte slot is in progress, ignore this event");
                return;
            }
            boolean oldSimCardTypeIsCMCCCard = mChipCommon.subCarrierTypeArray[slotId].isCMCCCard();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("processSubStateChanged: oldSubCarrierType = ");
            stringBuilder2.append(mChipCommon.subCarrierTypeArray[slotId]);
            logd(stringBuilder2.toString());
            this.mChipQcom.refreshCardState();
            mChipCommon.judgeSubCarrierType();
            this.mChipQcom.is4GSlotReviewNeeded = 1;
            if (HwFullNetworkConfig.IS_CMCC_4GSWITCH_DISABLE && (mChipCommon.subCarrierTypeArray[slotId].isCMCCCard() || (subState != 1 && oldSimCardTypeIsCMCCCard))) {
                this.mChipQcom.mNeedSetLteServiceAbility = true;
            }
            this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, subState, 0, "android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE").sendToTarget();
        }
    }

    public void processSimStateChanged(Intent intent) {
        String simState = intent.getStringExtra("ss");
        int slotId = intent.getIntExtra("slot", -1000);
        if ("IMSI".equals(simState) && mChipCommon.isValidIndex(slotId)) {
            processSimStateChanged(simState, slotId);
        }
    }

    private void processSimStateChanged(String simState, int slotId) {
        boolean isSubCarrierTypeChanged = true;
        if (1 == this.mChipQcom.is4GSlotReviewNeeded || 2 == this.mChipQcom.is4GSlotReviewNeeded) {
            StringBuilder stringBuilder;
            if (RadioState.RADIO_ON != this.mCis[slotId].getRadioState()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("processSimStateChanged radioState =");
                stringBuilder.append(this.mCis[slotId].getRadioState());
                logd(stringBuilder.toString());
                this.mCis[slotId].registerForOn(this, HwFullNetworkConstants.EVENT_RADIO_ON_PROCESS_SIM_STATE, Integer.valueOf(slotId));
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("processSimStateChanged: check if update main card for slot ");
            stringBuilder.append(slotId);
            logd(stringBuilder.toString());
            SubCarrierType oldSubCarrierType = mChipCommon.subCarrierTypeArray[slotId];
            this.mChipQcom.refreshCardState();
            mChipCommon.judgeSubCarrierType();
            mChipCommon.judgeSubCarrierTypeByMccMnc(slotId);
            if (oldSubCarrierType == mChipCommon.subCarrierTypeArray[slotId] || mChipCommon.subCarrierTypeArray[slotId].isReCheckFail()) {
                isSubCarrierTypeChanged = false;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("processSimStateChanged:oldSubCarrierType is ");
            stringBuilder2.append(oldSubCarrierType);
            stringBuilder2.append(", newSubCarrierType is ");
            stringBuilder2.append(mChipCommon.subCarrierTypeArray[slotId]);
            logd(stringBuilder2.toString());
            if (isSubCarrierTypeChanged) {
                this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, "android.intent.action.SIM_STATE_CHANGED").sendToTarget();
            } else {
                logd("processSimStateChanged: no need to update main card!");
                if (2 == this.mChipQcom.is4GSlotReviewNeeded && !mChipCommon.isSet4GSlotInProgress) {
                    this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_NETWORK_TYPE).sendToTarget();
                }
            }
        }
    }

    public void onGetIccCardStatusDone(AsyncResult ar, Integer index) {
    }

    protected void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    protected void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }
}
