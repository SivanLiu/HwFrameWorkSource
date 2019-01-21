package com.android.internal.telephony.uicc.euicc;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$MRlmz2j6osUyi5hGvD3j9D4Tsrg implements ApduResponseHandler {
    public static final /* synthetic */ -$$Lambda$EuiccCard$MRlmz2j6osUyi5hGvD3j9D4Tsrg INSTANCE = new -$$Lambda$EuiccCard$MRlmz2j6osUyi5hGvD3j9D4Tsrg();

    private /* synthetic */ -$$Lambda$EuiccCard$MRlmz2j6osUyi5hGvD3j9D4Tsrg() {
    }

    public final Object handleResult(byte[] bArr) {
        return EuiccCard.parseResponseAndCheckSimpleError(bArr, 3).toBytes();
    }
}
