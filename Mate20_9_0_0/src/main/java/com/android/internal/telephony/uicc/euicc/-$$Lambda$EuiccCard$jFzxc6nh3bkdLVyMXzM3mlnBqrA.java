package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$jFzxc6nh3bkdLVyMXzM3mlnBqrA implements ApduExceptionHandler {
    private final /* synthetic */ AsyncResultCallback f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$jFzxc6nh3bkdLVyMXzM3mlnBqrA(AsyncResultCallback asyncResultCallback) {
        this.f$0 = asyncResultCallback;
    }

    public final void handleException(Throwable th) {
        EuiccCard.lambda$sendApduWithSimResetErrorWorkaround$50(this.f$0, th);
    }
}
