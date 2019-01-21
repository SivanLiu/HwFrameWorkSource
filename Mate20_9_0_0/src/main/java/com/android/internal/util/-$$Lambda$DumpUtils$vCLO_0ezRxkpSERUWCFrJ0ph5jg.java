package com.android.internal.util;

import android.content.ComponentName.WithComponentName;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpUtils$vCLO_0ezRxkpSERUWCFrJ0ph5jg implements Predicate {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$DumpUtils$vCLO_0ezRxkpSERUWCFrJ0ph5jg(int i, String str) {
        this.f$0 = i;
        this.f$1 = str;
    }

    public final boolean test(Object obj) {
        return DumpUtils.lambda$filterRecord$2(this.f$0, this.f$1, (WithComponentName) obj);
    }
}
