package android.telephony.euicc;

import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$21$srrmNYPqPTZF4uUZIcVq86p1JpU implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ EuiccNotification f$2;

    public /* synthetic */ -$$Lambda$EuiccCardManager$21$srrmNYPqPTZF4uUZIcVq86p1JpU(ResultCallback resultCallback, int i, EuiccNotification euiccNotification) {
        this.f$0 = resultCallback;
        this.f$1 = i;
        this.f$2 = euiccNotification;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, this.f$2);
    }
}
