package com.android.server.backup;

import android.app.IBackupAgent;
import android.app.backup.FullBackup;
import android.app.backup.FullBackupDataOutput;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.backup.utils.FullBackupUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import libcore.io.IoUtils;

public class KeyValueAdbBackupEngine {
    private static final String BACKUP_KEY_VALUE_BACKUP_DATA_FILENAME_SUFFIX = ".data";
    private static final String BACKUP_KEY_VALUE_BLANK_STATE_FILENAME = "blank_state";
    private static final String BACKUP_KEY_VALUE_DIRECTORY_NAME = "key_value_dir";
    private static final String BACKUP_KEY_VALUE_NEW_STATE_FILENAME_SUFFIX = ".new";
    private static final boolean DEBUG = false;
    private static final String TAG = "KeyValueAdbBackupEngine";
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private ParcelFileDescriptor mBackupData;
    private final File mBackupDataName;
    private BackupManagerServiceInterface mBackupManagerService;
    private final File mBlankStateName = new File(this.mStateDir, BACKUP_KEY_VALUE_BLANK_STATE_FILENAME);
    private final PackageInfo mCurrentPackage;
    private final File mDataDir;
    private final File mManifestFile;
    private ParcelFileDescriptor mNewState;
    private final File mNewStateName;
    private final OutputStream mOutput;
    private final PackageManager mPackageManager;
    private ParcelFileDescriptor mSavedState;
    private final File mStateDir;

    class KeyValueAdbBackupDataCopier implements Runnable {
        private final PackageInfo mPackage;
        private final ParcelFileDescriptor mPipe;
        private final int mToken;

        KeyValueAdbBackupDataCopier(PackageInfo pack, ParcelFileDescriptor pipe, int token) throws IOException {
            this.mPackage = pack;
            this.mPipe = ParcelFileDescriptor.dup(pipe.getFileDescriptor());
            this.mToken = token;
        }

        public void run() {
            try {
                FullBackupDataOutput output = new FullBackupDataOutput(this.mPipe);
                FullBackupUtils.writeAppManifest(this.mPackage, KeyValueAdbBackupEngine.this.mPackageManager, KeyValueAdbBackupEngine.this.mManifestFile, false, false);
                FullBackup.backupToTar(this.mPackage.packageName, "k", null, KeyValueAdbBackupEngine.this.mDataDir.getAbsolutePath(), KeyValueAdbBackupEngine.this.mManifestFile.getAbsolutePath(), output);
                KeyValueAdbBackupEngine.this.mManifestFile.delete();
                FullBackup.backupToTar(this.mPackage.packageName, "k", null, KeyValueAdbBackupEngine.this.mDataDir.getAbsolutePath(), KeyValueAdbBackupEngine.this.mBackupDataName.getAbsolutePath(), output);
                try {
                    new FileOutputStream(this.mPipe.getFileDescriptor()).write(new byte[4]);
                } catch (IOException e) {
                    Slog.e(KeyValueAdbBackupEngine.TAG, "Unable to finalize backup stream!");
                }
                try {
                    KeyValueAdbBackupEngine.this.mBackupManagerService.getBackupManagerBinder().opComplete(this.mToken, 0);
                } catch (RemoteException e2) {
                }
            } catch (IOException e3) {
                String str = KeyValueAdbBackupEngine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error running full backup for ");
                stringBuilder.append(this.mPackage.packageName);
                stringBuilder.append(". ");
                stringBuilder.append(e3);
                Slog.e(str, stringBuilder.toString());
            } catch (Throwable th) {
                IoUtils.closeQuietly(this.mPipe);
            }
            IoUtils.closeQuietly(this.mPipe);
        }
    }

    public KeyValueAdbBackupEngine(OutputStream output, PackageInfo packageInfo, BackupManagerServiceInterface backupManagerService, PackageManager packageManager, File baseStateDir, File dataDir) {
        this.mOutput = output;
        this.mCurrentPackage = packageInfo;
        this.mBackupManagerService = backupManagerService;
        this.mPackageManager = packageManager;
        this.mDataDir = dataDir;
        this.mStateDir = new File(baseStateDir, BACKUP_KEY_VALUE_DIRECTORY_NAME);
        this.mStateDir.mkdirs();
        String pkg = this.mCurrentPackage.packageName;
        File file = this.mDataDir;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pkg);
        stringBuilder.append(BACKUP_KEY_VALUE_BACKUP_DATA_FILENAME_SUFFIX);
        this.mBackupDataName = new File(file, stringBuilder.toString());
        file = this.mStateDir;
        stringBuilder = new StringBuilder();
        stringBuilder.append(pkg);
        stringBuilder.append(BACKUP_KEY_VALUE_NEW_STATE_FILENAME_SUFFIX);
        this.mNewStateName = new File(file, stringBuilder.toString());
        this.mManifestFile = new File(this.mDataDir, BackupManagerService.BACKUP_MANIFEST_FILENAME);
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
    }

    public void backupOnePackage() throws IOException {
        ApplicationInfo targetApp = this.mCurrentPackage.applicationInfo;
        String str;
        StringBuilder stringBuilder;
        try {
            prepareBackupFiles(this.mCurrentPackage.packageName);
            IBackupAgent agent = bindToAgent(targetApp);
            if (agent == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed binding to BackupAgent for package ");
                stringBuilder.append(this.mCurrentPackage.packageName);
                Slog.e(str, stringBuilder.toString());
                cleanup();
            } else if (invokeAgentForAdbBackup(this.mCurrentPackage.packageName, agent)) {
                writeBackupData();
                cleanup();
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Backup Failed for package ");
                stringBuilder.append(this.mCurrentPackage.packageName);
                Slog.e(str, stringBuilder.toString());
                cleanup();
            }
        } catch (FileNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed creating files for package ");
            stringBuilder.append(this.mCurrentPackage.packageName);
            stringBuilder.append(" will ignore package. ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            cleanup();
        }
    }

    private void prepareBackupFiles(String packageName) throws FileNotFoundException {
        this.mSavedState = ParcelFileDescriptor.open(this.mBlankStateName, 402653184);
        this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
        if (!SELinux.restorecon(this.mBackupDataName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SELinux restorecon failed on ");
            stringBuilder.append(this.mBackupDataName);
            Slog.e(str, stringBuilder.toString());
        }
        this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
    }

    private IBackupAgent bindToAgent(ApplicationInfo targetApp) {
        try {
            return this.mBackupManagerService.bindToAgentSynchronous(targetApp, 0);
        } catch (SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("error in binding to agent for package ");
            stringBuilder.append(targetApp.packageName);
            stringBuilder.append(". ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return null;
        }
    }

    private boolean invokeAgentForAdbBackup(String packageName, IBackupAgent agent) {
        RemoteException e;
        String str;
        StringBuilder stringBuilder;
        String str2 = packageName;
        int token = this.mBackupManagerService.generateRandomIntegerToken();
        int token2;
        try {
            this.mBackupManagerService.prepareOperationTimeout(token, this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis(), null, 0);
            token2 = token;
            try {
                agent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, JobStatus.NO_LATEST_RUNTIME, token, this.mBackupManagerService.getBackupManagerBinder(), 0);
                if (this.mBackupManagerService.waitUntilOperationComplete(token2)) {
                    return true;
                }
                String str3 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Key-value backup failed on package ");
                stringBuilder2.append(str2);
                Slog.e(str3, stringBuilder2.toString());
                return false;
            } catch (RemoteException e2) {
                e = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error invoking agent for backup on ");
                stringBuilder.append(str2);
                stringBuilder.append(". ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return false;
            }
        } catch (RemoteException e3) {
            e = e3;
            token2 = token;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error invoking agent for backup on ");
            stringBuilder.append(str2);
            stringBuilder.append(". ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:29:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x00a8  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x00b8  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void writeBackupData() throws IOException {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        Throwable th;
        int token = this.mBackupManagerService.generateRandomIntegerToken();
        long kvBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getKvBackupAgentTimeoutMillis();
        ParcelFileDescriptor[] pipes = null;
        ParcelFileDescriptor[] pipes2;
        try {
            pipes2 = ParcelFileDescriptor.createPipe();
            AutoCloseable autoCloseable;
            try {
                this.mBackupManagerService.prepareOperationTimeout(token, kvBackupAgentTimeoutMillis, null, 0);
                KeyValueAdbBackupDataCopier runner = new KeyValueAdbBackupDataCopier(this.mCurrentPackage, pipes2[1], token);
                pipes2[1].close();
                pipes2[1] = null;
                new Thread(runner, "key-value-app-data-runner").start();
                FullBackupUtils.routeSocketDataToOutput(pipes2[0], this.mOutput);
                if (!this.mBackupManagerService.waitUntilOperationComplete(token)) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Full backup failed on package ");
                    stringBuilder2.append(this.mCurrentPackage.packageName);
                    Slog.e(str2, stringBuilder2.toString());
                }
                this.mOutput.flush();
                if (pipes2 != null) {
                    IoUtils.closeQuietly(pipes2[0]);
                    autoCloseable = pipes2[1];
                    IoUtils.closeQuietly(autoCloseable);
                }
            } catch (IOException e2) {
                e = e2;
                try {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error backing up ");
                    stringBuilder.append(this.mCurrentPackage.packageName);
                    stringBuilder.append(": ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                    this.mOutput.flush();
                    if (pipes2 == null) {
                        IoUtils.closeQuietly(pipes2[0]);
                        autoCloseable = pipes2[1];
                        IoUtils.closeQuietly(autoCloseable);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    this.mOutput.flush();
                    if (pipes2 != null) {
                        IoUtils.closeQuietly(pipes2[0]);
                        IoUtils.closeQuietly(pipes2[1]);
                    }
                    throw th;
                }
            }
        } catch (IOException e3) {
            pipes2 = pipes;
            e = e3;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error backing up ");
            stringBuilder.append(this.mCurrentPackage.packageName);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            this.mOutput.flush();
            if (pipes2 == null) {
            }
        } catch (Throwable th3) {
            pipes2 = pipes;
            th = th3;
            this.mOutput.flush();
            if (pipes2 != null) {
            }
            throw th;
        }
    }

    private void cleanup() {
        this.mBackupManagerService.tearDownAgentAndKill(this.mCurrentPackage.applicationInfo);
        this.mBlankStateName.delete();
        this.mNewStateName.delete();
        this.mBackupDataName.delete();
    }
}
