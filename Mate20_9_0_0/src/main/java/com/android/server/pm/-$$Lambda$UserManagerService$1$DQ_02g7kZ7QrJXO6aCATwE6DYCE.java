package com.android.server.pm;

import android.content.IntentSender;
import com.android.server.pm.UserManagerService.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$UserManagerService$1$DQ_02g7kZ7QrJXO6aCATwE6DYCE implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ IntentSender f$2;

    public /* synthetic */ -$$Lambda$UserManagerService$1$DQ_02g7kZ7QrJXO6aCATwE6DYCE(AnonymousClass1 anonymousClass1, int i, IntentSender intentSender) {
        this.f$0 = anonymousClass1;
        this.f$1 = i;
        this.f$2 = intentSender;
    }

    public final void run() {
        UserManagerService.this.setQuietModeEnabled(this.f$1, false, this.f$2);
    }
}
