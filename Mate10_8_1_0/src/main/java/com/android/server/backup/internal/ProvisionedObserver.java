package com.android.server.backup.internal;

import android.database.ContentObserver;
import android.os.Handler;
import com.android.server.backup.KeyValueBackupJob;
import com.android.server.backup.RefactoredBackupManagerService;

public class ProvisionedObserver extends ContentObserver {
    private RefactoredBackupManagerService backupManagerService;

    public ProvisionedObserver(RefactoredBackupManagerService backupManagerService, Handler handler) {
        super(handler);
        this.backupManagerService = backupManagerService;
    }

    public void onChange(boolean selfChange) {
        boolean wasProvisioned = this.backupManagerService.isProvisioned();
        boolean isProvisioned = this.backupManagerService.deviceIsProvisioned();
        RefactoredBackupManagerService refactoredBackupManagerService = this.backupManagerService;
        if (wasProvisioned) {
            isProvisioned = true;
        }
        refactoredBackupManagerService.setProvisioned(isProvisioned);
        synchronized (this.backupManagerService.getQueueLock()) {
            if (this.backupManagerService.isProvisioned() && (wasProvisioned ^ 1) != 0 && this.backupManagerService.isEnabled()) {
                KeyValueBackupJob.schedule(this.backupManagerService.getContext());
                this.backupManagerService.scheduleNextFullBackupJob(0);
            }
        }
    }
}
