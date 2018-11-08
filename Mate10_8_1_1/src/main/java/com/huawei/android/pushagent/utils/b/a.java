package com.huawei.android.pushagent.utils.b;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.huawei.android.pushagent.utils.a.b;

public abstract class a implements d {
    private Context appCtx;

    public a(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) this.appCtx.getSystemService("phone");
        if (telephonyManager == null) {
            return null;
        }
        try {
            return telephonyManager.getDeviceId();
        } catch (Throwable e) {
            b.aa("PushLog2976", "get deviceId meets exception", e);
            return null;
        }
    }
}
