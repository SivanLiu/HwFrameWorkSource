package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$Wx9UmYdMwRy23Rf6Vd7b2aSx6S8 implements ApduRequestBuilder {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$Wx9UmYdMwRy23Rf6Vd7b2aSx6S8(int i) {
        this.f$0 = i;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48948).addChildAsBits(130, this.f$0).build().toHex());
    }
}
