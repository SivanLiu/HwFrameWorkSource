package com.android.server.locksettings;

import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LockSettingsService$Hh44Kcp05cKI6Hc6dJfQupn4QY8 implements Runnable {
    private final /* synthetic */ LockSettingsService f$0;
    private final /* synthetic */ PasswordMetrics f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$LockSettingsService$Hh44Kcp05cKI6Hc6dJfQupn4QY8(LockSettingsService lockSettingsService, PasswordMetrics passwordMetrics, int i) {
        this.f$0 = lockSettingsService;
        this.f$1 = passwordMetrics;
        this.f$2 = i;
    }

    public final void run() {
        ((DevicePolicyManager) this.f$0.mContext.getSystemService("device_policy")).setActivePasswordState(this.f$1, this.f$2);
    }
}
