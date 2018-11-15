package com.android.server.net;

import java.util.concurrent.CountDownLatch;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkPolicyManagerService$lv2qqWetKVoJzbe7z3LT5idTu54 implements Runnable {
    private final /* synthetic */ CountDownLatch f$0;

    public /* synthetic */ -$$Lambda$NetworkPolicyManagerService$lv2qqWetKVoJzbe7z3LT5idTu54(CountDownLatch countDownLatch) {
        this.f$0 = countDownLatch;
    }

    public final void run() {
        this.f$0.countDown();
    }
}
