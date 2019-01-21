package com.android.ims;

import android.telephony.ims.feature.ImsFeature.Capabilities;
import android.telephony.ims.feature.ImsFeature.CapabilityCallback;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$MmTelFeatureConnection$CapabilityCallbackManager$CapabilityCallbackAdapter$Fu_TJxPrz_icRRAcE-hESmVfVRI implements Consumer {
    private final /* synthetic */ Capabilities f$0;

    public /* synthetic */ -$$Lambda$MmTelFeatureConnection$CapabilityCallbackManager$CapabilityCallbackAdapter$Fu_TJxPrz_icRRAcE-hESmVfVRI(Capabilities capabilities) {
        this.f$0 = capabilities;
    }

    public final void accept(Object obj) {
        ((CapabilityCallback) obj).onCapabilitiesStatusChanged(this.f$0);
    }
}
