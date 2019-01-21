package android.contentsensor;

import android.app.Activity;
import android.app.AppGlobals;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ContentSensorManagerFactory {
    private static final String SENSOR_MANAGER_CLASS_NAME = "com.huawei.contentsensor.agent.ContentSensorManager";
    private static final String SENSOR_MANAGER_PACKAGE_NAME = "com.huawei.contentsensor";
    private static final String TAG = "ContentSensorFactory";
    private static Class<IContentSensorManager> sContentSensorClz = null;
    private static volatile PackageInfo sPackageInfo = null;

    static class LogUtil {
        public static final String TAG = "ContentSensorFactory";
        private static boolean mIsDLogCanPrint;
        private static boolean mIsELogCanPrint;
        private static boolean mIsILogCanPrint;
        private static boolean mIsVLogCanPrint;
        private static boolean mIsWLogCanPrint;

        LogUtil() {
        }

        static {
            mIsVLogCanPrint = false;
            mIsDLogCanPrint = true;
            mIsILogCanPrint = true;
            mIsWLogCanPrint = true;
            mIsELogCanPrint = true;
            mIsVLogCanPrint = isNormalLogCanPrint("ContentSensorFactory", 2);
            mIsDLogCanPrint = isNormalLogCanPrint("ContentSensorFactory", 3);
            mIsILogCanPrint = isNormalLogCanPrint("ContentSensorFactory", 4);
            mIsWLogCanPrint = isNormalLogCanPrint("ContentSensorFactory", 5);
            mIsELogCanPrint = isNormalLogCanPrint("ContentSensorFactory", 6);
        }

        private static boolean isNormalLogCanPrint(String tag, int level) {
            return Log.isLoggable(tag, level);
        }

        public static void v(String className, String msg) {
            if (mIsVLogCanPrint) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(className);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                Log.v("ContentSensorFactory", stringBuilder.toString());
            }
        }

        public static void d(String className, String msg) {
            if (mIsDLogCanPrint) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(className);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                Log.d("ContentSensorFactory", stringBuilder.toString());
            }
        }

        public static void i(String className, String msg) {
            if (mIsILogCanPrint) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(className);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                Log.i("ContentSensorFactory", stringBuilder.toString());
            }
        }

        public static void w(String className, String msg) {
            if (mIsWLogCanPrint) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(className);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                Log.w("ContentSensorFactory", stringBuilder.toString());
            }
        }

        public static void e(String className, String msg) {
            if (mIsELogCanPrint) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(className);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                Log.e("ContentSensorFactory", stringBuilder.toString());
            }
        }

        public static void logException(String className, String msg, Exception e) {
            if (!mIsELogCanPrint) {
                return;
            }
            StringBuilder stringBuilder;
            if (TextUtils.isEmpty(msg)) {
                if (e != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(className);
                    stringBuilder.append(": ");
                    stringBuilder.append(msg);
                    Log.e("ContentSensorFactory", stringBuilder.toString());
                }
            } else if (e != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(className);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                stringBuilder.append(e.getMessage());
                Log.e("ContentSensorFactory", stringBuilder.toString(), e);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(className);
                stringBuilder.append(": ");
                stringBuilder.append(msg);
                Log.e("ContentSensorFactory", stringBuilder.toString());
            }
        }
    }

    static class DefaultContentSensorManager implements IContentSensorManager {
        DefaultContentSensorManager() {
        }

        public void updateToken(int token, Activity activity) {
        }

        public void copyNode(Bundle data) {
        }

        public void processImageAndWebView(Bundle data) {
        }
    }

    public static IContentSensorManager createContentSensorManager(int token, Activity activity) {
        StringBuilder stringBuilder;
        long currentTime = System.currentTimeMillis();
        IContentSensorManager sensor = null;
        ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            Class<IContentSensorManager> clz = getContentSensorManagerClass();
            if (clz != null) {
                Constructor constructor = clz.getConstructor(new Class[]{Integer.TYPE, Activity.class});
                constructor.setAccessible(true);
                sensor = (IContentSensorManager) constructor.newInstance(new Object[]{Integer.valueOf(token), activity});
            }
        } catch (ClassNotFoundException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("ClassNotFoundExceptione:");
            stringBuilder.append(e);
            LogUtil.e("ContentSensorFactory", stringBuilder.toString());
        } catch (NoSuchMethodException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("NoSuchMethodExceptione:");
            stringBuilder.append(e2);
            LogUtil.e("ContentSensorFactory", stringBuilder.toString());
        } catch (InstantiationException e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("InstantiationExceptione:");
            stringBuilder.append(e3);
            LogUtil.e("ContentSensorFactory", stringBuilder.toString());
        } catch (IllegalAccessException e4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("IllegalAccessExceptione:");
            stringBuilder.append(e4);
            LogUtil.e("ContentSensorFactory", stringBuilder.toString());
        } catch (InvocationTargetException e5) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("InvocationTargetExceptione:");
            stringBuilder.append(e5);
            LogUtil.e("ContentSensorFactory", stringBuilder.toString());
        } catch (Throwable th) {
            StrictMode.setThreadPolicy(oldPolicy);
        }
        StrictMode.setThreadPolicy(oldPolicy);
        if (sensor != null) {
            return sensor;
        }
        LogUtil.w("ContentSensorFactory", "DefaultContentSensorManager is created");
        return new DefaultContentSensorManager();
    }

    private static synchronized Class<IContentSensorManager> getContentSensorManagerClass() throws ClassNotFoundException {
        synchronized (ContentSensorManagerFactory.class) {
            if (sContentSensorClz != null) {
                Class cls = sContentSensorClz;
                return cls;
            }
            PackageInfo packageInfo = fetchPackageInfo();
            if (packageInfo == null) {
                return null;
            }
            try {
                sContentSensorClz = Class.forName(SENSOR_MANAGER_CLASS_NAME, true, AppGlobals.getInitialApplication().createPackageContext(packageInfo.packageName, 3).getClassLoader());
                Class cls2 = sContentSensorClz;
                return cls2;
            } catch (NameNotFoundException e) {
                LogUtil.e("ContentSensorFactory", "can not find class com.huawei.contentsensor.agent.ContentSensorManager");
                return null;
            }
        }
    }

    private static PackageInfo fetchPackageInfo() {
        if (sPackageInfo != null) {
            return sPackageInfo;
        }
        Application app = AppGlobals.getInitialApplication();
        if (app == null) {
            return null;
        }
        PackageManager pm = app.getPackageManager();
        if (pm == null) {
            return null;
        }
        try {
            sPackageInfo = pm.getPackageInfo(SENSOR_MANAGER_PACKAGE_NAME, 128);
        } catch (NameNotFoundException e) {
            LogUtil.e("ContentSensorFactory", "can not find package com.huawei.contentsensor");
        }
        return sPackageInfo;
    }
}
