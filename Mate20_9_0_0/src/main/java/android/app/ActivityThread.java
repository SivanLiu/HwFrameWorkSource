package android.app;

import android.app.IApplicationThread.Stub;
import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.app.backup.BackupAgent;
import android.app.job.JobInfo;
import android.app.servertransaction.ActivityLifecycleItem;
import android.app.servertransaction.ActivityRelaunchItem;
import android.app.servertransaction.ActivityResultItem;
import android.app.servertransaction.ClientTransaction;
import android.app.servertransaction.PendingTransactionActions;
import android.app.servertransaction.PendingTransactionActions.StopInfo;
import android.app.servertransaction.TransactionExecutor;
import android.app.servertransaction.TransactionExecutorHelper;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.BroadcastReceiver.PendingResult;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.IContentProvider;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentProto;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ApplicationInfoProto.Detail;
import android.content.pm.IPackageManager;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ParceledListSlice;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.contentsensor.ContentSensorManagerFactory;
import android.contentsensor.IContentSensorManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDebug;
import android.database.sqlite.SQLiteDebug.DbStats;
import android.database.sqlite.SQLiteDebug.PagerStats;
import android.ddm.DdmHandleAppName;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.hardware.display.DisplayManagerGlobal;
import android.hwtheme.HwThemeManager;
import android.iawareperf.UniPerf;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Proxy;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.GraphicsEnvironment;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.FontsContract;
import android.renderscript.RenderScriptCacheDir;
import android.scrollerboost.ScrollerBoostManager;
import android.security.NetworkSecurityPolicy;
import android.security.net.config.NetworkSecurityConfigProvider;
import android.util.AndroidRuntimeException;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.HwVRUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.LogException;
import android.util.MergedConfiguration;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.SuperNotCalledException;
import android.util.proto.ProtoOutputStream;
import android.view.Choreographer;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.ThreadedRenderer;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewManager;
import android.view.ViewRootImpl;
import android.view.ViewRootImpl.ActivityConfigCallback;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerGlobal;
import android.webkit.WebView;
import android.zrhung.IZrHung;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.content.ReferrerIntent;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.RuntimeInit;
import com.android.internal.os.SomeArgs;
import com.android.internal.policy.AbsWindow;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.org.conscrypt.OpenSSLSocketImpl;
import com.android.org.conscrypt.TrustedCertificateStore;
import com.huawei.pgmng.common.Utils;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.CloseGuard;
import dalvik.system.VMDebug;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import libcore.io.DropBox;
import libcore.io.DropBox.Reporter;
import libcore.io.EventLogger;
import libcore.io.IoUtils;
import libcore.net.event.NetworkEventDispatcher;
import org.apache.harmony.dalvik.ddmc.DdmVmInternal;

public final class ActivityThread extends ClientTransactionHandler {
    private static final int ACTIVITY_PROCESS_ARGS = 3;
    private static final int ACTIVITY_THREAD_CHECKIN_VERSION = 4;
    private static final boolean DEBUG_BACKUP = false;
    public static final boolean DEBUG_BROADCAST = DEBUG_HW_BROADCAST;
    public static final boolean DEBUG_CONFIGURATION = DEBUG_HW_ACTIVITY;
    static final boolean DEBUG_HW_ACTIVITY = ams_log_switch.contains(Context.ACTIVITY_SERVICE);
    static final boolean DEBUG_HW_BROADCAST = ams_log_switch.contains("broadcast");
    static final boolean DEBUG_HW_PROVIDER = ams_log_switch.contains("provider");
    static final boolean DEBUG_HW_SERVICE = ams_log_switch.contains(Notification.CATEGORY_SERVICE);
    public static final boolean DEBUG_MEMORY_TRIM = false;
    static final boolean DEBUG_MESSAGES = false;
    public static final boolean DEBUG_ORDER = DEBUG_HW_ACTIVITY;
    private static final boolean DEBUG_PROVIDER = DEBUG_HW_PROVIDER;
    private static final boolean DEBUG_RESULTS = DEBUG_HW_ACTIVITY;
    private static final boolean DEBUG_SERVICE = DEBUG_HW_SERVICE;
    private static long HANDLER_BINDER_DURATION_TIME = JobInfo.MIN_BACKOFF_MILLIS;
    private static final String HEAP_COLUMN = "%13s %8s %8s %8s %8s %8s %8s %8s";
    private static final String HEAP_FULL_COLUMN = "%13s %8s %8s %8s %8s %8s %8s %8s %8s %8s %8s";
    public static final long INVALID_PROC_STATE_SEQ = -1;
    static final boolean IS_DEBUG_VERSION = (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3);
    private static final long MIN_TIME_BETWEEN_GCS = 5000;
    private static final String ONE_COUNT_COLUMN = "%21s %8d";
    private static final String ONE_COUNT_COLUMN_HEADER = "%21s %8s";
    private static final int OTHER_PROCESS_ARGS = 1;
    public static final String PROC_START_SEQ_IDENT = "seq=";
    private static final boolean REPORT_TO_ACTIVITY = true;
    public static final int SERVICE_DONE_EXECUTING_ANON = 0;
    public static final int SERVICE_DONE_EXECUTING_START = 1;
    public static final int SERVICE_DONE_EXECUTING_STOP = 2;
    private static final int SQLITE_MEM_RELEASED_EVENT_LOG_TAG = 75003;
    public static final String TAG = "ActivityThread";
    private static final Config THUMBNAIL_FORMAT = Config.RGB_565;
    private static final String TWO_COUNT_COLUMNS = "%21s %8d %21s %8d";
    private static boolean USE_CACHE = SystemProperties.getBoolean("persist.sys.freqinfo.cache", true);
    static final String ams_log_switch = SystemProperties.get("ro.config.hw_ams_log", "");
    static final boolean localLOGV = DEBUG_HW_ACTIVITY;
    private static boolean mChangedFont = false;
    private static final Object mPreloadLock = new Object();
    static IContentSensorManager sContentSensorManager = null;
    private static volatile ActivityThread sCurrentActivityThread;
    private static final ThreadLocal<Intent> sCurrentBroadcastIntent = new ThreadLocal();
    static volatile Handler sMainThreadHandler;
    static volatile IPackageManager sPackageManager;
    final ArrayMap<IBinder, ActivityClientRecord> mActivities = new ArrayMap();
    final ArrayList<Application> mAllApplications = new ArrayList();
    final ApplicationThread mAppThread = new ApplicationThread(this, null);
    final ArrayMap<String, BackupAgent> mBackupAgents = new ArrayMap();
    AppBindData mBoundApplication;
    Configuration mCompatConfiguration;
    Configuration mConfiguration;
    Bundle mCoreSettings = null;
    int mCurDefaultDisplayDpi;
    String mCurrentActivity = null;
    boolean mDensityCompatMode;
    int mDisplayId = 0;
    final Executor mExecutor = new HandlerExecutor(this.mH);
    final GcIdler mGcIdler = new GcIdler();
    boolean mGcIdlerScheduled = false;
    @GuardedBy("mGetProviderLocks")
    final ArrayMap<ProviderKey, Object> mGetProviderLocks = new ArrayMap();
    final H mH = new H();
    boolean mHiddenApiWarningShown = false;
    Application mInitialApplication;
    Instrumentation mInstrumentation;
    String mInstrumentationAppDir = null;
    String mInstrumentationLibDir = null;
    String mInstrumentationPackageName = null;
    String[] mInstrumentationSplitAppDirs = null;
    String mInstrumentedAppDir = null;
    String mInstrumentedLibDir = null;
    String[] mInstrumentedSplitAppDirs = null;
    private boolean mIsNeedStartUiProbe = false;
    boolean mJitEnabled = false;
    ArrayList<WeakReference<AssistStructure>> mLastAssistStructures = new ArrayList();
    private int mLastSessionId;
    final ArrayMap<IBinder, ProviderClientRecord> mLocalProviders = new ArrayMap();
    final ArrayMap<ComponentName, ProviderClientRecord> mLocalProvidersByName = new ArrayMap();
    final Looper mLooper = Looper.myLooper();
    private Configuration mMainThreadConfig = new Configuration();
    @GuardedBy("mNetworkPolicyLock")
    private long mNetworkBlockSeq = -1;
    private final Object mNetworkPolicyLock = new Object();
    ActivityClientRecord mNewActivities = null;
    int mNumVisibleActivities = 0;
    final ArrayMap<Activity, ArrayList<OnActivityPausedListener>> mOnPauseListeners = new ArrayMap();
    Configuration mOverrideConfig = null;
    @GuardedBy("mResourcesManager")
    final ArrayMap<String, WeakReference<LoadedApk>> mPackages = new ArrayMap();
    @GuardedBy("mResourcesManager")
    Configuration mPendingConfiguration = null;
    private PreloadThreadHandler mPreloadHandler;
    private HandlerThread mPreloadHandlerThread;
    Profiler mProfiler;
    final ArrayMap<ProviderKey, ProviderClientRecord> mProviderMap = new ArrayMap();
    final ArrayMap<IBinder, ProviderRefCount> mProviderRefCountMap = new ArrayMap();
    @GuardedBy("mResourcesManager")
    final ArrayList<ActivityClientRecord> mRelaunchingActivities = new ArrayList();
    @GuardedBy("mResourcesManager")
    final ArrayMap<String, WeakReference<LoadedApk>> mResourcePackages = new ArrayMap();
    private final ResourcesManager mResourcesManager = ResourcesManager.getInstance();
    final ArrayMap<IBinder, Service> mServices = new ArrayMap();
    boolean mSomeActivitiesChanged = false;
    private volatile ContextImpl mSystemContext;
    boolean mSystemThread = false;
    private volatile ContextImpl mSystemUiContext;
    private final TransactionExecutor mTransactionExecutor = new TransactionExecutor(this);
    boolean mUpdatingSystemConfig = false;
    private IZrHung mZrHungAppEyeUiProbe = HwFrameworkFactory.getZrHung("appeye_uiprobe");

    public static final class ActivityClientRecord {
        Activity activity;
        ActivityInfo activityInfo;
        CompatibilityInfo compatInfo;
        ActivityConfigCallback configCallback;
        Configuration createdConfig;
        String embeddedID;
        boolean hideForNow;
        int ident;
        Intent intent;
        public final boolean isForward;
        NonConfigurationInstances lastNonConfigurationInstances;
        private int mLifecycleState;
        Window mPendingRemoveWindow;
        WindowManager mPendingRemoveWindowManager;
        boolean mPreserveWindow;
        Configuration newConfig;
        ActivityClientRecord nextIdle;
        Configuration overrideConfig;
        public LoadedApk packageInfo;
        Activity parent;
        boolean paused;
        int pendingConfigChanges;
        List<ReferrerIntent> pendingIntents;
        List<ResultInfo> pendingResults;
        PersistableBundle persistentState;
        ProfilerInfo profilerInfo;
        String referrer;
        boolean startsNotResumed;
        Bundle state;
        boolean stopped;
        private Configuration tmpConfig;
        public IBinder token;
        IVoiceInteractor voiceInteractor;
        Window window;

        @VisibleForTesting
        public ActivityClientRecord() {
            this.tmpConfig = new Configuration();
            this.mLifecycleState = 0;
            this.isForward = false;
            init();
        }

        public ActivityClientRecord(IBinder token, Intent intent, int ident, ActivityInfo info, Configuration overrideConfig, CompatibilityInfo compatInfo, String referrer, IVoiceInteractor voiceInteractor, Bundle state, PersistableBundle persistentState, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents, boolean isForward, ProfilerInfo profilerInfo, ClientTransactionHandler client) {
            CompatibilityInfo compatibilityInfo = compatInfo;
            this.tmpConfig = new Configuration();
            this.mLifecycleState = 0;
            this.token = token;
            this.ident = ident;
            this.intent = intent;
            this.referrer = referrer;
            this.voiceInteractor = voiceInteractor;
            this.activityInfo = info;
            this.compatInfo = compatibilityInfo;
            this.state = state;
            this.persistentState = persistentState;
            this.pendingResults = pendingResults;
            this.pendingIntents = pendingNewIntents;
            this.isForward = isForward;
            this.profilerInfo = profilerInfo;
            this.overrideConfig = overrideConfig;
            this.packageInfo = client.getPackageInfoNoCheck(this.activityInfo.applicationInfo, compatibilityInfo);
            init();
        }

        private void init() {
            this.parent = null;
            this.embeddedID = null;
            this.paused = false;
            this.stopped = false;
            this.hideForNow = false;
            this.nextIdle = null;
            this.configCallback = new -$$Lambda$ActivityThread$ActivityClientRecord$HOrG1qglSjSUHSjKBn2rXtX0gGg(this);
        }

        public static /* synthetic */ void lambda$init$0(ActivityClientRecord activityClientRecord, Configuration overrideConfig, int newDisplayId) {
            if (activityClientRecord.activity != null) {
                if (activityClientRecord.activity.getPackageName() != null && activityClientRecord.activity.getPackageName().contains("launcher") && activityClientRecord.activity.getLocalClassName() != null && (activityClientRecord.activity.getLocalClassName().contains("UniHomeLauncher") || activityClientRecord.activity.getLocalClassName().contains("DrawerLauncher") || activityClientRecord.activity.getLocalClassName().contains("NewSimpleLauncher"))) {
                    Display display = activityClientRecord.activity.getDisplay();
                    if (display != null && ((overrideConfig.orientation == 1 && display.getHeight() < display.getWidth()) || (overrideConfig.orientation == 2 && display.getHeight() > display.getWidth()))) {
                        String str = ActivityThread.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("abnormal configuration for launcher ");
                        stringBuilder.append(overrideConfig.orientation);
                        stringBuilder.append(" ");
                        stringBuilder.append(display.getHeight());
                        stringBuilder.append(" ");
                        stringBuilder.append(display.getWidth());
                        Log.d(str, stringBuilder.toString());
                        return;
                    }
                }
                activityClientRecord.activity.mMainThread.handleActivityConfigurationChanged(activityClientRecord.token, overrideConfig, newDisplayId);
                return;
            }
            throw new IllegalStateException("Received config update for non-existing activity");
        }

        public int getLifecycleState() {
            return this.mLifecycleState;
        }

        public void setState(int newLifecycleState) {
            this.mLifecycleState = newLifecycleState;
            switch (this.mLifecycleState) {
                case 1:
                    this.paused = true;
                    this.stopped = true;
                    return;
                case 2:
                    this.paused = true;
                    this.stopped = false;
                    return;
                case 3:
                    this.paused = false;
                    this.stopped = false;
                    return;
                case 4:
                    this.paused = true;
                    this.stopped = false;
                    return;
                case 5:
                    this.paused = true;
                    this.stopped = true;
                    return;
                default:
                    return;
            }
        }

        private boolean isPreHoneycomb() {
            return this.activity != null && this.activity.getApplicationInfo().targetSdkVersion < 11;
        }

        private boolean isPreP() {
            return this.activity != null && this.activity.getApplicationInfo().targetSdkVersion < 28;
        }

        public boolean isPersistable() {
            return this.activityInfo.persistableMode == 2;
        }

        public boolean isVisibleFromServer() {
            return this.activity != null && this.activity.mVisibleFromServer;
        }

        public String toString() {
            ComponentName componentName = this.intent != null ? this.intent.getComponent() : null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ActivityRecord{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" token=");
            stringBuilder.append(this.token);
            stringBuilder.append(" ");
            stringBuilder.append(componentName == null ? "no component name" : componentName.toShortString());
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        public String getStateString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ActivityClientRecord{");
            sb.append("paused=");
            sb.append(this.paused);
            sb.append(", stopped=");
            sb.append(this.stopped);
            sb.append(", hideForNow=");
            sb.append(this.hideForNow);
            sb.append(", startsNotResumed=");
            sb.append(this.startsNotResumed);
            sb.append(", isForward=");
            sb.append(this.isForward);
            sb.append(", pendingConfigChanges=");
            sb.append(this.pendingConfigChanges);
            sb.append(", preserveWindow=");
            sb.append(this.mPreserveWindow);
            if (this.activity != null) {
                sb.append(", Activity{");
                sb.append("resumed=");
                sb.append(this.activity.mResumed);
                sb.append(", stopped=");
                sb.append(this.activity.mStopped);
                sb.append(", finished=");
                sb.append(this.activity.isFinishing());
                sb.append(", destroyed=");
                sb.append(this.activity.isDestroyed());
                sb.append(", startedActivity=");
                sb.append(this.activity.mStartedActivity);
                sb.append(", temporaryPause=");
                sb.append(this.activity.mTemporaryPause);
                sb.append(", changingConfigurations=");
                sb.append(this.activity.mChangingConfigurations);
                sb.append("}");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    static final class AppBindData {
        ApplicationInfo appInfo;
        boolean autofillCompatibilityEnabled;
        String buildSerial;
        CompatibilityInfo compatInfo;
        Configuration config;
        int debugMode;
        boolean enableBinderTracking;
        LoadedApk info;
        ProfilerInfo initProfilerInfo;
        Bundle instrumentationArgs;
        ComponentName instrumentationName;
        IUiAutomationConnection instrumentationUiAutomationConnection;
        IInstrumentationWatcher instrumentationWatcher;
        boolean persistent;
        String processName;
        List<ProviderInfo> providers;
        boolean restrictedBackupMode;
        boolean trackAllocation;

        AppBindData() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AppBindData{appInfo=");
            stringBuilder.append(this.appInfo);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static final class BindServiceData {
        Intent intent;
        boolean rebind;
        IBinder token;

        BindServiceData() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BindServiceData{token=");
            stringBuilder.append(this.token);
            stringBuilder.append(" intent=");
            stringBuilder.append(this.intent);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static final class ContextCleanupInfo {
        ContextImpl context;
        String what;
        String who;

        ContextCleanupInfo() {
        }
    }

    static final class CreateBackupAgentData {
        ApplicationInfo appInfo;
        int backupMode;
        CompatibilityInfo compatInfo;

        CreateBackupAgentData() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CreateBackupAgentData{appInfo=");
            stringBuilder.append(this.appInfo);
            stringBuilder.append(" backupAgent=");
            stringBuilder.append(this.appInfo.backupAgentName);
            stringBuilder.append(" mode=");
            stringBuilder.append(this.backupMode);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static final class CreateServiceData {
        CompatibilityInfo compatInfo;
        ServiceInfo info;
        Intent intent;
        IBinder token;

        CreateServiceData() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CreateServiceData{token=");
            stringBuilder.append(this.token);
            stringBuilder.append(" className=");
            stringBuilder.append(this.info.name);
            stringBuilder.append(" packageName=");
            stringBuilder.append(this.info.packageName);
            stringBuilder.append(" intent=");
            stringBuilder.append(this.intent);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private class DropBoxReporter implements Reporter {
        private DropBoxManager dropBox;

        public void addData(String tag, byte[] data, int flags) {
            ensureInitialized();
            this.dropBox.addData(tag, data, flags);
        }

        public void addText(String tag, String data) {
            ensureInitialized();
            this.dropBox.addText(tag, data);
        }

        private synchronized void ensureInitialized() {
            if (this.dropBox == null) {
                this.dropBox = (DropBoxManager) ActivityThread.this.getSystemContext().getSystemService(Context.DROPBOX_SERVICE);
            }
        }
    }

    static final class DumpComponentInfo {
        String[] args;
        ParcelFileDescriptor fd;
        String prefix;
        IBinder token;

        DumpComponentInfo() {
        }
    }

    static final class DumpHeapData {
        ParcelFileDescriptor fd;
        public boolean mallocInfo;
        public boolean managed;
        String path;
        public boolean runGc;

        DumpHeapData() {
        }
    }

    private static class EventLoggingReporter implements EventLogger.Reporter {
        private EventLoggingReporter() {
        }

        /* synthetic */ EventLoggingReporter(AnonymousClass1 x0) {
            this();
        }

        public void report(int code, Object... list) {
            EventLog.writeEvent(code, list);
        }
    }

    static final class Profiler {
        boolean autoStopProfiler;
        boolean handlingProfiling;
        ParcelFileDescriptor profileFd;
        String profileFile;
        boolean profiling;
        int samplingInterval;
        boolean streamingOutput;

        Profiler() {
        }

        public void setProfiler(ProfilerInfo profilerInfo) {
            ParcelFileDescriptor fd = profilerInfo.profileFd;
            if (this.profiling) {
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e) {
                    }
                }
                return;
            }
            if (this.profileFd != null) {
                try {
                    this.profileFd.close();
                } catch (IOException e2) {
                }
            }
            this.profileFile = profilerInfo.profileFile;
            this.profileFd = fd;
            this.samplingInterval = profilerInfo.samplingInterval;
            this.autoStopProfiler = profilerInfo.autoStopProfiler;
            this.streamingOutput = profilerInfo.streamingOutput;
        }

        public void startProfiling() {
            if (this.profileFd != null && !this.profiling) {
                try {
                    VMDebug.startMethodTracing(this.profileFile, this.profileFd.getFileDescriptor(), (SystemProperties.getInt("debug.traceview-buffer-size-mb", 8) * 1024) * 1024, 0, this.samplingInterval != 0, this.samplingInterval, this.streamingOutput);
                    this.profiling = true;
                } catch (RuntimeException e) {
                    String str = ActivityThread.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Profiling failed on path ");
                    stringBuilder.append(this.profileFile);
                    Slog.w(str, stringBuilder.toString(), e);
                    try {
                        this.profileFd.close();
                        this.profileFd = null;
                    } catch (IOException e2) {
                        Slog.w(ActivityThread.TAG, "Failure closing profile fd", e2);
                    }
                }
            }
        }

        public void stopProfiling() {
            if (this.profiling) {
                this.profiling = false;
                Debug.stopMethodTracing();
                if (this.profileFd != null) {
                    try {
                        this.profileFd.close();
                    } catch (IOException e) {
                    }
                }
                this.profileFd = null;
                this.profileFile = null;
            }
        }
    }

    final class ProviderClientRecord {
        final ContentProviderHolder mHolder;
        final ContentProvider mLocalProvider;
        final String[] mNames;
        final IContentProvider mProvider;

        ProviderClientRecord(String[] names, IContentProvider provider, ContentProvider localProvider, ContentProviderHolder holder) {
            this.mNames = names;
            this.mProvider = provider;
            this.mLocalProvider = localProvider;
            this.mHolder = holder;
        }
    }

    private static final class ProviderKey {
        final String authority;
        final int userId;

        public ProviderKey(String authority, int userId) {
            this.authority = authority;
            this.userId = userId;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof ProviderKey)) {
                return false;
            }
            ProviderKey other = (ProviderKey) o;
            if (Objects.equals(this.authority, other.authority) && this.userId == other.userId) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (this.authority != null ? this.authority.hashCode() : 0) ^ this.userId;
        }
    }

    private static final class ProviderRefCount {
        public final ProviderClientRecord client;
        public final ContentProviderHolder holder;
        public boolean removePending;
        public int stableCount;
        public int unstableCount;

        ProviderRefCount(ContentProviderHolder inHolder, ProviderClientRecord inClient, int sCount, int uCount) {
            this.holder = inHolder;
            this.client = inClient;
            this.stableCount = sCount;
            this.unstableCount = uCount;
        }
    }

    static final class RequestAssistContextExtras {
        IBinder activityToken;
        int flags;
        IBinder requestToken;
        int requestType;
        int sessionId;

        RequestAssistContextExtras() {
        }
    }

    static final class ServiceArgsData {
        Intent args;
        int flags;
        int startId;
        boolean taskRemoved;
        IBinder token;

        ServiceArgsData() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ServiceArgsData{token=");
            stringBuilder.append(this.token);
            stringBuilder.append(" startId=");
            stringBuilder.append(this.startId);
            stringBuilder.append(" args=");
            stringBuilder.append(this.args);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    static final class UpdateCompatibilityData {
        CompatibilityInfo info;
        String pkg;

        UpdateCompatibilityData() {
        }
    }

    final class GcIdler implements IdleHandler {
        GcIdler() {
        }

        public final boolean queueIdle() {
            ActivityThread.this.doGcIfNeeded();
            return false;
        }
    }

    class H extends Handler {
        public static final int APPLICATION_INFO_CHANGED = 156;
        public static final int ATTACH_AGENT = 155;
        public static final int BIND_APPLICATION = 110;
        public static final int BIND_SERVICE = 121;
        public static final int CLEAN_UP_CONTEXT = 119;
        public static final int CONFIGURATION_CHANGED = 118;
        public static final int CREATE_BACKUP_AGENT = 128;
        public static final int CREATE_SERVICE = 114;
        public static final int CUSTOM_MSG = 1000;
        public static final int DESTROY_BACKUP_AGENT = 129;
        public static final int DISPATCH_PACKAGE_BROADCAST = 133;
        public static final int DUMP_ACTIVITY = 136;
        public static final int DUMP_HEAP = 135;
        public static final int DUMP_PROVIDER = 141;
        public static final int DUMP_SERVICE = 123;
        public static final int ENABLE_JIT = 132;
        public static final int ENTER_ANIMATION_COMPLETE = 149;
        public static final int EXECUTE_TRANSACTION = 159;
        public static final int EXIT_APPLICATION = 111;
        public static final int GC_WHEN_IDLE = 120;
        public static final int INSTALL_PROVIDER = 145;
        public static final int LOCAL_VOICE_INTERACTION_STARTED = 154;
        public static final int LOW_MEMORY = 124;
        public static final int ON_NEW_ACTIVITY_OPTIONS = 146;
        public static final int PROFILER_CONTROL = 127;
        public static final int RECEIVER = 113;
        public static final int RELAUNCH_ACTIVITY = 160;
        public static final int REMOVE_PROVIDER = 131;
        public static final int REQUEST_ASSIST_CONTEXT_EXTRAS = 143;
        public static final int REQUEST_NODEGROUP_CONTENT = 162;
        public static final int REQUEST_OTHER_CONTENT = 161;
        public static final int RUN_ISOLATED_ENTRY_POINT = 158;
        public static final int SCHEDULE_CRASH = 134;
        public static final int SERVICE_ARGS = 115;
        public static final int SET_CORE_SETTINGS = 138;
        public static final int SLEEPING = 137;
        public static final int START_BINDER_TRACKING = 150;
        public static final int STOP_BINDER_TRACKING_AND_DUMP = 151;
        public static final int STOP_SERVICE = 116;
        public static final int SUICIDE = 130;
        public static final int TRANSLUCENT_CONVERSION_COMPLETE = 144;
        public static final int UNBIND_SERVICE = 122;
        public static final int UNSTABLE_PROVIDER_DIED = 142;
        public static final int UPDATE_PACKAGE_COMPATIBILITY_INFO = 139;
        public static final int WINDOW_STATE_CHANGED = 1003;

        H() {
        }

        String codeToString(int code) {
            return Integer.toString(code);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 1003) {
                switch (i) {
                    case 110:
                        Trace.traceBegin(64, "bindApplication");
                        AppBindData data = msg.obj;
                        ScrollerBoostManager.getInstance().init();
                        ActivityThread.this.handleBindApplication(data);
                        Trace.traceEnd(64);
                        break;
                    case 111:
                        if (ActivityThread.this.mInitialApplication != null) {
                            ActivityThread.this.mInitialApplication.onTerminate();
                        }
                        Looper.myLooper().quit();
                        break;
                    default:
                        StringBuilder stringBuilder;
                        switch (i) {
                            case 113:
                                Trace.traceBegin(64, "broadcastReceiveComp");
                                ActivityThread.this.handleReceiver((ReceiverData) msg.obj);
                                Trace.traceEnd(64);
                                break;
                            case 114:
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("serviceCreate: ");
                                stringBuilder.append(String.valueOf(msg.obj));
                                Trace.traceBegin(64, stringBuilder.toString());
                                ActivityThread.this.handleCreateService((CreateServiceData) msg.obj);
                                Trace.traceEnd(64);
                                break;
                            case 115:
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("serviceStart: ");
                                stringBuilder.append(String.valueOf(msg.obj));
                                Trace.traceBegin(64, stringBuilder.toString());
                                ActivityThread.this.handleServiceArgs((ServiceArgsData) msg.obj);
                                Trace.traceEnd(64);
                                break;
                            case 116:
                                Trace.traceBegin(64, "serviceStop");
                                ActivityThread.this.handleStopService((IBinder) msg.obj);
                                Trace.traceEnd(64);
                                break;
                            default:
                                switch (i) {
                                    case 118:
                                        ActivityThread.this.handleConfigurationChanged((Configuration) msg.obj);
                                        break;
                                    case 119:
                                        ContextCleanupInfo cci = msg.obj;
                                        cci.context.performFinalCleanup(cci.who, cci.what);
                                        break;
                                    case 120:
                                        ActivityThread.this.scheduleGcIdler();
                                        break;
                                    case 121:
                                        Trace.traceBegin(64, "serviceBind");
                                        ActivityThread.this.handleBindService((BindServiceData) msg.obj);
                                        Trace.traceEnd(64);
                                        break;
                                    case 122:
                                        Trace.traceBegin(64, "serviceUnbind");
                                        ActivityThread.this.handleUnbindService((BindServiceData) msg.obj);
                                        Trace.traceEnd(64);
                                        break;
                                    case 123:
                                        ActivityThread.this.handleDumpService((DumpComponentInfo) msg.obj);
                                        break;
                                    case 124:
                                        Trace.traceBegin(64, "lowMemory");
                                        ActivityThread.this.handleLowMemory();
                                        Trace.traceEnd(64);
                                        break;
                                    default:
                                        boolean z = false;
                                        boolean z2 = true;
                                        ActivityThread activityThread;
                                        switch (i) {
                                            case 127:
                                                activityThread = ActivityThread.this;
                                                if (msg.arg1 != 0) {
                                                    z = true;
                                                }
                                                activityThread.handleProfilerControl(z, (ProfilerInfo) msg.obj, msg.arg2);
                                                break;
                                            case 128:
                                                Trace.traceBegin(64, "backupCreateAgent");
                                                ActivityThread.this.handleCreateBackupAgent((CreateBackupAgentData) msg.obj);
                                                Trace.traceEnd(64);
                                                break;
                                            case 129:
                                                Trace.traceBegin(64, "backupDestroyAgent");
                                                ActivityThread.this.handleDestroyBackupAgent((CreateBackupAgentData) msg.obj);
                                                Trace.traceEnd(64);
                                                break;
                                            case 130:
                                                Process.killProcess(Process.myPid());
                                                break;
                                            case 131:
                                                Trace.traceBegin(64, "providerRemove");
                                                ActivityThread.this.completeRemoveProvider((ProviderRefCount) msg.obj);
                                                Trace.traceEnd(64);
                                                break;
                                            case 132:
                                                ActivityThread.this.ensureJitEnabled();
                                                break;
                                            case 133:
                                                Trace.traceBegin(64, "broadcastPackage");
                                                ActivityThread.this.handleDispatchPackageBroadcast(msg.arg1, (String[]) msg.obj);
                                                Trace.traceEnd(64);
                                                break;
                                            case 134:
                                                throw new RemoteServiceException((String) msg.obj);
                                            case 135:
                                                ActivityThread.handleDumpHeap((DumpHeapData) msg.obj);
                                                break;
                                            case 136:
                                                ActivityThread.this.handleDumpActivity((DumpComponentInfo) msg.obj);
                                                break;
                                            case 137:
                                                Trace.traceBegin(64, "sleeping");
                                                activityThread = ActivityThread.this;
                                                IBinder iBinder = (IBinder) msg.obj;
                                                if (msg.arg1 != 0) {
                                                    z = true;
                                                }
                                                activityThread.handleSleeping(iBinder, z);
                                                Trace.traceEnd(64);
                                                break;
                                            case 138:
                                                Trace.traceBegin(64, "setCoreSettings");
                                                ActivityThread.this.handleSetCoreSettings((Bundle) msg.obj);
                                                Trace.traceEnd(64);
                                                break;
                                            case 139:
                                                ActivityThread.this.handleUpdatePackageCompatibilityInfo((UpdateCompatibilityData) msg.obj);
                                                break;
                                            default:
                                                IBinder iBinder2;
                                                switch (i) {
                                                    case 141:
                                                        ActivityThread.this.handleDumpProvider((DumpComponentInfo) msg.obj);
                                                        break;
                                                    case 142:
                                                        ActivityThread.this.handleUnstableProviderDied((IBinder) msg.obj, false);
                                                        break;
                                                    case 143:
                                                        ActivityThread.this.handleRequestAssistContextExtras((RequestAssistContextExtras) msg.obj);
                                                        break;
                                                    case 144:
                                                        activityThread = ActivityThread.this;
                                                        iBinder2 = (IBinder) msg.obj;
                                                        if (msg.arg1 == 1) {
                                                            z = true;
                                                        }
                                                        activityThread.handleTranslucentConversionComplete(iBinder2, z);
                                                        break;
                                                    case 145:
                                                        ActivityThread.this.handleInstallProvider((ProviderInfo) msg.obj);
                                                        break;
                                                    case 146:
                                                        Pair<IBinder, ActivityOptions> pair = msg.obj;
                                                        ActivityThread.this.onNewActivityOptions((IBinder) pair.first, (ActivityOptions) pair.second);
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case 149:
                                                                ActivityThread.this.handleEnterAnimationComplete((IBinder) msg.obj);
                                                                break;
                                                            case 150:
                                                                ActivityThread.this.handleStartBinderTracking();
                                                                break;
                                                            case 151:
                                                                ActivityThread.this.handleStopBinderTrackingAndDump((ParcelFileDescriptor) msg.obj);
                                                                break;
                                                            default:
                                                                switch (i) {
                                                                    case 154:
                                                                        ActivityThread.this.handleLocalVoiceInteractionStarted((IBinder) ((SomeArgs) msg.obj).arg1, (IVoiceInteractor) ((SomeArgs) msg.obj).arg2);
                                                                        break;
                                                                    case 155:
                                                                        Application app = ActivityThread.this.getApplication();
                                                                        ActivityThread.handleAttachAgent((String) msg.obj, app != null ? app.mLoadedApk : null);
                                                                        break;
                                                                    case 156:
                                                                        ActivityThread.this.mUpdatingSystemConfig = true;
                                                                        try {
                                                                            activityThread = ActivityThread.this;
                                                                            ApplicationInfo applicationInfo = (ApplicationInfo) msg.obj;
                                                                            if (msg.arg1 != 1) {
                                                                                z2 = false;
                                                                            }
                                                                            activityThread.handleApplicationInfoChanged(applicationInfo, z2);
                                                                            break;
                                                                        } finally {
                                                                            ActivityThread.this.mUpdatingSystemConfig = false;
                                                                        }
                                                                    default:
                                                                        SomeArgs args1;
                                                                        switch (i) {
                                                                            case 158:
                                                                                ActivityThread.this.handleRunIsolatedEntryPoint((String) ((SomeArgs) msg.obj).arg1, (String[]) ((SomeArgs) msg.obj).arg2);
                                                                                break;
                                                                            case 159:
                                                                                ClientTransaction transaction = msg.obj;
                                                                                ActivityThread.this.mTransactionExecutor.execute(transaction);
                                                                                if (ActivityThread.isSystem()) {
                                                                                    transaction.recycle();
                                                                                    break;
                                                                                }
                                                                                break;
                                                                            case 160:
                                                                                activityThread = ActivityThread.this;
                                                                                iBinder2 = (IBinder) msg.obj;
                                                                                if (msg.arg1 == 1) {
                                                                                    z = true;
                                                                                }
                                                                                activityThread.handleRelaunchActivityLocally(iBinder2, z);
                                                                                break;
                                                                            case 161:
                                                                                args1 = msg.obj;
                                                                                ActivityThread.this.handleContentOther((IBinder) args1.arg1, (Bundle) args1.arg2, msg.arg1);
                                                                                break;
                                                                            case 162:
                                                                                args1 = msg.obj;
                                                                                ActivityThread.this.handleRequestNode((IBinder) args1.arg1, (Bundle) args1.arg2, msg.arg1);
                                                                                break;
                                                                        }
                                                                        break;
                                                                }
                                                                break;
                                                        }
                                                }
                                        }
                                }
                        }
                }
            }
            ActivityThread.this.handlePCWindowStateChanged((IBinder) msg.obj, msg.arg1);
            Object obj = msg.obj;
            if (obj instanceof SomeArgs) {
                ((SomeArgs) obj).recycle();
            }
        }
    }

    private class Idler implements IdleHandler {
        private Idler() {
        }

        /* synthetic */ Idler(ActivityThread x0, AnonymousClass1 x1) {
            this();
        }

        public final boolean queueIdle() {
            ActivityClientRecord a = ActivityThread.this.mNewActivities;
            boolean stopProfiling = false;
            if (!(ActivityThread.this.mBoundApplication == null || ActivityThread.this.mProfiler.profileFd == null || !ActivityThread.this.mProfiler.autoStopProfiler)) {
                stopProfiling = true;
            }
            if (a != null) {
                ActivityThread.this.mNewActivities = null;
                IActivityManager am = ActivityManager.getService();
                do {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Reporting idle of ");
                    stringBuilder.append(a);
                    stringBuilder.append(" finished=");
                    boolean z = a.activity != null && a.activity.mFinished;
                    stringBuilder.append(z);
                    Flog.i(101, stringBuilder.toString());
                    if (!(a.activity == null || a.activity.mFinished)) {
                        try {
                            am.activityIdle(a.token, a.createdConfig, stopProfiling);
                            a.createdConfig = null;
                        } catch (RemoteException ex) {
                            throw ex.rethrowFromSystemServer();
                        }
                    }
                    ActivityClientRecord prev = a;
                    a = a.nextIdle;
                    prev.nextIdle = null;
                } while (a != null);
            }
            if (stopProfiling) {
                ActivityThread.this.mProfiler.stopProfiling();
            }
            ActivityThread.this.ensureJitEnabled();
            return false;
        }
    }

    private final class PreloadThreadHandler extends Handler {
        private static final int ASYNC_THREAD_QUIT = 0;
        private static final int PRE_BINDER_API_CACHE = 6;
        private static final int PRE_GET_RECEIVER_RESOURCE = 3;
        private static final int PRE_INIT_BUILD_MODEL = 2;
        private static final int PRE_INIT_LOGEXCEPTION = 1;
        private static final int PRE_INSTALL_MEMORY_LEAK_MONITOR = 4;
        private static final int PRE_LODE_MULTIDPIINFO = 5;

        public PreloadThreadHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            AppBindData data;
            switch (msg.what) {
                case 0:
                    synchronized (ActivityThread.mPreloadLock) {
                        if (ActivityThread.USE_CACHE) {
                            HwFrameworkFactory.getHwApiCacheManagerEx().disableCache();
                        }
                        if (!(ActivityThread.this.mPreloadHandlerThread == null || ActivityThread.this.mPreloadHandlerThread.getLooper() == null)) {
                            ActivityThread.this.mPreloadHandlerThread.getLooper().quit();
                            ActivityThread.this.mPreloadHandlerThread = null;
                            ActivityThread.this.mPreloadHandler = null;
                        }
                    }
                    return;
                case 1:
                    data = msg.obj;
                    LogException logexception = HwFrameworkFactory.getLogException();
                    if (data.appInfo.packageName != null && logexception.isInLogBlackList(data.appInfo.packageName)) {
                        BatteryManager batteryManager = (BatteryManager) ActivityThread.this.getSystemContext().getSystemService(Context.BATTERY_SERVICE);
                        StringBuilder stringBuilder;
                        if (batteryManager == null || !batteryManager.isCharging()) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("package ");
                            stringBuilder.append(data.appInfo.packageName);
                            stringBuilder.append(" is in black list, forbid logcat output");
                            Flog.i(101, stringBuilder.toString());
                            logexception.setliblogparam(2, "");
                            return;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("package ");
                        stringBuilder.append(data.appInfo.packageName);
                        stringBuilder.append(" is in black list but usb is connected, enable logcat output");
                        Flog.i(101, stringBuilder.toString());
                        return;
                    }
                    return;
                case 2:
                    data = msg.obj;
                    if (HwFrameworkFactory.getHwActivityThread() != null) {
                        HwFrameworkFactory.getHwActivityThread().changeToSpecialModel(data.appInfo.packageName);
                        return;
                    }
                    return;
                case 3:
                    msg.obj.getHwReceiverResource();
                    return;
                case 4:
                    msg.obj.installMemoryLeakMonitor();
                    return;
                case 5:
                    Resources.getPreMultidpiInfo((String) msg.obj);
                    return;
                case 6:
                    PackageManager pm = ActivityThread.this.getSystemContext().getPackageManager();
                    if (pm != null) {
                        HwFrameworkFactory.getHwApiCacheManagerEx().apiPreCache(pm);
                    }
                    ActivityThread.this.sendPreloadMessage(0, null, ActivityThread.HANDLER_BINDER_DURATION_TIME);
                    return;
                default:
                    String str = ActivityThread.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Invalid preload activity message msg:");
                    stringBuilder2.append(msg.what);
                    Slog.e(str, stringBuilder2.toString());
                    return;
            }
        }
    }

    static final class ReceiverData extends PendingResult {
        CompatibilityInfo compatInfo;
        ActivityInfo info;
        Intent intent;

        public ReceiverData(Intent intent, int resultCode, String resultData, Bundle resultExtras, boolean ordered, boolean sticky, IBinder token, int sendingUser) {
            super(resultCode, resultData, resultExtras, 0, ordered, sticky, token, sendingUser, intent.getFlags());
            this.intent = intent;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ReceiverData{intent=");
            stringBuilder.append(this.intent);
            stringBuilder.append(" packageName=");
            stringBuilder.append(this.info.packageName);
            stringBuilder.append(" resultCode=");
            stringBuilder.append(getResultCode());
            stringBuilder.append(" resultData=");
            stringBuilder.append(getResultData());
            stringBuilder.append(" resultExtras=");
            stringBuilder.append(getResultExtras(false));
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private class ApplicationThread extends Stub {
        private static final String DB_INFO_FORMAT = "  %8s %8s %14s %14s  %s";
        private int mLastProcessState;

        private ApplicationThread() {
            this.mLastProcessState = -1;
        }

        /* synthetic */ ApplicationThread(ActivityThread x0, AnonymousClass1 x1) {
            this();
        }

        private void updatePendingConfiguration(Configuration config) {
            synchronized (ActivityThread.this.mResourcesManager) {
                if (ActivityThread.this.mPendingConfiguration == null || ActivityThread.this.mPendingConfiguration.isOtherSeqNewer(config)) {
                    ActivityThread.this.mPendingConfiguration = config;
                }
            }
        }

        public final void scheduleSleeping(IBinder token, boolean sleeping) {
            ActivityThread.this.sendMessage(137, token, sleeping);
        }

        public final void scheduleReceiver(Intent intent, ActivityInfo info, CompatibilityInfo compatInfo, int resultCode, String data, Bundle extras, boolean sync, int sendingUser, int processState) {
            updateProcessState(processState, false);
            ReceiverData receiverData = new ReceiverData(intent, resultCode, data, extras, sync, false, ActivityThread.this.mAppThread.asBinder(), sendingUser);
            receiverData.info = info;
            receiverData.compatInfo = compatInfo;
            ActivityThread.this.sendMessage(113, receiverData);
        }

        public final void scheduleCreateBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo, int backupMode) {
            CreateBackupAgentData d = new CreateBackupAgentData();
            d.appInfo = app;
            d.compatInfo = compatInfo;
            d.backupMode = backupMode;
            ActivityThread.this.sendMessage(128, d);
        }

        public final void scheduleDestroyBackupAgent(ApplicationInfo app, CompatibilityInfo compatInfo) {
            CreateBackupAgentData d = new CreateBackupAgentData();
            d.appInfo = app;
            d.compatInfo = compatInfo;
            ActivityThread.this.sendMessage(129, d);
        }

        public final void scheduleCreateService(IBinder token, ServiceInfo info, CompatibilityInfo compatInfo, int processState) {
            updateProcessState(processState, false);
            CreateServiceData s = new CreateServiceData();
            s.token = token;
            s.info = info;
            s.compatInfo = compatInfo;
            ActivityThread.this.sendMessage(114, s);
        }

        public final void scheduleBindService(IBinder token, Intent intent, boolean rebind, int processState) {
            updateProcessState(processState, false);
            BindServiceData s = new BindServiceData();
            s.token = token;
            s.intent = intent;
            s.rebind = rebind;
            if (ActivityThread.DEBUG_SERVICE) {
                String str = ActivityThread.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("scheduleBindService token=");
                stringBuilder.append(token);
                stringBuilder.append(" intent=");
                stringBuilder.append(intent);
                stringBuilder.append(" uid=");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(" pid=");
                stringBuilder.append(Binder.getCallingPid());
                Slog.v(str, stringBuilder.toString());
            }
            ActivityThread.this.sendMessage(121, s);
        }

        public final void scheduleUnbindService(IBinder token, Intent intent) {
            BindServiceData s = new BindServiceData();
            s.token = token;
            s.intent = intent;
            ActivityThread.this.sendMessage(122, s);
        }

        public final void scheduleServiceArgs(IBinder token, ParceledListSlice args) {
            List<ServiceStartArgs> list = args.getList();
            for (int i = 0; i < list.size(); i++) {
                ServiceStartArgs ssa = (ServiceStartArgs) list.get(i);
                ServiceArgsData s = new ServiceArgsData();
                s.token = token;
                s.taskRemoved = ssa.taskRemoved;
                s.startId = ssa.startId;
                s.flags = ssa.flags;
                s.args = ssa.args;
                ActivityThread.this.sendMessage(115, s);
            }
        }

        public final void scheduleStopService(IBinder token) {
            ActivityThread.this.sendMessage(116, token);
        }

        public final void bindApplication(String processName, ApplicationInfo appInfo, List<ProviderInfo> providers, ComponentName instrumentationName, ProfilerInfo profilerInfo, Bundle instrumentationArgs, IInstrumentationWatcher instrumentationWatcher, IUiAutomationConnection instrumentationUiConnection, int debugMode, boolean enableBinderTracking, boolean trackAllocation, boolean isRestrictedBackupMode, boolean persistent, Configuration config, CompatibilityInfo compatInfo, Map services, Bundle coreSettings, String buildSerial, boolean autofillCompatibilityEnabled) {
            String str = processName;
            if (Jlog.isPerfTest()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pid=");
                stringBuilder.append(Process.myPid());
                stringBuilder.append("&processname=");
                stringBuilder.append(str);
                Jlog.i(3035, Jlog.getMessage(ActivityThread.TAG, "bindApplication", stringBuilder.toString()));
            }
            if (Log.isLoggable("36406078", 3)) {
                String str2 = ActivityThread.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("bindApplication: ");
                stringBuilder2.append(str);
                Log.d(str2, stringBuilder2.toString());
            }
            if (services != null) {
                ServiceManager.initServiceCache(services);
            }
            setCoreSettings(coreSettings);
            AppBindData data = new AppBindData();
            data.processName = str;
            data.appInfo = appInfo;
            data.providers = providers;
            data.instrumentationName = instrumentationName;
            data.instrumentationArgs = instrumentationArgs;
            data.instrumentationWatcher = instrumentationWatcher;
            data.instrumentationUiAutomationConnection = instrumentationUiConnection;
            data.debugMode = debugMode;
            data.enableBinderTracking = enableBinderTracking;
            data.trackAllocation = trackAllocation;
            data.restrictedBackupMode = isRestrictedBackupMode;
            data.persistent = persistent;
            Configuration config2 = ActivityThread.this.updateConfig(config);
            data.config = config2;
            data.compatInfo = compatInfo;
            data.initProfilerInfo = profilerInfo;
            data.buildSerial = buildSerial;
            data.autofillCompatibilityEnabled = autofillCompatibilityEnabled;
            ActivityThread.this.sendMessage(110, data);
        }

        public final void runIsolatedEntryPoint(String entryPoint, String[] entryPointArgs) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = entryPoint;
            args.arg2 = entryPointArgs;
            ActivityThread.this.sendMessage(158, args);
        }

        public final void scheduleExit() {
            ActivityThread.this.sendMessage(111, null);
        }

        public final void scheduleSuicide() {
            ActivityThread.this.sendMessage(130, null);
        }

        public void scheduleApplicationInfoChanged(ApplicationInfo ai) {
            scheduleApplicationThemeInfoChanged(ai, false);
        }

        public void scheduleApplicationThemeInfoChanged(ApplicationInfo ai, boolean fromThemeChange) {
            ActivityThread.this.sendMessage(156, ai, fromThemeChange);
        }

        public void updateTimeZone() {
            TimeZone.setDefault(null);
        }

        public void clearDnsCache() {
            InetAddress.clearDnsCache();
            NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
        }

        public void setHttpProxy(String host, String port, String exclList, Uri pacFileUrl) {
            ConnectivityManager cm = ConnectivityManager.from(ActivityThread.this.getApplication() != null ? ActivityThread.this.getApplication() : ActivityThread.this.getSystemContext());
            if (cm == null || cm.getBoundNetworkForProcess() == null) {
                Proxy.setHttpProxySystemProperty(host, port, exclList, pacFileUrl);
            } else {
                Proxy.setHttpProxySystemProperty(cm.getDefaultProxy());
            }
        }

        public void processInBackground() {
            ActivityThread.this.mH.removeMessages(120);
            ActivityThread.this.mH.sendMessage(ActivityThread.this.mH.obtainMessage(120));
        }

        public void dumpService(ParcelFileDescriptor pfd, IBinder servicetoken, String[] args) {
            DumpComponentInfo data = new DumpComponentInfo();
            try {
                data.fd = pfd.dup();
                data.token = servicetoken;
                data.args = args;
                ActivityThread.this.sendMessage(123, (Object) data, 0, 0, true);
            } catch (IOException e) {
                Slog.w(ActivityThread.TAG, "dumpService failed", e);
            } catch (Throwable th) {
                IoUtils.closeQuietly(pfd);
            }
            IoUtils.closeQuietly(pfd);
        }

        public void scheduleRegisteredReceiver(IIntentReceiver receiver, Intent intent, int resultCode, String dataStr, Bundle extras, boolean ordered, boolean sticky, int sendingUser, int processState) throws RemoteException {
            updateProcessState(processState, false);
            receiver.performReceive(intent, resultCode, dataStr, extras, ordered, sticky, sendingUser);
        }

        public void scheduleLowMemory() {
            ActivityThread.this.sendMessage(124, null);
        }

        public void profilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) {
            ActivityThread.this.sendMessage(127, profilerInfo, start, profileType);
        }

        public void dumpHeap(boolean managed, boolean mallocInfo, boolean runGc, String path, ParcelFileDescriptor fd) {
            DumpHeapData dhd = new DumpHeapData();
            dhd.managed = managed;
            dhd.mallocInfo = mallocInfo;
            dhd.runGc = runGc;
            dhd.path = path;
            dhd.fd = fd;
            ActivityThread.this.sendMessage(135, (Object) dhd, 0, 0, true);
        }

        public void attachAgent(String agent) {
            ActivityThread.this.sendMessage(155, agent);
        }

        public void setSchedulingGroup(int group) {
            try {
                Process.setProcessGroup(Process.myPid(), group);
            } catch (Exception e) {
                String str = ActivityThread.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed setting process group to ");
                stringBuilder.append(group);
                Slog.w(str, stringBuilder.toString(), e);
            }
        }

        public void dispatchPackageBroadcast(int cmd, String[] packages) {
            ActivityThread.this.sendMessage(133, packages, cmd);
        }

        public void scheduleCrash(String msg) {
            ActivityThread.this.sendMessage(134, msg);
        }

        public void dumpActivity(ParcelFileDescriptor pfd, IBinder activitytoken, String prefix, String[] args) {
            DumpComponentInfo data = new DumpComponentInfo();
            try {
                data.fd = pfd.dup();
                data.token = activitytoken;
                data.prefix = prefix;
                data.args = args;
                ActivityThread.this.sendMessage(136, (Object) data, 0, 0, true);
            } catch (IOException e) {
                Slog.w(ActivityThread.TAG, "dumpActivity failed", e);
            } catch (Throwable th) {
                IoUtils.closeQuietly(pfd);
            }
            IoUtils.closeQuietly(pfd);
        }

        public void dumpProvider(ParcelFileDescriptor pfd, IBinder providertoken, String[] args) {
            DumpComponentInfo data = new DumpComponentInfo();
            try {
                data.fd = pfd.dup();
                data.token = providertoken;
                data.args = args;
                ActivityThread.this.sendMessage(141, (Object) data, 0, 0, true);
            } catch (IOException e) {
                Slog.w(ActivityThread.TAG, "dumpProvider failed", e);
            } catch (Throwable th) {
                IoUtils.closeQuietly(pfd);
            }
            IoUtils.closeQuietly(pfd);
        }

        public void dumpMemInfo(ParcelFileDescriptor pfd, MemoryInfo mem, boolean checkin, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) {
            IOException iOException;
            FileOutputStream fout = new FileOutputStream(pfd.getFileDescriptor());
            IOException e = new FastPrintWriter(fout);
            PrintWriter pw = e;
            try {
                dumpMemInfo(pw, mem, checkin, dumpFullInfo, dumpDalvik, dumpSummaryOnly, dumpUnreachable);
                try {
                    fout.close();
                } catch (IOException e2) {
                    iOException = e2;
                    Slog.w(ActivityThread.TAG, "Unable to close fout ");
                }
                pw.close();
                IoUtils.closeQuietly(pfd);
            } finally {
                pw.flush();
                try {
                    fout.close();
                } catch (IOException e3) {
                    e2 = e3;
                    Slog.w(ActivityThread.TAG, "Unable to close fout ");
                }
                pw.close();
                IoUtils.closeQuietly(pfd);
                iOException = e2;
            }
        }

        private void dumpMemInfo(PrintWriter pw, MemoryInfo memInfo, boolean checkin, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable) {
            PrintWriter printWriter = pw;
            long nativeMax = Debug.getNativeHeapSize() / 1024;
            long nativeAllocated = Debug.getNativeHeapAllocatedSize() / 1024;
            long nativeFree = Debug.getNativeHeapFreeSize() / 1024;
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long dalvikMax = runtime.totalMemory() / 1024;
            long dalvikFree = runtime.freeMemory() / 1024;
            long dalvikAllocated = dalvikMax - dalvikFree;
            r1 = new Class[4];
            boolean i = false;
            r1[0] = ContextImpl.class;
            r1[1] = Activity.class;
            r1[2] = WebView.class;
            r1[3] = OpenSSLSocketImpl.class;
            Class[] classesToCount = r1;
            long[] instanceCounts = VMDebug.countInstancesOfClasses(classesToCount, true);
            long appContextInstanceCount = instanceCounts[0];
            long activityInstanceCount = instanceCounts[1];
            long webviewInstanceCount = instanceCounts[2];
            long openSslSocketCount = instanceCounts[3];
            long viewInstanceCount = ViewDebug.getViewInstanceCount();
            long viewRootInstanceCount = ViewDebug.getViewRootImplCount();
            int globalAssetCount = AssetManager.getGlobalAssetCount();
            long viewRootInstanceCount2 = viewRootInstanceCount;
            int globalAssetManagerCount = AssetManager.getGlobalAssetManagerCount();
            int binderLocalObjectCount = Debug.getBinderLocalObjectCount();
            int globalAssetManagerCount2 = globalAssetManagerCount;
            int binderProxyObjectCount = Debug.getBinderProxyObjectCount();
            globalAssetManagerCount = Debug.getBinderDeathObjectCount();
            long parcelSize = Parcel.getGlobalAllocSize();
            int binderDeathObjectCount = globalAssetManagerCount;
            int binderLocalObjectCount2 = binderLocalObjectCount;
            long parcelCount = Parcel.getGlobalAllocCount();
            PagerStats stats = SQLiteDebug.getDatabaseInfo();
            binderLocalObjectCount = Process.myPid();
            String str = ActivityThread.this.mBoundApplication != null ? ActivityThread.this.mBoundApplication.processName : "unknown";
            long openSslSocketCount2 = openSslSocketCount;
            long webviewInstanceCount2 = webviewInstanceCount;
            long activityInstanceCount2 = activityInstanceCount;
            long appContextInstanceCount2 = appContextInstanceCount;
            int i2 = 2;
            PagerStats stats2 = stats;
            long viewInstanceCount2 = viewInstanceCount;
            long viewRootInstanceCount3 = viewRootInstanceCount2;
            int globalAssetManagerCount3 = globalAssetManagerCount2;
            int binderProxyObjectCount2 = binderProxyObjectCount;
            int binderLocalObjectCount3 = binderLocalObjectCount2;
            int binderDeathObjectCount2 = binderDeathObjectCount;
            long parcelCount2 = parcelCount;
            int globalAssetCount2 = globalAssetCount;
            PrintWriter printWriter2 = printWriter;
            ActivityThread.dumpMemInfoTable(printWriter, memInfo, checkin, dumpFullInfo, dumpDalvik, dumpSummaryOnly, binderLocalObjectCount, str, nativeMax, nativeAllocated, nativeFree, dalvikMax, dalvikAllocated, dalvikFree);
            PagerStats stats3;
            int i3;
            long openSslSocketCount3;
            int globalAssetManagerCount4;
            int binderLocalObjectCount4;
            if (checkin) {
                printWriter2.print(viewInstanceCount2);
                printWriter2.print(',');
                printWriter2.print(viewRootInstanceCount3);
                printWriter2.print(',');
                printWriter2.print(appContextInstanceCount2);
                printWriter2.print(',');
                printWriter2.print(activityInstanceCount2);
                printWriter2.print(',');
                printWriter2.print(globalAssetCount2);
                printWriter2.print(',');
                int globalAssetManagerCount5 = globalAssetManagerCount3;
                printWriter2.print(globalAssetManagerCount5);
                printWriter2.print(',');
                globalAssetManagerCount = binderLocalObjectCount3;
                printWriter2.print(globalAssetManagerCount);
                printWriter2.print(',');
                binderLocalObjectCount = binderProxyObjectCount2;
                printWriter2.print(binderLocalObjectCount);
                printWriter2.print(',');
                int binderDeathObjectCount3 = binderDeathObjectCount2;
                printWriter2.print(binderDeathObjectCount3);
                printWriter2.print(',');
                long openSslSocketCount4 = openSslSocketCount2;
                printWriter2.print(openSslSocketCount4);
                printWriter2.print(',');
                stats3 = stats2;
                printWriter2.print(stats3.memoryUsed / 1024);
                printWriter2.print(',');
                printWriter2.print(stats3.memoryUsed / 1024);
                printWriter2.print(',');
                printWriter2.print(stats3.pageCacheOverflow / 1024);
                printWriter2.print(',');
                printWriter2.print(stats3.largestMemAlloc / 1024);
                while (true) {
                    int i4;
                    i3 = i4;
                    openSslSocketCount3 = openSslSocketCount4;
                    if (i3 < stats3.dbStats.size()) {
                        DbStats dbStats = (DbStats) stats3.dbStats.get(i3);
                        printWriter2.print(',');
                        printWriter2.print(dbStats.dbName);
                        printWriter2.print(',');
                        globalAssetManagerCount4 = globalAssetManagerCount5;
                        binderLocalObjectCount4 = globalAssetManagerCount;
                        printWriter2.print(dbStats.pageSize);
                        printWriter2.print(',');
                        printWriter2.print(dbStats.dbSize);
                        printWriter2.print(',');
                        printWriter2.print(dbStats.lookaside);
                        printWriter2.print(',');
                        printWriter2.print(dbStats.cache);
                        printWriter2.print(',');
                        printWriter2.print(dbStats.cache);
                        i4 = i3 + 1;
                        openSslSocketCount4 = openSslSocketCount3;
                        globalAssetManagerCount5 = globalAssetManagerCount4;
                        globalAssetManagerCount = binderLocalObjectCount4;
                    } else {
                        binderLocalObjectCount4 = globalAssetManagerCount;
                        pw.println();
                        return;
                    }
                }
            }
            PagerStats stats4;
            openSslSocketCount3 = openSslSocketCount2;
            long viewInstanceCount3 = viewInstanceCount2;
            activityInstanceCount = viewRootInstanceCount3;
            int binderDeathObjectCount4 = binderDeathObjectCount2;
            stats3 = stats2;
            printWriter2.println(" ");
            printWriter2.println(" Objects");
            ActivityThread.printRow(printWriter2, ActivityThread.TWO_COUNT_COLUMNS, "Views:", Long.valueOf(viewInstanceCount3), "ViewRootImpl:", Long.valueOf(activityInstanceCount));
            ActivityThread.printRow(printWriter2, ActivityThread.TWO_COUNT_COLUMNS, "AppContexts:", Long.valueOf(appContextInstanceCount), "Activities:", Long.valueOf(activityInstanceCount));
            ActivityThread.printRow(printWriter2, ActivityThread.TWO_COUNT_COLUMNS, "Assets:", Integer.valueOf(globalAssetCount), "AssetManagers:", Integer.valueOf(globalAssetManagerCount4));
            ActivityThread.printRow(printWriter2, ActivityThread.TWO_COUNT_COLUMNS, "Local Binders:", Integer.valueOf(binderLocalObjectCount4), "Proxy Binders:", Integer.valueOf(binderProxyObjectCount));
            String str2 = ActivityThread.TWO_COUNT_COLUMNS;
            r2 = new Object[4];
            r2[1] = Long.valueOf(parcelSize / 1024);
            r2[2] = "Parcel count:";
            activityInstanceCount = parcelCount2;
            r2[3] = Long.valueOf(activityInstanceCount);
            ActivityThread.printRow(printWriter2, str2, r2);
            str2 = ActivityThread.TWO_COUNT_COLUMNS;
            Object[] objArr = new Object[4];
            objArr[0] = "Death Recipients:";
            int binderDeathObjectCount5 = binderDeathObjectCount4;
            objArr[1] = Integer.valueOf(binderDeathObjectCount5);
            objArr[2] = "OpenSSL Sockets:";
            activityInstanceCount = openSslSocketCount3;
            objArr[3] = Long.valueOf(activityInstanceCount);
            ActivityThread.printRow(printWriter2, str2, objArr);
            str2 = ActivityThread.ONE_COUNT_COLUMN;
            objArr = new Object[2];
            objArr[0] = "WebViews:";
            objArr[1] = Long.valueOf(webviewInstanceCount2);
            ActivityThread.printRow(printWriter2, str2, objArr);
            printWriter2.println(" ");
            printWriter2.println(" SQL");
            ActivityThread.printRow(printWriter2, ActivityThread.ONE_COUNT_COLUMN, "MEMORY_USED:", Integer.valueOf(stats3.memoryUsed / 1024));
            ActivityThread.printRow(printWriter2, ActivityThread.TWO_COUNT_COLUMNS, "PAGECACHE_OVERFLOW:", Integer.valueOf(stats3.pageCacheOverflow / 1024), "MALLOC_SIZE:", Integer.valueOf(stats3.largestMemAlloc / 1024));
            printWriter2.println(" ");
            i3 = stats3.dbStats.size();
            if (i3 > 0) {
                printWriter2.println(" DATABASES");
                int i5 = 5;
                ActivityThread.printRow(printWriter2, DB_INFO_FORMAT, "pgsz", "dbsz", "Lookaside(b)", "cache", "Dbname");
                binderDeathObjectCount5 = 0;
                while (binderDeathObjectCount5 < i3) {
                    DbStats dbStats2 = (DbStats) stats3.dbStats.get(binderDeathObjectCount5);
                    int N = i3;
                    str2 = DB_INFO_FORMAT;
                    stats4 = stats3;
                    String[] strArr = new Object[i5];
                    strArr[0] = dbStats2.pageSize > 0 ? String.valueOf(dbStats2.pageSize) : " ";
                    strArr[1] = dbStats2.dbSize > 0 ? String.valueOf(dbStats2.dbSize) : " ";
                    strArr[2] = dbStats2.lookaside > 0 ? String.valueOf(dbStats2.lookaside) : " ";
                    strArr[3] = dbStats2.cache;
                    strArr[4] = dbStats2.dbName;
                    ActivityThread.printRow(printWriter2, str2, strArr);
                    binderDeathObjectCount5++;
                    i3 = N;
                    stats3 = stats4;
                    i5 = 5;
                }
            }
            stats4 = stats3;
            str2 = AssetManager.getAssetAllocations();
            if (str2 != null) {
                printWriter2.println(" ");
                printWriter2.println(" Asset Allocations");
                printWriter2.print(str2);
            }
            if (dumpUnreachable) {
                if (!(ActivityThread.this.mBoundApplication == null || (2 & ActivityThread.this.mBoundApplication.appInfo.flags) == 0) || Build.IS_DEBUGGABLE) {
                    i = true;
                }
                boolean showContents = i;
                printWriter2.println(" ");
                printWriter2.println(" Unreachable memory");
                printWriter2.print(Debug.getUnreachableMemory(100, showContents));
            }
        }

        public void dumpMemInfoProto(ParcelFileDescriptor pfd, MemoryInfo mem, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable, String[] args) {
            ProtoOutputStream proto = new ProtoOutputStream(pfd.getFileDescriptor());
            try {
                dumpMemInfo(proto, mem, dumpFullInfo, dumpDalvik, dumpSummaryOnly, dumpUnreachable);
                proto.flush();
                IoUtils.closeQuietly(pfd);
            } catch (Throwable th) {
                proto.flush();
                IoUtils.closeQuietly(pfd);
                Throwable th2 = th;
            }
        }

        private void dumpMemInfo(ProtoOutputStream proto, MemoryInfo memInfo, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, boolean dumpUnreachable) {
            int n;
            long appContextInstanceCount;
            ProtoOutputStream protoOutputStream = proto;
            long nativeMax = Debug.getNativeHeapSize() / 1024;
            long nativeAllocated = Debug.getNativeHeapAllocatedSize() / 1024;
            long nativeFree = Debug.getNativeHeapFreeSize() / 1024;
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long dalvikMax = runtime.totalMemory() / 1024;
            long dalvikFree = runtime.freeMemory() / 1024;
            long dalvikAllocated = dalvikMax - dalvikFree;
            r1 = new Class[4];
            boolean z = false;
            r1[0] = ContextImpl.class;
            r1[1] = Activity.class;
            r1[2] = WebView.class;
            r1[3] = OpenSSLSocketImpl.class;
            Class[] classesToCount = r1;
            long[] instanceCounts = VMDebug.countInstancesOfClasses(classesToCount, true);
            long appContextInstanceCount2 = instanceCounts[0];
            long activityInstanceCount = instanceCounts[1];
            long webviewInstanceCount = instanceCounts[2];
            long openSslSocketCount = instanceCounts[3];
            long viewInstanceCount = ViewDebug.getViewInstanceCount();
            long viewRootInstanceCount = ViewDebug.getViewRootImplCount();
            int globalAssetCount = AssetManager.getGlobalAssetCount();
            long viewRootInstanceCount2 = viewRootInstanceCount;
            int globalAssetManagerCount = AssetManager.getGlobalAssetManagerCount();
            int binderLocalObjectCount = Debug.getBinderLocalObjectCount();
            int globalAssetManagerCount2 = globalAssetManagerCount;
            int binderProxyObjectCount = Debug.getBinderProxyObjectCount();
            globalAssetManagerCount = Debug.getBinderDeathObjectCount();
            long parcelSize = Parcel.getGlobalAllocSize();
            int binderDeathObjectCount = globalAssetManagerCount;
            int binderLocalObjectCount2 = binderLocalObjectCount;
            long parcelCount = Parcel.getGlobalAllocCount();
            PagerStats stats = SQLiteDebug.getDatabaseInfo();
            long viewInstanceCount2 = viewInstanceCount;
            viewInstanceCount = protoOutputStream.start(1146756268033L);
            Class[] classesToCount2 = classesToCount;
            int globalAssetCount2 = globalAssetCount;
            protoOutputStream.write(1120986464257, Process.myPid());
            protoOutputStream.write(1138166333442L, ActivityThread.this.mBoundApplication != null ? ActivityThread.this.mBoundApplication.processName : "unknown");
            long mToken = viewInstanceCount;
            long openSslSocketCount2 = openSslSocketCount;
            long webviewInstanceCount2 = webviewInstanceCount;
            long activityInstanceCount2 = activityInstanceCount;
            long appContextInstanceCount3 = appContextInstanceCount2;
            long viewInstanceCount3 = viewInstanceCount2;
            PagerStats stats2 = stats;
            long viewRootInstanceCount3 = viewRootInstanceCount2;
            int globalAssetManagerCount3 = globalAssetManagerCount2;
            int binderProxyObjectCount2 = binderProxyObjectCount;
            int binderLocalObjectCount3 = binderLocalObjectCount2;
            int binderDeathObjectCount2 = binderDeathObjectCount;
            long parcelCount2 = parcelCount;
            int globalAssetCount3 = globalAssetCount2;
            ProtoOutputStream protoOutputStream2 = protoOutputStream;
            ActivityThread.dumpMemInfoTable(protoOutputStream, memInfo, dumpDalvik, dumpSummaryOnly, nativeMax, nativeAllocated, nativeFree, dalvikMax, dalvikAllocated, dalvikFree);
            viewInstanceCount = mToken;
            protoOutputStream2.end(viewInstanceCount);
            openSslSocketCount = protoOutputStream2.start(1146756268034L);
            webviewInstanceCount = viewInstanceCount3;
            protoOutputStream2.write(1120986464257L, webviewInstanceCount);
            protoOutputStream2.write(1120986464258L, viewRootInstanceCount3);
            appContextInstanceCount2 = appContextInstanceCount3;
            protoOutputStream2.write(1120986464259L, appContextInstanceCount2);
            activityInstanceCount = activityInstanceCount2;
            protoOutputStream2.write(1120986464260L, activityInstanceCount);
            int globalAssetCount4 = globalAssetCount3;
            protoOutputStream2.write(1120986464261L, globalAssetCount4);
            int globalAssetManagerCount4 = globalAssetManagerCount3;
            protoOutputStream2.write(1120986464262L, globalAssetManagerCount4);
            int binderLocalObjectCount4 = binderLocalObjectCount3;
            protoOutputStream2.write(1120986464263L, binderLocalObjectCount4);
            globalAssetManagerCount4 = binderProxyObjectCount2;
            protoOutputStream2.write(1120986464264L, globalAssetManagerCount4);
            protoOutputStream2.write(1112396529673L, parcelSize / 1024);
            protoOutputStream2.write(1120986464266L, parcelCount2);
            int binderDeathObjectCount3 = binderDeathObjectCount2;
            protoOutputStream2.write(1120986464267L, binderDeathObjectCount3);
            webviewInstanceCount = openSslSocketCount2;
            protoOutputStream2.write(Detail.DESCRIPTION_RES, webviewInstanceCount);
            webviewInstanceCount = webviewInstanceCount2;
            protoOutputStream2.write(Detail.UI_OPTIONS, webviewInstanceCount);
            protoOutputStream2.end(openSslSocketCount);
            viewInstanceCount = protoOutputStream2.start(1146756268035L);
            PagerStats stats3 = stats2;
            protoOutputStream2.write(1120986464257L, stats3.memoryUsed / 1024);
            protoOutputStream2.write(1120986464258L, stats3.pageCacheOverflow / 1024);
            protoOutputStream2.write(1120986464259L, stats3.largestMemAlloc / 1024);
            int n2 = stats3.dbStats.size();
            binderDeathObjectCount3 = 0;
            while (true) {
                long activityInstanceCount3 = activityInstanceCount;
                if (binderDeathObjectCount3 >= n2) {
                    break;
                }
                DbStats dbStats = (DbStats) stats3.dbStats.get(binderDeathObjectCount3);
                long dToken = protoOutputStream2.start(2246267895812L);
                PagerStats stats4 = stats3;
                n = n2;
                appContextInstanceCount = appContextInstanceCount2;
                protoOutputStream2.write(1138166333441L, dbStats.dbName);
                protoOutputStream2.write(1120986464258L, dbStats.pageSize);
                protoOutputStream2.write(1120986464259L, dbStats.dbSize);
                protoOutputStream2.write(1120986464260L, dbStats.lookaside);
                protoOutputStream2.write(1138166333445L, dbStats.cache);
                protoOutputStream2.end(dToken);
                binderDeathObjectCount3++;
                activityInstanceCount = activityInstanceCount3;
                stats3 = stats4;
                n2 = n;
                appContextInstanceCount2 = appContextInstanceCount;
            }
            n = n2;
            appContextInstanceCount = appContextInstanceCount2;
            protoOutputStream2.end(viewInstanceCount);
            String assetAlloc = AssetManager.getAssetAllocations();
            if (assetAlloc != null) {
                protoOutputStream2.write(1138166333444L, assetAlloc);
            }
            if (dumpUnreachable) {
                if (((ActivityThread.this.mBoundApplication == null ? 0 : ActivityThread.this.mBoundApplication.appInfo.flags) & 2) != 0 || Build.IS_DEBUGGABLE) {
                    z = true;
                }
                protoOutputStream2.write(1138166333445L, Debug.getUnreachableMemory(100, z));
                return;
            }
            int i = globalAssetCount4;
        }

        public void dumpGfxInfo(ParcelFileDescriptor pfd, String[] args) {
            ActivityThread.this.nDumpGraphicsInfo(pfd.getFileDescriptor());
            WindowManagerGlobal.getInstance().dumpGfxInfo(pfd.getFileDescriptor(), args);
            IoUtils.closeQuietly(pfd);
        }

        private void dumpDatabaseInfo(ParcelFileDescriptor pfd, String[] args) {
            PrintWriter pw = new FastPrintWriter(new FileOutputStream(pfd.getFileDescriptor()));
            SQLiteDebug.dump(new PrintWriterPrinter(pw), args);
            pw.flush();
        }

        public void dumpDbInfo(ParcelFileDescriptor pfd, final String[] args) {
            if (ActivityThread.this.mSystemThread) {
                ParcelFileDescriptor dup;
                try {
                    dup = pfd.dup();
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                        public void run() {
                            try {
                                ApplicationThread.this.dumpDatabaseInfo(dup, args);
                            } finally {
                                IoUtils.closeQuietly(dup);
                            }
                        }
                    });
                } catch (IOException e) {
                    dup = e;
                    String str = ActivityThread.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not dup FD ");
                    stringBuilder.append(pfd.getFileDescriptor().getInt$());
                    Log.w(str, stringBuilder.toString());
                } finally {
                    IoUtils.closeQuietly(pfd);
                }
            } else {
                dumpDatabaseInfo(pfd, args);
                IoUtils.closeQuietly(pfd);
            }
        }

        public void unstableProviderDied(IBinder provider) {
            ActivityThread.this.sendMessage(142, provider);
        }

        public void requestAssistContextExtras(IBinder activityToken, IBinder requestToken, int requestType, int sessionId, int flags) {
            RequestAssistContextExtras cmd = new RequestAssistContextExtras();
            cmd.activityToken = activityToken;
            cmd.requestToken = requestToken;
            cmd.requestType = requestType;
            cmd.sessionId = sessionId;
            cmd.flags = flags;
            ActivityThread.this.sendMessage(143, cmd);
        }

        public void setCoreSettings(Bundle coreSettings) {
            ActivityThread.this.sendMessage(138, coreSettings);
        }

        public void updatePackageCompatibilityInfo(String pkg, CompatibilityInfo info) {
            UpdateCompatibilityData ucd = new UpdateCompatibilityData();
            ucd.pkg = pkg;
            ucd.info = info;
            ActivityThread.this.sendMessage(139, ucd);
        }

        public void scheduleTrimMemory(int level) {
            iawareTrimMemory(level, false);
        }

        public void iawareTrimMemory(int level, boolean fromIAware) {
            Runnable r = PooledLambda.obtainRunnable(-$$Lambda$ActivityThread$ApplicationThread$eNIzQZ974tdrS8H-1o1fp2sJZxk.INSTANCE, ActivityThread.this, Integer.valueOf(level), Boolean.valueOf(fromIAware));
            Choreographer choreographer = Choreographer.getMainThreadInstance();
            if (choreographer != null) {
                choreographer.postCallback(3, r, null);
            } else {
                ActivityThread.this.mH.post(r);
            }
        }

        public void scheduleTranslucentConversionComplete(IBinder token, boolean drawComplete) {
            ActivityThread.this.sendMessage(144, token, drawComplete);
        }

        public void scheduleOnNewActivityOptions(IBinder token, Bundle options) {
            ActivityThread.this.sendMessage(146, new Pair(token, ActivityOptions.fromBundle(options)));
        }

        public void setProcessState(int state) {
            updateProcessState(state, true);
        }

        public void updateProcessState(int processState, boolean fromIpc) {
            synchronized (this) {
                if (this.mLastProcessState != processState) {
                    this.mLastProcessState = processState;
                    int dalvikProcessState = 1;
                    if (processState <= 5) {
                        dalvikProcessState = 0;
                    }
                    VMRuntime.getRuntime().updateProcessState(dalvikProcessState);
                }
            }
        }

        public void setNetworkBlockSeq(long procStateSeq) {
            synchronized (ActivityThread.this.mNetworkPolicyLock) {
                ActivityThread.this.mNetworkBlockSeq = procStateSeq;
            }
        }

        public void scheduleInstallProvider(ProviderInfo provider) {
            ActivityThread.this.sendMessage(145, provider);
        }

        public final void updateTimePrefs(int timeFormatPreference) {
            Boolean timeFormatPreferenceBool;
            if (timeFormatPreference == 0) {
                timeFormatPreferenceBool = Boolean.FALSE;
            } else if (timeFormatPreference == 1) {
                timeFormatPreferenceBool = Boolean.TRUE;
            } else {
                timeFormatPreferenceBool = null;
            }
            DateFormat.set24HourTimePref(timeFormatPreferenceBool);
        }

        public void scheduleEnterAnimationComplete(IBinder token) {
            ActivityThread.this.sendMessage(149, token);
        }

        public void notifyCleartextNetwork(byte[] firstPacket) {
            if (StrictMode.vmCleartextNetworkEnabled()) {
                StrictMode.onCleartextNetworkDetected(firstPacket);
            }
        }

        public void startBinderTracking() {
            ActivityThread.this.sendMessage(150, null);
        }

        public void stopBinderTrackingAndDump(ParcelFileDescriptor pfd) {
            try {
                ActivityThread.this.sendMessage(151, pfd.dup());
            } catch (IOException e) {
                Log.e(ActivityThread.TAG, "stopBinderTrackingAndDump()");
            } catch (Throwable th) {
                IoUtils.closeQuietly(pfd);
            }
            IoUtils.closeQuietly(pfd);
        }

        public void scheduleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor voiceInteractor) throws RemoteException {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = token;
            args.arg2 = voiceInteractor;
            ActivityThread.this.sendMessage(154, args);
        }

        public void handleTrustStorageUpdate() {
            NetworkSecurityPolicy.getInstance().handleTrustStorageUpdate();
        }

        public void schedulePCWindowStateChanged(IBinder token, int windowState) throws RemoteException {
            ActivityThread.this.sendMessage(1003, token, windowState);
        }

        public void requestContentNode(IBinder appToken, Bundle data, int token) throws RemoteException {
            if (ActivityThread.sContentSensorManager == null) {
                ActivityClientRecord r = (ActivityClientRecord) ActivityThread.this.mActivities.get(appToken);
                ActivityThread.sContentSensorManager = ContentSensorManagerFactory.createContentSensorManager(token, r == null ? null : r.activity);
            }
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = appToken;
            args.arg2 = data;
            ActivityThread.this.sendMessage(162, args, token);
        }

        public void requestContentOther(IBinder appToken, Bundle data, int token) throws RemoteException {
            if (ActivityThread.sContentSensorManager == null) {
                ActivityClientRecord r = (ActivityClientRecord) ActivityThread.this.mActivities.get(appToken);
                ActivityThread.sContentSensorManager = ContentSensorManagerFactory.createContentSensorManager(token, r == null ? null : r.activity);
            }
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = appToken;
            args.arg2 = data;
            ActivityThread.this.sendMessage(161, args, token);
        }

        public void scheduleTransaction(ClientTransaction transaction) throws RemoteException {
            ActivityThread.this.scheduleTransaction(transaction);
        }
    }

    private native void nDumpGraphicsInfo(FileDescriptor fileDescriptor);

    public void updatePendingConfiguration(Configuration config) {
        this.mAppThread.updatePendingConfiguration(updateConfig(config));
    }

    public void updateProcessState(int processState, boolean fromIpc) {
        this.mAppThread.updateProcessState(processState, fromIpc);
    }

    private void sendPreloadMessage(int msgWhat, Object data, long delayTime) {
        synchronized (mPreloadLock) {
            if (this.mPreloadHandler == null) {
                this.mPreloadHandlerThread = new HandlerThread("queued-work-looper", 10);
                this.mPreloadHandlerThread.start();
                this.mPreloadHandler = new PreloadThreadHandler(this.mPreloadHandlerThread.getLooper());
            }
            this.mPreloadHandler.sendMessageDelayed(this.mPreloadHandler.obtainMessage(msgWhat, data), delayTime);
        }
    }

    public static ActivityThread currentActivityThread() {
        return sCurrentActivityThread;
    }

    public static boolean isSystem() {
        return sCurrentActivityThread != null ? sCurrentActivityThread.mSystemThread : false;
    }

    public static String currentOpPackageName() {
        ActivityThread am = currentActivityThread();
        return (am == null || am.getApplication() == null) ? null : am.getApplication().getOpPackageName();
    }

    public static String currentPackageName() {
        ActivityThread am = currentActivityThread();
        return (am == null || am.mBoundApplication == null) ? null : am.mBoundApplication.appInfo.packageName;
    }

    public static String currentProcessName() {
        ActivityThread am = currentActivityThread();
        return (am == null || am.mBoundApplication == null) ? null : am.mBoundApplication.processName;
    }

    public static Application currentApplication() {
        ActivityThread am = currentActivityThread();
        return am != null ? am.mInitialApplication : null;
    }

    public static String currentActivityName() {
        ActivityThread am = currentActivityThread();
        return am != null ? am.mCurrentActivity : null;
    }

    public static boolean isWechatScanOpt() {
        String name;
        boolean isOptEnabled = false;
        boolean isWechatScanOpt = false;
        if (HwFrameworkFactory.getHwActivityThread() != null) {
            isOptEnabled = HwFrameworkFactory.getHwActivityThread().getWechatScanOpt();
        }
        if (isOptEnabled) {
            name = HwFrameworkFactory.getHwActivityThread().getWechatScanActivity();
            isWechatScanOpt = name != null ? name.equals(currentActivityName()) : false;
        }
        name = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isOptEnabled=");
        stringBuilder.append(isOptEnabled);
        stringBuilder.append(" isWechatScanOpt=");
        stringBuilder.append(isWechatScanOpt);
        Slog.d(name, stringBuilder.toString());
        return isWechatScanOpt;
    }

    public static IContentSensorManager getContentSensorManager() {
        return sContentSensorManager;
    }

    public static void setContentSensorManager(IContentSensorManager contentSensorManager) {
        sContentSensorManager = contentSensorManager;
    }

    public static IPackageManager getPackageManager() {
        if (sPackageManager != null) {
            return sPackageManager;
        }
        sPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        return sPackageManager;
    }

    Configuration applyConfigCompatMainThread(int displayDensity, Configuration config, CompatibilityInfo compat) {
        if (config == null) {
            return null;
        }
        if (!compat.supportsScreen()) {
            this.mMainThreadConfig.setTo(config);
            config = this.mMainThreadConfig;
            compat.applyToConfiguration(displayDensity, config);
        }
        return config;
    }

    Resources getTopLevelResources(String resDir, String[] splitResDirs, String[] overlayDirs, String[] libDirs, int displayId, LoadedApk pkgInfo) {
        return this.mResourcesManager.getResources(null, resDir, splitResDirs, overlayDirs, libDirs, displayId, null, pkgInfo.getCompatibilityInfo(), pkgInfo.getClassLoader());
    }

    final Handler getHandler() {
        return this.mH;
    }

    public final LoadedApk getPackageInfo(String packageName, CompatibilityInfo compatInfo, int flags) {
        return getPackageInfo(packageName, compatInfo, flags, UserHandle.myUserId());
    }

    /* JADX WARNING: Missing block: B:32:0x0082, code skipped:
            return r4;
     */
    /* JADX WARNING: Missing block: B:34:0x0084, code skipped:
            r1 = null;
     */
    /* JADX WARNING: Missing block: B:37:0x0090, code skipped:
            r1 = getPackageManager().getApplicationInfo(r8, 268436480, r11);
     */
    /* JADX WARNING: Missing block: B:38:0x0092, code skipped:
            if (r1 == null) goto L_0x0099;
     */
    /* JADX WARNING: Missing block: B:40:0x0098, code skipped:
            return getPackageInfo(r1, r9, r10);
     */
    /* JADX WARNING: Missing block: B:41:0x0099, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:42:0x009a, code skipped:
            r2 = move-exception;
     */
    /* JADX WARNING: Missing block: B:44:0x009f, code skipped:
            throw r2.rethrowFromSystemServer();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final LoadedApk getPackageInfo(String packageName, CompatibilityInfo compatInfo, int flags, int userId) {
        boolean differentUser = UserHandle.myUserId() != userId;
        synchronized (this.mResourcesManager) {
            WeakReference<LoadedApk> ref;
            if (differentUser) {
                ref = null;
            } else if ((flags & 1) != 0) {
                try {
                    ref = (WeakReference) this.mPackages.get(packageName);
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            } else {
                ref = (WeakReference) this.mResourcePackages.get(packageName);
            }
            LoadedApk packageInfo = ref != null ? (LoadedApk) ref.get() : null;
            if (packageInfo == null || !(packageInfo.mResources == null || packageInfo.mResources.getAssets().isUpToDate())) {
            } else if (packageInfo.isSecurityViolation()) {
                if ((flags & 2) == 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Requesting code from ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" to be run in process ");
                    stringBuilder.append(this.mBoundApplication.processName);
                    stringBuilder.append("/");
                    stringBuilder.append(this.mBoundApplication.appInfo.uid);
                    throw new SecurityException(stringBuilder.toString());
                }
            }
        }
    }

    public final LoadedApk getPackageInfo(ApplicationInfo ai, CompatibilityInfo compatInfo, int flags) {
        boolean includeCode = (flags & 1) != 0;
        boolean z = includeCode && ai.uid != 0 && ai.uid != 1000 && (this.mBoundApplication == null || !UserHandle.isSameApp(ai.uid, this.mBoundApplication.appInfo.uid));
        boolean securityViolation = z;
        boolean registerPackage = includeCode && (1073741824 & flags) != 0;
        if ((flags & 3) != 1 || !securityViolation) {
            return getPackageInfo(ai, compatInfo, null, securityViolation, includeCode, registerPackage);
        }
        String msg = new StringBuilder();
        msg.append("Requesting code from ");
        msg.append(ai.packageName);
        msg.append(" (with uid ");
        msg.append(ai.uid);
        msg.append(")");
        msg = msg.toString();
        if (this.mBoundApplication != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(" to be run in process ");
            stringBuilder.append(this.mBoundApplication.processName);
            stringBuilder.append(" (with uid ");
            stringBuilder.append(this.mBoundApplication.appInfo.uid);
            stringBuilder.append(")");
            msg = stringBuilder.toString();
        }
        throw new SecurityException(msg);
    }

    public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai, CompatibilityInfo compatInfo) {
        return getPackageInfo(ai, compatInfo, null, false, true, false);
    }

    public final LoadedApk peekPackageInfo(String packageName, boolean includeCode) {
        LoadedApk loadedApk;
        synchronized (this.mResourcesManager) {
            WeakReference<LoadedApk> ref;
            if (includeCode) {
                try {
                    ref = (WeakReference) this.mPackages.get(packageName);
                } catch (Throwable th) {
                }
            } else {
                ref = (WeakReference) this.mResourcePackages.get(packageName);
            }
            loadedApk = ref != null ? (LoadedApk) ref.get() : null;
        }
        return loadedApk;
    }

    private LoadedApk getPackageInfo(ApplicationInfo aInfo, CompatibilityInfo compatInfo, ClassLoader baseLoader, boolean securityViolation, boolean includeCode, boolean registerPackage) {
        LoadedApk packageInfo;
        ApplicationInfo applicationInfo = aInfo;
        CompatibilityInfo compatibilityInfo = compatInfo;
        boolean differentUser = UserHandle.myUserId() != UserHandle.getUserId(applicationInfo.uid);
        synchronized (this.mResourcesManager) {
            WeakReference<LoadedApk> ref;
            if (differentUser) {
                ref = null;
            } else if (includeCode) {
                try {
                    ref = (WeakReference) this.mPackages.get(applicationInfo.packageName);
                } catch (Throwable th) {
                }
            } else {
                ref = (WeakReference) this.mResourcePackages.get(applicationInfo.packageName);
            }
            String str = null;
            packageInfo = ref != null ? (LoadedApk) ref.get() : null;
            if (packageInfo == null || !(packageInfo.mResources == null || packageInfo.mResources.getAssets().isUpToDate())) {
                if (localLOGV) {
                    String str2;
                    String str3 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    if (includeCode) {
                        str2 = "Loading code package ";
                    } else {
                        str2 = "Loading resource-only package ";
                    }
                    stringBuilder.append(str2);
                    stringBuilder.append(applicationInfo.packageName);
                    stringBuilder.append(" (in ");
                    if (this.mBoundApplication != null) {
                        str = this.mBoundApplication.processName;
                    }
                    stringBuilder.append(str);
                    stringBuilder.append(")");
                    Slog.v(str3, stringBuilder.toString());
                }
                boolean z = includeCode && (applicationInfo.flags & 4) != 0;
                packageInfo = new LoadedApk(this, applicationInfo, compatibilityInfo, baseLoader, securityViolation, z, registerPackage);
                if (this.mSystemThread && "android".equals(applicationInfo.packageName)) {
                    packageInfo.installSystemApplicationInfo(applicationInfo, getSystemContext().mPackageInfo.getClassLoader());
                }
                if (!differentUser) {
                    if (includeCode) {
                        this.mPackages.put(applicationInfo.packageName, new WeakReference(packageInfo));
                    } else {
                        this.mResourcePackages.put(applicationInfo.packageName, new WeakReference(packageInfo));
                    }
                }
            }
            if (!(compatibilityInfo == null || compatInfo.supportsScreen() || packageInfo.getCompatibilityInfo().equals(compatibilityInfo))) {
                packageInfo.setCompatibilityInfo(compatibilityInfo);
            }
        }
        return packageInfo;
    }

    ActivityThread() {
    }

    public ApplicationThread getApplicationThread() {
        return this.mAppThread;
    }

    public Instrumentation getInstrumentation() {
        return this.mInstrumentation;
    }

    public boolean isProfiling() {
        return (this.mProfiler == null || this.mProfiler.profileFile == null || this.mProfiler.profileFd != null) ? false : true;
    }

    public String getProfileFilePath() {
        return this.mProfiler.profileFile;
    }

    public Looper getLooper() {
        return this.mLooper;
    }

    public Executor getExecutor() {
        return this.mExecutor;
    }

    public Application getApplication() {
        return this.mInitialApplication;
    }

    public String getProcessName() {
        return this.mBoundApplication.processName;
    }

    public ContextImpl getSystemContext() {
        if (this.mSystemContext == null) {
            synchronized (this) {
                if (this.mSystemContext == null) {
                    this.mSystemContext = ContextImpl.createSystemContext(this);
                }
            }
        }
        return this.mSystemContext;
    }

    public ContextImpl getSystemUiContext() {
        if (this.mSystemUiContext == null) {
            synchronized (this) {
                if (this.mSystemUiContext == null) {
                    this.mSystemUiContext = ContextImpl.createSystemUiContext(getSystemContext());
                }
            }
        }
        return this.mSystemUiContext;
    }

    public void installSystemApplicationInfo(ApplicationInfo info, ClassLoader classLoader) {
        synchronized (this) {
            getSystemContext().installSystemApplicationInfo(info, classLoader);
            getSystemUiContext().installSystemApplicationInfo(info, classLoader);
            this.mProfiler = new Profiler();
        }
    }

    void ensureJitEnabled() {
        if (!this.mJitEnabled) {
            this.mJitEnabled = true;
            VMRuntime.getRuntime().startJitCompilation();
        }
    }

    void scheduleGcIdler() {
        if (!this.mGcIdlerScheduled) {
            this.mGcIdlerScheduled = true;
            Looper.myQueue().addIdleHandler(this.mGcIdler);
        }
        this.mH.removeMessages(120);
    }

    void unscheduleGcIdler() {
        if (this.mGcIdlerScheduled) {
            this.mGcIdlerScheduled = false;
            Looper.myQueue().removeIdleHandler(this.mGcIdler);
        }
        this.mH.removeMessages(120);
    }

    void doGcIfNeeded() {
        this.mGcIdlerScheduled = false;
        if (BinderInternal.getLastGcTime() + MIN_TIME_BETWEEN_GCS < SystemClock.uptimeMillis()) {
            BinderInternal.forceGc("bg");
        }
    }

    static void printRow(PrintWriter pw, String format, Object... objs) {
        pw.println(String.format(format, objs));
    }

    public static void dumpMemInfoTable(PrintWriter pw, MemoryInfo memInfo, boolean checkin, boolean dumpFullInfo, boolean dumpDalvik, boolean dumpSummaryOnly, int pid, String processName, long nativeMax, long nativeAllocated, long nativeFree, long dalvikMax, long dalvikAllocated, long dalvikFree) {
        PrintWriter printWriter = pw;
        MemoryInfo memoryInfo = memInfo;
        long j = nativeMax;
        long j2 = nativeAllocated;
        long j3 = nativeFree;
        long j4 = dalvikMax;
        long j5 = dalvikAllocated;
        long j6 = dalvikFree;
        int i = 0;
        MemoryInfo memoryInfo2;
        int i2;
        if (checkin) {
            printWriter.print(4);
            printWriter.print(',');
            printWriter.print(pid);
            printWriter.print(',');
            printWriter.print(processName);
            printWriter.print(',');
            printWriter.print(j);
            printWriter.print(',');
            printWriter.print(j4);
            printWriter.print(',');
            printWriter.print("N/A,");
            printWriter.print(j + j4);
            printWriter.print(',');
            printWriter.print(j2);
            printWriter.print(',');
            printWriter.print(j5);
            printWriter.print(',');
            printWriter.print("N/A,");
            printWriter.print(j2 + j5);
            printWriter.print(',');
            printWriter.print(j3);
            printWriter.print(',');
            printWriter.print(j6);
            printWriter.print(',');
            printWriter.print("N/A,");
            printWriter.print(j3 + j6);
            printWriter.print(',');
            memoryInfo2 = memInfo;
            printWriter.print(memoryInfo2.nativePss);
            printWriter.print(',');
            printWriter.print(memoryInfo2.dalvikPss);
            printWriter.print(',');
            printWriter.print(memoryInfo2.otherPss);
            printWriter.print(',');
            printWriter.print(memInfo.getTotalPss());
            printWriter.print(',');
            printWriter.print(memoryInfo2.nativeSwappablePss);
            printWriter.print(',');
            printWriter.print(memoryInfo2.dalvikSwappablePss);
            printWriter.print(',');
            printWriter.print(memoryInfo2.otherSwappablePss);
            printWriter.print(',');
            printWriter.print(memInfo.getTotalSwappablePss());
            printWriter.print(',');
            printWriter.print(memoryInfo2.nativeSharedDirty);
            printWriter.print(',');
            printWriter.print(memoryInfo2.dalvikSharedDirty);
            printWriter.print(',');
            printWriter.print(memoryInfo2.otherSharedDirty);
            printWriter.print(',');
            printWriter.print(memInfo.getTotalSharedDirty());
            printWriter.print(',');
            printWriter.print(memoryInfo2.nativeSharedClean);
            printWriter.print(',');
            printWriter.print(memoryInfo2.dalvikSharedClean);
            printWriter.print(',');
            printWriter.print(memoryInfo2.otherSharedClean);
            printWriter.print(',');
            printWriter.print(memInfo.getTotalSharedClean());
            printWriter.print(',');
            printWriter.print(memoryInfo2.nativePrivateDirty);
            printWriter.print(',');
            printWriter.print(memoryInfo2.dalvikPrivateDirty);
            printWriter.print(',');
            printWriter.print(memoryInfo2.otherPrivateDirty);
            printWriter.print(',');
            printWriter.print(memInfo.getTotalPrivateDirty());
            printWriter.print(',');
            printWriter.print(memoryInfo2.nativePrivateClean);
            printWriter.print(',');
            printWriter.print(memoryInfo2.dalvikPrivateClean);
            printWriter.print(',');
            printWriter.print(memoryInfo2.otherPrivateClean);
            printWriter.print(',');
            printWriter.print(memInfo.getTotalPrivateClean());
            printWriter.print(',');
            printWriter.print(memoryInfo2.nativeSwappedOut);
            printWriter.print(',');
            printWriter.print(memoryInfo2.dalvikSwappedOut);
            printWriter.print(',');
            printWriter.print(memoryInfo2.otherSwappedOut);
            printWriter.print(',');
            printWriter.print(memInfo.getTotalSwappedOut());
            printWriter.print(',');
            if (memoryInfo2.hasSwappedOutPss) {
                printWriter.print(memoryInfo2.nativeSwappedOutPss);
                printWriter.print(',');
                printWriter.print(memoryInfo2.dalvikSwappedOutPss);
                printWriter.print(',');
                printWriter.print(memoryInfo2.otherSwappedOutPss);
                printWriter.print(',');
                printWriter.print(memInfo.getTotalSwappedOutPss());
                printWriter.print(',');
            } else {
                printWriter.print("N/A,");
                printWriter.print("N/A,");
                printWriter.print("N/A,");
                printWriter.print("N/A,");
            }
            while (true) {
                i2 = i;
                if (i2 < 17) {
                    printWriter.print(MemoryInfo.getOtherLabel(i2));
                    printWriter.print(',');
                    printWriter.print(memoryInfo2.getOtherPss(i2));
                    printWriter.print(',');
                    printWriter.print(memoryInfo2.getOtherSwappablePss(i2));
                    printWriter.print(',');
                    printWriter.print(memoryInfo2.getOtherSharedDirty(i2));
                    printWriter.print(',');
                    printWriter.print(memoryInfo2.getOtherSharedClean(i2));
                    printWriter.print(',');
                    printWriter.print(memoryInfo2.getOtherPrivateDirty(i2));
                    printWriter.print(',');
                    printWriter.print(memoryInfo2.getOtherPrivateClean(i2));
                    printWriter.print(',');
                    printWriter.print(memoryInfo2.getOtherSwappedOut(i2));
                    printWriter.print(',');
                    if (memoryInfo2.hasSwappedOutPss) {
                        printWriter.print(memoryInfo2.getOtherSwappedOutPss(i2));
                        printWriter.print(',');
                    } else {
                        printWriter.print("N/A,");
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
        memoryInfo2 = memoryInfo;
        long j7;
        long j8;
        if (dumpSummaryOnly) {
            j7 = j5;
            j8 = dalvikMax;
        } else {
            String str;
            Object[] objArr;
            int i3;
            int i4;
            int myPss;
            int mySwappablePss;
            int mySharedDirty;
            int myPrivateDirty;
            int mySharedClean;
            int myPrivateClean;
            int mySwappedOut;
            int mySwappedOutPss;
            int otherSwappedOutPss;
            int otherSharedClean;
            int otherPrivateClean;
            Object[] objArr2;
            if (dumpFullInfo) {
                str = HEAP_FULL_COLUMN;
                objArr = new Object[11];
                objArr[0] = "";
                objArr[1] = "Pss";
                objArr[2] = "Pss";
                objArr[3] = "Shared";
                objArr[4] = "Private";
                objArr[5] = "Shared";
                objArr[6] = "Private";
                objArr[7] = memoryInfo2.hasSwappedOutPss ? "SwapPss" : "Swap";
                objArr[8] = "Heap";
                objArr[9] = "Heap";
                objArr[10] = "Heap";
                printRow(printWriter, str, objArr);
                printRow(printWriter, HEAP_FULL_COLUMN, "", "Total", "Clean", "Dirty", "Dirty", "Clean", "Clean", "Dirty", "Size", "Alloc", "Free");
                printRow(printWriter, HEAP_FULL_COLUMN, "", "------", "------", "------", "------", "------", "------", "------", "------", "------", "------");
                str = HEAP_FULL_COLUMN;
                objArr = new Object[11];
                objArr[0] = "Native Heap";
                objArr[1] = Integer.valueOf(memoryInfo2.nativePss);
                objArr[2] = Integer.valueOf(memoryInfo2.nativeSwappablePss);
                objArr[3] = Integer.valueOf(memoryInfo2.nativeSharedDirty);
                objArr[4] = Integer.valueOf(memoryInfo2.nativePrivateDirty);
                objArr[5] = Integer.valueOf(memoryInfo2.nativeSharedClean);
                objArr[6] = Integer.valueOf(memoryInfo2.nativePrivateClean);
                objArr[7] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? memoryInfo2.nativeSwappedOutPss : memoryInfo2.nativeSwappedOut);
                objArr[8] = Long.valueOf(nativeMax);
                objArr[9] = Long.valueOf(nativeAllocated);
                objArr[10] = Long.valueOf(nativeFree);
                printRow(printWriter, str, objArr);
                str = HEAP_FULL_COLUMN;
                objArr = new Object[11];
                objArr[0] = "Dalvik Heap";
                objArr[1] = Integer.valueOf(memoryInfo2.dalvikPss);
                objArr[2] = Integer.valueOf(memoryInfo2.dalvikSwappablePss);
                objArr[3] = Integer.valueOf(memoryInfo2.dalvikSharedDirty);
                objArr[4] = Integer.valueOf(memoryInfo2.dalvikPrivateDirty);
                objArr[5] = Integer.valueOf(memoryInfo2.dalvikSharedClean);
                objArr[6] = Integer.valueOf(memoryInfo2.dalvikPrivateClean);
                objArr[7] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? memoryInfo2.dalvikSwappedOutPss : memoryInfo2.dalvikSwappedOut);
                j8 = dalvikMax;
                objArr[8] = Long.valueOf(dalvikMax);
                objArr[9] = Long.valueOf(dalvikAllocated);
                objArr[10] = Long.valueOf(dalvikFree);
                printRow(printWriter, str, objArr);
            } else {
                j8 = dalvikMax;
                str = HEAP_COLUMN;
                objArr = new Object[8];
                objArr[0] = "";
                objArr[1] = "Pss";
                objArr[2] = "Private";
                objArr[3] = "Private";
                objArr[4] = memoryInfo2.hasSwappedOutPss ? "SwapPss" : "Swap";
                objArr[5] = "Heap";
                objArr[6] = "Heap";
                objArr[7] = "Heap";
                printRow(printWriter, str, objArr);
                printRow(printWriter, HEAP_COLUMN, "", "Total", "Dirty", "Clean", "Dirty", "Size", "Alloc", "Free");
                printRow(printWriter, HEAP_COLUMN, "", "------", "------", "------", "------", "------", "------", "------", "------");
                str = HEAP_COLUMN;
                objArr = new Object[8];
                objArr[0] = "Native Heap";
                objArr[1] = Integer.valueOf(memoryInfo2.nativePss);
                objArr[2] = Integer.valueOf(memoryInfo2.nativePrivateDirty);
                objArr[3] = Integer.valueOf(memoryInfo2.nativePrivateClean);
                if (memoryInfo2.hasSwappedOutPss) {
                    i3 = memoryInfo2.nativeSwappedOutPss;
                } else {
                    i3 = memoryInfo2.nativeSwappedOut;
                }
                objArr[4] = Integer.valueOf(i3);
                objArr[5] = Long.valueOf(nativeMax);
                objArr[6] = Long.valueOf(nativeAllocated);
                objArr[7] = Long.valueOf(nativeFree);
                printRow(printWriter, str, objArr);
                str = HEAP_COLUMN;
                Object[] objArr3 = new Object[8];
                objArr3[0] = "Dalvik Heap";
                objArr3[1] = Integer.valueOf(memoryInfo2.dalvikPss);
                objArr3[2] = Integer.valueOf(memoryInfo2.dalvikPrivateDirty);
                objArr3[3] = Integer.valueOf(memoryInfo2.dalvikPrivateClean);
                if (memoryInfo2.hasSwappedOutPss) {
                    i4 = memoryInfo2.dalvikSwappedOutPss;
                } else {
                    i4 = memoryInfo2.dalvikSwappedOut;
                }
                objArr3[4] = Integer.valueOf(i4);
                objArr3[5] = Long.valueOf(dalvikMax);
                objArr3[6] = Long.valueOf(dalvikAllocated);
                objArr3[7] = Long.valueOf(dalvikFree);
                printRow(printWriter, str, objArr3);
            }
            i2 = memoryInfo2.otherPss;
            i4 = memoryInfo2.otherSwappablePss;
            i3 = memoryInfo2.otherSharedDirty;
            int otherPrivateDirty = memoryInfo2.otherPrivateDirty;
            int otherPss = i2;
            int otherSharedClean2 = memoryInfo2.otherSharedClean;
            int otherPrivateClean2 = memoryInfo2.otherPrivateClean;
            int otherSwappedOut = memoryInfo2.otherSwappedOut;
            int otherSwappedOutPss2 = memoryInfo2.otherSwappedOutPss;
            int otherPrivateDirty2 = otherPrivateDirty;
            i2 = 0;
            int otherSharedClean3 = otherSharedClean2;
            int otherPrivateClean3 = otherPrivateClean2;
            otherPrivateDirty = i3;
            i3 = i4;
            i4 = otherPss;
            while (i2 < 17) {
                int mySharedClean2;
                myPss = memoryInfo2.getOtherPss(i2);
                mySwappablePss = memoryInfo2.getOtherSwappablePss(i2);
                mySharedDirty = memoryInfo2.getOtherSharedDirty(i2);
                myPrivateDirty = memoryInfo2.getOtherPrivateDirty(i2);
                mySharedClean = memoryInfo2.getOtherSharedClean(i2);
                myPrivateClean = memoryInfo2.getOtherPrivateClean(i2);
                mySwappedOut = memoryInfo2.getOtherSwappedOut(i2);
                mySwappedOutPss = memoryInfo2.getOtherSwappedOutPss(i2);
                if (myPss == 0 && mySharedDirty == 0 && myPrivateDirty == 0 && mySharedClean == 0 && myPrivateClean == 0) {
                    otherSwappedOutPss = otherSwappedOutPss2;
                    if ((memoryInfo2.hasSwappedOutPss != 0 ? mySwappedOutPss : mySwappedOut) == 0) {
                        otherSwappedOutPss2 = otherSwappedOutPss;
                        i2++;
                        j = nativeMax;
                        j2 = nativeAllocated;
                        j5 = dalvikAllocated;
                    }
                } else {
                    otherSwappedOutPss = otherSwappedOutPss2;
                }
                if (dumpFullInfo) {
                    String str2 = HEAP_FULL_COLUMN;
                    otherSharedClean = otherSharedClean3;
                    otherPrivateClean = otherPrivateClean3;
                    objArr2 = new Object[11];
                    objArr2[0] = MemoryInfo.getOtherLabel(i2);
                    objArr2[1] = Integer.valueOf(myPss);
                    objArr2[2] = Integer.valueOf(mySwappablePss);
                    objArr2[3] = Integer.valueOf(mySharedDirty);
                    objArr2[4] = Integer.valueOf(myPrivateDirty);
                    objArr2[5] = Integer.valueOf(mySharedClean);
                    objArr2[6] = Integer.valueOf(myPrivateClean);
                    objArr2[7] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut);
                    mySharedClean2 = mySharedClean;
                    objArr2[8] = "";
                    objArr2[9] = "";
                    objArr2[10] = "";
                    printRow(printWriter, str2, objArr2);
                } else {
                    mySharedClean2 = mySharedClean;
                    otherSharedClean = otherSharedClean3;
                    otherPrivateClean = otherPrivateClean3;
                    String str3 = HEAP_COLUMN;
                    Object[] objArr4 = new Object[8];
                    objArr4[0] = MemoryInfo.getOtherLabel(i2);
                    objArr4[1] = Integer.valueOf(myPss);
                    objArr4[2] = Integer.valueOf(myPrivateDirty);
                    objArr4[3] = Integer.valueOf(myPrivateClean);
                    objArr4[4] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut);
                    objArr4[5] = "";
                    objArr4[6] = "";
                    objArr4[7] = "";
                    printRow(printWriter, str3, objArr4);
                }
                i4 -= myPss;
                i3 -= mySwappablePss;
                otherPrivateDirty -= mySharedDirty;
                otherPrivateDirty2 -= myPrivateDirty;
                otherSharedClean3 = otherSharedClean - mySharedClean2;
                otherPrivateClean3 = otherPrivateClean - myPrivateClean;
                otherSwappedOut -= mySwappedOut;
                otherSwappedOutPss2 = otherSwappedOutPss - mySwappedOutPss;
                i2++;
                j = nativeMax;
                j2 = nativeAllocated;
                j5 = dalvikAllocated;
            }
            otherSharedClean = otherSharedClean3;
            otherPrivateClean = otherPrivateClean3;
            otherSwappedOutPss = otherSwappedOutPss2;
            if (dumpFullInfo) {
                str = HEAP_FULL_COLUMN;
                Object[] objArr5 = new Object[11];
                objArr5[0] = "Unknown";
                objArr5[1] = Integer.valueOf(i4);
                objArr5[2] = Integer.valueOf(i3);
                objArr5[3] = Integer.valueOf(otherPrivateDirty);
                objArr5[4] = Integer.valueOf(otherPrivateDirty2);
                otherSharedClean3 = otherSharedClean;
                objArr5[5] = Integer.valueOf(otherSharedClean3);
                otherPrivateClean3 = otherPrivateClean;
                objArr5[6] = Integer.valueOf(otherPrivateClean3);
                objArr5[7] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? otherSwappedOutPss : otherSwappedOut);
                objArr5[8] = "";
                objArr5[9] = "";
                objArr5[10] = "";
                printRow(printWriter, str, objArr5);
                str = HEAP_FULL_COLUMN;
                objArr5 = new Object[11];
                objArr5[0] = "TOTAL";
                objArr5[1] = Integer.valueOf(memInfo.getTotalPss());
                objArr5[2] = Integer.valueOf(memInfo.getTotalSwappablePss());
                objArr5[3] = Integer.valueOf(memInfo.getTotalSharedDirty());
                objArr5[4] = Integer.valueOf(memInfo.getTotalPrivateDirty());
                objArr5[5] = Integer.valueOf(memInfo.getTotalSharedClean());
                objArr5[6] = Integer.valueOf(memInfo.getTotalPrivateClean());
                if (memoryInfo2.hasSwappedOutPss) {
                    mySharedClean = memInfo.getTotalSwappedOutPss();
                } else {
                    mySharedClean = memInfo.getTotalSwappedOut();
                }
                objArr5[7] = Integer.valueOf(mySharedClean);
                objArr5[8] = Long.valueOf(nativeMax + j8);
                int otherPrivateDirty3 = otherPrivateDirty2;
                objArr5[9] = Long.valueOf(nativeAllocated + dalvikAllocated);
                i3 = otherPrivateClean3;
                mySharedClean = otherPrivateDirty3;
                objArr5[10] = Long.valueOf(nativeFree + dalvikFree);
                printRow(printWriter, str, objArr5);
                int i5 = mySharedClean;
                mySharedClean = nativeAllocated;
            } else {
                mySharedClean = otherPrivateDirty2;
                int i6 = i3;
                i3 = otherPrivateClean;
                int i7 = otherSharedClean;
                j2 = nativeMax;
                j3 = nativeFree;
                j6 = dalvikFree;
                j7 = dalvikAllocated;
                str = HEAP_COLUMN;
                Object[] objArr6 = new Object[8];
                objArr6[0] = "Unknown";
                objArr6[1] = Integer.valueOf(i4);
                objArr6[2] = Integer.valueOf(mySharedClean);
                objArr6[3] = Integer.valueOf(i3);
                objArr6[4] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? otherSwappedOutPss : otherSwappedOut);
                objArr6[5] = "";
                objArr6[6] = "";
                objArr6[7] = "";
                printRow(printWriter, str, objArr6);
                str = HEAP_COLUMN;
                objArr6 = new Object[8];
                objArr6[0] = "TOTAL";
                objArr6[1] = Integer.valueOf(memInfo.getTotalPss());
                objArr6[2] = Integer.valueOf(memInfo.getTotalPrivateDirty());
                objArr6[3] = Integer.valueOf(memInfo.getTotalPrivateClean());
                if (memoryInfo2.hasSwappedOutPss) {
                    myPrivateClean = memInfo.getTotalSwappedOutPss();
                } else {
                    myPrivateClean = memInfo.getTotalSwappedOut();
                }
                objArr6[4] = Integer.valueOf(myPrivateClean);
                objArr6[5] = Long.valueOf(j2 + j8);
                objArr6[6] = Long.valueOf(nativeAllocated + j7);
                objArr6[7] = Long.valueOf(j3 + j6);
                printRow(printWriter, str, objArr6);
            }
            if (dumpDalvik) {
                printWriter.println(" ");
                printWriter.println(" Dalvik Details");
                int i8 = 17;
                while (true) {
                    mySwappablePss = i8;
                    if (mySwappablePss >= 31) {
                        break;
                    }
                    int otherPss2;
                    myPss = memoryInfo2.getOtherPss(mySwappablePss);
                    mySharedDirty = memoryInfo2.getOtherSwappablePss(mySwappablePss);
                    myPrivateDirty = memoryInfo2.getOtherSharedDirty(mySwappablePss);
                    i2 = memoryInfo2.getOtherPrivateDirty(mySwappablePss);
                    mySharedClean = memoryInfo2.getOtherSharedClean(mySwappablePss);
                    myPrivateClean = memoryInfo2.getOtherPrivateClean(mySwappablePss);
                    mySwappedOut = memoryInfo2.getOtherSwappedOut(mySwappablePss);
                    mySwappedOutPss = memoryInfo2.getOtherSwappedOutPss(mySwappablePss);
                    if (myPss == 0 && myPrivateDirty == 0 && i2 == 0 && mySharedClean == 0 && myPrivateClean == 0) {
                        if ((memoryInfo2.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut) == 0) {
                            otherPss2 = i4;
                            i8 = mySwappablePss + 1;
                            i4 = otherPss2;
                            j = nativeAllocated;
                            j3 = nativeFree;
                        }
                    }
                    String str4;
                    if (dumpFullInfo) {
                        str4 = HEAP_FULL_COLUMN;
                        otherPss2 = i4;
                        objArr = new Object[11];
                        objArr[0] = MemoryInfo.getOtherLabel(mySwappablePss);
                        objArr[1] = Integer.valueOf(myPss);
                        objArr[2] = Integer.valueOf(mySharedDirty);
                        objArr[3] = Integer.valueOf(myPrivateDirty);
                        objArr[4] = Integer.valueOf(i2);
                        objArr[5] = Integer.valueOf(mySharedClean);
                        objArr[6] = Integer.valueOf(myPrivateClean);
                        objArr[7] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut);
                        objArr[8] = "";
                        objArr[9] = "";
                        objArr[10] = "";
                        printRow(printWriter, str4, objArr);
                    } else {
                        otherPss2 = i4;
                        str4 = HEAP_COLUMN;
                        objArr2 = new Object[8];
                        objArr2[0] = MemoryInfo.getOtherLabel(mySwappablePss);
                        objArr2[1] = Integer.valueOf(myPss);
                        objArr2[2] = Integer.valueOf(i2);
                        objArr2[3] = Integer.valueOf(myPrivateClean);
                        objArr2[4] = Integer.valueOf(memoryInfo2.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut);
                        objArr2[5] = "";
                        objArr2[6] = "";
                        objArr2[7] = "";
                        printRow(printWriter, str4, objArr2);
                    }
                    i8 = mySwappablePss + 1;
                    i4 = otherPss2;
                    j = nativeAllocated;
                    j3 = nativeFree;
                }
            }
        }
        printWriter.println(" ");
        printWriter.println(" App Summary");
        printRow(printWriter, ONE_COUNT_COLUMN_HEADER, "", "Pss(KB)");
        printRow(printWriter, ONE_COUNT_COLUMN_HEADER, "", "------");
        printRow(printWriter, ONE_COUNT_COLUMN, "Java Heap:", Integer.valueOf(memInfo.getSummaryJavaHeap()));
        printRow(printWriter, ONE_COUNT_COLUMN, "Native Heap:", Integer.valueOf(memInfo.getSummaryNativeHeap()));
        printRow(printWriter, ONE_COUNT_COLUMN, "Code:", Integer.valueOf(memInfo.getSummaryCode()));
        printRow(printWriter, ONE_COUNT_COLUMN, "Stack:", Integer.valueOf(memInfo.getSummaryStack()));
        printRow(printWriter, ONE_COUNT_COLUMN, "Graphics:", Integer.valueOf(memInfo.getSummaryGraphics()));
        printRow(printWriter, ONE_COUNT_COLUMN, "Private Other:", Integer.valueOf(memInfo.getSummaryPrivateOther()));
        printRow(printWriter, ONE_COUNT_COLUMN, "System:", Integer.valueOf(memInfo.getSummarySystem()));
        printWriter.println(" ");
        if (memoryInfo2.hasSwappedOutPss) {
            printRow(printWriter, TWO_COUNT_COLUMNS, "TOTAL:", Integer.valueOf(memInfo.getSummaryTotalPss()), "TOTAL SWAP PSS:", Integer.valueOf(memInfo.getSummaryTotalSwapPss()));
        } else {
            printRow(printWriter, TWO_COUNT_COLUMNS, "TOTAL:", Integer.valueOf(memInfo.getSummaryTotalPss()), "TOTAL SWAP (KB):", Integer.valueOf(memInfo.getSummaryTotalSwap()));
        }
    }

    private static void dumpMemoryInfo(ProtoOutputStream proto, long fieldId, String name, int pss, int cleanPss, int sharedDirty, int privateDirty, int sharedClean, int privateClean, boolean hasSwappedOutPss, int dirtySwap, int dirtySwapPss) {
        ProtoOutputStream protoOutputStream = proto;
        long token = proto.start(fieldId);
        protoOutputStream.write(1138166333441L, name);
        protoOutputStream.write(1120986464258L, pss);
        protoOutputStream.write(1120986464259L, cleanPss);
        protoOutputStream.write(1120986464260L, sharedDirty);
        protoOutputStream.write(1120986464261L, privateDirty);
        protoOutputStream.write(1120986464262L, sharedClean);
        protoOutputStream.write(1120986464263L, privateClean);
        if (hasSwappedOutPss) {
            protoOutputStream.write(NotificationChannelProto.LIGHT_COLOR, dirtySwapPss);
            int i = dirtySwap;
        } else {
            int i2 = dirtySwapPss;
            protoOutputStream.write(1120986464264L, dirtySwap);
        }
        protoOutputStream.end(token);
    }

    public static void dumpMemInfoTable(ProtoOutputStream proto, MemoryInfo memInfo, boolean dumpDalvik, boolean dumpSummaryOnly, long nativeMax, long nativeAllocated, long nativeFree, long dalvikMax, long dalvikAllocated, long dalvikFree) {
        long nhToken;
        ProtoOutputStream protoOutputStream = proto;
        MemoryInfo memoryInfo = memInfo;
        long j = nativeMax;
        long j2 = nativeAllocated;
        long j3 = nativeFree;
        long j4 = dalvikMax;
        long j5 = dalvikAllocated;
        long j6 = dalvikFree;
        long j7;
        long j8;
        long j9;
        long j10;
        long j11;
        if (dumpSummaryOnly) {
            j7 = j5;
            j8 = j4;
            j9 = j3;
            j10 = j2;
            long j12 = j;
            j11 = dalvikFree;
        } else {
            int myPrivateClean;
            int mySwappedOut;
            int mySwappedOutPss;
            int i;
            j6 = protoOutputStream.start(1146756268035L);
            int i2 = memoryInfo.nativePss;
            long nhToken2 = j6;
            int i3 = memoryInfo.nativeSwappablePss;
            int i4 = memoryInfo.nativeSharedDirty;
            int i5 = i3;
            int i6 = memoryInfo.nativePrivateDirty;
            int i7 = memoryInfo.nativeSharedClean;
            int i8 = memoryInfo.nativePrivateClean;
            boolean z = memoryInfo.hasSwappedOutPss;
            i3 = memoryInfo.nativeSwappedOut;
            int i9 = memoryInfo.nativeSwappedOutPss;
            j11 = nhToken2;
            long j13 = j2;
            long j14 = nativeMax;
            dumpMemoryInfo(protoOutputStream, 1146756268033L, "Native Heap", i2, i5, i4, i6, i7, i8, z, i3, i9);
            protoOutputStream.write(1120986464258L, j14);
            protoOutputStream.write(1120986464259L, nativeAllocated);
            j3 = nativeFree;
            protoOutputStream.write(1120986464260L, j3);
            nhToken = j11;
            protoOutputStream.end(nhToken);
            j5 = protoOutputStream.start(1146756268036L);
            MemoryInfo memoryInfo2 = memInfo;
            i4 = memoryInfo2.dalvikPss;
            memoryInfo = memoryInfo2;
            long dvToken = j5;
            int i10 = i4;
            j9 = j3;
            j10 = nativeAllocated;
            dumpMemoryInfo(protoOutputStream, 1146756268033L, "Dalvik Heap", i10, memoryInfo2.dalvikSwappablePss, memoryInfo2.dalvikSharedDirty, memoryInfo2.dalvikPrivateDirty, memoryInfo2.dalvikSharedClean, memoryInfo2.dalvikPrivateClean, memoryInfo2.hasSwappedOutPss, memoryInfo2.dalvikSwappedOut, memoryInfo2.dalvikSwappedOutPss);
            j = dalvikMax;
            protoOutputStream.write(1120986464258L, j);
            j2 = dalvikAllocated;
            protoOutputStream.write(1120986464259L, j2);
            j4 = dalvikFree;
            protoOutputStream.write(1120986464260L, j4);
            j6 = dvToken;
            protoOutputStream.end(j6);
            i2 = memoryInfo.otherPss;
            int otherSwappablePss = memoryInfo.otherSwappablePss;
            int otherSharedDirty = memoryInfo.otherSharedDirty;
            i10 = memoryInfo.otherPrivateDirty;
            int otherSharedClean = memoryInfo.otherSharedClean;
            int otherPrivateClean = memoryInfo.otherPrivateClean;
            int otherPss = i2;
            int otherSwappedOut = memoryInfo.otherSwappedOut;
            int i11 = 0;
            int otherSwappedOutPss = memoryInfo.otherSwappedOutPss;
            int otherSharedDirty2 = otherSharedDirty;
            int otherPrivateDirty = i10;
            int otherSharedClean2 = otherSharedClean;
            int otherPrivateClean2 = otherPrivateClean;
            while (true) {
                otherPrivateClean = i11;
                if (otherPrivateClean >= 17) {
                    break;
                }
                long dvToken2;
                i11 = memoryInfo.getOtherPss(otherPrivateClean);
                i5 = memoryInfo.getOtherSwappablePss(otherPrivateClean);
                i6 = memoryInfo.getOtherSharedDirty(otherPrivateClean);
                i7 = memoryInfo.getOtherPrivateDirty(otherPrivateClean);
                i8 = memoryInfo.getOtherSharedClean(otherPrivateClean);
                myPrivateClean = memoryInfo.getOtherPrivateClean(otherPrivateClean);
                mySwappedOut = memoryInfo.getOtherSwappedOut(otherPrivateClean);
                mySwappedOutPss = memoryInfo.getOtherSwappedOutPss(otherPrivateClean);
                if (i11 == 0 && i6 == 0 && i7 == 0 && i8 == 0 && myPrivateClean == 0) {
                    if ((memoryInfo.hasSwappedOutPss ? mySwappedOutPss : mySwappedOut) == 0) {
                        dvToken2 = j6;
                        j11 = j4;
                        i = otherPrivateClean;
                        j7 = j2;
                        j8 = j;
                        i11 = i + 1;
                        j6 = dvToken2;
                        j4 = j11;
                        j2 = j7;
                        j = j8;
                    }
                }
                dvToken2 = j6;
                j11 = j4;
                i = otherPrivateClean;
                j7 = j2;
                j8 = j;
                dumpMemoryInfo(protoOutputStream, 2246267895813L, MemoryInfo.getOtherLabel(otherPrivateClean), i11, i5, i6, i7, i8, myPrivateClean, memoryInfo.hasSwappedOutPss, mySwappedOut, mySwappedOutPss);
                otherPss -= i11;
                otherSwappablePss -= i5;
                otherSharedDirty2 -= i6;
                otherPrivateDirty -= i7;
                otherSharedClean2 -= i8;
                otherPrivateClean2 -= myPrivateClean;
                otherSwappedOut -= mySwappedOut;
                otherSwappedOutPss -= mySwappedOutPss;
                i11 = i + 1;
                j6 = dvToken2;
                j4 = j11;
                j2 = j7;
                j = j8;
            }
            j11 = j4;
            j7 = j2;
            j8 = j;
            i11 = 17;
            dumpMemoryInfo(protoOutputStream, RemoteAnimationTargetProto.CONTENT_INSETS, "Unknown", otherPss, otherSwappablePss, otherSharedDirty2, otherPrivateDirty, otherSharedClean2, otherPrivateClean2, memoryInfo.hasSwappedOutPss, otherSwappedOut, otherSwappedOutPss);
            long tToken = protoOutputStream.start(IntentProto.COMPONENT);
            dumpMemoryInfo(protoOutputStream, 1146756268033L, "TOTAL", memInfo.getTotalPss(), memInfo.getTotalSwappablePss(), memInfo.getTotalSharedDirty(), memInfo.getTotalPrivateDirty(), memInfo.getTotalSharedClean(), memInfo.getTotalPrivateClean(), memoryInfo.hasSwappedOutPss, memInfo.getTotalSwappedOut(), memInfo.getTotalSwappedOutPss());
            protoOutputStream.write(1120986464258L, nativeMax + j8);
            protoOutputStream.write(1120986464259L, j10 + j7);
            protoOutputStream.write(1120986464260L, j9 + j11);
            j3 = tToken;
            protoOutputStream.end(j3);
            if (dumpDalvik) {
                while (true) {
                    int i12 = i11;
                    if (i12 >= 31) {
                        break;
                    }
                    int i13;
                    long tToken2;
                    i11 = memoryInfo.getOtherPss(i12);
                    i8 = memoryInfo.getOtherSwappablePss(i12);
                    myPrivateClean = memoryInfo.getOtherSharedDirty(i12);
                    mySwappedOut = memoryInfo.getOtherPrivateDirty(i12);
                    mySwappedOutPss = memoryInfo.getOtherSharedClean(i12);
                    int myPrivateClean2 = memoryInfo.getOtherPrivateClean(i12);
                    i = memoryInfo.getOtherSwappedOut(i12);
                    int mySwappedOutPss2 = memoryInfo.getOtherSwappedOutPss(i12);
                    if (i11 == 0 && myPrivateClean == 0 && mySwappedOut == 0 && mySwappedOutPss == 0 && myPrivateClean2 == 0) {
                        if ((memoryInfo.hasSwappedOutPss ? mySwappedOutPss2 : i) == 0) {
                            i13 = i12;
                            tToken2 = j3;
                            i11 = i13 + 1;
                            j3 = tToken2;
                        }
                    }
                    i13 = i12;
                    tToken2 = j3;
                    dumpMemoryInfo(protoOutputStream, 2246267895816L, MemoryInfo.getOtherLabel(i12), i11, i8, myPrivateClean, mySwappedOut, mySwappedOutPss, myPrivateClean2, memoryInfo.hasSwappedOutPss, i, mySwappedOutPss2);
                    i11 = i13 + 1;
                    j3 = tToken2;
                }
            }
        }
        nhToken = protoOutputStream.start(RemoteAnimationTargetProto.SOURCE_CONTAINER_BOUNDS);
        protoOutputStream.write(1120986464257L, memInfo.getSummaryJavaHeap());
        protoOutputStream.write(1120986464258L, memInfo.getSummaryNativeHeap());
        protoOutputStream.write(1120986464259L, memInfo.getSummaryCode());
        protoOutputStream.write(1120986464260L, memInfo.getSummaryStack());
        protoOutputStream.write(1120986464261L, memInfo.getSummaryGraphics());
        protoOutputStream.write(1120986464262L, memInfo.getSummaryPrivateOther());
        protoOutputStream.write(1120986464263L, memInfo.getSummarySystem());
        if (memoryInfo.hasSwappedOutPss) {
            protoOutputStream.write(1120986464264L, memInfo.getSummaryTotalSwapPss());
        } else {
            protoOutputStream.write(1120986464264L, memInfo.getSummaryTotalSwap());
        }
        protoOutputStream.end(nhToken);
    }

    public void registerOnActivityPausedListener(Activity activity, OnActivityPausedListener listener) {
        synchronized (this.mOnPauseListeners) {
            ArrayList<OnActivityPausedListener> list = (ArrayList) this.mOnPauseListeners.get(activity);
            if (list == null) {
                list = new ArrayList();
                this.mOnPauseListeners.put(activity, list);
            }
            list.add(listener);
        }
    }

    public void unregisterOnActivityPausedListener(Activity activity, OnActivityPausedListener listener) {
        synchronized (this.mOnPauseListeners) {
            ArrayList<OnActivityPausedListener> list = (ArrayList) this.mOnPauseListeners.get(activity);
            if (list != null) {
                list.remove(listener);
            }
        }
    }

    public final ActivityInfo resolveActivityInfo(Intent intent) {
        ActivityInfo aInfo = intent.resolveActivityInfo(this.mInitialApplication.getPackageManager(), 1024);
        if (aInfo == null) {
            Instrumentation.checkStartActivityResult(-92, intent);
        }
        return aInfo;
    }

    public final Activity startActivityNow(Activity parent, String id, Intent intent, ActivityInfo activityInfo, IBinder token, Bundle state, NonConfigurationInstances lastNonConfigurationInstances) {
        ActivityClientRecord r = new ActivityClientRecord();
        r.token = token;
        r.ident = 0;
        r.intent = intent;
        r.state = state;
        r.parent = parent;
        r.embeddedID = id;
        r.activityInfo = activityInfo;
        r.lastNonConfigurationInstances = lastNonConfigurationInstances;
        if (localLOGV) {
            String name;
            ComponentName compname = intent.getComponent();
            if (compname != null) {
                name = compname.toShortString();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("(Intent ");
                stringBuilder.append(intent);
                stringBuilder.append(").getComponent() returned null");
                name = stringBuilder.toString();
            }
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Performing launch: action=");
            stringBuilder2.append(intent.getAction());
            stringBuilder2.append(", comp=");
            stringBuilder2.append(name);
            stringBuilder2.append(", token=");
            stringBuilder2.append(token);
            Slog.v(str, stringBuilder2.toString());
        }
        return performLaunchActivity(r, null);
    }

    public final Activity getActivity(IBinder token) {
        return ((ActivityClientRecord) this.mActivities.get(token)).activity;
    }

    public ActivityClientRecord getActivityClient(IBinder token) {
        return (ActivityClientRecord) this.mActivities.get(token);
    }

    public final void sendActivityResult(IBinder token, String id, int requestCode, int resultCode, Intent data) {
        if (DEBUG_RESULTS) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendActivityResult: id=");
            stringBuilder.append(id);
            stringBuilder.append(" req=");
            stringBuilder.append(requestCode);
            stringBuilder.append(" res=");
            stringBuilder.append(resultCode);
            stringBuilder.append(" data=");
            stringBuilder.append(data);
            Slog.v(str, stringBuilder.toString());
        }
        ArrayList<ResultInfo> list = new ArrayList();
        list.add(new ResultInfo(id, requestCode, resultCode, data));
        ClientTransaction clientTransaction = ClientTransaction.obtain(this.mAppThread, token);
        clientTransaction.addCallback(ActivityResultItem.obtain(list));
        try {
            this.mAppThread.scheduleTransaction(clientTransaction);
        } catch (RemoteException e) {
        }
    }

    TransactionExecutor getTransactionExecutor() {
        return this.mTransactionExecutor;
    }

    void sendMessage(int what, Object obj) {
        sendMessage(what, obj, 0, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1) {
        sendMessage(what, obj, arg1, 0, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2) {
        sendMessage(what, obj, arg1, arg2, false);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2, boolean async) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        if (async) {
            msg.setAsynchronous(true);
        }
        this.mH.sendMessage(msg);
    }

    private void sendMessage(int what, Object obj, int arg1, int arg2, int seq) {
        Message msg = Message.obtain();
        msg.what = what;
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = obj;
        args.argi1 = arg1;
        args.argi2 = arg2;
        args.argi3 = seq;
        msg.obj = args;
        this.mH.sendMessage(msg);
    }

    final void scheduleContextCleanup(ContextImpl context, String who, String what) {
        ContextCleanupInfo cci = new ContextCleanupInfo();
        cci.context = context;
        cci.who = who;
        cci.what = what;
        sendMessage(119, cci);
    }

    /* JADX WARNING: Removed duplicated region for block: B:111:0x027e  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x027e  */
    /* JADX WARNING: Removed duplicated region for block: B:106:0x026a A:{ExcHandler: Exception (e java.lang.Exception), Splitter:B:18:0x0087} */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x027e  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x027e  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x027e  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x027e  */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x027e  */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:106:0x026a, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:107:0x026b, code skipped:
            r1 = r5;
            r26 = r8;
            r28 = r9;
            r20 = r12;
            r2 = r13;
            r3 = r14;
            r4 = r15;
     */
    /* JADX WARNING: Missing block: B:113:0x02a3, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:114:0x02a4, code skipped:
            r1 = r5;
            r26 = r8;
            r7 = r9;
            r20 = r12;
            r2 = r13;
            r3 = r14;
            r4 = r15;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Activity performLaunchActivity(ActivityClientRecord r, Intent customIntent) {
        Exception e;
        ComponentName component;
        ActivityInfo activityInfo;
        Intent intent;
        ActivityClientRecord activityClientRecord;
        StringBuilder stringBuilder;
        Context context;
        ComponentName component2;
        ActivityClientRecord activityClientRecord2 = r;
        Intent intent2 = customIntent;
        ActivityInfo aInfo = activityClientRecord2.activityInfo;
        if (activityClientRecord2.packageInfo == null) {
            activityClientRecord2.packageInfo = getPackageInfo(aInfo.applicationInfo, activityClientRecord2.compatInfo, 1);
        }
        ComponentName component3 = activityClientRecord2.intent.getComponent();
        if (component3 == null) {
            component3 = activityClientRecord2.intent.resolveActivity(this.mInitialApplication.getPackageManager());
            activityClientRecord2.intent.setComponent(component3);
        }
        if (activityClientRecord2.activityInfo.targetActivity != null) {
            component3 = new ComponentName(activityClientRecord2.activityInfo.packageName, activityClientRecord2.activityInfo.targetActivity);
        }
        ComponentName component4 = component3;
        Context appContext = createBaseContextForActivity(r);
        Activity activity = null;
        try {
            ClassLoader cl = appContext.getClassLoader();
            activity = this.mInstrumentation.newActivity(cl, component4.getClassName(), activityClientRecord2.intent);
            this.mCurrentActivity = component4.getClassName();
            StrictMode.incrementExpectedActivityCount(activity.getClass());
            activityClientRecord2.intent.setExtrasClassLoader(cl);
            activityClientRecord2.intent.prepareToEnterProcess();
            if (activityClientRecord2.state != null) {
                activityClientRecord2.state.setClassLoader(cl);
            }
        } catch (Exception e2) {
            if (!this.mInstrumentation.onException(activity, e2)) {
                component = component4;
                activityInfo = aInfo;
                intent = intent2;
                activityClientRecord = activityClientRecord2;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to instantiate activity ");
                stringBuilder.append(component);
                stringBuilder.append(": ");
                stringBuilder.append(e2.toString());
                throw new RuntimeException(stringBuilder.toString(), e2);
            }
        }
        Activity activity2 = activity;
        try {
            ActivityThread activityThread = null;
            Application app = activityClientRecord2.packageInfo.makeApplication(false, this.mInstrumentation);
            if (activity2 != null) {
                Application app2;
                Activity activity3;
                Activity activity4;
                ActivityInfo activityInfo2;
                Activity activity5;
                String str;
                NonConfigurationInstances nonConfigurationInstances;
                CharSequence title = activityClientRecord2.activityInfo.loadLabel(appContext.getPackageManager());
                Configuration config = new Configuration(this.mCompatConfiguration);
                if (activityClientRecord2.overrideConfig != null) {
                    try {
                        config.updateFrom(activityClientRecord2.overrideConfig);
                    } catch (SuperNotCalledException e3) {
                        e2 = e3;
                        activity = activity2;
                        context = appContext;
                        component = component4;
                        activityInfo = aInfo;
                        intent = intent2;
                        activityClientRecord = activityClientRecord2;
                    } catch (Exception e4) {
                        e2 = e4;
                        activity = activity2;
                        context = appContext;
                        component2 = component4;
                        activityInfo = aInfo;
                        intent = intent2;
                        activityClientRecord = activityClientRecord2;
                        activityThread = this;
                        if (!activityThread.mInstrumentation.onException(activity, e2)) {
                        }
                        return activity;
                    }
                }
                if (localLOGV) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Performing launch of ");
                    stringBuilder2.append(activityClientRecord2);
                    stringBuilder2.append(" with config ");
                    stringBuilder2.append(config);
                    Slog.v(str2, stringBuilder2.toString());
                }
                Window window = null;
                if (activityClientRecord2.mPendingRemoveWindow != null) {
                    if (activityClientRecord2.mPreserveWindow) {
                        window = activityClientRecord2.mPendingRemoveWindow;
                        activityClientRecord2.mPendingRemoveWindow = null;
                        activityClientRecord2.mPendingRemoveWindowManager = null;
                    }
                }
                Window window2 = window;
                appContext.setOuterContext(activity2);
                Instrumentation instrumentation = getInstrumentation();
                IBinder iBinder = activityClientRecord2.token;
                int i = activityClientRecord2.ident;
                activity = activityClientRecord2.intent;
                ActivityInfo activityInfo3 = activityClientRecord2.activityInfo;
                ActivityInfo aInfo2 = aInfo;
                try {
                    Activity activity6 = activityClientRecord2.parent;
                    try {
                        String str3 = activityClientRecord2.embeddedID;
                        app2 = app;
                        NonConfigurationInstances nonConfigurationInstances2 = activityClientRecord2.lastNonConfigurationInstances;
                        activity3 = activity;
                        activity = activity2;
                        activityThread = instrumentation;
                        activity4 = activity2;
                        component2 = component4;
                        activityInfo2 = activityInfo3;
                        activity5 = activity6;
                        str = str3;
                        nonConfigurationInstances = nonConfigurationInstances2;
                        ActivityClientRecord activityClientRecord3 = activityClientRecord2;
                    } catch (SuperNotCalledException e5) {
                        e2 = e5;
                        activity = activity2;
                        context = appContext;
                        activityClientRecord = activityClientRecord2;
                        activityInfo = aInfo2;
                        intent = customIntent;
                        component = component4;
                        throw e2;
                    } catch (Exception e6) {
                        e2 = e6;
                        activity = activity2;
                        context = appContext;
                        component2 = component4;
                        activityClientRecord = activityClientRecord2;
                        activityThread = this;
                        activityInfo = aInfo2;
                        intent = customIntent;
                        if (activityThread.mInstrumentation.onException(activity, e2)) {
                        }
                        return activity;
                    }
                } catch (SuperNotCalledException e7) {
                    e2 = e7;
                    activity = activity2;
                    context = appContext;
                    intent = intent2;
                    activityClientRecord = activityClientRecord2;
                    activityInfo = aInfo2;
                    component = component4;
                    throw e2;
                } catch (Exception e8) {
                    e2 = e8;
                    activity = activity2;
                    context = appContext;
                    component2 = component4;
                    intent = intent2;
                    activityClientRecord = activityClientRecord2;
                    activityThread = this;
                    activityInfo = aInfo2;
                    if (activityThread.mInstrumentation.onException(activity, e2)) {
                    }
                    return activity;
                }
                try {
                    activity.attach(appContext, this, activityThread, iBinder, i, app2, activity3, activityInfo2, title, activity5, str, nonConfigurationInstances, config, activityClientRecord2.referrer, activityClientRecord2.voiceInteractor, window2, activityClientRecord2.configCallback);
                    intent = customIntent;
                    if (intent != null) {
                        activity = activity4;
                        try {
                            activity.mIntent = intent;
                        } catch (SuperNotCalledException e9) {
                            e2 = e9;
                            component = component2;
                            activityClientRecord = r;
                        } catch (Exception e10) {
                            e2 = e10;
                            activityThread = this;
                            activityClientRecord = r;
                            if (activityThread.mInstrumentation.onException(activity, e2)) {
                            }
                            return activity;
                        }
                    }
                    activity = activity4;
                    activityClientRecord = r;
                    activityThread = null;
                    try {
                        activityClientRecord.lastNonConfigurationInstances = null;
                        checkAndBlockForNetworkAccess();
                        activity.mStartedActivity = false;
                        activity2 = activityClientRecord.activityInfo.getThemeResource();
                        if (activity2 != null) {
                            activity.setTheme(activity2);
                        }
                        activity.mCalled = false;
                        Slog.v(TAG, "callActivityOnCreate");
                        if (r.isPersistable()) {
                            activityThread = this;
                            try {
                                activityThread.mInstrumentation.callActivityOnCreate(activity, activityClientRecord.state, activityClientRecord.persistentState);
                            } catch (SuperNotCalledException e11) {
                                e2 = e11;
                                throw e2;
                            } catch (Exception e12) {
                                e2 = e12;
                                if (activityThread.mInstrumentation.onException(activity, e2)) {
                                }
                                return activity;
                            }
                        }
                        activityThread = this;
                        activityThread.mInstrumentation.callActivityOnCreate(activity, activityClientRecord.state);
                        if (activity.mCalled) {
                            activityClientRecord.activity = activity;
                        } else {
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Activity ");
                            stringBuilder3.append(activityClientRecord.intent.getComponent().toShortString());
                            stringBuilder3.append(" did not call through to super.onCreate()");
                            throw new SuperNotCalledException(stringBuilder3.toString());
                        }
                    } catch (SuperNotCalledException e13) {
                        e2 = e13;
                        throw e2;
                    } catch (Exception e14) {
                        e2 = e14;
                        activityThread = this;
                        if (activityThread.mInstrumentation.onException(activity, e2)) {
                        }
                        return activity;
                    }
                } catch (SuperNotCalledException e15) {
                    e2 = e15;
                    activity = activity4;
                    intent = customIntent;
                    activityClientRecord = r;
                    component = component2;
                    throw e2;
                } catch (Exception e16) {
                    e2 = e16;
                    activityThread = this;
                    activity = activity4;
                    intent = customIntent;
                    activityClientRecord = r;
                    if (activityThread.mInstrumentation.onException(activity, e2)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to start activity ");
                        stringBuilder.append(component2);
                        stringBuilder.append(": ");
                        stringBuilder.append(e2.toString());
                        throw new RuntimeException(stringBuilder.toString(), e2);
                    }
                    return activity;
                }
            }
            activity = activity2;
            context = appContext;
            component2 = component4;
            activityInfo = aInfo;
            intent = intent2;
            activityClientRecord = activityClientRecord2;
            activityThread = this;
            activityClientRecord.setState(1);
            activityThread.mActivities.put(activityClientRecord.token, activityClientRecord);
            String str4 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("add activity client record, r= ");
            stringBuilder4.append(activityClientRecord);
            stringBuilder4.append(" token= ");
            stringBuilder4.append(activityClientRecord.token);
            Slog.d(str4, stringBuilder4.toString());
        } catch (SuperNotCalledException e17) {
            e2 = e17;
            activity = activity2;
            context = appContext;
            activityInfo = aInfo;
            intent = intent2;
            activityClientRecord = activityClientRecord2;
            component = component4;
            throw e2;
        } catch (Exception e18) {
        }
        return activity;
    }

    public void handleStartActivity(ActivityClientRecord r, PendingTransactionActions pendingActions) {
        Activity activity = r.activity;
        if (r.activity != null) {
            if (!r.stopped) {
                throw new IllegalStateException("Can't start activity that is not stopped.");
            } else if (!r.activity.mFinished) {
                activity.performStart("handleStartActivity");
                r.setState(2);
                if (pendingActions != null) {
                    if (pendingActions.shouldRestoreInstanceState()) {
                        if (r.isPersistable()) {
                            if (!(r.state == null && r.persistentState == null)) {
                                this.mInstrumentation.callActivityOnRestoreInstanceState(activity, r.state, r.persistentState);
                            }
                        } else if (r.state != null) {
                            this.mInstrumentation.callActivityOnRestoreInstanceState(activity, r.state);
                        }
                    }
                    if (pendingActions.shouldCallOnPostCreate()) {
                        activity.mCalled = false;
                        if (r.isPersistable()) {
                            this.mInstrumentation.callActivityOnPostCreate(activity, r.state, r.persistentState);
                        } else {
                            this.mInstrumentation.callActivityOnPostCreate(activity, r.state);
                        }
                        if (!activity.mCalled) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Activity ");
                            stringBuilder.append(r.intent.getComponent().toShortString());
                            stringBuilder.append(" did not call through to super.onPostCreate()");
                            throw new SuperNotCalledException(stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    private void checkAndBlockForNetworkAccess() {
        synchronized (this.mNetworkPolicyLock) {
            if (this.mNetworkBlockSeq != -1) {
                try {
                    ActivityManager.getService().waitForNetworkStateUpdate(this.mNetworkBlockSeq);
                    this.mNetworkBlockSeq = -1;
                } catch (RemoteException e) {
                    Log.e(TAG, "checkAndBlockForNetworkAccess()");
                }
            }
        }
    }

    private ContextImpl createBaseContextForActivity(ActivityClientRecord r) {
        try {
            int displayId = ActivityManager.getService().getActivityDisplayId(r.token);
            if (displayId != this.mDisplayId && HwPCUtils.isPcCastMode()) {
                if (r.parent != null && HwPCUtils.isValidExtDisplayId(this.mDisplayId)) {
                    displayId = this.mDisplayId;
                }
                if (!(!HwPCUtils.enabledInPad() || displayId == 0 || displayId == -1 || HwPCUtils.isValidExtDisplayId(displayId))) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("displayId :");
                    stringBuilder.append(displayId);
                    stringBuilder.append(",mDisplayId:");
                    stringBuilder.append(this.mDisplayId);
                    stringBuilder.append(",getPCDisplayID:");
                    stringBuilder.append(HwPCUtils.getPCDisplayID());
                    HwPCUtils.log(str, stringBuilder.toString());
                    HwPCUtils.setPCDisplayID(displayId);
                }
            }
            ContextImpl appContext = ContextImpl.createActivityContext(this, r.packageInfo, r.activityInfo, r.token, displayId, r.overrideConfig);
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            String pkgName = SystemProperties.get("debug.second-display.pkg");
            if (pkgName == null || pkgName.isEmpty() || !r.packageInfo.mPackageName.contains(pkgName)) {
                return appContext;
            }
            for (int id : dm.getDisplayIds()) {
                if (id != 0) {
                    return (ContextImpl) appContext.createDisplayContext(dm.getCompatibleDisplay(id, appContext.getResources()));
                }
            }
            return appContext;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Activity handleLaunchActivity(ActivityClientRecord r, PendingTransactionActions pendingActions, Intent customIntent) {
        if (Jlog.isMicroTest() && r != null) {
            Jlog.i(3084, Jlog.getMessage(TAG, "handleLaunchActivity", Intent.toPkgClsString(r.intent)));
        }
        if (this.mZrHungAppEyeUiProbe != null && this.mIsNeedStartUiProbe) {
            this.mZrHungAppEyeUiProbe.start(null);
        }
        if (Jlog.isPerfTest() && r != null) {
            Jlog.i(3040, Jlog.getMessage(TAG, "handleLaunchActivity", Intent.toPkgClsString(r.intent)));
        }
        if (!(r == null || r.intent == null || r.intent.getComponent() == null)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(r.intent.getComponent().getPackageName());
            stringBuilder.append("/");
            stringBuilder.append(r.intent.getComponent().getClassName());
            Jlog.d(R.styleable.Theme_toastFrameBackground, stringBuilder.toString(), "");
        }
        unscheduleGcIdler();
        this.mSomeActivitiesChanged = true;
        if (!(r == null || r.profilerInfo == null)) {
            this.mProfiler.setProfiler(r.profilerInfo);
            this.mProfiler.startProfiling();
        }
        handleConfigurationChanged(null, null);
        if (localLOGV) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Handling launch of ");
            stringBuilder2.append(r);
            Slog.v(str, stringBuilder2.toString());
        }
        if (!ThreadedRenderer.sRendererDisabled) {
            GraphicsEnvironment.earlyInitEGL();
        }
        WindowManagerGlobal.initialize();
        if (r == null) {
            return null;
        }
        Activity a = performLaunchActivity(r, customIntent);
        if (Jlog.isPerfTest()) {
            Jlog.i(3043, Jlog.getMessage(TAG, "handleLaunchActivity", Intent.toPkgClsString(r.intent)));
        }
        if (a != null) {
            r.createdConfig = new Configuration(this.mConfiguration);
            reportSizeConfigurations(r);
            if (!(r.activity.mFinished || pendingActions == null)) {
                pendingActions.setOldState(r.state);
                pendingActions.setRestoreInstanceState(true);
                pendingActions.setCallOnPostCreate(true);
            }
        } else {
            try {
                ActivityManager.getService().finishActivity(r.token, 0, null, 0);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
        if (Jlog.isMicroTest()) {
            Jlog.i(3085, Jlog.getMessage(TAG, "handleLaunchActivity", Intent.toPkgClsString(r.intent)));
        }
        return a;
    }

    private void reportSizeConfigurations(ActivityClientRecord r) {
        Configuration[] configurations = r.activity.getResources().getSizeConfigurations();
        if (configurations != null) {
            SparseIntArray horizontal = new SparseIntArray();
            SparseIntArray vertical = new SparseIntArray();
            SparseIntArray smallest = new SparseIntArray();
            for (int i = configurations.length - 1; i >= 0; i--) {
                Configuration config = configurations[i];
                if (config.screenHeightDp != 0) {
                    vertical.put(config.screenHeightDp, 0);
                }
                if (config.screenWidthDp != 0) {
                    horizontal.put(config.screenWidthDp, 0);
                }
                if (config.smallestScreenWidthDp != 0) {
                    smallest.put(config.smallestScreenWidthDp, 0);
                }
            }
            try {
                ActivityManager.getService().reportSizeConfigurations(r.token, horizontal.copyKeys(), vertical.copyKeys(), smallest.copyKeys());
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
    }

    private void deliverNewIntents(ActivityClientRecord r, List<ReferrerIntent> intents) {
        int N = intents.size();
        for (int i = 0; i < N; i++) {
            ReferrerIntent intent = (ReferrerIntent) intents.get(i);
            intent.setExtrasClassLoader(r.activity.getClassLoader());
            intent.prepareToEnterProcess();
            r.activity.mFragments.noteStateNotSaved();
            this.mInstrumentation.callActivityOnNewIntent(r.activity, intent);
        }
    }

    void performNewIntents(IBinder token, List<ReferrerIntent> intents, boolean andPause) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null) {
            boolean resumed = r.paused ^ true;
            if (resumed) {
                r.activity.mTemporaryPause = true;
                this.mInstrumentation.callActivityOnPause(r.activity);
            }
            checkAndBlockForNetworkAccess();
            deliverNewIntents(r, intents);
            if (resumed) {
                r.activity.performResume(false, "performNewIntents");
                r.activity.mTemporaryPause = false;
            }
            if (r.paused && andPause) {
                performResumeActivity(token, false, "performNewIntents");
                performPauseActivityIfNeeded(r, "performNewIntents");
            }
        }
    }

    public void handleNewIntent(IBinder token, List<ReferrerIntent> intents, boolean andPause) {
        performNewIntents(token, intents, andPause);
    }

    public void handleRequestNode(IBinder appToken, Bundle data, int token) {
        try {
            ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(appToken);
            Activity a = r == null ? null : r.activity;
            if (sContentSensorManager != null) {
                sContentSensorManager.updateToken(token, a);
                sContentSensorManager.copyNode(data);
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("copyNode get exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    public void handleContentOther(IBinder appToken, Bundle data, int token) {
        try {
            ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(appToken);
            Activity a = r == null ? null : r.activity;
            if (sContentSensorManager != null) {
                sContentSensorManager.updateToken(token, a);
                sContentSensorManager.processImageAndWebView(data);
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processImageAndWebView get exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        }
    }

    public void handleRequestAssistContextExtras(RequestAssistContextExtras cmd) {
        AssistStructure structure;
        AssistStructure structure2;
        boolean notSecure = false;
        boolean forAutofill = cmd.requestType == 2;
        if (this.mLastSessionId != cmd.sessionId) {
            this.mLastSessionId = cmd.sessionId;
            for (int i = this.mLastAssistStructures.size() - 1; i >= 0; i--) {
                structure = (AssistStructure) ((WeakReference) this.mLastAssistStructures.get(i)).get();
                if (structure != null) {
                    structure.clearSendChannel();
                }
                this.mLastAssistStructures.remove(i);
            }
        }
        Bundle data = new Bundle();
        structure = null;
        AssistContent content = forAutofill ? null : new AssistContent();
        long startTime = SystemClock.uptimeMillis();
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(cmd.activityToken);
        Uri referrer = null;
        if (r != null) {
            if (!forAutofill) {
                r.activity.getApplication().dispatchOnProvideAssistData(r.activity, data);
                r.activity.onProvideAssistData(data);
                referrer = r.activity.onProvideReferrer();
            }
            if (cmd.requestType == 1 || forAutofill) {
                structure = new AssistStructure(r.activity, forAutofill, cmd.flags);
                Intent activityIntent = r.activity.getIntent();
                if (r.window == null || (r.window.getAttributes().flags & 8192) == 0) {
                    notSecure = true;
                }
                if (activityIntent == null || !notSecure) {
                    if (!forAutofill) {
                        content.setDefaultIntent(new Intent());
                    }
                } else if (!forAutofill) {
                    Intent intent = new Intent(activityIntent);
                    intent.setFlags(intent.getFlags() & -67);
                    intent.removeUnsafeExtras();
                    content.setDefaultIntent(intent);
                }
                if (!forAutofill) {
                    r.activity.onProvideAssistContent(content);
                }
            }
        }
        Uri referrer2 = referrer;
        if (structure == null) {
            structure2 = new AssistStructure();
        } else {
            structure2 = structure;
        }
        structure2.setAcquisitionStartTime(startTime);
        structure2.setAcquisitionEndTime(SystemClock.uptimeMillis());
        this.mLastAssistStructures.add(new WeakReference(structure2));
        try {
            ActivityManager.getService().reportAssistContextExtras(cmd.requestToken, data, structure2, content, referrer2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void handleTranslucentConversionComplete(IBinder token, boolean drawComplete) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null) {
            r.activity.onTranslucentConversionComplete(drawComplete);
        }
    }

    public void onNewActivityOptions(IBinder token, ActivityOptions options) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null) {
            r.activity.onNewActivityOptions(options);
        }
    }

    public void handleInstallProvider(ProviderInfo info) {
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            installContentProviders(this.mInitialApplication, Arrays.asList(new ProviderInfo[]{info}));
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleEnterAnimationComplete(IBinder token) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null) {
            r.activity.dispatchEnterAnimationComplete();
        }
    }

    private void handleStartBinderTracking() {
        Binder.enableTracing();
    }

    private void handleStopBinderTrackingAndDump(ParcelFileDescriptor fd) {
        try {
            Binder.disableTracing();
            Binder.getTransactionTracker().writeTracesToFile(fd);
        } finally {
            IoUtils.closeQuietly(fd);
            Binder.getTransactionTracker().clearTraces();
        }
    }

    public void handleMultiWindowModeChanged(IBinder token, boolean isInMultiWindowMode, Configuration overrideConfig) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null) {
            Configuration newConfig = new Configuration(this.mConfiguration);
            if (overrideConfig != null) {
                newConfig.updateFrom(overrideConfig);
            }
            r.activity.dispatchMultiWindowModeChanged(isInMultiWindowMode, newConfig);
        }
    }

    public void handlePictureInPictureModeChanged(IBinder token, boolean isInPipMode, Configuration overrideConfig) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null) {
            Configuration newConfig = new Configuration(this.mConfiguration);
            if (overrideConfig != null) {
                newConfig.updateFrom(overrideConfig);
            }
            r.activity.dispatchPictureInPictureModeChanged(isInPipMode, newConfig);
        }
    }

    private void handleLocalVoiceInteractionStarted(IBinder token, IVoiceInteractor interactor) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null) {
            r.voiceInteractor = interactor;
            r.activity.setVoiceInteractor(interactor);
            if (interactor == null) {
                r.activity.onLocalVoiceInteractionStopped();
            } else {
                r.activity.onLocalVoiceInteractionStarted();
            }
        }
    }

    private static boolean attemptAttachAgent(String agent, ClassLoader classLoader) {
        try {
            VMDebug.attachAgent(agent, classLoader);
            return true;
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attaching agent with ");
            stringBuilder.append(classLoader);
            stringBuilder.append(" failed: ");
            stringBuilder.append(agent);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    static void handleAttachAgent(String agent, LoadedApk loadedApk) {
        ClassLoader classLoader = loadedApk != null ? loadedApk.getClassLoader() : null;
        if (!(attemptAttachAgent(agent, classLoader) || classLoader == null)) {
            attemptAttachAgent(agent, null);
        }
    }

    public static Intent getIntentBeingBroadcast() {
        return (Intent) sCurrentBroadcastIntent.get();
    }

    private void handleReceiver(ReceiverData data) {
        StringBuilder stringBuilder;
        unscheduleGcIdler();
        String component = data.intent.getComponent().getClassName();
        LoadedApk packageInfo = getPackageInfoNoCheck(data.info.applicationInfo, data.compatInfo);
        IActivityManager mgr = ActivityManager.getService();
        try {
            Application app = packageInfo.makeApplication(null, this.mInstrumentation);
            ContextImpl context = (ContextImpl) app.getBaseContext();
            if (data.info.splitName != null) {
                context = (ContextImpl) context.createContextForSplit(data.info.splitName);
            }
            ClassLoader cl = context.getClassLoader();
            data.intent.setExtrasClassLoader(cl);
            data.intent.prepareToEnterProcess();
            data.setExtrasClassLoader(cl);
            BroadcastReceiver receiver = packageInfo.getAppFactory().instantiateReceiver(cl, data.info.name, data.intent);
            try {
                if (DEBUG_BROADCAST) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Performing receive of ");
                    stringBuilder2.append(data.intent);
                    stringBuilder2.append(": app=");
                    stringBuilder2.append(app);
                    stringBuilder2.append(", appName=");
                    stringBuilder2.append(app.getPackageName());
                    stringBuilder2.append(", pkg=");
                    stringBuilder2.append(packageInfo.getPackageName());
                    stringBuilder2.append(", comp=");
                    stringBuilder2.append(data.intent.getComponent().toShortString());
                    stringBuilder2.append(", dir=");
                    stringBuilder2.append(packageInfo.getAppDir());
                    Slog.v(str, stringBuilder2.toString());
                }
                sCurrentBroadcastIntent.set(data.intent);
                receiver.setPendingResult(data);
                receiver.onReceive(context.getReceiverRestrictedContext(), data.intent);
            } catch (Exception e) {
                if (DEBUG_BROADCAST) {
                    String str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Finishing failed broadcast to ");
                    stringBuilder.append(data.intent.getComponent());
                    Slog.i(str2, stringBuilder.toString());
                }
                data.sendFinished(mgr);
                if (!this.mInstrumentation.onException(receiver, e)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to start receiver ");
                    stringBuilder.append(component);
                    stringBuilder.append(": ");
                    stringBuilder.append(e.toString());
                    throw new RuntimeException(stringBuilder.toString(), e);
                }
            } catch (Throwable th) {
                sCurrentBroadcastIntent.set(null);
            }
            sCurrentBroadcastIntent.set(null);
            if (receiver.getPendingResult() != null) {
                data.finish();
            }
        } catch (Exception e2) {
            if (DEBUG_BROADCAST) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Finishing failed broadcast to ");
                stringBuilder3.append(data.intent.getComponent());
                Slog.i(TAG, stringBuilder3.toString());
            }
            data.sendFinished(mgr);
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Unable to instantiate receiver ");
            stringBuilder4.append(component);
            stringBuilder4.append(": ");
            stringBuilder4.append(e2.toString());
            throw new RuntimeException(stringBuilder4.toString(), e2);
        }
    }

    private void handleCreateBackupAgent(CreateBackupAgentData data) {
        try {
            String str;
            if (getPackageManager().getPackageInfo(data.appInfo.packageName, 0, UserHandle.myUserId()).applicationInfo.uid != Process.myUid()) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Asked to instantiate non-matching package ");
                stringBuilder.append(data.appInfo.packageName);
                Slog.w(str, stringBuilder.toString());
                return;
            }
            unscheduleGcIdler();
            LoadedApk packageInfo = getPackageInfoNoCheck(data.appInfo, data.compatInfo);
            str = packageInfo.mPackageName;
            if (str == null) {
                Slog.d(TAG, "Asked to create backup agent for nonexistent package");
                return;
            }
            String classname = data.appInfo.backupAgentName;
            if (classname == null && (data.backupMode == 1 || data.backupMode == 3)) {
                classname = "android.app.backup.FullBackupAgent";
            }
            IBinder binder = null;
            try {
                BackupAgent agent = (BackupAgent) this.mBackupAgents.get(str);
                if (agent != null) {
                    binder = agent.onBind();
                } else {
                    agent = (BackupAgent) packageInfo.getClassLoader().loadClass(classname).newInstance();
                    ContextImpl context = ContextImpl.createAppContext(this, packageInfo);
                    context.setOuterContext(agent);
                    agent.attach(context);
                    agent.onCreate();
                    binder = agent.onBind();
                    this.mBackupAgents.put(str, agent);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (Exception e2) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Agent threw during creation: ");
                stringBuilder2.append(e2);
                Slog.e(str2, stringBuilder2.toString());
                if (data.backupMode != 2) {
                    if (data.backupMode != 3) {
                        throw e2;
                    }
                }
            } catch (Exception e3) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unable to create BackupAgent ");
                stringBuilder3.append(classname);
                stringBuilder3.append(": ");
                stringBuilder3.append(e3.toString());
                throw new RuntimeException(stringBuilder3.toString(), e3);
            }
            ActivityManager.getService().backupAgentCreated(str, binder);
        } catch (RemoteException e4) {
            throw e4.rethrowFromSystemServer();
        }
    }

    private void handleDestroyBackupAgent(CreateBackupAgentData data) {
        String packageName = getPackageInfoNoCheck(data.appInfo, data.compatInfo).mPackageName;
        BackupAgent agent = (BackupAgent) this.mBackupAgents.get(packageName);
        if (agent != null) {
            try {
                agent.onDestroy();
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception thrown in onDestroy by backup agent of ");
                stringBuilder.append(data.appInfo);
                Slog.w(str, stringBuilder.toString());
                e.printStackTrace();
            }
            this.mBackupAgents.remove(packageName);
            return;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Attempt to destroy unknown backup agent ");
        stringBuilder2.append(data);
        Slog.w(str2, stringBuilder2.toString());
    }

    private void handleCreateService(CreateServiceData data) {
        StringBuilder stringBuilder;
        unscheduleGcIdler();
        LoadedApk packageInfo = getPackageInfoNoCheck(data.info.applicationInfo, data.compatInfo);
        Service service = null;
        try {
            service = packageInfo.getAppFactory().instantiateService(packageInfo.getClassLoader(), data.info.name, data.intent);
        } catch (Exception e) {
            if (!this.mInstrumentation.onException(null, e)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to instantiate service ");
                stringBuilder.append(data.info.name);
                stringBuilder.append(": ");
                stringBuilder.append(e.toString());
                throw new RuntimeException(stringBuilder.toString(), e);
            }
        }
        try {
            if (localLOGV) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Creating service ");
                stringBuilder2.append(data.info.name);
                Slog.v(str, stringBuilder2.toString());
            }
            Context context = ContextImpl.createAppContext(this, packageInfo);
            context.setOuterContext(service);
            Service service2 = service;
            Context context2 = context;
            service2.attach(context2, this, data.info.name, data.token, packageInfo.makeApplication(false, this.mInstrumentation), ActivityManager.getService());
            service.onCreate();
            this.mServices.put(data.token, service);
            ActivityManager.getService().serviceDoneExecuting(data.token, 0, 0, 0);
            if (Jlog.isUBMEnable()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("SC#");
                stringBuilder.append(data.info.name);
                stringBuilder.append("(");
                stringBuilder.append(currentProcessName());
                stringBuilder.append(")");
                Jlog.d(270, stringBuilder.toString());
            }
        } catch (RemoteException e2) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("serviceDone failed when creating service ");
            stringBuilder3.append(data.info.name);
            stringBuilder3.append(": ");
            stringBuilder3.append(e2.toString());
            Flog.w(102, stringBuilder3.toString());
            throw e2.rethrowFromSystemServer();
        } catch (Exception e3) {
            if (!this.mInstrumentation.onException(service, e3)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create service ");
                stringBuilder.append(data.info.name);
                stringBuilder.append(": ");
                stringBuilder.append(e3.toString());
                throw new RuntimeException(stringBuilder.toString(), e3);
            }
        }
    }

    private void handleBindService(BindServiceData data) {
        StringBuilder stringBuilder;
        Service s = (Service) this.mServices.get(data.token);
        if (DEBUG_SERVICE) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("handleBindService s=");
            stringBuilder2.append(s);
            stringBuilder2.append(" rebind=");
            stringBuilder2.append(data.rebind);
            Slog.v(str, stringBuilder2.toString());
        }
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                data.intent.prepareToEnterProcess();
                if (data.rebind) {
                    s.onRebind(data.intent);
                    ActivityManager.getService().serviceDoneExecuting(data.token, 0, 0, 0);
                } else {
                    ActivityManager.getService().publishService(data.token, data.intent, s.onBind(data.intent));
                }
                ensureJitEnabled();
                return;
            } catch (RemoteException ex) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("publishService failed when binding service ");
                stringBuilder.append(s);
                stringBuilder.append(" with ");
                stringBuilder.append(data.intent);
                stringBuilder.append(": ");
                stringBuilder.append(ex.toString());
                Flog.w(102, stringBuilder.toString());
                throw ex.rethrowFromSystemServer();
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(s, e)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to bind to service ");
                    stringBuilder.append(s);
                    stringBuilder.append(" with ");
                    stringBuilder.append(data.intent);
                    stringBuilder.append(": ");
                    stringBuilder.append(e.toString());
                    throw new RuntimeException(stringBuilder.toString(), e);
                }
                return;
            }
        }
        Flog.v(101, "service can't be binded");
    }

    private void handleUnbindService(BindServiceData data) {
        Service s = (Service) this.mServices.get(data.token);
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                data.intent.prepareToEnterProcess();
                boolean doRebind = s.onUnbind(data.intent);
                if (doRebind) {
                    ActivityManager.getService().unbindFinished(data.token, data.intent, doRebind);
                } else {
                    ActivityManager.getService().serviceDoneExecuting(data.token, 0, 0, 0);
                }
            } catch (RemoteException ex) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to finish unbind to service ");
                stringBuilder.append(s);
                stringBuilder.append(" with ");
                stringBuilder.append(data.intent);
                stringBuilder.append(": ");
                stringBuilder.append(ex.toString());
                Flog.w(102, stringBuilder.toString());
                throw ex.rethrowFromSystemServer();
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(s, e)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to unbind to service ");
                    stringBuilder2.append(s);
                    stringBuilder2.append(" with ");
                    stringBuilder2.append(data.intent);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(e.toString());
                    throw new RuntimeException(stringBuilder2.toString(), e);
                }
            }
        }
    }

    private void handleDumpService(DumpComponentInfo info) {
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            Service s = (Service) this.mServices.get(info.token);
            if (s != null) {
                PrintWriter pw = new FastPrintWriter(new FileOutputStream(info.fd.getFileDescriptor()));
                s.dump(info.fd.getFileDescriptor(), pw, info.args);
                pw.flush();
            }
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        } catch (Throwable th) {
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleDumpActivity(DumpComponentInfo info) {
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(info.token);
            if (!(r == null || r.activity == null)) {
                PrintWriter pw = new FastPrintWriter(new FileOutputStream(info.fd.getFileDescriptor()));
                r.activity.dump(info.prefix, info.fd.getFileDescriptor(), pw, info.args);
                pw.flush();
            }
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        } catch (Throwable th) {
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleDumpProvider(DumpComponentInfo info) {
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        try {
            ProviderClientRecord r = (ProviderClientRecord) this.mLocalProviders.get(info.token);
            if (!(r == null || r.mLocalProvider == null)) {
                PrintWriter pw = new FastPrintWriter(new FileOutputStream(info.fd.getFileDescriptor()));
                r.mLocalProvider.dump(info.fd.getFileDescriptor(), pw, info.args);
                pw.flush();
            }
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        } catch (Throwable th) {
            IoUtils.closeQuietly(info.fd);
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void handleServiceArgs(ServiceArgsData data) {
        Service s = (Service) this.mServices.get(data.token);
        if (s != null) {
            try {
                int res;
                if (data.args != null) {
                    data.args.setExtrasClassLoader(s.getClassLoader());
                    data.args.prepareToEnterProcess();
                }
                if (data.taskRemoved) {
                    s.onTaskRemoved(data.args);
                    res = 1000;
                } else {
                    res = s.onStartCommand(data.args, data.flags, data.startId);
                }
                QueuedWork.waitToFinish();
                ActivityManager.getService().serviceDoneExecuting(data.token, 1, data.startId, res);
                ensureJitEnabled();
            } catch (RemoteException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to finish starting service ");
                stringBuilder.append(s);
                stringBuilder.append(" with ");
                stringBuilder.append(data.args);
                stringBuilder.append(": ");
                stringBuilder.append(e.toString());
                Flog.w(102, stringBuilder.toString());
                throw e.rethrowFromSystemServer();
            } catch (Exception e2) {
                if (!this.mInstrumentation.onException(s, e2)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to start service ");
                    stringBuilder2.append(s);
                    stringBuilder2.append(" with ");
                    stringBuilder2.append(data.args);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(e2.toString());
                    throw new RuntimeException(stringBuilder2.toString(), e2);
                }
            }
        }
    }

    private void handleStopService(IBinder token) {
        Service s = (Service) this.mServices.remove(token);
        String str;
        StringBuilder stringBuilder;
        if (s != null) {
            StringBuilder stringBuilder2;
            try {
                if (localLOGV) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Destroying service ");
                    stringBuilder.append(s);
                    Slog.v(str, stringBuilder.toString());
                }
                s.onDestroy();
                s.detachAndCleanUp();
                Context context = s.getBaseContext();
                if (context instanceof ContextImpl) {
                    ((ContextImpl) context).scheduleFinalCleanup(s.getClassName(), "Service");
                }
                QueuedWork.waitToFinish();
                ActivityManager.getService().serviceDoneExecuting(token, 2, 0, 0);
                if (Jlog.isUBMEnable()) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("SE#");
                    stringBuilder2.append(s.getClassName());
                    Jlog.d(271, stringBuilder2.toString());
                    return;
                }
                return;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (Exception e2) {
                if (this.mInstrumentation.onException(s, e2)) {
                    String str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("handleStopService: exception for ");
                    stringBuilder2.append(token);
                    Slog.i(str2, stringBuilder2.toString(), e2);
                    return;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to stop service ");
                stringBuilder2.append(s);
                stringBuilder2.append(": ");
                stringBuilder2.append(e2.toString());
                throw new RuntimeException(stringBuilder2.toString(), e2);
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("handleStopService: token=");
        stringBuilder.append(token);
        stringBuilder.append(" not found.");
        Slog.i(str, stringBuilder.toString());
    }

    @VisibleForTesting
    public ActivityClientRecord performResumeActivity(IBinder token, boolean finalStateRequest, String reason) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (localLOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Performing resume of ");
            stringBuilder.append(r);
            stringBuilder.append(" finished=");
            stringBuilder.append(r.activity.mFinished);
            stringBuilder.append(" for reason:");
            stringBuilder.append(reason);
            Slog.v(str, stringBuilder.toString());
        }
        if (r == null || r.activity.mFinished) {
            return null;
        }
        if (r.getLifecycleState() == 3) {
            if (!(finalStateRequest || "com.huawei.desktop.explorer".equals(r.activity.getPackageName()))) {
                RuntimeException e = new IllegalStateException("Trying to resume activity which is already resumed");
                Slog.e(TAG, e.getMessage(), e);
                Slog.e(TAG, r.getStateString());
            }
            return null;
        }
        if (finalStateRequest) {
            r.hideForNow = false;
            r.activity.mStartedActivity = false;
        }
        try {
            r.activity.onStateNotSaved();
            r.activity.mFragments.noteStateNotSaved();
            checkAndBlockForNetworkAccess();
            if (r.pendingIntents != null) {
                deliverNewIntents(r, r.pendingIntents);
                r.pendingIntents = null;
            }
            if (r.pendingResults != null) {
                deliverResults(r, r.pendingResults, reason);
                r.pendingResults = null;
            }
            r.activity.performResume(r.startsNotResumed, reason);
            if ("com.huawei.android.launcher.unihome.UniHomeLauncher".equals(r.activity.getComponentName().getClassName())) {
                Jlog.d(383, "JLID_LAUNCHER_ONRESUMED");
            }
            if ("com.tencent.mm.plugin.sns.ui.SnsBrowseUI".equals(r.activity.getComponentName().getClassName())) {
                Looper.myQueue().enableReduceDelay(true);
            }
            r.state = null;
            r.persistentState = null;
            r.setState(3);
        } catch (Exception e2) {
            if (!this.mInstrumentation.onException(r.activity, e2)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to resume activity ");
                stringBuilder2.append(r.intent.getComponent().toShortString());
                stringBuilder2.append(": ");
                stringBuilder2.append(e2.toString());
                throw new RuntimeException(stringBuilder2.toString(), e2);
            }
        }
        return r;
    }

    static final void cleanUpPendingRemoveWindows(ActivityClientRecord r, boolean force) {
        if (!r.mPreserveWindow || force) {
            if (r.mPendingRemoveWindow != null) {
                r.mPendingRemoveWindowManager.removeViewImmediate(r.mPendingRemoveWindow.getDecorView());
                IBinder wtoken = r.mPendingRemoveWindow.getDecorView().getWindowToken();
                if (wtoken != null) {
                    WindowManagerGlobal.getInstance().closeAll(wtoken, r.activity.getClass().getName(), "Activity");
                }
            }
            r.mPendingRemoveWindow = null;
            r.mPendingRemoveWindowManager = null;
        }
    }

    public void handleResumeActivity(IBinder token, boolean finalStateRequest, boolean isForward, String reason) {
        boolean z = isForward;
        if (this.mZrHungAppEyeUiProbe != null && this.mIsNeedStartUiProbe) {
            this.mZrHungAppEyeUiProbe.start(null);
        }
        UniPerf.getInstance().uniPerfEvent(4098, "", new int[0]);
        unscheduleGcIdler();
        this.mSomeActivitiesChanged = true;
        ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);
        if (r != null) {
            String str;
            String str2;
            StringBuilder stringBuilder;
            if (Jlog.isPerfTest()) {
                Jlog.i(3049, Jlog.getMessage(TAG, "handleResumeActivity", Intent.toPkgClsString(r.intent)));
            }
            Activity a = r.activity;
            if (localLOGV) {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Resume ");
                stringBuilder2.append(r);
                stringBuilder2.append(" started activity: ");
                stringBuilder2.append(a.mStartedActivity);
                stringBuilder2.append(", hideForNow: ");
                stringBuilder2.append(r.hideForNow);
                stringBuilder2.append(", finished: ");
                stringBuilder2.append(a.mFinished);
                Slog.v(str, stringBuilder2.toString());
            }
            int forwardBit = z ? 256 : 0;
            boolean willBeVisible = a.mStartedActivity ^ true;
            if (!willBeVisible) {
                try {
                    willBeVisible = ActivityManager.getService().willActivityBeVisible(a.getActivityToken());
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            if (r.window == null && !a.mFinished && willBeVisible) {
                r.window = r.activity.getWindow();
                View decor = r.window.getDecorView();
                decor.setVisibility(4);
                ViewManager wm = a.getWindowManager();
                LayoutParams l = r.window.getAttributes();
                a.mDecor = decor;
                l.type = 1;
                l.softInputMode |= forwardBit;
                if (r.mPreserveWindow) {
                    a.mWindowAdded = true;
                    r.mPreserveWindow = false;
                    ViewRootImpl impl = decor.getViewRootImpl();
                    if (impl != null) {
                        impl.notifyChildRebuilt();
                    }
                }
                if (a.mVisibleFromClient) {
                    if (a.mWindowAdded) {
                        a.onWindowAttributesChanged(l);
                    } else {
                        a.mWindowAdded = true;
                        wm.addView(decor, l);
                    }
                }
            } else if (!willBeVisible) {
                if (localLOGV) {
                    str = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Launch ");
                    stringBuilder3.append(r);
                    stringBuilder3.append(" mStartedActivity set");
                    Slog.v(str, stringBuilder3.toString());
                }
                r.hideForNow = true;
            }
            cleanUpPendingRemoveWindows(r, false);
            if (!(r.activity.mFinished || !willBeVisible || r.activity.mDecor == null || r.hideForNow)) {
                if (r.newConfig != null) {
                    performConfigurationChangedForActivity(r, r.newConfig);
                    if (DEBUG_CONFIGURATION) {
                        str2 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Resuming activity ");
                        stringBuilder.append(r.activityInfo.name);
                        stringBuilder.append(" with newConfig ");
                        stringBuilder.append(r.activity.mCurrentConfig);
                        Slog.v(str2, stringBuilder.toString());
                    }
                    r.newConfig = null;
                }
                if (localLOGV) {
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Resuming ");
                    stringBuilder.append(r);
                    stringBuilder.append(" with isForward=");
                    stringBuilder.append(z);
                    Slog.v(str2, stringBuilder.toString());
                }
                LayoutParams l2 = r.window.getAttributes();
                if ((l2.softInputMode & 256) != forwardBit) {
                    l2.softInputMode = (l2.softInputMode & -257) | forwardBit;
                    if (r.activity.mVisibleFromClient) {
                        a.getWindowManager().updateViewLayout(r.window.getDecorView(), l2);
                    }
                }
                r.activity.mVisibleFromServer = true;
                this.mNumVisibleActivities++;
                if (r.activity.mVisibleFromClient) {
                    r.activity.makeVisible();
                }
            }
            r.nextIdle = this.mNewActivities;
            this.mNewActivities = r;
            if (localLOGV) {
                str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Scheduling idle handler for ");
                stringBuilder.append(r);
                Slog.v(str2, stringBuilder.toString());
            }
            ViewRootImpl.setIsFirstFrame(true);
            if (Jlog.isPerfTest()) {
                Jlog.i(3050, Jlog.getMessage(TAG, "handleResumeActivity", Intent.toPkgClsString(r.intent)));
            }
            Looper.myQueue().addIdleHandler(new Idler(this, null));
        }
    }

    public void handlePauseActivity(IBinder token, boolean finished, boolean userLeaving, int configChanges, PendingTransactionActions pendingActions, String reason) {
        StringBuilder stringBuilder;
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (Jlog.isMicroTest() && r != null) {
            Jlog.i(3086, Jlog.getMessage(TAG, "handlePauseActivity", Intent.toPkgClsString(r.intent, "who")));
        }
        if (localLOGV) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Handling pause of ");
            stringBuilder.append(r);
            stringBuilder.append(", finished: ");
            stringBuilder.append(finished);
            stringBuilder.append(" userLeaving:");
            stringBuilder.append(userLeaving);
            Slog.d(str, stringBuilder.toString());
        }
        if (r != null) {
            if (Jlog.isPerfTest()) {
                Jlog.i(3025, Jlog.getMessage(TAG, "handlePauseActivity", Intent.toPkgClsString(r.intent, "who")));
            }
            if (userLeaving) {
                performUserLeavingActivity(r);
            }
            Activity activity = r.activity;
            activity.mConfigChangeFlags |= configChanges;
            performPauseActivity(r, finished, reason, pendingActions);
            if (Jlog.isPerfTest()) {
                Jlog.i(3028, Jlog.getMessage(TAG, "handlePauseActivity", Intent.toPkgClsString(r.intent, "who")));
            }
            if (r.isPreHoneycomb()) {
                QueuedWork.waitToFinish();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(r.activity.getComponentName().getClassName());
            stringBuilder.append(" paused ");
            Jlog.d(386, stringBuilder.toString());
            this.mSomeActivitiesChanged = true;
        }
        if (this.mZrHungAppEyeUiProbe != null) {
            this.mZrHungAppEyeUiProbe.stop(null);
        }
        if (Jlog.isMicroTest() && r != null) {
            Jlog.i(3087, Jlog.getMessage(TAG, "handlePauseActivity", Intent.toPkgClsString(r.intent, "who")));
        }
    }

    final void performUserLeavingActivity(ActivityClientRecord r) {
        this.mInstrumentation.callActivityOnUserLeaving(r.activity);
    }

    final Bundle performPauseActivity(IBinder token, boolean finished, String reason, PendingTransactionActions pendingActions) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        return r != null ? performPauseActivity(r, finished, reason, pendingActions) : null;
    }

    private Bundle performPauseActivity(ActivityClientRecord r, boolean finished, String reason, PendingTransactionActions pendingActions) {
        ArrayList<OnActivityPausedListener> listeners;
        Bundle bundle = null;
        if (r.paused) {
            if (r.activity.mFinished) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Performing pause of activity that is not resumed: ");
            stringBuilder.append(r.intent.getComponent().toShortString());
            RuntimeException e = new RuntimeException(stringBuilder.toString());
            Slog.e(TAG, e.getMessage(), e);
        }
        boolean shouldSaveState = true;
        if (finished) {
            r.activity.mFinished = true;
        }
        int i = 0;
        if (r.activity.mFinished || !r.isPreHoneycomb()) {
            shouldSaveState = false;
        }
        if (shouldSaveState) {
            callActivityOnSaveInstanceState(r);
        }
        performPauseActivityIfNeeded(r, reason);
        synchronized (this.mOnPauseListeners) {
            listeners = (ArrayList) this.mOnPauseListeners.remove(r.activity);
        }
        int size = listeners != null ? listeners.size() : 0;
        while (i < size) {
            ((OnActivityPausedListener) listeners.get(i)).onPaused(r.activity);
            i++;
        }
        Bundle oldState = pendingActions != null ? pendingActions.getOldState() : null;
        if (oldState != null && r.isPreHoneycomb()) {
            r.state = oldState;
        }
        if (shouldSaveState) {
            bundle = r.state;
        }
        return bundle;
    }

    private void performPauseActivityIfNeeded(ActivityClientRecord r, String reason) {
        if (!r.paused) {
            StringBuilder stringBuilder;
            if (localLOGV) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Performing pause of ");
                stringBuilder.append(r);
                stringBuilder.append(" for reason: ");
                stringBuilder.append(reason);
                Slog.d(str, stringBuilder.toString());
            }
            try {
                r.activity.mCalled = false;
                this.mInstrumentation.callActivityOnPause(r.activity);
                if ("com.tencent.mm.plugin.sns.ui.SnsBrowseUI".equals(r.activity.getComponentName().getClassName())) {
                    Looper.myQueue().enableReduceDelay(false);
                }
                if (r.activity.mCalled) {
                    r.setState(4);
                    return;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Activity ");
                stringBuilder.append(safeToComponentShortString(r.intent));
                stringBuilder.append(" did not call through to super.onPause()");
                throw new SuperNotCalledException(stringBuilder.toString());
            } catch (SuperNotCalledException e) {
                throw e;
            } catch (Exception e2) {
                if (!this.mInstrumentation.onException(r.activity, e2)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to pause activity ");
                    stringBuilder2.append(safeToComponentShortString(r.intent));
                    stringBuilder2.append(": ");
                    stringBuilder2.append(e2.toString());
                    throw new RuntimeException(stringBuilder2.toString(), e2);
                }
            }
        }
    }

    final void performStopActivity(IBinder token, boolean saveState, String reason) {
        performStopActivityInner((ActivityClientRecord) this.mActivities.get(token), null, false, saveState, false, reason);
    }

    private void performStopActivityInner(ActivityClientRecord r, StopInfo info, boolean keepShown, boolean saveState, boolean finalStateRequest, String reason) {
        StringBuilder stringBuilder;
        if (localLOGV) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Performing stop of ");
            stringBuilder.append(r);
            stringBuilder.append(" keepShown:");
            stringBuilder.append(keepShown);
            stringBuilder.append(" for reason:");
            stringBuilder.append(reason);
            Slog.v(str, stringBuilder.toString());
        }
        if (r != null) {
            if (!keepShown && r.stopped) {
                if (!r.activity.mFinished) {
                    if (!finalStateRequest) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Performing stop of activity that is already stopped: ");
                        stringBuilder.append(r.intent.getComponent().toShortString());
                        RuntimeException e = new RuntimeException(stringBuilder.toString());
                        Slog.e(TAG, e.getMessage(), e);
                        Slog.e(TAG, r.getStateString());
                    }
                } else {
                    return;
                }
            }
            performPauseActivityIfNeeded(r, reason);
            if (info != null) {
                try {
                    info.setDescription(r.activity.onCreateDescription());
                } catch (Exception e2) {
                    if (!this.mInstrumentation.onException(r.activity, e2)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unable to save state of activity ");
                        stringBuilder2.append(r.intent.getComponent().toShortString());
                        stringBuilder2.append(": ");
                        stringBuilder2.append(e2.toString());
                        throw new RuntimeException(stringBuilder2.toString(), e2);
                    }
                }
            }
            if (!keepShown) {
                callActivityOnStop(r, saveState, reason);
            }
        }
    }

    private void callActivityOnStop(ActivityClientRecord r, boolean saveState, String reason) {
        boolean shouldSaveState = saveState && !r.activity.mFinished && r.state == null && !r.isPreHoneycomb();
        boolean isPreP = r.isPreP();
        if (shouldSaveState && isPreP) {
            callActivityOnSaveInstanceState(r);
        }
        try {
            r.activity.performStop(false, reason);
        } catch (SuperNotCalledException e) {
            throw e;
        } catch (Exception e2) {
            if (!this.mInstrumentation.onException(r.activity, e2)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to stop activity ");
                stringBuilder.append(r.intent.getComponent().toShortString());
                stringBuilder.append(": ");
                stringBuilder.append(e2.toString());
                throw new RuntimeException(stringBuilder.toString(), e2);
            }
        }
        r.setState(5);
        if (shouldSaveState && !isPreP) {
            callActivityOnSaveInstanceState(r);
        }
    }

    private void updateVisibility(ActivityClientRecord r, boolean show) {
        View v = r.activity.mDecor;
        if (v == null) {
            return;
        }
        if (show) {
            if (!r.activity.mVisibleFromServer) {
                r.activity.mVisibleFromServer = true;
                this.mNumVisibleActivities++;
                if (r.activity.mVisibleFromClient) {
                    r.activity.makeVisible();
                }
            }
            if (r.newConfig != null) {
                performConfigurationChangedForActivity(r, r.newConfig);
                if (DEBUG_CONFIGURATION) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Updating activity vis ");
                    stringBuilder.append(r.activityInfo.name);
                    stringBuilder.append(" with new config ");
                    stringBuilder.append(r.activity.mCurrentConfig);
                    Slog.v(str, stringBuilder.toString());
                }
                r.newConfig = null;
            }
        } else if (r.activity.mVisibleFromServer) {
            r.activity.mVisibleFromServer = false;
            this.mNumVisibleActivities--;
            v.setVisibility(4);
        }
    }

    public void handleStopActivity(IBinder token, boolean show, int configChanges, PendingTransactionActions pendingActions, boolean finalStateRequest, String reason) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        Activity activity = r.activity;
        activity.mConfigChangeFlags |= configChanges;
        StopInfo stopInfo = new StopInfo();
        performStopActivityInner(r, stopInfo, show, true, finalStateRequest, reason);
        if (localLOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Finishing stop of ");
            stringBuilder.append(r);
            stringBuilder.append(": show=");
            stringBuilder.append(show);
            stringBuilder.append(" win=");
            stringBuilder.append(r.window);
            Slog.v(str, stringBuilder.toString());
        }
        updateVisibility(r, show);
        if (!r.isPreHoneycomb()) {
            QueuedWork.waitToFinish();
        }
        stopInfo.setActivity(r);
        stopInfo.setState(r.state);
        stopInfo.setPersistentState(r.persistentState);
        pendingActions.setStopInfo(stopInfo);
        this.mSomeActivitiesChanged = true;
    }

    public void reportStop(PendingTransactionActions pendingActions) {
        this.mH.post(pendingActions.getStopInfo());
    }

    public void performRestartActivity(IBinder token, boolean start) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (localLOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Performing restart of ");
            stringBuilder.append(r);
            stringBuilder.append(" stopped=");
            stringBuilder.append(r.stopped);
            Slog.v(str, stringBuilder.toString());
        }
        if (r.stopped) {
            r.activity.performRestart(start, "performRestartActivity");
            if (start) {
                r.setState(2);
            }
        }
    }

    public void handleWindowVisibility(IBinder token, boolean show) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        String str;
        StringBuilder stringBuilder;
        if (r == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleWindowVisibility: no activity for token ");
            stringBuilder.append(token);
            Log.w(str, stringBuilder.toString());
            return;
        }
        if (!show && !r.stopped) {
            performStopActivityInner(r, null, show, false, false, "handleWindowVisibility");
        } else if (show && r.stopped) {
            unscheduleGcIdler();
            r.activity.performRestart(true, "handleWindowVisibility");
            r.setState(2);
        }
        if (r.activity.mDecor != null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Handle window ");
            stringBuilder.append(r);
            stringBuilder.append(" visibility: ");
            stringBuilder.append(show);
            Slog.v(str, stringBuilder.toString());
            updateVisibility(r, show);
        }
        this.mSomeActivitiesChanged = true;
    }

    private void handleSleeping(IBinder token, boolean sleeping) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleSleeping: no activity for token ");
            stringBuilder.append(token);
            Log.w(str, stringBuilder.toString());
            return;
        }
        if (sleeping) {
            if (!(r.stopped || r.isPreHoneycomb())) {
                callActivityOnStop(r, true, "sleeping");
            }
            if (!r.isPreHoneycomb()) {
                QueuedWork.waitToFinish();
            }
            try {
                ActivityManager.getService().activitySlept(r.token);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        } else if (r.stopped && r.activity.mVisibleFromServer) {
            r.activity.performRestart(true, "handleSleeping");
            r.setState(2);
        }
    }

    private void handleSetCoreSettings(Bundle coreSettings) {
        synchronized (this.mResourcesManager) {
            this.mCoreSettings = coreSettings;
        }
        onCoreSettingsChange();
    }

    private void onCoreSettingsChange() {
        boolean debugViewAttributes = this.mCoreSettings.getInt("debug_view_attributes", 0) != 0;
        if (debugViewAttributes != View.mDebugViewAttributes) {
            View.mDebugViewAttributes = debugViewAttributes;
            relaunchAllActivities(false);
        }
    }

    private void relaunchAllActivities(boolean fromThemeChange) {
        for (Entry<IBinder, ActivityClientRecord> entry : this.mActivities.entrySet()) {
            if (!((ActivityClientRecord) entry.getValue()).activity.mFinished) {
                scheduleRelaunchActivity((IBinder) entry.getKey(), fromThemeChange);
            }
        }
    }

    private void handleUpdatePackageCompatibilityInfo(UpdateCompatibilityData data) {
        LoadedApk apk = peekPackageInfo(data.pkg, false);
        if (apk != null) {
            apk.setCompatibilityInfo(data.info);
        }
        apk = peekPackageInfo(data.pkg, true);
        if (apk != null) {
            apk.setCompatibilityInfo(data.info);
        }
        handleConfigurationChanged(this.mConfiguration, data.info);
        WindowManagerGlobal.getInstance().reportNewConfiguration(this.mConfiguration);
    }

    private void deliverResults(ActivityClientRecord r, List<ResultInfo> results, String reason) {
        int N = results.size();
        for (int i = 0; i < N; i++) {
            ResultInfo ri = (ResultInfo) results.get(i);
            try {
                if (ri.mData != null) {
                    ri.mData.setExtrasClassLoader(r.activity.getClassLoader());
                    ri.mData.prepareToEnterProcess();
                }
                if (DEBUG_RESULTS) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Delivering result to activity ");
                    stringBuilder.append(r);
                    stringBuilder.append(" : ");
                    stringBuilder.append(ri);
                    Slog.v(str, stringBuilder.toString());
                }
                r.activity.dispatchActivityResult(ri.mResultWho, ri.mRequestCode, ri.mResultCode, ri.mData, reason);
            } catch (Exception e) {
                if (!this.mInstrumentation.onException(r.activity, e)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failure delivering result ");
                    stringBuilder2.append(ri);
                    stringBuilder2.append(" to activity ");
                    stringBuilder2.append(r.intent.getComponent().toShortString());
                    stringBuilder2.append(": ");
                    stringBuilder2.append(e.toString());
                    throw new RuntimeException(stringBuilder2.toString(), e);
                }
            }
        }
    }

    public void handleSendResult(IBinder token, List<ResultInfo> results, String reason) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (DEBUG_RESULTS) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Handling send result to ");
            stringBuilder.append(r);
            Slog.v(str, stringBuilder.toString());
        }
        if (r != null) {
            boolean resumed = r.paused ^ true;
            if (!r.activity.mFinished && r.activity.mDecor != null && r.hideForNow && resumed) {
                updateVisibility(r, true);
            }
            if (resumed) {
                StringBuilder stringBuilder2;
                try {
                    r.activity.mCalled = false;
                    r.activity.mTemporaryPause = true;
                    this.mInstrumentation.callActivityOnPause(r.activity);
                    if (!r.activity.mCalled) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Activity ");
                        stringBuilder2.append(r.intent.getComponent().toShortString());
                        stringBuilder2.append(" did not call through to super.onPause()");
                        throw new SuperNotCalledException(stringBuilder2.toString());
                    }
                } catch (SuperNotCalledException e) {
                    throw e;
                } catch (Exception e2) {
                    if (!this.mInstrumentation.onException(r.activity, e2)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unable to pause activity ");
                        stringBuilder2.append(r.intent.getComponent().toShortString());
                        stringBuilder2.append(": ");
                        stringBuilder2.append(e2.toString());
                        throw new RuntimeException(stringBuilder2.toString(), e2);
                    }
                }
            }
            checkAndBlockForNetworkAccess();
            deliverResults(r, results, reason);
            if (resumed) {
                r.activity.performResume(false, reason);
                r.activity.mTemporaryPause = false;
            }
        }
    }

    ActivityClientRecord performDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance, String reason) {
        String str;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        Class<? extends Activity> activityClass = null;
        if (localLOGV) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Performing destroy of ");
            stringBuilder.append(r);
            stringBuilder.append(" finishing:");
            stringBuilder.append(finishing);
            Slog.v(str, stringBuilder.toString());
        }
        if (r != null) {
            activityClass = r.activity.getClass();
            Activity activity = r.activity;
            activity.mConfigChangeFlags |= configChanges;
            if (finishing) {
                r.activity.mFinished = true;
            }
            performPauseActivityIfNeeded(r, "destroy");
            if (!r.stopped) {
                callActivityOnStop(r, false, "destroy");
            }
            if (getNonConfigInstance) {
                try {
                    r.lastNonConfigurationInstances = r.activity.retainNonConfigurationInstances();
                } catch (Exception e) {
                    if (!this.mInstrumentation.onException(r.activity, e)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unable to retain activity ");
                        stringBuilder2.append(r.intent.getComponent().toShortString());
                        stringBuilder2.append(": ");
                        stringBuilder2.append(e.toString());
                        throw new RuntimeException(stringBuilder2.toString(), e);
                    }
                }
            }
            try {
                r.activity.mCalled = false;
                this.mInstrumentation.callActivityOnDestroy(r.activity);
                if (r.activity.mCalled) {
                    if (r.window != null) {
                        r.window.closeAllPanels();
                    }
                    r.setState(6);
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Activity ");
                    stringBuilder.append(safeToComponentShortString(r.intent));
                    stringBuilder.append(" did not call through to super.onDestroy()");
                    throw new SuperNotCalledException(stringBuilder.toString());
                }
            } catch (SuperNotCalledException e2) {
                throw e2;
            } catch (Exception e3) {
                if (!this.mInstrumentation.onException(r.activity, e3)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to destroy activity ");
                    stringBuilder2.append(safeToComponentShortString(r.intent));
                    stringBuilder2.append(": ");
                    stringBuilder2.append(e3.toString());
                    throw new RuntimeException(stringBuilder2.toString(), e3);
                }
            }
        }
        this.mActivities.remove(token);
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Remove activity client record, r= ");
        stringBuilder.append(r);
        stringBuilder.append(" token= ");
        stringBuilder.append(token);
        Slog.d(str, stringBuilder.toString());
        StrictMode.decrementExpectedActivityCount(activityClass);
        return r;
    }

    private static String safeToComponentShortString(Intent intent) {
        ComponentName component = intent.getComponent();
        return component == null ? "[Unknown]" : component.toShortString();
    }

    public void handleDestroyActivity(IBinder token, boolean finishing, int configChanges, boolean getNonConfigInstance, String reason) {
        ActivityClientRecord r = performDestroyActivity(token, finishing, configChanges, getNonConfigInstance, reason);
        if (r != null) {
            cleanUpPendingRemoveWindows(r, finishing);
            WindowManager wm = r.activity.getWindowManager();
            View v = r.activity.mDecor;
            if (v != null) {
                if (r.activity.mVisibleFromServer) {
                    this.mNumVisibleActivities--;
                }
                IBinder wtoken = v.getWindowToken();
                if (r.activity.mWindowAdded) {
                    if (r.mPreserveWindow) {
                        r.mPendingRemoveWindow = r.window;
                        r.mPendingRemoveWindowManager = wm;
                        r.window.clearContentView();
                    } else {
                        wm.removeViewImmediate(v);
                    }
                }
                if (wtoken != null && r.mPendingRemoveWindow == null) {
                    WindowManagerGlobal.getInstance().closeAll(wtoken, r.activity.getClass().getName(), "Activity");
                } else if (r.mPendingRemoveWindow != null) {
                    WindowManagerGlobal.getInstance().closeAllExceptView(token, v, r.activity.getClass().getName(), "Activity");
                }
                r.activity.mDecor = null;
            }
            if (r.mPendingRemoveWindow == null) {
                WindowManagerGlobal.getInstance().closeAll(token, r.activity.getClass().getName(), "Activity");
            }
            Context c = r.activity.getBaseContext();
            if (c instanceof ContextImpl) {
                ((ContextImpl) c).scheduleFinalCleanup(r.activity.getClass().getName(), "Activity");
            }
        }
        if (finishing) {
            try {
                ActivityManager.getService().activityDestroyed(token);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
        this.mSomeActivitiesChanged = true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:27:0x0073  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ActivityClientRecord prepareRelaunchActivity(IBinder token, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingNewIntents, int configChanges, MergedConfiguration config, boolean preserveWindow) {
        ActivityClientRecord target = null;
        boolean scheduleRelaunch = false;
        if (HwPCUtils.isValidExtDisplayId(this.mDisplayId)) {
            config.setGlobalConfiguration(updateConfig(config.getGlobalConfiguration()));
        }
        synchronized (this.mResourcesManager) {
            int i = 0;
            while (i < this.mRelaunchingActivities.size()) {
                ActivityClientRecord r = (ActivityClientRecord) this.mRelaunchingActivities.get(i);
                if (DEBUG_ORDER) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("requestRelaunchActivity: ");
                    stringBuilder.append(this);
                    stringBuilder.append(", trying: ");
                    stringBuilder.append(r);
                    Slog.d(str, stringBuilder.toString());
                }
                if (r.token == token) {
                    target = r;
                    if (pendingResults != null) {
                        if (r.pendingResults != null) {
                            r.pendingResults.addAll(pendingResults);
                        } else {
                            r.pendingResults = pendingResults;
                        }
                    }
                    if (pendingNewIntents != null) {
                        if (r.pendingIntents != null) {
                            r.pendingIntents.addAll(pendingNewIntents);
                        } else {
                            r.pendingIntents = pendingNewIntents;
                        }
                    }
                    if (target == null) {
                        if (DEBUG_ORDER) {
                            Slog.d(TAG, "requestRelaunchActivity: target is null");
                        }
                        target = new ActivityClientRecord();
                        target.token = token;
                        target.pendingResults = pendingResults;
                        target.pendingIntents = pendingNewIntents;
                        target.mPreserveWindow = preserveWindow;
                        this.mRelaunchingActivities.add(target);
                        scheduleRelaunch = true;
                    }
                    target.createdConfig = config.getGlobalConfiguration();
                    target.overrideConfig = config.getOverrideConfiguration();
                    target.pendingConfigChanges |= configChanges;
                } else {
                    i++;
                }
            }
            if (target == null) {
            }
            target.createdConfig = config.getGlobalConfiguration();
            target.overrideConfig = config.getOverrideConfiguration();
            target.pendingConfigChanges |= configChanges;
        }
        return scheduleRelaunch ? target : null;
    }

    /* JADX WARNING: Missing block: B:33:0x0059, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:39:0x0067, code skipped:
            if (r12.createdConfig == null) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:41:0x006b, code skipped:
            if (r10.mConfiguration == null) goto L_0x0081;
     */
    /* JADX WARNING: Missing block: B:43:0x0075, code skipped:
            if (r12.createdConfig.isOtherSeqNewer(r10.mConfiguration) == false) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:45:0x007f, code skipped:
            if (r10.mConfiguration.diff(r12.createdConfig) == 0) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:46:0x0081, code skipped:
            if (r1 == null) goto L_0x008b;
     */
    /* JADX WARNING: Missing block: B:48:0x0089, code skipped:
            if (r12.createdConfig.isOtherSeqNewer(r1) == false) goto L_0x008d;
     */
    /* JADX WARNING: Missing block: B:49:0x008b, code skipped:
            r1 = r12.createdConfig;
     */
    /* JADX WARNING: Missing block: B:50:0x008d, code skipped:
            r14 = r1;
     */
    /* JADX WARNING: Missing block: B:51:0x008e, code skipped:
            if (r14 == null) goto L_0x009a;
     */
    /* JADX WARNING: Missing block: B:52:0x0090, code skipped:
            r10.mCurDefaultDisplayDpi = r14.densityDpi;
            updateDefaultDensity();
            handleConfigurationChanged(r14, null);
     */
    /* JADX WARNING: Missing block: B:53:0x009a, code skipped:
            r15 = (android.app.ActivityThread.ActivityClientRecord) r10.mActivities.get(r12.token);
     */
    /* JADX WARNING: Missing block: B:54:0x00a7, code skipped:
            if (localLOGV == false) goto L_0x00d3;
     */
    /* JADX WARNING: Missing block: B:55:0x00a9, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Handling relaunch of ");
            r2.append(r15);
            r2.append(": changedConfig=");
            r2.append(r14);
            r2.append(" with configChanges=0x");
            r2.append(java.lang.Integer.toHexString(r13));
            android.util.Slog.v(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:56:0x00d3, code skipped:
            if (r15 != null) goto L_0x00d6;
     */
    /* JADX WARNING: Missing block: B:57:0x00d5, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:58:0x00d6, code skipped:
            r1 = r15.activity;
            r1.mConfigChangeFlags |= r13;
            r15.mPreserveWindow = r12.mPreserveWindow;
            r15.activity.mChangingConfigurations = true;
     */
    /* JADX WARNING: Missing block: B:61:0x00e7, code skipped:
            if (r15.mPreserveWindow == false) goto L_0x00f2;
     */
    /* JADX WARNING: Missing block: B:62:0x00e9, code skipped:
            android.view.WindowManagerGlobal.getWindowSession().prepareToReplaceWindows(r15.token, true);
     */
    /* JADX WARNING: Missing block: B:63:0x00f2, code skipped:
            handleRelaunchActivityInner(r15, r13, r12.pendingResults, r12.pendingIntents, r11, r12.startsNotResumed, r12.overrideConfig, "handleRelaunchActivity");
     */
    /* JADX WARNING: Missing block: B:64:0x0104, code skipped:
            if (r11 == null) goto L_0x0109;
     */
    /* JADX WARNING: Missing block: B:65:0x0106, code skipped:
            r11.setReportRelaunchToWindowManager(true);
     */
    /* JADX WARNING: Missing block: B:66:0x0109, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:67:0x010a, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:69:0x010f, code skipped:
            throw r0.rethrowFromSystemServer();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void handleRelaunchActivity(ActivityClientRecord tmp, PendingTransactionActions pendingActions) {
        Throwable th;
        PendingTransactionActions pendingTransactionActions = pendingActions;
        unscheduleGcIdler();
        this.mSomeActivitiesChanged = true;
        Configuration changedConfig = null;
        synchronized (this.mResourcesManager) {
            ActivityClientRecord activityClientRecord;
            ActivityClientRecord tmp2;
            try {
                int N = this.mRelaunchingActivities.size();
                activityClientRecord = tmp;
                try {
                    IBinder token = activityClientRecord.token;
                    int i = 0;
                    int configChanges = 0;
                    tmp2 = null;
                    while (true) {
                        int i2 = i;
                        if (i2 >= N) {
                            break;
                        }
                        try {
                            activityClientRecord = (ActivityClientRecord) this.mRelaunchingActivities.get(i2);
                            if (activityClientRecord.token == token) {
                                int configChanges2;
                                ActivityClientRecord tmp3 = activityClientRecord;
                                try {
                                    configChanges2 = tmp3.pendingConfigChanges | configChanges;
                                } catch (Throwable th2) {
                                    th = th2;
                                    tmp2 = tmp3;
                                    while (true) {
                                        try {
                                            break;
                                        } catch (Throwable th3) {
                                            th = th3;
                                        }
                                    }
                                    throw th;
                                }
                                try {
                                    this.mRelaunchingActivities.remove(i2);
                                    i2--;
                                    N--;
                                    tmp2 = tmp3;
                                    configChanges = configChanges2;
                                } catch (Throwable th4) {
                                    th = th4;
                                    tmp2 = tmp3;
                                    i2 = configChanges2;
                                    while (true) {
                                        break;
                                    }
                                    throw th;
                                }
                            }
                            i = i2 + 1;
                        } catch (Throwable th5) {
                            th = th5;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    }
                    if (tmp2 == null) {
                        if (DEBUG_CONFIGURATION) {
                            Slog.v(TAG, "Abort, activity not relaunching!");
                        }
                    } else if (this.mPendingConfiguration != null) {
                        changedConfig = this.mPendingConfiguration;
                        this.mPendingConfiguration = null;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    tmp2 = activityClientRecord;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
                activityClientRecord = tmp;
                tmp2 = activityClientRecord;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    void scheduleRelaunchActivity(IBinder token) {
        scheduleRelaunchActivity(token, false);
    }

    void scheduleRelaunchActivity(IBinder token, boolean fromThemeChange) {
        sendMessage(160, token, fromThemeChange);
    }

    private void handleRelaunchActivityLocally(IBinder token, boolean fromThemeChange) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r == null) {
            Log.w(TAG, "Activity to relaunch no longer exists");
            return;
        }
        int prevState = r.getLifecycleState();
        if (prevState == 5 && fromThemeChange) {
            Log.w(TAG, "Activity can not relaunch in state ON_STOP and fromThemeChange");
        } else if (prevState < 3 || prevState > 5) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Activity state must be in [ON_RESUME..ON_STOP] in order to be relaunched,current state is ");
            stringBuilder.append(prevState);
            Log.w(str, stringBuilder.toString());
        } else {
            ActivityRelaunchItem activityRelaunchItem = ActivityRelaunchItem.obtain(null, null, null, new MergedConfiguration(r.createdConfig != null ? r.createdConfig : this.mConfiguration, r.overrideConfig), r.mPreserveWindow);
            ActivityLifecycleItem lifecycleRequest = TransactionExecutorHelper.getLifecycleRequestForCurrentState(r);
            ClientTransaction transaction = ClientTransaction.obtain(this.mAppThread, r.token);
            transaction.addCallback(activityRelaunchItem);
            transaction.setLifecycleStateRequest(lifecycleRequest);
            executeTransaction(transaction);
        }
    }

    private void handleRelaunchActivityInner(ActivityClientRecord r, int configChanges, List<ResultInfo> pendingResults, List<ReferrerIntent> pendingIntents, PendingTransactionActions pendingActions, boolean startsNotResumed, Configuration overrideConfig, String reason) {
        ActivityClientRecord activityClientRecord = r;
        List<ResultInfo> list = pendingResults;
        List<ReferrerIntent> list2 = pendingIntents;
        String str = reason;
        Intent customIntent = activityClientRecord.activity.mIntent;
        if (!activityClientRecord.paused) {
            performPauseActivity(activityClientRecord, false, str, null);
        }
        if (!activityClientRecord.stopped) {
            callActivityOnStop(activityClientRecord, true, str);
        }
        handleDestroyActivity(activityClientRecord.token, false, configChanges, true, str);
        activityClientRecord.activity = null;
        activityClientRecord.window = null;
        activityClientRecord.hideForNow = false;
        activityClientRecord.nextIdle = null;
        if (list != null) {
            if (activityClientRecord.pendingResults == null) {
                activityClientRecord.pendingResults = list;
            } else {
                activityClientRecord.pendingResults.addAll(list);
            }
        }
        if (list2 != null) {
            if (activityClientRecord.pendingIntents == null) {
                activityClientRecord.pendingIntents = list2;
            } else {
                activityClientRecord.pendingIntents.addAll(list2);
            }
        }
        activityClientRecord.startsNotResumed = startsNotResumed;
        activityClientRecord.overrideConfig = overrideConfig;
        handleLaunchActivity(activityClientRecord, pendingActions, customIntent);
    }

    public void reportRelaunch(IBinder token, PendingTransactionActions pendingActions) {
        try {
            ActivityManager.getService().activityRelaunched(token);
            ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
            if (pendingActions.shouldReportRelaunchToWindowManager() && r != null && r.window != null) {
                r.window.reportActivityRelaunched();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void callActivityOnSaveInstanceState(ActivityClientRecord r) {
        r.state = new Bundle();
        r.state.setAllowFds(false);
        if (r.isPersistable()) {
            r.persistentState = new PersistableBundle();
            this.mInstrumentation.callActivityOnSaveInstanceState(r.activity, r.state, r.persistentState);
            return;
        }
        this.mInstrumentation.callActivityOnSaveInstanceState(r.activity, r.state);
    }

    ArrayList<ComponentCallbacks2> collectComponentCallbacks(boolean allActivities, Configuration newConfig) {
        int i;
        ArrayList<ComponentCallbacks2> callbacks = new ArrayList();
        synchronized (this.mResourcesManager) {
            int i2;
            int i3;
            int NAPP = this.mAllApplications.size();
            i = 0;
            for (i2 = 0; i2 < NAPP; i2++) {
                callbacks.add((ComponentCallbacks2) this.mAllApplications.get(i2));
            }
            i2 = this.mActivities.size();
            for (i3 = 0; i3 < i2; i3++) {
                ActivityClientRecord ar = (ActivityClientRecord) this.mActivities.valueAt(i3);
                Activity a = ar.activity;
                if (a != null) {
                    Configuration thisConfig = applyConfigCompatMainThread(this.mCurDefaultDisplayDpi, newConfig, ar.packageInfo.getCompatibilityInfo());
                    if (!ar.activity.mFinished && (allActivities || !ar.paused)) {
                        callbacks.add(a);
                    } else if (thisConfig != null) {
                        if (DEBUG_CONFIGURATION) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Setting activity ");
                            stringBuilder.append(ar.activityInfo.name);
                            stringBuilder.append(" newConfig=");
                            stringBuilder.append(thisConfig);
                            Slog.v(str, stringBuilder.toString());
                        }
                        ar.newConfig = thisConfig;
                    }
                }
            }
            i3 = this.mServices.size();
            for (int i4 = 0; i4 < i3; i4++) {
                callbacks.add((ComponentCallbacks2) this.mServices.valueAt(i4));
            }
        }
        synchronized (this.mProviderMap) {
            int NPRV = this.mLocalProviders.size();
            while (i < NPRV) {
                callbacks.add(((ProviderClientRecord) this.mLocalProviders.valueAt(i)).mLocalProvider);
                i++;
            }
        }
        return callbacks;
    }

    private void performConfigurationChangedForActivity(ActivityClientRecord r, Configuration newBaseConfig) {
        performConfigurationChangedForActivity(r, newBaseConfig, r.activity.getDisplay().getDisplayId(), false);
    }

    private Configuration performConfigurationChangedForActivity(ActivityClientRecord r, Configuration newBaseConfig, int displayId, boolean movedToDifferentDisplay) {
        r.tmpConfig.setTo(newBaseConfig);
        if (r.overrideConfig != null) {
            r.tmpConfig.updateFrom(r.overrideConfig);
        }
        Configuration reportedConfig = performActivityConfigurationChanged(r.activity, r.tmpConfig, r.overrideConfig, displayId, movedToDifferentDisplay);
        freeTextLayoutCachesIfNeeded(r.activity.mCurrentConfig.diff(r.tmpConfig));
        return reportedConfig;
    }

    private static Configuration createNewConfigAndUpdateIfNotNull(Configuration base, Configuration override) {
        if (override == null) {
            return base;
        }
        Configuration newConfig = new Configuration(base);
        newConfig.updateFrom(override);
        return newConfig;
    }

    private void performConfigurationChanged(ComponentCallbacks2 cb, Configuration newConfig) {
        Configuration contextThemeWrapperOverrideConfig = null;
        if (cb instanceof ContextThemeWrapper) {
            contextThemeWrapperOverrideConfig = ((ContextThemeWrapper) cb).getOverrideConfiguration();
        }
        cb.onConfigurationChanged(createNewConfigAndUpdateIfNotNull(newConfig, contextThemeWrapperOverrideConfig));
    }

    private Configuration performActivityConfigurationChanged(Activity activity, Configuration newConfig, Configuration amOverrideConfig, int displayId, boolean movedToDifferentDisplay) {
        if (activity != null) {
            IBinder activityToken = activity.getActivityToken();
            if (activityToken != null) {
                boolean shouldChangeConfig = false;
                if (activity.mCurrentConfig == null) {
                    shouldChangeConfig = true;
                } else {
                    int diff = activity.mCurrentConfig.diffPublicOnly(newConfig);
                    if (!(diff == 0 && this.mResourcesManager.isSameResourcesOverrideConfig(activityToken, amOverrideConfig)) && (!this.mUpdatingSystemConfig || ((~activity.mActivityInfo.getRealConfigChanged()) & diff) == 0)) {
                        shouldChangeConfig = true;
                    }
                }
                if (!shouldChangeConfig && !movedToDifferentDisplay) {
                    return null;
                }
                Configuration contextThemeWrapperOverrideConfig = activity.getOverrideConfiguration();
                this.mResourcesManager.updateResourcesForActivity(activityToken, createNewConfigAndUpdateIfNotNull(amOverrideConfig, contextThemeWrapperOverrideConfig), displayId, movedToDifferentDisplay);
                activity.mConfigChangeFlags = 0;
                activity.mCurrentConfig = new Configuration(newConfig);
                Configuration configToReport = createNewConfigAndUpdateIfNotNull(newConfig, contextThemeWrapperOverrideConfig);
                if (movedToDifferentDisplay) {
                    activity.dispatchMovedToDisplay(displayId, configToReport);
                }
                if (shouldChangeConfig) {
                    activity.mCalled = false;
                    if (!(activity.getResources() == null || activity.getResources().getCompatibilityInfo() == null)) {
                        CompatibilityInfo info = activity.getResources().getCompatibilityInfo();
                        if (!info.supportsScreen()) {
                            info.applyToConfiguration(configToReport.densityDpi, configToReport);
                        }
                    }
                    activity.onConfigurationChanged(configToReport);
                    if (!activity.mCalled) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Activity ");
                        stringBuilder.append(activity.getLocalClassName());
                        stringBuilder.append(" did not call through to super.onConfigurationChanged()");
                        throw new SuperNotCalledException(stringBuilder.toString());
                    }
                }
                return configToReport;
            }
            throw new IllegalArgumentException("Activity token not set. Is the activity attached?");
        }
        throw new IllegalArgumentException("No activity provided.");
    }

    public final void applyConfigurationToResources(Configuration config) {
        synchronized (this.mResourcesManager) {
            this.mResourcesManager.applyConfigurationToResourcesLocked(config, null);
        }
    }

    final Configuration applyCompatConfiguration(int displayDensity) {
        Configuration config = this.mConfiguration;
        if (this.mCompatConfiguration == null) {
            this.mCompatConfiguration = new Configuration();
        }
        this.mCompatConfiguration.setTo(this.mConfiguration);
        if (this.mResourcesManager.applyCompatConfigurationLocked(displayDensity, this.mCompatConfiguration)) {
            return this.mCompatConfiguration;
        }
        return config;
    }

    public void handleConfigurationChanged(Configuration config) {
        Trace.traceBegin(64, "configChanged");
        this.mCurDefaultDisplayDpi = config.densityDpi;
        this.mUpdatingSystemConfig = true;
        try {
            handleConfigurationChanged(config, null);
            Trace.traceEnd(64);
        } finally {
            this.mUpdatingSystemConfig = false;
        }
    }

    /* JADX WARNING: Missing block: B:40:0x00f5, code skipped:
            r5 = collectComponentCallbacks(false, r13);
            freeTextLayoutCachesIfNeeded(r0);
     */
    /* JADX WARNING: Missing block: B:41:0x0100, code skipped:
            if (android.hwtheme.HwThemeManager.setThemeFontOnConfigChg(r13) == false) goto L_0x0105;
     */
    /* JADX WARNING: Missing block: B:42:0x0102, code skipped:
            android.graphics.Typeface.loadSystemFonts();
     */
    /* JADX WARNING: Missing block: B:44:0x0109, code skipped:
            if (currentPackageName() == null) goto L_0x0135;
     */
    /* JADX WARNING: Missing block: B:46:0x0115, code skipped:
            if (currentPackageName().contains("com.tencent.mm") == false) goto L_0x0135;
     */
    /* JADX WARNING: Missing block: B:47:0x0117, code skipped:
            r7 = TAG;
            r8 = new java.lang.StringBuilder();
            r8.append("ActivityThread.handleConfigurationChanged , new config = ");
            r8.append(r13);
            r8.append(", compat = ");
            r8.append(r14);
            android.util.Slog.i(r7, r8.toString());
     */
    /* JADX WARNING: Missing block: B:48:0x0135, code skipped:
            if (r5 == null) goto L_0x017f;
     */
    /* JADX WARNING: Missing block: B:49:0x0137, code skipped:
            r7 = r5.size();
     */
    /* JADX WARNING: Missing block: B:50:0x013c, code skipped:
            if (r1 >= r7) goto L_0x0199;
     */
    /* JADX WARNING: Missing block: B:51:0x013e, code skipped:
            r8 = (android.content.ComponentCallbacks2) r5.get(r1);
     */
    /* JADX WARNING: Missing block: B:52:0x0146, code skipped:
            if ((r8 instanceof android.app.Activity) == false) goto L_0x015b;
     */
    /* JADX WARNING: Missing block: B:53:0x0148, code skipped:
            performConfigurationChangedForActivity((android.app.ActivityThread.ActivityClientRecord) r12.mActivities.get(((android.app.Activity) r8).getActivityToken()), r13);
     */
    /* JADX WARNING: Missing block: B:54:0x015b, code skipped:
            if (r2 != false) goto L_0x0161;
     */
    /* JADX WARNING: Missing block: B:55:0x015d, code skipped:
            performConfigurationChanged(r8, r13);
     */
    /* JADX WARNING: Missing block: B:56:0x0161, code skipped:
            r9 = TAG;
            r10 = new java.lang.StringBuilder();
            r10.append("Skipping handle non-activity callbacks for app:");
            r10.append(currentPackageName());
            android.util.Slog.v(r9, r10.toString());
     */
    /* JADX WARNING: Missing block: B:57:0x017b, code skipped:
            r1 = r1 + 1;
     */
    /* JADX WARNING: Missing block: B:58:0x017f, code skipped:
            r1 = TAG;
            r7 = new java.lang.StringBuilder();
            r7.append("There are no configuration change callbacks for app:");
            r7.append(currentPackageName());
            android.util.Slog.v(r1, r7.toString());
     */
    /* JADX WARNING: Missing block: B:59:0x0199, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleConfigurationChanged(Configuration config, CompatibilityInfo compat) {
        int i = 0;
        boolean equivalent = (config == null || this.mConfiguration == null || this.mConfiguration.diffPublicOnly(config) != 0) ? false : true;
        Theme systemTheme = getSystemContext().getTheme();
        Theme systemUiTheme = getSystemUiContext().getTheme();
        synchronized (this.mResourcesManager) {
            if (this.mPendingConfiguration != null) {
                if (!this.mPendingConfiguration.isOtherSeqNewer(config)) {
                    config = this.mPendingConfiguration;
                    this.mCurDefaultDisplayDpi = config.densityDpi;
                    updateDefaultDensity();
                }
                this.mPendingConfiguration = null;
            }
            String str;
            StringBuilder stringBuilder;
            if (config == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Config is null for app:");
                stringBuilder.append(currentPackageName());
                Slog.v(str, stringBuilder.toString());
                return;
            }
            if (DEBUG_CONFIGURATION) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Handle configuration changed: ");
                stringBuilder2.append(config);
                Slog.v(str2, stringBuilder2.toString());
            }
            this.mResourcesManager.applyConfigurationToResourcesLocked(config, compat);
            updateLocaleListFromAppContext(this.mInitialApplication.getApplicationContext(), this.mResourcesManager.getConfiguration().getLocales());
            if (this.mConfiguration == null) {
                this.mConfiguration = new Configuration();
            }
            if (this.mConfiguration.isOtherSeqNewer(config) || compat != null) {
                int configDiff = this.mConfiguration.updateFrom(config);
                config = applyCompatConfiguration(this.mCurDefaultDisplayDpi);
                if ((systemTheme.getChangingConfigurations() & configDiff) != 0) {
                    systemTheme.rebase();
                }
                if ((systemUiTheme.getChangingConfigurations() & configDiff) != 0) {
                    systemUiTheme.rebase();
                }
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping new config:");
                stringBuilder.append(this.mConfiguration);
                stringBuilder.append(", config:");
                stringBuilder.append(config);
                stringBuilder.append(" for app:");
                stringBuilder.append(currentPackageName());
                Slog.v(str, stringBuilder.toString());
            }
        }
    }

    void handleApplicationInfoChanged(ApplicationInfo ai, boolean fromThemeChange) {
        LoadedApk apk;
        LoadedApk resApk;
        ArrayList<String> oldPaths;
        synchronized (this.mResourcesManager) {
            WeakReference<LoadedApk> ref = (WeakReference) this.mPackages.get(ai.packageName);
            apk = ref != null ? (LoadedApk) ref.get() : null;
            ref = (WeakReference) this.mResourcePackages.get(ai.packageName);
            resApk = ref != null ? (LoadedApk) ref.get() : null;
        }
        if (apk != null) {
            oldPaths = new ArrayList();
            LoadedApk.makePaths(this, apk.getApplicationInfo(), oldPaths);
            apk.updateApplicationInfo(ai, oldPaths);
        }
        if (resApk != null) {
            oldPaths = new ArrayList();
            LoadedApk.makePaths(this, resApk.getApplicationInfo(), oldPaths);
            resApk.updateApplicationInfo(ai, oldPaths);
        }
        synchronized (this.mResourcesManager) {
            this.mResourcesManager.applyNewResourceDirsLocked(ai.sourceDir, ai.resourceDirs);
        }
        ApplicationPackageManager.configurationChanged();
        Configuration newConfig = new Configuration();
        newConfig.assetsSeq = (this.mConfiguration != null ? this.mConfiguration.assetsSeq : 0) + 1;
        handleConfigurationChanged(newConfig, null);
        relaunchAllActivities(fromThemeChange);
    }

    static void freeTextLayoutCachesIfNeeded(int configDiff) {
        if (configDiff != 0) {
            if ((configDiff & 4) != 0) {
                Canvas.freeTextLayoutCaches();
                if (DEBUG_CONFIGURATION) {
                    Slog.v(TAG, "Cleared TextLayout Caches");
                }
            }
        }
    }

    public void handleActivityConfigurationChanged(IBinder activityToken, Configuration overrideConfig, int displayId) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(activityToken);
        if (r == null || r.activity == null) {
            if (DEBUG_CONFIGURATION) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not found target activity to report to: ");
                stringBuilder.append(r);
                Slog.w(str, stringBuilder.toString());
            }
            return;
        }
        boolean movedToDifferentDisplay = (displayId == -1 || displayId == r.activity.getDisplay().getDisplayId()) ? false : true;
        r.overrideConfig = overrideConfig;
        ViewRootImpl viewRoot = r.activity.mDecor != null ? r.activity.mDecor.getViewRootImpl() : null;
        String str2;
        StringBuilder stringBuilder2;
        if (movedToDifferentDisplay) {
            if (DEBUG_CONFIGURATION) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Handle activity moved to display, activity:");
                stringBuilder2.append(r.activityInfo.name);
                stringBuilder2.append(", displayId=");
                stringBuilder2.append(displayId);
                stringBuilder2.append(", config=");
                stringBuilder2.append(overrideConfig);
                Slog.v(str2, stringBuilder2.toString());
            }
            Configuration reportedConfig = performConfigurationChangedForActivity(r, this.mCompatConfiguration, displayId, true);
            if (viewRoot != null) {
                viewRoot.onMovedToDisplay(displayId, reportedConfig);
            }
        } else {
            if (DEBUG_CONFIGURATION) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Handle activity config changed: ");
                stringBuilder2.append(r.activityInfo.name);
                stringBuilder2.append(", config=");
                stringBuilder2.append(overrideConfig);
                Slog.v(str2, stringBuilder2.toString());
            }
            performConfigurationChangedForActivity(r, this.mCompatConfiguration);
        }
        if (viewRoot != null) {
            viewRoot.updateConfiguration(displayId);
        }
        this.mSomeActivitiesChanged = true;
    }

    final void handleProfilerControl(boolean start, ProfilerInfo profilerInfo, int profileType) {
        if (start) {
            try {
                this.mProfiler.setProfiler(profilerInfo);
                this.mProfiler.startProfiling();
            } catch (RuntimeException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Profiling failed on path ");
                stringBuilder.append(profilerInfo.profileFile);
                stringBuilder.append(" -- can the process access this path?");
                Slog.w(str, stringBuilder.toString());
            } catch (Throwable th) {
                profilerInfo.closeFd();
            }
            profilerInfo.closeFd();
            return;
        }
        this.mProfiler.stopProfiling();
    }

    public void stopProfiling() {
        if (this.mProfiler != null) {
            this.mProfiler.stopProfiling();
        }
    }

    static void handleDumpHeap(DumpHeapData dhd) {
        if (dhd.runGc) {
            System.gc();
            System.runFinalization();
            System.gc();
        }
        if (dhd.managed) {
            try {
                Debug.dumpHprofData(dhd.path, dhd.fd.getFileDescriptor());
                try {
                    dhd.fd.close();
                } catch (IOException e) {
                    Slog.w(TAG, "Failure closing profile fd", e);
                }
            } catch (IOException e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Managed heap dump failed on path ");
                stringBuilder.append(dhd.path);
                stringBuilder.append(" -- can the process access this path?");
                Slog.w(str, stringBuilder.toString());
                dhd.fd.close();
            } catch (Throwable th) {
                try {
                    dhd.fd.close();
                } catch (IOException e3) {
                    Slog.w(TAG, "Failure closing profile fd", e3);
                }
                throw th;
            }
        } else if (dhd.mallocInfo) {
            Debug.dumpNativeMallocInfo(dhd.fd.getFileDescriptor());
        } else {
            Debug.dumpNativeHeap(dhd.fd.getFileDescriptor());
        }
        try {
            ActivityManager.getService().dumpHeapFinished(dhd.path);
        } catch (RemoteException e4) {
            throw e4.rethrowFromSystemServer();
        }
    }

    final void handleDispatchPackageBroadcast(int cmd, String[] packages) {
        int i;
        boolean hasPkgInfo = false;
        if (cmd != 0) {
            switch (cmd) {
                case 2:
                    break;
                case 3:
                    if (packages != null) {
                        synchronized (this.mResourcesManager) {
                            int i2 = packages.length - 1;
                            while (true) {
                                i = i2;
                                if (i >= 0) {
                                    WeakReference<LoadedApk> ref = (WeakReference) this.mPackages.get(packages[i]);
                                    LoadedApk loadedApk = null;
                                    LoadedApk pkgInfo = ref != null ? (LoadedApk) ref.get() : null;
                                    if (pkgInfo != null) {
                                        hasPkgInfo = true;
                                    } else {
                                        ref = (WeakReference) this.mResourcePackages.get(packages[i]);
                                        if (ref != null) {
                                            loadedApk = (LoadedApk) ref.get();
                                        }
                                        pkgInfo = loadedApk;
                                        if (pkgInfo != null) {
                                            hasPkgInfo = true;
                                        }
                                    }
                                    if (pkgInfo != null) {
                                        try {
                                            String packageName = packages[i];
                                            ApplicationInfo aInfo = sPackageManager.getApplicationInfo(packageName, 1024, UserHandle.myUserId());
                                            if (this.mActivities.size() > 0) {
                                                for (ActivityClientRecord ar : this.mActivities.values()) {
                                                    if (ar.activityInfo.applicationInfo.packageName.equals(packageName)) {
                                                        ar.activityInfo.applicationInfo = aInfo;
                                                        ar.packageInfo = pkgInfo;
                                                    }
                                                }
                                            }
                                            ArrayList<String> oldPaths = new ArrayList();
                                            LoadedApk.makePaths(this, pkgInfo.getApplicationInfo(), oldPaths);
                                            pkgInfo.updateApplicationInfo(aInfo, oldPaths);
                                        } catch (RemoteException e) {
                                        }
                                    }
                                    i2 = i - 1;
                                }
                            }
                        }
                        break;
                    }
                    break;
            }
        }
        boolean killApp = cmd == 0;
        if (packages != null) {
            synchronized (this.mResourcesManager) {
                int i3 = packages.length - 1;
                while (true) {
                    i = i3;
                    if (i >= 0) {
                        if (!hasPkgInfo) {
                            WeakReference<LoadedApk> ref2 = (WeakReference) this.mPackages.get(packages[i]);
                            if (ref2 == null || ref2.get() == null) {
                                ref2 = (WeakReference) this.mResourcePackages.get(packages[i]);
                                if (!(ref2 == null || ref2.get() == null)) {
                                    hasPkgInfo = true;
                                }
                            } else {
                                hasPkgInfo = true;
                            }
                        }
                        if (killApp) {
                            this.mPackages.remove(packages[i]);
                            this.mResourcePackages.remove(packages[i]);
                        }
                        i3 = i - 1;
                    }
                }
            }
        }
        ApplicationPackageManager.handlePackageBroadcast(cmd, packages, hasPkgInfo);
    }

    final void handleLowMemory() {
        ArrayList<ComponentCallbacks2> callbacks = collectComponentCallbacks(true, null);
        int N = callbacks.size();
        for (int i = 0; i < N; i++) {
            ((ComponentCallbacks2) callbacks.get(i)).onLowMemory();
        }
        if (Process.myUid() != 1000) {
            EventLog.writeEvent(SQLITE_MEM_RELEASED_EVENT_LOG_TAG, SQLiteDatabase.releaseMemory());
        }
        Canvas.freeCaches();
        Canvas.freeTextLayoutCaches();
        BinderInternal.forceGc("mem");
    }

    private void handleTrimMemory(int level, boolean fromIAware) {
        Trace.traceBegin(64, "trimMemory");
        if (!fromIAware) {
            ArrayList<ComponentCallbacks2> callbacks = collectComponentCallbacks(true, null);
            int N = callbacks.size();
            for (int i = 0; i < N; i++) {
                ((ComponentCallbacks2) callbacks.get(i)).onTrimMemory(level);
            }
        }
        WindowManagerGlobal.getInstance().trimMemory(level);
        Trace.traceEnd(64);
    }

    private void setupGraphicsSupport(Context context) {
        Trace.traceBegin(64, "setupGraphicsSupport");
        if (!"android".equals(context.getPackageName())) {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null) {
                System.setProperty("java.io.tmpdir", cacheDir.getAbsolutePath());
            } else {
                Log.v(TAG, "Unable to initialize \"java.io.tmpdir\" property due to missing cache directory");
            }
            File codeCacheDir = context.createDeviceProtectedStorageContext().getCodeCacheDir();
            if (codeCacheDir != null) {
                try {
                    if (getPackageManager().getPackagesForUid(Process.myUid()) != null) {
                        ThreadedRenderer.setupDiskCache(codeCacheDir);
                        RenderScriptCacheDir.setupDiskCache(codeCacheDir);
                    }
                } catch (RemoteException e) {
                    Trace.traceEnd(64);
                    throw e.rethrowFromSystemServer();
                }
            }
            Log.w(TAG, "Unable to use shader/script cache: missing code-cache directory");
        }
        GraphicsEnvironment.getInstance().setup(context);
        Trace.traceEnd(64);
    }

    private void updateDefaultDensity() {
        int densityDpi = this.mCurDefaultDisplayDpi;
        if (!this.mDensityCompatMode && densityDpi != 0 && densityDpi != DisplayMetrics.DENSITY_DEVICE) {
            DisplayMetrics.DENSITY_DEVICE = densityDpi;
            Bitmap.setDefaultDensity(densityDpi);
        }
    }

    private String getInstrumentationLibrary(ApplicationInfo appInfo, InstrumentationInfo insInfo) {
        if (!(appInfo.primaryCpuAbi == null || appInfo.secondaryCpuAbi == null || !appInfo.secondaryCpuAbi.equals(insInfo.secondaryCpuAbi))) {
            String secondaryIsa = VMRuntime.getInstructionSet(appInfo.secondaryCpuAbi);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ro.dalvik.vm.isa.");
            stringBuilder.append(secondaryIsa);
            String secondaryDexCodeIsa = SystemProperties.get(stringBuilder.toString());
            if (VMRuntime.getRuntime().vmInstructionSet().equals(secondaryDexCodeIsa.isEmpty() ? secondaryIsa : secondaryDexCodeIsa)) {
                return insInfo.secondaryNativeLibraryDir;
            }
        }
        return insInfo.nativeLibraryDir;
    }

    private void updateLocaleListFromAppContext(Context context, LocaleList newLocaleList) {
        int i = 0;
        Locale bestLocale = context.getResources().getConfiguration().getLocales().get(0);
        int newLocaleListSize = newLocaleList.size();
        while (i < newLocaleListSize) {
            if (bestLocale.equals(newLocaleList.get(i))) {
                LocaleList.setDefault(newLocaleList, i);
                return;
            }
            i++;
        }
        LocaleList.setDefault(new LocaleList(bestLocale, newLocaleList));
    }

    private void handleBindApplication(AppBindData data) {
        StringBuilder stringBuilder;
        InstrumentationInfo ii;
        ApplicationInfo instrApp;
        StringBuilder stringBuilder2;
        AppBindData appBindData = data;
        if (Jlog.isPerfTest()) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("pid=");
            stringBuilder3.append(Process.myPid());
            Jlog.i(3038, Jlog.getMessage(TAG, "handleBindApplication", stringBuilder3.toString()));
        }
        VMRuntime.registerSensitiveThread();
        if (appBindData.trackAllocation) {
            DdmVmInternal.enableRecentAllocations(true);
        }
        Process.setStartTimes(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
        this.mBoundApplication = appBindData;
        this.mConfiguration = new Configuration(appBindData.config);
        this.mCompatConfiguration = new Configuration(appBindData.config);
        this.mProfiler = new Profiler();
        String agent = null;
        if (appBindData.initProfilerInfo != null) {
            this.mProfiler.profileFile = appBindData.initProfilerInfo.profileFile;
            this.mProfiler.profileFd = appBindData.initProfilerInfo.profileFd;
            this.mProfiler.samplingInterval = appBindData.initProfilerInfo.samplingInterval;
            this.mProfiler.autoStopProfiler = appBindData.initProfilerInfo.autoStopProfiler;
            this.mProfiler.streamingOutput = appBindData.initProfilerInfo.streamingOutput;
            if (appBindData.initProfilerInfo.attachAgentDuringBind) {
                agent = appBindData.initProfilerInfo.agent;
            }
        }
        String agent2 = agent;
        if (USE_CACHE) {
            sendPreloadMessage(6, null, 0);
        }
        sendPreloadMessage(5, currentPackageName(), 0);
        Process.setArgV0(appBindData.processName);
        DdmHandleAppName.setAppName(appBindData.processName, UserHandle.myUserId());
        VMRuntime.setProcessPackageName(appBindData.appInfo.packageName);
        if (this.mProfiler.profileFd != null) {
            this.mProfiler.startProfiling();
        }
        if (appBindData.appInfo.targetSdkVersion <= 12) {
            AsyncTask.setDefaultExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        Message.updateCheckRecycle(appBindData.appInfo.targetSdkVersion);
        ImageDecoder.sApiLevel = appBindData.appInfo.targetSdkVersion;
        TimeZone.setDefault(null);
        LocaleList.setDefault(appBindData.config.getLocales());
        synchronized (this.mResourcesManager) {
            this.mResourcesManager.applyConfigurationToResourcesLocked(appBindData.config, appBindData.compatInfo);
            this.mCurDefaultDisplayDpi = appBindData.config.densityDpi;
            applyCompatConfiguration(this.mCurDefaultDisplayDpi);
        }
        appBindData.info = getPackageInfoNoCheck(appBindData.appInfo, appBindData.compatInfo);
        if (agent2 != null) {
            handleAttachAgent(agent2, appBindData.info);
        }
        if ((appBindData.appInfo.flags & 8192) == 0) {
            this.mDensityCompatMode = true;
            Bitmap.setDefaultDensity(160);
        }
        updateDefaultDensity();
        String use24HourSetting = this.mCoreSettings.getString("time_12_24");
        Boolean is24Hr = null;
        if (use24HourSetting != null) {
            is24Hr = "24".equals(use24HourSetting) ? Boolean.TRUE : Boolean.FALSE;
        }
        Boolean is24Hr2 = is24Hr;
        DateFormat.set24HourTimePref(is24Hr2);
        View.mDebugViewAttributes = this.mCoreSettings.getInt("debug_view_attributes", 0) != 0;
        StrictMode.initThreadDefaults(appBindData.appInfo);
        StrictMode.initVmDefaults(appBindData.appInfo);
        if ((appBindData.appInfo.flags & 129) != 0 && (appBindData.appInfo.hwFlags & 33554432) == 0) {
            HwFrameworkFactory.getLogException().setliblogparam(1, "");
        }
        sendPreloadMessage(1, appBindData, 0);
        try {
            Field field = Build.class.getDeclaredField("SERIAL");
            field.setAccessible(true);
            field.set(Build.class, appBindData.buildSerial);
        } catch (IllegalAccessException | NoSuchFieldException e) {
        }
        sendPreloadMessage(2, appBindData, 0);
        if (appBindData.debugMode != 0) {
            Debug.changeDebugPort(8100);
            String str;
            if (appBindData.debugMode == 2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Application ");
                stringBuilder.append(appBindData.info.getPackageName());
                stringBuilder.append(" is waiting for the debugger on port 8100...");
                Slog.w(str, stringBuilder.toString());
                IActivityManager mgr = ActivityManager.getService();
                try {
                    mgr.showWaitingForDebugger(this.mAppThread, true);
                    Debug.waitForDebugger();
                    try {
                        mgr.showWaitingForDebugger(this.mAppThread, false);
                    } catch (RemoteException ex) {
                        throw ex.rethrowFromSystemServer();
                    }
                } catch (RemoteException ex2) {
                    throw ex2.rethrowFromSystemServer();
                }
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Application ");
            stringBuilder.append(appBindData.info.getPackageName());
            stringBuilder.append(" can be debugged on port 8100...");
            Slog.w(str, stringBuilder.toString());
        }
        boolean isAppDebuggable = (2 & appBindData.appInfo.flags) != 0;
        Trace.setAppTracingAllowed(isAppDebuggable);
        boolean z = isAppDebuggable || Build.IS_DEBUGGABLE;
        ThreadedRenderer.setDebuggingEnabled(z);
        if (isAppDebuggable && appBindData.enableBinderTracking) {
            Binder.enableTracing();
        }
        Trace.traceBegin(64, "Setup proxies");
        IBinder b = ServiceManager.getService(Context.CONNECTIVITY_SERVICE);
        if (b != null) {
            try {
                Proxy.setHttpProxySystemProperty(IConnectivityManager.Stub.asInterface(b).getProxyForNetwork(null));
            } catch (RemoteException ex22) {
                Trace.traceEnd(64);
                throw ex22.rethrowFromSystemServer();
            }
        }
        Trace.traceEnd(64);
        if (appBindData.instrumentationName != null) {
            try {
                ii = new ApplicationPackageManager(null, getPackageManager()).getInstrumentationInfo(appBindData.instrumentationName, 0);
                if (!(Objects.equals(appBindData.appInfo.primaryCpuAbi, ii.primaryCpuAbi) && Objects.equals(appBindData.appInfo.secondaryCpuAbi, ii.secondaryCpuAbi))) {
                    String str2 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Package uses different ABI(s) than its instrumentation: package[");
                    stringBuilder4.append(appBindData.appInfo.packageName);
                    stringBuilder4.append("]: ");
                    stringBuilder4.append(appBindData.appInfo.primaryCpuAbi);
                    stringBuilder4.append(", ");
                    stringBuilder4.append(appBindData.appInfo.secondaryCpuAbi);
                    stringBuilder4.append(" instrumentation[");
                    stringBuilder4.append(ii.packageName);
                    stringBuilder4.append("]: ");
                    stringBuilder4.append(ii.primaryCpuAbi);
                    stringBuilder4.append(", ");
                    stringBuilder4.append(ii.secondaryCpuAbi);
                    Slog.w(str2, stringBuilder4.toString());
                }
                this.mInstrumentationPackageName = ii.packageName;
                this.mInstrumentationAppDir = ii.sourceDir;
                this.mInstrumentationSplitAppDirs = ii.splitSourceDirs;
                this.mInstrumentationLibDir = getInstrumentationLibrary(appBindData.appInfo, ii);
                this.mInstrumentedAppDir = appBindData.info.getAppDir();
                this.mInstrumentedSplitAppDirs = appBindData.info.getSplitAppDirs();
                this.mInstrumentedLibDir = appBindData.info.getLibDir();
            } catch (NameNotFoundException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to find instrumentation info for: ");
                stringBuilder.append(appBindData.instrumentationName);
                throw new RuntimeException(stringBuilder.toString());
            }
        }
        ii = null;
        this.mIsNeedStartUiProbe = true;
        InstrumentationInfo ii2 = ii;
        Context appContext = ContextImpl.createAppContext(this, appBindData.info);
        updateLocaleListFromAppContext(appContext, this.mResourcesManager.getConfiguration().getLocales());
        if (Process.isIsolated()) {
            ThreadedRenderer.setIsolatedProcess(true);
        } else {
            int oldMask = StrictMode.allowThreadDiskWritesMask();
            try {
                setupGraphicsSupport(appContext);
                StrictMode.setThreadPolicyMask(oldMask);
            } catch (Throwable th) {
                StrictMode.setThreadPolicyMask(oldMask);
                Throwable th2 = th;
            }
        }
        if (SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false)) {
            BaseDexClassLoader.setReporter(DexLoadReporter.getInstance());
        }
        Trace.traceBegin(64, "NetworkSecurityConfigProvider.install");
        NetworkSecurityConfigProvider.install(appContext);
        Trace.traceEnd(64);
        if (ii2 != null) {
            try {
                instrApp = getPackageManager().getApplicationInfo(ii2.packageName, 0, UserHandle.myUserId());
            } catch (RemoteException e3) {
                instrApp = null;
            }
            if (instrApp == null) {
                instrApp = new ApplicationInfo();
            }
            ApplicationInfo instrApp2 = instrApp;
            ii2.copyTo(instrApp2);
            instrApp2.initForUser(UserHandle.myUserId());
            is24Hr2 = getPackageInfo(instrApp2, appBindData.compatInfo, appContext.getClassLoader(), false, true, null);
            Context instrContext = ContextImpl.createAppContext(this, is24Hr2);
            Boolean pi;
            try {
                this.mInstrumentation = (Instrumentation) instrContext.getClassLoader().loadClass(appBindData.instrumentationName.getClassName()).newInstance();
                pi = is24Hr2;
                this.mInstrumentation.init(this, instrContext, appContext, new ComponentName(ii2.packageName, ii2.name), appBindData.instrumentationWatcher, appBindData.instrumentationUiAutomationConnection);
                if (!(this.mProfiler.profileFile == null || ii2.handleProfiling || this.mProfiler.profileFd != null)) {
                    this.mProfiler.handlingProfiling = true;
                    File file = new File(this.mProfiler.profileFile);
                    file.getParentFile().mkdirs();
                    Debug.startMethodTracing(file.toString(), 8388608);
                }
            } catch (Exception e4) {
                Context context = instrContext;
                pi = is24Hr2;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to instantiate instrumentation ");
                stringBuilder.append(appBindData.instrumentationName);
                stringBuilder.append(": ");
                stringBuilder.append(e4.toString());
                throw new RuntimeException(stringBuilder.toString(), e4);
            }
        }
        boolean z2 = isAppDebuggable;
        Boolean bool = is24Hr2;
        this.mInstrumentation = new Instrumentation();
        this.mInstrumentation.basicInit(this);
        if ((appBindData.appInfo.flags & 1048576) != 0) {
            VMRuntime.getRuntime().clearGrowthLimit();
        } else {
            VMRuntime.getRuntime().clampGrowthLimit();
        }
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
        ThreadPolicy writesAllowedPolicy = StrictMode.getThreadPolicy();
        Application app;
        try {
            app = appBindData.info.makeApplication(appBindData.restrictedBackupMode, null);
            sendPreloadMessage(3, appBindData.info, 0);
            app.setAutofillCompatibilityEnabled(appBindData.autofillCompatibilityEnabled);
            this.mInitialApplication = app;
            if (HwFrameworkFactory.getHwActivityThread() != null) {
                HwFrameworkFactory.getHwActivityThread().reportBindApplicationToAware(app, appBindData.processName);
            }
            if (!(appBindData.restrictedBackupMode || ArrayUtils.isEmpty(appBindData.providers))) {
                installContentProviders(app, appBindData.providers);
                this.mH.sendEmptyMessageDelayed(132, JobInfo.MIN_BACKOFF_MILLIS);
            }
            this.mInstrumentation.onCreate(appBindData.instrumentationArgs);
            this.mInstrumentation.callApplicationOnCreate(app);
            sendPreloadMessage(4, app, 0);
            HwThemeManager.initForThemeFont(this.mConfiguration);
            HwThemeManager.setThemeFont();
            if (!mChangedFont) {
                mChangedFont = true;
                Typeface.loadSystemFonts();
            }
        } catch (Exception e42) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Exception thrown in onCreate() of ");
            stringBuilder2.append(appBindData.instrumentationName);
            stringBuilder2.append(": ");
            stringBuilder2.append(e42.toString());
            throw new RuntimeException(stringBuilder2.toString(), e42);
        } catch (Exception e422) {
            if (!this.mInstrumentation.onException(app, e422)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to create application ");
                stringBuilder2.append(app.getClass().getName());
                stringBuilder2.append(": ");
                stringBuilder2.append(e422.toString());
                throw new RuntimeException(stringBuilder2.toString(), e422);
            }
        } catch (Throwable th3) {
            if (appBindData.appInfo.targetSdkVersion < 27 || StrictMode.getThreadPolicy().equals(writesAllowedPolicy)) {
                StrictMode.setThreadPolicy(savedPolicy);
            }
        }
        if (appBindData.appInfo.targetSdkVersion < 27 || StrictMode.getThreadPolicy().equals(writesAllowedPolicy)) {
            StrictMode.setThreadPolicy(savedPolicy);
        }
        if (Jlog.isPerfTest()) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("pid=");
            stringBuilder2.append(Process.myPid());
            Jlog.i(3039, Jlog.getMessage(TAG, "handleBindApplication", stringBuilder2.toString()));
        }
        FontsContract.setApplicationContextForResources(appContext);
        if (!Process.isIsolated()) {
            try {
                instrApp = getPackageManager().getApplicationInfo(appBindData.appInfo.packageName, 128, UserHandle.myUserId());
                if (instrApp.metaData != null) {
                    int preloadedFontsResource = instrApp.metaData.getInt(ApplicationInfo.METADATA_PRELOADED_FONTS, 0);
                    if (preloadedFontsResource != 0) {
                        appBindData.info.getResources().preloadFonts(preloadedFontsResource);
                    }
                }
            } catch (RemoteException e4222) {
                throw e4222.rethrowFromSystemServer();
            }
        }
    }

    final void finishInstrumentation(int resultCode, Bundle results) {
        IActivityManager am = ActivityManager.getService();
        if (this.mProfiler.profileFile != null && this.mProfiler.handlingProfiling && this.mProfiler.profileFd == null) {
            Debug.stopMethodTracing();
        }
        try {
            am.finishInstrumentation(this.mAppThread, resultCode, results);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    private void installContentProviders(Context context, List<ProviderInfo> providers) {
        ArrayList<ContentProviderHolder> results = new ArrayList();
        for (ProviderInfo cpi : providers) {
            if (DEBUG_PROVIDER) {
                StringBuilder buf = new StringBuilder(128);
                buf.append("Pub ");
                buf.append(cpi.authority);
                buf.append(": ");
                buf.append(cpi.name);
                Log.i(TAG, buf.toString());
            }
            ContentProviderHolder cph = installProvider(context, null, cpi, false, true, true);
            if (cph != null) {
                cph.noReleaseNeeded = true;
                results.add(cph);
            }
        }
        try {
            ActivityManager.getService().publishContentProviders(getApplicationThread(), results);
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0024, code skipped:
            if (r4 != null) goto L_0x003d;
     */
    /* JADX WARNING: Missing block: B:14:0x0026, code skipped:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("Failed to find provider info for ");
            r6.append(r1);
            android.util.Slog.e(r5, r6.toString());
     */
    /* JADX WARNING: Missing block: B:15:0x003c, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:17:0x004c, code skipped:
            return installProvider(r15, r4, r4.info, true, r4.noReleaseNeeded, r13).provider;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final IContentProvider acquireProvider(Context c, String auth, int userId, boolean stable) {
        Throwable th;
        RemoteException ex;
        String str = auth;
        int i = userId;
        IContentProvider provider = acquireExistingProvider(c, auth, userId, stable);
        if (provider != null) {
            return provider;
        }
        ContentProviderHolder holder = null;
        boolean z;
        try {
            synchronized (getGetProviderLock(str, i)) {
                try {
                    z = stable;
                    try {
                        holder = ActivityManager.getService().getContentProvider(getApplicationThread(), str, i, z);
                    } catch (Throwable th2) {
                        th = th2;
                        try {
                            throw th;
                        } catch (RemoteException e) {
                            ex = e;
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    z = stable;
                    throw th;
                }
            }
        } catch (RemoteException e2) {
            ex = e2;
            z = stable;
            throw ex.rethrowFromSystemServer();
        }
    }

    private Object getGetProviderLock(String auth, int userId) {
        Object lock;
        ProviderKey key = new ProviderKey(auth, userId);
        synchronized (this.mGetProviderLocks) {
            lock = this.mGetProviderLocks.get(key);
            if (lock == null) {
                lock = key;
                this.mGetProviderLocks.put(key, lock);
            }
        }
        return lock;
    }

    private final void incProviderRefLocked(ProviderRefCount prc, boolean stable) {
        int unstableDelta = 0;
        StringBuilder stringBuilder;
        if (stable) {
            prc.stableCount++;
            if (prc.stableCount == 1) {
                if (prc.removePending) {
                    if (DEBUG_PROVIDER) {
                        Slog.v(TAG, "incProviderRef: stable snatched provider from the jaws of death");
                    }
                    prc.removePending = false;
                    this.mH.removeMessages(131, prc);
                    unstableDelta = -1;
                }
                int unstableDelta2 = unstableDelta;
                try {
                    if (DEBUG_PROVIDER) {
                        String str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("incProviderRef Now stable - ");
                        stringBuilder.append(prc.holder.info.name);
                        stringBuilder.append(": unstableDelta=");
                        stringBuilder.append(unstableDelta2);
                        Slog.v(str, stringBuilder.toString());
                    }
                    ActivityManager.getService().refContentProvider(prc.holder.connection, 1, unstableDelta2);
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            return;
        }
        prc.unstableCount++;
        if (prc.unstableCount != 1) {
            return;
        }
        if (prc.removePending) {
            if (DEBUG_PROVIDER) {
                Slog.v(TAG, "incProviderRef: unstable snatched provider from the jaws of death");
            }
            prc.removePending = false;
            this.mH.removeMessages(131, prc);
            return;
        }
        try {
            if (DEBUG_PROVIDER) {
                String str2 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("incProviderRef: Now unstable - ");
                stringBuilder.append(prc.holder.info.name);
                Slog.v(str2, stringBuilder.toString());
            }
            ActivityManager.getService().refContentProvider(prc.holder.connection, 0, 1);
        } catch (RemoteException e2) {
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0058, code skipped:
            return r4;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final IContentProvider acquireExistingProvider(Context c, String auth, int userId, boolean stable) {
        synchronized (this.mProviderMap) {
            ProviderClientRecord pr = (ProviderClientRecord) this.mProviderMap.get(new ProviderKey(auth, userId));
            if (pr == null) {
                return null;
            }
            IContentProvider provider = pr.mProvider;
            IBinder jBinder = provider.asBinder();
            if (jBinder.isBinderAlive()) {
                ProviderRefCount prc = (ProviderRefCount) this.mProviderRefCountMap.get(jBinder);
                if (prc != null) {
                    incProviderRefLocked(prc, stable);
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Acquiring provider ");
                stringBuilder.append(auth);
                stringBuilder.append(" for user ");
                stringBuilder.append(userId);
                stringBuilder.append(": existing object's process dead");
                Log.i(str, stringBuilder.toString());
                handleUnstableProviderDiedLocked(jBinder, true);
                return null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x002d, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:43:0x008c, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:68:0x0122, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final boolean releaseProvider(IContentProvider provider, boolean stable) {
        int i = 0;
        if (provider == null) {
            return false;
        }
        IBinder jBinder = provider.asBinder();
        synchronized (this.mProviderMap) {
            ProviderRefCount prc = (ProviderRefCount) this.mProviderRefCountMap.get(jBinder);
            if (prc == null) {
                return false;
            }
            boolean lastRef = false;
            String str;
            StringBuilder stringBuilder;
            if (stable) {
                if (prc.stableCount != 0) {
                    prc.stableCount--;
                    if (prc.stableCount == 0) {
                        lastRef = prc.unstableCount == 0;
                        try {
                            if (DEBUG_PROVIDER) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("releaseProvider: No longer stable w/lastRef=");
                                stringBuilder.append(lastRef);
                                stringBuilder.append(" - ");
                                stringBuilder.append(prc.holder.info.name);
                                Slog.v(str, stringBuilder.toString());
                            }
                            IActivityManager service = ActivityManager.getService();
                            IBinder iBinder = prc.holder.connection;
                            if (lastRef) {
                                i = 1;
                            }
                            service.refContentProvider(iBinder, -1, i);
                        } catch (RemoteException e) {
                        }
                    }
                } else if (DEBUG_PROVIDER) {
                    Slog.v(TAG, "releaseProvider: stable ref count already 0, how?");
                }
            } else if (prc.unstableCount != 0) {
                prc.unstableCount--;
                if (prc.unstableCount == 0) {
                    lastRef = prc.stableCount == 0;
                    if (!lastRef) {
                        try {
                            if (DEBUG_PROVIDER) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("releaseProvider: No longer unstable - ");
                                stringBuilder.append(prc.holder.info.name);
                                Slog.v(str, stringBuilder.toString());
                            }
                            ActivityManager.getService().refContentProvider(prc.holder.connection, 0, -1);
                        } catch (RemoteException e2) {
                        }
                    }
                }
            } else if (DEBUG_PROVIDER) {
                Slog.v(TAG, "releaseProvider: unstable ref count already 0, how?");
            }
            if (lastRef) {
                String str2;
                StringBuilder stringBuilder2;
                if (prc.removePending) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Duplicate remove pending of provider ");
                    stringBuilder2.append(prc.holder.info.name);
                    Slog.w(str2, stringBuilder2.toString());
                } else {
                    if (DEBUG_PROVIDER) {
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("releaseProvider: Enqueueing pending removal - ");
                        stringBuilder2.append(prc.holder.info.name);
                        Slog.v(str2, stringBuilder2.toString());
                    }
                    prc.removePending = true;
                    this.mH.sendMessage(this.mH.obtainMessage(131, prc));
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0013, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:23:0x0055, code skipped:
            if (DEBUG_PROVIDER == false) goto L_0x0079;
     */
    /* JADX WARNING: Missing block: B:24:0x0057, code skipped:
            r0 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("removeProvider: Invoking ActivityManagerService.removeContentProvider(");
            r2.append(r9.holder.info.name);
            r2.append(")");
            android.util.Slog.v(r0, r2.toString());
     */
    /* JADX WARNING: Missing block: B:25:0x0079, code skipped:
            android.app.ActivityManager.getService().removeContentProvider(r9.holder.connection, false);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void completeRemoveProvider(ProviderRefCount prc) {
        synchronized (this.mProviderMap) {
            if (prc.removePending) {
                prc.removePending = false;
                IBinder jBinder = prc.holder.provider.asBinder();
                if (((ProviderRefCount) this.mProviderRefCountMap.get(jBinder)) == prc) {
                    this.mProviderRefCountMap.remove(jBinder);
                }
                for (int i = this.mProviderMap.size() - 1; i >= 0; i--) {
                    if (((ProviderClientRecord) this.mProviderMap.valueAt(i)).mProvider.asBinder() == jBinder) {
                        this.mProviderMap.removeAt(i);
                    }
                }
                prc.removePending = false;
            } else if (DEBUG_PROVIDER) {
                Slog.v(TAG, "completeRemoveProvider: lost the race, provider still in use");
            }
        }
    }

    final void handleUnstableProviderDied(IBinder provider, boolean fromClient) {
        synchronized (this.mProviderMap) {
            handleUnstableProviderDiedLocked(provider, fromClient);
        }
    }

    final void handleUnstableProviderDiedLocked(IBinder provider, boolean fromClient) {
        ProviderRefCount prc = (ProviderRefCount) this.mProviderRefCountMap.get(provider);
        if (prc != null) {
            if (DEBUG_PROVIDER) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cleaning up dead provider ");
                stringBuilder.append(provider);
                stringBuilder.append(" ");
                stringBuilder.append(prc.holder.info.name);
                Slog.v(str, stringBuilder.toString());
            }
            this.mProviderRefCountMap.remove(provider);
            for (int i = this.mProviderMap.size() - 1; i >= 0; i--) {
                ProviderClientRecord pr = (ProviderClientRecord) this.mProviderMap.valueAt(i);
                if (pr != null && pr.mProvider.asBinder() == provider) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Removing dead content provider:");
                    stringBuilder2.append(pr.mProvider.toString());
                    Slog.i(str2, stringBuilder2.toString());
                    this.mProviderMap.removeAt(i);
                }
            }
            if (fromClient) {
                try {
                    ActivityManager.getService().unstableProviderDied(prc.holder.connection);
                } catch (RemoteException e) {
                }
            }
        }
    }

    final void appNotRespondingViaProvider(IBinder provider) {
        synchronized (this.mProviderMap) {
            ProviderRefCount prc = (ProviderRefCount) this.mProviderRefCountMap.get(provider);
            if (prc != null) {
                try {
                    ActivityManager.getService().appNotRespondingViaProvider(prc.holder.connection);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private ProviderClientRecord installProviderAuthoritiesLocked(IContentProvider provider, ContentProvider localProvider, ContentProviderHolder holder) {
        String auth;
        String[] auths = holder.info.authority.split(";");
        int userId = UserHandle.getUserId(holder.info.applicationInfo.uid);
        int i = 0;
        if (provider != null) {
            for (String auth2 : auths) {
                int i2 = -1;
                switch (auth2.hashCode()) {
                    case -845193793:
                        if (auth2.equals("com.android.contacts")) {
                            i2 = 0;
                            break;
                        }
                        break;
                    case -456066902:
                        if (auth2.equals("com.android.calendar")) {
                            i2 = 4;
                            break;
                        }
                        break;
                    case -172298781:
                        if (auth2.equals("call_log")) {
                            i2 = 1;
                            break;
                        }
                        break;
                    case 63943420:
                        if (auth2.equals("call_log_shadow")) {
                            i2 = 2;
                            break;
                        }
                        break;
                    case 783201304:
                        if (auth2.equals("telephony")) {
                            i2 = 6;
                            break;
                        }
                        break;
                    case 1312704747:
                        if (auth2.equals("downloads")) {
                            i2 = 5;
                            break;
                        }
                        break;
                    case 1995645513:
                        if (auth2.equals("com.android.blockednumber")) {
                            i2 = 3;
                            break;
                        }
                        break;
                }
                switch (i2) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        Binder.allowBlocking(provider.asBinder());
                        break;
                    default:
                        break;
                }
            }
        }
        ProviderClientRecord pcr = new ProviderClientRecord(auths, provider, localProvider, holder);
        int length = auths.length;
        while (i < length) {
            auth2 = auths[i];
            ProviderKey key = new ProviderKey(auth2, userId);
            if (((ProviderClientRecord) this.mProviderMap.get(key)) != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Content provider ");
                stringBuilder.append(pcr.mHolder.info.name);
                stringBuilder.append(" already published as ");
                stringBuilder.append(auth2);
                Slog.w(str, stringBuilder.toString());
            } else {
                this.mProviderMap.put(key, pcr);
            }
            i++;
        }
        return pcr;
    }

    private ContentProviderHolder installProvider(Context context, ContentProviderHolder holder, ProviderInfo info, boolean noisy, boolean noReleaseNeeded, boolean stable) {
        String str;
        StringBuilder stringBuilder;
        ApplicationInfo ai;
        String str2;
        IContentProvider provider;
        ContentProviderHolder retHolder;
        Context context2 = context;
        ContentProviderHolder contentProviderHolder = holder;
        ProviderInfo providerInfo = info;
        boolean z = stable;
        IContentProvider provider2 = null;
        if (contentProviderHolder == null || contentProviderHolder.provider == null) {
            if (DEBUG_PROVIDER || noisy) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Loading provider ");
                stringBuilder.append(providerInfo.authority);
                stringBuilder.append(": ");
                stringBuilder.append(providerInfo.name);
                Slog.d(str, stringBuilder.toString());
            }
            Context c = null;
            ai = providerInfo.applicationInfo;
            if (context.getPackageName().equals(ai.packageName)) {
                c = context2;
            } else if (this.mInitialApplication == null || !this.mInitialApplication.getPackageName().equals(ai.packageName)) {
                try {
                    c = context2.createPackageContext(ai.packageName, 1);
                } catch (NameNotFoundException e) {
                    str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to get context for package ");
                    stringBuilder2.append(ai.packageName);
                    Slog.w(str2, stringBuilder2.toString());
                }
            } else {
                c = this.mInitialApplication;
            }
            c = getContextForClassLoader(ai, context2, c);
            if (c == null) {
                str = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unable to get context for package ");
                stringBuilder3.append(ai.packageName);
                stringBuilder3.append(" while loading content provider ");
                stringBuilder3.append(providerInfo.name);
                Slog.w(str, stringBuilder3.toString());
                return null;
            }
            if (providerInfo.splitName != null) {
                try {
                    c = c.createContextForSplit(providerInfo.splitName);
                } catch (NameNotFoundException e2) {
                    throw new RuntimeException(e2);
                }
            }
            try {
                ClassLoader cl = c.getClassLoader();
                LoadedApk packageInfo = peekPackageInfo(ai.packageName, true);
                if (packageInfo == null) {
                    packageInfo = getSystemContext().mPackageInfo;
                }
                Object provider22 = packageInfo.getAppFactory().instantiateProvider(cl, providerInfo.name);
                IContentProvider provider3 = provider22.getIContentProvider();
                StringBuilder stringBuilder4;
                if (provider3 == null) {
                    String str3 = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Failed to instantiate class ");
                    stringBuilder4.append(providerInfo.name);
                    stringBuilder4.append(" from sourceDir ");
                    stringBuilder4.append(providerInfo.applicationInfo.sourceDir);
                    Slog.e(str3, stringBuilder4.toString());
                    return null;
                }
                if (DEBUG_PROVIDER) {
                    str2 = TAG;
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Instantiating local provider ");
                    stringBuilder4.append(providerInfo.name);
                    Slog.v(str2, stringBuilder4.toString());
                }
                provider22.attachInfo(c, providerInfo);
                provider = provider3;
            } catch (Exception e3) {
                if (this.mInstrumentation.onException(null, e3)) {
                    return null;
                }
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("Unable to get provider ");
                stringBuilder5.append(providerInfo.name);
                stringBuilder5.append(": ");
                stringBuilder5.append(e3.toString());
                throw new RuntimeException(stringBuilder5.toString(), e3);
            }
        }
        provider = contentProviderHolder.provider;
        if (DEBUG_PROVIDER) {
            String str4 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Installing external provider ");
            stringBuilder.append(providerInfo.authority);
            stringBuilder.append(": ");
            stringBuilder.append(providerInfo.name);
            Slog.v(str4, stringBuilder.toString());
        }
        IContentProvider localProvider = provider22;
        provider22 = provider;
        if (providerInfo.applicationInfo.uid > 10000 && !context.getPackageName().equals(providerInfo.applicationInfo.packageName)) {
            Utils.handleTimeOut("acquire_provider", providerInfo.applicationInfo.packageName, "");
        }
        synchronized (this.mProviderMap) {
            if (DEBUG_PROVIDER) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Checking to add ");
                stringBuilder.append(provider22);
                stringBuilder.append(" / ");
                stringBuilder.append(providerInfo.name);
                Slog.v(str, stringBuilder.toString());
            }
            IBinder c2 = provider22.asBinder();
            if (localProvider != null) {
                ComponentName cname = new ComponentName(providerInfo.packageName, providerInfo.name);
                ProviderClientRecord pr = (ProviderClientRecord) this.mLocalProvidersByName.get(cname);
                if (pr != null) {
                    if (DEBUG_PROVIDER) {
                        Slog.v(TAG, "installProvider: lost the race, using existing local provider");
                    }
                    provider22 = pr.mProvider;
                } else {
                    contentProviderHolder = new ContentProviderHolder(providerInfo);
                    contentProviderHolder.provider = provider22;
                    contentProviderHolder.noReleaseNeeded = true;
                    pr = installProviderAuthoritiesLocked(provider22, localProvider, contentProviderHolder);
                    this.mLocalProviders.put(c2, pr);
                    this.mLocalProvidersByName.put(cname, pr);
                }
                retHolder = pr.mHolder;
            } else {
                ai = (ProviderRefCount) this.mProviderRefCountMap.get(c2);
                if (ai != null) {
                    if (DEBUG_PROVIDER) {
                        Slog.v(TAG, "installProvider: lost the race, updating ref count");
                    }
                    if (!noReleaseNeeded) {
                        incProviderRefLocked(ai, z);
                        try {
                            ActivityManager.getService().removeContentProvider(contentProviderHolder.connection, z);
                        } catch (RemoteException e4) {
                            Log.e(TAG, "installProvider()");
                        }
                    }
                } else {
                    ProviderClientRecord client = installProviderAuthoritiesLocked(provider22, localProvider, contentProviderHolder);
                    if (noReleaseNeeded) {
                        ai = new ProviderRefCount(contentProviderHolder, client, 1000, 1000);
                    } else {
                        ProviderRefCount providerRefCount;
                        if (z) {
                            providerRefCount = new ProviderRefCount(contentProviderHolder, client, 1, 0);
                        } else {
                            providerRefCount = new ProviderRefCount(contentProviderHolder, client, 0, 1);
                        }
                        ai = providerRefCount;
                    }
                    this.mProviderRefCountMap.put(c2, ai);
                }
                retHolder = ai.holder;
            }
        }
        return retHolder;
    }

    private void handleRunIsolatedEntryPoint(String entryPoint, String[] entryPointArgs) {
        try {
            Class.forName(entryPoint).getMethod("main", new Class[]{String[].class}).invoke(null, new Object[]{entryPointArgs});
            System.exit(0);
        } catch (ReflectiveOperationException e) {
            throw new AndroidRuntimeException("runIsolatedEntryPoint failed", e);
        }
    }

    private void attach(boolean system, long startSeq) {
        sCurrentActivityThread = this;
        this.mSystemThread = system;
        if (system) {
            DdmHandleAppName.setAppName("system_process", UserHandle.myUserId());
            try {
                this.mInstrumentation = new Instrumentation();
                this.mInstrumentation.basicInit(this);
                this.mInitialApplication = ContextImpl.createAppContext(this, getSystemContext().mPackageInfo).mPackageInfo.makeApplication(true, null);
                this.mInitialApplication.onCreate();
                HwThemeManager.setThemeFont();
                if (getApplicationThread() != null) {
                    getApplicationThread().scheduleTrimMemory(80);
                }
                Typeface.loadSystemFonts();
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to instantiate Application():");
                stringBuilder.append(e.toString());
                throw new RuntimeException(stringBuilder.toString(), e);
            }
        }
        ViewRootImpl.addFirstDrawHandler(new Runnable() {
            public void run() {
                ActivityThread.this.ensureJitEnabled();
            }
        });
        DdmHandleAppName.setAppName("<pre-initialized>", UserHandle.myUserId());
        RuntimeInit.setApplicationObject(this.mAppThread.asBinder());
        final IActivityManager mgr = ActivityManager.getService();
        try {
            Slog.d(TAG, "Attach thread to application");
            if (Jlog.isPerfTest()) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("pid=");
                stringBuilder2.append(Process.myPid());
                Jlog.i(3033, Jlog.getMessage(TAG, "attach", stringBuilder2.toString()));
            }
            mgr.attachApplication(this.mAppThread, startSeq);
            BinderInternal.addGcWatcher(new Runnable() {
                public void run() {
                    if (ActivityThread.this.mSomeActivitiesChanged) {
                        Runtime runtime = Runtime.getRuntime();
                        if (runtime.totalMemory() - runtime.freeMemory() > (3 * runtime.maxMemory()) / 4) {
                            ActivityThread.this.mSomeActivitiesChanged = false;
                            try {
                                mgr.releaseSomeActivities(ActivityThread.this.mAppThread);
                            } catch (RemoteException e) {
                                throw e.rethrowFromSystemServer();
                            }
                        }
                    }
                }
            });
        } catch (RemoteException ex) {
            throw ex.rethrowFromSystemServer();
        }
        DropBox.setReporter(new DropBoxReporter());
        ViewRootImpl.addConfigCallback(new -$$Lambda$ActivityThread$ZXDWm3IBeFmLnFVblhB-IOZCr9o(this));
    }

    public static /* synthetic */ void lambda$attach$0(ActivityThread activityThread, Configuration globalConfig) {
        synchronized (activityThread.mResourcesManager) {
            globalConfig = activityThread.updateConfig(globalConfig);
            if (activityThread.mResourcesManager.applyConfigurationToResourcesLocked(globalConfig, null)) {
                activityThread.updateLocaleListFromAppContext(activityThread.mInitialApplication.getApplicationContext(), activityThread.mResourcesManager.getConfiguration().getLocales());
                if (activityThread.mPendingConfiguration == null || activityThread.mPendingConfiguration.isOtherSeqNewer(globalConfig)) {
                    activityThread.mPendingConfiguration = new Configuration(globalConfig);
                    activityThread.sendMessage(118, globalConfig);
                }
            }
        }
    }

    public static ActivityThread systemMain() {
        if (ActivityManager.isHighEndGfx()) {
            ThreadedRenderer.enableForegroundTrimming();
        } else {
            ThreadedRenderer.disable(true);
        }
        ActivityThread thread = new ActivityThread();
        thread.attach(true, 0);
        return thread;
    }

    public final void installSystemProviders(List<ProviderInfo> providers) {
        if (providers != null) {
            installContentProviders(this.mInitialApplication, providers);
        }
    }

    public int getIntCoreSetting(String key, int defaultValue) {
        synchronized (this.mResourcesManager) {
            if (this.mCoreSettings != null) {
                int i = this.mCoreSettings.getInt(key, defaultValue);
                return i;
            }
            return defaultValue;
        }
    }

    public static void main(String[] args) {
        Trace.traceBegin(64, "ActivityThreadMain");
        if (Jlog.isMicroTest()) {
            Jlog.i(3088, Jlog.getMessage(TAG, "main", Integer.valueOf(Process.myPid())));
        }
        CloseGuard.setEnabled(false);
        Environment.initForCurrentUser();
        Log.initHWLog();
        EventLogger.setReporter(new EventLoggingReporter());
        TrustedCertificateStore.setDefaultUserDirectory(Environment.getUserConfigDirectory(UserHandle.myUserId()));
        Process.setArgV0("<pre-initialized>");
        Looper.prepareMainLooper();
        long startSeq = 0;
        ArrayList<String> displayArgs = new ArrayList();
        if (args != null) {
            int i = args.length - 1;
            while (i >= 0) {
                if (args[i] != null && args[i].startsWith(PROC_START_SEQ_IDENT)) {
                    startSeq = Long.parseLong(args[i].substring(PROC_START_SEQ_IDENT.length()));
                } else if (args[i] != null) {
                    displayArgs.add(0, args[i]);
                }
                i--;
            }
        }
        ActivityThread thread = new ActivityThread();
        if (!initVRArgs(thread, (String[]) displayArgs.toArray(new String[displayArgs.size()]))) {
            initPCArgs(thread, (String[]) displayArgs.toArray(new String[displayArgs.size()]));
        }
        thread.attach(false, startSeq);
        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();
        }
        if (Jlog.isMicroTest()) {
            Jlog.i(3089, Jlog.getMessage(TAG, "main", Integer.valueOf(Process.myPid())));
        }
        Trace.traceEnd(64);
        Looper.loop();
        throw new RuntimeException("Main thread loop unexpectedly exited");
    }

    private Context getContextForClassLoader(ApplicationInfo ai, Context context, Context defContext) {
        try {
            ApplicationInfo applicationInfo;
            String packageName = ai.packageName;
            String sourceDir = ai.sourceDir;
            String publicSourceDir = ai.publicSourceDir;
            if (USE_CACHE) {
                applicationInfo = getSystemContext().getPackageManager().getApplicationInfoAsUser(packageName, 0, UserHandle.getUserId(ai.uid));
            } else {
                applicationInfo = getPackageManager().getApplicationInfo(packageName, 0, UserHandle.getUserId(ai.uid));
            }
            String str;
            StringBuilder stringBuilder;
            if (applicationInfo == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(packageName);
                stringBuilder.append(" is uninstalled, can't install provider");
                Slog.w(str, stringBuilder.toString());
                return null;
            }
            if (!(applicationInfo.sourceDir.equals(sourceDir) && applicationInfo.publicSourceDir.equals(publicSourceDir))) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(packageName);
                stringBuilder.append(" is replaced, sourceDir is changed from ");
                stringBuilder.append(sourceDir);
                stringBuilder.append(" to ");
                stringBuilder.append(applicationInfo.sourceDir);
                stringBuilder.append(", publicSourceDir is changed from ");
                stringBuilder.append(publicSourceDir);
                stringBuilder.append(" to ");
                stringBuilder.append(applicationInfo.publicSourceDir);
                Slog.w(str, stringBuilder.toString());
                return context.createPackageContext(ai.packageName, 1);
            }
            return defContext;
        } catch (NameNotFoundException | RemoteException e) {
        }
    }

    public void handlePCWindowStateChanged(IBinder token, int windowState) {
        ActivityClientRecord r = (ActivityClientRecord) this.mActivities.get(token);
        if (r != null && r.window != null && (r.window instanceof AbsWindow)) {
            ((AbsWindow) r.window).onWindowStateChanged(windowState);
        }
    }

    private void setInitParam(int displayId, int width, int height) {
        this.mDisplayId = displayId;
        HwPCUtils.setPCDisplayID(displayId);
        if (width > 0 && height > 0) {
            Display display = this.mResourcesManager.getAdjustedDisplay(displayId, Resources.getSystem());
            if (display != null) {
                DisplayMetrics dm = new DisplayMetrics();
                display.getMetrics(dm);
                if (this.mOverrideConfig == null) {
                    this.mOverrideConfig = new Configuration();
                }
                dm.widthPixels = width;
                dm.heightPixels = height;
                this.mOverrideConfig.touchscreen = 1;
                this.mOverrideConfig.densityDpi = dm.densityDpi;
                Configuration configuration = this.mOverrideConfig;
                int i = (int) (((float) dm.widthPixels) / dm.density);
                this.mOverrideConfig.screenWidthDp = i;
                configuration.compatScreenWidthDp = i;
                configuration = this.mOverrideConfig;
                i = (int) (((float) dm.heightPixels) / dm.density);
                this.mOverrideConfig.screenHeightDp = i;
                configuration.compatScreenHeightDp = i;
                configuration = this.mOverrideConfig;
                Configuration configuration2 = this.mOverrideConfig;
                i = this.mOverrideConfig.screenWidthDp > this.mOverrideConfig.screenHeightDp ? this.mOverrideConfig.screenHeightDp : this.mOverrideConfig.screenWidthDp;
                configuration2.smallestScreenWidthDp = i;
                configuration.compatSmallestScreenWidthDp = i;
                int sl = Configuration.resetScreenLayout(this.mOverrideConfig.screenLayout);
                if (dm.widthPixels > dm.heightPixels) {
                    this.mOverrideConfig.orientation = 2;
                    this.mOverrideConfig.screenLayout = Configuration.reduceScreenLayout(sl, this.mOverrideConfig.screenWidthDp, this.mOverrideConfig.screenHeightDp);
                } else {
                    this.mOverrideConfig.orientation = 1;
                    this.mOverrideConfig.screenLayout = Configuration.reduceScreenLayout(sl, this.mOverrideConfig.screenHeightDp, this.mOverrideConfig.screenWidthDp);
                }
            }
        }
    }

    public Configuration getOverrideConfig() {
        return this.mOverrideConfig;
    }

    public void updateOverrideConfig(Configuration config) {
        if (config != null && HwPCUtils.isValidExtDisplayId(this.mDisplayId)) {
            if (this.mOverrideConfig != null && !this.mOverrideConfig.equals(config)) {
                this.mOverrideConfig.setTo(config);
                if (this.mOverrideConfig.windowConfiguration.getAppBounds() != null) {
                    this.mOverrideConfig.windowConfiguration.getAppBounds().offsetTo(0, 0);
                }
            } else if (this.mOverrideConfig == null) {
                this.mOverrideConfig = new Configuration(config);
                if (this.mOverrideConfig.windowConfiguration.getAppBounds() != null) {
                    this.mOverrideConfig.windowConfiguration.getAppBounds().offsetTo(0, 0);
                }
            }
        }
    }

    public int getDisplayId() {
        return this.mDisplayId;
    }

    private Configuration updateConfig(Configuration config) {
        if (!(config == null || !HwPCUtils.isValidExtDisplayId(this.mDisplayId) || this.mOverrideConfig == null || this.mOverrideConfig.equals(Configuration.EMPTY))) {
            config = new Configuration(config);
            config.updateFrom(this.mOverrideConfig);
            if (!(this.mConfiguration == null || new Configuration(this.mConfiguration).updateFrom(config) == 0)) {
                config.seq = 0;
            }
        }
        return config;
    }

    private static void initPCArgs(ActivityThread thread, String[] args) {
        if (HwPCUtils.enabled() && args != null) {
            if (args.length == 3 || args.length == 1) {
                try {
                    if (Integer.parseInt(args[0]) > 0) {
                        if (args.length == 1) {
                            thread.setInitParam(Integer.parseInt(args[0]), 0, 0);
                        } else {
                            thread.setInitParam(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
                        }
                        HwPCUtils.setPCDisplayID(Integer.parseInt(args[0]));
                        return;
                    }
                    HwPCUtils.setPCDisplayID(-Integer.parseInt(args[0]));
                } catch (NumberFormatException e) {
                    HwPCUtils.log(TAG, "args format error.");
                }
            }
        }
    }

    private static boolean initVRArgs(ActivityThread thread, String[] args) {
        if (!(thread == null || args == null || args.length != 3)) {
            try {
                int displayId = Integer.parseInt(args[0]);
                if (HwVRUtils.isVRDisplay(displayId, Integer.parseInt(args[1]), Integer.parseInt(args[2]))) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("initVRArgs displayid = ");
                    stringBuilder.append(displayId);
                    HwVRUtils.log(str, stringBuilder.toString());
                    thread.setVRInitParam(displayId);
                    return true;
                }
            } catch (NumberFormatException e) {
                HwVRUtils.log(TAG, "args format error.");
            }
        }
        return false;
    }

    private void setVRInitParam(int displayId) {
        this.mDisplayId = displayId;
        HwVRUtils.setVRDisplayID(displayId, true);
    }
}
