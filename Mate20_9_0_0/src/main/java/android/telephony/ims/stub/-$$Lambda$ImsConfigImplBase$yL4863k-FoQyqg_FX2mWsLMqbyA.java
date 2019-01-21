package android.telephony.ims.stub;

import android.telephony.ims.aidl.IImsConfigCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsConfigImplBase$yL4863k-FoQyqg_FX2mWsLMqbyA implements Consumer {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$ImsConfigImplBase$yL4863k-FoQyqg_FX2mWsLMqbyA(int i, int i2) {
        this.f$0 = i;
        this.f$1 = i2;
    }

    public final void accept(Object obj) {
        ImsConfigImplBase.lambda$notifyConfigChanged$0(this.f$0, this.f$1, (IImsConfigCallback) obj);
    }
}
