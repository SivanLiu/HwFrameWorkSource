package android.net.ip;

import android.net.IpPrefix;
import android.net.ip.IpClient.InitialConfiguration;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$gxc5co5uJUOrlWJ8HYqcngxR5gI implements Predicate {
    public static final /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$gxc5co5uJUOrlWJ8HYqcngxR5gI INSTANCE = new -$$Lambda$IpClient$InitialConfiguration$gxc5co5uJUOrlWJ8HYqcngxR5gI();

    private /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$gxc5co5uJUOrlWJ8HYqcngxR5gI() {
    }

    public final boolean test(Object obj) {
        return InitialConfiguration.isIPv6DefaultRoute((IpPrefix) obj);
    }
}
