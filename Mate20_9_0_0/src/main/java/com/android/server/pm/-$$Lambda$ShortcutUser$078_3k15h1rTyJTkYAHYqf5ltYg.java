package com.android.server.pm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutUser$078_3k15h1rTyJTkYAHYqf5ltYg implements Consumer {
    private final /* synthetic */ ShortcutUser f$0;
    private final /* synthetic */ ShortcutService f$1;
    private final /* synthetic */ int[] f$2;
    private final /* synthetic */ int[] f$3;

    public /* synthetic */ -$$Lambda$ShortcutUser$078_3k15h1rTyJTkYAHYqf5ltYg(ShortcutUser shortcutUser, ShortcutService shortcutService, int[] iArr, int[] iArr2) {
        this.f$0 = shortcutUser;
        this.f$1 = shortcutService;
        this.f$2 = iArr;
        this.f$3 = iArr2;
    }

    public final void accept(Object obj) {
        ShortcutUser.lambda$mergeRestoredFile$4(this.f$0, this.f$1, this.f$2, this.f$3, (ShortcutPackage) obj);
    }
}
