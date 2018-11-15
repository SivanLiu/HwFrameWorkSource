package com.android.server.mtm.dump;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$DumpAppMngClean$KfhossEyWguRgocnpArCBogtq84 implements Consumer {
    public static final /* synthetic */ -$$Lambda$DumpAppMngClean$KfhossEyWguRgocnpArCBogtq84 INSTANCE = new -$$Lambda$DumpAppMngClean$KfhossEyWguRgocnpArCBogtq84();

    private /* synthetic */ -$$Lambda$DumpAppMngClean$KfhossEyWguRgocnpArCBogtq84() {
    }

    public final void accept(Object obj) {
        DumpAppMngClean.clean(((Params) obj).context, ((Params) obj).pw, ((Params) obj).args);
    }
}
