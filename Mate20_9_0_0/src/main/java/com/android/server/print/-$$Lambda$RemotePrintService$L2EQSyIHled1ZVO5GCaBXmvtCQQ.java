package com.android.server.print;

import android.print.PrinterId;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemotePrintService$L2EQSyIHled1ZVO5GCaBXmvtCQQ implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$RemotePrintService$L2EQSyIHled1ZVO5GCaBXmvtCQQ INSTANCE = new -$$Lambda$RemotePrintService$L2EQSyIHled1ZVO5GCaBXmvtCQQ();

    private /* synthetic */ -$$Lambda$RemotePrintService$L2EQSyIHled1ZVO5GCaBXmvtCQQ() {
    }

    public final void accept(Object obj, Object obj2) {
        ((RemotePrintService) obj).handleStopPrinterStateTracking((PrinterId) obj2);
    }
}
