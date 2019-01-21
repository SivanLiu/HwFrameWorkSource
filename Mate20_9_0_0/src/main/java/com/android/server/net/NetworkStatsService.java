package com.android.server.net;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.app.IUidObserver.Stub;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.DataUsageRequest;
import android.net.IConnectivityManager;
import android.net.INetworkManagementEventObserver;
import android.net.INetworkStatsSession;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkIdentity;
import android.net.NetworkState;
import android.net.NetworkStats;
import android.net.NetworkStats.NonMonotonicObserver;
import android.net.NetworkStatsHistory;
import android.net.NetworkStatsHistory.Entry;
import android.net.NetworkTemplate;
import android.os.BestClock;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.telephony.SubscriptionPlan;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.net.VpnInfo;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IHwNetworkStatsService;
import com.android.server.LocalServices;
import com.android.server.NetPluginDelegate;
import com.android.server.NetworkManagementService;
import com.android.server.NetworkManagementSocketTagger;
import com.android.server.ServiceThread;
import com.android.server.job.controllers.JobStatus;
import com.android.server.lights.LightsManager;
import com.android.server.pm.PackageManagerService;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class NetworkStatsService extends AbsNetworkStatsService {
    @VisibleForTesting
    public static final String ACTION_NETWORK_STATS_POLL = "com.android.server.action.NETWORK_STATS_POLL";
    public static final String ACTION_NETWORK_STATS_UPDATED = "com.android.server.action.NETWORK_STATS_UPDATED";
    private static final int DUMP_STATS_SESSION_COUNT = 20;
    private static final int FLAG_PERSIST_ALL = 3;
    private static final int FLAG_PERSIST_FORCE = 256;
    private static final int FLAG_PERSIST_NETWORK = 1;
    private static final int FLAG_PERSIST_UID = 2;
    static final boolean LOGD = Log.isLoggable(TAG, 3);
    static final boolean LOGV = Log.isLoggable(TAG, 2);
    private static final int MSG_PERFORM_POLL = 1;
    private static final int MSG_REGISTER_GLOBAL_ALERT = 3;
    private static final int MSG_UPDATE_IFACES = 2;
    private static final int MY_PID = Process.myPid();
    private static final long POLL_RATE_LIMIT_MS = 15000;
    private static final String PREFIX_DEV = "dev";
    private static final String PREFIX_UID = "uid";
    private static final String PREFIX_UID_TAG = "uid_tag";
    private static final String PREFIX_XT = "xt";
    static final String TAG = "NetworkStats";
    private static final String TAG_NETSTATS_ERROR = "netstats_error";
    private static int TYPE_RX_BYTES = 0;
    private static int TYPE_RX_PACKETS = 0;
    private static int TYPE_TCP_RX_PACKETS = 0;
    private static int TYPE_TCP_TX_PACKETS = 0;
    private static int TYPE_TX_BYTES = 0;
    private static int TYPE_TX_PACKETS = 0;
    private static final int UID_MSG_ACTIVE = 102;
    private static final int UID_MSG_GONE = 101;
    private static final int UID_MSG_IDLE = 103;
    private static final int UID_MSG_STATE_CHANGED = 100;
    private static final int USER_TYPE = SystemProperties.getInt("ro.logsystem.usertype", 1);
    public static final String VT_INTERFACE = "vt_data0";
    private static IHwNetworkStatsService mHwNetworkStatsService = null;
    protected String mActiveIface;
    @GuardedBy("mStatsLock")
    private final ArrayMap<String, NetworkIdentitySet> mActiveIfaces = new ArrayMap();
    private SparseIntArray mActiveUidCounterSet = new SparseIntArray();
    @GuardedBy("mStatsLock")
    protected final ArrayMap<String, NetworkIdentitySet> mActiveUidIfaces = new ArrayMap();
    final IActivityManager mActivityManager = ActivityManager.getService();
    private final AlarmManager mAlarmManager;
    private INetworkManagementEventObserver mAlertObserver = new BaseNetworkObserver() {
        public void limitReached(String limitName, String iface) {
            NetworkStatsService.this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", NetworkStatsService.TAG);
            Slog.i(NetworkStatsService.TAG, "limitReached");
            if (NetworkManagementService.LIMIT_GLOBAL_ALERT.equals(limitName)) {
                NetworkStatsService.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
                NetworkStatsService.this.mHandler.obtainMessage(3).sendToTarget();
            }
        }
    };
    private final File mBaseDir;
    private final Clock mClock;
    private IConnectivityManager mConnManager;
    protected final Context mContext;
    @GuardedBy("mStatsLock")
    private Network[] mDefaultNetworks = new Network[0];
    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mDevRecorder;
    private long mGlobalAlertBytes;
    private Handler mHandler;
    private Callback mHandlerCallback;
    private long mLastStatsSessionPoll;
    @GuardedBy("mStatsLock")
    private String[] mMobileIfaces = new String[0];
    private final INetworkManagementService mNetworkManager;
    private final DropBoxNonMonotonicObserver mNonMonotonicObserver = new DropBoxNonMonotonicObserver(this, null);
    @GuardedBy("mOpenSessionCallsPerUid")
    private final SparseIntArray mOpenSessionCallsPerUid = new SparseIntArray();
    final PackageManagerService mPMS = ((PackageManagerService) ServiceManager.getService("package"));
    private long mPersistThreshold = 2097152;
    private PendingIntent mPollIntent;
    private BroadcastReceiver mPollReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Slog.i(NetworkStatsService.TAG, "Alarm manager triggered");
            NetworkStatsService.this.performPoll(3);
            NetworkStatsService.this.registerGlobalAlert();
        }
    };
    private BroadcastReceiver mRemovedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra("android.intent.extra.UID", -1) != -1) {
                Slog.i(NetworkStatsService.TAG, "ACTION_UID_REMOVED onReceive");
                synchronized (NetworkStatsService.this.mStatsLock) {
                    NetworkStatsService.this.mWakeLock.acquire();
                    try {
                        NetworkStatsService.this.removeUidsLocked(uid);
                    } finally {
                        NetworkStatsService.this.mWakeLock.release();
                    }
                }
            }
        }
    };
    protected final NetworkStatsSettings mSettings;
    private BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            synchronized (NetworkStatsService.this.mStatsLock) {
                NetworkStatsService.this.shutdownLocked();
            }
        }
    };
    private final Object mStatsLock = new Object();
    private final NetworkStatsObservers mStatsObservers;
    protected final File mSystemDir;
    private volatile boolean mSystemReady;
    private final TelephonyManager mTeleManager;
    private BroadcastReceiver mTetherReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Slog.i(NetworkStatsService.TAG, "ACTION_TETHER_STATE_CHANGED onReceive");
            NetworkStatsService.this.performPoll(1);
        }
    };
    private long mTimeRefreshRealtime;
    public final Handler mUidEventHandler;
    private final Callback mUidEventHandlerCallback = new Callback() {
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    NetworkStatsService.this.handleUidChanged(msg.arg1, msg.arg2, ((Long) msg.obj).longValue());
                    return true;
                case 101:
                    NetworkStatsService.this.handleUidGone(msg.arg1);
                    return true;
                case 102:
                    return true;
                case 103:
                    return true;
                default:
                    return false;
            }
        }
    };
    private final ServiceThread mUidEventThread;
    private IUidObserver mUidObserver = new Stub() {
        public void onUidStateChanged(int uid, int procState, long procStateSeq) throws RemoteException {
            if (NetworkStatsService.this.mUidEventHandler != null) {
                NetworkStatsService.this.mUidEventHandler.obtainMessage(100, uid, procState, Long.valueOf(procStateSeq)).sendToTarget();
            }
        }

        public void onUidGone(int uid, boolean disabled) throws RemoteException {
            if (NetworkStatsService.this.mUidEventHandler != null) {
                NetworkStatsService.this.mUidEventHandler.obtainMessage(101, uid, 0).sendToTarget();
            }
        }

        public void onUidActive(int uid) throws RemoteException {
            if (NetworkStatsService.this.mUidEventHandler != null) {
                NetworkStatsService.this.mUidEventHandler.obtainMessage(102, uid, 0).sendToTarget();
            }
        }

        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
            if (NetworkStatsService.this.mUidEventHandler != null) {
                NetworkStatsService.this.mUidEventHandler.obtainMessage(103, uid, 0).sendToTarget();
            }
        }

        public void onUidCachedChanged(int uid, boolean cached) throws RemoteException {
        }
    };
    private NetworkStats mUidOperations = new NetworkStats(0, 10);
    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mUidRecorder;
    final Object mUidRulesFirstLock = new Object();
    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mUidTagRecorder;
    private int mUpdateStatsCount = 0;
    private final boolean mUseBpfTrafficStats;
    private BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            if (userId != -1) {
                Slog.i(NetworkStatsService.TAG, "ACTION_USER_REMOVED onReceive");
                synchronized (NetworkStatsService.this.mStatsLock) {
                    NetworkStatsService.this.mWakeLock.acquire();
                    try {
                        NetworkStatsService.this.removeUserLocked(userId);
                    } finally {
                        NetworkStatsService.this.mWakeLock.release();
                    }
                }
            }
        }
    };
    private final WakeLock mWakeLock;
    @GuardedBy("mStatsLock")
    private NetworkStatsRecorder mXtRecorder;
    @GuardedBy("mStatsLock")
    private NetworkStatsCollection mXtStatsCached;

    private class DropBoxNonMonotonicObserver implements NonMonotonicObserver<String> {
        private DropBoxNonMonotonicObserver() {
        }

        /* synthetic */ DropBoxNonMonotonicObserver(NetworkStatsService x0, AnonymousClass1 x1) {
            this();
        }

        public void foundNonMonotonic(NetworkStats left, int leftIndex, NetworkStats right, int rightIndex, String cookie) {
            Log.w(NetworkStatsService.TAG, "Found non-monotonic values; saving to dropbox");
            StringBuilder builder = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("found non-monotonic ");
            stringBuilder.append(cookie);
            stringBuilder.append(" values at left[");
            stringBuilder.append(leftIndex);
            stringBuilder.append("] - right[");
            stringBuilder.append(rightIndex);
            stringBuilder.append("]\n");
            builder.append(stringBuilder.toString());
            builder.append("left=");
            builder.append(left);
            builder.append(10);
            builder.append("right=");
            builder.append(right);
            builder.append(10);
            ((DropBoxManager) NetworkStatsService.this.mContext.getSystemService(DropBoxManager.class)).addText(NetworkStatsService.TAG_NETSTATS_ERROR, builder.toString());
        }

        public void foundNonMonotonic(NetworkStats stats, int statsIndex, String cookie) {
            Log.w(NetworkStatsService.TAG, "Found non-monotonic values; saving to dropbox");
            StringBuilder builder = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Found non-monotonic ");
            stringBuilder.append(cookie);
            stringBuilder.append(" values at [");
            stringBuilder.append(statsIndex);
            stringBuilder.append("]\n");
            builder.append(stringBuilder.toString());
            builder.append("stats=");
            builder.append(stats);
            builder.append(10);
            ((DropBoxManager) NetworkStatsService.this.mContext.getSystemService(DropBoxManager.class)).addText(NetworkStatsService.TAG_NETSTATS_ERROR, builder.toString());
        }
    }

    @VisibleForTesting
    static class HandlerCallback implements Callback {
        private final NetworkStatsService mService;

        HandlerCallback(NetworkStatsService service) {
            this.mService = service;
        }

        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    this.mService.performPoll(msg.arg1);
                    return true;
                case 2:
                    this.mService.updateIfaces(null);
                    return true;
                case 3:
                    this.mService.registerGlobalAlert();
                    return true;
                default:
                    return false;
            }
        }
    }

    public interface NetworkStatsSettings {

        public static class Config {
            public final long bucketDuration;
            public final long deleteAgeMillis;
            public final long rotateAgeMillis;

            public Config(long bucketDuration, long rotateAgeMillis, long deleteAgeMillis) {
                this.bucketDuration = bucketDuration;
                this.rotateAgeMillis = rotateAgeMillis;
                this.deleteAgeMillis = deleteAgeMillis;
            }
        }

        boolean getAugmentEnabled();

        Config getDevConfig();

        long getDevPersistBytes(long j);

        long getGlobalAlertBytes(long j);

        long getPollInterval();

        boolean getSampleEnabled();

        Config getUidConfig();

        long getUidPersistBytes(long j);

        Config getUidTagConfig();

        long getUidTagPersistBytes(long j);

        Config getXtConfig();

        long getXtPersistBytes(long j);
    }

    private static class DefaultNetworkStatsSettings implements NetworkStatsSettings {
        private final ContentResolver mResolver;

        public DefaultNetworkStatsSettings(Context context) {
            this.mResolver = (ContentResolver) Preconditions.checkNotNull(context.getContentResolver());
        }

        private long getGlobalLong(String name, long def) {
            return Global.getLong(this.mResolver, name, def);
        }

        private boolean getGlobalBoolean(String name, boolean def) {
            return Global.getInt(this.mResolver, name, def) != 0;
        }

        public long getPollInterval() {
            return getGlobalLong("netstats_poll_interval", 1800000);
        }

        public long getGlobalAlertBytes(long def) {
            return getGlobalLong("netstats_global_alert_bytes", def);
        }

        public boolean getSampleEnabled() {
            return getGlobalBoolean("netstats_sample_enabled", true);
        }

        public boolean getAugmentEnabled() {
            return getGlobalBoolean("netstats_augment_enabled", true);
        }

        public Config getDevConfig() {
            return new Config(getGlobalLong("netstats_dev_bucket_duration", SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT), getGlobalLong("netstats_dev_rotate_age", 1296000000), getGlobalLong("netstats_dev_delete_age", 7776000000L));
        }

        public Config getXtConfig() {
            return getDevConfig();
        }

        public Config getUidConfig() {
            return new Config(getGlobalLong("netstats_uid_bucket_duration", SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT), getGlobalLong("netstats_uid_rotate_age", 1296000000), getGlobalLong("netstats_uid_delete_age", 7776000000L));
        }

        public Config getUidTagConfig() {
            return new Config(getGlobalLong("netstats_uid_tag_bucket_duration", SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT), getGlobalLong("netstats_uid_tag_rotate_age", 432000000), getGlobalLong("netstats_uid_tag_delete_age", 1296000000));
        }

        public long getDevPersistBytes(long def) {
            return getGlobalLong("netstats_dev_persist_bytes", def);
        }

        public long getXtPersistBytes(long def) {
            return getDevPersistBytes(def);
        }

        public long getUidPersistBytes(long def) {
            return getGlobalLong("netstats_uid_persist_bytes", def);
        }

        public long getUidTagPersistBytes(long def) {
            return getGlobalLong("netstats_uid_tag_persist_bytes", def);
        }
    }

    private class NetworkStatsManagerInternalImpl extends NetworkStatsManagerInternal {
        private NetworkStatsManagerInternalImpl() {
        }

        /* synthetic */ NetworkStatsManagerInternalImpl(NetworkStatsService x0, AnonymousClass1 x1) {
            this();
        }

        public long getNetworkTotalBytes(NetworkTemplate template, long start, long end) {
            Trace.traceBegin(2097152, "getNetworkTotalBytes");
            try {
                long access$1600 = NetworkStatsService.this.getNetworkTotalBytes(template, start, end);
                return access$1600;
            } finally {
                Trace.traceEnd(2097152);
            }
        }

        public NetworkStats getNetworkUidBytes(NetworkTemplate template, long start, long end) {
            Trace.traceBegin(2097152, "getNetworkUidBytes");
            try {
                NetworkStats access$1700 = NetworkStatsService.this.getNetworkUidBytes(template, start, end);
                return access$1700;
            } finally {
                Trace.traceEnd(2097152);
            }
        }

        public void setUidForeground(int uid, boolean uidForeground) {
            NetworkStatsService.this.setUidForeground(uid, uidForeground);
        }

        public void advisePersistThreshold(long thresholdBytes) {
            NetworkStatsService.this.advisePersistThreshold(thresholdBytes);
        }

        public void forceUpdate() {
            NetworkStatsService.this.forceUpdate();
        }
    }

    private static native long nativeGetIfaceStat(String str, int i, boolean z);

    private static native long nativeGetTotalStat(int i, boolean z);

    private static native long nativeGetUidStat(int i, int i2, boolean z);

    private static native int nativeTagSocket(FileDescriptor fileDescriptor, int i, int i2);

    private static File getDefaultSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    private static File getDefaultBaseDir() {
        File baseDir = new File(getDefaultSystemDir(), "netstats");
        baseDir.mkdirs();
        return baseDir;
    }

    private static Clock getDefaultClock() {
        return new BestClock(ZoneOffset.UTC, new Clock[]{SystemClock.currentNetworkTimeClock(), Clock.systemUTC()});
    }

    public static NetworkStatsService create(Context context, INetworkManagementService networkManager) {
        NetworkStatsService service;
        NetworkStatsService networkStatsService;
        Context context2 = context;
        AlarmManager alarmManager = (AlarmManager) context2.getSystemService("alarm");
        WakeLock wakeLock = ((PowerManager) context2.getSystemService("power")).newWakeLock(1, TAG);
        mHwNetworkStatsService = HwServiceFactory.getHwNetworkStatsService();
        if (mHwNetworkStatsService != null) {
            service = mHwNetworkStatsService.getInstance(context2, networkManager, alarmManager, wakeLock, getDefaultClock(), TelephonyManager.getDefault(), new DefaultNetworkStatsSettings(context2), new NetworkStatsObservers(), getDefaultSystemDir(), getDefaultBaseDir());
        } else {
            networkStatsService = new NetworkStatsService(context2, networkManager, alarmManager, wakeLock, getDefaultClock(), TelephonyManager.getDefault(), new DefaultNetworkStatsSettings(context2), new NetworkStatsObservers(), getDefaultSystemDir(), getDefaultBaseDir());
        }
        networkStatsService = service;
        HandlerThread handlerThread = new HandlerThread(TAG);
        Callback callback = new HandlerCallback(networkStatsService);
        handlerThread.start();
        networkStatsService.setHandler(new Handler(handlerThread.getLooper(), callback), callback);
        return networkStatsService;
    }

    @VisibleForTesting
    NetworkStatsService(Context context, INetworkManagementService networkManager, AlarmManager alarmManager, WakeLock wakeLock, Clock clock, TelephonyManager teleManager, NetworkStatsSettings settings, NetworkStatsObservers statsObservers, File systemDir, File baseDir) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing Context");
        this.mNetworkManager = (INetworkManagementService) Preconditions.checkNotNull(networkManager, "missing INetworkManagementService");
        this.mAlarmManager = (AlarmManager) Preconditions.checkNotNull(alarmManager, "missing AlarmManager");
        this.mTimeRefreshRealtime = -1;
        this.mClock = (Clock) Preconditions.checkNotNull(clock, "missing Clock");
        this.mSettings = (NetworkStatsSettings) Preconditions.checkNotNull(settings, "missing NetworkStatsSettings");
        this.mTeleManager = (TelephonyManager) Preconditions.checkNotNull(teleManager, "missing TelephonyManager");
        this.mWakeLock = (WakeLock) Preconditions.checkNotNull(wakeLock, "missing WakeLock");
        this.mStatsObservers = (NetworkStatsObservers) Preconditions.checkNotNull(statsObservers, "missing NetworkStatsObservers");
        this.mSystemDir = (File) Preconditions.checkNotNull(systemDir, "missing systemDir");
        this.mBaseDir = (File) Preconditions.checkNotNull(baseDir, "missing baseDir");
        this.mUseBpfTrafficStats = new File("/sys/fs/bpf/traffic_uid_stats_map").exists();
        LocalServices.addService(NetworkStatsManagerInternal.class, new NetworkStatsManagerInternalImpl(this, null));
        this.mUidEventThread = new ServiceThread("NetworkStats.uid", -2, false);
        this.mUidEventThread.start();
        this.mUidEventHandler = new Handler(this.mUidEventThread.getLooper(), this.mUidEventHandlerCallback);
        try {
            this.mActivityManager.registerUidObserver(this.mUidObserver, 3, -1, null);
        } catch (RemoteException e) {
            Slog.w(TAG, "ignored;both services live in system_server");
        }
        try {
            List<RunningAppProcessInfo> procInfos = this.mActivityManager.getRunningAppProcesses();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < procInfos.size()) {
                    NetworkStatsRecorder.initeSilent(((RunningAppProcessInfo) procInfos.get(i2)).uid);
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        } catch (RemoteException e2) {
            Slog.w(TAG, "getRunningAppProcesses RemoteException");
        }
    }

    private boolean isUidStateForegroundUL(int state) {
        return state <= 2 || state == 3;
    }

    void handleUidChanged(int uid, int procState, long procStateSeq) {
        try {
            synchronized (this.mUidRulesFirstLock) {
                String[] temp = this.mPMS.getPackagesForUid(uid);
                int len = 0;
                if (temp != null) {
                    len = temp.length;
                }
                for (int i = 0; i < len; i++) {
                    NetworkStatsRecorder.operatorAppwatch(uid, isUidStateForegroundUL(procState));
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "handleUidChanged Exception");
        }
    }

    void handleUidGone(int uid) {
        try {
            synchronized (this.mUidRulesFirstLock) {
                String[] temp = this.mPMS.getPackagesForUid(uid);
                int len = 0;
                if (temp != null) {
                    len = temp.length;
                }
                for (int i = 0; i < len; i++) {
                    NetworkStatsRecorder.operatorAppwatch(uid);
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "handleUidGone Exception");
        }
    }

    @VisibleForTesting
    void setHandler(Handler handler, Callback callback) {
        this.mHandler = handler;
        this.mHandlerCallback = callback;
    }

    public void bindConnectivityManager(IConnectivityManager connManager) {
        this.mConnManager = (IConnectivityManager) Preconditions.checkNotNull(connManager, "missing IConnectivityManager");
    }

    public void systemReady() {
        this.mSystemReady = true;
        if (isBandwidthControlEnabled()) {
            synchronized (this.mStatsLock) {
                this.mDevRecorder = buildRecorder(PREFIX_DEV, this.mSettings.getDevConfig(), false);
                this.mXtRecorder = buildRecorder(PREFIX_XT, this.mSettings.getXtConfig(), false);
                this.mUidRecorder = buildRecorder("uid", this.mSettings.getUidConfig(), false);
                this.mUidTagRecorder = buildRecorder(PREFIX_UID_TAG, this.mSettings.getUidTagConfig(), true);
                hwInitProcRecorder();
                hwInitUidAndProcRecorder();
                updatePersistThresholdsLocked();
                maybeUpgradeLegacyStatsLocked();
                this.mXtStatsCached = this.mXtRecorder.getOrLoadCompleteLocked();
                hwInitProcStatsCollection();
                bootstrapStatsLocked();
            }
            this.mContext.registerReceiver(this.mTetherReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"), null, this.mHandler);
            this.mContext.registerReceiver(this.mPollReceiver, new IntentFilter(ACTION_NETWORK_STATS_POLL), "android.permission.READ_NETWORK_USAGE_HISTORY", this.mHandler);
            this.mContext.registerReceiver(this.mRemovedReceiver, new IntentFilter("android.intent.action.UID_REMOVED"), null, this.mHandler);
            this.mContext.registerReceiver(this.mUserReceiver, new IntentFilter("android.intent.action.USER_REMOVED"), null, this.mHandler);
            this.mContext.registerReceiver(this.mShutdownReceiver, new IntentFilter("android.intent.action.ACTION_SHUTDOWN"));
            try {
                this.mNetworkManager.registerObserver(this.mAlertObserver);
            } catch (RemoteException e) {
            }
            registerPollAlarmLocked();
            registerGlobalAlert();
            return;
        }
        Slog.w(TAG, "bandwidth controls disabled, unable to track stats");
    }

    protected NetworkStatsRecorder buildRecorder(String prefix, Config config, boolean includeTags) {
        DropBoxManager dropBox = (DropBoxManager) this.mContext.getSystemService("dropbox");
        return new NetworkStatsRecorder(new FileRotator(this.mBaseDir, prefix, config.rotateAgeMillis, config.deleteAgeMillis), this.mNonMonotonicObserver, dropBox, prefix, config.bucketDuration, includeTags);
    }

    @GuardedBy("mStatsLock")
    private void shutdownLocked() {
        this.mContext.unregisterReceiver(this.mTetherReceiver);
        this.mContext.unregisterReceiver(this.mPollReceiver);
        this.mContext.unregisterReceiver(this.mRemovedReceiver);
        this.mContext.unregisterReceiver(this.mUserReceiver);
        this.mContext.unregisterReceiver(this.mShutdownReceiver);
        long currentTime = this.mClock.millis();
        this.mDevRecorder.forcePersistLocked(currentTime);
        this.mXtRecorder.forcePersistLocked(currentTime);
        this.mUidRecorder.forcePersistLocked(currentTime);
        this.mUidTagRecorder.forcePersistLocked(currentTime);
        this.mDevRecorder = null;
        this.mXtRecorder = null;
        this.mUidRecorder = null;
        this.mUidTagRecorder = null;
        this.mXtStatsCached = null;
        hwShutdownLocked(currentTime);
        hwShutdownUidAndProcLocked(currentTime);
        this.mSystemReady = false;
    }

    @GuardedBy("mStatsLock")
    private void maybeUpgradeLegacyStatsLocked() {
        try {
            File file = new File(this.mSystemDir, "netstats.bin");
            if (file.exists()) {
                this.mDevRecorder.importLegacyNetworkLocked(file);
                file.delete();
            }
            file = new File(this.mSystemDir, "netstats_xt.bin");
            if (file.exists()) {
                file.delete();
            }
            file = new File(this.mSystemDir, "netstats_uid.bin");
            if (file.exists()) {
                this.mUidRecorder.importLegacyUidLocked(file);
                this.mUidTagRecorder.importLegacyUidLocked(file);
                file.delete();
            }
            hwImportLegacyNetworkLocked();
        } catch (IOException e) {
            Log.wtf(TAG, "problem during legacy upgrade", e);
        } catch (OutOfMemoryError e2) {
            Log.wtf(TAG, "problem during legacy upgrade", e2);
        }
    }

    private void registerPollAlarmLocked() {
        if (this.mPollIntent != null) {
            this.mAlarmManager.cancel(this.mPollIntent);
        }
        this.mPollIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_NETWORK_STATS_POLL), 0);
        this.mAlarmManager.setInexactRepeating(3, SystemClock.elapsedRealtime(), this.mSettings.getPollInterval(), this.mPollIntent);
    }

    private void registerGlobalAlert() {
        try {
            this.mNetworkManager.setGlobalAlert(this.mGlobalAlertBytes);
        } catch (IllegalStateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("problem registering for global alert: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
        } catch (RemoteException e2) {
        }
    }

    public INetworkStatsSession openSession() {
        int callingPid = Binder.getCallingPid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("openSession, pid = ");
        stringBuilder.append(callingPid);
        Slog.i(str, stringBuilder.toString());
        return openSessionInternal(4, null, callingPid);
    }

    public INetworkStatsSession openSessionForUsageStats(int flags, String callingPackage) {
        int callingPid = Binder.getCallingPid();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("openSessionForUsageStats, pid = ");
        stringBuilder.append(callingPid);
        stringBuilder.append(", pkg = ");
        stringBuilder.append(callingPackage);
        Slog.i(str, stringBuilder.toString());
        return openSessionInternal(flags, callingPackage, callingPid);
    }

    private boolean isRateLimitedForPoll(int callingUid) {
        boolean z = false;
        if (callingUid == 1000) {
            return false;
        }
        long lastCallTime;
        long now = SystemClock.elapsedRealtime();
        synchronized (this.mOpenSessionCallsPerUid) {
            this.mOpenSessionCallsPerUid.put(callingUid, this.mOpenSessionCallsPerUid.get(callingUid, 0) + 1);
            lastCallTime = this.mLastStatsSessionPoll;
            this.mLastStatsSessionPoll = now;
        }
        if (now - lastCallTime < POLL_RATE_LIMIT_MS) {
            z = true;
        }
        return z;
    }

    private INetworkStatsSession openSessionInternal(int flags, final String callingPackage, int callingPid) {
        int usedFlags;
        assertBandwidthControlEnabled();
        final int callingUid = Binder.getCallingUid();
        if (isRateLimitedForPoll(callingUid)) {
            usedFlags = flags & -2;
        } else {
            usedFlags = flags;
        }
        if ((usedFlags & 3) != 0) {
            long ident = Binder.clearCallingIdentity();
            try {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("openSessionInternal,pid = ");
                stringBuilder.append(callingPid);
                Slog.i(str, stringBuilder.toString());
                if (USER_TYPE == 3 && callingPid == MY_PID) {
                    this.mUpdateStatsCount++;
                    if (this.mUpdateStatsCount >= 10) {
                        Slog.w(TAG, new Exception("Update network status too often"));
                        this.mUpdateStatsCount = 0;
                    }
                    performPoll(3);
                } else {
                    this.mUpdateStatsCount = 0;
                    try {
                        performPoll(3);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            } catch (Exception e) {
                Slog.w(TAG, "print log error");
            }
        }
        return new INetworkStatsSession.Stub() {
            private final int mAccessLevel = NetworkStatsService.this.checkAccessLevel(callingPackage);
            private final String mCallingPackage = callingPackage;
            private final int mCallingUid = callingUid;
            private NetworkStatsCollection mUidAndProcComplete;
            private NetworkStatsCollection mUidComplete;
            private NetworkStatsCollection mUidTagComplete;

            private NetworkStatsCollection getUidComplete() {
                NetworkStatsCollection networkStatsCollection;
                synchronized (NetworkStatsService.this.mStatsLock) {
                    if (this.mUidComplete == null) {
                        this.mUidComplete = NetworkStatsService.this.mUidRecorder.getOrLoadCompleteLocked();
                    }
                    networkStatsCollection = this.mUidComplete;
                }
                return networkStatsCollection;
            }

            private NetworkStatsCollection getUidTagComplete() {
                NetworkStatsCollection networkStatsCollection;
                synchronized (NetworkStatsService.this.mStatsLock) {
                    if (this.mUidTagComplete == null) {
                        this.mUidTagComplete = NetworkStatsService.this.mUidTagRecorder.getOrLoadCompleteLocked();
                    }
                    networkStatsCollection = this.mUidTagComplete;
                }
                return networkStatsCollection;
            }

            private NetworkStatsCollection getUidAndProcComplete() {
                NetworkStatsCollection networkStatsCollection;
                synchronized (NetworkStatsService.this.mStatsLock) {
                    if (this.mUidAndProcComplete == null && NetworkStatsService.this.getUidAndProcNetworkStatsRecorder() != null) {
                        this.mUidAndProcComplete = NetworkStatsService.this.getUidAndProcNetworkStatsRecorder().getOrLoadCompleteLocked();
                    }
                    networkStatsCollection = this.mUidAndProcComplete;
                }
                return networkStatsCollection;
            }

            public int[] getRelevantUids() {
                return getUidComplete().getRelevantUids(this.mAccessLevel);
            }

            public NetworkStats getDeviceSummaryForNetwork(NetworkTemplate template, long start, long end) {
                return NetworkStatsService.this.internalGetSummaryForNetwork(template, usedFlags, start, end, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStats getSummaryForNetwork(NetworkTemplate template, long start, long end) {
                return NetworkStatsService.this.internalGetSummaryForNetwork(template, usedFlags, start, end, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStatsHistory getHistoryForNetwork(NetworkTemplate template, int fields) {
                return NetworkStatsService.this.internalGetHistoryForNetwork(template, usedFlags, fields, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStats getSummaryForAllUid(NetworkTemplate template, long start, long end, boolean includeTags) {
                NetworkStats stats = new NetworkStats(0, 0);
                try {
                    stats = getUidComplete().getSummary(template, start, end, this.mAccessLevel, this.mCallingUid);
                    if (includeTags) {
                        stats.combineAllValues(getUidTagComplete().getSummary(template, start, end, this.mAccessLevel, this.mCallingUid));
                    }
                    return stats;
                } catch (NullPointerException e) {
                    if (NetworkStatsService.this.mSystemReady) {
                        Slog.wtf(NetworkStatsService.TAG, "NullPointerException in getSummaryForAllUid", e);
                        throw e;
                    }
                    Slog.wtf(NetworkStatsService.TAG, "NullPointerException when the system is shutting down", e);
                    return stats;
                }
            }

            public NetworkStatsHistory getHistoryForUid(NetworkTemplate template, int uid, int set, int tag, int fields) {
                if (tag == 0) {
                    return getUidComplete().getHistory(template, null, uid, set, tag, fields, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, this.mAccessLevel, this.mCallingUid);
                }
                return getUidTagComplete().getHistory(template, null, uid, set, tag, fields, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, this.mAccessLevel, this.mCallingUid);
            }

            public NetworkStatsHistory getHistoryIntervalForUid(NetworkTemplate template, int uid, int set, int tag, int fields, long start, long end) {
                if (tag == 0) {
                    return getUidComplete().getHistory(template, null, uid, set, tag, fields, start, end, this.mAccessLevel, this.mCallingUid);
                }
                int i = uid;
                if (i == Binder.getCallingUid()) {
                    return getUidTagComplete().getHistory(template, null, i, set, tag, fields, start, end, this.mAccessLevel, this.mCallingUid);
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Calling package ");
                stringBuilder.append(this.mCallingPackage);
                stringBuilder.append(" cannot access tag information from a different uid");
                throw new SecurityException(stringBuilder.toString());
            }

            public NetworkStats getSummaryForAllPid(NetworkTemplate template, long start, long end) {
                NetworkStats stats = getUidAndProcComplete().getSummary(template, start, end, NetworkStatsService.this.checkAccessLevel(this.mCallingPackage), this.mCallingUid);
                String str = NetworkStatsService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getSummaryForAllPid stats size= ");
                stringBuilder.append(stats.size());
                Slog.i(str, stringBuilder.toString());
                return stats;
            }

            public void close() {
                this.mUidComplete = null;
                this.mUidTagComplete = null;
                this.mUidAndProcComplete = null;
            }
        };
    }

    private int checkAccessLevel(String callingPackage) {
        return NetworkStatsAccess.checkAccessLevel(this.mContext, Binder.getCallingUid(), callingPackage);
    }

    private SubscriptionPlan resolveSubscriptionPlan(NetworkTemplate template, int flags) {
        SubscriptionPlan plan = null;
        if ((flags & 4) != 0 && this.mSettings.getAugmentEnabled()) {
            if (LOGD) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Resolving plan for ");
                stringBuilder.append(template);
                Slog.d(str, stringBuilder.toString());
            }
            long token = Binder.clearCallingIdentity();
            try {
                SubscriptionPlan subscriptionPlan = ((NetworkPolicyManagerInternal) LocalServices.getService(NetworkPolicyManagerInternal.class)).getSubscriptionPlan(template);
                plan = subscriptionPlan;
                if (subscriptionPlan != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Resolved to plan ");
                    stringBuilder2.append(plan);
                    Slog.d(str2, stringBuilder2.toString());
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        return plan;
    }

    private NetworkStats internalGetSummaryForNetwork(NetworkTemplate template, int flags, long start, long end, int accessLevel, int callingUid) {
        Entry entry = internalGetHistoryForNetwork(template, flags, -1, accessLevel, callingUid).getValues(start, end, System.currentTimeMillis(), null);
        NetworkStats stats = new NetworkStats(end - start, 1);
        String str = NetworkStats.IFACE_ALL;
        long j = entry.rxBytes;
        long j2 = entry.rxPackets;
        long j3 = j2;
        NetworkStats.Entry entry2 = r9;
        NetworkStats.Entry entry3 = new NetworkStats.Entry(str, -1, -1, 0, -1, -1, -1, j, j3, entry.txBytes, entry.txPackets, entry.operations);
        stats.addValues(entry2);
        return stats;
    }

    private NetworkStatsHistory internalGetHistoryForNetwork(NetworkTemplate template, int flags, int fields, int accessLevel, int callingUid) {
        Throwable th;
        SubscriptionPlan augmentPlan = resolveSubscriptionPlan(template, flags);
        Object obj = this.mStatsLock;
        synchronized (obj) {
            Object obj2;
            try {
                if (this.mXtStatsCached == null) {
                } else {
                    obj2 = obj;
                    NetworkStatsHistory history = this.mXtStatsCached.getHistory(template, augmentPlan, -1, -1, 0, fields, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, accessLevel, callingUid);
                    return history;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
        return null;
    }

    private long getNetworkTotalBytes(NetworkTemplate template, long start, long end) {
        assertSystemReady();
        assertBandwidthControlEnabled();
        return internalGetSummaryForNetwork(template, 4, start, end, 3, Binder.getCallingUid()).getTotalBytes();
    }

    private NetworkStats getNetworkUidBytes(NetworkTemplate template, long start, long end) {
        NetworkStatsCollection uidComplete;
        assertSystemReady();
        assertBandwidthControlEnabled();
        synchronized (this.mStatsLock) {
            uidComplete = this.mUidRecorder.getOrLoadCompleteLocked();
        }
        return uidComplete.getSummary(template, start, end, 3, 1000);
    }

    public NetworkStats getDataLayerSnapshotForUid(int uid) throws RemoteException {
        if (Binder.getCallingUid() != uid) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_NETWORK_STATE", TAG);
        }
        assertBandwidthControlEnabled();
        long token = Binder.clearCallingIdentity();
        try {
            NetworkStats networkLayer = this.mNetworkManager.getNetworkStatsUidDetail(uid, NetworkStats.INTERFACES_ALL);
            networkLayer.spliceOperationsFrom(this.mUidOperations);
            NetworkStats dataLayer = new NetworkStats(networkLayer.getElapsedRealtime(), networkLayer.size());
            NetworkStats.Entry entry = null;
            for (int i = 0; i < networkLayer.size(); i++) {
                entry = networkLayer.getValues(i, entry);
                entry.iface = NetworkStats.IFACE_ALL;
                dataLayer.combineValues(entry);
            }
            return dataLayer;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public NetworkStats getDetailedUidStats(String[] requiredIfaces) {
        try {
            return getNetworkStatsUidDetail(NetworkStatsFactory.augmentWithStackedInterfaces(requiredIfaces));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Error compiling UID stats", e);
            return new NetworkStats(0, 0);
        }
    }

    public String[] getMobileIfaces() {
        return this.mMobileIfaces;
    }

    public void incrementOperationCount(int uid, int tag, int operationCount) {
        Throwable th;
        int i = uid;
        int i2 = operationCount;
        if (Binder.getCallingUid() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", TAG);
        }
        int i3;
        if (i2 < 0) {
            i3 = i2;
            throw new IllegalArgumentException("operation count can only be incremented");
        } else if (tag != 0) {
            Object obj = this.mStatsLock;
            synchronized (obj) {
                Object obj2;
                try {
                    int set = this.mActiveUidCounterSet.get(i, 0);
                    obj2 = obj;
                    try {
                        this.mUidOperations.combineValues(this.mActiveIface, i, set, tag, 0, 0, 0, 0, (long) i2);
                        this.mUidOperations.combineValues(this.mActiveIface, uid, set, 0, 0, 0, 0, 0, (long) operationCount);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    i3 = i2;
                    obj2 = obj;
                    throw th;
                }
            }
        } else {
            i3 = i2;
            throw new IllegalArgumentException("operation count must have specific tag");
        }
    }

    @VisibleForTesting
    void setUidForeground(int uid, boolean uidForeground) {
        synchronized (this.mStatsLock) {
            boolean set = uidForeground;
            if (this.mActiveUidCounterSet.get(uid, 0) != set) {
                this.mActiveUidCounterSet.put(uid, set);
                NetworkManagementSocketTagger.setKernelCounterSet(uid, set);
            }
        }
    }

    public void forceUpdateIfaces(Network[] defaultNetworks) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("forceUpdateIfaces, pid = ");
        stringBuilder.append(Binder.getCallingPid());
        Slog.i(str, stringBuilder.toString());
        long token = Binder.clearCallingIdentity();
        try {
            updateIfaces(defaultNetworks);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void forceUpdate() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("forceUpdate, pid = ");
        stringBuilder.append(Binder.getCallingPid());
        Slog.i(str, stringBuilder.toString());
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_NETWORK_USAGE_HISTORY", TAG);
        assertBandwidthControlEnabled();
        long token = Binder.clearCallingIdentity();
        try {
            performPoll(3);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void advisePersistThreshold(long thresholdBytes) {
        assertBandwidthControlEnabled();
        this.mPersistThreshold = MathUtils.constrain(thresholdBytes, 131072, 2097152);
        if (LOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("advisePersistThreshold() given ");
            stringBuilder.append(thresholdBytes);
            stringBuilder.append(", clamped to ");
            stringBuilder.append(this.mPersistThreshold);
            Slog.v(str, stringBuilder.toString());
        }
        long currentTime = this.mClock.millis();
        synchronized (this.mStatsLock) {
            if (this.mSystemReady) {
                updatePersistThresholdsLocked();
                this.mDevRecorder.maybePersistLocked(currentTime);
                this.mXtRecorder.maybePersistLocked(currentTime);
                this.mUidRecorder.maybePersistLocked(currentTime);
                this.mUidTagRecorder.maybePersistLocked(currentTime);
                hwMaybePersistLocked(currentTime);
                hwMaybeUidAndProcPersistLocked(currentTime);
                registerGlobalAlert();
                return;
            }
        }
    }

    public DataUsageRequest registerUsageCallback(String callingPackage, DataUsageRequest request, Messenger messenger, IBinder binder) {
        Preconditions.checkNotNull(callingPackage, "calling package is null");
        Preconditions.checkNotNull(request, "DataUsageRequest is null");
        Preconditions.checkNotNull(request.template, "NetworkTemplate is null");
        Preconditions.checkNotNull(messenger, "messenger is null");
        Preconditions.checkNotNull(binder, "binder is null");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("registerUsageCallback, pid = ");
        stringBuilder.append(Binder.getCallingPid());
        Slog.i(str, stringBuilder.toString());
        int callingUid = Binder.getCallingUid();
        int accessLevel = checkAccessLevel(callingPackage);
        long token = Binder.clearCallingIdentity();
        try {
            DataUsageRequest normalizedRequest = this.mStatsObservers.register(request, messenger, binder, callingUid, accessLevel);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, Integer.valueOf(3)));
            return normalizedRequest;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void unregisterUsageRequest(DataUsageRequest request) {
        Preconditions.checkNotNull(request, "DataUsageRequest is null");
        int callingUid = Binder.getCallingUid();
        long token = Binder.clearCallingIdentity();
        try {
            this.mStatsObservers.unregister(request, callingUid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public long getUidStats(int uid, int type) {
        return nativeGetUidStat(uid, type, checkBpfStatsEnable());
    }

    public long getIfaceStats(String iface, int type) {
        boolean bpfStatsEnable = checkBpfStatsEnable();
        long ret = nativeGetIfaceStat(iface, type, bpfStatsEnable);
        if (!bpfStatsEnable || -1 != ret) {
            return ret;
        }
        if (TYPE_TCP_TX_PACKETS != type && TYPE_TCP_RX_PACKETS != type) {
            return ret;
        }
        ret = nativeGetIfaceStat(iface, type, false);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getIfaceStats ");
        stringBuilder.append(iface);
        stringBuilder.append(" type ");
        stringBuilder.append(type);
        stringBuilder.append(" failed, return qtaguid stat ");
        stringBuilder.append(ret);
        Log.e(str, stringBuilder.toString());
        return ret;
    }

    public long getTotalStats(int type) {
        return nativeGetTotalStat(type, checkBpfStatsEnable());
    }

    private boolean checkBpfStatsEnable() {
        return this.mUseBpfTrafficStats;
    }

    public int tagSocket(ParcelFileDescriptor pfd, int tag, int uid) {
        int ret = -1;
        if (this.mContext.checkCallingPermission("android.permission.UPDATE_DEVICE_STATS") == 0) {
            ret = nativeTagSocket(pfd.getFileDescriptor(), tag, uid);
        }
        try {
            pfd.close();
            return ret;
        } catch (IOException e) {
            Slog.w(TAG, "Closed duplicate socket fd failed");
            return -1;
        }
    }

    @GuardedBy("mStatsLock")
    private void updatePersistThresholdsLocked() {
        this.mDevRecorder.setPersistThreshold(this.mSettings.getDevPersistBytes(this.mPersistThreshold));
        this.mXtRecorder.setPersistThreshold(this.mSettings.getXtPersistBytes(this.mPersistThreshold));
        this.mUidRecorder.setPersistThreshold(this.mSettings.getUidPersistBytes(this.mPersistThreshold));
        this.mUidTagRecorder.setPersistThreshold(this.mSettings.getUidTagPersistBytes(this.mPersistThreshold));
        hwUpdateProcPersistThresholds(this.mPersistThreshold);
        hwUpdateProcPersistThresholds(this.mPersistThreshold);
        this.mGlobalAlertBytes = this.mSettings.getGlobalAlertBytes(this.mPersistThreshold);
    }

    private void updateIfaces(Network[] defaultNetworks) {
        synchronized (this.mStatsLock) {
            this.mWakeLock.acquire();
            try {
                updateIfacesLocked(defaultNetworks);
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void updateIfacesLocked(Network[] defaultNetworks) {
        Network[] networkArr = defaultNetworks;
        if (this.mSystemReady) {
            if (LOGV) {
                Slog.v(TAG, "updateIfacesLocked()");
            }
            performPollLocked(1);
            try {
                NetworkState[] states = this.mConnManager.getAllNetworkState();
                LinkProperties activeLink = this.mConnManager.getActiveLinkProperties();
                this.mActiveIface = activeLink != null ? activeLink.getInterfaceName() : null;
                this.mActiveIfaces.clear();
                this.mActiveUidIfaces.clear();
                if (networkArr != null) {
                    this.mDefaultNetworks = networkArr;
                }
                ArraySet<String> mobileIfaces = new ArraySet();
                int length = states.length;
                int i = 0;
                while (i < length) {
                    NetworkState state = states[i];
                    if (state.networkInfo.isConnected()) {
                        boolean isMobile = ConnectivityManager.isNetworkTypeMobile(state.networkInfo.getType());
                        NetworkIdentity ident = NetworkIdentity.buildNetworkIdentity(this.mContext, state, ArrayUtils.contains(this.mDefaultNetworks, state.network));
                        String baseIface = state.linkProperties.getInterfaceName();
                        if (baseIface != null) {
                            findOrCreateNetworkIdentitySet(this.mActiveIfaces, baseIface).add(ident);
                            findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, baseIface).add(ident);
                            if (state.networkCapabilities.hasCapability(4) && !ident.getMetered()) {
                                NetworkIdentity networkIdentity = new NetworkIdentity(ident.getType(), ident.getSubType(), ident.getSubscriberId(), ident.getNetworkId(), ident.getRoaming(), true, true);
                                findOrCreateNetworkIdentitySet(this.mActiveIfaces, VT_INTERFACE).add(networkIdentity);
                                findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, VT_INTERFACE).add(networkIdentity);
                            }
                            if (isMobile) {
                                mobileIfaces.add(baseIface);
                            }
                        }
                        for (LinkProperties stackedLink : state.linkProperties.getStackedLinks()) {
                            NetworkState[] states2;
                            String stackedIface = stackedLink.getInterfaceName();
                            if (stackedIface != null) {
                                states2 = states;
                                findOrCreateNetworkIdentitySet(this.mActiveUidIfaces, stackedIface).add(ident);
                                if (isMobile) {
                                    mobileIfaces.add(stackedIface);
                                }
                                NetworkStatsFactory.noteStackedIface(stackedIface, baseIface);
                            } else {
                                states2 = states;
                            }
                            states = states2;
                        }
                    }
                    i++;
                    states = states;
                }
                this.mMobileIfaces = (String[]) mobileIfaces.toArray(new String[mobileIfaces.size()]);
            } catch (RemoteException e) {
            }
        }
    }

    private static <K> NetworkIdentitySet findOrCreateNetworkIdentitySet(ArrayMap<K, NetworkIdentitySet> map, K key) {
        NetworkIdentitySet ident = (NetworkIdentitySet) map.get(key);
        if (ident != null) {
            return ident;
        }
        ident = new NetworkIdentitySet();
        map.put(key, ident);
        return ident;
    }

    @GuardedBy("mStatsLock")
    private void recordSnapshotLocked(long currentTime) throws RemoteException {
        Trace.traceBegin(2097152, "snapshotUid");
        NetworkStats uidSnapshot = getNetworkStatsUidDetail(NetworkStats.INTERFACES_ALL);
        Trace.traceEnd(2097152);
        Trace.traceBegin(2097152, "snapshotXt");
        NetworkStats xtSnapshot = getNetworkStatsXt();
        Trace.traceEnd(2097152);
        Trace.traceBegin(2097152, "snapshotDev");
        NetworkStats devSnapshot = this.mNetworkManager.getNetworkStatsSummaryDev();
        Trace.traceEnd(2097152);
        Trace.traceBegin(2097152, "snapshotTether");
        NetworkStats tetherSnapshot = getNetworkStatsTethering(0);
        Trace.traceEnd(2097152);
        xtSnapshot.combineAllValues(tetherSnapshot);
        devSnapshot.combineAllValues(tetherSnapshot);
        if (HuaweiTelephonyConfigs.isQcomPlatform()) {
            NetPluginDelegate.getTetherStats(uidSnapshot, xtSnapshot, devSnapshot);
        }
        Trace.traceBegin(2097152, "recordDev");
        long j = currentTime;
        this.mDevRecorder.recordSnapshotLocked(devSnapshot, this.mActiveIfaces, null, j);
        Trace.traceEnd(2097152);
        Trace.traceBegin(2097152, "recordXt");
        this.mXtRecorder.recordSnapshotLocked(xtSnapshot, this.mActiveIfaces, null, j);
        Trace.traceEnd(2097152);
        VpnInfo[] vpnArray = this.mConnManager.getAllVpnInfo();
        Trace.traceBegin(2097152, "recordUid");
        NetworkStats networkStats = uidSnapshot;
        VpnInfo[] vpnInfoArr = vpnArray;
        long j2 = currentTime;
        this.mUidRecorder.recordSnapshotLocked(networkStats, this.mActiveUidIfaces, vpnInfoArr, j2);
        Trace.traceEnd(2097152);
        Trace.traceBegin(2097152, "recordUidTag");
        this.mUidTagRecorder.recordSnapshotLocked(networkStats, this.mActiveUidIfaces, vpnInfoArr, j2);
        hwRecordSnapshotLocked(currentTime);
        hwRecordUidAndProcSnapshotLocked(currentTime);
        Trace.traceEnd(2097152);
        this.mStatsObservers.updateStats(xtSnapshot, uidSnapshot, new ArrayMap(this.mActiveIfaces), new ArrayMap(this.mActiveUidIfaces), vpnArray, currentTime);
    }

    @GuardedBy("mStatsLock")
    private void bootstrapStatsLocked() {
        try {
            recordSnapshotLocked(this.mClock.millis());
        } catch (IllegalStateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("problem reading network stats: ");
            stringBuilder.append(e);
            Slog.w(str, stringBuilder.toString());
        } catch (RemoteException e2) {
        }
    }

    private void performPoll(int flags) {
        synchronized (this.mStatsLock) {
            this.mWakeLock.acquire();
            try {
                performPollLocked(flags);
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void performPollLocked(int flags) {
        if (this.mSystemReady) {
            String str;
            if (LOGV) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("performPollLocked(flags=0x");
                stringBuilder.append(Integer.toHexString(flags));
                stringBuilder.append(")");
                Slog.v(str, stringBuilder.toString());
            }
            Trace.traceBegin(2097152, "performPollLocked");
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("performPollLocked, pid = ");
            stringBuilder2.append(Binder.getCallingPid());
            Slog.i(str, stringBuilder2.toString());
            boolean persistForce = false;
            boolean persistNetwork = (flags & 1) != 0;
            boolean persistUid = (flags & 2) != 0;
            if ((flags & 256) != 0) {
                persistForce = true;
            }
            long currentTime = this.mClock.millis();
            try {
                recordSnapshotLocked(currentTime);
                Trace.traceBegin(2097152, "[persisting]");
                if (persistForce) {
                    this.mDevRecorder.forcePersistLocked(currentTime);
                    this.mXtRecorder.forcePersistLocked(currentTime);
                    this.mUidRecorder.forcePersistLocked(currentTime);
                    this.mUidTagRecorder.forcePersistLocked(currentTime);
                    hwForcePersistLocked(currentTime);
                    hwForceUidAndProcPersistLocked(currentTime);
                } else {
                    if (persistNetwork) {
                        this.mDevRecorder.maybePersistLocked(currentTime);
                        this.mXtRecorder.maybePersistLocked(currentTime);
                    }
                    if (persistUid) {
                        this.mUidRecorder.maybePersistLocked(currentTime);
                        this.mUidTagRecorder.maybePersistLocked(currentTime);
                        hwMaybePersistLocked(currentTime);
                        hwMaybeUidAndProcPersistLocked(currentTime);
                    }
                }
                Trace.traceEnd(2097152);
                if (this.mSettings.getSampleEnabled()) {
                    performSampleLocked();
                }
                Intent updatedIntent = new Intent(ACTION_NETWORK_STATS_UPDATED);
                updatedIntent.setFlags(1073741824);
                this.mContext.sendBroadcastAsUser(updatedIntent, UserHandle.ALL, "android.permission.READ_NETWORK_USAGE_HISTORY");
                Trace.traceEnd(2097152);
            } catch (IllegalStateException e) {
                Log.wtf(TAG, "problem reading network stats", e);
            } catch (RemoteException e2) {
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void performSampleLocked() {
        long currentTime = this.mClock.millis();
        NetworkTemplate template = NetworkTemplate.buildTemplateMobileWildcard();
        NetworkStats.Entry devTotal = this.mDevRecorder.getTotalSinceBootLocked(template);
        NetworkStats.Entry xtTotal = this.mXtRecorder.getTotalSinceBootLocked(template);
        NetworkStats.Entry uidTotal = this.mUidRecorder.getTotalSinceBootLocked(template);
        long j = currentTime;
        EventLogTags.writeNetstatsMobileSample(devTotal.rxBytes, devTotal.rxPackets, devTotal.txBytes, devTotal.txPackets, xtTotal.rxBytes, xtTotal.rxPackets, xtTotal.txBytes, xtTotal.txPackets, uidTotal.rxBytes, uidTotal.rxPackets, uidTotal.txBytes, uidTotal.txPackets, j);
        NetworkTemplate template2 = NetworkTemplate.buildTemplateWifiWildcard();
        devTotal = this.mDevRecorder.getTotalSinceBootLocked(template2);
        xtTotal = this.mXtRecorder.getTotalSinceBootLocked(template2);
        uidTotal = this.mUidRecorder.getTotalSinceBootLocked(template2);
        EventLogTags.writeNetstatsWifiSample(devTotal.rxBytes, devTotal.rxPackets, devTotal.txBytes, devTotal.txPackets, xtTotal.rxBytes, xtTotal.rxPackets, xtTotal.txBytes, xtTotal.txPackets, uidTotal.rxBytes, uidTotal.rxPackets, uidTotal.txBytes, uidTotal.txPackets, j);
    }

    @GuardedBy("mStatsLock")
    private void removeUidsLocked(int... uids) {
        if (LOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeUidsLocked() for UIDs ");
            stringBuilder.append(Arrays.toString(uids));
            Slog.v(str, stringBuilder.toString());
        }
        performPollLocked(3);
        if (this.mUidRecorder != null) {
            this.mUidRecorder.removeUidsLocked(uids);
        }
        if (this.mUidTagRecorder != null) {
            this.mUidTagRecorder.removeUidsLocked(uids);
        }
        for (int uid : uids) {
            NetworkManagementSocketTagger.resetKernelUidStats(uid);
        }
    }

    @GuardedBy("mStatsLock")
    private void removeUserLocked(int userId) {
        if (LOGV) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeUserLocked() for userId=");
            stringBuilder.append(userId);
            Slog.v(str, stringBuilder.toString());
        }
        int[] uids = new int[null];
        for (ApplicationInfo app : this.mContext.getPackageManager().getInstalledApplications(4194816)) {
            uids = ArrayUtils.appendInt(uids, UserHandle.getUid(userId, app.uid));
        }
        removeUidsLocked(uids);
    }

    protected void dump(FileDescriptor fd, PrintWriter rawWriter, String[] args) {
        Throwable i;
        Object obj;
        PrintWriter printWriter = rawWriter;
        String[] strArr = args;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            int duration;
            int i2;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dump, pid = ");
            stringBuilder.append(Binder.getCallingPid());
            Slog.i(str, stringBuilder.toString());
            HashSet<String> argSet = new HashSet();
            long duration2 = 86400000;
            for (String arg : strArr) {
                argSet.add(arg);
                if (arg.startsWith("--duration=")) {
                    try {
                        duration2 = Long.parseLong(arg.substring(11));
                    } catch (NumberFormatException e) {
                    }
                }
            }
            boolean z = true;
            boolean z2 = argSet.contains("--poll") || argSet.contains("poll");
            boolean poll = z2;
            boolean checkin = argSet.contains("--checkin");
            z2 = argSet.contains("--full") || argSet.contains("full");
            boolean fullHistory = z2;
            z2 = argSet.contains("--uid") || argSet.contains("detail");
            boolean includeUid = z2;
            if (!(argSet.contains("--tag") || argSet.contains("detail"))) {
                z = false;
            }
            boolean includeTag = z;
            IndentingPrintWriter pw = new IndentingPrintWriter(printWriter, "  ");
            Object obj2 = this.mStatsLock;
            synchronized (obj2) {
                IndentingPrintWriter pw2;
                HashSet<String> hashSet;
                long j;
                try {
                    if (strArr.length > 0) {
                        try {
                            if (PriorityDump.PROTO_ARG.equals(strArr[0])) {
                                dumpProtoLocked(fd);
                                return;
                            }
                        } catch (Throwable th) {
                            i = th;
                            obj = obj2;
                            throw i;
                        }
                    }
                    if (poll) {
                        performPollLocked(LightsManager.LIGHT_ID_MANUALCUSTOMBACKLIGHT);
                        pw.println("Forced poll");
                    } else if (checkin) {
                        try {
                            long end = System.currentTimeMillis();
                            long start = end - duration2;
                            pw.print("v1,");
                            pw.print(start / 1000);
                            pw.print(',');
                            pw.print(end / 1000);
                            pw.println();
                            pw.println(PREFIX_XT);
                            pw2 = pw;
                            obj = obj2;
                            try {
                                this.mXtRecorder.dumpCheckin(printWriter, start, end);
                                if (includeUid) {
                                    pw2.println("uid");
                                    this.mUidRecorder.dumpCheckin(printWriter, start, end);
                                }
                                if (includeTag) {
                                    pw2.println("tag");
                                    this.mUidTagRecorder.dumpCheckin(printWriter, start, end);
                                }
                            } catch (Throwable th2) {
                                i = th2;
                                throw i;
                            }
                        } catch (Throwable th3) {
                            i = th3;
                            pw2 = pw;
                            obj = obj2;
                            hashSet = argSet;
                            j = duration2;
                            throw i;
                        }
                    } else {
                        pw2 = pw;
                        obj = obj2;
                        try {
                            int i3;
                            SparseIntArray calls;
                            pw2.println("Active interfaces:");
                            pw2.increaseIndent();
                            for (i3 = 0; i3 < this.mActiveIfaces.size(); i3++) {
                                pw2.printPair("iface", this.mActiveIfaces.keyAt(i3));
                                pw2.printPair("ident", this.mActiveIfaces.valueAt(i3));
                                pw2.println();
                            }
                            pw2.decreaseIndent();
                            pw2.println("Active UID interfaces:");
                            pw2.increaseIndent();
                            for (i3 = 0; i3 < this.mActiveUidIfaces.size(); i3++) {
                                pw2.printPair("iface", this.mActiveUidIfaces.keyAt(i3));
                                pw2.printPair("ident", this.mActiveUidIfaces.valueAt(i3));
                                pw2.println();
                            }
                            pw2.decreaseIndent();
                            synchronized (this.mOpenSessionCallsPerUid) {
                                try {
                                    calls = this.mOpenSessionCallsPerUid.clone();
                                } catch (Throwable th4) {
                                    i = th4;
                                    throw i;
                                }
                            }
                            duration = calls.size();
                            long[] values = new long[duration];
                            i2 = 0;
                            while (i2 < duration) {
                                hashSet = argSet;
                                j = duration2;
                                values[i2] = (((long) calls.valueAt(i2)) << 32) | ((long) calls.keyAt(i2));
                                i2++;
                                argSet = hashSet;
                                duration2 = j;
                                printWriter = rawWriter;
                            }
                            j = duration2;
                            Arrays.sort(values);
                            pw2.println("Top openSession callers (uid=count):");
                            pw2.increaseIndent();
                            i2 = Math.max(0, duration - 20);
                            for (int j2 = duration - 1; j2 >= i2; j2--) {
                                int count = (int) (values[j2] >> 32);
                                pw2.print((int) (values[j2] & -1));
                                pw2.print("=");
                                pw2.println(count);
                            }
                            pw2.decreaseIndent();
                            pw2.println();
                            pw2.println("Dev stats:");
                            pw2.increaseIndent();
                            this.mDevRecorder.dumpLocked(pw2, fullHistory);
                            pw2.decreaseIndent();
                            pw2.println("Xt stats:");
                            pw2.increaseIndent();
                            this.mXtRecorder.dumpLocked(pw2, fullHistory);
                            pw2.decreaseIndent();
                            if (includeUid) {
                                pw2.println("UID stats:");
                                pw2.increaseIndent();
                                this.mUidRecorder.dumpLocked(pw2, fullHistory);
                                pw2.decreaseIndent();
                            }
                            if (includeTag) {
                                pw2.println("UID tag stats:");
                                pw2.increaseIndent();
                                this.mUidTagRecorder.dumpLocked(pw2, fullHistory);
                                pw2.decreaseIndent();
                            }
                        } catch (Throwable th5) {
                            i = th5;
                            hashSet = argSet;
                            j = duration2;
                            throw i;
                        }
                    }
                } catch (Throwable th6) {
                    i = th6;
                    pw2 = pw;
                    obj = obj2;
                    hashSet = argSet;
                    j = duration2;
                    throw i;
                }
            }
        }
    }

    @GuardedBy("mStatsLock")
    private void dumpProtoLocked(FileDescriptor fd) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        dumpInterfaces(proto, 2246267895809L, this.mActiveIfaces);
        dumpInterfaces(proto, 2246267895810L, this.mActiveUidIfaces);
        this.mDevRecorder.writeToProtoLocked(proto, 1146756268035L);
        this.mXtRecorder.writeToProtoLocked(proto, 1146756268036L);
        this.mUidRecorder.writeToProtoLocked(proto, 1146756268037L);
        this.mUidTagRecorder.writeToProtoLocked(proto, 1146756268038L);
        proto.flush();
    }

    private static void dumpInterfaces(ProtoOutputStream proto, long tag, ArrayMap<String, NetworkIdentitySet> ifaces) {
        for (int i = 0; i < ifaces.size(); i++) {
            long start = proto.start(tag);
            proto.write(1138166333441L, (String) ifaces.keyAt(i));
            ((NetworkIdentitySet) ifaces.valueAt(i)).writeToProto(proto, 1146756268034L);
            proto.end(start);
        }
    }

    private NetworkStats getNetworkStatsUidDetail(String[] ifaces) throws RemoteException {
        NetworkStats uidSnapshot = this.mNetworkManager.getNetworkStatsUidDetail(-1, ifaces);
        NetworkStats tetherSnapshot = getNetworkStatsTethering(1);
        tetherSnapshot.filter(-1, ifaces, -1);
        NetworkStatsFactory.apply464xlatAdjustments(uidSnapshot, tetherSnapshot);
        uidSnapshot.combineAllValues(tetherSnapshot);
        NetworkStats vtStats = ((TelephonyManager) this.mContext.getSystemService("phone")).getVtDataUsage(1);
        if (vtStats != null) {
            vtStats.filter(-1, ifaces, -1);
            NetworkStatsFactory.apply464xlatAdjustments(uidSnapshot, vtStats);
            uidSnapshot.combineAllValues(vtStats);
        }
        uidSnapshot.combineAllValues(this.mUidOperations);
        return uidSnapshot;
    }

    private NetworkStats getNetworkStatsXt() throws RemoteException {
        NetworkStats xtSnapshot = this.mNetworkManager.getNetworkStatsSummaryXt();
        NetworkStats vtSnapshot = ((TelephonyManager) this.mContext.getSystemService("phone")).getVtDataUsage(null);
        if (vtSnapshot != null) {
            xtSnapshot.combineAllValues(vtSnapshot);
        }
        return xtSnapshot;
    }

    private NetworkStats getNetworkStatsTethering(int how) throws RemoteException {
        try {
            return this.mNetworkManager.getNetworkStatsTethering(how);
        } catch (IllegalStateException e) {
            Log.wtf(TAG, "problem reading network stats", e);
            return new NetworkStats(0, 10);
        }
    }

    private void assertSystemReady() {
        if (!this.mSystemReady) {
            throw new IllegalStateException("System not ready");
        }
    }

    private void assertBandwidthControlEnabled() {
        if (!isBandwidthControlEnabled()) {
            throw new IllegalStateException("Bandwidth module disabled");
        }
    }

    private boolean isBandwidthControlEnabled() {
        long token = Binder.clearCallingIdentity();
        boolean e;
        try {
            e = this.mNetworkManager.isBandwidthControlEnabled();
            return e;
        } catch (RemoteException e2) {
            e = e2;
            return false;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public long getTimeRefreshElapsedRealtime() {
        if (this.mTimeRefreshRealtime != -1) {
            return SystemClock.elapsedRealtime() - this.mTimeRefreshRealtime;
        }
        return JobStatus.NO_LATEST_RUNTIME;
    }
}
