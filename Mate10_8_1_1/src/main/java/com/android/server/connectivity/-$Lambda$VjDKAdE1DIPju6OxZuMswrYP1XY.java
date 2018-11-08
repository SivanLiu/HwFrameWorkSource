package com.android.server.connectivity;

import android.net.metrics.ConnectStats;
import android.net.metrics.DnsEvent;
import java.util.function.Function;

final /* synthetic */ class -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY implements Function {
    public static final /* synthetic */ -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY $INST$0 = new -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY((byte) 0);
    public static final /* synthetic */ -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY $INST$1 = new -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY((byte) 1);
    public static final /* synthetic */ -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY $INST$2 = new -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY((byte) 2);
    public static final /* synthetic */ -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY $INST$3 = new -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY((byte) 3);
    public static final /* synthetic */ -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY $INST$4 = new -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY((byte) 4);
    public static final /* synthetic */ -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY $INST$5 = new -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY((byte) 5);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ Object $m$0(Object arg0) {
        return IpConnectivityEventBuilder.toProto((ConnectStats) arg0);
    }

    private final /* synthetic */ Object $m$1(Object arg0) {
        return IpConnectivityEventBuilder.toProto((DnsEvent) arg0);
    }

    private final /* synthetic */ Object $m$2(Object arg0) {
        return NetdEventListenerService.lambda$-com_android_server_connectivity_NetdEventListenerService_9766((ConnectStats) arg0);
    }

    private final /* synthetic */ Object $m$3(Object arg0) {
        return NetdEventListenerService.lambda$-com_android_server_connectivity_NetdEventListenerService_9818((DnsEvent) arg0);
    }

    private final /* synthetic */ Object $m$4(Object arg0) {
        return IpConnectivityEventBuilder.toProto((ConnectStats) arg0);
    }

    private final /* synthetic */ Object $m$5(Object arg0) {
        return IpConnectivityEventBuilder.toProto((DnsEvent) arg0);
    }

    private /* synthetic */ -$Lambda$VjDKAdE1DIPju6OxZuMswrYP1XY(byte b) {
        this.$id = b;
    }

    public final Object apply(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            case (byte) 2:
                return $m$2(obj);
            case (byte) 3:
                return $m$3(obj);
            case (byte) 4:
                return $m$4(obj);
            case (byte) 5:
                return $m$5(obj);
            default:
                throw new AssertionError();
        }
    }
}
