package com.android.server.backup.internal;

import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Slog;
import com.android.server.backup.RefactoredBackupManagerService;

public class RunBackupReceiver extends BroadcastReceiver {
    private RefactoredBackupManagerService backupManagerService;

    public RunBackupReceiver(RefactoredBackupManagerService backupManagerService) {
        this.backupManagerService = backupManagerService;
    }

    public void onReceive(Context context, Intent intent) {
        if (RefactoredBackupManagerService.RUN_BACKUP_ACTION.equals(intent.getAction())) {
            synchronized (this.backupManagerService.getQueueLock()) {
                if (this.backupManagerService.getPendingInits().size() > 0) {
                    try {
                        this.backupManagerService.getAlarmManager().cancel(this.backupManagerService.getRunInitIntent());
                        this.backupManagerService.getRunInitIntent().send();
                    } catch (CanceledException e) {
                        Slog.e(RefactoredBackupManagerService.TAG, "Run init intent cancelled");
                    }
                } else if (!this.backupManagerService.isEnabled() || !this.backupManagerService.isProvisioned()) {
                    Slog.w(RefactoredBackupManagerService.TAG, "Backup pass but e=" + this.backupManagerService.isEnabled() + " p=" + this.backupManagerService.isProvisioned());
                } else if (this.backupManagerService.isBackupRunning()) {
                    Slog.i(RefactoredBackupManagerService.TAG, "Backup time but one already running");
                } else {
                    Slog.v(RefactoredBackupManagerService.TAG, "Running a backup pass");
                    this.backupManagerService.setBackupRunning(true);
                    this.backupManagerService.getWakelock().acquire();
                    this.backupManagerService.getBackupHandler().sendMessage(this.backupManagerService.getBackupHandler().obtainMessage(1));
                }
            }
        }
    }
}
