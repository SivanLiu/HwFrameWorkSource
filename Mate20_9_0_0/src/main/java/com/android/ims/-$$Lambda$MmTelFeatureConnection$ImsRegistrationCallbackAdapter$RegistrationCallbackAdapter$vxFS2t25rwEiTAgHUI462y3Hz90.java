package com.android.ims;

import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.stub.ImsRegistrationImplBase.Callback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$vxFS2t25rwEiTAgHUI462y3Hz90 implements Consumer {
    private final /* synthetic */ ImsReasonInfo f$0;

    public /* synthetic */ -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$vxFS2t25rwEiTAgHUI462y3Hz90(ImsReasonInfo imsReasonInfo) {
        this.f$0 = imsReasonInfo;
    }

    public final void accept(Object obj) {
        ((Callback) obj).onDeregistered(this.f$0);
    }
}
