package com.huawei.android.pushagent.utils.b;

import android.text.TextUtils;
import android.util.AndroidRuntimeException;
import com.huawei.android.pushagent.utils.a.b;
import java.util.UUID;

public class c extends f {
    private int deviceIdType = 9;

    public String getDeviceId() {
        try {
            return super.getDeviceId();
        } catch (AndroidRuntimeException e) {
            b.y("PushLog2976", "framework get udid exist exception");
            return ax();
        } catch (Exception e2) {
            b.y("PushLog2976", "framework get udid exist uncatch exception, identify as udid");
            return ax();
        }
    }

    public String aw() {
        String deviceId = getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            return ax();
        }
        return deviceId;
    }

    public int getDeviceIdType() {
        return this.deviceIdType;
    }

    public String ax() {
        b.z("PushLog2976", "get UUID as deviceID");
        this.deviceIdType = 6;
        StringBuilder stringBuilder = new StringBuilder("_" + UUID.randomUUID().toString().replace("-", ""));
        while (stringBuilder.length() < 64) {
            stringBuilder.append("0");
        }
        return stringBuilder.toString().substring(0, 64);
    }
}
