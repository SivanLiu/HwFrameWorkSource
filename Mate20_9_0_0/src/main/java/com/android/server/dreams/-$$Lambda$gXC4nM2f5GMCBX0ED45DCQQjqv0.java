package com.android.server.dreams;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$gXC4nM2f5GMCBX0ED45DCQQjqv0 implements Runnable {
    private final /* synthetic */ DreamRecord f$0;

    public /* synthetic */ -$$Lambda$gXC4nM2f5GMCBX0ED45DCQQjqv0(DreamRecord dreamRecord) {
        this.f$0 = dreamRecord;
    }

    public final void run() {
        this.f$0.releaseWakeLockIfNeeded();
    }
}
