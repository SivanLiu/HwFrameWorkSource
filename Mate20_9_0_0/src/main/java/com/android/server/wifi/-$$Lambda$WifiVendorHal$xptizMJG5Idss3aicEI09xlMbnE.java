package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiRttController.getResponderInfoCallback;
import android.hardware.wifi.V1_0.RttResponder;
import android.hardware.wifi.V1_0.WifiStatus;
import com.android.server.wifi.WifiVendorHal.AnonymousClass3AnswerBox;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$xptizMJG5Idss3aicEI09xlMbnE implements getResponderInfoCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ AnonymousClass3AnswerBox f$1;

    public /* synthetic */ -$$Lambda$WifiVendorHal$xptizMJG5Idss3aicEI09xlMbnE(WifiVendorHal wifiVendorHal, AnonymousClass3AnswerBox anonymousClass3AnswerBox) {
        this.f$0 = wifiVendorHal;
        this.f$1 = anonymousClass3AnswerBox;
    }

    public final void onValues(WifiStatus wifiStatus, RttResponder rttResponder) {
        WifiVendorHal.lambda$getRttResponder$5(this.f$0, this.f$1, wifiStatus, rttResponder);
    }
}
