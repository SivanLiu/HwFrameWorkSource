package com.android.server.content;

import android.content.SyncStatusInfo.Stats;
import com.android.internal.util.function.QuadConsumer;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$9EoLpTk5JrHZn9R-uS0lqCVrpRw implements QuadConsumer {
    private final /* synthetic */ StringBuilder f$0;
    private final /* synthetic */ PrintTable f$1;

    public /* synthetic */ -$$Lambda$SyncManager$9EoLpTk5JrHZn9R-uS0lqCVrpRw(StringBuilder stringBuilder, PrintTable printTable) {
        this.f$0 = stringBuilder;
        this.f$1 = printTable;
    }

    public final void accept(Object obj, Object obj2, Object obj3, Object obj4) {
        SyncManager.lambda$dumpSyncState$10(this.f$0, this.f$1, (String) obj, (Stats) obj2, (Function) obj3, (Integer) obj4);
    }
}
