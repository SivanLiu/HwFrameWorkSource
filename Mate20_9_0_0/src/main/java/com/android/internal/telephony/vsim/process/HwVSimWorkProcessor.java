package com.android.internal.telephony.vsim.process;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.vsim.HwVSimConstants;
import com.android.internal.telephony.vsim.HwVSimController;
import com.android.internal.telephony.vsim.HwVSimController.EnableParam;
import com.android.internal.telephony.vsim.HwVSimController.ProcessState;
import com.android.internal.telephony.vsim.HwVSimEventReport.VSimEventInfoUtils;
import com.android.internal.telephony.vsim.HwVSimModemAdapter;
import com.android.internal.telephony.vsim.HwVSimModemAdapter.SimStateInfo;
import com.android.internal.telephony.vsim.HwVSimRequest;
import com.android.internal.telephony.vsim.HwVSimSlotSwitchController;
import com.android.internal.telephony.vsim.HwVSimSlotSwitchController.CommrilMode;
import com.android.internal.telephony.vsim.HwVSimUtilsInner;
import java.util.Arrays;

public abstract class HwVSimWorkProcessor extends HwVSimProcessor {
    static final int GET_ICC_CARD_STATUS_RETRY_TIMES = 5;
    static final String LOG_TAG = "VSimWorkProcessor";
    private int[] mGetIccCardStatusTimes;
    protected Handler mHandler;
    boolean mHasReceiveNvMatchUnsol = false;
    private boolean mInDSDSPreProcess;
    boolean mIsWaitingRestartRild;
    private boolean[] mRadioAvailableMark;
    protected HwVSimController mVSimController;

    protected abstract void logd(String str);

    protected abstract void onCardPowerOffDone(Message message);

    protected abstract void onCardPowerOnDone(Message message);

    protected abstract void onRadioPowerOnDone(Message message);

    protected abstract void onSetActiveModemModeDone(Message message);

    protected abstract void onSetPreferredNetworkTypeDone(Message message);

    protected abstract void onSwitchSlotDone(Message message);

    HwVSimWorkProcessor(HwVSimController controller, HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        super(modemAdapter, request);
        this.mVSimController = controller;
        this.mInDSDSPreProcess = false;
        this.mIsWaitingRestartRild = false;
        if (controller != null) {
            this.mHandler = controller.getHandler();
        }
    }

    public void onEnter() {
        logd("onEnter");
        HwVSimRequest request = this.mRequest;
        if (request != null) {
            this.mModemAdapter.handleSubSwapProcess(this, request);
            if (this.mVSimController.isEnableProcess() && !this.mVSimController.isDirectProcess()) {
                this.mModemAdapter.setHwVSimPowerOn(this, request);
            }
            if (isSwapProcess()) {
                this.mInDSDSPreProcess = true;
                int slaveSlot = request.getMainSlot() == 0 ? 1 : 0;
                this.mVSimController.setProhibitSubUpdateSimNoChange(slaveSlot, true);
                this.mModemAdapter.radioPowerOff(this, request, slaveSlot);
            } else {
                if (HwVSimModemAdapter.IS_FAST_SWITCH_SIMSLOT && this.mRequest.getIsNeedSwitchCommrilMode()) {
                    this.mVSimController.setIsWaitingSwitchCdmaModeSide(true);
                }
                this.mModemAdapter.radioPowerOff(this, request);
            }
            setProcessState(ProcessState.PROCESS_STATE_WORK);
        }
    }

    public void onExit() {
        logd("onExit");
    }

    public void transitionToState(int state) {
        this.mVSimController.transitionToState(state);
    }

    public Message obtainMessage(int what, Object obj) {
        return this.mVSimController.obtainMessage(what, obj);
    }

    protected void onRadioPowerOffDone(Message msg) {
        logd("onRadioPowerOffDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 2);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidForRequestNotSupport(ar)) {
            if (this.mInDSDSPreProcess) {
                this.mInDSDSPreProcess = false;
                HwVSimRequest request = this.mRequest;
                if (request != null) {
                    this.mModemAdapter.radioPowerOff(this, request);
                    this.mModemAdapter.onRadioPowerOffSlaveModemDone(this, request);
                } else {
                    return;
                }
            }
            this.mModemAdapter.onRadioPowerOffDone(this, ar);
        }
    }

    protected void onGetSimStateDone(Message msg) {
        logd("onGetSimStateDone");
        VSimEventInfoUtils.setCauseType(this.mVSimController.mEventInfo, 3);
        AsyncResult ar = msg.obj;
        if (isAsyncResultValidForRequestNotSupport(ar)) {
            SimStateInfo ssInfo = this.mModemAdapter.onGetSimStateDone(this, ar);
            if (ssInfo != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onGetSimStateDone ssInfo = ");
                stringBuilder.append(ssInfo.toString());
                logd(stringBuilder.toString());
            }
            HwVSimRequest request = ar.userObj;
            int subCount = request.getSubCount();
            int subId = request.mSubId;
            for (int i = 0; i < subCount; i++) {
                if (subId == request.getSubIdByIndex(i)) {
                    request.setSimStateMark(i, false);
                }
            }
            if (ssInfo != null) {
                this.mModemAdapter.cardPowerOff(this, request, subId, ssInfo.simIndex);
            }
        }
    }

    void getIccCardStatus(HwVSimRequest request, int subId) {
        logd("onCardPowerOffDone->getIccCardStatus,wait card status is absent");
        setIccCardStatusRetryTimes(subId, 0);
        this.mModemAdapter.getIccCardStatus(this, request, subId);
    }

    protected void onGetIccCardStatusDone(Message msg) {
        AsyncResult ar = msg.obj;
        if (ar != null) {
            HwVSimRequest request = ar.userObj;
            if (request != null) {
                int subId = request.mSubId;
                IccCardStatus status = ar.result;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onGetIccCardStatusDone:mCardState[");
                stringBuilder.append(subId);
                stringBuilder.append("]=");
                stringBuilder.append(status != null ? status.mCardState : " state is null");
                logd(stringBuilder.toString());
                int retryTimes = getIccCardStatusRetryTimes(subId);
                if (!(ar.exception == null && (status == null || status.mCardState == CardState.CARDSTATE_ABSENT)) && retryTimes < 5) {
                    retryTimes++;
                    setIccCardStatusRetryTimes(subId, retryTimes);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onGetIccCardStatusDone: retry getIccCardStatus,Times=");
                    stringBuilder2.append(retryTimes);
                    logd(stringBuilder2.toString());
                    this.mModemAdapter.getIccCardStatus(this, request, subId);
                } else {
                    int subCount = request.getSubCount();
                    for (int i = 0; i < subCount; i++) {
                        if (subId == request.getSubIdByIndex(i)) {
                            request.setGetIccCardStatusMark(i, false);
                        }
                    }
                    setIccCardStatusRetryTimes(subId, 0);
                    if (isAllMarkClear(request)) {
                        afterGetAllCardStateDone();
                    }
                }
            }
        }
    }

    protected void afterGetAllCardStateDone() {
        logd("afterGetAllCardStateDone - do nothing.");
    }

    void setIccCardStatusRetryTimes(int subId, int times) {
        if (this.mGetIccCardStatusTimes == null) {
            this.mGetIccCardStatusTimes = new int[HwVSimModemAdapter.MAX_SUB_COUNT];
        }
        if (subId >= 0 && subId < this.mGetIccCardStatusTimes.length) {
            this.mGetIccCardStatusTimes[subId] = times;
        }
    }

    int getIccCardStatusRetryTimes(int subId) {
        if (this.mGetIccCardStatusTimes == null || subId < 0 || subId >= this.mGetIccCardStatusTimes.length) {
            return 5;
        }
        return this.mGetIccCardStatusTimes[subId];
    }

    void setPrefNetworkForM0(AsyncResult ar, HwVSimRequest request) {
        EnableParam param = getEnableParam(request);
        if (param == null) {
            doEnableProcessException(ar, request, Integer.valueOf(3));
            return;
        }
        int networkMode = acqorderToNetworkMode(param.acqorder);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set m0 preferred network to ");
        stringBuilder.append(networkMode);
        logd(stringBuilder.toString());
        this.mModemAdapter.setPreferredNetworkType(this, request, 2, networkMode);
    }

    int acqorderToNetworkMode(String acqorder) {
        return calcNetworkModeByAcqorder(acqorder);
    }

    void setCdmaModeSide(Message msg) {
        AsyncResult ar = msg.obj;
        if (isAsyncResultValid(ar)) {
            HwVSimRequest request = ar.userObj;
            CommrilMode expectCommrilMode = request.getExpectCommrilMode();
            int subOnM0 = request.getSlots()[0];
            if (CommrilMode.HISI_CG_MODE == expectCommrilMode) {
                this.mModemAdapter.setCdmaModeSide(this, this.mRequest, subOnM0, 1);
            } else if (CommrilMode.HISI_CGUL_MODE == expectCommrilMode) {
                this.mModemAdapter.setCdmaModeSide(this, this.mRequest, subOnM0, 0);
            } else if (CommrilMode.HISI_VSIM_MODE == expectCommrilMode) {
                this.mModemAdapter.setCdmaModeSide(this, this.mRequest, subOnM0, 2);
            }
        }
    }

    private int calcNetworkModeByAcqorder(String acqorder) {
        if ("0201".equals(acqorder)) {
            return 3;
        }
        if ("01".equals(acqorder)) {
            return 1;
        }
        return 9;
    }

    int modifyNetworkMode(int oldNetworkMode) {
        int networkMode = oldNetworkMode;
        if (oldNetworkMode == 2) {
            return 3;
        }
        if (oldNetworkMode != 12) {
            return networkMode;
        }
        return 9;
    }

    EnableParam getEnableParam(HwVSimRequest request) {
        if (this.mVSimController == null) {
            return null;
        }
        return this.mVSimController.getEnableParam(request);
    }

    public boolean isDirectProcess() {
        return this.mVSimController != null && this.mVSimController.isDirectProcess();
    }

    void startListenForRildNvMatch() {
        if (isNeedWaitNvCfgMatchAndRestartRild()) {
            this.mVSimController.setOnRestartRildNvMatch(0, this.mHandler, 81, null);
            this.mVSimController.setOnRestartRildNvMatch(1, this.mHandler, 81, null);
            this.mIsWaitingRestartRild = true;
            if (this.mHandler != null) {
                this.mHandler.sendEmptyMessageDelayed(82, HwVSimConstants.WAIT_FOR_NV_CFG_MATCH_TIMEOUT);
            }
        }
    }

    void onJudgeRestartRildNvMatch(Message msg) {
        logd("onJudgeRestartRildNvMatch");
        AsyncResult ar = msg.obj;
        if (ar != null && ar.exception == null && (ar.result instanceof int[]) && !this.mHasReceiveNvMatchUnsol) {
            int response = ((int[]) ar.result)[0];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onJudgeRestartRildNvMatch, response = ");
            stringBuilder.append(response);
            logd(stringBuilder.toString());
            this.mHasReceiveNvMatchUnsol = true;
            switch (response) {
                case 0:
                    removeMessageAndStopListen();
                    afterJudgeRestartRildNvMatch();
                    return;
                case 1:
                    removeMessageAndStopListen();
                    restartRild();
                    return;
                default:
                    this.mHasReceiveNvMatchUnsol = false;
                    return;
            }
        }
    }

    private void removeMessageAndStopListen() {
        if (this.mHandler != null) {
            this.mHandler.removeMessages(81);
            this.mHandler.removeMessages(82);
            this.mVSimController.unSetOnRestartRildNvMatch(0, this.mHandler);
            this.mVSimController.unSetOnRestartRildNvMatch(1, this.mHandler);
        }
    }

    void onJudgeRestartRildNvMatchTimeout() {
        logd("onJudgeRestartRildNvMatchTimeout");
        removeMessageAndStopListen();
        afterJudgeRestartRildNvMatch();
    }

    void onRadioAvailable(Message msg) {
        Integer index = HwVSimUtilsInner.getCiIndex(msg);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRadioAvailable, index = ");
        stringBuilder.append(index);
        logd(stringBuilder.toString());
        if (index.intValue() < 0 || index.intValue() >= HwVSimModemAdapter.MAX_SUB_COUNT) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("onRadioAvailable: Invalid index : ");
            stringBuilder.append(index);
            stringBuilder.append(" received with event ");
            stringBuilder.append(msg.what);
            logd(stringBuilder.toString());
        } else if (this.mIsWaitingRestartRild) {
            setRadioAvailableMark(index.intValue(), true);
            if (HwVSimUtilsInner.isPlatformTwoModems()) {
                int unavailableSlotId = this.mVSimController.getSimSlotTableLastSlotId();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("onRadioAvailable, [2 modems] sub ");
                stringBuilder2.append(unavailableSlotId);
                stringBuilder2.append(" is unavailable, ignore it.");
                logd(stringBuilder2.toString());
                setRadioAvailableMark(unavailableSlotId, true);
            }
            if (isAllRadioAvailable()) {
                afterJudgeRestartRildNvMatch();
            }
        } else {
            logd("onRadioAvailable, not waiting restart rild, return.");
        }
    }

    protected void afterJudgeRestartRildNvMatch() {
        logd("afterJudgeRestartRildNvMatch - do nothing.");
    }

    void restartRild() {
        for (int i = 0; i < HwVSimModemAdapter.MAX_SUB_COUNT; i++) {
            setRadioAvailableMark(i, false);
        }
        HwVSimSlotSwitchController.getInstance().restartRildBySubState();
    }

    private void setRadioAvailableMark(int subId, boolean available) {
        if (this.mRadioAvailableMark == null) {
            this.mRadioAvailableMark = new boolean[HwVSimModemAdapter.MAX_SUB_COUNT];
        }
        if (subId >= 0 && subId < this.mRadioAvailableMark.length) {
            this.mRadioAvailableMark[subId] = available;
        }
    }

    private boolean isAllRadioAvailable() {
        if (this.mRadioAvailableMark == null) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isAllRadioAvailable: ");
        stringBuilder.append(Arrays.toString(this.mRadioAvailableMark));
        logd(stringBuilder.toString());
        for (boolean z : this.mRadioAvailableMark) {
            if (!z) {
                return false;
            }
        }
        return true;
    }
}
