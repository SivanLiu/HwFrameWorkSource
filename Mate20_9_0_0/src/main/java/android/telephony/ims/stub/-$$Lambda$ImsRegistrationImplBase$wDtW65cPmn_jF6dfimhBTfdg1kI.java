package android.telephony.ims.stub;

import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.aidl.IImsRegistrationCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsRegistrationImplBase$wDtW65cPmn_jF6dfimhBTfdg1kI implements Consumer {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ ImsReasonInfo f$1;

    public /* synthetic */ -$$Lambda$ImsRegistrationImplBase$wDtW65cPmn_jF6dfimhBTfdg1kI(int i, ImsReasonInfo imsReasonInfo) {
        this.f$0 = i;
        this.f$1 = imsReasonInfo;
    }

    public final void accept(Object obj) {
        ImsRegistrationImplBase.lambda$onTechnologyChangeFailed$3(this.f$0, this.f$1, (IImsRegistrationCallback) obj);
    }
}
