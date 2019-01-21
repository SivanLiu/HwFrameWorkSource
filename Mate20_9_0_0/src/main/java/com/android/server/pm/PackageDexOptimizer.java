package com.android.server.pm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser.Package;
import android.content.pm.dex.ArtManager;
import android.content.pm.dex.DexMetadataHelper;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.DexoptUtils;
import com.android.server.pm.dex.PackageDexUsage.DexUseInfo;
import com.android.server.pm.dex.PackageDexUsage.PackageUseInfo;
import dalvik.system.DexFile;
import dalvik.system.DexFile.OptimizationInfo;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipFile;

public class PackageDexOptimizer {
    public static final int DEX_OPT_FAILED = -1;
    public static final int DEX_OPT_PERFORMED = 1;
    public static final int DEX_OPT_SKIPPED = 0;
    static final String OAT_DIR_NAME = "oat";
    public static final String SKIP_SHARED_LIBRARY_CHECK = "&";
    private static final String TAG = "PackageManager.DexOptimizer";
    private static final long WAKELOCK_TIMEOUT_MS = (PackageManagerService.WATCHDOG_TIMEOUT + 60000);
    private long mDexOptTotalTime = 0;
    @GuardedBy("mInstallLock")
    private final WakeLock mDexoptWakeLock;
    private final Object mInstallLock;
    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    ArrayList<String> mPatchoatNeededApps = new ArrayList();
    private volatile boolean mSystemReady;

    public static class ForcedUpdatePackageDexOptimizer extends PackageDexOptimizer {
        public ForcedUpdatePackageDexOptimizer(Installer installer, Object installLock, Context context, String wakeLockTag) {
            super(installer, installLock, context, wakeLockTag);
        }

        public ForcedUpdatePackageDexOptimizer(PackageDexOptimizer from) {
            super(from);
        }

        protected int adjustDexoptNeeded(int dexoptNeeded) {
            if (dexoptNeeded == 0) {
                return -3;
            }
            return dexoptNeeded;
        }

        protected int adjustDexoptFlags(int flags) {
            return flags | 64;
        }
    }

    PackageDexOptimizer(Installer installer, Object installLock, Context context, String wakeLockTag) {
        this.mInstaller = installer;
        this.mInstallLock = installLock;
        this.mDexoptWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, wakeLockTag);
    }

    protected PackageDexOptimizer(PackageDexOptimizer from) {
        this.mInstaller = from.mInstaller;
        this.mInstallLock = from.mInstallLock;
        this.mDexoptWakeLock = from.mDexoptWakeLock;
        this.mSystemReady = from.mSystemReady;
    }

    static boolean canOptimizePackage(Package pkg) {
        if ((pkg.applicationInfo.flags & 4) == 0) {
            return false;
        }
        return true;
    }

    int performDexOpt(Package pkg, String[] sharedLibraries, String[] instructionSets, PackageStats packageStats, PackageUseInfo packageUseInfo, DexoptOptions options) {
        if (pkg.applicationInfo.uid == -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Dexopt for ");
            stringBuilder.append(pkg.packageName);
            stringBuilder.append(" has invalid uid.");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!canOptimizePackage(pkg)) {
            return 0;
        } else {
            int performDexOptLI;
            synchronized (this.mInstallLock) {
                long acquireTime = acquireWakeLockLI(pkg.applicationInfo.uid);
                try {
                    performDexOptLI = performDexOptLI(pkg, sharedLibraries, instructionSets, packageStats, packageUseInfo, options);
                } finally {
                    releaseWakeLockLI(acquireTime);
                }
            }
            return performDexOptLI;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:65:0x0186  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mInstallLock")
    private int performDexOptLI(Package pkg, String[] sharedLibraries, String[] targetInstructionSets, PackageStats packageStats, PackageUseInfo packageUseInfo, DexoptOptions options) {
        StringBuilder stringBuilder;
        PackageDexOptimizer i = this;
        Package packageR = pkg;
        String[] instructionSets = targetInstructionSets != null ? targetInstructionSets : InstructionSets.getAppDexInstructionSets(packageR.applicationInfo);
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
        List<String> paths = pkg.getAllCodePaths();
        int sharedGid = UserHandle.getSharedAppGid(packageR.applicationInfo.uid);
        Object obj = -1;
        if (sharedGid == -1) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Well this is awkward; package ");
            stringBuilder.append(packageR.applicationInfo.name);
            stringBuilder.append(" had UID ");
            stringBuilder.append(packageR.applicationInfo.uid);
            Slog.wtf(str, stringBuilder.toString(), new Throwable());
            sharedGid = 9999;
        }
        int sharedGid2 = sharedGid;
        boolean[] pathsWithCode = new boolean[paths.size()];
        pathsWithCode[0] = (packageR.applicationInfo.flags & 4) != 0;
        for (sharedGid = 1; sharedGid < paths.size(); sharedGid++) {
            pathsWithCode[sharedGid] = (packageR.splitFlags[sharedGid + -1] & 4) != 0;
        }
        String[] classLoaderContexts = DexoptUtils.getClassLoaderContexts(packageR.applicationInfo, sharedLibraries, pathsWithCode);
        if (paths.size() != classLoaderContexts.length) {
            String[] splitCodePaths = packageR.applicationInfo.getSplitCodePaths();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Inconsistent information between PackageParser.Package and its ApplicationInfo. pkg.getAllCodePaths=");
            stringBuilder.append(paths);
            stringBuilder.append(" pkg.applicationInfo.getBaseCodePath=");
            stringBuilder.append(packageR.applicationInfo.getBaseCodePath());
            stringBuilder.append(" pkg.applicationInfo.getSplitCodePaths=");
            stringBuilder.append(splitCodePaths == null ? "null" : Arrays.toString(splitCodePaths));
            throw new IllegalStateException(stringBuilder.toString());
        }
        int result = 0;
        sharedGid = 0;
        while (true) {
            int i2 = sharedGid;
            boolean[] pathsWithCode2;
            int sharedGid3;
            String[] dexCodeInstructionSets2;
            String[] instructionSets2;
            if (i2 < paths.size()) {
                int i3;
                String[] classLoaderContexts2;
                List<String> paths2;
                String[] strArr;
                Object obj2;
                if (pathsWithCode[i2]) {
                    if (classLoaderContexts[i2] != null) {
                        String path = (String) paths.get(i2);
                        if (options.getSplitName() == null || options.getSplitName().equals(new File(path).getName())) {
                            String dexMetadataPath;
                            boolean isUsedByOtherApps;
                            String compilerFilter;
                            boolean z;
                            int dexoptFlags;
                            boolean profileUpdated;
                            int profileUpdated2;
                            int result2;
                            String profileName = ArtManager.getProfileName(i2 == 0 ? null : packageR.splitNames[i2 - 1]);
                            if (options.isDexoptInstallWithDexMetadata()) {
                                File dexMetadataFile = DexMetadataHelper.findDexMetadataForFile(new File(path));
                                dexMetadataPath = dexMetadataFile == null ? null : dexMetadataFile.getAbsolutePath();
                            } else {
                                dexMetadataPath = null;
                            }
                            if (options.isDexoptAsSharedLibrary()) {
                                PackageUseInfo packageUseInfo2 = packageUseInfo;
                            } else if (!packageUseInfo.isUsedByOtherApps(path)) {
                                isUsedByOtherApps = false;
                                compilerFilter = i.getRealCompilerFilter(packageR.applicationInfo, options.getCompilerFilter(), isUsedByOtherApps);
                                z = options.isCheckForProfileUpdates() && i.isProfileUpdated(packageR, sharedGid2, profileName, compilerFilter);
                                isUsedByOtherApps = z;
                                dexoptFlags = i.getDexFlags(packageR, compilerFilter, options);
                                profileUpdated = isUsedByOtherApps;
                                profileUpdated2 = dexCodeInstructionSets.length;
                                result2 = result;
                                result = 0;
                                while (result < profileUpdated2) {
                                    int i4 = result;
                                    String profileName2 = profileName;
                                    String path2 = path;
                                    i3 = i2;
                                    classLoaderContexts2 = classLoaderContexts;
                                    pathsWithCode2 = pathsWithCode;
                                    sharedGid3 = sharedGid2;
                                    String compilerFilter2 = compilerFilter;
                                    paths2 = paths;
                                    dexCodeInstructionSets2 = dexCodeInstructionSets;
                                    instructionSets2 = instructionSets;
                                    boolean z2 = profileUpdated;
                                    int i5 = profileUpdated2;
                                    isUsedByOtherApps = z2;
                                    sharedGid = i.dexOptPath(packageR, path, dexCodeInstructionSets[result], compilerFilter, isUsedByOtherApps, classLoaderContexts[i2], dexoptFlags, sharedGid2, packageStats, options.isDowngrade(), profileName2, dexMetadataPath, options.getCompilationReason());
                                    result = result2;
                                    result2 = (result == -1 || sharedGid == 0) ? result : sharedGid;
                                    result = i4 + 1;
                                    strArr = sharedLibraries;
                                    DexoptOptions dexoptOptions = options;
                                    i2 = i3;
                                    compilerFilter = compilerFilter2;
                                    pathsWithCode = pathsWithCode2;
                                    dexCodeInstructionSets = dexCodeInstructionSets2;
                                    profileName = profileName2;
                                    path = path2;
                                    classLoaderContexts = classLoaderContexts2;
                                    sharedGid2 = sharedGid3;
                                    paths = paths2;
                                    instructionSets = instructionSets2;
                                    i = this;
                                    packageR = pkg;
                                    int i6 = i5;
                                    profileUpdated = isUsedByOtherApps;
                                    profileUpdated2 = i6;
                                }
                                i3 = i2;
                                classLoaderContexts2 = classLoaderContexts;
                                pathsWithCode2 = pathsWithCode;
                                sharedGid3 = sharedGid2;
                                paths2 = paths;
                                dexCodeInstructionSets2 = dexCodeInstructionSets;
                                instructionSets2 = instructionSets;
                                result = result2;
                                obj2 = -1;
                                sharedGid = i3 + 1;
                                strArr = sharedLibraries;
                                obj = obj2;
                                pathsWithCode = pathsWithCode2;
                                dexCodeInstructionSets = dexCodeInstructionSets2;
                                classLoaderContexts = classLoaderContexts2;
                                sharedGid2 = sharedGid3;
                                paths = paths2;
                                instructionSets = instructionSets2;
                                i = this;
                                packageR = pkg;
                            }
                            isUsedByOtherApps = true;
                            compilerFilter = i.getRealCompilerFilter(packageR.applicationInfo, options.getCompilerFilter(), isUsedByOtherApps);
                            if (!options.isCheckForProfileUpdates()) {
                            }
                            isUsedByOtherApps = z;
                            dexoptFlags = i.getDexFlags(packageR, compilerFilter, options);
                            profileUpdated = isUsedByOtherApps;
                            profileUpdated2 = dexCodeInstructionSets.length;
                            result2 = result;
                            result = 0;
                            while (result < profileUpdated2) {
                            }
                            i3 = i2;
                            classLoaderContexts2 = classLoaderContexts;
                            pathsWithCode2 = pathsWithCode;
                            sharedGid3 = sharedGid2;
                            paths2 = paths;
                            dexCodeInstructionSets2 = dexCodeInstructionSets;
                            instructionSets2 = instructionSets;
                            result = result2;
                            obj2 = -1;
                            sharedGid = i3 + 1;
                            strArr = sharedLibraries;
                            obj = obj2;
                            pathsWithCode = pathsWithCode2;
                            dexCodeInstructionSets = dexCodeInstructionSets2;
                            classLoaderContexts = classLoaderContexts2;
                            sharedGid2 = sharedGid3;
                            paths = paths2;
                            instructionSets = instructionSets2;
                            i = this;
                            packageR = pkg;
                        }
                    } else {
                        i3 = i2;
                        classLoaderContexts2 = classLoaderContexts;
                        pathsWithCode2 = pathsWithCode;
                        sharedGid3 = sharedGid2;
                        paths2 = paths;
                        dexCodeInstructionSets2 = dexCodeInstructionSets;
                        instructionSets2 = instructionSets;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Inconsistent information in the package structure. A split is marked to contain code but has no dependency listed. Index=");
                        stringBuilder.append(i3);
                        stringBuilder.append(" path=");
                        stringBuilder.append((String) paths2.get(i3));
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                }
                i3 = i2;
                classLoaderContexts2 = classLoaderContexts;
                pathsWithCode2 = pathsWithCode;
                sharedGid3 = sharedGid2;
                obj2 = obj;
                paths2 = paths;
                dexCodeInstructionSets2 = dexCodeInstructionSets;
                instructionSets2 = instructionSets;
                sharedGid = i3 + 1;
                strArr = sharedLibraries;
                obj = obj2;
                pathsWithCode = pathsWithCode2;
                dexCodeInstructionSets = dexCodeInstructionSets2;
                classLoaderContexts = classLoaderContexts2;
                sharedGid2 = sharedGid3;
                paths = paths2;
                instructionSets = instructionSets2;
                i = this;
                packageR = pkg;
            } else {
                pathsWithCode2 = pathsWithCode;
                sharedGid3 = sharedGid2;
                List<String> list = paths;
                dexCodeInstructionSets2 = dexCodeInstructionSets;
                instructionSets2 = instructionSets;
                return result;
            }
        }
    }

    @GuardedBy("mInstallLock")
    private int dexOptPath(Package pkg, String path, String isa, String compilerFilter, boolean profileUpdated, String classLoaderContext, int dexoptFlags, int uid, PackageStats packageStats, boolean downgrade, String profileName, String dexMetadataPath, int compilationReason) {
        InstallerException e;
        Package packageR = pkg;
        String str = path;
        String str2 = isa;
        PackageStats packageStats2 = packageStats;
        int dexoptNeeded = getDexoptNeeded(str, str2, compilerFilter, classLoaderContext, profileUpdated, downgrade);
        if (Math.abs(dexoptNeeded) == 0) {
            return 0;
        }
        String oatDir = createOatDirIfSupported(packageR, str2);
        String str3 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Running dexopt (dexoptNeeded=");
        stringBuilder.append(dexoptNeeded);
        stringBuilder.append(") on: ");
        stringBuilder.append(str);
        stringBuilder.append(" pkg=");
        stringBuilder.append(packageR.applicationInfo.packageName);
        stringBuilder.append(" isa=");
        stringBuilder.append(str2);
        stringBuilder.append(" dexoptFlags=");
        int i = dexoptFlags;
        stringBuilder.append(printDexoptFlags(i));
        stringBuilder.append(" targetFilter=");
        String str4 = compilerFilter;
        stringBuilder.append(str4);
        stringBuilder.append(" oatDir=");
        stringBuilder.append(oatDir);
        stringBuilder.append(" classLoaderContext=");
        String str5 = classLoaderContext;
        stringBuilder.append(str5);
        Log.i(str3, stringBuilder.toString());
        PackageStats packageStats3;
        String str6;
        try {
            long startTime = System.currentTimeMillis();
            Installer installer = this.mInstaller;
            str3 = packageR.packageName;
            String str7 = packageR.volumeUuid;
            String str8 = packageR.applicationInfo.seInfo;
            int i2 = packageR.applicationInfo.targetSdkVersion;
            packageStats3 = packageStats2;
            str6 = str;
            try {
                installer.dexopt(str, uid, str3, isa, dexoptNeeded, oatDir, i, str4, str7, str5, str8, false, i2, profileName, dexMetadataPath, PackageManagerServiceCompilerMapping.getReasonName(compilationReason));
                if (packageStats3 != null) {
                    packageStats3.setCompileTime(str6, (long) ((int) (System.currentTimeMillis() - startTime)));
                }
                return 1;
            } catch (InstallerException e2) {
                e = e2;
                Slog.w(TAG, "Failed to dexopt", e);
                return -1;
            }
        } catch (InstallerException e3) {
            e = e3;
            packageStats3 = packageStats2;
            str6 = str;
            Slog.w(TAG, "Failed to dexopt", e);
            return -1;
        }
    }

    public int dexOptSecondaryDexPath(ApplicationInfo info, String path, DexUseInfo dexUseInfo, DexoptOptions options) {
        if (info.uid != -1) {
            int dexOptSecondaryDexPathLI;
            synchronized (this.mInstallLock) {
                long acquireTime = acquireWakeLockLI(info.uid);
                try {
                    dexOptSecondaryDexPathLI = dexOptSecondaryDexPathLI(info, path, dexUseInfo, options);
                } finally {
                    releaseWakeLockLI(acquireTime);
                }
            }
            return dexOptSecondaryDexPathLI;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Dexopt for path ");
        stringBuilder.append(path);
        stringBuilder.append(" has invalid uid.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @GuardedBy("mInstallLock")
    private long acquireWakeLockLI(int uid) {
        if (!this.mSystemReady) {
            return -1;
        }
        this.mDexoptWakeLock.setWorkSource(new WorkSource(uid));
        this.mDexoptWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
        return SystemClock.elapsedRealtime();
    }

    @GuardedBy("mInstallLock")
    private void releaseWakeLockLI(long acquireTime) {
        if (acquireTime >= 0) {
            try {
                if (this.mDexoptWakeLock.isHeld()) {
                    this.mDexoptWakeLock.release();
                }
                long duration = SystemClock.elapsedRealtime() - acquireTime;
                if (duration >= WAKELOCK_TIMEOUT_MS) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("WakeLock ");
                    stringBuilder.append(this.mDexoptWakeLock.getTag());
                    stringBuilder.append(" time out. Operation took ");
                    stringBuilder.append(duration);
                    stringBuilder.append(" ms. Thread: ");
                    stringBuilder.append(Thread.currentThread().getName());
                    Slog.wtf(str, stringBuilder.toString());
                }
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error while releasing ");
                stringBuilder2.append(this.mDexoptWakeLock.getTag());
                stringBuilder2.append(" lock");
                Slog.wtf(str2, stringBuilder2.toString(), e);
            }
        }
    }

    @GuardedBy("mInstallLock")
    private int dexOptSecondaryDexPathLI(ApplicationInfo info, String path, DexUseInfo dexUseInfo, DexoptOptions options) {
        ApplicationInfo applicationInfo = info;
        String str = path;
        if (options.isDexoptOnlySharedDex() && !dexUseInfo.isUsedByOtherApps()) {
            return 0;
        }
        String compilerFilter = getRealCompilerFilter(applicationInfo, options.getCompilerFilter(), dexUseInfo.isUsedByOtherApps());
        int dexoptFlags = getDexFlags(applicationInfo, compilerFilter, options) | 32;
        if (applicationInfo.deviceProtectedDataDir != null && FileUtils.contains(applicationInfo.deviceProtectedDataDir, str)) {
            dexoptFlags |= 256;
        } else if (applicationInfo.credentialProtectedDataDir == null || !FileUtils.contains(applicationInfo.credentialProtectedDataDir, str)) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not infer CE/DE storage for package ");
            stringBuilder.append(applicationInfo.packageName);
            Slog.e(str2, stringBuilder.toString());
            return -1;
        } else {
            dexoptFlags |= 128;
        }
        int dexoptFlags2 = dexoptFlags;
        String str3 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Running dexopt on: ");
        stringBuilder2.append(str);
        stringBuilder2.append(" pkg=");
        stringBuilder2.append(applicationInfo.packageName);
        stringBuilder2.append(" isa=");
        stringBuilder2.append(dexUseInfo.getLoaderIsas());
        stringBuilder2.append(" dexoptFlags=");
        stringBuilder2.append(printDexoptFlags(dexoptFlags2));
        stringBuilder2.append(" target-filter=");
        stringBuilder2.append(compilerFilter);
        Log.d(str3, stringBuilder2.toString());
        String classLoaderContext = SKIP_SHARED_LIBRARY_CHECK;
        int reason = options.getCompilationReason();
        String str4;
        int reason2;
        int dexoptFlags3;
        try {
            for (String isa : dexUseInfo.getLoaderIsas()) {
                Installer installer = this.mInstaller;
                int i = applicationInfo.uid;
                String str5 = applicationInfo.packageName;
                String str6 = applicationInfo.volumeUuid;
                String str7 = applicationInfo.seInfoUser;
                str4 = str6;
                int i2 = dexoptFlags2;
                reason2 = reason;
                dexoptFlags3 = dexoptFlags2;
                String str8 = str4;
                str4 = compilerFilter;
                try {
                    installer.dexopt(str, i, str5, isa, 0, null, i2, compilerFilter, str8, classLoaderContext, str7, options.isDowngrade(), applicationInfo.targetSdkVersion, null, null, PackageManagerServiceCompilerMapping.getReasonName(reason));
                    str = path;
                    DexoptOptions dexoptOptions = options;
                    compilerFilter = str4;
                    reason = reason2;
                    dexoptFlags2 = dexoptFlags3;
                } catch (InstallerException e) {
                    dexoptFlags = e;
                    Slog.w(TAG, "Failed to dexopt", dexoptFlags);
                    return -1;
                }
            }
            dexoptFlags3 = dexoptFlags2;
            str4 = compilerFilter;
            return 1;
        } catch (InstallerException e2) {
            dexoptFlags = e2;
            reason2 = reason;
            dexoptFlags3 = dexoptFlags2;
            str4 = compilerFilter;
            Slog.w(TAG, "Failed to dexopt", dexoptFlags);
            return -1;
        }
    }

    protected int adjustDexoptNeeded(int dexoptNeeded) {
        return dexoptNeeded;
    }

    protected int adjustDexoptFlags(int dexoptFlags) {
        return dexoptFlags;
    }

    void dumpDexoptState(IndentingPrintWriter pw, Package pkg, PackageUseInfo useInfo) {
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(InstructionSets.getAppDexInstructionSets(pkg.applicationInfo));
        for (String path : pkg.getAllCodePathsExcludingResourceOnly()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("path: ");
            stringBuilder.append(path);
            pw.println(stringBuilder.toString());
            pw.increaseIndent();
            for (String isa : dexCodeInstructionSets) {
                StringBuilder stringBuilder2;
                try {
                    OptimizationInfo info = DexFile.getDexFileOptimizationInfo(path, isa);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(isa);
                    stringBuilder2.append(": [status=");
                    stringBuilder2.append(info.getStatus());
                    stringBuilder2.append("] [reason=");
                    stringBuilder2.append(info.getReason());
                    stringBuilder2.append("]");
                    pw.println(stringBuilder2.toString());
                } catch (IOException ioe) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(isa);
                    stringBuilder2.append(": [Exception]: ");
                    stringBuilder2.append(ioe.getMessage());
                    pw.println(stringBuilder2.toString());
                }
            }
            if (useInfo.isUsedByOtherApps(path)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("used by other apps: ");
                stringBuilder.append(useInfo.getLoadingPackages(path));
                pw.println(stringBuilder.toString());
            }
            Map<String, DexUseInfo> dexUseInfoMap = useInfo.getDexUseInfoMap();
            if (!dexUseInfoMap.isEmpty()) {
                pw.println("known secondary dex files:");
                pw.increaseIndent();
                for (Entry<String, DexUseInfo> e : dexUseInfoMap.entrySet()) {
                    DexUseInfo dexUseInfo = (DexUseInfo) e.getValue();
                    pw.println((String) e.getKey());
                    pw.increaseIndent();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("class loader context: ");
                    stringBuilder3.append(dexUseInfo.getClassLoaderContext());
                    pw.println(stringBuilder3.toString());
                    if (dexUseInfo.isUsedByOtherApps()) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("used by other apps: ");
                        stringBuilder3.append(dexUseInfo.getLoadingPackages());
                        pw.println(stringBuilder3.toString());
                    }
                    pw.decreaseIndent();
                }
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
        }
    }

    private String getRealCompilerFilter(ApplicationInfo info, String targetCompilerFilter, boolean isUsedByOtherApps) {
        boolean vmSafeMode = (info.flags & 16384) != 0;
        if (info.isPrivilegedApp() && DexManager.isPackageSelectedToRunOob(info.packageName)) {
            return "verify";
        }
        if (vmSafeMode) {
            return DexFile.getSafeModeCompilerFilter(targetCompilerFilter);
        }
        if (DexFile.isProfileGuidedCompilerFilter(targetCompilerFilter) && isUsedByOtherApps) {
            return PackageManagerServiceCompilerMapping.getCompilerFilterForReason(6);
        }
        return targetCompilerFilter;
    }

    private int getDexFlags(Package pkg, String compilerFilter, DexoptOptions options) {
        return getDexFlags(pkg.applicationInfo, compilerFilter, options);
    }

    private boolean isAppImageEnabled() {
        return SystemProperties.get("dalvik.vm.appimageformat", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS).length() > 0;
    }

    private int getDexFlags(ApplicationInfo info, String compilerFilter, DexoptOptions options) {
        boolean generateAppImage = true;
        int i = 0;
        boolean debuggable = (info.flags & 2) != 0;
        boolean isProfileGuidedFilter = DexFile.isProfileGuidedCompilerFilter(compilerFilter);
        boolean isPublic = !info.isForwardLocked() && (!isProfileGuidedFilter || options.isDexoptInstallWithDexMetadata());
        int profileFlag = isProfileGuidedFilter ? 16 : 0;
        int hiddenApiFlag = info.getHiddenApiEnforcementPolicy() == 0 ? 0 : 1024;
        boolean generateCompactDex = true;
        switch (options.getCompilationReason()) {
            case 0:
            case 1:
            case 2:
                generateCompactDex = false;
                break;
        }
        if (!isProfileGuidedFilter || ((info.splitDependencies != null && info.requestsIsolatedSplitLoading()) || !isAppImageEnabled())) {
            generateAppImage = false;
        }
        int i2 = ((((((isPublic ? 2 : 0) | (options.isForce() ? 64 : 0)) | (debuggable ? 4 : 0)) | profileFlag) | (options.isBootComplete() ? 8 : 0)) | (options.isDexoptIdleBackgroundJob() ? 512 : 0)) | (generateCompactDex ? 2048 : 0);
        if (generateAppImage) {
            i = 4096;
        }
        return adjustDexoptFlags((i | i2) | hiddenApiFlag);
    }

    private int getDexoptNeeded(String path, String isa, String compilerFilter, String classLoaderContext, boolean newProfile, boolean downgrade) {
        try {
            return adjustDexoptNeeded(DexFile.getDexOptNeeded(path, isa, compilerFilter, classLoaderContext, newProfile, downgrade));
        } catch (IOException ioe) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOException reading apk: ");
            stringBuilder.append(path);
            Slog.w(str, stringBuilder.toString(), ioe);
            return -1;
        }
    }

    private boolean isProfileUpdated(Package pkg, int uid, String profileName, String compilerFilter) {
        if (!DexFile.isProfileGuidedCompilerFilter(compilerFilter)) {
            return false;
        }
        try {
            return this.mInstaller.mergeProfiles(uid, pkg.packageName, profileName);
        } catch (InstallerException e) {
            Slog.w(TAG, "Failed to merge profiles", e);
            return false;
        }
    }

    private String createOatDirIfSupported(Package pkg, String dexInstructionSet) {
        if (!pkg.canHaveOatDir()) {
            return null;
        }
        File codePath = new File(pkg.codePath);
        if (!codePath.isDirectory()) {
            return null;
        }
        File oatDir = getOatDir(codePath);
        try {
            this.mInstaller.createOatDir(oatDir.getAbsolutePath(), dexInstructionSet);
            return oatDir.getAbsolutePath();
        } catch (InstallerException e) {
            Slog.w(TAG, "Failed to create oat dir", e);
            return null;
        }
    }

    static File getOatDir(File codePath) {
        return new File(codePath, OAT_DIR_NAME);
    }

    void systemReady() {
        this.mSystemReady = true;
    }

    private String printDexoptFlags(int flags) {
        ArrayList<String> flagsList = new ArrayList();
        if ((flags & 8) == 8) {
            flagsList.add("boot_complete");
        }
        if ((flags & 4) == 4) {
            flagsList.add("debuggable");
        }
        if ((flags & 16) == 16) {
            flagsList.add("profile_guided");
        }
        if ((flags & 2) == 2) {
            flagsList.add("public");
        }
        if ((flags & 32) == 32) {
            flagsList.add("secondary");
        }
        if ((flags & 64) == 64) {
            flagsList.add("force");
        }
        if ((flags & 128) == 128) {
            flagsList.add("storage_ce");
        }
        if ((flags & 256) == 256) {
            flagsList.add("storage_de");
        }
        if ((flags & 512) == 512) {
            flagsList.add("idle_background_job");
        }
        if ((flags & 1024) == 1024) {
            flagsList.add("enable_hidden_api_checks");
        }
        return String.join(",", flagsList);
    }

    private static boolean dexEntryExists(String path) {
        ZipFile apkFile = null;
        boolean z = false;
        try {
            apkFile = new ZipFile(path);
            if (apkFile.getEntry("classes.dex") != null) {
                z = true;
            }
            try {
                apkFile.close();
            } catch (IOException e) {
            }
            return z;
        } catch (IOException | IllegalArgumentException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception reading apk: ");
            stringBuilder.append(path);
            Slog.w(str, stringBuilder.toString(), e2);
            if (apkFile != null) {
                try {
                    apkFile.close();
                } catch (IOException e3) {
                }
            }
            return false;
        } catch (Throwable th) {
            if (apkFile != null) {
                try {
                    apkFile.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    public long getDexOptTotalTime() {
        return this.mDexOptTotalTime;
    }

    public ArrayList<String> getPatchoatNeededApps() {
        return this.mPatchoatNeededApps;
    }
}
