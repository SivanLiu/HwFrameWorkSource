package com.android.server.pm;

import com.android.server.pm.ShortcutService.AnonymousClass3;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$3$n_VdEzyBcjs0pGZO8GnB0FoTgR0 implements Runnable {
    private final /* synthetic */ AnonymousClass3 f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$ShortcutService$3$n_VdEzyBcjs0pGZO8GnB0FoTgR0(AnonymousClass3 anonymousClass3, int i, int i2) {
        this.f$0 = anonymousClass3;
        this.f$1 = i;
        this.f$2 = i2;
    }

    public final void run() {
        ShortcutService.this.handleOnUidStateChanged(this.f$1, this.f$2);
    }
}
