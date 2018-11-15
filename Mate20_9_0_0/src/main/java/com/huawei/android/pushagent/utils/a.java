package com.huawei.android.pushagent.utils;

import android.app.ActivityManager;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.c;

public abstract class a {
    public static int fb() {
        return ActivityManager.getCurrentUser();
    }

    public static String fa(int i) {
        if (i < 0 || i > 99) {
            i = 0;
        }
        return fc(String.valueOf(i));
    }

    public static String fe(String str) {
        return fc(str);
    }

    public static int fd(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                c.eq("PushLog3413", "format userId failt, userId is: " + str);
            }
        }
        return fb();
    }

    private static String fc(String str) {
        if (TextUtils.isEmpty(str)) {
            return "00";
        }
        while (str.length() < 2) {
            str = "0" + str;
        }
        return str.substring(0, 2);
    }
}
