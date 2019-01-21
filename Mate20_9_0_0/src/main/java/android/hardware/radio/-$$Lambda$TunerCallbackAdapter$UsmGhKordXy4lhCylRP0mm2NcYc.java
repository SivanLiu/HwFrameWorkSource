package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$UsmGhKordXy4lhCylRP0mm2NcYc implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$UsmGhKordXy4lhCylRP0mm2NcYc(TunerCallbackAdapter tunerCallbackAdapter) {
        this.f$0 = tunerCallbackAdapter;
    }

    public final void run() {
        this.f$0.mCallback.onProgramListChanged();
    }
}
