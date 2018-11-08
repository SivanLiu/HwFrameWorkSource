package com.huawei.android.pushagent.utils;

public abstract class g {
    public static String hc(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj != null) {
            return String.valueOf(obj);
        }
        return null;
    }

    public static String hb(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("\"", "\\\"");
    }
}
