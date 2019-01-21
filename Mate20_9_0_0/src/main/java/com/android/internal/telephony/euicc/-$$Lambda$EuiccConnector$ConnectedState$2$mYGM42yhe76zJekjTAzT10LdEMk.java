package com.android.internal.telephony.euicc;

import android.service.euicc.GetDownloadableSubscriptionMetadataResult;
import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass2;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$2$mYGM42yhe76zJekjTAzT10LdEMk implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ GetDownloadableSubscriptionMetadataResult f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$2$mYGM42yhe76zJekjTAzT10LdEMk(AnonymousClass2 anonymousClass2, BaseEuiccCommandCallback baseEuiccCommandCallback, GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult) {
        this.f$0 = anonymousClass2;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = getDownloadableSubscriptionMetadataResult;
    }

    public final void run() {
        AnonymousClass2.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
