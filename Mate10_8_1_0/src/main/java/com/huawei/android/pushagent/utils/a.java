package com.huawei.android.pushagent.utils;

import android.app.ActivityManager;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;

public abstract class a {
    public static int fc() {
        return ActivityManager.getCurrentUser();
    }

    public static String fe(int i) {
        if (i < 0 || i > 99) {
            i = 0;
        }
        return fb(String.valueOf(i));
    }

    public static String ff(String str) {
        return fb(str);
    }

    public static int fd(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                b.y("PushLog2976", "format userId failt, userId is: " + str);
            }
        }
        return fc();
    }

    private static String fb(String str) {
        if (TextUtils.isEmpty(str)) {
            return "00";
        }
        while (str.length() < 2) {
            str = "0" + str;
        }
        return str.substring(0, 2);
    }
}
