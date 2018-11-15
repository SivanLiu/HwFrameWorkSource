package com.android.server;

import android.app.IAlarmListener;
import android.app.PendingIntent;
import com.android.server.AlarmManagerService.Alarm;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlarmManagerService$ZVedZIeWdB3G6AGM0_-9P_GEO24 implements Predicate {
    private final /* synthetic */ PendingIntent f$0;
    private final /* synthetic */ IAlarmListener f$1;

    public /* synthetic */ -$$Lambda$AlarmManagerService$ZVedZIeWdB3G6AGM0_-9P_GEO24(PendingIntent pendingIntent, IAlarmListener iAlarmListener) {
        this.f$0 = pendingIntent;
        this.f$1 = iAlarmListener;
    }

    public final boolean test(Object obj) {
        return AlarmManagerService.lambda$removeLocked$0(this.f$0, this.f$1, (Alarm) obj);
    }
}
