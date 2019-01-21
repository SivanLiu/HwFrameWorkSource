package com.android.internal.telephony.ims;

import android.telephony.ims.stub.ImsFeatureConfiguration.FeatureSlotPair;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsResolver$kF808g2NWzNL8H1SwzDc1FxiQdQ implements Predicate {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$ImsResolver$kF808g2NWzNL8H1SwzDc1FxiQdQ(int i) {
        this.f$0 = i;
    }

    public final boolean test(Object obj) {
        return ImsResolver.lambda$calculateFeaturesToCreate$5(this.f$0, (FeatureSlotPair) obj);
    }
}
