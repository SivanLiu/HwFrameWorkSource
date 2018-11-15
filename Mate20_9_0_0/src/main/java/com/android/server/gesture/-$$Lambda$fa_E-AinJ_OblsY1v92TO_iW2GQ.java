package com.android.server.gesture;

import android.os.IBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$fa_E-AinJ_OblsY1v92TO_iW2GQ implements DeathRecipient {
    private final /* synthetic */ OverviewProxyService f$0;

    public /* synthetic */ -$$Lambda$fa_E-AinJ_OblsY1v92TO_iW2GQ(OverviewProxyService overviewProxyService) {
        this.f$0 = overviewProxyService;
    }

    public final void binderDied() {
        this.f$0.startConnectionToCurrentUser();
    }
}
