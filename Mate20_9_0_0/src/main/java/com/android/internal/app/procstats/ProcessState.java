package com.android.internal.app.procstats;

import android.os.Parcel;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.internal.app.procstats.ProcessStats.PackageState;
import com.android.internal.app.procstats.ProcessStats.ProcessDataCollection;
import com.android.internal.app.procstats.ProcessStats.ProcessStateHolder;
import com.android.internal.app.procstats.ProcessStats.TotalMemoryUseCollection;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.widget.LockPatternUtils;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class ProcessState {
    public static final Comparator<ProcessState> COMPARATOR = new Comparator<ProcessState>() {
        public int compare(ProcessState lhs, ProcessState rhs) {
            if (lhs.mTmpTotalTime < rhs.mTmpTotalTime) {
                return -1;
            }
            if (lhs.mTmpTotalTime > rhs.mTmpTotalTime) {
                return 1;
            }
            return 0;
        }
    };
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PARCEL = false;
    private static final int[] PROCESS_STATE_TO_STATE = new int[]{0, 0, 1, 2, 2, 2, 3, 3, 4, 5, 7, 1, 8, 9, 10, 11, 12, 11, 13};
    private static final String TAG = "ProcessStats";
    private boolean mActive;
    private long mAvgCachedKillPss;
    private ProcessState mCommonProcess;
    private int mCurState = -1;
    private boolean mDead;
    private final DurationsTable mDurations;
    private int mLastPssState = -1;
    private long mLastPssTime;
    private long mMaxCachedKillPss;
    private long mMinCachedKillPss;
    private boolean mMultiPackage;
    private final String mName;
    private int mNumActiveServices;
    private int mNumCachedKill;
    private int mNumExcessiveCpu;
    private int mNumStartedServices;
    private final String mPackage;
    private final PssTable mPssTable;
    private long mStartTime;
    private final ProcessStats mStats;
    private long mTmpTotalTime;
    private final int mUid;
    private final long mVersion;
    public ProcessState tmpFoundSubProc;
    public int tmpNumInUse;

    static class PssAggr {
        long pss = 0;
        long samples = 0;

        PssAggr() {
        }

        void add(long newPss, long newSamples) {
            this.pss = ((long) ((((double) this.pss) * ((double) this.samples)) + (((double) newPss) * ((double) newSamples)))) / (this.samples + newSamples);
            this.samples += newSamples;
        }
    }

    public ProcessState(ProcessStats processStats, String pkg, int uid, long vers, String name) {
        this.mStats = processStats;
        this.mName = name;
        this.mCommonProcess = this;
        this.mPackage = pkg;
        this.mUid = uid;
        this.mVersion = vers;
        this.mDurations = new DurationsTable(processStats.mTableData);
        this.mPssTable = new PssTable(processStats.mTableData);
    }

    public ProcessState(ProcessState commonProcess, String pkg, int uid, long vers, String name, long now) {
        this.mStats = commonProcess.mStats;
        this.mName = name;
        this.mCommonProcess = commonProcess;
        this.mPackage = pkg;
        this.mUid = uid;
        this.mVersion = vers;
        this.mCurState = commonProcess.mCurState;
        this.mStartTime = now;
        this.mDurations = new DurationsTable(commonProcess.mStats.mTableData);
        this.mPssTable = new PssTable(commonProcess.mStats.mTableData);
    }

    public ProcessState clone(long now) {
        ProcessState pnew = new ProcessState(this, this.mPackage, this.mUid, this.mVersion, this.mName, now);
        pnew.mDurations.addDurations(this.mDurations);
        pnew.mPssTable.copyFrom(this.mPssTable, 10);
        pnew.mNumExcessiveCpu = this.mNumExcessiveCpu;
        pnew.mNumCachedKill = this.mNumCachedKill;
        pnew.mMinCachedKillPss = this.mMinCachedKillPss;
        pnew.mAvgCachedKillPss = this.mAvgCachedKillPss;
        pnew.mMaxCachedKillPss = this.mMaxCachedKillPss;
        pnew.mActive = this.mActive;
        pnew.mNumActiveServices = this.mNumActiveServices;
        pnew.mNumStartedServices = this.mNumStartedServices;
        return pnew;
    }

    public String getName() {
        return this.mName;
    }

    public ProcessState getCommonProcess() {
        return this.mCommonProcess;
    }

    public void makeStandalone() {
        this.mCommonProcess = this;
    }

    public String getPackage() {
        return this.mPackage;
    }

    public int getUid() {
        return this.mUid;
    }

    public long getVersion() {
        return this.mVersion;
    }

    public boolean isMultiPackage() {
        return this.mMultiPackage;
    }

    public void setMultiPackage(boolean val) {
        this.mMultiPackage = val;
    }

    public int getDurationsBucketCount() {
        return this.mDurations.getKeyCount();
    }

    public void add(ProcessState other) {
        this.mDurations.addDurations(other.mDurations);
        this.mPssTable.mergeStats(other.mPssTable);
        this.mNumExcessiveCpu += other.mNumExcessiveCpu;
        if (other.mNumCachedKill > 0) {
            addCachedKill(other.mNumCachedKill, other.mMinCachedKillPss, other.mAvgCachedKillPss, other.mMaxCachedKillPss);
        }
    }

    public void resetSafely(long now) {
        this.mDurations.resetTable();
        this.mPssTable.resetTable();
        this.mStartTime = now;
        this.mLastPssState = -1;
        this.mLastPssTime = 0;
        this.mNumExcessiveCpu = 0;
        this.mNumCachedKill = 0;
        this.mMaxCachedKillPss = 0;
        this.mAvgCachedKillPss = 0;
        this.mMinCachedKillPss = 0;
    }

    public void makeDead() {
        this.mDead = true;
    }

    private void ensureNotDead() {
        if (this.mDead) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ProcessState dead: name=");
            stringBuilder.append(this.mName);
            stringBuilder.append(" pkg=");
            stringBuilder.append(this.mPackage);
            stringBuilder.append(" uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(" common.name=");
            stringBuilder.append(this.mCommonProcess.mName);
            Slog.w("ProcessStats", stringBuilder.toString());
        }
    }

    public void writeToParcel(Parcel out, long now) {
        out.writeInt(this.mMultiPackage);
        this.mDurations.writeToParcel(out);
        this.mPssTable.writeToParcel(out);
        out.writeInt(0);
        out.writeInt(this.mNumExcessiveCpu);
        out.writeInt(this.mNumCachedKill);
        if (this.mNumCachedKill > 0) {
            out.writeLong(this.mMinCachedKillPss);
            out.writeLong(this.mAvgCachedKillPss);
            out.writeLong(this.mMaxCachedKillPss);
        }
    }

    public boolean readFromParcel(Parcel in, boolean fully) {
        boolean multiPackage = in.readInt() != 0;
        if (fully) {
            this.mMultiPackage = multiPackage;
        }
        if (!this.mDurations.readFromParcel(in) || !this.mPssTable.readFromParcel(in)) {
            return false;
        }
        in.readInt();
        this.mNumExcessiveCpu = in.readInt();
        this.mNumCachedKill = in.readInt();
        if (this.mNumCachedKill > 0) {
            this.mMinCachedKillPss = in.readLong();
            this.mAvgCachedKillPss = in.readLong();
            this.mMaxCachedKillPss = in.readLong();
        } else {
            this.mMaxCachedKillPss = 0;
            this.mAvgCachedKillPss = 0;
            this.mMinCachedKillPss = 0;
        }
        return true;
    }

    public void makeActive() {
        ensureNotDead();
        this.mActive = true;
    }

    public void makeInactive() {
        this.mActive = false;
    }

    public boolean isInUse() {
        return this.mActive || this.mNumActiveServices > 0 || this.mNumStartedServices > 0 || this.mCurState != -1;
    }

    public boolean isActive() {
        return this.mActive;
    }

    public boolean hasAnyData() {
        return (this.mDurations.getKeyCount() == 0 && this.mCurState == -1 && this.mPssTable.getKeyCount() == 0) ? false : true;
    }

    public void setState(int state, int memFactor, long now, ArrayMap<String, ProcessStateHolder> pkgList) {
        if (state < 0) {
            state = this.mNumStartedServices > 0 ? 6 + (memFactor * 14) : -1;
        } else {
            state = PROCESS_STATE_TO_STATE[state] + (memFactor * 14);
        }
        this.mCommonProcess.setState(state, now);
        if (this.mCommonProcess.mMultiPackage && pkgList != null) {
            for (int ip = pkgList.size() - 1; ip >= 0; ip--) {
                pullFixedProc(pkgList, ip).setState(state, now);
            }
        }
    }

    public void setState(int state, long now) {
        ensureNotDead();
        if (!this.mDead && this.mCurState != state) {
            commitStateTime(now);
            this.mCurState = state;
        }
    }

    public void commitStateTime(long now) {
        if (this.mCurState != -1) {
            long dur = now - this.mStartTime;
            if (dur > 0) {
                this.mDurations.addDuration(this.mCurState, dur);
            }
        }
        this.mStartTime = now;
    }

    public void incActiveServices(String serviceName) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.incActiveServices(serviceName);
        }
        this.mNumActiveServices++;
    }

    public void decActiveServices(String serviceName) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.decActiveServices(serviceName);
        }
        this.mNumActiveServices--;
        if (this.mNumActiveServices < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Proc active services underrun: pkg=");
            stringBuilder.append(this.mPackage);
            stringBuilder.append(" uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(" proc=");
            stringBuilder.append(this.mName);
            stringBuilder.append(" service=");
            stringBuilder.append(serviceName);
            Slog.wtfStack("ProcessStats", stringBuilder.toString());
            this.mNumActiveServices = 0;
        }
    }

    public void incStartedServices(int memFactor, long now, String serviceName) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.incStartedServices(memFactor, now, serviceName);
        }
        this.mNumStartedServices++;
        if (this.mNumStartedServices == 1 && this.mCurState == -1) {
            setState(6 + (memFactor * 14), now);
        }
    }

    public void decStartedServices(int memFactor, long now, String serviceName) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.decStartedServices(memFactor, now, serviceName);
        }
        this.mNumStartedServices--;
        if (this.mNumStartedServices == 0 && this.mCurState % 14 == 6) {
            setState(-1, now);
        } else if (this.mNumStartedServices < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Proc started services underrun: pkg=");
            stringBuilder.append(this.mPackage);
            stringBuilder.append(" uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(" name=");
            stringBuilder.append(this.mName);
            Slog.wtfStack("ProcessStats", stringBuilder.toString());
            this.mNumStartedServices = 0;
        }
    }

    public void addPss(long pss, long uss, long rss, boolean always, int type, long duration, ArrayMap<String, ProcessStateHolder> pkgList) {
        ArrayMap<String, ProcessStateHolder> arrayMap = pkgList;
        ensureNotDead();
        ProcessStats processStats;
        ProcessStats processStats2;
        switch (type) {
            case 0:
                processStats = this.mStats;
                processStats.mInternalSinglePssCount++;
                processStats2 = this.mStats;
                processStats2.mInternalSinglePssTime += duration;
                break;
            case 1:
                processStats = this.mStats;
                processStats.mInternalAllMemPssCount++;
                processStats2 = this.mStats;
                processStats2.mInternalAllMemPssTime += duration;
                break;
            case 2:
                processStats = this.mStats;
                processStats.mInternalAllPollPssCount++;
                processStats2 = this.mStats;
                processStats2.mInternalAllPollPssTime += duration;
                break;
            case 3:
                processStats = this.mStats;
                processStats.mExternalPssCount++;
                processStats2 = this.mStats;
                processStats2.mExternalPssTime += duration;
                break;
            case 4:
                processStats = this.mStats;
                processStats.mExternalSlowPssCount++;
                processStats2 = this.mStats;
                processStats2.mExternalSlowPssTime += duration;
                break;
        }
        if (always || this.mLastPssState != this.mCurState || SystemClock.uptimeMillis() >= this.mLastPssTime + LockPatternUtils.FAILED_ATTEMPT_TIMEOUT_MS) {
            this.mLastPssState = this.mCurState;
            this.mLastPssTime = SystemClock.uptimeMillis();
            if (this.mCurState != -1) {
                this.mCommonProcess.mPssTable.mergeStats(this.mCurState, 1, pss, pss, pss, uss, uss, uss, rss, rss, rss);
                if (this.mCommonProcess.mMultiPackage && arrayMap != null) {
                    for (int ip = pkgList.size() - 1; ip >= 0; ip--) {
                        pullFixedProc(arrayMap, ip).mPssTable.mergeStats(this.mCurState, 1, pss, pss, pss, uss, uss, uss, rss, rss, rss);
                    }
                }
            }
        }
    }

    public void reportExcessiveCpu(ArrayMap<String, ProcessStateHolder> pkgList) {
        ensureNotDead();
        ProcessState processState = this.mCommonProcess;
        processState.mNumExcessiveCpu++;
        if (this.mCommonProcess.mMultiPackage) {
            for (int ip = pkgList.size() - 1; ip >= 0; ip--) {
                ProcessState pullFixedProc = pullFixedProc(pkgList, ip);
                pullFixedProc.mNumExcessiveCpu++;
            }
        }
    }

    private void addCachedKill(int num, long minPss, long avgPss, long maxPss) {
        if (this.mNumCachedKill <= 0) {
            this.mNumCachedKill = num;
            this.mMinCachedKillPss = minPss;
            this.mAvgCachedKillPss = avgPss;
            this.mMaxCachedKillPss = maxPss;
            return;
        }
        if (minPss < this.mMinCachedKillPss) {
            this.mMinCachedKillPss = minPss;
        }
        if (maxPss > this.mMaxCachedKillPss) {
            this.mMaxCachedKillPss = maxPss;
        }
        this.mAvgCachedKillPss = (long) (((((double) this.mAvgCachedKillPss) * ((double) this.mNumCachedKill)) + ((double) avgPss)) / ((double) (this.mNumCachedKill + num)));
        this.mNumCachedKill += num;
    }

    public void reportCachedKill(ArrayMap<String, ProcessStateHolder> pkgList, long pss) {
        ensureNotDead();
        this.mCommonProcess.addCachedKill(1, pss, pss, pss);
        if (this.mCommonProcess.mMultiPackage) {
            for (int ip = pkgList.size() - 1; ip >= 0; ip--) {
                pullFixedProc(pkgList, ip).addCachedKill(1, pss, pss, pss);
            }
        }
    }

    public ProcessState pullFixedProc(String pkgName) {
        if (!this.mMultiPackage) {
            return this;
        }
        LongSparseArray<PackageState> vpkg = (LongSparseArray) this.mStats.mPackages.get(pkgName, this.mUid);
        if (vpkg != null) {
            PackageState pkg = (PackageState) vpkg.get(this.mVersion);
            if (pkg != null) {
                ProcessState proc = (ProcessState) pkg.mProcesses.get(this.mName);
                if (proc != null) {
                    return proc;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Didn't create per-package process ");
                stringBuilder.append(this.mName);
                stringBuilder.append(" in pkg ");
                stringBuilder.append(pkgName);
                stringBuilder.append(" / ");
                stringBuilder.append(this.mUid);
                stringBuilder.append(" vers ");
                stringBuilder.append(this.mVersion);
                throw new IllegalStateException(stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Didn't find package ");
            stringBuilder2.append(pkgName);
            stringBuilder2.append(" / ");
            stringBuilder2.append(this.mUid);
            stringBuilder2.append(" vers ");
            stringBuilder2.append(this.mVersion);
            throw new IllegalStateException(stringBuilder2.toString());
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Didn't find package ");
        stringBuilder3.append(pkgName);
        stringBuilder3.append(" / ");
        stringBuilder3.append(this.mUid);
        throw new IllegalStateException(stringBuilder3.toString());
    }

    private ProcessState pullFixedProc(ArrayMap<String, ProcessStateHolder> pkgList, int index) {
        ProcessStateHolder holder = (ProcessStateHolder) pkgList.valueAt(index);
        ProcessState proc = holder.state;
        if (this.mDead && proc.mCommonProcess != proc) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Pulling dead proc: name=");
            stringBuilder.append(this.mName);
            stringBuilder.append(" pkg=");
            stringBuilder.append(this.mPackage);
            stringBuilder.append(" uid=");
            stringBuilder.append(this.mUid);
            stringBuilder.append(" common.name=");
            stringBuilder.append(this.mCommonProcess.mName);
            Log.wtf("ProcessStats", stringBuilder.toString());
            proc = this.mStats.getProcessStateLocked(proc.mPackage, proc.mUid, proc.mVersion, proc.mName);
        }
        if (proc.mMultiPackage) {
            LongSparseArray<PackageState> vpkg = (LongSparseArray) this.mStats.mPackages.get((String) pkgList.keyAt(index), proc.mUid);
            if (vpkg != null) {
                PackageState pkg = (PackageState) vpkg.get(proc.mVersion);
                if (pkg != null) {
                    String savedName = proc.mName;
                    proc = (ProcessState) pkg.mProcesses.get(proc.mName);
                    if (proc != null) {
                        holder.state = proc;
                    } else {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Didn't create per-package process ");
                        stringBuilder2.append(savedName);
                        stringBuilder2.append(" in pkg ");
                        stringBuilder2.append(pkg.mPackageName);
                        stringBuilder2.append("/");
                        stringBuilder2.append(pkg.mUid);
                        throw new IllegalStateException(stringBuilder2.toString());
                    }
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("No existing package ");
                stringBuilder3.append((String) pkgList.keyAt(index));
                stringBuilder3.append("/");
                stringBuilder3.append(proc.mUid);
                stringBuilder3.append(" for multi-proc ");
                stringBuilder3.append(proc.mName);
                stringBuilder3.append(" version ");
                stringBuilder3.append(proc.mVersion);
                throw new IllegalStateException(stringBuilder3.toString());
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("No existing package ");
            stringBuilder4.append((String) pkgList.keyAt(index));
            stringBuilder4.append("/");
            stringBuilder4.append(proc.mUid);
            stringBuilder4.append(" for multi-proc ");
            stringBuilder4.append(proc.mName);
            throw new IllegalStateException(stringBuilder4.toString());
        }
        return proc;
    }

    public long getDuration(int state, long now) {
        long time = this.mDurations.getValueForId((byte) state);
        if (this.mCurState == state) {
            return time + (now - this.mStartTime);
        }
        return time;
    }

    public long getPssSampleCount(int state) {
        return this.mPssTable.getValueForId((byte) state, 0);
    }

    public long getPssMinimum(int state) {
        return this.mPssTable.getValueForId((byte) state, 1);
    }

    public long getPssAverage(int state) {
        return this.mPssTable.getValueForId((byte) state, 2);
    }

    public long getPssMaximum(int state) {
        return this.mPssTable.getValueForId((byte) state, 3);
    }

    public long getPssUssMinimum(int state) {
        return this.mPssTable.getValueForId((byte) state, 4);
    }

    public long getPssUssAverage(int state) {
        return this.mPssTable.getValueForId((byte) state, 5);
    }

    public long getPssUssMaximum(int state) {
        return this.mPssTable.getValueForId((byte) state, 6);
    }

    public long getPssRssMinimum(int state) {
        return this.mPssTable.getValueForId((byte) state, 7);
    }

    public long getPssRssAverage(int state) {
        return this.mPssTable.getValueForId((byte) state, 8);
    }

    public long getPssRssMaximum(int state) {
        return this.mPssTable.getValueForId((byte) state, 9);
    }

    public void aggregatePss(TotalMemoryUseCollection data, long now) {
        long avg;
        ProcessState processState = this;
        TotalMemoryUseCollection totalMemoryUseCollection = data;
        PssAggr fgPss = new PssAggr();
        PssAggr bgPss = new PssAggr();
        PssAggr cachedPss = new PssAggr();
        boolean havePss = false;
        for (int i = 0; i < processState.mDurations.getKeyCount(); i++) {
            int type = SparseMappingTable.getIdFromKey(processState.mDurations.getKeyAt(i));
            int procState = type % 14;
            boolean havePss2 = havePss;
            long samples = processState.getPssSampleCount(type);
            if (samples > 0) {
                avg = processState.getPssAverage(type);
                havePss2 = true;
                if (procState <= 2) {
                    fgPss.add(avg, samples);
                } else if (procState <= 7) {
                    bgPss.add(avg, samples);
                } else {
                    cachedPss.add(avg, samples);
                }
            }
            havePss = havePss2;
        }
        if (havePss) {
            boolean fgHasBg = false;
            boolean fgHasCached = false;
            havePss = false;
            if (fgPss.samples < 3 && bgPss.samples > 0) {
                fgHasBg = true;
                fgPss.add(bgPss.pss, bgPss.samples);
            }
            if (fgPss.samples < 3 && cachedPss.samples > 0) {
                fgHasCached = true;
                fgPss.add(cachedPss.pss, cachedPss.samples);
            }
            if (bgPss.samples < 3 && cachedPss.samples > 0) {
                havePss = true;
                bgPss.add(cachedPss.pss, cachedPss.samples);
            }
            if (bgPss.samples < 3 && !fgHasBg && fgPss.samples > 0) {
                bgPss.add(fgPss.pss, fgPss.samples);
            }
            if (cachedPss.samples < 3 && !bgHasCached && bgPss.samples > 0) {
                cachedPss.add(bgPss.pss, bgPss.samples);
            }
            if (cachedPss.samples < 3 && !fgHasCached && fgPss.samples > 0) {
                cachedPss.add(fgPss.pss, fgPss.samples);
            }
            int i2 = 0;
            while (true) {
                int i3 = i2;
                PssAggr bgPss2;
                PssAggr cachedPss2;
                boolean fgHasBg2;
                boolean fgHasCached2;
                boolean bgHasCached;
                if (i3 < processState.mDurations.getKeyCount()) {
                    long time;
                    long samples2;
                    long avg2;
                    int key = processState.mDurations.getKeyAt(i3);
                    int type2 = SparseMappingTable.getIdFromKey(key);
                    long time2 = processState.mDurations.getValue(key);
                    if (processState.mCurState == type2) {
                        time2 += now - processState.mStartTime;
                    }
                    int procState2 = type2 % 14;
                    long[] jArr = totalMemoryUseCollection.processStateTime;
                    jArr[procState2] = jArr[procState2] + time2;
                    long samples3 = processState.getPssSampleCount(type2);
                    if (samples3 > 0) {
                        time = time2;
                        samples2 = samples3;
                        avg2 = processState.getPssAverage(type2);
                    } else if (procState2 <= 2) {
                        time = time2;
                        samples2 = fgPss.samples;
                        avg2 = fgPss.pss;
                    } else {
                        time = time2;
                        if (procState2 <= 7) {
                            long samples4 = bgPss.samples;
                            avg2 = bgPss.pss;
                            samples2 = samples4;
                        } else {
                            samples2 = cachedPss.samples;
                            avg2 = cachedPss.pss;
                        }
                    }
                    PssAggr fgPss2 = fgPss;
                    bgPss2 = bgPss;
                    cachedPss2 = cachedPss;
                    fgHasBg2 = fgHasBg;
                    fgHasCached2 = fgHasCached;
                    bgHasCached = havePss;
                    fgPss = ((((double) totalMemoryUseCollection.processStatePss[procState2]) * ((double) totalMemoryUseCollection.processStateSamples[procState2])) + (((double) avg2) * ((double) samples2))) / ((double) (((long) totalMemoryUseCollection.processStateSamples[procState2]) + samples2));
                    totalMemoryUseCollection.processStatePss[procState2] = (long) fgPss;
                    int[] iArr = totalMemoryUseCollection.processStateSamples;
                    iArr[procState2] = (int) (((long) iArr[procState2]) + samples2);
                    double[] dArr = totalMemoryUseCollection.processStateWeight;
                    double newAvg = fgPss;
                    dArr[procState2] = dArr[procState2] + (((double) avg2) * ((double) time));
                    i2 = i3 + 1;
                    avg = 0;
                    fgPss = fgPss2;
                    bgPss = bgPss2;
                    cachedPss = cachedPss2;
                    fgHasBg = fgHasBg2;
                    fgHasCached = fgHasCached2;
                    havePss = bgHasCached;
                    processState = this;
                    totalMemoryUseCollection = data;
                } else {
                    bgPss2 = bgPss;
                    cachedPss2 = cachedPss;
                    fgHasBg2 = fgHasBg;
                    fgHasCached2 = fgHasCached;
                    bgHasCached = havePss;
                    return;
                }
            }
        }
    }

    public long computeProcessTimeLocked(int[] screenStates, int[] memStates, int[] procStates, long now) {
        long totalTime = 0;
        for (int i : screenStates) {
            int im = 0;
            while (im < memStates.length) {
                long totalTime2 = totalTime;
                for (int i2 : procStates) {
                    totalTime2 += getDuration(((i + memStates[im]) * 14) + i2, now);
                }
                im++;
                totalTime = totalTime2;
            }
        }
        this.mTmpTotalTime = totalTime;
        return totalTime;
    }

    public void dumpSummary(PrintWriter pw, String prefix, int[] screenStates, int[] memStates, int[] procStates, long now, long totalTime) {
        PrintWriter printWriter = pw;
        pw.print(prefix);
        printWriter.print("* ");
        printWriter.print(this.mName);
        printWriter.print(" / ");
        UserHandle.formatUid(printWriter, this.mUid);
        printWriter.print(" / v");
        printWriter.print(this.mVersion);
        printWriter.println(":");
        PrintWriter printWriter2 = printWriter;
        String str = prefix;
        int[] iArr = screenStates;
        int[] iArr2 = memStates;
        long j = now;
        long j2 = totalTime;
        dumpProcessSummaryDetails(printWriter2, str, "         TOTAL: ", iArr, iArr2, procStates, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "    Persistent: ", iArr, iArr2, new int[]{0}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "           Top: ", iArr, iArr2, new int[]{1}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "        Imp Fg: ", iArr, iArr2, new int[]{2}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "        Imp Bg: ", iArr, iArr2, new int[]{3}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "        Backup: ", iArr, iArr2, new int[]{4}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "     Heavy Wgt: ", iArr, iArr2, new int[]{8}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "       Service: ", iArr, iArr2, new int[]{5}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "    Service Rs: ", iArr, iArr2, new int[]{6}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "      Receiver: ", iArr, iArr2, new int[]{7}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "         Heavy: ", iArr, iArr2, new int[]{9}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "        (Home): ", iArr, iArr2, new int[]{9}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "    (Last Act): ", iArr, iArr2, new int[]{10}, j, j2, true);
        dumpProcessSummaryDetails(printWriter2, str, "      (Cached): ", iArr, iArr2, new int[]{11, 12, 13}, j, j2, true);
    }

    public void dumpProcessState(PrintWriter pw, String prefix, int[] screenStates, int[] memStates, int[] procStates, long now) {
        ProcessState processState = this;
        PrintWriter printWriter = pw;
        int[] iArr = screenStates;
        int[] iArr2 = memStates;
        int[] iArr3 = procStates;
        int printedScreen = -1;
        long totalTime = 0;
        int is = 0;
        while (is < iArr.length) {
            int printedMem = -1;
            long totalTime2 = totalTime;
            int im = 0;
            while (im < iArr2.length) {
                int ip = 0;
                while (ip < iArr3.length) {
                    String running;
                    int iscreen = iArr[is];
                    int imem = iArr2[im];
                    int bucket = ((iscreen + imem) * 14) + iArr3[ip];
                    long time = processState.mDurations.getValueForId((byte) bucket);
                    String running2 = "";
                    if (processState.mCurState == bucket) {
                        running = " (running)";
                    } else {
                        running = running2;
                    }
                    if (time != 0) {
                        pw.print(prefix);
                        if (iArr.length > 1) {
                            DumpUtils.printScreenLabel(printWriter, printedScreen != iscreen ? iscreen : -1);
                            printedScreen = iscreen;
                        }
                        if (iArr2.length > 1) {
                            DumpUtils.printMemLabel(printWriter, printedMem != imem ? imem : -1, '/');
                            printedMem = imem;
                        }
                        printWriter.print(DumpUtils.STATE_NAMES[iArr3[ip]]);
                        printWriter.print(": ");
                        TimeUtils.formatDuration(time, printWriter);
                        printWriter.println(running);
                        totalTime2 += time;
                    }
                    ip++;
                    processState = this;
                }
                im++;
                processState = this;
            }
            is++;
            totalTime = totalTime2;
            processState = this;
        }
        if (totalTime != 0) {
            int i;
            pw.print(prefix);
            if (iArr.length > 1) {
                i = -1;
                DumpUtils.printScreenLabel(printWriter, -1);
            } else {
                i = -1;
            }
            if (iArr2.length > 1) {
                DumpUtils.printMemLabel(printWriter, i, '/');
            }
            printWriter.print("TOTAL  : ");
            TimeUtils.formatDuration(totalTime, printWriter);
            pw.println();
        }
    }

    public void dumpPss(PrintWriter pw, String prefix, int[] screenStates, int[] memStates, int[] procStates) {
        PrintWriter printWriter = pw;
        int[] iArr = screenStates;
        int[] iArr2 = memStates;
        int[] iArr3 = procStates;
        int printedScreen = -1;
        boolean printedHeader = false;
        int is = 0;
        while (is < iArr.length) {
            int printedMem = -1;
            int printedScreen2 = printedScreen;
            boolean printedHeader2 = printedHeader;
            int im = 0;
            while (im < iArr2.length) {
                boolean z;
                int printedMem2 = printedMem;
                boolean printedHeader3 = printedHeader2;
                printedScreen = 0;
                while (printedScreen < iArr3.length) {
                    int iscreen = iArr[is];
                    int imem = iArr2[im];
                    int bucket = ((iscreen + imem) * 14) + iArr3[printedScreen];
                    long count = getPssSampleCount(bucket);
                    if (count > 0) {
                        if (printedHeader3) {
                            z = printedHeader3;
                        } else {
                            pw.print(prefix);
                            printWriter.print("PSS/USS (");
                            printWriter.print(this.mPssTable.getKeyCount());
                            printWriter.println(" entries):");
                            printedHeader3 = true;
                        }
                        pw.print(prefix);
                        boolean printedHeader4 = printedHeader3;
                        printWriter.print("  ");
                        if (iArr.length > 1) {
                            DumpUtils.printScreenLabel(printWriter, printedScreen2 != iscreen ? iscreen : -1);
                            printedScreen2 = iscreen;
                        }
                        if (iArr2.length > 1) {
                            DumpUtils.printMemLabel(printWriter, printedMem2 != imem ? imem : -1, '/');
                            printedMem2 = imem;
                        }
                        printWriter.print(DumpUtils.STATE_NAMES[iArr3[printedScreen]]);
                        printWriter.print(": ");
                        printWriter.print(count);
                        printWriter.print(" samples ");
                        DebugUtils.printSizeValue(printWriter, getPssMinimum(bucket) * 1024);
                        printWriter.print(" ");
                        DebugUtils.printSizeValue(printWriter, getPssAverage(bucket) * 1024);
                        printWriter.print(" ");
                        DebugUtils.printSizeValue(printWriter, getPssMaximum(bucket) * 1024);
                        printWriter.print(" / ");
                        DebugUtils.printSizeValue(printWriter, getPssUssMinimum(bucket) * 1024);
                        printWriter.print(" ");
                        DebugUtils.printSizeValue(printWriter, getPssUssAverage(bucket) * 1024);
                        printWriter.print(" ");
                        DebugUtils.printSizeValue(printWriter, getPssUssMaximum(bucket) * 1024);
                        printWriter.print(" / ");
                        DebugUtils.printSizeValue(printWriter, getPssRssMinimum(bucket) * 1024);
                        printWriter.print(" ");
                        DebugUtils.printSizeValue(printWriter, getPssRssAverage(bucket) * 1024);
                        printWriter.print(" ");
                        DebugUtils.printSizeValue(printWriter, getPssRssMaximum(bucket) * 1024);
                        pw.println();
                        printedHeader3 = printedHeader4;
                    } else {
                        z = printedHeader3;
                    }
                    printedScreen++;
                    iArr = screenStates;
                    iArr2 = memStates;
                }
                z = printedHeader3;
                im++;
                printedMem = printedMem2;
                printedHeader2 = z;
                iArr = screenStates;
                iArr2 = memStates;
            }
            is++;
            printedHeader = printedHeader2;
            printedScreen = printedScreen2;
            iArr = screenStates;
            iArr2 = memStates;
        }
        if (this.mNumExcessiveCpu != 0) {
            pw.print(prefix);
            printWriter.print("Killed for excessive CPU use: ");
            printWriter.print(this.mNumExcessiveCpu);
            printWriter.println(" times");
        }
        if (this.mNumCachedKill != 0) {
            pw.print(prefix);
            printWriter.print("Killed from cached state: ");
            printWriter.print(this.mNumCachedKill);
            printWriter.print(" times from pss ");
            DebugUtils.printSizeValue(printWriter, this.mMinCachedKillPss * 1024);
            printWriter.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            DebugUtils.printSizeValue(printWriter, this.mAvgCachedKillPss * 1024);
            printWriter.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            DebugUtils.printSizeValue(printWriter, this.mMaxCachedKillPss * 1024);
            pw.println();
        }
    }

    private void dumpProcessSummaryDetails(PrintWriter pw, String prefix, String label, int[] screenStates, int[] memStates, int[] procStates, long now, long totalTime, boolean full) {
        PrintWriter printWriter = pw;
        String str = label;
        long j = totalTime;
        ProcessDataCollection totals = new ProcessDataCollection(screenStates, memStates, procStates);
        computeProcessData(totals, now);
        if ((((double) totals.totalTime) / ((double) j)) * 100.0d >= 0.005d || totals.numPss != 0) {
            if (prefix != null) {
                pw.print(prefix);
            }
            if (str != null) {
                printWriter.print(str);
            }
            totals.print(printWriter, j, full);
            if (prefix != null) {
                pw.println();
                return;
            }
            return;
        }
        boolean z = full;
    }

    public void dumpInternalLocked(PrintWriter pw, String prefix, boolean dumpAll) {
        if (dumpAll) {
            pw.print(prefix);
            pw.print("myID=");
            pw.print(Integer.toHexString(System.identityHashCode(this)));
            pw.print(" mCommonProcess=");
            pw.print(Integer.toHexString(System.identityHashCode(this.mCommonProcess)));
            pw.print(" mPackage=");
            pw.println(this.mPackage);
            if (this.mMultiPackage) {
                pw.print(prefix);
                pw.print("mMultiPackage=");
                pw.println(this.mMultiPackage);
            }
            if (this != this.mCommonProcess) {
                pw.print(prefix);
                pw.print("Common Proc: ");
                pw.print(this.mCommonProcess.mName);
                pw.print("/");
                pw.print(this.mCommonProcess.mUid);
                pw.print(" pkg=");
                pw.println(this.mCommonProcess.mPackage);
            }
        }
        if (this.mActive) {
            pw.print(prefix);
            pw.print("mActive=");
            pw.println(this.mActive);
        }
        if (this.mDead) {
            pw.print(prefix);
            pw.print("mDead=");
            pw.println(this.mDead);
        }
        if (this.mNumActiveServices != 0 || this.mNumStartedServices != 0) {
            pw.print(prefix);
            pw.print("mNumActiveServices=");
            pw.print(this.mNumActiveServices);
            pw.print(" mNumStartedServices=");
            pw.println(this.mNumStartedServices);
        }
    }

    public void computeProcessData(ProcessDataCollection data, long now) {
        ProcessDataCollection processDataCollection = data;
        long j = 0;
        processDataCollection.totalTime = 0;
        processDataCollection.maxRss = 0;
        processDataCollection.avgRss = 0;
        processDataCollection.minRss = 0;
        processDataCollection.maxUss = 0;
        processDataCollection.avgUss = 0;
        processDataCollection.minUss = 0;
        processDataCollection.maxPss = 0;
        processDataCollection.avgPss = 0;
        processDataCollection.minPss = 0;
        processDataCollection.numPss = 0;
        int is = 0;
        while (is < processDataCollection.screenStates.length) {
            long j2;
            int im = 0;
            while (im < processDataCollection.memStates.length) {
                int is2;
                int ip = 0;
                while (ip < processDataCollection.procStates.length) {
                    int im2;
                    int ip2;
                    int bucket = ((processDataCollection.screenStates[is] + processDataCollection.memStates[im]) * 14) + processDataCollection.procStates[ip];
                    processDataCollection.totalTime += getDuration(bucket, now);
                    long samples = getPssSampleCount(bucket);
                    if (samples > j) {
                        long minPss = getPssMinimum(bucket);
                        is2 = is;
                        long avgPss = getPssAverage(bucket);
                        j = getPssMaximum(bucket);
                        long minUss = getPssUssMinimum(bucket);
                        im2 = im;
                        ip2 = ip;
                        long avgUss = getPssUssAverage(bucket);
                        long samples2 = samples;
                        long maxUss = getPssUssMaximum(bucket);
                        long minRss = getPssRssMinimum(bucket);
                        long avgRss = getPssRssAverage(bucket);
                        long maxRss = getPssRssMaximum(bucket);
                        j2 = 0;
                        long maxUss2;
                        long minRss2;
                        long avgRss2;
                        if (processDataCollection.numPss == 0) {
                            processDataCollection.minPss = minPss;
                            processDataCollection.avgPss = avgPss;
                            processDataCollection.maxPss = j;
                            processDataCollection.minUss = minUss;
                            processDataCollection.avgUss = avgUss;
                            long maxUss3 = maxUss;
                            processDataCollection.maxUss = maxUss3;
                            maxUss2 = maxUss3;
                            maxUss3 = minRss;
                            processDataCollection.minRss = maxUss3;
                            minRss2 = maxUss3;
                            maxUss3 = avgRss;
                            processDataCollection.avgRss = maxUss3;
                            avgRss2 = maxUss3;
                            maxUss3 = maxRss;
                            processDataCollection.maxRss = maxUss3;
                            long j3 = j;
                            long j4 = avgPss;
                            j = maxUss3;
                            long j5 = minPss;
                            avgPss = samples2;
                            long j6 = maxUss2;
                            long j7 = minRss2;
                            long j8 = avgRss2;
                            maxUss3 = avgUss;
                        } else {
                            maxUss2 = maxUss;
                            minRss2 = minRss;
                            avgRss2 = avgRss;
                            long maxRss2 = maxRss;
                            if (minPss < processDataCollection.minPss) {
                                processDataCollection.minPss = minPss;
                            }
                            double d = (double) avgPss;
                            long avgUss2 = avgUss;
                            avgPss = samples2;
                            processDataCollection.avgPss = (long) (((((double) processDataCollection.avgPss) * ((double) processDataCollection.numPss)) + (d * ((double) avgPss))) / ((double) (processDataCollection.numPss + avgPss)));
                            if (j > processDataCollection.maxPss) {
                                processDataCollection.maxPss = j;
                            }
                            if (minUss < processDataCollection.minUss) {
                                processDataCollection.minUss = minUss;
                            }
                            processDataCollection.avgUss = (long) (((((double) processDataCollection.avgUss) * ((double) processDataCollection.numPss)) + (((double) avgUss2) * ((double) avgPss))) / ((double) (processDataCollection.numPss + avgPss)));
                            if (maxUss2 > processDataCollection.maxUss) {
                                j = maxUss2;
                                processDataCollection.maxUss = j;
                            } else {
                                j = maxUss2;
                            }
                            if (minRss2 < processDataCollection.minRss) {
                                avgUss = minRss2;
                                processDataCollection.minRss = avgUss;
                            } else {
                                avgUss = minRss2;
                            }
                            j = avgRss2;
                            processDataCollection.avgRss = (long) (((((double) processDataCollection.avgRss) * ((double) processDataCollection.numPss)) + (((double) j) * ((double) avgPss))) / ((double) (processDataCollection.numPss + avgPss)));
                            if (maxRss2 > processDataCollection.maxRss) {
                                processDataCollection.maxRss = maxRss2;
                            }
                        }
                        processDataCollection.numPss += avgPss;
                    } else {
                        j2 = j;
                        is2 = is;
                        im2 = im;
                        ip2 = ip;
                    }
                    ip = ip2 + 1;
                    is = is2;
                    j = j2;
                    im = im2;
                }
                j2 = j;
                is2 = is;
                im++;
            }
            j2 = j;
            is++;
        }
    }

    public void dumpCsv(PrintWriter pw, boolean sepScreenStates, int[] screenStates, boolean sepMemStates, int[] memStates, boolean sepProcStates, int[] procStates, long now) {
        int NSS;
        int NMS;
        int NPS;
        long j;
        PrintWriter printWriter = pw;
        int[] iArr = screenStates;
        int[] iArr2 = memStates;
        int[] iArr3 = procStates;
        int NSS2 = sepScreenStates ? iArr.length : 1;
        int NMS2 = sepMemStates ? iArr2.length : 1;
        int NPS2 = sepProcStates ? iArr3.length : 1;
        int iss = 0;
        while (iss < NSS2) {
            int ims = 0;
            while (ims < NMS2) {
                int iss2;
                int ips = 0;
                while (ips < NPS2) {
                    int vsscreen = sepScreenStates ? iArr[iss] : 0;
                    int vsmem = sepMemStates ? iArr2[ims] : 0;
                    int vsproc = sepProcStates ? iArr3[ips] : 0;
                    int NSA = sepScreenStates ? 1 : iArr.length;
                    int NMA = sepMemStates ? 1 : iArr2.length;
                    if (sepProcStates) {
                        NSS = NSS2;
                        NSS2 = 1;
                    } else {
                        NSS = NSS2;
                        NSS2 = iArr3.length;
                    }
                    NMS = NMS2;
                    NPS = NPS2;
                    long totalTime = 0;
                    int isa = 0;
                    while (true) {
                        iss2 = iss;
                        iss = isa;
                        if (iss >= NSA) {
                            break;
                        }
                        long totalTime2 = totalTime;
                        NMS2 = 0;
                        while (NMS2 < NMA) {
                            NPS2 = 0;
                            while (NPS2 < NSS2) {
                                int vascreen = sepScreenStates ? 0 : iArr[iss];
                                totalTime2 += getDuration((((((vsscreen + vascreen) + vsmem) + (sepMemStates ? 0 : iArr2[NMS2])) * 14) + vsproc) + (sepProcStates ? 0 : iArr3[NPS2]), now);
                                NPS2++;
                                iArr = screenStates;
                                iArr2 = memStates;
                            }
                            j = now;
                            NMS2++;
                            iArr = screenStates;
                            iArr2 = memStates;
                        }
                        j = now;
                        int isa2 = iss + 1;
                        totalTime = totalTime2;
                        iss = iss2;
                        iArr = screenStates;
                        iArr2 = memStates;
                        isa = isa2;
                    }
                    j = now;
                    printWriter.print("\t");
                    printWriter.print(totalTime);
                    ips++;
                    NSS2 = NSS;
                    NMS2 = NMS;
                    NPS2 = NPS;
                    iss = iss2;
                    iArr = screenStates;
                    iArr2 = memStates;
                }
                j = now;
                NSS = NSS2;
                NMS = NMS2;
                NPS = NPS2;
                iss2 = iss;
                ims++;
                iArr = screenStates;
                iArr2 = memStates;
            }
            j = now;
            NSS = NSS2;
            NMS = NMS2;
            NPS = NPS2;
            iss++;
            iArr = screenStates;
            iArr2 = memStates;
        }
        j = now;
        NSS = NSS2;
        NMS = NMS2;
        NPS = NPS2;
    }

    public void dumpPackageProcCheckin(PrintWriter pw, String pkgName, int uid, long vers, String itemName, long now) {
        pw.print("pkgproc,");
        pw.print(pkgName);
        pw.print(",");
        pw.print(uid);
        pw.print(",");
        pw.print(vers);
        pw.print(",");
        pw.print(DumpUtils.collapseString(pkgName, itemName));
        dumpAllStateCheckin(pw, now);
        pw.println();
        if (this.mPssTable.getKeyCount() > 0) {
            pw.print("pkgpss,");
            pw.print(pkgName);
            pw.print(",");
            pw.print(uid);
            pw.print(",");
            pw.print(vers);
            pw.print(",");
            pw.print(DumpUtils.collapseString(pkgName, itemName));
            dumpAllPssCheckin(pw);
            pw.println();
        }
        if (this.mNumExcessiveCpu > 0 || this.mNumCachedKill > 0) {
            pw.print("pkgkills,");
            pw.print(pkgName);
            pw.print(",");
            pw.print(uid);
            pw.print(",");
            pw.print(vers);
            pw.print(",");
            pw.print(DumpUtils.collapseString(pkgName, itemName));
            pw.print(",");
            pw.print("0");
            pw.print(",");
            pw.print(this.mNumExcessiveCpu);
            pw.print(",");
            pw.print(this.mNumCachedKill);
            pw.print(",");
            pw.print(this.mMinCachedKillPss);
            pw.print(":");
            pw.print(this.mAvgCachedKillPss);
            pw.print(":");
            pw.print(this.mMaxCachedKillPss);
            pw.println();
        }
    }

    public void dumpProcCheckin(PrintWriter pw, String procName, int uid, long now) {
        if (this.mDurations.getKeyCount() > 0) {
            pw.print("proc,");
            pw.print(procName);
            pw.print(",");
            pw.print(uid);
            dumpAllStateCheckin(pw, now);
            pw.println();
        }
        if (this.mPssTable.getKeyCount() > 0) {
            pw.print("pss,");
            pw.print(procName);
            pw.print(",");
            pw.print(uid);
            dumpAllPssCheckin(pw);
            pw.println();
        }
        if (this.mNumExcessiveCpu > 0 || this.mNumCachedKill > 0) {
            pw.print("kills,");
            pw.print(procName);
            pw.print(",");
            pw.print(uid);
            pw.print(",");
            pw.print("0");
            pw.print(",");
            pw.print(this.mNumExcessiveCpu);
            pw.print(",");
            pw.print(this.mNumCachedKill);
            pw.print(",");
            pw.print(this.mMinCachedKillPss);
            pw.print(":");
            pw.print(this.mAvgCachedKillPss);
            pw.print(":");
            pw.print(this.mMaxCachedKillPss);
            pw.println();
        }
    }

    public void dumpAllStateCheckin(PrintWriter pw, long now) {
        boolean didCurState = false;
        for (int i = 0; i < this.mDurations.getKeyCount(); i++) {
            int key = this.mDurations.getKeyAt(i);
            int type = SparseMappingTable.getIdFromKey(key);
            long time = this.mDurations.getValue(key);
            if (this.mCurState == type) {
                didCurState = true;
                time += now - this.mStartTime;
            }
            DumpUtils.printProcStateTagAndValue(pw, type, time);
        }
        if (!didCurState && this.mCurState != -1) {
            DumpUtils.printProcStateTagAndValue(pw, this.mCurState, now - this.mStartTime);
        }
    }

    public void dumpAllPssCheckin(PrintWriter pw) {
        int N = this.mPssTable.getKeyCount();
        for (int i = 0; i < N; i++) {
            int key = this.mPssTable.getKeyAt(i);
            int type = SparseMappingTable.getIdFromKey(key);
            pw.print(',');
            DumpUtils.printProcStateTag(pw, type);
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 0));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 1));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 2));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 3));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 4));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 5));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 6));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 7));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 8));
            pw.print(':');
            pw.print(this.mPssTable.getValue(key, 9));
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("ProcessState{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" ");
        sb.append(this.mName);
        sb.append("/");
        sb.append(this.mUid);
        sb.append(" pkg=");
        sb.append(this.mPackage);
        if (this.mMultiPackage) {
            sb.append(" (multi)");
        }
        if (this.mCommonProcess != this) {
            sb.append(" (sub)");
        }
        sb.append("}");
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, String procName, int uid, long now) {
        long killToken;
        int i;
        int key;
        int type;
        long time;
        ProtoOutputStream protoOutputStream = proto;
        long token = proto.start(fieldId);
        protoOutputStream.write(1138166333441L, procName);
        protoOutputStream.write(1120986464258L, uid);
        if (this.mNumExcessiveCpu > 0 || this.mNumCachedKill > 0) {
            killToken = protoOutputStream.start(1146756268035L);
            protoOutputStream.write(1120986464257L, this.mNumExcessiveCpu);
            protoOutputStream.write(1120986464258L, this.mNumCachedKill);
            ProtoOutputStream protoOutputStream2 = protoOutputStream;
            long killToken2 = killToken;
            ProtoUtils.toAggStatsProto(protoOutputStream2, 1146756268035L, this.mMinCachedKillPss, this.mAvgCachedKillPss, this.mMaxCachedKillPss);
            protoOutputStream.end(killToken2);
        }
        Map<Integer, Long> durationByState = new HashMap();
        boolean didCurState = false;
        for (i = 0; i < this.mDurations.getKeyCount(); i++) {
            Map<Integer, Long> durationByState2;
            key = this.mDurations.getKeyAt(i);
            type = SparseMappingTable.getIdFromKey(key);
            time = this.mDurations.getValue(key);
            if (this.mCurState == type) {
                durationByState2 = durationByState;
                time += now - this.mStartTime;
                didCurState = true;
            } else {
                durationByState2 = durationByState;
            }
            durationByState = durationByState2;
            durationByState.put(Integer.valueOf(type), Long.valueOf(time));
        }
        if (!(didCurState || this.mCurState == -1)) {
            durationByState.put(Integer.valueOf(this.mCurState), Long.valueOf(now - this.mStartTime));
        }
        i = 0;
        while (true) {
            int i2 = i;
            time = 2246267895813L;
            if (i2 >= this.mPssTable.getKeyCount()) {
                break;
            }
            int i3;
            Map<Integer, Long> durationByState3;
            key = this.mPssTable.getKeyAt(i2);
            type = SparseMappingTable.getIdFromKey(key);
            if (durationByState.containsKey(Integer.valueOf(type))) {
                int key2 = key;
                int type2 = type;
                long stateToken = protoOutputStream.start(2246267895813L);
                i3 = i2;
                DumpUtils.printProcStateTagProto(protoOutputStream, 1159641169921L, 1159641169922L, 1159641169923L, type2);
                int type3 = type2;
                killToken = ((Long) durationByState.get(Integer.valueOf(type3))).longValue();
                durationByState.remove(Integer.valueOf(type3));
                protoOutputStream.write(1112396529668L, killToken);
                i2 = key2;
                protoOutputStream.write(1120986464261L, this.mPssTable.getValue(i2, 0));
                durationByState3 = durationByState;
                type3 = i2;
                ProtoUtils.toAggStatsProto(protoOutputStream, 1146756268038L, this.mPssTable.getValue(i2, 1), this.mPssTable.getValue(i2, 2), this.mPssTable.getValue(i2, 3));
                ProtoUtils.toAggStatsProto(protoOutputStream, 1146756268039L, this.mPssTable.getValue(type3, 4), this.mPssTable.getValue(type3, 5), this.mPssTable.getValue(type3, 6));
                ProtoUtils.toAggStatsProto(protoOutputStream, 1146756268040L, this.mPssTable.getValue(type3, 7), this.mPssTable.getValue(type3, 8), this.mPssTable.getValue(type3, 9));
                protoOutputStream.end(stateToken);
            } else {
                i3 = i2;
                durationByState3 = durationByState;
            }
            i = i3 + 1;
            durationByState = durationByState3;
        }
        long j = 1112396529668L;
        for (Entry<Integer, Long> entry : durationByState.entrySet()) {
            killToken = protoOutputStream.start(time);
            long j2 = time;
            long stateToken2 = killToken;
            DumpUtils.printProcStateTagProto(protoOutputStream, 1159641169921L, 1159641169922L, 1159641169923L, ((Integer) entry.getKey()).intValue());
            protoOutputStream.write(1112396529668L, ((Long) entry.getValue()).longValue());
            protoOutputStream.end(stateToken2);
            j = 1112396529668L;
            time = j2;
        }
        protoOutputStream.end(token);
    }
}
