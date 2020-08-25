package com.android.server.security.securityprofile;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.android.server.UiThread;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class SecurityProfileUtils {
    static final boolean DEBUG = Log.HWINFO;
    private static final int DEFAULT_PACKAGES_MULTI_USERS_CAPACITY = 128;
    private static final int DEFAULT_USERS_CAPACITY = 8;
    private static final Object LOCK = new Object();
    private static final String TAG = "SecurityProfileUtils";
    private static volatile ThreadPoolExecutor sWatcherThreadPool;
    private static volatile ThreadPoolExecutor sWorkerThreadPool;

    SecurityProfileUtils() {
    }

    static List<Integer> getUserIdListOnPhone(Context context) {
        List<Integer> userIdList = new ArrayList<>(8);
        UserManager userManager = (UserManager) context.getSystemService("user");
        if (userManager == null) {
            Log.w(TAG, "get user manager null, I must use default user id");
            userIdList.add(0);
            return userIdList;
        }
        for (UserInfo userInfo : userManager.getUsers(false)) {
            userIdList.add(Integer.valueOf(userInfo.id));
        }
        if (userIdList.isEmpty()) {
            Log.w(TAG, "get user id list size 0, I must use default user id");
            userIdList.add(0);
        }
        return userIdList;
    }

    static List<String> getInstalledPackages(Context context) {
        Set<String> tempSet = new HashSet<>(128);
        List<Integer> userIdList = getUserIdListOnPhone(context);
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            Log.w(TAG, "get package manager null, when get installed packages");
            return Collections.emptyList();
        }
        for (Integer num : userIdList) {
            for (PackageInfo info : pm.getInstalledPackagesAsUser(0, num.intValue())) {
                tempSet.add(info.packageName);
            }
        }
        return new ArrayList(tempSet);
    }

    static String getInstalledApkPath(String packageName, Context context) {
        List<Integer> userIdList = getUserIdListOnPhone(context);
        PackageManager pm = context.getPackageManager();
        String str = null;
        if (pm == null) {
            Log.w(TAG, "get package manager null, when get installed Apk path");
            return null;
        }
        int i = 0;
        int len = userIdList.size();
        while (i < len) {
            try {
                return pm.getApplicationInfoAsUser(packageName, 0, userIdList.get(i).intValue()).sourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                if (DEBUG) {
                    Log.d(TAG, "getInstalledApkPath name not found, packageName: " + packageName + ", index: " + i + " in length: " + len);
                }
                i++;
            }
        }
        return str;
    }

    static boolean isAccessibilitySelectToSpeakActive(Context context) {
        return isAccessibilitySelectToSpeakActive(context, getCurrentActiveUserId());
    }

    static boolean isAccessibilitySelectToSpeakActive(Context context, int userId) {
        String enabledServicesSetting = Settings.Secure.getStringForUser(context.getContentResolver(), "enabled_accessibility_services", userId);
        if (enabledServicesSetting == null || !enabledServicesSetting.contains("SelectToSpeakService")) {
            return false;
        }
        return true;
    }

    static int getCurrentActiveUserId() {
        return ActivityManager.getCurrentUser();
    }

    static boolean isSystemApp(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return false;
        }
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            if (appInfo == null) {
                return false;
            }
            if (appInfo.uid < 10000 || (appInfo.flags & 1) != 0) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can not check if " + packageName + " is a system app");
        }
    }

    static boolean isLauncherApp(Context context, String target) {
        String launcher;
        PackageManager manager = context.getPackageManager();
        if (manager == null) {
            return false;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        ResolveInfo resolveInfo = manager.resolveActivity(intent, 65536);
        if (resolveInfo == null || resolveInfo.activityInfo == null || (launcher = resolveInfo.activityInfo.packageName) == null || !launcher.equals(target)) {
            return false;
        }
        return true;
    }

    static void showToast(final Context context, final String text) {
        UiThread.getHandler().post(new Runnable() {
            /* class com.android.server.security.securityprofile.SecurityProfileUtils.AnonymousClass1 */

            public void run() {
                Toast toast = Toast.makeText(context, text, 1);
                toast.getWindowParams().privateFlags |= 16;
                toast.show();
            }
        });
    }

    static String replaceLineSeparator(String outerPackageName) {
        return outerPackageName.replaceAll(System.lineSeparator(), "");
    }

    static ThreadPoolExecutor getWorkerThreadPool() {
        if (sWorkerThreadPool == null) {
            synchronized (LOCK) {
                if (sWorkerThreadPool == null) {
                    sWorkerThreadPool = getThreadPoolImpl();
                }
            }
        }
        return sWorkerThreadPool;
    }

    private static ThreadPoolExecutor getThreadPoolImpl() {
        return new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() + 8, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100), new RejectedExecutionHandler() {
            /* class com.android.server.security.securityprofile.SecurityProfileUtils.AnonymousClass2 */

            public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
                Log.w(SecurityProfileUtils.TAG, "Thread pool and queue is full, dropped #runnable: " + runnable.toString());
            }
        });
    }

    static ThreadPoolExecutor getWatcherThreadPool() {
        if (sWatcherThreadPool == null) {
            synchronized (LOCK) {
                if (sWatcherThreadPool == null) {
                    sWatcherThreadPool = getThreadPoolImpl();
                }
            }
        }
        return sWatcherThreadPool;
    }
}
