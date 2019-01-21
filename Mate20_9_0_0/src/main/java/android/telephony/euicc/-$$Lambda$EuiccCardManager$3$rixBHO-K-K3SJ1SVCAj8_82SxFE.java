package android.telephony.euicc;

import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$3$rixBHO-K-K3SJ1SVCAj8_82SxFE implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$EuiccCardManager$3$rixBHO-K-K3SJ1SVCAj8_82SxFE(ResultCallback resultCallback, int i) {
        this.f$0 = resultCallback;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, null);
    }
}
