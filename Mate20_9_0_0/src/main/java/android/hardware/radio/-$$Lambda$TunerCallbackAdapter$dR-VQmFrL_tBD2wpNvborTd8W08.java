package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$dR-VQmFrL_tBD2wpNvborTd8W08 implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$dR-VQmFrL_tBD2wpNvborTd8W08(TunerCallbackAdapter tunerCallbackAdapter, boolean z) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = z;
    }

    public final void run() {
        this.f$0.mCallback.onAntennaState(this.f$1);
    }
}
