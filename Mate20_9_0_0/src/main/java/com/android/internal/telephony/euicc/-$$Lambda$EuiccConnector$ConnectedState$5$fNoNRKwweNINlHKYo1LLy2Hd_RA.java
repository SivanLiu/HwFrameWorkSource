package com.android.internal.telephony.euicc;

import android.service.euicc.GetDefaultDownloadableSubscriptionListResult;
import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass5;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$5$fNoNRKwweNINlHKYo1LLy2Hd_RA implements Runnable {
    private final /* synthetic */ AnonymousClass5 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ GetDefaultDownloadableSubscriptionListResult f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$5$fNoNRKwweNINlHKYo1LLy2Hd_RA(AnonymousClass5 anonymousClass5, BaseEuiccCommandCallback baseEuiccCommandCallback, GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult) {
        this.f$0 = anonymousClass5;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = getDefaultDownloadableSubscriptionListResult;
    }

    public final void run() {
        AnonymousClass5.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
