package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$BgTlvHbVclnASz-MrvERWxyMV-A implements Predicate {
    private final /* synthetic */ DisplayContent f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$BgTlvHbVclnASz-MrvERWxyMV-A(DisplayContent displayContent) {
        this.f$0 = displayContent;
    }

    public final boolean test(Object obj) {
        return DisplayContent.lambda$checkWaitingForWindows$20(this.f$0, (WindowState) obj);
    }
}
