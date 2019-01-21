package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.e.a;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class b {
    private static final byte[] ci = new byte[0];
    private static b cj;
    private final com.huawei.android.pushagent.utils.b.b ck;

    private b(Context context) {
        this.ck = new com.huawei.android.pushagent.utils.b.b(context, "pclient_info_v2");
    }

    public static b il(Context context) {
        return iq(context);
    }

    private static b iq(Context context) {
        b bVar;
        synchronized (ci) {
            if (cj == null) {
                cj = new b(context);
            }
            bVar = cj;
        }
        return bVar;
    }

    public boolean io() {
        return this.ck.tf();
    }

    public String ik(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return this.ck.tg(str);
    }

    public String in(String str) {
        return a.vt(ik(str));
    }

    public boolean is(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.ck.th(str, str2);
    }

    public boolean it(String str, String str2) {
        return is(str, a.vu(str2));
    }

    public void ir(String str) {
        this.ck.ti(str);
    }

    public Set<String> ip() {
        Map all = this.ck.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set keySet = all.keySet();
        if (keySet == null) {
            return new HashSet();
        }
        return keySet;
    }

    public String im(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            Map all = this.ck.getAll();
            if (all == null) {
                return null;
            }
            Set<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return null;
            }
            for (Entry entry : entrySet) {
                String str2 = (String) entry.getKey();
                if (str.equals(a.vt((String) entry.getValue()))) {
                    return str2;
                }
            }
            return null;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
        }
    }

    public void ij() {
        this.ck.tj();
    }
}
