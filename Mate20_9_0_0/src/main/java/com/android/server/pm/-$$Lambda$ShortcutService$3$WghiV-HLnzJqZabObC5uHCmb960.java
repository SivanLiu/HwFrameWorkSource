package com.android.server.pm;

import com.android.server.pm.ShortcutService.AnonymousClass3;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$3$WghiV-HLnzJqZabObC5uHCmb960 implements Runnable {
    private final /* synthetic */ AnonymousClass3 f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$ShortcutService$3$WghiV-HLnzJqZabObC5uHCmb960(AnonymousClass3 anonymousClass3, int i) {
        this.f$0 = anonymousClass3;
        this.f$1 = i;
    }

    public final void run() {
        ShortcutService.this.handleOnUidStateChanged(this.f$1, 19);
    }
}
