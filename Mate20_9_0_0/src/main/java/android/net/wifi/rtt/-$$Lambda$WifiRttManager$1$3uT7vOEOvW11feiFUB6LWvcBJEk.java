package android.net.wifi.rtt;

import java.util.List;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiRttManager$1$3uT7vOEOvW11feiFUB6LWvcBJEk implements Runnable {
    private final /* synthetic */ RangingResultCallback f$0;
    private final /* synthetic */ List f$1;

    public /* synthetic */ -$$Lambda$WifiRttManager$1$3uT7vOEOvW11feiFUB6LWvcBJEk(RangingResultCallback rangingResultCallback, List list) {
        this.f$0 = rangingResultCallback;
        this.f$1 = list;
    }

    public final void run() {
        this.f$0.onRangingResults(this.f$1);
    }
}
