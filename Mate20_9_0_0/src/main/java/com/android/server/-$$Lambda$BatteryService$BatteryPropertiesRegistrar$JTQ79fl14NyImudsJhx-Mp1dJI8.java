package com.android.server;

import android.hardware.health.V2_0.IHealth.getCurrentNowCallback;
import android.os.BatteryProperty;
import android.util.MutableInt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$BatteryService$BatteryPropertiesRegistrar$JTQ79fl14NyImudsJhx-Mp1dJI8 implements getCurrentNowCallback {
    private final /* synthetic */ MutableInt f$0;
    private final /* synthetic */ BatteryProperty f$1;

    public /* synthetic */ -$$Lambda$BatteryService$BatteryPropertiesRegistrar$JTQ79fl14NyImudsJhx-Mp1dJI8(MutableInt mutableInt, BatteryProperty batteryProperty) {
        this.f$0 = mutableInt;
        this.f$1 = batteryProperty;
    }

    public final void onValues(int i, int i2) {
        BatteryPropertiesRegistrar.lambda$getProperty$1(this.f$0, this.f$1, i, i2);
    }
}
