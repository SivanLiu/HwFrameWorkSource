package com.android.server.am;

import com.android.internal.os.ProcessCpuTracker.FilterStats;
import com.android.internal.os.ProcessCpuTracker.Stats;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ActivityManagerService$dLQ66dH4nIti4hweaVJTGHj2tMU implements FilterStats {
    public static final /* synthetic */ -$$Lambda$ActivityManagerService$dLQ66dH4nIti4hweaVJTGHj2tMU INSTANCE = new -$$Lambda$ActivityManagerService$dLQ66dH4nIti4hweaVJTGHj2tMU();

    private /* synthetic */ -$$Lambda$ActivityManagerService$dLQ66dH4nIti4hweaVJTGHj2tMU() {
    }

    public final boolean needed(Stats stats) {
        return ActivityManagerService.lambda$reportMemUsage$4(stats);
    }
}
