package com.huawei.android.pushagent.utils.f;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.a.a;
import com.huawei.android.pushagent.datatype.a.c;
import com.huawei.android.pushagent.utils.g;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class b {
    private static final Class[] bb = new Class[]{String.class, Object.class, Integer.class, Short.class, Long.class, Byte.class, Float.class, Double.class, Boolean.class};
    private static final Class[] bc = new Class[]{String.class, Object.class, Integer.class, Short.class, Long.class, Byte.class, Float.class, Double.class, Character.class, Boolean.class};
    private static final Map<Class, c> bd = new HashMap();

    static {
        d dVar = new d();
        bd.put(Integer.TYPE, dVar);
        bd.put(Integer.class, dVar);
        e eVar = new e();
        bd.put(Long.TYPE, eVar);
        bd.put(Long.class, eVar);
        f fVar = new f();
        bd.put(Float.TYPE, fVar);
        bd.put(Float.class, fVar);
        g gVar = new g();
        bd.put(Double.TYPE, gVar);
        bd.put(Double.class, gVar);
        h hVar = new h();
        bd.put(Short.TYPE, hVar);
        bd.put(Short.class, hVar);
        i iVar = new i();
        bd.put(Byte.TYPE, iVar);
        bd.put(Byte.class, iVar);
        j jVar = new j();
        bd.put(Boolean.TYPE, jVar);
        bd.put(Boolean.class, jVar);
    }

    public static <T> T er(String str, Class<T> cls, Class... clsArr) {
        if (TextUtils.isEmpty(str)) {
            throw ei(false, "Input json string cannot be empty!", new Object[0]);
        }
        dx(cls);
        return dy(str, cls, clsArr);
    }

    private static <T> T dy(String str, Class<T> cls, Class[] clsArr) {
        try {
            return eo(new JSONObject(str), cls, clsArr);
        } catch (JSONException e) {
            try {
                return en(new JSONArray(str), cls, clsArr);
            } catch (JSONException e2) {
                throw eh("Input string is not valid json string!", new Object[0]);
            }
        }
    }

    public static <T> T du(String str, Class<T> cls, Class... clsArr) {
        try {
            return er(str, cls, clsArr);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "toObject JSONException");
            return null;
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "toObject error");
            return null;
        }
    }

    public static String dt(Object obj) {
        try {
            return eq(obj, false);
        } catch (JSONException e) {
            throw e;
        } catch (IllegalAccessException e2) {
            throw eh("toJson error", new Object[0]);
        }
    }

    private static String eq(Object obj, boolean z) {
        if (obj == null) {
            return "";
        }
        dx(obj.getClass());
        if (obj instanceof List) {
            return eg((List) obj, z);
        }
        if (obj instanceof Map) {
            return ek((Map) obj, z);
        }
        if (obj instanceof JSONObject) {
            return obj.toString();
        }
        return el(obj, z);
    }

    private static String el(Object obj, boolean z) {
        Field[] dp = a.dp(obj.getClass());
        if (dp.length <= 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        int length = dp.length;
        for (int i = 0; i < length; i++) {
            dp[i] = a.ds(dp[i], true);
            if (ee(dp[i])) {
                String ea = ea(dp[i]);
                Object obj2 = dp[i].get(obj);
                String et = (z && dp[i].isAnnotationPresent(a.class)) ? obj2 != null ? "\"******\"" : null : et(obj2, z);
                if (et != null) {
                    stringBuilder.append('\"').append(ea).append("\":").append(et);
                    if (i < length - 1) {
                        stringBuilder.append(',');
                    }
                }
            }
        }
        dz(stringBuilder);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static String et(Object obj, boolean z) {
        if (obj == null) {
            return null;
        }
        String str;
        if ((obj instanceof String) || (obj instanceof Character)) {
            str = "\"" + g.hb(obj.toString()) + "\"";
        } else if ((obj instanceof Integer) || (obj instanceof Long) || (obj instanceof Boolean) || (obj instanceof Float) || (obj instanceof Byte) || (obj instanceof Double) || (obj instanceof Short)) {
            str = obj.toString();
        } else if (obj instanceof List) {
            str = eg((List) obj, z);
        } else if (obj instanceof Map) {
            str = ek((Map) obj, z);
        } else if (obj.getClass().isArray()) {
            str = dw(obj, z);
        } else {
            str = eq(obj, z);
        }
        return str;
    }

    private static String eg(List list, boolean z) {
        if (list.size() <= 0) {
            return "[]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < list.size(); i++) {
            String et = et(list.get(i), z);
            if (et != null) {
                stringBuilder.append(et).append(',');
            }
        }
        dz(stringBuilder);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private static String dw(Object obj, boolean z) {
        int length = Array.getLength(obj);
        if (length <= 0) {
            return "[]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < length; i++) {
            String et = et(Array.get(obj, i), z);
            if (et != null) {
                stringBuilder.append(et).append(',');
            }
        }
        dz(stringBuilder);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private static String ek(Map map, boolean z) {
        if (map.size() <= 0) {
            return "{}";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        Iterable<Entry> entrySet = map.entrySet();
        int size = entrySet.size();
        int i = 0;
        for (Entry entry : entrySet) {
            int i2 = i + 1;
            String str = (String) entry.getKey();
            String et = et(entry.getValue(), z);
            if (et != null) {
                stringBuilder.append('\"').append(str).append("\":");
                stringBuilder.append(et);
            }
            if (i2 < size && et != null) {
                stringBuilder.append(',');
            }
            i = i2;
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static void dz(StringBuilder stringBuilder) {
        int length = stringBuilder.length();
        if (length > 0 && stringBuilder.charAt(length - 1) == ',') {
            stringBuilder.delete(length - 1, length);
        }
    }

    private static void dx(Class cls) {
        if (cls.isPrimitive()) {
            throw eh("Root obj class (%s) cannot be primitive type!", cls);
        }
        for (Class cls2 : bc) {
            if (cls == cls2) {
                throw eh("Root obj class (%s) is invalid in conversion", cls);
            }
        }
    }

    private static <T> T eo(JSONObject jSONObject, Class<T> cls, Class[] clsArr) {
        Class cls2 = null;
        if (Collection.class.isAssignableFrom(cls)) {
            throw eh("Obj class %s is Collection type which mismatches with JsonObject", cls);
        } else if (cls.isArray()) {
            throw eh("Obj class %s is array type which mismatches with JsonObject", cls);
        } else if (Map.class.isAssignableFrom(cls)) {
            if (clsArr != null && clsArr.length > 0) {
                cls2 = clsArr[0];
            }
            return ej(cls, cls2, jSONObject);
        } else {
            try {
                return eb(jSONObject, cls.getConstructor(new Class[0]).newInstance(new Object[0]));
            } catch (NoSuchMethodException e) {
                throw eh("No default constructor for class %s", cls);
            } catch (IllegalAccessException e2) {
                throw eh("New instance failed for %s", cls);
            } catch (InstantiationException e3) {
                throw eh("New instance failed for %s", cls);
            } catch (InvocationTargetException e4) {
                throw eh("New instance failed for %s", cls);
            }
        }
    }

    private static <T> T eb(JSONObject jSONObject, T t) {
        Field[] dp = a.dp(t.getClass());
        for (Field ds : dp) {
            Field ds2 = a.ds(ds2, true);
            if (ee(ds2)) {
                Object opt = jSONObject.opt(ea(ds2));
                if (!(opt == null || JSONObject.NULL == opt)) {
                    ec(t, ds2, opt);
                }
            }
        }
        return t;
    }

    private static void ec(Object obj, Field field, Object obj2) {
        Object obj3 = null;
        try {
            obj3 = es(field.getType(), a.dq(field), obj2);
            field.set(obj, obj3);
        } catch (RuntimeException e) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", obj.getClass().getName() + ".fromJson runtime exception, fieldName: " + field.getName() + ", field: " + field);
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", obj.getClass().getName() + ".fromJson error, fieldName: " + field.getName() + ", field:" + field);
            em(obj, field, obj3);
        }
    }

    private static void em(Object obj, Field field, Object obj2) {
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
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "processValueError");
            }
        }
    }

    private static boolean ed(Class cls) {
        if (cls.isPrimitive()) {
            return true;
        }
        for (Class cls2 : bb) {
            if (cls == cls2) {
                return true;
            }
        }
        return false;
    }

    private static Object es(Class cls, Class cls2, Object obj) {
        if (ed(cls)) {
            return ep(cls, obj);
        }
        if (List.class.isAssignableFrom(cls)) {
            return ef(cls, cls2, obj);
        }
        if (Map.class.isAssignableFrom(cls)) {
            return ej(cls, cls2, obj);
        }
        if (obj instanceof JSONObject) {
            return eo((JSONObject) obj, cls, new Class[]{cls2});
        } else if (obj instanceof JSONArray) {
            return en((JSONArray) obj, cls, new Class[]{cls2});
        } else {
            throw eh("value from json error, field class: %s", cls);
        }
    }

    private static Object ep(Class cls, Object obj) {
        if (String.class == cls) {
            return g.hc(obj);
        }
        c cVar;
        if ((cls.isPrimitive() || Number.class.isAssignableFrom(cls)) && (obj instanceof Number)) {
            Number number = (Number) obj;
            cVar = (c) bd.get(cls);
            if (cVar != null) {
                return cVar.eu(number);
            }
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "cannot find value reader for:" + cls);
            return null;
        } else if (cls != Boolean.class) {
            return obj;
        } else {
            cVar = (c) bd.get(cls);
            if (cVar != null) {
                return cVar.eu(obj);
            }
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "cannot find value reader for:" + cls);
            return null;
        }
    }

    private static Map ej(Class cls, Class cls2, Object obj) {
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
                    throw eh("Fail to initiate %s", cls);
                } catch (IllegalAccessException e2) {
                    throw eh("Fail to initiate %s", cls);
                }
            } else {
                throw eh("%s is not Map type", cls);
            }
            JSONObject jSONObject = (JSONObject) obj;
            Iterator keys = jSONObject.keys();
            while (keys.hasNext()) {
                String str = (String) keys.next();
                Object es = es(cls2, null, jSONObject.get(str));
                if (es != null) {
                    if (cls2.isAssignableFrom(es.getClass())) {
                        linkedHashMap.put(str, es);
                    } else {
                        com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "mapFromJson error, memberClass:" + cls2 + ", valueClass:" + es.getClass());
                    }
                }
            }
            return linkedHashMap;
        }
        throw eh("jsonValue is not JSONObject", new Object[0]);
    }

    private static List ef(Class cls, Class cls2, Object obj) {
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
                    throw eh("Fail to initiate %s", cls);
                } catch (IllegalAccessException e2) {
                    throw eh("Fail to initiate %s", cls);
                }
            } else {
                throw eh("%s is not List type", cls);
            }
            JSONArray jSONArray = (JSONArray) obj;
            while (i < jSONArray.length()) {
                Object es = es(cls2, null, jSONArray.get(i));
                if (es != null) {
                    if (cls2.isAssignableFrom(es.getClass())) {
                        arrayList.add(es);
                    } else {
                        com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "listFromJson error, memberClass:" + cls2 + ", valueClass:" + es.getClass());
                    }
                }
                i++;
            }
            return arrayList;
        }
        throw eh("jsonobject is not JSONArray", new Object[0]);
    }

    private static <T> T en(JSONArray jSONArray, Class<T> cls, Class[] clsArr) {
        Class cls2 = null;
        if (List.class.isAssignableFrom(cls)) {
            if (clsArr != null && clsArr.length > 0) {
                cls2 = clsArr[0];
            }
            return ef(cls, cls2, jSONArray);
        }
        throw eh("Obj class (%s) is not List type", cls);
    }

    private static JSONException ei(boolean z, String str, Object... objArr) {
        String format = String.format(Locale.ENGLISH, str, objArr);
        if (z) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", format);
        }
        return new JSONException(format);
    }

    private static JSONException eh(String str, Object... objArr) {
        return ei(true, str, objArr);
    }

    private static String ea(Field field) {
        com.huawei.android.pushagent.datatype.a.b bVar = (com.huawei.android.pushagent.datatype.a.b) field.getAnnotation(com.huawei.android.pushagent.datatype.a.b.class);
        if (bVar != null && (TextUtils.isEmpty(bVar.xw()) ^ 1) != 0) {
            return bVar.xw();
        }
        String name = field.getName();
        if (name.endsWith("__")) {
            return name.substring(0, name.length() - "__".length());
        }
        return name;
    }

    private static boolean ee(Field field) {
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

    public static Map<String, Object> dv(String str) {
        Map<String, Object> hashMap = new HashMap();
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
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e.toString(), e);
        }
        return hashMap;
    }
}
