package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.AsyncResult;
import android.os.Message;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandException.Error;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.euicc.async.AsyncMessageInvocation;

class OpenLogicalChannelInvocation extends AsyncMessageInvocation<String, IccOpenLogicalChannelResponse> {
    private static final String LOG_TAG = "OpenChan";
    private final CommandsInterface mCi;

    OpenLogicalChannelInvocation(CommandsInterface ci) {
        this.mCi = ci;
    }

    protected void sendRequestMessage(String aid, Message msg) {
        this.mCi.iccOpenLogicalChannel(aid, 0, msg);
    }

    protected IccOpenLogicalChannelResponse parseResult(AsyncResult ar) {
        IccOpenLogicalChannelResponse openChannelResp;
        if (ar.exception != null || ar.result == null) {
            if (ar.result == null) {
                Rlog.e(LOG_TAG, "Empty response");
            }
            if (ar.exception != null) {
                Rlog.e(LOG_TAG, "Exception", ar.exception);
            }
            int errorCode = 4;
            if (ar.exception instanceof CommandException) {
                Error error = ((CommandException) ar.exception).getCommandError();
                if (error == Error.MISSING_RESOURCE) {
                    errorCode = 2;
                } else if (error == Error.NO_SUCH_ELEMENT) {
                    errorCode = 3;
                }
            }
            openChannelResp = new IccOpenLogicalChannelResponse(-1, errorCode, null);
        } else {
            int[] result = ar.result;
            int channel = result[0];
            byte[] selectResponse = null;
            if (result.length > 1) {
                selectResponse = new byte[(result.length - 1)];
                for (int i = 1; i < result.length; i++) {
                    selectResponse[i - 1] = (byte) result[i];
                }
            }
            openChannelResp = new IccOpenLogicalChannelResponse(channel, 1, selectResponse);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Response: ");
        stringBuilder.append(openChannelResp);
        Rlog.v(str, stringBuilder.toString());
        return openChannelResp;
    }
}
