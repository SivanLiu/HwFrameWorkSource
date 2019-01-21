package com.android.ims;

import android.os.IBinder.DeathRecipient;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MmTelFeatureConnection$ij8S4RNRiQPHfppwkejp36BG78I implements DeathRecipient {
    private final /* synthetic */ MmTelFeatureConnection f$0;

    public /* synthetic */ -$$Lambda$MmTelFeatureConnection$ij8S4RNRiQPHfppwkejp36BG78I(MmTelFeatureConnection mmTelFeatureConnection) {
        this.f$0 = mmTelFeatureConnection;
    }

    public final void binderDied() {
        MmTelFeatureConnection.lambda$new$0(this.f$0);
    }
}
