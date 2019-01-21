package android.net;

import android.net.IpSecTransform.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpSecTransform$1$Rc3lbWP51o1kJRHwkpVUEV1G_d8 implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$IpSecTransform$1$Rc3lbWP51o1kJRHwkpVUEV1G_d8(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void run() {
        IpSecTransform.this.mUserKeepaliveCallback.onStopped();
    }
}
