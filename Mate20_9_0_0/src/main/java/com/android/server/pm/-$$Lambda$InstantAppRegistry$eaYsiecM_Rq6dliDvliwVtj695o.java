package com.android.server.pm;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$InstantAppRegistry$eaYsiecM_Rq6dliDvliwVtj695o implements Predicate {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$InstantAppRegistry$eaYsiecM_Rq6dliDvliwVtj695o(String str) {
        this.f$0 = str;
    }

    public final boolean test(Object obj) {
        return ((UninstalledInstantAppState) obj).mInstantAppInfo.getPackageName().equals(this.f$0);
    }
}
