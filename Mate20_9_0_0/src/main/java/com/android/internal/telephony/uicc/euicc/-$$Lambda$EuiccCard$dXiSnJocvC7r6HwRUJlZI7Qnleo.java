package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$dXiSnJocvC7r6HwRUJlZI7Qnleo implements ApduRequestBuilder {
    private final /* synthetic */ EuiccCard f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ byte[] f$2;
    private final /* synthetic */ byte[] f$3;
    private final /* synthetic */ byte[] f$4;
    private final /* synthetic */ byte[] f$5;

    public /* synthetic */ -$$Lambda$EuiccCard$dXiSnJocvC7r6HwRUJlZI7Qnleo(EuiccCard euiccCard, String str, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        this.f$0 = euiccCard;
        this.f$1 = str;
        this.f$2 = bArr;
        this.f$3 = bArr2;
        this.f$4 = bArr3;
        this.f$5 = bArr4;
    }

    public final void build(RequestBuilder requestBuilder) {
        EuiccCard.lambda$authenticateServer$32(this.f$0, this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, requestBuilder);
    }
}
