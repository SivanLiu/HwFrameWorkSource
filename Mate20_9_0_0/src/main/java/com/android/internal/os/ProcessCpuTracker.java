package com.android.internal.os;

import android.os.FileUtils;
import android.os.Process;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.util.Slog;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.os.BatteryStatsImpl.Uid.Proc;
import com.android.internal.util.FastPrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import libcore.io.IoUtils;

public class ProcessCpuTracker {
    private static final boolean DEBUG = false;
    private static final int[] LOAD_AVERAGE_FORMAT = new int[]{16416, 16416, 16416};
    private static final int[] PROCESS_FULL_STATS_FORMAT = new int[]{32, 4640, 32, 32, 32, 32, 32, 32, 32, 8224, 32, 8224, 32, 8224, 8224, 32, 32, 32, 32, 32, 32, 32, 8224};
    static final int PROCESS_FULL_STAT_MAJOR_FAULTS = 2;
    static final int PROCESS_FULL_STAT_MINOR_FAULTS = 1;
    static final int PROCESS_FULL_STAT_STIME = 4;
    static final int PROCESS_FULL_STAT_UTIME = 3;
    static final int PROCESS_FULL_STAT_VSIZE = 5;
    private static final int[] PROCESS_STATS_FORMAT = new int[]{32, MetricsEvent.DIALOG_WIFI_SKIP, 32, 32, 32, 32, 32, 32, 32, 8224, 32, 8224, 32, 8224, 8224};
    static final int PROCESS_STAT_MAJOR_FAULTS = 1;
    static final int PROCESS_STAT_MINOR_FAULTS = 0;
    static final int PROCESS_STAT_STIME = 3;
    static final int PROCESS_STAT_UTIME = 2;
    private static final int[] SYSTEM_CPU_FORMAT = new int[]{MetricsEvent.OVERVIEW_SELECT_TIMEOUT, 8224, 8224, 8224, 8224, 8224, 8224, 8224};
    private static final String TAG = "ProcessCpuTracker";
    private static final boolean localLOGV = false;
    private static final Comparator<Stats> sLoadComparator = new Comparator<Stats>() {
        public final int compare(Stats sta, Stats stb) {
            int ta = sta.rel_utime + sta.rel_stime;
            int tb = stb.rel_utime + stb.rel_stime;
            int i = 1;
            if (ta != tb) {
                if (ta > tb) {
                    i = -1;
                }
                return i;
            } else if (sta.added != stb.added) {
                if (sta.added) {
                    i = -1;
                }
                return i;
            } else if (sta.removed == stb.removed) {
                return 0;
            } else {
                if (sta.added) {
                    i = -1;
                }
                return i;
            }
        }
    };
    private long mBaseIdleTime;
    private long mBaseIoWaitTime;
    private long mBaseIrqTime;
    private long mBaseSoftIrqTime;
    private long mBaseSystemTime;
    private long mBaseUserTime;
    private byte[] mBuffer = new byte[4096];
    private int[] mCurPids;
    private int[] mCurThreadPids;
    private long mCurrentSampleRealTime;
    private long mCurrentSampleTime;
    private long mCurrentSampleWallTime;
    private boolean mFirst = true;
    private final boolean mIncludeThreads;
    private final long mJiffyMillis;
    private long mLastSampleRealTime;
    private long mLastSampleTime;
    private long mLastSampleWallTime;
    private float mLoad1 = 0.0f;
    private float mLoad15 = 0.0f;
    private float mLoad5 = 0.0f;
    private final float[] mLoadAverageData = new float[3];
    private final ArrayList<Stats> mProcStats = new ArrayList();
    private final long[] mProcessFullStatsData = new long[6];
    private final String[] mProcessFullStatsStringData = new String[6];
    private final long[] mProcessStatsData = new long[4];
    private int mRelIdleTime;
    private int mRelIoWaitTime;
    private int mRelIrqTime;
    private int mRelSoftIrqTime;
    private boolean mRelStatsAreGood;
    private int mRelSystemTime;
    private int mRelUserTime;
    private final long[] mSinglePidStatsData = new long[4];
    private final long[] mSystemCpuData = new long[7];
    private final ArrayList<Stats> mWorkingProcs = new ArrayList();
    private boolean mWorkingProcsSorted;

    public interface FilterStats {
        boolean needed(Stats stats);
    }

    public static class Stats {
        public boolean active;
        public boolean added;
        public String baseName;
        public long base_majfaults;
        public long base_minfaults;
        public long base_stime;
        public long base_uptime;
        public long base_utime;
        public Proc batteryStats;
        final String cmdlineFile;
        public boolean interesting;
        public String name;
        public int nameWidth;
        public final int pid;
        public int rel_majfaults;
        public int rel_minfaults;
        public int rel_stime;
        public long rel_uptime;
        public int rel_utime;
        public boolean removed;
        final String statFile;
        final ArrayList<Stats> threadStats;
        final String threadsDir;
        public final int uid;
        public long vsize;
        public boolean working;
        final ArrayList<Stats> workingThreads;

        Stats(int _pid, int parentPid, boolean includeThreads) {
            this.pid = _pid;
            if (parentPid < 0) {
                File procDir = new File("/proc", Integer.toString(this.pid));
                this.statFile = new File(procDir, "stat").toString();
                this.cmdlineFile = new File(procDir, "cmdline").toString();
                this.threadsDir = new File(procDir, "task").toString();
                if (includeThreads) {
                    this.threadStats = new ArrayList();
                    this.workingThreads = new ArrayList();
                } else {
                    this.threadStats = null;
                    this.workingThreads = null;
                }
            } else {
                this.statFile = new File(new File(new File(new File("/proc", Integer.toString(parentPid)), "task"), Integer.toString(this.pid)), "stat").toString();
                this.cmdlineFile = null;
                this.threadsDir = null;
                this.threadStats = null;
                this.workingThreads = null;
            }
            this.uid = FileUtils.getUid(this.statFile.toString());
        }
    }

    public ProcessCpuTracker(boolean includeThreads) {
        this.mIncludeThreads = includeThreads;
        this.mJiffyMillis = 1000 / Os.sysconf(OsConstants._SC_CLK_TCK);
    }

    public void onLoadChanged(float load1, float load5, float load15) {
    }

    public int onMeasureProcessName(String name) {
        return 0;
    }

    public void init() {
        this.mFirst = true;
        update();
    }

    public void update() {
        long nowWallTime;
        long nowRealtime;
        long nowUptime;
        long nowUptime2 = SystemClock.uptimeMillis();
        long nowRealtime2 = SystemClock.elapsedRealtime();
        long nowWallTime2 = System.currentTimeMillis();
        long[] sysCpu = this.mSystemCpuData;
        int i = null;
        boolean z = true;
        if (Process.readProcFile("/proc/stat", SYSTEM_CPU_FORMAT, null, sysCpu, null)) {
            long usertime = (sysCpu[0] + sysCpu[1]) * this.mJiffyMillis;
            long systemtime = sysCpu[2] * this.mJiffyMillis;
            nowWallTime = nowWallTime2;
            long idletime = sysCpu[3] * this.mJiffyMillis;
            nowRealtime = nowRealtime2;
            nowWallTime2 = sysCpu[4] * this.mJiffyMillis;
            nowUptime = nowUptime2;
            nowRealtime2 = sysCpu[5] * this.mJiffyMillis;
            nowUptime2 = sysCpu[6] * this.mJiffyMillis;
            this.mRelUserTime = (int) (usertime - this.mBaseUserTime);
            this.mRelSystemTime = (int) (systemtime - this.mBaseSystemTime);
            this.mRelIoWaitTime = (int) (nowWallTime2 - this.mBaseIoWaitTime);
            this.mRelIrqTime = (int) (nowRealtime2 - this.mBaseIrqTime);
            this.mRelSoftIrqTime = (int) (nowUptime2 - this.mBaseSoftIrqTime);
            this.mRelIdleTime = (int) (idletime - this.mBaseIdleTime);
            z = true;
            this.mRelStatsAreGood = true;
            this.mBaseUserTime = usertime;
            this.mBaseSystemTime = systemtime;
            this.mBaseIoWaitTime = nowWallTime2;
            this.mBaseIrqTime = nowRealtime2;
            this.mBaseSoftIrqTime = nowUptime2;
            this.mBaseIdleTime = idletime;
        } else {
            nowUptime = nowUptime2;
            nowRealtime = nowRealtime2;
            nowWallTime = nowWallTime2;
            long[] jArr = sysCpu;
        }
        this.mLastSampleTime = this.mCurrentSampleTime;
        this.mCurrentSampleTime = nowUptime;
        this.mLastSampleRealTime = this.mCurrentSampleRealTime;
        this.mCurrentSampleRealTime = nowRealtime;
        this.mLastSampleWallTime = this.mCurrentSampleWallTime;
        this.mCurrentSampleWallTime = nowWallTime;
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            i = 0;
            boolean z2 = z;
            this.mCurPids = collectStats("/proc", -1, this.mFirst, this.mCurPids, this.mProcStats);
            float[] loadAverages = this.mLoadAverageData;
            if (Process.readProcFile("/proc/loadavg", LOAD_AVERAGE_FORMAT, null, null, loadAverages)) {
                float load1 = loadAverages[i];
                float load5 = loadAverages[z2];
                float load15 = loadAverages[2];
                if (!(load1 == this.mLoad1 && load5 == this.mLoad5 && load15 == this.mLoad15)) {
                    this.mLoad1 = load1;
                    this.mLoad5 = load5;
                    this.mLoad15 = load15;
                    onLoadChanged(load1, load5, load15);
                }
            }
            this.mWorkingProcsSorted = i;
            this.mFirst = i;
        } finally {
            StrictMode.setThreadPolicy(savedPolicy);
        }
    }

    private int[] collectStats(String statsFile, int parentPid, boolean first, int[] curPids, ArrayList<Stats> allProcs) {
        int[] pids;
        boolean pids2;
        int i = parentPid;
        ArrayList arrayList = allProcs;
        int[] pids3 = Process.getPids(statsFile, curPids);
        boolean z = false;
        int NP = pids3 == null ? 0 : pids3.length;
        int curStatsIndex = 0;
        int NS = allProcs.size();
        int i2 = 0;
        while (true) {
            int i3 = i2;
            int i4;
            int i5;
            int i6;
            if (i3 >= NP) {
                i4 = i;
                pids = pids3;
                i5 = NP;
                i6 = NS;
                pids2 = true;
                break;
            }
            int pid = pids3[i3];
            if (pid < 0) {
                NP = pid;
                i4 = i;
                pids = pids3;
                pids2 = true;
                break;
            }
            int i7;
            String str;
            int[] iArr;
            Stats st = curStatsIndex < NS ? (Stats) arrayList.get(curStatsIndex) : null;
            int pid2;
            if (st == null || st.pid != pid) {
                pid2 = pid;
                pids = pids3;
                i5 = NP;
                i6 = NS;
                i7 = i3;
                Stats st2 = st;
                if (st2 != null) {
                    i = pid2;
                    if (st2.pid > i) {
                        arrayList = allProcs;
                    } else {
                        st2.rel_utime = 0;
                        st2.rel_stime = 0;
                        st2.rel_minfaults = 0;
                        st2.rel_majfaults = 0;
                        st2.removed = true;
                        st2.working = true;
                        ArrayList<Stats> arrayList2 = allProcs;
                        arrayList2.remove(curStatsIndex);
                        NS = i6 - 1;
                        i7--;
                    }
                } else {
                    i = pid2;
                    arrayList = allProcs;
                }
                i4 = parentPid;
                Stats st3 = new Stats(i, i4, this.mIncludeThreads);
                arrayList.add(curStatsIndex, st3);
                int curStatsIndex2 = curStatsIndex + 1;
                NS = i6 + 1;
                String[] procStatsString = this.mProcessFullStatsStringData;
                long[] procStats = this.mProcessFullStatsData;
                st3.base_uptime = SystemClock.uptimeMillis();
                String path = st3.statFile.toString();
                if (Process.readProcFile(path, PROCESS_FULL_STATS_FORMAT, procStatsString, procStats, null)) {
                    st3.vsize = procStats[5];
                    st3.interesting = true;
                    st3.baseName = procStatsString[0];
                    st3.base_minfaults = procStats[1];
                    st3.base_majfaults = procStats[2];
                    st3.base_utime = procStats[3] * this.mJiffyMillis;
                    st3.base_stime = procStats[4] * this.mJiffyMillis;
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Skipping unknown process pid ");
                    stringBuilder.append(i);
                    Slog.w(str2, stringBuilder.toString());
                    st3.baseName = "<unknown>";
                    st3.base_stime = 0;
                    st3.base_utime = 0;
                    st3.base_majfaults = 0;
                    st3.base_minfaults = 0;
                }
                if (i4 < 0) {
                    getName(st3, st3.cmdlineFile);
                    if (st3.threadStats != null) {
                        this.mCurThreadPids = collectStats(st3.threadsDir, i, true, this.mCurThreadPids, st3.threadStats);
                    }
                } else {
                    if (st3.interesting) {
                        st3.name = st3.baseName;
                        st3.nameWidth = onMeasureProcessName(st3.name);
                    }
                }
                st3.rel_utime = 0;
                st3.rel_stime = 0;
                st3.rel_minfaults = 0;
                st3.rel_majfaults = 0;
                st3.added = true;
                if (!first && st3.interesting) {
                    st3.working = true;
                }
                curStatsIndex = curStatsIndex2;
                i2 = i7 + 1;
                i = i4;
                NP = i5;
                pids3 = pids;
                str = statsFile;
                iArr = curPids;
                z = false;
            } else {
                st.added = z;
                st.working = z;
                int curStatsIndex3 = curStatsIndex + 1;
                if (st.interesting) {
                    long uptime = SystemClock.uptimeMillis();
                    long[] procStats2 = this.mProcessStatsData;
                    long uptime2 = uptime;
                    if (Process.readProcFile(st.statFile.toString(), PROCESS_STATS_FORMAT, null, procStats2, null)) {
                        uptime = procStats2[0];
                        long majfaults = procStats2[1];
                        long minfaults = uptime;
                        uptime = procStats2[2] * this.mJiffyMillis;
                        i5 = NP;
                        long stime = this.mJiffyMillis * procStats2[3];
                        i6 = NS;
                        i7 = i3;
                        if (uptime == st.base_utime && stime == st.base_stime) {
                            st.rel_utime = 0;
                            st.rel_stime = 0;
                            st.rel_minfaults = 0;
                            st.rel_majfaults = 0;
                            if (st.active) {
                                st.active = false;
                            }
                            pids = pids3;
                        } else {
                            boolean z2;
                            long majfaults2;
                            long uptime3;
                            long minfaults2;
                            if (st.active) {
                                z2 = true;
                            } else {
                                z2 = true;
                                st.active = true;
                            }
                            if (i < 0) {
                                getName(st, st.cmdlineFile);
                                if (st.threadStats != null) {
                                    majfaults2 = majfaults;
                                    uptime3 = uptime2;
                                    minfaults2 = minfaults;
                                    majfaults = uptime;
                                    i3 = st;
                                    pids = pids3;
                                    pids2 = true;
                                    this.mCurThreadPids = collectStats(st.threadsDir, pid, false, this.mCurThreadPids, st.threadStats);
                                    i3.rel_uptime = uptime3 - i3.base_uptime;
                                    i3.base_uptime = uptime3;
                                    i3.rel_utime = (int) (majfaults - i3.base_utime);
                                    i3.rel_stime = (int) (stime - i3.base_stime);
                                    i3.base_utime = majfaults;
                                    i3.base_stime = stime;
                                    i3.rel_minfaults = (int) (minfaults2 - i3.base_minfaults);
                                    i3.rel_majfaults = (int) (majfaults2 - i3.base_majfaults);
                                    i3.base_minfaults = minfaults2;
                                    i3.base_majfaults = majfaults2;
                                    i3.working = pids2;
                                }
                            }
                            i3 = st;
                            pid2 = pid;
                            long[] jArr = procStats2;
                            majfaults2 = majfaults;
                            pids = pids3;
                            pids2 = z2;
                            uptime3 = uptime2;
                            minfaults2 = minfaults;
                            majfaults = uptime;
                            i3.rel_uptime = uptime3 - i3.base_uptime;
                            i3.base_uptime = uptime3;
                            i3.rel_utime = (int) (majfaults - i3.base_utime);
                            i3.rel_stime = (int) (stime - i3.base_stime);
                            i3.base_utime = majfaults;
                            i3.base_stime = stime;
                            i3.rel_minfaults = (int) (minfaults2 - i3.base_minfaults);
                            i3.rel_majfaults = (int) (majfaults2 - i3.base_majfaults);
                            i3.base_minfaults = minfaults2;
                            i3.base_majfaults = majfaults2;
                            i3.working = pids2;
                        }
                    } else {
                        pids = pids3;
                        i5 = NP;
                        i6 = NS;
                        i7 = i3;
                    }
                } else {
                    pids = pids3;
                    i5 = NP;
                    i6 = NS;
                    i7 = i3;
                }
                curStatsIndex = curStatsIndex3;
                NS = i6;
                arrayList = allProcs;
            }
            i4 = parentPid;
            i2 = i7 + 1;
            i = i4;
            NP = i5;
            pids3 = pids;
            str = statsFile;
            iArr = curPids;
            z = false;
        }
        while (curStatsIndex < NS) {
            Stats st4 = (Stats) arrayList.get(curStatsIndex);
            st4.rel_utime = 0;
            st4.rel_stime = 0;
            st4.rel_minfaults = 0;
            st4.rel_majfaults = 0;
            st4.removed = pids2;
            st4.working = pids2;
            arrayList.remove(curStatsIndex);
            NS--;
        }
        return pids;
    }

    public long getCpuTimeForPid(int pid) {
        synchronized (this.mSinglePidStatsData) {
            String statFile = new StringBuilder();
            statFile.append("/proc/");
            statFile.append(pid);
            statFile.append("/stat");
            statFile = statFile.toString();
            long[] statsData = this.mSinglePidStatsData;
            if (Process.readProcFile(statFile, PROCESS_STATS_FORMAT, null, statsData, null)) {
                long j = this.mJiffyMillis * (statsData[2] + statsData[3]);
                return j;
            }
            return 0;
        }
    }

    public final int getLastUserTime() {
        return this.mRelUserTime;
    }

    public final int getLastSystemTime() {
        return this.mRelSystemTime;
    }

    public final int getLastIoWaitTime() {
        return this.mRelIoWaitTime;
    }

    public final int getLastIrqTime() {
        return this.mRelIrqTime;
    }

    public final int getLastSoftIrqTime() {
        return this.mRelSoftIrqTime;
    }

    public final int getLastIdleTime() {
        return this.mRelIdleTime;
    }

    public final boolean hasGoodLastStats() {
        return this.mRelStatsAreGood;
    }

    public final float getTotalCpuPercent() {
        int denom = ((this.mRelUserTime + this.mRelSystemTime) + this.mRelIrqTime) + this.mRelIdleTime;
        if (denom <= 0) {
            return 0.0f;
        }
        return (((float) ((this.mRelUserTime + this.mRelSystemTime) + this.mRelIrqTime)) * 100.0f) / ((float) denom);
    }

    final void buildWorkingProcs() {
        if (!this.mWorkingProcsSorted) {
            this.mWorkingProcs.clear();
            int N = this.mProcStats.size();
            for (int i = 0; i < N; i++) {
                Stats stats = (Stats) this.mProcStats.get(i);
                if (stats.working) {
                    this.mWorkingProcs.add(stats);
                    if (stats.threadStats != null && stats.threadStats.size() > 1) {
                        stats.workingThreads.clear();
                        int M = stats.threadStats.size();
                        for (int j = 0; j < M; j++) {
                            Stats tstats = (Stats) stats.threadStats.get(j);
                            if (tstats.working) {
                                stats.workingThreads.add(tstats);
                            }
                        }
                        Collections.sort(stats.workingThreads, sLoadComparator);
                    }
                }
            }
            Collections.sort(this.mWorkingProcs, sLoadComparator);
            this.mWorkingProcsSorted = true;
        }
    }

    public final int countStats() {
        return this.mProcStats.size();
    }

    public final Stats getStats(int index) {
        return (Stats) this.mProcStats.get(index);
    }

    public final List<Stats> getStats(FilterStats filter) {
        ArrayList<Stats> statses = new ArrayList(this.mProcStats.size());
        int N = this.mProcStats.size();
        for (int p = 0; p < N; p++) {
            Stats stats = (Stats) this.mProcStats.get(p);
            if (filter.needed(stats)) {
                statses.add(stats);
            }
        }
        return statses;
    }

    public final int countWorkingStats() {
        buildWorkingProcs();
        return this.mWorkingProcs.size();
    }

    public final Stats getWorkingStats(int index) {
        return (Stats) this.mWorkingProcs.get(index);
    }

    public final String printCurrentLoad() {
        Writer sw = new StringWriter();
        PrintWriter pw = new FastPrintWriter(sw, false, 128);
        pw.print("Load: ");
        pw.print(this.mLoad1);
        pw.print(" / ");
        pw.print(this.mLoad5);
        pw.print(" / ");
        pw.println(this.mLoad15);
        pw.flush();
        return sw.toString();
    }

    public final String printCurrentState(long now) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        buildWorkingProcs();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new FastPrintWriter((Writer) sw, false, 1024);
        pw.print("CPU usage from ");
        if (now > this.mLastSampleTime) {
            pw.print(now - this.mLastSampleTime);
            pw.print("ms to ");
            pw.print(now - this.mCurrentSampleTime);
            pw.print("ms ago");
        } else {
            pw.print(this.mLastSampleTime - now);
            pw.print("ms to ");
            pw.print(this.mCurrentSampleTime - now);
            pw.print("ms later");
        }
        pw.print(" (");
        pw.print(sdf.format(new Date(this.mLastSampleWallTime)));
        pw.print(" to ");
        pw.print(sdf.format(new Date(this.mCurrentSampleWallTime)));
        pw.print(")");
        long sampleTime = this.mCurrentSampleTime - this.mLastSampleTime;
        long sampleRealTime = this.mCurrentSampleRealTime - this.mLastSampleRealTime;
        long j = 0;
        if (sampleRealTime > 0) {
            j = (sampleTime * 100) / sampleRealTime;
        }
        long percAwake = j;
        if (percAwake != 100) {
            pw.print(" with ");
            pw.print(percAwake);
            pw.print("% awake");
        }
        pw.println(":");
        int totalTime = ((((this.mRelUserTime + this.mRelSystemTime) + this.mRelIoWaitTime) + this.mRelIrqTime) + this.mRelSoftIrqTime) + this.mRelIdleTime;
        int N = this.mWorkingProcs.size();
        int i = 0;
        while (true) {
            int i2 = i;
            long percAwake2;
            PrintWriter pw2;
            StringWriter sw2;
            SimpleDateFormat sdf2;
            if (i2 < N) {
                Stats st = (Stats) this.mWorkingProcs.get(i2);
                String str = st.added ? " +" : st.removed ? " -" : "  ";
                percAwake2 = percAwake;
                Stats st2 = st;
                int i3 = i2;
                int N2 = N;
                pw2 = pw;
                sw2 = sw;
                sdf2 = sdf;
                printProcessCPU(pw, str, st.pid, st.name, (int) st.rel_uptime, st.rel_utime, st.rel_stime, 0, 0, 0, st.rel_minfaults, st.rel_majfaults);
                Stats st3 = st2;
                if (!st3.removed && st3.workingThreads != null) {
                    int M = st3.workingThreads.size();
                    i = 0;
                    while (true) {
                        int j2 = i;
                        if (j2 >= M) {
                            break;
                        }
                        Stats tst = (Stats) st3.workingThreads.get(j2);
                        str = tst.added ? "   +" : tst.removed ? "   -" : "    ";
                        int j3 = j2;
                        int M2 = M;
                        Stats st4 = st3;
                        printProcessCPU(pw2, str, tst.pid, tst.name, (int) st3.rel_uptime, tst.rel_utime, tst.rel_stime, 0, 0, 0, 0, null);
                        i = j3 + 1;
                        M = M2;
                        st3 = st4;
                    }
                }
                i = i3 + 1;
                sdf = sdf2;
                Object obj = null;
                percAwake = percAwake2;
                N = N2;
                pw = pw2;
                sw = sw2;
            } else {
                percAwake2 = percAwake;
                pw2 = pw;
                sw2 = sw;
                sdf2 = sdf;
                printProcessCPU(pw2, "", -1, "TOTAL", totalTime, this.mRelUserTime, this.mRelSystemTime, this.mRelIoWaitTime, this.mRelIrqTime, this.mRelSoftIrqTime, 0, 0);
                pw2.flush();
                return sw2.toString();
            }
        }
    }

    private void printRatio(PrintWriter pw, long numerator, long denominator) {
        long thousands = (1000 * numerator) / denominator;
        long hundreds = thousands / 10;
        pw.print(hundreds);
        if (hundreds < 10) {
            long remainder = thousands - (10 * hundreds);
            if (remainder != 0) {
                pw.print('.');
                pw.print(remainder);
            }
        }
    }

    private void printProcessCPU(PrintWriter pw, String prefix, int pid, String label, int totalTime, int user, int system, int iowait, int irq, int softIrq, int minFaults, int majFaults) {
        PrintWriter printWriter = pw;
        int i = pid;
        int i2 = user;
        int i3 = system;
        int i4 = iowait;
        int i5 = irq;
        int i6 = softIrq;
        int i7 = minFaults;
        int i8 = majFaults;
        pw.print(prefix);
        int totalTime2 = totalTime == 0 ? 1 : totalTime;
        printRatio(printWriter, (long) ((((i2 + i3) + i4) + i5) + i6), (long) totalTime2);
        printWriter.print("% ");
        if (i >= 0) {
            printWriter.print(i);
            printWriter.print("/");
        }
        printWriter.print(label);
        printWriter.print(": ");
        PrintWriter printWriter2 = printWriter;
        printRatio(printWriter2, (long) i2, (long) totalTime2);
        printWriter.print("% user + ");
        printRatio(printWriter2, (long) i3, (long) totalTime2);
        printWriter.print("% kernel");
        if (i4 > 0) {
            printWriter.print(" + ");
            printRatio(printWriter, (long) i4, (long) totalTime2);
            printWriter.print("% iowait");
        }
        if (i5 > 0) {
            printWriter.print(" + ");
            printRatio(printWriter, (long) i5, (long) totalTime2);
            printWriter.print("% irq");
        }
        if (i6 > 0) {
            printWriter.print(" + ");
            printRatio(printWriter, (long) i6, (long) totalTime2);
            printWriter.print("% softirq");
        }
        if (i7 > 0 || i8 > 0) {
            printWriter.print(" / faults:");
            if (i7 > 0) {
                printWriter.print(" ");
                printWriter.print(i7);
                printWriter.print(" minor");
            }
            if (i8 > 0) {
                printWriter.print(" ");
                printWriter.print(i8);
                printWriter.print(" major");
            }
        }
        pw.println();
    }

    private String readFile(String file, char endChar) {
        ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            int len = is.read(this.mBuffer);
            is.close();
            if (len > 0) {
                int i = 0;
                while (i < len) {
                    if (this.mBuffer[i] == endChar) {
                        break;
                    }
                    i++;
                }
                String str = new String(this.mBuffer, 0, i);
                IoUtils.closeQuietly(is);
                StrictMode.setThreadPolicy(savedPolicy);
                return str;
            }
        } catch (FileNotFoundException | IOException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(is);
            StrictMode.setThreadPolicy(savedPolicy);
        }
        IoUtils.closeQuietly(is);
        StrictMode.setThreadPolicy(savedPolicy);
        return null;
    }

    private void getName(Stats st, String cmdlineFile) {
        String newName = st.name;
        if (st.name == null || st.name.equals("app_process") || st.name.equals("<pre-initialized>")) {
            String cmdName = readFile(cmdlineFile, null);
            if (cmdName != null && cmdName.length() > 1) {
                newName = cmdName;
                int i = newName.lastIndexOf("/");
                if (i > 0 && i < newName.length() - 1) {
                    newName = newName.substring(i + 1);
                }
            }
            if (newName == null) {
                newName = st.baseName;
            }
        }
        if (st.name == null || !newName.equals(st.name)) {
            st.name = newName;
            st.nameWidth = onMeasureProcessName(st.name);
        }
    }
}
