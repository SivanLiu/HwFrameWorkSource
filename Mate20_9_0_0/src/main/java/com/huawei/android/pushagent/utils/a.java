package com.huawei.android.pushagent.utils;

import android.app.ActivityManager;
import android.text.TextUtils;

public abstract class a {
    public static int xy() {
        return ActivityManager.getCurrentUser();
    }

    public static String ya(int i) {
        if (i < 0 || i > 99) {
            i = 0;
        }
        return xx(String.valueOf(i));
    }

    public static String yb(String str) {
        return xx(str);
    }

    public static int xz(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "format userId failt, userId is: " + str);
            }
        }
        return xy();
    }

    private static String xx(String str) {
        if (TextUtils.isEmpty(str)) {
            return "00";
        }
        while (str.length() < 2) {
            str = "0" + str;
        }
        return str.substring(0, 2);
    }
}
