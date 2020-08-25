package com.android.server.magicwin;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Slog;
import com.android.server.magicwin.HwMagicWinStatistics;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class HwMagicWinStatistics {
    private static final int IDX_COUNT = 1;
    private static final int IDX_DRAG_LEFT = 4;
    private static final int IDX_DRAG_MIDDLE = 3;
    private static final int IDX_DRAG_RIGHT = 5;
    private static final int IDX_MW_DURATION = 2;
    private static final int IDX_PKG = 0;
    private static final String MGC_CACHE_FILE = "magic_cache";
    private static final long MILLIS_PER_SECOND = 1000;
    private static final long REPORT_INTERVAL = 86400000;
    private static final String SPLIT = ",";
    private static final int STATE_DRAG_LEFT = 1;
    private static final int STATE_DRAG_MIDDLE = 0;
    private static final int STATE_DRAG_RIGHT = 2;
    public static final int STATE_MAGIC_WIN = -1;
    public static final int STATE_NOT_MAGIC_WIN = -2;
    private static final String SYSTEM_DIR = "system";
    private static final String TAG = "HwMagicWinStatistics";
    private static final int USE_INFO_CNT = 6;
    private static HwMagicWinStatistics sInstance = null;
    private State mCurrentState;
    private long mLastReportTime = System.currentTimeMillis();
    private State mNoneState = new NoneMwModeState(this, -2);
    private Map<Integer, State> mStates = new HashMap();
    private Map<String, Usage> mUsages = new HashMap();

    private HwMagicWinStatistics() {
        this.mStates.put(-2, this.mNoneState);
        this.mStates.put(-1, new BaseState(this, -1));
        this.mStates.put(1, new DragLeftState(this, 1));
        this.mStates.put(0, new DragMiddleState(this, 0));
        this.mStates.put(2, new DragRightState(this, 2));
        this.mCurrentState = this.mNoneState;
    }

    public static HwMagicWinStatistics getInstance() {
        HwMagicWinStatistics hwMagicWinStatistics;
        synchronized (HwMagicWinStatistics.class) {
            if (sInstance == null) {
                sInstance = new HwMagicWinStatistics();
            }
            hwMagicWinStatistics = sInstance;
        }
        return hwMagicWinStatistics;
    }

    /* access modifiers changed from: private */
    public Usage getUsage(String pkg) {
        this.mUsages.putIfAbsent(pkg, new Usage(pkg));
        return this.mUsages.get(pkg);
    }

    /* access modifiers changed from: private */
    public class Usage {
        private int mCount = 0;
        private long mDragLeftDuration = 0;
        private long mDragMiddleDuration = 0;
        private long mDragRightDuration = 0;
        private long mMwDuration = 0;
        private String mPackage;

        Usage(String pkg) {
            this.mPackage = pkg;
        }

        public void count() {
            this.mCount++;
        }

        public void count(int cnt) {
            this.mCount += cnt;
        }

        public void appendTotalDuration(long millisecond) {
            this.mMwDuration += millisecond;
        }

        public void appendDragLeftDuration(long millisecond) {
            this.mDragLeftDuration += millisecond;
        }

        public void appendDragMiddleDuration(long millisecond) {
            this.mDragMiddleDuration += millisecond;
        }

        public void appendDragRightDuration(long millisecond) {
            this.mDragRightDuration += millisecond;
        }

        public String toString() {
            return "{\"app\":\"" + this.mPackage + "\", \"count\":" + this.mCount + ", \"duration\":" + (this.mMwDuration / 1000) + ", \"duration0\":" + (this.mDragMiddleDuration / 1000) + ", \"duration1\":" + (this.mDragLeftDuration / 1000) + ", \"duration2\":" + (this.mDragRightDuration / 1000) + "}";
        }

        public String toLine() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(this.mPackage);
            buffer.append(",");
            buffer.append(this.mCount);
            buffer.append(",");
            buffer.append(this.mMwDuration);
            buffer.append(",");
            buffer.append(this.mDragMiddleDuration);
            buffer.append(",");
            buffer.append(this.mDragLeftDuration);
            buffer.append(",");
            buffer.append(this.mDragRightDuration);
            return buffer.toString();
        }
    }

    public void stopTick() {
        this.mCurrentState.count();
        this.mCurrentState.onStop();
        this.mCurrentState = this.mNoneState;
    }

    public void startTick(HwMagicWindowConfig config, String pkg, int state) {
        startTick(pkg, (state != -1 || !config.isDragable(pkg)) ? state : config.getAppDragMode(pkg));
    }

    private void startTick(String pkg, int state) {
        if (!this.mCurrentState.isSameState(pkg, state)) {
            if (state == -2 || TextUtils.isEmpty(pkg)) {
                stopTick();
                return;
            }
            this.mCurrentState.onStop();
            this.mCurrentState = this.mStates.getOrDefault(Integer.valueOf(state), this.mNoneState);
            this.mCurrentState.onStart(pkg);
        }
    }

    private interface State {
        boolean isSameState(String str, int i);

        default void onStop() {
        }

        default void onStart(String pkg) {
        }

        default void count() {
        }
    }

    private final class NoneMwModeState implements State {
        private int mState;
        private HwMagicWinStatistics mStatistics;

        NoneMwModeState(HwMagicWinStatistics statistics, int state) {
            this.mStatistics = statistics;
            this.mState = state;
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.State
        public boolean isSameState(String pkgName, int state) {
            return this.mState == state;
        }
    }

    private class BaseState implements State {
        private String mPackage;
        private long mStartTimeStramp = 0;
        private int mState;
        private HwMagicWinStatistics mStatistics;
        private long mStopTimeStramp = 0;

        BaseState(HwMagicWinStatistics statistics, int state) {
            this.mStatistics = statistics;
            this.mState = state;
        }

        public void computeTotalDuration() {
            this.mStatistics.getUsage(this.mPackage).appendTotalDuration(this.mStopTimeStramp - this.mStartTimeStramp);
        }

        public void computeDragLeftDuration() {
            this.mStatistics.getUsage(this.mPackage).appendDragLeftDuration(this.mStopTimeStramp - this.mStartTimeStramp);
        }

        public void computeDragMiddleDuration() {
            this.mStatistics.getUsage(this.mPackage).appendDragMiddleDuration(this.mStopTimeStramp - this.mStartTimeStramp);
        }

        public void computeDragRightDuration() {
            this.mStatistics.getUsage(this.mPackage).appendDragRightDuration(this.mStopTimeStramp - this.mStartTimeStramp);
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.State
        public void onStart(String pkgName) {
            this.mStartTimeStramp = System.currentTimeMillis();
            this.mPackage = pkgName;
            Slog.i(HwMagicWinStatistics.TAG, "start trick for app:" + this.mPackage + " , state = " + this.mState);
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.State
        public void onStop() {
            this.mStopTimeStramp = System.currentTimeMillis();
            computeTotalDuration();
            Slog.i(HwMagicWinStatistics.TAG, "stop trick for app:" + this.mPackage + " , state = " + this.mState);
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.State
        public void count() {
            this.mStatistics.getUsage(this.mPackage).count();
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.State
        public boolean isSameState(String pkgName, int state) {
            return this.mPackage.equals(pkgName) && this.mState == state;
        }
    }

    private final class DragLeftState extends BaseState {
        DragLeftState(HwMagicWinStatistics statistics, int state) {
            super(statistics, state);
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.BaseState, com.android.server.magicwin.HwMagicWinStatistics.State
        public void onStop() {
            super.onStop();
            super.computeDragLeftDuration();
        }
    }

    private final class DragMiddleState extends BaseState {
        DragMiddleState(HwMagicWinStatistics statistics, int state) {
            super(statistics, state);
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.BaseState, com.android.server.magicwin.HwMagicWinStatistics.State
        public void onStop() {
            super.onStop();
            super.computeDragMiddleDuration();
        }
    }

    private final class DragRightState extends BaseState {
        DragRightState(HwMagicWinStatistics statistics, int state) {
            super(statistics, state);
        }

        @Override // com.android.server.magicwin.HwMagicWinStatistics.BaseState, com.android.server.magicwin.HwMagicWinStatistics.State
        public void onStop() {
            super.onStop();
            super.computeDragRightDuration();
        }
    }

    public void handleReport(Context context) {
        if (System.currentTimeMillis() - this.mLastReportTime > 86400000) {
            Slog.i(TAG, "Trigger report");
            this.mUsages.forEach(new BiConsumer(context) {
                /* class com.android.server.magicwin.$$Lambda$HwMagicWinStatistics$Vv2SpSWbszObjxDyvDDqlmxXrUA */
                private final /* synthetic */ Context f$0;

                {
                    this.f$0 = r1;
                }

                @Override // java.util.function.BiConsumer
                public final void accept(Object obj, Object obj2) {
                    HwMagicWinStatistics.lambda$handleReport$0(this.f$0, (String) obj, (Usage) obj2);
                }
            });
            this.mUsages.clear();
            this.mLastReportTime = System.currentTimeMillis();
            return;
        }
        Slog.i(TAG, "Not Ready To Trigger report");
    }

    static /* synthetic */ void lambda$handleReport$0(Context context, String key, Usage value) {
        Flog.bdReport(context, 10106, value.toString());
        Slog.i(TAG, value.toString());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x00c6, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x00c7, code lost:
        $closeResource(r3, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x00ca, code lost:
        throw r4;
     */
    public void loadCache() {
        Slog.i(TAG, "Load cache.");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(new File(Environment.getDataDirectory(), SYSTEM_DIR), MGC_CACHE_FILE)));
            String line = reader.readLine();
            if (line != null) {
                this.mLastReportTime = Long.parseLong(line);
                Slog.i(TAG, " -> " + line);
            }
            while (true) {
                String line2 = reader.readLine();
                if (line2 != null) {
                    Slog.i(TAG, " -> " + line2);
                    String[] useInfos = line2.split(",", 0);
                    if (useInfos.length == 6) {
                        Usage usage = new Usage(useInfos[0]);
                        usage.count(Integer.valueOf(useInfos[1]).intValue());
                        usage.appendTotalDuration(Long.valueOf(useInfos[2]).longValue());
                        usage.appendDragMiddleDuration(Long.valueOf(useInfos[3]).longValue());
                        usage.appendDragLeftDuration(Long.valueOf(useInfos[4]).longValue());
                        usage.appendDragRightDuration(Long.valueOf(useInfos[5]).longValue());
                        this.mUsages.putIfAbsent(useInfos[0], usage);
                    }
                } else {
                    $closeResource(null, reader);
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Slog.w(TAG, "Timestamp format error. " + e);
        } catch (FileNotFoundException e2) {
            Slog.w(TAG, "Cache not found. " + e2);
        } catch (IOException e3) {
            Slog.e(TAG, "Load cache exception: " + e3);
        } catch (Exception e4) {
            Slog.e(TAG, "loadCache error. " + e4);
        }
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

    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0065, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0066, code lost:
        $closeResource(r3, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0069, code lost:
        throw r4;
     */
    public void saveCache() {
        try {
            BufferedWriter wr = new BufferedWriter(new FileWriter(new File(new File(Environment.getDataDirectory(), SYSTEM_DIR), MGC_CACHE_FILE), false));
            Slog.i(TAG, "Save cache.");
            wr.write(String.valueOf(this.mLastReportTime));
            for (Map.Entry<String, Usage> host : this.mUsages.entrySet()) {
                wr.newLine();
                wr.write(host.getValue().toLine());
            }
            wr.flush();
            this.mUsages.clear();
            $closeResource(null, wr);
        } catch (IOException e) {
            Slog.e(TAG, "Save cache exception: ", e);
        }
    }
}
