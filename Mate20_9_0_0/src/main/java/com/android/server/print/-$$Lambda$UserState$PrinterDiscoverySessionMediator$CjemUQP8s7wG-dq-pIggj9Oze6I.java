package com.android.server.print;

import java.util.List;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wG-dq-pIggj9Oze6I implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wG-dq-pIggj9Oze6I INSTANCE = new -$$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wG-dq-pIggj9Oze6I();

    private /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wG-dq-pIggj9Oze6I() {
    }

    public final void accept(Object obj, Object obj2) {
        ((PrinterDiscoverySessionMediator) obj).handleDispatchPrintersRemoved((List) obj2);
    }
}
