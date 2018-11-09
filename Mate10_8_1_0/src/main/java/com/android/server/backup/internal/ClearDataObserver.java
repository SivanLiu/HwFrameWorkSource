package com.android.server.backup.internal;

import android.content.pm.IPackageDataObserver.Stub;
import com.android.server.backup.RefactoredBackupManagerService;

public class ClearDataObserver extends Stub {
    private RefactoredBackupManagerService backupManagerService;

    public ClearDataObserver(RefactoredBackupManagerService backupManagerService) {
        this.backupManagerService = backupManagerService;
    }

    public void onRemoveCompleted(String packageName, boolean succeeded) {
        synchronized (this.backupManagerService.getClearDataLock()) {
            this.backupManagerService.setClearingData(false);
            this.backupManagerService.getClearDataLock().notifyAll();
        }
    }
}
