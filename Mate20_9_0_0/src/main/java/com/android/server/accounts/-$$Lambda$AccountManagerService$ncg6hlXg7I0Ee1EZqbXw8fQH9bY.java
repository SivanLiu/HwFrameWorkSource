package com.android.server.accounts;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccountManagerService$ncg6hlXg7I0Ee1EZqbXw8fQH9bY implements Runnable {
    private final /* synthetic */ AccountManagerService f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$AccountManagerService$ncg6hlXg7I0Ee1EZqbXw8fQH9bY(AccountManagerService accountManagerService, int i) {
        this.f$0 = accountManagerService;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.syncSharedAccounts(this.f$1);
    }
}
