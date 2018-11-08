package com.android.server.om;

final /* synthetic */ class -$Lambda$Whs3NIaASrs6bpQxTTs9leTDPyo implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.om.-$Lambda$Whs3NIaASrs6bpQxTTs9leTDPyo$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((OverlayChangeListener) this.-$f1).lambda$-com_android_server_om_OverlayManagerService$OverlayChangeListener_30962(this.-$f0, (String) this.-$f2);
        }

        private final /* synthetic */ void $m$1() {
            ((OverlayChangeListener) this.-$f1).lambda$-com_android_server_om_OverlayManagerService$OverlayChangeListener_32021(this.-$f0, (String) this.-$f2);
        }

        public /* synthetic */ AnonymousClass1(byte b, int i, Object obj, Object obj2) {
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
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ void $m$0() {
        ((OverlayManagerService) this.-$f0).lambda$-com_android_server_om_OverlayManagerService_10254();
    }

    private final /* synthetic */ void $m$1() {
        ((OverlayManagerService) this.-$f0).lambda$-com_android_server_om_OverlayManagerService_36834();
    }

    public /* synthetic */ -$Lambda$Whs3NIaASrs6bpQxTTs9leTDPyo(byte b, Object obj) {
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
            default:
                throw new AssertionError();
        }
    }
}
