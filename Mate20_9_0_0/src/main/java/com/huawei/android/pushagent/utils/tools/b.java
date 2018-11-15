package com.huawei.android.pushagent.utils.tools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import com.huawei.android.pushagent.utils.a;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.util.List;

public class b {
    public static long co(Context context, String str) {
        long j = -1000;
        long cp;
        try {
            List gs = g.gs(context.getPackageManager(), new Intent("com.huawei.android.push.intent.REGISTER").setPackage(str), 787072, a.fb());
            if (gs == null || gs.size() == 0) {
                return -1000;
            }
            j = 228;
            String str2 = ((ResolveInfo) gs.get(0)).serviceInfo != null ? ((ResolveInfo) gs.get(0)).serviceInfo.packageName : ((ResolveInfo) gs.get(0)).activityInfo.packageName;
            cp = str2 != null ? str2.equals(str) ? cp((ResolveInfo) gs.get(0), "CS_cloud_version") : 228 : 228;
            c.er("PushLog3413", str + " version is :" + cp);
            return cp;
        } catch (Exception e) {
            c.eq("PushLog3413", "get Apk version faild ,Exception e= " + e.toString());
            cp = j;
        }
    }

    private static long cp(ResolveInfo resolveInfo, String str) {
        long j = -1;
        if (resolveInfo == null) {
            return j;
        }
        try {
            j = Long.parseLong(g.gt(resolveInfo, str));
        } catch (NumberFormatException e) {
            c.er("PushLog3413", str + " is not set in " + cq(resolveInfo));
        }
        return j;
    }

    private static String cq(ResolveInfo resolveInfo) {
        if (resolveInfo == null) {
            c.ep("PushLog3413", "ResolveInfo is null , cannot get packageName");
            return null;
        }
        return resolveInfo.serviceInfo != null ? resolveInfo.serviceInfo.packageName : resolveInfo.activityInfo.packageName;
    }
}
