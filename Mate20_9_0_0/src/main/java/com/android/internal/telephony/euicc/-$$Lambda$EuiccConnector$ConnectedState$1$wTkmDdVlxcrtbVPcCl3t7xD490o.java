package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass1;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$1$wTkmDdVlxcrtbVPcCl3t7xD490o implements Runnable {
    private final /* synthetic */ AnonymousClass1 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$1$wTkmDdVlxcrtbVPcCl3t7xD490o(AnonymousClass1 anonymousClass1, BaseEuiccCommandCallback baseEuiccCommandCallback, String str) {
        this.f$0 = anonymousClass1;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = str;
    }

    public final void run() {
        AnonymousClass1.lambda$onSuccess$0(this.f$0, this.f$1, this.f$2);
    }
}
