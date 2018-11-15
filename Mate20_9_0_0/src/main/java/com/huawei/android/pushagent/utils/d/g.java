package com.huawei.android.pushagent.utils.d;

import com.huawei.android.pushagent.utils.f.c;
import java.lang.reflect.InvocationTargetException;

public abstract class g implements b {
    public String getDeviceId() {
        try {
            Class cls = Class.forName("com.huawei.android.os.BuildEx");
            Object invoke = cls.getDeclaredMethod("getUDID", new Class[0]).invoke(cls, new Object[0]);
            if (invoke == null || !(invoke instanceof String)) {
                c.eq("PushLog3413", "udid is null");
                return null;
            }
            c.ep("PushLog3413", "get udid successful.");
            return (String) invoke;
        } catch (ClassNotFoundException e) {
            c.eq("PushLog3413", "not support udid class");
        } catch (NoSuchMethodException e2) {
            c.eq("PushLog3413", "not support udid method");
        } catch (SecurityException e3) {
            c.eq("PushLog3413", "not support udid method");
        } catch (IllegalAccessException e4) {
            c.eq("PushLog3413", "not support udid invoke");
        } catch (IllegalArgumentException e5) {
            c.eq("PushLog3413", "not support udid invoke");
        } catch (InvocationTargetException e6) {
            c.eq("PushLog3413", "not support udid invoke");
        }
    }
}
