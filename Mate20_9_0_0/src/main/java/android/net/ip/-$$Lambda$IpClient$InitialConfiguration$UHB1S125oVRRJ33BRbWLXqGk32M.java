package android.net.ip;

import android.net.IpPrefix;
import android.net.LinkAddress;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$UHB1S125oVRRJ33BRbWLXqGk32M implements Predicate {
    private final /* synthetic */ LinkAddress f$0;

    public /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$UHB1S125oVRRJ33BRbWLXqGk32M(LinkAddress linkAddress) {
        this.f$0 = linkAddress;
    }

    public final boolean test(Object obj) {
        return ((IpPrefix) obj).contains(this.f$0.getAddress());
    }
}
