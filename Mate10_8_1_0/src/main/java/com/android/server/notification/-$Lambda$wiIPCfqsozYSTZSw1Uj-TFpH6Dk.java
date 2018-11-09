package com.android.server.notification;

import android.content.ComponentName;
import java.util.function.Function;

final /* synthetic */ class -$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk implements Function {
    public static final /* synthetic */ -$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk $INST$0 = new -$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk();

    /* renamed from: com.android.server.notification.-$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk$2 */
    final /* synthetic */ class AnonymousClass2 implements Function {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ Object $m$0(Object arg0) {
            return ((ManagedServices) this.-$f0).-com_android_server_notification_ManagedServices-mthref-1((String) arg0);
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final Object apply(Object obj) {
            return $m$0(obj);
        }
    }

    private final /* synthetic */ Object $m$0(Object arg0) {
        return ComponentName.unflattenFromString((String) arg0);
    }

    private /* synthetic */ -$Lambda$wiIPCfqsozYSTZSw1Uj-TFpH6Dk() {
    }

    public final Object apply(Object obj) {
        return $m$0(obj);
    }
}
