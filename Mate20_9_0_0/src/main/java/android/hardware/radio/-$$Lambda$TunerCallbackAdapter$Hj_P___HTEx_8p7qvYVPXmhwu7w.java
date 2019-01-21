package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$Hj_P___HTEx_8p7qvYVPXmhwu7w implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ ProgramSelector f$2;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$Hj_P___HTEx_8p7qvYVPXmhwu7w(TunerCallbackAdapter tunerCallbackAdapter, int i, ProgramSelector programSelector) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = i;
        this.f$2 = programSelector;
    }

    public final void run() {
        this.f$0.mCallback.onTuneFailed(this.f$1, this.f$2);
    }
}
