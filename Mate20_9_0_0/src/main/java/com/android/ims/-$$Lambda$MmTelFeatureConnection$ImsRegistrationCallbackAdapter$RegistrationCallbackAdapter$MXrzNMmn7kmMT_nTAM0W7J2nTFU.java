package com.android.ims;

import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.stub.ImsRegistrationImplBase.Callback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$MXrzNMmn7kmMT_nTAM0W7J2nTFU implements Consumer {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ ImsReasonInfo f$1;

    public /* synthetic */ -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$MXrzNMmn7kmMT_nTAM0W7J2nTFU(int i, ImsReasonInfo imsReasonInfo) {
        this.f$0 = i;
        this.f$1 = imsReasonInfo;
    }

    public final void accept(Object obj) {
        ((Callback) obj).onTechnologyChangeFailed(this.f$0, this.f$1);
    }
}
