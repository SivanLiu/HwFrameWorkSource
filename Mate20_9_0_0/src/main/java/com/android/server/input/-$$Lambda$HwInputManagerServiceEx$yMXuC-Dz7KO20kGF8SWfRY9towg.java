package com.android.server.input;

import vendor.huawei.hardware.tp.V1_0.ITouchscreen.hwTsRunCommandCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$HwInputManagerServiceEx$yMXuC-Dz7KO20kGF8SWfRY9towg implements hwTsRunCommandCallback {
    private final /* synthetic */ HwInputManagerServiceEx f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$HwInputManagerServiceEx$yMXuC-Dz7KO20kGF8SWfRY9towg(HwInputManagerServiceEx hwInputManagerServiceEx, String str, String str2) {
        this.f$0 = hwInputManagerServiceEx;
        this.f$1 = str;
        this.f$2 = str2;
    }

    public final void onValues(int i, String str) {
        HwInputManagerServiceEx.lambda$runHwTHPCommandInternal$0(this.f$0, this.f$1, this.f$2, i, str);
    }
}
