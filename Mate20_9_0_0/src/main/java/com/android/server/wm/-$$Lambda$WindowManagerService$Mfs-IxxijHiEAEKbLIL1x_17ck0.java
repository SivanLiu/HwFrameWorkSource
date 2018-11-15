package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowManagerService$Mfs-IxxijHiEAEKbLIL1x_17ck0 implements Consumer {
    private final /* synthetic */ boolean f$0;

    public /* synthetic */ -$$Lambda$WindowManagerService$Mfs-IxxijHiEAEKbLIL1x_17ck0(boolean z) {
        this.f$0 = z;
    }

    public final void accept(Object obj) {
        ((WindowState) obj).setForceHideNonSystemOverlayWindowIfNeeded(this.f$0);
    }
}
