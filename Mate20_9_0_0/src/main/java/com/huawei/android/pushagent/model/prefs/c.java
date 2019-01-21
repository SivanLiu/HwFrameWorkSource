package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.b;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class c {
    private static final byte[] cl = new byte[0];
    private static c cm;
    private final b cn;

    private c(Context context) {
        this.cn = new b(context, "token_request_flag");
    }

    public static c iu(Context context) {
        return iz(context);
    }

    private static c iz(Context context) {
        c cVar;
        synchronized (cl) {
            if (cm == null) {
                cm = new c(context);
            }
            cVar = cm;
        }
        return cVar;
    }

    public boolean iy() {
        try {
            Map all = this.cn.getAll();
            if (all == null) {
                return false;
            }
            Set<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return false;
            }
            for (Entry value : entrySet) {
                if (((Boolean) value.getValue()).booleanValue()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            a.sw("PushLog3414", e.toString(), e);
        }
    }

    public boolean ix(String str) {
        return this.cn.tk(str, false);
    }

    public void iv(String str, boolean z) {
        this.cn.th(str, Boolean.valueOf(z));
    }

    public void iw(String str) {
        this.cn.ti(str);
    }
}
