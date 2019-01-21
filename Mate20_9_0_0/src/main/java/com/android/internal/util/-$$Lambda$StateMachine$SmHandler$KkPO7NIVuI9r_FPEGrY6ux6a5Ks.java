package com.android.internal.util;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StateMachine$SmHandler$KkPO7NIVuI9r_FPEGrY6ux6a5Ks implements Predicate {
    private final /* synthetic */ StateInfo f$0;

    public /* synthetic */ -$$Lambda$StateMachine$SmHandler$KkPO7NIVuI9r_FPEGrY6ux6a5Ks(StateInfo stateInfo) {
        this.f$0 = stateInfo;
    }

    public final boolean test(Object obj) {
        return SmHandler.lambda$removeState$0(this.f$0, (StateInfo) obj);
    }
}
