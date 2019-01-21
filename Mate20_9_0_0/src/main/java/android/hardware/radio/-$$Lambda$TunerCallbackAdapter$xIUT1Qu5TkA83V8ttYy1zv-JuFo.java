package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$xIUT1Qu5TkA83V8ttYy1zv-JuFo implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$xIUT1Qu5TkA83V8ttYy1zv-JuFo(TunerCallbackAdapter tunerCallbackAdapter) {
        this.f$0 = tunerCallbackAdapter;
    }

    public final void run() {
        this.f$0.mCallback.onBackgroundScanComplete();
    }
}
