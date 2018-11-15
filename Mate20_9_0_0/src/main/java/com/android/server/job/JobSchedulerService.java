package com.android.server.job;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.app.AppGlobals;
import android.app.IUidObserver;
import android.app.IUidObserver.Stub;
import android.app.job.IJobScheduler;
import android.app.job.JobInfo;
import android.app.job.JobWorkItem;
import android.app.usage.UsageStatsManagerInternal;
import android.app.usage.UsageStatsManagerInternal.AppIdleStateChangeListener;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryStatsInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.provider.Settings.Global;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.AppStateTracker;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.job.JobSchedulerInternal.JobStorePersistStats;
import com.android.server.job.controllers.BackgroundJobsController;
import com.android.server.job.controllers.BatteryController;
import com.android.server.job.controllers.ConnectivityController;
import com.android.server.job.controllers.ContentObserverController;
import com.android.server.job.controllers.DeviceIdleJobsController;
import com.android.server.job.controllers.HwTimeController;
import com.android.server.job.controllers.IdleController;
import com.android.server.job.controllers.JobStatus;
import com.android.server.job.controllers.StateController;
import com.android.server.job.controllers.StorageController;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.IHwShutdownThread;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.utils.PriorityDump;
import huawei.android.security.IHwBehaviorCollectManager;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import libcore.util.EmptyArray;

public class JobSchedulerService extends AbsJobSchedulerService implements StateChangedListener, JobCompletedListener {
    static final int ACTIVE_INDEX = 0;
    public static final boolean DEBUG = Log.isLoggable(TAG, 3);
    public static final boolean DEBUG_STANDBY = (DEBUG);
    private static final boolean ENFORCE_MAX_JOBS = true;
    private static final int FG_JOB_CONTEXTS_COUNT = 2;
    static final int FREQUENT_INDEX = 2;
    static final String HEARTBEAT_TAG = "*job.heartbeat*";
    private static final int MAX_JOBS_PER_APP = 100;
    private static final int MAX_JOB_CONTEXTS_COUNT = 16;
    static final int MSG_CHECK_JOB = 1;
    static final int MSG_CHECK_JOB_GREEDY = 3;
    static final int MSG_JOB_EXPIRED = 0;
    static final int MSG_STOP_JOB = 2;
    static final int MSG_UID_ACTIVE = 6;
    static final int MSG_UID_GONE = 5;
    static final int MSG_UID_IDLE = 7;
    static final int MSG_UID_STATE_CHANGED = 4;
    static final int NEVER_INDEX = 4;
    static final int RARE_INDEX = 3;
    public static final String TAG = "JobScheduler";
    static final int WORKING_INDEX = 1;
    static final Comparator<JobStatus> mEnqueueTimeComparator = -$$Lambda$JobSchedulerService$V6_ZmVmzJutg4w0s0LktDOsRAss.INSTANCE;
    @VisibleForTesting
    public static Clock sElapsedRealtimeClock = SystemClock.elapsedRealtimeClock();
    @VisibleForTesting
    public static Clock sSystemClock = Clock.systemUTC();
    @VisibleForTesting
    public static Clock sUptimeMillisClock = SystemClock.uptimeMillisClock();
    final List<JobServiceContext> mActiveServices = new ArrayList();
    ActivityManagerInternal mActivityManagerInternal = ((ActivityManagerInternal) Preconditions.checkNotNull((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)));
    AppStateTracker mAppStateTracker;
    final SparseIntArray mBackingUpUids = new SparseIntArray();
    private final BatteryController mBatteryController;
    IBatteryStats mBatteryStats;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Removed duplicated region for block: B:25:0x00a1 A:{Splitter: B:16:0x0065, ExcHandler: android.os.RemoteException (e android.os.RemoteException)} */
        /* JADX WARNING: Missing block: B:88:?, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            String str;
            String action = intent.getAction();
            if (JobSchedulerService.DEBUG) {
                str = JobSchedulerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Receieved: ");
                stringBuilder.append(action);
                Slog.d(str, stringBuilder.toString());
            }
            str = JobSchedulerService.this.getPackageName(intent);
            int pkgUid = intent.getIntExtra("android.intent.extra.UID", -1);
            int i = 0;
            String str2;
            StringBuilder stringBuilder2;
            int length;
            String str3;
            StringBuilder stringBuilder3;
            int uidRemoved;
            if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                if (str == null || pkgUid == -1) {
                    str2 = JobSchedulerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("PACKAGE_CHANGED for ");
                    stringBuilder2.append(str);
                    stringBuilder2.append(" / uid ");
                    stringBuilder2.append(pkgUid);
                    Slog.w(str2, stringBuilder2.toString());
                    return;
                }
                String[] changedComponents = intent.getStringArrayExtra("android.intent.extra.changed_component_name_list");
                if (changedComponents != null) {
                    length = changedComponents.length;
                    while (i < length) {
                        if (changedComponents[i].equals(str)) {
                            if (JobSchedulerService.DEBUG) {
                                str3 = JobSchedulerService.TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Package state change: ");
                                stringBuilder3.append(str);
                                Slog.d(str3, stringBuilder3.toString());
                            }
                            try {
                                length = UserHandle.getUserId(pkgUid);
                                int state = AppGlobals.getPackageManager().getApplicationEnabledSetting(str, length);
                                if (state == 2 || state == 3) {
                                    if (JobSchedulerService.DEBUG) {
                                        String str4 = JobSchedulerService.TAG;
                                        StringBuilder stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("Removing jobs for package ");
                                        stringBuilder4.append(str);
                                        stringBuilder4.append(" in user ");
                                        stringBuilder4.append(length);
                                        Slog.d(str4, stringBuilder4.toString());
                                    }
                                    JobSchedulerService.this.cancelJobsForPackageAndUid(str, pkgUid, "app disabled");
                                    return;
                                }
                                return;
                            } catch (RemoteException e) {
                            }
                        } else {
                            i++;
                        }
                    }
                }
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false)) {
                    uidRemoved = intent.getIntExtra("android.intent.extra.UID", -1);
                    if (JobSchedulerService.DEBUG) {
                        str3 = JobSchedulerService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Removing jobs for uid: ");
                        stringBuilder3.append(uidRemoved);
                        Slog.d(str3, stringBuilder3.toString());
                    }
                    JobSchedulerService.this.cancelJobsForPackageAndUid(str, uidRemoved, "app uninstalled");
                }
            } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                uidRemoved = intent.getIntExtra("android.intent.extra.user_handle", 0);
                if (JobSchedulerService.DEBUG) {
                    str3 = JobSchedulerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Removing jobs for user: ");
                    stringBuilder3.append(uidRemoved);
                    Slog.d(str3, stringBuilder3.toString());
                }
                JobSchedulerService.this.cancelJobsForUser(uidRemoved);
            } else if ("android.intent.action.QUERY_PACKAGE_RESTART".equals(action)) {
                if (pkgUid != -1) {
                    List<JobStatus> jobsForUid;
                    synchronized (JobSchedulerService.this.mLock) {
                        jobsForUid = JobSchedulerService.this.mJobs.getJobsByUid(pkgUid);
                    }
                    for (length = jobsForUid.size() - 1; length >= 0; length--) {
                        if (((JobStatus) jobsForUid.get(length)).getSourcePackageName().equals(str)) {
                            if (JobSchedulerService.DEBUG) {
                                String str5 = JobSchedulerService.TAG;
                                StringBuilder stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("Restart query: package ");
                                stringBuilder5.append(str);
                                stringBuilder5.append(" at uid ");
                                stringBuilder5.append(pkgUid);
                                stringBuilder5.append(" has jobs");
                                Slog.d(str5, stringBuilder5.toString());
                            }
                            setResultCode(-1);
                            return;
                        }
                    }
                }
            } else if ("android.intent.action.PACKAGE_RESTARTED".equals(action) && pkgUid != -1) {
                if (JobSchedulerService.DEBUG) {
                    str2 = JobSchedulerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Removing jobs for pkg ");
                    stringBuilder2.append(str);
                    stringBuilder2.append(" at uid ");
                    stringBuilder2.append(pkgUid);
                    Slog.d(str2, stringBuilder2.toString());
                }
                JobSchedulerService.this.cancelJobsForPackageAndUid(str, pkgUid, "app force stopped");
            }
        }
    };
    final Constants mConstants;
    final ConstantsObserver mConstantsObserver;
    private final List<StateController> mControllers;
    private final DeviceIdleJobsController mDeviceIdleJobsController;
    final JobHandler mHandler;
    long mHeartbeat = 0;
    final HeartbeatAlarmListener mHeartbeatAlarm = new HeartbeatAlarmListener();
    private final HwTimeController mHwTimeController;
    volatile boolean mInParole;
    private final Predicate<Integer> mIsUidActivePredicate = new -$$Lambda$JobSchedulerService$AauD0it1BcgWldVm_V1m2Jo7_Zc(this);
    final JobPackageTracker mJobPackageTracker = new JobPackageTracker();
    final JobSchedulerStub mJobSchedulerStub;
    private final Runnable mJobTimeUpdater = new -$$Lambda$JobSchedulerService$nXpbkYDrU0yC5DuTafFiblXBdTY(this);
    final JobStore mJobs;
    long mLastHeartbeatTime = sElapsedRealtimeClock.millis();
    final SparseArray<HashMap<String, Long>> mLastJobHeartbeats = new SparseArray();
    com.android.server.DeviceIdleController.LocalService mLocalDeviceIdleController;
    PackageManagerInternal mLocalPM = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class));
    final Object mLock = new Object();
    int mMaxActiveJobs = 1;
    private final MaybeReadyJobQueueFunctor mMaybeQueueFunctor = new MaybeReadyJobQueueFunctor();
    final long[] mNextBucketHeartbeat = new long[]{0, 0, 0, 0, JobStatus.NO_LATEST_RUNTIME};
    final ArrayList<JobStatus> mPendingJobs = new ArrayList();
    private final ReadyJobQueueFunctor mReadyQueueFunctor = new ReadyJobQueueFunctor();
    boolean mReadyToRock;
    boolean mReportedActive;
    final StandbyTracker mStandbyTracker;
    int[] mStartedUsers = EmptyArray.INT;
    private final StorageController mStorageController;
    private final BroadcastReceiver mTimeSetReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.TIME_SET".equals(intent.getAction()) && JobSchedulerService.this.mJobs.clockNowValidToInflate(JobSchedulerService.sSystemClock.millis())) {
                Slog.i(JobSchedulerService.TAG, "RTC now valid; recalculating persisted job windows");
                context.unregisterReceiver(this);
                FgThread.getHandler().post(JobSchedulerService.this.mJobTimeUpdater);
            }
        }
    };
    boolean[] mTmpAssignAct = new boolean[16];
    JobStatus[] mTmpAssignContextIdToJobMap = new JobStatus[16];
    int[] mTmpAssignPreferredUidForContext = new int[16];
    private final IUidObserver mUidObserver = new Stub() {
        public void onUidStateChanged(int uid, int procState, long procStateSeq) {
            JobSchedulerService.this.mHandler.obtainMessage(4, uid, procState).sendToTarget();
        }

        public void onUidGone(int uid, boolean disabled) {
            JobSchedulerService.this.mHandler.obtainMessage(5, uid, disabled).sendToTarget();
        }

        public void onUidActive(int uid) throws RemoteException {
            JobSchedulerService.this.mHandler.obtainMessage(6, uid, 0).sendToTarget();
        }

        public void onUidIdle(int uid, boolean disabled) {
            JobSchedulerService.this.mHandler.obtainMessage(7, uid, disabled).sendToTarget();
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }
    };
    final SparseIntArray mUidPriorityOverride = new SparseIntArray();
    final UsageStatsManagerInternal mUsageStats;

    public static class Constants {
        private static final int DEFAULT_BG_CRITICAL_JOB_COUNT = 1;
        private static final int DEFAULT_BG_LOW_JOB_COUNT = 1;
        private static final int DEFAULT_BG_MODERATE_JOB_COUNT = 4;
        private static final int DEFAULT_BG_NORMAL_JOB_COUNT = 6;
        private static final float DEFAULT_CONN_CONGESTION_DELAY_FRAC = 0.5f;
        private static final float DEFAULT_CONN_PREFETCH_RELAX_FRAC = 0.5f;
        private static final int DEFAULT_FG_JOB_COUNT = 4;
        private static final float DEFAULT_HEAVY_USE_FACTOR = 0.9f;
        private static final int DEFAULT_MAX_STANDARD_RESCHEDULE_COUNT = Integer.MAX_VALUE;
        private static final int DEFAULT_MAX_WORK_RESCHEDULE_COUNT = Integer.MAX_VALUE;
        private static final int DEFAULT_MIN_BATTERY_NOT_LOW_COUNT = 1;
        private static final int DEFAULT_MIN_CHARGING_COUNT = 1;
        private static final int DEFAULT_MIN_CONNECTIVITY_COUNT = 1;
        private static final int DEFAULT_MIN_CONTENT_COUNT = 1;
        private static final long DEFAULT_MIN_EXP_BACKOFF_TIME = 10000;
        private static final int DEFAULT_MIN_IDLE_COUNT = 1;
        private static final long DEFAULT_MIN_LINEAR_BACKOFF_TIME = 10000;
        private static final int DEFAULT_MIN_READY_JOBS_COUNT = 1;
        private static final int DEFAULT_MIN_STORAGE_NOT_LOW_COUNT = 1;
        private static final float DEFAULT_MODERATE_USE_FACTOR = 0.5f;
        private static final int DEFAULT_STANDBY_FREQUENT_BEATS = 43;
        private static final long DEFAULT_STANDBY_HEARTBEAT_TIME = 660000;
        private static final int DEFAULT_STANDBY_RARE_BEATS = 130;
        private static final int DEFAULT_STANDBY_WORKING_BEATS = 11;
        private static final String KEY_BG_CRITICAL_JOB_COUNT = "bg_critical_job_count";
        private static final String KEY_BG_LOW_JOB_COUNT = "bg_low_job_count";
        private static final String KEY_BG_MODERATE_JOB_COUNT = "bg_moderate_job_count";
        private static final String KEY_BG_NORMAL_JOB_COUNT = "bg_normal_job_count";
        private static final String KEY_CONN_CONGESTION_DELAY_FRAC = "conn_congestion_delay_frac";
        private static final String KEY_CONN_PREFETCH_RELAX_FRAC = "conn_prefetch_relax_frac";
        private static final String KEY_FG_JOB_COUNT = "fg_job_count";
        private static final String KEY_HEAVY_USE_FACTOR = "heavy_use_factor";
        private static final String KEY_MAX_STANDARD_RESCHEDULE_COUNT = "max_standard_reschedule_count";
        private static final String KEY_MAX_WORK_RESCHEDULE_COUNT = "max_work_reschedule_count";
        private static final String KEY_MIN_BATTERY_NOT_LOW_COUNT = "min_battery_not_low_count";
        private static final String KEY_MIN_CHARGING_COUNT = "min_charging_count";
        private static final String KEY_MIN_CONNECTIVITY_COUNT = "min_connectivity_count";
        private static final String KEY_MIN_CONTENT_COUNT = "min_content_count";
        private static final String KEY_MIN_EXP_BACKOFF_TIME = "min_exp_backoff_time";
        private static final String KEY_MIN_IDLE_COUNT = "min_idle_count";
        private static final String KEY_MIN_LINEAR_BACKOFF_TIME = "min_linear_backoff_time";
        private static final String KEY_MIN_READY_JOBS_COUNT = "min_ready_jobs_count";
        private static final String KEY_MIN_STORAGE_NOT_LOW_COUNT = "min_storage_not_low_count";
        private static final String KEY_MODERATE_USE_FACTOR = "moderate_use_factor";
        private static final String KEY_STANDBY_FREQUENT_BEATS = "standby_frequent_beats";
        private static final String KEY_STANDBY_HEARTBEAT_TIME = "standby_heartbeat_time";
        private static final String KEY_STANDBY_RARE_BEATS = "standby_rare_beats";
        private static final String KEY_STANDBY_WORKING_BEATS = "standby_working_beats";
        int BG_CRITICAL_JOB_COUNT = 1;
        int BG_LOW_JOB_COUNT = 1;
        int BG_MODERATE_JOB_COUNT = 4;
        int BG_NORMAL_JOB_COUNT = 6;
        public float CONN_CONGESTION_DELAY_FRAC = 0.5f;
        public float CONN_PREFETCH_RELAX_FRAC = 0.5f;
        int FG_JOB_COUNT = 4;
        float HEAVY_USE_FACTOR = DEFAULT_HEAVY_USE_FACTOR;
        int MAX_STANDARD_RESCHEDULE_COUNT = HwBootFail.STAGE_BOOT_SUCCESS;
        int MAX_WORK_RESCHEDULE_COUNT = HwBootFail.STAGE_BOOT_SUCCESS;
        int MIN_BATTERY_NOT_LOW_COUNT = 1;
        int MIN_CHARGING_COUNT = 1;
        int MIN_CONNECTIVITY_COUNT = 1;
        int MIN_CONTENT_COUNT = 1;
        long MIN_EXP_BACKOFF_TIME = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        int MIN_IDLE_COUNT = 1;
        long MIN_LINEAR_BACKOFF_TIME = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        int MIN_READY_JOBS_COUNT = 1;
        int MIN_STORAGE_NOT_LOW_COUNT = 1;
        float MODERATE_USE_FACTOR = 0.5f;
        final int[] STANDBY_BEATS = new int[]{0, 11, 43, DEFAULT_STANDBY_RARE_BEATS};
        long STANDBY_HEARTBEAT_TIME = DEFAULT_STANDBY_HEARTBEAT_TIME;
        private final KeyValueListParser mParser = new KeyValueListParser(',');

        void updateConstantsLocked(String value) {
            try {
                this.mParser.setString(value);
            } catch (Exception e) {
                Slog.e(JobSchedulerService.TAG, "Bad jobscheduler settings", e);
            }
            this.MIN_IDLE_COUNT = this.mParser.getInt(KEY_MIN_IDLE_COUNT, 1);
            this.MIN_CHARGING_COUNT = this.mParser.getInt(KEY_MIN_CHARGING_COUNT, 1);
            this.MIN_BATTERY_NOT_LOW_COUNT = this.mParser.getInt(KEY_MIN_BATTERY_NOT_LOW_COUNT, 1);
            this.MIN_STORAGE_NOT_LOW_COUNT = this.mParser.getInt(KEY_MIN_STORAGE_NOT_LOW_COUNT, 1);
            this.MIN_CONNECTIVITY_COUNT = this.mParser.getInt(KEY_MIN_CONNECTIVITY_COUNT, 1);
            this.MIN_CONTENT_COUNT = this.mParser.getInt(KEY_MIN_CONTENT_COUNT, 1);
            this.MIN_READY_JOBS_COUNT = this.mParser.getInt(KEY_MIN_READY_JOBS_COUNT, 1);
            this.HEAVY_USE_FACTOR = this.mParser.getFloat(KEY_HEAVY_USE_FACTOR, DEFAULT_HEAVY_USE_FACTOR);
            this.MODERATE_USE_FACTOR = this.mParser.getFloat(KEY_MODERATE_USE_FACTOR, 0.5f);
            this.FG_JOB_COUNT = this.mParser.getInt(KEY_FG_JOB_COUNT, 4);
            this.BG_NORMAL_JOB_COUNT = this.mParser.getInt(KEY_BG_NORMAL_JOB_COUNT, 6);
            if (this.FG_JOB_COUNT + this.BG_NORMAL_JOB_COUNT > 16) {
                this.BG_NORMAL_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.BG_MODERATE_JOB_COUNT = this.mParser.getInt(KEY_BG_MODERATE_JOB_COUNT, 4);
            if (this.FG_JOB_COUNT + this.BG_MODERATE_JOB_COUNT > 16) {
                this.BG_MODERATE_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.BG_LOW_JOB_COUNT = this.mParser.getInt(KEY_BG_LOW_JOB_COUNT, 1);
            if (this.FG_JOB_COUNT + this.BG_LOW_JOB_COUNT > 16) {
                this.BG_LOW_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.BG_CRITICAL_JOB_COUNT = this.mParser.getInt(KEY_BG_CRITICAL_JOB_COUNT, 1);
            if (this.FG_JOB_COUNT + this.BG_CRITICAL_JOB_COUNT > 16) {
                this.BG_CRITICAL_JOB_COUNT = 16 - this.FG_JOB_COUNT;
            }
            this.MAX_STANDARD_RESCHEDULE_COUNT = this.mParser.getInt(KEY_MAX_STANDARD_RESCHEDULE_COUNT, HwBootFail.STAGE_BOOT_SUCCESS);
            this.MAX_WORK_RESCHEDULE_COUNT = this.mParser.getInt(KEY_MAX_WORK_RESCHEDULE_COUNT, HwBootFail.STAGE_BOOT_SUCCESS);
            this.MIN_LINEAR_BACKOFF_TIME = this.mParser.getDurationMillis(KEY_MIN_LINEAR_BACKOFF_TIME, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            this.MIN_EXP_BACKOFF_TIME = this.mParser.getDurationMillis(KEY_MIN_EXP_BACKOFF_TIME, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            this.STANDBY_HEARTBEAT_TIME = this.mParser.getDurationMillis(KEY_STANDBY_HEARTBEAT_TIME, DEFAULT_STANDBY_HEARTBEAT_TIME);
            this.STANDBY_BEATS[1] = this.mParser.getInt(KEY_STANDBY_WORKING_BEATS, 11);
            this.STANDBY_BEATS[2] = this.mParser.getInt(KEY_STANDBY_FREQUENT_BEATS, 43);
            this.STANDBY_BEATS[3] = this.mParser.getInt(KEY_STANDBY_RARE_BEATS, DEFAULT_STANDBY_RARE_BEATS);
            this.CONN_CONGESTION_DELAY_FRAC = this.mParser.getFloat(KEY_CONN_CONGESTION_DELAY_FRAC, 0.5f);
            this.CONN_PREFETCH_RELAX_FRAC = this.mParser.getFloat(KEY_CONN_PREFETCH_RELAX_FRAC, 0.5f);
        }

        void dump(IndentingPrintWriter pw) {
            pw.println("Settings:");
            pw.increaseIndent();
            pw.printPair(KEY_MIN_IDLE_COUNT, Integer.valueOf(this.MIN_IDLE_COUNT)).println();
            pw.printPair(KEY_MIN_CHARGING_COUNT, Integer.valueOf(this.MIN_CHARGING_COUNT)).println();
            pw.printPair(KEY_MIN_BATTERY_NOT_LOW_COUNT, Integer.valueOf(this.MIN_BATTERY_NOT_LOW_COUNT)).println();
            pw.printPair(KEY_MIN_STORAGE_NOT_LOW_COUNT, Integer.valueOf(this.MIN_STORAGE_NOT_LOW_COUNT)).println();
            pw.printPair(KEY_MIN_CONNECTIVITY_COUNT, Integer.valueOf(this.MIN_CONNECTIVITY_COUNT)).println();
            pw.printPair(KEY_MIN_CONTENT_COUNT, Integer.valueOf(this.MIN_CONTENT_COUNT)).println();
            pw.printPair(KEY_MIN_READY_JOBS_COUNT, Integer.valueOf(this.MIN_READY_JOBS_COUNT)).println();
            pw.printPair(KEY_HEAVY_USE_FACTOR, Float.valueOf(this.HEAVY_USE_FACTOR)).println();
            pw.printPair(KEY_MODERATE_USE_FACTOR, Float.valueOf(this.MODERATE_USE_FACTOR)).println();
            pw.printPair(KEY_FG_JOB_COUNT, Integer.valueOf(this.FG_JOB_COUNT)).println();
            pw.printPair(KEY_BG_NORMAL_JOB_COUNT, Integer.valueOf(this.BG_NORMAL_JOB_COUNT)).println();
            pw.printPair(KEY_BG_MODERATE_JOB_COUNT, Integer.valueOf(this.BG_MODERATE_JOB_COUNT)).println();
            pw.printPair(KEY_BG_LOW_JOB_COUNT, Integer.valueOf(this.BG_LOW_JOB_COUNT)).println();
            pw.printPair(KEY_BG_CRITICAL_JOB_COUNT, Integer.valueOf(this.BG_CRITICAL_JOB_COUNT)).println();
            pw.printPair(KEY_MAX_STANDARD_RESCHEDULE_COUNT, Integer.valueOf(this.MAX_STANDARD_RESCHEDULE_COUNT)).println();
            pw.printPair(KEY_MAX_WORK_RESCHEDULE_COUNT, Integer.valueOf(this.MAX_WORK_RESCHEDULE_COUNT)).println();
            pw.printPair(KEY_MIN_LINEAR_BACKOFF_TIME, Long.valueOf(this.MIN_LINEAR_BACKOFF_TIME)).println();
            pw.printPair(KEY_MIN_EXP_BACKOFF_TIME, Long.valueOf(this.MIN_EXP_BACKOFF_TIME)).println();
            pw.printPair(KEY_STANDBY_HEARTBEAT_TIME, Long.valueOf(this.STANDBY_HEARTBEAT_TIME)).println();
            pw.print("standby_beats={");
            pw.print(this.STANDBY_BEATS[0]);
            for (int i = 1; i < this.STANDBY_BEATS.length; i++) {
                pw.print(", ");
                pw.print(this.STANDBY_BEATS[i]);
            }
            pw.println('}');
            pw.printPair(KEY_CONN_CONGESTION_DELAY_FRAC, Float.valueOf(this.CONN_CONGESTION_DELAY_FRAC)).println();
            pw.printPair(KEY_CONN_PREFETCH_RELAX_FRAC, Float.valueOf(this.CONN_PREFETCH_RELAX_FRAC)).println();
            pw.decreaseIndent();
        }

        void dump(ProtoOutputStream proto, long fieldId) {
            long token = proto.start(fieldId);
            proto.write(1120986464257L, this.MIN_IDLE_COUNT);
            proto.write(1120986464258L, this.MIN_CHARGING_COUNT);
            proto.write(1120986464259L, this.MIN_BATTERY_NOT_LOW_COUNT);
            proto.write(1120986464260L, this.MIN_STORAGE_NOT_LOW_COUNT);
            proto.write(1120986464261L, this.MIN_CONNECTIVITY_COUNT);
            proto.write(1120986464262L, this.MIN_CONTENT_COUNT);
            proto.write(1120986464263L, this.MIN_READY_JOBS_COUNT);
            proto.write(1103806595080L, this.HEAVY_USE_FACTOR);
            proto.write(1103806595081L, this.MODERATE_USE_FACTOR);
            proto.write(1120986464266L, this.FG_JOB_COUNT);
            proto.write(1120986464267L, this.BG_NORMAL_JOB_COUNT);
            proto.write(1120986464268L, this.BG_MODERATE_JOB_COUNT);
            proto.write(1120986464269L, this.BG_LOW_JOB_COUNT);
            proto.write(1120986464270L, this.BG_CRITICAL_JOB_COUNT);
            proto.write(1120986464271L, this.MAX_STANDARD_RESCHEDULE_COUNT);
            proto.write(1120986464272L, this.MAX_WORK_RESCHEDULE_COUNT);
            proto.write(1112396529681L, this.MIN_LINEAR_BACKOFF_TIME);
            proto.write(1112396529682L, this.MIN_EXP_BACKOFF_TIME);
            proto.write(1112396529683L, this.STANDBY_HEARTBEAT_TIME);
            for (int period : this.STANDBY_BEATS) {
                proto.write(2220498092052L, period);
            }
            proto.write(1103806595093L, this.CONN_CONGESTION_DELAY_FRAC);
            proto.write(1103806595094L, this.CONN_PREFETCH_RELAX_FRAC);
            proto.end(token);
        }
    }

    private class ConstantsObserver extends ContentObserver {
        private ContentResolver mResolver;

        public ConstantsObserver(Handler handler) {
            super(handler);
        }

        public void start(ContentResolver resolver) {
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Global.getUriFor("job_scheduler_constants"), false, this);
            updateConstants();
        }

        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (JobSchedulerService.this.mLock) {
                try {
                    JobSchedulerService.this.mConstants.updateConstantsLocked(Global.getString(this.mResolver, "job_scheduler_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(JobSchedulerService.TAG, "Bad jobscheduler settings", e);
                }
            }
            JobSchedulerService.this.setNextHeartbeatAlarm();
        }
    }

    static class DeferredJobCounter implements Consumer<JobStatus> {
        private int mDeferred = 0;

        DeferredJobCounter() {
        }

        public int numDeferred() {
            return this.mDeferred;
        }

        public void accept(JobStatus job) {
            if (job.getWhenStandbyDeferred() > 0) {
                this.mDeferred++;
            }
        }
    }

    class HeartbeatAlarmListener implements OnAlarmListener {
        HeartbeatAlarmListener() {
        }

        public void onAlarm() {
            synchronized (JobSchedulerService.this.mLock) {
                long beatsElapsed = (JobSchedulerService.sElapsedRealtimeClock.millis() - JobSchedulerService.this.mLastHeartbeatTime) / JobSchedulerService.this.mConstants.STANDBY_HEARTBEAT_TIME;
                if (beatsElapsed > 0) {
                    JobSchedulerService jobSchedulerService = JobSchedulerService.this;
                    jobSchedulerService.mLastHeartbeatTime += JobSchedulerService.this.mConstants.STANDBY_HEARTBEAT_TIME * beatsElapsed;
                    JobSchedulerService.this.advanceHeartbeatLocked(beatsElapsed);
                }
            }
            JobSchedulerService.this.setNextHeartbeatAlarm();
        }
    }

    private final class JobHandler extends Handler {
        public JobHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARNING: Missing block: B:68:0x00e9, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void handleMessage(Message message) {
            synchronized (JobSchedulerService.this.mLock) {
                if (JobSchedulerService.this.mReadyToRock) {
                    boolean needRemoveCheckJobMsg = false;
                    int uid;
                    switch (message.what) {
                        case 0:
                            JobStatus runNow = message.obj;
                            if (runNow != null && JobSchedulerService.this.isReadyToBeExecutedLocked(runNow)) {
                                JobSchedulerService.this.mJobPackageTracker.notePending(runNow);
                                JobSchedulerService.addOrderedItem(JobSchedulerService.this.mPendingJobs, runNow, JobSchedulerService.mEnqueueTimeComparator);
                                break;
                            }
                            JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                            needRemoveCheckJobMsg = true;
                            break;
                        case 1:
                            if (JobSchedulerService.this.mReportedActive) {
                                JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                            } else {
                                JobSchedulerService.this.maybeQueueReadyJobsForExecutionLocked();
                            }
                            needRemoveCheckJobMsg = true;
                            break;
                        case 2:
                            JobSchedulerService.this.cancelJobImplLocked((JobStatus) message.obj, null, "app no longer allowed to run");
                            break;
                        case 3:
                            JobSchedulerService.this.queueReadyJobsForExecutionLocked();
                            needRemoveCheckJobMsg = true;
                            break;
                        case 4:
                            JobSchedulerService.this.updateUidState(message.arg1, message.arg2);
                            break;
                        case 5:
                            uid = message.arg1;
                            boolean disabled = message.arg2 != 0;
                            JobSchedulerService.this.updateUidState(uid, 18);
                            if (disabled) {
                                JobSchedulerService.this.cancelJobsForUid(uid, "uid gone");
                            }
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(uid, false);
                            }
                        case 6:
                            uid = message.arg1;
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(uid, true);
                            }
                        case 7:
                            uid = message.arg1;
                            if (message.arg2 != 0) {
                                JobSchedulerService.this.cancelJobsForUid(uid, "app uid idle");
                            }
                            synchronized (JobSchedulerService.this.mLock) {
                                JobSchedulerService.this.mDeviceIdleJobsController.setUidActiveLocked(uid, false);
                            }
                    }
                    JobSchedulerService.this.maybeRunPendingJobsLocked();
                    if (needRemoveCheckJobMsg) {
                        removeMessages(1);
                    }
                } else {
                    Slog.i(JobSchedulerService.TAG, "handleMessage mReadyToRock false");
                }
            }
        }
    }

    final class JobSchedulerStub extends IJobScheduler.Stub {
        private final SparseArray<Boolean> mPersistCache = new SparseArray();

        JobSchedulerStub() {
        }

        private void enforceValidJobRequest(int uid, JobInfo job) {
            IPackageManager pm = AppGlobals.getPackageManager();
            ComponentName service = job.getService();
            try {
                ServiceInfo si = pm.getServiceInfo(service, 786432, UserHandle.getUserId(uid));
                StringBuilder stringBuilder;
                if (si == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No such service ");
                    stringBuilder.append(service);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (si.applicationInfo.uid != uid) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("uid ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" cannot schedule job in ");
                    stringBuilder.append(service.getPackageName());
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (!"android.permission.BIND_JOB_SERVICE".equals(si.permission)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Scheduled service ");
                    stringBuilder.append(service);
                    stringBuilder.append(" does not require android.permission.BIND_JOB_SERVICE permission");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (RemoteException e) {
            }
        }

        private boolean canPersistJobs(int pid, int uid) {
            boolean canPersist;
            synchronized (this.mPersistCache) {
                Boolean cached = (Boolean) this.mPersistCache.get(uid);
                if (cached != null) {
                    canPersist = cached.booleanValue();
                } else {
                    boolean canPersist2 = JobSchedulerService.this.getContext().checkPermission("android.permission.RECEIVE_BOOT_COMPLETED", pid, uid) == 0;
                    this.mPersistCache.put(uid, Boolean.valueOf(canPersist2));
                    canPersist = canPersist2;
                }
            }
            return canPersist;
        }

        private void validateJobFlags(JobInfo job, int callingUid) {
            if ((job.getFlags() & 1) != 0) {
                JobSchedulerService.this.getContext().enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", JobSchedulerService.TAG);
            }
            if ((job.getFlags() & 8) == 0) {
                return;
            }
            if (callingUid != 1000) {
                throw new SecurityException("Job has invalid flags");
            } else if (job.isPeriodic()) {
                String str = JobSchedulerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Periodic jobs mustn't have FLAG_EXEMPT_FROM_APP_STANDBY. Job=");
                stringBuilder.append(job);
                Slog.wtf(str, stringBuilder.toString());
            }
        }

        public int schedule(JobInfo job) throws RemoteException {
            IHwBehaviorCollectManager manager = HwFrameworkFactory.getHwBehaviorCollectManager();
            if (manager != null) {
                manager.sendBehavior(BehaviorId.JOBSCHEDULER_SCHEDULE);
            }
            if (JobSchedulerService.DEBUG) {
                String str = JobSchedulerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Scheduling job: ");
                stringBuilder.append(job.toString());
                Slog.d(str, stringBuilder.toString());
            }
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(uid);
            enforceValidJobRequest(uid, job);
            if (!job.isPersisted() || canPersistJobs(pid, uid)) {
                validateJobFlags(job, uid);
                long ident = Binder.clearCallingIdentity();
                try {
                    int scheduleAsPackage = JobSchedulerService.this.scheduleAsPackage(job, null, uid, null, userId, null);
                    return scheduleAsPackage;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("Error: requested job be persisted without holding RECEIVE_BOOT_COMPLETED permission.");
            }
        }

        public int enqueue(JobInfo job, JobWorkItem work) throws RemoteException {
            if (JobSchedulerService.DEBUG) {
                String str = JobSchedulerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Enqueueing job: ");
                stringBuilder.append(job.toString());
                stringBuilder.append(" work: ");
                stringBuilder.append(work);
                Slog.d(str, stringBuilder.toString());
            }
            int uid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(uid);
            enforceValidJobRequest(uid, job);
            if (job.isPersisted()) {
                throw new IllegalArgumentException("Can't enqueue work for persisted jobs");
            } else if (work != null) {
                validateJobFlags(job, uid);
                long ident = Binder.clearCallingIdentity();
                try {
                    int scheduleAsPackage = JobSchedulerService.this.scheduleAsPackage(job, work, uid, null, userId, null);
                    return scheduleAsPackage;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new NullPointerException("work is null");
            }
        }

        public int scheduleAsPackage(JobInfo job, String packageName, int userId, String tag) throws RemoteException {
            StringBuilder stringBuilder;
            int callerUid = Binder.getCallingUid();
            if (JobSchedulerService.DEBUG) {
                String str = JobSchedulerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Caller uid ");
                stringBuilder.append(callerUid);
                stringBuilder.append(" scheduling job: ");
                stringBuilder.append(job.toString());
                stringBuilder.append(" on behalf of ");
                stringBuilder.append(packageName);
                stringBuilder.append(SliceAuthority.DELIMITER);
                Slog.d(str, stringBuilder.toString());
            }
            if (packageName == null) {
                throw new NullPointerException("Must specify a package for scheduleAsPackage()");
            } else if (JobSchedulerService.this.getContext().checkCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS") == 0) {
                validateJobFlags(job, callerUid);
                long ident = Binder.clearCallingIdentity();
                try {
                    int scheduleAsPackage = JobSchedulerService.this.scheduleAsPackage(job, null, callerUid, packageName, userId, tag);
                    return scheduleAsPackage;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Caller uid ");
                stringBuilder.append(callerUid);
                stringBuilder.append(" not permitted to schedule jobs for other apps");
                throw new SecurityException(stringBuilder.toString());
            }
        }

        public List<JobInfo> getAllPendingJobs() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                List<JobInfo> pendingJobs = JobSchedulerService.this.getPendingJobs(uid);
                return pendingJobs;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public JobInfo getPendingJob(int jobId) throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobInfo pendingJob = JobSchedulerService.this.getPendingJob(uid, jobId);
                return pendingJob;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void cancelAll() throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobSchedulerService jobSchedulerService = JobSchedulerService.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cancelAll() called by app, callingUid=");
                stringBuilder.append(uid);
                jobSchedulerService.cancelJobsForUid(uid, stringBuilder.toString());
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void cancel(int jobId) throws RemoteException {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                JobSchedulerService.this.cancelJob(uid, jobId, uid);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:26:0x0066  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpAndUsageStatsPermission(JobSchedulerService.this.getContext(), JobSchedulerService.TAG, pw)) {
                boolean proto;
                int filterUid = -1;
                if (ArrayUtils.isEmpty(args)) {
                    proto = false;
                } else {
                    boolean proto2;
                    proto = false;
                    for (proto2 = false; proto2 < args.length; proto2++) {
                        String arg = args[proto2];
                        if ("-h".equals(arg)) {
                            JobSchedulerService.dumpHelp(pw);
                            return;
                        }
                        if (!"-a".equals(arg)) {
                            if (PriorityDump.PROTO_ARG.equals(arg)) {
                                proto = true;
                            } else {
                                if (arg.length() > 0 && arg.charAt(0) == '-') {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unknown option: ");
                                    stringBuilder.append(arg);
                                    pw.println(stringBuilder.toString());
                                    return;
                                }
                                if (proto2 < args.length) {
                                    String pkg = args[proto2];
                                    try {
                                        filterUid = JobSchedulerService.this.getContext().getPackageManager().getPackageUid(pkg, DumpState.DUMP_CHANGES);
                                    } catch (NameNotFoundException e) {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Invalid package: ");
                                        stringBuilder2.append(pkg);
                                        pw.println(stringBuilder2.toString());
                                        return;
                                    }
                                }
                            }
                        }
                    }
                    if (proto2 < args.length) {
                    }
                }
                long identityToken = Binder.clearCallingIdentity();
                if (proto) {
                    try {
                        JobSchedulerService.this.dumpInternalProto(fd, filterUid);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(identityToken);
                    }
                } else {
                    JobSchedulerService.this.dumpInternal(new IndentingPrintWriter(pw, "  "), filterUid);
                }
                Binder.restoreCallingIdentity(identityToken);
            }
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new JobSchedulerShellCommand(JobSchedulerService.this).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    final class MaybeReadyJobQueueFunctor implements Consumer<JobStatus> {
        int backoffCount;
        int batteryNotLowCount;
        int chargingCount;
        int connectivityCount;
        int contentCount;
        int idleCount;
        List<JobStatus> runnableJobs;
        int storageNotLowCount;

        public MaybeReadyJobQueueFunctor() {
            reset();
        }

        public void accept(JobStatus job) {
            if (JobSchedulerService.this.isReadyToBeExecutedLocked(job)) {
                try {
                    if (ActivityManager.getService().isAppStartModeDisabled(job.getUid(), job.getJob().getService().getPackageName())) {
                        String str = JobSchedulerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Aborting job ");
                        stringBuilder.append(job.getUid());
                        stringBuilder.append(":");
                        stringBuilder.append(job.getJob().toString());
                        stringBuilder.append(" -- package not allowed to start");
                        Slog.w(str, stringBuilder.toString());
                        JobSchedulerService.this.mHandler.obtainMessage(2, job).sendToTarget();
                        return;
                    }
                } catch (RemoteException e) {
                }
                if (job.getNumFailures() > 0) {
                    this.backoffCount++;
                }
                if (job.hasIdleConstraint()) {
                    this.idleCount++;
                }
                if (job.hasConnectivityConstraint()) {
                    this.connectivityCount++;
                }
                if (job.hasChargingConstraint()) {
                    this.chargingCount++;
                }
                if (job.hasBatteryNotLowConstraint()) {
                    this.batteryNotLowCount++;
                }
                if (job.hasStorageNotLowConstraint()) {
                    this.storageNotLowCount++;
                }
                if (job.hasContentTriggerConstraint()) {
                    this.contentCount++;
                }
                if (this.runnableJobs == null) {
                    this.runnableJobs = new ArrayList();
                }
                this.runnableJobs.add(job);
            }
        }

        public void postProcess() {
            if (this.backoffCount > 0 || this.idleCount >= JobSchedulerService.this.mConstants.MIN_IDLE_COUNT || this.connectivityCount >= JobSchedulerService.this.mConstants.MIN_CONNECTIVITY_COUNT || this.chargingCount >= JobSchedulerService.this.mConstants.MIN_CHARGING_COUNT || this.batteryNotLowCount >= JobSchedulerService.this.mConstants.MIN_BATTERY_NOT_LOW_COUNT || this.storageNotLowCount >= JobSchedulerService.this.mConstants.MIN_STORAGE_NOT_LOW_COUNT || this.contentCount >= JobSchedulerService.this.mConstants.MIN_CONTENT_COUNT || (this.runnableJobs != null && this.runnableJobs.size() >= JobSchedulerService.this.mConstants.MIN_READY_JOBS_COUNT)) {
                if (JobSchedulerService.DEBUG) {
                    Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLocked: Running jobs.");
                }
                JobSchedulerService.this.noteJobsPending(this.runnableJobs);
                JobSchedulerService.this.mPendingJobs.addAll(this.runnableJobs);
                if (JobSchedulerService.this.mPendingJobs.size() > 1) {
                    JobSchedulerService.this.mPendingJobs.sort(JobSchedulerService.mEnqueueTimeComparator);
                }
            } else if (JobSchedulerService.DEBUG) {
                Slog.d(JobSchedulerService.TAG, "maybeQueueReadyJobsForExecutionLocked: Not running anything.");
            }
            reset();
        }

        private void reset() {
            this.chargingCount = 0;
            this.idleCount = 0;
            this.backoffCount = 0;
            this.connectivityCount = 0;
            this.batteryNotLowCount = 0;
            this.storageNotLowCount = 0;
            this.contentCount = 0;
            this.runnableJobs = null;
        }
    }

    final class ReadyJobQueueFunctor implements Consumer<JobStatus> {
        ArrayList<JobStatus> newReadyJobs;

        ReadyJobQueueFunctor() {
        }

        public void accept(JobStatus job) {
            if (JobSchedulerService.this.isReadyToBeExecutedLocked(job)) {
                if (JobSchedulerService.DEBUG) {
                    String str = JobSchedulerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("    queued ");
                    stringBuilder.append(job.toShortString());
                    Slog.d(str, stringBuilder.toString());
                }
                if (this.newReadyJobs == null) {
                    this.newReadyJobs = new ArrayList();
                }
                this.newReadyJobs.add(job);
            }
        }

        public void postProcess() {
            if (this.newReadyJobs != null) {
                JobSchedulerService.this.noteJobsPending(this.newReadyJobs);
                JobSchedulerService.this.mPendingJobs.addAll(this.newReadyJobs);
                if (JobSchedulerService.this.mPendingJobs.size() > 1) {
                    JobSchedulerService.this.mPendingJobs.sort(JobSchedulerService.mEnqueueTimeComparator);
                }
            }
            this.newReadyJobs = null;
        }
    }

    final class StandbyTracker extends AppIdleStateChangeListener {
        StandbyTracker() {
        }

        public void onAppIdleStateChanged(String packageName, int userId, boolean idle, int bucket, int reason) {
            int uid = JobSchedulerService.this.mLocalPM.getPackageUid(packageName, 8192, userId);
            if (uid < 0) {
                if (JobSchedulerService.DEBUG_STANDBY) {
                    String str = JobSchedulerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("App idle state change for unknown app ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(SliceAuthority.DELIMITER);
                    stringBuilder.append(userId);
                    Slog.i(str, stringBuilder.toString());
                }
                return;
            }
            BackgroundThread.getHandler().post(new -$$Lambda$JobSchedulerService$StandbyTracker$18Nt1smLe-l9bimlwR39k5RbMdM(this, uid, JobSchedulerService.standbyBucketToBucketIndex(bucket), packageName));
        }

        public static /* synthetic */ void lambda$onAppIdleStateChanged$1(StandbyTracker standbyTracker, int uid, int bucketIndex, String packageName) {
            if (JobSchedulerService.DEBUG_STANDBY) {
                String str = JobSchedulerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Moving uid ");
                stringBuilder.append(uid);
                stringBuilder.append(" to bucketIndex ");
                stringBuilder.append(bucketIndex);
                Slog.i(str, stringBuilder.toString());
            }
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mJobs.forEachJobForSourceUid(uid, new -$$Lambda$JobSchedulerService$StandbyTracker$Ofnn0P__SXhzXRU-7eLyuPrl31w(packageName, bucketIndex));
                JobSchedulerService.this.onControllerStateChanged();
            }
        }

        static /* synthetic */ void lambda$onAppIdleStateChanged$0(String packageName, int bucketIndex, JobStatus job) {
            if (packageName.equals(job.getSourcePackageName())) {
                job.setStandbyBucket(bucketIndex);
            }
        }

        public void onParoleStateChanged(boolean isParoleOn) {
            if (JobSchedulerService.DEBUG_STANDBY) {
                String str = JobSchedulerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Global parole state now ");
                stringBuilder.append(isParoleOn ? "ON" : "OFF");
                Slog.i(str, stringBuilder.toString());
            }
            JobSchedulerService.this.mInParole = isParoleOn;
        }

        public void onUserInteractionStarted(String packageName, int userId) {
            int uid = JobSchedulerService.this.mLocalPM.getPackageUid(packageName, 8192, userId);
            if (uid >= 0) {
                long sinceLast = JobSchedulerService.this.mUsageStats.getTimeSinceLastJobRun(packageName, userId);
                if (sinceLast > 172800000) {
                    sinceLast = 0;
                }
                DeferredJobCounter counter = new DeferredJobCounter();
                synchronized (JobSchedulerService.this.mLock) {
                    JobSchedulerService.this.mJobs.forEachJobForSourceUid(uid, counter);
                }
                if (counter.numDeferred() > 0 || sinceLast > 0) {
                    ((BatteryStatsInternal) LocalServices.getService(BatteryStatsInternal.class)).noteJobsDeferred(uid, counter.numDeferred(), sinceLast);
                }
            }
        }
    }

    final class LocalService implements JobSchedulerInternal {
        LocalService() {
        }

        public long currentHeartbeat() {
            return JobSchedulerService.this.getCurrentHeartbeat();
        }

        public long nextHeartbeatForBucket(int bucket) {
            long j;
            synchronized (JobSchedulerService.this.mLock) {
                j = JobSchedulerService.this.mNextBucketHeartbeat[bucket];
            }
            return j;
        }

        public long baseHeartbeatForApp(String packageName, int userId, int appStandbyBucket) {
            if (appStandbyBucket == 0 || appStandbyBucket >= JobSchedulerService.this.mConstants.STANDBY_BEATS.length) {
                if (JobSchedulerService.DEBUG_STANDBY) {
                    String str = JobSchedulerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Base heartbeat forced ZERO for new job in ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(SliceAuthority.DELIMITER);
                    stringBuilder.append(userId);
                    Slog.v(str, stringBuilder.toString());
                }
                return 0;
            }
            long baseHeartbeat = JobSchedulerService.this.heartbeatWhenJobsLastRun(packageName, userId);
            if (JobSchedulerService.DEBUG_STANDBY) {
                String str2 = JobSchedulerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Base heartbeat ");
                stringBuilder2.append(baseHeartbeat);
                stringBuilder2.append(" for new job in ");
                stringBuilder2.append(packageName);
                stringBuilder2.append(SliceAuthority.DELIMITER);
                stringBuilder2.append(userId);
                Slog.v(str2, stringBuilder2.toString());
            }
            return baseHeartbeat;
        }

        public void noteJobStart(String packageName, int userId) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.setLastJobHeartbeatLocked(packageName, userId, JobSchedulerService.this.mHeartbeat);
            }
        }

        public List<JobInfo> getSystemScheduledPendingJobs() {
            List<JobInfo> pendingJobs;
            synchronized (JobSchedulerService.this.mLock) {
                pendingJobs = new ArrayList();
                JobSchedulerService.this.mJobs.forEachJob(1000, new -$$Lambda$JobSchedulerService$LocalService$yaChpLJ2odu2Fk7A6H8erUndrN8(this, pendingJobs));
            }
            return pendingJobs;
        }

        public static /* synthetic */ void lambda$getSystemScheduledPendingJobs$0(LocalService localService, List pendingJobs, JobStatus job) {
            if (job.getJob().isPeriodic() || !JobSchedulerService.this.isCurrentlyActiveLocked(job)) {
                pendingJobs.add(job.getJob());
            }
        }

        public boolean proxyService(int type, List<String> value) {
            boolean proxyServiceLocked;
            synchronized (JobSchedulerService.this.mLock) {
                proxyServiceLocked = JobSchedulerService.this.mHwTimeController.proxyServiceLocked(type, value);
            }
            return proxyServiceLocked;
        }

        public void cancelJobsForUid(int uid, String reason) {
            JobSchedulerService.this.cancelJobsForUid(uid, reason);
        }

        public void addBackingUpUid(int uid) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mBackingUpUids.put(uid, uid);
            }
        }

        public void removeBackingUpUid(int uid) {
            synchronized (JobSchedulerService.this.mLock) {
                JobSchedulerService.this.mBackingUpUids.delete(uid);
                if (JobSchedulerService.this.mJobs.countJobsForUid(uid) > 0) {
                    JobSchedulerService.this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }

        public void clearAllBackingUpUids() {
            synchronized (JobSchedulerService.this.mLock) {
                if (JobSchedulerService.this.mBackingUpUids.size() > 0) {
                    JobSchedulerService.this.mBackingUpUids.clear();
                    JobSchedulerService.this.mHandler.obtainMessage(1).sendToTarget();
                }
            }
        }

        public void reportAppUsage(String packageName, int userId) {
            JobSchedulerService.this.reportAppUsage(packageName, userId);
        }

        public JobStorePersistStats getPersistStats() {
            JobStorePersistStats jobStorePersistStats;
            synchronized (JobSchedulerService.this.mLock) {
                jobStorePersistStats = new JobStorePersistStats(JobSchedulerService.this.mJobs.getPersistStats());
            }
            return jobStorePersistStats;
        }
    }

    static /* synthetic */ int lambda$static$0(JobStatus o1, JobStatus o2) {
        if (o1.enqueueTime < o2.enqueueTime) {
            return -1;
        }
        return o1.enqueueTime > o2.enqueueTime ? 1 : 0;
    }

    static <T> void addOrderedItem(ArrayList<T> array, T newItem, Comparator<T> comparator) {
        int where = Collections.binarySearch(array, newItem, comparator);
        if (where < 0) {
            where = ~where;
        }
        array.add(where, newItem);
    }

    private String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        return uri != null ? uri.getSchemeSpecificPart() : null;
    }

    public Context getTestableContext() {
        return getContext();
    }

    public Object getLock() {
        return this.mLock;
    }

    public JobStore getJobStore() {
        return this.mJobs;
    }

    public Constants getConstants() {
        return this.mConstants;
    }

    public void onStartUser(int userHandle) {
        this.mStartedUsers = ArrayUtils.appendInt(this.mStartedUsers, userHandle);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    public void onUnlockUser(int userHandle) {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    public void onStopUser(int userHandle) {
        this.mStartedUsers = ArrayUtils.removeInt(this.mStartedUsers, userHandle);
    }

    private boolean isUidActive(int uid) {
        return this.mAppStateTracker.isUidActiveSynced(uid);
    }

    /* JADX WARNING: Missing block: B:39:0x010b, code:
            return 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int scheduleAsPackage(JobInfo job, JobWorkItem work, int uId, String packageName, int userId, String tag) {
        Throwable th;
        JobInfo jobInfo = job;
        JobWorkItem jobWorkItem = work;
        int i = uId;
        String str = packageName;
        try {
            if (ActivityManager.getService().isAppStartModeDisabled(i, job.getService().getPackageName())) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not scheduling job ");
                stringBuilder.append(i);
                stringBuilder.append(":");
                stringBuilder.append(job.toString());
                stringBuilder.append(" -- package not allowed to start");
                Slog.w(str2, stringBuilder.toString());
                return 0;
            }
        } catch (RemoteException e) {
        }
        synchronized (this.mLock) {
            try {
                JobStatus toCancel = this.mJobs.getJobByUidAndJobId(i, job.getId());
                if (jobWorkItem == null || toCancel == null || !toCancel.getJob().equals(jobInfo)) {
                    String str3;
                    StringBuilder stringBuilder2;
                    JobStatus jobStatus = JobStatus.createFromJobInfo(jobInfo, i, str, userId, tag);
                    jobStatus.maybeAddForegroundExemption(this.mIsUidActivePredicate);
                    if (DEBUG) {
                        str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("SCHEDULE: ");
                        stringBuilder2.append(jobStatus.toShortString());
                        Slog.d(str3, stringBuilder2.toString());
                    }
                    if (str != null || this.mJobs.countJobsForUid(i) <= 100) {
                        jobStatus.prepareLocked(ActivityManager.getService());
                        if (toCancel != null) {
                            cancelJobImplLocked(toCancel, jobStatus, "job rescheduled by app");
                        }
                        if (jobWorkItem != null) {
                            jobStatus.enqueueWorkLocked(ActivityManager.getService(), jobWorkItem);
                        }
                        startTrackingJobLocked(jobStatus, toCancel);
                        JobStatus jobStatus2 = jobStatus;
                        StatsLog.write_non_chained(8, i, null, jobStatus.getBatteryName(), 2, null);
                        if (isReadyToBeExecutedLocked(jobStatus2)) {
                            this.mJobPackageTracker.notePending(jobStatus2);
                            addOrderedItem(this.mPendingJobs, jobStatus2, mEnqueueTimeComparator);
                            maybeRunPendingJobsLocked();
                        }
                    } else {
                        str3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Too many jobs for uid ");
                        stringBuilder2.append(i);
                        Slog.w(str3, stringBuilder2.toString());
                        throw new IllegalStateException("Apps may not schedule more than 100 distinct jobs");
                    }
                }
                toCancel.enqueueWorkLocked(ActivityManager.getService(), jobWorkItem);
                toCancel.maybeAddForegroundExemption(this.mIsUidActivePredicate);
                return 1;
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public List<JobInfo> getPendingJobs(int uid) {
        ArrayList<JobInfo> outList;
        synchronized (this.mLock) {
            List<JobStatus> jobs = this.mJobs.getJobsByUid(uid);
            outList = new ArrayList(jobs.size());
            for (int i = jobs.size() - 1; i >= 0; i--) {
                outList.add(((JobStatus) jobs.get(i)).getJob());
            }
        }
        return outList;
    }

    public JobInfo getPendingJob(int uid, int jobId) {
        synchronized (this.mLock) {
            List<JobStatus> jobs = this.mJobs.getJobsByUid(uid);
            for (int i = jobs.size() - 1; i >= 0; i--) {
                JobStatus job = (JobStatus) jobs.get(i);
                if (job.getJobId() == jobId) {
                    JobInfo job2 = job.getJob();
                    return job2;
                }
            }
            return null;
        }
    }

    void cancelJobsForUser(int userHandle) {
        synchronized (this.mLock) {
            List<JobStatus> jobsForUser = this.mJobs.getJobsByUser(userHandle);
            for (int i = 0; i < jobsForUser.size(); i++) {
                cancelJobImplLocked((JobStatus) jobsForUser.get(i), null, "user removed");
            }
        }
    }

    private void cancelJobsForNonExistentUsers() {
        UserManagerInternal umi = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        synchronized (this.mLock) {
            this.mJobs.removeJobsOfNonUsers(umi.getUserIds());
        }
    }

    void cancelJobsForPackageAndUid(String pkgName, int uid, String reason) {
        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(pkgName)) {
            Slog.wtfStack(TAG, "Can't cancel all jobs for system package");
            return;
        }
        synchronized (this.mLock) {
            List<JobStatus> jobsForUid = this.mJobs.getJobsByUid(uid);
            for (int i = jobsForUid.size() - 1; i >= 0; i--) {
                JobStatus job = (JobStatus) jobsForUid.get(i);
                if (job.getSourcePackageName().equals(pkgName)) {
                    cancelJobImplLocked(job, null, reason);
                }
            }
        }
    }

    public boolean cancelJobsForUid(int uid, String reason) {
        int i = 0;
        if (uid == 1000) {
            Slog.wtfStack(TAG, "Can't cancel all jobs for system uid");
            return false;
        }
        boolean jobsCanceled = false;
        synchronized (this.mLock) {
            List<JobStatus> jobsForUid = this.mJobs.getJobsByUid(uid);
            while (i < jobsForUid.size()) {
                cancelJobImplLocked((JobStatus) jobsForUid.get(i), null, reason);
                jobsCanceled = true;
                i++;
            }
        }
        return jobsCanceled;
    }

    public boolean cancelJob(int uid, int jobId, int callingUid) {
        boolean z;
        synchronized (this.mLock) {
            JobStatus toCancel = this.mJobs.getJobByUidAndJobId(uid, jobId);
            if (toCancel != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cancel() called by app, callingUid=");
                stringBuilder.append(callingUid);
                stringBuilder.append(" uid=");
                stringBuilder.append(uid);
                stringBuilder.append(" jobId=");
                stringBuilder.append(jobId);
                cancelJobImplLocked(toCancel, null, stringBuilder.toString());
            }
            z = toCancel != null;
        }
        return z;
    }

    private void cancelJobImplLocked(JobStatus cancelled, JobStatus incomingJob, String reason) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CANCEL: ");
            stringBuilder.append(cancelled.toShortString());
            Slog.d(str, stringBuilder.toString());
        }
        cancelled.unprepareLocked(ActivityManager.getService());
        stopTrackingJobLocked(cancelled, incomingJob, true);
        if (this.mPendingJobs.remove(cancelled)) {
            this.mJobPackageTracker.noteNonpending(cancelled);
        }
        stopJobOnServiceContextLocked(cancelled, 0, reason);
        reportActiveLocked();
    }

    void updateUidState(int uid, int procState) {
        synchronized (this.mLock) {
            if (procState == 2) {
                this.mUidPriorityOverride.put(uid, 40);
            } else if (procState <= 4) {
                this.mUidPriorityOverride.put(uid, 30);
            } else {
                this.mUidPriorityOverride.delete(uid);
            }
        }
    }

    public void onDeviceIdleStateChanged(boolean deviceIdle) {
        synchronized (this.mLock) {
            if (deviceIdle) {
                for (int i = 0; i < this.mActiveServices.size(); i++) {
                    JobServiceContext jsc = (JobServiceContext) this.mActiveServices.get(i);
                    JobStatus executing = jsc.getRunningJobLocked();
                    if (executing != null && (executing.getFlags() & 1) == 0) {
                        jsc.cancelExecutingJobLocked(4, "cancelled due to doze");
                    }
                }
            } else if (this.mReadyToRock) {
                if (!(this.mLocalDeviceIdleController == null || this.mReportedActive)) {
                    this.mReportedActive = true;
                    this.mLocalDeviceIdleController.setJobsActive(true);
                }
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    }

    void reportActiveLocked() {
        int i = 0;
        boolean active = this.mPendingJobs.size() > 0;
        if (this.mPendingJobs.size() <= 0) {
            while (i < this.mActiveServices.size()) {
                JobStatus job = ((JobServiceContext) this.mActiveServices.get(i)).getRunningJobLocked();
                if (job != null && (job.getJob().getFlags() & 1) == 0 && !job.dozeWhitelisted && !job.uidActive) {
                    active = true;
                    break;
                }
                i++;
            }
        }
        if (this.mReportedActive != active) {
            this.mReportedActive = active;
            if (this.mLocalDeviceIdleController != null) {
                this.mLocalDeviceIdleController.setJobsActive(active);
            }
        }
    }

    void reportAppUsage(String packageName, int userId) {
    }

    public JobSchedulerService(Context context) {
        super(context);
        this.mHandler = new JobHandler(context.getMainLooper());
        this.mConstants = new Constants();
        this.mConstantsObserver = new ConstantsObserver(this.mHandler);
        this.mJobSchedulerStub = new JobSchedulerStub();
        this.mStandbyTracker = new StandbyTracker();
        this.mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        this.mUsageStats.addAppIdleStateChangeListener(this.mStandbyTracker);
        publishLocalService(JobSchedulerInternal.class, new LocalService());
        this.mJobs = JobStore.initAndGet(this);
        this.mControllers = new ArrayList();
        this.mControllers.add(new ConnectivityController(this));
        this.mHwTimeController = new HwTimeController(this);
        this.mControllers.add(this.mHwTimeController);
        this.mControllers.add(new IdleController(this));
        this.mBatteryController = new BatteryController(this);
        this.mControllers.add(this.mBatteryController);
        this.mStorageController = new StorageController(this);
        this.mControllers.add(this.mStorageController);
        this.mControllers.add(new BackgroundJobsController(this));
        this.mControllers.add(new ContentObserverController(this));
        this.mDeviceIdleJobsController = new DeviceIdleJobsController(this);
        this.mControllers.add(this.mDeviceIdleJobsController);
        if (!this.mJobs.jobTimesInflatedValid()) {
            Slog.w(TAG, "!!! RTC not yet good; tracking time updates for job scheduling");
            context.registerReceiver(this.mTimeSetReceiver, new IntentFilter("android.intent.action.TIME_SET"));
        }
    }

    public static /* synthetic */ void lambda$new$1(JobSchedulerService jobSchedulerService) {
        ArrayList<JobStatus> toRemove = new ArrayList();
        ArrayList<JobStatus> toAdd = new ArrayList();
        synchronized (jobSchedulerService.mLock) {
            jobSchedulerService.getJobStore().getRtcCorrectedJobsLocked(toAdd, toRemove);
            int N = toAdd.size();
            for (int i = 0; i < N; i++) {
                JobStatus oldJob = (JobStatus) toRemove.get(i);
                JobStatus newJob = (JobStatus) toAdd.get(i);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  replacing ");
                    stringBuilder.append(oldJob);
                    stringBuilder.append(" with ");
                    stringBuilder.append(newJob);
                    Slog.v(str, stringBuilder.toString());
                }
                jobSchedulerService.cancelJobImplLocked(oldJob, newJob, "deferred rtc calculation");
            }
        }
    }

    public void onStart() {
        publishBinderService("jobscheduler", this.mJobSchedulerStub);
    }

    public void onBootPhase(int phase) {
        if (500 == phase) {
            this.mConstantsObserver.start(getContext().getContentResolver());
            this.mAppStateTracker = (AppStateTracker) Preconditions.checkNotNull((AppStateTracker) LocalServices.getService(AppStateTracker.class));
            setNextHeartbeatAlarm();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addAction("android.intent.action.PACKAGE_RESTARTED");
            filter.addAction("android.intent.action.QUERY_PACKAGE_RESTART");
            filter.addDataScheme("package");
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, null);
            getContext().registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, new IntentFilter("android.intent.action.USER_REMOVED"), null, null);
            try {
                ActivityManager.getService().registerUidObserver(this.mUidObserver, 15, -1, null);
            } catch (RemoteException e) {
            }
            cancelJobsForNonExistentUsers();
        } else if (phase == 600) {
            synchronized (this.mLock) {
                this.mReadyToRock = true;
                this.mBatteryStats = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
                this.mLocalDeviceIdleController = (com.android.server.DeviceIdleController.LocalService) LocalServices.getService(com.android.server.DeviceIdleController.LocalService.class);
                for (int i = 0; i < 16; i++) {
                    this.mActiveServices.add(new JobServiceContext(this, this.mBatteryStats, this.mJobPackageTracker, getContext().getMainLooper()));
                }
                this.mJobs.forEachJob(new -$$Lambda$JobSchedulerService$Lfddr1PhKRLtm92W7niRGMWO69M(this));
                this.mHandler.obtainMessage(1).sendToTarget();
            }
        }
    }

    public static /* synthetic */ void lambda$onBootPhase$2(JobSchedulerService jobSchedulerService, JobStatus job) {
        for (int controller = 0; controller < jobSchedulerService.mControllers.size(); controller++) {
            ((StateController) jobSchedulerService.mControllers.get(controller)).maybeStartTrackingJobLocked(job, null);
        }
    }

    private void startTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        if (!jobStatus.isPreparedLocked()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Not yet prepared when started tracking: ");
            stringBuilder.append(jobStatus);
            Slog.wtf(str, stringBuilder.toString());
        }
        jobStatus.enqueueTime = sElapsedRealtimeClock.millis();
        boolean update = this.mJobs.add(jobStatus);
        if (this.mReadyToRock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                StateController controller = (StateController) this.mControllers.get(i);
                if (update) {
                    controller.maybeStopTrackingJobLocked(jobStatus, null, true);
                }
                controller.maybeStartTrackingJobLocked(jobStatus, lastJob);
            }
        }
    }

    private boolean stopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean writeBack) {
        jobStatus.stopTrackingJobLocked(ActivityManager.getService(), incomingJob);
        boolean removed = this.mJobs.remove(jobStatus, writeBack);
        if (removed && this.mReadyToRock) {
            for (int i = 0; i < this.mControllers.size(); i++) {
                ((StateController) this.mControllers.get(i)).maybeStopTrackingJobLocked(jobStatus, incomingJob, false);
            }
        }
        return removed;
    }

    private boolean stopJobOnServiceContextLocked(JobStatus job, int reason, String debugReason) {
        int i = 0;
        while (i < this.mActiveServices.size()) {
            JobServiceContext jsc = (JobServiceContext) this.mActiveServices.get(i);
            JobStatus executing = jsc.getRunningJobLocked();
            if (executing == null || !executing.matches(job.getUid(), job.getJobId())) {
                i++;
            } else {
                jsc.cancelExecutingJobLocked(reason, debugReason);
                return true;
            }
        }
        return false;
    }

    private boolean isCurrentlyActiveLocked(JobStatus job) {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobStatus running = ((JobServiceContext) this.mActiveServices.get(i)).getRunningJobLocked();
            if (running != null && running.matches(job.getUid(), job.getJobId())) {
                return true;
            }
        }
        return false;
    }

    void noteJobsPending(List<JobStatus> jobs) {
        for (int i = jobs.size() - 1; i >= 0; i--) {
            this.mJobPackageTracker.notePending((JobStatus) jobs.get(i));
        }
    }

    void noteJobsNonpending(List<JobStatus> jobs) {
        for (int i = jobs.size() - 1; i >= 0; i--) {
            this.mJobPackageTracker.noteNonpending((JobStatus) jobs.get(i));
        }
    }

    private JobStatus getRescheduleJobForFailureLocked(JobStatus failureToReschedule) {
        long delayMillis;
        JobStatus jobStatus = failureToReschedule;
        long elapsedNowMillis = sElapsedRealtimeClock.millis();
        JobInfo job = failureToReschedule.getJob();
        long initialBackoffMillis = job.getInitialBackoffMillis();
        int backoffAttempts = failureToReschedule.getNumFailures() + 1;
        String str;
        StringBuilder stringBuilder;
        if (failureToReschedule.hasWorkLocked()) {
            if (backoffAttempts > this.mConstants.MAX_WORK_RESCHEDULE_COUNT) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Not rescheduling ");
                stringBuilder.append(jobStatus);
                stringBuilder.append(": attempt #");
                stringBuilder.append(backoffAttempts);
                stringBuilder.append(" > work limit ");
                stringBuilder.append(this.mConstants.MAX_STANDARD_RESCHEDULE_COUNT);
                Slog.w(str, stringBuilder.toString());
                return null;
            }
        } else if (backoffAttempts > this.mConstants.MAX_STANDARD_RESCHEDULE_COUNT) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not rescheduling ");
            stringBuilder.append(jobStatus);
            stringBuilder.append(": attempt #");
            stringBuilder.append(backoffAttempts);
            stringBuilder.append(" > std limit ");
            stringBuilder.append(this.mConstants.MAX_STANDARD_RESCHEDULE_COUNT);
            Slog.w(str, stringBuilder.toString());
            return null;
        }
        switch (job.getBackoffPolicy()) {
            case 0:
                delayMillis = initialBackoffMillis;
                if (delayMillis < this.mConstants.MIN_LINEAR_BACKOFF_TIME) {
                    delayMillis = this.mConstants.MIN_LINEAR_BACKOFF_TIME;
                }
                delayMillis *= (long) backoffAttempts;
                break;
            case 1:
                break;
            default:
                if (DEBUG) {
                    Slog.v(TAG, "Unrecognised back-off policy, defaulting to exponential.");
                    break;
                }
                break;
        }
        delayMillis = initialBackoffMillis;
        if (delayMillis < this.mConstants.MIN_EXP_BACKOFF_TIME) {
            delayMillis = this.mConstants.MIN_EXP_BACKOFF_TIME;
        }
        delayMillis = (long) Math.scalb((float) delayMillis, backoffAttempts - 1);
        JobStatus jobStatus2 = jobStatus;
        int i = backoffAttempts;
        JobStatus newJob = new JobStatus(jobStatus2, getCurrentHeartbeat(), elapsedNowMillis + Math.min(delayMillis, 18000000), JobStatus.NO_LATEST_RUNTIME, i, failureToReschedule.getLastSuccessfulRunTime(), sSystemClock.millis());
        for (int ic = 0; ic < this.mControllers.size(); ic++) {
            ((StateController) this.mControllers.get(ic)).rescheduleForFailureLocked(newJob, jobStatus);
        }
        return newJob;
    }

    private JobStatus getRescheduleJobForPeriodic(JobStatus periodicToReschedule) {
        long elapsedNow = sElapsedRealtimeClock.millis();
        long runEarly = 0;
        if (periodicToReschedule.hasDeadlineConstraint()) {
            runEarly = Math.max(periodicToReschedule.getLatestRunTimeElapsed() - elapsedNow, 0);
        }
        long newLatestRuntimeElapsed = (elapsedNow + runEarly) + periodicToReschedule.getJob().getIntervalMillis();
        long newEarliestRunTimeElapsed = newLatestRuntimeElapsed - periodicToReschedule.getJob().getFlexMillis();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Rescheduling executed periodic. New execution window [");
            stringBuilder.append(newEarliestRunTimeElapsed / 1000);
            stringBuilder.append(", ");
            stringBuilder.append(newLatestRuntimeElapsed / 1000);
            stringBuilder.append("]s");
            Slog.v(str, stringBuilder.toString());
        }
        return new JobStatus(periodicToReschedule, getCurrentHeartbeat(), newEarliestRunTimeElapsed, newLatestRuntimeElapsed, 0, sSystemClock.millis(), periodicToReschedule.getLastFailedRunTime());
    }

    long heartbeatWhenJobsLastRun(String packageName, int userId) {
        long heartbeat = (long) (-this.mConstants.STANDBY_BEATS[3]);
        boolean cacheHit = false;
        synchronized (this.mLock) {
            long cachedValue;
            HashMap<String, Long> jobPackages = (HashMap) this.mLastJobHeartbeats.get(userId);
            if (jobPackages != null) {
                cachedValue = ((Long) jobPackages.getOrDefault(packageName, Long.valueOf(JobStatus.NO_LATEST_RUNTIME))).longValue();
                if (cachedValue < JobStatus.NO_LATEST_RUNTIME) {
                    cacheHit = true;
                    heartbeat = cachedValue;
                }
            }
            if (!cacheHit) {
                cachedValue = this.mUsageStats.getTimeSinceLastJobRun(packageName, userId);
                if (cachedValue < JobStatus.NO_LATEST_RUNTIME) {
                    heartbeat = this.mHeartbeat - (cachedValue / this.mConstants.STANDBY_HEARTBEAT_TIME);
                }
                setLastJobHeartbeatLocked(packageName, userId, heartbeat);
            }
        }
        if (DEBUG_STANDBY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Last job heartbeat ");
            stringBuilder.append(heartbeat);
            stringBuilder.append(" for ");
            stringBuilder.append(packageName);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(userId);
            Slog.v(str, stringBuilder.toString());
        }
        return heartbeat;
    }

    long heartbeatWhenJobsLastRun(JobStatus job) {
        return heartbeatWhenJobsLastRun(job.getSourcePackageName(), job.getSourceUserId());
    }

    void setLastJobHeartbeatLocked(String packageName, int userId, long heartbeat) {
        HashMap<String, Long> jobPackages = (HashMap) this.mLastJobHeartbeats.get(userId);
        if (jobPackages == null) {
            jobPackages = new HashMap();
            this.mLastJobHeartbeats.put(userId, jobPackages);
        }
        jobPackages.put(packageName, Long.valueOf(heartbeat));
    }

    public void onJobCompletedLocked(JobStatus jobStatus, boolean needsReschedule) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Completed ");
            stringBuilder.append(jobStatus);
            stringBuilder.append(", reschedule=");
            stringBuilder.append(needsReschedule);
            Slog.d(str, stringBuilder.toString());
        }
        JobStatus rescheduledJob = needsReschedule ? getRescheduleJobForFailureLocked(jobStatus) : null;
        if (stopTrackingJobLocked(jobStatus, rescheduledJob, jobStatus.getJob().isPeriodic() ^ 1)) {
            if (rescheduledJob != null) {
                try {
                    rescheduledJob.prepareLocked(ActivityManager.getService());
                } catch (SecurityException e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to regrant job permissions for ");
                    stringBuilder2.append(rescheduledJob);
                    Slog.w(str2, stringBuilder2.toString());
                }
                startTrackingJobLocked(rescheduledJob, jobStatus);
            } else if (jobStatus.getJob().isPeriodic()) {
                JobStatus rescheduledPeriodic = getRescheduleJobForPeriodic(jobStatus);
                try {
                    rescheduledPeriodic.prepareLocked(ActivityManager.getService());
                } catch (SecurityException e2) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unable to regrant job permissions for ");
                    stringBuilder3.append(rescheduledPeriodic);
                    Slog.w(str3, stringBuilder3.toString());
                }
                startTrackingJobLocked(rescheduledPeriodic, jobStatus);
            }
            jobStatus.unprepareLocked(ActivityManager.getService());
            reportActiveLocked();
            this.mHandler.obtainMessage(3).sendToTarget();
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "Could not find job to remove. Was job removed while executing?");
        }
        this.mHandler.obtainMessage(3).sendToTarget();
    }

    public void onControllerStateChanged() {
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    public void onRunJobNow(JobStatus jobStatus) {
        this.mHandler.obtainMessage(0, jobStatus).sendToTarget();
    }

    private void stopNonReadyActiveJobsLocked() {
        for (int i = 0; i < this.mActiveServices.size(); i++) {
            JobServiceContext serviceContext = (JobServiceContext) this.mActiveServices.get(i);
            JobStatus running = serviceContext.getRunningJobLocked();
            if (!(running == null || running.isReady())) {
                serviceContext.cancelExecutingJobLocked(1, "cancelled due to unsatisfied constraints");
            }
        }
    }

    private void queueReadyJobsForExecutionLocked() {
        Slog.d(TAG, "queuing all ready jobs for execution:");
        noteJobsNonpending(this.mPendingJobs);
        this.mPendingJobs.clear();
        stopNonReadyActiveJobsLocked();
        this.mJobs.forEachJob(this.mReadyQueueFunctor);
        this.mReadyQueueFunctor.postProcess();
        if (DEBUG) {
            int queuedJobs = this.mPendingJobs.size();
            if (queuedJobs == 0) {
                Slog.d(TAG, "No jobs pending.");
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(queuedJobs);
            stringBuilder.append(" jobs queued.");
            Slog.d(str, stringBuilder.toString());
        }
    }

    private void maybeQueueReadyJobsForExecutionLocked() {
        Slog.d(TAG, "Maybe queuing ready jobs...");
        noteJobsNonpending(this.mPendingJobs);
        this.mPendingJobs.clear();
        stopNonReadyActiveJobsLocked();
        this.mJobs.forEachJob(this.mMaybeQueueFunctor);
        this.mMaybeQueueFunctor.postProcess();
    }

    void advanceHeartbeatLocked(long beatsElapsed) {
        this.mHeartbeat += beatsElapsed;
        if (DEBUG_STANDBY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Advancing standby heartbeat by ");
            stringBuilder.append(beatsElapsed);
            stringBuilder.append(" to ");
            stringBuilder.append(this.mHeartbeat);
            Slog.v(str, stringBuilder.toString());
        }
        boolean didAdvanceBucket = false;
        for (int i = 1; i < this.mNextBucketHeartbeat.length - 1; i++) {
            if (this.mHeartbeat >= this.mNextBucketHeartbeat[i]) {
                didAdvanceBucket = true;
            }
            while (this.mHeartbeat > this.mNextBucketHeartbeat[i]) {
                long[] jArr = this.mNextBucketHeartbeat;
                jArr[i] = jArr[i] + ((long) this.mConstants.STANDBY_BEATS[i]);
            }
            if (DEBUG_STANDBY) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("   Bucket ");
                stringBuilder2.append(i);
                stringBuilder2.append(" next heartbeat ");
                stringBuilder2.append(this.mNextBucketHeartbeat[i]);
                Slog.v(str2, stringBuilder2.toString());
            }
        }
        if (didAdvanceBucket) {
            if (DEBUG_STANDBY) {
                Slog.v(TAG, "Hit bucket boundary; reevaluating job runnability");
            }
            this.mHandler.obtainMessage(1).sendToTarget();
        }
    }

    void setNextHeartbeatAlarm() {
        long heartbeatLength;
        synchronized (this.mLock) {
            heartbeatLength = this.mConstants.STANDBY_HEARTBEAT_TIME;
        }
        long now = sElapsedRealtimeClock.millis();
        long nextHeartbeat = ((now + heartbeatLength) / heartbeatLength) * heartbeatLength;
        if (DEBUG_STANDBY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting heartbeat alarm for ");
            stringBuilder.append(nextHeartbeat);
            stringBuilder.append(" = ");
            stringBuilder.append(TimeUtils.formatDuration(nextHeartbeat - now));
            Slog.i(str, stringBuilder.toString());
        }
        ((AlarmManager) getContext().getSystemService("alarm")).setExact(3, nextHeartbeat, HEARTBEAT_TAG, this.mHeartbeatAlarm, this.mHandler);
    }

    private boolean isReadyToBeExecutedLocked(JobStatus job) {
        boolean jobReady = job.isReady();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isReadyToBeExecutedLocked: ");
            stringBuilder.append(job.toShortString());
            stringBuilder.append(" ready=");
            stringBuilder.append(jobReady);
            Slog.v(str, stringBuilder.toString());
        }
        boolean componentPresent = false;
        if (jobReady) {
            boolean jobExists = this.mJobs.containsJob(job);
            int userId = job.getUserId();
            boolean userStarted = ArrayUtils.contains(this.mStartedUsers, userId);
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("isReadyToBeExecutedLocked: ");
                stringBuilder2.append(job.toShortString());
                stringBuilder2.append(" exists=");
                stringBuilder2.append(jobExists);
                stringBuilder2.append(" userStarted=");
                stringBuilder2.append(userStarted);
                Slog.v(str2, stringBuilder2.toString());
            }
            if (!jobExists || !userStarted) {
                return false;
            }
            String str3;
            StringBuilder stringBuilder3;
            boolean jobPending = this.mPendingJobs.contains(job);
            boolean jobActive = isCurrentlyActiveLocked(job);
            if (DEBUG) {
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isReadyToBeExecutedLocked: ");
                stringBuilder3.append(job.toShortString());
                stringBuilder3.append(" pending=");
                stringBuilder3.append(jobPending);
                stringBuilder3.append(" active=");
                stringBuilder3.append(jobActive);
                Slog.v(str3, stringBuilder3.toString());
            }
            if (jobPending || jobActive) {
                return false;
            }
            if (DEBUG_STANDBY) {
                str3 = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("isReadyToBeExecutedLocked: ");
                stringBuilder3.append(job.toShortString());
                stringBuilder3.append(" parole=");
                stringBuilder3.append(this.mInParole);
                stringBuilder3.append(" active=");
                stringBuilder3.append(job.uidActive);
                stringBuilder3.append(" exempt=");
                stringBuilder3.append(job.getJob().isExemptedFromAppStandby());
                Slog.v(str3, stringBuilder3.toString());
            }
            if (!(this.mInParole || job.uidActive || job.getJob().isExemptedFromAppStandby())) {
                int bucket = job.getStandbyBucket();
                if (DEBUG_STANDBY) {
                    String str4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("  bucket=");
                    stringBuilder4.append(bucket);
                    stringBuilder4.append(" heartbeat=");
                    stringBuilder4.append(this.mHeartbeat);
                    stringBuilder4.append(" next=");
                    stringBuilder4.append(this.mNextBucketHeartbeat[bucket]);
                    Slog.v(str4, stringBuilder4.toString());
                }
                if (this.mHeartbeat < this.mNextBucketHeartbeat[bucket]) {
                    long appLastRan = heartbeatWhenJobsLastRun(job);
                    String str5;
                    StringBuilder stringBuilder5;
                    if (bucket >= this.mConstants.STANDBY_BEATS.length || (this.mHeartbeat > appLastRan && this.mHeartbeat < ((long) this.mConstants.STANDBY_BEATS[bucket]) + appLastRan)) {
                        if (job.getWhenStandbyDeferred() == 0) {
                            if (DEBUG_STANDBY) {
                                str5 = TAG;
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("Bucket deferral: ");
                                stringBuilder5.append(this.mHeartbeat);
                                stringBuilder5.append(" < ");
                                stringBuilder5.append(((long) this.mConstants.STANDBY_BEATS[bucket]) + appLastRan);
                                stringBuilder5.append(" for ");
                                stringBuilder5.append(job);
                                Slog.v(str5, stringBuilder5.toString());
                            }
                            job.setWhenStandbyDeferred(sElapsedRealtimeClock.millis());
                        }
                        return false;
                    } else if (DEBUG_STANDBY) {
                        str5 = TAG;
                        stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Bucket deferred job aged into runnability at ");
                        stringBuilder5.append(this.mHeartbeat);
                        stringBuilder5.append(" : ");
                        stringBuilder5.append(job);
                        Slog.v(str5, stringBuilder5.toString());
                    }
                }
            }
            try {
                if (AppGlobals.getPackageManager().getServiceInfo(job.getServiceComponent(), 268435456, userId) != null) {
                    componentPresent = true;
                }
                if (DEBUG) {
                    str3 = TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("isReadyToBeExecutedLocked: ");
                    stringBuilder3.append(job.toShortString());
                    stringBuilder3.append(" componentPresent=");
                    stringBuilder3.append(componentPresent);
                    Slog.v(str3, stringBuilder3.toString());
                }
                return componentPresent;
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
        if (job.getSourcePackageName().equals("android.jobscheduler.cts.jobtestapp")) {
            String str6 = TAG;
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append("    NOT READY: ");
            stringBuilder6.append(job);
            Slog.v(str6, stringBuilder6.toString());
        }
        return false;
    }

    private void maybeRunPendingJobsLocked() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pending queue: ");
            stringBuilder.append(this.mPendingJobs.size());
            stringBuilder.append(" jobs.");
            Slog.d(str, stringBuilder.toString());
        }
        assignJobsToContextsLocked();
        reportActiveLocked();
    }

    private int adjustJobPriority(int curPriority, JobStatus job) {
        if (curPriority >= 40) {
            return curPriority;
        }
        float factor = this.mJobPackageTracker.getLoadFactor(job);
        if (factor >= this.mConstants.HEAVY_USE_FACTOR) {
            return curPriority - 80;
        }
        if (factor >= this.mConstants.MODERATE_USE_FACTOR) {
            return curPriority - 40;
        }
        return curPriority;
    }

    private int evaluateJobPriorityLocked(JobStatus job) {
        int priority = job.getPriority();
        if (priority >= 30) {
            return adjustJobPriority(priority, job);
        }
        int override = this.mUidPriorityOverride.get(job.getSourceUid(), 0);
        if (override != 0) {
            return adjustJobPriority(override, job);
        }
        return adjustJobPriority(priority, job);
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x0116  */
    /* JADX WARNING: Removed duplicated region for block: B:58:0x0108  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void assignJobsToContextsLocked() {
        int memLevel;
        if (DEBUG) {
            Slog.d(TAG, printPendingQueue());
        }
        boolean z = false;
        try {
            memLevel = ActivityManager.getService().getMemoryTrimLevel();
        } catch (RemoteException e) {
            memLevel = 0;
        }
        switch (memLevel) {
            case 1:
                this.mMaxActiveJobs = this.mConstants.BG_MODERATE_JOB_COUNT;
                break;
            case 2:
                this.mMaxActiveJobs = this.mConstants.BG_LOW_JOB_COUNT;
                break;
            case 3:
                this.mMaxActiveJobs = this.mConstants.BG_CRITICAL_JOB_COUNT;
                break;
            default:
                this.mMaxActiveJobs = this.mConstants.BG_NORMAL_JOB_COUNT;
                break;
        }
        JobStatus[] contextIdToJobMap = this.mTmpAssignContextIdToJobMap;
        boolean[] act = this.mTmpAssignAct;
        int[] preferredUidForContext = this.mTmpAssignPreferredUidForContext;
        int numForeground = 0;
        int numActive = 0;
        int i = 0;
        while (true) {
            int i2 = 16;
            JobStatus status;
            if (i < 16) {
                JobServiceContext js = (JobServiceContext) this.mActiveServices.get(i);
                status = js.getRunningJobLocked();
                contextIdToJobMap[i] = status;
                if (status != null) {
                    numActive++;
                    if (status.lastEvaluatedPriority >= 40) {
                        numForeground++;
                    }
                }
                act[i] = false;
                preferredUidForContext[i] = js.getPreferredUid();
                i++;
            } else {
                JobStatus job;
                if (DEBUG) {
                    Slog.d(TAG, printContextIdToJobMap(contextIdToJobMap, "running jobs initial"));
                }
                i = 0;
                while (i < this.mPendingJobs.size()) {
                    int memLevel2;
                    status = (JobStatus) this.mPendingJobs.get(i);
                    if (findJobContextIdFromMap(status, contextIdToJobMap) != -1) {
                        memLevel2 = memLevel;
                    } else {
                        int priority = evaluateJobPriorityLocked(status);
                        status.lastEvaluatedPriority = priority;
                        int minPriorityContextId = -1;
                        int j = z;
                        int minPriority = HwBootFail.STAGE_BOOT_SUCCESS;
                        for (i2 = 
/*
Method generation error in method: com.android.server.job.JobSchedulerService.assignJobsToContextsLocked():void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r10_4 'i2' int) = (r10_0 'i2' int), (r10_8 'i2' int) binds: {(r10_0 'i2' int)=B:25:0x007f, (r10_8 'i2' int)=B:62:0x0118} in method: com.android.server.job.JobSchedulerService.assignJobsToContextsLocked():void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:183)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:61)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:128)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:57)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:218)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:61)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:128)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:57)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:173)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:61)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:173)
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
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 35 more

*/

    int findJobContextIdFromMap(JobStatus jobStatus, JobStatus[] map) {
        int i = 0;
        while (i < map.length) {
            if (map[i] != null && map[i].matches(jobStatus.getUid(), jobStatus.getJobId())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static int standbyBucketToBucketIndex(int bucket) {
        if (bucket == 50) {
            return 4;
        }
        if (bucket > 30) {
            return 3;
        }
        if (bucket > 20) {
            return 2;
        }
        if (bucket > 10) {
            return 1;
        }
        return 0;
    }

    public static int standbyBucketForPackage(String packageName, int userId, long elapsedNow) {
        int bucket;
        UsageStatsManagerInternal usageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
        if (usageStats != null) {
            bucket = usageStats.getAppStandbyBucket(packageName, userId, elapsedNow);
        } else {
            bucket = 0;
        }
        bucket = standbyBucketToBucketIndex(bucket);
        if (DEBUG_STANDBY) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(packageName);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(userId);
            stringBuilder.append(" standby bucket index: ");
            stringBuilder.append(bucket);
            Slog.v(str, stringBuilder.toString());
        }
        return bucket;
    }

    int executeRunCommand(String pkgName, int userId, int jobId, boolean force) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("executeRunCommand(): ");
            stringBuilder.append(pkgName);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(userId);
            stringBuilder.append(" ");
            stringBuilder.append(jobId);
            stringBuilder.append(" f=");
            stringBuilder.append(force);
            Slog.v(str, stringBuilder.toString());
        }
        try {
            int uid = AppGlobals.getPackageManager().getPackageUid(pkgName, 0, userId != -1 ? userId : 0);
            if (uid < 0) {
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            synchronized (this.mLock) {
                JobStatus js = this.mJobs.getJobByUidAndJobId(uid, jobId);
                if (js == null) {
                    return JobSchedulerShellCommand.CMD_ERR_NO_JOB;
                }
                js.overrideState = force ? 2 : 1;
                if (js.isConstraintsSatisfied()) {
                    queueReadyJobsForExecutionLocked();
                    maybeRunPendingJobsLocked();
                } else {
                    js.overrideState = 0;
                    return JobSchedulerShellCommand.CMD_ERR_CONSTRAINTS;
                }
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    int executeTimeoutCommand(PrintWriter pw, String pkgName, int userId, boolean hasJobId, int jobId) {
        String str;
        int i;
        int i2;
        PrintWriter printWriter = pw;
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("executeTimeoutCommand(): ");
            str = pkgName;
            stringBuilder.append(str);
            stringBuilder.append(SliceAuthority.DELIMITER);
            i = userId;
            stringBuilder.append(i);
            stringBuilder.append(" ");
            i2 = jobId;
            stringBuilder.append(i2);
            Slog.v(str2, stringBuilder.toString());
        } else {
            str = pkgName;
            i = userId;
            i2 = jobId;
        }
        synchronized (this.mLock) {
            boolean foundSome = false;
            for (int i3 = 0; i3 < this.mActiveServices.size(); i3++) {
                JobServiceContext jc = (JobServiceContext) this.mActiveServices.get(i3);
                JobStatus js = jc.getRunningJobLocked();
                if (jc.timeoutIfExecutingLocked(str, i, hasJobId, i2, "shell")) {
                    printWriter.print("Timing out: ");
                    js.printUniqueId(printWriter);
                    printWriter.print(" ");
                    printWriter.println(js.getServiceComponent().flattenToShortString());
                    foundSome = true;
                }
            }
            if (!foundSome) {
                printWriter.println("No matching executing jobs found.");
            }
        }
        return 0;
    }

    int executeCancelCommand(PrintWriter pw, String pkgName, int userId, boolean hasJobId, int jobId) {
        StringBuilder stringBuilder;
        if (DEBUG) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("executeCancelCommand(): ");
            stringBuilder.append(pkgName);
            stringBuilder.append(SliceAuthority.DELIMITER);
            stringBuilder.append(userId);
            stringBuilder.append(" ");
            stringBuilder.append(jobId);
            Slog.v(str, stringBuilder.toString());
        }
        int pkgUid = -1;
        try {
            pkgUid = AppGlobals.getPackageManager().getPackageUid(pkgName, 0, userId);
        } catch (RemoteException e) {
        }
        if (pkgUid < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(pkgName);
            stringBuilder.append(" not found.");
            pw.println(stringBuilder.toString());
            return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
        }
        StringBuilder stringBuilder2;
        if (hasJobId) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Canceling job ");
            stringBuilder2.append(pkgName);
            stringBuilder2.append("/#");
            stringBuilder2.append(jobId);
            stringBuilder2.append(" in user ");
            stringBuilder2.append(userId);
            pw.println(stringBuilder2.toString());
            if (!cancelJob(pkgUid, jobId, IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME)) {
                pw.println("No matching job found.");
            }
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Canceling all jobs for ");
            stringBuilder2.append(pkgName);
            stringBuilder2.append(" in user ");
            stringBuilder2.append(userId);
            pw.println(stringBuilder2.toString());
            if (!cancelJobsForUid(pkgUid, "cancel shell command for package")) {
                pw.println("No matching jobs found.");
            }
        }
        return 0;
    }

    void setMonitorBattery(boolean enabled) {
        synchronized (this.mLock) {
            if (this.mBatteryController != null) {
                this.mBatteryController.getTracker().setMonitorBatteryLocked(enabled);
            }
        }
    }

    int getBatterySeq() {
        int seq;
        synchronized (this.mLock) {
            seq = this.mBatteryController != null ? this.mBatteryController.getTracker().getSeq() : -1;
        }
        return seq;
    }

    boolean getBatteryCharging() {
        boolean isOnStablePower;
        synchronized (this.mLock) {
            isOnStablePower = this.mBatteryController != null ? this.mBatteryController.getTracker().isOnStablePower() : false;
        }
        return isOnStablePower;
    }

    boolean getBatteryNotLow() {
        boolean isBatteryNotLow;
        synchronized (this.mLock) {
            isBatteryNotLow = this.mBatteryController != null ? this.mBatteryController.getTracker().isBatteryNotLow() : false;
        }
        return isBatteryNotLow;
    }

    int getStorageSeq() {
        int seq;
        synchronized (this.mLock) {
            seq = this.mStorageController != null ? this.mStorageController.getTracker().getSeq() : -1;
        }
        return seq;
    }

    boolean getStorageNotLow() {
        boolean isStorageNotLow;
        synchronized (this.mLock) {
            isStorageNotLow = this.mStorageController != null ? this.mStorageController.getTracker().isStorageNotLow() : false;
        }
        return isStorageNotLow;
    }

    long getCurrentHeartbeat() {
        long j;
        synchronized (this.mLock) {
            j = this.mHeartbeat;
        }
        return j;
    }

    int getJobState(PrintWriter pw, String pkgName, int userId, int jobId) {
        try {
            int uid = AppGlobals.getPackageManager().getPackageUid(pkgName, 0, userId != -1 ? userId : 0);
            if (uid < 0) {
                pw.print("unknown(");
                pw.print(pkgName);
                pw.println(")");
                return JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            synchronized (this.mLock) {
                JobStatus js = this.mJobs.getJobByUidAndJobId(uid, jobId);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("get-job-state ");
                    stringBuilder.append(uid);
                    stringBuilder.append(SliceAuthority.DELIMITER);
                    stringBuilder.append(jobId);
                    stringBuilder.append(": ");
                    stringBuilder.append(js);
                    Slog.d(str, stringBuilder.toString());
                }
                if (js == null) {
                    pw.print("unknown(");
                    UserHandle.formatUid(pw, uid);
                    pw.print("/jid");
                    pw.print(jobId);
                    pw.println(")");
                    return JobSchedulerShellCommand.CMD_ERR_NO_JOB;
                }
                boolean printed = false;
                if (this.mPendingJobs.contains(js)) {
                    pw.print("pending");
                    printed = true;
                }
                if (isCurrentlyActiveLocked(js)) {
                    if (printed) {
                        pw.print(" ");
                    }
                    printed = true;
                    pw.println("active");
                }
                if (!ArrayUtils.contains(this.mStartedUsers, js.getUserId())) {
                    if (printed) {
                        pw.print(" ");
                    }
                    printed = true;
                    pw.println("user-stopped");
                }
                if (this.mBackingUpUids.indexOfKey(js.getSourceUid()) >= 0) {
                    if (printed) {
                        pw.print(" ");
                    }
                    printed = true;
                    pw.println("backing-up");
                }
                boolean componentPresent = false;
                try {
                    componentPresent = AppGlobals.getPackageManager().getServiceInfo(js.getServiceComponent(), 268435456, js.getUserId()) != null;
                } catch (RemoteException e) {
                }
                if (!componentPresent) {
                    if (printed) {
                        pw.print(" ");
                    }
                    printed = true;
                    pw.println("no-component");
                }
                if (js.isReady()) {
                    if (printed) {
                        pw.print(" ");
                    }
                    printed = true;
                    pw.println("ready");
                }
                if (!printed) {
                    pw.print("waiting");
                }
                pw.println();
            }
        } catch (RemoteException e2) {
        }
        return 0;
    }

    int executeHeartbeatCommand(PrintWriter pw, int numBeats) {
        if (numBeats < 1) {
            pw.println(getCurrentHeartbeat());
            return 0;
        }
        pw.print("Advancing standby heartbeat by ");
        pw.println(numBeats);
        synchronized (this.mLock) {
            advanceHeartbeatLocked((long) numBeats);
        }
        return 0;
    }

    void triggerDockState(boolean idleState) {
        Intent dockIntent;
        if (idleState) {
            dockIntent = new Intent("android.intent.action.DOCK_IDLE");
        } else {
            dockIntent = new Intent("android.intent.action.DOCK_ACTIVE");
        }
        dockIntent.setPackage(PackageManagerService.PLATFORM_PACKAGE_NAME);
        dockIntent.addFlags(1342177280);
        getContext().sendBroadcastAsUser(dockIntent, UserHandle.ALL);
    }

    private String printContextIdToJobMap(JobStatus[] map, String initial) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(initial);
        stringBuilder.append(": ");
        StringBuilder s = new StringBuilder(stringBuilder.toString());
        for (int i = 0; i < map.length; i++) {
            s.append("(");
            int i2 = -1;
            s.append(map[i] == null ? -1 : map[i].getJobId());
            if (map[i] != null) {
                i2 = map[i].getUid();
            }
            s.append(i2);
            s.append(")");
        }
        return s.toString();
    }

    private String printPendingQueue() {
        StringBuilder s = new StringBuilder("Pending queue: ");
        Iterator<JobStatus> it = this.mPendingJobs.iterator();
        while (it.hasNext()) {
            JobStatus js = (JobStatus) it.next();
            s.append("(");
            s.append(js.getJob().getId());
            s.append(", ");
            s.append(js.getUid());
            s.append(") ");
        }
        return s.toString();
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Job Scheduler (jobscheduler) dump options:");
        pw.println("  [-h] [package] ...");
        pw.println("    -h: print this help");
        pw.println("  [package] is an optional package name to limit the output to.");
    }

    private static void sortJobs(List<JobStatus> jobs) {
        Collections.sort(jobs, new Comparator<JobStatus>() {
            public int compare(JobStatus o1, JobStatus o2) {
                int uid1 = o1.getUid();
                int uid2 = o2.getUid();
                int id1 = o1.getJobId();
                int id2 = o2.getJobId();
                int i = 1;
                if (uid1 != uid2) {
                    if (uid1 < uid2) {
                        i = -1;
                    }
                    return i;
                }
                if (id1 < id2) {
                    i = -1;
                } else if (id1 <= id2) {
                    i = 0;
                }
                return i;
            }
        });
    }

    void dumpInternal(IndentingPrintWriter pw, int filterUid) {
        Throwable th;
        PrintWriter printWriter = pw;
        int filterUidFinal = UserHandle.getAppId(filterUid);
        long nowElapsed = sElapsedRealtimeClock.millis();
        long nowUptime = sUptimeMillisClock.millis();
        Predicate<JobStatus> predicate = new -$$Lambda$JobSchedulerService$e8zIA2HHN2tnGMuc6TZ2xWw_c20(filterUidFinal);
        synchronized (this.mLock) {
            long nowUptime2;
            int i;
            try {
                boolean componentPresent;
                int i2;
                this.mConstants.dump(printWriter);
                pw.println();
                printWriter.println("  Heartbeat:");
                printWriter.print("    Current:    ");
                printWriter.println(this.mHeartbeat);
                printWriter.println("    Next");
                printWriter.print("      ACTIVE:   ");
                int i3 = 0;
                printWriter.println(this.mNextBucketHeartbeat[0]);
                printWriter.print("      WORKING:  ");
                printWriter.println(this.mNextBucketHeartbeat[1]);
                printWriter.print("      FREQUENT: ");
                printWriter.println(this.mNextBucketHeartbeat[2]);
                printWriter.print("      RARE:     ");
                printWriter.println(this.mNextBucketHeartbeat[3]);
                printWriter.print("    Last heartbeat: ");
                TimeUtils.formatDuration(this.mLastHeartbeatTime, nowElapsed, printWriter);
                pw.println();
                printWriter.print("    Next heartbeat: ");
                TimeUtils.formatDuration(this.mLastHeartbeatTime + this.mConstants.STANDBY_HEARTBEAT_TIME, nowElapsed, printWriter);
                pw.println();
                printWriter.print("    In parole?: ");
                printWriter.print(this.mInParole);
                pw.println();
                pw.println();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Started users: ");
                stringBuilder.append(Arrays.toString(this.mStartedUsers));
                printWriter.println(stringBuilder.toString());
                printWriter.print("Registered ");
                printWriter.print(this.mJobs.size());
                printWriter.println(" jobs:");
                if (this.mJobs.size() > 0) {
                    try {
                        List<JobStatus> jobs = this.mJobs.mJobSet.getAllJobs();
                        sortJobs(jobs);
                        Iterator it = jobs.iterator();
                        while (it.hasNext()) {
                            JobStatus job = (JobStatus) it.next();
                            printWriter.print("  JOB #");
                            job.printUniqueId(printWriter);
                            printWriter.print(": ");
                            printWriter.println(job.toShortStringExceptUniqueId());
                            if (predicate.test(job)) {
                                nowUptime2 = nowUptime;
                                nowUptime = job;
                                List<JobStatus> jobs2 = jobs;
                                Iterator it2 = it;
                                job.dump(printWriter, "    ", true, nowElapsed);
                                printWriter.print("    Last run heartbeat: ");
                                printWriter.print(heartbeatWhenJobsLastRun(nowUptime));
                                pw.println();
                                printWriter.print("    Ready: ");
                                printWriter.print(isReadyToBeExecutedLocked(nowUptime));
                                printWriter.print(" (job=");
                                printWriter.print(nowUptime.isReady());
                                printWriter.print(" user=");
                                printWriter.print(ArrayUtils.contains(this.mStartedUsers, nowUptime.getUserId()));
                                printWriter.print(" !pending=");
                                printWriter.print(this.mPendingJobs.contains(nowUptime) ^ 1);
                                printWriter.print(" !active=");
                                printWriter.print(isCurrentlyActiveLocked(nowUptime) ^ 1);
                                printWriter.print(" !backingup=");
                                printWriter.print(this.mBackingUpUids.indexOfKey(nowUptime.getSourceUid()) < 0);
                                printWriter.print(" comp=");
                                componentPresent = false;
                                try {
                                    componentPresent = AppGlobals.getPackageManager().getServiceInfo(nowUptime.getServiceComponent(), 268435456, nowUptime.getUserId()) != null;
                                } catch (RemoteException e) {
                                }
                                printWriter.print(componentPresent);
                                printWriter.println(")");
                                jobs = jobs2;
                                nowUptime = nowUptime2;
                                it = it2;
                            }
                        }
                        nowUptime2 = nowUptime;
                    } catch (Throwable th2) {
                        th = th2;
                        i = filterUid;
                        throw th;
                    }
                }
                nowUptime2 = nowUptime;
                printWriter.println("  None.");
                for (i2 = 0; i2 < this.mControllers.size(); i2++) {
                    pw.println();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(((StateController) this.mControllers.get(i2)).getClass().getSimpleName());
                    stringBuilder2.append(":");
                    printWriter.println(stringBuilder2.toString());
                    pw.increaseIndent();
                    ((StateController) this.mControllers.get(i2)).dumpControllerStateLocked(printWriter, predicate);
                    pw.decreaseIndent();
                }
                pw.println();
                printWriter.println("Uid priority overrides:");
                for (i2 = 0; i2 < this.mUidPriorityOverride.size(); i2++) {
                    i = this.mUidPriorityOverride.keyAt(i2);
                    if (filterUidFinal == -1 || filterUidFinal == UserHandle.getAppId(i)) {
                        printWriter.print("  ");
                        printWriter.print(UserHandle.formatUid(i));
                        printWriter.print(": ");
                        printWriter.println(this.mUidPriorityOverride.valueAt(i2));
                    }
                }
                if (this.mBackingUpUids.size() > 0) {
                    pw.println();
                    printWriter.println("Backing up uids:");
                    componentPresent = true;
                    for (i2 = 0; i2 < this.mBackingUpUids.size(); i2++) {
                        int uid = this.mBackingUpUids.keyAt(i2);
                        if (filterUidFinal == -1 || filterUidFinal == UserHandle.getAppId(uid)) {
                            if (componentPresent) {
                                printWriter.print("  ");
                                componentPresent = false;
                            } else {
                                printWriter.print(", ");
                            }
                            printWriter.print(UserHandle.formatUid(uid));
                        }
                    }
                    pw.println();
                }
                pw.println();
                this.mJobPackageTracker.dump(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, filterUidFinal);
                pw.println();
                if (this.mJobPackageTracker.dumpHistory(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, filterUidFinal)) {
                    pw.println();
                }
                printWriter.println("Pending queue:");
                for (i2 = 0; i2 < this.mPendingJobs.size(); i2++) {
                    JobStatus job2 = (JobStatus) this.mPendingJobs.get(i2);
                    printWriter.print("  Pending #");
                    printWriter.print(i2);
                    printWriter.print(": ");
                    printWriter.println(job2.toShortString());
                    job2.dump(printWriter, "    ", false, nowElapsed);
                    i = evaluateJobPriorityLocked(job2);
                    if (i != 0) {
                        printWriter.print("    Evaluated priority: ");
                        printWriter.println(i);
                    }
                    printWriter.print("    Tag: ");
                    printWriter.println(job2.getTag());
                    printWriter.print("    Enq: ");
                    TimeUtils.formatDuration(job2.madePending - nowUptime2, printWriter);
                    pw.println();
                }
                pw.println();
                printWriter.println("Active jobs:");
                while (true) {
                    i2 = i3;
                    if (i2 >= this.mActiveServices.size()) {
                        break;
                    }
                    JobServiceContext jsc = (JobServiceContext) this.mActiveServices.get(i2);
                    printWriter.print("  Slot #");
                    printWriter.print(i2);
                    printWriter.print(": ");
                    JobStatus job3 = jsc.getRunningJobLocked();
                    if (job3 != null) {
                        printWriter.println(job3.toShortString());
                        printWriter.print("    Running for: ");
                        TimeUtils.formatDuration(nowElapsed - jsc.getExecutionStartTimeElapsed(), printWriter);
                        printWriter.print(", timeout at: ");
                        TimeUtils.formatDuration(jsc.getTimeoutElapsed() - nowElapsed, printWriter);
                        pw.println();
                        JobStatus job4 = job3;
                        job3.dump(printWriter, "    ", false, nowElapsed);
                        i = evaluateJobPriorityLocked(jsc.getRunningJobLocked());
                        if (i != 0) {
                            printWriter.print("    Evaluated priority: ");
                            printWriter.println(i);
                        }
                        printWriter.print("    Active at ");
                        TimeUtils.formatDuration(job4.madeActive - nowUptime2, printWriter);
                        printWriter.print(", pending for ");
                        TimeUtils.formatDuration(job4.madeActive - job4.madePending, printWriter);
                        pw.println();
                    } else if (jsc.mStoppedReason != null) {
                        printWriter.print("inactive since ");
                        TimeUtils.formatDuration(jsc.mStoppedTime, nowElapsed, printWriter);
                        printWriter.print(", stopped because: ");
                        printWriter.println(jsc.mStoppedReason);
                    } else {
                        printWriter.println("inactive");
                    }
                    i3 = i2 + 1;
                }
                if (filterUid == -1) {
                    try {
                        pw.println();
                        printWriter.print("mReadyToRock=");
                        printWriter.println(this.mReadyToRock);
                        printWriter.print("mReportedActive=");
                        printWriter.println(this.mReportedActive);
                        printWriter.print("mMaxActiveJobs=");
                        printWriter.println(this.mMaxActiveJobs);
                    } catch (Throwable th3) {
                        th = th3;
                        throw th;
                    }
                }
                pw.println();
                printWriter.print("PersistStats: ");
                printWriter.println(this.mJobs.getPersistStats());
                pw.println();
            } catch (Throwable th4) {
                th = th4;
                i = filterUid;
                nowUptime2 = nowUptime;
                throw th;
            }
        }
    }

    static /* synthetic */ boolean lambda$dumpInternal$3(int filterUidFinal, JobStatus js) {
        return filterUidFinal == -1 || UserHandle.getAppId(js.getUid()) == filterUidFinal || UserHandle.getAppId(js.getSourceUid()) == filterUidFinal;
    }

    /* JADX WARNING: Missing block: B:88:?, code:
            r1.mJobPackageTracker.dump(r10, 1146756268040L, r15);
            r1.mJobPackageTracker.dumpHistory(r10, 1146756268039L, r15);
            r0 = r1.mPendingJobs.iterator();
     */
    /* JADX WARNING: Missing block: B:90:0x026c, code:
            if (r0.hasNext() == false) goto L_0x02c1;
     */
    /* JADX WARNING: Missing block: B:92:?, code:
            r2 = (com.android.server.job.controllers.JobStatus) r0.next();
            r8 = r10.start(2246267895817L);
            r2.writeToShortProto(r10, 1146756268033L);
     */
    /* JADX WARNING: Missing block: B:93:0x0286, code:
            r24 = r15;
            r14 = 1146756268033L;
            r14 = r8;
     */
    /* JADX WARNING: Missing block: B:95:?, code:
            r2.dump(r10, 1146756268034L, false, r12);
            r3 = evaluateJobPriorityLocked(r2);
     */
    /* JADX WARNING: Missing block: B:96:0x029c, code:
            if (r3 == 0) goto L_0x02a6;
     */
    /* JADX WARNING: Missing block: B:97:0x029e, code:
            r10.write(1172526071811L, r3);
     */
    /* JADX WARNING: Missing block: B:98:0x02a6, code:
            r10.write(1112396529668L, r21 - r2.madePending);
            r10.end(r14);
            r15 = r24;
     */
    /* JADX WARNING: Missing block: B:99:0x02ba, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:100:0x02bb, code:
            r24 = r15;
            r2 = r30;
     */
    /* JADX WARNING: Missing block: B:101:0x02c1, code:
            r24 = r15;
            r0 = r1.mActiveServices.iterator();
     */
    /* JADX WARNING: Missing block: B:103:0x02cd, code:
            if (r0.hasNext() == false) goto L_0x0395;
     */
    /* JADX WARNING: Missing block: B:104:0x02cf, code:
            r2 = (com.android.server.job.JobServiceContext) r0.next();
            r14 = r10.start(2246267895818L);
            r8 = r2.getRunningJobLocked();
     */
    /* JADX WARNING: Missing block: B:105:0x02e4, code:
            if (r8 != null) goto L_0x031a;
     */
    /* JADX WARNING: Missing block: B:106:0x02e6, code:
            r3 = r10.start(1146756268033L);
            r25 = r14;
            r10.write(1112396529665L, r12 - r2.mStoppedTime);
     */
    /* JADX WARNING: Missing block: B:107:0x0300, code:
            if (r2.mStoppedReason == null) goto L_0x030c;
     */
    /* JADX WARNING: Missing block: B:108:0x0302, code:
            r10.write(1138166333442L, r2.mStoppedReason);
     */
    /* JADX WARNING: Missing block: B:109:0x030c, code:
            r10.end(r3);
            r27 = r0;
            r0 = r8;
     */
    /* JADX WARNING: Missing block: B:110:0x031a, code:
            r25 = r14;
            r14 = r10.start(1146756268034L);
            r8.writeToShortProto(r10, 1146756268033L);
            r10.write(1112396529666L, r12 - r2.getExecutionStartTimeElapsed());
            r10.write(1112396529667L, r2.getTimeoutElapsed() - r12);
            r27 = r0;
            r0 = r8;
            r8.dump(r10, 1146756268036L, false, r12);
            r3 = evaluateJobPriorityLocked(r2.getRunningJobLocked());
     */
    /* JADX WARNING: Missing block: B:111:0x0365, code:
            if (r3 == 0) goto L_0x036f;
     */
    /* JADX WARNING: Missing block: B:112:0x0367, code:
            r10.write(1172526071813L, r3);
     */
    /* JADX WARNING: Missing block: B:113:0x036f, code:
            r10.write(1112396529670L, r21 - r0.madeActive);
            r10.write(1112396529671L, r0.madeActive - r0.madePending);
            r10.end(r14);
     */
    /* JADX WARNING: Missing block: B:114:0x038b, code:
            r10.end(r25);
     */
    /* JADX WARNING: Missing block: B:115:0x0390, code:
            r0 = r27;
     */
    /* JADX WARNING: Missing block: B:117:0x0398, code:
            if (r30 != -1) goto L_0x03b8;
     */
    /* JADX WARNING: Missing block: B:120:?, code:
            r10.write(1133871366155L, r1.mReadyToRock);
            r10.write(1133871366156L, r1.mReportedActive);
            r10.write(1120986464269L, r1.mMaxActiveJobs);
     */
    /* JADX WARNING: Missing block: B:121:0x03b8, code:
            monitor-exit(r17);
     */
    /* JADX WARNING: Missing block: B:122:0x03b9, code:
            r10.flush();
     */
    /* JADX WARNING: Missing block: B:123:0x03bc, code:
            return;
     */
    /* JADX WARNING: Missing block: B:124:0x03bd, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:125:0x03be, code:
            r2 = r30;
     */
    /* JADX WARNING: Missing block: B:135:0x03d9, code:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void dumpInternalProto(FileDescriptor fd, int filterUid) {
        Throwable th;
        int i;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        int filterUidFinal = UserHandle.getAppId(filterUid);
        long nowElapsed = sElapsedRealtimeClock.millis();
        long nowUptime = sUptimeMillisClock.millis();
        Predicate<JobStatus> predicate = new -$$Lambda$JobSchedulerService$rARZcsrvtM2sYbF4SrEE2BXDQ3U(filterUidFinal);
        Object obj = this.mLock;
        synchronized (obj) {
            int i2;
            Object obj2;
            long j;
            try {
                long rjToken;
                int filterUidFinal2;
                this.mConstants.dump(proto, 1146756268033L);
                proto.write(1120986464270L, this.mHeartbeat);
                int i3 = 0;
                proto.write(2220498092047L, this.mNextBucketHeartbeat[0]);
                proto.write(2220498092047L, this.mNextBucketHeartbeat[1]);
                proto.write(2220498092047L, this.mNextBucketHeartbeat[2]);
                proto.write(2220498092047L, this.mNextBucketHeartbeat[3]);
                proto.write(1112396529680L, this.mLastHeartbeatTime - nowUptime);
                proto.write(1112396529681L, (this.mLastHeartbeatTime + this.mConstants.STANDBY_HEARTBEAT_TIME) - nowUptime);
                proto.write(1133871366162L, this.mInParole);
                int[] iArr = this.mStartedUsers;
                int length = iArr.length;
                int i4 = 0;
                while (i4 < length) {
                    try {
                        proto.write(2220498092034L, iArr[i4]);
                        i4++;
                    } catch (Throwable th2) {
                        th = th2;
                        i2 = filterUid;
                        obj2 = obj;
                        i = filterUidFinal;
                        j = nowUptime;
                        filterUidFinal = predicate;
                    }
                }
                if (this.mJobs.size() > 0) {
                    try {
                        List<JobStatus> jobs = this.mJobs.mJobSet.getAllJobs();
                        sortJobs(jobs);
                        Iterator it = jobs.iterator();
                        while (it.hasNext()) {
                            JobStatus job = (JobStatus) it.next();
                            rjToken = proto.start(2246267895811L);
                            j = nowUptime;
                            Predicate predicate2;
                            try {
                                job.writeToShortProto(proto, 1146756268033L);
                                if (predicate2.test(job)) {
                                    nowUptime = rjToken;
                                    Iterator it2 = it;
                                    JobStatus job2 = job;
                                    List<JobStatus> jobs2 = jobs;
                                    obj2 = obj;
                                    filterUidFinal2 = filterUidFinal;
                                    Predicate<JobStatus> predicate3 = predicate2;
                                    try {
                                        job.dump(proto, 1146756268034L, true, nowElapsed);
                                        proto.write(1133871366147L, job2.isReady());
                                        proto.write(1133871366148L, ArrayUtils.contains(this.mStartedUsers, job2.getUserId()));
                                        proto.write(1133871366149L, this.mPendingJobs.contains(job2));
                                        proto.write(1133871366150L, isCurrentlyActiveLocked(job2));
                                        proto.write(1133871366151L, this.mBackingUpUids.indexOfKey(job2.getSourceUid()) >= 0);
                                        boolean componentPresent = false;
                                        try {
                                            componentPresent = AppGlobals.getPackageManager().getServiceInfo(job2.getServiceComponent(), 268435456, job2.getUserId()) != null;
                                        } catch (RemoteException e) {
                                        }
                                        proto.write(1133871366152L, componentPresent);
                                        proto.write(1112396529673L, heartbeatWhenJobsLastRun(job2));
                                        proto.end(nowUptime);
                                        predicate2 = predicate3;
                                        jobs = jobs2;
                                        obj = obj2;
                                        it = it2;
                                        nowUptime = j;
                                        filterUidFinal = filterUidFinal2;
                                        FileDescriptor fileDescriptor = fd;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        i2 = filterUid;
                                        i = filterUidFinal2;
                                    }
                                } else {
                                    nowUptime = j;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                obj2 = obj;
                                filterUidFinal2 = filterUidFinal;
                                filterUidFinal = predicate2;
                                i2 = filterUid;
                                i = filterUidFinal2;
                            }
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        obj2 = obj;
                        filterUidFinal2 = filterUidFinal;
                        j = nowUptime;
                        filterUidFinal = predicate;
                        i2 = filterUid;
                        i = filterUidFinal2;
                        throw th;
                    }
                }
                obj2 = obj;
                filterUidFinal2 = filterUidFinal;
                j = nowUptime;
                filterUidFinal = predicate;
                try {
                    int filterUidFinal3;
                    for (StateController controller : this.mControllers) {
                        controller.dumpControllerStateLocked(proto, 2246267895812L, filterUidFinal);
                    }
                    int i5 = 0;
                    while (i5 < this.mUidPriorityOverride.size()) {
                        try {
                            i2 = this.mUidPriorityOverride.keyAt(i5);
                            filterUidFinal3 = filterUidFinal2;
                            if (filterUidFinal3 != -1) {
                                try {
                                    if (filterUidFinal3 != UserHandle.getAppId(i2)) {
                                        i5++;
                                        filterUidFinal2 = filterUidFinal3;
                                    }
                                } catch (Throwable th6) {
                                    th = th6;
                                    i2 = filterUid;
                                    i = filterUidFinal3;
                                    throw th;
                                }
                            }
                            rjToken = proto.start(2246267895813L);
                            proto.write(1120986464257L, i2);
                            proto.write(1172526071810L, this.mUidPriorityOverride.valueAt(i5));
                            proto.end(rjToken);
                            i5++;
                            filterUidFinal2 = filterUidFinal3;
                        } catch (Throwable th7) {
                            th = th7;
                            i2 = filterUid;
                            i = filterUidFinal2;
                        }
                    }
                    filterUidFinal3 = filterUidFinal2;
                    while (true) {
                        i5 = i3;
                        try {
                            if (i5 >= this.mBackingUpUids.size()) {
                                break;
                            }
                            i2 = this.mBackingUpUids.keyAt(i5);
                            if (filterUidFinal3 == -1 || filterUidFinal3 == UserHandle.getAppId(i2)) {
                                proto.write(2220498092038L, i2);
                            }
                            i3 = i5 + 1;
                        } catch (Throwable th8) {
                            th = th8;
                            i2 = filterUid;
                            i = filterUidFinal3;
                        }
                    }
                } catch (Throwable th9) {
                    th = th9;
                    i2 = filterUid;
                    i = filterUidFinal2;
                }
            } catch (Throwable th10) {
                th = th10;
                i2 = filterUid;
                obj2 = obj;
                i = filterUidFinal;
                j = nowUptime;
                throw th;
            }
        }
    }

    static /* synthetic */ boolean lambda$dumpInternalProto$4(int filterUidFinal, JobStatus js) {
        return filterUidFinal == -1 || UserHandle.getAppId(js.getUid()) == filterUidFinal || UserHandle.getAppId(js.getSourceUid()) == filterUidFinal;
    }
}
