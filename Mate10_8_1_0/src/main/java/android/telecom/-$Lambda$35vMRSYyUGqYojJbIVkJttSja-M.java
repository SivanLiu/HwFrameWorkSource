package android.telecom;

import android.telecom.Call.Callback;

final /* synthetic */ class -$Lambda$35vMRSYyUGqYojJbIVkJttSja-M implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    private final /* synthetic */ void $m$0() {
        ((Callback) this.-$f1).onRttModeChanged((Call) this.-$f2, this.-$f0);
    }

    private final /* synthetic */ void $m$1() {
        ((Callback) this.-$f1).onRttInitiationFailure((Call) this.-$f2, this.-$f0);
    }

    private final /* synthetic */ void $m$2() {
        ((Callback) this.-$f1).onRttRequest((Call) this.-$f2, this.-$f0);
    }

    private final /* synthetic */ void $m$3() {
        ((RemoteConnection.Callback) this.-$f1).onRttInitiationFailure((RemoteConnection) this.-$f2, this.-$f0);
    }

    public /* synthetic */ -$Lambda$35vMRSYyUGqYojJbIVkJttSja-M(byte b, int i, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = i;
        this.-$f1 = obj;
        this.-$f2 = obj2;
    }

    public final void run() {
        switch (this.$id) {
            case (byte) 0:
                $m$0();
                return;
            case (byte) 1:
                $m$1();
                return;
            case (byte) 2:
                $m$2();
                return;
            case (byte) 3:
                $m$3();
                return;
            default:
                throw new AssertionError();
        }
    }
}
