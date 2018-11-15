package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.a;

public class i {
    private static final byte[] ga = new byte[0];
    private static i gb;
    private final a gc;

    private i(Context context) {
        this.gc = new a(context, "push_disagreement");
    }

    public static i qx(Context context) {
        return ra(context);
    }

    private static i ra(Context context) {
        i iVar;
        synchronized (ga) {
            if (gb == null) {
                gb = new i(context);
            }
            iVar = gb;
        }
        return iVar;
    }

    public boolean qy(String str) {
        return this.gc.eb(str, false);
    }

    public boolean qz(String str, boolean z) {
        return this.gc.ea(str, Boolean.valueOf(z));
    }
}
