package android.telephony.ims.stub;

import android.telephony.ims.aidl.IImsRegistrationCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsRegistrationImplBase$cWwTXSDsk-bWPbsDJYI--DUBMnE implements Consumer {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$ImsRegistrationImplBase$cWwTXSDsk-bWPbsDJYI--DUBMnE(int i) {
        this.f$0 = i;
    }

    public final void accept(Object obj) {
        ImsRegistrationImplBase.lambda$onRegistered$0(this.f$0, (IImsRegistrationCallback) obj);
    }
}
