package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$4zf9n0sz_rU8z6a9GJmRInWrYkQ implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$4zf9n0sz_rU8z6a9GJmRInWrYkQ(TunerCallbackAdapter tunerCallbackAdapter, boolean z) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.mCallback.onBackgroundScanAvailabilityChange(this.f$1);
    }
}
