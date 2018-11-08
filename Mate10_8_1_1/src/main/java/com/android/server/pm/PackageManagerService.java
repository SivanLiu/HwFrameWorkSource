package com.android.server.pm;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.ResourcesManager;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.SecurityLog;
import android.app.backup.IBackupManager;
import android.common.HwFrameworkFactory;
import android.common.HwFrameworkMonitor;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.AuthorityEntry;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.AppsQueryHelper;
import android.content.pm.AuxiliaryResolveInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FallbackCategoryProvider;
import android.content.pm.FeatureInfo;
import android.content.pm.IDexModuleRegisterCallback;
import android.content.pm.IOnPermissionsChangeListener;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageManagerNative;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.InstantAppInfo;
import android.content.pm.InstantAppRequest;
import android.content.pm.InstantAppResolveInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.KeySet;
import android.content.pm.PackageCleanItem;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInfoLite;
import android.content.pm.PackageInstaller.SessionInfo;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.LegacyPackageDeleteObserver;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerInternal.ExternalSourcesPolicy;
import android.content.pm.PackageManagerInternal.PackagesProvider;
import android.content.pm.PackageManagerInternal.SyncAdapterPackagesProvider;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Activity;
import android.content.pm.PackageParser.ActivityIntentInfo;
import android.content.pm.PackageParser.Callback;
import android.content.pm.PackageParser.Instrumentation;
import android.content.pm.PackageParser.NewPermissionInfo;
import android.content.pm.PackageParser.Package;
import android.content.pm.PackageParser.PackageParserException;
import android.content.pm.PackageParser.Permission;
import android.content.pm.PackageParser.PermissionGroup;
import android.content.pm.PackageParser.Provider;
import android.content.pm.PackageParser.ProviderIntentInfo;
import android.content.pm.PackageParser.Service;
import android.content.pm.PackageParser.ServiceIntentInfo;
import android.content.pm.PackageStats;
import android.content.pm.PackageUserState;
import android.content.pm.ParceledListSlice;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.Signature;
import android.content.pm.UserInfo;
import android.content.pm.VerifierDeviceIdentity;
import android.content.pm.VerifierInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.hardware.display.DisplayManager;
import android.hdm.HwDeviceManager;
import android.hwtheme.HwThemeManager;
import android.iawareperf.UniPerf;
import android.installerMgr.InstallerMgr;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Environment.UserEnvironment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.os.storage.IStorageManager;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageManagerInternal;
import android.os.storage.StorageManagerInternal.ExternalStorageMountPolicy;
import android.os.storage.VolumeInfo;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.security.KeyStore;
import android.security.SystemKeyStore;
import android.system.ErrnoException;
import android.system.Os;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.ExceptionUtils;
import android.util.Flog;
import android.util.Jlog;
import android.util.Log;
import android.util.LogPrinter;
import android.util.MathUtils;
import android.util.PackageUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.TimingsTraceLog;
import android.util.Xml;
import android.util.jar.StrictJarFile;
import android.util.proto.ProtoOutputStream;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.app.IMediaContainerService.Stub;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.app.ResolverActivity;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.content.NativeLibraryHelper.Handle;
import com.android.internal.content.PackageHelper;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.RoSystemProperties;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.CarrierAppUtils;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.AttributeCache;
import com.android.server.DeviceIdleController.LocalService;
import com.android.server.EventLogTags;
import com.android.server.FgThread;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwUserManagerService;
import com.android.server.IntentResolver;
import com.android.server.LocalServices;
import com.android.server.LockGuard;
import com.android.server.ServiceThread;
import com.android.server.SmartShrinker;
import com.android.server.SystemConfig;
import com.android.server.SystemConfig.PermissionEntry;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.UiThread;
import com.android.server.Watchdog;
import com.android.server.am.HwBroadcastRadarUtil;
import com.android.server.am.ProcessList;
import com.android.server.lights.LightsManager;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.os.HwBootFail;
import com.android.server.pm.Installer.InstallerException;
import com.android.server.pm.PackageDexOptimizer.ForcedUpdatePackageDexOptimizer;
import com.android.server.pm.PermissionsState.PermissionState;
import com.android.server.pm.Settings.VersionInfo;
import com.android.server.pm.dex.DexManager;
import com.android.server.pm.dex.DexManager.RegisterDexModuleResult;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.radar.FrameworkRadar;
import com.android.server.storage.DeviceStorageMonitorInternal;
import com.android.server.usage.UnixCalendar;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import com.huawei.android.content.pm.IHwPackageManager;
import com.huawei.cust.HwCfgFilePolicy;
import com.huawei.indexsearch.IndexSearchManager;
import dalvik.system.CloseGuard;
import dalvik.system.VMRuntime;
import huawei.android.app.HwCustEmergDataManager;
import huawei.android.bootanimation.IBootAnmation;
import huawei.cust.HwCustUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import libcore.io.IoUtils;
import libcore.io.Streams;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class PackageManagerService extends AbsPackageManagerService implements PackageSender, IHwPackageManagerInner {
    private static final List<String> ALL_DANGEROUS_PERMISSIONS = Arrays.asList(new String[]{"android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR", "android.permission.CAMERA", "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.GET_ACCOUNTS", "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.RECORD_AUDIO", "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE", "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG", "com.android.voicemail.permission.ADD_VOICEMAIL", "android.permission.USE_SIP", "android.permission.PROCESS_OUTGOING_CALLS", "android.permission.READ_CELL_BROADCASTS", "android.permission.BODY_SENSORS", "android.permission.SEND_SMS", "android.permission.RECEIVE_SMS", "android.permission.READ_SMS", "android.permission.RECEIVE_WAP_PUSH", "android.permission.RECEIVE_MMS", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_PHONE_NUMBERS", "android.permission.ANSWER_PHONE_CALLS"});
    private static final String ATTR_IS_GRANTED = "g";
    private static final String ATTR_PACKAGE_NAME = "pkg";
    private static final String ATTR_PERMISSION_NAME = "name";
    private static final String ATTR_REVOKE_ON_UPGRADE = "rou";
    private static final String ATTR_USER_FIXED = "fixed";
    private static final String ATTR_USER_SET = "set";
    private static final int BLUETOOTH_UID = 1002;
    static final int BROADCAST_DELAY = 10000;
    static final int CHECK_PENDING_VERIFICATION = 16;
    private static final long CLEAR_DATA_THRESHOLD_MS = 1000;
    static final boolean CLEAR_RUNTIME_PERMISSIONS_ON_UPGRADE = false;
    private static final String COMPRESSED_EXTENSION = ".gz";
    private static final boolean DEBUG_ABI_SELECTION = false;
    private static final boolean DEBUG_APP_DATA = false;
    private static final boolean DEBUG_BACKUP = false;
    private static final boolean DEBUG_BROADCASTS = false;
    private static final boolean DEBUG_COMPRESSION = Build.IS_DEBUGGABLE;
    private static final boolean DEBUG_DELAPP = false;
    public static final boolean DEBUG_DEXOPT = false;
    static final boolean DEBUG_DOMAIN_VERIFICATION = false;
    private static final boolean DEBUG_EPHEMERAL = Build.IS_DEBUGGABLE;
    private static final boolean DEBUG_FILTERS = false;
    private static final boolean DEBUG_INSTALL = false;
    private static final boolean DEBUG_INTENT_MATCHING = false;
    private static final boolean DEBUG_PACKAGE_INFO = false;
    private static final boolean DEBUG_PACKAGE_SCANNING = false;
    private static final boolean DEBUG_PERMISSIONS = false;
    static final boolean DEBUG_PREFERRED = false;
    private static final boolean DEBUG_REMOVE = false;
    static final boolean DEBUG_SD_INSTALL = false;
    static final boolean DEBUG_SETTINGS = false;
    private static final boolean DEBUG_SHARED_LIBRARIES = false;
    private static final boolean DEBUG_SHOW_INFO = false;
    private static final boolean DEBUG_TRIAGED_MISSING = false;
    static final boolean DEBUG_UPGRADE = false;
    private static final boolean DEBUG_VERIFY = false;
    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName(DEFAULT_CONTAINER_PACKAGE, "com.android.defcontainer.DefaultContainerService");
    static final String DEFAULT_CONTAINER_PACKAGE = "com.android.defcontainer";
    private static final long DEFAULT_MANDATORY_FSTRIM_INTERVAL = 259200000;
    private static final boolean DEFAULT_PACKAGE_PARSER_CACHE_ENABLED = true;
    private static final long DEFAULT_UNUSED_STATIC_SHARED_LIB_MIN_CACHE_PERIOD = 7200000;
    private static final int DEFAULT_VERIFICATION_RESPONSE = 1;
    private static final long DEFAULT_VERIFICATION_TIMEOUT = 10000;
    private static final boolean DEFAULT_VERIFY_ENABLE = true;
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final boolean ENABLE_FREE_CACHE_V2 = SystemProperties.getBoolean("fw.free_cache_v2", true);
    static final int END_COPY = 4;
    static final int FIND_INSTALL_LOC = 8;
    static final int FLAGS_REMOVE_CHATTY = Integer.MIN_VALUE;
    private static final int GRANT_DENIED = 1;
    private static final int GRANT_INSTALL = 2;
    private static final int GRANT_RUNTIME = 3;
    private static final int GRANT_UPGRADE = 4;
    private static final boolean HIDE_EPHEMERAL_APIS = false;
    protected static final boolean HWFLOW;
    static final int INIT_COPY = 5;
    private static final String INSTALL_PACKAGE_SUFFIX = "-";
    static final int INSTANT_APP_RESOLUTION_PHASE_TWO = 20;
    static final int INTENT_FILTER_VERIFIED = 18;
    private static final boolean IS_FPGA = boardname.contains("fpga");
    private static final String KILL_APP_REASON_GIDS_CHANGED = "permission grant or revoke changed gids";
    private static final String KILL_APP_REASON_PERMISSIONS_REVOKED = "permissions revoked";
    private static final int LOG_UID = 1007;
    private static final int MAX_PERMISSION_TREE_FOOTPRINT = 32768;
    static final int MCS_BOUND = 3;
    static final int MCS_GIVE_UP = 11;
    static final int MCS_RECONNECT = 10;
    static final int MCS_UNBIND = 6;
    static final int MSG_LAN_ENGLISH = 256;
    static final int MSG_OPER_DATA_UPDATE = 2;
    static final int MSG_OPER_STOP = 1;
    private static final int NFC_UID = 1027;
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String PACKAGE_NAME_BASICADMINRECEIVER_CTS_DEIVCEOWNER = "com.android.cts.deviceowner";
    private static final String PACKAGE_NAME_BASICADMINRECEIVER_CTS_DEVICEANDPROFILEOWNER = "com.android.cts.deviceandprofileowner";
    private static final String PACKAGE_NAME_BASICADMINRECEIVER_CTS_PACKAGEINSTALLER = "com.android.cts.packageinstaller";
    private static final String PACKAGE_PARSER_CACHE_VERSION = "1";
    private static final String PACKAGE_SCHEME = "package";
    static final int PACKAGE_VERIFIED = 15;
    static final String PLATFORM_PACKAGE_NAME = "android";
    static final int POST_INSTALL = 9;
    private static final Set<String> PROTECTED_ACTIONS = new ArraySet();
    private static final int RADIO_UID = 1001;
    public static final int REASON_AB_OTA = 4;
    public static final int REASON_BACKGROUND_DEXOPT = 3;
    public static final int REASON_BG_SPEED_DEXOPT = 7;
    public static final int REASON_BOOT = 1;
    public static final int REASON_FIRST_BOOT = 0;
    public static final int REASON_INACTIVE_PACKAGE_DOWNGRADE = 5;
    public static final int REASON_INSTALL = 2;
    public static final int REASON_LAST = 7;
    public static final int REASON_SHARED = 6;
    static final int SCAN_AS_FULL_APP = 262144;
    static final int SCAN_AS_INSTANT_APP = 131072;
    static final int SCAN_AS_VIRTUAL_PRELOAD = 524288;
    static final int SCAN_BOOTING = 64;
    static final int SCAN_CHECK_ONLY = 8192;
    static final int SCAN_DELETE_DATA_ON_FAILURES = 256;
    static final int SCAN_DONT_KILL_APP = 16384;
    static final int SCAN_FIRST_BOOT_OR_UPGRADE = 65536;
    static final int SCAN_FORCE_DEX = 4;
    static final int SCAN_IGNORE_FROZEN = 32768;
    static final int SCAN_INITIAL = 4096;
    static final int SCAN_MOVE = 2048;
    static final int SCAN_NEW_INSTALL = 16;
    static final int SCAN_NO_DEX = 2;
    static final int SCAN_REPLACING = 512;
    static final int SCAN_REQUIRE_KNOWN = 1024;
    static final int SCAN_TRUSTED_OVERLAY = 128;
    static final int SCAN_UNPACKING_LIB = 1048576;
    static final int SCAN_UPDATE_SIGNATURE = 8;
    static final int SCAN_UPDATE_TIME = 32;
    private static final String SD_ENCRYPTION_ALGORITHM = "AES";
    private static final String SD_ENCRYPTION_KEYSTORE_NAME = "AppsOnSD";
    static final int SEND_PENDING_BROADCAST = 1;
    private static final int SHELL_UID = 2000;
    private static final int SPI_UID = 1054;
    static final int START_CLEANING_PACKAGE = 7;
    static final int START_INTENT_FILTER_VERIFICATIONS = 17;
    private static final String STATIC_SHARED_LIB_DELIMITER = "_";
    private static final String STUB_SUFFIX = "-Stub";
    private static final String SUW_FRP_STATE = "hw_suw_frp_state";
    private static final int SYSTEM_RUNTIME_GRANT_MASK = 52;
    static final String TAG = "PackageManager";
    private static final String TAG_ALL_GRANTS = "rt-grants";
    private static final String TAG_DEFAULT_APPS = "da";
    private static final String TAG_GRANT = "grant";
    private static final String TAG_INTENT_FILTER_VERIFICATION = "iv";
    private static final String TAG_PERMISSION = "perm";
    private static final String TAG_PERMISSION_BACKUP = "perm-grant-backup";
    private static final String TAG_PREFERRED_BACKUP = "pa";
    private static final int TYPE_ACTIVITY = 1;
    private static final int TYPE_PROVIDER = 4;
    private static final int TYPE_RECEIVER = 2;
    private static final int TYPE_SERVICE = 3;
    private static final int TYPE_UNKNOWN = 0;
    static final int UPDATED_MEDIA_STATUS = 12;
    static final int UPDATE_PERMISSIONS_ALL = 1;
    static final int UPDATE_PERMISSIONS_REPLACE_ALL = 4;
    static final int UPDATE_PERMISSIONS_REPLACE_PKG = 2;
    private static final int USER_RUNTIME_GRANT_MASK = 11;
    private static final String VENDOR_OVERLAY_DIR = "/product/overlay";
    static final long WATCHDOG_TIMEOUT;
    static final int WRITE_PACKAGE_LIST = 19;
    static final int WRITE_PACKAGE_RESTRICTIONS = 14;
    static final int WRITE_SETTINGS = 13;
    static final int WRITE_SETTINGS_DELAY = 10000;
    private static String boardname = SystemProperties.get("ro.board.boardname", "0");
    private static boolean mOptimizeBootOn = SystemProperties.getBoolean("ro.config.hw_optimizeBoot", true);
    private static final Comparator<ProviderInfo> mProviderInitOrderSorter = new Comparator<ProviderInfo>() {
        public int compare(ProviderInfo p1, ProviderInfo p2) {
            int v1 = p1.initOrder;
            int v2 = p2.initOrder;
            if (v1 > v2) {
                return -1;
            }
            return v1 < v2 ? 1 : 0;
        }
    };
    private static final Comparator<ResolveInfo> mResolvePrioritySorter = new Comparator<ResolveInfo>() {
        public int compare(ResolveInfo r1, ResolveInfo r2) {
            int i = -1;
            int v1 = r1.priority;
            int v2 = r2.priority;
            if (v1 != v2) {
                if (v1 <= v2) {
                    i = 1;
                }
                return i;
            }
            v1 = r1.preferredOrder;
            v2 = r2.preferredOrder;
            if (v1 != v2) {
                if (v1 <= v2) {
                    i = 1;
                }
                return i;
            } else if (r1.isDefault != r2.isDefault) {
                if (!r1.isDefault) {
                    i = 1;
                }
                return i;
            } else {
                v1 = r1.match;
                v2 = r2.match;
                if (v1 != v2) {
                    if (v1 <= v2) {
                        i = 1;
                    }
                    return i;
                } else if (r1.system != r2.system) {
                    if (!r1.system) {
                        i = 1;
                    }
                    return i;
                } else if (r1.activityInfo != null) {
                    return r1.activityInfo.packageName.compareTo(r2.activityInfo.packageName);
                } else {
                    if (r1.serviceInfo != null) {
                        return r1.serviceInfo.packageName.compareTo(r2.serviceInfo.packageName);
                    }
                    if (r1.providerInfo != null) {
                        return r1.providerInfo.packageName.compareTo(r2.providerInfo.packageName);
                    }
                    return 0;
                }
            }
        }
    };
    private static final int mThreadnum = (Runtime.getRuntime().availableProcessors() + 1);
    private static final Intent sBrowserIntent = new Intent();
    static UserManagerService sUserManager;
    ExecutorService clearDirectoryThread;
    final ActivityIntentResolver mActivities = new ActivityIntentResolver();
    ApplicationInfo mAndroidApplication;
    final File mAppInstallDir;
    protected File mAppLib32InstallDir;
    final ArrayMap<String, ArraySet<String>> mAppOpPermissionPackages;
    final String mAsecInternalPath;
    @GuardedBy("mAvailableFeatures")
    final ArrayMap<String, FeatureInfo> mAvailableFeatures;
    private File mCacheDir;
    @GuardedBy("mPackages")
    final SparseArray<SparseArray<String>> mChangedPackages = new SparseArray();
    @GuardedBy("mPackages")
    int mChangedPackagesSequenceNumber;
    @GuardedBy("mPackages")
    final SparseArray<Map<String, Integer>> mChangedPackagesSequenceNumbers = new SparseArray();
    private final CompilerStats mCompilerStats;
    private IMediaContainerService mContainerService;
    final Context mContext;
    protected boolean mCotaFlag = false;
    private HwCustPackageManagerService mCustPms = ((HwCustPackageManagerService) HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]));
    ComponentName mCustomResolverComponentName;
    private final DefaultContainerConnection mDefContainerConn;
    final int mDefParseFlags;
    @GuardedBy("mPackages")
    boolean mDefaultContainerWhitelisted = false;
    protected DefaultPermissionGrantPolicy mDefaultPermissionPolicy;
    private boolean mDeferProtectedFilters = true;
    private LocalService mDeviceIdleController;
    private final DexManager mDexManager;
    @GuardedBy("mPackages")
    private boolean mDexOptDialogShown;
    private ArraySet<Integer> mDirtyUsers;
    final File mDrmAppPrivateInstallDir;
    private volatile boolean mEphemeralAppsDisabled;
    ExecutorService mExecutorService;
    private final ArraySet<String> mExistingSystemPackages = new ArraySet();
    private final ArrayMap<String, File> mExpectingBetter = new ArrayMap();
    ExternalSourcesPolicy mExternalSourcesPolicy;
    final boolean mFactoryTest;
    boolean mFirstBoot;
    boolean mFoundPolicyFile;
    @GuardedBy("mPackages")
    final ArraySet<String> mFrozenPackages = new ArraySet();
    final int[] mGlobalGids;
    final PackageHandler mHandler;
    final ServiceThread mHandlerThread;
    volatile boolean mHasSystemUidErrors;
    HwInnerPackageManagerService mHwInnerService;
    IHwPackageManagerServiceEx mHwPMSEx;
    IBootAnmation mIBootAnmation;
    final Object mInstallLock = new Object();
    @GuardedBy("mInstallLock")
    final Installer mInstaller;
    final PackageInstallerService mInstallerService;
    ActivityInfo mInstantAppInstallerActivity;
    final ResolveInfo mInstantAppInstallerInfo;
    private final InstantAppRegistry mInstantAppRegistry;
    final EphemeralResolverConnection mInstantAppResolverConnection;
    final ComponentName mInstantAppResolverSettingsComponent;
    final ArrayMap<ComponentName, Instrumentation> mInstrumentation;
    final SparseArray<IntentFilterVerificationState> mIntentFilterVerificationStates;
    private int mIntentFilterVerificationToken;
    private final IntentFilterVerifier<ActivityIntentInfo> mIntentFilterVerifier;
    private final ComponentName mIntentFilterVerifierComponent;
    protected boolean mIsDefaultGoogleCalendar = SystemProperties.getBoolean("ro.default_GoogleCalendar", false);
    protected boolean mIsDefaultPreferredActivityChanged = false;
    boolean mIsPackageScanMultiThread;
    final boolean mIsPreNMR1Upgrade;
    final boolean mIsPreNUpgrade;
    final boolean mIsUpgrade;
    @GuardedBy("mPackages")
    final SparseIntArray mIsolatedOwners = new SparseIntArray();
    private List<String> mKeepUninstalledPackages;
    final ArrayMap<String, Set<String>> mKnownCodebase = new ArrayMap();
    @GuardedBy("mLoadedVolumes")
    final ArraySet<String> mLoadedVolumes = new ArraySet();
    private boolean mMediaMounted;
    final DisplayMetrics mMetrics;
    private HwFrameworkMonitor mMonitor;
    private final MoveCallbacks mMoveCallbacks;
    public boolean mNeedClearDeviceForCTS;
    int mNextInstallToken;
    private AtomicInteger mNextMoveId;
    private final OnPermissionChangeListeners mOnPermissionChangeListeners;
    final boolean mOnlyCore;
    protected final PackageDexOptimizer mPackageDexOptimizer;
    final Callback mPackageParserCallback = new PackageParserCallback();
    private final PackageUsage mPackageUsage;
    @GuardedBy("mPackages")
    final ArrayMap<String, Package> mPackages = new ArrayMap();
    final ParallelPackageParserCallback mParallelPackageParserCallback = new ParallelPackageParserCallback();
    final PendingPackageBroadcasts mPendingBroadcasts;
    final SparseArray<PackageVerificationState> mPendingVerification;
    private int mPendingVerificationToken;
    final ArrayMap<String, PermissionGroup> mPermissionGroups;
    final boolean mPermissionReviewRequired;
    Package mPlatformPackage;
    private ArrayList<Message> mPostSystemReadyMessages;
    private Future<?> mPrepareAppDataFuture;
    private ArraySet<String> mPrivappPermissionsViolations;
    private final ProcessLoggingHandler mProcessLoggingHandler;
    boolean mPromoteSystemApps;
    @GuardedBy("mProtectedBroadcasts")
    final ArraySet<String> mProtectedBroadcasts;
    private final List<ActivityIntentInfo> mProtectedFilters = new ArrayList();
    final ProtectedPackages mProtectedPackages;
    final ProviderIntentResolver mProviders;
    final ArrayMap<String, Provider> mProvidersByAuthority;
    final ActivityIntentResolver mReceivers = new ActivityIntentResolver();
    final String mRequiredInstallerPackage;
    final String mRequiredUninstallerPackage;
    final String mRequiredVerifierPackage;
    final ActivityInfo mResolveActivity;
    ComponentName mResolveComponentName;
    final ResolveInfo mResolveInfo;
    boolean mResolverReplaced;
    final SparseArray<PostInstallData> mRunningInstalls;
    volatile boolean mSafeMode;
    final int mSdkVersion = VERSION.SDK_INT;
    final String[] mSeparateProcesses;
    final ServiceIntentResolver mServices;
    final String mServicesSystemSharedLibraryPackageName;
    @GuardedBy("mPackages")
    final Settings mSettings;
    final String mSetupWizardPackage;
    final ArrayMap<String, SparseArray<SharedLibraryEntry>> mSharedLibraries = new ArrayMap();
    final String mSharedSystemSharedLibraryPackageName;
    private boolean mShouldRestoreconSdAppData = false;
    final ArrayMap<String, SparseArray<SharedLibraryEntry>> mStaticLibsByDeclaringPackage = new ArrayMap();
    private StorageEventListener mStorageListener;
    final String mStorageManagerPackage;
    final SparseArray<ArraySet<String>> mSystemPermissions;
    volatile boolean mSystemReady;
    protected ArrayList<Package> mTempPkgList = new ArrayList();
    private int mTimerCounter = 0;
    final ArraySet<String> mTransferedPackages;
    private UserManagerInternal mUserManagerInternal;
    SparseBooleanArray mUserNeedsBadging;
    private long startTimer = 0;

    private interface BlobXmlRestorer {
        void apply(XmlPullParser xmlPullParser, int i) throws IOException, XmlPullParserException;
    }

    final class ActivityIntentResolver extends IntentResolver<ActivityIntentInfo, ResolveInfo> {
        private final ArrayMap<ComponentName, Activity> mActivities = new ArrayMap();
        private int mFlags;

        public class IterGenerator<E> {
            public Iterator<E> generate(ActivityIntentInfo info) {
                return null;
            }
        }

        public class ActionIterGenerator extends IterGenerator<String> {
            public ActionIterGenerator() {
                super();
            }

            public Iterator<String> generate(ActivityIntentInfo info) {
                return info.actionsIterator();
            }
        }

        public class AuthoritiesIterGenerator extends IterGenerator<AuthorityEntry> {
            public AuthoritiesIterGenerator() {
                super();
            }

            public Iterator<AuthorityEntry> generate(ActivityIntentInfo info) {
                return info.authoritiesIterator();
            }
        }

        public class CategoriesIterGenerator extends IterGenerator<String> {
            public CategoriesIterGenerator() {
                super();
            }

            public Iterator<String> generate(ActivityIntentInfo info) {
                return info.categoriesIterator();
            }
        }

        public class SchemesIterGenerator extends IterGenerator<String> {
            public SchemesIterGenerator() {
                super();
            }

            public Iterator<String> generate(ActivityIntentInfo info) {
                return info.schemesIterator();
            }
        }

        ActivityIntentResolver() {
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = defaultOnly ? 65536 : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            boolean z = true;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            if (PackageManagerService.this.mCustPms == null || !PackageManagerService.this.mCustPms.isSkipMmsSendImageAction()) {
                if ((flags & 65536) == 0) {
                    z = false;
                }
                return super.queryIntent(intent, resolvedType, z, userId);
            }
            if ((flags & 65536) == 0) {
                z = false;
            }
            return PackageManagerService.this.mCustPms.filterResolveInfos(super.queryIntent(intent, resolvedType, z, userId), intent, resolvedType);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<Activity> packageActivities, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageActivities == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (65536 & flags) != 0;
            int N = packageActivities.size();
            ArrayList<ActivityIntentInfo[]> listCut = new ArrayList(N);
            for (int i = 0; i < N; i++) {
                ArrayList<ActivityIntentInfo> intentFilters = ((Activity) packageActivities.get(i)).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    ActivityIntentInfo[] array = new ActivityIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        private Activity findMatchingActivity(List<Activity> activityList, ActivityInfo activityInfo) {
            for (Activity sysActivity : activityList) {
                if (sysActivity.info.name.equals(activityInfo.name) || sysActivity.info.name.equals(activityInfo.targetActivity)) {
                    return sysActivity;
                }
                if (sysActivity.info.targetActivity != null && (sysActivity.info.targetActivity.equals(activityInfo.name) || sysActivity.info.targetActivity.equals(activityInfo.targetActivity))) {
                    return sysActivity;
                }
            }
            return null;
        }

        private <T> void getIntentListSubset(List<ActivityIntentInfo> intentList, IterGenerator<T> generator, Iterator<T> searchIterator) {
            while (searchIterator.hasNext() && intentList.size() != 0) {
                T searchAction = searchIterator.next();
                Iterator<ActivityIntentInfo> intentIter = intentList.iterator();
                while (intentIter.hasNext()) {
                    boolean selectionFound = false;
                    Iterator<T> intentSelectionIter = generator.generate((ActivityIntentInfo) intentIter.next());
                    while (intentSelectionIter != null && intentSelectionIter.hasNext()) {
                        T intentSelection = intentSelectionIter.next();
                        if (intentSelection != null && intentSelection.equals(searchAction)) {
                            selectionFound = true;
                            break;
                        }
                    }
                    if (!selectionFound) {
                        intentIter.remove();
                    }
                }
            }
        }

        private boolean isProtectedAction(ActivityIntentInfo filter) {
            Iterator<String> actionsIter = filter.actionsIterator();
            while (actionsIter != null && actionsIter.hasNext()) {
                if (PackageManagerService.PROTECTED_ACTIONS.contains((String) actionsIter.next())) {
                    return true;
                }
            }
            return false;
        }

        private void adjustPriority(List<Activity> systemActivities, ActivityIntentInfo intent) {
            if (intent.getPriority() > 0) {
                ActivityInfo activityInfo = intent.activity.info;
                if (!((activityInfo.applicationInfo.privateFlags & 8) != 0)) {
                    intent.setPriority(0);
                } else if (systemActivities != null) {
                    Activity foundActivity = findMatchingActivity(systemActivities, activityInfo);
                    if (foundActivity == null) {
                        intent.setPriority(0);
                        return;
                    }
                    List<ActivityIntentInfo> intentListCopy = new ArrayList(foundActivity.intents);
                    List<ActivityIntentInfo> foundFilters = findFilters(intent);
                    Iterator<String> actionsIterator = intent.actionsIterator();
                    if (actionsIterator != null) {
                        getIntentListSubset(intentListCopy, new ActionIterGenerator(), actionsIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    Iterator<String> categoriesIterator = intent.categoriesIterator();
                    if (categoriesIterator != null) {
                        getIntentListSubset(intentListCopy, new CategoriesIterGenerator(), categoriesIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    Iterator<String> schemesIterator = intent.schemesIterator();
                    if (schemesIterator != null) {
                        getIntentListSubset(intentListCopy, new SchemesIterGenerator(), schemesIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    Iterator<AuthorityEntry> authoritiesIterator = intent.authoritiesIterator();
                    if (authoritiesIterator != null) {
                        getIntentListSubset(intentListCopy, new AuthoritiesIterGenerator(), authoritiesIterator);
                        if (intentListCopy.size() == 0) {
                            intent.setPriority(0);
                            return;
                        }
                    }
                    int cappedPriority = 0;
                    for (int i = intentListCopy.size() - 1; i >= 0; i--) {
                        cappedPriority = Math.max(cappedPriority, ((ActivityIntentInfo) intentListCopy.get(i)).getPriority());
                    }
                    if (intent.getPriority() > cappedPriority) {
                        intent.setPriority(cappedPriority);
                    }
                } else if (!isProtectedAction(intent)) {
                } else {
                    if (PackageManagerService.this.mDeferProtectedFilters) {
                        PackageManagerService.this.mProtectedFilters.add(intent);
                    } else if (!intent.activity.info.packageName.equals(PackageManagerService.this.mSetupWizardPackage)) {
                        intent.setPriority(0);
                    }
                }
            }
        }

        public final void addActivity(Activity a, String type) {
            this.mActivities.put(a.getComponentName(), a);
            int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                ActivityIntentInfo intent = (ActivityIntentInfo) a.intents.get(j);
                if ("activity".equals(type)) {
                    PackageSetting ps = PackageManagerService.this.mSettings.getDisabledSystemPkgLPr(intent.activity.info.packageName);
                    List list = (ps == null || ps.pkg == null) ? null : ps.pkg.activities;
                    adjustPriority(list, intent);
                }
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Activity " + a.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeActivity(Activity a, String type) {
            this.mActivities.remove(a.getComponentName());
            int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                removeFilter((ActivityIntentInfo) a.intents.get(j));
            }
        }

        protected boolean allowFilterResult(ActivityIntentInfo filter, List<ResolveInfo> dest) {
            ActivityInfo filterAi = filter.activity.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ActivityInfo destAi = ((ResolveInfo) dest.get(i)).activityInfo;
                if (destAi.name == filterAi.name && destAi.packageName == filterAi.packageName) {
                    return false;
                }
            }
            return true;
        }

        protected ActivityIntentInfo[] newArray(int size) {
            return new ActivityIntentInfo[size];
        }

        protected boolean isFilterStopped(ActivityIntentInfo filter, int userId) {
            boolean z = false;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return true;
            }
            Package p = filter.activity.owner;
            if (p != null) {
                PackageSetting ps = p.mExtras;
                if (ps != null) {
                    if ((ps.pkgFlags & 1) == 0 || HwServiceFactory.isCustedCouldStopped(p.packageName, true, ps.getStopped(userId))) {
                        z = ps.getStopped(userId);
                    }
                    return z;
                }
            }
            return false;
        }

        protected boolean isPackageForFilter(String packageName, ActivityIntentInfo info) {
            return packageName.equals(info.activity.owner.packageName);
        }

        protected ResolveInfo newResult(ActivityIntentInfo info, int match, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            if (!PackageManagerService.this.mSettings.isEnabledAndMatchLPr(info.activity.info, this.mFlags, userId)) {
                return null;
            }
            Activity activity = info.activity;
            PackageSetting ps = activity.owner.mExtras;
            if (ps == null) {
                return null;
            }
            if (PackageManagerService.this.mSafeMode && (PackageManagerService.this.isSystemPathApp(ps) ^ 1) != 0) {
                return null;
            }
            PackageUserState userState = ps.readUserState(userId);
            ActivityInfo ai = PackageParser.generateActivityInfo(activity, this.mFlags, userState, userId);
            if (ai == null) {
                return null;
            }
            boolean matchExplicitlyVisibleOnly = (this.mFlags & 33554432) != 0;
            boolean matchVisibleToInstantApp = (this.mFlags & 16777216) != 0;
            boolean componentVisible = (matchVisibleToInstantApp && info.isVisibleToInstantApp()) ? matchExplicitlyVisibleOnly ? info.isExplicitlyVisibleToInstantApp() : true : false;
            boolean matchInstantApp = (this.mFlags & DumpState.DUMP_VOLUMES) != 0;
            if (matchVisibleToInstantApp) {
                if (((!componentVisible ? userState.instantApp : 1) ^ 1) != 0) {
                    return null;
                }
            }
            if (!matchInstantApp && userState.instantApp) {
                return null;
            }
            if (userState.instantApp && ps.isUpdateAvailable()) {
                return null;
            }
            ResolveInfo res = new ResolveInfo();
            res.activityInfo = ai;
            if ((this.mFlags & 64) != 0) {
                res.filter = info;
            }
            if (info != null && info.countActionFilters() > 0) {
                res.filter = info;
            }
            if (info != null) {
                res.handleAllWebDataURI = info.handleAllWebDataURI();
            }
            res.priority = info.getPriority();
            res.preferredOrder = activity.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = info.hasDefault;
            res.labelRes = info.labelRes;
            res.nonLocalizedLabel = info.nonLocalizedLabel;
            if (PackageManagerService.this.userNeedsBadging(userId)) {
                res.noResourceId = true;
            } else {
                res.icon = info.icon;
            }
            res.iconResourceId = info.icon;
            res.system = res.activityInfo.applicationInfo.isSystemApp();
            res.isInstantAppAvailable = userState.instantApp;
            return res;
        }

        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, PackageManagerService.mResolvePrioritySorter);
        }

        protected void dumpFilter(PrintWriter out, String prefix, ActivityIntentInfo filter) {
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(filter.activity)));
            out.print(' ');
            filter.activity.printComponentShortName(out);
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        protected Object filterToLabel(ActivityIntentInfo filter) {
            return filter.activity;
        }

        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            Activity activity = (Activity) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(activity)));
            out.print(' ');
            activity.printComponentShortName(out);
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }
    }

    static abstract class InstallArgs {
        final String abiOverride;
        final Certificate[][] certificates;
        final int installFlags;
        final String[] installGrantPermissions;
        final int installReason;
        final String installerPackageName;
        String[] instructionSets;
        final MoveInfo move;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
        String packageName;
        int packageVersion;
        final int traceCookie;
        final String traceMethod;
        final UserHandle user;
        final String volumeUuid;

        abstract void cleanUpResourcesLI();

        abstract int copyApk(IMediaContainerService iMediaContainerService, boolean z) throws RemoteException;

        abstract boolean doPostDeleteLI(boolean z);

        abstract int doPostInstall(int i, int i2);

        abstract int doPreInstall(int i);

        abstract boolean doRename(int i, Package packageR, String str);

        abstract String getCodePath();

        abstract String getResourcePath();

        InstallArgs(OriginInfo origin, MoveInfo move, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, String volumeUuid, UserHandle user, String[] instructionSets, String abiOverride, String[] installGrantPermissions, String traceMethod, int traceCookie, Certificate[][] certificates, int installReason) {
            this.origin = origin;
            this.move = move;
            this.installFlags = installFlags;
            this.observer = observer;
            this.installerPackageName = installerPackageName;
            this.volumeUuid = volumeUuid;
            this.user = user;
            this.instructionSets = instructionSets;
            this.abiOverride = abiOverride;
            this.installGrantPermissions = installGrantPermissions;
            this.traceMethod = traceMethod;
            this.traceCookie = traceCookie;
            this.certificates = certificates;
            this.installReason = installReason;
        }

        int doPreCopy() {
            return 1;
        }

        int doPostCopy(int uid) {
            return 1;
        }

        protected boolean isFwdLocked() {
            return (this.installFlags & 1) != 0;
        }

        protected boolean isExternalAsec() {
            return (this.installFlags & 8) != 0;
        }

        protected boolean isEphemeral() {
            return (this.installFlags & 2048) != 0;
        }

        UserHandle getUser() {
            return this.user;
        }
    }

    class AsecInstallArgs extends InstallArgs {
        static final String PUBLIC_RES_FILE_NAME = "res.zip";
        static final String RES_FILE_NAME = "pkg.apk";
        String cid;
        String packagePath;
        String resourcePath;

        AsecInstallArgs(InstallParams params) {
            super(params.origin, params.move, params.observer, params.installFlags, params.installerPackageName, params.volumeUuid, params.getUser(), null, params.packageAbiOverride, params.grantedRuntimePermissions, params.traceMethod, params.traceCookie, params.certificates, params.installReason);
        }

        AsecInstallArgs(String fullCodePath, String[] instructionSets, boolean isExternal, boolean isForwardLocked) {
            super(OriginInfo.fromNothing(), null, null, (isExternal ? 8 : 0) | (isForwardLocked ? 1 : 0), null, null, null, instructionSets, null, null, null, 0, null, 0);
            if (!fullCodePath.endsWith(RES_FILE_NAME)) {
                fullCodePath = new File(fullCodePath, RES_FILE_NAME).getAbsolutePath();
            }
            int eidx = fullCodePath.lastIndexOf("/");
            String subStr1 = fullCodePath.substring(0, eidx);
            this.cid = subStr1.substring(subStr1.lastIndexOf("/") + 1, eidx);
            setMountPath(subStr1);
        }

        AsecInstallArgs(String cid, String[] instructionSets, boolean isForwardLocked) {
            super(OriginInfo.fromNothing(), null, null, (PackageManagerService.this.isAsecExternal(cid) ? 8 : 0) | (isForwardLocked ? 1 : 0), null, null, null, instructionSets, null, null, null, 0, null, 0);
            this.cid = cid;
            String sdDir = PackageHelper.getSdDir(cid);
            if (sdDir != null) {
                setMountPath(sdDir);
            }
        }

        void createCopyFile() {
            this.cid = PackageManagerService.this.mInstallerService.allocateExternalStageCidLegacy();
        }

        int copyApk(IMediaContainerService imcs, boolean temp) throws RemoteException {
            if (!this.origin.staged || this.origin.cid == null) {
                if (temp) {
                    createCopyFile();
                } else {
                    PackageHelper.destroySdDir(this.cid);
                }
                String newMountPath = imcs.copyPackageToContainer(this.origin.file.getAbsolutePath(), this.cid, PackageManagerService.getEncryptKey(), isExternalAsec(), isFwdLocked(), PackageManagerService.deriveAbiOverride(this.abiOverride, null));
                if (newMountPath != null) {
                    setMountPath(newMountPath);
                    return 1;
                }
                String reason = "DCS:cPTC;f(" + this.origin.file.getAbsolutePath() + ")c(" + this.cid + ")iExA(" + isExternalAsec() + ")iFL(" + isFwdLocked() + ")";
                FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "AIA::cA", reason);
                PackageManagerService.this.uploadInstallErrRadar(reason);
                return -18;
            }
            this.cid = this.origin.cid;
            setMountPath(PackageHelper.getSdDir(this.cid));
            return 1;
        }

        String getCodePath() {
            return this.packagePath;
        }

        String getResourcePath() {
            return this.resourcePath;
        }

        int doPreInstall(int status) {
            if (status != 1) {
                PackageHelper.destroySdDir(this.cid);
            } else if (!PackageHelper.isContainerMounted(this.cid)) {
                String newMountPath = PackageHelper.mountSdDir(this.cid, PackageManagerService.getEncryptKey(), 1000);
                if (newMountPath != null) {
                    setMountPath(newMountPath);
                } else {
                    String reason = "PH:mSdD;c(" + this.cid + ")";
                    FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "AIA::dPrI", reason);
                    PackageManagerService.this.uploadInstallErrRadar(reason);
                    return -18;
                }
            }
            return status;
        }

        boolean doRename(int status, Package pkg, String oldCodePath) {
            String newCacheId = PackageManagerService.getNextCodePath(oldCodePath, pkg.packageName, "/pkg.apk");
            if (!PackageHelper.isContainerMounted(this.cid) || PackageHelper.unMountSdDir(this.cid)) {
                String newMountPath;
                if (!PackageHelper.renameSdDir(this.cid, newCacheId)) {
                    Slog.e(PackageManagerService.TAG, "Failed to rename " + this.cid + " to " + newCacheId + " which might be stale. Will try to clean up.");
                    if (!PackageHelper.destroySdDir(newCacheId)) {
                        Slog.e(PackageManagerService.TAG, "Very strange. Cannot clean up stale container " + newCacheId);
                        return false;
                    } else if (!PackageHelper.renameSdDir(this.cid, newCacheId)) {
                        Slog.e(PackageManagerService.TAG, "Failed to rename " + this.cid + " to " + newCacheId + " inspite of cleaning it up.");
                        return false;
                    }
                }
                if (PackageHelper.isContainerMounted(newCacheId)) {
                    newMountPath = PackageHelper.getSdDir(newCacheId);
                } else {
                    Slog.w(PackageManagerService.TAG, "Mounting container " + newCacheId);
                    newMountPath = PackageHelper.mountSdDir(newCacheId, PackageManagerService.getEncryptKey(), 1000);
                }
                if (newMountPath == null) {
                    Slog.w(PackageManagerService.TAG, "Failed to get cache path for  " + newCacheId);
                    return false;
                }
                Log.i(PackageManagerService.TAG, "Succesfully renamed " + this.cid + " to " + newCacheId + " at new path: " + newMountPath);
                this.cid = newCacheId;
                File beforeCodeFile = new File(this.packagePath);
                setMountPath(newMountPath);
                File afterCodeFile = new File(this.packagePath);
                pkg.setCodePath(afterCodeFile.getAbsolutePath());
                pkg.setBaseCodePath(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.baseCodePath));
                pkg.setSplitCodePaths(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.splitCodePaths));
                pkg.setApplicationVolumeUuid(pkg.volumeUuid);
                pkg.setApplicationInfoCodePath(pkg.codePath);
                pkg.setApplicationInfoBaseCodePath(pkg.baseCodePath);
                pkg.setApplicationInfoSplitCodePaths(pkg.splitCodePaths);
                pkg.setApplicationInfoResourcePath(pkg.codePath);
                pkg.setApplicationInfoBaseResourcePath(pkg.baseCodePath);
                pkg.setApplicationInfoSplitResourcePaths(pkg.splitCodePaths);
                return true;
            }
            Slog.i(PackageManagerService.TAG, "Failed to unmount " + this.cid + " before renaming");
            return false;
        }

        private void setMountPath(String mountPath) {
            File mountFile = new File(mountPath);
            File monolithicFile = new File(mountFile, RES_FILE_NAME);
            if (monolithicFile.exists()) {
                this.packagePath = monolithicFile.getAbsolutePath();
                if (isFwdLocked()) {
                    this.resourcePath = new File(mountFile, PUBLIC_RES_FILE_NAME).getAbsolutePath();
                    return;
                } else {
                    this.resourcePath = this.packagePath;
                    return;
                }
            }
            this.packagePath = mountFile.getAbsolutePath();
            this.resourcePath = this.packagePath;
        }

        int doPostInstall(int status, int uid) {
            if (status != 1) {
                cleanUp();
                PackageManagerService.this.hwCertCleanUp();
            } else {
                int groupOwner;
                String str;
                if (isFwdLocked()) {
                    groupOwner = UserHandle.getSharedAppGid(uid);
                    str = RES_FILE_NAME;
                } else {
                    groupOwner = -1;
                    str = null;
                }
                if (uid < 10000 || (PackageHelper.fixSdPermissions(this.cid, groupOwner, r2) ^ 1) != 0) {
                    Slog.e(PackageManagerService.TAG, "Failed to finalize " + this.cid);
                    PackageHelper.destroySdDir(this.cid);
                    String reason = "PH:fSP;c(" + this.cid + ")g(" + groupOwner + ")";
                    FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "AIA::dPstI", reason);
                    PackageManagerService.this.uploadInstallErrRadar(reason);
                    return -18;
                } else if (!PackageHelper.isContainerMounted(this.cid)) {
                    PackageHelper.mountSdDir(this.cid, PackageManagerService.getEncryptKey(), Process.myUid());
                }
            }
            return status;
        }

        private void cleanUp() {
            PackageHelper.destroySdDir(this.cid);
        }

        private List<String> getAllCodePaths() {
            File codeFile = new File(getCodePath());
            if (codeFile != null && codeFile.exists()) {
                try {
                    return PackageParser.parsePackageLite(codeFile, 0).getAllCodePaths();
                } catch (PackageParserException e) {
                }
            }
            return Collections.EMPTY_LIST;
        }

        void cleanUpResourcesLI() {
            cleanUpResourcesLI(getAllCodePaths());
        }

        private void cleanUpResourcesLI(List<String> allCodePaths) {
            cleanUp();
            PackageManagerService.this.removeDexFiles(allCodePaths, this.instructionSets);
        }

        String getPackageName() {
            return PackageManagerService.getAsecPackageName(this.cid);
        }

        boolean doPostDeleteLI(boolean delete) {
            List<String> allCodePaths = getAllCodePaths();
            int mounted = PackageHelper.isContainerMounted(this.cid);
            if (mounted != 0 && PackageHelper.unMountSdDir(this.cid)) {
                mounted = 0;
            }
            if (mounted == 0 && delete) {
                cleanUpResourcesLI(allCodePaths);
            }
            return mounted ^ 1;
        }

        int doPreCopy() {
            if (!isFwdLocked() || PackageHelper.fixSdPermissions(this.cid, PackageManagerService.this.getPackageUid(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, 1048576, 0), RES_FILE_NAME)) {
                return 1;
            }
            String reason = "PH:fSP;c(" + this.cid + ")g(" + PackageManagerService.this.getPackageUid(PackageManagerService.DEFAULT_CONTAINER_PACKAGE, 268435456, 0) + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "AIA::dPrC", reason);
            PackageManagerService.this.uploadInstallErrRadar(reason);
            return -18;
        }

        int doPostCopy(int uid) {
            if (!isFwdLocked() || (uid >= 10000 && (PackageHelper.fixSdPermissions(this.cid, UserHandle.getSharedAppGid(uid), RES_FILE_NAME) ^ 1) == 0)) {
                return 1;
            }
            Slog.e(PackageManagerService.TAG, "Failed to finalize " + this.cid);
            PackageHelper.destroySdDir(this.cid);
            String reason = "PH:dPstC;c(" + this.cid + ")g(" + UserHandle.getSharedAppGid(uid) + ")";
            FrameworkRadar.msg(65, FrameworkRadar.RADAR_FWK_ERR_INSTALL_SD, "AIA::dPstC", reason);
            PackageManagerService.this.uploadInstallErrRadar(reason);
            return -18;
        }
    }

    private final class ClearStorageConnection implements ServiceConnection {
        IMediaContainerService mContainerService;

        private ClearStorageConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (this) {
                this.mContainerService = Stub.asInterface(Binder.allowBlocking(service));
                notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            synchronized (this) {
                if (this.mContainerService == null) {
                    Slog.w(PackageManagerService.TAG, "onServiceDisconnected unknown reason");
                    notifyAll();
                }
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ComponentType {
    }

    private static class CrossProfileDomainInfo {
        int bestDomainVerificationStatus;
        ResolveInfo resolveInfo;

        private CrossProfileDomainInfo() {
        }
    }

    class DefaultContainerConnection implements ServiceConnection {
        DefaultContainerConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            PackageManagerService.this.mHandler.sendMessage(PackageManagerService.this.mHandler.obtainMessage(3, Stub.asInterface(Binder.allowBlocking(service))));
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }

    static class DumpState {
        public static final int DUMP_ACTIVITY_RESOLVERS = 4;
        public static final int DUMP_CHANGES = 4194304;
        public static final int DUMP_COMPILER_STATS = 2097152;
        public static final int DUMP_CONTENT_RESOLVERS = 32;
        public static final int DUMP_DEXOPT = 1048576;
        public static final int DUMP_DOMAIN_PREFERRED = 262144;
        public static final int DUMP_FEATURES = 2;
        public static final int DUMP_FROZEN = 524288;
        public static final int DUMP_INSTALLS = 65536;
        public static final int DUMP_INTENT_FILTER_VERIFIERS = 131072;
        public static final int DUMP_KEYSETS = 16384;
        public static final int DUMP_LIBS = 1;
        public static final int DUMP_MESSAGES = 512;
        public static final int DUMP_PACKAGES = 128;
        public static final int DUMP_PERMISSIONS = 64;
        public static final int DUMP_PREFERRED = 4096;
        public static final int DUMP_PREFERRED_XML = 8192;
        public static final int DUMP_PROVIDERS = 1024;
        public static final int DUMP_RECEIVER_RESOLVERS = 16;
        public static final int DUMP_SERVICE_RESOLVERS = 8;
        public static final int DUMP_SHARED_USERS = 256;
        public static final int DUMP_VERIFIERS = 2048;
        public static final int DUMP_VERSION = 32768;
        public static final int DUMP_VOLUMES = 8388608;
        public static final int OPTION_SHOW_FILTERS = 1;
        private int mOptions;
        private SharedUserSetting mSharedUser;
        private boolean mTitlePrinted;
        private int mTypes;

        DumpState() {
        }

        public boolean isDumping(int type) {
            boolean z = true;
            if (this.mTypes == 0 && type != 8192) {
                return true;
            }
            if ((this.mTypes & type) == 0) {
                z = false;
            }
            return z;
        }

        public void setDump(int type) {
            this.mTypes |= type;
        }

        public boolean isOptionEnabled(int option) {
            return (this.mOptions & option) != 0;
        }

        public void setOptionEnabled(int option) {
            this.mOptions |= option;
        }

        public boolean onTitlePrinted() {
            boolean printed = this.mTitlePrinted;
            this.mTitlePrinted = true;
            return printed;
        }

        public boolean getTitlePrinted() {
            return this.mTitlePrinted;
        }

        public void setTitlePrinted(boolean enabled) {
            this.mTitlePrinted = enabled;
        }

        public SharedUserSetting getSharedUser() {
            return this.mSharedUser;
        }

        public void setSharedUser(SharedUserSetting user) {
            this.mSharedUser = user;
        }
    }

    static final class EphemeralIntentResolver extends IntentResolver<AuxiliaryResolveInfo, AuxiliaryResolveInfo> {
        final ArrayMap<String, Pair<Integer, InstantAppResolveInfo>> mOrderResult = new ArrayMap();

        EphemeralIntentResolver() {
        }

        protected AuxiliaryResolveInfo[] newArray(int size) {
            return new AuxiliaryResolveInfo[size];
        }

        protected boolean isPackageForFilter(String packageName, AuxiliaryResolveInfo responseObj) {
            return true;
        }

        protected AuxiliaryResolveInfo newResult(AuxiliaryResolveInfo responseObj, int match, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            String packageName = responseObj.resolveInfo.getPackageName();
            Integer order = Integer.valueOf(responseObj.getOrder());
            Pair<Integer, InstantAppResolveInfo> lastOrderResult = (Pair) this.mOrderResult.get(packageName);
            if (lastOrderResult != null && ((Integer) lastOrderResult.first).intValue() >= order.intValue()) {
                return null;
            }
            InstantAppResolveInfo res = responseObj.resolveInfo;
            if (order.intValue() > 0) {
                this.mOrderResult.put(packageName, new Pair(order, res));
            }
            return responseObj;
        }

        protected void filterResults(List<AuxiliaryResolveInfo> results) {
            if (this.mOrderResult.size() != 0) {
                int resultSize = results.size();
                int i = 0;
                while (i < resultSize) {
                    InstantAppResolveInfo info = ((AuxiliaryResolveInfo) results.get(i)).resolveInfo;
                    String packageName = info.getPackageName();
                    Pair<Integer, InstantAppResolveInfo> savedInfo = (Pair) this.mOrderResult.get(packageName);
                    if (savedInfo != null) {
                        if (savedInfo.second == info) {
                            this.mOrderResult.remove(packageName);
                            if (this.mOrderResult.size() == 0) {
                                break;
                            }
                        } else {
                            results.remove(i);
                            resultSize--;
                            i--;
                        }
                    }
                    i++;
                }
            }
        }
    }

    class FileInstallArgs extends InstallArgs {
        private File codeFile;
        private File resourceFile;

        private int doCopyApk(com.android.internal.app.IMediaContainerService r11, boolean r12) throws android.os.RemoteException {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:28:? in {3, 7, 12, 13, 16, 20, 25, 27, 29, 30} preds:[]
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:129)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r10 = this;
            r9 = 1;
            r7 = r10.origin;
            r7 = r7.staged;
            if (r7 == 0) goto L_0x0014;
        L_0x0007:
            r7 = r10.origin;
            r7 = r7.file;
            r10.codeFile = r7;
            r7 = r10.origin;
            r7 = r7.file;
            r10.resourceFile = r7;
            return r9;
        L_0x0014:
            r7 = r10.installFlags;	 Catch:{ IOException -> 0x0049 }
            r7 = r7 & 2048;	 Catch:{ IOException -> 0x0049 }
            if (r7 == 0) goto L_0x0047;	 Catch:{ IOException -> 0x0049 }
        L_0x001a:
            r2 = 1;	 Catch:{ IOException -> 0x0049 }
        L_0x001b:
            r7 = com.android.server.pm.PackageManagerService.this;	 Catch:{ IOException -> 0x0049 }
            r7 = r7.mInstallerService;	 Catch:{ IOException -> 0x0049 }
            r8 = r10.volumeUuid;	 Catch:{ IOException -> 0x0049 }
            r6 = r7.allocateStageDirLegacy(r8, r2);	 Catch:{ IOException -> 0x0049 }
            r10.codeFile = r6;	 Catch:{ IOException -> 0x0049 }
            r10.resourceFile = r6;	 Catch:{ IOException -> 0x0049 }
            r5 = new com.android.server.pm.PackageManagerService$FileInstallArgs$1;
            r5.<init>();
            r4 = 1;
            r7 = r10.origin;
            r7 = r7.file;
            r7 = r7.getAbsolutePath();
            r4 = r11.copyPackage(r7, r5);
            if (r4 == r9) goto L_0x0066;
        L_0x003d:
            r7 = "PackageManager";
            r8 = "Failed to copy package";
            android.util.Slog.e(r7, r8);
            return r4;
        L_0x0047:
            r2 = 0;
            goto L_0x001b;
        L_0x0049:
            r0 = move-exception;
            r7 = "PackageManager";
            r8 = new java.lang.StringBuilder;
            r8.<init>();
            r9 = "Failed to create copy file: ";
            r8 = r8.append(r9);
            r8 = r8.append(r0);
            r8 = r8.toString();
            android.util.Slog.w(r7, r8);
            r7 = -4;
            return r7;
        L_0x0066:
            r3 = new java.io.File;
            r7 = r10.codeFile;
            r8 = "lib";
            r3.<init>(r7, r8);
            r1 = 0;
            r7 = r10.codeFile;	 Catch:{ IOException -> 0x0081, all -> 0x0091 }
            r1 = com.android.internal.content.NativeLibraryHelper.Handle.create(r7);	 Catch:{ IOException -> 0x0081, all -> 0x0091 }
            r7 = r10.abiOverride;	 Catch:{ IOException -> 0x0081, all -> 0x0091 }
            r4 = com.android.internal.content.NativeLibraryHelper.copyNativeBinariesWithOverride(r1, r3, r7);	 Catch:{ IOException -> 0x0081, all -> 0x0091 }
            libcore.io.IoUtils.closeQuietly(r1);
        L_0x0080:
            return r4;
        L_0x0081:
            r0 = move-exception;
            r7 = "PackageManager";	 Catch:{ IOException -> 0x0081, all -> 0x0091 }
            r8 = "Copying native libraries failed";	 Catch:{ IOException -> 0x0081, all -> 0x0091 }
            android.util.Slog.e(r7, r8, r0);	 Catch:{ IOException -> 0x0081, all -> 0x0091 }
            r4 = -110; // 0xffffffffffffff92 float:NaN double:NaN;
            libcore.io.IoUtils.closeQuietly(r1);
            goto L_0x0080;
        L_0x0091:
            r7 = move-exception;
            libcore.io.IoUtils.closeQuietly(r1);
            throw r7;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.PackageManagerService.FileInstallArgs.doCopyApk(com.android.internal.app.IMediaContainerService, boolean):int");
        }

        FileInstallArgs(InstallParams params) {
            super(params.origin, params.move, params.observer, params.installFlags, params.installerPackageName, params.volumeUuid, params.getUser(), null, params.packageAbiOverride, params.grantedRuntimePermissions, params.traceMethod, params.traceCookie, params.certificates, params.installReason);
            if (isFwdLocked()) {
                throw new IllegalArgumentException("Forward locking only supported in ASEC");
            }
        }

        FileInstallArgs(String codePath, String resourcePath, String[] instructionSets) {
            super(OriginInfo.fromNothing(), null, null, 0, null, null, null, instructionSets, null, null, null, 0, null, 0);
            this.codeFile = codePath != null ? new File(codePath) : null;
            this.resourceFile = resourcePath != null ? new File(resourcePath) : null;
        }

        int copyApk(IMediaContainerService imcs, boolean temp) throws RemoteException {
            Trace.traceBegin(262144, "copyApk");
            try {
                int doCopyApk = doCopyApk(imcs, temp);
                return doCopyApk;
            } finally {
                Trace.traceEnd(262144);
            }
        }

        int doPreInstall(int status) {
            if (status != 1) {
                cleanUp();
            }
            return status;
        }

        boolean doRename(int status, Package pkg, String oldCodePath) {
            if (status != 1) {
                cleanUp();
                return false;
            }
            File targetDir = this.codeFile.getParentFile();
            File beforeCodeFile = this.codeFile;
            File afterCodeFile = PackageManagerService.this.getNextCodePath(targetDir, pkg.packageName);
            try {
                Os.rename(beforeCodeFile.getAbsolutePath(), afterCodeFile.getAbsolutePath());
                if (SELinux.restoreconRecursive(afterCodeFile)) {
                    this.codeFile = afterCodeFile;
                    this.resourceFile = afterCodeFile;
                    pkg.setCodePath(afterCodeFile.getAbsolutePath());
                    pkg.setBaseCodePath(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.baseCodePath));
                    pkg.setSplitCodePaths(FileUtils.rewriteAfterRename(beforeCodeFile, afterCodeFile, pkg.splitCodePaths));
                    pkg.setApplicationVolumeUuid(pkg.volumeUuid);
                    pkg.setApplicationInfoCodePath(pkg.codePath);
                    pkg.setApplicationInfoBaseCodePath(pkg.baseCodePath);
                    pkg.setApplicationInfoSplitCodePaths(pkg.splitCodePaths);
                    pkg.setApplicationInfoResourcePath(pkg.codePath);
                    pkg.setApplicationInfoBaseResourcePath(pkg.baseCodePath);
                    pkg.setApplicationInfoSplitResourcePaths(pkg.splitCodePaths);
                    return true;
                }
                Slog.w(PackageManagerService.TAG, "Failed to restorecon");
                return false;
            } catch (ErrnoException e) {
                Slog.w(PackageManagerService.TAG, "Failed to rename", e);
                return false;
            }
        }

        int doPostInstall(int status, int uid) {
            if (status != 1) {
                cleanUp();
                PackageManagerService.this.hwCertCleanUp();
            }
            return status;
        }

        String getCodePath() {
            return this.codeFile != null ? this.codeFile.getAbsolutePath() : null;
        }

        String getResourcePath() {
            return this.resourceFile != null ? this.resourceFile.getAbsolutePath() : null;
        }

        private boolean cleanUp() {
            if (this.codeFile == null || (this.codeFile.exists() ^ 1) != 0) {
                return false;
            }
            PackageManagerService.this.removeCodePathLI(this.codeFile);
            if (!(this.resourceFile == null || (FileUtils.contains(this.codeFile, this.resourceFile) ^ 1) == 0)) {
                this.resourceFile.delete();
            }
            return true;
        }

        void cleanUpResourcesLI() {
            List<String> allCodePaths = Collections.EMPTY_LIST;
            if (this.codeFile != null && this.codeFile.exists()) {
                try {
                    allCodePaths = PackageParser.parsePackageLite(this.codeFile, 0).getAllCodePaths();
                } catch (PackageParserException e) {
                }
            }
            cleanUp();
            PackageManagerService.this.removeDexFiles(allCodePaths, this.instructionSets);
        }

        boolean doPostDeleteLI(boolean delete) {
            cleanUpResourcesLI();
            return true;
        }
    }

    private abstract class HandlerParams {
        private static final int MAX_RETRIES = 4;
        private int mRetries = 0;
        private final UserHandle mUser;
        int traceCookie;
        String traceMethod;

        abstract void handleReturnCode();

        abstract void handleServiceError();

        abstract void handleStartCopy() throws RemoteException;

        HandlerParams(UserHandle user) {
            this.mUser = user;
        }

        UserHandle getUser() {
            return this.mUser;
        }

        HandlerParams setTraceMethod(String traceMethod) {
            this.traceMethod = traceMethod;
            return this;
        }

        HandlerParams setTraceCookie(int traceCookie) {
            this.traceCookie = traceCookie;
            return this;
        }

        final boolean startCopy() {
            boolean res;
            try {
                int i = this.mRetries + 1;
                this.mRetries = i;
                if (i > 4) {
                    Slog.w(PackageManagerService.TAG, "Failed to invoke remote methods on default container service. Giving up");
                    PackageManagerService.this.mHandler.sendEmptyMessage(11);
                    handleServiceError();
                    return false;
                }
                handleStartCopy();
                res = true;
                handleReturnCode();
                return res;
            } catch (RemoteException e) {
                PackageManagerService.this.mHandler.sendEmptyMessage(10);
                res = false;
            } catch (Exception e2) {
                Log.e(PackageManagerService.TAG, "Posting install MCS_GIVE_UP");
                PackageManagerService.this.mHandler.sendEmptyMessage(11);
                res = false;
            }
        }

        final void serviceError() {
            handleServiceError();
            handleReturnCode();
        }
    }

    public class HwInnerPackageManagerService extends IHwPackageManager.Stub {
        PackageManagerService mPMS;

        HwInnerPackageManagerService(PackageManagerService pms) {
            this.mPMS = pms;
        }

        public int getAppUseNotchMode(String packageName) {
            return PackageManagerService.this.mHwPMSEx.getAppUseNotchMode(packageName);
        }

        public void setAppUseNotchMode(String packageName, int mode) {
            PackageManagerService.this.mHwPMSEx.setAppUseNotchMode(packageName, mode);
        }
    }

    private static class IFVerificationParams {
        Package pkg;
        boolean replacing;
        int userId;
        int verifierUid;

        public IFVerificationParams(Package _pkg, boolean _replacing, int _userId, int _verifierUid) {
            this.pkg = _pkg;
            this.replacing = _replacing;
            this.userId = _userId;
            this.replacing = _replacing;
            this.verifierUid = _verifierUid;
        }
    }

    class InstallParams extends HandlerParams {
        final Certificate[][] certificates;
        final String[] grantedRuntimePermissions;
        int installFlags;
        final int installReason;
        final String installerPackageName;
        private InstallArgs mArgs;
        private int mRet;
        final MoveInfo move;
        final IPackageInstallObserver2 observer;
        final OriginInfo origin;
        final String packageAbiOverride;
        final VerificationInfo verificationInfo;
        final String volumeUuid;

        InstallParams(OriginInfo origin, MoveInfo move, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, String volumeUuid, VerificationInfo verificationInfo, UserHandle user, String packageAbiOverride, String[] grantedPermissions, Certificate[][] certificates, int installReason) {
            super(user);
            this.origin = origin;
            this.move = move;
            this.observer = observer;
            this.installFlags = installFlags;
            this.installerPackageName = installerPackageName;
            this.volumeUuid = volumeUuid;
            this.verificationInfo = verificationInfo;
            this.packageAbiOverride = packageAbiOverride;
            this.grantedRuntimePermissions = grantedPermissions;
            this.certificates = certificates;
            this.installReason = installReason;
        }

        public String toString() {
            return "InstallParams{" + Integer.toHexString(System.identityHashCode(this)) + " file=" + this.origin.file + " cid=" + this.origin.cid + "}";
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int installLocationPolicy(PackageInfoLite pkgLite) {
            String packageName = pkgLite.packageName;
            int installLocation = pkgLite.installLocation;
            boolean onSd = (this.installFlags & 8) != 0;
            synchronized (PackageManagerService.this.mPackages) {
                Package installedPkg = (Package) PackageManagerService.this.mPackages.get(packageName);
                Package dataOwnerPkg = installedPkg;
                if (installedPkg == null) {
                    PackageSetting ps = (PackageSetting) PackageManagerService.this.mSettings.mPackages.get(packageName);
                    if (ps != null) {
                        dataOwnerPkg = ps.pkg;
                    }
                }
                if (dataOwnerPkg != null) {
                    boolean z = (this.installFlags & 128) != 0 ? !Build.IS_DEBUGGABLE ? (dataOwnerPkg.applicationInfo.flags & 2) != 0 : true : false;
                    if (!z) {
                        try {
                            PackageManagerService.checkDowngrade(dataOwnerPkg, pkgLite);
                        } catch (PackageManagerException e) {
                            Slog.w(PackageManagerService.TAG, "Downgrade detected: " + e.getMessage());
                            return -7;
                        }
                    }
                }
                if (installedPkg != null) {
                    if ((this.installFlags & 2) == 0) {
                        return -4;
                    } else if ((installedPkg.applicationInfo.flags & 1) != 0) {
                        if (onSd) {
                            Slog.w(PackageManagerService.TAG, "Cannot install update to system app on sdcard");
                            return -3;
                        }
                        return 1;
                    } else if (onSd) {
                        return 2;
                    } else if (installLocation == 1) {
                        return 1;
                    } else if (installLocation != 2) {
                        if (PackageManagerService.isExternal(installedPkg)) {
                            return 2;
                        }
                        return 1;
                    }
                }
            }
        }

        public void handleStartCopy() throws RemoteException {
            int ret = 1;
            if (this.origin.staged) {
                if (this.origin.file != null) {
                    this.installFlags |= 16;
                    this.installFlags &= -9;
                } else if (this.origin.cid != null) {
                    this.installFlags |= 8;
                    this.installFlags &= -17;
                } else {
                    throw new IllegalStateException("Invalid stage location");
                }
            }
            boolean onSd = (this.installFlags & 8) != 0;
            boolean onInt = (this.installFlags & 16) != 0;
            boolean ephemeral = (this.installFlags & 2048) != 0;
            PackageInfoLite pkgLite = null;
            if (onInt && onSd) {
                Slog.w(PackageManagerService.TAG, "Conflicting flags specified for installing on both internal and external");
                ret = -19;
            } else if (onSd && ephemeral) {
                Slog.w(PackageManagerService.TAG, "Conflicting flags specified for installing ephemeral on external");
                ret = -19;
            } else {
                pkgLite = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                if (PackageManagerService.DEBUG_EPHEMERAL && ephemeral) {
                    Slog.v(PackageManagerService.TAG, "pkgLite for install: " + pkgLite);
                }
                if (!this.origin.staged && pkgLite.recommendedInstallLocation == -1) {
                    try {
                        PackageManagerService.this.mInstaller.freeCache(null, PackageManagerService.this.mContainerService.calculateInstalledSize(this.origin.resolvedPath, isForwardLocked(), this.packageAbiOverride) + StorageManager.from(PackageManagerService.this.mContext).getStorageLowBytes(Environment.getDataDirectory()), 0, 0);
                        pkgLite = PackageManagerService.this.mContainerService.getMinimalPackageInfo(this.origin.resolvedPath, this.installFlags, this.packageAbiOverride);
                    } catch (Throwable e) {
                        Slog.w(PackageManagerService.TAG, "Failed to free cache", e);
                    }
                    if (pkgLite.recommendedInstallLocation == -6) {
                        pkgLite.recommendedInstallLocation = -1;
                    }
                }
            }
            if (ret == 1) {
                int loc = pkgLite.recommendedInstallLocation;
                if (loc == -3) {
                    ret = -19;
                } else if (loc == -4) {
                    ret = -1;
                } else if (loc == -1) {
                    ret = -4;
                } else if (loc == -2) {
                    ret = -2;
                } else if (loc == -6) {
                    ret = -3;
                } else if (loc == -5) {
                    ret = -20;
                } else {
                    loc = installLocationPolicy(pkgLite);
                    if (loc == -7) {
                        ret = -25;
                    } else if (!(onSd || (onInt ^ 1) == 0)) {
                        if (loc == 2) {
                            this.installFlags |= 8;
                            this.installFlags &= -17;
                        } else if (loc == 3) {
                            if (PackageManagerService.DEBUG_EPHEMERAL) {
                                Slog.v(PackageManagerService.TAG, "...setting INSTALL_EPHEMERAL install flag");
                            }
                            this.installFlags |= 2048;
                            this.installFlags &= -25;
                        } else {
                            this.installFlags |= 16;
                            this.installFlags &= -9;
                        }
                    }
                }
            }
            InstallArgs args = PackageManagerService.this.createInstallArgs(this);
            if (pkgLite != null) {
                args.packageName = pkgLite.packageName;
                args.packageVersion = pkgLite.versionCode;
            }
            this.mArgs = args;
            if (ret == 1) {
                int requiredUid;
                UserHandle verifierUser = getUser();
                if (verifierUser == UserHandle.ALL) {
                    verifierUser = UserHandle.SYSTEM;
                }
                if (PackageManagerService.this.mRequiredVerifierPackage == null) {
                    requiredUid = -1;
                } else {
                    requiredUid = PackageManagerService.this.getPackageUid(PackageManagerService.this.mRequiredVerifierPackage, 268435456, verifierUser.getIdentifier());
                }
                int installerUid = this.verificationInfo == null ? -1 : this.verificationInfo.installerUid;
                if (this.origin.existing || requiredUid == -1 || !PackageManagerService.this.isVerificationEnabled(verifierUser.getIdentifier(), this.installFlags, installerUid)) {
                    ret = args.copyApk(PackageManagerService.this.mContainerService, true);
                } else {
                    Intent verification = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
                    verification.addFlags(268435456);
                    verification.setDataAndType(Uri.fromFile(new File(this.origin.resolvedPath)), PackageManagerService.PACKAGE_MIME_TYPE);
                    verification.addFlags(1);
                    List<ResolveInfo> receivers = PackageManagerService.this.queryIntentReceiversInternal(verification, PackageManagerService.PACKAGE_MIME_TYPE, 0, verifierUser.getIdentifier(), false);
                    PackageManagerService packageManagerService = PackageManagerService.this;
                    int verificationId = packageManagerService.mPendingVerificationToken;
                    packageManagerService.mPendingVerificationToken = verificationId + 1;
                    verification.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_PACKAGE", this.installerPackageName);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALL_FLAGS", this.installFlags);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_PACKAGE_NAME", pkgLite.packageName);
                    verification.putExtra("android.content.pm.extra.VERIFICATION_VERSION_CODE", pkgLite.versionCode);
                    if (this.verificationInfo != null) {
                        if (this.verificationInfo.originatingUri != null) {
                            verification.putExtra("android.intent.extra.ORIGINATING_URI", this.verificationInfo.originatingUri);
                        }
                        if (this.verificationInfo.referrer != null) {
                            verification.putExtra("android.intent.extra.REFERRER", this.verificationInfo.referrer);
                        }
                        if (this.verificationInfo.originatingUid >= 0) {
                            verification.putExtra("android.intent.extra.ORIGINATING_UID", this.verificationInfo.originatingUid);
                        }
                        if (this.verificationInfo.installerUid >= 0) {
                            verification.putExtra("android.content.pm.extra.VERIFICATION_INSTALLER_UID", this.verificationInfo.installerUid);
                        }
                    }
                    PackageVerificationState packageVerificationState = new PackageVerificationState(requiredUid, args);
                    PackageManagerService.this.mPendingVerification.append(verificationId, packageVerificationState);
                    List<ComponentName> sufficientVerifiers = PackageManagerService.this.matchVerifiers(pkgLite, receivers, packageVerificationState);
                    LocalService idleController = PackageManagerService.this.getDeviceIdleController();
                    long idleDuration = PackageManagerService.this.getVerificationTimeout();
                    if (sufficientVerifiers != null) {
                        int N = sufficientVerifiers.size();
                        if (N == 0) {
                            Slog.i(PackageManagerService.TAG, "Additional verifiers required, but none installed.");
                            ret = -22;
                        } else {
                            for (int i = 0; i < N; i++) {
                                ComponentName verifierComponent = (ComponentName) sufficientVerifiers.get(i);
                                idleController.addPowerSaveTempWhitelistApp(Process.myUid(), verifierComponent.getPackageName(), idleDuration, verifierUser.getIdentifier(), false, "package verifier");
                                Intent intent = new Intent(verification);
                                intent.setComponent(verifierComponent);
                                PackageManagerService.this.mContext.sendBroadcastAsUser(intent, verifierUser);
                            }
                        }
                    }
                    ComponentName requiredVerifierComponent = PackageManagerService.this.matchComponentForVerifier(PackageManagerService.this.mRequiredVerifierPackage, receivers);
                    if (ret == 1 && PackageManagerService.this.mRequiredVerifierPackage != null) {
                        Trace.asyncTraceBegin(262144, "verification", verificationId);
                        verification.setComponent(requiredVerifierComponent);
                        idleController.addPowerSaveTempWhitelistApp(Process.myUid(), PackageManagerService.this.mRequiredVerifierPackage, idleDuration, verifierUser.getIdentifier(), false, "package verifier");
                        final int i2 = verificationId;
                        PackageManagerService.this.mContext.sendOrderedBroadcastAsUser(verification, verifierUser, "android.permission.PACKAGE_VERIFICATION_AGENT", new BroadcastReceiver() {
                            public void onReceive(Context context, Intent intent) {
                                Message msg = PackageManagerService.this.mHandler.obtainMessage(16);
                                msg.arg1 = i2;
                                PackageManagerService.this.mHandler.sendMessageDelayed(msg, PackageManagerService.this.getVerificationTimeout());
                            }
                        }, null, 0, null, null);
                        this.mArgs = null;
                    }
                }
            }
            this.mRet = ret;
        }

        void handleReturnCode() {
            if (this.mArgs != null) {
                PackageManagerService.this.processPendingInstall(this.mArgs, this.mRet);
            }
        }

        void handleServiceError() {
            this.mArgs = PackageManagerService.this.createInstallArgs(this);
            this.mRet = RequestStatus.SYS_ETIMEDOUT;
        }

        public boolean isForwardLocked() {
            return (this.installFlags & 1) != 0;
        }
    }

    private interface IntentFilterVerifier<T extends IntentFilter> {
        boolean addOneIntentFilterVerification(int i, int i2, int i3, T t, String str);

        void receiveVerificationResponse(int i);

        void startVerifications(int i);
    }

    private class IntentVerifierProxy implements IntentFilterVerifier<ActivityIntentInfo> {
        private Context mContext;
        private ArrayList<Integer> mCurrentIntentFilterVerifications = new ArrayList();
        private ComponentName mIntentFilterVerifierComponent;

        public IntentVerifierProxy(Context context, ComponentName verifierComponent) {
            this.mContext = context;
            this.mIntentFilterVerifierComponent = verifierComponent;
        }

        private String getDefaultScheme() {
            return "https";
        }

        public void startVerifications(int userId) {
            int count = this.mCurrentIntentFilterVerifications.size();
            for (int n = 0; n < count; n++) {
                int verificationId = ((Integer) this.mCurrentIntentFilterVerifications.get(n)).intValue();
                IntentFilterVerificationState ivs = (IntentFilterVerificationState) PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId);
                String packageName = ivs.getPackageName();
                ArrayList<ActivityIntentInfo> filters = ivs.getFilters();
                int filterCount = filters.size();
                ArraySet<String> domainsSet = new ArraySet();
                for (int m = 0; m < filterCount; m++) {
                    domainsSet.addAll(((ActivityIntentInfo) filters.get(m)).getHostsList());
                }
                synchronized (PackageManagerService.this.mPackages) {
                    if (PackageManagerService.this.mSettings.createIntentFilterVerificationIfNeededLPw(packageName, domainsSet) != null) {
                        PackageManagerService.this.scheduleWriteSettingsLocked();
                    }
                }
                sendVerificationRequest(verificationId, ivs);
            }
            this.mCurrentIntentFilterVerifications.clear();
        }

        private void sendVerificationRequest(int verificationId, IntentFilterVerificationState ivs) {
            Intent verificationIntent = new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION");
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_ID", verificationId);
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_URI_SCHEME", getDefaultScheme());
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_HOSTS", ivs.getHostsString());
            verificationIntent.putExtra("android.content.pm.extra.INTENT_FILTER_VERIFICATION_PACKAGE_NAME", ivs.getPackageName());
            verificationIntent.setComponent(this.mIntentFilterVerifierComponent);
            verificationIntent.addFlags(268435456);
            PackageManagerService.this.getDeviceIdleController().addPowerSaveTempWhitelistApp(Process.myUid(), this.mIntentFilterVerifierComponent.getPackageName(), PackageManagerService.this.getVerificationTimeout(), 0, true, "intent filter verifier");
            this.mContext.sendBroadcastAsUser(verificationIntent, UserHandle.SYSTEM);
        }

        public void receiveVerificationResponse(int verificationId) {
            IntentFilterVerificationState ivs = (IntentFilterVerificationState) PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId);
            boolean verified = ivs.isVerified();
            ArrayList<ActivityIntentInfo> filters = ivs.getFilters();
            int count = filters.size();
            for (int n = 0; n < count; n++) {
                ((ActivityIntentInfo) filters.get(n)).setVerified(verified);
            }
            PackageManagerService.this.mIntentFilterVerificationStates.remove(verificationId);
            String packageName = ivs.getPackageName();
            synchronized (PackageManagerService.this.mPackages) {
                IntentFilterVerificationInfo ivi = PackageManagerService.this.mSettings.getIntentFilterVerificationLPr(packageName);
            }
            if (ivi == null) {
                Slog.w(PackageManagerService.TAG, "IntentFilterVerificationInfo not found for verificationId:" + verificationId + " packageName:" + packageName);
                return;
            }
            synchronized (PackageManagerService.this.mPackages) {
                if (verified) {
                    ivi.setStatus(2);
                } else {
                    ivi.setStatus(1);
                }
                PackageManagerService.this.scheduleWriteSettingsLocked();
                int userId = ivs.getUserId();
                if (userId != -1) {
                    int updatedStatus = 0;
                    boolean needUpdate = false;
                    switch (PackageManagerService.this.mSettings.getIntentFilterVerificationStatusLPr(packageName, userId)) {
                        case 0:
                            if (verified) {
                                updatedStatus = 2;
                            } else {
                                updatedStatus = 1;
                            }
                            needUpdate = true;
                            break;
                        case 1:
                            if (verified) {
                                updatedStatus = 2;
                                needUpdate = true;
                                break;
                            }
                            break;
                    }
                    if (needUpdate) {
                        PackageManagerService.this.mSettings.updateIntentFilterVerificationStatusLPw(packageName, updatedStatus, userId);
                        PackageManagerService.this.scheduleWritePackageRestrictionsLocked(userId);
                    }
                }
            }
        }

        public boolean addOneIntentFilterVerification(int verifierUid, int userId, int verificationId, ActivityIntentInfo filter, String packageName) {
            if (!PackageManagerService.hasValidDomains(filter)) {
                return false;
            }
            IntentFilterVerificationState ivs = (IntentFilterVerificationState) PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId);
            if (ivs == null) {
                ivs = createDomainVerificationState(verifierUid, userId, verificationId, packageName);
            }
            ivs.addFilter(filter);
            return true;
        }

        private IntentFilterVerificationState createDomainVerificationState(int verifierUid, int userId, int verificationId, String packageName) {
            IntentFilterVerificationState ivs = new IntentFilterVerificationState(verifierUid, userId, packageName);
            ivs.setPendingState();
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mIntentFilterVerificationStates.append(verificationId, ivs);
                this.mCurrentIntentFilterVerifications.add(Integer.valueOf(verificationId));
            }
            return ivs;
        }
    }

    private static class MoveCallbacks extends Handler {
        private static final int MSG_CREATED = 1;
        private static final int MSG_STATUS_CHANGED = 2;
        private final RemoteCallbackList<IPackageMoveObserver> mCallbacks = new RemoteCallbackList();
        private final SparseIntArray mLastStatus = new SparseIntArray();

        public MoveCallbacks(Looper looper) {
            super(looper);
        }

        public void register(IPackageMoveObserver callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IPackageMoveObserver callback) {
            this.mCallbacks.unregister(callback);
        }

        public void handleMessage(Message msg) {
            SomeArgs args = msg.obj;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    invokeCallback((IPackageMoveObserver) this.mCallbacks.getBroadcastItem(i), msg.what, args);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IPackageMoveObserver callback, int what, SomeArgs args) throws RemoteException {
            switch (what) {
                case 1:
                    callback.onCreated(args.argi1, (Bundle) args.arg2);
                    return;
                case 2:
                    callback.onStatusChanged(args.argi1, args.argi2, ((Long) args.arg3).longValue());
                    return;
                default:
                    return;
            }
        }

        private void notifyCreated(int moveId, Bundle extras) {
            Slog.v(PackageManagerService.TAG, "Move " + moveId + " created " + extras.toString());
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.arg2 = extras;
            obtainMessage(1, args).sendToTarget();
        }

        private void notifyStatusChanged(int moveId, int status) {
            notifyStatusChanged(moveId, status, -1);
        }

        private void notifyStatusChanged(int moveId, int status, long estMillis) {
            Slog.v(PackageManagerService.TAG, "Move " + moveId + " status " + status);
            SomeArgs args = SomeArgs.obtain();
            args.argi1 = moveId;
            args.argi2 = status;
            args.arg3 = Long.valueOf(estMillis);
            obtainMessage(2, args).sendToTarget();
            synchronized (this.mLastStatus) {
                this.mLastStatus.put(moveId, status);
            }
        }
    }

    static class MoveInfo {
        final int appId;
        final String dataAppName;
        final String fromUuid;
        final int moveId;
        final String packageName;
        final String seinfo;
        final int targetSdkVersion;
        final String toUuid;

        public MoveInfo(int moveId, String fromUuid, String toUuid, String packageName, String dataAppName, int appId, String seinfo, int targetSdkVersion) {
            this.moveId = moveId;
            this.fromUuid = fromUuid;
            this.toUuid = toUuid;
            this.packageName = packageName;
            this.dataAppName = dataAppName;
            this.appId = appId;
            this.seinfo = seinfo;
            this.targetSdkVersion = targetSdkVersion;
        }
    }

    class MoveInstallArgs extends InstallArgs {
        private File codeFile;
        private File resourceFile;

        MoveInstallArgs(InstallParams params) {
            super(params.origin, params.move, params.observer, params.installFlags, params.installerPackageName, params.volumeUuid, params.getUser(), null, params.packageAbiOverride, params.grantedRuntimePermissions, params.traceMethod, params.traceCookie, params.certificates, params.installReason);
        }

        int copyApk(IMediaContainerService imcs, boolean temp) {
            synchronized (PackageManagerService.this.mInstaller) {
                try {
                    PackageManagerService.this.mInstaller.moveCompleteApp(this.move.fromUuid, this.move.toUuid, this.move.packageName, this.move.dataAppName, this.move.appId, this.move.seinfo, this.move.targetSdkVersion);
                } catch (InstallerException e) {
                    Slog.w(PackageManagerService.TAG, "Failed to move app", e);
                    return RequestStatus.SYS_ETIMEDOUT;
                }
            }
            this.codeFile = new File(Environment.getDataAppDirectory(this.move.toUuid), this.move.dataAppName);
            this.resourceFile = this.codeFile;
            return 1;
        }

        int doPreInstall(int status) {
            if (status != 1) {
                cleanUp(this.move.toUuid);
            }
            return status;
        }

        boolean doRename(int status, Package pkg, String oldCodePath) {
            if (status != 1) {
                cleanUp(this.move.toUuid);
                return false;
            }
            pkg.setApplicationVolumeUuid(pkg.volumeUuid);
            pkg.setApplicationInfoCodePath(pkg.codePath);
            pkg.setApplicationInfoBaseCodePath(pkg.baseCodePath);
            pkg.setApplicationInfoSplitCodePaths(pkg.splitCodePaths);
            pkg.setApplicationInfoResourcePath(pkg.codePath);
            pkg.setApplicationInfoBaseResourcePath(pkg.baseCodePath);
            pkg.setApplicationInfoSplitResourcePaths(pkg.splitCodePaths);
            return true;
        }

        int doPostInstall(int status, int uid) {
            if (status == 1) {
                cleanUp(this.move.fromUuid);
            } else {
                PackageManagerService.this.hwCertCleanUp();
                cleanUp(this.move.toUuid);
            }
            return status;
        }

        String getCodePath() {
            return this.codeFile != null ? this.codeFile.getAbsolutePath() : null;
        }

        String getResourcePath() {
            return this.resourceFile != null ? this.resourceFile.getAbsolutePath() : null;
        }

        private boolean cleanUp(String volumeUuid) {
            File codeFile = new File(Environment.getDataAppDirectory(volumeUuid), this.move.dataAppName);
            Slog.d(PackageManagerService.TAG, "Cleaning up " + this.move.packageName + " on " + volumeUuid);
            synchronized (PackageManagerService.this.mInstallLock) {
                for (int userId : PackageManagerService.sUserManager.getUserIds()) {
                    try {
                        PackageManagerService.this.mInstaller.destroyAppData(volumeUuid, this.move.packageName, userId, 3, 0);
                    } catch (InstallerException e) {
                        Slog.w(PackageManagerService.TAG, String.valueOf(e));
                    }
                }
                PackageManagerService.this.removeCodePathLI(codeFile);
            }
            return true;
        }

        void cleanUpResourcesLI() {
            throw new UnsupportedOperationException();
        }

        boolean doPostDeleteLI(boolean delete) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class OnPermissionChangeListeners extends Handler {
        private static final int MSG_ON_PERMISSIONS_CHANGED = 1;
        private final RemoteCallbackList<IOnPermissionsChangeListener> mPermissionListeners = new RemoteCallbackList();

        public OnPermissionChangeListeners(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    handleOnPermissionsChanged(msg.arg1);
                    return;
                default:
                    return;
            }
        }

        public void addListenerLocked(IOnPermissionsChangeListener listener) {
            this.mPermissionListeners.register(listener);
        }

        public void removeListenerLocked(IOnPermissionsChangeListener listener) {
            this.mPermissionListeners.unregister(listener);
        }

        public void onPermissionsChanged(int uid) {
            if (this.mPermissionListeners.getRegisteredCallbackCount() > 0) {
                obtainMessage(1, uid, 0).sendToTarget();
            }
        }

        private void handleOnPermissionsChanged(int uid) {
            int count = this.mPermissionListeners.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    ((IOnPermissionsChangeListener) this.mPermissionListeners.getBroadcastItem(i)).onPermissionsChanged(uid);
                } catch (RemoteException e) {
                    Log.e(PackageManagerService.TAG, "Permission listener is dead", e);
                } catch (Throwable th) {
                    this.mPermissionListeners.finishBroadcast();
                }
            }
            this.mPermissionListeners.finishBroadcast();
        }
    }

    static class OriginInfo {
        final String cid;
        final boolean existing;
        final File file;
        final File resolvedFile;
        final String resolvedPath;
        final boolean staged;

        static OriginInfo fromNothing() {
            return new OriginInfo(null, null, false, false);
        }

        static OriginInfo fromUntrustedFile(File file) {
            return new OriginInfo(file, null, false, false);
        }

        static OriginInfo fromExistingFile(File file) {
            return new OriginInfo(file, null, false, true);
        }

        static OriginInfo fromStagedFile(File file) {
            return new OriginInfo(file, null, true, false);
        }

        static OriginInfo fromStagedContainer(String cid) {
            return new OriginInfo(null, cid, true, false);
        }

        private OriginInfo(File file, String cid, boolean staged, boolean existing) {
            this.file = file;
            this.cid = cid;
            this.staged = staged;
            this.existing = existing;
            if (cid != null) {
                this.resolvedPath = PackageHelper.getSdDir(cid);
                this.resolvedFile = new File(this.resolvedPath);
            } else if (file != null) {
                this.resolvedPath = file.getAbsolutePath();
                this.resolvedFile = file;
            } else {
                this.resolvedPath = null;
                this.resolvedFile = null;
            }
        }
    }

    private class PackageFreezer implements AutoCloseable {
        private final PackageFreezer[] mChildren;
        private final CloseGuard mCloseGuard;
        private final AtomicBoolean mClosed;
        private final String mPackageName;
        private final boolean mWeFroze;

        public PackageFreezer() {
            this.mClosed = new AtomicBoolean();
            this.mCloseGuard = CloseGuard.get();
            this.mPackageName = null;
            this.mChildren = null;
            this.mWeFroze = false;
            this.mCloseGuard.open("close");
        }

        public PackageFreezer(String packageName, int userId, String killReason) {
            this.mClosed = new AtomicBoolean();
            this.mCloseGuard = CloseGuard.get();
            synchronized (PackageManagerService.this.mPackages) {
                this.mPackageName = packageName;
                this.mWeFroze = PackageManagerService.this.mFrozenPackages.add(this.mPackageName);
                Slog.d(PackageManagerService.TAG, "mFrozenPackages add package:" + this.mPackageName);
                PackageSetting ps = (PackageSetting) PackageManagerService.this.mSettings.mPackages.get(this.mPackageName);
                if (ps != null) {
                    PackageManagerService.this.killApplication(ps.name, ps.appId, userId, killReason);
                }
                Package p = (Package) PackageManagerService.this.mPackages.get(packageName);
                if (p == null || p.childPackages == null) {
                    this.mChildren = null;
                } else {
                    int N = p.childPackages.size();
                    this.mChildren = new PackageFreezer[N];
                    for (int i = 0; i < N; i++) {
                        this.mChildren[i] = new PackageFreezer(((Package) p.childPackages.get(i)).packageName, userId, killReason);
                    }
                }
            }
            this.mCloseGuard.open("close");
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                close();
            } finally {
                super.finalize();
            }
        }

        public void close() {
            int i = 0;
            this.mCloseGuard.close();
            if (this.mClosed.compareAndSet(false, true)) {
                synchronized (PackageManagerService.this.mPackages) {
                    if (this.mWeFroze) {
                        PackageManagerService.this.mFrozenPackages.remove(this.mPackageName);
                        Slog.d(PackageManagerService.TAG, "mFrozenPackages remove package:" + this.mPackageName);
                    }
                    if (this.mChildren != null) {
                        PackageFreezer[] packageFreezerArr = this.mChildren;
                        int length = packageFreezerArr.length;
                        while (i < length) {
                            packageFreezerArr[i].close();
                            i++;
                        }
                    }
                }
            }
        }
    }

    class PackageHandler extends Handler {
        private boolean mBound = false;
        final ArrayList<HandlerParams> mPendingInstalls = new ArrayList();

        private boolean connectToService() {
            Intent service = new Intent().setComponent(PackageManagerService.DEFAULT_CONTAINER_COMPONENT);
            Process.setThreadPriority(0);
            if (PackageManagerService.this.mContext.bindServiceAsUser(service, PackageManagerService.this.mDefContainerConn, 1, UserHandle.SYSTEM)) {
                Process.setThreadPriority(10);
                this.mBound = true;
                return true;
            }
            Process.setThreadPriority(10);
            return false;
        }

        private void disconnectService() {
            PackageManagerService.this.mContainerService = null;
            this.mBound = false;
            Process.setThreadPriority(0);
            PackageManagerService.this.mContext.unbindService(PackageManagerService.this.mDefContainerConn);
            Process.setThreadPriority(10);
        }

        PackageHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            try {
                doHandleMessage(msg);
            } finally {
                Process.setThreadPriority(10);
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        void doHandleMessage(Message msg) {
            int i;
            int uid;
            HandlerParams params;
            Iterator params$iterator;
            int userId;
            InstallArgs args;
            int verificationId;
            PackageVerificationState state;
            Uri originUri;
            int ret;
            switch (msg.what) {
                case 1:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        if (PackageManagerService.this.mPendingBroadcasts != null) {
                            int size = PackageManagerService.this.mPendingBroadcasts.size();
                            if (size > 0) {
                                String[] packages = new String[size];
                                ArrayList<String>[] components = new ArrayList[size];
                                int[] uids = new int[size];
                                i = 0;
                                for (int n = 0; n < PackageManagerService.this.mPendingBroadcasts.userIdCount(); n++) {
                                    int packageUserId = PackageManagerService.this.mPendingBroadcasts.userIdAt(n);
                                    Iterator<Entry<String, ArrayList<String>>> it = PackageManagerService.this.mPendingBroadcasts.packagesForUserId(packageUserId).entrySet().iterator();
                                    while (it.hasNext() && i < size) {
                                        Entry<String, ArrayList<String>> ent = (Entry) it.next();
                                        packages[i] = (String) ent.getKey();
                                        components[i] = (ArrayList) ent.getValue();
                                        PackageSetting ps = (PackageSetting) PackageManagerService.this.mSettings.mPackages.get(ent.getKey());
                                        if (ps != null) {
                                            uid = UserHandle.getUid(packageUserId, ps.appId);
                                        } else {
                                            uid = -1;
                                        }
                                        uids[i] = uid;
                                        i++;
                                    }
                                }
                                size = i;
                                PackageManagerService.this.mPendingBroadcasts.clear();
                                break;
                            }
                            return;
                        }
                        return;
                    }
                    break;
                case 3:
                    if (msg.obj != null) {
                        PackageManagerService.this.mContainerService = (IMediaContainerService) msg.obj;
                        Trace.asyncTraceEnd(262144, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                    }
                    if (PackageManagerService.this.mContainerService != null) {
                        if (this.mPendingInstalls.size() <= 0) {
                            Slog.w(PackageManagerService.TAG, "Empty queue");
                            break;
                        }
                        params = (HandlerParams) this.mPendingInstalls.get(0);
                        if (params != null) {
                            Trace.asyncTraceEnd(262144, "queueInstall", System.identityHashCode(params));
                            Trace.traceBegin(262144, "startCopy");
                            if (params.startCopy()) {
                                if (this.mPendingInstalls.size() > 0) {
                                    this.mPendingInstalls.remove(0);
                                }
                                if (this.mPendingInstalls.size() != 0) {
                                    PackageManagerService.this.mHandler.sendEmptyMessage(3);
                                } else if (this.mBound) {
                                    removeMessages(6);
                                    sendMessageDelayed(obtainMessage(6), 10000);
                                }
                            }
                            Trace.traceEnd(262144);
                            break;
                        }
                    } else if (!this.mBound) {
                        Slog.e(PackageManagerService.TAG, "Cannot bind to media container service");
                        params$iterator = this.mPendingInstalls.iterator();
                        if (!params$iterator.hasNext()) {
                            this.mPendingInstalls.clear();
                            break;
                        }
                        params = (HandlerParams) params$iterator.next();
                        params.serviceError();
                        Trace.asyncTraceEnd(262144, "queueInstall", System.identityHashCode(params));
                        if (params.traceMethod != null) {
                            Trace.asyncTraceEnd(262144, params.traceMethod, params.traceCookie);
                        }
                        return;
                    } else {
                        Slog.w(PackageManagerService.TAG, "Waiting to connect to media container service");
                        break;
                    }
                    break;
                case 5:
                    params = msg.obj;
                    int idx = this.mPendingInstalls.size();
                    if (!this.mBound) {
                        Trace.asyncTraceBegin(262144, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                        if (connectToService()) {
                            this.mPendingInstalls.add(idx, params);
                            break;
                        }
                        Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                        params.serviceError();
                        Trace.asyncTraceEnd(262144, "bindingMCS", System.identityHashCode(PackageManagerService.this.mHandler));
                        if (params.traceMethod != null) {
                            Trace.asyncTraceEnd(262144, params.traceMethod, params.traceCookie);
                        }
                        return;
                    }
                    this.mPendingInstalls.add(idx, params);
                    if (idx == 0) {
                        PackageManagerService.this.mHandler.sendEmptyMessage(3);
                        break;
                    }
                    break;
                case 6:
                    if (this.mPendingInstalls.size() != 0 || PackageManagerService.this.mPendingVerification.size() != 0) {
                        if (this.mPendingInstalls.size() > 0) {
                            PackageManagerService.this.mHandler.sendEmptyMessage(3);
                            break;
                        }
                    } else if (this.mBound) {
                        disconnectService();
                        break;
                    }
                    break;
                case 7:
                    Process.setThreadPriority(0);
                    String packageName = msg.obj;
                    userId = msg.arg1;
                    boolean andCode = msg.arg2 != 0;
                    synchronized (PackageManagerService.this.mPackages) {
                        if (userId == -1) {
                            for (int user : PackageManagerService.sUserManager.getUserIds()) {
                                PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(user, packageName, andCode));
                            }
                        } else {
                            PackageManagerService.this.mSettings.addPackageToCleanLPw(new PackageCleanItem(userId, packageName, andCode));
                        }
                    }
                    Process.setThreadPriority(10);
                    PackageManagerService.this.startCleaningPackages();
                    break;
                case 9:
                    PostInstallData data = (PostInstallData) PackageManagerService.this.mRunningInstalls.get(msg.arg1);
                    boolean didRestore = msg.arg2 != 0;
                    PackageManagerService.this.mRunningInstalls.delete(msg.arg1);
                    if (data != null) {
                        args = data.args;
                        PackageInstalledInfo parentRes = data.res;
                        if (1 != parentRes.returnCode) {
                            PackageManagerService.this.uploadInstallErrRadar("{pkg:" + parentRes.name + "pkgName:" + args.packageName + ",Ver:" + args.packageVersion + ",ErrNum:" + parentRes.returnCode + ",Intaller:" + args.installerPackageName + ",Msg:" + parentRes.returnMsg + "}");
                        }
                        boolean grantPermissions = (args.installFlags & 256) != 0;
                        if (parentRes.pkg != null) {
                            PackageManagerService.this.addGrantedInstalledPkg(parentRes.pkg.packageName, grantPermissions);
                        }
                        boolean killApp = (args.installFlags & 4096) == 0;
                        boolean virtualPreload = (args.installFlags & 65536) != 0;
                        String[] grantedPermissions = args.installGrantPermissions;
                        PackageManagerService.this.handlePackagePostInstall(parentRes, grantPermissions, killApp, virtualPreload, grantedPermissions, didRestore, args.installerPackageName, args.observer);
                        int childCount = parentRes.addedChildPackages != null ? parentRes.addedChildPackages.size() : 0;
                        for (i = 0; i < childCount; i++) {
                            PackageManagerService.this.handlePackagePostInstall((PackageInstalledInfo) parentRes.addedChildPackages.valueAt(i), grantPermissions, killApp, virtualPreload, grantedPermissions, false, args.installerPackageName, args.observer);
                        }
                        if (parentRes.pkg != null) {
                            uid = (PackageManagerService.isSystemApp(parentRes.pkg) && (parentRes.pkg.applicationInfo.hwFlags & 33554432) == 0) ? (parentRes.pkg.applicationInfo.hwFlags & 67108864) == 0 ? 1 : 0 : 0;
                            if ((uid ^ 1) != 0) {
                                PackageManagerService.this.parseInstalledPkgInfo(args, parentRes);
                            }
                        }
                        SmartShrinker.reclaim(Process.myPid(), 3);
                        if (args.traceMethod != null) {
                            Trace.asyncTraceEnd(262144, args.traceMethod, args.traceCookie);
                        }
                    } else {
                        Slog.e(PackageManagerService.TAG, "Bogus post-install token " + msg.arg1);
                    }
                    Trace.asyncTraceEnd(262144, "postInstall", msg.arg1);
                    break;
                case 10:
                    if (this.mPendingInstalls.size() > 0) {
                        if (this.mBound) {
                            disconnectService();
                        }
                        if (!connectToService()) {
                            Slog.e(PackageManagerService.TAG, "Failed to bind to media container service");
                            for (HandlerParams params2 : this.mPendingInstalls) {
                                params2.serviceError();
                                Trace.asyncTraceEnd(262144, "queueInstall", System.identityHashCode(params2));
                            }
                            this.mPendingInstalls.clear();
                            break;
                        }
                    }
                    break;
                case 11:
                    Trace.asyncTraceEnd(262144, "queueInstall", System.identityHashCode((HandlerParams) this.mPendingInstalls.remove(0)));
                    break;
                case 12:
                    boolean reportStatus = msg.arg1 == 1;
                    if (msg.arg2 == 1) {
                        Runtime.getRuntime().gc();
                    }
                    if (msg.obj != null) {
                        PackageManagerService.this.unloadAllContainers(msg.obj);
                    }
                    if (reportStatus) {
                        try {
                            PackageHelper.getStorageManager().finishMediaUpdate();
                            break;
                        } catch (RemoteException e) {
                            Log.e(PackageManagerService.TAG, "StorageManagerService not running?");
                            break;
                        }
                    }
                    break;
                case 13:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(13);
                        removeMessages(14);
                        PackageManagerService.this.mSettings.writeLPr();
                        PackageManagerService.this.mDirtyUsers.clear();
                    }
                    Process.setThreadPriority(10);
                    break;
                case 14:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(14);
                        for (Integer intValue : PackageManagerService.this.mDirtyUsers) {
                            PackageManagerService.this.mSettings.writePackageRestrictionsLPr(intValue.intValue());
                        }
                        PackageManagerService.this.mDirtyUsers.clear();
                    }
                    Process.setThreadPriority(10);
                    break;
                case 15:
                    verificationId = msg.arg1;
                    state = (PackageVerificationState) PackageManagerService.this.mPendingVerification.get(verificationId);
                    if (state != null) {
                        PackageVerificationResponse response = msg.obj;
                        state.setVerifierResponse(response.callerUid, response.code);
                        if (state.isVerificationComplete()) {
                            PackageManagerService.this.mPendingVerification.remove(verificationId);
                            args = state.getInstallArgs();
                            originUri = Uri.fromFile(args.origin.resolvedFile);
                            if (state.isInstallAllowed()) {
                                ret = RequestStatus.SYS_ETIMEDOUT;
                                PackageManagerService.this.broadcastPackageVerified(verificationId, originUri, response.code, state.getInstallArgs().getUser());
                                try {
                                    ret = args.copyApk(PackageManagerService.this.mContainerService, true);
                                } catch (RemoteException e2) {
                                    Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                                }
                            } else {
                                ret = -22;
                            }
                            Trace.asyncTraceEnd(262144, "verification", verificationId);
                            PackageManagerService.this.processPendingInstall(args, ret);
                            PackageManagerService.this.mHandler.sendEmptyMessage(6);
                            break;
                        }
                    }
                    Slog.w(PackageManagerService.TAG, "Invalid verification token " + verificationId + " received");
                    break;
                    break;
                case 16:
                    verificationId = msg.arg1;
                    state = (PackageVerificationState) PackageManagerService.this.mPendingVerification.get(verificationId);
                    if (!(state == null || (state.timeoutExtended() ^ 1) == 0)) {
                        args = state.getInstallArgs();
                        originUri = Uri.fromFile(args.origin.resolvedFile);
                        Slog.i(PackageManagerService.TAG, "Verification timed out for " + originUri);
                        PackageManagerService.this.mPendingVerification.remove(verificationId);
                        ret = -22;
                        UserHandle user2 = args.getUser();
                        if (PackageManagerService.this.getDefaultVerificationResponse(user2) == 1) {
                            Slog.i(PackageManagerService.TAG, "Continuing with installation of " + originUri);
                            state.setVerifierResponse(Binder.getCallingUid(), 2);
                            PackageManagerService.this.broadcastPackageVerified(verificationId, originUri, 1, user2);
                            try {
                                ret = args.copyApk(PackageManagerService.this.mContainerService, true);
                            } catch (RemoteException e3) {
                                Slog.e(PackageManagerService.TAG, "Could not contact the ContainerService");
                            }
                        } else {
                            PackageManagerService.this.broadcastPackageVerified(verificationId, originUri, -1, user2);
                        }
                        Trace.asyncTraceEnd(262144, "verification", verificationId);
                        PackageManagerService.this.processPendingInstall(args, ret);
                        PackageManagerService.this.mHandler.sendEmptyMessage(6);
                        break;
                    }
                case 17:
                    IFVerificationParams params3 = msg.obj;
                    PackageManagerService.this.verifyIntentFiltersIfNeeded(params3.userId, params3.verifierUid, params3.replacing, params3.pkg);
                    break;
                case 18:
                    verificationId = msg.arg1;
                    IntentFilterVerificationState state2 = (IntentFilterVerificationState) PackageManagerService.this.mIntentFilterVerificationStates.get(verificationId);
                    if (state2 != null) {
                        userId = state2.getUserId();
                        IntentFilterVerificationResponse response2 = msg.obj;
                        state2.setVerifierResponse(response2.callerUid, response2.code);
                        uid = response2.code;
                        if (state2.isVerificationComplete()) {
                            PackageManagerService.this.mIntentFilterVerifier.receiveVerificationResponse(verificationId);
                            break;
                        }
                    }
                    Slog.w(PackageManagerService.TAG, "Invalid IntentFilter verification token " + verificationId + " received");
                    break;
                    break;
                case 19:
                    Process.setThreadPriority(0);
                    synchronized (PackageManagerService.this.mPackages) {
                        removeMessages(19);
                        PackageManagerService.this.mSettings.writePackageListLPr(msg.arg1);
                    }
                    Process.setThreadPriority(10);
                    break;
                case 20:
                    InstantAppResolver.doInstantAppResolutionPhaseTwo(PackageManagerService.this.mContext, PackageManagerService.this.mInstantAppResolverConnection, (InstantAppRequest) msg.obj, PackageManagerService.this.mInstantAppInstallerActivity, PackageManagerService.this.mHandler);
                    break;
            }
        }
    }

    static class PackageInstalledInfo {
        ArrayMap<String, PackageInstalledInfo> addedChildPackages;
        String installerPackageName;
        String name;
        int[] newUsers;
        String origPackage;
        String origPermission;
        int[] origUsers;
        Package pkg;
        PackageRemovedInfo removedInfo;
        int returnCode;
        String returnMsg;
        int uid;

        PackageInstalledInfo() {
        }

        public void setError(int code, String msg) {
            setReturnCode(code);
            setReturnMessage(msg);
            Slog.w(PackageManagerService.TAG, msg);
        }

        public void setError(String msg, PackageParserException e) {
            setReturnCode(e.error);
            setReturnMessage(ExceptionUtils.getCompleteMessage(msg, e));
            int childCount = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                ((PackageInstalledInfo) this.addedChildPackages.valueAt(i)).setError(msg, e);
            }
            Slog.w(PackageManagerService.TAG, msg, e);
        }

        public void setError(String msg, PackageManagerException e) {
            this.returnCode = e.error;
            setReturnMessage(ExceptionUtils.getCompleteMessage(msg, e));
            int childCount = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                ((PackageInstalledInfo) this.addedChildPackages.valueAt(i)).setError(msg, e);
            }
            Slog.w(PackageManagerService.TAG, msg, e);
        }

        public void setReturnCode(int returnCode) {
            this.returnCode = returnCode;
            int childCount = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                ((PackageInstalledInfo) this.addedChildPackages.valueAt(i)).returnCode = returnCode;
            }
        }

        private void setReturnMessage(String returnMsg) {
            this.returnMsg = returnMsg;
            int childCount = this.addedChildPackages != null ? this.addedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                ((PackageInstalledInfo) this.addedChildPackages.valueAt(i)).returnMsg = returnMsg;
            }
        }
    }

    private class PackageManagerInternalImpl extends PackageManagerInternal {
        private PackageManagerInternalImpl() {
        }

        public void setLocationPackagesProvider(PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setLocationPackagesProviderLPw(provider);
            }
        }

        public void setVoiceInteractionPackagesProvider(PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setVoiceInteractionPackagesProviderLPw(provider);
            }
        }

        public void setSmsAppPackagesProvider(PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setSmsAppPackagesProviderLPw(provider);
            }
        }

        public void setDialerAppPackagesProvider(PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setDialerAppPackagesProviderLPw(provider);
            }
        }

        public void setSimCallManagerPackagesProvider(PackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setSimCallManagerPackagesProviderLPw(provider);
            }
        }

        public void setSyncAdapterPackagesprovider(SyncAdapterPackagesProvider provider) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.setSyncAdapterPackagesProviderLPw(provider);
            }
        }

        public void grantDefaultPermissionsToDefaultSmsApp(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultSmsAppLPr(packageName, userId);
            }
        }

        public void grantDefaultPermissionsToDefaultDialerApp(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mSettings.setDefaultDialerPackageNameLPw(packageName, userId);
                PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultDialerAppLPr(packageName, userId);
            }
        }

        public void grantDefaultPermissionsToDefaultSimCallManager(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultSimCallManagerLPr(packageName, userId);
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void setKeepUninstalledPackages(List<String> packageList) {
            Throwable th;
            Preconditions.checkNotNull(packageList);
            List list = null;
            synchronized (PackageManagerService.this.mPackages) {
                int i;
                if (PackageManagerService.this.mKeepUninstalledPackages != null) {
                    int packagesCount = PackageManagerService.this.mKeepUninstalledPackages.size();
                    i = 0;
                    List<String> removedFromList = null;
                    while (i < packagesCount) {
                        List<String> removedFromList2;
                        try {
                            String oldPackage = (String) PackageManagerService.this.mKeepUninstalledPackages.get(i);
                            if (packageList == null || !packageList.contains(oldPackage)) {
                                if (removedFromList == null) {
                                    removedFromList2 = new ArrayList();
                                } else {
                                    removedFromList2 = removedFromList;
                                }
                                try {
                                    removedFromList2.add(oldPackage);
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } else {
                                removedFromList2 = removedFromList;
                            }
                            i++;
                            removedFromList = removedFromList2;
                        } catch (Throwable th3) {
                            th = th3;
                            removedFromList2 = removedFromList;
                        }
                    }
                    list = removedFromList;
                }
                PackageManagerService.this.mKeepUninstalledPackages = new ArrayList(packageList);
                if (list != null) {
                    int removedCount = list.size();
                    for (i = 0; i < removedCount; i++) {
                        PackageManagerService.this.deletePackageIfUnusedLPr((String) list.get(i));
                    }
                }
            }
            throw th;
        }

        public boolean isPermissionsReviewRequired(String packageName, int userId) {
            synchronized (PackageManagerService.this.mPackages) {
                if (PackageManagerService.this.mPermissionReviewRequired) {
                    PackageSetting packageSetting = (PackageSetting) PackageManagerService.this.mSettings.mPackages.get(packageName);
                    if (packageSetting == null) {
                        return false;
                    } else if (packageSetting.pkg.applicationInfo.targetSdkVersion >= 23) {
                        return false;
                    } else {
                        boolean isPermissionReviewRequired = packageSetting.getPermissionsState().isPermissionReviewRequired(userId);
                        return isPermissionReviewRequired;
                    }
                }
                return false;
            }
        }

        public PackageInfo getPackageInfo(String packageName, int flags, int filterCallingUid, int userId) {
            return PackageManagerService.this.getPackageInfoInternal(packageName, -1, flags, filterCallingUid, userId);
        }

        public ApplicationInfo getApplicationInfo(String packageName, int flags, int filterCallingUid, int userId) {
            return PackageManagerService.this.getApplicationInfoInternal(packageName, flags, filterCallingUid, userId);
        }

        public ActivityInfo getActivityInfo(ComponentName component, int flags, int filterCallingUid, int userId) {
            return PackageManagerService.this.getActivityInfoInternal(component, flags, filterCallingUid, userId);
        }

        public List<ResolveInfo> queryIntentActivities(Intent intent, int flags, int filterCallingUid, int userId) {
            return PackageManagerService.this.queryIntentActivitiesInternal(intent, intent.resolveTypeIfNeeded(PackageManagerService.this.mContext.getContentResolver()), flags, filterCallingUid, userId, false, true);
        }

        public ComponentName getHomeActivitiesAsUser(List<ResolveInfo> allHomeCandidates, int userId) {
            return PackageManagerService.this.getHomeActivitiesAsUser(allHomeCandidates, userId);
        }

        public void setDeviceAndProfileOwnerPackages(int deviceOwnerUserId, String deviceOwnerPackage, SparseArray<String> profileOwnerPackages) {
            PackageManagerService.this.mProtectedPackages.setDeviceAndProfileOwnerPackages(deviceOwnerUserId, deviceOwnerPackage, profileOwnerPackages);
        }

        public boolean isPackageDataProtected(int userId, String packageName) {
            return PackageManagerService.this.mProtectedPackages.isPackageDataProtected(userId, packageName);
        }

        public boolean isPackageEphemeral(int userId, String packageName) {
            boolean instantApp;
            synchronized (PackageManagerService.this.mPackages) {
                PackageSetting ps = (PackageSetting) PackageManagerService.this.mSettings.mPackages.get(packageName);
                instantApp = ps != null ? ps.getInstantApp(userId) : false;
            }
            return instantApp;
        }

        public boolean wasPackageEverLaunched(String packageName, int userId) {
            boolean wasPackageEverLaunchedLPr;
            synchronized (PackageManagerService.this.mPackages) {
                wasPackageEverLaunchedLPr = PackageManagerService.this.mSettings.wasPackageEverLaunchedLPr(packageName, userId);
            }
            return wasPackageEverLaunchedLPr;
        }

        public void grantRuntimePermission(String packageName, String name, int userId, boolean overridePolicy) {
            PackageManagerService.this.grantRuntimePermission(packageName, name, userId, overridePolicy);
        }

        public void revokeRuntimePermission(String packageName, String name, int userId, boolean overridePolicy) {
            PackageManagerService.this.revokeRuntimePermission(packageName, name, userId, overridePolicy);
        }

        public String getNameForUid(int uid) {
            return PackageManagerService.this.getNameForUid(uid);
        }

        public void requestInstantAppResolutionPhaseTwo(AuxiliaryResolveInfo responseObj, Intent origIntent, String resolvedType, String callingPackage, Bundle verificationBundle, int userId) {
            PackageManagerService.this.requestInstantAppResolutionPhaseTwo(responseObj, origIntent, resolvedType, callingPackage, verificationBundle, userId);
        }

        public void grantEphemeralAccess(int userId, Intent intent, int targetAppId, int ephemeralAppId) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mInstantAppRegistry.grantInstantAccessLPw(userId, intent, targetAppId, ephemeralAppId);
            }
        }

        public boolean isInstantAppInstallerComponent(ComponentName component) {
            boolean equals;
            synchronized (PackageManagerService.this.mPackages) {
                if (PackageManagerService.this.mInstantAppInstallerActivity != null) {
                    equals = PackageManagerService.this.mInstantAppInstallerActivity.getComponentName().equals(component);
                } else {
                    equals = false;
                }
            }
            return equals;
        }

        public void pruneInstantApps() {
            PackageManagerService.this.mInstantAppRegistry.pruneInstantApps();
        }

        public String getSetupWizardPackageName() {
            return PackageManagerService.this.mSetupWizardPackage;
        }

        public void setExternalSourcesPolicy(ExternalSourcesPolicy policy) {
            if (policy != null) {
                PackageManagerService.this.mExternalSourcesPolicy = policy;
            }
        }

        public boolean isPackagePersistent(String packageName) {
            boolean z = false;
            synchronized (PackageManagerService.this.mPackages) {
                Package pkg = (Package) PackageManagerService.this.mPackages.get(packageName);
                if (pkg != null && (pkg.applicationInfo.flags & 9) == 9) {
                    z = true;
                }
            }
            return z;
        }

        public List<PackageInfo> getOverlayPackages(int userId) {
            ArrayList<PackageInfo> overlayPackages = new ArrayList();
            synchronized (PackageManagerService.this.mPackages) {
                for (Package p : PackageManagerService.this.mPackages.values()) {
                    if (p.mOverlayTarget != null) {
                        PackageInfo pkg = PackageManagerService.this.generatePackageInfo((PackageSetting) p.mExtras, 0, userId);
                        if (pkg != null) {
                            overlayPackages.add(pkg);
                        }
                    }
                }
            }
            return overlayPackages;
        }

        public List<String> getTargetPackageNames(int userId) {
            List<String> targetPackages = new ArrayList();
            synchronized (PackageManagerService.this.mPackages) {
                for (Package p : PackageManagerService.this.mPackages.values()) {
                    if (p.mOverlayTarget == null) {
                        targetPackages.add(p.packageName);
                    }
                }
            }
            return targetPackages;
        }

        public boolean setEnabledOverlayPackages(int userId, String targetPackageName, List<String> overlayPackageNames) {
            synchronized (PackageManagerService.this.mPackages) {
                if (targetPackageName != null) {
                    if (PackageManagerService.this.mPackages.get(targetPackageName) != null) {
                        List list = null;
                        if (overlayPackageNames != null) {
                            if (overlayPackageNames.size() > 0) {
                                int N = overlayPackageNames.size();
                                list = new ArrayList(N);
                                for (int i = 0; i < N; i++) {
                                    String packageName = (String) overlayPackageNames.get(i);
                                    Package pkg = (Package) PackageManagerService.this.mPackages.get(packageName);
                                    if (pkg == null) {
                                        Slog.e(PackageManagerService.TAG, "failed to find package " + packageName);
                                        return false;
                                    }
                                    list.add(pkg.baseCodePath);
                                }
                            }
                        }
                        ((PackageSetting) PackageManagerService.this.mSettings.mPackages.get(targetPackageName)).setOverlayPaths(list, userId);
                        return true;
                    }
                }
                Slog.e(PackageManagerService.TAG, "failed to find package " + targetPackageName);
                return false;
            }
        }

        public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
            return PackageManagerService.this.resolveIntentInternal(intent, resolvedType, flags, userId, true);
        }

        public ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId, int callingUid) {
            return PackageManagerService.this.resolveServiceInternal(intent, resolvedType, flags, userId, callingUid);
        }

        public void addIsolatedUid(int isolatedUid, int ownerUid) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mIsolatedOwners.put(isolatedUid, ownerUid);
            }
        }

        public void removeIsolatedUid(int isolatedUid) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.mIsolatedOwners.delete(isolatedUid);
            }
        }

        public boolean isInMWPortraitWhiteList(String packageName) {
            return PackageManagerService.this.isInMWPortraitWhiteList(packageName);
        }

        public int getUidTargetSdkVersion(int uid) {
            int -wrap19;
            synchronized (PackageManagerService.this.mPackages) {
                -wrap19 = PackageManagerService.this.getUidTargetSdkVersionLockedLPr(uid);
            }
            return -wrap19;
        }

        public boolean canAccessInstantApps(int callingUid, int userId) {
            return PackageManagerService.this.canViewInstantApps(callingUid, userId);
        }

        public boolean hasInstantApplicationMetadata(String packageName, int userId) {
            boolean hasInstantApplicationMetadataLPr;
            synchronized (PackageManagerService.this.mPackages) {
                hasInstantApplicationMetadataLPr = PackageManagerService.this.mInstantAppRegistry.hasInstantApplicationMetadataLPr(packageName, userId);
            }
            return hasInstantApplicationMetadataLPr;
        }

        public void notifyPackageUse(String packageName, int reason) {
            synchronized (PackageManagerService.this.mPackages) {
                PackageManagerService.this.notifyPackageUseLocked(packageName, reason);
            }
        }

        public float getUserMaxAspectRatio(String packageName) {
            return PackageManagerService.this.getApplicationMaxAspectRatio(packageName);
        }

        public void checkPackageStartable(String packageName, int userId) {
            PackageManagerService.this.checkPackageStartable(packageName, userId);
        }
    }

    private class PackageManagerNative extends IPackageManagerNative.Stub {
        private PackageManagerNative() {
        }

        public String[] getNamesForUids(int[] uids) throws RemoteException {
            String[] results = PackageManagerService.this.getNamesForUids(uids);
            for (int i = results.length - 1; i >= 0; i--) {
                if (results[i] == null) {
                    results[i] = "";
                }
            }
            return results;
        }

        public String getInstallerForPackage(String packageName) throws RemoteException {
            String installerName = PackageManagerService.this.getInstallerPackageName(packageName);
            if (!TextUtils.isEmpty(installerName)) {
                return installerName;
            }
            ApplicationInfo appInfo = PackageManagerService.this.getApplicationInfo(packageName, 0, UserHandle.getUserId(Binder.getCallingUid()));
            if (appInfo == null || (appInfo.flags & 1) == 0) {
                return "";
            }
            return "preload";
        }

        public int getVersionCodeForPackage(String packageName) throws RemoteException {
            try {
                PackageInfo pInfo = PackageManagerService.this.getPackageInfo(packageName, 0, UserHandle.getUserId(Binder.getCallingUid()));
                if (pInfo != null) {
                    return pInfo.versionCode;
                }
            } catch (Exception e) {
            }
            return 0;
        }
    }

    class PackageParserCallback implements Callback {
        PackageParserCallback() {
        }

        public final boolean hasFeature(String feature) {
            return PackageManagerService.this.hasSystemFeature(feature, 0);
        }

        final List<Package> getStaticOverlayPackagesLocked(Collection<Package> allPackages, String targetPackageName) {
            List<Package> overlayPackages = null;
            for (Package p : allPackages) {
                if (targetPackageName.equals(p.mOverlayTarget) && p.mIsStaticOverlay) {
                    if (overlayPackages == null) {
                        overlayPackages = new ArrayList();
                    }
                    overlayPackages.add(p);
                }
            }
            if (overlayPackages != null) {
                Collections.sort(overlayPackages, new Comparator<Package>() {
                    public int compare(Package p1, Package p2) {
                        return p1.mOverlayPriority - p2.mOverlayPriority;
                    }
                });
            }
            return overlayPackages;
        }

        final String[] getStaticOverlayPathsLocked(Collection<Package> allPackages, String targetPackageName, String targetPath) {
            String[] strArr = null;
            if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(targetPackageName)) {
                return null;
            }
            List<Package> overlayPackages = getStaticOverlayPackagesLocked(allPackages, targetPackageName);
            if (overlayPackages == null || overlayPackages.isEmpty()) {
                return null;
            }
            List overlayPathList = null;
            for (Package overlayPackage : overlayPackages) {
                if (targetPath == null) {
                    if (overlayPathList == null) {
                        overlayPathList = new ArrayList();
                    }
                    overlayPathList.add(overlayPackage.baseCodePath);
                } else {
                    try {
                        PackageManagerService.this.mInstaller.idmap(targetPath, overlayPackage.baseCodePath, UserHandle.getSharedAppGid(UserHandle.getUserGid(0)));
                        if (overlayPathList == null) {
                            overlayPathList = new ArrayList();
                        }
                        overlayPathList.add(overlayPackage.baseCodePath);
                    } catch (InstallerException e) {
                        Slog.e(PackageManagerService.TAG, "Failed to generate idmap for " + targetPath + " and " + overlayPackage.baseCodePath);
                    }
                }
            }
            if (overlayPathList != null) {
                strArr = (String[]) overlayPathList.toArray(new String[0]);
            }
            return strArr;
        }

        String[] getStaticOverlayPaths(String targetPackageName, String targetPath) {
            String[] staticOverlayPathsLocked;
            synchronized (PackageManagerService.this.mPackages) {
                staticOverlayPathsLocked = getStaticOverlayPathsLocked(PackageManagerService.this.mPackages.values(), targetPackageName, targetPath);
            }
            return staticOverlayPathsLocked;
        }

        public final String[] getOverlayApks(String targetPackageName) {
            return getStaticOverlayPaths(targetPackageName, null);
        }

        public final String[] getOverlayPaths(String targetPackageName, String targetPath) {
            return getStaticOverlayPaths(targetPackageName, targetPath);
        }
    }

    static class PackageRemovedInfo {
        ArrayMap<String, PackageInstalledInfo> appearedChildPackages;
        InstallArgs args = null;
        int[] broadcastUsers = null;
        boolean dataRemoved;
        SparseArray<Integer> installReasons;
        String installerPackageName;
        boolean isRemovedPackageSystemUpdate = false;
        boolean isStaticSharedLib;
        boolean isUpdate;
        int[] origUsers;
        final PackageSender packageSender;
        int removedAppId = -1;
        ArrayMap<String, PackageRemovedInfo> removedChildPackages;
        boolean removedForAllUsers;
        String removedPackage;
        int[] removedUsers = null;
        int uid = -1;

        PackageRemovedInfo(PackageSender packageSender) {
            this.packageSender = packageSender;
        }

        void sendPackageRemovedBroadcasts(boolean killApp) {
            sendPackageRemovedBroadcastInternal(killApp);
            int childCount = this.removedChildPackages != null ? this.removedChildPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                ((PackageRemovedInfo) this.removedChildPackages.valueAt(i)).sendPackageRemovedBroadcastInternal(killApp);
            }
        }

        void sendSystemPackageUpdatedBroadcasts() {
            if (this.isRemovedPackageSystemUpdate) {
                sendSystemPackageUpdatedBroadcastsInternal();
                int childCount = this.removedChildPackages != null ? this.removedChildPackages.size() : 0;
                for (int i = 0; i < childCount; i++) {
                    PackageRemovedInfo childInfo = (PackageRemovedInfo) this.removedChildPackages.valueAt(i);
                    if (childInfo.isRemovedPackageSystemUpdate) {
                        childInfo.sendSystemPackageUpdatedBroadcastsInternal();
                    }
                }
            }
        }

        void sendSystemPackageAppearedBroadcasts() {
            int packageCount = this.appearedChildPackages != null ? this.appearedChildPackages.size() : 0;
            for (int i = 0; i < packageCount; i++) {
                PackageInstalledInfo installedInfo = (PackageInstalledInfo) this.appearedChildPackages.valueAt(i);
                this.packageSender.sendPackageAddedForNewUsers(installedInfo.name, true, false, UserHandle.getAppId(installedInfo.uid), installedInfo.newUsers);
            }
        }

        private void sendSystemPackageUpdatedBroadcastsInternal() {
            Bundle extras = new Bundle(2);
            extras.putInt("android.intent.extra.UID", this.removedAppId >= 0 ? this.removedAppId : this.uid);
            extras.putBoolean("android.intent.extra.REPLACING", true);
            this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", this.removedPackage, extras, 0, null, null, null);
            this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", this.removedPackage, extras, 0, null, null, null);
            this.packageSender.sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, this.removedPackage, null, null);
            if (this.installerPackageName != null) {
                this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", this.removedPackage, extras, 0, this.installerPackageName, null, null);
                this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", this.removedPackage, extras, 0, this.installerPackageName, null, null);
            }
        }

        private void sendPackageRemovedBroadcastInternal(boolean killApp) {
            if (!this.isStaticSharedLib) {
                Bundle extras = new Bundle(2);
                extras.putInt("android.intent.extra.UID", this.removedAppId >= 0 ? this.removedAppId : this.uid);
                extras.putBoolean("android.intent.extra.DATA_REMOVED", this.dataRemoved);
                extras.putBoolean("android.intent.extra.DONT_KILL_APP", killApp ^ 1);
                if (this.isUpdate || this.isRemovedPackageSystemUpdate) {
                    extras.putBoolean("android.intent.extra.REPLACING", true);
                }
                extras.putBoolean("android.intent.extra.REMOVED_FOR_ALL_USERS", this.removedForAllUsers);
                if (this.removedPackage != null) {
                    this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", this.removedPackage, extras, 0, null, null, this.broadcastUsers);
                    if (this.installerPackageName != null) {
                        this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_REMOVED", this.removedPackage, extras, 0, this.installerPackageName, null, this.broadcastUsers);
                    }
                    if (this.dataRemoved && (this.isRemovedPackageSystemUpdate ^ 1) != 0) {
                        this.packageSender.sendPackageBroadcast("android.intent.action.PACKAGE_FULLY_REMOVED", this.removedPackage, extras, 16777216, null, null, this.broadcastUsers);
                    }
                    if ((this.packageSender instanceof PackageManagerService) && (this.isUpdate ^ 1) != 0) {
                        ((PackageManagerService) this.packageSender).mHwPMSEx.updateNochScreenWhite(this.removedPackage, "removed", 0);
                    }
                }
                if (this.removedAppId >= 0) {
                    this.packageSender.sendPackageBroadcast("android.intent.action.UID_REMOVED", null, extras, 16777216, null, null, this.broadcastUsers);
                }
                if (this.packageSender instanceof PackageManagerService) {
                    ((PackageManagerService) this.packageSender).deleteClonedProfileIfNeed(this.broadcastUsers);
                }
            }
        }

        void populateUsers(int[] userIds, PackageSetting deletedPackageSetting) {
            this.removedUsers = userIds;
            if (this.removedUsers == null) {
                this.broadcastUsers = null;
                return;
            }
            this.broadcastUsers = PackageManagerService.EMPTY_INT_ARRAY;
            for (int i = userIds.length - 1; i >= 0; i--) {
                int userId = userIds[i];
                if (!deletedPackageSetting.getInstantApp(userId)) {
                    this.broadcastUsers = ArrayUtils.appendInt(this.broadcastUsers, userId);
                }
            }
        }
    }

    class ParallelPackageParserCallback extends PackageParserCallback {
        List<Package> mOverlayPackages = null;

        ParallelPackageParserCallback() {
            super();
        }

        void findStaticOverlayPackages() {
            synchronized (PackageManagerService.this.mPackages) {
                for (Package p : PackageManagerService.this.mPackages.values()) {
                    if (p.mIsStaticOverlay) {
                        if (this.mOverlayPackages == null) {
                            this.mOverlayPackages = new ArrayList();
                        }
                        this.mOverlayPackages.add(p);
                    }
                }
            }
        }

        synchronized String[] getStaticOverlayPaths(String targetPackageName, String targetPath) {
            String[] strArr = null;
            synchronized (this) {
                if (this.mOverlayPackages != null) {
                    strArr = getStaticOverlayPathsLocked(this.mOverlayPackages, targetPackageName, targetPath);
                }
            }
            return strArr;
        }
    }

    static class PendingPackageBroadcasts {
        final SparseArray<ArrayMap<String, ArrayList<String>>> mUidMap = new SparseArray(2);

        public ArrayList<String> get(int userId, String packageName) {
            return (ArrayList) getOrAllocate(userId).get(packageName);
        }

        public void put(int userId, String packageName, ArrayList<String> components) {
            getOrAllocate(userId).put(packageName, components);
        }

        public void remove(int userId, String packageName) {
            ArrayMap<String, ArrayList<String>> packages = (ArrayMap) this.mUidMap.get(userId);
            if (packages != null) {
                packages.remove(packageName);
            }
        }

        public void remove(int userId) {
            this.mUidMap.remove(userId);
        }

        public int userIdCount() {
            return this.mUidMap.size();
        }

        public int userIdAt(int n) {
            return this.mUidMap.keyAt(n);
        }

        public ArrayMap<String, ArrayList<String>> packagesForUserId(int userId) {
            return (ArrayMap) this.mUidMap.get(userId);
        }

        public int size() {
            int num = 0;
            for (int i = 0; i < this.mUidMap.size(); i++) {
                num += ((ArrayMap) this.mUidMap.valueAt(i)).size();
            }
            return num;
        }

        public void clear() {
            this.mUidMap.clear();
        }

        private ArrayMap<String, ArrayList<String>> getOrAllocate(int userId) {
            ArrayMap<String, ArrayList<String>> map = (ArrayMap) this.mUidMap.get(userId);
            if (map != null) {
                return map;
            }
            map = new ArrayMap();
            this.mUidMap.put(userId, map);
            return map;
        }
    }

    static class PostInstallData {
        public InstallArgs args;
        public PackageInstalledInfo res;

        PostInstallData(InstallArgs _a, PackageInstalledInfo _r) {
            this.args = _a;
            this.res = _r;
        }
    }

    private final class ProviderIntentResolver extends IntentResolver<ProviderIntentInfo, ResolveInfo> {
        private int mFlags;
        private final ArrayMap<ComponentName, Provider> mProviders;

        private ProviderIntentResolver() {
            this.mProviders = new ArrayMap();
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            this.mFlags = defaultOnly ? 65536 : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            boolean z = false;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            if ((65536 & flags) != 0) {
                z = true;
            }
            return super.queryIntent(intent, resolvedType, z, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<Provider> packageProviders, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageProviders == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (65536 & flags) != 0;
            int N = packageProviders.size();
            ArrayList<ProviderIntentInfo[]> listCut = new ArrayList(N);
            for (int i = 0; i < N; i++) {
                ArrayList<ProviderIntentInfo> intentFilters = ((Provider) packageProviders.get(i)).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    ProviderIntentInfo[] array = new ProviderIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        public final void addProvider(Provider p) {
            if (this.mProviders.containsKey(p.getComponentName())) {
                Slog.w(PackageManagerService.TAG, "Provider " + p.getComponentName() + " already defined; ignoring");
                return;
            }
            this.mProviders.put(p.getComponentName(), p);
            int NI = p.intents.size();
            for (int j = 0; j < NI; j++) {
                ProviderIntentInfo intent = (ProviderIntentInfo) p.intents.get(j);
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Provider " + p.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeProvider(Provider p) {
            this.mProviders.remove(p.getComponentName());
            int NI = p.intents.size();
            for (int j = 0; j < NI; j++) {
                removeFilter((ProviderIntentInfo) p.intents.get(j));
            }
        }

        protected boolean allowFilterResult(ProviderIntentInfo filter, List<ResolveInfo> dest) {
            ProviderInfo filterPi = filter.provider.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ProviderInfo destPi = ((ResolveInfo) dest.get(i)).providerInfo;
                if (destPi.name == filterPi.name && destPi.packageName == filterPi.packageName) {
                    return false;
                }
            }
            return true;
        }

        protected ProviderIntentInfo[] newArray(int size) {
            return new ProviderIntentInfo[size];
        }

        protected boolean isFilterStopped(ProviderIntentInfo filter, int userId) {
            boolean z = false;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return true;
            }
            Package p = filter.provider.owner;
            if (p != null) {
                PackageSetting ps = p.mExtras;
                if (ps != null) {
                    if ((ps.pkgFlags & 1) == 0) {
                        z = ps.getStopped(userId);
                    }
                    return z;
                }
            }
            return false;
        }

        protected boolean isPackageForFilter(String packageName, ProviderIntentInfo info) {
            return packageName.equals(info.provider.owner.packageName);
        }

        protected ResolveInfo newResult(ProviderIntentInfo filter, int match, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            ProviderIntentInfo info = filter;
            if (!PackageManagerService.this.mSettings.isEnabledAndMatchLPr(filter.provider.info, this.mFlags, userId)) {
                return null;
            }
            Provider provider = filter.provider;
            PackageSetting ps = provider.owner.mExtras;
            if (ps == null) {
                return null;
            }
            if (PackageManagerService.this.mSafeMode && (PackageManagerService.this.isSystemPathApp(ps) ^ 1) != 0) {
                return null;
            }
            PackageUserState userState = ps.readUserState(userId);
            boolean matchVisibleToInstantApp = (this.mFlags & 16777216) != 0;
            boolean isInstantApp = (this.mFlags & DumpState.DUMP_VOLUMES) != 0;
            if (matchVisibleToInstantApp) {
                if (((!filter.isVisibleToInstantApp() ? userState.instantApp : 1) ^ 1) != 0) {
                    return null;
                }
            }
            if (!isInstantApp && userState.instantApp) {
                return null;
            }
            if (userState.instantApp && ps.isUpdateAvailable()) {
                return null;
            }
            ProviderInfo pi = PackageParser.generateProviderInfo(provider, this.mFlags, userState, userId);
            if (pi == null) {
                return null;
            }
            ResolveInfo res = new ResolveInfo();
            res.providerInfo = pi;
            if ((this.mFlags & 64) != 0) {
                res.filter = filter;
            }
            res.priority = filter.getPriority();
            res.preferredOrder = provider.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = filter.hasDefault;
            res.labelRes = filter.labelRes;
            res.nonLocalizedLabel = filter.nonLocalizedLabel;
            res.icon = filter.icon;
            res.system = res.providerInfo.applicationInfo.isSystemApp();
            return res;
        }

        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, PackageManagerService.mResolvePrioritySorter);
        }

        protected void dumpFilter(PrintWriter out, String prefix, ProviderIntentInfo filter) {
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(filter.provider)));
            out.print(' ');
            filter.provider.printComponentShortName(out);
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        protected Object filterToLabel(ProviderIntentInfo filter) {
            return filter.provider;
        }

        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            Provider provider = (Provider) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(provider)));
            out.print(' ');
            provider.printComponentShortName(out);
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }
    }

    private final class ServiceIntentResolver extends IntentResolver<ServiceIntentInfo, ResolveInfo> {
        private int mFlags;
        private final ArrayMap<ComponentName, Service> mServices;

        private ServiceIntentResolver() {
            this.mServices = new ArrayMap();
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            this.mFlags = defaultOnly ? 65536 : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            boolean z = false;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            this.mFlags = flags;
            if ((65536 & flags) != 0) {
                z = true;
            }
            return super.queryIntent(intent, resolvedType, z, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags, ArrayList<Service> packageServices, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId) || packageServices == null) {
                return null;
            }
            this.mFlags = flags;
            boolean defaultOnly = (65536 & flags) != 0;
            int N = packageServices.size();
            ArrayList<ServiceIntentInfo[]> listCut = new ArrayList(N);
            for (int i = 0; i < N; i++) {
                ArrayList<ServiceIntentInfo> intentFilters = ((Service) packageServices.get(i)).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    ServiceIntentInfo[] array = new ServiceIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        public final void addService(Service s) {
            this.mServices.put(s.getComponentName(), s);
            int NI = s.intents.size();
            for (int j = 0; j < NI; j++) {
                ServiceIntentInfo intent = (ServiceIntentInfo) s.intents.get(j);
                if (!intent.debugCheck()) {
                    Log.w(PackageManagerService.TAG, "==> For Service " + s.info.name);
                }
                addFilter(intent);
            }
        }

        public final void removeService(Service s) {
            this.mServices.remove(s.getComponentName());
            int NI = s.intents.size();
            for (int j = 0; j < NI; j++) {
                removeFilter((ServiceIntentInfo) s.intents.get(j));
            }
        }

        protected boolean allowFilterResult(ServiceIntentInfo filter, List<ResolveInfo> dest) {
            ServiceInfo filterSi = filter.service.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ServiceInfo destAi = ((ResolveInfo) dest.get(i)).serviceInfo;
                if (destAi.name == filterSi.name && destAi.packageName == filterSi.packageName) {
                    return false;
                }
            }
            return true;
        }

        protected ServiceIntentInfo[] newArray(int size) {
            return new ServiceIntentInfo[size];
        }

        protected boolean isFilterStopped(ServiceIntentInfo filter, int userId) {
            boolean z = false;
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return true;
            }
            Package p = filter.service.owner;
            if (p != null) {
                PackageSetting ps = p.mExtras;
                if (ps != null) {
                    if ((ps.pkgFlags & 1) == 0) {
                        z = ps.getStopped(userId);
                    }
                    return z;
                }
            }
            return false;
        }

        protected boolean isPackageForFilter(String packageName, ServiceIntentInfo info) {
            return packageName.equals(info.service.owner.packageName);
        }

        protected ResolveInfo newResult(ServiceIntentInfo filter, int match, int userId) {
            if (!PackageManagerService.sUserManager.exists(userId)) {
                return null;
            }
            ServiceIntentInfo info = filter;
            if (!PackageManagerService.this.mSettings.isEnabledAndMatchLPr(filter.service.info, this.mFlags, userId)) {
                return null;
            }
            Service service = filter.service;
            PackageSetting ps = service.owner.mExtras;
            if (ps == null) {
                return null;
            }
            if (PackageManagerService.this.mSafeMode && (PackageManagerService.this.isSystemPathApp(ps) ^ 1) != 0) {
                return null;
            }
            PackageUserState userState = ps.readUserState(userId);
            ServiceInfo si = PackageParser.generateServiceInfo(service, this.mFlags, userState, userId);
            if (si == null) {
                return null;
            }
            boolean matchVisibleToInstantApp = (this.mFlags & 16777216) != 0;
            boolean isInstantApp = (this.mFlags & DumpState.DUMP_VOLUMES) != 0;
            if (matchVisibleToInstantApp) {
                if (((!filter.isVisibleToInstantApp() ? userState.instantApp : 1) ^ 1) != 0) {
                    return null;
                }
            }
            if (!isInstantApp && userState.instantApp) {
                return null;
            }
            if (userState.instantApp && ps.isUpdateAvailable()) {
                return null;
            }
            ResolveInfo res = new ResolveInfo();
            res.serviceInfo = si;
            if ((this.mFlags & 64) != 0) {
                res.filter = filter;
            }
            res.priority = filter.getPriority();
            res.preferredOrder = service.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = filter.hasDefault;
            res.labelRes = filter.labelRes;
            res.nonLocalizedLabel = filter.nonLocalizedLabel;
            res.icon = filter.icon;
            res.system = res.serviceInfo.applicationInfo.isSystemApp();
            return res;
        }

        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, PackageManagerService.mResolvePrioritySorter);
        }

        protected void dumpFilter(PrintWriter out, String prefix, ServiceIntentInfo filter) {
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(filter.service)));
            out.print(' ');
            filter.service.printComponentShortName(out);
            out.print(" filter ");
            out.println(Integer.toHexString(System.identityHashCode(filter)));
        }

        protected Object filterToLabel(ServiceIntentInfo filter) {
            return filter.service;
        }

        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
            Service service = (Service) label;
            out.print(prefix);
            out.print(Integer.toHexString(System.identityHashCode(service)));
            out.print(' ');
            service.printComponentShortName(out);
            if (count > 1) {
                out.print(" (");
                out.print(count);
                out.print(" filters)");
            }
            out.println();
        }
    }

    public static final class SharedLibraryEntry {
        public final String apk;
        public final SharedLibraryInfo info;
        public final String path;

        SharedLibraryEntry(String _path, String _apk, String name, int version, int type, String declaringPackageName, int declaringPackageVersionCode) {
            this.path = _path;
            this.apk = _apk;
            this.info = new SharedLibraryInfo(name, version, type, new VersionedPackage(declaringPackageName, declaringPackageVersionCode), null);
        }
    }

    static class VerificationInfo {
        public static final int NO_UID = -1;
        final int installerUid;
        final int originatingUid;
        final Uri originatingUri;
        final Uri referrer;

        VerificationInfo(Uri originatingUri, Uri referrer, int originatingUid, int installerUid) {
            this.originatingUri = originatingUri;
            this.referrer = referrer;
            this.originatingUid = originatingUid;
            this.installerUid = installerUid;
        }
    }

    private java.io.File decompressPackage(android.content.pm.PackageParser.Package r17) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:43:? in {6, 8, 15, 27, 28, 30, 35, 37, 39, 40, 41, 44, 45} preds:[]
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:129)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.rerun(BlockProcessor.java:44)
	at jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract.visit(BlockFinallyExtract.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r16 = this;
        r0 = r17;
        r12 = r0.codePath;
        r0 = r16;
        r1 = r0.getCompressedFiles(r12);
        if (r1 == 0) goto L_0x000f;
    L_0x000c:
        r12 = r1.length;
        if (r12 != 0) goto L_0x0033;
    L_0x000f:
        r12 = DEBUG_COMPRESSION;
        if (r12 == 0) goto L_0x0031;
    L_0x0013:
        r12 = "PackageManager";
        r13 = new java.lang.StringBuilder;
        r13.<init>();
        r14 = "No files to decompress: ";
        r13 = r13.append(r14);
        r0 = r17;
        r14 = r0.baseCodePath;
        r13 = r13.append(r14);
        r13 = r13.toString();
        android.util.Slog.i(r12, r13);
    L_0x0031:
        r12 = 0;
        return r12;
    L_0x0033:
        r12 = 0;
        r12 = android.os.Environment.getDataAppDirectory(r12);
        r0 = r17;
        r13 = r0.packageName;
        r0 = r16;
        r2 = r0.getNextCodePath(r12, r13);
        r9 = 1;
        r12 = r2.getAbsolutePath();	 Catch:{ ErrnoException -> 0x00cf }
        r13 = 493; // 0x1ed float:6.91E-43 double:2.436E-321;	 Catch:{ ErrnoException -> 0x00cf }
        android.system.Os.mkdir(r12, r13);	 Catch:{ ErrnoException -> 0x00cf }
        r12 = r2.getAbsolutePath();	 Catch:{ ErrnoException -> 0x00cf }
        r13 = 493; // 0x1ed float:6.91E-43 double:2.436E-321;	 Catch:{ ErrnoException -> 0x00cf }
        android.system.Os.chmod(r12, r13);	 Catch:{ ErrnoException -> 0x00cf }
        r12 = 0;	 Catch:{ ErrnoException -> 0x00cf }
        r13 = r1.length;	 Catch:{ ErrnoException -> 0x00cf }
    L_0x0057:
        if (r12 >= r13) goto L_0x00a5;	 Catch:{ ErrnoException -> 0x00cf }
    L_0x0059:
        r10 = r1[r12];	 Catch:{ ErrnoException -> 0x00cf }
        r11 = r10.getName();	 Catch:{ ErrnoException -> 0x00cf }
        r14 = r11.length();	 Catch:{ ErrnoException -> 0x00cf }
        r15 = ".gz";	 Catch:{ ErrnoException -> 0x00cf }
        r15 = r15.length();	 Catch:{ ErrnoException -> 0x00cf }
        r14 = r14 - r15;	 Catch:{ ErrnoException -> 0x00cf }
        r15 = 0;	 Catch:{ ErrnoException -> 0x00cf }
        r4 = r11.substring(r15, r14);	 Catch:{ ErrnoException -> 0x00cf }
        r3 = new java.io.File;	 Catch:{ ErrnoException -> 0x00cf }
        r3.<init>(r2, r4);	 Catch:{ ErrnoException -> 0x00cf }
        r0 = r16;	 Catch:{ ErrnoException -> 0x00cf }
        r9 = r0.decompressFile(r10, r3);	 Catch:{ ErrnoException -> 0x00cf }
        r14 = 1;	 Catch:{ ErrnoException -> 0x00cf }
        if (r9 == r14) goto L_0x00cc;	 Catch:{ ErrnoException -> 0x00cf }
    L_0x007e:
        r12 = new java.lang.StringBuilder;	 Catch:{ ErrnoException -> 0x00cf }
        r12.<init>();	 Catch:{ ErrnoException -> 0x00cf }
        r13 = "Failed to decompress; pkg: ";	 Catch:{ ErrnoException -> 0x00cf }
        r12 = r12.append(r13);	 Catch:{ ErrnoException -> 0x00cf }
        r0 = r17;	 Catch:{ ErrnoException -> 0x00cf }
        r13 = r0.packageName;	 Catch:{ ErrnoException -> 0x00cf }
        r12 = r12.append(r13);	 Catch:{ ErrnoException -> 0x00cf }
        r13 = ", file: ";	 Catch:{ ErrnoException -> 0x00cf }
        r12 = r12.append(r13);	 Catch:{ ErrnoException -> 0x00cf }
        r12 = r12.append(r4);	 Catch:{ ErrnoException -> 0x00cf }
        r12 = r12.toString();	 Catch:{ ErrnoException -> 0x00cf }
        r13 = 6;	 Catch:{ ErrnoException -> 0x00cf }
        logCriticalInfo(r13, r12);	 Catch:{ ErrnoException -> 0x00cf }
    L_0x00a5:
        r12 = 1;
        if (r9 != r12) goto L_0x00bd;
    L_0x00a8:
        r8 = new java.io.File;
        r12 = "lib";
        r8.<init>(r2, r12);
        r7 = 0;
        r7 = com.android.internal.content.NativeLibraryHelper.Handle.create(r2);	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r12 = 0;	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r9 = com.android.internal.content.NativeLibraryHelper.copyNativeBinariesWithOverride(r7, r8, r12);	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        libcore.io.IoUtils.closeQuietly(r7);
    L_0x00bd:
        r12 = 1;
        if (r9 == r12) goto L_0x0129;
    L_0x00c0:
        if (r2 == 0) goto L_0x00ca;
    L_0x00c2:
        r12 = r2.exists();
        r12 = r12 ^ 1;
        if (r12 == 0) goto L_0x0122;
    L_0x00ca:
        r12 = 0;
        return r12;
    L_0x00cc:
        r12 = r12 + 1;
        goto L_0x0057;
    L_0x00cf:
        r5 = move-exception;
        r12 = new java.lang.StringBuilder;
        r12.<init>();
        r13 = "Failed to decompress; pkg: ";
        r12 = r12.append(r13);
        r0 = r17;
        r13 = r0.packageName;
        r12 = r12.append(r13);
        r13 = ", err: ";
        r12 = r12.append(r13);
        r13 = r5.errno;
        r12 = r12.append(r13);
        r12 = r12.toString();
        r13 = 6;
        logCriticalInfo(r13, r12);
        goto L_0x00a5;
    L_0x00fa:
        r6 = move-exception;
        r12 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r12.<init>();	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r13 = "Failed to extract native libraries; pkg: ";	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r12 = r12.append(r13);	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r0 = r17;	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r13 = r0.packageName;	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r12 = r12.append(r13);	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r12 = r12.toString();	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r13 = 6;	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        logCriticalInfo(r13, r12);	 Catch:{ IOException -> 0x00fa, all -> 0x011d }
        r9 = -110; // 0xffffffffffffff92 float:NaN double:NaN;
        libcore.io.IoUtils.closeQuietly(r7);
        goto L_0x00bd;
    L_0x011d:
        r12 = move-exception;
        libcore.io.IoUtils.closeQuietly(r7);
        throw r12;
    L_0x0122:
        r0 = r16;
        r0.removeCodePathLI(r2);
        r12 = 0;
        return r12;
    L_0x0129:
        return r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.PackageManagerService.decompressPackage(android.content.pm.PackageParser$Package):java.io.File");
    }

    private boolean deleteSystemPackageLIF(android.content.pm.PackageParser.Package r22, com.android.server.pm.PackageSetting r23, int[] r24, int r25, com.android.server.pm.PackageManagerService.PackageRemovedInfo r26, boolean r27) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x0249 in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r21 = this;
        r0 = r23;
        r2 = r0.parentPackageName;
        if (r2 == 0) goto L_0x0026;
    L_0x0006:
        r2 = "PackageManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Attempt to delete child system package ";
        r3 = r3.append(r4);
        r0 = r22;
        r4 = r0.packageName;
        r3 = r3.append(r4);
        r3 = r3.toString();
        android.util.Slog.w(r2, r3);
        r2 = 0;
        return r2;
    L_0x0026:
        if (r24 == 0) goto L_0x0069;
    L_0x0028:
        if (r26 == 0) goto L_0x0069;
    L_0x002a:
        r0 = r26;
        r2 = r0.origUsers;
        if (r2 == 0) goto L_0x0069;
    L_0x0030:
        r10 = 1;
    L_0x0031:
        r0 = r21;
        r3 = r0.mPackages;
        monitor-enter(r3);
        r0 = r21;
        r2 = r0.mSettings;
        r0 = r23;
        r4 = r0.name;
        r14 = r2.getDisabledSystemPkgLPr(r4);
        monitor-exit(r3);
        if (r14 == 0) goto L_0x0049;
    L_0x0045:
        r2 = r14.pkg;
        if (r2 != 0) goto L_0x006e;
    L_0x0049:
        r2 = "PackageManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Attempt to delete unknown system package ";
        r3 = r3.append(r4);
        r0 = r22;
        r4 = r0.packageName;
        r3 = r3.append(r4);
        r3 = r3.toString();
        android.util.Slog.w(r2, r3);
        r2 = 0;
        return r2;
    L_0x0069:
        r10 = 0;
        goto L_0x0031;
    L_0x006b:
        r2 = move-exception;
        monitor-exit(r3);
        throw r2;
    L_0x006e:
        r0 = r21;
        r3 = r0.mPackages;
        monitor-enter(r3);
        r0 = r21;
        r2 = r0.isDelapp(r14);
        if (r2 != 0) goto L_0x0099;
    L_0x007b:
        r0 = r21;
        r2 = r0.isDelappInData(r14);
        if (r2 != 0) goto L_0x0099;
    L_0x0083:
        r0 = r21;
        r2 = r0.isDelappInCust(r14);
        if (r2 != 0) goto L_0x0099;
    L_0x008b:
        r2 = r14.codePath;
        r2 = r2.toString();
        r0 = r21;
        r2 = r0.isPreRemovableApp(r2);
        if (r2 == 0) goto L_0x00a4;
    L_0x0099:
        r0 = r21;
        r2 = r0.mSettings;
        r0 = r23;
        r4 = r0.name;
        r2.removeDisabledSystemPackageLPw(r4);
    L_0x00a4:
        monitor-exit(r3);
        if (r26 == 0) goto L_0x00ac;
    L_0x00a7:
        r2 = 1;
        r0 = r26;
        r0.isRemovedPackageSystemUpdate = r2;
    L_0x00ac:
        if (r26 == 0) goto L_0x00f7;
    L_0x00ae:
        r0 = r26;
        r2 = r0.removedChildPackages;
        if (r2 == 0) goto L_0x00f7;
    L_0x00b4:
        r0 = r23;
        r2 = r0.childPackageNames;
        if (r2 == 0) goto L_0x00f5;
    L_0x00ba:
        r0 = r23;
        r2 = r0.childPackageNames;
        r11 = r2.size();
    L_0x00c2:
        r16 = 0;
    L_0x00c4:
        r0 = r16;
        if (r0 >= r11) goto L_0x00f7;
    L_0x00c8:
        r0 = r23;
        r2 = r0.childPackageNames;
        r0 = r16;
        r13 = r2.get(r0);
        r13 = (java.lang.String) r13;
        r2 = r14.childPackageNames;
        if (r2 == 0) goto L_0x00ef;
    L_0x00d8:
        r2 = r14.childPackageNames;
        r2 = r2.contains(r13);
        if (r2 == 0) goto L_0x00ef;
    L_0x00e0:
        r0 = r26;
        r2 = r0.removedChildPackages;
        r12 = r2.get(r13);
        r12 = (com.android.server.pm.PackageManagerService.PackageRemovedInfo) r12;
        if (r12 == 0) goto L_0x00ef;
    L_0x00ec:
        r2 = 1;
        r12.isRemovedPackageSystemUpdate = r2;
    L_0x00ef:
        r16 = r16 + 1;
        goto L_0x00c4;
    L_0x00f2:
        r2 = move-exception;
        monitor-exit(r3);
        throw r2;
    L_0x00f5:
        r11 = 0;
        goto L_0x00c2;
    L_0x00f7:
        r2 = r14.versionCode;
        r0 = r23;
        r3 = r0.versionCode;
        if (r2 >= r3) goto L_0x0118;
    L_0x00ff:
        r25 = r25 & -2;
    L_0x0101:
        r9 = r14.pkg;
        r4 = 1;
        r2 = r21;
        r3 = r23;
        r5 = r25;
        r6 = r24;
        r7 = r26;
        r8 = r27;
        r19 = r2.deleteInstalledPackageLIF(r3, r4, r5, r6, r7, r8, r9);
        if (r19 != 0) goto L_0x014c;
    L_0x0116:
        r2 = 0;
        return r2;
    L_0x0118:
        r0 = r21;
        r2 = r0.isDelapp(r14);
        if (r2 != 0) goto L_0x013e;
    L_0x0120:
        r0 = r21;
        r2 = r0.isDelappInData(r14);
        if (r2 != 0) goto L_0x013e;
    L_0x0128:
        r0 = r21;
        r2 = r0.isDelappInCust(r14);
        if (r2 != 0) goto L_0x013e;
    L_0x0130:
        r2 = r14.codePath;
        r2 = r2.toString();
        r0 = r21;
        r2 = r0.isPreRemovableApp(r2);
        if (r2 == 0) goto L_0x0149;
    L_0x013e:
        r2 = r14.versionCode;
        r0 = r23;
        r3 = r0.versionCode;
        if (r2 != r3) goto L_0x0149;
    L_0x0146:
        r25 = r25 & -2;
        goto L_0x0101;
    L_0x0149:
        r25 = r25 | 1;
        goto L_0x0101;
    L_0x014c:
        r0 = r21;
        r3 = r0.mPackages;
        monitor-enter(r3);
        r2 = r14.pkg;
        r0 = r21;
        r0.enableSystemPackageLPw(r2);
        r0 = r21;
        r1 = r23;
        r0.removeNativeBinariesLI(r1);
        monitor-exit(r3);
        r18 = 0;
        r20 = 0;
        r17 = new com.android.server.pm.PackageManagerService$PackageRemovedInfo;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r17;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r1 = r21;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0.<init>(r1);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r21;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r0.isDelapp(r14);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        if (r2 != 0) goto L_0x0193;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
    L_0x0175:
        r0 = r21;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r0.isDelappInData(r14);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        if (r2 != 0) goto L_0x0193;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
    L_0x017d:
        r0 = r21;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r0.isDelappInCust(r14);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        if (r2 != 0) goto L_0x0193;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
    L_0x0185:
        r2 = r14.codePath;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r2.toString();	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r21;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r0.isPreRemovableApp(r2);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        if (r2 == 0) goto L_0x01d0;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
    L_0x0193:
        r0 = r22;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r0.packageName;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3 = r14.codePathString;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r21;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0.recordUninstalledDelapp(r2, r3);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r22;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r0.packageName;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r17;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0.removedPackage = r2;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        if (r26 == 0) goto L_0x01ad;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
    L_0x01a8:
        r2 = 0;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r26;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0.isRemovedPackageSystemUpdate = r2;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
    L_0x01ad:
        r2 = 1;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r17;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0.sendPackageRemovedBroadcasts(r2);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r20 = 1;
    L_0x01b5:
        if (r18 != 0) goto L_0x01f8;
    L_0x01b7:
        if (r20 == 0) goto L_0x01e6;
    L_0x01b9:
        r20 = 0;
        r2 = 1;
        r3 = r14.pkg;
        r3 = r3.isStub;
        if (r3 == 0) goto L_0x01cc;
    L_0x01c2:
        r0 = r21;
        r3 = r0.mSettings;
        r4 = r14.name;
        r5 = 1;
        r3.disableSystemPackageLPw(r4, r5);
    L_0x01cc:
        return r2;
    L_0x01cd:
        r2 = move-exception;
        monitor-exit(r3);
        throw r2;
    L_0x01d0:
        r3 = r14.codePath;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r26;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r6 = r0.origUsers;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r7 = r23.getPermissionsState();	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r4 = 0;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = r21;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r5 = r24;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r8 = r27;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r18 = r2.installPackageFromSystemLIF(r3, r4, r5, r6, r7, r8);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        goto L_0x01b5;
    L_0x01e6:
        r2 = 0;
        r3 = r14.pkg;
        r3 = r3.isStub;
        if (r3 == 0) goto L_0x01f7;
    L_0x01ed:
        r0 = r21;
        r3 = r0.mSettings;
        r4 = r14.name;
        r5 = 1;
        r3.disableSystemPackageLPw(r4, r5);
    L_0x01f7:
        return r2;
    L_0x01f8:
        r2 = r14.pkg;
        r2 = r2.isStub;
        if (r2 == 0) goto L_0x0208;
    L_0x01fe:
        r0 = r21;
        r2 = r0.mSettings;
        r3 = r14.name;
        r4 = 1;
        r2.disableSystemPackageLPw(r3, r4);
    L_0x0208:
        r2 = 1;
        return r2;
    L_0x020a:
        r15 = move-exception;
        r2 = "PackageManager";	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3 = new java.lang.StringBuilder;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3.<init>();	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r4 = "Failed to restore system package:";	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3 = r3.append(r4);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r0 = r22;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r4 = r0.packageName;	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3 = r3.append(r4);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r4 = ": ";	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3 = r3.append(r4);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r4 = r15.getMessage();	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3 = r3.append(r4);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r3 = r3.toString();	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        android.util.Slog.w(r2, r3);	 Catch:{ PackageManagerException -> 0x020a, all -> 0x024a }
        r2 = 0;
        r3 = r14.pkg;
        r3 = r3.isStub;
        if (r3 == 0) goto L_0x0249;
    L_0x023f:
        r0 = r21;
        r3 = r0.mSettings;
        r4 = r14.name;
        r5 = 1;
        r3.disableSystemPackageLPw(r4, r5);
    L_0x0249:
        return r2;
    L_0x024a:
        r2 = move-exception;
        r3 = r14.pkg;
        r3 = r3.isStub;
        if (r3 == 0) goto L_0x025b;
    L_0x0251:
        r0 = r21;
        r3 = r0.mSettings;
        r4 = r14.name;
        r5 = 1;
        r3.disableSystemPackageLPw(r4, r5);
    L_0x025b:
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.PackageManagerService.deleteSystemPackageLIF(android.content.pm.PackageParser$Package, com.android.server.pm.PackageSetting, int[], int, com.android.server.pm.PackageManagerService$PackageRemovedInfo, boolean):boolean");
    }

    static {
        boolean z;
        int i;
        if (Log.HWINFO) {
            z = true;
        } else if (Log.HWModuleLog) {
            z = Log.isLoggable(TAG, 4);
        } else {
            z = false;
        }
        HWFLOW = z;
        if (IS_FPGA) {
            i = 1200000;
        } else {
            i = ProcessList.PSS_ALL_INTERVAL;
        }
        WATCHDOG_TIMEOUT = (long) i;
        sBrowserIntent.setAction("android.intent.action.VIEW");
        sBrowserIntent.addCategory("android.intent.category.BROWSABLE");
        sBrowserIntent.setData(Uri.parse("http:"));
        PROTECTED_ACTIONS.add("android.intent.action.SEND");
        PROTECTED_ACTIONS.add("android.intent.action.SENDTO");
        PROTECTED_ACTIONS.add("android.intent.action.SEND_MULTIPLE");
        PROTECTED_ACTIONS.add("android.intent.action.VIEW");
    }

    private static boolean hasValidDomains(ActivityIntentInfo filter) {
        if (!filter.hasCategory("android.intent.category.BROWSABLE")) {
            return false;
        }
        if (filter.hasDataScheme("http")) {
            return true;
        }
        return filter.hasDataScheme("https");
    }

    private void handlePackagePostInstall(PackageInstalledInfo res, boolean grantPermissions, boolean killApp, boolean virtualPreload, String[] grantedPermissions, boolean launchedForRestore, String installerPackage, IPackageInstallObserver2 installObserver) {
        if (res.returnCode == 1) {
            String str;
            if (res.removedInfo != null) {
                res.removedInfo.sendPackageRemovedBroadcasts(killApp);
            }
            if (grantPermissions) {
                grantRequestedRuntimePermissions(res.pkg, res.newUsers, grantedPermissions);
            }
            boolean update = res.removedInfo != null ? res.removedInfo.removedPackage != null : false;
            if (res.installerPackageName != null) {
                str = res.installerPackageName;
            } else if (res.removedInfo != null) {
                str = res.removedInfo.installerPackageName;
            } else {
                str = null;
            }
            if (res.pkg.parentPackage != null) {
                synchronized (this.mPackages) {
                    grantRuntimePermissionsGrantedToDisabledPrivSysPackageParentLPw(res.pkg);
                }
            }
            synchronized (this.mPackages) {
                this.mInstantAppRegistry.onPackageInstalledLPw(res.pkg, res.newUsers);
            }
            String packageName = res.pkg.applicationInfo.packageName;
            int[] firstUsers = EMPTY_INT_ARRAY;
            int[] updateUsers = EMPTY_INT_ARRAY;
            boolean allNewUsers = res.origUsers == null || res.origUsers.length == 0;
            PackageSetting ps = res.pkg.mExtras;
            for (int newUser : res.newUsers) {
                if (!ps.getInstantApp(newUser)) {
                    if (allNewUsers) {
                        firstUsers = ArrayUtils.appendInt(firstUsers, newUser);
                    } else {
                        boolean isNew = true;
                        for (int i : res.origUsers) {
                            if (i == newUser) {
                                isNew = false;
                                break;
                            }
                        }
                        if (isNew) {
                            firstUsers = ArrayUtils.appendInt(firstUsers, newUser);
                        } else {
                            updateUsers = ArrayUtils.appendInt(updateUsers, newUser);
                        }
                    }
                }
            }
            setNeedClearDeviceForCTS(true, packageName);
            try {
                updatePackageBlackListInfo(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "update BlackList info failed");
            }
            if (res.pkg.staticSharedLibName == null) {
                this.mProcessLoggingHandler.invalidateProcessLoggingBaseApkHash(res.pkg.baseCodePath);
                sendIncompatibleNotificationIfNeeded(packageName);
                sendPackageAddedForNewUsers(packageName, !res.pkg.applicationInfo.isSystemApp() ? virtualPreload : true, virtualPreload, UserHandle.getAppId(res.uid), firstUsers);
                Bundle extras = new Bundle(1);
                extras.putInt("android.intent.extra.UID", res.uid);
                if (update) {
                    extras.putBoolean("android.intent.extra.REPLACING", true);
                }
                sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, null, null, updateUsers);
                if (str != null) {
                    sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, str, null, updateUsers);
                }
                this.mHwPMSEx.updateNochScreenWhite(packageName, "add", res.pkg.applicationInfo.versionCode);
                if (update) {
                    sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", packageName, extras, 0, null, null, updateUsers);
                    if (str != null) {
                        sendPackageBroadcast("android.intent.action.PACKAGE_REPLACED", packageName, extras, 0, str, null, updateUsers);
                    }
                    sendPackageBroadcast("android.intent.action.MY_PACKAGE_REPLACED", null, null, 0, packageName, null, updateUsers);
                } else if (launchedForRestore && (isSystemApp(res.pkg) ^ 1) != 0) {
                    sendFirstLaunchBroadcast(packageName, installerPackage, firstUsers);
                }
                if (res.pkg.isForwardLocked() || isExternal(res.pkg)) {
                    int[] uidArray = new int[]{res.pkg.applicationInfo.uid};
                    ArrayList arrayList = new ArrayList(1);
                    arrayList.add(packageName);
                    sendResourcesChangedBroadcast(true, true, arrayList, uidArray, null);
                }
            }
            if (firstUsers != null && firstUsers.length > 0) {
                synchronized (this.mPackages) {
                    for (int userId : firstUsers) {
                        if (packageIsBrowser(packageName, userId)) {
                            this.mSettings.setDefaultBrowserPackageNameLPw(null, userId);
                        }
                        this.mSettings.applyPendingPermissionGrantsLPw(packageName, userId);
                    }
                }
            }
            EventLog.writeEvent(EventLogTags.UNKNOWN_SOURCES_ENABLED, getUnknownSourcesSettings());
            if (res.removedInfo == null || res.removedInfo.args == null) {
                VMRuntime.getRuntime().requestConcurrentGC();
            } else {
                Runtime.getRuntime().gc();
                synchronized (this.mInstallLock) {
                    res.removedInfo.args.doPostDeleteLI(true);
                }
            }
            for (int userId2 : firstUsers) {
                PackageInfo info = getPackageInfo(packageName, 0, userId2);
                if (info != null) {
                    this.mDexManager.notifyPackageInstalled(info, userId2);
                }
            }
        }
        if (installObserver != null) {
            try {
                installObserver.onPackageInstalled(res.name, res.returnCode, res.returnMsg, extrasForInstallResult(res));
            } catch (RemoteException e2) {
                Slog.i(TAG, "Observer no longer exists.");
            }
        }
    }

    private void grantRuntimePermissionsGrantedToDisabledPrivSysPackageParentLPw(Package pkg) {
        if (pkg.parentPackage != null && pkg.requestedPermissions != null) {
            PackageSetting disabledSysParentPs = this.mSettings.getDisabledSystemPkgLPr(pkg.parentPackage.packageName);
            if (disabledSysParentPs != null && disabledSysParentPs.pkg != null && (disabledSysParentPs.isPrivileged() ^ 1) == 0 && (disabledSysParentPs.childPackageNames == null || (disabledSysParentPs.childPackageNames.isEmpty() ^ 1) == 0)) {
                int[] allUserIds = sUserManager.getUserIds();
                int permCount = pkg.requestedPermissions.size();
                for (int i = 0; i < permCount; i++) {
                    String permission = (String) pkg.requestedPermissions.get(i);
                    BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(permission);
                    if (bp != null) {
                        if (((!bp.isRuntime() ? bp.isDevelopment() : 1) ^ 1) == 0) {
                            for (int userId : allUserIds) {
                                if (disabledSysParentPs.getPermissionsState().hasRuntimePermission(permission, userId)) {
                                    grantRuntimePermission(pkg.packageName, permission, userId);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void setNeedClearDeviceForCTS(boolean needvalue, String packageName) {
        if (packageName == null) {
            this.mNeedClearDeviceForCTS = false;
        } else if (packageName.equals(PACKAGE_NAME_BASICADMINRECEIVER_CTS_DEIVCEOWNER) || packageName.equals(PACKAGE_NAME_BASICADMINRECEIVER_CTS_DEVICEANDPROFILEOWNER) || packageName.equals(PACKAGE_NAME_BASICADMINRECEIVER_CTS_PACKAGEINSTALLER)) {
            this.mNeedClearDeviceForCTS = needvalue;
            Log.d(TAG, "setmNeedClearDeviceForCTS:" + this.mNeedClearDeviceForCTS);
        } else {
            this.mNeedClearDeviceForCTS = false;
        }
    }

    public boolean getNeedClearDeviceForCTS() {
        return this.mNeedClearDeviceForCTS;
    }

    private void grantRequestedRuntimePermissions(Package pkg, int[] userIds, String[] grantedPermissions) {
        for (int userId : userIds) {
            grantRequestedRuntimePermissionsForUser(pkg, userId, grantedPermissions);
        }
    }

    private void grantRequestedRuntimePermissionsForUser(Package pkg, int userId, String[] grantedPermissions) {
        PackageSetting ps = pkg.mExtras;
        if (ps != null) {
            PermissionsState permissionsState = ps.getPermissionsState();
            boolean supportsRuntimePermissions = pkg.applicationInfo.targetSdkVersion >= 23;
            boolean instantApp = isInstantApp(pkg.packageName, userId);
            for (String permission : pkg.requestedPermissions) {
                BasePermission bp;
                synchronized (this.mPackages) {
                    bp = (BasePermission) this.mSettings.mPermissions.get(permission);
                }
                if (bp != null && ((bp.isRuntime() || bp.isDevelopment()) && ((!instantApp || bp.isInstant()) && ((supportsRuntimePermissions || (bp.isRuntimeOnly() ^ 1) != 0) && (grantedPermissions == null || ArrayUtils.contains(grantedPermissions, permission)))))) {
                    int flags = permissionsState.getPermissionFlags(permission, userId);
                    if (supportsRuntimePermissions) {
                        if ((flags & 20) == 0) {
                            grantRuntimePermission(pkg.packageName, permission, userId);
                        }
                    } else if (this.mPermissionReviewRequired && (flags & 64) != 0) {
                        updatePermissionFlags(permission, pkg.packageName, 64, 0, userId);
                    }
                }
            }
        }
    }

    Bundle extrasForInstallResult(PackageInstalledInfo res) {
        boolean z = false;
        Bundle extras;
        switch (res.returnCode) {
            case -112:
                extras = new Bundle();
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PERMISSION", res.origPermission);
                extras.putString("android.content.pm.extra.FAILURE_EXISTING_PACKAGE", res.origPackage);
                return extras;
            case 1:
                extras = new Bundle();
                String str = "android.intent.extra.REPLACING";
                if (!(res.removedInfo == null || res.removedInfo.removedPackage == null)) {
                    z = true;
                }
                extras.putBoolean(str, z);
                return extras;
            default:
                return null;
        }
    }

    void scheduleWriteSettingsLocked() {
        if (!this.mHandler.hasMessages(13)) {
            this.mHandler.sendEmptyMessageDelayed(13, 10000);
        }
    }

    void scheduleWritePackageListLocked(int userId) {
        if (!this.mHandler.hasMessages(19)) {
            Message msg = this.mHandler.obtainMessage(19);
            msg.arg1 = userId;
            this.mHandler.sendMessageDelayed(msg, 10000);
        }
    }

    void scheduleWritePackageRestrictionsLocked(UserHandle user) {
        scheduleWritePackageRestrictionsLocked(user == null ? -1 : user.getIdentifier());
    }

    void scheduleWritePackageRestrictionsLocked(int userId) {
        int i = 0;
        int[] userIds = userId == -1 ? sUserManager.getUserIds() : new int[]{userId};
        int length = userIds.length;
        while (i < length) {
            int nextUserId = userIds[i];
            if (sUserManager.exists(nextUserId)) {
                this.mDirtyUsers.add(Integer.valueOf(nextUserId));
                if (!this.mHandler.hasMessages(14)) {
                    this.mHandler.sendEmptyMessageDelayed(14, 10000);
                }
                i++;
            } else {
                return;
            }
        }
    }

    public static PackageManagerService main(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        long startTime = SystemClock.uptimeMillis();
        PackageManagerServiceCompilerMapping.checkProperties();
        PackageManagerService m = HwServiceFactory.getHuaweiPackageManagerService(context, installer, factoryTest, onlyCore);
        m.enableSystemUserPackages();
        ServiceManager.addService("package", m);
        m.getClass();
        ServiceManager.addService("package_native", new PackageManagerNative());
        Slog.i(TAG, "PackageManagerService booting timestamp : " + (SystemClock.uptimeMillis() - startTime) + " ms");
        return m;
    }

    private void enableSystemUserPackages() {
        if (UserManager.isSplitSystemUser()) {
            AppsQueryHelper queryHelper = new AppsQueryHelper(this);
            Set<String> enableApps = new ArraySet();
            enableApps.addAll(queryHelper.queryApps((AppsQueryHelper.GET_NON_LAUNCHABLE_APPS | AppsQueryHelper.GET_APPS_WITH_INTERACT_ACROSS_USERS_PERM) | AppsQueryHelper.GET_IMES, true, UserHandle.SYSTEM));
            enableApps.addAll(SystemConfig.getInstance().getSystemUserWhitelistedApps());
            enableApps.addAll(queryHelper.queryApps(AppsQueryHelper.GET_REQUIRED_FOR_SYSTEM_USER, false, UserHandle.SYSTEM));
            enableApps.removeAll(SystemConfig.getInstance().getSystemUserBlacklistedApps());
            Log.i(TAG, "Applications installed for system user: " + enableApps);
            List<String> allAps = queryHelper.queryApps(0, false, UserHandle.SYSTEM);
            int allAppsSize = allAps.size();
            synchronized (this.mPackages) {
                for (int i = 0; i < allAppsSize; i++) {
                    String pName = (String) allAps.get(i);
                    PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(pName);
                    if (pkgSetting != null) {
                        boolean install = enableApps.contains(pName);
                        if (pkgSetting.getInstalled(0) != install) {
                            Log.i(TAG, (install ? "Installing " : "Uninstalling ") + pName + " for system user");
                            pkgSetting.setInstalled(install, 0);
                        } else {
                            continue;
                        }
                    }
                }
                scheduleWritePackageRestrictionsLocked(0);
            }
        }
    }

    private static void getDefaultDisplayMetrics(Context context, DisplayMetrics metrics) {
        ((DisplayManager) context.getSystemService("display")).getDisplay(0).getMetrics(metrics);
    }

    private static void requestCopyPreoptedFiles() {
        String CP_PREOPT_PROPERTY = "sys.cppreopt";
        if (SystemProperties.getInt("ro.cp_system_other_odex", 0) == 1) {
            SystemProperties.set("sys.cppreopt", "requested");
            long timeStart = SystemClock.uptimeMillis();
            long timeEnd = timeStart + 100000;
            long timeNow = timeStart;
            while (!SystemProperties.get("sys.cppreopt").equals("finished")) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                timeNow = SystemClock.uptimeMillis();
                if (timeNow > timeEnd) {
                    SystemProperties.set("sys.cppreopt", "timed-out");
                    Slog.wtf(TAG, "cppreopt did not finish!");
                    break;
                }
            }
            Slog.i(TAG, "cppreopts took " + (timeNow - timeStart) + " ms");
        }
    }

    public PackageManagerService(Context context, Installer installer, boolean factoryTest, boolean onlyCore) {
        String str;
        StringBuilder append;
        long startTime;
        File file;
        long j;
        PackageManagerService packageManagerService = this;
        this.mServices = new ServiceIntentResolver();
        packageManagerService = this;
        this.mProviders = new ProviderIntentResolver();
        this.mProvidersByAuthority = new ArrayMap();
        this.mInstrumentation = new ArrayMap();
        this.mPermissionGroups = new ArrayMap();
        this.mTransferedPackages = new ArraySet();
        this.mProtectedBroadcasts = new ArraySet();
        this.mPendingVerification = new SparseArray();
        this.mAppOpPermissionPackages = new ArrayMap();
        this.mNextMoveId = new AtomicInteger();
        this.mUserNeedsBadging = new SparseBooleanArray();
        this.mPendingVerificationToken = 0;
        this.mIsPackageScanMultiThread = SystemProperties.getBoolean("ro.config.hw_packagescan_multi", true);
        this.mResolveActivity = new ActivityInfo();
        this.mResolveInfo = new ResolveInfo();
        this.mResolverReplaced = false;
        this.mExecutorService = Executors.newSingleThreadExecutor();
        this.clearDirectoryThread = Executors.newSingleThreadExecutor();
        this.mIntentFilterVerificationToken = 0;
        this.mInstantAppInstallerInfo = new ResolveInfo();
        this.mIntentFilterVerificationStates = new SparseArray();
        this.mDefaultPermissionPolicy = null;
        this.mPendingBroadcasts = new PendingPackageBroadcasts();
        this.mContainerService = null;
        this.mDirtyUsers = new ArraySet();
        this.mDefContainerConn = new DefaultContainerConnection();
        this.mRunningInstalls = new SparseArray();
        this.mNextInstallToken = 1;
        this.mPackageUsage = new PackageUsage();
        this.mCompilerStats = new CompilerStats();
        this.mNeedClearDeviceForCTS = false;
        this.mStorageListener = new StorageEventListener() {
            public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
                if (vol.type == 1) {
                    if (vol.state == 2) {
                        String volumeUuid = vol.getFsUuid();
                        PackageManagerService.sUserManager.reconcileUsers(volumeUuid);
                        PackageManagerService.this.reconcileApps(volumeUuid);
                        PackageManagerService.this.mInstallerService.onPrivateVolumeMounted(volumeUuid);
                        PackageManagerService.this.loadPrivatePackages(vol);
                    } else if (vol.state == 5) {
                        PackageManagerService.this.unloadPrivatePackages(vol);
                    }
                }
                if ((vol.type == 0 && vol.isPrimary()) || (PackageManagerService.this.mCustPms != null && PackageManagerService.this.mCustPms.isSdVol(vol))) {
                    if (vol.state == 2) {
                        PackageManagerService.this.updateExternalMediaStatus(true, false);
                    } else if (vol.state == 5) {
                        PackageManagerService.this.updateExternalMediaStatus(false, false);
                    }
                }
            }

            public void onVolumeForgotten(String fsUuid) {
                if (TextUtils.isEmpty(fsUuid)) {
                    Slog.e(PackageManagerService.TAG, "Forgetting internal storage is probably a mistake; ignoring");
                    return;
                }
                synchronized (PackageManagerService.this.mPackages) {
                    for (PackageSetting ps : PackageManagerService.this.mSettings.getVolumePackagesLPr(fsUuid)) {
                        Slog.d(PackageManagerService.TAG, "Destroying " + ps.name + " because volume was forgotten");
                        PackageManagerService.this.deletePackageVersioned(new VersionedPackage(ps.name, -1), new LegacyPackageDeleteObserver(null).getBinder(), 0, 2);
                        AttributeCache.instance().removePackage(ps.name);
                    }
                    PackageManagerService.this.mSettings.onVolumeForgotten(fsUuid);
                    PackageManagerService.this.mSettings.writeLPr();
                }
            }
        };
        this.mMediaMounted = false;
        this.mMonitor = HwFrameworkFactory.getHwFrameworkMonitor();
        this.mIBootAnmation = null;
        this.mHwPMSEx = null;
        this.mHwInnerService = new HwInnerPackageManagerService(this);
        this.mHwPMSEx = HwServiceExFactory.getHwPackageManagerServiceEx(this, context);
        LockGuard.installLock(this.mPackages, 3);
        Trace.traceBegin(262144, "create package manager");
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_START, SystemClock.uptimeMillis());
        Jlog.d(31, "JL_BOOT_PROGRESS_PMS_START");
        if (this.mSdkVersion <= 0) {
            Slog.w(TAG, "**** ro.build.version.sdk not set!");
        }
        this.mContext = context;
        this.mPermissionReviewRequired = context.getResources().getBoolean(17956996);
        if (HWFLOW) {
            this.startTimer = SystemClock.uptimeMillis();
        }
        this.mFactoryTest = factoryTest;
        this.mOnlyCore = onlyCore;
        this.mMetrics = new DisplayMetrics();
        this.mSettings = new Settings(this.mPackages);
        this.mSettings.addSharedUserLPw("android.uid.system", 1000, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.phone", 1001, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.log", LOG_UID, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.nfc", 1027, 1, 8);
        this.mSettings.addSharedUserLPw("com.nxp.uid.nfceeapi", SPI_UID, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.bluetooth", BLUETOOTH_UID, 1, 8);
        this.mSettings.addSharedUserLPw("android.uid.shell", 2000, 1, 8);
        HwServiceFactory.getHwPackageServiceManager().addHwSharedUserLP(this.mSettings);
        String separateProcesses = SystemProperties.get("debug.separate_processes");
        if (separateProcesses == null || separateProcesses.length() <= 0) {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = null;
        } else if ("*".equals(separateProcesses)) {
            this.mDefParseFlags = 8;
            this.mSeparateProcesses = null;
            Slog.w(TAG, "Running with debug.separate_processes: * (ALL)");
        } else {
            this.mDefParseFlags = 0;
            this.mSeparateProcesses = separateProcesses.split(",");
            Slog.w(TAG, "Running with debug.separate_processes: " + separateProcesses);
        }
        this.mInstaller = installer;
        this.mPackageDexOptimizer = new PackageDexOptimizer(installer, this.mInstallLock, context, "*dexopt*");
        this.mDexManager = new DexManager(this, this.mPackageDexOptimizer, installer, this.mInstallLock);
        this.mMoveCallbacks = new MoveCallbacks(FgThread.get().getLooper());
        this.mOnPermissionChangeListeners = new OnPermissionChangeListeners(FgThread.get().getLooper());
        getDefaultDisplayMetrics(context, this.mMetrics);
        Trace.traceBegin(262144, "get system config");
        SystemConfig systemConfig = SystemConfig.getInstance();
        this.mGlobalGids = systemConfig.getGlobalGids();
        this.mSystemPermissions = systemConfig.getSystemPermissions();
        this.mAvailableFeatures = systemConfig.getAvailableFeatures();
        Trace.traceEnd(262144);
        this.mProtectedPackages = new ProtectedPackages(this.mContext);
        if (HWFLOW) {
            str = TAG;
            append = new StringBuilder().append("TimerCounter = ");
            int i = this.mTimerCounter + 1;
            this.mTimerCounter = i;
            Slog.i(str, append.append(i).append(" **** Config Init  ************ Time to elapsed: ").append(SystemClock.uptimeMillis() - this.startTimer).append(" ms").toString());
        }
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                int i2;
                this.mHandlerThread = new ServiceThread(TAG, 10, true);
                this.mHandlerThread.start();
                this.mHandler = new PackageHandler(this.mHandlerThread.getLooper());
                this.mProcessLoggingHandler = new ProcessLoggingHandler();
                Watchdog.getInstance().addThread(this.mHandler, WATCHDOG_TIMEOUT);
                this.mDefaultPermissionPolicy = HwServiceFactory.getHwDefaultPermissionGrantPolicy(this.mContext, this);
                Slog.i(TAG, "mDefaultPermissionPolicy :" + this.mDefaultPermissionPolicy);
                this.mInstantAppRegistry = new InstantAppRegistry(this);
                File dataDir = Environment.getDataDirectory();
                this.mAppInstallDir = new File(dataDir, "app");
                this.mAppLib32InstallDir = new File(dataDir, "app-lib");
                this.mAsecInternalPath = new File(dataDir, "app-asec").getPath();
                this.mDrmAppPrivateInstallDir = new File(dataDir, "app-private");
                try {
                    Slog.i(TAG, "UserManagerService");
                    IHwUserManagerService service = HwServiceFactory.getHwUserManagerService();
                    if (service != null) {
                        sUserManager = service.getInstance(context, this, new UserDataPreparer(this.mInstaller, this.mInstallLock, this.mContext, this.mOnlyCore), this.mPackages);
                    } else {
                        sUserManager = new UserManagerService(context, this, new UserDataPreparer(this.mInstaller, this.mInstallLock, this.mContext, this.mOnlyCore), this.mPackages);
                    }
                } catch (Throwable e) {
                    Slog.e(TAG, "UserManagerService error " + e);
                }
                ArrayMap<String, PermissionEntry> permConfig = systemConfig.getPermissions();
                for (i2 = 0; i2 < permConfig.size(); i2++) {
                    PermissionEntry perm = (PermissionEntry) permConfig.valueAt(i2);
                    BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(perm.name);
                    if (bp == null) {
                        BasePermission basePermission = new BasePermission(perm.name, PLATFORM_PACKAGE_NAME, 1);
                        this.mSettings.mPermissions.put(perm.name, basePermission);
                    }
                    if (perm.gids != null) {
                        bp.setGids(perm.gids, perm.perUser);
                    }
                }
                ArrayMap<String, String> libConfig = systemConfig.getSharedLibraries();
                int builtInLibCount = libConfig.size();
                for (i2 = 0; i2 < builtInLibCount; i2++) {
                    addSharedLibraryLPw((String) libConfig.valueAt(i2), null, (String) libConfig.keyAt(i2), -1, 0, PLATFORM_PACKAGE_NAME, 0);
                }
                this.mFoundPolicyFile = SELinuxMMAC.readInstallPolicy();
                initHwCertificationManager();
                Trace.traceBegin(262144, "read user settings");
                this.mFirstBoot = this.mSettings.readLPw(sUserManager.getUsers(false)) ^ 1;
                Trace.traceEnd(262144);
                for (i2 = this.mSettings.mPackages.size() - 1; i2 >= 0; i2--) {
                    PackageSetting ps = (PackageSetting) this.mSettings.mPackages.valueAt(i2);
                    if (!isExternal(ps) && ((ps.codePath == null || (ps.codePath.exists() ^ 1) != 0) && this.mSettings.getDisabledSystemPkgLPr(ps.name) != null)) {
                        this.mSettings.mPackages.removeAt(i2);
                        this.mSettings.enableSystemPackageLPw(ps.name);
                    }
                }
                if (this.mFirstBoot) {
                    requestCopyPreoptedFiles();
                }
                initCertCompatSettings();
                String customResolverActivity = Resources.getSystem().getString(17039762);
                customResolverActivity = HwFrameworkFactory.getHuaweiResolverActivity(this.mContext);
                if (!TextUtils.isEmpty(customResolverActivity)) {
                    this.mCustomResolverComponentName = ComponentName.unflattenFromString(customResolverActivity);
                }
                startTime = SystemClock.uptimeMillis();
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SYSTEM_SCAN_START, startTime);
                String bootClassPath = System.getenv("BOOTCLASSPATH");
                String systemServerClassPath = System.getenv("SYSTEMSERVERCLASSPATH");
                if (bootClassPath == null) {
                    Slog.w(TAG, "No BOOTCLASSPATH found!");
                }
                if (systemServerClassPath == null) {
                    Slog.w(TAG, "No SYSTEMSERVERCLASSPATH found!");
                }
                file = new File(Environment.getRootDirectory(), "framework");
            }
        }
        VersionInfo ver = this.mSettings.getInternalVersion();
        this.mIsUpgrade = ver != null ? Build.FINGERPRINT.equals(ver.fingerprint) ^ 1 : false;
        if (this.mIsUpgrade) {
            logCriticalInfo(4, "Upgrading from " + ver.fingerprint + " to " + Build.FINGERPRINT);
        }
        loadCorrectUninstallDelapp();
        if (HWFLOW) {
            this.startTimer = SystemClock.uptimeMillis();
        }
        loadSysWhitelist();
        if (HWFLOW) {
            str = TAG;
            append = new StringBuilder().append("TimerCounter = ");
            i = this.mTimerCounter + 1;
            this.mTimerCounter = i;
            Slog.i(str, append.append(i).append(" **** loadSysWhitelist  ************ Time to elapsed: ").append(SystemClock.uptimeMillis() - this.startTimer).append(" ms").toString());
        }
        synchronized (this.mPackages) {
            resetSharedUserSignaturesIfNeeded();
        }
        if (this.mIsUpgrade && (onlyCore ^ 1) != 0) {
            deletePackagesAbiFile();
        }
        boolean z = this.mIsUpgrade && ver.sdkVersion <= 22;
        this.mPromoteSystemApps = z;
        z = this.mIsUpgrade && ver.sdkVersion < 24;
        this.mIsPreNUpgrade = z;
        z = this.mIsUpgrade && ver.sdkVersion < 25;
        this.mIsPreNMR1Upgrade = z;
        if (this.mPromoteSystemApps) {
            for (PackageSetting ps2 : this.mSettings.mPackages.values()) {
                if (isSystemApp(ps2)) {
                    this.mExistingSystemPackages.add(ps2.name);
                }
            }
        }
        String originalCotaVersion = SystemProperties.get("persist.sys.cotaPkgVersion", "");
        String cotaVersion = SystemProperties.get("ro.product.CotaVersion", "");
        boolean isCotaUpdate = originalCotaVersion.equals(cotaVersion) ^ 1;
        this.mCacheDir = preparePackageParserCache(!this.mIsUpgrade ? isCotaUpdate : true);
        if (isCotaUpdate) {
            SystemProperties.set("persist.sys.cotaPkgVersion", cotaVersion);
        }
        int scanFlags = 4160;
        if (this.mIsUpgrade || this.mFirstBoot) {
            scanFlags = 69696;
        }
        readPreInstallApkList();
        Iterable fileList = null;
        try {
            fileList = HwCfgFilePolicy.getCfgFileList("/overlay", 1);
        } catch (NoClassDefFoundError er) {
            Slog.e(TAG, er.getMessage());
        }
        if (r49 != null) {
            for (File file2 : r49) {
                scanDirTracedLI(file2, ((this.mDefParseFlags | 1) | 64) | 512, scanFlags | 128, 0);
            }
        } else {
            scanDirTracedLI(new File(VENDOR_OVERLAY_DIR), ((this.mDefParseFlags | 1) | 64) | 512, scanFlags | 128, 0);
        }
        if (HWFLOW) {
            this.startTimer = SystemClock.uptimeMillis();
        }
        getUninstallApk();
        if (HWFLOW) {
            str = TAG;
            append = new StringBuilder().append("TimerCounter = ");
            i = this.mTimerCounter + 1;
            this.mTimerCounter = i;
            Slog.i(str, append.append(i).append(" **** getUninstallApk  ************ Time to elapsed: ").append(SystemClock.uptimeMillis() - this.startTimer).append(" ms").toString());
        }
        this.mParallelPackageParserCallback.findStaticOverlayPackages();
        scanDirTracedLI(file, ((this.mDefParseFlags | 1) | 64) | 128, scanFlags | 2, 0);
        File privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app");
        scanDirTracedLI(privilegedAppDir, ((this.mDefParseFlags | 1) | 64) | 128, scanFlags, 0);
        File systemAppDir = new File(Environment.getRootDirectory(), "app");
        scanDirTracedLI(systemAppDir, (this.mDefParseFlags | 1) | 64, scanFlags, 0);
        file = new File("/vendor/app");
        try {
            File vendorAppDir = file.getCanonicalFile();
        } catch (IOException e2) {
        }
        if (HWFLOW) {
            this.startTimer = SystemClock.uptimeMillis();
        }
        scanNonSystemPartitionDir(scanFlags);
        if (HWFLOW) {
            str = TAG;
            append = new StringBuilder().append("TimerCounter = ");
            i = this.mTimerCounter + 1;
            this.mTimerCounter = i;
            Slog.i(str, append.append(i).append(" **** scanNonSystemPartitionDir  ************ Time to elapsed: ").append(SystemClock.uptimeMillis() - this.startTimer).append(" ms").toString());
        }
        if (HWFLOW) {
            this.startTimer = SystemClock.uptimeMillis();
        }
        scanRemovableAppDir(scanFlags);
        if (HWFLOW) {
            str = TAG;
            append = new StringBuilder().append("TimerCounter = ");
            i = this.mTimerCounter + 1;
            this.mTimerCounter = i;
            Slog.i(str, append.append(i).append(" **** scanRemovableAppDir  ************ Time to elapsed: ").append(SystemClock.uptimeMillis() - this.startTimer).append(" ms").toString());
        }
        File oemAppDir = new File(Environment.getOemDirectory(), "app");
        scanDirTracedLI(oemAppDir, (this.mDefParseFlags | 1) | 64, scanFlags, 0);
        List<String> possiblyDeletedUpdatedSystemApps = new ArrayList();
        List<String> stubSystemApps = new ArrayList();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                if (!this.mOnlyCore) {
                    for (Package pkg : this.mPackages.values()) {
                        if (pkg.isStub) {
                            stubSystemApps.add(pkg.packageName);
                        }
                    }
                    Iterator<PackageSetting> psit = this.mSettings.mPackages.values().iterator();
                    while (psit.hasNext()) {
                        ps2 = (PackageSetting) psit.next();
                        if ((ps2.pkgFlags & 1) != 0) {
                            makeSetupDisabled(ps2.name);
                            Package scannedPkg = (Package) this.mPackages.get(ps2.name);
                            if (scannedPkg != null) {
                                if (this.mSettings.isDisabledSystemPackageLPr(ps2.name)) {
                                    logCriticalInfo(5, "Expecting better updated system app for " + ps2.name + "; removing system app.  Last known codePath=" + ps2.codePathString + ", installStatus=" + ps2.installStatus + ", versionCode=" + ps2.versionCode + "; scanned versionCode=" + scannedPkg.mVersionCode);
                                    removePackageLI(scannedPkg, true);
                                    if (!skipSetupEnable(ps2.name)) {
                                        this.mExpectingBetter.put(ps2.name, ps2.codePath);
                                    }
                                }
                            } else if (this.mSettings.isDisabledSystemPackageLPr(ps2.name)) {
                                PackageSetting disabledPs = this.mSettings.getDisabledSystemPkgLPr(ps2.name);
                                if (disabledPs.codePath == null || (disabledPs.codePath.exists() ^ 1) != 0 || disabledPs.pkg == null) {
                                    possiblyDeletedUpdatedSystemApps.add(ps2.name);
                                }
                            } else {
                                psit.remove();
                                logCriticalInfo(5, "System package " + ps2.name + " no longer exists; it's data will be wiped");
                                writeNetQinFlag(ps2.name);
                            }
                        }
                    }
                }
                ArrayList<PackageSetting> deletePkgsList = this.mSettings.getListOfIncompleteInstallPackagesLPr();
                for (i2 = 0; i2 < deletePkgsList.size(); i2++) {
                    String packageName = ((PackageSetting) deletePkgsList.get(i2)).name;
                    logCriticalInfo(5, "Cleaning up incompletely installed app: " + packageName);
                    synchronized (this.mPackages) {
                        this.mSettings.removePackageLPw(packageName);
                    }
                }
                deleteTempPackageFiles();
                this.mSettings.pruneSharedUsersLPw();
            }
        }
        int cachedSystemApps = PackageParser.sCachedPackageReadCount.get();
        long systemScanTime = SystemClock.uptimeMillis() - startTime;
        int systemPackagesCount = this.mPackages.size();
        str = TAG;
        append = new StringBuilder().append("Finished scanning system apps. Time: ").append(systemScanTime).append(" ms, packageCount: ").append(systemPackagesCount).append(" , timePerPackage: ");
        if (systemPackagesCount == 0) {
            j = 0;
        } else {
            j = systemScanTime / ((long) systemPackagesCount);
        }
        Slog.i(str, append.append(j).append(" , cached: ").append(cachedSystemApps).toString());
        if (this.mIsUpgrade && systemPackagesCount > 0) {
            MetricsLogger.histogram(null, "ota_package_manager_system_app_avg_scan_time", ((int) systemScanTime) / systemPackagesCount);
        }
        if (!this.mOnlyCore) {
            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_DATA_SCAN_START, SystemClock.uptimeMillis());
            scanDirTracedLI(this.mAppInstallDir, 0, scanFlags | 1024, 0);
            scanDirTracedLI(this.mDrmAppPrivateInstallDir, this.mDefParseFlags | 16, scanFlags | 1024, 0);
            for (String deletedAppName : possiblyDeletedUpdatedSystemApps) {
                String msg;
                Package deletedPkg = (Package) this.mPackages.get(deletedAppName);
                this.mSettings.removeDisabledSystemPackageLPw(deletedAppName);
                if (deletedPkg == null) {
                    msg = "Updated system package " + deletedAppName + " no longer exists; removing its data";
                } else {
                    msg = "Updated system package + " + deletedAppName + " no longer exists; revoking system privileges";
                    PackageSetting deletedPs = (PackageSetting) this.mSettings.mPackages.get(deletedAppName);
                    ApplicationInfo applicationInfo = deletedPkg.applicationInfo;
                    applicationInfo.flags &= -2;
                    deletedPs.pkgFlags &= -2;
                }
                logCriticalInfo(5, msg);
            }
            for (i2 = 0; i2 < this.mExpectingBetter.size(); i2++) {
                packageName = (String) this.mExpectingBetter.keyAt(i2);
                if (!this.mPackages.containsKey(packageName)) {
                    File scanFile = (File) this.mExpectingBetter.valueAt(i2);
                    logCriticalInfo(5, "Expected better " + packageName + " but never showed up; reverting to system");
                    int reparseFlags = this.mDefParseFlags;
                    if (FileUtils.contains(privilegedAppDir, scanFile)) {
                        reparseFlags = HdmiCecKeycode.UI_SOUND_PRESENTATION_TREBLE_STEP_PLUS;
                    } else if (FileUtils.contains(systemAppDir, scanFile)) {
                        reparseFlags = 65;
                    } else if (FileUtils.contains(vendorAppDir, scanFile)) {
                        reparseFlags = 65;
                    } else if (FileUtils.contains(oemAppDir, scanFile)) {
                        reparseFlags = 65;
                    } else {
                        Slog.e(TAG, "Ignoring unexpected fallback path " + scanFile);
                    }
                    this.mSettings.enableSystemPackageLPw(packageName);
                    try {
                        scanPackageTracedLI(scanFile, reparseFlags, scanFlags, 0, null);
                    } catch (PackageManagerException e3) {
                        Slog.e(TAG, "Failed to parse original system package: " + e3.getMessage());
                    }
                }
            }
            decompressSystemApplications(stubSystemApps, scanFlags);
            int cachedNonSystemApps = PackageParser.sCachedPackageReadCount.get() - cachedSystemApps;
            long dataScanTime = (SystemClock.uptimeMillis() - systemScanTime) - startTime;
            int dataPackagesCount = this.mPackages.size() - systemPackagesCount;
            str = TAG;
            append = new StringBuilder().append("Finished scanning non-system apps. Time: ").append(dataScanTime).append(" ms, packageCount: ").append(dataPackagesCount).append(" , timePerPackage: ");
            if (dataPackagesCount == 0) {
                j = 0;
            } else {
                j = dataScanTime / ((long) dataPackagesCount);
            }
            Slog.i(str, append.append(j).append(" , cached: ").append(cachedNonSystemApps).toString());
            if (this.mIsUpgrade && dataPackagesCount > 0) {
                MetricsLogger.histogram(null, "ota_package_manager_data_app_avg_scan_time", ((int) dataScanTime) / dataPackagesCount);
            }
        }
        this.mExpectingBetter.clear();
        this.mStorageManagerPackage = getStorageManagerPackageName();
        this.mSetupWizardPackage = getSetupWizardPackageName();
        if (this.mProtectedFilters.size() > 0) {
            for (ActivityIntentInfo filter : this.mProtectedFilters) {
                if (!filter.activity.info.packageName.equals(this.mSetupWizardPackage)) {
                    filter.setPriority(0);
                }
            }
        }
        this.mDeferProtectedFilters = false;
        this.mProtectedFilters.clear();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                int storageFlags;
                updateAllSharedLibrariesLPw(null);
                for (SharedUserSetting setting : this.mSettings.getAllSharedUsersLPw()) {
                    adjustCpuAbisForSharedUserLPw(setting.packages, null);
                }
                this.mPackageUsage.read(this.mPackages);
                this.mCompilerStats.read();
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SCAN_END, SystemClock.uptimeMillis());
                Slog.i(TAG, "Time to scan packages: " + (((float) (SystemClock.uptimeMillis() - startTime)) / 1000.0f) + " seconds");
                if (HWFLOW) {
                    this.startTimer = SystemClock.uptimeMillis();
                }
                int updateFlags = 1;
                if (!(ver == null || ver.sdkVersion == this.mSdkVersion)) {
                    Slog.i(TAG, "Platform changed from " + ver.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for internal storage");
                    updateFlags = 7;
                }
                updatePermissionsLPw(null, null, StorageManager.UUID_PRIVATE_INTERNAL, updateFlags);
                if (ver != null) {
                    ver.sdkVersion = this.mSdkVersion;
                }
                if (!onlyCore && (this.mPromoteSystemApps || this.mFirstBoot)) {
                    for (UserInfo user : sUserManager.getUsers(true)) {
                        this.mSettings.applyDefaultPreferredAppsLPw(this, user.id);
                        applyFactoryDefaultBrowserLPw(user.id);
                        primeDomainVerificationsLPw(user.id);
                    }
                }
                if (StorageManager.isFileEncryptedNativeOrEmulated()) {
                    storageFlags = 1;
                } else {
                    storageFlags = 3;
                }
                this.mPrepareAppDataFuture = SystemServerInitThreadPool.get().submit(new -$Lambda$i1ZZeLvwPPAZVBl_nnQ0C2t5oMs((byte) 1, storageFlags, this, reconcileAppsDataLI(StorageManager.UUID_PRIVATE_INTERNAL, 0, storageFlags, true, true)), "prepareAppData");
                if (this.mIsUpgrade && (onlyCore ^ 1) != 0) {
                    Slog.i(TAG, "Build fingerprint changed; clearing code caches");
                    for (i2 = 0; i2 < this.mSettings.mPackages.size(); i2++) {
                        ps2 = (PackageSetting) this.mSettings.mPackages.valueAt(i2);
                        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, ps2.volumeUuid)) {
                            clearAppDataLIF(ps2.pkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                        }
                    }
                    ver.fingerprint = Build.FINGERPRINT;
                }
                checkAndEnableWebview();
                checkDefaultBrowser();
                this.mExistingSystemPackages.clear();
                this.mPromoteSystemApps = false;
                if (ver != null) {
                    ver.databaseVersion = 3;
                }
                if (HWFLOW) {
                    str = TAG;
                    append = new StringBuilder().append("TimerCounter = ");
                    int i3 = this.mTimerCounter + 1;
                    this.mTimerCounter = i3;
                    Slog.i(str, append.append(i3).append(" **** checkDefaultBrowser  ************ Time to elapsed: ").append(SystemClock.uptimeMillis() - this.startTimer).append(" ms").toString());
                }
                if (HWFLOW) {
                    this.startTimer = SystemClock.uptimeMillis();
                }
                this.mSettings.writeLPr();
                if (HWFLOW) {
                    str = TAG;
                    append = new StringBuilder().append("TimerCounter = ");
                    i3 = this.mTimerCounter + 1;
                    this.mTimerCounter = i3;
                    Slog.i(str, append.append(i3).append(" **** mSettings.writeLPr  ************ Time to elapsed: ").append(SystemClock.uptimeMillis() - this.startTimer).append(" ms").toString());
                }
                writeCertCompatPackages(true);
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_READY, SystemClock.uptimeMillis());
                Jlog.d(32, "JL_BOOT_PROGRESS_PMS_READY");
                if (this.mOnlyCore) {
                    this.mRequiredVerifierPackage = null;
                    this.mRequiredInstallerPackage = null;
                    this.mRequiredUninstallerPackage = null;
                    this.mIntentFilterVerifierComponent = null;
                    this.mIntentFilterVerifier = null;
                    this.mServicesSystemSharedLibraryPackageName = null;
                    this.mSharedSystemSharedLibraryPackageName = null;
                } else {
                    this.mRequiredVerifierPackage = getRequiredButNotReallyRequiredVerifierLPr();
                    this.mRequiredInstallerPackage = getRequiredInstallerLPr();
                    this.mRequiredUninstallerPackage = getRequiredUninstallerLPr();
                    this.mIntentFilterVerifierComponent = getIntentFilterVerifierComponentNameLPr();
                    if (this.mIntentFilterVerifierComponent != null) {
                        this.mIntentFilterVerifier = new IntentVerifierProxy(this.mContext, this.mIntentFilterVerifierComponent);
                    } else {
                        this.mIntentFilterVerifier = null;
                    }
                    this.mServicesSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr("android.ext.services", -1);
                    this.mSharedSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr("android.ext.shared", -1);
                }
                this.mInstallerService = new PackageInstallerService(context, this);
                Pair<ComponentName, String> instantAppResolverComponent = getInstantAppResolverLPr();
                if (instantAppResolverComponent != null) {
                    if (DEBUG_EPHEMERAL) {
                        Slog.d(TAG, "Set ephemeral resolver: " + instantAppResolverComponent);
                    }
                    this.mInstantAppResolverConnection = new EphemeralResolverConnection(this.mContext, (ComponentName) instantAppResolverComponent.first, (String) instantAppResolverComponent.second);
                    this.mInstantAppResolverSettingsComponent = getInstantAppResolverSettingsLPr((ComponentName) instantAppResolverComponent.first);
                } else {
                    this.mInstantAppResolverConnection = null;
                    this.mInstantAppResolverSettingsComponent = null;
                }
                updateInstantAppInstallerLocked(null);
                Map<Integer, List<PackageInfo>> userPackages = new HashMap();
                for (int userId : UserManagerService.getInstance().getUserIds()) {
                    userPackages.put(Integer.valueOf(userId), getInstalledPackages(0, userId).getList());
                }
                this.mDexManager.load(userPackages);
                if (this.mIsUpgrade) {
                    MetricsLogger.histogram(null, "ota_package_manager_init_time", (int) (SystemClock.uptimeMillis() - startTime));
                }
            }
        }
        Trace.traceBegin(262144, "GC");
        Runtime.getRuntime().gc();
        Trace.traceEnd(262144);
        Trace.traceBegin(262144, "loadFallbacks");
        FallbackCategoryProvider.loadFallbacks();
        Trace.traceEnd(262144);
        this.mInstaller.setWarnIfHeld(this.mPackages);
        packageManagerService = this;
        LocalServices.addService(PackageManagerInternal.class, new PackageManagerInternalImpl());
        Trace.traceEnd(262144);
    }

    /* synthetic */ void lambda$-com_android_server_pm_PackageManagerService_164333(List deferPackages, int storageFlags) {
        TimingsTraceLog traceLog = new TimingsTraceLog("SystemServerTimingAsync", 262144);
        traceLog.traceBegin("AppDataFixup");
        try {
            this.mInstaller.fixupAppData(StorageManager.UUID_PRIVATE_INTERNAL, 3);
        } catch (InstallerException e) {
            Slog.w(TAG, "Trouble fixing GIDs", e);
        }
        traceLog.traceEnd();
        traceLog.traceBegin("AppDataPrepare");
        if (deferPackages != null && !deferPackages.isEmpty()) {
            int count = 0;
            for (String pkgName : deferPackages) {
                Package pkg = null;
                synchronized (this.mPackages) {
                    PackageSetting ps = this.mSettings.getPackageLPr(pkgName);
                    if (ps != null && ps.getInstalled(0)) {
                        pkg = ps.pkg;
                    }
                }
                if (pkg != null) {
                    synchronized (this.mInstallLock) {
                        prepareAppDataAndMigrateLIF(pkg, 0, storageFlags, true);
                    }
                    count++;
                }
            }
            traceLog.traceEnd();
            Slog.i(TAG, "Deferred reconcileAppsData finished " + count + " packages");
        }
    }

    private void checkAndEnableWebview() {
        if (this.mIsUpgrade) {
            String pkgWebViewName = "com.google.android.webview";
            boolean isChina = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", ""));
            try {
                int state = getApplicationEnabledSetting(pkgWebViewName, this.mContext.getUserId());
                boolean isEnabled = state == 1;
                Slog.i(TAG, "WebViewGoogle state=" + state + " version is china = " + isChina);
                if (!isEnabled && isChina) {
                    Slog.i(TAG, "current WebViewGoogle disable, enable it");
                    setApplicationEnabledSetting(pkgWebViewName, 1, 0, this.mContext.getUserId(), this.mContext.getOpPackageName());
                }
            } catch (Exception e) {
                Slog.w(TAG, "enable WebViewGoogle exception " + e.getMessage());
            }
        }
    }

    private void decompressSystemApplications(List<String> stubSystemApps, int scanFlags) {
        int i;
        for (i = stubSystemApps.size() - 1; i >= 0; i--) {
            String pkgName = (String) stubSystemApps.get(i);
            if (this.mSettings.isDisabledSystemPackageLPr(pkgName)) {
                stubSystemApps.remove(i);
            } else {
                Package pkg = (Package) this.mPackages.get(pkgName);
                if (pkg == null) {
                    stubSystemApps.remove(i);
                } else {
                    PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
                    if (ps == null || ps.getEnabled(0) != 3) {
                        if (DEBUG_COMPRESSION) {
                            Slog.i(TAG, "Uncompressing system stub; pkg: " + pkgName);
                        }
                        File scanFile = decompressPackage(pkg);
                        if (scanFile != null) {
                            try {
                                this.mSettings.disableSystemPackageLPw(pkgName, true);
                                removePackageLI(pkg, true);
                                scanPackageTracedLI(scanFile, 0, scanFlags, 0, null);
                                ps.setEnabled(0, 0, PLATFORM_PACKAGE_NAME);
                                stubSystemApps.remove(i);
                            } catch (PackageManagerException e) {
                                Slog.e(TAG, "Failed to parse uncompressed system package: " + e.getMessage());
                            }
                        }
                    } else {
                        stubSystemApps.remove(i);
                    }
                }
            }
        }
        for (i = stubSystemApps.size() - 1; i >= 0; i--) {
            pkgName = (String) stubSystemApps.get(i);
            ((PackageSetting) this.mSettings.mPackages.get(pkgName)).setEnabled(2, 0, PLATFORM_PACKAGE_NAME);
            logCriticalInfo(6, "Stub disabled; pkg: " + pkgName);
        }
    }

    private int decompressFile(File srcFile, File dstFile) throws ErrnoException {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        if (DEBUG_COMPRESSION) {
            Slog.i(TAG, "Decompress file; src: " + srcFile.getAbsolutePath() + ", dst: " + dstFile.getAbsolutePath());
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            OutputStream fileOut;
            InputStream fileIn = new GZIPInputStream(new FileInputStream(srcFile));
            try {
                fileOut = new FileOutputStream(dstFile, false);
            } catch (Throwable th4) {
                th = th4;
                inputStream = fileIn;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable th5) {
                        th2 = th5;
                        if (th3 != null) {
                            if (th3 != th2) {
                                th3.addSuppressed(th2);
                                th2 = th3;
                            }
                        }
                    }
                }
                th2 = th3;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable th6) {
                        th3 = th6;
                        if (th2 != null) {
                            if (th2 != th3) {
                                th2.addSuppressed(th3);
                                th3 = th2;
                            }
                        }
                    }
                }
                th3 = th2;
                if (th3 != null) {
                    throw th;
                }
                try {
                    throw th3;
                } catch (IOException e) {
                    logCriticalInfo(6, "Failed to decompress file; src: " + srcFile.getAbsolutePath() + ", dst: " + dstFile.getAbsolutePath());
                    return RequestStatus.SYS_ETIMEDOUT;
                }
            }
            try {
                Streams.copy(fileIn, fileOut);
                Os.chmod(dstFile.getAbsolutePath(), 420);
                if (fileOut != null) {
                    try {
                        fileOut.close();
                    } catch (Throwable th7) {
                        th3 = th7;
                    }
                }
                if (fileIn != null) {
                    try {
                        fileIn.close();
                    } catch (Throwable th8) {
                        th = th8;
                        if (th3 != null) {
                            if (th3 != th) {
                                th3.addSuppressed(th);
                                th = th3;
                            }
                        }
                    }
                }
                th = th3;
                if (th == null) {
                    return 1;
                }
                try {
                    throw th;
                } catch (IOException e2) {
                    inputStream = fileIn;
                }
            } catch (Throwable th9) {
                th = th9;
                outputStream = fileOut;
                inputStream = fileIn;
                if (outputStream != null) {
                    outputStream.close();
                }
                th2 = th3;
                if (inputStream != null) {
                    inputStream.close();
                }
                th3 = th2;
                if (th3 != null) {
                    throw th3;
                }
                throw th;
            }
        } catch (Throwable th10) {
            th = th10;
            if (outputStream != null) {
                outputStream.close();
            }
            th2 = th3;
            if (inputStream != null) {
                inputStream.close();
            }
            th3 = th2;
            if (th3 != null) {
                throw th3;
            }
            throw th;
        }
    }

    private File[] getCompressedFiles(String codePath) {
        File stubCodePath = new File(codePath);
        String stubName = stubCodePath.getName();
        int idx = stubName.lastIndexOf(STUB_SUFFIX);
        if (idx < 0 || stubName.length() != STUB_SUFFIX.length() + idx) {
            return null;
        }
        File stubParentDir = stubCodePath.getParentFile();
        if (stubParentDir == null) {
            Slog.e(TAG, "Unable to determine stub parent dir for codePath: " + codePath);
            return null;
        }
        File[] files = new File(stubParentDir, stubName.substring(0, idx)).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(PackageManagerService.COMPRESSED_EXTENSION);
            }
        });
        if (DEBUG_COMPRESSION && files != null && files.length > 0) {
            Slog.i(TAG, "getCompressedFiles[" + codePath + "]: " + Arrays.toString(files));
        }
        return files;
    }

    private boolean compressedFileExists(String codePath) {
        File[] compressedFiles = getCompressedFiles(codePath);
        if (compressedFiles == null || compressedFiles.length <= 0) {
            return false;
        }
        return true;
    }

    private void updateInstantAppInstallerLocked(String modifiedPackage) {
        if (this.mInstantAppInstallerActivity == null || (this.mInstantAppInstallerActivity.getComponentName().getPackageName().equals(modifiedPackage) ^ 1) == 0) {
            setUpInstantAppInstallerActivityLP(getInstantAppInstallerLPr());
        }
    }

    private static File preparePackageParserCache(boolean isUpgrade) {
        if (Build.IS_ENG) {
            return null;
        }
        if (SystemProperties.getBoolean("pm.boot.disable_package_cache", false)) {
            Slog.i(TAG, "Disabling package parser cache due to system property.");
            return null;
        }
        File cacheBaseDir = FileUtils.createDir(Environment.getDataSystemDirectory(), "package_cache");
        if (cacheBaseDir == null) {
            return null;
        }
        if (isUpgrade) {
            FileUtils.deleteContents(cacheBaseDir);
        }
        File cacheDir = FileUtils.createDir(cacheBaseDir, PACKAGE_PARSER_CACHE_VERSION);
        if (Build.IS_USERDEBUG && VERSION.INCREMENTAL.startsWith("eng.")) {
            Slog.w(TAG, "Wiping cache directory because the system partition changed.");
            if (cacheDir.lastModified() < new File(Environment.getRootDirectory(), "framework").lastModified()) {
                FileUtils.deleteContents(cacheBaseDir);
                cacheDir = FileUtils.createDir(cacheBaseDir, PACKAGE_PARSER_CACHE_VERSION);
            }
        }
        return cacheDir;
    }

    public boolean isFirstBoot() {
        return this.mFirstBoot;
    }

    public boolean isOnlyCoreApps() {
        return this.mOnlyCore;
    }

    public boolean isUpgrade() {
        return this.mIsUpgrade;
    }

    private String getRequiredButNotReallyRequiredVerifierLPr() {
        List<ResolveInfo> matches = queryIntentReceiversInternal(new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION"), PACKAGE_MIME_TYPE, 1835008, 0, false);
        if (matches.size() == 1) {
            return ((ResolveInfo) matches.get(0)).getComponentInfo().packageName;
        }
        if (matches.size() == 0) {
            Log.e(TAG, "There should probably be a verifier, but, none were found");
            return null;
        }
        throw new RuntimeException("There must be exactly one verifier; found " + matches);
    }

    private String getRequiredSharedLibraryLPr(String name, int version) {
        String str;
        synchronized (this.mPackages) {
            SharedLibraryEntry libraryEntry = getSharedLibraryEntryLPr(name, version);
            if (libraryEntry == null) {
                throw new IllegalStateException("Missing required shared library:" + name);
            }
            str = libraryEntry.apk;
        }
        return str;
    }

    private String getRequiredInstallerLPr() {
        Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        List<ResolveInfo> matches = queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, 1835008, 0);
        if (matches.size() != 1) {
            throw new RuntimeException("There must be exactly one installer; found " + matches);
        } else if (((ResolveInfo) matches.get(0)).activityInfo.applicationInfo.isPrivilegedApp()) {
            return ((ResolveInfo) matches.get(0)).getComponentInfo().packageName;
        } else {
            throw new RuntimeException("The installer must be a privileged app");
        }
    }

    private String getRequiredUninstallerLPr() {
        Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setData(Uri.fromParts("package", "foo.bar", null));
        ResolveInfo resolveInfo = resolveIntent(intent, null, 1835008, 0);
        if (resolveInfo != null && !this.mResolveActivity.name.equals(resolveInfo.getComponentInfo().name)) {
            return resolveInfo.getComponentInfo().packageName;
        }
        throw new RuntimeException("There must be exactly one uninstaller; found " + resolveInfo);
    }

    private ComponentName getIntentFilterVerifierComponentNameLPr() {
        List<ResolveInfo> matches = queryIntentReceiversInternal(new Intent("android.intent.action.INTENT_FILTER_NEEDS_VERIFICATION"), PACKAGE_MIME_TYPE, 1835008, 0, false);
        ResolveInfo best = null;
        int N = matches.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo cur = (ResolveInfo) matches.get(i);
            if (checkPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", cur.getComponentInfo().packageName, 0) == 0 && (best == null || cur.priority > best.priority)) {
                best = cur;
            }
        }
        if (best != null) {
            return best.getComponentInfo().getComponentName();
        }
        Slog.w(TAG, "Intent filter verifier not found");
        return null;
    }

    public ComponentName getInstantAppResolverComponent() {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            Pair<ComponentName, String> instantAppResolver = getInstantAppResolverLPr();
            if (instantAppResolver == null) {
                return null;
            }
            ComponentName componentName = (ComponentName) instantAppResolver.first;
            return componentName;
        }
    }

    private Pair<ComponentName, String> getInstantAppResolverLPr() {
        String[] packageArray = this.mContext.getResources().getStringArray(17236008);
        if (packageArray.length != 0 || (Build.IS_DEBUGGABLE ^ 1) == 0) {
            int i;
            int callingUid = Binder.getCallingUid();
            if (Build.IS_DEBUGGABLE) {
                i = 0;
            } else {
                i = 1048576;
            }
            int resolveFlags = 786432 | i;
            String actionName = "android.intent.action.RESOLVE_INSTANT_APP_PACKAGE";
            Intent resolverIntent = new Intent(actionName);
            List<ResolveInfo> resolvers = queryIntentServicesInternal(resolverIntent, null, resolveFlags, 0, callingUid, false);
            if (resolvers.size() == 0) {
                if (DEBUG_EPHEMERAL) {
                    Slog.d(TAG, "Ephemeral resolver not found with new action; try old one");
                }
                actionName = "android.intent.action.RESOLVE_EPHEMERAL_PACKAGE";
                resolverIntent.setAction(actionName);
                resolvers = queryIntentServicesInternal(resolverIntent, null, resolveFlags, 0, callingUid, false);
            }
            int N = resolvers.size();
            if (N == 0) {
                if (DEBUG_EPHEMERAL) {
                    Slog.d(TAG, "Ephemeral resolver NOT found; no matching intent filters");
                }
                return null;
            }
            Set<String> possiblePackages = new ArraySet(Arrays.asList(packageArray));
            for (int i2 = 0; i2 < N; i2++) {
                ResolveInfo info = (ResolveInfo) resolvers.get(i2);
                if (info.serviceInfo != null) {
                    String packageName = info.serviceInfo.packageName;
                    if (possiblePackages.contains(packageName) || (Build.IS_DEBUGGABLE ^ 1) == 0) {
                        if (DEBUG_EPHEMERAL) {
                            Slog.v(TAG, "Ephemeral resolver found; pkg: " + packageName + ", info:" + info);
                        }
                        return new Pair(new ComponentName(packageName, info.serviceInfo.name), actionName);
                    } else if (DEBUG_EPHEMERAL) {
                        Slog.d(TAG, "Ephemeral resolver not in allowed package list; pkg: " + packageName + ", info:" + info);
                    }
                }
            }
            if (DEBUG_EPHEMERAL) {
                Slog.v(TAG, "Ephemeral resolver NOT found");
            }
            return null;
        }
        if (DEBUG_EPHEMERAL) {
            Slog.d(TAG, "Ephemeral resolver NOT found; empty package list");
        }
        return null;
    }

    private ActivityInfo getInstantAppInstallerLPr() {
        int i;
        Intent intent = new Intent("android.intent.action.INSTALL_INSTANT_APP_PACKAGE");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(new File("foo.apk")), PACKAGE_MIME_TYPE);
        if (Build.IS_DEBUGGABLE) {
            i = 0;
        } else {
            i = 1048576;
        }
        int resolveFlags = 786432 | i;
        List<ResolveInfo> matches = queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, resolveFlags, 0);
        if (matches.isEmpty()) {
            if (DEBUG_EPHEMERAL) {
                Slog.d(TAG, "Ephemeral installer not found with new action; try old one");
            }
            intent.setAction("android.intent.action.INSTALL_EPHEMERAL_PACKAGE");
            matches = queryIntentActivitiesInternal(intent, PACKAGE_MIME_TYPE, resolveFlags, 0);
        }
        Iterator<ResolveInfo> iter = matches.iterator();
        while (iter.hasNext()) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(((ResolveInfo) iter.next()).activityInfo.packageName);
            if (ps == null || !ps.getPermissionsState().hasPermission("android.permission.INSTALL_PACKAGES", 0)) {
                iter.remove();
            }
        }
        if (matches.size() == 0) {
            return null;
        }
        if (matches.size() == 1) {
            return (ActivityInfo) ((ResolveInfo) matches.get(0)).getComponentInfo();
        }
        throw new RuntimeException("There must be at most one ephemeral installer; found " + matches);
    }

    private ComponentName getInstantAppResolverSettingsLPr(ComponentName resolver) {
        Intent intent = new Intent("android.intent.action.INSTANT_APP_RESOLVER_SETTINGS").addCategory("android.intent.category.DEFAULT").setPackage(resolver.getPackageName());
        List<ResolveInfo> matches = queryIntentActivitiesInternal(intent, null, 786432, 0);
        if (matches.isEmpty()) {
            if (DEBUG_EPHEMERAL) {
                Slog.d(TAG, "Ephemeral resolver settings not found with new action; try old one");
            }
            intent.setAction("android.intent.action.EPHEMERAL_RESOLVER_SETTINGS");
            matches = queryIntentActivitiesInternal(intent, null, 786432, 0);
        }
        if (matches.isEmpty()) {
            return null;
        }
        return ((ResolveInfo) matches.get(0)).getComponentInfo().getComponentName();
    }

    private void primeDomainVerificationsLPw(int userId) {
        for (String packageName : SystemConfig.getInstance().getLinkedApps()) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "Unknown package " + packageName + " in sysconfig <app-link>");
            } else if (pkg.isSystemApp()) {
                ArraySet domains = null;
                for (Activity a : pkg.activities) {
                    for (ActivityIntentInfo filter : a.intents) {
                        if (hasValidDomains(filter)) {
                            if (domains == null) {
                                domains = new ArraySet();
                            }
                            domains.addAll(filter.getHostsList());
                        }
                    }
                }
                if (domains == null || domains.size() <= 0) {
                    Slog.w(TAG, "Sysconfig <app-link> package '" + packageName + "' does not handle web links");
                } else {
                    this.mSettings.createIntentFilterVerificationIfNeededLPw(packageName, domains).setStatus(0);
                    this.mSettings.updateIntentFilterVerificationStatusLPw(packageName, 2, userId);
                }
            } else {
                Slog.w(TAG, "Non-system app '" + packageName + "' in sysconfig <app-link>");
            }
        }
        scheduleWritePackageRestrictionsLocked(userId);
        scheduleWriteSettingsLocked();
    }

    private void applyFactoryDefaultBrowserLPw(int userId) {
        String browserPkg = this.mContext.getResources().getString(17039885);
        if (!TextUtils.isEmpty(browserPkg)) {
            if (((PackageSetting) this.mSettings.mPackages.get(browserPkg)) == null) {
                Slog.e(TAG, "Product default browser app does not exist: " + browserPkg);
                browserPkg = null;
            } else {
                this.mSettings.setDefaultBrowserPackageNameLPw(browserPkg, userId);
            }
        }
        if (browserPkg == null) {
            calculateDefaultBrowserLPw(userId);
        }
    }

    private void calculateDefaultBrowserLPw(int userId) {
        List<String> allBrowsers = resolveAllBrowserApps(userId);
        this.mSettings.setDefaultBrowserPackageNameLPw(allBrowsers.size() == 1 ? (String) allBrowsers.get(0) : null, userId);
    }

    private List<String> resolveAllBrowserApps(int userId) {
        List<ResolveInfo> list = queryIntentActivitiesInternal(sBrowserIntent, null, 131072, userId);
        int count = list.size();
        List<String> result = new ArrayList(count);
        for (int i = 0; i < count; i++) {
            ResolveInfo info = (ResolveInfo) list.get(i);
            if (!(info.activityInfo == null || (info.handleAllWebDataURI ^ 1) != 0 || (info.activityInfo.applicationInfo.flags & 1) == 0 || result.contains(info.activityInfo.packageName))) {
                result.add(info.activityInfo.packageName);
            }
        }
        return result;
    }

    private boolean packageIsBrowser(String packageName, int userId) {
        List<ResolveInfo> list = queryIntentActivitiesInternal(sBrowserIntent, null, 131072, userId);
        int N = list.size();
        for (int i = 0; i < N; i++) {
            if (packageName.equals(((ResolveInfo) list.get(i)).activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private void checkDefaultBrowser() {
        int myUserId = UserHandle.myUserId();
        String packageName = getDefaultBrowserPackageName(myUserId);
        if (packageName != null && getPackageInfo(packageName, 0, myUserId) == null) {
            Slog.w(TAG, "Default browser no longer installed: " + packageName);
            synchronized (this.mPackages) {
                applyFactoryDefaultBrowserLPw(myUserId);
            }
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!((e instanceof SecurityException) || ((e instanceof IllegalArgumentException) ^ 1) == 0)) {
                Slog.wtf(TAG, "Package Manager Crash", e);
            }
            throw e;
        }
    }

    static int[] appendInts(int[] cur, int[] add) {
        if (add == null) {
            return cur;
        }
        if (cur == null) {
            return add;
        }
        for (int appendInt : add) {
            cur = ArrayUtils.appendInt(cur, appendInt);
        }
        return cur;
    }

    private boolean canViewInstantApps(int callingUid, int userId) {
        if (callingUid < 10000 || this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS") == 0) {
            return true;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.VIEW_INSTANT_APPS") == 0) {
            ComponentName homeComponent = getDefaultHomeActivity(userId);
            return homeComponent != null && isCallerSameApp(homeComponent.getPackageName(), callingUid);
        }
    }

    private PackageInfo generatePackageInfo(PackageSetting ps, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        if (ps == null) {
            return null;
        }
        Package p = ps.pkg;
        if (p == null) {
            return null;
        }
        if (filterAppAccessLPr(ps, Binder.getCallingUid(), userId)) {
            return null;
        }
        PermissionsState permissionsState = ps.getPermissionsState();
        int[] gids = (flags & 256) == 0 ? EMPTY_INT_ARRAY : permissionsState.computeGids(userId);
        Set<String> permissions = ArrayUtils.isEmpty(p.requestedPermissions) ? Collections.emptySet() : permissionsState.getPermissions(userId);
        PackageUserState state = ps.readUserState(userId);
        if ((flags & 8192) != 0 && ps.isSystem()) {
            flags |= DumpState.DUMP_CHANGES;
        }
        PackageInfo packageInfo = PackageParser.generatePackageInfo(p, gids, flags, ps.firstInstallTime, ps.lastUpdateTime, permissions, state, userId);
        if (packageInfo == null) {
            return null;
        }
        String resolveExternalPackageNameLPr = resolveExternalPackageNameLPr(p);
        packageInfo.applicationInfo.packageName = resolveExternalPackageNameLPr;
        packageInfo.packageName = resolveExternalPackageNameLPr;
        return packageInfo;
    }

    public void checkPackageStartable(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        boolean userKeyUnlocked = StorageManager.isUserKeyUnlocked(userId);
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null || filterAppAccessLPr(ps, callingUid, userId)) {
                throw new SecurityException("Package " + packageName + " was not found!");
            } else if (!ps.getInstalled(userId)) {
                throw new SecurityException("Package " + packageName + " was not installed for user " + userId + "!");
            } else if (this.mSafeMode && (ps.isSystem() ^ 1) != 0) {
                throw new SecurityException("Package " + packageName + " not a system app!");
            } else if (this.mFrozenPackages.contains(packageName)) {
                throw new SecurityException("Package " + packageName + " is currently frozen!");
            } else if (userKeyUnlocked || (ps.pkg.applicationInfo.isEncryptionAware() ^ 1) == 0) {
            } else {
                throw new SecurityException("Package " + packageName + " is not encryption aware!");
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isPackageAvailable(String packageName, int userId) {
        if (!sUserManager.exists(userId)) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, false, false, "is package available");
        synchronized (this.mPackages) {
            Package p = (Package) this.mPackages.get(packageName);
            if (p != null) {
                PackageSetting ps = p.mExtras;
                if (filterAppAccessLPr(ps, callingUid, userId)) {
                    return false;
                } else if (ps != null) {
                    PackageUserState state = ps.readUserState(userId);
                    if (state != null) {
                        boolean isAvailable = PackageParser.isAvailable(state);
                        return isAvailable;
                    }
                }
            }
        }
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        return getPackageInfoInternal(packageName, -1, flags, Binder.getCallingUid(), userId);
    }

    public PackageInfo getPackageInfoVersioned(VersionedPackage versionedPackage, int flags, int userId) {
        return getPackageInfoInternal(versionedPackage.getPackageName(), versionedPackage.getVersionCode(), flags, Binder.getCallingUid(), userId);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private PackageInfo getPackageInfoInternal(String packageName, int versionCode, int flags, int filterCallingUid, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        flags = updateFlagsForPackage(flags, userId, packageName);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get package info");
        synchronized (this.mPackages) {
            PackageSetting ps;
            packageName = resolveInternalPackageNameLPr(packageName, versionCode);
            boolean matchFactoryOnly = (DumpState.DUMP_COMPILER_STATS & flags) != 0;
            if (matchFactoryOnly) {
                ps = this.mSettings.getDisabledSystemPkgLPr(packageName);
                if (ps != null) {
                    if (filterSharedLibPackageLPr(ps, filterCallingUid, userId, flags)) {
                        return null;
                    } else if (filterAppAccessLPr(ps, filterCallingUid, userId)) {
                        return null;
                    } else {
                        PackageInfo generatePackageInfo = generatePackageInfo(ps, flags, userId);
                        return generatePackageInfo;
                    }
                }
            }
            Package p = (Package) this.mPackages.get(packageName);
            if (matchFactoryOnly && p != null && (isSystemApp(p) ^ 1) != 0) {
                return null;
            } else if (isHwCustHiddenInfoPackage(p)) {
                return null;
            } else if (p != null) {
                ps = (PackageSetting) p.mExtras;
                if (filterSharedLibPackageLPr(ps, filterCallingUid, userId, flags)) {
                    return null;
                }
                if (ps != null) {
                    if (filterAppAccessLPr(ps, filterCallingUid, userId)) {
                        return null;
                    }
                }
                generatePackageInfo = generatePackageInfo((PackageSetting) p.mExtras, flags, userId);
                return generatePackageInfo;
            } else if (matchFactoryOnly || (4202496 & flags) == 0) {
            } else {
                ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (ps == null) {
                    return null;
                } else if (filterSharedLibPackageLPr(ps, filterCallingUid, userId, flags)) {
                    return null;
                } else if (filterAppAccessLPr(ps, filterCallingUid, userId)) {
                    return null;
                } else {
                    generatePackageInfo = generatePackageInfo(ps, flags, userId);
                    return generatePackageInfo;
                }
            }
        }
    }

    private boolean isComponentVisibleToInstantApp(ComponentName component) {
        if (isComponentVisibleToInstantApp(component, 1) || isComponentVisibleToInstantApp(component, 3) || isComponentVisibleToInstantApp(component, 4)) {
            return true;
        }
        return false;
    }

    private boolean isComponentVisibleToInstantApp(ComponentName component, int type) {
        boolean z = true;
        boolean z2 = false;
        Activity activity;
        if (type == 1) {
            activity = (Activity) this.mActivities.mActivities.get(component);
            if (activity == null) {
                z = false;
            } else if ((activity.info.flags & 1048576) == 0) {
                z = false;
            }
            return z;
        } else if (type == 2) {
            activity = (Activity) this.mReceivers.mActivities.get(component);
            if (!(activity == null || (activity.info.flags & 1048576) == 0)) {
                z2 = true;
            }
            return z2;
        } else if (type == 3) {
            Service service = (Service) this.mServices.mServices.get(component);
            if (!(service == null || (service.info.flags & 1048576) == 0)) {
                z2 = true;
            }
            return z2;
        } else if (type == 4) {
            Provider provider = (Provider) this.mProviders.mProviders.get(component);
            if (!(provider == null || (provider.info.flags & 1048576) == 0)) {
                z2 = true;
            }
            return z2;
        } else if (type == 0) {
            return isComponentVisibleToInstantApp(component);
        } else {
            return false;
        }
    }

    private boolean filterAppAccessLPr(PackageSetting ps, int callingUid, ComponentName component, int componentType, int userId) {
        boolean z = true;
        if (Process.isIsolated(callingUid)) {
            callingUid = this.mIsolatedOwners.get(callingUid);
        }
        boolean callerIsInstantApp = getInstantAppPackageName(callingUid) != null;
        if (ps == null) {
            return callerIsInstantApp;
        } else {
            if (isCallerSameApp(ps.name, callingUid)) {
                return false;
            }
            if (callerIsInstantApp) {
                if (component != null) {
                    return isComponentVisibleToInstantApp(component, componentType) ^ 1;
                }
                if (!ps.getInstantApp(userId)) {
                    z = ps.pkg.visibleToInstantApps ^ 1;
                }
                return z;
            } else if (!ps.getInstantApp(userId) || canViewInstantApps(callingUid, userId)) {
                return false;
            } else {
                if (component != null) {
                    return true;
                }
                return this.mInstantAppRegistry.isInstantAccessGranted(userId, UserHandle.getAppId(callingUid), ps.appId) ^ 1;
            }
        }
    }

    private boolean filterAppAccessLPr(PackageSetting ps, int callingUid, int userId) {
        return filterAppAccessLPr(ps, callingUid, null, 0, userId);
    }

    private boolean filterSharedLibPackageLPr(PackageSetting ps, int uid, int userId, int flags) {
        if ((67108864 & flags) != 0) {
            int appId = UserHandle.getAppId(uid);
            if (appId == 1000 || appId == 2000 || appId == 0) {
                return false;
            }
        }
        if (ps == null || ps.pkg == null || (ps.pkg.applicationInfo.isStaticSharedLibrary() ^ 1) != 0) {
            return false;
        }
        SharedLibraryEntry libEntry = getSharedLibraryEntryLPr(ps.pkg.staticSharedLibName, ps.pkg.staticSharedLibVersion);
        if (libEntry == null) {
            return false;
        }
        String[] uidPackageNames = getPackagesForUid(UserHandle.getUid(userId, UserHandle.getAppId(uid)));
        if (uidPackageNames == null) {
            return true;
        }
        for (String uidPackageName : uidPackageNames) {
            if (ps.name.equals(uidPackageName)) {
                return false;
            }
            PackageSetting uidPs = this.mSettings.getPackageLPr(uidPackageName);
            if (uidPs != null) {
                int index = ArrayUtils.indexOf(uidPs.usesStaticLibraries, libEntry.info.getName());
                if (index >= 0 && uidPs.pkg.usesStaticLibrariesVersions[index] == libEntry.info.getVersion()) {
                    return false;
                }
            }
        }
        return true;
    }

    public String[] currentToCanonicalPackageNames(String[] names) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return names;
        }
        String[] out = new String[names.length];
        synchronized (this.mPackages) {
            int callingUserId = UserHandle.getUserId(callingUid);
            boolean canViewInstantApps = canViewInstantApps(callingUid, callingUserId);
            for (int i = names.length - 1; i >= 0; i--) {
                String str;
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(names[i]);
                boolean translateName = false;
                if (!(ps == null || ps.realName == null)) {
                    translateName = (!ps.getInstantApp(callingUserId) || canViewInstantApps) ? true : this.mInstantAppRegistry.isInstantAccessGranted(callingUserId, UserHandle.getAppId(callingUid), ps.appId);
                }
                if (translateName) {
                    str = ps.realName;
                } else {
                    str = names[i];
                }
                out[i] = str;
            }
        }
        return out;
    }

    public String[] canonicalToCurrentPackageNames(String[] names) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return names;
        }
        String[] out = new String[names.length];
        synchronized (this.mPackages) {
            int callingUserId = UserHandle.getUserId(callingUid);
            boolean canViewInstantApps = canViewInstantApps(callingUid, callingUserId);
            for (int i = names.length - 1; i >= 0; i--) {
                String cur = this.mSettings.getRenamedPackageLPr(names[i]);
                boolean translateName = false;
                if (cur != null) {
                    PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(names[i]);
                    if (!(ps != null ? ps.getInstantApp(callingUserId) : false) || canViewInstantApps) {
                        translateName = true;
                    } else {
                        translateName = this.mInstantAppRegistry.isInstantAccessGranted(callingUserId, UserHandle.getAppId(callingUid), ps.appId);
                    }
                }
                if (!translateName) {
                    cur = names[i];
                }
                out[i] = cur;
            }
        }
        return out;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getPackageUid(String packageName, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return -1;
        }
        int callingUid = Binder.getCallingUid();
        flags = updateFlagsForPackage(flags, userId, packageName);
        enforceCrossUserPermission(callingUid, userId, false, false, "getPackageUid");
        synchronized (this.mPackages) {
            Package p = (Package) this.mPackages.get(packageName);
            int uid;
            if (p == null || !p.isMatch(flags)) {
                if ((4202496 & flags) != 0) {
                    PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                    if (!(ps == null || !ps.isMatch(flags) || (filterAppAccessLPr(ps, callingUid, userId) ^ 1) == 0)) {
                        uid = UserHandle.getUid(userId, ps.appId);
                        return uid;
                    }
                }
            } else if (filterAppAccessLPr(p.mExtras, callingUid, userId)) {
                return -1;
            } else {
                uid = UserHandle.getUid(userId, p.applicationInfo.uid);
                return uid;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int[] getPackageGids(String packageName, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        flags = updateFlagsForPackage(flags, userId, packageName);
        enforceCrossUserPermission(callingUid, userId, false, false, "getPackageGids");
        synchronized (this.mPackages) {
            Package p = (Package) this.mPackages.get(packageName);
            PackageSetting ps;
            int[] computeGids;
            if (p != null && p.isMatch(flags)) {
                ps = p.mExtras;
                if (filterAppAccessLPr(ps, callingUid, userId)) {
                    return null;
                }
                computeGids = ps.getPermissionsState().computeGids(userId);
                return computeGids;
            } else if ((4202496 & flags) != 0) {
                ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (!(ps == null || !ps.isMatch(flags) || (filterAppAccessLPr(ps, callingUid, userId) ^ 1) == 0)) {
                    computeGids = ps.getPermissionsState().computeGids(userId);
                    return computeGids;
                }
            }
        }
    }

    static PermissionInfo generatePermissionInfo(BasePermission bp, int flags) {
        if (bp.perm != null) {
            return PackageParser.generatePermissionInfo(bp.perm, flags);
        }
        PermissionInfo pi = new PermissionInfo();
        pi.name = bp.name;
        pi.packageName = bp.sourcePackage;
        pi.nonLocalizedLabel = bp.name;
        pi.protectionLevel = bp.protectionLevel;
        return pi;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public PermissionInfo getPermissionInfo(String name, String packageName, int flags) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            BasePermission p = (BasePermission) this.mSettings.mPermissions.get(name);
            if (p == null) {
                return null;
            }
            PermissionInfo permissionInfo = generatePermissionInfo(p, flags);
            if (permissionInfo != null) {
                int protectionLevel = adjustPermissionProtectionFlagsLPr(permissionInfo.protectionLevel, packageName, callingUid);
                if (permissionInfo.protectionLevel != protectionLevel) {
                    if (p.perm != null && p.perm.info == permissionInfo) {
                        permissionInfo = new PermissionInfo(permissionInfo);
                    }
                    permissionInfo.protectionLevel = protectionLevel;
                }
            }
        }
    }

    private int adjustPermissionProtectionFlagsLPr(int protectionLevel, String packageName, int uid) {
        int protectionLevelMasked = protectionLevel & 3;
        if (protectionLevelMasked == 2) {
            return protectionLevel;
        }
        int appId = UserHandle.getAppId(uid);
        if (appId == 1000 || appId == 0 || appId == 2000) {
            return protectionLevel;
        }
        packageName = resolveInternalPackageNameLPr(packageName, -1);
        PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
        if (ps == null || ps.appId != appId) {
            return protectionLevel;
        }
        Package pkg = (Package) this.mPackages.get(packageName);
        if (pkg != null && pkg.applicationInfo.targetSdkVersion < 26) {
            return protectionLevelMasked;
        }
        return protectionLevel;
    }

    public ParceledListSlice<PermissionInfo> queryPermissionsByGroup(String group, int flags) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            if (group != null) {
                if ((this.mPermissionGroups.containsKey(group) ^ 1) != 0) {
                    return null;
                }
            }
            ArrayList<PermissionInfo> out = new ArrayList(10);
            for (BasePermission p : this.mSettings.mPermissions.values()) {
                if (group == null) {
                    if (p.perm == null || p.perm.info.group == null) {
                        out.add(generatePermissionInfo(p, flags));
                    }
                } else if (p.perm != null && group.equals(p.perm.info.group)) {
                    out.add(PackageParser.generatePermissionInfo(p.perm, flags));
                }
            }
            ParceledListSlice<PermissionInfo> parceledListSlice = new ParceledListSlice(out);
            return parceledListSlice;
        }
    }

    public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        PermissionGroupInfo generatePermissionGroupInfo;
        synchronized (this.mPackages) {
            generatePermissionGroupInfo = PackageParser.generatePermissionGroupInfo((PermissionGroup) this.mPermissionGroups.get(name), flags);
        }
        return generatePermissionGroupInfo;
    }

    public ParceledListSlice<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return ParceledListSlice.emptyList();
        }
        ParceledListSlice<PermissionGroupInfo> parceledListSlice;
        synchronized (this.mPackages) {
            ArrayList<PermissionGroupInfo> out = new ArrayList(this.mPermissionGroups.size());
            for (PermissionGroup pg : this.mPermissionGroups.values()) {
                out.add(PackageParser.generatePermissionGroupInfo(pg, flags));
            }
            parceledListSlice = new ParceledListSlice(out);
        }
        return parceledListSlice;
    }

    private ApplicationInfo generateApplicationInfoFromSettingsLPw(String packageName, int flags, int filterCallingUid, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
        if (ps == null || filterSharedLibPackageLPr(ps, filterCallingUid, userId, flags) || filterAppAccessLPr(ps, filterCallingUid, userId)) {
            return null;
        }
        if (ps.pkg == null) {
            PackageInfo pInfo = generatePackageInfo(ps, flags, userId);
            if (pInfo != null) {
                return pInfo.applicationInfo;
            }
            return null;
        }
        ApplicationInfo ai = PackageParser.generateApplicationInfo(ps.pkg, flags, ps.readUserState(userId), userId);
        if (ai != null) {
            ai.packageName = resolveExternalPackageNameLPr(ps.pkg);
        }
        return ai;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        return getApplicationInfoInternal(packageName, flags, Binder.getCallingUid(), userId);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ApplicationInfo getApplicationInfoInternal(String packageName, int flags, int filterCallingUid, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        flags = updateFlagsForApplication(flags, userId, packageName);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get application info");
        synchronized (this.mPackages) {
            packageName = resolveInternalPackageNameLPr(packageName, -1);
            Package p = (Package) this.mPackages.get(packageName);
            if (isHwCustHiddenInfoPackage(p)) {
                return null;
            } else if (p != null) {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (ps == null) {
                    return null;
                } else if (filterSharedLibPackageLPr(ps, filterCallingUid, userId, flags)) {
                    return null;
                } else if (filterAppAccessLPr(ps, filterCallingUid, userId)) {
                    return null;
                } else {
                    ApplicationInfo ai = PackageParser.generateApplicationInfo(p, flags, ps.readUserState(userId), userId);
                    if (ai != null) {
                        ai.packageName = resolveExternalPackageNameLPr(p);
                    }
                }
            } else if (PLATFORM_PACKAGE_NAME.equals(packageName) || "system".equals(packageName)) {
                r0 = this.mAndroidApplication;
                return r0;
            } else if ((4202496 & flags) != 0) {
                r0 = generateApplicationInfoFromSettingsLPw(packageName, flags, filterCallingUid, userId);
                return r0;
            } else {
                return null;
            }
        }
    }

    private String normalizePackageNameLPr(String packageName) {
        String normalizedPackageName = this.mSettings.getRenamedPackageLPr(packageName);
        return normalizedPackageName != null ? normalizedPackageName : packageName;
    }

    public void deletePreloadsFileCache() {
        if (UserHandle.isSameApp(Binder.getCallingUid(), 1000)) {
            File dir = Environment.getDataPreloadsFileCacheDirectory();
            Slog.i(TAG, "Deleting preloaded file cache " + dir);
            FileUtils.deleteContents(dir);
            return;
        }
        throw new SecurityException("Only system or settings may call deletePreloadsFileCache");
    }

    public void freeStorageAndNotify(String volumeUuid, long freeStorageSize, int storageFlags, IPackageDataObserver observer) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", null);
        this.mHandler.post(new com.android.server.pm.-$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E.AnonymousClass2((byte) 1, storageFlags, freeStorageSize, this, volumeUuid, observer));
    }

    /* synthetic */ void lambda$-com_android_server_pm_PackageManagerService_236164(String volumeUuid, long freeStorageSize, int storageFlags, IPackageDataObserver observer) {
        boolean success = false;
        try {
            freeStorage(volumeUuid, freeStorageSize, storageFlags);
            success = true;
        } catch (IOException e) {
            Slog.w(TAG, e);
        }
        if (observer != null) {
            try {
                observer.onRemoveCompleted(null, success);
            } catch (RemoteException e2) {
                Slog.w(TAG, e2);
            }
        }
    }

    public void freeStorage(String volumeUuid, long freeStorageSize, int storageFlags, IntentSender pi) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_CACHE", TAG);
        this.mHandler.post(new com.android.server.pm.-$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E.AnonymousClass2((byte) 0, storageFlags, freeStorageSize, this, volumeUuid, pi));
    }

    /* synthetic */ void lambda$-com_android_server_pm_PackageManagerService_236963(String volumeUuid, long freeStorageSize, int storageFlags, IntentSender pi) {
        boolean success = false;
        try {
            freeStorage(volumeUuid, freeStorageSize, storageFlags);
            success = true;
        } catch (IOException e) {
            Slog.w(TAG, e);
        }
        if (pi != null) {
            int i;
            if (success) {
                i = 1;
            } else {
                i = 0;
            }
            try {
                pi.sendIntent(null, i, null, null, null);
            } catch (SendIntentException e2) {
                Slog.w(TAG, e2);
            }
        }
    }

    public void freeStorage(String volumeUuid, long bytes, int storageFlags) throws IOException {
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        File file = storage.findPathForUuid(volumeUuid);
        if (file.getUsableSpace() < bytes) {
            if (ENABLE_FREE_CACHE_V2) {
                boolean internalVolume = Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid);
                boolean aggressive = (storageFlags & 1) != 0;
                long reservedBytes = storage.getStorageCacheBytes(file, storageFlags);
                if (internalVolume && (aggressive || SystemProperties.getBoolean("persist.sys.preloads.file_cache_expired", false))) {
                    deletePreloadsFileCache();
                    if (file.getUsableSpace() >= bytes) {
                        return;
                    }
                }
                if (internalVolume && aggressive) {
                    FileUtils.deleteContents(this.mCacheDir);
                    if (file.getUsableSpace() >= bytes) {
                        return;
                    }
                }
                try {
                    this.mInstaller.freeCache(volumeUuid, bytes, reservedBytes, 8192);
                } catch (InstallerException e) {
                }
                if (file.getUsableSpace() < bytes) {
                    if (internalVolume) {
                        if (pruneUnusedStaticSharedLibraries(bytes, Global.getLong(this.mContext.getContentResolver(), "unused_static_shared_lib_min_cache_period", DEFAULT_UNUSED_STATIC_SHARED_LIB_MIN_CACHE_PERIOD))) {
                            return;
                        }
                    }
                    if (internalVolume) {
                        if (this.mInstantAppRegistry.pruneInstalledInstantApps(bytes, Global.getLong(this.mContext.getContentResolver(), "installed_instant_app_min_cache_period", UnixCalendar.WEEK_IN_MILLIS))) {
                            return;
                        }
                    }
                    try {
                        this.mInstaller.freeCache(volumeUuid, bytes, reservedBytes, 24576);
                    } catch (InstallerException e2) {
                    }
                    if (file.getUsableSpace() < bytes) {
                        if (internalVolume) {
                            if (this.mInstantAppRegistry.pruneUninstalledInstantApps(bytes, Global.getLong(this.mContext.getContentResolver(), "uninstalled_instant_app_min_cache_period", UnixCalendar.WEEK_IN_MILLIS))) {
                                return;
                            }
                        }
                    }
                    return;
                }
                return;
            }
            try {
                this.mInstaller.freeCache(volumeUuid, bytes, 0, 0);
            } catch (InstallerException e3) {
            }
            if (file.getUsableSpace() >= bytes) {
                return;
            }
            throw new IOException("Failed to free " + bytes + " on storage device at " + file);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean pruneUnusedStaticSharedLibraries(long neededSpace, long maxCachePeriod) throws IOException {
        Throwable th;
        File volume = ((StorageManager) this.mContext.getSystemService(StorageManager.class)).findPathForUuid(StorageManager.UUID_PRIVATE_INTERNAL);
        List<VersionedPackage> packagesToDelete = null;
        long now = System.currentTimeMillis();
        synchronized (this.mPackages) {
            try {
                int[] allUsers = sUserManager.getUserIds();
                int libCount = this.mSharedLibraries.size();
                for (int i = 0; i < libCount; i++) {
                    SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.valueAt(i);
                    if (versionedLib != null) {
                        int versionCount = versionedLib.size();
                        int j = 0;
                        List<VersionedPackage> packagesToDelete2 = packagesToDelete;
                        while (j < versionCount) {
                            try {
                                SharedLibraryInfo libInfo = ((SharedLibraryEntry) versionedLib.valueAt(j)).info;
                                if (!libInfo.isStatic()) {
                                    packagesToDelete = packagesToDelete2;
                                    break;
                                }
                                VersionedPackage declaringPackage = libInfo.getDeclaringPackage();
                                String internalPackageName = resolveInternalPackageNameLPr(declaringPackage.getPackageName(), declaringPackage.getVersionCode());
                                PackageSetting ps = this.mSettings.getPackageLPr(internalPackageName);
                                if (ps == null || now - ps.lastUpdateTime < maxCachePeriod) {
                                    packagesToDelete = packagesToDelete2;
                                } else {
                                    if (packagesToDelete2 == null) {
                                        packagesToDelete = new ArrayList();
                                    } else {
                                        packagesToDelete = packagesToDelete2;
                                    }
                                    packagesToDelete.add(new VersionedPackage(internalPackageName, declaringPackage.getVersionCode()));
                                }
                                j++;
                                packagesToDelete2 = packagesToDelete;
                            } catch (Throwable th2) {
                                th = th2;
                                packagesToDelete = packagesToDelete2;
                            }
                        }
                        packagesToDelete = packagesToDelete2;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    private int updateFlags(int flags, int userId) {
        if ((flags & 786432) != 0) {
            return flags;
        }
        if (getUserManagerInternal().isUserUnlockingOrUnlocked(userId)) {
            return flags | 786432;
        }
        return flags | 524288;
    }

    UserManagerInternal getUserManagerInternal() {
        if (this.mUserManagerInternal == null) {
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUserManagerInternal;
    }

    private LocalService getDeviceIdleController() {
        if (this.mDeviceIdleController == null) {
            this.mDeviceIdleController = (LocalService) LocalServices.getService(LocalService.class);
        }
        return this.mDeviceIdleController;
    }

    private int updateFlagsForPackage(int flags, int userId, Object cookie) {
        boolean isCallerSystemUser = UserHandle.getCallingUserId() == 0;
        if ((flags & 15) != 0 && (269221888 & flags) == 0) {
        }
        if ((269492224 & flags) == 0) {
        }
        if ((flags & DumpState.DUMP_CHANGES) != 0) {
            enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "MATCH_ANY_USER flag requires INTERACT_ACROSS_USERS permission at " + Debug.getCallers(5));
        } else if ((flags & 8192) != 0 && isCallerSystemUser && sUserManager.hasManagedProfile(0)) {
            flags |= DumpState.DUMP_CHANGES;
        }
        return updateFlags(flags, userId);
    }

    private int updateFlagsForApplication(int flags, int userId, Object cookie) {
        return updateFlagsForPackage(flags, userId, cookie);
    }

    private int updateFlagsForComponent(int flags, int userId, Object cookie) {
        if ((cookie instanceof Intent) && (((Intent) cookie).getFlags() & 256) != 0) {
            flags |= 268435456;
        }
        if ((269221888 & flags) == 0) {
        }
        return updateFlags(flags, userId);
    }

    private Intent updateIntentForResolve(Intent intent) {
        if (intent.getSelector() != null) {
            return intent.getSelector();
        }
        return intent;
    }

    int updateFlagsForResolve(int flags, int userId, Intent intent, int callingUid) {
        return updateFlagsForResolve(flags, userId, intent, callingUid, false, false);
    }

    int updateFlagsForResolve(int flags, int userId, Intent intent, int callingUid, boolean wantInstantApps) {
        return updateFlagsForResolve(flags, userId, intent, callingUid, wantInstantApps, false);
    }

    int updateFlagsForResolve(int flags, int userId, Intent intent, int callingUid, boolean wantInstantApps, boolean onlyExposedExplicitly) {
        if (this.mSafeMode) {
            flags |= 1048576;
        }
        if (getInstantAppPackageName(callingUid) != null) {
            if (onlyExposedExplicitly) {
                flags |= 33554432;
            }
            flags = (flags | 16777216) | DumpState.DUMP_VOLUMES;
        } else {
            boolean canViewInstantApps = (wantInstantApps && "android.intent.action.VIEW".equals(intent.getAction()) && hasWebURI(intent)) ? true : (flags & DumpState.DUMP_VOLUMES) != 0 ? canViewInstantApps(callingUid, userId) : false;
            flags &= -50331649;
            if (!canViewInstantApps) {
                flags &= -8388609;
            }
        }
        return updateFlagsForComponent(flags, userId, intent);
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        return getActivityInfoInternal(component, flags, Binder.getCallingUid(), userId);
    }

    private ActivityInfo getActivityInfoInternal(ComponentName component, int flags, int filterCallingUid, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        flags = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "get activity info");
        synchronized (this.mPackages) {
            Activity a = (Activity) this.mActivities.mActivities.get(component);
            ActivityInfo generateActivityInfo;
            if (a != null && this.mSettings.isEnabledAndMatchLPr(a.info, flags, userId)) {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(component.getPackageName());
                if (ps == null) {
                    return null;
                } else if (filterAppAccessLPr(ps, filterCallingUid, component, 1, userId)) {
                    return null;
                } else {
                    generateActivityInfo = PackageParser.generateActivityInfo(a, flags, ps.readUserState(userId), userId);
                    return generateActivityInfo;
                }
            } else if (this.mResolveComponentName.equals(component)) {
                generateActivityInfo = PackageParser.generateActivityInfo(this.mResolveActivity, flags, new PackageUserState(), userId);
                return generateActivityInfo;
            } else {
                return null;
            }
        }
    }

    public boolean activitySupportsIntent(ComponentName component, Intent intent, String resolvedType) {
        synchronized (this.mPackages) {
            if (component.equals(this.mResolveComponentName)) {
                return true;
            }
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            Activity a = (Activity) this.mActivities.mActivities.get(component);
            if (a == null) {
                return false;
            }
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(component.getPackageName());
            if (ps == null) {
                return false;
            } else if (filterAppAccessLPr(ps, callingUid, component, 1, callingUserId)) {
                return false;
            } else {
                for (int i = 0; i < a.intents.size(); i++) {
                    if (((ActivityIntentInfo) a.intents.get(i)).match(intent.getAction(), resolvedType, intent.getScheme(), intent.getData(), intent.getCategories(), TAG) >= 0) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ActivityInfo getReceiverInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        flags = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(callingUid, userId, false, false, "get receiver info");
        synchronized (this.mPackages) {
            Activity a = (Activity) this.mReceivers.mActivities.get(component);
            if (a == null || !this.mSettings.isEnabledAndMatchLPr(a.info, flags, userId)) {
            } else {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(component.getPackageName());
                if (ps == null) {
                    return null;
                } else if (filterAppAccessLPr(ps, callingUid, component, 2, userId)) {
                    return null;
                } else {
                    ActivityInfo generateActivityInfo = PackageParser.generateActivityInfo(a, flags, ps.readUserState(userId), userId);
                    return generateActivityInfo;
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ParceledListSlice<SharedLibraryInfo> getSharedLibraries(String packageName, int flags, int userId) {
        Throwable th;
        if (!sUserManager.exists(userId)) {
            return null;
        }
        Preconditions.checkArgumentNonnegative(userId, "userId must be >= 0");
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        flags = updateFlagsForPackage(flags, userId, null);
        boolean canSeeStaticLibraries = (this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES") == 0 || this.mContext.checkCallingOrSelfPermission("android.permission.DELETE_PACKAGES") == 0 || canRequestPackageInstallsInternal(packageName, 67108864, userId, false)) ? true : this.mContext.checkCallingOrSelfPermission("android.permission.REQUEST_DELETE_PACKAGES") == 0;
        synchronized (this.mPackages) {
            List<SharedLibraryInfo> result = null;
            int libCount = this.mSharedLibraries.size();
            for (int i = 0; i < libCount; i++) {
                SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.valueAt(i);
                if (versionedLib != null) {
                    int versionCount = versionedLib.size();
                    int j = 0;
                    List<SharedLibraryInfo> result2 = result;
                    while (j < versionCount) {
                        SharedLibraryInfo libInfo = ((SharedLibraryEntry) versionedLib.valueAt(j)).info;
                        if (!canSeeStaticLibraries && libInfo.isStatic()) {
                            result = result2;
                            break;
                        }
                        long identity = Binder.clearCallingIdentity();
                        try {
                            if (getPackageInfoVersioned(libInfo.getDeclaringPackage(), 67108864 | flags, userId) == null) {
                                Binder.restoreCallingIdentity(identity);
                                result = result2;
                            } else {
                                Binder.restoreCallingIdentity(identity);
                                SharedLibraryInfo resLibInfo = new SharedLibraryInfo(libInfo.getName(), libInfo.getVersion(), libInfo.getType(), libInfo.getDeclaringPackage(), getPackagesUsingSharedLibraryLPr(libInfo, flags, userId));
                                if (result2 == null) {
                                    result = new ArrayList();
                                } else {
                                    result = result2;
                                }
                                try {
                                    result.add(resLibInfo);
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            j++;
                            result2 = result;
                        } catch (Throwable th3) {
                            th = th3;
                            result = result2;
                        }
                    }
                    result = result2;
                }
            }
            ParceledListSlice<SharedLibraryInfo> parceledListSlice = result != null ? new ParceledListSlice(result) : null;
        }
        throw th;
    }

    private List<VersionedPackage> getPackagesUsingSharedLibraryLPr(SharedLibraryInfo libInfo, int flags, int userId) {
        List<VersionedPackage> versionedPackages = null;
        int packageCount = this.mSettings.mPackages.size();
        for (int i = 0; i < packageCount; i++) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.valueAt(i);
            if (ps != null && ((PackageUserState) ps.getUserState().get(userId)).isAvailable(flags)) {
                String libName = libInfo.getName();
                if (libInfo.isStatic()) {
                    int libIdx = ArrayUtils.indexOf(ps.usesStaticLibraries, libName);
                    if (libIdx >= 0 && ps.usesStaticLibrariesVersions[libIdx] == libInfo.getVersion()) {
                        if (versionedPackages == null) {
                            versionedPackages = new ArrayList();
                        }
                        String dependentPackageName = ps.name;
                        if (ps.pkg != null && ps.pkg.applicationInfo.isStaticSharedLibrary()) {
                            dependentPackageName = ps.pkg.manifestPackageName;
                        }
                        versionedPackages.add(new VersionedPackage(dependentPackageName, ps.versionCode));
                    }
                } else if (ps.pkg != null && (ArrayUtils.contains(ps.pkg.usesLibraries, libName) || ArrayUtils.contains(ps.pkg.usesOptionalLibraries, libName))) {
                    if (versionedPackages == null) {
                        versionedPackages = new ArrayList();
                    }
                    versionedPackages.add(new VersionedPackage(ps.name, ps.versionCode));
                }
            }
        }
        return versionedPackages;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        flags = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(callingUid, userId, false, false, "get service info");
        synchronized (this.mPackages) {
            Service s = (Service) this.mServices.mServices.get(component);
            if (s == null || !this.mSettings.isEnabledAndMatchLPr(s.info, flags, userId)) {
            } else {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(component.getPackageName());
                if (ps == null) {
                    return null;
                } else if (filterAppAccessLPr(ps, callingUid, component, 3, userId)) {
                    return null;
                } else {
                    ServiceInfo generateServiceInfo = PackageParser.generateServiceInfo(s, flags, ps.readUserState(userId), userId);
                    return generateServiceInfo;
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        flags = updateFlagsForComponent(flags, userId, component);
        enforceCrossUserPermission(callingUid, userId, false, false, "get provider info");
        synchronized (this.mPackages) {
            Provider p = (Provider) this.mProviders.mProviders.get(component);
            if (p == null || !this.mSettings.isEnabledAndMatchLPr(p.info, flags, userId)) {
            } else {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(component.getPackageName());
                if (ps == null) {
                    return null;
                } else if (filterAppAccessLPr(ps, callingUid, component, 4, userId)) {
                    return null;
                } else {
                    ProviderInfo generateProviderInfo = PackageParser.generateProviderInfo(p, flags, ps.readUserState(userId), userId);
                    return generateProviderInfo;
                }
            }
        }
    }

    public String[] getSystemSharedLibraryNames() {
        Throwable th;
        synchronized (this.mPackages) {
            int libCount = this.mSharedLibraries.size();
            int i = 0;
            Set<String> libs = null;
            while (i < libCount) {
                Set<String> libs2;
                SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.valueAt(i);
                if (versionedLib == null) {
                    libs2 = libs;
                } else {
                    int versionCount = versionedLib.size();
                    int j = 0;
                    while (j < versionCount) {
                        SharedLibraryEntry libEntry = (SharedLibraryEntry) versionedLib.valueAt(j);
                        if (libEntry.info.isStatic()) {
                            try {
                                PackageSetting ps = this.mSettings.getPackageLPr(libEntry.apk);
                                if (ps == null || (filterSharedLibPackageLPr(ps, Binder.getCallingUid(), UserHandle.getUserId(Binder.getCallingUid()), 67108864) ^ 1) == 0) {
                                    j++;
                                } else {
                                    if (libs == null) {
                                        libs2 = new ArraySet();
                                    } else {
                                        libs2 = libs;
                                    }
                                    libs2.add(libEntry.info.getName());
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                libs2 = libs;
                            }
                        } else {
                            if (libs == null) {
                                libs2 = new ArraySet();
                            } else {
                                libs2 = libs;
                            }
                            try {
                                libs2.add(libEntry.info.getName());
                            } catch (Throwable th3) {
                                th = th3;
                            }
                        }
                    }
                    libs2 = libs;
                }
                i++;
                libs = libs2;
            }
            if (libs != null) {
                String[] libsArray = new String[libs.size()];
                libs.toArray(libsArray);
                return libsArray;
            }
            return null;
        }
        throw th;
    }

    public String getServicesSystemSharedLibraryPackageName() {
        String str;
        synchronized (this.mPackages) {
            str = this.mServicesSystemSharedLibraryPackageName;
        }
        return str;
    }

    public String getSharedSystemSharedLibraryPackageName() {
        String str;
        synchronized (this.mPackages) {
            str = this.mSharedSystemSharedLibraryPackageName;
        }
        return str;
    }

    private void updateSequenceNumberLP(PackageSetting pkgSetting, int[] userList) {
        for (int i = userList.length - 1; i >= 0; i--) {
            int userId = userList[i];
            if (!pkgSetting.getInstantApp(userId)) {
                SparseArray<String> changedPackages = (SparseArray) this.mChangedPackages.get(userId);
                if (changedPackages == null) {
                    changedPackages = new SparseArray();
                    this.mChangedPackages.put(userId, changedPackages);
                }
                Map<String, Integer> sequenceNumbers = (Map) this.mChangedPackagesSequenceNumbers.get(userId);
                if (sequenceNumbers == null) {
                    sequenceNumbers = new HashMap();
                    this.mChangedPackagesSequenceNumbers.put(userId, sequenceNumbers);
                }
                Integer sequenceNumber = (Integer) sequenceNumbers.get(pkgSetting.name);
                if (sequenceNumber != null) {
                    changedPackages.remove(sequenceNumber.intValue());
                }
                changedPackages.put(this.mChangedPackagesSequenceNumber, pkgSetting.name);
                sequenceNumbers.put(pkgSetting.name, Integer.valueOf(this.mChangedPackagesSequenceNumber));
            }
        }
        this.mChangedPackagesSequenceNumber++;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ChangedPackages getChangedPackages(int sequenceNumber, int userId) {
        ChangedPackages changedPackages = null;
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            if (sequenceNumber >= this.mChangedPackagesSequenceNumber) {
                return null;
            }
            SparseArray<String> changedPackages2 = (SparseArray) this.mChangedPackages.get(userId);
            if (changedPackages2 == null) {
                return null;
            }
            List<String> packageNames = new ArrayList(this.mChangedPackagesSequenceNumber - sequenceNumber);
            for (int i = sequenceNumber; i < this.mChangedPackagesSequenceNumber; i++) {
                String packageName = (String) changedPackages2.get(i);
                if (packageName != null) {
                    packageNames.add(packageName);
                }
            }
            if (!packageNames.isEmpty()) {
                changedPackages = new ChangedPackages(this.mChangedPackagesSequenceNumber, packageNames);
            }
        }
    }

    public ParceledListSlice<FeatureInfo> getSystemAvailableFeatures() {
        ArrayList<FeatureInfo> res;
        synchronized (this.mAvailableFeatures) {
            res = new ArrayList(this.mAvailableFeatures.size() + 1);
            res.addAll(this.mAvailableFeatures.values());
        }
        FeatureInfo fi = new FeatureInfo();
        fi.reqGlEsVersion = SystemProperties.getInt("ro.opengles.version", 0);
        res.add(fi);
        return new ParceledListSlice(res);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean hasSystemFeature(String name, int version) {
        boolean z = false;
        synchronized (this.mAvailableFeatures) {
            FeatureInfo feat = (FeatureInfo) this.mAvailableFeatures.get(name);
            if (feat == null) {
                return false;
            } else if (feat.version >= version) {
                z = true;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int checkPermission(String permName, String pkgName, int userId) {
        if (!sUserManager.exists(userId)) {
            return -1;
        }
        int callingUid = Binder.getCallingUid();
        synchronized (this.mPackages) {
            Package p = (Package) this.mPackages.get(pkgName);
            if (!(p == null || p.mExtras == null)) {
                PackageSetting ps = p.mExtras;
                if (filterAppAccessLPr(ps, callingUid, userId)) {
                    return -1;
                }
                boolean instantApp = ps.getInstantApp(userId);
                PermissionsState permissionsState = ps.getPermissionsState();
                if (permissionsState.hasPermission(permName, userId)) {
                    if (instantApp) {
                        BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(permName);
                        if (bp != null && bp.isInstant()) {
                            return 0;
                        }
                    }
                    return 0;
                }
                if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", userId)) {
                    return 0;
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int checkUidPermission(String permName, int uid) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        boolean isCallerInstantApp = getInstantAppPackageName(callingUid) != null;
        boolean isUidInstantApp = getInstantAppPackageName(uid) != null;
        int userId = UserHandle.getUserId(uid);
        if (!sUserManager.exists(userId)) {
            return -1;
        }
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            if (obj != null) {
                if (obj instanceof SharedUserSetting) {
                    if (isCallerInstantApp) {
                        return -1;
                    }
                } else if ((obj instanceof PackageSetting) && filterAppAccessLPr((PackageSetting) obj, callingUid, callingUserId)) {
                    return -1;
                }
                PermissionsState permissionsState = ((SettingBase) obj).getPermissionsState();
                if (permissionsState.hasPermission(permName, userId)) {
                    if (isUidInstantApp) {
                        BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(permName);
                        if (bp != null && bp.isInstant()) {
                            return 0;
                        }
                    }
                    return 0;
                }
                if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && permissionsState.hasPermission("android.permission.ACCESS_FINE_LOCATION", userId)) {
                    return 0;
                }
            }
            ArraySet<String> perms = (ArraySet) this.mSystemPermissions.get(uid);
            if (perms != null) {
                if (perms.contains(permName)) {
                    return 0;
                } else if ("android.permission.ACCESS_COARSE_LOCATION".equals(permName) && perms.contains("android.permission.ACCESS_FINE_LOCATION")) {
                    return 0;
                }
            }
        }
    }

    public boolean isPermissionRevokedByPolicy(String permission, String packageName, int userId) {
        boolean z = false;
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "isPermissionRevokedByPolicy for user " + userId);
        }
        if (checkPermission(permission, packageName, userId) == 0) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            if (!isCallerSameApp(packageName, callingUid)) {
                return false;
            }
        } else if (isInstantApp(packageName, userId)) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            if ((getPermissionFlags(permission, packageName, userId) & 4) != 0) {
                z = true;
            }
            Binder.restoreCallingIdentity(identity);
            return z;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public String getPermissionControllerPackageName() {
        String str;
        synchronized (this.mPackages) {
            str = this.mRequiredInstallerPackage;
        }
        return str;
    }

    void enforceCrossUserPermission(int callingUid, int userId, boolean requireFullPermission, boolean checkShell, String message) {
        if (userId < 0) {
            throw new IllegalArgumentException("Invalid userId " + userId);
        }
        if (checkShell) {
            enforceShellRestriction("no_debugging_features", callingUid, userId);
        }
        if (!(userId == UserHandle.getUserId(callingUid) || callingUid == 1000 || callingUid == 0)) {
            if (requireFullPermission) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
            } else {
                try {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
                } catch (SecurityException e) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", message);
                }
            }
        }
    }

    void enforceShellRestriction(String restriction, int callingUid, int userHandle) {
        if (callingUid != 2000) {
            return;
        }
        if (userHandle >= 0 && sUserManager.hasUserRestriction(restriction, userHandle)) {
            throw new SecurityException("Shell does not have permission to access user " + userHandle);
        } else if (userHandle < 0) {
            Slog.e(TAG, "Unable to check shell permission for user " + userHandle + "\n\t" + Debug.getCallers(3));
        }
    }

    private BasePermission findPermissionTreeLP(String permName) {
        for (BasePermission bp : this.mSettings.mPermissionTrees.values()) {
            if (permName.startsWith(bp.name) && permName.length() > bp.name.length() && permName.charAt(bp.name.length()) == '.') {
                return bp;
            }
        }
        return null;
    }

    private BasePermission checkPermissionTreeLP(String permName) {
        if (permName != null) {
            BasePermission bp = findPermissionTreeLP(permName);
            if (bp != null) {
                if (bp.uid == UserHandle.getAppId(Binder.getCallingUid())) {
                    return bp;
                }
                throw new SecurityException("Calling uid " + Binder.getCallingUid() + " is not allowed to add to permission tree " + bp.name + " owned by uid " + bp.uid);
            }
        }
        throw new SecurityException("No permission tree found for " + permName);
    }

    static boolean compareStrings(CharSequence s1, CharSequence s2) {
        boolean z = false;
        if (s1 == null) {
            if (s2 == null) {
                z = true;
            }
            return z;
        } else if (s2 != null && s1.getClass() == s2.getClass()) {
            return s1.equals(s2);
        } else {
            return false;
        }
    }

    static boolean comparePermissionInfos(PermissionInfo pi1, PermissionInfo pi2) {
        if (pi1.icon == pi2.icon && pi1.logo == pi2.logo && pi1.protectionLevel == pi2.protectionLevel && compareStrings(pi1.name, pi2.name) && compareStrings(pi1.nonLocalizedLabel, pi2.nonLocalizedLabel) && compareStrings(pi1.packageName, pi2.packageName)) {
            return true;
        }
        return false;
    }

    int permissionInfoFootprint(PermissionInfo info) {
        int size = info.name.length();
        if (info.nonLocalizedLabel != null) {
            size += info.nonLocalizedLabel.length();
        }
        if (info.nonLocalizedDescription != null) {
            return size + info.nonLocalizedDescription.length();
        }
        return size;
    }

    int calculateCurrentPermissionFootprintLocked(BasePermission tree) {
        int size = 0;
        for (BasePermission perm : this.mSettings.mPermissions.values()) {
            if (perm.uid == tree.uid) {
                size += perm.name.length() + permissionInfoFootprint(perm.perm.info);
            }
        }
        return size;
    }

    void enforcePermissionCapLocked(PermissionInfo info, BasePermission tree) {
        if (tree.uid != 1000) {
            if (permissionInfoFootprint(info) + calculateCurrentPermissionFootprintLocked(tree) > 32768) {
                throw new SecurityException("Permission tree size cap exceeded");
            }
        }
    }

    boolean addPermissionLocked(PermissionInfo info, boolean async) {
        if (info == null) {
            throw new SecurityException("Failed to addPermissionLocked, permission info to add is null!");
        } else if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            throw new SecurityException("Instant apps can't add permissions");
        } else if (info.labelRes == 0 && info.nonLocalizedLabel == null) {
            throw new SecurityException("Label must be specified in permission");
        } else {
            BasePermission tree = checkPermissionTreeLP(info.name);
            BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(info.name);
            boolean added = bp == null;
            boolean changed = true;
            int fixedLevel = PermissionInfo.fixProtectionLevel(info.protectionLevel);
            if (added) {
                enforcePermissionCapLocked(info, tree);
                bp = new BasePermission(info.name, tree.sourcePackage, 2);
            } else if (bp.type != 2) {
                throw new SecurityException("Not allowed to modify non-dynamic permission " + info.name);
            } else if (bp.protectionLevel == fixedLevel && bp.perm.owner.equals(tree.perm.owner) && bp.uid == tree.uid && comparePermissionInfos(bp.perm.info, info)) {
                changed = false;
            }
            bp.protectionLevel = fixedLevel;
            PermissionInfo info2 = new PermissionInfo(info);
            info2.protectionLevel = fixedLevel;
            bp.perm = new Permission(tree.perm.owner, info2);
            bp.perm.info.packageName = tree.perm.info.packageName;
            bp.uid = tree.uid;
            if (added) {
                this.mSettings.mPermissions.put(info2.name, bp);
            }
            if (changed) {
                if (async) {
                    scheduleWriteSettingsLocked();
                } else {
                    this.mSettings.writeLPr();
                }
            }
            return added;
        }
    }

    public boolean addPermission(PermissionInfo info) {
        boolean addPermissionLocked;
        synchronized (this.mPackages) {
            addPermissionLocked = addPermissionLocked(info, false);
        }
        return addPermissionLocked;
    }

    public boolean addPermissionAsync(PermissionInfo info) {
        boolean addPermissionLocked;
        synchronized (this.mPackages) {
            addPermissionLocked = addPermissionLocked(info, true);
        }
        return addPermissionLocked;
    }

    public void removePermission(String name) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        synchronized (this.mPackages) {
            checkPermissionTreeLP(name);
            BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(name);
            if (bp != null) {
                if (bp.type != 2) {
                    throw new SecurityException("Not allowed to modify non-dynamic permission " + name);
                }
                this.mSettings.mPermissions.remove(name);
                this.mSettings.writeLPr();
            }
        }
    }

    private static void enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission(Package pkg, BasePermission bp) {
        if (pkg.requestedPermissions.indexOf(bp.name) == -1) {
            throw new SecurityException("Package " + pkg.packageName + " has not requested permission " + bp.name);
        } else if (!bp.isRuntime() && (bp.isDevelopment() ^ 1) != 0) {
            throw new SecurityException("Permission " + bp.name + " is not a changeable permission type");
        }
    }

    public void grantRuntimePermission(String packageName, String name, int userId) {
        grantRuntimePermission(packageName, name, userId, false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void grantRuntimePermission(String packageName, String name, int userId, boolean overridePolicy) {
        if (sUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "grantRuntimePermission");
            enforceCrossUserPermission(callingUid, userId, true, true, "grantRuntimePermission");
            synchronized (this.mPackages) {
                Package pkg = (Package) this.mPackages.get(packageName);
                if (pkg == null) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(name);
                if (bp == null) {
                    throw new IllegalArgumentException("Unknown permission: " + name);
                }
                PackageSetting ps = pkg.mExtras;
                if (ps == null || filterAppAccessLPr(ps, callingUid, userId)) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission(pkg, bp);
                if (this.mPermissionReviewRequired && pkg.applicationInfo.targetSdkVersion < 23 && bp.isRuntime()) {
                    return;
                }
                int uid = UserHandle.getUid(userId, pkg.applicationInfo.uid);
                PermissionsState permissionsState = ps.getPermissionsState();
                int flags = permissionsState.getPermissionFlags(name, userId);
                if ((flags & 16) != 0) {
                    throw new SecurityException("Cannot grant system fixed permission " + name + " for package " + packageName);
                } else if (!overridePolicy && (flags & 4) != 0) {
                    throw new SecurityException("Cannot grant policy fixed permission " + name + " for package " + packageName);
                } else if (bp.isDevelopment()) {
                    if (permissionsState.grantInstallPermission(bp) != -1) {
                        scheduleWriteSettingsLocked();
                    }
                } else if (ps.getInstantApp(userId) && (bp.isInstant() ^ 1) != 0) {
                    throw new SecurityException("Cannot grant non-ephemeral permission" + name + " for package " + packageName);
                } else if (pkg.applicationInfo.targetSdkVersion < 23) {
                    Slog.w(TAG, "Cannot grant runtime permission to a legacy app");
                } else {
                    int result = permissionsState.grantRuntimePermission(bp, userId);
                    switch (result) {
                        case -1:
                            Flog.i(201, "grantRuntimePermission : for " + packageName + ", Permission " + name + ", userId " + userId + ", result " + result);
                            return;
                        case 1:
                            final int appId = UserHandle.getAppId(pkg.applicationInfo.uid);
                            final int i = userId;
                            this.mHandler.post(new Runnable() {
                                public void run() {
                                    PackageManagerService.this.killUid(appId, i, PackageManagerService.KILL_APP_REASON_GIDS_CHANGED);
                                }
                            });
                            break;
                    }
                    if (bp.isRuntime()) {
                        logPermissionGranted(this.mContext, name, packageName);
                    }
                    this.mOnPermissionChangeListeners.onPermissionsChanged(uid);
                    this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
                }
            }
        } else {
            Log.e(TAG, "No such user:" + userId);
        }
    }

    public void revokeRuntimePermission(String packageName, String name, int userId) {
        revokeRuntimePermission(packageName, name, userId, false);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void revokeRuntimePermission(String packageName, String name, int userId, boolean overridePolicy) {
        Flog.i(201, "revokeRuntimePermission : for " + packageName + ", Permission " + name + ", userId " + userId + ", calling pid " + Binder.getCallingPid());
        if (sUserManager.exists(userId)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
            enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "revokeRuntimePermission");
            synchronized (this.mPackages) {
                Package pkg = (Package) this.mPackages.get(packageName);
                if (pkg == null) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                PackageSetting ps = pkg.mExtras;
                if (ps == null || filterAppAccessLPr(ps, Binder.getCallingUid(), userId)) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(name);
                if (bp == null) {
                    throw new IllegalArgumentException("Unknown permission: " + name);
                }
                enforceDeclaredAsUsedAndRuntimeOrDevelopmentPermission(pkg, bp);
                if (this.mPermissionReviewRequired && pkg.applicationInfo.targetSdkVersion < 23 && bp.isRuntime()) {
                    return;
                }
                PermissionsState permissionsState = ps.getPermissionsState();
                int flags = permissionsState.getPermissionFlags(name, userId);
                if ((flags & 16) != 0) {
                    throw new SecurityException("Cannot revoke system fixed permission " + name + " for package " + packageName);
                } else if (!overridePolicy && (flags & 4) != 0) {
                    throw new SecurityException("Cannot revoke policy fixed permission " + name + " for package " + packageName);
                } else if (bp.isDevelopment()) {
                    if (permissionsState.revokeInstallPermission(bp) != -1) {
                        scheduleWriteSettingsLocked();
                    }
                } else if (permissionsState.revokeRuntimePermission(bp, userId) == -1) {
                    return;
                } else {
                    if (bp.isRuntime()) {
                        logPermissionRevoked(this.mContext, name, packageName);
                    }
                    this.mOnPermissionChangeListeners.onPermissionsChanged(pkg.applicationInfo.uid);
                    this.mSettings.writeRuntimePermissionsForUserLPr(userId, true);
                    int appId = UserHandle.getAppId(pkg.applicationInfo.uid);
                    killUid(appId, userId, KILL_APP_REASON_PERMISSIONS_REVOKED);
                    return;
                }
            }
        }
        Log.e(TAG, "No such user:" + userId);
    }

    private void revokeRuntimePermissionsIfGroupChanged(Package newPackage, Package oldPackage, ArrayList<String> allPackageNames) {
        int numOldPackagePermissions = oldPackage.permissions.size();
        ArrayMap<String, String> arrayMap = new ArrayMap(numOldPackagePermissions);
        for (int i = 0; i < numOldPackagePermissions; i++) {
            Permission permission = (Permission) oldPackage.permissions.get(i);
            if (permission.group != null) {
                arrayMap.put(permission.info.name, permission.group.info.name);
            }
        }
        int numNewPackagePermissions = newPackage.permissions.size();
        for (int newPermissionNum = 0; newPermissionNum < numNewPackagePermissions; newPermissionNum++) {
            Permission newPermission = (Permission) newPackage.permissions.get(newPermissionNum);
            if ((newPermission.info.protectionLevel & 1) != 0) {
                String permissionName = newPermission.info.name;
                String str = newPermission.group == null ? null : newPermission.group.info.name;
                String oldPermissionGroupName = (String) arrayMap.get(permissionName);
                if (!(str == null || (str.equals(oldPermissionGroupName) ^ 1) == 0)) {
                    List<UserInfo> users = ((UserManager) this.mContext.getSystemService(UserManager.class)).getUsers();
                    int numUsers = users.size();
                    for (int userNum = 0; userNum < numUsers; userNum++) {
                        int userId = ((UserInfo) users.get(userNum)).id;
                        int numPackages = allPackageNames.size();
                        for (int packageNum = 0; packageNum < numPackages; packageNum++) {
                            String packageName = (String) allPackageNames.get(packageNum);
                            if (checkPermission(permissionName, packageName, userId) == 0) {
                                EventLog.writeEvent(1397638484, new Object[]{"72710897", Integer.valueOf(newPackage.applicationInfo.uid), "Revoking permission", permissionName, "from package", packageName, "as the group changed from", oldPermissionGroupName, "to", str});
                                try {
                                    revokeRuntimePermission(packageName, permissionName, userId, false);
                                } catch (IllegalArgumentException e) {
                                    Slog.e(TAG, "Could not revoke " + permissionName + " from " + packageName, e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static int getBaseEventId(String name) {
        int eventIdIndex = ALL_DANGEROUS_PERMISSIONS.indexOf(name);
        if (eventIdIndex != -1) {
            return (eventIdIndex * 4) + 634;
        }
        if (AppOpsManager.permissionToOpCode(name) == -1 || Build.IS_USER) {
            Log.i(TAG, "Unknown permission " + name);
            return 630;
        }
        throw new IllegalStateException("Unknown permission " + name);
    }

    private static void logPermissionRevoked(Context context, String name, String packageName) {
        MetricsLogger.action(context, getBaseEventId(name) + 3, packageName);
    }

    private static void logPermissionGranted(Context context, String name, String packageName) {
        MetricsLogger.action(context, getBaseEventId(name) + 1, packageName);
    }

    public void resetRuntimePermissions() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS", "revokeRuntimePermission");
        int callingUid = Binder.getCallingUid();
        if (!(callingUid == 1000 || callingUid == 0)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "resetRuntimePermissions");
        }
        synchronized (this.mPackages) {
            updatePermissionsLPw(null, null, 1);
            for (int userId : UserManagerService.getInstance().getUserIds()) {
                int packageCount = this.mPackages.size();
                for (int i = 0; i < packageCount; i++) {
                    Package pkg = (Package) this.mPackages.valueAt(i);
                    if (pkg.mExtras instanceof PackageSetting) {
                        resetUserChangesToRuntimePermissionsAndFlagsLPw(pkg.mExtras, userId);
                    }
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getPermissionFlags(String name, String packageName, int userId) {
        if (!sUserManager.exists(userId)) {
            return 0;
        }
        enforceGrantRevokeRuntimePermissionPermissions("getPermissionFlags");
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "getPermissionFlags");
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null) {
                return 0;
            } else if (((BasePermission) this.mSettings.mPermissions.get(name)) == null) {
                return 0;
            } else {
                PackageSetting ps = pkg.mExtras;
                if (ps == null || filterAppAccessLPr(ps, callingUid, userId)) {
                } else {
                    int permissionFlags = ps.getPermissionsState().getPermissionFlags(name, userId);
                    return permissionFlags;
                }
            }
        }
    }

    public void updatePermissionFlags(String name, String packageName, int flagMask, int flagValues, int userId) {
        if (sUserManager.exists(userId)) {
            enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlags");
            int callingUid = Binder.getCallingUid();
            enforceCrossUserPermission(callingUid, userId, true, true, "updatePermissionFlags");
            if (getCallingUid() != 1000) {
                flagMask = (flagMask & -17) & -33;
                flagValues = ((flagValues & -17) & -33) & -65;
            }
            synchronized (this.mPackages) {
                Package pkg = (Package) this.mPackages.get(packageName);
                if (pkg == null) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                PackageSetting ps = pkg.mExtras;
                if (ps == null || filterAppAccessLPr(ps, callingUid, userId)) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                }
                BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(name);
                if (bp == null) {
                    throw new IllegalArgumentException("Unknown permission: " + name);
                }
                PermissionsState permissionsState = ps.getPermissionsState();
                boolean hadState = permissionsState.getRuntimePermissionState(name, userId) != null;
                if (permissionsState.updatePermissionFlags(bp, userId, flagMask, flagValues)) {
                    if (permissionsState.getInstallPermissionState(name) != null) {
                        scheduleWriteSettingsLocked();
                    } else if (permissionsState.getRuntimePermissionState(name, userId) != null || hadState) {
                        this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
                    }
                }
            }
        }
    }

    public void updatePermissionFlagsForAllApps(int flagMask, int flagValues, int userId) {
        if (sUserManager.exists(userId)) {
            enforceGrantRevokeRuntimePermissionPermissions("updatePermissionFlagsForAllApps");
            enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "updatePermissionFlagsForAllApps");
            if (getCallingUid() != 1000) {
                flagMask &= -17;
                flagValues &= -17;
            }
            synchronized (this.mPackages) {
                boolean changed = false;
                int packageCount = this.mPackages.size();
                for (int pkgIndex = 0; pkgIndex < packageCount; pkgIndex++) {
                    PackageSetting ps = ((Package) this.mPackages.valueAt(pkgIndex)).mExtras;
                    if (ps != null) {
                        changed |= ps.getPermissionsState().updatePermissionFlagsForAllPermissions(userId, flagMask, flagValues);
                    }
                }
                if (changed) {
                    this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
                }
            }
        }
    }

    private void enforceGrantRevokeRuntimePermissionPermissions(String message) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS") != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.REVOKE_RUNTIME_PERMISSIONS") != 0) {
            throw new SecurityException(message + " requires " + "android.permission.GRANT_RUNTIME_PERMISSIONS" + " or " + "android.permission.REVOKE_RUNTIME_PERMISSIONS");
        }
    }

    public boolean shouldShowRequestPermissionRationale(String permissionName, String packageName, int userId) {
        boolean z = false;
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "canShowRequestPermissionRationale for user " + userId);
        }
        if (UserHandle.getAppId(getCallingUid()) != UserHandle.getAppId(getPackageUid(packageName, 268435456, userId)) || checkPermission(permissionName, packageName, userId) == 0) {
            return false;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            int flags = getPermissionFlags(permissionName, packageName, userId);
            if ((flags & 22) != 0) {
                return false;
            }
            if ((flags & 1) != 0) {
                z = true;
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void addOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS", "addOnPermissionsChangeListener");
        synchronized (this.mPackages) {
            this.mOnPermissionChangeListeners.addListenerLocked(listener);
        }
    }

    public void removeOnPermissionsChangeListener(IOnPermissionsChangeListener listener) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        synchronized (this.mPackages) {
            this.mOnPermissionChangeListeners.removeListenerLocked(listener);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isProtectedBroadcast(String actionName) {
        synchronized (this.mProtectedBroadcasts) {
            if (this.mProtectedBroadcasts.contains(actionName)) {
                return true;
            } else if (actionName != null) {
                if (actionName.startsWith("android.net.netmon.lingerExpired") || actionName.startsWith("com.android.server.sip.SipWakeupTimer") || actionName.startsWith("com.android.internal.telephony.data-reconnect") || actionName.startsWith("android.net.netmon.launchCaptivePortalApp")) {
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int checkSignatures(String pkg1, String pkg2) {
        synchronized (this.mPackages) {
            Package p1 = (Package) this.mPackages.get(pkg1);
            Package p2 = (Package) this.mPackages.get(pkg2);
            if (!(p1 == null || p1.mExtras == null || p2 == null)) {
                if (p2.mExtras != null) {
                    int callingUid = Binder.getCallingUid();
                    int callingUserId = UserHandle.getUserId(callingUid);
                    PackageSetting ps2 = p2.mExtras;
                    if (filterAppAccessLPr(p1.mExtras, callingUid, callingUserId) || filterAppAccessLPr(ps2, callingUid, callingUserId)) {
                    } else {
                        int compareSignatures = compareSignatures(p1.mSignatures, p2.mSignatures);
                        return compareSignatures;
                    }
                }
            }
        }
    }

    public int checkUidSignatures(int uid1, int uid2) {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        boolean isCallerInstantApp = getInstantAppPackageName(callingUid) != null;
        uid1 = UserHandle.getAppId(uid1);
        uid2 = UserHandle.getAppId(uid2);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid1);
            if (obj != null) {
                Signature[] s1;
                PackageSetting ps;
                if (obj instanceof SharedUserSetting) {
                    if (isCallerInstantApp) {
                        return -4;
                    }
                    s1 = ((SharedUserSetting) obj).signatures.mSignatures;
                } else if (obj instanceof PackageSetting) {
                    ps = (PackageSetting) obj;
                    if (filterAppAccessLPr(ps, callingUid, callingUserId)) {
                        return -4;
                    }
                    s1 = ps.signatures.mSignatures;
                } else {
                    return -4;
                }
                obj = this.mSettings.getUserIdLPr(uid2);
                if (obj != null) {
                    Signature[] s2;
                    if (obj instanceof SharedUserSetting) {
                        if (isCallerInstantApp) {
                            return -4;
                        }
                        s2 = ((SharedUserSetting) obj).signatures.mSignatures;
                    } else if (obj instanceof PackageSetting) {
                        ps = (PackageSetting) obj;
                        if (filterAppAccessLPr(ps, callingUid, callingUserId)) {
                            return -4;
                        }
                        s2 = ps.signatures.mSignatures;
                    } else {
                        return -4;
                    }
                    int compareSignatures = compareSignatures(s1, s2);
                    return compareSignatures;
                }
                return -4;
            }
            return -4;
        }
    }

    private void killUid(int appId, int userId, String reason) {
        long identity = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManager.getService();
            if (am != null) {
                try {
                    am.killUid(appId, userId, reason);
                } catch (RemoteException e) {
                }
            }
            Binder.restoreCallingIdentity(identity);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
        }
    }

    static int compareSignatures(Signature[] s1, Signature[] s2) {
        int i = 1;
        if (s1 == null) {
            if (s2 != null) {
                i = -1;
            }
            return i;
        } else if (s2 == null) {
            return -2;
        } else {
            if (s1.length != s2.length) {
                return -3;
            }
            if (s1.length == 1) {
                return s1[0].equals(s2[0]) ? 0 : -3;
            }
            ArraySet<Signature> set1 = new ArraySet();
            for (Signature sig : s1) {
                set1.add(sig);
            }
            ArraySet<Signature> set2 = new ArraySet();
            for (Signature sig2 : s2) {
                set2.add(sig2);
            }
            return set1.equals(set2) ? 0 : -3;
        }
    }

    private boolean isCompatSignatureUpdateNeeded(Package scannedPkg) {
        VersionInfo ver = getSettingsVersionForPackage(scannedPkg);
        if (ver == null || ver.databaseVersion >= 2) {
            return false;
        }
        return true;
    }

    private int compareSignaturesCompat(PackageSignatures existingSigs, Package scannedPkg) {
        if (!isCompatSignatureUpdateNeeded(scannedPkg)) {
            return -3;
        }
        ArraySet<Signature> existingSet = new ArraySet();
        for (Signature sig : existingSigs.mSignatures) {
            existingSet.add(sig);
        }
        ArraySet<Signature> scannedCompatSet = new ArraySet();
        for (Signature sig2 : scannedPkg.mSignatures) {
            try {
                for (Signature chainSig : sig2.getChainSignatures()) {
                    scannedCompatSet.add(chainSig);
                }
            } catch (CertificateEncodingException e) {
                scannedCompatSet.add(sig2);
            }
        }
        if (!scannedCompatSet.equals(existingSet)) {
            return -3;
        }
        existingSigs.assignSignatures(scannedPkg.mSignatures);
        synchronized (this.mPackages) {
            this.mSettings.mKeySetManagerService.removeAppKeySetDataLPw(scannedPkg.packageName);
        }
        return 0;
    }

    private boolean isRecoverSignatureUpdateNeeded(Package scannedPkg) {
        VersionInfo ver = getSettingsVersionForPackage(scannedPkg);
        if (ver == null || ver.databaseVersion >= 3) {
            return false;
        }
        return true;
    }

    private int compareSignaturesRecover(PackageSignatures existingSigs, Package scannedPkg) {
        if (!isRecoverSignatureUpdateNeeded(scannedPkg)) {
            return -3;
        }
        String msg = null;
        try {
            if (Signature.areEffectiveMatch(existingSigs.mSignatures, scannedPkg.mSignatures)) {
                logCriticalInfo(4, "Recovered effectively matching certificates for " + scannedPkg.packageName);
                return 0;
            }
        } catch (CertificateException e) {
            msg = e.getMessage();
        }
        logCriticalInfo(4, "Failed to recover certificates for " + scannedPkg.packageName + ": " + msg);
        return -3;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<String> getAllPackages() {
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        synchronized (this.mPackages) {
            if (canViewInstantApps(callingUid, callingUserId)) {
                List arrayList = new ArrayList(this.mPackages.keySet());
                return arrayList;
            }
            String instantAppPkgName = getInstantAppPackageName(callingUid);
            List<String> result = new ArrayList();
            if (instantAppPkgName != null) {
                for (Package pkg : this.mPackages.values()) {
                    if (pkg.visibleToInstantApps) {
                        result.add(pkg.packageName);
                    }
                }
            } else {
                for (Package pkg2 : this.mPackages.values()) {
                    PackageSetting packageSetting = pkg2.mExtras != null ? (PackageSetting) pkg2.mExtras : null;
                    if (packageSetting == null || !packageSetting.getInstantApp(callingUserId) || (this.mInstantAppRegistry.isInstantAccessGranted(callingUserId, UserHandle.getAppId(callingUid), packageSetting.appId) ^ 1) == 0) {
                        result.add(pkg2.packageName);
                    }
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String[] getPackagesForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        boolean isCallerInstantApp = getInstantAppPackageName(callingUid) != null;
        int userId = UserHandle.getUserId(uid);
        uid = UserHandle.getAppId(uid);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid);
            PackageSetting ps;
            if (obj instanceof SharedUserSetting) {
                if (isCallerInstantApp) {
                    return null;
                }
                SharedUserSetting sus = (SharedUserSetting) obj;
                String[] res = new String[sus.packages.size()];
                Iterator<PackageSetting> it = sus.packages.iterator();
                int i = 0;
                while (it.hasNext()) {
                    int i2;
                    ps = (PackageSetting) it.next();
                    if (this.mIsUpgrade) {
                        PackageSetting psNow = (PackageSetting) this.mSettings.mPackages.get(ps.name);
                        if (!(psNow == null || psNow.appId == ps.appId)) {
                            res = (String[]) ArrayUtils.removeElement(String.class, res, res[i]);
                            StringBuilder sbWarn = new StringBuilder();
                            sbWarn.append("getPackagesForUid ").append(uid).append(" warning, found package ").append(ps.name).append(" was user id ").append(ps.appId).append(", but mismatch ").append(psNow.appId).append(" now!");
                            Slog.w(TAG, sbWarn.toString());
                        }
                    }
                    if (ps.getInstalled(userId)) {
                        i2 = i + 1;
                        res[i] = ps.name;
                    } else {
                        res = (String[]) ArrayUtils.removeElement(String.class, res, res[i]);
                        i2 = i;
                    }
                    i = i2;
                }
                return res;
            } else if (obj instanceof PackageSetting) {
                ps = (PackageSetting) obj;
                if (ps.getInstalled(userId) && (filterAppAccessLPr(ps, callingUid, userId) ^ 1) != 0) {
                    String[] strArr = new String[]{ps.name};
                    return strArr;
                }
            }
        }
    }

    public String getNameForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            String str;
            if (obj instanceof SharedUserSetting) {
                SharedUserSetting sus = (SharedUserSetting) obj;
                str = sus.name + ":" + sus.userId;
                return str;
            } else if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                if (filterAppAccessLPr(ps, callingUid, UserHandle.getUserId(callingUid))) {
                    return null;
                }
                str = ps.name;
                return str;
            } else {
                return null;
            }
        }
    }

    public String[] getNamesForUids(int[] uids) {
        if (uids == null || uids.length == 0) {
            return null;
        }
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        String[] names = new String[uids.length];
        synchronized (this.mPackages) {
            for (int i = uids.length - 1; i >= 0; i--) {
                Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uids[i]));
                if (obj instanceof SharedUserSetting) {
                    names[i] = "shared:" + ((SharedUserSetting) obj).name;
                } else if (obj instanceof PackageSetting) {
                    PackageSetting ps = (PackageSetting) obj;
                    if (filterAppAccessLPr(ps, callingUid, UserHandle.getUserId(callingUid))) {
                        names[i] = null;
                    } else {
                        names[i] = ps.name;
                    }
                } else {
                    names[i] = null;
                }
            }
        }
        return names;
    }

    public int getUidForSharedUser(String sharedUserName) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || sharedUserName == null) {
            return -1;
        }
        synchronized (this.mPackages) {
            try {
                SharedUserSetting suid = this.mSettings.getSharedUserLPw(sharedUserName, 0, 0, false);
                if (suid != null) {
                    int i = suid.userId;
                    return i;
                }
            } catch (PackageManagerException e) {
            }
        }
        return -1;
    }

    public int getFlagsForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            int i;
            if (obj instanceof SharedUserSetting) {
                i = ((SharedUserSetting) obj).pkgFlags;
                return i;
            } else if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                if (filterAppAccessLPr(ps, callingUid, UserHandle.getUserId(callingUid))) {
                    return 0;
                }
                i = ps.pkgFlags;
                return i;
            } else {
                return 0;
            }
        }
    }

    public int getPrivateFlagsForUid(int uid) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(uid));
            int i;
            if (obj instanceof SharedUserSetting) {
                i = ((SharedUserSetting) obj).pkgPrivateFlags;
                return i;
            } else if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                if (filterAppAccessLPr(ps, callingUid, UserHandle.getUserId(callingUid))) {
                    return 0;
                }
                i = ps.pkgPrivateFlags;
                return i;
            } else {
                return 0;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isUidPrivileged(int uid) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return false;
        }
        uid = UserHandle.getAppId(uid);
        synchronized (this.mPackages) {
            Object obj = this.mSettings.getUserIdLPr(uid);
            if (obj instanceof SharedUserSetting) {
                Iterator<PackageSetting> it = ((SharedUserSetting) obj).packages.iterator();
                while (it.hasNext()) {
                    if (((PackageSetting) it.next()).isPrivileged()) {
                        return true;
                    }
                }
            } else if (obj instanceof PackageSetting) {
                boolean isPrivileged = ((PackageSetting) obj).isPrivileged();
                return isPrivileged;
            }
        }
    }

    public String[] getAppOpPermissionPackages(String permissionName) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            ArraySet<String> pkgs = (ArraySet) this.mAppOpPermissionPackages.get(permissionName);
            if (pkgs == null) {
                return null;
            }
            String[] strArr = (String[]) pkgs.toArray(new String[pkgs.size()]);
            return strArr;
        }
    }

    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        return resolveIntentInternal(intent, resolvedType, flags, userId, false);
    }

    private ResolveInfo resolveIntentInternal(Intent intent, String resolvedType, int flags, int userId, boolean resolveForStart) {
        try {
            Trace.traceBegin(262144, "resolveIntent");
            if (!sUserManager.exists(userId)) {
                return null;
            }
            int callingUid = Binder.getCallingUid();
            flags = updateFlagsForResolve(flags, userId, intent, callingUid, resolveForStart);
            enforceCrossUserPermission(callingUid, userId, false, false, "resolve intent");
            Trace.traceBegin(262144, "queryIntentActivities");
            List<ResolveInfo> query = queryIntentActivitiesInternal(intent, resolvedType, flags, callingUid, userId, resolveForStart, true);
            Trace.traceEnd(262144);
            ResolveInfo bestChoice = chooseBestActivity(intent, resolvedType, flags, query, userId);
            if (!(bestChoice == null || query == null || query.size() <= 1)) {
                Slog.d(TAG, "resolve intent for uid:" + callingUid + ", matchs:" + query.size() + ", res:" + bestChoice);
            }
            Trace.traceEnd(262144);
            return bestChoice;
        } finally {
            Trace.traceEnd(262144);
        }
    }

    public ResolveInfo findPersistentPreferredActivity(Intent intent, int userId) {
        if (!UserHandle.isSameApp(Binder.getCallingUid(), 1000)) {
            throw new SecurityException("findPersistentPreferredActivity can only be run by the system");
        } else if (!sUserManager.exists(userId)) {
            return null;
        } else {
            ResolveInfo findPersistentPreferredActivityLP;
            int callingUid = Binder.getCallingUid();
            intent = updateIntentForResolve(intent);
            String resolvedType = intent.resolveTypeIfNeeded(this.mContext.getContentResolver());
            int flags = updateFlagsForResolve(0, userId, intent, callingUid, false);
            List<ResolveInfo> query = queryIntentActivitiesInternal(intent, resolvedType, flags, userId);
            synchronized (this.mPackages) {
                findPersistentPreferredActivityLP = findPersistentPreferredActivityLP(intent, resolvedType, flags, query, false, userId);
            }
            return findPersistentPreferredActivityLP;
        }
    }

    public void setLastChosenActivity(Intent intent, String resolvedType, int flags, IntentFilter filter, int match, ComponentName activity) {
        if (getInstantAppPackageName(Binder.getCallingUid()) == null) {
            int userId = UserHandle.getCallingUserId();
            intent.setComponent(null);
            findPreferredActivity(intent, resolvedType, flags, queryIntentActivitiesInternal(intent, resolvedType, flags, userId), 0, false, true, false, userId);
            addPreferredActivityInternal(filter, match, null, activity, false, userId, "Setting last chosen");
        }
    }

    public ResolveInfo getLastChosenActivity(Intent intent, String resolvedType, int flags) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        int userId = UserHandle.getCallingUserId();
        return findPreferredActivity(intent, resolvedType, flags, queryIntentActivitiesInternal(intent, resolvedType, flags, userId), 0, false, false, false, userId);
    }

    private boolean isEphemeralDisabled() {
        return this.mEphemeralAppsDisabled;
    }

    private boolean isInstantAppAllowed(Intent intent, List<ResolveInfo> resolvedActivities, int userId, boolean skipPackageCheck) {
        if (this.mInstantAppResolverConnection == null) {
            return false;
        }
        if (this.mInstantAppInstallerActivity == null) {
            return false;
        }
        if (intent.getComponent() != null) {
            return false;
        }
        if ((intent.getFlags() & 512) != 0) {
            return false;
        }
        if (!skipPackageCheck && intent.getPackage() != null) {
            return false;
        }
        if (!hasWebURI(intent) || intent.getData().getHost() == null) {
            return false;
        }
        synchronized (this.mPackages) {
            int count = resolvedActivities == null ? 0 : resolvedActivities.size();
            for (int n = 0; n < count; n++) {
                ResolveInfo info = (ResolveInfo) resolvedActivities.get(n);
                String packageName = info.activityInfo.packageName;
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (ps != null) {
                    if (!info.handleAllWebDataURI) {
                        int status = (int) (getDomainVerificationStatusLPr(ps, userId) >> 32);
                        if (status == 2 || status == 4) {
                            if (DEBUG_EPHEMERAL) {
                                Slog.v(TAG, "DENY instant app; pkg: " + packageName + ", status: " + status);
                            }
                            return false;
                        }
                    }
                    if (ps.getInstantApp(userId)) {
                        if (DEBUG_EPHEMERAL) {
                            Slog.v(TAG, "DENY instant app installed; pkg: " + packageName);
                        }
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private void requestInstantAppResolutionPhaseTwo(AuxiliaryResolveInfo responseObj, Intent origIntent, String resolvedType, String callingPackage, Bundle verificationBundle, int userId) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(20, new InstantAppRequest(responseObj, origIntent, resolvedType, callingPackage, userId, verificationBundle, false)));
    }

    private ResolveInfo chooseBestActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int userId) {
        if (query != null) {
            int N = query.size();
            if (N == 1) {
                return (ResolveInfo) query.get(0);
            }
            if (N > 1) {
                boolean debug = (intent.getFlags() & 8) != 0;
                ResolveInfo r0 = (ResolveInfo) query.get(0);
                ResolveInfo r1 = (ResolveInfo) query.get(1);
                if (debug) {
                    Slog.v(TAG, r0.activityInfo.name + "=" + r0.priority + " vs " + r1.activityInfo.name + "=" + r1.priority);
                }
                if (r0.priority != r1.priority || r0.preferredOrder != r1.preferredOrder || r0.isDefault != r1.isDefault) {
                    return (ResolveInfo) query.get(0);
                }
                ResolveInfo ri = findPreferredActivity(intent, resolvedType, flags, query, r0.priority, true, false, debug, userId);
                if (ri != null) {
                    return ri;
                }
                for (int i = 0; i < N; i++) {
                    ri = (ResolveInfo) query.get(i);
                    if (ri.activityInfo.applicationInfo.isInstantApp()) {
                        if (((int) (getDomainVerificationStatusLPr((PackageSetting) this.mSettings.mPackages.get(ri.activityInfo.packageName), userId) >> 32)) != 4) {
                            return ri;
                        }
                    }
                }
                ResolveInfo resolveInfo = new ResolveInfo(this.mResolveInfo);
                resolveInfo.activityInfo = new ActivityInfo(resolveInfo.activityInfo);
                resolveInfo.activityInfo.labelRes = ResolverActivity.getLabelRes(intent.getAction());
                String intentPackage = intent.getPackage();
                if (!TextUtils.isEmpty(intentPackage) && allHavePackage(query, intentPackage)) {
                    ApplicationInfo appi = ((ResolveInfo) query.get(0)).activityInfo.applicationInfo;
                    resolveInfo.resolvePackageName = intentPackage;
                    if (userNeedsBadging(userId)) {
                        resolveInfo.noResourceId = true;
                    } else {
                        resolveInfo.icon = appi.icon;
                    }
                    resolveInfo.iconResourceId = appi.icon;
                    resolveInfo.labelRes = appi.labelRes;
                }
                resolveInfo.activityInfo.applicationInfo = new ApplicationInfo(resolveInfo.activityInfo.applicationInfo);
                if (userId != 0) {
                    resolveInfo.activityInfo.applicationInfo.uid = UserHandle.getUid(userId, UserHandle.getAppId(resolveInfo.activityInfo.applicationInfo.uid));
                }
                if (resolveInfo.activityInfo.metaData == null) {
                    resolveInfo.activityInfo.metaData = new Bundle();
                }
                resolveInfo.activityInfo.metaData.putBoolean("android.dock_home", true);
                return resolveInfo;
            }
        }
        return null;
    }

    private boolean allHavePackage(List<ResolveInfo> list, String packageName) {
        if (ArrayUtils.isEmpty(list)) {
            return false;
        }
        int N = list.size();
        for (int i = 0; i < N; i++) {
            ResolveInfo ri = (ResolveInfo) list.get(i);
            ActivityInfo activityInfo = ri != null ? ri.activityInfo : null;
            if (activityInfo == null || (packageName.equals(activityInfo.packageName) ^ 1) != 0) {
                return false;
            }
        }
        return true;
    }

    private ResolveInfo findPersistentPreferredActivityLP(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, boolean debug, int userId) {
        List queryIntent;
        int N = query.size();
        PersistentPreferredIntentResolver ppir = (PersistentPreferredIntentResolver) this.mSettings.mPersistentPreferredActivities.get(userId);
        if (debug) {
            Slog.v(TAG, "Looking for presistent preferred activities...");
        }
        if (ppir != null) {
            queryIntent = ppir.queryIntent(intent, resolvedType, (65536 & flags) != 0, userId);
        } else {
            queryIntent = null;
        }
        if (queryIntent != null && queryIntent.size() > 0) {
            int M = queryIntent.size();
            for (int i = 0; i < M; i++) {
                PersistentPreferredActivity ppa = (PersistentPreferredActivity) queryIntent.get(i);
                if (debug) {
                    Slog.v(TAG, "Checking PersistentPreferredActivity ds=" + (ppa.countDataSchemes() > 0 ? ppa.getDataScheme(0) : "<none>") + "\n  component=" + ppa.mComponent);
                    ppa.dump(new LogPrinter(2, TAG, 3), "  ");
                }
                ActivityInfo ai = getActivityInfo(ppa.mComponent, flags | 512, userId);
                if (debug) {
                    Slog.v(TAG, "Found persistent preferred activity:");
                    if (ai != null) {
                        ai.dump(new LogPrinter(2, TAG, 3), "  ");
                    } else {
                        Slog.v(TAG, "  null");
                    }
                }
                if (ai != null) {
                    for (int j = 0; j < N; j++) {
                        ResolveInfo ri = (ResolveInfo) query.get(j);
                        if (ri.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) && ri.activityInfo.name.equals(ai.name)) {
                            if (debug) {
                                Slog.v(TAG, "Returning persistent preferred activity: " + ri.activityInfo.packageName + "/" + ri.activityInfo.name);
                            }
                            return ri;
                        }
                    }
                    continue;
                }
            }
        }
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    ResolveInfo findPreferredActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int priority, boolean always, boolean removeMatches, boolean debug, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        flags = updateFlagsForResolve(flags, userId, intent, Binder.getCallingUid(), false);
        intent = updateIntentForResolve(intent);
        synchronized (this.mPackages) {
            ResolveInfo pri = findPersistentPreferredActivityLP(intent, resolvedType, flags, query, debug, userId);
            if (pri != null) {
                return pri;
            }
            List queryIntent;
            PreferredIntentResolver pir = (PreferredIntentResolver) this.mSettings.mPreferredActivities.get(userId);
            if (debug) {
                Slog.v(TAG, "Looking for preferred activities...");
            }
            if (pir != null) {
                queryIntent = pir.queryIntent(intent, resolvedType, (65536 & flags) != 0, userId);
            } else {
                queryIntent = null;
            }
            if (queryIntent != null && queryIntent.size() > 0) {
                int j;
                ResolveInfo ri;
                boolean changed = false;
                int match = 0;
                if (debug) {
                    try {
                        Slog.v(TAG, "Figuring out best match...");
                    } catch (Throwable th) {
                        if (changed) {
                            scheduleWritePackageRestrictionsLocked(userId);
                        }
                    }
                }
                int N = query.size();
                for (j = 0; j < N; j++) {
                    ri = (ResolveInfo) query.get(j);
                    if (debug) {
                        Slog.v(TAG, "Match for " + ri.activityInfo + ": 0x" + Integer.toHexString(match));
                    }
                    if (ri.match > match) {
                        match = ri.match;
                    }
                }
                if (debug) {
                    Slog.v(TAG, "Best match: 0x" + Integer.toHexString(match));
                }
                match &= 268369920;
                int M = queryIntent.size();
                for (int i = 0; i < M; i++) {
                    IntentFilter pa = (PreferredActivity) queryIntent.get(i);
                    if (debug) {
                        Slog.v(TAG, "Checking PreferredActivity ds=" + (pa.countDataSchemes() > 0 ? pa.getDataScheme(0) : "<none>") + "\n  component=" + pa.mPref.mComponent);
                        pa.dump(new LogPrinter(2, TAG, 3), "  ");
                    }
                    if (pa.mPref.mMatch != match) {
                        if (debug) {
                            Slog.v(TAG, "Skipping bad match " + Integer.toHexString(pa.mPref.mMatch));
                        }
                    } else if (!always || (pa.mPref.mAlways ^ 1) == 0) {
                        ActivityInfo ai = getActivityInfo(pa.mPref.mComponent, ((flags | 512) | 524288) | 262144, userId);
                        if (debug) {
                            Slog.v(TAG, "Found preferred activity:");
                            if (ai != null) {
                                ai.dump(new LogPrinter(2, TAG, 3), "  ");
                            } else {
                                Slog.v(TAG, "  null");
                            }
                        }
                        if (ai == null) {
                            Slog.w(TAG, "Removing dangling preferred activity: " + pa.mPref.mComponent);
                            pir.removeFilter(pa);
                            changed = true;
                        } else {
                            j = 0;
                            while (j < N) {
                                ri = (ResolveInfo) query.get(j);
                                if (!ri.activityInfo.applicationInfo.packageName.equals(ai.applicationInfo.packageName) || !ri.activityInfo.name.equals(ai.name)) {
                                    j++;
                                } else if (removeMatches) {
                                    pir.removeFilter(pa);
                                    changed = true;
                                } else {
                                    boolean audioType;
                                    int sameSet = always ? pa.mPref.sameSet((List) query) ^ 1 : 0;
                                    if (intent.getAction() == null || !intent.getAction().equals("android.intent.action.VIEW") || intent.getData() == null || intent.getData().getScheme() == null || (!(intent.getData().getScheme().equals("file") || intent.getData().getScheme().equals("content")) || intent.getType() == null)) {
                                        audioType = false;
                                    } else {
                                        audioType = intent.getType().startsWith("audio/");
                                    }
                                    if (audioType) {
                                        Slog.i(TAG, "preferred activity for " + intent + " type " + resolvedType + ", do not dropping preferred activity");
                                        sameSet = 0;
                                    }
                                    if (sameSet != 0) {
                                        int i2 = intent.hasCategory("android.intent.category.HOME") ? (intent.getFlags() & 512) != 0 ? 1 : 0 : 0;
                                        if ((i2 ^ 1) != 0) {
                                            if (pa.mPref.isSuperset(query)) {
                                                PreferredActivity freshPa = new PreferredActivity(pa, pa.mPref.mMatch, pa.mPref.discardObsoleteComponents(query), pa.mPref.mComponent, pa.mPref.mAlways);
                                                pir.removeFilter(pa);
                                                pir.addFilter(freshPa);
                                                changed = true;
                                            } else {
                                                Slog.i(TAG, "Result set changed, dropping preferred activity for " + intent + " type " + resolvedType);
                                                if (this.mIsDefaultGoogleCalendar) {
                                                    this.mIsDefaultPreferredActivityChanged = true;
                                                }
                                                pir.removeFilter(pa);
                                                pir.addFilter(new PreferredActivity(pa, pa.mPref.mMatch, null, pa.mPref.mComponent, false));
                                                if (true) {
                                                    scheduleWritePackageRestrictionsLocked(userId);
                                                }
                                                return null;
                                            }
                                        }
                                    }
                                    if (debug) {
                                        Slog.v(TAG, "Returning preferred activity: " + ri.activityInfo.packageName + "/" + ri.activityInfo.name);
                                    }
                                    if (this.mIsDefaultGoogleCalendar && ri.activityInfo.packageName != null && "com.android.calendar".equals(ri.activityInfo.packageName)) {
                                        Log.i(TAG, "break huawei calendar, set default calendar is google calendar");
                                    } else if (changed) {
                                        scheduleWritePackageRestrictionsLocked(userId);
                                    }
                                }
                            }
                            continue;
                        }
                    } else if (debug) {
                        Slog.v(TAG, "Skipping mAlways=false entry");
                    }
                }
                if (changed) {
                    scheduleWritePackageRestrictionsLocked(userId);
                }
            }
        }
    }

    public boolean canForwardTo(Intent intent, String resolvedType, int sourceUserId, int targetUserId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        List<CrossProfileIntentFilter> matches = getMatchingCrossProfileIntentFilters(intent, resolvedType, sourceUserId);
        if (matches != null) {
            int size = matches.size();
            for (int i = 0; i < size; i++) {
                if (((CrossProfileIntentFilter) matches.get(i)).getTargetUserId() == targetUserId) {
                    return true;
                }
            }
        }
        if (!hasWebURI(intent)) {
            return false;
        }
        boolean z;
        int callingUid = Binder.getCallingUid();
        UserInfo parent = getProfileParent(sourceUserId);
        synchronized (this.mPackages) {
            z = getCrossProfileDomainPreferredLpr(intent, resolvedType, updateFlagsForResolve(0, parent.id, intent, callingUid, false), sourceUserId, parent.id) != null;
        }
        return z;
    }

    private UserInfo getProfileParent(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo profileParent = sUserManager.getProfileParent(userId);
            return profileParent;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private List<CrossProfileIntentFilter> getMatchingCrossProfileIntentFilters(Intent intent, String resolvedType, int userId) {
        CrossProfileIntentResolver resolver = (CrossProfileIntentResolver) this.mSettings.mCrossProfileIntentResolvers.get(userId);
        if (resolver != null) {
            return resolver.queryIntent(intent, resolvedType, false, userId);
        }
        return null;
    }

    public ParceledListSlice<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) {
        try {
            Trace.traceBegin(262144, "queryIntentActivities");
            ParceledListSlice<ResolveInfo> parceledListSlice = new ParceledListSlice(queryIntentActivitiesInternal(intent, resolvedType, flags, userId));
            Trace.traceEnd(262144);
            return parceledListSlice;
        } catch (Exception e) {
            Slog.e(TAG, "queryIntent : " + intent + " failed, calling from " + Binder.getCallingUid());
            throw e;
        } catch (Throwable th) {
            Trace.traceEnd(262144);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String getInstantAppPackageName(int callingUid) {
        String str = null;
        synchronized (this.mPackages) {
            if (Process.isIsolated(callingUid)) {
                callingUid = this.mIsolatedOwners.get(callingUid);
            }
            Object obj = this.mSettings.getUserIdLPr(UserHandle.getAppId(callingUid));
            if (obj instanceof PackageSetting) {
                PackageSetting ps = (PackageSetting) obj;
                if (ps.getInstantApp(UserHandle.getUserId(callingUid))) {
                    str = ps.pkg.packageName;
                }
            } else {
                return null;
            }
        }
    }

    protected List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, int flags, int userId) {
        return queryIntentActivitiesInternal(intent, resolvedType, flags, Binder.getCallingUid(), userId, false, true);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected List<ResolveInfo> queryIntentActivitiesInternal(Intent intent, String resolvedType, int flags, int filterCallingUid, int userId, boolean resolveForStart, boolean allowDynamicSplits) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        String instantAppPkgName = getInstantAppPackageName(filterCallingUid);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "query intent activities");
        String pkgName = intent.getPackage();
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        boolean z = (comp == null && pkgName == null) ? false : true;
        flags = updateFlagsForResolve(flags, userId, intent, filterCallingUid, resolveForStart, z);
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList(1);
            ActivityInfo ai = getActivityInfo(comp, flags, userId);
            if (ai != null) {
                boolean blockResolution;
                boolean matchInstantApp = (DumpState.DUMP_VOLUMES & flags) != 0;
                boolean matchVisibleToInstantAppOnly = (16777216 & flags) != 0;
                boolean matchExplicitlyVisibleOnly = (33554432 & flags) != 0;
                boolean isCallerInstantApp = instantAppPkgName != null;
                boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                boolean isTargetInstantApp = (ai.applicationInfo.privateFlags & 128) != 0;
                boolean isTargetVisibleToInstantApp = (ai.flags & 1048576) != 0;
                boolean isTargetExplicitlyVisibleToInstantApp = isTargetVisibleToInstantApp ? (ai.flags & DumpState.DUMP_COMPILER_STATS) == 0 : false;
                boolean z2 = isTargetVisibleToInstantApp ? matchExplicitlyVisibleOnly ? isTargetExplicitlyVisibleToInstantApp ^ 1 : false : true;
                if (isTargetSameInstantApp) {
                    blockResolution = false;
                } else if (!matchInstantApp && (isCallerInstantApp ^ 1) != 0 && isTargetInstantApp) {
                    blockResolution = true;
                } else if (matchVisibleToInstantAppOnly && isCallerInstantApp) {
                    blockResolution = z2;
                } else {
                    blockResolution = false;
                }
                if (!blockResolution) {
                    ResolveInfo ri = new ResolveInfo();
                    ri.activityInfo = ai;
                    list.add(ri);
                }
            }
            return applyPostResolutionFilter(list, instantAppPkgName, allowDynamicSplits, filterCallingUid, userId);
        }
        boolean sortResult = false;
        int i = 0;
        boolean ephemeralDisabled = isEphemeralDisabled();
        synchronized (this.mPackages) {
            List result;
            if (pkgName == null) {
                List<CrossProfileIntentFilter> matchingFilters = getMatchingCrossProfileIntentFilters(intent, resolvedType, userId);
                ResolveInfo xpResolveInfo = querySkipCurrentProfileIntents(matchingFilters, intent, resolvedType, flags, userId);
                List<ResolveInfo> applyPostResolutionFilter;
                if (xpResolveInfo != null) {
                    List<ResolveInfo> arrayList = new ArrayList(1);
                    arrayList.add(xpResolveInfo);
                    applyPostResolutionFilter = applyPostResolutionFilter(filterIfNotSystemUser(arrayList, userId), instantAppPkgName, allowDynamicSplits, filterCallingUid, userId);
                    return applyPostResolutionFilter;
                }
                result = filterIfNotSystemUser(this.mActivities.queryIntent(intent, resolvedType, flags, userId), userId);
                if (ephemeralDisabled) {
                    i = 0;
                } else {
                    i = isInstantAppAllowed(intent, result, userId, false);
                }
                xpResolveInfo = queryCrossProfileIntents(matchingFilters, intent, resolvedType, flags, userId, hasNonNegativePriority(result));
                if (xpResolveInfo != null) {
                    if (isUserEnabled(xpResolveInfo.targetUserId)) {
                        if (filterIfNotSystemUser(Collections.singletonList(xpResolveInfo), userId).size() > 0) {
                            result.add(xpResolveInfo);
                            sortResult = true;
                        }
                    }
                }
                if (hasWebURI(intent)) {
                    CrossProfileDomainInfo crossProfileDomainInfo = null;
                    UserInfo parent = getProfileParent(userId);
                    if (parent != null) {
                        crossProfileDomainInfo = getCrossProfileDomainPreferredLpr(intent, resolvedType, flags, userId, parent.id);
                    }
                    if (crossProfileDomainInfo != null) {
                        if (xpResolveInfo != null) {
                            result.remove(xpResolveInfo);
                        }
                        if (result.size() == 0 && (r25 ^ 1) != 0) {
                            result.add(crossProfileDomainInfo.resolveInfo);
                            applyPostResolutionFilter = applyPostResolutionFilter(result, instantAppPkgName, allowDynamicSplits, filterCallingUid, userId);
                            return applyPostResolutionFilter;
                        }
                    } else if (result.size() <= 1 && (r25 ^ 1) != 0) {
                        applyPostResolutionFilter = applyPostResolutionFilter(result, instantAppPkgName, allowDynamicSplits, filterCallingUid, userId);
                        return applyPostResolutionFilter;
                    }
                    result = filterCandidatesWithDomainPreferredActivitiesLPr(intent, flags, result, crossProfileDomainInfo, userId);
                    sortResult = true;
                }
            } else {
                Package pkg = (Package) this.mPackages.get(pkgName);
                result = null;
                if (pkg != null) {
                    result = filterIfNotSystemUser(this.mActivities.queryIntentForPackage(intent, resolvedType, flags, pkg.activities, userId), userId);
                }
                if (result == null || result.size() == 0) {
                    if (ephemeralDisabled) {
                        i = 0;
                    } else {
                        i = isInstantAppAllowed(intent, null, userId, true);
                    }
                    if (result == null) {
                        result = new ArrayList();
                    }
                }
            }
        }
    }

    private List<ResolveInfo> maybeAddInstantAppInstaller(List<ResolveInfo> result, Intent intent, String resolvedType, int flags, int userId, boolean resolveForStart) {
        PackageSetting ps;
        ResolveInfo localInstantApp = null;
        boolean blockResolution = false;
        if (!((DumpState.DUMP_VOLUMES & flags) != 0)) {
            List<ResolveInfo> instantApps = this.mActivities.queryIntent(intent, resolvedType, ((flags | 64) | DumpState.DUMP_VOLUMES) | 16777216, userId);
            int i = instantApps.size() - 1;
            while (i >= 0) {
                ResolveInfo info = (ResolveInfo) instantApps.get(i);
                String packageName = info.activityInfo.packageName;
                ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (ps.getInstantApp(userId)) {
                    long packedStatus = getDomainVerificationStatusLPr(ps, userId);
                    int linkGeneration = (int) (-1 & packedStatus);
                    if (((int) (packedStatus >> 32)) == 3) {
                        if (DEBUG_EPHEMERAL) {
                            Slog.v(TAG, "Instant app marked to never run; pkg: " + packageName);
                        }
                        blockResolution = true;
                    } else {
                        if (DEBUG_EPHEMERAL) {
                            Slog.v(TAG, "Found installed instant app; pkg: " + packageName);
                        }
                        localInstantApp = info;
                    }
                } else {
                    i--;
                }
            }
        }
        AuxiliaryResolveInfo auxiliaryResponse = null;
        if (!blockResolution) {
            if (localInstantApp == null) {
                Trace.traceBegin(262144, "resolveEphemeral");
                auxiliaryResponse = InstantAppResolver.doInstantAppResolutionPhaseOne(this.mContext, this.mInstantAppResolverConnection, new InstantAppRequest(null, intent, resolvedType, null, userId, null, resolveForStart));
                Trace.traceEnd(262144);
            } else {
                ApplicationInfo ai = localInstantApp.activityInfo.applicationInfo;
                auxiliaryResponse = new AuxiliaryResolveInfo(ai.packageName, null, null, ai.versionCode, null);
            }
        }
        if (auxiliaryResponse != null) {
            if (DEBUG_EPHEMERAL) {
                Slog.v(TAG, "Adding ephemeral installer to the ResolveInfo list");
            }
            ResolveInfo ephemeralInstaller = new ResolveInfo(this.mInstantAppInstallerInfo);
            ps = (PackageSetting) this.mSettings.mPackages.get(this.mInstantAppInstallerActivity.packageName);
            if (ps != null) {
                ephemeralInstaller.activityInfo = PackageParser.generateActivityInfo(this.mInstantAppInstallerActivity, 0, ps.readUserState(userId), userId);
                ephemeralInstaller.activityInfo.launchToken = auxiliaryResponse.token;
                ephemeralInstaller.auxiliaryInfo = auxiliaryResponse;
                ephemeralInstaller.isDefault = true;
                ephemeralInstaller.match = 5799936;
                ephemeralInstaller.filter = new IntentFilter(intent.getAction());
                ephemeralInstaller.filter.addDataPath(intent.getData().getPath(), 0);
                ephemeralInstaller.isInstantAppAvailable = true;
                result.add(ephemeralInstaller);
            }
        }
        return result;
    }

    private CrossProfileDomainInfo getCrossProfileDomainPreferredLpr(Intent intent, String resolvedType, int flags, int sourceUserId, int parentUserId) {
        if (!sUserManager.hasUserRestriction("allow_parent_profile_app_linking", sourceUserId)) {
            return null;
        }
        List<ResolveInfo> resultTargetUser = this.mActivities.queryIntent(intent, resolvedType, flags, parentUserId);
        if (resultTargetUser == null || resultTargetUser.isEmpty()) {
            return null;
        }
        CrossProfileDomainInfo result = null;
        int size = resultTargetUser.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo riTargetUser = (ResolveInfo) resultTargetUser.get(i);
            if (!riTargetUser.handleAllWebDataURI) {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(riTargetUser.activityInfo.packageName);
                if (ps != null) {
                    int status = (int) (getDomainVerificationStatusLPr(ps, parentUserId) >> 32);
                    if (result == null) {
                        result = new CrossProfileDomainInfo();
                        result.resolveInfo = createForwardingResolveInfoUnchecked(new IntentFilter(), sourceUserId, parentUserId);
                        result.bestDomainVerificationStatus = status;
                    } else {
                        result.bestDomainVerificationStatus = bestDomainVerificationStatus(status, result.bestDomainVerificationStatus);
                    }
                }
            }
        }
        if (result == null || result.bestDomainVerificationStatus != 3) {
            return result;
        }
        return null;
    }

    private int bestDomainVerificationStatus(int status1, int status2) {
        if (status1 == 3) {
            return status2;
        }
        if (status2 == 3) {
            return status1;
        }
        return (int) MathUtils.max(status1, status2);
    }

    private boolean isUserEnabled(int userId) {
        long callingId = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = sUserManager.getUserInfo(userId);
            boolean isEnabled = userInfo != null ? userInfo.isEnabled() : false;
            Binder.restoreCallingIdentity(callingId);
            return isEnabled;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private List<ResolveInfo> filterIfNotSystemUser(List<ResolveInfo> resolveInfos, int userId) {
        if (userId == 0) {
            return resolveInfos;
        }
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            if ((((ResolveInfo) resolveInfos.get(i)).activityInfo.flags & 536870912) != 0) {
                resolveInfos.remove(i);
            }
        }
        return resolveInfos;
    }

    private List<ResolveInfo> applyPostResolutionFilter(List<ResolveInfo> resolveInfos, String ephemeralPkgName, boolean allowDynamicSplits, int filterCallingUid, int userId) {
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            ResolveInfo info = (ResolveInfo) resolveInfos.get(i);
            if (!allowDynamicSplits || info.activityInfo.splitName == null || (ArrayUtils.contains(info.activityInfo.applicationInfo.splitNames, info.activityInfo.splitName) ^ 1) == 0) {
                if (!(ephemeralPkgName == null || ephemeralPkgName.equals(info.activityInfo.packageName) || (!info.activityInfo.applicationInfo.isInstantApp() && (info.activityInfo.flags & 1048576) != 0))) {
                    resolveInfos.remove(i);
                }
            } else if (this.mInstantAppInstallerInfo == null) {
                resolveInfos.remove(i);
            } else {
                ResolveInfo installerInfo = new ResolveInfo(this.mInstantAppInstallerInfo);
                installerInfo.auxiliaryInfo = new AuxiliaryResolveInfo(info.activityInfo.packageName, info.activityInfo.splitName, findInstallFailureActivity(info.activityInfo.packageName, filterCallingUid, userId), info.activityInfo.applicationInfo.versionCode, null);
                installerInfo.match = 5799936;
                installerInfo.filter = new IntentFilter();
                installerInfo.resolvePackageName = info.getComponentInfo().packageName;
                installerInfo.labelRes = info.resolveLabelResId();
                installerInfo.icon = info.resolveIconResId();
                installerInfo.priority = info.priority;
                installerInfo.preferredOrder = info.preferredOrder;
                installerInfo.isDefault = info.isDefault;
                resolveInfos.set(i, installerInfo);
            }
        }
        return resolveInfos;
    }

    private ComponentName findInstallFailureActivity(String packageName, int filterCallingUid, int userId) {
        Intent failureActivityIntent = new Intent("android.intent.action.INSTALL_FAILURE");
        failureActivityIntent.setPackage(packageName);
        List<ResolveInfo> result = queryIntentActivitiesInternal(failureActivityIntent, null, 0, filterCallingUid, userId, false, false);
        int NR = result.size();
        if (NR > 0) {
            for (int i = 0; i < NR; i++) {
                ResolveInfo info = (ResolveInfo) result.get(i);
                if (info.activityInfo.splitName == null) {
                    return new ComponentName(packageName, info.activityInfo.name);
                }
            }
        }
        return null;
    }

    private boolean hasNonNegativePriority(List<ResolveInfo> resolveInfos) {
        return resolveInfos.size() > 0 && ((ResolveInfo) resolveInfos.get(0)).priority >= 0;
    }

    private static boolean hasWebURI(Intent intent) {
        if (intent.getData() == null) {
            return false;
        }
        String scheme = intent.getScheme();
        if (TextUtils.isEmpty(scheme)) {
            return false;
        }
        return !scheme.equals("http") ? scheme.equals("https") : true;
    }

    private List<ResolveInfo> filterCandidatesWithDomainPreferredActivitiesLPr(Intent intent, int matchFlags, List<ResolveInfo> candidates, CrossProfileDomainInfo xpDomainInfo, int userId) {
        boolean debug = (intent.getFlags() & 8) != 0;
        ArrayList<ResolveInfo> result = new ArrayList();
        ArrayList<ResolveInfo> alwaysList = new ArrayList();
        ArrayList<ResolveInfo> undefinedList = new ArrayList();
        ArrayList<ResolveInfo> alwaysAskList = new ArrayList();
        ArrayList<ResolveInfo> neverList = new ArrayList();
        ArrayList<ResolveInfo> matchAllList = new ArrayList();
        synchronized (this.mPackages) {
            int n;
            int count = candidates.size();
            for (n = 0; n < count; n++) {
                ResolveInfo info = (ResolveInfo) candidates.get(n);
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(info.activityInfo.packageName);
                if (ps != null) {
                    if (info.handleAllWebDataURI) {
                        matchAllList.add(info);
                    } else {
                        long packedStatus = getDomainVerificationStatusLPr(ps, userId);
                        int status = (int) (packedStatus >> 32);
                        int linkGeneration = (int) (-1 & packedStatus);
                        if (status == 2) {
                            if (debug) {
                                Slog.i(TAG, "  + always: " + info.activityInfo.packageName + " : linkgen=" + linkGeneration);
                            }
                            info.preferredOrder = linkGeneration;
                            alwaysList.add(info);
                        } else if (status == 3) {
                            if (debug) {
                                Slog.i(TAG, "  + never: " + info.activityInfo.packageName);
                            }
                            neverList.add(info);
                        } else if (status == 4) {
                            if (debug) {
                                Slog.i(TAG, "  + always-ask: " + info.activityInfo.packageName);
                            }
                            alwaysAskList.add(info);
                        } else if (status == 0 || status == 1) {
                            if (debug) {
                                Slog.i(TAG, "  + ask: " + info.activityInfo.packageName);
                            }
                            undefinedList.add(info);
                        }
                    }
                }
            }
            boolean includeBrowser = false;
            if (alwaysList.size() > 0) {
                result.addAll(alwaysList);
            } else {
                result.addAll(undefinedList);
                if (!(xpDomainInfo == null || xpDomainInfo.bestDomainVerificationStatus == 3)) {
                    result.add(xpDomainInfo.resolveInfo);
                }
                includeBrowser = true;
            }
            if (alwaysAskList.size() > 0) {
                for (ResolveInfo i : result) {
                    i.preferredOrder = 0;
                }
                result.addAll(alwaysAskList);
                includeBrowser = true;
            }
            if (includeBrowser) {
                if ((131072 & matchFlags) != 0) {
                    result.addAll(matchAllList);
                } else {
                    String defaultBrowserPackageName = getDefaultBrowserPackageName(userId);
                    int maxMatchPrio = 0;
                    ResolveInfo defaultBrowserMatch = null;
                    int numCandidates = matchAllList.size();
                    for (n = 0; n < numCandidates; n++) {
                        info = (ResolveInfo) matchAllList.get(n);
                        if (info.priority > maxMatchPrio) {
                            maxMatchPrio = info.priority;
                        }
                        if (info.activityInfo.packageName.equals(defaultBrowserPackageName) && (defaultBrowserMatch == null || defaultBrowserMatch.priority < info.priority)) {
                            if (debug) {
                                Slog.v(TAG, "Considering default browser match " + info);
                            }
                            defaultBrowserMatch = info;
                        }
                    }
                    if (defaultBrowserMatch == null || defaultBrowserMatch.priority < maxMatchPrio || (TextUtils.isEmpty(defaultBrowserPackageName) ^ 1) == 0) {
                        result.addAll(matchAllList);
                    } else {
                        if (debug) {
                            Slog.v(TAG, "Default browser match " + defaultBrowserMatch);
                        }
                        result.add(defaultBrowserMatch);
                    }
                }
                if (result.size() == 0) {
                    result.addAll(candidates);
                    result.removeAll(neverList);
                }
            }
        }
        return result;
    }

    private long getDomainVerificationStatusLPr(PackageSetting ps, int userId) {
        long result = ps.getDomainVerificationStatusForUser(userId);
        if ((result >> 32) != 0 || ps.getIntentFilterVerificationInfo() == null) {
            return result;
        }
        return ((long) ps.getIntentFilterVerificationInfo().getStatus()) << 32;
    }

    private ResolveInfo querySkipCurrentProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, int flags, int sourceUserId) {
        if (matchingFilters != null) {
            int size = matchingFilters.size();
            for (int i = 0; i < size; i++) {
                CrossProfileIntentFilter filter = (CrossProfileIntentFilter) matchingFilters.get(i);
                if ((filter.getFlags() & 2) != 0) {
                    ResolveInfo resolveInfo = createForwardingResolveInfo(filter, intent, resolvedType, flags, sourceUserId);
                    if (resolveInfo != null) {
                        return resolveInfo;
                    }
                }
            }
        }
        return null;
    }

    private ResolveInfo queryCrossProfileIntents(List<CrossProfileIntentFilter> matchingFilters, Intent intent, String resolvedType, int flags, int sourceUserId, boolean matchInCurrentProfile) {
        if (matchingFilters != null) {
            SparseBooleanArray alreadyTriedUserIds = new SparseBooleanArray();
            int size = matchingFilters.size();
            for (int i = 0; i < size; i++) {
                CrossProfileIntentFilter filter = (CrossProfileIntentFilter) matchingFilters.get(i);
                int targetUserId = filter.getTargetUserId();
                boolean skipCurrentProfile = (filter.getFlags() & 2) != 0;
                boolean skipCurrentProfileIfNoMatchFound = (filter.getFlags() & 4) != 0;
                if (!(skipCurrentProfile || (alreadyTriedUserIds.get(targetUserId) ^ 1) == 0 || (skipCurrentProfileIfNoMatchFound && (matchInCurrentProfile ^ 1) == 0))) {
                    ResolveInfo resolveInfo = createForwardingResolveInfo(filter, intent, resolvedType, flags, sourceUserId);
                    if (resolveInfo != null) {
                        return resolveInfo;
                    }
                    alreadyTriedUserIds.put(targetUserId, true);
                }
            }
        }
        return null;
    }

    private ResolveInfo createForwardingResolveInfo(CrossProfileIntentFilter filter, Intent intent, String resolvedType, int flags, int sourceUserId) {
        int targetUserId = filter.getTargetUserId();
        List<ResolveInfo> resultTargetUser = this.mActivities.queryIntent(intent, resolvedType, flags, targetUserId);
        if (resultTargetUser != null && isUserEnabled(targetUserId)) {
            for (int i = resultTargetUser.size() - 1; i >= 0; i--) {
                if ((((ResolveInfo) resultTargetUser.get(i)).activityInfo.applicationInfo.flags & 1073741824) == 0) {
                    return createForwardingResolveInfoUnchecked(filter, sourceUserId, targetUserId);
                }
            }
        }
        return null;
    }

    private ResolveInfo createForwardingResolveInfoUnchecked(IntentFilter filter, int sourceUserId, int targetUserId) {
        ResolveInfo forwardingResolveInfo = new ResolveInfo();
        long ident = Binder.clearCallingIdentity();
        try {
            String className;
            boolean targetIsProfile = sUserManager.getUserInfo(targetUserId).isManagedProfile();
            if (targetIsProfile) {
                className = IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE;
            } else {
                className = IntentForwarderActivity.FORWARD_INTENT_TO_PARENT;
            }
            ActivityInfo forwardingActivityInfo = getActivityInfo(new ComponentName(this.mAndroidApplication.packageName, className), 0, sourceUserId);
            if (!targetIsProfile) {
                forwardingActivityInfo.showUserIcon = targetUserId;
                forwardingResolveInfo.noResourceId = true;
            }
            forwardingResolveInfo.activityInfo = forwardingActivityInfo;
            forwardingResolveInfo.priority = 0;
            forwardingResolveInfo.preferredOrder = 0;
            forwardingResolveInfo.match = 0;
            forwardingResolveInfo.isDefault = true;
            forwardingResolveInfo.filter = filter;
            forwardingResolveInfo.targetUserId = targetUserId;
            return forwardingResolveInfo;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public ParceledListSlice<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice(queryIntentActivityOptionsInternal(caller, specifics, specificTypes, intent, resolvedType, flags, userId));
    }

    private List<ResolveInfo> queryIntentActivityOptionsInternal(ComponentName caller, Intent[] specifics, String[] specificTypes, Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int i;
        String action;
        int N;
        int j;
        int callingUid = Binder.getCallingUid();
        flags = updateFlagsForResolve(flags, userId, intent, callingUid, false);
        enforceCrossUserPermission(callingUid, userId, false, false, "query intent activity options");
        String resultsAction = intent.getAction();
        List<ResolveInfo> results = queryIntentActivitiesInternal(intent, resolvedType, flags | 64, userId);
        int specificsPos = 0;
        if (specifics != null) {
            i = 0;
            while (i < specifics.length) {
                Intent sintent = specifics[i];
                if (sintent != null) {
                    ActivityInfo ai;
                    action = sintent.getAction();
                    if (resultsAction != null && resultsAction.equals(action)) {
                        action = null;
                    }
                    ResolveInfo resolveInfo = null;
                    ComponentName comp = sintent.getComponent();
                    if (comp == null) {
                        resolveInfo = resolveIntent(sintent, specificTypes != null ? specificTypes[i] : null, flags, userId);
                        if (resolveInfo != null) {
                            ResolveInfo resolveInfo2 = this.mResolveInfo;
                            ai = resolveInfo.activityInfo;
                            ComponentName componentName = new ComponentName(ai.applicationInfo.packageName, ai.name);
                        }
                    } else {
                        ai = getActivityInfo(comp, flags, userId);
                        if (ai == null) {
                        }
                    }
                    N = results.size();
                    j = specificsPos;
                    while (j < N) {
                        ResolveInfo sri = (ResolveInfo) results.get(j);
                        if ((sri.activityInfo.name.equals(comp.getClassName()) && sri.activityInfo.applicationInfo.packageName.equals(comp.getPackageName())) || (r14 != null && sri.filter.matchAction(r14))) {
                            results.remove(j);
                            if (resolveInfo == null) {
                                resolveInfo = sri;
                            }
                            j--;
                            N--;
                        }
                        j++;
                    }
                    if (resolveInfo == null) {
                        resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = ai;
                    }
                    results.add(specificsPos, resolveInfo);
                    resolveInfo.specificIndex = i;
                    specificsPos++;
                }
                i++;
            }
        }
        N = results.size();
        for (i = specificsPos; i < N - 1; i++) {
            ResolveInfo rii = (ResolveInfo) results.get(i);
            if (rii.filter != null) {
                Iterator<String> it = rii.filter.actionsIterator();
                if (it != null) {
                    while (it.hasNext()) {
                        action = (String) it.next();
                        if (resultsAction == null || !resultsAction.equals(action)) {
                            j = i + 1;
                            while (j < N) {
                                ResolveInfo rij = (ResolveInfo) results.get(j);
                                if (rij.filter != null && rij.filter.hasAction(action)) {
                                    results.remove(j);
                                    j--;
                                    N--;
                                }
                                j++;
                            }
                        }
                    }
                    if ((flags & 64) == 0) {
                        rii.filter = null;
                    }
                }
            }
        }
        if (caller != null) {
            N = results.size();
            for (i = 0; i < N; i++) {
                ActivityInfo ainfo = ((ResolveInfo) results.get(i)).activityInfo;
                if (caller.getPackageName().equals(ainfo.applicationInfo.packageName) && caller.getClassName().equals(ainfo.name)) {
                    results.remove(i);
                    break;
                }
            }
        }
        if ((flags & 64) == 0) {
            N = results.size();
            for (i = 0; i < N; i++) {
                ((ResolveInfo) results.get(i)).filter = null;
            }
        }
        return results;
    }

    public ParceledListSlice<ResolveInfo> queryIntentReceivers(Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice(queryIntentReceiversInternal(intent, resolvedType, flags, userId, false));
    }

    private List<ResolveInfo> queryIntentReceiversInternal(Intent intent, String resolvedType, int flags, int userId, boolean allowDynamicSplits) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, false, false, "query intent receivers");
        String instantAppPkgName = getInstantAppPackageName(callingUid);
        flags = updateFlagsForResolve(flags, userId, intent, callingUid, false);
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> list = new ArrayList(1);
            ActivityInfo ai = getReceiverInfo(comp, flags, userId);
            if (ai != null) {
                boolean blockResolution;
                boolean matchInstantApp = (DumpState.DUMP_VOLUMES & flags) != 0;
                boolean matchVisibleToInstantAppOnly = (16777216 & flags) != 0;
                boolean matchExplicitlyVisibleOnly = (33554432 & flags) != 0;
                boolean isCallerInstantApp = instantAppPkgName != null;
                boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                boolean isTargetInstantApp = (ai.applicationInfo.privateFlags & 128) != 0;
                boolean isTargetVisibleToInstantApp = (ai.flags & 1048576) != 0;
                boolean isTargetExplicitlyVisibleToInstantApp = isTargetVisibleToInstantApp ? (ai.flags & DumpState.DUMP_COMPILER_STATS) == 0 : false;
                boolean z = isTargetVisibleToInstantApp ? matchExplicitlyVisibleOnly ? isTargetExplicitlyVisibleToInstantApp ^ 1 : false : true;
                if (isTargetSameInstantApp) {
                    blockResolution = false;
                } else if (!matchInstantApp && (isCallerInstantApp ^ 1) != 0 && isTargetInstantApp) {
                    blockResolution = true;
                } else if (matchVisibleToInstantAppOnly && isCallerInstantApp) {
                    blockResolution = z;
                } else {
                    blockResolution = false;
                }
                if (!blockResolution) {
                    ResolveInfo ri = new ResolveInfo();
                    ri.activityInfo = ai;
                    list.add(ri);
                }
            }
            return applyPostResolutionFilter(list, instantAppPkgName, allowDynamicSplits, callingUid, userId);
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                List<ResolveInfo> applyPostResolutionFilter = applyPostResolutionFilter(this.mReceivers.queryIntent(intent, resolvedType, flags, userId), instantAppPkgName, allowDynamicSplits, callingUid, userId);
                return applyPostResolutionFilter;
            }
            Package pkg = (Package) this.mPackages.get(pkgName);
            if (pkg != null) {
                applyPostResolutionFilter = applyPostResolutionFilter(this.mReceivers.queryIntentForPackage(intent, resolvedType, flags, pkg.receivers, userId), instantAppPkgName, allowDynamicSplits, callingUid, userId);
                return applyPostResolutionFilter;
            }
            applyPostResolutionFilter = Collections.emptyList();
            return applyPostResolutionFilter;
        }
    }

    public ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId) {
        return resolveServiceInternal(intent, resolvedType, flags, userId, Binder.getCallingUid());
    }

    private ResolveInfo resolveServiceInternal(Intent intent, String resolvedType, int flags, int userId, int callingUid) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        List<ResolveInfo> query = queryIntentServicesInternal(intent, resolvedType, updateFlagsForResolve(flags, userId, intent, callingUid, false), userId, callingUid, false);
        if (query == null || query.size() < 1) {
            return null;
        }
        return (ResolveInfo) query.get(0);
    }

    public ParceledListSlice<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice(queryIntentServicesInternal(intent, resolvedType, flags, userId, Binder.getCallingUid(), false));
    }

    private List<ResolveInfo> queryIntentServicesInternal(Intent intent, String resolvedType, int flags, int userId, int callingUid, boolean includeInstantApps) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        enforceCrossUserPermission(callingUid, userId, false, false, "query intent receivers");
        String instantAppPkgName = getInstantAppPackageName(callingUid);
        flags = updateFlagsForResolve(flags, userId, intent, callingUid, includeInstantApps);
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> arrayList = new ArrayList(1);
            ServiceInfo si = getServiceInfo(comp, flags, userId);
            if (si != null) {
                boolean blockResolution;
                boolean matchInstantApp = (DumpState.DUMP_VOLUMES & flags) != 0;
                boolean matchVisibleToInstantAppOnly = (16777216 & flags) != 0;
                boolean isCallerInstantApp = instantAppPkgName != null;
                boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                boolean isTargetInstantApp = (si.applicationInfo.privateFlags & 128) != 0;
                boolean isTargetHiddenFromInstantApp = (si.flags & 1048576) == 0;
                if (isTargetSameInstantApp) {
                    blockResolution = false;
                } else if (!matchInstantApp && (isCallerInstantApp ^ 1) != 0 && isTargetInstantApp) {
                    blockResolution = true;
                } else if (matchVisibleToInstantAppOnly && isCallerInstantApp) {
                    blockResolution = isTargetHiddenFromInstantApp;
                } else {
                    blockResolution = false;
                }
                if (!blockResolution) {
                    ResolveInfo ri = new ResolveInfo();
                    ri.serviceInfo = si;
                    arrayList.add(ri);
                }
            }
            return arrayList;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                List<ResolveInfo> applyPostServiceResolutionFilter = applyPostServiceResolutionFilter(this.mServices.queryIntent(intent, resolvedType, flags, userId), instantAppPkgName);
                return applyPostServiceResolutionFilter;
            }
            Package pkg = (Package) this.mPackages.get(pkgName);
            if (pkg != null) {
                applyPostServiceResolutionFilter = applyPostServiceResolutionFilter(this.mServices.queryIntentForPackage(intent, resolvedType, flags, pkg.services, userId), instantAppPkgName);
                return applyPostServiceResolutionFilter;
            }
            applyPostServiceResolutionFilter = Collections.emptyList();
            return applyPostServiceResolutionFilter;
        }
    }

    private List<ResolveInfo> applyPostServiceResolutionFilter(List<ResolveInfo> resolveInfos, String instantAppPkgName) {
        if (instantAppPkgName == null) {
            return resolveInfos;
        }
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            ResolveInfo info = (ResolveInfo) resolveInfos.get(i);
            boolean isEphemeralApp = info.serviceInfo.applicationInfo.isInstantApp();
            if (isEphemeralApp && instantAppPkgName.equals(info.serviceInfo.packageName)) {
                if (!(info.serviceInfo.splitName == null || (ArrayUtils.contains(info.serviceInfo.applicationInfo.splitNames, info.serviceInfo.splitName) ^ 1) == 0)) {
                    if (DEBUG_EPHEMERAL) {
                        Slog.v(TAG, "Adding ephemeral installer to the ResolveInfo list");
                    }
                    ResolveInfo installerInfo = new ResolveInfo(this.mInstantAppInstallerInfo);
                    installerInfo.auxiliaryInfo = new AuxiliaryResolveInfo(info.serviceInfo.packageName, info.serviceInfo.splitName, null, info.serviceInfo.applicationInfo.versionCode, null);
                    installerInfo.isDefault = true;
                    installerInfo.match = 5799936;
                    installerInfo.filter = new IntentFilter();
                    installerInfo.resolvePackageName = info.getComponentInfo().packageName;
                    resolveInfos.set(i, installerInfo);
                }
            } else if (isEphemeralApp || (info.serviceInfo.flags & 1048576) == 0) {
                resolveInfos.remove(i);
            }
        }
        return resolveInfos;
    }

    public ParceledListSlice<ResolveInfo> queryIntentContentProviders(Intent intent, String resolvedType, int flags, int userId) {
        return new ParceledListSlice(queryIntentContentProvidersInternal(intent, resolvedType, flags, userId));
    }

    private List<ResolveInfo> queryIntentContentProvidersInternal(Intent intent, String resolvedType, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return Collections.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        String instantAppPkgName = getInstantAppPackageName(callingUid);
        flags = updateFlagsForResolve(flags, userId, intent, callingUid, false);
        ComponentName comp = intent.getComponent();
        if (comp == null && intent.getSelector() != null) {
            intent = intent.getSelector();
            comp = intent.getComponent();
        }
        if (comp != null) {
            List<ResolveInfo> arrayList = new ArrayList(1);
            ProviderInfo pi = getProviderInfo(comp, flags, userId);
            if (pi != null) {
                boolean blockResolution;
                boolean matchInstantApp = (DumpState.DUMP_VOLUMES & flags) != 0;
                boolean matchVisibleToInstantAppOnly = (16777216 & flags) != 0;
                boolean isCallerInstantApp = instantAppPkgName != null;
                boolean isTargetSameInstantApp = comp.getPackageName().equals(instantAppPkgName);
                boolean isTargetInstantApp = (pi.applicationInfo.privateFlags & 128) != 0;
                boolean isTargetHiddenFromInstantApp = (pi.flags & 1048576) == 0;
                if (isTargetSameInstantApp) {
                    blockResolution = false;
                } else if (!matchInstantApp && (isCallerInstantApp ^ 1) != 0 && isTargetInstantApp) {
                    blockResolution = true;
                } else if (matchVisibleToInstantAppOnly && isCallerInstantApp) {
                    blockResolution = isTargetHiddenFromInstantApp;
                } else {
                    blockResolution = false;
                }
                if (!blockResolution) {
                    ResolveInfo ri = new ResolveInfo();
                    ri.providerInfo = pi;
                    arrayList.add(ri);
                }
            }
            return arrayList;
        }
        synchronized (this.mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                List<ResolveInfo> applyPostContentProviderResolutionFilter = applyPostContentProviderResolutionFilter(this.mProviders.queryIntent(intent, resolvedType, flags, userId), instantAppPkgName);
                return applyPostContentProviderResolutionFilter;
            }
            Package pkg = (Package) this.mPackages.get(pkgName);
            if (pkg != null) {
                applyPostContentProviderResolutionFilter = applyPostContentProviderResolutionFilter(this.mProviders.queryIntentForPackage(intent, resolvedType, flags, pkg.providers, userId), instantAppPkgName);
                return applyPostContentProviderResolutionFilter;
            }
            applyPostContentProviderResolutionFilter = Collections.emptyList();
            return applyPostContentProviderResolutionFilter;
        }
    }

    private List<ResolveInfo> applyPostContentProviderResolutionFilter(List<ResolveInfo> resolveInfos, String instantAppPkgName) {
        if (instantAppPkgName == null) {
            return resolveInfos;
        }
        for (int i = resolveInfos.size() - 1; i >= 0; i--) {
            ResolveInfo info = (ResolveInfo) resolveInfos.get(i);
            boolean isEphemeralApp = info.providerInfo.applicationInfo.isInstantApp();
            if (isEphemeralApp && instantAppPkgName.equals(info.providerInfo.packageName)) {
                if (!(info.providerInfo.splitName == null || (ArrayUtils.contains(info.providerInfo.applicationInfo.splitNames, info.providerInfo.splitName) ^ 1) == 0)) {
                    if (DEBUG_EPHEMERAL) {
                        Slog.v(TAG, "Adding ephemeral installer to the ResolveInfo list");
                    }
                    ResolveInfo installerInfo = new ResolveInfo(this.mInstantAppInstallerInfo);
                    installerInfo.auxiliaryInfo = new AuxiliaryResolveInfo(info.providerInfo.packageName, info.providerInfo.splitName, null, info.providerInfo.applicationInfo.versionCode, null);
                    installerInfo.isDefault = true;
                    installerInfo.match = 5799936;
                    installerInfo.filter = new IntentFilter();
                    installerInfo.resolvePackageName = info.getComponentInfo().packageName;
                    resolveInfos.set(i, installerInfo);
                }
            } else if (isEphemeralApp || (info.providerInfo.flags & 1048576) == 0) {
                resolveInfos.remove(i);
            }
        }
        return resolveInfos;
    }

    public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return ParceledListSlice.emptyList();
        }
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        ParceledListSlice<PackageInfo> parceledListSlice;
        flags = updateFlagsForPackage(flags, userId, null);
        boolean listUninstalled = (4202496 & flags) != 0;
        enforceCrossUserPermission(callingUid, userId, true, false, "get installed packages");
        synchronized (this.mPackages) {
            ArrayList<PackageInfo> list;
            PackageSetting ps;
            PackageInfo pi;
            if (listUninstalled) {
                list = new ArrayList(this.mSettings.mPackages.size());
                for (PackageSetting ps2 : this.mSettings.mPackages.values()) {
                    if (!(filterSharedLibPackageLPr(ps2, callingUid, userId, flags) || filterAppAccessLPr(ps2, callingUid, userId))) {
                        pi = generatePackageInfo(ps2, flags, userId);
                        if (pi != null) {
                            list.add(pi);
                        }
                    }
                }
            } else {
                list = new ArrayList(this.mPackages.size());
                for (Package p : this.mPackages.values()) {
                    ps2 = (PackageSetting) p.mExtras;
                    if (!(filterSharedLibPackageLPr(ps2, callingUid, userId, flags) || filterAppAccessLPr(ps2, callingUid, userId))) {
                        pi = generatePackageInfo((PackageSetting) p.mExtras, flags, userId);
                        if (pi != null) {
                            list.add(pi);
                        }
                    }
                }
            }
            parceledListSlice = new ParceledListSlice(list);
        }
        return parceledListSlice;
    }

    private void addPackageHoldingPermissions(ArrayList<PackageInfo> list, PackageSetting ps, String[] permissions, boolean[] tmp, int flags, int userId) {
        int i;
        int numMatch = 0;
        PermissionsState permissionsState = ps.getPermissionsState();
        for (i = 0; i < permissions.length; i++) {
            if (permissionsState.hasPermission(permissions[i], userId)) {
                tmp[i] = true;
                numMatch++;
            } else {
                tmp[i] = false;
            }
        }
        if (numMatch != 0) {
            PackageInfo pi = generatePackageInfo(ps, flags, userId);
            if (pi != null) {
                if ((flags & 4096) == 0) {
                    if (numMatch == permissions.length) {
                        pi.requestedPermissions = permissions;
                    } else {
                        pi.requestedPermissions = new String[numMatch];
                        numMatch = 0;
                        for (i = 0; i < permissions.length; i++) {
                            if (tmp[i]) {
                                pi.requestedPermissions[numMatch] = permissions[i];
                                numMatch++;
                            }
                        }
                    }
                }
                list.add(pi);
            }
        }
    }

    public ParceledListSlice<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        ParceledListSlice<PackageInfo> parceledListSlice;
        flags = updateFlagsForPackage(flags, userId, permissions);
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "get packages holding permissions");
        boolean listUninstalled = (4202496 & flags) != 0;
        synchronized (this.mPackages) {
            ArrayList<PackageInfo> list = new ArrayList();
            boolean[] tmpBools = new boolean[permissions.length];
            PackageSetting ps;
            if (listUninstalled) {
                for (PackageSetting ps2 : this.mSettings.mPackages.values()) {
                    addPackageHoldingPermissions(list, ps2, permissions, tmpBools, flags, userId);
                }
            } else {
                for (Package pkg : this.mPackages.values()) {
                    ps2 = (PackageSetting) pkg.mExtras;
                    if (ps2 != null) {
                        addPackageHoldingPermissions(list, ps2, permissions, tmpBools, flags, userId);
                    }
                }
            }
            parceledListSlice = new ParceledListSlice(list);
        }
        return parceledListSlice;
    }

    public ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return ParceledListSlice.emptyList();
        }
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        ParceledListSlice<ApplicationInfo> parceledListSlice;
        flags = updateFlagsForApplication(flags, userId, null);
        boolean listUninstalled = (4202496 & flags) != 0;
        synchronized (this.mPackages) {
            ArrayList<ApplicationInfo> list;
            PackageSetting ps;
            ApplicationInfo ai;
            if (listUninstalled) {
                list = new ArrayList(this.mSettings.mPackages.size());
                for (PackageSetting ps2 : this.mSettings.mPackages.values()) {
                    int effectiveFlags = flags;
                    if (ps2.isSystem()) {
                        effectiveFlags = flags | DumpState.DUMP_CHANGES;
                    }
                    if (ps2.pkg == null) {
                        ai = generateApplicationInfoFromSettingsLPw(ps2.name, callingUid, effectiveFlags, userId);
                    } else if (!(filterSharedLibPackageLPr(ps2, callingUid, userId, flags) || filterAppAccessLPr(ps2, callingUid, userId))) {
                        ai = PackageParser.generateApplicationInfo(ps2.pkg, effectiveFlags, ps2.readUserState(userId), userId);
                        if (ai != null) {
                            ai.packageName = resolveExternalPackageNameLPr(ps2.pkg);
                        }
                    }
                    if (ai != null) {
                        list.add(ai);
                    }
                }
            } else {
                list = new ArrayList(this.mPackages.size());
                for (Package p : this.mPackages.values()) {
                    if (p.mExtras != null) {
                        ps2 = (PackageSetting) p.mExtras;
                        if (!(filterSharedLibPackageLPr(ps2, Binder.getCallingUid(), userId, flags) || filterAppAccessLPr(ps2, callingUid, userId))) {
                            ai = PackageParser.generateApplicationInfo(p, flags, ps2.readUserState(userId), userId);
                            if (ai != null) {
                                ai.packageName = resolveExternalPackageNameLPr(p);
                                list.add(ai);
                            }
                        }
                    }
                }
            }
            parceledListSlice = new ParceledListSlice(list);
        }
        return parceledListSlice;
    }

    public ParceledListSlice<InstantAppInfo> getInstantApps(int userId) {
        if (isEphemeralDisabled()) {
            return null;
        }
        if (!canViewInstantApps(Binder.getCallingUid(), userId)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getEphemeralApplications");
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getEphemeralApplications");
        synchronized (this.mPackages) {
            List<InstantAppInfo> instantApps = this.mInstantAppRegistry.getInstantAppsLPr(userId);
            if (instantApps != null) {
                ParceledListSlice<InstantAppInfo> parceledListSlice = new ParceledListSlice(instantApps);
                return parceledListSlice;
            }
            return null;
        }
    }

    public boolean isInstantApp(String packageName, int userId) {
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "isInstantApp");
        if (isEphemeralDisabled()) {
            return false;
        }
        synchronized (this.mPackages) {
            boolean returnAllowed;
            int callingUid = Binder.getCallingUid();
            if (Process.isIsolated(callingUid)) {
                callingUid = this.mIsolatedOwners.get(callingUid);
            }
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            Package pkg = (Package) this.mPackages.get(packageName);
            if (ps == null) {
                returnAllowed = false;
            } else if (isCallerSameApp(packageName, callingUid) || canViewInstantApps(callingUid, userId)) {
                returnAllowed = true;
            } else {
                returnAllowed = this.mInstantAppRegistry.isInstantAccessGranted(userId, UserHandle.getAppId(callingUid), ps.appId);
            }
            if (returnAllowed) {
                boolean instantApp = ps.getInstantApp(userId);
                return instantApp;
            }
            return false;
        }
    }

    public byte[] getInstantAppCookie(String packageName, int userId) {
        if (isEphemeralDisabled()) {
            return null;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getInstantAppCookie");
        if (!isCallerSameApp(packageName, Binder.getCallingUid())) {
            return null;
        }
        byte[] instantAppCookieLPw;
        synchronized (this.mPackages) {
            instantAppCookieLPw = this.mInstantAppRegistry.getInstantAppCookieLPw(packageName, userId);
        }
        return instantAppCookieLPw;
    }

    public boolean setInstantAppCookie(String packageName, byte[] cookie, int userId) {
        if (isEphemeralDisabled()) {
            return true;
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, true, "setInstantAppCookie");
        if (!isCallerSameApp(packageName, Binder.getCallingUid())) {
            return false;
        }
        boolean instantAppCookieLPw;
        synchronized (this.mPackages) {
            instantAppCookieLPw = this.mInstantAppRegistry.setInstantAppCookieLPw(packageName, cookie, userId);
        }
        return instantAppCookieLPw;
    }

    public Bitmap getInstantAppIcon(String packageName, int userId) {
        if (isEphemeralDisabled()) {
            return null;
        }
        Bitmap instantAppIconLPw;
        if (!canViewInstantApps(Binder.getCallingUid(), userId)) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getInstantAppIcon");
        }
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getInstantAppIcon");
        synchronized (this.mPackages) {
            instantAppIconLPw = this.mInstantAppRegistry.getInstantAppIconLPw(packageName, userId);
        }
        return instantAppIconLPw;
    }

    private boolean isCallerSameApp(String packageName, int uid) {
        Package pkg = (Package) this.mPackages.get(packageName);
        if (pkg == null || UserHandle.getAppId(uid) != pkg.applicationInfo.uid) {
            return false;
        }
        return true;
    }

    public ParceledListSlice<ApplicationInfo> getPersistentApplications(int flags) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return ParceledListSlice.emptyList();
        }
        return new ParceledListSlice(getPersistentApplicationsInternal(flags));
    }

    private List<ApplicationInfo> getPersistentApplicationsInternal(int flags) {
        ArrayList<ApplicationInfo> finalList = new ArrayList();
        synchronized (this.mPackages) {
            int userId = UserHandle.getCallingUserId();
            for (Package p : this.mPackages.values()) {
                if (p.applicationInfo != null) {
                    int isDirectBootAware;
                    if ((262144 & flags) != 0) {
                        isDirectBootAware = p.applicationInfo.isDirectBootAware() ^ 1;
                    } else {
                        isDirectBootAware = 0;
                    }
                    boolean isDirectBootAware2;
                    if ((524288 & flags) != 0) {
                        isDirectBootAware2 = p.applicationInfo.isDirectBootAware();
                    } else {
                        isDirectBootAware2 = false;
                    }
                    if ((p.applicationInfo.flags & 8) != 0 && ((!this.mSafeMode || isSystemApp(p)) && (r4 != 0 || r3))) {
                        PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(p.packageName);
                        if (ps != null) {
                            ApplicationInfo ai = PackageParser.generateApplicationInfo(p, flags, ps.readUserState(userId), userId);
                            if (ai != null) {
                                finalList.add(ai);
                            }
                        }
                    }
                }
            }
        }
        return finalList;
    }

    public ProviderInfo resolveContentProvider(String name, int flags, int userId) {
        if (!sUserManager.exists(userId)) {
            return null;
        }
        flags = updateFlagsForComponent(flags, userId, name);
        String instantAppPkgName = getInstantAppPackageName(Binder.getCallingUid());
        synchronized (this.mPackages) {
            PackageSetting packageSetting;
            Provider provider = (Provider) this.mProvidersByAuthority.get(name);
            if (provider != null) {
                packageSetting = (PackageSetting) this.mSettings.mPackages.get(provider.owner.packageName);
            } else {
                packageSetting = null;
            }
            if (packageSetting != null) {
                boolean isInstantApp = packageSetting.getInstantApp(userId);
                if (instantAppPkgName == null && isInstantApp) {
                    return null;
                }
                if (instantAppPkgName != null && isInstantApp) {
                    if ((provider.owner.packageName.equals(instantAppPkgName) ^ 1) != 0) {
                        return null;
                    }
                }
                if (!(instantAppPkgName == null || (isInstantApp ^ 1) == 0)) {
                    if ((provider.info.flags & 1048576) == 0) {
                        return null;
                    }
                }
                if (this.mSettings.isEnabledAndMatchLPr(provider.info, flags, userId)) {
                    ProviderInfo generateProviderInfo = PackageParser.generateProviderInfo(provider, flags, packageSetting.readUserState(userId), userId);
                    return generateProviderInfo;
                }
                return null;
            }
            return null;
        }
    }

    @Deprecated
    public void querySyncProviders(List<String> outNames, List<ProviderInfo> outInfo) {
        if (getInstantAppPackageName(Binder.getCallingUid()) == null) {
            synchronized (this.mPackages) {
                int userId = UserHandle.getCallingUserId();
                for (Entry<String, Provider> entry : this.mProvidersByAuthority.entrySet()) {
                    Provider p = (Provider) entry.getValue();
                    PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(p.owner.packageName);
                    if (ps != null && p.syncable) {
                        if (!this.mSafeMode || (p.info.applicationInfo.flags & 1) != 0) {
                            ProviderInfo info = PackageParser.generateProviderInfo(p, 0, ps.readUserState(userId), userId);
                            if (info != null) {
                                outNames.add((String) entry.getKey());
                                outInfo.add(info);
                            }
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ParceledListSlice<ProviderInfo> queryContentProviders(String processName, int uid, int flags, String metaDataKey) {
        int userId;
        Throwable th;
        int callingUid = Binder.getCallingUid();
        if (processName != null) {
            userId = UserHandle.getUserId(uid);
        } else {
            userId = UserHandle.getCallingUserId();
        }
        if (!sUserManager.exists(userId)) {
            return ParceledListSlice.emptyList();
        }
        flags = updateFlagsForComponent(flags, userId, processName);
        synchronized (this.mPackages) {
            try {
                ArrayList<ProviderInfo> finalList = null;
                for (Provider p : this.mProviders.mProviders.values()) {
                    ArrayList<ProviderInfo> finalList2;
                    try {
                        PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(p.owner.packageName);
                        if (ps == null || p.info.authority == null || !(processName == null || (p.info.processName.equals(processName) && UserHandle.isSameApp(p.info.applicationInfo.uid, uid)))) {
                            finalList2 = finalList;
                        } else if (!this.mSettings.isEnabledAndMatchLPr(p.info, flags, userId)) {
                            finalList2 = finalList;
                        } else if (metaDataKey == null || (p.metaData != null && (p.metaData.containsKey(metaDataKey) ^ 1) == 0)) {
                            if (filterAppAccessLPr(ps, callingUid, new ComponentName(p.info.packageName, p.info.name), 4, userId)) {
                                continue;
                            } else {
                                if (finalList == null) {
                                    finalList2 = new ArrayList(3);
                                } else {
                                    finalList2 = finalList;
                                }
                                ProviderInfo info = PackageParser.generateProviderInfo(p, flags, ps.readUserState(userId), userId);
                                if (info != null) {
                                    finalList2.add(info);
                                }
                            }
                        }
                        finalList = finalList2;
                    } catch (Throwable th2) {
                        th = th2;
                        finalList2 = finalList;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
        throw th;
    }

    public InstrumentationInfo getInstrumentationInfo(ComponentName component, int flags) {
        synchronized (this.mPackages) {
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(component.getPackageName());
            if (ps == null) {
                return null;
            } else if (filterAppAccessLPr(ps, callingUid, component, 0, callingUserId)) {
                return null;
            } else {
                InstrumentationInfo generateInstrumentationInfo = PackageParser.generateInstrumentationInfo((Instrumentation) this.mInstrumentation.get(component), flags);
                return generateInstrumentationInfo;
            }
        }
    }

    public ParceledListSlice<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        int callingUid = Binder.getCallingUid();
        if (filterAppAccessLPr((PackageSetting) this.mSettings.mPackages.get(targetPackage), callingUid, UserHandle.getUserId(callingUid))) {
            return ParceledListSlice.emptyList();
        }
        return new ParceledListSlice(queryInstrumentationInternal(targetPackage, flags));
    }

    private List<InstrumentationInfo> queryInstrumentationInternal(String targetPackage, int flags) {
        ArrayList<InstrumentationInfo> finalList = new ArrayList();
        synchronized (this.mPackages) {
            for (Instrumentation p : this.mInstrumentation.values()) {
                if (targetPackage == null || targetPackage.equals(p.info.targetPackage)) {
                    InstrumentationInfo ii = PackageParser.generateInstrumentationInfo(p, flags);
                    if (ii != null) {
                        finalList.add(ii);
                    }
                }
            }
        }
        return finalList;
    }

    private void scanDirTracedLI(File dir, int parseFlags, int scanFlags, long currentTime) {
        Trace.traceBegin(262144, "scanDir [" + dir.getAbsolutePath() + "]");
        try {
            scanDirLI(dir, parseFlags, scanFlags, currentTime);
        } finally {
            Trace.traceEnd(262144);
        }
    }

    protected void scanDirLI(File dir, int parseFlags, int scanFlags, long currentTime) {
        scanDirLI(dir, parseFlags, scanFlags, currentTime, 0);
    }

    protected void scanDirLI(File dir, int parseFlags, int scanFlags, long currentTime, int hwFlags) {
        if (HWFLOW) {
            this.startTimer = SystemClock.uptimeMillis();
        }
        File[] files = dir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            Log.d(TAG, "No files in app dir " + dir);
            return;
        }
        ParallelPackageParser parallelPackageParser = new ParallelPackageParser(this.mSeparateProcesses, this.mOnlyCore, this.mMetrics, this.mCacheDir, this.mParallelPackageParserCallback);
        int fileCount = 0;
        for (File file : files) {
            boolean isStageName;
            if (PackageParser.isApkFile(file) || file.isDirectory()) {
                isStageName = PackageInstallerService.isStageName(file.getName()) ^ 1;
            } else {
                isStageName = false;
            }
            if (isStageName && !isUninstallApk(file.getPath() + ".apk")) {
                HwCustEmergDataManager emergDataManager = HwCustEmergDataManager.getDefault();
                if (emergDataManager == null || (emergDataManager.isEmergencyState() ^ 1) == 0 || !emergDataManager.getEmergencyPkgName().contains(file.getName())) {
                    parallelPackageParser.submit(file, parseFlags);
                    fileCount++;
                } else {
                    Log.i(TAG, "dont scan EmergencyData.apk");
                }
            }
        }
        while (fileCount > 0) {
            ParseResult parseResult = parallelPackageParser.take();
            Throwable throwable = parseResult.throwable;
            int errorCode = 1;
            if (throwable == null) {
                if (parseResult.pkg.applicationInfo.isStaticSharedLibrary()) {
                    renameStaticSharedLibraryPackage(parseResult.pkg);
                }
                try {
                    parseResult.pkg.applicationInfo.hwFlags = hwFlags;
                    int currHwFlags = hwFlags;
                    if (isPreRemovableApp(parseResult.pkg.codePath) && (33554432 & hwFlags) == 0) {
                        currHwFlags = hwFlags | 33554432;
                        parseResult.pkg.applicationInfo.hwFlags = currHwFlags;
                    }
                    scanPackageLI(parseResult.pkg, parseResult.scanFile, parseFlags, scanFlags, currentTime, null, currHwFlags);
                } catch (PackageManagerException e) {
                    errorCode = e.error;
                    Slog.w(TAG, "Failed to scan " + parseResult.scanFile + ": " + e.getMessage());
                }
            } else if (throwable instanceof PackageParserException) {
                PackageParserException e2 = (PackageParserException) throwable;
                errorCode = e2.error;
                Slog.w(TAG, "Failed to parse " + parseResult.scanFile + ": " + e2.getMessage());
            } else {
                throw new IllegalStateException("Unexpected exception occurred while parsing " + parseResult.scanFile, throwable);
            }
            if ((parseFlags & 1) == 0 && errorCode == -2) {
                logCriticalInfo(5, "Deleting invalid package at " + parseResult.scanFile);
                removeCodePathLI(parseResult.scanFile);
            }
            fileCount--;
        }
        parallelPackageParser.close();
    }

    private static File getSettingsProblemFile() {
        return new File(new File(Environment.getDataDirectory(), "system"), "uiderrors.txt");
    }

    static void reportSettingsProblem(int priority, String msg) {
        logCriticalInfo(priority, msg);
    }

    public static void logCriticalInfo(int priority, String msg) {
        Slog.println(priority, TAG, msg);
        EventLogTags.writePmCriticalInfo(msg);
        try {
            File fname = getSettingsProblemFile();
            PrintWriter pw = new FastPrintWriter(new FileOutputStream(fname, true));
            pw.println(new SimpleDateFormat().format(new Date(System.currentTimeMillis())) + ": " + msg);
            pw.close();
            FileUtils.setPermissions(fname.toString(), 508, -1, -1);
        } catch (IOException e) {
        }
    }

    private long getLastModifiedTime(Package pkg, File srcFile) {
        if (!srcFile.isDirectory()) {
            return srcFile.lastModified();
        }
        long maxModifiedTime = new File(pkg.baseCodePath).lastModified();
        if (pkg.splitCodePaths != null) {
            for (int i = pkg.splitCodePaths.length - 1; i >= 0; i--) {
                maxModifiedTime = Math.max(maxModifiedTime, new File(pkg.splitCodePaths[i]).lastModified());
            }
        }
        return maxModifiedTime;
    }

    private void collectCertificatesLI(PackageSetting ps, Package pkg, File srcFile, int policyFlags) throws PackageManagerException {
        long lastModifiedTime = this.mIsPreNMR1Upgrade ? new File(pkg.codePath).lastModified() : getLastModifiedTime(pkg, srcFile);
        if (ps == null || !ps.codePath.equals(srcFile) || ps.timeStamp != lastModifiedTime || (isCompatSignatureUpdateNeeded(pkg) ^ 1) == 0 || (isRecoverSignatureUpdateNeeded(pkg) ^ 1) == 0) {
            Slog.i(TAG, srcFile.toString() + " changed; collecting certs");
        } else {
            ArraySet<PublicKey> signingKs;
            long mSigningKeySetId = ps.keySetData.getProperSigningKeySet();
            KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
            synchronized (this.mPackages) {
                signingKs = ksms.getPublicKeysFromKeySetLPr(mSigningKeySetId);
            }
            if (ps.signatures.mSignatures == null || ps.signatures.mSignatures.length == 0 || signingKs == null) {
                Slog.w(TAG, "PackageSetting for " + ps.name + " is missing signatures.  Collecting certs again to recover them.");
            } else {
                pkg.mSignatures = ps.signatures.mSignatures;
                pkg.mSigningKeys = signingKs;
                return;
            }
        }
        try {
            Trace.traceBegin(262144, "collectCertificates");
            PackageParser.collectCertificates(pkg, policyFlags);
            Trace.traceEnd(262144);
        } catch (PackageParserException e) {
            throw PackageManagerException.from(e);
        } catch (Throwable th) {
            Trace.traceEnd(262144);
        }
    }

    private Package scanPackageTracedLI(File scanFile, int parseFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        return scanPackageTracedLI(scanFile, parseFlags, scanFlags, currentTime, user, 0);
    }

    private Package scanPackageTracedLI(File scanFile, int parseFlags, int scanFlags, long currentTime, UserHandle user, int hwFlags) throws PackageManagerException {
        Trace.traceBegin(262144, "scanPackage [" + scanFile.toString() + "]");
        try {
            Package scanPackageLI = scanPackageLI(scanFile, parseFlags, scanFlags, currentTime, user, hwFlags);
            return scanPackageLI;
        } finally {
            Trace.traceEnd(262144);
        }
    }

    protected Package scanPackageLI(File scanFile, int parseFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        return scanPackageLI(scanFile, parseFlags, scanFlags, currentTime, user, 0);
    }

    protected Package scanPackageLI(File scanFile, int parseFlags, int scanFlags, long currentTime, UserHandle user, int hwFlags) throws PackageManagerException {
        if (isUninstallApk(scanFile.getPath() + ".apk")) {
            return null;
        }
        PackageParser pp = new PackageParser();
        pp.setSeparateProcesses(this.mSeparateProcesses);
        pp.setOnlyCoreApps(this.mOnlyCore);
        pp.setDisplayMetrics(this.mMetrics);
        pp.setCallback(this.mPackageParserCallback);
        pp.setCacheDir(this.mCacheDir);
        if ((scanFlags & 128) != 0) {
            parseFlags |= 512;
        }
        if ((134217728 & hwFlags) != 0 && !isCustApkRecorded(scanFile)) {
            return null;
        }
        Trace.traceBegin(262144, "parsePackage");
        try {
            Package pkg = pp.parsePackage(scanFile, parseFlags, true, hwFlags);
            int currHwFlags = pkg.applicationInfo.hwFlags;
            if (isPreRemovableApp(pkg.codePath) && (33554432 & currHwFlags) == 0) {
                hwFlags |= 33554432;
                pkg.applicationInfo.hwFlags = currHwFlags | 33554432;
            }
            Trace.traceEnd(262144);
            if (pkg.applicationInfo.isStaticSharedLibrary()) {
                renameStaticSharedLibraryPackage(pkg);
            }
            return scanPackageLI(pkg, scanFile, parseFlags, scanFlags, currentTime, user, hwFlags);
        } catch (PackageParserException e) {
            throw PackageManagerException.from(e);
        } catch (Throwable th) {
            Trace.traceEnd(262144);
        }
    }

    private Package scanPackageLI(Package pkg, File scanFile, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        return scanPackageLI(pkg, scanFile, policyFlags, scanFlags, currentTime, user, 0);
    }

    private Package scanPackageLI(Package pkg, File scanFile, int policyFlags, int scanFlags, long currentTime, UserHandle user, int hwFlags) throws PackageManagerException {
        if (pkg != null) {
            if (isInMultiWinWhiteList(pkg.packageName)) {
                pkg.forceResizeableAllActivity();
            }
        }
        if ((scanFlags & 8192) != 0) {
            scanFlags &= -8193;
        } else if (pkg.childPackages != null && pkg.childPackages.size() > 0) {
            scanFlags |= 8192;
        }
        boolean containsKey = (1073741824 & hwFlags) != 0 ? this.mPackages.containsKey(pkg.packageName) ^ 1 : false;
        Package scannedPkg = scanPackageInternalLI(pkg, scanFile, policyFlags, scanFlags, currentTime, user, hwFlags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            scanPackageInternalLI((Package) pkg.childPackages.get(i), scanFile, policyFlags, scanFlags, currentTime, user, hwFlags);
        }
        if ((scanFlags & 8192) != 0) {
            return scanPackageLI(pkg, scanFile, policyFlags, scanFlags, currentTime, user, hwFlags);
        }
        if (this.mCotaFlag && this.mTempPkgList != null) {
            this.mTempPkgList.add(pkg);
        }
        doPostScanInstall(pkg, user, containsKey, hwFlags);
        return scannedPkg;
    }

    private Package scanPackageInternalLI(Package pkg, File scanFile, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        return scanPackageInternalLI(pkg, scanFile, policyFlags, scanFlags, currentTime, user, 0);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Package scanPackageInternalLI(Package pkg, File scanFile, int policyFlags, int scanFlags, long currentTime, UserHandle user, int hwFlags) throws PackageManagerException {
        Throwable th;
        synchronized (this.mPackages) {
            if (!needInstallRemovablePreApk(pkg, hwFlags) && (1073741824 & hwFlags) == 0) {
                addUnisntallDataToCache(pkg.packageName, pkg.codePath);
                PackageSetting psTemp = this.mSettings.getPackageLPr(pkg.packageName);
                if (psTemp != null) {
                    if ((psTemp.isAnyInstalled(sUserManager.getUserIds()) ^ 1) == 0) {
                        if (this.mSettings.getDisabledSystemPkgLPr(pkg.packageName) == null) {
                            if (psTemp.codePathString != null) {
                            }
                        }
                    }
                }
                Slog.d(TAG, "scan return here for package:" + pkg.packageName);
                return null;
            }
        }
        if (r42 != null) {
            throw r42;
        }
        PackageSetting packageSetting = null;
        if (!((policyFlags & 64) != 0 || packageSetting == null || (packageSetting.codePath.equals(packageSetting.resourcePath) ^ 1) == 0)) {
            policyFlags |= 16;
        }
        int userId = user == null ? 0 : user.getIdentifier();
        if (packageSetting != null && packageSetting.getInstantApp(userId)) {
            scanFlags |= 131072;
        }
        if (packageSetting != null && packageSetting.getVirtulalPreload(userId)) {
            scanFlags |= 524288;
        }
        Package scannedPkg = scanPackageLI(pkg, policyFlags, scanFlags | 8, currentTime, user, hwFlags);
        if (shouldHideSystemApp) {
            synchronized (this.mPackages) {
                this.mSettings.disableSystemPackageLPw(pkg.packageName, true);
            }
        }
        addPreinstalledPkgToList(scannedPkg);
        return scannedPkg;
        if (r25 != null) {
            try {
                r25.close();
            } catch (Throwable th2) {
                if (th == null) {
                    th = th2;
                } else if (th != th2) {
                    th.addSuppressed(th2);
                }
            }
        }
        if (th != null) {
            throw th;
        }
        throw r4;
    }

    private void renameStaticSharedLibraryPackage(Package pkg) {
        pkg.setPackageName(pkg.packageName + STATIC_SHARED_LIB_DELIMITER + pkg.staticSharedLibVersion);
    }

    private static String fixProcessName(String defProcessName, String processName) {
        if (processName == null) {
            return defProcessName;
        }
        return processName;
    }

    private void verifySignaturesLP(PackageSetting pkgSetting, Package pkg) throws PackageManagerException {
        boolean match;
        if (pkgSetting.signatures.mSignatures != null) {
            match = compareSignatures(pkgSetting.signatures.mSignatures, pkg.mSignatures) == 0;
            if (!match) {
                match = compareSignaturesCompat(pkgSetting.signatures, pkg) == 0;
            }
            if (!match) {
                match = compareSignaturesRecover(pkgSetting.signatures, pkg) == 0;
            }
            if (!match) {
                throw new PackageManagerException(-7, "Package " + pkg.packageName + " signatures do not match the " + "previously installed version; ignoring!");
            }
        }
        if (pkgSetting.sharedUser != null && pkgSetting.sharedUser.signatures.mSignatures != null) {
            match = compareSignatures(pkgSetting.sharedUser.signatures.mSignatures, pkg.mSignatures) == 0;
            if (!match) {
                match = compareSignaturesCompat(pkgSetting.sharedUser.signatures, pkg) == 0;
            }
            if (!match) {
                match = compareSignaturesRecover(pkgSetting.sharedUser.signatures, pkg) == 0;
            }
            if (!match) {
                throw new PackageManagerException(-8, "Package " + pkg.packageName + " has no signatures that match those in shared user " + pkgSetting.sharedUser.name + "; ignoring!");
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void verifyValidVerifierInstall(String installerPackageName, String pkgName, int userId, int appId) throws PackageManagerException {
        String checkInstallPackage = "com.android.vending";
        if (pkgName.equals(checkInstallPackage) && ((TextUtils.isEmpty(installerPackageName) || (!installerPackageName.equals("com.android.packageinstaller") && !installerPackageName.equals("com.huawei.appmarket"))) && pkgName.equals(checkInstallPackage) && ((TextUtils.isEmpty(installerPackageName) || !installerPackageName.equals(checkInstallPackage)) && checkPermission("android.permission.INSTALL_PACKAGES", installerPackageName, userId) != -1 && appId != 0 && appId != 2000 && appId != 1000))) {
            throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Invalid installer for " + checkInstallPackage + "!");
        }
    }

    private static final void enforceSystemOrRoot(String message) {
        int uid = Binder.getCallingUid();
        if (uid != 1000 && uid != 0) {
            throw new SecurityException(message);
        }
    }

    public void performFstrimIfNeeded() {
        enforceSystemOrRoot("Only the system can request fstrim");
        HwThemeManager.applyDefaultHwTheme(false, this.mContext, 0);
        HwThemeManager.linkDataSkinDirAsUser(0);
        try {
            IStorageManager sm = PackageHelper.getStorageManager();
            if (sm != null) {
                boolean doTrim = false;
                long interval = Global.getLong(this.mContext.getContentResolver(), "fstrim_mandatory_interval", DEFAULT_MANDATORY_FSTRIM_INTERVAL);
                if (interval > 0) {
                    long timeSinceLast = System.currentTimeMillis() - sm.lastMaintenance();
                    if (timeSinceLast > interval) {
                        doTrim = true;
                        Slog.w(TAG, "No disk maintenance in " + timeSinceLast + "; running immediately");
                    }
                }
                if (doTrim) {
                    synchronized (this.mPackages) {
                        boolean dexOptDialogShown = this.mDexOptDialogShown;
                    }
                    if (!isFirstBoot() && dexOptDialogShown) {
                        try {
                            ActivityManager.getService().showBootMessage(this.mContext.getResources().getString(17039579), true);
                            return;
                        } catch (RemoteException e) {
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            Slog.e(TAG, "storageManager service unavailable!");
        } catch (RemoteException e2) {
        }
    }

    public void updatePackagesIfNeeded() {
        enforceSystemOrRoot("Only the system can request package update");
        boolean causeUpgrade = isUpgrade();
        int i = !isFirstBoot() ? this.mIsPreNUpgrade : 1;
        boolean causePrunedCache = VMRuntime.didPruneDalvikCache();
        if (causeUpgrade || (i ^ 1) == 0 || (causePrunedCache ^ 1) == 0) {
            List<Package> pkgs;
            int i2;
            synchronized (this.mPackages) {
                pkgs = PackageManagerServiceUtils.getPackagesForDexopt(this.mPackages.values(), this);
            }
            boolean showDialog = this.mIsPreNUpgrade;
            if (SystemProperties.getBoolean("ro.config.show_dex2oatDialog", true)) {
                showDialog = this.mIsUpgrade;
            }
            long startTime = System.nanoTime();
            if (i != 0) {
                i2 = 0;
            } else {
                i2 = 1;
            }
            int[] stats = performDexOptUpgrade(pkgs, showDialog, PackageManagerServiceCompilerMapping.getCompilerFilterForReason(i2), false);
            int elapsedTimeSeconds = (int) TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            MetricsLogger.histogram(this.mContext, "opt_dialog_num_dexopted", stats[0]);
            MetricsLogger.histogram(this.mContext, "opt_dialog_num_skipped", stats[1]);
            MetricsLogger.histogram(this.mContext, "opt_dialog_num_failed", stats[2]);
            MetricsLogger.histogram(this.mContext, "opt_dialog_num_total", getOptimizablePackages().size());
            MetricsLogger.histogram(this.mContext, "opt_dialog_time_s", elapsedTimeSeconds);
        }
    }

    private static String getPrebuildProfilePath(Package pkg) {
        return pkg.baseCodePath + ".prof";
    }

    private int[] performDexOptUpgrade(List<Package> pkgs, boolean showDialog, String compilerFilter, boolean bootComplete) {
        int numberOfPackagesVisited = 0;
        int numberOfPackagesOptimized = 0;
        int numberOfPackagesSkipped = 0;
        int numberOfPackagesFailed = 0;
        int numberOfPackagesToDexopt = pkgs.size();
        boolean isChina = "CN".equalsIgnoreCase(SystemProperties.get("ro.product.locale.region", ""));
        connectBootAnimation();
        for (Package pkg : pkgs) {
            numberOfPackagesVisited++;
            boolean useProfileForDexopt = false;
            if ((isFirstBoot() || isUpgrade()) && isSystemApp(pkg)) {
                File file = new File(getPrebuildProfilePath(pkg));
                if (file.exists()) {
                    try {
                        if (!this.mInstaller.copySystemProfile(file.getAbsolutePath(), pkg.applicationInfo.uid, pkg.packageName)) {
                            Log.e(TAG, "Installer failed to copy system profile!");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to copy profile " + file.getAbsolutePath() + " ", e);
                    }
                } else {
                    PackageSetting disabledPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
                    if (disabledPs != null && disabledPs.pkg.isStub) {
                        File profileFile = new File(getPrebuildProfilePath(disabledPs.pkg).replace(STUB_SUFFIX, ""));
                        if (profileFile.exists()) {
                            try {
                                if (this.mInstaller.copySystemProfile(profileFile.getAbsolutePath(), pkg.applicationInfo.uid, pkg.packageName)) {
                                    useProfileForDexopt = true;
                                } else {
                                    Log.e(TAG, "Failed to copy system profile for stub package!");
                                }
                            } catch (Exception e2) {
                                Log.e(TAG, "Failed to copy profile " + profileFile.getAbsolutePath() + " ", e2);
                            }
                        }
                    }
                }
            }
            if (PackageDexOptimizer.canOptimizePackage(pkg)) {
                if (showDialog && this.mIBootAnmation != null) {
                    int inputData = 0;
                    if (numberOfPackagesVisited != 1) {
                        inputData = 33554432;
                    }
                    int data = (numberOfPackagesVisited * 100) / numberOfPackagesToDexopt;
                    if (data == 0) {
                        data = 1;
                    }
                    try {
                        this.mIBootAnmation.notifyProcessing((inputData | (data << 16)) | (isChina ? 0 : 256));
                    } catch (RemoteException e3) {
                        Slog.w(TAG, "show boot dexoat process error," + e3.getMessage());
                    }
                }
                String pkgCompilerFilter = compilerFilter;
                if (useProfileForDexopt) {
                    pkgCompilerFilter = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(3);
                }
                int primaryDexOptStaus = performDexOptTraced(new DexoptOptions(pkg.packageName, pkgCompilerFilter, bootComplete ? 4 : 0));
                switch (primaryDexOptStaus) {
                    case -1:
                        numberOfPackagesFailed++;
                        break;
                    case 0:
                        numberOfPackagesSkipped++;
                        break;
                    case 1:
                        numberOfPackagesOptimized++;
                        break;
                    default:
                        Log.e(TAG, "Unexpected dexopt return code " + primaryDexOptStaus);
                        break;
                }
            }
            numberOfPackagesSkipped++;
        }
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                if (PackageManagerService.this.mIBootAnmation != null) {
                    try {
                        PackageManagerService.this.mIBootAnmation.notifyProcessing(16777216);
                    } catch (RemoteException e) {
                        Slog.w(PackageManagerService.TAG, "finish boot dexoat process error," + e.getMessage());
                    }
                }
                PackageManagerService.this.mIBootAnmation = null;
            }
        }, 1000);
        return new int[]{numberOfPackagesOptimized, numberOfPackagesSkipped, numberOfPackagesFailed};
    }

    public void notifyPackageUse(String packageName, int reason) {
        synchronized (this.mPackages) {
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            if (getInstantAppPackageName(callingUid) != null) {
                if (!isCallerSameApp(packageName, callingUid)) {
                    return;
                }
            } else if (isInstantApp(packageName, callingUserId)) {
                return;
            }
            notifyPackageUseLocked(packageName, reason);
        }
    }

    private void notifyPackageUseLocked(String packageName, int reason) {
        Package p = (Package) this.mPackages.get(packageName);
        if (p != null) {
            p.mLastPackageUsageTimeInMills[reason] = System.currentTimeMillis();
        }
    }

    public void notifyDexLoad(String loadingPackageName, List<String> classLoaderNames, List<String> classPaths, String loaderIsa) {
        int userId = UserHandle.getCallingUserId();
        ApplicationInfo ai = getApplicationInfo(loadingPackageName, 0, userId);
        if (ai == null) {
            Slog.w(TAG, "Loading a package that does not exist for the calling user. package=" + loadingPackageName + ", user=" + userId);
        } else {
            this.mDexManager.notifyDexLoad(ai, classLoaderNames, classPaths, loaderIsa, userId);
        }
    }

    public void registerDexModule(String packageName, String dexModulePath, boolean isSharedModule, IDexModuleRegisterCallback callback) {
        RegisterDexModuleResult result;
        int userId = UserHandle.getCallingUserId();
        ApplicationInfo ai = getApplicationInfo(packageName, 0, userId);
        if (ai == null) {
            Slog.w(TAG, "Registering a dex module for a package that does not exist for the calling user. package=" + packageName + ", user=" + userId);
            result = new RegisterDexModuleResult(false, "Package not installed");
        } else {
            result = this.mDexManager.registerDexModule(ai, dexModulePath, isSharedModule, userId);
        }
        if (callback != null) {
            this.mHandler.post(new -$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E(callback, dexModulePath, result));
        }
    }

    static /* synthetic */ void lambda$-com_android_server_pm_PackageManagerService_514151(IDexModuleRegisterCallback callback, String dexModulePath, RegisterDexModuleResult result) {
        try {
            callback.onDexModuleRegistered(dexModulePath, result.success, result.message);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to callback after module registration " + dexModulePath, e);
        }
    }

    public boolean performDexOptMode(String packageName, boolean checkProfiles, String targetCompilerFilter, boolean force, boolean bootComplete, String splitName) {
        int i;
        int i2;
        int i3 = 0;
        if (checkProfiles) {
            i = 1;
        } else {
            i = 0;
        }
        if (force) {
            i2 = 2;
        } else {
            i2 = 0;
        }
        i2 |= i;
        if (bootComplete) {
            i3 = 4;
        }
        return performDexOpt(new DexoptOptions(packageName, targetCompilerFilter, splitName, i2 | i3));
    }

    public boolean performDexOptSecondary(String packageName, String compilerFilter, boolean force) {
        return performDexOpt(new DexoptOptions(packageName, compilerFilter, (force ? 2 : 0) | 13));
    }

    boolean performDexOpt(DexoptOptions options) {
        boolean z = false;
        if (getInstantAppPackageName(Binder.getCallingUid()) != null || isInstantApp(options.getPackageName(), UserHandle.getCallingUserId())) {
            return false;
        }
        if (options.isDexoptOnlySecondaryDex()) {
            return this.mDexManager.dexoptSecondaryDex(options);
        }
        if (performDexOptWithStatus(options) != -1) {
            z = true;
        }
        return z;
    }

    int performDexOptWithStatus(DexoptOptions options) {
        return performDexOptTraced(options);
    }

    private int performDexOptTraced(DexoptOptions options) {
        Trace.traceBegin(262144, "dexopt");
        try {
            int performDexOptInternal = performDexOptInternal(options);
            return performDexOptInternal;
        } finally {
            Trace.traceEnd(262144);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int performDexOptInternal(DexoptOptions options) {
        int i = this.mPackages;
        synchronized (i) {
            Package p = (Package) this.mPackages.get(options.getPackageName());
            if (p == null) {
                return -1;
            }
            this.mPackageUsage.maybeWriteAsync(this.mPackages);
            this.mCompilerStats.maybeWriteAsync();
        }
    }

    public ArraySet<String> getOptimizablePackages() {
        ArraySet<String> pkgs = new ArraySet();
        synchronized (this.mPackages) {
            for (Package p : this.mPackages.values()) {
                if (PackageDexOptimizer.canOptimizePackage(p)) {
                    pkgs.add(p.packageName);
                }
            }
        }
        return pkgs;
    }

    private int performDexOptInternalWithDependenciesLI(Package p, DexoptOptions options) {
        PackageDexOptimizer pdo;
        if (options.isForce()) {
            pdo = new ForcedUpdatePackageDexOptimizer(this.mPackageDexOptimizer);
        } else {
            pdo = this.mPackageDexOptimizer;
        }
        Collection<Package> deps = findSharedNonSystemLibraries(p);
        String[] instructionSets = InstructionSets.getAppDexInstructionSets(p.applicationInfo);
        if (!deps.isEmpty()) {
            DexoptOptions libraryOptions = new DexoptOptions(options.getPackageName(), options.getCompilerFilter(), options.getSplitName(), options.getFlags() | 64);
            for (Package depPackage : deps) {
                pdo.performDexOpt(depPackage, null, instructionSets, getOrCreateCompilerPackageStats(depPackage), this.mDexManager.getPackageUseInfoOrDefault(depPackage.packageName), libraryOptions);
            }
        }
        return pdo.performDexOpt(p, p.usesLibraryFiles, instructionSets, getOrCreateCompilerPackageStats(p), this.mDexManager.getPackageUseInfoOrDefault(p.packageName), options);
    }

    public void reconcileSecondaryDexFiles(String packageName) {
        if (getInstantAppPackageName(Binder.getCallingUid()) == null && !isInstantApp(packageName, UserHandle.getCallingUserId())) {
            this.mDexManager.reconcileSecondaryDexFiles(packageName);
        }
    }

    DexManager getDexManager() {
        return this.mDexManager;
    }

    public boolean runBackgroundDexoptJob() {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return false;
        }
        return BackgroundDexOptService.runIdleOptimizationsNow(this, this.mContext);
    }

    List<Package> findSharedNonSystemLibraries(Package p) {
        if (p.usesLibraries == null && p.usesOptionalLibraries == null && p.usesStaticLibraries == null) {
            return Collections.emptyList();
        }
        ArrayList<Package> retValue = new ArrayList();
        findSharedNonSystemLibrariesRecursive(p, retValue, new HashSet());
        retValue.remove(p);
        return retValue;
    }

    private void findSharedNonSystemLibrariesRecursive(Package p, ArrayList<Package> collected, Set<String> collectedNames) {
        if (!collectedNames.contains(p.packageName)) {
            collectedNames.add(p.packageName);
            collected.add(p);
            if (p.usesLibraries != null) {
                findSharedNonSystemLibrariesRecursive(p.usesLibraries, null, collected, collectedNames);
            }
            if (p.usesOptionalLibraries != null) {
                findSharedNonSystemLibrariesRecursive(p.usesOptionalLibraries, null, collected, collectedNames);
            }
            if (p.usesStaticLibraries != null) {
                findSharedNonSystemLibrariesRecursive(p.usesStaticLibraries, p.usesStaticLibrariesVersions, collected, collectedNames);
            }
        }
    }

    private void findSharedNonSystemLibrariesRecursive(ArrayList<String> libs, int[] versions, ArrayList<Package> collected, Set<String> collectedNames) {
        int libNameCount = libs.size();
        int i = 0;
        while (i < libNameCount) {
            String libName = (String) libs.get(i);
            int version = (versions == null || versions.length != libNameCount) ? -1 : versions[i];
            Package libPkg = findSharedNonSystemLibrary(libName, version);
            if (libPkg != null) {
                findSharedNonSystemLibrariesRecursive(libPkg, collected, collectedNames);
            }
            i++;
        }
    }

    private Package findSharedNonSystemLibrary(String name, int version) {
        synchronized (this.mPackages) {
            SharedLibraryEntry libEntry = getSharedLibraryEntryLPr(name, version);
            if (libEntry != null) {
                Package packageR = (Package) this.mPackages.get(libEntry.apk);
                return packageR;
            }
            return null;
        }
    }

    private SharedLibraryEntry getSharedLibraryEntryLPr(String name, int version) {
        SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.get(name);
        if (versionedLib == null) {
            return null;
        }
        return (SharedLibraryEntry) versionedLib.get(version);
    }

    private SharedLibraryEntry getLatestSharedLibraVersionLPr(Package pkg) {
        SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.get(pkg.staticSharedLibName);
        if (versionedLib == null) {
            return null;
        }
        int previousLibVersion = -1;
        int versionCount = versionedLib.size();
        for (int i = 0; i < versionCount; i++) {
            int libVersion = versionedLib.keyAt(i);
            if (libVersion < pkg.staticSharedLibVersion) {
                previousLibVersion = Math.max(previousLibVersion, libVersion);
            }
        }
        if (previousLibVersion >= 0) {
            return (SharedLibraryEntry) versionedLib.get(previousLibVersion);
        }
        return null;
    }

    public void shutdown() {
        this.mPackageUsage.writeNow(this.mPackages);
        this.mCompilerStats.writeNow();
        this.mDexManager.writePackageDexUsageNow();
    }

    public void dumpProfiles(String packageName) {
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
        }
        int callingUid = Binder.getCallingUid();
        if (callingUid == 2000 || callingUid == 0 || callingUid == pkg.applicationInfo.uid) {
            synchronized (this.mInstallLock) {
                Trace.traceBegin(262144, "dump profiles");
                int sharedGid = UserHandle.getSharedAppGid(pkg.applicationInfo.uid);
                try {
                    this.mInstaller.dumpProfiles(sharedGid, packageName, TextUtils.join(";", pkg.getAllCodePathsExcludingResourceOnly()));
                } catch (InstallerException e) {
                    Slog.w(TAG, "Failed to dump profiles", e);
                }
                Trace.traceEnd(262144);
            }
            return;
        }
        throw new SecurityException("dumpProfiles");
    }

    public void forceDexOpt(String packageName) {
        enforceSystemOrRoot("forceDexOpt");
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null) {
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
        }
        synchronized (this.mInstallLock) {
            Trace.traceBegin(262144, "dexopt");
            int res = performDexOptInternalWithDependenciesLI(pkg, new DexoptOptions(packageName, PackageManagerServiceCompilerMapping.getDefaultCompilerFilter(), 6));
            Trace.traceEnd(262144);
            if (res != 1) {
                throw new IllegalStateException("Failed to dexopt: " + res);
            }
        }
    }

    private boolean verifyPackageUpdateLPr(PackageSetting oldPkg, Package newPkg) {
        if ((oldPkg.pkgFlags & 1) == 0) {
            Slog.w(TAG, "Unable to update from " + oldPkg.name + " to " + newPkg.packageName + ": old package not in system partition");
            return false;
        } else if (this.mPackages.get(oldPkg.name) == null) {
            return true;
        } else {
            Slog.w(TAG, "Unable to update from " + oldPkg.name + " to " + newPkg.packageName + ": old package still exists");
            return false;
        }
    }

    void removeCodePathLI(File codePath) {
        if (codePath.isDirectory()) {
            try {
                this.mInstaller.rmPackageDir(codePath.getAbsolutePath());
                return;
            } catch (InstallerException e) {
                Slog.w(TAG, "Failed to remove code path", e);
                return;
            }
        }
        codePath.delete();
    }

    private int[] resolveUserIds(int userId) {
        if (userId == -1) {
            return sUserManager.getUserIds();
        }
        return new int[]{userId};
    }

    private void clearAppDataLIF(Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        clearAppDataLeafLIF(pkg, userId, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            clearAppDataLeafLIF((Package) pkg.childPackages.get(i), userId, flags);
        }
    }

    private void clearAppDataLeafLIF(Package pkg, int userId, int flags) {
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkg.packageName);
        }
        for (int realUserId : resolveUserIds(userId)) {
            try {
                this.mInstaller.clearAppData(pkg.volumeUuid, pkg.packageName, realUserId, flags, ps != null ? ps.getCeDataInode(realUserId) : 0);
            } catch (InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
            }
        }
    }

    private void destroyAppDataLIF(Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        destroyAppDataLeafLIF(pkg, userId, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            destroyAppDataLeafLIF((Package) pkg.childPackages.get(i), userId, flags);
        }
    }

    private void destroyAppDataLeafLIF(Package pkg, int userId, int flags) {
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkg.packageName);
        }
        for (int realUserId : resolveUserIds(userId)) {
            try {
                this.mInstaller.destroyAppData(pkg.volumeUuid, pkg.packageName, realUserId, flags, ps != null ? ps.getCeDataInode(realUserId) : 0);
            } catch (InstallerException e) {
                Slog.w(TAG, String.valueOf(e));
            }
            this.mDexManager.notifyPackageDataDestroyed(pkg.packageName, userId);
        }
    }

    private void destroyAppProfilesLIF(Package pkg, int userId) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        destroyAppProfilesLeafLIF(pkg);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            destroyAppProfilesLeafLIF((Package) pkg.childPackages.get(i));
        }
    }

    private void destroyAppProfilesLeafLIF(Package pkg) {
        try {
            this.mInstaller.destroyAppProfiles(pkg.packageName);
        } catch (InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    private void clearAppProfilesLIF(Package pkg, int userId) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        clearAppProfilesLeafLIF(pkg);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            clearAppProfilesLeafLIF((Package) pkg.childPackages.get(i));
        }
    }

    private void clearAppProfilesLeafLIF(Package pkg) {
        try {
            this.mInstaller.clearAppProfiles(pkg.packageName);
        } catch (InstallerException e) {
            Slog.w(TAG, String.valueOf(e));
        }
    }

    private void setInstallAndUpdateTime(Package pkg, long firstInstallTime, long lastUpdateTime) {
        PackageSetting ps = pkg.mExtras;
        if (ps != null) {
            ps.firstInstallTime = firstInstallTime;
            ps.lastUpdateTime = lastUpdateTime;
        }
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            ps = ((Package) pkg.childPackages.get(i)).mExtras;
            if (ps != null) {
                ps.firstInstallTime = firstInstallTime;
                ps.lastUpdateTime = lastUpdateTime;
            }
        }
    }

    private void addSharedLibraryLPr(ArraySet<String> usesLibraryFiles, SharedLibraryEntry file, Package changingLib) {
        if (file.path != null) {
            usesLibraryFiles.add(file.path);
            return;
        }
        Package p = (Package) this.mPackages.get(file.apk);
        if (changingLib != null && changingLib.packageName.equals(file.apk) && (p == null || p.packageName.equals(changingLib.packageName))) {
            p = changingLib;
        }
        if (p != null) {
            usesLibraryFiles.addAll(p.getAllCodePaths());
            if (p.usesLibraryFiles != null) {
                Collections.addAll(usesLibraryFiles, p.usesLibraryFiles);
            }
        }
    }

    private void updateSharedLibrariesLPr(Package pkg, Package changingLib) throws PackageManagerException {
        if (pkg != null) {
            ArraySet arraySet = null;
            if (pkg.usesLibraries != null) {
                arraySet = addSharedLibrariesLPw(pkg.usesLibraries, null, null, pkg.packageName, changingLib, true, pkg.applicationInfo.targetSdkVersion, null);
            }
            if (pkg.usesStaticLibraries != null) {
                arraySet = addSharedLibrariesLPw(pkg.usesStaticLibraries, pkg.usesStaticLibrariesVersions, pkg.usesStaticLibrariesCertDigests, pkg.packageName, changingLib, true, pkg.applicationInfo.targetSdkVersion, arraySet);
            }
            if (pkg.usesOptionalLibraries != null) {
                arraySet = addSharedLibrariesLPw(pkg.usesOptionalLibraries, null, null, pkg.packageName, changingLib, false, pkg.applicationInfo.targetSdkVersion, arraySet);
            }
            if (ArrayUtils.isEmpty(arraySet)) {
                pkg.usesLibraryFiles = null;
            } else {
                pkg.usesLibraryFiles = (String[]) arraySet.toArray(new String[arraySet.size()]);
            }
        }
    }

    private ArraySet<String> addSharedLibrariesLPw(List<String> requestedLibraries, int[] requiredVersions, String[][] requiredCertDigests, String packageName, Package changingLib, boolean required, int targetSdk, ArraySet<String> outUsedLibraries) throws PackageManagerException {
        int libCount = requestedLibraries.size();
        for (int i = 0; i < libCount; i++) {
            int libVersion;
            String libName = (String) requestedLibraries.get(i);
            if (requiredVersions != null) {
                libVersion = requiredVersions[i];
            } else {
                libVersion = -1;
            }
            SharedLibraryEntry libEntry = getSharedLibraryEntryLPr(libName, libVersion);
            if (libEntry != null) {
                if (!(requiredVersions == null || requiredCertDigests == null)) {
                    if (libEntry.info.getVersion() != requiredVersions[i]) {
                        throw new PackageManagerException(-9, "Package " + packageName + " requires unavailable static shared" + " library " + libName + " version " + libEntry.info.getVersion() + "; failing!");
                    }
                    Package libPkg = (Package) this.mPackages.get(libEntry.apk);
                    if (libPkg == null) {
                        throw new PackageManagerException(-9, "Package " + packageName + " requires unavailable static shared" + " library; failing!");
                    }
                    String[] libCertDigests;
                    String[] expectedCertDigests = requiredCertDigests[i];
                    if (targetSdk > 26) {
                        libCertDigests = PackageUtils.computeSignaturesSha256Digests(libPkg.mSignatures);
                    } else {
                        libCertDigests = PackageUtils.computeSignaturesSha256Digests(new Signature[]{libPkg.mSignatures[0]});
                    }
                    if (expectedCertDigests.length != libCertDigests.length) {
                        throw new PackageManagerException(-9, "Package " + packageName + " requires differently signed" + " static sDexLoadReporter.java:45.19hared library; failing!");
                    }
                    Arrays.sort(libCertDigests);
                    Arrays.sort(expectedCertDigests);
                    int certCount = libCertDigests.length;
                    int j = 0;
                    while (j < certCount) {
                        if (libCertDigests[j].equalsIgnoreCase(expectedCertDigests[j])) {
                            j++;
                        } else {
                            throw new PackageManagerException(-9, "Package " + packageName + " requires differently signed" + " static shared library; failing!");
                        }
                    }
                }
                if (outUsedLibraries == null) {
                    outUsedLibraries = new ArraySet();
                }
                addSharedLibraryLPr(outUsedLibraries, libEntry, changingLib);
            } else if (required) {
                throw new PackageManagerException(-9, "Package " + packageName + " requires unavailable shared library " + libName + "; failing!");
            }
        }
        return outUsedLibraries;
    }

    private static boolean hasString(List<String> list, List<String> which) {
        if (list == null) {
            return false;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            for (int j = which.size() - 1; j >= 0; j--) {
                if (((String) which.get(j)).equals(list.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    protected ArrayList<Package> updateAllSharedLibrariesLPw(Package changingPkg) {
        ArrayList<Package> res = null;
        for (Package pkg : this.mPackages.values()) {
            if (changingPkg != null && (hasString(pkg.usesLibraries, changingPkg.libraryNames) ^ 1) != 0 && (hasString(pkg.usesOptionalLibraries, changingPkg.libraryNames) ^ 1) != 0 && (ArrayUtils.contains(pkg.usesStaticLibraries, changingPkg.staticSharedLibName) ^ 1) != 0) {
                return null;
            }
            if (res == null) {
                res = new ArrayList();
            }
            res.add(pkg);
            try {
                updateSharedLibrariesLPr(pkg, changingPkg);
            } catch (PackageManagerException e) {
                if (!pkg.isSystemApp() || pkg.isUpdatedSystemApp()) {
                    deletePackageLIF(pkg.packageName, null, true, sUserManager.getUserIds(), pkg.isUpdatedSystemApp() ? 1 : 0, null, true, null);
                }
                Slog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
            }
        }
        return res;
    }

    private static String deriveAbiOverride(String abiOverride, PackageSetting settings) {
        if (INSTALL_PACKAGE_SUFFIX.equals(abiOverride)) {
            return null;
        }
        if (abiOverride != null) {
            return abiOverride;
        }
        if (settings != null) {
            return settings.cpuAbiOverrideString;
        }
        return null;
    }

    private Package scanPackageTracedLI(Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        Trace.traceBegin(262144, "scanPackage");
        if ((scanFlags & 8192) != 0) {
            scanFlags &= -8193;
        } else if (pkg.childPackages != null && pkg.childPackages.size() > 0) {
            scanFlags |= 8192;
        }
        try {
            Package scannedPkg = scanPackageLI(pkg, policyFlags, scanFlags, currentTime, user);
            int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                scanPackageLI((Package) pkg.childPackages.get(i), policyFlags, scanFlags, currentTime, user);
            }
            if ((scanFlags & 8192) != 0) {
                return scanPackageTracedLI(pkg, policyFlags, scanFlags, currentTime, user);
            }
            return scannedPkg;
        } finally {
            Trace.traceEnd(262144);
        }
    }

    private Package scanPackageLI(Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        return scanPackageLI(pkg, policyFlags, scanFlags, currentTime, user, 0);
    }

    private Package scanPackageLI(Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user, int hwFlags) throws PackageManagerException {
        boolean success = false;
        try {
            if (!isSystemApp(pkg) && HwDeviceManager.disallowOp(7, pkg.packageName)) {
                UiThread.getHandler().post(new Runnable() {
                    public void run() {
                        if (PackageManagerService.this.mContext != null) {
                            Toast toast = Toast.makeText(PackageManagerService.this.mContext, PackageManagerService.this.mContext.getString(33686055), 0);
                            LayoutParams windowParams = toast.getWindowParams();
                            windowParams.privateFlags |= 16;
                            toast.show();
                        }
                    }
                });
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "app is not in the installpackage_whitelist");
            } else if (isSystemApp(pkg) || !HwDeviceManager.disallowOp(19, pkg.packageName)) {
                Package res = scanPackageDirtyLI(pkg, policyFlags, scanFlags, currentTime, user, hwFlags);
                success = true;
                return res;
            } else {
                final String pkgName = getCallingAppName(this.mContext, pkg);
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        Toast.makeText(PackageManagerService.this.mContext, PackageManagerService.this.mContext.getResources().getString(33685933, new Object[]{pkgName}), 0).show();
                    }
                }, 500);
                throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "app is in the installpackage_blacklist");
            }
        } finally {
            if (!(success || (scanFlags & 256) == 0)) {
                destroyAppDataLIF(pkg, -1, 3);
                destroyAppProfilesLIF(pkg, -1);
            }
        }
    }

    private static boolean apkHasCode(String fileName) {
        Throwable th;
        boolean z = false;
        StrictJarFile strictJarFile = null;
        try {
            StrictJarFile jarFile = new StrictJarFile(fileName, false, false);
            try {
                if (jarFile.findEntry("classes.dex") != null) {
                    z = true;
                }
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
                }
                return z;
            } catch (IOException e2) {
                strictJarFile = jarFile;
                if (strictJarFile != null) {
                    try {
                        strictJarFile.close();
                    } catch (IOException e3) {
                    }
                }
                return false;
            } catch (Throwable th2) {
                th = th2;
                strictJarFile = jarFile;
                if (strictJarFile != null) {
                    try {
                        strictJarFile.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
            if (strictJarFile != null) {
                strictJarFile.close();
            }
            return false;
        } catch (Throwable th3) {
            th = th3;
            if (strictJarFile != null) {
                strictJarFile.close();
            }
            throw th;
        }
    }

    private static void assertCodePolicy(Package pkg) throws PackageManagerException {
        if (((pkg.applicationInfo.flags & 4) != 0) && (apkHasCode(pkg.baseCodePath) ^ 1) != 0) {
            throw new PackageManagerException(-2, "Package " + pkg.baseCodePath + " code is missing");
        } else if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
            int i = 0;
            while (i < pkg.splitCodePaths.length) {
                if (!((pkg.splitFlags[i] & 4) != 0) || (apkHasCode(pkg.splitCodePaths[i]) ^ 1) == 0) {
                    i++;
                } else {
                    throw new PackageManagerException(-2, "Package " + pkg.splitCodePaths[i] + " code is missing");
                }
            }
        }
    }

    private Package scanPackageDirtyLI(Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user) throws PackageManagerException {
        return scanPackageDirtyLI(pkg, policyFlags, scanFlags, currentTime, user, 0);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Package scanPackageDirtyLI(Package pkg, int policyFlags, int scanFlags, long currentTime, UserHandle user, int hwFlags) throws PackageManagerException {
        Throwable th;
        applyPolicy(pkg, policyFlags, hwFlags);
        assertPackageIsValid(pkg, policyFlags, scanFlags, hwFlags);
        File file = new File(pkg.codePath);
        File destCodeFile = new File(pkg.applicationInfo.getCodePath());
        File destResourceFile = new File(pkg.applicationInfo.getResourcePath());
        SharedUserSetting suid = null;
        PackageSetting nonMutatedPs = null;
        String str = null;
        String secondaryCpuAbiFromSettings = null;
        synchronized (this.mPackages) {
            PackageSetting pkgSetting;
            PackageSetting signatureCheckPs;
            try {
                int i;
                PackageSetting foundPs;
                PackageSetting pkgSetting2;
                PackageSetting oldPkgSetting;
                if (pkg.mSharedUserId != null) {
                    suid = this.mSettings.getSharedUserLPw(pkg.mSharedUserId, 0, 0, true);
                }
                PackageSetting packageSetting = null;
                String realName = null;
                if (pkg.mOriginalPackages != null) {
                    String renamed = this.mSettings.getRenamedPackageLPr(pkg.mRealPackage);
                    if (pkg.mOriginalPackages.contains(renamed)) {
                        realName = pkg.mRealPackage;
                        if (!pkg.packageName.equals(renamed)) {
                            pkg.setPackageName(renamed);
                        }
                    } else {
                        for (i = pkg.mOriginalPackages.size() - 1; i >= 0; i--) {
                            packageSetting = this.mSettings.getPackageLPr((String) pkg.mOriginalPackages.get(i));
                            if (packageSetting != null) {
                                if (verifyPackageUpdateLPr(packageSetting, pkg)) {
                                    if (packageSetting.sharedUser == null || packageSetting.sharedUser.name.equals(pkg.mSharedUserId)) {
                                        break;
                                    }
                                    Slog.w(TAG, "Unable to migrate data from " + packageSetting.name + " to " + pkg.packageName + ": old uid " + packageSetting.sharedUser.name + " differs from " + pkg.mSharedUserId);
                                    packageSetting = null;
                                } else {
                                    packageSetting = null;
                                }
                            }
                        }
                    }
                }
                if (this.mTransferedPackages.contains(pkg.packageName)) {
                    Slog.w(TAG, "Package " + pkg.packageName + " was transferred to another, but its .apk remains");
                }
                if ((scanFlags & 8192) != 0) {
                    foundPs = this.mSettings.getPackageLPr(pkg.packageName);
                    if (foundPs != null) {
                        nonMutatedPs = new PackageSetting(foundPs);
                    }
                }
                if ((65536 & scanFlags) == 0) {
                    foundPs = this.mSettings.getPackageLPr(pkg.packageName);
                    if (foundPs != null) {
                        str = foundPs.primaryCpuAbiString;
                        secondaryCpuAbiFromSettings = foundPs.secondaryCpuAbiString;
                    }
                }
                pkgSetting = this.mSettings.getPackageLPr(pkg.packageName);
                if (pkgSetting == null || pkgSetting.sharedUser == suid) {
                    pkgSetting2 = pkgSetting;
                } else {
                    reportSettingsProblem(5, "Package " + pkg.packageName + " shared user changed from " + (pkgSetting.sharedUser != null ? pkgSetting.sharedUser.name : "<nothing>") + " to " + (suid != null ? suid.name : "<nothing>") + "; replacing with new");
                    pkgSetting2 = null;
                }
                if (pkgSetting2 == null) {
                    oldPkgSetting = null;
                } else {
                    PackageSetting packageSetting2 = new PackageSetting(pkgSetting2);
                }
                try {
                    PackageSetting disabledPkgSetting = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
                    Package packageR;
                    if (oldPkgSetting == null) {
                        packageR = null;
                    } else {
                        packageR = oldPkgSetting.pkg;
                    }
                    String[] strArr = null;
                    if (pkg.usesStaticLibraries != null) {
                        strArr = new String[pkg.usesStaticLibraries.size()];
                        pkg.usesStaticLibraries.toArray(strArr);
                    }
                    if (pkgSetting2 == null) {
                        pkgSetting = Settings.createNewSetting(pkg.packageName, packageSetting, disabledPkgSetting, realName, suid, destCodeFile, destResourceFile, pkg.applicationInfo.nativeLibraryRootDir, pkg.applicationInfo.primaryCpuAbi, pkg.applicationInfo.secondaryCpuAbi, pkg.mVersionCode, pkg.applicationInfo.flags, pkg.applicationInfo.privateFlags, user, true, (131072 & scanFlags) != 0, (524288 & scanFlags) != 0, pkg.parentPackage != null ? pkg.parentPackage.packageName : null, pkg.getChildPackageNames(), UserManagerService.getInstance(), strArr, pkg.usesStaticLibrariesVersions);
                        if (packageSetting != null) {
                            this.mSettings.addRenamedPackageLPw(pkg.packageName, packageSetting.name);
                        }
                        this.mSettings.addUserToSettingLPw(pkgSetting);
                    } else {
                        Settings.updatePackageSetting(pkgSetting2, disabledPkgSetting, suid, destCodeFile, pkg.applicationInfo.nativeLibraryDir, pkg.applicationInfo.primaryCpuAbi, pkg.applicationInfo.secondaryCpuAbi, pkg.applicationInfo.flags, pkg.applicationInfo.privateFlags, pkg.getChildPackageNames(), UserManagerService.getInstance(), strArr, pkg.usesStaticLibrariesVersions);
                        pkgSetting = pkgSetting2;
                    }
                    this.mSettings.writeUserRestrictionsLPw(pkgSetting, oldPkgSetting);
                    if (pkgSetting.origPackage != null) {
                        pkg.setPackageName(packageSetting.name);
                        reportSettingsProblem(5, "New package " + pkgSetting.realName + " renamed to replace old package " + pkgSetting.name);
                        if ((scanFlags & 8192) == 0) {
                            this.mTransferedPackages.add(packageSetting.name);
                        }
                        pkgSetting.origPackage = null;
                    }
                    if ((scanFlags & 8192) == 0 && realName != null) {
                        this.mTransferedPackages.add(pkg.packageName);
                    }
                    if (this.mSettings.isDisabledSystemPackageLPr(pkg.packageName)) {
                        ApplicationInfo applicationInfo = pkg.applicationInfo;
                        applicationInfo.flags |= 128;
                        updateFlagsForMarketSystemApp(pkg);
                        String disableSysPath = this.mSettings.getDisabledSysPackagesPath(pkg.packageName);
                        if (!(disableSysPath == null || new File(disableSysPath).exists())) {
                            Log.i(TAG, "sysPackagesPath " + disableSysPath + ", has removed, remove its FLAG_SYSTEM & removeDisabledSystemPackageLPw");
                            applicationInfo = pkg.applicationInfo;
                            applicationInfo.flags &= -2;
                            applicationInfo = pkg.applicationInfo;
                            applicationInfo.flags &= -129;
                            this.mSettings.removeDisabledSystemPackageLPw(pkg.packageName);
                        }
                    }
                    if ((1073741824 & hwFlags) != 0 || ((scanFlags & 64) == 0 && (policyFlags & 64) == 0)) {
                        updateSharedLibrariesLPr(pkg, null);
                    } else {
                        String deletedSysAppName = this.mSettings.getDisabledSystemPackageName(pkg.codePath);
                        if (deletedSysAppName != null) {
                            if ((deletedSysAppName.equals(pkg.packageName) ^ 1) != 0 && (this.mCustPms == null || this.mCustPms.isListedApp(deletedSysAppName) == -1)) {
                                Log.i(TAG, "deletedSysAppName " + deletedSysAppName + ", IN " + pkg.codePath + ", REMOVED");
                                this.mSettings.removeDisabledSystemPackageLPw(deletedSysAppName);
                            }
                        }
                    }
                    if (this.mFoundPolicyFile) {
                        SELinuxMMAC.assignSeInfoValue(pkg);
                    }
                    pkg.applicationInfo.uid = pkgSetting.appId;
                    pkg.mExtras = pkgSetting;
                    signatureCheckPs = pkgSetting;
                    if (pkg.applicationInfo.isStaticSharedLibrary()) {
                        SharedLibraryEntry libraryEntry = getLatestSharedLibraVersionLPr(pkg);
                        if (libraryEntry != null) {
                            signatureCheckPs = this.mSettings.getPackageLPr(libraryEntry.apk);
                        }
                    }
                    if (!shouldCheckUpgradeKeySetLP(signatureCheckPs, scanFlags)) {
                        verifySignaturesLP(signatureCheckPs, pkg);
                        pkgSetting.signatures.mSignatures = pkg.mSignatures;
                    } else if (checkUpgradeKeySetLP(signatureCheckPs, pkg)) {
                        pkgSetting.signatures.mSignatures = pkg.mSignatures;
                    } else if ((policyFlags & 64) == 0) {
                        throw new PackageManagerException(-7, "Package " + pkg.packageName + " upgrade keys do not match the " + "previously installed version");
                    } else {
                        pkgSetting.signatures.mSignatures = pkg.mSignatures;
                        reportSettingsProblem(5, "System package " + pkg.packageName + " signature changed; retaining data.");
                    }
                    if ((scanFlags & 8192) == 0 && pkg.mAdoptPermissions != null) {
                        for (i = pkg.mAdoptPermissions.size() - 1; i >= 0; i--) {
                            String origName = (String) pkg.mAdoptPermissions.get(i);
                            PackageSetting orig = this.mSettings.getPackageLPr(origName);
                            if (orig != null && verifyPackageUpdateLPr(orig, pkg)) {
                                Slog.i(TAG, "Adopting permissions from " + origName + " to " + pkg.packageName);
                                this.mSettings.transferPermissionsLPw(origName, pkg.packageName);
                            }
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    pkgSetting = pkgSetting2;
                    throw th;
                }
            } catch (PackageManagerException e) {
                if ((policyFlags & 64) == 0) {
                    throw e;
                }
                boolean isSystemSignUpdate = isSystemSignatureUpdated(pkgSetting.signatures.mSignatures, pkg.mSignatures);
                pkgSetting.signatures.mSignatures = pkg.mSignatures;
                if (signatureCheckPs.sharedUser == null || (isSystemSignUpdate ^ 1) == 0 || compareSignatures(signatureCheckPs.sharedUser.signatures.mSignatures, pkg.mSignatures) == 0) {
                    reportSettingsProblem(5, "System package " + pkg.packageName + " signature changed; retaining data.");
                } else {
                    throw new PackageManagerException(-104, "Signature mismatch for shared user: " + pkgSetting.sharedUser);
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    private void applyPolicy(Package pkg, int policyFlags, int hwFlags) {
        ApplicationInfo applicationInfo;
        if ((policyFlags & 1) != 0) {
            applicationInfo = pkg.applicationInfo;
            applicationInfo.flags |= 1;
            if (pkg.applicationInfo.isDirectBootAware()) {
                ActivityInfo activityInfo;
                for (Service s : pkg.services) {
                    ServiceInfo serviceInfo = s.info;
                    s.info.directBootAware = true;
                    serviceInfo.encryptionAware = true;
                }
                for (Provider p : pkg.providers) {
                    ProviderInfo providerInfo = p.info;
                    p.info.directBootAware = true;
                    providerInfo.encryptionAware = true;
                }
                for (Activity a : pkg.activities) {
                    activityInfo = a.info;
                    a.info.directBootAware = true;
                    activityInfo.encryptionAware = true;
                }
                for (Activity r : pkg.receivers) {
                    activityInfo = r.info;
                    r.info.directBootAware = true;
                    activityInfo.encryptionAware = true;
                }
            }
            if (compressedFileExists(pkg.codePath)) {
                pkg.isStub = true;
            }
        } else {
            pkg.coreApp = false;
            applicationInfo = pkg.applicationInfo;
            applicationInfo.privateFlags &= -33;
            applicationInfo = pkg.applicationInfo;
            applicationInfo.privateFlags &= -65;
        }
        addFlagsForRemovablePreApk(pkg, hwFlags);
        addFlagsForUpdatedRemovablePreApk(pkg, hwFlags);
        pkg.mTrustedOverlay = (policyFlags & 512) != 0;
        if ((policyFlags & 128) != 0) {
            applicationInfo = pkg.applicationInfo;
            applicationInfo.privateFlags |= 8;
        }
        if (!isSystemApp(pkg)) {
            pkg.mOriginalPackages = null;
            pkg.mRealPackage = null;
            pkg.mAdoptPermissions = null;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void assertPackageIsValid(Package pkg, int policyFlags, int scanFlags, int hwFlags) throws PackageManagerException {
        if ((policyFlags & 1024) != 0) {
            assertCodePolicy(pkg);
        }
        if (pkg.applicationInfo.getCodePath() == null || pkg.applicationInfo.getResourcePath() == null) {
            throw new PackageManagerException(-2, "Code and resource paths haven't been set correctly");
        }
        this.mSettings.mKeySetManagerService.assertScannedPackageValid(pkg);
        synchronized (this.mPackages) {
            if (!pkg.packageName.equals(PLATFORM_PACKAGE_NAME) || this.mAndroidApplication == null) {
                int i;
                if ((1073741824 & hwFlags) == 0) {
                    if (this.mPackages.containsKey(pkg.packageName)) {
                        throw new PackageManagerException(-5, "Application package " + pkg.packageName + " already installed.  Skipping duplicate.");
                    }
                }
                if (pkg.applicationInfo.isStaticSharedLibrary()) {
                    if (this.mPackages.containsKey(pkg.manifestPackageName)) {
                        throw new PackageManagerException("Duplicate static shared lib provider package");
                    } else if (pkg.applicationInfo.targetSdkVersion < 26) {
                        throw new PackageManagerException("Packages declaring static-shared libs must target O SDK or higher");
                    } else if ((131072 & scanFlags) != 0) {
                        throw new PackageManagerException("Packages declaring static-shared libs cannot be instant apps");
                    } else if (!ArrayUtils.isEmpty(pkg.mOriginalPackages)) {
                        throw new PackageManagerException("Packages declaring static-shared libs cannot be renamed");
                    } else if (!ArrayUtils.isEmpty(pkg.childPackages)) {
                        throw new PackageManagerException("Packages declaring static-shared libs cannot have child packages");
                    } else if (!ArrayUtils.isEmpty(pkg.libraryNames)) {
                        throw new PackageManagerException("Packages declaring static-shared libs cannot declare dynamic libs");
                    } else if (pkg.mSharedUserId != null) {
                        throw new PackageManagerException("Packages declaring static-shared libs cannot declare shared users");
                    } else if (!pkg.activities.isEmpty()) {
                        throw new PackageManagerException("Static shared libs cannot declare activities");
                    } else if (!pkg.services.isEmpty()) {
                        throw new PackageManagerException("Static shared libs cannot declare services");
                    } else if (!pkg.providers.isEmpty()) {
                        throw new PackageManagerException("Static shared libs cannot declare content providers");
                    } else if (!pkg.receivers.isEmpty()) {
                        throw new PackageManagerException("Static shared libs cannot declare broadcast receivers");
                    } else if (!pkg.permissionGroups.isEmpty()) {
                        throw new PackageManagerException("Static shared libs cannot declare permission groups");
                    } else if (!pkg.permissions.isEmpty()) {
                        throw new PackageManagerException("Static shared libs cannot declare permissions");
                    } else if (pkg.protectedBroadcasts != null) {
                        throw new PackageManagerException("Static shared libs cannot declare protected broadcasts");
                    } else if (pkg.mOverlayTarget != null) {
                        throw new PackageManagerException("Static shared libs cannot be overlay targets");
                    } else {
                        int minVersionCode = Integer.MIN_VALUE;
                        int maxVersionCode = HwBootFail.STAGE_BOOT_SUCCESS;
                        SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.get(pkg.staticSharedLibName);
                        if (versionedLib != null) {
                            int versionCount = versionedLib.size();
                            for (i = 0; i < versionCount; i++) {
                                SharedLibraryInfo libInfo = ((SharedLibraryEntry) versionedLib.valueAt(i)).info;
                                int libVersionCode = libInfo.getDeclaringPackage().getVersionCode();
                                if (libInfo.getVersion() >= pkg.staticSharedLibVersion) {
                                    if (libInfo.getVersion() <= pkg.staticSharedLibVersion) {
                                        maxVersionCode = libVersionCode;
                                        minVersionCode = libVersionCode;
                                        break;
                                    }
                                    maxVersionCode = Math.min(maxVersionCode, libVersionCode - 1);
                                } else {
                                    minVersionCode = Math.max(minVersionCode, libVersionCode + 1);
                                }
                            }
                        }
                        if (pkg.mVersionCode < minVersionCode || pkg.mVersionCode > maxVersionCode) {
                            throw new PackageManagerException("Static shared lib version codes must be ordered as lib versions");
                        }
                    }
                }
                if (!(pkg.childPackages == null || (pkg.childPackages.isEmpty() ^ 1) == 0)) {
                    if ((policyFlags & 128) == 0) {
                        throw new PackageManagerException("Only privileged apps can add child packages. Ignoring package " + pkg.packageName);
                    }
                    int childCount = pkg.childPackages.size();
                    for (i = 0; i < childCount; i++) {
                        if (this.mSettings.hasOtherDisabledSystemPkgWithChildLPr(pkg.packageName, ((Package) pkg.childPackages.get(i)).packageName)) {
                            throw new PackageManagerException("Can't override child of another disabled app. Ignoring package " + pkg.packageName);
                        }
                    }
                }
                if ((scanFlags & 1024) != 0) {
                    if (this.mExpectingBetter.containsKey(pkg.packageName)) {
                        logCriticalInfo(5, "Relax SCAN_REQUIRE_KNOWN requirement for package " + pkg.packageName);
                    } else {
                        PackageSetting known = this.mSettings.getPackageLPr(pkg.packageName);
                        if (known != null) {
                            if (pkg.applicationInfo.getCodePath().equals(known.codePathString)) {
                            }
                            throw new PackageManagerException(-23, "Application package " + pkg.packageName + " found at " + pkg.applicationInfo.getCodePath() + " but expected at " + known.codePathString + "; ignoring.");
                        }
                        throw new PackageManagerException(-19, "Application package " + pkg.packageName + " not found; ignoring.");
                    }
                }
                if ((scanFlags & 16) != 0) {
                    int N = pkg.providers.size();
                    for (i = 0; i < N; i++) {
                        Provider p = (Provider) pkg.providers.get(i);
                        if (p.info.authority != null) {
                            String[] names = p.info.authority.split(";");
                            for (int j = 0; j < names.length; j++) {
                                if (this.mProvidersByAuthority.containsKey(names[j])) {
                                    Provider other = (Provider) this.mProvidersByAuthority.get(names[j]);
                                    String otherPackageName = (other == null || other.getComponentName() == null) ? "?" : other.getComponentName().getPackageName();
                                    throw new PackageManagerException(-13, "Can't install because provider name " + names[j] + " (in package " + pkg.applicationInfo.packageName + ") is already used by " + otherPackageName);
                                }
                            }
                            continue;
                        }
                    }
                }
            } else {
                Slog.w(TAG, "*************************************************");
                Slog.w(TAG, "Core android package being redefined.  Skipping.");
                Slog.w(TAG, " codePath=" + pkg.codePath);
                Slog.w(TAG, "*************************************************");
                throw new PackageManagerException(-5, "Core android package being redefined.  Skipping.");
            }
        }
    }

    private boolean addSharedLibraryLPw(String path, String apk, String name, int version, int type, String declaringPackageName, int declaringVersionCode) {
        SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.get(name);
        if (versionedLib == null) {
            versionedLib = new SparseArray();
            this.mSharedLibraries.put(name, versionedLib);
            if (type == 2) {
                this.mStaticLibsByDeclaringPackage.put(declaringPackageName, versionedLib);
            }
        } else if (versionedLib.indexOfKey(version) >= 0) {
            return false;
        }
        versionedLib.put(version, new SharedLibraryEntry(path, apk, name, version, type, declaringPackageName, declaringVersionCode));
        return true;
    }

    private boolean removeSharedLibraryLPw(String name, int version) {
        SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.get(name);
        if (versionedLib == null) {
            return false;
        }
        int libIdx = versionedLib.indexOfKey(version);
        if (libIdx < 0) {
            return false;
        }
        SharedLibraryEntry libEntry = (SharedLibraryEntry) versionedLib.valueAt(libIdx);
        versionedLib.remove(version);
        if (versionedLib.size() <= 0) {
            this.mSharedLibraries.remove(name);
            if (libEntry.info.getType() == 2) {
                this.mStaticLibsByDeclaringPackage.remove(libEntry.info.getDeclaringPackage().getPackageName());
            }
        }
        return true;
    }

    private void commitPackageSettings(Package pkg, PackageSetting pkgSetting, UserHandle user, int scanFlags, boolean chatty) throws PackageManagerException {
        int i;
        int j;
        String pkgName = pkg.packageName;
        if (this.mCustomResolverComponentName != null && this.mCustomResolverComponentName.getPackageName().equals(pkg.packageName)) {
            setUpCustomResolverActivity(pkg);
        }
        if (pkg.packageName.equals(PLATFORM_PACKAGE_NAME)) {
            Slog.w(TAG, "commitPackageSettings: Package " + pkg.packageName);
            synchronized (this.mPackages) {
                Slog.w(TAG, "commitPackageSettings: scanFlags & SCAN_CHECK_ONLY = " + (scanFlags & 8192));
                if ((scanFlags & 8192) == 0) {
                    this.mPlatformPackage = pkg;
                    pkg.mVersionCode = this.mSdkVersion;
                    this.mAndroidApplication = pkg.applicationInfo;
                    Slog.w(TAG, "commitPackageSettings: mResolverReplaced =  " + this.mResolverReplaced);
                    if (!this.mResolverReplaced) {
                        Slog.w(TAG, "commitPackageSettings: set mResolveActivity ");
                        this.mResolveActivity.applicationInfo = this.mAndroidApplication;
                        this.mResolveActivity.name = ResolverActivity.class.getName();
                        this.mResolveActivity.packageName = this.mAndroidApplication.packageName;
                        this.mResolveActivity.processName = "system:ui";
                        this.mResolveActivity.launchMode = 0;
                        this.mResolveActivity.documentLaunchMode = 3;
                        this.mResolveActivity.flags = 32;
                        this.mResolveActivity.theme = 16974374;
                        this.mResolveActivity.exported = true;
                        this.mResolveActivity.enabled = true;
                        this.mResolveActivity.resizeMode = 2;
                        this.mResolveActivity.configChanges = 3504;
                        this.mResolveInfo.activityInfo = this.mResolveActivity;
                        this.mResolveInfo.priority = 0;
                        this.mResolveInfo.preferredOrder = 0;
                        this.mResolveInfo.match = 0;
                        this.mResolveComponentName = new ComponentName(this.mAndroidApplication.packageName, this.mResolveActivity.name);
                    }
                }
            }
        }
        setGMSPackage(pkg);
        ArrayList arrayList = null;
        synchronized (this.mPackages) {
            boolean hasStaticSharedLibs = false;
            if (pkg.staticSharedLibName != null) {
                if (addSharedLibraryLPw(null, pkg.packageName, pkg.staticSharedLibName, pkg.staticSharedLibVersion, 2, pkg.manifestPackageName, pkg.mVersionCode)) {
                    hasStaticSharedLibs = true;
                } else {
                    Slog.w(TAG, "Package " + pkg.packageName + " library " + pkg.staticSharedLibName + " already exists; skipping");
                }
            }
            if (!(hasStaticSharedLibs || (pkg.applicationInfo.flags & 1) == 0 || pkg.libraryNames == null)) {
                for (i = 0; i < pkg.libraryNames.size(); i++) {
                    String name = (String) pkg.libraryNames.get(i);
                    boolean allowed = false;
                    if (pkg.isUpdatedSystemApp()) {
                        PackageSetting sysPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
                        if (sysPs.pkg != null && sysPs.pkg.libraryNames != null) {
                            for (j = 0; j < sysPs.pkg.libraryNames.size(); j++) {
                                if (name.equals(sysPs.pkg.libraryNames.get(j))) {
                                    allowed = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        allowed = true;
                    }
                    if (!allowed) {
                        Slog.w(TAG, "Package " + pkg.packageName + " declares lib " + name + " that is not declared on system image; skipping");
                    } else if (!addSharedLibraryLPw(null, pkg.packageName, name, -1, 1, pkg.packageName, pkg.mVersionCode)) {
                        Slog.w(TAG, "Package " + pkg.packageName + " library " + name + " already exists; skipping");
                    }
                }
                if ((scanFlags & 64) == 0) {
                    arrayList = updateAllSharedLibrariesLPw(pkg);
                }
            }
        }
        if ((scanFlags & 64) == 0 && (scanFlags & 16384) == 0 && (32768 & scanFlags) == 0) {
            checkPackageFrozen(pkgName);
        }
        if (arrayList != null) {
            for (i = 0; i < arrayList.size(); i++) {
                Package clientPkg = (Package) arrayList.get(i);
                killApplication(clientPkg.applicationInfo.packageName, clientPkg.applicationInfo.uid, "update lib");
            }
        }
        Trace.traceBegin(262144, "updateSettings");
        synchronized (this.mPackages) {
            this.mSettings.insertPackageSettingLPw(pkgSetting, pkg);
            updateCertCompatPackage(pkg, pkgSetting);
            this.mPackages.put(pkg.applicationInfo.packageName, pkg);
            Iterator<PackageCleanItem> iter = this.mSettings.mPackagesToBeCleaned.iterator();
            while (iter.hasNext()) {
                if (pkgName.equals(((PackageCleanItem) iter.next()).packageName)) {
                    iter.remove();
                }
            }
            this.mSettings.mKeySetManagerService.addScannedPackageLPw(pkg);
            int N = pkg.providers.size();
            StringBuilder r = null;
            for (i = 0; i < N; i++) {
                Provider p = (Provider) pkg.providers.get(i);
                p.info.processName = fixProcessName(pkg.applicationInfo.processName, p.info.processName);
                this.mProviders.addProvider(p);
                p.syncable = p.info.isSyncable;
                if (p.info.authority != null) {
                    String[] names = p.info.authority.split(";");
                    p.info.authority = null;
                    j = 0;
                    Provider p2 = p;
                    while (j < names.length) {
                        if (j != 1) {
                            p = p2;
                        } else if (p2.syncable) {
                            p = new Provider(p2);
                            p.syncable = false;
                        } else {
                            p = p2;
                        }
                        if (this.mProvidersByAuthority.containsKey(names[j])) {
                            Provider other = (Provider) this.mProvidersByAuthority.get(names[j]);
                            String str = TAG;
                            StringBuilder append = new StringBuilder().append("Skipping provider name ").append(names[j]).append(" (in package ").append(pkg.applicationInfo.packageName).append("): name already used by ");
                            String packageName = (other == null || other.getComponentName() == null) ? "?" : other.getComponentName().getPackageName();
                            Slog.w(str, append.append(packageName).toString());
                        } else {
                            this.mProvidersByAuthority.put(names[j], p);
                            if (p.info.authority == null) {
                                p.info.authority = names[j];
                            } else {
                                p.info.authority += ";" + names[j];
                            }
                        }
                        j++;
                        p2 = p;
                    }
                    p = p2;
                }
                if (chatty) {
                    if (r == null) {
                        StringBuilder stringBuilder = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append(p.info.name);
                }
            }
            N = pkg.services.size();
            r = null;
            for (i = 0; i < N; i++) {
                Service s = (Service) pkg.services.get(i);
                s.info.processName = fixProcessName(pkg.applicationInfo.processName, s.info.processName);
                this.mServices.addService(s);
                if (chatty) {
                    if (r == null) {
                        stringBuilder = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append(s.info.name);
                }
            }
            N = pkg.receivers.size();
            r = null;
            for (i = 0; i < N; i++) {
                Activity a = (Activity) pkg.receivers.get(i);
                a.info.processName = fixProcessName(pkg.applicationInfo.processName, a.info.processName);
                this.mReceivers.addActivity(a, HwBroadcastRadarUtil.KEY_RECEIVER);
                if (chatty) {
                    if (r == null) {
                        stringBuilder = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append(a.info.name);
                }
            }
            N = pkg.activities.size();
            r = null;
            for (i = 0; i < N; i++) {
                a = (Activity) pkg.activities.get(i);
                a.info.processName = fixProcessName(pkg.applicationInfo.processName, a.info.processName);
                this.mActivities.addActivity(a, "activity");
                if (chatty) {
                    if (r == null) {
                        stringBuilder = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append(a.info.name);
                }
            }
            N = pkg.permissionGroups.size();
            r = null;
            for (i = 0; i < N; i++) {
                PermissionGroup pg = (PermissionGroup) pkg.permissionGroups.get(i);
                PermissionGroup cur = (PermissionGroup) this.mPermissionGroups.get(pg.info.name);
                String str2 = cur == null ? null : cur.info.packageName;
                if ((131072 & scanFlags) != 0) {
                    Slog.w(TAG, "Permission group " + pg.info.name + " from package " + pg.info.packageName + " ignored: instant apps cannot define new permission groups.");
                } else {
                    boolean isPackageUpdate = pg.info.packageName.equals(str2);
                    if (cur == null || isPackageUpdate) {
                        this.mPermissionGroups.put(pg.info.name, pg);
                        if (chatty) {
                            if (r == null) {
                                stringBuilder = new StringBuilder(256);
                            } else {
                                r.append(' ');
                            }
                            if (isPackageUpdate) {
                                r.append("UPD:");
                            }
                            r.append(pg.info.name);
                        }
                    } else {
                        Slog.w(TAG, "Permission group " + pg.info.name + " from package " + pg.info.packageName + " ignored: original from " + cur.info.packageName);
                        if (chatty) {
                            if (r == null) {
                                stringBuilder = new StringBuilder(256);
                            } else {
                                r.append(' ');
                            }
                            r.append("DUP:");
                            r.append(pg.info.name);
                        }
                    }
                }
            }
            N = pkg.permissions.size();
            r = null;
            for (i = 0; i < N; i++) {
                Permission p3 = (Permission) pkg.permissions.get(i);
                if ((131072 & scanFlags) != 0) {
                    Slog.w(TAG, "Permission " + p3.info.name + " from package " + p3.info.packageName + " ignored: instant apps cannot define new permissions.");
                } else {
                    ArrayMap<String, BasePermission> permissionMap;
                    PermissionInfo permissionInfo = p3.info;
                    permissionInfo.flags &= -1073741825;
                    if (pkg.applicationInfo.targetSdkVersion > 22) {
                        p3.group = (PermissionGroup) this.mPermissionGroups.get(p3.info.group);
                    }
                    if (p3.tree) {
                        permissionMap = this.mSettings.mPermissionTrees;
                    } else {
                        permissionMap = this.mSettings.mPermissions;
                    }
                    BasePermission bp = (BasePermission) permissionMap.get(p3.info.name);
                    if (!(bp == null || (Objects.equals(bp.sourcePackage, p3.info.packageName) ^ 1) == 0)) {
                        boolean currentOwnerIsSystem;
                        if (bp.perm != null) {
                            currentOwnerIsSystem = isSystemApp(bp.perm.owner);
                        } else {
                            currentOwnerIsSystem = false;
                        }
                        if (isSystemApp(p3.owner)) {
                            if (bp.type == 1 && bp.perm == null) {
                                bp.packageSetting = pkgSetting;
                                bp.perm = p3;
                                bp.uid = pkg.applicationInfo.uid;
                                bp.sourcePackage = p3.info.packageName;
                                permissionInfo = p3.info;
                                permissionInfo.flags |= 1073741824;
                            } else if (!currentOwnerIsSystem) {
                                reportSettingsProblem(5, "New decl " + p3.owner + " of permission  " + p3.info.name + " is system; overriding " + bp.sourcePackage);
                                bp = null;
                            }
                        }
                    }
                    if (bp == null) {
                        bp = new BasePermission(p3.info.name, p3.info.packageName, 0);
                        permissionMap.put(p3.info.name, bp);
                    }
                    if (bp.perm == null) {
                        if (bp.sourcePackage == null || bp.sourcePackage.equals(p3.info.packageName)) {
                            BasePermission tree = findPermissionTreeLP(p3.info.name);
                            if (tree == null || tree.sourcePackage.equals(p3.info.packageName)) {
                                bp.packageSetting = pkgSetting;
                                bp.perm = p3;
                                bp.uid = pkg.applicationInfo.uid;
                                bp.sourcePackage = p3.info.packageName;
                                permissionInfo = p3.info;
                                permissionInfo.flags |= 1073741824;
                                if (chatty) {
                                    if (r == null) {
                                        stringBuilder = new StringBuilder(256);
                                    } else {
                                        r.append(' ');
                                    }
                                    r.append(p3.info.name);
                                }
                            } else {
                                Slog.w(TAG, "Permission " + p3.info.name + " from package " + p3.info.packageName + " ignored: base tree " + tree.name + " is from package " + tree.sourcePackage);
                            }
                        } else {
                            Slog.w(TAG, "Permission " + p3.info.name + " from package " + p3.info.packageName + " ignored: original from " + bp.sourcePackage);
                        }
                    } else if (chatty) {
                        if (r == null) {
                            stringBuilder = new StringBuilder(256);
                        } else {
                            r.append(' ');
                        }
                        r.append("DUP:");
                        r.append(p3.info.name);
                    }
                    if (bp.perm == p3) {
                        bp.protectionLevel = p3.info.protectionLevel;
                    }
                }
            }
            N = pkg.instrumentation.size();
            r = null;
            for (i = 0; i < N; i++) {
                Instrumentation a2 = (Instrumentation) pkg.instrumentation.get(i);
                a2.info.packageName = pkg.applicationInfo.packageName;
                a2.info.sourceDir = pkg.applicationInfo.sourceDir;
                a2.info.publicSourceDir = pkg.applicationInfo.publicSourceDir;
                a2.info.splitNames = pkg.splitNames;
                a2.info.splitSourceDirs = pkg.applicationInfo.splitSourceDirs;
                a2.info.splitPublicSourceDirs = pkg.applicationInfo.splitPublicSourceDirs;
                a2.info.splitDependencies = pkg.applicationInfo.splitDependencies;
                a2.info.dataDir = pkg.applicationInfo.dataDir;
                a2.info.deviceProtectedDataDir = pkg.applicationInfo.deviceProtectedDataDir;
                a2.info.credentialProtectedDataDir = pkg.applicationInfo.credentialProtectedDataDir;
                a2.info.nativeLibraryDir = pkg.applicationInfo.nativeLibraryDir;
                a2.info.secondaryNativeLibraryDir = pkg.applicationInfo.secondaryNativeLibraryDir;
                this.mInstrumentation.put(a2.getComponentName(), a2);
                if (chatty) {
                    if (r == null) {
                        stringBuilder = new StringBuilder(256);
                    } else {
                        r.append(' ');
                    }
                    r.append(a2.info.name);
                }
            }
            if (pkg.protectedBroadcasts != null) {
                N = pkg.protectedBroadcasts.size();
                synchronized (this.mProtectedBroadcasts) {
                    for (i = 0; i < N; i++) {
                        this.mProtectedBroadcasts.add((String) pkg.protectedBroadcasts.get(i));
                    }
                }
            }
        }
        Trace.traceEnd(262144);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void derivePackageAbi(Package pkg, File scanFile, String cpuAbiOverride, boolean extractLibs, File appLib32InstallDir) throws PackageManagerException {
        setNativeLibraryPaths(pkg, appLib32InstallDir);
        if (pkg.isForwardLocked() || pkg.applicationInfo.isExternalAsec() || (isSystemApp(pkg) && (pkg.isUpdatedSystemApp() ^ 1) != 0)) {
            extractLibs = false;
        }
        String nativeLibraryRootStr = pkg.applicationInfo.nativeLibraryRootDir;
        boolean useIsaSpecificSubdirs = pkg.applicationInfo.nativeLibraryRootRequiresIsa;
        AutoCloseable autoCloseable = null;
        try {
            autoCloseable = Handle.create(pkg);
            File nativeLibraryRoot = new File(nativeLibraryRootStr);
            pkg.applicationInfo.primaryCpuAbi = null;
            pkg.applicationInfo.secondaryCpuAbi = null;
            if (isMultiArch(pkg.applicationInfo)) {
                if (!(pkg.cpuAbiOverride == null || (INSTALL_PACKAGE_SUFFIX.equals(pkg.cpuAbiOverride) ^ 1) == 0)) {
                    Slog.w(TAG, "Ignoring abiOverride for multi arch application.");
                }
                int abi32 = -114;
                int abi64 = -114;
                if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                    if (extractLibs) {
                        Trace.traceBegin(262144, "copyNativeBinaries");
                        abi32 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(autoCloseable, nativeLibraryRoot, Build.SUPPORTED_32_BIT_ABIS, useIsaSpecificSubdirs);
                    } else {
                        Trace.traceBegin(262144, "findSupportedAbi");
                        abi32 = NativeLibraryHelper.findSupportedAbi(autoCloseable, Build.SUPPORTED_32_BIT_ABIS);
                    }
                    Trace.traceEnd(262144);
                }
                if (abi32 >= 0 && pkg.isLibrary() && extractLibs) {
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library native lib extraction not supported");
                }
                maybeThrowExceptionForMultiArchCopy("Error unpackaging 32 bit native libs for multiarch app.", abi32);
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    if (extractLibs) {
                        Trace.traceBegin(262144, "copyNativeBinaries");
                        abi64 = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(autoCloseable, nativeLibraryRoot, Build.SUPPORTED_64_BIT_ABIS, useIsaSpecificSubdirs);
                    } else {
                        Trace.traceBegin(262144, "findSupportedAbi");
                        abi64 = NativeLibraryHelper.findSupportedAbi(autoCloseable, Build.SUPPORTED_64_BIT_ABIS);
                    }
                    Trace.traceEnd(262144);
                }
                maybeThrowExceptionForMultiArchCopy("Error unpackaging 64 bit native libs for multiarch app.", abi64);
                if (abi64 >= 0) {
                    if (extractLibs && pkg.isLibrary()) {
                        throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library native lib extraction not supported");
                    }
                    pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[abi64];
                }
                if (abi32 >= 0) {
                    String abi = Build.SUPPORTED_32_BIT_ABIS[abi32];
                    if (abi64 < 0) {
                        pkg.applicationInfo.primaryCpuAbi = abi;
                    } else if (pkg.use32bitAbi) {
                        pkg.applicationInfo.secondaryCpuAbi = pkg.applicationInfo.primaryCpuAbi;
                        pkg.applicationInfo.primaryCpuAbi = abi;
                    } else {
                        pkg.applicationInfo.secondaryCpuAbi = abi;
                    }
                }
                Flog.i(203, "derivePackageAbi for MultiArch : " + pkg + ", path " + scanFile + ", need extractLibs " + extractLibs + ", abi32 " + abi32 + ", abi64 " + abi64);
            } else {
                int copyRet;
                String[] abiList = cpuAbiOverride != null ? new String[]{cpuAbiOverride} : Build.SUPPORTED_ABIS;
                boolean needsRenderScriptOverride = false;
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0 && cpuAbiOverride == null && NativeLibraryHelper.hasRenderscriptBitcode(autoCloseable)) {
                    abiList = Build.SUPPORTED_32_BIT_ABIS;
                    needsRenderScriptOverride = true;
                }
                if (extractLibs) {
                    Trace.traceBegin(262144, "copyNativeBinaries");
                    copyRet = NativeLibraryHelper.copyNativeBinariesForSupportedAbi(autoCloseable, nativeLibraryRoot, abiList, useIsaSpecificSubdirs);
                } else {
                    Trace.traceBegin(262144, "findSupportedAbi");
                    copyRet = NativeLibraryHelper.findSupportedAbi(autoCloseable, abiList);
                }
                Trace.traceEnd(262144);
                if (copyRet < 0 && copyRet != -114) {
                    throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Error unpackaging native libs for app, errorCode=" + copyRet);
                } else if (copyRet >= 0) {
                    if (pkg.isLibrary()) {
                        throw new PackageManagerException(RequestStatus.SYS_ETIMEDOUT, "Shared library with native libs must be multiarch");
                    }
                    pkg.applicationInfo.primaryCpuAbi = abiList[copyRet];
                } else if (copyRet == -114 && cpuAbiOverride != null) {
                    pkg.applicationInfo.primaryCpuAbi = cpuAbiOverride;
                } else if (needsRenderScriptOverride) {
                    pkg.applicationInfo.primaryCpuAbi = abiList[0];
                }
            }
            IoUtils.closeQuietly(autoCloseable);
        } catch (IOException ioe) {
            Slog.e(TAG, "Unable to get canonical file " + ioe.toString());
        } catch (Throwable th) {
            IoUtils.closeQuietly(autoCloseable);
        }
        setNativeLibraryPaths(pkg, appLib32InstallDir);
    }

    private void adjustCpuAbisForSharedUserLPw(Set<PackageSetting> packagesForUser, Package scannedPackage) {
        String requiredInstructionSet = null;
        if (!(scannedPackage == null || scannedPackage.applicationInfo.primaryCpuAbi == null)) {
            requiredInstructionSet = VMRuntime.getInstructionSet(scannedPackage.applicationInfo.primaryCpuAbi);
        }
        PackageSetting requirer = null;
        for (PackageSetting ps : packagesForUser) {
            if ((scannedPackage == null || (scannedPackage.packageName.equals(ps.name) ^ 1) != 0) && ps.primaryCpuAbiString != null) {
                String instructionSet = VMRuntime.getInstructionSet(ps.primaryCpuAbiString);
                if (!(requiredInstructionSet == null || (instructionSet.equals(requiredInstructionSet) ^ 1) == 0)) {
                    Object obj;
                    StringBuilder append = new StringBuilder().append("Instruction set mismatch, ");
                    if (requirer == null) {
                        obj = "[caller]";
                    } else {
                        PackageSetting packageSetting = requirer;
                    }
                    Slog.w(TAG, append.append(obj).append(" requires ").append(requiredInstructionSet).append(" whereas ").append(ps).append(" requires ").append(instructionSet).toString());
                }
                if (requiredInstructionSet == null) {
                    requiredInstructionSet = instructionSet;
                    requirer = ps;
                }
            }
        }
        if (requiredInstructionSet != null) {
            String adjustedAbi;
            if (requirer != null) {
                adjustedAbi = requirer.primaryCpuAbiString;
                if (scannedPackage != null) {
                    scannedPackage.applicationInfo.primaryCpuAbi = adjustedAbi;
                }
            } else {
                adjustedAbi = scannedPackage.applicationInfo.primaryCpuAbi;
            }
            for (PackageSetting ps2 : packagesForUser) {
                if ((scannedPackage == null || (scannedPackage.packageName.equals(ps2.name) ^ 1) != 0) && ps2.primaryCpuAbiString == null) {
                    if (SystemProperties.get("persist.sys.shareduid_abi_check", PACKAGE_PARSER_CACHE_VERSION).equals("0")) {
                        ps2.primaryCpuAbiString = adjustedAbi;
                    }
                    if (!(ps2.pkg == null || ps2.pkg.applicationInfo == null || (TextUtils.equals(adjustedAbi, ps2.pkg.applicationInfo.primaryCpuAbi) ^ 1) == 0)) {
                        if (SystemProperties.get("persist.sys.shareduid_abi_check", PACKAGE_PARSER_CACHE_VERSION).equals("0")) {
                            ps2.pkg.applicationInfo.primaryCpuAbi = adjustedAbi;
                        }
                        if (SystemProperties.get("persist.sys.shareduid_abi_check", PACKAGE_PARSER_CACHE_VERSION).equals("0")) {
                            try {
                                this.mInstaller.rmdex(ps2.codePathString, InstructionSets.getDexCodeInstructionSet(InstructionSets.getPreferredInstructionSet()));
                            } catch (InstallerException e) {
                            }
                        }
                    }
                }
            }
        }
    }

    void setUpCustomResolverActivity(Package pkg) {
        Slog.w(TAG, "setUpCustomResolverActivity");
        synchronized (this.mPackages) {
            this.mResolverReplaced = true;
            this.mResolveActivity.applicationInfo = pkg.applicationInfo;
            this.mResolveActivity.name = this.mCustomResolverComponentName.getClassName();
            this.mResolveActivity.packageName = pkg.applicationInfo.packageName;
            this.mResolveActivity.processName = pkg.applicationInfo.packageName;
            this.mResolveActivity.launchMode = 0;
            this.mResolveActivity.flags = 288;
            this.mResolveActivity.theme = 0;
            this.mResolveActivity.exported = true;
            this.mResolveActivity.enabled = true;
            this.mResolveInfo.activityInfo = this.mResolveActivity;
            this.mResolveInfo.priority = 0;
            this.mResolveInfo.preferredOrder = 0;
            this.mResolveInfo.match = 0;
            this.mResolveComponentName = this.mCustomResolverComponentName;
            Slog.i(TAG, "Replacing default ResolverActivity with custom activity: " + this.mResolveComponentName);
        }
    }

    private void setUpInstantAppInstallerActivityLP(ActivityInfo installerActivity) {
        if (installerActivity == null) {
            if (DEBUG_EPHEMERAL) {
                Slog.d(TAG, "Clear ephemeral installer activity");
            }
            this.mInstantAppInstallerActivity = null;
            return;
        }
        if (DEBUG_EPHEMERAL) {
            Slog.d(TAG, "Set ephemeral installer activity: " + installerActivity.getComponentName());
        }
        this.mInstantAppInstallerActivity = installerActivity;
        ActivityInfo activityInfo = this.mInstantAppInstallerActivity;
        activityInfo.flags |= 288;
        this.mInstantAppInstallerActivity.exported = true;
        this.mInstantAppInstallerActivity.enabled = true;
        this.mInstantAppInstallerInfo.activityInfo = this.mInstantAppInstallerActivity;
        this.mInstantAppInstallerInfo.priority = 0;
        this.mInstantAppInstallerInfo.preferredOrder = 1;
        this.mInstantAppInstallerInfo.isDefault = true;
        this.mInstantAppInstallerInfo.match = 5799936;
    }

    private static String calculateBundledApkRoot(String codePathString) {
        File codeRoot;
        File codePath = new File(codePathString);
        if (FileUtils.contains(Environment.getRootDirectory(), codePath)) {
            codeRoot = Environment.getRootDirectory();
        } else if (FileUtils.contains(Environment.getOemDirectory(), codePath)) {
            codeRoot = Environment.getOemDirectory();
        } else if (FileUtils.contains(Environment.getVendorDirectory(), codePath)) {
            codeRoot = Environment.getVendorDirectory();
        } else {
            try {
                File f = codePath.getCanonicalFile();
                File parent = f.getParentFile();
                while (true) {
                    File tmp = parent.getParentFile();
                    if (tmp == null) {
                        break;
                    }
                    f = parent;
                    parent = tmp;
                }
                codeRoot = f;
                Slog.w(TAG, "Unrecognized code path " + codePath + " - using " + codeRoot);
            } catch (IOException e) {
                Slog.w(TAG, "Can't canonicalize code path " + codePath);
                return Environment.getRootDirectory().getPath();
            }
        }
        return codeRoot.getPath();
    }

    protected static void setNativeLibraryPaths(Package pkg, File appLib32InstallDir) {
        ApplicationInfo info = pkg.applicationInfo;
        String codePath = pkg.codePath;
        File codeFile = new File(codePath);
        int isUpdatedSystemApp = info.isSystemApp() ? info.isUpdatedSystemApp() ^ 1 : 0;
        boolean isExternalAsec = !info.isForwardLocked() ? info.isExternalAsec() : true;
        info.nativeLibraryRootDir = null;
        info.nativeLibraryRootRequiresIsa = false;
        info.nativeLibraryDir = null;
        info.secondaryNativeLibraryDir = null;
        if (PackageParser.isApkFile(codeFile)) {
            if (isUpdatedSystemApp != 0) {
                String apkRoot = calculateBundledApkRoot(info.sourceDir);
                boolean is64Bit = VMRuntime.is64BitInstructionSet(InstructionSets.getPrimaryInstructionSet(info));
                String apkName = deriveCodePathName(codePath);
                String libDir = is64Bit ? "lib64" : "lib";
                info.nativeLibraryRootDir = Environment.buildPath(new File(apkRoot), new String[]{libDir, apkName}).getAbsolutePath();
                if (info.secondaryCpuAbi != null) {
                    String secondaryLibDir = is64Bit ? "lib" : "lib64";
                    info.secondaryNativeLibraryDir = Environment.buildPath(new File(apkRoot), new String[]{secondaryLibDir, apkName}).getAbsolutePath();
                }
            } else if (isExternalAsec) {
                info.nativeLibraryRootDir = new File(codeFile.getParentFile(), "lib").getAbsolutePath();
            } else {
                info.nativeLibraryRootDir = new File(appLib32InstallDir, deriveCodePathName(codePath)).getAbsolutePath();
            }
            info.nativeLibraryRootRequiresIsa = false;
            info.nativeLibraryDir = info.nativeLibraryRootDir;
            return;
        }
        info.nativeLibraryRootDir = new File(codeFile, "lib").getAbsolutePath();
        info.nativeLibraryRootRequiresIsa = true;
        info.nativeLibraryDir = new File(info.nativeLibraryRootDir, InstructionSets.getPrimaryInstructionSet(info)).getAbsolutePath();
        if (info.secondaryCpuAbi != null) {
            info.secondaryNativeLibraryDir = new File(info.nativeLibraryRootDir, VMRuntime.getInstructionSet(info.secondaryCpuAbi)).getAbsolutePath();
        }
    }

    private static void setBundledAppAbisAndRoots(Package pkg, PackageSetting pkgSetting) {
        setBundledAppAbi(pkg, calculateBundledApkRoot(pkg.applicationInfo.sourceDir), deriveCodePathName(pkg.applicationInfo.getCodePath()));
        if (pkgSetting != null) {
            pkgSetting.primaryCpuAbiString = pkg.applicationInfo.primaryCpuAbi;
            pkgSetting.secondaryCpuAbiString = pkg.applicationInfo.secondaryCpuAbi;
        }
    }

    private static void setBundledAppAbi(Package pkg, String apkRoot, String apkName) {
        int has64BitLibs;
        int exists;
        File codeFile = new File(pkg.codePath);
        if (PackageParser.isApkFile(codeFile)) {
            has64BitLibs = new File(apkRoot, new File("lib64", apkName).getPath()).exists();
            exists = new File(apkRoot, new File("lib", apkName).getPath()).exists();
        } else {
            File rootDir = new File(codeFile, "lib");
            if (ArrayUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS) || (TextUtils.isEmpty(Build.SUPPORTED_64_BIT_ABIS[0]) ^ 1) == 0) {
                has64BitLibs = 0;
            } else {
                has64BitLibs = new File(rootDir, VMRuntime.getInstructionSet(Build.SUPPORTED_64_BIT_ABIS[0])).exists();
            }
            if (ArrayUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS) || (TextUtils.isEmpty(Build.SUPPORTED_32_BIT_ABIS[0]) ^ 1) == 0) {
                exists = 0;
            } else {
                exists = new File(rootDir, VMRuntime.getInstructionSet(Build.SUPPORTED_32_BIT_ABIS[0])).exists();
            }
        }
        if (has64BitLibs != 0 && (exists ^ 1) != 0) {
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = null;
        } else if (exists != 0 && (has64BitLibs ^ 1) != 0) {
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = null;
        } else if (exists == 0 || has64BitLibs == 0) {
            pkg.applicationInfo.primaryCpuAbi = null;
            pkg.applicationInfo.secondaryCpuAbi = null;
        } else {
            if ((pkg.applicationInfo.flags & Integer.MIN_VALUE) == 0) {
                Slog.e(TAG, "Package " + pkg + " has multiple bundled libs, but is not multiarch.");
            }
            if (VMRuntime.is64BitInstructionSet(InstructionSets.getPreferredInstructionSet())) {
                pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
                pkg.applicationInfo.secondaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
                return;
            }
            pkg.applicationInfo.primaryCpuAbi = Build.SUPPORTED_32_BIT_ABIS[0];
            pkg.applicationInfo.secondaryCpuAbi = Build.SUPPORTED_64_BIT_ABIS[0];
        }
    }

    private void killApplication(String pkgName, int appId, String reason) {
        killApplication(pkgName, appId, -1, reason);
    }

    private void killApplication(String pkgName, int appId, int userId, String reason) {
        long token = Binder.clearCallingIdentity();
        try {
            IActivityManager am = ActivityManager.getService();
            if (am != null) {
                try {
                    am.killApplication(pkgName, appId, userId, reason);
                } catch (RemoteException e) {
                }
            }
            Binder.restoreCallingIdentity(token);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void removePackageLI(Package pkg, boolean chatty) {
        PackageSetting ps = pkg.mExtras;
        if (ps != null) {
            removePackageLI(ps, chatty);
        }
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            ps = ((Package) pkg.childPackages.get(i)).mExtras;
            if (ps != null) {
                removePackageLI(ps, chatty);
            }
        }
    }

    void removePackageLI(PackageSetting ps, boolean chatty) {
        synchronized (this.mPackages) {
            this.mPackages.remove(ps.name);
            Package pkg = ps.pkg;
            if (pkg != null) {
                cleanPackageDataStructuresLILPw(pkg, chatty);
            }
        }
    }

    void removeInstalledPackageLI(Package pkg, boolean chatty) {
        synchronized (this.mPackages) {
            this.mPackages.remove(pkg.applicationInfo.packageName);
            cleanPackageDataStructuresLILPw(pkg, chatty);
            int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
            for (int i = 0; i < childCount; i++) {
                Package childPkg = (Package) pkg.childPackages.get(i);
                this.mPackages.remove(childPkg.applicationInfo.packageName);
                cleanPackageDataStructuresLILPw(childPkg, chatty);
            }
        }
    }

    void cleanPackageDataStructuresLILPw(Package pkg, boolean chatty) {
        int i;
        ArraySet<String> appOpPkgs;
        int N = pkg.providers.size();
        for (i = 0; i < N; i++) {
            Provider p = (Provider) pkg.providers.get(i);
            this.mProviders.removeProvider(p);
            if (p.info.authority != null) {
                String[] names = p.info.authority.split(";");
                for (int j = 0; j < names.length; j++) {
                    if (this.mProvidersByAuthority.get(names[j]) == p) {
                        this.mProvidersByAuthority.remove(names[j]);
                    }
                }
            }
        }
        N = pkg.services.size();
        StringBuilder r = null;
        for (i = 0; i < N; i++) {
            Service s = (Service) pkg.services.get(i);
            this.mServices.removeService(s);
            if (chatty) {
                if (r == null) {
                    r = new StringBuilder(256);
                } else {
                    r.append(' ');
                }
                r.append(s.info.name);
            }
        }
        N = pkg.receivers.size();
        for (i = 0; i < N; i++) {
            Activity a = (Activity) pkg.receivers.get(i);
            this.mReceivers.removeActivity(a, HwBroadcastRadarUtil.KEY_RECEIVER);
        }
        N = pkg.activities.size();
        for (i = 0; i < N; i++) {
            this.mActivities.removeActivity((Activity) pkg.activities.get(i), "activity");
        }
        N = pkg.permissions.size();
        for (i = 0; i < N; i++) {
            Permission p2 = (Permission) pkg.permissions.get(i);
            BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(p2.info.name);
            if (bp == null) {
                bp = (BasePermission) this.mSettings.mPermissionTrees.get(p2.info.name);
            }
            if (bp != null && bp.perm == p2) {
                bp.perm = null;
            }
            if ((p2.info.protectionLevel & 64) != 0) {
                appOpPkgs = (ArraySet) this.mAppOpPermissionPackages.get(p2.info.name);
                if (appOpPkgs != null) {
                    appOpPkgs.remove(pkg.packageName);
                }
            }
        }
        N = pkg.requestedPermissions.size();
        for (i = 0; i < N; i++) {
            String perm = (String) pkg.requestedPermissions.get(i);
            bp = (BasePermission) this.mSettings.mPermissions.get(perm);
            if (!(bp == null || (bp.protectionLevel & 64) == 0)) {
                appOpPkgs = (ArraySet) this.mAppOpPermissionPackages.get(perm);
                if (appOpPkgs != null) {
                    appOpPkgs.remove(pkg.packageName);
                    if (appOpPkgs.isEmpty()) {
                        this.mAppOpPermissionPackages.remove(perm);
                    }
                }
            }
        }
        N = pkg.instrumentation.size();
        for (i = 0; i < N; i++) {
            this.mInstrumentation.remove(((Instrumentation) pkg.instrumentation.get(i)).getComponentName());
        }
        if (!((pkg.applicationInfo.flags & 1) == 0 || pkg.libraryNames == null)) {
            for (i = 0; i < pkg.libraryNames.size(); i++) {
                boolean removeSharedLibraryLPw = removeSharedLibraryLPw((String) pkg.libraryNames.get(i), 0);
            }
        }
        if (pkg.staticSharedLibName != null) {
            removeSharedLibraryLPw = removeSharedLibraryLPw(pkg.staticSharedLibName, pkg.staticSharedLibVersion);
        }
        writePackagesAbi();
    }

    private static boolean hasPermission(Package pkgInfo, String perm) {
        for (int i = pkgInfo.permissions.size() - 1; i >= 0; i--) {
            if (((Permission) pkgInfo.permissions.get(i)).info.name.equals(perm)) {
                return true;
            }
        }
        return false;
    }

    private void updatePermissionsLPw(Package pkg, int flags) {
        updatePermissionsLPw(pkg.packageName, pkg, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            Package childPkg = (Package) pkg.childPackages.get(i);
            updatePermissionsLPw(childPkg.packageName, childPkg, flags);
        }
    }

    private void updatePermissionsLPw(String changingPkg, Package pkgInfo, int flags) {
        updatePermissionsLPw(changingPkg, pkgInfo, pkgInfo != null ? getVolumeUuidForPackage(pkgInfo) : null, flags);
    }

    protected void updatePermissionsLPw(String changingPkg, Package pkgInfo, String replaceVolumeUuid, int flags) {
        String volumeUuid;
        boolean equals;
        Iterator<BasePermission> it = this.mSettings.mPermissionTrees.values().iterator();
        while (it.hasNext()) {
            BasePermission bp = (BasePermission) it.next();
            if (bp.packageSetting == null) {
                bp.packageSetting = (PackageSettingBase) this.mSettings.mPackages.get(bp.sourcePackage);
            }
            if (bp.packageSetting == null) {
                Slog.w(TAG, "Removing dangling permission tree: " + bp.name + " from package " + bp.sourcePackage);
                it.remove();
            } else if (changingPkg != null && changingPkg.equals(bp.sourcePackage)) {
                if (pkgInfo == null || (hasPermission(pkgInfo, bp.name) ^ 1) != 0) {
                    Slog.i(TAG, "Removing old permission tree: " + bp.name + " from package " + bp.sourcePackage);
                    flags |= 1;
                    it.remove();
                }
            }
        }
        it = this.mSettings.mPermissions.values().iterator();
        while (it.hasNext()) {
            bp = (BasePermission) it.next();
            if (bp.type == 2 && bp.packageSetting == null && bp.pendingInfo != null) {
                BasePermission tree = findPermissionTreeLP(bp.name);
                if (!(tree == null || tree.perm == null)) {
                    bp.packageSetting = tree.packageSetting;
                    bp.perm = new Permission(tree.perm.owner, new PermissionInfo(bp.pendingInfo));
                    bp.perm.info.packageName = tree.perm.info.packageName;
                    bp.perm.info.name = bp.name;
                    bp.uid = tree.uid;
                }
            }
            if (bp.packageSetting == null) {
                bp.packageSetting = (PackageSettingBase) this.mSettings.mPackages.get(bp.sourcePackage);
            }
            if (bp.packageSetting == null) {
                Slog.w(TAG, "Removing dangling permission: " + bp.name + " from package " + bp.sourcePackage);
                it.remove();
            } else if (changingPkg != null && changingPkg.equals(bp.sourcePackage)) {
                if (pkgInfo == null || (hasPermission(pkgInfo, bp.name) ^ 1) != 0) {
                    Slog.i(TAG, "Removing old permission: " + bp.name + " from package " + bp.sourcePackage);
                    flags |= 1;
                    it.remove();
                }
            }
        }
        Trace.traceBegin(262144, "grantPermissions");
        if ((flags & 1) != 0) {
            for (Package pkg : this.mPackages.values()) {
                if (pkg != pkgInfo) {
                    volumeUuid = getVolumeUuidForPackage(pkg);
                    if ((flags & 4) != 0) {
                        equals = Objects.equals(replaceVolumeUuid, volumeUuid);
                    } else {
                        equals = false;
                    }
                    grantPermissionsLPw(pkg, equals, changingPkg);
                }
            }
        }
        if (pkgInfo != null) {
            volumeUuid = getVolumeUuidForPackage(pkgInfo);
            if ((flags & 2) != 0) {
                equals = Objects.equals(replaceVolumeUuid, volumeUuid);
            } else {
                equals = false;
            }
            grantPermissionsLPw(pkgInfo, equals, changingPkg);
        }
        Trace.traceEnd(262144);
    }

    private void grantPermissionsLPw(Package pkg, boolean replace, String packageOfInterest) {
        PackageSetting ps = pkg.mExtras;
        if (ps != null) {
            int i;
            PermissionsState permissionsState = ps.getPermissionsState();
            PermissionsState origPermissions = permissionsState;
            int[] currentUserIds = UserManagerService.getInstance().getUserIds();
            boolean runtimePermissionsRevoked = false;
            int[] changedRuntimePermissionUserIds = EMPTY_INT_ARRAY;
            boolean changedInstallPermission = false;
            if (replace) {
                ps.installPermissionsFixed = false;
                if (ps.isSharedUser()) {
                    changedRuntimePermissionUserIds = revokeUnusedSharedUserPermissionsLPw(ps.sharedUser, UserManagerService.getInstance().getUserIds());
                    if (!ArrayUtils.isEmpty(changedRuntimePermissionUserIds)) {
                        runtimePermissionsRevoked = true;
                    }
                } else {
                    PermissionsState permissionsState2 = new PermissionsState(permissionsState);
                    permissionsState.reset();
                }
            }
            permissionsState.setGlobalGids(this.mGlobalGids);
            int N = pkg.requestedPermissions.size();
            for (int i2 = 0; i2 < N; i2++) {
                BasePermission bp = (BasePermission) this.mSettings.mPermissions.get((String) pkg.requestedPermissions.get(i2));
                boolean appSupportsRuntimePermissions = pkg.applicationInfo.targetSdkVersion >= 23;
                boolean equals;
                if (bp == null || bp.packageSetting == null) {
                    if (packageOfInterest != null) {
                        equals = packageOfInterest.equals(pkg.packageName);
                    }
                } else if ((!pkg.applicationInfo.isInstantApp() || (bp.isInstant() ^ 1) == 0) && (!bp.isRuntimeOnly() || (appSupportsRuntimePermissions ^ 1) == 0)) {
                    String perm = bp.name;
                    boolean allowedSig = false;
                    int grant = 1;
                    if ((bp.protectionLevel & 64) != 0) {
                        ArraySet<String> pkgs = (ArraySet) this.mAppOpPermissionPackages.get(bp.name);
                        if (pkgs == null) {
                            pkgs = new ArraySet();
                            this.mAppOpPermissionPackages.put(bp.name, pkgs);
                        }
                        pkgs.add(pkg.packageName);
                    }
                    switch (bp.protectionLevel & 15) {
                        case 0:
                            grant = 2;
                            break;
                        case 1:
                            if (!appSupportsRuntimePermissions && (this.mPermissionReviewRequired ^ 1) != 0) {
                                grant = 2;
                                break;
                            }
                            if (!origPermissions.hasInstallPermission(bp.name)) {
                                if (!this.mPromoteSystemApps || !isSystemApp(ps) || !this.mExistingSystemPackages.contains(ps.name)) {
                                    grant = 3;
                                    break;
                                } else {
                                    grant = 4;
                                    break;
                                }
                            }
                            grant = 4;
                            break;
                            break;
                        case 2:
                            allowedSig = grantSignaturePermission(perm, pkg, bp, origPermissions);
                            if (this.mCustPms != null) {
                                if (this.mCustPms.isHwFiltReqInstallPerm(pkg.packageName, perm)) {
                                    allowedSig = false;
                                }
                            }
                            if (allowedSig) {
                                grant = 2;
                                break;
                            }
                            break;
                    }
                    if (grant != 1) {
                        if (!(isSystemApp(ps) || !ps.installPermissionsFixed || r5 || (origPermissions.hasInstallPermission(perm) ^ 1) == 0 || isNewPlatformPermissionForPackage(perm, pkg))) {
                            grant = 1;
                        }
                        PermissionState permissionState;
                        int flags;
                        switch (grant) {
                            case 2:
                                for (int userId : UserManagerService.getInstance().getUserIds()) {
                                    if (origPermissions.getRuntimePermissionState(bp.name, userId) != null) {
                                        origPermissions.revokeRuntimePermission(bp, userId);
                                        origPermissions.updatePermissionFlags(bp, userId, 255, 0);
                                        changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId);
                                    }
                                }
                                if (permissionsState.grantInstallPermission(bp) == -1) {
                                    break;
                                }
                                changedInstallPermission = true;
                                break;
                            case 3:
                                for (int userId2 : UserManagerService.getInstance().getUserIds()) {
                                    permissionState = origPermissions.getRuntimePermissionState(bp.name, userId2);
                                    flags = permissionState != null ? permissionState.getFlags() : 0;
                                    if (origPermissions.hasRuntimePermission(bp.name, userId2)) {
                                        boolean revokeOnUpgrade = (flags & 8) != 0;
                                        if (revokeOnUpgrade) {
                                            flags &= -9;
                                            changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                        }
                                        if (!(this.mPermissionReviewRequired && (revokeOnUpgrade ^ 1) == 0) && permissionsState.grantRuntimePermission(bp, userId2) == -1) {
                                            changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                        }
                                        if (this.mPermissionReviewRequired && appSupportsRuntimePermissions && (flags & 64) != 0) {
                                            flags &= -65;
                                            changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                        }
                                    } else if (this.mPermissionReviewRequired && (appSupportsRuntimePermissions ^ 1) != 0) {
                                        if (PLATFORM_PACKAGE_NAME.equals(bp.sourcePackage) && (flags & 64) == 0) {
                                            flags |= 64;
                                            changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                        }
                                        if (permissionsState.grantRuntimePermission(bp, userId2) != -1) {
                                            changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId2);
                                        }
                                    }
                                    permissionsState.updatePermissionFlags(bp, userId2, flags, flags);
                                }
                                break;
                            case 4:
                                permissionState = origPermissions.getInstallPermissionState(bp.name);
                                flags = permissionState != null ? permissionState.getFlags() : 0;
                                if (origPermissions.revokeInstallPermission(bp) != -1) {
                                    origPermissions.updatePermissionFlags(bp, -1, 255, 0);
                                    changedInstallPermission = true;
                                }
                                if ((flags & 8) != 0) {
                                    break;
                                }
                                for (int userId22 : currentUserIds) {
                                    if (permissionsState.grantRuntimePermission(bp, userId22) != -1) {
                                        permissionsState.updatePermissionFlags(bp, userId22, flags, flags);
                                        changedRuntimePermissionUserIds = ArrayUtils.appendInt(changedRuntimePermissionUserIds, userId22);
                                    }
                                }
                                break;
                            default:
                                if (packageOfInterest == null) {
                                    break;
                                }
                                equals = packageOfInterest.equals(pkg.packageName);
                                break;
                        }
                    } else if (permissionsState.revokeInstallPermission(bp) != -1) {
                        permissionsState.updatePermissionFlags(bp, -1, 255, 0);
                        changedInstallPermission = true;
                        Slog.i(TAG, "Un-granting permission " + perm + " from package " + pkg.packageName + " (protectionLevel=" + bp.protectionLevel + " flags=0x" + Integer.toHexString(pkg.applicationInfo.flags) + ")");
                    } else {
                        i = bp.protectionLevel & 64;
                    }
                }
            }
            if (!((!changedInstallPermission && !replace) || (ps.installPermissionsFixed ^ 1) == 0 || (isSystemApp(ps) ^ 1) == 0) || isUpdatedSystemApp(ps)) {
                ps.installPermissionsFixed = true;
            }
            for (int userId222 : changedRuntimePermissionUserIds) {
                this.mSettings.writeRuntimePermissionsForUserLPr(userId222, runtimePermissionsRevoked);
            }
        }
    }

    private boolean isNewPlatformPermissionForPackage(String perm, Package pkg) {
        int NP = PackageParser.NEW_PERMISSIONS.length;
        int ip = 0;
        while (ip < NP) {
            NewPermissionInfo npi = PackageParser.NEW_PERMISSIONS[ip];
            if (!npi.name.equals(perm) || pkg.applicationInfo.targetSdkVersion >= npi.sdkVersion) {
                ip++;
            } else {
                Log.i(TAG, "Auto-granting " + perm + " to old pkg " + pkg.packageName);
                return true;
            }
        }
        return false;
    }

    private boolean grantSignaturePermission(String perm, Package pkg, BasePermission bp, PermissionsState origPermissions) {
        boolean privilegedPermission = (bp.protectionLevel & 16) != 0;
        boolean privappPermissionsDisable = RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_DISABLE;
        boolean platformPermission = PLATFORM_PACKAGE_NAME.equals(bp.sourcePackage);
        boolean platformPackage = PLATFORM_PACKAGE_NAME.equals(pkg.packageName);
        if (!privappPermissionsDisable && privilegedPermission && pkg.isPrivilegedApp() && (platformPackage ^ 1) != 0 && platformPermission) {
            ArraySet<String> allowedPermissions = SystemConfig.getInstance().getPrivAppPermissions(pkg.packageName);
            if (!(allowedPermissions != null ? allowedPermissions.contains(perm) : false)) {
                Slog.w(TAG, "Privileged permission " + perm + " for package " + pkg.packageName + " - not in privapp-permissions whitelist");
                if (!(this.mSystemReady || (pkg.isUpdatedSystemApp() ^ 1) == 0)) {
                    ArraySet<String> deniedPermissions = SystemConfig.getInstance().getPrivAppDenyPermissions(pkg.packageName);
                    if ((deniedPermissions != null ? deniedPermissions.contains(perm) ^ 1 : 1) == 0) {
                        return false;
                    }
                    if (this.mPrivappPermissionsViolations == null) {
                        this.mPrivappPermissionsViolations = new ArraySet();
                    }
                    this.mPrivappPermissionsViolations.add(pkg.packageName + ": " + perm);
                }
                if (RoSystemProperties.CONTROL_PRIVAPP_PERMISSIONS_ENFORCE) {
                    return false;
                }
            }
        }
        boolean z = compareSignatures(bp.packageSetting.signatures.mSignatures, pkg.mSignatures) != 0 ? compareSignatures(this.mPlatformPackage.mSignatures, pkg.mSignatures) == 0 : true;
        if (!z && privilegedPermission) {
            if (!isSystemApp(pkg)) {
                z = getGMSPackagePermission(pkg);
            } else if (pkg.isUpdatedSystemApp()) {
                PackageSetting sysPs = this.mSettings.getDisabledSystemPkgLPr(pkg.packageName);
                if (sysPs == null || !sysPs.getPermissionsState().hasInstallPermission(perm)) {
                    if (sysPs != null && sysPs.pkg != null && sysPs.isPrivileged()) {
                        for (int j = 0; j < sysPs.pkg.requestedPermissions.size(); j++) {
                            if (perm.equals(sysPs.pkg.requestedPermissions.get(j))) {
                                z = true;
                                break;
                            }
                        }
                    }
                    if (pkg.parentPackage != null) {
                        PackageSetting disabledSysParentPs = this.mSettings.getDisabledSystemPkgLPr(pkg.parentPackage.packageName);
                        if (disabledSysParentPs != null && disabledSysParentPs.pkg != null && disabledSysParentPs.isPrivileged()) {
                            if (isPackageRequestingPermission(disabledSysParentPs.pkg, perm)) {
                                z = true;
                            } else if (disabledSysParentPs.pkg.childPackages != null) {
                                int count = disabledSysParentPs.pkg.childPackages.size();
                                for (int i = 0; i < count; i++) {
                                    if (isPackageRequestingPermission((Package) disabledSysParentPs.pkg.childPackages.get(i), perm)) {
                                        z = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (sysPs.isPrivileged()) {
                    z = true;
                }
            } else {
                z = isPrivilegedApp(pkg);
            }
        }
        if (!z) {
            if (!(z || (bp.protectionLevel & 128) == 0 || pkg.applicationInfo.targetSdkVersion >= 23)) {
                z = true;
            }
            if (!(z || (bp.protectionLevel & 256) == 0 || !pkg.packageName.equals(this.mRequiredInstallerPackage))) {
                z = true;
            }
            if (!(z || (bp.protectionLevel & 512) == 0 || !pkg.packageName.equals(this.mRequiredVerifierPackage))) {
                z = true;
            }
            if (!(z || (bp.protectionLevel & 1024) == 0 || !isSystemApp(pkg))) {
                z = true;
            }
            if (!(z || (bp.protectionLevel & 32) == 0)) {
                z = origPermissions.hasInstallPermission(perm);
            }
            if (!(z || (bp.protectionLevel & 2048) == 0 || !pkg.packageName.equals(this.mSetupWizardPackage))) {
                z = true;
            }
        }
        if (!z) {
            z = getHwCertificationPermission(z, pkg, perm);
        }
        return z;
    }

    private boolean isPackageRequestingPermission(Package pkg, String permission) {
        int permCount = pkg.requestedPermissions.size();
        for (int j = 0; j < permCount; j++) {
            if (permission.equals((String) pkg.requestedPermissions.get(j))) {
                return true;
            }
        }
        return false;
    }

    public void sendPackageBroadcast(String action, String pkg, Bundle extras, int flags, String targetPkg, IIntentReceiver finishedReceiver, int[] userIds) {
        final int[] iArr = userIds;
        final String str = action;
        final String str2 = pkg;
        final Bundle bundle = extras;
        final String str3 = targetPkg;
        final int i = flags;
        final IIntentReceiver iIntentReceiver = finishedReceiver;
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    IActivityManager am = ActivityManager.getService();
                    if (am != null) {
                        int[] resolvedUserIds;
                        if (iArr == null) {
                            resolvedUserIds = am.getRunningUserIds();
                        } else {
                            resolvedUserIds = iArr;
                        }
                        for (int id : resolvedUserIds) {
                            Intent intent = new Intent(str, str2 != null ? Uri.fromParts("package", str2, null) : null);
                            if (bundle != null) {
                                intent.putExtras(bundle);
                            }
                            if (str3 != null) {
                                intent.setPackage(str3);
                            }
                            int uid = intent.getIntExtra("android.intent.extra.UID", -1);
                            if (uid > 0 && UserHandle.getUserId(uid) != id) {
                                intent.putExtra("android.intent.extra.UID", UserHandle.getUid(id, UserHandle.getAppId(uid)));
                            }
                            intent.putExtra("android.intent.extra.user_handle", id);
                            intent.addFlags(i | 67108864);
                            am.broadcastIntent(null, intent, null, iIntentReceiver, 0, null, null, null, -1, null, iIntentReceiver != null, false, id);
                        }
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    private boolean isExternalMediaAvailable() {
        return !this.mMediaMounted ? Environment.isExternalStorageEmulated() : true;
    }

    public PackageCleanItem nextPackageToClean(PackageCleanItem lastPackage) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            if (isExternalMediaAvailable()) {
                ArrayList<PackageCleanItem> pkgs = this.mSettings.mPackagesToBeCleaned;
                if (lastPackage != null) {
                    pkgs.remove(lastPackage);
                }
                if (pkgs.size() > 0) {
                    PackageCleanItem packageCleanItem = (PackageCleanItem) pkgs.get(0);
                    return packageCleanItem;
                }
                return null;
            }
            return null;
        }
    }

    void schedulePackageCleaning(String packageName, int userId, boolean andCode) {
        Message msg = this.mHandler.obtainMessage(7, userId, andCode ? 1 : 0, packageName);
        if (this.mSystemReady) {
            msg.sendToTarget();
            return;
        }
        if (this.mPostSystemReadyMessages == null) {
            this.mPostSystemReadyMessages = new ArrayList();
        }
        this.mPostSystemReadyMessages.add(msg);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void startCleaningPackages() {
        if (isExternalMediaAvailable()) {
            synchronized (this.mPackages) {
                if (this.mSettings.mPackagesToBeCleaned.isEmpty()) {
                }
            }
        }
    }

    public void installPackageAsUser(String originPath, IPackageInstallObserver2 observer, int installFlags, String installerPackageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, true, "installPackageAsUser");
        if (isUserRestricted(userId, "no_install_apps")) {
            if (observer != null) {
                try {
                    observer.onPackageInstalled("", -111, null, null);
                } catch (RemoteException e) {
                }
            }
        } else if (!HwDeviceManager.disallowOp(6)) {
            UserHandle user;
            if (callingUid == 2000 || callingUid == 0) {
                installFlags |= 32;
            } else {
                installFlags = (installFlags & -33) & -65;
            }
            if ((installFlags & 64) != 0) {
                user = UserHandle.ALL;
            } else {
                user = new UserHandle(userId);
            }
            if ((installFlags & 256) != 0 && this.mContext.checkCallingOrSelfPermission("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS") == -1) {
                throw new SecurityException("You need the android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS permission to use the PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS flag");
            } else if ((installFlags & 1) == 0 && (installFlags & 8) == 0) {
                File file = new File(originPath);
                OriginInfo origin = OriginInfo.fromUntrustedFile(file);
                Message msg = this.mHandler.obtainMessage(5);
                InstallParams params = new InstallParams(origin, null, observer, installFlags, installerPackageName, null, new VerificationInfo(null, null, -1, callingUid), user, null, null, null, 0);
                params.setTraceMethod("installAsUser").setTraceCookie(System.identityHashCode(params));
                msg.obj = params;
                Trace.asyncTraceBegin(262144, "installAsUser", System.identityHashCode(msg.obj));
                Trace.asyncTraceBegin(262144, "queueInstall", System.identityHashCode(msg.obj));
                this.mHandler.sendMessage(msg);
                parseInstallerInfo(callingUid, file.toString());
            } else {
                throw new IllegalArgumentException("New installs into ASEC containers no longer supported");
            }
        }
    }

    private int fixUpInstallReason(String installerPackageName, int installerUid, int installReason) {
        if (checkUidPermission("android.permission.INSTALL_PACKAGES", installerUid) == 0) {
            return installReason;
        }
        IDevicePolicyManager dpm = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (dpm != null) {
            ComponentName componentName = null;
            try {
                componentName = dpm.getDeviceOwnerComponent(true);
                if (componentName == null) {
                    componentName = dpm.getProfileOwner(UserHandle.getUserId(installerUid));
                }
            } catch (RemoteException e) {
            }
            if (componentName != null && componentName.getPackageName().equals(installerPackageName)) {
                return 1;
            }
        }
        if (installReason == 1) {
            return 0;
        }
        return installReason;
    }

    void installStage(String packageName, File stagedDir, String stagedCid, IPackageInstallObserver2 observer, SessionParams sessionParams, String installerPackageName, int installerUid, UserHandle user, Certificate[][] certificates) {
        OriginInfo origin;
        if (DEBUG_EPHEMERAL && (sessionParams.installFlags & 2048) != 0) {
            Slog.d(TAG, "Ephemeral install of " + packageName);
        }
        VerificationInfo verificationInfo = new VerificationInfo(sessionParams.originatingUri, sessionParams.referrerUri, sessionParams.originatingUid, installerUid);
        if (stagedDir != null) {
            origin = OriginInfo.fromStagedFile(stagedDir);
        } else {
            origin = OriginInfo.fromStagedContainer(stagedCid);
        }
        Message msg = this.mHandler.obtainMessage(5);
        IPackageInstallObserver2 iPackageInstallObserver2 = observer;
        String str = installerPackageName;
        UserHandle userHandle = user;
        InstallParams params = new InstallParams(origin, null, iPackageInstallObserver2, sessionParams.installFlags, str, sessionParams.volumeUuid, verificationInfo, userHandle, sessionParams.abiOverride, sessionParams.grantedRuntimePermissions, certificates, fixUpInstallReason(installerPackageName, installerUid, sessionParams.installReason));
        params.setTraceMethod("installStage").setTraceCookie(System.identityHashCode(params));
        msg.obj = params;
        Trace.asyncTraceBegin(262144, "installStage", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(262144, "queueInstall", System.identityHashCode(msg.obj));
        this.mHandler.sendMessage(msg);
    }

    private void sendPackageAddedForUser(String packageName, PackageSetting pkgSetting, int userId) {
        sendPackageAddedForNewUsers(packageName, !isSystemApp(pkgSetting) ? isUpdatedSystemApp(pkgSetting) : true, false, pkgSetting.appId, userId);
        SessionInfo info = new SessionInfo();
        info.installReason = pkgSetting.getInstallReason(userId);
        info.appPackageName = packageName;
        sendSessionCommitBroadcast(info, userId);
    }

    public void sendPackageAddedForNewUsers(String packageName, boolean sendBootCompleted, boolean includeStopped, int appId, int... userIds) {
        if (!ArrayUtils.isEmpty(userIds)) {
            Bundle extras = new Bundle(1);
            extras.putInt("android.intent.extra.UID", UserHandle.getUid(userIds[0], appId));
            sendPackageBroadcast("android.intent.action.PACKAGE_ADDED", packageName, extras, 0, null, null, userIds);
            if (sendBootCompleted) {
                this.mHandler.post(new com.android.server.pm.-$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E.AnonymousClass3(includeStopped, this, userIds, packageName));
            }
        }
    }

    /* synthetic */ void lambda$-com_android_server_pm_PackageManagerService_771611(int[] userIds, String packageName, boolean includeStopped) {
        for (int userId : userIds) {
            sendBootCompletedBroadcastToSystemApp(packageName, includeStopped, userId);
        }
    }

    private void sendBootCompletedBroadcastToSystemApp(String packageName, boolean includeStopped, int userId) {
        if (this.mUserManagerInternal.isUserRunning(userId)) {
            IActivityManager am = ActivityManager.getService();
            try {
                Intent lockedBcIntent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED").setPackage(packageName);
                if (includeStopped) {
                    lockedBcIntent.addFlags(32);
                }
                String[] requiredPermissions = new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"};
                am.broadcastIntent(null, lockedBcIntent, null, null, 0, null, null, requiredPermissions, -1, null, false, false, userId);
                if (this.mUserManagerInternal.isUserUnlockingOrUnlocked(userId)) {
                    Intent bcIntent = new Intent("android.intent.action.BOOT_COMPLETED").setPackage(packageName);
                    if (includeStopped) {
                        bcIntent.addFlags(32);
                    }
                    am.broadcastIntent(null, bcIntent, null, null, 0, null, null, requiredPermissions, -1, null, false, false, userId);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setApplicationHiddenSettingAsUser(String packageName, boolean hidden, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, true, "setApplicationHiddenSetting for user " + userId);
        if (hidden && isPackageDeviceAdmin(packageName, userId)) {
            Slog.w(TAG, "Not hiding package " + packageName + ": has active device admin");
            return false;
        }
        long callingId = Binder.clearCallingIdentity();
        boolean sendAdded = false;
        boolean sendRemoved = false;
        try {
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (pkgSetting != null) {
                    if (filterAppAccessLPr(pkgSetting, callingUid, userId)) {
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    } else if (PLATFORM_PACKAGE_NAME.equals(packageName)) {
                        Slog.w(TAG, "Cannot hide package: android");
                        Binder.restoreCallingIdentity(callingId);
                        return false;
                    } else {
                        Package pkg = (Package) this.mPackages.get(packageName);
                        if (pkg == null || pkg.staticSharedLibName == null) {
                            if (hidden) {
                                if ((UserHandle.isSameApp(callingUid, pkgSetting.appId) ^ 1) != 0 && this.mProtectedPackages.isPackageStateProtected(userId, packageName)) {
                                    Slog.w(TAG, "Not hiding protected package: " + packageName);
                                    Binder.restoreCallingIdentity(callingId);
                                    return false;
                                }
                            }
                            if (pkgSetting.getHidden(userId) != hidden) {
                                pkgSetting.setHidden(hidden, userId);
                                this.mSettings.writePackageRestrictionsLPr(userId);
                                if (hidden) {
                                    sendRemoved = true;
                                } else {
                                    sendAdded = true;
                                }
                            }
                        } else {
                            Slog.w(TAG, "Cannot hide package: " + packageName + " providing static shared library: " + pkg.staticSharedLibName);
                            Binder.restoreCallingIdentity(callingId);
                            return false;
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    private void sendApplicationHiddenForUser(String packageName, PackageSetting pkgSetting, int userId) {
        PackageRemovedInfo info = new PackageRemovedInfo(this);
        info.removedPackage = packageName;
        info.installerPackageName = pkgSetting.installerPackageName;
        info.removedUsers = new int[]{userId};
        info.broadcastUsers = new int[]{userId};
        info.uid = UserHandle.getUid(userId, pkgSetting.appId);
        info.sendPackageRemovedBroadcasts(true);
    }

    private void sendPackagesSuspendedForUser(String[] pkgList, int userId, boolean suspended) {
        if (pkgList.length > 0) {
            String str;
            Bundle extras = new Bundle(1);
            extras.putStringArray("android.intent.extra.changed_package_list", pkgList);
            if (suspended) {
                str = "android.intent.action.PACKAGES_SUSPENDED";
            } else {
                str = "android.intent.action.PACKAGES_UNSUSPENDED";
            }
            sendPackageBroadcast(str, null, extras, 1073741824, null, null, new int[]{userId});
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean getApplicationHiddenSettingAsUser(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "getApplicationHidden for user " + userId);
        long callingId = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (ps != null) {
                    if (filterAppAccessLPr(ps, callingUid, userId)) {
                        Binder.restoreCallingIdentity(callingId);
                        return true;
                    }
                    boolean hidden = ps.getHidden(userId);
                    Binder.restoreCallingIdentity(callingId);
                    return hidden;
                }
            }
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public int installExistingPackageAsUser(String packageName, int userId, int installFlags, int installReason) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        return installExistingPackageAsUserInternal(packageName, userId, installFlags, installReason);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int installExistingPackageAsUserInternal(String packageName, int userId, int installFlags, int installReason) {
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, true, "installExistingPackage for user " + userId);
        if (isUserRestricted(userId, "no_install_apps")) {
            return -111;
        }
        long callingId = Binder.clearCallingIdentity();
        boolean installed = false;
        boolean instantApp = (installFlags & 2048) != 0;
        boolean fullApp = (installFlags & 16384) != 0;
        try {
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (pkgSetting == null) {
                    Binder.restoreCallingIdentity(callingId);
                    return -3;
                }
                if (!canViewInstantApps(callingUid, UserHandle.getUserId(callingUid))) {
                    boolean installAllowed = false;
                    for (int checkUserId : sUserManager.getUserIds()) {
                        installAllowed = pkgSetting.getInstantApp(checkUserId) ^ 1;
                        if (installAllowed) {
                            break;
                        }
                    }
                    if (!installAllowed) {
                        Binder.restoreCallingIdentity(callingId);
                        return -3;
                    }
                }
                if (!pkgSetting.getInstalled(userId)) {
                    pkgSetting.setInstalled(true, userId);
                    pkgSetting.setHidden(false, userId);
                    pkgSetting.setInstallReason(installReason, userId);
                    this.mSettings.writePackageRestrictionsLPr(userId);
                    this.mSettings.writeKernelMappingLPr(pkgSetting);
                    installed = true;
                } else if (fullApp) {
                    if (pkgSetting.getInstantApp(userId)) {
                        installed = true;
                    }
                }
                setInstantAppForUser(pkgSetting, userId, instantApp, fullApp);
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    void setInstantAppForUser(PackageSetting pkgSetting, int userId, boolean instantApp, boolean fullApp) {
        if (instantApp || (fullApp ^ 1) == 0) {
            if (userId == -1) {
                for (int currentUserId : sUserManager.getUserIds()) {
                    if (instantApp && (pkgSetting.getInstantApp(currentUserId) ^ 1) != 0) {
                        pkgSetting.setInstantApp(true, currentUserId);
                    } else if (fullApp && pkgSetting.getInstantApp(currentUserId)) {
                        pkgSetting.setInstantApp(false, currentUserId);
                    }
                }
            } else if (instantApp && (pkgSetting.getInstantApp(userId) ^ 1) != 0) {
                pkgSetting.setInstantApp(true, userId);
            } else if (fullApp && pkgSetting.getInstantApp(userId)) {
                pkgSetting.setInstantApp(false, userId);
            }
        }
    }

    boolean isUserRestricted(int userId, String restrictionKey) {
        if (!sUserManager.getUserRestrictions(userId).getBoolean(restrictionKey, false)) {
            return false;
        }
        Log.w(TAG, "User is restricted: " + restrictionKey);
        return true;
    }

    public String[] setPackagesSuspendedAsUser(String[] packageNames, boolean suspended, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USERS", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, true, "setPackagesSuspended for user " + userId);
        if (ArrayUtils.isEmpty(packageNames)) {
            return packageNames;
        }
        List<String> changedPackages = new ArrayList(packageNames.length);
        List<String> arrayList = new ArrayList(packageNames.length);
        long callingId = Binder.clearCallingIdentity();
        for (String packageName : packageNames) {
            boolean changed = false;
            synchronized (this.mPackages) {
                PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (pkgSetting == null || filterAppAccessLPr(pkgSetting, callingUid, userId)) {
                    Slog.w(TAG, "Could not find package setting for package \"" + packageName + "\". Skipping suspending/un-suspending.");
                    arrayList.add(packageName);
                } else {
                    int appId = pkgSetting.appId;
                    if (pkgSetting.getSuspended(userId) != suspended) {
                        if (canSuspendPackageForUserLocked(packageName, userId)) {
                            pkgSetting.setSuspended(suspended, userId);
                            this.mSettings.writePackageRestrictionsLPr(userId);
                            changed = true;
                            changedPackages.add(packageName);
                        } else {
                            arrayList.add(packageName);
                        }
                    }
                    try {
                        if (changed && suspended) {
                            killApplication(packageName, UserHandle.getUid(userId, appId), "suspending package");
                        }
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(callingId);
                    }
                }
            }
        }
        Binder.restoreCallingIdentity(callingId);
        if (!changedPackages.isEmpty()) {
            sendPackagesSuspendedForUser((String[]) changedPackages.toArray(new String[changedPackages.size()]), userId, suspended);
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public boolean isPackageSuspendedForUser(String packageName, int userId) {
        boolean suspended;
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "isPackageSuspendedForUser for user " + userId);
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null || filterAppAccessLPr(ps, callingUid, userId)) {
                throw new IllegalArgumentException("Unknown target package: " + packageName);
            }
            suspended = ps.getSuspended(userId);
        }
        return suspended;
    }

    private boolean canSuspendPackageForUserLocked(String packageName, int userId) {
        if (isPackageDeviceAdmin(packageName, userId)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": has an active device admin");
            return false;
        } else if (packageName.equals(getActiveLauncherPackageName(userId))) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": contains the active launcher");
            return false;
        } else if (packageName.equals(this.mRequiredInstallerPackage)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": required for package installation");
            return false;
        } else if (packageName.equals(this.mRequiredUninstallerPackage)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": required for package uninstallation");
            return false;
        } else if (packageName.equals(this.mRequiredVerifierPackage)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": required for package verification");
            return false;
        } else if (packageName.equals(getDefaultDialerPackageName(userId))) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": is the default dialer");
            return false;
        } else if (this.mProtectedPackages.isPackageStateProtected(userId, packageName)) {
            Slog.w(TAG, "Cannot suspend/un-suspend package \"" + packageName + "\": protected package");
            return false;
        } else {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null || !pkg.applicationInfo.isStaticSharedLibrary()) {
                return true;
            }
            Slog.w(TAG, "Cannot suspend package: " + packageName + " providing static shared library: " + pkg.staticSharedLibName);
            return false;
        }
    }

    private String getActiveLauncherPackageName(int userId) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        ResolveInfo resolveInfo = resolveIntent(intent, intent.resolveTypeIfNeeded(this.mContext.getContentResolver()), 65536, userId);
        if (resolveInfo == null) {
            return null;
        }
        return resolveInfo.activityInfo.packageName;
    }

    private String getDefaultDialerPackageName(int userId) {
        String defaultDialerPackageNameLPw;
        synchronized (this.mPackages) {
            defaultDialerPackageNameLPw = this.mSettings.getDefaultDialerPackageNameLPw(userId);
        }
        return defaultDialerPackageNameLPw;
    }

    public void verifyPendingInstall(int id, int verificationCode) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can verify applications");
        Message msg = this.mHandler.obtainMessage(15);
        PackageVerificationResponse response = new PackageVerificationResponse(verificationCode, Binder.getCallingUid());
        msg.arg1 = id;
        msg.obj = response;
        this.mHandler.sendMessage(msg);
    }

    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can extend verification timeouts");
        PackageVerificationState state = (PackageVerificationState) this.mPendingVerification.get(id);
        PackageVerificationResponse response = new PackageVerificationResponse(verificationCodeAtTimeout, Binder.getCallingUid());
        if (millisecondsToDelay > 3600000) {
            millisecondsToDelay = 3600000;
        }
        if (millisecondsToDelay < 0) {
            millisecondsToDelay = 0;
        }
        if (!(verificationCodeAtTimeout == 1 || verificationCodeAtTimeout == -1)) {
        }
        if (state != null && (state.timeoutExtended() ^ 1) != 0) {
            state.extendTimeout();
            Message msg = this.mHandler.obtainMessage(15);
            msg.arg1 = id;
            msg.obj = response;
            this.mHandler.sendMessageDelayed(msg, millisecondsToDelay);
        }
    }

    private void broadcastPackageVerified(int verificationId, Uri packageUri, int verificationCode, UserHandle user) {
        Intent intent = new Intent("android.intent.action.PACKAGE_VERIFIED");
        intent.setDataAndType(packageUri, PACKAGE_MIME_TYPE);
        intent.addFlags(1);
        intent.putExtra("android.content.pm.extra.VERIFICATION_ID", verificationId);
        intent.putExtra("android.content.pm.extra.VERIFICATION_RESULT", verificationCode);
        this.mContext.sendBroadcastAsUser(intent, user, "android.permission.PACKAGE_VERIFICATION_AGENT");
    }

    private ComponentName matchComponentForVerifier(String packageName, List<ResolveInfo> receivers) {
        ActivityInfo targetReceiver = null;
        int NR = receivers.size();
        for (int i = 0; i < NR; i++) {
            ResolveInfo info = (ResolveInfo) receivers.get(i);
            if (info.activityInfo != null && packageName.equals(info.activityInfo.packageName)) {
                targetReceiver = info.activityInfo;
                break;
            }
        }
        if (targetReceiver == null) {
            return null;
        }
        return new ComponentName(targetReceiver.packageName, targetReceiver.name);
    }

    private List<ComponentName> matchVerifiers(PackageInfoLite pkgInfo, List<ResolveInfo> receivers, PackageVerificationState verificationState) {
        if (pkgInfo.verifiers.length == 0) {
            return null;
        }
        List<ComponentName> sufficientVerifiers = new ArrayList(N + 1);
        for (VerifierInfo verifierInfo : pkgInfo.verifiers) {
            ComponentName comp = matchComponentForVerifier(verifierInfo.packageName, receivers);
            if (comp != null) {
                int verifierUid = getUidForVerifier(verifierInfo);
                if (verifierUid != -1) {
                    sufficientVerifiers.add(comp);
                    verificationState.addSufficientVerifier(verifierUid);
                }
            }
        }
        return sufficientVerifiers;
    }

    private int getUidForVerifier(VerifierInfo verifierInfo) {
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(verifierInfo.packageName);
            if (pkg == null) {
                return -1;
            } else if (pkg.mSignatures.length != 1) {
                Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " has more than one signature; ignoring");
                return -1;
            } else {
                try {
                    if (Arrays.equals(verifierInfo.publicKey.getEncoded(), pkg.mSignatures[0].getPublicKey().getEncoded())) {
                        int i = pkg.applicationInfo.uid;
                        return i;
                    }
                    Slog.i(TAG, "Verifier package " + verifierInfo.packageName + " does not have the expected public key; ignoring");
                    return -1;
                } catch (CertificateException e) {
                    return -1;
                }
            }
        }
    }

    public void finishPackageInstall(int token, boolean didLaunch) {
        enforceSystemOrRoot("Only the system is allowed to finish installs");
        Trace.asyncTraceEnd(262144, "restore", token);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, token, didLaunch ? 1 : 0));
    }

    private long getVerificationTimeout() {
        return Global.getLong(this.mContext.getContentResolver(), "verifier_timeout", 10000);
    }

    private int getDefaultVerificationResponse(UserHandle user) {
        if (sUserManager.hasUserRestriction("ensure_verify_apps", user.getIdentifier())) {
            return -1;
        }
        return Global.getInt(this.mContext.getContentResolver(), "verifier_default_response", 1);
    }

    private boolean isVerificationEnabled(int userId, int installFlags, int installerUid) {
        boolean ensureVerifyAppsEnabled = isUserRestricted(userId, "ensure_verify_apps");
        if ((installFlags & 32) != 0) {
            if (ActivityManager.isRunningInTestHarness()) {
                return false;
            }
            if (ensureVerifyAppsEnabled) {
                return true;
            }
            if (Global.getInt(this.mContext.getContentResolver(), "verifier_verify_adb_installs", 1) == 0) {
                return false;
            }
        } else if (!((installFlags & 2048) == 0 || this.mInstantAppInstallerActivity == null || !this.mInstantAppInstallerActivity.packageName.equals(this.mRequiredVerifierPackage))) {
            try {
                ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(installerUid, this.mRequiredVerifierPackage);
                return false;
            } catch (SecurityException e) {
            }
        }
        if (ensureVerifyAppsEnabled) {
            return true;
        }
        return Global.getInt(this.mContext.getContentResolver(), "package_verifier_enable", 1) == 1;
    }

    public void verifyIntentFilter(int id, int verificationCode, List<String> failedDomains) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTENT_FILTER_VERIFICATION_AGENT", "Only intentfilter verification agents can verify applications");
        Message msg = this.mHandler.obtainMessage(18);
        IntentFilterVerificationResponse response = new IntentFilterVerificationResponse(Binder.getCallingUid(), verificationCode, failedDomains);
        msg.arg1 = id;
        msg.obj = response;
        this.mHandler.sendMessage(msg);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getIntentVerificationStatus(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getUserId(callingUid) != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "getIntentVerificationStatus" + userId);
        }
        if (getInstantAppPackageName(callingUid) != null) {
            return 0;
        }
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null || filterAppAccessLPr(ps, callingUid, UserHandle.getUserId(callingUid))) {
            } else {
                int intentFilterVerificationStatusLPr = this.mSettings.getIntentFilterVerificationStatusLPr(packageName, userId);
                return intentFilterVerificationStatusLPr;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean updateIntentVerificationStatus(String packageName, int status, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        synchronized (this.mPackages) {
            if (filterAppAccessLPr((PackageSetting) this.mSettings.mPackages.get(packageName), Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                return false;
            }
            boolean result = this.mSettings.updateIntentFilterVerificationStatusLPw(packageName, status, userId);
        }
    }

    public ParceledListSlice<IntentFilterVerificationInfo> getIntentFilterVerifications(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return ParceledListSlice.emptyList();
        }
        synchronized (this.mPackages) {
            if (filterAppAccessLPr((PackageSetting) this.mSettings.mPackages.get(packageName), callingUid, UserHandle.getUserId(callingUid))) {
                ParceledListSlice<IntentFilterVerificationInfo> emptyList = ParceledListSlice.emptyList();
                return emptyList;
            }
            emptyList = new ParceledListSlice(this.mSettings.getIntentFilterVerificationsLPr(packageName));
            return emptyList;
        }
    }

    public ParceledListSlice<IntentFilter> getAllIntentFilters(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return ParceledListSlice.emptyList();
        }
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getUserId(callingUid);
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            ParceledListSlice<IntentFilter> emptyList;
            if (pkg == null || pkg.activities == null) {
                emptyList = ParceledListSlice.emptyList();
                return emptyList;
            } else if (pkg.mExtras == null) {
                emptyList = ParceledListSlice.emptyList();
                return emptyList;
            } else if (filterAppAccessLPr(pkg.mExtras, callingUid, callingUserId)) {
                emptyList = ParceledListSlice.emptyList();
                return emptyList;
            } else {
                int count = pkg.activities.size();
                ArrayList<IntentFilter> result = new ArrayList();
                for (int n = 0; n < count; n++) {
                    Activity activity = (Activity) pkg.activities.get(n);
                    if (activity.intents != null && activity.intents.size() > 0) {
                        result.addAll(activity.intents);
                    }
                }
                emptyList = new ParceledListSlice(result);
                return emptyList;
            }
        }
    }

    public boolean setDefaultBrowserPackageName(String packageName, int userId) {
        boolean result;
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        }
        synchronized (this.mPackages) {
            result = this.mSettings.setDefaultBrowserPackageNameLPw(packageName, userId);
            if (packageName != null) {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToDefaultBrowserLPr(packageName, userId);
            }
        }
        return result;
    }

    public String getDefaultBrowserPackageName(int userId) {
        if (UserHandle.getCallingUserId() != userId) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        }
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        String defaultBrowserPackageNameLPw;
        synchronized (this.mPackages) {
            defaultBrowserPackageNameLPw = this.mSettings.getDefaultBrowserPackageNameLPw(userId);
        }
        return defaultBrowserPackageNameLPw;
    }

    private int getUnknownSourcesSettings() {
        return Secure.getInt(this.mContext.getContentResolver(), "install_non_market_apps", -1);
    }

    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) == null) {
            synchronized (this.mPackages) {
                PackageSetting targetPackageSetting = (PackageSetting) this.mSettings.mPackages.get(targetPackage);
                if (targetPackageSetting == null || filterAppAccessLPr(targetPackageSetting, callingUid, UserHandle.getUserId(callingUid))) {
                    throw new IllegalArgumentException("Unknown target package: " + targetPackage);
                }
                PackageSetting packageSetting;
                if (installerPackageName != null) {
                    packageSetting = (PackageSetting) this.mSettings.mPackages.get(installerPackageName);
                    if (packageSetting == null) {
                        throw new IllegalArgumentException("Unknown installer package: " + installerPackageName);
                    }
                }
                packageSetting = null;
                Object obj = this.mSettings.getUserIdLPr(callingUid);
                if (obj != null) {
                    Signature[] callerSignature;
                    if (obj instanceof SharedUserSetting) {
                        callerSignature = ((SharedUserSetting) obj).signatures.mSignatures;
                    } else if (obj instanceof PackageSetting) {
                        callerSignature = ((PackageSetting) obj).signatures.mSignatures;
                    } else {
                        throw new SecurityException("Bad object " + obj + " for uid " + callingUid);
                    }
                    if (packageSetting == null || compareSignatures(callerSignature, packageSetting.signatures.mSignatures) == 0) {
                        if (targetPackageSetting.installerPackageName != null) {
                            PackageSetting setting = (PackageSetting) this.mSettings.mPackages.get(targetPackageSetting.installerPackageName);
                            if (!(setting == null || compareSignatures(callerSignature, setting.signatures.mSignatures) == 0)) {
                                throw new SecurityException("Caller does not have same cert as old installer package " + targetPackageSetting.installerPackageName);
                            }
                        }
                        targetPackageSetting.installerPackageName = installerPackageName;
                        if (installerPackageName != null) {
                            this.mSettings.mInstallerPackages.add(installerPackageName);
                        }
                        scheduleWriteSettingsLocked();
                    } else {
                        throw new SecurityException("Caller does not have same cert as new installer package " + installerPackageName);
                    }
                }
                throw new SecurityException("Unknown calling UID: " + callingUid);
            }
        }
    }

    public void setApplicationCategoryHint(String packageName, int categoryHint, String callerPackageName) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            throw new SecurityException("Instant applications don't have access to this method");
        }
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), callerPackageName);
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                throw new IllegalArgumentException("Unknown target package " + packageName);
            } else if (filterAppAccessLPr(ps, Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                throw new IllegalArgumentException("Unknown target package " + packageName);
            } else if (Objects.equals(callerPackageName, ps.installerPackageName)) {
                if (ps.categoryHint != categoryHint) {
                    ps.categoryHint = categoryHint;
                    scheduleWriteSettingsLocked();
                }
            } else {
                throw new IllegalArgumentException("Calling package " + callerPackageName + " is not installer for " + packageName);
            }
        }
    }

    private void processPendingInstall(final InstallArgs args, final int currentStatus) {
        this.mHandler.post(new Runnable() {
            public void run() {
                PackageManagerService.this.mHandler.removeCallbacks(this);
                PackageInstalledInfo res = new PackageInstalledInfo();
                res.setReturnCode(currentStatus);
                res.uid = -1;
                res.pkg = null;
                res.removedInfo = null;
                if (res.returnCode == 1) {
                    args.doPreInstall(res.returnCode);
                    synchronized (PackageManagerService.this.mInstallLock) {
                        PackageManagerService.this.installPackageTracedLI(args, res);
                        Package pkg = res.pkg;
                        String pkgName = null;
                        if (pkg != null) {
                            pkgName = pkg.packageName;
                        }
                        if (pkgName != null) {
                            InstallerMgr.getInstance().installPackage(1, args.installerPackageName, pkgName);
                        }
                    }
                    args.doPostInstall(res.returnCode, res.uid);
                }
                boolean update = res.removedInfo != null ? res.removedInfo.removedPackage != null : false;
                boolean doRestore = !update ? (32768 & (res.pkg == null ? 0 : res.pkg.applicationInfo.flags)) != 0 : false;
                if (PackageManagerService.this.mNextInstallToken < 0) {
                    PackageManagerService.this.mNextInstallToken = 1;
                }
                PackageManagerService packageManagerService = PackageManagerService.this;
                int token = packageManagerService.mNextInstallToken;
                packageManagerService.mNextInstallToken = token + 1;
                PackageManagerService.this.mRunningInstalls.put(token, new PostInstallData(args, res));
                if (res.returnCode == 1 && doRestore) {
                    IBackupManager bm = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
                    if (bm != null) {
                        Trace.asyncTraceBegin(262144, "restore", token);
                        try {
                            if (bm.isBackupServiceActive(0)) {
                                bm.restoreAtInstall(res.pkg.applicationInfo.packageName, token);
                            } else {
                                doRestore = false;
                            }
                        } catch (RemoteException e) {
                        } catch (Exception e2) {
                            Slog.e(PackageManagerService.TAG, "Exception trying to enqueue restore", e2);
                            doRestore = false;
                        }
                    } else {
                        Slog.e(PackageManagerService.TAG, "Backup Manager not found!");
                        doRestore = false;
                    }
                }
                if (!doRestore) {
                    Trace.asyncTraceBegin(262144, "postInstall", token);
                    PackageManagerService.this.mHandler.sendMessage(PackageManagerService.this.mHandler.obtainMessage(9, token, 0));
                }
            }
        });
    }

    void notifyFirstLaunch(final String pkgName, final String installerPackage, final int userId) {
        this.mHandler.post(new Runnable() {
            public void run() {
                for (int i = 0; i < PackageManagerService.this.mRunningInstalls.size(); i++) {
                    PostInstallData data = (PostInstallData) PackageManagerService.this.mRunningInstalls.valueAt(i);
                    if (data.res.returnCode == 1 && data.res.pkg != null && pkgName.equals(data.res.pkg.applicationInfo.packageName)) {
                        int uIndex = 0;
                        while (uIndex < data.res.newUsers.length) {
                            if (userId != data.res.newUsers[uIndex]) {
                                uIndex++;
                            } else {
                                return;
                            }
                        }
                        continue;
                    }
                }
                PackageManagerService.this.sendFirstLaunchBroadcast(pkgName, installerPackage, new int[]{userId});
            }
        });
    }

    private void sendFirstLaunchBroadcast(String pkgName, String installerPkg, int[] userIds) {
        sendPackageBroadcast("android.intent.action.PACKAGE_FIRST_LAUNCH", pkgName, null, 0, installerPkg, null, userIds);
    }

    private static void clearDirectory(IMediaContainerService mcs, File[] paths) {
        for (File path : paths) {
            try {
                mcs.clearDirectory(path.getAbsolutePath());
            } catch (RemoteException e) {
            }
        }
    }

    private static boolean installOnExternalAsec(int installFlags) {
        if ((installFlags & 16) == 0 && (installFlags & 8) != 0) {
            return true;
        }
        return false;
    }

    private static boolean installForwardLocked(int installFlags) {
        return (installFlags & 1) != 0;
    }

    private InstallArgs createInstallArgs(InstallParams params) {
        if (params.move != null) {
            return new MoveInstallArgs(params);
        }
        if (installOnExternalAsec(params.installFlags) || params.isForwardLocked()) {
            return new AsecInstallArgs(params);
        }
        return new FileInstallArgs(params);
    }

    private InstallArgs createInstallArgsForExisting(int installFlags, String codePath, String resourcePath, String[] instructionSets) {
        boolean isInAsec;
        if (installOnExternalAsec(installFlags)) {
            isInAsec = true;
        } else if (!installForwardLocked(installFlags) || (codePath.startsWith(this.mDrmAppPrivateInstallDir.getAbsolutePath()) ^ 1) == 0) {
            isInAsec = false;
        } else {
            isInAsec = true;
        }
        if (!isInAsec) {
            return new FileInstallArgs(codePath, resourcePath, instructionSets);
        }
        return new AsecInstallArgs(codePath, instructionSets, installOnExternalAsec(installFlags), installForwardLocked(installFlags));
    }

    void removeDexFiles(List<String> allCodePaths, String[] instructionSets) {
        if (!allCodePaths.isEmpty()) {
            if (instructionSets == null) {
                throw new IllegalStateException("instructionSet == null");
            }
            String[] dexCodeInstructionSets = InstructionSets.getDexCodeInstructionSets(instructionSets);
            for (String codePath : allCodePaths) {
                for (String dexCodeInstructionSet : dexCodeInstructionSets) {
                    try {
                        this.mInstaller.rmdex(codePath, dexCodeInstructionSet);
                    } catch (InstallerException e) {
                    }
                }
            }
        }
    }

    private boolean isAsecExternal(String cid) {
        String asecPath = PackageHelper.getSdFilesystem(cid);
        if (asecPath == null) {
            return false;
        }
        return asecPath.startsWith(this.mAsecInternalPath) ^ 1;
    }

    private static void maybeThrowExceptionForMultiArchCopy(String message, int copyRet) throws PackageManagerException {
        if (copyRet < 0 && copyRet != -114 && copyRet != -113) {
            throw new PackageManagerException(copyRet, message);
        }
    }

    static String cidFromCodePath(String fullCodePath) {
        int eidx = fullCodePath.lastIndexOf("/");
        String subStr1 = fullCodePath.substring(0, eidx);
        return subStr1.substring(subStr1.lastIndexOf("/") + 1, eidx);
    }

    static String getAsecPackageName(String packageCid) {
        int idx = packageCid.lastIndexOf(INSTALL_PACKAGE_SUFFIX);
        if (idx == -1) {
            return packageCid;
        }
        return packageCid.substring(0, idx);
    }

    private static String getNextCodePath(String oldCodePath, String prefix, String suffix) {
        String idxStr = "";
        int idx = 1;
        if (oldCodePath != null) {
            String subStr = oldCodePath;
            if (suffix != null && oldCodePath.endsWith(suffix)) {
                subStr = oldCodePath.substring(0, oldCodePath.length() - suffix.length());
            }
            int sidx = subStr.lastIndexOf(prefix);
            if (sidx != -1) {
                subStr = subStr.substring(prefix.length() + sidx);
                if (subStr != null) {
                    if (subStr.startsWith(INSTALL_PACKAGE_SUFFIX)) {
                        subStr = subStr.substring(INSTALL_PACKAGE_SUFFIX.length());
                    }
                    try {
                        idx = Integer.parseInt(subStr);
                        idx = idx <= 1 ? idx + 1 : idx - 1;
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        return prefix + (INSTALL_PACKAGE_SUFFIX + Integer.toString(idx));
    }

    private File getNextCodePath(File targetDir, String packageName) {
        File result;
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        do {
            random.nextBytes(bytes);
            result = new File(targetDir, packageName + INSTALL_PACKAGE_SUFFIX + Base64.encodeToString(bytes, 10));
        } while (result.exists());
        return result;
    }

    static String deriveCodePathName(String codePath) {
        if (codePath == null) {
            return null;
        }
        File codeFile = new File(codePath);
        String name = codeFile.getName();
        if (codeFile.isDirectory()) {
            return name;
        }
        if (name.endsWith(".apk") || name.endsWith(".tmp")) {
            return name.substring(0, name.lastIndexOf(46));
        }
        Slog.w(TAG, "Odd, " + codePath + " doesn't look like an APK");
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void installNewPackageLIF(Package pkg, int policyFlags, int scanFlags, UserHandle user, String installerPackageName, String volumeUuid, PackageInstalledInfo res, int installReason) {
        Trace.traceBegin(262144, "installNewPackage");
        String pkgName = pkg.packageName;
        synchronized (this.mPackages) {
            String renamedPackage = this.mSettings.getRenamedPackageLPr(pkgName);
            if (renamedPackage != null) {
                res.setError(-1, "Attempt to re-install " + pkgName + " without first uninstalling package running as " + renamedPackage);
                return;
            } else if (this.mPackages.containsKey(pkgName)) {
                res.setError(-1, "Attempt to re-install " + pkgName + " without first uninstalling.");
                return;
            }
        }
        Trace.traceEnd(262144);
    }

    private boolean shouldCheckUpgradeKeySetLP(PackageSetting oldPs, int scanFlags) {
        if (oldPs == null || (scanFlags & 4096) != 0 || oldPs.sharedUser != null || (oldPs.keySetData.isUsingUpgradeKeySets() ^ 1) != 0) {
            return false;
        }
        KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
        long[] upgradeKeySets = oldPs.keySetData.getUpgradeKeySets();
        int i = 0;
        while (i < upgradeKeySets.length) {
            if (ksms.isIdValidKeySetId(upgradeKeySets[i])) {
                i++;
            } else {
                Slog.wtf(TAG, "Package " + (oldPs.name != null ? oldPs.name : "<null>") + " contains upgrade-key-set reference to unknown key-set: " + upgradeKeySets[i] + " reverting to signatures check.");
                return false;
            }
        }
        return true;
    }

    private boolean checkUpgradeKeySetLP(PackageSetting oldPS, Package newPkg) {
        long[] upgradeKeySets = oldPS.keySetData.getUpgradeKeySets();
        KeySetManagerService ksms = this.mSettings.mKeySetManagerService;
        for (long publicKeysFromKeySetLPr : upgradeKeySets) {
            Set<PublicKey> upgradeSet = ksms.getPublicKeysFromKeySetLPr(publicKeysFromKeySetLPr);
            if (upgradeSet != null && newPkg.mSigningKeys.containsAll(upgradeSet)) {
                return true;
            }
        }
        return false;
    }

    private static void updateDigest(MessageDigest digest, File file) throws IOException {
        Throwable th;
        Throwable th2 = null;
        DigestInputStream digestInputStream = null;
        try {
            DigestInputStream digestStream = new DigestInputStream(new FileInputStream(file), digest);
            do {
                try {
                } catch (Throwable th3) {
                    th = th3;
                    digestInputStream = digestStream;
                }
            } while (digestStream.read() != -1);
            if (digestStream != null) {
                try {
                    digestStream.close();
                } catch (Throwable th4) {
                    th2 = th4;
                }
            }
            if (th2 != null) {
                throw th2;
            }
        } catch (Throwable th5) {
            th = th5;
            if (digestInputStream != null) {
                try {
                    digestInputStream.close();
                } catch (Throwable th6) {
                    if (th2 == null) {
                        th2 = th6;
                    } else if (th2 != th6) {
                        th2.addSuppressed(th6);
                    }
                }
            }
            if (th2 != null) {
                throw th2;
            }
            throw th;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void replacePackageLIF(Package pkg, int policyFlags, int scanFlags, UserHandle user, String installerPackageName, PackageInstalledInfo res, int installReason) {
        boolean isInstantApp = (131072 & scanFlags) != 0;
        String pkgName = pkg.packageName;
        synchronized (this.mPackages) {
            Package oldPackage = (Package) this.mPackages.get(pkgName);
            boolean oldTargetsPreRelease = oldPackage.applicationInfo.targetSdkVersion == 10000;
            boolean newTargetsPreRelease = pkg.applicationInfo.targetSdkVersion == 10000;
            if (oldTargetsPreRelease && (newTargetsPreRelease ^ 1) != 0 && (policyFlags & 4096) == 0) {
                Slog.w(TAG, "Can't install package targeting released sdk");
                res.setReturnCode(-7);
                return;
            }
            int i;
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
            if (shouldCheckUpgradeKeySetLP(ps, scanFlags)) {
                if (!checkUpgradeKeySetLP(ps, pkg)) {
                    res.setError(-7, "New package not signed by keys specified by upgrade-keysets: " + pkgName);
                    return;
                }
            } else if (compareSignatures(oldPackage.mSignatures, pkg.mSignatures) != 0) {
                if (isSystemSignatureUpdated(oldPackage.mSignatures, pkg.mSignatures)) {
                    Slog.d(TAG, pkg.packageName + " system signature update");
                } else {
                    res.setError(-7, "New package has a different signature: " + pkgName);
                    return;
                }
            }
            if (oldPackage.restrictUpdateHash != null && oldPackage.isSystemApp()) {
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-512");
                    updateDigest(digest, new File(pkg.baseCodePath));
                    if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
                        for (String path : pkg.splitCodePaths) {
                            updateDigest(digest, new File(path));
                        }
                    }
                    if (Arrays.equals(oldPackage.restrictUpdateHash, digest.digest())) {
                        pkg.restrictUpdateHash = oldPackage.restrictUpdateHash;
                    } else {
                        res.setError(-2, "New package fails restrict-update check: " + pkgName);
                        return;
                    }
                } catch (NoSuchAlgorithmException e) {
                    res.setError(-2, "Could not compute hash: " + pkgName);
                    return;
                }
            }
            String invalidPackageName = getParentOrChildPackageChangedSharedUser(oldPackage, pkg);
            if (invalidPackageName != null) {
                res.setError(-8, "Package " + invalidPackageName + " tried to change user " + oldPackage.mSharedUserId);
                return;
            }
            int[] allUsers = sUserManager.getUserIds();
            int[] installedUsers = ps.queryInstalledUsers(allUsers, true);
            if (isInstantApp) {
                if (user == null || user.getIdentifier() == -1) {
                    i = 0;
                    int length = allUsers.length;
                    while (i < length) {
                        int currentUser = allUsers[i];
                        if (ps.getInstantApp(currentUser)) {
                            i++;
                        } else {
                            Slog.w(TAG, "Can't replace full app with instant app: " + pkgName + " for user: " + currentUser);
                            res.setReturnCode(-116);
                            return;
                        }
                    }
                }
                if (!ps.getInstantApp(user.getIdentifier())) {
                    Slog.w(TAG, "Can't replace full app with instant app: " + pkgName + " for user: " + user.getIdentifier());
                    res.setReturnCode(-116);
                }
            }
        }
    }

    public List<String> getPreviousCodePaths(String packageName) {
        int callingUid = Binder.getCallingUid();
        List<String> result = new ArrayList();
        if (getInstantAppPackageName(callingUid) != null) {
            return result;
        }
        PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
        if (!(ps == null || ps.oldCodePaths == null || (filterAppAccessLPr(ps, callingUid, UserHandle.getUserId(callingUid)) ^ 1) == 0)) {
            result.addAll(ps.oldCodePaths);
        }
        return result;
    }

    private void replaceNonSystemPackageLIF(Package deletedPackage, Package pkg, int policyFlags, int scanFlags, UserHandle user, int[] allUsers, String installerPackageName, PackageInstalledInfo res, int installReason) {
        if (!isMDMDisallowedInstallPackage(pkg, res)) {
            PackageSetting ps;
            int i;
            String pkgName = deletedPackage.packageName;
            boolean deletedPkg = true;
            boolean addedPkg = false;
            boolean killApp = (scanFlags & 16384) == 0;
            int deleteFlags = (killApp ? 0 : 8) | 1;
            long origUpdateTime = pkg.mExtras != null ? ((PackageSetting) pkg.mExtras).lastUpdateTime : 0;
            if (deletePackageLIF(pkgName, null, true, allUsers, deleteFlags, res.removedInfo, true, pkg)) {
                if (deletedPackage.isForwardLocked() || isExternal(deletedPackage)) {
                    int[] uidArray = new int[]{deletedPackage.applicationInfo.uid};
                    ArrayList pkgList = new ArrayList(1);
                    pkgList.add(deletedPackage.applicationInfo.packageName);
                    sendResourcesChangedBroadcast(false, true, pkgList, uidArray, null);
                }
                clearAppDataLIF(pkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                clearAppProfilesLIF(deletedPackage, -1);
                try {
                    Package newPackage = scanPackageTracedLI(pkg, policyFlags, scanFlags | 32, System.currentTimeMillis(), user);
                    updateSettingsLI(newPackage, installerPackageName, allUsers, res, user, installReason);
                    ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
                    if (killApp) {
                        ps.oldCodePaths = null;
                    } else {
                        if (ps.oldCodePaths == null) {
                            ps.oldCodePaths = new ArraySet();
                        }
                        Collections.addAll(ps.oldCodePaths, new String[]{deletedPackage.baseCodePath});
                        if (deletedPackage.splitCodePaths != null) {
                            Collections.addAll(ps.oldCodePaths, deletedPackage.splitCodePaths);
                        }
                    }
                    if (ps.childPackageNames != null) {
                        for (i = ps.childPackageNames.size() - 1; i >= 0; i--) {
                            ((PackageSetting) this.mSettings.mPackages.get((String) ps.childPackageNames.get(i))).oldCodePaths = ps.oldCodePaths;
                        }
                    }
                    setInstantAppForUser(ps, user.getIdentifier(), (131072 & scanFlags) != 0, (262144 & scanFlags) != 0);
                    prepareAppDataAfterInstallLIF(newPackage);
                    addedPkg = true;
                    this.mDexManager.notifyPackageUpdated(newPackage.packageName, newPackage.baseCodePath, newPackage.splitCodePaths);
                } catch (PackageManagerException e) {
                    res.setError("Package couldn't be installed in " + pkg.codePath, e);
                }
            } else {
                res.setError(-10, "replaceNonSystemPackageLI");
                deletedPkg = false;
            }
            if (res.returnCode != 1) {
                if (addedPkg) {
                    deletePackageLIF(pkgName, null, true, allUsers, deleteFlags, res.removedInfo, true, null);
                }
                if (deletedPkg) {
                    try {
                        scanPackageTracedLI(new File(deletedPackage.codePath), ((this.mDefParseFlags | 2) | (deletedPackage.isForwardLocked() ? 16 : 0)) | (isExternal(deletedPackage) ? 32 : 0), 40, origUpdateTime, null);
                        synchronized (this.mPackages) {
                            setInstallerPackageNameLPw(deletedPackage, installerPackageName);
                            updatePermissionsLPw(deletedPackage, 1);
                            this.mSettings.writeLPr();
                        }
                        Slog.i(TAG, "Successfully restored package : " + pkgName + " after failed upgrade");
                    } catch (PackageManagerException e2) {
                        Slog.e(TAG, "Failed to restore package : " + pkgName + " after failed upgrade: " + e2.getMessage());
                        return;
                    }
                }
            }
            synchronized (this.mPackages) {
                ps = this.mSettings.getPackageLPr(pkg.packageName);
                if (ps != null) {
                    res.removedInfo.removedForAllUsers = this.mPackages.get(ps.name) == null;
                    if (res.removedInfo.removedChildPackages != null) {
                        for (i = res.removedInfo.removedChildPackages.size() - 1; i >= 0; i--) {
                            if (res.addedChildPackages.containsKey((String) res.removedInfo.removedChildPackages.keyAt(i))) {
                                res.removedInfo.removedChildPackages.removeAt(i);
                            } else {
                                boolean z;
                                PackageRemovedInfo childInfo = (PackageRemovedInfo) res.removedInfo.removedChildPackages.valueAt(i);
                                if (this.mPackages.get(childInfo.removedPackage) == null) {
                                    z = true;
                                } else {
                                    z = false;
                                }
                                childInfo.removedForAllUsers = z;
                            }
                        }
                    }
                }
            }
        }
    }

    private void replaceSystemPackageLIF(Package deletedPackage, Package pkg, int policyFlags, int scanFlags, UserHandle user, int[] allUsers, String installerPackageName, PackageInstalledInfo res, int installReason) {
        Package newPackage;
        PackageManagerException e;
        String packageName = deletedPackage.packageName;
        PackageSetting deletePackageSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
        removePackageLI(deletedPackage, true);
        synchronized (this.mPackages) {
            boolean disabledSystem = disableSystemPackageLPw(deletedPackage, pkg);
        }
        if (disabledSystem) {
            res.removedInfo.args = null;
        } else {
            res.removedInfo.args = createInstallArgsForExisting(0, deletedPackage.applicationInfo.getCodePath(), deletedPackage.applicationInfo.getResourcePath(), InstructionSets.getAppDexInstructionSets(deletedPackage.applicationInfo));
        }
        clearAppDataLIF(pkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
        clearAppProfilesLIF(deletedPackage, -1);
        res.setReturnCode(1);
        pkg.setApplicationInfoFlags(128, 128);
        synchronized (this.mPackages) {
            String disableSysPath = this.mSettings.getDisabledSysPackagesPath(packageName);
            PackageSetting disableSysSetting = this.mSettings.getDisabledSystemPkgLPr(packageName);
            boolean disableSysPathInDel = false;
            boolean disableSysInData = false;
            if (disableSysPath != null && (containDelPath(disableSysPath) || isPreRemovableApp(disableSysPath))) {
                disableSysPathInDel = true;
            }
            if (disableSysSetting != null && isDelappInData(disableSysSetting)) {
                disableSysInData = true;
            }
            if (disableSysSetting != null && isDelappInCust(disableSysSetting)) {
                disableSysInData = true;
            }
            if (containDelPath(deletedPackage.applicationInfo.sourceDir) || isPreRemovableApp(deletedPackage.applicationInfo.sourceDir) || isDelappInData(deletePackageSetting) || isDelappInCust(deletePackageSetting) || disableSysPathInDel || disableSysInData) {
                ApplicationInfo applicationInfo = pkg.applicationInfo;
                applicationInfo.hwFlags |= 67108864;
            }
        }
        try {
            newPackage = scanPackageTracedLI(pkg, policyFlags, scanFlags, 0, user);
            try {
                setInstallAndUpdateTime(newPackage, deletedPackage.mExtras.firstInstallTime, System.currentTimeMillis());
                if (res.returnCode == 1) {
                    int deletedChildCount = deletedPackage.childPackages != null ? deletedPackage.childPackages.size() : 0;
                    int newChildCount = newPackage.childPackages != null ? newPackage.childPackages.size() : 0;
                    for (int i = 0; i < deletedChildCount; i++) {
                        Package deletedChildPkg = (Package) deletedPackage.childPackages.get(i);
                        boolean childPackageDeleted = true;
                        for (int j = 0; j < newChildCount; j++) {
                            if (deletedChildPkg.packageName.equals(((Package) newPackage.childPackages.get(j)).packageName)) {
                                childPackageDeleted = false;
                                break;
                            }
                        }
                        if (childPackageDeleted) {
                            PackageSetting ps = this.mSettings.getDisabledSystemPkgLPr(deletedChildPkg.packageName);
                            if (!(ps == null || res.removedInfo.removedChildPackages == null)) {
                                boolean z;
                                PackageRemovedInfo removedChildRes = (PackageRemovedInfo) res.removedInfo.removedChildPackages.get(deletedChildPkg.packageName);
                                removePackageDataLIF(ps, allUsers, removedChildRes, 0, false);
                                if (this.mPackages.get(ps.name) == null) {
                                    z = true;
                                } else {
                                    z = false;
                                }
                                removedChildRes.removedForAllUsers = z;
                            }
                        }
                    }
                    updateSettingsLI(newPackage, installerPackageName, allUsers, res, user, installReason);
                    prepareAppDataAfterInstallLIF(newPackage);
                    this.mDexManager.notifyPackageUpdated(newPackage.packageName, newPackage.baseCodePath, newPackage.splitCodePaths);
                }
            } catch (PackageManagerException e2) {
                e = e2;
                res.setReturnCode(RequestStatus.SYS_ETIMEDOUT);
                res.setError("Package couldn't be installed in " + pkg.codePath, e);
                if (res.returnCode == 1) {
                    if (newPackage != null) {
                        removeInstalledPackageLI(newPackage, true);
                    }
                    try {
                        scanPackageTracedLI(deletedPackage, policyFlags, 8, 0, user);
                    } catch (PackageManagerException e3) {
                        Slog.e(TAG, "Failed to restore original package: " + e3.getMessage());
                    }
                    synchronized (this.mPackages) {
                        if (disabledSystem) {
                            enableSystemPackageLPw(deletedPackage);
                        }
                        setInstallerPackageNameLPw(deletedPackage, installerPackageName);
                        updatePermissionsLPw(deletedPackage, 1);
                        this.mSettings.writeLPr();
                    }
                    Slog.i(TAG, "Successfully restored package : " + deletedPackage.packageName + " after failed upgrade");
                }
            }
        } catch (PackageManagerException e4) {
            e3 = e4;
            newPackage = null;
            res.setReturnCode(RequestStatus.SYS_ETIMEDOUT);
            res.setError("Package couldn't be installed in " + pkg.codePath, e3);
            if (res.returnCode == 1) {
                if (newPackage != null) {
                    removeInstalledPackageLI(newPackage, true);
                }
                scanPackageTracedLI(deletedPackage, policyFlags, 8, 0, user);
                synchronized (this.mPackages) {
                    if (disabledSystem) {
                        enableSystemPackageLPw(deletedPackage);
                    }
                    setInstallerPackageNameLPw(deletedPackage, installerPackageName);
                    updatePermissionsLPw(deletedPackage, 1);
                    this.mSettings.writeLPr();
                }
                Slog.i(TAG, "Successfully restored package : " + deletedPackage.packageName + " after failed upgrade");
            }
        }
        if (res.returnCode == 1) {
            if (newPackage != null) {
                removeInstalledPackageLI(newPackage, true);
            }
            scanPackageTracedLI(deletedPackage, policyFlags, 8, 0, user);
            synchronized (this.mPackages) {
                if (disabledSystem) {
                    enableSystemPackageLPw(deletedPackage);
                }
                setInstallerPackageNameLPw(deletedPackage, installerPackageName);
                updatePermissionsLPw(deletedPackage, 1);
                this.mSettings.writeLPr();
            }
            Slog.i(TAG, "Successfully restored package : " + deletedPackage.packageName + " after failed upgrade");
        }
    }

    private String getParentOrChildPackageChangedSharedUser(Package oldPkg, Package newPkg) {
        if (!Objects.equals(oldPkg.mSharedUserId, newPkg.mSharedUserId)) {
            return newPkg.packageName;
        }
        int oldChildCount = oldPkg.childPackages != null ? oldPkg.childPackages.size() : 0;
        int newChildCount = newPkg.childPackages != null ? newPkg.childPackages.size() : 0;
        for (int i = 0; i < newChildCount; i++) {
            Package newChildPkg = (Package) newPkg.childPackages.get(i);
            for (int j = 0; j < oldChildCount; j++) {
                Package oldChildPkg = (Package) oldPkg.childPackages.get(j);
                if (newChildPkg.packageName.equals(oldChildPkg.packageName) && (Objects.equals(newChildPkg.mSharedUserId, oldChildPkg.mSharedUserId) ^ 1) != 0) {
                    return newChildPkg.packageName;
                }
            }
        }
        return null;
    }

    private void removeNativeBinariesLI(PackageSetting ps) {
        if (ps != null) {
            NativeLibraryHelper.removeNativeBinariesLI(ps.legacyNativeLibraryPathString);
            int childCount = ps.childPackageNames != null ? ps.childPackageNames.size() : 0;
            for (int i = 0; i < childCount; i++) {
                PackageSetting childPs;
                synchronized (this.mPackages) {
                    childPs = this.mSettings.getPackageLPr((String) ps.childPackageNames.get(i));
                }
                if (childPs != null) {
                    NativeLibraryHelper.removeNativeBinariesLI(childPs.legacyNativeLibraryPathString);
                }
            }
        }
    }

    private void enableSystemPackageLPw(Package pkg) {
        this.mSettings.enableSystemPackageLPw(pkg.packageName);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            this.mSettings.enableSystemPackageLPw(((Package) pkg.childPackages.get(i)).packageName);
        }
    }

    private boolean disableSystemPackageLPw(Package oldPkg, Package newPkg) {
        boolean disabled = this.mSettings.disableSystemPackageLPw(oldPkg.packageName, true);
        for (int i = 0; i < (oldPkg.childPackages != null ? oldPkg.childPackages.size() : 0); i++) {
            Package childPkg = (Package) oldPkg.childPackages.get(i);
            disabled |= this.mSettings.disableSystemPackageLPw(childPkg.packageName, newPkg.hasChildPackage(childPkg.packageName));
        }
        return disabled;
    }

    private void setInstallerPackageNameLPw(Package pkg, String installerPackageName) {
        this.mSettings.setInstallerPackageName(pkg.packageName, installerPackageName);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            this.mSettings.setInstallerPackageName(((Package) pkg.childPackages.get(i)).packageName, installerPackageName);
        }
    }

    private int[] revokeUnusedSharedUserPermissionsLPw(SharedUserSetting su, int[] allUserIds) {
        int i;
        ArraySet<String> usedPermissions = new ArraySet();
        int packageCount = su.packages.size();
        for (i = 0; i < packageCount; i++) {
            PackageSetting ps = (PackageSetting) su.packages.valueAt(i);
            if (ps.pkg != null) {
                int requestedPermCount = ps.pkg.requestedPermissions.size();
                for (int j = 0; j < requestedPermCount; j++) {
                    String permission = (String) ps.pkg.requestedPermissions.get(j);
                    if (((BasePermission) this.mSettings.mPermissions.get(permission)) != null) {
                        usedPermissions.add(permission);
                    }
                }
            }
        }
        PermissionsState permissionsState = su.getPermissionsState();
        List<PermissionState> installPermStates = permissionsState.getInstallPermissionStates();
        for (i = installPermStates.size() - 1; i >= 0; i--) {
            BasePermission bp;
            PermissionState permissionState = (PermissionState) installPermStates.get(i);
            if (!usedPermissions.contains(permissionState.getName())) {
                bp = (BasePermission) this.mSettings.mPermissions.get(permissionState.getName());
                if (bp != null) {
                    permissionsState.revokeInstallPermission(bp);
                    permissionsState.updatePermissionFlags(bp, -1, 255, 0);
                }
            }
        }
        int[] runtimePermissionChangedUserIds = EmptyArray.INT;
        for (int userId : allUserIds) {
            List<PermissionState> runtimePermStates = permissionsState.getRuntimePermissionStates(userId);
            for (i = runtimePermStates.size() - 1; i >= 0; i--) {
                permissionState = (PermissionState) runtimePermStates.get(i);
                if (!usedPermissions.contains(permissionState.getName())) {
                    bp = (BasePermission) this.mSettings.mPermissions.get(permissionState.getName());
                    if (bp != null) {
                        permissionsState.revokeRuntimePermission(bp, userId);
                        permissionsState.updatePermissionFlags(bp, userId, 255, 0);
                        runtimePermissionChangedUserIds = ArrayUtils.appendInt(runtimePermissionChangedUserIds, userId);
                    }
                }
            }
        }
        return runtimePermissionChangedUserIds;
    }

    protected void updateSettingsLI(Package newPackage, String installerPackageName, int[] allUsers, PackageInstalledInfo res, UserHandle user, int installReason) {
        updateSettingsInternalLI(newPackage, installerPackageName, allUsers, res.origUsers, res, user, installReason);
        int childCount = newPackage.childPackages != null ? newPackage.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            Package childPackage = (Package) newPackage.childPackages.get(i);
            PackageInstalledInfo childRes = (PackageInstalledInfo) res.addedChildPackages.get(childPackage.packageName);
            updateSettingsInternalLI(childPackage, installerPackageName, allUsers, childRes.origUsers, childRes, user, installReason);
        }
    }

    private void updateSettingsInternalLI(Package newPackage, String installerPackageName, int[] allUsers, int[] installedForUsers, PackageInstalledInfo res, UserHandle user, int installReason) {
        Trace.traceBegin(262144, "updateSettings");
        String pkgName = newPackage.packageName;
        synchronized (this.mPackages) {
            this.mSettings.setInstallStatus(pkgName, 0);
            Trace.traceBegin(262144, "writeSettings");
            this.mSettings.writeLPr();
            Trace.traceEnd(262144);
        }
        synchronized (this.mPackages) {
            updatePermissionsLPw(newPackage.packageName, newPackage, (newPackage.permissions.size() > 0 ? 1 : 0) | 2);
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
            updateFlagsForMarketSystemApp(newPackage);
            int userId = user.getIdentifier();
            if (ps != null) {
                if (isSystemApp(newPackage)) {
                    if (res.origUsers != null) {
                        for (int origUserId : res.origUsers) {
                            if (userId == -1 || userId == origUserId) {
                                ps.setEnabled(0, origUserId, installerPackageName);
                            }
                        }
                    }
                    if (!(allUsers == null || installedForUsers == null)) {
                        for (int currentUserId : allUsers) {
                            ps.setInstalled(ArrayUtils.contains(installedForUsers, currentUserId), currentUserId);
                        }
                    }
                }
                if (userId != -1) {
                    ps.setInstalled(true, userId);
                    ps.setEnabled(0, userId, installerPackageName);
                }
                Set<Integer> previousUserIds = new ArraySet();
                if (!(res.removedInfo == null || res.removedInfo.installReasons == null)) {
                    int installReasonCount = res.removedInfo.installReasons.size();
                    for (int i = 0; i < installReasonCount; i++) {
                        int previousUserId = res.removedInfo.installReasons.keyAt(i);
                        ps.setInstallReason(((Integer) res.removedInfo.installReasons.valueAt(i)).intValue(), previousUserId);
                        previousUserIds.add(Integer.valueOf(previousUserId));
                    }
                }
                if (userId == -1) {
                    for (int currentUserId2 : sUserManager.getUserIds()) {
                        if (!previousUserIds.contains(Integer.valueOf(currentUserId2))) {
                            ps.setInstallReason(installReason, currentUserId2);
                        }
                    }
                } else if (!previousUserIds.contains(Integer.valueOf(userId))) {
                    ps.setInstallReason(installReason, userId);
                }
                this.mSettings.writeKernelMappingLPr(ps);
            }
            res.name = pkgName;
            res.uid = newPackage.applicationInfo.uid;
            res.pkg = newPackage;
            this.mSettings.setInstallStatus(pkgName, 1);
            this.mSettings.setInstallerPackageName(pkgName, installerPackageName);
            res.setReturnCode(1);
            Trace.traceBegin(262144, "writeSettings");
            this.mSettings.writeLPr();
            Trace.traceEnd(262144);
        }
        Trace.traceEnd(262144);
    }

    private void installPackageTracedLI(InstallArgs args, PackageInstalledInfo res) {
        try {
            Trace.traceBegin(262144, "installPackage");
            installPackageLI(args, res);
        } finally {
            Trace.traceEnd(262144);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void installPackageLI(InstallArgs args, PackageInstalledInfo res) {
        int installFlags;
        String installerPackageName;
        boolean forwardLocked;
        boolean instantApp;
        boolean replace;
        int scanFlags;
        int parseFlags;
        Package pkg;
        int childCount;
        int i;
        Package childPkg;
        PackageInstalledInfo childRes;
        PackageSetting childPs;
        String pkgName;
        PackageSetting ps;
        Throwable th;
        if (Secure.getInt(this.mContext.getContentResolver(), SUW_FRP_STATE, 0) != 1 || Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 1) {
            installFlags = args.installFlags;
            installerPackageName = args.installerPackageName;
            String volumeUuid = args.volumeUuid;
            File file = new File(args.getCodePath());
            forwardLocked = (installFlags & 1) != 0;
            boolean onExternal = (installFlags & 8) == 0 ? args.volumeUuid != null : true;
            instantApp = (installFlags & 2048) != 0;
            boolean fullApp = (installFlags & 16384) != 0;
            boolean forceSdk = (installFlags & 8192) != 0;
            boolean virtualPreload = (65536 & installFlags) != 0;
            replace = false;
            scanFlags = 24;
            if (args.move != null) {
                scanFlags = 4120;
            }
            if ((installFlags & 4096) != 0) {
                scanFlags |= 16384;
            }
            if (instantApp) {
                scanFlags |= 131072;
            }
            if (fullApp) {
                scanFlags |= 262144;
            }
            if (virtualPreload) {
                scanFlags |= 524288;
            }
            res.setReturnCode(1);
            res.installerPackageName = installerPackageName;
            long installBeginTime = SystemClock.elapsedRealtime();
            if (instantApp && (forwardLocked || onExternal)) {
                Slog.i(TAG, "Incompatible ephemeral install; fwdLocked=" + forwardLocked + " external=" + onExternal);
                res.setReturnCode(-116);
                return;
            }
            int i2;
            int i3 = ((((this.mDefParseFlags | 2) | 1024) | (forwardLocked ? 16 : 0)) | (onExternal ? 32 : 0)) | (instantApp ? 2048 : 0);
            if (forceSdk) {
                i2 = 4096;
            } else {
                i2 = 0;
            }
            parseFlags = i3 | i2;
            PackageParser pp = new PackageParser();
            pp.setSeparateProcesses(this.mSeparateProcesses);
            pp.setDisplayMetrics(this.mMetrics);
            pp.setCallback(this.mPackageParserCallback);
            Trace.traceBegin(262144, "parsePackage");
            try {
                pkg = pp.parsePackage(file, parseFlags);
                if (pkg != null && isInMultiWinWhiteList(pkg.packageName)) {
                    pkg.forceResizeableAllActivity();
                }
                Trace.traceEnd(262144);
                if (instantApp && pkg.applicationInfo.targetSdkVersion <= 25) {
                    Slog.w(TAG, "Instant app package " + pkg.packageName + " does not target O");
                    res.setError(-27, "Instant app package must target O");
                    return;
                } else if (!instantApp || pkg.applicationInfo.targetSandboxVersion == 2) {
                    if (pkg.applicationInfo.isStaticSharedLibrary()) {
                        renameStaticSharedLibraryPackage(pkg);
                        if (onExternal) {
                            Slog.i(TAG, "Static shared libs can only be installed on internal storage.");
                            res.setError(-19, "Packages declaring static-shared libs cannot be updated");
                            return;
                        }
                    }
                    if (pkg.childPackages != null) {
                        synchronized (this.mPackages) {
                            childCount = pkg.childPackages.size();
                            for (i = 0; i < childCount; i++) {
                                childPkg = (Package) pkg.childPackages.get(i);
                                childRes = new PackageInstalledInfo();
                                childRes.setReturnCode(1);
                                childRes.pkg = childPkg;
                                childRes.name = childPkg.packageName;
                                childPs = this.mSettings.getPackageLPr(childPkg.packageName);
                                if (childPs != null) {
                                    childRes.origUsers = childPs.queryInstalledUsers(sUserManager.getUserIds(), true);
                                }
                                if (this.mPackages.containsKey(childPkg.packageName)) {
                                    childRes.removedInfo = new PackageRemovedInfo(this);
                                    childRes.removedInfo.removedPackage = childPkg.packageName;
                                    childRes.removedInfo.installerPackageName = childPs.installerPackageName;
                                }
                                if (res.addedChildPackages == null) {
                                    res.addedChildPackages = new ArrayMap();
                                }
                                res.addedChildPackages.put(childPkg.packageName, childRes);
                            }
                        }
                    }
                    if (TextUtils.isEmpty(pkg.cpuAbiOverride)) {
                        pkg.cpuAbiOverride = args.abiOverride;
                    }
                    pkgName = pkg.packageName;
                    res.name = pkgName;
                    if (!isAppInstallAllowed(installerPackageName, pkgName) || (args.origin != null && isUnAppInstallAllowed(args.origin.resolvedPath))) {
                        res.setError(-111, "Disallow install new apps");
                        Slog.i(TAG, installerPackageName + " is disallowed to install new app " + pkgName);
                        return;
                    } else if ((pkg.applicationInfo.flags & 256) == 0 || (installFlags & 4) != 0) {
                        computeMetaHash(pkg);
                        try {
                            if (args.certificates != null) {
                                try {
                                    PackageParser.populateCertificates(pkg, args.certificates);
                                } catch (PackageParserException e) {
                                    PackageParser.collectCertificates(pkg, parseFlags);
                                }
                            } else {
                                PackageParser.collectCertificates(pkg, parseFlags);
                            }
                            if (this.mHwPMSEx.isDisallowedInstallApk(pkg)) {
                                res.setError(-111, "PackageName is disallowd to be installed");
                                Slog.i(TAG, installerPackageName + " is disallowed to be installed");
                                return;
                            }
                            try {
                                if (checkUninstalledSystemApp(pkg, args, res)) {
                                    Slog.i(TAG, "restore the uninstalled app and upgrad it");
                                    installFlags |= 2;
                                }
                                String str = null;
                                boolean systemApp = false;
                                synchronized (this.mPackages) {
                                    if ((installFlags & 2) != 0) {
                                        String oldName = this.mSettings.getRenamedPackageLPr(pkgName);
                                        if (pkg.mOriginalPackages != null && pkg.mOriginalPackages.contains(oldName) && this.mPackages.containsKey(oldName)) {
                                            pkg.setPackageName(oldName);
                                            pkgName = pkg.packageName;
                                            replace = true;
                                        } else if (this.mPackages.containsKey(pkgName)) {
                                            replace = true;
                                        }
                                        if (pkg.parentPackage != null) {
                                            res.setError(-106, "Package " + pkg.packageName + " is child of package " + pkg.parentPackage.parentPackage + ". Child packages " + "can be updated only through the parent package.");
                                            return;
                                        } else if (replace) {
                                            Package oldPackage = (Package) this.mPackages.get(pkgName);
                                            int oldTargetSdk = oldPackage.applicationInfo.targetSdkVersion;
                                            int newTargetSdk = pkg.applicationInfo.targetSdkVersion;
                                            if (oldTargetSdk <= 22 || newTargetSdk > 22) {
                                                int oldTargetSandbox = oldPackage.applicationInfo.targetSandboxVersion;
                                                int newTargetSandbox = pkg.applicationInfo.targetSandboxVersion;
                                                if (oldTargetSandbox == 2 && newTargetSandbox != 2) {
                                                    res.setError(-27, "Package " + pkg.packageName + " new target sandbox " + newTargetSandbox + " is incompatible with the previous value of" + oldTargetSandbox + ".");
                                                    return;
                                                } else if (oldPackage.parentPackage != null) {
                                                    res.setError(-106, "Package " + pkg.packageName + " is child of package " + oldPackage.parentPackage + ". Child packages " + "can be updated only through the parent package.");
                                                    return;
                                                } else if (isSystemApp(oldPackage) && pkgName != null) {
                                                    if ((pkgName.equals(this.mRequiredVerifierPackage) ^ 1) != 0) {
                                                        if ((pkgName.equals(this.mRequiredInstallerPackage) ^ 1) != 0) {
                                                            if ((pkgName.equals(this.mRequiredUninstallerPackage) ^ 1) != 0) {
                                                                boolean hasPackageMimeType;
                                                                for (Activity a : pkg.activities) {
                                                                    for (ActivityIntentInfo filter : a.intents) {
                                                                        boolean hasInstallAction = filter.matchAction("android.intent.action.INSTALL_PACKAGE");
                                                                        boolean hasDefaultCategory = filter.hasCategory("android.intent.category.DEFAULT");
                                                                        hasPackageMimeType = filter.hasDataType(PACKAGE_MIME_TYPE);
                                                                        if (hasInstallAction && hasDefaultCategory && hasPackageMimeType) {
                                                                            res.setError(-112, "Detect dangerous App, may cause system problem!");
                                                                            res.origPackage = this.mRequiredInstallerPackage;
                                                                            return;
                                                                        }
                                                                        boolean hasUninstallAction = filter.matchAction("android.intent.action.UNINSTALL_PACKAGE");
                                                                        boolean hasPackageScheme = filter.hasDataScheme("package");
                                                                        if (hasUninstallAction && hasDefaultCategory && hasPackageScheme) {
                                                                            res.setError(-112, "Detect dangerous App, may cause system problem!");
                                                                            res.origPackage = this.mRequiredUninstallerPackage;
                                                                            return;
                                                                        }
                                                                    }
                                                                }
                                                                for (Activity a2 : pkg.receivers) {
                                                                    for (ActivityIntentInfo filter2 : a2.intents) {
                                                                        boolean hasVerifierAction = filter2.matchAction("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
                                                                        hasPackageMimeType = filter2.hasDataType(PACKAGE_MIME_TYPE);
                                                                        if (hasVerifierAction && hasPackageMimeType) {
                                                                            res.setError(-112, "Detect dangerous App, may cause system problem!");
                                                                            res.origPackage = this.mRequiredVerifierPackage;
                                                                            return;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                res.setError(-26, "Package " + pkg.packageName + " new target SDK " + newTargetSdk + " doesn't support runtime permissions but the old" + " target SDK " + oldTargetSdk + " does.");
                                                return;
                                            }
                                        }
                                    }
                                    ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
                                    checkHwCertification(pkg, true);
                                    replaceSignatureIfNeeded(ps, pkg, false, true);
                                    if (ps != null) {
                                        PackageSetting signatureCheckPs = ps;
                                        if (pkg.applicationInfo.isStaticSharedLibrary()) {
                                            SharedLibraryEntry libraryEntry = getLatestSharedLibraVersionLPr(pkg);
                                            if (libraryEntry != null) {
                                                signatureCheckPs = this.mSettings.getPackageLPr(libraryEntry.apk);
                                            }
                                        }
                                        if (!shouldCheckUpgradeKeySetLP(signatureCheckPs, scanFlags)) {
                                            try {
                                                verifySignaturesLP(signatureCheckPs, pkg);
                                            } catch (PackageManagerException e2) {
                                                if (ps.sharedUser == null && isSystemSignatureUpdated(ps.signatures.mSignatures, pkg.mSignatures)) {
                                                    Slog.d(TAG, pkg.packageName + " system signature updated");
                                                    ps.signatures.mSignatures = pkg.mSignatures;
                                                } else {
                                                    res.setError(e2.error, e2.getMessage());
                                                    return;
                                                }
                                            }
                                        } else if (!checkUpgradeKeySetLP(signatureCheckPs, pkg)) {
                                            res.setError(-7, "Package " + pkg.packageName + " upgrade keys do not match the " + "previously installed version");
                                            return;
                                        }
                                        if (SystemProperties.get("ro.config.hw_optb", "0").equals("156")) {
                                            try {
                                                verifyValidVerifierInstall(installerPackageName, pkgName, args.user.getIdentifier(), Binder.getCallingUid());
                                            } catch (PackageManagerException e22) {
                                                res.setError(e22.error, e22.getMessage());
                                                return;
                                            }
                                        }
                                        str = ((PackageSetting) this.mSettings.mPackages.get(pkgName)).codePathString;
                                        if (!(ps.pkg == null || ps.pkg.applicationInfo == null)) {
                                            systemApp = (ps.pkg.applicationInfo.flags & 1) != 0;
                                        }
                                        res.origUsers = ps.queryInstalledUsers(sUserManager.getUserIds(), true);
                                    }
                                    for (i = pkg.permissions.size() - 1; i >= 0; i--) {
                                        Permission perm = (Permission) pkg.permissions.get(i);
                                        BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(perm.info.name);
                                        if (!((perm.info.protectionLevel & 4096) == 0 || (systemApp ^ 1) == 0)) {
                                            Slog.w(TAG, "Non-System package " + pkg.packageName + " attempting to delcare ephemeral permission " + perm.info.name + "; Removing ephemeral.");
                                            PermissionInfo permissionInfo = perm.info;
                                            permissionInfo.protectionLevel &= -4097;
                                        }
                                        if (bp != null) {
                                            boolean sigsOk = (bp.sourcePackage.equals(pkg.packageName) && (bp.packageSetting instanceof PackageSetting) && shouldCheckUpgradeKeySetLP((PackageSetting) bp.packageSetting, scanFlags)) ? checkUpgradeKeySetLP((PackageSetting) bp.packageSetting, pkg) : compareSignatures(bp.packageSetting.signatures.mSignatures, pkg.mSignatures) == 0;
                                            if (sigsOk) {
                                                if (!(PLATFORM_PACKAGE_NAME.equals(pkg.packageName) || (perm.info.protectionLevel & 15) != 1 || bp == null || (bp.isRuntime() ^ 1) == 0)) {
                                                    Slog.w(TAG, "Package " + pkg.packageName + " trying to change a " + "non-runtime permission " + perm.info.name + " to runtime; keeping old protection level");
                                                    perm.info.protectionLevel = bp.protectionLevel;
                                                }
                                            } else if (bp.sourcePackage.equals(PLATFORM_PACKAGE_NAME)) {
                                                Slog.w(TAG, "Package " + pkg.packageName + " attempting to redeclare system permission " + perm.info.name + "; ignoring new declaration");
                                                pkg.permissions.remove(i);
                                            } else if (((bp.protectionLevel | perm.info.protectionLevel) & 15) == 2) {
                                                res.setError(-112, "Package " + pkg.packageName + " attempting to redeclare permission " + perm.info.name + " already owned by " + bp.sourcePackage);
                                                res.origPermission = perm.info.name;
                                                res.origPackage = bp.sourcePackage;
                                                return;
                                            } else {
                                                Slog.w(TAG, "Package " + pkg.packageName + " attempting to redeclare permission " + perm.info.name + " already owned by " + bp.sourcePackage + "; ignoring new declaration");
                                                pkg.permissions.remove(i);
                                            }
                                        }
                                    }
                                }
                            } catch (PackageManagerException e3) {
                                Slog.i(TAG, "downgrade package from preset system app, just restore the uninstalled system app");
                                return;
                            }
                        } catch (PackageParserException e4) {
                            res.setError("Failed collect during installPackageLI", e4);
                            return;
                        }
                    } else {
                        res.setError(-15, "installPackageLI");
                        return;
                    }
                } else {
                    Slog.w(TAG, "Instant app package " + pkg.packageName + " does not target targetSandboxVersion 2");
                    res.setError(-27, "Instant app package must use targetSanboxVersion 2");
                    return;
                }
            } catch (PackageParserException e42) {
                res.setError("Failed parse during installPackageLI", e42);
                return;
            } catch (Throwable th2) {
                Trace.traceEnd(262144);
            }
        } else {
            res.setReturnCode(RequestStatus.SYS_ETIMEDOUT);
            Log.w(TAG, "can not install packages before FRP unlock");
            return;
        }
        PackageFreezer packageFreezer;
        if (th == null) {
            throw th;
        }
        synchronized (this.mPackages) {
            ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
            if (ps != null) {
                res.newUsers = ps.queryInstalledUsers(sUserManager.getUserIds(), true);
                ps.setUpdateAvailable(false);
            }
            if (res.returnCode == 1) {
                writeCertCompatPackages(false);
            }
            childCount = pkg.childPackages == null ? pkg.childPackages.size() : 0;
            for (i = 0; i < childCount; i++) {
                childPkg = (Package) pkg.childPackages.get(i);
                childRes = (PackageInstalledInfo) res.addedChildPackages.get(childPkg.packageName);
                childPs = this.mSettings.getPackageLPr(childPkg.packageName);
                if (childPs != null) {
                    childRes.newUsers = childPs.queryInstalledUsers(sUserManager.getUserIds(), true);
                }
            }
            if (res.returnCode == 1) {
                updateSequenceNumberLP(ps, res.newUsers);
                updateInstantAppInstallerLocked(pkgName);
            }
        }
        if (res.returnCode == 1) {
            recordInstallAppInfo(pkg.packageName, installBeginTime, installFlags);
        }
        return;
        if (packageFreezer != null) {
            try {
                packageFreezer.close();
            } catch (Throwable th3) {
                if (th == null) {
                    th = th3;
                } else if (th != th3) {
                    th.addSuppressed(th3);
                }
            }
        }
        if (th != null) {
            throw th;
        }
        Throwable th4;
        throw th4;
        if (th != null) {
            throw th;
        }
        return;
        if (this.mCustPms != null && this.mCustPms.needDerivePkgAbi(pkg)) {
            try {
                derivePackageAbi(pkg, new File(pkg.codePath), args.abiOverride, true, this.mAppLib32InstallDir);
            } catch (Throwable pme) {
                Slog.e(TAG, "Error deriving application ABI install app to sdcard", pme);
                res.setError((int) RequestStatus.SYS_ETIMEDOUT, "Error deriving application ABI");
                return;
            }
        }
        if (args.doRename(res.returnCode, pkg, str)) {
            boolean performDexopt = (forwardLocked || (pkg.applicationInfo.isExternalAsec() ^ 1) == 0) ? false : (instantApp && Global.getInt(this.mContext.getContentResolver(), "instant_app_dexopt_enabled", 0) == 0) ? false : true;
            if (performDexopt) {
                Trace.traceBegin(262144, "dexopt");
                this.mPackageDexOptimizer.performDexOpt(pkg, pkg.usesLibraryFiles, null, getOrCreateCompilerPackageStats(pkg), this.mDexManager.getPackageUseInfoOrDefault(pkg.packageName), new DexoptOptions(pkg.packageName, 2, 4));
                Trace.traceEnd(262144);
            }
            BackgroundDexOptService.notifyPackageChanged(pkg.packageName);
            startIntentFilterVerifications(args.user.getIdentifier(), replace, pkg);
            th = null;
            packageFreezer = null;
            try {
                packageFreezer = freezePackageForInstall(pkgName, installFlags, "installPackageLI");
                if (replace) {
                    if (pkg.applicationInfo.isStaticSharedLibrary()) {
                        Package existingPkg = (Package) this.mPackages.get(pkg.packageName);
                        if (!(existingPkg == null || existingPkg.mVersionCode == pkg.mVersionCode)) {
                            res.setError(-5, "Packages declaring static-shared libs cannot be updated");
                            if (packageFreezer != null) {
                                try {
                                    packageFreezer.close();
                                } catch (Throwable th5) {
                                    th = th5;
                                }
                            }
                            if (th != null) {
                                throw th;
                            }
                            return;
                        }
                    }
                    replacePackageLIF(pkg, parseFlags, (scanFlags | 512) | 1048576, args.user, installerPackageName, res, args.installReason);
                } else {
                    installNewPackageLIF(pkg, parseFlags, scanFlags | 256, args.user, installerPackageName, volumeUuid, res, args.installReason);
                }
                if (packageFreezer != null) {
                    try {
                        packageFreezer.close();
                    } catch (Throwable th6) {
                        th = th6;
                    }
                }
                if (th == null) {
                    synchronized (this.mPackages) {
                        ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
                        if (ps != null) {
                            res.newUsers = ps.queryInstalledUsers(sUserManager.getUserIds(), true);
                            ps.setUpdateAvailable(false);
                        }
                        if (res.returnCode == 1) {
                            writeCertCompatPackages(false);
                        }
                        if (pkg.childPackages == null) {
                        }
                        for (i = 0; i < childCount; i++) {
                            childPkg = (Package) pkg.childPackages.get(i);
                            childRes = (PackageInstalledInfo) res.addedChildPackages.get(childPkg.packageName);
                            childPs = this.mSettings.getPackageLPr(childPkg.packageName);
                            if (childPs != null) {
                                childRes.newUsers = childPs.queryInstalledUsers(sUserManager.getUserIds(), true);
                            }
                        }
                        if (res.returnCode == 1) {
                            updateSequenceNumberLP(ps, res.newUsers);
                            updateInstantAppInstallerLocked(pkgName);
                        }
                    }
                    if (res.returnCode == 1) {
                        recordInstallAppInfo(pkg.packageName, installBeginTime, installFlags);
                    }
                    return;
                }
                throw th;
            } catch (Throwable th7) {
                Throwable th8 = th7;
                th7 = th4;
                th4 = th8;
            }
        } else {
            res.setError(-4, "Failed rename");
        }
    }

    private void startIntentFilterVerifications(int userId, boolean replacing, Package pkg) {
        if (this.mIntentFilterVerifierComponent == null) {
            Slog.w(TAG, "No IntentFilter verification will not be done as there is no IntentFilterVerifier available!");
            return;
        }
        int i;
        String packageName = this.mIntentFilterVerifierComponent.getPackageName();
        if (userId == -1) {
            i = 0;
        } else {
            i = userId;
        }
        int verifierUid = getPackageUid(packageName, 268435456, i);
        Message msg = this.mHandler.obtainMessage(17);
        msg.obj = new IFVerificationParams(pkg, replacing, userId, verifierUid);
        this.mHandler.sendMessage(msg);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i2 = 0; i2 < childCount; i2++) {
            Package childPkg = (Package) pkg.childPackages.get(i2);
            msg = this.mHandler.obtainMessage(17);
            msg.obj = new IFVerificationParams(childPkg, replacing, userId, verifierUid);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void verifyIntentFiltersIfNeeded(int userId, int verifierUid, boolean replacing, Package pkg) {
        if (pkg.activities.size() != 0 && hasDomainURLs(pkg)) {
            int count = 0;
            String packageName = pkg.packageName;
            synchronized (this.mPackages) {
                if (!replacing) {
                    if (this.mSettings.getIntentFilterVerificationLPr(packageName) != null) {
                        return;
                    }
                }
                boolean needToVerify = false;
                for (Activity a : pkg.activities) {
                    for (ActivityIntentInfo filter : a.intents) {
                        if (filter.needsVerification() && needsNetworkVerificationLPr(filter)) {
                            needToVerify = true;
                            break;
                        }
                    }
                }
                if (needToVerify) {
                    int verificationId = this.mIntentFilterVerificationToken;
                    this.mIntentFilterVerificationToken = verificationId + 1;
                    for (Activity a2 : pkg.activities) {
                        for (ActivityIntentInfo filter2 : a2.intents) {
                            if (filter2.handlesWebUris(true) && needsNetworkVerificationLPr(filter2)) {
                                this.mIntentFilterVerifier.addOneIntentFilterVerification(verifierUid, userId, verificationId, filter2, packageName);
                                count++;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean needsNetworkVerificationLPr(ActivityIntentInfo filter) {
        IntentFilterVerificationInfo ivi = this.mSettings.getIntentFilterVerificationLPr(filter.activity.getComponentName().getPackageName());
        if (ivi == null) {
            return true;
        }
        switch (ivi.getStatus()) {
            case 0:
            case 1:
                return true;
            default:
                return false;
        }
    }

    private static boolean isMultiArch(ApplicationInfo info) {
        return (info.flags & Integer.MIN_VALUE) != 0;
    }

    private static boolean isExternal(Package pkg) {
        return (pkg.applicationInfo.flags & 262144) != 0;
    }

    private static boolean isExternal(PackageSetting ps) {
        return (ps.pkgFlags & 262144) != 0;
    }

    private static boolean isSystemApp(Package pkg) {
        return (pkg.applicationInfo.flags & 1) != 0;
    }

    private static boolean isPrivilegedApp(Package pkg) {
        return (pkg.applicationInfo.privateFlags & 8) != 0;
    }

    private static boolean hasDomainURLs(Package pkg) {
        return (pkg.applicationInfo.privateFlags & 16) != 0;
    }

    private static boolean isSystemApp(PackageSetting ps) {
        return (ps.pkgFlags & 1) != 0;
    }

    private static boolean isUpdatedSystemApp(PackageSetting ps) {
        return (ps.pkgFlags & 128) != 0;
    }

    private int packageFlagsToInstallFlags(PackageSetting ps) {
        int installFlags = 0;
        if (isExternal(ps) && TextUtils.isEmpty(ps.volumeUuid)) {
            installFlags = 8;
        }
        if (ps.isForwardLocked()) {
            return installFlags | 1;
        }
        return installFlags;
    }

    private String getVolumeUuidForPackage(Package pkg) {
        if (!isExternal(pkg)) {
            return StorageManager.UUID_PRIVATE_INTERNAL;
        }
        if (TextUtils.isEmpty(pkg.volumeUuid)) {
            return "primary_physical";
        }
        return pkg.volumeUuid;
    }

    private VersionInfo getSettingsVersionForPackage(Package pkg) {
        if (!isExternal(pkg)) {
            return this.mSettings.getInternalVersion();
        }
        if (TextUtils.isEmpty(pkg.volumeUuid)) {
            return this.mSettings.getExternalVersion();
        }
        return this.mSettings.findOrCreateVersion(pkg.volumeUuid);
    }

    private void deleteTempPackageFiles() {
        for (File file : this.mDrmAppPrivateInstallDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("vmdl") ? name.endsWith(".tmp") : false;
            }
        })) {
            file.delete();
        }
    }

    public void deletePackageAsUser(String packageName, int versionCode, IPackageDeleteObserver observer, int userId, int flags) {
        deletePackageVersioned(new VersionedPackage(packageName, versionCode), new LegacyPackageDeleteObserver(observer).getBinder(), userId, flags);
    }

    public void deletePackageVersioned(VersionedPackage versionedPackage, IPackageDeleteObserver2 observer, int userId, int deleteFlags) {
        final int callingUid = Binder.getCallingUid();
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        final boolean canViewInstantApps = canViewInstantApps(callingUid, userId);
        Preconditions.checkNotNull(versionedPackage);
        Preconditions.checkNotNull(observer);
        Preconditions.checkArgumentInRange(versionedPackage.getVersionCode(), -1, HwBootFail.STAGE_BOOT_SUCCESS, "versionCode must be >= -1");
        final String packageName = versionedPackage.getPackageName();
        final int versionCode = versionedPackage.getVersionCode();
        synchronized (this.mPackages) {
            final String internalPackageName = resolveInternalPackageNameLPr(versionedPackage.getPackageName(), versionedPackage.getVersionCode());
        }
        int uid = Binder.getCallingUid();
        if (isOrphaned(internalPackageName) || (isCallerAllowedToSilentlyUninstall(uid, internalPackageName) ^ 1) == 0) {
            final boolean deleteAllUsers = (deleteFlags & 2) != 0;
            final int[] users = deleteAllUsers ? sUserManager.getUserIds() : new int[]{userId};
            if (UserHandle.getUserId(uid) != userId || (deleteAllUsers && users.length > 1)) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "deletePackage for user " + userId);
            }
            if (isUserRestricted(userId, "no_uninstall_apps")) {
                try {
                    observer.onPackageDeleted(packageName, -3, null);
                } catch (RemoteException e) {
                }
                return;
            } else if (deleteAllUsers || !getBlockUninstallForUser(internalPackageName, userId)) {
                final int i = userId;
                final int i2 = deleteFlags;
                final IPackageDeleteObserver2 iPackageDeleteObserver2 = observer;
                this.mHandler.post(new Runnable() {
                    public void run() {
                        int returnCode;
                        PackageManagerService.this.mHandler.removeCallbacks(this);
                        PackageSetting ps = (PackageSetting) PackageManagerService.this.mSettings.mPackages.get(internalPackageName);
                        boolean doDeletePackage = true;
                        if (ps != null) {
                            if (ps.getInstantApp(UserHandle.getUserId(callingUid))) {
                                doDeletePackage = canViewInstantApps;
                            } else {
                                doDeletePackage = true;
                            }
                        }
                        if (!doDeletePackage) {
                            returnCode = -1;
                        } else if (deleteAllUsers) {
                            int[] blockUninstallUserIds = PackageManagerService.this.getBlockUninstallForUsers(internalPackageName, users);
                            if (ArrayUtils.isEmpty(blockUninstallUserIds)) {
                                returnCode = PackageManagerService.this.deletePackageX(internalPackageName, versionCode, i, i2);
                            } else {
                                int userFlags = i2 & -3;
                                for (int userId : users) {
                                    if (!ArrayUtils.contains(blockUninstallUserIds, userId)) {
                                        returnCode = PackageManagerService.this.deletePackageX(internalPackageName, versionCode, userId, userFlags);
                                        if (returnCode != 1) {
                                            Slog.w(PackageManagerService.TAG, "Package delete failed for user " + userId + ", returnCode " + returnCode);
                                        }
                                    }
                                }
                                returnCode = -4;
                            }
                        } else {
                            returnCode = PackageManagerService.this.deletePackageX(internalPackageName, versionCode, i, i2);
                        }
                        try {
                            iPackageDeleteObserver2.onPackageDeleted(packageName, returnCode, null);
                        } catch (RemoteException e) {
                            Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                        }
                    }
                });
                setNeedClearDeviceForCTS(false, packageName);
                Log.d(TAG, "setmNeedClearDeviceForCTS:false ");
                return;
            } else {
                try {
                    observer.onPackageDeleted(packageName, -4, null);
                } catch (RemoteException e2) {
                }
                return;
            }
        }
        try {
            Intent intent = new Intent("android.intent.action.UNINSTALL_PACKAGE");
            intent.setData(Uri.fromParts("package", packageName, null));
            intent.putExtra("android.content.pm.extra.CALLBACK", observer.asBinder());
            observer.onUserActionRequired(intent);
        } catch (RemoteException e3) {
        }
    }

    private String resolveExternalPackageNameLPr(Package pkg) {
        if (pkg.staticSharedLibName != null) {
            return pkg.manifestPackageName;
        }
        return pkg.packageName;
    }

    private String resolveInternalPackageNameLPr(String packageName, int versionCode) {
        String normalizedPackageName = this.mSettings.getRenamedPackageLPr(packageName);
        if (normalizedPackageName != null) {
            packageName = normalizedPackageName;
        }
        SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mStaticLibsByDeclaringPackage.get(packageName);
        if (versionedLib == null || versionedLib.size() <= 0) {
            return packageName;
        }
        SparseIntArray sparseIntArray = null;
        int callingAppId = UserHandle.getAppId(Binder.getCallingUid());
        if (!(callingAppId == 1000 || callingAppId == 2000 || callingAppId == 0)) {
            sparseIntArray = new SparseIntArray();
            String libName = ((SharedLibraryEntry) versionedLib.valueAt(0)).info.getName();
            String[] uidPackages = getPackagesForUid(Binder.getCallingUid());
            if (uidPackages != null) {
                for (String uidPackage : uidPackages) {
                    PackageSetting ps = this.mSettings.getPackageLPr(uidPackage);
                    int libIdx = ArrayUtils.indexOf(ps.usesStaticLibraries, libName);
                    if (libIdx >= 0) {
                        int libVersion = ps.usesStaticLibrariesVersions[libIdx];
                        sparseIntArray.append(libVersion, libVersion);
                    }
                }
            }
        }
        if (sparseIntArray != null && sparseIntArray.size() <= 0) {
            return packageName;
        }
        SharedLibraryEntry highestVersion = null;
        int versionCount = versionedLib.size();
        for (int i = 0; i < versionCount; i++) {
            SharedLibraryEntry libEntry = (SharedLibraryEntry) versionedLib.valueAt(i);
            if (sparseIntArray == null || sparseIntArray.indexOfKey(libEntry.info.getVersion()) >= 0) {
                int libVersionCode = libEntry.info.getDeclaringPackage().getVersionCode();
                if (versionCode != -1) {
                    if (libVersionCode == versionCode) {
                        return libEntry.apk;
                    }
                } else if (highestVersion == null) {
                    highestVersion = libEntry;
                } else if (libVersionCode > highestVersion.info.getDeclaringPackage().getVersionCode()) {
                    highestVersion = libEntry;
                }
            }
        }
        if (highestVersion != null) {
            return highestVersion.apk;
        }
        return packageName;
    }

    boolean isCallerVerifier(int callingUid) {
        int callingUserId = UserHandle.getUserId(callingUid);
        if (this.mRequiredVerifierPackage == null || callingUid != getPackageUid(this.mRequiredVerifierPackage, 0, callingUserId)) {
            return false;
        }
        return true;
    }

    private boolean isCallerAllowedToSilentlyUninstall(int callingUid, String pkgName) {
        if (callingUid == 2000 || callingUid == 0 || UserHandle.getAppId(callingUid) == 1000) {
            return true;
        }
        int callingUserId = UserHandle.getUserId(callingUid);
        if (callingUid == getPackageUid(getInstallerPackageName(pkgName), 0, callingUserId)) {
            return true;
        }
        if (this.mRequiredVerifierPackage != null && callingUid == getPackageUid(this.mRequiredVerifierPackage, 0, callingUserId)) {
            return true;
        }
        if (this.mRequiredUninstallerPackage == null || callingUid != getPackageUid(this.mRequiredUninstallerPackage, 0, callingUserId)) {
            return (this.mStorageManagerPackage != null && callingUid == getPackageUid(this.mStorageManagerPackage, 0, callingUserId)) || checkUidPermission("android.permission.MANAGE_PROFILE_AND_DEVICE_OWNERS", callingUid) == 0;
        } else {
            return true;
        }
    }

    private int[] getBlockUninstallForUsers(String packageName, int[] userIds) {
        int[] result = EMPTY_INT_ARRAY;
        for (int userId : userIds) {
            if (getBlockUninstallForUser(packageName, userId)) {
                result = ArrayUtils.appendInt(result, userId);
            }
        }
        return result;
    }

    public boolean isPackageDeviceAdminOnAnyUser(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) == null || (isCallerSameApp(packageName, callingUid) ^ 1) == 0) {
            return isPackageDeviceAdmin(packageName, -1);
        }
        return false;
    }

    private boolean isPackageDeviceAdmin(String packageName, int userId) {
        IDevicePolicyManager dpm = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        if (dpm != null) {
            try {
                Object obj;
                ComponentName deviceOwnerComponentName = dpm.getDeviceOwnerComponent(false);
                if (deviceOwnerComponentName == null) {
                    obj = null;
                } else {
                    obj = deviceOwnerComponentName.getPackageName();
                }
                if (packageName.equals(obj)) {
                    return true;
                }
                int[] users = userId == -1 ? sUserManager.getUserIds() : new int[]{userId};
                for (int packageHasActiveAdmins : users) {
                    if (dpm.packageHasActiveAdmins(packageName, packageHasActiveAdmins)) {
                        return true;
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    private boolean shouldKeepUninstalledPackageLPr(String packageName) {
        return this.mKeepUninstalledPackages != null ? this.mKeepUninstalledPackages.contains(packageName) : false;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int deletePackageX(String packageName, int versionCode, int userId, int deleteFlags) {
        PackageSetting uninstalledPs;
        Package pkg;
        int i;
        Throwable th;
        final PackageRemovedInfo info = new PackageRemovedInfo(this);
        int removeUser = (deleteFlags & 2) != 0 ? -1 : userId;
        if (isPackageDeviceAdmin(packageName, removeUser)) {
            Slog.w(TAG, "Not removing package " + packageName + ": has active device admin");
            return -2;
        } else if (HwDeviceManager.disallowOp(5, packageName)) {
            return -4;
        } else {
            synchronized (this.mPackages) {
                uninstalledPs = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (uninstalledPs == null) {
                    Slog.w(TAG, "Not removing non-existent package " + packageName);
                    return -1;
                }
                if (versionCode != -1) {
                    if (uninstalledPs.versionCode != versionCode) {
                        Slog.w(TAG, "Not removing package " + packageName + " with versionCode " + uninstalledPs.versionCode + " != " + versionCode);
                        return -1;
                    }
                }
                pkg = (Package) this.mPackages.get(packageName);
                int[] allUsers = sUserManager.getUserIds();
                if (!(pkg == null || pkg.staticSharedLibName == null)) {
                    SharedLibraryEntry libEntry = getSharedLibraryEntryLPr(pkg.staticSharedLibName, pkg.staticSharedLibVersion);
                    if (libEntry != null) {
                        for (int currUserId : allUsers) {
                            if (removeUser == -1 || removeUser == currUserId) {
                                List<VersionedPackage> libClientPackages = getPackagesUsingSharedLibraryLPr(libEntry.info, 0, currUserId);
                                if (!ArrayUtils.isEmpty(libClientPackages)) {
                                    Slog.w(TAG, "Not removing package " + pkg.manifestPackageName + " hosting lib " + libEntry.info.getName() + " version " + libEntry.info.getVersion() + " used by " + libClientPackages + " for user " + currUserId);
                                    return -6;
                                }
                            }
                        }
                    }
                }
                info.origUsers = uninstalledPs.queryInstalledUsers(allUsers, true);
            }
        }
        if (r26 != null) {
            throw r26;
        } else {
            synchronized (this.mPackages) {
                if (res) {
                    if (pkg != null) {
                        this.mInstantAppRegistry.onPackageUninstalledLPw(pkg, info.removedUsers);
                    }
                    updateSequenceNumberLP(uninstalledPs, info.removedUsers);
                    updateInstantAppInstallerLocked(packageName);
                }
            }
            if (res) {
                boolean removedForAllUsers = false;
                boolean systemUpdate = info.isRemovedPackageSystemUpdate;
                synchronized (this.mPackages) {
                    if (!systemUpdate) {
                        if (this.mPackages.get(packageName) == null) {
                            removedForAllUsers = true;
                        }
                    }
                }
                if (removedForAllUsers || systemUpdate) {
                    try {
                        updatePackageBlackListInfo(packageName);
                    } catch (Exception e) {
                        Slog.e(TAG, "update BlackListApp info failed");
                    }
                }
                sendIncompatibleNotificationIfNeeded(packageName);
                info.sendPackageRemovedBroadcasts((deleteFlags & 8) != 0);
                info.sendSystemPackageUpdatedBroadcasts();
                info.sendSystemPackageAppearedBroadcasts();
            }
            Runtime.getRuntime().gc();
            if (info.args != null) {
                if (this.mCustPms == null && this.mCustPms.isSdInstallEnabled()) {
                    this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            synchronized (PackageManagerService.this.mInstallLock) {
                                info.args.doPostDeleteLI(true);
                            }
                        }
                    }, 500);
                } else {
                    synchronized (this.mInstallLock) {
                        info.args.doPostDeleteLI(true);
                    }
                }
            }
            if (res) {
                i = -1;
            } else {
                i = 1;
            }
            return i;
        }
        sendIncompatibleNotificationIfNeeded(packageName);
        if ((deleteFlags & 8) != 0) {
        }
        info.sendPackageRemovedBroadcasts((deleteFlags & 8) != 0);
        info.sendSystemPackageUpdatedBroadcasts();
        info.sendSystemPackageAppearedBroadcasts();
        Runtime.getRuntime().gc();
        if (info.args != null) {
            if (this.mCustPms == null) {
            }
            synchronized (this.mInstallLock) {
                info.args.doPostDeleteLI(true);
            }
        }
        if (res) {
            i = -1;
        } else {
            i = 1;
        }
        return i;
        if (r16 != null) {
            try {
                r16.close();
            } catch (Throwable th2) {
                if (th == null) {
                    th = th2;
                } else if (th != th2) {
                    th.addSuppressed(th2);
                }
            }
        }
        if (th != null) {
            throw th;
        }
        throw r4;
    }

    private void removePackageDataLIF(PackageSetting ps, int[] allUserHandles, PackageRemovedInfo outInfo, int flags, boolean writeSettings) {
        String packageName = ps.name;
        synchronized (this.mPackages) {
            Package deletedPkg = (Package) this.mPackages.get(packageName);
            final PackageSetting deletedPs = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (outInfo != null) {
                int[] iArr;
                outInfo.removedPackage = packageName;
                outInfo.installerPackageName = ps.installerPackageName;
                boolean z = deletedPkg != null ? deletedPkg.staticSharedLibName != null : false;
                outInfo.isStaticSharedLib = z;
                if (deletedPs == null) {
                    iArr = null;
                } else {
                    iArr = deletedPs.queryInstalledUsers(sUserManager.getUserIds(), true);
                }
                outInfo.populateUsers(iArr, deletedPs);
            }
        }
        removePackageLI(ps, (Integer.MIN_VALUE & flags) != 0);
        if ((flags & 1) == 0) {
            Package resolvedPkg;
            if (deletedPkg != null) {
                resolvedPkg = deletedPkg;
            } else {
                resolvedPkg = new Package(ps.name);
                resolvedPkg.setVolumeUuid(ps.volumeUuid);
            }
            destroyAppDataLIF(resolvedPkg, -1, 3);
            destroyAppProfilesLIF(resolvedPkg, -1);
            if (outInfo != null) {
                outInfo.dataRemoved = true;
            }
            schedulePackageCleaning(packageName, -1, true);
        } else {
            Flog.i(206, "removePackageDataLI : " + ps.name + ", keep data");
        }
        int removedAppId = -1;
        synchronized (this.mPackages) {
            boolean installedStateChanged = false;
            if (deletedPs != null) {
                if ((flags & 1) == 0) {
                    clearIntentFilterVerificationsLPw(deletedPs.name, -1);
                    clearDefaultBrowserIfNeeded(packageName);
                    this.mSettings.mKeySetManagerService.removeAppKeySetDataLPw(packageName);
                    removedAppId = this.mSettings.removePackageLPw(packageName);
                    if (outInfo != null) {
                        outInfo.removedAppId = removedAppId;
                    }
                    updatePermissionsLPw(deletedPs.name, null, 0);
                    if (deletedPs.sharedUser != null) {
                        for (int userId : UserManagerService.getInstance().getUserIds()) {
                            int userIdToKill = this.mSettings.updateSharedUserPermsLPw(deletedPs, userId);
                            if (userIdToKill == -1 || userIdToKill >= 0) {
                                this.mHandler.post(new Runnable() {
                                    public void run() {
                                        PackageManagerService.this.killApplication(deletedPs.name, deletedPs.appId, PackageManagerService.KILL_APP_REASON_GIDS_CHANGED);
                                    }
                                });
                                break;
                            }
                        }
                    }
                    clearPackagePreferredActivitiesLPw(deletedPs.name, -1);
                }
                if (!(allUserHandles == null || outInfo == null || outInfo.origUsers == null)) {
                    for (int userId2 : allUserHandles) {
                        boolean installed = ArrayUtils.contains(outInfo.origUsers, userId2);
                        if (installed != ps.getInstalled(userId2)) {
                            installedStateChanged = true;
                        }
                        ps.setInstalled(installed, userId2);
                    }
                }
            }
            if (writeSettings) {
                this.mSettings.writeLPr();
                writePackagesAbi();
            }
            if (installedStateChanged) {
                this.mSettings.writeKernelMappingLPr(ps);
            }
        }
        if (removedAppId != -1) {
            removeKeystoreDataIfNeeded(-1, removedAppId);
        }
    }

    static boolean locationIsPrivileged(File path) {
        boolean z = true;
        try {
            if (!(path.getCanonicalPath().startsWith(new File(Environment.getRootDirectory(), "priv-app").getCanonicalPath()) || HwServiceFactory.isPrivAppNonSystemPartitionDir(path))) {
                z = HwServiceFactory.isPrivAppInCust(path);
            }
            return z;
        } catch (IOException e) {
            Slog.e(TAG, "Unable to access code path " + path);
            return false;
        }
    }

    private Package installPackageFromSystemLIF(File codePath, boolean isPrivileged, int[] allUserHandles, int[] origUserHandles, PermissionsState origPermissionState, boolean writeSettings) throws PackageManagerException {
        int parseFlags = ((this.mDefParseFlags | 4) | 1) | 64;
        if (isPrivileged || locationIsPrivileged(codePath)) {
            parseFlags |= 128;
        }
        Package newPkg = scanPackageTracedLI(codePath, parseFlags, 65536, 0, null);
        try {
            updateSharedLibrariesLPr(newPkg, null);
        } catch (PackageManagerException e) {
            Slog.e(TAG, "updateAllSharedLibrariesLPw failed: " + e.getMessage());
        }
        prepareAppDataAfterInstallLIF(newPkg);
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(newPkg.packageName);
            if (origPermissionState != null) {
                ps.getPermissionsState().copyFrom(origPermissionState);
            }
            updatePermissionsLPw(newPkg.packageName, newPkg, 3);
            boolean applyUserRestrictions = (allUserHandles == null || origUserHandles == null) ? false : true;
            if (applyUserRestrictions) {
                boolean installedStateChanged = false;
                for (int userId : allUserHandles) {
                    boolean installed = ArrayUtils.contains(origUserHandles, userId);
                    if (installed != ps.getInstalled(userId)) {
                        installedStateChanged = true;
                    }
                    ps.setInstalled(installed, userId);
                    this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
                }
                this.mSettings.writeAllUsersPackageRestrictionsLPr();
                if (installedStateChanged) {
                    this.mSettings.writeKernelMappingLPr(ps);
                }
            }
            if (writeSettings) {
                this.mSettings.writeLPr();
            }
        }
        return newPkg;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean deleteInstalledPackageLIF(PackageSetting ps, boolean deleteCodeAndResources, int flags, int[] allUserHandles, PackageRemovedInfo outInfo, boolean writeSettings, Package replacingPackage) {
        synchronized (this.mPackages) {
            if (outInfo != null) {
                outInfo.uid = ps.appId;
            }
            if (!(outInfo == null || outInfo.removedChildPackages == null)) {
                int childCount = ps.childPackageNames != null ? ps.childPackageNames.size() : 0;
                for (int i = 0; i < childCount; i++) {
                    String childPackageName = (String) ps.childPackageNames.get(i);
                    PackageSetting childPs = (PackageSetting) this.mSettings.mPackages.get(childPackageName);
                    if (childPs == null) {
                        return false;
                    }
                    PackageRemovedInfo childInfo = (PackageRemovedInfo) outInfo.removedChildPackages.get(childPackageName);
                    if (childInfo != null) {
                        childInfo.uid = childPs.appId;
                    }
                }
            }
        }
    }

    public boolean setBlockUninstallForUser(String packageName, boolean blockUninstall, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_PACKAGES", null);
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null || pkg.staticSharedLibName == null) {
                this.mSettings.setBlockUninstallLPw(userId, packageName, blockUninstall);
                this.mSettings.writePackageRestrictionsLPr(userId);
                return true;
            }
            Slog.w(TAG, "Cannot block uninstall of package: " + packageName + " providing static shared library: " + pkg.staticSharedLibName);
            return false;
        }
    }

    public boolean getBlockUninstallForUser(String packageName, int userId) {
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null || filterAppAccessLPr(ps, Binder.getCallingUid(), userId)) {
                return false;
            }
            boolean blockUninstallLPr = this.mSettings.getBlockUninstallLPr(userId, packageName);
            return blockUninstallLPr;
        }
    }

    public boolean setRequiredForSystemUser(String packageName, boolean systemUserApp) {
        enforceSystemOrRoot("setRequiredForSystemUser can only be run by the system or root");
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Log.w(TAG, "Package doesn't exist: " + packageName);
                return false;
            }
            if (systemUserApp) {
                ps.pkgPrivateFlags |= 512;
            } else {
                ps.pkgPrivateFlags &= -513;
            }
            this.mSettings.writeLPr();
            return true;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean deletePackageLIF(String packageName, UserHandle user, boolean deleteCodeAndResources, int[] allUserHandles, int flags, PackageRemovedInfo outInfo, boolean writeSettings, Package replacingPackage) {
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
                return false;
            } else if (ps.parentPackageName == null || (isSystemApp(ps) && (flags & 4) == 0)) {
            } else {
                int removedUserId;
                if (user != null) {
                    removedUserId = user.getIdentifier();
                } else {
                    removedUserId = -1;
                }
                if (clearPackageStateForUserLIF(ps, removedUserId, outInfo)) {
                    markPackageUninstalledForUserLPw(ps, user);
                    scheduleWritePackageRestrictionsLocked(user);
                    return true;
                }
                return false;
            }
        }
    }

    private void markPackageUninstalledForUserLPw(PackageSetting ps, UserHandle user) {
        int[] userIds = (user == null || user.getIdentifier() == -1) ? sUserManager.getUserIds() : new int[]{user.getIdentifier()};
        for (int nextUserId : userIds) {
            ps.setUserState(nextUserId, 0, 0, false, true, true, false, false, false, false, null, null, null, ps.readUserState(nextUserId).domainVerificationStatus, 0, 0);
        }
        this.mSettings.writeKernelMappingLPr(ps);
    }

    private boolean clearPackageStateForUserLIF(PackageSetting ps, int userId, PackageRemovedInfo outInfo) {
        boolean z = false;
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(ps.name);
        }
        int[] userIds = userId == -1 ? sUserManager.getUserIds() : new int[]{userId};
        for (int nextUserId : userIds) {
            destroyAppDataLIF(pkg, userId, 3);
            destroyAppProfilesLIF(pkg, userId);
            clearDefaultBrowserIfNeededForUser(ps.name, userId);
            removeKeystoreDataIfNeeded(nextUserId, ps.appId);
            schedulePackageCleaning(ps.name, nextUserId, false);
            synchronized (this.mPackages) {
                if (clearPackagePreferredActivitiesLPw(ps.name, nextUserId)) {
                    scheduleWritePackageRestrictionsLocked(nextUserId);
                }
                resetUserChangesToRuntimePermissionsAndFlagsLPw(ps, nextUserId);
            }
        }
        if (outInfo != null) {
            outInfo.removedPackage = ps.name;
            outInfo.installerPackageName = ps.installerPackageName;
            if (!(pkg == null || pkg.staticSharedLibName == null)) {
                z = true;
            }
            outInfo.isStaticSharedLib = z;
            outInfo.removedAppId = ps.appId;
            outInfo.removedUsers = userIds;
            outInfo.broadcastUsers = userIds;
        }
        return true;
    }

    private void checkMemoryExec(boolean succeeded) {
        if (succeeded) {
            DeviceStorageMonitorInternal dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
            if (dsm != null) {
                dsm.checkMemory();
            }
        }
    }

    private void removeCompletedExec(String packageName, IPackageDataObserver observer, boolean succeeded) {
        if (observer != null) {
            try {
                observer.onRemoveCompleted(packageName, succeeded);
            } catch (RemoteException e) {
                Log.i(TAG, "Observer no longer exists.");
            }
        }
    }

    private void clearApplicationUserDataExec(String packageName, int userId, boolean allData, boolean succeeded, IPackageDataObserver observer) {
        final String str = packageName;
        final int i = userId;
        final boolean z = allData;
        final boolean z2 = succeeded;
        final IPackageDataObserver iPackageDataObserver = observer;
        this.clearDirectoryThread.submit(new Runnable() {
            public void run() {
                PackageManagerService.this.clearExternalStorageDataSync(str, i, z);
                PackageManagerService.this.checkMemoryExec(z2);
                PackageManagerService.this.removeCompletedExec(str, iPackageDataObserver, z2);
            }
        });
    }

    private void deleteApplicationCacheFilesExec(String packageName, int userId, boolean allData, boolean succeeded, IPackageDataObserver observer) {
        final String str = packageName;
        final int i = userId;
        final boolean z = allData;
        final IPackageDataObserver iPackageDataObserver = observer;
        final boolean z2 = succeeded;
        this.clearDirectoryThread.submit(new Runnable() {
            public void run() {
                PackageManagerService.this.clearExternalStorageDataSync(str, i, z);
                PackageManagerService.this.removeCompletedExec(str, iPackageDataObserver, z2);
            }
        });
    }

    private void clearExternalStorageDataSync(String packageName, int userId, boolean allData) {
        if (packageName == null) {
            Slog.w(TAG, "clearExternalStorageDataSync packageName is null!");
        } else if (!DEFAULT_CONTAINER_PACKAGE.equals(packageName)) {
            boolean z;
            if (Environment.isExternalStorageEmulated()) {
                z = true;
            } else {
                String status = Environment.getExternalStorageState();
                if (status.equals("mounted")) {
                    z = true;
                } else {
                    z = status.equals("mounted_ro");
                }
            }
            if (z) {
                Intent containerIntent = new Intent().setComponent(DEFAULT_CONTAINER_COMPONENT);
                int[] users = userId == -1 ? sUserManager.getUserIds() : new int[]{userId};
                PackageManagerService packageManagerService = this;
                ClearStorageConnection conn = new ClearStorageConnection();
                if (this.mContext.bindServiceAsUser(containerIntent, conn, 1, UserHandle.SYSTEM)) {
                    for (int curUser : users) {
                        long timeout = SystemClock.uptimeMillis() + ((WATCHDOG_TIMEOUT * 4) / 5);
                        synchronized (conn) {
                            while (conn.mContainerService == null) {
                                long now = SystemClock.uptimeMillis();
                                if (now < timeout) {
                                    try {
                                        conn.wait(timeout - now);
                                    } catch (InterruptedException e) {
                                    }
                                    now = SystemClock.uptimeMillis();
                                }
                            }
                            try {
                            } finally {
                                this.mContext.unbindService(conn);
                            }
                        }
                        if (conn.mContainerService == null) {
                            Slog.w(TAG, "clearExternalStorageDataSync fail reason: Bind ContainerService Timeout");
                            return;
                        }
                        UserEnvironment userEnv = new UserEnvironment(curUser);
                        clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppCacheDirs(packageName));
                        if (allData) {
                            clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppDataDirs(packageName));
                            clearDirectory(conn.mContainerService, userEnv.buildExternalStorageAppMediaDirs(packageName));
                        }
                    }
                    this.mContext.unbindService(conn);
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void clearApplicationProfileData(String packageName) {
        Throwable th = null;
        enforceSystemOrRoot("Only the system can clear all profile data");
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
        }
        PackageFreezer packageFreezer = null;
        try {
            packageFreezer = freezePackage(packageName, "clearApplicationProfileData");
            synchronized (this.mInstallLock) {
                clearAppProfilesLIF(pkg, -1);
            }
            if (packageFreezer != null) {
                try {
                    packageFreezer.close();
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            if (th != null) {
                throw th;
            }
        } catch (Throwable th3) {
            Throwable th4 = th3;
            if (packageFreezer != null) {
                try {
                    packageFreezer.close();
                } catch (Throwable th5) {
                    if (th == null) {
                        th = th5;
                    } else if (th != th5) {
                        th.addSuppressed(th5);
                    }
                }
            }
            if (th != null) {
                throw th;
            }
            throw th4;
        }
    }

    public void clearApplicationUserData(String packageName, IPackageDataObserver observer, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CLEAR_APP_USER_DATA", null);
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "clear application data");
        PackageSetting ps = this.mSettings.getPackageLPr(packageName);
        final boolean filterAppAccessLPr = ps != null ? filterAppAccessLPr(ps, callingUid, userId) : false;
        if (filterAppAccessLPr || !this.mProtectedPackages.isPackageDataProtected(userId, packageName)) {
            final String str = packageName;
            final int i = userId;
            final IPackageDataObserver iPackageDataObserver = observer;
            this.mHandler.post(new Runnable() {
                /* JADX WARNING: inconsistent code. */
                /* Code decompiled incorrectly, please refer to instructions dump. */
                public void run() {
                    boolean succeeded;
                    Throwable th = null;
                    PackageManagerService.this.mHandler.removeCallbacks(this);
                    if (filterAppAccessLPr) {
                        succeeded = false;
                    } else {
                        PackageFreezer packageFreezer = null;
                        try {
                            packageFreezer = PackageManagerService.this.freezePackage(str, "clearApplicationUserData");
                            long start = SystemClock.uptimeMillis();
                            synchronized (PackageManagerService.this.mInstallLock) {
                                succeeded = PackageManagerService.this.clearApplicationUserDataLIF(str, i);
                            }
                            start = PackageManagerService.this.printClearDataTimeoutLogs("UserData", start, str);
                            PackageManagerService.this.clearExternalStorageDataSync(str, i, true);
                            start = PackageManagerService.this.printClearDataTimeoutLogs("ExternalStorageData", start, str);
                            synchronized (PackageManagerService.this.mPackages) {
                                PackageManagerService.this.mInstantAppRegistry.deleteInstantApplicationMetadataLPw(str, i);
                            }
                            start = PackageManagerService.this.printClearDataTimeoutLogs("Metadata", start, str);
                            Slog.i(PackageManagerService.TAG, "clearApplicationUserData, finish freeze package:" + str);
                            if (packageFreezer != null) {
                                try {
                                    packageFreezer.close();
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            if (th != null) {
                                throw th;
                            } else if (succeeded) {
                                DeviceStorageMonitorInternal dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
                                if (dsm != null) {
                                    dsm.checkMemory();
                                }
                                IndexSearchManager.getInstance().clearUserIndexSearchData(str, i);
                            }
                        } catch (Throwable th3) {
                            Throwable th4 = th3;
                            if (packageFreezer != null) {
                                try {
                                    packageFreezer.close();
                                } catch (Throwable th5) {
                                    if (th == null) {
                                        th = th5;
                                    } else if (th != th5) {
                                        th.addSuppressed(th5);
                                    }
                                }
                            }
                            if (th != null) {
                                throw th;
                            }
                            throw th4;
                        }
                    }
                    if (iPackageDataObserver != null) {
                        try {
                            iPackageDataObserver.onRemoveCompleted(str, succeeded);
                        } catch (RemoteException e) {
                            Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                        }
                    }
                }
            });
            return;
        }
        throw new SecurityException("Cannot clear data for a protected package: " + packageName);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean clearApplicationUserDataLIF(String packageName, int userId) {
        if (packageName == null) {
            Slog.w(TAG, "Attempt to delete null packageName.");
            return false;
        }
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null) {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (ps != null) {
                    pkg = ps.pkg;
                }
            }
            if (pkg == null) {
                Slog.w(TAG, "Package named '" + packageName + "' doesn't exist.");
                return false;
            }
            resetUserChangesToRuntimePermissionsAndFlagsLPw((PackageSetting) pkg.mExtras, userId);
        }
    }

    private void resetUserChangesToRuntimePermissionsAndFlagsLPw(int userId) {
        int packageCount = this.mPackages.size();
        for (int i = 0; i < packageCount; i++) {
            resetUserChangesToRuntimePermissionsAndFlagsLPw(((Package) this.mPackages.valueAt(i)).mExtras, userId);
        }
    }

    private void resetNetworkPolicies(int userId) {
        ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).resetUserState(userId);
    }

    private void resetUserChangesToRuntimePermissionsAndFlagsLPw(PackageSetting ps, int userId) {
        if (ps.pkg != null) {
            boolean writeInstallPermissions = false;
            boolean writeRuntimePermissions = false;
            int permissionCount = ps.pkg.requestedPermissions.size();
            for (int i = 0; i < permissionCount; i++) {
                String permission = (String) ps.pkg.requestedPermissions.get(i);
                BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(permission);
                if (bp != null) {
                    if (ps.sharedUser != null) {
                        boolean used = false;
                        int packageCount = ps.sharedUser.packages.size();
                        int j = 0;
                        while (j < packageCount) {
                            PackageSetting pkg = (PackageSetting) ps.sharedUser.packages.valueAt(j);
                            if (pkg.pkg == null || (pkg.pkg.packageName.equals(ps.pkg.packageName) ^ 1) == 0 || !pkg.pkg.requestedPermissions.contains(permission)) {
                                j++;
                            } else {
                                used = true;
                                if (used) {
                                }
                            }
                        }
                        if (used) {
                        }
                    }
                    PermissionsState permissionsState = ps.getPermissionsState();
                    int oldFlags = permissionsState.getPermissionFlags(bp.name, userId);
                    boolean hasInstallState = permissionsState.getInstallPermissionState(bp.name) != null;
                    int flags = 0;
                    if (this.mPermissionReviewRequired && ps.pkg.applicationInfo.targetSdkVersion < 23) {
                        flags = 64;
                    }
                    if (permissionsState.updatePermissionFlags(bp, userId, 75, flags)) {
                        if (hasInstallState) {
                            writeInstallPermissions = true;
                        } else {
                            writeRuntimePermissions = true;
                        }
                    }
                    if (bp.isRuntime() && (oldFlags & 20) == 0) {
                        if ((oldFlags & 32) != 0) {
                            if (permissionsState.grantRuntimePermission(bp, userId) != -1) {
                                writeRuntimePermissions = true;
                            }
                        } else if ((flags & 64) == 0) {
                            switch (permissionsState.revokeRuntimePermission(bp, userId)) {
                                case 0:
                                case 1:
                                    writeRuntimePermissions = true;
                                    final int appId = ps.appId;
                                    final int i2 = userId;
                                    this.mHandler.post(new Runnable() {
                                        public void run() {
                                            PackageManagerService.this.killUid(appId, i2, PackageManagerService.KILL_APP_REASON_PERMISSIONS_REVOKED);
                                        }
                                    });
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
            if (writeRuntimePermissions) {
                this.mSettings.writeRuntimePermissionsForUserLPr(userId, true);
            }
            if (writeInstallPermissions) {
                this.mSettings.writeLPr();
            }
        }
    }

    private static void removeKeystoreDataIfNeeded(int userId, int appId) {
        if (appId >= 0) {
            KeyStore keyStore = KeyStore.getInstance();
            if (keyStore == null) {
                Slog.w(TAG, "Could not contact keystore to clear entries for app id " + appId);
            } else if (userId == -1) {
                for (int individual : sUserManager.getUserIds()) {
                    keyStore.clearUid(UserHandle.getUid(individual, appId));
                }
            } else {
                keyStore.clearUid(UserHandle.getUid(userId, appId));
            }
        }
    }

    public void deleteApplicationCacheFiles(String packageName, IPackageDataObserver observer) {
        if (packageName == null) {
            Slog.w(TAG, "Failed to delete cache files, for packageName is null!");
        } else {
            deleteApplicationCacheFilesAsUser(packageName, UserHandle.getCallingUserId(), observer);
        }
    }

    public void deleteApplicationCacheFilesAsUser(String packageName, int userId, IPackageDataObserver observer) {
        final Package pkg;
        int callingUid = Binder.getCallingUid();
        this.mContext.enforceCallingOrSelfPermission("android.permission.DELETE_CACHE_FILES", null);
        enforceCrossUserPermission(callingUid, userId, true, false, "delete application cache files");
        final int hasAccessInstantApps = this.mContext.checkCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS");
        synchronized (this.mPackages) {
            pkg = (Package) this.mPackages.get(packageName);
        }
        final int i = callingUid;
        final int i2 = userId;
        final String str = packageName;
        final IPackageDataObserver iPackageDataObserver = observer;
        this.mHandler.post(new Runnable() {
            public void run() {
                PackageSetting packageSetting = pkg == null ? null : pkg.mExtras;
                boolean doClearData = true;
                if (packageSetting != null) {
                    doClearData = packageSetting.getInstantApp(UserHandle.getUserId(i)) ? hasAccessInstantApps == 0 : true;
                }
                if (doClearData) {
                    synchronized (PackageManagerService.this.mInstallLock) {
                        PackageManagerService.this.clearAppDataLIF(pkg, i2, LightsManager.LIGHT_ID_MANUALCUSTOMBACKLIGHT);
                        PackageManagerService.this.clearAppDataLIF(pkg, i2, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                    }
                    PackageManagerService.this.clearExternalStorageDataSync(str, i2, false);
                }
                if (iPackageDataObserver != null) {
                    try {
                        iPackageDataObserver.onRemoveCompleted(str, true);
                    } catch (RemoteException e) {
                        Log.i(PackageManagerService.TAG, "Observer no longer exists.");
                    }
                }
            }
        });
    }

    public void getPackageSizeInfo(String packageName, int userHandle, IPackageStatsObserver observer) {
        throw new UnsupportedOperationException("Shame on you for calling the hidden API getPackageSizeInfo(). Shame!");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean getPackageSizeInfoLI(String packageName, int userId, PackageStats stats) {
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                Slog.w(TAG, "Failed to find settings for " + packageName);
                return false;
            }
        }
    }

    private int getUidTargetSdkVersionLockedLPr(int uid) {
        SharedUserSetting obj = this.mSettings.getUserIdLPr(uid);
        PackageSetting ps;
        if (obj instanceof SharedUserSetting) {
            int vers = 10000;
            Iterator<PackageSetting> it = obj.packages.iterator();
            while (it.hasNext()) {
                ps = (PackageSetting) it.next();
                if (ps.pkg != null) {
                    int v = ps.pkg.applicationInfo.targetSdkVersion;
                    if (v < vers) {
                        vers = v;
                    }
                }
            }
            return vers;
        }
        if (obj instanceof PackageSetting) {
            ps = (PackageSetting) obj;
            if (ps.pkg != null) {
                return ps.pkg.applicationInfo.targetSdkVersion;
            }
        }
        return 10000;
    }

    public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        addPreferredActivityInternal(filter, match, set, activity, true, userId, "Adding preferred");
    }

    protected void addPreferredActivityInternal(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, boolean always, int userId, String opname) {
        int callingUid = Binder.getCallingUid();
        if (activity != null) {
            Slog.d(TAG, opname + "add pref activity " + activity.flattenToShortString() + " from uid " + Binder.getCallingUid());
        }
        enforceCrossUserPermission(callingUid, userId, true, false, "add preferred activity");
        if (filter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a preferred activity with no filter actions");
        } else if (!filter.hasCategory("android.intent.category.HOME") || activity == null || (this.mHwPMSEx.isAllowedSetHomeActivityForAntiMal(getPackageInfo(activity.getPackageName(), 0, userId), userId) ^ 1) == 0) {
            synchronized (this.mPackages) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                    if (getUidTargetSdkVersionLockedLPr(callingUid) < 8) {
                        Slog.w(TAG, "Ignoring addPreferredActivity() from uid " + callingUid);
                        return;
                    }
                    this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
                }
                PreferredIntentResolver pir = this.mSettings.editPreferredActivitiesLPw(userId);
                if (activity == null) {
                    Slog.w(TAG, "Cannot set a preferred activity with activity is null");
                    return;
                }
                Slog.i(TAG, opname + " activity " + activity.flattenToShortString() + " for user " + userId + ":");
                filter.dump(new LogPrinter(4, TAG), "  ");
                pir.addFilter(new PreferredActivity(filter, match, set, activity, always));
                scheduleWritePackageRestrictionsLocked(userId);
                postPreferredActivityChangedBroadcast(userId);
            }
        } else {
            Slog.i(TAG, "NOT ALLOWED TO add preferred activity current time!");
        }
    }

    private void postPreferredActivityChangedBroadcast(int userId) {
        this.mHandler.post(new com.android.server.pm.-$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E.AnonymousClass1(userId));
    }

    static /* synthetic */ void lambda$-com_android_server_pm_PackageManagerService_1081142(int userId) {
        IActivityManager am = ActivityManager.getService();
        if (am != null) {
            Intent intent = new Intent("android.intent.action.ACTION_PREFERRED_ACTIVITY_CHANGED");
            intent.putExtra("android.intent.extra.user_handle", userId);
            try {
                am.broadcastIntent(null, intent, null, null, 0, null, null, null, -1, null, false, false, userId);
            } catch (RemoteException e) {
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void replacePreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity, int userId) {
        if (filter.countActions() != 1) {
            throw new IllegalArgumentException("replacePreferredActivity expects filter to have only 1 action.");
        } else if (filter.countDataAuthorities() == 0 && filter.countDataPaths() == 0 && filter.countDataSchemes() <= 1 && filter.countDataTypes() == 0) {
            if (!filter.hasCategory("android.intent.category.HOME") || activity == null || (this.mHwPMSEx.isAllowedSetHomeActivityForAntiMal(getPackageInfo(activity.getPackageName(), 0, userId), userId) ^ 1) == 0) {
                int callingUid = Binder.getCallingUid();
                enforceCrossUserPermission(callingUid, userId, true, false, "replace preferred activity");
                synchronized (this.mPackages) {
                    if (this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                        if (getUidTargetSdkVersionLockedLPr(callingUid) < 8) {
                            Slog.w(TAG, "Ignoring replacePreferredActivity() from uid " + Binder.getCallingUid());
                            return;
                        }
                        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
                    }
                    PreferredIntentResolver pir = (PreferredIntentResolver) this.mSettings.mPreferredActivities.get(userId);
                    if (pir != null) {
                        ArrayList<PreferredActivity> existing = pir.findFilters(filter);
                        if (existing != null && existing.size() == 1) {
                            PreferredActivity cur = (PreferredActivity) existing.get(0);
                            if (cur.mPref.mAlways && cur.mPref.mComponent.equals(activity) && cur.mPref.mMatch == (268369920 & match) && cur.mPref.sameSet(set)) {
                                return;
                            }
                        }
                        if (existing != null) {
                            for (int i = 0; i < existing.size(); i++) {
                                pir.removeFilter((PreferredActivity) existing.get(i));
                            }
                        }
                    }
                }
            } else {
                Slog.i(TAG, "NOT ALLOWED TO replace preferred activity current time!");
                throw new IllegalArgumentException("Component " + activity + " not allowed to be home on user " + userId);
            }
        } else {
            throw new IllegalArgumentException("replacePreferredActivity expects filter to have no data authorities, paths, or types; and at most one scheme.");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void clearPackagePreferredActivities(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) == null) {
            synchronized (this.mPackages) {
                Package pkg = (Package) this.mPackages.get(packageName);
                if ((pkg == null || pkg.applicationInfo.uid != callingUid) && this.mContext.checkCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS") != 0) {
                    if (getUidTargetSdkVersionLockedLPr(callingUid) < 8) {
                        Slog.w(TAG, "Ignoring clearPackagePreferredActivities() from uid " + callingUid);
                        return;
                    }
                    this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
                }
                PackageSetting ps = this.mSettings.getPackageLPr(packageName);
                if (ps == null || !filterAppAccessLPr(ps, callingUid, UserHandle.getUserId(callingUid))) {
                    int user = UserHandle.getCallingUserId();
                    if (clearPackagePreferredActivitiesLPw(packageName, user)) {
                        scheduleWritePackageRestrictionsLocked(user);
                    }
                }
            }
        }
    }

    boolean clearPackagePreferredActivitiesLPw(String packageName, int userId) {
        Slog.d(TAG, "clear pref activity " + packageName + " from uid " + Binder.getCallingUid());
        ArrayList removed = null;
        boolean changed = false;
        for (int i = 0; i < this.mSettings.mPreferredActivities.size(); i++) {
            int thisUserId = this.mSettings.mPreferredActivities.keyAt(i);
            PreferredIntentResolver pir = (PreferredIntentResolver) this.mSettings.mPreferredActivities.valueAt(i);
            if (userId == -1 || userId == thisUserId) {
                Iterator<PreferredActivity> it = pir.filterIterator();
                while (it.hasNext()) {
                    PreferredActivity pa = (PreferredActivity) it.next();
                    if (packageName == null || (pa.mPref.mComponent.getPackageName().equals(packageName) && pa.mPref.mAlways)) {
                        if (!HwDeviceManager.disallowOp(17) || !pa.hasAction("android.intent.action.MAIN") || !pa.hasCategory("android.intent.category.HOME") || !pa.hasCategory("android.intent.category.DEFAULT")) {
                            if (removed == null) {
                                removed = new ArrayList();
                            }
                            removed.add(pa);
                        }
                    }
                }
                if (removed != null) {
                    for (int j = 0; j < removed.size(); j++) {
                        pir.removeFilter((PreferredActivity) removed.get(j));
                    }
                    changed = true;
                }
            }
        }
        if (changed) {
            postPreferredActivityChangedBroadcast(userId);
        }
        return changed;
    }

    private void clearIntentFilterVerificationsLPw(int userId) {
        int packageCount = this.mPackages.size();
        for (int i = 0; i < packageCount; i++) {
            clearIntentFilterVerificationsLPw(((Package) this.mPackages.valueAt(i)).packageName, userId);
        }
    }

    void clearIntentFilterVerificationsLPw(String packageName, int userId) {
        if (userId == -1) {
            if (this.mSettings.removeIntentFilterVerificationLPw(packageName, sUserManager.getUserIds())) {
                for (int oneUserId : sUserManager.getUserIds()) {
                    scheduleWritePackageRestrictionsLocked(oneUserId);
                }
            }
        } else if (this.mSettings.removeIntentFilterVerificationLPw(packageName, userId)) {
            scheduleWritePackageRestrictionsLocked(userId);
        }
    }

    void clearDefaultBrowserIfNeeded(String packageName) {
        for (int oneUserId : sUserManager.getUserIds()) {
            clearDefaultBrowserIfNeededForUser(packageName, oneUserId);
        }
    }

    private void clearDefaultBrowserIfNeededForUser(String packageName, int userId) {
        String defaultBrowserPackageName = getDefaultBrowserPackageName(userId);
        if (!TextUtils.isEmpty(defaultBrowserPackageName) && packageName.equals(defaultBrowserPackageName)) {
            setDefaultBrowserPackageName(null, userId);
        }
    }

    public void resetApplicationPreferences(int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.SET_PREFERRED_APPLICATIONS", null);
        long identity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mPackages) {
                clearPackagePreferredActivitiesLPw(null, userId);
                this.mSettings.applyDefaultPreferredAppsLPw(this, userId);
                applyFactoryDefaultBrowserLPw(userId);
                clearIntentFilterVerificationsLPw(userId);
                primeDomainVerificationsLPw(userId);
                resetUserChangesToRuntimePermissionsAndFlagsLPw(userId);
                scheduleWritePackageRestrictionsLocked(userId);
            }
            resetNetworkPolicies(userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return 0;
        }
        int userId = UserHandle.getCallingUserId();
        synchronized (this.mPackages) {
            PreferredIntentResolver pir = (PreferredIntentResolver) this.mSettings.mPreferredActivities.get(userId);
            if (pir != null) {
                Iterator<PreferredActivity> it = pir.filterIterator();
                while (it.hasNext()) {
                    PreferredActivity pa = (PreferredActivity) it.next();
                    if (packageName == null || (pa.mPref.mComponent.getPackageName().equals(packageName) && pa.mPref.mAlways)) {
                        if (outFilters != null) {
                            outFilters.add(new IntentFilter(pa));
                        }
                        if (outActivities != null) {
                            outActivities.add(pa.mPref.mComponent);
                        }
                    }
                }
            }
        }
        return 0;
    }

    public void addPersistentPreferredActivity(IntentFilter filter, ComponentName activity, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("addPersistentPreferredActivity can only be run by the system");
        } else if (filter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a preferred activity with no filter actions");
        } else if (!filter.hasCategory("android.intent.category.HOME") || activity == null || (this.mHwPMSEx.isAllowedSetHomeActivityForAntiMal(getPackageInfo(activity.getPackageName(), 0, userId), userId) ^ 1) == 0) {
            synchronized (this.mPackages) {
                Slog.i(TAG, "Adding persistent preferred activity " + activity + " for user " + userId + ":");
                filter.dump(new LogPrinter(4, TAG), "  ");
                this.mSettings.editPersistentPreferredActivitiesLPw(userId).addFilter(new PersistentPreferredActivity(filter, activity));
                scheduleWritePackageRestrictionsLocked(userId);
                postPreferredActivityChangedBroadcast(userId);
            }
        } else {
            Slog.i(TAG, "NOT ALLOWED TO add persistent preferred activity current time!");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void clearPackagePersistentPreferredActivities(String packageName, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("clearPackagePersistentPreferredActivities can only be run by the system");
        }
        ArrayList<PersistentPreferredActivity> removed = null;
        boolean changed = false;
        synchronized (this.mPackages) {
            int i = 0;
            while (i < this.mSettings.mPersistentPreferredActivities.size()) {
                try {
                    PersistentPreferredIntentResolver ppir = (PersistentPreferredIntentResolver) this.mSettings.mPersistentPreferredActivities.valueAt(i);
                    if (userId == this.mSettings.mPersistentPreferredActivities.keyAt(i)) {
                        Iterator<PersistentPreferredActivity> it = ppir.filterIterator();
                        ArrayList<PersistentPreferredActivity> removed2 = removed;
                        while (it.hasNext()) {
                            PersistentPreferredActivity ppa = (PersistentPreferredActivity) it.next();
                            if (ppa.mComponent.getPackageName().equals(packageName)) {
                                if (removed2 == null) {
                                    removed = new ArrayList();
                                } else {
                                    removed = removed2;
                                }
                                removed.add(ppa);
                            } else {
                                removed = removed2;
                            }
                            removed2 = removed;
                        }
                        if (removed2 != null) {
                            int j = 0;
                            while (j < removed2.size()) {
                                try {
                                    ppir.removeFilter((PersistentPreferredActivity) removed2.get(j));
                                    j++;
                                } catch (Throwable th) {
                                    Throwable th2 = th;
                                    removed = removed2;
                                }
                            }
                            changed = true;
                            removed = removed2;
                        } else {
                            removed = removed2;
                        }
                    }
                    i++;
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (changed) {
                scheduleWritePackageRestrictionsLocked(userId);
                postPreferredActivityChangedBroadcast(userId);
            }
        }
        throw th2;
    }

    private void restoreFromXml(XmlPullParser parser, int userId, String expectedStartTag, BlobXmlRestorer functor) throws IOException, XmlPullParserException {
        int type;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type == 2) {
            Slog.v(TAG, ":: restoreFromXml() : got to tag " + parser.getName());
            if (expectedStartTag.equals(parser.getName())) {
                do {
                } while (parser.next() == 4);
                Slog.v(TAG, ":: stepped forward, applying functor at tag " + parser.getName());
                functor.apply(parser, userId);
            }
        }
    }

    public byte[] getPreferredActivityBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getPreferredActivityBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.startTag(null, TAG_PREFERRED_BACKUP);
            synchronized (this.mPackages) {
                this.mSettings.writePreferredActivitiesLPr(serializer, userId, true);
            }
            serializer.endTag(null, TAG_PREFERRED_BACKUP);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public void restorePreferredActivities(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePreferredActivities()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_PREFERRED_BACKUP, new BlobXmlRestorer() {
                public void apply(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readPreferredActivitiesLPw(parser, userId);
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    public byte[] getDefaultAppsBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getDefaultAppsBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.startTag(null, TAG_DEFAULT_APPS);
            synchronized (this.mPackages) {
                this.mSettings.writeDefaultAppsLPr(serializer, userId);
            }
            serializer.endTag(null, TAG_DEFAULT_APPS);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public void restoreDefaultApps(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restoreDefaultApps()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_DEFAULT_APPS, new BlobXmlRestorer() {
                public void apply(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readDefaultAppsLPw(parser, userId);
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    public byte[] getIntentFilterVerificationBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getIntentFilterVerificationBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.startTag(null, TAG_INTENT_FILTER_VERIFICATION);
            synchronized (this.mPackages) {
                this.mSettings.writeAllDomainVerificationsLPr(serializer, userId);
            }
            serializer.endTag(null, TAG_INTENT_FILTER_VERIFICATION);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public void restoreIntentFilterVerification(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePreferredActivities()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_INTENT_FILTER_VERIFICATION, new BlobXmlRestorer() {
                public void apply(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.mSettings.readAllDomainVerificationsLPr(parser, userId);
                        PackageManagerService.this.mSettings.writeLPr();
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    public byte[] getPermissionGrantBackup(int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call getPermissionGrantBackup()");
        }
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try {
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(dataStream, StandardCharsets.UTF_8.name());
            serializer.startDocument(null, Boolean.valueOf(true));
            serializer.startTag(null, TAG_PERMISSION_BACKUP);
            synchronized (this.mPackages) {
                serializeRuntimePermissionGrantsLPr(serializer, userId);
            }
            serializer.endTag(null, TAG_PERMISSION_BACKUP);
            serializer.endDocument();
            serializer.flush();
            return dataStream.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public void restorePermissionGrants(byte[] backup, int userId) {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("Only the system may call restorePermissionGrants()");
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(backup), StandardCharsets.UTF_8.name());
            restoreFromXml(parser, userId, TAG_PERMISSION_BACKUP, new BlobXmlRestorer() {
                public void apply(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
                    synchronized (PackageManagerService.this.mPackages) {
                        PackageManagerService.this.processRestoredPermissionGrantsLPr(parser, userId);
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    private void serializeRuntimePermissionGrantsLPr(XmlSerializer serializer, int userId) throws IOException {
        serializer.startTag(null, TAG_ALL_GRANTS);
        int N = this.mSettings.mPackages.size();
        for (int i = 0; i < N; i++) {
            boolean pkgGrantsKnown = false;
            for (PermissionState state : ((PackageSetting) this.mSettings.mPackages.valueAt(i)).getPermissionsState().getRuntimePermissionStates(userId)) {
                int grantFlags = state.getFlags();
                if ((grantFlags & 52) == 0) {
                    boolean isGranted = state.isGranted();
                    if (isGranted || (grantFlags & 11) != 0) {
                        String packageName = (String) this.mSettings.mPackages.keyAt(i);
                        if (!pkgGrantsKnown) {
                            serializer.startTag(null, TAG_GRANT);
                            serializer.attribute(null, "pkg", packageName);
                            pkgGrantsKnown = true;
                        }
                        boolean userSet = (grantFlags & 1) != 0;
                        boolean userFixed = (grantFlags & 2) != 0;
                        boolean revoke = (grantFlags & 8) != 0;
                        serializer.startTag(null, TAG_PERMISSION);
                        serializer.attribute(null, ATTR_PERMISSION_NAME, state.getName());
                        if (isGranted) {
                            serializer.attribute(null, ATTR_IS_GRANTED, "true");
                        }
                        if (userSet) {
                            serializer.attribute(null, ATTR_USER_SET, "true");
                        }
                        if (userFixed) {
                            serializer.attribute(null, ATTR_USER_FIXED, "true");
                        }
                        if (revoke) {
                            serializer.attribute(null, ATTR_REVOKE_ON_UPGRADE, "true");
                        }
                        serializer.endTag(null, TAG_PERMISSION);
                    }
                }
            }
            if (pkgGrantsKnown) {
                serializer.endTag(null, TAG_GRANT);
            }
        }
        serializer.endTag(null, TAG_ALL_GRANTS);
    }

    private void processRestoredPermissionGrantsLPr(XmlPullParser parser, int userId) throws XmlPullParserException, IOException {
        String pkgName = null;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                scheduleWriteSettingsLocked();
                this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
            } else if (!(type == 3 || type == 4)) {
                String tagName = parser.getName();
                if (tagName.equals(TAG_GRANT)) {
                    pkgName = parser.getAttributeValue(null, "pkg");
                } else if (tagName.equals(TAG_PERMISSION)) {
                    boolean isGranted = "true".equals(parser.getAttributeValue(null, ATTR_IS_GRANTED));
                    String permName = parser.getAttributeValue(null, ATTR_PERMISSION_NAME);
                    int newFlagSet = 0;
                    if ("true".equals(parser.getAttributeValue(null, ATTR_USER_SET))) {
                        newFlagSet = 1;
                    }
                    if ("true".equals(parser.getAttributeValue(null, ATTR_USER_FIXED))) {
                        newFlagSet |= 2;
                    }
                    if ("true".equals(parser.getAttributeValue(null, ATTR_REVOKE_ON_UPGRADE))) {
                        newFlagSet |= 8;
                    }
                    PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
                    if (ps != null) {
                        PermissionsState perms = ps.getPermissionsState();
                        BasePermission bp = (BasePermission) this.mSettings.mPermissions.get(permName);
                        if (bp != null) {
                            if (isGranted) {
                                perms.grantRuntimePermission(bp, userId);
                            }
                            if (newFlagSet != 0) {
                                perms.updatePermissionFlags(bp, userId, 11, newFlagSet);
                            }
                        }
                    } else {
                        this.mSettings.processRestoredPermissionGrantLPr(pkgName, permName, isGranted, newFlagSet, userId);
                    }
                } else {
                    reportSettingsProblem(5, "Unknown element under <perm-grant-backup>: " + tagName);
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        scheduleWriteSettingsLocked();
        this.mSettings.writeRuntimePermissionsForUserLPr(userId, false);
    }

    public void addCrossProfileIntentFilter(IntentFilter intentFilter, String ownerPackage, int sourceUserId, int targetUserId, int flags) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(ownerPackage, callingUid);
        enforceShellRestriction("no_debugging_features", callingUid, sourceUserId);
        if (intentFilter.countActions() == 0) {
            Slog.w(TAG, "Cannot set a crossProfile intent filter with no filter actions");
            return;
        }
        synchronized (this.mPackages) {
            CrossProfileIntentFilter newFilter = new CrossProfileIntentFilter(intentFilter, ownerPackage, targetUserId, flags);
            CrossProfileIntentResolver resolver = this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
            ArrayList<CrossProfileIntentFilter> existing = resolver.findFilters(intentFilter);
            if (existing != null) {
                int size = existing.size();
                for (int i = 0; i < size; i++) {
                    if (newFilter.equalsIgnoreFilter((CrossProfileIntentFilter) existing.get(i))) {
                        return;
                    }
                }
            }
            resolver.addFilter(newFilter);
            scheduleWritePackageRestrictionsLocked(sourceUserId);
        }
    }

    public void clearCrossProfileIntentFilters(int sourceUserId, String ownerPackage) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", null);
        int callingUid = Binder.getCallingUid();
        enforceOwnerRights(ownerPackage, callingUid);
        enforceShellRestriction("no_debugging_features", callingUid, sourceUserId);
        synchronized (this.mPackages) {
            CrossProfileIntentResolver resolver = this.mSettings.editCrossProfileIntentResolverLPw(sourceUserId);
            for (CrossProfileIntentFilter filter : new ArraySet(resolver.filterSet())) {
                if (filter.getOwnerPackage().equals(ownerPackage)) {
                    resolver.removeFilter(filter);
                }
            }
            scheduleWritePackageRestrictionsLocked(sourceUserId);
        }
    }

    private void enforceOwnerRights(String pkg, int callingUid) {
        if (UserHandle.getAppId(callingUid) != 1000) {
            int callingUserId = UserHandle.getUserId(callingUid);
            PackageInfo pi = getPackageInfo(pkg, 0, callingUserId);
            if (pi == null) {
                throw new IllegalArgumentException("Unknown package " + pkg + " on user " + callingUserId);
            } else if (!UserHandle.isSameApp(pi.applicationInfo.uid, callingUid)) {
                throw new SecurityException("Calling uid " + callingUid + " does not own package " + pkg);
            }
        }
    }

    public ComponentName getHomeActivities(List<ResolveInfo> allHomeCandidates) {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return getHomeActivitiesAsUser(allHomeCandidates, UserHandle.getCallingUserId());
    }

    public void sendSessionCommitBroadcast(SessionInfo sessionInfo, int userId) {
        UserManagerService ums = UserManagerService.getInstance();
        if (ums != null) {
            UserInfo parent = ums.getProfileParent(userId);
            int launcherUid = parent != null ? parent.id : userId;
            ComponentName launcherComponent = getDefaultHomeActivity(launcherUid);
            if (launcherComponent != null) {
                this.mContext.sendBroadcastAsUser(new Intent("android.content.pm.action.SESSION_COMMITTED").putExtra("android.content.pm.extra.SESSION", sessionInfo).putExtra("android.intent.extra.USER", UserHandle.of(userId)).setPackage(launcherComponent.getPackageName()), UserHandle.of(launcherUid));
            }
        }
    }

    private ComponentName getDefaultHomeActivity(int userId) {
        List<ResolveInfo> allHomeCandidates = new ArrayList();
        ComponentName cn = getHomeActivitiesAsUser(allHomeCandidates, userId);
        if (cn != null) {
            return cn;
        }
        int lastPriority = Integer.MIN_VALUE;
        ComponentName lastComponent = null;
        int size = allHomeCandidates.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo ri = (ResolveInfo) allHomeCandidates.get(i);
            if (ri.priority > lastPriority) {
                lastComponent = ri.activityInfo.getComponentName();
                lastPriority = ri.priority;
            } else if (ri.priority == lastPriority) {
                lastComponent = null;
            }
        }
        return lastComponent;
    }

    private Intent getHomeIntent() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        intent.addCategory("android.intent.category.DEFAULT");
        return intent;
    }

    private IntentFilter getHomeFilter() {
        IntentFilter filter = new IntentFilter("android.intent.action.MAIN");
        filter.addCategory("android.intent.category.HOME");
        filter.addCategory("android.intent.category.DEFAULT");
        return filter;
    }

    ComponentName getHomeActivitiesAsUser(List<ResolveInfo> allHomeCandidates, int userId) {
        Intent intent = getHomeIntent();
        List<ResolveInfo> list = queryIntentActivitiesInternal(intent, null, 128, userId);
        ResolveInfo preferred = findPreferredActivity(intent, null, 0, list, 0, true, false, false, userId);
        allHomeCandidates.clear();
        if (list != null) {
            for (ResolveInfo ri : list) {
                allHomeCandidates.add(ri);
            }
        }
        if (preferred == null || preferred.activityInfo == null) {
            return null;
        }
        return new ComponentName(preferred.activityInfo.packageName, preferred.activityInfo.name);
    }

    public void setHomeActivity(ComponentName comp, int userId) {
        if (getInstantAppPackageName(Binder.getCallingUid()) == null) {
            ArrayList<ResolveInfo> homeActivities = new ArrayList();
            getHomeActivitiesAsUser(homeActivities, userId);
            boolean found = false;
            int size = homeActivities.size();
            ComponentName[] set = new ComponentName[size];
            for (int i = 0; i < size; i++) {
                ActivityInfo info = ((ResolveInfo) homeActivities.get(i)).activityInfo;
                ComponentName activityName = new ComponentName(info.packageName, info.name);
                set[i] = activityName;
                if (!found && activityName.equals(comp)) {
                    found = true;
                }
            }
            if (found) {
                replacePreferredActivity(getHomeFilter(), 1048576, set, comp, userId);
                return;
            }
            throw new IllegalArgumentException("Component " + comp + " cannot be home on user " + userId);
        }
    }

    private String getSetupWizardPackageName() {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.SETUP_WIZARD");
        List<ResolveInfo> matches = queryIntentActivitiesInternal(intent, null, 1835520, UserHandle.myUserId());
        if (matches.size() == 1) {
            return ((ResolveInfo) matches.get(0)).getComponentInfo().packageName;
        }
        Slog.e(TAG, "There should probably be exactly one setup wizard; found " + matches.size() + ": matches=" + matches);
        return null;
    }

    private String getStorageManagerPackageName() {
        List<ResolveInfo> matches = queryIntentActivitiesInternal(new Intent("android.os.storage.action.MANAGE_STORAGE"), null, 1835520, UserHandle.myUserId());
        if (matches.size() == 1) {
            return ((ResolveInfo) matches.get(0)).getComponentInfo().packageName;
        }
        Slog.e(TAG, "There should probably be exactly one storage manager; found " + matches.size() + ": matches=" + matches);
        return null;
    }

    public void setApplicationEnabledSetting(String appPackageName, int newState, int flags, int userId, String callingPackage) {
        if (sUserManager.exists(userId)) {
            if (callingPackage == null) {
                callingPackage = Integer.toString(Binder.getCallingUid());
            }
            setEnabledSetting(appPackageName, null, newState, flags, userId, callingPackage);
        }
    }

    public void setUpdateAvailable(String packageName, boolean updateAvailable) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.INSTALL_PACKAGES", null);
        synchronized (this.mPackages) {
            PackageSetting pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (pkgSetting != null) {
                pkgSetting.setUpdateAvailable(updateAvailable);
            }
        }
    }

    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags, int userId) {
        if (sUserManager.exists(userId)) {
            setEnabledSetting(componentName.getPackageName(), componentName.getClassName(), newState, flags, userId, null);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setEnabledSetting(String packageName, String className, int newState, int flags, int userId, String callingPackage) {
        String componentName;
        PackageSetting pkgSetting;
        long callingId;
        ArrayList<String> components;
        Throwable th;
        PackageFreezer packageFreezer;
        boolean sendNow;
        boolean newPackage;
        if (newState == 0 || newState == 1 || newState == 2 || newState == 3 || newState == 4) {
            int permission;
            String str;
            Flog.i(206, "setEnabledSetting pkg:" + packageName + ", className:" + className + ", newState:" + newState + ", flags:" + flags + ", userId:" + userId + ", CallingPid:" + Binder.getCallingPid() + ", CallingUid:" + Binder.getCallingUid());
            int callingUid = Binder.getCallingUid();
            if (callingUid == 1000) {
                permission = 0;
            } else {
                permission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
            }
            enforceCrossUserPermission(callingUid, userId, false, true, "set enabled");
            boolean allowedByPermission = permission == 0;
            sendNow = false;
            boolean isApp = className == null;
            boolean isCallerInstantApp = getInstantAppPackageName(callingUid) != null;
            componentName = isApp ? packageName : className;
            synchronized (this.mPackages) {
                pkgSetting = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (pkgSetting != null) {
                } else if (isCallerInstantApp) {
                    StringBuilder append = new StringBuilder().append("Attempt to change component state; pid=").append(Binder.getCallingPid()).append(", uid=").append(callingUid);
                    if (className == null) {
                        str = ", package=" + packageName;
                    } else {
                        str = ", component=" + packageName + "/" + className;
                    }
                    throw new SecurityException(append.append(str).toString());
                } else if (className == null) {
                    throw new IllegalArgumentException("Unknown package: " + packageName);
                } else {
                    throw new IllegalArgumentException("Unknown component: " + packageName + "/" + className);
                }
            }
            if (!UserHandle.isSameApp(callingUid, pkgSetting.appId)) {
                if (!allowedByPermission || filterAppAccessLPr(pkgSetting, callingUid, userId)) {
                    StringBuilder append2 = new StringBuilder().append("Attempt to change component state; pid=").append(Binder.getCallingPid()).append(", uid=").append(callingUid);
                    if (className == null) {
                        str = ", package=" + packageName;
                    } else {
                        str = ", component=" + packageName + "/" + className;
                    }
                    throw new SecurityException(append2.append(str).toString());
                } else if (this.mProtectedPackages.isPackageStateProtected(userId, packageName)) {
                    throw new SecurityException("Cannot disable a protected package: " + packageName);
                }
            }
            synchronized (this.mPackages) {
                if (callingUid == 2000) {
                    if ((pkgSetting.pkgFlags & 256) == 0) {
                        int oldState = pkgSetting.getEnabled(userId);
                        if (className == null && (oldState == 3 || oldState == 0 || oldState == 1)) {
                            if (!(newState == 3 || newState == 0)) {
                                if (newState == 1) {
                                }
                            }
                        }
                        throw new SecurityException("Shell cannot change component state for " + packageName + "/" + className + " to " + newState);
                    }
                }
            }
            if (className == null) {
                synchronized (this.mPackages) {
                    if (pkgSetting.getEnabled(userId) == newState) {
                        return;
                    }
                }
            }
            synchronized (this.mPackages) {
                Package pkg = pkgSetting.pkg;
                if (pkg == null || (pkg.hasComponentClassName(className) ^ 1) != 0) {
                    if (pkg == null || pkg.applicationInfo.targetSdkVersion < 16) {
                        Slog.w(TAG, "Failed setComponentEnabledSetting: component class " + className + " does not exist in " + packageName);
                    } else {
                        throw new IllegalArgumentException("Component class " + className + " does not exist in " + packageName);
                    }
                }
                switch (newState) {
                    case 0:
                        if (!pkgSetting.restoreComponentLPw(className, userId)) {
                            return;
                        }
                        break;
                    case 1:
                        if (!pkgSetting.enableComponentLPw(className, userId)) {
                            return;
                        }
                        break;
                    case 2:
                        if (!pkgSetting.disableComponentLPw(className, userId)) {
                            return;
                        }
                        break;
                    default:
                        Slog.e(TAG, "Invalid new component state: " + newState);
                        return;
                }
            }
            synchronized (this.mPackages) {
                scheduleWritePackageRestrictionsLocked(userId);
                updateSequenceNumberLP(pkgSetting, new int[]{userId});
                callingId = Binder.clearCallingIdentity();
                try {
                    updateInstantAppInstallerLocked(packageName);
                    Binder.restoreCallingIdentity(callingId);
                    components = this.mPendingBroadcasts.get(userId, packageName);
                    newPackage = components == null;
                    if (newPackage) {
                        components = new ArrayList();
                    }
                    if (!components.contains(componentName)) {
                        components.add(componentName);
                    }
                    if ((flags & 1) == 0) {
                        sendNow = true;
                        this.mPendingBroadcasts.remove(userId, packageName);
                    } else {
                        if (newPackage) {
                            this.mPendingBroadcasts.put(userId, packageName, components);
                        }
                        if (!this.mHandler.hasMessages(1)) {
                            this.mHandler.sendEmptyMessageDelayed(1, 10000);
                        }
                    }
                } catch (Throwable th2) {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
            callingId = Binder.clearCallingIdentity();
            if (sendNow) {
                try {
                    sendPackageChangedBroadcast(packageName, (flags & 1) != 0, components, UserHandle.getUid(userId, pkgSetting.appId));
                } catch (Throwable th3) {
                    Binder.restoreCallingIdentity(callingId);
                }
            }
            Binder.restoreCallingIdentity(callingId);
            return;
        }
        throw new IllegalArgumentException("Invalid new component state: " + newState);
        Throwable th4;
        if (th != null) {
            throw th;
        } else {
            synchronized (this.mPackages) {
                this.mSettings.disableSystemPackageLPw(deletedPkg.packageName, true);
                this.mSettings.writeLPr();
                return;
            }
        }
        if (th4 == null) {
            try {
                throw th4;
            } catch (Throwable e) {
                Slog.w(TAG, "Failed to install compressed system package:" + pkgSetting.name, e);
                removeCodePathLI(codePath);
                th = null;
                packageFreezer = null;
                try {
                    packageFreezer = freezePackage(deletedPkg.packageName, "setEnabledSetting");
                    synchronized (this.mPackages) {
                        enableSystemPackageLPw(deletedPkg);
                        installPackageFromSystemLIF(new File(deletedPkg.codePath), false, null, null, null, true);
                        if (packageFreezer != null) {
                            packageFreezer.close();
                        }
                    }
                } catch (Throwable th5) {
                    Throwable th6 = th5;
                    if (packageFreezer != null) {
                        try {
                            packageFreezer.close();
                        } catch (Throwable th7) {
                            if (th == null) {
                                th = th7;
                            } else if (th != th7) {
                                th.addSuppressed(th7);
                            }
                        }
                    }
                    if (th != null) {
                        throw th;
                    } else {
                        throw th6;
                    }
                }
            } catch (Throwable pme) {
                try {
                    Slog.w(TAG, "Failed to restore system package:" + deletedPkg.packageName, pme);
                    synchronized (this.mPackages) {
                        this.mSettings.disableSystemPackageLPw(deletedPkg.packageName, true);
                        this.mSettings.writeLPr();
                        return;
                    }
                } catch (Throwable th8) {
                    synchronized (this.mPackages) {
                        this.mSettings.disableSystemPackageLPw(deletedPkg.packageName, true);
                        this.mSettings.writeLPr();
                    }
                }
            } catch (Throwable th9) {
                th = th9;
            }
        } else {
            clearAppDataLIF(newPkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
            clearAppProfilesLIF(newPkg, -1);
            this.mDexManager.notifyPackageUpdated(newPkg.packageName, newPkg.baseCodePath, newPkg.splitCodePaths);
            if (newState == 0 || newState == 1) {
                callingPackage = null;
            }
            synchronized (this.mPackages) {
                pkgSetting.setEnabled(newState, userId, callingPackage);
            }
            synchronized (this.mPackages) {
                scheduleWritePackageRestrictionsLocked(userId);
                updateSequenceNumberLP(pkgSetting, new int[]{userId});
                callingId = Binder.clearCallingIdentity();
                updateInstantAppInstallerLocked(packageName);
                Binder.restoreCallingIdentity(callingId);
                components = this.mPendingBroadcasts.get(userId, packageName);
                if (components == null) {
                }
                if (newPackage) {
                    components = new ArrayList();
                }
                if (components.contains(componentName)) {
                    components.add(componentName);
                }
                if ((flags & 1) == 0) {
                    if (newPackage) {
                        this.mPendingBroadcasts.put(userId, packageName, components);
                    }
                    if (this.mHandler.hasMessages(1)) {
                        this.mHandler.sendEmptyMessageDelayed(1, 10000);
                    }
                } else {
                    sendNow = true;
                    this.mPendingBroadcasts.remove(userId, packageName);
                }
            }
            callingId = Binder.clearCallingIdentity();
            if (sendNow) {
                if ((flags & 1) != 0) {
                }
                sendPackageChangedBroadcast(packageName, (flags & 1) != 0, components, UserHandle.getUid(userId, pkgSetting.appId));
            }
            Binder.restoreCallingIdentity(callingId);
            return;
        }
        updatePermissionsLPw(newPkg.packageName, newPkg, 3);
        this.mSettings.writeLPr();
        if (packageFreezer != null) {
            try {
                packageFreezer.close();
            } catch (Throwable th10) {
                th4 = th10;
            }
        }
        if (th4 == null) {
            clearAppDataLIF(newPkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
            clearAppProfilesLIF(newPkg, -1);
            this.mDexManager.notifyPackageUpdated(newPkg.packageName, newPkg.baseCodePath, newPkg.splitCodePaths);
            callingPackage = null;
            synchronized (this.mPackages) {
                pkgSetting.setEnabled(newState, userId, callingPackage);
            }
            synchronized (this.mPackages) {
                scheduleWritePackageRestrictionsLocked(userId);
                updateSequenceNumberLP(pkgSetting, new int[]{userId});
                callingId = Binder.clearCallingIdentity();
                updateInstantAppInstallerLocked(packageName);
                Binder.restoreCallingIdentity(callingId);
                components = this.mPendingBroadcasts.get(userId, packageName);
                if (components == null) {
                }
                if (newPackage) {
                    components = new ArrayList();
                }
                if (components.contains(componentName)) {
                    components.add(componentName);
                }
                if ((flags & 1) == 0) {
                    sendNow = true;
                    this.mPendingBroadcasts.remove(userId, packageName);
                } else {
                    if (newPackage) {
                        this.mPendingBroadcasts.put(userId, packageName, components);
                    }
                    if (this.mHandler.hasMessages(1)) {
                        this.mHandler.sendEmptyMessageDelayed(1, 10000);
                    }
                }
            }
            callingId = Binder.clearCallingIdentity();
            if (sendNow) {
                if ((flags & 1) != 0) {
                }
                sendPackageChangedBroadcast(packageName, (flags & 1) != 0, components, UserHandle.getUid(userId, pkgSetting.appId));
            }
            Binder.restoreCallingIdentity(callingId);
            return;
        }
        throw th4;
    }

    public void flushPackageRestrictionsAsUser(int userId) {
        if (getInstantAppPackageName(Binder.getCallingUid()) == null && sUserManager.exists(userId)) {
            enforceCrossUserPermission(Binder.getCallingUid(), userId, false, false, "flushPackageRestrictions");
            synchronized (this.mPackages) {
                this.mSettings.writePackageRestrictionsLPr(userId);
                this.mDirtyUsers.remove(Integer.valueOf(userId));
                if (this.mDirtyUsers.isEmpty()) {
                    this.mHandler.removeMessages(14);
                }
            }
        }
    }

    protected void sendPackageChangedBroadcast(String packageName, boolean killFlag, ArrayList<String> componentNames, int packageUid) {
        Bundle extras = new Bundle(4);
        extras.putString("android.intent.extra.changed_component_name", (String) componentNames.get(0));
        String[] nameList = new String[componentNames.size()];
        componentNames.toArray(nameList);
        extras.putStringArray("android.intent.extra.changed_component_name_list", nameList);
        extras.putBoolean("android.intent.extra.DONT_KILL_APP", killFlag);
        extras.putInt("android.intent.extra.UID", packageUid);
        sendPackageBroadcast("android.intent.action.PACKAGE_CHANGED", packageName, extras, !componentNames.contains(packageName) ? 1073741824 : 0, null, null, new int[]{UserHandle.getUserId(packageUid)});
    }

    public void setPackageStoppedState(String packageName, boolean stopped, int userId) {
        if (sUserManager.exists(userId)) {
            int callingUid = Binder.getCallingUid();
            if (getInstantAppPackageName(callingUid) == null) {
                boolean allowedByPermission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE") == 0;
                enforceCrossUserPermission(callingUid, userId, true, true, "stop package");
                synchronized (this.mPackages) {
                    if (!filterAppAccessLPr((PackageSetting) this.mSettings.mPackages.get(packageName), callingUid, userId) && this.mSettings.setPackageStoppedStateLPw(this, packageName, stopped, allowedByPermission, callingUid, userId)) {
                        scheduleWritePackageRestrictionsLocked(userId);
                    }
                }
            }
        }
    }

    public String getInstallerPackageName(String packageName) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null) {
            return null;
        }
        synchronized (this.mPackages) {
            if (filterAppAccessLPr((PackageSetting) this.mSettings.mPackages.get(packageName), callingUid, UserHandle.getUserId(callingUid))) {
                return null;
            }
            String installerPackageNameLPr = this.mSettings.getInstallerPackageNameLPr(packageName);
            return installerPackageNameLPr;
        }
    }

    public boolean isOrphaned(String packageName) {
        boolean isOrphaned;
        synchronized (this.mPackages) {
            isOrphaned = this.mSettings.isOrphaned(packageName);
        }
        return isOrphaned;
    }

    public int getApplicationEnabledSetting(String packageName, int userId) {
        if (!sUserManager.exists(userId)) {
            return 2;
        }
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, false, false, "get enabled");
        synchronized (this.mPackages) {
            if (filterAppAccessLPr(this.mSettings.getPackageLPr(packageName), callingUid, userId)) {
                return 2;
            }
            int applicationEnabledSettingLPr = this.mSettings.getApplicationEnabledSettingLPr(packageName, userId);
            return applicationEnabledSettingLPr;
        }
    }

    public int getComponentEnabledSetting(ComponentName component, int userId) {
        if (!sUserManager.exists(userId)) {
            return 2;
        }
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, false, false, "getComponentEnabled");
        synchronized (this.mPackages) {
            if (filterAppAccessLPr(this.mSettings.getPackageLPr(component.getPackageName()), callingUid, component, 0, userId)) {
                return 2;
            }
            int componentEnabledSettingLPr = this.mSettings.getComponentEnabledSettingLPr(component, userId);
            return componentEnabledSettingLPr;
        }
    }

    public void enterSafeMode() {
        enforceSystemOrRoot("Only the system can request entering safe mode");
        if (!this.mSystemReady) {
            this.mSafeMode = true;
        }
    }

    public void systemReady() {
        enforceSystemOrRoot("Only the system can claim the system is ready");
        this.mSystemReady = true;
        final ContentResolver contentResolver = this.mContext.getContentResolver();
        ContentObserver co = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                boolean z = true;
                PackageManagerService packageManagerService = PackageManagerService.this;
                if (!(Global.getInt(contentResolver, "enable_ephemeral_feature", 1) == 0 || Secure.getInt(contentResolver, "instant_apps_enabled", 1) == 0)) {
                    z = false;
                }
                packageManagerService.mEphemeralAppsDisabled = z;
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("enable_ephemeral_feature"), false, co, 0);
        this.mContext.getContentResolver().registerContentObserver(Global.getUriFor("instant_apps_enabled"), false, co, 0);
        co.onChange(true);
        CarrierAppUtils.disableCarrierAppsUntilPrivileged(this.mContext.getOpPackageName(), this, this.mContext.getContentResolver(), 0);
        PackageParser.setCompatibilityModeEnabled(Global.getInt(this.mContext.getContentResolver(), "compatibility_mode", 1) == 1);
        int[] grantPermissionsUserIds = EMPTY_INT_ARRAY;
        synchronized (this.mPackages) {
            ArrayList<PreferredActivity> removed = new ArrayList();
            for (int i = 0; i < this.mSettings.mPreferredActivities.size(); i++) {
                PreferredIntentResolver pir = (PreferredIntentResolver) this.mSettings.mPreferredActivities.valueAt(i);
                removed.clear();
                for (PreferredActivity pa : pir.filterSet()) {
                    PreferredActivity pa2;
                    if (this.mActivities.mActivities.get(pa2.mPref.mComponent) == null) {
                        removed.add(pa2);
                    }
                }
                if (removed.size() > 0) {
                    for (int r = 0; r < removed.size(); r++) {
                        pa2 = (PreferredActivity) removed.get(r);
                        Slog.w(TAG, "Removing dangling preferred activity: " + pa2.mPref.mComponent);
                        pir.removeFilter(pa2);
                    }
                    this.mSettings.writePackageRestrictionsLPr(this.mSettings.mPreferredActivities.keyAt(i));
                }
            }
            for (int userId : UserManagerService.getInstance().getUserIds()) {
                Slog.i(TAG, "TO DO, pms system ready grant default permission for user " + userId);
                if (!this.mSettings.areDefaultRuntimePermissionsGrantedLPr(userId)) {
                    Slog.i(TAG, "add user " + userId);
                    grantPermissionsUserIds = ArrayUtils.appendInt(grantPermissionsUserIds, userId);
                }
            }
        }
        sUserManager.systemReady();
        HwThemeManager.applyDefaultHwTheme(true, this.mContext, 0);
        for (int userId2 : grantPermissionsUserIds) {
            this.mDefaultPermissionPolicy.grantDefaultPermissions(userId2);
        }
        if (grantPermissionsUserIds == EMPTY_INT_ARRAY) {
            this.mDefaultPermissionPolicy.scheduleReadDefaultPermissionExceptions();
        }
        for (int userId22 : UserManagerService.getInstance().getUserIds()) {
            this.mDefaultPermissionPolicy.grantCustDefaultPermissions(userId22);
        }
        if (this.mPostSystemReadyMessages != null) {
            for (Message msg : this.mPostSystemReadyMessages) {
                msg.sendToTarget();
            }
            this.mPostSystemReadyMessages = null;
        }
        ((StorageManager) this.mContext.getSystemService(StorageManager.class)).registerListener(this.mStorageListener);
        this.mInstallerService.systemReady();
        this.mPackageDexOptimizer.systemReady();
        ((StorageManagerInternal) LocalServices.getService(StorageManagerInternal.class)).addExternalStoragePolicy(new ExternalStorageMountPolicy() {
            public int getMountMode(int uid, String packageName) {
                if (Process.isIsolated(uid)) {
                    return 0;
                }
                if (PackageManagerService.this.checkUidPermission("android.permission.WRITE_MEDIA_STORAGE", uid) == 0 || PackageManagerService.this.checkUidPermission("android.permission.READ_EXTERNAL_STORAGE", uid) == -1) {
                    return 1;
                }
                if (PackageManagerService.this.checkUidPermission("android.permission.WRITE_EXTERNAL_STORAGE", uid) == -1) {
                    return 2;
                }
                return 3;
            }

            public boolean hasExternalStorage(int uid, String packageName) {
                return true;
            }
        });
        sUserManager.reconcileUsers(StorageManager.UUID_PRIVATE_INTERNAL);
        reconcileApps(StorageManager.UUID_PRIVATE_INTERNAL);
        if (this.mPrivappPermissionsViolations != null) {
            Slog.wtf(TAG, "Signature|privileged permissions not in privapp-permissions whitelist: " + this.mPrivappPermissionsViolations);
            this.mPrivappPermissionsViolations = null;
        }
    }

    public void waitForAppDataPrepared() {
        if (this.mPrepareAppDataFuture != null) {
            ConcurrentUtils.waitForFutureNoInterrupt(this.mPrepareAppDataFuture, "wait for prepareAppData");
            this.mPrepareAppDataFuture = null;
        }
    }

    public boolean isSafeMode() {
        return this.mSafeMode;
    }

    public boolean hasSystemUidErrors() {
        return this.mHasSystemUidErrors;
    }

    static String arrayToString(int[] array) {
        StringBuffer buf = new StringBuffer(128);
        buf.append('[');
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(array[i]);
            }
        }
        buf.append(']');
        return buf.toString();
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new PackageManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        BufferedReader in;
        AutoCloseable autoCloseable;
        Throwable th;
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, pw)) {
            int user;
            Log.i(TAG, "Start dump, calling from : pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            DumpState dumpState = new DumpState();
            boolean fullPreferred = false;
            boolean checkin = false;
            String str = null;
            ArraySet<String> permissionNames = null;
            int opti = 0;
            while (opti < args.length) {
                String opt = args[opti];
                if (opt == null || opt.length() <= 0 || opt.charAt(0) != '-') {
                    break;
                }
                opti++;
                if (!"-a".equals(opt)) {
                    if ("-h".equals(opt)) {
                        pw.println("Package manager dump options:");
                        pw.println("  [-h] [-f] [--checkin] [cmd] ...");
                        pw.println("    --checkin: dump for a checkin");
                        pw.println("    -f: print details of intent filters");
                        pw.println("    -h: print this help");
                        pw.println("  cmd may be one of:");
                        pw.println("    l[ibraries]: list known shared libraries");
                        pw.println("    f[eatures]: list device features");
                        pw.println("    k[eysets]: print known keysets");
                        pw.println("    r[esolvers] [activity|service|receiver|content]: dump intent resolvers");
                        pw.println("    perm[issions]: dump permissions");
                        pw.println("    permission [name ...]: dump declaration and use of given permission");
                        pw.println("    pref[erred]: print preferred package settings");
                        pw.println("    preferred-xml [--full]: print preferred package settings as xml");
                        pw.println("    prov[iders]: dump content providers");
                        pw.println("    p[ackages]: dump installed packages");
                        pw.println("    s[hared-users]: dump shared user IDs");
                        pw.println("    m[essages]: print collected runtime messages");
                        pw.println("    v[erifiers]: print package verifier info");
                        pw.println("    d[omain-preferred-apps]: print domains preferred apps");
                        pw.println("    i[ntent-filter-verifiers]|ifv: print intent filter verifier info");
                        pw.println("    version: print database version info");
                        pw.println("    write: write current settings now");
                        pw.println("    installs: details about install sessions");
                        pw.println("    check-permission <permission> <package> [<user>]: does pkg hold perm?");
                        pw.println("    dexopt: dump dexopt state");
                        pw.println("    compiler-stats: dump compiler statistics");
                        pw.println("    enabled-overlays: dump list of enabled overlay packages");
                        pw.println("    <package.name>: info about given package");
                        return;
                    } else if ("--checkin".equals(opt)) {
                        checkin = true;
                    } else if ("-f".equals(opt)) {
                        dumpState.setOptionEnabled(1);
                    } else if ("--proto".equals(opt)) {
                        dumpProto(fd);
                        return;
                    } else {
                        pw.println("Unknown argument: " + opt + "; use -h for help");
                    }
                }
            }
            if (opti < args.length) {
                String cmd = args[opti];
                opti++;
                if (!PLATFORM_PACKAGE_NAME.equals(cmd)) {
                    if (!cmd.contains(".")) {
                        if ("check-permission".equals(cmd)) {
                            if (opti >= args.length) {
                                pw.println("Error: check-permission missing permission argument");
                                return;
                            }
                            String perm = args[opti];
                            opti++;
                            if (opti >= args.length) {
                                pw.println("Error: check-permission missing package argument");
                                return;
                            }
                            String pkg = args[opti];
                            opti++;
                            user = UserHandle.getUserId(Binder.getCallingUid());
                            if (opti < args.length) {
                                try {
                                    user = Integer.parseInt(args[opti]);
                                } catch (NumberFormatException e) {
                                    pw.println("Error: check-permission user argument is not a number: " + args[opti]);
                                    return;
                                }
                            }
                            pw.println(checkPermission(perm, resolveInternalPackageNameLPr(pkg, -1), user));
                            return;
                        } else if ("l".equals(cmd) || "libraries".equals(cmd)) {
                            dumpState.setDump(1);
                        } else if ("f".equals(cmd) || "features".equals(cmd)) {
                            dumpState.setDump(2);
                        } else if ("r".equals(cmd) || "resolvers".equals(cmd)) {
                            if (opti >= args.length) {
                                dumpState.setDump(60);
                            } else {
                                while (opti < args.length) {
                                    String name = args[opti];
                                    if ("a".equals(name) || "activity".equals(name)) {
                                        dumpState.setDump(4);
                                    } else if ("s".equals(name) || "service".equals(name)) {
                                        dumpState.setDump(8);
                                    } else if ("r".equals(name) || HwBroadcastRadarUtil.KEY_RECEIVER.equals(name)) {
                                        dumpState.setDump(16);
                                    } else if ("c".equals(name) || "content".equals(name)) {
                                        dumpState.setDump(32);
                                    } else {
                                        pw.println("Error: unknown resolver table type: " + name);
                                        return;
                                    }
                                    opti++;
                                }
                            }
                        } else if (TAG_PERMISSION.equals(cmd) || "permissions".equals(cmd)) {
                            dumpState.setDump(64);
                        } else if ("permission".equals(cmd)) {
                            if (opti >= args.length) {
                                pw.println("Error: permission requires permission name");
                                return;
                            }
                            permissionNames = new ArraySet();
                            while (opti < args.length) {
                                permissionNames.add(args[opti]);
                                opti++;
                            }
                            dumpState.setDump(448);
                        } else if ("pref".equals(cmd) || "preferred".equals(cmd)) {
                            dumpState.setDump(4096);
                        } else if ("preferred-xml".equals(cmd)) {
                            dumpState.setDump(8192);
                            if (opti < args.length && "--full".equals(args[opti])) {
                                fullPreferred = true;
                                opti++;
                            }
                        } else if ("d".equals(cmd) || "domain-preferred-apps".equals(cmd)) {
                            dumpState.setDump(262144);
                        } else if ("p".equals(cmd) || "packages".equals(cmd)) {
                            dumpState.setDump(128);
                        } else if ("s".equals(cmd) || "shared-users".equals(cmd)) {
                            dumpState.setDump(256);
                        } else if ("prov".equals(cmd) || "providers".equals(cmd)) {
                            dumpState.setDump(1024);
                        } else if ("m".equals(cmd) || "messages".equals(cmd)) {
                            dumpState.setDump(512);
                        } else if ("v".equals(cmd) || "verifiers".equals(cmd)) {
                            dumpState.setDump(2048);
                        } else if ("i".equals(cmd) || "ifv".equals(cmd) || "intent-filter-verifiers".equals(cmd)) {
                            dumpState.setDump(131072);
                        } else if ("version".equals(cmd)) {
                            dumpState.setDump(32768);
                        } else if ("k".equals(cmd) || "keysets".equals(cmd)) {
                            dumpState.setDump(16384);
                        } else if ("installs".equals(cmd)) {
                            dumpState.setDump(65536);
                        } else if ("frozen".equals(cmd)) {
                            dumpState.setDump(524288);
                        } else if ("volumes".equals(cmd)) {
                            dumpState.setDump(DumpState.DUMP_VOLUMES);
                        } else if ("dexopt".equals(cmd)) {
                            dumpState.setDump(1048576);
                        } else if ("compiler-stats".equals(cmd)) {
                            dumpState.setDump(DumpState.DUMP_COMPILER_STATS);
                        } else if ("changes".equals(cmd)) {
                            dumpState.setDump(DumpState.DUMP_CHANGES);
                        } else if ("write".equals(cmd)) {
                            synchronized (this.mPackages) {
                                this.mSettings.writeLPr();
                                pw.println("Settings written.");
                            }
                            return;
                        }
                    }
                }
                str = cmd;
                dumpState.setOptionEnabled(1);
            }
            if (checkin) {
                pw.println("vers,1");
            }
            synchronized (this.mPackages) {
                int i;
                ActivityIntentResolver activityIntentResolver;
                String str2;
                IndentingPrintWriter indentingPrintWriter;
                BufferedReader bufferedReader;
                String line;
                if (dumpState.isDumping(32768) && str == null && !checkin) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Database versions:");
                    this.mSettings.dumpVersionLPr(new IndentingPrintWriter(pw, "  "));
                }
                if (dumpState.isDumping(2048) && str == null) {
                    if (!checkin) {
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        pw.println("Verifiers:");
                        pw.print("  Required: ");
                        pw.print(this.mRequiredVerifierPackage);
                        pw.print(" (uid=");
                        pw.print(getPackageUid(this.mRequiredVerifierPackage, 268435456, 0));
                        pw.println(")");
                    } else if (this.mRequiredVerifierPackage != null) {
                        pw.print("vrfy,");
                        pw.print(this.mRequiredVerifierPackage);
                        pw.print(",");
                        pw.println(getPackageUid(this.mRequiredVerifierPackage, 268435456, 0));
                    }
                }
                if (dumpState.isDumping(131072) && str == null) {
                    if (this.mIntentFilterVerifierComponent != null) {
                        String verifierPackageName = this.mIntentFilterVerifierComponent.getPackageName();
                        if (!checkin) {
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            pw.println("Intent Filter Verifier:");
                            pw.print("  Using: ");
                            pw.print(verifierPackageName);
                            pw.print(" (uid=");
                            pw.print(getPackageUid(verifierPackageName, 268435456, 0));
                            pw.println(")");
                        } else if (verifierPackageName != null) {
                            pw.print("ifv,");
                            pw.print(verifierPackageName);
                            pw.print(",");
                            pw.println(getPackageUid(verifierPackageName, 268435456, 0));
                        }
                    } else {
                        pw.println();
                        pw.println("No Intent Filter Verifier available!");
                    }
                }
                if (dumpState.isDumping(1) && str == null) {
                    boolean printedHeader = false;
                    for (String libName : this.mSharedLibraries.keySet()) {
                        SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.get(libName);
                        if (versionedLib != null) {
                            int versionCount = versionedLib.size();
                            for (i = 0; i < versionCount; i++) {
                                SharedLibraryEntry libEntry = (SharedLibraryEntry) versionedLib.valueAt(i);
                                if (checkin) {
                                    pw.print("lib,");
                                } else {
                                    if (!printedHeader) {
                                        if (dumpState.onTitlePrinted()) {
                                            pw.println();
                                        }
                                        pw.println("Libraries:");
                                        printedHeader = true;
                                    }
                                    pw.print("  ");
                                }
                                pw.print(libEntry.info.getName());
                                if (libEntry.info.isStatic()) {
                                    pw.print(" version=" + libEntry.info.getVersion());
                                }
                                if (!checkin) {
                                    pw.print(" -> ");
                                }
                                if (libEntry.path != null) {
                                    pw.print(" (jar) ");
                                    pw.print(libEntry.path);
                                } else {
                                    pw.print(" (apk) ");
                                    pw.print(libEntry.apk);
                                }
                                pw.println();
                            }
                        }
                    }
                }
                if (dumpState.isDumping(2) && str == null) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    if (!checkin) {
                        pw.println("Features:");
                    }
                    synchronized (this.mAvailableFeatures) {
                        for (FeatureInfo feat : this.mAvailableFeatures.values()) {
                            if (checkin) {
                                pw.print("feat,");
                                pw.print(feat.name);
                                pw.print(",");
                                pw.println(feat.version);
                            } else {
                                pw.print("  ");
                                pw.print(feat.name);
                                if (feat.version > 0) {
                                    pw.print(" version=");
                                    pw.print(feat.version);
                                }
                                pw.println();
                            }
                        }
                    }
                }
                if (!checkin && dumpState.isDumping(4)) {
                    activityIntentResolver = this.mActivities;
                    if (dumpState.getTitlePrinted()) {
                        str2 = "\nActivity Resolver Table:";
                    } else {
                        str2 = "Activity Resolver Table:";
                    }
                    if (activityIntentResolver.dump(pw, str2, "  ", str, dumpState.isOptionEnabled(1), true)) {
                        dumpState.setTitlePrinted(true);
                    }
                }
                if (!checkin && dumpState.isDumping(16)) {
                    activityIntentResolver = this.mReceivers;
                    if (dumpState.getTitlePrinted()) {
                        str2 = "\nReceiver Resolver Table:";
                    } else {
                        str2 = "Receiver Resolver Table:";
                    }
                    if (activityIntentResolver.dump(pw, str2, "  ", str, dumpState.isOptionEnabled(1), true)) {
                        dumpState.setTitlePrinted(true);
                    }
                }
                if (!checkin && dumpState.isDumping(8)) {
                    ServiceIntentResolver serviceIntentResolver = this.mServices;
                    if (dumpState.getTitlePrinted()) {
                        str2 = "\nService Resolver Table:";
                    } else {
                        str2 = "Service Resolver Table:";
                    }
                    if (serviceIntentResolver.dump(pw, str2, "  ", str, dumpState.isOptionEnabled(1), true)) {
                        dumpState.setTitlePrinted(true);
                    }
                }
                if (!checkin && dumpState.isDumping(32)) {
                    ProviderIntentResolver providerIntentResolver = this.mProviders;
                    if (dumpState.getTitlePrinted()) {
                        str2 = "\nProvider Resolver Table:";
                    } else {
                        str2 = "Provider Resolver Table:";
                    }
                    if (providerIntentResolver.dump(pw, str2, "  ", str, dumpState.isOptionEnabled(1), true)) {
                        dumpState.setTitlePrinted(true);
                    }
                }
                if (!checkin && dumpState.isDumping(4096)) {
                    for (i = 0; i < this.mSettings.mPreferredActivities.size(); i++) {
                        PreferredIntentResolver pir = (PreferredIntentResolver) this.mSettings.mPreferredActivities.valueAt(i);
                        user = this.mSettings.mPreferredActivities.keyAt(i);
                        if (dumpState.getTitlePrinted()) {
                            str2 = "\nPreferred Activities User " + user + ":";
                        } else {
                            str2 = "Preferred Activities User " + user + ":";
                        }
                        if (pir.dump(pw, str2, "  ", str, true, false)) {
                            dumpState.setTitlePrinted(true);
                        }
                    }
                }
                if (!checkin && dumpState.isDumping(8192)) {
                    pw.flush();
                    OutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fd));
                    XmlSerializer serializer = new FastXmlSerializer();
                    try {
                        serializer.setOutput(bufferedOutputStream, StandardCharsets.UTF_8.name());
                        serializer.startDocument(null, Boolean.valueOf(true));
                        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                        this.mSettings.writePreferredActivitiesLPr(serializer, 0, fullPreferred);
                        serializer.endDocument();
                        serializer.flush();
                    } catch (IllegalArgumentException e2) {
                        pw.println("Failed writing: " + e2);
                    } catch (IllegalStateException e3) {
                        pw.println("Failed writing: " + e3);
                    } catch (IOException e4) {
                        pw.println("Failed writing: " + e4);
                    }
                }
                if (!checkin) {
                    if (dumpState.isDumping(262144) && str == null) {
                        pw.println();
                        if (this.mSettings.mPackages.size() == 0) {
                            pw.println("No applications!");
                            pw.println();
                        } else {
                            String prefix = "  ";
                            Collection<PackageSetting> allPackageSettings = this.mSettings.mPackages.values();
                            if (allPackageSettings.size() == 0) {
                                pw.println("No domain preferred apps!");
                                pw.println();
                            } else {
                                pw.println("App verification status:");
                                pw.println();
                                int count = 0;
                                for (PackageSetting ps : allPackageSettings) {
                                    IntentFilterVerificationInfo ivi = ps.getIntentFilterVerificationInfo();
                                    if (!(ivi == null || ivi.getPackageName() == null)) {
                                        pw.println("  Package: " + ivi.getPackageName());
                                        pw.println("  Domains: " + ivi.getDomainsString());
                                        pw.println("  Status:  " + ivi.getStatusString());
                                        pw.println();
                                        count++;
                                    }
                                }
                                if (count == 0) {
                                    pw.println("  No app verification established.");
                                    pw.println();
                                }
                                for (int userId : sUserManager.getUserIds()) {
                                    pw.println("App linkages for user " + userId + ":");
                                    pw.println();
                                    count = 0;
                                    for (PackageSetting ps2 : allPackageSettings) {
                                        long status = ps2.getDomainVerificationStatusForUser(userId);
                                        if ((status >> 32) != 0) {
                                            pw.println("  Package: " + ps2.name);
                                            pw.println("  Domains: " + dumpDomainString(ps2.name));
                                            pw.println("  Status:  " + IntentFilterVerificationInfo.getStatusStringFromValue(status));
                                            pw.println();
                                            count++;
                                        }
                                    }
                                    if (count == 0) {
                                        pw.println("  No configured app linkages.");
                                        pw.println();
                                    }
                                }
                            }
                        }
                    }
                }
                if (!checkin && dumpState.isDumping(64)) {
                    this.mSettings.dumpPermissionsLPr(pw, str, permissionNames, dumpState);
                    if (str == null && permissionNames == null) {
                        for (int iperm = 0; iperm < this.mAppOpPermissionPackages.size(); iperm++) {
                            if (iperm == 0) {
                                if (dumpState.onTitlePrinted()) {
                                    pw.println();
                                }
                                pw.println("AppOp Permissions:");
                            }
                            pw.print("  AppOp Permission ");
                            pw.print((String) this.mAppOpPermissionPackages.keyAt(iperm));
                            pw.println(":");
                            ArraySet<String> pkgs = (ArraySet) this.mAppOpPermissionPackages.valueAt(iperm);
                            for (int ipkg = 0; ipkg < pkgs.size(); ipkg++) {
                                pw.print("    ");
                                pw.println((String) pkgs.valueAt(ipkg));
                            }
                        }
                    }
                }
                if (!checkin && dumpState.isDumping(1024)) {
                    Provider p;
                    boolean printedSomething = false;
                    for (Provider p2 : this.mProviders.mProviders.values()) {
                        if (str == null || (str.equals(p2.info.packageName) ^ 1) == 0) {
                            if (!printedSomething) {
                                if (dumpState.onTitlePrinted()) {
                                    pw.println();
                                }
                                pw.println("Registered ContentProviders:");
                                printedSomething = true;
                            }
                            pw.print("  ");
                            p2.printComponentShortName(pw);
                            pw.println(":");
                            pw.print("    ");
                            pw.println(p2.toString());
                        }
                    }
                    printedSomething = false;
                    for (Entry<String, Provider> entry : this.mProvidersByAuthority.entrySet()) {
                        p2 = (Provider) entry.getValue();
                        if (str == null || (str.equals(p2.info.packageName) ^ 1) == 0) {
                            if (!printedSomething) {
                                if (dumpState.onTitlePrinted()) {
                                    pw.println();
                                }
                                pw.println("ContentProvider Authorities:");
                                printedSomething = true;
                            }
                            pw.print("  [");
                            pw.print((String) entry.getKey());
                            pw.println("]:");
                            pw.print("    ");
                            pw.println(p2.toString());
                            if (!(p2.info == null || p2.info.applicationInfo == null)) {
                                String appInfo = p2.info.applicationInfo.toString();
                                pw.print("      applicationInfo=");
                                pw.println(appInfo);
                            }
                        }
                    }
                }
                if (!checkin && dumpState.isDumping(16384)) {
                    this.mSettings.mKeySetManagerService.dumpLPr(pw, str, dumpState);
                }
                if (dumpState.isDumping(128)) {
                    this.mSettings.dumpPackagesLPr(pw, str, permissionNames, dumpState, checkin);
                }
                if (dumpState.isDumping(256)) {
                    this.mSettings.dumpSharedUsersLPr(pw, str, permissionNames, dumpState, checkin);
                }
                if (dumpState.isDumping(DumpState.DUMP_CHANGES)) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    pw.println("Package Changes:");
                    pw.print("  Sequence number=");
                    pw.println(this.mChangedPackagesSequenceNumber);
                    int K = this.mChangedPackages.size();
                    for (i = 0; i < K; i++) {
                        SparseArray<String> changes = (SparseArray) this.mChangedPackages.valueAt(i);
                        pw.print("  User ");
                        pw.print(this.mChangedPackages.keyAt(i));
                        pw.println(":");
                        int N = changes.size();
                        if (N == 0) {
                            pw.print("    ");
                            pw.println("No packages changed");
                        } else {
                            for (int j = 0; j < N; j++) {
                                String pkgName = (String) changes.valueAt(j);
                                int sequenceNumber = changes.keyAt(j);
                                pw.print("    ");
                                pw.print("seq=");
                                pw.print(sequenceNumber);
                                pw.print(", package=");
                                pw.println(pkgName);
                            }
                        }
                    }
                }
                if (!checkin && dumpState.isDumping(64) && str == null) {
                    this.mSettings.dumpRestoredPermissionGrantsLPr(pw, dumpState);
                }
                if (!checkin && dumpState.isDumping(524288) && str == null) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    indentingPrintWriter = new IndentingPrintWriter(pw, "  ", 120);
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Frozen packages:");
                    indentingPrintWriter.increaseIndent();
                    if (this.mFrozenPackages.size() == 0) {
                        indentingPrintWriter.println("(none)");
                    } else {
                        for (i = 0; i < this.mFrozenPackages.size(); i++) {
                            indentingPrintWriter.println((String) this.mFrozenPackages.valueAt(i));
                        }
                    }
                    indentingPrintWriter.decreaseIndent();
                }
                if (!checkin && dumpState.isDumping(DumpState.DUMP_VOLUMES) && str == null) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    indentingPrintWriter = new IndentingPrintWriter(pw, "  ", 120);
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Loaded volumes:");
                    indentingPrintWriter.increaseIndent();
                    if (this.mLoadedVolumes.size() == 0) {
                        indentingPrintWriter.println("(none)");
                    } else {
                        for (i = 0; i < this.mLoadedVolumes.size(); i++) {
                            indentingPrintWriter.println((String) this.mLoadedVolumes.valueAt(i));
                        }
                    }
                    indentingPrintWriter.decreaseIndent();
                }
                if (!checkin && dumpState.isDumping(1048576)) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    dumpDexoptStateLPr(pw, str);
                }
                if (!checkin && dumpState.isDumping(DumpState.DUMP_COMPILER_STATS)) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    dumpCompilerStatsLPr(pw, str);
                }
                if (!checkin && dumpState.isDumping(512) && str == null) {
                    if (dumpState.onTitlePrinted()) {
                        pw.println();
                    }
                    this.mSettings.dumpReadMessagesLPr(pw, dumpState);
                    pw.println();
                    pw.println("Package warning messages:");
                    in = null;
                    try {
                        bufferedReader = new BufferedReader(new FileReader(getSettingsProblemFile()));
                        while (true) {
                            try {
                                line = bufferedReader.readLine();
                                if (line == null) {
                                    break;
                                }
                                if (!line.contains("ignored: updated version")) {
                                    pw.println(line);
                                }
                            } catch (IOException e5) {
                                autoCloseable = bufferedReader;
                            } catch (Throwable th2) {
                                th = th2;
                                in = bufferedReader;
                            }
                        }
                        IoUtils.closeQuietly(bufferedReader);
                    } catch (IOException e6) {
                        IoUtils.closeQuietly(autoCloseable);
                        in = null;
                        try {
                            bufferedReader = new BufferedReader(new FileReader(getSettingsProblemFile()));
                            while (true) {
                                try {
                                    line = bufferedReader.readLine();
                                    if (line != null) {
                                        break;
                                    }
                                    if (!line.contains("ignored: updated version")) {
                                        pw.print("msg,");
                                        pw.println(line);
                                    }
                                } catch (IOException e7) {
                                    autoCloseable = bufferedReader;
                                } catch (Throwable th3) {
                                    th = th3;
                                    in = bufferedReader;
                                }
                            }
                            IoUtils.closeQuietly(bufferedReader);
                        } catch (IOException e8) {
                            IoUtils.closeQuietly(autoCloseable);
                            if (dumpState.onTitlePrinted()) {
                                pw.println();
                            }
                            this.mInstallerService.dump(new IndentingPrintWriter(pw, "  ", 120));
                        } catch (Throwable th4) {
                            th = th4;
                            IoUtils.closeQuietly(in);
                            throw th;
                        }
                        if (dumpState.onTitlePrinted()) {
                            pw.println();
                        }
                        this.mInstallerService.dump(new IndentingPrintWriter(pw, "  ", 120));
                    } catch (Throwable th5) {
                        th = th5;
                        IoUtils.closeQuietly(in);
                        throw th;
                    }
                }
                if (checkin && dumpState.isDumping(512)) {
                    in = null;
                    bufferedReader = new BufferedReader(new FileReader(getSettingsProblemFile()));
                    while (true) {
                        line = bufferedReader.readLine();
                        if (line != null) {
                            break;
                        }
                        if (!line.contains("ignored: updated version")) {
                            pw.print("msg,");
                            pw.println(line);
                        }
                    }
                    IoUtils.closeQuietly(bufferedReader);
                }
            }
            if (!checkin && dumpState.isDumping(65536) && str == null) {
                if (dumpState.onTitlePrinted()) {
                    pw.println();
                }
                this.mInstallerService.dump(new IndentingPrintWriter(pw, "  ", 120));
            }
        }
    }

    private void dumpProto(FileDescriptor fd) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mPackages) {
            long requiredVerifierPackageToken = proto.start(1172526071809L);
            proto.write(1159641169921L, this.mRequiredVerifierPackage);
            proto.write(1112396529666L, getPackageUid(this.mRequiredVerifierPackage, 268435456, 0));
            proto.end(requiredVerifierPackageToken);
            if (this.mIntentFilterVerifierComponent != null) {
                String verifierPackageName = this.mIntentFilterVerifierComponent.getPackageName();
                long verifierPackageToken = proto.start(1172526071810L);
                proto.write(1159641169921L, verifierPackageName);
                proto.write(1112396529666L, getPackageUid(verifierPackageName, 268435456, 0));
                proto.end(verifierPackageToken);
            }
            dumpSharedLibrariesProto(proto);
            dumpFeaturesProto(proto);
            this.mSettings.dumpPackagesProto(proto);
            this.mSettings.dumpSharedUsersProto(proto);
            dumpMessagesProto(proto);
        }
        proto.flush();
    }

    private void dumpMessagesProto(ProtoOutputStream proto) {
        AutoCloseable autoCloseable;
        Throwable th;
        BufferedReader in = null;
        try {
            BufferedReader in2 = new BufferedReader(new FileReader(getSettingsProblemFile()));
            while (true) {
                try {
                    String line = in2.readLine();
                    if (line == null) {
                        IoUtils.closeQuietly(in2);
                        return;
                    } else if (!line.contains("ignored: updated version")) {
                        proto.write(2259152797703L, line);
                    }
                } catch (IOException e) {
                    autoCloseable = in2;
                } catch (Throwable th2) {
                    th = th2;
                    in = in2;
                }
            }
        } catch (IOException e2) {
            IoUtils.closeQuietly(autoCloseable);
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly(in);
            throw th;
        }
    }

    private void dumpFeaturesProto(ProtoOutputStream proto) {
        synchronized (this.mAvailableFeatures) {
            int count = this.mAvailableFeatures.size();
            for (int i = 0; i < count; i++) {
                FeatureInfo feat = (FeatureInfo) this.mAvailableFeatures.valueAt(i);
                long featureToken = proto.start(2272037699588L);
                proto.write(1159641169921L, feat.name);
                proto.write(1112396529666L, feat.version);
                proto.end(featureToken);
            }
        }
    }

    private void dumpSharedLibrariesProto(ProtoOutputStream proto) {
        int count = this.mSharedLibraries.size();
        for (int i = 0; i < count; i++) {
            SparseArray<SharedLibraryEntry> versionedLib = (SparseArray) this.mSharedLibraries.get((String) this.mSharedLibraries.keyAt(i));
            if (versionedLib != null) {
                int versionCount = versionedLib.size();
                for (int j = 0; j < versionCount; j++) {
                    SharedLibraryEntry libEntry = (SharedLibraryEntry) versionedLib.valueAt(j);
                    long sharedLibraryToken = proto.start(2272037699587L);
                    proto.write(1159641169921L, libEntry.info.getName());
                    boolean isJar = libEntry.path != null;
                    proto.write(1155346202626L, isJar);
                    if (isJar) {
                        proto.write(1159641169923L, libEntry.path);
                    } else {
                        proto.write(1159641169924L, libEntry.apk);
                    }
                    proto.end(sharedLibraryToken);
                }
            }
        }
    }

    private void dumpDexoptStateLPr(PrintWriter pw, String packageName) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
        ipw.println();
        ipw.println("Dexopt state:");
        ipw.increaseIndent();
        if (packageName != null) {
            Package targetPackage = (Package) this.mPackages.get(packageName);
            if (targetPackage != null) {
                Collection<Package> packages = Collections.singletonList(targetPackage);
            } else {
                ipw.println("Unable to find package: " + packageName);
                return;
            }
        }
        packages = this.mPackages.values();
        for (Package pkg : packages) {
            ipw.println("[" + pkg.packageName + "]");
            ipw.increaseIndent();
            this.mPackageDexOptimizer.dumpDexoptState(ipw, pkg, this.mDexManager.getPackageUseInfoOrDefault(pkg.packageName));
            ipw.decreaseIndent();
        }
    }

    private void dumpCompilerStatsLPr(PrintWriter pw, String packageName) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ", 120);
        ipw.println();
        ipw.println("Compiler stats:");
        ipw.increaseIndent();
        if (packageName != null) {
            Package targetPackage = (Package) this.mPackages.get(packageName);
            if (targetPackage != null) {
                Collection<Package> packages = Collections.singletonList(targetPackage);
            } else {
                ipw.println("Unable to find package: " + packageName);
                return;
            }
        }
        packages = this.mPackages.values();
        for (Package pkg : packages) {
            ipw.println("[" + pkg.packageName + "]");
            ipw.increaseIndent();
            PackageStats stats = getCompilerPackageStats(pkg.packageName);
            if (stats == null) {
                ipw.println("(No recorded stats)");
            } else {
                stats.dump(ipw);
            }
            ipw.decreaseIndent();
        }
    }

    private String dumpDomainString(String packageName) {
        List<IntentFilterVerificationInfo> iviList = getIntentFilterVerifications(packageName).getList();
        List<IntentFilter> filters = getAllIntentFilters(packageName).getList();
        ArraySet<String> result = new ArraySet();
        if (iviList.size() > 0) {
            for (IntentFilterVerificationInfo ivi : iviList) {
                for (String host : ivi.getDomains()) {
                    result.add(host);
                }
            }
        }
        if (filters != null && filters.size() > 0) {
            for (IntentFilter filter : filters) {
                if (filter.hasCategory("android.intent.category.BROWSABLE") && (filter.hasDataScheme("http") || filter.hasDataScheme("https"))) {
                    result.addAll(filter.getHostsList());
                }
            }
        }
        StringBuilder sb = new StringBuilder(result.size() * 16);
        for (String domain : result) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(domain);
        }
        return sb.toString();
    }

    static String getEncryptKey() {
        try {
            String sdEncKey = SystemKeyStore.getInstance().retrieveKeyHexString(SD_ENCRYPTION_KEYSTORE_NAME);
            if (sdEncKey == null) {
                sdEncKey = SystemKeyStore.getInstance().generateNewKeyHexString(128, SD_ENCRYPTION_ALGORITHM, SD_ENCRYPTION_KEYSTORE_NAME);
                if (sdEncKey == null) {
                    Slog.e(TAG, "Failed to create encryption keys");
                    return null;
                }
            }
            return sdEncKey;
        } catch (NoSuchAlgorithmException nsae) {
            Slog.e(TAG, "Failed to create encryption keys with exception: " + nsae);
            return null;
        } catch (IOException ioe) {
            Slog.e(TAG, "Failed to retrieve encryption keys with exception: " + ioe);
            return null;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateExternalMediaStatus(final boolean mediaStatus, final boolean reportStatus) {
        int i = 1;
        enforceSystemOrRoot("Media status can only be updated by the system");
        synchronized (this.mPackages) {
            Log.i(TAG, "Updating external media status from " + (this.mMediaMounted ? "mounted" : "unmounted") + " to " + (mediaStatus ? "mounted" : "unmounted"));
            if (mediaStatus == this.mMediaMounted) {
                PackageHandler packageHandler = this.mHandler;
                if (!reportStatus) {
                    i = 0;
                }
                this.mHandler.sendMessage(packageHandler.obtainMessage(12, i, -1));
                return;
            }
            this.mMediaMounted = mediaStatus;
        }
    }

    public void scanAvailableAsecs() {
        updateExternalMediaStatusInner(true, false, false);
    }

    private void updateExternalMediaStatusInner(boolean isMounted, boolean reportStatus, boolean externalStorage) {
        ArrayMap<AsecInstallArgs, String> processCids = new ArrayMap();
        int[] uidArr = EmptyArray.INT;
        String[] list = PackageHelper.getSecureContainerList();
        if (ArrayUtils.isEmpty(list)) {
            Log.i(TAG, "No secure containers found");
        } else {
            synchronized (this.mPackages) {
                for (String cid : list) {
                    if (!PackageInstallerService.isStageName(cid)) {
                        String pkgName = getAsecPackageName(cid);
                        if (pkgName == null) {
                            Slog.i(TAG, "Found stale container " + cid + " with no package name");
                        } else {
                            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkgName);
                            if (ps == null) {
                                Slog.i(TAG, "Found stale container " + cid + " with no matching settings");
                            } else if (!externalStorage || (isMounted ^ 1) == 0 || (isExternal(ps) ^ 1) == 0) {
                                try {
                                    AsecInstallArgs args = new AsecInstallArgs(cid, InstructionSets.getAppDexInstructionSets(ps), ps.isForwardLocked());
                                    if (ps.codePathString == null || args == null || args.getCodePath() == null || !ps.codePathString.startsWith(args.getCodePath())) {
                                        Slog.i(TAG, "Found stale container " + cid + ": expected codePath=" + ps.codePathString);
                                    } else {
                                        processCids.put(args, ps.codePathString);
                                        int uid = ps.appId;
                                        if (uid != -1) {
                                            uidArr = ArrayUtils.appendInt(uidArr, uid);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.w(TAG, "avoid exception is " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
            Arrays.sort(uidArr);
        }
        if (isMounted) {
            loadMediaPackages(processCids, uidArr, externalStorage);
            startCleaningPackages();
            this.mInstallerService.onSecureContainersAvailable();
            return;
        }
        unloadMediaPackages(processCids, uidArr, reportStatus);
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, ArrayList<ApplicationInfo> infos, IIntentReceiver finishedReceiver) {
        int size = infos.size();
        String[] packageNames = new String[size];
        int[] packageUids = new int[size];
        for (int i = 0; i < size; i++) {
            ApplicationInfo info = (ApplicationInfo) infos.get(i);
            packageNames[i] = info.packageName;
            packageUids[i] = info.uid;
        }
        sendResourcesChangedBroadcast(mediaStatus, replacing, packageNames, packageUids, finishedReceiver);
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, ArrayList<String> pkgList, int[] uidArr, IIntentReceiver finishedReceiver) {
        sendResourcesChangedBroadcast(mediaStatus, replacing, (String[]) pkgList.toArray(new String[pkgList.size()]), uidArr, finishedReceiver);
    }

    private void sendResourcesChangedBroadcast(boolean mediaStatus, boolean replacing, String[] pkgList, int[] uidArr, IIntentReceiver finishedReceiver) {
        if (pkgList.length > 0) {
            String action;
            Bundle extras = new Bundle();
            extras.putStringArray("android.intent.extra.changed_package_list", pkgList);
            if (uidArr != null) {
                extras.putIntArray("android.intent.extra.changed_uid_list", uidArr);
            }
            if (replacing) {
                extras.putBoolean("android.intent.extra.REPLACING", replacing);
            }
            if (mediaStatus) {
                action = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
            } else {
                action = "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
            }
            sendPackageBroadcast(action, null, extras, 0, null, finishedReceiver, null);
        }
    }

    private void loadMediaPackages(ArrayMap<AsecInstallArgs, String> processCids, int[] uidArr, boolean externalStorage) {
        ArrayList<String> pkgList = new ArrayList();
        for (AsecInstallArgs args : processCids.keySet()) {
            String codePath = (String) processCids.get(args);
            int retCode = -18;
            if (this.mCustPms != null && this.mCustPms.isSdInstallEnabled()) {
                PackageHelper.unMountSdDir(args.cid);
            }
            if (args.doPreInstall(1) != 1) {
                Slog.e(TAG, "Failed to mount cid : " + args.cid + " when installing from sdcard");
                Log.w(TAG, "Container " + args.cid + " is stale, retCode=" + -18);
            } else {
                if (codePath != null) {
                    if ((codePath.startsWith(args.getCodePath()) ^ 1) == 0) {
                        int parseFlags = this.mDefParseFlags;
                        if (args.isExternalAsec()) {
                            parseFlags |= 32;
                        }
                        if (args.isFwdLocked()) {
                            parseFlags |= 16;
                        }
                        synchronized (this.mInstallLock) {
                            Package pkg = null;
                            try {
                                pkg = scanPackageTracedLI(new File(codePath), parseFlags, 32768, 0, null);
                            } catch (PackageManagerException e) {
                                Slog.w(TAG, "Failed to scan " + codePath + ": " + e.getMessage());
                            }
                            if (pkg != null) {
                                synchronized (this.mPackages) {
                                    retCode = 1;
                                    pkgList.add(pkg.packageName);
                                    args.doPostInstall(1, pkg.applicationInfo.uid);
                                }
                            } else {
                                Slog.i(TAG, "Failed to install pkg from  " + codePath + " from sdcard");
                            }
                            try {
                            } catch (Throwable th) {
                                if (retCode != 1) {
                                    Log.w(TAG, "Container " + args.cid + " is stale, retCode=" + retCode);
                                }
                            }
                        }
                        if (retCode != 1) {
                            Log.w(TAG, "Container " + args.cid + " is stale, retCode=" + retCode);
                        }
                    }
                }
                Slog.e(TAG, "Container " + args.cid + " cachepath " + args.getCodePath() + " does not match one in settings " + codePath);
                Log.w(TAG, "Container " + args.cid + " is stale, retCode=" + -18);
            }
        }
        synchronized (this.mPackages) {
            VersionInfo ver;
            String volumeUuid;
            if (externalStorage) {
                ver = this.mSettings.getExternalVersion();
            } else {
                ver = this.mSettings.getInternalVersion();
            }
            if (externalStorage) {
                volumeUuid = "primary_physical";
            } else {
                volumeUuid = StorageManager.UUID_PRIVATE_INTERNAL;
            }
            int updateFlags = 1;
            if (!(ver == null || ver.sdkVersion == this.mSdkVersion)) {
                logCriticalInfo(4, "Platform changed from " + ver.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for external");
                updateFlags = 7;
            }
            if (this.mCustPms != null && this.mCustPms.isSdInstallEnabled()) {
                updateFlags |= 6;
            }
            updatePermissionsLPw(null, null, volumeUuid, updateFlags);
            if (ver != null) {
                ver.forceCurrent();
            }
            this.mSettings.writeLPr();
        }
        if (pkgList.size() > 0) {
            sendResourcesChangedBroadcast(true, false, (ArrayList) pkgList, uidArr, null);
        }
    }

    private void unloadAllContainers(Set<AsecInstallArgs> cidArgs) {
        for (AsecInstallArgs arg : cidArgs) {
            synchronized (this.mInstallLock) {
                arg.doPostDeleteLI(false);
            }
        }
    }

    private void unloadMediaPackages(ArrayMap<AsecInstallArgs, String> processCids, int[] uidArr, boolean reportStatus) {
        Throwable th;
        Throwable th2;
        ArrayList pkgList = new ArrayList();
        ArrayList<AsecInstallArgs> failedList = new ArrayList();
        Set<AsecInstallArgs> keys = processCids.keySet();
        for (AsecInstallArgs args : keys) {
            String pkgName = args.getPackageName();
            PackageRemovedInfo outInfo = new PackageRemovedInfo(this);
            synchronized (this.mInstallLock) {
                Throwable th3 = null;
                PackageFreezer packageFreezer = null;
                try {
                    packageFreezer = freezePackageForDelete(pkgName, 1, "unloadMediaPackages");
                    boolean res = deletePackageLIF(pkgName, null, false, null, 1, outInfo, false, null);
                    if (packageFreezer != null) {
                        try {
                            packageFreezer.close();
                        } catch (Throwable th4) {
                            th3 = th4;
                        }
                    }
                    if (th3 != null) {
                        throw th3;
                    } else {
                        if (res) {
                            pkgList.add(pkgName);
                        } else {
                            Slog.e(TAG, "Failed to delete pkg from sdcard : " + pkgName);
                            failedList.add(args);
                        }
                    }
                } catch (Throwable th22) {
                    Throwable th5 = th22;
                    th22 = th;
                    th = th5;
                }
            }
        }
        synchronized (this.mPackages) {
            this.mSettings.writeLPr();
        }
        if (pkgList.size() > 0) {
            final boolean z = reportStatus;
            final Set<AsecInstallArgs> set = keys;
            sendResourcesChangedBroadcast(false, false, pkgList, uidArr, new IIntentReceiver.Stub() {
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                    PackageManagerService.this.mHandler.sendMessage(PackageManagerService.this.mHandler.obtainMessage(12, z ? 1 : 0, 1, set));
                }
            });
            return;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(12, reportStatus ? 1 : 0, -1, keys));
        return;
        if (packageFreezer != null) {
            try {
                packageFreezer.close();
            } catch (Throwable th6) {
                if (th22 == null) {
                    th22 = th6;
                } else if (th22 != th6) {
                    th22.addSuppressed(th6);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    private void loadPrivatePackages(final VolumeInfo vol) {
        this.mHandler.post(new Runnable() {
            public void run() {
                PackageManagerService.this.loadPrivatePackagesInner(vol);
            }
        });
    }

    private void loadPrivatePackagesInner(VolumeInfo vol) {
        String volumeUuid = vol.fsUuid;
        if (TextUtils.isEmpty(volumeUuid)) {
            Slog.e(TAG, "Loading internal storage is probably a mistake; ignoring");
            return;
        }
        ArrayList<PackageFreezer> freezers = new ArrayList();
        ArrayList<ApplicationInfo> loaded = new ArrayList();
        int parseFlags = this.mDefParseFlags | 32;
        synchronized (this.mPackages) {
            VersionInfo ver = this.mSettings.findOrCreateVersion(volumeUuid);
            List<PackageSetting> packages = this.mSettings.getVolumePackagesLPr(volumeUuid);
        }
        for (PackageSetting ps : packages) {
            freezers.add(freezePackage(ps.name, "loadPrivatePackagesInner"));
            synchronized (this.mInstallLock) {
                try {
                    loaded.add(scanPackageTracedLI(ps.codePath, parseFlags, 4096, 0, null).applicationInfo);
                } catch (PackageManagerException e) {
                    Slog.w(TAG, "Failed to scan " + ps.codePath + ": " + e.getMessage());
                }
                if (!Build.FINGERPRINT.equals(ver.fingerprint)) {
                    clearAppDataLIF(ps.pkg, -1, UsbTerminalTypes.TERMINAL_IN_PERSONAL_MIC);
                }
            }
        }
        StorageManager sm = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
        UserManagerInternal umInternal = getUserManagerInternal();
        for (UserInfo user : um.getUsers()) {
            int flags;
            if (umInternal.isUserUnlockingOrUnlocked(user.id)) {
                flags = 3;
            } else {
                if (umInternal.isUserRunning(user.id)) {
                    flags = 1;
                } else {
                    continue;
                }
            }
            try {
                sm.prepareUserStorage(volumeUuid, user.id, user.serialNumber, flags);
                synchronized (this.mInstallLock) {
                    reconcileAppsDataLI(volumeUuid, user.id, flags, true);
                }
            } catch (IllegalStateException e2) {
                Slog.w(TAG, "Failed to prepare storage: " + e2);
            }
        }
        synchronized (this.mPackages) {
            int updateFlags = 1;
            if (ver.sdkVersion != this.mSdkVersion) {
                logCriticalInfo(4, "Platform changed from " + ver.sdkVersion + " to " + this.mSdkVersion + "; regranting permissions for " + volumeUuid);
                updateFlags = 7;
            }
            updatePermissionsLPw(null, null, volumeUuid, updateFlags);
            ver.forceCurrent();
            this.mSettings.writeLPr();
        }
        for (PackageFreezer freezer : freezers) {
            freezer.close();
        }
        sendResourcesChangedBroadcast(true, false, loaded, null);
        this.mLoadedVolumes.add(vol.getId());
    }

    private void unloadPrivatePackages(final VolumeInfo vol) {
        this.mHandler.post(new Runnable() {
            public void run() {
                PackageManagerService.this.unloadPrivatePackagesInner(vol);
            }
        });
    }

    private void unloadPrivatePackagesInner(VolumeInfo vol) {
        Throwable th;
        Throwable th2;
        String volumeUuid = vol.fsUuid;
        if (TextUtils.isEmpty(volumeUuid)) {
            Slog.e(TAG, "Unloading internal storage is probably a mistake; ignoring");
            return;
        }
        ArrayList<ApplicationInfo> unloaded = new ArrayList();
        synchronized (this.mInstallLock) {
            synchronized (this.mPackages) {
                for (PackageSetting ps : this.mSettings.getVolumePackagesLPr(volumeUuid)) {
                    PackageFreezer packageFreezer;
                    if (ps.pkg != null) {
                        ApplicationInfo info = ps.pkg.applicationInfo;
                        PackageRemovedInfo outInfo = new PackageRemovedInfo(this);
                        Throwable th3 = null;
                        packageFreezer = null;
                        try {
                            packageFreezer = freezePackageForDelete(ps.name, 1, "unloadPrivatePackagesInner");
                            if (deletePackageLIF(ps.name, null, false, null, 1, outInfo, false, null)) {
                                unloaded.add(info);
                            } else {
                                Slog.w(TAG, "Failed to unload " + ps.codePath);
                            }
                            if (packageFreezer != null) {
                                try {
                                    packageFreezer.close();
                                } catch (Throwable th4) {
                                    th3 = th4;
                                }
                            }
                            if (th3 != null) {
                                throw th3;
                            } else {
                                AttributeCache.instance().removePackage(ps.name);
                            }
                        } catch (Throwable th22) {
                            Throwable th5 = th22;
                            th22 = th;
                            th = th5;
                        }
                    }
                }
                this.mSettings.writeLPr();
            }
        }
        sendResourcesChangedBroadcast(false, false, unloaded, null);
        this.mLoadedVolumes.remove(vol.getId());
        ResourcesManager.getInstance().invalidatePath(vol.getPath().getAbsolutePath());
        for (int i = 0; i < 3; i++) {
            System.gc();
            System.runFinalization();
        }
        return;
        if (packageFreezer != null) {
            try {
                packageFreezer.close();
            } catch (Throwable th6) {
                if (th22 == null) {
                    th22 = th6;
                } else if (th22 != th6) {
                    th22.addSuppressed(th6);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    private void assertPackageKnown(String volumeUuid, String packageName) throws PackageManagerException {
        synchronized (this.mPackages) {
            packageName = normalizePackageNameLPr(packageName);
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                throw new PackageManagerException("Package " + packageName + " is unknown");
            } else if (TextUtils.equals(volumeUuid, ps.volumeUuid)) {
            } else {
                throw new PackageManagerException("Package " + packageName + " found on unknown volume " + volumeUuid + "; expected volume " + ps.volumeUuid);
            }
        }
    }

    private void assertPackageKnownAndInstalled(String volumeUuid, String packageName, int userId) throws PackageManagerException {
        synchronized (this.mPackages) {
            packageName = normalizePackageNameLPr(packageName);
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (ps == null) {
                throw new PackageManagerException("Package " + packageName + " is unknown");
            } else if (!TextUtils.equals(volumeUuid, ps.volumeUuid)) {
                throw new PackageManagerException("Package " + packageName + " found on unknown volume " + volumeUuid + "; expected volume " + ps.volumeUuid);
            } else if (ps.getInstalled(userId)) {
            } else {
                throw new PackageManagerException("Package " + packageName + " not installed for user " + userId);
            }
        }
    }

    private List<String> collectAbsoluteCodePaths() {
        List<String> codePaths;
        synchronized (this.mPackages) {
            codePaths = new ArrayList();
            int packageCount = this.mSettings.mPackages.size();
            for (int i = 0; i < packageCount; i++) {
                codePaths.add(((PackageSetting) this.mSettings.mPackages.valueAt(i)).codePath.getAbsolutePath());
            }
        }
        return codePaths;
    }

    private void reconcileApps(String volumeUuid) {
        List<String> absoluteCodePaths = collectAbsoluteCodePaths();
        List filesToDelete = null;
        for (File file : FileUtils.listFilesOrEmpty(Environment.getDataAppDirectory(volumeUuid))) {
            boolean isStageName;
            int i;
            if (PackageParser.isApkFile(file) || file.isDirectory()) {
                isStageName = PackageInstallerService.isStageName(file.getName()) ^ 1;
            } else {
                isStageName = false;
            }
            if (isStageName) {
                String absolutePath = file.getAbsolutePath();
                boolean pathValid = false;
                int absoluteCodePathCount = absoluteCodePaths.size();
                for (i = 0; i < absoluteCodePathCount; i++) {
                    if (absolutePath.startsWith((String) absoluteCodePaths.get(i))) {
                        pathValid = true;
                        break;
                    }
                }
                if (!pathValid) {
                    if (filesToDelete == null) {
                        filesToDelete = new ArrayList();
                    }
                    filesToDelete.add(file);
                }
            }
        }
        if (filesToDelete != null) {
            int fileToDeleteCount = filesToDelete.size();
            for (i = 0; i < fileToDeleteCount; i++) {
                File fileToDelete = (File) filesToDelete.get(i);
                logCriticalInfo(5, "Destroying orphaned" + fileToDelete);
                synchronized (this.mInstallLock) {
                    removeCodePathLI(fileToDelete);
                }
            }
        }
    }

    void reconcileAppsData(int userId, int flags, boolean migrateAppsData) {
        for (VolumeInfo vol : ((StorageManager) this.mContext.getSystemService(StorageManager.class)).getWritablePrivateVolumes()) {
            String volumeUuid = vol.getFsUuid();
            synchronized (this.mInstallLock) {
                reconcileAppsDataLI(volumeUuid, userId, flags, migrateAppsData);
            }
        }
    }

    private void reconcileAppsDataLI(String volumeUuid, int userId, int flags, boolean migrateAppData) {
        reconcileAppsDataLI(volumeUuid, userId, flags, migrateAppData, false);
    }

    private List<String> reconcileAppsDataLI(String volumeUuid, int userId, int flags, boolean migrateAppData, boolean onlyCoreApps) {
        String packageName;
        Slog.v(TAG, "reconcileAppsData for " + volumeUuid + " u" + userId + " 0x" + Integer.toHexString(flags) + " migrateAppData=" + migrateAppData);
        List<String> arrayList = onlyCoreApps ? new ArrayList() : null;
        File ceDir = Environment.getDataUserCeDirectory(volumeUuid, userId);
        File deDir = Environment.getDataUserDeDirectory(volumeUuid, userId);
        if ((flags & 2) != 0) {
            if (!StorageManager.isFileEncryptedNativeOrEmulated() || (StorageManager.isUserKeyUnlocked(userId) ^ 1) == 0) {
                for (File file : FileUtils.listFilesOrEmpty(ceDir)) {
                    packageName = file.getName();
                    if (!this.mIsUpgrade || this.mCustPms == null || this.mCustPms.isListedApp(packageName) != 1) {
                        try {
                            assertPackageKnownAndInstalled(volumeUuid, packageName, userId);
                        } catch (PackageManagerException e) {
                            if (this.mIsUpgrade && this.mCustPms != null && this.mCustPms.isListedApp(packageName) == 1) {
                                Slog.w(TAG, "Skip destroy " + file + " due to: " + packageName);
                            } else {
                                logCriticalInfo(5, "Destroying " + file + " due to: " + e);
                                try {
                                    this.mInstaller.destroyAppData(volumeUuid, packageName, userId, 2, 0);
                                } catch (InstallerException e2) {
                                    logCriticalInfo(5, "Failed to destroy: " + e2);
                                }
                            }
                        }
                    }
                }
            } else {
                throw new RuntimeException("Yikes, someone asked us to reconcile CE storage while " + userId + " was still locked; this would have caused massive data loss!");
            }
        }
        if ((flags & 1) != 0) {
            for (File file2 : FileUtils.listFilesOrEmpty(deDir)) {
                packageName = file2.getName();
                try {
                    assertPackageKnownAndInstalled(volumeUuid, packageName, userId);
                } catch (PackageManagerException e3) {
                    logCriticalInfo(5, "Destroying " + file2 + " due to: " + e3);
                    try {
                        this.mInstaller.destroyAppData(volumeUuid, packageName, userId, 1, 0);
                    } catch (InstallerException e22) {
                        logCriticalInfo(5, "Failed to destroy: " + e22);
                    }
                }
            }
        }
        synchronized (this.mPackages) {
            List<PackageSetting> packages = this.mSettings.getVolumePackagesLPr(volumeUuid);
        }
        if ((flags & 2) != 0) {
            try {
                UniPerf.getInstance().uniPerfEvent(4099, "", new int[0]);
            } catch (RuntimeException e4) {
                Slog.v(TAG, "raise cpu error!");
            }
        }
        int preparedCount = 0;
        for (PackageSetting ps : packages) {
            packageName = ps.name;
            if (ps.pkg == null) {
                Slog.w(TAG, "Odd, missing scanned package " + packageName);
            } else if (onlyCoreApps && (ps.pkg.coreApp ^ 1) != 0) {
                arrayList.add(packageName);
            } else if (ps.getInstalled(userId)) {
                prepareAppDataAndMigrateLIF(ps.pkg, userId, flags, migrateAppData);
                preparedCount++;
            }
        }
        Slog.v(TAG, "reconcileAppsData finished " + preparedCount + " packages");
        return arrayList;
    }

    protected void prepareAppDataAfterInstallLIF(Package pkg) {
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkg.packageName);
            this.mSettings.writeKernelMappingLPr(ps);
        }
        UserManager um = (UserManager) this.mContext.getSystemService(UserManager.class);
        UserManagerInternal umInternal = getUserManagerInternal();
        for (UserInfo user : um.getUsers()) {
            int flags;
            if (umInternal.isUserUnlockingOrUnlocked(user.id)) {
                flags = 3;
            } else if (umInternal.isUserRunning(user.id)) {
                flags = 1;
            }
            if (ps != null && ps.getInstalled(user.id)) {
                prepareAppDataLIF(pkg, user.id, flags);
            }
        }
    }

    private void prepareAppDataLIF(Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        prepareAppDataLeafLIF(pkg, userId, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            prepareAppDataLeafLIF((Package) pkg.childPackages.get(i), userId, flags);
        }
    }

    private void prepareAppDataAndMigrateLIF(Package pkg, int userId, int flags, boolean maybeMigrateAppData) {
        prepareAppDataLIF(pkg, userId, flags);
        if (maybeMigrateAppData && maybeMigrateAppDataLIF(pkg, userId)) {
            prepareAppDataLIF(pkg, userId, flags);
        }
    }

    private void prepareAppDataLeafLIF(Package pkg, int userId, int flags) {
        String volumeUuid = pkg.volumeUuid;
        String packageName = pkg.packageName;
        ApplicationInfo app = pkg.applicationInfo;
        int appId = UserHandle.getAppId(app.uid);
        Preconditions.checkNotNull(app.seInfo);
        long ceDataInode = -1;
        try {
            ceDataInode = this.mInstaller.createAppData(volumeUuid, packageName, userId, flags, appId, app.seInfo, app.targetSdkVersion);
        } catch (InstallerException e) {
            Slog.e(TAG, "Failed to create app data for " + packageName + ": " + e);
        }
        if (!((flags & 2) == 0 || ceDataInode == -1)) {
            synchronized (this.mPackages) {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
                if (ps != null) {
                    ps.setCeDataInode(ceDataInode, userId);
                }
            }
        }
        prepareAppDataContentsLeafLIF(pkg, userId, flags);
    }

    private void prepareAppDataContentsLIF(Package pkg, int userId, int flags) {
        if (pkg == null) {
            Slog.wtf(TAG, "Package was null!", new Throwable());
            return;
        }
        prepareAppDataContentsLeafLIF(pkg, userId, flags);
        int childCount = pkg.childPackages != null ? pkg.childPackages.size() : 0;
        for (int i = 0; i < childCount; i++) {
            prepareAppDataContentsLeafLIF((Package) pkg.childPackages.get(i), userId, flags);
        }
    }

    private void prepareAppDataContentsLeafLIF(Package pkg, int userId, int flags) {
        String volumeUuid = pkg.volumeUuid;
        String packageName = pkg.packageName;
        ApplicationInfo app = pkg.applicationInfo;
        if ((flags & 2) != 0 && app.primaryCpuAbi != null && (VMRuntime.is64BitAbi(app.primaryCpuAbi) ^ 1) != 0) {
            try {
                this.mInstaller.linkNativeLibraryDirectory(volumeUuid, packageName, app.nativeLibraryDir, userId);
            } catch (InstallerException e) {
                Slog.e(TAG, "Failed to link native for " + packageName + ": " + e);
            }
        }
    }

    private boolean maybeMigrateAppDataLIF(Package pkg, int userId) {
        if (!pkg.isSystemApp() || (StorageManager.isFileEncryptedNativeOrEmulated() ^ 1) == 0) {
            return false;
        }
        try {
            this.mInstaller.migrateAppData(pkg.volumeUuid, pkg.packageName, userId, pkg.applicationInfo.isDefaultToDeviceProtectedStorage() ? 1 : 2);
        } catch (InstallerException e) {
            logCriticalInfo(5, "Failed to migrate " + pkg.packageName + ": " + e.getMessage());
        }
        return true;
    }

    public PackageFreezer freezePackage(String packageName, String killReason) {
        return freezePackage(packageName, -1, killReason);
    }

    public PackageFreezer freezePackage(String packageName, int userId, String killReason) {
        return new PackageFreezer(packageName, userId, killReason);
    }

    public PackageFreezer freezePackageForInstall(String packageName, int installFlags, String killReason) {
        return freezePackageForInstall(packageName, -1, installFlags, killReason);
    }

    public PackageFreezer freezePackageForInstall(String packageName, int userId, int installFlags, String killReason) {
        if ((installFlags & 4096) != 0) {
            return new PackageFreezer();
        }
        return freezePackage(packageName, userId, killReason);
    }

    public PackageFreezer freezePackageForDelete(String packageName, int deleteFlags, String killReason) {
        return freezePackageForDelete(packageName, -1, deleteFlags, killReason);
    }

    public PackageFreezer freezePackageForDelete(String packageName, int userId, int deleteFlags, String killReason) {
        if ((deleteFlags & 8) != 0) {
            return new PackageFreezer();
        }
        return freezePackage(packageName, userId, killReason);
    }

    private void checkPackageFrozen(String packageName) {
        synchronized (this.mPackages) {
            if (!this.mFrozenPackages.contains(packageName)) {
                Slog.wtf(TAG, "Expected " + packageName + " to be frozen!", new Throwable());
            }
        }
    }

    public int movePackage(String packageName, String volumeUuid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
        final int callingUid = Binder.getCallingUid();
        final UserHandle user = new UserHandle(UserHandle.getUserId(callingUid));
        final int moveId = this.mNextMoveId.getAndIncrement();
        final String str = packageName;
        final String str2 = volumeUuid;
        this.mHandler.post(new Runnable() {
            public void run() {
                try {
                    PackageManagerService.this.movePackageInternal(str, str2, moveId, callingUid, user);
                } catch (PackageManagerException e) {
                    Slog.w(PackageManagerService.TAG, "Failed to move " + str, e);
                    PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(moveId, e.error);
                }
            }
        });
        return moveId;
    }

    private void movePackageInternal(String packageName, String volumeUuid, int moveId, int callingUid, UserHandle user) throws PackageManagerException {
        boolean currentAsec;
        String label;
        int installFlags;
        boolean moveCompleteApp;
        File measurePath;
        int i;
        long sizeBytes;
        StorageManager storage = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        PackageManager pm = this.mContext.getPackageManager();
        synchronized (this.mPackages) {
            String currentVolumeUuid;
            File file;
            Package pkg = (Package) this.mPackages.get(packageName);
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (!(pkg == null || ps == null)) {
                if (!filterAppAccessLPr(ps, callingUid, user.getIdentifier())) {
                    if (pkg.applicationInfo.isSystemApp()) {
                        throw new PackageManagerException(-3, "Cannot move system application");
                    }
                    boolean isInternalStorage = "private".equals(volumeUuid);
                    boolean allow3rdPartyOnInternal = this.mContext.getResources().getBoolean(17956869);
                    if (!isInternalStorage || (allow3rdPartyOnInternal ^ 1) == 0) {
                        if (pkg.applicationInfo.isExternalAsec()) {
                            currentAsec = true;
                            currentVolumeUuid = "primary_physical";
                        } else if (pkg.applicationInfo.isForwardLocked()) {
                            currentAsec = true;
                            currentVolumeUuid = "forward_locked";
                        } else {
                            currentAsec = false;
                            currentVolumeUuid = ps.volumeUuid;
                            file = new File(pkg.codePath);
                            file = new File(file, "oat");
                            if (!(file.isDirectory() && (file.isDirectory() ^ 1) == 0)) {
                                throw new PackageManagerException(-6, "Move only supported for modern cluster style installs");
                            }
                        }
                        if (Objects.equals(currentVolumeUuid, volumeUuid)) {
                            throw new PackageManagerException(-6, "Package already moved to " + volumeUuid);
                        } else if (pkg.applicationInfo.isInternal() && isPackageDeviceAdminOnAnyUser(packageName)) {
                            throw new PackageManagerException(-8, "Device admin cannot be moved");
                        } else if (this.mFrozenPackages.contains(packageName)) {
                            throw new PackageManagerException(-7, "Failed to move already frozen package");
                        } else {
                            file = new File(pkg.codePath);
                            String installerPackageName = ps.installerPackageName;
                            String packageAbiOverride = ps.cpuAbiOverrideString;
                            int appId = UserHandle.getAppId(pkg.applicationInfo.uid);
                            String seinfo = pkg.applicationInfo.seInfo;
                            label = String.valueOf(pm.getApplicationLabel(pkg.applicationInfo));
                            int targetSdkVersion = pkg.applicationInfo.targetSdkVersion;
                            PackageFreezer freezer = freezePackage(packageName, "movePackageInternal");
                            int[] installedUserIds = ps.queryInstalledUsers(sUserManager.getUserIds(), true);
                        }
                    } else {
                        throw new PackageManagerException(-9, "3rd party apps are not allowed on internal storage");
                    }
                }
            }
            throw new PackageManagerException(-2, "Missing package");
        }
        Bundle extras = new Bundle();
        extras.putString("android.intent.extra.PACKAGE_NAME", packageName);
        extras.putString("android.intent.extra.TITLE", label);
        this.mMoveCallbacks.notifyCreated(moveId, extras);
        if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeUuid)) {
            installFlags = 16;
            moveCompleteApp = currentAsec ^ 1;
            measurePath = Environment.getDataAppDirectory(volumeUuid);
        } else if (Objects.equals("primary_physical", volumeUuid)) {
            installFlags = 8;
            moveCompleteApp = false;
            measurePath = storage.getPrimaryPhysicalVolume().getPath();
        } else {
            VolumeInfo volume = storage.findVolumeByUuid(volumeUuid);
            if (this.mCustPms != null && this.mCustPms.canAppMoveToPublicSd(volume)) {
                installFlags = 8;
                moveCompleteApp = false;
                measurePath = volume.getPath();
            } else if (volume != null && volume.getType() == 1 && (volume.isMountedWritable() ^ 1) == 0) {
                Preconditions.checkState(currentAsec ^ 1);
                installFlags = 16;
                moveCompleteApp = true;
                measurePath = Environment.getDataAppDirectory(volumeUuid);
            } else {
                freezer.close();
                throw new PackageManagerException(-6, "Move location not mounted private volume");
            }
        }
        if (moveCompleteApp) {
            i = 0;
            int length = installedUserIds.length;
            while (i < length) {
                int userId = installedUserIds[i];
                if (!StorageManager.isFileEncryptedNativeOrEmulated() || (StorageManager.isUserKeyUnlocked(userId) ^ 1) == 0) {
                    i++;
                } else {
                    throw new PackageManagerException(-10, "User " + userId + " must be unlocked");
                }
            }
        }
        PackageStats packageStats = new PackageStats(null, -1);
        synchronized (this.mInstaller) {
            i = 0;
            int length2 = installedUserIds.length;
            while (i < length2) {
                if (getPackageSizeInfoLI(packageName, installedUserIds[i], packageStats)) {
                    i++;
                } else {
                    freezer.close();
                    throw new PackageManagerException(-6, "Failed to measure package size");
                }
            }
        }
        final long startFreeBytes = measurePath.getUsableSpace();
        if (moveCompleteApp) {
            sizeBytes = packageStats.codeSize + packageStats.dataSize;
        } else {
            sizeBytes = packageStats.codeSize;
        }
        if (sizeBytes > storage.getStorageBytesUntilLow(measurePath)) {
            freezer.close();
            throw new PackageManagerException(-6, "Not enough free space to move");
        }
        MoveInfo moveInfo;
        this.mMoveCallbacks.notifyStatusChanged(moveId, 10);
        final CountDownLatch installedLatch = new CountDownLatch(1);
        final PackageFreezer packageFreezer = freezer;
        final int i2 = moveId;
        IPackageInstallObserver2 anonymousClass33 = new IPackageInstallObserver2.Stub() {
            public void onUserActionRequired(Intent intent) throws RemoteException {
                throw new IllegalStateException();
            }

            public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) throws RemoteException {
                installedLatch.countDown();
                packageFreezer.close();
                switch (PackageManager.installStatusToPublicStatus(returnCode)) {
                    case 0:
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i2, -100);
                        return;
                    case 6:
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i2, -1);
                        return;
                    default:
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i2, -6);
                        return;
                }
            }
        };
        if (moveCompleteApp) {
            final int i3 = moveId;
            new Thread() {
                public void run() {
                    while (!installedLatch.await(1, TimeUnit.SECONDS)) {
                        try {
                        } catch (InterruptedException e) {
                        }
                        PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(i3, ((int) MathUtils.constrain(((startFreeBytes - measurePath.getUsableSpace()) * 80) / sizeBytes, 0, 80)) + 10);
                    }
                }
            }.start();
            moveInfo = new MoveInfo(moveId, currentVolumeUuid, volumeUuid, packageName, file.getName(), appId, seinfo, targetSdkVersion);
        } else {
            moveInfo = null;
        }
        installFlags |= 2;
        Message msg = this.mHandler.obtainMessage(5);
        InstallParams params = new InstallParams(OriginInfo.fromExistingFile(file), moveInfo, anonymousClass33, installFlags, installerPackageName, volumeUuid, null, user, packageAbiOverride, null, null, 0);
        params.setTraceMethod("movePackage").setTraceCookie(System.identityHashCode(params));
        msg.obj = params;
        Trace.asyncTraceBegin(262144, "movePackage", System.identityHashCode(msg.obj));
        Trace.asyncTraceBegin(262144, "queueInstall", System.identityHashCode(msg.obj));
        this.mHandler.sendMessage(msg);
    }

    public int movePrimaryStorage(String volumeUuid) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOVE_PACKAGE", null);
        final int realMoveId = this.mNextMoveId.getAndIncrement();
        Bundle extras = new Bundle();
        extras.putString("android.os.storage.extra.FS_UUID", volumeUuid);
        this.mMoveCallbacks.notifyCreated(realMoveId, extras);
        ((StorageManager) this.mContext.getSystemService(StorageManager.class)).setPrimaryStorageUuid(volumeUuid, new IPackageMoveObserver.Stub() {
            public void onCreated(int moveId, Bundle extras) {
            }

            public void onStatusChanged(int moveId, int status, long estMillis) {
                PackageManagerService.this.mMoveCallbacks.notifyStatusChanged(realMoveId, status, estMillis);
            }
        });
        return realMoveId;
    }

    public int getMoveStatus(int moveId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        return this.mMoveCallbacks.mLastStatus.get(moveId);
    }

    public void registerMoveCallback(IPackageMoveObserver callback) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mMoveCallbacks.register(callback);
    }

    public void unregisterMoveCallback(IPackageMoveObserver callback) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS", null);
        this.mMoveCallbacks.unregister(callback);
    }

    public boolean setInstallLocation(int loc) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", null);
        if (getInstallLocation() == loc) {
            return true;
        }
        if (loc != 0 && loc != 1 && loc != 2) {
            return false;
        }
        Global.putInt(this.mContext.getContentResolver(), "default_install_location", loc);
        return true;
    }

    public int getInstallLocation() {
        return Global.getInt(this.mContext.getContentResolver(), "default_install_location", 0);
    }

    void cleanUpUser(UserManagerService userManager, int userHandle) {
        synchronized (this.mPackages) {
            this.mDirtyUsers.remove(Integer.valueOf(userHandle));
            this.mUserNeedsBadging.delete(userHandle);
            this.mSettings.removeUserLPw(userHandle);
            this.mPendingBroadcasts.remove(userHandle);
            this.mInstantAppRegistry.onUserRemovedLPw(userHandle);
            removeUnusedPackagesLPw(userManager, userHandle);
        }
    }

    private void removeUnusedPackagesLPw(UserManagerService userManager, final int userHandle) {
        int[] users = userManager.getUserIds();
        for (PackageSetting ps : this.mSettings.mPackages.values()) {
            if (ps.pkg != null) {
                final String packageName = ps.pkg.packageName;
                if ((ps.pkgFlags & 1) != 0) {
                    boolean isPreRemovable = ps.pkg.applicationInfo != null ? (ps.pkg.applicationInfo.hwFlags & 33554432) == 0 ? (ps.pkg.applicationInfo.hwFlags & 67108864) != 0 : true : false;
                    if (!isPreRemovable) {
                    }
                }
                boolean keep = shouldKeepUninstalledPackageLPr(packageName);
                if (!keep) {
                    int i = 0;
                    while (i < users.length) {
                        if (users[i] != userHandle && ps.getInstalled(users[i])) {
                            keep = true;
                            break;
                        }
                        i++;
                    }
                }
                if (!keep) {
                    this.mHandler.post(new Runnable() {
                        public void run() {
                            PackageManagerService.this.deletePackageX(packageName, -1, userHandle, 0);
                        }
                    });
                }
            }
        }
    }

    void createNewUser(int userId, String[] disallowedPackages) {
        synchronized (this.mInstallLock) {
            this.mSettings.createNewUserLI(this, this.mInstaller, userId, disallowedPackages);
        }
        synchronized (this.mPackages) {
            scheduleWritePackageRestrictionsLocked(userId);
            scheduleWritePackageListLocked(userId);
            applyFactoryDefaultBrowserLPw(userId);
            primeDomainVerificationsLPw(userId);
        }
    }

    void onNewUserCreated(int userId) {
        this.mDefaultPermissionPolicy.grantDefaultPermissions(userId);
        if (this.mPermissionReviewRequired) {
            updatePermissionsLPw(null, null, 5);
        }
    }

    public VerifierDeviceIdentity getVerifierDeviceIdentity() throws RemoteException {
        VerifierDeviceIdentity verifierDeviceIdentityLPw;
        this.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_VERIFICATION_AGENT", "Only package verification agents can read the verifier device identity");
        synchronized (this.mPackages) {
            verifierDeviceIdentityLPw = this.mSettings.getVerifierDeviceIdentityLPw();
        }
        return verifierDeviceIdentityLPw;
    }

    public void setPermissionEnforced(String permission, boolean enforced) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.GRANT_RUNTIME_PERMISSIONS", "setPermissionEnforced");
        if ("android.permission.READ_EXTERNAL_STORAGE".equals(permission)) {
            synchronized (this.mPackages) {
                if (this.mSettings.mReadExternalStorageEnforced == null || this.mSettings.mReadExternalStorageEnforced.booleanValue() != enforced) {
                    this.mSettings.mReadExternalStorageEnforced = Boolean.valueOf(enforced);
                    this.mSettings.writeLPr();
                }
            }
            IActivityManager am = ActivityManager.getService();
            if (am != null) {
                long token = Binder.clearCallingIdentity();
                try {
                    am.killProcessesBelowForeground("setPermissionEnforcement");
                } catch (RemoteException e) {
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
                return;
            }
            return;
        }
        throw new IllegalArgumentException("No selective enforcement for " + permission);
    }

    @Deprecated
    public boolean isPermissionEnforced(String permission) {
        return true;
    }

    public boolean isStorageLow() {
        long token = Binder.clearCallingIdentity();
        try {
            DeviceStorageMonitorInternal dsm = (DeviceStorageMonitorInternal) LocalServices.getService(DeviceStorageMonitorInternal.class);
            if (dsm != null) {
                boolean isMemoryLow = dsm.isMemoryLow();
                return isMemoryLow;
            }
            Binder.restoreCallingIdentity(token);
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public IPackageInstaller getPackageInstaller() {
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        return this.mInstallerService;
    }

    private boolean userNeedsBadging(int userId) {
        int index = this.mUserNeedsBadging.indexOfKey(userId);
        if (index >= 0) {
            return this.mUserNeedsBadging.valueAt(index);
        }
        long token = Binder.clearCallingIdentity();
        try {
            boolean b;
            UserInfo userInfo = sUserManager.getUserInfo(userId);
            if (userInfo == null || !userInfo.isManagedProfile()) {
                b = false;
            } else {
                b = true;
            }
            this.mUserNeedsBadging.put(userId, b);
            return b;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public KeySet getKeySetByAlias(String packageName, String alias) {
        if (packageName == null || alias == null) {
            return null;
        }
        KeySet keySet;
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            } else if (filterAppAccessLPr(pkg.mExtras, Binder.getCallingUid(), UserHandle.getCallingUserId())) {
                Slog.w(TAG, "KeySet requested for filtered package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            } else {
                keySet = new KeySet(this.mSettings.mKeySetManagerService.getKeySetByAliasAndPackageNameLPr(packageName, alias));
            }
        }
        return keySet;
    }

    public KeySet getSigningKeySet(String packageName) {
        if (packageName == null) {
            return null;
        }
        KeySet keySet;
        synchronized (this.mPackages) {
            int callingUid = Binder.getCallingUid();
            int callingUserId = UserHandle.getUserId(callingUid);
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            } else if (filterAppAccessLPr(pkg.mExtras, callingUid, callingUserId)) {
                Slog.w(TAG, "KeySet requested for filtered package: " + packageName + ", uid:" + callingUid);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            } else if (pkg.applicationInfo.uid == callingUid || 1000 == callingUid) {
                keySet = new KeySet(this.mSettings.mKeySetManagerService.getSigningKeySetByPackageNameLPr(packageName));
            } else {
                throw new SecurityException("May not access signing KeySet of other apps.");
            }
        }
        return keySet;
    }

    public boolean isPackageSignedByKeySet(String packageName, KeySet ks) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null || packageName == null || ks == null) {
            return false;
        }
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null || filterAppAccessLPr((PackageSetting) pkg.mExtras, callingUid, UserHandle.getUserId(callingUid))) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            IBinder ksh = ks.getToken();
            if (ksh instanceof KeySetHandle) {
                boolean packageIsSignedByLPr = this.mSettings.mKeySetManagerService.packageIsSignedByLPr(packageName, (KeySetHandle) ksh);
                return packageIsSignedByLPr;
            }
            return false;
        }
    }

    public boolean isPackageSignedByKeySetExactly(String packageName, KeySet ks) {
        int callingUid = Binder.getCallingUid();
        if (getInstantAppPackageName(callingUid) != null || packageName == null || ks == null) {
            return false;
        }
        synchronized (this.mPackages) {
            Package pkg = (Package) this.mPackages.get(packageName);
            if (pkg == null || filterAppAccessLPr((PackageSetting) pkg.mExtras, callingUid, UserHandle.getUserId(callingUid))) {
                Slog.w(TAG, "KeySet requested for unknown package: " + packageName);
                throw new IllegalArgumentException("Unknown package: " + packageName);
            }
            IBinder ksh = ks.getToken();
            if (ksh instanceof KeySetHandle) {
                boolean packageIsSignedByExactlyLPr = this.mSettings.mKeySetManagerService.packageIsSignedByExactlyLPr(packageName, (KeySetHandle) ksh);
                return packageIsSignedByExactlyLPr;
            }
            return false;
        }
    }

    private static String getParam(String params, String prefix, String separator) {
        int left = params.indexOf(prefix);
        if (left < 0) {
            Log.e(TAG, params + " not contains " + prefix);
            return null;
        }
        left += prefix.length();
        int right = params.indexOf(separator, left);
        if (right >= 0) {
            return params.substring(left, right);
        }
        Log.e(TAG, params + " not contains " + separator);
        return null;
    }

    private static void saveDex2oatList(List<String> list) {
        IOException e;
        Throwable th;
        BufferedWriter bufferedWriter = null;
        try {
            BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/data/system/dex2oat.list"), "UTF-8"));
            try {
                for (String i : list) {
                    fout.write(i + "\n");
                }
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException e3) {
                e = e3;
                bufferedWriter = fout;
                try {
                    Log.e(TAG, "saveDex2oatList error: ", e);
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e4) {
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e5) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                bufferedWriter = fout;
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                throw th;
            }
        } catch (IOException e6) {
            e = e6;
            Log.e(TAG, "saveDex2oatList error: ", e);
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
    }

    private void deletePackageIfUnusedLPr(final String packageName) {
        PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
        if (!(ps == null || ps.isAnyInstalled(sUserManager.getUserIds()))) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    PackageManagerService.this.deletePackageX(packageName, -1, 0, 2);
                }
            });
        }
    }

    private static void checkDowngrade(Package before, PackageInfoLite after) throws PackageManagerException {
        if (after.versionCode < before.mVersionCode) {
            throw new PackageManagerException(-25, "Update version code " + after.versionCode + " is older than current " + before.mVersionCode);
        } else if (after.versionCode != before.mVersionCode) {
        } else {
            if (after.baseRevisionCode < before.baseRevisionCode) {
                throw new PackageManagerException(-25, "Update base revision code " + after.baseRevisionCode + " is older than current " + before.baseRevisionCode);
            } else if (!ArrayUtils.isEmpty(after.splitNames)) {
                int i = 0;
                while (i < after.splitNames.length) {
                    String splitName = after.splitNames[i];
                    int j = ArrayUtils.indexOf(before.splitNames, splitName);
                    if (j == -1 || after.splitRevisionCodes[i] >= before.splitRevisionCodes[j]) {
                        i++;
                    } else {
                        throw new PackageManagerException(-25, "Update split " + splitName + " revision code " + after.splitRevisionCodes[i] + " is older than current " + before.splitRevisionCodes[j]);
                    }
                }
            }
        }
    }

    public void grantDefaultPermissionsToEnabledCarrierApps(String[] packageNames, int userId) {
        enforceSystemOrPhoneCaller("grantPermissionsToEnabledCarrierApps");
        synchronized (this.mPackages) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToEnabledCarrierAppsLPr(packageNames, userId);
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    public void grantDefaultPermissionsToEnabledImsServices(String[] packageNames, int userId) {
        enforceSystemOrPhoneCaller("grantDefaultPermissionsToEnabledImsServices");
        synchronized (this.mPackages) {
            long identity = Binder.clearCallingIdentity();
            try {
                this.mDefaultPermissionPolicy.grantDefaultPermissionsToEnabledImsServicesLPr(packageNames, userId);
                Binder.restoreCallingIdentity(identity);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private static void enforceSystemOrPhoneCaller(String tag) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 1001 && callingUid != 1000) {
            throw new SecurityException("Cannot call " + tag + " from UID " + callingUid);
        }
    }

    boolean isHistoricalPackageUsageAvailable() {
        return this.mPackageUsage.isHistoricalPackageUsageAvailable();
    }

    Collection<Package> getPackages() {
        Collection arrayList;
        synchronized (this.mPackages) {
            arrayList = new ArrayList(this.mPackages.values());
        }
        return arrayList;
    }

    public void logAppProcessStartIfNeeded(String processName, int uid, String seinfo, String apkFile, int pid) {
        if (getInstantAppPackageName(Binder.getCallingUid()) == null && SecurityLog.isLoggingEnabled()) {
            Bundle data = new Bundle();
            data.putLong("startTimestamp", System.currentTimeMillis());
            data.putString("processName", processName);
            data.putInt("uid", uid);
            data.putString("seinfo", seinfo);
            data.putString("apkFile", apkFile);
            data.putInt("pid", pid);
            Message msg = this.mProcessLoggingHandler.obtainMessage(1);
            msg.setData(data);
            this.mProcessLoggingHandler.sendMessage(msg);
        }
    }

    public PackageStats getCompilerPackageStats(String pkgName) {
        return this.mCompilerStats.getPackageStats(pkgName);
    }

    public PackageStats getOrCreateCompilerPackageStats(Package pkg) {
        return getOrCreateCompilerPackageStats(pkg.packageName);
    }

    public PackageStats getOrCreateCompilerPackageStats(String pkgName) {
        return this.mCompilerStats.getOrCreatePackageStats(pkgName);
    }

    public void deleteCompilerPackageStats(String pkgName) {
        this.mCompilerStats.deletePackageStats(pkgName);
    }

    public int getInstallReason(String packageName, int userId) {
        int callingUid = Binder.getCallingUid();
        enforceCrossUserPermission(callingUid, userId, true, false, "get install reason");
        synchronized (this.mPackages) {
            PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(packageName);
            if (filterAppAccessLPr(ps, callingUid, userId)) {
                return 0;
            } else if (ps != null) {
                int installReason = ps.getInstallReason(userId);
                return installReason;
            } else {
                return 0;
            }
        }
    }

    public boolean canRequestPackageInstalls(String packageName, int userId) {
        return canRequestPackageInstallsInternal(packageName, 0, userId, true);
    }

    private boolean canRequestPackageInstallsInternal(String packageName, int flags, int userId, boolean throwIfPermNotDeclared) {
        boolean z = true;
        int callingUid = Binder.getCallingUid();
        int uid = getPackageUid(packageName, 0, userId);
        if (callingUid == uid || callingUid == 0 || callingUid == 1000) {
            ApplicationInfo info = getApplicationInfo(packageName, flags, userId);
            if (info == null || info.targetSdkVersion < 26) {
                return false;
            }
            String appOpPermission = "android.permission.REQUEST_INSTALL_PACKAGES";
            if (ArrayUtils.contains(getAppOpPermissionPackages(appOpPermission), packageName)) {
                if (sUserManager.hasUserRestriction("no_install_unknown_sources", userId)) {
                    return false;
                }
                if (this.mExternalSourcesPolicy != null) {
                    int isTrusted = this.mExternalSourcesPolicy.getPackageTrustedToInstallApps(packageName, uid);
                    if (isTrusted != 2) {
                        if (isTrusted != 0) {
                            z = false;
                        }
                        return z;
                    }
                }
                if (checkUidPermission(appOpPermission, uid) != 0) {
                    z = false;
                }
                return z;
            } else if (throwIfPermNotDeclared) {
                throw new SecurityException("Need to declare " + appOpPermission + " to call this api");
            } else {
                Slog.e(TAG, "Need to declare " + appOpPermission + " to call this api");
                return false;
            }
        }
        throw new SecurityException("Caller uid " + callingUid + " does not own package " + packageName);
    }

    private void parseInstalledPkgInfo(InstallArgs args, PackageInstalledInfo res) {
        StringBuilder pkgPath = new StringBuilder(100);
        int pkgInstallResult = 0;
        int pkgVersionCode = 0;
        String pkgVersionName = "";
        String pkgName = "";
        boolean pkgUpdate = false;
        if (!(args == null || args.origin == null || args.origin.file == null)) {
            pkgPath.append(args.origin.file.toString()).append(";");
        }
        if (!(args == null || args.installerPackageName == null)) {
            pkgPath.append(args.installerPackageName);
        }
        if (res != null) {
            pkgInstallResult = res.returnCode;
            if (res.pkg != null) {
                pkgVersionCode = res.pkg.mVersionCode;
                pkgVersionName = res.pkg.mVersionName;
                if (res.pkg.applicationInfo != null) {
                    pkgName = res.pkg.applicationInfo.packageName;
                }
            }
            if (res.removedInfo != null) {
                pkgUpdate = res.removedInfo.removedPackage != null;
            }
        }
        parseInstalledPkgInfo(pkgPath.toString(), pkgName, pkgVersionName, pkgVersionCode, pkgInstallResult, pkgUpdate);
    }

    protected boolean isAppInstallAllowed(String installer, String appName) {
        return true;
    }

    protected boolean isUnAppInstallAllowed(String originPath) {
        return false;
    }

    public void loadSysWhitelist() {
    }

    public void checkIllegalSysApk(Package pkg, int hwFlags) throws PackageManagerException {
    }

    protected void addGrantedInstalledPkg(String pkgName, boolean grant) {
    }

    protected Signature[] getRealSignature(Package pkg) {
        if (pkg == null) {
            return new Signature[0];
        }
        return pkg.mRealSignatures;
    }

    protected void setRealSignature(Package pkg, Signature[] sign) {
        if (pkg != null) {
            pkg.mRealSignatures = sign;
        }
    }

    private void uploadInstallErrRadar(String reason) {
        Bundle data = new Bundle();
        data.putString("package", "PMS");
        data.putString(HwBroadcastRadarUtil.KEY_VERSION_NAME, "0");
        data.putString("extra", reason);
        if (this.mMonitor != null) {
            this.mMonitor.monitor(907400000, data);
        }
    }

    private void writeNetQinFlag(String pkgName) {
        if ("com.nqmobile.antivirus20.hw".equalsIgnoreCase(pkgName)) {
            File file = new File(new File(Environment.getDataDirectory(), "system"), "netqin.tmp");
            synchronized (this.mPackages) {
                if (file.exists()) {
                    return;
                }
                try {
                    if (file.createNewFile()) {
                        FileUtils.setPermissions(file.getPath(), 416, -1, -1);
                        Log.i(TAG, "Create netqin flag successfully");
                    }
                } catch (IOException e) {
                    Log.i(TAG, "Fail to create netqin flag");
                }
            }
        } else {
            return;
        }
    }

    public static String getCallingAppName(Context context, Package pkg) {
        PackageManager pm = context.getPackageManager();
        String displayName = pkg.packageName;
        if (pm != null) {
            return String.valueOf(pm.getApplicationLabel(pkg.applicationInfo));
        }
        return displayName;
    }

    private void connectBootAnimation() {
        IBinder binder = ServiceManager.getService("BootAnimationBinderServer");
        if (binder != null) {
            this.mIBootAnmation = IBootAnmation.Stub.asInterface(binder);
        } else {
            Slog.w(TAG, "BootAnimationBinderServer not found; can not display dexoat process!");
        }
    }

    public ComponentName getInstantAppResolverSettingsComponent() {
        return this.mInstantAppResolverSettingsComponent;
    }

    public ComponentName getInstantAppInstallerComponent() {
        ComponentName componentName = null;
        if (getInstantAppPackageName(Binder.getCallingUid()) != null) {
            return null;
        }
        if (this.mInstantAppInstallerActivity != null) {
            componentName = this.mInstantAppInstallerActivity.getComponentName();
        }
        return componentName;
    }

    public String getInstantAppAndroidId(String packageName, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_INSTANT_APPS", "getInstantAppAndroidId");
        enforceCrossUserPermission(Binder.getCallingUid(), userId, true, false, "getInstantAppAndroidId");
        if (!isInstantApp(packageName, userId)) {
            return null;
        }
        String instantAppAndroidIdLPw;
        synchronized (this.mPackages) {
            instantAppAndroidIdLPw = this.mInstantAppRegistry.getInstantAppAndroidIdLPw(packageName, userId);
        }
        return instantAppAndroidIdLPw;
    }

    boolean canHaveOatDir(String packageName) {
        synchronized (this.mPackages) {
            Package p = (Package) this.mPackages.get(packageName);
            if (p == null) {
                return false;
            }
            boolean canHaveOatDir = p.canHaveOatDir();
            return canHaveOatDir;
        }
    }

    private String getOatDir(Package pkg) {
        if (!pkg.canHaveOatDir()) {
            return null;
        }
        File codePath = new File(pkg.codePath);
        if (codePath.isDirectory()) {
            return PackageDexOptimizer.getOatDir(codePath).getAbsolutePath();
        }
        return null;
    }

    void deleteOatArtifactsOfPackage(String packageName) {
        Package pkg;
        synchronized (this.mPackages) {
            pkg = (Package) this.mPackages.get(packageName);
        }
        String[] instructionSets = InstructionSets.getAppDexInstructionSets(pkg.applicationInfo);
        List<String> codePaths = pkg.getAllCodePaths();
        String oatDir = getOatDir(pkg);
        for (String codePath : codePaths) {
            for (String isa : instructionSets) {
                try {
                    this.mInstaller.deleteOdex(codePath, isa, oatDir);
                } catch (InstallerException e) {
                    Log.e(TAG, "Failed deleting oat files for " + codePath, e);
                }
            }
        }
    }

    Set<String> getUnusedPackages(long downgradeTimeThresholdMillis) {
        Set<String> unusedPackages = new HashSet();
        long currentTimeInMillis = System.currentTimeMillis();
        synchronized (this.mPackages) {
            for (Package pkg : this.mPackages.values()) {
                PackageSetting ps = (PackageSetting) this.mSettings.mPackages.get(pkg.packageName);
                if (ps != null) {
                    if (PackageManagerServiceUtils.isUnusedSinceTimeInMillis(ps.firstInstallTime, currentTimeInMillis, downgradeTimeThresholdMillis, getDexManager().getPackageUseInfoOrDefault(pkg.packageName), pkg.getLatestPackageUseTimeInMills(), pkg.getLatestForegroundPackageUseTimeInMills())) {
                        unusedPackages.add(pkg.packageName);
                    }
                }
            }
        }
        return unusedPackages;
    }

    public ArrayMap<String, Package> getPackagesLock() {
        return this.mPackages;
    }

    public Settings getSettings() {
        return this.mSettings;
    }

    public IBinder getHwInnerService() {
        return this.mHwInnerService;
    }

    private long printClearDataTimeoutLogs(String type, long start, String packageName) {
        long end = SystemClock.uptimeMillis();
        if (end - start > 1000) {
            Slog.i(TAG, "printClearDataTimeoutLogs type: " + type + ",cost: " + (end - start) + "ms for packageName: " + packageName);
        }
        return end;
    }
}
