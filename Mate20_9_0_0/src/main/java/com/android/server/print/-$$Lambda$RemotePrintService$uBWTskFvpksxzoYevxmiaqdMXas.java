package com.android.server.print;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemotePrintService$uBWTskFvpksxzoYevxmiaqdMXas implements Consumer {
    public static final /* synthetic */ -$$Lambda$RemotePrintService$uBWTskFvpksxzoYevxmiaqdMXas INSTANCE = new -$$Lambda$RemotePrintService$uBWTskFvpksxzoYevxmiaqdMXas();

    private /* synthetic */ -$$Lambda$RemotePrintService$uBWTskFvpksxzoYevxmiaqdMXas() {
    }

    public final void accept(Object obj) {
        ((RemotePrintService) obj).handleBinderDied();
    }
}
