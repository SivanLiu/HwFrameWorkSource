package com.android.server.wifi;

import android.os.IHwBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HostapdHal$ykyXfQPF5iy3e1W0s1ikBBPfH-Y implements DeathRecipient {
    private final /* synthetic */ HostapdHal f$0;

    public /* synthetic */ -$$Lambda$HostapdHal$ykyXfQPF5iy3e1W0s1ikBBPfH-Y(HostapdHal hostapdHal) {
        this.f$0 = hostapdHal;
    }

    public final void serviceDied(long j) {
        HostapdHal.lambda$new$0(this.f$0, j);
    }
}
