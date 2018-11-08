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
        return this.mPowerProfile.getAveragePower(type, level);
    }

    public double getAveragePower(String type) {
        return this.mPowerProfile.getAveragePowerOrDefault(type, 0.0d);
    }

    public double getBatteryCapacity() {
        return this.mPowerProfile.getBatteryCapacity();
    }

    public long getTotalClusterTime(HwBatterySipper sipper) {
        long totalTime = 0;
        Uid u = sipper.getBatterySipper().uidObj;
        int numClusters = this.mPowerProfile.getNumCpuClusters();
        for (int cluster = 0; cluster < numClusters; cluster++) {
            for (int speed = 0; speed < this.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster); speed++) {
                totalTime += u.getTimeAtCpuSpeed(cluster, speed, 2);
            }
        }
        return totalTime;
    }

    public double getCpuPowerMaMs(HwBatterySipper sipper, long totalTime) {
        Uid u = sipper.getBatterySipper().uidObj;
        long cpuTimeMs = (u.getUserCpuTimeUs(2) + u.getSystemCpuTimeUs(2)) / 1000;
        int numClusters = this.mPowerProfile.getNumCpuClusters();
        double cpuPowerMaMs = 0.0d;
        for (int cluster = 0; cluster < numClusters; cluster++) {
            for (int speed = 0; speed < this.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster); speed++) {
                cpuPowerMaMs += (((double) cpuTimeMs) * (((double) u.getTimeAtCpuSpeed(cluster, speed, 2)) / ((double) totalTime))) * this.mPowerProfile.getAveragePowerForCpu(cluster, speed);
            }
        }
        return cpuPowerMaMs;
    }

    public double getMinAveragePowerForCpu() {
        return this.mPowerProfile.getAveragePowerForCpu(0, 0);
    }

    public long getCpuFgTimeMs(HwBatterySipper sipper) {
        long cpuFgTimeMs = 0;
        ArrayMap<String, ? extends Proc> processStats = sipper.getBatterySipper().uidObj.getProcessStats();
        for (int i = 0; i < processStats.size(); i++) {
            cpuFgTimeMs += ((Proc) processStats.valueAt(i)).getForegroundTime(2);
        }
        return cpuFgTimeMs;
    }
}
