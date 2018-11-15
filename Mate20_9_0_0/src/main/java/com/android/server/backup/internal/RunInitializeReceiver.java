package com.android.server.backup.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager.WakeLock;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;

public class RunInitializeReceiver extends BroadcastReceiver {
    private final BackupManagerService mBackupManagerService;

    public RunInitializeReceiver(BackupManagerService backupManagerService) {
        this.mBackupManagerService = backupManagerService;
    }

    public void onReceive(Context context, Intent intent) {
        if (BackupManagerService.RUN_INITIALIZE_ACTION.equals(intent.getAction())) {
            synchronized (this.mBackupManagerService.getQueueLock()) {
                ArraySet<String> pendingInits = this.mBackupManagerService.getPendingInits();
                String str = BackupManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Running a device init; ");
                stringBuilder.append(pendingInits.size());
                stringBuilder.append(" pending");
                Slog.v(str, stringBuilder.toString());
                if (pendingInits.size() > 0) {
                    String[] transports = (String[]) pendingInits.toArray(new String[pendingInits.size()]);
                    this.mBackupManagerService.clearPendingInits();
                    WakeLock wakelock = this.mBackupManagerService.getWakelock();
                    wakelock.acquire();
                    this.mBackupManagerService.getBackupHandler().post(new PerformInitializeTask(this.mBackupManagerService, transports, null, new -$$Lambda$RunInitializeReceiver$6NFkS59RniyJ8xe_gfe6oyt63HQ(wakelock)));
                }
            }
        }
    }
}
