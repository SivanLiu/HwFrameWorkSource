package com.huawei.android.pushagent.utils.a;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;
import java.util.UUID;

public class d extends c {
    private int deviceIdType = 0;

    public d(Context context) {
        super(context);
    }

    public String getDeviceId() {
        String deviceId = super.getDeviceId();
        if (TextUtils.isEmpty(deviceId) || deviceId.matches("[0]+")) {
            a.sv("PushLog3414", "get uniqueId from device is empty or all 0");
            return null;
        }
        StringBuffer stringBuffer = new StringBuffer();
        if (deviceId.length() >= 16) {
            deviceId.substring(deviceId.length() - 16);
        } else {
            stringBuffer.append("0").append(deviceId);
        }
        if (stringBuffer.length() < 16) {
            StringBuffer stringBuffer2 = new StringBuffer();
            for (int i = 0; i < 16 - stringBuffer.length(); i++) {
                stringBuffer2.append("0");
            }
            stringBuffer.append(stringBuffer2);
        }
        return stringBuffer.toString();
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
        this.deviceIdType = 6;
        StringBuilder stringBuilder = new StringBuilder("_" + UUID.randomUUID().toString().replace("-", ""));
        while (stringBuilder.length() < 16) {
            stringBuilder.append("0");
        }
        return stringBuilder.toString().substring(0, 16);
    }
}
