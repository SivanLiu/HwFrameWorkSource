package android.net.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiManager$ProvisioningCallbackProxy$0_NXiwyrbrT_579x-6QMO0y3rzc implements Runnable {
    private final /* synthetic */ ProvisioningCallbackProxy f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$WifiManager$ProvisioningCallbackProxy$0_NXiwyrbrT_579x-6QMO0y3rzc(ProvisioningCallbackProxy provisioningCallbackProxy, int i) {
        this.f$0 = provisioningCallbackProxy;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mCallback.onProvisioningStatus(this.f$1);
    }
}
