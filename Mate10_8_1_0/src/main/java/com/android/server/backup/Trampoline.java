package com.android.server.backup;

import android.app.backup.IBackupManager.Stub;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IBackupObserver;
import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.app.backup.ISelectBackupTransportCallback;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;

public class Trampoline extends Stub {
    static final String BACKUP_DISABLE_PROPERTY = "ro.backup.disable";
    static final String BACKUP_SUPPRESS_FILENAME = "backup-suppress";
    static final boolean DEBUG_TRAMPOLINE = false;
    static final String TAG = "BackupManagerService";
    final Context mContext;
    final boolean mGlobalDisable = isBackupDisabled();
    volatile BackupManagerServiceInterface mService;
    final File mSuppressFile = getSuppressFile();

    public Trampoline(Context context) {
        this.mContext = context;
        this.mSuppressFile.getParentFile().mkdirs();
    }

    protected BackupManagerServiceInterface createService() {
        if (isRefactoredServiceEnabled()) {
            Slog.i("BackupManagerService", "Instantiating RefactoredBackupManagerService");
            return createRefactoredBackupManagerService();
        }
        Slog.i("BackupManagerService", "Instantiating BackupManagerService");
        return createBackupManagerService();
    }

    protected boolean isBackupDisabled() {
        return SystemProperties.getBoolean(BACKUP_DISABLE_PROPERTY, false);
    }

    protected boolean isRefactoredServiceEnabled() {
        return Global.getInt(this.mContext.getContentResolver(), "backup_refactored_service_disabled", 1) == 0;
    }

    protected int binderGetCallingUid() {
        return Binder.getCallingUid();
    }

    protected File getSuppressFile() {
        return new File(new File(Environment.getDataDirectory(), "backup"), BACKUP_SUPPRESS_FILENAME);
    }

    protected BackupManagerServiceInterface createRefactoredBackupManagerService() {
        return new RefactoredBackupManagerService(this.mContext, this);
    }

    protected BackupManagerServiceInterface createBackupManagerService() {
        return new BackupManagerService(this.mContext, this);
    }

    public void initialize(int whichUser) {
        if (whichUser == 0) {
            if (this.mGlobalDisable) {
                Slog.i("BackupManagerService", "Backup/restore not supported");
                return;
            }
            synchronized (this) {
                if (this.mSuppressFile.exists()) {
                    Slog.i("BackupManagerService", "Backup inactive in user " + whichUser);
                } else {
                    this.mService = createService();
                }
            }
        }
    }

    public void setBackupServiceActive(int userHandle, boolean makeActive) {
        int caller = binderGetCallingUid();
        if (caller != 1000 && caller != 0) {
            throw new SecurityException("No permission to configure backup activity");
        } else if (this.mGlobalDisable) {
            Slog.i("BackupManagerService", "Backup/restore not supported");
        } else {
            if (userHandle == 0) {
                synchronized (this) {
                    if (makeActive != isBackupServiceActive(userHandle)) {
                        Slog.i("BackupManagerService", "Making backup " + (makeActive ? "" : "in") + "active in user " + userHandle);
                        if (makeActive) {
                            this.mService = createService();
                            this.mSuppressFile.delete();
                        } else {
                            this.mService = null;
                            try {
                                this.mSuppressFile.createNewFile();
                            } catch (IOException e) {
                                Slog.e("BackupManagerService", "Unable to persist backup service inactivity");
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isBackupServiceActive(int userHandle) {
        boolean z = false;
        if (userHandle != 0) {
            return false;
        }
        synchronized (this) {
            if (this.mService != null) {
                z = true;
            }
        }
        return z;
    }

    public void dataChanged(String packageName) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.dataChanged(packageName);
        }
    }

    public void initializeTransports(String[] transportNames, IBackupObserver observer) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.initializeTransports(transportNames, observer);
        }
    }

    public void clearBackupData(String transportName, String packageName) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.clearBackupData(transportName, packageName);
        }
    }

    public void agentConnected(String packageName, IBinder agent) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.agentConnected(packageName, agent);
        }
    }

    public void agentDisconnected(String packageName) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.agentDisconnected(packageName);
        }
    }

    public void restoreAtInstall(String packageName, int token) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.restoreAtInstall(packageName, token);
        }
    }

    public void setBackupEnabled(boolean isEnabled) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.setBackupEnabled(isEnabled);
        }
    }

    public void setAutoRestore(boolean doAutoRestore) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.setAutoRestore(doAutoRestore);
        }
    }

    public void setBackupProvisioned(boolean isProvisioned) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.setBackupProvisioned(isProvisioned);
        }
    }

    public boolean isBackupEnabled() throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        return svc != null ? svc.isBackupEnabled() : false;
    }

    public boolean setBackupPassword(String currentPw, String newPw) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        return svc != null ? svc.setBackupPassword(currentPw, newPw) : false;
    }

    public boolean hasBackupPassword() throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        return svc != null ? svc.hasBackupPassword() : false;
    }

    public void backupNow() throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.backupNow();
        }
    }

    public void adbBackup(ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, boolean allApps, boolean allIncludesSystem, boolean doCompress, boolean doKeyValue, String[] packageNames) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.adbBackup(fd, includeApks, includeObbs, includeShared, doWidgets, allApps, allIncludesSystem, doCompress, doKeyValue, packageNames);
        }
    }

    public void fullTransportBackup(String[] packageNames) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.fullTransportBackup(packageNames);
        }
    }

    public void adbRestore(ParcelFileDescriptor fd) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.adbRestore(fd);
        }
    }

    public void acknowledgeFullBackupOrRestore(int token, boolean allow, String curPassword, String encryptionPassword, IFullBackupRestoreObserver observer) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.acknowledgeAdbBackupOrRestore(token, allow, curPassword, encryptionPassword, observer);
        }
    }

    public String getCurrentTransport() throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.getCurrentTransport();
        }
        return null;
    }

    public String[] listAllTransports() throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.listAllTransports();
        }
        return null;
    }

    public ComponentName[] listAllTransportComponents() throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.listAllTransportComponents();
        }
        return null;
    }

    public String[] getTransportWhitelist() {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.getTransportWhitelist();
        }
        return null;
    }

    public String selectBackupTransport(String transport) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.selectBackupTransport(transport);
        }
        return null;
    }

    public void selectBackupTransportAsync(ComponentName transport, ISelectBackupTransportCallback listener) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.selectBackupTransportAsync(transport, listener);
        } else if (listener != null) {
            try {
                listener.onFailure(-2001);
            } catch (RemoteException e) {
            }
        }
    }

    public Intent getConfigurationIntent(String transport) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.getConfigurationIntent(transport);
        }
        return null;
    }

    public String getDestinationString(String transport) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.getDestinationString(transport);
        }
        return null;
    }

    public Intent getDataManagementIntent(String transport) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.getDataManagementIntent(transport);
        }
        return null;
    }

    public String getDataManagementLabel(String transport) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.getDataManagementLabel(transport);
        }
        return null;
    }

    public IRestoreSession beginRestoreSession(String packageName, String transportID) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            return svc.beginRestoreSession(packageName, transportID);
        }
        return null;
    }

    public void opComplete(int token, long result) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.opComplete(token, result);
        }
    }

    public long getAvailableRestoreToken(String packageName) {
        BackupManagerServiceInterface svc = this.mService;
        return svc != null ? svc.getAvailableRestoreToken(packageName) : 0;
    }

    public boolean isAppEligibleForBackup(String packageName) {
        BackupManagerServiceInterface svc = this.mService;
        return svc != null ? svc.isAppEligibleForBackup(packageName) : false;
    }

    public int requestBackup(String[] packages, IBackupObserver observer, IBackupManagerMonitor monitor, int flags) throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc == null) {
            return -2001;
        }
        return svc.requestBackup(packages, observer, monitor, flags);
    }

    public void cancelBackups() throws RemoteException {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.cancelBackups();
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, "BackupManagerService", pw)) {
            BackupManagerServiceInterface svc = this.mService;
            if (svc != null) {
                svc.dump(fd, pw, args);
            } else {
                pw.println("Inactive");
            }
        }
    }

    boolean beginFullBackup(FullBackupJob scheduledJob) {
        BackupManagerServiceInterface svc = this.mService;
        return svc != null ? svc.beginFullBackup(scheduledJob) : false;
    }

    void endFullBackup() {
        BackupManagerServiceInterface svc = this.mService;
        if (svc != null) {
            svc.endFullBackup();
        }
    }
}
