package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AlertWindowNotification$ZuqSYML-X-nkNVTba_yeIT9hJ1s implements Runnable {
    private final /* synthetic */ AlertWindowNotification f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$AlertWindowNotification$ZuqSYML-X-nkNVTba_yeIT9hJ1s(AlertWindowNotification alertWindowNotification, boolean z) {
        this.f$0 = alertWindowNotification;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.onCancelNotification(this.f$1);
    }
}
