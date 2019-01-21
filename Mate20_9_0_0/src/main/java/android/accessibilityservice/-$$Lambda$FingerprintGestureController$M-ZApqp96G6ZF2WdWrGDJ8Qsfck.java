package android.accessibilityservice;

import android.accessibilityservice.FingerprintGestureController.FingerprintGestureCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FingerprintGestureController$M-ZApqp96G6ZF2WdWrGDJ8Qsfck implements Runnable {
    private final /* synthetic */ FingerprintGestureCallback f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$FingerprintGestureController$M-ZApqp96G6ZF2WdWrGDJ8Qsfck(FingerprintGestureCallback fingerprintGestureCallback, boolean z) {
        this.f$0 = fingerprintGestureCallback;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.onGestureDetectionAvailabilityChanged(this.f$1);
    }
}
