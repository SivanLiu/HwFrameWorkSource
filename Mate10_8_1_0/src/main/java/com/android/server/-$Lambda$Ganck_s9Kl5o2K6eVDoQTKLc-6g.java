package com.android.server;

import android.content.Context;
import android.net.Network;

final /* synthetic */ class -$Lambda$Ganck_s9Kl5o2K6eVDoQTKLc-6g implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0() {
        ((ConnectivityService) this.-$f0).lambda$-com_android_server_ConnectivityService_126257((Network) this.-$f1);
    }

    private final /* synthetic */ void $m$1() {
        ((ContextHubSystemService) this.-$f0).lambda$-com_android_server_ContextHubSystemService_1237((Context) this.-$f1);
    }

    private final /* synthetic */ void $m$2() {
        SystemServerInitThreadPool.lambda$-com_android_server_SystemServerInitThreadPool_2249((String) this.-$f0, (Runnable) this.-$f1);
    }

    public /* synthetic */ -$Lambda$Ganck_s9Kl5o2K6eVDoQTKLc-6g(byte b, Object obj, Object obj2) {
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
            case (byte) 2:
                $m$2();
                return;
            default:
                throw new AssertionError();
        }
    }
}
