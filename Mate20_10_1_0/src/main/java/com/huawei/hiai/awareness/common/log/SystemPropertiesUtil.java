package com.huawei.hiai.awareness.common.log;

import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;

public class SystemPropertiesUtil {
    private static final String DEBUG_OFF = "0";
    private static final String DEBUG_ON = "1";
    private static final String GET_METHOD = "get";
    private static final String PROPERTY_CLASS = "com.huawei.android.os.SystemPropertiesEx";
    private static final String PROPERTY_DEBUG_ON = "persist.sys.huawei.debug.on";
    private static final String TAG = "SystemPropertiesUtil";

    private SystemPropertiesUtil() {
    }

    public static boolean isDebugOn() {
        String debugOnFlag = getProp(PROPERTY_DEBUG_ON, "0");
        Log.i(TAG, " CAWARENESS_CLIENT: debugOnFlag is:" + debugOnFlag);
        if ("1".equals(debugOnFlag)) {
            return true;
        }
        return false;
    }

    private static String getProp(String name, String defaultValue) {
        try {
            Class systemProperties = Class.forName(PROPERTY_CLASS);
            Object object = systemProperties.getDeclaredMethod(GET_METHOD, String.class, String.class).invoke(systemProperties.newInstance(), name, defaultValue);
            String value = null;
            if (object != null && (object instanceof String)) {
                value = (String) object;
            }
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
            return defaultValue;
        } catch (InstantiationException e) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil InstantiationException");
        } catch (InvocationTargetException e2) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil InvocationTargetException");
        } catch (NoSuchMethodException e3) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil NoSuchMethodException");
        } catch (IllegalAccessException e4) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil IllegalAccessException");
        } catch (ClassNotFoundException e5) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil ClassNotFoundException");
        } catch (SecurityException e6) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil SecurityException");
        } catch (IllegalArgumentException e7) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil IllegalArgumentException");
        } catch (ClassCastException e8) {
            Log.e(TAG, " CAWARENESS_CLIENT: SystemPropertiesUtil ClassCastException");
        }
    }
}
