package com.android.server;

import android.util.Slog;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LockGuard$C107ImDhsfBAwlfWxZPBoVXIl_4 implements Runnable {
    private final /* synthetic */ Throwable f$0;

    public /* synthetic */ -$$Lambda$LockGuard$C107ImDhsfBAwlfWxZPBoVXIl_4(Throwable th) {
        this.f$0 = th;
    }

    public final void run() {
        Slog.wtf(LockGuard.TAG, this.f$0);
    }
}
