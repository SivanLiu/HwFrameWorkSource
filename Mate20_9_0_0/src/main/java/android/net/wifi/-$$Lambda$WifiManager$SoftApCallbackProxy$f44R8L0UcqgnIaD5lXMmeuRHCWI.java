package android.net.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiManager$SoftApCallbackProxy$f44R8L0UcqgnIaD5lXMmeuRHCWI implements Runnable {
    private final /* synthetic */ SoftApCallbackProxy f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$WifiManager$SoftApCallbackProxy$f44R8L0UcqgnIaD5lXMmeuRHCWI(SoftApCallbackProxy softApCallbackProxy, int i) {
        this.f$0 = softApCallbackProxy;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mCallback.onNumClientsChanged(this.f$1);
    }
}
