package android.os;

import android.view.IWindowManager;
import java.util.ArrayList;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StrictMode$AndroidBlockGuardPolicy$Mxbi12aLrPMWhtfmockn9dQK-dQ implements Runnable {
    private final /* synthetic */ AndroidBlockGuardPolicy f$0;
    private final /* synthetic */ IWindowManager f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ ArrayList f$3;

    public /* synthetic */ -$$Lambda$StrictMode$AndroidBlockGuardPolicy$Mxbi12aLrPMWhtfmockn9dQK-dQ(AndroidBlockGuardPolicy androidBlockGuardPolicy, IWindowManager iWindowManager, boolean z, ArrayList arrayList) {
        this.f$0 = androidBlockGuardPolicy;
        this.f$1 = iWindowManager;
        this.f$2 = z;
        this.f$3 = arrayList;
    }

    public final void run() {
        AndroidBlockGuardPolicy.lambda$handleViolationWithTimingAttempt$0(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
