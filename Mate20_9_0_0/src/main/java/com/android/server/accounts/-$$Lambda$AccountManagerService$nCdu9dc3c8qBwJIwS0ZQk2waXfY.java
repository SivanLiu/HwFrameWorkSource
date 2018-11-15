package com.android.server.accounts;

import android.accounts.Account;
import android.accounts.AccountManagerInternal.OnAppPermissionChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccountManagerService$nCdu9dc3c8qBwJIwS0ZQk2waXfY implements Runnable {
    private final /* synthetic */ OnAppPermissionChangeListener f$0;
    private final /* synthetic */ Account f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$AccountManagerService$nCdu9dc3c8qBwJIwS0ZQk2waXfY(OnAppPermissionChangeListener onAppPermissionChangeListener, Account account, int i) {
        this.f$0 = onAppPermissionChangeListener;
        this.f$1 = account;
        this.f$2 = i;
    }

    public final void run() {
        this.f$0.onAppPermissionChanged(this.f$1, this.f$2);
    }
}
