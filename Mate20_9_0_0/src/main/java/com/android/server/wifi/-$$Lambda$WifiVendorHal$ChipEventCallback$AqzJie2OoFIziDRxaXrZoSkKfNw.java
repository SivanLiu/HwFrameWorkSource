package com.android.server.wifi;

import android.hardware.wifi.V1_0.WifiDebugRingBufferStatus;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$ChipEventCallback$AqzJie2OoFIziDRxaXrZoSkKfNw implements Runnable {
    private final /* synthetic */ ChipEventCallback f$0;
    private final /* synthetic */ WifiDebugRingBufferStatus f$1;
    private final /* synthetic */ ArrayList f$2;

    public /* synthetic */ -$$Lambda$WifiVendorHal$ChipEventCallback$AqzJie2OoFIziDRxaXrZoSkKfNw(ChipEventCallback chipEventCallback, WifiDebugRingBufferStatus wifiDebugRingBufferStatus, ArrayList arrayList) {
        this.f$0 = chipEventCallback;
        this.f$1 = wifiDebugRingBufferStatus;
        this.f$2 = arrayList;
    }

    public final void run() {
        ChipEventCallback.lambda$onDebugRingBufferDataAvailable$0(this.f$0, this.f$1, this.f$2);
    }
}
