package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$MoRNAw8O6kYG_c2AJkozlJwO2NM implements ApduRequestBuilder {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$MoRNAw8O6kYG_c2AJkozlJwO2NM(String str) {
        this.f$0 = str;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48947).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(this.f$0))).build().toHex());
    }
}
