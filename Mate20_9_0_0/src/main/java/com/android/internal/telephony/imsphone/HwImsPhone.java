package com.android.internal.telephony.imsphone;

import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.ims.ImsException;
import com.android.ims.ImsUtInterface;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;

public class HwImsPhone extends Handler {
    private static final int EVENT_GET_CLIR_DONE = 102;
    private static final int EVENT_HW_IMS = 100;
    private static final int EVENT_SET_CALL_FORWARD_TIMER_DONE = 103;
    private static final int EVENT_SET_CLIR_DONE = 101;
    private static final String LOG_TAG = "HwImsPhone";
    private boolean isBusy = false;
    boolean isImsEvent;
    ImsPhoneCallTracker mCT;

    private static class Cf {
        final boolean mIsCfu;
        final Message mOnComplete;
        final String mSetCfNumber;

        Cf(String cfNumber, boolean isCfu, Message onComplete) {
            this.mSetCfNumber = cfNumber;
            this.mIsCfu = isCfu;
            this.mOnComplete = onComplete;
        }

        String getCfNumber() {
            return this.mSetCfNumber;
        }

        boolean getIsCfu() {
            return this.mIsCfu;
        }
    }

    public HwImsPhone(ImsPhoneCallTracker ct) {
        this.mCT = ct;
    }

    public void handleMessage(Message msg) {
        AsyncResult ar = msg.obj;
        this.isImsEvent = true;
        switch (msg.what) {
            case 101:
                sendResponse((Message) ar.userObj, null, ar.exception);
                break;
            case 102:
                if (!(ar.result instanceof Bundle)) {
                    sendResponse((Message) ar.userObj, ar.result, ar.exception);
                    break;
                }
                int[] clirInfo = null;
                Bundle ssInfo = ar.result;
                if (ssInfo != null) {
                    clirInfo = ssInfo.getIntArray(ImsPhoneMmiCode.UT_BUNDLE_KEY_CLIR);
                }
                sendResponse((Message) ar.userObj, clirInfo, ar.exception);
                break;
            case EVENT_SET_CALL_FORWARD_TIMER_DONE /*103*/:
                sendResponse(ar.userObj.mOnComplete, null, ar.exception);
                break;
            default:
                this.isImsEvent = false;
                break;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMessage what=");
        stringBuilder.append(msg.what);
        stringBuilder.append(", isImsEvent = ");
        stringBuilder.append(this.isImsEvent);
        Rlog.d(str, stringBuilder.toString());
    }

    public void getOutgoingCallerIdDisplay(Message onComplete) {
        Rlog.d(LOG_TAG, "getCLIR");
        try {
            this.mCT.getUtInterface().queryCLIR(obtainMessage(102, onComplete));
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    public void setOutgoingCallerIdDisplay(int clirMode, Message onComplete) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCLIR action= ");
        stringBuilder.append(clirMode);
        Rlog.d(str, stringBuilder.toString());
        try {
            this.mCT.getUtInterface().updateCLIR(clirMode, obtainMessage(101, onComplete));
        } catch (ImsException e) {
            sendErrorResponse(onComplete, e);
        }
    }

    public boolean isSupportCFT() {
        Rlog.d(LOG_TAG, "isSupportCFT");
        try {
            return this.mCT.getUtInterface().isSupportCFT();
        } catch (ImsException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("e=");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public boolean isUtEnable() {
        boolean z = false;
        try {
            ImsUtInterface ut = this.mCT.getUtInterface();
            if (this.mCT.getPhoneType() == 6 && !this.mCT.isVolteEnabledByPlatform()) {
                return false;
            }
            if (ut.isUtEnable() || this.mCT.isUtEnabledForQcom()) {
                z = true;
            }
            return z;
        } catch (ImsException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsException e=");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void setCallForwardingUncondTimerOption(int startHour, int startMinute, int endHour, int endMinute, int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, Message onComplete) {
        int i = commandInterfaceCFAction;
        int i2 = commandInterfaceCFReason;
        Message message = onComplete;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setCallForwardingUncondTimerOption action=");
        stringBuilder.append(i);
        stringBuilder.append(", reason=");
        stringBuilder.append(i2);
        stringBuilder.append(", startHour=");
        int i3 = startHour;
        stringBuilder.append(i3);
        stringBuilder.append(", startMinute=");
        int i4 = startMinute;
        stringBuilder.append(i4);
        stringBuilder.append(", endHour=");
        int i5 = endHour;
        stringBuilder.append(i5);
        stringBuilder.append(", endMinute=");
        int i6 = endMinute;
        stringBuilder.append(i6);
        Rlog.d(str, stringBuilder.toString());
        if (isValidCommandInterfaceCFAction(i) && isValidCommandInterfaceCFReason(i2)) {
            try {
                this.mCT.getUtInterface().updateCallForwardUncondTimer(i3, i4, i5, i6, getActionFromCFAction(i), getConditionFromCFReason(i2), dialingNumber, obtainMessage(EVENT_SET_CALL_FORWARD_TIMER_DONE, isCfEnable(i), 0, new Cf(dialingNumber, i2 == 0, message)));
            } catch (ImsException e) {
                sendErrorResponse(message, e);
            }
        } else if (message != null) {
            sendErrorResponse(message);
        }
    }

    public Message popUtMessage(int id) {
        try {
            return this.mCT.getUtInterface().popUtMessage(id);
        } catch (ImsException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImsException e=");
            stringBuilder.append(e);
            Rlog.e(str, stringBuilder.toString());
            return null;
        }
    }

    void sendErrorResponse(Message onComplete, Throwable e) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, null, getCommandException(e));
            onComplete.sendToTarget();
        }
    }

    private boolean isValidCommandInterfaceCFReason(int commandInterfaceCFReason) {
        switch (commandInterfaceCFReason) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    private boolean isValidCommandInterfaceCFAction(int commandInterfaceCFAction) {
        switch (commandInterfaceCFAction) {
            case 0:
            case 1:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    private void sendResponse(Message onComplete, Object result, Throwable e) {
        if (onComplete != null) {
            if (e != null) {
                AsyncResult.forMessage(onComplete, result, getCommandException(e));
            } else {
                AsyncResult.forMessage(onComplete, result, null);
            }
            onComplete.sendToTarget();
        }
    }

    void sendErrorResponse(Message onComplete) {
        Rlog.d(LOG_TAG, "sendErrorResponse");
        if (onComplete != null) {
            AsyncResult.forMessage(onComplete, null, new CommandException(Error.GENERIC_FAILURE));
            onComplete.sendToTarget();
        }
    }

    CommandException getCommandException(int code) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCommandException code=");
        stringBuilder.append(code);
        Rlog.d(str, stringBuilder.toString());
        Error error = Error.GENERIC_FAILURE;
        if (code == 801) {
            error = Error.REQUEST_NOT_SUPPORTED;
        } else if (code == 821) {
            error = Error.PASSWORD_INCORRECT;
        } else if (code == 831) {
            error = Error.UT_NO_CONNECTION;
        }
        return new CommandException(error);
    }

    CommandException getCommandException(Throwable e) {
        if (e instanceof ImsException) {
            return getCommandException(((ImsException) e).getCode());
        }
        if (e instanceof CommandException) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("e instanceof CommandException  : ");
            stringBuilder.append(e);
            Rlog.d(str, stringBuilder.toString());
            return new CommandException(((CommandException) e).getCommandError());
        }
        Rlog.d(LOG_TAG, "getCommandException generic failure");
        return new CommandException(Error.GENERIC_FAILURE);
    }

    public boolean beforeHandleMessage(Message msg) {
        handleMessage(msg);
        return this.isImsEvent;
    }

    private boolean isCfEnable(int action) {
        return action == 1 || action == 3;
    }

    private int getActionFromCFAction(int action) {
        switch (action) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                return -1;
        }
    }

    private int getConditionFromCFReason(int reason) {
        switch (reason) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return -1;
        }
    }

    public void setIsBusy(boolean isImsPhoneBusy) {
        this.isBusy = isImsPhoneBusy;
    }

    public boolean isBusy() {
        return this.isBusy;
    }

    public void disableUTForQcom() {
        this.mCT.disableUTForQcom();
    }

    public void enableUTForQcom() {
        this.mCT.enableUTForQcom();
    }
}
