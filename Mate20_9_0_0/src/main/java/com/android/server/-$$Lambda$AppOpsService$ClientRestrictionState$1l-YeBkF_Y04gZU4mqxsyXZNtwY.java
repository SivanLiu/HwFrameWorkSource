package com.android.server;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AppOpsService$ClientRestrictionState$1l-YeBkF_Y04gZU4mqxsyXZNtwY implements Runnable {
    private final /* synthetic */ ClientRestrictionState f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$AppOpsService$ClientRestrictionState$1l-YeBkF_Y04gZU4mqxsyXZNtwY(ClientRestrictionState clientRestrictionState, int i) {
        this.f$0 = clientRestrictionState;
        this.f$1 = i;
    }

    public final void run() {
        AppOpsService.this.notifyWatchersOfChange(this.f$1, -2);
    }
}
