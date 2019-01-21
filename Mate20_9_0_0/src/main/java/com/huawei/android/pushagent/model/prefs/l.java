package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.b;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class l {
    private static final byte[] dl = new byte[0];
    private static l dm;
    private final b dn;

    private l(Context context) {
        this.dn = new b(context, "push_notify_switch");
    }

    public static l lw(Context context) {
        return mb(context);
    }

    private static l mb(Context context) {
        l lVar;
        synchronized (dl) {
            if (dm == null) {
                dm = new l(context);
            }
            lVar = dm;
        }
        return lVar;
    }

    public boolean lz(String str) {
        return this.dn.tk(str, false);
    }

    public boolean ly(String str, boolean z) {
        return this.dn.th(str, Boolean.valueOf(z));
    }

    public boolean lx(String str) {
        return this.dn.ti(str);
    }

    public Set<Entry<String, Boolean>> ma() {
        Map all = this.dn.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set entrySet = all.entrySet();
        if (entrySet == null) {
            return new HashSet();
        }
        return entrySet;
    }
}
