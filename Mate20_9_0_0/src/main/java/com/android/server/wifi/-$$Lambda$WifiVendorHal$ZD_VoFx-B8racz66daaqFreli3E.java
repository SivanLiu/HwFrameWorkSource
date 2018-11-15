package com.android.server.wifi;

import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.V1_2.IWifiStaIface.readApfPacketFilterDataCallback;
import com.android.server.wifi.WifiVendorHal.AnonymousClass5AnswerBox;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$ZD_VoFx-B8racz66daaqFreli3E implements readApfPacketFilterDataCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass5AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$ZD_VoFx-B8racz66daaqFreli3E(WifiVendorHal wifiVendorHal, AnonymousClass5AnswerBox anonymousClass5AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass5AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
        WifiVendorHal.lambda$readPacketFilter$7(this.f$0, this.f$1, wifiStatus, arrayList);
    }
}
