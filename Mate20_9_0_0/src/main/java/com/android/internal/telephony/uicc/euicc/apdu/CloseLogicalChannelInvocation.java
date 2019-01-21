package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;

class CloseLogicalChannelInvocation extends AsyncMessageInvocation<Integer, Boolean> {
    private static final String LOG_TAG = "CloseChan";
    private final CommandsInterface mCi;

    CloseLogicalChannelInvocation(CommandsInterface ci) {
        this.mCi = ci;
    }

    protected void sendRequestMessage(Integer channel, Message msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Channel: ");
        stringBuilder.append(channel);
        Rlog.v(str, stringBuilder.toString());
        this.mCi.iccCloseLogicalChannel(channel.intValue(), msg);
    }

    protected Boolean parseResult(AsyncResult ar) {
        if (ar.exception == null) {
            return Boolean.valueOf(true);
        }
        if (ar.exception instanceof CommandException) {
            Rlog.e(LOG_TAG, "CommandException", ar.exception);
        } else {
            Rlog.e(LOG_TAG, "Unknown exception", ar.exception);
        }
        return Boolean.valueOf(false);
    }
}
