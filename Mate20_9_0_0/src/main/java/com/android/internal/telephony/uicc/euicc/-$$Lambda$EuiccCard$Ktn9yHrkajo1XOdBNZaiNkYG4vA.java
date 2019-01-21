package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$Ktn9yHrkajo1XOdBNZaiNkYG4vA implements ApduRequestBuilder {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$Ktn9yHrkajo1XOdBNZaiNkYG4vA(int i) {
        this.f$0 = i;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48936).addChildAsBits(129, this.f$0).build().toHex());
    }
}
