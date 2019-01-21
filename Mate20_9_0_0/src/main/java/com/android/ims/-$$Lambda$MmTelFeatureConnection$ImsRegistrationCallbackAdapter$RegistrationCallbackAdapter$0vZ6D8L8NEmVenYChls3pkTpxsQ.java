package com.android.ims;

import android.net.Uri;
import android.telephony.ims.stub.ImsRegistrationImplBase.Callback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$0vZ6D8L8NEmVenYChls3pkTpxsQ implements Consumer {
    private final /* synthetic */ Uri[] f$0;

    public /* synthetic */ -$$Lambda$MmTelFeatureConnection$ImsRegistrationCallbackAdapter$RegistrationCallbackAdapter$0vZ6D8L8NEmVenYChls3pkTpxsQ(Uri[] uriArr) {
        this.f$0 = uriArr;
    }

    public final void accept(Object obj) {
        ((Callback) obj).onSubscriberAssociatedUriChanged(this.f$0);
    }
}
