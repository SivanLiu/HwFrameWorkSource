package com.android.server.pm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$okoC4pj1SnB9cHJpbZ-Xc5MyThk implements Consumer {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$ShortcutService$okoC4pj1SnB9cHJpbZ-Xc5MyThk(String str, int i) {
        this.f$0 = str;
        this.f$1 = i;
    }

    public final void accept(Object obj) {
        ((ShortcutLauncher) obj).cleanUpPackage(this.f$0, this.f$1);
    }
}
