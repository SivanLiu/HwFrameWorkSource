package com.android.server.pm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageParser;
import android.os.Binder;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public final class HotInstall {
    public static final String AUTO_INSTALL_APK_CONFIG = "/data/system/auto_install/APKInstallListEMUI5Release.txt";
    public static final String AUTO_INSTALL_DEL_APK_CONFIG = "/data/system/auto_install/DelAPKInstallListEMUI5Release.txt";
    private static final String AUTO_INSTALL_PATH = "/data/system/auto_install";
    private static final String COTA_APP_UPDATE_APPWIDGET = "huawei.intent.action.UPDATE_COTA_APP_WIDGET";
    private static final String COTA_APP_UPDATE_APPWIDGET_EXTRA = "huawei.intent.extra.cota_package_list";
    private static boolean DEBUG = PackageManagerService.DEBUG_INSTALL;
    private static final String FLAG_APK_NOSYS = "nosys";
    private static final String FLAG_APK_PRIV = "priv";
    private static final String FLAG_APK_SYS = "sys";
    private static final int SCAN_AS_PRIVILEGED = 262144;
    private static final int SCAN_AS_SYSTEM = 131072;
    private static final int SCAN_BOOTING = 16;
    private static final int SCAN_FIRST_BOOT_OR_UPGRADE = 8192;
    private static final int SCAN_INITIAL = 512;
    private static final String TAG = "HotInstall";
    private static List<PackageParser.Package> sAutoInstallPkgList = new ArrayList();
    private static HotInstall sHotInstaller = new HotInstall();
    private static boolean sIsAutoInstall = false;
    private HashSet<String> mCurrenPaths = new HashSet<>();
    private PackageManagerService mPms;

    private HotInstall() {
    }

    public static HotInstall getInstance() {
        return sHotInstaller;
    }

    public static void recordAutoInstallPkg(PackageParser.Package pkg) {
        if (sIsAutoInstall) {
            Log.i(TAG, "pkg installed " + pkg.packageName);
            sAutoInstallPkgList.add(pkg);
        }
    }

    public void setPackageManagerInner(IHwPackageManagerInner pmsInner) {
        if (pmsInner instanceof PackageManagerService) {
            this.mPms = (PackageManagerService) pmsInner;
        }
    }

    /* JADX INFO: finally extract failed */
    public void realStartAutoInstall(Context context, IHwPackageManagerServiceEx packageServiceEx, String apkInstallConfig, String removableApkInstallConfig, String strMccMnc) {
        if (context == null || packageServiceEx == null || (TextUtils.isEmpty(apkInstallConfig) && TextUtils.isEmpty(removableApkInstallConfig))) {
            Log.e(TAG, "param is not valid!");
            return;
        }
        if (DEBUG) {
            Log.i(TAG, "Start Auto Install mccmnc:" + strMccMnc + " apkInstallConfig:" + apkInstallConfig + " removableApkInstallConfig:" + removableApkInstallConfig);
        }
        saveAutoInstallConfig(apkInstallConfig, AUTO_INSTALL_APK_CONFIG);
        saveAutoInstallConfig(removableApkInstallConfig, AUTO_INSTALL_DEL_APK_CONFIG);
        auotInstallForMccMnc(packageServiceEx);
        int size = sAutoInstallPkgList.size();
        if (size == 0) {
            Log.e(TAG, "size=0");
            return;
        }
        PackageManagerService packageManagerService = this.mPms;
        if (packageManagerService == null) {
            Log.e(TAG, "mPms null");
            return;
        }
        packageManagerService.updateAllSharedLibrariesLocked((PackageParser.Package) null, Collections.unmodifiableMap(packageManagerService.mPackages));
        this.mPms.mPermissionManager.updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, false, this.mPms.mPackages.values(), this.mPms.mPermissionCallback);
        Log.i(TAG, "auto install size = " + size);
        for (int i = 0; i < size; i++) {
            this.mPms.prepareAppDataAfterInstallLIFInner(sAutoInstallPkgList.get(i));
        }
        this.mPms.mSettings.writeLPr();
        updateWidgetForHotInstall(context, sAutoInstallPkgList);
        sendPreBootBroadcastToManagedProvisioning(context);
        long identity = Binder.clearCallingIdentity();
        try {
            int[] userIds = UserManagerService.getInstance().getUserIds();
            for (int userId : userIds) {
                Log.i(TAG, "auto install apps have installed ,grantCustDefaultPermissions userId = " + userId);
                this.mPms.mDefaultPermissionPolicy.grantCustDefaultPermissions(userId);
            }
            Binder.restoreCallingIdentity(identity);
            updateLauncherLayout(context, strMccMnc);
            sIsAutoInstall = false;
            Log.i(TAG, "auto install complete!");
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public boolean isNonSystemPartition(String path) {
        if (this.mCurrenPaths.contains(path)) {
            return true;
        }
        return false;
    }

    private void updateLauncherLayout(Context context, String strMccMnc) {
        Intent intent = new Intent("com.huawei.android.launcher.STOP_LAUNCHER");
        intent.setPackage("com.huawei.android.launcher");
        intent.addFlags(268435456);
        intent.putExtra("mccmnc", strMccMnc);
        intent.putExtra("autoInstall", true);
        context.sendBroadcast(intent);
    }

    private void updateWidgetForHotInstall(Context context, List<PackageParser.Package> hotInstallPackageList) {
        Intent intent = new Intent(COTA_APP_UPDATE_APPWIDGET);
        Bundle extras = new Bundle();
        int size = hotInstallPackageList.size();
        String[] pkgList = new String[size];
        for (int j = 0; j < size; j++) {
            pkgList[j] = hotInstallPackageList.get(j).packageName;
        }
        extras.putStringArray(COTA_APP_UPDATE_APPWIDGET_EXTRA, pkgList);
        intent.addFlags(268435456);
        intent.putExtras(extras);
        intent.setPackage(AppStartupDataMgr.HWPUSH_PKGNAME);
        intent.putExtra("android.intent.extra.user_handle", 0);
        context.sendBroadcast(intent);
    }

    private void auotInstallForMccMnc(IHwPackageManagerServiceEx packageServiceEx) {
        HashMap<String, HashSet<String>> installMap = new HashMap<>();
        HashMap<String, HashSet<String>> delInstallMap = new HashMap<>();
        File installCfgFile = new File(AUTO_INSTALL_APK_CONFIG);
        if (installCfgFile.exists()) {
            HashSet<String> sysInstallSet = new HashSet<>();
            HashSet<String> privInstallSet = new HashSet<>();
            installMap.put(FLAG_APK_SYS, sysInstallSet);
            installMap.put(FLAG_APK_PRIV, privInstallSet);
            ArrayList<File> installFileList = new ArrayList<>();
            installFileList.add(installCfgFile);
            packageServiceEx.getAPKInstallListForHwPMS(installFileList, installMap);
            this.mCurrenPaths.addAll(sysInstallSet);
            this.mCurrenPaths.addAll(privInstallSet);
        }
        File delInstallCfgFile = new File(AUTO_INSTALL_DEL_APK_CONFIG);
        if (delInstallCfgFile.exists()) {
            HashSet<String> sysInstallSet2 = new HashSet<>();
            HashSet<String> privInstallSet2 = new HashSet<>();
            HashSet<String> noSysDelInstallSet = new HashSet<>();
            delInstallMap.put(FLAG_APK_SYS, sysInstallSet2);
            delInstallMap.put(FLAG_APK_PRIV, privInstallSet2);
            delInstallMap.put(FLAG_APK_NOSYS, noSysDelInstallSet);
            ArrayList<File> delInstallFileList = new ArrayList<>();
            delInstallFileList.add(delInstallCfgFile);
            packageServiceEx.getAPKInstallListForHwPMS(delInstallFileList, delInstallMap);
            HwPackageManagerServiceUtils.setAutoInstallMapForDelApps(delInstallMap);
            this.mCurrenPaths.addAll(sysInstallSet2);
            this.mCurrenPaths.addAll(privInstallSet2);
            this.mCurrenPaths.addAll(noSysDelInstallSet);
        }
        sIsAutoInstall = true;
        if (!installMap.isEmpty()) {
            packageServiceEx.installAPKforInstallListForHwPMS(installMap.get(FLAG_APK_SYS), 16, 8720 | 131072, 0, 0);
            packageServiceEx.installAPKforInstallListForHwPMS(installMap.get(FLAG_APK_PRIV), 16, 8720 | 131072 | 262144, 0, 0);
        }
        if (!delInstallMap.isEmpty()) {
            packageServiceEx.installAPKforInstallListForHwPMS(delInstallMap.get(FLAG_APK_SYS), 16, 8720 | 131072, 0, 33554432);
            packageServiceEx.installAPKforInstallListForHwPMS(delInstallMap.get(FLAG_APK_PRIV), 16, 8720 | 131072 | 262144, 0, 33554432);
            packageServiceEx.installAPKforInstallListForHwPMS(delInstallMap.get(FLAG_APK_NOSYS), 0, 8720, 0, 33554432);
        }
    }

    private void sendPreBootBroadcastToManagedProvisioning(Context context) {
        Intent preBootBroadcastIntent = new Intent("android.intent.action.PRE_BOOT_COMPLETED");
        preBootBroadcastIntent.addFlags(268435456);
        preBootBroadcastIntent.setComponent(new ComponentName("com.android.managedprovisioning", "com.android.managedprovisioning.ota.PreBootListener"));
        context.sendBroadcast(preBootBroadcastIntent);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0037, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:?, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x003c, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x003d, code lost:
        r3.addSuppressed(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0040, code lost:
        throw r4;
     */
    private void saveAutoInstallConfig(String config, String targetPath) {
        if (!TextUtils.isEmpty(config)) {
            File autoInstallPath = new File(AUTO_INSTALL_PATH);
            if (autoInstallPath.exists() || autoInstallPath.mkdir()) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(targetPath);
                    outputStream.write(config.getBytes("utf-8"));
                    try {
                        outputStream.close();
                    } catch (FileNotFoundException | UnsupportedEncodingException e) {
                        Log.e(TAG, "save auto install config failed, file not found or not supported encoding");
                    }
                } catch (IOException e2) {
                    Log.e(TAG, "save auto install config failed, io exception");
                }
            } else {
                Log.e(TAG, "create directory failed");
            }
        }
    }
}
