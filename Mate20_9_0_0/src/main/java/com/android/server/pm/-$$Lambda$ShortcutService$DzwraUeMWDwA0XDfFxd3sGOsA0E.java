package com.android.server.pm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$DzwraUeMWDwA0XDfFxd3sGOsA0E implements Runnable {
    private final /* synthetic */ ShortcutService f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ String f$2;

    public /* synthetic */ -$$Lambda$ShortcutService$DzwraUeMWDwA0XDfFxd3sGOsA0E(ShortcutService shortcutService, int i, String str) {
        this.f$0 = shortcutService;
        this.f$1 = i;
        this.f$2 = str;
    }

    public final void run() {
        ShortcutService.lambda$notifyListeners$1(this.f$0, this.f$1, this.f$2);
    }
}
