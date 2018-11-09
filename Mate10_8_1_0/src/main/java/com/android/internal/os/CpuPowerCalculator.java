package com.android.internal.os;

import android.os.BatteryStats.Uid;
import android.os.BatteryStats.Uid.Proc;
import android.util.ArrayMap;
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
        app.cpuTimeMs = (u.getUserCpuTimeUs(statsType) + u.getSystemCpuTimeUs(statsType)) / 1000;
        int numClusters = this.mProfile.getNumCpuClusters();
        double cpuPowerMaUs = 0.0d;
        for (int cluster = 0; cluster < numClusters; cluster++) {
            for (int speed = 0; speed < this.mProfile.getNumSpeedStepsInCpuCluster(cluster); speed++) {
                cpuPowerMaUs += ((double) u.getTimeAtCpuSpeed(cluster, speed, statsType)) * this.mProfile.getAveragePowerForCpu(cluster, speed);
            }
        }
        app.cpuPowerMah = cpuPowerMaUs / 3.6E9d;
        double highestDrain = 0.0d;
        app.cpuFgTimeMs = 0;
        ArrayMap<String, ? extends Proc> processStats = u.getProcessStats();
        int processStatsCount = processStats.size();
        for (int i = 0; i < processStatsCount; i++) {
            Proc ps = (Proc) processStats.valueAt(i);
            String processName = (String) processStats.keyAt(i);
            app.cpuFgTimeMs += ps.getForegroundTime(statsType);
            long costValue = (ps.getUserTime(statsType) + ps.getSystemTime(statsType)) + ps.getForegroundTime(statsType);
            if (app.packageWithHighestDrain != null) {
                if (!app.packageWithHighestDrain.startsWith(PhoneConstants.APN_TYPE_ALL)) {
                    if (highestDrain < ((double) costValue) && (processName.startsWith(PhoneConstants.APN_TYPE_ALL) ^ 1) != 0) {
                        highestDrain = (double) costValue;
                        app.packageWithHighestDrain = processName;
                    }
                }
            }
            highestDrain = (double) costValue;
            app.packageWithHighestDrain = processName;
        }
        if (app.cpuFgTimeMs > app.cpuTimeMs) {
            app.cpuTimeMs = app.cpuFgTimeMs;
        }
    }
}
