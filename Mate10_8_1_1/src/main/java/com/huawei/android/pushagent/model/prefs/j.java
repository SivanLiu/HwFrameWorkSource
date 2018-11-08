package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.c;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class j {
    private static final byte[] dn = new byte[0];
    private static j do;
    private final c dp;

    private j(Context context) {
        this.dp = new c(context, "pclient_request_info");
    }

    public static j pn(Context context) {
        return ps(context);
    }

    private static j ps(Context context) {
        j jVar;
        synchronized (dn) {
            if (do == null) {
                do = new j(context);
            }
            jVar = do;
        }
        return jVar;
    }

    public boolean pr() {
        return this.dp.al();
    }

    public boolean pq(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.dp.am(str, "true");
    }

    public boolean remove(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.dp.an(str);
    }

    public String po(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return this.dp.aj(str);
    }

    public Set<String> pp() {
        Map all = this.dp.getAll();
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
