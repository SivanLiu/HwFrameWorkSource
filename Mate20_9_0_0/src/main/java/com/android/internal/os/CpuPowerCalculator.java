package com.android.internal.os;

import android.os.BatteryStats.Uid;
import android.os.BatteryStats.Uid.Proc;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;

public class CpuPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final long MICROSEC_IN_HR = 3600000000L;
    private static final String TAG = "CpuPowerCalculator";
    private final PowerProfile mProfile;

    public CpuPowerCalculator(PowerProfile profile) {
        this.mProfile = profile;
    }

    public void calculateApp(BatterySipper app, Uid u, long rawRealtimeUs, long rawUptimeUs, int statsType) {
        long[] cpuClusterTimes;
        BatterySipper batterySipper = app;
        Uid uid = u;
        int i = statsType;
        batterySipper.cpuTimeMs = (uid.getUserCpuTimeUs(i) + uid.getSystemCpuTimeUs(i)) / 1000;
        int numClusters = this.mProfile.getNumCpuClusters();
        double cpuPowerMaUs = 0.0d;
        int cluster = 0;
        while (cluster < numClusters) {
            double cpuPowerMaUs2 = cpuPowerMaUs;
            for (int speed = 0; speed < this.mProfile.getNumSpeedStepsInCpuCluster(cluster); speed++) {
                cpuPowerMaUs2 += ((double) uid.getTimeAtCpuSpeed(cluster, speed, i)) * this.mProfile.getAveragePowerForCpuCore(cluster, speed);
            }
            cluster++;
            cpuPowerMaUs = cpuPowerMaUs2;
        }
        cpuPowerMaUs += ((double) (u.getCpuActiveTime() * 1000)) * this.mProfile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE);
        long[] cpuClusterTimes2 = u.getCpuClusterTimes();
        if (cpuClusterTimes2 != null) {
            if (cpuClusterTimes2.length == numClusters) {
                for (int i2 = 0; i2 < numClusters; i2++) {
                    cpuPowerMaUs += ((double) (cpuClusterTimes2[i2] * 1000)) * this.mProfile.getAveragePowerForCpuCluster(i2);
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UID ");
                stringBuilder.append(u.getUid());
                stringBuilder.append(" CPU cluster # mismatch: Power Profile # ");
                stringBuilder.append(numClusters);
                stringBuilder.append(" actual # ");
                stringBuilder.append(cpuClusterTimes2.length);
                Log.w(str, stringBuilder.toString());
            }
        }
        batterySipper.cpuPowerMah = cpuPowerMaUs / 3.6E9d;
        double highestDrain = 0.0d;
        batterySipper.cpuFgTimeMs = 0;
        ArrayMap<String, ? extends Proc> processStats = u.getProcessStats();
        int processStatsCount = processStats.size();
        int i3 = 0;
        while (true) {
            int i4 = i3;
            if (i4 >= processStatsCount) {
                break;
            }
            double highestDrain2;
            Proc ps = (Proc) processStats.valueAt(i4);
            String processName = (String) processStats.keyAt(i4);
            int numClusters2 = numClusters;
            cpuClusterTimes = cpuClusterTimes2;
            batterySipper.cpuFgTimeMs += ps.getForegroundTime(i);
            numClusters = (ps.getUserTime(i) + ps.getSystemTime(i)) + ps.getForegroundTime(i);
            if (batterySipper.packageWithHighestDrain == null || batterySipper.packageWithHighestDrain.startsWith(PhoneConstants.APN_TYPE_ALL)) {
                highestDrain2 = (double) numClusters;
                batterySipper.packageWithHighestDrain = processName;
            } else {
                if (highestDrain < ((double) numClusters) && !processName.startsWith(PhoneConstants.APN_TYPE_ALL)) {
                    highestDrain2 = (double) numClusters;
                    batterySipper.packageWithHighestDrain = processName;
                }
                i3 = i4 + 1;
                numClusters = numClusters2;
                cpuClusterTimes2 = cpuClusterTimes;
                uid = u;
                i = statsType;
            }
            highestDrain = highestDrain2;
            i3 = i4 + 1;
            numClusters = numClusters2;
            cpuClusterTimes2 = cpuClusterTimes;
            uid = u;
            i = statsType;
        }
        cpuClusterTimes = cpuClusterTimes2;
        if (batterySipper.cpuFgTimeMs > batterySipper.cpuTimeMs) {
            batterySipper.cpuTimeMs = batterySipper.cpuFgTimeMs;
        }
    }
}
