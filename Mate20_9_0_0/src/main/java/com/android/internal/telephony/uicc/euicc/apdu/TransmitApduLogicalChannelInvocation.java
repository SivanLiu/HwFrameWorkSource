package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;

public class TransmitApduLogicalChannelInvocation extends AsyncMessageInvocation<ApduCommand, IccIoResult> {
    private static final String LOG_TAG = "TransApdu";
    private static final int SW1_ERROR = 111;
    private final CommandsInterface mCi;

    TransmitApduLogicalChannelInvocation(CommandsInterface ci) {
        this.mCi = ci;
    }

    protected void sendRequestMessage(ApduCommand command, Message msg) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Send: ");
        stringBuilder.append(command);
        Rlog.v(str, stringBuilder.toString());
        this.mCi.iccTransmitApduLogicalChannel(command.channel, command.cla | command.channel, command.ins, command.p1, command.p2, command.p3, command.cmdHex, msg);
    }

    protected IccIoResult parseResult(AsyncResult ar) {
        IccIoResult response;
        if (ar.exception != null || ar.result == null) {
            if (ar.result == null) {
                Rlog.e(LOG_TAG, "Empty response");
            } else if (ar.exception instanceof CommandException) {
                Rlog.e(LOG_TAG, "CommandException", ar.exception);
            } else {
                Rlog.e(LOG_TAG, "CommandException", ar.exception);
            }
            response = new IccIoResult(111, 0, (byte[]) null);
        } else {
            response = ar.result;
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Response: ");
        stringBuilder.append(response);
        Rlog.v(str, stringBuilder.toString());
        return response;
    }
}
