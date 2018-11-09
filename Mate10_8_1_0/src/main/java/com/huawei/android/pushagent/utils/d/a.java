package com.huawei.android.pushagent.utils.d;

import android.content.Context;
import android.os.Build;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.a.c;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;

public class a {
    private static void da(Context context, int i, HashMap<Short, Object> hashMap) {
        if (context == null || hashMap == null) {
            b.z("PushLog2976", "startMonitor, map is null");
            return;
        }
        b.z("PushLog2976", "startMonitor, eventId is " + i);
        String aj = new c(context, "PushMonitor").aj(String.valueOf(i));
        com.huawei.android.pushagent.model.flowcontrol.a.b bVar = new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, 2);
        bVar.hh(aj);
        if (bVar.he(1)) {
            b.z("PushLog2976", "begin to startMonitor");
            new Thread(new b(context, bVar, i, hashMap)).start();
            return;
        }
        b.z("PushLog2976", "can't report too many times");
    }

    public static void cy(Context context) {
        HashMap hashMap = new HashMap();
        hashMap.put(Short.valueOf((short) 0), String.valueOf(2976));
        hashMap.put(Short.valueOf((short) 1), Build.MODEL);
        da(context, 907124001, hashMap);
    }

    public static void cz(Context context, String str) {
        HashMap hashMap = new HashMap();
        hashMap.put(Short.valueOf((short) 0), String.valueOf(2976));
        hashMap.put(Short.valueOf((short) 1), str);
        da(context, 907124002, hashMap);
    }

    private static void cx(Context context, com.huawei.android.pushagent.model.flowcontrol.a.b bVar, int i, HashMap<Short, Object> hashMap) {
        try {
            Class cls = Class.forName("android.util.IMonitor");
            Class cls2 = Class.forName("android.util.IMonitor$EventStream");
            Method declaredMethod = cls.getDeclaredMethod("openEventStream", new Class[]{Integer.TYPE});
            Method declaredMethod2 = cls.getDeclaredMethod("closeEventStream", new Class[]{cls2});
            Method declaredMethod3 = cls.getDeclaredMethod("sendEvent", new Class[]{cls2});
            Object invoke = declaredMethod.invoke(cls, new Object[]{Integer.valueOf(i)});
            if (invoke != null) {
                for (Entry entry : hashMap.entrySet()) {
                    short shortValue = ((Short) entry.getKey()).shortValue();
                    Object value = entry.getValue();
                    cls2.getDeclaredMethod("setParam", new Class[]{Short.TYPE, value.getClass()}).invoke(invoke, new Object[]{Short.valueOf(shortValue), value});
                }
                declaredMethod3.invoke(cls, new Object[]{invoke});
                declaredMethod2.invoke(cls, new Object[]{invoke});
                bVar.hf(1);
                new c(context, "PushMonitor").ak(String.valueOf(i), bVar.hg());
            }
        } catch (ClassNotFoundException e) {
            b.y("PushLog2976", " ClassNotFoundException startMonitor " + e.toString());
        } catch (NoSuchMethodException e2) {
            b.y("PushLog2976", " NoSuchMethodException startMonitor " + e2.toString());
        } catch (IllegalArgumentException e3) {
            b.y("PushLog2976", " IllegalArgumentException startMonitor " + e3.toString());
        } catch (IllegalAccessException e4) {
            b.y("PushLog2976", " IllegalAccessException startMonitor " + e4.toString());
        } catch (Exception e5) {
            b.y("PushLog2976", " Exception startMonitor " + e5.toString());
        }
    }
}
