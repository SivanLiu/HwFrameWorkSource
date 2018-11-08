package com.android.server.pm;

import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$QyqU-JNCRoGQda4e7us8wqygKfQ implements Consumer {
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    /* renamed from: com.android.server.pm.-$Lambda$QyqU-JNCRoGQda4e7us8wqygKfQ$1 */
    final /* synthetic */ class AnonymousClass1 implements Consumer {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((ShortcutUser) this.-$f0).lambda$-com_android_server_pm_ShortcutUser_18707((ShortcutService) this.-$f1, (int[]) this.-$f2, (int[]) this.-$f3, (ShortcutPackage) arg0);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2, Object obj3, Object obj4) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
            this.-$f3 = obj4;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        ((ShortcutUser) this.-$f0).lambda$-com_android_server_pm_ShortcutUser_18284((ShortcutService) this.-$f1, (int[]) this.-$f2, (ShortcutLauncher) arg0);
    }

    public /* synthetic */ -$Lambda$QyqU-JNCRoGQda4e7us8wqygKfQ(Object obj, Object obj2, Object obj3) {
        this.-$f0 = obj;
        this.-$f1 = obj2;
        this.-$f2 = obj3;
    }

    public final void accept(Object obj) {
        $m$0(obj);
    }
}
