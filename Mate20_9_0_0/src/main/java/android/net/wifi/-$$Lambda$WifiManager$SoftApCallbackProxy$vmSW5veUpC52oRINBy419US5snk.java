package android.net.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiManager$SoftApCallbackProxy$vmSW5veUpC52oRINBy419US5snk implements Runnable {
    private final /* synthetic */ SoftApCallbackProxy f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$WifiManager$SoftApCallbackProxy$vmSW5veUpC52oRINBy419US5snk(SoftApCallbackProxy softApCallbackProxy, int i, int i2) {
        this.f$0 = softApCallbackProxy;
        this.f$1 = i;
        this.f$2 = i2;
    }

    public final void run() {
        this.f$0.mCallback.onStateChanged(this.f$1, this.f$2);
    }
}
