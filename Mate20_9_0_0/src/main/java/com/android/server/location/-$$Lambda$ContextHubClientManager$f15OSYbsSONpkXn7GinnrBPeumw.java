package com.android.server.location;

import android.hardware.location.NanoAppMessage;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubClientManager$f15OSYbsSONpkXn7GinnrBPeumw implements Consumer {
    private final /* synthetic */ NanoAppMessage f$0;

    public /* synthetic */ -$$Lambda$ContextHubClientManager$f15OSYbsSONpkXn7GinnrBPeumw(NanoAppMessage nanoAppMessage) {
        this.f$0 = nanoAppMessage;
    }

    public final void accept(Object obj) {
        ((ContextHubClientBroker) obj).sendMessageToClient(this.f$0);
    }
}
