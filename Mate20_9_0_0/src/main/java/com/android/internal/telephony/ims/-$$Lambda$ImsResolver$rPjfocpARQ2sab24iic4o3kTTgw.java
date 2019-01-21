package com.android.internal.telephony.ims;

import com.android.internal.telephony.ims.ImsResolver.ImsServiceInfo;
import java.util.Objects;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsResolver$rPjfocpARQ2sab24iic4o3kTTgw implements Predicate {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$ImsResolver$rPjfocpARQ2sab24iic4o3kTTgw(String str) {
        this.f$0 = str;
    }

    public final boolean test(Object obj) {
        return Objects.equals(((ImsServiceInfo) obj).name.getPackageName(), this.f$0);
    }
}
