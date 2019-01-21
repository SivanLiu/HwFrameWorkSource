package android.telephony;

import android.telephony.TelephonyScanManager.AnonymousClass1;
import android.telephony.TelephonyScanManager.NetworkScanCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TelephonyScanManager$1$X9SMshZoHjJ6SzCbmgVMwQip2Q0 implements Runnable {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ NetworkScanCallback f$1;

    public /* synthetic */ -$$Lambda$TelephonyScanManager$1$X9SMshZoHjJ6SzCbmgVMwQip2Q0(int i, NetworkScanCallback networkScanCallback) {
        this.f$0 = i;
        this.f$1 = networkScanCallback;
    }

    public final void run() {
        AnonymousClass1.lambda$handleMessage$1(this.f$0, this.f$1);
    }
}
