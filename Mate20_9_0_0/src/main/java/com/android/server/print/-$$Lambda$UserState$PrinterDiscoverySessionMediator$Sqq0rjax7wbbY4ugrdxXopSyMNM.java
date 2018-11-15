package com.android.server.print;

import com.android.internal.util.function.TriConsumer;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserState$PrinterDiscoverySessionMediator$Sqq0rjax7wbbY4ugrdxXopSyMNM implements TriConsumer {
    public static final /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$Sqq0rjax7wbbY4ugrdxXopSyMNM INSTANCE = new -$$Lambda$UserState$PrinterDiscoverySessionMediator$Sqq0rjax7wbbY4ugrdxXopSyMNM();

    private /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$Sqq0rjax7wbbY4ugrdxXopSyMNM() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((PrinterDiscoverySessionMediator) obj).handleValidatePrinters((RemotePrintService) obj2, (List) obj3);
    }
}
