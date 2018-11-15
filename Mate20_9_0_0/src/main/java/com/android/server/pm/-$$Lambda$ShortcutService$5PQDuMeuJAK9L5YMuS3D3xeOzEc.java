package com.android.server.pm;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$5PQDuMeuJAK9L5YMuS3D3xeOzEc implements Consumer {
    public static final /* synthetic */ -$$Lambda$ShortcutService$5PQDuMeuJAK9L5YMuS3D3xeOzEc INSTANCE = new -$$Lambda$ShortcutService$5PQDuMeuJAK9L5YMuS3D3xeOzEc();

    private /* synthetic */ -$$Lambda$ShortcutService$5PQDuMeuJAK9L5YMuS3D3xeOzEc() {
    }

    public final void accept(Object obj) {
        ((ShortcutLauncher) obj).ensurePackageInfo();
    }
}
