package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser.Package;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import java.util.HashMap;
import java.util.HashSet;

public final class HwPackageManagerServiceUtils {
    private static final String FLAG_APK_NOSYS = "nosys";
    private static final String TAG = "HwPackageManagerServiceUtils";
    private static HashMap<String, HashSet<String>> sCotaDelInstallMap = null;
    private static HashMap<String, HashSet<String>> sDelMultiInstallMap = null;

    public static void updateFlagsForMarketSystemApp(Package pkg) {
        if (pkg != null && pkg.isUpdatedSystemApp() && pkg.mAppMetaData != null && pkg.mAppMetaData.getBoolean("android.huawei.MARKETED_SYSTEM_APP", false)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateFlagsForMarketSystemApp");
            stringBuilder.append(pkg.packageName);
            stringBuilder.append(" has MetaData HUAWEI_MARKETED_SYSTEM_APP, add FLAG_MARKETED_SYSTEM_APP");
            Slog.i(str, stringBuilder.toString());
            ApplicationInfo applicationInfo = pkg.applicationInfo;
            applicationInfo.hwFlags |= 536870912;
            if (pkg.mPersistentApp) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateFlagsForMarketSystemApp ");
                stringBuilder.append(pkg.packageName);
                stringBuilder.append(" is a persistent updated system app!");
                Slog.i(str, stringBuilder.toString());
                applicationInfo = pkg.applicationInfo;
                applicationInfo.flags |= 8;
            }
        }
    }

    public static void setDelMultiInstallMap(HashMap<String, HashSet<String>> delMultiInstallMap) {
        if (delMultiInstallMap == null) {
            Slog.w(TAG, "DelMultiInstallMap is null!");
        }
        sDelMultiInstallMap = delMultiInstallMap;
    }

    public static void setCotaDelInstallMap(HashMap<String, HashSet<String>> cotaDelInstallMap) {
        if (cotaDelInstallMap == null) {
            Slog.w(TAG, "CotaDelInstallMap is null!");
        }
        sCotaDelInstallMap = cotaDelInstallMap;
    }

    public static boolean isNoSystemPreApp(String codePath) {
        boolean z = false;
        if (TextUtils.isEmpty(codePath)) {
            Slog.w(TAG, "CodePath is null when check isNoSystemPreApp!");
            return false;
        }
        String path;
        if (codePath.endsWith(".apk")) {
            path = getCustPackagePath(codePath);
        } else {
            path = codePath;
        }
        if (path == null) {
            return false;
        }
        boolean normalDelNoSysApp = sDelMultiInstallMap != null && ((HashSet) sDelMultiInstallMap.get(FLAG_APK_NOSYS)).contains(path);
        boolean cotaNoBootDelNoSysApp = sCotaDelInstallMap != null && ((HashSet) sCotaDelInstallMap.get(FLAG_APK_NOSYS)).contains(path);
        if (normalDelNoSysApp || cotaNoBootDelNoSysApp) {
            z = true;
        }
        return z;
    }

    public static String getCustPackagePath(String codePath) {
        if (TextUtils.isEmpty(codePath)) {
            Slog.w(TAG, "CodePath is null when getCustPackagePath!");
            return null;
        }
        String packagePath;
        int lastIndex = codePath.lastIndexOf(47);
        if (lastIndex > 0) {
            packagePath = codePath.substring(0, lastIndex);
        } else {
            packagePath = null;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCustPackagePath ERROR:  ");
            stringBuilder.append(codePath);
            Log.e(str, stringBuilder.toString());
        }
        return packagePath;
    }

    public static void addFlagsForRemovablePreApk(Package pkg, int hwFlags) {
        if ((hwFlags & DumpState.DUMP_HANDLE) != 0) {
            ApplicationInfo applicationInfo = pkg.applicationInfo;
            applicationInfo.hwFlags = DumpState.DUMP_HANDLE | applicationInfo.hwFlags;
        }
    }

    public static void addFlagsForUpdatedRemovablePreApk(Package pkg, int hwFlags) {
        if ((hwFlags & 67108864) != 0) {
            ApplicationInfo applicationInfo = pkg.applicationInfo;
            applicationInfo.hwFlags = 67108864 | applicationInfo.hwFlags;
        }
    }

    public static boolean hwlocationIsVendor(String codePath) {
        if (!TextUtils.isEmpty(codePath)) {
            return codePath.startsWith("/data/hw_init/vendor/");
        }
        Slog.w(TAG, "CodePath is null when check is vendor!");
        return false;
    }

    public static boolean hwlocationIsProduct(String codePath) {
        if (!TextUtils.isEmpty(codePath)) {
            return codePath.startsWith("/data/hw_init/product/");
        }
        Slog.w(TAG, "CodePath is null when check is product!");
        return false;
    }
}
