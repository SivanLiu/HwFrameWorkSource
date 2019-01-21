package android.os;

import android.os.StrictMode.OnVmViolationListener;
import android.os.strictmode.Violation;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StrictMode$UFC_nI1x6u8ZwMQmA7bmj9NHZz4 implements Runnable {
    private final /* synthetic */ OnVmViolationListener f$0;
    private final /* synthetic */ Violation f$1;

    public /* synthetic */ -$$Lambda$StrictMode$UFC_nI1x6u8ZwMQmA7bmj9NHZz4(OnVmViolationListener onVmViolationListener, Violation violation) {
        this.f$0 = onVmViolationListener;
        this.f$1 = violation;
    }

    public final void run() {
        StrictMode.lambda$onVmPolicyViolation$3(this.f$0, this.f$1);
    }
}
