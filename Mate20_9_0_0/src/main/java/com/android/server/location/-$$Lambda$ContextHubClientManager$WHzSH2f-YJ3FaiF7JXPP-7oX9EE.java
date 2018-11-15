package com.android.server.location;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubClientManager$WHzSH2f-YJ3FaiF7JXPP-7oX9EE implements Consumer {
    private final /* synthetic */ long f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$ContextHubClientManager$WHzSH2f-YJ3FaiF7JXPP-7oX9EE(long j, int i) {
        this.f$0 = j;
        this.f$1 = i;
    }

    public final void accept(Object obj) {
        ((ContextHubClientBroker) obj).onNanoAppAborted(this.f$0, this.f$1);
    }
}
