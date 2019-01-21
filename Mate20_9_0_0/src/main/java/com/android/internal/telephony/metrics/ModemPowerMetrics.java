package com.android.internal.telephony.metrics;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.connectivity.CellularBatteryStats;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.IBatteryStats.Stub;
import com.android.internal.telephony.nano.TelephonyProto.ModemPowerStats;

public class ModemPowerMetrics {
    private final IBatteryStats mBatteryStats = Stub.asInterface(ServiceManager.getService("batterystats"));

    public ModemPowerStats buildProto() {
        ModemPowerStats m = new ModemPowerStats();
        CellularBatteryStats stats = getStats();
        if (stats != null) {
            m.loggingDurationMs = stats.getLoggingDurationMs();
            m.energyConsumedMah = ((double) stats.getEnergyConsumedMaMs()) / 3600000.0d;
            m.numPacketsTx = stats.getNumPacketsTx();
            m.cellularKernelActiveTimeMs = stats.getKernelActiveTimeMs();
            int i = 0;
            if (stats.getTimeInRxSignalStrengthLevelMs() != null && stats.getTimeInRxSignalStrengthLevelMs().length > 0) {
                m.timeInVeryPoorRxSignalLevelMs = stats.getTimeInRxSignalStrengthLevelMs()[0];
            }
            m.sleepTimeMs = stats.getSleepTimeMs();
            m.idleTimeMs = stats.getIdleTimeMs();
            m.rxTimeMs = stats.getRxTimeMs();
            long[] t = stats.getTxTimeMs();
            m.txTimeMs = new long[t.length];
            while (i < t.length) {
                m.txTimeMs[i] = t[i];
                i++;
            }
        }
        return m;
    }

    private CellularBatteryStats getStats() {
        try {
            return this.mBatteryStats.getCellularBatteryStats();
        } catch (RemoteException e) {
            return null;
        }
    }
}
