package android.hardware.biometrics;

import android.hardware.biometrics.BiometricPrompt.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BiometricPrompt$1$C3fuslKNv7eJTZG9_jFRfCo5_Y4 implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;

    public /* synthetic */ -$$Lambda$BiometricPrompt$1$C3fuslKNv7eJTZG9_jFRfCo5_Y4(AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    public final void run() {
        BiometricPrompt.this.mPositiveButtonInfo.listener.onClick(null, -1);
    }
}
