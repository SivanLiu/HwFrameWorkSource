package com.android.internal.telephony.util;

import android.os.SystemProperties;

public class HwCustUtil {
    public static final boolean isVZW;

    static {
        boolean equals;
        if ("389".equals(SystemProperties.get("ro.config.hw_opta"))) {
            equals = "840".equals(SystemProperties.get("ro.config.hw_optb"));
        } else {
            equals = false;
        }
        isVZW = equals;
    }
}
