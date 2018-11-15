package com.huawei.android.pushagent.utils.c;

import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.b.b;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.f.c;
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

public abstract class a {
    private static final Class[] u = new Class[]{String.class, Object.class, Integer.class, Short.class, Long.class, Byte.class, Float.class, Double.class, Boolean.class};
    private static final Class[] v = new Class[]{String.class, Object.class, Integer.class, Short.class, Long.class, Byte.class, Float.class, Double.class, Character.class, Boolean.class};
    private static final Map<Class, b> w = new HashMap();

    static {
        c cVar = new c();
        w.put(Integer.TYPE, cVar);
        w.put(Integer.class, cVar);
        d dVar = new d();
        w.put(Long.TYPE, dVar);
        w.put(Long.class, dVar);
        e eVar = new e();
        w.put(Float.TYPE, eVar);
        w.put(Float.class, eVar);
        f fVar = new f();
        w.put(Double.TYPE, fVar);
        w.put(Double.class, fVar);
        g gVar = new g();
        w.put(Short.TYPE, gVar);
        w.put(Short.class, gVar);
        h hVar = new h();
        w.put(Byte.TYPE, hVar);
        w.put(Byte.class, hVar);
        i iVar = new i();
        w.put(Boolean.TYPE, iVar);
        w.put(Boolean.class, iVar);
    }

    public static <T> T bq(String str, Class<T> cls, Class... clsArr) {
        if (TextUtils.isEmpty(str)) {
            throw bg(false, "Input json string cannot be empty!", new Object[0]);
        }
        au(cls);
        return av(str, cls, clsArr);
    }

    private static <T> T av(String str, Class<T> cls, Class[] clsArr) {
        try {
            return bm(new JSONObject(str), cls, clsArr);
        } catch (JSONException e) {
            try {
                return bl(new JSONArray(str), cls, clsArr);
            } catch (JSONException e2) {
                throw bf("Input string is not valid json string!", new Object[0]);
            }
        }
    }

    public static <T> T br(String str, Class<T> cls, Class... clsArr) {
        try {
            return bq(str, cls, clsArr);
        } catch (JSONException e) {
            c.eo("PushLog3413", "toObject JSONException");
            return null;
        } catch (Exception e2) {
            c.eo("PushLog3413", "toObject error");
            return null;
        }
    }

    public static String bo(Object obj) {
        try {
            return bp(obj, false);
        } catch (JSONException e) {
            throw e;
        } catch (IllegalAccessException e2) {
            throw bf("toJson error", new Object[0]);
        }
    }

    private static String bp(Object obj, boolean z) {
        if (obj == null) {
            return "";
        }
        au(obj.getClass());
        if (obj instanceof List) {
            return be((List) obj, z);
        }
        if (obj instanceof Map) {
            return bi((Map) obj, z);
        }
        if (obj instanceof JSONObject) {
            return obj.toString();
        }
        return bj(obj, z);
    }

    private static String bj(Object obj, boolean z) {
        Field[] cb = j.cb(obj.getClass());
        if (cb.length <= 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        int length = cb.length;
        for (int i = 0; i < length; i++) {
            cb[i] = j.cc(cb[i], true);
            if (bb(cb[i])) {
                String ax = ax(cb[i]);
                Object obj2 = cb[i].get(obj);
                String bt = (z && cb[i].isAnnotationPresent(b.class)) ? obj2 != null ? "\"******\"" : null : bt(obj2, z);
                if (bt != null) {
                    stringBuilder.append('\"').append(ax).append("\":").append(bt);
                    if (i < length - 1) {
                        stringBuilder.append(',');
                    }
                }
            }
        }
        aw(stringBuilder);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static String bt(Object obj, boolean z) {
        if (obj == null) {
            return null;
        }
        String str;
        if ((obj instanceof String) || (obj instanceof Character)) {
            str = "\"" + f.fo(obj.toString()) + "\"";
        } else if ((obj instanceof Integer) || (obj instanceof Long) || (obj instanceof Boolean) || (obj instanceof Float) || (obj instanceof Byte) || (obj instanceof Double) || (obj instanceof Short)) {
            str = obj.toString();
        } else if (obj instanceof List) {
            str = be((List) obj, z);
        } else if (obj instanceof Map) {
            str = bi((Map) obj, z);
        } else if (obj.getClass().isArray()) {
            str = at(obj, z);
        } else {
            str = bp(obj, z);
        }
        return str;
    }

    private static String be(List list, boolean z) {
        if (list.size() <= 0) {
            return "[]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < list.size(); i++) {
            String bt = bt(list.get(i), z);
            if (bt != null) {
                stringBuilder.append(bt).append(',');
            }
        }
        aw(stringBuilder);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private static String at(Object obj, boolean z) {
        int length = Array.getLength(obj);
        if (length <= 0) {
            return "[]";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        for (int i = 0; i < length; i++) {
            String bt = bt(Array.get(obj, i), z);
            if (bt != null) {
                stringBuilder.append(bt).append(',');
            }
        }
        aw(stringBuilder);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    private static String bi(Map map, boolean z) {
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
            String bt = bt(entry.getValue(), z);
            if (bt != null) {
                stringBuilder.append('\"').append(str).append("\":");
                stringBuilder.append(bt);
            }
            if (i2 < size && bt != null) {
                stringBuilder.append(',');
            }
            i = i2;
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    private static void aw(StringBuilder stringBuilder) {
        int length = stringBuilder.length();
        if (length > 0 && stringBuilder.charAt(length - 1) == ',') {
            stringBuilder.delete(length - 1, length);
        }
    }

    private static void au(Class cls) {
        if (cls.isPrimitive()) {
            throw bf("Root obj class (%s) cannot be primitive type!", cls);
        }
        for (Class cls2 : v) {
            if (cls == cls2) {
                throw bf("Root obj class (%s) is invalid in conversion", cls);
            }
        }
    }

    private static <T> T bm(JSONObject jSONObject, Class<T> cls, Class[] clsArr) {
        Class cls2 = null;
        if (Collection.class.isAssignableFrom(cls)) {
            throw bf("Obj class %s is Collection type which mismatches with JsonObject", cls);
        } else if (cls.isArray()) {
            throw bf("Obj class %s is array type which mismatches with JsonObject", cls);
        } else if (Map.class.isAssignableFrom(cls)) {
            if (clsArr != null && clsArr.length > 0) {
                cls2 = clsArr[0];
            }
            return bh(cls, cls2, jSONObject);
        } else {
            try {
                return ay(jSONObject, cls.getConstructor(new Class[0]).newInstance(new Object[0]));
            } catch (NoSuchMethodException e) {
                throw bf("No default constructor for class %s", cls);
            } catch (IllegalAccessException e2) {
                throw bf("New instance failed for %s", cls);
            } catch (InstantiationException e3) {
                throw bf("New instance failed for %s", cls);
            } catch (InvocationTargetException e4) {
                throw bf("New instance failed for %s", cls);
            }
        }
    }

    private static <T> T ay(JSONObject jSONObject, T t) {
        Field[] cb = j.cb(t.getClass());
        for (Field cc : cb) {
            Field cc2 = j.cc(cc2, true);
            if (bb(cc2)) {
                Object opt = jSONObject.opt(ax(cc2));
                if (!(opt == null || JSONObject.NULL == opt)) {
                    az(t, cc2, opt);
                }
            }
        }
        return t;
    }

    private static void az(Object obj, Field field, Object obj2) {
        Object obj3 = null;
        try {
            obj3 = bs(field.getType(), j.cd(field), obj2);
            field.set(obj, obj3);
        } catch (RuntimeException e) {
            c.eo("PushLog3413", obj.getClass().getName() + ".fromJson runtime exception, fieldName: " + field.getName() + ", field: " + field);
        } catch (Exception e2) {
            c.eo("PushLog3413", obj.getClass().getName() + ".fromJson error, fieldName: " + field.getName() + ", field:" + field);
            bk(obj, field, obj3);
        }
    }

    private static void bk(Object obj, Field field, Object obj2) {
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
                c.eq("PushLog3413", "processValueError");
            }
        }
    }

    private static boolean ba(Class cls) {
        if (cls.isPrimitive()) {
            return true;
        }
        for (Class cls2 : u) {
            if (cls == cls2) {
                return true;
            }
        }
        return false;
    }

    private static Object bs(Class cls, Class cls2, Object obj) {
        if (ba(cls)) {
            return bn(cls, obj);
        }
        if (List.class.isAssignableFrom(cls)) {
            return bd(cls, cls2, obj);
        }
        if (Map.class.isAssignableFrom(cls)) {
            return bh(cls, cls2, obj);
        }
        if (obj instanceof JSONObject) {
            return bm((JSONObject) obj, cls, new Class[]{cls2});
        } else if (obj instanceof JSONArray) {
            return bl((JSONArray) obj, cls, new Class[]{cls2});
        } else {
            throw bf("value from json error, field class: %s", cls);
        }
    }

    private static Object bn(Class cls, Object obj) {
        if (String.class == cls) {
            return f.fp(obj);
        }
        b bVar;
        if ((cls.isPrimitive() || Number.class.isAssignableFrom(cls)) && (obj instanceof Number)) {
            Number number = (Number) obj;
            bVar = (b) w.get(cls);
            if (bVar != null) {
                return bVar.bu(number);
            }
            c.eo("PushLog3413", "cannot find value reader for:" + cls);
            return null;
        } else if (cls != Boolean.class) {
            return obj;
        } else {
            bVar = (b) w.get(cls);
            if (bVar != null) {
                return bVar.bu(obj);
            }
            c.eo("PushLog3413", "cannot find value reader for:" + cls);
            return null;
        }
    }

    private static Map bh(Class cls, Class cls2, Object obj) {
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
                    throw bf("Fail to initiate %s", cls);
                } catch (IllegalAccessException e2) {
                    throw bf("Fail to initiate %s", cls);
                }
            } else {
                throw bf("%s is not Map type", cls);
            }
            JSONObject jSONObject = (JSONObject) obj;
            Iterator keys = jSONObject.keys();
            while (keys.hasNext()) {
                String str = (String) keys.next();
                Object bs = bs(cls2, null, jSONObject.get(str));
                if (bs != null) {
                    if (cls2.isAssignableFrom(bs.getClass())) {
                        linkedHashMap.put(str, bs);
                    } else {
                        c.eq("PushLog3413", "mapFromJson error, memberClass:" + cls2 + ", valueClass:" + bs.getClass());
                    }
                }
            }
            return linkedHashMap;
        }
        throw bf("jsonValue is not JSONObject", new Object[0]);
    }

    private static List bd(Class cls, Class cls2, Object obj) {
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
                    throw bf("Fail to initiate %s", cls);
                } catch (IllegalAccessException e2) {
                    throw bf("Fail to initiate %s", cls);
                }
            } else {
                throw bf("%s is not List type", cls);
            }
            JSONArray jSONArray = (JSONArray) obj;
            while (i < jSONArray.length()) {
                Object bs = bs(cls2, null, jSONArray.get(i));
                if (bs != null) {
                    if (cls2.isAssignableFrom(bs.getClass())) {
                        arrayList.add(bs);
                    } else {
                        c.eq("PushLog3413", "listFromJson error, memberClass:" + cls2 + ", valueClass:" + bs.getClass());
                    }
                }
                i++;
            }
            return arrayList;
        }
        throw bf("jsonobject is not JSONArray", new Object[0]);
    }

    private static <T> T bl(JSONArray jSONArray, Class<T> cls, Class[] clsArr) {
        Class cls2 = null;
        if (List.class.isAssignableFrom(cls)) {
            if (clsArr != null && clsArr.length > 0) {
                cls2 = clsArr[0];
            }
            return bd(cls, cls2, jSONArray);
        }
        throw bf("Obj class (%s) is not List type", cls);
    }

    private static JSONException bg(boolean z, String str, Object... objArr) {
        String format = String.format(Locale.ENGLISH, str, objArr);
        if (z) {
            c.eo("PushLog3413", format);
        }
        return new JSONException(format);
    }

    private static JSONException bf(String str, Object... objArr) {
        return bg(true, str, objArr);
    }

    private static String ax(Field field) {
        com.huawei.android.pushagent.datatype.b.a aVar = (com.huawei.android.pushagent.datatype.b.a) field.getAnnotation(com.huawei.android.pushagent.datatype.b.a.class);
        if (aVar != null && (TextUtils.isEmpty(aVar.kr()) ^ 1) != 0) {
            return aVar.kr();
        }
        String name = field.getName();
        if (name.endsWith("__")) {
            return name.substring(0, name.length() - "__".length());
        }
        return name;
    }

    private static boolean bb(Field field) {
        boolean z = false;
        if (field == null) {
            return false;
        }
        String name = field.getName();
        if (!(Modifier.isStatic(field.getModifiers()) || name == null || (name.contains("$") ^ 1) == 0)) {
            z = field.isAnnotationPresent(com.huawei.android.pushagent.datatype.b.c.class) ^ 1;
        }
        return z;
    }

    public static Map<String, Object> bc(String str) {
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
            c.es("PushLog3413", e.toString(), e);
        }
        return hashMap;
    }
}
