package android.hardware.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubTransaction$7a5H6DrY_dOy9M3qnYHhlmDHRNQ implements Runnable {
    private final /* synthetic */ ContextHubTransaction f$0;

    public /* synthetic */ -$$Lambda$ContextHubTransaction$7a5H6DrY_dOy9M3qnYHhlmDHRNQ(ContextHubTransaction contextHubTransaction) {
        this.f$0 = contextHubTransaction;
    }

    public final void run() {
        this.f$0.mListener.onComplete(this.f$0, this.f$0.mResponse);
    }
}
