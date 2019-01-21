package com.android.internal.telephony.imsphone;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker.SharedPreferenceProxy;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ImsPhoneCallTracker$Zw03itjXT6-LrhiYuD-9nKFg2Wg implements SharedPreferenceProxy {
    public static final /* synthetic */ -$$Lambda$ImsPhoneCallTracker$Zw03itjXT6-LrhiYuD-9nKFg2Wg INSTANCE = new -$$Lambda$ImsPhoneCallTracker$Zw03itjXT6-LrhiYuD-9nKFg2Wg();

    private /* synthetic */ -$$Lambda$ImsPhoneCallTracker$Zw03itjXT6-LrhiYuD-9nKFg2Wg() {
    }

    public final SharedPreferences getDefaultSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
