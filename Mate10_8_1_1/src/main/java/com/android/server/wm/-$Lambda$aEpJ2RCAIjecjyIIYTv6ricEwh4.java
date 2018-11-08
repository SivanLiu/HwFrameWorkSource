package com.android.server.wm;

import android.view.WindowManagerPolicy.StartingSurface;

final /* synthetic */ class -$Lambda$aEpJ2RCAIjecjyIIYTv6ricEwh4 implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((AlertWindowNotification) this.-$f0).-com_android_server_wm_AlertWindowNotification-mthref-1();
    }

    private final /* synthetic */ void $m$1() {
        ((AlertWindowNotification) this.-$f0).-com_android_server_wm_AlertWindowNotification-mthref-0();
    }

    private final /* synthetic */ void $m$10() {
        ((UnknownAppVisibilityController) this.-$f0).-com_android_server_wm_UnknownAppVisibilityController-mthref-0();
    }

    private final /* synthetic */ void $m$11() {
        ((WindowAnimator) this.-$f0).lambda$-com_android_server_wm_WindowAnimator_4391();
    }

    private final /* synthetic */ void $m$12() {
        ((WindowSurfacePlacer) this.-$f0).lambda$-com_android_server_wm_WindowSurfacePlacer_6429();
    }

    private final /* synthetic */ void $m$2() {
        ((AppWindowContainerController) this.-$f0).lambda$-com_android_server_wm_AppWindowContainerController_4943();
    }

    private final /* synthetic */ void $m$3() {
        ((AppWindowContainerController) this.-$f0).lambda$-com_android_server_wm_AppWindowContainerController_5234();
    }

    private final /* synthetic */ void $m$4() {
        ((AppWindowContainerController) this.-$f0).lambda$-com_android_server_wm_AppWindowContainerController_5523();
    }

    private final /* synthetic */ void $m$5() {
        AppWindowContainerController.lambda$-com_android_server_wm_AppWindowContainerController_37095((StartingSurface) this.-$f0);
    }

    private final /* synthetic */ void $m$6() {
        ((BoundsAnimator) this.-$f0).lambda$-com_android_server_wm_BoundsAnimationController$BoundsAnimator_7595();
    }

    private final /* synthetic */ void $m$7() {
        ((BoundsAnimationController) this.-$f0).-com_android_server_wm_BoundsAnimationController-mthref-0();
    }

    private final /* synthetic */ void $m$8() {
        ((TaskSnapshotSurface) this.-$f0).remove();
    }

    private final /* synthetic */ void $m$9() {
        ((TaskSnapshotSurface) this.-$f0).-com_android_server_wm_TaskSnapshotSurface-mthref-0();
    }

    public /* synthetic */ -$Lambda$aEpJ2RCAIjecjyIIYTv6ricEwh4(byte b, Object obj) {
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
            case (byte) 5:
                $m$5();
                return;
            case (byte) 6:
                $m$6();
                return;
            case (byte) 7:
                $m$7();
                return;
            case (byte) 8:
                $m$8();
                return;
            case (byte) 9:
                $m$9();
                return;
            case (byte) 10:
                $m$10();
                return;
            case (byte) 11:
                $m$11();
                return;
            case (byte) 12:
                $m$12();
                return;
            default:
                throw new AssertionError();
        }
    }
}
