package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass3;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$3$kCYyTG6MMZu-1yQLS6p1_Mk7KM8 implements Runnable {
    private final /* synthetic */ AnonymousClass3 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$3$kCYyTG6MMZu-1yQLS6p1_Mk7KM8(AnonymousClass3 anonymousClass3, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
        this.f$0 = anonymousClass3;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = i;
    }

    public final void run() {
        AnonymousClass3.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
