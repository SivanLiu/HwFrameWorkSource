package com.android.server.security.tsmagent.utils;

import android.content.Context;
import java.util.UUID;

public class PhoneDeviceUtil {
    private static String GLOBAL_DEVICE_ID = "";

    public static String getNumUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        if (uuid.length() > 15) {
            return uuid.substring(0, 16);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("0000000000000000".substring(15 - uuid.length()));
        stringBuilder.append(uuid);
        return stringBuilder.toString();
    }

    public static String getSerialNumber(Context context) {
        return "deprecated";
    }
}
