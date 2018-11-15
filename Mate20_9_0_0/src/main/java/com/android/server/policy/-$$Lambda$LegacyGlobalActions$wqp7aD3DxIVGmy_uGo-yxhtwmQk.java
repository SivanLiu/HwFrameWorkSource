package com.android.server.policy;

import java.util.function.BooleanSupplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LegacyGlobalActions$wqp7aD3DxIVGmy_uGo-yxhtwmQk implements BooleanSupplier {
    private final /* synthetic */ LegacyGlobalActions f$0;

    public /* synthetic */ -$$Lambda$LegacyGlobalActions$wqp7aD3DxIVGmy_uGo-yxhtwmQk(LegacyGlobalActions legacyGlobalActions) {
        this.f$0 = legacyGlobalActions;
    }

    public final boolean getAsBoolean() {
        return this.f$0.mDeviceProvisioned;
    }
}
