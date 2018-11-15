package com.android.server.backup.internal;

import android.database.ContentObserver;
import android.os.Handler;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.KeyValueBackupJob;

public class ProvisionedObserver extends ContentObserver {
    private BackupManagerService backupManagerService;

    public ProvisionedObserver(BackupManagerService backupManagerService, Handler handler) {
        super(handler);
        this.backupManagerService = backupManagerService;
    }

    public void onChange(boolean selfChange) {
        boolean wasProvisioned = this.backupManagerService.isProvisioned();
        boolean isProvisioned = this.backupManagerService.deviceIsProvisioned();
        BackupManagerService backupManagerService = this.backupManagerService;
        boolean z = wasProvisioned || isProvisioned;
        backupManagerService.setProvisioned(z);
        synchronized (this.backupManagerService.getQueueLock()) {
            if (this.backupManagerService.isProvisioned() && !wasProvisioned && this.backupManagerService.isEnabled()) {
                KeyValueBackupJob.schedule(this.backupManagerService.getContext(), this.backupManagerService.getConstants());
                this.backupManagerService.scheduleNextFullBackupJob(0);
            }
        }
    }
}
