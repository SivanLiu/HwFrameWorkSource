package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.b;
import com.huawei.android.pushagent.utils.e.a;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class d {
    private static final byte[] co = new byte[0];
    private static d cp;
    private final b cq;

    private d(Context context) {
        this.cq = new b(context, "pclient_unRegist_info_v2");
    }

    public static d ja(Context context) {
        return jh(context);
    }

    private static d jh(Context context) {
        d dVar;
        synchronized (co) {
            if (cp == null) {
                cp = new d(context);
            }
            dVar = cp;
        }
        return dVar;
    }

    public boolean jg() {
        return this.cq.tf();
    }

    public String jf(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        try {
            Map all = this.cq.getAll();
            if (all == null) {
                return "";
            }
            Set<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return "";
            }
            for (Entry entry : entrySet) {
                String str2 = (String) entry.getKey();
                String str3 = (String) entry.getValue();
                if (str.equals(a.vt(str2))) {
                    return str3;
                }
            }
            return "";
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
        }
    }

    public String jb(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String str2;
        for (Entry entry : this.cq.getAll().entrySet()) {
            if (str.equals((String) entry.getValue())) {
                str2 = (String) entry.getKey();
                break;
            }
        }
        str2 = null;
        return str2;
    }

    public Set<Entry<String, String>> je() {
        Map all = this.cq.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set entrySet = all.entrySet();
        if (entrySet == null) {
            return new HashSet();
        }
        return entrySet;
    }

    public boolean ji(String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return false;
        }
        return this.cq.th(str, str2);
    }

    public boolean jd(String str, String str2) {
        return ji(a.vu(str), str2);
    }

    public void jc(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                Map all = this.cq.getAll();
                if (all != null) {
                    Set<Entry> entrySet = all.entrySet();
                    if (entrySet != null) {
                        for (Entry key : entrySet) {
                            String str2 = (String) key.getKey();
                            if (str.equals(a.vt(str2))) {
                                this.cq.ti(str2);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
            }
        }
    }
}
