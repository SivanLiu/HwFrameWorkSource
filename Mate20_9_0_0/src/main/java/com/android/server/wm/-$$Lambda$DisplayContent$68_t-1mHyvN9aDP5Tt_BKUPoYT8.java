package com.android.server.wm;

import com.android.server.policy.WindowManagerPolicy;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$68_t-1mHyvN9aDP5Tt_BKUPoYT8 implements Consumer {
    private final /* synthetic */ WindowManagerPolicy f$0;
    private final /* synthetic */ boolean f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$DisplayContent$68_t-1mHyvN9aDP5Tt_BKUPoYT8(WindowManagerPolicy windowManagerPolicy, boolean z, boolean z2) {
        this.f$0 = windowManagerPolicy;
        this.f$1 = z;
        this.f$2 = z2;
    }

    public final void accept(Object obj) {
        DisplayContent.lambda$startKeyguardExitOnNonAppWindows$19(this.f$0, this.f$1, this.f$2, (WindowState) obj);
    }
}
