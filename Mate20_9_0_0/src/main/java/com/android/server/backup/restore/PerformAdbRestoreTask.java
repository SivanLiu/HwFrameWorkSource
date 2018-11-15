package com.android.server.backup.restore;

import android.app.IBackupAgent;
import android.app.backup.IBackupManagerMonitor;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.KeyValueAdbRestoreEngine;
import com.android.server.backup.PackageManagerBackupAgent;
import com.android.server.backup.fullbackup.FullBackupObbConnection;
import com.android.server.backup.utils.BytesReadListener;
import com.android.server.backup.utils.FullBackupRestoreObserverUtils;
import com.android.server.backup.utils.PasswordUtils;
import com.android.server.backup.utils.RestoreUtils;
import com.android.server.backup.utils.TarBackupReader;
import com.android.server.pm.PackageManagerService;
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
    private IBackupAgent mAgent;
    private String mAgentPackage;
    private final BackupAgentTimeoutParameters mAgentTimeoutParameters;
    private long mAppVersion;
    private final BackupManagerService mBackupManagerService;
    private long mBytes;
    private final HashSet<String> mClearedPackages = new HashSet();
    private final String mCurrentPassword;
    private final String mDecryptPassword;
    private final RestoreDeleteObserver mDeleteObserver = new RestoreDeleteObserver();
    private final ParcelFileDescriptor mInputFile;
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
        private final BackupManagerService mBackupManagerService;
        private final int mToken;

        RestoreFinishedRunnable(IBackupAgent agent, int token, BackupManagerService backupManagerService) {
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

    static /* synthetic */ long access$014(PerformAdbRestoreTask x0, long x1) {
        long j = x0.mBytes + x1;
        x0.mBytes = j;
        return j;
    }

    public PerformAdbRestoreTask(BackupManagerService backupManagerService, ParcelFileDescriptor fd, String curPassword, String decryptPassword, IFullBackupRestoreObserver observer, AtomicBoolean latch) {
        this.mBackupManagerService = backupManagerService;
        this.mInputFile = fd;
        this.mCurrentPassword = curPassword;
        this.mDecryptPassword = decryptPassword;
        this.mObserver = observer;
        this.mLatchObject = latch;
        this.mAgent = null;
        this.mPackageManagerBackupAgent = backupManagerService.makeMetadataAgent();
        this.mAgentPackage = null;
        this.mTargetApp = null;
        this.mObbConnection = new FullBackupObbConnection(backupManagerService);
        this.mAgentTimeoutParameters = (BackupAgentTimeoutParameters) Preconditions.checkNotNull(backupManagerService.getAgentTimeoutParameters(), "Timeout parameters cannot be null");
        this.mClearedPackages.add(PackageManagerService.PLATFORM_PACKAGE_NAME);
        this.mClearedPackages.add(BackupManagerService.SETTINGS_PACKAGE);
    }

    /* JADX WARNING: Removed duplicated region for block: B:97:0x01ae A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x0160 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        Slog.i(BackupManagerService.TAG, "--- Performing full-dataset restore ---");
        this.mObbConnection.establish();
        this.mObserver = FullBackupRestoreObserverUtils.sendStartRestore(this.mObserver);
        if (Environment.getExternalStorageState().equals("mounted")) {
            this.mPackagePolicies.put(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, RestorePolicy.ACCEPT);
        }
        FileInputStream rawInStream = null;
        try {
            if (this.mBackupManagerService.backupPasswordMatches(this.mCurrentPassword)) {
                this.mBytes = 0;
                rawInStream = new FileInputStream(this.mInputFile.getFileDescriptor());
                InputStream tarInputStream = parseBackupFileHeaderAndReturnTarStream(rawInStream, this.mDecryptPassword);
                if (tarInputStream == null) {
                    tearDownPipes();
                    tearDownAgent(this.mTargetApp, true);
                    try {
                        rawInStream.close();
                        this.mInputFile.close();
                    } catch (IOException e) {
                        Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e);
                    }
                    synchronized (this.mLatchObject) {
                        this.mLatchObject.set(true);
                        this.mLatchObject.notifyAll();
                    }
                    this.mObbConnection.tearDown();
                    this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                    Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                    this.mBackupManagerService.getWakelock().release();
                    return;
                }
                do {
                } while (restoreOneFile(tarInputStream, false, new byte[32768], null, true, this.mBackupManagerService.generateRandomIntegerToken(), null));
                tearDownPipes();
                tearDownAgent(this.mTargetApp, true);
                try {
                    rawInStream.close();
                    this.mInputFile.close();
                } catch (IOException e2) {
                    Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e2);
                }
                synchronized (this.mLatchObject) {
                    this.mLatchObject.set(true);
                    this.mLatchObject.notifyAll();
                }
                this.mObbConnection.tearDown();
                this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                this.mBackupManagerService.getWakelock().release();
                return;
            }
            Slog.w(BackupManagerService.TAG, "Backup password mismatch; aborting");
            tearDownPipes();
            tearDownAgent(this.mTargetApp, true);
            if (rawInStream != null) {
                try {
                    rawInStream.close();
                } catch (IOException e22) {
                    Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e22);
                }
            }
            this.mInputFile.close();
            synchronized (this.mLatchObject) {
                this.mLatchObject.set(true);
                this.mLatchObject.notifyAll();
            }
            this.mObbConnection.tearDown();
            this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
            Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
            this.mBackupManagerService.getWakelock().release();
        } catch (IOException e3) {
            try {
                Slog.e(BackupManagerService.TAG, "Unable to read restore input");
                tearDownPipes();
                tearDownAgent(this.mTargetApp, true);
                if (rawInStream != null) {
                    try {
                        rawInStream.close();
                    } catch (IOException e222) {
                        Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e222);
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                            this.mObbConnection.tearDown();
                            this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                            Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                            this.mBackupManagerService.getWakelock().release();
                            return;
                        }
                    }
                }
                this.mInputFile.close();
                synchronized (this.mLatchObject) {
                }
            } catch (Throwable th) {
                tearDownPipes();
                tearDownAgent(this.mTargetApp, true);
                if (rawInStream != null) {
                    try {
                        rawInStream.close();
                    } catch (IOException e4) {
                        Slog.w(BackupManagerService.TAG, "Close of restore data pipe threw", e4);
                        synchronized (this.mLatchObject) {
                            this.mLatchObject.set(true);
                            this.mLatchObject.notifyAll();
                            this.mObbConnection.tearDown();
                            this.mObserver = FullBackupRestoreObserverUtils.sendEndRestore(this.mObserver);
                            Slog.d(BackupManagerService.TAG, "Full restore pass complete.");
                            this.mBackupManagerService.getWakelock().release();
                        }
                    }
                }
                this.mInputFile.close();
                synchronized (this.mLatchObject) {
                }
            }
        }
    }

    private static void readFullyOrThrow(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int bytesRead = in.read(buffer, offset, buffer.length - offset);
            if (bytesRead > 0) {
                offset += bytesRead;
            } else {
                throw new IOException("Couldn't fully read data");
            }
        }
    }

    @VisibleForTesting
    public static InputStream parseBackupFileHeaderAndReturnTarStream(InputStream rawInputStream, String decryptPassword) throws IOException {
        boolean compressed = false;
        InputStream preCompressStream = rawInputStream;
        boolean okay = false;
        byte[] streamHeader = new byte[BackupManagerService.BACKUP_FILE_HEADER_MAGIC.length()];
        readFullyOrThrow(rawInputStream, streamHeader);
        if (Arrays.equals(BackupManagerService.BACKUP_FILE_HEADER_MAGIC.getBytes("UTF-8"), streamHeader)) {
            String s = readHeaderLine(rawInputStream);
            int archiveVersion = Integer.parseInt(s);
            if (archiveVersion <= 5) {
                boolean z = false;
                boolean pbkdf2Fallback = archiveVersion == 1;
                if (Integer.parseInt(readHeaderLine(rawInputStream)) != 0) {
                    z = true;
                }
                compressed = z;
                s = readHeaderLine(rawInputStream);
                if (s.equals("none")) {
                    okay = true;
                } else if (decryptPassword == null || decryptPassword.length() <= 0) {
                    Slog.w(BackupManagerService.TAG, "Archive is encrypted but no password given");
                } else {
                    preCompressStream = decodeAesHeaderAndInitialize(decryptPassword, s, pbkdf2Fallback, rawInputStream);
                    if (preCompressStream != null) {
                        okay = true;
                    }
                }
            } else {
                String str = BackupManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Wrong header version: ");
                stringBuilder.append(s);
                Slog.w(str, stringBuilder.toString());
            }
        } else {
            Slog.w(BackupManagerService.TAG, "Didn't read the right header magic");
        }
        if (okay) {
            return compressed ? new InflaterInputStream(preCompressStream) : preCompressStream;
        }
        Slog.w(BackupManagerService.TAG, "Invalid restore data; aborting.");
        return null;
    }

    private static String readHeaderLine(InputStream in) throws IOException {
        StringBuilder buffer = new StringBuilder(80);
        while (true) {
            int read = in.read();
            int c = read;
            if (read >= 0 && c != 10) {
                buffer.append((char) c);
            }
        }
        return buffer.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:86:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x012f  */
    /* JADX WARNING: Removed duplicated region for block: B:84:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x011d  */
    /* JADX WARNING: Removed duplicated region for block: B:82:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x010b  */
    /* JADX WARNING: Removed duplicated region for block: B:80:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00f9  */
    /* JADX WARNING: Removed duplicated region for block: B:78:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:73:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x00d4  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x012f  */
    /* JADX WARNING: Removed duplicated region for block: B:86:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x011d  */
    /* JADX WARNING: Removed duplicated region for block: B:84:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x010b  */
    /* JADX WARNING: Removed duplicated region for block: B:82:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00f9  */
    /* JADX WARNING: Removed duplicated region for block: B:80:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:78:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x00d4  */
    /* JADX WARNING: Removed duplicated region for block: B:73:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:86:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x012f  */
    /* JADX WARNING: Removed duplicated region for block: B:84:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x011d  */
    /* JADX WARNING: Removed duplicated region for block: B:82:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x010b  */
    /* JADX WARNING: Removed duplicated region for block: B:80:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00f9  */
    /* JADX WARNING: Removed duplicated region for block: B:78:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00e7  */
    /* JADX WARNING: Removed duplicated region for block: B:73:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x00d4  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static InputStream attemptMasterKeyDecryption(String decryptPassword, String algorithm, byte[] userSalt, byte[] ckSalt, int rounds, String userIvHex, String masterKeyBlobHex, InputStream rawInStream, boolean doLog) {
        InputStream result;
        InvalidAlgorithmParameterException e;
        InputStream inputStream;
        String str;
        byte[] bArr;
        String str2 = algorithm;
        int i = rounds;
        InputStream result2 = null;
        try {
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            try {
                SecretKey userKey = PasswordUtils.buildPasswordKey(str2, decryptPassword, userSalt, i);
                c.init(2, new SecretKeySpec(userKey.getEncoded(), "AES"), new IvParameterSpec(PasswordUtils.hexToByteArray(userIvHex)));
                byte[] mkBlob = c.doFinal(PasswordUtils.hexToByteArray(masterKeyBlobHex));
                int offset = 0 + 1;
                int len = mkBlob[0];
                byte[] IV = Arrays.copyOfRange(mkBlob, offset, offset + len);
                offset += len;
                int offset2 = offset + 1;
                len = mkBlob[offset];
                byte[] mk = Arrays.copyOfRange(mkBlob, offset2, offset2 + len);
                offset2 += len;
                int offset3 = offset2 + 1;
                byte[] mkChecksum = Arrays.copyOfRange(mkBlob, offset3, offset3 + mkBlob[offset2]);
                result = result2;
                try {
                    byte[] calculatedCk = PasswordUtils.makeKeyChecksum(str2, mk, ckSalt, i);
                    if (Arrays.equals(calculatedCk, mkChecksum)) {
                        try {
                            c.init(2, new SecretKeySpec(mk, "AES"), new IvParameterSpec(IV));
                            return new CipherInputStream(rawInStream, c);
                        } catch (InvalidAlgorithmParameterException e2) {
                            e = e2;
                            inputStream = rawInStream;
                            if (doLog) {
                                return result;
                            }
                            Slog.e(BackupManagerService.TAG, "Needed parameter spec unavailable!", e);
                            return result;
                        } catch (BadPaddingException e3) {
                            inputStream = rawInStream;
                            if (doLog) {
                                return result;
                            }
                            Slog.w(BackupManagerService.TAG, "Incorrect password");
                            return result;
                        } catch (IllegalBlockSizeException e4) {
                            inputStream = rawInStream;
                            if (doLog) {
                                return result;
                            }
                            Slog.w(BackupManagerService.TAG, "Invalid block size in master key");
                            return result;
                        } catch (NoSuchAlgorithmException e5) {
                            inputStream = rawInStream;
                            if (doLog) {
                                return result;
                            }
                            Slog.e(BackupManagerService.TAG, "Needed decryption algorithm unavailable!");
                            return result;
                        } catch (NoSuchPaddingException e6) {
                            inputStream = rawInStream;
                            if (doLog) {
                                return result;
                            }
                            Slog.e(BackupManagerService.TAG, "Needed padding mechanism unavailable!");
                            return result;
                        } catch (InvalidKeyException e7) {
                            inputStream = rawInStream;
                            if (doLog) {
                                return result;
                            }
                            Slog.w(BackupManagerService.TAG, "Illegal password; aborting");
                            return result;
                        }
                    }
                    byte[] bArr2 = calculatedCk;
                    if (!doLog) {
                        return result;
                    }
                    Slog.w(BackupManagerService.TAG, "Incorrect password");
                    return result;
                } catch (InvalidAlgorithmParameterException e8) {
                    e = e8;
                    if (doLog) {
                    }
                } catch (BadPaddingException e9) {
                    if (doLog) {
                    }
                } catch (IllegalBlockSizeException e10) {
                    if (doLog) {
                    }
                } catch (NoSuchAlgorithmException e11) {
                    if (doLog) {
                    }
                } catch (NoSuchPaddingException e12) {
                    if (doLog) {
                    }
                } catch (InvalidKeyException e13) {
                    if (doLog) {
                    }
                }
            } catch (InvalidAlgorithmParameterException e14) {
                e = e14;
                result = result2;
                result2 = ckSalt;
                if (doLog) {
                }
            } catch (BadPaddingException e15) {
                result = result2;
                result2 = ckSalt;
                if (doLog) {
                }
            } catch (IllegalBlockSizeException e16) {
                result = result2;
                result2 = ckSalt;
                if (doLog) {
                }
            } catch (NoSuchAlgorithmException e17) {
                result = result2;
                result2 = ckSalt;
                if (doLog) {
                }
            } catch (NoSuchPaddingException e18) {
                result = result2;
                result2 = ckSalt;
                if (doLog) {
                }
            } catch (InvalidKeyException e19) {
                result = result2;
                result2 = ckSalt;
                if (doLog) {
                }
            }
        } catch (InvalidAlgorithmParameterException e20) {
            e = e20;
            str = decryptPassword;
            bArr = userSalt;
            result = result2;
            result2 = ckSalt;
            if (doLog) {
            }
        } catch (BadPaddingException e21) {
            str = decryptPassword;
            bArr = userSalt;
            result = result2;
            result2 = ckSalt;
            if (doLog) {
            }
        } catch (IllegalBlockSizeException e22) {
            str = decryptPassword;
            bArr = userSalt;
            result = result2;
            result2 = ckSalt;
            if (doLog) {
            }
        } catch (NoSuchAlgorithmException e23) {
            str = decryptPassword;
            bArr = userSalt;
            result = result2;
            result2 = ckSalt;
            if (doLog) {
            }
        } catch (NoSuchPaddingException e24) {
            str = decryptPassword;
            bArr = userSalt;
            result = result2;
            result2 = ckSalt;
            if (doLog) {
            }
        } catch (InvalidKeyException e25) {
            str = decryptPassword;
            bArr = userSalt;
            result = result2;
            result2 = ckSalt;
            if (doLog) {
            }
        }
    }

    private static InputStream decodeAesHeaderAndInitialize(String decryptPassword, String encryptionName, boolean pbkdf2Fallback, InputStream rawInStream) {
        String str = encryptionName;
        InputStream result = null;
        try {
            if (str.equals(PasswordUtils.ENCRYPTION_ALGORITHM_NAME)) {
                byte[] userSalt = PasswordUtils.hexToByteArray(readHeaderLine(rawInStream));
                byte[] ckSalt = PasswordUtils.hexToByteArray(readHeaderLine(rawInStream));
                int rounds = Integer.parseInt(readHeaderLine(rawInStream));
                String userIvHex = readHeaderLine(rawInStream);
                String masterKeyBlobHex = readHeaderLine(rawInStream);
                result = attemptMasterKeyDecryption(decryptPassword, BackupPasswordManager.PBKDF_CURRENT, userSalt, ckSalt, rounds, userIvHex, masterKeyBlobHex, rawInStream, false);
                if (result != null || !pbkdf2Fallback) {
                    return result;
                }
                return attemptMasterKeyDecryption(decryptPassword, BackupPasswordManager.PBKDF_FALLBACK, userSalt, ckSalt, rounds, userIvHex, masterKeyBlobHex, rawInStream, true);
            }
            String str2 = BackupManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported encryption method: ");
            stringBuilder.append(str);
            Slog.w(str2, stringBuilder.toString());
            return result;
        } catch (NumberFormatException e) {
            Slog.w(BackupManagerService.TAG, "Can't parse restore data header");
            return result;
        } catch (IOException e2) {
            Slog.w(BackupManagerService.TAG, "Can't read input header");
            return result;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:55:0x017d A:{Catch:{ IOException -> 0x04fd }} */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x01f5 A:{Catch:{ IOException -> 0x021d, NameNotFoundException -> 0x021b }} */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x01bd A:{Catch:{ IOException -> 0x021d, NameNotFoundException -> 0x021b }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0211 A:{Catch:{ IOException -> 0x021d, NameNotFoundException -> 0x021b }} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x020e A:{Catch:{ IOException -> 0x021d, NameNotFoundException -> 0x021b }} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0223 A:{Catch:{ IOException -> 0x04fd }} */
    /* JADX WARNING: Removed duplicated region for block: B:167:0x04b4 A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x0274 A:{Catch:{ IOException -> 0x04fd }} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x04bb A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x046b A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x03e8 A:{Catch:{ IOException -> 0x04ae }} */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x0475 A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x04bb A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x03e8 A:{Catch:{ IOException -> 0x04ae }} */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x046b A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x0475 A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x04bb A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x046b A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x03e8 A:{Catch:{ IOException -> 0x04ae }} */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x0475 A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x04bb A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x03e8 A:{Catch:{ IOException -> 0x04ae }} */
    /* JADX WARNING: Removed duplicated region for block: B:161:0x046b A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x0475 A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x04bb A:{Catch:{ IOException -> 0x04f8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0526  */
    /* JADX WARNING: Removed duplicated region for block: B:200:0x0523  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean restoreOneFile(InputStream instream, boolean mustKillAgent, byte[] buffer, PackageInfo onlyPackage, boolean allowApks, int token, IBackupManagerMonitor monitor) {
        IOException e;
        InputStream inputStream;
        IBackupManagerMonitor iBackupManagerMonitor;
        FileMetadata info;
        boolean okay;
        boolean agentSuccess;
        boolean okay2;
        InputStream inputStream2 = instream;
        byte[] bArr = buffer;
        BytesReadListener bytesReadListener = new BytesReadListener() {
            public void onBytesRead(long bytesRead) {
                PerformAdbRestoreTask.access$014(PerformAdbRestoreTask.this, bytesRead);
            }
        };
        IBackupManagerMonitor iBackupManagerMonitor2 = monitor;
        TarBackupReader tarBackupReader = new TarBackupReader(inputStream2, bytesReadListener, iBackupManagerMonitor2);
        int i;
        byte[] bArr2;
        try {
            FileMetadata info2;
            FileMetadata info3 = tarBackupReader.readTarHeaders();
            if (info3 != null) {
                String pkg = info3.packageName;
                if (!pkg.equals(this.mAgentPackage)) {
                    try {
                        if (!this.mPackagePolicies.containsKey(pkg)) {
                            this.mPackagePolicies.put(pkg, RestorePolicy.IGNORE);
                        }
                        if (this.mAgent != null) {
                            Slog.d(BackupManagerService.TAG, "Saw new package; finalizing old one");
                            tearDownPipes();
                            tearDownAgent(this.mTargetApp, true);
                            this.mTargetApp = null;
                            this.mAgentPackage = null;
                        }
                    } catch (IOException e2) {
                        e = e2;
                        i = token;
                        inputStream = inputStream2;
                        bArr2 = bArr;
                        iBackupManagerMonitor = iBackupManagerMonitor2;
                        Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e);
                        info = null;
                        if (info == null) {
                        }
                        return info == null;
                    }
                }
                String pkg2;
                if (info3.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME)) {
                    Signature[] signatures = tarBackupReader.readAppManifestAndReturnSignatures(info3);
                    this.mAppVersion = info3.version;
                    pkg2 = pkg;
                    info2 = info3;
                    RestorePolicy restorePolicy = tarBackupReader.chooseRestorePolicy(this.mBackupManagerService.getPackageManager(), allowApks, info3, signatures, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
                    this.mManifestSignatures.put(info2.packageName, signatures);
                    this.mPackagePolicies.put(pkg2, restorePolicy);
                    this.mPackageInstallers.put(pkg2, info2.installerPackageName);
                    tarBackupReader.skipTarPadding(info2.size);
                    this.mObserver = FullBackupRestoreObserverUtils.sendOnRestorePackage(this.mObserver, pkg2);
                    i = token;
                    inputStream = inputStream2;
                    bArr2 = bArr;
                } else {
                    pkg2 = pkg;
                    info2 = info3;
                    if (info2.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME)) {
                        tarBackupReader.readMetadata(info2);
                        this.mWidgetData = tarBackupReader.getWidgetData();
                        IBackupManagerMonitor monitor2 = tarBackupReader.getMonitor();
                        try {
                            tarBackupReader.skipTarPadding(info2.size);
                            i = token;
                            iBackupManagerMonitor = monitor2;
                            inputStream = inputStream2;
                            bArr2 = bArr;
                            info = info2;
                        } catch (IOException e3) {
                            e = e3;
                            i = token;
                            inputStream = inputStream2;
                            bArr2 = bArr;
                            Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e);
                            info = null;
                            if (info == null) {
                            }
                            return info == null;
                        }
                        return info == null;
                    }
                    boolean isSuccessfullyInstalled;
                    String str;
                    StringBuilder stringBuilder;
                    BackupManagerService backupManagerService;
                    ApplicationInfo applicationInfo;
                    int i2;
                    boolean okay3;
                    boolean okay4 = true;
                    RestorePolicy policy = (RestorePolicy) this.mPackagePolicies.get(pkg2);
                    boolean z;
                    switch (policy) {
                        case IGNORE:
                            z = true;
                            okay4 = false;
                        case ACCEPT_IF_APK:
                            if (info2.domain.equals("a")) {
                                Object obj;
                                Slog.d(BackupManagerService.TAG, "APK file; installing");
                                isSuccessfullyInstalled = inputStream2;
                                Object obj2 = null;
                                RestorePolicy restorePolicy2 = policy;
                                z = true;
                                isSuccessfullyInstalled = RestoreUtils.installApk(isSuccessfullyInstalled, this.mBackupManagerService.getContext(), this.mDeleteObserver, this.mManifestSignatures, this.mPackagePolicies, info2, (String) this.mPackageInstallers.get(pkg2), bytesReadListener);
                                HashMap hashMap = this.mPackagePolicies;
                                if (isSuccessfullyInstalled) {
                                    obj = RestorePolicy.ACCEPT;
                                } else {
                                    obj = RestorePolicy.IGNORE;
                                }
                                hashMap.put(pkg2, obj);
                                tarBackupReader.skipTarPadding(info2.size);
                                return z;
                            }
                            z = true;
                            this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                            okay4 = false;
                        case ACCEPT:
                            if (info2.domain.equals("a")) {
                                Slog.d(BackupManagerService.TAG, "apk present but ACCEPT");
                                okay4 = false;
                            }
                            z = true;
                            if (!isCanonicalFilePath(info2.path)) {
                                okay4 = false;
                            }
                            isSuccessfullyInstalled = okay4;
                            if (isSuccessfullyInstalled && this.mAgent != null) {
                                Slog.i(BackupManagerService.TAG, "Reusing existing agent instance");
                            }
                            if (isSuccessfullyInstalled && this.mAgent == null) {
                                str = BackupManagerService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Need to launch agent for ");
                                stringBuilder.append(pkg2);
                                Slog.d(str, stringBuilder.toString());
                                this.mTargetApp = this.mBackupManagerService.getPackageManager().getApplicationInfo(pkg2, 0);
                                if (this.mClearedPackages.contains(pkg2)) {
                                    if (this.mTargetApp.backupAgentName == null) {
                                        Slog.d(BackupManagerService.TAG, "Clearing app data preparatory to full restore");
                                        this.mBackupManagerService.clearApplicationDataSynchronous(pkg2, z);
                                    } else {
                                        str = BackupManagerService.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("backup agent (");
                                        stringBuilder.append(this.mTargetApp.backupAgentName);
                                        stringBuilder.append(") => no clear");
                                        Slog.d(str, stringBuilder.toString());
                                    }
                                    this.mClearedPackages.add(pkg2);
                                } else {
                                    Slog.d(BackupManagerService.TAG, "We've initialized this app already; no clear required");
                                }
                                setUpPipes();
                                backupManagerService = this.mBackupManagerService;
                                applicationInfo = this.mTargetApp;
                                if ("k".equals(info2.domain)) {
                                    i2 = 3;
                                } else {
                                    i2 = 0;
                                }
                                this.mAgent = backupManagerService.bindToAgentSynchronous(applicationInfo, i2);
                                this.mAgentPackage = pkg2;
                                if (this.mAgent == null) {
                                    str = BackupManagerService.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unable to create agent for ");
                                    stringBuilder.append(pkg2);
                                    Slog.e(str, stringBuilder.toString());
                                    isSuccessfullyInstalled = false;
                                    tearDownPipes();
                                    this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                                }
                            }
                            if (isSuccessfullyInstalled && !pkg2.equals(this.mAgentPackage)) {
                                str = BackupManagerService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Restoring data for ");
                                stringBuilder.append(pkg2);
                                stringBuilder.append(" but agent is for ");
                                stringBuilder.append(this.mAgentPackage);
                                Slog.e(str, stringBuilder.toString());
                                isSuccessfullyInstalled = false;
                            }
                            okay3 = isSuccessfullyInstalled;
                            if (okay3) {
                                i = token;
                                okay = okay3;
                                bArr2 = bArr;
                            } else {
                                StringBuilder stringBuilder2;
                                boolean agentSuccess2 = true;
                                long toCopy = info2.size;
                                try {
                                    this.mBackupManagerService.prepareOperationTimeout(token, this.mAgentTimeoutParameters.getRestoreAgentTimeoutMillis(), null, 1);
                                    if ("obb".equals(info2.domain)) {
                                        str = BackupManagerService.TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Restoring OBB file for ");
                                        stringBuilder2.append(pkg2);
                                        stringBuilder2.append(" : ");
                                        stringBuilder2.append(info2.path);
                                        Slog.d(str, stringBuilder2.toString());
                                        okay = okay3;
                                        try {
                                            this.mObbConnection.restoreObbFile(pkg2, this.mPipes[0], info2.size, info2.type, info2.path, info2.mode, info2.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                                        } catch (IOException e4) {
                                            try {
                                                Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                                agentSuccess2 = false;
                                                okay4 = false;
                                                okay3 = okay4;
                                                if (okay3) {
                                                }
                                                if (!agentSuccess) {
                                                }
                                                okay = okay2;
                                                if (!okay) {
                                                }
                                                inputStream = instream;
                                                iBackupManagerMonitor = monitor;
                                                info = info2;
                                            } catch (IOException e5) {
                                                e = e5;
                                                i = token;
                                                bArr2 = buffer;
                                                inputStream = instream;
                                                Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e);
                                                info = null;
                                                if (info == null) {
                                                }
                                                return info == null;
                                            }
                                            if (info == null) {
                                            }
                                            return info == null;
                                        } catch (RemoteException e6) {
                                            try {
                                                Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                                agentSuccess2 = false;
                                                okay4 = false;
                                                okay3 = okay4;
                                                if (okay3) {
                                                }
                                                if (agentSuccess) {
                                                }
                                                okay = okay2;
                                                if (okay) {
                                                }
                                                inputStream = instream;
                                                iBackupManagerMonitor = monitor;
                                                info = info2;
                                            } catch (IOException e7) {
                                                e = e7;
                                                i = token;
                                                inputStream = instream;
                                                bArr2 = buffer;
                                                Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e);
                                                info = null;
                                                if (info == null) {
                                                }
                                                return info == null;
                                            }
                                            if (info == null) {
                                            }
                                            return info == null;
                                        }
                                    }
                                    okay = okay3;
                                    if ("k".equals(info2.domain)) {
                                        str = BackupManagerService.TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Restoring key-value file for ");
                                        stringBuilder2.append(pkg2);
                                        stringBuilder2.append(" : ");
                                        stringBuilder2.append(info2.path);
                                        Slog.d(str, stringBuilder2.toString());
                                        info2.version = this.mAppVersion;
                                        new Thread(new KeyValueAdbRestoreEngine(this.mBackupManagerService, this.mBackupManagerService.getDataDir(), info2, this.mPipes[0], this.mAgent, token), "restore-key-value-runner").start();
                                    } else {
                                        str = BackupManagerService.TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Invoking agent to restore file ");
                                        stringBuilder2.append(info2.path);
                                        Slog.d(str, stringBuilder2.toString());
                                        if (this.mTargetApp.processName.equals("system")) {
                                            Slog.d(BackupManagerService.TAG, "system process agent - spinning a thread");
                                            new Thread(new RestoreFileRunnable(this.mBackupManagerService, this.mAgent, info2, this.mPipes[0], token), "restore-sys-runner").start();
                                        } else {
                                            this.mAgent.doRestoreFile(this.mPipes[0], info2.size, info2.type, info2.domain, info2.path, info2.mode, info2.mtime, token, this.mBackupManagerService.getBackupManagerBinder());
                                        }
                                    }
                                    okay3 = okay;
                                } catch (IOException e8) {
                                    okay = okay3;
                                    Slog.d(BackupManagerService.TAG, "Couldn't establish restore");
                                    agentSuccess2 = false;
                                    okay4 = false;
                                    okay3 = okay4;
                                    if (okay3) {
                                    }
                                    if (agentSuccess) {
                                    }
                                    okay = okay2;
                                    if (okay) {
                                    }
                                    inputStream = instream;
                                    iBackupManagerMonitor = monitor;
                                    info = info2;
                                    if (info == null) {
                                    }
                                    return info == null;
                                } catch (RemoteException e9) {
                                    okay = okay3;
                                    Slog.e(BackupManagerService.TAG, "Agent crashed during full restore");
                                    agentSuccess2 = false;
                                    okay4 = false;
                                    okay3 = okay4;
                                    if (okay3) {
                                    }
                                    if (agentSuccess) {
                                    }
                                    okay = okay2;
                                    if (okay) {
                                    }
                                    inputStream = instream;
                                    iBackupManagerMonitor = monitor;
                                    info = info2;
                                    if (info == null) {
                                    }
                                    return info == null;
                                }
                                if (okay3) {
                                    FileOutputStream pipe = new FileOutputStream(this.mPipes[1].getFileDescriptor());
                                    boolean pipeOkay = true;
                                    long toCopy2 = toCopy;
                                    while (toCopy2 > 0) {
                                        bArr2 = buffer;
                                        try {
                                            int toRead = toCopy2 > ((long) bArr2.length) ? bArr2.length : (int) toCopy2;
                                            InputStream inputStream3 = instream;
                                            try {
                                                i = inputStream3.read(bArr2, 0, toRead);
                                                if (i >= 0) {
                                                    okay2 = okay3;
                                                    agentSuccess = agentSuccess2;
                                                    this.mBytes += (long) i;
                                                } else {
                                                    okay2 = okay3;
                                                    agentSuccess = agentSuccess2;
                                                    int i3 = toRead;
                                                }
                                                if (i <= 0) {
                                                    tarBackupReader.skipTarPadding(info2.size);
                                                    toCopy = toCopy2;
                                                    agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                                                } else {
                                                    toCopy2 -= (long) i;
                                                    if (pipeOkay) {
                                                        try {
                                                            pipe.write(bArr2, 0, i);
                                                        } catch (IOException e10) {
                                                            IOException iOException = e10;
                                                            Slog.e(BackupManagerService.TAG, "Failed to write to restore pipe", e10);
                                                            pipeOkay = false;
                                                        }
                                                    }
                                                    okay3 = okay2;
                                                    agentSuccess2 = agentSuccess;
                                                }
                                            } catch (IOException e11) {
                                                e10 = e11;
                                                i = token;
                                                inputStream = inputStream3;
                                                Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e10);
                                                info = null;
                                                if (info == null) {
                                                }
                                                return info == null;
                                            }
                                        } catch (IOException e12) {
                                            e10 = e12;
                                            i = token;
                                        }
                                    }
                                    okay2 = okay3;
                                    agentSuccess = agentSuccess2;
                                    bArr2 = buffer;
                                    tarBackupReader.skipTarPadding(info2.size);
                                    try {
                                        toCopy = toCopy2;
                                        agentSuccess = this.mBackupManagerService.waitUntilOperationComplete(token);
                                    } catch (IOException e13) {
                                        e10 = e13;
                                        inputStream = instream;
                                        Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e10);
                                        info = null;
                                        if (info == null) {
                                        }
                                        return info == null;
                                    }
                                }
                                i = token;
                                okay2 = okay3;
                                agentSuccess = agentSuccess2;
                                bArr2 = buffer;
                                if (agentSuccess) {
                                    str = BackupManagerService.TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Agent failure restoring ");
                                    stringBuilder2.append(pkg2);
                                    stringBuilder2.append("; now ignoring");
                                    Slog.d(str, stringBuilder2.toString());
                                    this.mBackupManagerService.getBackupHandler().removeMessages(18);
                                    tearDownPipes();
                                    tearDownAgent(this.mTargetApp, false);
                                    this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                                }
                                okay = okay2;
                            }
                            if (okay) {
                                Slog.d(BackupManagerService.TAG, "[discarding file content]");
                                long bytesToConsume = (info2.size + 511) & -512;
                                while (bytesToConsume > 0) {
                                    try {
                                        long nRead;
                                        long nRead2 = (long) instream.read(bArr2, 0, bytesToConsume > ((long) bArr2.length) ? bArr2.length : (int) bytesToConsume);
                                        if (nRead2 >= 0) {
                                            nRead = nRead2;
                                            this.mBytes += nRead;
                                        } else {
                                            nRead = nRead2;
                                        }
                                        if (nRead > 0) {
                                            bytesToConsume -= nRead;
                                        }
                                    } catch (IOException e14) {
                                        e10 = e14;
                                        Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e10);
                                        info = null;
                                        if (info == null) {
                                        }
                                        return info == null;
                                    }
                                }
                            }
                            inputStream = instream;
                            break;
                        default:
                            z = true;
                            try {
                                Slog.e(BackupManagerService.TAG, "Invalid policy from manifest");
                                okay4 = false;
                                this.mPackagePolicies.put(pkg2, RestorePolicy.IGNORE);
                            } catch (IOException e15) {
                                e10 = e15;
                                i = token;
                                bArr2 = bArr;
                                break;
                            }
                    }
                    if (isCanonicalFilePath(info2.path)) {
                    }
                    isSuccessfullyInstalled = okay4;
                    Slog.i(BackupManagerService.TAG, "Reusing existing agent instance");
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Need to launch agent for ");
                    stringBuilder.append(pkg2);
                    Slog.d(str, stringBuilder.toString());
                    try {
                        this.mTargetApp = this.mBackupManagerService.getPackageManager().getApplicationInfo(pkg2, 0);
                        if (this.mClearedPackages.contains(pkg2)) {
                        }
                        setUpPipes();
                        backupManagerService = this.mBackupManagerService;
                        applicationInfo = this.mTargetApp;
                        if ("k".equals(info2.domain)) {
                        }
                        this.mAgent = backupManagerService.bindToAgentSynchronous(applicationInfo, i2);
                        this.mAgentPackage = pkg2;
                    } catch (IOException e16) {
                    } catch (NameNotFoundException e17) {
                    }
                    if (this.mAgent == null) {
                    }
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Restoring data for ");
                    stringBuilder.append(pkg2);
                    stringBuilder.append(" but agent is for ");
                    stringBuilder.append(this.mAgentPackage);
                    Slog.e(str, stringBuilder.toString());
                    isSuccessfullyInstalled = false;
                    okay3 = isSuccessfullyInstalled;
                    if (okay3) {
                    }
                    if (okay) {
                    }
                    inputStream = instream;
                }
            } else {
                i = token;
                info2 = info3;
                inputStream = inputStream2;
                bArr2 = bArr;
            }
            iBackupManagerMonitor = monitor;
            info = info2;
        } catch (IOException e18) {
            e10 = e18;
            i = token;
            inputStream = inputStream2;
            bArr2 = bArr;
            Slog.w(BackupManagerService.TAG, "io exception on restore socket read", e10);
            info = null;
            if (info == null) {
            }
            return info == null;
        }
        if (info == null) {
        }
        return info == null;
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
                Slog.w(BackupManagerService.TAG, "Couldn't close agent pipes", e);
            }
            this.mPipes = null;
        }
    }

    private void tearDownAgent(ApplicationInfo app, boolean doRestoreFinished) {
        if (this.mAgent != null) {
            if (doRestoreFinished) {
                try {
                    int token = this.mBackupManagerService.generateRandomIntegerToken();
                    long fullBackupAgentTimeoutMillis = this.mAgentTimeoutParameters.getFullBackupAgentTimeoutMillis();
                    AdbRestoreFinishedLatch latch = new AdbRestoreFinishedLatch(this.mBackupManagerService, token);
                    this.mBackupManagerService.prepareOperationTimeout(token, fullBackupAgentTimeoutMillis, latch, 1);
                    if (this.mTargetApp.processName.equals("system")) {
                        new Thread(new RestoreFinishedRunnable(this.mAgent, token, this.mBackupManagerService), "restore-sys-finished-runner").start();
                    } else {
                        this.mAgent.doRestoreFinished(token, this.mBackupManagerService.getBackupManagerBinder());
                    }
                    latch.await();
                } catch (RemoteException e) {
                    Slog.d(BackupManagerService.TAG, "Lost app trying to shut down");
                }
            }
            this.mBackupManagerService.tearDownAgentAndKill(app);
            this.mAgent = null;
        }
    }
}
