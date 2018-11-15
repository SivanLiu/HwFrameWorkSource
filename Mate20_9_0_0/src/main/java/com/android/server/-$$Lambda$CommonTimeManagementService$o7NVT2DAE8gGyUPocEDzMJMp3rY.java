package com.android.server;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CommonTimeManagementService$o7NVT2DAE8gGyUPocEDzMJMp3rY implements Runnable {
    private final /* synthetic */ CommonTimeManagementService f$0;

    public /* synthetic */ -$$Lambda$CommonTimeManagementService$o7NVT2DAE8gGyUPocEDzMJMp3rY(CommonTimeManagementService commonTimeManagementService) {
        this.f$0 = commonTimeManagementService;
    }

    public final void run() {
        this.f$0.connectToTimeConfig();
    }
}
