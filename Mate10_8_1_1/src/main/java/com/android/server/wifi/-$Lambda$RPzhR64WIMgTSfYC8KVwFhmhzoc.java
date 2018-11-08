package com.android.server.wifi;

import android.os.IBinder.DeathRecipient;

final /* synthetic */ class -$Lambda$RPzhR64WIMgTSfYC8KVwFhmhzoc implements DeathRecipient {
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;

    private final /* synthetic */ void $m$0() {
        ((RttServiceImpl) this.-$f1).lambda$-com_android_server_wifi_RttService$RttServiceImpl_1874(this.-$f0);
    }

    public /* synthetic */ -$Lambda$RPzhR64WIMgTSfYC8KVwFhmhzoc(int i, Object obj) {
        this.-$f0 = i;
        this.-$f1 = obj;
    }

    public final void binderDied() {
        $m$0();
    }
}
