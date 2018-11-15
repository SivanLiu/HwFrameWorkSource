package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.a;

public class c {
    private static final byte[] fe = new byte[0];
    private static c ff;
    private final a fg;

    private c(Context context) {
        this.fg = new a(context, "PowerGenieControl");
    }

    public static c pc(Context context) {
        return pe(context);
    }

    private static c pe(Context context) {
        c cVar;
        synchronized (fe) {
            if (ff == null) {
                ff = new c(context);
            }
            cVar = ff;
        }
        return cVar;
    }

    public String pd() {
        return this.fg.ec("whiteList");
    }

    public boolean pg(String str) {
        return this.fg.ee("whiteList", str);
    }

    public int pb() {
        return this.fg.getInt("ctrlState", 0);
    }

    public void pf(int i) {
        this.fg.ef("ctrlState", Integer.valueOf(i));
    }
}
