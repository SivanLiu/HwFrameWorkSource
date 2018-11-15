package com.android.server.autofill;

import android.os.IBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Session$xw4trZ-LA7gCvZvpKJ93vf377ak implements DeathRecipient {
    private final /* synthetic */ Session f$0;

    public /* synthetic */ -$$Lambda$Session$xw4trZ-LA7gCvZvpKJ93vf377ak(Session session) {
        this.f$0 = session;
    }

    public final void binderDied() {
        Session.lambda$setClientLocked$0(this.f$0);
    }
}
