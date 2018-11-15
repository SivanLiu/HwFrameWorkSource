package com.android.server.content;

import android.accounts.AccountAndUser;
import android.os.Bundle;
import android.os.RemoteCallback.OnResultListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$o7UdjgcI2E4HDw_-2JMHW-T1SJs implements OnResultListener {
    private final /* synthetic */ SyncManager f$0;
    private final /* synthetic */ AccountAndUser f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ int f$3;
    private final /* synthetic */ String f$4;
    private final /* synthetic */ Bundle f$5;
    private final /* synthetic */ int f$6;
    private final /* synthetic */ long f$7;
    private final /* synthetic */ int f$8;

    public /* synthetic */ -$$Lambda$SyncManager$o7UdjgcI2E4HDw_-2JMHW-T1SJs(SyncManager syncManager, AccountAndUser accountAndUser, int i, int i2, String str, Bundle bundle, int i3, long j, int i4) {
        this.f$0 = syncManager;
        this.f$1 = accountAndUser;
        this.f$2 = i;
        this.f$3 = i2;
        this.f$4 = str;
        this.f$5 = bundle;
        this.f$6 = i3;
        this.f$7 = j;
        this.f$8 = i4;
    }

    public final void onResult(Bundle bundle) {
        SyncManager.lambda$scheduleSync$4(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, bundle);
    }
}
