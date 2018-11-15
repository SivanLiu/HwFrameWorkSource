package android.net.lowpan;

import android.net.lowpan.LowpanManager.AnonymousClass2;
import android.net.lowpan.LowpanManager.Callback;

final /* synthetic */ class -$Lambda$fU5N8X3bFktKBQFPK6v4czv7e_Y implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    private final /* synthetic */ void $m$0() {
        ((AnonymousClass2) this.-$f0).lambda$-android_net_lowpan_LowpanManager$2_8833((ILowpanInterface) this.-$f1, (Callback) this.-$f2);
    }

    private final /* synthetic */ void $m$1() {
        ((AnonymousClass2) this.-$f0).lambda$-android_net_lowpan_LowpanManager$2_9391((ILowpanInterface) this.-$f1, (Callback) this.-$f2);
    }

    public /* synthetic */ -$Lambda$fU5N8X3bFktKBQFPK6v4czv7e_Y(byte b, Object obj, Object obj2, Object obj3) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
        this.-$f2 = obj3;
    }

    public final void run() {
        switch (this.$id) {
            case (byte) 0:
                $m$0();
                return;
            case (byte) 1:
                $m$1();
                return;
            default:
                throw new AssertionError();
        }
    }
}
