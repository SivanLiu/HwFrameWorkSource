package android.net.lowpan;

import android.net.lowpan.LowpanManager.AnonymousClass2;
import android.net.lowpan.LowpanManager.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanManager$2$2qKIy18LeIjTlm4mROg-pHOPNU0 implements Runnable {
    private final /* synthetic */ AnonymousClass2 f$0;
    private final /* synthetic */ ILowpanInterface f$1;
    private final /* synthetic */ Callback f$2;

    public /* synthetic */ -$$Lambda$LowpanManager$2$2qKIy18LeIjTlm4mROg-pHOPNU0(AnonymousClass2 anonymousClass2, ILowpanInterface iLowpanInterface, Callback callback) {
        this.f$0 = anonymousClass2;
        this.f$1 = iLowpanInterface;
        this.f$2 = callback;
    }

    public final void run() {
        AnonymousClass2.lambda$onInterfaceAdded$0(this.f$0, this.f$1, this.f$2);
    }
}
