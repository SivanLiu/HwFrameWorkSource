package com.android.server.pm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$jZzCUQd1whVIqs_s1XMLbFqTP_E implements Runnable {
    private final /* synthetic */ ShortcutService f$0;

    public /* synthetic */ -$$Lambda$jZzCUQd1whVIqs_s1XMLbFqTP_E(ShortcutService shortcutService) {
        this.f$0 = shortcutService;
    }

    public final void run() {
        this.f$0.saveDirtyInfo();
    }
}
