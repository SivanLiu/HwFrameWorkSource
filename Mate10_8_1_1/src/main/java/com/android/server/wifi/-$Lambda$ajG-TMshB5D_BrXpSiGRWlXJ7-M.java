package com.android.server.wifi;

import java.util.ArrayList;

final /* synthetic */ class -$Lambda$ajG-TMshB5D_BrXpSiGRWlXJ7-M implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    private final /* synthetic */ void $m$0() {
        ((WifiServiceImpl) this.-$f1).lambda$-com_android_server_wifi_WifiServiceImpl_83566((String) this.-$f2, this.-$f0);
    }

    private final /* synthetic */ void $m$1() {
        ((ChipEventCallback) this.-$f1).lambda$-com_android_server_wifi_WifiVendorHal$ChipEventCallback_102324((ArrayList) this.-$f2, this.-$f0);
    }

    public /* synthetic */ -$Lambda$ajG-TMshB5D_BrXpSiGRWlXJ7-M(byte b, int i, Object obj, Object obj2) {
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
