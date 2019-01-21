package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass7;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$7$-Ogvr7PIASwQa0kQAqAyfdEKAG4 implements Runnable {
    private final /* synthetic */ AnonymousClass7 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$7$-Ogvr7PIASwQa0kQAqAyfdEKAG4(AnonymousClass7 anonymousClass7, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
        this.f$0 = anonymousClass7;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = i;
    }

    public final void run() {
        AnonymousClass7.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
