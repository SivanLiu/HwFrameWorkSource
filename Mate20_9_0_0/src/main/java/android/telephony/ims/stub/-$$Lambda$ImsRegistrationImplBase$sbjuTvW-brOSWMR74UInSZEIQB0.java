package android.telephony.ims.stub;

import android.telephony.ims.aidl.IImsRegistrationCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsRegistrationImplBase$sbjuTvW-brOSWMR74UInSZEIQB0 implements Consumer {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$ImsRegistrationImplBase$sbjuTvW-brOSWMR74UInSZEIQB0(int i) {
        this.f$0 = i;
    }

    public final void accept(Object obj) {
        ImsRegistrationImplBase.lambda$onRegistering$1(this.f$0, (IImsRegistrationCallback) obj);
    }
}
