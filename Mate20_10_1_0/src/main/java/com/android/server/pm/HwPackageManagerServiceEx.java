package com.android.server.pm;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SynchronousUserSwitchObserver;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.HwPackageParser;
import android.content.pm.IHwPackageParser;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.hardware.display.HwFoldScreenState;
import android.hdm.HwDeviceManager;
import android.hwtheme.HwThemeManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBackupSessionCallback;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.perf.HwOptPackageParser;
import android.provider.Settings;
import android.securitydiagnose.HwSecurityDiagnoseManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.Flog;
import android.util.HwSlog;
import android.util.IMonitor;
import android.util.Log;
import android.util.PackageUtils;
import android.util.Slog;
import android.util.SplitNotificationUtils;
import android.util.Xml;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.ZygoteInit;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.UserIcons;
import com.android.internal.util.XmlUtils;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.am.HwActivityManagerService;
import com.android.server.cust.utils.ForbidShellFuncUtil;
import com.android.server.devicepolicy.HwAdminCache;
import com.android.server.displayside.HwDisplaySideRegionConfig;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.mplink.HwMpLinkServiceImpl;
import com.android.server.mtm.iaware.appmng.appstart.datamgr.AppStartupDataMgr;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.pm.BlackListInfo;
import com.android.server.pm.CertCompatSettings;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.pm.permission.BasePermission;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.android.server.pm.permission.PermissionsState;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.security.securityprofile.ISecurityProfileController;
import com.android.server.wifipro.WifiProCommonUtils;
import com.android.server.wm.utils.HwDisplaySizeUtil;
import com.huawei.android.app.HwActivityManager;
import com.huawei.android.content.pm.HwHepPackageInfo;
import com.huawei.android.content.pm.IExtServiceProvider;
import com.huawei.android.manufacture.ManufactureNativeUtils;
import com.huawei.cust.HwCustUtils;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import com.huawei.hsm.permission.StubController;
import com.huawei.permission.IHoldService;
import com.huawei.server.pm.antimal.AntiMalPreInstallScanner;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import huawei.android.app.admin.HwDevicePolicyManagerEx;
import huawei.com.android.server.security.fileprotect.HwAppAuthManager;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class HwPackageManagerServiceEx implements IHwPackageManagerServiceEx, IHwPackageManagerServiceExInner {
    private static final String ACCESS_SYSTEM_WHITE_LIST = "com.huawei.permission.ACCESS_SYSTEM_WHITE_LIST";
    private static final String ACTION_GET_PACKAGE_INSTALLATION_INFO = "com.huawei.android.action.GET_PACKAGE_INSTALLATION_INFO";
    private static final boolean ANTIMAL_DEBUG_ON = (SystemProperties.getInt(PROPERTY_ANTIMAL_DEBUG, 0) == 1);
    private static final String APK_DELETE_FILE = "/cust/ecota/xml/delete_system_app.txt";
    public static final int APP_FORCE_DARK_USER_SET_FLAG = 128;
    private static final boolean APP_INSTALL_AS_SYS_ALLOW = SystemProperties.getBoolean("ro.config.romUpgradeDataReserved", true);
    public static final int APP_SIDE_MODE_USER_SET_FLAG = 4;
    public static final int APP_USE_SIDE_MODE_EXPANDED = 1;
    public static final int APP_USE_SIDE_MODE_UNEXPANDED = 0;
    private static final String BROADCAST_PERMISSION = "com.android.permission.system_manager_interface";
    private static final String CACHE_BASE_DIR = "/data/system/package_cache/";
    private static final String CERT_TYPE_MEDIA = "media";
    private static final String CERT_TYPE_PLATFORM = "platform";
    private static final String CERT_TYPE_SHARED = "shared";
    private static final String CERT_TYPE_TESTKEY = "testkey";
    private static final String COTA_APK_XML_PATH = "xml/APKInstallListEMUI5Release.txt";
    private static final int COTA_APP_INSTALLING = -1;
    private static final String COTA_DEL_APK_XML_PATH = "xml/DelAPKInstallListEMUI5Release.txt";
    private static final boolean DEBUG = SystemProperties.get("ro.dbg.pms_log", "0").equals("on");
    private static final boolean DEBUG_DEXOPT_SHELL = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String DEVICE_POLICIES_XML = "device_policies.xml";
    private static final int DEVICE_TYPE_PC = 1;
    public static final int DPERMISSION_DEFAULT = 0;
    public static final int DPERMISSION_DENY = 2;
    public static final int DPERMISSION_GRANT = 1;
    private static final String ECOTA_VERSION = SystemProperties.get("ro.product.EcotaVersion", "");
    private static final String FLAG_APKPATCH_PATH = "/patch_hw/apk/apk_patch.xml";
    private static final String FLAG_APKPATCH_PKGNAME = "pkgname";
    private static final String FLAG_APKPATCH_TAG = "android.huawei.SYSTEM_APP_PATCH";
    private static final String FLAG_APKPATCH_TAGPATCH = "apkpatch";
    private static final String FLAG_APKPATCH_TAGVALUE = "value";
    private static final String FREE_FORM_LIST = "freeFormList";
    private static final String GMS_CORE_NAME = "com.google.android.gms";
    private static final int GUNSTALL_USER_STATE_ALLOW = 1;
    private static final int GUNSTALL_USER_STATE_AUTO = 0;
    private static final int GUNSTALL_USER_STATE_FORBID = 2;
    private static final String HW_PMS_GET_PCASSISTANT_RESULT = "com.huawei.permission.GET_PCASSISTANT_RESULT";
    private static final String HW_PMS_SET_APP_PERMISSION = "huawei.android.permission.SET_CANNOT_UNINSTALLED_PERMISSION";
    private static final String HW_PMS_SET_PCASSISTANT_RESULT = "com.huawei.permission.SET_PCASSISTANT_RESULT";
    public static final String HW_PRODUCT_DIR = "/hw_product";
    private static final String HW_SOUND_RECORDER = "com.android.soundrecorder.upgrade";
    private static final String INSTALLATION_EXTRA_INSTALLER_PACKAGE_NAME = "installerPackageName";
    private static final String INSTALLATION_EXTRA_PACKAGE_INSTALL_RESULT = "pkgInstallResult";
    private static final String INSTALLATION_EXTRA_PACKAGE_NAME = "pkgName";
    private static final String INSTALLATION_EXTRA_PACKAGE_UPDATE = "pkgUpdate";
    private static final String INSTALLATION_EXTRA_PACKAGE_URI = "pkgUri";
    private static final String INSTALLATION_EXTRA_PACKAGE_VERSION_CODE = "pkgVersionCode";
    private static final String INSTALLATION_EXTRA_PACKAGE_VERSION_NAME = "pkgVersionName";
    private static final int INVALID_VALUE = -1;
    private static final boolean IS_DEVICE_MAPLE_ENABLED = "1".equals(SystemProperties.get("ro.maple.enable", "0"));
    private static final boolean IS_HW_MULTIWINDOW_SUPPORTED = SystemProperties.getBoolean("ro.config.hw_multiwindow_optimization", false);
    private static final boolean IS_NOTCH_PROP = (!SystemProperties.get("ro.config.hw_notch_size", "").equals(""));
    private static final String KEY_IS_PROTECTED = "is_protected";
    private static final String KEY_PKG_NAME = "pkg_name";
    private static final int LEGAL_RECORD_NUM = 4;
    private static final Set<MySysAppInfo> MDM_SYSTEM_APPS = new HashSet();
    private static final Set<MySysAppInfo> MDM_SYSTEM_UNDETACHABLE_APPS = new HashSet();
    private static final String META_KEY_KEEP_ALIVE = "android.server.pm.KEEP_ALIVE";
    private static final String METHOD_HSM_INSTALL_UNIFIEDPOWERAPPS = "hsm_install_unifiedpowerapps";
    private static final String MIDDLEWARE_LIMITED_DPC_PKGS = "com.huawei.mdm.dpc";
    private static final int MSG_DEL_SOUNDRECORDER = 2;
    public static final int MSG_SET_CURRENT_EMUI_SYS_IMG_VERSION = 1;
    private static final int NORMAL_APP_TYPE = 0;
    static final long OTA_WAIT_DEXOPT_TIME = 480000;
    private static final int PHONE2PC_VERSION = 1;
    private static final String PLAY_STORE_NAME = "com.android.vending";
    private static final String POLICY_CHANGED_INTENT_ACTION = "com.huawei.devicepolicy.action.POLICY_CHANGED";
    public static final String PREINSTALLED_APK_LIST_DIR = "/data/system/";
    public static final String PREINSTALLED_APK_LIST_FILE = "preinstalled_app_list_file.xml";
    private static final int PRIVILEGE_APP_TYPE = 1;
    private static final String PROPERTY_ANTIMAL_DEBUG = "persist.sys.antimal.debug";
    private static final String REGIONAL_PHONE_SWITCH = SystemProperties.get("persist.sys.rpforpms", "");
    private static final int SCAN_AS_PRIVILEGED = 262144;
    public static final int SCAN_AS_SYSTEM = 131072;
    private static final int SCAN_BOOTING = 16;
    private static final int SCAN_FIRST_BOOT_OR_UPGRADE = 8192;
    private static final int SCAN_INITIAL = 512;
    private static final ArrayList<String> SCAN_INSTALL_CALLER_PACKAGES = new ArrayList<>(Arrays.asList("com.huawei.android.launcher", "com.huawei.hiassistant", "com.huawei.search", "com.huawei.tips", "android.uid.phone:1001", "com.huawei.appmarket", "com.huawei.gameassistant"));
    private static final String SEPARATOR = ":";
    private static final String SET_ASPECT_RATIO_PERMISSION = "com.huawei.permission.HW_SET_APPLICATION_ASPECT_RATIO";
    private static final String SIMPLE_COTA_APK_XML_PATH = "/data/cota/live_update/work/xml/APKInstallListEMUI5Release.txt";
    private static final String SIMPLE_COTA_DEL_APK_XML_PATH = "/data/cota/live_update/work/xml/DelAPKInstallListEMUI5Release.txt";
    private static final String SKIP_TRIGGER_FREEFORM = "com.huawei.permission.SKIP_TRIGGER_FREEFORM";
    public static final String SYSTEM_APP_DIR = "/system/app";
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    public static final String SYSTEM_PRIV_APP_DIR = "/system/priv-app";
    private static final String SYSTEM_SECURE = "ro.secure";
    private static final String SYSTEM_SIGN_STR = "30820405308202eda00302010202090083309550b47e0583300d06092a864886f70d0101050500308198310b300906035504061302434e3112301006035504080c094775616e67646f6e673112301006035504070c095368656e677a68656e310f300d060355040a0c0648756177656931183016060355040b0c0f5465726d696e616c436f6d70616e793114301206035504030c0b416e64726f69645465616d3120301e06092a864886f70d01090116116d6f62696c65406875617765692e636f6d301e170d3136303530353037333531345a170d3433303932313037333531345a308198310b300906035504061302434e3112301006035504080c094775616e67646f6e673112301006035504070c095368656e677a68656e310f300d060355040a0c0648756177656931183016060355040b0c0f5465726d696e616c436f6d70616e793114301206035504030c0b416e64726f69645465616d3120301e06092a864886f70d01090116116d6f62696c65406875617765692e636f6d30820122300d06092a864886f70d01010105000382010f003082010a0282010100c9fe1b699203091cb3944030cb1ba7996567182c1ce8be5535d673bc2025f37958e5bb1f4ed870dc229ffc2ed7d16f6cf10c08bc63f53624abe49db543518ef0069686ea5b3f129188652e87eca4b794df591828dd94de14b91ddbf2af156426453b8e739b12625a44b0895bfa1db3cdcce7db52f4d5af7c9918c325475c8273a5e4fe002e0f68082e9ec61d100913618982928ab5767701a8f576113c0810a4850a606233fd654531562bf8a74ac81bf8bacd66ca8a5ca9751f08e9575b402221e48e474f7f2dc91d02cfd87ceeaeb39ccf754cff5f1e8dfe23587955481bf0b8a386993edadc0f725e124f1ecedbef8d3cfbd6ddc783cde4b193f79fae05ed0203010001a350304e301d0603551d0e041604148d42132bfdc2ed970e25f5677cedd26f32527bc8301f0603551d230418301680148d42132bfdc2ed970e25f5677cedd26f32527bc8300c0603551d13040530030101ff300d06092a864886f70d010105050003820101003bc6e2ba8703a211222da8ed350e12cf31ac4d91290c5524da44626c382c8186f8238860b7ebddebba996f204802d72246d1326332ca85aff4a10cdaaa0d886016e26075c9b98799bf4767663d8c1097dccbc609dd3946f6431a35a71ee9ff3731c5b2715c158fe8d64c700b7e3e387e63a62e80ecdd4d007af242abed4b694d5a70d12dbde433fd18e1a7d033142f44cbe9ca187134830b86ecfa78ae2ff6d201014e4cf1d1655f40f4e4f4dd04af3c0416709dd159845d25515ff12f2854180e2ccbc1b05dffce93f9487839c126fa39f1453468a41eb7872b84c736dcb0d90a29775cd863707044f28bce4d05edcce4699605b27ae11e981590f87384726d";
    static final String TAG = "HwPackageManagerServiceEx";
    private static final String TAG_SPECIAL_POLICY_SYS_APP_LIST = "update-sys-app-install-list";
    private static final String TAG_SPECIAL_POLICY_UNDETACHABLE_SYS_APP_LIST = "update-sys-app-undetachable-install-list";
    private static final String TERMINATOR = ";";
    private static final String TME_CUSTOMIZE_SWITCH = SystemProperties.get("persist.sys.mccmnc", "");
    static final long WAIT_DEXOPT_TIME = 180000;
    private static final boolean isPerfHw_launcher = SystemProperties.getBoolean("ro.config.pref.hw_launcher", true);
    private static List<String> mCustStoppedApps = new ArrayList();
    private static List<String> preinstalledPackageList = new ArrayList();
    private boolean isBlackListExist = false;
    private BlackListInfo mBlackListInfo = new BlackListInfo();
    private boolean mBootCompleted = false;
    private final HandlerThread mCommonThread = new HandlerThread("PMSCommonThread");
    private CertCompatSettings mCompatSettings;
    final Context mContext;
    private Object mCust = null;
    private ArrayList<String> mDataApkShouldNotUpdateByCota = new ArrayList<>();
    private boolean mDexoptNow = false;
    private BlackListInfo mDisableAppListInfo = new BlackListInfo();
    private HashMap<String, BlackListInfo.BlackListApp> mDisableAppMap = new HashMap<>();
    private ExtServiceProvider mExtServiceProvider;
    private boolean mFoundCertCompatFile;
    private final HashSet<String> mGrantedInstalledPkg = new HashSet<>();
    final PackageExHandler mHandler;
    private HwFileBackupManager mHwFileBackupManager = null;
    private HwHepApplicationManager mHwHepApplicationManager = null;
    private HwOptPackageParser mHwOptPackageParser = null;
    IHwPackageManagerInner mIPmsInner = null;
    private Set<String> mIncompatNotificationList = new ArraySet();
    private final Set<String> mIncompatiblePkg = new ArraySet();
    private AtomicBoolean mIsOpting = new AtomicBoolean(false);
    PackageManagerService mPms = null;
    private final ArrayList<String> mScanInstallApkList = new ArrayList<>();
    /* access modifiers changed from: private */
    public final Object mSpeedOptLock = new Object();
    private ArraySet<String> mSpeedOptPkgs = new ArraySet<>();
    private HashSet<String> mUninstallBlackListPkgNames = new HashSet<>();
    /* access modifiers changed from: private */
    public long mUserSwitchingTime = 0;

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

        /* access modifiers changed from: package-private */
        public void doHandleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                PackageParser.setCurrentEmuiSysImgVersion(HwPackageManagerServiceEx.deriveEmuiSysImgVersion());
            } else if (i == 2) {
                HwPackageManagerServiceEx.this.deleteSoundercorderIfNeed();
            }
        }
    }

    public HwPackageManagerServiceEx(IHwPackageManagerInner pms, Context context) {
        this.mIPmsInner = pms;
        this.mContext = context;
        if (pms instanceof PackageManagerService) {
            this.mPms = (PackageManagerService) pms;
        }
        HotInstall.getInstance().setPackageManagerInner(pms);
        this.mHwOptPackageParser = HwFrameworkFactory.getHwOptPackageParser();
        this.mHwOptPackageParser.getOptPackages();
        if (!SystemProperties.getBoolean("ro.config.hwpmsthread.disable", false)) {
            this.mCommonThread.start();
            this.mHandler = new PackageExHandler(this.mCommonThread.getLooper());
        } else {
            this.mHandler = null;
        }
        MspesExUtil.getInstance(this).initMspesForbidInstallApps();
        MDM_SYSTEM_APPS.clear();
        MDM_SYSTEM_UNDETACHABLE_APPS.clear();
        readSysInfoFromDevicePolicyXml();
    }

    public boolean isPerfOptEnable(String packageName, int optType) {
        return this.mHwOptPackageParser.isPerfOptEnable(packageName, optType);
    }

    public void checkHwCertification(PackageParser.Package pkg, boolean isUpdate) {
        HwAppAuthManager.getInstance().checkFileProtect(pkg);
        if (HwCertificationManager.hasFeature()) {
            if (HwCertificationManager.isSupportHwCertification(pkg)) {
                boolean isUpgrade = this.mIPmsInner.isUpgrade();
                if (isUpdate || !isContainHwCertification(pkg) || isUpgrade) {
                    checkContainHwCert(pkg);
                }
            } else if (isContainHwCertification(pkg)) {
                cleanUpHwCert(pkg);
            }
        }
    }

    private void checkContainHwCert(PackageParser.Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager == null || manager.checkHwCertification(pkg)) {
        }
    }

    public boolean getHwCertPermission(boolean allowed, PackageParser.Package pkg, String perm) {
        if (allowed || !HwCertificationManager.hasFeature()) {
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

    private int getHwCertSignatureVersion(PackageParser.Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager == null) {
            return -1;
        }
        return manager.getHwCertSignatureVersion(pkg.packageName);
    }

    private void cleanUpHwCert(PackageParser.Package pkg) {
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
        HwCertificationManager.getIntance();
    }

    public int getHwCertificateType(PackageParser.Package pkg) {
        if (!HwCertificationManager.isSupportHwCertification(pkg)) {
            return HwCertificationManager.getIntance().getHwCertificateTypeNotMDM();
        }
        return HwCertificationManager.getIntance().getHwCertificateType(pkg.packageName);
    }

    public boolean isContainHwCertification(PackageParser.Package pkg) {
        return HwCertificationManager.getIntance().isContainHwCertification(pkg.packageName);
    }

    public boolean isAllowedSetHomeActivityForAntiMal(PackageInfo pi, int userId) {
        HwSecurityDiagnoseManager sdm = HwSecurityDiagnoseManager.getInstance();
        if (sdm == null || pi == null) {
            return true;
        }
        Bundle params = new Bundle();
        params.putString("pkg", pi.packageName);
        params.putInt("src", HwSecurityDiagnoseManager.AntiMalProtectLauncherType.PMS.ordinal());
        if (sdm.getAntimalProtectionPolicy(HwSecurityDiagnoseManager.AntiMalProtectType.LAUNCHER.ordinal(), params) != 1) {
            return true;
        }
        return false;
    }

    public void updateNotchScreenWhite(String packageName, String flag, int versionCode) {
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
                    return -1;
                }
                int appUseNotchMode = pkgSetting.getAppUseNotchMode();
                Binder.restoreCallingIdentity(callingId);
                return appUseNotchMode;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void setAppUseNotchMode(String packageName, int mode) {
        if (IS_NOTCH_PROP) {
            int uid = Binder.getCallingUid();
            if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    synchronized (this.mIPmsInner.getPackagesLock()) {
                        PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                        if (pkgSetting != null) {
                            if (pkgSetting.getAppUseNotchMode() != mode) {
                                pkgSetting.setAppUseNotchMode(mode);
                                this.mIPmsInner.getSettings().writeLPr();
                                HwNotchScreenWhiteConfig.getInstance().updateAppUseNotchMode(packageName, mode);
                            }
                            Binder.restoreCallingIdentity(callingId);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            } else {
                throw new SecurityException("Only the system can set app use notch mode");
            }
        }
    }

    public int getAppUseSideMode(String packageName) {
        if (!HwDisplaySizeUtil.hasSideInScreen()) {
            return -1;
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                if (pkgSetting == null) {
                    return -1;
                }
                int appUseSideMode = pkgSetting.getAppUseSideMode() & -5;
                Binder.restoreCallingIdentity(callingId);
                return appUseSideMode;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void setAppUseSideMode(String packageName, int mode) {
        if (HwDisplaySizeUtil.hasSideInScreen()) {
            int uid = Binder.getCallingUid();
            if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    synchronized (this.mIPmsInner.getPackagesLock()) {
                        PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                        if (pkgSetting != null) {
                            if ((pkgSetting.getAppUseSideMode() & -5) != mode) {
                                pkgSetting.setAppUseSideMode(mode | 4);
                                this.mIPmsInner.getSettings().writeLPr();
                                HwDisplaySideRegionConfig instance = HwDisplaySideRegionConfig.getInstance();
                                boolean z = true;
                                if (mode != 1) {
                                    z = false;
                                }
                                instance.updateExtendApp(packageName, z);
                            }
                            Binder.restoreCallingIdentity(callingId);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            } else {
                throw new SecurityException("Only the system can set app use side mode");
            }
        }
    }

    public void updateAppsUseSideWhitelist(ArrayMap<String, String> compressApps, ArrayMap<String, String> extendApps) {
        if (HwDisplaySizeUtil.hasSideInScreen()) {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                updateAppsSideMode(compressApps, 0);
                updateAppsSideMode(extendApps, 1);
                this.mIPmsInner.getSettings().writeLPr();
            }
        }
    }

    private void updateAppsSideMode(ArrayMap<String, String> apps, int mode) {
        if (apps == null || apps.size() == 0) {
            Slog.d(TAG, "apps is null");
            return;
        }
        for (String packageName : apps.keySet()) {
            PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
            if (pkgSetting == null) {
                Slog.d(TAG, "pkgSetting is null");
            } else {
                boolean isUserSet = (pkgSetting.getAppUseSideMode() & 4) != 0;
                int realMode = pkgSetting.getAppUseSideMode() & -5;
                Slog.d(TAG, "packageName: " + packageName + ", isUserSet: " + isUserSet + ", realMode: " + realMode + ", mode: " + mode);
                if (!isUserSet && realMode != mode) {
                    pkgSetting.setAppUseSideMode(mode);
                }
            }
        }
    }

    public List<String> getAppsUseSideList() {
        ArrayMap<String, PackageSetting> packageSettings = this.mIPmsInner.getSettings().mPackages;
        List<String> result = new ArrayList<>();
        for (String pkgname : packageSettings.keySet()) {
            if ((packageSettings.get(pkgname).getAppUseSideMode() & -5) == 1) {
                result.add(pkgname);
            }
        }
        return result;
    }

    public void updateUseSideMode(String pkgName, PackageSetting ps) {
        if (HwDisplaySizeUtil.hasSideInScreen()) {
            if (ps == null) {
                Slog.d(TAG, "updateUseSideMode ps is null for pkgName: " + pkgName);
                return;
            }
            String installVersion = null;
            String whiteListVersion = HwDisplaySideRegionConfig.getInstance().getAppVersionInWhiteList(pkgName);
            PackageInfo pInfo = this.mPms.getPackageInfo(pkgName, (int) AwarenessConstants.PHONE_STATE_CHANGED_ACTION, 0);
            if (pInfo != null) {
                installVersion = pInfo.versionName;
            }
            if (whiteListVersion == null || HwDisplaySideRegionConfig.getInstance().compareVersion(installVersion, whiteListVersion) < 0) {
                Slog.d(TAG, "updateUseSideMode UNEXPANDED pkgName: " + pkgName);
                ps.appUseSideMode = 0;
                HwDisplaySideRegionConfig.getInstance().updateExtendApp(pkgName, false);
                return;
            }
            Slog.d(TAG, "updateUseSideMode EXPANDED pkgName: " + pkgName);
            ps.appUseSideMode = 1;
            HwDisplaySideRegionConfig.getInstance().updateExtendApp(pkgName, true);
        }
    }

    public boolean setAllAppsUseSideMode(boolean isUse) {
        if (!HwDisplaySizeUtil.hasSideInScreen()) {
            return false;
        }
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
            long callingId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    int mode = isUse ? 1 : 0;
                    ArrayMap<String, PackageSetting> packageSettings = this.mIPmsInner.getSettings().mPackages;
                    for (String pkgName : packageSettings.keySet()) {
                        PackageSetting pkgSetting = packageSettings.get(pkgName);
                        if (pkgSetting != null) {
                            if ((pkgSetting.getAppUseSideMode() & -5) != mode) {
                                pkgSetting.setAppUseSideMode(mode | 4);
                                HwDisplaySideRegionConfig.getInstance().updateExtendApp(pkgName, mode == 1);
                            }
                        }
                    }
                    this.mIPmsInner.getSettings().writeLPr();
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        } else {
            throw new SecurityException("Only the system can set app use side mode");
        }
    }

    public boolean restoreAllAppsUseSideMode() {
        if (!HwDisplaySizeUtil.hasSideInScreen()) {
            return false;
        }
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
            long callingId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    ArrayMap<String, PackageSetting> packageSettings = this.mIPmsInner.getSettings().mPackages;
                    for (String pkgName : packageSettings.keySet()) {
                        PackageSetting pkgSetting = packageSettings.get(pkgName);
                        if (pkgSetting != null) {
                            if (HwDisplaySideRegionConfig.getInstance().isAppInWhiteList(pkgName)) {
                                pkgSetting.setAppUseSideMode(0);
                                HwDisplaySideRegionConfig.getInstance().updateExtendApp(pkgName, false);
                            } else {
                                pkgSetting.setAppUseSideMode(1);
                                HwDisplaySideRegionConfig.getInstance().updateExtendApp(pkgName, true);
                            }
                        }
                    }
                    this.mIPmsInner.getSettings().writeLPr();
                }
                return true;
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        } else {
            throw new SecurityException("Only the system can restore app use side mode");
        }
    }

    public boolean isAllAppsUseSideMode(List<String> packages) {
        if (!HwDisplaySizeUtil.hasSideInScreen()) {
            return false;
        }
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
            long callingId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    for (String pkgName : packages) {
                        PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(pkgName);
                        if (pkgSetting == null) {
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        } else if ((pkgSetting.getAppUseSideMode() & -5) == 0) {
                            return false;
                        }
                    }
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        } else {
            throw new SecurityException("Only the system can read app use side mode");
        }
    }

    private void listenForUserSwitches() {
        try {
            ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                /* class com.android.server.pm.HwPackageManagerServiceEx.AnonymousClass1 */

                public void onUserSwitching(int newUserId) throws RemoteException {
                    synchronized (HwPackageManagerServiceEx.this.mSpeedOptLock) {
                        long unused = HwPackageManagerServiceEx.this.mUserSwitchingTime = SystemClock.elapsedRealtime();
                        Slog.d(HwPackageManagerServiceEx.TAG, "onUserSwitching " + HwPackageManagerServiceEx.this.mUserSwitchingTime);
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

    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00b6, code lost:
        r18.mIsOpting.set(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00be, code lost:
        r6 = r18.mIPmsInner.performDexOptMode(r2, r20, "speed-profile", r22, r23, r24);
        r7 = r18.mSpeedOptLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00d0, code lost:
        monitor-enter(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00d7, code lost:
        if (r18.mSpeedOptPkgs.isEmpty() != false) goto L_0x00ef;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00db, code lost:
        if (r18.mDexoptNow != false) goto L_0x00de;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00de, code lost:
        r2 = r18.mSpeedOptPkgs.valueAt(0);
        r18.mSpeedOptPkgs.removeAt(0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00ec, code lost:
        monitor-exit(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00ef, code lost:
        monitor-exit(r7);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00f0, code lost:
        r18.mIsOpting.set(false);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00f5, code lost:
        return r6;
     */
    public boolean hwPerformDexOptMode(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force, boolean bootComplete, String splitName) {
        String packageName2 = packageName;
        synchronized (this.mSpeedOptLock) {
            if (!this.mBootCompleted) {
                this.mBootCompleted = SystemProperties.get("sys.boot_completed", "0").equals("1");
            }
            long elapsedTime = SystemClock.elapsedRealtime();
            if (!this.mDexoptNow) {
                this.mDexoptNow = elapsedTime > (this.mIPmsInner.isUpgrade() ? OTA_WAIT_DEXOPT_TIME : 180000);
            }
            if (this.mUserSwitchingTime != 0 && this.mDexoptNow) {
                this.mDexoptNow = false;
                if (elapsedTime > this.mUserSwitchingTime) {
                    this.mDexoptNow = elapsedTime - this.mUserSwitchingTime > WAIT_DEXOPT_TIME;
                }
            }
            Slog.i(TAG, "now " + elapsedTime + " optNow " + this.mDexoptNow + " upgrade " + this.mIPmsInner.isUpgrade() + " BootCompleted " + this.mBootCompleted + " UserSwitching " + this.mUserSwitchingTime);
            if (!this.mIsOpting.get() && this.mDexoptNow) {
                if (!this.mBootCompleted) {
                }
            }
            this.mSpeedOptPkgs.add(packageName2);
            Slog.d(TAG, "performDexOptMode add list " + packageName2 + " size " + this.mSpeedOptPkgs.size());
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
        return !this.mUninstallBlackListPkgNames.contains(packageName) && !MspesExUtil.getInstance(this).isForbidMspesUninstall(packageName);
    }

    public boolean isDisallowedInstallApk(PackageParser.Package pkg) {
        if (pkg == null || TextUtils.isEmpty(pkg.packageName) || pkg.mSigningDetails == null || !MIDDLEWARE_LIMITED_DPC_PKGS.equals(pkg.packageName)) {
            return false;
        }
        if (PackageManagerServiceUtils.compareSignatures(new Signature[]{new Signature(SYSTEM_SIGN_STR)}, pkg.mSigningDetails.signatures) != 0) {
            return true;
        }
        return false;
    }

    private boolean isUserRestricted(int userId, String restrictionKey) {
        if (!UserManager.get(this.mContext).getUserRestrictions(UserHandle.of(userId)).getBoolean(restrictionKey, false)) {
            return false;
        }
        Slog.w(TAG, "User is restricted: " + restrictionKey);
        return true;
    }

    private int redirectInstallForClone(int userId) {
        if (userId == 0 || !HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
            return userId;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo ui = PackageManagerService.sUserManager.getUserInfo(userId);
            if (ui != null && ui.isClonedProfile()) {
                return ui.profileGroupId;
            }
            Binder.restoreCallingIdentity(ident);
            return userId;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void installPackageAsUser(String originPath, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, int userId) {
        int installFlags2;
        UserHandle user;
        int userId2 = redirectInstallForClone(userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int callingUid = Binder.getCallingUid();
        this.mIPmsInner.getPermissionManager().enforceCrossUserPermission(callingUid, userId2, true, true, "installPackageAsUser");
        if (isUserRestricted(userId2, "no_install_apps")) {
            if (observer != null) {
                try {
                    observer.onPackageInstalled("", -111, (String) null, (Bundle) null);
                } catch (RemoteException e) {
                }
            }
        } else if (!HwDeviceManager.disallowOp(6)) {
            if (callingUid == 2000 || callingUid == 0) {
                installFlags2 = installFlags | 32;
            } else {
                installFlags2 = installFlags & -33 & -65;
            }
            if ((installFlags2 & 64) != 0) {
                user = UserHandle.ALL;
            } else {
                user = new UserHandle(userId2);
            }
            if ((installFlags2 & 256) == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") != -1) {
                PackageManagerService.OriginInfo origin = PackageManagerService.OriginInfo.fromUntrustedFile(new File(originPath));
                Message msg = this.mIPmsInner.getPackageHandler().obtainMessage(5);
                msg.obj = this.mIPmsInner.createInstallParams(origin, (PackageManagerService.MoveInfo) null, observer, installFlags2, installerPackageName, (String) null, new PackageManagerService.VerificationInfo((Uri) null, (Uri) null, -1, callingUid), user, (String) null, (String[]) null, PackageParser.SigningDetails.UNKNOWN, 0);
                this.mIPmsInner.getPackageHandler().sendMessage(msg);
                return;
            }
            throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
        }
    }

    public boolean isPrivilegedPreApp(File scanFile) {
        return HwPreAppManager.getInstance(this).isPrivilegedPreApp(scanFile);
    }

    public boolean isSystemPreApp(File scanFile) {
        return HwPreAppManager.getInstance(this).isSystemPreApp(scanFile);
    }

    public void readPersistentConfig() {
        HwPersistentAppManager.readPersistentConfig();
    }

    public void resolvePersistentFlagForPackage(int oldFlags, PackageParser.Package pkg) {
        HwPersistentAppManager.resolvePersistentFlagForPackage(oldFlags, pkg);
    }

    public boolean isPersistentUpdatable(PackageParser.Package pkg) {
        return HwPersistentAppManager.isPersistentUpdatable(pkg);
    }

    public void systemReady() {
        checkAndEnableWebview();
        CertCompatSettings certCompatSettings = this.mCompatSettings;
        if (certCompatSettings != null) {
            certCompatSettings.systemReady();
        }
        if (HwCertificationManager.hasFeature()) {
            if (!HwCertificationManager.isInitialized()) {
                HwCertificationManager.initialize(this.mContext);
            }
            HwCertificationManager.getIntance().systemReady();
        }
        try {
            initPackageBlackList();
        } catch (Exception e) {
            Slog.e(TAG, "initBlackList failed");
        }
        setCurrentEmuiSysImgVersion();
        listenForUserSwitches();
        deleteSoundrecorder();
        if (SystemProperties.getBoolean("ro.config.hw_mg_copyright", true)) {
            HwThemeInstaller.getInstance(this.mContext).createMagazineFolder();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.huawei.devicepolicy.action.POLICY_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.android.server.pm.HwPackageManagerServiceEx.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                Bundle bundle;
                String policyName = intent.getStringExtra("policy_name");
                Slog.d(HwPackageManagerServiceEx.TAG, "devicepolicy.action.POLICY_CHANGED policyName:" + policyName);
                if ((HwPackageManagerServiceEx.TAG_SPECIAL_POLICY_UNDETACHABLE_SYS_APP_LIST.equals(policyName) || HwPackageManagerServiceEx.TAG_SPECIAL_POLICY_SYS_APP_LIST.equals(policyName)) && (bundle = intent.getExtras()) != null) {
                    String value = bundle.getString("value");
                    Slog.d(HwPackageManagerServiceEx.TAG, "value:" + value);
                    HwPackageManagerServiceEx.this.updateSysAppInfoList(value, policyName);
                }
            }
        }, intentFilter, "android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS", null);
        new Handler().postDelayed(new Runnable() {
            /* class com.android.server.pm.HwPackageManagerServiceEx.AnonymousClass3 */

            public void run() {
                new HwPileApplicationManager(HwPackageManagerServiceEx.this.mContext).startInstallPileApk();
            }
        }, 1000);
    }

    /* access modifiers changed from: private */
    public void updateSysAppInfoList(String currentValue, String policyName) {
        Slog.d(TAG, "updateSysAppInfoList currentValue:" + currentValue);
        if (!TextUtils.isEmpty(currentValue)) {
            if (TAG_SPECIAL_POLICY_UNDETACHABLE_SYS_APP_LIST.equals(policyName)) {
                MDM_SYSTEM_UNDETACHABLE_APPS.clear();
                String[] split = currentValue.split(";");
                int length = split.length;
                int i = 0;
                while (i < length) {
                    String[] infoList = split[i].split(":");
                    if (infoList.length == 4) {
                        MDM_SYSTEM_UNDETACHABLE_APPS.add(new MySysAppInfo(infoList[0], infoList[1], infoList[2], infoList[3]));
                        i++;
                    } else {
                        return;
                    }
                }
            } else if (TAG_SPECIAL_POLICY_SYS_APP_LIST.equals(policyName)) {
                MDM_SYSTEM_APPS.clear();
                String[] split2 = currentValue.split(";");
                int length2 = split2.length;
                int i2 = 0;
                while (i2 < length2) {
                    String[] infoList2 = split2[i2].split(":");
                    if (infoList2.length == 4) {
                        MDM_SYSTEM_APPS.add(new MySysAppInfo(infoList2[0], infoList2[1], infoList2[2], infoList2[3]));
                        i2++;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public int adjustScanFlagForApk(PackageParser.Package pkg, int scanFlags) {
        int result = scanFlags;
        if (APP_INSTALL_AS_SYS_ALLOW && isValidPackage(pkg)) {
            MySysAppInfo target = getRecordFromCache(MDM_SYSTEM_APPS, pkg.packageName, sha256(pkg.mSigningDetails.signatures[0].toByteArray()));
            if (target == null) {
                target = getRecordFromCache(MDM_SYSTEM_UNDETACHABLE_APPS, pkg.packageName, sha256(pkg.mSigningDetails.signatures[0].toByteArray()));
            }
            if (target != null) {
                if (target.getPrivileged()) {
                    Slog.d(TAG, "add privileged for " + pkg.packageName);
                    result = result | 262144 | 131072;
                } else {
                    Slog.d(TAG, "add sytem for " + pkg.packageName);
                    result |= 131072;
                }
                if (!target.getUndetachable()) {
                    Slog.d(TAG, "add del for " + pkg.packageName);
                    ApplicationInfo applicationInfo = pkg.applicationInfo;
                    applicationInfo.hwFlags = applicationInfo.hwFlags | 33554432;
                }
            }
        }
        return result;
    }

    public boolean isSystemAppGrantByMdm(PackageParser.Package pkg) {
        if (APP_INSTALL_AS_SYS_ALLOW && isValidPackage(pkg)) {
            MySysAppInfo target = getRecordFromCache(MDM_SYSTEM_APPS, pkg.packageName, sha256(pkg.mSigningDetails.signatures[0].toByteArray()));
            if (target == null) {
                target = getRecordFromCache(MDM_SYSTEM_UNDETACHABLE_APPS, pkg.packageName, sha256(pkg.mSigningDetails.signatures[0].toByteArray()));
            }
            if (target != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isSystemAppGrantByMdm(String pkgName) {
        if (!APP_INSTALL_AS_SYS_ALLOW || TextUtils.isEmpty(pkgName)) {
            return false;
        }
        MySysAppInfo target = getRecordFromCache(MDM_SYSTEM_APPS, pkgName, null);
        if (target == null) {
            target = getRecordFromCache(MDM_SYSTEM_UNDETACHABLE_APPS, pkgName, null);
        }
        if (target != null) {
            return true;
        }
        return false;
    }

    public void updateDozeList(String packageName, boolean isProtect) {
        if (!TextUtils.isEmpty(packageName)) {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_PKG_NAME, packageName);
            bundle.putInt(KEY_IS_PROTECTED, isProtect ? 1 : 0);
            try {
                IHoldService service = StubController.getHoldService();
                if (service == null) {
                    Slog.e(TAG, "hsm_install_unifiedpowerapps service is null!");
                } else {
                    service.callHsmService(METHOD_HSM_INSTALL_UNIFIEDPOWERAPPS, bundle);
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "unable to reach HSM service");
            } catch (Exception e2) {
                Slog.e(TAG, "failed to call HSM service ");
            }
        }
    }

    private boolean isValidPackage(PackageParser.Package pkg) {
        if ((MDM_SYSTEM_APPS.isEmpty() && MDM_SYSTEM_UNDETACHABLE_APPS.isEmpty()) || pkg == null || TextUtils.isEmpty(pkg.packageName) || pkg.mSigningDetails == null || pkg.mSigningDetails.signatures == null || pkg.mSigningDetails.signatures.length < 1 || pkg.mSigningDetails.signatures[0] == null) {
            return false;
        }
        return true;
    }

    private String sha256(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA-256");
            mDigest.update(data);
            return bytesToString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] chars = new char[(bytes.length * 2)];
        for (int j = 0; j < bytes.length; j++) {
            int byteValue = bytes[j] & 255;
            chars[j * 2] = hexChars[byteValue >>> 4];
            chars[(j * 2) + 1] = hexChars[byteValue & 15];
        }
        return new String(chars).toUpperCase(Locale.ENGLISH);
    }

    private MySysAppInfo getRecordFromCache(Set<MySysAppInfo> cacheApps, String pkgName, String pkgSignature) {
        if (cacheApps == null || cacheApps.isEmpty()) {
            return null;
        }
        for (MySysAppInfo app : cacheApps) {
            if (app.getPkgName().equalsIgnoreCase(pkgName) && (pkgSignature == null || app.getPkgSignature().equalsIgnoreCase(pkgSignature))) {
                return app;
            }
        }
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:10:0x0033 A[Catch:{ FileNotFoundException -> 0x0086, XmlPullParserException -> 0x0079, IOException -> 0x006c, all -> 0x006a }] */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0042  */
    private void readSysInfoFromDevicePolicyXml() {
        int type;
        FileInputStream inputStr = null;
        File sysPackageFile = new File(Environment.getDataSystemDirectory(), DEVICE_POLICIES_XML);
        if (sysPackageFile.exists()) {
            try {
                FileInputStream inputStr2 = new FileInputStream(sysPackageFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(inputStr2, null);
                while (true) {
                    type = parser.next();
                    if (type == 2 || type == 1) {
                        if (type == 2) {
                            Slog.e(TAG, "No start tag found in package file");
                            try {
                                inputStr2.close();
                                return;
                            } catch (IOException e) {
                                Slog.e(TAG, "Unable to close the inputStr");
                                return;
                            }
                        } else {
                            int outerDepth = parser.getDepth();
                            while (true) {
                                int type2 = parser.next();
                                if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                                    try {
                                        inputStr2.close();
                                        return;
                                    } catch (IOException e2) {
                                        Slog.e(TAG, "Unable to close the inputStr");
                                        return;
                                    }
                                } else if (!(type2 == 3 || type2 == 4)) {
                                    parsePolicyFile(parser);
                                }
                            }
                        }
                    }
                }
                if (type == 2) {
                }
            } catch (FileNotFoundException e3) {
                Slog.e(TAG, "FileNotFoundException when try to parse device_policies");
                if (0 != 0) {
                    inputStr.close();
                }
            } catch (XmlPullParserException e4) {
                Slog.e(TAG, "XmlPullParserException when try to parse device_policies");
                if (0 != 0) {
                    inputStr.close();
                }
            } catch (IOException e5) {
                Slog.e(TAG, "IOException when try to parse device_policies");
                if (0 != 0) {
                    inputStr.close();
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        inputStr.close();
                    } catch (IOException e6) {
                        Slog.e(TAG, "Unable to close the inputStr");
                    }
                }
                throw th;
            }
        }
    }

    private void parsePolicyFile(XmlPullParser parser) {
        String tagName = parser.getName();
        char c = 2;
        int i = 4;
        char c2 = 1;
        char c3 = 0;
        if (TAG_SPECIAL_POLICY_SYS_APP_LIST.equals(tagName)) {
            String pkgInfoList = XmlUtils.readStringAttribute(parser, "value");
            if (!TextUtils.isEmpty(pkgInfoList)) {
                String[] split = pkgInfoList.split(";");
                int length = split.length;
                int i2 = 0;
                while (i2 < length) {
                    String[] infoList = split[i2].split(":");
                    if (infoList.length == 4) {
                        MDM_SYSTEM_APPS.add(new MySysAppInfo(infoList[0].intern(), infoList[c2], infoList[c], infoList[3]));
                    }
                    i2++;
                    c = 2;
                    c2 = 1;
                }
                Slog.d(TAG, "read " + pkgInfoList);
            }
        } else if (TAG_SPECIAL_POLICY_UNDETACHABLE_SYS_APP_LIST.equals(tagName)) {
            String pkgInfoList2 = XmlUtils.readStringAttribute(parser, "value");
            if (!TextUtils.isEmpty(pkgInfoList2)) {
                String[] split2 = pkgInfoList2.split(";");
                int length2 = split2.length;
                int i3 = 0;
                while (i3 < length2) {
                    String[] infoList2 = split2[i3].split(":");
                    if (infoList2.length == i) {
                        MDM_SYSTEM_UNDETACHABLE_APPS.add(new MySysAppInfo(infoList2[c3].intern(), infoList2[1], infoList2[2], infoList2[3]));
                    }
                    i3++;
                    i = 4;
                    c3 = 0;
                }
                Slog.d(TAG, "read " + pkgInfoList2);
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean hasOtaUpdate() {
        try {
            UserInfo userInfo = PackageManagerService.sUserManager.getUserInfo(0);
            if (userInfo == null) {
                return false;
            }
            Log.i(TAG, "userInfo.lastLoggedInFingerprint : " + userInfo.lastLoggedInFingerprint + ", Build.FINGERPRINT : " + Build.FINGERPRINT + "userInfo.lastLoggedInFingerprintEx : " + userInfo.lastLoggedInFingerprintEx + ", Build.FINGERPRINTEX : " + Build.FINGERPRINTEX);
            if (!Objects.equals(userInfo.lastLoggedInFingerprint, Build.FINGERPRINT) || !Objects.equals(userInfo.lastLoggedInFingerprintEx, Build.FINGERPRINTEX)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.i(TAG, "hasOtaUpdate catch Exception");
            return false;
        }
    }

    public void filterShellApps(ArrayList<PackageParser.Package> pkgs, LinkedList<PackageParser.Package> sortedPkgs) {
        Installer mInstaller = this.mIPmsInner.getInstallerInner();
        if ((!hasOtaUpdate() || this.mIPmsInner.isFirstBootInner()) && !hasPrunedDalvikCache()) {
            Slog.i(TAG, "Do not filt shell Apps! not OTA case.");
            return;
        }
        HwShellAppsHandler handler = new HwShellAppsHandler(mInstaller);
        Iterator<PackageParser.Package> it = pkgs.iterator();
        while (it.hasNext()) {
            PackageParser.Package pkg = it.next();
            String shellName = handler.analyseShell(pkg);
            if (shellName != null) {
                if (DEBUG_DEXOPT_SHELL) {
                    Log.i(TAG, "Find a " + shellName + " Shell Pkgs: " + pkg.packageName);
                }
                sortedPkgs.add(pkg);
                handler.processShellApp(pkg);
            }
        }
        pkgs.removeAll(sortedPkgs);
    }

    private boolean hasPrunedDalvikCache() {
        if (new File(Environment.getDataDirectory(), "system/.dalvik-cache-pruned").exists()) {
            return true;
        }
        return false;
    }

    public boolean isMDMDisallowedInstallPackage(PackageParser.Package pkg, PackageManagerService.PackageInstalledInfo res) {
        if (!pkg.applicationInfo.isSystemApp() && HwDeviceManager.disallowOp(7, pkg.packageName)) {
            UiThread.getHandler().post(new Runnable() {
                /* class com.android.server.pm.HwPackageManagerServiceEx.AnonymousClass4 */

                public void run() {
                    Toast toast = Toast.makeText(HwPackageManagerServiceEx.this.mContext, HwPackageManagerServiceEx.this.mContext.getString(33685978), 0);
                    toast.getWindowParams().privateFlags |= 16;
                    toast.show();
                }
            });
            res.setError((int) RequestStatus.SYS_ETIMEDOUT, "app is not in the installpackage_whitelist");
            return true;
        } else if (pkg.applicationInfo.isSystemApp() || !HwDeviceManager.disallowOp(19, pkg.packageName)) {
            return false;
        } else {
            final String pkgName = getApplicationLabel(this.mContext, pkg);
            new Handler().postDelayed(new Runnable() {
                /* class com.android.server.pm.HwPackageManagerServiceEx.AnonymousClass5 */

                public void run() {
                    Toast.makeText(HwPackageManagerServiceEx.this.mContext, HwPackageManagerServiceEx.this.mContext.getResources().getString(33685933, pkgName), 0).show();
                }
            }, 500);
            res.setError((int) RequestStatus.SYS_ETIMEDOUT, "app is in the installpackage_blacklist");
            return true;
        }
    }

    public static String getApplicationLabel(Context context, PackageParser.Package pkg) {
        PackageManager pm = context.getPackageManager();
        String displayName = pkg.packageName;
        if (pm != null) {
            return String.valueOf(pm.getApplicationLabel(pkg.applicationInfo));
        }
        return displayName;
    }

    public ResolveInfo hwFindPreferredActivity(Intent intent, List<ResolveInfo> query) {
        boolean mIsDefaultPreferredActivityChanged = this.mIPmsInner.getIsDefaultPreferredActivityChangedInner();
        HwCustPackageManagerService mCustPackageManagerService = this.mIPmsInner.getHwPMSCustPackageManagerService();
        boolean mIsDefaultGoogleCalendar = this.mIPmsInner.getIsDefaultGoogleCalendarInner();
        if (intent.hasCategory("android.intent.category.HOME") && query != null && query.size() > 1) {
            if (!isPerfHw_launcher) {
                int num = query.size() - 1;
                List<String> whiteListLauncher = new ArrayList<>();
                whiteListLauncher.add("com.huawei.android.launcher");
                whiteListLauncher.add("com.huawei.kidsmode");
                whiteListLauncher.add("com.android.settings");
                while (num >= 0) {
                    int num2 = num - 1;
                    ResolveInfo info = query.get(num);
                    if (info.activityInfo == null || whiteListLauncher.contains(info.activityInfo.applicationInfo.packageName)) {
                        num = num2;
                    } else {
                        HwSlog.v(TAG, "return default Launcher null");
                        return null;
                    }
                }
            }
            int index = query.size() - 1;
            while (index >= 0) {
                int index2 = index - 1;
                ResolveInfo info2 = query.get(index);
                String defaultLauncher = "com.huawei.android.launcher";
                if (!(mCustPackageManagerService == null || info2.activityInfo == null)) {
                    String custDefaultLauncher = mCustPackageManagerService.getCustDefaultLauncher(this.mContext, info2.activityInfo.applicationInfo.packageName);
                    if (!TextUtils.isEmpty(custDefaultLauncher)) {
                        defaultLauncher = custDefaultLauncher;
                    }
                }
                if (info2.activityInfo == null || !info2.activityInfo.applicationInfo.packageName.equals(defaultLauncher)) {
                    index = index2;
                } else {
                    HwSlog.v(TAG, "Returning system default Launcher ");
                    return info2;
                }
            }
        }
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.DIAL") && intent.getData() != null && intent.getData().getScheme() != null && intent.getData().getScheme().equals("tel") && query != null && query.size() > 1) {
            return getSpecificPreferredActivity(query, true, HwThemeInstaller.HWT_OLD_CONTACT);
        }
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.VIEW") && intent.getData() != null && intent.getData().getScheme() != null && ((intent.getData().getScheme().equals("file") || intent.getData().getScheme().equals("content")) && intent.getType() != null && intent.getType().startsWith("image/") && query != null && query.size() > 1)) {
            ResolveInfo info3 = getSpecificPreferredActivity(query, false, "com.android.gallery3d");
            if (info3 == null) {
                return getSpecificPreferredActivity(query, false, "com.huawei.photos");
            }
            return info3;
        } else if (intent.getAction() != null && intent.getAction().equals("android.intent.action.VIEW") && intent.getData() != null && intent.getData().getScheme() != null && ((intent.getData().getScheme().equals("file") || intent.getData().getScheme().equals("content")) && intent.getType() != null && intent.getType().startsWith("audio/") && query != null && query.size() > 1)) {
            ResolveInfo info4 = getSpecificPreferredActivity(query, false, "com.android.mediacenter");
            if (info4 == null) {
                return getSpecificPreferredActivity(query, false, "com.huawei.music");
            }
            return info4;
        } else if (intent.getAction() != null && intent.getAction().equals("android.media.action.IMAGE_CAPTURE") && query != null && query.size() > 1) {
            return getSpecificPreferredActivity(query, false, "com.huawei.camera");
        } else {
            if (intent.getAction() == null || !intent.getAction().equals("android.intent.action.VIEW") || intent.getData() == null || intent.getData().getScheme() == null || !intent.getData().getScheme().equals("mailto") || query == null || query.size() <= 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("!mIsDefaultPreferredActivityChanged= ");
                sb.append(!mIsDefaultPreferredActivityChanged);
                sb.append(" ,mIsDefaultGoogleCalendar= ");
                sb.append(mIsDefaultGoogleCalendar);
                Log.i(TAG, sb.toString());
                if (!mIsDefaultPreferredActivityChanged && mIsDefaultGoogleCalendar && isCalendarType(intent) && query != null && query.size() > 1) {
                    return getSpecificPreferredActivity(query, true, "com.google.android.calendar");
                }
                if (intent.getAction() == null || !intent.getAction().equals("android.intent.action.VIEW") || intent.getData() == null || !intent.getData().toString().startsWith("market://details") || intent.getData().getScheme() == null || !intent.getData().getScheme().equals("market") || query == null || query.size() <= 1) {
                    return null;
                }
                String default_appmarket = Settings.Global.getString(this.mContext.getContentResolver(), "default_appmarket");
                Log.i(TAG, "find default appmarket is : " + default_appmarket);
                return getSpecificPreferredActivity(query, false, default_appmarket);
            } else if (intent.getCategories() != null && intent.getCategories().size() > 0) {
                return null;
            } else {
                ResolveInfo info5 = getSpecificPreferredActivity(query, false, "com.android.email");
                if (info5 == null) {
                    return getSpecificPreferredActivity(query, false, "com.huawei.email");
                }
                return info5;
            }
        }
    }

    private ResolveInfo getSpecificPreferredActivity(List<ResolveInfo> query, boolean checkPriorityFlag, String specificPackageName) {
        int index = query.size() - 1;
        while (index >= 0) {
            int index2 = index - 1;
            ResolveInfo info = query.get(index);
            if ((!checkPriorityFlag || info.priority >= 0) && info.activityInfo != null && info.activityInfo.applicationInfo.packageName.equals(specificPackageName)) {
                return info;
            }
            index = index2;
        }
        return null;
    }

    private boolean isCalendarType(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "isCalendarType, intent or action is null, return false");
            return false;
        }
        String action = intent.getAction();
        Uri data = intent.getData();
        String type = intent.getType();
        if (("android.intent.action.EDIT".equals(action) || "android.intent.action.INSERT".equals(action) || "android.intent.action.VIEW".equals(action)) && "vnd.android.cursor.item/event".equals(type)) {
            return true;
        }
        if (("android.intent.action.EDIT".equals(action) || "android.intent.action.INSERT".equals(action)) && "vnd.android.cursor.dir/event".equals(type)) {
            return true;
        }
        if ("android.intent.action.VIEW".equals(action) && data != null && ("http".equals(data.getScheme()) || "https".equals(data.getScheme())) && "www.google.com".equals(data.getHost()) && data.getPath() != null && (data.getPath().startsWith("/calendar/event") || (data.getPath().startsWith("/calendar/hosted") && data.getPath().endsWith("/event")))) {
            return true;
        }
        if ("android.intent.action.VIEW".equals(action) && "text/calendar".equals(type)) {
            return true;
        }
        if ("android.intent.action.VIEW".equals(action) && "time/epoch".equals(type)) {
            return true;
        }
        if ("android.intent.action.VIEW".equals(action) && data != null && "content".equals(data.getScheme()) && ("com.android.calendar".equals(data.getHost()) || "com.huawei.calendar".equals(data.getHost()))) {
            return true;
        }
        return false;
    }

    private static boolean firstScan() {
        boolean exists = new File(Environment.getDataDirectory(), "system/packages.xml").exists();
        StringBuilder sb = new StringBuilder();
        sb.append("is first scan?");
        sb.append(!exists);
        Slog.i(TAG, sb.toString());
        return !exists;
    }

    private int startBackupSession(IBackupSessionCallback callback) {
        getHwFileBackupManager();
        Slog.i(TAG, "application bind call startBackupSession");
        if (!checkBackupSessionCaller()) {
            return -2;
        }
        return this.mHwFileBackupManager.startBackupSession(callback);
    }

    private int executeBackupTask(int sessionId, String taskCmd) {
        getHwFileBackupManager();
        Slog.i(TAG, "bind call executeBackupTask on session:" + sessionId);
        if (!checkBackupSessionCaller()) {
            return -2;
        }
        return this.mHwFileBackupManager.executeBackupTask(sessionId, this.mHwFileBackupManager.prepareBackupTaskCmd(taskCmd, this.mIPmsInner.getPackagesLock()));
    }

    private int finishBackupSession(int sessionId) {
        getHwFileBackupManager();
        Slog.i(TAG, "bind call finishBackupSession sessionId:" + sessionId);
        if (!checkBackupSessionCaller()) {
            return -2;
        }
        return this.mHwFileBackupManager.finishBackupSession(sessionId);
    }

    private boolean checkBackupSessionCaller() {
        getHwFileBackupManager();
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1001) {
            return true;
        }
        String pkgName = this.mIPmsInner.getNameForUidInner(callingUid);
        if (!this.mHwFileBackupManager.checkBackupPackageName(pkgName) || !isPlatformSignatureApp(pkgName)) {
            return false;
        }
        return true;
    }

    @Override // com.android.server.pm.IHwPackageManagerServiceExInner
    public boolean isPlatformSignatureApp(String pkgName) {
        boolean result = this.mIPmsInner.checkSignaturesInner(AppStartupDataMgr.HWPUSH_PKGNAME, pkgName) == 0;
        if (!result) {
            Slog.d(TAG, "is not platform signature app, pkgName is " + pkgName);
        }
        return result;
    }

    private void getHwFileBackupManager() {
        if (this.mHwFileBackupManager == null) {
            this.mHwFileBackupManager = HwFileBackupManager.getInstance(this.mIPmsInner.getInstallerInner());
        }
    }

    private void getAPKInstallList(List<File> apkInstallLists, HashMap<String, HashSet<String>> multiInstallMap) {
        HwPreAppManager.getInstance(this).getMultiApkInstallList(apkInstallLists, multiInstallMap);
    }

    private void installAPKforInstallList(HashSet<String> installList, int flags, int scanMode, long currentTime) {
        installAPKforInstallList(installList, flags, scanMode, currentTime, 0);
    }

    @Override // com.android.server.pm.IHwPackageManagerServiceExInner
    public void installAPKforInstallList(HashSet<String> installList, int parseFlags, int scanFlags, long currentTime, int hwFlags) {
        if (installList != null && installList.size() != 0) {
            if (this.mIPmsInner.getCotaFlagInner()) {
                this.mIPmsInner.setHwPMSCotaApksInstallStatus(-1);
            }
            int fileSize = installList.size();
            File[] files = new File[fileSize];
            Iterator<String> it = installList.iterator();
            int i = 0;
            while (it.hasNext()) {
                String installPath = it.next();
                File file = new File(installPath);
                if (i < fileSize) {
                    files[i] = file;
                    Flog.i(205, "add package install path : " + installPath);
                    i++;
                } else {
                    Slog.w(TAG, "faile to add package install path : " + installPath + "fileSize:" + fileSize + ",i:" + i);
                }
            }
            this.mIPmsInner.scanPackageFilesLIInner(files, parseFlags, scanFlags, currentTime, hwFlags);
        }
    }

    public boolean isDelappInData(PackageSetting ps) {
        return HwPreAppManager.getInstance(this).isDelappInData(ps);
    }

    public boolean isUninstallApk(String filePath) {
        return HwForbidUninstallManager.getInstance(this).isUninstallApk(filePath);
    }

    public void getUninstallApk() {
        long startTime = HwPackageManagerServiceUtils.hwTimingsBegin();
        HwForbidUninstallManager.getInstance(this).getUninstallApk();
        HwPackageManagerServiceUtils.hwTimingsEnd(TAG, "getUninstallApk", startTime);
    }

    @Override // com.android.server.pm.IHwPackageManagerServiceExInner
    public synchronized HwCustPackageManagerService getCust() {
        if (this.mCust == null) {
            this.mCust = HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]);
        }
        return (HwCustPackageManagerService) this.mCust;
    }

    public boolean isHwCustHiddenInfoPackage(PackageParser.Package pkgInfo) {
        if (getCust() != null) {
            return getCust().isHwCustHiddenInfoPackage(pkgInfo);
        }
        return false;
    }

    public void setUpCustomResolverActivity(PackageParser.Package pkg) {
        synchronized (this.mIPmsInner.getPackagesLock()) {
            ActivityInfo mResolveActivity = this.mIPmsInner.getResolveActivityInner();
            this.mIPmsInner.setUpCustomResolverActivityInner(pkg);
            if (!TextUtils.isEmpty(HwFrameworkFactory.getHuaweiResolverActivity(this.mContext))) {
                mResolveActivity.processName = "system:ui";
                mResolveActivity.theme = 16974858;
            }
        }
    }

    public static boolean firstScanForHwPMS() {
        return firstScan();
    }

    public int getStartBackupSession(IBackupSessionCallback callback) {
        return startBackupSession(callback);
    }

    public int getExecuteBackupTask(int sessionId, String taskCmd) {
        return executeBackupTask(sessionId, taskCmd);
    }

    public int getFinishBackupSession(int sessionId) {
        return finishBackupSession(sessionId);
    }

    public void getAPKInstallListForHwPMS(List<File> apkInstallLists, HashMap<String, HashSet<String>> multiInstallMap) {
        getAPKInstallList(apkInstallLists, multiInstallMap);
    }

    public void installAPKforInstallListForHwPMS(HashSet<String> installList, int flags, int scanMode, long currentTime) {
        installAPKforInstallList(installList, flags, scanMode, currentTime);
    }

    public void installAPKforInstallListForHwPMS(HashSet<String> installList, int parseFlags, int scanFlags, long currentTime, int hwFlags) {
        installAPKforInstallList(installList, parseFlags, scanFlags, currentTime, hwFlags);
    }

    private void initPackageBlackList() {
        boolean isCompleteProcess;
        BlackListAppsUtils.readBlackList(this.mBlackListInfo);
        synchronized (this.mIPmsInner.getPackagesLock()) {
            BlackListAppsUtils.readDisableAppList(this.mDisableAppListInfo);
            Iterator<BlackListInfo.BlackListApp> it = this.mDisableAppListInfo.mBlackList.iterator();
            while (it.hasNext()) {
                BlackListInfo.BlackListApp app = it.next();
                this.mDisableAppMap.put(app.mPackageName, app);
            }
        }
        this.isBlackListExist = (this.mBlackListInfo.mBlackList.size() == 0 || this.mBlackListInfo.mVersionCode == -1) ? false : true;
        if (!this.isBlackListExist) {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                if (this.mDisableAppMap.size() > 0) {
                    Slog.i(TAG, "blacklist not exists, enable all disabled apps");
                    Set<String> needEnablePackage = new ArraySet<>();
                    for (Map.Entry<String, BlackListInfo.BlackListApp> entry : this.mDisableAppMap.entrySet()) {
                        needEnablePackage.add(entry.getKey());
                    }
                    enableComponentForAllUser(needEnablePackage, true);
                    this.mDisableAppMap.clear();
                }
            }
            BlackListAppsUtils.deleteDisableAppListFile();
            return;
        }
        synchronized (this.mIPmsInner.getPackagesLock()) {
            if (!this.mIPmsInner.isUpgrade() && !BlackListAppsUtils.isBlackListUpdate(this.mBlackListInfo, this.mDisableAppListInfo)) {
                if (validateDisabledAppFile()) {
                    isCompleteProcess = false;
                }
            }
            isCompleteProcess = true;
        }
        Slog.i(TAG, "initBlackList start, is completed process: " + isCompleteProcess);
        if (isCompleteProcess) {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                Set<String> needDisablePackage = new ArraySet<>();
                Set<String> needEnablePackage2 = new ArraySet<>();
                Iterator<BlackListInfo.BlackListApp> it2 = this.mBlackListInfo.mBlackList.iterator();
                while (it2.hasNext()) {
                    BlackListInfo.BlackListApp app2 = it2.next();
                    String pkg = app2.mPackageName;
                    if (!needDisablePackage.contains(pkg)) {
                        if (BlackListAppsUtils.comparePackage((PackageParser.Package) this.mIPmsInner.getPackagesLock().get(pkg), app2) && !needDisablePackage.contains(pkg)) {
                            setPackageDisableFlag(pkg, true);
                            needDisablePackage.add(pkg);
                            this.mDisableAppMap.put(pkg, app2);
                        }
                    }
                }
                for (String pkg2 : new ArrayList<>(this.mDisableAppMap.keySet())) {
                    if (!BlackListAppsUtils.containsApp(this.mBlackListInfo.mBlackList, this.mDisableAppMap.get(pkg2)) && !needDisablePackage.contains(pkg2)) {
                        if (this.mIPmsInner.getPackagesLock().get(pkg2) != null) {
                            needEnablePackage2.add(pkg2);
                        }
                        this.mDisableAppMap.remove(pkg2);
                    }
                }
                enableComponentForAllUser(needEnablePackage2, true);
                enableComponentForAllUser(needDisablePackage, false);
                this.mDisableAppListInfo.mBlackList.clear();
                for (Map.Entry<String, BlackListInfo.BlackListApp> entry2 : this.mDisableAppMap.entrySet()) {
                    this.mDisableAppListInfo.mBlackList.add(entry2.getValue());
                }
                this.mDisableAppListInfo.mVersionCode = this.mBlackListInfo.mVersionCode;
                BlackListAppsUtils.writeBlackListToXml(this.mDisableAppListInfo);
            }
        } else {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                Set<String> needDisablePackage2 = new ArraySet<>();
                for (Map.Entry<String, BlackListInfo.BlackListApp> entry3 : this.mDisableAppMap.entrySet()) {
                    setPackageDisableFlag(entry3.getKey(), true);
                    needDisablePackage2.add(entry3.getKey());
                }
                enableComponentForAllUser(needDisablePackage2, false);
            }
        }
        Slog.i(TAG, "initBlackList end");
    }

    private boolean validateDisabledAppFile() {
        if (this.mBlackListInfo.mBlackList.size() == 0) {
            return false;
        }
        synchronized (this.mIPmsInner.getPackagesLock()) {
            for (Map.Entry<String, BlackListInfo.BlackListApp> entry : this.mDisableAppMap.entrySet()) {
                if (this.mIPmsInner.getPackagesLock().get(entry.getKey()) == null) {
                    return false;
                }
                if (!BlackListAppsUtils.containsApp(this.mBlackListInfo.mBlackList, entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    private void enableComponentForAllUser(Set<String> packages, boolean enable) {
        int[] userIds = UserManagerService.getInstance().getUserIds();
        for (int userId : userIds) {
            if (packages != null && packages.size() > 0) {
                for (String pkg : packages) {
                    enableComponentForPackage(pkg, enable, userId);
                }
            }
        }
    }

    private void setPackageDisableFlag(String packageName, boolean disable) {
        PackageParser.Package pkg;
        if (TextUtils.isEmpty(packageName) || (pkg = (PackageParser.Package) this.mIPmsInner.getPackagesLock().get(packageName)) == null) {
            return;
        }
        if (disable) {
            pkg.applicationInfo.hwFlags |= 268435456;
            return;
        }
        pkg.applicationInfo.hwFlags |= -268435457;
    }

    private void enableComponentForPackage(String packageName, boolean enable, int userId) {
        if (!TextUtils.isEmpty(packageName)) {
            int newState = enable ? 0 : 2;
            try {
                PackageInfo packageInfo = AppGlobals.getPackageManager().getPackageInfo(packageName, 786959, userId);
                if (!(packageInfo == null || packageInfo.receivers == null || packageInfo.receivers.length == 0)) {
                    for (int i = 0; i < packageInfo.receivers.length; i++) {
                        setEnabledComponentInner(new ComponentName(packageName, packageInfo.receivers[i].name), newState, userId);
                    }
                }
                if (!(packageInfo == null || packageInfo.services == null || packageInfo.services.length == 0)) {
                    for (int i2 = 0; i2 < packageInfo.services.length; i2++) {
                        setEnabledComponentInner(new ComponentName(packageName, packageInfo.services[i2].name), newState, userId);
                    }
                }
                if (!(packageInfo == null || packageInfo.providers == null || packageInfo.providers.length == 0)) {
                    for (int i3 = 0; i3 < packageInfo.providers.length; i3++) {
                        setEnabledComponentInner(new ComponentName(packageName, packageInfo.providers[i3].name), newState, userId);
                    }
                }
                if (packageInfo != null && packageInfo.activities != null && packageInfo.activities.length != 0) {
                    for (int i4 = 0; i4 < packageInfo.activities.length; i4++) {
                        setEnabledComponentInner(new ComponentName(packageName, packageInfo.activities[i4].name), newState, userId);
                    }
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "BlackList: Get packageInfo fail, name=" + packageName);
            }
        }
    }

    public void updatePackageBlackListInfo(String packageName) {
        try {
            if (!this.isBlackListExist) {
                return;
            }
            if (!TextUtils.isEmpty(packageName)) {
                int[] userIds = UserManagerService.getInstance().getUserIds();
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    PackageParser.Package pkgInfo = (PackageParser.Package) this.mIPmsInner.getPackagesLock().get(packageName);
                    boolean needDisable = false;
                    boolean needEnable = false;
                    if (pkgInfo != null) {
                        Iterator<BlackListInfo.BlackListApp> it = this.mBlackListInfo.mBlackList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            BlackListInfo.BlackListApp app = it.next();
                            if (BlackListAppsUtils.comparePackage(pkgInfo, app)) {
                                setPackageDisableFlag(packageName, true);
                                this.mDisableAppMap.put(packageName, app);
                                needDisable = true;
                                break;
                            }
                        }
                        if (!needDisable && this.mDisableAppMap.containsKey(packageName)) {
                            setPackageDisableFlag(packageName, false);
                            this.mDisableAppMap.remove(packageName);
                            needEnable = true;
                        }
                    } else if (this.mDisableAppMap.containsKey(packageName)) {
                        this.mDisableAppMap.remove(packageName);
                    }
                    for (int userId : userIds) {
                        if (needDisable) {
                            enableComponentForPackage(packageName, false, userId);
                        } else if (needEnable) {
                            enableComponentForPackage(packageName, true, userId);
                        }
                    }
                    this.mDisableAppListInfo.mBlackList.clear();
                    for (Map.Entry<String, BlackListInfo.BlackListApp> entry : this.mDisableAppMap.entrySet()) {
                        this.mDisableAppListInfo.mBlackList.add(entry.getValue());
                    }
                    this.mDisableAppListInfo.mVersionCode = this.mBlackListInfo.mVersionCode;
                    BlackListAppsUtils.writeBlackListToXml(this.mDisableAppListInfo);
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "update BlackList info failed");
        }
    }

    private void setEnabledComponentInner(ComponentName componentName, int newState, int userId) {
        if (componentName != null) {
            String packageName = componentName.getPackageName();
            String className = componentName.getClassName();
            if (packageName != null && className != null) {
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                    if (pkgSetting == null) {
                        Slog.e(TAG, "setEnabledSetting, can not find pkgSetting, packageName = " + packageName);
                        return;
                    }
                    PackageParser.Package pkg = pkgSetting.pkg;
                    if (pkg != null) {
                        if (pkg.hasComponentClassName(className)) {
                            if (newState != 0) {
                                if (newState != 2) {
                                    Slog.e(TAG, "Invalid new component state: " + newState);
                                    return;
                                } else if (!pkgSetting.disableComponentLPw(className, userId)) {
                                    return;
                                }
                            } else if (!pkgSetting.restoreComponentLPw(className, userId)) {
                                return;
                            }
                            return;
                        }
                    }
                    Slog.w(TAG, "Failed setComponentEnabledSetting: component class " + className + " does not exist in " + packageName);
                }
            }
        }
    }

    private void initBlackListForNewUser(int userHandle) {
        if (this.isBlackListExist) {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                for (String pkg : this.mDisableAppMap.keySet()) {
                    enableComponentForPackage(pkg, false, userHandle);
                }
            }
        }
    }

    public void onNewUserCreated(int userId) {
        initBlackListForNewUser(userId);
    }

    public void replaceSignatureIfNeeded(PackageSetting ps, PackageParser.Package pkg, boolean isBootScan, boolean isUpdate) {
        if (pkg != null && this.mCompatSettings != null) {
            if (!isBootScan) {
                synchronized (this.mIncompatiblePkg) {
                    if (this.mIncompatiblePkg.contains(pkg.packageName)) {
                        this.mIncompatiblePkg.remove(pkg.packageName);
                    }
                }
            }
            boolean needReplace = false;
            String packageSignType = null;
            boolean isSignedByOldSystemSignature = this.mCompatSettings.isOldSystemSignature(pkg.mSigningDetails.signatures);
            if (isBootScan && ps != null) {
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    CertCompatSettings.Package compatPkg = this.mCompatSettings.getCompatPackage(pkg.packageName);
                    if (compatPkg != null && compatPkg.codePath.equals(ps.codePathString) && compatPkg.timeStamp == ps.timeStamp) {
                        needReplace = true;
                        packageSignType = compatPkg.certType;
                        if (this.mCompatSettings.isIncompatPackage(pkg)) {
                            needReplace = false;
                            Slog.i(TAG, "CertCompat: remove incompat package:" + pkg.packageName + ",type:" + packageSignType);
                        } else if (!isSignedByOldSystemSignature && !isContainHwCertification(pkg)) {
                            needReplace = false;
                            Slog.i(TAG, "CertCompat: remove normal package:" + pkg.packageName + ",type:" + packageSignType);
                        } else if (this.mCompatSettings.isUpgrade()) {
                            Slog.i(TAG, "CertCompat: system signature compat for OTA package:" + pkg.packageName + ",type:" + packageSignType);
                        }
                    }
                }
            }
            if (!needReplace && HwCertificationManager.isSupportHwCertification(pkg)) {
                int resultCode = getHwCertificateType(pkg);
                if (resultCode == 1) {
                    packageSignType = "platform";
                } else if (resultCode == 2) {
                    packageSignType = "testkey";
                } else if (resultCode == 3) {
                    packageSignType = "shared";
                } else if (resultCode == 4) {
                    packageSignType = "media";
                }
                if (packageSignType != null) {
                    int certVersion = getHwCertSignatureVersion(pkg);
                    if (certVersion == 2 || this.mCompatSettings.isCompatAllLegacyPackages() || this.mCompatSettings.isWhiteListedApp(pkg, isBootScan) || (isBootScan && this.mCompatSettings.isUpgrade() && this.mIPmsInner.isUpgrade())) {
                        needReplace = true;
                        Slog.i(TAG, "CertCompat: system signature compat for hwcert package:" + pkg.packageName + ",type:" + packageSignType + ",certVersion:" + certVersion);
                    } else {
                        Slog.i(TAG, "CertCompat: illegal system signature compat for hwcert package:" + pkg.packageName + ",type:" + packageSignType + ",certVersion:" + certVersion);
                    }
                }
            }
            if (!needReplace && isSignedByOldSystemSignature && !this.mCompatSettings.isIncompatPackage(pkg) && this.mCompatSettings.isWhiteListedApp(pkg, isBootScan)) {
                packageSignType = this.mCompatSettings.getOldSignTpye(pkg.mSigningDetails.signatures);
                needReplace = true;
                Slog.i(TAG, "CertCompat: system signature compat for whitelist package:" + pkg.packageName + ",type:" + packageSignType);
                if (!isBootScan) {
                    Context context = this.mContext;
                    Flog.bdReport(context, 125, "{package:" + pkg.packageName + ",version:" + pkg.mVersionCode + ",type:" + packageSignType + "}");
                }
            }
            if (!needReplace && isSignedByOldSystemSignature && isBootScan && ((!this.mFoundCertCompatFile || this.mCompatSettings.isUpgrade()) && !this.mCompatSettings.isIncompatPackage(pkg) && this.mIPmsInner.isUpgrade())) {
                packageSignType = this.mCompatSettings.getOldSignTpye(pkg.mSigningDetails.signatures);
                needReplace = true;
                Slog.i(TAG, "CertCompat: system signature compat for OTA package:" + pkg.packageName);
            }
            if (needReplace) {
                replaceSignatureInner(ps, pkg, packageSignType);
            } else if (this.mCompatSettings.isLegacySignature(pkg.mSigningDetails.signatures) && !isBootScan) {
                synchronized (this.mIncompatiblePkg) {
                    if (!this.mIncompatiblePkg.contains(pkg.packageName) && !this.mCompatSettings.isIncompatPackage(pkg)) {
                        this.mIncompatiblePkg.add(pkg.packageName);
                    }
                }
                String packageSignType2 = this.mCompatSettings.getOldSignTpye(pkg.mSigningDetails.signatures);
                if (packageSignType2 != null) {
                    Slog.i(TAG, "CertCompat: illegal system signature package:" + pkg.packageName + ",type:" + packageSignType2);
                    Context context2 = this.mContext;
                    Flog.bdReport(context2, (int) CPUFeature.MSG_SET_CPUCONFIG, "{package:" + pkg.packageName + ",version:" + pkg.mVersionCode + ",type:" + packageSignType2 + "}");
                    return;
                }
                Slog.i(TAG, "CertCompat: Legacy system signature package:" + pkg.packageName);
            }
        }
    }

    private void replaceSignatureInner(PackageSetting ps, PackageParser.Package pkg, String signType) {
        CertCompatSettings certCompatSettings;
        if (signType != null && pkg != null && (certCompatSettings = this.mCompatSettings) != null) {
            Signature[] signs = certCompatSettings.getNewSign(signType);
            if (signs.length == 0) {
                Slog.e(TAG, "CertCompat: signs init fail");
                return;
            }
            PackageParser.SigningDetails newSignDetails = createNewSigningDetails(pkg.mSigningDetails, signs);
            this.mIPmsInner.setRealSigningDetails(pkg, pkg.mSigningDetails);
            pkg.mSigningDetails = newSignDetails;
            if (ps != null && ps.signatures.mSigningDetails.hasSignatures()) {
                ps.signatures.mSigningDetails = pkg.mSigningDetails;
            }
            Slog.d(TAG, "CertCompat: CertCompatPackage:" + pkg.packageName);
        }
    }

    private PackageParser.SigningDetails createNewSigningDetails(PackageParser.SigningDetails orig, Signature[] newSigns) {
        return new PackageParser.SigningDetails(newSigns, orig.signatureSchemeVersion, orig.publicKeys, orig.pastSigningCertificates);
    }

    public void initCertCompatSettings() {
        Slog.i(TAG, "CertCompat: init CertCompatSettings");
        this.mCompatSettings = new CertCompatSettings();
        this.mFoundCertCompatFile = this.mCompatSettings.readCertCompatPackages();
    }

    public void resetSharedUserSignaturesIfNeeded() {
    }

    @SuppressLint({"PreferForInArrayList"})
    public void writeCertCompatPackages(boolean update) {
        CertCompatSettings certCompatSettings = this.mCompatSettings;
        if (certCompatSettings != null) {
            if (update) {
                Iterator<CertCompatSettings.Package> it = new ArrayList<>(certCompatSettings.getALLCompatPackages()).iterator();
                while (it.hasNext()) {
                    CertCompatSettings.Package pkg = it.next();
                    if (pkg != null && !this.mIPmsInner.getPackagesLock().containsKey(pkg.packageName)) {
                        this.mCompatSettings.removeCertCompatPackage(pkg.packageName);
                    }
                }
            }
            this.mCompatSettings.writeCertCompatPackages();
        }
    }

    public void updateCertCompatPackage(PackageParser.Package pkg, PackageSetting ps) {
        if (pkg != null && this.mCompatSettings != null) {
            Signature[] realSign = this.mIPmsInner.getRealSignature(pkg);
            if (realSign == null || realSign.length == 0 || ps == null) {
                this.mCompatSettings.removeCertCompatPackage(pkg.applicationInfo.packageName);
            } else {
                this.mCompatSettings.insertCompatPackage(pkg.applicationInfo.packageName, ps);
            }
        }
    }

    public boolean isSystemSignatureUpdated(Signature[] previous, Signature[] current) {
        CertCompatSettings certCompatSettings = this.mCompatSettings;
        if (certCompatSettings == null) {
            return false;
        }
        return certCompatSettings.isSystemSignatureUpdated(previous, current);
    }

    public void sendIncompatibleNotificationIfNeeded(final String packageName) {
        synchronized (this.mIncompatiblePkg) {
            boolean update = false;
            final boolean send = false;
            if (this.mIncompatiblePkg.contains(packageName)) {
                this.mIncompatiblePkg.remove(packageName);
                update = true;
                send = true;
            } else if (this.mIncompatNotificationList.contains(packageName)) {
                update = true;
            }
            if (update) {
                UiThread.getHandler().post(new Runnable() {
                    /* class com.android.server.pm.HwPackageManagerServiceEx.AnonymousClass6 */

                    public void run() {
                        HwPackageManagerServiceEx.this.updateIncompatibleNotification(packageName, send);
                    }
                });
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateIncompatibleNotification(String packageName, boolean isSend) {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            if (am != null) {
                int[] resolvedUserIds = am.getRunningUserIds();
                int i = 0;
                if (isSend) {
                    synchronized (this.mIncompatiblePkg) {
                        this.mIncompatNotificationList.add(packageName);
                    }
                    int length = resolvedUserIds.length;
                    while (i < length) {
                        sendIncompatibleNotificationInner(packageName, resolvedUserIds[i]);
                        i++;
                    }
                    return;
                }
                boolean cancelAll = false;
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    boolean isLegacySignature = false;
                    PackageParser.Package pkgInfo = (PackageParser.Package) this.mIPmsInner.getPackagesLock().get(packageName);
                    if (!(pkgInfo == null || this.mCompatSettings == null)) {
                        isLegacySignature = this.mCompatSettings.isLegacySignature(pkgInfo.mSigningDetails.signatures);
                    }
                    if (pkgInfo == null || !isLegacySignature) {
                        Slog.d(TAG, "CertCompat: Package removed or update to new system signature version, cancel all incompatible notification.");
                        cancelAll = true;
                    }
                }
                if (cancelAll) {
                    synchronized (this.mIncompatiblePkg) {
                        this.mIncompatNotificationList.remove(packageName);
                    }
                }
                int length2 = resolvedUserIds.length;
                while (i < length2) {
                    int id = resolvedUserIds[i];
                    if (cancelAll || !AppGlobals.getPackageManager().isPackageAvailable(packageName, id)) {
                        cancelIncompatibleNotificationInner(packageName, id);
                    }
                    i++;
                }
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "CertCompat: RemoteException throw when update Incompatible Notification.");
        }
    }

    private void cancelIncompatibleNotificationInner(String packageName, int userId) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService("notification");
        if (nm != null) {
            nm.cancelAsUser(packageName, 33685898, new UserHandle(userId));
            Slog.d(TAG, "CertCompat: cancel incompatible notification for u" + userId + ", packageName:" + packageName);
        }
    }

    private void sendIncompatibleNotificationInner(String packageName, int userId) {
        Slog.d(TAG, "CertCompat: send incompatible notification to u" + userId + ", packageName:" + packageName);
        PackageManager pm = this.mContext.getPackageManager();
        if (pm != null) {
            try {
                ApplicationInfo info = pm.getApplicationInfoAsUser(packageName, 0, userId);
                Drawable icon = pm.getApplicationIcon(info);
                CharSequence title = pm.getApplicationLabel(info);
                String text = this.mContext.getString(33685898);
                PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse("package:" + packageName)), 0, null, new UserHandle(userId));
                if (pi == null) {
                    Slog.w(TAG, "CertCompat: Get PendingIntent fail, package: " + packageName);
                    return;
                }
                Notification notification = new Notification.Builder(this.mContext, SystemNotificationChannels.ALERTS).setLargeIcon(UserIcons.convertToBitmap(icon)).setSmallIcon(17301642).setContentTitle(title).setContentText(text).setContentIntent(pi).setDefaults(2).setPriority(2).setWhen(System.currentTimeMillis()).setShowWhen(true).setAutoCancel(true).addAction(new Notification.Action.Builder((Icon) null, this.mContext.getString(33685899), pi).build()).build();
                NotificationManager nm = (NotificationManager) this.mContext.getSystemService("notification");
                if (nm != null) {
                    nm.notifyAsUser(packageName, 33685898, notification, new UserHandle(userId));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.w(TAG, "CertCompat: incompatible package: " + packageName + " not find for u" + userId);
            }
        }
    }

    public void recordInstallAppInfo(String pkgName, long beginTime, int installFlags) {
        int srcPkg;
        long endTime = SystemClock.elapsedRealtime();
        if ((installFlags & 32) != 0) {
            srcPkg = 1;
        } else {
            srcPkg = 0;
        }
        insertAppInfo(pkgName, srcPkg, beginTime, endTime);
    }

    private void insertAppInfo(String pkgName, int srcPkg, long beginTime, long endTime) {
        if (TextUtils.isEmpty(pkgName)) {
            Slog.e(TAG, "insertAppInfo pkgName is null");
            return;
        }
        HwSecurityDiagnoseManager sdm = HwSecurityDiagnoseManager.getInstance();
        if (sdm == null) {
            Slog.e(TAG, "insertAppInfo error, sdm is null");
            return;
        }
        try {
            Bundle bundle = new Bundle();
            bundle.putString("pkg", pkgName);
            bundle.putInt("src", srcPkg);
            bundle.putLong("begintime", beginTime);
            bundle.putLong("endtime", endTime);
            if (!sdm.setMalData(HwSecurityDiagnoseManager.AntiMalDataSrcType.PMS.ordinal(), bundle)) {
                Slog.w(TAG, "insertAppInfo, failed to pass the filling information");
            }
        } catch (Exception e) {
            Slog.e(TAG, "insertAppInfo EXCEPTION");
        }
    }

    private void setCurrentEmuiSysImgVersion() {
        PackageExHandler packageExHandler = this.mHandler;
        if (packageExHandler != null) {
            packageExHandler.sendEmptyMessage(1);
        }
    }

    private void deleteSoundrecorder() {
        PackageExHandler packageExHandler = this.mHandler;
        if (packageExHandler != null) {
            packageExHandler.sendEmptyMessage(2);
        }
    }

    /* access modifiers changed from: private */
    public void deleteSoundercorderIfNeed() {
        Log.i(TAG, "begin uninstall soundrecorder");
        if ("0".equals(SystemProperties.get("persist.sys.uninstallapk", "0"))) {
            try {
                this.mIPmsInner.deletePackageInner(HW_SOUND_RECORDER, -1, 0, 2);
                Log.i(TAG, "uninstall soundrecorder ...");
            } catch (Exception e) {
                Log.e(TAG, "uninstall soundrecorder error");
            }
            SystemProperties.set("persist.sys.uninstallapk", "1");
        }
    }

    /* access modifiers changed from: private */
    public static int deriveEmuiSysImgVersion() {
        try {
            String str = ManufactureNativeUtils.getVersionInfo(3);
            Slog.d(TAG, "deriveEmuiSysImgVersion, version info is " + str);
            if (TextUtils.isEmpty(str)) {
                return 0;
            }
            String ret = "";
            Matcher matcher = Pattern.compile("(\\d+\\.){3}\\d+").matcher(str);
            if (matcher.find()) {
                ret = matcher.group().trim();
            }
            Slog.d(TAG, "deriveEmuiSysImgVersion,find:" + ret);
            if (TextUtils.isEmpty(ret)) {
                return 0;
            }
            int version = Integer.parseInt(ret.replace(".", ""));
            Slog.d(TAG, "deriveEmuiSysImgVersion,version:" + version);
            return version;
        } catch (Exception e) {
            Slog.w(TAG, "deriveEmuiSysImgVersion error");
            return 0;
        }
    }

    public boolean setApplicationAspectRatio(String packageName, String aspectName, float ar) {
        if (UserHandle.getAppId(Binder.getCallingUid()) != 1000) {
            this.mContext.enforceCallingPermission(SET_ASPECT_RATIO_PERMISSION, "Permission Denide for setApplicationAspectRatio!");
        }
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                if (pkgSetting == null) {
                    return false;
                }
                if (pkgSetting.getAspectRatio(aspectName) != ar) {
                    pkgSetting.setAspectRatio(aspectName, ar);
                    this.mIPmsInner.getSettings().writeLPr();
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                }
                Binder.restoreCallingIdentity(callingId);
                return false;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public float getApplicationAspectRatio(String packageName, String aspectName) {
        long callingId = Binder.clearCallingIdentity();
        try {
            PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
            if (pkgSetting == null) {
                Binder.restoreCallingIdentity(callingId);
                return 0.0f;
            }
            float aspectRatio = pkgSetting.getAspectRatio(aspectName);
            Binder.restoreCallingIdentity(callingId);
            return aspectRatio;
        } catch (ArrayIndexOutOfBoundsException e) {
            Slog.w(TAG, "get " + aspectName + " index out of bounds! packageName :" + packageName);
        } catch (Exception e2) {
            Slog.w(TAG, "get " + aspectName + " other exception! packageName :" + packageName);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
            throw th;
        }
        Binder.restoreCallingIdentity(callingId);
        return 0.0f;
    }

    private boolean isSystemSecure() {
        return "1".equals(SystemProperties.get(SYSTEM_SECURE, "1")) && "0".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
    }

    public void loadSysWhitelist() {
        long startTime = HwPackageManagerServiceUtils.hwTimingsBegin();
        AntiMalPreInstallScanner.init(this.mContext, this.mIPmsInner.isUpgrade());
        AntiMalPreInstallScanner.getInstance().loadSysWhitelist();
        HwPackageManagerServiceUtils.hwTimingsEnd(TAG, "loadSysWhitelist", startTime);
    }

    public void checkIllegalSysApk(PackageParser.Package pkg, int hwFlags) throws PackageManagerException {
        int result = AntiMalPreInstallScanner.getInstance().checkIllegalSysApk(pkg, hwFlags);
        if (result != 1) {
            if (result != 2) {
                Slog.i(TAG, "Other types of aplication " + pkg);
                return;
            }
            int hwFlags2 = hwFlags | 33554432;
            addFlagsForRemovablePreApk(pkg, hwFlags2);
            if (!needInstallRemovablePreApk(pkg, hwFlags2)) {
                throw new PackageManagerException(-115, "checkIllegalSysApk apk changed illegally!");
            }
        } else if (isSystemSecure() || ANTIMAL_DEBUG_ON) {
            throw new PackageManagerException(-115, "checkIllegalSysApk add illegally!");
        }
    }

    public void addPreinstalledPkgToList(PackageParser.Package scannedPkg) {
        if (scannedPkg != null && scannedPkg.baseCodePath != null && !scannedPkg.baseCodePath.startsWith("/data/app/") && !scannedPkg.baseCodePath.startsWith("/data/app-private/")) {
            preinstalledPackageList.add(scannedPkg.packageName);
        }
    }

    public List<String> getPreinstalledApkList() {
        List<String> preinstalledApkList = new ArrayList<>();
        File preinstalledApkFile = new File("/data/system/", PREINSTALLED_APK_LIST_FILE);
        FileInputStream stream = null;
        try {
            FileInputStream stream2 = new FileInputStream(preinstalledApkFile);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream2, null);
            while (true) {
                int type = parser.next();
                if (type == 1 || type == 2) {
                    String tag = parser.getName();
                }
            }
            String tag2 = parser.getName();
            if ("values".equals(tag2)) {
                parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        try {
                            stream2.close();
                            break;
                        } catch (IOException e) {
                        }
                    } else if (type2 != 3) {
                        if (type2 != 4) {
                            if ("string".equals(parser.getName()) && parser.getAttributeValue(1) != null) {
                                preinstalledApkList.add(parser.getAttributeValue(1));
                            }
                        }
                    }
                }
                return preinstalledApkList;
            }
            throw new XmlPullParserException("Settings do not start with policies tag: found " + tag2);
        } catch (FileNotFoundException e2) {
            Slog.w(TAG, "file is not exist " + e2.getMessage());
            if (0 != 0) {
                stream.close();
            }
        } catch (XmlPullParserException e3) {
            Slog.w(TAG, "failed parsing " + preinstalledApkFile + " " + e3.getMessage());
            if (0 != 0) {
                stream.close();
            }
        } catch (IOException e4) {
            Slog.w(TAG, "failed parsing " + preinstalledApkFile + " " + e4.getMessage());
            if (0 != 0) {
                stream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    stream.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x00cd  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00f3  */
    /* JADX WARNING: Removed duplicated region for block: B:41:? A[ORIG_RETURN, RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:42:? A[RETURN, SYNTHETIC] */
    public void writePreinstalledApkListToFile() {
        File preinstalledApkFile = new File("/data/system/", PREINSTALLED_APK_LIST_FILE);
        if (!preinstalledApkFile.exists()) {
            FileOutputStream stream = null;
            try {
                boolean isCreateSuccess = preinstalledApkFile.createNewFile();
                if (isCreateSuccess) {
                    FileUtils.setPermissions(preinstalledApkFile.getPath(), 416, -1, -1);
                }
                stream = new FileOutputStream(preinstalledApkFile, false);
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, "utf-8");
                out.startDocument(null, true);
                out.startTag(null, "values");
                int N = preinstalledPackageList.size();
                try {
                    PackageManager pm = this.mContext.getPackageManager();
                    int i = 0;
                    while (i < N) {
                        String packageName = preinstalledPackageList.get(i);
                        String apkName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 67108864)).toString();
                        out.startTag(null, "string");
                        out.attribute(null, "name", packageName);
                        out.attribute(null, HwSecDiagnoseConstant.MALAPP_APK_NAME, apkName);
                        out.endTag(null, "string");
                        i++;
                        isCreateSuccess = isCreateSuccess;
                    }
                    out.endTag(null, "values");
                    out.endDocument();
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                } catch (PackageManager.NameNotFoundException e2) {
                    e = e2;
                    Slog.w(TAG, "failed parsing " + preinstalledApkFile + " " + e.getMessage());
                    if (stream == null) {
                    }
                } catch (IOException e3) {
                    e = e3;
                    try {
                        Slog.w(TAG, "failed parsing " + preinstalledApkFile + " " + e.getMessage());
                        if (stream == null) {
                        }
                    } catch (Throwable th) {
                        th = th;
                    }
                }
            } catch (PackageManager.NameNotFoundException e4) {
                e = e4;
                Slog.w(TAG, "failed parsing " + preinstalledApkFile + " " + e.getMessage());
                if (stream == null) {
                    stream.close();
                }
            } catch (IOException e5) {
                e = e5;
                Slog.w(TAG, "failed parsing " + preinstalledApkFile + " " + e.getMessage());
                if (stream == null) {
                    stream.close();
                }
            } catch (Throwable th2) {
                th = th2;
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e6) {
                    }
                }
                throw th;
            }
        }
    }

    public void createPublicityFile() {
        AntiMalPreInstallScanner.init(this.mContext, this.mIPmsInner.isUpgrade());
        if ("CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""))) {
            ParceledListSlice<ApplicationInfo> slice = this.mIPmsInner.getInstalledApplications(0, 0);
            if (this.mIPmsInner.isUpgrade()) {
                PackagePublicityUtils.deletePublicityFile();
            }
            PackagePublicityUtils.writeAllPakcagePublicityInfoIntoFile(this.mContext, slice);
        }
    }

    public List<String> getHwPublicityAppList() {
        return PackagePublicityUtils.getHwPublicityAppList(this.mContext);
    }

    public ParcelFileDescriptor getHwPublicityAppParcelFileDescriptor() {
        return PackagePublicityUtils.getHwPublicityAppParcelFileDescriptor();
    }

    protected static void initCustStoppedApps() {
        File file;
        if (!firstScanForHwPMS()) {
            Slog.i(TAG, "not first boot. don't init cust stopped apps.");
            return;
        }
        Slog.i(TAG, "first boot. init cust stopped apps.");
        File file2 = null;
        try {
            file2 = HwCfgFilePolicy.getCfgFile("xml/not_start_firstboot.xml", 0);
            if (file2 == null) {
                file = new File(HwDelAppManager.CUST_PRE_DEL_DIR, "xml/not_start_firstboot.xml");
                file2 = file;
            }
        } catch (NoClassDefFoundError e) {
            Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            if (0 == 0) {
                file = new File(HwDelAppManager.CUST_PRE_DEL_DIR, "xml/not_start_firstboot.xml");
            }
        } catch (Throwable th) {
            if (0 == 0) {
                new File(HwDelAppManager.CUST_PRE_DEL_DIR, "xml/not_start_firstboot.xml");
            }
            throw th;
        }
        parseCustStoppedApps(file2);
    }

    private static void parseCustStoppedApps(File file) {
        try {
            FileReader xmlReader = new FileReader(file);
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(xmlReader);
                XmlUtils.beginDocument(parser, "resources");
                while (true) {
                    XmlUtils.nextElement(parser);
                    if (parser.getName() == null) {
                        try {
                            xmlReader.close();
                            return;
                        } catch (IOException e) {
                            Slog.w(TAG, "Got execption when close black_package_name.xml!", e);
                            return;
                        }
                    } else if ("package".equals(parser.getName())) {
                        String value = parser.getAttributeValue(null, "name");
                        if (!TextUtils.isEmpty(value)) {
                            mCustStoppedApps.add(value.intern());
                            Slog.i(TAG, "cust stopped apps:" + value);
                        } else {
                            mCustStoppedApps.clear();
                            Slog.e(TAG, "not_start_firstboot.xml bad format.");
                        }
                    }
                }
            } catch (XmlPullParserException e2) {
                Slog.w(TAG, "Got execption parsing black_package_name.xml!", e2);
                xmlReader.close();
            } catch (IOException e3) {
                Slog.w(TAG, "Got execption parsing black_package_name.xml!", e3);
                xmlReader.close();
            } catch (Throwable th) {
                try {
                    xmlReader.close();
                } catch (IOException e4) {
                    Slog.w(TAG, "Got execption when close black_package_name.xml!", e4);
                }
                throw th;
            }
        } catch (FileNotFoundException e5) {
            Slog.w(TAG, "There is no file named not_start_firstboot.xml!", e5);
        }
    }

    public static boolean isCustedCouldStopped(String pkg, boolean block, boolean stopped) {
        boolean contain = mCustStoppedApps.contains(pkg);
        if (contain) {
            if (block) {
                Slog.i(TAG, "blocked broadcast send to system app:" + pkg + ", stopped?" + stopped);
            } else {
                Slog.i(TAG, "a system app is customized not to start at first boot. app:" + pkg);
            }
        }
        return contain;
    }

    public void scanRemovableAppDir(int scanMode) {
        long startTime = HwPackageManagerServiceUtils.hwTimingsBegin();
        HwDelAppManager.getInstance(this).scanRemovableAppDir(scanMode);
        HwPackageManagerServiceUtils.hwTimingsEnd(TAG, "scanRemovableAppDir", startTime);
    }

    public boolean needInstallRemovablePreApk(PackageParser.Package pkg, int hwFlags) {
        return HwUninstalledAppManager.getInstance(this, this).needInstallRemovablePreApk(pkg, hwFlags);
    }

    public boolean isDelapp(PackageSetting ps) {
        return HwDelAppManager.getInstance(this).isDelapp(ps);
    }

    public boolean isReservePersistentApp(PackageSetting ps) {
        if (ps.codePath == null) {
            return false;
        }
        String codePath = ps.codePath.toString();
        for (String path : new String[]{SYSTEM_PRIV_APP_DIR, SYSTEM_APP_DIR, HW_PRODUCT_DIR}) {
            if (codePath.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    public void addFlagsForRemovablePreApk(PackageParser.Package pkg, int hwFlags) {
        HwPackageManagerServiceUtils.addFlagsForRemovablePreApk(pkg, hwFlags);
    }

    public boolean isDisallowUninstallApk(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        if (HwDeviceManager.disallowOp(5, packageName)) {
            Flog.i((int) HwMpLinkServiceImpl.MPLINK_MSG_WIFIPRO_SWITCH_ENABLE, "Not removing package " + packageName + ": is disallowed!");
            return true;
        }
        try {
            String disallowUninstallPkgList = Settings.Secure.getString(this.mContext.getContentResolver(), "enterprise_disallow_uninstall_apklist");
            Slog.w(TAG, "isEnterpriseDisallowUninstallApk disallowUninstallPkgList : " + disallowUninstallPkgList);
            if (!TextUtils.isEmpty(disallowUninstallPkgList)) {
                for (String pkg : disallowUninstallPkgList.split(";")) {
                    if (packageName.equals(pkg)) {
                        Slog.i(TAG, packageName + " is in the enterprise Disallow UninstallApk blacklist!");
                        Flog.i((int) HwMpLinkServiceImpl.MPLINK_MSG_WIFIPRO_SWITCH_ENABLE, "Not removing package " + packageName + ": is disallowed!");
                        return true;
                    }
                }
            }
            return false;
        } catch (IllegalStateException e) {
            Slog.e(TAG, "get disallow uninstall pkg list IllegalStateException : " + e.getMessage());
            return false;
        } catch (Exception e2) {
            Slog.e(TAG, "isDisallowUninstallApk, get disallow uninstall list failed");
            return false;
        }
    }

    public boolean isInMultiWinWhiteList(String packageName) {
        return !IS_HW_MULTIWINDOW_SUPPORTED && MultiWinWhiteListManager.getInstance().isInMultiWinWhiteList(packageName);
    }

    public boolean isInMWPortraitWhiteList(String packageName) {
        return MultiWinWhiteListManager.getInstance().isInMWPortraitWhiteList(packageName);
    }

    public String getResourcePackageNameByIcon(String pkgName, int icon, int userId) {
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            Log.w(TAG, "packageManager is null !");
            return null;
        }
        try {
            return pm.getResourcesForApplicationAsUser(pkgName, userId).getResourcePackageName(icon);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "packageName " + pkgName + ": Resources not found !");
            return null;
        } catch (Resources.NotFoundException e2) {
            Log.w(TAG, "packageName " + pkgName + ": ResourcesPackageName not found !");
            return null;
        } catch (RuntimeException e3) {
            Log.w(TAG, "RuntimeException in getResourcePackageNameByIcon !");
            return null;
        }
    }

    public List<String> getOldDataBackup() {
        return HwUninstalledAppManager.getInstance(this, this).getOldDataBackup();
    }

    private boolean assertScanInstallApkLocked(String packageName, String apkFile, int userId) {
        if (this.mScanInstallApkList.contains(apkFile)) {
            Slog.w(TAG, "Scan install , the apk file " + apkFile + " is already in scanning.  Skipping duplicate.");
            return false;
        }
        Map<String, String> mUninstalledMap = getUninstalledMap();
        if (mUninstalledMap == null || !mUninstalledMap.containsValue(apkFile)) {
            Slog.w(TAG, "Scan install , the apk file " + apkFile + " is not a uninstalled system app's codePath.  Skipping.");
            return false;
        }
        PackageParser.Package pkg = parsePackage(apkFile);
        if (!(pkg == null || pkg.providers == null)) {
            try {
                this.mIPmsInner.assertProvidersNotDefined(pkg);
            } catch (PackageManagerException e) {
                Slog.w(TAG, "Scan install, " + e.getMessage());
                return false;
            }
        }
        if (userId == -1) {
            Slog.i(TAG, "Scan install for all users!");
            return true;
        }
        PackageSetting psTemp = this.mIPmsInner.getSettings().getPackageLPr(packageName);
        if (psTemp == null || !psTemp.getInstalled(userId)) {
            return true;
        }
        Slog.w(TAG, "Scan install , " + packageName + " is already installed in user " + userId + " .  Skipping scan " + apkFile);
        return false;
    }

    private PackageParser.Package parsePackage(String apkFile) {
        try {
            return new PackageParser().parsePackage(new File(apkFile), 0, true, 0);
        } catch (PackageParser.PackageParserException e) {
            Slog.w(TAG, "Scan install, parse " + apkFile + " to get package name failed!" + e.getMessage());
            return null;
        }
    }

    private boolean checkScanInstallCaller() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000) {
            return true;
        }
        return SCAN_INSTALL_CALLER_PACKAGES.contains(this.mIPmsInner.getNameForUidInner(callingUid));
    }

    /* JADX WARNING: Code restructure failed: missing block: B:102:0x024d, code lost:
        if (r21.mScanInstallApkList.remove(r23) != false) goto L_0x024f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:103:0x024f, code lost:
        android.util.Slog.i(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install , remove from list:" + r23);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:109:0x026b, code lost:
        android.os.Binder.restoreCallingIdentity(r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x0270, code lost:
        monitor-enter(r21.mScanInstallApkList);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:113:0x0277, code lost:
        if (r21.mScanInstallApkList.remove(r23) != false) goto L_0x0279;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:114:0x0279, code lost:
        android.util.Slog.i(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install , remove from list:" + r23);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:116:0x0290, code lost:
        throw r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:129:?, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0061, code lost:
        r7 = android.os.UserHandle.getUserId(android.os.Binder.getCallingUid());
        r8 = android.os.Binder.clearCallingIdentity();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:?, code lost:
        r0 = r21.mIPmsInner.getSettings().getPackageLPr(r5);
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x007a, code lost:
        if (r0 == null) goto L_0x0176;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0086, code lost:
        if (r0.isAnyInstalled(com.android.server.pm.PackageManagerService.sUserManager.getUserIds()) == false) goto L_0x0176;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x008e, code lost:
        if (r23.equals(r0.codePathString) != false) goto L_0x00b6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0090, code lost:
        if (r22 == null) goto L_0x0093;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0093, code lost:
        android.util.Slog.w(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install ," + r5 + " installed by other user from " + r0.codePathString);
        r4 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x00b7, code lost:
        if (r24 != -1) goto L_0x00c4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x00b9, code lost:
        r11 = r0.queryInstalledUsers(com.android.server.pm.PackageManagerService.sUserManager.getUserIds(), false);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x00c4, code lost:
        r11 = new int[]{r24};
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00c8, code lost:
        r12 = 1;
        r13 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00cc, code lost:
        if (r13 >= r11.length) goto L_0x0131;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00d0, code lost:
        if (r11[r13] == 0) goto L_0x00fe;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00de, code lost:
        if (r21.mIPmsInner.getUserManagerInternalInner().isClonedProfile(r11[r13]) == false) goto L_0x00fe;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00e0, code lost:
        android.util.Slog.d(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install, skipping cloned user " + r11[r13] + com.huawei.hiai.awareness.AwarenessInnerConstants.EXCLAMATORY_MARK_KEY);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00fe, code lost:
        r12 = r21.mIPmsInner.installExistingPackageAsUserInternalInner(r5, r11[r13], 4194304, 0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x010a, code lost:
        if (1 == r12) goto L_0x012d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x010c, code lost:
        android.util.Slog.w(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install failed for user " + r11[r13] + ", installExistingPackageAsUser:" + r23);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x012d, code lost:
        r13 = r13 + 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x0131, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x0132, code lost:
        if (1 != r12) goto L_0x0135;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x0135, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x0137, code lost:
        if (r0 == false) goto L_0x0155;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x0139, code lost:
        r0 = r21.mIPmsInner.getSettings().getPackageLPr(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x0143, code lost:
        if (r0 == null) goto L_0x0155;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:0x0150, code lost:
        if (r0.queryInstalledUsers(com.android.server.pm.PackageManagerService.sUserManager.getUserIds(), false).length != 0) goto L_0x0155;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x0152, code lost:
        removeFromUninstalledDelapp(r5);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:0x0155, code lost:
        android.util.Slog.d(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install , installExistingPackageAsUser:" + r23 + " success:" + r0);
        r4 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x0176, code lost:
        r14 = new java.io.File(r23);
        r11 = 139792;
        r12 = 16;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x0184, code lost:
        if (isNoSystemPreApp(r23) == false) goto L_0x018c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x0186, code lost:
        r12 = 0;
        r11 = 139792 & -131073;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x0194, code lost:
        if (com.android.server.pm.HwPreAppManager.getInstance(r21).isPrivilegedPreApp(r23) == false) goto L_0x0199;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x0196, code lost:
        r11 = 139792 | 262144;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:?, code lost:
        r13 = r21.mIPmsInner.scanPackageLIInner(r14, r12, r11, 0, new android.os.UserHandle(r7), 1107296256);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x01ad, code lost:
        if (r13 == null) goto L_0x01b0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x01b0, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x01b2, code lost:
        if (r0 == false) goto L_0x01bc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x01b4, code lost:
        r21.mIPmsInner.setWhitelistedRestrictedPermissionsInner(r5, r13.requestedPermissions, 2, r24);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x01bc, code lost:
        android.util.Slog.d(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install , restore from :" + r23 + " success:" + r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x01da, code lost:
        r4 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x01dc, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x01dd, code lost:
        android.util.Slog.e(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install, failed to parse package: " + r0.getMessage());
        r4 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:92:0x0221, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:?, code lost:
        android.util.Slog.e(com.android.server.pm.HwPackageManagerServiceEx.TAG, "Scan install " + r23 + " failed!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:98:0x0240, code lost:
        android.os.Binder.restoreCallingIdentity(r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:0x0246, code lost:
        monitor-enter(r21.mScanInstallApkList);
     */
    @Override // com.android.server.pm.IHwPackageManagerServiceExInner
    public boolean scanInstallApk(String packageName, String apkFile, int userId) {
        String pkgName;
        long token;
        boolean success;
        if (!checkScanInstallCaller()) {
            Slog.w(TAG, "Scan install ,check caller failed!");
            return false;
        } else if (apkFile == null || !isPreRemovableApp(apkFile)) {
            Slog.d(TAG, "Illegal install apk file:" + apkFile);
            return false;
        } else {
            if (TextUtils.isEmpty(packageName)) {
                PackageParser.Package pkg = parsePackage(apkFile);
                if (pkg == null) {
                    Slog.w(TAG, "Scan install, get package name failed, pkg is null!");
                    return false;
                }
                pkgName = pkg.packageName;
            } else {
                pkgName = packageName;
            }
            synchronized (this.mScanInstallApkList) {
                if (!assertScanInstallApkLocked(pkgName, apkFile, userId)) {
                    return false;
                }
                this.mScanInstallApkList.add(apkFile);
                Slog.i(TAG, "Scan install , add to list:" + apkFile);
            }
        }
        Binder.restoreCallingIdentity(token);
        synchronized (this.mScanInstallApkList) {
            if (this.mScanInstallApkList.remove(apkFile)) {
                Slog.i(TAG, "Scan install , remove from list:" + apkFile);
            }
        }
        return success;
    }

    public boolean scanInstallApk(String apkFile) {
        return scanInstallApk(null, apkFile, UserHandle.getUserId(Binder.getCallingUid()));
    }

    public List<String> getScanInstallList() {
        return HwUninstalledAppManager.getInstance(this, this).getScanInstallList();
    }

    public void doPostScanInstall(PackageParser.Package pkg, UserHandle user, boolean isNewInstall, int hwFlags, PackageParser.Package scannedPkg) {
        int userId = user != null ? user.getIdentifier() : 0;
        if (needSetPermissionFlag() && scannedPkg != null && HwPreAppManager.getInstance(this).isAppNonSystemPartitionDir(pkg.codePath)) {
            this.mIPmsInner.setWhitelistedRestrictedPermissionsInner(pkg.packageName, pkg.requestedPermissions, 2, userId);
        }
        if (scannedPkg != null) {
            HotInstall.getInstance();
            HotInstall.recordAutoInstallPkg(pkg);
        }
        if ((hwFlags & 1073741824) != 0 && isPreRemovableApp(pkg.codePath)) {
            PackageManagerService.PackageInstalledInfo res = new PackageManagerService.PackageInstalledInfo();
            res.setReturnCode(1);
            res.uid = -1;
            if (isNewInstall) {
                res.origUsers = new int[]{user.getIdentifier()};
            } else {
                res.origUsers = this.mIPmsInner.getSettings().getPackageLPr(pkg.packageName).queryInstalledUsers(PackageManagerService.sUserManager.getUserIds(), true);
            }
            res.pkg = null;
            res.removedInfo = null;
            try {
                this.mIPmsInner.updateSharedLibrariesLPrInner(pkg, (PackageParser.Package) null);
            } catch (PackageManagerException e) {
                Slog.e(TAG, "updateSharedLibrariesLPr failed: " + e.getMessage());
            }
            this.mIPmsInner.updateSettingsLIInner(pkg, "com.huawei.android.launcher", PackageManagerService.sUserManager.getUserIds(), res, user, 4);
            this.mIPmsInner.prepareAppDataAfterInstallLIFInner(pkg);
            Bundle extras = new Bundle();
            extras.putInt("android.intent.extra.UID", pkg.applicationInfo != null ? pkg.applicationInfo.uid : 0);
            this.mIPmsInner.sendPackageBroadcastInner("android.intent.action.PACKAGE_ADDED", pkg.packageName, extras, 0, (String) null, (IIntentReceiver) null, new int[]{user.getIdentifier()}, (int[]) null);
            PackageSetting psTemp = this.mIPmsInner.getSettings().getPackageLPr(pkg.packageName);
            if (psTemp != null) {
                int[] iUser = psTemp.queryInstalledUsers(PackageManagerService.sUserManager.getUserIds(), false);
                int countClonedUser = 0;
                for (int i = 0; i < iUser.length; i++) {
                    if (this.mIPmsInner.getUserManagerInternalInner().isClonedProfile(iUser[i])) {
                        countClonedUser++;
                        Slog.d(TAG, iUser[i] + " skiped, it is cloned user when install package:" + pkg.packageName);
                    }
                }
                if (iUser.length == 0 || iUser.length == countClonedUser) {
                    removeFromUninstalledDelapp(pkg.packageName);
                }
            }
            Slog.d(TAG, "Scan install done for package:" + pkg.packageName);
        }
    }

    private boolean needSetPermissionFlag() {
        if (this.mIPmsInner.getSystemReadyInner()) {
            return true;
        }
        if (this.mIPmsInner.isUpgrade()) {
            return false;
        }
        if (TextUtils.isEmpty(REGIONAL_PHONE_SWITCH) && TextUtils.isEmpty(TME_CUSTOMIZE_SWITCH) && TextUtils.isEmpty(ECOTA_VERSION)) {
            return false;
        }
        return true;
    }

    private void removeFromUninstalledDelapp(String s) {
        HwUninstalledAppManager.getInstance(this, this).removeFromUninstalledDelapp(s);
    }

    public void recordUninstalledDelapp(String s, String path) {
        HwUninstalledAppManager.getInstance(this, this).recordUninstalledDelapp(s, path);
    }

    public void readPreInstallApkList() {
        HwPreAppManager.getInstance(this).readPreInstallApkList();
    }

    private boolean isNoSystemPreApp(String codePath) {
        return HwPackageManagerServiceUtils.isNoSystemPreApp(codePath);
    }

    public boolean isPreRemovableApp(String codePath) {
        return HwPreAppManager.getInstance(this).isPreRemovableApp(codePath);
    }

    public void parseInstalledPkgInfo(String pkgUri, String pkgName, String pkgVerName, int pkgVerCode, int resultCode, boolean pkgUpdate) {
        String installedPath = "";
        String installerPackageName = "";
        if (!(pkgUri == null || pkgUri.length() == 0)) {
            int splitIndex = pkgUri.indexOf(";");
            if (splitIndex >= 0) {
                installedPath = pkgUri.substring(0, splitIndex);
                installerPackageName = pkgUri.substring(splitIndex + 1);
            } else {
                installerPackageName = pkgUri;
            }
        }
        Bundle extrasInfo = new Bundle(1);
        extrasInfo.putString("pkgName", pkgName);
        extrasInfo.putInt(INSTALLATION_EXTRA_PACKAGE_VERSION_CODE, pkgVerCode);
        extrasInfo.putString(INSTALLATION_EXTRA_PACKAGE_VERSION_NAME, pkgVerName);
        extrasInfo.putBoolean(INSTALLATION_EXTRA_PACKAGE_UPDATE, pkgUpdate);
        extrasInfo.putInt(INSTALLATION_EXTRA_PACKAGE_INSTALL_RESULT, resultCode);
        extrasInfo.putString(INSTALLATION_EXTRA_PACKAGE_URI, installedPath);
        extrasInfo.putString(INSTALLATION_EXTRA_INSTALLER_PACKAGE_NAME, installerPackageName);
        Intent intentInfo = new Intent(ACTION_GET_PACKAGE_INSTALLATION_INFO);
        intentInfo.putExtras(extrasInfo);
        intentInfo.setFlags(1073741824);
        this.mContext.sendBroadcast(intentInfo, BROADCAST_PERMISSION);
        Slog.v(TAG, "POST_INSTALL:  pkgName = " + pkgName + ", pkgUri = " + pkgUri + ", pkgInstalledPath = " + installedPath + ", pkgInstallerPackageName = " + installerPackageName + ", pkgVerName = " + pkgVerName + ", pkgVerCode = " + pkgVerCode + ", resultCode = " + resultCode + ", pkgUpdate = " + pkgUpdate);
    }

    @Override // com.android.server.pm.IHwPackageManagerServiceExInner
    public boolean containDelPath(String sensePath) {
        return HwDelAppManager.getInstance(this).containDelPath(sensePath);
    }

    public void addUpdatedRemoveableAppFlag(String scanFileString, String packageName) {
        HwPreAppManager.getInstance(this).addUpdatedRemoveableAppFlag(scanFileString, packageName);
    }

    public boolean needAddUpdatedRemoveableAppFlag(String packageName) {
        return HwPreAppManager.getInstance(this).needAddUpdatedRemoveableAppFlag(packageName);
    }

    public boolean isUnAppInstallAllowed(String originPath) {
        HwCustPackageManagerService mCustPackageManagerService = this.mIPmsInner.getHwPMSCustPackageManagerService();
        if (mCustPackageManagerService == null || !mCustPackageManagerService.isUnAppInstallAllowed(originPath, this.mContext)) {
            return false;
        }
        return true;
    }

    public boolean isPrivAppNonSystemPartitionDir(File path) {
        return HwPreAppManager.getInstance(this).isPrivAppNonSystemPartitionDir(path);
    }

    public void scanNonSystemPartitionDir(int scanMode) {
        long startTime = HwPackageManagerServiceUtils.hwTimingsBegin();
        HwPreAppManager.getInstance(this).scanNonSystemPartitionDir(scanMode);
        HwPackageManagerServiceUtils.hwTimingsEnd(TAG, "scanNonSystemPartitionDir", startTime);
    }

    public void scanNoSysAppInNonSystemPartitionDir(int scanMode) {
        long startTime = HwPackageManagerServiceUtils.hwTimingsBegin();
        HwPreAppManager.getInstance(this).scanNoSysAppInNonSystemPartitionDir(scanMode);
        HwPackageManagerServiceUtils.hwTimingsEnd(TAG, "scanNoSysAppInNonSystemPartitionDir", startTime);
    }

    public void setHdbKey(String key) {
        HwAdbManager.setHdbKey(key);
    }

    public void loadCorrectUninstallDelapp() {
        HwUninstalledAppManager.getInstance(this, this).loadCorrectUninstallDelapp();
    }

    public void addUnisntallDataToCache(String packageName, String codePath) {
        HwUninstalledAppManager.getInstance(this, this).addUnisntallDataToCache(packageName, codePath);
    }

    public boolean checkUninstalledSystemApp(PackageParser.Package pkg, PackageManagerService.InstallArgs args, PackageManagerService.PackageInstalledInfo res) throws PackageManagerException {
        return HwPreAppManager.getInstance(this).checkUninstalledSystemApp(pkg, args, res);
    }

    @Override // com.android.server.pm.IHwPackageManagerServiceExInner
    public IHwPackageManagerInner getIPmsInner() {
        return this.mIPmsInner;
    }

    @Override // com.android.server.pm.IHwPackageManagerServiceExInner
    public Map<String, String> getUninstalledMap() {
        return HwUninstalledAppManager.getInstance(this, this).getUninstalledMap();
    }

    public boolean pmInstallHwTheme(String themePath, boolean setwallpaper, int userId) {
        return HwThemeInstaller.getInstance(this.mContext).pmInstallHwTheme(themePath, setwallpaper, userId);
    }

    public void onUserRemoved(int userId) {
        HwThemeInstaller.getInstance(this.mContext).onUserRemoved(userId);
    }

    public boolean isAppInstallAllowed(String appName) {
        return !isInMspesForbidInstallPackageList(appName);
    }

    private void checkAndEnableWebview() {
        if (this.mIPmsInner.isUpgrade() && this.mPms != null) {
            boolean isChina = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
            try {
                int state = this.mPms.getApplicationEnabledSetting("com.google.android.webview", this.mContext.getUserId());
                boolean isEnabled = true;
                if (state != 1) {
                    isEnabled = false;
                }
                Slog.i(TAG, "WebViewGoogle state=" + state + " version is china = " + isChina);
                if (!isEnabled && isChina) {
                    Slog.i(TAG, "current WebViewGoogle disable, enable it");
                    this.mPms.setApplicationEnabledSetting("com.google.android.webview", 1, 0, this.mContext.getUserId(), this.mContext.getOpPackageName());
                }
            } catch (Exception e) {
                Slog.w(TAG, "checkAndEnableWebview, enable WebViewGoogle exception");
            }
        }
    }

    public boolean verifyPackageSecurityPolicy(String packageName, File baseApkPath) {
        ISecurityProfileController spc = HwServiceFactory.getSecurityProfileController();
        if (spc == null || spc.verifyPackage(packageName, baseApkPath)) {
            return true;
        }
        Slog.e(TAG, "Security policy verification failed");
        return false;
    }

    public void reportEventStream(int eventId, String message) {
        if (907400027 != eventId && !TextUtils.isEmpty(message)) {
            IMonitor.EventStream eventStream = IMonitor.openEventStream(eventId);
            if (eventStream != null) {
                eventStream.setParam("message", message);
                IMonitor.sendEvent(eventStream);
            }
            IMonitor.closeEventStream(eventStream);
        }
    }

    public void deleteExistsIfNeedForHwPMS() {
        ArrayList<ArrayList<File>> allApkInstallList;
        boolean z;
        ArrayList<ArrayList<File>> allApkInstallList2 = getCotaApkInstallXMLPath();
        int i = 0;
        boolean z2 = true;
        File[] files = getCotaApkInstallXMLFile((HashSet) getAllCotaApkPath(allApkInstallList2.get(0), allApkInstallList2.get(1)));
        int i2 = 0;
        while (i2 < files.length) {
            try {
                Log.i(TAG, "deleteExistsIfNeed files " + i2 + "  " + files[i2]);
                PackageParser pp = new PackageParser();
                pp.setCallback(new ParserCallback());
                PackageParser.Package pkg = pp.parsePackage(files[i2], 16, z2, i);
                if (pkg == null) {
                    allApkInstallList = allApkInstallList2;
                    z = z2;
                } else {
                    String pkgName = pkg.packageName;
                    PackageParser.Package oldPkg = (PackageParser.Package) this.mIPmsInner.getPackagesLock().get(pkgName);
                    if (oldPkg == null) {
                        allApkInstallList = allApkInstallList2;
                        z = z2;
                    } else {
                        String oldCodePath = oldPkg.codePath;
                        String oldApkName = new File(oldCodePath).getName();
                        PackageSetting ps = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(pkgName);
                        StringBuilder sb = new StringBuilder();
                        allApkInstallList = allApkInstallList2;
                        try {
                            sb.append("parsePackage  pkg= ");
                            sb.append(pkgName);
                            sb.append(" ,oldCodePath = ");
                            sb.append(oldCodePath);
                            sb.append(" ,oldApkName = ");
                            sb.append(oldApkName);
                            sb.append(" ,ps = ");
                            sb.append(ps);
                            Log.i(TAG, sb.toString());
                            if (!this.mIPmsInner.getPackagesLock().containsKey(pkgName)) {
                                z = true;
                            } else if (ps == null) {
                                z = true;
                            } else if (oldCodePath.startsWith("/data/app")) {
                                this.mDataApkShouldNotUpdateByCota.add(files[i2].getCanonicalPath());
                                Log.i(TAG, "deleteExistsIfNeed ignore " + files[i2].getCanonicalPath());
                                z = true;
                            } else if (oldCodePath.startsWith("/cust/cota")) {
                                Log.i(TAG, "removePackageLI pkgName= " + pkgName + " ,oldApkName= " + oldApkName);
                                this.mIPmsInner.killApplicationInner(pkgName, ps.appId, "killed by cota");
                                z = true;
                                this.mIPmsInner.removePackageLIInner(oldPkg, true);
                                deletePackageCache(oldApkName);
                            } else {
                                z = true;
                                Log.i(TAG, "not belong to the above two situations, do nothing");
                            }
                        } catch (PackageParser.PackageParserException e) {
                            e = e;
                            Log.e(TAG, "HWPMEX.deleteExistsIfNeedForHwPMS error for PackageParserException , " + e.getMessage());
                            return;
                        } catch (IOException e2) {
                            ex = e2;
                            Log.e(TAG, "HWPMEX.deleteExistsIfNeedForHwPMS error for IO , " + ex.getMessage());
                            return;
                        }
                    }
                }
                i2++;
                z2 = z;
                allApkInstallList2 = allApkInstallList;
                i = 0;
            } catch (PackageParser.PackageParserException e3) {
                e = e3;
                Log.e(TAG, "HWPMEX.deleteExistsIfNeedForHwPMS error for PackageParserException , " + e.getMessage());
                return;
            } catch (IOException e4) {
                ex = e4;
                Log.e(TAG, "HWPMEX.deleteExistsIfNeedForHwPMS error for IO , " + ex.getMessage());
                return;
            }
        }
    }

    private class ParserCallback implements PackageParser.Callback {
        private ParserCallback() {
        }

        public boolean hasFeature(String feature) {
            return false;
        }

        public String[] getOverlayPaths(String targetPackageName, String targetPath) {
            return null;
        }

        public String[] getOverlayApks(String targetPackageName) {
            return null;
        }
    }

    private class MySysAppInfo {
        private boolean isPrivileged;
        private boolean isUndetachable;
        private String pkgName;
        private String pkgSignature;

        MySysAppInfo(String name, String sign, String priv, String del) {
            this.pkgName = name;
            this.pkgSignature = sign;
            if ("true".equals(priv)) {
                this.isPrivileged = true;
            } else {
                this.isPrivileged = false;
            }
            if ("true".equals(del)) {
                this.isUndetachable = true;
            } else {
                this.isUndetachable = false;
            }
        }

        /* access modifiers changed from: package-private */
        public String getPkgName() {
            return this.pkgName;
        }

        /* access modifiers changed from: package-private */
        public void setPkgName(String value) {
            this.pkgName = value;
        }

        /* access modifiers changed from: package-private */
        public String getPkgSignature() {
            return this.pkgSignature;
        }

        /* access modifiers changed from: package-private */
        public void setPkgSignature(String value) {
            this.pkgSignature = value;
        }

        /* access modifiers changed from: package-private */
        public boolean getPrivileged() {
            return this.isPrivileged;
        }

        /* access modifiers changed from: package-private */
        public void setPrivileged(boolean value) {
            this.isPrivileged = value;
        }

        /* access modifiers changed from: package-private */
        public boolean getUndetachable() {
            return this.isUndetachable;
        }

        /* access modifiers changed from: package-private */
        public void setUndetachable(boolean value) {
            this.isUndetachable = value;
        }
    }

    public ArrayList<String> getDataApkShouldNotUpdateByCota() {
        return this.mDataApkShouldNotUpdateByCota;
    }

    private Set<String> getAPKInstallPathList(File scanApk) {
        Set<String> apkInstallPathList = new HashSet<>();
        BufferedReader reader = null;
        try {
            HwPreAppManager appManager = HwPreAppManager.getInstance(this);
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(scanApk), "UTF-8"));
            while (true) {
                String line = reader2.readLine();
                if (line != null) {
                    String[] strSplit = line.trim().split(",");
                    if (strSplit.length != 0) {
                        String packagePath = appManager.replaceCotaPath(scanApk.getPath(), HwPackageManagerServiceUtils.getCustPackagePath(strSplit[0]));
                        Log.i(TAG, "getAPKInstallPathList packagePath= " + packagePath);
                        apkInstallPathList.add(packagePath);
                    }
                } else {
                    try {
                        break;
                    } catch (IOException e) {
                        Log.e(TAG, "HWPMEX.getAPKInstallPathList error for closing IO");
                    }
                }
            }
            reader2.close();
        } catch (IOException e2) {
            Log.e(TAG, "HWPMEX.getAPKInstallPathList error for IO");
            if (0 != 0) {
                reader.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    reader.close();
                } catch (IOException e3) {
                    Log.e(TAG, "HWPMEX.getAPKInstallPathList error for closing IO");
                }
            }
            throw th;
        }
        return apkInstallPathList;
    }

    private Set<String> getAllCotaApkPath(ArrayList<File> installPath, ArrayList<File> delInstallPath) {
        Set<String> allCotaApkPath = new HashSet<>();
        int installPathSize = installPath.size();
        for (int i = 0; i < installPathSize; i++) {
            File file = installPath.get(i);
            if (file != null && file.exists()) {
                allCotaApkPath.addAll(getAPKInstallPathList(file));
            }
        }
        int delInstallPathSize = delInstallPath.size();
        for (int i2 = 0; i2 < delInstallPathSize; i2++) {
            File file2 = delInstallPath.get(i2);
            if (file2 != null && file2.exists()) {
                allCotaApkPath.addAll(getAPKInstallPathList(file2));
            }
        }
        return allCotaApkPath;
    }

    private static File[] getCotaApkInstallXMLFile(Set<String> allCotaApkPath) {
        int fileSize = allCotaApkPath.size();
        File[] files = new File[fileSize];
        int i = 0;
        for (String installPath : allCotaApkPath) {
            File file = new File(installPath);
            if (i < fileSize) {
                files[i] = file;
                i++;
            }
        }
        return files;
    }

    public static ArrayList<ArrayList<File>> getCotaApkInstallXMLPath() {
        ArrayList<File> apkInstallList = new ArrayList<>();
        ArrayList<File> apkDelInstallList = new ArrayList<>();
        File apkInstallFile = new File(SIMPLE_COTA_APK_XML_PATH);
        File apkDelInstallListFile = new File(SIMPLE_COTA_DEL_APK_XML_PATH);
        if (apkInstallFile.exists() || apkDelInstallListFile.exists()) {
            apkInstallList.add(apkInstallFile);
            apkDelInstallList.add(apkDelInstallListFile);
        } else {
            apkInstallList = getCotaXMLFile(COTA_APK_XML_PATH);
            apkDelInstallList = getCotaXMLFile(COTA_DEL_APK_XML_PATH);
        }
        apkInstallList.size();
        ArrayList<ArrayList<File>> result = new ArrayList<>();
        result.add(apkInstallList);
        result.add(apkDelInstallList);
        return result;
    }

    private static void deletePackageCache(String apkName) {
        File[] listOfFiles;
        File cacheDir;
        File[] allPackageNameFile;
        File cacheBaseDir = new File(CACHE_BASE_DIR);
        if (cacheBaseDir.exists() && (listOfFiles = cacheBaseDir.listFiles()) != null && listOfFiles.length > 0 && (cacheDir = listOfFiles[0]) != null && (allPackageNameFile = cacheDir.listFiles()) != null && allPackageNameFile.length > 0) {
            for (int i = 0; i < allPackageNameFile.length; i++) {
                String name = allPackageNameFile[i].getName();
                if (name.startsWith(apkName + AwarenessInnerConstants.DASH_KEY)) {
                    Log.i(TAG, "deletePackageCache " + allPackageNameFile[i]);
                    allPackageNameFile[i].delete();
                }
            }
        }
    }

    public static ArrayList<File> getSysdllInstallXMLPath() {
        ArrayList<File> result = new ArrayList<>();
        File sysdllFile = null;
        try {
            sysdllFile = HwCfgFilePolicy.getCfgFile(HwPackageManagerService.SYSDLL_PATH, 0);
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "getSysdllInstallXMLPath getCfgFile NoClassDefFoundError");
        }
        if (sysdllFile != null) {
            result.add(sysdllFile);
        }
        return result;
    }

    private static ArrayList<File> getCotaXMLFile(String xmlType) {
        ArrayList<File> cotaFile = new ArrayList<>();
        try {
            String[] policyDir = HwCfgFilePolicy.getCfgPolicyDir(0);
            for (int i = 0; i < policyDir.length; i++) {
                if (policyDir[i] != null && policyDir[i].startsWith("/cust/cota")) {
                    cotaFile.add(new File(policyDir[i] + "/" + xmlType));
                    Log.i(TAG, "getCotaXMLFile add = " + policyDir[i] + "/" + xmlType);
                }
            }
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "HwCfgFilePolicy getCotaXMLFile NoClassDefFoundError");
        }
        return cotaFile;
    }

    public String readMspesFile(String fileName) {
        return MspesExUtil.getInstance(this).readMspesFile(fileName);
    }

    public boolean writeMspesFile(String fileName, String content) {
        return MspesExUtil.getInstance(this).writeMspesFile(fileName, content);
    }

    public String getMspesOEMConfig() {
        return MspesExUtil.getInstance(this).getMspesOEMConfig();
    }

    public int updateMspesOEMConfig(String src) {
        return MspesExUtil.getInstance(this).updateMspesOEMConfig(src);
    }

    public boolean isInMspesForbidInstallPackageList(String pkg) {
        return MspesExUtil.getInstance(this).isInMspesForbidInstallPackageList(pkg);
    }

    public void preSendPackageBroadcast(String action, String pkg, String targetPkg) {
        HwAppAuthManager.getInstance().preSendPackageBroadcast(action, pkg, targetPkg);
    }

    public List<String> getSystemWhiteList(String type) {
        this.mContext.enforceCallingOrSelfPermission(ACCESS_SYSTEM_WHITE_LIST, "Permission Denide for getSystemWhiteList!");
        if (FREE_FORM_LIST.equals(type)) {
            return SplitNotificationUtils.getInstance(this.mContext).getListPkgName(3);
        }
        return null;
    }

    public boolean shouldSkipTriggerFreeform(String pkgName, int userId) {
        this.mContext.enforceCallingOrSelfPermission(SKIP_TRIGGER_FREEFORM, "Permission Denide for shouldSkipTriggerFreeform!");
        return SplitNotificationUtils.getInstance(this.mContext).shouldSkipTriggerFreeform(pkgName, userId);
    }

    public int getPrivilegeAppType(String pkgName) {
        return checkInstallGranted(pkgName) ? 1 : 0;
    }

    public void addGrantedInstalledPkg(PackageParser.Package pkg, boolean grant) {
        if (pkg == null) {
            Slog.i(TAG, "addGrantedInstalledPkg package is null");
            return;
        }
        String pkgName = pkg.packageName;
        if (TextUtils.isEmpty(pkgName)) {
            Slog.i(TAG, "addGrantedInstalledPkg packageName is null");
        } else if (!grant) {
            Slog.i(TAG, "addGrantedInstalledPkg not granted: " + pkgName);
        } else {
            synchronized (this.mGrantedInstalledPkg) {
                Slog.i(TAG, "addGrantedInstalledPkg package added: " + pkgName);
                this.mGrantedInstalledPkg.add(pkgName);
            }
        }
    }

    private boolean checkInstallGranted(String pkgName) {
        boolean contains;
        synchronized (this.mGrantedInstalledPkg) {
            contains = this.mGrantedInstalledPkg.contains(pkgName);
        }
        return contains;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0071, code lost:
        r8.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
     */
    public void clearPreferredActivityAsUser(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        int callingUid = Binder.getCallingUid();
        Slog.d(TAG, "clearPreferredActivity " + activity + " for user " + userId + " from uid " + callingUid);
        this.mIPmsInner.getPermissionManager().enforceCrossUserPermission(callingUid, userId, true, false, "clear preferred activity");
        if (this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                if (this.mIPmsInner.getUidTargetSdkVersionLockedLPrEx(callingUid) < 8) {
                    Slog.w(TAG, "Ignoring clearPreferredActivity() from uid " + Binder.getCallingUid());
                    return;
                }
            }
        }
        if (filter.countCategories() == 0) {
            filter.addCategory("android.intent.category.DEFAULT");
        }
        synchronized (this.mIPmsInner.getPackagesLock()) {
            PreferredIntentResolver pir = (PreferredIntentResolver) this.mIPmsInner.getSettings().mPreferredActivities.get(userId);
            if (pir != null) {
                ArrayList<PreferredActivity> existing = pir.findFilters(filter);
                if (existing != null) {
                    for (int i = existing.size() - 1; i >= 0; i--) {
                        PreferredActivity pa = existing.get(i);
                        if (pa.mPref.mAlways && ((activity == null || pa.mPref.mComponent.equals(activity)) && pa.mPref.mMatch == (268369920 & match) && (set == null || pa.mPref.sameSet(set)))) {
                            pir.removeFilter(pa);
                        }
                    }
                    this.mIPmsInner.WritePackageRestrictions(userId);
                    this.mIPmsInner.sendPreferredActivityChangedBroadcast(userId);
                }
            }
        }
    }

    public void registerExtServiceProvider(IExtServiceProvider extServiceProvider, Intent filter) {
        if (this.mExtServiceProvider == null) {
            this.mExtServiceProvider = new ExtServiceProvider();
        }
        this.mExtServiceProvider.registerExtServiceProvider(extServiceProvider, filter);
    }

    public void unregisterExtServiceProvider(IExtServiceProvider extServiceProvider) {
        ExtServiceProvider extServiceProvider2 = this.mExtServiceProvider;
        if (extServiceProvider2 != null) {
            extServiceProvider2.unregisterExtServiceProvider(extServiceProvider);
        }
    }

    public ResolveInfo[] queryExtService(String action, String packageName) {
        if (this.mExtServiceProvider == null) {
            this.mExtServiceProvider = new ExtServiceProvider();
        }
        return this.mExtServiceProvider.queryExtService(action, packageName);
    }

    public boolean isInValidApkPatchFile(File file, int parseFlags) {
        boolean bInValidapk = false;
        if (file == null) {
            return false;
        }
        try {
            PackageParser.Package ppkg = new PackageParser().parsePackage(file, parseFlags);
            if (ppkg.mAppMetaData == null || !ppkg.mAppMetaData.getBoolean(FLAG_APKPATCH_TAG, false)) {
                return false;
            }
            if (!checkAllowtoInstallPatchApk(ppkg.packageName)) {
                bInValidapk = true;
            }
            Slog.i(TAG, "isInvalidApkPatchFile with file " + bInValidapk);
            return bInValidapk;
        } catch (PackageParser.PackageParserException e) {
            Slog.w(TAG, "failed to parse " + file);
            return false;
        }
    }

    public boolean isInValidApkPatchPkg(PackageParser.Package pkg) {
        if (pkg == null || TextUtils.isEmpty(pkg.packageName) || pkg.mAppMetaData == null || !pkg.mAppMetaData.getBoolean(FLAG_APKPATCH_TAG, false)) {
            return false;
        }
        boolean bInValidapk = !checkAllowtoInstallPatchApk(pkg.packageName);
        Slog.i(TAG, "isInvalidApkPatch with pkg " + bInValidapk);
        return bInValidapk;
    }

    private boolean checkAllowtoInstallPatchApk(String srcpkgName) {
        File fileForParse = new File(FLAG_APKPATCH_PATH);
        InputStream is = null;
        boolean bIsLegalPatchPkg = false;
        if (TextUtils.isEmpty(srcpkgName) || !fileForParse.exists()) {
            return false;
        }
        try {
            is = new FileInputStream(fileForParse);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, null);
            XmlUtils.beginDocument(parser, FLAG_APKPATCH_TAGPATCH);
            while (true) {
                XmlUtils.nextElement(parser);
                String element = parser.getName();
                if (element != null) {
                    String packageName = XmlUtils.readStringAttribute(parser, "value");
                    if ("pkgname".equals(element) && srcpkgName.equalsIgnoreCase(packageName)) {
                        Slog.d(TAG, "this is legal apk");
                        bIsLegalPatchPkg = true;
                        break;
                    }
                    bIsLegalPatchPkg = false;
                } else {
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            Slog.e(TAG, "patch info parse fail");
        } catch (IOException | XmlPullParserException e2) {
            Slog.e(TAG, "patch info parse fail & io fail");
        } catch (Throwable th) {
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
        IoUtils.closeQuietly(is);
        return bIsLegalPatchPkg;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0029, code lost:
        if (r14.size() == 0) goto L_0x002b;
     */
    public boolean setForceDarkSetting(List<String> packageNames, int forceDarkMode) {
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
            long callingId = Binder.clearCallingIdentity();
            boolean isSetAll = false;
            try {
                synchronized (this.mIPmsInner.getPackagesLock()) {
                    if (packageNames != null) {
                    }
                    packageNames = getAllSupportForceDarkApps();
                    isSetAll = true;
                    boolean isChanged = false;
                    for (String packageName : packageNames) {
                        PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                        if (!(pkgSetting == null || pkgSetting.pkg == null)) {
                            if (pkgSetting.pkg.applicationInfo != null) {
                                int realMode = pkgSetting.getForceDarkMode() & -129;
                                if (DEBUG) {
                                    Slog.d(TAG, "setForceDarkSetting packageName: " + packageName + ", realMode: " + realMode + ", mode: " + forceDarkMode);
                                }
                                if (realMode != forceDarkMode) {
                                    isChanged = true;
                                    pkgSetting.setForceDarkMode(forceDarkMode | 128);
                                    pkgSetting.pkg.applicationInfo.forceDarkMode = forceDarkMode;
                                }
                            }
                        }
                        Slog.d(TAG, "setForceDarkSetting pkgSetting is null for packageName: " + packageName);
                    }
                    if (isChanged) {
                        this.mIPmsInner.getSettings().writeLPr();
                    }
                }
                updateOrStopPackage(isSetAll, packageNames);
                return true;
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        } else {
            throw new SecurityException("Only the system can set app force dark mode");
        }
    }

    public int getForceDarkSetting(String packageName) {
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                PackageSetting pkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(packageName);
                if (pkgSetting == null) {
                    Slog.d(TAG, "getForceDarkSetting pkgSetting is null for packageName: " + packageName);
                    return 2;
                }
                int forceDarkMode = pkgSetting.getForceDarkMode() & -129;
                Binder.restoreCallingIdentity(callingId);
                return forceDarkMode;
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public void updateForceDarkMode(String pkgName, PackageSetting ps) {
        if (ps == null) {
            Slog.d(TAG, "updateForceDarkMode ps is null for pkgName: " + pkgName);
            return;
        }
        ps.forceDarkMode = HwForceDarkModeConfig.getInstance().getForceDarkModeFromAppTypeRecoManager(pkgName, ps);
    }

    private List<String> getAllSupportForceDarkApps() {
        List<String> packageNames = new ArrayList<>();
        for (String pkgName : this.mIPmsInner.getSettings().mPackages.keySet()) {
            if (getForceDarkSetting(pkgName) != 2) {
                packageNames.add(pkgName);
            }
        }
        return packageNames;
    }

    private void updateOrStopPackage(boolean isSetAll, List<String> packageNames) {
        if (isSetAll) {
            updateConfiguration();
            return;
        }
        for (String packageName : packageNames) {
            forceStopPackage(packageName);
        }
    }

    private void forceStopPackage(String packageName) {
        IActivityManager am = ActivityManagerNative.getDefault();
        if (am == null) {
            Slog.e(TAG, "forceStopPackage am is null for package " + packageName);
            return;
        }
        try {
            am.forceStopPackage(packageName, -1);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to force stop package of " + packageName + " for RemoteException.");
        } catch (SecurityException e2) {
            Slog.e(TAG, "forceStopPackage permission denial, requires FORCE_STOP_PACKAGES");
        }
    }

    private void updateConfiguration() {
        HwThemeManager.IHwThemeManager manager = HwFrameworkFactory.getHwThemeManagerFactory().getThemeManagerInstance();
        if (manager != null) {
            manager.updateConfiguration();
        } else {
            Slog.e(TAG, "updateConfiguration manager is null");
        }
    }

    public void updateWhitelistByHot() {
        new WhitelistUpdateThread(this.mContext, "/data/cota/para/xml/HwExtDisplay/fold", "hw_tahiti_app_aspect_list.xml", new HotUpdateRunnable(HwPackageParser.getTahitiAppAspectList(), HwPackageParser.getTahitiAppVersionCodeList())).start();
    }

    private class HotUpdateRunnable implements Runnable {
        private static final double COMPARISON = 1.0E-8d;
        Map<String, Float> mAppAspects = null;
        Map<String, Integer> mAppVersionCodes = null;

        protected HotUpdateRunnable(Map<String, Float> appAspects, Map<String, Integer> appVersionCodes) {
            this.mAppAspects = appAspects;
            this.mAppVersionCodes = appVersionCodes;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:111:0x02c5, code lost:
            r2 = r2;
            r4 = r18;
            r5 = r19;
            r3 = r3;
            r6 = r6;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:36:0x0110, code lost:
            r18 = r4;
            r19 = r5;
         */
        /* JADX WARNING: Removed duplicated region for block: B:59:0x019c A[ADDED_TO_REGION, Catch:{ all -> 0x02db }] */
        /* JADX WARNING: Removed duplicated region for block: B:81:0x023e A[Catch:{ all -> 0x02d1, all -> 0x02f0 }] */
        public void run() {
            PackageParser pp;
            Set<String> pkgs;
            boolean isUserSet;
            Float after;
            int newCode;
            HwPackageParser.initTahitiAppAspectList();
            Map<String, Float> appAspectsAfterUpdate = HwPackageParser.getTahitiAppAspectList();
            Map<String, Integer> appVersionCodesAfterUpdate = HwPackageParser.getTahitiAppVersionCodeList();
            Set<String> pkgs2 = new HashSet<>();
            pkgs2.addAll(this.mAppAspects.keySet());
            pkgs2.addAll(appAspectsAfterUpdate.keySet());
            PackageParser pp2 = new PackageParser();
            Set<String> pkgsChanges = new HashSet<>();
            List<String> pkgsToStops = new ArrayList<>();
            for (String pkg : pkgs2) {
                if (!TextUtils.isEmpty(pkg)) {
                    Float before = this.mAppAspects.get(pkg);
                    Float after2 = appAspectsAfterUpdate.get(pkg);
                    int newCode2 = 0;
                    int oldCode = 0;
                    if (appVersionCodesAfterUpdate != null && appVersionCodesAfterUpdate.containsKey(pkg)) {
                        newCode2 = appVersionCodesAfterUpdate.get(pkg).intValue();
                    }
                    Map<String, Integer> map = this.mAppVersionCodes;
                    if (map != null && map.containsKey(pkg)) {
                        oldCode = this.mAppVersionCodes.get(pkg).intValue();
                    }
                    if (before == null || after2 == null || ((double) Math.abs(before.floatValue() - after2.floatValue())) >= COMPARISON || newCode2 != oldCode) {
                        Slog.i(HwPackageManagerServiceEx.TAG, "hot update changed pkg: " + pkg);
                        pkgsChanges.add(pkg);
                    }
                }
            }
            boolean isShouldUpdate = false;
            for (String pkg2 : pkgsChanges) {
                if (TextUtils.isEmpty(pkg2)) {
                    pkgs = pkgs2;
                    pp = pp2;
                } else {
                    synchronized (HwPackageManagerServiceEx.this.getIPmsInner().getPackagesLock()) {
                        try {
                            PackageSetting pkgSetting = (PackageSetting) HwPackageManagerServiceEx.this.getIPmsInner().getSettings().mPackages.get(pkg2);
                            if (pkgSetting == null) {
                                try {
                                    Slog.i(HwPackageManagerServiceEx.TAG, "hot update but not installed package " + pkg2);
                                } catch (Throwable th) {
                                    pkgSetting = th;
                                    throw pkgSetting;
                                }
                            } else {
                                Float pkgSettingSet = Float.valueOf(pkgSetting.getAspectRatio("minAspectRatio"));
                                Float before2 = this.mAppAspects.get(pkg2);
                                int oldCode2 = 0;
                                if (this.mAppVersionCodes != null && this.mAppVersionCodes.containsKey(pkg2)) {
                                    oldCode2 = this.mAppVersionCodes.get(pkg2).intValue();
                                }
                                if (before2 != null) {
                                    try {
                                        if (isAspectValid(pkgSetting.pkg.mVersionCode, oldCode2)) {
                                            pkgs = pkgs2;
                                            pp = pp2;
                                            if (((double) Math.abs(before2.floatValue() - pkgSettingSet.floatValue())) < COMPARISON) {
                                                isUserSet = false;
                                                if ((pkgSettingSet.floatValue() != 0.0f || !isUserSet) && pkgSetting.pkg.applicationInfo.canChangeAspectRatio("minAspectRatio")) {
                                                    after = appAspectsAfterUpdate.get(pkg2);
                                                    newCode = 0;
                                                    if (appVersionCodesAfterUpdate != null && appVersionCodesAfterUpdate.containsKey(pkg2)) {
                                                        newCode = appVersionCodesAfterUpdate.get(pkg2).intValue();
                                                    }
                                                    int currentCode = pkgSetting.pkg.mVersionCode;
                                                    StringBuilder sb = new StringBuilder();
                                                    sb.append("hot update pkg: ");
                                                    sb.append(pkg2);
                                                    sb.append("code: ");
                                                    sb.append(currentCode);
                                                    sb.append(" old minAspect: ");
                                                    sb.append(before2);
                                                    sb.append(" code: ");
                                                    sb.append(oldCode2);
                                                    sb.append(" new minAspect: ");
                                                    sb.append(after);
                                                    sb.append(" code: ");
                                                    sb.append(newCode);
                                                    Slog.i(HwPackageManagerServiceEx.TAG, sb.toString());
                                                    if ((before2 != null || !isAspectValid(currentCode, oldCode2)) && after != null && isAspectValid(currentCode, newCode)) {
                                                        pkgSetting.setAspectRatio("minAspectRatio", after.floatValue());
                                                    } else if (before2 != null && isAspectValid(currentCode, oldCode2) && (after == null || !isAspectValid(currentCode, newCode))) {
                                                        Float tempAspect = Float.valueOf(HwFoldScreenState.getScreenFoldFullRatio());
                                                        Iterator it = pkgSetting.pkg.activities.iterator();
                                                        while (true) {
                                                            if (!it.hasNext()) {
                                                                break;
                                                            } else if (!((PackageParser.Activity) it.next()).isResizeable()) {
                                                                tempAspect = Float.valueOf(1.3333334f);
                                                                break;
                                                            }
                                                        }
                                                        pkgSetting.setAspectRatio("minAspectRatio", tempAspect.floatValue());
                                                    } else if (before2 == null || !isAspectValid(currentCode, oldCode2) || after == null || !isAspectValid(currentCode, newCode)) {
                                                        Slog.i(HwPackageManagerServiceEx.TAG, "hot update pkg: do nothing!");
                                                    } else {
                                                        pkgSetting.setAspectRatio("minAspectRatio", after.floatValue());
                                                    }
                                                    pkgsToStops.add(pkg2);
                                                    isShouldUpdate = true;
                                                } else {
                                                    try {
                                                        Slog.i(HwPackageManagerServiceEx.TAG, "hot update skip " + pkg2 + " for reason user set or can not change");
                                                    } catch (Throwable th2) {
                                                        pkgSetting = th2;
                                                        throw pkgSetting;
                                                    }
                                                }
                                            }
                                            isUserSet = true;
                                            if (pkgSettingSet.floatValue() != 0.0f) {
                                            }
                                            after = appAspectsAfterUpdate.get(pkg2);
                                            newCode = 0;
                                            newCode = appVersionCodesAfterUpdate.get(pkg2).intValue();
                                            int currentCode2 = pkgSetting.pkg.mVersionCode;
                                            try {
                                                StringBuilder sb2 = new StringBuilder();
                                                sb2.append("hot update pkg: ");
                                                sb2.append(pkg2);
                                                sb2.append("code: ");
                                                sb2.append(currentCode2);
                                                sb2.append(" old minAspect: ");
                                                sb2.append(before2);
                                                sb2.append(" code: ");
                                                sb2.append(oldCode2);
                                                sb2.append(" new minAspect: ");
                                                sb2.append(after);
                                                sb2.append(" code: ");
                                                sb2.append(newCode);
                                                Slog.i(HwPackageManagerServiceEx.TAG, sb2.toString());
                                                if (before2 != null) {
                                                }
                                                pkgSetting.setAspectRatio("minAspectRatio", after.floatValue());
                                                pkgsToStops.add(pkg2);
                                                isShouldUpdate = true;
                                            } catch (Throwable th3) {
                                                pkgSetting = th3;
                                                throw pkgSetting;
                                            }
                                        }
                                    } catch (Throwable th4) {
                                        pkgSetting = th4;
                                        throw pkgSetting;
                                    }
                                }
                                pkgs = pkgs2;
                                pp = pp2;
                                isUserSet = true;
                                try {
                                    if (pkgSettingSet.floatValue() != 0.0f) {
                                    }
                                    after = appAspectsAfterUpdate.get(pkg2);
                                    newCode = 0;
                                    newCode = appVersionCodesAfterUpdate.get(pkg2).intValue();
                                    int currentCode22 = pkgSetting.pkg.mVersionCode;
                                    StringBuilder sb22 = new StringBuilder();
                                    sb22.append("hot update pkg: ");
                                    sb22.append(pkg2);
                                    sb22.append("code: ");
                                    sb22.append(currentCode22);
                                    sb22.append(" old minAspect: ");
                                    sb22.append(before2);
                                    sb22.append(" code: ");
                                    sb22.append(oldCode2);
                                    sb22.append(" new minAspect: ");
                                    sb22.append(after);
                                    sb22.append(" code: ");
                                    sb22.append(newCode);
                                    Slog.i(HwPackageManagerServiceEx.TAG, sb22.toString());
                                    if (before2 != null) {
                                    }
                                    pkgSetting.setAspectRatio("minAspectRatio", after.floatValue());
                                    pkgsToStops.add(pkg2);
                                    isShouldUpdate = true;
                                } catch (Throwable th5) {
                                    pkgSetting = th5;
                                    throw pkgSetting;
                                }
                            }
                        } catch (Throwable th6) {
                            pkgSetting = th6;
                            throw pkgSetting;
                        }
                    }
                }
                pkgs2 = pkgs;
                pp2 = pp;
            }
            if (isShouldUpdate) {
                synchronized (HwPackageManagerServiceEx.this.getIPmsInner().getPackagesLock()) {
                    HwPackageManagerServiceEx.this.getIPmsInner().getSettings().writeLPr();
                }
                if (!HwPackageParser.getIsNeedBootUpdate()) {
                    HwActivityManager.forceStopPackages(pkgsToStops, -1);
                }
            }
            Slog.i(HwPackageManagerServiceEx.TAG, "hot update finish!");
        }

        private boolean isAspectValid(int apkCode, int maxVaildCode) {
            if (maxVaildCode == 0 || apkCode < maxVaildCode) {
                return true;
            }
            return false;
        }
    }

    private static class WhitelistUpdateThread extends Thread {
        Context mContext = null;
        String mFileName = null;
        String mFilePath = null;
        Runnable mRunnable = null;

        protected WhitelistUpdateThread(Context context, String filePath, String fileName, Runnable runnable) {
            super("config update thread");
            this.mContext = context;
            this.mFilePath = filePath;
            this.mFileName = fileName;
            this.mRunnable = runnable;
        }

        public void run() {
            Runnable runnable;
            if (!TextUtils.isEmpty(this.mFilePath) && !TextUtils.isEmpty(this.mFileName)) {
                FileInputStream inputStream = null;
                try {
                    File file = new File(this.mFilePath, this.mFileName);
                    if (file.exists()) {
                        inputStream = new FileInputStream(file);
                    }
                    File targetFileTemp = createFileForWrite(this.mFileName);
                    if (!(targetFileTemp == null || inputStream == null)) {
                        parseConfigsToTargetFile(targetFileTemp, inputStream);
                    }
                    closeInputStream(inputStream);
                    runnable = this.mRunnable;
                    if (runnable == null) {
                        return;
                    }
                } catch (FileNotFoundException e) {
                    Log.e(HwPackageManagerServiceEx.TAG, "FileNotFoundException");
                    closeInputStream(null);
                    runnable = this.mRunnable;
                    if (runnable == null) {
                        return;
                    }
                } catch (Throwable th) {
                    closeInputStream(null);
                    Runnable runnable2 = this.mRunnable;
                    if (runnable2 != null) {
                        runnable2.run();
                    }
                    throw th;
                }
                runnable.run();
            }
        }

        private static File createFileForWrite(String fileName) {
            File file = new File(Environment.getDataSystemDirectory(), fileName);
            if (!file.exists() || file.delete()) {
                try {
                    if (!file.createNewFile()) {
                        Log.e(HwPackageManagerServiceEx.TAG, "createFileForWrite createNewFile error!");
                        return null;
                    }
                    file.setReadable(true, false);
                    return file;
                } catch (IOException ioException) {
                    Log.e(HwPackageManagerServiceEx.TAG, "ioException: " + ioException);
                    return null;
                }
            } else {
                Log.e(HwPackageManagerServiceEx.TAG, "delete file error!");
                return null;
            }
        }

        private void parseConfigsToTargetFile(File targetFile, FileInputStream inputStream) {
            BufferedReader reader = null;
            FileOutputStream outputStream = null;
            InputStreamReader inputStreamReader = null;
            StringBuilder targetStringBuilder = new StringBuilder();
            boolean isRecordStarted = true;
            try {
                inputStreamReader = new InputStreamReader(inputStream, "utf-8");
                reader = new BufferedReader(inputStreamReader);
                while (true) {
                    String tempLineString = reader.readLine();
                    if (tempLineString == null) {
                        break;
                    }
                    String tempLineString2 = tempLineString.trim();
                    if (isRecordStarted) {
                        targetStringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                        isRecordStarted = false;
                    } else {
                        targetStringBuilder.append("\n");
                        targetStringBuilder.append(tempLineString2);
                    }
                }
                outputStream = new FileOutputStream(targetFile);
                byte[] outputStrings = targetStringBuilder.toString().getBytes("utf-8");
                outputStream.write(outputStrings, 0, outputStrings.length);
            } catch (IOException e) {
                deleteAbnormalXml(targetFile);
                Log.e(HwPackageManagerServiceEx.TAG, "parseConfigsToTargetFile IOException");
            } catch (RuntimeException e2) {
                deleteAbnormalXml(targetFile);
                Log.e(HwPackageManagerServiceEx.TAG, "parseConfigsToTargetFile RuntimeException ");
            } catch (Throwable th) {
                closeBufferedReader(null);
                closeInputStreamReader(null);
                closeFileOutputStream(null);
                throw th;
            }
            closeBufferedReader(reader);
            closeInputStreamReader(inputStreamReader);
            closeFileOutputStream(outputStream);
        }

        private void deleteAbnormalXml(File file) {
            if (file.exists() && !file.delete()) {
                Log.e(HwPackageManagerServiceEx.TAG, "delete abnormal xml error!");
            }
        }

        private void closeBufferedReader(BufferedReader bufferedReader) {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.e(HwPackageManagerServiceEx.TAG, "closeBufferedReader error!");
                }
            }
        }

        private void closeInputStreamReader(InputStreamReader inputStreamReader) {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    Log.e(HwPackageManagerServiceEx.TAG, "closeInputStreamReader error!");
                }
            }
        }

        private void closeInputStream(InputStream inputStream) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(HwPackageManagerServiceEx.TAG, "closeInputStream error!");
                }
            }
        }

        private void closeFileOutputStream(FileOutputStream fileOutputStream) {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    Log.e(HwPackageManagerServiceEx.TAG, "closeFileOutputStream error!");
                }
            }
        }
    }

    public void restoreHwLauncherMode(int[] allUserHandles) {
        Slog.i(TAG, "restore hw launcher mode");
        if (allUserHandles != null) {
            List<ComponentName> componentList = getHwLauncherHomeActivities();
            if (componentList.size() != 0) {
                for (int userId : allUserHandles) {
                    for (ComponentName component : componentList) {
                        this.mPms.setComponentEnabledSetting(component, 2, 0, userId);
                    }
                    String hwLauncherMode = getHwLauncherMode(userId);
                    Slog.i(TAG, "hwLauncherMode: " + hwLauncherMode + ", userId: " + userId);
                    this.mPms.setComponentEnabledSetting(new ComponentName("com.huawei.android.launcher", hwLauncherMode), 1, 0, userId);
                }
            }
        }
    }

    public void revokePermissionsFromApp(String pkgName, List<String> permissionList) {
        Slog.d(TAG, "revokePermissionsFromApp " + pkgName + " from uid " + Binder.getCallingUid());
        if (pkgName == null || permissionList == null || permissionList.size() == 0) {
            Slog.e(TAG, "pkgName is null or permissionList is null");
            return;
        }
        PackageManagerService packageManagerService = this.mPms;
        if (packageManagerService == null || packageManagerService.mPermissionCallback == null) {
            Slog.e(TAG, "mPermissionCallback is null");
        } else if (this.mIPmsInner.getSettings() != null && this.mIPmsInner.getSettings().mPackages != null) {
            PackageSetting ps = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(pkgName);
            if (ps == null) {
                Slog.e(TAG, "ps is null");
                return;
            }
            PermissionsState permissionsState = ps.getPermissionsState();
            PermissionManagerServiceInternal permissionManager = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
            for (String permName : permissionList) {
                if (permissionsState.hasInstallPermission(permName)) {
                    BasePermission bp = permissionManager.getPermissionTEMP(permName);
                    if (bp == null) {
                        Slog.i(TAG, "Unknown permission " + permName + " in package " + pkgName);
                    } else {
                        permissionsState.revokeInstallPermission(bp);
                    }
                }
            }
            this.mPms.mPermissionCallback.onInstallPermissionRevoked();
        }
    }

    private List<ComponentName> getHwLauncherHomeActivities() {
        List<ComponentName> componentList = new ArrayList<>();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setPackage("com.huawei.android.launcher");
        List<ResolveInfo> resolveInfoList = this.mPms.queryIntentActivitiesInternal(intent, (String) null, 786432, 0);
        if (!(resolveInfoList == null || resolveInfoList.size() == 0)) {
            for (ResolveInfo resolveInfo : resolveInfoList) {
                componentList.add(new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
            }
        }
        return componentList;
    }

    private String getHwLauncherMode(int userId) {
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), GestureNavConst.SIMPLE_MODE_DB_KEY, 0, userId) == 1) {
            return "com.huawei.android.launcher.newsimpleui.NewSimpleLauncher";
        }
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "Simple mode", 0, userId) == 1) {
            return "com.huawei.android.launcher.simpleui.SimpleUILauncher";
        }
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "launcher_record", 0, userId) == 4) {
            return "com.huawei.android.launcher.drawer.DrawerLauncher";
        }
        return "com.huawei.android.launcher.unihome.UniHomeLauncher";
    }

    public ComponentName getMdmDefaultLauncher(List<ResolveInfo> resolveInfos) {
        ComponentName componentName = null;
        Bundle bundle = new HwDevicePolicyManagerEx().getCachedPolicyForFwk((ComponentName) null, HwAdminCache.POLICY_DEFAULTE_LAUNCHER, (Bundle) null);
        if (bundle != null) {
            componentName = ComponentName.unflattenFromString(bundle.getString("value"));
            Slog.i(TAG, "componentName = " + componentName);
        }
        if (componentName == null) {
            return null;
        }
        String packageName = componentName.getPackageName();
        Slog.i(TAG, "packageName = " + packageName);
        int resolveInfosSize = resolveInfos.size();
        int i = 0;
        while (i < resolveInfosSize) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            if (resolveInfo.activityInfo == null || !TextUtils.equals(resolveInfo.activityInfo.packageName, packageName)) {
                i++;
            } else {
                Slog.i(TAG, "return mdm default launcher componentname");
                return componentName;
            }
        }
        return null;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0044, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0045, code lost:
        $closeResource(r5, r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0048, code lost:
        throw r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x004b, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x004c, code lost:
        $closeResource(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x004f, code lost:
        throw r5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0052, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0053, code lost:
        $closeResource(r3, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0056, code lost:
        throw r4;
     */
    public ArrayList<String> getDelPackageList() {
        ArrayList<String> delPackageNameList = new ArrayList<>();
        File apkDeleteFile = new File(APK_DELETE_FILE);
        if (!apkDeleteFile.exists()) {
            return delPackageNameList;
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(apkDeleteFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
            BufferedReader reader = new BufferedReader(inputStreamReader);
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    $closeResource(null, reader);
                    $closeResource(null, inputStreamReader);
                    $closeResource(null, fileInputStream);
                    break;
                }
                delPackageNameList.add(line.trim());
            }
        } catch (IOException e) {
            Log.e(TAG, "PackageManagerService.getDelPackageList error for IO");
        }
        return delPackageNameList;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    public void putPreferredActivityInPcMode(int userId, IntentFilter filter, PreferredActivity preferredActivity) {
        HwResolverManager.getInstance().putPreferredActivityInPcMode(userId, filter, preferredActivity);
    }

    public boolean isFindPreferredActivityInCache(Intent intent, String resolvedType, int userId) {
        if (intent == null) {
            Slog.d(TAG, "isFindPreferredActivityInCache: intent is null.");
            return false;
        } else if (!HwResolverManager.getInstance().isMultiScreenCollaborationEnabled(this.mContext, null) || !checkResolvedType(intent, resolvedType)) {
            return false;
        } else {
            boolean isSuccessOpen = HwResolverManager.getInstance().getOpenFileResult(intent) == 0;
            boolean isOnlyOnce = HwResolverManager.getInstance().isOnlyOncePreferredActivity(intent, resolvedType, userId);
            if (isSuccessOpen || isOnlyOnce) {
                return true;
            }
            return false;
        }
    }

    private boolean checkResolvedType(Intent intent, String resolvedType) {
        Uri data = intent.getData();
        if (resolvedType != null) {
            return true;
        }
        if (resolvedType != null || data == null) {
            return false;
        }
        if ("http".equals(data.getScheme()) || "https".equals(data.getScheme())) {
            return true;
        }
        return false;
    }

    public ResolveInfo findPreferredActivityInCache(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int userId) {
        if (intent == null) {
            Slog.d(TAG, "findPreferredActivityInCache: intent is null.");
            return null;
        }
        String data = intent.getDataString();
        if (!"image/*".equals(resolvedType) || data == null || !data.startsWith("content://media/external_primary/")) {
            HwResolverManager.getInstance().preChooseBestActivity(intent, query, resolvedType, userId);
            return HwResolverManager.getInstance().findPreferredActivityInCache(this.mIPmsInner, intent, resolvedType, flags, query, userId);
        }
        Slog.d(TAG, "findPreferredActivityInCache: Record the screen or a touch pass.");
        return hwFindPreferredActivity(intent, query);
    }

    public boolean isMultiScreenCollaborationEnabled(Intent intent) {
        return HwResolverManager.getInstance().isMultiScreenCollaborationEnabled(this.mContext, intent);
    }

    public void filterResolveInfo(Intent intent, String resolvedType, List<ResolveInfo> resolveInfoList) {
        HwResolverManager.getInstance().filterResolveInfo(this.mContext, intent, UserHandle.getUserId(Binder.getCallingUid()), resolvedType, resolveInfoList);
    }

    public void setVersionMatchFlag(int deviceType, int version, boolean isMatchSuccess) {
        this.mContext.enforceCallingPermission(HW_PMS_SET_PCASSISTANT_RESULT, "Permission Denide for setVersionMatchFlag.");
        HwResolverManager.getInstance().setVersionMatchFlag(deviceType, version, isMatchSuccess);
    }

    public boolean getVersionMatchFlag(int deviceType, int version) {
        this.mContext.enforceCallingOrSelfPermission(HW_PMS_GET_PCASSISTANT_RESULT, "Permission Denide for getVersionMatchFlag.");
        return HwResolverManager.getInstance().getVersionMatchFlag(deviceType, version);
    }

    public void setOpenFileResult(Intent intent, int retCode) {
        this.mContext.enforceCallingPermission(HW_PMS_SET_PCASSISTANT_RESULT, "Permission Denide for setOpenFileResult.");
        HwResolverManager.getInstance().setOpenFileResult(intent, retCode);
    }

    public int getOpenFileResult(Intent intent) {
        this.mContext.enforceCallingOrSelfPermission(HW_PMS_GET_PCASSISTANT_RESULT, "Permission Denide for getOpenFileResult.");
        return HwResolverManager.getInstance().getOpenFileResult(intent);
    }

    private int checkSinglePermissionGrant(BasePermission basePermission, String[] signingDetails, int protectionLevel) {
        if (basePermission.isNormal()) {
            return 1;
        }
        if (basePermission.isRuntime()) {
            return protectionLevel == 1 ? 0 : 2;
        }
        if (!basePermission.isSignature()) {
            return 2;
        }
        Signature[] tmpSignings = basePermission.getSourcePackageSetting().getSignatures();
        List<String> localHashes = new ArrayList<>();
        for (Signature signature : tmpSignings) {
            localHashes.add(Base64.encodeToString(PackageUtils.computeSha256DigestBytes(signature.toByteArray()), 0).trim());
        }
        if (signingDetails.length != tmpSignings.length) {
            return 2;
        }
        for (String signature2 : signingDetails) {
            if (!localHashes.contains(signature2)) {
                return 2;
            }
        }
        return 1;
    }

    private Bundle canGrantDPermission(String packageName, String[] permissionNames, String[] signingDetails, int[] protectionLevels) {
        Bundle resultBundle = new Bundle();
        resultBundle.putString("package", packageName);
        int[] resultPermissions = new int[permissionNames.length];
        for (int i = 0; i < permissionNames.length; i++) {
            BasePermission basePermission = this.mPms.mPermissionManager.getPermissionTEMP(permissionNames[i]);
            if (basePermission == null) {
                resultPermissions[i] = 2;
                Slog.i(TAG, "canGrantDPermission, deny packageName " + packageName);
            } else {
                resultPermissions[i] = checkSinglePermissionGrant(basePermission, signingDetails, protectionLevels[i]);
            }
        }
        resultBundle.putIntArray("results", resultPermissions);
        return resultBundle;
    }

    private boolean isCanGrantDPermissionsCaller() {
        if (Binder.getCallingUid() == 1000) {
            return true;
        }
        return false;
    }

    public Bundle[] canGrantDPermissions(Bundle[] bundles) {
        Bundle[] invalidBundles = new Bundle[0];
        if (!isCanGrantDPermissionsCaller()) {
            Slog.e(TAG, "invalid invoke");
            return invalidBundles;
        } else if (bundles == null || bundles.length == 0 || bundles.length > 64) {
            Slog.e(TAG, "bundle invalid");
            return invalidBundles;
        } else {
            Bundle[] resultBundles = new Bundle[bundles.length];
            int i = 0;
            while (i < bundles.length) {
                if (bundles[i] == null) {
                    Slog.e(TAG, "bundle[ " + i + " ] is null");
                    return invalidBundles;
                }
                String packageName = bundles[i].getString("package", "");
                String[] permissionNames = bundles[i].getStringArray("permission");
                String[] signingDigests = bundles[i].getStringArray("sign");
                int[] protectionLevels = bundles[i].getIntArray("protectionLevel");
                if (TextUtils.isEmpty(packageName) || permissionNames == null || signingDigests == null || protectionLevels == null) {
                    Slog.e(TAG, "packageName:" + packageName + " [ " + i + " ] exist null element");
                    return invalidBundles;
                } else if (permissionNames.length == 0 || permissionNames.length > 1024) {
                    Slog.e(TAG, "length of permissionNames is " + permissionNames.length);
                    return invalidBundles;
                } else if (signingDigests.length == 0 || signingDigests.length > 1024) {
                    Slog.e(TAG, "length of signingDigests is " + signingDigests.length);
                    return invalidBundles;
                } else if (protectionLevels.length == 0 || protectionLevels.length != permissionNames.length) {
                    Slog.e(TAG, "length of permissionNames is " + permissionNames.length + " protectionLevels.length " + protectionLevels.length);
                    return invalidBundles;
                } else {
                    resultBundles[i] = canGrantDPermission(packageName, permissionNames, signingDigests, protectionLevels);
                    i++;
                }
            }
            return resultBundles;
        }
    }

    public Optional<HwRenamedPackagePolicy> generateRenamedPackagePolicyLocked(PackageParser.Package pkg) {
        Optional<HwRenamedPackagePolicy> renamedPackagePolicyOptional = HwRenamedPackagePolicyManager.generateRenamedPackagePolicyLocked(pkg, this.mIPmsInner);
        if (!renamedPackagePolicyOptional.isPresent()) {
            return Optional.empty();
        }
        HwRenamedPackagePolicy renamedPackagePolicy = renamedPackagePolicyOptional.get();
        if (HwRenamedPackagePolicyManager.getInstance().addRenamedPackagePolicy(renamedPackagePolicy)) {
            return Optional.of(renamedPackagePolicy);
        }
        return Optional.empty();
    }

    public Map<String, String> getHwRenamedPackages(int flags) {
        List<HwRenamedPackagePolicy> renamedPackagePolicyList = HwRenamedPackagePolicyManager.getInstance().getRenamedPackagePolicy(flags);
        Map<String, String> renamedPackages = new HashMap<>();
        if (renamedPackagePolicyList == null || renamedPackagePolicyList.isEmpty()) {
            return renamedPackages;
        }
        for (HwRenamedPackagePolicy renamedPackagePolicy : renamedPackagePolicyList) {
            renamedPackages.put(renamedPackagePolicy.getOriginalPackageName(), renamedPackagePolicy.getNewPackageName());
        }
        return renamedPackages;
    }

    public Optional<HwRenamedPackagePolicy> getRenamedPackagePolicyByOriginalName(String originalPackageName) {
        return HwRenamedPackagePolicyManager.getInstance().getRenamedPackagePolicyByOriginalName(originalPackageName);
    }

    @GuardedBy({"PackagesLock"})
    public boolean migrateDataForRenamedPackageLocked(PackageParser.Package pkg, int userId, int flags) {
        return HwRenamedPackagePolicyManager.getInstance().migrateDataForRenamedPackageLocked(pkg, userId, flags, this.mIPmsInner);
    }

    public boolean isOldPackageNameCanNotInstall(String packageName) {
        Map<String, String> renamedPackages = new HashMap<>();
        renamedPackages.putAll(new HashMap<>(getHwRenamedPackages(4)));
        renamedPackages.putAll(new HashMap<>(HwUninstalledAppManager.getInstance(this, this).getUninstalledRenamedPackagesMaps()));
        if (renamedPackages.size() != 0 && renamedPackages.containsKey(packageName)) {
            return true;
        }
        return false;
    }

    public boolean migrateAppUninstalledState(String packageName) {
        if (packageName == null || !isNeedMigrateUninstallStatus(packageName)) {
            return false;
        }
        String oldPkgName = HwRenamedPackagePolicyManager.getInstance().getRenamedPackagePolicyByNewPackageName(packageName).get().getOriginalPackageName();
        PackageSetting oldPkgSetting = (PackageSetting) this.mIPmsInner.getSettings().mPackages.get(oldPkgName);
        if (oldPkgSetting == null) {
            this.mPms.deletePackageVersioned(new VersionedPackage(packageName, -1), new PackageManager.LegacyPackageDeleteObserver((IPackageDeleteObserver) null).getBinder(), 0, 2);
            Slog.i(TAG, "migrate uninstalled state, newPkgName: " + packageName + ", oldPkgName: " + oldPkgName + " for all users");
            HwUninstalledAppManager.getInstance(this, this).removeFromUninstalledDelapp(oldPkgName);
            return true;
        }
        for (UserInfo user : PackageManagerService.sUserManager.getUsers(false)) {
            if (!oldPkgSetting.getInstalled(user.id)) {
                Slog.i(TAG, "migrate uninstalled state, newPkgName: " + packageName + ", oldPkgName: " + oldPkgName + ", userid: " + user.id);
                this.mPms.setSystemAppInstallState(packageName, false, user.id);
            }
        }
        HwUninstalledAppManager.getInstance(this, this).removeFromUninstalledDelapp(oldPkgName);
        return true;
    }

    private boolean isNeedMigrateUninstallStatus(String packageName) {
        Optional<HwRenamedPackagePolicy> policyOptional = HwRenamedPackagePolicyManager.getInstance().getRenamedPackagePolicyByNewPackageName(packageName);
        if (!policyOptional.isPresent()) {
            return false;
        }
        if (HwUninstalledAppManager.getInstance(this, this).getUninstalledMap().get(policyOptional.get().getOriginalPackageName()) != null) {
            Slog.i(TAG, "original package name app is uninstalled, need migrate the uninstall state for " + packageName);
            return true;
        }
        Slog.i(TAG, "original package name app is installed, don't need migrate the uninstall state for " + packageName);
        return false;
    }

    public List<ApplicationInfo> getClusterApplications(int flags, int clusterMask, boolean isOnlyDisabled, int userId) {
        if (DEBUG) {
            Slog.d(TAG, "getClusterApplications flags:" + flags + ",clusterMask:" + clusterMask + ",isOnlyDisabled:" + isOnlyDisabled + ",userId:" + userId);
        }
        boolean isQueryPlugin = false;
        boolean isQueryBundle = (clusterMask & 1) != 0;
        if ((clusterMask & 2) != 0) {
            isQueryPlugin = true;
        }
        if (isQueryBundle || isQueryPlugin) {
            synchronized (this.mIPmsInner.getPackagesLock()) {
                ParceledListSlice<ApplicationInfo> applicationInfos = getAllApplicationList(flags, isOnlyDisabled, userId);
                if (applicationInfos == null) {
                    return Collections.emptyList();
                }
                List<ApplicationInfo> resultList = new ArrayList<>();
                for (ApplicationInfo applicationInfo : applicationInfos.getList()) {
                    if (!isFilterQueryClusterApplication(applicationInfo, isQueryBundle, isQueryPlugin)) {
                        resultList.add(applicationInfo);
                    }
                }
                if (DEBUG) {
                    for (ApplicationInfo app : resultList) {
                        Slog.i(TAG, "getClusterApplications result:" + app.packageName + ",splitSize:" + app.splitNames.length);
                    }
                }
                return new ArrayList(resultList);
            }
        }
        Slog.e(TAG, "Invalid clusterMask!");
        return Collections.emptyList();
    }

    @GuardedBy({"mPackages"})
    private ParceledListSlice<ApplicationInfo> getAllApplicationList(int flags, boolean isOnlyDisabled, int userId) {
        if (!isOnlyDisabled) {
            return this.mIPmsInner.getInstalledApplications(flags, userId);
        }
        List<ApplicationInfo> appInfos = new ArrayList<>();
        for (PackageSetting ps : this.mIPmsInner.getSettings().getDisabledSysPackages().values()) {
            PackageParser.Package pkg = ps.pkg;
            if (pkg != null) {
                ApplicationInfo app = PackageParser.generateApplicationInfo(pkg, flags, ps.readUserState(userId), userId);
                if (HwPackageManagerUtils.isDynamicApplication(app)) {
                    app.packageName = pkg.staticSharedLibName != null ? pkg.manifestPackageName : pkg.packageName;
                    appInfos.add(app);
                }
            }
        }
        return new ParceledListSlice<>(appInfos);
    }

    private boolean isFilterQueryClusterApplication(ApplicationInfo app, boolean isQueryBundle, boolean isQueryPlugin) {
        boolean isFilter = true;
        if ((isQueryBundle && HwPackageManagerUtils.isBundleApplication(app)) || (isQueryPlugin && HwPackageManagerUtils.isSplitApplication(app))) {
            isFilter = false;
        }
        if (DEBUG) {
            Slog.d(TAG, "getClusterApplications " + app.packageName + " isFilter:" + isFilter);
        }
        return isFilter;
    }

    public int installHepApp(File stageDir) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        if (stageDir == null) {
            return -1;
        }
        if (this.mHwHepApplicationManager == null) {
            this.mHwHepApplicationManager = new HwHepApplicationManager(this.mContext);
        }
        return this.mHwHepApplicationManager.installHepInStageDir(stageDir);
    }

    public List<HwHepPackageInfo> getInstalledHep(int flags) {
        if (this.mHwHepApplicationManager == null) {
            this.mHwHepApplicationManager = new HwHepApplicationManager(this.mContext);
        }
        return this.mHwHepApplicationManager.getInstalledHep(flags);
    }

    public int uninstallHep(String packageName, int flags) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        if (packageName == null) {
            return -1;
        }
        if (this.mHwHepApplicationManager == null) {
            this.mHwHepApplicationManager = new HwHepApplicationManager(this.mContext);
        }
        return this.mHwHepApplicationManager.uninstallHep(packageName, flags);
    }

    public String obtainMapleClassPathByPkg(PackageParser.Package pkg) {
        if (pkg == null || pkg.mAppMetaData == null) {
            Slog.i(TAG, "[DCP] -> fail to obtain maple class path  due to null pointer of pkg");
            return null;
        }
        String mapleClassPath = concatMapleClassPath(pkg);
        if (mapleClassPath == null) {
            Slog.i(TAG, "[DCP] -> no maple class path found");
        }
        return mapleClassPath;
    }

    public void callGenMplCacheAtPmsInstaller(String baseCodePath, int sharedGid, int level, String mapleClassPath) {
        try {
            this.mIPmsInner.getInstallerInner().generateMplCache(baseCodePath, sharedGid, level, mapleClassPath);
        } catch (Installer.InstallerException e) {
            Slog.e(TAG, "[DCP] -> fail to generate maple cache for " + baseCodePath);
        }
    }

    public boolean isMygoteEnabled() {
        return IS_DEVICE_MAPLE_ENABLED && !SystemProperties.get("persist.mygote.disable", "0").equals("1");
    }

    private void collectMapleClassPath(List<String> resultList, List<String> inputList) {
        if (resultList != null && inputList != null) {
            for (String tmp : inputList) {
                if (!resultList.contains(tmp)) {
                    resultList.add(tmp);
                }
            }
        }
    }

    private String concatMapleClassPath(PackageParser.Package pkg) {
        if (pkg == null || pkg.baseCodePath == null) {
            return null;
        }
        StringBuilder retVal = new StringBuilder();
        List<String> resultList = new ArrayList<>();
        collectMapleClassPath(resultList, ZygoteInit.getNonBootClasspathList());
        collectMapleClassPath(resultList, pkg.usesLibraries);
        collectMapleClassPath(resultList, pkg.usesOptionalLibraries);
        Iterator<String> it = resultList.iterator();
        while (it.hasNext()) {
            retVal.append("/system/lib64/libmaple" + it.next() + ".so:");
        }
        retVal.append(pkg.baseCodePath + "!/maple/arm64/mapleclasses.so");
        return retVal.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0040  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0043 A[SYNTHETIC] */
    public List<PackageParser.Package> obtainMaplePkgsToGenCache() {
        boolean isMaplized;
        List<PackageParser.Package> pkgs = new ArrayList<>();
        synchronized (this.mIPmsInner.getPackagesLock()) {
            for (PackageParser.Package pkg : this.mIPmsInner.getPackagesLock().values()) {
                if ((pkg.applicationInfo.hwFlags & 16777216) == 0) {
                    if ((pkg.applicationInfo.hwFlags & 8388608) == 0) {
                        isMaplized = false;
                        if (!isMaplized) {
                            pkgs.add(pkg);
                        }
                    }
                }
                isMaplized = true;
                if (!isMaplized) {
                }
            }
        }
        return pkgs;
    }

    public void clearMplCacheLIF(PackageParser.Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        PackageSetting ps = this.mIPmsInner.getPackageSettingByPackageName(pkg.packageName);
        int[] resolvedUserIds = userId == -1 ? PackageManagerService.sUserManager.getUserIds() : new int[]{userId};
        for (int realUserId : resolvedUserIds) {
            long ceDataInode = ps != null ? ps.getCeDataInode(realUserId) : 0;
            try {
                Slog.i(TAG, "[DCP] clearMplCacheLIF: pkg = " + pkg);
                try {
                    this.mIPmsInner.getInstallerInner().clearMplCache(pkg.volumeUuid, pkg.packageName, realUserId, flags, ceDataInode);
                } catch (Installer.InstallerException e) {
                    e = e;
                }
            } catch (Installer.InstallerException e2) {
                e = e2;
                Slog.w(TAG, String.valueOf(e));
            }
        }
    }

    public boolean isMplPackage(PackageParser.Package pkg) {
        return (pkg == null || ((pkg.applicationInfo.hwFlags & 16777216) == 0 && (pkg.applicationInfo.hwFlags & 8388608) == 0)) ? false : true;
    }

    public int getCacheLevelForMapleApp(PackageParser.Package pkg) {
        int cacheLevel = -1;
        if (pkg == null || pkg.mAppMetaData == null) {
            Slog.i(TAG, "[DCP] -> fail to get cache level for  maple cache due to null pointer");
            return -1;
        }
        String cacheLevelStr = pkg.mAppMetaData.getString("com.huawei.maple.pre.generatecache");
        if (cacheLevelStr == null) {
            Slog.i(TAG, "[DCP] -> failed, cacheLevelStr is null");
            return -1;
        }
        String cacheLevelStr2 = cacheLevelStr.trim();
        if ("true".equals(cacheLevelStr2) || "3".equals(cacheLevelStr2)) {
            cacheLevel = 3;
        }
        if ("1".equals(cacheLevelStr2)) {
            cacheLevel = 1;
        }
        if ("7".equals(cacheLevelStr2)) {
            cacheLevel = 7;
        }
        if (cacheLevel == -1) {
            Slog.i(TAG, "[DCP] -> invalid value of cacheLevel");
        }
        return cacheLevel;
    }

    public boolean isNeedForbidShellFunc(String packageName) {
        return ForbidShellFuncUtil.isNeedForbidShellFunc(packageName);
    }

    public int getDisplayChangeAppRestartConfig(int type, String pkgName) {
        IHwPackageParser hwPackageParser;
        if (pkgName == null || (hwPackageParser = HwFrameworkFactory.getHwPackageParser()) == null) {
            return -1;
        }
        return hwPackageParser.getDisplayChangeAppRestartConfig(type, pkgName);
    }

    private boolean isEnableGupdateSwitch() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "gunstall_show_switch", 0) != 0;
    }

    private boolean isNoGMSScheme() {
        return TextUtils.isEmpty(SystemProperties.get("ro.com.google.gmsversion", ""));
    }

    private int getGunstallUserState() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "gunstall_user_state", 0);
    }

    private boolean getGunstallCloudUpdateState() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "gunstall_update_state", 0) != 0;
    }

    private int getGunstallCloudAppVersion() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "gunstall_app_version", Integer.MAX_VALUE);
    }

    public boolean forbidGMSUpgrade(PackageParser.Package pkg, PackageParser.Package oldPackage, int callingSessionUid) {
        if (!isEnableGupdateSwitch() || isNoGMSScheme() || pkg == null || oldPackage == null) {
            return false;
        }
        String updatingPackageName = pkg.packageName;
        if (TextUtils.isEmpty(updatingPackageName)) {
            return false;
        }
        String[] callerUidPacakge = this.mIPmsInner.getPackagesForUid(callingSessionUid);
        if (!(callerUidPacakge == null || callerUidPacakge[0] == null)) {
            Slog.i(TAG, "forbidGMSUpgrade: callerUidPacakge=" + callerUidPacakge[0] + ", updatingPackageName=" + updatingPackageName);
        }
        if (updatingPackageName.equals("com.google.android.gms") && callerUidPacakge != null && callerUidPacakge[0] != null && (callerUidPacakge[0].equals(PLAY_STORE_NAME) || ArrayUtils.contains(callerUidPacakge, PLAY_STORE_NAME))) {
            int oldVersionCode = oldPackage.mVersionCode;
            int updatingVersionCode = pkg.mVersionCode;
            Slog.i(TAG, "forbidGMSUpgrade: updatingVersionCode=" + updatingVersionCode + ",oldVersionCode=" + oldVersionCode);
            if (getGunstallUserState() == 0) {
                if (!getGunstallCloudUpdateState() || updatingVersionCode < getGunstallCloudAppVersion()) {
                    Slog.i(TAG, "cloud set allow, cloud update state:" + getGunstallCloudUpdateState() + ", cloud app version:" + getGunstallCloudAppVersion());
                } else {
                    Slog.i(TAG, "cloud set forbid");
                    return true;
                }
            } else if (getGunstallUserState() == 2) {
                Slog.i(TAG, "user set forbid");
                return true;
            } else {
                Slog.i(TAG, "user set allow");
            }
        }
        return false;
    }
}
