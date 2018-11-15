package com.android.server.devicepolicy;

import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DevicePolicyManagerService$mignzFcOqIvnBFOYi8O3tmqXI68 implements ThrowingRunnable {
    private final /* synthetic */ DevicePolicyManagerService f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ int f$3;

    public /* synthetic */ -$$Lambda$DevicePolicyManagerService$mignzFcOqIvnBFOYi8O3tmqXI68(DevicePolicyManagerService devicePolicyManagerService, String str, String str2, int i) {
        this.f$0 = devicePolicyManagerService;
        this.f$1 = str;
        this.f$2 = str2;
        this.f$3 = i;
    }

    public final void runOrThrow() {
        this.f$0.mInjector.settingsSystemPutStringForUser(this.f$1, this.f$2, this.f$3);
    }
}
