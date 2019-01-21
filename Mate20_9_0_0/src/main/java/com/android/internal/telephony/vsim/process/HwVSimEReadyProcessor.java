package com.android.internal.telephony.vsim.process;

import android.os.AsyncResult;
import android.os.Message;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimController;
import com.android.internal.telephony.vsim.HwVSimController.ProcessState;
import com.android.internal.telephony.vsim.HwVSimEventReport.VSimEventInfoUtils;
import com.android.internal.telephony.vsim.HwVSimLog;
import com.android.internal.telephony.vsim.HwVSimModemAdapter;
import com.android.internal.telephony.vsim.HwVSimRequest;
import com.android.internal.telephony.vsim.HwVSimSlotSwitchController.CommrilMode;
import com.android.internal.telephony.vsim.HwVSimUtilsInner;

public class HwVSimEReadyProcessor extends HwVSimReadyProcessor {
    public static final String LOG_TAG = "VSimEReadyProcessor";
    boolean mHasEnterAfterNetwork = false;

    public static HwVSimEReadyProcessor create(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        if (controller == null || !controller.isDirectProcess()) {
            return new HwVSimEReadyProcessor(controller, modemAdapter, request);
        }
        return new HwVSimEDReadyProcessor(controller, modemAdapter, request);
    }

    HwVSimEReadyProcessor(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        super(controller, modemAdapter, request);
    }

    public void onEnter() {
        logd("onEnter");
        this.mHasEnterAfterNetwork = false;
        setProcessState(ProcessState.PROCESS_STATE_READY);
        this.mIsM0Ready = false;
        this.mVSimController.setOnVsimRegPLMNSelInfo(this.mHandler, 65, null);
        this.mVSimController.registerNetStateReceiver();
        if (this.mHandler != null) {
            this.mHandler.sendEmptyMessageDelayed(71, 60000);
        }
    }

    public void onExit() {
        logd("onExit");
        this.mHasEnterAfterNetwork = false;
        if (this.mHandler != null) {
            this.mHandler.removeMessages(71);
        }
        this.mVSimController.unregisterNetStateReceiver();
        this.mVSimController.unSetOnVsimRegPLMNSelInfo(this.mHandler);
        setProcessState(ProcessState.PROCESS_STATE_NONE);
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    public boolean processMessage(Message msg) {
        boolean retVal = true;
        if (!isMessageShouldDeal(msg)) {
            return false;
        }
        switch (msg.what) {
            case HwVSimConstants.EVENT_CARD_POWER_ON_DONE /*45*/:
                onCardPowerOnDone(msg);
                break;
            case HwVSimConstants.EVENT_RADIO_POWER_ON_DONE /*46*/:
                onRadioPowerOnDone(msg);
                break;
            case 50:
                onNetworkConnected();
                break;
            case HwVSimConstants.EVENT_ENABLE_VSIM_FINISH /*57*/:
                onEnableVSimFinish();
                break;
            case HwVSimConstants.EVENT_VSIM_PLMN_SELINFO /*65*/:
                onPlmnSelInfoDone(msg);
                break;
            case HwVSimConstants.EVENT_SET_NETWORK_RAT_AND_SRVDOMAIN_DONE /*66*/:
                onSetNetworkRatAndSrvdomainDone(msg);
                break;
            case HwVSimConstants.EVENT_NETWORK_CONNECT_TIMEOUT /*71*/:
                onNetworkConnectTimeout();
                break;
            default:
                retVal = false;
                break;
        }
        return retVal;
    }

    public void doProcessException(AsyncResult ar, HwVSimRequest request) {
        doEnableProcessException(ar, request, Integer.valueOf(3));
    }

    void onNetworkConnected() {
        logd("onNetworkConnected");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 16);
        if (this.mIsM0Ready) {
            if (this.mHandler != null) {
                this.mHandler.removeMessages(71);
            }
            this.mVSimController.unregisterNetStateReceiver();
            this.mIsM0Ready = false;
            afterNetwork();
        }
    }

    void onNetworkConnectTimeout() {
        logd("onNetworkConnectTimeout");
        this.mVSimController.unregisterNetStateReceiver();
        if (this.mHandler != null) {
            this.mHandler.removeMessages(65);
            this.mHandler.removeMessages(50);
        }
        this.mIsM0Ready = false;
        afterNetwork();
    }

    protected void afterNetwork() {
        logd("afterNetwork");
        if (this.mHasEnterAfterNetwork) {
            logd("can not enter more than once.");
            return;
        }
        this.mHasEnterAfterNetwork = true;
        HwVSimRequest request = this.mRequest;
        if (request != null) {
            int subId;
            int simIndex;
            if (isSwapProcess()) {
                subId = request.getMainSlot();
                simIndex = 1;
                if (subId == 2) {
                    simIndex = 11;
                }
                this.mModemAdapter.cardPowerOn(this, request, subId, simIndex);
                request.setSource(3);
                this.mVSimController.setMarkForCardReload(subId, true);
            } else {
                simIndex = 0;
                if (isCrossProcess()) {
                    subId = request.getMainSlot();
                    if (subId == 0) {
                        simIndex = 1;
                    }
                    this.mRequest.setSource(3);
                    HwVSimRequest slaveRequest = this.mRequest.clone();
                    if (slaveRequest != null) {
                        slaveRequest.setPowerOnOffMark(simIndex, true);
                        slaveRequest.setCardOnOffMark(simIndex, true);
                        this.mModemAdapter.cardPowerOn(this, slaveRequest, simIndex, 1);
                        this.mVSimController.setMarkForCardReload(simIndex, true);
                    }
                    if (HwVSimUtilsInner.isPlatformRealTripple()) {
                        int mainIndex = subId == 2 ? 11 : 1;
                        HwVSimRequest mainRequest = this.mRequest.clone();
                        if (mainRequest != null) {
                            mainRequest.setPowerOnOffMark(subId, true);
                            mainRequest.setCardOnOffMark(subId, true);
                            this.mModemAdapter.cardPowerOn(this, mainRequest, subId, mainIndex);
                            this.mVSimController.setMarkForCardReload(subId, true);
                        }
                    }
                } else if (isDirectProcess()) {
                    int subId2 = request.getMainSlot();
                    if (this.mModemAdapter.isNeedRadioOnM2()) {
                        this.mModemAdapter.setNetworkRatAndSrvdomain(this, request, subId2, calculateNetworkModeForModem2(), 0);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("m2 subId = ");
                        stringBuilder.append(subId2);
                        stringBuilder.append("set radio power off");
                        logd(stringBuilder.toString());
                        this.mVSimController.getPhoneBySub(subId2).getServiceStateTracker().setDesiredPowerState(false);
                        enableVSimFinish();
                    }
                }
            }
            if (HwVSimUtilsInner.isPlatformTwoModems()) {
                powerOnSlaveCardOnDSDS();
            } else {
                powerOnSlaveRadioOnDSDS();
            }
        }
    }

    private int calculateNetworkModeForModem2() {
        int rat = 1;
        if (HwVSimUtilsInner.isDualImsSupported() && this.mVSimController.hasIccCardOnM2() && this.mVSimController.getCommrilMode() == CommrilMode.HISI_VSIM_MODE) {
            rat = 59;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("calculateNetworkModeForModem2, rat = ");
        stringBuilder.append(rat);
        logd(stringBuilder.toString());
        return rat;
    }

    protected void onCardPowerOnDone(Message msg) {
        logd("onCardPowerOnDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 7);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidNoProcessException(ar)) {
            HwVSimRequest request = ar.userObj;
            int subId = request.mSubId;
            int mainSlot = request.getMainSlot();
            request.setCardOnOffMark(subId, false);
            if (ar.exception != null) {
                this.mVSimController.setMarkForCardReload(subId, false);
            }
            if (isSwapProcess()) {
                doRadioPowerOnForSwap(request);
            } else if (isCrossProcess()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCardPowerOnDone: isCrossProcess subId = ");
                stringBuilder.append(subId);
                stringBuilder.append(" mainSlot=");
                stringBuilder.append(mainSlot);
                logd(stringBuilder.toString());
                if (HwVSimUtilsInner.isVSimDsdsVersionOne()) {
                    if (this.mVSimController.isSubOnM2(subId)) {
                        request.setPowerOnOffMark(subId, false);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("onCardPowerOnDone: isCrossProcess, m2 on subId = ");
                        stringBuilder.append(subId);
                        stringBuilder.append(", set radio_power off");
                        logd(stringBuilder.toString());
                        this.mVSimController.getPhoneBySub(subId).getServiceStateTracker().setDesiredPowerState(false);
                        if (isAllMarkClear(request)) {
                            enableVSimFinish();
                        }
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("onCardPowerOnDone: isCrossProcess set radio_power On subId =");
                        stringBuilder2.append(subId);
                        logd(stringBuilder2.toString());
                        this.mModemAdapter.radioPowerOn(this, request, subId);
                    }
                } else if (subId != mainSlot) {
                    this.mModemAdapter.radioPowerOn(this, request, subId);
                } else if (HwVSimUtilsInner.isPlatformRealTripple() && HwVSimUtilsInner.IS_DSDSPOWER_SUPPORT) {
                    this.mModemAdapter.radioPowerOn(this, request, subId);
                } else {
                    request.setPowerOnOffMark(subId, false);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("m2 subId = ");
                    stringBuilder.append(subId);
                    stringBuilder.append(" set radio power off");
                    logd(stringBuilder.toString());
                    this.mVSimController.getPhoneBySub(subId).getServiceStateTracker().setDesiredPowerState(false);
                    if (isAllMarkClear(request)) {
                        enableVSimFinish();
                    }
                }
            }
        }
    }

    private void onSetNetworkRatAndSrvdomainDone(Message msg) {
        AsyncResult ar = msg.obj;
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 1);
        if (isAsyncResultValid(ar)) {
            this.mModemAdapter.onSetNetworkRatAndSrvdomainDone(this, ar);
            int subId = ar.userObj.mSubId;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSetNetworkRatAndSrvdomainDone subId = ");
            stringBuilder.append(subId);
            logd(stringBuilder.toString());
            this.mModemAdapter.radioPowerOn(this, this.mRequest, subId);
        }
    }

    protected void onRadioPowerOnDone(Message msg) {
        logd("onRadioPowerOnDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 11);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidForRequestNotSupport(ar)) {
            HwVSimRequest request = ar.userObj;
            int subId = request.mSubId;
            request.setPowerOnOffMark(subId, false);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onRadioPowerOnDone : subId = ");
            stringBuilder.append(subId);
            logd(stringBuilder.toString());
            if (HwVSimUtilsInner.IS_DSDSPOWER_SUPPORT && isSwapProcess() && subId != request.getMainSlot()) {
                logd("onRadioPowerOnDone : slave slot not finish!");
            } else if (isAllMarkClear(request)) {
                enableVSimFinish();
            }
        }
    }

    private void onEnableVSimFinish() {
        logd("onEnableVSimFinish");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 13);
        transitionToState(0);
    }

    private void enableVSimFinish() {
        logd("enableVSimFinish");
        Message onCompleted = this.mVSimController.obtainMessage(57, this.mRequest);
        AsyncResult.forMessage(onCompleted);
        onCompleted.sendToTarget();
    }

    private void doRadioPowerOnForSwap(HwVSimRequest request) {
        if (HwVSimUtilsInner.isPlatformTwoModems()) {
            powerOnSlaveRadioOnDSDS();
        }
        int userReservedSubId = this.mVSimController.getUserReservedSubId();
        int subId = request.getMainSlot();
        int insertedSimCount = this.mVSimController.getInsertedSimCount();
        boolean hasIccCardOnM2 = this.mVSimController.hasIccCardOnM2();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("doRadioPowerOnForSwap subId = ");
        stringBuilder.append(subId);
        stringBuilder.append(" userReservedSubId = ");
        stringBuilder.append(userReservedSubId);
        stringBuilder.append(" insertedSimCount = ");
        stringBuilder.append(insertedSimCount);
        stringBuilder.append(" hasIccCardOnM2 = ");
        stringBuilder.append(hasIccCardOnM2);
        logd(stringBuilder.toString());
        boolean z = true;
        if (!HwVSimUtilsInner.isPlatformTwoModemsActual() || (insertedSimCount != 0 && ((insertedSimCount != 1 || hasIccCardOnM2) && (insertedSimCount != 2 || subId == userReservedSubId)))) {
            z = false;
        }
        if (z) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("doRadioPowerOnForSwap no need m2 power on from subId = ");
            stringBuilder.append(subId);
            logd(stringBuilder.toString());
            request.setPowerOnOffMark(subId, false);
            this.mVSimController.getPhoneBySub(subId).getServiceStateTracker().setDesiredPowerState(false);
            enableVSimFinish();
            return;
        }
        this.mModemAdapter.setNetworkRatAndSrvdomain(this, this.mRequest, subId, 1, 0);
    }

    private void powerOnSlaveCardOnDSDS() {
        if (this.mRequest != null && HwVSimUtilsInner.IS_DSDSPOWER_SUPPORT && isSwapProcess()) {
            int slaveSlot = HwVSimUtilsInner.getAnotherSlotId(this.mRequest.getMainSlot());
            int slaveIndex = HwVSimUtilsInner.getSinIndexBySlotId(slaveSlot);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("powerOnSlaveCardOnDSDS, slotid = ");
            stringBuilder.append(slaveSlot);
            logd(stringBuilder.toString());
            HwVSimRequest slaveRequest = this.mRequest.clone();
            if (slaveRequest != null) {
                slaveRequest.setSource(3);
                this.mModemAdapter.cardPowerOn(this, slaveRequest, slaveSlot, slaveIndex);
            }
        }
    }

    private void powerOnSlaveRadioOnDSDS() {
        if (this.mRequest != null && HwVSimUtilsInner.IS_DSDSPOWER_SUPPORT && isSwapProcess()) {
            int slaveSlot = this.mRequest.getMainSlot() == 0 ? 1 : 0;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("powerOnSlaveRadioOnDSDS, slotid = ");
            stringBuilder.append(slaveSlot);
            logd(stringBuilder.toString());
            this.mVSimController.setProhibitSubUpdateSimNoChange(slaveSlot, false);
            HwVSimRequest slaveRequest = this.mRequest.clone();
            if (slaveRequest != null) {
                slaveRequest.setSource(3);
                this.mModemAdapter.radioPowerOn(this, slaveRequest, slaveSlot);
            }
        }
    }

    private boolean isMessageShouldDeal(Message msg) {
        return super.isMessageShouldDeal(msg, 3);
    }
}
