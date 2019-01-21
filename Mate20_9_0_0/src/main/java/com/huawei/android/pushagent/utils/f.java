package com.huawei.android.pushagent.utils;

public abstract class f {
    public static String zv(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj != null) {
            return String.valueOf(obj);
        }
        return null;
    }

    public static String zu(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("\"", "\\\"");
    }
}
