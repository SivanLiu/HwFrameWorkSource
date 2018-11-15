package com.android.server.rms.iaware.memory.policy;

import android.os.Bundle;
import android.os.SystemClock;
import android.rms.iaware.AwareLog;
import com.android.server.rms.iaware.memory.action.Action;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.rms.iaware.memory.utils.MemoryReader;
import java.util.List;

public class MemoryScenePolicy {
    private static final String TAG = "AwareMem_MemPolicy";
    private final List<Action> mActions;
    private Bundle mExtras;
    private final String mName;

    public MemoryScenePolicy(String name, List<Action> actionList) {
        this.mActions = actionList;
        this.mName = name;
    }

    public String getSceneName() {
        return this.mName;
    }

    public void setExtras(Bundle extras) {
        this.mExtras = extras;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public int execute() {
        if (this.mExtras == null) {
            return -1;
        }
        long reqMem = this.mExtras.getLong("reqMem");
        String str;
        StringBuilder stringBuilder;
        if (reqMem <= 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Execute memorypolicy exit, because of reqMem is negative:");
            stringBuilder.append(reqMem);
            AwareLog.w(str, stringBuilder.toString());
            return -1;
        } else if (reqMem > MemoryConstant.getMaxReqMem()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Execute memorypolicy exit, because of reqMem is too big:");
            stringBuilder.append(reqMem);
            AwareLog.w(str, stringBuilder.toString());
            return -1;
        } else if (this.mActions == null || this.mActions.isEmpty()) {
            AwareLog.w(TAG, "Memorypolicy actions is empty");
            return -1;
        } else {
            int result = 0;
            long start = SystemClock.elapsedRealtime();
            for (Action action : this.mActions) {
                result |= action.execute(this.mExtras);
            }
            long end = SystemClock.elapsedRealtime();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Execute memorypolicy use: ");
            stringBuilder2.append(end - start);
            stringBuilder2.append(" ms");
            AwareLog.d(str2, stringBuilder2.toString());
            return result;
        }
    }

    public void clear() {
        AwareLog.d(TAG, "clear memorypolicy");
        this.mExtras = null;
        forceInterrupt(false);
    }

    public void reset() {
        AwareLog.d(TAG, "reset memorypolicy");
        this.mExtras = null;
        forceInterrupt(true);
        if (this.mActions != null && !this.mActions.isEmpty()) {
            for (Action action : this.mActions) {
                action.reset();
            }
        }
    }

    public boolean canBeExecuted() {
        if (this.mActions == null || this.mActions.isEmpty()) {
            AwareLog.w(TAG, "Memorypolicy canBeExecuted: actions is empty");
            return false;
        }
        for (Action action : this.mActions) {
            if (!action.canBeExecuted()) {
                return false;
            }
        }
        return true;
    }

    public long getPollingPeriod() {
        if (this.mActions == null || this.mActions.isEmpty()) {
            return MemoryConstant.getDefaultTimerPeriod();
        }
        int maxStepCount = getPollingStepCount();
        if (maxStepCount <= 0) {
            return getMemPressure();
        }
        long pollingPeriod;
        if (maxStepCount < 15) {
            pollingPeriod = (long) (((double) MemoryConstant.getDefaultTimerPeriod()) * Math.pow(2.0d, (double) maxStepCount));
        } else {
            pollingPeriod = MemoryConstant.getMaxTimerPeriod();
        }
        return pollingPeriod;
    }

    private int getPollingStepCount() {
        int maxStepCount = 0;
        for (Action action : this.mActions) {
            int stepCount = action.getLastExecFailCount();
            if (action.getLastExecFailCount() < 1) {
                return 0;
            }
            maxStepCount = maxStepCount < stepCount ? stepCount : maxStepCount;
        }
        return maxStepCount;
    }

    private long getMemPressure() {
        long availableRam = MemoryReader.getInstance().getMemAvailable();
        if (availableRam <= 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("calcMemPressure read availableRam err!");
            stringBuilder.append(availableRam);
            AwareLog.w(str, stringBuilder.toString());
            return MemoryConstant.getDefaultTimerPeriod();
        } else if (availableRam >= MemoryConstant.getIdleMemory()) {
            return MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
        } else {
            long minInterval = MemoryConstant.getMinTimerPeriod();
            if (availableRam <= MemoryConstant.getEmergencyMemory()) {
                return minInterval;
            }
            long interval = (MemoryConstant.MIN_INTERVAL_OP_TIMEOUT * (availableRam - MemoryConstant.getEmergencyMemory())) / (MemoryConstant.getIdleMemory() - MemoryConstant.getEmergencyMemory());
            return interval < minInterval ? minInterval : interval;
        }
    }

    public long getCpuPressure(long availableRam, long cpuThresHold, long sysCpuOverLoadCnt) {
        long j = sysCpuOverLoadCnt;
        if (availableRam < MemoryConstant.getEmergencyMemory() || availableRam >= MemoryConstant.getCriticalMemory()) {
            return cpuThresHold;
        }
        long curCpuLoad = (cpuThresHold + (((MemoryConstant.getCriticalMemory() - availableRam) * (100 - cpuThresHold)) / (MemoryConstant.getCriticalMemory() - MemoryConstant.getEmergencyMemory()))) + (6 * j);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCpuPressure curCpuLoad=");
        stringBuilder.append(curCpuLoad);
        stringBuilder.append(" sysCpuOverLoadCnt:");
        stringBuilder.append(j);
        AwareLog.d(str, stringBuilder.toString());
        return curCpuLoad;
    }

    public boolean interrupt(boolean forced) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Interrupt memorypolicy: forced=");
        stringBuilder.append(forced);
        AwareLog.i(str, stringBuilder.toString());
        if (!forced) {
            return reqInterrupt();
        }
        forceInterrupt(true);
        return true;
    }

    private void forceInterrupt(boolean interrupted) {
        if (this.mActions != null && !this.mActions.isEmpty()) {
            for (Action action : this.mActions) {
                action.interrupt(interrupted);
            }
        }
    }

    private boolean reqInterrupt() {
        if (this.mActions == null || this.mActions.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (Action action : this.mActions) {
            if (!action.reqInterrupt(this.mExtras)) {
                result = false;
            }
        }
        return result;
    }
}
