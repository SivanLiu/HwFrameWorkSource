package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.c;
import com.huawei.android.pushagent.utils.f.a;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class m {
    private static final byte[] gj = new byte[0];
    private static m gk;
    private final a gl;

    private m(Context context) {
        this.gl = new a(context, "pclient_unRegist_info_v2");
    }

    public static m vg(Context context) {
        return vo(context);
    }

    private static m vo(Context context) {
        m mVar;
        synchronized (gj) {
            if (gk == null) {
                gk = new m(context);
            }
            mVar = gk;
        }
        return mVar;
    }

    public boolean vi() {
        return this.gl.eg();
    }

    public String vn(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        try {
            Map all = this.gl.getAll();
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
                if (str.equals(c.j(str2))) {
                    return str3;
                }
            }
            return "";
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", e.toString(), e);
        }
    }

    public String vj(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        String str2;
        for (Entry entry : this.gl.getAll().entrySet()) {
            if (str.equals((String) entry.getValue())) {
                str2 = (String) entry.getKey();
                break;
            }
        }
        str2 = null;
        return str2;
    }

    public Set<Entry<String, String>> vm() {
        Map all = this.gl.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set<Entry<String, String>> entrySet = all.entrySet();
        if (entrySet == null) {
            return new HashSet();
        }
        return entrySet;
    }

    public boolean vh(String str, String str2) {
        if (TextUtils.isEmpty(str2)) {
            return false;
        }
        return this.gl.ea(str, str2);
    }

    public boolean vl(String str, String str2) {
        return vh(c.k(str), str2);
    }

    public void vk(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                Map all = this.gl.getAll();
                if (all != null) {
                    Iterable<Entry> entrySet = all.entrySet();
                    if (entrySet != null) {
                        for (Entry key : entrySet) {
                            String str2 = (String) key.getKey();
                            if (str.equals(c.j(str2))) {
                                this.gl.ed(str2);
                                return;
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.f.c.es("PushLog3413", e.toString(), e);
            }
        }
    }
}
