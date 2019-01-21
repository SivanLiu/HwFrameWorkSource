package android.telephony.ims.stub;

import android.net.Uri;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsRegistrationImplBase$wwtkoeOtGwMjG5I0-ZTfjNpGU-s implements Consumer {
    private final /* synthetic */ Uri[] f$0;

    public /* synthetic */ -$$Lambda$ImsRegistrationImplBase$wwtkoeOtGwMjG5I0-ZTfjNpGU-s(Uri[] uriArr) {
        this.f$0 = uriArr;
    }

    public final void accept(Object obj) {
        ImsRegistrationImplBase.lambda$onSubscriberAssociatedUriChanged$4(this.f$0, (IImsRegistrationCallback) obj);
    }
}
