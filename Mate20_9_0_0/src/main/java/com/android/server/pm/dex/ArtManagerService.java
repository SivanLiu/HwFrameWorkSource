package com.android.server.pm.dex;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser.Package;
import android.content.pm.dex.ArtManager;
import android.content.pm.dex.ArtManagerInternal;
import android.content.pm.dex.DexMetadataHelper;
import android.content.pm.dex.IArtManager.Stub;
import android.content.pm.dex.ISnapshotRuntimeProfileCallback;
import android.content.pm.dex.PackageOptimizationInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.Os;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.LocalServices;
import com.android.server.pm.Installer;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.PackageManagerServiceCompilerMapping;
import com.android.server.power.IHwShutdownThread;
import dalvik.system.DexFile;
import dalvik.system.DexFile.OptimizationInfo;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileNotFoundException;
import libcore.io.IoUtils;

public class ArtManagerService extends Stub {
    private static final String BOOT_IMAGE_ANDROID_PACKAGE = "android";
    private static final String BOOT_IMAGE_PROFILE_NAME = "android.prof";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "ArtManagerService";
    private static final int TRON_COMPILATION_FILTER_ASSUMED_VERIFIED = 2;
    private static final int TRON_COMPILATION_FILTER_ERROR = 0;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING = 11;
    private static final int TRON_COMPILATION_FILTER_EVERYTHING_PROFILE = 10;
    private static final int TRON_COMPILATION_FILTER_EXTRACT = 3;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK = 12;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_APK_FALLBACK = 13;
    private static final int TRON_COMPILATION_FILTER_FAKE_RUN_FROM_VDEX_FALLBACK = 14;
    private static final int TRON_COMPILATION_FILTER_QUICKEN = 5;
    private static final int TRON_COMPILATION_FILTER_SPACE = 7;
    private static final int TRON_COMPILATION_FILTER_SPACE_PROFILE = 6;
    private static final int TRON_COMPILATION_FILTER_SPEED = 9;
    private static final int TRON_COMPILATION_FILTER_SPEED_PROFILE = 8;
    private static final int TRON_COMPILATION_FILTER_UNKNOWN = 1;
    private static final int TRON_COMPILATION_FILTER_VERIFY = 4;
    private static final int TRON_COMPILATION_REASON_AB_OTA = 6;
    private static final int TRON_COMPILATION_REASON_BG_DEXOPT = 5;
    private static final int TRON_COMPILATION_REASON_BG_SPEED_DEXOPT = 9;
    private static final int TRON_COMPILATION_REASON_BOOT = 3;
    private static final int TRON_COMPILATION_REASON_ERROR = 0;
    private static final int TRON_COMPILATION_REASON_FIRST_BOOT = 2;
    private static final int TRON_COMPILATION_REASON_INACTIVE = 7;
    private static final int TRON_COMPILATION_REASON_INSTALL = 4;
    private static final int TRON_COMPILATION_REASON_SHARED = 8;
    private static final int TRON_COMPILATION_REASON_UNKNOWN = 1;
    private final Context mContext;
    private final Handler mHandler = new Handler(BackgroundThread.getHandler().getLooper());
    private final Object mInstallLock;
    @GuardedBy("mInstallLock")
    private final Installer mInstaller;
    private final IPackageManager mPackageManager;

    private class ArtManagerInternalImpl extends ArtManagerInternal {
        private ArtManagerInternalImpl() {
        }

        public PackageOptimizationInfo getPackageOptimizationInfo(ApplicationInfo info, String abi) {
            String compilationFilter;
            String compilationReason;
            String str;
            StringBuilder stringBuilder;
            try {
                OptimizationInfo optInfo = DexFile.getDexFileOptimizationInfo(info.getBaseCodePath(), VMRuntime.getInstructionSet(abi));
                compilationFilter = optInfo.getStatus();
                compilationReason = optInfo.getReason();
            } catch (FileNotFoundException e) {
                str = ArtManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not get optimizations status for ");
                stringBuilder.append(info.getBaseCodePath());
                Slog.e(str, stringBuilder.toString(), e);
                compilationFilter = "error";
                compilationReason = "error";
            } catch (IllegalArgumentException e2) {
                str = ArtManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Requested optimization status for ");
                stringBuilder.append(info.getBaseCodePath());
                stringBuilder.append(" due to an invalid abi ");
                stringBuilder.append(abi);
                Slog.wtf(str, stringBuilder.toString(), e2);
                compilationFilter = "error";
                compilationReason = "error";
            }
            return new PackageOptimizationInfo(ArtManagerService.getCompilationFilterTronValue(compilationFilter), ArtManagerService.getCompilationReasonTronValue(compilationReason));
        }
    }

    static {
        verifyTronLoggingConstants();
    }

    public ArtManagerService(Context context, IPackageManager pm, Installer installer, Object installLock) {
        this.mContext = context;
        this.mPackageManager = pm;
        this.mInstaller = installer;
        this.mInstallLock = installLock;
        LocalServices.addService(ArtManagerInternal.class, new ArtManagerInternalImpl());
    }

    private boolean checkAndroidPermissions(int callingUid, String callingPackage) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_RUNTIME_PROFILES", TAG);
        int noteOp = ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).noteOp(43, callingUid, callingPackage);
        if (noteOp == 0) {
            return true;
        }
        if (noteOp != 3) {
            return false;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", TAG);
        return true;
    }

    private boolean checkShellPermissions(int profileType, String packageName, int callingUid) {
        boolean z = false;
        if (callingUid != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME) {
            return false;
        }
        if (RoSystemProperties.DEBUGGABLE) {
            return true;
        }
        if (profileType == 1) {
            return false;
        }
        PackageInfo info = null;
        try {
            info = this.mPackageManager.getPackageInfo(packageName, 0, 0);
        } catch (RemoteException e) {
        }
        if (info == null) {
            return false;
        }
        if ((info.applicationInfo.flags & 2) == 2) {
            z = true;
        }
        return z;
    }

    public void snapshotRuntimeProfile(int profileType, String packageName, String codePath, ISnapshotRuntimeProfileCallback callback, String callingPackage) {
        int callingUid = Binder.getCallingUid();
        if (checkShellPermissions(profileType, packageName, callingUid) || checkAndroidPermissions(callingUid, callingPackage)) {
            Preconditions.checkNotNull(callback);
            boolean bootImageProfile = true;
            if (profileType != 1) {
                bootImageProfile = false;
            }
            if (!bootImageProfile) {
                Preconditions.checkStringNotEmpty(codePath);
                Preconditions.checkStringNotEmpty(packageName);
            }
            StringBuilder stringBuilder;
            if (isRuntimeProfilingEnabled(profileType, callingPackage)) {
                if (DEBUG) {
                    String str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Requested snapshot for ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(":");
                    stringBuilder.append(codePath);
                    Slog.d(str, stringBuilder.toString());
                }
                if (bootImageProfile) {
                    snapshotBootImageProfile(callback);
                } else {
                    snapshotAppProfile(packageName, codePath, callback);
                }
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Runtime profiling is not enabled for ");
            stringBuilder.append(profileType);
            throw new IllegalStateException(stringBuilder.toString());
        }
        try {
            callback.onError(2);
        } catch (RemoteException e) {
        }
    }

    private void snapshotAppProfile(String packageName, String codePath, ISnapshotRuntimeProfileCallback callback) {
        PackageInfo info = null;
        try {
            info = this.mPackageManager.getPackageInfo(packageName, 0, 0);
        } catch (RemoteException e) {
        }
        if (info == null) {
            postError(callback, packageName, 0);
            return;
        }
        boolean pathFound = info.applicationInfo.getBaseCodePath().equals(codePath);
        String splitName = null;
        String[] splitCodePaths = info.applicationInfo.getSplitCodePaths();
        if (!pathFound && splitCodePaths != null) {
            for (int i = splitCodePaths.length - 1; i >= 0; i--) {
                if (splitCodePaths[i].equals(codePath)) {
                    pathFound = true;
                    splitName = info.applicationInfo.splitNames[i];
                    break;
                }
            }
        }
        if (pathFound) {
            int appId = UserHandle.getAppId(info.applicationInfo.uid);
            if (appId < 0) {
                postError(callback, packageName, 2);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AppId is -1 for package: ");
                stringBuilder.append(packageName);
                Slog.wtf(str, stringBuilder.toString());
                return;
            }
            createProfileSnapshot(packageName, ArtManager.getProfileName(splitName), codePath, appId, callback);
            destroyProfileSnapshot(packageName, ArtManager.getProfileName(splitName));
            return;
        }
        postError(callback, packageName, 1);
    }

    /* JADX WARNING: Missing block: B:12:0x0013, code skipped:
            r0 = android.content.pm.dex.ArtManager.getProfileSnapshotFileForName(r8, r9);
     */
    /* JADX WARNING: Missing block: B:14:?, code skipped:
            r2 = android.os.ParcelFileDescriptor.open(r0, 268435456);
     */
    /* JADX WARNING: Missing block: B:15:0x001f, code skipped:
            if (r2 == null) goto L_0x0030;
     */
    /* JADX WARNING: Missing block: B:17:0x0029, code skipped:
            if (r2.getFileDescriptor().valid() != false) goto L_0x002c;
     */
    /* JADX WARNING: Missing block: B:19:0x002c, code skipped:
            postSuccess(r8, r2, r12);
     */
    /* JADX WARNING: Missing block: B:20:0x0030, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("ParcelFileDescriptor.open returned an invalid descriptor for ");
            r4.append(r8);
            r4.append(":");
            r4.append(r0);
            r4.append(". isNull=");
     */
    /* JADX WARNING: Missing block: B:21:0x004c, code skipped:
            if (r2 != null) goto L_0x0050;
     */
    /* JADX WARNING: Missing block: B:22:0x004e, code skipped:
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:23:0x0050, code skipped:
            r5 = false;
     */
    /* JADX WARNING: Missing block: B:24:0x0051, code skipped:
            r4.append(r5);
            android.util.Slog.wtf(r3, r4.toString());
            postError(r12, r8, 2);
     */
    /* JADX WARNING: Missing block: B:25:0x005f, code skipped:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:26:0x0060, code skipped:
            r4 = TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("Could not open snapshot profile for ");
            r5.append(r8);
            r5.append(":");
            r5.append(r0);
            android.util.Slog.w(r4, r5.toString(), r3);
            postError(r12, r8, 2);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void createProfileSnapshot(String packageName, String profileName, String classpath, int appId, ISnapshotRuntimeProfileCallback callback) {
        synchronized (this.mInstallLock) {
            try {
                if (!this.mInstaller.createProfileSnapshot(appId, packageName, profileName, classpath)) {
                    postError(callback, packageName, 2);
                }
            } catch (InstallerException e) {
                postError(callback, packageName, 2);
            }
        }
    }

    private void destroyProfileSnapshot(String packageName, String profileName) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Destroying profile snapshot for");
            stringBuilder.append(packageName);
            stringBuilder.append(":");
            stringBuilder.append(profileName);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this.mInstallLock) {
            try {
                this.mInstaller.destroyProfileSnapshot(packageName, profileName);
            } catch (InstallerException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to destroy profile snapshot for ");
                stringBuilder2.append(packageName);
                stringBuilder2.append(":");
                stringBuilder2.append(profileName);
                Slog.e(str2, stringBuilder2.toString(), e);
            }
        }
    }

    public boolean isRuntimeProfilingEnabled(int profileType, String callingPackage) {
        int callingUid = Binder.getCallingUid();
        boolean z = false;
        if (callingUid != IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME && !checkAndroidPermissions(callingUid, callingPackage)) {
            return false;
        }
        switch (profileType) {
            case 0:
                return SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false);
            case 1:
                if ((Build.IS_USERDEBUG || Build.IS_ENG) && SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false) && SystemProperties.getBoolean("dalvik.vm.profilebootimage", false)) {
                    z = true;
                }
                return z;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid profile type:");
                stringBuilder.append(profileType);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private void snapshotBootImageProfile(ISnapshotRuntimeProfileCallback callback) {
        createProfileSnapshot("android", BOOT_IMAGE_PROFILE_NAME, String.join(":", new CharSequence[]{Os.getenv("BOOTCLASSPATH"), Os.getenv("SYSTEMSERVERCLASSPATH")}), -1, callback);
        destroyProfileSnapshot("android", BOOT_IMAGE_PROFILE_NAME);
    }

    private void postError(ISnapshotRuntimeProfileCallback callback, String packageName, int errCode) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to snapshot profile for ");
            stringBuilder.append(packageName);
            stringBuilder.append(" with error: ");
            stringBuilder.append(errCode);
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.post(new -$$Lambda$ArtManagerService$_rD0Y6OPSJHMdjTIOtucoGQ1xag(callback, errCode, packageName));
    }

    static /* synthetic */ void lambda$postError$0(ISnapshotRuntimeProfileCallback callback, int errCode, String packageName) {
        try {
            callback.onError(errCode);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to callback after profile snapshot for ");
            stringBuilder.append(packageName);
            Slog.w(str, stringBuilder.toString(), e);
        }
    }

    private void postSuccess(String packageName, ParcelFileDescriptor fd, ISnapshotRuntimeProfileCallback callback) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Successfully snapshot profile for ");
            stringBuilder.append(packageName);
            Slog.d(str, stringBuilder.toString());
        }
        this.mHandler.post(new -$$Lambda$ArtManagerService$MEVzU-orlv4msZVF-bA5NLti04g(fd, callback, packageName));
    }

    static /* synthetic */ void lambda$postSuccess$1(ParcelFileDescriptor fd, ISnapshotRuntimeProfileCallback callback, String packageName) {
        try {
            if (fd.getFileDescriptor().valid()) {
                callback.onSuccess(fd);
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The snapshot FD became invalid before posting the result for ");
                stringBuilder.append(packageName);
                Slog.wtf(str, stringBuilder.toString());
                callback.onError(2);
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to call onSuccess after profile snapshot for ");
            stringBuilder2.append(packageName);
            Slog.w(str2, stringBuilder2.toString(), e);
        } catch (Throwable th) {
            IoUtils.closeQuietly(fd);
        }
        IoUtils.closeQuietly(fd);
    }

    public void prepareAppProfiles(Package pkg, int user) {
        int appId = UserHandle.getAppId(pkg.applicationInfo.uid);
        String str;
        StringBuilder stringBuilder;
        if (user < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid user id: ");
            stringBuilder.append(user);
            Slog.wtf(str, stringBuilder.toString());
        } else if (appId < 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid app id: ");
            stringBuilder.append(appId);
            Slog.wtf(str, stringBuilder.toString());
        } else {
            String str2;
            StringBuilder stringBuilder2;
            try {
                ArrayMap<String, String> codePathsProfileNames = getPackageProfileNames(pkg);
                int i = codePathsProfileNames.size() - 1;
                while (true) {
                    int i2 = i;
                    if (i2 < 0) {
                        break;
                    }
                    String codePath = (String) codePathsProfileNames.keyAt(i2);
                    String profileName = (String) codePathsProfileNames.valueAt(i2);
                    File dexMetadata = DexMetadataHelper.findDexMetadataForFile(new File(codePath));
                    String dexMetadataPath = dexMetadata == null ? null : dexMetadata.getAbsolutePath();
                    synchronized (this.mInstaller) {
                        if (!this.mInstaller.prepareAppProfile(pkg.packageName, user, appId, profileName, codePath, dexMetadataPath)) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Failed to prepare profile for ");
                            stringBuilder2.append(pkg.packageName);
                            stringBuilder2.append(":");
                            stringBuilder2.append(codePath);
                            Slog.e(str2, stringBuilder2.toString());
                        }
                    }
                    i = i2 - 1;
                }
            } catch (InstallerException e) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to prepare profile for ");
                stringBuilder2.append(pkg.packageName);
                Slog.e(str2, stringBuilder2.toString(), e);
            }
        }
    }

    public void prepareAppProfiles(Package pkg, int[] user) {
        for (int prepareAppProfiles : user) {
            prepareAppProfiles(pkg, prepareAppProfiles);
        }
    }

    public void clearAppProfiles(Package pkg) {
        try {
            ArrayMap<String, String> packageProfileNames = getPackageProfileNames(pkg);
            for (int i = packageProfileNames.size() - 1; i >= 0; i--) {
                this.mInstaller.clearAppProfiles(pkg.packageName, (String) packageProfileNames.valueAt(i));
            }
        } catch (InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    public void dumpProfiles(Package pkg) {
        int sharedGid = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
        try {
            ArrayMap<String, String> packageProfileNames = getPackageProfileNames(pkg);
            for (int i = packageProfileNames.size() - 1; i >= 0; i--) {
                String codePath = (String) packageProfileNames.keyAt(i);
                String profileName = (String) packageProfileNames.valueAt(i);
                synchronized (this.mInstallLock) {
                    this.mInstaller.dumpProfiles(sharedGid, pkg.packageName, profileName, codePath);
                }
            }
        } catch (InstallerException e) {
            Slog.w(TAG, "Failed to dump profiles", e);
        }
    }

    private ArrayMap<String, String> getPackageProfileNames(Package pkg) {
        ArrayMap<String, String> result = new ArrayMap();
        if ((pkg.applicationInfo.flags & 4) != 0) {
            result.put(pkg.baseCodePath, ArtManager.getProfileName(null));
        }
        if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
            for (int i = 0; i < pkg.splitCodePaths.length; i++) {
                if ((pkg.splitFlags[i] & 4) != 0) {
                    result.put(pkg.splitCodePaths[i], ArtManager.getProfileName(pkg.splitNames[i]));
                }
            }
        }
        return result;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getCompilationReasonTronValue(String compilationReason) {
        int i;
        switch (compilationReason.hashCode()) {
            case -1968171580:
                if (compilationReason.equals("bg-dexopt")) {
                    i = 5;
                    break;
                }
            case -1425983632:
                if (compilationReason.equals("ab-ota")) {
                    i = 6;
                    break;
                }
            case -903566235:
                if (compilationReason.equals("shared")) {
                    i = 8;
                    break;
                }
            case -284840886:
                if (compilationReason.equals(Shell.NIGHT_MODE_STR_UNKNOWN)) {
                    i = 0;
                    break;
                }
            case -207505425:
                if (compilationReason.equals("first-boot")) {
                    i = 2;
                    break;
                }
            case 3029746:
                if (compilationReason.equals("boot")) {
                    i = 3;
                    break;
                }
            case 24665195:
                if (compilationReason.equals("inactive")) {
                    i = 7;
                    break;
                }
            case 96784904:
                if (compilationReason.equals("error")) {
                    i = 1;
                    break;
                }
            case 1022487562:
                if (compilationReason.equals("bg-speed-dexopt")) {
                    i = 9;
                    break;
                }
            case 1957569947:
                if (compilationReason.equals("install")) {
                    i = 4;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 0;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case 8:
                return 8;
            case 9:
                return 9;
            default:
                return 1;
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getCompilationFilterTronValue(String compilationFilter) {
        int i;
        String str = compilationFilter;
        switch (compilationFilter.hashCode()) {
            case -1957514039:
                if (str.equals("assume-verified")) {
                    i = 2;
                    break;
                }
            case -1803365233:
                if (str.equals("everything-profile")) {
                    i = 10;
                    break;
                }
            case -1305289599:
                if (str.equals("extract")) {
                    i = 3;
                    break;
                }
            case -1129892317:
                if (str.equals("speed-profile")) {
                    i = 8;
                    break;
                }
            case -902315795:
                if (str.equals("run-from-vdex-fallback")) {
                    i = 14;
                    break;
                }
            case -819951495:
                if (str.equals("verify")) {
                    i = 4;
                    break;
                }
            case -284840886:
                if (str.equals(Shell.NIGHT_MODE_STR_UNKNOWN)) {
                    i = 1;
                    break;
                }
            case 96784904:
                if (str.equals("error")) {
                    i = 0;
                    break;
                }
            case 109637894:
                if (str.equals("space")) {
                    i = 7;
                    break;
                }
            case 109641799:
                if (str.equals("speed")) {
                    i = 9;
                    break;
                }
            case 348518370:
                if (str.equals("space-profile")) {
                    i = 6;
                    break;
                }
            case 401590963:
                if (str.equals("everything")) {
                    i = 11;
                    break;
                }
            case 658336598:
                if (str.equals("quicken")) {
                    i = 5;
                    break;
                }
            case 922064507:
                if (str.equals("run-from-apk")) {
                    i = 12;
                    break;
                }
            case 1906552308:
                if (str.equals("run-from-apk-fallback")) {
                    i = 13;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            case 8:
                return 8;
            case 9:
                return 9;
            case 10:
                return 10;
            case 11:
                return 11;
            case 12:
                return 12;
            case 13:
                return 13;
            case 14:
                return 14;
            default:
                return 1;
        }
    }

    private static void verifyTronLoggingConstants() {
        for (String reason : PackageManagerServiceCompilerMapping.REASON_STRINGS) {
            int value = getCompilationReasonTronValue(reason);
            if (value == 0 || value == 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Compilation reason not configured for TRON logging: ");
                stringBuilder.append(reason);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }
}
