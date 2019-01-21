package android.hardware.radio;

import java.util.Map;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$Yz-4KCDu1MOynGdkDf_oMxqhjeY implements Runnable {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ Map f$1;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$Yz-4KCDu1MOynGdkDf_oMxqhjeY(TunerCallbackAdapter tunerCallbackAdapter, Map map) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = map;
    }

    public final void run() {
        this.f$0.mCallback.onParametersUpdated(this.f$1);
    }
}
