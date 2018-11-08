package com.android.server.wm;

import java.util.Comparator;

final /* synthetic */ class -$Lambda$LEqle-ue9vesHjZva-SwvAvwBx8 implements Comparator {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return ((NonAppWindowContainers) this.-$f0).lambda$-com_android_server_wm_DisplayContent$NonAppWindowContainers_179921((WindowToken) arg0, (WindowToken) arg1);
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return ((WindowToken) this.-$f0).lambda$-com_android_server_wm_WindowToken_3754((WindowState) arg0, (WindowState) arg1);
    }

    public /* synthetic */ -$Lambda$LEqle-ue9vesHjZva-SwvAvwBx8(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final int compare(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
