package android.net.lowpan;

import android.net.lowpan.LowpanScanner.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanScanner$1$lUw1npYnRpaO9LS5odGyASQYaic implements Runnable {
    private final /* synthetic */ Callback f$0;

    public /* synthetic */ -$$Lambda$LowpanScanner$1$lUw1npYnRpaO9LS5odGyASQYaic(Callback callback) {
        this.f$0 = callback;
    }

    public final void run() {
        this.f$0.onScanFinished();
    }
}
