package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiChip.requestFirmwareDebugDumpCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass7AnswerBox;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$0nn1d2XVTxIXDSyzfYz5nuiMmaM implements requestFirmwareDebugDumpCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass7AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$0nn1d2XVTxIXDSyzfYz5nuiMmaM(WifiVendorHal wifiVendorHal, AnonymousClass7AnswerBox anonymousClass7AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass7AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
        WifiVendorHal.lambda$getFwMemoryDump$10(this.f$0, this.f$1, wifiStatus, arrayList);
    }
}
