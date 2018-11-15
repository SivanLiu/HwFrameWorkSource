package android.net.ip;

import android.net.LinkAddress;
import android.net.ip.IpClient.InitialConfiguration;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$WB134Aq_hrEPp-6UsNJgWvtMzBM implements Predicate {
    public static final /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$WB134Aq_hrEPp-6UsNJgWvtMzBM INSTANCE = new -$$Lambda$IpClient$InitialConfiguration$WB134Aq_hrEPp-6UsNJgWvtMzBM();

    private /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$WB134Aq_hrEPp-6UsNJgWvtMzBM() {
    }

    public final boolean test(Object obj) {
        return InitialConfiguration.isIPv6GUA((LinkAddress) obj);
    }
}
