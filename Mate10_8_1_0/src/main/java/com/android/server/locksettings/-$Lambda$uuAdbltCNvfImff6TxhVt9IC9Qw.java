package com.android.server.locksettings;

import android.app.admin.PasswordMetrics;

final /* synthetic */ class -$Lambda$uuAdbltCNvfImff6TxhVt9IC9Qw implements Runnable {
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;

    /* renamed from: com.android.server.locksettings.-$Lambda$uuAdbltCNvfImff6TxhVt9IC9Qw$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((LockSettingsService) this.-$f1).lambda$-com_android_server_locksettings_LockSettingsService_91606((PasswordMetrics) this.-$f2, this.-$f0);
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

    private final /* synthetic */ void $m$0() {
        ((LockSettingsService) this.-$f1).lambda$-com_android_server_locksettings_LockSettingsService_92154(this.-$f0);
    }

    public /* synthetic */ -$Lambda$uuAdbltCNvfImff6TxhVt9IC9Qw(int i, Object obj) {
        this.-$f0 = i;
        this.-$f1 = obj;
    }

    public final void run() {
        $m$0();
    }
}
