package com.huawei.android.pushagent.utils.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.d;
import java.util.List;

public class c {
    public static long sk(Context context, String str) {
        long j = -1000;
        long sl;
        try {
            List zp = d.zp(context.getPackageManager(), new Intent("com.huawei.android.push.intent.REGISTER").setPackage(str), 787072, a.xy());
            if (zp == null || zp.size() == 0) {
                return -1000;
            }
            j = 228;
            String str2 = ((ResolveInfo) zp.get(0)).serviceInfo != null ? ((ResolveInfo) zp.get(0)).serviceInfo.packageName : ((ResolveInfo) zp.get(0)).activityInfo.packageName;
            sl = str2 != null ? str2.equals(str) ? sl((ResolveInfo) zp.get(0), "CS_cloud_version") : 228 : 228;
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", str + " version is :" + sl);
            return sl;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "get Apk version faild ,Exception e= " + e.toString());
            sl = j;
        }
    }

    private static long sl(ResolveInfo resolveInfo, String str) {
        long j = -1;
        if (resolveInfo == null) {
            return j;
        }
        try {
            j = Long.parseLong(d.zg(resolveInfo, str));
        } catch (NumberFormatException e) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", str + " is not set in " + sm(resolveInfo));
        }
        return j;
    }

    private static String sm(ResolveInfo resolveInfo) {
        if (resolveInfo == null) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "ResolveInfo is null , cannot get packageName");
            return null;
        }
        return resolveInfo.serviceInfo != null ? resolveInfo.serviceInfo.packageName : resolveInfo.activityInfo.packageName;
    }
}
