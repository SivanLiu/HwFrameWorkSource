package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;
import com.android.internal.telephony.uicc.euicc.apdu.RequestProvider;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$7vWsDgJ3RMY6kHsGeBw-CxIKViI implements RequestProvider {
    private final /* synthetic */ EuiccCard f$0;
    private final /* synthetic */ ApduRequestBuilder f$1;

    public /* synthetic */ -$$Lambda$EuiccCard$7vWsDgJ3RMY6kHsGeBw-CxIKViI(EuiccCard euiccCard, ApduRequestBuilder apduRequestBuilder) {
        this.f$0 = euiccCard;
        this.f$1 = apduRequestBuilder;
    }

    public final void buildRequest(byte[] bArr, RequestBuilder requestBuilder) {
        EuiccCard.lambda$newRequestProvider$48(this.f$0, this.f$1, bArr, requestBuilder);
    }
}
