package com.android.server;

import com.android.server.AlarmManagerService.Alarm;
import com.android.server.AlarmManagerService.Batch;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlarmManagerService$Batch$Xltkj5RTKUMuFVeuavpuY7-Ogzc implements Predicate {
    private final /* synthetic */ Alarm f$0;

    public /* synthetic */ -$$Lambda$AlarmManagerService$Batch$Xltkj5RTKUMuFVeuavpuY7-Ogzc(Alarm alarm) {
        this.f$0 = alarm;
    }

    public final boolean test(Object obj) {
        return Batch.lambda$remove$0(this.f$0, (Alarm) obj);
    }
}
