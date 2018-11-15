package com.android.server.backup.fullbackup;

import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.AppWidgetBackupBridge;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.BackupPasswordManager;
import com.android.server.backup.BackupRestoreTask;
import com.android.server.backup.utils.AppBackupUtils;
import com.android.server.backup.utils.PasswordUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class PerformAdbBackupTask extends FullBackupTask implements BackupRestoreTask {
    private BackupManagerService backupManagerService;
    boolean mAllApps;
    FullBackupEngine mBackupEngine;
    boolean mCompress;
    private final int mCurrentOpToken;
    String mCurrentPassword;
    PackageInfo mCurrentTarget;
    DeflaterOutputStream mDeflater;
    boolean mDoWidgets;
    String mEncryptPassword;
    boolean mIncludeApks;
    boolean mIncludeObbs;
    boolean mIncludeShared;
    boolean mIncludeSystem;
    boolean mKeyValue;
    final AtomicBoolean mLatch;
    ParcelFileDescriptor mOutputFile;
    ArrayList<String> mPackages;

    public PerformAdbBackupTask(BackupManagerService backupManagerService, ParcelFileDescriptor fd, IFullBackupRestoreObserver observer, boolean includeApks, boolean includeObbs, boolean includeShared, boolean doWidgets, String curPassword, String encryptPassword, boolean doAllApps, boolean doSystem, boolean doCompress, boolean doKeyValue, String[] packages, AtomicBoolean latch) {
        ArrayList arrayList;
        String str = curPassword;
        String str2 = encryptPassword;
        super(observer);
        this.backupManagerService = backupManagerService;
        this.mCurrentOpToken = backupManagerService.generateRandomIntegerToken();
        this.mLatch = latch;
        this.mOutputFile = fd;
        this.mIncludeApks = includeApks;
        this.mIncludeObbs = includeObbs;
        this.mIncludeShared = includeShared;
        this.mDoWidgets = doWidgets;
        this.mAllApps = doAllApps;
        this.mIncludeSystem = doSystem;
        if (packages == null) {
            arrayList = new ArrayList();
        } else {
            arrayList = new ArrayList(Arrays.asList(packages));
        }
        this.mPackages = arrayList;
        this.mCurrentPassword = str;
        if (str2 == null || BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str2)) {
            this.mEncryptPassword = str;
        } else {
            this.mEncryptPassword = str2;
        }
        this.mCompress = doCompress;
        this.mKeyValue = doKeyValue;
    }

    void addPackagesToSet(TreeMap<String, PackageInfo> set, List<String> pkgNames) {
        for (String pkgName : pkgNames) {
            if (!set.containsKey(pkgName)) {
                try {
                    set.put(pkgName, this.backupManagerService.getPackageManager().getPackageInfo(pkgName, 134217728));
                } catch (NameNotFoundException e) {
                    String str = BackupManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown package ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(", skipping");
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }
    }

    private OutputStream emitAesBackupHeader(StringBuilder headerbuf, OutputStream ofstream) throws Exception {
        StringBuilder stringBuilder = headerbuf;
        byte[] newUserSalt = this.backupManagerService.randomBytes(512);
        SecretKey userKey = PasswordUtils.buildPasswordKey(BackupPasswordManager.PBKDF_CURRENT, this.mEncryptPassword, newUserSalt, 10000);
        byte[] masterPw = new byte[32];
        this.backupManagerService.getRng().nextBytes(masterPw);
        byte[] checksumSalt = this.backupManagerService.randomBytes(512);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec masterKeySpec = new SecretKeySpec(masterPw, "AES");
        c.init(1, masterKeySpec);
        OutputStream finalOutput = new CipherOutputStream(ofstream, c);
        stringBuilder.append(PasswordUtils.ENCRYPTION_ALGORITHM_NAME);
        stringBuilder.append(10);
        stringBuilder.append(PasswordUtils.byteArrayToHex(newUserSalt));
        stringBuilder.append(10);
        stringBuilder.append(PasswordUtils.byteArrayToHex(checksumSalt));
        stringBuilder.append(10);
        stringBuilder.append(10000);
        stringBuilder.append(10);
        Cipher mkC = Cipher.getInstance("AES/CBC/PKCS5Padding");
        mkC.init(1, userKey);
        stringBuilder.append(PasswordUtils.byteArrayToHex(mkC.getIV()));
        stringBuilder.append(10);
        byte[] IV = c.getIV();
        byte[] mk = masterKeySpec.getEncoded();
        byte[] checksum = PasswordUtils.makeKeyChecksum(BackupPasswordManager.PBKDF_CURRENT, masterKeySpec.getEncoded(), checksumSalt, 10000);
        ByteArrayOutputStream blob = new ByteArrayOutputStream(((IV.length + mk.length) + checksum.length) + 3);
        DataOutputStream mkOut = new DataOutputStream(blob);
        mkOut.writeByte(IV.length);
        mkOut.write(IV);
        mkOut.writeByte(mk.length);
        mkOut.write(mk);
        mkOut.writeByte(checksum.length);
        mkOut.write(checksum);
        mkOut.flush();
        stringBuilder.append(PasswordUtils.byteArrayToHex(mkC.doFinal(blob.toByteArray())));
        stringBuilder.append(10);
        return finalOutput;
    }

    private void finalizeBackup(OutputStream out) {
        try {
            out.write(new byte[1024]);
        } catch (IOException e) {
            Slog.w(BackupManagerService.TAG, "Error attempting to finalize backup stream");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:97:0x0222 A:{SYNTHETIC, Splitter: B:97:0x0222} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x01c6 A:{SYNTHETIC, Splitter: B:78:0x01c6} */
    /* JADX WARNING: Removed duplicated region for block: B:421:0x0576 A:{SYNTHETIC, EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  , EDGE_INSN: B:421:0x0576->B:250:0x0576 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:168:0x0368 A:{SYNTHETIC, Splitter: B:168:0x0368} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x06bf A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x0692 A:{SYNTHETIC, Splitter: B:302:0x0692} */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x06bf A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x0692 A:{SYNTHETIC, Splitter: B:302:0x0692} */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x06bf A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:264:0x05e8 A:{Splitter: B:255:0x0589, ExcHandler: android.os.RemoteException (e android.os.RemoteException)} */
    /* JADX WARNING: Removed duplicated region for block: B:263:0x05e5 A:{Splitter: B:255:0x0589, ExcHandler: java.lang.Exception (e java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x0256 A:{Splitter: B:100:0x0237, ExcHandler: android.os.RemoteException (e android.os.RemoteException)} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x023a A:{Splitter: B:100:0x0237, ExcHandler: all (th java.lang.Throwable)} */
    /* JADX WARNING: Removed duplicated region for block: B:302:0x0692 A:{SYNTHETIC, Splitter: B:302:0x0692} */
    /* JADX WARNING: Removed duplicated region for block: B:309:0x06bf A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:324:0x0701 A:{Splitter: B:97:0x0222, ExcHandler: android.os.RemoteException (e android.os.RemoteException)} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:320:0x06e9 A:{Splitter: B:97:0x0222, ExcHandler: all (th java.lang.Throwable)} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:352:0x0783 A:{SYNTHETIC, Splitter: B:352:0x0783} */
    /* JADX WARNING: Removed duplicated region for block: B:359:0x07b0 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:334:0x0733 A:{SYNTHETIC, Splitter: B:334:0x0733} */
    /* JADX WARNING: Removed duplicated region for block: B:341:0x0760 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:373:0x07dd A:{SYNTHETIC, Splitter: B:373:0x07dd} */
    /* JADX WARNING: Removed duplicated region for block: B:380:0x080a A:{SYNTHETIC} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:102:0x023a, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:103:0x023b, code:
            r29 = r3;
            r31 = r6;
            r32 = r7;
            r1 = r8;
            r19 = r9;
            r28 = r11;
     */
    /* JADX WARNING: Missing block: B:104:0x0246, code:
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:109:0x0257, code:
            r31 = r6;
            r32 = r7;
            r1 = r8;
            r19 = r9;
            r28 = r11;
     */
    /* JADX WARNING: Missing block: B:118:0x0270, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:119:0x0271, code:
            r31 = r6;
            r32 = r7;
            r1 = r8;
            r19 = r9;
            r27 = r10;
            r28 = r11;
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:250:0x0576, code:
            r2 = r5;
            r31 = r6;
            r32 = r7;
            r27 = r10;
            r28 = r11;
            r33 = r19;
            r19 = r9;
     */
    /* JADX WARNING: Missing block: B:253:0x0585, code:
            if (r12.mKeyValue == false) goto L_0x05eb;
     */
    /* JADX WARNING: Missing block: B:256:?, code:
            r4 = r33.iterator();
     */
    /* JADX WARNING: Missing block: B:258:0x0591, code:
            if (r4.hasNext() == false) goto L_0x05ed;
     */
    /* JADX WARNING: Missing block: B:259:0x0593, code:
            r5 = (android.content.pm.PackageInfo) r4.next();
            r6 = com.android.server.backup.BackupManagerService.TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("--- Performing key-value backup for package ");
            r7.append(r5.packageName);
            r7.append(" ---");
            android.util.Slog.i(r6, r7.toString());
            r20 = new com.android.server.backup.KeyValueAdbBackupEngine(r2, r5, r12.backupManagerService, r12.backupManagerService.getPackageManager(), r12.backupManagerService.getBaseStateDir(), r12.backupManagerService.getDataDir());
            sendOnBackupPackage(r5.packageName);
            r20.backupOnePackage();
     */
    /* JADX WARNING: Missing block: B:261:0x05e0, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:262:0x05e1, code:
            r29 = r3;
     */
    /* JADX WARNING: Missing block: B:263:0x05e5, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:265:0x05eb, code:
            r1 = r33;
     */
    /* JADX WARNING: Missing block: B:267:?, code:
            finalizeBackup(r2);
     */
    /* JADX WARNING: Missing block: B:268:0x05f0, code:
            if (r2 == null) goto L_0x05fb;
     */
    /* JADX WARNING: Missing block: B:270:?, code:
            r2.flush();
            r2.close();
     */
    /* JADX WARNING: Missing block: B:271:0x05f9, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:272:0x05fb, code:
            r12.mOutputFile.close();
     */
    /* JADX WARNING: Missing block: B:273:0x0601, code:
            r4 = com.android.server.backup.BackupManagerService.TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("IO error closing adb backup file: ");
            r5.append(r0.getMessage());
            android.util.Slog.e(r4, r5.toString());
     */
    /* JADX WARNING: Missing block: B:275:0x061e, code:
            monitor-enter(r12.mLatch);
     */
    /* JADX WARNING: Missing block: B:277:?, code:
            r12.mLatch.set(true);
            r12.mLatch.notifyAll();
     */
    /* JADX WARNING: Missing block: B:282:0x0630, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:283:0x0631, code:
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:284:0x0634, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:285:0x0635, code:
            r1 = r33;
            r5 = true;
            r29 = r3;
            r3 = r2;
            r2 = r0;
     */
    /* JADX WARNING: Missing block: B:286:0x063e, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:287:0x063f, code:
            r1 = r33;
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:289:0x0645, code:
            r1 = r33;
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:296:0x067b, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:297:0x067c, code:
            r31 = r6;
            r32 = r7;
            r1 = r8;
            r19 = r9;
            r27 = r10;
            r28 = r11;
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:303:?, code:
            r2.flush();
            r2.close();
     */
    /* JADX WARNING: Missing block: B:304:0x0699, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:306:0x06a1, code:
            r7 = com.android.server.backup.BackupManagerService.TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("IO error closing adb backup file: ");
            r8.append(r0.getMessage());
            android.util.Slog.e(r7, r8.toString());
     */
    /* JADX WARNING: Missing block: B:310:?, code:
            r12.mLatch.set(r5);
            r12.mLatch.notifyAll();
     */
    /* JADX WARNING: Missing block: B:320:0x06e9, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:321:0x06ea, code:
            r31 = r6;
            r32 = r7;
            r1 = r8;
            r19 = r9;
            r28 = r11;
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:325:0x0702, code:
            r31 = r6;
            r32 = r7;
            r1 = r8;
            r19 = r9;
            r28 = r11;
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:335:?, code:
            r2.flush();
            r2.close();
     */
    /* JADX WARNING: Missing block: B:336:0x073a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:338:0x0742, code:
            r4 = com.android.server.backup.BackupManagerService.TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("IO error closing adb backup file: ");
            r6.append(r0.getMessage());
            android.util.Slog.e(r4, r6.toString());
     */
    /* JADX WARNING: Missing block: B:342:?, code:
            r12.mLatch.set(r5);
            r12.mLatch.notifyAll();
     */
    /* JADX WARNING: Missing block: B:353:?, code:
            r2.flush();
            r2.close();
     */
    /* JADX WARNING: Missing block: B:354:0x078a, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:356:0x0792, code:
            r4 = com.android.server.backup.BackupManagerService.TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("IO error closing adb backup file: ");
            r6.append(r0.getMessage());
            android.util.Slog.e(r4, r6.toString());
     */
    /* JADX WARNING: Missing block: B:360:?, code:
            r12.mLatch.set(r5);
            r12.mLatch.notifyAll();
     */
    /* JADX WARNING: Missing block: B:374:?, code:
            r3.flush();
            r3.close();
     */
    /* JADX WARNING: Missing block: B:375:0x07e4, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:377:0x07ec, code:
            r4 = new java.lang.StringBuilder();
            r4.append("IO error closing adb backup file: ");
            r4.append(r0.getMessage());
            android.util.Slog.e(com.android.server.backup.BackupManagerService.TAG, r4.toString());
     */
    /* JADX WARNING: Missing block: B:381:?, code:
            r12.mLatch.set(r5);
            r12.mLatch.notifyAll();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void run() {
        int i;
        Iterator<Entry<String, PackageInfo>> iter;
        String str;
        String str2;
        StringBuilder stringBuilder;
        Exception e;
        OutputStream outputStream;
        ArrayList<PackageInfo> arrayList;
        ArrayList<PackageInfo> arrayList2;
        Iterator<Entry<String, PackageInfo>> it;
        PackageManager packageManager;
        boolean z;
        Throwable N;
        OutputStream pkg;
        Throwable th;
        boolean keyValueBackupQueue;
        StringBuilder stringBuilder2;
        Iterator<Entry<String, PackageInfo>> it2;
        OutputStream out;
        FileOutputStream fileOutputStream;
        int N2;
        int i2;
        int i3;
        ArrayList<PackageInfo> iter2;
        ArrayList<PackageInfo> keyValueBackupQueue2;
        Object out2;
        PackageInfo pkg2;
        String includeKeyValue = this.mKeyValue ? ", including key-value backups" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String str3 = BackupManagerService.TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("--- Performing adb backup");
        stringBuilder3.append(includeKeyValue);
        stringBuilder3.append(" ---");
        Slog.i(str3, stringBuilder3.toString());
        TreeMap<String, PackageInfo> packagesToBackup = new TreeMap();
        FullBackupObbConnection obbConnection = new FullBackupObbConnection(this.backupManagerService);
        obbConnection.establish();
        sendStartBackup();
        PackageManager pm = this.backupManagerService.getPackageManager();
        if (this.mAllApps) {
            List<PackageInfo> allPackages = pm.getInstalledPackages(134217728);
            for (i = 0; i < allPackages.size(); i++) {
                PackageInfo pkg3 = (PackageInfo) allPackages.get(i);
                if (this.mIncludeSystem || (pkg3.applicationInfo.flags & 1) == 0) {
                    packagesToBackup.put(pkg3.packageName, pkg3);
                }
            }
        }
        if (this.mDoWidgets) {
            List<String> pkgs = AppWidgetBackupBridge.getWidgetParticipants(0);
            if (pkgs != null) {
                addPackagesToSet(packagesToBackup, pkgs);
            }
        }
        if (this.mPackages != null) {
            addPackagesToSet(packagesToBackup, this.mPackages);
        }
        ArrayList<PackageInfo> keyValueBackupQueue3 = new ArrayList();
        Iterator<Entry<String, PackageInfo>> iter3 = packagesToBackup.entrySet().iterator();
        while (true) {
            iter = iter3;
            if (!iter.hasNext()) {
                break;
            }
            PackageInfo pkg4 = (PackageInfo) ((Entry) iter.next()).getValue();
            StringBuilder stringBuilder4;
            if (!AppBackupUtils.appIsEligibleForBackup(pkg4.applicationInfo, pm) || AppBackupUtils.appIsStopped(pkg4.applicationInfo)) {
                iter.remove();
                str = BackupManagerService.TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Package ");
                stringBuilder4.append(pkg4.packageName);
                stringBuilder4.append(" is not eligible for backup, removing.");
                Slog.i(str, stringBuilder4.toString());
            } else if (AppBackupUtils.appIsKeyValueOnly(pkg4)) {
                iter.remove();
                str = BackupManagerService.TAG;
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Package ");
                stringBuilder4.append(pkg4.packageName);
                stringBuilder4.append(" is key-value.");
                Slog.i(str, stringBuilder4.toString());
                keyValueBackupQueue3.add(pkg4);
            }
            iter3 = iter;
        }
        ArrayList<PackageInfo> backupQueue = new ArrayList(packagesToBackup.values());
        FileOutputStream ofstream = new FileOutputStream(this.mOutputFile.getFileDescriptor());
        OutputStream out3 = null;
        OutputStream out4 = null;
        try {
            boolean z2;
            boolean encrypting;
            OutputStream finalOutput;
            if (this.mEncryptPassword != null) {
                PackageInfo pkg5;
                try {
                    if (this.mEncryptPassword.length() > 0) {
                        z2 = true;
                        encrypting = z2;
                        if (this.backupManagerService.deviceIsEncrypted() || encrypting) {
                            finalOutput = ofstream;
                            if (this.backupManagerService.backupPasswordMatches(this.mCurrentPassword)) {
                                Slog.w(BackupManagerService.TAG, "Backup password mismatch; aborting");
                                if (out3 != null) {
                                    try {
                                        out3.flush();
                                        out3.close();
                                    } catch (IOException e2) {
                                        str2 = BackupManagerService.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("IO error closing adb backup file: ");
                                        stringBuilder.append(e2.getMessage());
                                        Slog.e(str2, stringBuilder.toString());
                                    }
                                }
                                this.mOutputFile.close();
                                synchronized (this.mLatch) {
                                    this.mLatch.set(true);
                                    this.mLatch.notifyAll();
                                }
                                sendEndBackup();
                                obbConnection.tearDown();
                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                this.backupManagerService.getWakelock().release();
                                return;
                            }
                            StringBuilder headerbuf;
                            try {
                                OutputStream finalOutput2;
                                headerbuf = new StringBuilder(1024);
                                headerbuf.append(BackupManagerService.BACKUP_FILE_HEADER_MAGIC);
                                headerbuf.append(5);
                                if (this.mCompress) {
                                    try {
                                        str3 = "\n1\n";
                                    } catch (RemoteException e3) {
                                    } catch (Exception e4) {
                                        e = e4;
                                        outputStream = ofstream;
                                        arrayList = backupQueue;
                                        arrayList2 = keyValueBackupQueue3;
                                        it = iter;
                                        packageManager = pm;
                                        z = true;
                                        try {
                                            Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                            if (out3 != null) {
                                            }
                                            this.mOutputFile.close();
                                            synchronized (this.mLatch) {
                                            }
                                            sendEndBackup();
                                            obbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                        } catch (Throwable th2) {
                                            N = th2;
                                            pkg = out4;
                                            out4 = out3;
                                            th = N;
                                            if (out4 != null) {
                                            }
                                            this.mOutputFile.close();
                                            synchronized (this.mLatch) {
                                            }
                                            sendEndBackup();
                                            obbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                            throw th;
                                        }
                                    } catch (Throwable th3) {
                                    }
                                } else {
                                    str3 = "\n0\n";
                                }
                                headerbuf.append(str3);
                                if (encrypting) {
                                    finalOutput = emitAesBackupHeader(headerbuf, finalOutput);
                                } else {
                                    headerbuf.append("none\n");
                                }
                                ofstream.write(headerbuf.toString().getBytes("UTF-8"));
                                if (this.mCompress) {
                                    try {
                                        it = keyValueBackupQueue3;
                                        keyValueBackupQueue = true;
                                        try {
                                            finalOutput2 = new DeflaterOutputStream(finalOutput, new Deflater(9), true);
                                        } catch (Exception e5) {
                                            e = e5;
                                            outputStream = ofstream;
                                            arrayList = backupQueue;
                                            z = true;
                                            stringBuilder2 = headerbuf;
                                            packageManager = pm;
                                            it2 = it;
                                            it = iter;
                                            try {
                                                Slog.e(BackupManagerService.TAG, "Unable to emit archive header", e);
                                                if (out3 != null) {
                                                }
                                                this.mOutputFile.close();
                                                synchronized (this.mLatch) {
                                                }
                                                sendEndBackup();
                                                obbConnection.tearDown();
                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                this.backupManagerService.getWakelock().release();
                                                return;
                                            } catch (RemoteException e6) {
                                                Slog.e(BackupManagerService.TAG, "App died during full backup");
                                                if (out3 != null) {
                                                }
                                                this.mOutputFile.close();
                                                synchronized (this.mLatch) {
                                                }
                                                sendEndBackup();
                                                obbConnection.tearDown();
                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                this.backupManagerService.getWakelock().release();
                                            } catch (Exception e7) {
                                                e = e7;
                                                Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                                if (out3 != null) {
                                                }
                                                this.mOutputFile.close();
                                                synchronized (this.mLatch) {
                                                }
                                                sendEndBackup();
                                                obbConnection.tearDown();
                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                this.backupManagerService.getWakelock().release();
                                            }
                                        } catch (RemoteException e8) {
                                            z = keyValueBackupQueue;
                                            it2 = it;
                                            it = iter;
                                            Slog.e(BackupManagerService.TAG, "App died during full backup");
                                            if (out3 != null) {
                                            }
                                            this.mOutputFile.close();
                                            synchronized (this.mLatch) {
                                            }
                                            sendEndBackup();
                                            obbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                        } catch (Throwable th4) {
                                            N = th4;
                                            pkg = out4;
                                            outputStream = ofstream;
                                            arrayList = backupQueue;
                                            z = true;
                                            packageManager = pm;
                                            arrayList2 = it;
                                            out4 = out3;
                                            th = N;
                                            if (out4 != null) {
                                            }
                                            this.mOutputFile.close();
                                            synchronized (this.mLatch) {
                                            }
                                            sendEndBackup();
                                            obbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                            throw th;
                                        }
                                    } catch (Exception e9) {
                                        e = e9;
                                        outputStream = ofstream;
                                        arrayList = backupQueue;
                                        stringBuilder2 = headerbuf;
                                        packageManager = pm;
                                        arrayList2 = keyValueBackupQueue3;
                                        z = true;
                                        it = iter;
                                        Slog.e(BackupManagerService.TAG, "Unable to emit archive header", e);
                                        if (out3 != null) {
                                        }
                                        this.mOutputFile.close();
                                        synchronized (this.mLatch) {
                                        }
                                        sendEndBackup();
                                        obbConnection.tearDown();
                                        Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                        this.backupManagerService.getWakelock().release();
                                        return;
                                    } catch (RemoteException e10) {
                                        outputStream = ofstream;
                                        arrayList = backupQueue;
                                        packageManager = pm;
                                        arrayList2 = keyValueBackupQueue3;
                                        z = true;
                                        it = iter;
                                        Slog.e(BackupManagerService.TAG, "App died during full backup");
                                        if (out3 != null) {
                                        }
                                        this.mOutputFile.close();
                                        synchronized (this.mLatch) {
                                        }
                                        sendEndBackup();
                                        obbConnection.tearDown();
                                        Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                        this.backupManagerService.getWakelock().release();
                                    } catch (Throwable N3) {
                                        pkg = out4;
                                        outputStream = ofstream;
                                        arrayList = backupQueue;
                                        packageManager = pm;
                                        arrayList2 = keyValueBackupQueue3;
                                        z = true;
                                        out4 = out3;
                                        it = iter;
                                        th = N3;
                                        if (out4 != null) {
                                        }
                                        this.mOutputFile.close();
                                        synchronized (this.mLatch) {
                                        }
                                        sendEndBackup();
                                        obbConnection.tearDown();
                                        Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                        this.backupManagerService.getWakelock().release();
                                        throw th;
                                    }
                                }
                                it = keyValueBackupQueue3;
                                keyValueBackupQueue = true;
                                finalOutput2 = finalOutput;
                                out = finalOutput2;
                            } catch (RemoteException e11) {
                            } catch (Exception e12) {
                                e = e12;
                                fileOutputStream = ofstream;
                                arrayList = backupQueue;
                                arrayList2 = keyValueBackupQueue3;
                                it = iter;
                                packageManager = pm;
                                z = true;
                                Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                if (out3 != null) {
                                }
                                this.mOutputFile.close();
                                synchronized (this.mLatch) {
                                }
                                sendEndBackup();
                                obbConnection.tearDown();
                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                this.backupManagerService.getWakelock().release();
                            } catch (Throwable th5) {
                            }
                            try {
                                if (this.mIncludeShared) {
                                    try {
                                        i = 0;
                                        try {
                                            out4 = this.backupManagerService.getPackageManager().getPackageInfo(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE, 0);
                                            backupQueue.add(out4);
                                        } catch (NameNotFoundException e13) {
                                        }
                                    } catch (NameNotFoundException e14) {
                                        i = 0;
                                        try {
                                            Slog.e(BackupManagerService.TAG, "Unable to find shared-storage backup handler");
                                            N2 = backupQueue.size();
                                            while (true) {
                                                i2 = i;
                                                if (i2 < N2) {
                                                }
                                                out = out3;
                                                i = i3 + 1;
                                                iter = it;
                                                headerbuf = stringBuilder2;
                                                pm = packageManager;
                                                ofstream = fileOutputStream;
                                                backupQueue = arrayList;
                                                iter2 = keyValueBackupQueue2;
                                                keyValueBackupQueue = true;
                                                out2 = pkg2;
                                            }
                                        } catch (RemoteException e15) {
                                            out3 = out;
                                        } catch (Exception e16) {
                                            e = e16;
                                            out3 = out;
                                            outputStream = ofstream;
                                            arrayList = backupQueue;
                                            z = keyValueBackupQueue;
                                            packageManager = pm;
                                            it2 = iter2;
                                            it = iter;
                                            Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                            if (out3 != null) {
                                            }
                                            this.mOutputFile.close();
                                            synchronized (this.mLatch) {
                                            }
                                            sendEndBackup();
                                            obbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                        } catch (Throwable N32) {
                                            th = N32;
                                            pkg = out4;
                                            out4 = out;
                                            outputStream = ofstream;
                                            arrayList = backupQueue;
                                            z = keyValueBackupQueue;
                                            packageManager = pm;
                                            it2 = iter2;
                                            it = iter;
                                            if (out4 != null) {
                                            }
                                            this.mOutputFile.close();
                                            synchronized (this.mLatch) {
                                            }
                                            sendEndBackup();
                                            obbConnection.tearDown();
                                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                            this.backupManagerService.getWakelock().release();
                                            throw th;
                                        }
                                        sendEndBackup();
                                        obbConnection.tearDown();
                                        Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                        this.backupManagerService.getWakelock().release();
                                    }
                                }
                                i = 0;
                                N2 = backupQueue.size();
                                while (true) {
                                    i2 = i;
                                    if (i2 < N2) {
                                        break;
                                    }
                                    try {
                                        out4 = (PackageInfo) backupQueue.get(i2);
                                        try {
                                            str2 = BackupManagerService.TAG;
                                            StringBuilder stringBuilder5 = new StringBuilder();
                                            try {
                                                stringBuilder5.append("--- Performing full backup for package ");
                                                stringBuilder5.append(out4.packageName);
                                                stringBuilder5.append(" ---");
                                                Slog.i(str2, stringBuilder5.toString());
                                                boolean isSharedStorage = out4.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE);
                                                Iterator<Entry<String, PackageInfo>> iter4 = iter;
                                                try {
                                                    FullBackupEngine fullBackupEngine = fullBackupEngine;
                                                    pkg5 = out4;
                                                    i3 = i2;
                                                    OutputStream out5 = out;
                                                    fileOutputStream = ofstream;
                                                    arrayList = backupQueue;
                                                    FullBackupEngine fullBackupEngine2 = fullBackupEngine;
                                                    keyValueBackupQueue2 = iter2;
                                                    it = iter4;
                                                    stringBuilder2 = headerbuf;
                                                    packageManager = pm;
                                                    try {
                                                        fullBackupEngine = new FullBackupEngine(this.backupManagerService, out, null, pkg5, this.mIncludeApks, this, JobStatus.NO_LATEST_RUNTIME, this.mCurrentOpToken, null);
                                                        this.mBackupEngine = fullBackupEngine2;
                                                        if (isSharedStorage) {
                                                            try {
                                                                str = "Shared storage";
                                                                pkg2 = pkg5;
                                                            } catch (RemoteException e17) {
                                                                out4 = pkg5;
                                                                out3 = out5;
                                                            } catch (Exception e18) {
                                                                e = e18;
                                                                out4 = pkg5;
                                                                out3 = out5;
                                                            } catch (Throwable N322) {
                                                                th = N322;
                                                                out4 = out5;
                                                                arrayList2 = keyValueBackupQueue2;
                                                                z = true;
                                                            }
                                                        } else {
                                                            pkg2 = pkg5;
                                                            try {
                                                                str = pkg2.packageName;
                                                            } catch (RemoteException e19) {
                                                                out3 = out5;
                                                                out4 = pkg2;
                                                                arrayList2 = keyValueBackupQueue2;
                                                                z = true;
                                                                Slog.e(BackupManagerService.TAG, "App died during full backup");
                                                                if (out3 != null) {
                                                                }
                                                                this.mOutputFile.close();
                                                                synchronized (this.mLatch) {
                                                                }
                                                                sendEndBackup();
                                                                obbConnection.tearDown();
                                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                                this.backupManagerService.getWakelock().release();
                                                            } catch (Exception e20) {
                                                                e = e20;
                                                                out3 = out5;
                                                                out4 = pkg2;
                                                                arrayList2 = keyValueBackupQueue2;
                                                                z = true;
                                                                Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                                                if (out3 != null) {
                                                                }
                                                                this.mOutputFile.close();
                                                                synchronized (this.mLatch) {
                                                                }
                                                                sendEndBackup();
                                                                obbConnection.tearDown();
                                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                                this.backupManagerService.getWakelock().release();
                                                            } catch (Throwable N3222) {
                                                                pkg5 = pkg2;
                                                                out4 = out5;
                                                                arrayList2 = keyValueBackupQueue2;
                                                                z = true;
                                                                th = N3222;
                                                                if (out4 != null) {
                                                                }
                                                                this.mOutputFile.close();
                                                                synchronized (this.mLatch) {
                                                                }
                                                                sendEndBackup();
                                                                obbConnection.tearDown();
                                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                                this.backupManagerService.getWakelock().release();
                                                                throw th;
                                                            }
                                                        }
                                                        sendOnBackupPackage(str);
                                                        this.mCurrentTarget = pkg2;
                                                        this.mBackupEngine.backupOnePackage();
                                                        if (!this.mIncludeObbs || isSharedStorage) {
                                                            out3 = out5;
                                                        } else {
                                                            out3 = out5;
                                                            try {
                                                                if (obbConnection.backupObbs(pkg2, out3) == null) {
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("Failure writing OBB stack for ");
                                                                    stringBuilder.append(pkg2);
                                                                    throw new RuntimeException(stringBuilder.toString());
                                                                }
                                                            } catch (RemoteException e21) {
                                                                out2 = pkg2;
                                                                z = true;
                                                                Slog.e(BackupManagerService.TAG, "App died during full backup");
                                                                if (out3 != null) {
                                                                }
                                                                this.mOutputFile.close();
                                                                synchronized (this.mLatch) {
                                                                }
                                                                sendEndBackup();
                                                                obbConnection.tearDown();
                                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                                this.backupManagerService.getWakelock().release();
                                                            } catch (Exception e22) {
                                                                e = e22;
                                                                out2 = pkg2;
                                                                z = true;
                                                                Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                                                if (out3 != null) {
                                                                }
                                                                this.mOutputFile.close();
                                                                synchronized (this.mLatch) {
                                                                }
                                                                sendEndBackup();
                                                                obbConnection.tearDown();
                                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                                this.backupManagerService.getWakelock().release();
                                                            } catch (Throwable th6) {
                                                                N3222 = th6;
                                                                pkg5 = pkg2;
                                                                out4 = out3;
                                                                arrayList2 = keyValueBackupQueue2;
                                                                z = true;
                                                                th = N3222;
                                                                if (out4 != null) {
                                                                }
                                                                this.mOutputFile.close();
                                                                synchronized (this.mLatch) {
                                                                }
                                                                sendEndBackup();
                                                                obbConnection.tearDown();
                                                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                                                this.backupManagerService.getWakelock().release();
                                                                throw th;
                                                            }
                                                        }
                                                        out = out3;
                                                        i = i3 + 1;
                                                        iter = it;
                                                        headerbuf = stringBuilder2;
                                                        pm = packageManager;
                                                        ofstream = fileOutputStream;
                                                        backupQueue = arrayList;
                                                        iter2 = keyValueBackupQueue2;
                                                        keyValueBackupQueue = true;
                                                        out2 = pkg2;
                                                    } catch (RemoteException e23) {
                                                        out3 = out5;
                                                        out4 = pkg5;
                                                        arrayList2 = keyValueBackupQueue2;
                                                        z = true;
                                                    } catch (Exception e24) {
                                                        e = e24;
                                                        out3 = out5;
                                                        out4 = pkg5;
                                                        arrayList2 = keyValueBackupQueue2;
                                                        z = true;
                                                    } catch (Throwable N32222) {
                                                        pkg2 = pkg5;
                                                        out4 = out5;
                                                        arrayList2 = keyValueBackupQueue2;
                                                        z = true;
                                                        th = N32222;
                                                    }
                                                } catch (RemoteException e25) {
                                                    pkg2 = out4;
                                                    out3 = out;
                                                    fileOutputStream = ofstream;
                                                    arrayList = backupQueue;
                                                    keyValueBackupQueue2 = iter2;
                                                    it = iter4;
                                                    arrayList2 = keyValueBackupQueue2;
                                                    z = true;
                                                } catch (Exception e26) {
                                                    e = e26;
                                                    pkg2 = out4;
                                                    out3 = out;
                                                    fileOutputStream = ofstream;
                                                    arrayList = backupQueue;
                                                    keyValueBackupQueue2 = iter2;
                                                    it = iter4;
                                                    packageManager = pm;
                                                    arrayList2 = keyValueBackupQueue2;
                                                    z = true;
                                                } catch (Throwable N322222) {
                                                    fileOutputStream = ofstream;
                                                    arrayList = backupQueue;
                                                    keyValueBackupQueue2 = iter2;
                                                    it = iter4;
                                                    packageManager = pm;
                                                    pkg5 = out4;
                                                    out4 = out;
                                                    arrayList2 = keyValueBackupQueue2;
                                                    z = true;
                                                    th = N322222;
                                                }
                                            } catch (RemoteException e27) {
                                                pkg2 = out4;
                                                out3 = out;
                                                fileOutputStream = ofstream;
                                                arrayList = backupQueue;
                                                packageManager = pm;
                                                keyValueBackupQueue2 = iter2;
                                                it = iter;
                                                arrayList2 = keyValueBackupQueue2;
                                                z = true;
                                            } catch (Exception e28) {
                                                e = e28;
                                                pkg2 = out4;
                                                out3 = out;
                                                fileOutputStream = ofstream;
                                                arrayList = backupQueue;
                                                packageManager = pm;
                                                keyValueBackupQueue2 = iter2;
                                                it = iter;
                                                arrayList2 = keyValueBackupQueue2;
                                                z = true;
                                            } catch (Throwable th7) {
                                                N322222 = th7;
                                                fileOutputStream = ofstream;
                                                arrayList = backupQueue;
                                                packageManager = pm;
                                                keyValueBackupQueue2 = iter2;
                                                it = iter;
                                                pkg5 = out4;
                                                out4 = out;
                                                arrayList2 = keyValueBackupQueue2;
                                                z = true;
                                            }
                                        } catch (RemoteException e29) {
                                            pkg2 = out4;
                                            out3 = out;
                                            fileOutputStream = ofstream;
                                            arrayList = backupQueue;
                                            packageManager = pm;
                                            keyValueBackupQueue2 = iter2;
                                            it = iter;
                                            z = keyValueBackupQueue;
                                            arrayList2 = keyValueBackupQueue2;
                                        } catch (Exception e30) {
                                            e = e30;
                                            pkg2 = out4;
                                            out3 = out;
                                            fileOutputStream = ofstream;
                                            arrayList = backupQueue;
                                            packageManager = pm;
                                            keyValueBackupQueue2 = iter2;
                                            it = iter;
                                            z = keyValueBackupQueue;
                                            arrayList2 = keyValueBackupQueue2;
                                        } catch (Throwable th8) {
                                            N322222 = th8;
                                            fileOutputStream = ofstream;
                                            arrayList = backupQueue;
                                            packageManager = pm;
                                            keyValueBackupQueue2 = iter2;
                                            it = iter;
                                            pkg5 = out4;
                                            out4 = out;
                                            z = keyValueBackupQueue;
                                            arrayList2 = keyValueBackupQueue2;
                                        }
                                    } catch (RemoteException e31) {
                                        out3 = out;
                                        fileOutputStream = ofstream;
                                        arrayList = backupQueue;
                                        packageManager = pm;
                                        keyValueBackupQueue2 = iter2;
                                        it = iter;
                                        z = keyValueBackupQueue;
                                        arrayList2 = keyValueBackupQueue2;
                                    } catch (Exception e32) {
                                        e = e32;
                                        out3 = out;
                                        fileOutputStream = ofstream;
                                        arrayList = backupQueue;
                                        packageManager = pm;
                                        keyValueBackupQueue2 = iter2;
                                        it = iter;
                                        z = keyValueBackupQueue;
                                        arrayList2 = keyValueBackupQueue2;
                                    } catch (Throwable N3222222) {
                                        out3 = out;
                                        fileOutputStream = ofstream;
                                        arrayList = backupQueue;
                                        packageManager = pm;
                                        keyValueBackupQueue2 = iter2;
                                        it = iter;
                                        pkg = out4;
                                        z = keyValueBackupQueue;
                                        arrayList2 = keyValueBackupQueue2;
                                        out4 = out3;
                                        th = N3222222;
                                    }
                                }
                            } catch (RemoteException e33) {
                                out3 = out;
                                fileOutputStream = ofstream;
                                arrayList = backupQueue;
                                z = keyValueBackupQueue;
                                packageManager = pm;
                                arrayList2 = iter2;
                            } catch (Exception e34) {
                                e = e34;
                                out3 = out;
                                fileOutputStream = ofstream;
                                arrayList = backupQueue;
                                z = keyValueBackupQueue;
                                packageManager = pm;
                                arrayList2 = iter2;
                                it = iter;
                                Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                                if (out3 != null) {
                                }
                                this.mOutputFile.close();
                                synchronized (this.mLatch) {
                                }
                                sendEndBackup();
                                obbConnection.tearDown();
                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                this.backupManagerService.getWakelock().release();
                            } catch (Throwable N32222222) {
                                out3 = out;
                                fileOutputStream = ofstream;
                                arrayList = backupQueue;
                                z = keyValueBackupQueue;
                                packageManager = pm;
                                arrayList2 = iter2;
                                it = iter;
                                pkg = out4;
                                out4 = out3;
                                th = N32222222;
                                if (out4 != null) {
                                }
                                this.mOutputFile.close();
                                synchronized (this.mLatch) {
                                }
                                sendEndBackup();
                                obbConnection.tearDown();
                                Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                                this.backupManagerService.getWakelock().release();
                                throw th;
                            }
                            sendEndBackup();
                            obbConnection.tearDown();
                            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                            this.backupManagerService.getWakelock().release();
                        }
                        Slog.e(BackupManagerService.TAG, "Unencrypted backup of encrypted device; aborting");
                        if (out3 != null) {
                            try {
                                out3.flush();
                                out3.close();
                            } catch (IOException e210) {
                                str2 = BackupManagerService.TAG;
                                StringBuilder stringBuilder6 = new StringBuilder();
                                stringBuilder6.append("IO error closing adb backup file: ");
                                stringBuilder6.append(e210.getMessage());
                                Slog.e(str2, stringBuilder6.toString());
                            }
                        }
                        this.mOutputFile.close();
                        synchronized (this.mLatch) {
                            this.mLatch.set(true);
                            this.mLatch.notifyAll();
                        }
                        sendEndBackup();
                        obbConnection.tearDown();
                        Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                        this.backupManagerService.getWakelock().release();
                        return;
                    }
                } catch (RemoteException e35) {
                    fileOutputStream = ofstream;
                    arrayList = backupQueue;
                    arrayList2 = keyValueBackupQueue3;
                    it = iter;
                    z = true;
                    packageManager = pm;
                    Slog.e(BackupManagerService.TAG, "App died during full backup");
                    if (out3 != null) {
                    }
                    this.mOutputFile.close();
                    synchronized (this.mLatch) {
                    }
                    sendEndBackup();
                    obbConnection.tearDown();
                    Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                    this.backupManagerService.getWakelock().release();
                } catch (Exception e36) {
                    e = e36;
                    fileOutputStream = ofstream;
                    arrayList = backupQueue;
                    arrayList2 = keyValueBackupQueue3;
                    it = iter;
                    z = true;
                    packageManager = pm;
                    Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
                    if (out3 != null) {
                    }
                    this.mOutputFile.close();
                    synchronized (this.mLatch) {
                    }
                    sendEndBackup();
                    obbConnection.tearDown();
                    Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                    this.backupManagerService.getWakelock().release();
                } catch (Throwable th9) {
                    N32222222 = th9;
                    pkg5 = out4;
                    fileOutputStream = ofstream;
                    arrayList = backupQueue;
                    arrayList2 = keyValueBackupQueue3;
                    it = iter;
                    z = true;
                    packageManager = pm;
                    out4 = out3;
                    th = N32222222;
                    if (out4 != null) {
                    }
                    this.mOutputFile.close();
                    synchronized (this.mLatch) {
                    }
                    sendEndBackup();
                    obbConnection.tearDown();
                    Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
                    this.backupManagerService.getWakelock().release();
                    throw th;
                }
            }
            z2 = false;
            encrypting = z2;
            if (this.backupManagerService.deviceIsEncrypted()) {
            }
            finalOutput = ofstream;
            if (this.backupManagerService.backupPasswordMatches(this.mCurrentPassword)) {
            }
            th = N32222222;
            if (out4 != null) {
            }
            this.mOutputFile.close();
            synchronized (this.mLatch) {
            }
            sendEndBackup();
            obbConnection.tearDown();
            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
            this.backupManagerService.getWakelock().release();
            throw th;
        } catch (RemoteException e37) {
            fileOutputStream = ofstream;
            arrayList = backupQueue;
            arrayList2 = keyValueBackupQueue3;
            it = iter;
            z = true;
            packageManager = pm;
            Slog.e(BackupManagerService.TAG, "App died during full backup");
            if (out3 != null) {
            }
            this.mOutputFile.close();
            synchronized (this.mLatch) {
            }
            sendEndBackup();
            obbConnection.tearDown();
            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
            this.backupManagerService.getWakelock().release();
        } catch (Exception e38) {
            e = e38;
            fileOutputStream = ofstream;
            arrayList = backupQueue;
            arrayList2 = keyValueBackupQueue3;
            it = iter;
            z = true;
            packageManager = pm;
            Slog.e(BackupManagerService.TAG, "Internal exception during full backup", e);
            if (out3 != null) {
            }
            this.mOutputFile.close();
            synchronized (this.mLatch) {
            }
            sendEndBackup();
            obbConnection.tearDown();
            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
            this.backupManagerService.getWakelock().release();
        } catch (Throwable th10) {
            N32222222 = th10;
            fileOutputStream = ofstream;
            arrayList = backupQueue;
            arrayList2 = keyValueBackupQueue3;
            it = iter;
            z = true;
            packageManager = pm;
            pkg = out4;
            out4 = out3;
            th = N32222222;
            if (out4 != null) {
            }
            this.mOutputFile.close();
            synchronized (this.mLatch) {
            }
            sendEndBackup();
            obbConnection.tearDown();
            Slog.d(BackupManagerService.TAG, "Full backup pass complete.");
            this.backupManagerService.getWakelock().release();
            throw th;
        }
    }

    public void execute() {
    }

    public void operationComplete(long result) {
    }

    public void handleCancel(boolean cancelAll) {
        PackageInfo target = this.mCurrentTarget;
        String str = BackupManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("adb backup cancel of ");
        stringBuilder.append(target);
        Slog.w(str, stringBuilder.toString());
        if (target != null) {
            this.backupManagerService.tearDownAgentAndKill(this.mCurrentTarget.applicationInfo);
        }
        this.backupManagerService.removeOperation(this.mCurrentOpToken);
    }
}
