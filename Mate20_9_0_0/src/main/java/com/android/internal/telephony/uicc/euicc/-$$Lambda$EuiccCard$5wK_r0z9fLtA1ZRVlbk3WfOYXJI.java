package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$5wK_r0z9fLtA1ZRVlbk3WfOYXJI implements ApduRequestBuilder {
    private final /* synthetic */ byte[] f$0;
    private final /* synthetic */ byte[] f$1;
    private final /* synthetic */ byte[] f$2;
    private final /* synthetic */ byte[] f$3;

    public /* synthetic */ -$$Lambda$EuiccCard$5wK_r0z9fLtA1ZRVlbk3WfOYXJI(byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        this.f$0 = bArr;
        this.f$1 = bArr2;
        this.f$2 = bArr3;
        this.f$3 = bArr4;
    }

    public final void build(RequestBuilder requestBuilder) {
        EuiccCard.lambda$prepareDownload$34(this.f$0, this.f$1, this.f$2, this.f$3, requestBuilder);
    }
}
