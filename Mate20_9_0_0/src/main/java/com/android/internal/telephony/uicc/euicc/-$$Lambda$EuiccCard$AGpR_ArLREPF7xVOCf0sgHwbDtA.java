package com.android.internal.telephony.uicc.euicc;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$AGpR_ArLREPF7xVOCf0sgHwbDtA implements ApduResponseHandler {
    public static final /* synthetic */ -$$Lambda$EuiccCard$AGpR_ArLREPF7xVOCf0sgHwbDtA INSTANCE = new -$$Lambda$EuiccCard$AGpR_ArLREPF7xVOCf0sgHwbDtA();

    private /* synthetic */ -$$Lambda$EuiccCard$AGpR_ArLREPF7xVOCf0sgHwbDtA() {
    }

    public final Object handleResult(byte[] bArr) {
        return EuiccCard.parseResponse(bArr).getChild(128, new int[0]).asBytes();
    }
}
