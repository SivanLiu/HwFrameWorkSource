package com.android.server.pm;

import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.SigningDetails;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;
import com.android.server.pm.permission.PermissionManagerInternal;
import java.util.HashMap;
import java.util.HashSet;

public interface IHwPackageManagerInner {
    InstallParams createInstallParams(OriginInfo originInfo, MoveInfo moveInfo, IPackageInstallObserver2 iPackageInstallObserver2, int i, String str, String str2, VerificationInfo verificationInfo, UserHandle userHandle, String str3, String[] strArr, SigningDetails signingDetails, int i2);

    HashMap<String, HashSet<String>> getHwPMSDelMultiInstallMap();

    HashMap<String, HashSet<String>> getHwPMSMultiInstallMap();

    Handler getPackageHandler();

    ArrayMap<String, Package> getPackagesLock();

    PermissionManagerInternal getPermissionManager();

    Settings getSettings();

    boolean isUpgrade();

    boolean performDexOptMode(String str, boolean z, String str2, boolean z2, boolean z3, String str3);
}
