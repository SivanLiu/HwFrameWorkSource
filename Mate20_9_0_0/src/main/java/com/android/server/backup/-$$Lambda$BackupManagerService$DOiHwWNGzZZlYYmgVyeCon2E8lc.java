package com.android.server.backup;

import android.app.backup.ISelectBackupTransportCallback;
import android.content.ComponentName;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$DOiHwWNGzZZlYYmgVyeCon2E8lc implements Runnable {
    private final /* synthetic */ BackupManagerService f$0;
    private final /* synthetic */ ComponentName f$1;
    private final /* synthetic */ ISelectBackupTransportCallback f$2;

    public /* synthetic */ -$$Lambda$BackupManagerService$DOiHwWNGzZZlYYmgVyeCon2E8lc(BackupManagerService backupManagerService, ComponentName componentName, ISelectBackupTransportCallback iSelectBackupTransportCallback) {
        this.f$0 = backupManagerService;
        this.f$1 = componentName;
        this.f$2 = iSelectBackupTransportCallback;
    }

    public final void run() {
        BackupManagerService.lambda$selectBackupTransportAsync$5(this.f$0, this.f$1, this.f$2);
    }
}
