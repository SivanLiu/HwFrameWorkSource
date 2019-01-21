package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass9;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$9$xm26YKGxl72UYoxSNyEMJslmmNk implements Runnable {
    private final /* synthetic */ AnonymousClass9 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$9$xm26YKGxl72UYoxSNyEMJslmmNk(AnonymousClass9 anonymousClass9, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
        this.f$0 = anonymousClass9;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = i;
    }

    public final void run() {
        AnonymousClass9.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
