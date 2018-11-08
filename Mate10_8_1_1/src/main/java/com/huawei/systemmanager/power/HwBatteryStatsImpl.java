package com.huawei.systemmanager.power;

import android.content.Context;
import android.os.Bundle;
import com.android.internal.os.BatteryStatsHelper;

class HwBatteryStatsImpl implements IBatteryStats {
    private static final long STEP_LEVEL_TIME_MASK = 1099511627775L;
    private static HwBatteryStatsImpl sInstance;
    private BatteryStatsHelper mBatteryStatsHelper;
    private IHwPowerProfile mLocalPowerProfile;

    public HwBatteryStatsImpl(Context context, boolean collectBatteryBroadcast) {
        this.mBatteryStatsHelper = new BatteryStatsHelper(context, collectBatteryBroadcast);
    }

    public static synchronized IBatteryStats get(Context context, boolean collectBatteryBroadcast) {
        IBatteryStats iBatteryStats;
        synchronized (HwBatteryStatsImpl.class) {
            if (sInstance == null) {
                sInstance = new HwBatteryStatsImpl(context, collectBatteryBroadcast);
                sInstance.create();
            }
            iBatteryStats = sInstance;
        }
        return iBatteryStats;
    }

    public synchronized IHwPowerProfile getIHwPowerProfile() {
        if (this.mLocalPowerProfile == null) {
            this.mLocalPowerProfile = new HwPowerProfileImpl(this.mBatteryStatsHelper.getPowerProfile());
        }
        return this.mLocalPowerProfile;
    }

    public BatteryStatsHelper getInnerBatteryStatsHelper() {
        return this.mBatteryStatsHelper;
    }

    public long getTimeOfItem(long rawRealTime, int index) {
        switch (index) {
            case 0:
                return this.mBatteryStatsHelper.getStats().getWifiOnTime(rawRealTime, 0);
            case 1:
                return this.mBatteryStatsHelper.getStats().computeBatteryRealtime(rawRealTime, 0) - this.mBatteryStatsHelper.getStats().getScreenOnTime(rawRealTime, 0);
            case 2:
                return getRadioTime(rawRealTime);
            case 3:
                return this.mBatteryStatsHelper.getStats().getPhoneOnTime(rawRealTime, 0);
            case 4:
                return this.mBatteryStatsHelper.getStats().getScreenOnTime(rawRealTime, 0);
            case 5:
                return this.mBatteryStatsHelper.getStats().getPhoneSignalScanningTime(rawRealTime, 0);
            case 6:
                return this.mBatteryStatsHelper.getStats().computeBatteryRealtime(rawRealTime, 2);
            default:
                return 0;
        }
    }

    private long getRadioTime(long rawRealTime) {
        long signalTimeMs = 0;
        for (int i = 0; i < 5; i++) {
            signalTimeMs += this.mBatteryStatsHelper.getStats().getPhoneSignalStrengthTime(i, rawRealTime, 0);
        }
        return signalTimeMs;
    }

    public void create() {
        this.mBatteryStatsHelper.create((Bundle) null);
    }

    public void init() {
        this.mBatteryStatsHelper.clearStats();
        this.mBatteryStatsHelper.getStats();
    }

    public long computeTimePerLevel() {
        int numSteps = this.mBatteryStatsHelper.getStats().getDischargeLevelStepTracker().mNumStepDurations;
        long[] steps = this.mBatteryStatsHelper.getStats().getDischargeLevelStepTracker().mStepDurations;
        if (numSteps <= 0) {
            return -1;
        }
        long total;
        if (numSteps == 1) {
            total = steps[0] & STEP_LEVEL_TIME_MASK;
        } else if (numSteps == 2) {
            total = ((steps[0] & STEP_LEVEL_TIME_MASK) + (steps[1] & STEP_LEVEL_TIME_MASK)) / 2;
        } else {
            int former = numSteps / 3;
            int middle = (numSteps * 2) / 3;
            long total_former = 0;
            long total_middle = 0;
            long total_latter = 0;
            for (int i = 0; i < former; i++) {
                total_former += steps[i] & STEP_LEVEL_TIME_MASK;
            }
            for (int j = former; j < middle; j++) {
                total_middle += steps[j] & STEP_LEVEL_TIME_MASK;
            }
            for (int k = middle; k < numSteps; k++) {
                total_latter += steps[k] & STEP_LEVEL_TIME_MASK;
            }
            total = ((total_former / (((long) former) * 2)) + ((3 * total_middle) / (((long) (middle - former)) * 10))) + (total_latter / (((long) (numSteps - middle)) * 5));
        }
        return total;
    }

    public boolean startIteratingHistoryLocked() {
        return this.mBatteryStatsHelper.getStats().startIteratingHistoryLocked();
    }

    public boolean getNextHistoryLocked(HwHistoryItem out) {
        return this.mBatteryStatsHelper.getStats().getNextHistoryLocked(out.getInnerHistoryItem());
    }

    public void finishIteratingHistoryLocked() {
        this.mBatteryStatsHelper.getStats().finishIteratingHistoryLocked();
    }
}
