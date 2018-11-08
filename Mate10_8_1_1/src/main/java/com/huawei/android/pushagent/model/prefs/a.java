package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.a.c;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class a {
    private static final byte[] cl = new byte[0];
    private static a cm;
    private final c cn;

    private a(Context context) {
        this.cn = new c(context, "pclient_info_v2");
    }

    public static a kc(Context context) {
        return kh(context);
    }

    private static a kh(Context context) {
        a aVar;
        synchronized (cl) {
            if (cm == null) {
                cm = new a(context);
            }
            aVar = cm;
        }
        return aVar;
    }

    public boolean kf() {
        return this.cn.al();
    }

    public String kb(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return this.cn.aj(str);
    }

    public String ke(String str) {
        return com.huawei.android.pushagent.utils.c.c.bb(kb(str));
    }

    public boolean kj(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.cn.am(str, str2);
    }

    public boolean kk(String str, String str2) {
        return kj(str, com.huawei.android.pushagent.utils.c.c.bc(str2));
    }

    public void ki(String str) {
        this.cn.an(str);
    }

    public Set<String> kg() {
        Map all = this.cn.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set<String> keySet = all.keySet();
        if (keySet == null) {
            return new HashSet();
        }
        return keySet;
    }

    public String kd(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        try {
            Map all = this.cn.getAll();
            if (all == null) {
                return null;
            }
            Iterable<Entry> entrySet = all.entrySet();
            if (entrySet == null) {
                return null;
            }
            for (Entry entry : entrySet) {
                String str2 = (String) entry.getKey();
                if (str.equals(com.huawei.android.pushagent.utils.c.c.bb((String) entry.getValue()))) {
                    return str2;
                }
            }
            return null;
        } catch (Throwable e) {
            b.aa("PushLog2976", e.toString(), e);
        }
    }

    public void ka() {
        this.cn.ao();
    }
}
