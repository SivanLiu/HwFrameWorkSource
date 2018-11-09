package com.android.server.pm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser.Package;
import android.os.FileUtils;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.DexoptUtils;
import com.android.server.pm.dex.PackageDexUsage.DexUseInfo;
import com.android.server.pm.dex.PackageDexUsage.PackageUseInfo;
import dalvik.system.DexFile;
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
        return (pkg.applicationInfo.flags & 4) != 0;
    }

    int performDexOpt(Package pkg, String[] sharedLibraries, String[] instructionSets, PackageStats packageStats, PackageUseInfo packageUseInfo, DexoptOptions options) {
        if (!canOptimizePackage(pkg)) {
            return 0;
        }
        int performDexOptLI;
        synchronized (this.mInstallLock) {
            long acquireTime = acquireWakeLockLI(pkg.applicationInfo.uid);
            try {
                performDexOptLI = performDexOptLI(pkg, sharedLibraries, instructionSets, packageStats, packageUseInfo, options);
                releaseWakeLockLI(acquireTime);
            } catch (Throwable th) {
                releaseWakeLockLI(acquireTime);
            }
        }
        return performDexOptLI;
    }

    @GuardedBy("mInstallLock")
    private int performDexOptLI(Package pkg, String[] sharedLibraries, String[] targetInstructionSets, PackageStats packageStats, PackageUseInfo packageUseInfo, DexoptOptions options) {
        int i;
        String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(targetInstructionSets != null ? targetInstructionSets : InstructionSets.getAppDexInstructionSets(pkg.applicationInfo));
        List<String> paths = pkg.getAllCodePaths();
        int sharedGid = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
        boolean[] pathsWithCode = new boolean[paths.size()];
        pathsWithCode[0] = (pkg.applicationInfo.flags & 4) != 0;
        for (i = 1; i < paths.size(); i++) {
            pathsWithCode[i] = (pkg.splitFlags[i + -1] & 4) != 0;
        }
        String[] classLoaderContexts = DexoptUtils.getClassLoaderContexts(pkg.applicationInfo, sharedLibraries, pathsWithCode);
        if (paths.size() != classLoaderContexts.length) {
            String str;
            String[] splitCodePaths = pkg.applicationInfo.getSplitCodePaths();
            StringBuilder append = new StringBuilder().append("Inconsistent information between PackageParser.Package and its ApplicationInfo. pkg.getAllCodePaths=").append(paths).append(" pkg.applicationInfo.getBaseCodePath=").append(pkg.applicationInfo.getBaseCodePath()).append(" pkg.applicationInfo.getSplitCodePaths=");
            if (splitCodePaths == null) {
                str = "null";
            } else {
                str = Arrays.toString(splitCodePaths);
            }
            throw new IllegalStateException(append.append(str).toString());
        }
        int result = 0;
        for (i = 0; i < paths.size(); i++) {
            if (pathsWithCode[i]) {
                if (classLoaderContexts[i] == null) {
                    throw new IllegalStateException("Inconsistent information in the package structure. A split is marked to contain code but has no dependency listed. Index=" + i + " path=" + ((String) paths.get(i)));
                }
                String path = (String) paths.get(i);
                if (options.getSplitName() == null || options.getSplitName().equals(new File(path).getName())) {
                    boolean isUsedByOtherApps;
                    boolean isProfileUpdated;
                    if (options.isDexoptAsSharedLibrary()) {
                        isUsedByOtherApps = true;
                    } else {
                        isUsedByOtherApps = packageUseInfo.isUsedByOtherApps(path);
                    }
                    String compilerFilter = getRealCompilerFilter(pkg.applicationInfo, options.getCompilerFilter(), isUsedByOtherApps);
                    if (options.isCheckForProfileUpdates()) {
                        isProfileUpdated = isProfileUpdated(pkg, sharedGid, compilerFilter);
                    } else {
                        isProfileUpdated = false;
                    }
                    int dexoptFlags = getDexFlags(pkg, compilerFilter, options.isBootComplete());
                    for (String dexCodeIsa : dexCodeInstructionSets) {
                        int newResult = dexOptPath(pkg, path, dexCodeIsa, compilerFilter, isProfileUpdated, classLoaderContexts[i], dexoptFlags, sharedGid, packageStats, options.isDowngrade());
                        if (!(result == -1 || newResult == 0)) {
                            result = newResult;
                        }
                    }
                }
            }
        }
        return result;
    }

    @GuardedBy("mInstallLock")
    private int dexOptPath(Package pkg, String path, String isa, String compilerFilter, boolean profileUpdated, String sharedLibrariesPath, int dexoptFlags, int uid, PackageStats packageStats, boolean downgrade) {
        int dexoptNeeded = getDexoptNeeded(path, isa, compilerFilter, profileUpdated, downgrade);
        if (Math.abs(dexoptNeeded) == 0) {
            return 0;
        }
        String oatDir = createOatDirIfSupported(pkg, isa);
        Log.i(TAG, "Running dexopt (dexoptNeeded=" + dexoptNeeded + ") on: " + path + " pkg=" + pkg.applicationInfo.packageName + " isa=" + isa + " dexoptFlags=" + printDexoptFlags(dexoptFlags) + " target-filter=" + compilerFilter + " oatDir=" + oatDir + " sharedLibraries=" + sharedLibrariesPath);
        try {
            long startTime = System.currentTimeMillis();
            this.mInstaller.dexopt(path, uid, pkg.packageName, isa, dexoptNeeded, oatDir, dexoptFlags, compilerFilter, pkg.volumeUuid, sharedLibrariesPath, pkg.applicationInfo.seInfo, false);
            if (packageStats != null) {
                packageStats.setCompileTime(path, (long) ((int) (System.currentTimeMillis() - startTime)));
            }
            return 1;
        } catch (Throwable e) {
            Slog.w(TAG, "Failed to dexopt", e);
            return -1;
        }
    }

    public int dexOptSecondaryDexPath(ApplicationInfo info, String path, DexUseInfo dexUseInfo, DexoptOptions options) {
        int dexOptSecondaryDexPathLI;
        synchronized (this.mInstallLock) {
            long acquireTime = acquireWakeLockLI(info.uid);
            try {
                dexOptSecondaryDexPathLI = dexOptSecondaryDexPathLI(info, path, dexUseInfo, options);
                releaseWakeLockLI(acquireTime);
            } catch (Throwable th) {
                releaseWakeLockLI(acquireTime);
            }
        }
        return dexOptSecondaryDexPathLI;
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
                    Slog.wtf(TAG, "WakeLock " + this.mDexoptWakeLock.getTag() + " time out. Operation took " + duration + " ms. Thread: " + Thread.currentThread().getName());
                }
            } catch (Exception e) {
                Slog.wtf(TAG, "Error while releasing " + this.mDexoptWakeLock.getTag() + " lock", e);
            }
        }
    }

    @GuardedBy("mInstallLock")
    private int dexOptSecondaryDexPathLI(ApplicationInfo info, String path, DexUseInfo dexUseInfo, DexoptOptions options) {
        if (options.isDexoptOnlySharedDex() && (dexUseInfo.isUsedByOtherApps() ^ 1) != 0) {
            return 0;
        }
        String compilerFilter = getRealCompilerFilter(info, options.getCompilerFilter(), dexUseInfo.isUsedByOtherApps());
        int dexoptFlags = getDexFlags(info, compilerFilter, true) | 32;
        if (info.deviceProtectedDataDir != null && FileUtils.contains(info.deviceProtectedDataDir, path)) {
            dexoptFlags |= 256;
        } else if (info.credentialProtectedDataDir == null || !FileUtils.contains(info.credentialProtectedDataDir, path)) {
            Slog.e(TAG, "Could not infer CE/DE storage for package " + info.packageName);
            return -1;
        } else {
            dexoptFlags |= 128;
        }
        Log.d(TAG, "Running dexopt on: " + path + " pkg=" + info.packageName + " isa=" + dexUseInfo.getLoaderIsas() + " dexoptFlags=" + printDexoptFlags(dexoptFlags) + " target-filter=" + compilerFilter);
        String classLoaderContext = SKIP_SHARED_LIBRARY_CHECK;
        try {
            for (String isa : dexUseInfo.getLoaderIsas()) {
                String str = path;
                this.mInstaller.dexopt(str, info.uid, info.packageName, isa, 0, null, dexoptFlags, compilerFilter, info.volumeUuid, classLoaderContext, info.seInfoUser, options.isDowngrade());
            }
            return 1;
        } catch (InstallerException e) {
            Slog.w(TAG, "Failed to dexopt", e);
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
            pw.println("path: " + path);
            pw.increaseIndent();
            for (String isa : dexCodeInstructionSets) {
                String status;
                try {
                    status = DexFile.getDexFileStatus(path, isa);
                } catch (IOException ioe) {
                    status = "[Exception]: " + ioe.getMessage();
                }
                pw.println(isa + ": " + status);
            }
            if (useInfo.isUsedByOtherApps(path)) {
                pw.println("used by other apps: " + useInfo.getLoadingPackages(path));
            }
            Map<String, DexUseInfo> dexUseInfoMap = useInfo.getDexUseInfoMap();
            if (!dexUseInfoMap.isEmpty()) {
                pw.println("known secondary dex files:");
                pw.increaseIndent();
                for (Entry<String, DexUseInfo> e : dexUseInfoMap.entrySet()) {
                    DexUseInfo dexUseInfo = (DexUseInfo) e.getValue();
                    pw.println((String) e.getKey());
                    pw.increaseIndent();
                    pw.println("class loader context: " + dexUseInfo.getClassLoaderContext());
                    if (dexUseInfo.isUsedByOtherApps()) {
                        pw.println("used by other apps: " + dexUseInfo.getLoadingPackages());
                    }
                    pw.decreaseIndent();
                }
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
        }
    }

    private String getRealCompilerFilter(ApplicationInfo info, String targetCompilerFilter, boolean isUsedByOtherApps) {
        if ((info.flags & 16384) != 0) {
            return DexFile.getSafeModeCompilerFilter(targetCompilerFilter);
        }
        if (DexFile.isProfileGuidedCompilerFilter(targetCompilerFilter) && isUsedByOtherApps) {
            return PackageManagerServiceCompilerMapping.getCompilerFilterForReason(6);
        }
        return targetCompilerFilter;
    }

    private int getDexFlags(Package pkg, String compilerFilter, boolean bootComplete) {
        return getDexFlags(pkg.applicationInfo, compilerFilter, bootComplete);
    }

    private int getDexFlags(ApplicationInfo info, String compilerFilter, boolean bootComplete) {
        int i;
        int i2;
        int i3 = 0;
        boolean debuggable = (info.flags & 2) != 0;
        boolean isProfileGuidedFilter = DexFile.isProfileGuidedCompilerFilter(compilerFilter);
        int i4 = !info.isForwardLocked() ? isProfileGuidedFilter ^ 1 : 0;
        int profileFlag = isProfileGuidedFilter ? 16 : 0;
        if (i4 != 0) {
            i = 2;
        } else {
            i = 0;
        }
        if (debuggable) {
            i2 = 4;
        } else {
            i2 = 0;
        }
        i2 = (i2 | i) | profileFlag;
        if (bootComplete) {
            i3 = 8;
        }
        return adjustDexoptFlags(i2 | i3);
    }

    private int getDexoptNeeded(String path, String isa, String compilerFilter, boolean newProfile, boolean downgrade) {
        try {
            return adjustDexoptNeeded(DexFile.getDexOptNeeded(path, isa, compilerFilter, newProfile, downgrade));
        } catch (IOException ioe) {
            Slog.w(TAG, "IOException reading apk: " + path, ioe);
            return -1;
        }
    }

    private boolean isProfileUpdated(Package pkg, int uid, String compilerFilter) {
        if (!DexFile.isProfileGuidedCompilerFilter(compilerFilter)) {
            return false;
        }
        try {
            return this.mInstaller.mergeProfiles(uid, pkg.packageName);
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
        return String.join(",", flagsList);
    }

    private static boolean dexEntryExists(String path) {
        Exception e;
        Throwable th;
        boolean z = false;
        ZipFile zipFile = null;
        try {
            ZipFile apkFile = new ZipFile(path);
            try {
                if (apkFile.getEntry("classes.dex") != null) {
                    z = true;
                }
                if (apkFile != null) {
                    try {
                        apkFile.close();
                    } catch (IOException e2) {
                    }
                }
                return z;
            } catch (IOException e3) {
                e = e3;
                zipFile = apkFile;
                try {
                    Slog.w(TAG, "Exception reading apk: " + path, e);
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch (IOException e4) {
                        }
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    if (zipFile != null) {
                        try {
                            zipFile.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                zipFile = apkFile;
                if (zipFile != null) {
                    zipFile.close();
                }
                throw th;
            }
        } catch (IOException e6) {
            e = e6;
            Slog.w(TAG, "Exception reading apk: " + path, e);
            if (zipFile != null) {
                zipFile.close();
            }
            return false;
        }
    }

    public long getDexOptTotalTime() {
        return this.mDexOptTotalTime;
    }

    public ArrayList<String> getPatchoatNeededApps() {
        return this.mPatchoatNeededApps;
    }
}
