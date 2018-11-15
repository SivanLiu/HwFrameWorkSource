package android.net.ip;

import android.net.IpPrefix;
import java.net.InetAddress;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$Vb5KM9_T-c7Jv1Lqbet5CRNEyjU implements Predicate {
    private final /* synthetic */ InetAddress f$0;

    public /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$Vb5KM9_T-c7Jv1Lqbet5CRNEyjU(InetAddress inetAddress) {
        this.f$0 = inetAddress;
    }

    public final boolean test(Object obj) {
        return ((IpPrefix) obj).contains(this.f$0);
    }
}
