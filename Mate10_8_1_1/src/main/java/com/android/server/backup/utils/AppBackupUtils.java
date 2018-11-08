package com.android.server.backup.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import com.android.internal.util.ArrayUtils;
import com.android.server.backup.RefactoredBackupManagerService;

public class AppBackupUtils {
    public static boolean appIsEligibleForBackup(ApplicationInfo app) {
        if ((app.flags & 32768) == 0) {
            return false;
        }
        if ((app.uid >= 10000 || app.backupAgentName != null) && !app.packageName.equals(RefactoredBackupManagerService.SHARED_BACKUP_AGENT_PACKAGE)) {
            return true;
        }
        return false;
    }

    public static boolean appIsStopped(ApplicationInfo app) {
        return (app.flags & DumpState.DUMP_COMPILER_STATS) != 0;
    }

    public static boolean appGetsFullBackup(PackageInfo pkg) {
        boolean z = true;
        if (pkg.applicationInfo.backupAgentName == null) {
            return true;
        }
        if ((pkg.applicationInfo.flags & 67108864) == 0) {
            z = false;
        }
        return z;
    }

    public static boolean appIsKeyValueOnly(PackageInfo pkg) {
        return appGetsFullBackup(pkg) ^ 1;
    }

    public static boolean signaturesMatch(Signature[] storedSigs, PackageInfo target) {
        if (target == null) {
            return false;
        }
        if ((target.applicationInfo.flags & 1) != 0) {
            return true;
        }
        Signature[] deviceSigs = target.signatures;
        if (ArrayUtils.isEmpty(storedSigs) || ArrayUtils.isEmpty(deviceSigs)) {
            return false;
        }
        for (Signature equals : storedSigs) {
            boolean match = false;
            for (Object equals2 : deviceSigs) {
                if (equals.equals(equals2)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }
}
