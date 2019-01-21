package com.huawei.android.pushagent.utils.d;

import com.huawei.android.pushagent.utils.b.a;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public abstract class j {
    public static Field[] vp(Class cls) {
        Object obj = null;
        if (cls.getSuperclass() != null) {
            obj = vp(cls.getSuperclass());
        }
        Field[] declaredFields = cls.getDeclaredFields();
        if (obj == null || obj.length <= 0) {
            return declaredFields;
        }
        Field[] fieldArr = new Field[(declaredFields.length + obj.length)];
        System.arraycopy(obj, 0, fieldArr, 0, obj.length);
        System.arraycopy(declaredFields, 0, fieldArr, obj.length, declaredFields.length);
        return fieldArr;
    }

    public static Class vr(Field field) {
        if (Map.class.isAssignableFrom(field.getType())) {
            return vs(field, 1);
        }
        if (List.class.isAssignableFrom(field.getType())) {
            return vs(field, 0);
        }
        return null;
    }

    private static Class vs(Field field, int i) {
        int i2 = 0;
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }
        Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
        if (actualTypeArguments == null || actualTypeArguments.length <= i) {
            return null;
        }
        try {
            if (actualTypeArguments[i] instanceof Class) {
                return (Class) actualTypeArguments[i];
            }
            String obj = actualTypeArguments[i].toString();
            int indexOf = obj.indexOf("class ");
            if (indexOf >= 0) {
                i2 = indexOf;
            }
            indexOf = obj.indexOf("<");
            if (indexOf < 0) {
                indexOf = obj.length();
            }
            return Class.forName(obj.substring(i2, indexOf));
        } catch (ClassNotFoundException e) {
            a.sx("PushLog3414", "getType ClassNotFoundException");
            return null;
        } catch (Exception e2) {
            a.sx("PushLog3414", "getType Exception");
            return null;
        }
    }

    public static Field vq(Field field, boolean z) {
        field.setAccessible(z);
        return field;
    }
}
