package com.android.internal.telephony.uicc.euicc;

import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$EuiccCard$8wofF-Li1V6a8rJQc-M2IGeJ26E implements ApduRequestBuilder {
    public static final /* synthetic */ -$$Lambda$EuiccCard$8wofF-Li1V6a8rJQc-M2IGeJ26E INSTANCE = new -$$Lambda$EuiccCard$8wofF-Li1V6a8rJQc-M2IGeJ26E();

    private /* synthetic */ -$$Lambda$EuiccCard$8wofF-Li1V6a8rJQc-M2IGeJ26E() {
    }

    public final void build(RequestBuilder requestBuilder) {
        requestBuilder.addStoreData(Asn1Node.newBuilder(48942).build().toHex());
    }
}
