package com.android.server.backup;

import com.android.server.backup.DataChangedJournal.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BackupManagerService$-mOc1e-1SsZws3njOjKXfyubq98 implements Consumer {
    private final /* synthetic */ BackupManagerService f$0;

    public /* synthetic */ -$$Lambda$BackupManagerService$-mOc1e-1SsZws3njOjKXfyubq98(BackupManagerService backupManagerService) {
        this.f$0 = backupManagerService;
    }

    public final void accept(String str) {
        BackupManagerService.lambda$parseLeftoverJournals$0(this.f$0, str);
    }
}
