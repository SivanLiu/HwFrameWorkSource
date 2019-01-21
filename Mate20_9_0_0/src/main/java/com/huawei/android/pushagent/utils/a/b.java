package com.huawei.android.pushagent.utils.a;

import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import com.huawei.android.pushagent.utils.b.a;
import java.util.UUID;

public class b extends e {
    private int deviceIdType = 9;

    public String getDeviceId() {
        try {
            return super.getDeviceId();
        } catch (AndroidRuntimeException e) {
            a.su("PushLog3414", "framework get udid exist exception");
        } catch (Exception e2) {
            a.su("PushLog3414", "framework get udid exist uncatch exception, identify as udid");
        }
        return ry();
    }

    public String rx() {
        String deviceId = getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            return ry();
        }
        return deviceId;
    }

    public int getDeviceIdType() {
        return this.deviceIdType;
    }

    public String ry() {
        a.sv("PushLog3414", "get UUID as deviceID");
        this.deviceIdType = 6;
        StringBuilder stringBuilder = new StringBuilder("_" + UUID.randomUUID().toString().replace("-", ""));
        while (stringBuilder.length() < 64) {
            stringBuilder.append("0");
        }
        return stringBuilder.toString().substring(0, 64);
    }
}
