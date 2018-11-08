package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.a.c;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class l {
    private static final byte[] ds = new byte[0];
    private static l dt;
    private final c du;

    private l(Context context) {
        this.du = new c(context, "pclient_unRegist_info_v2");
    }

    public static l qo(Context context) {
        return qw(context);
    }

    private static l qw(Context context) {
        l lVar;
        synchronized (ds) {
            if (dt == null) {
                dt = new l(context);
            }
            lVar = dt;
        }
        return lVar;
    }

    public boolean qt() {
        return this.du.al();
    }

    public String qv(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        try {
            Map all = this.du.getAll();
            if (all == null) {
                return "";
            }
            Iterable<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return "";
            }
            for (Entry entry : entrySet) {
                String str2 = (String) entry.getKey();
                String str3 = (String) entry.getValue();
                if (str.equals(com.huawei.android.pushagent.utils.c.c.bb(str2))) {
                    return str3;
                }
            }
            return "";
        } catch (Throwable e) {
            b.aa("PushLog2976", e.toString(), e);
        }
    }

    public String qp(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String str2;
        for (Entry entry : this.du.getAll().entrySet()) {
            if (str.equals((String) entry.getValue())) {
                str2 = (String) entry.getKey();
                break;
            }
        }
        str2 = null;
        return str2;
    }

    public Set<Entry<String, String>> qu() {
        Map all = this.du.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set<Entry<String, String>> entrySet = all.entrySet();
        if (entrySet == null) {
            return new HashSet();
        }
        return entrySet;
    }

    public boolean qs(String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return false;
        }
        return this.du.am(str, str2);
    }

    public boolean qr(String str, String str2) {
        return qs(com.huawei.android.pushagent.utils.c.c.bc(str), str2);
    }

    public void qq(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                Map all = this.du.getAll();
                if (all != null) {
                    Iterable<Entry> entrySet = all.entrySet();
                    if (entrySet != null) {
                        for (Entry key : entrySet) {
                            String str2 = (String) key.getKey();
                            if (str.equals(com.huawei.android.pushagent.utils.c.c.bb(str2))) {
                                this.du.an(str2);
                                return;
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                b.aa("PushLog2976", e.toString(), e);
            }
        }
    }
}
