package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.c;
import com.huawei.android.pushagent.utils.f.a;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class e {
    private static final byte[] fk = new byte[0];
    private static e fl;
    private final a fm;

    private e(Context context) {
        this.fm = new a(context, "pclient_info_v2");
    }

    public static e pn(Context context) {
        return pw(context);
    }

    private static e pw(Context context) {
        e eVar;
        synchronized (fk) {
            if (fl == null) {
                fl = new e(context);
            }
            eVar = fl;
        }
        return eVar;
    }

    public boolean pt() {
        return this.fm.eg();
    }

    public String pp(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return this.fm.ec(str);
    }

    public String ps(String str) {
        return c.j(pp(str));
    }

    public boolean px(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.fm.ea(str, str2);
    }

    public boolean po(String str, String str2) {
        return px(str, c.k(str2));
    }

    public void pr(String str) {
        this.fm.ed(str);
    }

    public Set<String> pq() {
        Map all = this.fm.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set<String> keySet = all.keySet();
        if (keySet == null) {
            return new HashSet();
        }
        return keySet;
    }

    public String pu(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            Map all = this.fm.getAll();
            if (all == null) {
                return null;
            }
            Iterable<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return null;
            }
            for (Entry entry : entrySet) {
                String str2 = (String) entry.getKey();
                if (str.equals(c.j((String) entry.getValue()))) {
                    return str2;
                }
            }
            return null;
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", e.toString(), e);
        }
    }

    public void pv() {
        this.fm.dz();
    }
}
