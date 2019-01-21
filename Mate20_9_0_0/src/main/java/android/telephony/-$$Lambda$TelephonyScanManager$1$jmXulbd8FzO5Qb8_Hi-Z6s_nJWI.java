package android.telephony;

import android.telephony.TelephonyScanManager.AnonymousClass1;
import android.telephony.TelephonyScanManager.NetworkScanCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TelephonyScanManager$1$jmXulbd8FzO5Qb8_Hi-Z6s_nJWI implements Runnable {
    private final /* synthetic */ CellInfo[] f$0;
    private final /* synthetic */ NetworkScanCallback f$1;

    public /* synthetic */ -$$Lambda$TelephonyScanManager$1$jmXulbd8FzO5Qb8_Hi-Z6s_nJWI(CellInfo[] cellInfoArr, NetworkScanCallback networkScanCallback) {
        this.f$0 = cellInfoArr;
        this.f$1 = networkScanCallback;
    }

    public final void run() {
        AnonymousClass1.lambda$handleMessage$0(this.f$0, this.f$1);
    }
}
