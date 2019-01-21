package com.android.server.pm;

import android.app.ActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.hdm.HwDeviceManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.perf.HwOptPackageParser;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.pm.PackageManagerService.OriginInfo;
import com.android.server.pm.PackageManagerService.VerificationInfo;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.security.antimal.HwAntiMalStatus;
import com.huawei.android.manufacture.ManufactureNativeUtils;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HwPackageManagerServiceEx implements IHwPackageManagerServiceEx {
    private static final String FLAG_APK_NOSYS = "nosys";
    private static final String FLAG_APK_PRIV = "priv";
    private static final String FLAG_APK_SYS = "sys";
    private static final String HW_PMS_SET_APP_PERMISSION = "huawei.android.permission.SET_CANNOT_UNINSTALLED_PERMISSION";
    private static final boolean IS_NOTCH_PROP = (SystemProperties.get("ro.config.hw_notch_size", "").equals("") ^ 1);
    private static final String MIDDLEWARE_LIMITED_DPC_PKGS = "com.huawei.mdm.dpc";
    public static final int MSG_SET_CURRENT_EMUI_SYS_IMG_VERSION = 1;
    static final long OTA_WAIT_DEXOPT_TIME = 480000;
    private static final String SYSTEM_SIGN_STR = "30820405308202eda00302010202090083309550b47e0583300d06092a864886f70d0101050500308198310b300906035504061302434e3112301006035504080c094775616e67646f6e673112301006035504070c095368656e677a68656e310f300d060355040a0c0648756177656931183016060355040b0c0f5465726d696e616c436f6d70616e793114301206035504030c0b416e64726f69645465616d3120301e06092a864886f70d01090116116d6f62696c65406875617765692e636f6d301e170d3136303530353037333531345a170d3433303932313037333531345a308198310b300906035504061302434e3112301006035504080c094775616e67646f6e673112301006035504070c095368656e677a68656e310f300d060355040a0c0648756177656931183016060355040b0c0f5465726d696e616c436f6d70616e793114301206035504030c0b416e64726f69645465616d3120301e06092a864886f70d01090116116d6f62696c65406875617765692e636f6d30820122300d06092a864886f70d01010105000382010f003082010a0282010100c9fe1b699203091cb3944030cb1ba7996567182c1ce8be5535d673bc2025f37958e5bb1f4ed870dc229ffc2ed7d16f6cf10c08bc63f53624abe49db543518ef0069686ea5b3f129188652e87eca4b794df591828dd94de14b91ddbf2af156426453b8e739b12625a44b0895bfa1db3cdcce7db52f4d5af7c9918c325475c8273a5e4fe002e0f68082e9ec61d100913618982928ab5767701a8f576113c0810a4850a606233fd654531562bf8a74ac81bf8bacd66ca8a5ca9751f08e9575b402221e48e474f7f2dc91d02cfd87ceeaeb39ccf754cff5f1e8dfe23587955481bf0b8a386993edadc0f725e124f1ecedbef8d3cfbd6ddc783cde4b193f79fae05ed0203010001a350304e301d0603551d0e041604148d42132bfdc2ed970e25f5677cedd26f32527bc8301f0603551d230418301680148d42132bfdc2ed970e25f5677cedd26f32527bc8300c0603551d13040530030101ff300d06092a864886f70d010105050003820101003bc6e2ba8703a211222da8ed350e12cf31ac4d91290c5524da44626c382c8186f8238860b7ebddebba996f204802d72246d1326332ca85aff4a10cdaaa0d886016e26075c9b98799bf4767663d8c1097dccbc609dd3946f6431a35a71ee9ff3731c5b2715c158fe8d64c700b7e3e387e63a62e80ecdd4d007af242abed4b694d5a70d12dbde433fd18e1a7d033142f44cbe9ca187134830b86ecfa78ae2ff6d201014e4cf1d1655f40f4e4f4dd04af3c0416709dd159845d25515ff12f2854180e2ccbc1b05dffce93f9487839c126fa39f1453468a41eb7872b84c736dcb0d90a29775cd863707044f28bce4d05edcce4699605b27ae11e981590f87384726d";
    static final String TAG = "HwPackageManagerServiceEx";
    static final long WAIT_DEXOPT_TIME = 180000;
    private boolean mBootCompleted = false;
    private final HandlerThread mCommonThread = new HandlerThread("PMSCommonThread");
    final Context mContext;
    private boolean mDexoptNow = false;
    final PackageExHandler mHandler;
    private HwAntiMalStatus mHwAntiMalStatus = null;
    private HwFastAppManager mHwFastAppManager = null;
    private HwOptPackageParser mHwOptPackageParser = null;
    IHwPackageManagerInner mIPmsInner = null;
    private AtomicBoolean mIsOpting = new AtomicBoolean(false);
    final Object mSpeedOptLock = new Object();
    private ArraySet<String> mSpeedOptPkgs = new ArraySet();
    private HashSet<String> mUninstallBlackListPkgNames = new HashSet();
    private long mUserSwitchingTime = 0;

    class PackageExHandler extends Handler {
        PackageExHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            try {
                doHandleMessage(msg);
            } catch (Exception e) {
            }
        }

        void doHandleMessage(Message msg) {
            if (msg.what == 1) {
                PackageParser.setCurrentEmuiSysImgVersion(HwPackageManagerServiceEx.deriveEmuiSysImgVersion());
            }
        }
    }

    public HwPackageManagerServiceEx(IHwPackageManagerInner pms, Context context) {
        this.mIPmsInner = pms;
        this.mContext = context;
        this.mHwOptPackageParser = HwFrameworkFactory.getHwOptPackageParser();
        this.mHwOptPackageParser.getOptPackages();
        this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
        this.mCommonThread.start();
        this.mHwFastAppManager = new HwFastAppManager(this.mContext, new Handler(this.mCommonThread.getLooper()));
        this.mHandler = new PackageExHandler(this.mCommonThread.getLooper());
    }

    public boolean isPerfOptEnable(String packageName, int optType) {
        return this.mHwOptPackageParser.isPerfOptEnable(packageName, optType);
    }

    public void checkHwCertification(Package pkg, boolean isUpdate) {
        if (!HwCertificationManager.hasFeature()) {
            return;
        }
        if (HwCertificationManager.isSupportHwCertification(pkg)) {
            boolean isUpgrade = this.mIPmsInner.isUpgrade();
            if (isUpdate || !isContainHwCertification(pkg) || isUpgrade) {
                cleanUpHwCert(pkg);
                checkContainHwCert(pkg);
            }
            return;
        }
        if (isContainHwCertification(pkg)) {
            cleanUpHwCert(pkg);
        }
    }

    private void checkContainHwCert(Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager != null && !manager.checkHwCertification(pkg)) {
        }
    }

    public boolean getHwCertPermission(boolean allowed, Package pkg, String perm) {
        if (!HwCertificationManager.hasFeature()) {
            return allowed;
        }
        if (!HwCertificationManager.isInitialized()) {
            HwCertificationManager.initialize(this.mContext);
        }
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager == null) {
            return allowed;
        }
        return manager.getHwCertificationPermission(allowed, pkg, perm);
    }

    private void cleanUpHwCert(Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager != null) {
            manager.cleanUp(pkg);
        }
    }

    public void cleanUpHwCert() {
        if (HwCertificationManager.hasFeature()) {
            if (!HwCertificationManager.isInitialized()) {
                HwCertificationManager.initialize(this.mContext);
            }
            HwCertificationManager manager = HwCertificationManager.getIntance();
            if (manager != null) {
                manager.cleanUp();
            }
        }
    }

    public void initHwCertificationManager() {
        if (!HwCertificationManager.isInitialized()) {
            HwCertificationManager.initialize(this.mContext);
        }
        HwCertificationManager manager = HwCertificationManager.getIntance();
    }

    public int getHwCertificateType(Package pkg) {
        if (HwCertificationManager.isSupportHwCertification(pkg)) {
            return HwCertificationManager.getIntance().getHwCertificateType(pkg.packageName);
        }
        return HwCertificationManager.getIntance().getHwCertificateTypeNotMDM();
    }

    public boolean isContainHwCertification(Package pkg) {
        return HwCertificationManager.getIntance().isContainHwCertification(pkg.packageName);
    }

    public boolean isAllowedSetHomeActivityForAntiMal(PackageInfo pi, int userId) {
        if (this.mHwAntiMalStatus == null) {
            this.mHwAntiMalStatus = new HwAntiMalStatus(this.mContext);
        }
        return this.mHwAntiMalStatus.isAllowedSetHomeActivityForAntiMal(pi, userId);
    }

    public void updateNochScreenWhite(String packageName, String flag, int versionCode) {
        if (IS_NOTCH_PROP) {
            HwNotchScreenWhiteConfig.getInstance().updateVersionCodeInNoch(packageName, flag, versionCode);
            if ("removed".equals(flag)) {
                HwNotchScreenWhiteConfig.getInstance().removeAppUseNotchMode(packageName);
            }
        }
    }

    public int getAppUseNotchMode(String packageName) {
        if (!IS_NOTCH_PROP) {
            return -1;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                if (pkgSetting == null) {
                    Binder.restoreCallingIdentity(callingId);
                    return -1;
                }
                int appUseNotchMode = pkgSetting.getAppUseNotchMode();
                Binder.restoreCallingIdentity(callingId);
                return appUseNotchMode;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    /* JADX WARNING: Missing block: B:22:0x0055, code skipped:
            r3 = r4;
            android.os.Binder.restoreCallingIdentity(r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setAppUseNotchMode(String packageName, int mode) {
        if (IS_NOTCH_PROP) {
            int uid = Binder.getCallingUid();
            if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    synchronized (this.mIPmsInner.getPackagesLock()) {
                        PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                        if (pkgSetting == null) {
                            Binder.restoreCallingIdentity(callingId);
                        } else if (pkgSetting.getAppUseNotchMode() != mode) {
                            pkgSetting.setAppUseNotchMode(mode);
                            this.mIPmsInner.getSettings().writeLPr();
                            HwNotchScreenWhiteConfig.getInstance().updateAppUseNotchMode(packageName, mode);
                        }
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(callingId);
                }
            } else {
                throw new SecurityException("Only the system can set app use notch mode");
            }
        }
    }

    private void listenForUserSwitches() {
        try {
            ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                public void onUserSwitching(int newUserId) throws RemoteException {
                    synchronized (HwPackageManagerServiceEx.this.mSpeedOptLock) {
                        HwPackageManagerServiceEx.this.mUserSwitchingTime = SystemClock.elapsedRealtime();
                        String str = HwPackageManagerServiceEx.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onUserSwitching ");
                        stringBuilder.append(HwPackageManagerServiceEx.this.mUserSwitchingTime);
                        Slog.d(str, stringBuilder.toString());
                    }
                }
            }, TAG);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to listen for user switching event");
        }
    }

    public boolean isApkDexOpt(String targetCompilerFilter) {
        return "speed-profile-opt".equals(targetCompilerFilter);
    }

    /* JADX WARNING: Missing block: B:36:0x00b5, code skipped:
            r5 = "speed-profile";
            r1.mIsOpting.set(true);
     */
    /* JADX WARNING: Missing block: B:37:0x00bd, code skipped:
            r6 = r1.mIPmsInner.performDexOptMode(r2, r20, r5, r22, r23, r24);
            r7 = r1.mSpeedOptLock;
     */
    /* JADX WARNING: Missing block: B:38:0x00cf, code skipped:
            monitor-enter(r7);
     */
    /* JADX WARNING: Missing block: B:41:0x00d6, code skipped:
            if (r1.mSpeedOptPkgs.isEmpty() != false) goto L_0x00ee;
     */
    /* JADX WARNING: Missing block: B:43:0x00da, code skipped:
            if (r1.mDexoptNow != false) goto L_0x00dd;
     */
    /* JADX WARNING: Missing block: B:45:0x00dd, code skipped:
            r2 = (java.lang.String) r1.mSpeedOptPkgs.valueAt(0);
            r1.mSpeedOptPkgs.removeAt(0);
     */
    /* JADX WARNING: Missing block: B:46:0x00eb, code skipped:
            monitor-exit(r7);
     */
    /* JADX WARNING: Missing block: B:47:0x00ec, code skipped:
            r3 = r6;
     */
    /* JADX WARNING: Missing block: B:48:0x00ee, code skipped:
            monitor-exit(r7);
     */
    /* JADX WARNING: Missing block: B:49:0x00ef, code skipped:
            r1.mIsOpting.set(false);
     */
    /* JADX WARNING: Missing block: B:50:0x00f4, code skipped:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hwPerformDexOptMode(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force, boolean bootComplete, String splitName) {
        String packageName2 = packageName;
        synchronized (this.mSpeedOptLock) {
            if (!this.mBootCompleted) {
                this.mBootCompleted = SystemProperties.get("sys.boot_completed", "0").equals("1");
            }
            long elapsedTime = SystemClock.elapsedRealtime();
            if (!this.mDexoptNow) {
                this.mDexoptNow = elapsedTime > (this.mIPmsInner.isUpgrade() ? OTA_WAIT_DEXOPT_TIME : WAIT_DEXOPT_TIME);
            }
            if (this.mUserSwitchingTime != 0 && this.mDexoptNow) {
                this.mDexoptNow = false;
                if (elapsedTime > this.mUserSwitchingTime) {
                    this.mDexoptNow = elapsedTime - this.mUserSwitchingTime > WAIT_DEXOPT_TIME;
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("now ");
            stringBuilder.append(elapsedTime);
            stringBuilder.append(" optNow ");
            stringBuilder.append(this.mDexoptNow);
            stringBuilder.append(" upgrade ");
            stringBuilder.append(this.mIPmsInner.isUpgrade());
            stringBuilder.append(" BootCompleted ");
            stringBuilder.append(this.mBootCompleted);
            stringBuilder.append(" UserSwitching ");
            stringBuilder.append(this.mUserSwitchingTime);
            Slog.i(str, stringBuilder.toString());
            if (!this.mIsOpting.get() && this.mDexoptNow) {
                if (!this.mBootCompleted) {
                }
            }
            this.mSpeedOptPkgs.add(packageName2);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("performDexOptMode add list ");
            stringBuilder.append(packageName2);
            stringBuilder.append(" size ");
            stringBuilder.append(this.mSpeedOptPkgs.size());
            Slog.d(str, stringBuilder.toString());
            return true;
        }
    }

    public void setAppCanUninstall(String packageName, boolean canUninstall) {
        this.mContext.enforceCallingPermission(HW_PMS_SET_APP_PERMISSION, "setAppCanUninstall");
        String callingName = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        if (callingName != null && callingName.equalsIgnoreCase(packageName)) {
            if (canUninstall) {
                this.mUninstallBlackListPkgNames.remove(packageName);
            } else {
                this.mUninstallBlackListPkgNames.add(packageName);
            }
        }
    }

    public boolean isAllowUninstallApp(String packageName) {
        return this.mUninstallBlackListPkgNames.contains(packageName) ^ 1;
    }

    /* JADX WARNING: Missing block: B:13:0x0034, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isDisallowedInstallApk(Package pkg) {
        boolean z = false;
        if (pkg == null || TextUtils.isEmpty(pkg.packageName) || pkg.mSigningDetails == null || !MIDDLEWARE_LIMITED_DPC_PKGS.equals(pkg.packageName)) {
            return false;
        }
        if (PackageManagerServiceUtils.compareSignatures(new Signature[]{new Signature(SYSTEM_SIGN_STR)}, pkg.mSigningDetails.signatures) != 0) {
            z = true;
        }
        return z;
    }

    private boolean isUserRestricted(int userId, String restrictionKey) {
        if (!UserManager.get(this.mContext).getUserRestrictions(UserHandle.of(userId)).getBoolean(restrictionKey, false)) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User is restricted: ");
        stringBuilder.append(restrictionKey);
        Slog.w(str, stringBuilder.toString());
        return true;
    }

    public void installPackageAsUser(String originPath, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, int userId) {
        IPackageInstallObserver2 iPackageInstallObserver2 = observer;
        int i = userId;
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int callingUid = Binder.getCallingUid();
        this.mIPmsInner.getPermissionManager().enforceCrossUserPermission(callingUid, i, true, true, "installPackageAsUser");
        if (isUserRestricted(i, "no_install_apps")) {
            if (iPackageInstallObserver2 != null) {
                try {
                    iPackageInstallObserver2.onPackageInstalled("", -111, null, null);
                } catch (RemoteException e) {
                }
            }
        } else if (!HwDeviceManager.disallowOp(6)) {
            int installFlags2;
            UserHandle userHandle;
            if (callingUid == 2000 || callingUid == 0) {
                installFlags2 = installFlags | 32;
            } else {
                installFlags2 = (installFlags & -33) & -65;
            }
            if ((installFlags2 & 64) != 0) {
                userHandle = UserHandle.ALL;
            } else {
                userHandle = new UserHandle(i);
            }
            UserHandle user = userHandle;
            if ((installFlags2 & 256) != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") == -1) {
                throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
            } else if ((installFlags2 & 1) == 0 && (installFlags2 & 8) == 0) {
                File originFile = new File(originPath);
                OriginInfo origin = OriginInfo.fromUntrustedFile(originFile);
                Message msg = this.mIPmsInner.getPackageHandler().obtainMessage(5);
                Message msg2 = msg;
                Message msg3 = msg2;
                msg3.obj = this.mIPmsInner.createInstallParams(origin, null, iPackageInstallObserver2, installFlags2, installerPackageName, null, new VerificationInfo(null, null, -1, callingUid), user, null, null, SigningDetails.UNKNOWN, 0);
                this.mIPmsInner.getPackageHandler().sendMessage(msg3);
            } else {
                throw new IllegalArgumentException("New installs into ASEC containers no longer supported");
            }
        }
    }

    public boolean isPrivilegedPreApp(File scanFile) {
        boolean z = false;
        if (scanFile == null) {
            return false;
        }
        String path;
        String codePath = scanFile.getAbsolutePath();
        if (codePath.endsWith(".apk")) {
            path = getCustPackagePath(codePath);
        } else {
            path = codePath;
        }
        if (path == null) {
            return false;
        }
        if (path.startsWith("/system/priv-app/")) {
            return true;
        }
        HashMap<String, HashSet<String>> mMultiInstallMap = this.mIPmsInner.getHwPMSMultiInstallMap();
        HashMap<String, HashSet<String>> mDelMultiInstallMap = this.mIPmsInner.getHwPMSDelMultiInstallMap();
        boolean normalDelMultiApp = mDelMultiInstallMap != null && ((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV)).contains(path);
        boolean normalMultiApp = mMultiInstallMap != null && ((HashSet) mMultiInstallMap.get(FLAG_APK_PRIV)).contains(path);
        if (normalDelMultiApp || normalMultiApp) {
            z = true;
        }
        return z;
    }

    public boolean isSystemPreApp(File scanFile) {
        boolean z = false;
        if (scanFile == null) {
            return false;
        }
        String path;
        String codePath = scanFile.getAbsolutePath();
        if (codePath.endsWith(".apk")) {
            path = getCustPackagePath(codePath);
        } else {
            path = codePath;
        }
        if (path == null) {
            return false;
        }
        HashMap<String, HashSet<String>> mMultiInstallMap = this.mIPmsInner.getHwPMSMultiInstallMap();
        HashMap<String, HashSet<String>> mDelMultiInstallMap = this.mIPmsInner.getHwPMSDelMultiInstallMap();
        boolean normalDelMultiApp = mDelMultiInstallMap != null && ((HashSet) mDelMultiInstallMap.get(FLAG_APK_SYS)).contains(path);
        boolean normalMultiApp = mMultiInstallMap != null && ((HashSet) mMultiInstallMap.get(FLAG_APK_SYS)).contains(path);
        if (normalDelMultiApp || normalMultiApp) {
            z = true;
        }
        return z;
    }

    private static String getCustPackagePath(String readLine) {
        int lastIndex = readLine.lastIndexOf(47);
        if (lastIndex > 0) {
            return readLine.substring(0, lastIndex);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAPKInstallList ERROR:  ");
        stringBuilder.append(readLine);
        Log.e(str, stringBuilder.toString());
        return null;
    }

    public void readPersistentConfig() {
        HwPersistentAppManager.readPersistentConfig();
    }

    public void resolvePersistentFlagForPackage(int oldFlags, Package pkg) {
        HwPersistentAppManager.resolvePersistentFlagForPackage(oldFlags, pkg);
    }

    public boolean isPersistentUpdatable(Package pkg) {
        return HwPersistentAppManager.isPersistentUpdatable(pkg);
    }

    public PackageInfo handlePackageNotFound(String packageName, int flag, int callingUid) {
        if (this.mHwFastAppManager != null) {
            return this.mHwFastAppManager.getPacakgeInfoForFastApp(packageName, flag, callingUid);
        }
        return null;
    }

    public void systemReady() {
        if (this.mHwFastAppManager != null) {
            this.mHwFastAppManager.systemReady();
        }
        setCurrentEmuiSysImgVersion();
        listenForUserSwitches();
    }

    public void handleActivityInfoNotFound(int flags, Intent intent, int callingUid, List<ResolveInfo> list) {
        if (this.mHwFastAppManager != null) {
            this.mHwFastAppManager.updateActivityInfo(flags, intent, callingUid, list);
        }
    }

    private void setCurrentEmuiSysImgVersion() {
        if (this.mHandler != null) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private static int deriveEmuiSysImgVersion() {
        String str;
        try {
            str = ManufactureNativeUtils.getVersionInfo(3);
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deriveEmuiSysImgVersion, version info is ");
            stringBuilder.append(str);
            Slog.d(str2, stringBuilder.toString());
            if (TextUtils.isEmpty(str)) {
                return 0;
            }
            str2 = "";
            Matcher matcher = Pattern.compile("(\\d+\\.){3}\\d+").matcher(str);
            if (matcher.find()) {
                str2 = matcher.group().trim();
            }
            String str3 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("deriveEmuiSysImgVersion,find:");
            stringBuilder2.append(str2);
            Slog.d(str3, stringBuilder2.toString());
            if (TextUtils.isEmpty(str2)) {
                return 0;
            }
            int version = Integer.parseInt(str2.replace(".", ""));
            String str4 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("deriveEmuiSysImgVersion,version:");
            stringBuilder3.append(version);
            Slog.d(str4, stringBuilder3.toString());
            return version;
        } catch (Exception e) {
            str = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("deriveEmuiSysImgVersion error:");
            stringBuilder4.append(e.getMessage());
            Slog.w(str, stringBuilder4.toString());
            return 0;
        }
    }

    public boolean isDisallowUninstallApk(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        String disallowUninstallPkgList = Secure.getString(this.mContext.getContentResolver(), "enterprise_disallow_uninstall_apklist");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isEnterpriseDisallowUninstallApk disallowUninstallPkgList : ");
        stringBuilder.append(disallowUninstallPkgList);
        Slog.w(str, stringBuilder.toString());
        if (!TextUtils.isEmpty(disallowUninstallPkgList)) {
            for (String pkg : disallowUninstallPkgList.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)) {
                if (packageName.equals(pkg)) {
                    String str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(packageName);
                    stringBuilder.append(" is in the enterprise Disallow UninstallApk blacklist!");
                    Slog.i(str2, stringBuilder.toString());
                    return true;
                }
            }
        }
        return false;
    }
}
