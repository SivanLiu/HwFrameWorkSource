package com.android.ims;

import android.telephony.ims.stub.ImsRegistrationImplBase.Callback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$u4ZBOw30LePcwafim6pu64v4hNM implements Consumer {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$u4ZBOw30LePcwafim6pu64v4hNM(int i) {
        this.f$0 = i;
    }

    public final void accept(Object obj) {
        ((Callback) obj).onRegistering(this.f$0);
    }
}
