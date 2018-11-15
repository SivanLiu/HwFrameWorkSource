package com.huawei.nb.client.callback;

import android.os.RemoteException;
import com.huawei.nb.callback.ISubscribeCallback.Stub;

public class SubscribeCallback extends Stub implements WaitableCallback<Boolean> {
    private final CallbackManager callbackManager;
    private final CallbackWaiter<Boolean> callbackWaiter = new CallbackWaiter(Boolean.valueOf(false));

    SubscribeCallback(CallbackManager callbackManager) {
        this.callbackManager = callbackManager;
    }

    public void onSuccess(int transactionId) throws RemoteException {
        this.callbackWaiter.set(transactionId, Boolean.valueOf(true));
    }

    public void onFailure(int transactionId, String message) throws RemoteException {
        this.callbackWaiter.set(transactionId, Boolean.valueOf(false));
    }

    public void interrupt() {
        this.callbackWaiter.interrupt();
    }

    public Boolean await(int transactionId, long timeout) {
        this.callbackManager.startWaiting(this);
        Boolean result = (Boolean) this.callbackWaiter.await(transactionId, timeout);
        this.callbackManager.stopWaiting(this);
        return result;
    }
}
