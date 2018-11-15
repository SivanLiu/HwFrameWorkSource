package com.android.server.am;

import com.android.server.wm.WindowManagerService;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$5hokEl5hcign5FXeGZdl53qh2zg implements Runnable {
    private final /* synthetic */ WindowManagerService f$0;

    public /* synthetic */ -$$Lambda$5hokEl5hcign5FXeGZdl53qh2zg(WindowManagerService windowManagerService) {
        this.f$0 = windowManagerService;
    }

    public final void run() {
        this.f$0.onOverlayChanged();
    }
}
