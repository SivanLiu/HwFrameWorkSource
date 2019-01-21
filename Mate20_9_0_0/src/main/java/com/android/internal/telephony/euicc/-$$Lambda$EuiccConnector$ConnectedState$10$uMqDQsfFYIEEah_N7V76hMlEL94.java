package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass10;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$10$uMqDQsfFYIEEah_N7V76hMlEL94 implements Runnable {
    private final /* synthetic */ AnonymousClass10 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$10$uMqDQsfFYIEEah_N7V76hMlEL94(AnonymousClass10 anonymousClass10, BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
        this.f$0 = anonymousClass10;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = i;
    }

    public final void run() {
        AnonymousClass10.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
