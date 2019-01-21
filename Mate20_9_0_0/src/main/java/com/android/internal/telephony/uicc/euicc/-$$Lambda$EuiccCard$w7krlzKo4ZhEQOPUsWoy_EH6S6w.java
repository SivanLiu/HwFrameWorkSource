package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$w7krlzKo4ZhEQOPUsWoy_EH6S6w implements ApduRequestBuilder {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$w7krlzKo4ZhEQOPUsWoy_EH6S6w(int i) {
        this.f$0 = i;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48939).addChild(Asn1Node.newBuilder(160).addChildAsBits(129, this.f$0)).build().toHex());
    }
}
