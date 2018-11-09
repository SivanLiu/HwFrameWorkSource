package com.android.server.autofill;

import android.service.autofill.FillResponse;

final /* synthetic */ class -$Lambda$JYqZriexGNVTrQ5cwTlcgjPSZFY implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    /* renamed from: com.android.server.autofill.-$Lambda$JYqZriexGNVTrQ5cwTlcgjPSZFY$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;

        private final /* synthetic */ void $m$0() {
            ((RemoteFillService) this.-$f2).lambda$-com_android_server_autofill_RemoteFillService_10547((PendingRequest) this.-$f3, this.-$f0, (FillResponse) this.-$f4, this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(int i, int i2, Object obj, Object obj2, Object obj3) {
            this.-$f0 = i;
            this.-$f1 = i2;
            this.-$f2 = obj;
            this.-$f3 = obj2;
            this.-$f4 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        ((RemoteFillService) this.-$f0).lambda$-com_android_server_autofill_RemoteFillService_10952((PendingRequest) this.-$f1, (CharSequence) this.-$f2);
    }

    private final /* synthetic */ void $m$1() {
        ((RemoteFillService) this.-$f0).lambda$-com_android_server_autofill_RemoteFillService_11954((PendingRequest) this.-$f1, (CharSequence) this.-$f2);
    }

    public /* synthetic */ -$Lambda$JYqZriexGNVTrQ5cwTlcgjPSZFY(byte b, Object obj, Object obj2, Object obj3) {
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
