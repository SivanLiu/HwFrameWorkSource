package com.android.internal.telephony.euicc;

import android.service.euicc.GetEuiccProfileInfoListResult;
import com.android.internal.telephony.euicc.EuiccConnector.BaseEuiccCommandCallback;
import com.android.internal.telephony.euicc.EuiccConnector.ConnectedState.AnonymousClass4;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccConnector$ConnectedState$4$S52i3hpE3-FGho807KZ1LR5rXQM implements Runnable {
    private final /* synthetic */ AnonymousClass4 f$0;
    private final /* synthetic */ BaseEuiccCommandCallback f$1;
    private final /* synthetic */ GetEuiccProfileInfoListResult f$2;

    public /* synthetic */ -$$Lambda$EuiccConnector$ConnectedState$4$S52i3hpE3-FGho807KZ1LR5rXQM(AnonymousClass4 anonymousClass4, BaseEuiccCommandCallback baseEuiccCommandCallback, GetEuiccProfileInfoListResult getEuiccProfileInfoListResult) {
        this.f$0 = anonymousClass4;
        this.f$1 = baseEuiccCommandCallback;
        this.f$2 = getEuiccProfileInfoListResult;
    }

    public final void run() {
        AnonymousClass4.lambda$onComplete$0(this.f$0, this.f$1, this.f$2);
    }
}
