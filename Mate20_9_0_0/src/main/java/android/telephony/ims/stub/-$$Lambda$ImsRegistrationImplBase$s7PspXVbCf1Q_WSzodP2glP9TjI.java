package android.telephony.ims.stub;

import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsRegistrationImplBase$s7PspXVbCf1Q_WSzodP2glP9TjI implements Consumer {
    private final /* synthetic */ ImsReasonInfo f$0;

    public /* synthetic */ -$$Lambda$ImsRegistrationImplBase$s7PspXVbCf1Q_WSzodP2glP9TjI(ImsReasonInfo imsReasonInfo) {
        this.f$0 = imsReasonInfo;
    }

    public final void accept(Object obj) {
        ImsRegistrationImplBase.lambda$onDeregistered$2(this.f$0, (IImsRegistrationCallback) obj);
    }
}
