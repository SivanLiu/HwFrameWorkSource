package com.android.server.broadcastradio;

import android.os.IBinder.DeathRecipient;

final /* synthetic */ class -$Lambda$B3g7x97xEp_kpgRrmZTNuVQljJA implements DeathRecipient {
    private final /* synthetic */ Object -$f0;

    private final /* synthetic */ void $m$0() {
        ((Tuner) this.-$f0).-com_android_server_broadcastradio_Tuner-mthref-0();
    }

    public /* synthetic */ -$Lambda$B3g7x97xEp_kpgRrmZTNuVQljJA(Object obj) {
        this.-$f0 = obj;
    }

    public final void binderDied() {
        $m$0();
    }
}
