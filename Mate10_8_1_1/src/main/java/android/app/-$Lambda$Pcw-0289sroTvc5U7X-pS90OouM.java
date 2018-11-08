package android.app;

import android.app.EnterTransitionCoordinator.AnonymousClass3;
import android.os.Bundle;
import android.util.ArrayMap;
import java.util.ArrayList;

final /* synthetic */ class -$Lambda$Pcw-0289sroTvc5U7X-pS90OouM implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0() {
        ((ActivityTransitionCoordinator) this.-$f0).lambda$-android_app_ActivityTransitionCoordinator_27512((ArrayList) this.-$f1);
    }

    private final /* synthetic */ void $m$1() {
        ((ActivityTransitionState) this.-$f0).lambda$-android_app_ActivityTransitionState_12157((Activity) this.-$f1);
    }

    private final /* synthetic */ void $m$2() {
        ((AnonymousClass3) this.-$f0).lambda$-android_app_EnterTransitionCoordinator$3_18171((Bundle) this.-$f1);
    }

    private final /* synthetic */ void $m$3() {
        ((AnonymousClass3) this.-$f0).lambda$-android_app_EnterTransitionCoordinator$3_18123((Bundle) this.-$f1);
    }

    private final /* synthetic */ void $m$4() {
        ((EnterTransitionCoordinator) this.-$f0).lambda$-android_app_EnterTransitionCoordinator_6461((ArrayMap) this.-$f1);
    }

    private final /* synthetic */ void $m$5() {
        ((ExitTransitionCoordinator) this.-$f0).lambda$-android_app_ExitTransitionCoordinator_6416((ArrayList) this.-$f1);
    }

    public /* synthetic */ -$Lambda$Pcw-0289sroTvc5U7X-pS90OouM(byte b, Object obj, Object obj2) {
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
            case (byte) 4:
                $m$4();
                return;
            case (byte) 5:
                $m$5();
                return;
            default:
                throw new AssertionError();
        }
    }
}
