package android.telecom;

import android.telecom.Call.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Call$aPdcAxyKfpxcuraTjET8ce3xApc implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ Call f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$Call$aPdcAxyKfpxcuraTjET8ce3xApc(Callback callback, Call call, int i) {
        this.f$0 = callback;
        this.f$1 = call;
        this.f$2 = i;
    }

    public final void run() {
        this.f$0.onHandoverFailed(this.f$1, this.f$2);
    }
}
