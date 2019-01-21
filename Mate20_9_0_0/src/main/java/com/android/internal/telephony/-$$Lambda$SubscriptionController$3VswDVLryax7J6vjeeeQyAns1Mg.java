package com.android.internal.telephony;

import android.telephony.SubscriptionInfo;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SubscriptionController$3VswDVLryax7J6vjeeeQyAns1Mg implements Predicate {
    private final /* synthetic */ SubscriptionController f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$SubscriptionController$3VswDVLryax7J6vjeeeQyAns1Mg(SubscriptionController subscriptionController, String str) {
        this.f$0 = subscriptionController;
        this.f$1 = str;
    }

    public final boolean test(Object obj) {
        return ((SubscriptionInfo) obj).canManageSubscription(this.f$0.mContext, this.f$1);
    }
}
