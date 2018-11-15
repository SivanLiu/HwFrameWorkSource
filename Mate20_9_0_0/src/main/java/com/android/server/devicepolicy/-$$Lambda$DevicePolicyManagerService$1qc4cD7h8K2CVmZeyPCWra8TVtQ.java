package com.android.server.devicepolicy;

import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DevicePolicyManagerService$1qc4cD7h8K2CVmZeyPCWra8TVtQ implements ThrowingRunnable {
    private final /* synthetic */ DevicePolicyManagerService f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$DevicePolicyManagerService$1qc4cD7h8K2CVmZeyPCWra8TVtQ(DevicePolicyManagerService devicePolicyManagerService, String str) {
        this.f$0 = devicePolicyManagerService;
        this.f$1 = str;
    }

    public final void runOrThrow() {
        this.f$0.mInjector.getAlarmManager().setTimeZone(this.f$1);
    }
}
