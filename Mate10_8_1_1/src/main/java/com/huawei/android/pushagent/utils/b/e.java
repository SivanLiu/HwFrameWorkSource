package com.huawei.android.pushagent.utils.b;

import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;

public class e {
    public String getDeviceId() {
        String serial;
        if (VERSION.SDK_INT >= 26) {
            try {
                serial = Build.getSerial();
            } catch (Throwable e) {
                b.aa("PushLog2976", "get deviceId meets exception", e);
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
