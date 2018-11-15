package com.android.server.pm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$7yTF58WvWYAkuD-B4tHmI0K7nyI implements Consumer {
    private final /* synthetic */ ShortcutService f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ boolean f$3;

    public /* synthetic */ -$$Lambda$ShortcutService$7yTF58WvWYAkuD-B4tHmI0K7nyI(ShortcutService shortcutService, String str, int i, boolean z) {
        this.f$0 = shortcutService;
        this.f$1 = str;
        this.f$2 = i;
        this.f$3 = z;
    }

    public final void accept(Object obj) {
        this.f$0.cleanUpPackageLocked(this.f$1, ((ShortcutUser) obj).getUserId(), this.f$2, this.f$3);
    }
}
