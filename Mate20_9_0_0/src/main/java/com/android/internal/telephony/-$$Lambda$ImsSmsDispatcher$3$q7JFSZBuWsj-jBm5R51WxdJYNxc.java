package com.android.internal.telephony;

import android.telephony.SmsMessage;
import com.android.internal.telephony.ImsSmsDispatcher.AnonymousClass3;
import com.android.internal.telephony.SmsDispatchersController.SmsInjectionCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsSmsDispatcher$3$q7JFSZBuWsj-jBm5R51WxdJYNxc implements SmsInjectionCallback {
    private final /* synthetic */ AnonymousClass3 f$0;
    private final /* synthetic */ SmsMessage f$1;
    private final /* synthetic */ int f$2;

    public /* synthetic */ -$$Lambda$ImsSmsDispatcher$3$q7JFSZBuWsj-jBm5R51WxdJYNxc(AnonymousClass3 anonymousClass3, SmsMessage smsMessage, int i) {
        this.f$0 = anonymousClass3;
        this.f$1 = smsMessage;
        this.f$2 = i;
    }

    public final void onSmsInjectedResult(int i) {
        AnonymousClass3.lambda$onSmsReceived$0(this.f$0, this.f$1, this.f$2, i);
    }
}
