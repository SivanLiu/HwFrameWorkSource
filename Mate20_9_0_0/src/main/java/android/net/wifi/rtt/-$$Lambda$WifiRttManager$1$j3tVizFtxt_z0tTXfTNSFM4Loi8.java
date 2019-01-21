package android.net.wifi.rtt;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiRttManager$1$j3tVizFtxt_z0tTXfTNSFM4Loi8 implements Runnable {
    private final /* synthetic */ RangingResultCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$WifiRttManager$1$j3tVizFtxt_z0tTXfTNSFM4Loi8(RangingResultCallback rangingResultCallback, int i) {
        this.f$0 = rangingResultCallback;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.onRangingFailure(this.f$1);
    }
}
