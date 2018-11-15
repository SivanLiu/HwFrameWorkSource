package com.android.server.wifi.p2p;

import android.os.Handler;
import com.android.server.wifi.HalDeviceManager.ManagerStatusListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiP2pNative$OugPqsliuKv73AxYwflB8JKX3Gg implements ManagerStatusListener {
    private final /* synthetic */ WifiP2pNative f$0;
    private final /* synthetic */ Handler f$1;

    public /* synthetic */ -$$Lambda$WifiP2pNative$OugPqsliuKv73AxYwflB8JKX3Gg(WifiP2pNative wifiP2pNative, Handler handler) {
        this.f$0 = wifiP2pNative;
        this.f$1 = handler;
    }

    public final void onStatusChanged() {
        WifiP2pNative.lambda$registerInterfaceAvailableListener$0(this.f$0, this.f$1);
    }
}
