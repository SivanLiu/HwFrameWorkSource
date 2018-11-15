package com.android.server.pm;

import java.util.ArrayList;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$KKtB89b9du8RtyDY2LIMGlzZzzg implements Consumer {
    private final /* synthetic */ ShortcutService f$0;
    private final /* synthetic */ ArrayList f$1;

    public /* synthetic */ -$$Lambda$ShortcutService$KKtB89b9du8RtyDY2LIMGlzZzzg(ShortcutService shortcutService, ArrayList arrayList) {
        this.f$0 = shortcutService;
        this.f$1 = arrayList;
    }

    public final void accept(Object obj) {
        ShortcutService.lambda$checkPackageChanges$6(this.f$0, this.f$1, (ShortcutPackageItem) obj);
    }
}
