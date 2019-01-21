package com.android.internal.telephony.vsim.process;

import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimController;
import com.android.internal.telephony.vsim.HwVSimController.EnableParam;
import com.android.internal.telephony.vsim.HwVSimEventReport.VSimEventInfoUtils;
import com.android.internal.telephony.vsim.HwVSimLog;
import com.android.internal.telephony.vsim.HwVSimModemAdapter;
import com.android.internal.telephony.vsim.HwVSimRequest;
import com.android.internal.telephony.vsim.HwVSimSlotSwitchController.CommrilMode;
import com.android.internal.telephony.vsim.HwVSimUtilsInner;

public class HwVSimEWorkProcessor extends HwVSimWorkProcessor {
    public static final String LOG_TAG = "VSimEWorkProcessor";
    private Message mMessage;

    public static HwVSimEWorkProcessor create(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        if (controller == null || !controller.isDirectProcess()) {
            return new HwVSimEWorkProcessor(controller, modemAdapter, request);
        }
        return new HwVSimEDWorkProcessor(controller, modemAdapter, request);
    }

    public HwVSimEWorkProcessor(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        super(controller, modemAdapter, request);
    }

    public void onEnter() {
        super.onEnter();
        if (!this.mVSimController.isDirectProcess() && HwVSimUtilsInner.isPlatformNeedWaitNvMatchUnsol()) {
            this.mVSimController.setIsWaitingNvMatchUnsol(true);
        }
    }

    public boolean processMessage(Message msg) {
        boolean retVal = true;
        if (!isMessageShouldDeal(msg)) {
            return false;
        }
        int i = msg.what;
        if (i == 2) {
            onGetSimStateDone(msg);
        } else if (i == 49) {
            onSetPreferredNetworkTypeDone(msg);
        } else if (i != 51) {
            switch (i) {
                case 41:
                    onRadioPowerOffDone(msg);
                    break;
                case 42:
                    onCardPowerOffDone(msg);
                    break;
                case 43:
                    onSwitchSlotDone(msg);
                    break;
                default:
                    switch (i) {
                        case HwVSimConstants.EVENT_CARD_POWER_ON_DONE /*45*/:
                            onCardPowerOnDone(msg);
                            break;
                        case HwVSimConstants.EVENT_RADIO_POWER_ON_DONE /*46*/:
                            onRadioPowerOnDone(msg);
                            break;
                        case HwVSimConstants.EVENT_SET_ACTIVE_MODEM_MODE_DONE /*47*/:
                            onSetActiveModemModeDone(msg);
                            break;
                        default:
                            switch (i) {
                                case HwVSimConstants.EVENT_GET_ICC_STATUS_DONE /*79*/:
                                    onGetIccCardStatusDone(msg);
                                    break;
                                case HwVSimConstants.EVENT_SET_CDMA_MODE_SIDE_DONE /*80*/:
                                    onSetCdmaModeSideDone(msg);
                                    break;
                                case HwVSimConstants.EVENT_JUDGE_RESTART_RILD_NV_MATCH /*81*/:
                                    onJudgeRestartRildNvMatch(msg);
                                    break;
                                case HwVSimConstants.EVENT_JUDGE_RESTART_RILD_NV_MATCH_TIMEOUT /*82*/:
                                    onJudgeRestartRildNvMatchTimeout();
                                    break;
                                case HwVSimConstants.EVENT_RADIO_AVAILABLE /*83*/:
                                    onRadioAvailable(msg);
                                    break;
                                default:
                                    retVal = false;
                                    break;
                            }
                    }
            }
        } else {
            onEnableVSimDone(msg);
        }
        return retVal;
    }

    public void doProcessException(AsyncResult ar, HwVSimRequest request) {
        doEnableProcessException(ar, request, Integer.valueOf(3));
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    protected void onSwitchSlotDone(Message msg) {
        logd("onSwitchSlotDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 5);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            this.mModemAdapter.onSwitchSlotDone(this, ar);
            this.mVSimController.clearAllMarkForCardReload();
            this.mVSimController.setBlockPinFlag(false);
            HwVSimRequest request = ar.userObj;
            if (HwVSimModemAdapter.IS_FAST_SWITCH_SIMSLOT && request.getIsNeedSwitchCommrilMode()) {
                setCdmaModeSide(msg);
            } else {
                cardPowerOnModem1orWriteVSim(msg);
            }
        }
    }

    protected void onSetCdmaModeSideDone(Message msg) {
        logd("onSetCdmaModeSideDone");
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            HwVSimRequest request = ar.userObj;
            HwVSimController.getInstance().setIsWaitingSwitchCdmaModeSide(false);
            CommrilMode expectCommrilMode = request.getExpectCommrilMode();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSetCdmaModeSideDone, expectCommrilMode = ");
            stringBuilder.append(expectCommrilMode);
            logd(stringBuilder.toString());
            SystemProperties.set("persist.radio.commril_mode", expectCommrilMode.toString());
            cardPowerOnModem1orWriteVSim(msg);
        }
    }

    private void cardPowerOnModem1orWriteVSim(Message msg) {
        if (isNeedWaitNvCfgMatchAndRestartRild()) {
            logd("cardPowerOnModem1orWriteVSim");
            this.mMessage = Message.obtain(msg);
            cardPowerOnModem1andWaitForNvCfgMatch(msg);
            return;
        }
        writeVsim(msg);
    }

    private void cardPowerOnModem1andWaitForNvCfgMatch(Message msg) {
        logd("cardPowerOnModem1andWaitForNvCfgMatch");
        HwVSimRequest request = msg.obj.userObj;
        int subId = request.getMainSlot();
        request.setSource(2);
        this.mModemAdapter.cardPowerOn(this, request, subId, 1);
        subId = request.getExpectSlot();
        this.mModemAdapter.cardPowerOn(this, request.clone(), subId, 1);
        startListenForRildNvMatch();
    }

    private void writeVsim(Message msg) {
        AsyncResult ar = msg.obj;
        HwVSimRequest request = ar.userObj;
        int subId = request.mSubId;
        if (isSwapProcess()) {
            int mainSlot = request.getMainSlot();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mainSlot = ");
            stringBuilder.append(mainSlot);
            logd(stringBuilder.toString());
        } else if (isCrossProcess()) {
            request.setMainSlot(request.getExpectSlot());
        }
        EnableParam arg = request.getArgument();
        EnableParam param = null;
        if (arg != null) {
            param = arg;
        }
        if (param == null) {
            doProcessException(ar, request);
            return;
        }
        int result = this.mVSimController.writeVsimToTA(param.imsi, param.cardType, param.apnType, param.challenge, param.taPath, param.vsimLoc, 0);
        if (result != 0) {
            doEnableProcessException(ar, request, Integer.valueOf(result));
            return;
        }
        this.mVSimController.setVSimCurCardType(this.mVSimController.getCardTypeFromEnableParam(request));
        this.mModemAdapter.cardPowerOn(this, this.mRequest, 2, 11);
    }

    protected void onCardPowerOffDone(Message msg) {
        logd("onCardPowerOffDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 4);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidForRequestNotSupport(ar)) {
            HwVSimRequest request = ar.userObj;
            int subId = request.mSubId;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCardPowerOffDone, subId: ");
            stringBuilder.append(subId);
            logd(stringBuilder.toString());
            int subCount = request.getSubCount();
            for (int i = 0; i < subCount; i++) {
                if (subId == request.getSubIdByIndex(i)) {
                    request.setCardOnOffMark(i, false);
                }
            }
            this.mModemAdapter.onCardPowerOffDoneInEWork(this, subId);
            getIccCardStatus(request, subId);
        }
    }

    protected void onCardPowerOnDone(Message msg) {
        logd("onCardPowerOnDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 7);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidForCardPowerOn(ar)) {
            int subId = ar.userObj.mSubId;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCardPowerOnDone : subId  = ");
            stringBuilder.append(subId);
            logd(stringBuilder.toString());
            if (subId == 2) {
                this.mModemAdapter.setActiveModemMode(this, this.mRequest, 2);
            }
        }
    }

    protected void afterJudgeRestartRildNvMatch() {
        logd("afterJudgeRestartRildNvMatch");
        this.mIsWaitingRestartRild = false;
        this.mHasReceiveNvMatchUnsol = false;
        this.mVSimController.setIsWaitingNvMatchUnsol(false);
        writeVsim(this.mMessage);
    }

    protected void onSetActiveModemModeDone(Message msg) {
        logd("onSetActiveModemModeDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 8);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            setPrefNetworkForM0(ar, ar.userObj);
        }
    }

    protected void onSetPreferredNetworkTypeDone(Message msg) {
        logd("onSetPreferredNetworkTypeDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 10);
        AsyncResult ar = msg.obj;
        HwVSimRequest request = ar.userObj;
        int slotIdInModem1 = 0;
        if (isAsyncResultValid(ar)) {
            int flag = this.mVSimController.getPreferredNetworkTypeEnableFlag();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSetPreferredNetworkTypeDone, flag = ");
            stringBuilder.append(flag);
            logd(stringBuilder.toString());
            int slotIdInModem2 = request.getMainSlot();
            if (slotIdInModem2 == 0) {
                slotIdInModem1 = 1;
            }
            if (!HwVSimModemAdapter.IS_FAST_SWITCH_SIMSLOT) {
                radioPowerOnForVSim();
            } else if (flag == 0) {
                this.mModemAdapter.setPreferredNetworkType(this, request, slotIdInModem1, HwVSimUtilsInner.getNetworkTypeInModem1ForCmcc(getNetworkTypeOnModem1ForEWork()));
                this.mVSimController.setPreferredNetworkTypeEnableFlag(1);
            } else if (HwVSimUtilsInner.isPlatformRealTripple() && 1 == flag) {
                this.mModemAdapter.setPreferredNetworkType(this, request, slotIdInModem2, getNetworkTypeOnModem2());
                this.mVSimController.setPreferredNetworkTypeEnableFlag(2);
                if (HwVSimUtilsInner.isDualImsSupported()) {
                    this.mModemAdapter.saveNetworkTypeToDB(slotIdInModem1, HwVSimUtilsInner.getNetworkTypeInModem1ForCmcc(getNetworkTypeOnModem1ForEWork()));
                }
            } else {
                radioPowerOnForVSim();
                if (HwVSimUtilsInner.isPlatformRealTripple() && HwVSimUtilsInner.isDualImsSupported()) {
                    this.mModemAdapter.saveNetworkTypeToDB(slotIdInModem2, getNetworkTypeOnModem2());
                }
            }
            return;
        }
        if (HwVSimModemAdapter.IS_FAST_SWITCH_SIMSLOT && request.getIsNeedSwitchCommrilMode()) {
            this.mVSimController.setPreferredNetworkTypeEnableFlag(0);
        }
    }

    private void radioPowerOnForVSim() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("radioPowerOnForVSim : subId  = ");
        stringBuilder.append(2);
        logd(stringBuilder.toString());
        this.mVSimController.allowData(2);
        this.mModemAdapter.radioPowerOn(this, this.mRequest, 2);
        this.mVSimController.setPreferredNetworkTypeEnableFlag(0);
    }

    protected void onRadioPowerOnDone(Message msg) {
        logd("onRadioPowerOnDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 11);
        if (isAsyncResultValid(msg.obj)) {
            enableVSimDone();
        }
    }

    protected void onEnableVSimDone(Message msg) {
        logd("onEnableVSimDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 12);
        notifyResult(this.mRequest, Integer.valueOf(0));
        if (this.mVSimController.isDirectProcess()) {
            this.mModemAdapter.onEDWorkTransitionState(this);
        } else {
            transitionToState(4);
        }
    }

    protected void enableVSimDone() {
        logd("enableVSimDone");
        Message onCompleted = obtainMessage(51, this.mRequest);
        AsyncResult.forMessage(onCompleted);
        onCompleted.sendToTarget();
    }

    protected boolean isAsyncResultValidForCardPowerOn(AsyncResult ar) {
        if (ar == null) {
            doProcessException(null, null);
            return false;
        }
        HwVSimRequest request = ar.userObj;
        if (request == null) {
            return false;
        }
        if (ar.exception == null || request.mSubId != 2) {
            return true;
        }
        doEnableProcessException(ar, request, Integer.valueOf(2));
        return false;
    }

    protected void afterGetAllCardStateDone() {
        logd("afterGetAllCardStateDone: onGetIccCardStatusDone->switchSimSlot");
        this.mModemAdapter.switchSimSlot(this, this.mRequest);
    }

    private boolean isMessageShouldDeal(Message msg) {
        return super.isMessageShouldDeal(msg, 2);
    }

    private int getNetworkTypeOnModem1ForEWork() {
        return this.mModemAdapter.getAllAbilityNetworkTypeOnModem1(true);
    }

    private int getNetworkTypeOnModem2() {
        return 1;
    }
}
