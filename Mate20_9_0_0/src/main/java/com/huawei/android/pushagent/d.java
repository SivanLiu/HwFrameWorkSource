package com.huawei.android.pushagent;

import android.os.MessageQueue.IdleHandler;

final class d implements IdleHandler {
    final /* synthetic */ a jt;

    d(a aVar) {
        this.jt = aVar;
    }

    public boolean queueIdle() {
        this.jt.abq();
        return true;
    }
}
