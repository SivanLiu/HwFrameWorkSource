package com.android.server.companion;

import com.android.internal.util.CollectionUtils;
import java.util.Set;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManagerService$1$EelUlD0Ldboon98oq6H5kDCPW9I implements Function {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$CompanionDeviceManagerService$1$EelUlD0Ldboon98oq6H5kDCPW9I(String str) {
        this.f$0 = str;
    }

    public final Object apply(Object obj) {
        return CollectionUtils.filter((Set) obj, new -$$Lambda$CompanionDeviceManagerService$1$IwZz9SPheLuA45R-qkZX_v1sHV4(this.f$0));
    }
}
