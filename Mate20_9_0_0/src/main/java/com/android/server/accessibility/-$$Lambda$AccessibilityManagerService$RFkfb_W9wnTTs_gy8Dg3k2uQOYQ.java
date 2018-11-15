package com.android.server.accessibility;

import com.android.internal.util.FunctionalUtils;
import com.android.server.accessibility.AccessibilityManagerService.UserState;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AccessibilityManagerService$RFkfb_W9wnTTs_gy8Dg3k2uQOYQ implements Runnable {
    private final /* synthetic */ AccessibilityManagerService f$0;
    private final /* synthetic */ UserState f$1;

    public /* synthetic */ -$$Lambda$AccessibilityManagerService$RFkfb_W9wnTTs_gy8Dg3k2uQOYQ(AccessibilityManagerService accessibilityManagerService, UserState userState) {
        this.f$0 = accessibilityManagerService;
        this.f$1 = userState;
    }

    public final void run() {
        this.f$0.broadcastToClients(this.f$1, FunctionalUtils.ignoreRemoteException(new -$$Lambda$AccessibilityManagerService$CNt8wbTQCYcsUnUkUCQHtKqr-tY(this.f$0, this.f$1)));
    }
}
