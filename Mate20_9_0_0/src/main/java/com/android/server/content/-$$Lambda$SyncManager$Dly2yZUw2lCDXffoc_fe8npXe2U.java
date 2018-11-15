package com.android.server.content;

import android.accounts.AccountAndUser;
import android.os.Bundle;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$Dly2yZUw2lCDXffoc_fe8npXe2U implements OnReadyCallback {
    private final /* synthetic */ SyncManager f$0;
    private final /* synthetic */ AccountAndUser f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ String f$3;
    private final /* synthetic */ Bundle f$4;
    private final /* synthetic */ int f$5;
    private final /* synthetic */ long f$6;
    private final /* synthetic */ int f$7;

    public /* synthetic */ -$$Lambda$SyncManager$Dly2yZUw2lCDXffoc_fe8npXe2U(SyncManager syncManager, AccountAndUser accountAndUser, int i, String str, Bundle bundle, int i2, long j, int i3) {
        this.f$0 = syncManager;
        this.f$1 = accountAndUser;
        this.f$2 = i;
        this.f$3 = str;
        this.f$4 = bundle;
        this.f$5 = i2;
        this.f$6 = j;
        this.f$7 = i3;
    }

    public final void onReady() {
        SyncManager.lambda$scheduleSync$5(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7);
    }
}
