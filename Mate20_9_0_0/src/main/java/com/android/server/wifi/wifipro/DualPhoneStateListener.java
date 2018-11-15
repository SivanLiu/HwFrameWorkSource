package com.android.server.wifi.wifipro;

import android.os.Build.VERSION;
import android.telephony.PhoneStateListener;
import android.util.Log;
import java.lang.reflect.Field;

public class DualPhoneStateListener extends PhoneStateListener {
    private static final Field FIELD_subID;
    private static int LOLLIPOP_VER = 21;
    private static String TAG = "MQoS";

    static {
        Field declaredField;
        if (VERSION.SDK_INT >= LOLLIPOP_VER) {
            declaredField = getDeclaredField(PhoneStateListener.class, "mSubId");
        } else {
            declaredField = getDeclaredField(PhoneStateListener.class, "mSubscription");
        }
        FIELD_subID = declaredField;
    }

    public DualPhoneStateListener(int subscription) {
        if (FIELD_subID != null) {
            if (!FIELD_subID.isAccessible()) {
                FIELD_subID.setAccessible(true);
            }
            if (VERSION.SDK_INT == LOLLIPOP_VER) {
                setFieldValue(this, FIELD_subID, Long.valueOf((long) subscription));
                return;
            } else {
                setFieldValue(this, FIELD_subID, Integer.valueOf(subscription));
                return;
            }
        }
        throw new UnsupportedOperationException();
    }

    public static int getSubscription(PhoneStateListener obj) {
        if (FIELD_subID != null) {
            if (!FIELD_subID.isAccessible()) {
                FIELD_subID.setAccessible(true);
            }
            if (VERSION.SDK_INT == LOLLIPOP_VER) {
                return ((Long) getFieldValue(obj, FIELD_subID)).intValue();
            }
            return ((Integer) getFieldValue(obj, FIELD_subID)).intValue();
        }
        throw new UnsupportedOperationException();
    }

    private static boolean isEmpty(String str) {
        boolean z = true;
        if (str == null || str.length() == 0) {
            return true;
        }
        if (str.trim().length() != 0) {
            z = false;
        }
        return z;
    }

    private static Field getDeclaredField(Class<?> targetClass, String name) {
        String str;
        StringBuilder stringBuilder;
        if (targetClass == null || isEmpty(name)) {
            return null;
        }
        try {
            return targetClass.getDeclaredField(name);
        } catch (SecurityException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append(":");
            stringBuilder.append(e.getCause());
            Log.e(str, stringBuilder.toString());
            return null;
        } catch (NoSuchFieldException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(name);
            stringBuilder.append(",no such field.");
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    private static void setFieldValue(Object receiver, Field field, Object value) {
        if (field != null) {
            try {
                field.set(receiver, value);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception in setFieldValue: ");
                stringBuilder.append(e.getClass().getSimpleName());
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    private static Object getFieldValue(Object receiver, Field field) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(receiver);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in getFieldValue: ");
            stringBuilder.append(e.getClass().getSimpleName());
            Log.e(str, stringBuilder.toString());
            throw new UnsupportedOperationException();
        }
    }
}
