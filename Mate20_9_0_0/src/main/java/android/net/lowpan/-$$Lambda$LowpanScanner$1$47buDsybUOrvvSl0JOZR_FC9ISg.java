package android.net.lowpan;

import android.net.lowpan.LowpanScanner.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanScanner$1$47buDsybUOrvvSl0JOZR_FC9ISg implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ LowpanBeaconInfo f$1;

    public /* synthetic */ -$$Lambda$LowpanScanner$1$47buDsybUOrvvSl0JOZR_FC9ISg(Callback callback, LowpanBeaconInfo lowpanBeaconInfo) {
        this.f$0 = callback;
        this.f$1 = lowpanBeaconInfo;
    }

    public final void run() {
        this.f$0.onNetScanBeacon(this.f$1);
    }
}
