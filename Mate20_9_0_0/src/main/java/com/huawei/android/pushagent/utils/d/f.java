package com.huawei.android.pushagent.utils.d;

import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import com.huawei.android.pushagent.utils.f.c;
import java.util.UUID;

public class f extends g {
    private int deviceIdType = 9;

    public String getDeviceId() {
        try {
            return super.getDeviceId();
        } catch (AndroidRuntimeException e) {
            c.eq("PushLog3413", "framework get udid exist exception");
        } catch (Exception e2) {
            c.eq("PushLog3413", "framework get udid exist uncatch exception, identify as udid");
        }
        return cm();
    }

    public String cl() {
        String deviceId = getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            return cm();
        }
        return deviceId;
    }

    public int getDeviceIdType() {
        return this.deviceIdType;
    }

    public String cm() {
        c.ep("PushLog3413", "get UUID as deviceID");
        this.deviceIdType = 6;
        StringBuilder stringBuilder = new StringBuilder("_" + UUID.randomUUID().toString().replace("-", ""));
        while (stringBuilder.length() < 64) {
            stringBuilder.append("0");
        }
        return stringBuilder.toString().substring(0, 64);
    }
}
