package com.android.server.location;

import android.location.GnssMeasurementsEvent;
import android.location.IGnssMeasurementsListener;
import android.os.IInterface;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GnssMeasurementsProvider$865xzodmeiSeR2xhh7cKZjiZkhE implements ListenerOperation {
    private final /* synthetic */ GnssMeasurementsEvent f$0;

    public /* synthetic */ -$$Lambda$GnssMeasurementsProvider$865xzodmeiSeR2xhh7cKZjiZkhE(GnssMeasurementsEvent gnssMeasurementsEvent) {
        this.f$0 = gnssMeasurementsEvent;
    }

    public final void execute(IInterface iInterface) {
        ((IGnssMeasurementsListener) iInterface).onGnssMeasurementsReceived(this.f$0);
    }
}
