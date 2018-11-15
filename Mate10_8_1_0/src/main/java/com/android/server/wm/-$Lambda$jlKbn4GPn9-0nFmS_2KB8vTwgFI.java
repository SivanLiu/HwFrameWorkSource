package com.android.server.wm;

import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.WindowManagerPolicy.ScreenOffListener;
import com.android.internal.app.IAssistScreenshotReceiver;

final /* synthetic */ class -$Lambda$jlKbn4GPn9-0nFmS_2KB8vTwgFI implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0() {
        ((AppTransition) this.-$f0).lambda$-com_android_server_wm_AppTransition_101764((IAppTransitionAnimationSpecsFuture) this.-$f1);
    }

    private final /* synthetic */ void $m$1() {
        ((TaskSnapshotController) this.-$f0).lambda$-com_android_server_wm_TaskSnapshotController_15354((ScreenOffListener) this.-$f1);
    }

    private final /* synthetic */ void $m$2() {
        ((WindowManagerService) this.-$f0).lambda$-com_android_server_wm_WindowManagerService_166007((Runnable) this.-$f1);
    }

    private final /* synthetic */ void $m$3() {
        ((WindowManagerService) this.-$f0).lambda$-com_android_server_wm_WindowManagerService_201642((IAssistScreenshotReceiver) this.-$f1);
    }

    public /* synthetic */ -$Lambda$jlKbn4GPn9-0nFmS_2KB8vTwgFI(byte b, Object obj, Object obj2) {
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
            case (byte) 3:
                $m$3();
                return;
            default:
                throw new AssertionError();
        }
    }
}
