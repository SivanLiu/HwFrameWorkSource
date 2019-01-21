package android.hardware.fingerprint;

import android.hardware.biometrics.BiometricAuthenticator.AuthenticationCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FingerprintManager$0Q_OnkqSSy_nQ9iUWqvqVi6QjNE implements Runnable {
    private final /* synthetic */ FingerprintManager f$0;
    private final /* synthetic */ AuthenticationCallback f$1;

    public /* synthetic */ -$$Lambda$FingerprintManager$0Q_OnkqSSy_nQ9iUWqvqVi6QjNE(FingerprintManager fingerprintManager, AuthenticationCallback authenticationCallback) {
        this.f$0 = fingerprintManager;
        this.f$1 = authenticationCallback;
    }

    public final void run() {
        this.f$1.onAuthenticationError(1, this.f$0.getErrorString(1, 0));
    }
}
