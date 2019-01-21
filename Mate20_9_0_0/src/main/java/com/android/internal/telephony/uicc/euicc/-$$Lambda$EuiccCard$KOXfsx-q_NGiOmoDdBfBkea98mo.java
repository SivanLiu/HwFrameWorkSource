package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$KOXfsx-q_NGiOmoDdBfBkea98mo implements ApduRequestBuilder {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$KOXfsx-q_NGiOmoDdBfBkea98mo(int i) {
        this.f$0 = i;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48939).addChild(Asn1Node.newBuilder(160).addChildAsInteger(128, this.f$0)).build().toHex());
    }
}
