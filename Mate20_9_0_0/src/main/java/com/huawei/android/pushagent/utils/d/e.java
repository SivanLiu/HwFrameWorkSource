package com.huawei.android.pushagent.utils.d;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.huawei.android.pushagent.utils.f.c;

public abstract class e implements b {
    private Context appCtx;

    public e(Context context) {
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
            c.es("PushLog3413", "get deviceId meets exception", e);
            return null;
        }
    }
}
