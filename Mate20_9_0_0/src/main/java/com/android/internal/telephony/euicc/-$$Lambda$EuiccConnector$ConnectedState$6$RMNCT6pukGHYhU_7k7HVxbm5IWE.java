package com.android.internal.telephony.euicc;

import android.telephony.euicc.EuiccInfo;
import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass6;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$6$RMNCT6pukGHYhU_7k7HVxbm5IWE implements Runnable {
    private final /* synthetic */ AnonymousClass6 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ EuiccInfo f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$6$RMNCT6pukGHYhU_7k7HVxbm5IWE(AnonymousClass6 anonymousClass6, BaseEuiccCommandCallback baseEuiccCommandCallback, EuiccInfo euiccInfo) {
        this.f$0 = anonymousClass6;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = euiccInfo;
    }

    public final void run() {
        AnonymousClass6.lambda$onSuccess$0(this.f$0, this.f$1, this.f$2);
    }
}
