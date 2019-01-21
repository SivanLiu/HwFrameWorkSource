package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$_VOB5FQfE7RUMgpmr8bK-j3CsUA implements ApduRequestBuilder {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ String f$1;

    public /* synthetic */ -$$Lambda$EuiccCard$_VOB5FQfE7RUMgpmr8bK-j3CsUA(String str, String str2) {
        this.f$0 = str;
        this.f$1 = str2;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48937).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(this.f$0))).addChildAsString(144, this.f$1).build().toHex());
    }
}
