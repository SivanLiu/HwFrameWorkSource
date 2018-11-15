package android.net.ip;

import android.net.LinkAddress;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$InitialConfiguration$guZQ276VyezHQuIEE0CG9osvUes implements Predicate {
    private final /* synthetic */ Class f$0;

    public /* synthetic */ -$$Lambda$IpClient$InitialConfiguration$guZQ276VyezHQuIEE0CG9osvUes(Class cls) {
        this.f$0 = cls;
    }

    public final boolean test(Object obj) {
        return this.f$0.isInstance((LinkAddress) obj);
    }
}
