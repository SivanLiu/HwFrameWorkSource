package com.android.server.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiController$DeviceActiveState$j60bKc3b7Z47vZXsDHcRTlqzedE implements Runnable {
    private final /* synthetic */ DeviceActiveState f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$WifiController$DeviceActiveState$j60bKc3b7Z47vZXsDHcRTlqzedE(DeviceActiveState deviceActiveState, String str, String str2) {
        this.f$0 = deviceActiveState;
        this.f$1 = str;
        this.f$2 = str2;
    }

    public final void run() {
        WifiController.this.mWifiStateMachine.takeBugReport(this.f$1, this.f$2);
    }
}
