package com.android.server.wifi;

import android.os.IHwBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$noScTs3Ynk8rNxP5lvUv8ww_gg4 implements DeathRecipient {
    private final /* synthetic */ HalDeviceManager f$0;

    public /* synthetic */ -$$Lambda$HalDeviceManager$noScTs3Ynk8rNxP5lvUv8ww_gg4(HalDeviceManager halDeviceManager) {
        this.f$0 = halDeviceManager;
    }

    public final void serviceDied(long j) {
        HalDeviceManager.lambda$new$3(this.f$0, j);
    }
}
