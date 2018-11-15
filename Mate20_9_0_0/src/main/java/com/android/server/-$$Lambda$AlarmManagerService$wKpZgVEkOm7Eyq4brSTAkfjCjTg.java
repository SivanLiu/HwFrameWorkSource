package com.android.server;

import com.android.server.AlarmManagerService.Alarm;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlarmManagerService$wKpZgVEkOm7Eyq4brSTAkfjCjTg implements Predicate {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$AlarmManagerService$wKpZgVEkOm7Eyq4brSTAkfjCjTg(String str, String str2) {
        this.f$0 = str;
        this.f$1 = str2;
    }

    public final boolean test(Object obj) {
        return AlarmManagerService.lambda$removeLocked$2(this.f$0, this.f$1, (Alarm) obj);
    }
}
