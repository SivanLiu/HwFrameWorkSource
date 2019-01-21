package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$FbRMt6fKnYLkYt6oi5qhs1ZyEvc implements ApduRequestBuilder {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$FbRMt6fKnYLkYt6oi5qhs1ZyEvc(String str) {
        this.f$0 = str;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48959).addChildAsString(128, this.f$0).build().toHex());
    }
}
