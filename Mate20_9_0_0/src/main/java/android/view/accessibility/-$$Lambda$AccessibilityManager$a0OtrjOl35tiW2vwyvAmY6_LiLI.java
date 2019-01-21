package android.view.accessibility;

import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManager$a0OtrjOl35tiW2vwyvAmY6_LiLI implements Runnable {
    private final /* synthetic */ TouchExplorationStateChangeListener f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$AccessibilityManager$a0OtrjOl35tiW2vwyvAmY6_LiLI(TouchExplorationStateChangeListener touchExplorationStateChangeListener, boolean z) {
        this.f$0 = touchExplorationStateChangeListener;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.onTouchExplorationStateChanged(this.f$1);
    }
}
