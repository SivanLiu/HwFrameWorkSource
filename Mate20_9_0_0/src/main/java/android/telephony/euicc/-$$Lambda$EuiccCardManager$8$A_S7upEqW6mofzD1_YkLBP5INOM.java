package android.telephony.euicc;

import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$8$A_S7upEqW6mofzD1_YkLBP5INOM implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$EuiccCardManager$8$A_S7upEqW6mofzD1_YkLBP5INOM(ResultCallback resultCallback, int i, String str) {
        this.f$0 = resultCallback;
        this.f$1 = i;
        this.f$2 = str;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, this.f$2);
    }
}
