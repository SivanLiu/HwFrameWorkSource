package com.android.server.wifi;

import android.net.wifi.ScanResult;
import java.util.Set;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WakeupController$sB8N4NPbyfefFu6fc4L75U1Md4E implements Predicate {
    private final /* synthetic */ Set f$0;

    public /* synthetic */ -$$Lambda$WakeupController$sB8N4NPbyfefFu6fc4L75U1Md4E(Set set) {
        this.f$0 = set;
    }

    public final boolean test(Object obj) {
        return (this.f$0.contains(Integer.valueOf(((ScanResult) obj).frequency)) ^ 1);
    }
}
