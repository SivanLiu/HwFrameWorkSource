package com.android.server.pm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LauncherAppsService$LauncherAppsImpl$MyPackageMonitor$eTair5Mvr14v4M0nq9aQEW2cp-Y implements Runnable {
    private final /* synthetic */ MyPackageMonitor f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$LauncherAppsService$LauncherAppsImpl$MyPackageMonitor$eTair5Mvr14v4M0nq9aQEW2cp-Y(MyPackageMonitor myPackageMonitor, String str, int i) {
        this.f$0 = myPackageMonitor;
        this.f$1 = str;
        this.f$2 = i;
    }

    public final void run() {
        this.f$0.onShortcutChangedInner(this.f$1, this.f$2);
    }
}
