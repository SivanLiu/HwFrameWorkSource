package com.android.server.wm;

import android.util.SparseIntArray;
import android.view.WindowManagerPolicy;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$qRW_P-TWddDPPnAT8S1SNpM72ho implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_140556((WindowManagerPolicy) this.-$f1, (WindowState) arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_141225((WindowManagerPolicy) this.-$f1, (WindowState) arg0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        ((RootWindowContainer) this.-$f0).lambda$-com_android_server_wm_RootWindowContainer_28065((SparseIntArray) this.-$f1, (WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$qRW_P-TWddDPPnAT8S1SNpM72ho(byte b, Object obj, Object obj2) {
        this.$id = b;
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final void accept(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(obj);
                return;
            case (byte) 1:
                $m$1(obj);
                return;
            case (byte) 2:
                $m$2(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
