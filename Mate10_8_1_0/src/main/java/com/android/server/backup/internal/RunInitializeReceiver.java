package com.android.server.backup.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Slog;
import com.android.server.backup.RefactoredBackupManagerService;

public class RunInitializeReceiver extends BroadcastReceiver {
    private RefactoredBackupManagerService backupManagerService;

    public RunInitializeReceiver(RefactoredBackupManagerService backupManagerService) {
        this.backupManagerService = backupManagerService;
    }

    public void onReceive(Context context, Intent intent) {
        if (RefactoredBackupManagerService.RUN_INITIALIZE_ACTION.equals(intent.getAction())) {
            synchronized (this.backupManagerService.getQueueLock()) {
                Slog.v(RefactoredBackupManagerService.TAG, "Running a device init");
                String[] pendingInits = (String[]) this.backupManagerService.getPendingInits().toArray();
                this.backupManagerService.clearPendingInits();
                PerformInitializeTask initTask = new PerformInitializeTask(this.backupManagerService, pendingInits, null);
                this.backupManagerService.getWakelock().acquire();
                this.backupManagerService.getBackupHandler().post(initTask);
            }
        }
    }
}
