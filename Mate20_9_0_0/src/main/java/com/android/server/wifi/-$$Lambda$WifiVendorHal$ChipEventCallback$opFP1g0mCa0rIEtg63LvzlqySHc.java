package com.android.server.wifi;

import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiVendorHal$ChipEventCallback$opFP1g0mCa0rIEtg63LvzlqySHc implements Runnable {
    private final /* synthetic */ ChipEventCallback f$0;
    private final /* synthetic */ ArrayList f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$WifiVendorHal$ChipEventCallback$opFP1g0mCa0rIEtg63LvzlqySHc(ChipEventCallback chipEventCallback, ArrayList arrayList, int i) {
        this.f$0 = chipEventCallback;
        this.f$1 = arrayList;
        this.f$2 = i;
    }

    public final void run() {
        ChipEventCallback.lambda$onDebugErrorAlert$1(this.f$0, this.f$1, this.f$2);
    }
}
