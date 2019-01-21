package android.net;

import android.net.IpSecTransform.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpSecTransform$1$zl9bpxiE2uj_QuCOkuJ091wPuwo implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$IpSecTransform$1$zl9bpxiE2uj_QuCOkuJ091wPuwo(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void run() {
        IpSecTransform.this.mUserKeepaliveCallback.onStarted();
    }
}
