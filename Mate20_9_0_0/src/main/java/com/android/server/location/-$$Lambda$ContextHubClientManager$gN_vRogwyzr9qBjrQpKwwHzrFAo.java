package com.android.server.location;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubClientManager$gN_vRogwyzr9qBjrQpKwwHzrFAo implements Consumer {
    private final /* synthetic */ long f$0;

    public /* synthetic */ -$$Lambda$ContextHubClientManager$gN_vRogwyzr9qBjrQpKwwHzrFAo(long j) {
        this.f$0 = j;
    }

    public final void accept(Object obj) {
        ((ContextHubClientBroker) obj).onNanoAppUnloaded(this.f$0);
    }
}
