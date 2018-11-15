package com.huawei.nb.client.callback;

import android.util.ArraySet;
import java.util.Iterator;

public class CallbackManager {
    private final ArraySet<WaitableCallback> waitingCallbacks = new ArraySet();

    public void interruptAll() {
        synchronized (this) {
            Iterator it = this.waitingCallbacks.iterator();
            while (it.hasNext()) {
                ((WaitableCallback) it.next()).interrupt();
            }
            this.waitingCallbacks.clear();
        }
    }

    void startWaiting(WaitableCallback callback) {
        if (callback != null) {
            synchronized (this) {
                this.waitingCallbacks.add(callback);
            }
        }
    }

    void stopWaiting(WaitableCallback callback) {
        if (callback != null) {
            synchronized (this) {
                this.waitingCallbacks.remove(callback);
            }
        }
    }

    public FetchCallback createFetchCallback() {
        return new FetchCallback(this);
    }

    public InsertCallback createInsertCallback() {
        return new InsertCallback(this);
    }

    public UpdateCallback createUpdateCallback() {
        return new UpdateCallback(this);
    }

    public DeleteCallback createDeleteCallback() {
        return new DeleteCallback(this);
    }

    public SubscribeCallback createSubscribeCallback() {
        return new SubscribeCallback(this);
    }

    public SendEventCallback createSendEventCallback() {
        return new SendEventCallback(this);
    }
}
