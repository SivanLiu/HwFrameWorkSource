package com.huawei.android.pushagent.a;

import android.content.Context;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.utils.b.a;

public class c {
    public static synchronized void aan(Context context) {
        synchronized (c.class) {
            int jv = e.jj(context).jv();
            if (4 == jv) {
                return;
            }
            if (jv < 3) {
                new a(context).aaa();
            }
            a.sv("PushLog3414", "update xml data, old version is " + jv + ",new version is " + 4);
            if (jv < 4) {
                new b(context).aaa();
            }
            e.jj(context).kd(4);
        }
    }
}
