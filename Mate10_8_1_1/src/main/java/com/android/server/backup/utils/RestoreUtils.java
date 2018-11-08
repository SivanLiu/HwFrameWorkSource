package com.android.server.backup.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.Uri;
import android.util.Slog;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.RefactoredBackupManagerService;
import com.android.server.backup.restore.RestoreDeleteObserver;
import com.android.server.backup.restore.RestoreInstallObserver;
import com.android.server.backup.restore.RestorePolicy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class RestoreUtils {
    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean installApk(InputStream instream, PackageManager packageManager, RestoreInstallObserver installObserver, RestoreDeleteObserver deleteObserver, HashMap<String, Signature[]> manifestSignatures, HashMap<String, RestorePolicy> packagePolicies, FileMetadata info, String installerPackage, BytesReadListener bytesReadListener, File dataDir) {
        boolean okay = true;
        Slog.d(RefactoredBackupManagerService.TAG, "Installing from backup: " + info.packageName);
        File apkFile = new File(dataDir, info.packageName);
        try {
            FileOutputStream apkStream = new FileOutputStream(apkFile);
            byte[] buffer = new byte[32768];
            long size = info.size;
            while (size > 0) {
                int didRead = instream.read(buffer, 0, (int) (((long) buffer.length) < size ? (long) buffer.length : size));
                if (didRead >= 0) {
                    bytesReadListener.onBytesRead((long) didRead);
                }
                apkStream.write(buffer, 0, didRead);
                size -= (long) didRead;
            }
            apkStream.close();
            apkFile.setReadable(true, false);
            Uri packageUri = Uri.fromFile(apkFile);
            installObserver.reset();
            packageManager.installPackage(packageUri, installObserver, 34, installerPackage);
            installObserver.waitForCompletion();
            if (installObserver.getResult() != 1) {
                if (packagePolicies.get(info.packageName) != RestorePolicy.ACCEPT) {
                    okay = false;
                }
            } else {
                boolean uninstall = false;
                if (installObserver.getPackageName().equals(info.packageName)) {
                    try {
                        PackageInfo pkg = packageManager.getPackageInfo(info.packageName, 64);
                        if ((pkg.applicationInfo.flags & 32768) == 0) {
                            Slog.w(RefactoredBackupManagerService.TAG, "Restore stream contains apk of package " + info.packageName + " but it disallows backup/restore");
                            okay = false;
                        } else {
                            if (!AppBackupUtils.signaturesMatch((Signature[]) manifestSignatures.get(info.packageName), pkg)) {
                                Slog.w(RefactoredBackupManagerService.TAG, "Installed app " + info.packageName + " signatures do not match restore manifest");
                                okay = false;
                                uninstall = true;
                            } else if (pkg.applicationInfo.uid < 10000 && pkg.applicationInfo.backupAgentName == null) {
                                Slog.w(RefactoredBackupManagerService.TAG, "Installed app " + info.packageName + " has restricted uid and no agent");
                                okay = false;
                            }
                        }
                    } catch (NameNotFoundException e) {
                        Slog.w(RefactoredBackupManagerService.TAG, "Install of package " + info.packageName + " succeeded but now not found");
                        okay = false;
                    }
                } else {
                    Slog.w(RefactoredBackupManagerService.TAG, "Restore stream claimed to include apk for " + info.packageName + " but apk was really " + installObserver.getPackageName());
                    okay = false;
                    uninstall = true;
                }
                if (uninstall) {
                    deleteObserver.reset();
                    packageManager.deletePackage(installObserver.getPackageName(), deleteObserver, 0);
                    deleteObserver.waitForCompletion();
                }
            }
            apkFile.delete();
            return okay;
        } catch (IOException e2) {
            Slog.e(RefactoredBackupManagerService.TAG, "Unable to transcribe restored apk for install");
            return false;
        } catch (Throwable th) {
            apkFile.delete();
        }
    }
}
