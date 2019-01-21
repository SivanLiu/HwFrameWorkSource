package android.telephony;

import android.telephony.TelephonyScanManager.AnonymousClass1;
import android.telephony.TelephonyScanManager.NetworkScanCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TelephonyScanManager$1$tGSpVQaVhc4GKIxjcECV-jCGYw4 implements Runnable {
    private final /* synthetic */ NetworkScanCallback f$0;

    public /* synthetic */ -$$Lambda$TelephonyScanManager$1$tGSpVQaVhc4GKIxjcECV-jCGYw4(NetworkScanCallback networkScanCallback) {
        this.f$0 = networkScanCallback;
    }

    public final void run() {
        AnonymousClass1.lambda$handleMessage$2(this.f$0);
    }
}
