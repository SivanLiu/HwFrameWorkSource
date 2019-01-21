package com.android.internal.os;

import android.os.BatteryStats;
import android.os.BatteryStats.ControllerActivityCounter;
import android.os.BatteryStats.Uid;

public class WifiPowerCalculator extends PowerCalculator {
    private static final boolean DEBUG = false;
    private static final String TAG = "WifiPowerCalculator";
    private final double mIdleCurrentMa;
    private final double mRxCurrentMa;
    private double mTotalAppPowerDrain = 0.0d;
    private long mTotalAppRunningTime = 0;
    private final double mTxCurrentMa;

    public WifiPowerCalculator(PowerProfile profile) {
        this.mIdleCurrentMa = profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_IDLE);
        this.mTxCurrentMa = profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_TX);
        this.mRxCurrentMa = profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_RX);
    }

    public void calculateApp(BatterySipper app, Uid u, long rawRealtimeUs, long rawUptimeUs, int statsType) {
        BatterySipper batterySipper = app;
        Uid uid = u;
        int i = statsType;
        ControllerActivityCounter counter = u.getWifiControllerActivity();
        if (counter != null) {
            long idleTime = counter.getIdleTimeCounter().getCountLocked(i);
            long txTime = counter.getTxTimeCounters()[0].getCountLocked(i);
            long rxTime = counter.getRxTimeCounter().getCountLocked(i);
            batterySipper.wifiRunningTimeMs = (idleTime + rxTime) + txTime;
            this.mTotalAppRunningTime += batterySipper.wifiRunningTimeMs;
            batterySipper.wifiPowerMah = (((((double) idleTime) * this.mIdleCurrentMa) + (((double) txTime) * this.mTxCurrentMa)) + (((double) rxTime) * this.mRxCurrentMa)) / 3600000.0d;
            this.mTotalAppPowerDrain += batterySipper.wifiPowerMah;
            batterySipper.wifiRxPackets = uid.getNetworkActivityPackets(2, i);
            batterySipper.wifiTxPackets = uid.getNetworkActivityPackets(3, i);
            batterySipper.wifiRxBytes = uid.getNetworkActivityBytes(2, i);
            batterySipper.wifiTxBytes = uid.getNetworkActivityBytes(3, i);
        }
    }

    public void calculateRemaining(BatterySipper app, BatteryStats stats, long rawRealtimeUs, long rawUptimeUs, int statsType) {
        BatterySipper batterySipper = app;
        int i = statsType;
        ControllerActivityCounter counter = stats.getWifiControllerActivity();
        long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(i);
        long txTimeMs = counter.getTxTimeCounters()[0].getCountLocked(i);
        long rxTimeMs = counter.getRxTimeCounter().getCountLocked(i);
        batterySipper.wifiRunningTimeMs = Math.max(0, ((idleTimeMs + rxTimeMs) + txTimeMs) - this.mTotalAppRunningTime);
        double powerDrainMah = ((double) counter.getPowerCounter().getCountLocked(i)) / 3600000.0d;
        if (powerDrainMah == 0.0d) {
            powerDrainMah = (((((double) idleTimeMs) * this.mIdleCurrentMa) + (((double) txTimeMs) * this.mTxCurrentMa)) + (((double) rxTimeMs) * this.mRxCurrentMa)) / 3600000.0d;
        }
        batterySipper.wifiPowerMah = Math.max(0.0d, powerDrainMah - this.mTotalAppPowerDrain);
    }

    public void reset() {
        this.mTotalAppPowerDrain = 0.0d;
        this.mTotalAppRunningTime = 0;
    }
}
