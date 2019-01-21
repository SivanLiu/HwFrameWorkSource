package android.rms.utils;

import android.app.AppGlobals;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Utils {
    public static final String BUNDLE_CURRENT_COUNT = "current_count";
    public static final String BUNDLE_HARD_THRESHOLD = "hard_threshold";
    public static final String BUNDLE_IS_IN_WHITELIST = "isInWhiteList";
    public static final String BUNDLE_THIRD_PARTY_APP_LIFETIME = "third_party_app_lifetime";
    public static final String BUNDLE_THIRD_PARTY_APP_USETIME = "third_party_app_usetime";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_FORMAT_DETAIL = "yyyy-MM-dd hh:mm:ss";
    public static final long DATE_TIME_24HOURS = 86400000;
    public static final boolean DEBUG;
    public static final int FLAG_CRASH_MONITOR = 2;
    public static final int FLAG_IO_STATISTIC = 1;
    public static final boolean HWFLOW;
    public static final boolean HWLOGW_E = true;
    public static final boolean IS_DEBUG_VERSION;
    private static final String PARAM_SPLIT = ":";
    public static final int RMSVERSION = SystemProperties.getInt("ro.config.RmsVersion", 2);
    public static final int SELF_COUNT_RESOURCE = -1;
    public static final String TAG = "RMS";

    static {
        boolean z = false;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        DEBUG = z2;
        z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z2;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3) {
            z = true;
        }
        IS_DEBUG_VERSION = z;
    }

    public static final boolean writeFile(String path, String data) {
        FileOutputStream fos = null;
        boolean success = true;
        try {
            fos = new FileOutputStream(path);
            fos.write(data.getBytes("UTF-8"));
            try {
                fos.close();
            } catch (IOException e) {
                Log.w(TAG, "writeFile : IOException when close");
            }
        } catch (IOException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to write ");
            stringBuilder.append(path);
            stringBuilder.append(" msg=");
            stringBuilder.append(e2.getMessage());
            Log.w(str, stringBuilder.toString());
            success = false;
            if (fos != null) {
                fos.close();
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e3) {
                    Log.w(TAG, "writeFile : IOException when close");
                }
            }
        }
        return success;
    }

    public static final boolean generateDirectory(String path) {
        boolean flag = false;
        if (path == null) {
            return false;
        }
        try {
            File directory = new File(path);
            if (!directory.exists()) {
                flag = directory.mkdirs();
            }
        } catch (SecurityException e) {
            Log.w(TAG, "mkdir fail");
        }
        return flag;
    }

    public static final void wait(int ms) {
        try {
            Thread.sleep((long) ms);
        } catch (InterruptedException e) {
        }
    }

    public static final String getPackageNameByUid(int uid) {
        String pkgName = "";
        try {
            return AppGlobals.getPackageManager().getNameForUid(uid);
        } catch (RemoteException e) {
            Log.e(TAG, "get pkg name fail");
            return pkgName;
        }
    }

    public static boolean scanArgs(String[] args, String value) {
        if (args != null) {
            for (String arg : args) {
                if (value.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String scanArgsWithParam(String[] args, String key) {
        if (args == null || key == null) {
            Log.e(TAG, "scanArgsWithParam,neither args or key is null");
            return null;
        }
        String result = null;
        for (String arg : args) {
            if (arg != null && arg.contains(key)) {
                String[] splitsArray = arg.split(PARAM_SPLIT);
                if (splitsArray.length < 2) {
                    break;
                }
                result = splitsArray[1];
            }
        }
        return result;
    }

    public static Object invokeMethod(Object instance, String methodName, Class[] parameterType, Object... argsValues) {
        String str;
        StringBuilder stringBuilder;
        Object resultObj = null;
        if (instance == null) {
            Log.e(TAG, "invokeMethod,instance is null");
            return null;
        }
        try {
            Method method;
            Class<?> classObj = instance.getClass();
            if (parameterType != null) {
                method = classObj.getDeclaredMethod(methodName, parameterType);
            } else {
                method = classObj.getDeclaredMethod(methodName, new Class[0]);
            }
            final Method methodResult = method;
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Void run() {
                    methodResult.setAccessible(true);
                    return null;
                }
            });
            resultObj = method.invoke(instance, argsValues);
        } catch (RuntimeException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invokeMethod,RuntimeException method:");
            stringBuilder.append(methodName);
            stringBuilder.append(",msg:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (NoSuchMethodException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invokeMethod,no such method:");
            stringBuilder.append(methodName);
            stringBuilder.append(",msg:");
            stringBuilder.append(e2.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (IllegalAccessException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invokeMethod,IllegalAccessException,method:");
            stringBuilder.append(methodName);
            stringBuilder.append(",msg:");
            stringBuilder.append(e3.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (InvocationTargetException ex) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invokeMethod,Exception,method:");
            stringBuilder.append(methodName);
            stringBuilder.append(",msg:");
            stringBuilder.append(ex.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        return resultObj;
    }

    public static String getDateFormatValue(long time) {
        return new SimpleDateFormat(DATE_FORMAT, Locale.US).format(new Date(time));
    }

    public static long getShortDateFormatValue(long time) {
        SimpleDateFormat sdFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        try {
            return sdFormatter.parse(sdFormatter.format(new Date(time))).getTime();
        } catch (ParseException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getShortDateFormatValue:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return 0;
        }
    }

    public static long getDifferencesByDay(long time1, long time2) {
        return (time1 - time2) / DATE_TIME_24HOURS;
    }

    public static long getSizeOfDirectory(File directory) {
        long totalSizeInDirectory = 0;
        try {
            if (directory.exists()) {
                String[] subFiles = directory.list();
                if (subFiles != null) {
                    for (String name : subFiles) {
                        totalSizeInDirectory += new File(directory, name).length();
                    }
                }
                return totalSizeInDirectory;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSizeOfDirectory,");
            stringBuilder.append(directory.getCanonicalPath());
            stringBuilder.append(" not exists");
            Log.e(str, stringBuilder.toString());
            return 0;
        } catch (IOException ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getSizeOfDirectory,IOException occurs:");
            stringBuilder2.append(ex.getMessage());
            Log.e(str2, stringBuilder2.toString());
        }
    }

    public static int getCompactPeriodInterval() {
        int compactPeriodInterval = SystemProperties.getInt("rms_debug_interval", 300000);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Debug CompactPeriodInterval is : ");
        stringBuilder.append(compactPeriodInterval);
        Log.w(str, stringBuilder.toString());
        return compactPeriodInterval;
    }

    public static String getDateFormatValue(long time, String format) {
        return new SimpleDateFormat(format).format(new Date(time));
    }
}
