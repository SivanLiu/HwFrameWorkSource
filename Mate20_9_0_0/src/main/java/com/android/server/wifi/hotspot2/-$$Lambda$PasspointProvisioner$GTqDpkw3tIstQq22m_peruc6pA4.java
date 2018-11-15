package com.android.server.wifi.hotspot2;

import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PasspointProvisioner$GTqDpkw3tIstQq22m_peruc6pA4 implements Runnable {
    private final /* synthetic */ PasspointProvisioner f$0;
    private final /* synthetic */ OsuProvider f$1;
    private final /* synthetic */ IProvisioningCallback f$2;

    public /* synthetic */ -$$Lambda$PasspointProvisioner$GTqDpkw3tIstQq22m_peruc6pA4(PasspointProvisioner passpointProvisioner, OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback) {
        this.f$0 = passpointProvisioner;
        this.f$1 = osuProvider;
        this.f$2 = iProvisioningCallback;
    }

    public final void run() {
        this.f$0.mProvisioningStateMachine.startProvisioning(this.f$1, this.f$2);
    }
}
