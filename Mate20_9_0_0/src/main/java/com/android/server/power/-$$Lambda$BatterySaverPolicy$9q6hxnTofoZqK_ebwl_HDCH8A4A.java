package com.android.server.power;

import com.android.server.power.BatterySaverPolicy.BatterySaverPolicyListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatterySaverPolicy$9q6hxnTofoZqK_ebwl_HDCH8A4A implements Runnable {
    private final /* synthetic */ BatterySaverPolicy f$0;
    private final /* synthetic */ BatterySaverPolicyListener[] f$1;

    public /* synthetic */ -$$Lambda$BatterySaverPolicy$9q6hxnTofoZqK_ebwl_HDCH8A4A(BatterySaverPolicy batterySaverPolicy, BatterySaverPolicyListener[] batterySaverPolicyListenerArr) {
        this.f$0 = batterySaverPolicy;
        this.f$1 = batterySaverPolicyListenerArr;
    }

    public final void run() {
        BatterySaverPolicy.lambda$refreshSettings$1(this.f$0, this.f$1);
    }
}
