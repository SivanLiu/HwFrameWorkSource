package com.android.server.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiLastResortWatchdog$mIpWfGvGcRn_UMBeTeYZKeAS5FM implements Runnable {
    private final /* synthetic */ WifiLastResortWatchdog f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$WifiLastResortWatchdog$mIpWfGvGcRn_UMBeTeYZKeAS5FM(WifiLastResortWatchdog wifiLastResortWatchdog, String str) {
        this.f$0 = wifiLastResortWatchdog;
        this.f$1 = str;
    }

    public final void run() {
        this.f$0.mWifiStateMachine.takeBugReport(WifiLastResortWatchdog.BUGREPORT_TITLE, this.f$1);
    }
}
