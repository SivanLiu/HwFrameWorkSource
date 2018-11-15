package com.huawei.android.pushagent;

import android.os.MessageQueue.IdleHandler;

final class g implements IdleHandler {
    final /* synthetic */ b jw;

    g(b bVar) {
        this.jw = bVar;
    }

    public boolean queueIdle() {
        this.jw.acj();
        return true;
    }
}
