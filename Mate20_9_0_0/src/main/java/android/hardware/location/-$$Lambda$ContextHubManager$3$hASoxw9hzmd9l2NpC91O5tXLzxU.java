package android.hardware.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubManager$3$hASoxw9hzmd9l2NpC91O5tXLzxU implements Runnable {
    private final /* synthetic */ ContextHubClientCallback f$0;
    private final /* synthetic */ ContextHubClient f$1;
    private final /* synthetic */ long f$2;
    private final /* synthetic */ int f$3;

    public /* synthetic */ -$$Lambda$ContextHubManager$3$hASoxw9hzmd9l2NpC91O5tXLzxU(ContextHubClientCallback contextHubClientCallback, ContextHubClient contextHubClient, long j, int i) {
        this.f$0 = contextHubClientCallback;
        this.f$1 = contextHubClient;
        this.f$2 = j;
        this.f$3 = i;
    }

    public final void run() {
        this.f$0.onNanoAppAborted(this.f$1, this.f$2, this.f$3);
    }
}
