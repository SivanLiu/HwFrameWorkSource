package com.android.server;

import android.hardware.health.V2_0.IHealth.getChargeStatusCallback;
import android.os.BatteryProperty;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryService$BatteryPropertiesRegistrar$hInbvsihGvN2hXqvdcoFYzdeqHw implements getChargeStatusCallback {
    private final /* synthetic */ MutableInt f$0;
    private final /* synthetic */ BatteryProperty f$1;

    public /* synthetic */ -$$Lambda$BatteryService$BatteryPropertiesRegistrar$hInbvsihGvN2hXqvdcoFYzdeqHw(MutableInt mutableInt, BatteryProperty batteryProperty) {
        this.f$0 = mutableInt;
        this.f$1 = batteryProperty;
    }

    public final void onValues(int i, int i2) {
        BatteryPropertiesRegistrar.lambda$getProperty$4(this.f$0, this.f$1, i, i2);
    }
}
