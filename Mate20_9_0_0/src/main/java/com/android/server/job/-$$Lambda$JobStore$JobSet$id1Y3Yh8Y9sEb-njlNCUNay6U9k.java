package com.android.server.job;

import com.android.internal.util.ArrayUtils;
import com.android.server.job.controllers.JobStatus;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$JobStore$JobSet$id1Y3Yh8Y9sEb-njlNCUNay6U9k implements Predicate {
    private final /* synthetic */ int[] f$0;

    public /* synthetic */ -$$Lambda$JobStore$JobSet$id1Y3Yh8Y9sEb-njlNCUNay6U9k(int[] iArr) {
        this.f$0 = iArr;
    }

    public final boolean test(Object obj) {
        return (ArrayUtils.contains(this.f$0, ((JobStatus) obj).getUserId()) ^ 1);
    }
}
