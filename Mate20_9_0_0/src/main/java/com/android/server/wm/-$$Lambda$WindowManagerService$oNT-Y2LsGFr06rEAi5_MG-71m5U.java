package com.android.server.wm;

import java.util.ArrayList;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WindowManagerService$oNT-Y2LsGFr06rEAi5_MG-71m5U implements Consumer {
    private final /* synthetic */ boolean f$0;
    private final /* synthetic */ boolean f$1;
    private final /* synthetic */ ArrayList f$2;

    public /* synthetic */ -$$Lambda$WindowManagerService$oNT-Y2LsGFr06rEAi5_MG-71m5U(boolean z, boolean z2, ArrayList arrayList) {
        this.f$0 = z;
        this.f$1 = z2;
        this.f$2 = arrayList;
    }

    public final void accept(Object obj) {
        WindowManagerService.lambda$dumpWindows$5(this.f$0, this.f$1, this.f$2, (WindowState) obj);
    }
}
