package com.android.server;

import android.content.Context;
import com.android.server.input.InputManagerService;
import com.android.server.media.MediaRouterService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.wm.WindowManagerService;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE implements Runnable {
    private final /* synthetic */ SystemServer f$0;
    private final /* synthetic */ Context f$1;
    private final /* synthetic */ NetworkTimeUpdateService f$10;
    private final /* synthetic */ CommonTimeManagementService f$11;
    private final /* synthetic */ InputManagerService f$12;
    private final /* synthetic */ TelephonyRegistry f$13;
    private final /* synthetic */ MediaRouterService f$14;
    private final /* synthetic */ MmsServiceBroker f$15;
    private final /* synthetic */ boolean f$16;
    private final /* synthetic */ WindowManagerService f$2;
    private final /* synthetic */ NetworkManagementService f$3;
    private final /* synthetic */ NetworkPolicyManagerService f$4;
    private final /* synthetic */ IpSecService f$5;
    private final /* synthetic */ NetworkStatsService f$6;
    private final /* synthetic */ ConnectivityService f$7;
    private final /* synthetic */ LocationManagerService f$8;
    private final /* synthetic */ CountryDetectorService f$9;

    public /* synthetic */ -$$Lambda$SystemServer$_k2zKlZCDIXIYhAHBQHIid56pXE(SystemServer systemServer, Context context, WindowManagerService windowManagerService, NetworkManagementService networkManagementService, NetworkPolicyManagerService networkPolicyManagerService, IpSecService ipSecService, NetworkStatsService networkStatsService, ConnectivityService connectivityService, LocationManagerService locationManagerService, CountryDetectorService countryDetectorService, NetworkTimeUpdateService networkTimeUpdateService, CommonTimeManagementService commonTimeManagementService, InputManagerService inputManagerService, TelephonyRegistry telephonyRegistry, MediaRouterService mediaRouterService, MmsServiceBroker mmsServiceBroker, boolean z) {
        this.f$0 = systemServer;
        this.f$1 = context;
        this.f$2 = windowManagerService;
        this.f$3 = networkManagementService;
        this.f$4 = networkPolicyManagerService;
        this.f$5 = ipSecService;
        this.f$6 = networkStatsService;
        this.f$7 = connectivityService;
        this.f$8 = locationManagerService;
        this.f$9 = countryDetectorService;
        this.f$10 = networkTimeUpdateService;
        this.f$11 = commonTimeManagementService;
        this.f$12 = inputManagerService;
        this.f$13 = telephonyRegistry;
        this.f$14 = mediaRouterService;
        this.f$15 = mmsServiceBroker;
        this.f$16 = z;
    }

    public final void run() {
        SystemServer.lambda$startOtherServices$5(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, this.f$6, this.f$7, this.f$8, this.f$9, this.f$10, this.f$11, this.f$12, this.f$13, this.f$14, this.f$15, this.f$16);
    }
}
