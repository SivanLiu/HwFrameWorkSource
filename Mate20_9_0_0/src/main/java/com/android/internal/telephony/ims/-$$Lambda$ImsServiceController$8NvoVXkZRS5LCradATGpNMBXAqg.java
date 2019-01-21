package com.android.internal.telephony.ims;

import android.telephony.ims.stub.ImsFeatureConfiguration.FeatureSlotPair;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsServiceController$8NvoVXkZRS5LCradATGpNMBXAqg implements Predicate {
    private final /* synthetic */ FeatureSlotPair f$0;

    public /* synthetic */ -$$Lambda$ImsServiceController$8NvoVXkZRS5LCradATGpNMBXAqg(FeatureSlotPair featureSlotPair) {
        this.f$0 = featureSlotPair;
    }

    public final boolean test(Object obj) {
        return ImsServiceController.lambda$removeImsServiceFeature$0(this.f$0, (ImsFeatureStatusCallback) obj);
    }
}
