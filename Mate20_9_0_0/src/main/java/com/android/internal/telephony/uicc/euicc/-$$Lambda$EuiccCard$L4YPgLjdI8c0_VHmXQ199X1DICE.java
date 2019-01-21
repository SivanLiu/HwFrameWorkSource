package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$L4YPgLjdI8c0_VHmXQ199X1DICE implements ApduExceptionHandler {
    private final /* synthetic */ AsyncResultCallback f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$L4YPgLjdI8c0_VHmXQ199X1DICE(AsyncResultCallback asyncResultCallback) {
        this.f$0 = asyncResultCallback;
    }

    public final void handleException(Throwable th) {
        this.f$0.onException(new EuiccCardException("Cannot send APDU.", th));
    }
}
