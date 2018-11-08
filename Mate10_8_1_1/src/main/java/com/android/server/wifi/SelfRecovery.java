package com.android.server.wifi;

import android.util.Log;
import java.util.Iterator;
import java.util.LinkedList;

public class SelfRecovery {
    public static final long MAX_RESTARTS_IN_TIME_WINDOW = 2;
    public static final long MAX_RESTARTS_TIME_WINDOW_MILLIS = 3600000;
    public static final int REASON_HAL_CRASH = 1;
    public static final int REASON_LAST_RESORT_WATCHDOG = 0;
    private static final String[] REASON_STRINGS = new String[]{"Last Resort Watchdog", "Hal Crash", "Wificond Crash"};
    public static final int REASON_WIFICOND_CRASH = 2;
    private static final String TAG = "WifiSelfRecovery";
    private final Clock mClock;
    private final LinkedList<Long> mPastRestartTimes = new LinkedList();
    private final WifiController mWifiController;

    public SelfRecovery(WifiController wifiController, Clock clock) {
        this.mWifiController = wifiController;
        this.mClock = clock;
    }

    public void trigger(int reason) {
        if (reason == 0 || reason == 1 || reason == 2) {
            Log.e(TAG, "Triggering recovery for reason: " + REASON_STRINGS[reason]);
            if (reason == 2 || reason == 1) {
                trimPastRestartTimes();
                if (((long) this.mPastRestartTimes.size()) >= 2) {
                    Log.e(TAG, "Already restarted wifi (2) times in last (3600000ms ). Ignoring...");
                    return;
                }
                this.mPastRestartTimes.add(Long.valueOf(this.mClock.getElapsedSinceBootMillis()));
            }
            this.mWifiController.sendMessage(155665);
            return;
        }
        Log.e(TAG, "Invalid trigger reason. Ignoring...");
    }

    private void trimPastRestartTimes() {
        Iterator<Long> iter = this.mPastRestartTimes.iterator();
        long now = this.mClock.getElapsedSinceBootMillis();
        while (iter.hasNext() && now - ((Long) iter.next()).longValue() > 3600000) {
            iter.remove();
        }
    }
}
