package com.android.server.print;

import java.util.ArrayList;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserState$PrinterDiscoverySessionMediator$y51cj-jOuPNqkjzP4R89xJuclvo implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$y51cj-jOuPNqkjzP4R89xJuclvo INSTANCE = new -$$Lambda$UserState$PrinterDiscoverySessionMediator$y51cj-jOuPNqkjzP4R89xJuclvo();

    private /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$y51cj-jOuPNqkjzP4R89xJuclvo() {
    }

    public final void accept(Object obj, Object obj2) {
        ((PrinterDiscoverySessionMediator) obj).handleDispatchPrintersAdded((ArrayList) obj2);
    }
}
