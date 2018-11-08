package com.android.internal.telephony.euicc;

import android.app.PendingIntent;
import android.content.Intent;

final /* synthetic */ class -$Lambda$PFBaWbKaV1GGSjJwCriLWFneaBs implements Runnable {
    private final /* synthetic */ int -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;
    private final /* synthetic */ Object -$f3;

    private final /* synthetic */ void $m$0() {
        ((EuiccController) this.-$f1).lambda$-com_android_internal_telephony_euicc_EuiccController_38558((PendingIntent) this.-$f2, this.-$f0, (Intent) this.-$f3);
    }

    public /* synthetic */ -$Lambda$PFBaWbKaV1GGSjJwCriLWFneaBs(int i, Object obj, Object obj2, Object obj3) {
        this.-$f0 = i;
        this.-$f1 = obj;
        this.-$f2 = obj2;
        this.-$f3 = obj3;
    }

    public final void run() {
        $m$0();
    }
}
