package com.android.server.wifi;

import android.os.Handler.Callback;
import android.os.Message;

final /* synthetic */ class -$Lambda$2ItOdD6JCrqoTLRQamUOtCfYAAM implements Callback {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.wifi.-$Lambda$2ItOdD6JCrqoTLRQamUOtCfYAAM$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((OpenNetworkNotifier) this.-$f0).lambda$-com_android_server_wifi_OpenNetworkNotifier_16544();
        }

        private final /* synthetic */ void $m$1() {
            ((OpenNetworkNotifier) this.-$f0).lambda$-com_android_server_wifi_OpenNetworkNotifier_13963();
        }

        private final /* synthetic */ void $m$2() {
            ((OpenNetworkNotifier) this.-$f0).lambda$-com_android_server_wifi_OpenNetworkNotifier_13081();
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
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

    private final /* synthetic */ boolean $m$0(Message arg0) {
        return ((OpenNetworkNotifier) this.-$f0).lambda$-com_android_server_wifi_OpenNetworkNotifier_8777(arg0);
    }

    public /* synthetic */ -$Lambda$2ItOdD6JCrqoTLRQamUOtCfYAAM(Object obj) {
        this.-$f0 = obj;
    }

    public final boolean handleMessage(Message message) {
        return $m$0(message);
    }
}
