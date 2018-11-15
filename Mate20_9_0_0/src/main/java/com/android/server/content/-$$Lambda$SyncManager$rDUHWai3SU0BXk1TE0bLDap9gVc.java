package com.android.server.content;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$rDUHWai3SU0BXk1TE0bLDap9gVc implements Predicate {
    public static final /* synthetic */ -$$Lambda$SyncManager$rDUHWai3SU0BXk1TE0bLDap9gVc INSTANCE = new -$$Lambda$SyncManager$rDUHWai3SU0BXk1TE0bLDap9gVc();

    private /* synthetic */ -$$Lambda$SyncManager$rDUHWai3SU0BXk1TE0bLDap9gVc() {
    }

    public final boolean test(Object obj) {
        return (((SyncOperation) obj).isPeriodic ^ 1);
    }
}
