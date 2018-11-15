package com.android.server.pm;

import android.content.pm.IDexModuleRegisterCallback;
import com.android.server.pm.dex.DexManager.RegisterDexModuleResult;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$PackageManagerService$opO5L-t6aW9gAx6B5CGlW6sAaX8 implements Runnable {
    private final /* synthetic */ IDexModuleRegisterCallback f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ RegisterDexModuleResult f$2;

    public /* synthetic */ -$$Lambda$PackageManagerService$opO5L-t6aW9gAx6B5CGlW6sAaX8(IDexModuleRegisterCallback iDexModuleRegisterCallback, String str, RegisterDexModuleResult registerDexModuleResult) {
        this.f$0 = iDexModuleRegisterCallback;
        this.f$1 = str;
        this.f$2 = registerDexModuleResult;
    }

    public final void run() {
        PackageManagerService.lambda$registerDexModule$3(this.f$0, this.f$1, this.f$2);
    }
}
