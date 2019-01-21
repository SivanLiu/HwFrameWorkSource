package android.net.lowpan;

import android.net.lowpan.LowpanScanner.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanScanner$2$n8MSb22N9MEsazioSumvyQhW3Z4 implements Runnable {
    private final /* synthetic */ Callback f$0;

    public /* synthetic */ -$$Lambda$LowpanScanner$2$n8MSb22N9MEsazioSumvyQhW3Z4(Callback callback) {
        this.f$0 = callback;
    }

    public final void run() {
        this.f$0.onScanFinished();
    }
}
