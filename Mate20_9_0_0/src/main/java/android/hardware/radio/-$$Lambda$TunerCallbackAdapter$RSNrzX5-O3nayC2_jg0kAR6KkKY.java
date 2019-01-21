package android.hardware.radio;

import android.hardware.radio.RadioManager.ProgramInfo;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$RSNrzX5-O3nayC2_jg0kAR6KkKY implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ ProgramInfo f$1;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$RSNrzX5-O3nayC2_jg0kAR6KkKY(TunerCallbackAdapter tunerCallbackAdapter, ProgramInfo programInfo) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = programInfo;
    }

    public final void run() {
        TunerCallbackAdapter.lambda$onCurrentProgramInfoChanged$6(this.f$0, this.f$1);
    }
}
