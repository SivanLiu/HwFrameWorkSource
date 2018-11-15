package com.android.server.wifi;

import android.os.IHwBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HalDeviceManager$jNAzj5YlVhwJm5NjZ6HiKskQStI implements DeathRecipient {
    private final /* synthetic */ HalDeviceManager f$0;

    public /* synthetic */ -$$Lambda$HalDeviceManager$jNAzj5YlVhwJm5NjZ6HiKskQStI(HalDeviceManager halDeviceManager) {
        this.f$0 = halDeviceManager;
    }

    public final void serviceDied(long j) {
        HalDeviceManager.lambda$new$2(this.f$0, j);
    }
}
