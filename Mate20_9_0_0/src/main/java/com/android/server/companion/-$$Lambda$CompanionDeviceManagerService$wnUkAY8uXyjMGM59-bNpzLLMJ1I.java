package com.android.server.companion;

import android.content.pm.PackageInfo;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$CompanionDeviceManagerService$wnUkAY8uXyjMGM59-bNpzLLMJ1I implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$CompanionDeviceManagerService$wnUkAY8uXyjMGM59-bNpzLLMJ1I INSTANCE = new -$$Lambda$CompanionDeviceManagerService$wnUkAY8uXyjMGM59-bNpzLLMJ1I();

    private /* synthetic */ -$$Lambda$CompanionDeviceManagerService$wnUkAY8uXyjMGM59-bNpzLLMJ1I() {
    }

    public final void accept(Object obj, Object obj2) {
        ((CompanionDeviceManagerService) obj).updateSpecialAccessPermissionAsSystem((PackageInfo) obj2);
    }
}
