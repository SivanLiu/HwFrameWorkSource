package com.android.server.am;

import android.app.ActivityThread;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.Trace;
import android.provider.DeviceConfig;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.ServiceThread;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class AppCompactor {
    private static final String COMPACT_ACTION_ANON = "anon";
    private static final int COMPACT_ACTION_ANON_FLAG = 2;
    private static final String COMPACT_ACTION_FILE = "file";
    private static final int COMPACT_ACTION_FILE_FLAG = 1;
    private static final String COMPACT_ACTION_FULL = "all";
    private static final int COMPACT_ACTION_FULL_FLAG = 3;
    private static final String COMPACT_ACTION_NONE = "";
    private static final int COMPACT_ACTION_NONE_FLAG = 4;
    static final int COMPACT_PROCESS_BFGS = 4;
    static final int COMPACT_PROCESS_FULL = 2;
    static final int COMPACT_PROCESS_MSG = 1;
    static final int COMPACT_PROCESS_PERSISTENT = 3;
    static final int COMPACT_PROCESS_SOME = 1;
    static final int COMPACT_SYSTEM_MSG = 2;
    @VisibleForTesting
    static final int DEFAULT_COMPACT_ACTION_1 = 1;
    @VisibleForTesting
    static final int DEFAULT_COMPACT_ACTION_2 = 3;
    @VisibleForTesting
    static final long DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB = 8000;
    @VisibleForTesting
    static final long DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB = 12000;
    @VisibleForTesting
    static final String DEFAULT_COMPACT_PROC_STATE_THROTTLE = String.valueOf(12);
    @VisibleForTesting
    static final long DEFAULT_COMPACT_THROTTLE_1 = 5000;
    @VisibleForTesting
    static final long DEFAULT_COMPACT_THROTTLE_2 = 10000;
    @VisibleForTesting
    static final long DEFAULT_COMPACT_THROTTLE_3 = 500;
    @VisibleForTesting
    static final long DEFAULT_COMPACT_THROTTLE_4 = 10000;
    @VisibleForTesting
    static final long DEFAULT_COMPACT_THROTTLE_5 = 600000;
    @VisibleForTesting
    static final long DEFAULT_COMPACT_THROTTLE_6 = 600000;
    @VisibleForTesting
    static final float DEFAULT_STATSD_SAMPLE_RATE = 0.1f;
    @VisibleForTesting
    static final Boolean DEFAULT_USE_COMPACTION = false;
    @VisibleForTesting
    static final String KEY_COMPACT_ACTION_1 = "compact_action_1";
    @VisibleForTesting
    static final String KEY_COMPACT_ACTION_2 = "compact_action_2";
    @VisibleForTesting
    static final String KEY_COMPACT_FULL_DELTA_RSS_THROTTLE_KB = "compact_full_delta_rss_throttle_kb";
    @VisibleForTesting
    static final String KEY_COMPACT_FULL_RSS_THROTTLE_KB = "compact_full_rss_throttle_kb";
    @VisibleForTesting
    static final String KEY_COMPACT_PROC_STATE_THROTTLE = "compact_proc_state_throttle";
    @VisibleForTesting
    static final String KEY_COMPACT_STATSD_SAMPLE_RATE = "compact_statsd_sample_rate";
    @VisibleForTesting
    static final String KEY_COMPACT_THROTTLE_1 = "compact_throttle_1";
    @VisibleForTesting
    static final String KEY_COMPACT_THROTTLE_2 = "compact_throttle_2";
    @VisibleForTesting
    static final String KEY_COMPACT_THROTTLE_3 = "compact_throttle_3";
    @VisibleForTesting
    static final String KEY_COMPACT_THROTTLE_4 = "compact_throttle_4";
    @VisibleForTesting
    static final String KEY_COMPACT_THROTTLE_5 = "compact_throttle_5";
    @VisibleForTesting
    static final String KEY_COMPACT_THROTTLE_6 = "compact_throttle_6";
    @VisibleForTesting
    static final String KEY_USE_COMPACTION = "use_compaction";
    /* access modifiers changed from: private */
    public final ActivityManagerService mAm;
    private int mBfgsCompactionCount;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile String mCompactActionFull;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile String mCompactActionSome;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile long mCompactThrottleBFGS;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile long mCompactThrottleFullFull;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile long mCompactThrottleFullSome;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile long mCompactThrottlePersistent;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile long mCompactThrottleSomeFull;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile long mCompactThrottleSomeSome;
    private Handler mCompactionHandler;
    final ServiceThread mCompactionThread;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile long mFullAnonRssThrottleKb;
    private int mFullCompactionCount;
    @GuardedBy({"mPhenoypeFlagLock"})
    @VisibleForTesting
    volatile long mFullDeltaRssThrottleKb;
    /* access modifiers changed from: private */
    public Map<Integer, LastCompactionStats> mLastCompactionStats;
    private final DeviceConfig.OnPropertiesChangedListener mOnFlagsChangedListener;
    /* access modifiers changed from: private */
    public final ArrayList<ProcessRecord> mPendingCompactionProcesses;
    private int mPersistentCompactionCount;
    /* access modifiers changed from: private */
    public final Object mPhenotypeFlagLock;
    @GuardedBy({"mPhenoypeFlagLock"})
    @VisibleForTesting
    final Set<Integer> mProcStateThrottle;
    /* access modifiers changed from: private */
    public final Random mRandom;
    private int mSomeCompactionCount;
    @GuardedBy({"mPhenotypeFlagLock"})
    @VisibleForTesting
    volatile float mStatsdSampleRate;
    /* access modifiers changed from: private */
    public PropertyChangedCallbackForTest mTestCallback;
    @GuardedBy({"mPhenotypeFlagLock"})
    private volatile boolean mUseCompaction;

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public interface PropertyChangedCallbackForTest {
        void onPropertyChanged();
    }

    /* access modifiers changed from: private */
    public native void compactSystem();

    static /* synthetic */ int access$1308(AppCompactor x0) {
        int i = x0.mSomeCompactionCount;
        x0.mSomeCompactionCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1408(AppCompactor x0) {
        int i = x0.mFullCompactionCount;
        x0.mFullCompactionCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1508(AppCompactor x0) {
        int i = x0.mPersistentCompactionCount;
        x0.mPersistentCompactionCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$1608(AppCompactor x0) {
        int i = x0.mBfgsCompactionCount;
        x0.mBfgsCompactionCount = i + 1;
        return i;
    }

    public AppCompactor(ActivityManagerService am) {
        this.mPendingCompactionProcesses = new ArrayList<>();
        this.mOnFlagsChangedListener = new DeviceConfig.OnPropertiesChangedListener() {
            /* class com.android.server.am.AppCompactor.AnonymousClass1 */

            public void onPropertiesChanged(DeviceConfig.Properties properties) {
                synchronized (AppCompactor.this.mPhenotypeFlagLock) {
                    for (String name : properties.getKeyset()) {
                        if (AppCompactor.KEY_USE_COMPACTION.equals(name)) {
                            AppCompactor.this.updateUseCompaction();
                        } else {
                            if (!AppCompactor.KEY_COMPACT_ACTION_1.equals(name)) {
                                if (!AppCompactor.KEY_COMPACT_ACTION_2.equals(name)) {
                                    if (!AppCompactor.KEY_COMPACT_THROTTLE_1.equals(name) && !AppCompactor.KEY_COMPACT_THROTTLE_2.equals(name) && !AppCompactor.KEY_COMPACT_THROTTLE_3.equals(name)) {
                                        if (!AppCompactor.KEY_COMPACT_THROTTLE_4.equals(name)) {
                                            if (AppCompactor.KEY_COMPACT_STATSD_SAMPLE_RATE.equals(name)) {
                                                AppCompactor.this.updateStatsdSampleRate();
                                            } else if (AppCompactor.KEY_COMPACT_FULL_RSS_THROTTLE_KB.equals(name)) {
                                                AppCompactor.this.updateFullRssThrottle();
                                            } else if (AppCompactor.KEY_COMPACT_FULL_DELTA_RSS_THROTTLE_KB.equals(name)) {
                                                AppCompactor.this.updateFullDeltaRssThrottle();
                                            } else if (AppCompactor.KEY_COMPACT_PROC_STATE_THROTTLE.equals(name)) {
                                                AppCompactor.this.updateProcStateThrottle();
                                            }
                                        }
                                    }
                                    AppCompactor.this.updateCompactionThrottles();
                                }
                            }
                            AppCompactor.this.updateCompactionActions();
                        }
                    }
                }
                if (AppCompactor.this.mTestCallback != null) {
                    AppCompactor.this.mTestCallback.onPropertyChanged();
                }
            }
        };
        this.mPhenotypeFlagLock = new Object();
        this.mCompactActionSome = compactActionIntToString(1);
        this.mCompactActionFull = compactActionIntToString(3);
        this.mCompactThrottleSomeSome = DEFAULT_COMPACT_THROTTLE_1;
        this.mCompactThrottleSomeFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mCompactThrottleFullSome = 500;
        this.mCompactThrottleFullFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        this.mCompactThrottleBFGS = 600000;
        this.mCompactThrottlePersistent = 600000;
        this.mUseCompaction = DEFAULT_USE_COMPACTION.booleanValue();
        this.mRandom = new Random();
        this.mStatsdSampleRate = DEFAULT_STATSD_SAMPLE_RATE;
        this.mFullAnonRssThrottleKb = DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB;
        this.mFullDeltaRssThrottleKb = DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB;
        this.mLastCompactionStats = new LinkedHashMap<Integer, LastCompactionStats>() {
            /* class com.android.server.am.AppCompactor.AnonymousClass2 */

            /* JADX DEBUG: Method arguments types fixed to match base method, original types: [java.util.Map$Entry] */
            /* access modifiers changed from: protected */
            @Override // java.util.LinkedHashMap
            public boolean removeEldestEntry(Entry<Integer, LastCompactionStats> entry) {
                return size() > 100;
            }
        };
        this.mAm = am;
        this.mCompactionThread = new ServiceThread("CompactionThread", -2, true);
        this.mProcStateThrottle = new HashSet();
    }

    @VisibleForTesting
    AppCompactor(ActivityManagerService am, PropertyChangedCallbackForTest callback) {
        this(am);
        this.mTestCallback = callback;
    }

    public void init() {
        DeviceConfig.addOnPropertiesChangedListener("activity_manager", ActivityThread.currentApplication().getMainExecutor(), this.mOnFlagsChangedListener);
        synchronized (this.mPhenotypeFlagLock) {
            updateUseCompaction();
            updateCompactionActions();
            updateCompactionThrottles();
            updateStatsdSampleRate();
            updateFullRssThrottle();
            updateFullDeltaRssThrottle();
            updateProcStateThrottle();
        }
        Process.setThreadGroupAndCpuset(this.mCompactionThread.getThreadId(), 2);
    }

    public boolean useCompaction() {
        boolean z;
        synchronized (this.mPhenotypeFlagLock) {
            z = this.mUseCompaction;
        }
        return z;
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public void dump(PrintWriter pw) {
        pw.println("AppCompactor settings");
        synchronized (this.mPhenotypeFlagLock) {
            pw.println("  use_compaction=" + this.mUseCompaction);
            pw.println("  compact_action_1=" + this.mCompactActionSome);
            pw.println("  compact_action_2=" + this.mCompactActionFull);
            pw.println("  compact_throttle_1=" + this.mCompactThrottleSomeSome);
            pw.println("  compact_throttle_2=" + this.mCompactThrottleSomeFull);
            pw.println("  compact_throttle_3=" + this.mCompactThrottleFullSome);
            pw.println("  compact_throttle_4=" + this.mCompactThrottleFullFull);
            pw.println("  compact_throttle_5=" + this.mCompactThrottleBFGS);
            pw.println("  compact_throttle_6=" + this.mCompactThrottlePersistent);
            pw.println("  compact_statsd_sample_rate=" + this.mStatsdSampleRate);
            pw.println("  compact_full_rss_throttle_kb=" + this.mFullAnonRssThrottleKb);
            pw.println("  compact_full_delta_rss_throttle_kb=" + this.mFullDeltaRssThrottleKb);
            pw.println("  compact_proc_state_throttle=" + Arrays.toString(this.mProcStateThrottle.toArray(new Integer[0])));
            pw.println("  " + this.mSomeCompactionCount + " some, " + this.mFullCompactionCount + " full, " + this.mPersistentCompactionCount + " persistent, " + this.mBfgsCompactionCount + " BFGS compactions.");
            StringBuilder sb = new StringBuilder();
            sb.append("  Tracking last compaction stats for ");
            sb.append(this.mLastCompactionStats.size());
            sb.append(" processes.");
            pw.println(sb.toString());
            if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                for (Map.Entry<Integer, LastCompactionStats> entry : this.mLastCompactionStats.entrySet()) {
                    int pid = entry.getKey().intValue();
                    pw.println("    " + pid + ": " + Arrays.toString(entry.getValue().getRssAfterCompaction()));
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public void compactAppSome(ProcessRecord app) {
        app.reqCompactAction = 1;
        this.mPendingCompactionProcesses.add(app);
        Handler handler = this.mCompactionHandler;
        handler.sendMessage(handler.obtainMessage(1, app.setAdj, app.setProcState));
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public void compactAppFull(ProcessRecord app) {
        app.reqCompactAction = 2;
        this.mPendingCompactionProcesses.add(app);
        Handler handler = this.mCompactionHandler;
        handler.sendMessage(handler.obtainMessage(1, app.setAdj, app.setProcState));
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public void compactAppPersistent(ProcessRecord app) {
        app.reqCompactAction = 3;
        this.mPendingCompactionProcesses.add(app);
        Handler handler = this.mCompactionHandler;
        handler.sendMessage(handler.obtainMessage(1, app.curAdj, app.setProcState));
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public boolean shouldCompactPersistent(ProcessRecord app, long now) {
        return app.lastCompactTime == 0 || now - app.lastCompactTime > this.mCompactThrottlePersistent;
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public void compactAppBfgs(ProcessRecord app) {
        app.reqCompactAction = 4;
        this.mPendingCompactionProcesses.add(app);
        Handler handler = this.mCompactionHandler;
        handler.sendMessage(handler.obtainMessage(1, app.curAdj, app.setProcState));
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public boolean shouldCompactBFGS(ProcessRecord app, long now) {
        return app.lastCompactTime == 0 || now - app.lastCompactTime > this.mCompactThrottleBFGS;
    }

    /* access modifiers changed from: package-private */
    @GuardedBy({"mAm"})
    public void compactAllSystem() {
        if (this.mUseCompaction) {
            Handler handler = this.mCompactionHandler;
            handler.sendMessage(handler.obtainMessage(2));
        }
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mPhenotypeFlagLock"})
    public void updateUseCompaction() {
        this.mUseCompaction = DeviceConfig.getBoolean("activity_manager", KEY_USE_COMPACTION, DEFAULT_USE_COMPACTION.booleanValue());
        if (this.mUseCompaction && !this.mCompactionThread.isAlive()) {
            this.mCompactionThread.start();
            this.mCompactionHandler = new MemCompactionHandler();
        }
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mPhenotypeFlagLock"})
    public void updateCompactionActions() {
        int compactAction1 = DeviceConfig.getInt("activity_manager", KEY_COMPACT_ACTION_1, 1);
        int compactAction2 = DeviceConfig.getInt("activity_manager", KEY_COMPACT_ACTION_2, 3);
        this.mCompactActionSome = compactActionIntToString(compactAction1);
        this.mCompactActionFull = compactActionIntToString(compactAction2);
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mPhenotypeFlagLock"})
    public void updateCompactionThrottles() {
        boolean useThrottleDefaults = false;
        String throttleSomeSomeFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_1);
        String throttleSomeFullFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_2);
        String throttleFullSomeFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_3);
        String throttleFullFullFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_4);
        String throttleBFGSFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_5);
        String throttlePersistentFlag = DeviceConfig.getProperty("activity_manager", KEY_COMPACT_THROTTLE_6);
        if (TextUtils.isEmpty(throttleSomeSomeFlag) || TextUtils.isEmpty(throttleSomeFullFlag) || TextUtils.isEmpty(throttleFullSomeFlag) || TextUtils.isEmpty(throttleFullFullFlag) || TextUtils.isEmpty(throttleBFGSFlag) || TextUtils.isEmpty(throttlePersistentFlag)) {
            useThrottleDefaults = true;
        } else {
            try {
                this.mCompactThrottleSomeSome = (long) Integer.parseInt(throttleSomeSomeFlag);
                this.mCompactThrottleSomeFull = (long) Integer.parseInt(throttleSomeFullFlag);
                this.mCompactThrottleFullSome = (long) Integer.parseInt(throttleFullSomeFlag);
                this.mCompactThrottleFullFull = (long) Integer.parseInt(throttleFullFullFlag);
                this.mCompactThrottleBFGS = (long) Integer.parseInt(throttleBFGSFlag);
                this.mCompactThrottlePersistent = (long) Integer.parseInt(throttlePersistentFlag);
            } catch (NumberFormatException e) {
                useThrottleDefaults = true;
            }
        }
        if (useThrottleDefaults) {
            this.mCompactThrottleSomeSome = DEFAULT_COMPACT_THROTTLE_1;
            this.mCompactThrottleSomeFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
            this.mCompactThrottleFullSome = 500;
            this.mCompactThrottleFullFull = JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
            this.mCompactThrottleBFGS = 600000;
            this.mCompactThrottlePersistent = 600000;
        }
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mPhenotypeFlagLock"})
    public void updateStatsdSampleRate() {
        this.mStatsdSampleRate = DeviceConfig.getFloat("activity_manager", KEY_COMPACT_STATSD_SAMPLE_RATE, (float) DEFAULT_STATSD_SAMPLE_RATE);
        this.mStatsdSampleRate = Math.min(1.0f, Math.max(0.0f, this.mStatsdSampleRate));
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mPhenotypeFlagLock"})
    public void updateFullRssThrottle() {
        this.mFullAnonRssThrottleKb = DeviceConfig.getLong("activity_manager", KEY_COMPACT_FULL_RSS_THROTTLE_KB, (long) DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB);
        if (this.mFullAnonRssThrottleKb < 0) {
            this.mFullAnonRssThrottleKb = DEFAULT_COMPACT_FULL_RSS_THROTTLE_KB;
        }
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mPhenotypeFlagLock"})
    public void updateFullDeltaRssThrottle() {
        this.mFullDeltaRssThrottleKb = DeviceConfig.getLong("activity_manager", KEY_COMPACT_FULL_DELTA_RSS_THROTTLE_KB, (long) DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB);
        if (this.mFullDeltaRssThrottleKb < 0) {
            this.mFullDeltaRssThrottleKb = DEFAULT_COMPACT_FULL_DELTA_RSS_THROTTLE_KB;
        }
    }

    /* access modifiers changed from: private */
    @GuardedBy({"mPhenotypeFlagLock"})
    public void updateProcStateThrottle() {
        String procStateThrottleString = DeviceConfig.getString("activity_manager", KEY_COMPACT_PROC_STATE_THROTTLE, DEFAULT_COMPACT_PROC_STATE_THROTTLE);
        if (!parseProcStateThrottle(procStateThrottleString)) {
            Slog.w(ActivityManagerService.TAG, "Unable to parse app compact proc state throttle \"" + procStateThrottleString + "\" falling back to default.");
            if (!parseProcStateThrottle(DEFAULT_COMPACT_PROC_STATE_THROTTLE)) {
                Slog.wtf(ActivityManagerService.TAG, "Unable to parse default app compact proc state throttle " + DEFAULT_COMPACT_PROC_STATE_THROTTLE);
            }
        }
    }

    private boolean parseProcStateThrottle(String procStateThrottleString) {
        String[] procStates = TextUtils.split(procStateThrottleString, ",");
        this.mProcStateThrottle.clear();
        int length = procStates.length;
        int i = 0;
        while (i < length) {
            String procState = procStates[i];
            try {
                this.mProcStateThrottle.add(Integer.valueOf(Integer.parseInt(procState)));
                i++;
            } catch (NumberFormatException e) {
                Slog.e(ActivityManagerService.TAG, "Failed to parse default app compaction proc state: " + procState);
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    static String compactActionIntToString(int action) {
        if (action == 1) {
            return COMPACT_ACTION_FILE;
        }
        if (action == 2) {
            return COMPACT_ACTION_ANON;
        }
        if (action != 3) {
            return action != 4 ? "" : "";
        }
        return COMPACT_ACTION_FULL;
    }

    /* access modifiers changed from: private */
    public static final class LastCompactionStats {
        private final long[] mRssAfterCompaction;

        LastCompactionStats(long[] rss) {
            this.mRssAfterCompaction = rss;
        }

        /* access modifiers changed from: package-private */
        public long[] getRssAfterCompaction() {
            return this.mRssAfterCompaction;
        }
    }

    private final class MemCompactionHandler extends Handler {
        private MemCompactionHandler() {
            super(AppCompactor.this.mCompactionThread.getLooper());
        }

        /* JADX INFO: Multiple debug info for r9v23 long: [D('anonRssBefore' long), D('absDelta' long)] */
        /* JADX WARNING: Code restructure failed: missing block: B:101:0x0285, code lost:
            if (r41[3] != 0) goto L_0x02a7;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:103:0x0289, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:104:0x028b, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping compaction forprocess " + r0 + " with no memory usage. Dead?");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:106:0x02ad, code lost:
            if (r15.equals(com.android.server.am.AppCompactor.COMPACT_ACTION_FULL) != false) goto L_0x02be;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:108:0x02b5, code lost:
            if (r15.equals(com.android.server.am.AppCompactor.COMPACT_ACTION_ANON) == false) goto L_0x02b8;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:109:0x02b8, code lost:
            r28 = r9;
            r25 = r13;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:111:0x02c6, code lost:
            if (r61.this$0.mFullAnonRssThrottleKb <= 0) goto L_0x02fc;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:112:0x02c8, code lost:
            r1 = r61;
            r25 = r13;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:113:0x02d2, code lost:
            if (r9 >= r1.this$0.mFullAnonRssThrottleKb) goto L_0x0300;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:115:0x02d6, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:116:0x02d8, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping full compaction for process " + r4 + "; anon RSS is too small: " + r9 + "KB.");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:117:0x02fc, code lost:
            r1 = r61;
            r25 = r13;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:118:0x0300, code lost:
            if (r0 == null) goto L_0x0367;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:120:0x030a, code lost:
            if (r1.this$0.mFullDeltaRssThrottleKb <= 0) goto L_0x0367;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:121:0x030c, code lost:
            r13 = r0.getRssAfterCompaction();
            r28 = r9;
            r9 = (java.lang.Math.abs(r41[1] - r13[1]) + java.lang.Math.abs(r41[2] - r13[2])) + java.lang.Math.abs(r41[3] - r13[3]);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:122:0x033d, code lost:
            if (r9 > r1.this$0.mFullDeltaRssThrottleKb) goto L_0x0369;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:124:0x0341, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:125:0x0343, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping full compaction for process " + r4 + "; abs delta is too small: " + r9 + "KB.");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:126:0x0367, code lost:
            r28 = r9;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:128:0x036a, code lost:
            if (r0 == 1) goto L_0x0390;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:130:0x036d, code lost:
            if (r0 == 2) goto L_0x0388;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:132:0x0370, code lost:
            if (r0 == 3) goto L_0x0380;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:134:0x0373, code lost:
            if (r0 == 4) goto L_0x0378;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:135:0x0375, code lost:
            r1 = r61;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:136:0x0378, code lost:
            r1 = r61;
            com.android.server.am.AppCompactor.access$1608(r1.this$0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:137:0x0380, code lost:
            r1 = r61;
            com.android.server.am.AppCompactor.access$1508(r1.this$0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:138:0x0388, code lost:
            r1 = r61;
            com.android.server.am.AppCompactor.access$1408(r1.this$0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:139:0x0390, code lost:
            r1 = r61;
            com.android.server.am.AppCompactor.access$1308(r1.this$0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:141:?, code lost:
            r9 = new java.lang.StringBuilder();
            r9.append("Compact ");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:143:0x03a3, code lost:
            if (r0 != 1) goto L_0x03cb;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:144:0x03a5, code lost:
            r10 = "some";
         */
        /* JADX WARNING: Code restructure failed: missing block: B:145:0x03cb, code lost:
            r10 = "full";
         */
        /* JADX WARNING: Code restructure failed: missing block: B:146:0x03cd, code lost:
            r9.append(r10);
            r9.append(": ");
            r9.append(r4);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:148:?, code lost:
            android.os.Trace.traceBegin(64, r9.toString());
         */
        /* JADX WARNING: Code restructure failed: missing block: B:149:0x03e1, code lost:
            r9 = android.os.Debug.getZramFreeKb();
            r9 = new java.io.FileOutputStream("/proc/" + r0 + "/reclaim");
            r9.write(r15.getBytes());
            r9.close();
            r9 = android.os.Process.getRss(r0);
            r9 = android.os.SystemClock.uptimeMillis();
            r44 = r9 - r7;
            r19 = android.os.Debug.getZramFreeKb();
            android.util.EventLog.writeEvent((int) com.android.server.am.EventLogTags.AM_COMPACT, java.lang.Integer.valueOf(r0), r4, r15, java.lang.Long.valueOf(r41[0]), java.lang.Long.valueOf(r41[1]), java.lang.Long.valueOf(r41[2]), java.lang.Long.valueOf(r41[3]), java.lang.Long.valueOf(r9[0] - r41[0]), java.lang.Long.valueOf(r9[1] - r41[1]), java.lang.Long.valueOf(r9[2] - r41[2]), java.lang.Long.valueOf(r9[3] - r41[3]), java.lang.Long.valueOf(r44), java.lang.Integer.valueOf(r0), java.lang.Long.valueOf(r5), java.lang.Integer.valueOf(r21), java.lang.Integer.valueOf(r25), java.lang.Long.valueOf(r9), java.lang.Long.valueOf(r19 - r9));
         */
        /* JADX WARNING: Code restructure failed: missing block: B:150:0x04ef, code lost:
            if (com.android.server.am.AppCompactor.access$1700(r1.this$0).nextFloat() >= r1.this$0.mStatsdSampleRate) goto L_0x0581;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:154:0x0512, code lost:
            r7 = r9;
            r2 = r0;
            r54 = r0;
            r58 = r9;
            r56 = r22;
            r60 = r15;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:156:?, code lost:
            android.util.StatsLog.write(com.android.server.hdmi.HdmiCecKeycode.CEC_KEYCODE_F3_GREEN, r0, r4, r0, r41[0], r41[1], r41[2], r41[3], r9[0], r9[1], r9[2], r9[3], r44, r0, r5, r21, android.app.ActivityManager.processStateAmToProto(r25), r9, r19);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:157:0x054b, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:160:0x0555, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:161:0x055f, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:164:0x0570, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:165:0x0581, code lost:
            r7 = r9;
            r2 = r0;
            r54 = r0;
            r58 = r9;
            r60 = r15;
            r56 = r22;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:167:?, code lost:
            r9 = com.android.server.am.AppCompactor.access$1000(r1.this$0);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:168:0x059b, code lost:
            monitor-enter(r9);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:170:?, code lost:
            com.android.server.am.ActivityManagerService.boostPriorityForLockedSection();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:173:?, code lost:
            r56.lastCompactTime = r7;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:176:?, code lost:
            r56.lastCompactAction = r54;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:177:0x05a7, code lost:
            monitor-exit(r9);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:179:?, code lost:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:183:0x05b3, code lost:
            if (r60.equals(com.android.server.am.AppCompactor.COMPACT_ACTION_FULL) != false) goto L_0x05bd;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:185:0x05bb, code lost:
            if (r60.equals(com.android.server.am.AppCompactor.COMPACT_ACTION_ANON) == false) goto L_0x05d1;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:186:0x05bd, code lost:
            com.android.server.am.AppCompactor.access$1200(r1.this$0).put(java.lang.Integer.valueOf(r2), new com.android.server.am.AppCompactor.LastCompactionStats(r58));
         */
        /* JADX WARNING: Code restructure failed: missing block: B:187:0x05d1, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:188:0x05d5, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:191:0x05db, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:192:0x05e1, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:194:0x05e7, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:196:0x05ef, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:199:?, code lost:
            monitor-exit(r9);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:200:0x05f9, code lost:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:201:0x05fc, code lost:
            throw r0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:202:0x05fd, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:204:0x0600, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:205:0x0603, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:206:0x0605, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:209:0x060e, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:211:0x0618, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:212:0x0627, code lost:
            r0 = th;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:214:0x0635, code lost:
            android.os.Trace.traceEnd(64);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:215:0x063a, code lost:
            throw r0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:217:0x063c, code lost:
            r7 = 64;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:232:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:233:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:234:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:235:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:236:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:237:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:238:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:239:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:240:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:241:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:242:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:243:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:244:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:245:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:246:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:247:?, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:28:0x00a3, code lost:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
         */
        /* JADX WARNING: Code restructure failed: missing block: B:29:0x00a6, code lost:
            if (r0 != 0) goto L_0x00a9;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:30:0x00a8, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x00ad, code lost:
            if (r5 == 0) goto L_0x020a;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:34:0x00b0, code lost:
            if (r0 != 1) goto L_0x011e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:35:0x00b2, code lost:
            if (r0 != 1) goto L_0x00c4;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:37:0x00bc, code lost:
            if ((r7 - r5) < r61.this$0.mCompactThrottleSomeSome) goto L_0x00bf;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:40:0x00c5, code lost:
            if (r0 != 2) goto L_0x0118;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:41:0x00c7, code lost:
            r22 = r0;
            r21 = r15;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:42:0x00d3, code lost:
            if ((r7 - r5) >= r61.this$0.mCompactThrottleSomeFull) goto L_0x020e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:44:0x00d7, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:45:0x00d9, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping some compaction for " + r4 + ": too soon. throttle=" + r61.this$0.mCompactThrottleSomeSome + com.android.server.slice.SliceClientPermissions.SliceAuthority.DELIMITER + r61.this$0.mCompactThrottleSomeFull + " last=" + (r7 - r5) + "ms ago");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:46:0x0118, code lost:
            r22 = r0;
            r21 = r15;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:47:0x011e, code lost:
            r22 = r0;
            r21 = r15;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:48:0x0123, code lost:
            if (r0 != 2) goto L_0x0182;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:50:0x0126, code lost:
            if (r0 != 1) goto L_0x0132;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:52:0x0130, code lost:
            if ((r7 - r5) < r61.this$0.mCompactThrottleFullSome) goto L_0x013f;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:54:0x0133, code lost:
            if (r0 != 2) goto L_0x020e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:56:0x013d, code lost:
            if ((r7 - r5) >= r61.this$0.mCompactThrottleFullFull) goto L_0x020e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:58:0x0141, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:59:0x0143, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping full compaction for " + r4 + ": too soon. throttle=" + r61.this$0.mCompactThrottleFullSome + com.android.server.slice.SliceClientPermissions.SliceAuthority.DELIMITER + r61.this$0.mCompactThrottleFullFull + " last=" + (r7 - r5) + "ms ago");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:61:0x0183, code lost:
            if (r0 != 3) goto L_0x01c6;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:63:0x018d, code lost:
            if ((r7 - r5) >= r61.this$0.mCompactThrottlePersistent) goto L_0x020e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:65:0x0191, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:66:0x0193, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping persistent compaction for " + r4 + ": too soon. throttle=" + r61.this$0.mCompactThrottlePersistent + " last=" + (r7 - r5) + "ms ago");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:68:0x01c7, code lost:
            if (r0 != 4) goto L_0x020e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:70:0x01d1, code lost:
            if ((r7 - r5) >= r61.this$0.mCompactThrottleBFGS) goto L_0x020e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:72:0x01d5, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:73:0x01d7, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping bfgs compaction for " + r4 + ": too soon. throttle=" + r61.this$0.mCompactThrottleBFGS + " last=" + (r7 - r5) + "ms ago");
         */
        /* JADX WARNING: Code restructure failed: missing block: B:74:0x020a, code lost:
            r22 = r0;
            r21 = r15;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:76:0x020f, code lost:
            if (r0 == 1) goto L_0x0224;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:78:0x0212, code lost:
            if (r0 == 2) goto L_0x021e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:80:0x0215, code lost:
            if (r0 == 3) goto L_0x021e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:82:0x0218, code lost:
            if (r0 == 4) goto L_0x021e;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:83:0x021a, code lost:
            r15 = "";
         */
        /* JADX WARNING: Code restructure failed: missing block: B:84:0x021e, code lost:
            r15 = r61.this$0.mCompactActionFull;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:85:0x0224, code lost:
            r15 = r61.this$0.mCompactActionSome;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:87:0x022f, code lost:
            if ("".equals(r15) == false) goto L_0x0232;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:88:0x0231, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:90:0x023e, code lost:
            if (r61.this$0.mProcStateThrottle.contains(java.lang.Integer.valueOf(r13)) == false) goto L_0x0263;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:92:0x0242, code lost:
            if (com.android.server.am.ActivityManagerDebugConfig.DEBUG_COMPACTION == false) goto L_?;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:93:0x0244, code lost:
            android.util.Slog.d(com.android.server.am.ActivityManagerService.TAG, "Skipping full compaction for process " + r4 + "; proc state is " + r13);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:94:0x0263, code lost:
            r41 = android.os.Process.getRss(r0);
            r9 = r41[2];
         */
        /* JADX WARNING: Code restructure failed: missing block: B:95:0x0271, code lost:
            if (r41[0] != 0) goto L_0x02a7;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:97:0x0278, code lost:
            if (r41[1] != 0) goto L_0x02a7;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:99:0x027e, code lost:
            if (r41[2] != 0) goto L_0x02a7;
         */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Removed duplicated region for block: B:212:0x0627 A[ExcHandler: all (th java.lang.Throwable), Splitter:B:140:0x0398] */
        public void handleMessage(Message msg) {
            String name;
            long start;
            int i = msg.what;
            if (i == 1) {
                long start2 = SystemClock.uptimeMillis();
                int lastOomAdj = msg.arg1;
                int procState = msg.arg2;
                synchronized (AppCompactor.this.mAm) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        ProcessRecord proc = (ProcessRecord) AppCompactor.this.mPendingCompactionProcesses.remove(0);
                        int pendingAction = proc.reqCompactAction;
                        int pid = proc.pid;
                        String name2 = proc.processName;
                        if (pendingAction != 1) {
                            if (pendingAction != 2) {
                                name = name2;
                                int lastCompactAction = proc.lastCompactAction;
                                long lastCompactTime = proc.lastCompactTime;
                                LastCompactionStats lastCompactionStats = (LastCompactionStats) AppCompactor.this.mLastCompactionStats.remove(Integer.valueOf(pid));
                            }
                        }
                        if (proc.setAdj <= 200) {
                            try {
                                if (ActivityManagerDebugConfig.DEBUG_COMPACTION) {
                                    Slog.d(ActivityManagerService.TAG, "Skipping compaction as process " + name2 + " is now perceptible.");
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return;
                            } catch (Throwable th) {
                                th = th;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        } else {
                            name = name2;
                            int lastCompactAction2 = proc.lastCompactAction;
                            long lastCompactTime2 = proc.lastCompactTime;
                            LastCompactionStats lastCompactionStats2 = (LastCompactionStats) AppCompactor.this.mLastCompactionStats.remove(Integer.valueOf(pid));
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        while (true) {
                            break;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
            } else if (i == 2) {
                Trace.traceBegin(64, "compactSystem");
                AppCompactor.this.compactSystem();
                Trace.traceEnd(64);
                return;
            } else {
                return;
            }
            Trace.traceEnd(start);
        }
    }
}
