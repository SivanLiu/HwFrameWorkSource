package android.hardware.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubTransaction$RNVGnle3xCUm9u68syzn6-2znnU implements Runnable {
    private final /* synthetic */ ContextHubTransaction f$0;

    public /* synthetic */ -$$Lambda$ContextHubTransaction$RNVGnle3xCUm9u68syzn6-2znnU(ContextHubTransaction contextHubTransaction) {
        this.f$0 = contextHubTransaction;
    }

    public final void run() {
        this.f$0.mListener.onComplete(this.f$0, this.f$0.mResponse);
    }
}
