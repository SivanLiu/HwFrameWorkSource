package com.android.server.wm;

import android.graphics.Rect;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskPositioner$TE0EjYzJeOSFARmUlY6wF3y3c2U implements Runnable {
    private final /* synthetic */ TaskPositioner f$0;
    private final /* synthetic */ Rect f$1;

    public /* synthetic */ -$$Lambda$TaskPositioner$TE0EjYzJeOSFARmUlY6wF3y3c2U(TaskPositioner taskPositioner, Rect rect) {
        this.f$0 = taskPositioner;
        this.f$1 = rect;
    }

    public final void run() {
        TaskPositioner.lambda$startDrag$0(this.f$0, this.f$1);
    }
}
