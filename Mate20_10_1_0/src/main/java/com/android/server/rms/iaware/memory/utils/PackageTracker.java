package com.android.server.rms.iaware.memory.utils;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.rms.iaware.AwareLog;
import android.text.TextUtils;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.rms.iaware.memory.utils.PackageStats;
import java.util.ArrayList;
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
    private static final int MAX_KILL_FREQ = 30;
    private static final int MAX_KILL_TIMES = 32;
    private static final String TAG = "AwareMem_PkgTracker";
    static final int[][] mKillThresHold = {new int[]{0, 2, 5, 30, 45}, new int[]{0, 2, 5, 30, 40}, new int[]{0, 2, 5, 20, 30}, new int[]{1, 2, 5, 20, 30}, new int[]{1, 2, 5, 20, 30}, new int[]{1, 2, 5, 20, 30}};
    private static final Object mLock = new Object();
    private static PackageTracker sPackageTracker;
    private boolean configPkgFreqSwitch = false;
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

    public void enableDebug() {
        this.mDebug = true;
    }

    public void disableDebug() {
        this.mDebug = false;
    }

    @SuppressLint({"PreferForInArrayList"})
    public void trackKillEvent(int uid, List<AwareProcessInfo> processList) {
        if (isEnabled()) {
            List<String> pkgList = new ArrayList<>();
            long timeStamp = SystemClock.uptimeMillis();
            for (AwareProcessInfo info : processList) {
                if (!(info.procProcInfo == null || info.procProcInfo.mPackageName == null)) {
                    Iterator it = info.procProcInfo.mPackageName.iterator();
                    while (it.hasNext()) {
                        String packageName = (String) it.next();
                        if (this.mDebug) {
                            AwareLog.d(TAG, "trackKillEvent:" + packageName + ", uid:" + uid + ", process=" + info.procProcInfo.mProcessName);
                        }
                        if (!pkgList.contains(packageName)) {
                            pkgList.add(packageName);
                        }
                        addKillRecord(packageName, uid, info.procProcInfo.mProcessName, timeStamp);
                    }
                }
            }
            if (this.configPkgFreqSwitch) {
                addPkgRecord(pkgList, uid, timeStamp);
            }
        }
    }

    private void addPkgRecord(List<String> pkgList, int uid, long timeStamp) {
        for (String packageName : pkgList) {
            if (TextUtils.isEmpty(packageName)) {
                AwareLog.w(TAG, "addRecord, empty packageName, uid=" + uid + ", packageName=" + packageName);
            } else {
                long totalCount = addPkgRecordLocked(2, packageName, uid, timeStamp);
                if (this.mDebug) {
                    AwareLog.d(TAG, "addPkgRecord, reason=kill, totalCount=" + totalCount + ", packageName=" + packageName);
                }
            }
        }
    }

    private long addPkgRecordLocked(int recordValue, String packageName, int uid, long timeStamp) {
        long addPkgRecord;
        if (TextUtils.isEmpty(packageName)) {
            return 0;
        }
        synchronized (mLock) {
            addPkgRecord = this.mPackageStats.addPkgRecord(recordValue, packageName, uid, timeStamp);
        }
        return addPkgRecord;
    }

    private void addKillRecord(String packageName, int uid, String processName, long timeStamp) {
        addProcRecord(2, MemoryConstant.MEM_POLICY_KILLACTION, packageName, uid, processName, timeStamp);
        if (!this.configPkgFreqSwitch) {
            addPkgRecordLocked(2, packageName, uid, timeStamp);
        }
    }

    public void addStartRecord(String reason, String packageName, int uid, String processName, long timeStamp) {
        if (isEnabled()) {
            int recordValue = 0;
            if ("restart".equalsIgnoreCase(reason)) {
                recordValue = 1;
            }
            addProcRecord(recordValue, reason, packageName, uid, processName, timeStamp);
            long totalCount = addPkgRecordLocked(recordValue, packageName, uid, timeStamp);
            if (this.mDebug) {
                AwareLog.d(TAG, "addPkgRecord, reason=start, totalCount=" + totalCount + ", packageName=" + packageName);
            }
        }
    }

    public void addExitRecord(String reason, String packageName, int uid, String processName, long timeStamp) {
        if (isEnabled()) {
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
                addProcRecord(recordValue, reason, packageName, uid, processName, timeStamp);
                long totalCount = addPkgRecordLocked(recordValue, packageName, uid, timeStamp);
                if (this.mDebug) {
                    AwareLog.d(TAG, "addPkgRecord, reason=exit, totalCount=" + totalCount + ", packageName=" + packageName);
                }
            }
        }
    }

    private void addProcRecord(int recordValue, String reason, String packageName, int uid, String processName, long timeStamp) {
        if (TextUtils.isEmpty(packageName)) {
            AwareLog.w(TAG, "addProcRecord, empty packageName, uid=" + uid + ", packageName=" + packageName);
            return;
        }
        synchronized (mLock) {
            try {
                update(false);
                long startTime = SystemClock.elapsedRealtime();
                try {
                    long totalCount = this.mPackageStats.addProcRecord(recordValue, packageName, uid, processName, timeStamp);
                    if (this.mDebug) {
                        AwareLog.d(TAG, "addProcRecord, reason=" + reason + ", totalCount=" + totalCount + ", packageName=" + packageName + ", processName=" + processName);
                    }
                    checkTime(startTime, " addProcRecord, reason=" + reason + ", packageName=" + packageName + ", processName=" + processName);
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void update(boolean quickUpdated) {
        long nowTime = SystemClock.elapsedRealtime();
        if ((quickUpdated ? 30000 : INTERVAL_TRIGGER) <= nowTime - this.mlastTriggerTime) {
            this.mPackageStats.cleanRange(SystemClock.uptimeMillis(), INTERVAL_HOUR);
            checkTime(nowTime, " cleanRange");
            this.mPackageStats.calcMax();
            checkTime(nowTime, " calcMax");
            this.mlastTriggerTime = nowTime;
        }
    }

    public boolean updateKillThresHold(boolean pkgFreqSwitch, int killFreqLimit, int scaleTimes) {
        int[] array;
        synchronized (mLock) {
            this.configPkgFreqSwitch = pkgFreqSwitch;
            if (killFreqLimit >= 1) {
                if (killFreqLimit <= 30) {
                    if (scaleTimes >= 1) {
                        if (scaleTimes <= 32) {
                            for (int index : new int[]{2, 1, 0}) {
                                mKillThresHold[index][INDEX_KILL_MINUTE] = killFreqLimit;
                            }
                            RecordTable.saveScaleTimes(scaleTimes);
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }
    }

    public KilledFrequency getPackageKilledFrequency(String packageName, int uid) {
        if (!this.mEnable) {
            return KilledFrequency.FREQUENCY_NORMAL;
        }
        synchronized (mLock) {
            try {
                PackageStats.PackageState state = this.mPackageStats.getPackageState(packageName, uid);
                if (state == null) {
                    return KilledFrequency.FREQUENCY_NORMAL;
                }
                if (this.mDebug) {
                    AwareLog.d(TAG, "killed pkg:" + state.mPackageName + ", uid:" + state.mUid + ", size:" + state.mProcesses.size());
                }
                update(true);
                long now = SystemClock.uptimeMillis();
                int[] array = {2, 1, 0};
                KilledFrequency lastFreq = KilledFrequency.FREQUENCY_NORMAL;
                for (int index : array) {
                    KilledFrequency frequency = compareRecordThreshold(state, index, mKillThresHold[index], now, this.configPkgFreqSwitch ? 0 : state.mProcesses.size());
                    if (frequency.ordinal() > lastFreq.ordinal()) {
                        lastFreq = frequency;
                    }
                }
                return lastFreq;
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        }
    }

    public KilledFrequency getProcessKilledFrequency(String packageName, String processName, int uid) {
        if (!this.mEnable) {
            return KilledFrequency.FREQUENCY_NORMAL;
        }
        synchronized (mLock) {
            try {
                PackageStats.PackageState state = this.mPackageStats.getPackageState(packageName, uid);
                if (state == null) {
                    return KilledFrequency.FREQUENCY_NORMAL;
                }
                PackageStats.ProcessState processState = state.mProcesses.get(processName);
                if (processState == null) {
                    return KilledFrequency.FREQUENCY_NORMAL;
                }
                if (this.mDebug) {
                    AwareLog.d(TAG, "getProcessKilledFrequency:" + processName + " " + state.mPackageName + ", uid:" + state.mUid);
                }
                update(true);
                long now = SystemClock.uptimeMillis();
                int[] array = {2, 1, 0};
                KilledFrequency lastFreq = KilledFrequency.FREQUENCY_NORMAL;
                for (int index : array) {
                    KilledFrequency frequency = compareRecordThreshold(processState.getRecordTable(), index, mKillThresHold[index], now, 1);
                    if (frequency.ordinal() > lastFreq.ordinal()) {
                        lastFreq = frequency;
                    }
                }
                return lastFreq;
            } catch (Throwable th) {
                th = th;
                throw th;
            }
        }
    }

    private KilledFrequency compareRecordThreshold(RecordTable state, int record, int[] threshold, long now, int processSize) {
        int processSize2 = processSize > 0 ? processSize : 1;
        List<Long> timeList = state.mRecordTimeStamps[record].mTimeStamps;
        int criticalCount = getCurTimes(state, timeList, now, ((long) state.getScaleTimes()) * 30000) / processSize2;
        if (threshold[INDEX_KILL_MINUTE] <= criticalCount) {
            if (this.mDebug) {
                AwareLog.d(TAG, "compareThresHold critical, index=" + record + ", critical Count=" + criticalCount + ", timeScale=" + state.getScaleTimes());
            }
            state.updateScaleTimes();
            return KilledFrequency.FREQUENCY_CRITICAL;
        }
        int curCount = getCurTimes(state, timeList, now, INTERVAL_HOUR) / processSize2;
        if (threshold[INDEX_KILL_CUR] <= curCount) {
            if (this.mDebug) {
                AwareLog.d(TAG, "compareThresHold high, index=" + record + ", curCount=" + curCount);
            }
            return KilledFrequency.FREQUENCY_HIGH;
        }
        long firstTime = state.mFirstTimes[record];
        long totalCount = state.mTotalRecordCounts[record];
        long avgCount = totalCount / ((long) processSize2);
        if (86400000 < now - firstTime) {
            avgCount = (86400000 * totalCount) / (now - firstTime);
            if (((long) threshold[INDEX_KILL_AVG]) <= avgCount) {
                if (this.mDebug) {
                    AwareLog.d(TAG, "compareThresHold high, index=" + record + ", avgCount=" + avgCount);
                }
                return KilledFrequency.FREQUENCY_HIGH;
            }
        }
        long maxCount = state.mDayRecordCounts[record] / ((long) processSize2);
        if (((long) threshold[INDEX_KILL_MAX]) <= maxCount) {
            if (this.mDebug) {
                AwareLog.d(TAG, "compareThresHold high, index=" + record + ", maxCount=" + maxCount);
            }
            return KilledFrequency.FREQUENCY_HIGH;
        }
        if (this.mDebug) {
            AwareLog.d(TAG, "compareThresHold normal, index=" + record + ", curCount=" + curCount + ", avgCount=" + avgCount + ", maxCount=" + maxCount + ", criticalCount=" + criticalCount + ", processSize=" + processSize2);
        }
        return KilledFrequency.FREQUENCY_NORMAL;
    }

    private int getCurTimes(RecordTable state, List<Long> timeList, long now, long interval) {
        int times = 0;
        int i = timeList.size() - 1;
        while (i >= 0 && interval >= now - timeList.get(i).longValue()) {
            times++;
            i--;
        }
        return times;
    }

    private void checkTime(long startTime, String action) {
        long now = SystemClock.elapsedRealtime();
        if (now - startTime > 10) {
            AwareLog.w(TAG, "[" + action + "] takes too much time:" + (now - startTime));
        }
    }
}
