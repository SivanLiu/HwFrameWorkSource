package com.huawei.android.pushagent.utils.a;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.huawei.android.pushagent.utils.b.a;

public abstract class c implements g {
    private Context appCtx;

    public c(Context context) {
        this.appCtx = context.getApplicationContext();
    }

    public String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) this.appCtx.getSystemService("phone");
        if (telephonyManager == null) {
            return null;
        }
        try {
            return telephonyManager.getDeviceId();
        } catch (Exception e) {
            a.sw("PushLog3414", "get deviceId meets exception", e);
            return null;
        }
    }
}
