package com.android.internal.util;

import android.content.ComponentName;
import android.content.ComponentName.WithComponentName;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpUtils$X8irOs5hfloCKy89_l1HRA1QeG0 implements Predicate {
    private final /* synthetic */ ComponentName f$0;

    public /* synthetic */ -$$Lambda$DumpUtils$X8irOs5hfloCKy89_l1HRA1QeG0(ComponentName componentName) {
        this.f$0 = componentName;
    }

    public final boolean test(Object obj) {
        return DumpUtils.lambda$filterRecord$1(this.f$0, (WithComponentName) obj);
    }
}
