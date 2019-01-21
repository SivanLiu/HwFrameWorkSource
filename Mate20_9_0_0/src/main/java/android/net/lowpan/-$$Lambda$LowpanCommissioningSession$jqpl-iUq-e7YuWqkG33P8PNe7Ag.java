package android.net.lowpan;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanCommissioningSession$jqpl-iUq-e7YuWqkG33P8PNe7Ag implements Runnable {
    private final /* synthetic */ LowpanCommissioningSession f$0;

    public /* synthetic */ -$$Lambda$LowpanCommissioningSession$jqpl-iUq-e7YuWqkG33P8PNe7Ag(LowpanCommissioningSession lowpanCommissioningSession) {
        this.f$0 = lowpanCommissioningSession;
    }

    public final void run() {
        this.f$0.mCallback.onClosed();
    }
}
