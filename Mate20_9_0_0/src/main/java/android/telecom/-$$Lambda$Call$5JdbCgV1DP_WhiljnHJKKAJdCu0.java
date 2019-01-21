package android.telecom;

import android.telecom.Call.Callback;
import android.telecom.Call.RttCall;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$Call$5JdbCgV1DP_WhiljnHJKKAJdCu0 implements Runnable {
    private final /* synthetic */ Callback f$0;
    private final /* synthetic */ Call f$1;
    private final /* synthetic */ boolean f$2;
    private final /* synthetic */ RttCall f$3;

    public /* synthetic */ -$$Lambda$Call$5JdbCgV1DP_WhiljnHJKKAJdCu0(Callback callback, Call call, boolean z, RttCall rttCall) {
        this.f$0 = callback;
        this.f$1 = call;
        this.f$2 = z;
        this.f$3 = rttCall;
    }

    public final void run() {
        this.f$0.onRttStatusChanged(this.f$1, this.f$2, this.f$3);
    }
}
