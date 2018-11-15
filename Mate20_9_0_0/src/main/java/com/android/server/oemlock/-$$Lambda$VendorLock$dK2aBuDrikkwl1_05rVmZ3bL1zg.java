package com.android.server.oemlock;

import android.hardware.oemlock.V1_0.IOemLock.isOemUnlockAllowedByDeviceCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$VendorLock$dK2aBuDrikkwl1_05rVmZ3bL1zg implements isOemUnlockAllowedByDeviceCallback {
    private final /* synthetic */ Integer[] f$0;
    private final /* synthetic */ Boolean[] f$1;

    public /* synthetic */ -$$Lambda$VendorLock$dK2aBuDrikkwl1_05rVmZ3bL1zg(Integer[] numArr, Boolean[] boolArr) {
        this.f$0 = numArr;
        this.f$1 = boolArr;
    }

    public final void onValues(int i, boolean z) {
        VendorLock.lambda$isOemUnlockAllowedByDevice$1(this.f$0, this.f$1, i, z);
    }
}
