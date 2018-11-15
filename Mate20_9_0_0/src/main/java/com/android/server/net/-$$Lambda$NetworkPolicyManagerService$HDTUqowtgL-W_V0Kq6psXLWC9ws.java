package com.android.server.net;

import java.util.concurrent.CountDownLatch;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$NetworkPolicyManagerService$HDTUqowtgL-W_V0Kq6psXLWC9ws implements Runnable {
    private final /* synthetic */ NetworkPolicyManagerService f$0;
    private final /* synthetic */ CountDownLatch f$1;

    public /* synthetic */ -$$Lambda$NetworkPolicyManagerService$HDTUqowtgL-W_V0Kq6psXLWC9ws(NetworkPolicyManagerService networkPolicyManagerService, CountDownLatch countDownLatch) {
        this.f$0 = networkPolicyManagerService;
        this.f$1 = countDownLatch;
    }

    public final void run() {
        this.f$0.initService(this.f$1);
    }
}
