package com.android.server.ethernet;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetTracker$InterfaceObserver$N47vO7QrVbS59gsxVAc8Mt2Opco implements Runnable {
    private final /* synthetic */ InterfaceObserver f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$EthernetTracker$InterfaceObserver$N47vO7QrVbS59gsxVAc8Mt2Opco(InterfaceObserver interfaceObserver, String str) {
        this.f$0 = interfaceObserver;
        this.f$1 = str;
    }

    public final void run() {
        EthernetTracker.this.removeInterface(this.f$1);
    }
}
