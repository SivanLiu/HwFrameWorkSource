package com.android.server.companion;

import com.android.internal.util.CollectionUtils;
import java.util.Set;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManagerService$pF7vjIJpy5wI-u498jmFdSjoS_0 implements Function {
    private final /* synthetic */ CompanionDeviceManagerService f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ String f$2;
    private final /* synthetic */ String f$3;

    public /* synthetic */ -$$Lambda$CompanionDeviceManagerService$pF7vjIJpy5wI-u498jmFdSjoS_0(CompanionDeviceManagerService companionDeviceManagerService, int i, String str, String str2) {
        this.f$0 = companionDeviceManagerService;
        this.f$1 = i;
        this.f$2 = str;
        this.f$3 = str2;
    }

    public final Object apply(Object obj) {
        return CollectionUtils.add((Set) obj, new Association(this.f$0, this.f$1, this.f$2, this.f$3, null));
    }
}
