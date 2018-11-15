package com.android.server.wm;

import android.content.Context;
import com.android.server.input.InputManagerService;
import com.android.server.policy.WindowManagerPolicy;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowManagerService$qOaUiWHWefHk1N5K-T4WND2mknQ implements Runnable {
    private final /* synthetic */ Context f$0;
    private final /* synthetic */ InputManagerService f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ boolean f$3;
    private final /* synthetic */ boolean f$4;
    private final /* synthetic */ WindowManagerPolicy f$5;

    public /* synthetic */ -$$Lambda$WindowManagerService$qOaUiWHWefHk1N5K-T4WND2mknQ(Context context, InputManagerService inputManagerService, boolean z, boolean z2, boolean z3, WindowManagerPolicy windowManagerPolicy) {
        this.f$0 = context;
        this.f$1 = inputManagerService;
        this.f$2 = z;
        this.f$3 = z2;
        this.f$4 = z3;
        this.f$5 = windowManagerPolicy;
    }

    public final void run() {
        WindowManagerService.lambda$main$0(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5);
    }
}
