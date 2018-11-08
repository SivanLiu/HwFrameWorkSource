package android.os;

import android.opengl.EGL14;

final /* synthetic */ class -$Lambda$BcGBlsGjMZMF6Ej78rWJ608MYSM implements Runnable {
    public static final /* synthetic */ -$Lambda$BcGBlsGjMZMF6Ej78rWJ608MYSM $INST$0 = new -$Lambda$BcGBlsGjMZMF6Ej78rWJ608MYSM((byte) 0);
    public static final /* synthetic */ -$Lambda$BcGBlsGjMZMF6Ej78rWJ608MYSM $INST$1 = new -$Lambda$BcGBlsGjMZMF6Ej78rWJ608MYSM((byte) 1);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ void $m$0() {
        EGL14.eglGetDisplay(0);
    }

    private final /* synthetic */ void $m$1() {
        Trace.lambda$-android_os_Trace_5120();
    }

    private /* synthetic */ -$Lambda$BcGBlsGjMZMF6Ej78rWJ608MYSM(byte b) {
        this.$id = b;
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
