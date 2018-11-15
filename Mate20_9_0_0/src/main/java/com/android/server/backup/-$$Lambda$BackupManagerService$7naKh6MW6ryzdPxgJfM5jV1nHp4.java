package com.android.server.backup;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$7naKh6MW6ryzdPxgJfM5jV1nHp4 implements Runnable {
    private final /* synthetic */ BackupManagerService f$0;

    public /* synthetic */ -$$Lambda$BackupManagerService$7naKh6MW6ryzdPxgJfM5jV1nHp4(BackupManagerService backupManagerService) {
        this.f$0 = backupManagerService;
    }

    public final void run() {
        this.f$0.parseLeftoverJournals();
    }
}
