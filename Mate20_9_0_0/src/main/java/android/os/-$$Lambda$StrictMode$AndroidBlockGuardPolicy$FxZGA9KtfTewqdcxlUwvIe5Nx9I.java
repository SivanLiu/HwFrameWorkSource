package android.os;

import android.os.StrictMode.OnThreadViolationListener;
import android.os.strictmode.Violation;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StrictMode$AndroidBlockGuardPolicy$FxZGA9KtfTewqdcxlUwvIe5Nx9I implements Runnable {
    private final /* synthetic */ OnThreadViolationListener f$0;
    private final /* synthetic */ Violation f$1;

    public /* synthetic */ -$$Lambda$StrictMode$AndroidBlockGuardPolicy$FxZGA9KtfTewqdcxlUwvIe5Nx9I(OnThreadViolationListener onThreadViolationListener, Violation violation) {
        this.f$0 = onThreadViolationListener;
        this.f$1 = violation;
    }

    public final void run() {
        AndroidBlockGuardPolicy.lambda$onThreadPolicyViolation$1(this.f$0, this.f$1);
    }
}
