package com.android.server.pm;

import android.content.Intent;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser.Package;
import android.content.pm.ResolveInfo;
import java.io.File;
import java.util.List;

public interface IHwPackageManagerServiceEx {
    void checkHwCertification(Package packageR, boolean z);

    void cleanUpHwCert();

    int getAppUseNotchMode(String str);

    boolean getHwCertPermission(boolean z, Package packageR, String str);

    void handleActivityInfoNotFound(int i, Intent intent, int i2, List<ResolveInfo> list);

    PackageInfo handlePackageNotFound(String str, int i, int i2);

    boolean hwPerformDexOptMode(String str, boolean z, String str2, boolean z2, boolean z3, String str3);

    void initHwCertificationManager();

    void installPackageAsUser(String str, IPackageInstallObserver2 iPackageInstallObserver2, int i, String str2, int i2);

    boolean isAllowUninstallApp(String str);

    boolean isAllowedSetHomeActivityForAntiMal(PackageInfo packageInfo, int i);

    boolean isApkDexOpt(String str);

    boolean isDisallowUninstallApk(String str);

    boolean isDisallowedInstallApk(Package packageR);

    boolean isPerfOptEnable(String str, int i);

    boolean isPersistentUpdatable(Package packageR);

    boolean isPrivilegedPreApp(File file);

    boolean isSystemPreApp(File file);

    void readPersistentConfig();

    void resolvePersistentFlagForPackage(int i, Package packageR);

    void setAppCanUninstall(String str, boolean z);

    void setAppUseNotchMode(String str, int i);

    void systemReady();

    void updateNochScreenWhite(String str, String str2, int i);
}
