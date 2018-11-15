package com.android.server.wm;

import android.util.SparseArray;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityController$WindowsForAccessibilityObserver$vRhBz0DqTZWNemKfoIyId7HacTk implements Consumer {
    private final /* synthetic */ WindowsForAccessibilityObserver f$0;
    private final /* synthetic */ SparseArray f$1;

    public /* synthetic */ -$$Lambda$AccessibilityController$WindowsForAccessibilityObserver$vRhBz0DqTZWNemKfoIyId7HacTk(WindowsForAccessibilityObserver windowsForAccessibilityObserver, SparseArray sparseArray) {
        this.f$0 = windowsForAccessibilityObserver;
        this.f$1 = sparseArray;
    }

    public final void accept(Object obj) {
        WindowsForAccessibilityObserver.lambda$populateVisibleWindowsOnScreenLocked$0(this.f$0, this.f$1, (WindowState) obj);
    }
}
