package com.android.server.am;

import com.android.server.am.UserController.AnonymousClass3;

final /* synthetic */ class -$Lambda$5yQSwWrsRDcxoFuTXgyaBIqPvDw implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ boolean -$f0;
    private final /* synthetic */ int -$f1;
    private final /* synthetic */ Object -$f2;

    private final /* synthetic */ void $m$0() {
        ((ActivityStackSupervisor) this.-$f2).lambda$-com_android_server_am_ActivityStackSupervisor_131760(this.-$f1, this.-$f0);
    }

    private final /* synthetic */ void $m$1() {
        ((AnonymousClass3) this.-$f2).lambda$-com_android_server_am_UserController$3_23658(this.-$f1, this.-$f0);
    }

    public /* synthetic */ -$Lambda$5yQSwWrsRDcxoFuTXgyaBIqPvDw(byte b, boolean z, int i, Object obj) {
        this.$id = b;
        this.-$f0 = z;
        this.-$f1 = i;
        this.-$f2 = obj;
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
