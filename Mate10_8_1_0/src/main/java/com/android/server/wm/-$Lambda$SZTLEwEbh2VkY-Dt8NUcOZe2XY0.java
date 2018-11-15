package com.android.server.wm;

import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0 implements Consumer {
    public static final /* synthetic */ -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0 $INST$0 = new -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0((byte) 0);
    public static final /* synthetic */ -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0 $INST$1 = new -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0((byte) 1);
    public static final /* synthetic */ -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0 $INST$2 = new -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0((byte) 2);
    public static final /* synthetic */ -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0 $INST$3 = new -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0((byte) 3);
    public static final /* synthetic */ -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0 $INST$4 = new -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0((byte) 4);
    public static final /* synthetic */ -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0 $INST$5 = new -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0((byte) 5);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ void $m$0(Object arg0) {
        ((WindowState) arg0).mWinAnimator.disableSurfaceTrace();
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        ((WindowState) arg0).resetDragResizingChangeReported();
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        RootWindowContainer.lambda$-com_android_server_wm_RootWindowContainer_9148((WindowState) arg0);
    }

    private final /* synthetic */ void $m$3(Object arg0) {
        ((WindowState) arg0).mWinAnimator.resetDrawState();
    }

    private final /* synthetic */ void $m$4(Object arg0) {
        WindowAnimator.lambda$-com_android_server_wm_WindowAnimator_20360((WindowState) arg0);
    }

    private final /* synthetic */ void $m$5(Object arg0) {
        WindowLayersController.lambda$-com_android_server_wm_WindowLayersController_7016((WindowState) arg0);
    }

    private /* synthetic */ -$Lambda$SZTLEwEbh2VkY-Dt8NUcOZe2XY0(byte b) {
        this.$id = b;
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
            default:
                throw new AssertionError();
        }
    }
}
