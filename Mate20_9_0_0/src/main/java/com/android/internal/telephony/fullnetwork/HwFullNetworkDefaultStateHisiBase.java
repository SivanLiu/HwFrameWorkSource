package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.HwDsdsController;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.vsim.HwVSimUtils;

public abstract class HwFullNetworkDefaultStateHisiBase extends HwFullNetworkDefaultStateBase {
    protected HwFullNetworkChipHisi mChipHisi = HwFullNetworkChipHisi.getInstance();

    public HwFullNetworkDefaultStateHisiBase(Context c, CommandsInterface[] ci, Handler h) {
        super(c, ci, h);
        logd("HwFullNetworkDefaultStateHisiBase constructor");
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.obj == null) {
            loge("msg or msg.obj is null, return!");
            return;
        }
        Integer index = this.mChipCommon.getCiIndex(msg);
        if (index.intValue() < 0 || index.intValue() >= this.mCis.length) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid index : ");
            stringBuilder.append(index);
            stringBuilder.append(" received with event ");
            stringBuilder.append(msg.what);
            loge(stringBuilder.toString());
            return;
        }
        AsyncResult ar = msg.obj;
        int i = msg.what;
        StringBuilder stringBuilder2;
        if (i == HwFullNetworkConstants.EVENT_ICC_GET_ATR_DONE) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Received EVENT_ICC_GET_ATR_DONE on index ");
            stringBuilder2.append(index);
            logd(stringBuilder2.toString());
            if (ar != null && ar.exception == null) {
                this.mChipHisi.handleIccATR((String) ar.result, index);
            }
        } else if (i == HwFullNetworkConstants.EVENT_GET_CDMA_MODE_SIDE_DONE) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Received EVENT_GET_CDMA_MODE_SIDE_DONE on index ");
            stringBuilder2.append(index);
            logd(stringBuilder2.toString());
            this.mChipHisi.onGetCdmaModeSideDone(ar, index);
        } else if (i != HwFullNetworkConstants.EVENT_CMCC_SET_NETWOR_DONE) {
            super.handleMessage(msg);
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("EVENT_CMCC_SET_NETWOR_DONE reveived for slot: ");
            stringBuilder2.append(msg.arg1);
            logd(stringBuilder2.toString());
            this.mChipHisi.handleSetCmccPrefNetwork(msg);
        }
    }

    protected void onRadioUnavailable(Integer index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRadioUnavailable, index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        this.mCis[index.intValue()].iccGetATR(obtainMessage(HwFullNetworkConstants.EVENT_ICC_GET_ATR_DONE, index));
        this.mChipHisi.mRadioOns[index.intValue()] = false;
        this.mChipHisi.mSwitchTypes[index.intValue()] = -1;
        this.mChipHisi.mGetUiccCardsStatusDone[index.intValue()] = false;
        this.mChipHisi.mGetBalongSimSlotDone[index.intValue()] = false;
        this.mChipHisi.mCardTypes[index.intValue()] = -1;
        this.mChipCommon.mIccIds[index.intValue()] = null;
        this.mChipHisi.mNvRestartRildDone = true;
        this.mChipHisi.mAllCardsReady = false;
    }

    protected void onRadioAvailable(Integer index) {
        int i;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRadioAvailable, index ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        super.onRadioAvailable(index);
        boolean ready = true;
        if (HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT && index.intValue() == this.mChipCommon.getUserSwitchDualCardSlots()) {
            this.mCis[index.intValue()].getCdmaModeSide(obtainMessage(HwFullNetworkConstants.EVENT_GET_CDMA_MODE_SIDE_DONE, index));
        }
        this.mChipHisi.mRadioOns[index.intValue()] = true;
        for (i = 0; i < HwFullNetworkConstants.SIM_NUM; i++) {
            if (!this.mChipHisi.mRadioOns[i]) {
                ready = false;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mRadioOns is ");
                stringBuilder2.append(this.mChipHisi.mRadioOns[i]);
                logd(stringBuilder2.toString());
                break;
            }
        }
        if (ready) {
            if (!(!HwFullNetworkConfig.IS_HISI_DSDX || HwVSimUtils.isVSimEnabled() || HwVSimUtils.isVSimCauseCardReload() || HwVSimUtils.isSubActivationUpdate())) {
                i = SubscriptionManager.getDefaultDataSubscriptionId();
                int curr4GSlot = this.mChipCommon.getUserSwitchDualCardSlots();
                if (i != curr4GSlot && this.mChipCommon.mSet4GSlotCompleteMsg == null) {
                    SubscriptionController.getInstance().setDefaultDataSubId(curr4GSlot);
                    logd("EVENT_RADIO_AVAILABLE set default data sub to 4G slot");
                }
                if ((this.mChipHisi.mNvRestartRildDone || this.mChipHisi.mSetSdcsCompleteMsg != null) && PhoneFactory.onDataSubChange(0, null) == 0) {
                    for (int i2 = 0; i2 < HwFullNetworkConstants.SIM_NUM; i2++) {
                        PhoneFactory.resendDataAllowed(i2);
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("EVENT_RADIO_AVAILABLE resend data allow with slot ");
                        stringBuilder3.append(i2);
                        logd(stringBuilder3.toString());
                    }
                }
            }
            logd("EVENT_RADIO_AVAILABLE set isSet4GSlotInProgress to false");
            this.mChipHisi.setWaitingSwitchBalongSlot(false);
            setDsdsCfgDone(false);
            this.mChipHisi.mNvRestartRildDone = false;
            if (!(HwFullNetworkConfig.IS_FAST_SWITCH_SIMSLOT || this.mChipCommon.mSet4GSlotCompleteMsg == null)) {
                AsyncResult.forMessage(this.mChipCommon.mSet4GSlotCompleteMsg, Boolean.valueOf(true), null);
                logd("Sending the mSet4GSlotCompleteMsg back!");
                this.mChipCommon.sendResponseToTarget(this.mChipCommon.mSet4GSlotCompleteMsg, 0);
                this.mChipCommon.mSet4GSlotCompleteMsg = null;
            }
            if (this.mChipHisi.mSetSdcsCompleteMsg != null) {
                AsyncResult.forMessage(this.mChipHisi.mSetSdcsCompleteMsg, Boolean.valueOf(true), null);
                logd("Sending the mSetSdcsCompleteMsg back!");
                this.mChipHisi.mSetSdcsCompleteMsg.sendToTarget();
                this.mChipHisi.mSetSdcsCompleteMsg = null;
            }
            return;
        }
        logd("clean iccids!!");
        PhoneFactory.getSubInfoRecordUpdater().cleanIccids();
    }

    private void setDsdsCfgDone(boolean isDone) {
        if (!HwDsdsController.IS_DSDSPOWER_SUPPORT) {
            return;
        }
        if (HwVSimUtils.isVSimInProcess()) {
            logd("setDsdsCfgDone, vsim in process, do nothing");
        } else {
            HwDsdsController.getInstance().setDsdsCfgDone(isDone);
        }
    }

    protected void processPreBootCompleted() {
        logd("processPreBootCompleted");
        this.mChipHisi.isPreBootCompleted = true;
        this.mChipHisi.mAutoSwitchDualCardsSlotDone = false;
        this.mStateHandler.sendMessage(this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT, Integer.valueOf(0)));
    }

    protected void setMainSlot(int slotId, Message responseMsg) {
        if (!SystemProperties.getBoolean("persist.sys.dualcards", false)) {
            loge("setMainSlot: main slot switch disabled, return failure");
            this.mChipCommon.sendResponseToTarget(responseMsg, 2);
        } else if (this.mChipCommon.isValidIndex(slotId)) {
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
                this.mChipHisi.refreshCardState();
                this.mStateHandler.obtainMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT, slotId, 0, responseMsg).sendToTarget();
            }
        } else {
            loge("setDefault4GSlot: invalid slotid, return failure");
            this.mChipCommon.sendResponseToTarget(responseMsg, 2);
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
                    }
                    this.mChipHisi.setPrefNwForCmcc(this);
                }
            }
            if (slotId == cmccSlotId) {
                if (this.mChipCommon.mCmccSubIdOldState != 0 && serviceState.getState() == 0) {
                    logd("OUT_OF_SERVICE -> IN_SERVICE, setPrefNW");
                    this.mChipHisi.setPrefNwForCmcc(this);
                }
                this.mChipCommon.mCmccSubIdOldState = serviceState.getState();
            }
        }
    }

    protected void setLteServiceAbilityForQCOM(int subId, int ability, int lteOnMappingMode) {
    }
}
