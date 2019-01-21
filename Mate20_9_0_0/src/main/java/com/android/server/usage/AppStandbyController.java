package com.android.server.usage;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.usage.AppStandbyInfo;
import android.app.usage.UsageEvents.Event;
import android.app.usage.UsageStatsManagerInternal.AppIdleStateChangeListener;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.net.NetworkScoreManager;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.IDeviceIdleController.Stub;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.job.JobPackageTracker;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.File;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class AppStandbyController {
    static final boolean COMPRESS_TIME = false;
    static final boolean DEBUG = false;
    private static final long DEFAULT_PREDICTION_TIMEOUT = 43200000;
    static final long[] ELAPSED_TIME_THRESHOLDS = new long[]{0, 43200000, 86400000, 172800000};
    static final int MSG_CHECK_IDLE_STATES = 5;
    static final int MSG_CHECK_PACKAGE_IDLE_STATE = 11;
    static final int MSG_CHECK_PAROLE_TIMEOUT = 6;
    static final int MSG_FORCE_IDLE_STATE = 4;
    static final int MSG_INFORM_LISTENERS = 3;
    static final int MSG_ONE_TIME_CHECK_IDLE_STATES = 10;
    static final int MSG_PAROLE_END_TIMEOUT = 7;
    static final int MSG_PAROLE_STATE_CHANGED = 9;
    static final int MSG_REPORT_CONTENT_PROVIDER_USAGE = 8;
    static final int MSG_REPORT_EXEMPTED_SYNC_SCHEDULED = 12;
    static final int MSG_REPORT_EXEMPTED_SYNC_START = 13;
    static final int MSG_UPDATE_STABLE_CHARGING = 14;
    private static final long ONE_DAY = 86400000;
    private static final long ONE_HOUR = 3600000;
    private static final long ONE_MINUTE = 60000;
    static final long[] SCREEN_TIME_THRESHOLDS = new long[]{0, 0, 3600000, SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT};
    private static final String TAG = "AppStandbyController";
    static final int[] THRESHOLD_BUCKETS = new int[]{10, 20, 30, 40};
    private static final long WAIT_FOR_ADMIN_DATA_TIMEOUT_MS = 10000;
    static final ArrayList<StandbyUpdateRecord> sStandbyUpdatePool = new ArrayList(4);
    @GuardedBy("mActiveAdminApps")
    private final SparseArray<Set<String>> mActiveAdminApps;
    private final CountDownLatch mAdminDataAvailableLatch;
    volatile boolean mAppIdleEnabled;
    @GuardedBy("mAppIdleLock")
    private AppIdleHistory mAppIdleHistory;
    private final Object mAppIdleLock;
    long mAppIdleParoleDurationMillis;
    long mAppIdleParoleIntervalMillis;
    long mAppIdleParoleWindowMillis;
    boolean mAppIdleTempParoled;
    long[] mAppStandbyElapsedThresholds;
    long[] mAppStandbyScreenThresholds;
    private AppWidgetManager mAppWidgetManager;
    @GuardedBy("mAppIdleLock")
    private List<String> mCarrierPrivilegedApps;
    boolean mCharging;
    boolean mChargingStable;
    long mCheckIdleIntervalMillis;
    private ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final DeviceStateReceiver mDeviceStateReceiver;
    private final DisplayListener mDisplayListener;
    long mExemptedSyncScheduledDozeTimeoutMillis;
    long mExemptedSyncScheduledNonDozeTimeoutMillis;
    long mExemptedSyncStartTimeoutMillis;
    private final AppStandbyHandler mHandler;
    @GuardedBy("mAppIdleLock")
    private boolean mHaveCarrierPrivilegedApps;
    Injector mInjector;
    private long mLastAppIdleParoledTime;
    private final NetworkCallback mNetworkCallback;
    private final NetworkRequest mNetworkRequest;
    long mNotificationSeenTimeoutMillis;
    @GuardedBy("mPackageAccessListeners")
    private ArrayList<AppIdleStateChangeListener> mPackageAccessListeners;
    private PackageManager mPackageManager;
    private boolean mPendingInitializeDefaults;
    private volatile boolean mPendingOneTimeCheckIdleStates;
    private PowerManager mPowerManager;
    long mPredictionTimeoutMillis;
    long mStableChargingThresholdMillis;
    long mStrongUsageTimeoutMillis;
    long mSyncAdapterTimeoutMillis;
    long mSystemInteractionTimeoutMillis;
    private boolean mSystemServicesReady;
    long mSystemUpdateUsageTimeoutMillis;

    class AppStandbyHandler extends Handler {
        AppStandbyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 3:
                    StandbyUpdateRecord r = msg.obj;
                    AppStandbyController.this.informListeners(r.packageName, r.userId, r.bucket, r.reason, r.isUserInteraction);
                    r.recycle();
                    return;
                case 4:
                    AppStandbyController appStandbyController = AppStandbyController.this;
                    String str = (String) msg.obj;
                    int i = msg.arg1;
                    if (msg.arg2 == 1) {
                        z = true;
                    }
                    appStandbyController.forceIdleState(str, i, z);
                    return;
                case 5:
                    if (AppStandbyController.this.checkIdleStates(msg.arg1) && AppStandbyController.this.mAppIdleEnabled) {
                        AppStandbyController.this.mHandler.sendMessageDelayed(AppStandbyController.this.mHandler.obtainMessage(5, msg.arg1, 0), AppStandbyController.this.mCheckIdleIntervalMillis);
                        return;
                    }
                    return;
                case 6:
                    AppStandbyController.this.checkParoleTimeout();
                    return;
                case 7:
                    AppStandbyController.this.setAppIdleParoled(false);
                    return;
                case 8:
                    SomeArgs args = msg.obj;
                    AppStandbyController.this.reportContentProviderUsage((String) args.arg1, (String) args.arg2, ((Integer) args.arg3).intValue());
                    args.recycle();
                    return;
                case 9:
                    AppStandbyController.this.informParoleStateChanged();
                    return;
                case 10:
                    AppStandbyController.this.mHandler.removeMessages(10);
                    AppStandbyController.this.waitForAdminData();
                    AppStandbyController.this.checkIdleStates(-1);
                    return;
                case 11:
                    AppStandbyController.this.checkAndUpdateStandbyState((String) msg.obj, msg.arg1, msg.arg2, AppStandbyController.this.mInjector.elapsedRealtime());
                    return;
                case 12:
                    AppStandbyController.this.reportExemptedSyncScheduled((String) msg.obj, msg.arg1);
                    return;
                case 13:
                    AppStandbyController.this.reportExemptedSyncStart((String) msg.obj, msg.arg1);
                    return;
                case 14:
                    AppStandbyController.this.updateChargingStableState();
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        }
    }

    private class DeviceStateReceiver extends BroadcastReceiver {
        private DeviceStateReceiver() {
        }

        /* synthetic */ DeviceStateReceiver(AppStandbyController x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Removed duplicated region for block: B:23:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0049  */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x0043  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003d  */
        /* JADX WARNING: Removed duplicated region for block: B:23:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0049  */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x0043  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003d  */
        /* JADX WARNING: Removed duplicated region for block: B:23:? A:{SYNTHETIC, RETURN} */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0049  */
        /* JADX WARNING: Removed duplicated region for block: B:18:0x0043  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003d  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            boolean z;
            String action = intent.getAction();
            int hashCode = action.hashCode();
            if (hashCode == -54942926) {
                if (action.equals("android.os.action.DISCHARGING")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 870701415) {
                if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                }
            } else if (hashCode == 948344062 && action.equals("android.os.action.CHARGING")) {
                z = false;
                switch (z) {
                    case false:
                        AppStandbyController.this.setChargingState(true);
                        return;
                    case true:
                        AppStandbyController.this.setChargingState(false);
                        return;
                    case true:
                        AppStandbyController.this.onDeviceIdleModeChanged();
                        return;
                    default:
                        return;
                }
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
        }
    }

    static class Injector {
        private IBatteryStats mBatteryStats;
        int mBootPhase;
        private final Context mContext;
        private IDeviceIdleController mDeviceIdleController;
        private DisplayManager mDisplayManager;
        private final Looper mLooper;
        private PackageManagerInternal mPackageManagerInternal;
        private PowerManager mPowerManager;

        Injector(Context context, Looper looper) {
            this.mContext = context;
            this.mLooper = looper;
        }

        Context getContext() {
            return this.mContext;
        }

        Looper getLooper() {
            return this.mLooper;
        }

        void onBootPhase(int phase) {
            if (phase == 500) {
                this.mDeviceIdleController = Stub.asInterface(ServiceManager.getService("deviceidle"));
                this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                this.mDisplayManager = (DisplayManager) this.mContext.getSystemService("display");
                this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            }
            this.mBootPhase = phase;
        }

        int getBootPhase() {
            return this.mBootPhase;
        }

        long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        boolean isAppIdleEnabled() {
            boolean buildFlag = this.mContext.getResources().getBoolean(17956949);
            boolean runtimeFlag = Global.getInt(this.mContext.getContentResolver(), "app_standby_enabled", 1) == 1 && Global.getInt(this.mContext.getContentResolver(), "adaptive_battery_management_enabled", 1) == 1;
            if (buildFlag && runtimeFlag) {
                return true;
            }
            return false;
        }

        boolean isCharging() {
            return ((BatteryManager) this.mContext.getSystemService(BatteryManager.class)).isCharging();
        }

        boolean isPowerSaveWhitelistExceptIdleApp(String packageName) throws RemoteException {
            return this.mDeviceIdleController.isPowerSaveWhitelistExceptIdleApp(packageName);
        }

        File getDataSystemDirectory() {
            return Environment.getDataSystemDirectory();
        }

        void noteEvent(int event, String packageName, int uid) throws RemoteException {
            this.mBatteryStats.noteEvent(event, packageName, uid);
        }

        boolean isPackageEphemeral(int userId, String packageName) {
            return this.mPackageManagerInternal.isPackageEphemeral(userId, packageName);
        }

        int[] getRunningUserIds() throws RemoteException {
            return ActivityManager.getService().getRunningUserIds();
        }

        boolean isDefaultDisplayOn() {
            return this.mDisplayManager.getDisplay(0).getState() == 2;
        }

        void registerDisplayListener(DisplayListener listener, Handler handler) {
            this.mDisplayManager.registerDisplayListener(listener, handler);
        }

        String getActiveNetworkScorer() {
            return ((NetworkScoreManager) this.mContext.getSystemService("network_score")).getActiveScorerPackage();
        }

        public boolean isBoundWidgetPackage(AppWidgetManager appWidgetManager, String packageName, int userId) {
            return appWidgetManager.isBoundWidgetPackage(packageName, userId);
        }

        String getAppIdleSettings() {
            return Global.getString(this.mContext.getContentResolver(), "app_idle_constants");
        }

        public boolean isDeviceIdleMode() {
            return this.mPowerManager.isDeviceIdleMode();
        }
    }

    static class Lock {
        Lock() {
        }
    }

    private class PackageReceiver extends BroadcastReceiver {
        private PackageReceiver() {
        }

        /* synthetic */ PackageReceiver(AppStandbyController x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.PACKAGE_ADDED".equals(action) || "android.intent.action.PACKAGE_CHANGED".equals(action)) {
                AppStandbyController.this.clearCarrierPrivilegedApps();
            }
            if (("android.intent.action.PACKAGE_REMOVED".equals(action) || "android.intent.action.PACKAGE_ADDED".equals(action)) && !intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                AppStandbyController.this.clearAppIdleForPackage(intent.getData().getSchemeSpecificPart(), getSendingUserId());
            }
        }
    }

    private class SettingsObserver extends ContentObserver {
        public static final long DEFAULT_EXEMPTED_SYNC_SCHEDULED_DOZE_TIMEOUT = 14400000;
        public static final long DEFAULT_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_TIMEOUT = 600000;
        public static final long DEFAULT_EXEMPTED_SYNC_START_TIMEOUT = 600000;
        public static final long DEFAULT_NOTIFICATION_TIMEOUT = 43200000;
        public static final long DEFAULT_STABLE_CHARGING_THRESHOLD = 600000;
        public static final long DEFAULT_STRONG_USAGE_TIMEOUT = 3600000;
        public static final long DEFAULT_SYNC_ADAPTER_TIMEOUT = 600000;
        public static final long DEFAULT_SYSTEM_INTERACTION_TIMEOUT = 600000;
        public static final long DEFAULT_SYSTEM_UPDATE_TIMEOUT = 7200000;
        private static final String KEY_ELAPSED_TIME_THRESHOLDS = "elapsed_thresholds";
        private static final String KEY_EXEMPTED_SYNC_SCHEDULED_DOZE_HOLD_DURATION = "exempted_sync_scheduled_d_duration";
        private static final String KEY_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_HOLD_DURATION = "exempted_sync_scheduled_nd_duration";
        private static final String KEY_EXEMPTED_SYNC_START_HOLD_DURATION = "exempted_sync_start_duration";
        @Deprecated
        private static final String KEY_IDLE_DURATION = "idle_duration2";
        @Deprecated
        private static final String KEY_IDLE_DURATION_OLD = "idle_duration";
        private static final String KEY_NOTIFICATION_SEEN_HOLD_DURATION = "notification_seen_duration";
        private static final String KEY_PAROLE_DURATION = "parole_duration";
        private static final String KEY_PAROLE_INTERVAL = "parole_interval";
        private static final String KEY_PAROLE_WINDOW = "parole_window";
        private static final String KEY_PREDICTION_TIMEOUT = "prediction_timeout";
        private static final String KEY_SCREEN_TIME_THRESHOLDS = "screen_thresholds";
        private static final String KEY_STABLE_CHARGING_THRESHOLD = "stable_charging_threshold";
        private static final String KEY_STRONG_USAGE_HOLD_DURATION = "strong_usage_duration";
        private static final String KEY_SYNC_ADAPTER_HOLD_DURATION = "sync_adapter_duration";
        private static final String KEY_SYSTEM_INTERACTION_HOLD_DURATION = "system_interaction_duration";
        private static final String KEY_SYSTEM_UPDATE_HOLD_DURATION = "system_update_usage_duration";
        @Deprecated
        private static final String KEY_WALLCLOCK_THRESHOLD = "wallclock_threshold";
        private final KeyValueListParser mParser = new KeyValueListParser(',');

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void registerObserver() {
            ContentResolver cr = AppStandbyController.this.mContext.getContentResolver();
            cr.registerContentObserver(Global.getUriFor("app_idle_constants"), false, this);
            cr.registerContentObserver(Global.getUriFor("app_standby_enabled"), false, this);
            cr.registerContentObserver(Global.getUriFor("adaptive_battery_management_enabled"), false, this);
        }

        public void onChange(boolean selfChange) {
            updateSettings();
            AppStandbyController.this.postOneTimeCheckIdleStates();
        }

        void updateSettings() {
            String str;
            AppStandbyController.this.setAppIdleEnabled(AppStandbyController.this.mInjector.isAppIdleEnabled());
            try {
                this.mParser.setString(AppStandbyController.this.mInjector.getAppIdleSettings());
            } catch (IllegalArgumentException e) {
                str = AppStandbyController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bad value for app idle settings: ");
                stringBuilder.append(e.getMessage());
                Slog.e(str, stringBuilder.toString());
            }
            synchronized (AppStandbyController.this.mAppIdleLock) {
                AppStandbyController.this.mAppIdleParoleIntervalMillis = this.mParser.getDurationMillis(KEY_PAROLE_INTERVAL, 86400000);
                AppStandbyController.this.mAppIdleParoleWindowMillis = this.mParser.getDurationMillis(KEY_PAROLE_WINDOW, DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                AppStandbyController.this.mAppIdleParoleDurationMillis = this.mParser.getDurationMillis(KEY_PAROLE_DURATION, 600000);
                str = this.mParser.getString(KEY_SCREEN_TIME_THRESHOLDS, null);
                AppStandbyController.this.mAppStandbyScreenThresholds = parseLongArray(str, AppStandbyController.SCREEN_TIME_THRESHOLDS);
                String elapsedThresholdsValue = this.mParser.getString(KEY_ELAPSED_TIME_THRESHOLDS, null);
                AppStandbyController.this.mAppStandbyElapsedThresholds = parseLongArray(elapsedThresholdsValue, AppStandbyController.ELAPSED_TIME_THRESHOLDS);
                AppStandbyController.this.mCheckIdleIntervalMillis = Math.min(AppStandbyController.this.mAppStandbyElapsedThresholds[1] / 4, 14400000);
                AppStandbyController.this.mStrongUsageTimeoutMillis = this.mParser.getDurationMillis(KEY_STRONG_USAGE_HOLD_DURATION, 3600000);
                AppStandbyController.this.mNotificationSeenTimeoutMillis = this.mParser.getDurationMillis(KEY_NOTIFICATION_SEEN_HOLD_DURATION, 43200000);
                AppStandbyController.this.mSystemUpdateUsageTimeoutMillis = this.mParser.getDurationMillis(KEY_SYSTEM_UPDATE_HOLD_DURATION, DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                AppStandbyController.this.mPredictionTimeoutMillis = this.mParser.getDurationMillis(KEY_PREDICTION_TIMEOUT, 43200000);
                AppStandbyController.this.mSyncAdapterTimeoutMillis = this.mParser.getDurationMillis(KEY_SYNC_ADAPTER_HOLD_DURATION, 600000);
                AppStandbyController.this.mExemptedSyncScheduledNonDozeTimeoutMillis = this.mParser.getDurationMillis(KEY_EXEMPTED_SYNC_SCHEDULED_NON_DOZE_HOLD_DURATION, 600000);
                AppStandbyController.this.mExemptedSyncScheduledDozeTimeoutMillis = this.mParser.getDurationMillis(KEY_EXEMPTED_SYNC_SCHEDULED_DOZE_HOLD_DURATION, 14400000);
                AppStandbyController.this.mExemptedSyncStartTimeoutMillis = this.mParser.getDurationMillis(KEY_EXEMPTED_SYNC_START_HOLD_DURATION, 600000);
                AppStandbyController.this.mSystemInteractionTimeoutMillis = this.mParser.getDurationMillis(KEY_SYSTEM_INTERACTION_HOLD_DURATION, 600000);
                AppStandbyController.this.mStableChargingThresholdMillis = this.mParser.getDurationMillis(KEY_STABLE_CHARGING_THRESHOLD, 600000);
            }
        }

        long[] parseLongArray(String values, long[] defaults) {
            if (values == null || values.isEmpty()) {
                return defaults;
            }
            String[] thresholds = values.split(SliceAuthority.DELIMITER);
            if (thresholds.length != AppStandbyController.THRESHOLD_BUCKETS.length) {
                return defaults;
            }
            long[] array = new long[AppStandbyController.THRESHOLD_BUCKETS.length];
            int i = 0;
            while (i < AppStandbyController.THRESHOLD_BUCKETS.length) {
                try {
                    if (!thresholds[i].startsWith("P")) {
                        if (!thresholds[i].startsWith("p")) {
                            array[i] = Long.parseLong(thresholds[i]);
                            i++;
                        }
                    }
                    array[i] = Duration.parse(thresholds[i]).toMillis();
                    i++;
                } catch (NumberFormatException | DateTimeParseException e) {
                    return defaults;
                }
            }
            return array;
        }
    }

    public static class StandbyUpdateRecord {
        int bucket;
        boolean isUserInteraction;
        String packageName;
        int reason;
        int userId;

        StandbyUpdateRecord(String pkgName, int userId, int bucket, int reason, boolean isInteraction) {
            this.packageName = pkgName;
            this.userId = userId;
            this.bucket = bucket;
            this.reason = reason;
            this.isUserInteraction = isInteraction;
        }

        public static StandbyUpdateRecord obtain(String pkgName, int userId, int bucket, int reason, boolean isInteraction) {
            synchronized (AppStandbyController.sStandbyUpdatePool) {
                int size = AppStandbyController.sStandbyUpdatePool.size();
                if (size < 1) {
                    StandbyUpdateRecord standbyUpdateRecord = new StandbyUpdateRecord(pkgName, userId, bucket, reason, isInteraction);
                    return standbyUpdateRecord;
                }
                StandbyUpdateRecord r = (StandbyUpdateRecord) AppStandbyController.sStandbyUpdatePool.remove(size - 1);
                r.packageName = pkgName;
                r.userId = userId;
                r.bucket = bucket;
                r.reason = reason;
                r.isUserInteraction = isInteraction;
                return r;
            }
        }

        public void recycle() {
            synchronized (AppStandbyController.sStandbyUpdatePool) {
                AppStandbyController.sStandbyUpdatePool.add(this);
            }
        }
    }

    AppStandbyController(Context context, Looper looper) {
        this(new Injector(context, looper));
    }

    AppStandbyController(Injector injector) {
        this.mAppIdleLock = new Lock();
        this.mPackageAccessListeners = new ArrayList();
        this.mActiveAdminApps = new SparseArray();
        this.mAdminDataAvailableLatch = new CountDownLatch(1);
        this.mAppStandbyScreenThresholds = SCREEN_TIME_THRESHOLDS;
        this.mAppStandbyElapsedThresholds = ELAPSED_TIME_THRESHOLDS;
        this.mSystemServicesReady = false;
        this.mNetworkRequest = new Builder().build();
        this.mNetworkCallback = new NetworkCallback() {
            public void onAvailable(Network network) {
                AppStandbyController.this.mConnectivityManager.unregisterNetworkCallback(this);
                AppStandbyController.this.checkParoleTimeout();
            }
        };
        this.mDisplayListener = new DisplayListener() {
            public void onDisplayAdded(int displayId) {
            }

            public void onDisplayRemoved(int displayId) {
            }

            public void onDisplayChanged(int displayId) {
                if (displayId == 0) {
                    boolean displayOn = AppStandbyController.this.isDisplayOn();
                    synchronized (AppStandbyController.this.mAppIdleLock) {
                        AppStandbyController.this.mAppIdleHistory.updateDisplay(displayOn, AppStandbyController.this.mInjector.elapsedRealtime());
                    }
                }
            }
        };
        this.mInjector = injector;
        this.mContext = this.mInjector.getContext();
        this.mHandler = new AppStandbyHandler(this.mInjector.getLooper());
        this.mPackageManager = this.mContext.getPackageManager();
        this.mDeviceStateReceiver = new DeviceStateReceiver(this, null);
        IntentFilter deviceStates = new IntentFilter("android.os.action.CHARGING");
        deviceStates.addAction("android.os.action.DISCHARGING");
        deviceStates.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(this.mDeviceStateReceiver, deviceStates);
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory = new AppIdleHistory(this.mInjector.getDataSystemDirectory(), this.mInjector.elapsedRealtime());
        }
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(new PackageReceiver(this, null), UserHandle.ALL, packageFilter, null, this.mHandler);
    }

    void setAppIdleEnabled(boolean enabled) {
        this.mAppIdleEnabled = enabled;
    }

    public void onBootPhase(int phase) {
        this.mInjector.onBootPhase(phase);
        if (phase == 500) {
            Slog.d(TAG, "Setting app idle enabled state");
            setAppIdleEnabled(this.mInjector.isAppIdleEnabled());
            SettingsObserver settingsObserver = new SettingsObserver(this.mHandler);
            settingsObserver.registerObserver();
            settingsObserver.updateSettings();
            this.mAppWidgetManager = (AppWidgetManager) this.mContext.getSystemService(AppWidgetManager.class);
            this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
            this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
            this.mInjector.registerDisplayListener(this.mDisplayListener, this.mHandler);
            synchronized (this.mAppIdleLock) {
                this.mAppIdleHistory.updateDisplay(isDisplayOn(), this.mInjector.elapsedRealtime());
            }
            this.mSystemServicesReady = true;
            if (this.mPendingInitializeDefaults) {
                initializeDefaultsForSystemApps(0);
            }
            if (this.mPendingOneTimeCheckIdleStates) {
                postOneTimeCheckIdleStates();
            }
        } else if (phase == 1000) {
            setChargingState(this.mInjector.isCharging());
        }
    }

    void reportContentProviderUsage(String authority, String providerPkgName, int userId) {
        Throwable th;
        int i = userId;
        if (this.mAppIdleEnabled) {
            String[] packages = ContentResolver.getSyncAdapterPackagesForAuthorityAsUser(authority, i);
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            int length = packages.length;
            int i2 = 0;
            while (i2 < length) {
                Object obj;
                int i3;
                long packages2;
                String packageName = packages[i2];
                String str;
                try {
                    PackageInfo pi = this.mPackageManager.getPackageInfoAsUser(packageName, DumpState.DUMP_DEXOPT, i);
                    PackageInfo packageInfo;
                    if (pi == null) {
                        packageInfo = pi;
                        str = packageName;
                        obj = length;
                        i3 = i2;
                        packages2 = packages;
                    } else if (pi.applicationInfo == null) {
                        packageInfo = pi;
                        str = packageName;
                        obj = length;
                        i3 = i2;
                        packages2 = packages;
                    } else if (packageName.equals(providerPkgName)) {
                        obj = length;
                        i3 = i2;
                        packages2 = packages;
                    } else {
                        obj = this.mAppIdleLock;
                        synchronized (obj) {
                            Object obj2;
                            try {
                                i3 = 0;
                                obj2 = obj;
                                obj = 0;
                                packages2 = elapsedRealtime + this.mSyncAdapterTimeoutMillis;
                                try {
                                    AppUsageHistory appUsage = this.mAppIdleHistory.reportUsage(packageName, i, 10, 8, (long) obj, packages2);
                                    obj = length;
                                    i3 = i2;
                                    packages2 = packages;
                                    maybeInformListeners(packageName, i, elapsedRealtime, appUsage.currentBucket, appUsage.bucketingReason, false);
                                } catch (Throwable th2) {
                                    th = th2;
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                packageInfo = pi;
                                str = packageName;
                                i3 = i2;
                                packages2 = packages;
                                obj2 = obj;
                                obj = length;
                                throw th;
                            }
                        }
                    }
                } catch (NameNotFoundException e) {
                    str = packageName;
                    obj = length;
                    i3 = i2;
                    packages2 = packages;
                }
                i2 = i3 + 1;
                String str2 = authority;
                i = userId;
                length = obj;
                packages = packages2;
            }
        }
    }

    void reportExemptedSyncScheduled(String packageName, int userId) {
        Throwable th;
        if (this.mAppIdleEnabled) {
            int bucketToPromote;
            int usageReason;
            long durationMillis;
            if (this.mInjector.isDeviceIdleMode()) {
                bucketToPromote = 20;
                usageReason = 12;
                durationMillis = this.mExemptedSyncScheduledDozeTimeoutMillis;
            } else {
                bucketToPromote = 10;
                usageReason = 11;
                durationMillis = this.mExemptedSyncScheduledNonDozeTimeoutMillis;
            }
            int bucketToPromote2 = bucketToPromote;
            int usageReason2 = usageReason;
            long durationMillis2 = durationMillis;
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            Object obj = this.mAppIdleLock;
            synchronized (obj) {
                Object obj2;
                try {
                    AppUsageHistory appUsage = this.mAppIdleHistory.reportUsage(packageName, userId, bucketToPromote2, usageReason2, 0, elapsedRealtime + durationMillis2);
                    obj2 = obj;
                    maybeInformListeners(packageName, userId, elapsedRealtime, appUsage.currentBucket, appUsage.bucketingReason, false);
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
    }

    void reportExemptedSyncStart(String packageName, int userId) {
        if (this.mAppIdleEnabled) {
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            synchronized (this.mAppIdleLock) {
                AppUsageHistory appUsage = this.mAppIdleHistory.reportUsage(packageName, userId, 10, 13, 0, elapsedRealtime + this.mExemptedSyncStartTimeoutMillis);
                maybeInformListeners(packageName, userId, elapsedRealtime, appUsage.currentBucket, appUsage.bucketingReason, false);
            }
        }
    }

    void setChargingState(boolean charging) {
        synchronized (this.mAppIdleLock) {
            if (this.mCharging != charging) {
                this.mCharging = charging;
                if (charging) {
                    this.mHandler.sendEmptyMessageDelayed(14, this.mStableChargingThresholdMillis);
                } else {
                    this.mHandler.removeMessages(14);
                    updateChargingStableState();
                }
            }
        }
    }

    void updateChargingStableState() {
        synchronized (this.mAppIdleLock) {
            if (this.mChargingStable != this.mCharging) {
                this.mChargingStable = this.mCharging;
                postParoleStateChanged();
            }
        }
    }

    void setAppIdleParoled(boolean paroled) {
        synchronized (this.mAppIdleLock) {
            long now = this.mInjector.currentTimeMillis();
            if (this.mAppIdleTempParoled != paroled) {
                this.mAppIdleTempParoled = paroled;
                if (paroled) {
                    postParoleEndTimeout();
                } else {
                    this.mLastAppIdleParoledTime = now;
                    postNextParoleTimeout(now, false);
                }
                postParoleStateChanged();
            }
        }
    }

    boolean isParoledOrCharging() {
        boolean z = true;
        if (!this.mAppIdleEnabled) {
            return true;
        }
        synchronized (this.mAppIdleLock) {
            if (!this.mAppIdleTempParoled) {
                if (!this.mChargingStable) {
                    z = false;
                }
            }
        }
        return z;
    }

    private void postNextParoleTimeout(long now, boolean forced) {
        this.mHandler.removeMessages(6);
        long timeLeft = (this.mLastAppIdleParoledTime + this.mAppIdleParoleIntervalMillis) - now;
        if (forced) {
            timeLeft += this.mAppIdleParoleWindowMillis;
        }
        if (timeLeft < 0) {
            timeLeft = 0;
        }
        this.mHandler.sendEmptyMessageDelayed(6, timeLeft);
    }

    private void postParoleEndTimeout() {
        this.mHandler.removeMessages(7);
        this.mHandler.sendEmptyMessageDelayed(7, this.mAppIdleParoleDurationMillis);
    }

    private void postParoleStateChanged() {
        this.mHandler.removeMessages(9);
        this.mHandler.sendEmptyMessage(9);
    }

    void postCheckIdleStates(int userId) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, userId, 0));
    }

    void postOneTimeCheckIdleStates() {
        if (this.mInjector.getBootPhase() < 500) {
            this.mPendingOneTimeCheckIdleStates = true;
            return;
        }
        this.mHandler.sendEmptyMessage(10);
        this.mPendingOneTimeCheckIdleStates = false;
    }

    boolean checkIdleStates(int checkUserId) {
        int i = checkUserId;
        if (!this.mAppIdleEnabled) {
            return false;
        }
        try {
            int[] runningUserIds = this.mInjector.getRunningUserIds();
            if (i != -1 && !ArrayUtils.contains(runningUserIds, i)) {
                return false;
            }
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 >= runningUserIds.length) {
                    return true;
                }
                int userId = runningUserIds[i3];
                if (i == -1 || i == userId) {
                    List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(512, userId);
                    int packageCount = packages.size();
                    i2 = 0;
                    while (true) {
                        int p = i2;
                        if (p >= packageCount) {
                            break;
                        }
                        PackageInfo pi = (PackageInfo) packages.get(p);
                        String packageName = pi.packageName;
                        int i4 = pi.applicationInfo.uid;
                        int packageCount2 = packageCount;
                        int p2 = p;
                        checkAndUpdateStandbyState(packageName, userId, i4, elapsedRealtime);
                        i2 = p2 + 1;
                        packageCount = packageCount2;
                    }
                }
                i2 = i3 + 1;
            }
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:58:0x00bf A:{Catch:{ all -> 0x00ea, all -> 0x00ef }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void checkAndUpdateStandbyState(String packageName, int userId, int uid, long elapsedRealtime) {
        int uid2;
        Throwable th;
        String str = packageName;
        int i = userId;
        long j = elapsedRealtime;
        if (uid <= 0) {
            try {
                uid2 = this.mPackageManager.getPackageUidAsUser(str, i);
            } catch (NameNotFoundException e) {
                return;
            }
        }
        uid2 = uid;
        if (isAppSpecial(str, UserHandle.getAppId(uid2), i)) {
            synchronized (this.mAppIdleLock) {
                this.mAppIdleHistory.setAppStandbyBucket(str, i, j, 5, 256);
            }
            maybeInformListeners(str, i, j, 5, 256, false);
        } else {
            Object obj = this.mAppIdleLock;
            synchronized (obj) {
                Object obj2;
                try {
                    AppUsageHistory app = this.mAppIdleHistory.getAppUsageHistory(str, i, j);
                    int reason = app.bucketingReason;
                    int oldMainReason = reason & JobPackageTracker.EVENT_STOP_REASON_MASK;
                    if (oldMainReason == 1024) {
                    } else {
                        int newBucket;
                        int reason2;
                        int newBucket2;
                        int newBucket3;
                        int oldBucket = app.currentBucket;
                        int newBucket4 = Math.max(oldBucket, 10);
                        boolean predictionLate = predictionTimedOut(app, j);
                        if (oldMainReason == 256 || oldMainReason == 768 || oldMainReason == 512 || predictionLate) {
                            if (predictionLate || app.lastPredictedBucket < 10 || app.lastPredictedBucket > 40) {
                                newBucket4 = getBucketForLocked(str, i, j);
                                reason = 512;
                            } else {
                                newBucket4 = app.lastPredictedBucket;
                                reason = UsbTerminalTypes.TERMINAL_TELE_PHONELINE;
                            }
                        }
                        long elapsedTimeAdjusted = this.mAppIdleHistory.getElapsedTime(j);
                        if (newBucket4 >= 10 && app.bucketActiveTimeoutTime > elapsedTimeAdjusted) {
                            newBucket = 10;
                            reason = app.bucketingReason;
                        } else if (newBucket4 < 20 || app.bucketWorkingSetTimeoutTime <= elapsedTimeAdjusted) {
                            reason2 = reason;
                            newBucket2 = newBucket4;
                            if (oldBucket >= newBucket2) {
                                if (!predictionLate) {
                                    obj2 = obj;
                                }
                            }
                            newBucket3 = newBucket2;
                            this.mAppIdleHistory.setAppStandbyBucket(str, i, j, newBucket3, reason2);
                            obj2 = obj;
                            maybeInformListeners(str, i, j, newBucket3, reason2, false);
                        } else {
                            newBucket = 20;
                            if (20 == oldBucket) {
                                newBucket4 = app.bucketingReason;
                            } else {
                                newBucket4 = UsbTerminalTypes.TERMINAL_OUT_LFSPEAKER;
                            }
                            reason = newBucket4;
                        }
                        reason2 = reason;
                        newBucket2 = newBucket;
                        if (oldBucket >= newBucket2) {
                        }
                        newBucket3 = newBucket2;
                        this.mAppIdleHistory.setAppStandbyBucket(str, i, j, newBucket3, reason2);
                        obj2 = obj;
                        maybeInformListeners(str, i, j, newBucket3, reason2, false);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
    }

    private boolean predictionTimedOut(AppUsageHistory app, long elapsedRealtime) {
        return app.lastPredictedTime > 0 && this.mAppIdleHistory.getElapsedTime(elapsedRealtime) - app.lastPredictedTime > this.mPredictionTimeoutMillis;
    }

    private void maybeInformListeners(String packageName, int userId, long elapsedRealtime, int bucket, int reason, boolean userStartedInteracting) {
        synchronized (this.mAppIdleLock) {
            if (this.mAppIdleHistory.shouldInformListeners(packageName, userId, elapsedRealtime, bucket)) {
                this.mHandler.sendMessage(this.mHandler.obtainMessage(3, StandbyUpdateRecord.obtain(packageName, userId, bucket, reason, userStartedInteracting)));
            }
        }
    }

    @GuardedBy("mAppIdleLock")
    int getBucketForLocked(String packageName, int userId, long elapsedRealtime) {
        return THRESHOLD_BUCKETS[this.mAppIdleHistory.getThresholdIndex(packageName, userId, elapsedRealtime, this.mAppStandbyScreenThresholds, this.mAppStandbyElapsedThresholds)];
    }

    void checkParoleTimeout() {
        boolean setParoled = false;
        boolean waitForNetwork = false;
        NetworkInfo activeNetwork = this.mConnectivityManager.getActiveNetworkInfo();
        boolean networkActive = activeNetwork != null && activeNetwork.isConnected();
        synchronized (this.mAppIdleLock) {
            long now = this.mInjector.currentTimeMillis();
            if (!this.mAppIdleTempParoled) {
                long timeSinceLastParole = now - this.mLastAppIdleParoledTime;
                if (timeSinceLastParole <= this.mAppIdleParoleIntervalMillis) {
                    postNextParoleTimeout(now, false);
                } else if (networkActive) {
                    setParoled = true;
                } else if (timeSinceLastParole > this.mAppIdleParoleIntervalMillis + this.mAppIdleParoleWindowMillis) {
                    setParoled = true;
                } else {
                    waitForNetwork = true;
                    postNextParoleTimeout(now, true);
                }
            }
        }
        if (waitForNetwork) {
            this.mConnectivityManager.registerNetworkCallback(this.mNetworkRequest, this.mNetworkCallback);
        }
        if (setParoled) {
            setAppIdleParoled(true);
        }
    }

    private void notifyBatteryStats(String packageName, int userId, boolean idle) {
        try {
            int uid = this.mPackageManager.getPackageUidAsUser(packageName, 8192, userId);
            if (idle) {
                this.mInjector.noteEvent(15, packageName, uid);
            } else {
                this.mInjector.noteEvent(16, packageName, uid);
            }
        } catch (NameNotFoundException | RemoteException e) {
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0021, code skipped:
            setAppIdleParoled(r1);
     */
    /* JADX WARNING: Missing block: B:12:0x0024, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void onDeviceIdleModeChanged() {
        boolean deviceIdle = this.mPowerManager.isDeviceIdleMode();
        synchronized (this.mAppIdleLock) {
            long timeSinceLastParole = this.mInjector.currentTimeMillis() - this.mLastAppIdleParoledTime;
            boolean paroled;
            if (!deviceIdle && timeSinceLastParole >= this.mAppIdleParoleIntervalMillis) {
                paroled = true;
            } else if (deviceIdle) {
                paroled = false;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:46:0x0105 A:{Catch:{ all -> 0x010d, all -> 0x0113 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void reportEvent(Event event, long elapsedRealtime, int userId) {
        Throwable th;
        Event event2 = event;
        long j = elapsedRealtime;
        int i = userId;
        if (this.mAppIdleEnabled) {
            Object obj = this.mAppIdleLock;
            synchronized (obj) {
                int i2;
                Object obj2;
                try {
                    int prevBucket;
                    long nextCheckTime;
                    boolean userStartedInteracting;
                    boolean previouslyIdle = this.mAppIdleHistory.isIdle(event2.mPackage, i, j);
                    if (!(event2.mEventType == 1 || event2.mEventType == 2 || event2.mEventType == 6 || event2.mEventType == 7 || event2.mEventType == 10 || event2.mEventType == 14)) {
                        if (event2.mEventType != 13) {
                            i2 = i;
                            obj2 = obj;
                        }
                    }
                    AppUsageHistory appHistory = this.mAppIdleHistory.getAppUsageHistory(event2.mPackage, i, j);
                    int prevBucket2 = appHistory.currentBucket;
                    int prevBucketReason = appHistory.bucketingReason;
                    int subReason = usageEventToSubReason(event2.mEventType);
                    int reason = 768 | subReason;
                    int i3;
                    int i4;
                    if (event2.mEventType == 10) {
                        i3 = 768;
                        prevBucket = prevBucket2;
                        i4 = 10;
                    } else if (event2.mEventType == 14) {
                        i3 = 768;
                        prevBucket = prevBucket2;
                        i4 = 10;
                    } else {
                        if (event2.mEventType == 6) {
                            this.mAppIdleHistory.reportUsage(appHistory, event2.mPackage, 10, subReason, 0, j + this.mSystemInteractionTimeoutMillis);
                            nextCheckTime = this.mSystemInteractionTimeoutMillis;
                            i3 = 768;
                            prevBucket = prevBucket2;
                            i4 = 10;
                        } else {
                            i3 = 768;
                            prevBucket = prevBucket2;
                            i4 = 10;
                            this.mAppIdleHistory.reportUsage(appHistory, event2.mPackage, 10, subReason, j, j + this.mStrongUsageTimeoutMillis);
                            nextCheckTime = this.mStrongUsageTimeoutMillis;
                        }
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(11, i, -1, event2.mPackage), nextCheckTime);
                        userStartedInteracting = (appHistory.currentBucket == 10 || prevBucket == appHistory.currentBucket || (prevBucketReason & JobPackageTracker.EVENT_STOP_REASON_MASK) == 768) ? false : true;
                        i2 = i;
                        obj2 = obj;
                        maybeInformListeners(event2.mPackage, i, j, appHistory.currentBucket, reason, userStartedInteracting);
                        if (previouslyIdle) {
                            notifyBatteryStats(event2.mPackage, i2, false);
                        }
                    }
                    this.mAppIdleHistory.reportUsage(appHistory, event2.mPackage, 20, subReason, 0, j + this.mNotificationSeenTimeoutMillis);
                    nextCheckTime = this.mNotificationSeenTimeoutMillis;
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(11, i, -1, event2.mPackage), nextCheckTime);
                    if (appHistory.currentBucket == 10) {
                    }
                    i2 = i;
                    obj2 = obj;
                    maybeInformListeners(event2.mPackage, i, j, appHistory.currentBucket, reason, userStartedInteracting);
                    if (previouslyIdle) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }
    }

    private int usageEventToSubReason(int eventType) {
        switch (eventType) {
            case 1:
                return 4;
            case 2:
                return 5;
            case 6:
                return 1;
            case 7:
                return 3;
            case 10:
                return 2;
            case 13:
                return 10;
            case 14:
                return 9;
            default:
                return 0;
        }
    }

    void forceIdleState(String packageName, int userId, boolean idle) {
        if (this.mAppIdleEnabled) {
            int appId = getAppId(packageName);
            if (appId >= 0) {
                int standbyBucket;
                long elapsedRealtime = this.mInjector.elapsedRealtime();
                AppStandbyController appStandbyController = this;
                boolean previouslyIdle = appStandbyController.isAppIdleFiltered(packageName, appId, userId, elapsedRealtime);
                synchronized (this.mAppIdleLock) {
                    try {
                        appStandbyController = this.mAppIdleHistory;
                        standbyBucket = appStandbyController.setIdle(packageName, userId, idle, elapsedRealtime);
                    } finally {
                        previouslyIdle = 
/*
Method generation error in method: com.android.server.usage.AppStandbyController.forceIdleState(java.lang.String, int, boolean):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r14_1 'previouslyIdle' boolean) = (r14_0 'previouslyIdle' boolean), (r15_0 'this' boolean A:{THIS}) in method: com.android.server.usage.AppStandbyController.forceIdleState(java.lang.String, int, boolean):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:102)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:52)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeSynchronizedRegion(RegionGen.java:230)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:67)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 42 more

*/

    public void setLastJobRunTime(String packageName, int userId, long elapsedRealtime) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.setLastJobRunTime(packageName, userId, elapsedRealtime);
        }
    }

    public long getTimeSinceLastJobRun(String packageName, int userId) {
        long timeSinceLastJobRun;
        long elapsedRealtime = this.mInjector.elapsedRealtime();
        synchronized (this.mAppIdleLock) {
            timeSinceLastJobRun = this.mAppIdleHistory.getTimeSinceLastJobRun(packageName, userId, elapsedRealtime);
        }
        return timeSinceLastJobRun;
    }

    public void onUserRemoved(int userId) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.onUserRemoved(userId);
            synchronized (this.mActiveAdminApps) {
                this.mActiveAdminApps.remove(userId);
            }
        }
    }

    private boolean isAppIdleUnfiltered(String packageName, int userId, long elapsedRealtime) {
        boolean isIdle;
        synchronized (this.mAppIdleLock) {
            isIdle = this.mAppIdleHistory.isIdle(packageName, userId, elapsedRealtime);
        }
        return isIdle;
    }

    void addListener(AppIdleStateChangeListener listener) {
        synchronized (this.mPackageAccessListeners) {
            if (!this.mPackageAccessListeners.contains(listener)) {
                this.mPackageAccessListeners.add(listener);
            }
        }
    }

    void removeListener(AppIdleStateChangeListener listener) {
        synchronized (this.mPackageAccessListeners) {
            this.mPackageAccessListeners.remove(listener);
        }
    }

    int getAppId(String packageName) {
        try {
            return this.mPackageManager.getApplicationInfo(packageName, 4194816).uid;
        } catch (NameNotFoundException e) {
            return -1;
        }
    }

    boolean isAppIdleFilteredOrParoled(String packageName, int userId, long elapsedRealtime, boolean shouldObfuscateInstantApps) {
        if (isParoledOrCharging()) {
            return false;
        }
        if (shouldObfuscateInstantApps && this.mInjector.isPackageEphemeral(userId, packageName)) {
            return false;
        }
        return isAppIdleFiltered(packageName, getAppId(packageName), userId, elapsedRealtime);
    }

    boolean isAppSpecial(String packageName, int appId, int userId) {
        if (packageName == null) {
            return false;
        }
        if (!this.mAppIdleEnabled || appId < 10000 || packageName.equals(PackageManagerService.PLATFORM_PACKAGE_NAME)) {
            return true;
        }
        if (this.mSystemServicesReady) {
            try {
                if (this.mInjector.isPowerSaveWhitelistExceptIdleApp(packageName) || isActiveDeviceAdmin(packageName, userId) || isActiveNetworkScorer(packageName)) {
                    return true;
                }
                if ((this.mAppWidgetManager != null && this.mInjector.isBoundWidgetPackage(this.mAppWidgetManager, packageName, userId)) || isDeviceProvisioningPackage(packageName)) {
                    return true;
                }
            } catch (RemoteException re) {
                throw re.rethrowFromSystemServer();
            }
        }
        if (isCarrierApp(packageName)) {
            return true;
        }
        return false;
    }

    boolean isAppIdleFiltered(String packageName, int appId, int userId, long elapsedRealtime) {
        if (isAppSpecial(packageName, appId, userId)) {
            return false;
        }
        return isAppIdleUnfiltered(packageName, userId, elapsedRealtime);
    }

    int[] getIdleUidsForUser(int userId) {
        if (!this.mAppIdleEnabled) {
            return new int[0];
        }
        long elapsedRealtime = this.mInjector.elapsedRealtime();
        try {
            ParceledListSlice<ApplicationInfo> slice = AppGlobals.getPackageManager().getInstalledApplications(0, userId);
            if (slice == null) {
                return new int[0];
            }
            int index;
            int i;
            List<ApplicationInfo> apps = slice.getList();
            SparseIntArray uidStates = new SparseIntArray();
            int i2 = apps.size() - 1;
            while (true) {
                int i3 = i2;
                if (i3 < 0) {
                    break;
                }
                ApplicationInfo ai = (ApplicationInfo) apps.get(i3);
                boolean idle = isAppIdleFiltered(ai.packageName, UserHandle.getAppId(ai.uid), userId, elapsedRealtime);
                index = uidStates.indexOfKey(ai.uid);
                i = 65536;
                if (index < 0) {
                    int i4 = ai.uid;
                    if (!idle) {
                        i = 0;
                    }
                    uidStates.put(i4, i + 1);
                } else {
                    int valueAt = uidStates.valueAt(index) + 1;
                    if (!idle) {
                        i = 0;
                    }
                    uidStates.setValueAt(index, valueAt + i);
                }
                i2 = i3 - 1;
            }
            int numIdle = 0;
            for (i2 = uidStates.size() - 1; i2 >= 0; i2--) {
                index = uidStates.valueAt(i2);
                if ((index & 32767) == (index >> 16)) {
                    numIdle++;
                }
            }
            int[] res = new int[numIdle];
            numIdle = 0;
            for (index = uidStates.size() - 1; index >= 0; index--) {
                i = uidStates.valueAt(index);
                if ((i & 32767) == (i >> 16)) {
                    res[numIdle] = uidStates.keyAt(index);
                    numIdle++;
                }
            }
            return res;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    void setAppIdleAsync(String packageName, boolean idle, int userId) {
        if (packageName != null && this.mAppIdleEnabled) {
            this.mHandler.obtainMessage(4, userId, idle, packageName).sendToTarget();
        }
    }

    public int getAppStandbyBucket(String packageName, int userId, long elapsedRealtime, boolean shouldObfuscateInstantApps) {
        if (!this.mAppIdleEnabled || (shouldObfuscateInstantApps && this.mInjector.isPackageEphemeral(userId, packageName))) {
            return 10;
        }
        int appStandbyBucket;
        synchronized (this.mAppIdleLock) {
            appStandbyBucket = this.mAppIdleHistory.getAppStandbyBucket(packageName, userId, elapsedRealtime);
        }
        return appStandbyBucket;
    }

    public List<AppStandbyInfo> getAppStandbyBuckets(int userId) {
        ArrayList appStandbyBuckets;
        synchronized (this.mAppIdleLock) {
            appStandbyBuckets = this.mAppIdleHistory.getAppStandbyBuckets(userId, this.mAppIdleEnabled);
        }
        return appStandbyBuckets;
    }

    void setAppStandbyBucket(String packageName, int userId, int newBucket, int reason, long elapsedRealtime) {
        setAppStandbyBucket(packageName, userId, newBucket, reason, elapsedRealtime, false);
    }

    void setAppStandbyBucket(String packageName, int userId, int newBucket, int reason, long elapsedRealtime, boolean resetTimeout) {
        Throwable th;
        int i = newBucket;
        long j = elapsedRealtime;
        synchronized (this.mAppIdleLock) {
            String str;
            int i2;
            try {
                str = packageName;
                i2 = userId;
                try {
                    AppUsageHistory app = this.mAppIdleHistory.getAppUsageHistory(str, i2, j);
                    boolean predicted = (reason & JobPackageTracker.EVENT_STOP_REASON_MASK) == 1280;
                    if (app.currentBucket < 10) {
                    } else if ((app.currentBucket == 50 || i == 50) && predicted) {
                    } else if ((JobPackageTracker.EVENT_STOP_REASON_MASK & app.bucketingReason) == 1024 && predicted) {
                    } else {
                        int newBucket2;
                        int reason2;
                        if (predicted) {
                            int reason3;
                            long elapsedTimeAdjusted = this.mAppIdleHistory.getElapsedTime(j);
                            this.mAppIdleHistory.updateLastPrediction(app, elapsedTimeAdjusted, i);
                            if (i > 10 && app.bucketActiveTimeoutTime > elapsedTimeAdjusted) {
                                i = 10;
                                reason3 = app.bucketingReason;
                            } else if (i > 20 && app.bucketWorkingSetTimeoutTime > elapsedTimeAdjusted) {
                                i = 20;
                                if (app.currentBucket != 20) {
                                    reason3 = UsbTerminalTypes.TERMINAL_OUT_LFSPEAKER;
                                } else {
                                    reason3 = app.bucketingReason;
                                }
                            }
                            newBucket2 = i;
                            reason2 = reason3;
                            this.mAppIdleHistory.setAppStandbyBucket(str, i2, j, newBucket2, reason2, resetTimeout);
                            maybeInformListeners(str, i2, j, newBucket2, reason2, false);
                        }
                        reason2 = reason;
                        newBucket2 = i;
                        try {
                            this.mAppIdleHistory.setAppStandbyBucket(str, i2, j, newBucket2, reason2, resetTimeout);
                            maybeInformListeners(str, i2, j, newBucket2, reason2, false);
                        } catch (Throwable th2) {
                            th = th2;
                            i = newBucket2;
                            int i3 = reason2;
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
                } catch (Throwable th4) {
                    th = th4;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            } catch (Throwable th5) {
                th = th5;
                str = packageName;
                i2 = userId;
                while (true) {
                    break;
                }
                throw th;
            }
        }
    }

    @VisibleForTesting
    boolean isActiveDeviceAdmin(String packageName, int userId) {
        boolean z;
        synchronized (this.mActiveAdminApps) {
            Set<String> adminPkgs = (Set) this.mActiveAdminApps.get(userId);
            z = adminPkgs != null && adminPkgs.contains(packageName);
        }
        return z;
    }

    public void addActiveDeviceAdmin(String adminPkg, int userId) {
        synchronized (this.mActiveAdminApps) {
            Set<String> adminPkgs = (Set) this.mActiveAdminApps.get(userId);
            if (adminPkgs == null) {
                adminPkgs = new ArraySet();
                this.mActiveAdminApps.put(userId, adminPkgs);
            }
            adminPkgs.add(adminPkg);
        }
    }

    public void setActiveAdminApps(Set<String> adminPkgs, int userId) {
        synchronized (this.mActiveAdminApps) {
            if (adminPkgs == null) {
                try {
                    this.mActiveAdminApps.remove(userId);
                } catch (Throwable th) {
                }
            } else {
                this.mActiveAdminApps.put(userId, adminPkgs);
            }
        }
    }

    public void onAdminDataAvailable() {
        this.mAdminDataAvailableLatch.countDown();
    }

    private void waitForAdminData() {
        if (this.mContext.getPackageManager().hasSystemFeature("android.software.device_admin")) {
            ConcurrentUtils.waitForCountDownNoInterrupt(this.mAdminDataAvailableLatch, 10000, "Wait for admin data");
        }
    }

    Set<String> getActiveAdminAppsForTest(int userId) {
        Set set;
        synchronized (this.mActiveAdminApps) {
            set = (Set) this.mActiveAdminApps.get(userId);
        }
        return set;
    }

    private boolean isDeviceProvisioningPackage(String packageName) {
        String deviceProvisioningPackage = this.mContext.getResources().getString(17039794);
        return deviceProvisioningPackage != null && deviceProvisioningPackage.equals(packageName);
    }

    private boolean isCarrierApp(String packageName) {
        synchronized (this.mAppIdleLock) {
            if (!this.mHaveCarrierPrivilegedApps) {
                fetchCarrierPrivilegedAppsLocked();
            }
            if (this.mCarrierPrivilegedApps != null) {
                boolean contains = this.mCarrierPrivilegedApps.contains(packageName);
                return contains;
            }
            return false;
        }
    }

    void clearCarrierPrivilegedApps() {
        synchronized (this.mAppIdleLock) {
            this.mHaveCarrierPrivilegedApps = false;
            this.mCarrierPrivilegedApps = null;
        }
    }

    @GuardedBy("mAppIdleLock")
    private void fetchCarrierPrivilegedAppsLocked() {
        this.mCarrierPrivilegedApps = ((TelephonyManager) this.mContext.getSystemService(TelephonyManager.class)).getPackagesWithCarrierPrivileges();
        this.mHaveCarrierPrivilegedApps = true;
    }

    private boolean isActiveNetworkScorer(String packageName) {
        return packageName != null && packageName.equals(this.mInjector.getActiveNetworkScorer());
    }

    void informListeners(String packageName, int userId, int bucket, int reason, boolean userInteraction) {
        boolean idle = bucket >= 40;
        synchronized (this.mPackageAccessListeners) {
            Iterator it = this.mPackageAccessListeners.iterator();
            while (it.hasNext()) {
                AppIdleStateChangeListener appIdleStateChangeListener = (AppIdleStateChangeListener) it.next();
                AppIdleStateChangeListener listener = appIdleStateChangeListener;
                appIdleStateChangeListener.onAppIdleStateChanged(packageName, userId, idle, bucket, reason);
                if (userInteraction) {
                    listener.onUserInteractionStarted(packageName, userId);
                }
            }
        }
    }

    void informParoleStateChanged() {
        boolean paroled = isParoledOrCharging();
        synchronized (this.mPackageAccessListeners) {
            Iterator it = this.mPackageAccessListeners.iterator();
            while (it.hasNext()) {
                ((AppIdleStateChangeListener) it.next()).onParoleStateChanged(paroled);
            }
        }
    }

    void flushToDisk(int userId) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.writeAppIdleTimes(userId);
        }
    }

    void flushDurationsToDisk() {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.writeAppIdleDurations();
        }
    }

    boolean isDisplayOn() {
        return this.mInjector.isDefaultDisplayOn();
    }

    void clearAppIdleForPackage(String packageName, int userId) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.clearUsage(packageName, userId);
        }
    }

    void initializeDefaultsForSystemApps(int userId) {
        Throwable th;
        int i = userId;
        if (this.mSystemServicesReady) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Initializing defaults for system apps on user ");
            stringBuilder.append(i);
            stringBuilder.append(", appIdleEnabled=");
            stringBuilder.append(this.mAppIdleEnabled);
            Slog.d(str, stringBuilder.toString());
            long elapsedRealtime = this.mInjector.elapsedRealtime();
            List<PackageInfo> packages = this.mPackageManager.getInstalledPackagesAsUser(512, i);
            int packageCount = packages.size();
            Object obj = this.mAppIdleLock;
            synchronized (obj) {
                int i2 = 0;
                while (i2 < packageCount) {
                    Object obj2;
                    try {
                        PackageInfo pi = (PackageInfo) packages.get(i2);
                        String packageName = pi.packageName;
                        if (pi.applicationInfo == null || !pi.applicationInfo.isSystemApp()) {
                            obj2 = obj;
                        } else {
                            obj2 = obj;
                            this.mAppIdleHistory.reportUsage(packageName, i, 10, 6, 0, elapsedRealtime + this.mSystemUpdateUsageTimeoutMillis);
                        }
                        i2++;
                        obj = obj2;
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
                return;
            }
        }
        this.mPendingInitializeDefaults = true;
    }

    void postReportContentProviderUsage(String name, String packageName, int userId) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = name;
        args.arg2 = packageName;
        args.arg3 = Integer.valueOf(userId);
        this.mHandler.obtainMessage(8, args).sendToTarget();
    }

    void postReportExemptedSyncScheduled(String packageName, int userId) {
        this.mHandler.obtainMessage(12, userId, 0, packageName).sendToTarget();
    }

    void postReportExemptedSyncStart(String packageName, int userId) {
        this.mHandler.obtainMessage(13, userId, 0, packageName).sendToTarget();
    }

    void dumpUser(IndentingPrintWriter idpw, int userId, String pkg) {
        synchronized (this.mAppIdleLock) {
            this.mAppIdleHistory.dump(idpw, userId, pkg);
        }
    }

    void dumpState(String[] args, PrintWriter pw) {
        synchronized (this.mAppIdleLock) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Carrier privileged apps (have=");
            stringBuilder.append(this.mHaveCarrierPrivilegedApps);
            stringBuilder.append("): ");
            stringBuilder.append(this.mCarrierPrivilegedApps);
            pw.println(stringBuilder.toString());
        }
        pw.println();
        pw.println("Settings:");
        pw.print("  mCheckIdleIntervalMillis=");
        TimeUtils.formatDuration(this.mCheckIdleIntervalMillis, pw);
        pw.println();
        pw.print("  mAppIdleParoleIntervalMillis=");
        TimeUtils.formatDuration(this.mAppIdleParoleIntervalMillis, pw);
        pw.println();
        pw.print("  mAppIdleParoleWindowMillis=");
        TimeUtils.formatDuration(this.mAppIdleParoleWindowMillis, pw);
        pw.println();
        pw.print("  mAppIdleParoleDurationMillis=");
        TimeUtils.formatDuration(this.mAppIdleParoleDurationMillis, pw);
        pw.println();
        pw.print("  mExemptedSyncScheduledNonDozeTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncScheduledNonDozeTimeoutMillis, pw);
        pw.println();
        pw.print("  mExemptedSyncScheduledDozeTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncScheduledDozeTimeoutMillis, pw);
        pw.println();
        pw.print("  mExemptedSyncStartTimeoutMillis=");
        TimeUtils.formatDuration(this.mExemptedSyncStartTimeoutMillis, pw);
        pw.println();
        pw.println();
        pw.print("mAppIdleEnabled=");
        pw.print(this.mAppIdleEnabled);
        pw.print(" mAppIdleTempParoled=");
        pw.print(this.mAppIdleTempParoled);
        pw.print(" mCharging=");
        pw.print(this.mCharging);
        pw.print(" mChargingStable=");
        pw.print(this.mChargingStable);
        pw.print(" mLastAppIdleParoledTime=");
        TimeUtils.formatDuration(this.mLastAppIdleParoledTime, pw);
        pw.println();
        pw.print("mScreenThresholds=");
        pw.println(Arrays.toString(this.mAppStandbyScreenThresholds));
        pw.print("mElapsedThresholds=");
        pw.println(Arrays.toString(this.mAppStandbyElapsedThresholds));
        pw.print("mStableChargingThresholdMillis=");
        TimeUtils.formatDuration(this.mStableChargingThresholdMillis, pw);
        pw.println();
    }
}
