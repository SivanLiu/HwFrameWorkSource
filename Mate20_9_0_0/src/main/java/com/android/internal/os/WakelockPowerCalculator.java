package com.android.internal.os;

import android.os.BatteryStats;
import android.os.BatteryStats.Timer;
import android.os.BatteryStats.Uid;
import android.os.BatteryStats.Uid.Wakelock;
import android.util.ArrayMap;

public class WakelockPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "WakelockPowerCalculator";
    private final double mPowerWakelock;
    private long mTotalAppWakelockTimeMs = 0;

    public WakelockPowerCalculator(PowerProfile profile) {
        this.mPowerWakelock = profile.getAveragePower(PowerProfile.POWER_CPU_IDLE);
    }

    public void calculateApp(BatterySipper app, Uid u, long rawRealtimeUs, long rawUptimeUs, int statsType) {
        long j;
        int i;
        BatterySipper batterySipper = app;
        ArrayMap<String, ? extends Wakelock> wakelockStats = u.getWakelockStats();
        int wakelockStatsCount = wakelockStats.size();
        long wakeLockTimeUs = 0;
        for (int i2 = 0; i2 < wakelockStatsCount; i2++) {
            Timer timer = ((Wakelock) wakelockStats.valueAt(i2)).getWakeTime(0);
            if (timer != null) {
                wakeLockTimeUs += timer.getTotalTimeLocked(rawRealtimeUs, statsType);
            } else {
                j = rawRealtimeUs;
                i = statsType;
            }
        }
        j = rawRealtimeUs;
        i = statsType;
        batterySipper.wakeLockTimeMs = wakeLockTimeUs / 1000;
        this.mTotalAppWakelockTimeMs += batterySipper.wakeLockTimeMs;
        batterySipper.wakeLockPowerMah = (((double) batterySipper.wakeLockTimeMs) * this.mPowerWakelock) / 3600000.0d;
    }

    public void calculateRemaining(BatterySipper app, BatteryStats stats, long rawRealtimeUs, long rawUptimeUs, int statsType) {
        if (stats != null) {
            long wakeTimeMillis = (stats.getBatteryUptime(rawUptimeUs) / 1000) - (this.mTotalAppWakelockTimeMs + (stats.getScreenOnTime(rawRealtimeUs, statsType) / 1000));
            if (wakeTimeMillis > 0) {
                double power = (((double) wakeTimeMillis) * this.mPowerWakelock) / 3600000.0d;
                app.wakeLockTimeMs += wakeTimeMillis;
                app.wakeLockPowerMah += power;
            }
        }
    }

    public void reset() {
        this.mTotalAppWakelockTimeMs = 0;
    }
}
