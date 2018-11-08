package com.huawei.android.pushselfshow.richpush.tools;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.c;
import com.huawei.android.pushselfshow.utils.b.a;
import com.huawei.android.pushselfshow.utils.b.b;
import java.io.File;

public class d {
    public static String a(Context context, String str) {
        c.a("PushSelfShowLog", "download richpush file successed ,try to unzip file,file path is " + str);
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        if (str.startsWith(b.b(context))) {
            Object -l_2_R = "";
            try {
                -l_2_R = str.substring(0, str.lastIndexOf(File.separator));
                new a(str, -l_2_R + File.separator).a();
                Object -l_4_R = new File(-l_2_R + "/" + "index.html");
                if (-l_4_R.exists()) {
                    c.a("PushSelfShowLog", "unzip success ,so delete src zip file");
                    File -l_5_R = new File(str);
                    if (-l_5_R.exists()) {
                        com.huawei.android.pushselfshow.utils.a.a(-l_5_R);
                    }
                    return -l_4_R.getAbsolutePath();
                }
                c.a("PushSelfShowLog", "unzip fail ,don't exist index.html");
                com.huawei.android.pushselfshow.utils.a.a(new File(-l_2_R));
                return null;
            } catch (Object -l_3_R) {
                c.d("PushSelfShowLog", -l_3_R.toString());
                return "";
            }
        }
        c.a("PushSelfShowLog", "localfile dose not startsWith PushService directory");
        return "";
    }

    public String a(Context context, String str, int i, String str2) {
        String -l_5_R = null;
        try {
            -l_5_R = new b().a(context, str, str2);
            if (-l_5_R != null && -l_5_R.length() > 0) {
                return -l_5_R;
            }
            c.a("PushSelfShowLog", "download failed");
            if (i <= 0) {
                i = 1;
            }
            int i2 = i - 1;
            return (i2 > 0 && a(context, str, i2, str2) != null) ? -l_5_R : null;
        } catch (Object -l_6_R) {
            c.a("PushSelfShowLog", "download err" + -l_6_R.toString());
        }
    }
}
