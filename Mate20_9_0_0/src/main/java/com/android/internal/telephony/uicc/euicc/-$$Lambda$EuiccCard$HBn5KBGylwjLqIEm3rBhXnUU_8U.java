package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$HBn5KBGylwjLqIEm3rBhXnUU_8U implements ApduRequestBuilder {
    public static final /* synthetic */ -$$Lambda$EuiccCard$HBn5KBGylwjLqIEm3rBhXnUU_8U INSTANCE = new -$$Lambda$EuiccCard$HBn5KBGylwjLqIEm3rBhXnUU_8U();

    private /* synthetic */ -$$Lambda$EuiccCard$HBn5KBGylwjLqIEm3rBhXnUU_8U() {
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48958).addChildAsBytes(92, new byte[]{(byte) 90}).build().toHex());
    }
}
