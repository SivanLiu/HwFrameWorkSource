package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy.StartingSurface;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppWindowContainerController$8qyUV78Is6_I1WVMp6w8VGpeuOE implements Runnable {
    private final /* synthetic */ StartingSurface f$0;

    public /* synthetic */ -$$Lambda$AppWindowContainerController$8qyUV78Is6_I1WVMp6w8VGpeuOE(StartingSurface startingSurface) {
        this.f$0 = startingSurface;
    }

    public final void run() {
        AppWindowContainerController.lambda$removeStartingWindow$2(this.f$0);
    }
}
