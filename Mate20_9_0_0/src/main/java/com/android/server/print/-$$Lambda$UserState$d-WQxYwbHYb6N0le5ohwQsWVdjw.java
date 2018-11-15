package com.android.server.print;

import android.print.PrintJobId;
import com.android.internal.util.function.TriConsumer;
import com.android.internal.util.function.pooled.PooledSupplier.OfInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserState$d-WQxYwbHYb6N0le5ohwQsWVdjw implements TriConsumer {
    public static final /* synthetic */ -$$Lambda$UserState$d-WQxYwbHYb6N0le5ohwQsWVdjw INSTANCE = new -$$Lambda$UserState$d-WQxYwbHYb6N0le5ohwQsWVdjw();

    private /* synthetic */ -$$Lambda$UserState$d-WQxYwbHYb6N0le5ohwQsWVdjw() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((UserState) obj).handleDispatchPrintJobStateChanged((PrintJobId) obj2, (OfInt) obj3);
    }
}
