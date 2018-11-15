package com.android.server.wifi.p2p;

import com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM implements InterfaceDestroyedListener {
    private final /* synthetic */ P2pDisabledState f$0;

    public /* synthetic */ -$$Lambda$WifiP2pServiceImpl$P2pStateMachine$P2pDisabledState$13XANUNRJEt7WjtJr5tKTd2g-PM(P2pDisabledState p2pDisabledState) {
        this.f$0 = p2pDisabledState;
    }

    public final void onDestroyed(String str) {
        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DISABLE_P2P);
    }
}
