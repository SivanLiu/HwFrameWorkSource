package com.android.server.wm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$cHAc_wCK_9-nlRTF5Ggz5ZbNDr0 implements Consumer {
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    /* renamed from: com.android.server.wm.-$Lambda$cHAc_wCK_9-nlRTF5Ggz5ZbNDr0$1 */
    final /* synthetic */ class AnonymousClass1 implements Consumer {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0(Object arg0) {
            RootWindowContainer.lambda$-com_android_server_wm_RootWindowContainer_57163((ArrayList) this.-$f1, (PrintWriter) this.-$f2, (int[]) this.-$f3, this.-$f0, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass1(boolean z, Object obj, Object obj2, Object obj3) {
            this.-$f0 = z;
            this.-$f1 = obj;
            this.-$f2 = obj2;
            this.-$f3 = obj3;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    /* renamed from: com.android.server.wm.-$Lambda$cHAc_wCK_9-nlRTF5Ggz5ZbNDr0$2 */
    final /* synthetic */ class AnonymousClass2 implements Consumer {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ int -$f1;

        private final /* synthetic */ void $m$0(Object arg0) {
            RootWindowContainer.lambda$-com_android_server_wm_RootWindowContainer_24496(this.-$f1, this.-$f0, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass2(boolean z, int i) {
            this.-$f0 = z;
            this.-$f1 = i;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        RootWindowContainer.lambda$-com_android_server_wm_RootWindowContainer_19432((String) this.-$f1, (ArrayList) this.-$f2, this.-$f0, (WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$cHAc_wCK_9-nlRTF5Ggz5ZbNDr0(int i, Object obj, Object obj2) {
        this.-$f0 = i;
        this.-$f1 = obj;
        this.-$f2 = obj2;
    }

    public final void accept(Object obj) {
        $m$0(obj);
    }
}
