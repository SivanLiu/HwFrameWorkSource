package com.huawei.android.pushagent.utils.d;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.b.b;
import com.huawei.android.pushagent.datatype.b.c;
import com.huawei.android.pushagent.utils.f;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class a {
    private static final Class[] hn = new Class[]{String.class, Object.class, Integer.class, Short.class, Long.class, Byte.class, Float.class, Double.class, Boolean.class};
    private static final Class[] ho = new Class[]{String.class, Object.class, Integer.class, Short.class, Long.class, Byte.class, Float.class, Double.class, Character.class, Boolean.class};
    private static final Map<Class, b> hp = new HashMap();

    static {
        c cVar = new c();
        hp.put(Integer.TYPE, cVar);
        hp.put(Integer.class, cVar);
        d dVar = new d();
        hp.put(Long.TYPE, dVar);
        hp.put(Long.class, dVar);
        e eVar = new e();
        hp.put(Float.TYPE, eVar);
        hp.put(Float.class, eVar);
        f fVar = new f();
        hp.put(Double.TYPE, fVar);
        hp.put(Double.class, fVar);
        g gVar = new g();
        hp.put(Short.TYPE, gVar);
        hp.put(Short.class, gVar);
        h hVar = new h();
        hp.put(Byte.TYPE, hVar);
        hp.put(Byte.class, hVar);
        i iVar = new i();
        hp.put(Boolean.TYPE, iVar);
        hp.put(Boolean.class, iVar);
    }

    public static <T> T vf(String str, Class<T> cls, Class... clsArr) {
        if (TextUtils.isEmpty(str)) {
            throw uw(false, "Input json string cannot be empty!", new Object[0]);
        }
        uk(cls);
        return ul(str, cls, clsArr);
    }

    private static <T> T ul(String str, Class<T> cls, Class[] clsArr) {
        try {
            return vc(new JSONObject(str), cls, clsArr);
        } catch (JSONException e) {
            try {
                return vb(new JSONArray(str), cls, clsArr);
            } catch (JSONException e2) {
                throw uv("Input string is not valid json string!", new Object[0]);
            }
        }
    }

    public static <T> T ui(String str, Class<T> cls, Class... clsArr) {
        try {
            return vf(str, cls, clsArr);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "toObject JSONException");
            return null;
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "toObject error");
            return null;
        }
    }

    public static String uh(Object obj) {
        try {
            return ve(obj, false);
        } catch (JSONException e) {
            throw e;
        } catch (IllegalAccessException e2) {
            throw uv("toJson error", new Object[0]);
        }
    }

    private static String ve(Object obj, boolean z) {
        if (obj == null) {
            return "";
        }
        uk(obj.getClass());
        if (obj instanceof List) {
            return uu((List) obj, z);
        }
        if (obj instanceof Map) {
            return uy((Map) obj, z);
        }
        if (obj instanceof JSONObject) {
            return obj.toString();
        }
        return uz(obj, z);
    }

    private static String uz(Object obj, boolean z) {
        Field[] vp = j.vp(obj.getClass());
        if (vp.length <= 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        int length = vp.length;
        for (int i = 0; i < length; i++) {
            vp[i] = j.vq(vp[i], true);
            if (ur(vp[i])) {
                String un = un(vp[i]);
                Object obj2 = vp[i].get(obj);
                String vh = (z && vp[i].isAnnotationPresent(b.class)) ? obj2 != null ? "\"******\"" : null : vh(obj2, z);
                if (vh != null) {
                    stringBuilder.append('\"').append(un).append("\":").append(vh);
                    if (i < length - 1) {
                        stringBuilder.append(',');
                    }
                }
            }
        }
        um(stringBuilder);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static String vh(Object obj, boolean z) {
        if (obj == null) {
            return null;
        }
        String str;
        if ((obj instanceof String) || (obj instanceof Character)) {
            str = "\"" + f.zu(obj.toString()) + "\"";
        } else if ((obj instanceof Integer) || (obj instanceof Long) || (obj instanceof Boolean) || (obj instanceof Float) || (obj instanceof Byte) || (obj instanceof Double) || (obj instanceof Short)) {
            str = obj.toString();
        } else if (obj instanceof List) {
            str = uu((List) obj, z);
        } else if (obj instanceof Map) {
            str = uy((Map) obj, z);
        } else if (obj.getClass().isArray()) {
            str = uj(obj, z);
        } else {
            str = ve(obj, z);
        }
        return str;
    }

    private static String uu(List list, boolean z) {
        if (list.size() <= 0) {
            return "[]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < list.size(); i++) {
            String vh = vh(list.get(i), z);
            if (vh != null) {
                stringBuilder.append(vh).append(',');
            }
        }
        um(stringBuilder);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private static String uj(Object obj, boolean z) {
        int length = Array.getLength(obj);
        if (length <= 0) {
            return "[]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < length; i++) {
            String vh = vh(Array.get(obj, i), z);
            if (vh != null) {
                stringBuilder.append(vh).append(',');
            }
        }
        um(stringBuilder);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private static String uy(Map map, boolean z) {
        if (map.size() <= 0) {
            return "{}";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        Set<Entry> entrySet = map.entrySet();
        int size = entrySet.size();
        int i = 0;
        for (Entry entry : entrySet) {
            int i2 = i + 1;
            String str = (String) entry.getKey();
            String vh = vh(entry.getValue(), z);
            if (vh != null) {
                stringBuilder.append('\"').append(str).append("\":");
                stringBuilder.append(vh);
            }
            if (i2 < size && vh != null) {
                stringBuilder.append(',');
            }
            i = i2;
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static void um(StringBuilder stringBuilder) {
        int length = stringBuilder.length();
        if (length > 0 && stringBuilder.charAt(length - 1) == ',') {
            stringBuilder.delete(length - 1, length);
        }
    }

    private static void uk(Class cls) {
        if (cls.isPrimitive()) {
            throw uv("Root obj class (%s) cannot be primitive type!", cls);
        }
        for (Class cls2 : ho) {
            if (cls == cls2) {
                throw uv("Root obj class (%s) is invalid in conversion", cls);
            }
        }
    }

    private static <T> T vc(JSONObject jSONObject, Class<T> cls, Class[] clsArr) {
        Class cls2 = null;
        if (Collection.class.isAssignableFrom(cls)) {
            throw uv("Obj class %s is Collection type which mismatches with JsonObject", cls);
        } else if (cls.isArray()) {
            throw uv("Obj class %s is array type which mismatches with JsonObject", cls);
        } else if (Map.class.isAssignableFrom(cls)) {
            if (clsArr != null && clsArr.length > 0) {
                cls2 = clsArr[0];
            }
            return ux(cls, cls2, jSONObject);
        } else {
            try {
                return uo(jSONObject, cls.getConstructor(new Class[0]).newInstance(new Object[0]));
            } catch (NoSuchMethodException e) {
                throw uv("No default constructor for class %s", cls);
            } catch (IllegalAccessException e2) {
                throw uv("New instance failed for %s", cls);
            } catch (InstantiationException e3) {
                throw uv("New instance failed for %s", cls);
            } catch (InvocationTargetException e4) {
                throw uv("New instance failed for %s", cls);
            }
        }
    }

    private static <T> T uo(JSONObject jSONObject, T t) {
        Field[] vp = j.vp(t.getClass());
        for (Field vq : vp) {
            Field vq2 = j.vq(vq2, true);
            if (ur(vq2)) {
                Object opt = jSONObject.opt(un(vq2));
                if (!(opt == null || JSONObject.NULL == opt)) {
                    up(t, vq2, opt);
                }
            }
        }
        return t;
    }

    private static void up(Object obj, Field field, Object obj2) {
        Object obj3 = null;
        try {
            obj3 = vg(field.getType(), j.vr(field), obj2);
            field.set(obj, obj3);
        } catch (RuntimeException e) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", obj.getClass().getName() + ".fromJson runtime exception, fieldName: " + field.getName() + ", field: " + field);
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", obj.getClass().getName() + ".fromJson error, fieldName: " + field.getName() + ", field:" + field);
            va(obj, field, obj3);
        }
    }

    private static void va(Object obj, Field field, Object obj2) {
        if (obj2 != null && ((obj2 instanceof String) ^ 1) == 0) {
            try {
                Class type = field.getType();
                if (type.isPrimitive()) {
                    if (Integer.TYPE == type) {
                        field.set(obj, Integer.valueOf(Integer.parseInt((String) obj2)));
                    } else if (Float.TYPE == type) {
                        field.set(obj, Float.valueOf(Float.parseFloat((String) obj2)));
                    } else if (Long.TYPE == type) {
                        field.set(obj, Long.valueOf(Long.parseLong((String) obj2)));
                    } else if (Boolean.TYPE == type) {
                        field.set(obj, Boolean.valueOf(Boolean.parseBoolean((String) obj2)));
                    } else if (Double.TYPE == type) {
                        field.set(obj, Double.valueOf(Double.parseDouble((String) obj2)));
                    } else if (Short.TYPE == type) {
                        field.set(obj, Short.valueOf(Short.parseShort((String) obj2)));
                    } else if (Byte.TYPE == type) {
                        field.set(obj, Byte.valueOf(Byte.parseByte((String) obj2)));
                    } else if (Character.TYPE == type) {
                        field.set(obj, Character.valueOf(((String) obj2).charAt(0)));
                    }
                }
            } catch (Throwable th) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "processValueError");
            }
        }
    }

    private static boolean uq(Class cls) {
        if (cls.isPrimitive()) {
            return true;
        }
        for (Class cls2 : hn) {
            if (cls == cls2) {
                return true;
            }
        }
        return false;
    }

    private static Object vg(Class cls, Class cls2, Object obj) {
        if (uq(cls)) {
            return vd(cls, obj);
        }
        if (List.class.isAssignableFrom(cls)) {
            return ut(cls, cls2, obj);
        }
        if (Map.class.isAssignableFrom(cls)) {
            return ux(cls, cls2, obj);
        }
        if (obj instanceof JSONObject) {
            return vc((JSONObject) obj, cls, new Class[]{cls2});
        } else if (obj instanceof JSONArray) {
            return vb((JSONArray) obj, cls, new Class[]{cls2});
        } else {
            throw uv("value from json error, field class: %s", cls);
        }
    }

    private static Object vd(Class cls, Object obj) {
        if (String.class == cls) {
            return f.zv(obj);
        }
        b bVar;
        if ((cls.isPrimitive() || Number.class.isAssignableFrom(cls)) && (obj instanceof Number)) {
            Number number = (Number) obj;
            bVar = (b) hp.get(cls);
            if (bVar != null) {
                return bVar.vi(number);
            }
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "cannot find value reader for:" + cls);
            return null;
        } else if (cls != Boolean.class) {
            return obj;
        } else {
            bVar = (b) hp.get(cls);
            if (bVar != null) {
                return bVar.vi(obj);
            }
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "cannot find value reader for:" + cls);
            return null;
        }
    }

    private static Map ux(Class cls, Class cls2, Object obj) {
        if (cls2 == null) {
            cls2 = String.class;
        }
        if (obj instanceof JSONObject) {
            Map linkedHashMap;
            if (Map.class == cls) {
                linkedHashMap = new LinkedHashMap();
            } else if (Map.class.isAssignableFrom(cls)) {
                try {
                    linkedHashMap = (Map) cls.newInstance();
                } catch (InstantiationException e) {
                    throw uv("Fail to initiate %s", cls);
                } catch (IllegalAccessException e2) {
                    throw uv("Fail to initiate %s", cls);
                }
            } else {
                throw uv("%s is not Map type", cls);
            }
            JSONObject jSONObject = (JSONObject) obj;
            Iterator keys = jSONObject.keys();
            while (keys.hasNext()) {
                String str = (String) keys.next();
                Object vg = vg(cls2, null, jSONObject.get(str));
                if (vg != null) {
                    if (cls2.isAssignableFrom(vg.getClass())) {
                        linkedHashMap.put(str, vg);
                    } else {
                        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "mapFromJson error, memberClass:" + cls2 + ", valueClass:" + vg.getClass());
                    }
                }
            }
            return linkedHashMap;
        }
        throw uv("jsonValue is not JSONObject", new Object[0]);
    }

    private static List ut(Class cls, Class cls2, Object obj) {
        int i = 0;
        if (cls2 == null) {
            cls2 = String.class;
        }
        if (obj instanceof JSONArray) {
            List arrayList;
            if (cls == List.class) {
                arrayList = new ArrayList();
            } else if (List.class.isAssignableFrom(cls)) {
                try {
                    arrayList = (List) cls.newInstance();
                } catch (InstantiationException e) {
                    throw uv("Fail to initiate %s", cls);
                } catch (IllegalAccessException e2) {
                    throw uv("Fail to initiate %s", cls);
                }
            } else {
                throw uv("%s is not List type", cls);
            }
            JSONArray jSONArray = (JSONArray) obj;
            while (i < jSONArray.length()) {
                Object vg = vg(cls2, null, jSONArray.get(i));
                if (vg != null) {
                    if (cls2.isAssignableFrom(vg.getClass())) {
                        arrayList.add(vg);
                    } else {
                        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "listFromJson error, memberClass:" + cls2 + ", valueClass:" + vg.getClass());
                    }
                }
                i++;
            }
            return arrayList;
        }
        throw uv("jsonobject is not JSONArray", new Object[0]);
    }

    private static <T> T vb(JSONArray jSONArray, Class<T> cls, Class[] clsArr) {
        Class cls2 = null;
        if (List.class.isAssignableFrom(cls)) {
            if (clsArr != null && clsArr.length > 0) {
                cls2 = clsArr[0];
            }
            return ut(cls, cls2, jSONArray);
        }
        throw uv("Obj class (%s) is not List type", cls);
    }

    private static JSONException uw(boolean z, String str, Object... objArr) {
        String format = String.format(Locale.ENGLISH, str, objArr);
        if (z) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", format);
        }
        return new JSONException(format);
    }

    private static JSONException uv(String str, Object... objArr) {
        return uw(true, str, objArr);
    }

    private static String un(Field field) {
        com.huawei.android.pushagent.datatype.b.a aVar = (com.huawei.android.pushagent.datatype.b.a) field.getAnnotation(com.huawei.android.pushagent.datatype.b.a.class);
        if (aVar != null && (TextUtils.isEmpty(aVar.az()) ^ 1) != 0) {
            return aVar.az();
        }
        String name = field.getName();
        if (name.endsWith("__")) {
            return name.substring(0, name.length() - "__".length());
        }
        return name;
    }

    private static boolean ur(Field field) {
        boolean z = false;
        if (field == null) {
            return false;
        }
        String name = field.getName();
        if (!(Modifier.isStatic(field.getModifiers()) || name == null || (name.contains("$") ^ 1) == 0)) {
            z = field.isAnnotationPresent(c.class) ^ 1;
        }
        return z;
    }

    public static Map<String, Object> us(String str) {
        HashMap hashMap = new HashMap();
        if (TextUtils.isEmpty(str)) {
            return hashMap;
        }
        try {
            JSONObject jSONObject = new JSONObject(str);
            Iterator keys = jSONObject.keys();
            while (keys.hasNext()) {
                String valueOf = String.valueOf(keys.next());
                hashMap.put(valueOf, jSONObject.get(valueOf));
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
        }
        return hashMap;
    }
}
