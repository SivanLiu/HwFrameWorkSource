package com.huawei.android.pushagent;

import android.os.MessageQueue.IdleHandler;

final class g implements IdleHandler {
    final /* synthetic */ b jr;

    g(b bVar) {
        this.jr = bVar;
    }

    public boolean queueIdle() {
        this.jr.abr();
        return true;
    }
}
