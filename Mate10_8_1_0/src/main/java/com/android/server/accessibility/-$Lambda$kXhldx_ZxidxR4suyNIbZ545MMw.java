package com.android.server.accessibility;

import android.view.accessibility.IAccessibilityManagerClient;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$kXhldx_ZxidxR4suyNIbZ545MMw implements Consumer {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ int -$f0;

    /* renamed from: com.android.server.accessibility.-$Lambda$kXhldx_ZxidxR4suyNIbZ545MMw$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((AccessibilityManagerService) this.-$f1).lambda$-com_android_server_accessibility_AccessibilityManagerService_63212((UserState) this.-$f2, this.-$f0);
        }

        public /* synthetic */ AnonymousClass1(int i, Object obj, Object obj2) {
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0(Object arg0) {
        MainHandler.lambda$-com_android_server_accessibility_AccessibilityManagerService$MainHandler_113703(this.-$f0, (IAccessibilityManagerClient) arg0);
    }

    private final /* synthetic */ void $m$1(Object arg0) {
        MainHandler.lambda$-com_android_server_accessibility_AccessibilityManagerService$MainHandler_115952(this.-$f0, (IAccessibilityManagerClient) arg0);
    }

    private final /* synthetic */ void $m$2(Object arg0) {
        AccessibilityManagerService.lambda$-com_android_server_accessibility_AccessibilityManagerService_63266(this.-$f0, (IAccessibilityManagerClient) arg0);
    }

    public /* synthetic */ -$Lambda$kXhldx_ZxidxR4suyNIbZ545MMw(byte b, int i) {
        this.$id = b;
        this.-$f0 = i;
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
            default:
                throw new AssertionError();
        }
    }
}
