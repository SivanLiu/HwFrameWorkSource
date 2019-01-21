package com.android.internal.telephony;

import android.telephony.SubscriptionInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SubscriptionController$tMI7DzRlXdGT29a2mf9-vcxGNO0 implements Predicate {
    private final /* synthetic */ SubscriptionController f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$SubscriptionController$tMI7DzRlXdGT29a2mf9-vcxGNO0(SubscriptionController subscriptionController, String str) {
        this.f$0 = subscriptionController;
        this.f$1 = str;
    }

    public final boolean test(Object obj) {
        return SubscriptionController.lambda$getActiveSubscriptionInfoList$1(this.f$0, this.f$1, (SubscriptionInfo) obj);
    }
}
