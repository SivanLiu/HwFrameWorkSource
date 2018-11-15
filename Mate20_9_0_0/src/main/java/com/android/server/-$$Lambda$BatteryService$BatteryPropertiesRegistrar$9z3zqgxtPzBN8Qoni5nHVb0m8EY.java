package com.android.server;

import android.hardware.health.V2_0.IHealth.getEnergyCounterCallback;
import android.os.BatteryProperty;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryService$BatteryPropertiesRegistrar$9z3zqgxtPzBN8Qoni5nHVb0m8EY implements getEnergyCounterCallback {
    private final /* synthetic */ MutableInt f$0;
    private final /* synthetic */ BatteryProperty f$1;

    public /* synthetic */ -$$Lambda$BatteryService$BatteryPropertiesRegistrar$9z3zqgxtPzBN8Qoni5nHVb0m8EY(MutableInt mutableInt, BatteryProperty batteryProperty) {
        this.f$0 = mutableInt;
        this.f$1 = batteryProperty;
    }

    public final void onValues(int i, long j) {
        BatteryPropertiesRegistrar.lambda$getProperty$5(this.f$0, this.f$1, i, j);
    }
}
