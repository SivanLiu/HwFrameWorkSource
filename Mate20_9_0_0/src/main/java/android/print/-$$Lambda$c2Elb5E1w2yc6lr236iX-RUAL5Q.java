package android.print;

import android.print.PrintManager.PrintServicesChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$c2Elb5E1w2yc6lr236iX-RUAL5Q implements Runnable {
    private final /* synthetic */ PrintServicesChangeListener f$0;

    public /* synthetic */ -$$Lambda$c2Elb5E1w2yc6lr236iX-RUAL5Q(PrintServicesChangeListener printServicesChangeListener) {
        this.f$0 = printServicesChangeListener;
    }

    public final void run() {
        this.f$0.onPrintServicesChanged();
    }
}
