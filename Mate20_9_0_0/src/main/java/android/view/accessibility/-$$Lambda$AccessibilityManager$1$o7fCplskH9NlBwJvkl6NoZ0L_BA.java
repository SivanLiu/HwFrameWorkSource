package android.view.accessibility;

import android.view.accessibility.AccessibilityManager.AccessibilityServicesStateChangeListener;
import android.view.accessibility.AccessibilityManager.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManager$1$o7fCplskH9NlBwJvkl6NoZ0L_BA implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ AccessibilityServicesStateChangeListener f$1;

    public /* synthetic */ -$$Lambda$AccessibilityManager$1$o7fCplskH9NlBwJvkl6NoZ0L_BA(AnonymousClass1 anonymousClass1, AccessibilityServicesStateChangeListener accessibilityServicesStateChangeListener) {
        this.f$0 = anonymousClass1;
        this.f$1 = accessibilityServicesStateChangeListener;
    }

    public final void run() {
        this.f$1.onAccessibilityServicesStateChanged(AccessibilityManager.this);
    }
}
