package com.android.server.rms.memrepair;

import android.rms.iaware.AwareLog;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ProcStateData {
    private static final String TAG = "AwareMem_PSData";
    private int mCustomProcState;
    private AtomicLong mLastPss = new AtomicLong(0);
    private long mLastPssTime;
    private AtomicLong mMaxPss = new AtomicLong(0);
    private int mMergeCount = 0;
    private AtomicLong mMinPss = new AtomicLong(0);
    private int mNextMergeCount = 0;
    private final Object mObjectLock = new Object();
    private int mPid;
    private String mProcName;
    private List<Long> mStatePssList = new ArrayList();

    public ProcStateData(int pid, String procName, int customProcState) {
        this.mPid = pid;
        this.mProcName = procName;
        this.mCustomProcState = customProcState;
    }

    public void addPssToList(long pss, long now, long intervalTime, int sampleCount) {
        long j = pss;
        long j2 = now;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pss=");
        stringBuilder.append(j);
        stringBuilder.append(";now=");
        stringBuilder.append(j2);
        stringBuilder.append(";intervalTime=");
        stringBuilder.append(((long) (1 << this.mMergeCount)) * intervalTime);
        AwareLog.d(str, stringBuilder.toString());
        this.mLastPss.set(j);
        synchronized (this.mObjectLock) {
            int lastIndex = this.mStatePssList.size() - 1;
            if (lastIndex < 0) {
                this.mMinPss.set(j);
                this.mMaxPss.set(j);
            } else {
                this.mMinPss.set(this.mMinPss.get() < j ? this.mMinPss.get() : j);
                this.mMaxPss.set(this.mMaxPss.get() > j ? this.mMaxPss.get() : j);
            }
            if (lastIndex <= -1 || j2 - this.mLastPssTime > ((long) (1 << this.mMergeCount)) * intervalTime) {
                this.mStatePssList.add(Long.valueOf(pss));
                this.mLastPssTime = j2;
                this.mNextMergeCount = 1;
            } else {
                this.mStatePssList.set(lastIndex, Long.valueOf(((((Long) this.mStatePssList.get(lastIndex)).longValue() * ((long) this.mNextMergeCount)) + j) / ((long) (this.mNextMergeCount + 1))));
                this.mNextMergeCount++;
            }
            if (this.mStatePssList.size() == 2 * sampleCount) {
                int i = 0;
                while (i < this.mStatePssList.size()) {
                    int lastIndex2 = lastIndex;
                    this.mStatePssList.set(i / 2, Long.valueOf((((Long) this.mStatePssList.get(i)).longValue() + ((Long) this.mStatePssList.get(i + 1)).longValue()) / 2));
                    i += 2;
                    lastIndex = lastIndex2;
                }
                for (lastIndex = this.mStatePssList.size() - 1; lastIndex > sampleCount - 1; lastIndex--) {
                    this.mStatePssList.remove(lastIndex);
                }
                this.mMergeCount++;
            }
        }
    }

    public int getPid() {
        return this.mPid;
    }

    public String getProcName() {
        return this.mProcName;
    }

    public int getState() {
        return this.mCustomProcState;
    }

    public long getMinPss() {
        return this.mMinPss.get();
    }

    public long getMaxPss() {
        return this.mMaxPss.get();
    }

    public long getLastPss() {
        return this.mLastPss.get();
    }

    public int getMergeCount() {
        return this.mMergeCount;
    }

    public List<Long> getStatePssList() {
        ArrayList<Long> cloneList = new ArrayList();
        synchronized (this.mObjectLock) {
            if (this.mStatePssList.size() > 0) {
                cloneList.addAll(this.mStatePssList);
                return cloneList;
            }
            AwareLog.d(TAG, "mStatePssList size is zero");
            return null;
        }
    }
}
