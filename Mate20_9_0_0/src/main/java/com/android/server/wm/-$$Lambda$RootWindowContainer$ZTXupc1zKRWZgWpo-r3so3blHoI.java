package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RootWindowContainer$ZTXupc1zKRWZgWpo-r3so3blHoI implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$RootWindowContainer$ZTXupc1zKRWZgWpo-r3so3blHoI(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return RootWindowContainer.lambda$canShowStrictModeViolation$6(this.f$0, (WindowState) obj);
    }
}
