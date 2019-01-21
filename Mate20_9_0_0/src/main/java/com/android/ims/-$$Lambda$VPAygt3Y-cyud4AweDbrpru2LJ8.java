package com.android.ims;

import com.android.ims.MmTelFeatureConnection.IFeatureUpdate;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$VPAygt3Y-cyud4AweDbrpru2LJ8 implements Consumer {
    public static final /* synthetic */ -$$Lambda$VPAygt3Y-cyud4AweDbrpru2LJ8 INSTANCE = new -$$Lambda$VPAygt3Y-cyud4AweDbrpru2LJ8();

    private /* synthetic */ -$$Lambda$VPAygt3Y-cyud4AweDbrpru2LJ8() {
    }

    public final void accept(Object obj) {
        ((IFeatureUpdate) obj).notifyUnavailable();
    }
}
