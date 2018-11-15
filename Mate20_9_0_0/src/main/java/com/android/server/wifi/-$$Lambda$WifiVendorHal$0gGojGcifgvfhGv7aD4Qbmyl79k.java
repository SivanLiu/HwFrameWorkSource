package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiStaIface.getDebugRxPacketFatesCallback;
import android.hardware.wifi.V1_0.WifiStatus;
import android.util.MutableBoolean;
import com.android.server.wifi.WifiNative.RxFateReport;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$0gGojGcifgvfhGv7aD4Qbmyl79k implements getDebugRxPacketFatesCallback {
    private final /* synthetic */ WifiVendorHal f$0;
    private final /* synthetic */ RxFateReport[] f$1;
    private final /* synthetic */ MutableBoolean f$2;

    public /* synthetic */ -$$Lambda$WifiVendorHal$0gGojGcifgvfhGv7aD4Qbmyl79k(WifiVendorHal wifiVendorHal, RxFateReport[] rxFateReportArr, MutableBoolean mutableBoolean) {
        this.f$0 = wifiVendorHal;
        this.f$1 = rxFateReportArr;
        this.f$2 = mutableBoolean;
    }

    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
        WifiVendorHal.lambda$getRxPktFates$13(this.f$0, this.f$1, this.f$2, wifiStatus, arrayList);
    }
}
