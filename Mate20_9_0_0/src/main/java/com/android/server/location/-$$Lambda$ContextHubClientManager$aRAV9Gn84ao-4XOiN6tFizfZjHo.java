package com.android.server.location;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubClientManager$aRAV9Gn84ao-4XOiN6tFizfZjHo implements Consumer {
    public static final /* synthetic */ -$$Lambda$ContextHubClientManager$aRAV9Gn84ao-4XOiN6tFizfZjHo INSTANCE = new -$$Lambda$ContextHubClientManager$aRAV9Gn84ao-4XOiN6tFizfZjHo();

    private /* synthetic */ -$$Lambda$ContextHubClientManager$aRAV9Gn84ao-4XOiN6tFizfZjHo() {
    }

    public final void accept(Object obj) {
        ((ContextHubClientBroker) obj).onHubReset();
    }
}
