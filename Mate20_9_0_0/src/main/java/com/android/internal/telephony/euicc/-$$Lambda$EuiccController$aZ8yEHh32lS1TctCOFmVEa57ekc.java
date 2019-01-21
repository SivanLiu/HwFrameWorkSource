package com.android.internal.telephony.euicc;

import android.app.PendingIntent;
import android.content.Intent;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccController$aZ8yEHh32lS1TctCOFmVEa57ekc implements Runnable {
    private final /* synthetic */ EuiccController f$0;
    private final /* synthetic */ PendingIntent f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ Intent f$3;

    public /* synthetic */ -$$Lambda$EuiccController$aZ8yEHh32lS1TctCOFmVEa57ekc(EuiccController euiccController, PendingIntent pendingIntent, int i, Intent intent) {
        this.f$0 = euiccController;
        this.f$1 = pendingIntent;
        this.f$2 = i;
        this.f$3 = intent;
    }

    public final void run() {
        this.f$0.sendResult(this.f$1, this.f$2, this.f$3);
    }
}
