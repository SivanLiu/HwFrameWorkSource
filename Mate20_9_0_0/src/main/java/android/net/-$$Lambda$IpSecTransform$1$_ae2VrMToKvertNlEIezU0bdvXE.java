package android.net;

import android.net.IpSecTransform.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpSecTransform$1$_ae2VrMToKvertNlEIezU0bdvXE implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$IpSecTransform$1$_ae2VrMToKvertNlEIezU0bdvXE(AnonymousClass1 anonymousClass1, int i) {
        this.f$0 = anonymousClass1;
        this.f$1 = i;
    }

    public final void run() {
        IpSecTransform.this.mUserKeepaliveCallback.onError(this.f$1);
    }
}
