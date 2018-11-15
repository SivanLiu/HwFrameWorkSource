package com.android.server.wm;

import android.graphics.Rect;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PinnedStackWindowController$x7R9b-0MaS9BJmen-irckXpBNyg implements Runnable {
    private final /* synthetic */ PinnedStackWindowController f$0;
    private final /* synthetic */ Rect f$1;
    private final /* synthetic */ Rect f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ int f$4;
    private final /* synthetic */ boolean f$5;
    private final /* synthetic */ boolean f$6;

    public /* synthetic */ -$$Lambda$PinnedStackWindowController$x7R9b-0MaS9BJmen-irckXpBNyg(PinnedStackWindowController pinnedStackWindowController, Rect rect, Rect rect2, int i, int i2, boolean z, boolean z2) {
        this.f$0 = pinnedStackWindowController;
        this.f$1 = rect;
        this.f$2 = rect2;
        this.f$3 = i;
        this.f$4 = i2;
        this.f$5 = z;
        this.f$6 = z2;
    }

    public final void run() {
        PinnedStackWindowController.lambda$animateResizePinnedStack$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
    }
}
