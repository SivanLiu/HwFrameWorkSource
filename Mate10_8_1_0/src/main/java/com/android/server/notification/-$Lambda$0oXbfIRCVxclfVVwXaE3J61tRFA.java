package com.android.server.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.os.UserHandle;
import android.os.VibrationEffect;
import com.android.server.notification.ManagedServices.ManagedServiceInfo;
import com.android.server.notification.NotificationManagerService.AnonymousClass16;
import com.android.server.notification.NotificationManagerService.AnonymousClass17;
import com.android.server.notification.NotificationManagerService.NotificationListeners;

final /* synthetic */ class -$Lambda$0oXbfIRCVxclfVVwXaE3J61tRFA implements FlagChecker {
    public static final /* synthetic */ -$Lambda$0oXbfIRCVxclfVVwXaE3J61tRFA $INST$0 = new -$Lambda$0oXbfIRCVxclfVVwXaE3J61tRFA();

    /* renamed from: com.android.server.notification.-$Lambda$0oXbfIRCVxclfVVwXaE3J61tRFA$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((NotificationManagerService) this.-$f0).lambda$-com_android_server_notification_NotificationManagerService_231523((NotificationRecord) this.-$f1, (VibrationEffect) this.-$f2);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2, Object obj3) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.notification.-$Lambda$0oXbfIRCVxclfVVwXaE3J61tRFA$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;

        private final /* synthetic */ void $m$0() {
            ((NotificationListeners) this.-$f1).lambda$-com_android_server_notification_NotificationManagerService$NotificationListeners_300387((ManagedServiceInfo) this.-$f2, (String) this.-$f3, (UserHandle) this.-$f4, (NotificationChannel) this.-$f5, this.-$f0);
        }

        private final /* synthetic */ void $m$1() {
            ((NotificationListeners) this.-$f1).lambda$-com_android_server_notification_NotificationManagerService$NotificationListeners_301421((ManagedServiceInfo) this.-$f2, (String) this.-$f3, (UserHandle) this.-$f4, (NotificationChannelGroup) this.-$f5, this.-$f0);
        }

        public /* synthetic */ AnonymousClass2(byte b, int i, Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
            this.$id = b;
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
            this.-$f3 = obj3;
            this.-$f4 = obj4;
            this.-$f5 = obj5;
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

    /* renamed from: com.android.server.notification.-$Lambda$0oXbfIRCVxclfVVwXaE3J61tRFA$3 */
    final /* synthetic */ class AnonymousClass3 implements FlagChecker {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;

        private final /* synthetic */ boolean $m$0(int arg0) {
            return AnonymousClass16.lambda$-com_android_server_notification_NotificationManagerService$16_261659(this.-$f0, this.-$f1, arg0);
        }

        public /* synthetic */ AnonymousClass3(int i, int i2) {
            this.-$f0 = i;
            this.-$f1 = i2;
        }

        public final boolean apply(int i) {
            return $m$0(i);
        }
    }

    private final /* synthetic */ boolean $m$0(int arg0) {
        return AnonymousClass17.lambda$-com_android_server_notification_NotificationManagerService$17_267457(arg0);
    }

    private /* synthetic */ -$Lambda$0oXbfIRCVxclfVVwXaE3J61tRFA() {
    }

    public final boolean apply(int i) {
        return $m$0(i);
    }
}
