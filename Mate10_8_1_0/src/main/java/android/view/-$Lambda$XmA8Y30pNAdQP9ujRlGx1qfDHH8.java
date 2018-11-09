package android.view;

final /* synthetic */ class -$Lambda$XmA8Y30pNAdQP9ujRlGx1qfDHH8 implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((SurfaceView) this.-$f0).lambda$-android_view_SurfaceView_37955();
    }

    private final /* synthetic */ void $m$1() {
        ((SurfaceView) this.-$f0).-android_view_SurfaceView-mthref-0();
    }

    private final /* synthetic */ void $m$2() {
        ((View) this.-$f0).-android_view_View-mthref-0();
    }

    private final /* synthetic */ void $m$3() {
        ((View) this.-$f0).-android_view_View-mthref-1();
    }

    private final /* synthetic */ void $m$4() {
        ((ViewRootImpl) this.-$f0).-android_view_ViewRootImpl-mthref-0();
    }

    public /* synthetic */ -$Lambda$XmA8Y30pNAdQP9ujRlGx1qfDHH8(byte b, Object obj) {
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
            case (byte) 3:
                $m$3();
                return;
            case (byte) 4:
                $m$4();
                return;
            default:
                throw new AssertionError();
        }
    }
}
