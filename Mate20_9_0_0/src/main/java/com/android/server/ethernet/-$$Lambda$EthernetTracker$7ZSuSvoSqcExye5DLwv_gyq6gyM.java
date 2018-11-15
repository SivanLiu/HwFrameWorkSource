package com.android.server.ethernet;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EthernetTracker$7ZSuSvoSqcExye5DLwv_gyq6gyM implements Runnable {
    private final /* synthetic */ EthernetTracker f$0;

    public /* synthetic */ -$$Lambda$EthernetTracker$7ZSuSvoSqcExye5DLwv_gyq6gyM(EthernetTracker ethernetTracker) {
        this.f$0 = ethernetTracker;
    }

    public final void run() {
        this.f$0.trackAvailableInterfaces();
    }
}
