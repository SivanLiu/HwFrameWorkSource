package com.android.server.pm;

import android.content.pm.ApplicationInfo;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ShortcutService$XepsJlzLd-VitYi8_ThhUsx37Ok implements Consumer {
    private final /* synthetic */ ShortcutService f$0;
    private final /* synthetic */ ShortcutUser f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$ShortcutService$XepsJlzLd-VitYi8_ThhUsx37Ok(ShortcutService shortcutService, ShortcutUser shortcutUser, int i) {
        this.f$0 = shortcutService;
        this.f$1 = shortcutUser;
        this.f$2 = i;
    }

    public final void accept(Object obj) {
        ShortcutService.lambda$rescanUpdatedPackagesLocked$7(this.f$0, this.f$1, this.f$2, (ApplicationInfo) obj);
    }
}
