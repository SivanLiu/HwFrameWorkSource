package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountManagerInternal.OnAppPermissionChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyncManager$HhiSFjEoPA_Hnv3xYZGfwkalc68 implements OnAppPermissionChangeListener {
    private final /* synthetic */ SyncManager f$0;

    public /* synthetic */ -$$Lambda$SyncManager$HhiSFjEoPA_Hnv3xYZGfwkalc68(SyncManager syncManager) {
        this.f$0 = syncManager;
    }

    public final void onAppPermissionChanged(Account account, int i) {
        SyncManager.lambda$new$0(this.f$0, account, i);
    }
}
