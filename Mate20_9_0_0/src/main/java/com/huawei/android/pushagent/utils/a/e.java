package com.huawei.android.pushagent.utils.a;

import com.huawei.android.pushagent.utils.b.a;
import java.lang.reflect.InvocationTargetException;

public abstract class e implements g {
    public String getDeviceId() {
        try {
            Class cls = Class.forName("com.huawei.android.os.BuildEx");
            Object invoke = cls.getDeclaredMethod("getUDID", new Class[0]).invoke(cls, new Object[0]);
            if (invoke == null || !(invoke instanceof String)) {
                a.su("PushLog3414", "udid is null");
                return null;
            }
            a.sv("PushLog3414", "get udid successful.");
            return (String) invoke;
        } catch (ClassNotFoundException e) {
            a.su("PushLog3414", "not support udid class");
        } catch (NoSuchMethodException e2) {
            a.su("PushLog3414", "not support udid method");
        } catch (SecurityException e3) {
            a.su("PushLog3414", "not support udid method");
        } catch (IllegalAccessException e4) {
            a.su("PushLog3414", "not support udid invoke");
        } catch (IllegalArgumentException e5) {
            a.su("PushLog3414", "not support udid invoke");
        } catch (InvocationTargetException e6) {
            a.su("PushLog3414", "not support udid invoke");
        }
    }
}
