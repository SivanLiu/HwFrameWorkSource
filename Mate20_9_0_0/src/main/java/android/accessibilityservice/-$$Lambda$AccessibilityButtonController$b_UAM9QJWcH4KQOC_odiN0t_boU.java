package android.accessibilityservice;

import android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityButtonController$b_UAM9QJWcH4KQOC_odiN0t_boU implements Runnable {
    private final /* synthetic */ AccessibilityButtonController f$0;
    private final /* synthetic */ AccessibilityButtonCallback f$1;

    public /* synthetic */ -$$Lambda$AccessibilityButtonController$b_UAM9QJWcH4KQOC_odiN0t_boU(AccessibilityButtonController accessibilityButtonController, AccessibilityButtonCallback accessibilityButtonCallback) {
        this.f$0 = accessibilityButtonController;
        this.f$1 = accessibilityButtonCallback;
    }

    public final void run() {
        this.f$1.onClicked(this.f$0);
    }
}
