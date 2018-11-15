package com.android.server.print;

import java.util.List;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserState$PrinterDiscoverySessionMediator$lfSsgTy_1NLRRkjOH_yL2Tk_x2w implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$lfSsgTy_1NLRRkjOH_yL2Tk_x2w INSTANCE = new -$$Lambda$UserState$PrinterDiscoverySessionMediator$lfSsgTy_1NLRRkjOH_yL2Tk_x2w();

    private /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$lfSsgTy_1NLRRkjOH_yL2Tk_x2w() {
    }

    public final void accept(Object obj, Object obj2) {
        ((PrinterDiscoverySessionMediator) obj).handleDispatchPrintersAdded((List) obj2);
    }
}
