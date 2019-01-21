package com.android.server.rms.iaware.memory.utils;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import java.util.Iterator;
import java.util.List;

public final class PackageTracker {
    private static int INDEX_KILL_AVG = 3;
    private static int INDEX_KILL_CUR = 2;
    private static int INDEX_KILL_MAX = 4;
    private static int INDEX_KILL_MINUTE = 1;
    static final long INTERVAL_DAY = 86400000;
    static final long INTERVAL_HOUR = 9000000;
    static final long INTERVAL_MINUTE = 30000;
    static final long INTERVAL_TRIGGER = 1800000;
    private static final String TAG = "AwareMem_PkgTracker";
    static final int[][] mKillThresHold = new int[][]{new int[]{0, 2, 5, 30, 45}, new int[]{0, 2, 5, 30, 40}, new int[]{0, 2, 5, 20, 30}, new int[]{1, 2, 5, 20, 30}, new int[]{1, 2, 5, 20, 30}, new int[]{1, 2, 5, 20, 30}};
    private static final Object mLock = new Object();
    private static PackageTracker sPackageTracker;
    private boolean mDebug = false;
    private boolean mEnable = true;
    private PackageStats mPackageStats = new PackageStats();
    private long mlastTriggerTime = SystemClock.elapsedRealtime();

    public enum KilledFrequency {
        FREQUENCY_NORMAL,
        FREQUENCY_HIGH,
        FREQUENCY_CRITICAL
    }

    public static PackageTracker getInstance() {
        PackageTracker packageTracker;
        synchronized (mLock) {
            if (sPackageTracker == null) {
                sPackageTracker = new PackageTracker();
            }
            packageTracker = sPackageTracker;
        }
        return packageTracker;
    }

    public boolean isEnabled() {
        return this.mEnable;
    }

    @SuppressLint({"PreferForInArrayList"})
    public void trackKillEvent(int uid, List<AwareProcessInfo> processList) {
        if (getInstance().isEnabled()) {
            long timeStamp = SystemClock.uptimeMillis();
            for (AwareProcessInfo info : processList) {
                if (info.mProcInfo != null) {
                    if (info.mProcInfo.mPackageName != null) {
                        Iterator it = info.mProcInfo.mPackageName.iterator();
                        while (it.hasNext()) {
                            String packageName = (String) it.next();
                            getInstance().addKillRecord(packageName, uid, info.mProcInfo.mProcessName, timeStamp);
                        }
                    }
                }
            }
        }
    }

    public void addKillRecord(String packageName, int uid, String processName, long timeStamp) {
        addRecord(2, MemoryConstant.MEM_POLICY_KILLACTION, packageName, uid, processName, timeStamp);
    }

    public void addStartRecord(String reason, String packageName, int uid, String processName, long timeStamp) {
        int recordValue = 0;
        if ("restart".equalsIgnoreCase(reason)) {
            recordValue = 1;
        }
        addRecord(recordValue, reason, packageName, uid, processName, timeStamp);
    }

    public void addExitRecord(String reason, String packageName, int uid, String processName, long timeStamp) {
        int recordValue = -1;
        if ("died".equalsIgnoreCase(reason)) {
            recordValue = 3;
        }
        if ("crash".equalsIgnoreCase(reason)) {
            recordValue = 4;
        }
        if ("anr".equalsIgnoreCase(reason)) {
            recordValue = 5;
        }
        if (recordValue > -1) {
            addRecord(recordValue, reason, packageName, uid, processName, timeStamp);
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0084, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append(" addRecord, reason=");
            r0.append(r2);
            r0.append(", packageName=");
            r0.append(r10);
            r0.append(", processName=");
            r0.append(r11);
            checkTime(r14, r0.toString());
     */
    /* JADX WARNING: Missing block: B:19:0x00a8, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addRecord(int recordValue, String reason, String packageName, int uid, String processName, long timeStamp) {
        Throwable th;
        String str = reason;
        String str2 = packageName;
        String str3 = processName;
        if (!this.mEnable) {
            return;
        }
        String str4;
        if (TextUtils.isEmpty(packageName)) {
            str4 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("addRecord, empty packageName, uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", packageName=");
            stringBuilder.append(str2);
            AwareLog.w(str4, stringBuilder.toString());
            return;
        }
        int i = uid;
        synchronized (mLock) {
            try {
                update(false);
                long startTime = SystemClock.elapsedRealtime();
                try {
                    long totalCount = this.mPackageStats.addRecord(recordValue, str2, i, str3, timeStamp);
                    if (this.mDebug) {
                        str4 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("addRecord, reason=");
                        stringBuilder2.append(str);
                        stringBuilder2.append(", totalCount=");
                        stringBuilder2.append(totalCount);
                        stringBuilder2.append(", packageName=");
                        stringBuilder2.append(str2);
                        stringBuilder2.append(", processName=");
                        stringBuilder2.append(str3);
                        AwareLog.d(str4, stringBuilder2.toString());
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    private void update(boolean quickUpdated) {
        long nowTime = SystemClock.elapsedRealtime();
        if ((quickUpdated ? 30000 : 1800000) <= nowTime - this.mlastTriggerTime) {
            this.mPackageStats.cleanRange(SystemClock.uptimeMillis(), INTERVAL_HOUR);
            checkTime(nowTime, " cleanRange");
            this.mPackageStats.calcMax();
            checkTime(nowTime, " calcMax");
            this.mlastTriggerTime = nowTime;
        }
    }

    public boolean isPackageCriticalFrequency(String packageName, int uid) {
        return getPackageKilledFrequency(packageName, uid) == KilledFrequency.FREQUENCY_CRITICAL;
    }

    public boolean isPackageHighFrequency(String packageName, int uid) {
        return getPackageKilledFrequency(packageName, uid) == KilledFrequency.FREQUENCY_HIGH;
    }

    private KilledFrequency getPackageKilledFrequency(String packageName, int uid) {
        if (!this.mEnable) {
            return KilledFrequency.FREQUENCY_NORMAL;
        }
        synchronized (mLock) {
            RecordTable state = this.mPackageStats.getPackageState(packageName, uid);
            KilledFrequency killedFrequency;
            if (state == null) {
                killedFrequency = KilledFrequency.FREQUENCY_NORMAL;
                return killedFrequency;
            }
            if (this.mDebug) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getPackageKilledFrequency:");
                stringBuilder.append(state.mPackageName);
                stringBuilder.append(", uid:");
                stringBuilder.append(state.mUid);
                AwareLog.d(str, stringBuilder.toString());
            }
            update(true);
            long now = SystemClock.uptimeMillis();
            int[] array = new int[]{2, 1, 0};
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < array.length) {
                    int index = array[i2];
                    killedFrequency = compareRecordThreshold(state, index, mKillThresHold[index], now, state.mProcesses.size());
                    if (killedFrequency != KilledFrequency.FREQUENCY_NORMAL) {
                        return killedFrequency;
                    }
                    i = i2 + 1;
                } else {
                    return KilledFrequency.FREQUENCY_NORMAL;
                }
            }
        }
    }

    public boolean isProcessCriticalFrequency(String packageName, String processName, int uid) {
        return getProcessKilledFrequency(packageName, processName, uid) == KilledFrequency.FREQUENCY_CRITICAL;
    }

    public boolean isProcessHighFrequency(String packageName, String processName, int uid) {
        return getProcessKilledFrequency(packageName, processName, uid) == KilledFrequency.FREQUENCY_HIGH;
    }

    private KilledFrequency getProcessKilledFrequency(String packageName, String processName, int uid) {
        Throwable th;
        String str = processName;
        if (!this.mEnable) {
            return KilledFrequency.FREQUENCY_NORMAL;
        }
        synchronized (mLock) {
            try {
                try {
                    PackageState state = this.mPackageStats.getPackageState(packageName, uid);
                    KilledFrequency killedFrequency;
                    if (state == null) {
                        killedFrequency = KilledFrequency.FREQUENCY_NORMAL;
                        return killedFrequency;
                    }
                    ProcessState processState = (ProcessState) state.mProcesses.get(str);
                    if (processState == null) {
                        killedFrequency = KilledFrequency.FREQUENCY_NORMAL;
                        return killedFrequency;
                    }
                    if (this.mDebug) {
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getProcessKilledFrequency:");
                        stringBuilder.append(str);
                        stringBuilder.append(" ");
                        stringBuilder.append(state.mPackageName);
                        stringBuilder.append(", uid:");
                        stringBuilder.append(state.mUid);
                        AwareLog.d(str2, stringBuilder.toString());
                    }
                    update(true);
                    long now = SystemClock.uptimeMillis();
                    int[] array = new int[]{2, 1, 0};
                    int i = 0;
                    while (true) {
                        int i2 = i;
                        if (i2 < array.length) {
                            int index = array[i2];
                            killedFrequency = compareRecordThreshold(processState.getRecordTable(), index, mKillThresHold[index], now, 1);
                            if (killedFrequency != KilledFrequency.FREQUENCY_NORMAL) {
                                return killedFrequency;
                            }
                            i = i2 + 1;
                        } else {
                            return KilledFrequency.FREQUENCY_NORMAL;
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                String str3 = packageName;
                int i3 = uid;
                throw th;
            }
        }
    }

    private KilledFrequency compareRecordThreshold(RecordTable state, int record, int[] threshold, long now, int processSize) {
        int i;
        RecordTable recordTable = state;
        int i2 = record;
        int processSize2 = processSize > 0 ? processSize : 1;
        List<Long> timeList = recordTable.mRecordTimeStamps[i2].mTimeStamps;
        int criticalCount = 0;
        int i3 = timeList.size() - 1;
        while (true) {
            i = i3;
            if (i < 0) {
                break;
            }
            if (30000 * ((long) state.getScaleTimes()) < now - ((Long) timeList.get(i)).longValue()) {
                break;
            }
            criticalCount++;
            i3 = i - 1;
        }
        criticalCount /= processSize2;
        StringBuilder stringBuilder;
        if (threshold[INDEX_KILL_MINUTE] <= criticalCount) {
            if (this.mDebug) {
                String str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("compareThresHold critical, index=");
                stringBuilder.append(i2);
                stringBuilder.append(", critical Count=");
                stringBuilder.append(criticalCount);
                stringBuilder.append(", timeScale=");
                stringBuilder.append(state.getScaleTimes());
                AwareLog.d(str, stringBuilder.toString());
            }
            state.updateScaleTimes();
            return KilledFrequency.FREQUENCY_CRITICAL;
        }
        i = timeList.size() / processSize2;
        if (threshold[INDEX_KILL_CUR] <= i) {
            if (this.mDebug) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("compareThresHold high, index=");
                stringBuilder2.append(i2);
                stringBuilder2.append(", curCount=");
                stringBuilder2.append(i);
                AwareLog.d(str2, stringBuilder2.toString());
            }
            return KilledFrequency.FREQUENCY_HIGH;
        }
        long firstTime = recordTable.mFirstTimes[i2];
        long totalCount = recordTable.mTotalRecordCounts[i2];
        long avgCount = totalCount / ((long) processSize2);
        if (86400000 < now - firstTime) {
            avgCount = (86400000 * totalCount) / (now - firstTime);
            if (((long) threshold[INDEX_KILL_AVG]) <= avgCount) {
                if (this.mDebug) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("compareThresHold high, index=");
                    stringBuilder3.append(i2);
                    stringBuilder3.append(", avgCount=");
                    stringBuilder3.append(avgCount);
                    AwareLog.d(str3, stringBuilder3.toString());
                }
                return KilledFrequency.FREQUENCY_HIGH;
            }
        }
        long maxCount = recordTable.mMaxRecordCounts[i2];
        maxCount = (maxCount == 0 ? recordTable.mDayRecordCounts[i2] : maxCount) / ((long) processSize2);
        String str4;
        if (((long) threshold[INDEX_KILL_MAX]) <= maxCount) {
            if (this.mDebug) {
                str4 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("compareThresHold high, index=");
                stringBuilder.append(i2);
                stringBuilder.append(", maxCount=");
                stringBuilder.append(maxCount);
                AwareLog.d(str4, stringBuilder.toString());
            }
            return KilledFrequency.FREQUENCY_HIGH;
        }
        if (this.mDebug) {
            str4 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("compareThresHold normal, index=");
            stringBuilder.append(i2);
            stringBuilder.append(", curCount=");
            stringBuilder.append(i);
            stringBuilder.append(", avgCount=");
            stringBuilder.append(avgCount);
            stringBuilder.append(", maxCount=");
            stringBuilder.append(maxCount);
            stringBuilder.append(", criticalCount=");
            stringBuilder.append(criticalCount);
            stringBuilder.append(", processSize=");
            stringBuilder.append(processSize2);
            AwareLog.d(str4, stringBuilder.toString());
        }
        return KilledFrequency.FREQUENCY_NORMAL;
    }

    private void checkTime(long startTime, String action) {
        long now = SystemClock.elapsedRealtime();
        if (now - startTime > 10) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            stringBuilder.append(action);
            stringBuilder.append("] takes too much time:");
            stringBuilder.append(now - startTime);
            AwareLog.w(str, stringBuilder.toString());
        }
    }
}
