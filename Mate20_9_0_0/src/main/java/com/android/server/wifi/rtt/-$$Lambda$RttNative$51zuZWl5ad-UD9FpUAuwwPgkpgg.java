package com.android.server.wifi.rtt;

import com.android.server.wifi.HalDeviceManager.ManagerStatusListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RttNative$51zuZWl5ad-UD9FpUAuwwPgkpgg implements ManagerStatusListener {
    private final /* synthetic */ RttNative f$0;

    public /* synthetic */ -$$Lambda$RttNative$51zuZWl5ad-UD9FpUAuwwPgkpgg(RttNative rttNative) {
        this.f$0 = rttNative;
    }

    public final void onStatusChanged() {
        this.f$0.updateController();
    }
}
