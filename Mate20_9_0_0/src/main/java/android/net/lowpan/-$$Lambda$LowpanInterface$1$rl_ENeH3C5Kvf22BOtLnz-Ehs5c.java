package android.net.lowpan;

import android.net.lowpan.LowpanInterface.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanInterface$1$rl_ENeH3C5Kvf22BOtLnz-Ehs5c implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ LowpanIdentity f$1;

    public /* synthetic */ -$$Lambda$LowpanInterface$1$rl_ENeH3C5Kvf22BOtLnz-Ehs5c(Callback callback, LowpanIdentity lowpanIdentity) {
        this.f$0 = callback;
        this.f$1 = lowpanIdentity;
    }

    public final void run() {
        this.f$0.onLowpanIdentityChanged(this.f$1);
    }
}
