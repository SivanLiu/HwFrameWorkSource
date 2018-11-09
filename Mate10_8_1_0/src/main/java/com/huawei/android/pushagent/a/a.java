package com.huawei.android.pushagent.a;

import android.content.Context;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.b;

public class a {
    public static synchronized void xi(Context context) {
        synchronized (a.class) {
            int pv = k.pt(context).pv();
            if (4 == pv) {
                return;
            }
            if (pv < 3) {
                new b(context).xj();
            }
            b.z("PushLog2976", "update xml data, old version is " + pv + ",new version is " + 4);
            if (pv < 4) {
                new c(context).xj();
            }
            k.pt(context).pw(4);
        }
    }
}
