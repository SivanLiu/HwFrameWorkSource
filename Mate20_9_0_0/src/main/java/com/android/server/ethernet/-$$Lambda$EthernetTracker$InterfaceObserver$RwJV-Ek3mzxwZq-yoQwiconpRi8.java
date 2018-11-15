package com.android.server.ethernet;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetTracker$InterfaceObserver$RwJV-Ek3mzxwZq-yoQwiconpRi8 implements Runnable {
    private final /* synthetic */ InterfaceObserver f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ boolean f$2;

    public /* synthetic */ -$$Lambda$EthernetTracker$InterfaceObserver$RwJV-Ek3mzxwZq-yoQwiconpRi8(InterfaceObserver interfaceObserver, String str, boolean z) {
        this.f$0 = interfaceObserver;
        this.f$1 = str;
        this.f$2 = z;
    }

    public final void run() {
        EthernetTracker.this.updateInterfaceState(this.f$1, this.f$2);
    }
}
