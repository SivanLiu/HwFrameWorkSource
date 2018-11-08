package com.huawei.android.pushselfshow.richpush.html.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import com.huawei.android.pushagent.a.a.c;
import java.io.File;
import java.util.ArrayList;

public class b {
    public static String a(String str, String str2) {
        if (a(str2)) {
            return str2;
        }
        if (str != null) {
            Object -l_2_R = str + File.separator + str2;
            c.e("PushSelfShowLog", "the audio path is " + -l_2_R);
            if (-l_2_R.startsWith("file://")) {
                -l_2_R = -l_2_R.substring(7);
            }
            if (new File(-l_2_R).exists()) {
                return -l_2_R;
            }
        }
        return null;
    }

    public static ArrayList a(Context context, Intent intent) {
        Object -l_2_R = new ArrayList();
        Object -l_3_R = context.getPackageManager().queryIntentActivities(intent, 0);
        if (!(-l_3_R == null || -l_3_R.size() == 0)) {
            int -l_4_I = -l_3_R.size();
            for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
                if (((ResolveInfo) -l_3_R.get(-l_5_I)).activityInfo != null) {
                    c.a("PushSelfShowLog", "getSupportPackage:" + ((ResolveInfo) -l_3_R.get(-l_5_I)).activityInfo.applicationInfo.packageName);
                    -l_2_R.add(((ResolveInfo) -l_3_R.get(-l_5_I)).activityInfo.applicationInfo.packageName);
                }
            }
        }
        return -l_2_R;
    }

    public static boolean a(String str) {
        return str.contains("http://") || str.contains("https://");
    }
}
