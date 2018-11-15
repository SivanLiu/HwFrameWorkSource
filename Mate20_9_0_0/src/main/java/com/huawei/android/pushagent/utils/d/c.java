package com.huawei.android.pushagent.utils.d;

import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;

public class c {
    public String getDeviceId() {
        String serial;
        if (VERSION.SDK_INT >= 26) {
            try {
                serial = Build.getSerial();
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "get deviceId meets exception", e);
                serial = null;
            }
        } else {
            serial = null;
        }
        if (TextUtils.isEmpty(serial)) {
            serial = Build.SERIAL;
        }
        if ("unknown".equals(serial)) {
            return null;
        }
        return serial;
    }
}
