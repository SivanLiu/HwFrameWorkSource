package com.android.server.locksettings;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LockSettingsService$lWTrcqR9gZxL-pxwBbtvTGqAifU implements Runnable {
    private final /* synthetic */ LockSettingsService f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$LockSettingsService$lWTrcqR9gZxL-pxwBbtvTGqAifU(LockSettingsService lockSettingsService, int i) {
        this.f$0 = lockSettingsService;
        this.f$1 = i;
    }

    public final void run() {
        LockSettingsService.lambda$tryRemoveUserFromSpCacheLater$2(this.f$0, this.f$1);
    }
}
