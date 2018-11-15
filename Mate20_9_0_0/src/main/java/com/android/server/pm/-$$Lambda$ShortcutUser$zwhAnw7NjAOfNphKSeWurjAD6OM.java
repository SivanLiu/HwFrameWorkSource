package com.android.server.pm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutUser$zwhAnw7NjAOfNphKSeWurjAD6OM implements Consumer {
    private final /* synthetic */ ShortcutUser f$0;
    private final /* synthetic */ ShortcutService f$1;
    private final /* synthetic */ int[] f$2;

    public /* synthetic */ -$$Lambda$ShortcutUser$zwhAnw7NjAOfNphKSeWurjAD6OM(ShortcutUser shortcutUser, ShortcutService shortcutService, int[] iArr) {
        this.f$0 = shortcutUser;
        this.f$1 = shortcutService;
        this.f$2 = iArr;
    }

    public final void accept(Object obj) {
        ShortcutUser.lambda$mergeRestoredFile$3(this.f$0, this.f$1, this.f$2, (ShortcutLauncher) obj);
    }
}
