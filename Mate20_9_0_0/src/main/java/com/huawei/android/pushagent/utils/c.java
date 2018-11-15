package com.huawei.android.pushagent.utils;

import java.util.regex.Pattern;

public abstract class c {
    public static boolean fj(String str) {
        return fk("^[0-9]*$", str);
    }

    private static boolean fk(String str, String str2) {
        return Pattern.compile(str).matcher(str2).matches();
    }
}
