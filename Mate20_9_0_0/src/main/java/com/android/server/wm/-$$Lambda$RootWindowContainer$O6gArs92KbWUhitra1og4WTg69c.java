package com.android.server.wm;

import java.util.ArrayList;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RootWindowContainer$O6gArs92KbWUhitra1og4WTg69c implements Consumer {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ ArrayList f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$RootWindowContainer$O6gArs92KbWUhitra1og4WTg69c(String str, ArrayList arrayList, int i) {
        this.f$0 = str;
        this.f$1 = arrayList;
        this.f$2 = i;
    }

    public final void accept(Object obj) {
        RootWindowContainer.lambda$getWindowsByName$2(this.f$0, this.f$1, this.f$2, (WindowState) obj);
    }
}
