package com.huawei.android.pushagent.a.a;

import android.content.Context;
import android.text.TextUtils;

public class d {
    public static String a(Context context, String str, String str2) {
        Object -l_3_R = "";
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return -l_3_R;
        }
        try {
            -l_3_R = com.huawei.android.pushagent.a.a.a.d.b(context, new e(context, str).b(str2 + "_v2"));
        } catch (Object -l_4_R) {
            c.d("PushLogSC2907", -l_4_R.toString(), -l_4_R);
        }
        if (TextUtils.isEmpty(-l_3_R)) {
            c.a("PushLogSC2907", "not exist for:" + str2);
        }
        return -l_3_R;
    }

    public static boolean a(Context context, String str, String str2, String str3) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return false;
        }
        return new e(context, str).a(str2 + "_v2", com.huawei.android.pushagent.a.a.a.d.a(context, str3));
    }
}
