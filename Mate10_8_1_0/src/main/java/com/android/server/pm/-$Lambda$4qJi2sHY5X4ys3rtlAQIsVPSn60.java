package com.android.server.pm;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$4qJi2sHY5X4ys3rtlAQIsVPSn60 implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0(Object arg0) {
        ((PrintWriter) arg0).println(StandardCharsets.UTF_8.decode(ByteBuffer.wrap((byte[]) this.-$f0)).toString());
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((ShortcutPackage) this.-$f0).lambda$-com_android_server_pm_ShortcutPackage_14748((ShortcutLauncher) arg0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        ((ShortcutService) this.-$f0).lambda$-com_android_server_pm_ShortcutService_127647((PrintWriter) arg0);
    }

    private final /* synthetic */ void $m$3(Object arg0) {
        ((ShortcutService) this.-$f0).-com_android_server_pm_ShortcutService-mthref-4((PrintWriter) arg0);
    }

    private final /* synthetic */ void $m$4(Object arg0) {
        ((ShortcutService) this.-$f0).-com_android_server_pm_ShortcutService-mthref-5((PrintWriter) arg0);
    }

    private final /* synthetic */ void $m$5(Object arg0) {
        ((ShortcutService) this.-$f0).-com_android_server_pm_ShortcutService-mthref-6((PrintWriter) arg0);
    }

    private final /* synthetic */ void $m$6(Object arg0) {
        ((ShortcutService) this.-$f0).lambda$-com_android_server_pm_ShortcutService_128865((PrintWriter) arg0);
    }

    public /* synthetic */ -$Lambda$4qJi2sHY5X4ys3rtlAQIsVPSn60(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
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
            case (byte) 3:
                $m$3(obj);
                return;
            case (byte) 4:
                $m$4(obj);
                return;
            case (byte) 5:
                $m$5(obj);
                return;
            case (byte) 6:
                $m$6(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
