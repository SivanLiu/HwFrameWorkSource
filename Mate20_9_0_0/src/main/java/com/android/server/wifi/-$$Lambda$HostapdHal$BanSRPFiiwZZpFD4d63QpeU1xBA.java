package com.android.server.wifi;

import android.os.IHwBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HostapdHal$BanSRPFiiwZZpFD4d63QpeU1xBA implements DeathRecipient {
    private final /* synthetic */ HostapdHal f$0;

    public /* synthetic */ -$$Lambda$HostapdHal$BanSRPFiiwZZpFD4d63QpeU1xBA(HostapdHal hostapdHal) {
        this.f$0 = hostapdHal;
    }

    public final void serviceDied(long j) {
        HostapdHal.lambda$new$1(this.f$0, j);
    }
}
