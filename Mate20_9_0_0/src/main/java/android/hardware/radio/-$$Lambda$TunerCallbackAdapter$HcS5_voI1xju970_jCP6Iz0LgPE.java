package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$HcS5_voI1xju970_jCP6Iz0LgPE implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$HcS5_voI1xju970_jCP6Iz0LgPE(TunerCallbackAdapter tunerCallbackAdapter, int i) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = i;
    }

    public final void run() {
        this.f$0.mCallback.onError(this.f$1);
    }
}
