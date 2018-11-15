package com.android.server.backup.internal;

import android.content.pm.PackageInfo;
import android.util.Slog;
import com.android.internal.backup.IBackupTransport;
import com.android.server.backup.RefactoredBackupManagerService;
import java.io.File;

public class PerformClearTask implements Runnable {
    private RefactoredBackupManagerService backupManagerService;
    PackageInfo mPackage;
    IBackupTransport mTransport;

    PerformClearTask(RefactoredBackupManagerService backupManagerService, IBackupTransport transport, PackageInfo packageInfo) {
        this.backupManagerService = backupManagerService;
        this.mTransport = transport;
        this.mPackage = packageInfo;
    }

    public void run() {
        try {
            new File(new File(this.backupManagerService.getBaseStateDir(), this.mTransport.transportDirName()), this.mPackage.packageName).delete();
            this.mTransport.clearBackupData(this.mPackage);
        } catch (Exception e) {
            Slog.e(RefactoredBackupManagerService.TAG, "Transport threw clearing data for " + this.mPackage + ": " + e.getMessage());
        } finally {
            try {
                this.mTransport.finishBackup();
            } catch (Exception e2) {
                Slog.e(RefactoredBackupManagerService.TAG, "Unable to mark clear operation finished: " + e2.getMessage());
            }
            this.backupManagerService.getWakelock().release();
        }
    }
}
