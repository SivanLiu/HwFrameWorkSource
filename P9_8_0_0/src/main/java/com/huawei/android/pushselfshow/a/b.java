package com.huawei.android.pushselfshow.a;

import com.huawei.android.pushagent.a.a.c;

class b implements Runnable {
    b() {
    }

    public void run() {
        int -l_1_I = 0;
        if (a.e.containsKey(a.c)) {
            -l_1_I = ((Integer) a.e.get(a.c)).intValue();
        }
        c.b("PushSelfShowLog", "runnable run, cached num is " + -l_1_I);
        a.b(a.c);
        a.e(a.b, a.c, a.d, a.d(a.b, a.c, a.d, 0) + -l_1_I);
    }
}
