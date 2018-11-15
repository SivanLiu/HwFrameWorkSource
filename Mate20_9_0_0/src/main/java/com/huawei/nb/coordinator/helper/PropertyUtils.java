package com.huawei.nb.coordinator.helper;

import com.huawei.nb.utils.logger.DSLog;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PropertyUtils {
    private static volatile Method get = null;

    private static String getProperty(String prop, String defaultValue) {
        ReflectiveOperationException e;
        String value = "";
        DSLog.d("PropertyUtils defaultValue: " + defaultValue, new Object[0]);
        try {
            if (get == null) {
                synchronized (PropertyUtils.class) {
                    if (get == null) {
                        get = Class.forName("android.os.SystemProperties").getDeclaredMethod("get", new Class[]{String.class, String.class});
                    }
                }
            }
            return (String) get.invoke(null, new Object[]{prop, defaultValue});
        } catch (InvocationTargetException e2) {
            e = e2;
        } catch (NoSuchMethodException e3) {
            e = e3;
        } catch (ClassNotFoundException e4) {
            e = e4;
        } catch (IllegalAccessException e5) {
            e = e5;
        }
        DSLog.e("PropertyUtils failed: " + e.getMessage(), new Object[0]);
        return null;
    }

    public static boolean isSupportHwPKI() {
        return "true".equalsIgnoreCase(getProperty("ro.config.support_hwpki", "false"));
    }
}
