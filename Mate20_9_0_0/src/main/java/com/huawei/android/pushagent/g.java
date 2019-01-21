package com.huawei.android.pushagent;

final class g implements Runnable {
    final /* synthetic */ PushService jw;

    g(PushService pushService) {
        this.jw = pushService;
    }

    public void run() {
        this.jw.acb();
    }
}
