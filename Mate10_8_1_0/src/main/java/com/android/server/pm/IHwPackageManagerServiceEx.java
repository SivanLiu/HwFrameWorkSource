package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.PackageParser.Package;

public interface IHwPackageManagerServiceEx {
    int getAppUseNotchMode(String str);

    boolean isAllowedSetHomeActivityForAntiMal(PackageInfo packageInfo, int i);

    boolean isDisallowedInstallApk(Package packageR);

    void setAppUseNotchMode(String str, int i);

    void updateNochScreenWhite(String str, String str2, int i);
}
