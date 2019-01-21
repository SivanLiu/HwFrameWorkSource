package android.net.wifi;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$WifiManager$ProvisioningCallbackProxy$rgPeSRj_1qriYZtaCu57EZHtc_Q implements Runnable {
    private final /* synthetic */ ProvisioningCallbackProxy f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$WifiManager$ProvisioningCallbackProxy$rgPeSRj_1qriYZtaCu57EZHtc_Q(ProvisioningCallbackProxy provisioningCallbackProxy, int i) {
        this.f$0 = provisioningCallbackProxy;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mCallback.onProvisioningFailure(this.f$1);
    }
}
