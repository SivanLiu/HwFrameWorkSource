package com.android.server.connectivity;

import android.net.metrics.ConnectStats;
import java.util.function.Function;

final /* synthetic */ class -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY implements Function {
    private final /* synthetic */ Object $m$0(Object arg0) {
        return IpConnectivityEventBuilder.toProto((ConnectStats) arg0);
    }

    public final Object apply(Object obj) {
        return $m$0(obj);
    }
}
