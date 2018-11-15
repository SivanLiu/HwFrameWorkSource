package com.huawei.android.pushagent.model.b;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.model.prefs.h;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.model.prefs.m;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.UnsupportedEncodingException;

public class a {
    private static final byte[] gq = new byte[0];
    private static a gr = null;

    public static void xc(Context context) {
        Iterable<String> pq = e.pn(context).pq();
        if (pq.size() > 0) {
            for (String str : pq) {
                if (!g.gc(context, g.fu(str), g.fv(str))) {
                    m.vg(context).vh(e.pn(context).pp(str), str);
                    e.pn(context).pr(str);
                }
            }
        }
    }

    public static boolean xa(Context context, String str, String str2) {
        if (TextUtils.isEmpty(e.pn(context).pp(g.fs(str, str2)))) {
            return false;
        }
        return true;
    }

    public static boolean wy(Context context, String str, String str2) {
        int or = b.oq(context).or();
        String fs = g.fs(str, str2);
        if (1 == or) {
            if (j.rb(context).rc(fs) || (TextUtils.isEmpty(e.pn(context).pp(fs)) ^ 1) != 0) {
                return true;
            }
        } else if (!TextUtils.isEmpty(e.pn(context).ps(fs))) {
            return true;
        }
        return false;
    }

    public static boolean xb(Context context, String str, String str2, byte[] bArr) {
        int or = b.oq(context).or();
        String fs = g.fs(str, str2);
        if (1 == or) {
            return true;
        }
        if (bArr != null) {
            try {
                if (new String(bArr, "UTF-8").equals(e.pn(context).ps(fs))) {
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                c.eq("PushLog3413", "server token parse string failed");
            }
        }
        return false;
    }

    public static boolean wz(Context context) {
        if (e.pn(context).pt() || (m.vg(context).vi() ^ 1) == 0 || (h.qr(context).qu() ^ 1) == 0 || (j.rb(context).rd() ^ 1) == 0) {
            return true;
        }
        return false;
    }
}
