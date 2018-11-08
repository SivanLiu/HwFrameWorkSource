package android.net.ip;

import android.net.ip.IpManager.AnonymousClass2;

final /* synthetic */ class -$Lambda$EIaFPv5OO8Upo9X60vMtrcUNUEQ implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0() {
        ((PacketListener) this.-$f0).lambda$-android_net_ip_ConnectivityPacketTracker$PacketListener_4824((String) this.-$f1);
    }

    private final /* synthetic */ void $m$1() {
        ((AnonymousClass2) this.-$f0).lambda$-android_net_ip_IpManager$2_27516((String) this.-$f1);
    }

    public /* synthetic */ -$Lambda$EIaFPv5OO8Upo9X60vMtrcUNUEQ(byte b, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
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
