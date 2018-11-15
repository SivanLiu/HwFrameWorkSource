package com.android.server.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;

public class AppInstallerUtil {
    private static final String LOG_TAG = "AppInstallerUtil";

    private static Intent resolveIntent(Context context, Intent i) {
        ResolveInfo result = context.getPackageManager().resolveActivity(i, 0);
        return result != null ? new Intent(i.getAction()).setClassName(result.activityInfo.packageName, result.activityInfo.name) : null;
    }

    public static String getInstallerPackageName(Context context, String packageName) {
        String installerPackageName = null;
        try {
            installerPackageName = context.getPackageManager().getInstallerPackageName(packageName);
        } catch (IllegalArgumentException e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception while retrieving the package installer of ");
            stringBuilder.append(packageName);
            Log.e(str, stringBuilder.toString(), e);
        }
        if (installerPackageName == null) {
            return null;
        }
        return installerPackageName;
    }

    public static Intent createIntent(Context context, String installerPackageName, String packageName) {
        Intent result = resolveIntent(context, new Intent("android.intent.action.SHOW_APP_INFO").setPackage(installerPackageName));
        if (result == null) {
            return null;
        }
        result.putExtra("android.intent.extra.PACKAGE_NAME", packageName);
        result.addFlags(268435456);
        return result;
    }

    public static Intent createIntent(Context context, String packageName) {
        return createIntent(context, getInstallerPackageName(context, packageName), packageName);
    }
}
