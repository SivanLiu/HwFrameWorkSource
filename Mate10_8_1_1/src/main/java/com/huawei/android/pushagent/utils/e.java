package com.huawei.android.pushagent.utils;

import java.util.regex.Pattern;

public abstract class e {
    public static boolean fn(String str) {
        return fo("^[0-9]*$", str);
    }

    private static boolean fo(String str, String str2) {
        return Pattern.compile(str).matcher(str2).matches();
    }
}
