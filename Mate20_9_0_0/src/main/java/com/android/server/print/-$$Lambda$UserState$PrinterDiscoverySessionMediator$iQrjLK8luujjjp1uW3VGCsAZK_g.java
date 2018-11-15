package com.android.server.print;

import android.print.PrinterId;
import com.android.internal.util.function.TriConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserState$PrinterDiscoverySessionMediator$iQrjLK8luujjjp1uW3VGCsAZK_g implements TriConsumer {
    public static final /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$iQrjLK8luujjjp1uW3VGCsAZK_g INSTANCE = new -$$Lambda$UserState$PrinterDiscoverySessionMediator$iQrjLK8luujjjp1uW3VGCsAZK_g();

    private /* synthetic */ -$$Lambda$UserState$PrinterDiscoverySessionMediator$iQrjLK8luujjjp1uW3VGCsAZK_g() {
    }

    public final void accept(Object obj, Object obj2, Object obj3) {
        ((PrinterDiscoverySessionMediator) obj).handleStartPrinterStateTracking((RemotePrintService) obj2, (PrinterId) obj3);
    }
}
