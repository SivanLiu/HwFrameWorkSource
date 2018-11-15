package com.android.server.print;

import android.print.PrinterId;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemotePrintService$TsHHZCuIB3sKEZ8IZ0oPokZZO6g implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$RemotePrintService$TsHHZCuIB3sKEZ8IZ0oPokZZO6g INSTANCE = new -$$Lambda$RemotePrintService$TsHHZCuIB3sKEZ8IZ0oPokZZO6g();

    private /* synthetic */ -$$Lambda$RemotePrintService$TsHHZCuIB3sKEZ8IZ0oPokZZO6g() {
    }

    public final void accept(Object obj, Object obj2) {
        ((RemotePrintService) obj).handleRequestCustomPrinterIcon((PrinterId) obj2);
    }
}
