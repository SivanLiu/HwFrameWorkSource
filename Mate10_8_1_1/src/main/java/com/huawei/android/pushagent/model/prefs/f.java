package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.a.c;

public class f {
    private static final byte[] dd = new byte[0];
    private static f de;
    private final c df;

    private f(Context context) {
        this.df = new c(context, "push_disagreement");
    }

    public static f lu(Context context) {
        return lv(context);
    }

    private static f lv(Context context) {
        f fVar;
        synchronized (dd) {
            if (de == null) {
                de = new f(context);
            }
            fVar = de;
        }
        return fVar;
    }

    public boolean lt(String str) {
        return this.df.ap(str, false);
    }

    public boolean lw(String str, boolean z) {
        return this.df.am(str, Boolean.valueOf(z));
    }
}
