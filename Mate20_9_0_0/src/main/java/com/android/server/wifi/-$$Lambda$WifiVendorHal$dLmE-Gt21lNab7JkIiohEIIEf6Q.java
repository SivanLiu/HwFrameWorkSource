package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.getDebugRingBuffersStatusCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass6AnswerBox;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$dLmE-Gt21lNab7JkIiohEIIEf6Q implements getDebugRingBuffersStatusCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass6AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$dLmE-Gt21lNab7JkIiohEIIEf6Q(WifiVendorHal wifiVendorHal, AnonymousClass6AnswerBox anonymousClass6AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass6AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
        WifiVendorHal.lambda$getRingBufferStatus$9(this.f$0, this.f$1, wifiStatus, arrayList);
    }
}
