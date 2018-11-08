package com.android.server.wm;

import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$YIZfR4m-B8z_tYbP2x4OJ3o7OYE implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0(Object arg0) {
        MagnifiedViewport.lambda$-com_android_server_wm_AccessibilityController$DisplayMagnifier$MagnifiedViewport_30521((SparseArray) this.-$f0, (WindowState) arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        WindowsForAccessibilityObserver.lambda$-com_android_server_wm_AccessibilityController$WindowsForAccessibilityObserver_62363((SparseArray) this.-$f0, (WindowState) arg0);
    }

    private final /* synthetic */ void $m$10(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_34516((WindowState) arg0);
    }

    private final /* synthetic */ void $m$11(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_119877((WindowState) arg0);
    }

    private final /* synthetic */ void $m$12(Object arg0) {
        ((WindowState) arg0).mWinAnimator.enableSurfaceTrace((FileDescriptor) this.-$f0);
    }

    private final /* synthetic */ void $m$13(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_166045((WindowState) arg0);
    }

    private final /* synthetic */ void $m$14(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_139788((WindowState) arg0);
    }

    private final /* synthetic */ void $m$15(Object arg0) {
        ((RootWindowContainer) this.-$f0).lambda$-com_android_server_wm_RootWindowContainer_8867((WindowState) arg0);
    }

    private final /* synthetic */ void $m$16(Object arg0) {
        ((RootWindowContainer) this.-$f0).lambda$-com_android_server_wm_RootWindowContainer_24761((WindowState) arg0);
    }

    private final /* synthetic */ void $m$17(Object arg0) {
        ((TaskSnapshotController) this.-$f0).lambda$-com_android_server_wm_TaskSnapshotController_15519((Task) arg0);
    }

    private final /* synthetic */ void $m$18(Object arg0) {
        ((WindowLayersController) this.-$f0).lambda$-com_android_server_wm_WindowLayersController_4495((WindowState) arg0);
    }

    private final /* synthetic */ void $m$19(Object arg0) {
        ((PrintWriter) this.-$f0).println((WindowState) arg0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        ((WindowManagerService) this.-$f0).makeWindowFreezingScreenIfNeededLocked((WindowState) arg0);
    }

    private final /* synthetic */ void $m$20(Object arg0) {
        ((ArrayList) this.-$f0).add((WindowState) arg0);
    }

    private final /* synthetic */ void $m$3(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_19596((WindowState) arg0);
    }

    private final /* synthetic */ void $m$4(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_21909((WindowState) arg0);
    }

    private final /* synthetic */ void $m$5(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_23858((WindowState) arg0);
    }

    private final /* synthetic */ void $m$6(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_26906((WindowState) arg0);
    }

    private final /* synthetic */ void $m$7(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_27326((WindowState) arg0);
    }

    private final /* synthetic */ void $m$8(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_32287((WindowState) arg0);
    }

    private final /* synthetic */ void $m$9(Object arg0) {
        ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_34311((WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$YIZfR4m-B8z_tYbP2x4OJ3o7OYE(byte b, Object obj) {
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
            case (byte) 7:
                $m$7(obj);
                return;
            case (byte) 8:
                $m$8(obj);
                return;
            case (byte) 9:
                $m$9(obj);
                return;
            case (byte) 10:
                $m$10(obj);
                return;
            case (byte) 11:
                $m$11(obj);
                return;
            case (byte) 12:
                $m$12(obj);
                return;
            case (byte) 13:
                $m$13(obj);
                return;
            case (byte) 14:
                $m$14(obj);
                return;
            case (byte) 15:
                $m$15(obj);
                return;
            case (byte) 16:
                $m$16(obj);
                return;
            case (byte) 17:
                $m$17(obj);
                return;
            case (byte) 18:
                $m$18(obj);
                return;
            case (byte) 19:
                $m$19(obj);
                return;
            case (byte) 20:
                $m$20(obj);
                return;
            default:
                throw new AssertionError();
        }
    }
}
