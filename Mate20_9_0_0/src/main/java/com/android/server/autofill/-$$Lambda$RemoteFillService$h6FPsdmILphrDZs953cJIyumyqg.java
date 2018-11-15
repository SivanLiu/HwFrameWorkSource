package com.android.server.autofill;

import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemoteFillService$h6FPsdmILphrDZs953cJIyumyqg implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$RemoteFillService$h6FPsdmILphrDZs953cJIyumyqg INSTANCE = new -$$Lambda$RemoteFillService$h6FPsdmILphrDZs953cJIyumyqg();

    private /* synthetic */ -$$Lambda$RemoteFillService$h6FPsdmILphrDZs953cJIyumyqg() {
    }

    public final void accept(Object obj, Object obj2) {
        ((RemoteFillService) obj).handlePendingRequest((PendingRequest) obj2);
    }
}
