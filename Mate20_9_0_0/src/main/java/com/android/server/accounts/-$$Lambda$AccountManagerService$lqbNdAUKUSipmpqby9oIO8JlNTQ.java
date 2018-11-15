package com.android.server.accounts;

import android.accounts.Account;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccountManagerService$lqbNdAUKUSipmpqby9oIO8JlNTQ implements Runnable {
    private final /* synthetic */ AccountManagerService f$0;
    private final /* synthetic */ Account f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$AccountManagerService$lqbNdAUKUSipmpqby9oIO8JlNTQ(AccountManagerService accountManagerService, Account account, int i) {
        this.f$0 = accountManagerService;
        this.f$1 = account;
        this.f$2 = i;
    }

    public final void run() {
        this.f$0.cancelAccountAccessRequestNotificationIfNeeded(this.f$1, this.f$2, false);
    }
}
