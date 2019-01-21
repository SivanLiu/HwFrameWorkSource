package android.print;

import android.print.PrintManager.PrintServiceRecommendationsChangeListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$KZ41E_yXUNYMY9k_Xeus1UG_cS8 implements Runnable {
    private final /* synthetic */ PrintServiceRecommendationsChangeListener f$0;

    public /* synthetic */ -$$Lambda$KZ41E_yXUNYMY9k_Xeus1UG_cS8(PrintServiceRecommendationsChangeListener printServiceRecommendationsChangeListener) {
        this.f$0 = printServiceRecommendationsChangeListener;
    }

    public final void run() {
        this.f$0.onPrintServiceRecommendationsChanged();
    }
}
