package com.android.server.devicepolicy;

import com.android.internal.telephony.SmsApplication;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DevicePolicyManagerService$dDeS1FUetDCbtT673Qp0Hcsm5Vw implements ThrowingRunnable {
    private final /* synthetic */ DevicePolicyManagerService f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$DevicePolicyManagerService$dDeS1FUetDCbtT673Qp0Hcsm5Vw(DevicePolicyManagerService devicePolicyManagerService, String str) {
        this.f$0 = devicePolicyManagerService;
        this.f$1 = str;
    }

    public final void runOrThrow() {
        SmsApplication.setDefaultApplication(this.f$1, this.f$0.mContext);
    }
}
