package com.android.server;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

public class Utils {
    public static boolean isPackageInstalled(String packageName, Context context) {
        if (packageName == null || context == null) {
            return false;
        }
        try {
            if (context.getPackageManager().getPackageInfo(packageName, 1) == null) {
                return false;
            }
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
