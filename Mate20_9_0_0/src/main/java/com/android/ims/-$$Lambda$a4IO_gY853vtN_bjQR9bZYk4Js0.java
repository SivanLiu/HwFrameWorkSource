package com.android.ims;

import com.android.ims.MmTelFeatureConnection.IFeatureUpdate;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$a4IO_gY853vtN_bjQR9bZYk4Js0 implements Consumer {
    public static final /* synthetic */ -$$Lambda$a4IO_gY853vtN_bjQR9bZYk4Js0 INSTANCE = new -$$Lambda$a4IO_gY853vtN_bjQR9bZYk4Js0();

    private /* synthetic */ -$$Lambda$a4IO_gY853vtN_bjQR9bZYk4Js0() {
    }

    public final void accept(Object obj) {
        ((IFeatureUpdate) obj).notifyStateChanged();
    }
}
