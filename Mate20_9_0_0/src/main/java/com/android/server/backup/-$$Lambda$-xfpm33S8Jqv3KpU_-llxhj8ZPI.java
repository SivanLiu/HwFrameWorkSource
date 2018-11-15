package com.android.server.backup;

import android.content.ComponentName;
import java.util.Set;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$-xfpm33S8Jqv3KpU_-llxhj8ZPI implements Predicate {
    private final /* synthetic */ Set f$0;

    public /* synthetic */ -$$Lambda$-xfpm33S8Jqv3KpU_-llxhj8ZPI(Set set) {
        this.f$0 = set;
    }

    public final boolean test(Object obj) {
        return this.f$0.contains((ComponentName) obj);
    }
}
