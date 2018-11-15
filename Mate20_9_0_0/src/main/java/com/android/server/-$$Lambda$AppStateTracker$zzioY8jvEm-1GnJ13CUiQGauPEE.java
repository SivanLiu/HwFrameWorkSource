package com.android.server;

import android.os.PowerSaveState;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppStateTracker$zzioY8jvEm-1GnJ13CUiQGauPEE implements Consumer {
    private final /* synthetic */ AppStateTracker f$0;

    public /* synthetic */ -$$Lambda$AppStateTracker$zzioY8jvEm-1GnJ13CUiQGauPEE(AppStateTracker appStateTracker) {
        this.f$0 = appStateTracker;
    }

    public final void accept(Object obj) {
        AppStateTracker.lambda$onSystemServicesReady$0(this.f$0, (PowerSaveState) obj);
    }
}
