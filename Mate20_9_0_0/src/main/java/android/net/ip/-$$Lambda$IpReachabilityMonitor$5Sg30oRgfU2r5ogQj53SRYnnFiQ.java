package android.net.ip;

import android.net.ip.IpNeighborMonitor.NeighborEvent;
import android.net.ip.IpNeighborMonitor.NeighborEventConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpReachabilityMonitor$5Sg30oRgfU2r5ogQj53SRYnnFiQ implements NeighborEventConsumer {
    private final /* synthetic */ IpReachabilityMonitor f$0;

    public /* synthetic */ -$$Lambda$IpReachabilityMonitor$5Sg30oRgfU2r5ogQj53SRYnnFiQ(IpReachabilityMonitor ipReachabilityMonitor) {
        this.f$0 = ipReachabilityMonitor;
    }

    public final void accept(NeighborEvent neighborEvent) {
        IpReachabilityMonitor.lambda$new$0(this.f$0, neighborEvent);
    }
}
