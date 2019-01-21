package com.android.internal.telephony.ims;

import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsServiceController$w3xbtqEhKr7IY81qFuw0e94p84Y implements Predicate {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$ImsServiceController$w3xbtqEhKr7IY81qFuw0e94p84Y(int i, int i2) {
        this.f$0 = i;
        this.f$1 = i2;
    }

    public final boolean test(Object obj) {
        return ImsServiceController.lambda$getImsFeatureContainer$2(this.f$0, this.f$1, (ImsFeatureContainer) obj);
    }
}
