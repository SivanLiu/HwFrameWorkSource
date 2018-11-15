package com.android.server.am;

import android.app.IStopUserCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserController$AHHTCREuropaUGilzG-tndQCCSM implements Runnable {
    private final /* synthetic */ IStopUserCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$UserController$AHHTCREuropaUGilzG-tndQCCSM(IStopUserCallback iStopUserCallback, int i) {
        this.f$0 = iStopUserCallback;
        this.f$1 = i;
    }

    public final void run() {
        UserController.lambda$stopSingleUserLU$2(this.f$0, this.f$1);
    }
}
