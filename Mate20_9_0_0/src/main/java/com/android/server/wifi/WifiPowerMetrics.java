package com.android.server.wifi;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.connectivity.WifiBatteryStats;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.internal.app.IBatteryStats.Stub;
import com.android.server.wifi.nano.WifiMetricsProto.WifiPowerStats;
import java.io.PrintWriter;

public class WifiPowerMetrics {
    private static final String TAG = "WifiPowerMetrics";
    private final IBatteryStats mBatteryStats = Stub.asInterface(ServiceManager.getService("batterystats"));

    public WifiPowerStats buildProto() {
        WifiPowerStats m = new WifiPowerStats();
        WifiBatteryStats stats = getStats();
        if (stats != null) {
            m.loggingDurationMs = stats.getLoggingDurationMs();
            m.energyConsumedMah = ((double) stats.getEnergyConsumedMaMs()) / 3600000.0d;
            m.idleTimeMs = stats.getIdleTimeMs();
            m.rxTimeMs = stats.getRxTimeMs();
            m.txTimeMs = stats.getTxTimeMs();
        }
        return m;
    }

    public void dump(PrintWriter pw) {
        WifiPowerStats s = buildProto();
        if (s != null) {
            pw.println("Wifi power metrics:");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Logging duration (time on battery): ");
            stringBuilder.append(s.loggingDurationMs);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Energy consumed by wifi (mAh): ");
            stringBuilder.append(s.energyConsumedMah);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Amount of time wifi is in idle (ms): ");
            stringBuilder.append(s.idleTimeMs);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Amount of time wifi is in rx (ms): ");
            stringBuilder.append(s.rxTimeMs);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Amount of time wifi is in tx (ms): ");
            stringBuilder.append(s.txTimeMs);
            pw.println(stringBuilder.toString());
        }
    }

    private WifiBatteryStats getStats() {
        try {
            return this.mBatteryStats.getWifiBatteryStats();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to obtain Wifi power stats from BatteryStats");
            return null;
        }
    }
}
