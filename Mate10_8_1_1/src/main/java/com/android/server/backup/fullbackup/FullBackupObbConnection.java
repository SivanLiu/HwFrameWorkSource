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
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.utils.FullBackupUtils;
import java.io.IOException;
import java.io.OutputStream;

public class FullBackupObbConnection implements ServiceConnection {
    private RefactoredBackupManagerService backupManagerService;
    volatile IObbBackupService mService = null;

    public FullBackupObbConnection(RefactoredBackupManagerService backupManagerService) {
        this.backupManagerService = backupManagerService;
    }

    public void establish() {
        this.backupManagerService.getContext().bindServiceAsUser(new Intent().setComponent(new ComponentName(RefactoredBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, "com.android.sharedstoragebackup.ObbBackupService")), this, 1, UserHandle.SYSTEM);
    }

    public void tearDown() {
        this.backupManagerService.getContext().unbindService(this);
    }

    public boolean backupObbs(PackageInfo pkg, OutputStream out) {
        boolean success = false;
        waitForConnection();
        ParcelFileDescriptor[] parcelFileDescriptorArr = null;
        try {
            parcelFileDescriptorArr = ParcelFileDescriptor.createPipe();
            int token = this.backupManagerService.generateRandomIntegerToken();
            this.backupManagerService.prepareOperationTimeout(token, RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, null, 0);
            this.mService.backupObbs(pkg.packageName, parcelFileDescriptorArr[1], token, this.backupManagerService.getBackupManagerBinder());
            FullBackupUtils.routeSocketDataToOutput(parcelFileDescriptorArr[0], out);
            success = this.backupManagerService.waitUntilOperationComplete(token);
            try {
                out.flush();
                if (parcelFileDescriptorArr != null) {
                    if (parcelFileDescriptorArr[0] != null) {
                        parcelFileDescriptorArr[0].close();
                    }
                    if (parcelFileDescriptorArr[1] != null) {
                        parcelFileDescriptorArr[1].close();
                    }
                }
            } catch (IOException e) {
                Slog.w(RefactoredBackupManagerService.TAG, "I/O error closing down OBB backup", e);
            }
        } catch (Exception e2) {
            Slog.w(RefactoredBackupManagerService.TAG, "Unable to back up OBBs for " + pkg, e2);
            try {
                out.flush();
                if (parcelFileDescriptorArr != null) {
                    if (parcelFileDescriptorArr[0] != null) {
                        parcelFileDescriptorArr[0].close();
                    }
                    if (parcelFileDescriptorArr[1] != null) {
                        parcelFileDescriptorArr[1].close();
                    }
                }
            } catch (IOException e3) {
                Slog.w(RefactoredBackupManagerService.TAG, "I/O error closing down OBB backup", e3);
            }
        } catch (Throwable th) {
            try {
                out.flush();
                if (parcelFileDescriptorArr != null) {
                    if (parcelFileDescriptorArr[0] != null) {
                        parcelFileDescriptorArr[0].close();
                    }
                    if (parcelFileDescriptorArr[1] != null) {
                        parcelFileDescriptorArr[1].close();
                    }
                }
            } catch (IOException e32) {
                Slog.w(RefactoredBackupManagerService.TAG, "I/O error closing down OBB backup", e32);
            }
        }
        return success;
    }

    public void restoreObbFile(String pkgName, ParcelFileDescriptor data, long fileSize, int type, String path, long mode, long mtime, int token, IBackupManager callbackBinder) {
        waitForConnection();
        try {
            this.mService.restoreObbFile(pkgName, data, fileSize, type, path, mode, mtime, token, callbackBinder);
        } catch (Exception e) {
            Slog.w(RefactoredBackupManagerService.TAG, "Unable to restore OBBs for " + pkgName, e);
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
