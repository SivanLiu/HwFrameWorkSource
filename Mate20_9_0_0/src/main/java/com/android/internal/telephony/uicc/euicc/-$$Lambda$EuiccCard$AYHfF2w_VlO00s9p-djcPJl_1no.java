package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$AYHfF2w_VlO00s9p-djcPJl_1no implements ApduRequestBuilder {
    private final /* synthetic */ String f$0;
    private final /* synthetic */ boolean f$1;

    public /* synthetic */ -$$Lambda$EuiccCard$AYHfF2w_VlO00s9p-djcPJl_1no(String str, boolean z) {
        this.f$0 = str;
        this.f$1 = z;
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48945).addChild(Asn1Node.newBuilder(160).addChildAsBytes(90, IccUtils.bcdToBytes(EuiccCard.padTrailingFs(this.f$0)))).addChildAsBoolean(129, this.f$1).build().toHex());
    }
}
