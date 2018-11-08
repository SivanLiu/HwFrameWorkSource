package com.android.server.pm.dex;

import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.FileUtils;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.pm.Installer;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.PackageDexOptimizer.ForcedUpdatePackageDexOptimizer;
import com.android.server.pm.PackageManagerServiceCompilerMapping;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.dex.PackageDexUsage.DexUseInfo;
import com.android.server.pm.dex.PackageDexUsage.PackageUseInfo;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DexManager {
    private static final boolean DEBUG = false;
    private static final PackageUseInfo DEFAULT_USE_INFO = new PackageUseInfo();
    private static int DEX_SEARCH_FOUND_PRIMARY = 1;
    private static int DEX_SEARCH_FOUND_SECONDARY = 3;
    private static int DEX_SEARCH_FOUND_SPLIT = 2;
    private static int DEX_SEARCH_NOT_FOUND = 0;
    private static final String TAG = "DexManager";
    private final Object mInstallLock;
    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    @GuardedBy("mPackageCodeLocationsCache")
    private final Map<String, PackageCodeLocations> mPackageCodeLocationsCache = new HashMap();
    private final PackageDexOptimizer mPackageDexOptimizer;
    private final PackageDexUsage mPackageDexUsage = new PackageDexUsage();
    private final IPackageManager mPackageManager;

    private class DexSearchResult {
        private int mOutcome;
        private String mOwningPackageName;

        public DexSearchResult(String owningPackageName, int outcome) {
            this.mOwningPackageName = owningPackageName;
            this.mOutcome = outcome;
        }

        public String toString() {
            return this.mOwningPackageName + "-" + this.mOutcome;
        }
    }

    private static class PackageCodeLocations {
        private final Map<Integer, Set<String>> mAppDataDirs;
        private String mBaseCodePath;
        private final String mPackageName;
        private final Set<String> mSplitCodePaths;

        public PackageCodeLocations(ApplicationInfo ai, int userId) {
            this(ai.packageName, ai.sourceDir, ai.splitSourceDirs);
            mergeAppDataDirs(ai.dataDir, userId);
        }

        public PackageCodeLocations(String packageName, String baseCodePath, String[] splitCodePaths) {
            this.mPackageName = packageName;
            this.mSplitCodePaths = new HashSet();
            this.mAppDataDirs = new HashMap();
            updateCodeLocation(baseCodePath, splitCodePaths);
        }

        public void updateCodeLocation(String baseCodePath, String[] splitCodePaths) {
            this.mBaseCodePath = baseCodePath;
            this.mSplitCodePaths.clear();
            if (splitCodePaths != null) {
                for (String split : splitCodePaths) {
                    this.mSplitCodePaths.add(split);
                }
            }
        }

        public void mergeAppDataDirs(String dataDir, int userId) {
            ((Set) DexManager.putIfAbsent(this.mAppDataDirs, Integer.valueOf(userId), new HashSet())).add(dataDir);
        }

        public int searchDex(String dexPath, int userId) {
            Set<String> userDataDirs = (Set) this.mAppDataDirs.get(Integer.valueOf(userId));
            if (userDataDirs == null) {
                return DexManager.DEX_SEARCH_NOT_FOUND;
            }
            if (this.mBaseCodePath.equals(dexPath)) {
                return DexManager.DEX_SEARCH_FOUND_PRIMARY;
            }
            if (this.mSplitCodePaths.contains(dexPath)) {
                return DexManager.DEX_SEARCH_FOUND_SPLIT;
            }
            for (String dataDir : userDataDirs) {
                if (dexPath.startsWith(dataDir)) {
                    return DexManager.DEX_SEARCH_FOUND_SECONDARY;
                }
            }
            return DexManager.DEX_SEARCH_NOT_FOUND;
        }
    }

    public static class RegisterDexModuleResult {
        public final String message;
        public final boolean success;

        public RegisterDexModuleResult() {
            this(false, null);
        }

        public RegisterDexModuleResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public DexManager(IPackageManager pms, PackageDexOptimizer pdo, Installer installer, Object installLock) {
        this.mPackageManager = pms;
        this.mPackageDexOptimizer = pdo;
        this.mInstaller = installer;
        this.mInstallLock = installLock;
    }

    public void notifyDexLoad(ApplicationInfo loadingAppInfo, List<String> classLoadersNames, List<String> classPaths, String loaderIsa, int loaderUserId) {
        try {
            notifyDexLoadInternal(loadingAppInfo, classLoadersNames, classPaths, loaderIsa, loaderUserId);
        } catch (Exception e) {
            Slog.w(TAG, "Exception while notifying dex load for package " + loadingAppInfo.packageName, e);
        }
    }

    private void notifyDexLoadInternal(ApplicationInfo loadingAppInfo, List<String> classLoaderNames, List<String> classPaths, String loaderIsa, int loaderUserId) {
        if (classLoaderNames.size() != classPaths.size()) {
            Slog.wtf(TAG, "Bad call to noitfyDexLoad: args have different size");
        } else if (classLoaderNames.isEmpty()) {
            Slog.wtf(TAG, "Bad call to notifyDexLoad: class loaders list is empty");
        } else if (PackageManagerServiceUtils.checkISA(loaderIsa)) {
            String[] dexPathsToRegister = ((String) classPaths.get(0)).split(File.pathSeparator);
            String[] classLoaderContexts = DexoptUtils.processContextForDexLoad(classLoaderNames, classPaths);
            int dexPathIndex = 0;
            for (String dexPath : dexPathsToRegister) {
                DexSearchResult searchResult = getDexPackage(loadingAppInfo, dexPath, loaderUserId);
                if (searchResult.mOutcome != DEX_SEARCH_NOT_FOUND) {
                    boolean isUsedByOtherApps = loadingAppInfo.packageName.equals(searchResult.mOwningPackageName) ^ 1;
                    boolean primaryOrSplit = searchResult.mOutcome != DEX_SEARCH_FOUND_PRIMARY ? searchResult.mOutcome == DEX_SEARCH_FOUND_SPLIT : true;
                    if (!primaryOrSplit || (isUsedByOtherApps ^ 1) == 0) {
                        String classLoaderContext;
                        if (classLoaderContexts == null) {
                            classLoaderContext = "=UnsupportedClassLoaderContext=";
                        } else {
                            classLoaderContext = classLoaderContexts[dexPathIndex];
                        }
                        if (this.mPackageDexUsage.record(searchResult.mOwningPackageName, dexPath, loaderUserId, loaderIsa, isUsedByOtherApps, primaryOrSplit, loadingAppInfo.packageName, classLoaderContext)) {
                            this.mPackageDexUsage.maybeWriteAsync();
                        }
                    } else {
                    }
                }
                dexPathIndex++;
            }
        } else {
            Slog.w(TAG, "Loading dex files " + classPaths + " in unsupported ISA: " + loaderIsa + "?");
        }
    }

    public void load(Map<Integer, List<PackageInfo>> existingPackages) {
        try {
            loadInternal(existingPackages);
        } catch (Exception e) {
            this.mPackageDexUsage.clear();
            Slog.w(TAG, "Exception while loading package dex usage. Starting with a fresh state.", e);
        }
    }

    public void notifyPackageInstalled(PackageInfo pi, int userId) {
        if (userId == -1) {
            throw new IllegalArgumentException("notifyPackageInstalled called with USER_ALL");
        } else if (pi == null) {
            Slog.i(TAG, "notifyPackageInstalled-> pi is null! return directly");
        } else {
            cachePackageInfo(pi, userId);
        }
    }

    public void notifyPackageUpdated(String packageName, String baseCodePath, String[] splitCodePaths) {
        cachePackageCodeLocation(packageName, baseCodePath, splitCodePaths, null, -1);
        if (this.mPackageDexUsage.clearUsedByOtherApps(packageName)) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
    }

    public void notifyPackageDataDestroyed(String packageName, int userId) {
        boolean updated;
        if (userId == -1) {
            updated = this.mPackageDexUsage.removePackage(packageName);
        } else {
            updated = this.mPackageDexUsage.removeUserPackage(packageName, userId);
        }
        if (updated) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
    }

    private void cachePackageInfo(PackageInfo pi, int userId) {
        ApplicationInfo ai = pi.applicationInfo;
        cachePackageCodeLocation(pi.packageName, ai.sourceDir, ai.splitSourceDirs, new String[]{ai.dataDir, ai.deviceProtectedDataDir, ai.credentialProtectedDataDir}, userId);
    }

    private void cachePackageCodeLocation(String packageName, String baseCodePath, String[] splitCodePaths, String[] dataDirs, int userId) {
        synchronized (this.mPackageCodeLocationsCache) {
            PackageCodeLocations pcl = (PackageCodeLocations) putIfAbsent(this.mPackageCodeLocationsCache, packageName, new PackageCodeLocations(packageName, baseCodePath, splitCodePaths));
            pcl.updateCodeLocation(baseCodePath, splitCodePaths);
            if (dataDirs != null) {
                for (String dataDir : dataDirs) {
                    if (dataDir != null) {
                        pcl.mergeAppDataDirs(dataDir, userId);
                    }
                }
            }
        }
    }

    private void loadInternal(Map<Integer, List<PackageInfo>> existingPackages) {
        Map<String, Set<Integer>> packageToUsersMap = new HashMap();
        Map<String, Set<String>> packageToCodePaths = new HashMap();
        for (Entry<Integer, List<PackageInfo>> entry : existingPackages.entrySet()) {
            List<PackageInfo> packageInfoList = (List) entry.getValue();
            int userId = ((Integer) entry.getKey()).intValue();
            for (PackageInfo pi : packageInfoList) {
                cachePackageInfo(pi, userId);
                ((Set) putIfAbsent(packageToUsersMap, pi.packageName, new HashSet())).add(Integer.valueOf(userId));
                Set<String> codePaths = (Set) putIfAbsent(packageToCodePaths, pi.packageName, new HashSet());
                codePaths.add(pi.applicationInfo.sourceDir);
                if (pi.applicationInfo.splitSourceDirs != null) {
                    Collections.addAll(codePaths, pi.applicationInfo.splitSourceDirs);
                }
            }
        }
        this.mPackageDexUsage.read();
        this.mPackageDexUsage.syncData(packageToUsersMap, packageToCodePaths);
    }

    public PackageUseInfo getPackageUseInfoOrDefault(String packageName) {
        PackageUseInfo useInfo = this.mPackageDexUsage.getPackageUseInfo(packageName);
        return useInfo == null ? DEFAULT_USE_INFO : useInfo;
    }

    boolean hasInfoOnPackage(String packageName) {
        return this.mPackageDexUsage.getPackageUseInfo(packageName) != null;
    }

    public boolean dexoptSecondaryDex(DexoptOptions options) {
        PackageDexOptimizer pdo;
        if (options.isForce()) {
            pdo = new ForcedUpdatePackageDexOptimizer(this.mPackageDexOptimizer);
        } else {
            pdo = this.mPackageDexOptimizer;
        }
        String packageName = options.getPackageName();
        PackageUseInfo useInfo = getPackageUseInfoOrDefault(packageName);
        if (useInfo == null || useInfo.getDexUseInfoMap().isEmpty()) {
            return true;
        }
        boolean success = true;
        for (Entry<String, DexUseInfo> entry : useInfo.getDexUseInfoMap().entrySet()) {
            String dexPath = (String) entry.getKey();
            DexUseInfo dexUseInfo = (DexUseInfo) entry.getValue();
            try {
                PackageInfo pkg = this.mPackageManager.getPackageInfo(packageName, 0, dexUseInfo.getOwnerUserId());
                if (pkg == null) {
                    Slog.d(TAG, "Could not find package when compiling secondary dex " + packageName + " for user " + dexUseInfo.getOwnerUserId());
                    this.mPackageDexUsage.removeUserPackage(packageName, dexUseInfo.getOwnerUserId());
                } else {
                    success = success && pdo.dexOptSecondaryDexPath(pkg.applicationInfo, dexPath, dexUseInfo, options) != -1;
                }
            } catch (RemoteException e) {
                throw new AssertionError(e);
            }
        }
        return success;
    }

    public void reconcileSecondaryDexFiles(String packageName) {
        PackageUseInfo useInfo = getPackageUseInfoOrDefault(packageName);
        if (useInfo != null && !useInfo.getDexUseInfoMap().isEmpty()) {
            boolean updated = false;
            for (Entry<String, DexUseInfo> entry : useInfo.getDexUseInfoMap().entrySet()) {
                String dexPath = (String) entry.getKey();
                DexUseInfo dexUseInfo = (DexUseInfo) entry.getValue();
                PackageInfo pkg = null;
                try {
                    pkg = this.mPackageManager.getPackageInfo(packageName, 0, dexUseInfo.getOwnerUserId());
                } catch (RemoteException e) {
                }
                if (pkg == null) {
                    Slog.d(TAG, "Could not find package when compiling secondary dex " + packageName + " for user " + dexUseInfo.getOwnerUserId());
                    if (this.mPackageDexUsage.removeUserPackage(packageName, dexUseInfo.getOwnerUserId())) {
                        updated = true;
                    }
                } else {
                    int flags;
                    ApplicationInfo info = pkg.applicationInfo;
                    if (info.deviceProtectedDataDir != null && FileUtils.contains(info.deviceProtectedDataDir, dexPath)) {
                        flags = 1;
                    } else if (info.credentialProtectedDataDir == null || !FileUtils.contains(info.credentialProtectedDataDir, dexPath)) {
                        Slog.e(TAG, "Could not infer CE/DE storage for path " + dexPath);
                        if (this.mPackageDexUsage.removeDexFile(packageName, dexPath, dexUseInfo.getOwnerUserId())) {
                            updated = true;
                        }
                    } else {
                        flags = 2;
                    }
                    boolean dexStillExists = true;
                    synchronized (this.mInstallLock) {
                        try {
                            String[] isas = (String[]) dexUseInfo.getLoaderIsas().toArray(new String[0]);
                            dexStillExists = this.mInstaller.reconcileSecondaryDexFile(dexPath, packageName, pkg.applicationInfo.uid, isas, pkg.applicationInfo.volumeUuid, flags);
                        } catch (InstallerException e2) {
                            Slog.e(TAG, "Got InstallerException when reconciling dex " + dexPath + " : " + e2.getMessage());
                        }
                    }
                    if (!dexStillExists) {
                        if (this.mPackageDexUsage.removeDexFile(packageName, dexPath, dexUseInfo.getOwnerUserId())) {
                            updated = true;
                        }
                    }
                }
            }
            if (updated) {
                this.mPackageDexUsage.maybeWriteAsync();
            }
        }
    }

    public RegisterDexModuleResult registerDexModule(ApplicationInfo info, String dexPath, boolean isUsedByOtherApps, int userId) {
        DexSearchResult searchResult = getDexPackage(info, dexPath, userId);
        if (searchResult.mOutcome == DEX_SEARCH_NOT_FOUND) {
            return new RegisterDexModuleResult(false, "Package not found");
        }
        if (!info.packageName.equals(searchResult.mOwningPackageName)) {
            return new RegisterDexModuleResult(false, "Dex path does not belong to package");
        }
        if (searchResult.mOutcome == DEX_SEARCH_FOUND_PRIMARY || searchResult.mOutcome == DEX_SEARCH_FOUND_SPLIT) {
            return new RegisterDexModuleResult(false, "Main apks cannot be registered");
        }
        boolean update = false;
        for (String isa : InstructionSets.getAppDexInstructionSets(info)) {
            update |= this.mPackageDexUsage.record(searchResult.mOwningPackageName, dexPath, userId, isa, isUsedByOtherApps, false, searchResult.mOwningPackageName, "=UnknownClassLoaderContext=");
        }
        if (update) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
        if (this.mPackageDexOptimizer.dexOptSecondaryDexPath(info, dexPath, (DexUseInfo) this.mPackageDexUsage.getPackageUseInfo(searchResult.mOwningPackageName).getDexUseInfoMap().get(dexPath), new DexoptOptions(info.packageName, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(2), 0)) != -1) {
            Slog.e(TAG, "Failed to optimize dex module " + dexPath);
        }
        return new RegisterDexModuleResult(true, "Dex module registered successfully");
    }

    public Set<String> getAllPackagesWithSecondaryDexFiles() {
        return this.mPackageDexUsage.getAllPackagesWithSecondaryDexFiles();
    }

    private DexSearchResult getDexPackage(ApplicationInfo loadingAppInfo, String dexPath, int userId) {
        if (dexPath.startsWith("/system/framework/")) {
            return new DexSearchResult("framework", DEX_SEARCH_NOT_FOUND);
        }
        PackageCodeLocations loadingPackageCodeLocations = new PackageCodeLocations(loadingAppInfo, userId);
        int outcome = loadingPackageCodeLocations.searchDex(dexPath, userId);
        if (outcome != DEX_SEARCH_NOT_FOUND) {
            return new DexSearchResult(loadingPackageCodeLocations.mPackageName, outcome);
        }
        synchronized (this.mPackageCodeLocationsCache) {
            for (PackageCodeLocations pcl : this.mPackageCodeLocationsCache.values()) {
                outcome = pcl.searchDex(dexPath, userId);
                if (outcome != DEX_SEARCH_NOT_FOUND) {
                    DexSearchResult dexSearchResult = new DexSearchResult(pcl.mPackageName, outcome);
                    return dexSearchResult;
                }
            }
            return new DexSearchResult(null, DEX_SEARCH_NOT_FOUND);
        }
    }

    private static <K, V> V putIfAbsent(Map<K, V> map, K key, V newValue) {
        V existingValue = map.putIfAbsent(key, newValue);
        return existingValue == null ? newValue : existingValue;
    }

    public void writePackageDexUsageNow() {
        this.mPackageDexUsage.writeNow();
    }
}
