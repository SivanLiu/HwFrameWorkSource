package com.android.server.pm;

import android.app.AppGlobals;
import android.content.Intent;
import android.content.pm.PackageParser.Package;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.util.ArraySet;
import com.android.server.pm.dex.PackageDexUsage.PackageUseInfo;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import libcore.io.Libcore;

public class PackageManagerServiceUtils {
    private static final long SEVEN_DAYS_IN_MILLISECONDS = 604800000;

    private static ArraySet<String> getPackageNamesForIntent(Intent intent, int userId) {
        Iterable ris = null;
        try {
            ris = AppGlobals.getPackageManager().queryIntentReceivers(intent, null, 0, userId).getList();
        } catch (RemoteException e) {
        }
        ArraySet<String> pkgNames = new ArraySet();
        if (r4 != null) {
            for (ResolveInfo ri : r4) {
                pkgNames.add(ri.activityInfo.packageName);
            }
        }
        return pkgNames;
    }

    public static void sortPackagesByUsageDate(List<Package> pkgs, PackageManagerService packageManagerService) {
        if (packageManagerService.isHistoricalPackageUsageAvailable()) {
            Collections.sort(pkgs, -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w.$INST$1);
        }
    }

    private static void applyPackageFilter(Predicate<Package> filter, Collection<Package> result, Collection<Package> packages, List<Package> sortTemp, PackageManagerService packageManagerService) {
        for (Package pkg : packages) {
            if (filter.test(pkg)) {
                sortTemp.add(pkg);
            }
        }
        sortPackagesByUsageDate(sortTemp, packageManagerService);
        packages.removeAll(sortTemp);
        for (Package pkg2 : sortTemp) {
            result.add(pkg2);
            Collection<Package> deps = packageManagerService.findSharedNonSystemLibraries(pkg2);
            if (!deps.isEmpty()) {
                deps.removeAll(result);
                result.addAll(deps);
                packages.removeAll(deps);
            }
        }
        sortTemp.clear();
    }

    public static List<Package> getPackagesForDexopt(Collection<Package> packages, PackageManagerService packageManagerService) {
        Predicate<Package> remainingPredicate;
        ArrayList<Package> remainingPkgs = new ArrayList(packages);
        LinkedList<Package> result = new LinkedList();
        ArrayList<Package> sortTemp = new ArrayList(remainingPkgs.size());
        applyPackageFilter(-$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw.$INST$0, result, remainingPkgs, sortTemp, packageManagerService);
        packageManagerService.filterShellApps(remainingPkgs, result);
        applyPackageFilter(new -$Lambda$KFbchFEqJgs_hY1HweauKRNA_ds((byte) 2, getPackageNamesForIntent(new Intent("android.intent.action.PRE_BOOT_COMPLETED"), 0)), result, remainingPkgs, sortTemp, packageManagerService);
        applyPackageFilter(new -$Lambda$KFbchFEqJgs_hY1HweauKRNA_ds((byte) 3, packageManagerService.getDexManager()), result, remainingPkgs, sortTemp, packageManagerService);
        if (remainingPkgs.isEmpty() || !packageManagerService.isHistoricalPackageUsageAvailable()) {
            remainingPredicate = -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw.$INST$2;
        } else {
            long estimatedPreviousSystemUseTime = ((Package) Collections.max(remainingPkgs, -$Lambda$tZuhGcRRWSq5m9LlSrypurdt-0w.$INST$0)).getLatestForegroundPackageUseTimeInMills();
            if (estimatedPreviousSystemUseTime != 0) {
                remainingPredicate = new -$Lambda$5qSWip3Q3NYNf0S8FNRU2st8ZfA((byte) 1, estimatedPreviousSystemUseTime - 604800000);
            } else {
                remainingPredicate = -$Lambda$s_oh3oeib-Exts1l3lS2Euiarsw.$INST$1;
            }
            sortPackagesByUsageDate(remainingPkgs, packageManagerService);
        }
        applyPackageFilter(remainingPredicate, result, remainingPkgs, sortTemp, packageManagerService);
        return result;
    }

    static /* synthetic */ boolean lambda$-com_android_server_pm_PackageManagerServiceUtils_7258(long cutoffTime, Package pkg) {
        return pkg.getLatestForegroundPackageUseTimeInMills() >= cutoffTime;
    }

    static boolean isUnusedSinceTimeInMillis(long firstInstallTime, long currentTimeInMillis, long thresholdTimeinMillis, PackageUseInfo packageUseInfo, long latestPackageUseTimeInMillis, long latestForegroundPackageUseTimeInMillis) {
        if (currentTimeInMillis - firstInstallTime < thresholdTimeinMillis) {
            return false;
        }
        if (currentTimeInMillis - latestForegroundPackageUseTimeInMillis < thresholdTimeinMillis) {
            return false;
        }
        int isAnyCodePathUsedByOtherApps;
        if (currentTimeInMillis - latestPackageUseTimeInMillis < thresholdTimeinMillis) {
            isAnyCodePathUsedByOtherApps = packageUseInfo.isAnyCodePathUsedByOtherApps();
        } else {
            isAnyCodePathUsedByOtherApps = 0;
        }
        return isAnyCodePathUsedByOtherApps ^ 1;
    }

    public static String realpath(File path) throws IOException {
        try {
            return Libcore.os.realpath(path.getAbsolutePath());
        } catch (ErrnoException ee) {
            throw ee.rethrowAsIOException();
        }
    }

    public static String packagesToString(Collection<Package> c) {
        StringBuilder sb = new StringBuilder();
        for (Package pkg : c) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(pkg.packageName);
        }
        return sb.toString();
    }

    public static boolean checkISA(String isa) {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (VMRuntime.getInstructionSet(abi).equals(isa)) {
                return true;
            }
        }
        return false;
    }
}
