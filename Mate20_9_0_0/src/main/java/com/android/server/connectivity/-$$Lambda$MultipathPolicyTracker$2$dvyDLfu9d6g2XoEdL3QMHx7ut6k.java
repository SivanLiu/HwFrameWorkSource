package com.android.server.connectivity;

import com.android.server.connectivity.MultipathPolicyTracker.AnonymousClass2;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MultipathPolicyTracker$2$dvyDLfu9d6g2XoEdL3QMHx7ut6k implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;

    public /* synthetic */ -$$Lambda$MultipathPolicyTracker$2$dvyDLfu9d6g2XoEdL3QMHx7ut6k(AnonymousClass2 anonymousClass2) {
        this.f$0 = anonymousClass2;
    }

    public final void run() {
        MultipathPolicyTracker.this.updateAllMultipathBudgets();
    }
}
