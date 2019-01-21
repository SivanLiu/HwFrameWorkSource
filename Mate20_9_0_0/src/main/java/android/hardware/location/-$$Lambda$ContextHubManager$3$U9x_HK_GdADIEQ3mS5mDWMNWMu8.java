package android.hardware.location;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ContextHubManager$3$U9x_HK_GdADIEQ3mS5mDWMNWMu8 implements Runnable {
    private final /* synthetic */ ContextHubClientCallback f$0;
    private final /* synthetic */ ContextHubClient f$1;
    private final /* synthetic */ NanoAppMessage f$2;

    public /* synthetic */ -$$Lambda$ContextHubManager$3$U9x_HK_GdADIEQ3mS5mDWMNWMu8(ContextHubClientCallback contextHubClientCallback, ContextHubClient contextHubClient, NanoAppMessage nanoAppMessage) {
        this.f$0 = contextHubClientCallback;
        this.f$1 = contextHubClient;
        this.f$2 = nanoAppMessage;
    }

    public final void run() {
        this.f$0.onMessageFromNanoApp(this.f$1, this.f$2);
    }
}
