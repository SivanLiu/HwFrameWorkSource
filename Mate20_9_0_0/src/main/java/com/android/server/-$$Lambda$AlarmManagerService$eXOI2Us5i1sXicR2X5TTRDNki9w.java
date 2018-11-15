package com.android.server;

import com.android.server.AlarmManagerService.Alarm;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlarmManagerService$eXOI2Us5i1sXicR2X5TTRDNki9w implements Predicate {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$AlarmManagerService$eXOI2Us5i1sXicR2X5TTRDNki9w(int i, String str) {
        this.f$0 = i;
        this.f$1 = str;
    }

    public final boolean test(Object obj) {
        return AlarmManagerService.lambda$removeLocked$3(this.f$0, this.f$1, (Alarm) obj);
    }
}
