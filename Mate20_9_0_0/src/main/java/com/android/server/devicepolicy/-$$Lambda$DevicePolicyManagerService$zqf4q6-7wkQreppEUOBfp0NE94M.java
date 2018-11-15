package com.android.server.devicepolicy;

import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DevicePolicyManagerService$zqf4q6-7wkQreppEUOBfp0NE94M implements ThrowingRunnable {
    private final /* synthetic */ DevicePolicyManagerService f$0;
    private final /* synthetic */ long f$1;

    public /* synthetic */ -$$Lambda$DevicePolicyManagerService$zqf4q6-7wkQreppEUOBfp0NE94M(DevicePolicyManagerService devicePolicyManagerService, long j) {
        this.f$0 = devicePolicyManagerService;
        this.f$1 = j;
    }

    public final void runOrThrow() {
        this.f$0.mInjector.getAlarmManager().setTime(this.f$1);
    }
}
