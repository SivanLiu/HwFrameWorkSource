package com.android.server.wifi.p2p;

import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import com.android.server.wifi.HalDeviceManager.InterfaceDestroyedListener;

final /* synthetic */ class -$Lambda$fn1kZ8DxUVXwaqX8IUvJlmRRm00 implements InterfaceDestroyedListener {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.wifi.p2p.-$Lambda$fn1kZ8DxUVXwaqX8IUvJlmRRm00$1 */
    final /* synthetic */ class AnonymousClass1 implements DeathRecipient {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((WifiP2pServiceImpl) this.-$f0).lambda$-com_android_server_wifi_p2p_WifiP2pServiceImpl_28131((IBinder) this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void binderDied() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        ((WifiP2pServiceImpl) this.-$f0).lambda$-com_android_server_wifi_p2p_WifiP2pServiceImpl_28963();
    }

    public /* synthetic */ -$Lambda$fn1kZ8DxUVXwaqX8IUvJlmRRm00(Object obj) {
        this.-$f0 = obj;
    }

    public final void onDestroyed() {
        $m$0();
    }
}
