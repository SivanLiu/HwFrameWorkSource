package android.telephony.ims.stub;

import android.telephony.ims.aidl.IImsConfigCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsConfigImplBase$GAuYvQ8qBc7KgCJhNp4Pt4j5t-0 implements Consumer {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$ImsConfigImplBase$GAuYvQ8qBc7KgCJhNp4Pt4j5t-0(int i, String str) {
        this.f$0 = i;
        this.f$1 = str;
    }

    public final void accept(Object obj) {
        ImsConfigImplBase.lambda$notifyConfigChanged$1(this.f$0, this.f$1, (IImsConfigCallback) obj);
    }
}
