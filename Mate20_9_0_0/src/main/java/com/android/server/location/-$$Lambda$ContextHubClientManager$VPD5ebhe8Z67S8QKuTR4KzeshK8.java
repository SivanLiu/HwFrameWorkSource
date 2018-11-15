package com.android.server.location;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubClientManager$VPD5ebhe8Z67S8QKuTR4KzeshK8 implements Consumer {
    private final /* synthetic */ long f$0;

    public /* synthetic */ -$$Lambda$ContextHubClientManager$VPD5ebhe8Z67S8QKuTR4KzeshK8(long j) {
        this.f$0 = j;
    }

    public final void accept(Object obj) {
        ((ContextHubClientBroker) obj).onNanoAppLoaded(this.f$0);
    }
}
