package com.android.server.wifi;

import android.os.Binder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwWifiService$bNOLyr8oakjuxLhdkXG9XdmZD-4 implements Runnable {
    private final /* synthetic */ HwWifiService f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$HwWifiService$bNOLyr8oakjuxLhdkXG9XdmZD-4(HwWifiService hwWifiService, String str) {
        this.f$0 = hwWifiService;
        this.f$1 = str;
    }

    public final void run() {
        HwWifiService.wifiStateMachineUtils.getScanRequestProxy((HwWifiStateMachine) this.f$0.mWifiStateMachine).startScanForSpecBand(Binder.getCallingUid(), this.f$1, 1);
    }
}
