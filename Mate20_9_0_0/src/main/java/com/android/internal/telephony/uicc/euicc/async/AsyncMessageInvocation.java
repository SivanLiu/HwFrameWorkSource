package com.android.internal.telephony.uicc.euicc.async;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

public abstract class AsyncMessageInvocation<Request, Response> implements Callback {
    protected abstract Response parseResult(AsyncResult asyncResult) throws Throwable;

    protected abstract void sendRequestMessage(Request request, Message message);

    public final void invoke(Request request, AsyncResultCallback<Response> resultCallback, Handler handler) {
        sendRequestMessage(request, new Handler(handler.getLooper(), this).obtainMessage(0, resultCallback));
    }

    public boolean handleMessage(Message msg) {
        AsyncResult result = msg.obj;
        AsyncResultCallback<Response> resultCallback = result.userObj;
        try {
            resultCallback.onResult(parseResult(result));
        } catch (Throwable t) {
            resultCallback.onException(t);
        }
        return true;
    }
}
