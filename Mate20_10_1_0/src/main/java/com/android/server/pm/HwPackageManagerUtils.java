package com.android.server.pm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.huawei.android.content.pm.ApplicationInfoEx;
import com.huawei.android.os.UserHandleEx;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class HwPackageManagerUtils {
    private static final String FILE_POLICY_CLASS_NAME = "com.huawei.cust.HwCfgFilePolicy";
    private static final String METHOD_NAME_FOR_FILE = "getCfgFile";
    static final String TAG = "HwPackageManagerUtils";

    public static File getCfgFile(String fileName, int type) throws Exception, NoClassDefFoundError {
        Class<?> filePolicyClazz = Class.forName(FILE_POLICY_CLASS_NAME);
        return (File) filePolicyClazz.getMethod(METHOD_NAME_FOR_FILE, String.class, Integer.TYPE).invoke(filePolicyClazz, fileName, Integer.valueOf(type));
    }

    public static File getCustomizedFileName(String xmlName, int flag) {
        try {
            return getCfgFile("xml/" + xmlName, flag);
        } catch (NoClassDefFoundError e) {
            Log.d(TAG, "HwCfgFilePolicy NoClassDefFoundError");
            return null;
        } catch (Exception e2) {
            Log.d(TAG, "getCustomizedFileName get layout file exception");
            return null;
        }
    }

    public static final boolean isPackageFilename(String name) {
        return name != null && name.toLowerCase(Locale.ENGLISH).endsWith(".apk");
    }

    public static final boolean isHepFileName(String name) {
        return name != null && name.toLowerCase(Locale.ENGLISH).endsWith(".hep");
    }

    public static boolean isHaveApkFile(File[] dirs, String codePath) {
        String[] files;
        if (dirs == null) {
            return false;
        }
        for (File dir : dirs) {
            if (dir != null && dir.exists()) {
                for (String str : dir.list()) {
                    File file = new File(dir, str);
                    String[] filesSub = file.list();
                    if (file.getPath().equals(codePath) && filesSub != null) {
                        for (String subFile : filesSub) {
                            if (isPackageFilename(subFile)) {
                                return true;
                            }
                        }
                        continue;
                    }
                }
                continue;
            }
        }
        return false;
    }

    public static boolean isDynamicApplication(ApplicationInfo applicationInfo) {
        return (applicationInfo == null || applicationInfo.splitNames == null || applicationInfo.splitNames.length <= 0) ? false : true;
    }

    public static boolean isBundleApplication(ApplicationInfo applicationInfo) {
        if (!isDynamicApplication(applicationInfo)) {
            return false;
        }
        int[] splitFlags = ApplicationInfoEx.getHwSplitFlags(applicationInfo);
        for (int i = 0; i < applicationInfo.splitNames.length; i++) {
            if ((splitFlags[i] & 536870912) != 0) {
                return true;
            }
            if ((splitFlags[i] & 1073741824) != 0 && (splitFlags[i] & Integer.MIN_VALUE) == 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSplitApplication(ApplicationInfo applicationInfo) {
        if (!isDynamicApplication(applicationInfo)) {
            return false;
        }
        int[] splitFlags = ApplicationInfoEx.getHwSplitFlags(applicationInfo);
        for (int i = 0; i < applicationInfo.splitNames.length; i++) {
            if ((splitFlags[i] & 1073741824) != 0 && (splitFlags[i] & Integer.MIN_VALUE) != 0) {
                return true;
            }
        }
        return false;
    }

    public static String getPackageNameFromApk(String apkFile) {
        PackageParser.ApkLite apkLite;
        if (TextUtils.isEmpty(apkFile)) {
            return "";
        }
        try {
            File tempFile = new File(apkFile.trim()).getCanonicalFile();
            if (tempFile.exists() && (apkLite = PackageParser.parseApkLite(tempFile, 0)) != null) {
                return apkLite.packageName;
            }
            return "";
        } catch (IOException e) {
            Slog.e(TAG, "getPackageNameFromApk IOException:" + apkFile);
            return "";
        } catch (PackageParser.PackageParserException e2) {
            Slog.e(TAG, "getPackageNameFromApk PackageParserException:" + apkFile);
            return "";
        }
    }

    public static boolean isUserUnlocked(Context context) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        if (userManager == null) {
            return false;
        }
        return userManager.isUserUnlocked(UserHandleEx.getUserId(0));
    }
}
