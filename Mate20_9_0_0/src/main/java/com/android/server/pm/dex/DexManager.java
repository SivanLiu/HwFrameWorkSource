package com.android.server.pm.dex;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser.Package;
import android.database.ContentObserver;
import android.os.Build;
import android.os.FileUtils;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.Slog;
import android.util.jar.StrictJarFile;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.server.pm.Installer;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageDexOptimizer;
import com.android.server.pm.PackageDexOptimizer.ForcedUpdatePackageDexOptimizer;
import com.android.server.pm.PackageManagerServiceUtils;
import com.android.server.pm.dex.PackageDexUsage.DexUseInfo;
import com.android.server.pm.dex.PackageDexUsage.PackageUseInfo;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;

public class DexManager {
    private static final boolean DEBUG = false;
    private static final PackageUseInfo DEFAULT_USE_INFO = new PackageUseInfo();
    private static int DEX_SEARCH_FOUND_PRIMARY = 1;
    private static int DEX_SEARCH_FOUND_SECONDARY = 3;
    private static int DEX_SEARCH_FOUND_SPLIT = 2;
    private static int DEX_SEARCH_NOT_FOUND = 0;
    private static final String PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB = "pm.dexopt.priv-apps-oob";
    private static final String PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST = "pm.dexopt.priv-apps-oob-list";
    private static final String TAG = "DexManager";
    private final Context mContext;
    private final Object mInstallLock;
    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    private final Listener mListener;
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mOwningPackageName);
            stringBuilder.append("-");
            stringBuilder.append(this.mOutcome);
            return stringBuilder.toString();
        }
    }

    public interface Listener {
        void onReconcileSecondaryDexFile(ApplicationInfo applicationInfo, DexUseInfo dexUseInfo, String str, int i);
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

    public DexManager(Context context, IPackageManager pms, PackageDexOptimizer pdo, Installer installer, Object installLock, Listener listener) {
        this.mContext = context;
        this.mPackageManager = pms;
        this.mPackageDexOptimizer = pdo;
        this.mInstaller = installer;
        this.mInstallLock = installLock;
        this.mListener = listener;
    }

    public void systemReady() {
        registerSettingObserver();
    }

    public void notifyDexLoad(ApplicationInfo loadingAppInfo, List<String> classLoadersNames, List<String> classPaths, String loaderIsa, int loaderUserId) {
        try {
            notifyDexLoadInternal(loadingAppInfo, classLoadersNames, classPaths, loaderIsa, loaderUserId);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception while notifying dex load for package ");
            stringBuilder.append(loadingAppInfo.packageName);
            Slog.w(str, stringBuilder.toString(), e);
        }
    }

    private void notifyDexLoadInternal(ApplicationInfo loadingAppInfo, List<String> classLoaderNames, List<String> classPaths, String loaderIsa, int loaderUserId) {
        ApplicationInfo applicationInfo = loadingAppInfo;
        List<String> list = classPaths;
        if (classLoaderNames.size() != classPaths.size()) {
            Slog.wtf(TAG, "Bad call to noitfyDexLoad: args have different size");
        } else if (classLoaderNames.isEmpty()) {
            Slog.wtf(TAG, "Bad call to notifyDexLoad: class loaders list is empty");
        } else if (PackageManagerServiceUtils.checkISA(loaderIsa)) {
            String str = loaderIsa;
            String[] dexPathsToRegister = ((String) list.get(0)).split(File.pathSeparator);
            String[] classLoaderContexts = DexoptUtils.processContextForDexLoad(classLoaderNames, classPaths);
            int length = dexPathsToRegister.length;
            int i = 0;
            int dexPathIndex = 0;
            while (i < length) {
                int i2;
                int i3;
                String dexPath = dexPathsToRegister[i];
                int i4 = loaderUserId;
                DexSearchResult searchResult = getDexPackage(applicationInfo, dexPath, i4);
                if (searchResult.mOutcome != DEX_SEARCH_NOT_FOUND) {
                    boolean z = true;
                    boolean isUsedByOtherApps = applicationInfo.packageName.equals(searchResult.mOwningPackageName) ^ 1;
                    if (!(searchResult.mOutcome == DEX_SEARCH_FOUND_PRIMARY || searchResult.mOutcome == DEX_SEARCH_FOUND_SPLIT)) {
                        z = false;
                    }
                    boolean primaryOrSplit = z;
                    if (!primaryOrSplit || isUsedByOtherApps) {
                        String str2;
                        if (classLoaderContexts == null) {
                            str2 = "=UnsupportedClassLoaderContext=";
                        } else {
                            str2 = classLoaderContexts[dexPathIndex];
                        }
                        String classLoaderContext = str2;
                        i2 = i;
                        i3 = length;
                        if (this.mPackageDexUsage.record(searchResult.mOwningPackageName, dexPath, i4, str, isUsedByOtherApps, primaryOrSplit, applicationInfo.packageName, classLoaderContext)) {
                            this.mPackageDexUsage.maybeWriteAsync();
                        }
                    } else {
                        i2 = i;
                        i3 = length;
                        i = i2 + 1;
                        length = i3;
                    }
                } else {
                    String str3 = dexPath;
                    i2 = i;
                    i3 = length;
                }
                dexPathIndex++;
                i = i2 + 1;
                length = i3;
            }
        } else {
            String str4 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Loading dex files ");
            stringBuilder.append(list);
            stringBuilder.append(" in unsupported ISA: ");
            stringBuilder.append(loaderIsa);
            stringBuilder.append("?");
            Slog.w(str4, stringBuilder.toString());
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
        if (useInfo.getDexUseInfoMap().isEmpty()) {
            return true;
        }
        boolean success = true;
        for (Entry<String, DexUseInfo> entry : useInfo.getDexUseInfoMap().entrySet()) {
            String dexPath = (String) entry.getKey();
            DexUseInfo dexUseInfo = (DexUseInfo) entry.getValue();
            try {
                boolean z = false;
                PackageInfo pkg = this.mPackageManager.getPackageInfo(packageName, 0, dexUseInfo.getOwnerUserId());
                if (pkg == null) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not find package when compiling secondary dex ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" for user ");
                    stringBuilder.append(dexUseInfo.getOwnerUserId());
                    Slog.d(str, stringBuilder.toString());
                    this.mPackageDexUsage.removeUserPackage(packageName, dexUseInfo.getOwnerUserId());
                } else {
                    int result = pdo.dexOptSecondaryDexPath(pkg.applicationInfo, dexPath, dexUseInfo, options);
                    if (success && result != -1) {
                        z = true;
                    }
                    success = z;
                }
            } catch (RemoteException e) {
                throw new AssertionError(e);
            }
        }
        return success;
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x012b  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x012b  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00f1 A:{Splitter: B:34:0x00b8, ExcHandler: all (th java.lang.Throwable)} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x012b  */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:43:0x00e5, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:44:0x00e6, code:
            r19 = r3;
            r20 = r4;
            r21 = r5;
            r22 = r7;
            r18 = false;
     */
    /* JADX WARNING: Missing block: B:45:0x00f1, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:46:0x00f2, code:
            r19 = r3;
            r20 = r4;
            r21 = r5;
            r22 = r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reconcileSecondaryDexFiles(String packageName) {
        InstallerException e;
        String str = packageName;
        PackageUseInfo useInfo = getPackageUseInfoOrDefault(packageName);
        if (!useInfo.getDexUseInfoMap().isEmpty()) {
            boolean updated = false;
            for (Entry<String, DexUseInfo> entry : useInfo.getDexUseInfoMap().entrySet()) {
                String dexPath = (String) entry.getKey();
                DexUseInfo dexUseInfo = (DexUseInfo) entry.getValue();
                PackageInfo pkg = null;
                try {
                    pkg = this.mPackageManager.getPackageInfo(str, 0, dexUseInfo.getOwnerUserId());
                } catch (RemoteException e2) {
                }
                PackageInfo pkg2 = pkg;
                boolean z = true;
                if (pkg2 == null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not find package when compiling secondary dex ");
                    stringBuilder.append(str);
                    stringBuilder.append(" for user ");
                    stringBuilder.append(dexUseInfo.getOwnerUserId());
                    Slog.d(str2, stringBuilder.toString());
                    if (!(this.mPackageDexUsage.removeUserPackage(str, dexUseInfo.getOwnerUserId()) || updated)) {
                        z = false;
                    }
                    updated = z;
                } else {
                    int flags;
                    PackageInfo packageInfo;
                    boolean z2;
                    String str3;
                    StringBuilder stringBuilder2;
                    ApplicationInfo info = pkg2.applicationInfo;
                    if (info.deviceProtectedDataDir != null && FileUtils.contains(info.deviceProtectedDataDir, dexPath)) {
                        flags = 0 | 1;
                    } else if (info.credentialProtectedDataDir == null || !FileUtils.contains(info.credentialProtectedDataDir, dexPath)) {
                        packageInfo = pkg2;
                        z2 = false;
                        str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Could not infer CE/DE storage for path ");
                        stringBuilder2.append(dexPath);
                        Slog.e(str3, stringBuilder2.toString());
                        if (!(this.mPackageDexUsage.removeDexFile(str, dexPath, dexUseInfo.getOwnerUserId()) || updated)) {
                            z = z2;
                        }
                        updated = z;
                    } else {
                        flags = 0 | 2;
                    }
                    int flags2 = flags;
                    if (this.mListener != null) {
                        this.mListener.onReconcileSecondaryDexFile(info, dexUseInfo, dexPath, flags2);
                    }
                    boolean dexStillExists = true;
                    Object obj = this.mInstallLock;
                    synchronized (obj) {
                        Object obj2;
                        try {
                            String[] isas = (String[]) dexUseInfo.getLoaderIsas().toArray(new String[0]);
                            obj2 = obj;
                            z2 = false;
                            try {
                                dexStillExists = this.mInstaller.reconcileSecondaryDexFile(dexPath, str, info.uid, isas, info.volumeUuid, flags2);
                            } catch (InstallerException e3) {
                                e = e3;
                                try {
                                    str3 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Got InstallerException when reconciling dex ");
                                    stringBuilder2.append(dexPath);
                                    stringBuilder2.append(" : ");
                                    stringBuilder2.append(e.getMessage());
                                    Slog.e(str3, stringBuilder2.toString());
                                    if (!dexStillExists) {
                                    }
                                } catch (Throwable th) {
                                    flags = th;
                                    throw flags;
                                }
                            }
                        } catch (InstallerException e4) {
                            e = e4;
                            obj2 = obj;
                            int i = flags2;
                            ApplicationInfo applicationInfo = info;
                            packageInfo = pkg2;
                            z2 = false;
                            str3 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Got InstallerException when reconciling dex ");
                            stringBuilder2.append(dexPath);
                            stringBuilder2.append(" : ");
                            stringBuilder2.append(e.getMessage());
                            Slog.e(str3, stringBuilder2.toString());
                            if (dexStillExists) {
                            }
                        } catch (Throwable th2) {
                        }
                        if (dexStillExists) {
                            if (!(this.mPackageDexUsage.removeDexFile(str, dexPath, dexUseInfo.getOwnerUserId()) || updated)) {
                                z = z2;
                            }
                            updated = z;
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
        ApplicationInfo applicationInfo = info;
        String str = dexPath;
        int i = userId;
        DexSearchResult searchResult = getDexPackage(applicationInfo, str, i);
        if (searchResult.mOutcome == DEX_SEARCH_NOT_FOUND) {
            return new RegisterDexModuleResult(false, "Package not found");
        }
        if (!applicationInfo.packageName.equals(searchResult.mOwningPackageName)) {
            return new RegisterDexModuleResult(false, "Dex path does not belong to package");
        }
        if (searchResult.mOutcome == DEX_SEARCH_FOUND_PRIMARY || searchResult.mOutcome == DEX_SEARCH_FOUND_SPLIT) {
            return new RegisterDexModuleResult(false, "Main apks cannot be registered");
        }
        String[] appDexInstructionSets = InstructionSets.getAppDexInstructionSets(info);
        boolean update = false;
        int i2 = 0;
        for (int length = appDexInstructionSets.length; i2 < length; length = length) {
            String isa = appDexInstructionSets[i2];
            int i3 = i2;
            update |= this.mPackageDexUsage.record(searchResult.mOwningPackageName, str, i, isa, isUsedByOtherApps, false, searchResult.mOwningPackageName, "=UnknownClassLoaderContext=");
            i2 = i3 + 1;
        }
        if (update) {
            this.mPackageDexUsage.maybeWriteAsync();
        }
        if (this.mPackageDexOptimizer.dexOptSecondaryDexPath(applicationInfo, str, (DexUseInfo) this.mPackageDexUsage.getPackageUseInfo(searchResult.mOwningPackageName).getDexUseInfoMap().get(str), new DexoptOptions(applicationInfo.packageName, 2, 0)) != -1) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to optimize dex module ");
            stringBuilder.append(str);
            Slog.e(str2, stringBuilder.toString());
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

    private void registerSettingObserver() {
        final ContentResolver resolver = this.mContext.getContentResolver();
        ContentObserver privAppOobObserver = new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                SystemProperties.set(DexManager.PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB, Global.getInt(resolver, "priv_app_oob_enabled", 0) == 1 ? "true" : "false");
            }
        };
        resolver.registerContentObserver(Global.getUriFor("priv_app_oob_enabled"), false, privAppOobObserver, 0);
        privAppOobObserver.onChange(true);
        ContentObserver privAppOobListObserver = new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                String oobList = Global.getString(resolver, "priv_app_oob_list");
                if (oobList == null) {
                    oobList = "ALL";
                }
                SystemProperties.set(DexManager.PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST, oobList);
            }
        };
        resolver.registerContentObserver(Global.getUriFor("priv_app_oob_list"), false, privAppOobListObserver, 0);
        privAppOobListObserver.onChange(true);
    }

    public static boolean isPackageSelectedToRunOob(String packageName) {
        return isPackageSelectedToRunOob(Arrays.asList(new String[]{packageName}));
    }

    public static boolean isPackageSelectedToRunOob(Collection<String> packageNamesInSameProcess) {
        if (!SystemProperties.getBoolean(PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB, false)) {
            return false;
        }
        String oobListProperty = SystemProperties.get(PROPERTY_NAME_PM_DEXOPT_PRIV_APPS_OOB_LIST, "ALL");
        if ("ALL".equals(oobListProperty)) {
            return true;
        }
        for (String oobPkgName : oobListProperty.split(",")) {
            if (packageNamesInSameProcess.contains(oobPkgName)) {
                return true;
            }
        }
        return false;
    }

    public static void maybeLogUnexpectedPackageDetails(Package pkg) {
        if (Build.IS_DEBUGGABLE && pkg.isPrivileged() && isPackageSelectedToRunOob(pkg.packageName)) {
            logIfPackageHasUncompressedCode(pkg);
        }
    }

    private static void logIfPackageHasUncompressedCode(Package pkg) {
        logIfApkHasUncompressedCode(pkg.baseCodePath);
        if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
            for (String logIfApkHasUncompressedCode : pkg.splitCodePaths) {
                logIfApkHasUncompressedCode(logIfApkHasUncompressedCode);
            }
        }
    }

    private static void logIfApkHasUncompressedCode(String fileName) {
        StrictJarFile jarFile = null;
        try {
            jarFile = new StrictJarFile(fileName, false, false);
            Iterator<ZipEntry> it = jarFile.iterator();
            while (it.hasNext()) {
                ZipEntry entry = (ZipEntry) it.next();
                String str;
                StringBuilder stringBuilder;
                if (entry.getName().endsWith(".dex")) {
                    if (entry.getMethod() != 0) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("APK ");
                        stringBuilder.append(fileName);
                        stringBuilder.append(" has compressed dex code ");
                        stringBuilder.append(entry.getName());
                        Slog.w(str, stringBuilder.toString());
                    } else if ((entry.getDataOffset() & 3) != 0) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("APK ");
                        stringBuilder.append(fileName);
                        stringBuilder.append(" has unaligned dex code ");
                        stringBuilder.append(entry.getName());
                        Slog.w(str, stringBuilder.toString());
                    }
                } else if (entry.getName().endsWith(".so")) {
                    if (entry.getMethod() != 0) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("APK ");
                        stringBuilder.append(fileName);
                        stringBuilder.append(" has compressed native code ");
                        stringBuilder.append(entry.getName());
                        Slog.w(str, stringBuilder.toString());
                    } else if ((entry.getDataOffset() & 4095) != 0) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("APK ");
                        stringBuilder.append(fileName);
                        stringBuilder.append(" has unaligned native code ");
                        stringBuilder.append(entry.getName());
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            }
            try {
                jarFile.close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Error when parsing APK ");
            stringBuilder2.append(fileName);
            Slog.wtf(str2, stringBuilder2.toString());
            if (jarFile != null) {
                jarFile.close();
            }
        } catch (Throwable th) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e3) {
                }
            }
        }
    }
}
