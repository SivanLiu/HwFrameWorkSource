package com.android.server.mtm.utils;

import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppStatusUtils$BBdDVstlycCdsjB1Z_n37eOuPtQ implements Predicate {
    private final /* synthetic */ AppStatusUtils f$0;

    public /* synthetic */ -$$Lambda$AppStatusUtils$BBdDVstlycCdsjB1Z_n37eOuPtQ(AppStatusUtils appStatusUtils) {
        this.f$0 = appStatusUtils;
    }

    public final boolean test(Object obj) {
        return this.f$0.checkVisibleWindow((AwareProcessInfo) obj);
    }
}
