package com.android.server.connectivity;

import android.net.LinkProperties;
import java.net.InetAddress;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DnsManager$Z_oEyRSp0wthIcVTcqKDoAJRe6Q implements Predicate {
    private final /* synthetic */ LinkProperties f$0;

    public /* synthetic */ -$$Lambda$DnsManager$Z_oEyRSp0wthIcVTcqKDoAJRe6Q(LinkProperties linkProperties) {
        this.f$0 = linkProperties;
    }

    public final boolean test(Object obj) {
        return this.f$0.isReachable((InetAddress) obj);
    }
}
