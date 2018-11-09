package com.huawei.android.pushagent.utils.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.f;
import java.util.List;

public class b {
    public static long l(Context context, String str) {
        long j = -1000;
        long m;
        try {
            List gm = f.gm(context.getPackageManager(), new Intent("com.huawei.android.push.intent.REGISTER").setPackage(str), 787072, a.fc());
            if (gm == null || gm.size() == 0) {
                return -1000;
            }
            j = 228;
            String str2 = ((ResolveInfo) gm.get(0)).serviceInfo != null ? ((ResolveInfo) gm.get(0)).serviceInfo.packageName : ((ResolveInfo) gm.get(0)).activityInfo.packageName;
            m = str2 != null ? str2.equals(str) ? m((ResolveInfo) gm.get(0), "CS_cloud_version") : 228 : 228;
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", str + " version is :" + m);
            return m;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "get Apk version faild ,Exception e= " + e.toString());
            m = j;
        }
    }

    private static long m(ResolveInfo resolveInfo, String str) {
        long j = -1;
        if (resolveInfo == null) {
            return j;
        }
        try {
            j = Long.parseLong(f.gn(resolveInfo, str));
        } catch (NumberFormatException e) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", str + " is not set in " + n(resolveInfo));
        }
        return j;
    }

    private static String n(ResolveInfo resolveInfo) {
        if (resolveInfo == null) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "ResolveInfo is null , cannot get packageName");
            return null;
        }
        return resolveInfo.serviceInfo != null ? resolveInfo.serviceInfo.packageName : resolveInfo.activityInfo.packageName;
    }
}
