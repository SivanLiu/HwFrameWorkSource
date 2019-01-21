package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$mf0dWT4hLdKlsLFFHVfdGFxHyX0 implements ApduRequestBuilder {
    private final /* synthetic */ byte[] f$0;
    private final /* synthetic */ int f$1;

    public /* synthetic */ -$$Lambda$EuiccCard$mf0dWT4hLdKlsLFFHVfdGFxHyX0(byte[] bArr, int i) {
        this.f$0 = bArr;
        this.f$1 = i;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48961).addChildAsBytes(128, this.f$0).addChildAsInteger(129, this.f$1).build().toHex());
    }
}
