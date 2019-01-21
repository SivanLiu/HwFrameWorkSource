package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass12;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$12$wYal9P4llN7g9YAk_zACL8m3nS0 implements Runnable {
    private final /* synthetic */ AnonymousClass12 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$12$wYal9P4llN7g9YAk_zACL8m3nS0(AnonymousClass12 anonymousClass12, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
        this.f$0 = anonymousClass12;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = i;
    }

    public final void run() {
        AnonymousClass12.lambda$onSuccess$0(this.f$0, this.f$1, this.f$2);
    }
}
