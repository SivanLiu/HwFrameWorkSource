package com.android.server.print;

import android.print.PrinterId;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemotePrintService$aHc-cJYzTXxafcxxvfW2janFHIc implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$RemotePrintService$aHc-cJYzTXxafcxxvfW2janFHIc INSTANCE = new -$$Lambda$RemotePrintService$aHc-cJYzTXxafcxxvfW2janFHIc();

    private /* synthetic */ -$$Lambda$RemotePrintService$aHc-cJYzTXxafcxxvfW2janFHIc() {
    }

    public final void accept(Object obj, Object obj2) {
        ((RemotePrintService) obj).handleStartPrinterStateTracking((PrinterId) obj2);
    }
}
