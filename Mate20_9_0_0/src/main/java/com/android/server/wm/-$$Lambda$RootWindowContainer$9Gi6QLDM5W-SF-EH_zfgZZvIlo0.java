package com.android.server.wm;

import android.util.ArraySet;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RootWindowContainer$9Gi6QLDM5W-SF-EH_zfgZZvIlo0 implements Consumer {
    private final /* synthetic */ ArraySet f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$RootWindowContainer$9Gi6QLDM5W-SF-EH_zfgZZvIlo0(ArraySet arraySet, boolean z) {
        this.f$0 = arraySet;
        this.f$1 = z;
    }

    public final void accept(Object obj) {
        RootWindowContainer.lambda$updateHiddenWhileSuspendedState$4(this.f$0, this.f$1, (WindowState) obj);
    }
}
