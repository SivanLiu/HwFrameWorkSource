package com.android.server.backup.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.transport.TransportClient;
import com.android.server.pm.DumpState;

public class AppBackupUtils {
    private static final boolean DEBUG = false;

    public static boolean appIsEligibleForBackup(ApplicationInfo app, PackageManager pm) {
        if ((app.flags & 32768) == 0) {
            return false;
        }
        if ((app.uid < 10000 && app.backupAgentName == null) || app.packageName.equals(BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE) || app.isInstantApp()) {
            return false;
        }
        return appIsDisabled(app, pm) ^ 1;
    }

    public static boolean appIsRunningAndEligibleForBackupWithTransport(TransportClient transportClient, String packageName, PackageManager pm) {
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 134217728);
            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (!appIsEligibleForBackup(applicationInfo, pm) || appIsStopped(applicationInfo) || appIsDisabled(applicationInfo, pm)) {
                return false;
            }
            if (transportClient != null) {
                try {
                    return transportClient.connectOrThrow("AppBackupUtils.appIsEligibleForBackupAtRuntime").isAppEligibleForBackup(packageInfo, appGetsFullBackup(packageInfo));
                } catch (Exception e) {
                    String str = BackupManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to ask about eligibility: ");
                    stringBuilder.append(e.getMessage());
                    Slog.e(str, stringBuilder.toString());
                }
            }
            return true;
        } catch (NameNotFoundException e2) {
            return false;
        }
    }

    public static boolean appIsDisabled(ApplicationInfo app, PackageManager pm) {
        switch (pm.getApplicationEnabledSetting(app.packageName)) {
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
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

    public static boolean signaturesMatch(Signature[] storedSigs, PackageInfo target, PackageManagerInternal pmi) {
        if (target == null || target.packageName == null) {
            return false;
        }
        if ((target.applicationInfo.flags & 1) != 0) {
            return true;
        }
        if (ArrayUtils.isEmpty(storedSigs)) {
            return false;
        }
        SigningInfo signingInfo = target.signingInfo;
        if (signingInfo == null) {
            Slog.w(BackupManagerService.TAG, "signingInfo is empty, app was either unsigned or the flag PackageManager#GET_SIGNING_CERTIFICATES was not specified");
            return false;
        }
        if (nStored == 1) {
            return pmi.isDataRestoreSafe(storedSigs[0], target.packageName);
        }
        for (Signature equals : storedSigs) {
            boolean match = false;
            for (Object equals2 : signingInfo.getApkContentsSigners()) {
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
