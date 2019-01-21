package android.hardware.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubManager$3$kLhhBRChCeue1LKohd5lK_lfKTU implements Runnable {
    private final /* synthetic */ ContextHubClientCallback f$0;
    private final /* synthetic */ ContextHubClient f$1;

    public /* synthetic */ -$$Lambda$ContextHubManager$3$kLhhBRChCeue1LKohd5lK_lfKTU(ContextHubClientCallback contextHubClientCallback, ContextHubClient contextHubClient) {
        this.f$0 = contextHubClientCallback;
        this.f$1 = contextHubClient;
    }

    public final void run() {
        this.f$0.onHubReset(this.f$1);
    }
}
