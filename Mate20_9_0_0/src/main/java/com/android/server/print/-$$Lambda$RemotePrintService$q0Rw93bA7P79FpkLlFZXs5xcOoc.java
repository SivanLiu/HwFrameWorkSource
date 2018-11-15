package com.android.server.print;

import java.util.List;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemotePrintService$q0Rw93bA7P79FpkLlFZXs5xcOoc implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$RemotePrintService$q0Rw93bA7P79FpkLlFZXs5xcOoc INSTANCE = new -$$Lambda$RemotePrintService$q0Rw93bA7P79FpkLlFZXs5xcOoc();

    private /* synthetic */ -$$Lambda$RemotePrintService$q0Rw93bA7P79FpkLlFZXs5xcOoc() {
    }

    public final void accept(Object obj, Object obj2) {
        ((RemotePrintService) obj).handleValidatePrinters((List) obj2);
    }
}
