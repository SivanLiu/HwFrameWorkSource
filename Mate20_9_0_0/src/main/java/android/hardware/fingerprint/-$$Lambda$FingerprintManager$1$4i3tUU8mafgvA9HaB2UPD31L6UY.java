package android.hardware.fingerprint;

import android.hardware.fingerprint.FingerprintManager.AnonymousClass1;
import android.hardware.fingerprint.FingerprintManager.LockoutResetCallback;
import android.os.PowerManager.WakeLock;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FingerprintManager$1$4i3tUU8mafgvA9HaB2UPD31L6UY implements Runnable {
    private final /* synthetic */ LockoutResetCallback f$0;
    private final /* synthetic */ WakeLock f$1;

    public /* synthetic */ -$$Lambda$FingerprintManager$1$4i3tUU8mafgvA9HaB2UPD31L6UY(LockoutResetCallback lockoutResetCallback, WakeLock wakeLock) {
        this.f$0 = lockoutResetCallback;
        this.f$1 = wakeLock;
    }

    public final void run() {
        AnonymousClass1.lambda$onLockoutReset$0(this.f$0, this.f$1);
    }
}
