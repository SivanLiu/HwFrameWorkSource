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
import com.android.server.job.controllers.JobStatus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import libcore.io.IoUtils;

class KeyValueAdbBackupEngine {
    private static final String BACKUP_KEY_VALUE_BACKUP_DATA_FILENAME_SUFFIX = ".data";
    private static final String BACKUP_KEY_VALUE_BLANK_STATE_FILENAME = "blank_state";
    private static final String BACKUP_KEY_VALUE_DIRECTORY_NAME = "key_value_dir";
    private static final String BACKUP_KEY_VALUE_NEW_STATE_FILENAME_SUFFIX = ".new";
    private static final boolean DEBUG = false;
    private static final String TAG = "KeyValueAdbBackupEngine";
    private ParcelFileDescriptor mBackupData;
    private final File mBackupDataName;
    private BackupManagerService mBackupManagerService;
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

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void run() {
            try {
                FullBackupDataOutput output = new FullBackupDataOutput(this.mPipe);
                BackupManagerService.writeAppManifest(this.mPackage, KeyValueAdbBackupEngine.this.mPackageManager, KeyValueAdbBackupEngine.this.mManifestFile, false, false);
                FullBackup.backupToTar(this.mPackage.packageName, "k", null, KeyValueAdbBackupEngine.this.mDataDir.getAbsolutePath(), KeyValueAdbBackupEngine.this.mManifestFile.getAbsolutePath(), output);
                KeyValueAdbBackupEngine.this.mManifestFile.delete();
                FullBackup.backupToTar(this.mPackage.packageName, "k", null, KeyValueAdbBackupEngine.this.mDataDir.getAbsolutePath(), KeyValueAdbBackupEngine.this.mBackupDataName.getAbsolutePath(), output);
                try {
                    new FileOutputStream(this.mPipe.getFileDescriptor()).write(new byte[4]);
                } catch (IOException e) {
                    Slog.e(KeyValueAdbBackupEngine.TAG, "Unable to finalize backup stream!");
                }
                try {
                    KeyValueAdbBackupEngine.this.mBackupManagerService.mBackupManagerBinder.opComplete(this.mToken, 0);
                } catch (RemoteException e2) {
                }
                IoUtils.closeQuietly(this.mPipe);
            } catch (IOException e3) {
                Slog.e(KeyValueAdbBackupEngine.TAG, "Error running full backup for " + this.mPackage.packageName + ". " + e3);
            } catch (Throwable th) {
                IoUtils.closeQuietly(this.mPipe);
            }
        }
    }

    private void writeBackupData() throws java.io.IOException {
        /* JADX: method processing error */
/*
Error: java.util.NoSuchElementException
	at java.util.HashMap$HashIterator.nextNode(HashMap.java:1439)
	at java.util.HashMap$KeyIterator.next(HashMap.java:1461)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.applyRemove(BlockFinallyExtract.java:537)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.extractFinally(BlockFinallyExtract.java:176)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.processExceptionHandler(BlockFinallyExtract.java:81)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:52)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r12 = this;
        r11 = 0;
        r10 = 1;
        r0 = r12.mBackupManagerService;
        r1 = r0.generateToken();
        r7 = 0;
        r7 = android.os.ParcelFileDescriptor.createPipe();	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = r12.mBackupManagerService;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = 30000; // 0x7530 float:4.2039E-41 double:1.4822E-319;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r4 = 0;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r5 = 0;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0.prepareOperationTimeout(r1, r2, r4, r5);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r8 = new com.android.server.backup.KeyValueAdbBackupEngine$KeyValueAdbBackupDataCopier;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = r12.mCurrentPackage;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = 1;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r7[r2];	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r8.<init>(r0, r2, r1);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = 1;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = r7[r0];	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0.close();	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = 0;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = 1;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r7[r2] = r0;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r9 = new java.lang.Thread;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = "key-value-app-data-runner";	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r9.<init>(r8, r0);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r9.start();	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = 0;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = r7[r0];	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r12.mOutput;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        com.android.server.backup.BackupManagerService.routeSocketDataToOutput(r0, r2);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = r12.mBackupManagerService;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = r0.waitUntilOperationComplete(r1);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        if (r0 != 0) goto L_0x0063;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
    L_0x0045:
        r0 = "KeyValueAdbBackupEngine";	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2.<init>();	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r3 = "Full backup failed on package ";	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.append(r3);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r3 = r12.mCurrentPackage;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r3 = r3.packageName;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.append(r3);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.toString();	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        android.util.Slog.e(r0, r2);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
    L_0x0063:
        r0 = r12.mOutput;
        r0.flush();
        if (r7 == 0) goto L_0x0074;
    L_0x006a:
        r0 = r7[r11];
        libcore.io.IoUtils.closeQuietly(r0);
        r0 = r7[r10];
        libcore.io.IoUtils.closeQuietly(r0);
    L_0x0074:
        return;
    L_0x0075:
        r6 = move-exception;
        r0 = "KeyValueAdbBackupEngine";	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2.<init>();	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r3 = "Error backing up ";	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.append(r3);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r3 = r12.mCurrentPackage;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r3 = r3.packageName;	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.append(r3);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r3 = ": ";	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.append(r3);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.append(r6);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r2 = r2.toString();	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        android.util.Slog.e(r0, r2);	 Catch:{ IOException -> 0x0075, all -> 0x00b1 }
        r0 = r12.mOutput;
        r0.flush();
        if (r7 == 0) goto L_0x0074;
    L_0x00a6:
        r0 = r7[r11];
        libcore.io.IoUtils.closeQuietly(r0);
        r0 = r7[r10];
        libcore.io.IoUtils.closeQuietly(r0);
        goto L_0x0074;
    L_0x00b1:
        r0 = move-exception;
        r2 = r12.mOutput;
        r2.flush();
        if (r7 == 0) goto L_0x00c3;
    L_0x00b9:
        r2 = r7[r11];
        libcore.io.IoUtils.closeQuietly(r2);
        r2 = r7[r10];
        libcore.io.IoUtils.closeQuietly(r2);
    L_0x00c3:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.backup.KeyValueAdbBackupEngine.writeBackupData():void");
    }

    KeyValueAdbBackupEngine(OutputStream output, PackageInfo packageInfo, BackupManagerService backupManagerService, PackageManager packageManager, File baseStateDir, File dataDir) {
        this.mOutput = output;
        this.mCurrentPackage = packageInfo;
        this.mBackupManagerService = backupManagerService;
        this.mPackageManager = packageManager;
        this.mDataDir = dataDir;
        this.mStateDir = new File(baseStateDir, BACKUP_KEY_VALUE_DIRECTORY_NAME);
        this.mStateDir.mkdirs();
        String pkg = this.mCurrentPackage.packageName;
        this.mBackupDataName = new File(this.mDataDir, pkg + BACKUP_KEY_VALUE_BACKUP_DATA_FILENAME_SUFFIX);
        this.mNewStateName = new File(this.mStateDir, pkg + BACKUP_KEY_VALUE_NEW_STATE_FILENAME_SUFFIX);
        this.mManifestFile = new File(this.mDataDir, "_manifest");
    }

    void backupOnePackage() throws IOException {
        ApplicationInfo targetApp = this.mCurrentPackage.applicationInfo;
        try {
            prepareBackupFiles(this.mCurrentPackage.packageName);
            IBackupAgent agent = bindToAgent(targetApp);
            if (agent == null) {
                Slog.e(TAG, "Failed binding to BackupAgent for package " + this.mCurrentPackage.packageName);
            } else if (invokeAgentForAdbBackup(this.mCurrentPackage.packageName, agent)) {
                writeBackupData();
                cleanup();
            } else {
                Slog.e(TAG, "Backup Failed for package " + this.mCurrentPackage.packageName);
                cleanup();
            }
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "Failed creating files for package " + this.mCurrentPackage.packageName + " will ignore package. " + e);
        } finally {
            cleanup();
        }
    }

    private void prepareBackupFiles(String packageName) throws FileNotFoundException {
        this.mSavedState = ParcelFileDescriptor.open(this.mBlankStateName, 402653184);
        this.mBackupData = ParcelFileDescriptor.open(this.mBackupDataName, 1006632960);
        if (!SELinux.restorecon(this.mBackupDataName)) {
            Slog.e(TAG, "SELinux restorecon failed on " + this.mBackupDataName);
        }
        this.mNewState = ParcelFileDescriptor.open(this.mNewStateName, 1006632960);
    }

    private IBackupAgent bindToAgent(ApplicationInfo targetApp) {
        try {
            return this.mBackupManagerService.bindToAgentSynchronous(targetApp, 0);
        } catch (SecurityException e) {
            Slog.e(TAG, "error in binding to agent for package " + targetApp.packageName + ". " + e);
            return null;
        }
    }

    private boolean invokeAgentForAdbBackup(String packageName, IBackupAgent agent) {
        int token = this.mBackupManagerService.generateToken();
        try {
            this.mBackupManagerService.prepareOperationTimeout(token, 30000, null, 0);
            agent.doBackup(this.mSavedState, this.mBackupData, this.mNewState, JobStatus.NO_LATEST_RUNTIME, token, this.mBackupManagerService.mBackupManagerBinder);
            if (this.mBackupManagerService.waitUntilOperationComplete(token)) {
                return true;
            }
            Slog.e(TAG, "Key-value backup failed on package " + packageName);
            return false;
        } catch (RemoteException e) {
            Slog.e(TAG, "Error invoking agent for backup on " + packageName + ". " + e);
            return false;
        }
    }

    private void cleanup() {
        this.mBackupManagerService.tearDownAgentAndKill(this.mCurrentPackage.applicationInfo);
        this.mBlankStateName.delete();
        this.mNewStateName.delete();
        this.mBackupDataName.delete();
    }
}
