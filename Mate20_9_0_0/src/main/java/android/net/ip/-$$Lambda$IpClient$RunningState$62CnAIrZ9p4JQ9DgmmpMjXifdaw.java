package android.net.ip;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IpClient$RunningState$62CnAIrZ9p4JQ9DgmmpMjXifdaw implements Runnable {
    private final /* synthetic */ RunningState f$0;

    public /* synthetic */ -$$Lambda$IpClient$RunningState$62CnAIrZ9p4JQ9DgmmpMjXifdaw(RunningState runningState) {
        this.f$0 = runningState;
    }

    public final void run() {
        IpClient.this.mLog.log("OBSERVED AvoidBadWifi changed");
    }
}
