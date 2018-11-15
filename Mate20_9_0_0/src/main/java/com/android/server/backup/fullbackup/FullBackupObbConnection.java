package com.android.server.backup.fullbackup;

import android.app.backup.IBackupManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.util.Slog;
import com.android.internal.backup.IObbBackupService;
import com.android.internal.backup.IObbBackupService.Stub;
import com.android.internal.util.Preconditions;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.utils.FullBackupUtils;
import java.io.IOException;
import java.io.OutputStream;

public class FullBackupObbConnection implements ServiceConnection {
    private BackupManagerService backupManagerService;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    volatile IObbBackupService mService = null;

    public FullBackupObbConnection(BackupManagerService backupManagerService) {
        this.backupManagerService = backupManagerService;
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public void establish() {
        this.backupManagerService.getContext().bindServiceAsUser(new Intent().setComponent(new ComponentName(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, "com.android.sharedstoragebackup.ObbBackupService")), this, 1, UserHandle.SYSTEM);
    }

    public void tearDown() {
        this.backupManagerService.getContext().unbindService(this);
    }

    public boolean backupObbs(PackageInfo pkg, OutputStream out) {
        boolean success = false;
        waitForConnection();
        ParcelFileDescriptor[] pipes = null;
        try {
            pipes = ParcelFileDescriptor.createPipe();
            int token = this.backupManagerService.generateRandomIntegerToken();
            this.backupManagerService.prepareOperationTimeout(token, this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis(), null, 0);
            this.mService.backupObbs(pkg.packageName, pipes[1], token, this.backupManagerService.getBackupManagerBinder());
            FullBackupUtils.routeSocketDataToOutput(pipes[0], out);
            success = this.backupManagerService.waitUntilOperationComplete(token);
            try {
                out.flush();
                if (pipes != null) {
                    if (pipes[0] != null) {
                        pipes[0].close();
                    }
                    if (pipes[1] != null) {
                        pipes[1].close();
                    }
                }
            } catch (IOException e) {
                Slog.w(BackupManagerService.TAG, "I/O error closing down OBB backup", e);
            }
        } catch (Exception e2) {
            String str = BackupManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to back up OBBs for ");
            stringBuilder.append(pkg);
            Slog.w(str, stringBuilder.toString(), e2);
            out.flush();
            if (pipes != null) {
                if (pipes[0] != null) {
                    pipes[0].close();
                }
                if (pipes[1] != null) {
                    pipes[1].close();
                }
            }
        } catch (Throwable th) {
            try {
                out.flush();
                if (pipes != null) {
                    if (pipes[0] != null) {
                        pipes[0].close();
                    }
                    if (pipes[1] != null) {
                        pipes[1].close();
                    }
                }
            } catch (IOException e3) {
                Slog.w(BackupManagerService.TAG, "I/O error closing down OBB backup", e3);
            }
            throw th;
        }
        return success;
    }

    public void restoreObbFile(String pkgName, ParcelFileDescriptor data, long fileSize, int type, String path, long mode, long mtime, int token, IBackupManager callbackBinder) {
        waitForConnection();
        try {
            this.mService.restoreObbFile(pkgName, data, fileSize, type, path, mode, mtime, token, callbackBinder);
            String str = pkgName;
        } catch (Exception e) {
            String str2 = BackupManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to restore OBBs for ");
            stringBuilder.append(pkgName);
            Slog.w(str2, stringBuilder.toString(), e);
        }
    }

    private void waitForConnection() {
        synchronized (this) {
            while (this.mService == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this) {
            this.mService = Stub.asInterface(service);
            notifyAll();
        }
    }

    public void onServiceDisconnected(ComponentName name) {
        synchronized (this) {
            this.mService = null;
            notifyAll();
        }
    }
}
