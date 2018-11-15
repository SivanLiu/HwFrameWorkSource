package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.requestDriverDebugDumpCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass8AnswerBox;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$tzHRLpLug6A0mb6rrMUdhsh-NDU implements requestDriverDebugDumpCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass8AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$tzHRLpLug6A0mb6rrMUdhsh-NDU(WifiVendorHal wifiVendorHal, AnonymousClass8AnswerBox anonymousClass8AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass8AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
        WifiVendorHal.lambda$getDriverStateDump$11(this.f$0, this.f$1, wifiStatus, arrayList);
    }
}
