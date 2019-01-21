package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.b;

public class k {
    private static final byte[] di = new byte[0];
    private static k dj;
    private final b dk;

    private k(Context context) {
        this.dk = new b(context, "push_disagreement");
    }

    public static k ls(Context context) {
        return lv(context);
    }

    private static k lv(Context context) {
        k kVar;
        synchronized (di) {
            if (dj == null) {
                dj = new k(context);
            }
            kVar = dj;
        }
        return kVar;
    }

    public boolean lt(String str) {
        return this.dk.tk(str, false);
    }

    public boolean lu(String str, boolean z) {
        return this.dk.th(str, Boolean.valueOf(z));
    }
}
