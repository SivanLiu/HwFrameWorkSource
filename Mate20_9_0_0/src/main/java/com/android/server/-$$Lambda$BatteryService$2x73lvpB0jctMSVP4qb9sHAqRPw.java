package com.android.server;

import android.app.ActivityManager;
import android.content.Intent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryService$2x73lvpB0jctMSVP4qb9sHAqRPw implements Runnable {
    private final /* synthetic */ Intent f$0;

    public /* synthetic */ -$$Lambda$BatteryService$2x73lvpB0jctMSVP4qb9sHAqRPw(Intent intent) {
        this.f$0 = intent;
    }

    public final void run() {
        ActivityManager.broadcastStickyIntent(this.f$0, -1);
    }
}
