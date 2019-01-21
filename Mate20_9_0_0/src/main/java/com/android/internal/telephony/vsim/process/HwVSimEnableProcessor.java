package com.android.internal.telephony.vsim.process;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatusUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimController;
import com.android.internal.telephony.vsim.HwVSimController.EnableParam;
import com.android.internal.telephony.vsim.HwVSimController.ProcessAction;
import com.android.internal.telephony.vsim.HwVSimEventReport.VSimEventInfoUtils;
import com.android.internal.telephony.vsim.HwVSimLog;
import com.android.internal.telephony.vsim.HwVSimModemAdapter;
import com.android.internal.telephony.vsim.HwVSimModemAdapter.SimStateInfo;
import com.android.internal.telephony.vsim.HwVSimRequest;
import com.android.internal.telephony.vsim.HwVSimUtilsInner;

public class HwVSimEnableProcessor extends HwVSimProcessor {
    public static final String LOG_TAG = "VSimEnableProcessor";
    protected Handler mHandler;
    private CardState mVSimCardState = CardState.CARDSTATE_ABSENT;
    protected HwVSimController mVSimController;

    public HwVSimEnableProcessor(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        super(modemAdapter, request);
        this.mVSimController = controller;
        this.mHandler = this.mVSimController.getHandler();
    }

    public void onEnter() {
        logd("onEnter");
        cmdSem_release();
        HwVSimRequest request = this.mRequest;
        int subId = 0;
        if (request == null) {
            transitionToState(0);
            return;
        }
        EnableParam param = getEnableParam(request);
        if (param == null) {
            notifyResult(request, Integer.valueOf(3));
            transitionToState(0);
            return;
        }
        if (param.operation == 5) {
            this.mVSimController.setProcessAction(ProcessAction.PROCESS_ACTION_ENABLE_OFFLINE);
        } else {
            this.mVSimController.setProcessAction(ProcessAction.PROCESS_ACTION_ENABLE);
        }
        this.mModemAdapter.doEnableStateEnter(this, request);
        this.mVSimController.registerForVSimIccChanged(this.mHandler, 3, Integer.valueOf(2));
        request.createGotCardType(HwVSimModemAdapter.PHONE_COUNT);
        request.createCardTypes(HwVSimModemAdapter.PHONE_COUNT);
        this.mVSimController.updateUiccCardCount();
        request.setGotSimSlotMark(false);
        while (subId < HwVSimModemAdapter.MAX_SUB_COUNT) {
            if (this.mModemAdapter.getCiBySub(subId) != null && HwVSimUtilsInner.isRadioAvailable(subId)) {
                this.mModemAdapter.getSimSlot(this, request.clone(), subId);
            }
            subId++;
        }
        this.mVSimController.setOnRadioAvaliable(this.mHandler, 83, null);
    }

    public void onExit() {
        logd("onExit");
        this.mVSimController.unregisterForVSimIccChanged(this.mHandler);
        this.mVSimController.setBlockPinFlag(false);
        this.mVSimController.clearProhibitSubUpdateSimNoChange();
        this.mVSimController.unSetOnRadioAvaliable(this.mHandler);
        this.mVSimController.delaymIsVSimCauseCardReloadRecover();
    }

    public boolean processMessage(Message msg) {
        int i = msg.what;
        if (i != 48) {
            switch (i) {
                case 2:
                    onGetSimStateDone(msg);
                    return true;
                case 3:
                    onIccChanged(msg);
                    return true;
                default:
                    switch (i) {
                        case 54:
                            onGetSimSlotDone(msg);
                            return true;
                        case HwVSimConstants.EVENT_SWITCH_COMMRIL_DONE /*55*/:
                            onSwitchCommrilDone(msg);
                            return true;
                        case HwVSimConstants.EVENT_QUERY_CARD_TYPE_DONE /*56*/:
                            onQueryCardTypeDone(msg);
                            return true;
                        default:
                            return false;
                    }
            }
        }
        onGetPreferredNetworkTypeDone(msg);
        return true;
    }

    public Message obtainMessage(int what, Object obj) {
        return this.mVSimController.obtainMessage(what, obj);
    }

    public void transitionToState(int state) {
        this.mVSimController.transitionToState(state);
    }

    public void doProcessException(AsyncResult ar, HwVSimRequest request) {
        doEnableProcessException(ar, request, Integer.valueOf(3));
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    private void onGetSimSlotDone(Message msg) {
        logd("onGetSimSlotDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 1);
        AsyncResult ar = msg.obj;
        if (ar != null) {
            if (ar.exception != null && isRequestNotSupport(ar.exception)) {
                logd("request not support, just skip");
            } else if (isAsyncResultValidForRequestNotSupport(ar)) {
                HwVSimRequest request = ar.userObj;
                if (request.isGotSimSlot()) {
                    logd("already got sim slot, just skip");
                    return;
                }
                this.mModemAdapter.onGetSimSlotDone(this, ar);
                boolean isVSimOnM0 = request.getIsVSimOnM0();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onGetSimSlotDone isVSimOnM0 = ");
                stringBuilder.append(isVSimOnM0);
                logd(stringBuilder.toString());
                if (isVSimOnM0) {
                    this.mModemAdapter.getSimState(this, request, 2);
                } else {
                    this.mVSimController.setIsVSimOn(false);
                    this.mModemAdapter.checkVSimCondition(this, this.mRequest);
                }
            }
        }
    }

    protected void onGetSimStateDone(Message msg) {
        logd("onGetSimStateDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 3);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            SimStateInfo ssInfo = this.mModemAdapter.onGetSimStateDone(this, ar);
            if (ssInfo != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onGetSimStateDone ssInfo = ");
                stringBuilder.append(ssInfo.toString());
                logd(stringBuilder.toString());
            }
            if (ssInfo != null) {
                if (ssInfo.simIndex != 11) {
                    logd("warning, slot switched yet vsim not loaded");
                }
                if (ssInfo.simSub != 2) {
                    logd("warning, wrong sim sub");
                }
                HwVSimController hwVSimController = this.mVSimController;
                boolean z = true;
                if (ssInfo.simNetInfo != 1) {
                    z = false;
                }
                hwVSimController.setIsVSimOn(z);
            }
            this.mModemAdapter.checkVSimCondition(this, this.mRequest);
        }
    }

    protected void onQueryCardTypeDone(Message msg) {
        logd("onQueryCardTypeDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 1);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            this.mModemAdapter.onQueryCardTypeDone(this, ar);
            if (this.mRequest.isGotAllCardTypes()) {
                int slotInModem0 = this.mRequest.getMainSlot();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onQueryCardTypeDone, get pref network type for subId :");
                stringBuilder.append(slotInModem0);
                logd(stringBuilder.toString());
                this.mModemAdapter.getPreferredNetworkType(this, this.mRequest, slotInModem0);
            }
        }
    }

    private void onGetPreferredNetworkTypeDone(Message msg) {
        logd("onGetPreferredNetworkTypeDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 9);
        AsyncResult ar = msg.obj;
        if (ar != null) {
            boolean isNotSupport = false;
            if (ar.exception != null && isRequestNotSupport(ar.exception)) {
                isNotSupport = true;
            }
            if (isAsyncResultValidForRequestNotSupport(ar)) {
                HwVSimRequest request = ar.userObj;
                int subId = request.mSubId;
                if (subId == request.getMainSlot() || isNotSupport) {
                    int slotInModem1 = 0;
                    if (!isNotSupport) {
                        this.mModemAdapter.onGetPreferredNetworkTypeDone(this, ar, 0);
                    }
                    HwVSimRequest cloneRequest = request.clone();
                    if (subId == 0) {
                        slotInModem1 = 1;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onGetPreferredNetworkTypeDone, get pref network type for subId :");
                    stringBuilder.append(slotInModem1);
                    logd(stringBuilder.toString());
                    this.mModemAdapter.getPreferredNetworkType(this, cloneRequest, slotInModem1);
                } else {
                    this.mModemAdapter.onGetPreferredNetworkTypeDone(this, ar, 1);
                    this.mModemAdapter.checkEnableSimCondition(this, this.mRequest);
                }
            }
        }
    }

    private void onSwitchCommrilDone(Message msg) {
        logd("onSwitchCommrilDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 1);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            this.mModemAdapter.onSwitchCommrilDone(this, ar);
            transitionToState(3);
        }
    }

    private void onIccChanged(Message msg) {
        logd("onIccChanged");
        if (msg.obj == null) {
            logd("onIccChanged : ar null");
        } else {
            updateIccAvailability(HwVSimUtilsInner.getCiIndex(msg).intValue());
        }
    }

    private void updateIccAvailability(int subId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateIccAvailability, subId = ");
        stringBuilder.append(subId);
        logd(stringBuilder.toString());
        if (subId != 2) {
            logd("only deal with VSIM");
            return;
        }
        UiccCard newCard = this.mVSimController.getUiccCard(subId);
        if (newCard == null) {
            logd("new card null");
            return;
        }
        CardState newState = newCard.getCardState();
        CardState oldState = this.mVSimCardState;
        this.mVSimCardState = newState;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Slot[");
        stringBuilder2.append(subId);
        stringBuilder2.append("]: New Card State = ");
        stringBuilder2.append(newState);
        stringBuilder2.append(" Old Card State = ");
        stringBuilder2.append(oldState);
        logd(stringBuilder2.toString());
        if (!IccCardStatusUtils.isCardPresent(oldState) && IccCardStatusUtils.isCardPresent(newState) && this.mVSimController.isWorkProcess()) {
            logd("vsim inserted");
            switchDDS();
        }
    }

    private void switchDDS() {
        if (this.mVSimController != null) {
            this.mVSimController.switchDDS();
        }
    }

    private EnableParam getEnableParam(HwVSimRequest request) {
        if (this.mVSimController == null) {
            return null;
        }
        return this.mVSimController.getEnableParam(request);
    }

    private void cmdSem_release() {
        if (this.mVSimController != null) {
            this.mVSimController.cmdSem_release();
        }
    }
}
