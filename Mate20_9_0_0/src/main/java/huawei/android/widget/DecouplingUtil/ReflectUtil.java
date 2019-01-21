package huawei.android.widget.DecouplingUtil;

import android.util.Log;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {
    private static String TAG = "ReflectUtil";

    public static void setObject(String reflectName, Object instance, Object object1, Class<?> clazz) {
        String str;
        StringBuilder stringBuilder;
        if (instance == null) {
            Log.w(TAG, "reflect setObject instance is null");
            return;
        }
        try {
            Field field = clazz.getDeclaredField(reflectName);
            field.setAccessible(true);
            field.set(instance, object1);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("no field in reflect ");
            stringBuilder.append(reflectName);
            stringBuilder.append(" in set object");
            Log.e(str, stringBuilder.toString());
        } catch (SecurityException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SecurityException in reflect ");
            stringBuilder.append(reflectName);
            stringBuilder.append(" in set object");
            Log.e(str, stringBuilder.toString());
        } catch (IllegalArgumentException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IllegalArgumentException in reflect ");
            stringBuilder.append(reflectName);
            stringBuilder.append(" in set object");
            Log.e(str, stringBuilder.toString());
        } catch (IllegalAccessException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IllegalAccessException in reflect ");
            stringBuilder.append(reflectName);
            stringBuilder.append(" in set object");
            Log.e(str, stringBuilder.toString());
        }
    }

    public static Object getObject(Object instance, String reflectName, Class<?> clazz) {
        String str;
        StringBuilder stringBuilder;
        if (instance == null) {
            Log.w(TAG, "reflect getObject instance is null");
            return null;
        }
        try {
            Field field = clazz.getDeclaredField(reflectName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("no field in reflect ");
            stringBuilder.append(reflectName);
            stringBuilder.append(" in get object");
            Log.e(str, stringBuilder.toString());
            return null;
        } catch (IllegalAccessException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IllegalAccessException in reflect ");
            stringBuilder.append(reflectName);
            stringBuilder.append(" in get object");
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static Object callMethod(Object instance, String methodName, Class[] classesArgs, Object[] objectsArgs, Class<?> clazz) {
        String str;
        StringBuilder stringBuilder;
        if (instance == null) {
            Log.w(TAG, "reflect callMethod instance is null");
            return null;
        }
        try {
            Method method = clazz.getDeclaredMethod(methodName, classesArgs);
            method.setAccessible(true);
            return method.invoke(instance, objectsArgs);
        } catch (NoSuchMethodException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("there is no ");
            stringBuilder.append(methodName);
            stringBuilder.append("method");
            Log.e(str, stringBuilder.toString());
            return null;
        } catch (IllegalArgumentException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IllegalArgumentException in reflect call ");
            stringBuilder.append(methodName);
            Log.e(str, stringBuilder.toString());
            return null;
        } catch (IllegalAccessException e3) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IllegalAccessException in reflect call ");
            stringBuilder.append(methodName);
            Log.e(str, stringBuilder.toString());
            return null;
        } catch (InvocationTargetException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("InvocationTargetException in reflect call ");
            stringBuilder.append(methodName);
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static Class<?> getPrivateClass(String clazzName) {
        try {
            return Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPrivateClass: no class named ");
            stringBuilder.append(clazzName);
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static Object createPrivateInnerInstance(Class<?> clazz, Class<?> outClass, Object outInstance, Class[] argsSignature, Object[] argsInstance) {
        if (clazz == null) {
            return null;
        }
        Constructor<?> constructor;
        if (argsSignature == null || argsInstance == null) {
            constructor = clazz.getDeclaredConstructor(new Class[]{outClass});
            constructor.setAccessible(true);
            return constructor.newInstance(new Object[]{outInstance});
        }
        try {
            constructor = clazz.getDeclaredConstructor(argsSignature);
            constructor.setAccessible(true);
            return constructor.newInstance(argsInstance);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, "createPrivateInnerInstance: no constructor");
            return null;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            Log.e(TAG, "createPrivateInnerInstance: IllegalAccessException");
            return null;
        } catch (InstantiationException e3) {
            e3.printStackTrace();
            Log.e(TAG, "createPrivateInnerInstance: InstantiationException");
            return null;
        } catch (InvocationTargetException e4) {
            e4.printStackTrace();
            Log.e(TAG, "createPrivateInnerInstance: InvocationTargetException");
            return null;
        }
    }
}
