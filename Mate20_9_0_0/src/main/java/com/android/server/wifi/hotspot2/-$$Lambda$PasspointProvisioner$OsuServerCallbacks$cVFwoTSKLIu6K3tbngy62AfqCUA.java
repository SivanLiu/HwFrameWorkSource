package com.android.server.wifi.hotspot2;

import com.android.server.wifi.hotspot2.PasspointProvisioner.OsuServerCallbacks;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PasspointProvisioner$OsuServerCallbacks$cVFwoTSKLIu6K3tbngy62AfqCUA implements Runnable {
    private final /* synthetic */ OsuServerCallbacks f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$PasspointProvisioner$OsuServerCallbacks$cVFwoTSKLIu6K3tbngy62AfqCUA(OsuServerCallbacks osuServerCallbacks, int i) {
        this.f$0 = osuServerCallbacks;
        this.f$1 = i;
    }

    public final void run() {
        PasspointProvisioner.this.mProvisioningStateMachine.handleServerValidationFailure(this.f$1);
    }
}
