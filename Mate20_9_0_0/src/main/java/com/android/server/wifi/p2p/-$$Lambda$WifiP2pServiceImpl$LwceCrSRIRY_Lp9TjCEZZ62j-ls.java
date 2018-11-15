package com.android.server.wifi.p2p;

import android.os.IBinder;
import android.os.IBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiP2pServiceImpl$LwceCrSRIRY_Lp9TjCEZZ62j-ls implements DeathRecipient {
    private final /* synthetic */ WifiP2pServiceImpl f$0;
    private final /* synthetic */ IBinder f$1;

    public /* synthetic */ -$$Lambda$WifiP2pServiceImpl$LwceCrSRIRY_Lp9TjCEZZ62j-ls(WifiP2pServiceImpl wifiP2pServiceImpl, IBinder iBinder) {
        this.f$0 = wifiP2pServiceImpl;
        this.f$1 = iBinder;
    }

    public final void binderDied() {
        WifiP2pServiceImpl.lambda$getMessenger$0(this.f$0, this.f$1);
    }
}
