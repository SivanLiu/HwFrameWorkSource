package com.android.server.am;

import android.hardware.display.DisplayManagerInternal;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityManagerService$gATL8uvTPRd405IfefK1RL9bNqA implements Runnable {
    private final /* synthetic */ DisplayManagerInternal f$0;

    public /* synthetic */ -$$Lambda$ActivityManagerService$gATL8uvTPRd405IfefK1RL9bNqA(DisplayManagerInternal displayManagerInternal) {
        this.f$0 = displayManagerInternal;
    }

    public final void run() {
        this.f$0.onOverlayChanged();
    }
}
