package com.android.server.content;

import android.content.Context;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$zZUXjd-GLFQgHtMQ3vq0EWHvir8 implements Runnable {
    private final /* synthetic */ Context f$0;
    private final /* synthetic */ OnUnsyncableAccountCheck f$1;

    public /* synthetic */ -$$Lambda$SyncManager$zZUXjd-GLFQgHtMQ3vq0EWHvir8(Context context, OnUnsyncableAccountCheck onUnsyncableAccountCheck) {
        this.f$0 = context;
        this.f$1 = onUnsyncableAccountCheck;
    }

    public final void run() {
        this.f$0.unbindService(this.f$1);
    }
}
