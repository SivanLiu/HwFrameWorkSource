package android.telephony.euicc;

import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$7$W9T937HBG-sD8BsVWGQ6kDb28dk implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$EuiccCardManager$7$W9T937HBG-sD8BsVWGQ6kDb28dk(ResultCallback resultCallback, int i) {
        this.f$0 = resultCallback;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, null);
    }
}
