package com.android.server.security.tsmagent.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class PackageUtil {
    public static int getVersionCode(Context context) {
        int versionCode = 0;
        if (context == null) {
            return versionCode;
        }
        try {
            PackageInfo pkInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (pkInfo != null) {
                return pkInfo.versionCode;
            }
            return versionCode;
        } catch (NameNotFoundException e) {
            HwLog.e("NameNotFoundException when getVersionCode");
            return versionCode;
        }
    }
}
