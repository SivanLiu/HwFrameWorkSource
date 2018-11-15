package com.android.server.dreams;

import android.content.ComponentName;
import android.os.Binder;
import android.os.PowerManager.WakeLock;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DreamManagerService$82T4r6I6KlFJCB_Je9yZ35l4Bg4 implements Runnable {
    private final /* synthetic */ DreamManagerService f$0;
    private final /* synthetic */ Binder f$1;
    private final /* synthetic */ ComponentName f$2;
    private final /* synthetic */ boolean f$3;
    private final /* synthetic */ int f$4;
    private final /* synthetic */ boolean f$5;
    private final /* synthetic */ WakeLock f$6;

    public /* synthetic */ -$$Lambda$DreamManagerService$82T4r6I6KlFJCB_Je9yZ35l4Bg4(DreamManagerService dreamManagerService, Binder binder, ComponentName componentName, boolean z, int i, boolean z2, WakeLock wakeLock) {
        this.f$0 = dreamManagerService;
        this.f$1 = binder;
        this.f$2 = componentName;
        this.f$3 = z;
        this.f$4 = i;
        this.f$5 = z2;
        this.f$6 = wakeLock;
    }

    public final void run() {
        DreamManagerService.lambda$startDreamLocked$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6);
    }
}
