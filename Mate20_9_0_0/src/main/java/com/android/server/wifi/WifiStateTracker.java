package com.android.server.wifi;

import android.os.RemoteException;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import java.util.concurrent.RejectedExecutionException;

public class WifiStateTracker {
    public static final int CONNECTED = 3;
    public static final int DISCONNECTED = 2;
    public static final int INVALID = 0;
    public static final int SCAN_MODE = 1;
    public static final int SOFT_AP = 4;
    private static final String TAG = "WifiStateTracker";
    private IBatteryStats mBatteryStats;
    private int mWifiState = 0;

    public WifiStateTracker(IBatteryStats stats) {
        this.mBatteryStats = stats;
    }

    private void informWifiStateBatteryStats(int state) {
        String str;
        StringBuilder stringBuilder;
        try {
            this.mBatteryStats.noteWifiState(state, null);
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Battery stats unreachable ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (RejectedExecutionException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Battery stats executor is being shutdown ");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0011, code skipped:
            r2.mWifiState = r3;
            informWifiStateBatteryStats(r0);
     */
    /* JADX WARNING: Missing block: B:9:0x001a, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateState(int state) {
        if (state != this.mWifiState) {
            int reportState;
            switch (state) {
                case 0:
                    this.mWifiState = 0;
                    break;
                case 1:
                    reportState = 1;
                    break;
                case 2:
                    reportState = 3;
                    break;
                case 3:
                    reportState = 4;
                    break;
                case 4:
                    reportState = 7;
                    break;
            }
        }
    }
}
