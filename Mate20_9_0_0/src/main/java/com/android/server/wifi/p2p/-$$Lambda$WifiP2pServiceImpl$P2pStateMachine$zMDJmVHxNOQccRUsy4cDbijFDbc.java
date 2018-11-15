package com.android.server.wifi.p2p;

import com.android.server.wifi.HalDeviceManager.InterfaceAvailableForRequestListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiP2pServiceImpl$P2pStateMachine$zMDJmVHxNOQccRUsy4cDbijFDbc implements InterfaceAvailableForRequestListener {
    private final /* synthetic */ P2pStateMachine f$0;

    public /* synthetic */ -$$Lambda$WifiP2pServiceImpl$P2pStateMachine$zMDJmVHxNOQccRUsy4cDbijFDbc(P2pStateMachine p2pStateMachine) {
        this.f$0 = p2pStateMachine;
    }

    public final void onAvailabilityChanged(boolean z) {
        P2pStateMachine.lambda$new$0(this.f$0, z);
    }
}
