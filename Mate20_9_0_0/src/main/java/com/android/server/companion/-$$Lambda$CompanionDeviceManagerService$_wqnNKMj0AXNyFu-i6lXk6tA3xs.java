package com.android.server.companion;

import java.io.FileOutputStream;
import java.util.Set;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManagerService$_wqnNKMj0AXNyFu-i6lXk6tA3xs implements Consumer {
    private final /* synthetic */ Set f$0;

    public /* synthetic */ -$$Lambda$CompanionDeviceManagerService$_wqnNKMj0AXNyFu-i6lXk6tA3xs(Set set) {
        this.f$0 = set;
    }

    public final void accept(Object obj) {
        CompanionDeviceManagerService.lambda$updateAssociations$4(this.f$0, (FileOutputStream) obj);
    }
}
