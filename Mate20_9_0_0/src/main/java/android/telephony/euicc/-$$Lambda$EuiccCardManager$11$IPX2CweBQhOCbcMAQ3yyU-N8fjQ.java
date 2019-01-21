package android.telephony.euicc;

import android.telephony.euicc.EuiccCardManager.ResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCardManager$11$IPX2CweBQhOCbcMAQ3yyU-N8fjQ implements Runnable {
    private final /* synthetic */ ResultCallback f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ EuiccRulesAuthTable f$2;

    public /* synthetic */ -$$Lambda$EuiccCardManager$11$IPX2CweBQhOCbcMAQ3yyU-N8fjQ(ResultCallback resultCallback, int i, EuiccRulesAuthTable euiccRulesAuthTable) {
        this.f$0 = resultCallback;
        this.f$1 = i;
        this.f$2 = euiccRulesAuthTable;
    }

    public final void run() {
        this.f$0.onComplete(this.f$1, this.f$2);
    }
}
