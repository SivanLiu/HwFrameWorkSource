package android.net.lowpan;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$LowpanCommissioningSession$InternalCallback$TrrmDykqIWeXNdgrXO7t2-rqCTo implements Runnable {
    private final /* synthetic */ InternalCallback f$0;
    private final /* synthetic */ byte[] f$1;

    public /* synthetic */ -$$Lambda$LowpanCommissioningSession$InternalCallback$TrrmDykqIWeXNdgrXO7t2-rqCTo(InternalCallback internalCallback, byte[] bArr) {
        this.f$0 = internalCallback;
        this.f$1 = bArr;
    }

    public final void run() {
        InternalCallback.lambda$onReceiveFromCommissioner$0(this.f$0, this.f$1);
    }
}
