package com.android.internal.telephony;

import android.app.PendingIntent;
import com.android.internal.telephony.SmsDispatchersController.SmsInjectionCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$IccSmsInterfaceManager$rB1zRNxMbL7VadRMSxZ5tebvHwM implements SmsInjectionCallback {
    private final /* synthetic */ PendingIntent f$0;

    public /* synthetic */ -$$Lambda$IccSmsInterfaceManager$rB1zRNxMbL7VadRMSxZ5tebvHwM(PendingIntent pendingIntent) {
        this.f$0 = pendingIntent;
    }

    public final void onSmsInjectedResult(int i) {
        IccSmsInterfaceManager.lambda$injectSmsPdu$0(this.f$0, i);
    }
}
