package com.huawei.opcollect.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class ReflectionUtils {
    private static final String TAG = "ReflectionUtils";

    private ReflectionUtils() {
        OPCollectLog.e(TAG, "static class should not initialize.");
    }

    public static Class<?> getClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            OPCollectLog.e(TAG, "ReflectionUtils : className not found:" + className);
            return null;
        }
    }

    public static Method getMethod(String className, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        Class<?> targetClass = getClass(className);
        if (targetClass == null || methodName == null) {
            return method;
        }
        try {
            return targetClass.getMethod(methodName, parameterTypes);
        } catch (SecurityException e) {
            OPCollectLog.e(TAG, "ReflectionUtils : " + e.getMessage());
            return method;
        } catch (NoSuchMethodException e2) {
            OPCollectLog.e(TAG, "ReflectionUtils : " + methodName + ", not such method.");
            return method;
        }
    }

    public static Object invoke(Method method, Object receiver, Object... args) {
        Exception e;
        if (method != null) {
            try {
                return method.invoke(receiver, args);
            } catch (RuntimeException e2) {
                e = e2;
            } catch (IllegalAccessException e3) {
                e = e3;
            } catch (InvocationTargetException e4) {
                e = e4;
            }
        }
        return null;
        OPCollectLog.e(TAG, "failed to invoke, method is " + method.getName() + ", cause: " + e.getMessage());
        return null;
    }

    public static Object invoke(Method method, Object receiver) {
        Exception e;
        if (method != null) {
            try {
                return method.invoke(receiver, new Object[0]);
            } catch (RuntimeException e2) {
                e = e2;
            } catch (IllegalAccessException e3) {
                e = e3;
            } catch (InvocationTargetException e4) {
                e = e4;
            }
        }
        return null;
        OPCollectLog.e(TAG, "failed to invoke, method is " + method.getName() + ", cause: " + e.getMessage());
        return null;
    }

    public static Constructor getConstructor(String className, Class<?>... parameterTypes) {
        try {
            Class<?> clz = getClass(className);
            Constructor conClz = null;
            if (clz != null) {
                conClz = clz.getDeclaredConstructor(parameterTypes);
            }
            if (conClz == null) {
                return conClz;
            }
            conClz.setAccessible(true);
            return conClz;
        } catch (NoSuchMethodException e) {
            OPCollectLog.e(TAG, "ReflectionUtils : NoSuchMethodException->" + e.getMessage());
            return null;
        } catch (SecurityException e2) {
            OPCollectLog.e(TAG, "ReflectionUtils : SecurityException->" + e2.getMessage());
            return null;
        }
    }

    public static Object newProxyInstance(String className, InvocationHandler h) {
        if (h == null || className == null) {
            return null;
        }
        try {
            Class clazz = getClass(className);
            ClassLoader classLoader = null;
            if (clazz != null) {
                classLoader = clazz.getClassLoader();
            }
            if (classLoader == null) {
                return null;
            }
            return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, h);
        } catch (IllegalArgumentException e) {
            OPCollectLog.e(TAG, "ReflectionUtils: IllegalArgumentException " + e.getMessage());
            return null;
        } catch (NullPointerException e2) {
            OPCollectLog.e(TAG, "ReflectionUtils: NullPointerException " + e2.getMessage());
            return null;
        }
    }

    public static Field getField(String className, String fieldName) {
        if (className == null || fieldName == null) {
            return null;
        }
        try {
            Class clazz = getClass(className);
            if (clazz != null) {
                return clazz.getDeclaredField(fieldName);
            }
            return null;
        } catch (SecurityException e) {
            OPCollectLog.e(TAG, "ReflectionUtils SecurityException: " + e.getMessage());
            return null;
        } catch (NoSuchFieldException e2) {
            OPCollectLog.e(TAG, "ReflectionUtils : " + fieldName + ", not such field.");
            return null;
        } catch (NullPointerException e3) {
            OPCollectLog.e(TAG, "ReflectionUtils NullPointerException: " + e3.getMessage());
            return null;
        }
    }

    public static Object getFieldValue(String className, Object targetObject, String fieldName) {
        if (className == null || targetObject == null || fieldName == null) {
            return null;
        }
        try {
            Class clazz = getClass(className);
            Field targetField = getField(className, fieldName);
            if (targetField == null) {
                return null;
            }
            targetField.setAccessible(true);
            return targetField.get(targetObject);
        } catch (SecurityException e) {
            OPCollectLog.e(TAG, "ReflectionUtils SecurityException: " + e.getMessage());
            return null;
        } catch (NullPointerException e2) {
            OPCollectLog.e(TAG, "ReflectionUtils NullPointerException: " + e2.getMessage());
            return null;
        } catch (IllegalAccessException e3) {
            OPCollectLog.e(TAG, "ReflectionUtils IllegalAccessException: " + e3.getMessage());
            return null;
        }
    }
}
