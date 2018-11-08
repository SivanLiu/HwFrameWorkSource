package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.KeyValueAdbRestoreEngine;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.utils.BytesReadListener;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;
import com.android.server.backup.utils.PasswordUtils;
import com.android.server.backup.utils.RestoreUtils;
import com.android.server.backup.utils.TarBackupReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.InflaterInputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PerformAdbRestoreTask implements Runnable {
    private static final /* synthetic */ int[] -com-android-server-backup-restore-RestorePolicySwitchesValues = null;
    private IBackupAgent mAgent;
    private String mAgentPackage;
    private final RefactoredBackupManagerService mBackupManagerService;
    private long mBytes;
    private final HashSet<String> mClearedPackages = new HashSet();
    private final String mCurrentPassword;
    private final String mDecryptPassword;
    private final RestoreDeleteObserver mDeleteObserver = new RestoreDeleteObserver();
    private final ParcelFileDescriptor mInputFile;
    private final RestoreInstallObserver mInstallObserver = new RestoreInstallObserver();
    private final AtomicBoolean mLatchObject;
    private final HashMap<String, Signature[]> mManifestSignatures = new HashMap();
    private FullBackupObbConnection mObbConnection = null;
    private IFullBackupRestoreObserver mObserver;
    private final HashMap<String, String> mPackageInstallers = new HashMap();
    private final PackageManagerBackupAgent mPackageManagerBackupAgent;
    private final HashMap<String, RestorePolicy> mPackagePolicies = new HashMap();
    private ParcelFileDescriptor[] mPipes = null;
    private ApplicationInfo mTargetApp;
    private byte[] mWidgetData = null;

    private static class RestoreFinishedRunnable implements Runnable {
        private final IBackupAgent mAgent;
        private final RefactoredBackupManagerService mBackupManagerService;
        private final int mToken;

        RestoreFinishedRunnable(IBackupAgent agent, int token, RefactoredBackupManagerService backupManagerService) {
            this.mAgent = agent;
            this.mToken = token;
            this.mBackupManagerService = backupManagerService;
        }

        public void run() {
            try {
                this.mAgent.doRestoreFinished(this.mToken, this.mBackupManagerService.getBackupManagerBinder());
            } catch (RemoteException e) {
            }
        }
    }

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

    public PerformAdbRestoreTask(RefactoredBackupManagerService backupManagerService, ParcelFileDescriptor fd, String curPassword, String decryptPassword, IFullBackupRestoreObserver observer, AtomicBoolean latch) {
        this.mBackupManagerService = backupManagerService;
        this.mInputFile = fd;
        this.mCurrentPassword = curPassword;
        this.mDecryptPassword = decryptPassword;
        this.mObserver = observer;
        this.mLatchObject = latch;
        this.mAgent = null;
        this.mPackageManagerBackupAgent = new PackageManagerBackupAgent(backupManagerService.getPackageManager());
        this.mAgentPackage = null;
        this.mTargetApp = null;
        this.mObbConnection = new FullBackupObbConnection(backupManagerService);
        this.mClearedPackages.add("android");
        this.mClearedPackages.add(RefactoredBackupManagerService.SETTINGS_PACKAGE);
    }

    public void run() {
        Throwable th;
        Slog.i(RefactoredBackupManagerService.TAG, "--- Performing full-dataset restore ---");
        this.mObbConnection.establish();
        this.mObserver = FullBackupRestoreObserverUtils.sendStartRestore(this.mObserver);
        if (Environment.getExternalStorageState().equals("mounted")) {
            this.mPackagePolicies.put(RefactoredBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, RestorePolicy.ACCEPT);
        }
        FileInputStream fileInputStream = null;
        try {
            if (this.mBackupManagerService.backupPasswordMatches(this.mCurrentPassword)) {
                this.mBytes = 0;
                FileInputStream rawInStream = new FileInputStream(this.mInputFile.getFileDescriptor());
                try {
                    InputStream tarInputStream = parseBackupFileHeaderAndReturnTarStream(rawInStream, this.mDecryptPassword);
                    if (tarInputStream == null) {
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, true);
                        if (rawInStream != null) {
                            try {
                                rawInStream.close();
                            } catch (IOException e) {
                                Slog.w(RefactoredBackupManagerService.TAG, "Close of restore data pipe threw", e);
                            }
                        }
                        this.mInputFile.close();
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                        }
                        this.mObbConnection.tearDown();
                        this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                        Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
                        this.mBackupManagerService.getWakelock().release();
                        return;
                    }
                    do {
                    } while (restoreOneFile(tarInputStream, false, new byte[32768], null, true, this.mBackupManagerService.generateRandomIntegerToken(), null));
                    tearDownPipes();
                    tearDownAgent(this.mTargetApp, true);
                    if (rawInStream != null) {
                        try {
                            rawInStream.close();
                        } catch (IOException e2) {
                            Slog.w(RefactoredBackupManagerService.TAG, "Close of restore data pipe threw", e2);
                        }
                    }
                    this.mInputFile.close();
                    synchronized (this.mLatchObject) {
                        this.mLatchObject.set(true);
                        this.mLatchObject.notifyAll();
                    }
                    this.mObbConnection.tearDown();
                    this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                    Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
                    this.mBackupManagerService.getWakelock().release();
                    fileInputStream = rawInStream;
                } catch (IOException e3) {
                    fileInputStream = rawInStream;
                    try {
                        Slog.e(RefactoredBackupManagerService.TAG, "Unable to read restore input");
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, true);
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e22) {
                                Slog.w(RefactoredBackupManagerService.TAG, "Close of restore data pipe threw", e22);
                                synchronized (this.mLatchObject) {
                                    this.mLatchObject.set(true);
                                    this.mLatchObject.notifyAll();
                                }
                                this.mObbConnection.tearDown();
                                this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                                Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
                                this.mBackupManagerService.getWakelock().release();
                            }
                        }
                        this.mInputFile.close();
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                        }
                        this.mObbConnection.tearDown();
                        this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                        Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
                        this.mBackupManagerService.getWakelock().release();
                    } catch (Throwable th2) {
                        th = th2;
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, true);
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e222) {
                                Slog.w(RefactoredBackupManagerService.TAG, "Close of restore data pipe threw", e222);
                                synchronized (this.mLatchObject) {
                                    this.mLatchObject.set(true);
                                    this.mLatchObject.notifyAll();
                                }
                                this.mObbConnection.tearDown();
                                this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                                Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
                                this.mBackupManagerService.getWakelock().release();
                                throw th;
                            }
                        }
                        this.mInputFile.close();
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                        }
                        this.mObbConnection.tearDown();
                        this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                        Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
                        this.mBackupManagerService.getWakelock().release();
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fileInputStream = rawInStream;
                    tearDownPipes();
                    tearDownAgent(this.mTargetApp, true);
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    this.mInputFile.close();
                    synchronized (this.mLatchObject) {
                        this.mLatchObject.set(true);
                        this.mLatchObject.notifyAll();
                    }
                    this.mObbConnection.tearDown();
                    this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                    Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
                    this.mBackupManagerService.getWakelock().release();
                    throw th;
                }
            }
            Slog.w(RefactoredBackupManagerService.TAG, "Backup password mismatch; aborting");
            tearDownPipes();
            tearDownAgent(this.mTargetApp, true);
            try {
                this.mInputFile.close();
            } catch (IOException e2222) {
                Slog.w(RefactoredBackupManagerService.TAG, "Close of restore data pipe threw", e2222);
            }
            synchronized (this.mLatchObject) {
                this.mLatchObject.set(true);
                this.mLatchObject.notifyAll();
            }
            this.mObbConnection.tearDown();
            this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
            Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
            this.mBackupManagerService.getWakelock().release();
        } catch (IOException e4) {
            Slog.e(RefactoredBackupManagerService.TAG, "Unable to read restore input");
            tearDownPipes();
            tearDownAgent(this.mTargetApp, true);
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            this.mInputFile.close();
            synchronized (this.mLatchObject) {
                this.mLatchObject.set(true);
                this.mLatchObject.notifyAll();
            }
            this.mObbConnection.tearDown();
            this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
            Slog.d(RefactoredBackupManagerService.TAG, "Full restore pass complete.");
            this.mBackupManagerService.getWakelock().release();
        }
    }

    private static void readFullyOrThrow(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int bytesRead = in.read(buffer, offset, buffer.length - offset);
            if (bytesRead <= 0) {
                throw new IOException("Couldn't fully read data");
            }
            offset += bytesRead;
        }
    }

    public static InputStream parseBackupFileHeaderAndReturnTarStream(InputStream rawInputStream, String decryptPassword) throws IOException {
        boolean compressed = false;
        InputStream preCompressStream = rawInputStream;
        boolean okay = false;
        byte[] streamHeader = new byte[RefactoredBackupManagerService.BACKUP_FILE_HEADER_MAGIC.length()];
        readFullyOrThrow(rawInputStream, streamHeader);
        if (Arrays.equals(RefactoredBackupManagerService.BACKUP_FILE_HEADER_MAGIC.getBytes("UTF-8"), streamHeader)) {
            String s = readHeaderLine(rawInputStream);
            int archiveVersion = Integer.parseInt(s);
            if (archiveVersion <= 5) {
                boolean pbkdf2Fallback = archiveVersion == 1;
                compressed = Integer.parseInt(readHeaderLine(rawInputStream)) != 0;
                s = readHeaderLine(rawInputStream);
                if (s.equals("none")) {
                    okay = true;
                } else if (decryptPassword == null || decryptPassword.length() <= 0) {
                    Slog.w(RefactoredBackupManagerService.TAG, "Archive is encrypted but no password given");
                } else {
                    preCompressStream = decodeAesHeaderAndInitialize(decryptPassword, s, pbkdf2Fallback, rawInputStream);
                    if (preCompressStream != null) {
                        okay = true;
                    }
                }
            } else {
                Slog.w(RefactoredBackupManagerService.TAG, "Wrong header version: " + s);
            }
        } else {
            Slog.w(RefactoredBackupManagerService.TAG, "Didn't read the right header magic");
        }
        if (okay) {
            if (compressed) {
                preCompressStream = new InflaterInputStream(preCompressStream);
            }
            return preCompressStream;
        }
        Slog.w(RefactoredBackupManagerService.TAG, "Invalid restore data; aborting.");
        return null;
    }

    private static String readHeaderLine(InputStream in) throws IOException {
        StringBuilder buffer = new StringBuilder(80);
        while (true) {
            int c = in.read();
            if (c >= 0 && c != 10) {
                buffer.append((char) c);
            }
        }
        return buffer.toString();
    }

    private static InputStream attemptMasterKeyDecryption(String decryptPassword, String algorithm, byte[] userSalt, byte[] ckSalt, int rounds, String userIvHex, String masterKeyBlobHex, InputStream rawInStream, boolean doLog) {
        try {
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKey userKey = PasswordUtils.buildPasswordKey(algorithm, decryptPassword, userSalt, rounds);
            c.init(2, new SecretKeySpec(userKey.getEncoded(), "AES"), new IvParameterSpec(PasswordUtils.hexToByteArray(userIvHex)));
            byte[] mkBlob = c.doFinal(PasswordUtils.hexToByteArray(masterKeyBlobHex));
            int len = mkBlob[0];
            byte[] IV = Arrays.copyOfRange(mkBlob, 1, len + 1);
            int offset = len + 1;
            int offset2 = offset + 1;
            len = mkBlob[offset];
            byte[] mk = Arrays.copyOfRange(mkBlob, offset2, offset2 + len);
            offset = offset2 + len;
            offset2 = offset + 1;
            if (Arrays.equals(PasswordUtils.makeKeyChecksum(algorithm, mk, ckSalt, rounds), Arrays.copyOfRange(mkBlob, offset2, offset2 + mkBlob[offset]))) {
                IvParameterSpec ivSpec = new IvParameterSpec(IV);
                c.init(2, new SecretKeySpec(mk, "AES"), ivSpec);
                return new CipherInputStream(rawInStream, c);
            } else if (!doLog) {
                return null;
            } else {
                Slog.w(RefactoredBackupManagerService.TAG, "Incorrect password");
                return null;
            }
        } catch (InvalidAlgorithmParameterException e) {
            if (!doLog) {
                return null;
            }
            Slog.e(RefactoredBackupManagerService.TAG, "Needed parameter spec unavailable!", e);
            return null;
        } catch (BadPaddingException e2) {
            if (!doLog) {
                return null;
            }
            Slog.w(RefactoredBackupManagerService.TAG, "Incorrect password");
            return null;
        } catch (IllegalBlockSizeException e3) {
            if (!doLog) {
                return null;
            }
            Slog.w(RefactoredBackupManagerService.TAG, "Invalid block size in master key");
            return null;
        } catch (NoSuchAlgorithmException e4) {
            if (!doLog) {
                return null;
            }
            Slog.e(RefactoredBackupManagerService.TAG, "Needed decryption algorithm unavailable!");
            return null;
        } catch (NoSuchPaddingException e5) {
            if (!doLog) {
                return null;
            }
            Slog.e(RefactoredBackupManagerService.TAG, "Needed padding mechanism unavailable!");
            return null;
        } catch (InvalidKeyException e6) {
            if (!doLog) {
                return null;
            }
            Slog.w(RefactoredBackupManagerService.TAG, "Illegal password; aborting");
            return null;
        }
    }

    private static InputStream decodeAesHeaderAndInitialize(String decryptPassword, String encryptionName, boolean pbkdf2Fallback, InputStream rawInStream) {
        try {
            if (encryptionName.equals(PasswordUtils.ENCRYPTION_ALGORITHM_NAME)) {
                byte[] userSalt = PasswordUtils.hexToByteArray(readHeaderLine(rawInStream));
                byte[] ckSalt = PasswordUtils.hexToByteArray(readHeaderLine(rawInStream));
                int rounds = Integer.parseInt(readHeaderLine(rawInStream));
                String userIvHex = readHeaderLine(rawInStream);
                String masterKeyBlobHex = readHeaderLine(rawInStream);
                InputStream result = attemptMasterKeyDecryption(decryptPassword, BackupPasswordManager.PBKDF_CURRENT, userSalt, ckSalt, rounds, userIvHex, masterKeyBlobHex, rawInStream, false);
                if (result != null || !pbkdf2Fallback) {
                    return result;
                }
                return attemptMasterKeyDecryption(decryptPassword, BackupPasswordManager.PBKDF_FALLBACK, userSalt, ckSalt, rounds, userIvHex, masterKeyBlobHex, rawInStream, true);
            }
            Slog.w(RefactoredBackupManagerService.TAG, "Unsupported encryption method: " + encryptionName);
            return null;
        } catch (NumberFormatException e) {
            Slog.w(RefactoredBackupManagerService.TAG, "Can't parse restore data header");
            return null;
        } catch (IOException e2) {
            Slog.w(RefactoredBackupManagerService.TAG, "Can't read input header");
            return null;
        }
    }

    boolean restoreOneFile(InputStream instream, boolean mustKillAgent, byte[] buffer, PackageInfo onlyPackage, boolean allowApks, int token, IBackupManagerMonitor monitor) {
        boolean z;
        BytesReadListener bytesReadListener = new BytesReadListener() {
            public void onBytesRead(long bytesRead) {
                PerformAdbRestoreTask performAdbRestoreTask = PerformAdbRestoreTask.this;
                performAdbRestoreTask.mBytes = performAdbRestoreTask.mBytes + bytesRead;
            }
        };
        TarBackupReader tarBackupReader = new TarBackupReader(instream, bytesReadListener, monitor);
        FileMetadata info = tarBackupReader.readTarHeaders();
        if (info != null) {
            String pkg = info.packageName;
            if (!pkg.equals(this.mAgentPackage)) {
                if (!this.mPackagePolicies.containsKey(pkg)) {
                    this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                }
                if (this.mAgent != null) {
                    Slog.d(RefactoredBackupManagerService.TAG, "Saw new package; finalizing old one");
                    tearDownPipes();
                    tearDownAgent(this.mTargetApp, true);
                    this.mTargetApp = null;
                    this.mAgentPackage = null;
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
                if (!isCanonicalFilePath(info.path)) {
                    okay = false;
                }
                if (okay && this.mAgent != null) {
                    Slog.i(RefactoredBackupManagerService.TAG, "Reusing existing agent instance");
                }
                if (okay && this.mAgent == null) {
                    Slog.d(RefactoredBackupManagerService.TAG, "Need to launch agent for " + pkg);
                    try {
                        int i;
                        this.mTargetApp = this.mBackupManagerService.getPackageManager().getApplicationInfo(pkg, 0);
                        if (this.mClearedPackages.contains(pkg)) {
                            Slog.d(RefactoredBackupManagerService.TAG, "We've initialized this app already; no clear required");
                        } else {
                            if (this.mTargetApp.backupAgentName == null) {
                                Slog.d(RefactoredBackupManagerService.TAG, "Clearing app data preparatory to full restore");
                                this.mBackupManagerService.clearApplicationDataSynchronous(pkg);
                            } else {
                                Slog.d(RefactoredBackupManagerService.TAG, "backup agent (" + this.mTargetApp.backupAgentName + ") => no clear");
                            }
                            this.mClearedPackages.add(pkg);
                        }
                        setUpPipes();
                        RefactoredBackupManagerService refactoredBackupManagerService = this.mBackupManagerService;
                        ApplicationInfo applicationInfo = this.mTargetApp;
                        if ("k".equals(info.domain)) {
                            i = 0;
                        } else {
                            i = 3;
                        }
                        this.mAgent = refactoredBackupManagerService.bindToAgentSynchronous(applicationInfo, i);
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
                    } catch (Throwable e3) {
                        Slog.w(RefactoredBackupManagerService.TAG, "io exception on restore socket read", e3);
                        info = null;
                    }
                }
                if (okay && (pkg.equals(this.mAgentPackage) ^ 1) != 0) {
                    Slog.e(RefactoredBackupManagerService.TAG, "Restoring data for " + pkg + " but agent is for " + this.mAgentPackage);
                    okay = false;
                }
                if (okay) {
                    boolean agentSuccess = true;
                    long toCopy = info.size;
                    try {
                        this.mBackupManagerService.prepareOperationTimeout(token, 60000, null, 1);
                        if ("obb".equals(info.domain)) {
                            Slog.d(RefactoredBackupManagerService.TAG, "Restoring OBB file for " + pkg + " : " + info.path);
                            this.mObbConnection.restoreObbFile(pkg, this.mPipes[0], info.size, info.type, info.path, info.mode, info.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                        } else if ("k".equals(info.domain)) {
                            Slog.d(RefactoredBackupManagerService.TAG, "Restoring key-value file for " + pkg + " : " + info.path);
                            new Thread(new KeyValueAdbRestoreEngine(this.mBackupManagerService, this.mBackupManagerService.getDataDir(), info, this.mPipes[0], this.mAgent, token), "restore-key-value-runner").start();
                        } else {
                            Slog.d(RefactoredBackupManagerService.TAG, "Invoking agent to restore file " + info.path);
                            if (this.mTargetApp.processName.equals("system")) {
                                Slog.d(RefactoredBackupManagerService.TAG, "system process agent - spinning a thread");
                                new Thread(new RestoreFileRunnable(this.mBackupManagerService, this.mAgent, info, this.mPipes[0], token), "restore-sys-runner").start();
                            } else {
                                this.mAgent.doRestoreFile(this.mPipes[0], info.size, info.type, info.domain, info.path, info.mode, info.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                            }
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
                                    } catch (Throwable e32) {
                                        Slog.e(RefactoredBackupManagerService.TAG, "Failed to write to restore pipe", e32);
                                        pipeOkay = false;
                                    }
                                }
                            }
                        }
                        tarBackupReader.skipTarPadding(info.size);
                        agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                    }
                    if (!agentSuccess) {
                        Slog.d(RefactoredBackupManagerService.TAG, "Agent failure restoring " + pkg + "; now ignoring");
                        this.mBackupManagerService.getBackupHandler().removeMessages(18);
                        tearDownPipes();
                        tearDownAgent(this.mTargetApp, false);
                        this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                    }
                }
                if (!okay) {
                    Slog.d(RefactoredBackupManagerService.TAG, "[discarding file content]");
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
        if (info != null) {
            z = true;
        } else {
            z = false;
        }
        return z;
    }

    private static boolean isCanonicalFilePath(String path) {
        if (path.contains("..") || path.contains("//")) {
            return false;
        }
        return true;
    }

    private void setUpPipes() throws IOException {
        this.mPipes = ParcelFileDescriptor.createPipe();
    }

    private void tearDownPipes() {
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

    private void tearDownAgent(ApplicationInfo app, boolean doRestoreFinished) {
        if (this.mAgent != null) {
            if (doRestoreFinished) {
                try {
                    int token = this.mBackupManagerService.generateRandomIntegerToken();
                    AdbRestoreFinishedLatch latch = new AdbRestoreFinishedLatch(this.mBackupManagerService, token);
                    this.mBackupManagerService.prepareOperationTimeout(token, RefactoredBackupManagerService.TIMEOUT_FULL_BACKUP_INTERVAL, latch, 1);
                    if (this.mTargetApp.processName.equals("system")) {
                        new Thread(new RestoreFinishedRunnable(this.mAgent, token, this.mBackupManagerService), "restore-sys-finished-runner").start();
                    } else {
                        this.mAgent.doRestoreFinished(token, this.mBackupManagerService.getBackupManagerBinder());
                    }
                    latch.await();
                } catch (RemoteException e) {
                    Slog.d(RefactoredBackupManagerService.TAG, "Lost app trying to shut down");
                }
            }
            this.mBackupManagerService.tearDownAgentAndKill(app);
            this.mAgent = null;
        }
    }
}
