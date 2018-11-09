package huawei.android.widget.DecouplingUtil;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {
    private static String TAG = "ReflectUtil";

    public static void setObject(String reflectName, Object instance, Object object1, Class<?> clazz) {
        if (instance == null) {
            Log.w(TAG, "reflect setObject instance is null");
            return;
        }
        try {
            Field field = clazz.getDeclaredField(reflectName);
            field.setAccessible(true);
            field.set(instance, object1);
            Log.i(TAG, "reflect " + reflectName + " success in set object");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Log.e(TAG, "no field in reflect " + reflectName + " in set object");
        } catch (SecurityException e2) {
            Log.e(TAG, "SecurityException in reflect " + reflectName + " in set object");
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "IllegalArgumentException in reflect " + reflectName + " in set object");
        } catch (IllegalAccessException e4) {
            Log.e(TAG, "IllegalAccessException in reflect " + reflectName + " in set object");
        }
    }

    public static Object getObject(Object instance, String reflectName, Class<?> clazz) {
        if (instance == null) {
            Log.w(TAG, "reflect getObject instance is null");
            return null;
        }
        try {
            Field field = clazz.getDeclaredField(reflectName);
            field.setAccessible(true);
            Log.i(TAG, "reflect " + reflectName + " success in get object");
            return field.get(instance);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            Log.e(TAG, "no field in reflect " + reflectName + " in get object");
            return null;
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "IllegalAccessException in reflect " + reflectName + " in get object");
            return null;
        }
    }

    public static Object callMethod(Object instance, String methodName, Class[] classesArgs, Object[] objectsArgs, Class<?> clazz) {
        if (instance == null) {
            Log.w(TAG, "reflect callMethod instance is null");
            return null;
        }
        try {
            Method method = clazz.getDeclaredMethod(methodName, classesArgs);
            method.setAccessible(true);
            Log.i(TAG, "reflect " + methodName + " success in call method");
            return method.invoke(instance, objectsArgs);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "there is no " + methodName + "method");
            return null;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "IllegalArgumentException in reflect call " + methodName);
            return null;
        } catch (IllegalAccessException e3) {
            Log.e(TAG, "IllegalAccessException in reflect call " + methodName);
            return null;
        } catch (InvocationTargetException e4) {
            Log.e(TAG, "InvocationTargetException in reflect call " + methodName);
            return null;
        }
    }
}
