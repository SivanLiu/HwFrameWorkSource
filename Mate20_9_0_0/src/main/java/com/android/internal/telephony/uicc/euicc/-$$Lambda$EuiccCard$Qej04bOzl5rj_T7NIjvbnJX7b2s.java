package com.android.internal.telephony.uicc.euicc;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$Qej04bOzl5rj_T7NIjvbnJX7b2s implements ApduResponseHandler {
    public static final /* synthetic */ -$$Lambda$EuiccCard$Qej04bOzl5rj_T7NIjvbnJX7b2s INSTANCE = new -$$Lambda$EuiccCard$Qej04bOzl5rj_T7NIjvbnJX7b2s();

    private /* synthetic */ -$$Lambda$EuiccCard$Qej04bOzl5rj_T7NIjvbnJX7b2s() {
    }

    public final Object handleResult(byte[] bArr) {
        return EuiccCard.parseResponse(bArr).getChild(128, new int[0]).asString();
    }
}
