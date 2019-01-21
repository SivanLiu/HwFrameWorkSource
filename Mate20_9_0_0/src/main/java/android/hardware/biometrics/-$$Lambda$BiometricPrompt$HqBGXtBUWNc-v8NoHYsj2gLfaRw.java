package android.hardware.biometrics;

import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BiometricPrompt$HqBGXtBUWNc-v8NoHYsj2gLfaRw implements Runnable {
    private final /* synthetic */ BiometricPrompt f$0;
    private final /* synthetic */ AuthenticationCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$BiometricPrompt$HqBGXtBUWNc-v8NoHYsj2gLfaRw(BiometricPrompt biometricPrompt, AuthenticationCallback authenticationCallback, int i) {
        this.f$0 = biometricPrompt;
        this.f$1 = authenticationCallback;
        this.f$2 = i;
    }

    public final void run() {
        this.f$1.onAuthenticationError(this.f$2, this.f$0.mFingerprintManager.getErrorString(this.f$2, 0));
    }
}
