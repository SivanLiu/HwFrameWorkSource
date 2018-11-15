package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$JYsrGdifTPH6ASJDC3B9YWMD2pw implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$JYsrGdifTPH6ASJDC3B9YWMD2pw(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return DisplayContent.lambda$canAddToastWindowForUid$15(this.f$0, (WindowState) obj);
    }
}
