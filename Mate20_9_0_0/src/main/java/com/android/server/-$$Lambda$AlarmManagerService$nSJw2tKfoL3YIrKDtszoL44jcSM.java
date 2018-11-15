package com.android.server;

import com.android.server.AlarmManagerService.Alarm;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlarmManagerService$nSJw2tKfoL3YIrKDtszoL44jcSM implements Predicate {
    private final /* synthetic */ AlarmManagerService f$0;

    public /* synthetic */ -$$Lambda$AlarmManagerService$nSJw2tKfoL3YIrKDtszoL44jcSM(AlarmManagerService alarmManagerService) {
        this.f$0 = alarmManagerService;
    }

    public final boolean test(Object obj) {
        return this.f$0.isBackgroundRestricted((Alarm) obj);
    }
}
