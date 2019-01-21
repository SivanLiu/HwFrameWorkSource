package com.android.internal.telephony.ims;

import android.telephony.ims.stub.ImsFeatureConfiguration.FeatureSlotPair;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsResolver$-jFhgP_NotuFSwzjQBXWuvls4x4 implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$ImsResolver$-jFhgP_NotuFSwzjQBXWuvls4x4(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return ImsResolver.lambda$calculateFeaturesToCreate$3(this.f$0, (FeatureSlotPair) obj);
    }
}
