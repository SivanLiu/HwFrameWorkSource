package com.android.server.pm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutUser$XHWlvjfCvG1SoVwGHi3envhmtfM implements Consumer {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ Consumer f$2;

    public /* synthetic */ -$$Lambda$ShortcutUser$XHWlvjfCvG1SoVwGHi3envhmtfM(int i, String str, Consumer consumer) {
        this.f$0 = i;
        this.f$1 = str;
        this.f$2 = consumer;
    }

    public final void accept(Object obj) {
        ShortcutUser.lambda$forPackageItem$0(this.f$0, this.f$1, this.f$2, (ShortcutPackageItem) obj);
    }
}
