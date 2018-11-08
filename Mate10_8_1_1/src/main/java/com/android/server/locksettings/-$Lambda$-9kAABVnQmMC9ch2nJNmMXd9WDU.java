package com.android.server.locksettings;

import android.hardware.weaver.V1_0.IWeaver.getConfigCallback;
import android.hardware.weaver.V1_0.IWeaver.readCallback;
import android.hardware.weaver.V1_0.WeaverConfig;
import android.hardware.weaver.V1_0.WeaverReadResponse;
import com.android.internal.widget.VerifyCredentialResponse;

final /* synthetic */ class -$Lambda$-9kAABVnQmMC9ch2nJNmMXd9WDU implements getConfigCallback {
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.locksettings.-$Lambda$-9kAABVnQmMC9ch2nJNmMXd9WDU$1 */
    final /* synthetic */ class AnonymousClass1 implements readCallback {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0(int arg0, WeaverReadResponse arg1) {
            SyntheticPasswordManager.lambda$-com_android_server_locksettings_SyntheticPasswordManager_14916((VerifyCredentialResponse[]) this.-$f1, this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass1(int i, Object obj) {
            this.-$f0 = i;
            this.-$f1 = obj;
        }

        public final void onValues(int i, WeaverReadResponse weaverReadResponse) {
            $m$0(i, weaverReadResponse);
        }
    }

    private final /* synthetic */ void $m$0(int arg0, WeaverConfig arg1) {
        ((SyntheticPasswordManager) this.-$f0).lambda$-com_android_server_locksettings_SyntheticPasswordManager_12018(arg0, arg1);
    }

    public /* synthetic */ -$Lambda$-9kAABVnQmMC9ch2nJNmMXd9WDU(Object obj) {
        this.-$f0 = obj;
    }

    public final void onValues(int i, WeaverConfig weaverConfig) {
        $m$0(i, weaverConfig);
    }
}
