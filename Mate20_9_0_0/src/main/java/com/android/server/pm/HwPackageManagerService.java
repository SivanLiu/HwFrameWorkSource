package com.android.server.pm;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothAddressNative;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.PackageParser.SigningDetails;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
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
import android.os.IBackupSessionCallback.Stub;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Flog;
import android.util.HwSlog;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.util.jar.StrictJarFile;
import android.view.WindowManagerGlobal;
import android.widget.Toast;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.NativeLibraryHelper.Handle;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.Preconditions;
import com.android.internal.util.UserIcons;
import com.android.internal.util.XmlUtils;
import com.android.server.LocalServices;
import com.android.server.LocationManagerServiceUtil;
import com.android.server.UiThread;
import com.android.server.am.HwActivityManagerService;
import com.android.server.cota.CotaInstallImpl;
import com.android.server.cota.CotaInstallImpl.CotaInstallCallBack;
import com.android.server.cota.CotaService;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.notch.HwNotchScreenWhiteConfig;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import com.android.server.pm.PackageManagerService.InstallArgs;
import com.android.server.pm.PackageManagerService.PackageInstalledInfo;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.power.PowerManagerService;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.android.hwutil.CommandLineUtil;
import com.huawei.cust.HwCustUtils;
import com.huawei.hsm.permission.StubController;
import com.huawei.permission.IHoldService;
import dalvik.system.VMRuntime;
import huawei.android.hwutil.ZipUtil;
import huawei.cust.HwCfgFilePolicy;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import libcore.io.IoUtils;
import libcore.io.Streams;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsDetailModeID;

public class HwPackageManagerService extends PackageManagerService {
    private static final String ACTION_GET_INSTALLER_PACKAGE_INFO = "com.huawei.android.action.GET_INSTALLER_PACKAGE_INFO";
    private static final String ACTION_GET_PACKAGE_INSTALLATION_INFO = "com.huawei.android.action.GET_PACKAGE_INSTALLATION_INFO";
    private static final boolean ANTIMAL_DEBUG_ON = (SystemProperties.getInt(PROPERTY_ANTIMAL_DEBUG, 0) == 1);
    private static final String ANTIMAL_MODULE = "antiMalware";
    private static final String APK_INSTALLFILE = "xml/APKInstallListEMUI5Release.txt";
    private static final String BROADCAST_PERMISSION = "com.android.permission.system_manager_interface";
    private static final String CERT_TYPE_MEDIA = "media";
    private static final String CERT_TYPE_PLATFORM = "platform";
    private static final String CERT_TYPE_SHARED = "shared";
    private static final String CERT_TYPE_TESTKEY = "testkey";
    private static final String CLONE_APP_LIST = "hw_clone_app_list.xml";
    private static final String COTA_APK_XML_PATH = "/data/cota/live_update/work/xml/APKInstallListEMUI5Release.txt";
    private static final int COTA_APP_INSTALLING = -1;
    private static final int COTA_APP_INSTALL_FIAL = 0;
    private static final int COTA_APP_INSTALL_ILLEGAL = -3;
    private static final int COTA_APP_INSTALL_INIT = -2;
    private static final int COTA_APP_INSTALL_SUCCESS = 1;
    private static final String COTA_APP_UPDATE_APPWIDGET = "huawei.intent.action.UPDATE_COTA_APP_WIDGET";
    private static final String COTA_APP_UPDATE_APPWIDGET_EXTRA = "huawei.intent.extra.cota_package_list";
    private static final String COTA_DEL_APK_XML_PATH = "/data/cota/live_update/work/xml/DelAPKInstallListEMUI5Release.txt";
    private static final String CUST_APP_DIR = "cust/app";
    private static final String CUST_DIR = "/data/hw_init/";
    private static final String CUST_SYS_APP_DIR = "system/app";
    private static final String DATA_DATA_DIR = "/data/data/";
    private static final boolean DEBUG = DEBUG_FLAG;
    private static final boolean DEBUG_DATA_CUST;
    private static final boolean DEBUG_DEXOPT_OPTIMIZE;
    private static final boolean DEBUG_DEXOPT_SHELL;
    private static final boolean DEBUG_FLAG = SystemProperties.get("ro.dbg.pms_log", "0").equals(PreciseIgnore.COMP_SCREEN_ON_VALUE_);
    private static final String DEFAULT_APPMARKET = SystemProperties.get("ro.config.default_appmarket", "");
    private static final int DEFAULT_PACKAGE_ABI = -1000;
    private static final String DELAPK_INSTALLFILE = "xml/DelAPKInstallListEMUI5Release.txt";
    private static final int DEL_MULTI_INSTALL_MAP_SIZE = 3;
    private static final String DESCRIPTOR = "huawei.com.android.server.IPackageManager";
    private static final String DEXOPT_IN_BOOTUP_APKLIST = "/system/etc/dexopt/dexopt_in_bootup_apklist.cfg";
    private static final String FILE_MULTIWINDOW_WHITELIST = "multiwindow_whitelist_apps.xml";
    private static final String FILE_POLICY_CLASS_NAME = "com.huawei.cust.HwCfgFilePolicy";
    private static final String FLAG_APK_NOSYS = "nosys";
    private static final String FLAG_APK_PRIV = "priv";
    private static final String FLAG_APK_SYS = "sys";
    private static final String GMS_CORE_PATH = "/data/hw_init/system/app/GmsCore";
    private static final String GMS_FWK_PATH = "/data/hw_init/system/app/GoogleServicesFramework";
    private static final String GMS_LOG_PATH = "/data/hw_init/system/app/GoogleLoginService";
    private static final String GOOGLESETUP_PKG = "com.google.android.setupwizard";
    private static final long HOTA_DEXOPT_THRESHOLD = 18000000;
    private static final String HSM_PACKAGE = "com.huawei.systemmanager";
    private static final String HWSETUP_PKG = "com.huawei.hwstartupguide";
    private static final String HWT_PATH_MAGAZINE;
    private static final String INSERT_RESULT = "result";
    private static final String INSTALLATION_EXTRA_INSTALLER_PACKAGE_NAME = "installerPackageName";
    private static final String INSTALLATION_EXTRA_PACKAGE_INSTALLER_PID = "pkgInstallerPid";
    private static final String INSTALLATION_EXTRA_PACKAGE_INSTALLER_UID = "pkgInstallerUid";
    private static final String INSTALLATION_EXTRA_PACKAGE_INSTALL_RESULT = "pkgInstallResult";
    private static final String INSTALLATION_EXTRA_PACKAGE_META_HASH = "pkgMetaHash";
    private static final String INSTALLATION_EXTRA_PACKAGE_NAME = "pkgName";
    private static final String INSTALLATION_EXTRA_PACKAGE_UPDATE = "pkgUpdate";
    private static final String INSTALLATION_EXTRA_PACKAGE_URI = "pkgUri";
    private static final String INSTALLATION_EXTRA_PACKAGE_VERSION_CODE = "pkgVersionCode";
    private static final String INSTALLATION_EXTRA_PACKAGE_VERSION_NAME = "pkgVersionName";
    private static final int INSTALLER_ADB = 1;
    private static final int INSTALLER_OTHERS = 0;
    private static final String INSTALL_BEGIN = "begin";
    private static final String INSTALL_END = "end";
    private static final String[] INSTALL_SAFEMODE_LIST = new String[]{"jp.co.omronsoft.iwnnime.ml"};
    private static final int LAST_DONE_VERSION = 10000;
    private static final String[] LIMITED_PACKAGE_NAMES = new String[]{"com.huawei.android.totemweather", MemoryConstant.CAMERA_PACKAGE_NAME, "com.android.calendar", "com.android.soundrecorder"};
    private static final String[] LIMITED_TARGET_PACKAGE_NAMES = new String[]{"com.google.android.wearable.app.cn", "com.google.android.wearable.app"};
    private static final boolean MAGAZINE_COPYRIGHT_ENABLE = SystemProperties.getBoolean("ro.config.hw_mg_copyright", true);
    private static int MAX_PKG = 100;
    private static final int MAX_THEME_SIZE = 100000000;
    private static final String META_KEY_KEEP_ALIVE = "android.server.pm.KEEP_ALIVE";
    private static final String METHOD_NAME_FOR_FILE = "getCfgFile";
    private static final int MULTI_INSTALL_MAP_SIZE = 2;
    private static final String NEVER_DEXOPT_APKLIST = "/system/etc/dexopt/never_dexopt_apklist.cfg";
    private static final int OPTIMIZE_FOR_BOOTING = 2;
    private static final int OPTIMIZE_FOR_OTA = 1;
    private static final int OPTIMIZE_FOR_OTHER = 4;
    private static final String PACKAGE_NAME = "pkg";
    private static final String PERINSTALL_FILE_LIST = "preinstalled_files_list.txt";
    private static final String PROPERTY_ANTIMAL_DEBUG = "persist.sys.antimal.debug";
    private static final long QUERY_RECENTLY_USED_THRESHOLD = 604800000;
    private static final ArrayList<String> SCAN_INSTALL_CALLER_PACKAGES = new ArrayList(Arrays.asList(new String[]{GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, "com.huawei.hiassistant", "com.huawei.search"}));
    private static final String SOURCE_PACKAGE_NAME = "src";
    private static final String SYSTEM_APP_DIR = "/system/app";
    private static final String SYSTEM_DEBUGGABLE = "ro.debuggable";
    private static final String SYSTEM_FRAMEWORK_DIR = "/system/framework/";
    private static final String SYSTEM_SECURE = "ro.secure";
    private static final String TAG = "HwPackageManagerService";
    private static final String TAG_DATA_CUST = "HwPMS_DataCust";
    public static final int TRANSACTION_CODE_CHECK_GMS_IS_UNINSTALLED = 1008;
    public static final int TRANSACTION_CODE_DELTE_GMS_FROM_UNINSTALLED_DELAPP = 1009;
    public static final int TRANSACTION_CODE_FILE_BACKUP_EXECUTE_TASK = 1019;
    public static final int TRANSACTION_CODE_FILE_BACKUP_FINISH_SESSION = 1020;
    public static final int TRANSACTION_CODE_FILE_BACKUP_START_SESSION = 1018;
    public static final int TRANSACTION_CODE_GET_APP_TYPE = 1023;
    public static final int TRANSACTION_CODE_GET_HDB_KEY = 1011;
    public static final int TRANSACTION_CODE_GET_IM_AND_VIDEO_APP_LIST = 1022;
    public static final int TRANSACTION_CODE_GET_MAX_ASPECT_RATIO = 1013;
    public static final int TRANSACTION_CODE_GET_PREINSTALLED_APK_LIST = 1007;
    public static final int TRANSACTION_CODE_GET_PUBLICITY_DESCRIPTOR = 1015;
    public static final int TRANSACTION_CODE_GET_PUBLICITY_INFO_LIST = 1014;
    public static final int TRANSACTION_CODE_GET_SCAN_INSTALL_LIST = 1017;
    public static final int TRANSACTION_CODE_IS_NOTIFICATION_SPLIT = 1021;
    public static final int TRANSACTION_CODE_SCAN_INSTALL_APK = 1016;
    public static final int TRANSACTION_CODE_SET_HDB_KEY = 1010;
    public static final int TRANSACTION_CODE_SET_MAX_ASPECT_RATIO = 1012;
    private static final String XML_ATTRIBUTE_PACKAGE_NAME = "package_name";
    private static final String XML_ELEMENT_APP_FORCED_PORTRAIT_ITEM = "mw_app_forced_portrait";
    private static final String XML_ELEMENT_APP_ITEM = "mw_app";
    private static final String XML_ELEMENT_APP_LIST = "multiwindow_whitelist";
    private static final String XML_ONE_SPLIT_SCREEN_IMS_ITEM = "mw_app_instant_msg";
    private static final String XML_ONE_SPLIT_SCREEN_VIDEO_ITEM = "mw_app_video";
    private static HashMap<String, HashSet<String>> mCotaDelInstallMap = null;
    private static HashMap<String, HashSet<String>> mCotaInstallMap = null;
    private static HwCustPackageManagerService mCustPackageManagerService = ((HwCustPackageManagerService) HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]));
    private static List<String> mCustStoppedApps = new ArrayList();
    static Map<String, HashSet<String>> mDefaultSystemList = null;
    private static HashMap<String, HashSet<String>> mDelMultiInstallMap = null;
    static final ArrayList<String> mDexoptInBootupApps = new ArrayList();
    static final ArrayList<String> mForceNotDexApps = new ArrayList();
    private static HwPackageManagerService mHwPackageManagerService = null;
    private static HashSet<String> mInstallSet = new HashSet();
    private static HashMap<String, HashSet<String>> mMultiInstallMap = null;
    static List<String> mOldDataBackup = new ArrayList();
    private static final int mOptimizeForDexopt = SystemProperties.getInt("ro.config.DexOptForBooting", 7);
    private static ArrayMap<String, AbiInfo> mPackagesAbi = null;
    private static File mPakcageAbiFilename = null;
    static final StringBuilder mReadMessages = new StringBuilder();
    private static ArrayList<String> mRemoveablePreInstallApks = new ArrayList();
    private static File mSystemDir = null;
    private static final int mThreadNum = (Runtime.getRuntime().availableProcessors() + 1);
    private static String mUninstallApk = null;
    static final ArrayList<String> mUninstalledDelappList = new ArrayList();
    static final Map<String, String> mUninstalledMap = new HashMap();
    private static List<String> oneSplitScreenImsListPkgNames = new ArrayList();
    private static List<String> oneSplitScreenVideoListPkgNames = new ArrayList();
    private static List<String> preinstalledPackageList = new ArrayList();
    private static List<String> sMWPortraitWhiteListPkgNames = new ArrayList();
    private static List<String> sMultiWinWhiteListPkgNames = new ArrayList();
    private static final Set<String> sSupportCloneApps = new HashSet();
    public static final int transaction_pmCheckGranted = 1005;
    public static final int transaction_pmCreateThemeFolder = 1003;
    public static final int transaction_pmGetResourcePackageName = 1004;
    public static final int transaction_pmInstallHwTheme = 1002;
    public static final int transaction_sendLimitedPackageBroadcast = 1006;
    public static final int transaction_setEnabledVisitorSetting = 1001;
    private boolean isBlackListExist = false;
    private BlackListInfo mBlackListInfo = new BlackListInfo();
    private HandlerThread mCommonHandlerThread = null;
    private CertCompatSettings mCompatSettings;
    private ComponentChangeMonitor mComponentChangeMonitor = null;
    private int mCotaApksInstallStatus = -2;
    private CotaInstallCallBack mCotaInstallCallBack = new CotaInstallCallBack() {
        public void startInstall() {
            Log.i(HwPackageManagerService.TAG, "startInstallCotaApks()");
            HwPackageManagerService.this.startInstallCotaApks();
        }

        public int getStatus() {
            Log.i(HwPackageManagerService.TAG, "getStatus()");
            return HwPackageManagerService.this.getCotaStatus();
        }
    };
    private Object mCust = null;
    private HwCustHwPackageManagerService mCustHwPms = ((HwCustHwPackageManagerService) HwCustUtils.createObj(HwCustHwPackageManagerService.class, new Object[0]));
    private BlackListInfo mDisableAppListInfo = new BlackListInfo();
    private HashMap<String, BlackListApp> mDisableAppMap = new HashMap();
    private boolean mFoundCertCompatFile;
    Package mGoogleServicePackage;
    private HashSet<String> mGrantedInstalledPkg = new HashSet();
    private boolean mHaveLoadedDexoptInBootUpApkList = false;
    private boolean mHaveLoadedNeverDexoptApkList = false;
    private HwFileBackupManager mHwFileBackupManager = null;
    private Set<String> mIncompatNotificationList = new ArraySet();
    private Set<String> mIncompatiblePkg = new ArraySet();
    private final Installer mInstaller;
    ArrayList<UsageStats> mRecentlyUsedApps = new ArrayList();
    private ArrayList<String> mScanInstallApkList = new ArrayList();
    ArrayList<Package> mSortDexoptApps = new ArrayList();
    private boolean needCollectAppInfo = true;
    private Map<String, String> pkgMetaHash = new HashMap();
    final Comparator<UsageStats> totalTimeInForegroundComparator = new Comparator<UsageStats>() {
        public int compare(UsageStats usageStats1, UsageStats usageStats2) {
            long usageStats1Time = usageStats1.getTotalTimeInForeground();
            long usageStats2Time = usageStats2.getTotalTimeInForeground();
            if (usageStats1Time > usageStats2Time) {
                return -1;
            }
            if (usageStats1Time < usageStats2Time) {
                return 1;
            }
            return 0;
        }
    };

    public static class AbiInfo {
        int abiCode;
        String name;
        int version;

        AbiInfo(String name, int abiCode, int version) {
            this.name = name;
            this.abiCode = abiCode;
            this.version = version;
        }

        int getAbiCode() {
            return this.abiCode;
        }

        String getName() {
            return this.name;
        }

        int getVersion() {
            return this.version;
        }

        void setVersion(int value) {
            this.version = value;
        }
    }

    static {
        boolean z = false;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG_DEXOPT_OPTIMIZE = z2;
        z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG_DEXOPT_SHELL = z2;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory());
        stringBuilder.append("/magazine");
        HWT_PATH_MAGAZINE = stringBuilder.toString();
        if (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4))) {
            z = true;
        }
        DEBUG_DATA_CUST = z;
    }

    public static synchronized PackageManagerService getInstance(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        HwPackageManagerService hwPackageManagerService;
        synchronized (HwPackageManagerService.class) {
            if (mHwPackageManagerService == null) {
                initCustStoppedApps();
                loadMultiWinWhiteList(context);
                createPackagesAbiFile();
                initCloneAppsFromCust();
                try {
                    mHwPackageManagerService = new HwPackageManagerService(context, installer, factoryTest, onlyCore);
                    deleteNonSupportedAppsForClone(mHwPackageManagerService);
                } catch (Exception e) {
                    Slog.e(TAG, "Error while package manager initializing! For:", e);
                }
            }
            if (mHwPackageManagerService == null) {
                String str;
                StringBuilder stringBuilder;
                File packagesXml = new File(new File(Environment.getDataDirectory(), "system"), "packages.xml");
                if (packagesXml.exists()) {
                    boolean result = packagesXml.delete();
                    Settings.setPackageSettingsError();
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("something may be missed in packages.xml, delete the file :");
                    stringBuilder.append(result);
                    Slog.e(str, stringBuilder.toString());
                }
                try {
                    PowerManagerService.lowLevelReboot("PackageManagerService is null, try to reboot...");
                } catch (Exception re) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("try to reboot error, exception:");
                    stringBuilder.append(re);
                    Slog.e(str, stringBuilder.toString());
                }
            }
            hwPackageManagerService = mHwPackageManagerService;
        }
        return hwPackageManagerService;
    }

    public HwPackageManagerService(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        super(context, installer, factoryTest, onlyCore);
        this.mInstaller = installer;
        try {
            initBlackList();
        } catch (Exception e) {
            Slog.e(TAG, "initBlackList failed");
        }
        this.mHwFileBackupManager = HwFileBackupManager.getInstance(installer);
        recordUninstalledDelapp(null, null);
        mOldDataBackup.clear();
        this.mCommonHandlerThread = new HandlerThread(TAG);
        this.mCommonHandlerThread.start();
        this.mComponentChangeMonitor = new ComponentChangeMonitor(context, this.mCommonHandlerThread.getLooper());
    }

    private void setEnabledVisitorSetting(int newState, int flags, String callingPackage, int userId) {
        String str;
        ArrayList<String> arrayList;
        Throwable th;
        ArrayList<String> components;
        int i = newState;
        int i2 = userId;
        if (i == 0 || i == 1 || i == 2 || i == 3 || i == 4) {
            String callingPackage2;
            int packageUid = -1;
            if (callingPackage == null) {
                callingPackage2 = Integer.toString(Binder.getCallingUid());
            } else {
                callingPackage2 = callingPackage;
            }
            ArrayList<String> components2 = null;
            HashMap<String, ArrayList<String>> componentsMap = new HashMap();
            HashMap<String, Integer> pkgMap = new HashMap();
            String pkgNameList = Secure.getString(this.mContext.getContentResolver(), "privacy_app_list");
            if (pkgNameList == null) {
                Slog.e(TAG, " pkgNameList = null ");
            } else if (pkgNameList.equals("")) {
                Slog.e(TAG, " pkgNameList is null");
            } else {
                PackageSetting pkgSetting;
                int packageUid2;
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    pkgSetting = null;
                    stringBuilder.append(" pkgNameList =   ");
                    stringBuilder.append(pkgNameList);
                    Slog.e(str2, stringBuilder.toString());
                } else {
                    pkgSetting = null;
                }
                String[] pkgNameArray = pkgNameList.contains(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER) ? pkgNameList.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER) : new String[]{pkgNameList};
                boolean sendNow = false;
                String packageName = null;
                int i3 = 0;
                while (true) {
                    int i4 = i3;
                    if (i4 >= MAX_PKG || pkgNameArray == null || i4 >= pkgNameArray.length) {
                        str = callingPackage2;
                        arrayList = components2;
                        this.mSettings.writePackageRestrictionsLPr(i2);
                        i3 = 0;
                    } else {
                        packageName = pkgNameArray[i4];
                        String componentName = packageName;
                        packageUid2 = packageUid;
                        synchronized (this.mPackages) {
                            String str3;
                            String str4;
                            try {
                                String callingPackage3;
                                str = callingPackage2;
                                PackageSetting pkgSetting2 = (PackageSetting) this.mSettings.mPackages.get(packageName);
                                if (pkgSetting2 == null) {
                                    arrayList = components2;
                                    try {
                                        pkgMap.put(packageName, Integer.valueOf(1));
                                    } catch (Throwable th2) {
                                        th = th2;
                                        str3 = packageName;
                                        str4 = componentName;
                                        pkgSetting = pkgSetting2;
                                        pkgSetting2 = str;
                                        components2 = arrayList;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        }
                                        throw th;
                                    }
                                }
                                arrayList = components2;
                                try {
                                    if (pkgSetting2.getEnabled(i2) == i) {
                                        pkgMap.put(packageName, Integer.valueOf(1));
                                    } else {
                                        callingPackage3 = (i == 0 || i == 1) ? null : str;
                                        try {
                                            pkgSetting2.setEnabled(i, i2, callingPackage3);
                                            pkgMap.put(packageName, Integer.valueOf(0));
                                            ArrayList<String> components3 = this.mPendingBroadcasts.get(i2, packageName);
                                            boolean newPackage = components3 == null;
                                            if (newPackage) {
                                                try {
                                                    components3 = new ArrayList();
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    str3 = packageName;
                                                    str4 = componentName;
                                                    pkgSetting = pkgSetting2;
                                                    pkgSetting2 = callingPackage3;
                                                    components2 = components3;
                                                    while (true) {
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            }
                                            if (!components3.contains(componentName)) {
                                                components3.add(componentName);
                                            }
                                            componentsMap.put(packageName, components3);
                                            if ((flags & 1) == 0) {
                                                sendNow = true;
                                                this.mPendingBroadcasts.remove(i2, packageName);
                                                components = components3;
                                                str3 = packageName;
                                                str4 = componentName;
                                            } else {
                                                if (newPackage) {
                                                    this.mPendingBroadcasts.put(i2, packageName, components3);
                                                }
                                                try {
                                                    components = components3;
                                                    try {
                                                        if (this.mHandler.hasMessages(1)) {
                                                            str3 = packageName;
                                                            str4 = componentName;
                                                        } else {
                                                            str3 = packageName;
                                                            str4 = componentName;
                                                            try {
                                                                this.mHandler.sendEmptyMessageDelayed(1, 10000);
                                                            } catch (Throwable th5) {
                                                                th = th5;
                                                                pkgSetting = pkgSetting2;
                                                                pkgSetting2 = callingPackage3;
                                                                components2 = components;
                                                                while (true) {
                                                                    break;
                                                                }
                                                                throw th;
                                                            }
                                                        }
                                                    } catch (Throwable th6) {
                                                        th = th6;
                                                        str3 = packageName;
                                                        str4 = componentName;
                                                        pkgSetting = pkgSetting2;
                                                        pkgSetting2 = callingPackage3;
                                                        components2 = components;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw th;
                                                    }
                                                } catch (Throwable th7) {
                                                    th = th7;
                                                    str3 = packageName;
                                                    str4 = componentName;
                                                    pkgSetting = pkgSetting2;
                                                    pkgSetting2 = callingPackage3;
                                                    components2 = components3;
                                                    while (true) {
                                                        break;
                                                    }
                                                    throw th;
                                                }
                                            }
                                        } catch (Throwable th8) {
                                            th = th8;
                                            str3 = packageName;
                                            str4 = componentName;
                                            pkgSetting = pkgSetting2;
                                            pkgSetting2 = callingPackage3;
                                            while (true) {
                                                break;
                                            }
                                            throw th;
                                        }
                                    }
                                } catch (Throwable th9) {
                                    th = th9;
                                    str3 = packageName;
                                    str4 = componentName;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                                str3 = packageName;
                                str4 = componentName;
                                callingPackage3 = str;
                                components = arrayList;
                                i3 = i4 + 1;
                                pkgSetting = pkgSetting2;
                                callingPackage2 = callingPackage3;
                                packageUid = packageUid2;
                                components2 = components;
                                packageName = str3;
                                componentName = str4;
                                i = newState;
                            } catch (Throwable th10) {
                                th = th10;
                                str3 = packageName;
                                str4 = componentName;
                                str = callingPackage2;
                                arrayList = components2;
                                while (true) {
                                    break;
                                }
                                throw th;
                            }
                        }
                    }
                }
                str = callingPackage2;
                arrayList = components2;
                this.mSettings.writePackageRestrictionsLPr(i2);
                i3 = 0;
                while (i3 < MAX_PKG && pkgNameArray != null && i3 < pkgNameArray.length) {
                    packageName = pkgNameArray[i3];
                    if (((Integer) pkgMap.get(packageName)).intValue() != 1) {
                        PackageSetting pkgSetting3 = (PackageSetting) this.mSettings.mPackages.get(packageName);
                        if (pkgSetting3 != null && sendNow) {
                            packageUid = UserHandle.getUid(i2, pkgSetting3.appId);
                            sendPackageChangedBroadcast(packageName, (flags & 1) != 0, (ArrayList) componentsMap.get(packageName), packageUid);
                            pkgSetting = pkgSetting3;
                            packageUid2 = packageUid;
                        } else {
                            pkgSetting = pkgSetting3;
                        }
                    }
                    i3++;
                }
            }
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        boolean setwallpaper = false;
        FileDescriptor fileDescriptor = null;
        int userId;
        List<String> list;
        switch (code) {
            case 1001:
                Slog.w(TAG, "onTransact");
                data.enforceInterface(DESCRIPTOR);
                setEnabledVisitorSetting(data.readInt(), data.readInt(), null, data.readInt());
                reply.writeNoException();
                return true;
            case 1002:
                Slog.w(TAG, "onTransact-pmInstallHwTheme");
                data.enforceInterface(DESCRIPTOR);
                String themePath = data.readString();
                if (data.readInt() != 0) {
                    setwallpaper = true;
                }
                boolean result = pmInstallHwTheme(themePath, setwallpaper, data.readInt());
                reply.writeNoException();
                reply.writeInt(result);
                return true;
            case 1003:
                Slog.w(TAG, "onTransact-transaction_pmCreateThemeFolder");
                data.enforceInterface(DESCRIPTOR);
                userId = data.readInt();
                if (userId < 0) {
                    return false;
                }
                createThemeFolder(userId);
                reply.writeNoException();
                return true;
            case 1004:
                Slog.w(TAG, "onTransact-transaction_pmGetResourcePackageName");
                data.enforceInterface(DESCRIPTOR);
                String packageName = getResourcePackageNameByIcon(data.readString(), data.readInt(), data.readInt());
                reply.writeNoException();
                reply.writeString(packageName);
                return true;
            case 1005:
                data.enforceInterface(DESCRIPTOR);
                boolean granted = checkInstallGranted(data.readString());
                reply.writeNoException();
                reply.writeInt(granted);
                return true;
            case 1006:
                data.enforceInterface(DESCRIPTOR);
                String action = data.readString();
                String pkg = data.readString();
                if (data.readInt() != 0) {
                    fileDescriptor = (Bundle) Bundle.CREATOR.createFromParcel(data);
                }
                FileDescriptor extras = fileDescriptor;
                sendLimitedPackageBroadcast(action, pkg, extras, data.readString(), data.createIntArray());
                reply.writeNoException();
                return true;
            case 1007:
                data.enforceInterface(DESCRIPTOR);
                list = getPreinstalledApkList();
                reply.writeNoException();
                reply.writeStringList(list);
                return true;
            case 1008:
                data.enforceInterface(DESCRIPTOR);
                setwallpaper = checkGmsCoreUninstalled();
                reply.writeNoException();
                reply.writeInt(setwallpaper);
                return true;
            case 1009:
                data.enforceInterface(DESCRIPTOR);
                deleteGmsCoreFromUninstalledDelapp();
                reply.writeNoException();
                return true;
            case 1010:
                data.enforceInterface(DESCRIPTOR);
                setHdbKey(data.readString());
                reply.writeNoException();
                return true;
            case TRANSACTION_CODE_GET_HDB_KEY /*1011*/:
                data.enforceInterface(DESCRIPTOR);
                reply.writeNoException();
                reply.writeString(HwAdbManager.getHdbKey());
                return true;
            case 1012:
                data.enforceInterface(DESCRIPTOR);
                setwallpaper = setApplicationMaxAspectRatio(data.readString(), data.readFloat());
                reply.writeNoException();
                reply.writeInt(setwallpaper);
                return true;
            case 1013:
                data.enforceInterface(DESCRIPTOR);
                float result2 = getApplicationMaxAspectRatio(data.readString());
                reply.writeNoException();
                reply.writeFloat(result2);
                return true;
            case 1014:
                data.enforceInterface(DESCRIPTOR);
                list = getHwPublicityAppList();
                reply.writeNoException();
                reply.writeStringList(list);
                return true;
            case 1015:
                data.enforceInterface(DESCRIPTOR);
                ParcelFileDescriptor filedescriptor = getHwPublicityAppParcelFileDescriptor();
                reply.writeNoException();
                if (filedescriptor == null) {
                    reply.writeFileDescriptor(null);
                } else {
                    reply.writeFileDescriptor(filedescriptor.getFileDescriptor());
                    try {
                        filedescriptor.close();
                    } catch (IOException e) {
                        Slog.e(TAG, "filedescriptor close IOException");
                    }
                }
                return true;
            case 1016:
                data.enforceInterface(DESCRIPTOR);
                setwallpaper = scanInstallApk(data.readString());
                reply.writeNoException();
                reply.writeInt(setwallpaper);
                return true;
            case 1017:
                data.enforceInterface(DESCRIPTOR);
                list = getScanInstallList();
                reply.writeNoException();
                reply.writeStringList(list);
                return true;
            case 1018:
                data.enforceInterface(DESCRIPTOR);
                userId = startBackupSession(Stub.asInterface(data.readStrongBinder()));
                reply.writeNoException();
                reply.writeInt(userId);
                return true;
            case 1019:
                data.enforceInterface(DESCRIPTOR);
                int result3 = executeBackupTask(data.readInt(), data.readString());
                reply.writeNoException();
                reply.writeInt(result3);
                return true;
            case 1020:
                data.enforceInterface(DESCRIPTOR);
                userId = finishBackupSession(data.readInt());
                reply.writeNoException();
                reply.writeInt(userId);
                return true;
            case 1021:
                data.enforceInterface(DESCRIPTOR);
                setwallpaper = isNotificationAddSplitButton(data.readString());
                reply.writeNoException();
                reply.writeInt(setwallpaper);
                return true;
            case 1022:
                data.enforceInterface(DESCRIPTOR);
                list = getSupportSplitScreenApps();
                reply.writeNoException();
                reply.writeStringList(list);
                return true;
            case 1023:
                data.enforceInterface(DESCRIPTOR);
                int appType = getApplicationType(data.readString());
                reply.writeNoException();
                reply.writeInt(appType);
                return true;
            default:
                return super.onTransact(code, data, reply, flags);
        }
    }

    private String getResourcePackageNameByIcon(String pkgName, int icon, int userId) {
        String str;
        StringBuilder stringBuilder;
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            Log.w(TAG, "packageManager is null !");
            return null;
        }
        String packageName;
        try {
            packageName = pm.getResourcesForApplicationAsUser(pkgName, userId).getResourcePackageName(icon);
        } catch (NameNotFoundException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageName ");
            stringBuilder.append(pkgName);
            stringBuilder.append(": Resources not found !");
            Log.w(str, stringBuilder.toString());
            packageName = null;
        } catch (NotFoundException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("packageName ");
            stringBuilder.append(pkgName);
            stringBuilder.append(": ResourcesPackageName not found !");
            Log.w(str, stringBuilder.toString());
            packageName = null;
        } catch (RuntimeException e3) {
            Log.w(TAG, "RuntimeException in getResourcePackageNameByIcon !");
            packageName = null;
        } catch (Throwable th) {
            return null;
        }
        return packageName;
    }

    private boolean pmInstallHwTheme(String themePath, boolean setwallpaper, int userId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pmInstallHwTheme, themePath:");
        stringBuilder.append(themePath);
        stringBuilder.append(" ;setwallpaper:");
        stringBuilder.append(setwallpaper);
        stringBuilder.append(" ;user:");
        stringBuilder.append(userId);
        Log.w(str, stringBuilder.toString());
        StringBuilder stringBuilder2;
        if (TextUtils.isEmpty(themePath)) {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("themePath is null, themePath:");
            stringBuilder2.append(themePath);
            Log.w(str, stringBuilder2.toString());
            return false;
        }
        if (themePath.startsWith("/data/themes")) {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("install online/download theme, themePath:");
            stringBuilder2.append(themePath);
            Log.w(str, stringBuilder2.toString());
        } else if (themePath.startsWith("/data/hw_init")) {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("install local theme, themePath:");
            stringBuilder2.append(themePath);
            Log.w(str, stringBuilder2.toString());
        } else {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("other message, themePath:");
            stringBuilder2.append(themePath);
            Log.w(str, stringBuilder2.toString());
        }
        if (setwallpaper) {
            rmSysWallpaper();
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_MEDIA_STORAGE") == 0) {
            File themeFile = new File(themePath);
            if (!themeFile.exists()) {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("install theme failed, ");
                stringBuilder3.append(themePath);
                stringBuilder3.append(" not found");
                Log.w(str2, stringBuilder3.toString());
                try {
                    createThemeFolder(userId);
                    restoreThemeCon(userId);
                } catch (Exception e) {
                    Log.w(TAG, "create theme folder failed, ", e);
                    deleteThemeTempFolder();
                }
                return false;
            } else if (((int) themeFile.length()) > MAX_THEME_SIZE || ZipUtil.isZipError(themePath)) {
                return false;
            } else {
                try {
                    createThemeFolder(userId);
                    if (userId == 0 && isDataSkinExists()) {
                        renameDataSkinFolder(userId);
                    } else {
                        createThemeTempFolder();
                        unzipThemePackage(themeFile);
                        unzipCustThemePackage(getCustThemePath(themePath));
                        renameThemeTempFolder(userId);
                        renameKeyguardFile(userId);
                    }
                    restoreThemeCon(userId);
                    deleteInstallFlag();
                    if (this.mCustHwPms != null && this.mCustHwPms.isSupportThemeRestore()) {
                        this.mCustHwPms.changeTheme(themePath, this.mContext);
                    }
                    return true;
                } catch (Exception e2) {
                    Log.w(TAG, "install theme failed, ", e2);
                    deleteThemeTempFolder();
                    return false;
                }
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Permission Denial: can't install theme from pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(" without permission ");
        stringBuilder.append("android.permission.WRITE_MEDIA_STORAGE");
        throw new SecurityException(stringBuilder.toString());
    }

    private void unzipCustThemePackage(File custThemeFile) {
        if (custThemeFile != null && custThemeFile.exists()) {
            unzipThemePackage(custThemeFile);
        }
    }

    private File getCustThemePath(String path) {
        File custDiffFile = null;
        if (path == null) {
            return null;
        }
        String[] paths = path.split("/");
        String themeName = paths[paths.length - 1];
        String diffThemePath = SystemProperties.get("ro.config.diff_themes");
        if (TextUtils.isEmpty(diffThemePath)) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("themes/diff/");
                stringBuilder.append(themeName);
                custDiffFile = HwCfgFilePolicy.getCfgFile(stringBuilder.toString(), 0);
            } catch (NoClassDefFoundError e) {
                Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            }
            return custDiffFile;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(diffThemePath);
        stringBuilder2.append("/");
        stringBuilder2.append(themeName);
        return new File(stringBuilder2.toString());
    }

    private String getHwThemePathAsUser(int userId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HwThemeManager.HWT_PATH_THEME);
        stringBuilder.append("/");
        stringBuilder.append(userId);
        return stringBuilder.toString();
    }

    private boolean isDataSkinExists() {
        File file = new File(HwThemeManager.HWT_PATH_SKIN);
        if (!file.exists()) {
            return false;
        }
        try {
            return file.getCanonicalPath().equals(new File(file.getParentFile().getCanonicalFile(), file.getName()).getPath());
        } catch (IOException e) {
            return false;
        }
    }

    private void renameDataSkinFolder(int userId) {
        CommandLineUtil.rm("system", getHwThemePathAsUser(userId));
        CommandLineUtil.mv("system", HwThemeManager.HWT_PATH_SKIN, getHwThemePathAsUser(userId));
        CommandLineUtil.chmod("system", "0775", getHwThemePathAsUser(userId));
    }

    private void createFolder(String dir) {
        CommandLineUtil.mkdir("system", dir);
        CommandLineUtil.chmod("system", "0775", dir);
    }

    private void restoreThemeCon(int userId) {
        File themePath = new File(getHwThemePathAsUser(userId));
        if (themePath.exists() && !SELinux.restoreconRecursive(themePath)) {
            Log.w(TAG, "restoreconRecursive HWT_PATH_SKIN failed!");
        }
    }

    private void createThemeFolder(int userId) {
        createFolder(HwThemeManager.HWT_PATH_SKIN_INSTALL_FLAG);
        createFolder(HwThemeManager.HWT_PATH_THEME);
        createFolder(getHwThemePathAsUser(userId));
    }

    private void createMagazineFolder() {
        String dir = HWT_PATH_MAGAZINE;
        if (new File(dir).exists()) {
            Slog.i(TAG, " Magazine Folder already exist return");
            return;
        }
        Slog.i(TAG, "createMagazineFolder Magazine Folder ing");
        CommandLineUtil.mkdir("system", dir);
        CommandLineUtil.chown("system", "system", "media_rw", dir);
        FileUtils.setPermissions(dir, 504, -1, -1);
        boolean restoreconResult = SELinux.restorecon(dir);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createMagazineFolder Magazine Folder restoreconResult ");
        stringBuilder.append(restoreconResult);
        Slog.i(str, stringBuilder.toString());
    }

    private void createThemeTempFolder() {
        CommandLineUtil.rm("system", "/data/skin.tmp");
        CommandLineUtil.mkdir("system", "/data/skin.tmp");
        CommandLineUtil.chmod("system", "0775", "/data/skin.tmp");
    }

    private void deleteThemeTempFolder() {
        CommandLineUtil.rm("system", "/data/skin.tmp");
        deleteInstallFlag();
    }

    private void deleteInstallFlag() {
        if (new File(HwThemeManager.HWT_PATH_SKIN_INSTALL_FLAG).exists()) {
            CommandLineUtil.rm("system", HwThemeManager.HWT_PATH_SKIN_INSTALL_FLAG);
        }
    }

    private void renameThemeTempFolder(int userId) {
        CommandLineUtil.rm("system", getHwThemePathAsUser(userId));
        CommandLineUtil.mv("system", "/data/skin.tmp", getHwThemePathAsUser(userId));
    }

    private void unzipThemePackage(File themeFile) {
        ZipUtil.unZipFile(themeFile, "/data/skin.tmp");
        CommandLineUtil.chmod("system", "0775", "/data/skin.tmp");
    }

    private void renameKeyguardFile(int userId) {
        if (!new File(getHwThemePathAsUser(userId), "com.android.keyguard").exists() && new File(getHwThemePathAsUser(userId), "com.huawei.android.hwlockscreen").exists()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getHwThemePathAsUser(userId));
            stringBuilder.append("/");
            stringBuilder.append("com.huawei.android.hwlockscreen");
            String stringBuilder2 = stringBuilder.toString();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(getHwThemePathAsUser(userId));
            stringBuilder3.append("/");
            stringBuilder3.append("com.android.keyguard");
            CommandLineUtil.mv("system", stringBuilder2, stringBuilder3.toString());
        }
    }

    private boolean rmSysWallpaper() {
        if (new File("/data/system/users/0/", "wallpaper").exists()) {
            CommandLineUtil.rm("system", "/data/system/users/0/wallpaper");
            if (this.mCustHwPms != null && this.mCustHwPms.isSupportThemeRestore()) {
                CommandLineUtil.rm("system", "/data/system/users/0/wallpaper_orig");
            }
        }
        return true;
    }

    private static boolean firstScan() {
        boolean exists = new File(Environment.getDataDirectory(), "system/packages.xml").exists();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("is first scan?");
        stringBuilder.append(exists ^ 1);
        Slog.i(str, stringBuilder.toString());
        return exists ^ 1;
    }

    protected static void initCustStoppedApps() {
        if (firstScan()) {
            Slog.i(TAG, "first boot. init cust stopped apps.");
            File file = null;
            File file2;
            try {
                file = HwCfgFilePolicy.getCfgFile("xml/not_start_firstboot.xml", 0);
                if (file == null) {
                    file2 = new File("/data/cust/", "xml/not_start_firstboot.xml");
                    file = file2;
                }
            } catch (NoClassDefFoundError e) {
                Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
                if (null == null) {
                    file2 = new File("/data/cust/", "xml/not_start_firstboot.xml");
                }
            } catch (Throwable th) {
                if (null == null) {
                    file = new File("/data/cust/", "xml/not_start_firstboot.xml");
                }
            }
            parseCustStoppedApps(file);
            return;
        }
        Slog.i(TAG, "not first boot. don't init cust stopped apps.");
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
                            break;
                        } catch (IOException e) {
                            Slog.w(TAG, "Got execption when close black_package_name.xml!", e);
                        }
                    } else if ("package".equals(parser.getName()) && "name".equals(parser.getAttributeName(0))) {
                        String value = parser.getAttributeValue(0);
                        if (value == null || "".equals(value)) {
                            mCustStoppedApps.clear();
                            Slog.e(TAG, "not_start_firstboot.xml bad format.");
                        } else {
                            mCustStoppedApps.add(value);
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("cust stopped apps:");
                            stringBuilder.append(value);
                            Slog.i(str, stringBuilder.toString());
                        }
                    }
                }
                xmlReader.close();
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
            String str;
            StringBuilder stringBuilder;
            if (block) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("blocked broadcast send to system app:");
                stringBuilder.append(pkg);
                stringBuilder.append(", stopped?");
                stringBuilder.append(stopped);
                Slog.i(str, stringBuilder.toString());
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("a system app is customized not to start at first boot. app:");
                stringBuilder.append(pkg);
                Slog.i(str, stringBuilder.toString());
            }
        }
        return contain;
    }

    protected ResolveInfo hwFindPreferredActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int priority, boolean always, boolean removeMatches, boolean debug, int userId) {
        int num;
        ResolveInfo info;
        int index;
        List<ResolveInfo> list = query;
        Intent intent2 = intent;
        if (intent2.hasCategory("android.intent.category.HOME") && list != null && list.size() > 1) {
            if (!SystemProperties.getBoolean("ro.config.pref.hw_launcher", true)) {
                num = list.size() - 1;
                List<String> whiteListLauncher = new ArrayList();
                whiteListLauncher.add(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE);
                whiteListLauncher.add("com.huawei.kidsmode");
                whiteListLauncher.add("com.android.settings");
                while (num >= 0) {
                    int num2 = num - 1;
                    info = (ResolveInfo) list.get(num);
                    if (info.activityInfo == null || whiteListLauncher.contains(info.activityInfo.applicationInfo.packageName)) {
                        num = num2;
                    } else {
                        HwSlog.v(TAG, "return default Launcher null");
                        return null;
                    }
                }
            }
            num = list.size() - 1;
            while (num >= 0) {
                index = num - 1;
                info = (ResolveInfo) list.get(num);
                String defaultLauncher = GestureNavConst.DEFAULT_LAUNCHER_PACKAGE;
                if (!(mCustPackageManagerService == null || info.activityInfo == null)) {
                    String custDefaultLauncher = mCustPackageManagerService.getCustDefaultLauncher(this.mContext, info.activityInfo.applicationInfo.packageName);
                    if (!TextUtils.isEmpty(custDefaultLauncher)) {
                        defaultLauncher = custDefaultLauncher;
                    }
                }
                if (info.activityInfo == null || !info.activityInfo.applicationInfo.packageName.equals(defaultLauncher)) {
                    num = index;
                } else {
                    HwSlog.v(TAG, "Returning system default Launcher ");
                    return info;
                }
            }
        }
        if (!(intent2.getAction() == null || !intent2.getAction().equals("android.intent.action.DIAL") || intent2.getData() == null || intent2.getData().getScheme() == null || !intent2.getData().getScheme().equals("tel") || list == null || list.size() <= 1)) {
            num = list.size() - 1;
            while (num >= 0) {
                index = num - 1;
                info = (ResolveInfo) list.get(num);
                if (info.priority >= 0 && info.activityInfo != null && info.activityInfo.applicationInfo.packageName.equals("com.android.contacts")) {
                    return info;
                }
                num = index;
            }
        }
        if (!(intent2.getAction() == null || !intent2.getAction().equals("android.intent.action.VIEW") || intent2.getData() == null || intent2.getData().getScheme() == null || ((!intent2.getData().getScheme().equals("file") && !intent2.getData().getScheme().equals("content")) || intent2.getType() == null || !intent2.getType().startsWith("image/") || list == null || list.size() <= 1))) {
            num = list.size() - 1;
            while (num >= 0) {
                index = num - 1;
                info = (ResolveInfo) list.get(num);
                if (info.activityInfo != null && info.activityInfo.applicationInfo.packageName.equals("com.android.gallery3d")) {
                    return info;
                }
                num = index;
            }
        }
        if (!(intent2.getAction() == null || !intent2.getAction().equals("android.intent.action.VIEW") || intent2.getData() == null || intent2.getData().getScheme() == null || ((!intent2.getData().getScheme().equals("file") && !intent2.getData().getScheme().equals("content")) || intent2.getType() == null || !intent2.getType().startsWith("audio/") || list == null || list.size() <= 1))) {
            num = list.size() - 1;
            while (num >= 0) {
                index = num - 1;
                info = (ResolveInfo) list.get(num);
                if (info.activityInfo != null && info.activityInfo.applicationInfo.packageName.equals("com.android.mediacenter")) {
                    return info;
                }
                num = index;
            }
        }
        if (intent2.getAction() != null && intent2.getAction().equals("android.media.action.IMAGE_CAPTURE") && list != null && list.size() > 1) {
            num = list.size() - 1;
            while (num >= 0) {
                index = num - 1;
                info = (ResolveInfo) list.get(num);
                if (info.activityInfo != null && info.activityInfo.applicationInfo.packageName.equals(MemoryConstant.CAMERA_PACKAGE_NAME)) {
                    return info;
                }
                num = index;
            }
        }
        if (!(intent2.getAction() == null || !intent2.getAction().equals("android.intent.action.VIEW") || intent2.getData() == null || intent2.getData().getScheme() == null || !intent2.getData().getScheme().equals("mailto") || list == null || list.size() <= 1)) {
            num = list.size() - 1;
            while (num >= 0) {
                index = num - 1;
                info = (ResolveInfo) list.get(num);
                if (info.activityInfo != null && info.activityInfo.applicationInfo.packageName.equals("com.android.email")) {
                    return info;
                }
                num = index;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("!mIsDefaultPreferredActivityChanged= ");
        stringBuilder.append(this.mIsDefaultPreferredActivityChanged ^ 1);
        stringBuilder.append(" ,mIsDefaultGoogleCalendar= ");
        stringBuilder.append(this.mIsDefaultGoogleCalendar);
        Log.i(str, stringBuilder.toString());
        if (!this.mIsDefaultPreferredActivityChanged && this.mIsDefaultGoogleCalendar && isCalendarType(intent2) && list != null && list.size() > 1) {
            num = list.size() - 1;
            while (num >= 0) {
                index = num - 1;
                info = (ResolveInfo) list.get(num);
                if (info.priority >= 0 && info.activityInfo != null && info.activityInfo.applicationInfo.packageName.equals("com.google.android.calendar")) {
                    return info;
                }
                num = index;
            }
        }
        if (intent2.getAction() != null && intent2.getAction().equals("android.intent.action.VIEW") && intent2.getData() != null && intent2.getData().toString().startsWith("market://details") && intent2.getData().getScheme() != null && intent2.getData().getScheme().equals("market") && list != null && list.size() > 1) {
            num = list.size() - 1;
            while (num >= 0) {
                int index2 = num - 1;
                info = (ResolveInfo) list.get(num);
                if (info.activityInfo == null || !info.activityInfo.applicationInfo.packageName.equals(DEFAULT_APPMARKET)) {
                    num = index2;
                } else {
                    String str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("app market : ");
                    stringBuilder.append(info);
                    Log.i(str2, stringBuilder.toString());
                    return info;
                }
            }
        }
        return null;
    }

    private boolean isCalendarType(Intent intent) {
        boolean z = false;
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "isCalendarType, intent or action is null, return false");
            return false;
        }
        String action = intent.getAction();
        Uri data = intent.getData();
        String type = intent.getType();
        boolean calendarType1 = ("android.intent.action.EDIT".equals(action) || "android.intent.action.INSERT".equals(action) || "android.intent.action.VIEW".equals(action)) && "vnd.android.cursor.item/event".equals(type);
        boolean calendarType2 = ("android.intent.action.EDIT".equals(action) || "android.intent.action.INSERT".equals(action)) && "vnd.android.cursor.dir/event".equals(type);
        boolean calendarType3 = "android.intent.action.VIEW".equals(action) && data != null && (("http".equals(data.getScheme()) || NetworkCheckerThread.TYPE_HTTPS.equals(data.getScheme())) && "www.google.com".equals(data.getHost()) && data.getPath() != null && (data.getPath().startsWith("/calendar/event") || (data.getPath().startsWith("/calendar/hosted") && data.getPath().endsWith("/event"))));
        boolean calendarType4 = "android.intent.action.VIEW".equals(action) && "text/calendar".equals(type);
        boolean calendarType5 = "android.intent.action.VIEW".equals(action) && "time/epoch".equals(type);
        boolean calendarType6 = "android.intent.action.VIEW".equals(action) && data != null && "content".equals(data.getScheme()) && "com.android.calendar".equals(data.getHost());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isCalendarType, calendarType1= ");
        stringBuilder.append(calendarType1);
        stringBuilder.append(" ,calendarType2= ");
        stringBuilder.append(calendarType2);
        stringBuilder.append(" ,calendarType3= ");
        stringBuilder.append(calendarType3);
        stringBuilder.append(" ,calendarType4= ");
        stringBuilder.append(calendarType4);
        stringBuilder.append(" ,calendarType5= ");
        stringBuilder.append(calendarType5);
        stringBuilder.append(" ,calendarType6= ");
        stringBuilder.append(calendarType6);
        Log.i(str, stringBuilder.toString());
        if (calendarType1 || calendarType2 || calendarType3 || calendarType4 || calendarType5 || calendarType6) {
            z = true;
        }
        return z;
    }

    public void scanCustDir(int scanMode) {
        File mCustAppDir = new File("/data/cust/", "app");
        if (mCustAppDir.exists()) {
            scanDirLI(mCustAppDir, 16, scanMode | 131072, 0);
        }
        HwCustPackageManagerService custObj = (HwCustPackageManagerService) HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]);
        if (custObj != null) {
            custObj.scanCustPrivDir(scanMode, this);
        }
    }

    public void custScanPrivDir(File dir, int parseFlags, int scanFlags, long currentTime, int hwFlags) {
        scanDirLI(dir, parseFlags, scanFlags, currentTime, hwFlags);
    }

    protected void addPreferredActivityInternal(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always, int userId, String opname) {
        super.addPreferredActivityInternal(filter, match, set, activity, always, userId, opname);
        if (filter.hasCategory("android.intent.category.HOME")) {
            if (Global.getInt(this.mContext.getContentResolver(), "temporary_home_mode", 0) == 1) {
                Slog.i(TAG, "Skip killing last non default home because the new default home is temporary");
                return;
            }
            doKillNondefaultHome(activity.getPackageName(), userId);
        }
    }

    private void doKillNondefaultHome(String defaultHome, int userId) {
        int i = userId;
        List<ResolveInfo> resolveInfos = queryIntentActivitiesInternal(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME").addCategory("android.intent.category.DEFAULT"), null, 128, i);
        int sz = resolveInfos.size();
        IActivityManager am = ActivityManagerNative.getDefault();
        boolean z = false;
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 < sz) {
                ResolveInfo info = (ResolveInfo) resolveInfos.get(i3);
                String homePkg = info.activityInfo.packageName;
                Bundle metaData = info.activityInfo.metaData;
                boolean isKeepAlive = false;
                if (metaData != null) {
                    isKeepAlive = metaData.getBoolean(META_KEY_KEEP_ALIVE, z);
                }
                boolean isKeepAlive2 = isKeepAlive;
                if (!homePkg.equals(defaultHome)) {
                    String str;
                    if (isKeepAlive2) {
                        str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Skip killing package : ");
                        stringBuilder.append(homePkg);
                        Slog.i(str, stringBuilder.toString());
                    } else if (2000 == Binder.getCallingUid() || getNameForUid(Binder.getCallingUid()) == null) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Skip killing package when calling from thread whose pgkname is null: ");
                        stringBuilder2.append(homePkg);
                        Slog.i(str, stringBuilder2.toString());
                    } else if (am != null) {
                        try {
                            am.forceStopPackage(homePkg, i);
                        } catch (RemoteException e) {
                            RemoteException remoteException = e;
                            String str2 = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Failed to kill home package of");
                            stringBuilder3.append(homePkg);
                            Slog.e(str2, stringBuilder3.toString());
                        } catch (SecurityException e2) {
                            SecurityException securityException = e2;
                            Slog.e(TAG, "Permission Denial, requires FORCE_STOP_PACKAGES");
                        }
                    }
                }
                i2 = i3 + 1;
                z = false;
            } else {
                String str3 = defaultHome;
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:37:?, code skipped:
            r2.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean parseCustPreInstallApks(File scanApk) {
        InputStreamReader inStreamReader;
        FileInputStream fileInStream;
        boolean result;
        if (scanApk == null) {
            Slog.i(TAG_DATA_CUST, "Invalid input arg (scanApk) null");
            return false;
        }
        String APK_DIR_TAG = "APK_DIR:";
        BufferedReader reader = null;
        inStreamReader = null;
        fileInStream = null;
        result = true;
        try {
            fileInStream = new FileInputStream(scanApk);
            inStreamReader = new InputStreamReader(fileInStream, "UTF-8");
            reader = new BufferedReader(inStreamReader);
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    line = line.trim();
                    if (DEBUG_DATA_CUST) {
                        readLine = TAG_DATA_CUST;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("line: ");
                        stringBuilder.append(line.trim());
                        Slog.i(readLine, stringBuilder.toString());
                    }
                    int startIndex = line.indexOf("APK_DIR:") + "APK_DIR:".length();
                    if (startIndex >= 0) {
                        line = line.substring(startIndex, line.indexOf(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER));
                        String pkgPath = line.substring(0, line.indexOf(","));
                        line = pkgPath;
                        if (line.endsWith(".apk")) {
                            pkgPath = line.substring(0, line.lastIndexOf("/"));
                        }
                        File pkg = new File(pkgPath);
                        String str;
                        StringBuilder stringBuilder2;
                        if (pkg.exists()) {
                            synchronized (mInstallSet) {
                                mInstallSet.add(pkg.getAbsolutePath());
                            }
                            if (DEBUG_DATA_CUST) {
                                str = TAG_DATA_CUST;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("mInstallSet.add: ");
                                stringBuilder2.append(pkg.getAbsolutePath());
                                Slog.i(str, stringBuilder2.toString());
                            }
                        } else {
                            str = TAG_DATA_CUST;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ignore (");
                            stringBuilder2.append(pkgPath);
                            stringBuilder2.append(") for not exist.");
                            Slog.w(str, stringBuilder2.toString());
                        }
                    }
                } else {
                    try {
                        break;
                    } catch (Exception e) {
                    }
                }
            }
            while (true) {
            }
        } catch (FileNotFoundException e2) {
            result = false;
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e3) {
                }
            }
            if (inStreamReader != null) {
                try {
                    inStreamReader.close();
                } catch (Exception e4) {
                }
            }
            if (fileInStream != null) {
                fileInStream.close();
            }
            return result;
        } catch (IOException e5) {
            result = false;
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e6) {
                }
            }
            if (inStreamReader != null) {
                try {
                    inStreamReader.close();
                } catch (Exception e7) {
                }
            }
            if (fileInStream != null) {
                fileInStream.close();
            }
            return result;
        } catch (Exception e8) {
            result = false;
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e9) {
                }
            }
            if (inStreamReader != null) {
                try {
                    inStreamReader.close();
                } catch (Exception e10) {
                }
            }
            if (fileInStream != null) {
                fileInStream.close();
            }
            return result;
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e11) {
                }
            }
            if (inStreamReader != null) {
                try {
                    inStreamReader.close();
                } catch (Exception e12) {
                }
            }
            if (fileInStream != null) {
                try {
                    fileInStream.close();
                } catch (Exception e13) {
                }
            }
        }
        try {
            fileInStream.close();
            break;
        } catch (Exception e14) {
            result = false;
        }
        return result;
        try {
            inStreamReader.close();
        } catch (Exception e15) {
        }
        fileInStream.close();
        return result;
    }

    public void scanHwCustAppDir(int scanMode) {
        if (!parseCustPreInstallApks(new File(CUST_DIR, PERINSTALL_FILE_LIST))) {
            Slog.e(TAG_DATA_CUST, "parse preinstalled_files_list failed. skip all the packages.");
        } else if (mInstallSet != null) {
            if (DEBUG_DATA_CUST) {
                Slog.i(TAG_DATA_CUST, "CUST APK BEGIN");
            }
            scanDirLI(new File(CUST_DIR, CUST_SYS_APP_DIR), 16, scanMode | 131072, 0, 167772160);
            scanDirLI(new File(CUST_DIR, CUST_APP_DIR), 16, scanMode | 131072, 0, 167772160);
            if (DEBUG_DATA_CUST) {
                Slog.i(TAG_DATA_CUST, "CUST APK END");
            }
        }
    }

    private boolean isDelappInCust(String scanFileString) {
        if (scanFileString == null) {
            Slog.w(TAG_DATA_CUST, "Invalid input arg (scanFileString) null");
            return false;
        }
        if (DEBUG_DATA_CUST) {
            String str = TAG_DATA_CUST;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isDelapp scanFile: ");
            stringBuilder.append(scanFileString);
            Slog.i(str, stringBuilder.toString());
        }
        if (mInstallSet == null || !mInstallSet.contains(scanFileString)) {
            return false;
        }
        return true;
    }

    public boolean isDelappInCust(PackageSetting ps) {
        if (ps == null || ps.codePath == null) {
            Slog.w(TAG_DATA_CUST, "Invalid input arg (ps) null");
            return false;
        }
        String codePath = ps.codePath.toString();
        if (mInstallSet != null && mInstallSet.contains(codePath)) {
            return true;
        }
        if (DEBUG_DATA_CUST) {
            String str = TAG_DATA_CUST;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("codePath: ");
            stringBuilder.append(codePath);
            Slog.i(str, stringBuilder.toString());
        }
        return false;
    }

    public boolean isCustApkRecorded(File file) {
        if (file == null) {
            Slog.w(TAG_DATA_CUST, "Invalid input arg (file) null");
            return false;
        }
        String str = TAG_DATA_CUST;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" isCustApkRecorded codePath: ");
        stringBuilder.append(file.getAbsolutePath());
        Slog.i(str, stringBuilder.toString());
        if (file.isDirectory() && mInstallSet != null && mInstallSet.contains(file.getAbsolutePath())) {
            return true;
        }
        return false;
    }

    public void scanRemovableAppDir(int scanMode) {
        File[] apps = getRemovableAppDirs();
        for (File app : apps) {
            if (app != null && app.exists()) {
                scanDirLI(app, 16, scanMode | 131072, 0, 33554432);
            }
        }
    }

    private File[] getRemovableAppDirs() {
        File mPreRemovableAppDir1 = new File("/data/cust/", "delapp");
        File mPreRemovableAppDir2 = new File("/system/", "delapp");
        return new File[]{mPreRemovableAppDir1, mPreRemovableAppDir2};
    }

    private void removeFromUninstalledDelapp(String s) {
        if (mUninstalledDelappList != null && mUninstalledDelappList.contains(s)) {
            mUninstalledDelappList.remove(s);
            mUninstalledMap.remove(s);
            recordUninstalledDelapp(null, null);
        }
    }

    public void recordUninstalledDelapp(String s, String path) {
        String str;
        StringBuilder stringBuilder;
        if (mUninstalledDelappList.contains(s)) {
            Slog.d(TAG, "duplicate recordUninstalledDelapp here, return!");
            return;
        }
        File file = new File("/data/system/", "uninstalled_delapp.xml");
        if (s != null) {
            loadUninstalledDelapp(file);
        }
        FileOutputStream stream = null;
        try {
            int i = 0;
            stream = new FileOutputStream(file, false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, "utf-8");
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, "values");
            if (s != null) {
                out.startTag(null, "string");
                out.attribute(null, "name", s);
                out.attribute(null, "codePath", path);
                out.endTag(null, "string");
            }
            int N = mUninstalledDelappList.size();
            while (i < N) {
                String temp = (String) mUninstalledDelappList.get(i);
                out.startTag(null, "string");
                out.attribute(null, "name", temp);
                out.attribute(null, "codePath", (String) mUninstalledMap.get(temp));
                out.endTag(null, "string");
                i++;
            }
            out.endTag(null, "values");
            out.endDocument();
            if (s != null) {
                mUninstalledDelappList.add(s);
                mUninstalledMap.put(s, path);
            }
            try {
                stream.close();
            } catch (IOException e) {
                Log.e(TAG, "recordUninstalledDelapp()");
            }
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e2);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Exception e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e3);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e4) {
                    Log.e(TAG, "recordUninstalledDelapp()");
                }
            }
        }
    }

    private void loadUninstalledDelapp(File file) {
        loadUninstalledDelapp(file, true);
    }

    /* JADX WARNING: Missing block: B:43:?, code skipped:
            r2.close();
     */
    /* JADX WARNING: Missing block: B:87:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void loadUninstalledDelapp(File file, boolean isIncludeCodePath) {
        String str;
        StringBuilder stringBuilder;
        Map<String, String> unistalledMap = new HashMap();
        unistalledMap.putAll(mUninstalledMap);
        mUninstalledDelappList.clear();
        mUninstalledMap.clear();
        FileInputStream stream = null;
        XmlPullParser parser;
        try {
            int type;
            String tag;
            stream = new FileInputStream(file);
            parser = Xml.newPullParser();
            parser.setInput(stream, null);
            while (true) {
                int next = parser.next();
                type = next;
                if (next == 1 || type == 2) {
                    tag = parser.getName();
                }
            }
            tag = parser.getName();
            if ("values".equals(tag)) {
                type = parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int next2 = parser.next();
                    type = next2;
                    if (next2 == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        try {
                            break;
                        } catch (IOException e) {
                            Log.e(TAG, "loadUninstalledDelapp()");
                            return;
                        }
                    } else if (type != 3) {
                        if (type != 4) {
                            if ("string".equals(parser.getName()) && parser.getAttributeValue(0) != null) {
                                if (isIncludeCodePath) {
                                    mUninstalledDelappList.add(parser.getAttributeValue(0));
                                    mUninstalledMap.put(parser.getAttributeValue(0), parser.getAttributeValue(1));
                                } else {
                                    mOldDataBackup.add(parser.getAttributeValue(0));
                                }
                            }
                        }
                    }
                }
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Settings do not start with policies tag: found ");
                stringBuilder2.append(tag);
                throw new XmlPullParserException(stringBuilder2.toString());
            }
        } catch (IndexOutOfBoundsException e2) {
            if (!this.mSystemReady) {
                throw e2;
            } else if (unistalledMap.get(parser.getAttributeValue(0)) != null) {
                mUninstalledMap.put(parser.getAttributeValue(0), (String) unistalledMap.get(parser.getAttributeValue(0)));
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("loadUninstalledDelapp pkg:");
                stringBuilder3.append(parser.getAttributeValue(0));
                stringBuilder3.append(" is remove!");
                Log.i(str2, stringBuilder3.toString());
                if (mUninstalledDelappList.contains(parser.getAttributeValue(0))) {
                    mUninstalledDelappList.remove(parser.getAttributeValue(0));
                }
            }
        } catch (FileNotFoundException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("file is not exist ");
            stringBuilder.append(e3);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (XmlPullParserException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e4);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e5) {
            try {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(file);
                stringBuilder.append(" ");
                stringBuilder.append(e5);
                Slog.w(str, stringBuilder.toString());
                if (stream != null) {
                    stream.close();
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e6) {
                        Log.e(TAG, "loadUninstalledDelapp()");
                    }
                }
            }
        }
    }

    private boolean isUninstalledDelapp(String s) {
        if (mOldDataBackup.size() != 0) {
            return mOldDataBackup.contains(s);
        }
        if (mUninstalledDelappList.size() != 0) {
            return mUninstalledDelappList.contains(s);
        }
        return false;
    }

    private boolean isApplicationInstalled(Package pkg) {
        int userId = UserHandle.getCallingUserId();
        PackageSetting p = (PackageSetting) this.mSettings.mPackages.get(pkg.applicationInfo.packageName);
        boolean z = false;
        if (p == null || p.pkg != null) {
            ApplicationInfo info = getApplicationInfo(pkg.applicationInfo.packageName, 8192, userId);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isApplicationInstalled: pkg ");
                stringBuilder.append(pkg);
                stringBuilder.append(", applicationInfo ");
                stringBuilder.append(info);
                stringBuilder.append(", packageSetting ");
                stringBuilder.append(p);
                Log.e(str, stringBuilder.toString());
            }
            if (!(info == null || p == null)) {
                z = true;
            }
            return z;
        }
        if (DEBUG) {
            Log.w(TAG, "isApplicationInstalled pkg is null, return false");
        }
        return false;
    }

    public boolean isDelapp(PackageSetting ps) {
        File[] dirs = getRemovableAppDirs();
        String codePath = ps.codePath.toString();
        File dir = null;
        for (File dir2 : dirs) {
            if (dir2 != null && dir2.exists()) {
                String[] files = dir2.list();
                for (String file : files) {
                    File file2 = new File(dir2, file);
                    String[] filesSub = file2.list();
                    if (file2.getPath().equals(codePath)) {
                        for (String isPackageFilename : filesSub) {
                            if (isPackageFilename(isPackageFilename)) {
                                return true;
                            }
                        }
                        continue;
                    }
                }
                continue;
            }
        }
        return false;
    }

    public boolean isSystemPathApp(PackageSetting ps) {
        if (ps.codePath == null) {
            return false;
        }
        String codePath = ps.codePath.toString();
        if (codePath.startsWith(SYSTEM_FRAMEWORK_DIR)) {
            return true;
        }
        File[] dirs = getSystemPathAppDirs();
        File dir = null;
        for (File dir2 : dirs) {
            if (dir2 != null && dir2.exists()) {
                String[] files = dir2.list();
                for (String file : files) {
                    File file2 = new File(dir2, file);
                    String[] filesSub = file2.list();
                    if (file2.getPath().equals(codePath)) {
                        for (String isPackageFilename : filesSub) {
                            if (isPackageFilename(isPackageFilename)) {
                                return true;
                            }
                        }
                        continue;
                    }
                }
                continue;
            }
        }
        if ((ps.pkgFlags & 1) == 0) {
            return false;
        }
        for (String equals : INSTALL_SAFEMODE_LIST) {
            if (equals.equals(ps.name)) {
                return true;
            }
        }
        return false;
    }

    private File[] getSystemPathAppDirs() {
        File systemPrivAppDir = new File("/system/priv-app");
        File systemAppDir = new File(SYSTEM_APP_DIR);
        return new File[]{systemPrivAppDir, systemAppDir};
    }

    /* JADX WARNING: Missing block: B:21:?, code skipped:
            r0.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isPrivAppInData(File path, String apkListFile) {
        BufferedReader reader = null;
        boolean result = false;
        try {
            if (mCustPackageManagerService != null && mCustPackageManagerService.isMccMncMatch()) {
                apkListFile = mCustPackageManagerService.getCustomizeAPKListFile(apkListFile, "APKInstallListEMUI5Release.txt", "DelAPKInstallListEMUI5Release.txt", "/data/cust/xml");
            }
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(apkListFile), "UTF-8"));
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    String[] strSplit = line.trim().split(",");
                    if (2 == strSplit.length && isPackageFilename(strSplit[0].trim()) && strSplit[1].trim().equalsIgnoreCase(FLAG_APK_PRIV)) {
                        result = path.getCanonicalPath().startsWith(strSplit[0].trim());
                        if (true != result) {
                        }
                    }
                }
                try {
                    break;
                } catch (Exception e) {
                }
            }
        } catch (FileNotFoundException e2) {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e3) {
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e4) {
                }
            }
        }
        return result;
    }

    public void scanDataDir(int scanMode) {
    }

    private void getMultiAPKInstallList(List<File> lists, HashMap<String, HashSet<String>> multiInstallMap) {
        if (multiInstallMap != null && lists.size() > 0) {
            for (File list : lists) {
                getAPKInstallList(list, multiInstallMap);
            }
        }
    }

    private void getAPKInstallList(File scanApk, HashMap<String, HashSet<String>> multiInstallMap) {
        BufferedReader reader = null;
        String str;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(scanApk), "UTF-8"));
            while (true) {
                String readLine = reader.readLine();
                String line = readLine;
                if (readLine != null) {
                    String[] strSplit = line.trim().split(",");
                    String packagePath = getCustPackagePath(strSplit[0]);
                    if (this.mCotaFlag) {
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("read cota xml getAPKInstallList packagePath = ");
                        stringBuilder.append(packagePath);
                        Log.i(str2, stringBuilder.toString());
                    }
                    StringBuilder stringBuilder2;
                    if (packagePath != null && packagePath.startsWith("/system/app/")) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("pre removable system app, packagePath: ");
                        stringBuilder2.append(packagePath);
                        Flog.i(205, stringBuilder2.toString());
                        ((HashSet) mDefaultSystemList.get(FLAG_APK_SYS)).add(packagePath.trim());
                    } else if (packagePath != null && packagePath.startsWith("/system/priv-app/")) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("pre removable system priv app, packagePath: ");
                        stringBuilder2.append(packagePath);
                        Flog.i(205, stringBuilder2.toString());
                        ((HashSet) mDefaultSystemList.get(FLAG_APK_PRIV)).add(packagePath.trim());
                    } else if (packagePath != null && isPackageFilename(strSplit[0].trim())) {
                        if (2 == strSplit.length && isCheckedKey(strSplit[1].trim(), multiInstallMap.size())) {
                            ((HashSet) multiInstallMap.get(strSplit[1].trim())).add(packagePath.trim());
                        } else if (1 == strSplit.length) {
                            ((HashSet) multiInstallMap.get(FLAG_APK_SYS)).add(packagePath.trim());
                        } else {
                            str = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Config error for packagePath:");
                            stringBuilder3.append(packagePath);
                            Slog.e(str, stringBuilder3.toString());
                        }
                    }
                } else {
                    try {
                        reader.close();
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "PackageManagerService.getAPKInstallList error for closing IO");
                        return;
                    }
                }
            }
        } catch (FileNotFoundException e2) {
            str = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("FileNotFound No such file or directory :");
            stringBuilder4.append(scanApk.getPath());
            Log.w(str, stringBuilder4.toString());
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e3) {
            Log.e(TAG, "PackageManagerService.getAPKInstallList error for IO");
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e4) {
                    Log.e(TAG, "PackageManagerService.getAPKInstallList error for closing IO");
                }
            }
        }
    }

    private boolean isCheckedKey(String key, int mapSize) {
        boolean z = true;
        if (mapSize == 2) {
            if (!(FLAG_APK_SYS.equals(key) || FLAG_APK_PRIV.equals(key))) {
                z = false;
            }
            return z;
        } else if (mapSize != 3) {
            return false;
        } else {
            if (!(FLAG_APK_SYS.equals(key) || FLAG_APK_PRIV.equals(key) || FLAG_APK_NOSYS.equals(key))) {
                z = false;
            }
            return z;
        }
    }

    private void installAPKforInstallList(HashSet<String> installList, int flags, int scanMode, long currentTime) {
        installAPKforInstallList(installList, flags, scanMode, currentTime, 0);
    }

    private void installAPKforInstallList(HashSet<String> installList, int parseFlags, int scanFlags, long currentTime, int hwFlags) {
        if (installList != null && installList.size() != 0) {
            if (this.mCotaFlag) {
                this.mCotaApksInstallStatus = -1;
            }
            int fileSize = installList.size();
            File[] files = new File[fileSize];
            Iterator it = installList.iterator();
            int i = 0;
            while (it.hasNext()) {
                String installPath = (String) it.next();
                File file = new File(installPath);
                if (i < fileSize) {
                    int i2 = i + 1;
                    files[i] = file;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("add package install path : ");
                    stringBuilder.append(installPath);
                    Flog.i(205, stringBuilder.toString());
                    i = i2;
                } else {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("faile to add package install path : ");
                    stringBuilder2.append(installPath);
                    stringBuilder2.append("fileSize:");
                    stringBuilder2.append(fileSize);
                    stringBuilder2.append(",i:");
                    stringBuilder2.append(i);
                    Slog.w(str, stringBuilder2.toString());
                }
            }
            scanPackageFilesLI(files, parseFlags, scanFlags, currentTime, hwFlags);
        }
    }

    private void installAPKforInstallListO(HashSet<String> installList, int flags, int scanMode, long currentTime, int hwFlags) {
        ExecutorService executorService = Executors.newFixedThreadPool(mThreadNum);
        if (this.mCotaFlag) {
            this.mCotaApksInstallStatus = -1;
        }
        Iterator it = installList.iterator();
        while (it.hasNext()) {
            String installPath = (String) it.next();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("package install path : ");
            stringBuilder.append(installPath);
            stringBuilder.append("scanMode:");
            int i = scanMode;
            stringBuilder.append(i);
            Flog.i(205, stringBuilder.toString());
            final File file = new File(installPath);
            if (this.mIsPackageScanMultiThread) {
                final int i2 = flags;
                final int i3 = i;
                final long j = currentTime;
                final int i4 = hwFlags;
                try {
                    executorService.submit(new Runnable() {
                        public void run() {
                            try {
                                HwPackageManagerService.this.scanPackageLI(file, i2, i3, j, null, i4);
                            } catch (PackageManagerException e) {
                                String str = HwPackageManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to parse package: ");
                                stringBuilder.append(e.getMessage());
                                Slog.e(str, stringBuilder.toString());
                            }
                        }
                    });
                } catch (Exception e) {
                    Exception exception = e;
                    this.mIsPackageScanMultiThread = false;
                }
            }
            if (!this.mIsPackageScanMultiThread) {
                try {
                    scanPackageLI(file, flags, i, currentTime, null, hwFlags);
                } catch (PackageManagerException e2) {
                    PackageManagerException packageManagerException = e2;
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to parse package: ");
                    stringBuilder2.append(e2.getMessage());
                    Slog.e(str, stringBuilder2.toString());
                }
            }
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e3) {
        }
    }

    public boolean isUninstallApk(String filePath) {
        return mUninstallApk != null && mUninstallApk.contains(filePath);
    }

    public static void setUninstallApk(String string) {
        if (mUninstallApk != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(mUninstallApk);
            stringBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
            stringBuilder.append(string);
            mUninstallApk = stringBuilder.toString();
            return;
        }
        mUninstallApk = string;
    }

    public static void restoreUninstallApk(String restoreApk) {
        if (mUninstallApk != null && restoreApk != null) {
            for (String apkPath : Pattern.compile("\\s*|\n|\r|\t").matcher(restoreApk).replaceAll("").split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)) {
                mUninstallApk = mUninstallApk.replaceAll(apkPath, "");
            }
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    public void getUninstallApk() {
        ArrayList<File> allList = new ArrayList();
        try {
            allList = HwCfgFilePolicy.getCfgFileList("xml/unstall_apk.xml", 0);
        } catch (NoClassDefFoundError e) {
            Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (allList.size() > 0) {
            Iterator it = allList.iterator();
            while (it.hasNext()) {
                loadUninstallApps((File) it.next());
            }
        }
        try {
            if (!new File(NFC_DEVICE_PATH).exists()) {
                if (this.mAvailableFeatures.containsKey("android.hardware.nfc")) {
                    this.mAvailableFeatures.remove("android.hardware.nfc");
                }
                if (this.mAvailableFeatures.containsKey("android.hardware.nfc.hce")) {
                    this.mAvailableFeatures.remove("android.hardware.nfc.hce");
                }
                if (this.mAvailableFeatures.containsKey("android.hardware.nfc.hcef")) {
                    this.mAvailableFeatures.remove("android.hardware.nfc.hcef");
                }
                if (mUninstallApk != null) {
                    if (!"".equals(mUninstallApk)) {
                        if (!mUninstallApk.contains("/system/app/NfcNci_45.apk")) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(mUninstallApk);
                            stringBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                            stringBuilder.append("/system/app/NfcNci_45.apk");
                            stringBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                            stringBuilder.append("/system/app/HwNfcTag.apk");
                            mUninstallApk = stringBuilder.toString();
                            return;
                        }
                        return;
                    }
                }
                mUninstallApk = "/system/app/NfcNci_45.apk;/system/app/HwNfcTag.apk";
            }
        } catch (Exception e2) {
        }
    }

    private void loadUninstallApps(File list) {
        File file = list;
        if (getCust() != null) {
            file = getCust().customizeUninstallApk(file);
        }
        if (file.exists()) {
            FileInputStream in = null;
            XmlPullParser xpp = null;
            try {
                in = new FileInputStream(file);
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                xpp = factory.newPullParser();
                xpp.setInput(in, null);
                for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                    if (eventType == 2) {
                        if ("apk".equals(xpp.getName())) {
                            setUninstallApk(xpp.nextText());
                        } else if ("restoreapk".equals(xpp.getName())) {
                            restoreUninstallApk(xpp.nextText());
                        }
                    }
                }
                try {
                    in.close();
                } catch (IOException e) {
                }
            } catch (XmlPullParserException e2) {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e3) {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e4) {
                    }
                }
            }
        }
    }

    public boolean isDelappInData(PackageSetting ps) {
        if (ps == null || ps.codePath == null) {
            return false;
        }
        return isDelappInData(ps.codePath.toString());
    }

    public static boolean isPrivAppInCust(File file) {
        HwCustPackageManagerService custObj = (HwCustPackageManagerService) HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]);
        if (custObj != null) {
            return custObj.isPrivAppInCust(file);
        }
        return false;
    }

    private void addTempCotaPartitionApkToHashMap() {
        if (mCotaInstallMap == null) {
            mCotaInstallMap = new HashMap();
        } else {
            mCotaInstallMap.clear();
        }
        File apkInstallList = new File(COTA_APK_XML_PATH);
        HashSet<String> sysInstallSet = new HashSet();
        HashSet<String> privInstallSet = new HashSet();
        mCotaInstallMap.put(FLAG_APK_SYS, sysInstallSet);
        mCotaInstallMap.put(FLAG_APK_PRIV, privInstallSet);
        getAPKInstallList(apkInstallList, mCotaInstallMap);
        if (mCotaDelInstallMap == null) {
            mCotaDelInstallMap = new HashMap();
        } else {
            mCotaDelInstallMap.clear();
        }
        File apkDelInstallList = new File(COTA_DEL_APK_XML_PATH);
        HashSet<String> sysDelInstallSet = new HashSet();
        HashSet<String> privDelInstallSet = new HashSet();
        HashSet<String> noSysDelInstallSet = new HashSet();
        mCotaDelInstallMap.put(FLAG_APK_SYS, sysDelInstallSet);
        mCotaDelInstallMap.put(FLAG_APK_PRIV, privDelInstallSet);
        mCotaDelInstallMap.put(FLAG_APK_NOSYS, noSysDelInstallSet);
        getAPKInstallList(apkDelInstallList, mCotaDelInstallMap);
        HwPackageManagerServiceUtils.setCotaDelInstallMap(mCotaDelInstallMap);
    }

    private void scanTempCotaPartitionDir(int scanMode) {
        if (!mCotaInstallMap.isEmpty()) {
            installAPKforInstallList((HashSet) mCotaInstallMap.get(FLAG_APK_SYS), 16, scanMode | 131072, 0, 0);
            installAPKforInstallList((HashSet) mCotaInstallMap.get(FLAG_APK_PRIV), 16, (scanMode | 131072) | HighBitsDetailModeID.MODE_FOLIAGE, 0, 0);
        }
        if (!mCotaDelInstallMap.isEmpty()) {
            installAPKforInstallList((HashSet) mCotaDelInstallMap.get(FLAG_APK_SYS), 16, scanMode | 131072, 0, 33554432);
            installAPKforInstallList((HashSet) mCotaDelInstallMap.get(FLAG_APK_PRIV), 16, (scanMode | 131072) | HighBitsDetailModeID.MODE_FOLIAGE, 0, 33554432);
            installAPKforInstallList((HashSet) mCotaDelInstallMap.get(FLAG_APK_NOSYS), 0, scanMode, 0, 33554432);
        }
    }

    private void startInstallCotaApks() {
        Throwable th;
        this.mCotaFlag = true;
        addTempCotaPartitionApkToHashMap();
        long beginCotaScanTime = System.currentTimeMillis();
        scanTempCotaPartitionDir(8720);
        long endCotaScanTime = System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("scanTempCotaPartitionDir take time is ");
        stringBuilder.append(endCotaScanTime - beginCotaScanTime);
        Log.i(str, stringBuilder.toString());
        updateAllSharedLibrariesLPw(null);
        this.mPermissionManager.updateAllPermissions(StorageManager.UUID_PRIVATE_INTERNAL, false, this.mTempPkgList, this.mPermissionCallback);
        int pksSize = this.mTempPkgList.size();
        for (int i = 0; i < pksSize; i++) {
            prepareAppDataAfterInstallLIF((Package) this.mTempPkgList.get(i));
        }
        this.mSettings.writeLPr();
        Intent cotaintent = new Intent(COTA_APP_UPDATE_APPWIDGET);
        Bundle extras = new Bundle();
        String[] pkgList = new String[pksSize];
        for (int j = 0; j < pksSize; j++) {
            pkgList[j] = ((Package) this.mTempPkgList.get(j)).packageName;
        }
        extras.putStringArray(COTA_APP_UPDATE_APPWIDGET_EXTRA, pkgList);
        cotaintent.addFlags(268435456);
        cotaintent.putExtras(extras);
        cotaintent.putExtra("android.intent.extra.user_handle", 0);
        this.mContext.sendBroadcast(cotaintent);
        long identity = Binder.clearCallingIdentity();
        long beginCotaScanTime2;
        try {
            int[] userIds = UserManagerService.getInstance().getUserIds();
            int length = userIds.length;
            int i2 = 0;
            while (i2 < length) {
                int userId = userIds[i2];
                if (this.mDefaultPermissionPolicy != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    beginCotaScanTime2 = beginCotaScanTime;
                    try {
                        stringBuilder2.append("Cota apps have installed ,grantCustDefaultPermissions userId = ");
                        beginCotaScanTime = userId;
                        stringBuilder2.append(beginCotaScanTime);
                        Log.i(str2, stringBuilder2.toString());
                        this.mDefaultPermissionPolicy.grantCustDefaultPermissions(beginCotaScanTime);
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } else {
                    beginCotaScanTime2 = beginCotaScanTime;
                }
                i2++;
                beginCotaScanTime = beginCotaScanTime2;
            }
            Binder.restoreCallingIdentity(identity);
            this.mCotaApksInstallStatus = 1;
            if (CotaService.getICotaCallBack() != null) {
                try {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("isCotaAppsInstallFinish = ");
                    stringBuilder3.append(getCotaStatus());
                    Log.i(str3, stringBuilder3.toString());
                    CotaService.getICotaCallBack().onAppInstallFinish(getCotaStatus());
                } catch (Exception e) {
                    String str4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("onAppInstallFinish error,");
                    stringBuilder4.append(e.getMessage());
                    Log.w(str4, stringBuilder4.toString());
                }
            }
            this.mCotaFlag = false;
        } catch (Throwable th3) {
            th = th3;
            beginCotaScanTime2 = beginCotaScanTime;
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    private int getCotaStatus() {
        return this.mCotaApksInstallStatus;
    }

    public void systemReady() {
        super.systemReady();
        if (HwCertificationManager.hasFeature()) {
            if (!HwCertificationManager.isInitialized()) {
                HwCertificationManager.initialize(this.mContext);
            }
            HwCertificationManager.getIntance().systemReady();
        }
        AntiMalPreInstallScanner.getInstance().systemReady();
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.USER_REMOVED".equals(intent.getAction())) {
                    HwPackageManagerService.this.onRemoveUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
                }
            }
        }, userFilter);
        writePreinstalledApkListToFile();
        createPublicityFile();
        if (MAGAZINE_COPYRIGHT_ENABLE) {
            createMagazineFolder();
        }
        if (SystemProperties.getBoolean("ro.config.hw_cota", false)) {
            CotaInstallImpl.getInstance().registInstallCallBack(this.mCotaInstallCallBack);
        }
        if (!TextUtils.isEmpty(SystemProperties.get("ro.config.hw_notch_size", ""))) {
            for (PackageSetting ps : this.mSettings.mPackages.values()) {
                if (ps.getAppUseNotchMode() > 0) {
                    HwNotchScreenWhiteConfig.getInstance().updateAppUseNotchMode(ps.pkg.packageName, ps.getAppUseNotchMode());
                }
            }
        }
    }

    public void addFlagsForRemovablePreApk(Package pkg, int hwFlags) {
        HwPackageManagerServiceUtils.addFlagsForRemovablePreApk(pkg, hwFlags);
    }

    public boolean needInstallRemovablePreApk(Package pkg, int hwFlags) {
        if ((33554432 & hwFlags) == 0 || isApplicationInstalled(pkg) || !isUninstalledDelapp(pkg.packageName)) {
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("needInstallRemovablePreApk :");
        stringBuilder.append(pkg.packageName);
        Flog.i(205, stringBuilder.toString());
        return false;
    }

    public void setGMSPackage(Package pkg) {
        if (pkg.packageName.equals("com.google.android.gsf") && isSystemApp(pkg)) {
            this.mGoogleServicePackage = pkg;
        }
    }

    private static boolean isSystemApp(Package pkg) {
        return (pkg.applicationInfo.flags & 1) != 0;
    }

    public boolean getGMSPackagePermission(Package pkg) {
        return this.mGoogleServicePackage != null && PackageManagerServiceUtils.compareSignatures(this.mGoogleServicePackage.mSigningDetails.signatures, pkg.mSigningDetails.signatures) == 0;
    }

    protected void setUpCustomResolverActivity(Package pkg) {
        synchronized (this.mPackages) {
            super.setUpCustomResolverActivity(pkg);
            if (!TextUtils.isEmpty(HwFrameworkFactory.getHuaweiResolverActivity(this.mContext))) {
                this.mResolveActivity.processName = "system:ui";
                this.mResolveActivity.theme = 16974813;
            }
        }
    }

    private static final boolean isPackageFilename(String name) {
        return name != null && name.endsWith(".apk");
    }

    protected void parseInstallerInfo(int uid, String packageUri) {
        int pid = Binder.getCallingPid();
        Bundle extrasInstallerInfo = new Bundle(1);
        extrasInstallerInfo.putInt(INSTALLATION_EXTRA_PACKAGE_INSTALLER_UID, uid);
        extrasInstallerInfo.putInt(INSTALLATION_EXTRA_PACKAGE_INSTALLER_PID, pid);
        extrasInstallerInfo.putString(INSTALLATION_EXTRA_PACKAGE_URI, packageUri);
        Intent intentInformation = new Intent(ACTION_GET_INSTALLER_PACKAGE_INFO);
        intentInformation.putExtras(extrasInstallerInfo);
        intentInformation.setPackage("com.huawei.systemmanager");
        intentInformation.setFlags(1073741824);
        long identity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcast(intentInformation);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("installPackageWithVerificationAndEncryption:  uid= ");
            stringBuilder.append(uid);
            stringBuilder.append(", pid=");
            stringBuilder.append(pid);
            stringBuilder.append(", packageUri= ");
            stringBuilder.append(packageUri);
            Slog.v(str, stringBuilder.toString());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    protected void parseInstalledPkgInfo(String pkgUri, String pkgName, String pkgVerName, int pkgVerCode, int resultCode, boolean pkgUpdate) {
        String installedPath = "";
        String installerPackageName = "";
        if (!(pkgUri == null || pkgUri.length() == 0)) {
            int splitIndex = pkgUri.indexOf(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
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
        String metaHash = (String) this.pkgMetaHash.remove(pkgName);
        extrasInfo.putString(INSTALLATION_EXTRA_PACKAGE_META_HASH, metaHash);
        extrasInfo.putString(INSTALLATION_EXTRA_INSTALLER_PACKAGE_NAME, installerPackageName);
        Intent intentInfo = new Intent(ACTION_GET_PACKAGE_INSTALLATION_INFO);
        intentInfo.putExtras(extrasInfo);
        intentInfo.setFlags(1073741824);
        this.mContext.sendBroadcast(intentInfo, BROADCAST_PERMISSION);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("POST_INSTALL:  pkgName = ");
        stringBuilder.append(pkgName);
        stringBuilder.append(", pkgUri = ");
        stringBuilder.append(pkgUri);
        stringBuilder.append(", pkgInstalledPath = ");
        stringBuilder.append(installedPath);
        stringBuilder.append(", pkgInstallerPackageName = ");
        stringBuilder.append(installerPackageName);
        stringBuilder.append(", pkgVerName = ");
        stringBuilder.append(pkgVerName);
        stringBuilder.append(", pkgVerCode = ");
        stringBuilder.append(pkgVerCode);
        stringBuilder.append(", resultCode = ");
        stringBuilder.append(resultCode);
        stringBuilder.append(", pkgUpdate = ");
        stringBuilder.append(pkgUpdate);
        stringBuilder.append(", pkgMetaHash = ");
        stringBuilder.append(metaHash);
        Slog.v(str, stringBuilder.toString());
    }

    public boolean containDelPath(String sensePath) {
        return sensePath.startsWith("/data/cust/delapp") || sensePath.startsWith("/system/delapp");
    }

    private boolean isDelappInData(String scanFileString) {
        HashSet<String> hashSet;
        if (!(scanFileString == null || mDelMultiInstallMap == null || mDelMultiInstallMap.isEmpty())) {
            for (Entry<String, HashSet<String>> entry : mDelMultiInstallMap.entrySet()) {
                hashSet = (HashSet) entry.getValue();
                if (hashSet != null && !hashSet.isEmpty() && hashSet.contains(scanFileString)) {
                    return true;
                }
            }
        }
        if (!(scanFileString == null || mCotaDelInstallMap == null || mCotaDelInstallMap.isEmpty())) {
            for (Entry<String, HashSet<String>> entry2 : mCotaDelInstallMap.entrySet()) {
                hashSet = (HashSet) entry2.getValue();
                if (hashSet != null && !hashSet.isEmpty() && hashSet.contains(scanFileString)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addUpdatedRemoveableAppFlag(String scanFileString, String packageName) {
        if (containDelPath(scanFileString) || isDelappInData(scanFileString) || isDelappInCust(scanFileString) || isPreRemovableApp(scanFileString)) {
            synchronized (this.mPackages) {
                mRemoveablePreInstallApks.add(packageName);
                Package p = (Package) this.mPackages.get(packageName);
                if (!(p == null || p.applicationInfo == null)) {
                    ApplicationInfo applicationInfo = p.applicationInfo;
                    applicationInfo.hwFlags &= -33554433;
                    applicationInfo = p.applicationInfo;
                    applicationInfo.hwFlags |= 67108864;
                    this.mPackages.put(p.applicationInfo.packageName, p);
                }
            }
        }
    }

    private static String getCustPackagePath(String readLine) {
        return HwPackageManagerServiceUtils.getCustPackagePath(readLine);
    }

    public boolean needAddUpdatedRemoveableAppFlag(String packageName) {
        if (!mRemoveablePreInstallApks.contains(packageName)) {
            return false;
        }
        mRemoveablePreInstallApks.remove(packageName);
        return true;
    }

    public void addFlagsForUpdatedRemovablePreApk(Package pkg, int hwFlags) {
        HwPackageManagerServiceUtils.addFlagsForUpdatedRemovablePreApk(pkg, hwFlags);
    }

    protected boolean hasOtaUpdate() {
        String str;
        StringBuilder stringBuilder;
        try {
            UserInfo userInfo = sUserManager.getUserInfo(0);
            if (userInfo == null) {
                return false;
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("userInfo.lastLoggedInFingerprint : ");
            stringBuilder.append(userInfo.lastLoggedInFingerprint);
            stringBuilder.append(", Build.FINGERPRINT : ");
            stringBuilder.append(Build.FINGERPRINT);
            Log.i(str, stringBuilder.toString());
            return Objects.equals(userInfo.lastLoggedInFingerprint, Build.FINGERPRINT) ^ 1;
        } catch (Exception e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception is ");
            stringBuilder.append(e);
            Log.i(str, stringBuilder.toString());
            return false;
        }
    }

    private boolean support64BitAbi() {
        int length = Build.SUPPORTED_ABIS.length;
        if (length > 1) {
            String instructionSetA = VMRuntime.getInstructionSet(Build.SUPPORTED_ABIS[0]);
            String instructionSetB = VMRuntime.getInstructionSet(Build.SUPPORTED_ABIS[1]);
            if (instructionSetA.equals("arm64") || instructionSetB.equals("arm64")) {
                return true;
            }
        } else if (length != 1 || VMRuntime.getInstructionSet(Build.SUPPORTED_ABIS[0]).equals("arm")) {
            return false;
        }
        return false;
    }

    protected boolean isOdexMode() {
        boolean support64BitAbi = support64BitAbi();
        File bootArtArm64File = new File("/system/framework/arm64/boot.art");
        File bootArtArmFile = new File("/system/framework/arm/boot.art");
        if (support64BitAbi) {
            if (bootArtArm64File.exists() && bootArtArmFile.exists()) {
                return true;
            }
        } else if (bootArtArmFile.exists()) {
            return true;
        }
        return false;
    }

    private boolean hasPrunedDalvikCache() {
        if (new File(Environment.getDataDirectory(), "system/.dalvik-cache-pruned").exists()) {
            return true;
        }
        return false;
    }

    protected boolean notDexOptForBootingSpeedup(boolean adjustCpuAbi) {
        if (adjustCpuAbi) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notDexOptForBootingSpeedup: adjustCpuAbi ");
            stringBuilder.append(adjustCpuAbi);
            stringBuilder.append(", return false");
            Log.i(str, stringBuilder.toString());
            return false;
        }
        String str2;
        StringBuilder stringBuilder2;
        boolean isOdexCase = isOdexMode();
        if (DEBUG_DEXOPT_OPTIMIZE) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("forceNotDex: isOdexCase ");
            stringBuilder2.append(isOdexCase);
            stringBuilder2.append(", mSystemReady ");
            stringBuilder2.append(this.mSystemReady);
            stringBuilder2.append(", mDexOptTotalTime ");
            stringBuilder2.append(this.mPackageDexOptimizer.getDexOptTotalTime());
            stringBuilder2.append(", isFirstBoot ");
            stringBuilder2.append(isFirstBoot());
            stringBuilder2.append(", hasOtaUpdate ");
            stringBuilder2.append(hasOtaUpdate());
            Log.i(str2, stringBuilder2.toString());
        }
        if (isOdexCase) {
            if (this.mSystemReady) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("forceNotDex = false: isOdexCase ");
                stringBuilder2.append(isOdexCase);
                stringBuilder2.append(", mSystemReady ");
                stringBuilder2.append(this.mSystemReady);
                Log.i(str2, stringBuilder2.toString());
                return false;
            } else if (hasOtaUpdate() && this.mPackageDexOptimizer.getDexOptTotalTime() < HOTA_DEXOPT_THRESHOLD) {
                if (DEBUG_DEXOPT_OPTIMIZE) {
                    Log.i(TAG, "forceNotDex = false: Ota Update & First Boot & withing 3 minutes");
                }
                return false;
            } else if (hasPrunedDalvikCache() && !isFirstBoot() && !hasOtaUpdate()) {
                if (DEBUG_DEXOPT_OPTIMIZE) {
                    Log.i(TAG, "forceNotDex = false: Force reboot when booting, pruned dalvik-cache");
                }
                return false;
            } else if ((mOptimizeForDexopt & 2) != 0) {
                if (DEBUG_DEXOPT_OPTIMIZE) {
                    Log.i(TAG, "forceNotDex = true: Booting now or outgoing 3 minutes");
                }
                return true;
            }
        }
        if (DEBUG_DEXOPT_OPTIMIZE) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("forceNotDex = false: isOdexCase ");
            stringBuilder2.append(isOdexCase);
            stringBuilder2.append(", mSystemReady ");
            stringBuilder2.append(this.mSystemReady);
            Log.i(str2, stringBuilder2.toString());
        }
        return false;
    }

    private String codePathEndName(String readLine) {
        int lastIndex = readLine.lastIndexOf(47);
        if (lastIndex > 0) {
            return readLine.substring(lastIndex + 1);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAPKInstallList ERROR:  ");
        stringBuilder.append(readLine);
        Log.e(str, stringBuilder.toString());
        return null;
    }

    private void loadAppsList(File file, ArrayList<String> appsList) {
        BufferedReader reader = null;
        if (file != null) {
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.defaultCharset()));
                while (true) {
                    String readLine = reader.readLine();
                    String line = readLine;
                    if (readLine != null) {
                        line = line.trim();
                        appsList.add(line);
                        if (DEBUG_DEXOPT_OPTIMIZE) {
                            readLine = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("appsList need add ");
                            stringBuilder.append(line);
                            Log.i(readLine, stringBuilder.toString());
                        }
                    } else {
                        try {
                            break;
                        } catch (Exception e) {
                        }
                    }
                }
                reader.close();
            } catch (FileNotFoundException e2) {
                Log.i(TAG, "loadAppsList FileNotFoundException ");
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e3) {
                Log.i(TAG, "loadAppsList IOException");
                if (reader != null) {
                    reader.close();
                }
            } catch (Throwable th) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e4) {
                    }
                }
            }
        }
    }

    protected boolean filterForceNotDexApps(Package pkg, boolean adjustCpuAbi) {
        StringBuilder stringBuilder;
        if (adjustCpuAbi) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("filterForceNotDexApps: pkg ");
            stringBuilder.append(pkg);
            stringBuilder.append(" adjustCpuAbi ");
            stringBuilder.append(adjustCpuAbi);
            stringBuilder.append(", return false");
            Log.i(str, stringBuilder.toString());
            return false;
        }
        ArrayList<File> allList = new ArrayList();
        try {
            allList = HwCfgFilePolicy.getCfgFileList("dexopt/never_dexopt_apklist.cfg", 0);
        } catch (NoClassDefFoundError e) {
            Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        loadAllAppList(allList);
        if (DEBUG_DEXOPT_OPTIMIZE) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("needed dexopt deferred pkg :");
            stringBuilder2.append(pkg.packageName);
            Log.i(str2, stringBuilder2.toString());
        }
        if ((pkg.applicationInfo.flags & 1) == 0 || (pkg.applicationInfo.flags & 128) != 0 || mForceNotDexApps == null || !mForceNotDexApps.contains(pkg.packageName)) {
            return false;
        }
        if (DEBUG_DEXOPT_OPTIMIZE) {
            String str3 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Skipping dexopt of ");
            stringBuilder.append(pkg.packageName);
            Log.i(str3, stringBuilder.toString());
        }
        return true;
    }

    private void loadAllAppList(ArrayList<File> allList) {
        if (allList.size() > 0) {
            synchronized (this.mPackages) {
                Iterator it = allList.iterator();
                while (it.hasNext()) {
                    File file = (File) it.next();
                    if (DEBUG_DEXOPT_OPTIMIZE) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("start loadForceNotDexApps file. mHaveLoadedNeverDexoptApkList ");
                        stringBuilder.append(this.mHaveLoadedNeverDexoptApkList);
                        Log.i(str, stringBuilder.toString());
                    }
                    if (file.exists() && !this.mHaveLoadedNeverDexoptApkList) {
                        if (DEBUG_DEXOPT_OPTIMIZE) {
                            Log.i(TAG, "loadAppsList file.");
                        }
                        this.mHaveLoadedNeverDexoptApkList = true;
                        loadAppsList(file, mForceNotDexApps);
                    } else if (!file.exists()) {
                        Slog.w(TAG, "/system/etc/dexopt/never_dexopt_apklist.cfg file not exists.");
                    } else if (DEBUG_DEXOPT_OPTIMIZE) {
                        Slog.w(TAG, "/system/etc/dexopt/never_dexopt_apklist.cfg file has loaded.");
                    }
                }
            }
        }
    }

    protected boolean filterDexoptInBootupApps(Package pkg) {
        ArrayList<File> allList = new ArrayList();
        try {
            allList = HwCfgFilePolicy.getCfgFileList("dexopt/dexopt_in_bootup_apklist.cfg", 0);
        } catch (NoClassDefFoundError e) {
            Slog.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (allList.size() > 0) {
            synchronized (this.mPackages) {
                Iterator it = allList.iterator();
                while (it.hasNext()) {
                    File file = (File) it.next();
                    if (DEBUG_DEXOPT_OPTIMIZE) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("start loadAppsList file. mHaveLoadedDexoptInBootUpApkList ");
                        stringBuilder.append(this.mHaveLoadedDexoptInBootUpApkList);
                        Log.i(str, stringBuilder.toString());
                    }
                    if (file.exists() && !this.mHaveLoadedDexoptInBootUpApkList) {
                        if (DEBUG_DEXOPT_OPTIMIZE) {
                            Log.i(TAG, "loadAppsList file.");
                        }
                        this.mHaveLoadedDexoptInBootUpApkList = true;
                        loadAppsList(file, mDexoptInBootupApps);
                    } else if (!file.exists()) {
                        Slog.w(TAG, "/system/etc/dexopt/dexopt_in_bootup_apklist.cfg file not exists.");
                    } else if (DEBUG_DEXOPT_OPTIMIZE) {
                        Slog.w(TAG, "/system/etc/dexopt/dexopt_in_bootup_apklist.cfg file has loaded.");
                    }
                }
            }
        }
        if (DEBUG_DEXOPT_OPTIMIZE) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("needed dexopt deferred pkg :");
            stringBuilder2.append(pkg.packageName);
            Log.i(str2, stringBuilder2.toString());
        }
        if (mDexoptInBootupApps == null || !mDexoptInBootupApps.contains(pkg.packageName)) {
            return false;
        }
        if (DEBUG_DEXOPT_OPTIMIZE) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Need to dexopt ");
            stringBuilder3.append(pkg.packageName);
            stringBuilder3.append(" in bootup.");
            Log.i(str3, stringBuilder3.toString());
        }
        return true;
    }

    protected ArrayList<Package> sortRecentlyUsedApps(Collection<Package> pkgs) {
        String str;
        StringBuilder stringBuilder;
        if (this.mSortDexoptApps != null) {
            this.mSortDexoptApps.clear();
        }
        if (DEBUG_DEXOPT_OPTIMIZE) {
            int size = 0;
            for (Package pkg : pkgs) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("before sortRecentlyUsedApps, remaining pkg : ");
                stringBuilder2.append(pkg.packageName);
                Log.i(str2, stringBuilder2.toString());
                size++;
            }
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("before sortRecentlyUsedApps, all remaining pkgs.size : ");
            stringBuilder3.append(size);
            Log.i(str3, stringBuilder3.toString());
        }
        long current = Calendar.getInstance().getTimeInMillis();
        for (Entry entry : ((UsageStatsManager) this.mContext.getSystemService("usagestats")).queryAndAggregateUsageStats(current - 604800000, current).entrySet()) {
            this.mRecentlyUsedApps.add((UsageStats) entry.getValue());
        }
        Collections.sort(this.mRecentlyUsedApps, this.totalTimeInForegroundComparator);
        int N = this.mRecentlyUsedApps.size();
        for (int i = 0; i < N; i++) {
            UsageStats recentlyUsedApp = (UsageStats) this.mRecentlyUsedApps.get(i);
            if (DEBUG_DEXOPT_OPTIMIZE) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("recentlyUsed Apps : ");
                stringBuilder.append(recentlyUsedApp.mPackageName);
                Log.i(str, stringBuilder.toString());
            }
            Iterator<Package> it = pkgs.iterator();
            while (it.hasNext()) {
                Package pkg2 = (Package) it.next();
                if (recentlyUsedApp.mPackageName.equals(pkg2.packageName)) {
                    if (DEBUG_DEXOPT_OPTIMIZE) {
                        String str4 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Adding recentlyUsedApps : ");
                        stringBuilder4.append(pkg2.packageName);
                        Log.i(str4, stringBuilder4.toString());
                    }
                    this.mSortDexoptApps.add(pkg2);
                    it.remove();
                }
            }
        }
        for (Package pkg3 : pkgs) {
            if (DEBUG_DEXOPT_OPTIMIZE) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Adding remaining app : ");
                stringBuilder.append(pkg3.packageName);
                Log.i(str, stringBuilder.toString());
            }
            this.mSortDexoptApps.add(pkg3);
        }
        this.mRecentlyUsedApps.clear();
        return this.mSortDexoptApps;
    }

    public boolean isSetupDisabled() {
        return this.mSetupDisabled;
    }

    private boolean needSkipSetupPhase() {
        return BluetoothAddressNative.isLibReady() && TextUtils.isEmpty(BluetoothAddressNative.getMacAddress());
    }

    private boolean isSetupPkg(String pname) {
        return HWSETUP_PKG.equals(pname) || GOOGLESETUP_PKG.equals(pname);
    }

    protected boolean skipSetupEnable(String pname) {
        boolean shouldskip = isSetupPkg(pname) && needSkipSetupPhase();
        if (shouldskip) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("skipSetupEnable skip pkg: ");
            stringBuilder.append(pname);
            Slog.i(str, stringBuilder.toString());
        }
        return shouldskip;
    }

    protected boolean makeSetupDisabled(String pname) {
        if (!isSetupPkg(pname) || this.mSettings.isDisabledSystemPackageLPr(pname) || !needSkipSetupPhase()) {
            return false;
        }
        this.mSettings.disableSystemPackageLPw(pname);
        this.mSetupDisabled = true;
        boolean shouldskip = true;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("makeSetupDisabled skip pkg: ");
        stringBuilder.append(pname);
        Slog.w(str, stringBuilder.toString());
        return shouldskip;
    }

    public synchronized HwCustPackageManagerService getCust() {
        if (this.mCust == null) {
            this.mCust = HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]);
        }
        return (HwCustPackageManagerService) this.mCust;
    }

    public void filterShellApps(ArrayList<Package> pkgs, LinkedList<Package> sortedPkgs) {
        if ((!hasOtaUpdate() || isFirstBoot()) && !hasPrunedDalvikCache()) {
            Slog.i(TAG, "Do not filt shell Apps! not OTA case.");
            return;
        }
        HwShellAppsHandler handler = new HwShellAppsHandler(this.mInstaller, sUserManager);
        Iterator it = pkgs.iterator();
        while (it.hasNext()) {
            Package pkg = (Package) it.next();
            String shellName = handler.AnalyseShell(pkg);
            if (shellName != null) {
                if (DEBUG_DEXOPT_SHELL) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Find a ");
                    stringBuilder.append(shellName);
                    stringBuilder.append(" Shell Pkgs: ");
                    stringBuilder.append(pkg.packageName);
                    Log.i(str, stringBuilder.toString());
                }
                sortedPkgs.add(pkg);
                handler.ProcessShellApp(pkg);
            }
        }
        pkgs.removeAll(sortedPkgs);
    }

    public static File getCfgFile(String fileName, int type) throws Exception, NoClassDefFoundError {
        Class<?> filePolicyClazz = Class.forName(FILE_POLICY_CLASS_NAME);
        return (File) filePolicyClazz.getMethod(METHOD_NAME_FOR_FILE, new Class[]{String.class, Integer.TYPE}).invoke(filePolicyClazz, new Object[]{fileName, Integer.valueOf(type)});
    }

    public static File getCustomizedFileName(String xmlName, int flag) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("xml/");
            stringBuilder.append(xmlName);
            return getCfgFile(stringBuilder.toString(), flag);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            return null;
        } catch (Exception e2) {
            Log.d(TAG, "getCustomizedFileName get layout file exception");
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x0048 A:{SYNTHETIC, Splitter:B:23:0x0048} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0039  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void loadMultiWinWhiteList(Context aContext) {
        File configFile = getCustomizedFileName(FILE_MULTIWINDOW_WHITELIST, 0);
        InputStream inputStream = null;
        XmlPullParser xmlParser = null;
        if (configFile != null) {
            try {
                if (configFile.exists()) {
                    inputStream = new FileInputStream(configFile);
                    if (inputStream != null) {
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                Slog.e(TAG, "loadMultiWinWhiteList:- IOE while closing stream", e);
                            }
                        }
                        return;
                    }
                    xmlParser = Xml.newPullParser();
                    xmlParser.setInput(inputStream, null);
                    for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                        String packageName;
                        StringBuilder stringBuilder;
                        if (xmlEventType != 2) {
                            if (xmlEventType == 3 && XML_ELEMENT_APP_LIST.equals(xmlParser.getName())) {
                                break;
                            }
                        } else if (XML_ELEMENT_APP_ITEM.equals(xmlParser.getName())) {
                            packageName = xmlParser.getAttributeValue(null, "package_name");
                            if (packageName != null) {
                                packageName = packageName.toLowerCase();
                            }
                            sMultiWinWhiteListPkgNames.add(packageName);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Multiwindow whitelist package name: [");
                            stringBuilder.append(packageName);
                            stringBuilder.append("]");
                            Flog.i(205, stringBuilder.toString());
                        } else if (XML_ELEMENT_APP_FORCED_PORTRAIT_ITEM.equals(xmlParser.getName())) {
                            packageName = xmlParser.getAttributeValue(null, "package_name");
                            if (packageName != null) {
                                packageName = packageName.toLowerCase();
                            }
                            sMWPortraitWhiteListPkgNames.add(packageName);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Multiwindow portrait whitelist package name: [");
                            stringBuilder.append(packageName);
                            stringBuilder.append("]");
                            Flog.i(205, stringBuilder.toString());
                        } else if (XML_ONE_SPLIT_SCREEN_VIDEO_ITEM.equals(xmlParser.getName())) {
                            packageName = xmlParser.getAttributeValue(null, "package_name");
                            if (packageName != null) {
                                packageName = packageName.toLowerCase();
                            }
                            oneSplitScreenVideoListPkgNames.add(packageName);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("one split screen video whitelist package name: [");
                            stringBuilder.append(packageName);
                            stringBuilder.append("]");
                            Flog.i(205, stringBuilder.toString());
                        } else if (XML_ONE_SPLIT_SCREEN_IMS_ITEM.equals(xmlParser.getName())) {
                            packageName = xmlParser.getAttributeValue(null, "package_name");
                            if (packageName != null) {
                                packageName = packageName.toLowerCase();
                            }
                            oneSplitScreenImsListPkgNames.add(packageName);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("one split screen IMS whitelist package name: [");
                            stringBuilder.append(packageName);
                            stringBuilder.append("]");
                            Flog.i(205, stringBuilder.toString());
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e2) {
                            Slog.e(TAG, "loadMultiWinWhiteList:- IOE while closing stream", e2);
                        }
                    }
                    return;
                }
            } catch (FileNotFoundException e22) {
                Log.e(TAG, "loadMultiWinWhiteList", e22);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (XmlPullParserException e222) {
                Log.e(TAG, "loadMultiWinWhiteList", e222);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e2222) {
                Log.e(TAG, "loadMultiWinWhiteList", e2222);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e3) {
                        Slog.e(TAG, "loadMultiWinWhiteList:- IOE while closing stream", e3);
                    }
                }
            }
        }
        Flog.i(205, "Multi Window white list taken from default configuration");
        inputStream = aContext.getAssets().open(FILE_MULTIWINDOW_WHITELIST);
        if (inputStream != null) {
        }
    }

    /* JADX WARNING: Missing block: B:9:0x001b, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean isInMultiWinWhiteList(String packageName) {
        if (packageName == null || sMultiWinWhiteListPkgNames.size() == 0 || !sMultiWinWhiteListPkgNames.contains(packageName.toLowerCase())) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:9:0x001b, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean isInMWPortraitWhiteList(String packageName) {
        if (packageName == null || sMWPortraitWhiteListPkgNames.size() == 0 || !sMWPortraitWhiteListPkgNames.contains(packageName.toLowerCase())) {
            return false;
        }
        return true;
    }

    protected void checkHwCertification(Package pkg, boolean isUpdate) {
        if (!HwCertificationManager.hasFeature()) {
            return;
        }
        if (HwCertificationManager.isSupportHwCertification(pkg)) {
            if (isUpdate || !isContainHwCertification(pkg) || isUpgrade()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("will checkCertificationInner,isUpdate = ");
                stringBuilder.append(isUpdate);
                stringBuilder.append("isHotaUpGrade = ");
                stringBuilder.append(isUpgrade());
                Slog.i("HwCertificationManager", stringBuilder.toString());
                hwCertCleanUp(pkg);
                checkCertificationInner(pkg);
            }
            return;
        }
        if (isContainHwCertification(pkg)) {
            hwCertCleanUp(pkg);
        }
    }

    private void checkCertificationInner(Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (!(manager == null || manager.checkHwCertification(pkg))) {
            Slog.e("HwCertificationManager", "checkHwCertification parse error");
        }
    }

    protected boolean getHwCertificationPermission(boolean allowed, Package pkg, String perm) {
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

    private void hwCertCleanUp(Package pkg) {
        HwCertificationManager manager = HwCertificationManager.getIntance();
        if (manager != null) {
            manager.cleanUp(pkg);
        }
    }

    protected void hwCertCleanUp() {
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

    protected boolean isHwCustHiddenInfoPackage(Package pkgInfo) {
        if (getCust() != null) {
            return getCust().isHwCustHiddenInfoPackage(pkgInfo);
        }
        return false;
    }

    void installStage(String packageName, File stagedDir, IPackageInstallObserver2 observer, SessionParams sessionParams, String installerPackageName, int installerUid, UserHandle user, SigningDetails signingDetails) {
        super.installStage(packageName, stagedDir, observer, sessionParams, installerPackageName, installerUid, new UserHandle(redirectInstallForClone(user.getIdentifier())), signingDetails);
    }

    protected boolean isAppInstallAllowed(String installer, String appName) {
        if (isParentControlEnabled() && !isInstallerValidForParentControl(installer) && getPackageInfo(appName, 0, 0) == null) {
            return false;
        }
        return true;
    }

    private boolean isParentControlEnabled() {
        if (Secure.getInt(this.mContext.getContentResolver(), "childmode_status", 0) == 0 || getPackageInfo("com.huawei.parentcontrol", 0, 0) == null || !isChinaArea()) {
            return false;
        }
        return true;
    }

    private boolean isChinaArea() {
        return SystemProperties.get("ro.config.hw_optb", "0").equals("156");
    }

    private boolean isInstallerValidForParentControl(String installer) {
        String whiteInstallerPackages = Secure.getString(this.mContext.getContentResolver(), "childmode_installer_whitelist");
        if (!(whiteInstallerPackages == null || "".equals(whiteInstallerPackages.trim()) || installer == null)) {
            for (String pkg : whiteInstallerPackages.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)) {
                if (installer.equals(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUnAppInstallAllowed(String originPath) {
        if (mCustPackageManagerService == null || !mCustPackageManagerService.isUnAppInstallAllowed(originPath, this.mContext)) {
            return false;
        }
        return true;
    }

    private void initBlackList() {
        Iterator it;
        BlackListAppsUtils.readBlackList(this.mBlackListInfo);
        synchronized (this.mPackages) {
            BlackListAppsUtils.readDisableAppList(this.mDisableAppListInfo);
            it = this.mDisableAppListInfo.mBlackList.iterator();
            while (it.hasNext()) {
                BlackListApp app = (BlackListApp) it.next();
                this.mDisableAppMap.put(app.mPackageName, app);
            }
        }
        boolean z = (this.mBlackListInfo.mBlackList.size() == 0 || this.mBlackListInfo.mVersionCode == -1) ? false : true;
        this.isBlackListExist = z;
        if (this.isBlackListExist) {
            synchronized (this.mPackages) {
                if (!(hasOtaUpdate() || BlackListAppsUtils.isBlackListUpdate(this.mBlackListInfo, this.mDisableAppListInfo))) {
                    if (validateDisabledAppFile()) {
                        z = false;
                    }
                }
                z = true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("initBlackList start, is completed process: ");
            stringBuilder.append(z);
            Slog.d(str, stringBuilder.toString());
            if (z) {
                synchronized (this.mPackages) {
                    String pkg;
                    Set<String> needDisablePackage = new ArraySet();
                    Set<String> needEnablePackage = new ArraySet();
                    Iterator it2 = this.mBlackListInfo.mBlackList.iterator();
                    while (it2.hasNext()) {
                        BlackListApp app2 = (BlackListApp) it2.next();
                        pkg = app2.mPackageName;
                        if (!needDisablePackage.contains(pkg)) {
                            if (BlackListAppsUtils.comparePackage((Package) this.mPackages.get(pkg), app2) && !needDisablePackage.contains(pkg)) {
                                setPackageDisableFlag(pkg, true);
                                needDisablePackage.add(pkg);
                                this.mDisableAppMap.put(pkg, app2);
                            }
                        }
                    }
                    for (String pkg2 : new ArrayList(this.mDisableAppMap.keySet())) {
                        if (!(BlackListAppsUtils.containsApp(this.mBlackListInfo.mBlackList, (BlackListApp) this.mDisableAppMap.get(pkg2)) || needDisablePackage.contains(pkg2))) {
                            if (this.mPackages.get(pkg2) != null) {
                                needEnablePackage.add(pkg2);
                            }
                            this.mDisableAppMap.remove(pkg2);
                        }
                    }
                    enableComponentForAllUser(needEnablePackage, true);
                    enableComponentForAllUser(needDisablePackage, false);
                    this.mDisableAppListInfo.mBlackList.clear();
                    for (Entry<String, BlackListApp> entry : this.mDisableAppMap.entrySet()) {
                        this.mDisableAppListInfo.mBlackList.add((BlackListApp) entry.getValue());
                    }
                    this.mDisableAppListInfo.mVersionCode = this.mBlackListInfo.mVersionCode;
                    BlackListAppsUtils.writeBlackListToXml(this.mDisableAppListInfo);
                }
            } else {
                Set<String> needDisablePackage2 = new ArraySet();
                for (Entry<String, BlackListApp> entry2 : this.mDisableAppMap.entrySet()) {
                    setPackageDisableFlag((String) entry2.getKey(), true);
                    needDisablePackage2.add((String) entry2.getKey());
                }
                enableComponentForAllUser(needDisablePackage2, false);
            }
            Slog.d(TAG, "initBlackList end");
            return;
        }
        synchronized (this.mPackages) {
            if (this.mDisableAppMap.size() > 0) {
                Slog.d(TAG, "blacklist not exists, enable all disabled apps");
                Set<String> needEnablePackage2 = new ArraySet();
                for (Entry<String, BlackListApp> entry3 : this.mDisableAppMap.entrySet()) {
                    needEnablePackage2.add((String) entry3.getKey());
                }
                enableComponentForAllUser(needEnablePackage2, true);
                this.mDisableAppMap.clear();
            }
        }
        BlackListAppsUtils.deleteDisableAppListFile();
    }

    private boolean validateDisabledAppFile() {
        if (this.mBlackListInfo.mBlackList.size() == 0) {
            return false;
        }
        synchronized (this.mPackages) {
            for (Entry<String, BlackListApp> entry : this.mDisableAppMap.entrySet()) {
                if (this.mPackages.get(entry.getKey()) == null) {
                    return false;
                } else if (!BlackListAppsUtils.containsApp(this.mBlackListInfo.mBlackList, (BlackListApp) entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    private void enableComponentForAllUser(Set<String> packages, boolean enable) {
        for (int userId : sUserManager.getUserIds()) {
            if (packages != null && packages.size() > 0) {
                for (String pkg : packages) {
                    enableComponentForPackage(pkg, enable, userId);
                }
            }
        }
    }

    private void setPackageDisableFlag(String packageName, boolean disable) {
        if (!TextUtils.isEmpty(packageName)) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg != null) {
                ApplicationInfo applicationInfo;
                if (disable) {
                    applicationInfo = pkg.applicationInfo;
                    applicationInfo.hwFlags |= 268435456;
                } else {
                    applicationInfo = pkg.applicationInfo;
                    applicationInfo.hwFlags |= -268435457;
                }
            }
        }
    }

    private void enableComponentForPackage(String packageName, boolean enable, int userId) {
        if (!TextUtils.isEmpty(packageName)) {
            int i = 0;
            int newState = enable ? 0 : 2;
            PackageInfo packageInfo = getPackageInfo(packageName, 786959, userId);
            if (!(packageInfo == null || packageInfo.receivers == null || packageInfo.receivers.length == 0)) {
                for (ActivityInfo activityInfo : packageInfo.receivers) {
                    setEnabledComponentInner(new ComponentName(packageName, activityInfo.name), newState, userId);
                }
            }
            if (!(packageInfo == null || packageInfo.services == null || packageInfo.services.length == 0)) {
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    setEnabledComponentInner(new ComponentName(packageName, serviceInfo.name), newState, userId);
                }
            }
            if (!(packageInfo == null || packageInfo.providers == null || packageInfo.providers.length == 0)) {
                for (ProviderInfo providerInfo : packageInfo.providers) {
                    setEnabledComponentInner(new ComponentName(packageName, providerInfo.name), newState, userId);
                }
            }
            if (!(packageInfo == null || packageInfo.activities == null || packageInfo.activities.length == 0)) {
                while (i < packageInfo.activities.length) {
                    setEnabledComponentInner(new ComponentName(packageName, packageInfo.activities[i].name), newState, userId);
                    i++;
                }
            }
            if (!enable) {
                clearPackagePreferredActivitiesLPw(packageName, userId);
            }
            scheduleWritePackageRestrictionsLocked(userId);
        }
    }

    protected void updatePackageBlackListInfo(String packageName) {
        if (this.isBlackListExist && !TextUtils.isEmpty(packageName)) {
            int[] userIds = sUserManager.getUserIds();
            synchronized (this.mPackages) {
                Package pkgInfo = (Package) this.mPackages.get(packageName);
                boolean needDisable = false;
                boolean needEnable = false;
                if (pkgInfo != null) {
                    Iterator it = this.mBlackListInfo.mBlackList.iterator();
                    while (it.hasNext()) {
                        BlackListApp app = (BlackListApp) it.next();
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
                for (Entry<String, BlackListApp> entry : this.mDisableAppMap.entrySet()) {
                    this.mDisableAppListInfo.mBlackList.add((BlackListApp) entry.getValue());
                }
                this.mDisableAppListInfo.mVersionCode = this.mBlackListInfo.mVersionCode;
                BlackListAppsUtils.writeBlackListToXml(this.mDisableAppListInfo);
            }
        }
    }

    /* JADX WARNING: Missing block: B:33:0x0072, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setEnabledComponentInner(ComponentName componentName, int newState, int userId) {
        if (componentName != null) {
            String packageName = componentName.getPackageName();
            String className = componentName.getClassName();
            if (packageName != null && className != null) {
                synchronized (this.mPackages) {
                    PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
                    if (pkgSetting == null) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setEnabledSetting, can not find pkgSetting, packageName = ");
                        stringBuilder.append(packageName);
                        Slog.e(str, stringBuilder.toString());
                        return;
                    }
                    String str2;
                    StringBuilder stringBuilder2;
                    Package pkg = pkgSetting.pkg;
                    if (pkg != null) {
                        if (pkg.hasComponentClassName(className)) {
                            if (newState != 0) {
                                if (newState != 2) {
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Invalid new component state: ");
                                    stringBuilder2.append(newState);
                                    Slog.e(str2, stringBuilder2.toString());
                                    return;
                                } else if (!pkgSetting.disableComponentLPw(className, userId)) {
                                    return;
                                }
                            } else if (!pkgSetting.restoreComponentLPw(className, userId)) {
                                return;
                            }
                        }
                    }
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed setComponentEnabledSetting: component class ");
                    stringBuilder2.append(className);
                    stringBuilder2.append(" does not exist in ");
                    stringBuilder2.append(packageName);
                    Slog.w(str2, stringBuilder2.toString());
                }
            }
        }
    }

    private void initBlackListForNewUser(int userHandle) {
        if (this.isBlackListExist) {
            synchronized (this.mPackages) {
                for (String pkg : this.mDisableAppMap.keySet()) {
                    enableComponentForPackage(pkg, false, userHandle);
                }
            }
        }
    }

    void onNewUserCreated(int userId) {
        super.onNewUserCreated(userId);
        initBlackListForNewUser(userId);
    }

    protected void initHwCertificationManager() {
        if (!HwCertificationManager.isInitialized()) {
            HwCertificationManager.initialize(this.mContext);
        }
        HwCertificationManager manager = HwCertificationManager.getIntance();
    }

    protected int getHwCertificateType(Package pkg) {
        if (HwCertificationManager.isSupportHwCertification(pkg)) {
            return HwCertificationManager.getIntance().getHwCertificateType(pkg.packageName);
        }
        return HwCertificationManager.getIntance().getHwCertificateTypeNotMDM();
    }

    protected boolean isContainHwCertification(Package pkg) {
        return HwCertificationManager.getIntance().isContainHwCertification(pkg.packageName);
    }

    private boolean isSystemSecure() {
        return "1".equals(SystemProperties.get(SYSTEM_SECURE, "1")) && "0".equals(SystemProperties.get(SYSTEM_DEBUGGABLE, "0"));
    }

    public void loadSysWhitelist() {
        AntiMalPreInstallScanner.init(this.mContext, isUpgrade());
        AntiMalPreInstallScanner.getInstance().loadSysWhitelist();
    }

    public void checkIllegalSysApk(Package pkg, int hwFlags) throws PackageManagerException {
        if (checkGmsCoreByInstaller(pkg)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkIllegalSysApk checkGmsCoreByInstaller");
            stringBuilder.append(pkg);
            Slog.i(str, stringBuilder.toString());
            return;
        }
        switch (AntiMalPreInstallScanner.getInstance().checkIllegalSysApk(pkg, hwFlags)) {
            case 1:
                if (isSystemSecure() || ANTIMAL_DEBUG_ON) {
                    throw new PackageManagerException(-115, "checkIllegalSysApk add illegally!");
                }
            case 2:
                hwFlags |= 33554432;
                addFlagsForRemovablePreApk(pkg, hwFlags);
                if (!needInstallRemovablePreApk(pkg, hwFlags)) {
                    throw new PackageManagerException(-115, "checkIllegalSysApk apk changed illegally!");
                }
                break;
        }
    }

    protected void addGrantedInstalledPkg(String pkgName, boolean grant) {
        if (grant) {
            synchronized (this.mGrantedInstalledPkg) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive() package added:");
                stringBuilder.append(pkgName);
                Slog.i(str, stringBuilder.toString());
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

    public static boolean isPrivAppNonSystemPartitionDir(File path) {
        HashSet<String> privAppHashSet;
        if (!(path == null || mMultiInstallMap == null || mDelMultiInstallMap == null)) {
            privAppHashSet = (HashSet) mMultiInstallMap.get(FLAG_APK_PRIV);
            if (privAppHashSet != null && !privAppHashSet.isEmpty() && privAppHashSet.contains(path.getPath())) {
                return true;
            }
            privAppHashSet = (HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV);
            if (!(privAppHashSet == null || privAppHashSet.isEmpty() || !privAppHashSet.contains(path.getPath()))) {
                return true;
            }
        }
        if (!(path == null || mCotaInstallMap == null || mCotaDelInstallMap == null)) {
            privAppHashSet = (HashSet) mCotaInstallMap.get(FLAG_APK_PRIV);
            if (privAppHashSet != null && !privAppHashSet.isEmpty() && privAppHashSet.contains(path.getPath())) {
                return true;
            }
            privAppHashSet = (HashSet) mCotaDelInstallMap.get(FLAG_APK_PRIV);
            if (!(privAppHashSet == null || privAppHashSet.isEmpty() || !privAppHashSet.contains(path.getPath()))) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    private synchronized void addNonSystemPartitionApkToHashMap() {
        int i;
        HashSet<String> sysInstallSet;
        int i2 = 0;
        if (mMultiInstallMap == null) {
            mMultiInstallMap = new HashMap();
            ArrayList<File> allAPKList = getApkInstallFileCfgList(APK_INSTALLFILE);
            if (allAPKList != null) {
                for (i = 0; i < allAPKList.size(); i++) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("get all apk cfg list -->");
                    stringBuilder.append(i);
                    stringBuilder.append(" --");
                    stringBuilder.append(((File) allAPKList.get(i)).getPath());
                    Flog.i(205, stringBuilder.toString());
                }
                sysInstallSet = new HashSet();
                HashSet<String> privInstallSet = new HashSet();
                mMultiInstallMap.put(FLAG_APK_SYS, sysInstallSet);
                mMultiInstallMap.put(FLAG_APK_PRIV, privInstallSet);
                getMultiAPKInstallList(allAPKList, mMultiInstallMap);
            }
        }
        if (mDelMultiInstallMap == null) {
            mDelMultiInstallMap = new HashMap();
            ArrayList<File> allDelAPKList = getApkInstallFileCfgList(DELAPK_INSTALLFILE);
            if (allDelAPKList != null) {
                while (true) {
                    i = i2;
                    if (i >= allDelAPKList.size()) {
                        break;
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("get all del apk cfg list -->");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" --");
                    stringBuilder2.append(((File) allDelAPKList.get(i)).getPath());
                    Flog.i(205, stringBuilder2.toString());
                    i2 = i + 1;
                }
                sysInstallSet = new HashSet();
                HashSet<String> privInstallSet2 = new HashSet();
                HashSet<String> noSysInstallSet = new HashSet();
                mDelMultiInstallMap.put(FLAG_APK_SYS, sysInstallSet);
                mDelMultiInstallMap.put(FLAG_APK_PRIV, privInstallSet2);
                mDelMultiInstallMap.put(FLAG_APK_NOSYS, noSysInstallSet);
                getMultiAPKInstallList(allDelAPKList, mDelMultiInstallMap);
                HwPackageManagerServiceUtils.setDelMultiInstallMap(mDelMultiInstallMap);
            }
        }
    }

    public void scanNonSystemPartitionDir(int scanMode) {
        if (!mMultiInstallMap.isEmpty()) {
            installAPKforInstallList((HashSet) mMultiInstallMap.get(FLAG_APK_SYS), 16, scanMode | 131072, 0);
            installAPKforInstallList((HashSet) mMultiInstallMap.get(FLAG_APK_PRIV), 16, (scanMode | 131072) | HighBitsDetailModeID.MODE_FOLIAGE, 0);
        }
        if (!mDelMultiInstallMap.isEmpty()) {
            installAPKforInstallList((HashSet) mDelMultiInstallMap.get(FLAG_APK_SYS), 16, scanMode | 131072, 0, 33554432);
            installAPKforInstallList((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV), 16, (scanMode | 131072) | HighBitsDetailModeID.MODE_FOLIAGE, 0, 33554432);
            installAPKforInstallList((HashSet) mDelMultiInstallMap.get(FLAG_APK_NOSYS), 0, scanMode, 0, 33554432);
        }
    }

    protected void readPreInstallApkList() {
        mDefaultSystemList = new HashMap();
        if (mMultiInstallMap == null || mDelMultiInstallMap == null) {
            HashSet<String> sysInstallSet = new HashSet();
            HashSet<String> privInstallSet = new HashSet();
            mDefaultSystemList.put(FLAG_APK_SYS, sysInstallSet);
            mDefaultSystemList.put(FLAG_APK_PRIV, privInstallSet);
            addNonSystemPartitionApkToHashMap();
        }
        addGmsCoreApkToHashMap();
    }

    protected boolean isPreRemovableApp(String codePath) {
        String path;
        if (codePath.endsWith(".apk")) {
            path = getCustPackagePath(codePath);
        } else {
            path = codePath;
        }
        boolean z = false;
        if (path == null) {
            return false;
        }
        boolean res1 = path.startsWith("/data/cust/delapp/") || path.startsWith("/system/delapp/");
        if (res1) {
            return true;
        }
        boolean z2;
        boolean res2 = ((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV)).contains(path) || ((HashSet) mDelMultiInstallMap.get(FLAG_APK_SYS)).contains(path) || ((HashSet) mDelMultiInstallMap.get(FLAG_APK_NOSYS)).contains(path);
        if (mCotaDelInstallMap != null) {
            z2 = res2 || ((HashSet) mCotaDelInstallMap.get(FLAG_APK_PRIV)).contains(path) || ((HashSet) mCotaDelInstallMap.get(FLAG_APK_SYS)).contains(path) || ((HashSet) mCotaDelInstallMap.get(FLAG_APK_NOSYS)).contains(path);
            res2 = z2;
        }
        if (mDefaultSystemList == null) {
            Flog.i(205, "isPreRemovableApp-> mDefaultSystemList is null;");
            return res2;
        }
        z2 = ((HashSet) mDefaultSystemList.get(FLAG_APK_PRIV)).contains(path) || ((HashSet) mDefaultSystemList.get(FLAG_APK_SYS)).contains(path);
        if (res2 || z2) {
            z = true;
        }
        return z;
    }

    private boolean isPrivilegedPreApp(String codePath) {
        String path;
        if (codePath.endsWith(".apk")) {
            path = getCustPackagePath(codePath);
        } else {
            path = codePath;
        }
        boolean z = false;
        if (path == null) {
            return false;
        }
        if (path.startsWith("/system/priv-app/")) {
            return true;
        }
        boolean normalDelMultiApp = mDelMultiInstallMap != null && ((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV)).contains(path);
        boolean normalMultiApp = mMultiInstallMap != null && ((HashSet) mMultiInstallMap.get(FLAG_APK_PRIV)).contains(path);
        boolean cotaDelMultiApp = mCotaDelInstallMap != null && ((HashSet) mCotaDelInstallMap.get(FLAG_APK_PRIV)).contains(path);
        boolean cotaMultiApp = mCotaInstallMap != null && ((HashSet) mCotaInstallMap.get(FLAG_APK_PRIV)).contains(path);
        if (normalDelMultiApp || normalMultiApp || cotaDelMultiApp || cotaMultiApp) {
            z = true;
        }
        return z;
    }

    public boolean isNoSystemPreApp(String codePath) {
        return HwPackageManagerServiceUtils.isNoSystemPreApp(codePath);
    }

    private List<String> getScanInstallList() {
        if (mUninstalledMap == null || mUninstalledMap.size() == 0) {
            return null;
        }
        List<String> res = new ArrayList();
        int currentUserId = UserHandle.getCallingUserId();
        for (String str : mUninstalledMap.keySet()) {
            PackageSetting psTemp = this.mSettings.getPackageLPr(str);
            if (psTemp == null || !psTemp.getInstalled(currentUserId)) {
                res.add((String) mUninstalledMap.get(str));
            }
        }
        return res;
    }

    private boolean assertScanInstallApkLocked(String packageName, String apkFile, int userId) {
        String str;
        StringBuilder stringBuilder;
        if (this.mScanInstallApkList.contains(apkFile)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Scan install , the apk file ");
            stringBuilder.append(apkFile);
            stringBuilder.append(" is already in scanning.  Skipping duplicate.");
            Slog.w(str, stringBuilder.toString());
            return false;
        } else if (mUninstalledMap == null || !mUninstalledMap.containsValue(apkFile)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Scan install , the apk file ");
            stringBuilder.append(apkFile);
            stringBuilder.append(" is not a uninstalled system app's codePath.  Skipping.");
            Slog.w(str, stringBuilder.toString());
            return false;
        } else if (userId == -1) {
            Slog.i(TAG, "Scan install for all users!");
            return true;
        } else {
            PackageSetting psTemp = this.mSettings.getPackageLPr(packageName);
            if (psTemp == null || !psTemp.getInstalled(userId)) {
                return true;
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Scan install , ");
            stringBuilder2.append(packageName);
            stringBuilder2.append(" is already installed in user ");
            stringBuilder2.append(userId);
            stringBuilder2.append(" .  Skipping scan ");
            stringBuilder2.append(apkFile);
            Slog.w(str2, stringBuilder2.toString());
            return false;
        }
    }

    private boolean checkScanInstallCaller() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 1000) {
            return true;
        }
        return SCAN_INSTALL_CALLER_PACKAGES.contains(getNameForUid(callingUid));
    }

    /* JADX WARNING: Removed duplicated region for block: B:88:0x0210 A:{SYNTHETIC} */
    /* JADX WARNING: Missing block: B:28:0x0092, code skipped:
            r15 = false;
     */
    /* JADX WARNING: Missing block: B:30:?, code skipped:
            r8 = r9.mSettings.getPackageLPr(r14);
     */
    /* JADX WARNING: Missing block: B:31:0x009a, code skipped:
            if (r8 == null) goto L_0x0187;
     */
    /* JADX WARNING: Missing block: B:33:0x00a6, code skipped:
            if (r8.isAnyInstalled(sUserManager.getUserIds()) == false) goto L_0x0187;
     */
    /* JADX WARNING: Missing block: B:35:0x00ae, code skipped:
            if (r10.equals(r8.codePathString) != false) goto L_0x00d5;
     */
    /* JADX WARNING: Missing block: B:36:0x00b0, code skipped:
            if (r21 == null) goto L_0x00b3;
     */
    /* JADX WARNING: Missing block: B:38:0x00b3, code skipped:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("Scan install ,");
            r1.append(r14);
            r1.append(" installed by other user from ");
            r1.append(r8.codePathString);
            android.util.Slog.w(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:40:0x00d6, code skipped:
            if (r11 != -1) goto L_0x00e3;
     */
    /* JADX WARNING: Missing block: B:41:0x00d8, code skipped:
            r0 = r8.queryInstalledUsers(sUserManager.getUserIds(), false);
     */
    /* JADX WARNING: Missing block: B:42:0x00e3, code skipped:
            r0 = new int[]{r11};
     */
    /* JADX WARNING: Missing block: B:43:0x00e7, code skipped:
            r2 = 1;
            r1 = 0;
     */
    /* JADX WARNING: Missing block: B:45:0x00eb, code skipped:
            if (r1 >= r0.length) goto L_0x0148;
     */
    /* JADX WARNING: Missing block: B:47:0x00ef, code skipped:
            if (r0[r1] == 0) goto L_0x011b;
     */
    /* JADX WARNING: Missing block: B:49:0x00fb, code skipped:
            if (getUserManagerInternal().isClonedProfile(r0[r1]) == false) goto L_0x011b;
     */
    /* JADX WARNING: Missing block: B:50:0x00fd, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Scan install, skipping cloned user ");
            r4.append(r0[r1]);
            r4.append("!");
            android.util.Slog.d(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:51:0x011b, code skipped:
            r2 = installExistingPackageAsUserInternal(r14, r0[r1], 0, 0);
     */
    /* JADX WARNING: Missing block: B:52:0x0122, code skipped:
            if (1 == r2) goto L_0x0145;
     */
    /* JADX WARNING: Missing block: B:53:0x0124, code skipped:
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Scan install failed for user ");
            r4.append(r0[r1]);
            r4.append(", installExistingPackageAsUser:");
            r4.append(r10);
            android.util.Slog.w(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:54:0x0145, code skipped:
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:55:0x0148, code skipped:
            if (1 != r2) goto L_0x014b;
     */
    /* JADX WARNING: Missing block: B:57:0x014b, code skipped:
            r13 = false;
     */
    /* JADX WARNING: Missing block: B:58:0x014c, code skipped:
            r15 = r13;
     */
    /* JADX WARNING: Missing block: B:59:0x014d, code skipped:
            if (r15 == false) goto L_0x0167;
     */
    /* JADX WARNING: Missing block: B:60:0x014f, code skipped:
            r1 = r9.mSettings.getPackageLPr(r14);
     */
    /* JADX WARNING: Missing block: B:61:0x0155, code skipped:
            if (r1 == null) goto L_0x0167;
     */
    /* JADX WARNING: Missing block: B:63:0x0162, code skipped:
            if (r1.queryInstalledUsers(sUserManager.getUserIds(), false).length != 0) goto L_0x0167;
     */
    /* JADX WARNING: Missing block: B:64:0x0164, code skipped:
            removeFromUninstalledDelapp(r14);
     */
    /* JADX WARNING: Missing block: B:65:0x0167, code skipped:
            r1 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Scan install , installExistingPackageAsUser:");
            r3.append(r10);
            r3.append(" success:");
            r3.append(r15);
            android.util.Slog.d(r1, r3.toString());
     */
    /* JADX WARNING: Missing block: B:66:0x0187, code skipped:
            r2 = new java.io.File(r10);
            r0 = 139792;
            r1 = 16;
     */
    /* JADX WARNING: Missing block: B:67:0x0195, code skipped:
            if (isNoSystemPreApp(r10) == false) goto L_0x01a1;
     */
    /* JADX WARNING: Missing block: B:68:0x0197, code skipped:
            r1 = 0;
            r0 = 139792 & -131073;
     */
    /* JADX WARNING: Missing block: B:71:0x01a5, code skipped:
            if (isPrivilegedPreApp(r10) == false) goto L_0x019c;
     */
    /* JADX WARNING: Missing block: B:72:0x01a7, code skipped:
            r0 = 139792 | vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsDetailModeID.MODE_FOLIAGE;
     */
    /* JADX WARNING: Missing block: B:75:0x01ba, code skipped:
            r19 = r8;
     */
    /* JADX WARNING: Missing block: B:78:0x01c9, code skipped:
            if (scanPackageLI(r2, r1, r0, 0, new android.os.UserHandle(android.os.UserHandle.getUserId(android.os.Binder.getCallingUid())), 1107296256) == null) goto L_0x01cd;
     */
    /* JADX WARNING: Missing block: B:79:0x01cb, code skipped:
            r12 = true;
     */
    /* JADX WARNING: Missing block: B:80:0x01cd, code skipped:
            r15 = r12;
            r3 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("Scan install , restore from :");
            r4.append(r10);
            r4.append(" success:");
            r4.append(r15);
            android.util.Slog.d(r3, r4.toString());
     */
    /* JADX WARNING: Missing block: B:81:0x01ed, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:82:0x01ef, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:83:0x01f0, code skipped:
            r19 = r8;
     */
    /* JADX WARNING: Missing block: B:85:?, code skipped:
            r1 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Scan install, failed to parse package: ");
            r3.append(r0.getMessage());
            android.util.Slog.e(r1, r3.toString());
     */
    /* JADX WARNING: Missing block: B:86:0x020c, code skipped:
            r1 = r15;
     */
    /* JADX WARNING: Missing block: B:87:0x020f, code skipped:
            monitor-enter(r9.mScanInstallApkList);
     */
    /* JADX WARNING: Missing block: B:90:0x0216, code skipped:
            if (r9.mScanInstallApkList.remove(r10) != false) goto L_0x0218;
     */
    /* JADX WARNING: Missing block: B:91:0x0218, code skipped:
            r0 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Scan install , remove from list:");
            r3.append(r10);
            android.util.Slog.i(r0, r3.toString());
     */
    /* JADX WARNING: Missing block: B:93:0x022f, code skipped:
            r15 = r1;
     */
    /* JADX WARNING: Missing block: B:98:0x0237, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:100:?, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Scan install ");
            r2.append(r10);
            r2.append(" failed!");
            r2.append(r0.getMessage());
            android.util.Slog.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:102:0x025c, code skipped:
            monitor-enter(r9.mScanInstallApkList);
     */
    /* JADX WARNING: Missing block: B:105:0x0263, code skipped:
            if (r9.mScanInstallApkList.remove(r10) != false) goto L_0x0265;
     */
    /* JADX WARNING: Missing block: B:106:0x0265, code skipped:
            r0 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Scan install , remove from list:");
            r2.append(r10);
            android.util.Slog.i(r0, r2.toString());
     */
    /* JADX WARNING: Missing block: B:109:0x027d, code skipped:
            return r15;
     */
    /* JADX WARNING: Missing block: B:114:0x0283, code skipped:
            monitor-enter(r9.mScanInstallApkList);
     */
    /* JADX WARNING: Missing block: B:117:0x028a, code skipped:
            if (r9.mScanInstallApkList.remove(r10) != false) goto L_0x028c;
     */
    /* JADX WARNING: Missing block: B:118:0x028c, code skipped:
            r1 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Scan install , remove from list:");
            r3.append(r10);
            android.util.Slog.i(r1, r3.toString());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean scanInstallApk(String packageName, String apkFile, int userId) {
        String str = apkFile;
        int i = userId;
        boolean z = false;
        String str2;
        if (!checkScanInstallCaller()) {
            Slog.w(TAG, "Scan install ,check caller failed!");
            return false;
        } else if (str == null || !isPreRemovableApp(str)) {
            str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal install apk file:");
            stringBuilder.append(str);
            Slog.d(str2, stringBuilder.toString());
            return false;
        } else {
            String pkgName = packageName;
            boolean z2 = true;
            if (TextUtils.isEmpty(packageName)) {
                Package pkg = null;
                try {
                    pkg = new PackageParser().parsePackage(new File(str), 0, true, 0);
                } catch (PackageParserException e) {
                    String str3 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Scan install ,parse ");
                    stringBuilder2.append(str);
                    stringBuilder2.append(" to get package name failed!");
                    stringBuilder2.append(e.getMessage());
                    Slog.w(str3, stringBuilder2.toString());
                }
                if (pkg == null) {
                    Slog.w(TAG, "Scan install ,get package name failed, pkg is null!");
                    return false;
                }
                pkgName = pkg.packageName;
            }
            String pkgName2 = pkgName;
            synchronized (this.mScanInstallApkList) {
                if (assertScanInstallApkLocked(pkgName2, str, i)) {
                    this.mScanInstallApkList.add(str);
                    str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Scan install , add to list:");
                    stringBuilder3.append(str);
                    Slog.i(str2, stringBuilder3.toString());
                } else {
                    return false;
                }
            }
        }
    }

    private boolean scanInstallApk(String apkFile) {
        return scanInstallApk(null, apkFile, UserHandle.getUserId(Binder.getCallingUid()));
    }

    protected void doPostScanInstall(Package pkg, UserHandle user, boolean isNewInstall, int hwFlags) {
        Package packageR = pkg;
        if ((hwFlags & 1073741824) != 0 && isPreRemovableApp(packageR.codePath)) {
            PackageInstalledInfo res = new PackageInstalledInfo();
            res.setReturnCode(1);
            res.uid = -1;
            int i = 0;
            if (isNewInstall) {
                res.origUsers = new int[]{user.getIdentifier()};
            } else {
                res.origUsers = this.mSettings.getPackageLPr(packageR.packageName).queryInstalledUsers(sUserManager.getUserIds(), true);
            }
            res.pkg = null;
            res.removedInfo = null;
            updateSettingsLI(packageR, GestureNavConst.DEFAULT_LAUNCHER_PACKAGE, sUserManager.getUserIds(), res, user, 4);
            prepareAppDataAfterInstallLIF(packageR);
            Bundle extras = new Bundle();
            extras.putInt("android.intent.extra.UID", packageR.applicationInfo != null ? packageR.applicationInfo.uid : 0);
            sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageR.packageName, extras, 0, null, null, new int[]{user.getIdentifier()}, null);
            PackageSetting psTemp = this.mSettings.getPackageLPr(packageR.packageName);
            if (psTemp != null) {
                int[] iUser = psTemp.queryInstalledUsers(sUserManager.getUserIds(), false);
                int countClonedUser = 0;
                while (true) {
                    int i2 = i;
                    if (i2 >= iUser.length) {
                        break;
                    }
                    if (getUserManagerInternal().isClonedProfile(iUser[i2])) {
                        countClonedUser++;
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(iUser[i2]);
                        stringBuilder.append(" skiped, it is cloned user when install package:");
                        stringBuilder.append(packageR.packageName);
                        Slog.d(str, stringBuilder.toString());
                    }
                    i = i2 + 1;
                }
                if (iUser.length == 0 || iUser.length == countClonedUser) {
                    removeFromUninstalledDelapp(packageR.packageName);
                }
            }
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Scan install done for package:");
            stringBuilder2.append(packageR.packageName);
            Slog.d(str2, stringBuilder2.toString());
        }
    }

    private void addGmsCoreApkToHashMap() {
        ((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV)).add(GMS_CORE_PATH);
        ((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV)).add(GMS_FWK_PATH);
        ((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV)).add(GMS_LOG_PATH);
    }

    protected static ArrayList<File> getApkInstallFileCfgList(String apkCfgFile) {
        String[] policyDir = null;
        ArrayList<File> allApkInstallList = new ArrayList();
        int i = 0;
        try {
            policyDir = HwCfgFilePolicy.getCfgPolicyDir(0);
            while (i < policyDir.length) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getApkInstallFileCfgList from custpolicy i=");
                stringBuilder.append(i);
                stringBuilder.append("| ");
                stringBuilder.append(policyDir[i]);
                Flog.i(205, stringBuilder.toString());
                i++;
            }
        } catch (NoClassDefFoundError e) {
            Slog.w(TAG, "HwCfgFilePolicy NoClassDefFoundError");
        }
        if (policyDir == null) {
            return null;
        }
        for (i = policyDir.length - 1; i >= 0; i--) {
            try {
                String canonicalPath = new File(policyDir[i]).getCanonicalPath();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getApkInstallFileCfgList canonicalPath:");
                stringBuilder2.append(canonicalPath);
                Flog.i(205, stringBuilder2.toString());
                File rawFileAddToList = adjustmccmncList(canonicalPath, apkCfgFile);
                if (rawFileAddToList != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("getApkInstallFileCfgList add File :");
                    stringBuilder3.append(rawFileAddToList.getPath());
                    Flog.i(205, stringBuilder3.toString());
                    allApkInstallList.add(rawFileAddToList);
                }
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("data/hw_init/");
                stringBuilder4.append(canonicalPath);
                File rawNewFileAddToList = adjustmccmncList(new File(stringBuilder4.toString()).getCanonicalPath(), apkCfgFile);
                if (rawNewFileAddToList != null) {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("getApkInstallFileCfgList add data File :");
                    stringBuilder5.append(rawNewFileAddToList.getPath());
                    Flog.i(205, stringBuilder5.toString());
                    allApkInstallList.add(rawNewFileAddToList);
                }
            } catch (IOException e2) {
                Slog.e(TAG, "Unable to obtain canonical paths");
            }
        }
        if (allApkInstallList.size() == 0) {
            String str = TAG;
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("No config file found for:");
            stringBuilder6.append(apkCfgFile);
            Log.w(str, stringBuilder6.toString());
        }
        return allApkInstallList;
    }

    private static File adjustmccmncList(String canonicalPath, String apkFile) {
        File adjustRetFile = null;
        try {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            if (mCustPackageManagerService == null || !mCustPackageManagerService.isMccMncMatch()) {
                String apkPath = new StringBuilder();
                apkPath.append(canonicalPath);
                apkPath.append("/");
                apkPath.append(apkFile);
                if (!new File(apkPath.toString()).exists()) {
                    return null;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(canonicalPath);
                stringBuilder.append("/");
                stringBuilder.append(apkFile);
                adjustRetFile = new File(stringBuilder.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("adjustRetFile :");
                stringBuilder2.append(adjustRetFile.getPath());
                Flog.i(205, stringBuilder2.toString());
                return adjustRetFile;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(canonicalPath);
            stringBuilder2.append("/");
            stringBuilder2.append(joinCustomizeFile(apkFile));
            File mccmncFile = new File(stringBuilder2.toString());
            if (mccmncFile.exists()) {
                adjustRetFile = new File(mccmncFile.getCanonicalPath());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("adjustRetFile mccmnc :");
                stringBuilder2.append(adjustRetFile.getPath());
                Flog.i(205, stringBuilder2.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(canonicalPath);
                stringBuilder.append("/");
                stringBuilder.append(apkFile);
                if (new File(stringBuilder.toString()).exists()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(canonicalPath);
                    stringBuilder.append("/");
                    stringBuilder.append(apkFile);
                    adjustRetFile = new File(stringBuilder.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("adjustRetFile :");
                    stringBuilder2.append(adjustRetFile.getPath());
                    Flog.i(205, stringBuilder2.toString());
                }
            }
            return adjustRetFile;
        } catch (IOException e) {
            Slog.e(TAG, "Unable to obtain canonical paths");
            return null;
        }
    }

    public static String joinCustomizeFile(String fileName) {
        String joinFileName = fileName;
        String mccmnc = SystemProperties.get("persist.sys.mccmnc", "");
        if (fileName == null) {
            return joinFileName;
        }
        String[] splitArray = fileName.split("\\.");
        if (splitArray.length != 2) {
            return joinFileName;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(splitArray[0]);
        stringBuilder.append(Constant.RESULT_SEPERATE);
        stringBuilder.append(mccmnc);
        stringBuilder.append(".");
        stringBuilder.append(splitArray[1]);
        return stringBuilder.toString();
    }

    void onRemoveUser(int userId) {
        if (userId >= 1) {
            deleteThemeUserFolder(userId);
        }
    }

    private void deleteThemeUserFolder(int userId) {
        CommandLineUtil.rm("system", getHwThemePathAsUser(userId));
    }

    protected void replaceSignatureIfNeeded(PackageSetting ps, Package pkg, boolean isBootScan, boolean isUpdate) {
        if (pkg != null && this.mCompatSettings != null) {
            String str;
            StringBuilder stringBuilder;
            Context context;
            StringBuilder stringBuilder2;
            if (!isBootScan) {
                synchronized (this.mIncompatiblePkg) {
                    if (this.mIncompatiblePkg.contains(pkg.packageName)) {
                        this.mIncompatiblePkg.remove(pkg.packageName);
                    }
                }
            }
            boolean needReplace = false;
            String packageSignType = null;
            if (isBootScan && ps != null) {
                synchronized (this.mPackages) {
                    Package compatPkg = this.mCompatSettings.getCompatPackage(pkg.packageName);
                    if (compatPkg != null && compatPkg.codePath.equals(ps.codePathString) && compatPkg.timeStamp == ps.timeStamp) {
                        needReplace = true;
                        packageSignType = compatPkg.certType;
                    }
                }
            }
            if (!needReplace && HwCertificationManager.isSupportHwCertification(pkg)) {
                switch (getHwCertificateType(pkg)) {
                    case 1:
                        packageSignType = "platform";
                        break;
                    case 2:
                        packageSignType = "testkey";
                        break;
                    case 3:
                        packageSignType = "shared";
                        break;
                    case 4:
                        packageSignType = "media";
                        break;
                }
                if (packageSignType != null) {
                    needReplace = true;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("CertCompat: system signature compat for hwcert package:");
                    stringBuilder.append(pkg.packageName);
                    stringBuilder.append(",type:");
                    stringBuilder.append(packageSignType);
                    Slog.i(str, stringBuilder.toString());
                }
            }
            boolean isSignedByOldSystemSignature = this.mCompatSettings.isOldSystemSignature(pkg.mSigningDetails.signatures);
            if (!needReplace && isSignedByOldSystemSignature && this.mCompatSettings.isWhiteListedApp(pkg)) {
                packageSignType = this.mCompatSettings.getOldSignTpye(pkg.mSigningDetails.signatures);
                needReplace = true;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CertCompat: system signature compat for whitelist package:");
                stringBuilder.append(pkg.packageName);
                stringBuilder.append(",type:");
                stringBuilder.append(packageSignType);
                Slog.i(str, stringBuilder.toString());
                if (!isBootScan) {
                    context = this.mContext;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("{package:");
                    stringBuilder2.append(pkg.packageName);
                    stringBuilder2.append(",version:");
                    stringBuilder2.append(pkg.mVersionCode);
                    stringBuilder2.append(",type:");
                    stringBuilder2.append(packageSignType);
                    stringBuilder2.append("}");
                    Flog.bdReport(context, CPUFeature.MSG_SET_CPUSETCONFIG_VR, stringBuilder2.toString());
                }
            }
            if (!needReplace && isSignedByOldSystemSignature && isBootScan && !this.mFoundCertCompatFile && isUpgrade()) {
                packageSignType = this.mCompatSettings.getOldSignTpye(pkg.mSigningDetails.signatures);
                needReplace = true;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CertCompat: system signature compat for OTA package:");
                stringBuilder.append(pkg.packageName);
                Slog.i(str, stringBuilder.toString());
            }
            if (needReplace) {
                replaceSignatureInner(ps, pkg, packageSignType);
            } else if (isSignedByOldSystemSignature && !isBootScan) {
                synchronized (this.mIncompatiblePkg) {
                    if (!this.mIncompatiblePkg.contains(pkg.packageName)) {
                        this.mIncompatiblePkg.add(pkg.packageName);
                    }
                }
                packageSignType = this.mCompatSettings.getOldSignTpye(pkg.mSigningDetails.signatures);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CertCompat: illegal system signature package:");
                stringBuilder.append(pkg.packageName);
                stringBuilder.append(",type:");
                stringBuilder.append(packageSignType);
                Slog.i(str, stringBuilder.toString());
                context = this.mContext;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("{package:");
                stringBuilder2.append(pkg.packageName);
                stringBuilder2.append(",version:");
                stringBuilder2.append(pkg.mVersionCode);
                stringBuilder2.append(",type:");
                stringBuilder2.append(packageSignType);
                stringBuilder2.append("}");
                Flog.bdReport(context, 124, stringBuilder2.toString());
            }
        }
    }

    private void replaceSignatureInner(PackageSetting ps, Package pkg, String signType) {
        if (signType != null && pkg != null && this.mCompatSettings != null) {
            Signature[] signs = this.mCompatSettings.getNewSign(signType);
            if (signs.length == 0) {
                Slog.e(TAG, "CertCompat: signs init fail");
                return;
            }
            SigningDetails newSignDetails = createNewSigningDetails(pkg.mSigningDetails, signs);
            setRealSigningDetails(pkg, pkg.mSigningDetails);
            pkg.mSigningDetails = newSignDetails;
            if (ps != null && ps.signatures.mSigningDetails.hasSignatures()) {
                ps.signatures.mSigningDetails = pkg.mSigningDetails;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CertCompat: CertCompatPackage:");
            stringBuilder.append(pkg.packageName);
            Slog.d(str, stringBuilder.toString());
        }
    }

    private SigningDetails createNewSigningDetails(SigningDetails orig, Signature[] newSigns) {
        return new SigningDetails(newSigns, orig.signatureSchemeVersion, orig.publicKeys, orig.pastSigningCertificates, orig.pastSigningCertificatesFlags);
    }

    protected void initCertCompatSettings() {
        Slog.i(TAG, "CertCompat: init CertCompatSettings");
        this.mCompatSettings = new CertCompatSettings();
        this.mFoundCertCompatFile = this.mCompatSettings.readCertCompatPackages();
    }

    protected void resetSharedUserSignaturesIfNeeded() {
        if (this.mCompatSettings != null && !this.mFoundCertCompatFile && isUpgrade()) {
            for (SharedUserSetting setting : this.mSettings.getAllSharedUsersLPw()) {
                if (this.mCompatSettings.isOldSystemSignature(setting.signatures.mSigningDetails.signatures)) {
                    setting.signatures.mSigningDetails = SigningDetails.UNKNOWN;
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CertCompat: SharedUser:");
                    stringBuilder.append(setting.name);
                    stringBuilder.append(" signature reset!");
                    Slog.i(str, stringBuilder.toString());
                }
            }
        }
    }

    @SuppressLint({"PreferForInArrayList"})
    protected void writeCertCompatPackages(boolean update) {
        if (this.mCompatSettings != null) {
            if (update) {
                Iterator it = new ArrayList(this.mCompatSettings.getALLCompatPackages()).iterator();
                while (it.hasNext()) {
                    Package pkg = (Package) it.next();
                    if (!(pkg == null || this.mPackages.containsKey(pkg.packageName))) {
                        this.mCompatSettings.removeCertCompatPackage(pkg.packageName);
                    }
                }
            }
            this.mCompatSettings.writeCertCompatPackages();
        }
    }

    protected void updateCertCompatPackage(Package pkg, PackageSetting ps) {
        if (pkg != null && this.mCompatSettings != null) {
            Signature[] realSign = getRealSignature(pkg);
            if (realSign == null || realSign.length == 0 || ps == null) {
                this.mCompatSettings.removeCertCompatPackage(pkg.applicationInfo.packageName);
            } else {
                this.mCompatSettings.insertCompatPackage(pkg.applicationInfo.packageName, ps);
            }
        }
    }

    protected boolean isSystemSignatureUpdated(Signature[] previous, Signature[] current) {
        if (this.mCompatSettings == null) {
            return false;
        }
        return this.mCompatSettings.isSystemSignatureUpdated(previous, current);
    }

    protected void sendIncompatibleNotificationIfNeeded(final String packageName) {
        synchronized (this.mIncompatiblePkg) {
            boolean update = false;
            boolean send = false;
            if (this.mIncompatiblePkg.contains(packageName)) {
                this.mIncompatiblePkg.remove(packageName);
                update = true;
                send = true;
            } else if (this.mIncompatNotificationList.contains(packageName)) {
                update = true;
            }
            if (update) {
                final boolean isSend = send;
                UiThread.getHandler().post(new Runnable() {
                    public void run() {
                        HwPackageManagerService.this.updateIncompatibleNotification(packageName, isSend);
                    }
                });
            }
        }
    }

    private void updateIncompatibleNotification(String packageName, boolean isSend) {
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
                }
                boolean cancelAll = false;
                synchronized (this.mPackages) {
                    boolean isSignedByOldSystemSignature = false;
                    Package pkgInfo = (Package) this.mPackages.get(packageName);
                    if (!(pkgInfo == null || this.mCompatSettings == null)) {
                        isSignedByOldSystemSignature = this.mCompatSettings.isOldSystemSignature(pkgInfo.mSigningDetails.signatures);
                    }
                    if (pkgInfo == null || !isSignedByOldSystemSignature) {
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
                    if (cancelAll || !isPackageAvailable(packageName, id)) {
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CertCompat: cancel incompatible notification for u");
            stringBuilder.append(userId);
            stringBuilder.append(", packageName:");
            stringBuilder.append(packageName);
            Slog.d(str, stringBuilder.toString());
        }
    }

    private void sendIncompatibleNotificationInner(String packageName, int userId) {
        String str = packageName;
        int i = userId;
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CertCompat: send incompatible notification to u");
        stringBuilder.append(i);
        stringBuilder.append(", packageName:");
        stringBuilder.append(str);
        Slog.d(str2, stringBuilder.toString());
        PackageManager pm = this.mContext.getPackageManager();
        if (pm != null) {
            ApplicationInfo info = null;
            try {
                info = pm.getApplicationInfoAsUser(str, 0, i);
                Drawable icon = pm.getApplicationIcon(info);
                CharSequence title = pm.getApplicationLabel(info);
                String text = this.mContext.getString(33685898);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("package:");
                stringBuilder2.append(str);
                PendingIntent pi = PendingIntent.getActivityAsUser(this.mContext, 0, new Intent("android.intent.action.UNINSTALL_PACKAGE", Uri.parse(stringBuilder2.toString())), 0, null, new UserHandle(i));
                if (pi == null) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("CertCompat: Get PendingIntent fail, package: ");
                    stringBuilder3.append(str);
                    Slog.w(str3, stringBuilder3.toString());
                    return;
                }
                Notification notification = new Builder(this.mContext, SystemNotificationChannels.ALERTS).setLargeIcon(UserIcons.convertToBitmap(icon)).setSmallIcon(17301642).setContentTitle(title).setContentText(text).setContentIntent(pi).setDefaults(2).setPriority(2).setWhen(System.currentTimeMillis()).setShowWhen(true).setAutoCancel(true).addAction(new Action.Builder(null, this.mContext.getString(33685899), pi).build()).build();
                NotificationManager nm = (NotificationManager) this.mContext.getSystemService("notification");
                if (nm != null) {
                    nm.notifyAsUser(str, 33685898, notification, new UserHandle(i));
                }
            } catch (NameNotFoundException e) {
                NameNotFoundException nameNotFoundException = e;
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("CertCompat: incompatible package: ");
                stringBuilder4.append(str);
                stringBuilder4.append(" not find for u");
                stringBuilder4.append(i);
                Slog.w(str4, stringBuilder4.toString());
            }
        }
    }

    protected static void createPackagesAbiFile() {
        mPackagesAbi = new ArrayMap();
        try {
            mSystemDir = new File(Environment.getDataDirectory(), "system");
            if (mSystemDir.exists() || mSystemDir.mkdirs()) {
                mPakcageAbiFilename = new File(mSystemDir, "packages-abi.xml");
            } else {
                Slog.i(TAG, "Packages-abi file create error");
            }
        } catch (SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Packages-abi file SecurityException: ");
            stringBuilder.append(e.getMessage());
            Slog.i(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x006b A:{SYNTHETIC, Splitter:B:17:0x006b} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0034 A:{Catch:{ XmlPullParserException -> 0x0153, IOException -> 0x0129, all -> 0x0127 }} */
    /* JADX WARNING: Missing block: B:53:?, code skipped:
            r0.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void readPackagesAbiLPw() {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        FileInputStream str2 = null;
        if (mPakcageAbiFilename.exists()) {
            try {
                int type;
                str2 = new FileInputStream(mPakcageAbiFilename);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(str2, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type == 2) {
                            mReadMessages.append("No start tag found in settings file\n");
                            PackageManagerService.reportSettingsProblem(5, "No start tag found in package manager settings");
                            Slog.wtf("PackageManager", "No start tag found in package manager settings");
                            try {
                                str2.close();
                            } catch (IOException e2) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("IO error when closing packages-abi file:");
                                stringBuilder2.append(e2.getMessage());
                                Slog.e("PackageManager", stringBuilder2.toString());
                            }
                            return;
                        }
                        next = parser.getDepth();
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                                try {
                                    break;
                                } catch (IOException e3) {
                                    e = e3;
                                    str = "PackageManager";
                                    stringBuilder = new StringBuilder();
                                }
                            } else if (type != 3) {
                                if (type != 4) {
                                    if (parser.getName().equals("package")) {
                                        String name = parser.getAttributeValue(null, "name");
                                        String abiCode = parser.getAttributeValue(null, "abiCode");
                                        String version = parser.getAttributeValue(null, "version");
                                        if (TextUtils.isEmpty(name)) {
                                            Slog.wtf("PackageManager", "Error in package abi file: pakcage name is null");
                                        }
                                        int code = -1;
                                        if (abiCode != null) {
                                            try {
                                                code = Integer.parseInt(abiCode);
                                            } catch (NumberFormatException e4) {
                                                String str3 = TAG;
                                                StringBuilder stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("read package: ");
                                                stringBuilder3.append(name);
                                                stringBuilder3.append(" abi code error.");
                                                Slog.i(str3, stringBuilder3.toString());
                                            }
                                        }
                                        int versionCode = 0;
                                        if (!TextUtils.isEmpty(version)) {
                                            try {
                                                versionCode = Integer.parseInt(version);
                                            } catch (NumberFormatException e5) {
                                                String str4 = TAG;
                                                StringBuilder stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("read package: ");
                                                stringBuilder4.append(version);
                                                stringBuilder4.append(" abi code error.");
                                                Slog.i(str4, stringBuilder4.toString());
                                            }
                                        }
                                        addPackagesAbiLPw(name, code, true, versionCode);
                                    }
                                }
                            }
                        }
                    }
                }
                if (type == 2) {
                }
            } catch (XmlPullParserException e6) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("XML parser error:");
                stringBuilder.append(e6.getMessage());
                Slog.e("PackageManager", stringBuilder.toString());
                mPackagesAbi.clear();
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e7) {
                        e = e7;
                        str = "PackageManager";
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (IOException e8) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error reading package manager settings file:");
                stringBuilder.append(e8.getMessage());
                Slog.e("PackageManager", stringBuilder.toString());
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e9) {
                        e8 = e9;
                        str = "PackageManager";
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e22) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("IO error when closing packages-abi file:");
                        stringBuilder.append(e22.getMessage());
                        Slog.e("PackageManager", stringBuilder.toString());
                    }
                }
            }
        } else {
            Slog.i(TAG, "PakcageAbiFilename isn't exists");
            return;
        }
        stringBuilder.append("IO error when closing packages-abi file:");
        stringBuilder.append(e8.getMessage());
        Slog.e(str, stringBuilder.toString());
    }

    protected void writePackagesAbi() {
        IOException e;
        StringBuilder stringBuilder;
        String str;
        StringBuilder stringBuilder2;
        FileOutputStream fstr = null;
        BufferedOutputStream str2 = null;
        if (mPackagesAbi != null) {
            try {
                fstr = new FileOutputStream(mPakcageAbiFilename);
                str2 = new BufferedOutputStream(fstr);
                XmlSerializer serializer = new FastXmlSerializer();
                serializer.setOutput(str2, StandardCharsets.UTF_8.name());
                serializer.startDocument(null, Boolean.valueOf(true));
                serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                serializer.startTag(null, HwSecDiagnoseConstant.MALAPP_APK_PACKAGES);
                for (AbiInfo info : mPackagesAbi.values()) {
                    serializer.startTag(null, "package");
                    serializer.attribute(null, "name", info.name);
                    serializer.attribute(null, "abiCode", Integer.toString(info.abiCode));
                    serializer.attribute(null, "version", Integer.toString(info.version));
                    serializer.endTag(null, "package");
                }
                serializer.endTag(null, HwSecDiagnoseConstant.MALAPP_APK_PACKAGES);
                serializer.endDocument();
                str2.flush();
                FileUtils.sync(fstr);
                try {
                    str2.close();
                    fstr.close();
                    FileUtils.setPermissions(mPakcageAbiFilename.toString(), 432, -1, -1);
                } catch (IOException e2) {
                    e = e2;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error close writing settings: ");
                    stringBuilder.append(e);
                    PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                    str = "PackageManager";
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error close writing package manager settings");
                    stringBuilder.append(e.getMessage());
                    Slog.e(str, stringBuilder.toString());
                }
            } catch (FileNotFoundException e3) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("File not found when writing packages-abi file: ");
                stringBuilder2.append(e3.getMessage());
                Slog.e("PackageManager", stringBuilder2.toString());
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e4) {
                        e = e4;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing settings: ");
                        stringBuilder.append(e);
                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                        str = "PackageManager";
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing package manager settings");
                        stringBuilder.append(e.getMessage());
                        Slog.e(str, stringBuilder.toString());
                    }
                }
                if (fstr != null) {
                    fstr.close();
                }
                FileUtils.setPermissions(mPakcageAbiFilename.toString(), 432, -1, -1);
            } catch (IllegalArgumentException e5) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IllegalArgument when writing packages-abi file: ");
                stringBuilder2.append(e5.getMessage());
                Slog.e("PackageManager", stringBuilder2.toString());
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e6) {
                        e = e6;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing settings: ");
                        stringBuilder.append(e);
                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                        str = "PackageManager";
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing package manager settings");
                        stringBuilder.append(e.getMessage());
                        Slog.e(str, stringBuilder.toString());
                    }
                }
                if (fstr != null) {
                    fstr.close();
                }
                FileUtils.setPermissions(mPakcageAbiFilename.toString(), 432, -1, -1);
            } catch (IOException e7) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("IOException when writing packages-abi settings: ");
                stringBuilder3.append(e7);
                PackageManagerService.reportSettingsProblem(6, stringBuilder3.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IOException when writing packages-abi file: ");
                stringBuilder2.append(e7.getMessage());
                Slog.e("PackageManager", stringBuilder2.toString());
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e8) {
                        e = e8;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing settings: ");
                        stringBuilder.append(e);
                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                        str = "PackageManager";
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing package manager settings");
                        stringBuilder.append(e.getMessage());
                        Slog.e(str, stringBuilder.toString());
                    }
                }
                if (fstr != null) {
                    fstr.close();
                }
                FileUtils.setPermissions(mPakcageAbiFilename.toString(), 432, -1, -1);
            } catch (Throwable th) {
                if (str2 != null) {
                    try {
                        str2.close();
                    } catch (IOException e9) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error close writing settings: ");
                        stringBuilder.append(e9);
                        PackageManagerService.reportSettingsProblem(6, stringBuilder.toString());
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Error close writing package manager settings");
                        stringBuilder4.append(e9.getMessage());
                        Slog.e("PackageManager", stringBuilder4.toString());
                    }
                }
                if (fstr != null) {
                    fstr.close();
                }
                FileUtils.setPermissions(mPakcageAbiFilename.toString(), 432, -1, -1);
            }
        }
    }

    protected int getPackagesAbi(String name) {
        int i = -1000;
        if (mPackagesAbi == null) {
            return -1000;
        }
        AbiInfo info = (AbiInfo) mPackagesAbi.get(name);
        if (info != null) {
            i = info.getAbiCode();
        }
        return i;
    }

    protected int getPackageVersion(String name) {
        int i = 0;
        if (mPackagesAbi == null) {
            return 0;
        }
        AbiInfo info = (AbiInfo) mPackagesAbi.get(name);
        if (info != null) {
            i = info.getVersion();
        }
        return i;
    }

    protected void removePackageAbiLPw(String name) {
        if (mPackagesAbi != null) {
            mPackagesAbi.remove(name);
        }
    }

    protected void addPackagesAbiLPw(String name, int code, boolean flag, int versionCode) {
        if (mPackagesAbi != null) {
            AbiInfo info = (AbiInfo) mPackagesAbi.get(name);
            if (info != null) {
                if (flag) {
                    int index = mPackagesAbi.indexOfKey(name);
                    info.setVersion(versionCode);
                    if (index >= 0) {
                        mPackagesAbi.setValueAt(index, info);
                    }
                }
                return;
            }
            mPackagesAbi.put(name, new AbiInfo(name, code, versionCode));
        }
    }

    @FindBugsSuppressWarnings({"UC_USELESS_CONDITION"})
    protected void readLastedAbi(Package pkg, File scanFile, String cpuAbiOverride) throws PackageManagerException {
        setNativeLibraryPaths(pkg, sAppLib32InstallDir);
        int copyRet = 0;
        Handle handle = null;
        try {
            handle = Handle.create(scanFile);
            String[] abiList = cpuAbiOverride != null ? new String[]{cpuAbiOverride} : Build.SUPPORTED_ABIS;
            boolean needsRenderScriptOverride = false;
            if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && cpuAbiOverride == null && NativeLibraryHelper.hasRenderscriptBitcode(handle)) {
                abiList = Build.SUPPORTED_32_BIT_ABIS;
                needsRenderScriptOverride = true;
            }
            if (this.mSettings != null) {
                copyRet = getPackagesAbi(pkg.packageName);
            }
            if (copyRet < 0) {
                if (copyRet != -114) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error unpackaging native libs for app, errorCode=");
                    stringBuilder.append(copyRet);
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, stringBuilder.toString());
                }
            }
            if (copyRet >= 0) {
                pkg.applicationInfo.primaryCpuAbi = abiList[copyRet];
            } else if (copyRet == -114 && cpuAbiOverride != null) {
                pkg.applicationInfo.primaryCpuAbi = cpuAbiOverride;
            } else if (needsRenderScriptOverride) {
                pkg.applicationInfo.primaryCpuAbi = abiList[0];
            }
            setNativeLibraryPaths(pkg, sAppLib32InstallDir);
        } catch (IOException ioe) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to get canonical file ");
            stringBuilder2.append(ioe.toString());
            Slog.e(str, stringBuilder2.toString());
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(handle);
    }

    protected boolean isPackageAbiRestored(String name) {
        boolean z = false;
        if (mPackagesAbi == null) {
            return false;
        }
        AbiInfo info = (AbiInfo) mPackagesAbi.get(name);
        if (!(info == null || info.getAbiCode() == -1000)) {
            z = true;
        }
        return z;
    }

    protected void deletePackagesAbiFile() {
        if (mPackagesAbi != null) {
            mPackagesAbi.clear();
        }
        try {
            File mSystemDir = new File(Environment.getDataDirectory(), "system");
            if (mSystemDir.exists()) {
                File abiConfigfile = new File(mSystemDir, "packages-abi.xml");
                if (abiConfigfile.exists() && !abiConfigfile.delete()) {
                    Slog.i(TAG, "Packages-abi file delete error.");
                }
            }
        } catch (SecurityException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Packages-abi file SecurityException: ");
            stringBuilder.append(e.getMessage());
            Slog.i(str, stringBuilder.toString());
        }
    }

    protected boolean isPackagePathWithNoSysFlag(File filePath) {
        HashSet<String> delInstallSet;
        if (!(filePath == null || mDelMultiInstallMap == null)) {
            delInstallSet = (HashSet) mDelMultiInstallMap.get(FLAG_APK_NOSYS);
            if (!(delInstallSet == null || delInstallSet.isEmpty() || !delInstallSet.contains(filePath.getPath()))) {
                return true;
            }
        }
        if (!(filePath == null || mCotaDelInstallMap == null)) {
            delInstallSet = (HashSet) mCotaDelInstallMap.get(FLAG_APK_NOSYS);
            if (!(delInstallSet == null || delInstallSet.isEmpty() || !delInstallSet.contains(filePath.getPath()))) {
                return true;
            }
        }
        return false;
    }

    private boolean checkLimitePackageBroadcast(String action, String pkg, String targetPkg) {
        String[] callingPkgNames = getPackagesForUid(Binder.getCallingUid());
        if (callingPkgNames == null || callingPkgNames.length <= 0) {
            Flog.i(205, "Android Wear-checkLimitePackageBroadcast: callingPkgNames is empty");
            return false;
        }
        String callingPkgName = callingPkgNames[0];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Android Wear-checkLimitePackageBroadcast: callingPkgName = ");
        stringBuilder.append(callingPkgName);
        Flog.d(205, stringBuilder.toString());
        if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_REMOVED".equals(action)) {
            boolean targetPkgExist = false;
            for (String name : LIMITED_TARGET_PACKAGE_NAMES) {
                if (name.equals(targetPkg)) {
                    targetPkgExist = true;
                    break;
                }
            }
            if (targetPkgExist) {
                boolean pkgExist = false;
                for (String name2 : LIMITED_PACKAGE_NAMES) {
                    if (name2.equals(pkg)) {
                        pkgExist = true;
                        break;
                    }
                }
                if (!pkgExist) {
                    Flog.i(205, "Android Wear-checkLimitePackageBroadcast: pkg is not permitted");
                    return false;
                } else if (isSystemApp(getApplicationInfo(callingPkgName, 0, this.mContext.getUserId()))) {
                    Flog.d(205, "Android Wear-checkLimitePackageBroadcast: success");
                    return true;
                } else {
                    Flog.i(205, "Android Wear-checkLimitePackageBroadcast: is not System App.");
                    return false;
                }
            }
            Flog.i(205, "Android Wear-checkLimitePackageBroadcast: targetPkg is not permitted");
            return false;
        }
        Flog.i(205, "Android Wear-checkLimitePackageBroadcast: action is not permitted");
        return false;
    }

    private boolean isSystemApp(ApplicationInfo appInfo) {
        boolean bSystemApp = false;
        if (appInfo != null) {
            boolean z = true;
            if ((appInfo.flags & 1) == 0) {
                z = false;
            }
            bSystemApp = z;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Android Wear-checkLimitePackageBroadcast: bSystemApp=");
        stringBuilder.append(bSystemApp);
        Flog.d(205, stringBuilder.toString());
        return bSystemApp;
    }

    private void sendLimitedPackageBroadcast(String action, String pkg, Bundle extras, String targetPkg, int[] userIds) {
        if (checkLimitePackageBroadcast(action, pkg, targetPkg)) {
            final int[] iArr = userIds;
            final String str = action;
            final String str2 = pkg;
            final Bundle bundle = extras;
            final String str3 = targetPkg;
            this.mHandler.post(new Runnable() {
                public void run() {
                    try {
                        IActivityManager am = ActivityManagerNative.getDefault();
                        if (am != null) {
                            int[] resolvedUserIds;
                            if (iArr == null) {
                                resolvedUserIds = am.getRunningUserIds();
                            } else {
                                resolvedUserIds = iArr;
                            }
                            int[] resolvedUserIds2 = resolvedUserIds;
                            int length = resolvedUserIds2.length;
                            int i = 0;
                            while (i < length) {
                                int id = resolvedUserIds2[i];
                                String str = str;
                                Uri uri = null;
                                if (str2 != null) {
                                    uri = Uri.fromParts("package", str2, null);
                                }
                                Intent intent = new Intent(str, uri);
                                if (bundle != null) {
                                    intent.putExtras(bundle);
                                }
                                if (str3 != null) {
                                    intent.setPackage(str3);
                                }
                                int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                                if (uid > 0 && UserHandle.getUserId(uid) != id) {
                                    uid = UserHandle.getUid(id, UserHandle.getAppId(uid));
                                    intent.putExtra("android.intent.extra.UID", uid);
                                }
                                intent.putExtra("android.intent.extra.user_handle", id);
                                int i2 = i;
                                int i3 = length;
                                int[] resolvedUserIds3 = resolvedUserIds2;
                                am.broadcastIntent(null, intent, null, null, 0, null, null, null, -1, null, false, false, id);
                                i = i2 + 1;
                                length = i3;
                                resolvedUserIds2 = resolvedUserIds3;
                            }
                        }
                    } catch (RemoteException e) {
                    }
                }
            });
            return;
        }
        throw new SecurityException("sendLimitedPackageBroadcast: checkLimitePackageBroadcast failed");
    }

    protected void computeMetaHash(Package pkg) {
        this.pkgMetaHash.put(pkg.packageName, getSHA256(pkg));
    }

    private String getSHA256(Package pkg) {
        StrictJarFile jarFile = null;
        try {
            jarFile = new StrictJarFile(pkg.baseCodePath, false, false);
            ZipEntry ze = jarFile.findEntry("META-INF/MANIFEST.MF");
            if (ze != null) {
                String sha = getSHA(Streams.readFully(jarFile.getInputStream(ze)));
                try {
                    jarFile.close();
                } catch (IOException e) {
                    Log.w(TAG, "close jar file counter exception!");
                }
                return sha;
            }
            Log.d(TAG, "ZipEntry is null");
            try {
                jarFile.close();
            } catch (IOException e2) {
                Log.w(TAG, "close jar file counter exception!");
            }
            return "";
        } catch (IOException e3) {
            if (jarFile != null) {
                jarFile.close();
            }
        } catch (Throwable th) {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e4) {
                    Log.w(TAG, "close jar file counter exception!");
                }
            }
        }
    }

    private String getSHA(byte[] manifest) {
        StringBuffer output = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(manifest);
            byte[] b = md.digest();
            for (byte b2 : b) {
                String temp = Integer.toHexString(b2 & 255);
                if (temp.length() < 2) {
                    output.append("0");
                }
                output.append(temp);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "get sha256 failed");
        }
        return output.toString();
    }

    protected void recordInstallAppInfo(String pkgName, long beginTime, int installFlags) {
        long endTime = SystemClock.elapsedRealtime();
        int srcPkg = 0;
        if ((installFlags & 32) != 0) {
            srcPkg = 1;
        }
        insertAppInfo(pkgName, srcPkg, beginTime, endTime);
    }

    public void insertAppInfo(String pkgName, int srcPkg, long beginTime, long endTime) {
        if (TextUtils.isEmpty(pkgName)) {
            Slog.e(TAG, "insertAppInfo pkgName is null");
        } else if (this.needCollectAppInfo) {
            try {
                IHoldService service = StubController.getHoldService();
                if (service == null) {
                    Slog.e(TAG, "insertAppInfo getHoldService is null.");
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putString("pkg", pkgName);
                bundle.putInt(SOURCE_PACKAGE_NAME, srcPkg);
                bundle.putLong(INSTALL_BEGIN, beginTime);
                bundle.putLong(INSTALL_END, endTime);
                Bundle res = service.callHsmService(ANTIMAL_MODULE, bundle);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("insertAppInfo pkgName:");
                stringBuilder.append(pkgName);
                stringBuilder.append(" time:");
                stringBuilder.append(SystemClock.elapsedRealtime());
                Slog.i(str, stringBuilder.toString());
                if (res != null && res.getInt(INSERT_RESULT) != 0) {
                    this.needCollectAppInfo = false;
                }
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("insertAppInfo EXCEPTION = ");
                stringBuilder2.append(e);
                Slog.e(str2, stringBuilder2.toString());
            }
        } else {
            Slog.i(TAG, "AntiMalware is closed");
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0023, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void addPreinstalledPkgToList(Package scannedPkg) {
        if (!(scannedPkg == null || scannedPkg.baseCodePath == null || scannedPkg.baseCodePath.startsWith("/data/app/") || scannedPkg.baseCodePath.startsWith("/data/app-private/"))) {
            preinstalledPackageList.add(scannedPkg.packageName);
        }
    }

    /* JADX WARNING: Missing block: B:28:?, code skipped:
            r3.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected List<String> getPreinstalledApkList() {
        String str;
        StringBuilder stringBuilder;
        List<String> preinstalledApkList = new ArrayList();
        File preinstalledApkFile = new File("/data/system/", "preinstalled_app_list_file.xml");
        FileInputStream stream = null;
        try {
            int type;
            String tag;
            stream = new FileInputStream(preinstalledApkFile);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, null);
            while (true) {
                int next = parser.next();
                type = next;
                if (next == 1 || type == 2) {
                    tag = parser.getName();
                }
            }
            tag = parser.getName();
            if ("values".equals(tag)) {
                parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int next2 = parser.next();
                    type = next2;
                    if (next2 == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                        try {
                            break;
                        } catch (IOException e) {
                        }
                    } else if (type != 3) {
                        if (type != 4) {
                            if ("string".equals(parser.getName()) && parser.getAttributeValue(1) != null) {
                                preinstalledApkList.add(parser.getAttributeValue(1));
                            }
                        }
                    }
                }
                return preinstalledApkList;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Settings do not start with policies tag: found ");
            stringBuilder2.append(tag);
            throw new XmlPullParserException(stringBuilder2.toString());
        } catch (FileNotFoundException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("file is not exist ");
            stringBuilder.append(e2.getMessage());
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (XmlPullParserException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(preinstalledApkFile);
            stringBuilder.append(" ");
            stringBuilder.append(e3.getMessage());
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(preinstalledApkFile);
            stringBuilder.append(" ");
            stringBuilder.append(e4.getMessage());
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e5) {
                }
            }
        }
    }

    protected void writePreinstalledApkListToFile() {
        String str;
        StringBuilder stringBuilder;
        File preinstalledApkFile = new File("/data/system/", "preinstalled_app_list_file.xml");
        if (!preinstalledApkFile.exists()) {
            FileOutputStream stream = null;
            try {
                if (preinstalledApkFile.createNewFile()) {
                    FileUtils.setPermissions(preinstalledApkFile.getPath(), 416, -1, -1);
                }
                int i = 0;
                stream = new FileOutputStream(preinstalledApkFile, false);
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, "utf-8");
                out.startDocument(null, Boolean.valueOf(true));
                out.startTag(null, "values");
                int N = preinstalledPackageList.size();
                PackageManager pm = this.mContext.getPackageManager();
                while (i < N) {
                    String packageName = (String) preinstalledPackageList.get(i);
                    String apkName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 128)).toString();
                    out.startTag(null, "string");
                    out.attribute(null, "name", packageName);
                    out.attribute(null, HwSecDiagnoseConstant.MALAPP_APK_NAME, apkName);
                    out.endTag(null, "string");
                    i++;
                }
                out.endTag(null, "values");
                out.endDocument();
                try {
                    stream.close();
                } catch (IOException e) {
                }
            } catch (NameNotFoundException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(preinstalledApkFile);
                stringBuilder.append(" ");
                stringBuilder.append(e2.getMessage());
                Slog.w(str, stringBuilder.toString());
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed parsing ");
                stringBuilder.append(preinstalledApkFile);
                stringBuilder.append(" ");
                stringBuilder.append(e3.getMessage());
                Slog.w(str, stringBuilder.toString());
                if (stream != null) {
                    stream.close();
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                    }
                }
            }
        }
    }

    public boolean checkIllegalGmsCoreApk(Package pkg) {
        if (checkGmsCoreByInstaller(pkg)) {
            if (PackageManagerServiceUtils.compareSignatures(new Signature[]{new Signature("308204433082032ba003020102020900c2e08746644a308d300d06092a864886f70d01010405003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3038303832313233313333345a170d3336303130373233313333345a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820120300d06092a864886f70d01010105000382010d00308201080282010100ab562e00d83ba208ae0a966f124e29da11f2ab56d08f58e2cca91303e9b754d372f640a71b1dcb130967624e4656a7776a92193db2e5bfb724a91e77188b0e6a47a43b33d9609b77183145ccdf7b2e586674c9e1565b1f4c6a5955bff251a63dabf9c55c27222252e875e4f8154a645f897168c0b1bfc612eabf785769bb34aa7984dc7e2ea2764cae8307d8c17154d7ee5f64a51a44a602c249054157dc02cd5f5c0e55fbef8519fbe327f0b1511692c5a06f19d18385f5c4dbc2d6b93f68cc2979c70e18ab93866b3bd5db8999552a0e3b4c99df58fb918bedc182ba35e003c1b4b10dd244a8ee24fffd333872ab5221985edab0fc0d0b145b6aa192858e79020103a381d93081d6301d0603551d0e04160414c77d8cc2211756259a7fd382df6be398e4d786a53081a60603551d2304819e30819b8014c77d8cc2211756259a7fd382df6be398e4d786a5a178a4763074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964820900c2e08746644a308d300c0603551d13040530030101ff300d06092a864886f70d010104050003820101006dd252ceef85302c360aaace939bcff2cca904bb5d7a1661f8ae46b2994204d0ff4a68c7ed1a531ec4595a623ce60763b167297a7ae35712c407f208f0cb109429124d7b106219c084ca3eb3f9ad5fb871ef92269a8be28bf16d44c8d9a08e6cb2f005bb3fe2cb96447e868e731076ad45b33f6009ea19c161e62641aa99271dfd5228c5c587875ddb7f452758d661f6cc0cccb7352e424cc4365c523532f7325137593c4ae341f4db41edda0d0b1071a7c440f0fe9ea01cb627ca674369d084bd2fd911ff06cdbf2cfa10dc0f893ae35762919048c7efc64c7144178342f70581c9de573af55b390dd7fdb9418631895d5f759f30112687ff621410c069308a")}, pkg.mSigningDetails.signatures) != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("GmsCore signature not match: ");
                stringBuilder.append(pkg.packageName);
                Slog.e(str, stringBuilder.toString());
                return true;
            }
            Slog.d(TAG, "GmsCore signature match");
        }
        return false;
    }

    private boolean checkGmsCoreByInstaller(Package pkg) {
        if (LocationManagerServiceUtil.GOOGLE_GMS_PROCESS.equals(pkg.applicationInfo.packageName) && "/data/hw_init/system/app/GmsCore/GmsCore.apk".equals(pkg.baseCodePath)) {
            return true;
        }
        if ("com.google.android.gsf".equals(pkg.applicationInfo.packageName) && "/data/hw_init/system/app/GoogleServicesFramework/GoogleServicesFramework.apk".equals(pkg.baseCodePath)) {
            return true;
        }
        if ("com.google.android.gsf.login".equals(pkg.applicationInfo.packageName) && "/data/hw_init/system/app/GoogleLoginService/GoogleLoginService.apk".equals(pkg.baseCodePath)) {
            return true;
        }
        return false;
    }

    private boolean checkGmsCoreUninstalled() {
        if (isUninstalledDelapp(LocationManagerServiceUtil.GOOGLE_GMS_PROCESS) || isUninstalledDelapp("com.google.android.gsf.login") || isUninstalledDelapp("com.google.android.gsf")) {
            return true;
        }
        return false;
    }

    private void deleteGmsCoreFromUninstalledDelapp() {
        String str;
        StringBuilder stringBuilder;
        File file = new File("/data/system/", "uninstalled_delapp.xml");
        loadUninstalledDelapp(file);
        FileOutputStream stream = null;
        try {
            int i = 0;
            stream = new FileOutputStream(file, false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, "utf-8");
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, "values");
            int N = mUninstalledDelappList.size();
            while (i < N) {
                if (!(LocationManagerServiceUtil.GOOGLE_GMS_PROCESS.equals(mUninstalledDelappList.get(i)) || "com.google.android.gsf.login".equals(mUninstalledDelappList.get(i)))) {
                    if (!"com.google.android.gsf".equals(mUninstalledDelappList.get(i))) {
                        out.startTag(null, "string");
                        out.attribute(null, "name", (String) mUninstalledDelappList.get(i));
                        out.endTag(null, "string");
                        i++;
                    }
                }
                Slog.d(TAG, "GmsCore no need write to file");
                i++;
            }
            out.endTag(null, "values");
            out.endDocument();
            try {
                stream.close();
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e2);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Exception e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("failed parsing ");
            stringBuilder.append(file);
            stringBuilder.append(" ");
            stringBuilder.append(e3);
            Slog.w(str, stringBuilder.toString());
            if (stream != null) {
                stream.close();
            }
        } catch (Throwable th) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    protected boolean isMDMDisallowedInstallPackage(Package pkg, PackageInstalledInfo res) {
        if (!HwDeviceManager.disallowOp(19, pkg.packageName)) {
            return false;
        }
        final String pkgName1 = getCallingAppName(this.mContext, pkg);
        new Handler().postDelayed(new Runnable() {
            public void run() {
                Toast.makeText(HwPackageManagerService.this.mContext, HwPackageManagerService.this.mContext.getResources().getString(33685933, new Object[]{pkgName1}), 0).show();
            }
        }, 500);
        res.setError(RequestStatus.SYS_ETIMEDOUT, "app is in the installpackage_blacklist");
        return true;
    }

    public void updateFlagsForMarketSystemApp(Package pkg) {
        HwPackageManagerServiceUtils.updateFlagsForMarketSystemApp(pkg);
    }

    public int checkPermission(String permName, String pkgName, int userId) {
        if (userId != 0 && HwActivityManagerService.IS_SUPPORT_CLONE_APP && (("android.permission.INTERACT_ACROSS_USERS_FULL".equals(permName) || "android.permission.INTERACT_ACROSS_USERS".equals(permName)) && sUserManager.isClonedProfile(userId))) {
            return 0;
        }
        return super.checkPermission(permName, pkgName, userId);
    }

    public int checkUidPermission(String permName, int uid) {
        if (UserHandle.getUserId(uid) != 0 && HwActivityManagerService.IS_SUPPORT_CLONE_APP && sUserManager.isClonedProfile(UserHandle.getUserId(uid)) && ("android.permission.INTERACT_ACROSS_USERS_FULL".equals(permName) || "android.permission.INTERACT_ACROSS_USERS".equals(permName))) {
            return 0;
        }
        return super.checkUidPermission(permName, uid);
    }

    protected void deleteNonRequiredAppsForClone(int clonedProfileUserId) {
        Set<String> requiredAppsSet;
        String pkg;
        Throwable th;
        int i = clonedProfileUserId;
        deleteNonRequiredComponentsForClone(this, clonedProfileUserId);
        String[] disallowedAppsList = this.mContext.getResources().getStringArray(33816585);
        String[] requiredAppsList = this.mContext.getResources().getStringArray(33816586);
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        ParceledListSlice<ResolveInfo> parceledList = queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 795136, i);
        HashSet<String> packagesToDelete = new HashSet();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Secure.getStringForUser(this.mContext.getContentResolver(), "clone_app_list", 0));
        stringBuilder.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
        String cloneAppList = stringBuilder.toString();
        if (parceledList != null) {
            requiredAppsSet = new HashSet(Arrays.asList(requiredAppsList));
            for (ResolveInfo resolveInfo : parceledList.getList()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(resolveInfo.activityInfo.packageName);
                stringBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                if (!cloneAppList.contains(stringBuilder2.toString())) {
                    if (requiredAppsSet.contains(resolveInfo.activityInfo.packageName)) {
                        setComponentEnabledSetting(resolveInfo.getComponentInfo().getComponentName(), 2, 1, i);
                    }
                    packagesToDelete.add(resolveInfo.activityInfo.packageName);
                }
            }
        }
        Intent homeIntent = new Intent("android.intent.action.MAIN");
        homeIntent.addCategory("android.intent.category.HOME");
        ParceledListSlice<ResolveInfo> homeList = queryIntentActivities(homeIntent, homeIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 795136, i);
        if (homeList != null) {
            requiredAppsSet = new HashSet(Arrays.asList(requiredAppsList));
            for (ResolveInfo resolveInfo2 : homeList.getList()) {
                if (requiredAppsSet.contains(resolveInfo2.activityInfo.packageName)) {
                    setComponentEnabledSetting(resolveInfo2.getComponentInfo().getComponentName(), 2, 1, i);
                }
                packagesToDelete.add(resolveInfo2.activityInfo.packageName);
            }
        }
        packagesToDelete.removeAll(Arrays.asList(requiredAppsList));
        packagesToDelete.addAll(Arrays.asList(disallowedAppsList));
        HashSet uninstalled = new HashSet();
        for (String pkg2 : packagesToDelete) {
            try {
                if (getPackageInfo(pkg2, 0, i) == null) {
                    uninstalled.add(pkg2);
                }
            } catch (Exception e) {
                uninstalled.add(pkg2);
            }
        }
        packagesToDelete.removeAll(uninstalled);
        synchronized (this.mPackages) {
            String[] disallowedAppsList2;
            try {
                Iterator it = packagesToDelete.iterator();
                while (it.hasNext()) {
                    pkg2 = (String) it.next();
                    ((PackageSetting) this.mSettings.mPackages.get(pkg2)).setInstalled(false, i);
                    String str = TAG;
                    Iterator it2 = it;
                    stringBuilder = new StringBuilder();
                    disallowedAppsList2 = disallowedAppsList;
                    stringBuilder.append("Deleting package [");
                    stringBuilder.append(pkg2);
                    stringBuilder.append("] for user ");
                    stringBuilder.append(i);
                    Slog.i(str, stringBuilder.toString());
                    it = it2;
                    disallowedAppsList = disallowedAppsList2;
                }
                scheduleWritePackageRestrictionsLocked(clonedProfileUserId);
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private static void deleteNonRequiredComponentsForClone(PackageManagerService pms, int clonedProfileUserId) {
        for (String str : pms.mContext.getResources().getStringArray(33816591)) {
            String[] componentArray = str.split("/");
            if (componentArray != null && componentArray.length == 2) {
                ComponentName component = new ComponentName(componentArray[0], componentArray[1]);
                try {
                    if (pms.getComponentEnabledSetting(component, clonedProfileUserId) != 2) {
                        pms.setComponentEnabledSetting(component, 2, 1, clonedProfileUserId);
                    }
                } catch (IllegalArgumentException | SecurityException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("deleteNonRequiredComponentsForClone exception:");
                    stringBuilder.append(e.getMessage());
                    Slog.d(str2, stringBuilder.toString());
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x00b1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void restoreAppDataForClone(String pkgName, int parentUserId, int clonedProfileUserId) {
        int i;
        Package pkg;
        Exception e;
        List<String> requestedPermissions;
        String str = pkgName;
        int i2 = parentUserId;
        int i3 = clonedProfileUserId;
        Package pkg2 = (Package) this.mPackages.get(str);
        String volumeUuid = pkg2.volumeUuid;
        String packageName = pkg2.packageName;
        ApplicationInfo app = pkg2.applicationInfo;
        int appId = UserHandle.getAppId(app.uid);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataUserCePackageDirectory(volumeUuid, i2, packageName).getPath());
        stringBuilder.append(File.separator);
        stringBuilder.append("_hwclone");
        String parentDataUserCePkgDir = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataUserDePackageDirectory(volumeUuid, i2, packageName).getPath());
        stringBuilder.append(File.separator);
        stringBuilder.append("_hwclone");
        String parentDataUserDePkgDir = stringBuilder.toString();
        String cloneDataUserCePkgDir = Environment.getDataUserCePackageDirectory(volumeUuid, i3, packageName).getPath();
        String cloneDataUserDePkgDir = Environment.getDataUserDePackageDirectory(volumeUuid, i3, packageName).getPath();
        Preconditions.checkNotNull(app.seInfo);
        try {
            i = i3;
            pkg = pkg2;
            try {
                this.mInstaller.restoreCloneAppData(volumeUuid, packageName, i, 3, appId, app.seInfo, parentDataUserCePkgDir, cloneDataUserCePkgDir, parentDataUserDePkgDir, cloneDataUserDePkgDir);
            } catch (Exception e2) {
                e = e2;
            }
        } catch (Exception e3) {
            e = e3;
            ApplicationInfo applicationInfo = app;
            String str2 = packageName;
            String str3 = volumeUuid;
            pkg = pkg2;
            String str4 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("failed to restore clone app data for  ");
            stringBuilder2.append(str);
            Slog.e(str4, stringBuilder2.toString(), e);
            requestedPermissions = pkg.requestedPermissions;
            if (requestedPermissions != null) {
            }
            i = clonedProfileUserId;
        }
        requestedPermissions = pkg.requestedPermissions;
        if (requestedPermissions != null) {
            for (String perm : requestedPermissions) {
                Package pkg3;
                if (checkPermission(perm, str, i2) == 0) {
                    pkg3 = pkg;
                    i = clonedProfileUserId;
                    if (-1 == checkPermission(perm, str, i)) {
                        grantRuntimePermission(str, perm, i);
                    }
                } else {
                    pkg3 = pkg;
                    i = clonedProfileUserId;
                }
                pkg = pkg3;
            }
        }
        i = clonedProfileUserId;
    }

    public void deletePackageVersioned(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int deleteFlags) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && (deleteFlags & 2) == 0 && versionedPackage != null && sSupportCloneApps.contains(versionedPackage.getPackageName()) && userId != 0 && sUserManager.isClonedProfile(userId)) {
            Package p = (Package) this.mPackages.get(versionedPackage.getPackageName());
            if (!(p == null || (p.applicationInfo.flags & 1) == 0)) {
                deleteFlags |= 4;
            }
        }
        if (versionedPackage != null) {
            super.deletePackageVersioned(versionedPackage, observer, userId, deleteFlags);
        }
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && (deleteFlags & 2) == 0 && versionedPackage != null && sSupportCloneApps.contains(versionedPackage.getPackageName())) {
            long ident = Binder.clearCallingIdentity();
            try {
                for (UserInfo ui : sUserManager.getProfiles(userId, false)) {
                    if (ui.isClonedProfile() && ui.id != userId && ui.profileGroupId == userId) {
                        PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(versionedPackage.getPackageName());
                        if (pkgSetting != null && pkgSetting.getInstalled(ui.id)) {
                            super.deletePackageVersioned(versionedPackage, observer, ui.id, deleteFlags);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    protected void deleteClonedProfileIfNeed(int[] removedUsers) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && removedUsers != null && removedUsers.length > 0) {
            int length = removedUsers.length;
            int i = 0;
            while (i < length) {
                int userId = removedUsers[i];
                long callingId = Binder.clearCallingIdentity();
                try {
                    UserInfo userInfo = sUserManager.getUserInfo(userId);
                    if (userInfo != null && userInfo.isClonedProfile() && !isAnyApkInstalledInClonedProfile(userId)) {
                        sUserManager.removeUser(userId);
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Remove cloned profile ");
                        stringBuilder.append(userId);
                        Slog.i(str, stringBuilder.toString());
                        Intent clonedProfileIntent = new Intent("android.intent.action.USER_REMOVED");
                        clonedProfileIntent.setPackage(GestureNavConst.DEFAULT_LAUNCHER_PACKAGE);
                        clonedProfileIntent.addFlags(1342177280);
                        clonedProfileIntent.putExtra("android.intent.extra.USER", new UserHandle(userId));
                        clonedProfileIntent.putExtra("android.intent.extra.user_handle", userId);
                        this.mContext.sendBroadcastAsUser(clonedProfileIntent, new UserHandle(userInfo.profileGroupId), null);
                        break;
                    }
                    Binder.restoreCallingIdentity(callingId);
                    i++;
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
    }

    private boolean isAnyApkInstalledInClonedProfile(int clonedProfileUserId) {
        Intent launcherIntent = new Intent("android.intent.action.MAIN");
        launcherIntent.addCategory("android.intent.category.LAUNCHER");
        return queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432, clonedProfileUserId).getList().size() > 0;
    }

    private int redirectInstallForClone(int userId) {
        if (!HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
            return userId;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            UserInfo ui = sUserManager.getUserInfo(userId);
            if (ui == null || !ui.isClonedProfile()) {
                Binder.restoreCallingIdentity(ident);
                return userId;
            }
            int i = ui.profileGroupId;
            return i;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void sendPackageBroadcast(String action, String pkg, Bundle extras, int flags, String targetPkg, IIntentReceiver finishedReceiver, int[] userIds, int[] instantUserIds) {
        String str = action;
        String str2 = pkg;
        int[] iArr = userIds;
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && !((!"android.intent.action.PACKAGE_ADDED".equals(str) && !"android.intent.action.PACKAGE_CHANGED".equals(str)) || iArr == null || sSupportCloneApps.contains(str2))) {
            long callingId = Binder.clearCallingIdentity();
            try {
                int length = iArr.length;
                ParceledListSlice<ResolveInfo> parceledList = null;
                while (parceledList < length) {
                    int userId = iArr[parceledList];
                    if (userId == 0 || !sUserManager.isClonedProfile(userId)) {
                        parceledList++;
                    } else {
                        Intent launcherIntent = new Intent("android.intent.action.MAIN");
                        launcherIntent.addCategory("android.intent.category.LAUNCHER");
                        launcherIntent.setPackage(str2);
                        parceledList = queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786432, userId);
                        if (parceledList != null) {
                            for (ResolveInfo resolveInfo : parceledList.getList()) {
                                setComponentEnabledSetting(resolveInfo.activityInfo.getComponentName(), 2, 1, userId);
                            }
                        }
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
                Binder.restoreCallingIdentity(callingId);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(callingId);
            }
        }
        super.sendPackageBroadcast(action, pkg, extras, flags, targetPkg, finishedReceiver, userIds, instantUserIds);
    }

    private static void initCloneAppsFromCust() {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP) {
            File configFile = getCustomizedFileName(CLONE_APP_LIST, 0);
            if (configFile == null || !configFile.exists()) {
                Flog.i(205, "hw_clone_app_list.xml does not exists.");
                return;
            }
            InputStream inputStream = null;
            XmlPullParser xmlParser = null;
            try {
                inputStream = new FileInputStream(configFile);
                xmlParser = Xml.newPullParser();
                xmlParser.setInput(inputStream, null);
                while (true) {
                    int next = xmlParser.next();
                    int xmlEventType = next;
                    if (next == 1) {
                        try {
                            break;
                        } catch (IOException e) {
                            Slog.e(TAG, "initCloneAppsFromCust:- IOE while closing stream", e);
                        }
                    } else if (xmlEventType == 2 && "package".equals(xmlParser.getName())) {
                        String packageName = xmlParser.getAttributeValue(null, "name");
                        if (!TextUtils.isEmpty(packageName)) {
                            sSupportCloneApps.add(packageName);
                        }
                    }
                }
                inputStream.close();
            } catch (FileNotFoundException e2) {
                Log.e(TAG, "initCloneAppsFromCust", e2);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (XmlPullParserException e3) {
                Log.e(TAG, "initCloneAppsFromCust", e3);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e4) {
                Log.e(TAG, "initCloneAppsFromCust", e4);
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable th) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e5) {
                        Slog.e(TAG, "initCloneAppsFromCust:- IOE while closing stream", e5);
                    }
                }
            }
        }
    }

    public static boolean isSupportCloneAppInCust(String packageName) {
        return sSupportCloneApps.contains(packageName);
    }

    private static void deleteNonSupportedAppsForClone(PackageManagerService pms) {
        PackageManagerService packageManagerService = pms;
        long callingId = Binder.clearCallingIdentity();
        try {
            boolean z = false;
            List<UserInfo> users = sUserManager.getUsers(false);
            for (UserInfo ui : users) {
                if (ui.isClonedProfile()) {
                    deleteNonRequiredComponentsForClone(packageManagerService, ui.id);
                    String[] disallowedAppsList = packageManagerService.mContext.getResources().getStringArray(33816585);
                    Set<String> requiredAppsSet = new HashSet(Arrays.asList(packageManagerService.mContext.getResources().getStringArray(33816586)));
                    boolean shouldUpdate = false;
                    for (String pkg : requiredAppsSet) {
                        if (!TextUtils.isEmpty(pkg)) {
                            PackageSetting pkgSetting = (PackageSetting) packageManagerService.mSettings.mPackages.get(pkg);
                            if (!(pkgSetting == null || pkgSetting.getInstalled(ui.id) || !pkgSetting.getInstalled(ui.profileGroupId))) {
                                pkgSetting.setInstalled(true, ui.id);
                                shouldUpdate = true;
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Adding required package [");
                                stringBuilder.append(pkg);
                                stringBuilder.append("] for clone user ");
                                stringBuilder.append(ui.id);
                                Slog.i(str, stringBuilder.toString());
                            }
                        }
                    }
                    Intent launcherIntent = new Intent("android.intent.action.MAIN");
                    launcherIntent.addCategory("android.intent.category.LAUNCHER");
                    ParceledListSlice<ResolveInfo> parceledList = packageManagerService.queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(packageManagerService.mContext.getContentResolver()), 786432, ui.id);
                    if (parceledList != null) {
                        for (ResolveInfo resolveInfo : parceledList.getList()) {
                            int i;
                            if (!(sSupportCloneApps.contains(resolveInfo.activityInfo.packageName) || requiredAppsSet.contains(resolveInfo.activityInfo.packageName))) {
                                ((PackageSetting) packageManagerService.mSettings.mPackages.get(resolveInfo.activityInfo.packageName)).setInstalled(z, ui.id);
                                shouldUpdate = true;
                                String str2 = TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Deleting non supported package [");
                                stringBuilder2.append(resolveInfo.activityInfo.packageName);
                                stringBuilder2.append("] for clone user ");
                                stringBuilder2.append(ui.id);
                                Slog.i(str2, stringBuilder2.toString());
                            }
                            if (requiredAppsSet.contains(resolveInfo.activityInfo.packageName)) {
                                i = 1;
                                packageManagerService.setComponentEnabledSetting(resolveInfo.activityInfo.getComponentName(), 2, 1, ui.id);
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Disable [");
                                stringBuilder3.append(resolveInfo.activityInfo.getComponentName());
                                stringBuilder3.append("] for clone user ");
                                stringBuilder3.append(ui.id);
                                Slog.i(str3, stringBuilder3.toString());
                                shouldUpdate = true;
                            } else {
                                i = 1;
                            }
                            int i2 = i;
                            z = false;
                        }
                    }
                    int length = disallowedAppsList.length;
                    boolean shouldUpdate2 = shouldUpdate;
                    int shouldUpdate3 = 0;
                    while (shouldUpdate3 < length) {
                        List<UserInfo> users2;
                        String pkg2 = disallowedAppsList[shouldUpdate3];
                        if (!TextUtils.isEmpty(pkg2)) {
                            PackageSetting pkgSetting2 = (PackageSetting) packageManagerService.mSettings.mPackages.get(pkg2);
                            if (pkgSetting2 != null && pkgSetting2.getInstalled(ui.id)) {
                                pkgSetting2.setInstalled(false, ui.id);
                                shouldUpdate2 = true;
                                String str4 = TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                users2 = users;
                                stringBuilder4.append("Deleting disallowed package [");
                                stringBuilder4.append(pkg2);
                                stringBuilder4.append("] for clone user ");
                                stringBuilder4.append(ui.id);
                                Slog.i(str4, stringBuilder4.toString());
                                shouldUpdate3++;
                                users = users2;
                            }
                        }
                        users2 = users;
                        shouldUpdate3++;
                        users = users2;
                    }
                    if (shouldUpdate2) {
                        packageManagerService.scheduleWritePackageRestrictionsLocked(ui.id);
                    }
                    Binder.restoreCallingIdentity(callingId);
                }
                z = false;
            }
            Binder.restoreCallingIdentity(callingId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private int updateFlagsForClone(int flags, int userId) {
        if (!HwActivityManagerService.IS_SUPPORT_CLONE_APP || userId == 0 || !getUserManagerInternal().isClonedProfile(userId)) {
            return flags;
        }
        int callingUid = Binder.getCallingUid();
        if (userId == UserHandle.getUserId(callingUid) && sSupportCloneApps.contains(getNameForUid(callingUid))) {
            return flags | 4202496;
        }
        return flags;
    }

    protected List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, int flags, int filterCallingUid, int userId, boolean resolveForStart, boolean allowDynamicSplits) {
        int i = userId;
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && i != 0) {
            int callingUid = Binder.getCallingUid();
            UserInfo ui = getUserManagerInternal().getUserInfo(i);
            if (ui != null && ui.isClonedProfile() && i == UserHandle.getUserId(callingUid)) {
                int flags2;
                boolean shouldCheckUninstall = (flags & 4202496) != 0 && UserHandle.getAppId(callingUid) == 1000;
                if (sSupportCloneApps.contains(getNameForUid(callingUid))) {
                    if ((flags & 4202496) == 0) {
                        shouldCheckUninstall = true;
                    }
                    flags2 = flags | 4202496;
                } else {
                    flags2 = flags;
                }
                boolean shouldCheckUninstall2 = shouldCheckUninstall;
                List<ResolveInfo> result = super.queryIntentActivitiesInternal(intent, resolvedType, flags2, filterCallingUid, i, resolveForStart, allowDynamicSplits);
                if (shouldCheckUninstall2) {
                    Iterator<ResolveInfo> iterator = result.iterator();
                    while (iterator.hasNext()) {
                        ResolveInfo ri = (ResolveInfo) iterator.next();
                        if (!(this.mSettings.isEnabledAndMatchLPr(ri.activityInfo, 786432, ui.profileGroupId) || this.mSettings.isEnabledAndMatchLPr(ri.activityInfo, 786432, i))) {
                            iterator.remove();
                        }
                    }
                }
                return result;
            }
        }
        return super.queryIntentActivitiesInternal(intent, resolvedType, flags, filterCallingUid, userId, resolveForStart, allowDynamicSplits);
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && userId != 0) {
            int callingUid = Binder.getCallingUid();
            UserInfo ui = getUserManagerInternal().getUserInfo(userId);
            if (ui != null && ui.isClonedProfile() && userId == UserHandle.getUserId(callingUid)) {
                boolean shouldCheckUninstall = (flags & 4202496) != 0 && UserHandle.getAppId(callingUid) == 1000;
                if (sSupportCloneApps.contains(getNameForUid(callingUid))) {
                    if ((flags & 4202496) == 0) {
                        shouldCheckUninstall = true;
                    }
                    flags |= 4202496;
                }
                ActivityInfo ai = super.getActivityInfo(component, flags, userId);
                if (!shouldCheckUninstall || ai == null || this.mSettings.isEnabledAndMatchLPr(ai, 786432, ui.profileGroupId) || this.mSettings.isEnabledAndMatchLPr(ai, 786432, userId)) {
                    return ai;
                }
                return null;
            }
        }
        return super.getActivityInfo(component, flags, userId);
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        return super.getPackageInfo(packageName, updateFlagsForClone(flags, userId), userId);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        return super.getApplicationInfo(packageName, updateFlagsForClone(flags, userId), userId);
    }

    public boolean isPackageAvailable(String packageName, int userId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && userId != 0) {
            int callingUid = Binder.getCallingUid();
            if (userId == UserHandle.getUserId(callingUid)) {
                long callingId = Binder.clearCallingIdentity();
                try {
                    UserInfo ui = sUserManager.getUserInfo(userId);
                    if (ui.isClonedProfile() && sSupportCloneApps.contains(getNameForUid(callingUid))) {
                        boolean isPackageAvailable = super.isPackageAvailable(packageName, ui.profileGroupId);
                        return isPackageAvailable;
                    }
                    Binder.restoreCallingIdentity(callingId);
                } finally {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
        }
        return super.isPackageAvailable(packageName, userId);
    }

    public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId) {
        return super.getInstalledPackages(updateFlagsForClone(flags, userId), userId);
    }

    public ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        return super.getInstalledApplications(updateFlagsForClone(flags, userId), userId);
    }

    public int installExistingPackageAsUser(String packageName, int userId, int installFlags, int installReason) {
        if (userId != 0 && sSupportCloneApps.contains(packageName) && getUserManagerInternal().isClonedProfile(userId)) {
            long callingId = Binder.clearCallingIdentity();
            try {
                setPackageStoppedState(packageName, true, userId);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(packageName);
                stringBuilder.append(" is set stopped for user ");
                stringBuilder.append(userId);
                Slog.d(str, stringBuilder.toString());
            } catch (IllegalArgumentException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("error in setPackageStoppedState for ");
                stringBuilder2.append(e.getMessage());
                Slog.w(str2, stringBuilder2.toString());
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(callingId);
            }
            Binder.restoreCallingIdentity(callingId);
        }
        return super.installExistingPackageAsUser(packageName, userId, installFlags, installReason);
    }

    public ProviderInfo resolveContentProvider(String name, int flags, int userId) {
        return super.resolveContentProvider(name, flags, ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).handleUserForClone(name, userId));
    }

    private void setHdbKey(String key) {
        HwAdbManager.setHdbKey(key);
    }

    public boolean setApplicationMaxAspectRatio(String packageName, float ar) {
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (pkgSetting == null) {
                    Binder.restoreCallingIdentity(callingId);
                    return false;
                } else if (pkgSetting.getMaxAspectRatio() != ar) {
                    pkgSetting.setMaxAspectRatio(ar);
                    this.mSettings.writeLPr();
                    Binder.restoreCallingIdentity(callingId);
                    return true;
                } else {
                    Binder.restoreCallingIdentity(callingId);
                    return false;
                }
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public float getApplicationMaxAspectRatio(String packageName) {
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000 || uid == 0) {
            long callingId = Binder.clearCallingIdentity();
            try {
                synchronized (this.mPackages) {
                    PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
                    if (pkgSetting == null) {
                        Binder.restoreCallingIdentity(callingId);
                        return GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                    }
                    float maxAspectRatio = pkgSetting.getMaxAspectRatio();
                    Binder.restoreCallingIdentity(callingId);
                    return maxAspectRatio;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(callingId);
            }
        } else {
            throw new SecurityException("Only the system can get application max ratio");
        }
    }

    protected void createPublicityFile() {
        AntiMalPreInstallScanner.init(this.mContext, isUpgrade());
        if ("CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""))) {
            ParceledListSlice<ApplicationInfo> slice = getInstalledApplications(0, 0);
            if (isUpgrade()) {
                PackagePublicityUtils.deletePublicityFile();
            }
            PackagePublicityUtils.writeAllPakcagePublicityInfoIntoFile(this.mContext, slice);
        }
    }

    private List<String> getHwPublicityAppList() {
        return PackagePublicityUtils.getHwPublicityAppList(this.mContext);
    }

    private ParcelFileDescriptor getHwPublicityAppParcelFileDescriptor() {
        return PackagePublicityUtils.getHwPublicityAppParcelFileDescriptor();
    }

    protected boolean isNotificationAddSplitButton(String ImsPkgName) {
        if (TextUtils.isEmpty(ImsPkgName) || oneSplitScreenImsListPkgNames.size() == 0 || !oneSplitScreenImsListPkgNames.contains(ImsPkgName.toLowerCase(Locale.getDefault())) || !isSupportSplitScreen(ImsPkgName)) {
            return false;
        }
        String dockableTopPkgName = getDockableTopPkgName();
        if (!TextUtils.isEmpty(dockableTopPkgName) && oneSplitScreenVideoListPkgNames.size() != 0 && oneSplitScreenVideoListPkgNames.contains(dockableTopPkgName.toLowerCase(Locale.getDefault())) && isSupportSplitScreen(dockableTopPkgName)) {
            return true;
        }
        return false;
    }

    private String getDockableTopPkgName() {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        List<RunningTaskInfo> tasks = null;
        if (am != null) {
            tasks = am.getRunningTasks(1);
        }
        RunningTaskInfo runningTaskInfo = null;
        if (!(tasks == null || tasks.isEmpty())) {
            runningTaskInfo = (RunningTaskInfo) tasks.get(0);
        }
        if (runningTaskInfo != null && runningTaskInfo.supportsSplitScreenMultiWindow) {
            int dockSide = -1;
            try {
                if (WindowManagerGlobal.getWindowManagerService().getDockedStackSide() == -1 && !ActivityManager.getService().isInLockTaskMode()) {
                    return runningTaskInfo.topActivity.getPackageName();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "get dockside failed by RemoteException");
            }
        }
        return "";
    }

    private boolean isSupportSplitScreen(String packageName) {
        PackageManager packageManager = this.mContext.getPackageManager();
        int userId = ActivityManager.getCurrentUser();
        Intent mainIntent = getLaunchIntentForPackageAsUser(packageName, packageManager, userId);
        if (mainIntent != null) {
            ComponentName mainComponentName = mainIntent.getComponent();
            if (mainComponentName != null) {
                try {
                    ActivityInfo activityInfo = getActivityInfo(mainComponentName, 0, userId);
                    if (activityInfo != null) {
                        return isResizeableMode(activityInfo.resizeMode);
                    }
                } catch (RuntimeException e) {
                    Slog.e(TAG, "get activityInfo failed by ComponentNameException");
                } catch (Exception e2) {
                    Slog.e(TAG, "get activityInfo failed by ComponentNameException");
                }
            }
        }
        return false;
    }

    private boolean isResizeableMode(int mode) {
        return mode == 2 || mode == 4 || mode == 1;
    }

    private Intent getLaunchIntentForPackageAsUser(String packageName, PackageManager pm, int userId) {
        Intent intentToResolve = new Intent("android.intent.action.MAIN");
        intentToResolve.addCategory("android.intent.category.INFO");
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivitiesAsUser(intentToResolve, 0, userId);
        if (ris == null || ris.size() <= 0) {
            intentToResolve.removeCategory("android.intent.category.INFO");
            intentToResolve.addCategory("android.intent.category.LAUNCHER");
            intentToResolve.setPackage(packageName);
            ris = pm.queryIntentActivitiesAsUser(intentToResolve, 0, userId);
        }
        if (ris == null || ris.size() <= 0) {
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(268435456);
        intent.setClassName(((ResolveInfo) ris.get(0)).activityInfo.packageName, ((ResolveInfo) ris.get(0)).activityInfo.name);
        return intent;
    }

    private int startBackupSession(IBackupSessionCallback callback) {
        Slog.i(TAG, "application bind call startBackupSession");
        if (checkBackupSessionCaller()) {
            return this.mHwFileBackupManager.startBackupSession(callback);
        }
        return -2;
    }

    private int executeBackupTask(int sessionId, String taskCmd) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bind call executeBackupTask on session:");
        stringBuilder.append(sessionId);
        Slog.i(str, stringBuilder.toString());
        if (!checkBackupSessionCaller()) {
            return -2;
        }
        return this.mHwFileBackupManager.executeBackupTask(sessionId, this.mHwFileBackupManager.prepareBackupTaskCmd(taskCmd, this.mPackages));
    }

    private int finishBackupSession(int sessionId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bind call finishBackupSession sessionId:");
        stringBuilder.append(sessionId);
        Slog.i(str, stringBuilder.toString());
        if (checkBackupSessionCaller()) {
            return this.mHwFileBackupManager.finishBackupSession(sessionId);
        }
        return -2;
    }

    private boolean checkBackupSessionCaller() {
        int callingUid = Binder.getCallingUid();
        boolean z = true;
        if (callingUid == 1001) {
            return true;
        }
        String pkgName = getNameForUid(callingUid);
        if (!(this.mHwFileBackupManager.checkBackupPackageName(pkgName) && checkBackupSignature(pkgName))) {
            z = false;
        }
        return z;
    }

    private boolean checkBackupSignature(String pkgName) {
        boolean result = checkSignatures("android", pkgName) == 0;
        if (!result) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BackupSession checkBackupSignature failed, pkgName is ");
            stringBuilder.append(pkgName);
            Slog.d(str, stringBuilder.toString());
        }
        return result;
    }

    protected void loadCorrectUninstallDelapp() {
        File file_ext;
        if (this.mIsPreNUpgrade) {
            file_ext = new File(DATA_DATA_DIR, "uninstalled_delapp.xml");
            if (file_ext.exists()) {
                loadUninstalledDelapp(file_ext, false);
                Slog.w(TAG, "Compatible Fix for pre-N update verify uninstalled App!");
            }
        }
        file_ext = new File("/data/system/", "uninstalled_delapp.xml");
        if (file_ext.exists()) {
            try {
                loadUninstalledDelapp(file_ext);
            } catch (IndexOutOfBoundsException e) {
                Slog.w(TAG, "load uninstalld delapp fail, try another way!");
                loadUninstalledDelapp(file_ext, false);
            }
        }
    }

    protected void addUnisntallDataToCache(String packageName, String codePath) {
        if (packageName == null || codePath == null || codePath.startsWith("/data/app/")) {
            Slog.d(TAG, "Add path to cache failed!");
            return;
        }
        if (!mUninstalledDelappList.contains(packageName)) {
            mUninstalledDelappList.add(packageName);
        }
        mUninstalledMap.put(packageName, codePath);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Add path to cache packageName:");
        stringBuilder.append(packageName);
        stringBuilder.append(",codePath:");
        stringBuilder.append(codePath);
        Slog.i(str, stringBuilder.toString());
    }

    protected boolean checkUninstalledSystemApp(Package pkg, InstallArgs args, PackageInstalledInfo res) throws PackageManagerException {
        Throwable th;
        long j;
        PackageParserException e;
        Package packageR = pkg;
        InstallArgs installArgs = args;
        PackageInstalledInfo packageInstalledInfo = res;
        if (!"com.google.android.syncadapters.contacts".equals(packageR.packageName) && (packageR.mAppMetaData == null || !packageR.mAppMetaData.getBoolean("android.huawei.MARKETED_SYSTEM_APP", false))) {
            return false;
        }
        String packageName = packageR.packageName;
        String codePath = (String) mUninstalledMap.get(packageName);
        String str;
        StringBuilder stringBuilder;
        if (codePath == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(" not a uninstalled system app");
            Slog.i(str, stringBuilder.toString());
            return false;
        }
        StringBuilder stringBuilder2;
        int parseFlags = 0;
        if (((HashSet) mDelMultiInstallMap.get(FLAG_APK_SYS)).contains(codePath) || ((HashSet) mDefaultSystemList.get(FLAG_APK_SYS)).contains(codePath) || (mCotaDelInstallMap != null && ((HashSet) mCotaDelInstallMap.get(FLAG_APK_SYS)).contains(codePath))) {
            parseFlags = 16;
        } else if (((HashSet) mDelMultiInstallMap.get(FLAG_APK_PRIV)).contains(codePath) || ((HashSet) mDefaultSystemList.get(FLAG_APK_PRIV)).contains(codePath) || (mCotaDelInstallMap != null && ((HashSet) mCotaDelInstallMap.get(FLAG_APK_PRIV)).contains(codePath))) {
            parseFlags = 16;
        } else if (!((HashSet) mDelMultiInstallMap.get(FLAG_APK_NOSYS)).contains(codePath) && (mCotaDelInstallMap == null || !((HashSet) mCotaDelInstallMap.get(FLAG_APK_NOSYS)).contains(codePath))) {
            String str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("unkown the parse flag of ");
            stringBuilder2.append(codePath);
            Slog.i(str2, stringBuilder2.toString());
            return false;
        }
        int parseFlags2 = parseFlags;
        File scanFile = new File(codePath);
        str = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("check uninstalled package:");
        stringBuilder2.append(codePath);
        stringBuilder2.append(",parseFlags=");
        stringBuilder2.append(Integer.toHexString(parseFlags2));
        Slog.i(str, stringBuilder2.toString());
        File file;
        String str3;
        try {
            Package uninstalledPkg = new PackageParser().parsePackage(scanFile, parseFlags2, true, 0);
            try {
                PackageParser.collectCertificates(uninstalledPkg, true);
                if (uninstalledPkg == null) {
                    file = scanFile;
                } else if (uninstalledPkg.mSigningDetails.signatures == null) {
                    Package packageR2 = uninstalledPkg;
                    file = scanFile;
                } else if (PackageManagerServiceUtils.compareSignatures(packageR.mSigningDetails.signatures, uninstalledPkg.mSigningDetails.signatures) != 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Warnning:");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" has same package name with system app:");
                    stringBuilder.append(codePath);
                    stringBuilder.append(", but has different signatures!");
                    Slog.w(str, stringBuilder.toString());
                    return false;
                } else {
                    ApplicationInfo applicationInfo = packageR.applicationInfo;
                    applicationInfo.hwFlags |= 536870912;
                    if (packageR.mPersistentApp) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(packageName);
                        stringBuilder.append(" is a persistent system app!");
                        Slog.i(str, stringBuilder.toString());
                        applicationInfo = packageR.applicationInfo;
                        applicationInfo.flags |= 8;
                    }
                    if (scanInstallApk(packageName, codePath, installArgs.user.getIdentifier())) {
                        long uninstalledVersion = uninstalledPkg.getLongVersionCode();
                        long pkgVersion = pkg.getLongVersionCode();
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("uninstalled package versioncode=");
                        stringBuilder2.append(uninstalledVersion);
                        stringBuilder2.append(", installing versionCode=");
                        stringBuilder2.append(pkgVersion);
                        Slog.i(str, stringBuilder2.toString());
                        if (uninstalledVersion < pkgVersion) {
                            return true;
                        }
                        ArrayMap arrayMap = this.mPackages;
                        synchronized (arrayMap) {
                            ArrayMap arrayMap2;
                            try {
                                Package newPackage = (Package) this.mPackages.get(packageName);
                                if (newPackage != null) {
                                    try {
                                        packageInstalledInfo.origUsers = new int[]{installArgs.user.getIdentifier()};
                                        PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                                        if (ps != null) {
                                            packageInstalledInfo.newUsers = ps.queryInstalledUsers(sUserManager.getUserIds(), true);
                                            try {
                                                UserHandle userHandle = installArgs.user;
                                                arrayMap2 = arrayMap;
                                                UserHandle userHandle2 = userHandle;
                                                updateSettingsLI(newPackage, installArgs.installerPackageName, null, packageInstalledInfo, userHandle2, installArgs.installReason);
                                            } catch (Throwable th2) {
                                                th = th2;
                                                throw th;
                                            }
                                        }
                                        arrayMap2 = arrayMap;
                                        file = scanFile;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        j = uninstalledVersion;
                                        arrayMap2 = arrayMap;
                                        file = scanFile;
                                        throw th;
                                    }
                                }
                                j = uninstalledVersion;
                                arrayMap2 = arrayMap;
                                file = scanFile;
                                str3 = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("can not found scanned package:");
                                stringBuilder.append(packageName);
                                Slog.e(str3, stringBuilder.toString());
                                packageInstalledInfo.setError(-25, "Update package's version code is older than uninstalled one");
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Package ");
                                stringBuilder3.append(packageName);
                                stringBuilder3.append(" only restored to uninstalled apk");
                                throw new PackageManagerException(stringBuilder3.toString());
                            } catch (Throwable th4) {
                                th = th4;
                                long j2 = pkgVersion;
                                j = uninstalledVersion;
                                arrayMap2 = arrayMap;
                                file = scanFile;
                                throw th;
                            }
                        }
                    }
                    file = scanFile;
                    Slog.w(TAG, "restore the uninstalled apk failed");
                    return false;
                }
                Slog.e(TAG, "parsed uninstalled package's signature failed!");
                return false;
            } catch (PackageParserException e2) {
                e = e2;
                file = scanFile;
                Package packageR3 = uninstalledPkg;
            }
        } catch (PackageParserException e3) {
            e = e3;
            file = scanFile;
            str3 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("collectCertificates throw ");
            stringBuilder2.append(e);
            Slog.e(str3, stringBuilder2.toString());
            return false;
        }
    }

    public List<String> getSupportSplitScreenApps() {
        List<String> list = new ArrayList();
        list.addAll(oneSplitScreenImsListPkgNames);
        list.addAll(oneSplitScreenVideoListPkgNames);
        return list;
    }

    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId) {
        if (HwActivityManagerService.IS_SUPPORT_CLONE_APP && userId != 0 && ((newState == 0 || newState == 1) && !sSupportCloneApps.contains(componentName.getPackageName()))) {
            long callingId = Binder.clearCallingIdentity();
            try {
                if (sUserManager.isClonedProfile(userId) && new HashSet(Arrays.asList(this.mContext.getResources().getStringArray(33816586))).contains(componentName.getPackageName())) {
                    Intent launcherIntent = new Intent("android.intent.action.MAIN");
                    launcherIntent.addCategory("android.intent.category.LAUNCHER");
                    launcherIntent.setPackage(componentName.getPackageName());
                    ParceledListSlice<ResolveInfo> parceledList = queryIntentActivities(launcherIntent, launcherIntent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 786944, userId);
                    if (parceledList != null) {
                        for (ResolveInfo resolveInfo : parceledList.getList()) {
                            if (componentName.equals(resolveInfo.getComponentInfo().getComponentName())) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("skip enable [");
                                stringBuilder.append(resolveInfo.activityInfo.getComponentName());
                                stringBuilder.append("] for clone user ");
                                stringBuilder.append(userId);
                                Slog.i(str, stringBuilder.toString());
                                return;
                            }
                        }
                    }
                }
                Binder.restoreCallingIdentity(callingId);
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
        if (this.mComponentChangeMonitor != null) {
            this.mComponentChangeMonitor.writeComponetChangeLogToFile(componentName, newState, userId);
        }
        super.setComponentEnabledSetting(componentName, newState, flags, userId);
    }

    private int getApplicationType(String pkgName) {
        int appType = 0;
        try {
            Binder.clearCallingIdentity();
            ApplicationInfo ai = getApplicationInfo(pkgName, 4194304, 0);
            if (isSystemApp(ai)) {
                appType = 0 | 1;
            }
            if (!ai.sourceDir.startsWith("/data/app/")) {
                appType |= 2;
            }
            return appType;
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("app:");
            stringBuilder.append(pkgName);
            stringBuilder.append(", not exists!");
            Slog.i(str, stringBuilder.toString());
            return 128;
        }
    }

    public HashMap<String, HashSet<String>> getMultiInstallMap() {
        return mMultiInstallMap;
    }

    public HashMap<String, HashSet<String>> getDelMultiInstallMap() {
        return mDelMultiInstallMap;
    }
}
