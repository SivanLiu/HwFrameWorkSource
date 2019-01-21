package android.net.lowpan;

import android.net.lowpan.LowpanScanner.AnonymousClass2;
import android.net.lowpan.LowpanScanner.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanScanner$2$GBDCgjndr24KQueHMX2qGNtfLPg implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$LowpanScanner$2$GBDCgjndr24KQueHMX2qGNtfLPg(Callback callback, int i, int i2) {
        this.f$0 = callback;
        this.f$1 = i;
        this.f$2 = i2;
    }

    public final void run() {
        AnonymousClass2.lambda$onEnergyScanResult$0(this.f$0, this.f$1, this.f$2);
    }
}
