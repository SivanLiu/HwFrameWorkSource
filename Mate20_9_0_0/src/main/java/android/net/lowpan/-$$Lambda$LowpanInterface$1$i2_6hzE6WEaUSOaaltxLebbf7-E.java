package android.net.lowpan;

import android.net.LinkAddress;
import android.net.lowpan.LowpanInterface.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanInterface$1$i2_6hzE6WEaUSOaaltxLebbf7-E implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ LinkAddress f$1;

    public /* synthetic */ -$$Lambda$LowpanInterface$1$i2_6hzE6WEaUSOaaltxLebbf7-E(Callback callback, LinkAddress linkAddress) {
        this.f$0 = callback;
        this.f$1 = linkAddress;
    }

    public final void run() {
        this.f$0.onLinkAddressAdded(this.f$1);
    }
}
