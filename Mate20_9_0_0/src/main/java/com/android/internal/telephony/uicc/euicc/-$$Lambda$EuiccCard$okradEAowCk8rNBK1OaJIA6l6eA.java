package com.android.internal.telephony.uicc.euicc;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$okradEAowCk8rNBK1OaJIA6l6eA implements ApduResponseHandler {
    private final /* synthetic */ EuiccCard f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$okradEAowCk8rNBK1OaJIA6l6eA(EuiccCard euiccCard) {
        this.f$0 = euiccCard;
    }

    public final Object handleResult(byte[] bArr) {
        return EuiccCard.lambda$getEid$11(this.f$0, bArr);
    }
}
