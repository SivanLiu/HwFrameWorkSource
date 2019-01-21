package android.hardware.biometrics;

import android.hardware.biometrics.BiometricPrompt.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BiometricPrompt$1$J5PqpiT8xZNiNN1gy9VraVgknaQ implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$BiometricPrompt$1$J5PqpiT8xZNiNN1gy9VraVgknaQ(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void run() {
        BiometricPrompt.this.mNegativeButtonInfo.listener.onClick(null, -2);
    }
}
