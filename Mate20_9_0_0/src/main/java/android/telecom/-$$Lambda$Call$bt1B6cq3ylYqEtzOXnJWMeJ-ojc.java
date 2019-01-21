package android.telecom;

import android.telecom.Call.Callback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Call$bt1B6cq3ylYqEtzOXnJWMeJ-ojc implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ Call f$1;

    public /* synthetic */ -$$Lambda$Call$bt1B6cq3ylYqEtzOXnJWMeJ-ojc(Callback callback, Call call) {
        this.f$0 = callback;
        this.f$1 = call;
    }

    public final void run() {
        this.f$0.onHandoverComplete(this.f$1);
    }
}
