package com.android.server.wm;

import android.content.Context;
import android.view.WindowManagerPolicy;
import com.android.server.input.InputManagerService;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$eBBEuGZ8VbEXJy0r5EYYbvnl-8w implements Consumer {
    private final /* synthetic */ boolean -$f0;

    /* renamed from: com.android.server.wm.-$Lambda$eBBEuGZ8VbEXJy0r5EYYbvnl-8w$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ boolean -$f1;
        private final /* synthetic */ boolean -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;

        private final /* synthetic */ void $m$0() {
            WindowManagerService.lambda$-com_android_server_wm_WindowManagerService_49356((Context) this.-$f3, (InputManagerService) this.-$f4, this.-$f0, this.-$f1, this.-$f2, (WindowManagerPolicy) this.-$f5);
        }

        public /* synthetic */ AnonymousClass1(boolean z, boolean z2, boolean z3, Object obj, Object obj2, Object obj3) {
            this.-$f0 = z;
            this.-$f1 = z2;
            this.-$f2 = z3;
            this.-$f3 = obj;
            this.-$f4 = obj2;
            this.-$f5 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        ((WindowState) arg0).setForceHideNonSystemOverlayWindowIfNeeded(this.-$f0);
    }

    public /* synthetic */ -$Lambda$eBBEuGZ8VbEXJy0r5EYYbvnl-8w(boolean z) {
        this.-$f0 = z;
    }

    public final void accept(Object obj) {
        $m$0(obj);
    }
}
