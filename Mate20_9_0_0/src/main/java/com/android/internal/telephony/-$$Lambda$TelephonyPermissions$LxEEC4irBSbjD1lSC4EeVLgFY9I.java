package com.android.internal.telephony;

import android.os.ServiceManager;
import com.android.internal.telephony.ITelephony.Stub;
import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TelephonyPermissions$LxEEC4irBSbjD1lSC4EeVLgFY9I implements Supplier {
    public static final /* synthetic */ -$$Lambda$TelephonyPermissions$LxEEC4irBSbjD1lSC4EeVLgFY9I INSTANCE = new -$$Lambda$TelephonyPermissions$LxEEC4irBSbjD1lSC4EeVLgFY9I();

    private /* synthetic */ -$$Lambda$TelephonyPermissions$LxEEC4irBSbjD1lSC4EeVLgFY9I() {
    }

    public final Object get() {
        return Stub.asInterface(ServiceManager.getService(PhoneConstants.PHONE_KEY));
    }
}
