package com.huawei.android.pushagent.utils;

import java.util.regex.Pattern;

public abstract class b {
    public static boolean yc(String str) {
        return yd("^[0-9]*$", str);
    }

    private static boolean yd(String str, String str2) {
        return Pattern.compile(str).matcher(str2).matches();
    }
}
