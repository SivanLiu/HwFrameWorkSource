package android.accessibilityservice;

import android.accessibilityservice.FingerprintGestureController.FingerprintGestureCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FingerprintGestureController$BQjrQQom4K3C98FNiI0fi7SvHfY implements Runnable {
    private final /* synthetic */ FingerprintGestureCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$FingerprintGestureController$BQjrQQom4K3C98FNiI0fi7SvHfY(FingerprintGestureCallback fingerprintGestureCallback, int i) {
        this.f$0 = fingerprintGestureCallback;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.onGestureDetected(this.f$1);
    }
}
