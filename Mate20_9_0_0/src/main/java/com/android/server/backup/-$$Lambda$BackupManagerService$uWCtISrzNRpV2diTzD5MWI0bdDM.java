package com.android.server.backup;

import com.android.server.backup.internal.OnTaskFinishedListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$uWCtISrzNRpV2diTzD5MWI0bdDM implements OnTaskFinishedListener {
    private final /* synthetic */ BackupManagerService f$0;

    public /* synthetic */ -$$Lambda$BackupManagerService$uWCtISrzNRpV2diTzD5MWI0bdDM(BackupManagerService backupManagerService) {
        this.f$0 = backupManagerService;
    }

    public final void onFinished(String str) {
        this.f$0.mWakelock.release();
    }
}
