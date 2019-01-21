package com.android.internal.telephony.ims;

import com.android.internal.telephony.ims.ImsResolver.ImsServiceInfo;
import java.util.Objects;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsResolver$aWLlEvfonhYSfDR8cVsM6A5pmqI implements Predicate {
    private final /* synthetic */ ImsServiceInfo f$0;

    public /* synthetic */ -$$Lambda$ImsResolver$aWLlEvfonhYSfDR8cVsM6A5pmqI(ImsServiceInfo imsServiceInfo) {
        this.f$0 = imsServiceInfo;
    }

    public final boolean test(Object obj) {
        return Objects.equals(((ImsServiceController) obj).getComponentName(), this.f$0.name);
    }
}
