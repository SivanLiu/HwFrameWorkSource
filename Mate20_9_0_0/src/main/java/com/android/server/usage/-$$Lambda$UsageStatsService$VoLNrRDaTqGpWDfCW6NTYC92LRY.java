package com.android.server.usage;

import android.app.PendingIntent;
import com.android.server.usage.AppTimeLimitController.OnLimitReachedListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UsageStatsService$VoLNrRDaTqGpWDfCW6NTYC92LRY implements OnLimitReachedListener {
    private final /* synthetic */ UsageStatsService f$0;

    public /* synthetic */ -$$Lambda$UsageStatsService$VoLNrRDaTqGpWDfCW6NTYC92LRY(UsageStatsService usageStatsService) {
        this.f$0 = usageStatsService;
    }

    public final void onLimitReached(int i, int i2, long j, long j2, PendingIntent pendingIntent) {
        UsageStatsService.lambda$onStart$0(this.f$0, i, i2, j, j2, pendingIntent);
    }
}
