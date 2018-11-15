package android.net.ip;

import android.net.IpPrefix;
import android.net.RouteInfo;
import android.net.ip.IpClient.InitialConfiguration;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$Id7yPLmMAQz0Sm1dnrJVkXkUQNQ implements Predicate {
    private final /* synthetic */ IpPrefix f$0;

    public /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$Id7yPLmMAQz0Sm1dnrJVkXkUQNQ(IpPrefix ipPrefix) {
        this.f$0 = ipPrefix;
    }

    public final boolean test(Object obj) {
        return InitialConfiguration.isDirectlyConnectedRoute((RouteInfo) obj, this.f$0);
    }
}
