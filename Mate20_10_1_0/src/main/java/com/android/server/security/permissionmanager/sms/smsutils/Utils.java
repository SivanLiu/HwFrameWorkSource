package com.android.server.security.permissionmanager.sms.smsutils;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.huawei.android.content.pm.HwPackageManager;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    private static final int DEFAULT_GET_PACKAGE_INFO = 0;
    private static final int DEFAULT_LENGTH = 8;
    private static final int GRANTED = 1;
    private static final int HSM_MATCH_DIRECT_BOOT = 786432;
    private static final long INT_TO_LONG_MASK = 4294967295L;
    public static final boolean IS_CHINA = SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    private static final Intent LAUNCHER_INTENT = new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER");
    private static final String LOG_TAG = "Utils";
    public static final int MIN_APPLICATION_UID = 10000;
    private static final int PKGNAME_NUM = 1;
    private static final List<String> SHARE_UID_LIST = new ArrayList(8);
    private static final int SYSTEM_APP_FLAG = 1;
    private static final int TRANSACT_CODE = 1004;

    static {
        SHARE_UID_LIST.add("android.uid.system");
        SHARE_UID_LIST.add("android.uid.phone");
        SHARE_UID_LIST.add("android.uid.bluetooth");
        SHARE_UID_LIST.add("android.uid.log");
        SHARE_UID_LIST.add("android.uid.nfc");
    }

    private Utils() {
    }

    public static boolean isTestApp(String pkgName) {
        return HwPackageManager.getPrivilegeAppType(pkgName) == 1;
    }

    public static boolean isPackageShouldMonitor(Context context, String pkgName, int uid) {
        if (pkgName == null || pkgName.isEmpty()) {
            Log.e(LOG_TAG, "pkgName is null or empty");
            return true;
        } else if (context == null) {
            return true;
        } else {
            int userId = UserHandle.getUserId(uid);
            PackageManager pm = context.getPackageManager();
            try {
                PackageInfo pkgInfo = pm.getPackageInfoAsUser(pkgName, HSM_MATCH_DIRECT_BOOT, userId);
                if (pkgInfo == null) {
                    Log.v(LOG_TAG, "Fail to get info of " + pkgName);
                    return true;
                } else if (pkgInfo.applicationInfo.uid < 10000) {
                    Log.v(LOG_TAG, pkgName + " uid is " + pkgInfo.applicationInfo.uid);
                    return false;
                } else if (SHARE_UID_LIST.contains(pkgInfo.sharedUserId)) {
                    Log.v(LOG_TAG, pkgName + " share uid: " + pkgInfo.sharedUserId);
                    return false;
                } else if (isTestApp(pkgName)) {
                    Log.v(LOG_TAG, pkgName + " is test apk");
                    return false;
                } else {
                    boolean isSystemApp = (pkgInfo.applicationInfo.flags & 1) != 0;
                    boolean isRemovable = isRemoveAblePreInstall(pm, pkgName, userId);
                    Log.v(LOG_TAG, pkgName + " is system app? " + isSystemApp + " is removable? " + isRemovable);
                    if (!isSystemApp || isRemovable) {
                        return true;
                    }
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_TAG, "checkSystemAppInternal get Exception!" + e.getMessage());
                return true;
            }
        }
    }

    private static boolean isRemoveAblePreInstall(PackageManager pm, String pkgName, int userId) {
        try {
            int hwFlags = pm.getApplicationInfoAsUser(pkgName, 8192, userId).hwFlags;
            boolean isRemovable = true;
            boolean notZeroMaskRemovableFlag = (hwFlags & 33554432) != 0;
            boolean notZeroUpdateRemovableFlag = (hwFlags & 67108864) != 0;
            if (!notZeroMaskRemovableFlag && !notZeroUpdateRemovableFlag) {
                isRemovable = false;
            }
            return isRemovable;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "getApplicationInfo exception: " + e.getMessage());
            return false;
        }
    }

    public static boolean checkRuntimePermission(Context context, String pkgName, String permissionName, int uid) {
        if (context == null || TextUtils.isEmpty(pkgName) || TextUtils.isEmpty(permissionName)) {
            return false;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(pkgName, 798720);
            if (packageInfo.applicationInfo == null) {
                return false;
            }
            if (packageInfo.applicationInfo.targetSdkVersion > 22) {
                return isPermissionGranted(packageInfo, permissionName);
            }
            return isOpAllowed(context, permissionName, pkgName, uid);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "getPackageInfo NameNotFoundException for " + pkgName);
            return false;
        }
    }

    private static boolean isOpAllowed(Context context, String permissionName, String pkgName, int uid) {
        AppOpsManager aom = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        String appOp = AppOpsManager.permissionToOp(permissionName);
        boolean isAppOpAllowed = false;
        if (TextUtils.isEmpty(appOp) || aom == null) {
            Log.e(LOG_TAG, "isApkOpAllow op is empty, or aom is null!");
            return false;
        }
        if (aom.checkOpNoThrow(appOp, uid, pkgName) == 0) {
            isAppOpAllowed = true;
        }
        return isAppOpAllowed;
    }

    private static boolean isPermissionGranted(PackageInfo pkgInfo, String permissionName) {
        if (pkgInfo == null || TextUtils.isEmpty(permissionName)) {
            Log.e(LOG_TAG, "isPermissionGrant packageInfo is null!");
            return false;
        }
        String[] permissionNames = pkgInfo.requestedPermissions;
        if (permissionNames == null || permissionNames.length == 0) {
            return false;
        }
        int length = permissionNames.length;
        int i = 0;
        while (i < length) {
            if (!permissionName.equals(permissionNames[i]) || i >= pkgInfo.requestedPermissionsFlags.length) {
                i++;
            } else if ((pkgInfo.requestedPermissionsFlags[i] & 2) != 0) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static String getAppInfoByUidAndPid(Context context, int uid, int pid) {
        String[] packages = context.getPackageManager().getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            return "";
        }
        if (packages.length == 1) {
            return packages[0];
        }
        String pkgName = "";
        ActivityManager am = (ActivityManager) context.getSystemService(BigMemoryConstant.BIGMEMINFO_ITEM_TAG);
        if (am == null) {
            return pkgName;
        }
        for (ActivityManager.RunningAppProcessInfo runningInfo : am.getRunningAppProcesses()) {
            int runningPid = runningInfo.pid;
            int runningUid = runningInfo.uid;
            if (runningPid == pid && runningUid == uid) {
                String[] resultPackages = runningInfo.pkgList;
                if (resultPackages != null && resultPackages.length > 0) {
                    pkgName = resultPackages[0];
                }
                Log.i(LOG_TAG, "getAppInfoByUidAndPid pkgName = " + pkgName);
                return pkgName;
            }
        }
        return pkgName;
    }
}
