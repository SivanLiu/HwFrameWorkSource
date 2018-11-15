package com.android.server.backup.fullbackup;

import android.app.IBackupAgent;
import android.app.backup.FullBackup;
import android.app.backup.FullBackupDataOutput;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Environment.UserEnvironment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import android.util.StringBuilderPrinter;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.utils.FullBackupUtils;
import com.android.server.job.JobSchedulerShellCommand;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FullBackupEngine {
    private RefactoredBackupManagerService backupManagerService;
    IBackupAgent mAgent;
    File mFilesDir = new File("/data/system");
    boolean mIncludeApks;
    File mManifestFile = new File(this.mFilesDir, RefactoredBackupManagerService.BACKUP_MANIFEST_FILENAME);
    File mMetadataFile = new File(this.mFilesDir, RefactoredBackupManagerService.BACKUP_METADATA_FILENAME);
    private final int mOpToken;
    OutputStream mOutput;
    PackageInfo mPkg;
    FullBackupPreflight mPreflightHook;
    private final long mQuota;
    BackupRestoreTask mTimeoutMonitor;

    class FullBackupRunner implements Runnable {
        IBackupAgent mAgent;
        PackageInfo mPackage;
        ParcelFileDescriptor mPipe;
        boolean mSendApk;
        int mToken;
        byte[] mWidgetData;
        boolean mWriteManifest;

        FullBackupRunner(PackageInfo pack, IBackupAgent agent, ParcelFileDescriptor pipe, int token, boolean sendApk, boolean writeManifest, byte[] widgetData) throws IOException {
            this.mPackage = pack;
            this.mWidgetData = widgetData;
            this.mAgent = agent;
            this.mPipe = ParcelFileDescriptor.dup(pipe.getFileDescriptor());
            this.mToken = token;
            this.mSendApk = sendApk;
            this.mWriteManifest = writeManifest;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            try {
                long timeout;
                FullBackupDataOutput output = new FullBackupDataOutput(this.mPipe);
                if (this.mWriteManifest) {
                    boolean writeWidgetData = this.mWidgetData != null;
                    FullBackupUtils.writeAppManifest(this.mPackage, FullBackupEngine.this.backupManagerService.getPackageManager(), FullBackupEngine.this.mManifestFile, this.mSendApk, writeWidgetData);
                    FullBackup.backupToTar(this.mPackage.packageName, null, null, FullBackupEngine.this.mFilesDir.getAbsolutePath(), FullBackupEngine.this.mManifestFile.getAbsolutePath(), output);
                    FullBackupEngine.this.mManifestFile.delete();
                    if (writeWidgetData) {
                        FullBackupEngine.this.writeMetadata(this.mPackage, FullBackupEngine.this.mMetadataFile, this.mWidgetData);
                        FullBackup.backupToTar(this.mPackage.packageName, null, null, FullBackupEngine.this.mFilesDir.getAbsolutePath(), FullBackupEngine.this.mMetadataFile.getAbsolutePath(), output);
                        FullBackupEngine.this.mMetadataFile.delete();
                    }
                }
                if (this.mSendApk) {
                    FullBackupEngine.this.writeApkToBackup(this.mPackage, output);
                }
                if (this.mPackage.packageName.equals(RefactoredBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE)) {
                    timeout = 1800000;
                } else {
                    timeout = RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL;
                }
                Slog.d(RefactoredBackupManagerService.TAG, "Calling doFullBackup() on " + this.mPackage.packageName);
                FullBackupEngine.this.backupManagerService.prepareOperationTimeout(this.mToken, timeout, FullBackupEngine.this.mTimeoutMonitor, 0);
                this.mAgent.doFullBackup(this.mPipe, FullBackupEngine.this.mQuota, this.mToken, FullBackupEngine.this.backupManagerService.getBackupManagerBinder());
                try {
                    this.mPipe.close();
                } catch (IOException e) {
                }
            } catch (IOException e2) {
                Slog.e(RefactoredBackupManagerService.TAG, "Error running full backup for " + this.mPackage.packageName);
            } catch (RemoteException e3) {
                Slog.e(RefactoredBackupManagerService.TAG, "Remote agent vanished during full backup of " + this.mPackage.packageName);
                try {
                    this.mPipe.close();
                } catch (IOException e4) {
                }
            } catch (Throwable th) {
                try {
                    this.mPipe.close();
                } catch (IOException e5) {
                }
            }
        }
    }

    public FullBackupEngine(RefactoredBackupManagerService backupManagerService, OutputStream output, FullBackupPreflight preflightHook, PackageInfo pkg, boolean alsoApks, BackupRestoreTask timeoutMonitor, long quota, int opToken) {
        this.backupManagerService = backupManagerService;
        this.mOutput = output;
        this.mPreflightHook = preflightHook;
        this.mPkg = pkg;
        this.mIncludeApks = alsoApks;
        this.mTimeoutMonitor = timeoutMonitor;
        this.mQuota = quota;
        this.mOpToken = opToken;
    }

    public int preflightCheck() throws RemoteException {
        if (this.mPreflightHook == null) {
            return 0;
        }
        if (initializeAgent()) {
            return this.mPreflightHook.preflightFullBackup(this.mPkg, this.mAgent);
        }
        Slog.w(RefactoredBackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
        return -1003;
    }

    public int backupOnePackage() throws RemoteException {
        int result = -1003;
        if (initializeAgent()) {
            try {
                ParcelFileDescriptor[] pipes = ParcelFileDescriptor.createPipe();
                ApplicationInfo app = this.mPkg.applicationInfo;
                boolean isSharedStorage = this.mPkg.packageName.equals(RefactoredBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                boolean sendApk = (this.mIncludeApks && (isSharedStorage ^ 1) != 0 && (app.privateFlags & 4) == 0) ? (app.flags & 1) != 0 ? (app.flags & 128) != 0 : true : false;
                FullBackupRunner runner = new FullBackupRunner(this.mPkg, this.mAgent, pipes[1], this.mOpToken, sendApk, isSharedStorage ^ 1, AppWidgetBackupBridge.getWidgetState(this.mPkg.packageName, 0));
                pipes[1].close();
                pipes[1] = null;
                new Thread(runner, "app-data-runner").start();
                FullBackupUtils.routeSocketDataToOutput(pipes[0], this.mOutput);
                if (this.backupManagerService.waitUntilOperationComplete(this.mOpToken)) {
                    result = 0;
                } else {
                    Slog.e(RefactoredBackupManagerService.TAG, "Full backup failed on package " + this.mPkg.packageName);
                }
                try {
                    this.mOutput.flush();
                    if (pipes != null) {
                        if (pipes[0] != null) {
                            pipes[0].close();
                        }
                        if (pipes[1] != null) {
                            pipes[1].close();
                        }
                    }
                } catch (IOException e) {
                    Slog.w(RefactoredBackupManagerService.TAG, "Error bringing down backup stack");
                    result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    tearDown();
                    return result;
                }
            } catch (IOException e2) {
                Slog.e(RefactoredBackupManagerService.TAG, "Error backing up " + this.mPkg.packageName + ": " + e2.getMessage());
                result = -1003;
                try {
                    this.mOutput.flush();
                    if (null != null) {
                        if (null[0] != null) {
                            null[0].close();
                        }
                        if (null[1] != null) {
                            null[1].close();
                        }
                    }
                } catch (IOException e3) {
                    Slog.w(RefactoredBackupManagerService.TAG, "Error bringing down backup stack");
                    result = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                    tearDown();
                    return result;
                }
            } catch (Throwable th) {
                try {
                    this.mOutput.flush();
                    if (null != null) {
                        if (null[0] != null) {
                            null[0].close();
                        }
                        if (null[1] != null) {
                            null[1].close();
                        }
                    }
                } catch (IOException e4) {
                    Slog.w(RefactoredBackupManagerService.TAG, "Error bringing down backup stack");
                }
            }
        } else {
            Slog.w(RefactoredBackupManagerService.TAG, "Unable to bind to full agent for " + this.mPkg.packageName);
        }
        tearDown();
        return result;
    }

    public void sendQuotaExceeded(long backupDataBytes, long quotaBytes) {
        if (initializeAgent()) {
            try {
                this.mAgent.doQuotaExceeded(backupDataBytes, quotaBytes);
            } catch (RemoteException e) {
                Slog.e(RefactoredBackupManagerService.TAG, "Remote exception while telling agent about quota exceeded");
            }
        }
    }

    private boolean initializeAgent() {
        if (this.mAgent == null) {
            this.mAgent = this.backupManagerService.bindToAgentSynchronous(this.mPkg.applicationInfo, 1);
        }
        if (this.mAgent != null) {
            return true;
        }
        return false;
    }

    private void writeApkToBackup(PackageInfo pkg, FullBackupDataOutput output) {
        String appSourceDir = pkg.applicationInfo.getBaseCodePath();
        FullBackup.backupToTar(pkg.packageName, "a", null, new File(appSourceDir).getParent(), appSourceDir, output);
        File obbDir = new UserEnvironment(0).buildExternalStorageAppObbDirs(pkg.packageName)[0];
        if (obbDir != null) {
            File[] obbFiles = obbDir.listFiles();
            if (obbFiles != null) {
                String obbDirName = obbDir.getAbsolutePath();
                for (File obb : obbFiles) {
                    FullBackup.backupToTar(pkg.packageName, "obb", null, obbDirName, obb.getAbsolutePath(), output);
                }
            }
        }
    }

    private void writeMetadata(PackageInfo pkg, File destination, byte[] widgetData) throws IOException {
        StringBuilder b = new StringBuilder(512);
        StringBuilderPrinter printer = new StringBuilderPrinter(b);
        printer.println(Integer.toString(1));
        printer.println(pkg.packageName);
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(destination));
        DataOutputStream out = new DataOutputStream(bout);
        bout.write(b.toString().getBytes());
        if (widgetData != null && widgetData.length > 0) {
            out.writeInt(RefactoredBackupManagerService.BACKUP_WIDGET_METADATA_TOKEN);
            out.writeInt(widgetData.length);
            out.write(widgetData);
        }
        bout.flush();
        out.close();
        destination.setLastModified(0);
    }

    private void tearDown() {
        if (this.mPkg != null) {
            this.backupManagerService.tearDownAgentAndKill(this.mPkg.applicationInfo);
        }
    }
}
