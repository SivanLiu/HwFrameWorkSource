package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiInfo;

public class AggressiveConnectedScore extends ConnectedScore {
    private int mFrequencyMHz = 5000;
    private int mRssi = 0;
    private final int mThresholdQualifiedRssi24;
    private final int mThresholdQualifiedRssi5;

    public AggressiveConnectedScore(Context context, Clock clock) {
        super(clock);
        this.mThresholdQualifiedRssi5 = context.getResources().getInteger(17694910);
        this.mThresholdQualifiedRssi24 = context.getResources().getInteger(17694909);
    }

    public void updateUsingRssi(int rssi, long millis, double standardDeviation) {
        this.mRssi = rssi;
    }

    public void updateUsingWifiInfo(WifiInfo wifiInfo, long millis) {
        this.mFrequencyMHz = wifiInfo.getFrequency();
        this.mRssi = wifiInfo.getRssi();
    }

    public void reset() {
        this.mFrequencyMHz = 5000;
    }

    public int generateScore() {
        return (this.mRssi - (this.mFrequencyMHz >= 5000 ? this.mThresholdQualifiedRssi5 : this.mThresholdQualifiedRssi24)) + 50;
    }
}
