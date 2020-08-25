package com.android.server.mtm.dump;

import com.android.server.mtm.dump.DumpAppMngClean;
import java.util.function.Consumer;

/* renamed from: com.android.server.mtm.dump.-$$Lambda$DumpAppMngClean$2XeGQhnFySZMuwgGMTUicbZ3jDI  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$DumpAppMngClean$2XeGQhnFySZMuwgGMTUicbZ3jDI implements Consumer {
    public static final /* synthetic */ $$Lambda$DumpAppMngClean$2XeGQhnFySZMuwgGMTUicbZ3jDI INSTANCE = new $$Lambda$DumpAppMngClean$2XeGQhnFySZMuwgGMTUicbZ3jDI();

    private /* synthetic */ $$Lambda$DumpAppMngClean$2XeGQhnFySZMuwgGMTUicbZ3jDI() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        DumpAppMngClean.clean(((DumpAppMngClean.Params) obj).context, ((DumpAppMngClean.Params) obj).pw, ((DumpAppMngClean.Params) obj).args);
    }
}
