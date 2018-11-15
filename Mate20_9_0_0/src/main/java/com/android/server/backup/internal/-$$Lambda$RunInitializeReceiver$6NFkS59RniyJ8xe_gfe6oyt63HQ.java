package com.android.server.backup.internal;

import android.os.PowerManager.WakeLock;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RunInitializeReceiver$6NFkS59RniyJ8xe_gfe6oyt63HQ implements OnTaskFinishedListener {
    private final /* synthetic */ WakeLock f$0;

    public /* synthetic */ -$$Lambda$RunInitializeReceiver$6NFkS59RniyJ8xe_gfe6oyt63HQ(WakeLock wakeLock) {
        this.f$0 = wakeLock;
    }

    public final void onFinished(String str) {
        this.f$0.release();
    }
}
