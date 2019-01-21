package com.android.internal.os;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.os.BatteryStats.Uid;
import android.util.SparseArray;
import java.util.List;

public class SensorPowerCalculator extends PowerCalculator {
    private final double mGpsPower;
    private final List<Sensor> mSensors;

    public SensorPowerCalculator(PowerProfile profile, SensorManager sensorManager, BatteryStats stats, long rawRealtimeUs, int statsType) {
        this.mSensors = sensorManager.getSensorList(-1);
        this.mGpsPower = getAverageGpsPower(profile, stats, rawRealtimeUs, statsType);
    }

    public void calculateApp(BatterySipper app, Uid u, long rawRealtimeUs, long rawUptimeUs, int statsType) {
        SparseArray<? extends Uid.Sensor> sensorStats;
        int NSE;
        BatterySipper batterySipper = app;
        SparseArray<? extends Uid.Sensor> sensorStats2 = u.getSensorStats();
        int NSE2 = sensorStats2.size();
        int ise = 0;
        while (ise < NSE2) {
            Uid.Sensor sensor = (Uid.Sensor) sensorStats2.valueAt(ise);
            int sensorHandle = sensorStats2.keyAt(ise);
            long sensorTime = sensor.getSensorTime().getTotalTimeLocked(rawRealtimeUs, statsType) / 1000;
            Uid.Sensor sensor2;
            int i;
            if (sensorHandle != -10000) {
                int sensorsCount = this.mSensors.size();
                int i2 = 0;
                while (i2 < sensorsCount) {
                    Sensor s = (Sensor) this.mSensors.get(i2);
                    sensorStats = sensorStats2;
                    if (s.getHandle() == sensorHandle) {
                        NSE = NSE2;
                        batterySipper.sensorPowerMah += (double) ((((float) sensorTime) * s.getPower()) / 3600000.0f);
                        break;
                    }
                    sensor2 = sensor;
                    i = sensorHandle;
                    i2++;
                    sensorStats2 = sensorStats;
                }
                sensorStats = sensorStats2;
                NSE = NSE2;
            } else {
                sensorStats = sensorStats2;
                NSE = NSE2;
                sensor2 = sensor;
                i = sensorHandle;
                batterySipper.gpsTimeMs = sensorTime;
                batterySipper.gpsPowerMah = (((double) batterySipper.gpsTimeMs) * this.mGpsPower) / 3600000.0d;
            }
            ise++;
            sensorStats2 = sensorStats;
            NSE2 = NSE;
        }
        long j = rawRealtimeUs;
        int i3 = statsType;
        sensorStats = sensorStats2;
        NSE = NSE2;
    }

    private double getAverageGpsPower(PowerProfile profile, BatteryStats stats, long rawRealtimeUs, int statsType) {
        PowerProfile powerProfile = profile;
        double averagePower = powerProfile.getAveragePowerOrDefault(PowerProfile.POWER_GPS_ON, -1.0d);
        if (averagePower != -1.0d) {
            return averagePower;
        }
        double averagePower2 = 0.0d;
        long totalTime = 0;
        double totalPower = 0.0d;
        int i = 0;
        while (i < 2) {
            long timePerLevel = stats.getGpsSignalQualityTime(i, rawRealtimeUs, statsType);
            totalTime += timePerLevel;
            totalPower += powerProfile.getAveragePower(PowerProfile.POWER_GPS_SIGNAL_QUALITY_BASED, i) * ((double) timePerLevel);
            i++;
            averagePower2 = averagePower2;
            powerProfile = profile;
        }
        BatteryStats batteryStats = stats;
        long j = rawRealtimeUs;
        int i2 = statsType;
        double averagePower3 = averagePower2;
        if (totalTime != 0) {
            averagePower3 = totalPower / ((double) totalTime);
        }
        return averagePower3;
    }
}
