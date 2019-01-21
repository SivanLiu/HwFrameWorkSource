package com.android.internal.telephony.euicc;

import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.OtaStatusChangedCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$13$5nh8TOHvAdIIa_S3V0gwsRICKC4 implements Runnable {
    private final /* synthetic */ BaseEuiccCommandCallback f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$13$5nh8TOHvAdIIa_S3V0gwsRICKC4(BaseEuiccCommandCallback baseEuiccCommandCallback, int i) {
        this.f$0 = baseEuiccCommandCallback;
        this.f$1 = i;
    }

    public final void run() {
        ((OtaStatusChangedCallback) this.f$0).onOtaStatusChanged(this.f$1);
    }
}
