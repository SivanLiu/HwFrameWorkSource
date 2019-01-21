package android.hardware.fingerprint;

import android.hardware.fingerprint.FingerprintManager.AnonymousClass2;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FingerprintManager$2$O5sigT8DLDwmCzdvD-k13MacOBU implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;
    private final /* synthetic */ Fingerprint f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$FingerprintManager$2$O5sigT8DLDwmCzdvD-k13MacOBU(AnonymousClass2 anonymousClass2, Fingerprint fingerprint, int i) {
        this.f$0 = anonymousClass2;
        this.f$1 = fingerprint;
        this.f$2 = i;
    }

    public final void run() {
        FingerprintManager.this.sendAuthenticatedSucceeded(this.f$1, this.f$2);
    }
}
