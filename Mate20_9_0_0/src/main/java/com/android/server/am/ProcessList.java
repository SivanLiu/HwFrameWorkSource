package com.android.server.am;

import android.content.res.Resources;
import android.graphics.Point;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.MemInfoReader;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerService.H;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public final class ProcessList {
    static final int BACKUP_APP_ADJ = 300;
    static final int BSERVICE_APP_THRESHOLD = SystemProperties.getInt("ro.sys.fw.bservice_limit", 5);
    static final int CACHED_APP_MAX_ADJ = 906;
    static final int CACHED_APP_MIN_ADJ = 900;
    static final int EMPTY_APP_PERCENT = SystemProperties.getInt("ro.sys.fw.empty_app_percent", 50);
    static final boolean ENABLE_B_SERVICE_PROPAGATION = SystemProperties.getBoolean("ro.sys.fw.bservice_enable", false);
    static final int FOREGROUND_APP_ADJ = 0;
    static final int HEAVY_WEIGHT_APP_ADJ = 400;
    static final int HOME_APP_ADJ = 600;
    static final int INVALID_ADJ = -10000;
    static final byte LMK_MEMORYCOMPACT = (byte) 53;
    static final byte LMK_MEMORYSHRINK = (byte) 52;
    static final byte LMK_PROCPRIO = (byte) 1;
    static final byte LMK_PROCRECLAIM = (byte) 54;
    static final byte LMK_PROCREMOVE = (byte) 2;
    static final byte LMK_TARGET = (byte) 0;
    static final int MAX_CACHED_APPS = SystemProperties.getInt("ro.sys.fw.bg_apps_limit", 32);
    private static final int MAX_EMPTY_APPS = computeEmptyProcessLimit(MAX_CACHED_APPS);
    static final long MAX_EMPTY_TIME = 1800000;
    static final int MIN_BSERVICE_AGING_TIME = SystemProperties.getInt("ro.sys.fw.bservice_age", PSS_TEST_FIRST_BACKGROUND_INTERVAL);
    static final int MIN_CACHED_APPS = 2;
    static final int MIN_CRASH_INTERVAL = 60000;
    static final int NATIVE_ADJ = -1000;
    static final int PAGE_SIZE = 4096;
    static final int PERCEPTIBLE_APP_ADJ = 200;
    static final int PERSISTENT_PROC_ADJ = -800;
    static final int PERSISTENT_SERVICE_ADJ = -700;
    static final int PREVIOUS_APP_ADJ = 700;
    public static final int PROC_MEM_CACHED = 4;
    public static final int PROC_MEM_IMPORTANT = 2;
    public static final int PROC_MEM_NUM = 5;
    public static final int PROC_MEM_PERSISTENT = 0;
    public static final int PROC_MEM_SERVICE = 3;
    public static final int PROC_MEM_TOP = 1;
    public static final int PSS_ALL_INTERVAL = 1200000;
    private static final int PSS_FIRST_ASLEEP_BACKGROUND_INTERVAL = 30000;
    private static final int PSS_FIRST_ASLEEP_CACHED_INTERVAL = 60000;
    private static final int PSS_FIRST_ASLEEP_PERSISTENT_INTERVAL = 60000;
    private static final int PSS_FIRST_ASLEEP_TOP_INTERVAL = 20000;
    private static final int PSS_FIRST_BACKGROUND_INTERVAL = 20000;
    private static final int PSS_FIRST_CACHED_INTERVAL = 20000;
    private static final int PSS_FIRST_PERSISTENT_INTERVAL = 30000;
    private static final int PSS_FIRST_TOP_INTERVAL = 10000;
    public static final int PSS_MAX_INTERVAL = 3600000;
    public static final int PSS_MIN_TIME_FROM_STATE_CHANGE = 15000;
    public static final int PSS_SAFE_TIME_FROM_STATE_CHANGE = 1000;
    private static final int PSS_SAME_CACHED_INTERVAL = 600000;
    private static final int PSS_SAME_IMPORTANT_INTERVAL = 600000;
    private static final int PSS_SAME_PERSISTENT_INTERVAL = 600000;
    private static final int PSS_SAME_SERVICE_INTERVAL = 300000;
    private static final int PSS_SAME_TOP_INTERVAL = 60000;
    private static final int PSS_TEST_FIRST_BACKGROUND_INTERVAL = 5000;
    private static final int PSS_TEST_FIRST_TOP_INTERVAL = 3000;
    public static final int PSS_TEST_MIN_TIME_FROM_STATE_CHANGE = 10000;
    private static final int PSS_TEST_SAME_BACKGROUND_INTERVAL = 15000;
    private static final int PSS_TEST_SAME_IMPORTANT_INTERVAL = 10000;
    static final int SCHED_GROUP_BACKGROUND = 0;
    static final int SCHED_GROUP_DEFAULT = 2;
    static final int SCHED_GROUP_KEY_BACKGROUND = 7;
    static final int SCHED_GROUP_RESTRICTED = 1;
    static final int SCHED_GROUP_TOP_APP = 3;
    static final int SCHED_GROUP_TOP_APP_BOUND = 4;
    static final int SERVICE_ADJ = 500;
    static final int SERVICE_B_ADJ = 800;
    static final int SYSTEM_ADJ = -900;
    private static final String TAG = "ActivityManager";
    static final int TRIM_CACHED_APPS = computeTrimCachedApps();
    static final int TRIM_CACHE_PERCENT = SystemProperties.getInt("ro.sys.fw.trim_cache_percent", 100);
    static final int TRIM_CRITICAL_THRESHOLD = 3;
    static final int TRIM_EMPTY_APPS = computeTrimEmptyApps();
    static final int TRIM_EMPTY_PERCENT = SystemProperties.getInt("ro.sys.fw.trim_empty_percent", 100);
    static final long TRIM_ENABLE_MEMORY = SystemProperties.getLong("ro.sys.fw.trim_enable_memory", 1073741824);
    static final int TRIM_LOW_THRESHOLD = 5;
    static final int UNKNOWN_ADJ = 1001;
    static final boolean USE_TRIM_SETTINGS = SystemProperties.getBoolean("ro.sys.fw.use_trim_settings", false);
    static final int VISIBLE_APP_ADJ = 100;
    static final int VISIBLE_APP_LAYER_MAX = 99;
    private static final long[] sFirstAsleepPssTimes = new long[]{60000, 20000, 30000, 30000, 60000};
    private static final long[] sFirstAwakePssTimes = new long[]{30000, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, 20000, 20000, 20000};
    private static OutputStream sLmkdOutputStream;
    private static LocalSocket sLmkdSocket;
    private static final int[] sProcStateToProcMem = new int[]{0, 0, 1, 2, 2, 2, 2, 2, 2, 3, 4, 1, 2, 4, 4, 4, 4, 4, 4};
    private static final long[] sSameAsleepPssTimes = new long[]{600000, 60000, 600000, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, 600000};
    private static final long[] sSameAwakePssTimes = new long[]{600000, 60000, 600000, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS, 600000};
    private static final long[] sTestFirstPssTimes = new long[]{3000, 3000, 5000, 5000, 5000};
    private static final long[] sTestSamePssTimes = new long[]{15000, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, 15000, 15000};
    private long mCachedRestoreLevel;
    private boolean mHaveDisplaySize;
    private final int[] mOomAdj = new int[]{0, 100, 200, 300, CACHED_APP_MIN_ADJ, CACHED_APP_MAX_ADJ};
    private final int[] mOomMinFree = new int[this.mOomAdj.length];
    private final int[] mOomMinFreeHigh = new int[]{73728, 92160, 110592, 129024, 147456, 184320};
    private final int[] mOomMinFreeLow = new int[]{12288, 18432, 24576, 36864, 43008, 49152};
    private final long mTotalMemMb;

    public static final class ProcStateMemTracker {
        final int[] mHighestMem = new int[5];
        int mPendingHighestMemState;
        int mPendingMemState;
        float mPendingScalingFactor;
        final float[] mScalingFactor = new float[5];
        int mTotalHighestMem = 4;

        public ProcStateMemTracker() {
            for (int i = 0; i < 5; i++) {
                this.mHighestMem[i] = 5;
                this.mScalingFactor[i] = 1.0f;
            }
            this.mPendingMemState = -1;
        }

        public void dumpLine(PrintWriter pw) {
            pw.print("best=");
            pw.print(this.mTotalHighestMem);
            pw.print(" (");
            boolean needSep = false;
            for (int i = 0; i < 5; i++) {
                if (this.mHighestMem[i] < 5) {
                    if (needSep) {
                        pw.print(", ");
                    }
                    pw.print(i);
                    pw.print("=");
                    pw.print(this.mHighestMem[i]);
                    pw.print(" ");
                    pw.print(this.mScalingFactor[i]);
                    pw.print("x");
                    needSep = true;
                }
            }
            pw.print(")");
            if (this.mPendingMemState >= 0) {
                pw.print(" / pending state=");
                pw.print(this.mPendingMemState);
                pw.print(" highest=");
                pw.print(this.mPendingHighestMemState);
                pw.print(" ");
                pw.print(this.mPendingScalingFactor);
                pw.print("x");
            }
            pw.println();
        }
    }

    public static boolean allowTrim() {
        return Process.getTotalMemory() < TRIM_ENABLE_MEMORY;
    }

    public static int computeTrimEmptyApps() {
        if (USE_TRIM_SETTINGS && allowTrim()) {
            return (MAX_EMPTY_APPS * TRIM_EMPTY_PERCENT) / 100;
        }
        return MAX_EMPTY_APPS / 2;
    }

    public static int computeTrimCachedApps() {
        if (USE_TRIM_SETTINGS && allowTrim()) {
            return (MAX_CACHED_APPS * TRIM_CACHE_PERCENT) / 100;
        }
        return (MAX_CACHED_APPS - MAX_EMPTY_APPS) / 3;
    }

    ProcessList() {
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        this.mTotalMemMb = minfo.getTotalSize() / 1048576;
        updateOomLevels(0, 0, false);
    }

    void applyDisplaySize(WindowManagerService wm) {
        if (!this.mHaveDisplaySize) {
            Point p = new Point();
            wm.getBaseDisplaySize(0, p);
            if (p.x != 0 && p.y != 0) {
                updateOomLevels(p.x, p.y, true);
                this.mHaveDisplaySize = true;
            }
        }
    }

    private void updateOomLevels(int displayWidth, int displayHeight, boolean write) {
        int high;
        int i;
        float scaleMem = ((float) (this.mTotalMemMb - 350)) / 350.0f;
        float scaleDisp = (((float) (displayWidth * displayHeight)) - ((float) 384000)) / ((float) (1024000 - 384000));
        float scale = scaleMem > scaleDisp ? scaleMem : scaleDisp;
        if (scale < 0.0f) {
            scale = 0.0f;
        } else if (scale > 1.0f) {
            scale = 1.0f;
        }
        int minfree_adj = Resources.getSystem().getInteger(17694807);
        int minfree_abs = Resources.getSystem().getInteger(17694806);
        boolean is64bit = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        int i2 = 0;
        while (i2 < this.mOomAdj.length) {
            int low = this.mOomMinFreeLow[i2];
            high = this.mOomMinFreeHigh[i2];
            if (is64bit) {
                if (i2 == 4) {
                    high = (high * 3) / 2;
                } else if (i2 == 5) {
                    high = (high * 7) / 4;
                }
            }
            float scaleMem2 = scaleMem;
            this.mOomMinFree[i2] = (int) (((float) low) + (((float) (high - low)) * scale));
            i2++;
            scaleMem = scaleMem2;
        }
        if (minfree_abs >= 0) {
            for (i = 0; i < this.mOomAdj.length; i++) {
                this.mOomMinFree[i] = (int) ((((float) minfree_abs) * ((float) this.mOomMinFree[i])) / ((float) this.mOomMinFree[this.mOomAdj.length - 1]));
            }
        }
        if (minfree_adj != 0) {
            for (i = 0; i < this.mOomAdj.length; i++) {
                int[] iArr = this.mOomMinFree;
                iArr[i] = iArr[i] + ((int) ((((float) minfree_adj) * ((float) this.mOomMinFree[i])) / ((float) this.mOomMinFree[this.mOomAdj.length - 1])));
                if (this.mOomMinFree[i] < 0) {
                    this.mOomMinFree[i] = 0;
                }
            }
        }
        this.mCachedRestoreLevel = (getMemLevel(CACHED_APP_MAX_ADJ) / 1024) / 3;
        i = (((displayWidth * displayHeight) * 4) * 3) / 1024;
        int reserve_adj = Resources.getSystem().getInteger(17694788);
        high = Resources.getSystem().getInteger(17694787);
        if (high >= 0) {
            i = high;
        }
        if (reserve_adj != 0) {
            i += reserve_adj;
            if (i < 0) {
                i = 0;
            }
        }
        if (write) {
            ByteBuffer buf = ByteBuffer.allocate(4 * ((2 * this.mOomAdj.length) + 1));
            int i3 = 0;
            buf.putInt(0);
            while (i3 < this.mOomAdj.length) {
                buf.putInt((this.mOomMinFree[i3] * 1024) / 4096);
                buf.putInt(this.mOomAdj[i3]);
                i3++;
            }
            writeLmkd(buf);
            SystemProperties.set("sys.sysctl.extra_free_kbytes", Integer.toString(i));
        }
    }

    public static int computeEmptyProcessLimit(int totalProcessLimit) {
        if (USE_TRIM_SETTINGS && allowTrim()) {
            return (EMPTY_APP_PERCENT * totalProcessLimit) / 100;
        }
        return totalProcessLimit / 2;
    }

    private static String buildOomTag(String prefix, String space, int val, int base) {
        StringBuilder stringBuilder;
        if (val != base) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("+");
            stringBuilder.append(Integer.toString(val - base));
            return stringBuilder.toString();
        } else if (space == null) {
            return prefix;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            return stringBuilder.toString();
        }
    }

    public static String makeOomAdjString(int setAdj) {
        if (setAdj >= CACHED_APP_MIN_ADJ) {
            return buildOomTag("cch", "  ", setAdj, CACHED_APP_MIN_ADJ);
        }
        if (setAdj >= SERVICE_B_ADJ) {
            return buildOomTag("svcb ", null, setAdj, SERVICE_B_ADJ);
        }
        if (setAdj >= PREVIOUS_APP_ADJ) {
            return buildOomTag("prev ", null, setAdj, PREVIOUS_APP_ADJ);
        }
        if (setAdj >= 600) {
            return buildOomTag("home ", null, setAdj, 600);
        }
        if (setAdj >= 500) {
            return buildOomTag("svc  ", null, setAdj, 500);
        }
        if (setAdj >= HEAVY_WEIGHT_APP_ADJ) {
            return buildOomTag("hvy  ", null, setAdj, HEAVY_WEIGHT_APP_ADJ);
        }
        if (setAdj >= 300) {
            return buildOomTag("bkup ", null, setAdj, 300);
        }
        if (setAdj >= 200) {
            return buildOomTag("prcp ", null, setAdj, 200);
        }
        if (setAdj >= 100) {
            return buildOomTag("vis  ", null, setAdj, 100);
        }
        if (setAdj >= 0) {
            return buildOomTag("fore ", null, setAdj, 0);
        }
        if (setAdj >= PERSISTENT_SERVICE_ADJ) {
            return buildOomTag("psvc ", null, setAdj, PERSISTENT_SERVICE_ADJ);
        }
        if (setAdj >= PERSISTENT_PROC_ADJ) {
            return buildOomTag("pers ", null, setAdj, PERSISTENT_PROC_ADJ);
        }
        if (setAdj >= SYSTEM_ADJ) {
            return buildOomTag("sys  ", null, setAdj, SYSTEM_ADJ);
        }
        if (setAdj >= -1000) {
            return buildOomTag("ntv  ", null, setAdj, -1000);
        }
        return Integer.toString(setAdj);
    }

    public static String makeProcStateString(int curProcState) {
        switch (curProcState) {
            case 0:
                return "PER ";
            case 1:
                return "PERU";
            case 2:
                return "TOP ";
            case 3:
                return "FGS ";
            case 4:
                return "BFGS";
            case 5:
                return "IMPF";
            case 6:
                return "IMPB";
            case 7:
                return "TRNB";
            case 8:
                return "BKUP";
            case 9:
                return "SVC ";
            case 10:
                return "RCVR";
            case 11:
                return "TPSL";
            case 12:
                return "HVY ";
            case 13:
                return "HOME";
            case 14:
                return "LAST";
            case 15:
                return "CAC ";
            case 16:
                return "CACC";
            case 17:
                return "CRE ";
            case 18:
                return "CEM ";
            case H.REPORT_WINDOWS_CHANGE /*19*/:
                return "NONE";
            default:
                return "??";
        }
    }

    public static int makeProcStateProtoEnum(int curProcState) {
        switch (curProcState) {
            case -1:
                return 999;
            case 0:
                return 1000;
            case 1:
                return 1001;
            case 2:
                return 1002;
            case 3:
                return 1003;
            case 4:
                return 1004;
            case 5:
                return 1005;
            case 6:
                return 1006;
            case 7:
                return 1007;
            case 8:
                return 1008;
            case 9:
                return 1009;
            case 10:
                return 1010;
            case 11:
                return 1011;
            case 12:
                return 1012;
            case 13:
                return 1013;
            case 14:
                return 1014;
            case 15:
                return 1015;
            case 16:
                return 1016;
            case 17:
                return 1017;
            case 18:
                return 1018;
            case H.REPORT_WINDOWS_CHANGE /*19*/:
                return 1019;
            default:
                return 998;
        }
    }

    public static void appendRamKb(StringBuilder sb, long ramKb) {
        int j = 0;
        int fact = 10;
        while (j < 6) {
            if (ramKb < ((long) fact)) {
                sb.append(' ');
            }
            j++;
            fact *= 10;
        }
        sb.append(ramKb);
    }

    public static boolean procStatesDifferForMem(int procState1, int procState2) {
        return sProcStateToProcMem[procState1] != sProcStateToProcMem[procState2];
    }

    public static long minTimeFromStateChange(boolean test) {
        return test ? JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY : 15000;
    }

    public static void commitNextPssTime(ProcStateMemTracker tracker) {
        if (tracker.mPendingMemState >= 0) {
            tracker.mHighestMem[tracker.mPendingMemState] = tracker.mPendingHighestMemState;
            tracker.mScalingFactor[tracker.mPendingMemState] = tracker.mPendingScalingFactor;
            tracker.mTotalHighestMem = tracker.mPendingHighestMemState;
            tracker.mPendingMemState = -1;
        }
    }

    public static void abortNextPssTime(ProcStateMemTracker tracker) {
        tracker.mPendingMemState = -1;
    }

    public static long computeNextPssTime(int procState, ProcStateMemTracker tracker, boolean test, boolean sleeping, long now) {
        boolean first;
        int memState = sProcStateToProcMem[procState];
        float scalingFactor = 1.0f;
        if (tracker != null) {
            int highestMemState = memState < tracker.mTotalHighestMem ? memState : tracker.mTotalHighestMem;
            first = highestMemState < tracker.mHighestMem[memState];
            tracker.mPendingMemState = memState;
            tracker.mPendingHighestMemState = highestMemState;
            if (first) {
                float scalingFactor2 = 1.0f;
                tracker.mPendingScalingFactor = 1.0f;
            } else {
                scalingFactor = tracker.mScalingFactor[memState];
                tracker.mPendingScalingFactor = 1.5f * scalingFactor;
            }
        } else {
            first = true;
        }
        long[] table = test ? first ? sTestFirstPssTimes : sTestSamePssTimes : first ? sleeping ? sFirstAsleepPssTimes : sFirstAwakePssTimes : sleeping ? sSameAsleepPssTimes : sSameAwakePssTimes;
        long delay = (long) (((float) table[memState]) * scalingFactor);
        if (delay > SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT) {
            delay = SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT;
        }
        return now + delay;
    }

    public long getMemLevel(int adjustment) {
        for (int i = 0; i < this.mOomAdj.length; i++) {
            if (adjustment <= this.mOomAdj[i]) {
                return (long) (this.mOomMinFree[i] * 1024);
            }
        }
        return (long) (this.mOomMinFree[this.mOomAdj.length - 1] * 1024);
    }

    long getCachedRestoreThresholdKb() {
        return this.mCachedRestoreLevel;
    }

    public static final void setOomAdj(int pid, int uid, int amt) {
        if (pid > 0 && amt != 1001) {
            long start = SystemClock.elapsedRealtime();
            ByteBuffer buf = ByteBuffer.allocate(16);
            buf.putInt(1);
            buf.putInt(pid);
            buf.putInt(uid);
            buf.putInt(amt);
            writeLmkd(buf);
            long now = SystemClock.elapsedRealtime();
            if (now - start > 250) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SLOW OOM ADJ: ");
                stringBuilder.append(now - start);
                stringBuilder.append("ms for pid ");
                stringBuilder.append(pid);
                stringBuilder.append(" = ");
                stringBuilder.append(amt);
                Slog.w("ActivityManager", stringBuilder.toString());
            }
        }
    }

    public static final void setOomAdj(int pid, int uid, int amt, String name) {
        if (amt != 1001) {
            long start = SystemClock.elapsedRealtime();
            int length = 0;
            byte[] nameLineByte = null;
            if (name != null) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(name);
                    stringBuilder.append("\n");
                    nameLineByte = stringBuilder.toString().getBytes("UTF-8");
                    length = nameLineByte.length;
                } catch (UnsupportedEncodingException e) {
                    Slog.w("AwareLog:CPU:ActivityManager", "unsupported encodeing: UTF-8");
                }
            }
            ByteBuffer buf = ByteBuffer.allocate(16 + length);
            buf.putInt(1);
            buf.putInt(pid);
            buf.putInt(uid);
            buf.putInt(amt);
            if (nameLineByte != null) {
                buf.put(nameLineByte);
            }
            writeLmkd(buf);
            long duration = SystemClock.elapsedRealtime() - start;
            if (duration > 250) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SLOW OOM ADJ: ");
                stringBuilder2.append(duration);
                stringBuilder2.append("ms for pid ");
                stringBuilder2.append(pid);
                stringBuilder2.append(" = ");
                stringBuilder2.append(amt);
                Slog.w("AwareLog:CPU:ActivityManager", stringBuilder2.toString());
            }
        }
    }

    public static final void remove(int pid) {
        if (pid > 0) {
            ByteBuffer buf = ByteBuffer.allocate(8);
            buf.putInt(2);
            buf.putInt(pid);
            writeLmkd(buf);
        }
    }

    private static boolean openLmkdSocket() {
        try {
            sLmkdSocket = new LocalSocket(3);
            sLmkdSocket.connect(new LocalSocketAddress("lmkd", Namespace.RESERVED));
            sLmkdOutputStream = sLmkdSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            Slog.w("ActivityManager", "lowmemorykiller daemon socket open failed");
            sLmkdSocket = null;
            return false;
        }
    }

    private static void writeLmkd(ByteBuffer buf) {
        int i = 0;
        while (i < 3) {
            if (sLmkdSocket != null || openLmkdSocket()) {
                try {
                    sLmkdOutputStream.write(buf.array(), 0, buf.position());
                    return;
                } catch (IOException e) {
                    Slog.w("ActivityManager", "Error writing to lowmemorykiller socket");
                    try {
                        sLmkdSocket.close();
                    } catch (IOException e2) {
                    }
                    sLmkdSocket = null;
                }
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e3) {
                }
                i++;
            }
        }
    }

    public static final void callMemoryShrinker(int value) {
        long start = SystemClock.elapsedRealtime();
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(52);
        buf.putInt(value);
        writeLmkd(buf);
        long now = SystemClock.elapsedRealtime();
        if (now - start > 250) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SLOW call memory shrinker: ");
            stringBuilder.append(now - start);
            Slog.w("ActivityManager", stringBuilder.toString());
        }
    }

    public static final void callMemoryCompact(int value) {
        long start = SystemClock.elapsedRealtime();
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(53);
        buf.putInt(value);
        writeLmkd(buf);
        long now = SystemClock.elapsedRealtime();
        if (now - start > 250) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SLOW call memory shrinker: ");
            stringBuilder.append(now - start);
            Slog.w("ActivityManager", stringBuilder.toString());
        }
    }

    public static final void callProcReclaim(int pid, int mode) {
        long start = SystemClock.elapsedRealtime();
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putInt(54);
        buf.putInt(pid);
        buf.putInt(mode);
        writeLmkd(buf);
        long now = SystemClock.elapsedRealtime();
        if (now - start > 250) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SLOW call process reclaim: ");
            stringBuilder.append(now - start);
            Slog.w("ActivityManager", stringBuilder.toString());
        }
    }

    public static final void setLmkByIAware(int[] lmkMinfree, int[] lmkAdj) {
        if (lmkMinfree.length == 6 && lmkAdj.length == 6) {
            long start = SystemClock.elapsedRealtime();
            ByteBuffer lmkBuf = ByteBuffer.allocate(4 * ((2 * lmkMinfree.length) + 1));
            int i = 0;
            lmkBuf.putInt(0);
            while (i < lmkAdj.length) {
                lmkBuf.putInt(lmkMinfree[i]);
                lmkBuf.putInt(lmkAdj[i]);
                i++;
            }
            writeLmkd(lmkBuf);
            long now = SystemClock.elapsedRealtime();
            if (now - start > 250) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SLOW call setLmkByIAware: ");
                stringBuilder.append(now - start);
                Slog.w("AwareLog", stringBuilder.toString());
            }
            return;
        }
        Slog.w("AwareLog", "SetLmkByIAware failed!");
    }
}
