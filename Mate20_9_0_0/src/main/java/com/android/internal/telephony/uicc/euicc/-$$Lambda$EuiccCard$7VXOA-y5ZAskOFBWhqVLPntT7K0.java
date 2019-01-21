package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$7VXOA-y5ZAskOFBWhqVLPntT7K0 implements ApduRequestBuilder {
    private final /* synthetic */ int f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$7VXOA-y5ZAskOFBWhqVLPntT7K0(int i) {
        this.f$0 = i;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48944).addChildAsInteger(128, this.f$0).build().toHex());
    }
}
