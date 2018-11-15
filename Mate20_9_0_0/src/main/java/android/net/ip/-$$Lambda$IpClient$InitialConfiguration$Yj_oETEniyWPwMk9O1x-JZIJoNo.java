package android.net.ip;

import android.net.LinkAddress;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$Yj_oETEniyWPwMk9O1x-JZIJoNo implements Predicate {
    private final /* synthetic */ LinkAddress f$0;

    public /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$Yj_oETEniyWPwMk9O1x-JZIJoNo(LinkAddress linkAddress) {
        this.f$0 = linkAddress;
    }

    public final boolean test(Object obj) {
        return this.f$0.isSameAddressAs((LinkAddress) obj);
    }
}
