package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass8;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$8$653ymvVUxXSmc5rF5YXkbNw3yw8 implements Runnable {
    private final /* synthetic */ AnonymousClass8 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$8$653ymvVUxXSmc5rF5YXkbNw3yw8(AnonymousClass8 anonymousClass8, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
        this.f$0 = anonymousClass8;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = i;
    }

    public final void run() {
        AnonymousClass8.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
