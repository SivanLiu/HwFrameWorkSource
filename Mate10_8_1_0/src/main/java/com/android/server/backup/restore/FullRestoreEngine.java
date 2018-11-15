package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.KeyValueAdbRestoreEngine;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.utils.BytesReadListener;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;
import com.android.server.backup.utils.RestoreUtils;
import com.android.server.backup.utils.TarBackupReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

public class FullRestoreEngine extends RestoreEngine {
    private static final /* synthetic */ int[] -com-android-server-backup-restore-RestorePolicySwitchesValues = null;
    private IBackupAgent mAgent;
    private String mAgentPackage;
    final boolean mAllowApks;
    private final boolean mAllowObbs;
    private final RefactoredBackupManagerService mBackupManagerService;
    final byte[] mBuffer;
    private long mBytes;
    private final HashSet<String> mClearedPackages = new HashSet();
    private final RestoreDeleteObserver mDeleteObserver = new RestoreDeleteObserver();
    final int mEphemeralOpToken;
    private final RestoreInstallObserver mInstallObserver = new RestoreInstallObserver();
    private final HashMap<String, Signature[]> mManifestSignatures = new HashMap();
    final IBackupManagerMonitor mMonitor;
    private final BackupRestoreTask mMonitorTask;
    private FullBackupObbConnection mObbConnection = null;
    private IFullBackupRestoreObserver mObserver;
    final PackageInfo mOnlyPackage;
    private final HashMap<String, String> mPackageInstallers = new HashMap();
    private final HashMap<String, RestorePolicy> mPackagePolicies = new HashMap();
    private ParcelFileDescriptor[] mPipes = null;
    private ApplicationInfo mTargetApp;
    private byte[] mWidgetData = null;

    private static /* synthetic */ int[] -getcom-android-server-backup-restore-RestorePolicySwitchesValues() {
        if (-com-android-server-backup-restore-RestorePolicySwitchesValues != null) {
            return -com-android-server-backup-restore-RestorePolicySwitchesValues;
        }
        int[] iArr = new int[RestorePolicy.values().length];
        try {
            iArr[RestorePolicy.ACCEPT.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[RestorePolicy.ACCEPT_IF_APK.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[RestorePolicy.IGNORE.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        -com-android-server-backup-restore-RestorePolicySwitchesValues = iArr;
        return iArr;
    }

    public FullRestoreEngine(RefactoredBackupManagerService backupManagerService, BackupRestoreTask monitorTask, IFullBackupRestoreObserver observer, IBackupManagerMonitor monitor, PackageInfo onlyPackage, boolean allowApks, boolean allowObbs, int ephemeralOpToken) {
        this.mBackupManagerService = backupManagerService;
        this.mEphemeralOpToken = ephemeralOpToken;
        this.mMonitorTask = monitorTask;
        this.mObserver = observer;
        this.mMonitor = monitor;
        this.mOnlyPackage = onlyPackage;
        this.mAllowApks = allowApks;
        this.mAllowObbs = allowObbs;
        this.mBuffer = new byte[32768];
        this.mBytes = 0;
    }

    public IBackupAgent getAgent() {
        return this.mAgent;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }

    public boolean restoreOneFile(InputStream instream, boolean mustKillAgent, byte[] buffer, PackageInfo onlyPackage, boolean allowApks, int token, IBackupManagerMonitor monitor) {
        if (isRunning()) {
            boolean z;
            BytesReadListener bytesReadListener = new BytesReadListener() {
                public void onBytesRead(long bytesRead) {
                    FullRestoreEngine fullRestoreEngine = FullRestoreEngine.this;
                    fullRestoreEngine.mBytes = fullRestoreEngine.mBytes + bytesRead;
                }
            };
            TarBackupReader tarBackupReader = new TarBackupReader(instream, bytesReadListener, monitor);
            FileMetadata info = tarBackupReader.readTarHeaders();
            if (info != null) {
                String pkg = info.packageName;
                if (!pkg.equals(this.mAgentPackage)) {
                    if (onlyPackage == null || pkg.equals(onlyPackage.packageName)) {
                        if (!this.mPackagePolicies.containsKey(pkg)) {
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                        }
                        if (this.mAgent != null) {
                            Slog.d(RefactoredBackupManagerService.TAG, "Saw new package; finalizing old one");
                            tearDownPipes();
                            tearDownAgent(this.mTargetApp);
                            this.mTargetApp = null;
                            this.mAgentPackage = null;
                        }
                    } else {
                        Slog.w(RefactoredBackupManagerService.TAG, "Expected data for " + onlyPackage + " but saw " + pkg);
                        setResult(-3);
                        setRunning(false);
                        return false;
                    }
                }
                if (info.path.equals(RefactoredBackupManagerService.BACKUP_MANIFEST_FILENAME)) {
                    Object signatures = tarBackupReader.readAppManifestAndReturnSignatures(info);
                    RestorePolicy restorePolicy = tarBackupReader.chooseRestorePolicy(this.mBackupManagerService.getPackageManager(), allowApks, info, signatures);
                    this.mManifestSignatures.put(info.packageName, signatures);
                    this.mPackagePolicies.put(pkg, restorePolicy);
                    this.mPackageInstallers.put(pkg, info.installerPackageName);
                    tarBackupReader.skipTarPadding(info.size);
                    this.mObserver = FullBackupRestoreObserverUtils.sendOnRestorePackage(this.mObserver, pkg);
                } else if (info.path.equals(RefactoredBackupManagerService.BACKUP_METADATA_FILENAME)) {
                    tarBackupReader.readMetadata(info);
                    this.mWidgetData = tarBackupReader.getWidgetData();
                    monitor = tarBackupReader.getMonitor();
                    tarBackupReader.skipTarPadding(info.size);
                } else {
                    boolean okay = true;
                    switch (-getcom-android-server-backup-restore-RestorePolicySwitchesValues()[((RestorePolicy) this.mPackagePolicies.get(pkg)).ordinal()]) {
                        case 1:
                            if (info.domain.equals("a")) {
                                Slog.d(RefactoredBackupManagerService.TAG, "apk present but ACCEPT");
                                okay = false;
                                break;
                            }
                            break;
                        case 2:
                            if (!info.domain.equals("a")) {
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                                okay = false;
                                break;
                            }
                            Object obj;
                            Slog.d(RefactoredBackupManagerService.TAG, "APK file; installing");
                            boolean isSuccessfullyInstalled = RestoreUtils.installApk(instream, this.mBackupManagerService.getPackageManager(), this.mInstallObserver, this.mDeleteObserver, this.mManifestSignatures, this.mPackagePolicies, info, (String) this.mPackageInstallers.get(pkg), bytesReadListener, this.mBackupManagerService.getDataDir());
                            HashMap hashMap = this.mPackagePolicies;
                            if (isSuccessfullyInstalled) {
                                obj = RestorePolicy.ACCEPT;
                            } else {
                                obj = RestorePolicy.IGNORE;
                            }
                            hashMap.put(pkg, obj);
                            tarBackupReader.skipTarPadding(info.size);
                            return true;
                        case 3:
                            okay = false;
                            break;
                        default:
                            Slog.e(RefactoredBackupManagerService.TAG, "Invalid policy from manifest");
                            okay = false;
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                            break;
                    }
                    if (!(isRestorableFile(info) && (isCanonicalFilePath(info.path) ^ 1) == 0)) {
                        okay = false;
                    }
                    if (okay && this.mAgent == null) {
                        try {
                            this.mTargetApp = this.mBackupManagerService.getPackageManager().getApplicationInfo(pkg, 0);
                            if (!this.mClearedPackages.contains(pkg)) {
                                if (this.mTargetApp.backupAgentName == null) {
                                    Slog.d(RefactoredBackupManagerService.TAG, "Clearing app data preparatory to full restore");
                                    this.mBackupManagerService.clearApplicationDataSynchronous(pkg);
                                }
                                this.mClearedPackages.add(pkg);
                            }
                            setUpPipes();
                            this.mAgent = this.mBackupManagerService.bindToAgentSynchronous(this.mTargetApp, 3);
                            this.mAgentPackage = pkg;
                        } catch (IOException e) {
                        } catch (NameNotFoundException e2) {
                        }
                        try {
                            if (this.mAgent == null) {
                                Slog.e(RefactoredBackupManagerService.TAG, "Unable to create agent for " + pkg);
                                okay = false;
                                tearDownPipes();
                                this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                            }
                        } catch (IOException e3) {
                            Slog.w(RefactoredBackupManagerService.TAG, "io exception on restore socket read: " + e3.getMessage());
                            setResult(-3);
                            info = null;
                        }
                    }
                    if (okay && (pkg.equals(this.mAgentPackage) ^ 1) != 0) {
                        Slog.e(RefactoredBackupManagerService.TAG, "Restoring data for " + pkg + " but agent is for " + this.mAgentPackage);
                        okay = false;
                    }
                    if (okay) {
                        long timeout;
                        boolean agentSuccess = true;
                        long toCopy = info.size;
                        if (pkg.equals(RefactoredBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE)) {
                            timeout = 1800000;
                        } else {
                            timeout = 60000;
                        }
                        try {
                            this.mBackupManagerService.prepareOperationTimeout(token, timeout, this.mMonitorTask, 1);
                            if ("obb".equals(info.domain)) {
                                Slog.d(RefactoredBackupManagerService.TAG, "Restoring OBB file for " + pkg + " : " + info.path);
                                this.mObbConnection.restoreObbFile(pkg, this.mPipes[0], info.size, info.type, info.path, info.mode, info.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                            } else if ("k".equals(info.domain)) {
                                Slog.d(RefactoredBackupManagerService.TAG, "Restoring key-value file for " + pkg + " : " + info.path);
                                new Thread(new KeyValueAdbRestoreEngine(this.mBackupManagerService, this.mBackupManagerService.getDataDir(), info, this.mPipes[0], this.mAgent, token), "restore-key-value-runner").start();
                            } else if (this.mTargetApp.processName.equals("system")) {
                                Slog.d(RefactoredBackupManagerService.TAG, "system process agent - spinning a thread");
                                new Thread(new RestoreFileRunnable(this.mBackupManagerService, this.mAgent, info, this.mPipes[0], token), "restore-sys-runner").start();
                            } else {
                                this.mAgent.doRestoreFile(this.mPipes[0], info.size, info.type, info.domain, info.path, info.mode, info.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                            }
                        } catch (IOException e4) {
                            Slog.d(RefactoredBackupManagerService.TAG, "Couldn't establish restore");
                            agentSuccess = false;
                            okay = false;
                        } catch (RemoteException e5) {
                            Slog.e(RefactoredBackupManagerService.TAG, "Agent crashed during full restore");
                            agentSuccess = false;
                            okay = false;
                        }
                        if (okay) {
                            boolean pipeOkay = true;
                            FileOutputStream fileOutputStream = new FileOutputStream(this.mPipes[1].getFileDescriptor());
                            while (toCopy > 0) {
                                int nRead = instream.read(buffer, 0, toCopy > ((long) buffer.length) ? buffer.length : (int) toCopy);
                                if (nRead >= 0) {
                                    this.mBytes += (long) nRead;
                                }
                                if (nRead <= 0) {
                                    tarBackupReader.skipTarPadding(info.size);
                                    agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                                } else {
                                    toCopy -= (long) nRead;
                                    if (pipeOkay) {
                                        try {
                                            fileOutputStream.write(buffer, 0, nRead);
                                        } catch (IOException e32) {
                                            Slog.e(RefactoredBackupManagerService.TAG, "Failed to write to restore pipe: " + e32.getMessage());
                                            pipeOkay = false;
                                        }
                                    }
                                }
                            }
                            tarBackupReader.skipTarPadding(info.size);
                            agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                        }
                        if (!agentSuccess) {
                            Slog.w(RefactoredBackupManagerService.TAG, "Agent failure restoring " + pkg + "; ending restore");
                            this.mBackupManagerService.getBackupHandler().removeMessages(18);
                            tearDownPipes();
                            tearDownAgent(this.mTargetApp);
                            this.mAgent = null;
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                            if (onlyPackage != null) {
                                setResult(-2);
                                setRunning(false);
                                return false;
                            }
                        }
                    }
                    if (!okay) {
                        long bytesToConsume = (info.size + 511) & -512;
                        while (bytesToConsume > 0) {
                            long nRead2 = (long) instream.read(buffer, 0, bytesToConsume > ((long) buffer.length) ? buffer.length : (int) bytesToConsume);
                            if (nRead2 >= 0) {
                                this.mBytes += nRead2;
                            }
                            if (nRead2 > 0) {
                                bytesToConsume -= nRead2;
                            }
                        }
                    }
                }
            }
            if (info == null) {
                tearDownPipes();
                setRunning(false);
                if (mustKillAgent) {
                    tearDownAgent(this.mTargetApp);
                }
            }
            if (info != null) {
                z = true;
            } else {
                z = false;
            }
            return z;
        }
        Slog.w(RefactoredBackupManagerService.TAG, "Restore engine used after halting");
        return false;
    }

    private void setUpPipes() throws IOException {
        this.mPipes = ParcelFileDescriptor.createPipe();
    }

    private void tearDownPipes() {
        synchronized (this) {
            if (this.mPipes != null) {
                try {
                    this.mPipes[0].close();
                    this.mPipes[0] = null;
                    this.mPipes[1].close();
                    this.mPipes[1] = null;
                } catch (IOException e) {
                    Slog.w(RefactoredBackupManagerService.TAG, "Couldn't close agent pipes", e);
                }
                this.mPipes = null;
            }
        }
    }

    private void tearDownAgent(ApplicationInfo app) {
        if (this.mAgent != null) {
            this.mBackupManagerService.tearDownAgentAndKill(app);
            this.mAgent = null;
        }
    }

    void handleTimeout() {
        tearDownPipes();
        setResult(-2);
        setRunning(false);
    }

    private static boolean isRestorableFile(FileMetadata info) {
        if ("c".equals(info.domain)) {
            return false;
        }
        if ("r".equals(info.domain) && info.path.startsWith("no_backup/")) {
            return false;
        }
        return true;
    }

    private static boolean isCanonicalFilePath(String path) {
        if (path.contains("..") || path.contains("//")) {
            return false;
        }
        return true;
    }

    void sendOnRestorePackage(String name) {
        if (this.mObserver != null) {
            try {
                this.mObserver.onRestorePackage(name);
            } catch (RemoteException e) {
                Slog.w(RefactoredBackupManagerService.TAG, "full restore observer went away: restorePackage");
                this.mObserver = null;
            }
        }
    }
}
