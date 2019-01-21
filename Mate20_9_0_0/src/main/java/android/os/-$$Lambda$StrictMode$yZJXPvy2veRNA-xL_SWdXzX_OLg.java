package android.os;

import android.os.StrictMode.ViolationInfo;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$StrictMode$yZJXPvy2veRNA-xL_SWdXzX_OLg implements Runnable {
    private final /* synthetic */ int f$0;
    private final /* synthetic */ ViolationInfo f$1;

    public /* synthetic */ -$$Lambda$StrictMode$yZJXPvy2veRNA-xL_SWdXzX_OLg(int i, ViolationInfo violationInfo) {
        this.f$0 = i;
        this.f$1 = violationInfo;
    }

    public final void run() {
        StrictMode.lambda$dropboxViolationAsync$2(this.f$0, this.f$1);
    }
}
