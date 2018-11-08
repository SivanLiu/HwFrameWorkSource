package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.a.c;
import java.util.Map;
import java.util.Map.Entry;

public class h {
    private static final byte[] dj = new byte[0];
    private static h dk;
    private final c dl;

    private h(Context context) {
        this.dl = new c(context, "token_request_flag");
    }

    public static h md(Context context) {
        return mi(context);
    }

    private static h mi(Context context) {
        h hVar;
        synchronized (dj) {
            if (dk == null) {
                dk = new h(context);
            }
            hVar = dk;
        }
        return hVar;
    }

    public boolean mh() {
        try {
            Map all = this.dl.getAll();
            if (all == null) {
                return false;
            }
            Iterable<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return false;
            }
            for (Entry value : entrySet) {
                if (((Boolean) value.getValue()).booleanValue()) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            b.aa("PushLog2976", e.toString(), e);
        }
    }

    public boolean mg(String str) {
        return this.dl.ap(str, false);
    }

    public void me(String str, boolean z) {
        this.dl.am(str, Boolean.valueOf(z));
    }

    public void mf(String str) {
        this.dl.an(str);
    }
}
