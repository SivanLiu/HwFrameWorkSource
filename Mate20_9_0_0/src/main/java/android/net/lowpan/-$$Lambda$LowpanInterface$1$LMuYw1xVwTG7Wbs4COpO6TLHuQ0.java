package android.net.lowpan;

import android.net.lowpan.LowpanInterface.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanInterface$1$LMuYw1xVwTG7Wbs4COpO6TLHuQ0 implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$LowpanInterface$1$LMuYw1xVwTG7Wbs4COpO6TLHuQ0(Callback callback, boolean z) {
        this.f$0 = callback;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.onEnabledChanged(this.f$1);
    }
}
