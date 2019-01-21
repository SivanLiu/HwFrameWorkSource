package com.android.internal.telephony.vsim.process;

import android.os.AsyncResult;
import android.os.Message;
import com.android.internal.telephony.vsim.HwVSimController;
import com.android.internal.telephony.vsim.HwVSimController.ProcessAction;
import com.android.internal.telephony.vsim.HwVSimController.ProcessState;
import com.android.internal.telephony.vsim.HwVSimController.ProcessType;
import com.android.internal.telephony.vsim.HwVSimModemAdapter;
import com.android.internal.telephony.vsim.HwVSimRequest;
import com.android.internal.telephony.vsim.HwVSimUtilsInner;

public abstract class HwVSimProcessor {
    static final boolean HANDLED = true;
    static final boolean NOT_HANDLED = false;
    protected HwVSimModemAdapter mModemAdapter;
    protected HwVSimRequest mRequest;

    public abstract void doProcessException(AsyncResult asyncResult, HwVSimRequest hwVSimRequest);

    protected abstract void logd(String str);

    public abstract Message obtainMessage(int i, Object obj);

    public abstract void onEnter();

    public abstract void onExit();

    public abstract boolean processMessage(Message message);

    public abstract void transitionToState(int i);

    public HwVSimProcessor(HwVSimModemAdapter modemAdapter, HwVSimRequest request) {
        this.mModemAdapter = modemAdapter;
        this.mRequest = request;
    }

    public void notifyResult(HwVSimRequest request, Object result) {
        if (request != null) {
            request.setResult(result);
            request.doNotify();
        }
    }

    public void doEnableProcessException(AsyncResult ar, HwVSimRequest request, Object cause) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("doEnableProcessException : ");
        stringBuilder.append(cause);
        logd(stringBuilder.toString());
        if (!(ar == null || ar.exception == null)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("error, exception ");
            stringBuilder.append(ar.exception);
            logd(stringBuilder.toString());
        }
        notifyResult(request, cause);
        transitionToState(0);
    }

    public void doDisableProcessException(AsyncResult ar, HwVSimRequest request) {
        if (!(ar == null || ar.exception == null)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error, exception ");
            stringBuilder.append(ar.exception);
            logd(stringBuilder.toString());
        }
        notifyResult(request, Boolean.valueOf(false));
        transitionToState(0);
    }

    public void doReconnectProcessException(AsyncResult ar, HwVSimRequest request) {
        transitionToState(0);
    }

    public void doSwitchModeProcessException(AsyncResult ar, HwVSimRequest request) {
        if (!(ar == null || ar.exception == null)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error, exception ");
            stringBuilder.append(ar.exception);
            logd(stringBuilder.toString());
        }
        notifyResult(request, Boolean.valueOf(false));
        transitionToState(0);
    }

    public boolean isAsyncResultValid(AsyncResult ar) {
        if (ar == null) {
            doProcessException(null, null);
            return false;
        }
        HwVSimRequest request = ar.userObj;
        if (request == null) {
            return false;
        }
        if (ar.exception == null) {
            return true;
        }
        doProcessException(ar, request);
        return false;
    }

    public boolean isAsyncResultValid(AsyncResult ar, Object cause) {
        if (ar == null) {
            doEnableProcessException(null, null, cause);
            return false;
        }
        HwVSimRequest request = ar.userObj;
        if (request == null) {
            return false;
        }
        if (ar.exception == null) {
            return true;
        }
        doEnableProcessException(ar, request, cause);
        return false;
    }

    public boolean isAsyncResultValidForRequestNotSupport(AsyncResult ar) {
        if (ar == null) {
            return false;
        }
        HwVSimRequest request = ar.userObj;
        if (request == null) {
            return false;
        }
        boolean noError = true;
        if (ar.exception != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error, exception ");
            stringBuilder.append(ar.exception);
            logd(stringBuilder.toString());
            if (!isRequestNotSupport(ar.exception)) {
                noError = false;
            }
        }
        if (noError) {
            return true;
        }
        doProcessException(ar, request);
        return false;
    }

    public boolean isAsyncResultValidNoProcessException(AsyncResult ar) {
        if (ar == null) {
            return false;
        }
        if (ar.exception != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error, exception ");
            stringBuilder.append(ar.exception);
            logd(stringBuilder.toString());
        }
        return true;
    }

    public void unhandledMessage(Message msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" - unhandledMessage: msg.what=");
        stringBuilder.append(msg.what);
        logd(stringBuilder.toString());
    }

    public void setIsVSimOn(boolean isVSimOn) {
        HwVSimController.getInstance().setIsVSimOn(isVSimOn);
    }

    public void setProcessAction(ProcessAction action) {
        HwVSimController.getInstance().setProcessAction(action);
    }

    public void setProcessType(ProcessType type) {
        HwVSimController.getInstance().setProcessType(type);
    }

    public void setProcessState(ProcessState state) {
        HwVSimController.getInstance().setProcessState(state);
    }

    public boolean isSwapProcess() {
        return HwVSimController.getInstance().isSwapProcess();
    }

    public boolean isCrossProcess() {
        return HwVSimController.getInstance().isCrossProcess();
    }

    public boolean isDirectProcess() {
        return HwVSimController.getInstance().isDirectProcess();
    }

    public boolean isEnableProcess() {
        return HwVSimController.getInstance().isEnableProcess();
    }

    public boolean isDisableProcess() {
        return HwVSimController.getInstance().isDisableProcess();
    }

    public boolean isReconnectProcess() {
        return HwVSimController.getInstance().isReconnectProcess();
    }

    public boolean isSwitchModeProcess() {
        return HwVSimController.getInstance().isSwitchModeProcess();
    }

    public boolean isWorkProcess() {
        return HwVSimController.getInstance().isWorkProcess();
    }

    public boolean isReadyProcess() {
        return HwVSimController.getInstance().isReadyProcess();
    }

    public boolean isRequestNotSupport(Throwable ex) {
        return HwVSimUtilsInner.isRequestNotSupport(ex);
    }

    public boolean isAllMarkClear(HwVSimRequest request) {
        if (request == null) {
            return false;
        }
        return request.isAllMarkClear();
    }

    protected boolean isNeedWaitNvCfgMatchAndRestartRild() {
        return isCrossProcess() && HwVSimUtilsInner.isPlatformNeedWaitNvMatchUnsol() && HwVSimController.getInstance().getInsertedCardCount() != 0;
    }

    protected boolean isMessageShouldDeal(Message msg, int current) {
        if (msg == null) {
            logd("msg is null, ignore this event.");
            return false;
        } else if (msg.obj == null || !(msg.obj instanceof AsyncResult)) {
            return true;
        } else {
            AsyncResult ar = msg.obj;
            if (ar.userObj == null || !(ar.userObj instanceof HwVSimRequest)) {
                return true;
            }
            HwVSimRequest request = ar.userObj;
            StringBuilder stringBuilder;
            if (request.getSource() != 0 && current != request.getSource()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("request source is ");
                stringBuilder.append(request.getSource());
                stringBuilder.append(", ignore it.");
                logd(stringBuilder.toString());
                return false;
            } else if (request.getSource() == 0) {
                return true;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("request source is ");
                stringBuilder.append(request.getSource());
                stringBuilder.append(", pass.");
                logd(stringBuilder.toString());
                return true;
            }
        }
    }
}
