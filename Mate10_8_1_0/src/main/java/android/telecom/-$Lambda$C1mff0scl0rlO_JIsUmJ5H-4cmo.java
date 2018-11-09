package android.telecom;

import android.telecom.Call.Callback;
import android.telecom.Call.RttCall;

final /* synthetic */ class -$Lambda$C1mff0scl0rlO_JIsUmJ5H-4cmo implements Runnable {
    private final /* synthetic */ boolean -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;
    private final /* synthetic */ Object -$f3;

    private final /* synthetic */ void $m$0() {
        ((Callback) this.-$f1).onRttStatusChanged((Call) this.-$f2, this.-$f0, (RttCall) this.-$f3);
    }

    public /* synthetic */ -$Lambda$C1mff0scl0rlO_JIsUmJ5H-4cmo(boolean z, Object obj, Object obj2, Object obj3) {
        this.-$f0 = z;
        this.-$f1 = obj;
        this.-$f2 = obj2;
        this.-$f3 = obj3;
    }

    public final void run() {
        $m$0();
    }
}
