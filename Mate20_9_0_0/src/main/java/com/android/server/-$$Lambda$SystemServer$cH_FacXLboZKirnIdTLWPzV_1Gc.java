package com.android.server;

import android.content.Context;
import android.os.ServiceManager;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SystemServer$cH_FacXLboZKirnIdTLWPzV_1Gc implements Runnable {
    private final /* synthetic */ Context f$0;

    public /* synthetic */ -$$Lambda$SystemServer$cH_FacXLboZKirnIdTLWPzV_1Gc(Context context) {
        this.f$0 = context;
    }

    public final void run() {
        ServiceManager.addService("consumer_ir", new ConsumerIrService(this.f$0));
    }
}
