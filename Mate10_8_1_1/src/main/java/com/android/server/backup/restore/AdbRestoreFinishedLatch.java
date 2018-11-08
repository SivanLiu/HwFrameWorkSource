package com.android.server.backup.restore;

import android.util.Slog;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.RefactoredBackupManagerService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AdbRestoreFinishedLatch implements BackupRestoreTask {
    private static final String TAG = "AdbRestoreFinishedLatch";
    private RefactoredBackupManagerService backupManagerService;
    private final int mCurrentOpToken;
    final CountDownLatch mLatch = new CountDownLatch(1);

    public AdbRestoreFinishedLatch(RefactoredBackupManagerService backupManagerService, int currentOpToken) {
        this.backupManagerService = backupManagerService;
        this.mCurrentOpToken = currentOpToken;
    }

    void await() {
        try {
            boolean latched = this.mLatch.await(RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Slog.w(TAG, "Interrupted!");
        }
    }

    public void execute() {
    }

    public void operationComplete(long result) {
        this.mLatch.countDown();
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }

    public void handleCancel(boolean cancelAll) {
        Slog.w(TAG, "adb onRestoreFinished() timed out");
        this.mLatch.countDown();
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }
}
