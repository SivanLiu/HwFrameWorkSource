package com.huawei.android.pushagent.utils.a;

import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;

public class a {
    public String getDeviceId() {
        String serial;
        if (VERSION.SDK_INT >= 26) {
            try {
                serial = Build.getSerial();
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "get deviceId meets exception", e);
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
