package android.telephony.euicc;

import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$5$Tw9Ac3hC3rh6YoO0o4ip_fVYWq0 implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$EuiccCardManager$5$Tw9Ac3hC3rh6YoO0o4ip_fVYWq0(ResultCallback resultCallback, int i) {
        this.f$0 = resultCallback;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, null);
    }
}
