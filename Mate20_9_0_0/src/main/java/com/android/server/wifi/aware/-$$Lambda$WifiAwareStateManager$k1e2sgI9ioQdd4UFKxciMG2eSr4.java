package com.android.server.wifi.aware;

import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiAwareStateManager$k1e2sgI9ioQdd4UFKxciMG2eSr4 implements Runnable {
    private final /* synthetic */ WifiAwareStateManager f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ List f$2;
    private final /* synthetic */ IWifiAwareMacAddressProvider f$3;

    public /* synthetic */ -$$Lambda$WifiAwareStateManager$k1e2sgI9ioQdd4UFKxciMG2eSr4(WifiAwareStateManager wifiAwareStateManager, int i, List list, IWifiAwareMacAddressProvider iWifiAwareMacAddressProvider) {
        this.f$0 = wifiAwareStateManager;
        this.f$1 = i;
        this.f$2 = list;
        this.f$3 = iWifiAwareMacAddressProvider;
    }

    public final void run() {
        WifiAwareStateManager.lambda$requestMacAddresses$0(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
