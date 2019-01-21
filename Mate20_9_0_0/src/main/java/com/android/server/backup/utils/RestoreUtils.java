package com.android.server.backup.utils;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender.Stub;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.Session;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.restore.RestoreDeleteObserver;
import com.android.server.backup.restore.RestorePolicy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class RestoreUtils {

    private static class LocalIntentReceiver {
        private Stub mLocalSender;
        private final Object mLock;
        @GuardedBy("mLock")
        private Intent mResult;

        private LocalIntentReceiver() {
            this.mLock = new Object();
            this.mResult = null;
            this.mLocalSender = new Stub() {
                public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                    synchronized (LocalIntentReceiver.this.mLock) {
                        LocalIntentReceiver.this.mResult = intent;
                        LocalIntentReceiver.this.mLock.notifyAll();
                    }
                }
            };
        }

        public IntentSender getIntentSender() {
            return new IntentSender(this.mLocalSender);
        }

        public Intent getResult() {
            Intent intent;
            synchronized (this.mLock) {
                while (this.mResult == null) {
                    try {
                        this.mLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                intent = this.mResult;
            }
            return intent;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:146:0x02bf  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02a9 A:{SYNTHETIC, Splitter:B:137:0x02a9} */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02a9 A:{SYNTHETIC, Splitter:B:137:0x02a9} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x02bf  */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x02bf  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02a9 A:{SYNTHETIC, Splitter:B:137:0x02a9} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x037a A:{SYNTHETIC, Splitter:B:184:0x037a} */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02a9 A:{SYNTHETIC, Splitter:B:137:0x02a9} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x02bf  */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x02bf  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02a9 A:{SYNTHETIC, Splitter:B:137:0x02a9} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02f8 A:{SYNTHETIC, Splitter:B:160:0x02f8} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x037a A:{SYNTHETIC, Splitter:B:184:0x037a} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02f8 A:{SYNTHETIC, Splitter:B:160:0x02f8} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02f8 A:{SYNTHETIC, Splitter:B:160:0x02f8} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x037a A:{SYNTHETIC, Splitter:B:184:0x037a} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02f8 A:{SYNTHETIC, Splitter:B:160:0x02f8} */
    /* JADX WARNING: Removed duplicated region for block: B:184:0x037a A:{SYNTHETIC, Splitter:B:184:0x037a} */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02a9 A:{SYNTHETIC, Splitter:B:137:0x02a9} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x02bf  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x0118 A:{ExcHandler: all (th java.lang.Throwable), Splitter:B:61:0x0114} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x02bf  */
    /* JADX WARNING: Removed duplicated region for block: B:137:0x02a9 A:{SYNTHETIC, Splitter:B:137:0x02a9} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:63:0x0118, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:64:0x0119, code skipped:
            r12 = r25;
            r14 = r26;
            r10 = r27;
            r1 = null;
            r4 = r28;
     */
    /* JADX WARNING: Missing block: B:150:0x02c9, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:151:0x02ca, code skipped:
            r12 = r25;
            r14 = r26;
            r10 = r27;
            r4 = r28;
     */
    /* JADX WARNING: Missing block: B:185:?, code skipped:
            $closeResource(r1, r2);
     */
    /* JADX WARNING: Missing block: B:186:0x037e, code skipped:
            r0 = e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean installApk(InputStream instream, Context context, RestoreDeleteObserver deleteObserver, HashMap<String, Signature[]> manifestSignatures, HashMap<String, RestorePolicy> packagePolicies, FileMetadata info, String installerPackageName, BytesReadListener bytesReadListener) {
        RestoreDeleteObserver restoreDeleteObserver;
        HashMap<String, Signature[]> hashMap;
        HashMap<String, RestorePolicy> hashMap2;
        BytesReadListener bytesReadListener2;
        Throwable th;
        Throwable th2;
        Throwable th3;
        InputStream params;
        Throwable th4;
        Exception t;
        String str;
        boolean okay;
        LocalIntentReceiver localIntentReceiver;
        FileMetadata fileMetadata = info;
        String str2 = BackupManagerService.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Installing from backup: ");
        stringBuilder.append(fileMetadata.packageName);
        Slog.d(str2, stringBuilder.toString());
        boolean okay2;
        FileMetadata fileMetadata2;
        try {
            LocalIntentReceiver receiver = new LocalIntentReceiver();
            PackageManager packageManager = context.getPackageManager();
            PackageInstaller installer = packageManager.getPackageInstaller();
            SessionParams params2 = new SessionParams(1);
            try {
                params2.setInstallerPackageName(installerPackageName);
                int sessionId = installer.createSession(params2);
                SessionParams sessionParams;
                try {
                    Session session = installer.openSession(sessionId);
                    boolean okay3;
                    try {
                        try {
                            okay2 = true;
                            okay3 = session;
                            try {
                                OutputStream apkStream = session.openWrite(fileMetadata.packageName, 0, fileMetadata.size);
                                try {
                                    byte[] buffer = new byte[32768];
                                    long size = fileMetadata.size;
                                    while (size > 0) {
                                        long toRead;
                                        try {
                                            if (((long) buffer.length) < size) {
                                                try {
                                                    toRead = (long) buffer.length;
                                                } catch (Throwable th5) {
                                                    th2 = th5;
                                                    restoreDeleteObserver = deleteObserver;
                                                    hashMap = manifestSignatures;
                                                    hashMap2 = packagePolicies;
                                                    bytesReadListener2 = bytesReadListener;
                                                    fileMetadata2 = fileMetadata;
                                                    sessionParams = params2;
                                                    th3 = null;
                                                    th = null;
                                                    params = instream;
                                                    if (apkStream != null) {
                                                    }
                                                    throw th2;
                                                }
                                            }
                                            toRead = size;
                                            sessionParams = params2;
                                        } catch (Throwable th6) {
                                            th2 = th6;
                                            bytesReadListener2 = bytesReadListener;
                                            sessionParams = params2;
                                            params = instream;
                                            restoreDeleteObserver = deleteObserver;
                                            hashMap = manifestSignatures;
                                            hashMap2 = packagePolicies;
                                            fileMetadata2 = fileMetadata;
                                            th3 = null;
                                            th = null;
                                            if (apkStream != null) {
                                            }
                                            throw th2;
                                        }
                                        try {
                                            int didRead = instream.read(buffer, 0, (int) toRead);
                                            if (didRead >= 0) {
                                                try {
                                                    bytesReadListener.onBytesRead((long) didRead);
                                                } catch (Throwable th7) {
                                                    th2 = th7;
                                                    restoreDeleteObserver = deleteObserver;
                                                    hashMap = manifestSignatures;
                                                    hashMap2 = packagePolicies;
                                                    th3 = null;
                                                    fileMetadata2 = info;
                                                    th = null;
                                                    if (apkStream != null) {
                                                    }
                                                    throw th2;
                                                }
                                            }
                                            bytesReadListener2 = bytesReadListener;
                                            apkStream.write(buffer, 0, didRead);
                                            size -= (long) didRead;
                                            params2 = sessionParams;
                                            fileMetadata = info;
                                        } catch (Throwable th8) {
                                            th2 = th8;
                                            bytesReadListener2 = bytesReadListener;
                                            restoreDeleteObserver = deleteObserver;
                                            hashMap = manifestSignatures;
                                            hashMap2 = packagePolicies;
                                            fileMetadata2 = fileMetadata;
                                            th3 = null;
                                            th = null;
                                            if (apkStream != null) {
                                            }
                                            throw th2;
                                        }
                                    }
                                    bytesReadListener2 = bytesReadListener;
                                    sessionParams = params2;
                                    params = instream;
                                    if (apkStream != null) {
                                        try {
                                            $closeResource(null, apkStream);
                                        } catch (Throwable th9) {
                                        }
                                    }
                                    okay3.abandon();
                                    if (okay3) {
                                        try {
                                            $closeResource(null, okay3);
                                        } catch (Exception e) {
                                            t = e;
                                            restoreDeleteObserver = deleteObserver;
                                            hashMap = manifestSignatures;
                                            hashMap2 = packagePolicies;
                                            fileMetadata2 = info;
                                        } catch (IOException e2) {
                                            restoreDeleteObserver = deleteObserver;
                                            hashMap = manifestSignatures;
                                            hashMap2 = packagePolicies;
                                            fileMetadata2 = info;
                                            Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                                            return false;
                                        }
                                    }
                                    Intent result = null;
                                    if (1 != null) {
                                        try {
                                            try {
                                                if (packagePolicies.get(info.packageName) != RestorePolicy.ACCEPT) {
                                                    restoreDeleteObserver = deleteObserver;
                                                    hashMap = manifestSignatures;
                                                    return false;
                                                }
                                                restoreDeleteObserver = deleteObserver;
                                                hashMap = manifestSignatures;
                                                return okay2;
                                            } catch (IOException e3) {
                                                restoreDeleteObserver = deleteObserver;
                                                hashMap = manifestSignatures;
                                                Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                                                return false;
                                            }
                                        } catch (IOException e4) {
                                            hashMap2 = packagePolicies;
                                            restoreDeleteObserver = deleteObserver;
                                            hashMap = manifestSignatures;
                                            Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                                            return false;
                                        }
                                    }
                                    hashMap2 = packagePolicies;
                                    fileMetadata2 = info;
                                    apkStream = null;
                                    String installedPackageName = result.getStringExtra("android.content.pm.extra.PACKAGE_NAME");
                                    int i;
                                    if (installedPackageName.equals(fileMetadata2.packageName)) {
                                        Intent intent;
                                        try {
                                            PackageInfo pkg = packageManager.getPackageInfo(fileMetadata2.packageName, 134217728);
                                            if ((pkg.applicationInfo.flags & 32768) == 0) {
                                                try {
                                                    String str3 = BackupManagerService.TAG;
                                                    StringBuilder stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("Restore stream contains apk of package ");
                                                    stringBuilder2.append(fileMetadata2.packageName);
                                                    stringBuilder2.append(" but it disallows backup/restore");
                                                    Slog.w(str3, stringBuilder2.toString());
                                                    hashMap = manifestSignatures;
                                                    intent = result;
                                                    i = 1;
                                                    okay3 = false;
                                                } catch (NameNotFoundException e5) {
                                                    hashMap = manifestSignatures;
                                                    intent = result;
                                                    i = 1;
                                                    try {
                                                        str = BackupManagerService.TAG;
                                                        okay3 = new StringBuilder();
                                                        okay3.append("Install of package ");
                                                        okay3.append(fileMetadata2.packageName);
                                                        okay3.append(" succeeded but now not found");
                                                        Slog.w(str, okay3.toString());
                                                        okay = false;
                                                        okay3 = okay;
                                                        if (apkStream == null) {
                                                        }
                                                        return okay3;
                                                    } catch (IOException e6) {
                                                        restoreDeleteObserver = deleteObserver;
                                                        Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                                                        return false;
                                                    }
                                                }
                                                if (apkStream == null) {
                                                    try {
                                                        deleteObserver.reset();
                                                        try {
                                                            packageManager.deletePackage(installedPackageName, deleteObserver, 0);
                                                            deleteObserver.waitForCompletion();
                                                        } catch (IOException e7) {
                                                        }
                                                    } catch (IOException e8) {
                                                        restoreDeleteObserver = deleteObserver;
                                                        Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                                                        return false;
                                                    }
                                                }
                                                restoreDeleteObserver = deleteObserver;
                                                return okay3;
                                            }
                                            try {
                                                if (AppBackupUtils.signaturesMatch((Signature[]) manifestSignatures.get(fileMetadata2.packageName), pkg, (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class))) {
                                                    intent = result;
                                                    try {
                                                        i = 1;
                                                        if (pkg.applicationInfo.uid < 10000) {
                                                            try {
                                                                if (pkg.applicationInfo.backupAgentName == null) {
                                                                    str = BackupManagerService.TAG;
                                                                    StringBuilder stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append("Installed app ");
                                                                    stringBuilder3.append(fileMetadata2.packageName);
                                                                    stringBuilder3.append(" has restricted uid and no agent");
                                                                    Slog.w(str, stringBuilder3.toString());
                                                                    okay3 = false;
                                                                }
                                                            } catch (NameNotFoundException e9) {
                                                                str = BackupManagerService.TAG;
                                                                okay3 = new StringBuilder();
                                                                okay3.append("Install of package ");
                                                                okay3.append(fileMetadata2.packageName);
                                                                okay3.append(" succeeded but now not found");
                                                                Slog.w(str, okay3.toString());
                                                                okay = false;
                                                                okay3 = okay;
                                                                if (apkStream == null) {
                                                                }
                                                                return okay3;
                                                            }
                                                        }
                                                        okay3 = okay2;
                                                    } catch (NameNotFoundException e10) {
                                                        i = 1;
                                                        str = BackupManagerService.TAG;
                                                        okay3 = new StringBuilder();
                                                        okay3.append("Install of package ");
                                                        okay3.append(fileMetadata2.packageName);
                                                        okay3.append(" succeeded but now not found");
                                                        Slog.w(str, okay3.toString());
                                                        okay = false;
                                                        okay3 = okay;
                                                        if (apkStream == null) {
                                                        }
                                                        return okay3;
                                                    }
                                                }
                                                PackageInfo packageInfo = pkg;
                                                intent = result;
                                                i = 1;
                                                str2 = BackupManagerService.TAG;
                                                StringBuilder stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("Installed app ");
                                                stringBuilder4.append(fileMetadata2.packageName);
                                                stringBuilder4.append(" signatures do not match restore manifest");
                                                Slog.w(str2, stringBuilder4.toString());
                                                okay3 = false;
                                                apkStream = true;
                                            } catch (NameNotFoundException e11) {
                                                intent = result;
                                                i = 1;
                                                str = BackupManagerService.TAG;
                                                okay3 = new StringBuilder();
                                                okay3.append("Install of package ");
                                                okay3.append(fileMetadata2.packageName);
                                                okay3.append(" succeeded but now not found");
                                                Slog.w(str, okay3.toString());
                                                okay = false;
                                                okay3 = okay;
                                                if (apkStream == null) {
                                                }
                                                return okay3;
                                            }
                                            if (apkStream == null) {
                                            }
                                            return okay3;
                                        } catch (NameNotFoundException e12) {
                                            hashMap = manifestSignatures;
                                            intent = result;
                                            i = 1;
                                            str = BackupManagerService.TAG;
                                            okay3 = new StringBuilder();
                                            okay3.append("Install of package ");
                                            okay3.append(fileMetadata2.packageName);
                                            okay3.append(" succeeded but now not found");
                                            Slog.w(str, okay3.toString());
                                            okay = false;
                                            okay3 = okay;
                                            if (apkStream == null) {
                                            }
                                            return okay3;
                                        } catch (IOException e13) {
                                            hashMap = manifestSignatures;
                                            restoreDeleteObserver = deleteObserver;
                                            Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                                            return false;
                                        }
                                    }
                                    str2 = BackupManagerService.TAG;
                                    StringBuilder stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("Restore stream claimed to include apk for ");
                                    stringBuilder5.append(fileMetadata2.packageName);
                                    stringBuilder5.append(" but apk was really ");
                                    stringBuilder5.append(installedPackageName);
                                    Slog.w(str2, stringBuilder5.toString());
                                    okay = false;
                                    apkStream = true;
                                    hashMap = manifestSignatures;
                                    i = 1;
                                    okay3 = okay;
                                    if (apkStream == null) {
                                    }
                                    return okay3;
                                } catch (Throwable th10) {
                                    th2 = th10;
                                    restoreDeleteObserver = deleteObserver;
                                    hashMap = manifestSignatures;
                                    hashMap2 = packagePolicies;
                                    bytesReadListener2 = bytesReadListener;
                                    fileMetadata2 = fileMetadata;
                                    sessionParams = params2;
                                    th3 = null;
                                    params = instream;
                                    th = null;
                                    if (apkStream != null) {
                                        try {
                                            $closeResource(th, apkStream);
                                        } catch (Throwable th11) {
                                            th2 = th11;
                                            if (okay3) {
                                            }
                                            throw th2;
                                        }
                                    }
                                    throw th2;
                                }
                            } catch (Throwable th12) {
                                th2 = th12;
                                restoreDeleteObserver = deleteObserver;
                                hashMap = manifestSignatures;
                                hashMap2 = packagePolicies;
                                bytesReadListener2 = bytesReadListener;
                                fileMetadata2 = fileMetadata;
                                sessionParams = params2;
                                th3 = null;
                                params = instream;
                                if (okay3) {
                                }
                                throw th2;
                            }
                        } catch (Throwable th13) {
                            th2 = th13;
                            restoreDeleteObserver = deleteObserver;
                            hashMap2 = packagePolicies;
                            bytesReadListener2 = bytesReadListener;
                            fileMetadata2 = fileMetadata;
                            okay2 = true;
                            sessionParams = params2;
                            okay3 = session;
                            th3 = null;
                            params = instream;
                            hashMap = manifestSignatures;
                            if (okay3) {
                            }
                            throw th2;
                        }
                    } catch (Throwable th14) {
                        th2 = th14;
                        restoreDeleteObserver = deleteObserver;
                        hashMap2 = packagePolicies;
                        bytesReadListener2 = bytesReadListener;
                        okay2 = true;
                        localIntentReceiver = receiver;
                        sessionParams = params2;
                        okay3 = session;
                        params = instream;
                        hashMap = manifestSignatures;
                        fileMetadata2 = fileMetadata;
                        th3 = null;
                        if (okay3) {
                        }
                        throw th2;
                    }
                } catch (Exception e14) {
                    t = e14;
                    restoreDeleteObserver = deleteObserver;
                    hashMap = manifestSignatures;
                    hashMap2 = packagePolicies;
                    bytesReadListener2 = bytesReadListener;
                    okay2 = true;
                    localIntentReceiver = receiver;
                    sessionParams = params2;
                    params = instream;
                    fileMetadata2 = fileMetadata;
                    try {
                        installer.abandonSession(sessionId);
                        throw t;
                    } catch (IOException e15) {
                        Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                        return false;
                    }
                }
            } catch (IOException e16) {
                params = instream;
                restoreDeleteObserver = deleteObserver;
                hashMap = manifestSignatures;
                hashMap2 = packagePolicies;
                bytesReadListener2 = bytesReadListener;
                fileMetadata2 = fileMetadata;
                okay2 = true;
                Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
                return false;
            }
        } catch (IOException e17) {
            params = instream;
            restoreDeleteObserver = deleteObserver;
            hashMap = manifestSignatures;
            hashMap2 = packagePolicies;
            String str4 = installerPackageName;
            bytesReadListener2 = bytesReadListener;
            fileMetadata2 = fileMetadata;
            okay2 = true;
            Slog.e(BackupManagerService.TAG, "Unable to transcribe restored apk for install");
            return false;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }
}
