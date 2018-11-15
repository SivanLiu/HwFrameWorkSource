package com.android.server.security.securityprofile;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SecurityProfileUtils {
    private static final int DEFAULT_USER_ID = 0;
    private static final String TAG = "SecurityProfileUtils";

    public static List<Integer> getUserIdListOnPhone(Context context) {
        List<Integer> UserIdList = new ArrayList();
        try {
            for (UserInfo userInfo : ((UserManager) context.getSystemService("user")).getUsers()) {
                UserIdList.add(Integer.valueOf(userInfo.id));
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get user id list err:");
            stringBuilder.append(e.getMessage());
            stringBuilder.append(".i must use default user id:");
            stringBuilder.append(0);
            Slog.e(str, stringBuilder.toString());
            UserIdList.add(Integer.valueOf(0));
        }
        return UserIdList;
    }

    public static List<String> getInstalledPackages(Context context) {
        List<String> result = new ArrayList();
        HashSet<String> set = new HashSet();
        for (Integer userId : getUserIdListOnPhone(context)) {
            for (PackageInfo info : context.getPackageManager().getInstalledPackagesAsUser(0, userId.intValue())) {
                set.add(info.packageName);
            }
        }
        result.addAll(set);
        return result;
    }

    public static String getInstalledApkPath(String packageName, Context context) {
        String ApkPath = null;
        for (Integer userId : getUserIdListOnPhone(context)) {
            int userId2 = userId.intValue();
            try {
                ApkPath = context.getPackageManager().getApplicationInfoAsUser(packageName, 0, userId2).sourceDir;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getInstalledApkPath name success,packageName:");
                stringBuilder.append(packageName);
                stringBuilder.append(",userId:");
                stringBuilder.append(userId2);
                stringBuilder.append(",ApkPath:");
                stringBuilder.append(ApkPath);
                Slog.d(str, stringBuilder.toString());
                return ApkPath;
            } catch (NameNotFoundException e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getInstalledApkPath name not found,packageName:");
                stringBuilder2.append(packageName);
                stringBuilder2.append(",userId:");
                stringBuilder2.append(userId2);
                Slog.d(str2, stringBuilder2.toString());
            }
        }
        return ApkPath;
    }
}
