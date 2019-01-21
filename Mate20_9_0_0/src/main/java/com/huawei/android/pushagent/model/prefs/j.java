package com.huawei.android.pushagent.model.prefs;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.b;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class j {
    private static final byte[] df = new byte[0];
    private static j dg;
    private final b dh;

    private j(Context context) {
        this.dh = new b(context, "pclient_request_info");
    }

    public static j lm(Context context) {
        return lr(context);
    }

    private static j lr(Context context) {
        j jVar;
        synchronized (df) {
            if (dg == null) {
                dg = new j(context);
            }
            jVar = dg;
        }
        return jVar;
    }

    public boolean lq() {
        return this.dh.tf();
    }

    public boolean ln(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.dh.th(str, "true");
    }

    public boolean remove(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        return this.dh.ti(str);
    }

    public String lo(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return this.dh.tg(str);
    }

    public Set<String> lp() {
        Map all = this.dh.getAll();
        if (all == null) {
            return new HashSet();
        }
        Set keySet = all.keySet();
        if (keySet == null) {
            return new HashSet();
        }
        return keySet;
    }
}
