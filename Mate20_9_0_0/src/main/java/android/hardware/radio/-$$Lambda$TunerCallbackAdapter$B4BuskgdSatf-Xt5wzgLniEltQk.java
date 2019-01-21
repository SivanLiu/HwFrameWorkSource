package android.hardware.radio;

import android.hardware.radio.RadioManager.BandConfig;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$B4BuskgdSatf-Xt5wzgLniEltQk implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ BandConfig f$1;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$B4BuskgdSatf-Xt5wzgLniEltQk(TunerCallbackAdapter tunerCallbackAdapter, BandConfig bandConfig) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = bandConfig;
    }

    public final void run() {
        this.f$0.mCallback.onConfigurationChanged(this.f$1);
    }
}
