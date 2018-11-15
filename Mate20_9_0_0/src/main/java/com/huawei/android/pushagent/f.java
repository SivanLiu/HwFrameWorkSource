package com.huawei.android.pushagent;

final class f implements Runnable {
    final /* synthetic */ PushService jv;

    f(PushService pushService) {
        this.jv = pushService;
    }

    public void run() {
        this.jv.abx();
    }
}
