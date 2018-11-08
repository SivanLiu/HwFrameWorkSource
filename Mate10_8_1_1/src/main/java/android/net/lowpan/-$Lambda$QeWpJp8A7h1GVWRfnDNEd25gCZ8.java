package android.net.lowpan;

import android.net.lowpan.LowpanScanner.Callback;

final /* synthetic */ class -$Lambda$QeWpJp8A7h1GVWRfnDNEd25gCZ8 implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((LowpanCommissioningSession) this.-$f0).lambda$-android_net_lowpan_LowpanCommissioningSession_4529();
    }

    private final /* synthetic */ void $m$1() {
        ((Callback) this.-$f0).onScanFinished();
    }

    private final /* synthetic */ void $m$2() {
        ((Callback) this.-$f0).onScanFinished();
    }

    public /* synthetic */ -$Lambda$QeWpJp8A7h1GVWRfnDNEd25gCZ8(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
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
            default:
                throw new AssertionError();
        }
    }
}
