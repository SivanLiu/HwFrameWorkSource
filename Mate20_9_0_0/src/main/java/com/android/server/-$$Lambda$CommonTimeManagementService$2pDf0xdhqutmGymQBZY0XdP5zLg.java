package com.android.server;

import android.os.CommonTimeConfig.OnServerDiedListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CommonTimeManagementService$2pDf0xdhqutmGymQBZY0XdP5zLg implements OnServerDiedListener {
    private final /* synthetic */ CommonTimeManagementService f$0;

    public /* synthetic */ -$$Lambda$CommonTimeManagementService$2pDf0xdhqutmGymQBZY0XdP5zLg(CommonTimeManagementService commonTimeManagementService) {
        this.f$0 = commonTimeManagementService;
    }

    public final void onServerDied() {
        this.f$0.scheduleTimeConfigReconnect();
    }
}
