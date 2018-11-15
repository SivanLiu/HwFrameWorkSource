package com.android.server.companion;

import java.util.Objects;
import java.util.function.Predicate;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManagerService$1$IwZz9SPheLuA45R-qkZX_v1sHV4 implements Predicate {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$CompanionDeviceManagerService$1$IwZz9SPheLuA45R-qkZX_v1sHV4(String str) {
        this.f$0 = str;
    }

    public final boolean test(Object obj) {
        return (Objects.equals(((Association) obj).companionAppPackage, this.f$0) ^ 1);
    }
}
