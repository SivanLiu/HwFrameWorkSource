package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$QGtQZCF6KEnI-x59_tp1eo8mWew implements ApduRequestBuilder {
    private final /* synthetic */ String f$0;

    public /* synthetic */ -$$Lambda$EuiccCard$QGtQZCF6KEnI-x59_tp1eo8mWew(String str) {
        this.f$0 = str;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48941).addChild(Asn1Node.newBuilder(160).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(this.f$0))).build()).addChildAsBytes(92, Tags.EUICC_PROFILE_TAGS).build().toHex());
    }
}
