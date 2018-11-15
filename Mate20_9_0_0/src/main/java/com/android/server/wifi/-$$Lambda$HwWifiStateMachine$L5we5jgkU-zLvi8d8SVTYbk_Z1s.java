package com.android.server.wifi;

import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwWifiStateMachine$L5we5jgkU-zLvi8d8SVTYbk_Z1s implements Runnable {
    private final /* synthetic */ HwWifiStateMachine f$0;
    private final /* synthetic */ List f$1;

    public /* synthetic */ -$$Lambda$HwWifiStateMachine$L5we5jgkU-zLvi8d8SVTYbk_Z1s(HwWifiStateMachine hwWifiStateMachine, List list) {
        this.f$0 = hwWifiStateMachine;
        this.f$1 = list;
    }

    public final void run() {
        this.f$1.addAll(HwWifiStateMachine.wifiStateMachineUtils.getScanRequestProxy(this.f$0).getScanResults());
    }
}
