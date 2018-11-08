package com.android.server.broadcastradio;

import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;

final /* synthetic */ class -$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI implements RunnableThrowingRemoteException {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.broadcastradio.-$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI$1 */
    final /* synthetic */ class AnonymousClass1 implements RunnableThrowingRemoteException {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TunerCallback) this.-$f0).lambda$-com_android_server_broadcastradio_TunerCallback_2650((BandConfig) this.-$f1);
        }

        private final /* synthetic */ void $m$1() {
            ((TunerCallback) this.-$f0).lambda$-com_android_server_broadcastradio_TunerCallback_2820((ProgramInfo) this.-$f1);
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

    /* renamed from: com.android.server.broadcastradio.-$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI$2 */
    final /* synthetic */ class AnonymousClass2 implements RunnableThrowingRemoteException {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TunerCallback) this.-$f1).lambda$-com_android_server_broadcastradio_TunerCallback_2499(this.-$f0);
        }

        public /* synthetic */ AnonymousClass2(int i, Object obj) {
            this.-$f0 = i;
            this.-$f1 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.broadcastradio.-$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI$3 */
    final /* synthetic */ class AnonymousClass3 implements RunnableThrowingRemoteException {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((TunerCallback) this.-$f1).lambda$-com_android_server_broadcastradio_TunerCallback_3268(this.-$f0);
        }

        private final /* synthetic */ void $m$1() {
            ((TunerCallback) this.-$f1).lambda$-com_android_server_broadcastradio_TunerCallback_3430(this.-$f0);
        }

        private final /* synthetic */ void $m$2() {
            ((TunerCallback) this.-$f1).lambda$-com_android_server_broadcastradio_TunerCallback_3122(this.-$f0);
        }

        private final /* synthetic */ void $m$3() {
            ((TunerCallback) this.-$f1).lambda$-com_android_server_broadcastradio_TunerCallback_2972(this.-$f0);
        }

        public /* synthetic */ AnonymousClass3(byte b, boolean z, Object obj) {
            this.$id = b;
            this.-$f0 = z;
            this.-$f1 = obj;
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

    private final /* synthetic */ void $m$0() {
        ((TunerCallback) this.-$f0).lambda$-com_android_server_broadcastradio_TunerCallback_3585();
    }

    private final /* synthetic */ void $m$1() {
        ((TunerCallback) this.-$f0).lambda$-com_android_server_broadcastradio_TunerCallback_3715();
    }

    public /* synthetic */ -$Lambda$UibxWVH8zVvcNBN03iM01Oc7JJI(byte b, Object obj) {
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
