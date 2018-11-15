package com.android.server.autofill;

import android.content.IntentSender;
import android.os.ICancellationSignal;

final /* synthetic */ class -$Lambda$tQjSpU6IVjrOfYzILn21rTYl4Vo implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.autofill.-$Lambda$tQjSpU6IVjrOfYzILn21rTYl4Vo$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((RemoteFillService) this.-$f0).lambda$-com_android_server_autofill_RemoteFillService_11609((PendingRequest) this.-$f1);
        }

        private final /* synthetic */ void $m$1() {
            ((Session) this.-$f0).lambda$-com_android_server_autofill_Session_27469((IntentSender) this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
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

    private final /* synthetic */ void $m$0() {
        ((PendingRequest) this.-$f0).lambda$-com_android_server_autofill_RemoteFillService$PendingRequest_16211();
    }

    private final /* synthetic */ void $m$1() {
        RemoteFillService.lambda$-com_android_server_autofill_RemoteFillService_11273((ICancellationSignal) this.-$f0);
    }

    private final /* synthetic */ void $m$2() {
        ((Session) this.-$f0).lambda$-com_android_server_autofill_Session_25476();
    }

    public /* synthetic */ -$Lambda$tQjSpU6IVjrOfYzILn21rTYl4Vo(byte b, Object obj) {
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
