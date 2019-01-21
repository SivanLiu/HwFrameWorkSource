package com.huawei.android.pushagent.utils.f;

import android.content.Context;
import android.os.Build;
import com.huawei.android.pushagent.utils.b.b;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;

public class a {
    private static void xv(Context context, int i, HashMap<Short, Object> hashMap) {
        if (context == null || hashMap == null) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "startMonitor, map is null");
            return;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "startMonitor, eventId is " + i);
        String tg = new b(context, "PushMonitor").tg(String.valueOf(i));
        com.huawei.android.pushagent.model.flowcontrol.a.b bVar = new com.huawei.android.pushagent.model.flowcontrol.a.b(86400000, 2);
        bVar.nh(tg);
        if (bVar.ne(1)) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "begin to startMonitor");
            new Thread(new b(context, bVar, i, hashMap)).start();
            return;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "can't report too many times");
    }

    public static void xt(Context context) {
        HashMap hashMap = new HashMap();
        hashMap.put(Short.valueOf((short) 0), String.valueOf(3414));
        hashMap.put(Short.valueOf((short) 1), Build.MODEL);
        xv(context, 907124001, hashMap);
    }

    public static void xs(Context context, String str) {
        HashMap hashMap = new HashMap();
        hashMap.put(Short.valueOf((short) 0), String.valueOf(3414));
        hashMap.put(Short.valueOf((short) 1), str);
        xv(context, 907124002, hashMap);
    }

    private static void xu(Context context, com.huawei.android.pushagent.model.flowcontrol.a.b bVar, int i, HashMap<Short, Object> hashMap) {
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
                bVar.nd(1);
                new b(context, "PushMonitor").tm(String.valueOf(i), bVar.ng());
            }
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", " ClassNotFoundException startMonitor " + e.toString());
        } catch (NoSuchMethodException e2) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", " NoSuchMethodException startMonitor " + e2.toString());
        } catch (IllegalArgumentException e3) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", " IllegalArgumentException startMonitor " + e3.toString());
        } catch (IllegalAccessException e4) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", " IllegalAccessException startMonitor " + e4.toString());
        } catch (Exception e5) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", " Exception startMonitor " + e5.toString());
        }
    }
}
