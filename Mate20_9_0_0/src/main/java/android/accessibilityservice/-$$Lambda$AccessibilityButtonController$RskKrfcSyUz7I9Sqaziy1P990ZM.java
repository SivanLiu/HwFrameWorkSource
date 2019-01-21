package android.accessibilityservice;

import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityButtonController$RskKrfcSyUz7I9Sqaziy1P990ZM implements Runnable {
    private final /* synthetic */ AccessibilityButtonController f$0;
    private final /* synthetic */ AccessibilityButtonCallback f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$AccessibilityButtonController$RskKrfcSyUz7I9Sqaziy1P990ZM(AccessibilityButtonController accessibilityButtonController, AccessibilityButtonCallback accessibilityButtonCallback, boolean z) {
        this.f$0 = accessibilityButtonController;
        this.f$1 = accessibilityButtonCallback;
        this.f$2 = z;
    }

    public final void run() {
        this.f$1.onAvailabilityChanged(this.f$0, this.f$2);
    }
}
