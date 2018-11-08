package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class c {
    private static final byte[] cr = new byte[0];
    private static c cs;
    private final com.huawei.android.pushagent.utils.a.c ct;

    private c(Context context) {
        this.ct = new com.huawei.android.pushagent.utils.a.c(context, "push_notify_switch");
    }

    public static c kw(Context context) {
        return kz(context);
    }

    private static c kz(Context context) {
        c cVar;
        synchronized (cr) {
            if (cs == null) {
                cs = new c(context);
            }
            cVar = cs;
        }
        return cVar;
    }

    public boolean kx(String str) {
        return this.ct.ap(str, false);
    }

    public boolean lb(String str, boolean z) {
        return this.ct.am(str, Boolean.valueOf(z));
    }

    public boolean la(String str) {
        return this.ct.an(str);
    }

    public Set<Entry<String, Boolean>> ky() {
        Map all = this.ct.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set<Entry<String, Boolean>> entrySet = all.entrySet();
        if (entrySet == null) {
            return new HashSet();
        }
        return entrySet;
    }
}
