package com.huawei.android.pushagent.utils.b;

import com.huawei.android.pushagent.utils.a.b;
import java.lang.reflect.InvocationTargetException;

public abstract class f implements d {
    public String getDeviceId() {
        try {
            Class cls = Class.forName("com.huawei.android.os.BuildEx");
            Object invoke = cls.getDeclaredMethod("getUDID", new Class[0]).invoke(cls, new Object[0]);
            if (invoke == null || !(invoke instanceof String)) {
                b.y("PushLog2976", "udid is null");
                return null;
            }
            b.z("PushLog2976", "get udid successful.");
            return (String) invoke;
        } catch (ClassNotFoundException e) {
            b.y("PushLog2976", "not support udid class");
        } catch (NoSuchMethodException e2) {
            b.y("PushLog2976", "not support udid method");
        } catch (SecurityException e3) {
            b.y("PushLog2976", "not support udid method");
        } catch (IllegalAccessException e4) {
            b.y("PushLog2976", "not support udid invoke");
        } catch (IllegalArgumentException e5) {
            b.y("PushLog2976", "not support udid invoke");
        } catch (InvocationTargetException e6) {
            b.y("PushLog2976", "not support udid invoke");
        }
    }
}
