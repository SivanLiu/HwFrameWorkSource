package com.android.server.pm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutPackage$ibOAVgfKWMZFYSeVV_hLNx6jogk implements Consumer {
    private final /* synthetic */ ShortcutPackage f$0;

    public /* synthetic */ -$$Lambda$ShortcutPackage$ibOAVgfKWMZFYSeVV_hLNx6jogk(ShortcutPackage shortcutPackage) {
        this.f$0 = shortcutPackage;
    }

    public final void accept(Object obj) {
        ShortcutPackage.lambda$refreshPinnedFlags$0(this.f$0, (ShortcutLauncher) obj);
    }
}
