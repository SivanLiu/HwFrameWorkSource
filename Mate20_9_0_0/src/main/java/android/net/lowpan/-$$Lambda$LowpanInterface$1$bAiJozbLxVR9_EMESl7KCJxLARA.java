package android.net.lowpan;

import android.net.LinkAddress;
import android.net.lowpan.LowpanInterface.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanInterface$1$bAiJozbLxVR9_EMESl7KCJxLARA implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ LinkAddress f$1;

    public /* synthetic */ -$$Lambda$LowpanInterface$1$bAiJozbLxVR9_EMESl7KCJxLARA(Callback callback, LinkAddress linkAddress) {
        this.f$0 = callback;
        this.f$1 = linkAddress;
    }

    public final void run() {
        this.f$0.onLinkAddressRemoved(this.f$1);
    }
}
