package com.android.server.job;

import com.android.internal.util.ArrayUtils;
import com.android.server.job.controllers.JobStatus;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobStore$JobSet$D9839QVHHu4X-hnxouyIMkP5NWA implements Predicate {
    private final /* synthetic */ int[] f$0;

    public /* synthetic */ -$$Lambda$JobStore$JobSet$D9839QVHHu4X-hnxouyIMkP5NWA(int[] iArr) {
        this.f$0 = iArr;
    }

    public final boolean test(Object obj) {
        return (ArrayUtils.contains(this.f$0, ((JobStatus) obj).getSourceUserId()) ^ 1);
    }
}
