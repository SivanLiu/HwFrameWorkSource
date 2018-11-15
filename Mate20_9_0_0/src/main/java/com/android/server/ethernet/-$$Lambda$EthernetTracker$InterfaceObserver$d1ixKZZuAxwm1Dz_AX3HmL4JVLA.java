package com.android.server.ethernet;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetTracker$InterfaceObserver$d1ixKZZuAxwm1Dz_AX3HmL4JVLA implements Runnable {
    private final /* synthetic */ InterfaceObserver f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$EthernetTracker$InterfaceObserver$d1ixKZZuAxwm1Dz_AX3HmL4JVLA(InterfaceObserver interfaceObserver, String str) {
        this.f$0 = interfaceObserver;
        this.f$1 = str;
    }

    public final void run() {
        EthernetTracker.this.maybeTrackInterface(this.f$1);
    }
}
