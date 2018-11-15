package com.android.server.net.watchlist;

import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WatchlistLoggingHandler$GBD0dX6RhipHIkM0Z_B5jLlwfHQ implements Function {
    private final /* synthetic */ WatchlistLoggingHandler f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$WatchlistLoggingHandler$GBD0dX6RhipHIkM0Z_B5jLlwfHQ(WatchlistLoggingHandler watchlistLoggingHandler, int i) {
        this.f$0 = watchlistLoggingHandler;
        this.f$1 = i;
    }

    public final Object apply(Object obj) {
        return WatchlistLoggingHandler.lambda$getDigestFromUid$0(this.f$0, this.f$1, (Integer) obj);
    }
}
