package android.view.accessibility;

import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManager$yzw5NYY7_MfAQ9gLy3mVllchaXo implements Runnable {
    private final /* synthetic */ AccessibilityStateChangeListener f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$AccessibilityManager$yzw5NYY7_MfAQ9gLy3mVllchaXo(AccessibilityStateChangeListener accessibilityStateChangeListener, boolean z) {
        this.f$0 = accessibilityStateChangeListener;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.onAccessibilityStateChanged(this.f$1);
    }
}
