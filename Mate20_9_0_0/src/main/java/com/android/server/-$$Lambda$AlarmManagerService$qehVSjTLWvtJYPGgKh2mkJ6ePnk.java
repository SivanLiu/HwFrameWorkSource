package com.android.server;

import com.android.server.AlarmManagerService.Alarm;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlarmManagerService$qehVSjTLWvtJYPGgKh2mkJ6ePnk implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$AlarmManagerService$qehVSjTLWvtJYPGgKh2mkJ6ePnk(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return AlarmManagerService.lambda$removeLocked$1(this.f$0, (Alarm) obj);
    }
}
