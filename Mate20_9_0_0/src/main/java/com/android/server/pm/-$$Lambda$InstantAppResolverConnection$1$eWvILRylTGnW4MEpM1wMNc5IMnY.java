package com.android.server.pm;

import com.android.server.pm.InstantAppResolverConnection.PhaseTwoCallback;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$InstantAppResolverConnection$1$eWvILRylTGnW4MEpM1wMNc5IMnY implements Runnable {
    private final /* synthetic */ PhaseTwoCallback f$0;
    private final /* synthetic */ ArrayList f$1;
    private final /* synthetic */ long f$2;

    public /* synthetic */ -$$Lambda$InstantAppResolverConnection$1$eWvILRylTGnW4MEpM1wMNc5IMnY(PhaseTwoCallback phaseTwoCallback, ArrayList arrayList, long j) {
        this.f$0 = phaseTwoCallback;
        this.f$1 = arrayList;
        this.f$2 = j;
    }

    public final void run() {
        this.f$0.onPhaseTwoResolved(this.f$1, this.f$2);
    }
}
