package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$toN63DWLt72dzp0WCl28UOMSmzE implements ApduRequestBuilder {
    public static final /* synthetic */ -$$Lambda$EuiccCard$toN63DWLt72dzp0WCl28UOMSmzE INSTANCE = new -$$Lambda$EuiccCard$toN63DWLt72dzp0WCl28UOMSmzE();

    private /* synthetic */ -$$Lambda$EuiccCard$toN63DWLt72dzp0WCl28UOMSmzE() {
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48941).addChildAsBytes(92, Tags.EUICC_PROFILE_TAGS).build().toHex());
    }
}
