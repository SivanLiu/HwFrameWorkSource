package com.android.server;

import com.android.server.AlarmManagerService.Alarm;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlarmManagerService$d1Nr3qXE-1WItEvvEEG1KMB46xw implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$AlarmManagerService$d1Nr3qXE-1WItEvvEEG1KMB46xw(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return AlarmManagerService.lambda$removeForStoppedLocked$4(this.f$0, (Alarm) obj);
    }
}
