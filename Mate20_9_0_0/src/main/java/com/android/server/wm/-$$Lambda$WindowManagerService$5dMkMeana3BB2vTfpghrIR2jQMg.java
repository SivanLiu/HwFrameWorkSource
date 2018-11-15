package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowManagerService$5dMkMeana3BB2vTfpghrIR2jQMg implements Runnable {
    private final /* synthetic */ WindowManagerService f$0;
    private final /* synthetic */ Runnable f$1;

    public /* synthetic */ -$$Lambda$WindowManagerService$5dMkMeana3BB2vTfpghrIR2jQMg(WindowManagerService windowManagerService, Runnable runnable) {
        this.f$0 = windowManagerService;
        this.f$1 = runnable;
    }

    public final void run() {
        WindowManagerService.lambda$notifyKeyguardFlagsChanged$1(this.f$0, this.f$1);
    }
}
