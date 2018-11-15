package com.android.server.mtm.utils;

import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppStatusUtils$_izxbGuTXo6Xb6qC_wXWOICm56c implements Predicate {
    private final /* synthetic */ AppStatusUtils f$0;

    public /* synthetic */ -$$Lambda$AppStatusUtils$_izxbGuTXo6Xb6qC_wXWOICm56c(AppStatusUtils appStatusUtils) {
        this.f$0 = appStatusUtils;
    }

    public final boolean test(Object obj) {
        return this.f$0.checkScreenRecord((AwareProcessInfo) obj);
    }
}
