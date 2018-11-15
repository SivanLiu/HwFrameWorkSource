package com.android.server.wm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DisplayContent$2VlyMN8z2sOPqE9-yf-z3-peRMI implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$DisplayContent$2VlyMN8z2sOPqE9-yf-z3-peRMI(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return DisplayContent.lambda$canAddToastWindowForUid$14(this.f$0, (WindowState) obj);
    }
}
