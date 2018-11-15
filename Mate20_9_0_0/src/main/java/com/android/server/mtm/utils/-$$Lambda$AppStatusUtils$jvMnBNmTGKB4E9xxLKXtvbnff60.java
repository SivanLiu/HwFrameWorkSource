package com.android.server.mtm.utils;

import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppStatusUtils$jvMnBNmTGKB4E9xxLKXtvbnff60 implements Predicate {
    private final /* synthetic */ AppStatusUtils f$0;

    public /* synthetic */ -$$Lambda$AppStatusUtils$jvMnBNmTGKB4E9xxLKXtvbnff60(AppStatusUtils appStatusUtils) {
        this.f$0 = appStatusUtils;
    }

    public final boolean test(Object obj) {
        return this.f$0.checkInSmallSampleList((AwareProcessInfo) obj);
    }
}
