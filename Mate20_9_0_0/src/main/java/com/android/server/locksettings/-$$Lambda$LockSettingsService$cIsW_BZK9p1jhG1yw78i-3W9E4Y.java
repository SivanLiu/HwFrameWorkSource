package com.android.server.locksettings;

import android.app.admin.DevicePolicyManager;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LockSettingsService$cIsW_BZK9p1jhG1yw78i-3W9E4Y implements Runnable {
    private final /* synthetic */ LockSettingsService f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$LockSettingsService$cIsW_BZK9p1jhG1yw78i-3W9E4Y(LockSettingsService lockSettingsService, int i) {
        this.f$0 = lockSettingsService;
        this.f$1 = i;
    }

    public final void run() {
        ((DevicePolicyManager) this.f$0.mContext.getSystemService("device_policy")).reportPasswordChanged(this.f$1);
    }
}
