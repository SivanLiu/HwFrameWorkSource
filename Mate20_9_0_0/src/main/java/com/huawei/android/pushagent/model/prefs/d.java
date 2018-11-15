package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.a;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class d {
    private static final byte[] fh = new byte[0];
    private static d fi;
    private final a fj;

    private d(Context context) {
        this.fj = new a(context, "push_notify_switch");
    }

    public static d ph(Context context) {
        return pm(context);
    }

    private static d pm(Context context) {
        d dVar;
        synchronized (fh) {
            if (fi == null) {
                fi = new d(context);
            }
            dVar = fi;
        }
        return dVar;
    }

    public boolean pl(String str) {
        return this.fj.eb(str, false);
    }

    public boolean pj(String str, boolean z) {
        return this.fj.ea(str, Boolean.valueOf(z));
    }

    public boolean pi(String str) {
        return this.fj.ed(str);
    }

    public Set<Entry<String, Boolean>> pk() {
        Map all = this.fj.getAll();
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
