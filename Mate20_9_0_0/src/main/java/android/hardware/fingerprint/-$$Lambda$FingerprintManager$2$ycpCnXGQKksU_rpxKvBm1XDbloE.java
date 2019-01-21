package android.hardware.fingerprint;

import android.hardware.fingerprint.FingerprintManager.AnonymousClass2;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FingerprintManager$2$ycpCnXGQKksU_rpxKvBm1XDbloE implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;

    public /* synthetic */ -$$Lambda$FingerprintManager$2$ycpCnXGQKksU_rpxKvBm1XDbloE(AnonymousClass2 anonymousClass2) {
        this.f$0 = anonymousClass2;
    }

    public final void run() {
        FingerprintManager.this.sendAuthenticatedFailed();
    }
}
