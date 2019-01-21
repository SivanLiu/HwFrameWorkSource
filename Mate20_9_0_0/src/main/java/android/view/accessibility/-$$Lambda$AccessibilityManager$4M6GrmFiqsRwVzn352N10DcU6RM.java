package android.view.accessibility;

import android.view.accessibility.AccessibilityManager.HighTextContrastChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManager$4M6GrmFiqsRwVzn352N10DcU6RM implements Runnable {
    private final /* synthetic */ HighTextContrastChangeListener f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$AccessibilityManager$4M6GrmFiqsRwVzn352N10DcU6RM(HighTextContrastChangeListener highTextContrastChangeListener, boolean z) {
        this.f$0 = highTextContrastChangeListener;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.onHighTextContrastStateChanged(this.f$1);
    }
}
