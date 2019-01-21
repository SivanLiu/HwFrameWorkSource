package com.android.internal.telephony.imsphone;

import com.android.internal.telephony.imsphone.ImsRttTextHandler.NetworkWriter;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsPhoneConnection$gXYXXIQcibrbO2gQqP7d18avaBI implements NetworkWriter {
    private final /* synthetic */ ImsPhoneConnection f$0;

    public /* synthetic */ -$$Lambda$ImsPhoneConnection$gXYXXIQcibrbO2gQqP7d18avaBI(ImsPhoneConnection imsPhoneConnection) {
        this.f$0 = imsPhoneConnection;
    }

    public final void write(String str) {
        this.f$0.getImsCall().sendRttMessage(str);
    }
}
