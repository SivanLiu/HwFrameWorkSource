package com.android.server.locksettings;

import android.hardware.weaver.V1_0.IWeaver.getConfigCallback;
import android.hardware.weaver.V1_0.WeaverConfig;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SyntheticPasswordManager$WjMV-qfQ1YUbeAiLzyAhyepqPFI implements getConfigCallback {
    private final /* synthetic */ SyntheticPasswordManager f$0;

    public /* synthetic */ -$$Lambda$SyntheticPasswordManager$WjMV-qfQ1YUbeAiLzyAhyepqPFI(SyntheticPasswordManager syntheticPasswordManager) {
        this.f$0 = syntheticPasswordManager;
    }

    public final void onValues(int i, WeaverConfig weaverConfig) {
        SyntheticPasswordManager.lambda$initWeaverService$0(this.f$0, i, weaverConfig);
    }
}
