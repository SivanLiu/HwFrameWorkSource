package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.a;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class h {
    private static final byte[] fx = new byte[0];
    private static h fy;
    private final a fz;

    private h(Context context) {
        this.fz = new a(context, "pclient_request_info");
    }

    public static h qr(Context context) {
        return qw(context);
    }

    private static h qw(Context context) {
        h hVar;
        synchronized (fx) {
            if (fy == null) {
                fy = new h(context);
            }
            hVar = fy;
        }
        return hVar;
    }

    public boolean qu() {
        return this.fz.eg();
    }

    public boolean qv(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.fz.ea(str, "true");
    }

    public boolean remove(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.fz.ed(str);
    }

    public String qs(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return this.fz.ec(str);
    }

    public Set<String> qt() {
        Map all = this.fz.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set<String> keySet = all.keySet();
        if (keySet == null) {
            return new HashSet();
        }
        return keySet;
    }
}
