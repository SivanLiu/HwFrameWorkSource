package com.android.server;

import android.content.Context;
import android.hardware.health.V1_0.HealthInfo;

public abstract class AbsBatteryService extends SystemService {
    public AbsBatteryService(Context context) {
        super(context);
    }

    protected void updateLight(boolean enable, int ledOnMS, int ledOffMS) {
    }

    protected void updateLight() {
    }

    protected void newUpdateLightsLocked() {
    }

    protected void playRing() {
    }

    protected void stopRing() {
    }

    protected void printBatteryLog(HealthInfo oldInfo, android.hardware.health.V2_0.HealthInfo newInfo, int oldPlugType, boolean updatesStopped) {
    }

    protected int alterWirelessTxSwitchInternal(int status) {
        return 0;
    }

    protected int getWirelessTxSwitchInternal() {
        return 0;
    }

    protected boolean supportWirelessTxChargeInternal() {
        return false;
    }
}
