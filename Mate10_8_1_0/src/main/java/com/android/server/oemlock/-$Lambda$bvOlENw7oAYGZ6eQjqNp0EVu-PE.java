package com.android.server.oemlock;

import android.hardware.oemlock.V1_0.IOemLock.isOemUnlockAllowedByCarrierCallback;
import android.hardware.oemlock.V1_0.IOemLock.isOemUnlockAllowedByDeviceCallback;

final /* synthetic */ class -$Lambda$bvOlENw7oAYGZ6eQjqNp0EVu-PE implements isOemUnlockAllowedByCarrierCallback {
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;

    /* renamed from: com.android.server.oemlock.-$Lambda$bvOlENw7oAYGZ6eQjqNp0EVu-PE$1 */
    final /* synthetic */ class AnonymousClass1 implements isOemUnlockAllowedByDeviceCallback {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(int arg0, boolean arg1) {
            VendorLock.lambda$-com_android_server_oemlock_VendorLock_4760((Integer[]) this.-$f0, (Boolean[]) this.-$f1, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void onValues(int i, boolean z) {
            $m$0(i, z);
        }
    }

    private final /* synthetic */ void $m$0(int arg0, boolean arg1) {
        VendorLock.lambda$-com_android_server_oemlock_VendorLock_2991((Integer[]) this.-$f0, (Boolean[]) this.-$f1, arg0, arg1);
    }

    public /* synthetic */ -$Lambda$bvOlENw7oAYGZ6eQjqNp0EVu-PE(Object obj, Object obj2) {
        this.-$f0 = obj;
        this.-$f1 = obj2;
    }

    public final void onValues(int i, boolean z) {
        $m$0(i, z);
    }
}
