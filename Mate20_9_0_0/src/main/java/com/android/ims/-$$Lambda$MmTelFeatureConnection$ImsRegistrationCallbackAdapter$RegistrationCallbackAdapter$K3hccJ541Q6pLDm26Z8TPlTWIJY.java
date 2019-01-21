package com.android.ims;

import android.telephony.ims.stub.ImsRegistrationImplBase.Callback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$K3hccJ541Q6pLDm26Z8TPlTWIJY implements Consumer {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$K3hccJ541Q6pLDm26Z8TPlTWIJY(int i) {
        this.f$0 = i;
    }

    public final void accept(Object obj) {
        ((Callback) obj).onRegistered(this.f$0);
    }
}
