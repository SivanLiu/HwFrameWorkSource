package com.android.internal.telephony.uicc.euicc;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$fcz5l0a6JlSxs8MXCst7wXG4bUc implements ApduResponseHandler {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$fcz5l0a6JlSxs8MXCst7wXG4bUc(String str) {
        this.f$0 = str;
    }

    public final Object handleResult(byte[] bArr) {
        return EuiccCard.lambda$switchToProfile$9(this.f$0, bArr);
    }
}
