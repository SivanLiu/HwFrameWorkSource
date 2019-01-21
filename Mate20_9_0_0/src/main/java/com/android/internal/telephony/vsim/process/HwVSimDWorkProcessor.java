package com.android.internal.telephony.vsim.process;

import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.HwTelephonyManagerInner;
import com.android.internal.telephony.HwVSimPhoneFactory;
import com.android.internal.telephony.fullnetwork.HwFullNetworkManager;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimController;
import com.android.internal.telephony.vsim.HwVSimEventReport.VSimEventInfoUtils;
import com.android.internal.telephony.vsim.HwVSimLog;
import com.android.internal.telephony.vsim.HwVSimModemAdapter;
import com.android.internal.telephony.vsim.HwVSimRequest;
import com.android.internal.telephony.vsim.HwVSimUtilsInner;

public class HwVSimDWorkProcessor extends HwVSimWorkProcessor {
    public static final String LOG_TAG = "VSimDWorkProcessor";

    public HwVSimDWorkProcessor(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        super(controller, modemAdapter, request);
    }

    public boolean processMessage(Message msg) {
        int i = msg.what;
        if (i == 2) {
            onGetSimStateDone(msg);
            return true;
        } else if (i == 49) {
            onSetPreferredNetworkTypeDone(msg);
            return true;
        } else if (i == 53) {
            onDisableVSimDone();
            return true;
        } else if (i != 85) {
            switch (i) {
                case 41:
                    onRadioPowerOffDone(msg);
                    return true;
                case 42:
                    onCardPowerOffDone(msg);
                    return true;
                case 43:
                    onSwitchSlotDone(msg);
                    return true;
                default:
                    switch (i) {
                        case HwVSimConstants.EVENT_CARD_POWER_ON_DONE /*45*/:
                            onCardPowerOnDone(msg);
                            return true;
                        case HwVSimConstants.EVENT_RADIO_POWER_ON_DONE /*46*/:
                            onRadioPowerOnDone(msg);
                            return true;
                        case HwVSimConstants.EVENT_SET_ACTIVE_MODEM_MODE_DONE /*47*/:
                            onSetActiveModemModeDone(msg);
                            return true;
                        default:
                            switch (i) {
                                case HwVSimConstants.EVENT_GET_ICC_STATUS_DONE /*79*/:
                                    onGetIccCardStatusDone(msg);
                                    return true;
                                case HwVSimConstants.EVENT_SET_CDMA_MODE_SIDE_DONE /*80*/:
                                    onSetCdmaModeSideDone(msg);
                                    return true;
                                default:
                                    return false;
                            }
                    }
            }
        } else {
            onGetIccCardStatusForGetCardCountDone(msg);
            return true;
        }
    }

    public void doProcessException(AsyncResult ar, HwVSimRequest request) {
        doDisableProcessException(ar, request);
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    protected void onCardPowerOffDone(Message msg) {
        logd("onCardPowerOffDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 4);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidNoProcessException(ar)) {
            HwVSimRequest request = ar.userObj;
            int subId = request.mSubId;
            int subCount = request.getSubCount();
            for (int i = 0; i < subCount; i++) {
                if (subId == request.getSubIdByIndex(i)) {
                    request.setCardOnOffMark(i, false);
                }
            }
            getIccCardStatus(request, subId);
        }
    }

    protected void afterGetAllCardStateDone() {
        logd("afterGetAllCardStateDone -> switch sim slot.");
        int expectSlot = this.mRequest.getExpectSlot();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("restore user switch dual card slot to expectSlot: ");
        stringBuilder.append(expectSlot);
        logd(stringBuilder.toString());
        if (expectSlot != -1) {
            HwVSimPhoneFactory.setUserSwitchDualCardSlots(expectSlot);
        }
        this.mVSimController.setVSimCurCardType(-1);
        this.mModemAdapter.switchSimSlot(this, this.mRequest);
    }

    protected void onSwitchSlotDone(Message msg) {
        logd("onSwitchSlotDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 5);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidForRequestNotSupport(ar)) {
            this.mModemAdapter.onSwitchSlotDone(this, ar);
            this.mVSimController.clearAllMarkForCardReload();
            HwVSimRequest request = ar.userObj;
            int subId = request.getMainSlot();
            this.mModemAdapter.getRadioCapability();
            if (!HwVSimModemAdapter.IS_FAST_SWITCH_SIMSLOT && request.getIsNeedSwitchCommrilMode()) {
                this.mVSimController.setBlockPinFlag(true);
                this.mVSimController.setBlockPinTable(subId, true);
            }
            if (HwVSimModemAdapter.IS_FAST_SWITCH_SIMSLOT && request.getIsNeedSwitchCommrilMode()) {
                setCdmaModeSide(msg);
            } else {
                restoreSavedNetworkForM0(request);
            }
        }
    }

    private void onSetCdmaModeSideDone(Message msg) {
        logd("onSetCdmaModeSideDone");
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            HwVSimRequest request = ar.userObj;
            HwVSimController.getInstance().setIsWaitingSwitchCdmaModeSide(false);
            SystemProperties.set("persist.radio.commril_mode", request.getExpectCommrilMode().toString());
            this.mModemAdapter.getRadioCapability();
            restoreSavedNetworkForM0(request);
        }
    }

    private void restoreSavedNetworkForM0(HwVSimRequest request) {
        int subId = request.getExpectSlot();
        int networkModeForM0 = getNetworkTypeOnModem0ForDWork();
        this.mVSimController.setPreferredNetworkTypeDisableFlag(1);
        this.mModemAdapter.setPreferredNetworkType(this, request, subId, networkModeForM0);
    }

    private int getNetworkTypeOnModem0ForDWork() {
        return this.mModemAdapter.restoreSavedNetworkMode(0);
    }

    private int getNetworkTypeOnModem1ForDWork(int slotInM1) {
        int networkModeForM1 = this.mModemAdapter.restoreSavedNetworkMode(1);
        if (!HwVSimUtilsInner.IS_CMCC_4GSWITCH_DISABLE || !HwFullNetworkManager.getInstance().isCMCCHybird()) {
            return networkModeForM1;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetPreferredNetworkTypeDone, slaveSlot ");
        stringBuilder.append(slotInM1);
        stringBuilder.append(" is not cmcc card, so set 3G/2G");
        logd(stringBuilder.toString());
        if (HwTelephonyManagerInner.getDefault().isCDMASimCard(slotInM1)) {
            return 4;
        }
        return 3;
    }

    protected void onSetPreferredNetworkTypeDone(Message msg) {
        logd("onSetPreferredNetworkTypeDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 10);
        if (isAsyncResultValid(msg.obj)) {
            int flag = this.mVSimController.getPreferredNetworkTypeDisableFlag();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSetPreferredNetworkTypeDone, flag = ");
            stringBuilder.append(flag);
            logd(stringBuilder.toString());
            int slotInM0 = this.mRequest.getExpectSlot();
            int slotInM1 = HwVSimUtilsInner.getAnotherSlotId(slotInM0);
            switch (flag) {
                case 1:
                    this.mModemAdapter.saveNetworkTypeToDB(slotInM0, getNetworkTypeOnModem0ForDWork());
                    int networkModeForM1 = getNetworkTypeOnModem1ForDWork(slotInM1);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onSetPreferredNetworkTypeDone, subId = ");
                    stringBuilder2.append(slotInM1);
                    stringBuilder2.append(", networkMode = ");
                    stringBuilder2.append(networkModeForM1);
                    logd(stringBuilder2.toString());
                    this.mModemAdapter.setPreferredNetworkType(this, this.mRequest, slotInM1, networkModeForM1);
                    this.mVSimController.setPreferredNetworkTypeDisableFlag(2);
                    break;
                case 2:
                    if (HwVSimUtilsInner.isDualImsSupported()) {
                        this.mModemAdapter.saveNetworkTypeToDB(slotInM1, getNetworkTypeOnModem1ForDWork(slotInM1));
                    }
                    cardPowerOn(msg);
                    this.mVSimController.setPreferredNetworkTypeDisableFlag(0);
                    break;
            }
            return;
        }
        if (HwVSimModemAdapter.IS_FAST_SWITCH_SIMSLOT) {
            this.mVSimController.setPreferredNetworkTypeDisableFlag(0);
        }
    }

    private void cardPowerOn(Message msg) {
        HwVSimRequest request = msg.obj.userObj;
        int subId = request.getMainSlot();
        if (isCrossProcess()) {
            for (int i = 0; i < HwVSimModemAdapter.PHONE_COUNT; i++) {
                subId = request.getSubIdByIndex(i);
                request.setCardOnOffMark(i, true);
                HwVSimRequest cloneRequest = request.clone();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cardPowerOn:cross subId = ");
                stringBuilder.append(subId);
                logd(stringBuilder.toString());
                this.mModemAdapter.cardPowerOn(this, cloneRequest, subId, 1);
                this.mVSimController.setMarkForCardReload(subId, true);
            }
        } else if (isSwapProcess()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("cardPowerOn:swap subId = ");
            stringBuilder2.append(subId);
            logd(stringBuilder2.toString());
            this.mModemAdapter.cardPowerOn(this, this.mRequest, subId, 1);
            this.mVSimController.setMarkForCardReload(subId, true);
        }
    }

    protected void onCardPowerOnDone(Message msg) {
        logd("onCardPowerOnDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 7);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidNoProcessException(ar)) {
            HwVSimRequest request = ar.userObj;
            int subId = request.mSubId;
            int subCount = request.getSubCount();
            for (int i = 0; i < subCount; i++) {
                if (subId == request.getSubIdByIndex(i)) {
                    request.setCardOnOffMark(i, false);
                }
            }
            if (ar.exception != null) {
                this.mVSimController.setMarkForCardReload(subId, false);
            }
            if (request.isAllMarkClear()) {
                if (HwVSimUtilsInner.isPlatformTwoModems()) {
                    getIccCardStatusForGetCardCount(request);
                } else {
                    setActiveModemMode(request);
                }
            }
        }
    }

    private void getIccCardStatusForGetCardCount(HwVSimRequest request) {
        int i;
        logd("getIccCardStatusForGetCardCount,wait card status is absent or present.");
        for (i = 0; i < HwVSimModemAdapter.PHONE_COUNT; i++) {
            request.setGetIccCardStatusMark(i, true);
        }
        for (i = 0; i < HwVSimModemAdapter.PHONE_COUNT; i++) {
            setIccCardStatusRetryTimes(i, 0);
            this.mModemAdapter.getIccCardStatusForGetCardCount(this, request.clone(), i);
        }
    }

    private void onGetIccCardStatusForGetCardCountDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar != null) {
            HwVSimRequest request = ar.userObj;
            if (request != null) {
                int subId = request.mSubId;
                IccCardStatus status = ar.result;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onGetIccCardStatusForGetCardCountDone:mCardState[");
                stringBuilder.append(subId);
                stringBuilder.append("]=");
                stringBuilder.append(status != null ? status.mCardState : " state is null");
                logd(stringBuilder.toString());
                int retryTimes = getIccCardStatusRetryTimes(subId);
                boolean isError = ar.exception != null;
                boolean isCardPresent = (isError || status == null || status.mCardState != CardState.CARDSTATE_PRESENT) ? false : true;
                boolean isCardAbsent = (isError || status == null || status.mCardState != CardState.CARDSTATE_ABSENT) ? false : true;
                boolean isCardReady = isCardPresent || isCardAbsent;
                boolean isErrorOrNotReady = isError || !isCardReady;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onGetIccCardStatusForGetCardCountDone: isError=");
                stringBuilder2.append(isError);
                stringBuilder2.append(", isCardPresent");
                stringBuilder2.append(isCardPresent);
                logd(stringBuilder2.toString());
                if (isCardPresent) {
                    request.setCardCount(request.getCardCount() + 1);
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("onGetIccCardStatusForGetCardCountDone, cardCount=");
                stringBuilder3.append(request.getCardCount());
                logd(stringBuilder3.toString());
                if (!isErrorOrNotReady || retryTimes >= 5) {
                    request.setGetIccCardStatusMark(subId, false);
                    setIccCardStatusRetryTimes(subId, 0);
                    if (isAllMarkClear(request)) {
                        this.mVSimController.setSavedMainSlotAndCardCount(HwVSimPhoneFactory.getVSimSavedMainSlot(), request.getCardCount());
                        setActiveModemMode(request);
                    }
                } else {
                    retryTimes++;
                    setIccCardStatusRetryTimes(subId, retryTimes);
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("onGetIccCardStatusForGetCardCountDone: retry getIccCardStatus,Times=");
                    stringBuilder4.append(retryTimes);
                    logd(stringBuilder4.toString());
                    this.mModemAdapter.getIccCardStatusForGetCardCount(this, request, subId);
                }
            }
        }
    }

    private void setActiveModemMode(HwVSimRequest request) {
        this.mModemAdapter.setActiveModemMode(this, request, request.getExpectSlot());
    }

    protected void onSetActiveModemModeDone(Message msg) {
        logd("onSetActiveModemModeDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 8);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            radioPowerOn(ar.userObj);
        }
    }

    private void radioPowerOn(HwVSimRequest request) {
        int subId = request.getMainSlot();
        if (isCrossProcess()) {
            for (int i = 0; i < HwVSimModemAdapter.PHONE_COUNT; i++) {
                subId = request.getSubIdByIndex(i);
                request.setPowerOnOffMark(i, true);
                HwVSimRequest cloneRequest = request.clone();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("radioPowerOn:cross subId = ");
                stringBuilder.append(subId);
                logd(stringBuilder.toString());
                this.mModemAdapter.radioPowerOn(this, cloneRequest, subId);
            }
        } else if (isSwapProcess()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("radioPowerOn:swap subId = ");
            stringBuilder2.append(subId);
            logd(stringBuilder2.toString());
            this.mModemAdapter.radioPowerOn(this, request, subId);
        }
    }

    protected void onRadioPowerOnDone(Message msg) {
        logd("onRadioPowerOnDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 11);
        AsyncResult ar = msg.obj;
        if (isNeedWaitNvCfgMatchAndRestartRild() || isAsyncResultValid(ar)) {
            HwVSimRequest request = ar.userObj;
            int subId = request.mSubId;
            int subCount = request.getSubCount();
            for (int i = 0; i < subCount; i++) {
                if (subId == request.getSubIdByIndex(i)) {
                    request.setPowerOnOffMark(i, false);
                }
            }
            if (isAllMarkClear(request)) {
                logd("onRadioPowerOnDone:isAllMarkClear");
                disableVSimDone();
            }
        }
    }

    private void onDisableVSimDone() {
        logd("onDisableVSimDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 14);
        notifyResult(this.mRequest, Boolean.valueOf(true));
        transitionToState(7);
    }

    private void disableVSimDone() {
        logd("disableVSimDone");
        Message onCompleted = obtainMessage(53, this.mRequest);
        AsyncResult.forMessage(onCompleted);
        onCompleted.sendToTarget();
    }

    protected boolean isNeedWaitNvCfgMatchAndRestartRild() {
        return HwVSimUtilsInner.isPlatformNeedWaitNvMatchUnsol() && HwVSimController.getInstance().getInsertedCardCount() != 0;
    }
}
