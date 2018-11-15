package android.net.ip;

import android.net.IpPrefix;
import android.net.ip.IpClient.InitialConfiguration;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$-qxDAAo5wjq2G7x-F8gQeNSxIxY implements Predicate {
    public static final /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$-qxDAAo5wjq2G7x-F8gQeNSxIxY INSTANCE = new -$$Lambda$IpClient$InitialConfiguration$-qxDAAo5wjq2G7x-F8gQeNSxIxY();

    private /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$-qxDAAo5wjq2G7x-F8gQeNSxIxY() {
    }

    public final boolean test(Object obj) {
        return InitialConfiguration.isPrefixLengthCompliant((IpPrefix) obj);
    }
}
