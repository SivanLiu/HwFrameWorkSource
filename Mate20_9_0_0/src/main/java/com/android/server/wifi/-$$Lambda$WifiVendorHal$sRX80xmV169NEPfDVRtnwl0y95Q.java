package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiStaIface.getDebugTxPacketFatesCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import com.android.server.wifi.WifiNative.TxFateReport;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$sRX80xmV169NEPfDVRtnwl0y95Q implements getDebugTxPacketFatesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ TxFateReport[] f$1;
    private final /* synthetic */ MutableBoolean f$2;

    public /* synthetic */ -$$Lambda$WifiVendorHal$sRX80xmV169NEPfDVRtnwl0y95Q(WifiVendorHal wifiVendorHal, TxFateReport[] txFateReportArr, MutableBoolean mutableBoolean) {
        this.f$0 = wifiVendorHal;
        this.f$1 = txFateReportArr;
        this.f$2 = mutableBoolean;
    }

    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
        WifiVendorHal.lambda$getTxPktFates$12(this.f$0, this.f$1, this.f$2, wifiStatus, arrayList);
    }
}
