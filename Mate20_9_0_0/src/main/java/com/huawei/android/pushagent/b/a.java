package com.huawei.android.pushagent.b;

import android.content.Context;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.f.c;

public class a {
    public static synchronized void ie(Context context) {
        synchronized (a.class) {
            int um = l.ul(context).um();
            if (4 == um) {
                return;
            }
            if (um < 3) {
                new b(context).m1if();
            }
            c.ep("PushLog3413", "update xml data, old version is " + um + ",new version is " + 4);
            if (um < 4) {
                new c(context).m0if();
            }
            l.ul(context).un(4);
        }
    }
}
