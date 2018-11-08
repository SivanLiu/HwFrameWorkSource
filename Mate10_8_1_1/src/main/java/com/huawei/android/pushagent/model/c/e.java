package com.huawei.android.pushagent.model.c;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.android.pushagent.model.prefs.a;
import com.huawei.android.pushagent.model.prefs.b;
import com.huawei.android.pushagent.model.prefs.h;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.f;
import java.io.UnsupportedEncodingException;

public class e {
    private static final byte[] ep = new byte[0];
    private static e eq = null;

    public static void tj(Context context) {
        Iterable<String> kg = a.kc(context).kg();
        if (kg.size() > 0) {
            for (String str : kg) {
                if (!f.gq(context, f.gh(str), f.gi(str))) {
                    l.qo(context).qs(a.kc(context).kb(str), str);
                    a.kc(context).ki(str);
                }
            }
        }
    }

    public static boolean th(Context context, String str, String str2) {
        if (TextUtils.isEmpty(a.kc(context).kb(f.fz(str, str2)))) {
            return false;
        }
        return true;
    }

    public static boolean tf(Context context, String str, String str2) {
        int km = b.kp(context).km();
        String fz = f.fz(str, str2);
        if (1 == km) {
            if (h.md(context).mg(fz) || (TextUtils.isEmpty(a.kc(context).kb(fz)) ^ 1) != 0) {
                return true;
            }
        } else if (!TextUtils.isEmpty(a.kc(context).ke(fz))) {
            return true;
        }
        return false;
    }

    public static boolean tg(Context context, String str, String str2, byte[] bArr) {
        int km = b.kp(context).km();
        String fz = f.fz(str, str2);
        if (1 == km) {
            return true;
        }
        if (bArr != null) {
            try {
                if (new String(bArr, "UTF-8").equals(a.kc(context).ke(fz))) {
                    return true;
                }
            } catch (UnsupportedEncodingException e) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "server token parse string failed");
            }
        }
        return false;
    }

    public static boolean ti(Context context) {
        if (a.kc(context).kf() || (l.qo(context).qt() ^ 1) == 0 || (j.pn(context).pr() ^ 1) == 0 || (h.md(context).mh() ^ 1) == 0) {
            return true;
        }
        return false;
    }
}
