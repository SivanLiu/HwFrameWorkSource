package com.huawei.systemmanager.power;

import android.os.BatteryStats.Uid;
import android.os.BatteryStats.Uid.Proc;
import android.util.ArrayMap;
import com.android.internal.os.PowerProfile;

class HwPowerProfileImpl implements IHwPowerProfile {
    private PowerProfile mPowerProfile;

    public HwPowerProfileImpl(PowerProfile powerprofile) {
        this.mPowerProfile = powerprofile;
    }

    public double getAveragePower(String type, int level) {
        if (this.mPowerProfile != null) {
            return this.mPowerProfile.getAveragePower(type, level);
        }
        return 0.0d;
    }

    public double getAveragePower(String type) {
        if (this.mPowerProfile != null) {
            return this.mPowerProfile.getAveragePowerOrDefault(type, 0.0d);
        }
        return 0.0d;
    }

    public double getBatteryCapacity() {
        if (this.mPowerProfile != null) {
            return this.mPowerProfile.getBatteryCapacity();
        }
        return 0.0d;
    }

    public long getTotalClusterTime(HwBatterySipper sipper) {
        if (this.mPowerProfile == null) {
            return 0;
        }
        Uid u = sipper.getBatterySipper().uidObj;
        int numClusters = this.mPowerProfile.getNumCpuClusters();
        long totalTime = 0;
        int cluster = 0;
        while (cluster < numClusters) {
            long totalTime2 = totalTime;
            for (totalTime = 0; totalTime < this.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster); totalTime++) {
                totalTime2 += u.getTimeAtCpuSpeed(cluster, totalTime, 2);
            }
            cluster++;
            totalTime = totalTime2;
        }
        return totalTime;
    }

    public double getCpuPowerMaMs(HwBatterySipper sipper, long totalTime) {
        HwPowerProfileImpl speed = this;
        long j;
        if (speed.mPowerProfile != null) {
            Uid u = sipper.getBatterySipper().uidObj;
            int i = 2;
            long cpuTimeMs = (u.getUserCpuTimeUs(2) + u.getSystemCpuTimeUs(2)) / 1000;
            int numClusters = speed.mPowerProfile.getNumCpuClusters();
            double cpuPowerMaMs = 0.0d;
            int cluster = 0;
            while (cluster < numClusters) {
                int speedsForCluster = speed.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster);
                double cpuPowerMaMs2 = cpuPowerMaMs;
                int speed2 = 0;
                while (speed2 < speedsForCluster) {
                    int speed3 = speed2;
                    double cpuSpeedStepPower = ((double) cpuTimeMs) * (((double) u.getTimeAtCpuSpeed(cluster, speed2, i)) / ((double) totalTime));
                    PowerProfile powerProfile = speed.mPowerProfile;
                    int speed4 = speed3;
                    cpuPowerMaMs2 += cpuSpeedStepPower * powerProfile.getAveragePowerForCpuCore(cluster, speed4);
                    speed2 = speed4 + 1;
                    speed = this;
                    i = 2;
                }
                j = totalTime;
                cluster++;
                cpuPowerMaMs = cpuPowerMaMs2;
                speed = this;
                i = 2;
            }
            j = totalTime;
            return cpuPowerMaMs;
        }
        j = totalTime;
        return 0.0d;
    }

    public double getMinAveragePowerForCpu() {
        if (this.mPowerProfile != null) {
            return this.mPowerProfile.getAveragePowerForCpuCore(0, 0);
        }
        return 0.0d;
    }

    public long getCpuFgTimeMs(HwBatterySipper sipper) {
        long cpuFgTimeMs = 0;
        if (this.mPowerProfile != null) {
            ArrayMap<String, ? extends Proc> processStats = sipper.getBatterySipper().uidObj.getProcessStats();
            for (int i = 0; i < processStats.size(); i++) {
                cpuFgTimeMs += ((Proc) processStats.valueAt(i)).getForegroundTime(2);
            }
        }
        return cpuFgTimeMs;
    }
}
