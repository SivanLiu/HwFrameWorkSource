package com.android.server.oemlock;

import android.hardware.oemlock.V1_0.IOemLock.isOemUnlockAllowedByCarrierCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$VendorLock$Xnx-_jv8ufdo_3b8_MqM0reCecE implements isOemUnlockAllowedByCarrierCallback {
    private final /* synthetic */ Integer[] f$0;
    private final /* synthetic */ Boolean[] f$1;

    public /* synthetic */ -$$Lambda$VendorLock$Xnx-_jv8ufdo_3b8_MqM0reCecE(Integer[] numArr, Boolean[] boolArr) {
        this.f$0 = numArr;
        this.f$1 = boolArr;
    }

    public final void onValues(int i, boolean z) {
        VendorLock.lambda$isOemUnlockAllowedByCarrier$0(this.f$0, this.f$1, i, z);
    }
}
