package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$_XfE1uZ9VUv6i0SxWUvqu69FNb4 implements Predicate {
    private final /* synthetic */ DisplayContent f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$DisplayContent$_XfE1uZ9VUv6i0SxWUvqu69FNb4(DisplayContent displayContent, int i, int i2) {
        this.f$0 = displayContent;
        this.f$1 = i;
        this.f$2 = i2;
    }

    public final boolean test(Object obj) {
        return DisplayContent.lambda$getTouchableWinAtPointLocked$13(this.f$0, this.f$1, this.f$2, (WindowState) obj);
    }
}
