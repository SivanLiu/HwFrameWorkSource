package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$XDNTzAU-9I92HztVAJQr4NXR3DU implements ApduRequestBuilder {
    private final /* synthetic */ byte[] f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$XDNTzAU-9I92HztVAJQr4NXR3DU(byte[] bArr) {
        this.f$0 = bArr;
    }

    public final void build(RequestBuilder requestBuilder) {
        EuiccCard.lambda$loadBoundProfilePackage$36(this.f$0, requestBuilder);
    }
}
