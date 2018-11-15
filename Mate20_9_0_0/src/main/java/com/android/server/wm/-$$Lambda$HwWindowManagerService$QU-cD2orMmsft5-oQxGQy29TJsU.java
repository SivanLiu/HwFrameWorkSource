package com.android.server.wm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwWindowManagerService$QU-cD2orMmsft5-oQxGQy29TJsU implements Consumer {
    private final /* synthetic */ HwWindowManagerService f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$HwWindowManagerService$QU-cD2orMmsft5-oQxGQy29TJsU(HwWindowManagerService hwWindowManagerService, String str) {
        this.f$0 = hwWindowManagerService;
        this.f$1 = str;
    }

    public final void accept(Object obj) {
        HwWindowManagerService.lambda$getLayerIndex$0(this.f$0, this.f$1, (WindowState) obj);
    }
}
